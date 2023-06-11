/**
 * Learning to generate here.
 *
 * @author Arash Ashrafzadeh, Arash Eshghi
 */

package qmul.ds.learn;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;

import org.apache.log4j.Logger;
import qmul.ds.*;
import qmul.ds.dag.*;
import qmul.ds.formula.*;
import qmul.ds.tree.Node;
import qmul.ds.tree.label.Requirement;
import qmul.ds.tree.label.TypeLabel;
import qmul.ds.type.DSType;

// TODO: save modelPath as a constant string so I can access it in BFG class by importing it from here.
// todo add support for Feature everywhere basically.

public class GeneratorLearner {

    // ====================== fields ======================
    protected DAGParser<? extends DAGTuple, ? extends DAGEdge> parser;  //todo why this is not ICP?
    RecordTypeCorpus corpus = new RecordTypeCorpus();

    protected TreeMap<String, TreeMap<Feature, Double>> conditionalCountTable = new TreeMap<>();
    protected TreeMap<String, TreeMap<Feature, Double>> conditionalProbTable = new TreeMap<>(); // TODO attention: these are NOT being globally updated.
    static final String corpusFolderPath = "dsttr/corpus/CHILDES/eveTrainPairs/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    public static String corpusPath = corpusFolderPath + "AAtrain-3.txt";//"auniq.txt";// //""AA-train-lower-396-matching-top1.txt";//"AAtrain-7.txt";
    static final String grammarPath = "dsttr/resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
//    static public String[] DSTypesString = new String[]{"e", "t", "cn", "e>t", "cn>e", "e>cn", "e>(e>t)", "e>(t>t)", "e>(e>(e>t))"};
//    static final int DSTypesCount = DSTypesString.length * 2;
    static final Double K_SMOOTHING = 0.01;
    static final boolean LOG_PROB = true;
    static final boolean useDSTypes = true;

    // copied from: core/src/babble/dialog/rl/TTRMDPStateEncoding.java
    // Semantic features of the goal, i.e. the grounded content, that are to be tracked by the mdp
    protected TreeSet<Feature> features = new TreeSet<>();//Collections.reverseOrder());  // AA: Shouldn't this be "new List<xxx>(); ? what's the difference?" because in your code it was null, and I changed it since I  was getting an error.
    protected HashMap<String, Integer> wordsCountMap = new HashMap<>(); // todo check for the usage

    protected static Logger logger = Logger.getLogger(GeneratorLearner.class);
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";

    // ====================== constructors ======================

    /**
     * Constructor from parser and corpus objects
     *
     * @param parser
     * @param corpus
     */
    public GeneratorLearner(DAGParser<? extends DAGTuple, ? extends DAGEdge> parser, RecordTypeCorpus corpus)  //todo why this is not ICP?
    {
        this.parser = parser;
        this.corpus = corpus;
    }

