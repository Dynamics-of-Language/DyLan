/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.formula;

import java.util.ArrayList;
import java.util.HashMap;

import qmul.ds.action.meta.MetaFormula;

/**
 * A (possibly bound) variable within a {@link Formula}
 * 
 * @author mpurver
 */
public class Variable extends AtomicFormula {

	private static final long serialVersionUID = 1L;

	private static final String ENTITY_VARIABLE_ROOT = "x";
	private static final String EVENT_VARIABLE_ROOT = "e";
	private static final String PROPOSITION_VARIABLE_ROOT = "p";

	private static ArrayList<Variable> entityPool = new ArrayList<Variable>();
	private static ArrayList<Variable> eventPool = new ArrayList<Variable>();
	private static ArrayList<Variable> propositionPool = new ArrayList<Variable>();

	/**
	 * A fresh entity variable x1, x2 etc
	 */
	public static Variable getFreshEntityVariable() {
		Variable v = new Variable(Variable.ENTITY_VARIABLE_ROOT + (entityPool.size() + 1));
		entityPool.add(v);
		return v;
	}

	/**
	 * A fresh event variable e1, e2 etc
	 */
	public static Variable getFreshEventVariable() {
		Variable v = new Variable(Variable.EVENT_VARIABLE_ROOT + (eventPool.size() + 1));
		eventPool.add(v);
		return v;
	}

	/**
	 * A fresh proposition variable p1, p2 etc
	 */
	public static Variable getFreshPropositionVariable() {
		Variable v = new Variable(Variable.PROPOSITION_VARIABLE_ROOT + (propositionPool.size() + 1));
		propositionPool.add(v);
		return v;
	}

	/**
	 * @param formula
	 */
	public Variable(String name) {
		super(name);
		getVariables().add(this);
	}

	public Variable(Variable l) {
		this(l.name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#subsumes(qmul.ds.formula.Formula, java.util.HashMap)
	 */
	@Override
	public boolean subsumesMapped(Formula other, HashMap<Variable, Variable> map) {
		if (other instanceof Variable) {
			Variable ov = (Variable) other;
			if (map.containsKey(this)) {
				return map.get(this).subsumesBasic(ov);
			} else {
				map.put(this, ov);
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.mp.AtomicFormula#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (this == o) {
			return true;
		}
		if (o instanceof Variable) {
			Variable other = (Variable) o;
			return name.equals(other.name);
		} else if (o instanceof MetaFormula) {
			MetaFormula other = (MetaFormula) o;
			return other.equals(this);
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.AtomicFormula#clone()
	 */
	public Variable clone() {
		return new Variable(this);
	}

	public Formula evaluate() {
		// this is so that variable USES within types act like paths.....
		// this means. e.g., that head will resolve to what it points to.... so [e3:es|head==e3:es|p==run(head):t]
		// will evaluate to [e3:es|head==e3:es|p==run(e3):t]
		TTRRecordType parent = getParentRecType();
		if (parent != null) {
			Formula pointedType = parent.get(new TTRLabel(this));
			if (pointedType != null && pointedType instanceof Variable)
				return pointedType;
		}
		return this;
	}

	
	public boolean hasManifestContent()
	{
		return false;
	}
	
	@Override
	public int toUniqueInt() {
		
		TTRRecordType parent = getParentRecType();
		if (parent != null) {
			Formula pointedType = parent.get(new TTRLabel(this));
			if (pointedType != null)
				return pointedType.toUniqueInt();
			else
				return 0;
		}
		return super.toUniqueInt();

	}
	public static void main(String[] args) {
		TTRRecordType f1 = (TTRRecordType) Formula.create("[e1:es|head==e1:es|p==subj(e1, R.head):t]");
		TTRRecordType tense = (TTRRecordType) Formula.create("[head:es|p1==future(head):t]");
		System.out.println("tense before conjunction:" + tense.evaluate());
		TTRFormula conjoined = f1.conjoin(tense);

		System.out.println("the conjunction" + conjoined);
		System.out.println("tense after conjunction:" + tense.evaluate());

	}

}
