package qmul.ds.interactiveInduction;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;
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


public class BabyDSInduction {
    private static final Logger logger = Logger.getLogger(BabyDSInduction.class);
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RED = "\u001B[31m";

    static final String corpusPath = "resource\\2023-babyds-induction-output\\".replace("\\", File.separator);
    static final String modelPath = "resource\\2024_babyds_induction\\".replace("\\", File.separator);
    static final String datasetName = "babyds3.txt";

    private static final long SEED = 45; // Set a constant seed for reproducibility
    private static final Random random = new Random(SEED);
    private static final int TOP_N = 4;  // topN learned actions to evaluate
    private static final double RATIO = 0.9;  // Train-Test split ratio
    private static final int FOLDS = 0;  // Number of folds for k-fold cross validation. Use 0 for train-test split.
    private static final boolean SAVE_TO_FILE = true;  // Save the training and testing sets to file

    /**
     * A pipeline for full evaluation:
     * 1. Loads the learned lexical actions (parsing model) for all topNs.
     * 2. Loads both training and testing sets.
     * 3. Evaluates the parsing model on both sets.
     * 4. Returns the calculated results: mainly semantic accuracy and parsing coverage.
     */
    public Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> evaluate_model(int kfcv) {
        HashMap<Integer, HashMap<String, HashMap<String, Double>>> semanticAccuracy = new HashMap<>();
        HashMap<Integer, HashMap<String, ArrayList<Double>>> parsingCoverage = new HashMap<>();  // Should have used double[] instead of ArrayList<Double>... Anyways.
        Evaluation eval = new Evaluation();
        for (int n = 1; n <= TOP_N; n++) {
            logger.info("Loading parser with top-" + n + " learned actions...");
            InteractiveContextParser parser = new InteractiveContextParser(modelPath, n);
            File[] train_test_files = get_corpus_files(kfcv);
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
                for (Pair<Sentence<Word>, TTRRecordType> pair : corpus) {
                    parser.init();  // Restarts parser.
                    Sentence<Word> sentence = pair.first();
                    TTRRecordType goldSem = pair.second();
                    boolean parsed = parser.parseUtterance(new Utterance(sentence));
                    if (parsed) {
                        logger.debug("With top-" + n + " actions parsed: sentence: " + sentence);
                        logger.debug("Gold semantics: " + goldSem);
                        TTRRecordType parsedSem = (TTRRecordType) parser.getState().getCurrentTuple().getSemantics();
                        evalList.add(new TTRRecordType[]{parsedSem, goldSem});
                        logger.debug("Hyp semantics: " + parsedSem);
                        logger.info("Was able to parse: " + sentence);
                        parsedCount++;
                        if (parsedSem.subsumes(goldSem) && goldSem.subsumes(parsedSem)) {
                            exactMatchCount++;
                            logger.info(ANSI_GREEN + "Exact match found for: " + ANSI_RESET + sentence);
                        }
                        else
                            logger.info(ANSI_YELLOW + "No exact match found for: " + ANSI_RESET + sentence);
                    } else {
                        logger.info(ANSI_RED + "Parsing failed for: " + ANSI_RESET + sentence);
                    }
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
//                parsingCoverage.get(n).put(dataset_name, (double) parsedCount / corpus.size() * 100);  // AA MODIFIED FROM parsedCount
                logger.info("Parsing coverage for " + dataset_name + ": " + parsedCount + " out of " + corpus.size());  // AA MODIFIED FROM parsedCount
            }
        }
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
    public void print_parsingCoverage_results(HashMap<Integer, HashMap<String, ArrayList<Double>>> results, String info) {
        String leftAlignFormat = "| %-15s | %10.2f | %11.2f | %10.2f | %11.2f |%n";
        String rowSeparator = "+-----------------+------------+-------------+------------+-------------+";

        System.out.println("====================| Parsing Coverage Results |====================");
        if (info != null)
            System.out.println(info);
        System.out.println("+-----------------+--------------------------+--------------------------+");
        System.out.format("| Actions \\ Data  |           Train          |          Test            |%n");
        System.out.println("|                 +--------------------------+--------------------------+");
         System.out.format("|                 |  Coverage  | Exact Match |  Coverage  | Exact Match | %n");
        System.out.println(rowSeparator);

        for (int topN : results.keySet()) {
            HashMap<String, ArrayList<Double>> coverageScores = results.get(topN);
            ArrayList<Double> trainScores = coverageScores.get("train");
            ArrayList<Double> testScores = coverageScores.get("test");
            System.out.printf(leftAlignFormat, "Top-" + topN, trainScores.get(0), trainScores.get(1), testScores.get(0), testScores.get(1));
        }
        System.out.println(rowSeparator);
    }


    /**
     * Returns the training and testing files from the corpus directory.
     * @param kfcv The k-fold cross validation index. If 0, a train-test data is returned.
     * @return train_test_files An array of two files: training and testing files.
     */
    public File[] get_corpus_files(int kfcv) {
        File[] train_test_files = new File[2];
        File folder = new File(corpusPath);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                logger.trace(file.getName());
                if (kfcv == 0) {
                    logger.trace("Trying to load train-test split files...");
                    if (file.isFile() && file.getName().startsWith("babyds_train")) {
                        train_test_files[0] = file;
                        continue;
                    }
                    if (file.isFile() && file.getName().startsWith("babyds_test"))
                        train_test_files[1] = file;
                } else {
                    logger.trace("Trying to load k-fold cross validation files...");
                    if (file.isFile() && file.getName().startsWith("babyds_kfcv_" + (kfcv-1) + "_train")) {
                        train_test_files[0] = file;
                        continue;
                    }
                    if (file.isFile() && file.getName().startsWith("babyds_kfcv_" + (kfcv-1) + "_test"))
                        train_test_files[1] = file;
                }
            }
            if (train_test_files[0] == null || train_test_files[1] == null)
                logger.error(ANSI_RED + "Couldn't find either training or testing file." + ANSI_RESET);
        } else {
            logger.error(ANSI_RED + "No files found in the corpus directory." + ANSI_RESET);
        }
        return train_test_files;
    }

    /**
     * Splits a generated dataset into training and testing sets based on the given ratio, and saves them to file.
     * @param ratio The ratio of training to testing data.
     * @return A pair of training and testing datasets.
     * @author Arash A.
     */
    public Pair<RecordTypeCorpus, RecordTypeCorpus> train_test_split(double ratio, boolean saveToFile) {
        RecordTypeCorpus corpus = new RecordTypeCorpus();
        try {
            corpus.loadCorpus(new File(corpusPath + datasetName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int size = corpus.size();
        int train_size = (int) (size * ratio);
        int test_size = size - train_size;
        logger.debug("Train size: " + train_size + " | Test size: " + test_size);
        RecordTypeCorpus trainCorpus = new RecordTypeCorpus();
        RecordTypeCorpus testCorpus = new RecordTypeCorpus();
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
            String train_set_name = "babyds_train_" + train_size + ".txt";
            String test_set_name = "babyds_test_" + test_size + ".txt";
            try {  // todo Filenames used to save the split data should be exactly the same as the original, plus  train/test and size. Currently it's just train/test and size. Same for when reading, and for kFold.
                trainCorpus.saveCorpus(corpusPath + train_set_name);
                testCorpus.saveCorpus(corpusPath + test_set_name);
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
    public List<Pair<RecordTypeCorpus, RecordTypeCorpus>> kFoldCrossValidation(int folds, boolean saveToFile) throws IOException {
        List<Pair<RecordTypeCorpus, RecordTypeCorpus>> train_test_pairs = new ArrayList<>();
        RecordTypeCorpus corpus = new RecordTypeCorpus();
        corpus.loadCorpus(new File(corpusPath + datasetName));
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
                trainCorpus.saveCorpus(corpusPath + train_set_name);
                testCorpus.saveCorpus(corpusPath + test_set_name);
            }
            train_test_pairs.add(new Pair<>(trainCorpus, testCorpus));
        }
        return train_test_pairs;
    }


    /**
     * Trains the BabyDS model on a given training data, and saves the top-5 learned lexicon to files.
     * @param trainingDataPath The path to the training data file.
     */
    public void train_model(String trainingDataPath) {
        TTRWordLearner babyDS = new TTRWordLearner(corpusPath);
        String lexiconPath = modelPath + "lexicon.lex";
        try {
            File corpusFile = new File(trainingDataPath);
            babyDS.loadCorpus(corpusFile);
            logger.info("BabyDS training starting...");
            babyDS.learn();

            babyDS.getHypothesisBase().saveLearnedLexicon(lexiconPath, 1);  // Testing if top-1 can be a thing here:
            babyDS.getHypothesisBase().saveLearnedLexicon(lexiconPath, 2);
            babyDS.getHypothesisBase().saveLearnedLexicon(lexiconPath, 3);
            babyDS.getHypothesisBase().saveLearnedLexicon(lexiconPath, 4);
            babyDS.getHypothesisBase().saveLearnedLexicon(lexiconPath, 5);

        } catch(Exception e) {
			e.printStackTrace();
		}
    }


    /**
     * A pipeline for full training and evaluation:
     * @param kFold The number of folds for k-fold cross validation. If 0, a train-test split is performed.
     * @param saveToFile A boolean to save the training and testing sets to file.
     */
    public void full_pipeline(int kFold, boolean saveToFile) {
        if (kFold != 0) {  // The k-fold cross validation scenario.
            logger.info("Performing " + kFold + "-fold Cross Validation...");
            List<HashMap<Integer, HashMap<String, HashMap<String, Double>>>> kfSemAccResults = new ArrayList<>();
            List<HashMap<Integer, HashMap<String, ArrayList<Double>>>> kfParsCvgResults = new ArrayList<>();
            try {
                List<Pair<RecordTypeCorpus, RecordTypeCorpus>> train_test_pairs = kFoldCrossValidation(kFold, saveToFile);
                int i = 0;  // fold index, aka kfcv counter.
                for (Pair<RecordTypeCorpus, RecordTypeCorpus> pair : train_test_pairs) {
                    int train_size = pair.first().size();
                    train_model(corpusPath + "babyds_kfcv_"+i+"_train_"+train_size+".txt");  // currently it doesn't save the kfcv models separately, just overwrites.
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
                print_semanticAcc_results(avgSemAccResults, String.format("%d-fold Cross Validation averaged results", kFold));
                System.out.println();
                print_parsingCoverage_results(avgParsCvgResults, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else { // The simply train-test split scenario.
            logger.info("Performing Train-Test Split training with ratio " + RATIO + " now...");
            Pair<RecordTypeCorpus, RecordTypeCorpus> train_test_pair = train_test_split(RATIO, saveToFile);
            int train_size = train_test_pair.first().size();
            int test_size = train_test_pair.second().size();
            train_model(corpusPath + "babyds_train_" + train_size + ".txt");
            Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> ttsResults = evaluate_model(0);
            print_semanticAcc_results(ttsResults.first(), String.format("Train-Test Split | Ratio: %.2f | Data Sizes: train=%d - test=%d",
                    RATIO, train_size, test_size));
            System.out.println();
            print_parsingCoverage_results(ttsResults.second(), null);
        }
    }


    public static void main(String[] args) throws IOException, ClassNotFoundException {

        // To run evaluation only, uncomment the following:
//        BabyDSInduction bbds = new BabyDSInduction();
//        Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> ttsResults = bbds.evaluate_model(FOLDS);
//        bbds.print_semanticAcc_results(ttsResults.first(), String.format("Train-Test Split | Ratio: %.2f | ", RATIO));
//        System.out.println();
//        bbds.print_parsingCoverage_results(ttsResults.second(), null);

        // To run full pipeline (training and testing based on parameters defined on top of this class), uncomment the following:
//        BabyDSInduction testInduction = new BabyDSInduction();
//        testInduction.full_pipeline(FOLDS, SAVE_TO_FILE);

        // training only
        BabyDSInduction bbds = new BabyDSInduction();
        bbds.train_model(corpusPath + datasetName);
    }

}
