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

import qmul.ds.tree.label.PredicateArgument;

public class MetaPredicateArgument extends PredicateArgument {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MetaElement<PredicateArgument> meta;

	/**
	 * @param meta
	 */

	protected MetaPredicateArgument(MetaElement<PredicateArgument> meta) {
		this.meta = meta;
	}

	/**
	 * @return the value
	 */
	public PredicateArgument getValue() {
		return meta.getValue();
	}

	/**
	 * @return the meta-element
	 */
	public MetaElement<PredicateArgument> getMeta() {
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
	public static MetaPredicateArgument get(String name) {
		return new MetaPredicateArgument(MetaElement.get(name, PredicateArgument.class));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.type.Type#instantiate()
	 */
	@Override
	public PredicateArgument instantiate() {
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
		if (!(obj instanceof PredicateArgument))
			return false;
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)
		if (obj instanceof MetaPredicateArgument) {
			return meta.equals(((MetaPredicateArgument) obj).meta.getValue());
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
