/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.tree.label;

import java.io.Serializable;
import java.util.ArrayList;

import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.boundvariable.BoundTypeVariable;
import qmul.ds.action.meta.MetaElement;
import qmul.ds.action.meta.MetaType;
import qmul.ds.type.DSType;

/**
 * A type label e.g. Ty(e)
 * 
 * @author mpurver
 */
public class TypeLabel extends Label implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public final static String FUNCTOR = "Ty";

	public final static TypeLabel e = new TypeLabel(DSType.e);
	public final static TypeLabel t = new TypeLabel(DSType.t);
	public final static TypeLabel cn = new TypeLabel(DSType.cn);
	public final static TypeLabel es = new TypeLabel(DSType.es);

	private DSType type;

	/**
	 * @param type
	 */
	public TypeLabel(DSType type, IfThenElse ite) {
		super(ite);
		this.type = type;
	}

	public TypeLabel(DSType t) {
		this(t, null);
	}

	/**
	 * @return the type
	 */
	public DSType getType() {
		return type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#getMetas()
	 */
	@Override
	public ArrayList<MetaElement<?>> getMetas() {
		ArrayList<MetaElement<?>> metas = super.getMetas();
		// TODO if ConstructedTypes can contain MetaTypes e.g. (e>(e>X)) then
		// we'll need Type to implement getMetas()
		if (type instanceof MetaType) {
			metas.addAll(((MetaType) type).getMetas());
		}
		return metas;
	}

	@Override
	public ArrayList<MetaElement<?>> getBoundMetas() {
		ArrayList<MetaElement<?>> metas = super.getBoundMetas();
		// TODO if ConstructedTypes can contain MetaTypes e.g. (e>(e>X)) then
		// we'll need Type to implement getMetas()
		if (type instanceof BoundTypeVariable) {
			metas.addAll(((BoundTypeVariable) type).getBoundMetas());
		}
		return metas;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#instantiate()
	 */
	@Override
	public Label instantiate() {
		return new TypeLabel(type.instantiate());
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
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		TypeLabel other = (TypeLabel) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR + "(" + type + ")";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#toUnicodeString()
	 */
	@Override
	public String toUnicodeString() {
		return FUNCTOR + "(" + type.toUnicodeString() + ")";
	}

}
