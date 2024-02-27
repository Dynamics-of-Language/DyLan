/**
 *
 */
package qmul.ds;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;
import org.apache.log4j.Logger;
import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.formula.Variable;
import qmul.ds.learn.*;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Requirement;
import qmul.ds.tree.label.TypeLabel;
import qmul.ds.type.DSType;

/**
 * @author Arash Ashrafzadeh, Arash Eshghi
 */
public class InteractiveProbabilisticGeneratorOld extends BestFirstGenerator {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RED = "\u001B[31m";
    final static String grammarPath = "dsttr/resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static final String corpusFolderPath = "dsttr/corpus/CHILDES/eveTrainPairs/".replaceAll("/", Matcher.quoteReplacement(File.separator));
    static final String corpusPath = GeneratorLearner.corpusPath;//corpusFolderPath + "AAtrain-3.txt";//""AA-train-lower-396-matching-top1.txt"; //todo same corpus as leanrer
//    static final Integer BEAM = 3;
//    static final String[] DSTypesString = GeneratorLearner.DSTypesString;
//    static final int DSTypesCount = DSTypesString.length * 2;
    final boolean useDSTypes = true; //GeneratorLearner.useDSTypes; // todo make it use the one in the learner
    TreeMap<String, TreeMap<Feature, Double>> learnedModel;
    HashMap<String, Double> wordProbs;
    TreeSet<Feature> goalFeatures;
    protected static Logger logger = Logger.getLogger(InteractiveProbabilisticGeneratorOld.class);

    // ---------------------------------- Constructors ----------------------------------
    // TODO  missing the (File, beam) constructor, whatever it is

    /**
     * @param resourceDir
     */
    public InteractiveProbabilisticGeneratorOld(File resourceDir) {
        super(resourceDir);
    }


    /**
     * @param lexicon
     * @param grammar
     */
    public InteractiveProbabilisticGeneratorOld(Lexicon lexicon, Grammar grammar) {
        super(lexicon, grammar);
    }

    // TODO this doesn't exist in the mother class.
//    /**
//     * @param parser
//     */
//    public InteractiveProbabilisticGenerator(DAGParser<DAGTuple, GroundableEdge> parser) {
//        super(parser);
//    }


    /**
     * @param resourceDirNameOrURL
     */
    public InteractiveProbabilisticGeneratorOld(String resourceDirNameOrURL) {
        super(new File(resourceDirNameOrURL));
    }

    @Override
    public DAG<DAGTuple, GroundableEdge> getNewState(Tree start) { // TODO I don't know the purpose of this.
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DAGParser<DAGTuple, GroundableEdge> getParser(Lexicon lexicon, Grammar grammar) {
        return new InteractiveContextParser(lexicon, grammar);
    }


    public TreeMap<String, TreeMap<Feature, Double>> loadModelFromFile(String grammarPath) throws IOException, ClassNotFoundException {
        TreeMap<String, TreeMap<Feature, Double>> table = new TreeMap<>();
        int lineNumber = 0;
        FileReader reader = new FileReader(grammarPath + "genLearnedModel.csv");
        BufferedReader stream = new BufferedReader(reader);
        TreeSet<Feature> features = new TreeSet<>();
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

                    features.add(newFeature);
                    logger.debug("Loaded feature \"" + newFeature + "\" from file.");
                    lineNumber++;
                }
                logger.info(ANSI_GREEN+"Loaded " + features.size() + " features from file."+ANSI_RESET);
                logger.info(ANSI_GREEN+"Features are: " + features+ANSI_RESET);
            }
            else {  // Otherwise we have a <WORD,PROBS> line.
                String word = lineList[0];
                TreeMap<Feature, Double> row = new TreeMap<>();
                ArrayList<Double> probs = new ArrayList<>();
                for (int i = 1; i < lineList.length; i++) {
                    Double prob = Double.parseDouble(lineList[i]);
                    probs.add(prob);
                }
                ArrayList<Feature> featuresList = new ArrayList<>(features); // AA: Doing this to be able to align the features with the probs.
                for (int i = 0; i < featuresList.size(); i++) {
                    Feature f = featuresList.get(i);
                    Double p = probs.get(i);
                    row.put(f, p);
                    logger.debug("Word \"" + word + "\" Feature \"" + f + "\" Prob " + p + " loaded.");
                }
                table.put(word, row);
                logger.info("Word \"" + word + "\" loaded from model.");
                lineNumber++;
            }
        }
        return table;
    }


    public List<WordLogProb> chooseTopWords(HashMap<String, Double> allProbs) {
        // returns the top beamSize words based on their probabilities
        //TODO make this treemap too, so always you pick top three elements and return them, it's sorted by prob value
        List<WordLogProb> topWords = new ArrayList<>();
        for (int i = 0; i < beam; i++) {
            String maxWord = allProbs.keySet().toArray()[0].toString(); // Initialising to the first element.
            Double maxProb = allProbs.get(maxWord);
            for (String w : allProbs.keySet()) {
                if (allProbs.get(w) > maxProb) {
                    maxProb = allProbs.get(w);
                    maxWord = w;
                }
            }
            topWords.add(new WordLogProb(maxWord, maxProb));
            allProbs.remove(maxWord);
        }
        logger.info(ANSI_YELLOW + "Top words: " + topWords + ANSI_RESET);
        return topWords;
    }


    // TODO Here, I want to pass different arguments ot the method, so should I use generics (ref: https://stackoverflow.com/questions/49509625/abstract-function-with-different-parameters)?
