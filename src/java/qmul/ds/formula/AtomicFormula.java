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

import qmul.ds.action.meta.MetaModality;

/**
 * An atomic (no pred-arg structure) formula, e.g. a functor or variable. Just a string really
 * 
 * @author mpurver
 */
public class AtomicFormula extends Formula {

	private static final long serialVersionUID = 1L;

	protected String name;

	/**
	 * @param name
	 */
	public AtomicFormula(String name) {
		this.name = name;
	}
	
	/**
	 * Just for use by {@link MetaTTRLabel}
	 */
	protected AtomicFormula() {
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#substitute(qmul.ds.formula.Formula, qmul.ds.formula.Formula)
	 */
	@Override
	public Formula substitute(Formula f1, Formula f2) {
		if (this.equals(f1)) {
			return f2;
		} else {
			return this;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#subsumesMapped(qmul.ds.formula.Formula, java.util.HashMap)
	 */
	public boolean subsumesMapped(Formula f, HashMap<Variable, Variable> map) {
		return subsumesBasic(f);
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AtomicFormula other = (AtomicFormula) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#clone()
	 */
	public AtomicFormula clone() {
		return new AtomicFormula(this.name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return name;
	}

	@Override
	public int toUniqueInt() {
		
		return hashCode();
	}
}
