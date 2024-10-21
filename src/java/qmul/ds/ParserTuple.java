/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import edu.stanford.nlp.util.Pair;
import qmul.ds.action.Action;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.formula.TTRFormula;
import qmul.ds.tree.Tree;

/**
 * A parser tuple (member of a {@link ParseState}). At its simplest, just a
 * {@link Tree}, but more complex {@link Parser}s may add context information,
 * probabilities etc
 * 
 * @author mpurver
 */
public class ParserTuple implements Comparable<ParserTuple>, Cloneable {

	Logger logger = Logger.getLogger(ParserTuple.class);
	protected Tree tree;

	protected TTRFormula semantics;

	public TTRFormula getSemantics() {
		if (this.semantics != null)
			return semantics;

		semantics = tree.getMaximalSemantics();
		return semantics;
	}
	
	public <T extends DAGTuple, E extends DAGEdge> TTRFormula getSemantics(Context<T,E> c) {
		if (c==null) {
			logger.error("ERROR: null context when computing maximal semantics of :"+this);
			logger.error("Computing semantics with new variables relative to tree, rather than context");
			return getSemantics();
		}
		
		if (this.semantics != null)
			return semantics;

		semantics = tree.getMaximalSemantics(c);
		return semantics;
	}


	public void setMaximalSemantics(TTRFormula maximalSemantics) {
		this.semantics = maximalSemantics;
	}

	public ParserTuple(Tree t, TTRFormula f) {
		this.tree = t;
		this.semantics = f;
	}

	/**
	 * A new tuple containing an AXIOM tree
	 */
	public ParserTuple() {
		tree = new Tree();
	}

	/**
	 * A new tuple containing a cloned copy of the given tree
	 * 
	 * @param tree
	 */
	public ParserTuple(Tree tree) {
		this.tree = new Tree(tree);
	}

	/**
	 * A new tuple containing a cloned copy of the given tree
	 * 
	 * @param tree
	 */
	public ParserTuple(ParserTuple tuple) {
		this.tree = (tuple.tree == null) ? null : new Tree(tuple.tree);
		this.semantics = tuple.semantics == null ? null : tuple.semantics
				.clone();
	}

	public ParserTuple(TTRFormula ttrRecordType) {
		this.semantics = ttrRecordType;
	}

	/**
	 * @return the tree
	 */
	public Tree getTree() {
		return tree;
	}

	public void setTree(Tree newtree) {
		tree = newtree;
	}

	/**
	 * @param action
	 * @param word
	 * @return the result of applying action to a copy of this tuple; this will
	 *         be null if the action aborts. Word may be null if this is a
	 *         non-lexical action
	 */
	public ParserTuple execAction(Action action, String word) {
		Tree result = action.execTupleContext(tree.clone(), this);
		if (result == null) {
			return null;
		}
		return new ParserTuple(result);
	}

	public Collection<? extends ParserTuple> execExhaustively(Action ca,
			String word) {

		Collection<Pair<? extends Action, Tree>> trees = ca.execExhaustively(
				tree, this);

		if (trees == null)
			return null;
		if (trees.isEmpty())
			return null;
		Collection<ParserTuple> result = new ArrayList<ParserTuple>();
		for (Pair<? extends Action, Tree> p : trees)
			result.add(new ParserTuple(p.second()));

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
		return new ParserTuple(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ParserTuple other) {

		if (this.tree.isComplete()) {
			if (other.tree.isComplete()) {
				return (other.hashCode() - this.hashCode());
			} else {
				return -1;
			}
		} else {
			if (other.tree.isComplete()) {
				return 1;
			} else {
				int r = this.tree.numRequirements()
						- other.tree.numRequirements();
				if (r == 0) {
					return (other.hashCode() - this.hashCode());
				}
				return r;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		// logger.debug("using hashcode in ParserTuple");
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tree == null) ? 0 : tree.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// logger.debug("using equals in parsertuple");
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParserTuple other = (ParserTuple) obj;
		if (tree == null) {
			if (other.tree != null)
				return false;
		} else if (!tree.equals(other.tree))
			return false;
		return true;
	}

	public boolean isComplete() {
		return this.tree.isComplete();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = (tree == null) ? "Tree:null" : tree.toString();
		//s += "\nSem:" + (getSemantics() == null ? "null" : getSemantics());
		return s;
	}

	public boolean subsumes(ParserTuple t) {
		if (getSemantics() != null) {
			if (t.getSemantics() == null)
				return false;
			
			TTRFormula headLess=getSemantics().removeHead();
			return headLess.subsumes(t.getSemantics().removeHead());
//			return getSemantics().subsumes(t.getSemantics());
		}
		if (this.tree == null) {
			if (t.tree == null)
				return true;
			return false;
		}

		return tree.subsumes(t.tree);
	}
}
