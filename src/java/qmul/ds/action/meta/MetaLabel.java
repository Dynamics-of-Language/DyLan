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

import qmul.ds.ParserTuple;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;

/**
 * A {@link Label} metavariable as used in rule specs e.g. X, ?Y
 * 
 * @author mpurver
 */
public class MetaLabel extends Label {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MetaElement<Label> meta;

	/**
	 * @param meta
	 */
	protected MetaLabel(MetaElement<Label> meta) {
		this.meta = meta;
	}

	/**
	 * @return the value
	 */
	public Label getValue() {
		return meta.getValue();
	}

	/**
	 * @return the meta-element
	 */
	public MetaElement<Label> getMeta() {
		return meta;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#getMetas()
	 */
	@Override
	public ArrayList<MetaElement<?>> getMetas() {
		ArrayList<MetaElement<?>> metas = new ArrayList<MetaElement<?>>();
		metas.add(meta);
		return metas;
	}

	/**
	 * @param name
	 * @return the existing metavariable of this name (with its value), a new one otherwise
	 */
	public static MetaLabel get(String name) {

		return new MetaLabel(MetaElement.get(name, Label.class));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#instantiate()
	 */
	@Override
	public Label instantiate() {
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
		if (!(obj instanceof Label))
			return false;
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)
		if (obj instanceof MetaLabel) {
			return meta.equals(((MetaLabel) obj).meta.getValue());
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#check(qmul.ds.tree.Node)
	 */
	@Override
	public boolean check(Node node) {
		if (getValue() == null) {

			return super.check(node);
		} else {
			logger.debug("MetaLabel value not null. Now checking label value:" + getValue());
			return getValue().check(node);
		}
	}

	public boolean check(Tree t, ParserTuple context) {
		if (getValue() == null) {

			return super.check(t, context);
		} else {
			logger.debug("MetaLabel value not null. Now checking label value:" + getValue());
			return getValue().check(t, context);
		}

	}

}
