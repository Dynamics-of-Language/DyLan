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
import java.util.Collection;
import java.util.List;

import edu.stanford.nlp.util.Pair;

import qmul.ds.ParseState;
import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.EffectFactory;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.learn.Hypothesiser;
import qmul.ds.learn.LexicalHypothesis;
import qmul.ds.tree.Tree;

/**
 * A generally available computational {@link Action}
 * 
 * @author mpurver
 */
public class ComputationalAction extends Action implements Comparable<ComputationalAction> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean alwaysGood = false;

	public ComputationalAction(String name, Effect action) {
		super(name, action);
	}

	public ComputationalAction(String name, Effect action, boolean a, boolean backtrack) {
		this(name, action);
		this.alwaysGood = a;
		this.backtrackOnSuccess = backtrack;
	}

	/**
	 * @param name
	 *            the name of this action
	 * @param lines
	 *            a {@link String} representation as used in lexicon specs
	 */
	public ComputationalAction(String name, List<String> lines) {
		
		super(name, EffectFactory.create(lines));
		
	}

	/**
	 * @return true if this {@link ComputationalAction} should always be applied if it can be, i.e. on application,
	 *         remove the previous {@link Tree} from the {@link ParseState}
	 */
	public boolean isAlwaysGood() {
		return alwaysGood;
	}

	/**
	 * @param alwaysGood
	 *            true if this {@link ComputationalAction} should always be applied if it can be, i.e. on application,
	 *            remove the previous {@link Tree} from the {@link ParseState}
	 */
	public void setAlwaysGood(boolean alwaysGood) {
		this.alwaysGood = alwaysGood;
	}

	/**
	 * 
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ComputationalAction arg0) {
		// ensure alwaysGood actions are returned first by iterators
		if (alwaysGood && !arg0.alwaysGood)
			return -1;
		if (!alwaysGood && arg0.alwaysGood)
			return 1;
		return hashCode() - arg0.hashCode();
	}

	/*
	 * public Action instantiate() { if (action instanceof IfThenElse) { IfThenElse ite = (IfThenElse) action; return
	 * new ComputationalAction(super.getName(), ite.instantiate()); } else return this; }
	 */

	public ComputationalAction instantiate() {
		if (getName().startsWith(Hypothesiser.HYP_ADJUNCTION_PREFIX)||getName().startsWith("link"))
			return new ComputationalAction(name, this.action);
		
		return new ComputationalAction(name, this.action.instantiate());
		
		

		
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
			allA.add(new Pair<ComputationalAction, T>(new ComputationalAction(getName(), p.first(), this.alwaysGood,
					this.backtrackOnSuccess), p.second()));
		}
		return allA;
	}

	public String getName() {
		/*if (super.getName().equals("thinning") || super.getName().equals("completion")
				|| super.getName().equals("merge")) {
			return super.getName() + "(" + ((IfThenElse) this.getEffect()).getIFClause()[0] + ")";
		}*/
		return super.getName();
	}
	
	public boolean equals(Object other)
	{
		if (!(other instanceof ComputationalAction))
			return false;
		ComputationalAction o=(ComputationalAction)other;
		return o.getName().equals(o.getName())&&this.action.equals(o.action);
		
	}
}