    /**
     * Constructor from parser and corpus paths
     *
     * @param parserPath
     * @param corpusPath
     */
    public GeneratorLearner(String parserPath, String corpusPath) {
        try { // I think this needs to be changed to RTcorpus
            this.corpus.loadCorpus(new File(corpusPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.parser = new InteractiveContextParser(parserPath);
    }

    // ====================== methods ======================

    /**
     * AA: copied from core/src/babble/dialog/rl/TTRMDPStateEncoding.java
     * AA's doc: gives you a list of the decomposed features that you want to add to add to the table.
     * no mcs here. adds features independently. Just decomposes and adds the
     * rec types. This is a looser notion of goal..... very unprincipled at this
     * minute!  // todo add a clear comment!
     *
     * @param goal
     */
    public void addAsGoal(TTRRecordType goal) {
        logger.trace("Adding " + goal);
        List<TTRRecordType> decomposition = goal.decompose();
        HashMap<Variable, Variable> map = new HashMap<>();
        List<TTRRecordType> newFeatures = new ArrayList<>();
        decompLoop:
        for (TTRRecordType newFeature : decomposition) {
            for (Feature existingFeature : features) { // AA: columns of the table
                HashMap<Variable, Variable> newMap = new HashMap<>(map);
                if (existingFeature.rt.subsumesMapped(newFeature, map)) {
                    logger.trace("Matched existing feature:" + existingFeature);
                    continue decompLoop;
                } else {
                    map.clear();
                    map.putAll(newMap);
                    logger.trace(existingFeature + " didn't subsume " + newFeature);
                }
            }
            logger.trace("NO MATCH FOR:" + newFeature);
            logger.trace("ADDING AT END");

            TTRRecordType mappedNewFeature = newFeature.relabel(map);
            newFeatures.add(mappedNewFeature);
            features.add(new Feature(mappedNewFeature));
            logger.trace("Adding feature:" + mappedNewFeature);
            logger.trace(ANSI_CYAN + "goal features: " + features + ANSI_RESET);
        }

        for (TTRRecordType f : newFeatures) {
            logger.info("Newly added feature is: " + f);
        }
    }


    public void addDSTypeFeature(Feature dsTypeFeature) {
        features.add(dsTypeFeature);
//        TreeMap<Feature, Double> row = new TreeMap<>();
//        row.put(dsTypeFeature, 0.0);
        for (String word : conditionalCountTable.keySet()) {
            TreeMap<Feature, Double> row = conditionalCountTable.get(word);
            row.put(dsTypeFeature, 0.0);
//            conditionalCountTable.put(word, );

        }
        logger.debug("Added dsTypeFeature: " + dsTypeFeature);
        logger.debug("conditionalCountTable is: " + conditionalCountTable);
    }


    /**
     * Written by Arash Eshghi, then edited and added to code by Arash Ash.
     * Finds a mapping from semantics during parse and the ones used as features in ConditionalProbTable
     * and returns the corresponding indecese (as the map).
     *
     * @param rInc
     * @return state
     */
    public List<TTRRecordType> mapFeatures(TTRFormula rInc) { //todo -------------------------------- only ttr
        List<TTRRecordType> mappedFeatures = new ArrayList<>();
        HashMap<Variable, Variable> map = new HashMap<>();
        logger.trace(rInc);
        ArrayList<TTRRecordType> onlySemanticsFeatures = new ArrayList<>();
        for (Feature feature : features) {
            if (feature.rt != null)
                onlySemanticsFeatures.add(feature.rt);
        }

        for (TTRRecordType feature : onlySemanticsFeatures) {
            feature.resetMetas();
            logger.trace(feature + " -> "); // what does this mean??#
            if (feature.subsumesMapped(rInc, map)) {
                logger.trace(feature + " added.");
                mappedFeatures.add(feature);
            }
            feature.resetMetas();  // AA: Why resetting metas twice? (6 lines before!))
            logger.trace("map is: " + map);
        }
        return mappedFeatures;
    }


    public Feature getPointedNodeAsFeature(DAGTuple currentTuple) {
        Feature pointedNodeFeature;
//        Node pointedNode = test.getTree().getPointedNode();//parser.getState().getCurrentTuple().getTree().getPointedNode(); // TODO ----------------------------------------------------------------------
        Node pointedNode = currentTuple.getTree().getPointedNode();
//        System.out.println("node address: " + pointedNode.getAddress());
        DSType t = pointedNode.getType();
        Requirement req = pointedNode.getTypeRequirement();
        if (t != null) {
            pointedNodeFeature = new Feature(t);
        } else if (req != null) {
            TypeLabel tl = (TypeLabel) req.getLabel();
            t = tl.getType();
            pointedNodeFeature = new Feature(t, true); // AA: CHANGED BY ME from false to true
        } else {
            logger.error("pointedNodeFeature is null.");
            throw new IllegalStateException("pointedNodeFeature is null.");
        }
//     DSType motherNodeType = parser.getState().getCurrentTuple().getTree().getLocalRoot(pointedNode).getType();  // todo is getLocalRoot correct to get the mother node? + deal with this later.
        return pointedNodeFeature;
    }


    // Calculates the probability of words by dividing their count by total count and writes results to file.
    public void calculateWordProbabilities(Integer totalWordsCount) throws IOException {
        FileWriter writer = new FileWriter(grammarPath + File.separator + "wordProbs.tsv", false);

        for (String word : wordsCountMap.keySet()) {
            double prob = (double) wordsCountMap.get(word) / (double) totalWordsCount;
            if (LOG_PROB)
                prob = Math.log(prob);
            String line = String.format("%s\t%f\n", word, prob);
            writer.write(line);
        }
        writer.close();

    }


    /**
     * Creates a 2D hashmap as the count table and initialises counts to 0.
     */
    public void initialiseCountTable() {
        Integer totalWordsCount = 0;
        // think about this: maybe it's not efficient to make a table and then add ot it, and it's better to add to table on the fly?
        // First, gather all the possible semantic features from data:
        for (Pair<Sentence<Word>, TTRRecordType> pair : corpus) {
            Sentence<Word> sentence = pair.first();
            TTRRecordType goldSem = pair.second();
            goldSem = goldSem.removeHeadIfManifest();
            addAsGoal(goldSem);
            for (Word word : sentence) { // todo Does this add duplicate words?
                String w = word.toString();
                wordsCountMap.put(w, wordsCountMap.getOrDefault(w, 0) + 1);
                totalWordsCount += 1;
            }
            try {
                calculateWordProbabilities(totalWordsCount);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Then add them as columns in the table.
        for (String word : wordsCountMap.keySet()) {
            TreeMap<Feature, Double> row = new TreeMap<>();
            for (Feature f : features)
                row.put(f, 0.0);

            conditionalCountTable.put(word, row);
//            logger.trace("Added row " + i + " to conditionalCountTable: " + row);
        }
        logger.info("goal_features at the end of initialiseCountTable: " + features);
    }


    /**
     * Normalises a table by dividing elements in a column by their sum.
     * Set K_SMOOTHING=0 on top of the class to have no smoothing.
     */
    public void normaliseCountTable() {
        HashMap<Feature, Double> total = new HashMap<>(); // Don't have to init to zero since I'm using getOrDefault method.
        // First, find total sum of each column in this loop:
        for (String word : wordsCountMap.keySet()) {
            TreeMap<Feature, Double> row = conditionalCountTable.get(word);
            for (Feature feature : features) {
                Double count = row.get(feature);
                Double smoothedCount = count + K_SMOOTHING;
//                if (feature.dsType != null) // to avoid smoothing DSTypes.
//                    smoothedCount = count;
                row.put(feature, smoothedCount);
                total.put(feature, total.getOrDefault(feature, 0.0) + smoothedCount);
                // If not, add `count` to 0, which means just put `count`. Used because key might not be available.
            }
            conditionalCountTable.put(word, row);
        }
        // Then divide columns by the corresponding `total` to get probabilities and save them in `conditionalProbTable`.
        for (String word : wordsCountMap.keySet()) {
            TreeMap<Feature, Double> row = conditionalCountTable.get(word);
            TreeMap<Feature, Double> probRow = new TreeMap<>();
            for (Feature feature : features) {
                Double prob;
//                if (feature.dsType == null) { // to avoid normalising DSTypes. todo
//                    prob = row.get(feature) / total.get(feature);
//                    if (LOG_PROB)
//                        prob = Math.log(prob);
//                } else {
//                    if (total.get(feature) == 0.0) // skip the feature if it's not in the corpus.
//                        continue;
//                    else
//                        prob = row.get(feature);
//                }
                prob = row.get(feature) / total.get(feature);
                if (LOG_PROB)
                    prob = Math.log(prob);
                probRow.put(feature, prob);
            }
            conditionalProbTable.put(word, probRow);
        }
    }


    /**
     * Writes the conditionalProbTable to a csv file.
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void saveModelToFile()
            throws FileNotFoundException, IOException // eclipse recommended it so I said yes.
    {
        // TODO Make more efficient
        // First, clear the file if it was already there. RF: https://stackoverflow.com/questions/6994518/how-to-delete-the-content-of-text-file-without-deleting-itself
        File f = new File(grammarPath + "top-1-400_11-6-2023_model.csv");
        if (f.exists() && !f.isDirectory()) {
            PrintWriter writer = new PrintWriter(f);
            writer.print("");
            writer.close();
        }
        // Writing features row
        ArrayList<String> featuresStr = new ArrayList<String>();

        for (Feature feature : features) // todo make this more efficient
            featuresStr.add(feature.toString());

        String strFeatures = String.join(" , ", featuresStr);
        strFeatures = "[WORDS\\FEATURES]" + " , " + strFeatures + "\n";
        FileWriter writer1 = new FileWriter(grammarPath + "top-1-400_11-6-2023_model.csv", true);
        writer1.write(strFeatures);
        writer1.close();

        // Writing words and probs
        int row_num = 0;
        for (String word : conditionalProbTable.keySet()) { // REF: https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
            ArrayList<String> probsStr = new ArrayList<>();
            TreeMap<Feature, Double> row = conditionalProbTable.get(word);
            for (Feature feature : features) {
                Double prob = row.get(feature);
                probsStr.add(Double.toString(prob));
            }

            // Converts the list to a string joined by comma
            String strRow = String.join(" , ", probsStr); //REF: https://mkyong.com/java/java-how-to-join-list-string-with-commas/
            strRow = word + " , " + strRow;
            // write to file
            try {
                FileWriter writer = new FileWriter(grammarPath + "top-1-400_11-6-2023_model.csv", true);
                writer.write(strRow);
                writer.write(System.lineSeparator()); // To go to next line. REF: https://stackoverflow.com/questions/18549704/create-a-new-line-in-javas-filewriter
                writer.close();
                logger.trace("Successfully wrote row " + row_num + " to the file."); // todo add logs
                row_num++;
            } catch (IOException e) {
                System.out.println("An error occurred."); // todo better message (could not write row x to file.)
                e.printStackTrace();
            }
        }
        System.out.println("Model saved successfully.");
    }


    /**
     * Learn to generate by:
     * - Loading the CHILDES corpus.
     * - Parsing <sentence, TTR-RT> pairs.
     * - Matching the parse with the gold TTR-RT semantics.
     * - Compute `rInc`.
     * - Decompose `rInc`.
     * - Populate `conditionalCountTable`.
     * - Create `conditionalProbTable` by normalising `conditionalCountTable`.
     * - Save `conditionalProbTable` to a csv file.
     */
    public void learn() throws IOException {
        initialiseCountTable();
        logger.info("features length: " + features.size());
        logger.info("table features: " + features);
        for (Pair<Sentence<Word>, TTRRecordType> pair : corpus) {
            parser.init();  // Restarts parser.
            Sentence<Word> sentence = pair.first();
            TTRRecordType rG = pair.second();
            Boolean parsed = parser.parseUtterance(new Utterance(sentence));  // Converting Sentence to Utterance to be able to parse it.
            while (parsed) {
                TTRRecordType finalSem = (TTRRecordType) parser.getState().getCurrentTuple().getSemantics();
                finalSem = finalSem.removeHeadIfManifest();
                rG = rG.removeHeadIfManifest();
                if (rG.subsumes(finalSem) && finalSem.subsumes(rG)) { // `true` if the sem from parser matches the one in the dataset.
                    Context<DAGTuple, GroundableEdge> context = (Context<DAGTuple, GroundableEdge>) parser.getContext();
                    DAG<DAGTuple, GroundableEdge> dag = context.getDAG();
                    DAGTuple curTuple = dag.getCurrentTuple();
                    GroundableEdge curEdge = dag.getParentEdge(curTuple);
                    while (curEdge != null) {
                        // While the semantic features need to come from the parent of curTuple at this point, the syntactic features need to come from curTuple.
                        // This is because generation is conditioned both on the future (the goal) and the past (reflected in the current semantic tree).
                        Feature pointedNodeFeature;
//                        if (useDSTypes){
//                            pointedNodeFeature = getPointedNodeAsFeature(curTuple);
//                            if (!features.contains(pointedNodeFeature)) // checks if the pointedNodeFeature is already in the table to decide whether to add and initialise it or not.
//                                addDSTypeFeature(pointedNodeFeature);
//                        }
                        String word = curEdge.word().word();
//                        logger.debug(ANSI_YELLOW + "pointedNodeFeature: " + pointedNodeFeature + " _____ Current word: " + word + ANSI_RESET);
                        curTuple = dag.getParent(curTuple); // init to correct tuple, one before the last.

                        // AA: NEWLY ADDED TO GET MOTHER NODE BASICALLY INSTEAD OF CURRENT NODE. copied from above. todo check for efficiency and modify respectively.
                        if (useDSTypes){
                            pointedNodeFeature = getPointedNodeAsFeature(curTuple);
                            if (!features.contains(pointedNodeFeature)) // checks if the pointedNodeFeature is already in the table to decide whether to add and initialise it or not.
                                addDSTypeFeature(pointedNodeFeature);
                        }
                        logger.debug(ANSI_YELLOW + " Type of mother of pointed Node: " + pointedNodeFeature + " | Current word: " + word + ANSI_RESET);

                        TTRRecordType rCur = (TTRRecordType) curTuple.getSemantics();
                        rCur = rCur.removeHeadIfManifest();
                        TTRRecordType rInc = finalSem.subtract(rCur, new HashMap<>());
                        logger.trace("rG: " + finalSem + " MINUS rCur: " + rCur + " EQUALS rInc: " + rInc);
                        TreeMap<Feature, Double> row = conditionalCountTable.get(word);
                        List<TTRRecordType> mappedFeatures = mapFeatures(rInc);
                        for (TTRRecordType correspondingFeature : mappedFeatures) { // Updates the CountTable with semantic features.
                            Feature f = new Feature(correspondingFeature);
                            row.put(f, row.get(f) + 1);
                        }
                        if (useDSTypes) {
                            Double val = row.get(pointedNodeFeature);
                            row.put(pointedNodeFeature, val + 1); // Updates the CountTable with syntactic features.
                        }
                        curEdge = dag.getParentEdge(curTuple);
//                        curTuple = dag.getParent(curTuple); // AA: Updating curTuple to the previous one (until we get to root).
                    }
                    break; // To prevent checking other parses after finding the correct one.
                }
                parsed = parser.parse();
            }
        }
        normaliseCountTable();
        System.out.println(features);
        saveModelToFile();
    }


    /**
     * Just to test the methods above.
     *
     * @param args
     */
    public static void main(String[] args) throws IOException { // this shouldn't be throwing this exception TODO FIX
        GeneratorLearner learner = new GeneratorLearner(grammarPath, corpusPath);

        learner.learn();
    }
}
