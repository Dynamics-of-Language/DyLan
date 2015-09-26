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
import java.util.ListIterator;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.tree.Tree;

/**
 * A {@link ScoredParserTuple} in a context lattice, with {@link ScoredParserTuple}s as nodes and word/action sequence
 * edges, from which possible contextual word & action sequences can be recovered (in order of occurrence i.e. most
 * recent last)
 * 
 * @author mpurver
 */
public class LatticeParserTuple extends ScoredParserTuple {

	private static Logger logger = Logger.getLogger(LatticeParserTuple.class);

	private ArrayList<String> words;
	private ArrayList<Action> actions;
	private LatticeParserTuple previous;

	/**
	 * A new tuple containing the AXIOM tree with a zero score in an empty context (new lattice with only this node)
	 */
	public LatticeParserTuple() {
		super();
		words = new ArrayList<String>();
		actions = new ArrayList<Action>();
		previous = null;
	}

	/**
	 * A new tuple containing the AXIOM tree with a (shallow) cloned copy of the context if present, an empty context
	 * otherwise
	 * 
	 * @param tuple
	 */
	public LatticeParserTuple(LatticeParserTuple tuple) {
		this();
		if (tuple instanceof LatticeParserTuple) {
			previous = ((LatticeParserTuple) tuple);
		}
	}

	/**
	 * A new tuple containing a cloned copy of the given tree, words and actions, and a ref to the previous context
	 * 
	 * @param tree
	 */
	public LatticeParserTuple(Tree tree, ArrayList<String> words, ArrayList<Action> actions, LatticeParserTuple previous) {
		super(tree);
		this.words = new ArrayList<String>(words);
		this.actions = new ArrayList<Action>(actions);
		this.previous = previous;
	}

	/**
	 * @return the word sequence in context (most recent last)
	 */
	public ArrayList<String> getWords() {
		return words;
	}

	/**
	 * @return the action sequence in context (most recent last)
	 */
	public ArrayList<Action> getActions() {
		return actions;
	}

	/**
	 * @return the previous {@link ParserTuple} in context (most recent)
	 */
	public LatticeParserTuple getPrevious() {
		return previous;
	}

	/**
	 * Add a word to the context
	 * 
	 * @param word
	 */
	public void addWord(String word) {
		words.add(word);
	}

	/**
	 * Add an action to the context
	 * 
	 * @param action
	 */
	public void addAction(Action action) {
		actions.add(action);
	}

	/**
	 * @return a contextual word iterator, most recent first - use hasPrevious(), previous() to traverse
	 */
	public ListIterator<String> getWordsByRecency() {
		return words.listIterator(words.size());
	}

	/**
	 * @return a contextual action iterator, most recent first - use hasPrevious(), previous() to traverse
	 */
	public ListIterator<Action> getActionsByRecency() {
		return actions.listIterator(actions.size());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.ParserTuple#execAction(qmul.ds.action.Action, java.lang.String)
	 */
	@Override
	public LatticeParserTuple execAction(Action action, String word) {
		Tree result = action.execTupleContext(getTree().clone(), this);
		logger.trace("execd action " + action);
		logger.trace("result " + result);
		if (result == null) {
			return null;
		}
		LatticeParserTuple tuple = new LatticeParserTuple(result, words, actions, previous);

		tuple.addAction(action);
		tuple.addWord(word);

		return tuple;
	}

	public boolean equals(Object obj) {

		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass()) {
			return false;
		}
		LatticeParserTuple other = (LatticeParserTuple) obj;
		if (tree == null) {
			if (other.tree != null)
				return false;
		} else if (!tree.equals(other.tree))
			return false;

		/*
		 * else if (!this.actions.equals(other.actions)) {
		 * 
		 * return false; } else if (!this.words.equals(other.words)) return false;
		 */

		return true;
	}

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
				return (other.hashCode() - this.hashCode());
			}
		}
	}
}
