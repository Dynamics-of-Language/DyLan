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

import org.apache.log4j.Logger;

import qmul.ds.tree.Modality;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;

/**
 * A {@link Modality} metavariable as used in rule specs e.g. <X>, <Y>
 * 
 * @author mpurver
 */
public class MetaModality extends Modality {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(MetaModality.class);

	private MetaElement<Modality> meta;

	/**
	 * @param meta
	 */
	protected MetaModality(MetaElement<Modality> meta) {
		this.meta = meta;
	}

	/**
	 * @return the value
	 */
	public Modality getValue() {
		return meta.getValue();
	}

	/**
	 * @return the meta-element
	 */
	public MetaElement<Modality> getMeta() {
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
	public static MetaModality get(String name) {
		MetaElement<Modality> m = MetaElement.get(name, Modality.class);
		return new MetaModality(m);
		/*
		 * if (!pool.containsKey(name)) { pool.put(name, new MetaModality(MetaElement.get(name, Modality.class))); }
		 * return pool.get(name);
		 */
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.Modality#instantiate()
	 */
	@Override
	public Modality instantiate() {
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
		if (!(obj instanceof Modality))
			return false;
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)
		if (obj instanceof MetaModality) {
			return meta.equals(((MetaModality) obj).meta.getValue());
		} else {
			return meta.equals(obj);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.Modality#relates(qmul.ds.tree.NodeAddress, qmul.ds.tree.NodeAddress)
	 */
	@Override
	public boolean relates(Tree t, NodeAddress from, NodeAddress to) {
		if (getValue() == null) {
			// instantiate to a suitable value, using equals() to ensure
			// backtracking works
			// Rule application must ensure this doesn't stick if not wanted
			Modality m = new Modality(false, from.pathTo(to));

			if (!meta.equals(m)) {
				return false;
			}
		}
		return from.to(t, to, getValue());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return Modality.EXIST_LEFT + meta.toString() + Modality.EXIST_RIGHT;
	}

}
