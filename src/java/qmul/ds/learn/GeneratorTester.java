/**
 * A class to test methods for the generation paper.
 * Classes to be tested are `GeneratorLearner` & ?.
 *
 * @author Arash Ashrafzadeh
 */

package qmul.ds.learn;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;
import org.apache.log4j.Logger;

import qmul.ds.InteractiveProbabilisticGenerator;
import qmul.ds.Utterance;
import qmul.ds.dag.UtteredWord;
import qmul.ds.formula.TTRRecordType;

public class GeneratorTester
{
    // TODO make the appropriate changes here so that I don't have to have the same path here and in the GeneratorLearner class.
    static final String corpusFolderPath = "dsttr/corpus/CHILDES/eveTrainPairs/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static public String corpusPath = corpusFolderPath + "AA-train-lower-396-matching-top1.txt";//"train-4000-1s.txt";//"test-4000-3.txt";//amat.txt";//"AA-train-lower-396-matching-top3.txt";//"amat.txt";//AA-full-lower-4000-Copy.txt";//""LC-CHILDESconversion400FinalCopy.txt";//"AA-train-lower-396-matching-top1.txt";//"AAtrain-3.txt";//"AA-train-lower-396-matching-top1.txt";//"AAtrain-3.txt"; //"AA-train-lower-396-matching-top1.txt";//"LC-CHILDESconversion800TestFinalCopy.txt"; //"LC-CHILDESconversion3200TrainFinalCopy.txt";//"AAtrain-72.txt"; //"LC-CHILDESconversion3200TrainFinalCopy.txt"; //"AAtrain-72.txt"; // "LC-CHILDESconversion396FinalCopy.txt"; // "LC-CHILDESconversion3200TrainFinal.txt"; //"LC-CHILDESconversion396FinalCopy.txt"; //"LC-CHILDESconversion400FinalCopy.txt";//"AAtrain-3.txt";//"CHILDESconversion100TestFinalCopy.txt";
    static final String grammarPath = "dsttr/resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    protected static Logger logger = Logger.getLogger(GeneratorTester.class);
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RED = "\u001B[31m";


// ==================================||   Learner Tests   ||==================================
//    public void testMapFeatures(){
//        // below is the semantics for "I took it" from "CHILDESconversion400Final", 8th sentence.
//        // but replacced `x` with `x2`
//        g.initialiseCountTable();
//        TTRRecordType r1 = TTRRecordType.parse("[x1==it : e|x2==i : e|e1==take : es|p2==subj(e1, x2) : t|p3==obj(e1, x1) : t|head==e1 : es]");
//        r1 = r1.removeHead();
//        for(TTRRecordType r: r1.decompose()){
//            List<TTRRecordType> mappedFeatures = g.mapFeatures(r);
//            for (TTRRecordType feature: mappedFeatures) {
//                System.out.println("My rt: " + r + " , subsumes in goal_features: " + feature); // double check
//            }
//        }
//    }


//    public static HashMap<String, HashMap<TTRRecordType, Double>> testNormaliseCountTable() //todo match the new method
//    {
//        HashMap<String, HashMap<TTRRecordType, Integer>> myCountTable = new HashMap<String, HashMap<TTRRecordType, Integer>>();
//        // TODO is there a better way to make this more compact?
//
//        HashMap<TTRRecordType, Integer> v1 = new HashMap<TTRRecordType, Integer>(); // value1
//        HashMap<TTRRecordType, Integer> v2 = new HashMap<TTRRecordType, Integer>(); // value2
//        v1.put(TTRRecordType.parse("[s==i : e]"), 2);
//        v1.put(TTRRecordType.parse("[s==you : e]"), 1);
//        myCountTable.put("i", v1);
//        v2.put(TTRRecordType.parse("[s==you : e]"), 3);
//        v2.put(TTRRecordType.parse("[s==i : e]"), 0);
//        myCountTable.put("you", v2);
//
//        HashMap<String, HashMap<TTRRecordType, Double>> myProbTable = g.normaliseCountTable(myCountTable, 2, false);
//
//        // print table // TODO make this cleaner
//        System.out.println("\nThe probTable is: ");
//        for (String k: myProbTable.keySet())
//        {
//            System.out.print(" word: " + k + " | ");
//            HashMap<TTRRecordType, Double> row = myProbTable.get(k);
//            for (TTRRecordType rt: row.keySet())
//                System.out.print(Double.toString(row.get(rt)) + ",  ");
//            System.out.println("\n");
//        }
//        return myProbTable;
//    }


//    public static void testInitialiseCountTable(){
//        g.initialiseCountTable();
//        System.out.println(g.conditionalCountTable);
//    }


//    public static void testSaveModelToFile() throws IOException {
//        g.saveModelToFile();
//    }

//    public static void testLearn() throws IOException {
//        g.learn();
//    }


