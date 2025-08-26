package qmul.ds.learn;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import me.tongfei.progressbar.ProgressBar;
import org.apache.log4j.Logger;

import qmul.ds.formula.TTRRecordType;
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

public abstract class WordLearner<T> {

	private static Logger logger = Logger.getLogger(WordLearner.class);

	Hypothesiser hypothesiser;

	Corpus<T> corpus;  //AE: becomes Corpus<TTRRecordType> in TTRWordLearner
	Iterator<Pair<Sentence<Word>, T>> corpusIterator;
	WordHypothesisBase hb = new WordHypothesisBase();
	public String parserResourceDir = "resource" + File.separator + "2009-english-test-induction";
	public String aaDir = "resource\\2025-babyds-RQ1\\".replace("\\", File.separator);
//	public static String seedResourceDir = "resource" + File.separator + "2009-english-test-induction-seed"; // commented out by Arash A.

    // IMPORTANT THIS USED TO WORK FOR RQ1 EXPERIMENTS. NOW CHANGING IT FOR RQ2.
//	public String seedResourceDir = "resource\\2023-english-ttr-induction-seed".replace("\\", File.separator);  // added by Arash A. // THE SEED DIR IS THIS!
	// TODO This shouldn't be hardcoded here
	// public String seedResourceDir = "resource\\2025-babyds-seeded-induction".replace("\\", File.separator);  // added by Arash A. // THE SEED DIR IS THIS!
   public String seedResourceDir = "resource\\2025-x".replace("\\", File.separator);  // added by Arash A. // THE SEED DIR IS THIS!



	Corpus<T> skipped=new Corpus<T>();
	
	
	public void writeCorpusToFile(Corpus<T> corpus, String file) throws IOException
	{
		BufferedWriter writer=new BufferedWriter(new FileWriter(file));
		
		for(Pair<Sentence<Word>, T> sent: corpus)
		{
			writer.write("Sent : "+sent.first().toString(true));
			writer.newLine();
			writer.write("Sem : " +sent.second().toString());
			writer.newLine();
			writer.write("File : Skipped");
			writer.newLine();
			writer.newLine();
		}
		writer.close();
	}


	public WordLearner(String seedResourceDir, int topN) {
		System.out.println("Using non-default seed directory: " + seedResourceDir);
		hypothesiser = new Hypothesiser(seedResourceDir, topN);
		corpus = null;
	}

	public WordLearner(String seedResourceDir) {
		System.out.println("Using non-default seed directory: " + seedResourceDir);
		hypothesiser = new Hypothesiser(seedResourceDir);
		corpus = null;
	}

	
	public WordLearner() {
        System.out.println("Using default seed directory in WordLearner: " + seedResourceDir);
		hypothesiser = new Hypothesiser(seedResourceDir);
		corpus = null;
	}

	/**
	 * Protected constructor that doesn't initialize hypothesiser.
	 * This prevents double loading of lexical actions when used with TTRHypothesiser.
	 * The subclass is responsible for proper hypothesiser initialization.
	 */
	protected WordLearner(boolean skipInitialization) {
		System.out.println("Using protected constructor - hypothesiser will be initialized by subclass");
		corpus = null;
	}

	
	
	public void loadAndParseCorpus(File sentences) throws IOException {
		
	}

	
	public abstract boolean learnOnce();
	
	public void reset() {
		corpus = null;
		hb.reset();
	}

	public void resetCorpus() {
		this.corpusIterator = corpus.iterator();
		hb.reset();
	}


	public void learn() {
		if (corpus == null || corpus.isEmpty()) {
			throw new IllegalStateException("Corpus not loaded or is empty");
		}
		int i=0;
		int corpusSize = corpus.size();

		try (ProgressBar pb = new ProgressBar("Learning progress", corpusSize)) {
			while (learnOnce()) {
				i++;
				pb.step();
				logger.info("So far processed: " + i + " of " + corpusSize + "\n");
			}
		}
	}


	public boolean corpusLoaded() {
		return corpus != null && !corpus.isEmpty();
	}


	public WordHypothesisBase getHypothesisBase() {
		return this.hb;
	}


	public void writeMissedCorpusToFile() throws IOException {
		this.writeCorpusToFile(this.skipped, "Skipped-Error-Corpus.txt");
	}


	public abstract void loadCorpus(File corpusFile) throws IOException, ClassNotFoundException;


	/**
	 * Usage: java WordLearner [resource Dir] [corpus File] [lexicon file] [number of hyps]
	 * @param a
	 */
	public static void main(String a[]) {
		if (a.length<4)
			System.out.println("Usage: java WordLearner [resource Dir] [corpus File] [lexicon file]");
		try{
			WordLearner<TTRRecordType> learner=new TTRWordLearner(a[0], a[1]);
			learner.learn();
			learner.writeMissedCorpusToFile();
			System.out.println("Done Learning!");
			System.out.println("Saving lexicon to:"+a[2]);
			int topN=5;
			boolean success=false;
			String corpusName=a[2];
			while(!success)
			{
				try{
					for(int i=1;i<=topN;i++)
						learner.getHypothesisBase().saveLearnedLexicon(corpusName, i);
					
					success=true;
				
				}catch(Exception e)
				{
					System.out.println("something wrong with file name.. ");
					
					System.out.println("file name:");
					e.printStackTrace();
					Console c=System.console();
					corpusName=c.readLine();
					
				}
			}
		}catch(Exception e)
		{
			logger.fatal(e);
		}
	}

}
