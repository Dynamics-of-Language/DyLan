package qmul.ds.learn;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.action.LexicalAction;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.WordHypothesis;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;

@SuppressWarnings("serial")
public class WordHypothesisBase {
	private static final Logger logger = Logger.getLogger(WordHypothesisBase.class);
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_RED = "\u001B[31m";
	//public static final String lexiconSaveFolder = "2013-learner-output" + File.separator;
	// all split sequences
	private List<List<WordHypothesis>> tuples;
	// the probability distribution obtained from the current training example
	private Map<HasWord, WordLogProbDistribution> curDist;
	// the probability distribution from previous training examples.
	private Map<HasWord, WordLogProbDistribution> priorDist;
	// this one stores the indeces (rows) in {@link tuples} at which a Word Hypothesis appears.
	// this is only to make the process of Maximum Likelyhood Estimation faster.
	private Map<WordHypothesis, Set<Integer>> indeces;
	private final static int EM_ROUNDS = 1;
	private int numTrainingSoFar = 0;

	public WordHypothesisBase() {
		super();
		tuples = new ArrayList<List<WordHypothesis>>();
		curDist = new HashMap<HasWord, WordLogProbDistribution>();
		priorDist = new HashMap<HasWord, WordLogProbDistribution>();
		// priorDistWeights=new HashMap<HasWord, Integer>();
		indeces = new HashMap<WordHypothesis, Set<Integer>>();
	}

	/**
	 * adds result of a split candidate sequence, i.e. a set of possible splits, to this hypothesis base. It does this
	 * by testing for each single word candidate sequence in its argument, whether it is intersectable with any of the
	 * existing {@link WordHypothesis}'s. If so, it will have modified that SI which it is intersected into. Otherwise a
	 * new one will be created. The method always results in adding a {@link List<WordHypothesis>} to the list of
	 * hypothesis tuples ( {@code this.tuples}) maintained by this hypothesis base.
	 * 
	 * @param set
	 */
	public void addSequenceTuples(Set<List<CandidateSequence>> set) {
		logger.info("Adding " + set.size() + " rows to base");
		
		for (List<CandidateSequence> split : set) {
			List<WordHypothesis> newTuple = new ArrayList<WordHypothesis>();
			for (CandidateSequence cs : split) {
				if (cs.getWords().size() != 1)
					throw new IllegalArgumentException(
							"trying to add candidate sequence not corresponding to a single word, you should split the CS first.");
				HasWord w = cs.getWords().get(0);
				if (!curDist.containsKey(w))
					curDist.put(w, new WordLogProbDistribution(w, 1.0));
				if (!priorDist.containsKey(w))
					priorDist.put(w, new WordLogProbDistribution(w));

				Set<WordHypothesis> existingHyps = priorDist.get(w).getAllHyps();
				WordHypothesis intersected = null;

				for (WordHypothesis si : existingHyps) {
					if (si.intersectInto(cs)) {
						// found compatible existing si
						intersected = si;
						break;
					}
				}
				if (intersected != null) { // managed to find compatible one.
											// Just duplicate that reference in
											// tuples. but add unique address to
											// wordmap
					newTuple.add(intersected);
					logger.debug("intersected into existing hyp:" + intersected);
					logger.debug(w + " now has " + curDist.get(w).size() + " hyps");

					// wordMap.get(w).get(intersected).add(tuples.size());
					if (indeces.containsKey(intersected))
						indeces.get(intersected).add(tuples.size());
					else {
						Set<Integer> indexSet = new HashSet<Integer>();
						indexSet.add(tuples.size());
						indeces.put(intersected, indexSet);
					}
					curDist.get(w).addHyp(intersected);
				} else {
					// didn't manage to find any compatible sequences
					// create new SequenceIntersection, and add it to tuples and
					// curDist
					WordHypothesis newIntersection = new WordHypothesis(priorDist.get(w).getFreshHypID());
					newIntersection.intersectInto(cs);
					logger.debug("Could not intersect into existing. Created new hyp:" + newIntersection);
					newTuple.add(newIntersection);
					// add new hyp to both distributions with prob 0 (positive
					// logprob) at this
					// initial stage
					curDist.get(w).addHyp(newIntersection);
					priorDist.get(w).addHyp(newIntersection);
					Set<Integer> indexSet = new HashSet<Integer>();
					indexSet.add(tuples.size());
					indeces.put(newIntersection, indexSet);

					logger.debug(w + " curDist now has: " + curDist.get(w).keySet());
					logger.debug(w + " priorDist now has: " + priorDist.get(w).keySet());
				}
			}
			tuples.add(newTuple);
		}
	}