    public static void seperateParsableData(GeneratorLearner g) {
        ArrayList<Integer> parseableIndices = new ArrayList<>();
        ArrayList<Integer> matchingIndices = new ArrayList<>();
        List<String[]> matchingCorpus = new ArrayList<>();
        List<String[]> uniqueCorpus = new ArrayList<>();
        List<String> uniqueSentences = new ArrayList<>();
        List<String[]> tpNotICPList = new ArrayList<>();
        ArrayList<Integer> tpNotICPIndices = new ArrayList<>(Arrays.asList(278, 279, 280, 282, 289));
        Integer tpNotICPIndex = 0;
        HashMap<Integer, Integer> lenCountMapMatching = new HashMap<>();
        CorpusStats matchingCorpusStats = new CorpusStats();
        CorpusStats uniqueCorpusStats = new CorpusStats();
        CorpusStats tpNotICPStats = new CorpusStats();

        Integer index = 0;

        for (Pair<Sentence<Word>, TTRRecordType> pair : g.corpus) {
            g.parser.init();
            Sentence<Word> sentence = pair.first();
            TTRRecordType goldSem = pair.second();

            boolean parsed = g.parser.parseUtterance(new Utterance(sentence));

            while (parsed) {
                parseableIndices.add(index);
                TTRRecordType parserSem = (TTRRecordType) g.parser.getState().getCurrentTuple().getSemantics();
                parserSem = parserSem.removeHeadIfManifest();
                goldSem = goldSem.removeHeadIfManifest();
                if (goldSem.subsumes(parserSem) && parserSem.subsumes(goldSem)){
                    matchingIndices.add(index);
                    lenCountMapMatching.put(sentence.size(), lenCountMapMatching.getOrDefault(sentence.size(),0)+1);
                    String[] pair1 = new String[3];
                    String sentenceStr = sentence.toString(); // Cached it since we are calling it a lot.
                    pair1[0] = sentenceStr;
                    pair1[1] = goldSem.toString();
                    pair1[2] = "1";
                    // To make a dataset of unique sentences:
                    if(!uniqueSentences.contains(sentenceStr)){
                        uniqueSentences.add(sentenceStr);
                        uniqueCorpus.add(pair1); //todo
                        uniqueCorpusStats.addSentence(sentence);
                    }
                    matchingCorpus.add(pair1); //todo
                    matchingCorpusStats.addSentence(sentence);

                    break;
                }
                parsed = g.parser.parse();
            }
            System.out.printf("\n ----------- Couldn't parse utt number %d: " + sentence + "\n", index); //todo add as logger (maybe WARN?)

            // To find the part of the data that is parseable by TP but not ICP:
//            Integer diffIdx = tpNotICPIndices.get(tpNotICPIndex);
//            if (diffIdx.equals(index)){
//                String[] pair2 = new String[3];
//                pair2[0] = sentence.toString();
//                pair2[1] = goldSem.toString();
//                pair2[2] = "1";
//                tpNotICPList.add(pair2);
//                tpNotICPStats.addSentence(sentence);
//                if (tpNotICPIndex <tpNotICPIndices.size()-1)
//                    tpNotICPIndex++;
//            }
            index++;
        }
        writeToFileData(corpusFolderPath, File.separator + "test-4000-3s.txt", matchingCorpus, matchingCorpusStats);
//        writeToFileData(corpusFolderPath, File.separator + "AA-full-lower-4000-unique-top3.txt", uniqueCorpus, uniqueCorpusStats);
//        writeToFile(corpusFolderPath + File.separator + "AA-train-lower-3200-unique-top1.txt", uniqueCorpus, corpusStatsUnique);
//        writeToFile(corpusFolderPath+File.separator+"tp_not_icp.txt", tpNotICPList, corpusStatsL);
        Integer totalSize = g.corpus.size();
        Set<Integer> parseablesSet = new HashSet<>(parseableIndices);
        Set<Integer> matchingsSet = new HashSet<>(matchingIndices);
        Integer parsedUttCount = parseablesSet.size();
        Integer matchedParses = matchingsSet.size();
        System.out.printf("\nAll: %d | Parsed: %d , coverage: %f | Matched: %d , among-all: %f, among-parsed: %f\n", totalSize, parsedUttCount, Double.valueOf(parsedUttCount)/totalSize, matchedParses, Double.valueOf(matchedParses)/totalSize, Double.valueOf(matchedParses)/parsedUttCount);
//        System.out.println("parseables | len: " + parsedUttCount);
        // IMPORTANT: these are not unique, so I wrote a Python script to make these unique and then used the
        // counts to compute coverage.
//        System.out.println("matchings | len: " + matchedParses);
//        System.out.println(matchingsSet); //todo make these sorted
//        System.out.println("tp not icp len: " + tpNotICPIndices.size());
        System.out.println("Unique | len: " + uniqueCorpus.size());
    }

