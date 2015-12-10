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
import java.util.HashSet;

import qmul.ds.action.meta.MetaFormula;

/**
 * A formula metavariable as used for e.g. anaphora. Keeping this distinct from {@link MetaFormula} (a rule metavariable
 * which ranges over formulae) as they are formally distinct - but the implementation is identical.
 * 
 * @author mpurver
 */
public class MetaTTRLabel extends TTRLabel {

	private static final long serialVersionUID = 1L;

	private String name;
	private Variable value;
	
	public HashSet<String> backtrack;
	private Variable last;

	/**
	 * @param name
	 *            a String name e.g. "X", "Y"
	 */
	private MetaTTRLabel(String name) {
		this.name = name;
		this.value = null;
		this.backtrack = new HashSet<String>();
		this.last = null;
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
	 * Un-instantiate the instantiated value, remembering it to prevent instantiation to this value again
	 * 
	 * @return true if it can be backtracked (i.e. is instantiated)
	 */
	public boolean backtrack() {
		// can't backtrack if not instantiated
		if ((value == null) || backtrack.contains(value.toString())) {
			return false;
		}
		backtrack.add(value.toString());
		last = value;
		value = null;
		logger.trace("Backtracked from " + last + " to " + this);
		return true;
	}
	
	public boolean canBacktrack()
	{
		return (value != null) && !backtrack.contains(value.toString());
	}
	
	/**
	 * Uninstantiate completely
	 */
	public void reset() {
		backtrack.clear();
		last=null;
		value = null;
	}
	
	/**
	 * Un-instantiate value, but don't forget backtracking history
	 */
	public void partialReset() {
		value = null;
	}

	private static HashMap<String, MetaTTRLabel> pool = new HashMap<String, MetaTTRLabel>();

	/**
	 * @param name
	 * @return the existing metavariable of this name (with its value), a new one otherwise
	 */
	public static MetaTTRLabel get(String name) {
		if (!pool.containsKey(name)) {
			pool.put(name, new MetaTTRLabel(name));
		}
		return pool.get(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#instantiate()
	 */
	@Override
	public TTRLabel instantiate() {
		if (value == null) {
			return this;
		}
		return new TTRLabel(value.instantiate());
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
	public MetaTTRLabel substitute(Formula f1, Formula f2) {
		if (this.equals(f1)) {
			return (MetaTTRLabel) f2;
		} else {
			return this;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#subsumesBasic(qmul.ds.formula.Formula)
	 */
	/*
	@Override
	protected boolean subsumesBasic(Formula other) {
		if (value == null) {
			return ;
		}
		if (other instanceof MetaTTRLabel) {
			MetaTTRLabel oth = (MetaTTRLabel) other;
			if (oth.value == null)
				return false;
			else
				return value.subsumesBasic(oth.value);
		} else {
			return value.subsumesBasic(other);
		}
	}*/

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#subsumesMapped(qmul.ds.formula.Formula, java.util.HashMap)
	 */
	@Override
	public boolean subsumesMapped(Formula other, HashMap<Variable, Variable> map) {
		if (value == null) {
			return true;
		}
		if (other instanceof MetaTTRLabel) {
			MetaTTRLabel oth = (MetaTTRLabel) other;
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
		if (!(obj instanceof Variable))
			return false;
		if (obj instanceof MetaTTRLabel) {
			MetaTTRLabel other = (MetaTTRLabel) obj;
			if (other.value == null && value == null)
				return true;
		}
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)
		Variable other = (obj instanceof MetaTTRLabel) ? ((MetaTTRLabel) obj).value : ((Variable) obj);
		if (value == null) {
			if (backtrack.contains(other.toString())) {
				// logger.debug("Can't inst MetaEl, already used " + other);
				return false;
			}
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
	public MetaTTRLabel clone() {
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
