package qmul.ds.learn;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;
import org.apache.log4j.Logger;
import qmul.ds.InteractiveContextParser;
import qmul.ds.Utterance;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.tree.Tree;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class TTR2TreeCorpusConverter {
    private static final Logger logger = Logger.getLogger(TTR2TreeCorpusConverter.class);
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RED = "\u001B[31m";

//    static final String SEED_LEX_PATH = "resource\\2025-babyds-seeded-induction\\".replace("\\", File.separator);
//    static final String CONVERSION_GRAMMAR_PATH = "resource\\2025-babyai-toy-grammar\\".replace("\\", File.separator);
    public static String CORPUS_PATH = "resource\\2025-babyai-conversion-grammar\\".replace("\\", File.separator);
    public static String CORPUS_FILE_NAME = "test-corpus.txt";
    public static String CONVERSION_GRAMMAR_PATH = "resource\\2025-babyai-conversion-grammar\\".replace("\\", File.separator);
//    public static String SEED_GRAMMAR_PATH2 = "resource\\2023-english-ttr-induction-seed\\".replace("\\", File.separator);

    static final String TREE_MODEL_PATH = "resource\\2025-babyds-seeded-induction\\".replace("\\", File.separator);

    public String conversionGrammarPath;
    public String corpusPath;

    /**
     *
     * @param conversionGrammarPath conversion grammar path
     * @param corpusDirName corpus path + name
     */
    public TTR2TreeCorpusConverter(String conversionGrammarPath, String corpusDirName) {
        this.conversionGrammarPath = conversionGrammarPath;
        this.corpusPath = corpusDirName;
    }


    /**
     * converts a ttr rt corpus to tree corpus, by parsing in parallel.
     * @param corpusPath path to the corpus file
     * @return a corpus of trees, where each tree is the result of parsing a sentence from the corpus (and "completed").
     */
    public Corpus<Tree> convertCorpusParallel(String corpusPath) {
        RecordTypeCorpus recordTypeCorpus = new RecordTypeCorpus("rtCorpus", corpusPath);
        
        // Use parallel stream with thread-safe collection
        List<Pair<Sentence<Word>, Tree>> results = recordTypeCorpus.parallelStream()
            .map(pair -> {
                // Create a separate parser instance for each thread to avoid race conditions
                InteractiveContextParser parser = new InteractiveContextParser(this.conversionGrammarPath);
                Tree parsedTree = convertSample(pair, parser);
                return new Pair<>(pair.first(), parsedTree);
            })
            .collect(java.util.stream.Collectors.toList());
        
        // Convert results to Corpus
        Corpus<Tree> treeCorpus = new Corpus<>();
        results.forEach(treeCorpus::add);
        return treeCorpus;
    }


    /**
     * converts a ttr rt corpus to tree corpus, by parsing.
     * @return a corpus of trees, where each tree is the result of parsing a sentence from the corpus (and "completed").
     */
    public Corpus<Tree> convertCorpus(String corpusPath) {
        Corpus<Tree> treeCorpus = new Corpus<>();
        RecordTypeCorpus recordTypeCorpus = new RecordTypeCorpus("rtCorpus", corpusPath);
        InteractiveContextParser parser = new InteractiveContextParser(this.conversionGrammarPath);  //TODO Is this the correct constructor I'm calling?

        // Parse, to make the tree corpus:
        for (Pair<Sentence<Word>, TTRRecordType> pair: recordTypeCorpus) {
            Tree parsedTree = convertSample(pair, parser);
            treeCorpus.add(new Pair<>(pair.first(), parsedTree));
        }
        return treeCorpus;
    }


    public Tree convertSample(Pair<Sentence<Word>, TTRRecordType> pair, InteractiveContextParser parser) {
        Tree parsedTree = null;
        Evaluation eval = new Evaluation();
        parser.init();
        Sentence<Word> sentence = pair.first();
        TTRRecordType goldSem = pair.second();
        try {
            boolean parsed = parser.parseUtterance(new Utterance(sentence));
            if (parsed) {
                List<Pair<TTRRecordType, Tree>> allSemantics = new ArrayList<>();
                logger.debug("Gold semantics: " + goldSem);
                TTRRecordType parsedSem = (TTRRecordType) parser.getState().getCurrentTuple().getSemantics();
//                    Tree correspondingTree = parser.getState().getCurrentTuple().getTree();
                Tree correspondingTree = parser.complete();
                allSemantics.add(new Pair<>(parsedSem, correspondingTree));
                logger.debug("Looking for more possible semantics...");  // Because of the ambiguity caused by computational actions.
                int parseIdx = 0;
                while (true) {  // Steps through all different semantic interpretations.
                    try {
                        if (!parser.parse()) {  // Check if parsing should continue
                            break;
                        }
                        logger.trace(ANSI_YELLOW + parseIdx + "- other semantics: " + parser.getState().getCurrentTuple().getSemantics() + ANSI_RESET);
                        TTRRecordType newSem = (TTRRecordType) parser.getState().getCurrentTuple().getSemantics();
                        // get the corresponding target tree as well. That's what we are returning now...
//                            Tree newCorrespondingTree = parser.getState().getCurrentTuple().getTree();
                        Tree newCorrespondingTree = parser.complete();
                        parseIdx++;
                        if (!allSemantics.contains(newSem)) {
                            allSemantics.add(new Pair<>(newSem, newCorrespondingTree));
                        }
                    } catch (Exception e) {
                        logger.warn(ANSI_RED + "Sem is probs DisjunctiveType? (or could be other problems): " + parser.getState().getCurrentTuple().getSemantics() + ANSI_RESET);
                        break;
                    }
                }
                parsedTree = eval.findBestTreeInterpretation(allSemantics, goldSem);
            }
        } catch (Exception e) {
            logger.error(ANSI_RED + "Error while parsing: " + sentence + ANSI_RESET, e);
        }
            return parsedTree;
    }


    public void treeLeanerTester() {
        // load corpus with the above
//        Corpus<Tree> treeCorpus = convert();

//		TreeWordLearner treeLearner = new TreeWordLearner(SEED_LEX_PATH, treeCorpus);
//		treeLearner.learn();

//        treeLearner.getHypothesisBase().saveLearnedLexicon(TREE_MODEL_PATH, 1);

    }


    public static void main(String[] args) {
        TTR2TreeCorpusConverter testTreeLearner = new TTR2TreeCorpusConverter(CONVERSION_GRAMMAR_PATH, CORPUS_PATH+CORPUS_FILE_NAME);
        Corpus<Tree> treeCorpus = testTreeLearner.convertCorpus(CORPUS_PATH+CORPUS_FILE_NAME);
        for (Pair<Sentence<Word>, Tree> pair : treeCorpus) {
            logger.debug(ANSI_CYAN + "Sentence: " + pair.first() + ANSI_RESET + "\nTree: " + pair.second());
        }
        logger.info(ANSI_GREEN + "Parsed " + treeCorpus.size() + " sentences into trees." + ANSI_RESET);

        TreeWordLearner treeLearner = new TreeWordLearner(TREE_MODEL_PATH, treeCorpus);
        treeLearner.learn();
    }


}
