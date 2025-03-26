package qmul.ds.interactiveInduction;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;
import me.tongfei.progressbar.ProgressBar;
import org.apache.log4j.Logger;
import qmul.ds.InteractiveContextParser;
import qmul.ds.Utterance;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.learn.Evaluation;
import qmul.ds.learn.RecordTypeCorpus;
import qmul.ds.learn.TTRWordLearner;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.io.*;


/**
 * A class to train and evaluate the BabyDS model on OUR BabyAI dataset ((instruction, TTR semantics) pairs).
 * @author: Arash A.
 */
public class BabyDSInduction {
    private static final Logger logger = Logger.getLogger(BabyDSInduction.class);
//    ProgressBarBuilder pbb = ProgressBar.builder()  // To fix later: this is not effective, because it's not used in the code.
//            .setStyle(ProgressBarStyle.builder()
//            .colorCode((byte) 37).build());

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RED = "\u001B[31m";

    //TODO fml, i have used this all wrong. THis is only the seed grammar path and nothing more. To fix. Everywhere else, it's modelPath.
    static final String seedGrammarPath = "resource\\2023-babyds-induction-output\\".replace("\\", File.separator); // The dir that works!
//    static String modelDirUser ="2025_babyds_RQ2" ;//"2024_babyds_induction";//"2025_babyds_RQ2";  // User has to modify every time!
    static final String modelPath = "resource\\2024_babyds_induction\\".replace("\\", File.separator);  //the dir that works!
// ("resource\\" + modelDirUser + "\\").replace("\\", File.separator);  // Wherever modelPath was needed, verify_model_path() has to be called!
//    static final String modelPath = "resource\\2025-babyds-RQ2\\".replace("\\", File.separator);

//        static final String corpusPath = modelPath;//"resource\\2023-babyds-induction-output\\".replace("\\", File.separator);

    static final String dataset = "class1";//""babyds_WoSubj.txt";//"babyds_wDylan.txt";
//    static final String resultsFileName = "";

    public static final long SEED = 45; // Set a constant seed for reproducibility
    public static final Random random = new Random(SEED);
    public static final int TOP_N = 5;  // topN learned actions to evaluate
    public static final double TRAIN_TEST_RATIO = 0.85;  // Train-Test split ratio (Meaning the x ratio is for train, 1-x is for test)
    public static final double INIT_BATCH_RATIO = 0.1;  // Size of the initial data batch for evaluation - used in prepare_batch().
    public static final double INC_BATCH_RATIO = 0.1; // Size of the incremental data batches for evaluation - used in prepare_batch().
    private static final int FOLDS = 0;  // Number of folds for k-fold cross validation. Use 0 for train-test split.
    public static final boolean SAVE_TO_FILE = true;  // Save the training and testing sets to file
    private static final boolean PRINT_DIAG = true;  // Print failed parses and exact matches (for each top-n and dataset) if true.


    // Two hashmaps for diagnostics, one for failed parses, and one for failed exact matches, for each topN and dataset.
    // Helps to see which sentences are not parsed, and which ones are parsed but not exactly matched, and this makes
    // "mis-learned" patterns more visible.
    HashMap<Pair<String, Integer> , List<String>> failedParses = new HashMap<>();
    HashMap<Pair<String, Integer> , List<String>> failedExactMatches = new HashMap<>();


