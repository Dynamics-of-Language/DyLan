/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.formula;

import java.util.HashMap;

import qmul.ds.action.meta.MetaFormula;

/**
 * A formula metavariable as used for e.g. anaphora. Keeping this distinct from {@link MetaFormula} (a rule metavariable
 * which ranges over formulae) as they are formally distinct - but the implementation is identical.
 * 
 * @author mpurver
 */
public class FormulaMetavariable extends Formula {

	private static final long serialVersionUID = 1L;

	private String name;
	private Formula value;

	/**
	 * @param name
	 *            a String name e.g. "X", "Y"
	 */
	private FormulaMetavariable(String name) {
		this.name = name;
		this.value = null;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the value
	 */
	public Formula getValue() {
		return value;
	}

	/**
	 * Unset value
	 */
	public void reset() {
		value = null;
	}

	private static HashMap<String, FormulaMetavariable> pool = new HashMap<String, FormulaMetavariable>();

	/**
	 * @param name
	 * @return the existing metavariable of this name (with its value), a new one otherwise
	 */
	public static FormulaMetavariable get(String name) {
		if (!pool.containsKey(name)) {
			pool.put(name, new FormulaMetavariable(name));
		}
		return pool.get(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#instantiate()
	 */
	@Override
	public Formula instantiate() {
		if (value == null) {
			return this;
		}
		return value.instantiate();
	}

	public static void resetPool() {
		pool.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#substitute(qmul.ds.formula.Formula, qmul.ds.formula.Formula)
	 */
	@Override
	public FormulaMetavariable substitute(Formula f1, Formula f2) {
		if (this.equals(f1)) {
			return (FormulaMetavariable) f2;
		} else {
			return this;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#subsumesBasic(qmul.ds.formula.Formula)
	 */
	@Override
	protected boolean subsumesBasic(Formula other) {
		if (value == null) {
			return true;
		}
		if (other instanceof FormulaMetavariable) {
			FormulaMetavariable oth = (FormulaMetavariable) other;
			if (oth.value == null)
				return false;
			else
				return value.subsumesBasic(oth.value);
		} else {
			return value.subsumesBasic(other);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#subsumesMapped(qmul.ds.formula.Formula, java.util.HashMap)
	 */
	@Override
	protected boolean subsumesMapped(Formula other, HashMap<Variable, Variable> map) {
		if (value == null) {
			return true;
		}
		if (other instanceof FormulaMetavariable) {
			FormulaMetavariable oth = (FormulaMetavariable) other;
			if (oth.value == null)
				return false;
			else
				return value.subsumesMapped(oth.value, map);
		} else {
			return value.subsumesMapped(other, map);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#hashCode()
	 */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	// @Override
	// public int hashCode() {
	// if (value == null) {
	// final int prime = 31;
	// int result = 1;
	// result = prime * result + ((name == null) ? 0 : name.hashCode());
	// return result;
	// } else {
	// return value.hashCode();
	// }
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Formula))
			return false;
		if (obj instanceof FormulaMetavariable) {
			FormulaMetavariable other = (FormulaMetavariable) obj;
			if (other.value == null && value == null)
				return true;
		}
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)
		Formula other = (obj instanceof FormulaMetavariable) ? ((FormulaMetavariable) obj).value : ((Formula) obj);
		if (value == null) {
			value = other;
		}
		return value.equals(other);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#clone()
	 */
	@Override
	public FormulaMetavariable clone() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (value == null) {
			return name;
		} else {
			return name + "=" + value;
		}
	}

	@Override
	public int toUniqueInt() {
		return this.value==null?name.hashCode():name.hashCode()+value.toUniqueInt();
		
	}

}
