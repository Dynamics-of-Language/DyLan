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
import qmul.ds.action.boundvariable.BoundNodeAddressVariable;
import qmul.ds.action.meta.MetaElement;
import qmul.ds.action.meta.MetaNodeAddress;
import qmul.ds.tree.Node;
import qmul.ds.tree.NodeAddress;

/**
 * A {@link NodeAddress} label e.g. Tn(0)
 * 
 * @author mpurver, arash
 */
public class AddressLabel extends Label implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public final static String FUNCTOR = "Tn";

	private NodeAddress address;

	public AddressLabel(NodeAddress address, IfThenElse ite) {
		super(ite);
		this.address = address;
	}

	public AddressLabel(NodeAddress address) {

		this.address = address;
	}

	public AddressLabel(String string, IfThenElse ite) {
		super(ite);
		if (string.matches("^" + LabelFactory.METAVARIABLE_PATTERN + "$")) {
			this.address = MetaNodeAddress.get(string);
		} else if (string.matches("^" + LabelFactory.VAR_PATTERN + "$")) {
			this.address = new BoundNodeAddressVariable(string);
		} else
			this.address = new NodeAddress(string);

	}

	public AddressLabel(String string) {
		this(string, null);

	}

	/**
	 * @return the address
	 */
	public NodeAddress getAddress() {
		return address;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#check(qmul.ds.tree.Node)
	 */
	@Override
	public boolean check(Node node) {
		logger.debug("cur node address is:" + node.getAddress());
		logger.debug("Label address is:" + address);
		return address.equals(node.getAddress());
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
		result = prime * result + ((address == null) ? 0 : address.hashCode());
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
		AddressLabel other = (AddressLabel) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
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
		return FUNCTOR + "(" + address + ")";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#instantiate()
	 */
	public Label instantiate() {
		return new AddressLabel(address.instantiate(), this.embeddingITE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#getMetas()
	 */
	@Override
	public ArrayList<MetaElement<?>> getMetas() {
		ArrayList<MetaElement<?>> metas = super.getMetas();
		if (address instanceof MetaNodeAddress) {
			metas.addAll(((MetaNodeAddress) address).getMetas());
		}
		return metas;
	}

	@Override
	public ArrayList<MetaElement<?>> getBoundMetas() {
		ArrayList<MetaElement<?>> metas = new ArrayList<MetaElement<?>>();
		if (address instanceof BoundNodeAddressVariable) {
			metas.addAll(((BoundNodeAddressVariable) (address)).getBoundMetas());
		}
		return metas;
	}

}
