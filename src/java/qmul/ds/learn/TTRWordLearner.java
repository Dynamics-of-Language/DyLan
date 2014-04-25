package qmul.ds.learn;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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

public class TTRWordLearner extends WordLearner<TTRRecordType>{
	
	public static final Logger logger=Logger.getLogger(TTRWordLearner.class);

	public TTRWordLearner(String seedResourceDir, RecordTypeCorpus c) {
		hypothesiser = new TTRHypothesiser(seedResourceDir);
		corpus = c;
		this.corpusIterator = corpus.iterator();
	}
	
	public TTRWordLearner(String resourceDir, String corpusFileName) throws IOException, ClassNotFoundException
	{
		
		this(resourceDir);		
		this.loadCorpus(new File(corpusFileName));
	}

	public TTRWordLearner(String seedResourceDir) {
		hypothesiser = new TTRHypothesiser(seedResourceDir);
	}

	public TTRWordLearner() {
		seedResourceDir="resource" + File.separator + "2013-english-ttr-induction-seed";
		hypothesiser = new TTRHypothesiser(seedResourceDir);
		
		corpus = null;
	}

	
	@Override
	public boolean learnOnce() {
		if (corpusIterator == null || !corpusIterator.hasNext()) {
			logger.warn("Either no corpus, or no more examples");
			return false;

		}
		Pair<Sentence<Word>, TTRRecordType> entry = corpusIterator.next();

		
		System.out.println("Hypthesising sequences for: " + entry.first());
		

		// logger.info("Hypothesising from training example: "+
		// sentence+"->"+target);
		long time=System.currentTimeMillis();
		Collection<CandidateSequence> hyps = null;
		try {
			((TTRHypothesiser)hypothesiser).loadTrainingExample(entry.first(), entry.second());
			hyps = hypothesiser.hypothesise();
			System.out.println();
			if (hyps.size()==0)
			{
				logger.warn("NO SEQUENCES RECEIVED from hypothesiser.. skipping... ");
				System.out.println("no sequences returned, skipping this");
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
		System.out.println("got "+hyps.size()+" sequences from Hypothesiser");
		System.out.println("now splitting the sequences");
		// DAGHypothesiser.printHypMap(hyps);
		hb.forgetCurrentDist();
		int totalSplit = 0;
		int i=0;
		try {
			for (CandidateSequence cs : hyps) {
				i++;
				Set<List<CandidateSequence>> splitSequences = cs.split();
				totalSplit += splitSequences.size();
				System.out.print(i+":"+splitSequences.size()+" ");
				if (i%15==0)
					System.out.println();
				hb.addSequenceTuples(splitSequences);
				
			}	
			System.out.println();
			this.hb.updateDistsEndOfExample(entry.first());
			System.out.println("Processing took:"+ (System.currentTimeMillis()-time)/1000 + " seconds");
		} catch (Exception e) {
			logger.fatal("problem while updating distributions on sentence:" + entry);
			logger.fatal("this is fatal :(");
			e.printStackTrace();
			
			System.exit(1);

		}
		// System.out.println("All Done. Prior after "+sentence);
		// System.out.println(hb.getPrior());
		return true;

	}

	@Override
	public void loadCorpus(File corpusFile) throws IOException, ClassNotFoundException {
		RecordTypeCorpus c=new RecordTypeCorpus();
		c.loadCorpus(corpusFile);
		this.corpus=c;
		this.corpusIterator=this.corpus.iterator();
	}
	
	
	public static void main(String args[])
	{
		TTRWordLearner learner=new TTRWordLearner();
		try{
			learner.loadCorpus(new File(args[2]));
			learner.learn();
			learner.getHypothesisBase().saveLearnedLexicon("resource/2013-ttr-learner-output/lexicon.lex", 2);
			learner.getHypothesisBase().saveLearnedLexicon("resource/2013-ttr-learner-output/lexicon.lex", 3);
			learner.getHypothesisBase().saveLearnedLexicon("resource/2013-ttr-learner-output/lexicon.lex", 4);
			learner.getHypothesisBase().saveLearnedLexicon("resource/2013-ttr-learner-output/lexicon.lex", 5);
			
		}catch( Exception e)
		{
			e.printStackTrace();
		}
		
		
	}


}
