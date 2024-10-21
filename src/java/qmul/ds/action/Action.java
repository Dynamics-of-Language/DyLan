/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import qmul.ds.Context;
import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.tree.Tree;
import edu.stanford.nlp.util.Pair;

/**
 * An IF-THEN-ELSE action template
 * 
 * @author mpurver
 */
public class Action implements Serializable {

	private static final long serialVersionUID = 1L;

	protected static Logger logger = Logger.getLogger(Action.class);

	protected String name;
	protected Effect action;
	protected boolean backtrackOnSuccess = false;

	public Action() {}

	public Action(String name) {
		this.name = name;
	}

	public Action(String name, Effect action) {
		this.name = name;
		this.action = action;
	}

	public Action(String name, Effect action, boolean backtrack) {
		this.name = name;
		this.action = action;
		this.backtrackOnSuccess = backtrack;
	}

	public Action(Action a) {
		this(a.getName(), a.getEffect());
	}

	public void setBacktrackOnSuccess(boolean a) {
		this.backtrackOnSuccess = a;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @return true if this action is to be repeatedly applied until we cannot backtrack with a successful result
	 */
	public boolean backtrackOnSuccess() {
		return this.backtrackOnSuccess;
	}

	public Effect getEffect() {
		return this.action;
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
		return action.execTupleContext(tree, context);
	}

	public <E extends DAGEdge, U extends DAGTuple, T extends Tree> T exec(T tree, Context<U,E> context) {
		return action.exec(tree, context);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return name;// + ":\n" + action;
	}

	public Action instantiate() {
		// by default, subclasse should override.
		return new Action(this.name, this.action.instantiate());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		
		if (this == o)
			return true;
		if (o == null)
			return false;
		else if (this.getClass() != o.getClass())
			return false;
		else {
			// by default (and for computational actions), name is unique
			Action a = (Action) o;			
			return a.getName().equals(getName());
		}
	}

	public <T extends Tree> Collection<Pair<? extends Action, T>> execExhaustively(T tree, ParserTuple context) {
		IfThenElse ite = (IfThenElse) action;

		Collection<Pair<IfThenElse, T>> all = ite.execExhaustively(tree, context);
		if (all == null)
			return null;
		if (all.isEmpty())
			return null;
		Collection<Pair<? extends Action, T>> allA = new ArrayList<Pair<? extends Action, T>>();
		for (Pair<IfThenElse, T> p : all) {
			allA.add(new Pair<Action, T>(new Action(getName(), p.first(), this.backtrackOnSuccess), p.second()));
		}
		return allA;
	}

	/*
	 * public <T extends Tree> Collection<T> execExhaustively(T tree, ParserTuple context){ //null implementation
	 * because this method is currently only overridden by the ComputationalAction class return null;}
	 */

	public String toDebugString() {
		return name + ":\n" + action;
	}

}
