package qmul.ds.learn;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;
import qmul.ds.formula.ttr.TTRRecordType;

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

	Corpus<T> corpus;
	Iterator<Pair<Sentence<Word>, T>> corpusIterator;
	WordHypothesisBase hb = new WordHypothesisBase();
	public static String parserResourceDir = "resource" + File.separator + "2009-english-test-induction";
	public static String seedResourceDir = "resource" + File.separator + "2009-english-test-induction-seed";
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

	public WordLearner(String seedResourceDir) {
		hypothesiser = new Hypothesiser(seedResourceDir);
		corpus = null;
	}

	public WordLearner() {
		hypothesiser = new Hypothesiser(seedResourceDir);
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
		while (learnOnce())
		{
			i++;
			System.out.println("so far processed:"+i+" of "+corpus.size());
			System.out.println();
			
		}
			

		
	}

	public boolean corpusLoaded() {
		return corpus != null && !corpus.isEmpty();
	}

	public WordHypothesisBase getHypothesisBase() {
		return this.hb;
	}

	public void writeMissedCorpusToFile() throws IOException
	{
		this.writeCorpusToFile(this.skipped, "Skipped-Error-Corpus.txt");
	}

	public abstract void loadCorpus(File corpusFile) throws IOException, ClassNotFoundException;
	/**
	 * Usage: java WordLearner [resource Dir] [corpus File] [lexicon file] [number of hyps]
	 * @param a
	 */
	public static void main(String a[])
	{
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
