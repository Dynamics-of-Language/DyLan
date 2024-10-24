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
import qmul.ds.formula.MetaPredicate;
import qmul.ds.formula.Predicate;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.formula.Variable;
import qmul.ds.tree.label.Label;

/**
 * A {@link Formula} metavariable as used in rule specs e.g. X, Y
 * 
 * @author mpurver
 */
public class MetaFormula extends Formula implements Serializable {

	private static final long serialVersionUID = 1L;

	private MetaElement<Formula> meta;

	/**
	 * @param meta
	 */
	protected MetaFormula(MetaElement<Formula> meta) {
		this.meta = meta;
	}

	/**
	 * @return the value
	 */
	public Formula getValue() {
		return meta.getValue();
	}

	/**
	 * @return the meta-element
	 */
	public MetaElement<Formula> getMeta() {
		return meta;
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
	public static MetaFormula get(String name) {
		return new MetaFormula(MetaElement.get(name, Formula.class));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#instantiate()
	 */
	@Override
	public Formula instantiate() {
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
//		System.out.println("Subst in metaformula:"+this);
		if (this.getValue()==null && !(f1 instanceof MetaFormula))
			return this;
		else if (this.getValue()==null)
		{
			MetaFormula metaF1=(MetaFormula)f1;
			if (metaF1.meta.getName().equals(meta.getName()))
				return f2;
			else
				return this;
			
		}
		else
		{
			//if we are here, value of this meta is non-null
			if (this.getValue().equals(f1) && f2 instanceof MetaFormula) {
				return f2;
			} else if (this.getValue().equals(f1)){
				
				meta.setValue(f2);
				return this;
			}
			else 
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

		if (obj instanceof MetaFormula) {
			return meta.equals(((MetaFormula) obj).meta.getValue());
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

	public MetaFormula clone() {
		return this;
	}
	
	@Override
	public int toUniqueInt() {
		throw new UnsupportedOperationException("Shouldn't need to turn MetaFormulae into ints.... ");
		
	}
	
	public boolean subsumesBasic(Formula f)
	{
		if (f==null)
			return false;
		if (meta.getValue()==null)
			return false;
		
		return meta.getValue().subsumesBasic(f);
		
		
	}
	
	public boolean subsumesMapped(Formula f)
	{
		if (f==null)
			return false;
		if (meta.getValue()==null)
			return false;
		
		return meta.getValue().subsumesMapped(f, new HashMap<Variable, Variable>());
		
	}
	
	
	public static void main(String[] a)
	{
		TTRRecordType rec=TTRRecordType.parse("[x==arash:e|y==U1:e|p==like(x,y):t]");
		TTRRecordType rec1=TTRRecordType.parse("[x==arash:e|y==yanchao:e|p==like(x,y):t]");
		System.out.println(rec.subsumes(rec1));
		System.out.println(rec);
		
		Predicate p2=(MetaPredicate)Formula.create("P2");
		TTRRecordType subst=rec.substitute(new Predicate("like"), p2);
		
		System.out.println(subst);
		
		
		
		
	}
	
	

}
