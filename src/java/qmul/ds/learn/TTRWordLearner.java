package qmul.ds.learn;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.apache.log4j.Logger;

import qmul.ds.formula.TTRRecordType;
import qmul.ds.action.Lexicon;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;

/**
 * Objects of this front-end class learn unknown words from a corpus of parsed sentences. The class provides methods for
 * parsing and loading corpora.
 * It makes use of a {@link Hypothesiser} to hypothesise whole action sequences that lead from the axiom tree to the
 * target tree. These are then split via {@code CandidateSequence.split()} into their comprising parts. These
 * different split possibilities are then handed over to the {@link WordHypothesisBase} for generalisation and
 * probability estimation/update. This happens incrementally, i.e. one training example at a time as they are
 * encountered in the corpus.
 * 
 * @author arash
 * 
 */

public class TTRWordLearner extends WordLearner<TTRRecordType>{
	
	public static final Logger logger=Logger.getLogger(TTRWordLearner.class);
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_RED = "\u001B[31m";


	public TTRWordLearner(String seedResourceDir, RecordTypeCorpus c) {
		hypothesiser = new TTRHypothesiser(seedResourceDir);
		corpus = c;
		this.corpusIterator = corpus.iterator();
	}


	/**
	 * TODO this can be improved in terms of the use of topN vs loadLearntLexicon (e.g. if topN is 1,2,3, then loadLearntLexicon should be true, or if it's 0, then loadLearntLexicon should be false)
	 * TODO it would be cleaner if I change the order of the arguments here so it matches the one in TTRHypothesiser.
	 * @param seedGrammarPath the seed grammar file address (used by Lexicon, so can be lexicon.lex or lexicon.txt files)
     * @param trainingCorpus the corpus to learn from
	 * @param learnerCompActionsPath the bigger computational actions file address
	 * @param whb the word hypothesis base to use
	 * @param topN the number of most probable lexical actions to be read from the learnt lexicon files.
	 * @param loadLearntLexicon whether to force load the learnt lexicon from the file or normally use the rule-based lexicon.
	 */
	public TTRWordLearner(String seedGrammarPath, RecordTypeCorpus trainingCorpus, String learnerCompActionsPath, WordHypothesisBase whb, int topN, boolean loadLearntLexicon) {
		super(true); // Use protected constructor to avoid double initialization
		hypothesiser = new TTRHypothesiser(learnerCompActionsPath, seedGrammarPath, topN, loadLearntLexicon);

		this.corpus = trainingCorpus;
		this.corpusIterator = corpus.iterator();
		
		if (whb != null) {
			this.hb = whb;
		} else {
			this.hb = new WordHypothesisBase();
		}
	}


	/**
     * @author AA
	 * The nicest constructor for the learner (since it doesn't assume everything is in the same directory)
	 * Supports separate directories for everything, as specified below.
     * @param seedGrammarPath the seed grammar file address (used by Lexicon, so can be lexicon.lex or lexicon.txt files)
     * @param trainingCorpus the corpus to learn from
	 * @param learnerCompActionsPath the bigger computational actions file address
	 * @param whb the word hypothesis base to use
	 * @param topN the number of most probable lexical actions to be read from the learnt lexicon files.
	 * TODO re-arrange parameters so it becomes even nicer.
     */
	public TTRWordLearner(String seedGrammarPath, RecordTypeCorpus trainingCorpus, String learnerCompActionsPath, WordHypothesisBase whb, int topN) {
		super(true); // Use protected constructor to avoid double initialization
		hypothesiser = new TTRHypothesiser(learnerCompActionsPath, seedGrammarPath, topN, true);

		this.corpus = trainingCorpus;
		this.corpusIterator = corpus.iterator();
		
		if (whb != null) {
			this.hb = whb;
		} else {
			this.hb = new WordHypothesisBase();
		}
	}

