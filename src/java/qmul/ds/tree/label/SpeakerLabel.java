/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.tree.label;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import qmul.ds.Context;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.boundvariable.BoundFormulaVariable;
import qmul.ds.action.meta.Meta;
import qmul.ds.action.meta.MetaFormula;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.formula.AtomicFormula;
import qmul.ds.formula.Formula;
import qmul.ds.formula.FormulaMetavariable;
import qmul.ds.tree.Tree;

/**
 * Speaker the current word: Speaker(X), where X is a meta that instantiates to the current speaker.
 * 
 * @author arash
 */
public class SpeakerLabel extends Label implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(SpeakerLabel.class);
	public final static String FUNCTOR = "Speaker";

	private Formula formula;

	/**
	 * @param formula
	 */
	public SpeakerLabel(Formula formula, IfThenElse ite) {
		super(ite);
		this.formula = formula;
	}

	public SpeakerLabel(Formula variable) {
		this(variable, null);
	}

	public SpeakerLabel(SpeakerLabel formulaLabel) {
		this(formulaLabel.getFormula().clone(), null);
	}

	/**
	 * @return the formula
	 */
	public Formula getFormula() {
		return formula;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#getMetas()
	 */
	@Override
	public ArrayList<Meta<?>> getMetas() {
		ArrayList<Meta<?>> metas = super.getMetas();
		if (formula instanceof MetaFormula) {
			metas.addAll(((MetaFormula) formula).getMetas());
		} else if (formula instanceof FormulaMetavariable) {

		}
		return metas;
	}

	@Override
	public ArrayList<Meta<?>> getBoundMetas() {
		ArrayList<Meta<?>> metas = super.getBoundMetas();
		if (formula instanceof BoundFormulaVariable) {
			metas.addAll(((BoundFormulaVariable) formula).getBoundMetas());
		} else if (formula instanceof FormulaMetavariable) {

		}
		return metas;
	}
	
	/**
	 * @param tree
	 * @param context (now a DAG). By default, check with null context, using old tuple context methods.
	 *            
	 * @return true if the pointed node is decorated with this label
	 */
	public <E extends DAGEdge, U extends DAGTuple> boolean check(Tree t, Context<U,E> context)
	{
		String speaker=context.getCurrentSpeaker();

		Formula speakerF=new AtomicFormula(speaker);
		return this.formula.equals(speakerF);
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#instantiate()
	 */
	@Override
	public Label instantiate() {
		return new SpeakerLabel(formula.instantiate().evaluate());
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
		result = prime * result + ((formula == null) ? 0 : formula.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// logger.debug("testing formula equality");
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SpeakerLabel other = (SpeakerLabel) obj;
		if (formula == null) {
			if (other.formula != null)
				return false;
		} else if (!formula.equals(other.formula)) {
			logger.debug("This formula: " + formula + " deemed unequal to:" + other.formula);
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR + "(" + formula + ")";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#toUnicodeString()
	 */
	@Override
	public String toUnicodeString() {
		return FUNCTOR + "(" + formula.toUnicodeString() + ")";
	}

	public boolean subsumes(Label l) {
		if (!(l instanceof SpeakerLabel))
			return false;

		SpeakerLabel la = (SpeakerLabel) l;
		boolean res = this.formula.subsumes(la.formula);
		if (!res)
			logger.debug(this.formula + ":" + this.formula.getClass() + " does not subsume " + la.formula + ":"
					+ la.formula.getClass());
		return res;
	}

	
	

	public SpeakerLabel clone() {
		return new SpeakerLabel(this);
	}

}
