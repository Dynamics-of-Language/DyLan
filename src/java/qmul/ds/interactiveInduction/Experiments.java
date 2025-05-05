package qmul.ds.interactiveInduction;

import edu.stanford.nlp.util.Pair;
import org.apache.log4j.Logger;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.learn.Evaluation;
import qmul.ds.learn.RecordTypeCorpus;
import static qmul.ds.interactiveInduction.BabyDSInduction.mergeFiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;

import java.util.*;
import java.util.stream.Collectors;


public class Experiments {
    private static final Logger logger = Logger.getLogger(Experiments.class);
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RED = "\u001B[31m";

    static final String seedGrammarPath = "resource\\2023-babyds-induction-output\\".replace("\\", File.separator); // The dir that works!
    static final String modelPath = "resource\\2025-babyds-RQ2\\minimal_data\\".replace("\\", File.separator);  //the dir that works!
    static final String rq1path = "resource\\2025-babyds-RQ1\\class2\\".replace("\\", File.separator);
    static final String rq2path = "resource\\2025-babyds-RQ2\\".replace("\\", File.separator);
    String forgettingPath = "resource\\2025-babyds-RQ2\\forgetting\\".replace("\\", File.separator);
    String generalisationPath = "resource\\2025-babyds-RQ2\\generalisation\\".replace("\\", File.separator);
    static final String NTRR_RQ1_CLASS1_PATH = "resource\\2025-babyds-RQ1\\class1\\nn_results\\".replace("\\", File.separator);


    public static final int SEED = 45; // Set a constant seed for reproducibility
    public static final double TRAIN_TEST_RATIO = 0.85;  // Train-Test split ratio (Meaning the x ratio is for train, 1-x is for test)
    public static final boolean SAVE_TO_FILE = true;  // Save the training and testing sets to file
    public static final String CORPUS_NAME = "class2";
    public static final int REPEAT = 1;  // Increments the seed for each repeat
    public static final int N = 3; // TopN actions to use.
    public static final double INIT_BATCH_RATIO = 0.1;  // Size of the initial data batch for evaluation - used in prepare_batch().
    public static final double INC_BATCH_RATIO = 0.1; // Size of the incremental data batches for evaluation - used in prepare_batch().
    public static final boolean SKIP_RETRAINING = true;  // If a model exists, and we don't want to get the "do you want to load it" message.