	/**
	 * returns a sorted list - from most probable to least - view of the current hypotheses for word w in this base.
	 * 
	 * @param w
	 * @return a sorted list of word hypotheses
	 */
	public List<WordHypothesis> getWordHyps(HasWord w) {
		return priorDist.get(w).getSortAllHyps();
	}

	/**
	 * calculates the log of the normalising factor Z, i.e. log(Z), for the probability distribution over hypotheses for
	 * w.
	 * @param w
	 * @return log(Z)
	 */
	public double logZ(HasWord w) {
		Set<Integer> wordIndeces = new HashSet<Integer>();

		for (WordHypothesis wh : curDist.get(w).getAllHyps()) {
			wordIndeces.addAll(indeces.get(wh));
		}

		List<Double> logProducts = new ArrayList<Double>();
		for (Integer i : wordIndeces) {
			// adjusting logZ for duplicate words
			// e.g. if w occurs twice in this row, the whole row should count twice in logZ
			for (int j = 0; j < countDifferentHypsForWordAt(w, i); j++)
				logProducts.add(logProbProduct(tuples.get(i)));
		}
		return sumLogProb(logProducts);
	}

	/**
	 * 
	 * @param w
	 * @param index
	 * @return
	 */
	private int countDifferentHypsForWordAt(HasWord w, int index) {
		int i = 0;
		List<WordHypothesis> tuple = tuples.get(index);
		Set<WordHypothesis> sofar = new HashSet<WordHypothesis>();
		for (WordHypothesis h : tuple) {
			if (h.getWord().equals(w)) {
				sofar.add(h);
			}
		}
		return sofar.size();
	}

	/**
	 * Calculates the (log) sum of the probabilities of the sequences that pass through word hypothesis wh. (logZ is
	 * then subtracted from this to obtain a maximum likelyhood estimate for wh.
	 * 
	 * @param wh
	 * @return
	 */
	private double logProbNumerator(WordHypothesis wh) {
		List<Double> logProducts = new ArrayList<Double>();
		for (Integer i : indeces.get(wh)) {
			logProducts.add(logProbProduct(tuples.get(i)));
		}
		return sumLogProb(logProducts);
	}

	private double getLogProb(WordHypothesis wh, double logZ) {
		return logProbNumerator(wh) - logZ;
	}

	private double logProbProduct(List<WordHypothesis> tuple) {
		double logSum = 0;
		for (WordHypothesis wh : tuple) {
			double logProb = curDist.get(wh.getWord()).get(wh);
			if (logProb > 0)
				throw new IllegalStateException("Hypothesis " + wh
						+ " should have negative log prob. Assign initial (uniform) probabilities first");
			logSum += logProb;
		}
		return logSum;
	}

	/**
	 * will aggregate current and prior distributions, setting prior to be this aggregate and updating the current dist
	 * to reflect the new probabilities associated with the aggregate, but without changing the set of hypotheses in the
	 * current distribution. I.e. after this operation, the sum of the current dist probs for words in the current
	 * training example will be less than 1.
	 */
	private void aggregateDistributions() {
		for (HasWord w : curDist.keySet()) {
			WordLogProbDistribution aggregate;
			if (!priorDist.containsKey(w)) {
				// new word. assume an empty distribution with 0 weight
				logger.debug("aggregating with empty distribution");
				aggregate = curDist.get(w).weightedAggregate(new WordLogProbDistribution(w));
			} else
				aggregate = curDist.get(w).weightedAggregate(priorDist.get(w));
			priorDist.put(w, aggregate);
			refreshCurDistFromPrior(w);
		}
	}

	/**
	 * transfers probabilities for word hypotheses for w from the prior distribution into the current distribution for a
	 * subsequent round of EM.
	 * 
	 * @param w
	 */
	private void refreshCurDistFromPrior(HasWord w) {

		if (!priorDist.containsKey(w))
			throw new IllegalStateException();
		WordLogProbDistribution wCur = curDist.get(w);
		WordLogProbDistribution wPrior = priorDist.get(w);

		for (WordHypothesis wh : wCur.keySet()) {
			if (!wPrior.containsKey(wh))
				throw new IllegalStateException("refreshing cur from prior, but prior misses a hypothesis " + wh);
			wCur.put(wh, wPrior.get(wh));
		}
	}

	public Set<LexicalAction> getActions(HasWord w) {
		return null;
	}

