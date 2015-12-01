/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.tree.label;

import java.util.ArrayList;

import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.boundvariable.BoundModalityVariable;
import qmul.ds.action.meta.MetaElement;
import qmul.ds.action.meta.MetaModality;
import qmul.ds.tree.Modality;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;

public class AddressSubsumptionLabel extends Label {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public final static String FUNCTOR = "Subsumes";

	private Modality modality;

	public AddressSubsumptionLabel(Modality modality, IfThenElse ite) {
		super(ite);
		this.modality = modality;
	}

	public AddressSubsumptionLabel(String string) {
		this(string, null);
	}

	public AddressSubsumptionLabel(String string, IfThenElse ite) {
		super(ite);
		this.modality = Modality.parse(string);
		logger.debug("created subsumption label:" + this);
	}

	public AddressSubsumptionLabel(Modality m) {
		this(m, null);
	}

	/**
	 * @return the modality
	 */
	public Modality getModality() {
		return modality;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#check(qmul.ds.tree.Node)
	 */
	public boolean checkWithTupleAsContext(Tree tree, ParserTuple context) {
		NodeAddress pointed = tree.getPointer();
		Modality instantiated = modality.instantiate();
		NodeAddress other;
		if (instantiated instanceof MetaModality && ((MetaModality) instantiated).getValue() == null) {
			return false;

		} else
			other = pointed.go(modality.instantiate());
		return pointed.subsumes(other) || other.subsumes(pointed);
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
		result = prime * result + ((modality == null) ? 0 : modality.hashCode());
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
		AddressSubsumptionLabel other = (AddressSubsumptionLabel) obj;
		if (modality == null) {
			if (other.modality != null)
				return false;
		} else if (!modality.equals(other.modality))
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
		return FUNCTOR + "(" + modality + ")";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#instantiate()
	 */
	public Label instantiate() {
		return new AddressSubsumptionLabel(modality.instantiate());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#getMetas()
	 */
	@Override
	public ArrayList<MetaElement<?>> getMetas() {
		ArrayList<MetaElement<?>> metas = super.getMetas();
		if (modality instanceof MetaModality) {
			metas.addAll(((MetaModality) modality).getMetas());
		}
		return metas;
	}

	@Override
	public ArrayList<MetaElement<?>> getBoundMetas() {
		ArrayList<MetaElement<?>> metas = super.getBoundMetas();
		if (modality instanceof BoundModalityVariable) {
			metas.addAll(((BoundModalityVariable) modality).getBoundMetas());
		}
		return metas;
	}

}
