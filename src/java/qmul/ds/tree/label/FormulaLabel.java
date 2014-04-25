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

import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.boundvariable.BoundFormulaVariable;
import qmul.ds.action.meta.MetaElement;
import qmul.ds.action.meta.MetaFormula;
import qmul.ds.formula.Formula;
import qmul.ds.formula.FormulaMetavariable;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;

/**
 * A formula label Fo(X)
 * 
 * @author mpurver
 */
public class FormulaLabel extends Label implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(FormulaLabel.class);
	public final static String FUNCTOR = "Fo";

	private Formula formula;

	/**
	 * @param formula
	 */
	public FormulaLabel(Formula formula, IfThenElse ite) {
		super(ite);
		this.formula = formula;
	}

	public FormulaLabel(Formula variable) {
		this(variable, null);
	}

	public FormulaLabel(FormulaLabel formulaLabel) {
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
	public ArrayList<MetaElement<?>> getMetas() {
		ArrayList<MetaElement<?>> metas = super.getMetas();
		if (formula instanceof MetaFormula) {
			metas.addAll(((MetaFormula) formula).getMetas());
		} else if (formula instanceof FormulaMetavariable) {

		}
		return metas;
	}

	@Override
	public ArrayList<MetaElement<?>> getBoundMetas() {
		ArrayList<MetaElement<?>> metas = super.getBoundMetas();
		if (formula instanceof BoundFormulaVariable) {
			metas.addAll(((BoundFormulaVariable) formula).getBoundMetas());
		} else if (formula instanceof FormulaMetavariable) {

		}
		return metas;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#instantiate()
	 */
	@Override
	public Label instantiate() {
		return new FormulaLabel(formula.instantiate().evaluate());
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
		FormulaLabel other = (FormulaLabel) obj;
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
		if (!(l instanceof FormulaLabel))
			return false;

		FormulaLabel la = (FormulaLabel) l;
		boolean res = this.formula.subsumes(la.formula);
		if (!res)
			logger.debug(this.formula + ":" + this.formula.getClass() + " does not subsume " + la.formula + ":"
					+ la.formula.getClass());
		return res;
	}

	/**
	 * @param tree
	 * @param context
	 *            (can be null)
	 * @return true if the pointed node is decorated with this label
	 */
	public boolean check(Tree tree, ParserTuple context) {
		return check(tree.getPointedNode());
	}

	/**
	 * @param node
	 * @return true if the node is decorated with this label
	 */
	public boolean check(Node node) {
		boolean result = node.hasLabel(this);
		if (!result) {
			logger.debug("formula label:" + this + " was not on:" + node);
		}
		return result;
	}

	public FormulaLabel clone() {
		return new FormulaLabel(this);
	}

}
