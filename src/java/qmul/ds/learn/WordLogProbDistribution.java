package qmul.ds.learn;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;

public class WordLogProbDistribution extends HashMap<WordHypothesis, Double> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1876125484204150012L;
	private HasWord word;
	private double weight = 0.0;
	private int maxID = 0;
	

	public WordLogProbDistribution(HasWord w) {
		super();
		this.word = w;
	}

	public WordLogProbDistribution(HasWord w, int maxID) {
		this(w);
		this.maxID = maxID;
	}

	public WordLogProbDistribution(HasWord w, double we) {
		super();
		this.word = w;
		this.weight = we;
	}

	public WordLogProbDistribution(HasWord w, double we, int maxID) {
		this(w, we);
		this.maxID = maxID;
	}

	public void incrementWeight(double w) {
		weight += w;
	}

	public HasWord getWord() {
		return this.word;
	}

	public double getWeight() {
		return this.weight;
	}

	

	public void setWeight(double w) {
		this.weight = w;
	}

	public void loadLogProbs() {
		for (WordHypothesis wh : this.keySet()) {
			wh.setLogProb(get(wh));
		}

	}

	public Set<WordHypothesis> getAllHyps() {
		return keySet();
	}

	public void addHyp(WordHypothesis wh) {
		// give positive logProb meaning prob 0
		put(wh, 1.0);
	}

	public Double getProb(WordHypothesis wh) {
		if (!containsKey(wh))
			return null;
		double logProb = get(wh);
		if (logProb > 0)
			return 0.0;

		return Math.exp(logProb);
	}

	public void makeUniform() {
		double probMass = 0;
		double howmanyZeros = 0;
		for (WordHypothesis wh : keySet()) {
			probMass += getProb(wh);
			if (getProb(wh) == 0)
				howmanyZeros++;

		}
		if (howmanyZeros == 0 || probMass >= 1) {
			System.out.println("returning from make Uniform with no changes");
		}
		// System.out.println("uniforming ... probMass:"+probMass);

		// System.out.println("uniforming ... probMass:"+probMass);
		for (WordHypothesis wh : keySet()) {
			if (getProb(wh) == 0) {
				double logprob = Math.log((1 - probMass) / howmanyZeros);
				// System.out.println("setting "+wh+ "to"+logprob);
				put(wh, logprob);
			}

		}

	}

	/**
	 * constructs the weighted aggregate of this distribution with the other. Returns a NEW distribution, leaving this
	 * and other unmodified.
	 * 
	 * @param other
	 * @return
	 */
	public WordLogProbDistribution weightedAggregate(WordLogProbDistribution other) {
		if (!this.word.equals(other.word))
			throw new IllegalArgumentException("Cannot aggregate different word distributions");

		// System.out.println("Aggregating "+this);
		// System.out.println("With:"+other);
		WordLogProbDistribution aggregate = new WordLogProbDistribution(other.getWord());
		if (this.maxID > other.maxID)
			aggregate.maxID = this.maxID;
		else
			aggregate.maxID = other.maxID;
		Set<WordHypothesis> all = new HashSet<WordHypothesis>();
		all.addAll(keySet());
		all.addAll(other.keySet());
		for (WordHypothesis wh : all) {
			double thisWhProb = 0.0;
			double otherWhProb = 0.0;
			if (containsKey(wh))
				thisWhProb = getProb(wh);
			if (other.containsKey(wh))
				otherWhProb = other.getProb(wh);
			double aggregateProb = (this.weight * thisWhProb + other.weight * otherWhProb)
					/ (this.weight + other.weight);
			aggregate.put(wh, Math.log(aggregateProb));
		}

		return aggregate;
	}

	public int getFreshHypID() {
		maxID++;
		return maxID;
	}

	public List<WordHypothesis> getSortAllHyps() {
		// this.loadLogProbs();
		List<WordHypothesis> result = new ArrayList<WordHypothesis>();
		result.addAll(keySet());
		Collections.sort(result, new WordHypothesisProbComparator());
		return result;

	}

	

	public String toString() {
		String result = this.word.toString() + ":\n";
		DecimalFormat df = new DecimalFormat("#.######");
		double sum = 0.0;
		for (WordHypothesis wh : keySet()) {
			double prob = getProb(wh);
			result += wh.getName() + "-->" + df.format(getProb(wh)) + "\n";
			sum += prob;
		}
		result += "SUM-->" + sum;
		result += "\nTotal Hyps -->" + size();
		result += "\nMaxID -->" + maxID;
		return result;
	}

	public void prune(int topN) {
		if (size()<=topN)
			return;
		
		List<WordHypothesis> all = getSortAllHyps();
		double prunedMass = 0;
		for (int i = all.size()-1; i > topN; i--) {
			WordHypothesis wh = all.get(i);
			prunedMass += getProb(wh);
			remove(wh);
		}
		for (WordHypothesis wh : keySet()) {
			double oldProb = getProb(wh);
			double newProb = oldProb * (1 + prunedMass / (1 - prunedMass));
			put(wh, Math.log(newProb));
		}
		loadLogProbs();
	}
	
	public void prune(double limit) {
		Map<WordHypothesis, Double> newMap = new HashMap<WordHypothesis, Double>();

		double prunedMass = 0;
		for (WordHypothesis wh : keySet()) {
			if (this.getProb(wh) < limit) {
				prunedMass += this.getProb(wh);

			}
		}
		for (WordHypothesis wh : keySet()) {
			double oldProb = getProb(wh);
			if (oldProb > limit) {
				double newProb = oldProb * (1 + prunedMass / (1 - prunedMass));
				newMap.put(wh, Math.log(newProb));
			}

		}
		clear();
		putAll(newMap);
	}

	public void discount(double discountFactor) {

		for (WordHypothesis wh : keySet()) {
			double whProb = getProb(wh);
			if (whProb >= 0) {
				put(wh, Math.log(discountFactor * whProb));
			}
		}

	}

	public void fillZerosUniform(double discountFactor) {
		int numZeros = 0;
		for (WordHypothesis wh : keySet()) {
			if (getProb(wh) == 0)
				numZeros++;

		}
		if (numZeros == 0)
			return;
		for (WordHypothesis wh : keySet()) {
			if (getProb(wh) == 0)
				put(wh, Math.log(discountFactor / numZeros));

		}

	}
}

class WordHypothesisProbComparator implements Comparator<WordHypothesis> {

	@Override
	public int compare(WordHypothesis wh1, WordHypothesis wh2) {
		if (wh1.getProb() > wh2.getProb())
			return -1;
		else if (wh1.getProb() < wh2.getProb())
			return +1;

		return 0;
	}

}
