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

import qmul.ds.action.ActionSequence;

/**
 * 
 * An {@link ActionSequence} metavariable as used in rule specs e.g. <<A>>
 * 
 * @author arash
 */
@SuppressWarnings("serial")
public class MetaActionSequence extends ActionSequence {

	private MetaElement<ActionSequence> meta;

	/**
	 * @param meta
	 */
	protected MetaActionSequence(MetaElement<ActionSequence> meta) {
		this.meta = meta;
	}

	/**
	 * @return the value
	 */
	public ActionSequence getValue() {
		return meta.getValue();
	}

	/**
	 * @return the meta-element
	 */
	public MetaElement<ActionSequence> getMeta() {
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
	public static MetaActionSequence get(String name) {
		return new MetaActionSequence(MetaElement.get(name, ActionSequence.class));
		/*
		 * if (!pool.containsKey(name)) { pool.put(name, new MetaActionSequence(MetaElement.get(name,
		 * ActionSequence.class))); }
		 * 
		 * return pool.get(name);
		 */
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.ActionSequence#instantiate()
	 */
	public ActionSequence instantiate() {
		if (getValue() == null) {
			return this;
		}
		return getValue().instantiate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.AbstractList#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ActionSequence))
			return false;
		// SIDE-EFFECT: checking equality sets action sequence! (no hashCode)
		if (obj instanceof MetaActionSequence) {
			return meta.equals(((MetaActionSequence) obj).meta.getValue());
		} else {
			return meta.equals(obj);
		}
	}

	public String toString() {
		String result = "<<\n";

		result += meta;
		// if (isEmpty())
		// result += "null";
		result += "\n>>";
		return result;
	}

}
