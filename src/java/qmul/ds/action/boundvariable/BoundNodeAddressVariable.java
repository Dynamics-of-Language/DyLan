/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action.boundvariable;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import qmul.ds.action.meta.MetaElement;
import qmul.ds.action.meta.MetaNodeAddress;
import qmul.ds.tree.NodeAddress;

/**
 * Represents a bound variable of type {@link NodeAddress}, as used in existential labels, e.g. Ex.tn(x)
 * 
 * @author Arash
 * 
 */
@SuppressWarnings("serial")
public class BoundNodeAddressVariable extends MetaNodeAddress {

	private static Logger logger = Logger.getLogger(BoundNodeAddressVariable.class);

	String name;

	public BoundNodeAddressVariable(String name) {
		super(MetaElement.getBoundMeta(NodeAddress.class));
		this.name = name;
	}

	public String toString() {
		return name;
	}

	public BoundNodeAddressVariable instantiate() {
		getMeta().reset();
		return this;
	}

	public String toUnicodeString() {
		return name;
	}

	public ArrayList<MetaElement<?>> getMetas() {
		return new ArrayList<MetaElement<?>>();
	}

	public ArrayList<MetaElement<?>> getBoundMetas() {
		return super.getMetas();

	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof NodeAddress))
			return false;
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)
		logger.trace("checking bound var equality");
		if (obj instanceof BoundNodeAddressVariable) {
			if (getMeta().getValue() == null && ((BoundNodeAddressVariable) obj).getMeta().getValue() == null)
				return true;
			logger.trace("calling equlas on Meta");
			return super.getMeta().equals(((BoundNodeAddressVariable) obj).getMeta().getValue());
		} else {
			return getMeta().equals(obj);
		}
	}
}
