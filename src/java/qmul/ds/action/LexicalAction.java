/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import qmul.ds.Context;
import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.EffectFactory;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.formula.Formula;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;

/**
 * A lexical {@link Action}. The {@link Effect} associated with this action is assumed to be an instance of
 * {@link IfThenElse}.
 * 
 * Now with the need to partition verb actions, lexical actions can contain a sequence of {@link IfThenElse} actions,
 * with their own respective triggers. These are run in order. If any of them aborts the whole action aborts
 * 
 * 
 * @author mpurver
 */
public class LexicalAction extends Action {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String word;
	protected String actionType = null;
	protected Effect[] actions = null;
	protected Formula semantics = null;
	protected int rank;
	protected double prob;
	private boolean noLeftAdjustment;

	public LexicalAction(String word, Effect[] actions, String mytype) {
		super(word, null);
		this.word = word;
		this.actions = actions;
		this.actionType = mytype;
	}

	private Effect[] flatten(ArrayList<Action> actions)
	{
		List<Effect> result=new ArrayList<Effect>();
		for(Action a:actions)
		{
			if (a instanceof LexicalAction)
			{
				for(Effect e:((LexicalAction) a).getEffects())
				{
					result.add(e);
				}
			}else
				result.add(a.getEffect());
				
		}
		Effect[] effects=new Effect[result.size()];
		return result.toArray(effects);
	}
	
	public LexicalAction(String word, ArrayList<Action> actions)
	{
		super(word, null);
		Effect[] ites=flatten(actions);
		
		this.word=word;
		this.actions=ites;	
		
	}
	/**
	 * @return the word
	 */
	public String getWord() {
		return word;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public int getRank() {
		return rank;
	}

	public void setProb(double d) {
		this.prob = d;
	}

	public double getProb() {
		return this.prob;
	}

	/**
	 * @return the actionType
	 */
	public String getLexicalActionType() {
		return actionType;
	}

	/**
	 * @param name
	 *            the name of this action
	 * @param lines
	 *            a {@link String} representation as used in lexicon specs
	 */
	public LexicalAction(String word, List<String> lines, Formula sem, String actiontype) {

		super(word, null);
		this.word = word;
		this.actionType = actiontype;
		List<Integer> ifIndices = EffectFactory.getIfIndices(lines);
		this.actions = EffectFactory.createMultiple(lines, ifIndices);
		this.semantics = sem;
		logger.debug("Created Partitioned Lex Action:" + this);

	}

	public LexicalAction(String word, List<String> lines, String actiontype, boolean noLeftAdjustment) {

		super(word, null);
		this.noLeftAdjustment=noLeftAdjustment;
		this.word = word;
		this.actionType = actiontype;
		List<Integer> ifIndices = EffectFactory.getIfIndices(lines);
		this.actions = EffectFactory.createMultiple(lines, ifIndices);
		this.semantics = null;
		logger.debug("Created Partitioned Lex Action:" + this);

	}

	/**
	 * @param name
	 *            the name of this action
	 * @param lines
	 *            a {@link String} representation as used in lexicon specs
	 */
	public LexicalAction(String word, List<String> lines) {

		super(word, null);
		this.word = word;
		List<Integer> ifIndices = EffectFactory.getIfIndices(lines);
		this.actions = EffectFactory.createMultiple(lines, ifIndices);
		this.semantics = null;
		this.actionType = null;
		logger.debug("Created Partitioned Lex Action:" + this);

	}

	public LexicalAction(String word, Effect e) {
		super(word, null);
		this.word = word;
		this.actions = new IfThenElse[1];
		this.actions[0] = e;
	}

	public LexicalAction instantiate() {

		Effect[] newActions = new IfThenElse[actions.length];
		for (int i = 0; i < this.actions.length; i++)
			newActions[i] = this.actions[i].instantiate();

		return new LexicalAction(word, newActions, actionType);
	}

	/**
	 * Apply a lexical/computational action to a {@link Tree} (optionally given a context {@link ParserTuple})
	 * 
	 * @param tree
	 * @param context
	 *            (can be null)
	 * @return a new {@link Tree} if successful, null otherwise
	 */

	public <T extends Tree> T execTupleContext(T tree, ParserTuple context) {

		T prev = tree;
		for (Effect action : actions) {
			prev = action.execTupleContext(prev, context);
			if (prev == null)
				return null;
		}

		return prev;
	}
	
	/**
	 * Apply a lexical/computational action to a {@link Tree} (optionally given a context {@link ParserTuple})
	 * 
	 * @param tree
	 * @param context
	 *            (can be null)
	 * @return a new {@link Tree} if successful, null otherwise
	 */

	public <E extends DAGEdge, U extends DAGTuple, T extends Tree> T exec(T tree, Context<U,E> context) {

		T prev = tree;
		for (Effect action : actions) {
			prev = action.exec(prev, context);
			if (prev == null)
				return null;
		}

		return prev;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 
	@Override
	public String toString() {
		String result = getName() + "\n";
		for (Effect action : actions)
			result += action + "\n";

		return result;
	}*/

	public Effect[] getEffects() {
		return this.actions;
	}

	public Formula getSemantics() {
		return this.semantics;
	}

	public List<Label> getTriggers() {
		List<Label> result = new ArrayList<Label>();
		if (this.action != null) {
			return Arrays.asList(((IfThenElse) action).getTriggers());

		} else if (this.actions != null) {
			for (Effect action : actions) {
				IfThenElse ite = (IfThenElse) action;
				result.addAll(Arrays.asList(ite.getTriggers()));
			}
			return result;
		} else
			return null;

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
		// word = name, already in super method
		result = prime * result + Arrays.hashCode(actions);
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
		// word = name, already in super method
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		LexicalAction other = (LexicalAction) obj;
		if (!Arrays.equals(actions, other.actions))
			return false;
		return true;
	}
	
	public boolean requiresLeftAdjustment()
	{
		return !noLeftAdjustment;
	}

}
