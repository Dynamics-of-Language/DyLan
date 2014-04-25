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

import qmul.ds.formula.Formula;
import qmul.ds.formula.TTRInfixExpression;
import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRRecordType;

/**
 * A {@link TTRRecordType} metavariable as used in rule specs e.g. R1, R2, etc.
 * 
 * @author mpurver
 */
public class MetaTTRRecordType extends TTRRecordType implements Serializable {

	private static final long serialVersionUID = 1L;

	private MetaElement<TTRRecordType> meta;

	/**
	 * @param meta
	 */
	protected MetaTTRRecordType(MetaElement<TTRRecordType> meta) {
		this.meta = meta;
	}

	/**
	 * @return the value
	 */
	public TTRRecordType getValue() {
		return meta.getValue();
	}

	/**
	 * @return the meta-element
	 */
	public MetaElement<TTRRecordType> getMeta() {
		return meta;
	}

	/**
	 * @return the meta-elements
	 */
	public ArrayList<MetaElement<?>> getMetas() {
		ArrayList<MetaElement<?>> metas = new ArrayList<MetaElement<?>>();
		metas.add(meta);
		return metas;
	}

	/**
	 * @param name
	 * @return the existing metavariable of this name (with its value), a new one otherwise
	 */
	public static MetaTTRRecordType get(String name) {
		return new MetaTTRRecordType(MetaElement.get(name, TTRRecordType.class));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#instantiate()
	 */
	@Override
	public TTRRecordType instantiate() {
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
	public TTRRecordType substitute(Formula f1, Formula f2) {
		if (this.equals(f1)) {
			return (TTRRecordType) f2;
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

		if (obj instanceof MetaTTRRecordType) {
			return meta.equals(((MetaTTRRecordType) obj).meta.getValue());
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

	public MetaTTRRecordType clone() {
		return this;
	}

	@Override
	public TTRRecordType evaluate() {
		if (getValue() != null)
			return getValue().evaluate();
		return this;
	}

	@Override
	public TTRFormula asymmetricMerge(TTRFormula rt) {
		if (getValue() != null)
			return getValue().asymmetricMerge(rt);

		return new TTRInfixExpression(TTRInfixExpression.ASYM_MERGE_FUNCTOR, this, rt);
	}

}