//    @Override
    public List<WordLogProb> populateBeam(List<TTRRecordType> mappedFeatures) {
        // looks into the current search space and picks the top beam candidates based on the probability of words given their semantics. Then returns the top beam candidates.
        // ATTENTION: This method assumes using log probabilities, therefore the probabilities are added instead of multiplied.
        List<WordLogProb> candidates;
        HashMap<String, Double> allProbs = new HashMap<>();
        Feature dsTypeFeature = getPointedNodeFeature();

        for (String w : learnedModel.keySet()) {
            TreeMap<Feature, Double> row = learnedModel.get(w);
            Double probSum = 0.0;
            for (TTRRecordType r : mappedFeatures) {
                probSum += row.get(new Feature(r));
            }
            if (useDSTypes) // I think I have to do this: see what type is required, get the prob of that for all the words and hope this helps for picking the right word.
                probSum += row.get(dsTypeFeature); // TODO TEST

            probSum += wordProbs.get(w); // Adds the probability of the word itself.
            allProbs.put(w, probSum); // choose a better name over allProbs.
        }
        // pick top beamSize words from allProbs and return them as candidates.
        candidates = chooseTopWords(allProbs);
        return candidates;
    }


    public void addToOutputCorpus(String goldSentence, String rG, Sentence<Word> generatedSentence, List<String[]> corpusList, CorpusStats corpusStats) {
        String[] pair = new String[3];
        pair[0] = goldSentence;
        pair[1] = rG;
        pair[2] = generatedSentence.toString();
        corpusList.add(pair);
        corpusStats.addSentence(generatedSentence);
    }

    // added this as part of the refactoring.
    public TreeSet<Feature> getGoalFeatures(TreeMap<String, TreeMap<Feature, Double>> model) {
        // TODO not clean. read it from file maybe? or by the time you are reading from file, add them to goal_features?
        TreeSet<Feature> goalFeatures = new TreeSet<>();
        for (String word : model.keySet()) {
            TreeMap<Feature, Double> row = model.get(word);
            for (Feature feature : row.keySet()) {
                goalFeatures.add(feature); // todo listen to the suggestion.
                logger.info("Added feature " + feature);
            }
            break;
        }
        return goalFeatures;
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

            if(f.rt != null){
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


    public HashMap<String, Double> loadWordProbsFromFile(String wordProbsPath) {
        HashMap<String, Double> wordProbs = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(wordProbsPath));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                wordProbs.put(parts[0], Double.parseDouble(parts[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("probability of words map: " + wordProbs);
        return wordProbs;
    }


    public Feature getPointedNodeFeature() {
        Feature pointedNodeFeature;
        Node pointedNode = this.parser.getContext().getDAG().getCurrentTuple().getTree().getPointedNode();
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
        logger.info(ANSI_YELLOW+"pointed Node Feature is: " + pointedNodeFeature+ANSI_RESET);
        return pointedNodeFeature;
    }


    @Override
    public List<String> populateBeam() {
        return null;
    }


//    @Override
//    public boolean generateNextWord() { //todo
//        return false;
//    }


    /**
     * @return
     */
    @Override
    public boolean generate() {
        List<String[]> fullyGeneratedList = new ArrayList<>();
        List<String[]> partiallyGeneratedList = new ArrayList<>();
        CorpusStats fullyGeneratedStats = new CorpusStats();
        CorpusStats partiallyGeneratedStats = new CorpusStats();
        int absCorrect = 0; // these have to go to test class.

        try { // recommended by the IDE...
            learnedModel = loadModelFromFile(grammarPath);
            goalFeatures = getGoalFeatures(learnedModel);
            logger.info("goalFeatures: " + goalFeatures);
            wordProbs = loadWordProbsFromFile(grammarPath + File.separator + "wordProbs.tsv"); // not to be confused wordsLogProb; This is for global word probabilities.
            RecordTypeCorpus corpus = new RecordTypeCorpus();
            corpus.loadCorpus(new File(corpusPath));

            for (Pair<Sentence<Word>, TTRRecordType> pair : corpus) {
                this.init();
                List<TTRRecordType> mappedFeatures;
                Sentence<Word> goldSentence = pair.first();
                // AE: This is assuming that the goal record type in the corpus is headless.
                TTRRecordType rG = pair.second();
                this.init();
                logger.info(ANSI_CYAN + "Gold sentence: " + goldSentence + ANSI_RESET);
                Sentence<Word> generatedSentence = new Sentence<>();
                TTRRecordType rInc = rG; // Init to goal.
                TTRRecordType rCur;

                while (!rInc.isEmpty()) {
                    mappedFeatures = mapFeatures(rInc); //TODO FIX THIS AND NEXT LINE.
                    List<WordLogProb> candidates = populateBeam(mappedFeatures);

//                    List<WordLogProb> candidates = populateBeam(); // TODO
//                    boolean nothingGenerated = true;


//                    for (WordLogProb wordLogProb : candidates) {
//                        // try to generate the word and skip to the next word if it's not parsable/generatable.
//                        String w = wordLogProb.getWord();
//                        Double prob = wordLogProb.getProb();
//                        DAG d = this.generateWord(w, rG); // ---------------------------------------------
//                        if (d != null) {
//                            logger.info(ANSI_GREEN + "Generated word \"" + w + "\" with log-probability " + prob + ANSI_RESET);
//                            generatedSentence.add(new Word(w));
//                            nothingGenerated = false;
//                            logger.info("Current sentence: " + generatedSentence);
//                            TTRRecordType rGCopy = TTRRecordType.parse(rG.toString()).removeHeadIfManifest(); // to get over the bug for now, I want to make a copy of rG, and use it to compute rInc.
//                            rCur = (TTRRecordType) this.getParser().getContext().getDAG().getCurrentTuple().getSemantics().removeHeadIfManifest();  // How expensive is this operation?
//                            rInc = rGCopy.subtract(rCur, new HashMap<>());
//                            // dstype of pointed node, only that, nothing else
//                            logger.info("rInc now is: " + rInc);
//                            break;
//                        } else
//                            logger.error(ANSI_RED + "Could NOT generate word \"" + w + "\" with log-probability " + prob + ANSI_RESET);
//                    }


                    if(generateNextWord()){ //TODO this new design (using genereateNextWord) is not allowing me to get word and prob easily.
                        String w = this.parser.getContext().getDAG().getParentEdge().word().word();
                        logger.info(ANSI_GREEN + "Generated word \"" + w);// + "\" with log-probability " + prob + ANSI_RESET);
                        generatedSentence.add(new Word(w));
//                        nothingGenerated = false;
                        logger.info("Current sentence: " + generatedSentence);
                        TTRRecordType rGCopy = TTRRecordType.parse(rG.toString()).removeHeadIfManifest(); // to get over the bug for now, I want to make a copy of rG, and use it to compute rInc.
                        rCur = (TTRRecordType) this.getParser().getContext().getDAG().getCurrentTuple().getSemantics().removeHeadIfManifest();  // How expensive is this operation?
                        rInc = rGCopy.subtract(rCur, new HashMap<>());
                        // dstype of pointed node, only that, nothing else
                        logger.info("rInc now is: " + rInc);
                        break;
                    }// else
                        //logger.error(ANSI_RED + "Could NOT generate word"); // \"" + w + "\" with log-probability " + prob + ANSI_RESET);

                    else {//if (nothingGenerated) {
                        logger.error(ANSI_RED + "Could NOT continue generation!" + ANSI_RESET); // later add the word that was not generated to the error message.
                        // todo better log message.
                        addToOutputCorpus(goldSentence.toString(), rG.toString(), generatedSentence, partiallyGeneratedList, partiallyGeneratedStats);
                        break; // todo: or not to break, this is the question.
                    }
                    // todo there is a bug here probably that writes even partially generated sentences to fully generated file todo.

                }
                // Below is not clean. Have to clean up later.
                if (rInc.isEmpty()) {    // If we are here, then we have a fully generated sentence.
                    logger.info(ANSI_BLUE + "Fully generated sentence: " + generatedSentence + ANSI_RESET);
                    if (generatedSentence.equals(goldSentence))
                        absCorrect++;
                    // TODO if a sentence is fully generated, it's not necessarily correct. We should check that too in a way: putting it in the other output file or keep this in mind when processing this output file.
                    addToOutputCorpus(goldSentence.toString(), rG.toString(), generatedSentence, fullyGeneratedList, fullyGeneratedStats);
                }
            }
            // later add the support to also write the prob of generated sentence for all beams.
            writeToFile(grammarPath + "genOutputFull.txt", fullyGeneratedList, fullyGeneratedStats);
            writeToFile(grammarPath + "genOutputPartial.txt", partiallyGeneratedList, partiallyGeneratedStats);
            System.out.println("Absolutely correct: " + absCorrect);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return false;
    }


    // inspired from the one in GeneratorTester, but modified to write generated sentence instead of "File : 1".
    public static void writeToFile(String fileDir, List<String[]> ttrCorpus, CorpusStats corpusStats) {
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


    public static void main(String[] args) {
        // main method only for testing purposes.

        // These are all written by AE. Commented out by AA.
//        InteractiveProbabilisticGenerator ipg = new InteractiveProbabilisticGenerator("dsttr/resource/2017-english-ttr");
//        //InteractiveContextParser p = new InteractiveContextParser("resource/2017-english-ttr");
//        Utterance u = new Utterance("I see a square.");
//        if (!ipg.getParser().parseUtterance(u))
//            System.out.println("Parse not successful.");
//
//        TTRFormula goal = ipg.getParser().getFinalSemantics();
//		System.out.println("Goal: " + goal);
//        ipg.init();
////		TTRFormula  goal = TTRRecordType.parse("[x5 : e|e5==see : es|x1==I : e|pred1==square(x5) : cn|p8==shape(pred1) : t|head==e5 : es|p3==pres(e5) : t|p5==subj(e5, x1) : t|p4==obj(e5, x5) : t]");
//
//		ipg.generateWord("I", goal);
////        ipg.generateWord("see", goal);
//        ipg.generateWord("recognise", goal);
////        //this will fail. to test whether we are now able to generate the correct word (that all changes were undone properly)
//        ipg.generateWord("see", goal);
//        ipg.generateWord("a", goal);
//        //again, this should fail. But the next ('square') should succeed.
//        ipg.generateWord("circle", goal);
//        ipg.generateWord("square", goal);
////        //goal = [x5 : e|e5==see : es|x1==Arash : e|pred1==square(x5) : cn|p8==shape(pred1) : t|head==e5 : es|p3==pres(e5) : t|p5==subj(e5, x1) : t|p4==obj(e5, x5) : t]


        // Tests by AA:
        InteractiveProbabilisticGeneratorOld ipg = new InteractiveProbabilisticGeneratorOld(grammarPath);

        ipg.generate();
    }
}
