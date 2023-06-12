package qmul.ds;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

import qmul.ds.formula.TTRRecordType;
import qmul.ds.formula.Variable;
import qmul.ds.learn.Feature;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Requirement;
import qmul.ds.tree.label.TypeLabel;
import qmul.ds.type.DSType;

public class InteractiveProbabilisticGenerator extends BestFirstGenerator {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RED = "\u001B[31m";

    final static String grammarPath = "resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static String modelFileName = "top-1-400_11-6-2023_model.csv";
    static String wordProbFileName = "wordProbs.tsv";
    protected TreeMap<String, TreeMap<Feature, Double>> model;
    protected HashMap<String, Double> wordProbs;
    protected TreeSet<Feature> goalFeatures;

    final boolean useDSTypes = true; //GeneratorLearner.useDSTypes; // todo make it use the one in the learner

    protected static org.apache.log4j.Logger logger = Logger.getLogger(InteractiveProbabilisticGenerator.class);

// -------------------- constructors --------------------

    /**
     * @param resourceDirNameOrURL
     */
    public InteractiveProbabilisticGenerator(String resourceDirNameOrURL, String modelPath) {
        super(resourceDirNameOrURL);
        try {
            loadModelFromFile(modelPath);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     * Written by Arash Eshghi, then edited and added to code by Arash Ash.
     * Finds a mapping from semantics during parse and the ones used as features in ConditionalProbTable and
     * and returns the corresponding indecese (as the map).
     *
     * @param rInc
     * @return state
     */
    public List<TTRRecordType> mapFeatures(TTRRecordType rInc) {
        List<TTRRecordType> mappedFeatures = new ArrayList<>();
        HashMap<Variable, Variable> map = new HashMap<Variable, Variable>();
        logger.trace(rInc);
        ArrayList<TTRRecordType> onlySemanticsFeatures = new ArrayList<>();
//        for (Feature feature : goal_features) {
//            if (feature.rt != null)
//                onlySemanticsFeatures.add(feature.rt);
//        }
        for (Feature f : goalFeatures) {

            if (f.rt != null) {
                TTRRecordType feature = f.rt;
                feature.resetMetas();
                logger.trace(feature + " -> "); // what does this mean??#
                if (feature.subsumesMapped(rInc, map)) { // or is it the other way?
                    logger.trace(feature + " added to map.");
                    mappedFeatures.add(feature);
                }
                feature.resetMetas();  // AA: todo Why resetting metas twice? (6 lines before!))
                logger.trace("map is: " + map);  // AA: The map is not being updated? [apparantly it is :/]
            }
        }
        return mappedFeatures;
    }


    public Feature getPointedNodeFeature() {
        Feature pointedNodeFeature;

        Tree curTree = this.parser.getContext().getDAG().getCurrentTuple().getTree();
        logger.debug(ANSI_YELLOW+"cur Tree: " + curTree +ANSI_RESET);
        Node pointedNode = curTree.getPointedNode();
        DSType t = pointedNode.getType();
        Requirement req = pointedNode.getTypeRequirement();
        logger.debug(ANSI_YELLOW+"pointed Node type is: " + t+ANSI_RESET);
        if (t != null) {
            pointedNodeFeature = new Feature(t);
        } else if (req != null) {
            TypeLabel tl = (TypeLabel) req.getLabel();
            t = tl.getType();
            pointedNodeFeature = new Feature(t, true);
//            logger.info(ANSI_YELLOW+"pointed Node Feature is: " + pointedNodeFeature+ANSI_RESET);
        } else {
            logger.error("pointedNodeFeature is null");
            throw new IllegalStateException("pointedNodeFeature is null");
        }
        logger.debug(ANSI_YELLOW+"pointed Node Feature is: " + pointedNodeFeature+ANSI_RESET);
        return pointedNodeFeature;
    }


    protected void loadWordProbsFromFile() {
        wordProbs = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(grammarPath + wordProbFileName));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                wordProbs.put(parts[0], Double.parseDouble(parts[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("probability of words map: " + wordProbs);
    }


    public List<String> chooseTopWords(HashMap<String, Double> allProbs) {
        // returns the top beamSize words based on their probabilities
        //TODO make this treemap too, so always you pick top three elements and return them, it's sorted by prob value
        // I want the probs to be printed too. This list isn't cool.
        List<String> topWords = new ArrayList<>();
        HashMap<String, Double> topWordProbs = new HashMap<>();
        for (int i = 0; i < beam; i++) {
            String maxWord = allProbs.keySet().toArray()[0].toString(); // Initialising to the first element.
            Double maxProb = allProbs.get(maxWord);
            for (String w : allProbs.keySet()) {
                if (allProbs.get(w) > maxProb) {
                    maxProb = allProbs.get(w);
                    maxWord = w;
                }
            }
            topWords.add((maxWord));
            topWordProbs.put(maxWord, maxProb);
            allProbs.remove(maxWord);
        }
        logger.info(ANSI_YELLOW + "Top words are: " + ANSI_RESET);
        for (String w : topWords)
            logger.info(ANSI_YELLOW + w + ": " + topWordProbs.get(w) + ANSI_RESET);
        return topWords;
    }


    @Override
    public List<String> populateBeam() {

        TTRRecordType rCur = (TTRRecordType)this.getParser().getContext().getDAG().getCurrentTuple().getSemantics().removeHeadIfManifest();  // How expensive is this operation?
        TTRRecordType rInc = ((TTRRecordType)this.goal).subtract(rCur, new HashMap<>());
        List<TTRRecordType> mappedFeatures = mapFeatures(rInc);
//        List<WordLogProb> candidates = populateBeam(mappedFeatures);


        // looks into the current search space and picks the top beam candidates based on the probability of words given their semantics. Then returns the top beam candidates.
        // ATTENTION: This method assumes using log probabilities, therefore the probabilities are added instead of multiplied.
        HashMap<String, Double> allProbs = new HashMap<>();
        Feature dsTypeFeature = getPointedNodeFeature(); // SHOULD this go below the "if useDSTypes" block?
        logger.info("pointed node: " + dsTypeFeature);

        for (String w : model.keySet()) {
            TreeMap<Feature, Double> row = model.get(w);
            Double probSum = 0.0;
            for (TTRRecordType r : mappedFeatures) {
                probSum += row.get(new Feature(r));
                logger.debug("word: " + w + ", feature: " + new Feature(r) + ", prob: " + row.get(new Feature(r)));
            }
            if (useDSTypes) { // I think I have to do this: see what type is required, get the prob of that for all the words and hope this helps for picking the right word.
                probSum += row.get(dsTypeFeature); // TODO TEST
                logger.info("word: " + w + ", feature: " + dsTypeFeature + ", prob: " + row.get(dsTypeFeature));
            }
            probSum += wordProbs.get(w); // Adds the probability of the word itself.
            allProbs.put(w, probSum); // choose a better name over allProbs.
        }
        // pick top beamSize words from allProbs and return them as candidates.
        for(String w: allProbs.keySet())
            logger.debug("word: " + w + ", prob: " + allProbs.get(w));
//        System.out.println("allProbs: " + allProbs);
        List<String> candidates = chooseTopWords(allProbs);
        return candidates;
    }


    public void loadModelFromFile(String grammarPath) throws IOException, ClassNotFoundException {
        this.model = new TreeMap<>();
        this.goalFeatures = new TreeSet<>();
        int lineNumber = 0;
        FileReader reader = new FileReader(grammarPath + modelFileName);
        BufferedReader stream = new BufferedReader(reader);
//        TreeSet<Feature> features = new TreeSet<>();
        String line;
        while ((line = stream.readLine()) != null) {
            String lineList[] = line.split(" , ");
            if (lineNumber == 0) { // Controls reading features line
                for (int i = 1; i < lineList.length; i++) { // Ignoring the first element since it's "WORDS\FEATURES"
                    // This assumes there is at least one DSType feature.
                    String featureStr = lineList[i];
                    Feature newFeature;
                    if (featureStr.startsWith("?"))
                        newFeature = new Feature(DSType.parse(featureStr.substring(1)), true);
                    else if (featureStr.startsWith("["))
                        newFeature = new Feature(TTRRecordType.parse(featureStr));
                    else
                        newFeature = new Feature(DSType.parse(featureStr));

//                    features.add(newFeature);
                    goalFeatures.add(newFeature);
                    logger.debug("Loaded feature \"" + newFeature + "\" from file.");
                    lineNumber++;
                }
                logger.info(ANSI_GREEN + "Loaded " + goalFeatures.size() + " features from file." + ANSI_RESET);
                logger.debug(ANSI_GREEN + "Features are: " + goalFeatures + ANSI_RESET);
            } else {  // Otherwise we have a <WORD,PROBS> line.
                String word = lineList[0];
                TreeMap<Feature, Double> row = new TreeMap<>();
                ArrayList<Double> probs = new ArrayList<>();
                for (int i = 1; i < lineList.length; i++) {
                    Double prob = Double.parseDouble(lineList[i]);
                    probs.add(prob);
                }


                ArrayList<Feature> featuresList = new ArrayList<>(goalFeatures); // AA: Doing this to be able to align the features with the probs, just ot have an index.
                for (int i = 0; i < featuresList.size(); i++) {
                    Feature f = featuresList.get(i);
                    Double p = probs.get(i);
                    row.put(f, p);
                    logger.debug("Word \"" + word + "\" Feature \"" + f + "\" Prob " + p + " loaded.");
                }
                this.model.put(word, row);
                logger.info("Word \"" + word + "\" loaded from model.");
                lineNumber++;
            }
        }
        logger.info(ANSI_GREEN + "Model loaded successfully from file with size: " + model.size() + ANSI_RESET);

        loadWordProbsFromFile();
        logger.info(ANSI_GREEN + "Word probabilities loaded successfully from file with size: " + wordProbs.size() + ANSI_RESET);
    }

    public static void main(String[] args){  // main method only for testing purposes.
        InteractiveProbabilisticGenerator ipg = new InteractiveProbabilisticGenerator(grammarPath, grammarPath);
        TTRRecordType goal = TTRRecordType.parse("[x1==that : e|e1==eq : es|p3==obj(e1, x1) : t|x==what : e|p2==subj(e1, x) : t|head==e1 : es]");
        ipg.setGoal(goal);
        ipg.generate();
    }
}