    BabyDSInduction () {
        logger.warn("Make sure (at least in IntelliJ) your working directory is set to /DyLan to prevent errors with dirs.");
        try{
            verify_model_path(modelPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

        /**
     * Overloads the below method, with default values for modelAddress and datasetName and kfcv.
     * @return A pair of semantic accuracy and parsing coverage results.
     * @author: AA
     */
    public Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> evaluate_model() {
        return evaluate_model(0, "", "");
    }

    /**
     * Overloads the below method, with default values for modelAddress and datasetName.
     * @param kfcv The number of folds for k-fold cross validation. If 0, a train-test split is performed.
     * @return A pair of semantic accuracy and parsing coverage results.
     * @author: AA
     */
    public Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> evaluate_model(int kfcv) {
        return evaluate_model(kfcv, "", "");
    }


    /**
     * AA: assumes that model and dataset are in the same directory.
     * A method for full evaluation:
     * 1. Loads the learned lexical actions (parsing model) for all topNs.
     * 2. Loads both training and testing sets.
     * 3. Evaluates the parsing model on both sets.
     * 4. Returns the calculated results: mainly semantic accuracy and parsing coverage.
     */
    public Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> evaluate_model(int kfcv, String modelAddress, String datasetName) {  // TODO take mode as input...
        logger.info("Evaluating BabyDS model...");
        EvalResult evalResult = new EvalResult();
        HashMap<Integer, HashMap<String, HashMap<String, Double>>> semanticAccuracy = new HashMap<>();
        HashMap<Integer, HashMap<String, ArrayList<Double>>> parsingCoverage = new HashMap<>();  // Should have used double[] instead of ArrayList<Double>... Anyways.
        Evaluation eval = new Evaluation();  // AA: For semantic accuracy - based on Julian's code.
        for (int n = 1; n <= TOP_N; n++) {
            logger.info("Loading parser with top-" + n + " learned actions...");
            InteractiveContextParser parser;
            if (modelAddress.isEmpty()) {
                parser = new InteractiveContextParser(modelPath, n);
            } else {
                parser = new InteractiveContextParser(modelAddress, n);
            }
            File[] train_test_files = get_corpus_files(kfcv, modelAddress, datasetName);
            for (int i = 0; i < train_test_files.length; i++) {
                int parsedCount = 0;
                int exactMatchCount = 0;
                File file = train_test_files[i];
                String dataset_name = i == 0 ? "train" : "test";
                RecordTypeCorpus corpus = new RecordTypeCorpus();
                try {
                    corpus.loadCorpus(file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                logger.info("Loaded corpus: " + file.getName());
                List<TTRRecordType[]> evalList = new ArrayList<>();
                for (Pair<Sentence<Word>, TTRRecordType> pair : ProgressBar.wrap(corpus, "Eval progress with top-" + n + " actions: ")) {
                    parser.init();  // Restarts parser.
                    Sentence<Word> sentence = pair.first();
                    TTRRecordType goldSem = pair.second();
                    boolean parsed = parser.parseUtterance(new Utterance(sentence));
                    if (parsed) {
                        List<TTRRecordType> allSemantics = new ArrayList<>();
                        logger.debug("With top-" + n + " actions parsed: sentence: " + sentence);
                        logger.debug("Gold semantics: " + goldSem);
                        TTRRecordType parsedSem = (TTRRecordType) parser.getState().getCurrentTuple().getSemantics();
                        allSemantics.add(parsedSem);
                        logger.debug("Looking for more possible semantics...");  // Because of the ambiguity caused by computational actions.
                        int parseIdx = 0;
                        while (parser.parse()) {  // Steps through all different semantic interpretations.
                            try {
                                logger.trace(ANSI_YELLOW + parseIdx + "- other semantics: " + parser.getState().getCurrentTuple().getSemantics() + ANSI_RESET);
                                TTRRecordType newSem = (TTRRecordType) parser.getState().getCurrentTuple().getSemantics();
                                parseIdx++;
                                if (!allSemantics.contains(newSem)) {
                                    allSemantics.add(newSem);
                                }
                            } catch (Exception e) {
                                logger.warn(ANSI_RED + "Sem is probs DisjunctiveType?: " + parser.getState().getCurrentTuple().getSemantics() + ANSI_RESET);
                                break;
                            }
                        }
                        for (TTRRecordType sem : allSemantics) {

                            if (sem.subsumes(goldSem) && goldSem.subsumes(sem)) {
                                parsedSem = sem;
                                break;
                            }
                        }

                        evalList.add(new TTRRecordType[]{parsedSem, goldSem});
                        logger.debug("Hyp semantics: " + parsedSem);
                        logger.info(ANSI_GREEN + "Was able to parse: " + ANSI_RESET + sentence);
                        parsedCount++;
                        if (parsedSem.subsumes(goldSem) && goldSem.subsumes(parsedSem)) {
                            exactMatchCount++;
                            logger.info(ANSI_GREEN + "Exact match found for: " + ANSI_RESET + sentence);
                        }
                        else {
                            logger.info(ANSI_YELLOW + "No exact match found for: " + ANSI_RESET + sentence);
                            failedExactMatches.putIfAbsent(new Pair<>(dataset_name, n) , new ArrayList<>());
                            failedExactMatches.get(new Pair<>(dataset_name, n)).add(sentence.toString());
                            evalResult.addFailedExactMatch(dataset_name, n, sentence.toString());
                        }

                    } else {
                        logger.info(ANSI_RED + "Parsing failed for: " + ANSI_RESET + sentence);
                        failedParses.putIfAbsent(new Pair<>(dataset_name, n) , new ArrayList<>());
                        failedParses.get(new Pair<>(dataset_name, n)).add(sentence.toString());
                        evalResult.addFailedParse(dataset_name, n, sentence.toString());
                    }
//                    System.out.print("\u001b[1A");
                }
                List<Float> scores = eval.precisionRecallMacro(evalList);
                HashMap<String, HashMap <String, Double>> datasetMap = semanticAccuracy.getOrDefault(n, new HashMap<>());
                HashMap<String, Double> scoresMap = datasetMap.getOrDefault(dataset_name, new HashMap<>());
                scoresMap.put("precision", (double) scores.get(0)*100);
                scoresMap.put("recall", (double) scores.get(1)*100);
                scoresMap.put("f1", (double) scores.get(2)*100);
                datasetMap.put(dataset_name, scoresMap);
                semanticAccuracy.put(n, datasetMap);
                logger.info("Semantic accuracy for " + dataset_name + ": " + scores);
                HashMap<String, ArrayList<Double>> datasetCoverage = parsingCoverage.getOrDefault(n, new HashMap<>());
                datasetCoverage.put(dataset_name,datasetCoverage.getOrDefault(dataset_name, new ArrayList<>()));
                datasetCoverage.get(dataset_name).add((double) parsedCount / corpus.size() * 100);
                datasetCoverage.get(dataset_name).add((double) exactMatchCount / corpus.size() * 100);
                parsingCoverage.put(n, datasetCoverage);

                evalResult.addSemanticAccuracy( n, dataset_name, scores.get(0)*100, scores.get(1)*100, scores.get(2)*100);
                evalResult.addParsingCoverage(n, dataset_name, (double) parsedCount / corpus.size() * 100, (double) exactMatchCount / corpus.size() * 100);

//                parsingCoverage.get(n).put(dataset_name, (double) parsedCount / corpus.size() * 100);  // AA MODIFIED FROM parsedCount
                logger.info("Parsing coverage for " + dataset_name + ": " + parsedCount + " out of " + corpus.size());  // AA MODIFIED FROM parsedCount
            }
        }
        evalResult.writeResutlsToFile(modelAddress);
        return new Pair<>(semanticAccuracy, parsingCoverage);
    }


    /**
     * Prints the semantic accuracy results generated by evaluate_model as a table.
     */
    public void print_semanticAcc_results(HashMap<Integer, HashMap<String, HashMap<String, Double>>> results, String info) {
        String leftAlignFormat = "| %-15s | %6.2f | %6.2f | %6.2f | %6.2f | %6.2f | %6.2f |%n";
        String rowSeparator = "+-----------------+--------+--------+--------+--------+--------+--------+";

        System.out.println("\n====================| Semantic Accuracy Results |====================");
        if (info != null)
            System.out.println(info);
        System.out.println("+-----------------+--------------------------+--------------------------+");
        System.out.format("| Actions \\ Data  |           Train          |          Test            |%n");
        System.out.println("|                 +--------------------------+--------------------------+");
        System.out.format("|                 |   P    |    R   |   F1   |   P    |    R   |   F1   |%n");
        System.out.println(rowSeparator);

        for (int topN : results.keySet()) {
            HashMap<String, Double> trainScores = results.get(topN).get("train");
            HashMap<String, Double> testScores = results.get(topN).get("test");
            System.out.printf(leftAlignFormat, "Top-" + topN, trainScores.get("precision"), trainScores.get("recall"),
                    trainScores.get("f1"), testScores.get("precision"), testScores.get("recall"), testScores.get("f1"));
        }
        System.out.println(rowSeparator);
    }

    /**
     * Prints the parsing coverage results generated by evaluate_model as a table.
     */
    public String get_parsingCoverage_results(HashMap<Integer, HashMap<String, ArrayList<Double>>> results, String info) {
        String parsingCoverage_results = "";
        String leftAlignFormat = "| %-15s | %10.2f | %11.2f | %10.2f | %11.2f |%n";
        String rowSeparator = "+-----------------+------------+-------------+------------+-------------+";

        parsingCoverage_results += "====================| Parsing Coverage Results |====================\n";
        if (info != null)
            parsingCoverage_results += info+"\n";
        parsingCoverage_results += "+-----------------+--------------------------+--------------------------+\n";
        parsingCoverage_results += String.format("| Actions \\ Data  |           Train          |          Test            |%n");
        parsingCoverage_results += "|                 +--------------------------+--------------------------+\n";
        parsingCoverage_results += String.format("|                 |  Coverage  | Exact Match |  Coverage  | Exact Match | %n");
        parsingCoverage_results += rowSeparator + "\n";

        for (int topN : results.keySet()) {
            HashMap<String, ArrayList<Double>> coverageScores = results.get(topN);
            ArrayList<Double> trainScores = coverageScores.get("train");
            ArrayList<Double> testScores = coverageScores.get("test");
            parsingCoverage_results += String.format(leftAlignFormat, "Top-" + topN, trainScores.get(0), trainScores.get(1), testScores.get(0), testScores.get(1));
        }
        parsingCoverage_results += rowSeparator;
        return parsingCoverage_results;
    }


    public void print_diagnostics() {
        System.out.println("\n =====================| Diagnostics |=====================\n");
        System.out.printf("Here are the failed parses and failed exact matches for each top-%d action:%n", TOP_N);
        System.out.println("***** Failed Parses *****");
        if (failedParses.isEmpty())
            System.out.println("No failed parses.");
        else
            for (Pair<String, Integer> key : failedParses.keySet()) {
                System.out.println(">>> Dataset: " + key.first() + " | Top-" + key.second() + " | Failed Parses:");
                for (String s : failedParses.get(key))
                    System.out.println(s);
            }
        System.out.println("\n***** Failed Exact Matches *****");
        if (failedExactMatches.isEmpty())
            System.out.println("No failed exact matches.");
        else
            for (Pair<String, Integer> key : failedExactMatches.keySet()) {
                System.out.println(">>> Dataset: " + key.first() + " | Top-" + key.second() + " | Failed Exact Matches:");
                for (String s : failedExactMatches.get(key))
                    System.out.println(s);
            }
    }


    /**
     * Returns the training and testing files from the corpus directory.
     * @return train_test_files An array of two files: training and testing files.
     */
//    public File load_data(String filename) {
//        File data_file = new File(corpusPath + filename);
//        File[] train_test_files = new File[2];
//        File folder = new File(corpusPath);
//        File[] listOfFiles = folder.listFiles();
//        logger.trace("Trying to load train-test split files...");
//        if (listOfFiles != null) {
//            for (File file : listOfFiles) {
////                logger.trace(file.getName());
//                 {
//                    logger.trace("Trying to load k-fold cross validation files...");
//                    if (file.isFile() && file.getName().startsWith("babyds_kfcv_" + (kfcv-1) + "_train")) {
//                        train_test_files[0] = file;
//                        continue;
//                    }
//                    if (file.isFile() && file.getName().startsWith("babyds_kfcv_" + (kfcv-1) + "_test"))
//                        train_test_files[1] = file;
//                }
//            }
//            if (train_test_files[0] == null || train_test_files[1] == null)
//                logger.warn(ANSI_RED + "Couldn't find either training or testing file." + ANSI_RESET);
//        } else {
//            logger.error(ANSI_RED + "No files found in the corpus directory." + ANSI_RESET);
//        }
//        return train_test_files;
//    }


    /**
     * Returns the training and testing files from the corpus directory.
     * @param kfcv The k-fold cross validation index. If 0, a train-test data is returned.
     * @param modelAddress The address of the model directory.
     * @param datasetName The dataset name.     *
     * @return train_test_files An array of two files: training and testing files.
     * TODO rename
     */
    public File[] get_corpus_files(int kfcv, String modelAddress, String datasetName) {
        File[] train_test_files = new File[2];
        File folder;
        if (modelAddress.isEmpty()) {
            folder = new File(modelPath);
        } else {
            folder = new File(modelAddress);
        }
        File[] listOfFiles = folder.listFiles();
        if (datasetName.isEmpty()) {
            datasetName = dataset;
        }
        logger.trace("Trying to load train-test split files...");
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
//                logger.trace(file.getName());
                if (kfcv == 0) {
                    if (file.isFile() && file.getName().startsWith(datasetName+"_train")) {
                        train_test_files[0] = file;
                        logger.trace("Training file found: " + file.getName());
                        continue;
                    }
                    if (file.isFile() && file.getName().startsWith(datasetName+"_test")) {
                        train_test_files[1] = file;
                        logger.trace("Testing file found: " + file.getName());
                    }
                } else {
                    logger.trace("Trying to load k-fold cross validation files...");
                    if (file.isFile() && file.getName().startsWith(datasetName+"_kfcv_" + (kfcv-1) + "_train")) {
                        train_test_files[0] = file;
                        continue;
                    }
                    if (file.isFile() && file.getName().startsWith(datasetName+"_kfcv_" + (kfcv-1) + "_test"))
                        train_test_files[1] = file;
                }
            }
            if (train_test_files[0] == null || train_test_files[1] == null)
                logger.warn(ANSI_RED + "Couldn't find either training or testing file." + ANSI_RESET);
        } else {
            logger.error(ANSI_RED + "No files found in the corpus directory." + ANSI_RESET);
        }
        return train_test_files;
    }

    // Used in the mergeFiles method below.
    private static void writeContent(File file, BufferedWriter writer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine(); // Ensure lines are properly separated
            }
        }
    }

    /** Used where I need to merge two datasets and create a RT corpus from them.
     * Merges two files into one and saves it.
     * @param file1 The first file to merge.
     * @param file2 The second file to merge.
     * @param outputFile The output file to save the merged content.
     * @throws IOException If an I/O error occurs.
     */
   public static void mergeFiles(File file1, File file2, File outputFile) throws IOException {
       try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
           // Read first file and write to output
           writeContent(file1, writer);
           // Read second file and write to output
           writeContent(file2, writer);
       }
   }


