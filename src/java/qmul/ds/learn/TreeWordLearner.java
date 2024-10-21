package qmul.ds.learn;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.tree.Tree;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;

/**
 * Objects of this front-end class learn unknown words from a corpus of parsed sentences. The class provides methods for
 * parsing and loading corpora.
 * 
 * It makes use of a {@link Hypothesiser} to hypothesise whole action sequences that lead from the axiom tree to the
 * target tree. These are then split via ( {@code CandidateSequence.split()} into their comprising parts. These
 * different split possibilities are then handed over to the {@link WordHypothesisBase} for generalisation and
 * probability estimation/update. This happens incrementally, i.e. one training example at a time as they are
 * encountered in the corpus.
 * 
 * @author arash
 * 
 */

public class TreeWordLearner extends WordLearner<Tree> {

	private static Logger logger = Logger.getLogger(TreeWordLearner.class);

	
	public final static String parserResourceDir = "resource" + File.separator + "2009-english-test-induction";
	public final static String seedResourceDir = "resource" + File.separator + "2009-english-test-induction-seed";

	public TreeWordLearner(String seedResourceDir, Corpus<Tree> c) {
		hypothesiser = new Hypothesiser(seedResourceDir);
		corpus = c;
		this.corpusIterator = corpus.iterator();
	}

	public TreeWordLearner(String seedResourceDir) {
		hypothesiser = new Hypothesiser(seedResourceDir);
		corpus = null;
	}

	public TreeWordLearner() {
		hypothesiser = new Hypothesiser(seedResourceDir);
		corpus = null;
	}

	

	public void loadAndParseCorpus(File sentences) throws IOException {
		Corpus<Tree> c = new Corpus<Tree>();
		c.loadAndParseCorpusFromFile(sentences, parserResourceDir);
		this.corpus = c;
		this.corpusIterator = c.iterator();
		hb.reset();
	}

	
	public boolean learnOnce() {
		if (corpusIterator == null || !corpusIterator.hasNext()) {
			logger.warn("Either no corpus, or no more examples");
			return false;
		}

		Pair<Sentence<Word>, Tree> entry = corpusIterator.next();
		Tree target = entry.second();
		System.out.println("processing:" + entry.first()+" with "+entry.first().size()+" words ");
		hypothesiser.loadTrainingExample(entry.first(), target);
		// logger.info("Hypothesising from training example: "+
		// sentence+"->"+target);
		System.out.print("Now adding rows to base");
		Collection<CandidateSequence> hyps = null;
		try {
			hyps = hypothesiser.hypothesise();
		} catch (Exception e) {
			logger.error("problem hypothesising. Sentence:" + entry.first());
			e.printStackTrace();
			logger.error("Skipping...");
			skipped++;
			return true;
		}
		// DAGHypothesiser.printHypMap(hyps);
		hb.forgetCurrentDist();
		int totalSplit = 0;
		try {
			for (CandidateSequence cs : hyps) {
				Set<List<CandidateSequence>> splitSequences = cs.split();
				totalSplit += splitSequences.size();
				hb.addSequenceTuples(splitSequences);
			}
			// System.out.println("Added a total of "+totalSplit+" split possibilities");
			this.hb.updateDistsEndOfExample(entry.first());
		} catch (Exception e) {
			logger.fatal("problem while updating distributions on sentence:" + entry.first());
			e.printStackTrace();
			System.exit(1);

		}
		// System.out.println("All Done. Prior after "+sentence);
		// System.out.println(hb.getPrior());
		return true;
	}

	int skipped = 0;


	public void learn() {
		if (corpus == null || corpus.isEmpty()) {
			throw new IllegalStateException("Corpus not loaded or is empty");
		}
		while (learnOnce()) ;
		logger.warn("Skipped: " + skipped);
	}


		logger.warn("Skipped:" + skipped);
	}

	public boolean corpusLoaded() {
		return corpus != null && !corpus.isEmpty();
	}

	public WordHypothesisBase getHypothesisBase() {
		return this.hb;
	}

	public static void main(String[] args) {
		Corpus<Tree> c = new Corpus<Tree>();
		try {
			c.loadAndParseCorpusFromFile("corpus" + File.separator + "RandomDSCorpus" + File.separator
					+ "sentences.txt", "resource" + File.separator + "2009-english-test-induction");
		} catch (IOException e) {
			e.printStackTrace();
		}

		TreeWordLearner learner = new TreeWordLearner("resource" + File.separator + "2009-english-test-induction-seed", c);
		learner.learn();

		// learner.printHypotheses();

	}

	public void loadCorpus(File corpusFile) throws IOException, ClassNotFoundException {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(corpusFile));
		this.corpus = (Corpus<Tree>) in.readObject();
		in.close();
		this.corpusIterator = this.corpus.iterator();
	}

}
