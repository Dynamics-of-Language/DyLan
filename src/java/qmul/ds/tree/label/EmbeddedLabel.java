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
import qmul.ds.action.meta.MetaElement;

/**
 * A label which embeds another label, e.g. a negated label �L
 * 
 * @author mpurver
 */
public abstract class EmbeddedLabel extends Label implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected Label label;

	protected EmbeddedLabel(IfThenElse ite) {
		super(ite);
	}

	protected EmbeddedLabel() {

	}

	/**
	 * @param label
	 */
	protected EmbeddedLabel(Label label, IfThenElse ite) {
		super(ite);
		this.label = label;
	}

	protected EmbeddedLabel(Label label) {
		this(label, null);
	}

	/**
	 * @return the label
	 */
	public Label getLabel() {
		return label;
	}

	/**
	 * @param label
	 *            the label to set
	 */
	protected void setLabel(Label label) {
		this.label = label;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#getMetas()
	 */
	@Override
	public ArrayList<MetaElement<?>> getMetas() {
		ArrayList<MetaElement<?>> metas = super.getMetas();
		metas.addAll(label.getMetas());
		return metas;
	}

	@Override
	public ArrayList<MetaElement<?>> getBoundMetas() {
		ArrayList<MetaElement<?>> metas = super.getMetas();
		metas.addAll(label.getBoundMetas());
		return metas;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#instantiate()
	 */
	@Override
	public abstract Label instantiate();

}