    /**
     * overload of the below method, with default values for filename.
     * @param corpusAddress
     * @param ratio
     * @param saveToFile
     * @return
     */
   public Pair<RecordTypeCorpus, RecordTypeCorpus> train_test_split(String corpusAddress, double ratio, boolean saveToFile, String saveAddress) {
               RecordTypeCorpus corpus = new RecordTypeCorpus();
        try {
//            corpus.loadCorpus(new File(modelPath + filename + ".txt"));
            corpus.loadCorpus(new File(corpusAddress));
//            corpus.loadCorpus(new File(corpusPath + datasetName));  // TODO modified by me. Need to make sure it's compatible with elsewhere.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return train_test_split(corpus, ratio, saveToFile, saveAddress);
   }


    /**
     * Splits a generated dataset into training and testing sets based on the given ratio, and saves them to file.
     * Convention for name: datasetName_train_size.txt and datasetName_test_size.txt
     * @param ratio The ratio of training to testing data.
     * @return A pair of training and testing datasets.
     * @author Arash A.
     */
    public Pair<RecordTypeCorpus, RecordTypeCorpus> train_test_split(RecordTypeCorpus corpus, double ratio, boolean saveToFile, String saveAddress) {
        logger.info("Splitting the dataset into training and testing sets...");
        int size = corpus.size();
        int train_size = (int) (size * ratio);
        int test_size = size - train_size;
        logger.debug("Train size: " + train_size + " | Test size: " + test_size);
        String train_set_name = corpus.corpusName + "_train_" + train_size + ".txt";
        String test_set_name = corpus.corpusName + "_test_" + test_size + ".txt";
        RecordTypeCorpus trainCorpus = new RecordTypeCorpus(train_set_name);
        RecordTypeCorpus testCorpus = new RecordTypeCorpus(test_set_name);
        int i = 0;
        Collections.shuffle(corpus, random);
        for (Pair<Sentence<Word>, TTRRecordType> pair : corpus) {
            if (i < train_size)
                trainCorpus.add(pair);
            else
                testCorpus.add(pair);
            i++;
        }
        if (saveToFile) {
//            String train_set_name = "babyds_train_" + train_size + ".txt";
//            String test_set_name = "babyds_test_" + test_size + ".txt";

            try {  // todo Filenames used to save the split data should be exactly the same as the original, plus  train/test and size. Currently it's just train/test and size. Same for when reading, and for kFold.
                if (saveAddress.isEmpty()) {
                    trainCorpus.saveCorpus(modelPath + train_set_name);
                    testCorpus.saveCorpus(modelPath + test_set_name);
                } else {
                    trainCorpus.saveCorpus(saveAddress + train_set_name);
                    testCorpus.saveCorpus(saveAddress + test_set_name);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new Pair<>(trainCorpus, testCorpus);
    }


    /**
     * Performs k-fold cross validation on the dataset and saves the training and testing sets to file.
     * @param folds The number of folds for k-fold cross validation.
     * @param saveToFile A boolean to save the training and testing sets to file or not.
     * @return A list of pairs of training and testing datasets for each fold.
     * @throws IOException If an I/O error occurs.
     */
    public List<Pair<RecordTypeCorpus, RecordTypeCorpus>> kFoldCrossValidation(int folds, boolean saveToFile, String modelAddress) throws IOException {
        List<Pair<RecordTypeCorpus, RecordTypeCorpus>> train_test_pairs = new ArrayList<>();
        RecordTypeCorpus corpus = new RecordTypeCorpus();
        if (modelAddress.isEmpty()) {
            corpus.loadCorpus(new File(modelPath + dataset + ".txt"));
        } else {
            corpus.loadCorpus(new File(modelAddress + dataset + ".txt"));
        }
        int corpus_size = corpus.size();
        int fold_size = corpus_size / folds;
        Collections.shuffle(corpus, random);
        for (int i = 0; i < folds; i++) {
            RecordTypeCorpus trainCorpus = new RecordTypeCorpus();
            RecordTypeCorpus testCorpus = new RecordTypeCorpus();
            for (int j = 0; j < corpus_size; j++) {
                Pair<Sentence<Word>, TTRRecordType> pair = corpus.get(j);
                if (j >= i * fold_size && j < (i + 1) * fold_size)
                    testCorpus.add(pair);
                else
                    trainCorpus.add(pair);
            }
            if (saveToFile) {
                String train_set_name = "babyds_kfcv_" + i + "_train_" + trainCorpus.size() + ".txt";
                String test_set_name = "babyds_kfcv_" + i + "_test_" + testCorpus.size() + ".txt";
                trainCorpus.saveCorpus(modelPath + train_set_name);
                testCorpus.saveCorpus(modelPath + test_set_name);
            }
            train_test_pairs.add(new Pair<>(trainCorpus, testCorpus));
        }
        return train_test_pairs;
    }


    /**
     * Overloads the below method, so training corpus can be passed as both a RecordTypeCorpus and a path to the corpus file.
     * @param trainingCorpusFileName The path to the training data file.
     */
    public void train_model(String trainingCorpusFileName, String userDir) {

        RecordTypeCorpus trainingCorpus = new RecordTypeCorpus();
        try {
            trainingCorpus.loadCorpus(new File(userDir + trainingCorpusFileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        train_model(trainingCorpus, userDir);

//        logger.info("Training BabyDS model...");
//        // todo fix now: has to be the same name for lexicon, therefore have to create new dirs...
//        TTRWordLearner babyDSLearner = new TTRWordLearner(corpusPath);  //todo maybe change the name of this to only computational actions file path
//        TTRWordLearner babyDSLearner = new TTRWordLearner(trainingDataPath);  // new
//
//        String lexiconPath = modelPath + userDir + "lexicon.lex";
//        if (modelName.isEmpty()){
//            lexiconPath = modelPath + "lexicon.lex";
//        } else {
//            lexiconPath = modelPath + modelName;
//        }
//
//        try {
//            File corpusFile = new File(trainingDataPath);
//            babyDSLearner.loadCorpus(corpusFile);
//            logger.info("BabyDS training starting...");
//            babyDSLearner.learn();
//
//            babyDSLearner.getHypothesisBase().saveLearnedLexicon(lexiconPath, 1);  // Testing if top-1 can be a thing here:
//            babyDSLearner.getHypothesisBase().saveLearnedLexicon(lexiconPath, 2);
//            babyDSLearner.getHypothesisBase().saveLearnedLexicon(lexiconPath, 3);
//            babyDSLearner.getHypothesisBase().saveLearnedLexicon(lexiconPath, 4);
//            babyDSLearner.getHypothesisBase().saveLearnedLexicon(lexiconPath, 5);
//
//        } catch(Exception e) {
//			e.printStackTrace();
//		}
    }


    /**
     * Trains the BabyDS model on a given training data, and saves the top-5 learned lexicon to files.
     * @param trainingCorpus The training data to train the model on.
     * @param userDir
     */
    public void train_model(RecordTypeCorpus trainingCorpus, String userDir) {
        logger.info("Training BabyDS model...");
//        String userDir = modelPath + userDir;
        // Check if a trained model already exists, and if so, prompt user to see if they want to use it or learn another one:
        String[] files = new File(userDir).list();
        String x = userDir + "lexicon.lex";
        if (files != null) {
            for (String file : files) {
                if (file.startsWith("lexicon.lex")) {
                    logger.info("Previously trained model found in the given directory with name: " + file);
                    x = userDir + file;
                }
            }
        }
        if (new File(x).exists()) {
            System.out.println("A trained model already exists at: " + userDir);
            Scanner scanner = new Scanner(System.in);
            System.out.println("Do you want to load this model instead of training from scratch? (y/n)");
            String answer = scanner.nextLine();
            if (answer.equals("y")) {
                logger.info("Loading the existing model...");
                return;
            } else if (answer.equals("n")) {
                logger.info("Training BabyDS model from scratch...");
            } else {
                logger.error("Invalid input. Please enter 'y' or 'n'.");
                return;
            }
        }

        TTRWordLearner babyDS = new TTRWordLearner(seedGrammarPath);  //todo maybe change the name of this to only computational actions file path
//        TTRWordLearner babyDS = new TTRWordLearner(trainingDataPath);  // new

//        if (modelName.isEmpty()){
//            lexiconPath = modelPath + "lexicon.lex";
//        } else {
//            lexiconPath = modelPath + modelName;
//        }
        try {
            babyDS.setTrainingCorpus(trainingCorpus);
            logger.info("BabyDS training starting...");
            babyDS.learn();
            String modelName = userDir + "lexicon.lex";

            babyDS.getHypothesisBase().saveLearnedLexicon(modelName, 1);
            babyDS.getHypothesisBase().saveLearnedLexicon(modelName, 2);
            babyDS.getHypothesisBase().saveLearnedLexicon(modelName, 3);
            babyDS.getHypothesisBase().saveLearnedLexicon(modelName, 4);
            babyDS.getHypothesisBase().saveLearnedLexicon(modelName, 5);
        } catch(Exception e) {
			e.printStackTrace();
		}
    }


    /**
     * A pipeline for full training and evaluation:
     * @param kFold The number of folds for k-fold cross validation. If 0, a train-test split is performed.
     * @param saveToFile A boolean to save the training and testing sets to file.
     */
    public void full_pipeline(int kFold, boolean saveToFile, String modelAddress) {
        if (kFold != 0) {  // The k-fold cross validation scenario.
            logger.info("Performing " + kFold + "-fold Cross Validation...");
            List<HashMap<Integer, HashMap<String, HashMap<String, Double>>>> kfSemAccResults = new ArrayList<>();
            List<HashMap<Integer, HashMap<String, ArrayList<Double>>>> kfParsCvgResults = new ArrayList<>();
            try {
                List<Pair<RecordTypeCorpus, RecordTypeCorpus>> train_test_pairs = kFoldCrossValidation(kFold, saveToFile, modelAddress);
                int i = 0;  // fold index, aka kfcv counter.
                for (Pair<RecordTypeCorpus, RecordTypeCorpus> pair : train_test_pairs) {
                    int train_size = pair.first().size();
                    // TODO all "babyds_"s to be replaced with datasetName, as I previously did for the train_test_split version.
                    train_model(modelAddress + "babyds_kfcv_"+i+"_train_"+train_size+".txt", "");  // currently it doesn't save the kfcv models separately, just overwrites. TODO
                    Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> kfResultsPair = evaluate_model(i+1);
                    kfSemAccResults.add(kfResultsPair.first());  // Maybe refactor so testing data can be specified...
                    kfParsCvgResults.add(kfResultsPair.second());
                }
                // !! Uncomment below to print results for each fold !!  // todo test this...
//                for (int j = 0; j < kfResults.size(); j++) {
//                    print_eval_results(kfResults.get(j), String.format("%d-fold Cross Validation | Fold %d", kFold, j));
//                }
                // Average the semAcc results over the k-folds
                HashMap<Integer, HashMap<String, HashMap<String, Double>>> avgSemAccResults = new HashMap<>();
                for (int n : kfSemAccResults.getFirst().keySet()) {
                    HashMap<String, HashMap<String, Double>> datasetMap = new HashMap<>();
                    for (String dataset : kfSemAccResults.getFirst().get(n).keySet()) {
                        HashMap<String, Double> scoresMap = new HashMap<>();
                        double precision = 0.0, recall = 0.0, f1 = 0.0;
                        for (HashMap<Integer, HashMap<String, HashMap<String, Double>>> result : kfSemAccResults) {
                            precision += result.get(n).get(dataset).get("precision");
                            recall += result.get(n).get(dataset).get("recall");
                            f1 += result.get(n).get(dataset).get("f1");
                        }
                        scoresMap.put("precision", precision / kfSemAccResults.size());
                        scoresMap.put("recall", recall / kfSemAccResults.size());
                        scoresMap.put("f1", f1 / kfSemAccResults.size());
                        datasetMap.put(dataset, scoresMap);
                    }
                    avgSemAccResults.put(n, datasetMap);
                }
                // Average the parsing coverage results over the k-folds
                HashMap<Integer, HashMap<String, ArrayList<Double>>> avgParsCvgResults = new HashMap<>();
                for (int n : kfParsCvgResults.getFirst().keySet()) {
                    HashMap<String, ArrayList<Double>> coverageMap = new HashMap<>();
                    double trainCvg = 0.0, testCvg = 0.0;
                    double trainEM = 0.0, testEM = 0.0;
                    for (HashMap<Integer, HashMap<String, ArrayList<Double>>> result : kfParsCvgResults) {
                        trainCvg += result.get(n).get("train").get(0);
                        trainEM += result.get(n).get("train").get(1);
                        testCvg += result.get(n).get("test").get(0);
                        testEM += result.get(n).get("test").get(1);
                    }
                    coverageMap.put("train", new ArrayList<>(Arrays.asList(trainCvg / kfParsCvgResults.size(), trainEM / kfParsCvgResults.size())));
                    coverageMap.put("test", new ArrayList<>(Arrays.asList(testCvg / kfParsCvgResults.size(), testEM / kfParsCvgResults.size())));
                    avgParsCvgResults.put(n, coverageMap);
                }
                // Here I better print file names, and seed value.
                System.out.println();
                System.out.println("Results on:  Dataset: " + dataset + " | Seed: " + SEED + " | Folds: " + kFold);
                print_semanticAcc_results(avgSemAccResults, String.format("%d-fold Cross Validation averaged results", kFold));
                System.out.println();
                System.out.println(get_parsingCoverage_results(avgParsCvgResults, null));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else { // The simply train-test split scenario.
            logger.info("Performing Train-Test Split training with ratio " + TRAIN_TEST_RATIO + " now...");
            Pair<RecordTypeCorpus, RecordTypeCorpus> train_test_pair = train_test_split(modelPath+ dataset +".txt", TRAIN_TEST_RATIO, saveToFile, modelPath);  //TODO fix hard-coded name here
            int train_size = train_test_pair.first().size();
            int test_size = train_test_pair.second().size();
            train_model(dataset + "_train_" + train_size + ".txt", modelPath);
            Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> ttsResults = evaluate_model(0);
            System.out.println();
            System.out.println("Results on:  Dataset: " + dataset + " | Seed: " + SEED + " | Folds: " + kFold);
            System.out.println();
            print_semanticAcc_results(ttsResults.first(), String.format("Train-Test Split | Ratio: %.2f | Data Sizes: train=%d - test=%d",
                    TRAIN_TEST_RATIO, train_size, test_size));
            System.out.println();
            System.out.println(get_parsingCoverage_results(ttsResults.second(), ""));
        }

        if (PRINT_DIAG)
            print_diagnostics();
    }

    /**
     * overloads the method below, with default values for INIT_BATCH_RATIO and INC_BATCH_RATIO.
     * @param corpus
     * @return
     */
    public List<RecordTypeCorpus> prepare_batches(RecordTypeCorpus corpus) {
        return prepare_batches(corpus,
                INIT_BATCH_RATIO, INC_BATCH_RATIO);
    }


    /**
     * Based on INIT_BATCH_SIZE and INC_BATCH_SIZE, splits the dataset into batches and returns them as a list.
     * For model training purposes, the input corpus should be shuffled first.
     * @return A list of data batches.
     * @author: AA
     */
    public List<RecordTypeCorpus> prepare_batches(RecordTypeCorpus corpus, double init_batch_size, double inc_batch_size) {
        logger.info("Preparing batches for corpus " + corpus.corpusName + " with size: " + corpus.size() + " | INIT_BATCH_RATIO: " + INIT_BATCH_RATIO + " | INC_BATCH_RATIO: " + INC_BATCH_RATIO);
        List<RecordTypeCorpus> batches = new ArrayList<>();
        int corpus_size = corpus.size();
        int batch_count = 1;
        int init_size = (int) (corpus_size * init_batch_size);
        int inc_size = (int) (corpus_size * inc_batch_size);
        // The first init_size elements of corpus go to the first batch. The rest are divided into inc_size batches
        // and added to the list.
        RecordTypeCorpus init_batch = new RecordTypeCorpus();
        for (int i = 0; i < init_size; i++) {
            init_batch.add(corpus.get(i));
        }
        logger.debug("Added batch " + batch_count + " with size: " + init_batch.size());
        batches.add(init_batch);
        batch_count++;

        RecordTypeCorpus inc_batch = new RecordTypeCorpus();
        for (int i = init_size; i < corpus_size; i++) {
            inc_batch.add(corpus.get(i));
            if (inc_batch.size() == inc_size) {
                batches.add(inc_batch);
                logger.debug("Added batch " + batch_count + " with size: " + inc_batch.size());
                batch_count++;
                inc_batch = new RecordTypeCorpus();
            }
        }
        if (!inc_batch.isEmpty()) {  // If there are any remaining elements in the last batch, add it to the list.
//            batches.getLast().add(inc_batch);
            // add remaining elements to the last existing batch and not create a new one.
            batches.getLast().addAll(inc_batch);
            logger.debug("Added last remaining elements with size: " + inc_batch.size() + " to the last batch. Updated size: " + batches.getLast().size());
        }
        logger.info("Batches prepared: " + batches.size());
        return batches;
    }


    /**
     * Verifies the model path and creates the directory if it doesn't exist.
     * Called in the constructor.
     * @author: AA
     */
public void verify_model_path(String modelAddress) {
    File modelDir = new File(modelAddress);
    if (!modelDir.exists()) {
        logger.info("Model directory doesn't exist. Creating it now...");
        if (!modelDir.mkdirs()) {
            try {
                throw new IOException("Could not create directory: " + modelDir.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        logger.info("Model directory created: " + modelDir);
        logger.info("double-verifying: " + modelDir.getAbsolutePath());
    } else {
        logger.warn(ANSI_RED + "Model directory already exists (WILL RE-WRITE MODEL): " + modelDir + ANSI_RESET);
    }
}


    public static void main(String[] args) throws IOException, ClassNotFoundException {

        // To run evaluation only, uncomment the following:
//        BabyDSInduction bbds = new BabyDSInduction();
//        Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> ttsResults = bbds.evaluate_model(FOLDS);
//        bbds.print_semanticAcc_results(ttsResults.first(), String.format("Train-Test Split | Ratio: %.2f | ", RATIO));
//        System.out.println();
//        System.out.println(bbds.get_parsingCoverage_results(ttsResults.second(), null));

        // To run full pipeline (training and testing based on parameters defined on top of this class), uncomment the following:
        BabyDSInduction testInduction = new BabyDSInduction();
        testInduction.full_pipeline(FOLDS, SAVE_TO_FILE, modelPath);

        // training only
//        BabyDSInduction bbds = new BabyDSInduction();
//        bbds.train_model(corpusPath + datasetName);
    }

}
