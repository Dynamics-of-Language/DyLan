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

import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.boundvariable.BoundFormulaVariable;
import qmul.ds.action.meta.MetaElement;
import qmul.ds.action.meta.MetaFormula;
import qmul.ds.formula.Formula;
import qmul.ds.formula.FormulaMetavariable;

/**
 * Represents scope statement among any number of variables, e.g. Scope(x<y<z<u). Note the sequence can involve
 * Meta-Formulae. . . e.g. Scope(X<y<Z).
 * 
 * 
 * @author arash
 */

public class ScopeStatement extends Label {
	public static final String FUNCTOR = "Scope";
	private static final Pattern VAR_SEQUENCE_PATTERN = Pattern.compile("(\\s*(" + Formula.VARIABLE_PATTERN.pattern()
			+ "|" + LabelFactory.METAVARIABLE_PATTERN + ")\\s*)(<\\s*(" + Formula.VARIABLE_PATTERN.pattern() + "|"
			+ LabelFactory.METAVARIABLE_PATTERN + ")\\s*)+");
	private static final Pattern SCOPE_STATEMENT_PATTERN = Pattern.compile(FUNCTOR + "\\s*\\(("
			+ VAR_SEQUENCE_PATTERN.pattern() + ")\\)");

	protected ArrayList<Formula> sequence = new ArrayList<Formula>();

	public ScopeStatement(ArrayList<Formula> vars) {
		super(null);
		sequence = vars;
	}

	public ScopeStatement(String statement, IfThenElse ite) {
		super(ite);
		Matcher m1 = SCOPE_STATEMENT_PATTERN.matcher(statement);
		if (m1.matches()) {
			String varSeq = m1.group(1);

			Matcher m2 = VAR_SEQUENCE_PATTERN.matcher(varSeq);

			while (m2.matches()) {

				sequence.add(Formula.create(m2.group(1).trim()));

				// create(m2.group(1).trim()));

				varSeq = varSeq.substring(m2.group(1).length() + 1, varSeq.length());

				m2 = VAR_SEQUENCE_PATTERN.matcher(varSeq);

			}
			sequence.add(Formula.create(varSeq.trim()));
		} else {
			throw new IllegalArgumentException("unrecognised scope statement " + statement);
		}
	}

	public ScopeStatement(String string) {
		this(string, null);
	}

	public ScopeStatement(Formula f1, Formula f2) {
		super(null);
		ArrayList<Formula> seq = new ArrayList<Formula>();
		seq.add(f1);
		seq.add(f2);
		this.sequence = seq;

	}

	/**
	 * 
	 * @return the (and {@link ArrayList}) sequence of variables in this scope statement
	 */
	public ArrayList<Formula> getSequence() {
		return sequence;
	}

	public String toString() {
		String res = FUNCTOR + "(";
		for (Formula v : sequence) {
			res += v + "<";
		}
		return res.substring(0, res.length() - 1) + ")";
	}

	public String toUnicodeString() {
		String res = FUNCTOR + "(";
		for (Formula v : sequence) {
			res += v.toUnicodeString() + "<";
		}
		return res.substring(0, res.length() - 1) + ")";
	}

	/**
	 * 
	 * @param variable
	 * @return true if this scope statement involves the variable passed as argument
	 */
	public boolean involves(Formula var) {

		for (Formula f : sequence) {
			if (f.equals(var))
				return true;
		}
		return false;
	}

	public ArrayList<MetaElement<?>> getMetas() {
		ArrayList<MetaElement<?>> result = super.getMetas();
		for (Formula f : sequence) {

			if (f instanceof MetaFormula) {

				result.addAll(((MetaFormula) f).getMetas());
			} else if (f instanceof FormulaMetavariable) {
			}

		}

		return result;

	}

	public ArrayList<MetaElement<?>> getBoundMetas() {
		ArrayList<MetaElement<?>> result = super.getMetas();
		for (Formula f : sequence) {

			if (f instanceof BoundFormulaVariable) {

				result.addAll(((BoundFormulaVariable) f).getBoundMetas());
			} else if (f instanceof FormulaMetavariable) {
			}

		}

		return result;

	}

	/**
	 * WARNING: equality sets meta-variable values if possible. This will only happen if equality succeeds fully.
	 * Otherwise all previously uninstantiated metavariables will remain uninstantiated.
	 * 
	 * @return true if this is equal to argument
	 * 
	 */
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o == this)
			return true;
		if (!(o instanceof ScopeStatement))
			return false;
		ScopeStatement other = (ScopeStatement) o;
		if (sequence.size() != other.getSequence().size())
			return false;

		ArrayList<MetaElement<?>> uninstantiatedBeforeCheckSelf = getUninstantiatedMetas();
		ArrayList<MetaElement<?>> uninstantiatedBeforeCheckOther = other.getUninstantiatedMetas();

		for (int i = 0; i < sequence.size(); i++) {
			Formula f = sequence.get(i);
			Formula otherF = other.sequence.get(i);
			// System.out.println(f.getClass()+" "+otherF.getClass());
			if (!f.equals(otherF)) {
				// System.out.println(f + "of type" + f.getClass() +
				// " does not equal " + otherF + " of type"
				// + otherF.getClass());
				partialResetMetas(uninstantiatedBeforeCheckSelf);
				partialResetMetas(uninstantiatedBeforeCheckOther);
				return false;
			}
		}
		return true;

	}

	public static void main(String a[]) {

		ScopeStatement s0 = new ScopeStatement("Scope(m<u)");
		ScopeStatement s = new ScopeStatement("Scope(Y<X)");
		ScopeStatement s1 = new ScopeStatement("Scope(Z<W)");
		System.out.println("before equals:");
		System.out.println(s);
		System.out.println(s0);
		System.out.println("result of equals:" + s.equals(s0));
		s1.equals(s0);
		System.out.println(s1.equals(s));
		/*
		 * System.out.println("After equals:"); System.out.println(s); System.out.println(s0);
		 */

	}

	public ScopeStatement instantiate() {
		ArrayList<Formula> instance = new ArrayList<Formula>();
		for (Formula f : sequence) {
			instance.add(f.instantiate());
		}
		return new ScopeStatement(instance);

	}

	public Formula getNarrowest() {

		return this.sequence.get(this.sequence.size() - 1);
	}

	public Formula getWidest() {
		return this.sequence.get(0);
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
		for (Formula var : this.sequence)
			result = prime * result + ((var == null) ? 0 : var.hashCode());

		return result;
	}
}
