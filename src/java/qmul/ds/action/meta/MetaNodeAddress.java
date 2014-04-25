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

import qmul.ds.tree.NodeAddress;

/**
 * A {@link NodeAddress} metavariable as used in rule specs e.g. X, Y
 * 
 * @author arash, mpurver
 */
public class MetaNodeAddress extends NodeAddress {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MetaElement<NodeAddress> meta;

	/**
	 * @param meta
	 */
	protected MetaNodeAddress(MetaElement<NodeAddress> meta) {
		this.meta = meta;
	}

	/**
	 * @return the value
	 */
	public NodeAddress getValue() {
		return meta.getValue();
	}

	/**
	 * @return the meta-element
	 */
	public MetaElement<NodeAddress> getMeta() {
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
	public static MetaNodeAddress get(String name) {
		return new MetaNodeAddress(MetaElement.get(name, NodeAddress.class));
		/*
		 * if (!pool.containsKey(name)) { pool.put(name, new MetaNodeAddress(MetaElement.get(name, NodeAddress.class)));
		 * } return pool.get(name);
		 */
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.NodeAddress#instantiate()
	 */
	public NodeAddress instantiate() {
		if (getValue() == null) {
			return this;
		} else {
			return getValue().instantiate();
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
		if (!(obj instanceof NodeAddress))
			return false;
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)
		if (obj instanceof MetaNodeAddress) {
			return meta.equals(((MetaNodeAddress) obj).meta.getValue());
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
