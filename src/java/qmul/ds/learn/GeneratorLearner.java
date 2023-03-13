/**
 * Learning to generate here.
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
import qmul.ds.*;
import qmul.ds.dag.*;
import qmul.ds.formula.*;

// TODO: save modelPath as a constant string so I can access it in BFG class by importing it from here.
// todo add support for Feature everywhere basically.

public class GeneratorLearner {

    // ====================== fields ======================
    protected DAGParser<? extends DAGTuple, ? extends DAGEdge> parser;  //todo why this is not ICP?
    RecordTypeCorpus corpus = new RecordTypeCorpus();

    protected HashMap<String, HashMap<TTRRecordType, Integer>> conditionalCountTable = new HashMap<String, HashMap<TTRRecordType, Integer>>();
    protected HashMap<String, HashMap<TTRRecordType, Double>> conditionalProbTable = new HashMap<String, HashMap<TTRRecordType, Double>>(); // TODO attention: these are NOT being globally updated.
    static final String corpusFolderPath = "dsttr/corpus/CHILDES/eveTrainPairs/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static final String corpusPath = corpusFolderPath + "LC-CHILDESconversion396FinalCopy.txt";//"AAtrain.txt";
    static final String grammarPath = "dsttr/resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static final Integer K_SMOOTHING = 0;

    // copied from: core/src/babble/dialog/rl/TTRMDPStateEncoding.java
    // Semantic features of the goal, i.e. the grounded content, that are to be tracked by the mdp
    protected List<TTRRecordType> goal_features = new ArrayList<TTRRecordType>();  // AA: Shouldn't this be "new List<xxx>(); ? what's the difference?" because in your code it was null, and I changed it since I  was getting an error.
    protected List<String> slot_values = new ArrayList<String>();
    // a set of lists of integers. Each list encodes a goal state, by enumerating the indeces of the features which should be on (i.e. = 1) in that goal state.
    protected Set<SortedSet<Integer>> goal_states = new HashSet<SortedSet<Integer>>();
    protected List<String> words = new ArrayList<String>();

    protected static Logger logger = Logger.getLogger(GeneratorLearner.class);

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

    // AA: copied from core/src/babble/dialog/rl/TTRMDPStateEncoding.java
    // AA's doc: gives you a list of the decomposed features that you want to add to add to the table.

    /**
     * no mcs here. adds features independently. Just decomposes and adds the
     * rec types. This is a looser notion of goal..... very unprincipled at this
     * minute!  // todo add a clear comment!
     *
     * @param goal
     */
    public List<TTRRecordType> addAsGoal(TTRRecordType goal) {

//		List<TTRRecordType> goal_features = null;
        logger.trace("Adding " + goal);
        TTRRecordType successContext = abstractOutSlotValues(goal);
        logger.trace("Abstracted " + successContext);
        List<TTRRecordType> decomposition = goal.decompose();

        SortedSet<Integer> goalIndeces = new TreeSet<Integer>();
        // int initSize=this.goal_ttr_features.size();
        int newCount = 0;
        List<TTRRecordType> newFeatures = new ArrayList<TTRRecordType>();
        decompLoop:
        for (int i = 0; i < decomposition.size(); i++) {
            TTRRecordType newFeature = decomposition.get(i);  // AA: You could just iterate without index: for(TTRRecordType newFeature: decomposition)

            for (int j = 0; j < goal_features.size(); j++) { // AA: columns of the table
                TTRRecordType existingFeature = goal_features.get(j);
                newFeature.resetMetas();
                existingFeature.resetMetas();
                if ((!newFeature.getMetas().isEmpty() && existingFeature.getMetas().isEmpty())  // AA: What is this doing?
                        || (newFeature.getMetas().isEmpty() && !existingFeature.getMetas().isEmpty()))
                    continue;
                else if (existingFeature.subsumes(newFeature)) {
                    logger.trace("Matched existing feature:" + existingFeature);
                    // if (j>=initSize)
                    goalIndeces.add(j + 2);  // first and second indeces reserved for pointed type and floor status
                    continue decompLoop;
                } else
                    logger.trace(existingFeature + " didn't subsume " + newFeature);
            }
            logger.trace("NO MATCH FOR:" + newFeature);
            logger.trace("ADDING AT END");
            newCount++;
            newFeatures.add(newFeature);
            goal_features.add(newFeature);
            goalIndeces.add(goal_features.size() + 1);  // first and second indeces reserved for pointed type and floor status
        }

        this.goal_states.add(goalIndeces);
        logger.trace("Added Goal:" + goalIndeces);
        logger.info("There were " + newCount + " new goal features in this dialogue.");
        for (TTRRecordType f : newFeatures) {
            logger.info(f);
        }
        return goal_features;
    }

    // AA: copied from core/src/babble/dialog/rl/TTRMDPStateEncoding.java

    /**
     * Abstracts out the slot values according to slot_values - replaces them by
     * the right kinds of meta-variable. (PredicateMetaVariable or
     * FormulaMetavariable)
     *
     * @param rec
     * @return new record type with slot values abstracted
     */
    public TTRRecordType abstractOutSlotValues(TTRRecordType rec) {  // todo I don't understand this.

        TTRRecordType result = new TTRRecordType();

        for (TTRField f : rec.getFields()) {
            TTRField newF = new TTRField(f);
            if (f.getType() != null && f.getType() instanceof AtomicFormula) {
                AtomicFormula af = (AtomicFormula) f.getType();

                if (slot_values.contains(af.getName())) {

                    newF.setType(result.getFreshAtomicMetaVariable());
                }

            } else if (f.getType() != null && f.getType() instanceof PredicateArgumentFormula) {
                PredicateArgumentFormula paf = (PredicateArgumentFormula) f.getType();
                if (slot_values.contains(paf.getPredicate().getName()))
                    newF.setType(
                            new PredicateArgumentFormula(result.getFreshPredicateMetaVariable(), paf.getArguments()));
            }
            result.add(newF);
        }
        return result;
    }

    /**
     * Written by Arash Eshghi, then edited and added to code by Arash Ash.
     * Finds a mapping from semantics during parse and the ones used as features in ConditionalProbTable and
     * and returns the corresponding indecese (as the map).
     *
     * @param rInc
     * @return state
     */
    public List<Integer> mapFeatures(TTRFormula rInc) {
        List<Integer> goalIndeces = new ArrayList<Integer>();
        HashMap<Variable, Variable> map = new HashMap<Variable, Variable>();
        logger.trace(rInc);

        for (int i = 0; i < goal_features.size(); i++) {
            TTRFormula feature = goal_features.get(i);
            feature.resetMetas();
            logger.trace(feature + " -> "); // what does this mean??#
            if (feature.subsumesMapped(rInc, map)) {
                logger.trace(feature + " added.");
                goalIndeces.add(i);
            }
            feature.resetMetas();  // AA: Why resetting metas twice? (6 lines before!))
            logger.trace("map is: " + map);  // AA: The map is not being updated? [apparantly it is :/]
        }
        return goalIndeces;
    }


    public HashMap<Variable, Variable> myMapFeatures(TTRFormula r1, TTRFormula r2) {
//        List<Integer> goalIndeces = new ArrayList<Integer>();
        HashMap<Variable, Variable> map = new HashMap<Variable, Variable>();
        logger.trace(r1);

//        for (int i = 0; i < goal_features.size(); i++) {
//            TTRFormula r2 = goal_features.get(i);
        r2.resetMetas();
        logger.trace(r2 + " -> "); // what does this mean??
        if (r1.subsumesMapped(r2, map)) {
            logger.trace(r2 + " added.");
//                goalIndeces.add(i);
        }
        r2.resetMetas();  // AA: Why resetting metas twice? (6 lines before!))
        logger.info("map is: " + map);
//        }
        return map;
    }


    /**
     * Creates a 2D hashmap as the count table and initialises counts to 0.
     */
    public void initialiseCountTable() {
        // think about this: maybe it's not efficient to make a table and then addd ot it, and it's better to add to table on the fly?
        // First, gather all the possible semantic features from data.
        for (Pair<Sentence<Word>, TTRRecordType> pair : corpus) {
            Sentence<Word> sentence = pair.first();
            TTRRecordType goldSem = pair.second();
//            goldSem = goldSem.removeHead();
            goldSem = goldSem.removeHeadIfManifest();
            addAsGoal(goldSem);
            for (Word word : sentence) // todo Does this add duplicate words?
                words.add(word.toString());
        }
        // Then add them as  columns in the table.
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            HashMap<TTRRecordType, Integer> row = new HashMap<TTRRecordType, Integer>();
            for (TTRRecordType rt : goal_features)
                row.put(rt, 0);
            conditionalCountTable.put(word, row);
            logger.trace("Added row " + i + " to conditionalCountTable: " + row);
        }
    }

    public void printCountTable() { //todo
        System.out.println();
        for (String w : words) {

            for (TTRRecordType rt : goal_features) {

            }
        }
    }


    /**
     * Normalises a table by dividing elements in a column by their sum.
     * Pass addKSmoothing =0 to have no smoothing.
     *
     * @param table the table to be normalised
     * @return probTable  the normalised table
     */
    public void normaliseCountTable(HashMap<String, HashMap<TTRRecordType, Integer>> table, int addKSmoothing, boolean log_prob) {
        // modify so it works with the global tables, not amke local ones and then pass them!
        // todo why pass int-table, when I can access conditionalCountTable? also may need refactoring to use goal_features over `row`.
        //todo also no need to return a new table, just update the one we have.
        HashMap<String, HashMap<TTRRecordType, Double>> probTable = new HashMap<String, HashMap<TTRRecordType, Double>>();
        HashMap<TTRRecordType, Double> total = new HashMap<TTRRecordType, Double>(); // Don't have to init to zero since I'm using getOrDefault method.
        // First find total of each column in this loop
        for (String word : words) {
            HashMap<TTRRecordType, Integer> row = conditionalCountTable.get(word);
            for (TTRRecordType feature : goal_features) {
                Integer count = row.get(feature);
                row.put(feature, count + addKSmoothing);
                total.put(feature, total.getOrDefault(feature, 0.0) + count + addKSmoothing); // If `feature` was already in `total`, add `count` to the previous value.
                // If not, add `count` to 0, which means just put `count`. Used because key might not be available.
            }
        }

        for (String word : words) {  // Divide columns by the corresponding `total` to get probabilities and save them in `conditionalProbTable`.
            HashMap<TTRRecordType, Integer> row = conditionalCountTable.get(word);
            HashMap<TTRRecordType, Double> probRow = new HashMap<TTRRecordType, Double>();
            for (TTRRecordType rt : goal_features) { //todo probably need to update this to use goal_features?
                Double prob = ((double) row.get(rt)) / total.get(rt); // do I need to do casting?
                if (log_prob)
                    prob = Math.log(prob);
                probRow.put(rt, prob);
            }
            conditionalProbTable.put(word, probRow);
        }
    }

    /**
     * Saves a HashMap<String, HashMap<TTRRecordType, Double>> to a csv file.
     *
     * @param model The 2D HashMap to be saved.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void saveModelToFile(HashMap<String, HashMap<TTRRecordType, Double>> model)
            throws FileNotFoundException, IOException // eclipse recommended it so I said yes.
    {
        // TODO write features: WORDS, feature1, feature2, ...
        // TODO Make more efficient
        // First, clear the file if it was already there. RF: https://stackoverflow.com/questions/6994518/how-to-delete-the-content-of-text-file-without-deleting-itself
        File f = new File(grammarPath + "model.csv");
        if (f.exists() && !f.isDirectory()) {
            PrintWriter writer = new PrintWriter(f);
            writer.print("");
            writer.close();
        }
        // Writing features row
        ArrayList<String> featuresStr = new ArrayList<String>();
        for (String wo : model.keySet()) { // This is not clean code.
            HashMap<TTRRecordType, Double> row = model.get(wo);
            ArrayList<TTRRecordType> features = new ArrayList<>(row.keySet());
            for (TTRRecordType feature : features) // todo make this more efficient
                featuresStr.add(feature.toString());
            break;
        }
        String strFeatures = String.join(",", featuresStr);
        strFeatures = "WORDS\\FEATURES" + "," + strFeatures + "\n";
        FileWriter writer1 = new FileWriter(grammarPath + "model.csv", true);
        writer1.write(strFeatures);
        writer1.close();

        // Writing words and probs
        int row_num = 0;
        for (var entry : model.entrySet()) // REF: https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
        {
            String word = entry.getKey();
            HashMap<TTRRecordType, Double> row = entry.getValue();
            ArrayList<Double> probs = new ArrayList<>(row.values());
            ArrayList<String> probsStr = new ArrayList<String>();
            for (Double prob : probs) // todo make this more efficient
                probsStr.add(Double.toString(prob));

            // Converts the list to a string joined by comma
            String strRow = String.join(",", probsStr); //REF: https://mkyong.com/java/java-how-to-join-list-string-with-commas/
            strRow = word + "," + strRow;
            // write to file
            try { // Do I need to use BufferedWriter as in https://stackoverflow.com/a/1625263/6306387 ?
                FileWriter writer = new FileWriter(grammarPath + "model.csv", true);
                writer.write(strRow);
                writer.write(System.lineSeparator()); // To go to next line. REF: https://stackoverflow.com/questions/18549704/create-a-new-line-in-javas-filewriter
                writer.close();
                System.out.printf("Successfully wrote row %d to the file.\n", row_num); // todo add logs
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
        // todo modify Lexicon constructor so we can pass default top-n value.
        initialiseCountTable();
        // todo change corpus here to the clean one.
        for (Pair<Sentence<Word>, TTRRecordType> pair : corpus) {
            parser.init();  // Restarts parser
            Sentence<Word> sentence = pair.first();
            TTRRecordType rG = pair.second();
            Boolean parsed = parser.parseUtterance(new Utterance(sentence));  // Converting Sentence to Utterance to be able to parse it.
            while (parsed) {
                TTRRecordType finalSem = (TTRRecordType) parser.getState().getCurrentTuple().getSemantics();
                finalSem = finalSem.removeHeadIfManifest();
//                    rG = rG.removeHead();
                rG = rG.removeHeadIfManifest();
//                    goldSem = goldSem.removeHead();
                if (rG.subsumes(finalSem) && finalSem.subsumes(rG)) { // `true` if the sem from parser matches the one in the dataset.
                    Context<DAGTuple, GroundableEdge> context = (Context<DAGTuple, GroundableEdge>) parser.getContext();
//                        List<GroundableEdge> pathToRoot  = cont.getDAG().getSequenceToRoot();
                    DAG<DAGTuple, GroundableEdge> dag = context.getDAG();
                    DAGTuple curTuple = dag.getCurrentTuple();
                    GroundableEdge curEdge = dag.getParentEdge(curTuple);
                    curTuple = dag.getParent(curTuple); // init to correct tuple, one before the last.
                    while (curEdge != null) {
                        TTRRecordType rCur = (TTRRecordType) curTuple.getSemantics();
//                            rCur = rCur.removeHead();
                        rCur = rCur.removeHeadIfManifest();
                        String word = curEdge.word().word();
//                                    Node pointedNode = parser.getState().getCurrentTuple().getTree().getPointedNode();
//                                    DSType pointedNodeType = pointedNode.getType();
//                                    DSType motherNodeType = parser.getState().getCurrentTuple().getTree().getLocalRoot(pointedNode).getType();  // todo is getLocalRoot correct to get the mother node?
                        // todo add two tree features as well + should I use dag here, or parser is fine?
                        TTRRecordType rInc = finalSem.subtract(rCur, new HashMap<Variable, Variable>());  // todo it doesn't make sense to me that I have to pass an empty map to this!
                        System.out.println("rG: " + finalSem + " MINUS rCur: " + rCur + " EQUALS rInc: " + rInc);
                        logger.trace("rG: " + finalSem + " MINUS rCur: " + rCur + " EQUALS rInc: " + rInc);
                        HashMap<TTRRecordType, Integer> row = conditionalCountTable.get(word);
                        List<Integer> indeces = mapFeatures(rInc);
                        for (Integer idx : indeces) {
                            TTRRecordType correspondingFeature = goal_features.get(idx);
                            row.put(correspondingFeature, row.get(correspondingFeature) + 1);
                        }
                        curEdge = dag.getParentEdge(curTuple);
                        curTuple = dag.getParent(curTuple); // AA: Updating curTuple to the previous one (until we get to root).
                    }
                    break; // To prevent checking other parses after finding the correct one.
                }
                parsed = parser.parse();
            }
        }
        normaliseCountTable(conditionalCountTable, K_SMOOTHING, false);
        saveModelToFile(conditionalProbTable);  // todo asked for some unhandled exception, I just added it. IS THAT OK?
    }

}