    /**
     * Initially copied from CorpusReaderWriter.
     * but then modified because I couldn't debug CorpusStatistics class. So I wrote my own class for the same purpose: CorpusStats.
     *
     * @param fileDir
     * @param ttrCorpus
     * @param corpusStats
     */
    public static void writeToFileData(String fileDir, String fileName, List<String[]> ttrCorpus, CorpusStats corpusStats) {
        PrintStream out = null;
        FileOutputStream fileOpen;
        try {
            fileOpen = new FileOutputStream(fileDir+fileName);
            out = new PrintStream(fileOpen);
            for (String[] thepair : ttrCorpus) {
                out.print("Sent : " + thepair[0] + "\nSem : " + thepair[1] + "\nFile : " + thepair[2] + "\n\n");
                // Since debugging CorpusStatistics was taking too much time (which was initially used in this method and was not adding correct
                // stats at the end of the corpus text file, I wrote CorpusStats and started using that.
            }
            out.print(corpusStats.statReporter());
            corpusStats.writeWordProbsToFile(fileDir, true);
        } catch (Exception e) {
            logger.error("Couldn't write to file!");
        } finally {
            if (out != null)
                out.close();
        }
    }

// ==================================||   Generator Tests   ||==================================

    public void addToOutputCorpus(String goldSentence, String rG, Sentence<Word> generatedSentence, List<String[]> corpusList, CorpusStats corpusStats) {
        String[] pair = new String[3];
        pair[0] = goldSentence;
        pair[1] = rG;
        pair[2] = generatedSentence.toString();
        corpusList.add(pair);
        corpusStats.addSentence(generatedSentence);
    }


    // inspired from the one in GeneratorTester, but modified to write generated sentence instead of "File : 1".
    public static void writeToFileOutput(String fileDir, List<String[]> ttrCorpus, CorpusStats corpusStats) {
        PrintStream out = null;
        FileOutputStream fileOpen = null;
        try {
            fileOpen = new FileOutputStream(fileDir);
            out = new PrintStream(fileOpen);
            for (String[] thepair : ttrCorpus)
                out.print("GoldSent : " + thepair[0] + "\nSem : " + thepair[1] + "\nGenSent : " + thepair[2] + "\n\n");
            // stats at the end of the corpus text file, I wrote CorpusStats and started using that.

            out.print(corpusStats.statReporter()); // If the corpus is empty, this will throw an error. Have to handle it properly.
        } catch (Exception e) {
            logger.error(ANSI_RED + "Couldn't write to \"" + fileDir + "\"!" + ANSI_RESET);
        } finally {
            if (out != null)
                out.close();
        }
    }


