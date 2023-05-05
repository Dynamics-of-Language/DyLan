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
import qmul.ds.tree.Node;
import qmul.ds.type.DSType;

// TODO: save modelPath as a constant string so I can access it in BFG class by importing it from here.
// todo add support for Feature everywhere basically.

public class GeneratorLearner {

    // ====================== fields ======================
    protected DAGParser<? extends DAGTuple, ? extends DAGEdge> parser;  //todo why this is not ICP?
    RecordTypeCorpus corpus = new RecordTypeCorpus();

    protected TreeMap<String, TreeMap<Feature, Integer>> conditionalCountTable = new TreeMap<>();
    protected TreeMap<String, TreeMap<Feature, Double>> conditionalProbTable = new TreeMap<>(); // TODO attention: these are NOT being globally updated.
    static final String corpusFolderPath = "dsttr/corpus/CHILDES/eveTrainPairs/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static final String corpusPath = corpusFolderPath + "LC-CHILDESconversion396FinalCopy.txt";//"AAtrain.txt";
    static final String grammarPath = "dsttr/resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static public String[] DSTypesString = new String[]{"e", "t", "cn", "e>t", "cn>e", "e>cn", "e>(e>t)", "e>(t>t)", "e>(e>(e>t))"};
    static final int DSTypesCount = DSTypesString.length*2;
    static final Integer K_SMOOTHING = 1;
    static final boolean LOG_PROB = true;

    // copied from: core/src/babble/dialog/rl/TTRMDPStateEncoding.java
    // Semantic features of the goal, i.e. the grounded content, that are to be tracked by the mdp
    protected TreeSet<Feature> goal_features = new TreeSet<>();//Collections.reverseOrder());  // AA: Shouldn't this be "new List<xxx>(); ? what's the difference?" because in your code it was null, and I changed it since I  was getting an error.
    protected List<String> slot_values = new ArrayList<String>();
    // a set of lists of integers. Each list encodes a goal state, by enumerating the indeces of the features which should be on (i.e. = 1) in that goal state.
//    protected Set<String> words = new HashSet<>();
    protected HashMap<String, Integer> words = new HashMap<>(); // todo check for the usage

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

    // AA: copied from core/src/babble/dialog/rl/TTRMDPStateEncoding.java
    // AA's doc: gives you a list of the decomposed features that you want to add to add to the table.

