package qmul.ds.interactiveInduction;

import edu.stanford.nlp.util.Pair;
import org.apache.log4j.Logger;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.learn.Evaluation;
import qmul.ds.learn.RecordTypeCorpus;
import qmul.ds.learn.WordHypothesisBase;

import java.io.*;
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

    static final String SEED_GRAMMAR_PATH = "resource\\2023-babyds-induction-output\\".replace("\\", File.separator); // The dir that works!
    static final String SEED_GRAMMAR_PATH_NEW = "resource\\2025-babyds-seeded-induction2\\".replace("\\", File.separator); // The dir that works!

    static final String SEED_GRAMMAR_PATH_RQ2 = "resource\\2025-babyds-seeded-induction2\\".replace("\\", File.separator); // The dir that works for RQ2
    static final String modelPath = "resource\\2025-babyds-RQ2\\minimal_data\\".replace("\\", File.separator);  //the dir that works!
    static final String rq1path = "resource\\2025-babyds-RQ1\\".replace("\\", File.separator);
    static final String RQ1CLASS1HB = "resource\\2025-babyds-RQ1\\class1HB\\".replace("\\", File.separator);
    static final String RQ1CLASS2HB = "resource\\2025-babyds-RQ1\\class2hb\\".replace("\\", File.separator);

    static final String rq2path = "resource\\2025-babyds-RQ2\\".replace("\\", File.separator);
    static final String forgettingPath = "resource\\2025-babyds-RQ2\\first-test\\".replace("\\", File.separator);
    static final String NTR_RQ1_CLASS1_PATH = "resource\\2025-babyds-RQ1\\c1_data\\BDS-TTR\\".replace("\\", File.separator);
    static final String NTR_RQ1_CLASS2_PATH = "resource\\2025-babyds-RQ1\\c2_data\\".replace("\\", File.separator);

    static final String NTR_RQ1_PATH = "resource\\2025-babyds-RQ1\\results_NTR\\".replace("\\", File.separator);
    static final String NN_DATA_OUTPUT_FOLDER_NAME = "NTR-larger-batches-10";
    static final String C1_BDS_TRAINED_MODELS_DIR = "resource\\2025-babyds-RQ1\\class1HB\\".replace("\\", File.separator);



    public static final int SEED = 46; // Set a constant seed for reproducibility
    public static final double TRAIN_TEST_RATIO = 0.85;  // Train-Test split ratio (Meaning the x ratio is for train, 1-x is for test)
    public static final boolean SAVE_TO_FILE = true;  // Save the training and testing sets to file
    public static final String CORPUS_NAME = "class1";
    public static final int REPEAT = 1;  // Increments the seed for each repeat
    public static final int N = 3; // TopN actions to use.

    // Info: BATCH_RATIO for class2: 0.02 | class1: 0.1
    public static final double INIT_BATCH_RATIO = 0.1;  // Size of the initial data batch for evaluation - used in prepare_batch().
    public static final double INC_BATCH_RATIO = 0.1; // Size of the incremental data batches for evaluation - used in prepare_batch().
    public static final int INC_BATCH_SIZE = 1;  // Number of samples to use for each training batch (fixed size, instead of the ratios above)
    public static final boolean USE_FIXED_BATCH_SIZE = true;  // If true, use fixed batch size, otherwise use ratio.
    public static final int RQ2_LAST_BATCH_SIZE = 11;  // Size of the last batch for RQ2 (Therefore the size of forgetting and generalisation test sets)

    public static final boolean SKIP_RETRAINING = true;  // If a model exists, and we don't want to get the "do you want to load it" message.
    public static final int MINIMUM_CLASS1_BATCHES = 3;  // Minimum number of class1 batches to start with in RQ2 tests.
    public static final double EARLY_STOPPING_MIN_THRESHOLD = 99.0;  // Minimum F1 threshold that must be reached before checking delta
    public static final double EARLY_STOPPING_DELTA = 0.01;  // Minimum improvement required between consecutive runs

    private static final int[] SEEDS = {46, 48, 50, 52, 56};


    /**
     * Scans a directory for folders with pattern S{seed}_B{batch} and finds the maximum batch number for each seed.
     * This method dynamically discovers seed-to-batch mappings by examining folder names in the given directory.
     * 
     * @param directoryPath The path to the directory containing folders with S{seed}_B{batch} pattern
     * @return A HashMap mapping each seed (Integer) to its maximum available batch number (Integer)
     * @throws IllegalArgumentException if the directory path is null, empty, or doesn't exist
     */
    public HashMap<Integer, Integer> discoverSeedBatchPairs(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Directory path cannot be null or empty");
        }
        
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("Directory does not exist or is not a directory: " + directoryPath);
        }
        
        HashMap<Integer, Integer> seedBatchMap = new HashMap<>();
        File[] folders = directory.listFiles(File::isDirectory);
        
        if (folders == null) {
            logger.warn("No folders found in directory: " + directoryPath);
            return seedBatchMap;
        }
        
        for (File folder : folders) {
            String folderName = folder.getName();
            
            // Check if folder name matches pattern S{seed}_B{batch}
            if (folderName.matches("S\\d+_B\\d+")) {
                try {
                    // Extract seed number (between 'S' and '_B')
                    int seedStartIndex = folderName.indexOf('S') + 1;
                    int batchStartIndex = folderName.indexOf("_B");
                    int seed = Integer.parseInt(folderName.substring(seedStartIndex, batchStartIndex));
                    
                    // Extract batch number (after '_B')
                    int batch = Integer.parseInt(folderName.substring(batchStartIndex + 2));
                    
                    // Update the maximum batch for this seed
                    seedBatchMap.put(seed, Math.max(seedBatchMap.getOrDefault(seed, 0), batch));
                    
                    logger.debug("Found folder: " + folderName + " -> Seed: " + seed + ", Batch: " + batch);
                    
                } catch (NumberFormatException e) {
                    logger.warn("Failed to parse seed or batch number from folder name: " + folderName);
                } catch (StringIndexOutOfBoundsException e) {
                    logger.warn("Invalid folder name format: " + folderName);
                }
            } else {
                logger.trace("Folder name doesn't match S{seed}_B{batch} pattern: " + folderName);
            }
        }
        
        logger.info("Discovered " + seedBatchMap.size() + " seed-batch pairs from directory: " + directoryPath);
        for (Map.Entry<Integer, Integer> entry : seedBatchMap.entrySet()) {
            logger.info("Seed " + entry.getKey() + " -> Max Batch: " + entry.getValue());
        }
        
        return seedBatchMap;
    }


    /**
     * Checks if early stopping condition is met based on F1 score and coverage criteria.
     * Early stopping is triggered when BOTH metrics are above their thresholds
     * both show small (not considerable) improvement compared to the previous iteration.
     * @param currentF1Score: The F1 score from the current training iteration
     * @param previousF1Score: The F1 score from the previous training iteration (null if first iteration)
     * @param minF1Threshold: Minimum F1 threshold that must be reached before checking delta
     * @param f1Delta: Maximum improvement considered "not considerable" for F1 score
     * @param currentCoverage: The coverage from the current training iteration
     * @param previousCoverage: The coverage from the previous training iteration (null if first iteration)
     * @param minCoverageThreshold: Minimum coverage threshold that must be reached before checking delta
     * @param coverageDelta: Maximum improvement considered "not considerable" for coverage
     * @return true if early stopping should be triggered, false otherwise
     */
    public boolean checkEarlyStoppingCondition(double currentF1Score, Double previousF1Score, double minF1Threshold, double f1Delta, double currentCoverage, Double previousCoverage, double minCoverageThreshold, double coverageDelta) {
        // First check if BOTH metrics are above their minimum thresholds
        if (currentF1Score < minF1Threshold || currentCoverage < minCoverageThreshold) {
            if (currentF1Score < minF1Threshold) {
                System.out.printf("F1 score %.3f below minimum threshold %.3f, continuing training%n",
                    currentF1Score, minF1Threshold);
            }
            if (currentCoverage < minCoverageThreshold) {
                System.out.printf("Coverage %.3f below minimum threshold %.3f, continuing training%n",
                    currentCoverage, minCoverageThreshold);
            }
            return false;
        }
        
        // If we have perfect results on both metrics, stop early
        if (currentF1Score == 100.0 && currentCoverage == 100.0) {
            System.out.printf("Early stopping triggered: Got perfect results with coverage %.3f and f1 score %.3f%n", currentCoverage, currentF1Score);
            return true;
        }
        
        // If we don't have previous scores, continue training
        if (previousF1Score == null || previousCoverage == null) {
            logger.debug("No previous scores available, continuing training");
            return false;
        }

        // Calculate improvements
        double f1Improvement = currentF1Score - previousF1Score;

        double coverageImprovement = currentCoverage - previousCoverage;
        
        // Check if both improvements are not considerable (absolute value <= delta)
        boolean f1ImprovementNotConsiderable = Math.abs(f1Improvement) <= f1Delta;
        boolean coverageImprovementNotConsiderable = Math.abs(coverageImprovement) <= coverageDelta;
        
        if (f1ImprovementNotConsiderable && coverageImprovementNotConsiderable) {
            System.out.printf("Early stopping triggered: F1 improvement %.3f (abs) below delta threshold %.3f and Coverage improvement %.3f (abs) below delta threshold %.3f%n",
                Math.abs(f1Improvement), f1Delta, Math.abs(coverageImprovement), coverageDelta);
            return true;
        } else {
            System.out.printf("Continuing training: F1 improvement %.3f, Coverage improvement %.3f%n",
                f1Improvement, coverageImprovement);
            return false;
        }
    }


    /** Used in the mergeFiles method below.
     * @param file The file to read from.
     * @param writer The writer to write to.
     * @throws IOException If an I/O error occurs.
     */
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
           // Read the first file and write to output
           writeContent(file1, writer);
           // Read the second file and write to output
           writeContent(file2, writer);
       }
   }


   public List<List<RecordTypeCorpus>> getRQ2Data(int startClass, int endClass, int seed, String savePath) {
       return getRQ2Data(rq2path, startClass, endClass, seed, savePath);
   }

   public List<List<RecordTypeCorpus>> getRQ2Data(String rootDirectory, int sourceClassId, int targetClassId, int seed, String savePath) {
       String sourceClassFileName = "class" + sourceClassId;
       String targetClassFileName = "class" + targetClassId;
       return getRQ2Data(rootDirectory, sourceClassFileName, targetClassFileName, seed, savePath);
   }


    /**
     * Prepares RQ2 data.
     * Reads in class 1 and class 2 data, creates batches from each, and uses the last batch of class1 as the forgetting
     * test set and last batch of class 2 as the generalisation test set. Returns all of these.
     * @param rootDirectory the root directory of the data
     * @param sourceClassFileName name of the first class to be used (inclusive)
     * @param targetClassFileName name of the last class to be used (inclusive)
     * @param seed the seed for reproducibility (used in shuffling)
     * @param savePath the path to save the data to
     * @return a list of lists of RecordTypeCorpus (size=4) in this order:
     *          1- class1 batches,
     *          2- class2 batches,
     *          3- forgetting test set (size=1),
     *          4- generalisation test set (size=1).
     * TODO replace class1 and class2 with sourceClass and targetClass in variables and in naming!
     */
    public List<List<RecordTypeCorpus>> getRQ2Data(String rootDirectory, String sourceClassFileName, String targetClassFileName, int seed, String savePath) {
    List<List<RecordTypeCorpus>> rq2Data = new ArrayList<>();
        File corpusFile1 = new File(rootDirectory + sourceClassFileName + ".txt");
        File corpusFile2 = new File(rootDirectory + targetClassFileName + ".txt");
        RecordTypeCorpus corpus1 = new RecordTypeCorpus();
        RecordTypeCorpus corpus2 = new RecordTypeCorpus();
        try {
            corpus1.loadCorpus(corpusFile1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.trace("Corpus1 loaded.");
        try {
            corpus2.loadCorpus(corpusFile2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.trace("Corpus2 loaded.");

        // First deal with class 1 batches
        Collections.shuffle(corpus1, new Random(seed));
        List<RecordTypeCorpus> sourceClassBatches;

        if (USE_FIXED_BATCH_SIZE) {
            sourceClassBatches = BabyDSInduction.prepare_batches(corpus1, INC_BATCH_SIZE, INC_BATCH_SIZE, RQ2_LAST_BATCH_SIZE);
        } else {
            sourceClassBatches = BabyDSInduction.prepare_batches(corpus1, INIT_BATCH_RATIO, INC_BATCH_RATIO);
        }
        // get the test data
        RecordTypeCorpus forgetting_test_data = sourceClassBatches.get(sourceClassBatches.size()-1);
        forgetting_test_data.corpusName = "forgetting_test_" + sourceClassFileName + "_" + forgetting_test_data.size() + ".txt";
        try {
            forgetting_test_data.saveCorpus(savePath + forgetting_test_data.corpusName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sourceClassBatches.remove(sourceClassBatches.size()-1);  // pop the last element from the list as it is used for test data.

        // Now deal with class 2 batches
        Collections.shuffle(corpus2, new Random(seed));
        List<RecordTypeCorpus> targetClassBatches;
        if (USE_FIXED_BATCH_SIZE) {
            targetClassBatches = BabyDSInduction.prepare_batches(corpus2, INC_BATCH_SIZE, INC_BATCH_SIZE, RQ2_LAST_BATCH_SIZE);
        } else {
            targetClassBatches = BabyDSInduction.prepare_batches(corpus2, INIT_BATCH_RATIO, INC_BATCH_RATIO);
        }
        // get the test data
        RecordTypeCorpus generalisation_test_data = targetClassBatches.get(targetClassBatches.size()-1);
        generalisation_test_data.corpusName = "generalisation_test_" + targetClassFileName + "_" + generalisation_test_data.size() + ".txt";
        try {
            generalisation_test_data.saveCorpus(savePath + generalisation_test_data.corpusName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        targetClassBatches.remove(targetClassBatches.size()-1);  // pop the last element from the list as it is used for test data.

        // At the end add these to the list and return.
        rq2Data.add(sourceClassBatches);
        rq2Data.add(targetClassBatches);
        rq2Data.add(Collections.singletonList(forgetting_test_data));  //todo what is this? is it correct?
        rq2Data.add(Collections.singletonList(generalisation_test_data));

        return rq2Data;
    }


    /**
     * Tests a BabyDS model for forgetting and generalisation on lower and higher classes.
     * ATTENTION at this moment, this works on class 1 to 2 (imagine start class = 1, and end class = 2). Should be extended to support more.
     * @param seedModelsPath path to the seed models.
     * @param sourceClassName name of the first class to be used
     * @param targetClassName name of the last class to be used
     * @param setting Specifies batches from what class of data should be added. Options: "first", "second", "both".
     * @param withCurriculum specifies if curriculum learning is used or not (sorts data or shuffles it based on the boolean value provided).
     * @param seed the seed for reproducibility (used in shuffling)
     * @param topN the top N actions to use.
     * @return a RQ2SeedResult object containing the results of the generalisation and forgetting experiments.
     */
    public RQ2SeedResult testGeneralisationForgetting(String seedModelsPath, String sourceClassName, String targetClassName, String setting, boolean withCurriculum, int seed, int topN) {
        logger.info("Initiating geenralisation and forgetting experiments...");
        RQ2SeedResult results = new RQ2SeedResult();
        List<List<RecordTypeCorpus>> rq2Data = getRQ2Data(forgettingPath, sourceClassName, targetClassName, seed, forgettingPath);  //TODO Fix to proper paths
        List<RecordTypeCorpus> sourceClass_batches = rq2Data.get(0);
        List<RecordTypeCorpus> targetClass_batches = rq2Data.get(1);
        RecordTypeCorpus forgetting_test_data = rq2Data.get(2).get(0);
        RecordTypeCorpus generalisation_test_data = rq2Data.get(3).get(0);

        // TODO fix HERE
        // targetClass_batches = targetClass_batches.subList(0, 1);  // This is so that we can have the first extra batch being empty, and then start with min data below (in the cumulativeTrainingData).
//        targetClass_batches.add(0, new RecordTypeCorpus());  // This is so that we can have the first extra batch being empty, and then start with min data below (in the cumulativeTrainingData).
        // First, grab the minimum and enough data of class 1 (number of batches to reach mastery from RQ1 basically),
        // and add batches of class1 or class2 or their mix, and train a model on it:
        int min_data_source_class_index = MINIMUM_CLASS1_BATCHES;  // TODO is it ok that this is from the visuals we got (6 was good enough)?

        RecordTypeCorpus sourceClass_minimum_data = RecordTypeCorpus.mergeAllCorpora(sourceClass_batches.subList(0, min_data_source_class_index)); // This operation is exclusive.
        logger.info("Class 1 minimum data size: " + sourceClass_minimum_data.size());
        // anything after it will be added as batches.
        List<RecordTypeCorpus> sourceClass_extra_batches = sourceClass_batches.subList(min_data_source_class_index + 1, sourceClass_batches.size());
        // sourceClass_extra_batches.add(0,new RecordTypeCorpus());  // This is so that we can have the first extra batch being empty, and then start with min data below (in the cumulativeTrainingData).

        // Now based on the setting, create the "training set".
        RecordTypeCorpus cumulativeTrainingData = sourceClass_minimum_data.mergeCorpora(new RecordTypeCorpus());  // basically just class 1 minimal data so making a copy of it.
        
        // TODO Have to write the below to file.
        List<RecordTypeCorpus> trainingDataBatches = new ArrayList<>();
        switch (setting) {
            case "first": {
                trainingDataBatches = sourceClass_extra_batches;
                logger.debug("source class extra batches size: " + sourceClass_extra_batches.size());
                break;
            }
            case "second": {
                trainingDataBatches = targetClass_batches;
                logger.debug("Target class batches size: " + targetClass_batches.size());
                break;
            }
            case "both": {
                // Create batches that are half class1 and half class2
                int numBatches = Math.min(sourceClass_extra_batches.size(), targetClass_batches.size());
                for (int i = 0; i < numBatches; i++) {
                    RecordTypeCorpus sourceClass_batch = sourceClass_extra_batches.get(i);
                    RecordTypeCorpus targetClass_batch = targetClass_batches.get(i);
                    RecordTypeCorpus mergedBatch = sourceClass_batch.mergeCorpora(targetClass_batch);
                    trainingDataBatches.add(mergedBatch);
                }
                logger.debug("Both classes batches size: " + trainingDataBatches.size());
                break;
            }
            default: logger.error("Invalid setting provided. Please use 'first', 'second' or 'both'.");
        }

        int currentMergedBatchIndex = min_data_source_class_index; // TODO double-check
        System.out.println(ANSI_GREEN + "******** Seed: " + seed + " | currentMergedBatch: " + currentMergedBatchIndex + " | setting: " + setting + "********" + ANSI_RESET);
        WordHypothesisBase previousModel = null;

        for (RecordTypeCorpus batch : trainingDataBatches) {
            RecordTypeCorpus nonCumulativeTrainingData = new RecordTypeCorpus(); // Resetting for non-cumulative.
            nonCumulativeTrainingData.addAll(batch);
            cumulativeTrainingData.addAll(batch); //TODO NEWLY added by AA: IT'S SIMILAR TO RQ1, but wasn't here. To be tested.
//                    cumulativeTrainingData = cumulativeTrainingData.mergeCorpora(batch);//.getSubCorpus(0, 40));  // TODO set name as well! + maybe deal with batch size here?
            logger.debug("Current merged corpus size: " + cumulativeTrainingData.size());
            if (withCurriculum) {
                // TODO implement curriculum learning sorting here.
            } else {
                Collections.shuffle(cumulativeTrainingData, new Random(seed));
            }
            cumulativeTrainingData.corpusName = "merged_" + sourceClassName + "_" + targetClassName + "_" + batch.corpusName;  // todo maybe add and use a setName method? //todo fix + include batch number in the name
            String currentFolderName = "S" + seed + "_B" + currentMergedBatchIndex;  // TODO Update to include setting here (cumulative/not + first/second/both)
            String currentOutputPath = forgettingPath + currentFolderName + File.separator; // TODO fix this.
            
            //TODO Missing dev test here. THINK IT CAN COME INTO PLAY.
            Pair<EvalResult, WordHypothesisBase> generalisationOutput = trainTestBatchHBSeeded(seedModelsPath, currentFolderName, forgettingPath, nonCumulativeTrainingData, generalisation_test_data, currentMergedBatchIndex, "_gens", previousModel, SEED_GRAMMAR_PATH_RQ2, topN, currentOutputPath, seed);
            EvalResult generalisationResult = generalisationOutput.first;
//            previousModel = generalisationOutput.second;
            logger.warn("Cumulative training set will overwrite the non-cumulative one in this process below, but no problem.");

            Pair<EvalResult, WordHypothesisBase> forgettingOutput = trainTestBatchHBSeeded(seedModelsPath, currentFolderName, forgettingPath, cumulativeTrainingData, forgetting_test_data, currentMergedBatchIndex, "_forg", previousModel, SEED_GRAMMAR_PATH_RQ2, topN, currentOutputPath, seed);
            EvalResult forgettingResult = forgettingOutput.first;
            previousModel = generalisationOutput.second;
            logger.info("Eval for batch number: ");
            logger.info(generalisationResult.getParsingCoverageResultsTable("test data size: "));  //todo add proper info
            logger.info(generalisationResult.getSemanticAccResultsTable("")); // todo fix
            logger.info(forgettingResult.getParsingCoverageResultsTable("test data size: "));  //todo add proper info
            logger.info(forgettingResult.getSemanticAccResultsTable("")); // todo fix

            currentMergedBatchIndex++;
            results.getGeneralisationResults().add(generalisationResult);
            results.getForgettingResults().add(forgettingResult);
            //TODO fix write to file/ relevant data structure here (with RQ1)
        }
        return results;
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
            RecordTypeCorpus devSet = trainingBatches.get(trainingBatches.size()-1);
            devSet.corpusName = corpus.corpusName+"_test";
            try {
                devSet.saveCorpus(currentSeedFolder + File.separator + devSet.corpusName + ".txt");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            trainingBatches.remove(trainingBatches.size()-1);
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
            // RecordTypeCorpus cumulativeTrainingData = new RecordTypeCorpus();
            WordHypothesisBase previousModel = null;
            Double previousF1Score = null;  // Track previous F1 score for early stopping
            Double previousCoverage = null;  // Track previous coverage for early stopping
            System.out.println(ANSI_GREEN + "******** Seed " + currentSeed + " ********" + ANSI_RESET);
            for (RecordTypeCorpus batch : trainingBatches) {
                RecordTypeCorpus cumulativeTrainingData = new RecordTypeCorpus(); // Moved it here to have it reset (non-cumulative).
                cumulativeTrainingData.addAll(batch);
                cumulativeTrainingData.corpusName = corpus.corpusName + "_trainBatch" + currentMergedBatchIndex;  // Todo is it used for anything beside writing results to file? I don't think so.
                String currentFolderName = "S" + currentSeed + "_B" + currentMergedBatchIndex;
                // Here, cumulative batch shouldn't be used and only the new current batch is necessary + has to pass previous model as hb.
                // TODO do a if-else for the first batch so I can pass no HB.

//                if (currentMergedBatchIndex == 1) {
//                    batchResult = trainTestBatchwithHB(currentFolderName, modelDir, cumulativeTrainingData, testSet, currentMergedBatchIndex, "", ""); // todo maybe use currentseeddir so it's all under the seed folder, for tidiness.
//                } else {
//                    String previousModelDir = modelDir + "S" + currentSeed + "_B" + (currentMergedBatchIndex-1);
//                    batchResult = trainTestBatchwithHB(currentFolderName, modelDir, cumulativeTrainingData, testSet, currentMergedBatchIndex, "", previousModelDir);
//                }
                Pair<EvalResult, WordHypothesisBase> trainOutput = trainTestBatchWithHB(currentFolderName, modelDir, cumulativeTrainingData, devSet, currentMergedBatchIndex, "", previousModel);
                EvalResult batchResult = trainOutput.first;
                previousModel = trainOutput.second;
                
                // Get current F1 score for early stopping check
                double currentF1Score = batchResult.getTestAccuracy(N, "f1");
                double currentCoverage = batchResult.getTestCoverage(N);
                
                // Check early stopping condition
                if (checkEarlyStoppingCondition(currentF1Score, previousF1Score, EARLY_STOPPING_MIN_THRESHOLD, EARLY_STOPPING_DELTA, currentCoverage, previousCoverage, EARLY_STOPPING_MIN_THRESHOLD, EARLY_STOPPING_DELTA)) {
                    logger.info(String.format("Early stopping triggered at batch %d for seed %d. F1 score: %.3f", 
                        currentMergedBatchIndex, currentSeed, currentF1Score));
                    resultInSeed.add(batchResult);
                    fullResults.put(i, resultInSeed);
                    addResultsToTSV(currentSeed, batchResult, modelDir + "fullResultsHB.tsv");
                    break;  // Exit the training loop
                }
                
                currentMergedBatchIndex++;
                resultInSeed.add(batchResult);
                fullResults.put(i, resultInSeed);
                addResultsToTSV(currentSeed, batchResult, modelDir + "fullResultsHB.tsv");
                
                // Update previous F1 score for next iteration
                previousF1Score = currentF1Score;
                previousCoverage = currentCoverage;
            }
        }
        return fullResults;
    }


        /**
     * Creates a neural parsing corpus from the given corpus name and model address -> Data must have been generated before (by the generateDataFolders method).
     * Called by convertCurrentCorpusToNN.
     * Does this in a paralleled way.
     * @param corpusName The name of the corpus to read (without .txt).
     * @param modelAddress The main directory (to read the corpus and save the model in).
     * todo move to their own class I think.
     */
    public void makeNeuralParsingCorpus(String corpusName, String modelAddress, String savePath) {
        RecordTypeCorpus corpus = new RecordTypeCorpus();
        try {
            corpus.loadCorpus(new File(modelAddress + File.separator + corpusName+".txt"));
            // Extract all TTRRecordTypes from corpus pairs
            List<TTRRecordType> rts = corpus.stream()
                .map(Pair::second)
                .collect(Collectors.toList());

            // Process RTTypes in parallel using embeddedRT2NN
            List<String> nnReprs = rts.parallelStream()
                .map(TTRRecordType::embeddedRT2NNList)
                .collect(Collectors.toList());

            logger.info("Processed " + nnReprs.size() + " record types into neural representations");
            // TODO: Save or further process the neural representations
            //TODO move this out of the try block
            if (savePath.isEmpty()) {
                savePath = modelAddress;
            }
            // Making sure it exists
            File saveDir = new File(savePath);
            if (!saveDir.exists()) {
                if (saveDir.mkdirs()) {
                    logger.info("Directory created: " + savePath);
                } else {
                    logger.error("Failed to create directory: " + savePath);
                    throw new IOException("Could not create directory: " + savePath);
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(savePath + File.separator + corpusName + "_neural_targets"+".txt"))) { // + idx was here.
                for (int i = 0; i < corpus.size(); i++) {
                    writer.write(corpus.get(i).first().toString());
                    writer.newLine();
//                    writer.write(String.join(" ", nnReprs.get(i)));
                    writer.write(nnReprs.get(i));
                    writer.newLine();
                    writer.newLine();
                }
                logger.info("Saved neural representations to: " + savePath + corpusName + "_neural_targets"+".txt");  // + idx was here.
            } catch (IOException e) {
                logger.error("Error saving neural representations: " + e.getMessage());
            }

        } catch (IOException e) {
            logger.error("Error loading corpus: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


    /**
     * Reads in generated corpora and converts them to neural representations.
     * Calls makeNeuralParsingCorpus for each corpus.
     * Basically looping over folders with the above method.
     * @param rootFolder The root folder where the converted corpora are stored.
     */
    public void convertCurrentCorporaToNN(String rootFolder) {
        File root = new File(rootFolder);
        File[] folders = root.listFiles(File::isDirectory);
        if (folders != null) {
            for (File folder : folders) {
                String folderName = folder.getName();
                if (folderName.contains("B")) {  // AA trick to read the desired (batch) folders
                    File[] files = folder.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            String fileName = file.getName();
                            if (fileName.startsWith("_tr")) {  // Train file
                                makeNeuralParsingCorpus("_train", folder.getAbsolutePath(), rootFolder+ NN_DATA_OUTPUT_FOLDER_NAME +File.separator+folderName);
                            } else if (fileName.startsWith("_te")) {  // Test file
                                makeNeuralParsingCorpus("_test", folder.getAbsolutePath(), rootFolder+ NN_DATA_OUTPUT_FOLDER_NAME +File.separator+folderName);
                            } else if (fileName.startsWith("_dev")) { // Dev file
                                makeNeuralParsingCorpus("_dev", folder.getAbsolutePath(), rootFolder+ NN_DATA_OUTPUT_FOLDER_NAME +File.separator+folderName);
                            } else {
                                logger.trace("File " + file.getName() + " does not start with _train or _test. Skipping...");
                            }
                        }
                    }
                }
            }
        } else {
            logger.error("No folders found in the root directory: " + rootFolder);
        }
    }


    /**
     * Generates data with **cumulative** training batches for each seed and batch combination in their own folders, in [sent, ttr sem] format.
     * REMINDER: for neural parsing data, convertCurrentCorpusToNN in BabyDSInduction has to be called on the root directory (after a call to this method!).
     * Mainly made for NeuralTTR data generation because I didn't want to wait for debugging BabyDS!
     * Creates separate folders for each batch with naming pattern "S{seed}_B{batchNumber}" and saves
     * cumulative training data (each batch contains all previous batches plus the current one).
     * @param corpusName: The name of the corpus file (without extension) to load for training
     * @param rootDir: The root directory path where batch folders and data will be saved - corpus (with name corpusName) should be in this directory.
     * @param repeatCount: The number of experimental repeats to perform with different seeds
     * @param seed: The base random seed for reproducibility (incremented for each repeat)
     * @author: AA
     */
    public void generateDataFolders(String corpusName, String rootDir, int repeatCount, int seed, double initBatchRatio, double incBatchRatio) {
        BabyDSInduction ds = new BabyDSInduction();
        for (int i = 0; i < repeatCount; i++) {
            RecordTypeCorpus corpus = new RecordTypeCorpus(corpusName);
            try { // todo better load corpus method, so users won't see all these try-catch everytime!
                corpus.loadCorpus(new File(rootDir + corpusName + ".txt"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // Split the data into training batches and a development set
            int currentSeed = seed + i;
            Collections.shuffle(corpus, new Random(currentSeed));
            List<RecordTypeCorpus> trainingBatches = ds.prepare_batches(corpus, initBatchRatio, incBatchRatio);
            
            // Extract test set (last batch) and dev set (second-to-last batch) and save them to main seed folder
            File currentSeedFolder = new File(rootDir + "S" + currentSeed + File.separator);
            if (!currentSeedFolder.exists()) {
                currentSeedFolder.mkdirs();
            } else
                logger.warn("Folder already exists: " + currentSeedFolder.getAbsolutePath());
            
            RecordTypeCorpus testSet = trainingBatches.get(trainingBatches.size() - 1);
            testSet.corpusName = corpus.corpusName + "_test";
            RecordTypeCorpus devSet = trainingBatches.get(trainingBatches.size() - 2);
            devSet.corpusName = corpus.corpusName + "_dev";
            
            try {
                testSet.saveCorpus(currentSeedFolder + File.separator + testSet.corpusName + ".txt");
                devSet.saveCorpus(currentSeedFolder + File.separator + devSet.corpusName + ".txt");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            trainingBatches.remove(trainingBatches.size() - 1); // Remove test set
            trainingBatches.remove(trainingBatches.size() - 1); // Remove dev set
            
            // Now create cumulative batches in separate folders
            int batchNumber = 1;
            RecordTypeCorpus cumulativeTrainingData = new RecordTypeCorpus();
            
            for (RecordTypeCorpus batch : trainingBatches) {
                // Add current batch to cumulative data
                cumulativeTrainingData.addAll(batch);
                cumulativeTrainingData.corpusName = corpus.corpusName + "_cumulativeBatch" + batchNumber;
                
                // Create folder for this batch with naming pattern "S{seed}_B{batchNumber}"
                String currentFolderName = "S" + currentSeed + "_B" + batchNumber;
                File batchFolder = new File(rootDir + currentFolderName + File.separator);
                boolean folderExisted = batchFolder.exists();
                if (!batchFolder.exists()) {
                    batchFolder.mkdirs();
                    logger.info("Created new batch folder: " + batchFolder.getAbsolutePath());
                } else {
                    logger.info("Batch folder already exists, will overwrite data files: " + batchFolder.getAbsolutePath());
                }
                
                // Copy computational-actions.txt to batch folder if it exists
                File sourceFile = new File(rootDir + "computational-actions.txt");
                if (sourceFile.exists()) {
                    File destFile = new File(batchFolder + File.separator + "computational-actions.txt");
                    try {
                        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        if (folderExisted && destFile.exists()) {
                            logger.debug("Overwritten computational-actions.txt in batch folder");
                        }
                    } catch (IOException e) {
                        logger.warn("Failed to copy computational-actions.txt to batch folder: " + e.getMessage());
                    }
                }
                
                // Save cumulative training data, dev set, and test set to this batch folder
                try {
                    cumulativeTrainingData.saveCorpus(batchFolder + File.separator + "_train.txt");
                    devSet.saveCorpus(batchFolder + File.separator + "_dev.txt");
                    testSet.saveCorpus(batchFolder + File.separator + "_test.txt");
                    if (folderExisted) {
                        logger.debug("Overwritten training, dev, and test data files in batch folder: " + currentFolderName);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to save corpus files to batch folder: " + e.getMessage());
                }
                
                batchNumber++;
            }
        }
    }


    /**
     * Conducts Research Question 1 (RQ1) experiments using Hypothesis Base (HB) incremental learning.
     * This method trains BabyDS models incrementally on data batches, where each new model builds upon
     * the previous one (hypothesis base), and evaluates performance with early stopping mechanisms.
     *
     * The workflow for each repeat includes:
     * 1. Load and shuffle corpus data with a deterministic seed
     * 2. Split data into training batches, development set (second-to-last batch), and test set (last batch)
     * 3. Train models incrementally on each batch using the previous model as hypothesis base
     * 4. Evaluate each trained model on both development and test sets
     * 5. Apply early stopping based on development set F1 score improvement threshold
     * 6. Report final results on test set and save all results to TSV files for analysis
     *
     * @param corpusName: The name of the corpus file (without extension) to load for training
     * @param modelDir: The root directory path where models, batches, and results will be saved
     * @param repeatCount: The number of experimental repeats to perform with different seeds
     * @param seed: The base random seed for reproducibility (incremented for each repeat)
     * @return A HashMap mapping repeat index to list of EvalResult objects from each training batch
     */
    public HashMap<Integer, List<EvalResult>> rq1HB(String corpusName, String modelDir, int repeatCount, int seed) {
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

            // Extract test set (last batch) and dev set (second-to-last batch)
            RecordTypeCorpus testSet = trainingBatches.get(trainingBatches.size()-1);
            testSet.corpusName = corpus.corpusName+"_test";
            RecordTypeCorpus devSet = trainingBatches.get(trainingBatches.size()-2);
            devSet.corpusName = corpus.corpusName+"_dev";
            // To save dev and test set in the main "all data" folder
            try {
                testSet.saveCorpus(currentSeedFolder + File.separator + testSet.corpusName + ".txt");
                devSet.saveCorpus(currentSeedFolder + File.separator + devSet.corpusName + ".txt");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Remove both test and dev sets from training batches (works in-place)
            trainingBatches.remove(trainingBatches.size()-1); // Remove test set
            trainingBatches.remove(trainingBatches.size()-1); // Remove dev set
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
            WordHypothesisBase previousModel = null;
            Double previousF1Score = null;  // Track previous F1 score for early stopping
            Double previousCoverage = null;  // Track previous coverage for early stopping
            System.out.println(ANSI_GREEN + "******** Seed " + currentSeed + " ********" + ANSI_RESET);
            for (RecordTypeCorpus batch : trainingBatches) {
                RecordTypeCorpus nonCumulativeTrainingData = new RecordTypeCorpus(); // reset for non-cumulative
                nonCumulativeTrainingData.addAll(batch);
                cumulativeTrainingData.addAll(batch);
                nonCumulativeTrainingData.corpusName = corpus.corpusName + "_trainBatch" + currentMergedBatchIndex;  // Todo is it used for anything beside writing results to file? I don't think so.
                cumulativeTrainingData.corpusName = corpus.corpusName + "_trainBatch" + currentMergedBatchIndex;  // Todo is it used for anything beside writing results to file? I don't think so.

                String currentFolderName = "S" + currentSeed + "_B" + currentMergedBatchIndex;

                // Evaluate on both dev set (for early stopping) and test set (for final results)
                Pair<EvalResult, WordHypothesisBase> devOutput = trainTestBatchWithHB(currentFolderName, modelDir, nonCumulativeTrainingData, devSet, currentMergedBatchIndex, "_dev", previousModel);
                EvalResult devResult = devOutput.first;
                previousModel = devOutput.second;

                // IMPORTANT NOTE: cumulative training set will overwrite the non-cumulative one in this process below, but no problem.
                logger.warn("Cumulative training set will overwrite the non-cumulative one in this process below, but no problem.");

                // Evaluate on test set using the same trained model (reminder: won't train the model again)
                Pair<EvalResult, WordHypothesisBase> testOutput = trainTestBatchWithHB(currentFolderName, modelDir, cumulativeTrainingData, testSet, currentMergedBatchIndex, "", previousModel);
                EvalResult testResult = testOutput.first;

                // Get dev set F1 score for early stopping check (based on top-1 actions)
                 double currentDevF1Score = devResult.getTestAccuracy(1, "f1");
                 double currentDevCoverage = devResult.getTestCoverage(1);
//                double currentDevF1Score = devResult.getTestEM(1); // So it is actually a bad variable name since it's em and not f1 anymore.

                // Check early stopping condition based on dev set performance
                if (checkEarlyStoppingCondition(currentDevF1Score, previousF1Score, EARLY_STOPPING_MIN_THRESHOLD, EARLY_STOPPING_DELTA, currentDevCoverage, previousCoverage, EARLY_STOPPING_MIN_THRESHOLD, EARLY_STOPPING_DELTA)) {
                    System.out.printf("Early stopping triggered at batch %d for seed %d. Dev F1 score: %.3f%n",
                        currentMergedBatchIndex, currentSeed, currentDevF1Score);
                    resultInSeed.add(testResult); // Add test result to final results
                    fullResults.put(i, resultInSeed);
                    addResultsToTSV(currentSeed, testResult, modelDir + "fullResultsHB.tsv");
                    break;  // Exit the training loop
                }

                currentMergedBatchIndex++;
                resultInSeed.add(testResult); // Add test result to final results
                fullResults.put(i, resultInSeed);
                addResultsToTSV(currentSeed, testResult, modelDir + "fullResultsHB.tsv");

                // Update previous dev F1 score for next iteration (for early stopping)
                previousF1Score = currentDevF1Score;
                previousCoverage = currentDevCoverage;
            }
        }
        return fullResults;
    }


    /**
     * Trains and tests a BabyDS model on a batch of data, and saves and returns the results.
     * @param folderName Path to the folder where the model and results will be saved. Used batches, so less general than modelDir.
     * @param modelDir Path to the root directory where all the models and results will be saved.
     * @param trainingBatch The training batch of data to be used for training the model.
     * @param testingData The testing data to be used for evaluating the model.
     * @param batchNumber The batch number of the current training batch.
     * @return The evaluation results of the model after training and testing.
     */
    public EvalResult trainTestBatch(String folderName, String modelDir, RecordTypeCorpus trainingBatch, RecordTypeCorpus testingData, int batchNumber, String testSetSuffix) {
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
                ds.train_model(trainingBatch, folderPath, SEED_GRAMMAR_PATH);
            }
        } else {
            logger.info("Training model...");
            ds.train_model(trainingBatch, folderPath, SEED_GRAMMAR_PATH);
        }
        EvalResult result = ds.evaluate_model(0, folderPath, "", testSetSuffix, N);
        result.setDatasetNames(trainingBatch.corpusName, testingData.corpusName);
        result.setDatasetSizes(trainingBatch.size(), testingData.size());
        System.out.println("\nEvaluation results for batch: " + batchNumber  + " are:");
        System.out.println(result.getSemanticAccResultsTable(String.format("Batch %d", batchNumber)));
        System.out.println(result.getParsingCoverageResultsTable(""));  //todo add proper info
        logger.trace(result.getDiagnosticResults());
        return result;
    }


    /**
     * A wrapper for the method below, that uses the default seed grammar path.
     */
    public Pair<EvalResult, WordHypothesisBase> trainTestBatchWithHB(String folderName, String modelDir, RecordTypeCorpus trainingBatch, RecordTypeCorpus testingData, int batchNumber, String testSetSuffix, WordHypothesisBase previousModel) {
        return trainTestBatchWithHB(folderName, modelDir, trainingBatch, testingData, batchNumber, testSetSuffix, previousModel, SEED_GRAMMAR_PATH_NEW);
    }


    /**
     * TODO Make this use the below trainTestBatchHBSeeded method with default values. But for now just get things working!
     * TODO trainingBatch should be renamed to just trainingData. Batch should be used in the upper method!
     * TODO Add documnetation here.
     */
    public Pair<EvalResult, WordHypothesisBase> trainTestBatchWithHB(String folderName, String modelDir, RecordTypeCorpus trainingBatch, RecordTypeCorpus testingData, int batchNumber, String testSetSuffix, WordHypothesisBase previousModel, String seedGrammarPath) {
        // todo properly test this method
        WordHypothesisBase newModel = null;
        // make foldername under modelDir and copy the computational action files there:
        String folderPath = modelDir + folderName + File.separator;
        File folder = new File(folderPath);
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                throw new RuntimeException("Could not create directory: " + folderPath);
            }
        }

        // Copy the comp-action file from current dir (model dir) to this folder:
        File sourceFile = new File(modelDir + "computational-actions.txt");
        if (!sourceFile.exists()) {
            throw new RuntimeException("Source file does not exist: " + sourceFile.getAbsolutePath());
        }

        File destFile = new File(folder + File.separator + "computational-actions.txt");
        try {
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy computational-actions.txt: " + e.getMessage());
        }

        // save training set and test set to this folder:
        try {
            trainingBatch.saveCorpus(folder + File.separator + "_train.txt");  //todo set their names?
            testingData.saveCorpus(folder + File.separator + "_test"+testSetSuffix+".txt");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save corpus files: " + e.getMessage());
        }

        BabyDSInduction ds = new BabyDSInduction();
        System.out.println(ANSI_CYAN + "******** Batch " + batchNumber + " ********" + ANSI_RESET);
        // If the model exists and skip is true, then skip!
        if (modelExists(folderPath)) {
            System.out.println("Model already exists in " + folderPath);
            if (SKIP_RETRAINING) {
                System.out.println("Skipping training...");
                logger.warn("new model (HB) will be null, as a model already exists in the given directory!");
            } else {
                logger.info("Training model...");
                newModel = ds.trainBabyDSwithHB(trainingBatch, folderPath, seedGrammarPath, previousModel);
            }
        } else {
            logger.info("Training model...");
            newModel = ds.trainBabyDSwithHB(trainingBatch, folderPath, seedGrammarPath, previousModel);
        }
        EvalResult result = ds.evaluate_model(0, folderPath, "", testSetSuffix, N);  //TODO I think I should add the capability of passing training data directly (check if it is string or data)
        // TODO then I can send in training data as full, train on the part I want, and test on full (so I get the same numbers as before HB)
        result.setDatasetNames(trainingBatch.corpusName, testingData.corpusName);
        result.setDatasetSizes(trainingBatch.size(), testingData.size());
        System.out.println("\nEvaluation results for batch: " + batchNumber  + " are:");
        System.out.println(result.getSemanticAccResultsTable(String.format("Batch %d", batchNumber)));
        System.out.println(result.getParsingCoverageResultsTable(""));  //todo add proper info
        logger.trace(result.getDiagnosticResults());

        return new Pair<>(result, newModel);
    }


    /**
     * TODO trainingBatch should be renamed to just trainingData. Batch should be used in the upper method!
     * TODO Add documnetation.
     * TODO many of the stuff being done below better be in one other method so it's tidier (loading data, and paths and copying files)
     */
    public Pair<EvalResult, WordHypothesisBase> trainTestBatchHBSeeded(String seedModelsPath, String folderName, String modelDir, RecordTypeCorpus trainingBatch, RecordTypeCorpus testingData, int batchNumber, String testSetSuffix, WordHypothesisBase previousModel, String seedGrammarPath, int topN, String outputModelDir, int seed) {
        WordHypothesisBase newModel = null;
        HashMap<Integer, Integer> seed_batch_pairs = discoverSeedBatchPairs(seedModelsPath);
        String currentSeedModelDir = seedModelsPath + "S" + seed + "_B" + seed_batch_pairs.get(seed) + File.separator;
        // make foldername under modelDir and copy the computational action files there:
        String folderPath = modelDir + folderName + File.separator;
        File folder = new File(folderPath);
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                throw new RuntimeException("Could not create directory: " + folderPath);
            }
        }

        // Copy the comp-action file from current dir (model dir) to this folder:
        File sourceFile = new File(modelDir + "computational-actions.txt");
        if (!sourceFile.exists()) {
            throw new RuntimeException("Source file does not exist: " + sourceFile.getAbsolutePath());
        }

        File destFile = new File(folder + File.separator + "computational-actions.txt");
        try {
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy computational-actions.txt: " + e.getMessage());
        }

        // save training set and test set to this folder:
        try {
            String trainFilePath = folder + File.separator + "_train.txt";
            File trainFile = new File(trainFilePath);
            if (!trainFile.exists()) {
                trainingBatch.saveCorpus(trainFilePath);
            }
            trainingBatch.corpusName = "_train"; // to avoid long names in results files
            testingData.saveCorpus(folder + File.separator + "_test"+testSetSuffix+".txt");
            testingData.corpusName = "_test"+testSetSuffix; // to avoid long names in results files
        } catch (IOException e) {
            throw new RuntimeException("Failed to save corpus files: " + e.getMessage());
        }

        BabyDSInduction ds = new BabyDSInduction();
        System.out.println(ANSI_CYAN + "******** Batch " + batchNumber + " ********" + ANSI_RESET);
        // If the model exists and skip is true, then skip!
        if (modelExists(folderPath)) {
            System.out.println("Model already exists in " + folderPath);
            if (SKIP_RETRAINING) {
                System.out.println("Skipping training...");
                logger.warn("new model (HB) will be null, as a model already exists in the given directory!");
            } else {
                logger.info("Training model...");
                newModel = ds.trainBabyDSHBSeeded(currentSeedModelDir, trainingBatch, seedGrammarPath, previousModel, topN, outputModelDir);
            }
        } else {
            logger.info("Training model...");
            newModel = ds.trainBabyDSHBSeeded(currentSeedModelDir, trainingBatch, seedGrammarPath, previousModel, topN, outputModelDir);
        }
        EvalResult result = ds.evaluate_model(0, folderPath, "", testSetSuffix, topN, topN);  //TODO I think I should add the capability of passing trainign data directly (check if it is string or data)
        // TODO then I can send in training data as full, train on the part I want, and test on full (so I get the same numbers as before HB)
//        result.setDatasetNames(trainingBatch.corpusName, testingData.corpusName);
        result.setDatasetSizes(trainingBatch.size(), testingData.size());
        System.out.println("\nEvaluation results for batch: " + batchNumber  + " are:");
        System.out.println(result.getSemanticAccResultsTable(String.format("Batch %d", batchNumber)));
        System.out.println(result.getParsingCoverageResultsTable(""));  //todo add proper info
        logger.trace(result.getDiagnosticResults());

        return new Pair<>(result, newModel);
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
        // HashMap<Integer, List<EvalResult>> minDataResults = this.findMinimumMasteryData(CORPUS_NAME, rq1path, REPEAT, SEED);
        HashMap<Integer, List<EvalResult>> rq1hbResults = this.rq1HB(CORPUS_NAME, RQ1CLASS1HB, REPEAT, SEED);
    //    this.allResultsToTSV(rq1hbResults); //todo move this under write results to file!
        // todo average over all classes
    }


    /**
     * Evaluates NeuralTTR. Provides default values for the targets and predictions file names, for the method below.
     * todo make it parallel
     * @param rootFolderPath
     */
    public HashMap<Integer, List<EvalResult>> evalNeuralTTR(String rootFolderPath, String outputFileName){
        return evalNeuralTTR(rootFolderPath, "_neural_targets", "_predictions", outputFileName);
    }


    public HashMap<Integer, List<EvalResult>> evalNeuralTTR(String rootFolderPath, String targetsFileName, String predictionsFileName, String outputFileName) {
        HashMap<Integer, List<EvalResult>> fullResults = new HashMap<>();
        String[] train_test_file_names = {"train", "test"};

        File rootFolder = new File(rootFolderPath);
        if (!rootFolder.exists() || !rootFolder.isDirectory()) {
            logger.error("Invalid root folder path: " + rootFolderPath);
            // return fullResults;
        }

        File[] subFolders = rootFolder.listFiles(File::isDirectory);
        if (subFolders == null || subFolders.length == 0) {
            logger.error("No subfolders found in the root folder: " + rootFolderPath);
            // return fullResults;
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
                        System.out.println(ANSI_RED+"Mismatch in size between targets and predictions in folder: " + subFolder.getName()+ANSI_RESET);
                        System.out.println("You have to debug, but I will skip it for now!");
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
            addResultsToTSV(seed, neuralTTRResult, rootFolderPath + outputFileName + ".tsv");
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
            }
            //todo ATTENTION TOOK IT OUT TO SEE IF IT WORKS BETTER!
            if (targetRT.subsumes(predictionRT) && predictionRT.subsumes(targetRT)) {
                exactMatches++;
            } else {
                System.out.println("Exact match failed for target: \n" + targetRT + "\nand prediction: \n" + predictionRT + "\n");
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


    /**
     * Runs all of RQ2 (generalisation and forgetting) experiments.
     * Loops through:
     * - classes -> "class1", "class2"
     * - settings -> "first", "second", "both"
     * - withCurriculum -> false
     * - seed -> 45..45+REPEAT
     */
    public void runRQ2(int repeatCount, int seed) {
        System.out.println("Running RQ2... Make sure the experiment parameters on the top of this class are set correctly!");
        RQ2FullResults fullResults = new RQ2FullResults();  //TODO refactor to include all the info above...
        for (int i = 0; i < repeatCount; i++) {
            int currentSeed = SEEDS[i];
            for (int n = 3; n <= N; n++) { // TODO change this to 1
                for (String setting : new String[]{"second"}) {  // Options: "first", "second", "both"
                    for (boolean withCurriculum : new boolean[]{false}) {  // Options: false [`true` is not an option!]
                        RQ2SeedResult resultInSeed = testGeneralisationForgetting(C1_BDS_TRAINED_MODELS_DIR, "class1", "class2", setting, withCurriculum, currentSeed, n);  //TODO fix how N is used here...
                        fullResults.addResult(currentSeed, withCurriculum, resultInSeed); //TODO add n to the result!
//                        addResultsToTSV(currentSeed, batchtResult, modelDir + "fullResults.tsv");  // TODO
                    }
                }
            }
        }
        System.out.println("RQ2 experiments finished!");
    }


    public static void main(String[] args) {

        Experiments exp = new Experiments();

        // To run RQ1 (check inside for params)
        // exp.runRQ1();

        // To run RQ2 (check inside for params)
        exp.runRQ2(REPEAT, SEED);


         // To generate data in folders for RQ1:
//        exp.generateDataFolders("c2", NTR_RQ1_CLASS2_PATH, REPEAT, SEED, INIT_BATCH_RATIO, INC_BATCH_RATIO);
        // To convert current corpora to NeuralTTR format:
//        exp.convertCurrentCorporaToNN(NTR_RQ1_CLASS2_PATH);
         // To make neural parsing corpus:
//        exp.makeNeuralParsingCorpus(DATASET_NAME, c2full, "");


        // To evaluate NeuralTTR:
        // String ntr_eval_address = NTR_RQ1_PATH + "NTR-CUR" + File.separator;
        // System.out.println(exp.evalNeuralTTR(ntr_eval_address, "NTR-CUR"));


//        exp.test_generalisation_forgetting(1, 2, "second", false, SEED);
    //    HashMap<Integer, List<EvalResult>> minData = exp.findMinimumMasteryData(CORPUS_NAME, modelPath, REPEAT, SEED);
    //    exp.allResultsToTSV(minData);

        //  HashMap<Integer, List<EvalResult>> rq1hbResults = exp.rq1HB(CORPUS_NAME, RQ1CLASS1HB, REPEAT, SEED);
        // exp.addResultsToTSV(rq1hbResults);

    }

}