	public Lexicon getLearnedLexicon(int topN) {
		logger.info("Getting learned lexicon with topN=" + topN);
		Lexicon l = new Lexicon();
		for (HasWord w : priorDist.keySet()) {
			l.put(w.word(), new ArrayList<LexicalAction>());
			int n = 1;  // A counter to limit the number of WordHypothesis instances that are processed for each word. By Copilot/Arash A.
			int rank = -1;
			double lastProb = -1;
			for (WordHypothesis h : getWordHyps(w)) {
				if (n > topN)
					break;
				// Set the rank based on the probability of the WordHypothesis (although, rank is faulty, remains 0)
				if (h.getProb() > lastProb)
					rank++;

				LexicalAction act = h.getCoreAction();
				act.setProb(h.getProb());
				act.setRank(rank);
				logger.info(ANSI_GREEN+ "Adding action \"" + act + "\" to lexicon for word: " + w + " with prob:" + h.getProb() + " and rank: " + rank + ANSI_RESET);
				logger.debug("[ACTION INFO]  word: " + act.getWord() + " | lexical action type: " + act.getLexicalActionType() + " | semantics: " + act.getSemantics());
				logger.debug("Action effect is: \n" + act.getEffects()[0]);
				logger.debug("Hyp was:" + h);
				l.get(w.word()).add(act);
				n++;
				lastProb = h.getProb();
			}
		}
		return l;
	}

	public void saveLearnedLexicon(String f, int topN) throws IOException {
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f+"-top-"+topN));
		
		Lexicon lex = this.getLearnedLexicon(topN);
		logger.info(ANSI_BLUE + "Lexicon size: " + lex.size()+  ANSI_RESET);
		logger.info(ANSI_BLUE + "Lexicon is: " + lex + ANSI_RESET);
		out.writeObject(lex);
