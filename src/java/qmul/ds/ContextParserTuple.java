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
import java.util.ListIterator;

import org.apache.log4j.Logger;

import edu.stanford.nlp.util.Pair;

import qmul.ds.action.Action;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.tree.Tree;

/**
 * A DS {@link ParserTuple} with contextual word & action sequences (in order of occurrence i.e. most recent last)
 * 
 * @author mpurver
 */
public class ContextParserTuple extends ParserTuple {

	private static Logger logger = Logger.getLogger(ContextParserTuple.class);

	private ArrayList<String> words;
	private ArrayList<Action> actions;
	private ContextParserTuple previous;

	/**
	 * A new tuple containing the AXIOM tree with an empty context
	 */
	public ContextParserTuple() {
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
	public ContextParserTuple(ContextParserTuple tuple) {
		this();
		if (tuple instanceof ContextParserTuple) {
			previous = ((ContextParserTuple) tuple);
		}
	}

	/**
	 * A new tuple containing a cloned copy of the given tree, words and actions, and a ref to the previous context
	 * 
	 * @param tree
	 */
	public ContextParserTuple(Tree tree, ArrayList<String> words, ArrayList<Action> actions, ContextParserTuple previous) {
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
	public ContextParserTuple getPrevious() {
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
		// if it's a lexical action, it can be associated with a sequence of effects
		// add to context as a sequence of lexical actions all associated with the same
		// word
		/*
		 * if (action instanceof LexicalAction) { LexicalAction la = (LexicalAction) action;
		 * 
		 * for (Effect e : la.getEffects()) { actions.add(new LexicalAction(la.getWord(), e)); } } else
		 */

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
	public ContextParserTuple execAction(Action action, String word) {
		Tree result = action.exec(getTree().clone(), this);
		logger.trace("execd action " + action);
		logger.trace("result " + result);
		if (result == null) {
			return null;
		}
		ContextParserTuple tuple = new ContextParserTuple(result, words, actions, previous);

		tuple.addAction(action.instantiate());
		tuple.addWord(word);

		return tuple;
	}

	public Collection<ContextParserTuple> execExhaustively(Action ca, String word) {

		Collection<Pair<? extends Action, Tree>> trees = ca.execExhaustively(tree, this);
		if (trees == null)
			return null;
		if (trees.isEmpty())
			return null;
		Collection<ContextParserTuple> result = new ArrayList<ContextParserTuple>();
		for (Pair<? extends Action, Tree> p : trees) {
			ContextParserTuple tuple = new ContextParserTuple(p.second(), words, actions, previous);

			tuple.addAction(p.first());
			tuple.addWord(word);
			result.add(tuple);
		}
		return result;
	}

	public boolean equals(Object obj) {
		logger.trace("Equals in ContextParserTuple");
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass()) {
			return false;
		}
		ContextParserTuple other = (ContextParserTuple) obj;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.ParserTuple#compareTo(qmul.ds.ParserTuple)
	 */
	@Override
	public int compareTo(ParserTuple other) {
		return super.compareTo(other);
	}
}
