package qmul.ds.interactiveInduction;

import edu.stanford.nlp.util.Pair;
import org.apache.log4j.Logger;
import qmul.ds.learn.RecordTypeCorpus;
import static qmul.ds.interactiveInduction.BabyDSInduction.mergeFiles;

import java.io.File;
import java.io.IOException;
import java.util.*;


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
    static final String rq2path = "resource\\2025-babyds-RQ2\\".replace("\\", File.separator);
    String forgettingPath = "resource\\2025-babyds-RQ2\\forgetting\\".replace("\\", File.separator);
    String generalisationPath = "resource\\2025-babyds-RQ2\\generalisation\\".replace("\\", File.separator);
    public static final int SEED = 45; // Set a constant seed for reproducibility
    public static final double TRAIN_TEST_RATIO = 0.85;  // Train-Test split ratio (Meaning the x ratio is for train, 1-x is for test)
    public static final boolean SAVE_TO_FILE = true;  // Save the training and testing sets to file


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
        Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> ttsResults = bds.evaluate_model(0);

//        evaluate_model(test_data);
        // Now time to train on the batches of class 2 only, and evaluate on the same test set.
        // for batch in training_batches_class2
        for(RecordTypeCorpus batch : training_batches_class2) {
            bds.train_model(batch.corpusName, "", seedGrammarPath);  //todo fix
            Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> ttsResults2 = bds.evaluate_model(0);
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
                Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> ttsResults = ds.evaluate_model(0, forgettingPath, "babyds");
                EvalResult ev = new EvalResult(ttsResults.first, ttsResults.second);
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
                Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>> ttsResults = ds.evaluate_model(0, forgettingPath, "babyds");
                EvalResult ev = new EvalResult(ttsResults.first, ttsResults.second);
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
     * - Train a model on the training batches.
     * - Evaluate the model on the development set.
     * - If the model has not reached the mastery threshold, add more data to the training batches and retrain the model.
     * - Keep track of results until the model has reached the mastery threshold.
     * - If the difference between the current and previous evaluation results is less than the improvement threshold for
     * a "patience" number of times, stop the process, otherwise continue.
     * - Return the minimum amount of data that is needed to reach the mastery threshold.
     * - Repeat the experiment a number of times (with different seeds) to get an average as a true estimate.
     *
     * @param corpus the data of a class
     * @param masteryThreshold the threshold for mastery
     * @param improvementThreshold the threshold for improvement
     * @param metric the metric to use for evaluation
     * @param repeatCount the number of times to repeat the experiment
     * @param seed the seed for reproducibility
     * @param patience the number of times to wait for improvement before stopping
     */
    public void findMinimumMasteryData(RecordTypeCorpus corpus, String modelPath, double masteryThreshold, double improvementThreshold, String metric, int repeatCount, int seed, int patience) {
        BabyDSInduction ds = new BabyDSInduction(modelPath);
        corpus.corpusName = "mycorpus";
        List<EvalResult > results = new ArrayList<>();  //todo integrate for furthur analysis
        for (int i = 0; i < repeatCount; i++) {
            int rollingPatience = patience;
            // Split the data into training batches and a development set
            Collections.shuffle(corpus, new Random(seed+i));
            List<RecordTypeCorpus> trainingBatches = ds.prepare_batches(corpus);
            RecordTypeCorpus devSet = trainingBatches.getLast();
            devSet.corpusName = corpus.corpusName+"test";
            trainingBatches.removeLast();
            RecordTypeCorpus currentBatch = trainingBatches.getLast();  // todo have to somehow handle these training sets, in terms of them being read for model training
            trainingBatches.removeLast();
            // Train a model on the training batches
            ds.train_model(currentBatch, modelPath, seedGrammarPath); //todo

            // Evaluate the model on the development set
            Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>>
                    ttsResults = ds.evaluate_model(0, modelPath, corpus.corpusName);  //todo

            // If the model has not reached the mastery threshold, add more data to the training batches and retrain the model
            while (ttsResults.second.get(0).get(metric).get(1) < masteryThreshold) {
                // Add more data to the training batches
                RecordTypeCorpus newBatch = trainingBatches.getLast();
                trainingBatches.removeLast();
                currentBatch.mergeCorpora(newBatch);

                // Retrain the model
                ds.train_model(currentBatch, modelPath, seedGrammarPath);  //todo

                // Evaluate the model on the development set
                Pair<HashMap<Integer, HashMap<String, HashMap<String, Double>>>, HashMap<Integer, HashMap<String, ArrayList<Double>>>>
                        newTtsResults = ds.evaluate_model(0, modelPath, corpus.corpusName);

                // If the difference between the current and previous evaluation results is less than the improvement threshold for
                // a "patience" number of times, stop the process, otherwise continue
                if (Math.abs(newTtsResults.second.get(0).get(metric).get(1) - ttsResults.second.get(0).get(metric).get(1)) < improvementThreshold) {
                    rollingPatience--;
                    if (rollingPatience == 0) {
                        break;
                    }
                } else {
                    rollingPatience = patience;  // rest? todo
                }
                ttsResults = newTtsResults;
            }
        }
    }


    public void rq2() {
    // TODO should add "G" for generalisation tests, "F" for forgetting tests, and "B" for both forgetting and generalisation tests.
    //todo add also "C" for curriculum learning tests, and "R" for random learning tests.
    }

    public void rq3() {

    }

    public void runAllExperiments() {
        // TODO run all experiments here, like a boss.
    }


    public static void main(String[] args) {

        Experiments exp = new Experiments();
        exp.test_forgetting(1, 2, "S");
    }

}
