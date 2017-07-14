/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/

package qmul.ds.action.meta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import qmul.ds.formula.Formula;
import qmul.ds.formula.Predicate;
import qmul.ds.formula.Variable;

/**
 * A {@link Predicate} metavariable as used e.g. PredicateArgumentFormulae, e.g. P(x,y).
 * 
 * @author arash
 */
public class MetaPredicate extends Predicate implements Serializable {

	private static final long serialVersionUID = 1L;

	private MetaElement<Predicate> meta;

	/**
	 * @param meta
	 */
	protected MetaPredicate(MetaElement<Predicate> meta) {
		this.meta = meta;
	}

	/**
	 * @return the value
	 */
	public Predicate getValue() {
		return meta.getValue();
	}

	/**
	 * @return the meta-element
	 */
	public MetaElement<Predicate> getMeta() {
		return meta;
	}

	public static void resetPool()
	{
		MetaElement.resetPool();
	}
	
	/**
	 * @return the meta-elements
	 */
	public ArrayList<Meta<?>> getMetas() {
		ArrayList<Meta<?>> metas = new ArrayList<Meta<?>>();
		metas.add(meta);
		return metas;
	}

	/**
	 * @param name
	 * @return the existing metavariable of this name (with its value), a new one otherwise
	 */
	public static MetaPredicate get(String name) {
		return new MetaPredicate(MetaElement.get(name, Predicate.class));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#instantiate()
	 */
	@Override
	public Predicate instantiate() {
		if (getValue() == null) {
			return this;
		}
		return getValue().instantiate();
	}

	

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#substitute(qmul.ds.formula.Formula, qmul.ds.formula.Formula)
	 */
	@Override
	public Formula substitute(Formula f1, Formula f2) {
		if (this.getValue()==null)
			return this;
		
		if (this.getValue().equals(f1)) {
			return f2;
		} else {
			return this;
		}
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
		if (!(obj instanceof Formula))
			return false;
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)

		if (obj instanceof MetaPredicate) {
			return meta.equals(((MetaPredicate) obj).meta.getValue());
		} else {
			return meta.equals(obj);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return meta.toString();
	}

	public MetaPredicate clone() {
		return this;
	}
	
	@Override
	public int toUniqueInt() {
		throw new UnsupportedOperationException("Shouldn't need to turn MetaFormulae into ints.... ");
		
	}
	
	public boolean subsumesBasic(Formula other)
	{
		if (this == other)
			return true;
		if (other == null)
			return false;
		
		if (other instanceof MetaPredicate) {
			MetaPredicate oth = (MetaPredicate) other;
			if (oth.getValue() == null && getValue() == null)
				return true;
		}
		
		// SIDE-EFFECT: checking subsumption sets metavariable value! 
		if (other instanceof MetaPredicate) {
			if (meta.getValue()==null)
				return meta.equals(((MetaPredicate)other).meta.getValue());
			
			return meta.getValue().subsumesBasic(((MetaPredicate) other).meta.getValue());
		} else {
			if (meta.getValue()==null)
				return meta.equals(other);
			
			return meta.getValue().subsumesBasic(other);
		}
		
		
	}
	
	public boolean subsumesMapped(Formula other, HashMap<Variable, Variable> map)
	{
		if (this == other)
			return true;
		if (other == null)
			return false;
		
		if (other instanceof MetaPredicate) {
			MetaPredicate oth = (MetaPredicate) other;
			if (oth.getValue() == null && getValue() == null)
				return true;
		}
		
		// SIDE-EFFECT: checking subsumption sets metavariable value! 
		if (other instanceof MetaPredicate) {
			if (meta.getValue()==null)
				return meta.equals(((MetaPredicate)other).meta.getValue());
			
			return meta.getValue().subsumesMapped(((MetaPredicate) other).meta.getValue(),map);
		} else {
			if (meta.getValue()==null)
				return meta.equals(other);
			
			return meta.getValue().subsumesMapped(other,map);
		}
		
		
	}

}