    /**
     * Assumes learning has happened on the correct data, therefore loads the last saved learned model.
     */
    public void testProbabilisticGenerator() {
        InteractiveProbabilisticGenerator generator = new InteractiveProbabilisticGenerator(grammarPath, grammarPath);
        List<String[]> fullyGeneratedList = new ArrayList<>();
        List<String[]> fullyGeneratedEM = new ArrayList<>();
        List<String[]> fullyGeneratedNonEM = new ArrayList<>();
        List<String[]> partiallyGeneratedList = new ArrayList<>();
        CorpusStats fullyGeneratedStats = new CorpusStats();
        CorpusStats fullyGeneratedEMStats = new CorpusStats();
        CorpusStats fullyGeneratedNonEMStats = new CorpusStats();
        CorpusStats partiallyGeneratedStats = new CorpusStats();
        int absCorrect = 0; // these have to go to test class.
        RecordTypeCorpus corpus = new RecordTypeCorpus();

        try {
            corpus.loadCorpus(new File(corpusPath));
            for (Pair<Sentence<Word>, TTRRecordType> pair : corpus) {
                generator.init();
                List<TTRRecordType> mappedFeatures;
                Sentence<Word> goldSentence = pair.first();
                // AE: This is assuming that the goal record type in the corpus is headless.
                TTRRecordType rG = pair.second().removeHeadIfManifest();
//                this.init(); // Why there is two of this? commenting this one out.
                logger.info(ANSI_CYAN + "Gold sentence: " + goldSentence + ANSI_RESET);
                Sentence<Word> generatedSentence = new Sentence<>(); // todo fix
                TTRRecordType rInc = rG; // Init to goal.
                TTRRecordType rCur;
                generator.setGoal(rG);

                while (!rInc.isEmpty()) { // rInc is now updated by populateBeam.
//                    generator.populateBeam();
                    boolean hasGenerated = generator.generate();
                    List<UtteredWord> sent = generator.getGenerated().getWords();
                    sent.forEach((w) -> generatedSentence.add(new Word(w.word()))); //todo test
                    if(hasGenerated){
//                        String w = generator.getParser().getContext().getDAG().getParentEdge().word().word();
                        // todo the following is only working for the last word of the sentence.
//                        logger.info(ANSI_GREEN + "Generated word \"" + w + "\"" + ANSI_RESET);// + "\" with log-probability " + prob + ANSI_RESET);

//                        List<UtteredWord> sent = generator.getGenerated().getWords();
//                        sent.forEach((w) -> generatedSentence.add(new Word(w.word()))); //todo test
//                        generatedSentence.remove(0); // remove the first word, which is the speaker.
                        rCur = (TTRRecordType) generator.getParser().getContext().getDAG().getCurrentTuple().getSemantics().removeHeadIfManifest();  // How expensive is this operation?
                        rInc = rG.subtract(rCur, new HashMap<>());
                        break;
                    }
                    else{
                        logger.error(ANSI_RED + "Could NOT continue generation!" + ANSI_RESET); // later add the word that was not generated to the error message.
                        // todo better log message.
                        addToOutputCorpus(goldSentence.toString(), rG.toString(), generatedSentence, partiallyGeneratedList, partiallyGeneratedStats);
                        break; // todo: or not to break, this is the question.
                    }
                }
                if (rInc.isEmpty()) {   //todo is this necessary? I mean this si the condition for the while loop!! // If we are here, then we have a fully generated sentence.
                    logger.info(ANSI_BLUE + "Fully generated sentence: " + generatedSentence + ANSI_RESET);
                    if (generatedSentence.equals(goldSentence)) {
                        absCorrect++;
                        addToOutputCorpus(goldSentence.toString(), rG.toString(), generatedSentence, fullyGeneratedEM, fullyGeneratedEMStats);
//                        fullyGeneratedEM.add(generatedSentence.toString());
                    } else {
//                        fullyGeneratedNonEM.add(generatedSentence.toString());
                        addToOutputCorpus(goldSentence.toString(), rG.toString(), generatedSentence, fullyGeneratedNonEM, fullyGeneratedNonEMStats);
                    }
                    // TODO if a sentence is fully generated, it's not necessarily correct. We should check that too in a way: putting it in the other output file or keep this in mind when processing this output file.
                    addToOutputCorpus(goldSentence.toString(), rG.toString(), generatedSentence, fullyGeneratedList, fullyGeneratedStats);
                }
            }
            writeToFileOutput(grammarPath + "genOutputFull.txt", fullyGeneratedList, fullyGeneratedStats);
            writeToFileOutput(grammarPath + "genOutputPartial.txt", partiallyGeneratedList, partiallyGeneratedStats);
            writeToFileOutput(grammarPath + "genOutputFullEM.txt", fullyGeneratedEM, fullyGeneratedEMStats);
            writeToFileOutput(grammarPath + "genOutputFullNonEM.txt", fullyGeneratedNonEM, fullyGeneratedNonEMStats);
            System.out.println("Absolutely correct: " + absCorrect);

        } catch (IOException e) {
            throw new RuntimeException("Could NOT load corpus from file.");
        }

    }


// ==================================||   MAIN   ||==================================
    public static void main(String[] args) throws IOException {
//        testComputeRInc(); // Seems to work fine.
//        testNormaliseCountTable();
//        testSaveModelToFile(testNormaliseCountTable());
//        testLoadModelFromFile; //todo
//        testInitialiseCountTable();
//        testAFewStuff();
//        testMapFeatures()
//        testLearn();
//        seperateParsableData(learner);

//          GeneratorLearner learner = new GeneratorLearner(grammarPath, corpusPath);
//            seperateParsableData(learner);

        GeneratorLearner learner = new GeneratorLearner(grammarPath, corpusPath); // TODO is this meaningful? how can I have a constructor that uses default
        GeneratorTester tester = new GeneratorTester();

        logger.info("Learning phase begins.");
        learner.learn();
        logger.info(ANSI_GREEN+"Learning phase finished successfully."+ANSI_RESET);

        logger.info("Generation phase begins.");
        tester.testProbabilisticGenerator();
        logger.info(ANSI_GREEN+"Generation phase finished successfully."+ANSI_RESET);
    }
}