    /**
     * todo fix doc
     * ATTENTION at this moment, this works on class 1 to 2 (imagine start class = 1, and end class = 2). Should be extended to support more. TODO
     * @param startClass
     * @param endClass
     */
    public void test_generalisation(int startClass, int endClass) {
        BabyDSInduction bds = new BabyDSInduction(generalisationPath);
        File data1 = new File(seedGrammarPath + "class" + startClass + ".txt");
        File data2 = new File(seedGrammarPath + "class" + endClass + ".txt");
        RecordTypeCorpus corpus1 = new RecordTypeCorpus();
        RecordTypeCorpus corpus2 = new RecordTypeCorpus();
        try {
            corpus1.loadCorpus(data1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            corpus2.loadCorpus(data2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // TODO actually get minimum viable data of class 1, and not just train test split.

        // First, grab the minimum and enough data of class 1, and add batches of class 2 to it (and then more batches
        // of class 1, and also a mix of these), and train a model on it:
//        RecordTypeCorpus class1_minimum_data = new RecordTypeCorpus();  //TODO and more because I want to add batches
        // First shuffle class 1 data
        Random random = new Random(SEED);
        Collections.shuffle(corpus1, random);
        List<RecordTypeCorpus> training_batches_class1 = bds.prepare_batches(corpus1);
        int minimum_data_class1_index = 6;  // TODO fix
        RecordTypeCorpus class1_minimum_data = training_batches_class1.get(minimum_data_class1_index);
        // anything after it will be added as batches.
        List<RecordTypeCorpus> class1_batches = training_batches_class1.subList(minimum_data_class1_index + 1, training_batches_class1.size());
        // Select the minimum data of class 1 (based on previous experiments - using indices) and add keep the rest as batches to be added later.

        // train test split class 2
        Pair<RecordTypeCorpus, RecordTypeCorpus> train_test_pair_class2 = bds.train_test_split("class" + endClass, TRAIN_TEST_RATIO, SAVE_TO_FILE, "", SEED);
        RecordTypeCorpus test_data = train_test_pair_class2.second();
        List<RecordTypeCorpus> training_batches_class2 = bds.prepare_batches(train_test_pair_class2.first());

        // Merging here probably has to happen inside a loop, as I need to combine batches of class 1 and 2.
        String mergedCorpusName = "merged_" + startClass + "_" + endClass;  // TODO come up with a better name convention, as I will need a reflection of the batch sizes in the name.
        try {
            //todo make a proper method for the below.
            mergeFiles(new File(seedGrammarPath + train_test_pair_class2.first.corpusName), data2, new File(seedGrammarPath + mergedCorpusName + ".txt")); //todo fix
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        RecordTypeCorpus merged_corpus = new RecordTypeCorpus();
        try {
            merged_corpus.loadCorpus(new File(seedGrammarPath + mergedCorpusName + ".txt")); //todo fix
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // TODO shuffle training data here (so that later on I can show the effect of curriculum learning).
        bds.train_model(seedGrammarPath + mergedCorpusName + ".txt", "model_" + startClass + "_" + endClass, seedGrammarPath);  //todo fix
        // Now that we have both models, evaluate them on the test set of class 1:
        EvalResult ttsResults = bds.evaluate_model(0);

//        evaluate_model(test_data);
        // Now time to train on the batches of class 2 only, and evaluate on the same test set.
        // for batch in training_batches_class2
        for(RecordTypeCorpus batch : training_batches_class2) {
            bds.train_model(batch.corpusName, "", seedGrammarPath);  //todo fix
            EvalResult ttsResults2 = bds.evaluate_model(0);
            // write to file the model.
//            bds.evaluate_model(test_data);
            // write to file the results.
        }
    }


    /**
     * ATTENTION at this moment, this works on class 1 to 2 (imagine start class = 1, and end class = 2). Should be extended to support more. TODO
     * @param startClass
     * @param endClass
     * @param format // F for only first class, S for only second class, M for mix of both -> based on what batches of
     *               data I am adding (to minimal data) during training.
     */
    public void test_forgetting(int startClass, int endClass, String format) {
        logger.info("Initiating forgetting experiments...");
        BabyDSInduction ds = new BabyDSInduction(forgettingPath);
        ds.verify_model_path(forgettingPath);
        logger.debug("BabyDSInduction instance created.");
        File data1 = new File(rq2path + "class" + startClass + ".txt");
        File data2 = new File(rq2path + "class" + endClass + ".txt");
        RecordTypeCorpus corpus1 = new RecordTypeCorpus();
        RecordTypeCorpus corpus2 = new RecordTypeCorpus();
        try {
            corpus1.loadCorpus(data1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.debug("Corpus1 loaded.");
        try {
            corpus2.loadCorpus(data2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.debug("Corpus2 loaded.");
        // TODO actually get minimal data of class 1, and not just train test split.

        // First, grab the minimum and enough data of class 1, and add batches of class 2 to it (and then more batches
        // of class 1, and also a mix of these), and train a model on it:
//        RecordTypeCorpus class1_minimum_data = new RecordTypeCorpus();  //TODO and more because I want to add batches
        // First shuffle class 1 data
        Collections.shuffle(corpus1, new Random(SEED));
        List<RecordTypeCorpus> class1_batches = ds.prepare_batches(corpus1);
        // get the test data
        RecordTypeCorpus test_data = class1_batches.getLast();  // TODO or maybe do train-test split...
        test_data.corpusName = "babyds_test_" + startClass + "_" + test_data.size() + ".txt";
        try {  // todo improve save and load methods to return the object itself.
            test_data.saveCorpus(forgettingPath + test_data.corpusName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // todo probably have to write it to file and later during eval, read it back...
        // pop the last element from the list as it is used for test data.
        class1_batches.removeLast();

        int minimum_data_class1_index = 7;  // TODO fix: calculate this actually in a method
        RecordTypeCorpus class1_minimum_data = RecordTypeCorpus.mergeAllCorpora(class1_batches.subList(0, minimum_data_class1_index)); // todo fix: the first 6 batches are the minimum data, not just the 6th batch.
        logger.info("Class 1 minimum data size: " + class1_minimum_data.size());
        // anything after it will be added as batches.
        List<RecordTypeCorpus> class1_extra_batches = class1_batches.subList(minimum_data_class1_index + 1, class1_batches.size());
        // Select the minimum data of class 1 (based on previous experiments - using indices) and add keep the rest as batches to be added later.
        // todo save these batched datasets to file
        // train test split class 2
//        Pair<RecordTypeCorpus, RecordTypeCorpus> train_test_pair_class2 = bds.train_test_split(corpus2, TRAIN_TEST_RATIO, SAVE_TO_FILE); // todo fix it has t be prepare batches actually!
        List<RecordTypeCorpus> training_batches_class2 = ds.prepare_batches(corpus2);  // TODO Since my classes will have different sizes, do I need to choose my batches not based on percentage but with a fixed length?

        RecordTypeCorpus mergedCorpusS = class1_minimum_data.mergeCorpora(new RecordTypeCorpus());  // basically just class 1 minimal data so making a copy of it.
        if (format.equals("S") || format.equals("B")) {
            // todo can make this a separate method: I need to do the same for batches of class 1 and the mix of class 1 and 2...
            for (RecordTypeCorpus batch : training_batches_class2.subList(0,3)) {  // TODO same thing has to happen with class 1 batches and mix of class 1 and 2 batches.
                mergedCorpusS = mergedCorpusS.mergeCorpora(batch.getSubCorpus(0,40));  // TODO set name as well! + maybe deal with batch size here?
                logger.debug("Current merged corpus size: " + mergedCorpusS.size());
                mergedCorpusS.corpusName = "merged_" + startClass + "_" + endClass + "_" + batch.corpusName;  // maybe add and use a setName method? //todo fix + include batch number in the name
                Collections.shuffle(mergedCorpusS, new Random(SEED));
                mergedCorpusS.corpusName = "babyds_train_" + startClass + "_" + mergedCorpusS.size() + ".txt";  //TODO
                try {  // todo improve save and load methods to return the object itself.
                    mergedCorpusS.saveCorpus(forgettingPath + mergedCorpusS.corpusName);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                ds.train_model(mergedCorpusS, forgettingPath, seedGrammarPath);
//                Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> ttsResults = ds.evaluate_model(0, forgettingPath, "babyds");
//                EvalResult ev = new EvalResult(ttsResults.first, ttsResults.second);
                EvalResult ev = ds.evaluate_model(0, forgettingPath, "babyds", "", N);
                System.out.println("Eval for batch number: " + training_batches_class2.indexOf(batch));
                System.out.println(ev.getParsingCoverageResultsTable("test data size: " + test_data.size()));  //todo add proper info
                System.out.println(ev.getSemanticAccResultsTable("")); /// todo fix
//                EvalResult ttsResults = bds.evaluate_model();
                // write these results to file + the model + the data in a proper folder!

            }
        }
        RecordTypeCorpus mergedCorpusF = class1_minimum_data.mergeCorpora(new RecordTypeCorpus());  // basically just class 1 data so making a copy of it.
        if (format.equals("F") || format.equals("B")) {
            // todo can make this a separate method: I need to do the same for batches of class 1 and the mix of class 1 and 2...
            // todo my batches are separate, therefore this has to add batches to previous ones and not ignore previous ones.
            for (RecordTypeCorpus batch : class1_extra_batches) {  // TODO same thing has to happen with class 1 batches and mix of class 1 and 2 batches.
                mergedCorpusF = mergedCorpusF.mergeCorpora(batch);  // TODO set name as well!
                mergedCorpusF.corpusName = "merged_" + startClass + "_" + endClass + "_" + batch.corpusName;  // maybe add and use a setName method? //todo fix
                // todo save in forgetting dir - later add batch number to the name
                Collections.shuffle(mergedCorpusF, new Random(SEED));
                ds.train_model(mergedCorpusF, forgettingPath, seedGrammarPath); //
//                Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> ttsResults = ds.evaluate_model(0, forgettingPath, "babyds");
//                EvalResult ev = new EvalResult(ttsResults.first, ttsResults.second);
                EvalResult ev = ds.evaluate_model(0, forgettingPath, "babyds", "", N);
                System.out.println(ev.getParsingCoverageResultsTable(""));
                System.out.println(ev.getSemanticAccResultsTable("")); /// todo fix
//                String semanticAcc = EvalResult.getSemanticAccResultsTable(ttsResults.first);
                // write these results to file + the model + the data in a proper folder!
            }
        }
    }


    /**
     * Finds the minimum amount of data that is needed to "have mastered" a class for BabyDS.
     * Workflow:
     * - Split the data into training batches and a development set.
     * - Train a model on the cumulative training batches.
     * - Evaluate the model on the development set.
     * - Repeat the experiment a number of times (with different seeds)
     * LATER we should get an average over the above as a true estimate.
     * todo add fixed batch size support, rather than percentage.
     * todo maybe call this RQ1, or call this in an RQ1 method?
     * todo make directory named RQ1 btw!
     * @param corpusName the data of a class
     * @param repeatCount the number of times to repeat the experiment
     * @param seed the seed for reproducibility (that weill be incremented with repeatCount)
     * @return a map (from seed to a list of results) to then be used in averaging.
     */
    public HashMap<Integer, List<EvalResult>> findMinimumMasteryData(String corpusName, String modelDir, int repeatCount, int seed) {
        BabyDSInduction ds = new BabyDSInduction();
        HashMap<Integer, List<EvalResult>> fullResults = new HashMap<>(); // Return value.

        for (int i = 0; i < repeatCount; i++) {
            RecordTypeCorpus corpus = new RecordTypeCorpus(corpusName);
            try { // todo better load corpus method, so users won't see all these try-catch everytime!
                corpus.loadCorpus(new File(modelDir+corpusName+".txt"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            List<EvalResult> resultInSeed = new ArrayList<>();
            // Split the data into training batches and a development set
            int currentSeed = seed + i;
            Collections.shuffle(corpus, new Random(currentSeed));
            List<RecordTypeCorpus> trainingBatches = ds.prepare_batches(corpus, INIT_BATCH_RATIO, INC_BATCH_RATIO);
            // todo put below in a separate method for tidiness.
            File currentSeedFolder = new File(modelDir + "S" + currentSeed + File.separator);
            if (!currentSeedFolder.exists()) {
                currentSeedFolder.mkdirs();
            } else
                logger.warn("Folder already exists: " + currentSeedFolder.getAbsolutePath());
            RecordTypeCorpus devSet = trainingBatches.getLast();
            devSet.corpusName = corpus.corpusName+"_test";
            try {
                devSet.saveCorpus(currentSeedFolder + File.separator + devSet.corpusName + ".txt");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            trainingBatches.removeLast();
            // Done with the test set, now dealing with training batches.
            int batchCounter = 0;
            // First, save all to file:
            for (RecordTypeCorpus batch : trainingBatches) { // todo double check logic
                //todo make method for this, so it's tidier and smaller here.
                batchCounter++;
                batch.corpusName = corpus.corpusName + "_trainBatch" + batchCounter;
                try {  //todo similar ot the load method: make it cleaner so users don't have to see all this everytime!
                    // Save these to their own folder, by creating it:
                    batch.saveCorpus(currentSeedFolder + File.separator+ batch.corpusName + ".txt"); // TODO apparently this is not working...
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            // Now do the training and eval:
            int currentMergedBatchIndex = 1;
            RecordTypeCorpus cumulativeTrainingData = new RecordTypeCorpus();

            System.out.println(ANSI_GREEN + "******** Seed " + currentSeed + " ********" + ANSI_RESET);
            for (RecordTypeCorpus batch : trainingBatches) {
                cumulativeTrainingData.addAll(batch);
                cumulativeTrainingData.corpusName = corpus.corpusName + "_trainBatch" + currentMergedBatchIndex;  // Todo is it used for anything beside writing results to file? I don't think so.
                String currentFolderName = "S" + currentSeed + "_B" + currentMergedBatchIndex;
                EvalResult batchtResult = trainTestBatch(currentFolderName, modelDir, cumulativeTrainingData, devSet, currentMergedBatchIndex); // todo maybe use currentseeddir so it's all under the seed folder, for tidiness.
                currentMergedBatchIndex++;
                resultInSeed.add(batchtResult);
                fullResults.put(i, resultInSeed);
                addResultsToTSV(currentSeed, batchtResult, modelDir + "fullResults.tsv");
            }
        }
        return fullResults;
    }


    /**
     * Trains and tests a BabyDS model on a batch of data, and saves and returns the results.
     * @param folderName
     * @param modelDir
     * @param trainingBatch
     * @param testingData
     * @param batchNumber
     * @return
     */
    public EvalResult trainTestBatch(String folderName, String modelDir, RecordTypeCorpus trainingBatch, RecordTypeCorpus testingData, int batchNumber) {
        // todo properly test this method
        // make foldername under modelDir and copy the computational action files there:
        String folderPath = modelDir + folderName + File.separator;
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // Copy the comp-action file from current dir (model dir) to this folder:
        File sourceFile = new File(modelDir + "computational-actions.txt");
        File destFile = new File(folder + File.separator + "computational-actions.txt");
        try {
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // save training set and test set to this folder:
        try {
            trainingBatch.saveCorpus(folder + File.separator + "_train.txt");  //todo set their names?
            testingData.saveCorpus(folder + File.separator + "_test.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BabyDSInduction ds = new BabyDSInduction();
        System.out.println(ANSI_CYAN + "******** Batch " + batchNumber + " ********" + ANSI_RESET);
        // If the model exists and skip is true, then skip!
        if (modelExists(folderPath)) {
            System.out.println("Model already exists in " + folderPath);
            if (SKIP_RETRAINING) {
                System.out.println("Skipping training...");
            } else {
                logger.info("Training model...");
                ds.train_model(trainingBatch, folderPath, seedGrammarPath);
            }
        } else {
            logger.info("Training model...");
            ds.train_model(trainingBatch, folderPath, seedGrammarPath);
        }
        EvalResult result = ds.evaluate_model(0, folderPath, "", "", N);
        result.setDatasetNames(trainingBatch.corpusName, testingData.corpusName);
        result.setDatasetSizes(trainingBatch.size(), testingData.size());
        System.out.println("\nEvaluation results for batch: " + batchNumber  + " are:");
        System.out.println(result.getSemanticAccResultsTable(String.format("Batch %d", batchNumber)));
        System.out.println(result.getParsingCoverageResultsTable(""));  //todo add proper info?
        logger.trace(result.getDiagnosticResults());
        return result;
    }


    /**
     * Checks if a model exists in the given directory.
     * @param dir the directory to check
     * @return true if a model exists, false otherwise
     */
    public boolean modelExists (String dir) {
        String[] files = new File(dir).list();
        String x = dir + "lexicon.lex";
        if (files != null) {
            for (String file : files) {
                if (file.startsWith("lexicon.lex")) {
                    logger.info("Previously trained model found in the given directory with name: " + file);
                    x = dir + file;
                }
            }
        }
        if (new File(x).exists()) {
            logger.info("A trained model already exists at: " + dir);
           return true;
        }
        else {
            logger.info("No trained model found at: " + dir);
            return false;
        }
    }


    /**
     * Adds the results from a batch over a seed to a given TSV file.
     * Used for real-time updating of the file (so no over-writing).
     * @param seed training shuffle seed, as part of the data to write.
     * @param results results over a batch.
     * @param resultsFileName the tsv file to update.
     */
    public void addResultsToTSV(int seed, EvalResult results, String resultsFileName) {
        StringBuilder modifiedLines = new StringBuilder();
        for (String line : results.toTSVString().split("\n")) {
            modifiedLines.append(seed).append("\t").append(line).append("\n");
        }
        String newResultLine = modifiedLines.toString();
        try {
            Files.write(
                    new File(resultsFileName).toPath(),
                    (newResultLine).getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Runs all RQ1 experiments.
     * todo expand to all classes.
     */
    public void runRQ1(){
        HashMap<Integer, List<EvalResult>> minDataResults = this.findMinimumMasteryData(CORPUS_NAME, rq1path, REPEAT, SEED);
//        this.allResultsToTSV(minDataResults); //todo move this under write results to file!
        // todo average.
    }


    /**
     * Evaluates NeuralTTR. Provides default values for the targets and predictions file names, for the method below.
     * todo make it parallel
     * @param rootFolderPath
     */
    public HashMap<Integer, List<EvalResult>> evalNeuralTTR(String rootFolderPath){
        return evalNeuralTTR(rootFolderPath, "_neural_targets", "_predictions");
    }


    public HashMap<Integer, List<EvalResult>> evalNeuralTTR(String rootFolderPath, String targetsFileName, String predictionsFileName) {
        HashMap<Integer, List<EvalResult>> fullResults = new HashMap<>();
        String[] train_test_file_names = {"train", "test"};

        File rootFolder = new File(rootFolderPath);
        if (!rootFolder.exists() || !rootFolder.isDirectory()) {
            logger.error("Invalid root folder path: " + rootFolderPath);
            return fullResults;
        }

        File[] subFolders = rootFolder.listFiles(File::isDirectory);
        if (subFolders == null || subFolders.length == 0) {
            logger.error("No subfolders found in the root folder: " + rootFolderPath);
            return fullResults;
        }

        Arrays.stream(subFolders).forEach(subFolder -> {
            // Extract seed from folder name (e.g., "S45_B1" -> 45)
            int seed = extractSeedFromFolderName(subFolder.getName());
            if (seed == -1) {
                logger.warn("Could not extract seed from folder name: " + subFolder.getName());
                return;
            }

            EvalResult neuralTTRResult = new EvalResult();
            int[] sizes = {0, 0}; // {trainingSize, testSize}

            for (String file_name : train_test_file_names) {
                File targetsFile = new File(subFolder, "_"+file_name+targetsFileName + ".txt");
                File predictionsFile = new File(subFolder, "_"+file_name+predictionsFileName + ".txt");
                Evaluation eval = new Evaluation();
                if (!targetsFile.exists() || !predictionsFile.exists()) {
                    logger.warn("Missing targets or predictions file in folder: " + subFolder.getName());
                    continue;
                }
                try {
                    List<String> targets = readTargetFile(targetsFile.toPath().toString());
                    List<String> predictions = Files.lines(predictionsFile.toPath())
                        .filter(line -> !line.trim().isEmpty())
                        .collect(Collectors.toList());

                    if (targets.size() != predictions.size()) {
                        logger.warn("Mismatch in size between targets and predictions in folder: " + subFolder.getName());
                        continue;
                    }

                    processEvaluation(targets, predictions, file_name, sizes, neuralTTRResult);
                    logger.info("Folder: " + subFolder.getName() + " | Results processed successfully");

                } catch (IOException e) {
                    logger.error("Error reading files in folder: " + subFolder.getName() + " | " + e.getMessage());
                } catch (Exception e) {
                    logger.error("Error processing files in folder: " + subFolder.getName() + " | " + e.getMessage());
                }
            }

            neuralTTRResult.setDatasetSizes(sizes[0], sizes[1]);

            // Add result to fullResults map
            synchronized (fullResults) {
                if (!fullResults.containsKey(seed)) {
                    fullResults.put(seed, new ArrayList<>());
                }
                fullResults.get(seed).add(neuralTTRResult);
            }

            // Write individual result to file
            neuralTTRResult.writeResultsToFile(subFolder+File.separator, "");

            // Add results to TSV
            addResultsToTSV(seed, neuralTTRResult, rootFolderPath + "fullResultsNeuralTTR.tsv");
        });

        return fullResults;
    }


    private int extractSeedFromFolderName(String folderName) {
        try {
            // Extract number between "S" and "_B"
            int startIndex = folderName.indexOf('S') + 1;
            int endIndex = folderName.indexOf("_B");
            if (startIndex > 0 && endIndex > startIndex) {
                return Integer.parseInt(folderName.substring(startIndex, endIndex));
            }
        } catch (Exception e) {
            logger.warn("Failed to extract seed from folder name: " + folderName);
        }
        return -1;
    }


    private void processEvaluation(List<String> targets, List<String> predictions,
                                 String file_name, int[] sizes, EvalResult neuralTTRResult) {
        int numParsed = 0;
        int exactMatches = 0;
        int total = targets.size();

        if (file_name.equals("train")) {
            sizes[0] = total;
        } else if (file_name.equals("test")) {
            sizes[1] = total;
        }

        List<TTRRecordType[]> evalList = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            Pair<TTRRecordType, Boolean> target = TTRRecordType.nn2RTfs(TTRRecordType.str2listTargets(targets.get(i)));
            Pair<TTRRecordType, Boolean> prediction = TTRRecordType.nn2RTfs(TTRRecordType.str2listPreds(predictions.get(i)));
            TTRRecordType targetRT = target.first;
            TTRRecordType predictionRT = prediction.first;
            evalList.add(new TTRRecordType[]{predictionRT, targetRT});

            if (prediction.second) {
                numParsed++;
                if (targetRT.subsumes(predictionRT) && predictionRT.subsumes(targetRT)) {
                    exactMatches++;
                }
            }
        }

        List<Float> scores = new Evaluation().precisionRecallMacro(evalList);
        neuralTTRResult.addSemanticAccuracy(1, file_name, scores.get(0)*100, scores.get(1)*100, scores.get(2)*100);
        neuralTTRResult.addParsingCoverage(1, file_name, (double) numParsed / total* 100, (double) exactMatches / total* 100);
        logger.info(String.format("Processed %s | Parsed: %d/%d | Exact Matches: %d/%d",
            file_name, numParsed, total, exactMatches, total));
    }


    public List<String> readTargetFile(String filePath) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (lineNum % 3 == 2 && !line.trim().isEmpty()) {  // Every second non-empty line
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            logger.error("Error reading file: " + filePath + " | " + e.getMessage());
        }
        return lines;
    }


    public void rq2() {
    // TODO should add "G" for generalisation tests, "F" for forgetting tests, and "B" for both forgetting and generalisation tests.
    //todo add also "C" for curriculum learning tests, and "R" for random learning tests.
    }

    public void rq3() {

    }


    public static void main(String[] args) {

        Experiments exp = new Experiments();
//        exp.runRQ1();
        System.out.println(exp.evalNeuralTTR(NTRR_RQ1_CLASS1_PATH));
////        exp.test_forgetting(1, 2, "S");
//        HashMap<Integer, List<EvalResult>> minData = exp.findMinimumMasteryData(CORPUS_NAME, modelPath, REPEAT, SEED);
//        exp.allResultsToTSV(minData);


    }

}