    /**
     * no mcs here. adds features independently. Just decomposes and adds the
     * rec types. This is a looser notion of goal..... very unprincipled at this
     * minute!  // todo add a clear comment!
     *
     * @param goal
     */
    public void addAsGoal(TTRRecordType goal) {

//		List<TTRRecordType> goal_features = null;
        logger.trace("Adding " + goal);
//        TTRRecordType successContext = abstractOutSlotValues(goal);
//        logger.trace("Abstracted " + successContext);
//        List<TTRRecordType> decomposition = successContext.decompose();
        List<TTRRecordType> decomposition = goal.decompose();
        HashMap<Variable, Variable> map = new HashMap<>();
        SortedSet<Integer> goalIndeces = new TreeSet<Integer>();
        List<TTRRecordType> newFeatures = new ArrayList<TTRRecordType>();
        decompLoop:
        for (TTRRecordType newFeature : decomposition) {
            for (Feature existingFeature : goal_features) { // AA: columns of the table
                HashMap<Variable, Variable> newMap = new HashMap<>(map);
                if (existingFeature.rt.subsumesMapped(newFeature, map)) { // is this correct? I think it is because when we are calling this method, we don't yet have the DSTree features.
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

//            newFeature.resetMetas();
//            newFeature = newFeature.freshenVars(newFeature, map);
            TTRRecordType mappedNewFeature = newFeature.relabel(map);
            newFeatures.add(mappedNewFeature);
            goal_features.add(new Feature(mappedNewFeature));
            logger.trace("Adding feature:" + mappedNewFeature);
            logger.trace(ANSI_CYAN+"goal features: " + goal_features+ANSI_RESET);
            goalIndeces.add(goal_features.size() + 1);  // first and second indeces reserved for pointed type and floor status
        }

//        this.goal_states.add(goalIndeces);
//        logger.trace("Added Goal:" + goalIndeces);
//        logger.info("There were " + newCount + " new goal features in this dialogue.");
        for (TTRRecordType f : newFeatures) {
            logger.info("Newly added feature is: " + f);
        }
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
            TTRField newF = new TTRField(f); // I think this only makes a copy.
            System.out.println(f.getType());
            if (f.getType() != null && f.getType() instanceof AtomicFormula) { // AA TODO: You are calling getType 3 times here. Wasn't it better to cache it?
                AtomicFormula af = (AtomicFormula) f.getType();
                System.out.println(af.getName());
                if (slot_values.contains(af.getName())) {
                    newF.setType(result.getFreshAtomicMetaVariable()); // I think the problem is it's not going inside here. Check if we ever get here.
                }
                // otherwise update slot_values
//                else {
//                    slot_values.add(af.getName());
//                }
            }
            else if (f.getType() != null && f.getType() instanceof PredicateArgumentFormula) {
                PredicateArgumentFormula paf = (PredicateArgumentFormula) f.getType();
                if (slot_values.contains(paf.getPredicate().getName()))
                    newF.setType(new PredicateArgumentFormula(result.getFreshPredicateMetaVariable(), paf.getArguments()));
//                else
//                    slot_values.add(paf.getPredicate().getName());
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
    public List<TTRRecordType> mapFeatures(TTRFormula rInc) { //todo -------------------------------- only ttr
        List<TTRRecordType> mappedFeatures = new ArrayList<>();
        HashMap<Variable, Variable> map = new HashMap<>();
        logger.trace(rInc);
        ArrayList<TTRRecordType> onlySemanticsFeatures = new ArrayList<>();
        for (Feature feature : goal_features) {
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


    // Calculates the probability of words by dividing their count by total count and writes results to file.
    public void calculateProbabilities(Integer totalWordsCount) {
        for (String word : words.keySet()) {
            double prob = (double) words.get(word) / (double) totalWordsCount;
            String line = String.format("%s\t%f", word, prob);
        }
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
//            goldSem = goldSem.removeHead();
            goldSem = goldSem.removeHeadIfManifest();
            addAsGoal(goldSem);
            for (Word word : sentence) { // todo Does this add duplicate words?
                String w = word.toString();
                Integer count = words.getOrDefault(w, 0) + 1;
                words.put(w, count);
                totalWordsCount += count;
            }
            calculateProbabilities(totalWordsCount);
        }

        // Now same thing for DSTree features and corresponding requirement types:
        for(String typeStr: DSTypesString){
            DSType dsType = DSType.parse(typeStr);
            Feature dsTypeAsFeature = new Feature(dsType, false);
            Feature reqTypeAsFeature = new Feature(dsType, true);
            goal_features.add(dsTypeAsFeature);
            logger.info("Added DSType " + dsTypeAsFeature);
            logger.info("goal features (size: " + goal_features.size() + "): " + goal_features);
            goal_features.add(reqTypeAsFeature);
            logger.info("Added reqType " + reqTypeAsFeature);
            logger.info("goal features (size: " + goal_features.size() + "): " + goal_features);
        }

        // Then add them as columns in the table.
        for (String word: words.keySet()) {
            TreeMap<Feature, Integer> row = new TreeMap<>();
            for (Feature f : goal_features)
                row.put(f, 0);
            conditionalCountTable.put(word, row);
//            logger.trace("Added row " + i + " to conditionalCountTable: " + row);
        }
        logger.info("goal_features at the end of initialiseCountTable: " + goal_features);
    }

    public void printCountTable() {
        for (String word : words.keySet()) {
            TreeMap<Feature, Integer> row = conditionalCountTable.get(word);
            for (Feature feature: goal_features) {
                Integer count = row.get(feature);
                logger.trace(word + "\t" + feature + "\t" + count);
            }
        }
    }


    /**
     * Normalises a table by dividing elements in a column by their sum.
     * Pass addKSmoothing=0 to have no smoothing.
     *
     * @param addKSmoothing value of K for addKSmoothing. Set the global variable `K_SMOOTHING`.
     * @param log_prob if we want to save log_prob or prob
     */
    public void normaliseCountTable(int addKSmoothing, boolean log_prob) throws IOException {
        FileWriter writer = new FileWriter(grammarPath + File.separator+"wordProbs.tsv", false);
        HashMap<String, Double> wordsProb = new HashMap<>();
        HashMap<Feature, Double> total = new HashMap<>(); // Don't have to init to zero since I'm using getOrDefault method.
        Double totalCount = 0.0; // to calculate wordsProb
        // First, find total sum of each column in this loop:
        for (String word : words.keySet()) {
            TreeMap<Feature, Integer> row = conditionalCountTable.get(word);
            for (Feature feature : goal_features) {
                Integer count = row.get(feature);
                wordsProb.put(word, wordsProb.getOrDefault(word, 0.0) + (double)count);
                totalCount += count;
                row.put(feature, count + addKSmoothing);
                total.put(feature, total.getOrDefault(feature, 0.0) + count + addKSmoothing); // todo I don't think if addk here is correct
                // If not, add `count` to 0, which means just put `count`. Used because key might not be available.
            }
        }
        // Then divide columns by the corresponding `total` to get probabilities and save them in `conditionalProbTable`.
        for (String word : words.keySet()) {
            TreeMap<Feature, Integer> row = conditionalCountTable.get(word);
            TreeMap<Feature, Double> probRow = new TreeMap<>();
            for (Feature feature : goal_features) { //todo probably need to update this to use goal_features?
                    Double prob = ((double) row.get(feature)) / total.get(feature); // do I need to do casting?
                if (log_prob)
                    prob = Math.log(prob);
                probRow.put(feature, prob);
            }
            conditionalProbTable.put(word, probRow);
            // AA: Write wordsprob to file
            Double wordProb = wordsProb.get(word) / totalCount;
            if (log_prob)
                wordProb = Math.log(wordProb);
            wordsProb.put(word, wordProb); // not going to use the map really after this. basically updating for no reason.
            String line = word + "\t" + wordProb + "\n";
            writer.write(line);
        }
        writer.close();
    }

    /**
     * Saves a HashMap<String, HashMap<TTRRecordType, Double>> to a csv file.
     *
     * @param model The 2D HashMap to be saved.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void saveModelToFile(TreeMap<String, TreeMap<Feature, Double>> model)
            throws FileNotFoundException, IOException // eclipse recommended it so I said yes.
    {
        // TODO Make more efficient
        // First, clear the file if it was already there. RF: https://stackoverflow.com/questions/6994518/how-to-delete-the-content-of-text-file-without-deleting-itself
        File f = new File(grammarPath + "genLearnedModel.csv");
        if (f.exists() && !f.isDirectory()) {
            PrintWriter writer = new PrintWriter(f);
            writer.print("");
            writer.close();
        }
        // Writing features row
        ArrayList<String> featuresStr = new ArrayList<String>();
//        for (String wo : model.keySet()) { // This is not clean code.
//            HashMap<TTRRecordType, Double> row = model.get(wo);
//            ArrayList<TTRRecordType> features = new ArrayList<>(row.keySet());
            for (Feature feature : goal_features) // todo make this more efficient
                featuresStr.add(feature.toString());
//            break;
//        }
        String strFeatures = String.join(" , ", featuresStr);
        strFeatures = "[WORDS\\FEATURES]" + " , " + strFeatures + "\n";
        FileWriter writer1 = new FileWriter(grammarPath + "genLearnedModel.csv", true);
        writer1.write(strFeatures);
        writer1.close();

        // Writing words and probs
        int row_num = 0;
        for (String word: model.keySet()) // REF: https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
        {
//            String word = entry.getKey();
//            ArrayList<Double> probs = new ArrayList<>();
            ArrayList<String> probsStr = new ArrayList<String>();
            TreeMap<Feature, Double> row = model.get(word);
            for (Feature feature: goal_features){
                Double prob = row.get(feature);
                probsStr.add(Double.toString(prob));
            }
//            ArrayList<Double> probs = new ArrayList<>();
//            ArrayList<String> probsStr = new ArrayList<String>();
//            for (Double prob : probs) // todo make this more efficient
//                probsStr.add(Double.toString(prob));

            // Converts the list to a string joined by comma
            String strRow = String.join(" , ", probsStr); //REF: https://mkyong.com/java/java-how-to-join-list-string-with-commas/
            strRow = word + " , " + strRow;
            // write to file
            try { // Do I need to use BufferedWriter as in https://stackoverflow.com/a/1625263/6306387 ?
                FileWriter writer = new FileWriter(grammarPath + "genLearnedModel.csv", true);
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
        logger.info("features length: " + goal_features.size());
        logger.info("table features: " + goal_features);
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
                        Node pointedNode = parser.getState().getCurrentTuple().getTree().getPointedNode(); // TODO ----------------------------------------------------------------------
                        Feature pointedNodeFeature = new Feature(pointedNode.getType());
//                        DSType motherNodeType = parser.getState().getCurrentTuple().getTree().getLocalRoot(pointedNode).getType();  // todo is getLocalRoot correct to get the mother node? + deal with this later.
                        TTRRecordType rInc = finalSem.subtract(rCur, new HashMap<>());
                        logger.trace("rG: " + finalSem + " MINUS rCur: " + rCur + " EQUALS rInc: " + rInc);
                        TreeMap<Feature, Integer> row = conditionalCountTable.get(word);
                           List<TTRRecordType> mappedFeatures = mapFeatures(rInc);

                        for (TTRRecordType correspondingFeature: mappedFeatures) { // Updates the CountTable with semantic features.
                            Feature f = new Feature(correspondingFeature);
                            row.put(f, row.get(f) + 1);
                        }
                        row.put(pointedNodeFeature, row.get(pointedNodeFeature)+ 1); // Updates the CountTable with syntactic features.
                        curEdge = dag.getParentEdge(curTuple);
                        curTuple = dag.getParent(curTuple); // AA: Updating curTuple to the previous one (until we get to root).
                    }
                    break; // To prevent checking other parses after finding the correct one.
                }
                parsed = parser.parse();
            }
        }
        normaliseCountTable(K_SMOOTHING, LOG_PROB);
        System.out.println(goal_features);
        saveModelToFile(conditionalProbTable);  // todo asked for some unhandled exception, I just added it. IS THAT OK?
        // todo there is a BUG in writeToFile: if you re-run it, it will append to the file, not overwrite it.
    }

}
