/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds;

import qmul.ds.action.Action;
import qmul.ds.tree.Tree;

/**
 * A DS {@link ParserTuple} with a score (for parse preferences)
 * 
 * @author mpurver
 */
public class ScoredParserTuple extends ParserTuple {

	private double score;

	/**
	 * A new tuple containing an AXIOM tree with a zero score
	 */
	public ScoredParserTuple() {
		super();
		score = 0.0;
	}

	/**
	 * A new tuple containing a cloned copy of the given tree with a zero score
	 * 
	 * @param tree
	 */
	public ScoredParserTuple(Tree tree) {
		super(tree);
		score = 0.0;
	}

	/**
	 * A new tuple containing a cloned copy of the given tree with the given score
	 * 
	 * @param tree
	 */
	public ScoredParserTuple(Tree tree, double score) {
		super(tree);
		this.score = score;
	}

	/**
	 * A new tuple containing a cloned copy of the given tree; score is copied, or set to zero if tuple is not a
	 * {@link ScoredParserTuple}
	 * 
	 * @param tuple
	 */
	public ScoredParserTuple(ParserTuple tuple) {
		super(tuple);
		if (tuple instanceof ScoredParserTuple) {
			score = ((ScoredParserTuple) tuple).score;
		} else {
			score = 0.0;
		}
	}

	/**
	 * @return the score
	 */
	public double getScore() {
		return score;
	}

	/**
	 * @param score
	 *            the score to set
	 */
	public void setScore(double score) {
		this.score = score;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.ParserTuple#execAction(qmul.ds.action.Action, java.lang.String)
	 */
	@Override
	public ScoredParserTuple execAction(Action action, String word) {
		Tree result = action.execTupleContext(getTree().clone(), this);
		if (result == null) {
			return null;
		}
		double score = getScore() + logProb(action);
		return new ScoredParserTuple(result, score);
	}

	/**
	 * @param action
	 * @return the conditional probability of action given this tuple's tree, context etc
	 */
	private double logProb(Action action) {
		// TODO
		return 0.0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.ParserTuple#compareTo(qmul.ds.ParserTuple)
	 */
	@Override
	public int compareTo(ParserTuple other) {
		if (other instanceof ScoredParserTuple) {
			ScoredParserTuple scored = (ScoredParserTuple) other;
			if (scored.score == score) {
				return super.compareTo(other);
			} else {
				return Double.compare(score, scored.score);
			}
		}
		return super.compareTo(other);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(score);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScoredParserTuple other = (ScoredParserTuple) obj;
		if (Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return score + ":" + super.toString();
	}

}
