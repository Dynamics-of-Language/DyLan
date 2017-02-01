/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action.meta;

import java.util.ArrayList;

import qmul.ds.type.DSType;

/**
 * A {@link DSType} metavariable as used in rule specs e.g. X, Y
 * 
 * @author mpurver
 */
public class MetaType extends DSType {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MetaElement<DSType> meta;

	/**
	 * @param meta
	 */
	protected MetaType(MetaElement<DSType> meta) {
		this.meta = meta;
	}

	/**
	 * @return the value
	 */
	public DSType getValue() {
		return meta.getValue();
	}

	/**
	 * @return the meta-element
	 */
	public MetaElement<DSType> getMeta() {
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
	public static MetaType get(String name) {
		return new MetaType(MetaElement.get(name, DSType.class));
		/*
		 * if (!pool.containsKey(name)) { pool.put(name, new MetaType(MetaElement.get(name, Type.class))); } return
		 * pool.get(name);
		 */
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.type.Type#instantiate()
	 */
	@Override
	public DSType instantiate() {

		if (getValue() == null) {
			return this;
		}

		return getValue().instantiate();
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
		if (!(obj instanceof DSType))
			return false;
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)
		if (obj instanceof MetaType) {
			return meta.equals(((MetaType) obj).meta.getValue());
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

}
