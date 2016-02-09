/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.tree.label;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.boundvariable.BoundFormulaVariable;
import qmul.ds.action.meta.Meta;
import qmul.ds.action.meta.MetaFormula;
import qmul.ds.formula.Formula;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;

/**
 * 
 * @author Arash
 * 
 */
public class ScopeDepSaturationLabel extends Label {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String FUNCTOR = "dep_saturated";
	public static final Pattern DEP_SATURATED_PATTERN = Pattern.compile(FUNCTOR + "\\(\\s*(.+)\\s*,\\s*(.+)\\s*\\)");

	private Formula f1;
	private Formula f2;

	public ScopeDepSaturationLabel(String s, IfThenElse ite) {
		super(ite);
		Matcher m = DEP_SATURATED_PATTERN.matcher(s);
		if (m.matches()) {
			f1 = Formula.create(m.group(1));
			f2 = Formula.create(m.group(2));

		} else
			throw new IllegalArgumentException("Unrecognised Dependency Saturation Label String: " + s + "END");
	}

	public ScopeDepSaturationLabel(String s) {
		this(s, null);
	}

	public ScopeDepSaturationLabel(Formula f1, Formula f2) {
		super(null);
		this.f1 = f1;
		this.f2 = f2;
	}

	public ArrayList<Meta<?>> getMetas() {
		ArrayList<Meta<?>> result = new ArrayList<Meta<?>>();
		if (f1 instanceof MetaFormula)
			result.addAll(((MetaFormula) f1).getMetas());
		if (f2 instanceof MetaFormula)
			result.addAll(((MetaFormula) f2).getMetas());

		return result;
	}

	public ArrayList<Meta<?>> getBoundMetas() {
		ArrayList<Meta<?>> result = new ArrayList<Meta<?>>();
		if (f1 instanceof BoundFormulaVariable)
			result.addAll(((BoundFormulaVariable) f1).getBoundMetas());
		if (f2 instanceof BoundFormulaVariable)
			result.addAll(((BoundFormulaVariable) f2).getBoundMetas());

		return result;
	}

	public ScopeDepSaturationLabel instantiate() {
		return new ScopeDepSaturationLabel(f1.instantiate(), f2.instantiate());
	}

	public boolean check(Node n) {

		Formula f1Instance = f1.instantiate();
		Formula f2Instance = f2.instantiate();
		if (f1Instance instanceof MetaFormula || f2Instance instanceof MetaFormula) {
			logger.debug("One of ScopeDepSaturation formulas weren't instantiated. Returning false");

			return false;
		}

		Label tyT = LabelFactory.create("ty(t)");
		if (!n.hasLabel(tyT))
			return false;

		ArrayList<ScopeStatement> toBeChecked = new ArrayList<ScopeStatement>();

		for (Label l : n) {
			if (l instanceof ScopeStatement) {
				ScopeStatement ss = (ScopeStatement) l;
				Formula narrowScoped = ss.getNarrowest();
				Formula wideScoped = ss.getWidest();

				if (wideScoped.equals(f1Instance)) {
					if (!(f2Instance.equals(narrowScoped)))
						toBeChecked.add(new ScopeStatement(f2Instance, narrowScoped));
				}

			}
		}

		for (ScopeStatement ss : toBeChecked) {
			if (!ss.check(n))
				return false;
		}

		return true;
	}

	public boolean checkWithTupleAsContext(Tree t, ParserTuple context) {
		return check(t.getPointedNode());
	}

	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o == this)
			return true;

		if (!(o instanceof ScopeDepSaturationLabel))
			return false;

		ScopeDepSaturationLabel other = (ScopeDepSaturationLabel) o;

		ArrayList<Meta<?>> uninstantiatedBeforeCheckSelf = getUninstantiatedMetas();
		ArrayList<Meta<?>> uninstantiatedBeforeCheckOther = other.getUninstantiatedMetas();

		if (f1.equals(other.f1) && f2.equals(other.f2))
			return true;
		else {
			partialResetMetas(uninstantiatedBeforeCheckSelf);
			partialResetMetas(uninstantiatedBeforeCheckOther);

			return false;
		}
	}

	public String toString() {
		return FUNCTOR + "(" + f1 + "," + f2 + ")";
	}

	public String toUnicodeString() {
		return FUNCTOR + "(" + f1.toUnicodeString() + "," + f2.toUnicodeString() + ")";
	}

	public static void main(String a[]) {
		ScopeDepSaturationLabel l = new ScopeDepSaturationLabel("dep_saturated(Y,X) ");
	}
}