	    /**
     * The one used by AE.
	 * AA Comment: not a good constructor, because it uses the default seed directory in WordLearner (widely used in code, so be careful).
     * @param resourceDir
     * @param corpusFileName
     */
	public TTRWordLearner(String resourceDir, String corpusFileName, int topN) {
		super(resourceDir);  // Call the parent constructor with the correct parameter
		hypothesiser = new TTRHypothesiser(resourceDir, topN);
		try {
			this.loadCorpus(new File(corpusFileName));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

    /**
     * The one used by AE. (later modified by me - added topN parameter)
	 * AA Comment: not a good constructor, because it uses the default seed directory in WordLearner (widely used in code, so be careful).
     * @param resourceDir
     * @param corpusFileName
     */
	public TTRWordLearner(String resourceDir, String corpusFileName) {
		this(resourceDir, corpusFileName, 3);
	}

    /**
     * The one used by trainTestWithHB
     * @param seedResourceDir
     * @param trainingCorpus
     * @param whb
     */
	public TTRWordLearner(String seedResourceDir, RecordTypeCorpus trainingCorpus, WordHypothesisBase whb) {
		super(seedResourceDir);  // Call the parent constructor with the correct parameter
		hypothesiser = new TTRHypothesiser(seedResourceDir);
		this.corpus = trainingCorpus;
		this.corpusIterator = corpus.iterator();
		if (whb != null) {
			this.hb = whb;
		} else {
			this.hb = new WordHypothesisBase();
		}
	}


	public TTRWordLearner(String seedResourceDir) {
		super(seedResourceDir);  // Call the parent constructor with the correct parameter
		hypothesiser = new TTRHypothesiser(seedResourceDir);
		corpus = null;
	}

	/**
	 * Generated by Copilot:
	 * Default constructor for the TTRWordLearner class.
	 * Initializes the seedResourceDir with a predefined path to the seed resource directory.
	 * Creates a new TTRHypothesiser with the seed resource directory.
	 * `seed` here refers to a beginning point for the lexicon and grammar (to not start from zero!): AA
	 */
	public TTRWordLearner() {
		seedResourceDir="resource" + File.separator + "2013-english-ttr-induction-seed";
		hypothesiser = new TTRHypothesiser(seedResourceDir);
		corpus = null;
	}


	@Override
	public boolean learnOnce() {
		if (corpusIterator == null) {
			logger.info("No corpus loaded.");
			return false;
		}
		if (!corpusIterator.hasNext()) {
			logger.info("No more examples in the corpus.");
			return false;
		}

		Pair<Sentence<Word>, TTRRecordType> entry = corpusIterator.next();
		logger.info("Hypothesising sequences for utterance: " + entry.first());
		// logger.info("Hypothesising from training example: "+
		// sentence+"->"+target);
		long time = System.currentTimeMillis();
		Collection<CandidateSequence> hyps = null;
		try {
			((TTRHypothesiser)hypothesiser).loadTrainingExample(entry.first(), entry.second());
			hyps = hypothesiser.hypothesise();
			logger.info("\n");
			if (hyps.isEmpty()) {
				logger.warn(ANSI_YELLOW + "NO SEQUENCES RECEIVED from hypothesiser! skipping... " + ANSI_RESET);
//				System.out.println("no sequences returned, skipping this");
				skipped.add(entry);
				return true;
			}
		} catch (Exception e) {
			logger.error("problem hypothesising. Sentence:" + entry);
			e.printStackTrace();
			logger.error("Skipping...");
			skipped.add(entry);
			return true;
		}
		logger.info(ANSI_GREEN +  "Got " + hyps.size() + " sequences from Hypothesiser for "+ entry.second() + ANSI_RESET);
		logger.info(ANSI_GREEN);
		for(CandidateSequence cs: hyps)
		{
			logger.info(cs.toShortString());

		}
		logger.info(ANSI_RESET);

		logger.info(ANSI_GREEN + "Now splitting the sequences..." + ANSI_RESET);
		// DAGHypothesiser.printHypMap(hyps);
		Collection<Word> unknownWords = getUnknownWords(entry.first());

		hb.forgetCurrentDist();
		int totalSplit = 0;  // AA: Better be called `totalSplits`!
		int i = 0;
		try {

			for (CandidateSequence cs: hyps) {  // TODO Potential parallelisable loop
				i++;
				logger.debug("Splitting: " + cs.toShortString());
				Set<List<CandidateSequence>> splitSequences = cs.split();
				for (List<CandidateSequence> seq: splitSequences) {
					logger.trace("Result: " + seq + "\n");
				}

				totalSplit += splitSequences.size();
				logger.debug(i + ":" + splitSequences.size()+ " ");
				logger.debug("Adding split sequences to hypothesis base...");

				hb.addSequenceTuples(splitSequences);
			}

			logger.info("\n");
			this.hb.updateDistsEndOfExample(unknownWords);
			logger.info("Processing took: "+ (System.currentTimeMillis()-time)/1000 + " seconds");  // AA Not working correctly!
		} catch (Exception e) {
			logger.fatal("problem while updating distributions on sentence:" + entry);
			logger.fatal("this is fatal :(");
			e.printStackTrace();
			System.exit(1);
		}

		return true;
	}

	private Collection<Word> getUnknownWords(Sentence<Word> sent) {
		HashSet<Word> result = new HashSet<Word>();
		for (Word w: sent)
		{
			if (!this.hypothesiser.seedLexicon.containsKey(w.word()))
				result.add(w);
		}
		return result;
	}


	@Override
	public void loadCorpus(File corpusFile) throws IOException, ClassNotFoundException {
		RecordTypeCorpus c=new RecordTypeCorpus();
		c.loadCorpus(corpusFile);
		this.corpus=c;
		this.corpusIterator=this.corpus.iterator();
	}


	public void setTrainingCorpus(RecordTypeCorpus corpus) {
//		RecordTypeCorpus c=new RecordTypeCorpus();
//		c.loadCorpus(corpusFile);
		this.corpus=corpus;
		this.corpusIterator=this.corpus.iterator();
	}


	public Lexicon getSeedLexicon() {
		return this.hypothesiser.getSeedLexicon();
	}

	/**
	 * A wrapper for the below method, with default value for saveTopNStart.
	 * @param savePath
	 * @param topN
	 */
	public void saveModel(String savePath, int topN) {
		this.saveModel(savePath, topN, 1);
	}


	/**
	 * Saves the model to a file. Another nice method by AA ;)
	 * @param savePath
	 * @param topN
	 */
	public void saveModel(String savePath, int topN, int saveTopNStart) {
		try {
			for (int i = saveTopNStart; i <= topN; i++) {
				this.getHypothesisBase().saveLearnedLexicon(savePath, i, this.getSeedLexicon());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public static void main(String[] args) {
		// String babyDSPath = "resource\\2025-babyds-seeded-induction\\".replace("\\", File.separator);  // This works (by AE).
		String babyDSPath = "resource\\2025-babyds-RQ1\\class1HB\\S46_B3\\".replace("\\", File.separator);  // Test

        // String learnerCompActionsPath = "resource\\2025-x\\".replace("\\", File.separator);
		String learnerCompActionsPath = "resource\\2025-babyds-seeded-induction2\\".replace("\\", File.separator);

		String corpusPath = babyDSPath + "class1-debug.txt";  // This works (by AE).
//        String corpusPath = babyDSPath + "class1.txt";
		String savePath = "resource\\2025-babyds-seeded-induction-output\\".replace("\\", File.separator);
       
		RecordTypeCorpus cp = new RecordTypeCorpus();
		try {
			cp.loadCorpus(new File(corpusPath));
		} catch (IOException e) {
			e.printStackTrace();
		}


		// TTRWordLearner learner = new TTRWordLearner(babyDSPath, cp, learnerCompActionsPath, null, 1);
		TTRWordLearner learner = new TTRWordLearner(babyDSPath, cp, learnerCompActionsPath, null, 3, true);

    //    TTRWordLearner learner = new TTRWordLearner(learnerCompActionsPath, cp, null);
		
	// TTRWordLearner learner = new TTRWordLearner(learnerCompActionsPath, corpusPath, 3);
		learner.learn();
		learner.saveModel(savePath, 3);

		//System.out.println(learner.hypothesiser.targetIndependentHyps);
	}
}
