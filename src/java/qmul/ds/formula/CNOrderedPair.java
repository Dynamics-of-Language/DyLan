/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/

package qmul.ds.formula;

import java.util.regex.Matcher;

/**
 * A {@link PredicateArgumentFormula} with a null functor, and two arguments: a {@link Variable} and a {@link Formula}
 * (presumably involving that variable)
 * 
 * @author arash, mpurver
 */
public class CNOrderedPair extends PredicateArgumentFormula {

	private static final long serialVersionUID = 1L;

	private static final String EMPTY_FUNCTOR = "";

	public CNOrderedPair(Formula v, Formula f) {
		super(EMPTY_FUNCTOR, v, f);
		if (v instanceof Variable && !f.getVariables().contains(v)) {
			logger.fatal("expecting variable " + v + " already bound in " + f + " " + f.getVariables());
		}
	}

	public CNOrderedPair(CNOrderedPair cnOrderedPair) {
		super(cnOrderedPair);
	}

	public static CNOrderedPair parse(String s1) {
		String s = s1.trim();
		String[] parts = s.split(",");
		if (parts.length != 2)
			return null;
		if (parts[0].isEmpty() || parts[1].isEmpty())
			return null;

		Matcher m = Formula.VARIABLE_PATTERN.matcher(parts[0]);
		if (m.matches()) {
			return new CNOrderedPair(new Variable(parts[0]), Formula.create(parts[1]));
		} else {
			TTRPath path = TTRPath.parse(parts[0]);
			if (path != null)
				return new CNOrderedPair(path, Formula.create(parts[1]));
		}

		return null;

	}

	/**
	 * @return the variable (first) argument
	 */
	public Variable getVariable() {

		if (getArguments().get(0) instanceof TTRPath) {
			TTRPath path = (TTRPath) getArguments().get(0);
			return path.getFinalLabel();
		}
		return (Variable) getArguments().get(0);
	}

	/**
	 * @return the formula (second) argument
	 */
	public Formula getFormula() {
		return getArguments().get(1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.mp.Formula#conjoin(qmul.ds.formula.mp.Formula)
	 */
	@Override
	public CNOrderedPair conjoin(Formula f) {
		if (getArguments().get(0) instanceof TTRPath)
			throw new UnsupportedOperationException();

		if (f instanceof CNOrderedPair) {
			CNOrderedPair cnop = (CNOrderedPair) f;
			if (cnop.getArguments().get(0) instanceof TTRPath)
				throw new UnsupportedOperationException();
			Formula fo = cnop.getFormula().substitute(cnop.getVariable(), getVariable());
			return new CNOrderedPair(getVariable(), getFormula().conjoin(fo));
		} else {
			return new CNOrderedPair(getVariable(), getFormula().conjoin(f));
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.mp.Formula#substitute(qmul.ds.formula.mp.Formula, qmul.ds.formula.mp.Formula)
	 */
	@Override
	public CNOrderedPair substitute(Formula f1, Formula f2) {
		Formula[] args = new Formula[getArity()];
		for (int i = 0; i < getArity(); i++) {
			args[i] = arguments.get(i).substitute(f1, f2);
		}
		return new CNOrderedPair(args[0], args[1]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.PredicateArgumentFormula#evaluate()
	 */
	public CNOrderedPair evaluate() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getArguments().get(0) + ", " + getFormula(); // override default to prevent unnecessary parentheses
	}

	public CNOrderedPair clone() {
		return new CNOrderedPair(this);
	}

}
