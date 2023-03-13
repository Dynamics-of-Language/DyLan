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
import qmul.ds.Utterance;
import qmul.ds.formula.TTRRecordType;

public class GeneratorTester
{
    // TODO make the appropriate changes here so that I don't have to have the same path here and in the GeneratorLearner class.
    static final String corpusFolderPath = "dsttr/corpus/CHILDES/eveTrainPairs/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static final String corpusPath = corpusFolderPath + "LC-CHILDESconversion3200TrainFinal.txt";//"LC-CHILDESconversion396FinalCopy.txt"; // "LC-CHILDESconversion3200TrainFinal.txt"; //"LC-CHILDESconversion396FinalCopy.txt"; //"LC-CHILDESconversion400FinalCopy.txt";//"AAtrain-3.txt";//"CHILDESconversion100TestFinalCopy.txt";
    static final String grammarPath = "dsttr/resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static GeneratorLearner g = new GeneratorLearner(grammarPath, corpusPath); // TODO is this meaningful? how can I have a constructor that uses default
    protected static Logger logger = Logger.getLogger(GeneratorTester.class);


    public static void testAFewStuff()
    {
        // below is the semantics for "I took it" from "CHILDESconversion400Final", 8th sentence.
        // but replacced `x` with `x2`
        TTRRecordType r = TTRRecordType.parse("[x1==it : e|x2==i : e|e1==take : es|p2==subj(e1, x2) : t|p3==obj(e1, x1) : t|head==e1 : es]");
        System.out.println("r is                               : " + r);

        r = r.removeHead();
        System.out.println("r after removeHead (works in-place): " + r);

        r.resetMetas();
        System.out.println("r after resetMetas (works in-place): " + r);

        System.out.println("r after decompose: ");
        for (TTRRecordType d: r.decompose())
            System.out.println(d);
    }


    public static void testMapFeatures(){
        // below is the semantics for "I took it" from "CHILDESconversion400Final", 8th sentence.
        // but replacced `x` with `x2`
        g.initialiseCountTable();
        TTRRecordType r1 = TTRRecordType.parse("[x1==it : e|x2==i : e|e1==take : es|p2==subj(e1, x2) : t|p3==obj(e1, x1) : t|head==e1 : es]");
        r1 = r1.removeHead();
        for(TTRRecordType r: r1.decompose()){
            List<Integer> indeces = g.mapFeatures(r);
            for (Integer i: indeces){
                System.out.println("My rt: " + r + " , subsumes in goal_features: " + g.goal_features.get(i));
            }
        }
    }


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


    public static void testInitialiseCountTable(){
        g.initialiseCountTable();
        System.out.println(g.conditionalCountTable);
    }


    public static void testSaveModelToFile(HashMap<String, HashMap<TTRRecordType, Double>> table) throws IOException {
        g.saveModelToFile(table);
    }

    public static void testLearn() throws IOException {
        g.learn();
    }


    public static void seperateParsableData() {
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
//        writeToFile(corpusFolderPath + File.separator + "AA-test2.txt", matchingCorpus, test);
        writeToFile(corpusFolderPath + File.separator + "AA-test2.txt", uniqueCorpus, uniqueCorpusStats);
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
    public static void writeToFile(String fileDir, List<String[]> ttrCorpus, CorpusStats corpusStats) {
        PrintStream out = null;
        FileOutputStream fileOpen = null;
        try {
            fileOpen = new FileOutputStream(fileDir);
            out = new PrintStream(fileOpen);
            for (String[] thepair : ttrCorpus) {
                out.print("Sent : " + thepair[0] + "\nSem : " + thepair[1] + "\nFile : " + thepair[2] + "\n\n");
                // Since debugging CorpusStatistics was taking too much time (which was initially used in this method and was not adding correct
                // stats at the end of the corpus text file, I wrote CorpusStats and started using that.
            }

            out.print(corpusStats.statReporter());
        } catch (Exception e) {
            logger.error("Couldn't write to file!");
        } finally {
            if (out != null)
                out.close();
        }
    }


    public static void main(String[] args) throws IOException {
//        testComputeRInc(); // Seems to work fine.
//        testNormaliseCountTable();
//        testSaveModelToFile(testNormaliseCountTable());
//        testLoadModelFromFile; //todo
//        testInitialiseCountTable();
//        testAFewStuff();
//        testMapFeatures()
//        testLearn();
        seperateParsableData();

    }
}