//		out.writeObject();
		out.close();
		lex.writeToTextFile(f + "-top-"+topN +".txt");
		logger.info("Saved top-" + topN + " lexicon to " + f);
	}

	private void loadPriorIntoCur(Collection<Word> sw) {
		// remove duplicate
		HashSet<Word> words = new HashSet<Word>(sw);
		for (HasWord w : words) {
			if (curDist.containsKey(w)) {
				for (WordHypothesis wh : curDist.get(w).keySet()) {
					if (priorDist.get(w).containsKey(wh)) {
						curDist.get(w).put(wh, priorDist.get(w).get(wh));
					}
				}
			} else
				throw new IllegalArgumentException("Trying to set current uniform probs for " + w
						+ " but this does not exist in the current Distribution");
		}
	}

	/**
	 * utility method for the calculation of the (log) sum of a set of probabilities.
	 * 
	 * @param logProbs
	 * @return
	 * @throws Exception
	 */
	private static double sumLogProb(List<Double> logProbs) {
		if (logProbs.size() == 1)
			return logProbs.get(0);
		double restLogProb = sumLogProb(logProbs.subList(1, logProbs.size()));
		double exp = Math.exp(restLogProb - logProbs.get(0));
		// if (restLogProb-logProbs.get(0)>0)
		// System.out.println("Positive Exp: exp("+(restLogProb-logProbs.get(0))+")");
		if (Double.isInfinite(exp)) {

			logger.error("Infinite exp(" + (restLogProb - logProbs.get(0)) + ")");
			logger.error("restLogProb was:" + restLogProb);
			logger.error("first Log prob was:" + logProbs.get(0));
			System.exit(1);
		}
		if (Double.isNaN(exp)) {
			logger.error("Infinite exp(" + (restLogProb - logProbs.get(0)) + ")");
			System.exit(1);
		}
		double logSum = logProbs.get(0) + Math.log(1 + exp);
		if (Double.isNaN(logSum)) {
			logger.error("NaN logSum for:" + logProbs);
			logger.error("Rest log prob:" + restLogProb);

		} else if (Double.isInfinite(logSum)) {
			logger.error("Infinite logSum for: " + logProbs);
			logger.error("Rest log prob:" + restLogProb);

		}

		return logSum;
	}

	public boolean containsWord(List<WordHypothesis> wh, HasWord w) {
		for (WordHypothesis h : wh)
			if (h.getWord().equals(w))
				return true;
		return false;
	}

	private void performLocalEM(int n) {
		for (int i = 0; i < n; i++) {
			Map<HasWord, Map<WordHypothesis, Double>> newDist = new HashMap<HasWord, Map<WordHypothesis, Double>>();
			for (HasWord w : curDist.keySet()) {
				double logZ = logZ(w);
				Map<WordHypothesis, Double> thisWordDist = new HashMap<WordHypothesis, Double>();
				for (WordHypothesis wh : curDist.get(w).keySet()) {
					thisWordDist.put(wh, getLogProb(wh, logZ));
				}
				newDist.put(w, thisWordDist);
			}
			for (HasWord w : newDist.keySet()) {
				curDist.get(w).clear();
				curDist.get(w).putAll(newDist.get(w));
			}
		}
	}

	public String toString() {
		String result = "Word Hypothesis table:\n";
		for (List<WordHypothesis> seqList : tuples) {
			for (WordHypothesis hyp : seqList) {
				result += hyp + ")|";
			}
			result += "\n";
		}
		return result;
	}

	public List<List<WordHypothesis>> getHypothesisTuples() {
		return this.tuples;
	}

	public Set<HasWord> getWords() {
		return priorDist.keySet();
	}

	public void forgetCurrentDist() {
		logger.info("forgetting cur dist");
		this.tuples.clear();
		this.curDist.clear();
		this.indeces.clear();
		logger.info("tuples now has " + tuples.size() + " rows");
	}

	public Map<HasWord, WordLogProbDistribution> getPrior() {
		return this.priorDist;
	}

	public Set<Integer> getHypIndeces(WordHypothesis wh) {
		return indeces.get(wh);
	}

	public void reset() {
		this.forgetCurrentDist();
		this.priorDist.clear();
	}

	public void exampleEnded() {
	}

	public void updateDistsEndOfExample(Collection<Word> sentence) {
		logger.info("Updating hypothesis probability distributions.... " );
		printHypNumbers(sentence);
		if (this.tuples.isEmpty())
			return;
		logger.debug("priorDist before discounting:" + priorDist);
		discountPrior(sentence);
		logger.debug("priorDist after discounting:" + priorDist);
		loadPriorIntoCur(sentence);
		logger.debug("curDist after loading prior:" + curDist);
		initCurUniform(sentence);
		logger.debug("curDist after init uniform:" + curDist);

		performLocalEM(EM_ROUNDS);
		logger.info("After EM curDis:" + curDist);
		// aggregateDistributions();
		this.loadCurIntoPrior();

		// System.out.println("After loading into cur:" + priorDist);
		//prune(20);
		//System.out.println("After pruning prior" + priorDist);
		incrementPriorWeights(sentence, 1.0);

		// this.forgetCurrentDist();
		loadLogProbsIntoHyps(sentence);
		logger.info("After processing " + sentence + " PriorDist is " + priorDist);
		this.numTrainingSoFar++;
		System.out.println("done");
	}

	private void printHypNumbers(Collection<Word> sentence) {
		System.out.println();
		for(Word w:sentence)
			System.out.print(w+"::"+this.priorDist.get(w).size()+" ");
		System.out.println();
	}

	private void discountPrior(Collection<Word> sentence) {
		// remove duplicates
		HashSet<Word> words = new HashSet<Word>(sentence);
		for (HasWord w : words) {
			if (!priorDist.containsKey(w)) {
				logger.error("word " + w + " not in prior");
				logger.error("sentence was" + sentence);
				logger.error("sentence had " + sentence.size());
				throw new IllegalArgumentException("trying to discount distribution for non-existing hyp");
			}
			WordLogProbDistribution priorDistW = priorDist.get(w);
			priorDistW.discount(priorDistW.getWeight() / (priorDistW.getWeight() + 1));
		}
	}

	private void initCurUniform(Collection<Word> sentence) {
		// remove duplicate
		HashSet<Word> words = new HashSet<Word>(sentence);
		for (HasWord w : words) {
			if (curDist.containsKey(w)) {
				curDist.get(w).fillZerosUniform(1 / (this.priorDist.get(w).getWeight() + 1));
			} else
				throw new IllegalArgumentException("Word " + w + " not in cur");
		}
	}

	
	private void prune(double d) {
		for (HasWord w : curDist.keySet())
			priorDist.get(w).prune(d);
	}

	private void prune(int topN) {
		for (HasWord w : curDist.keySet())
			priorDist.get(w).prune(topN);
	}

	private void loadLogProbsIntoHyps(Collection<Word> sentence) {
		for (Word w : sentence)
			this.priorDist.get(w).loadLogProbs();
	}

	/**
	 * This should be called ONLY after aggregation and second EM
	 * 
	 */
	private void loadCurIntoPrior() {
		for (HasWord w : curDist.keySet()) {
			WordLogProbDistribution wPrior = priorDist.get(w);
			WordLogProbDistribution wCur = curDist.get(w);
			for (WordHypothesis wh : wCur.keySet()) {

				double newProb = wPrior.getProb(wh) + (1 / (priorDist.get(w).getWeight() + 1))
						* curDist.get(w).getProb(wh);
				wPrior.put(wh, Math.log(newProb));
			}
		}
	}

	private void incrementPriorWeights(Collection<Word> sentence, double inc) {
		// remove duplicate words
		HashSet<Word> set = new HashSet<Word>();
		set.addAll(sentence);
		for (Word w : set)
			priorDist.get(w).incrementWeight(inc);
	}

}
