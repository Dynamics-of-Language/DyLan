package qmul.ds.learn;

import csli.util.Pair;

public class WordLogProb extends Pair<String, Double> implements Comparable<WordLogProb> {

    public WordLogProb(String word, double prob) {
        super(word, prob);
    }

    public String getWord() {
        return this.first();
    }

    public double getProb() {
        return this.second();
    }

    @Override
    public int compareTo(WordLogProb o) {
        return Double.compare(o.getProb(), getProb());
    }

    @Override
    public String toString() {
        return getWord() + " " + getProb();
    }
}

