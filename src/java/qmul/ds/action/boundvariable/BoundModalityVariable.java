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

import qmul.ds.action.meta.MetaElement;
import qmul.ds.action.meta.MetaModality;
import qmul.ds.tree.Modality;

/**
 * Represents a bound variable of type {@link Modality}, as used in existential labels, e.g. Ex.<x>fo(mary)
 * 
 * @author Arash
 * 
 */
public class BoundModalityVariable extends MetaModality {

	private static final long serialVersionUID = 1L;

	String name;

	public BoundModalityVariable(String name) {
		super(MetaElement.getBoundMeta(Modality.class));
		this.name = name;
	}

	public String toString() {
		return name;
	}

	public BoundModalityVariable instantiate() {
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
		if (!(obj instanceof Modality))
			return false;
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)

		if (obj instanceof BoundModalityVariable) {
			if (getMeta().getValue() == null && ((BoundModalityVariable) obj).getMeta().getValue() == null)
				return true;

			return super.getMeta().equals(((BoundModalityVariable) obj).getMeta().getValue());
		} else {
			return getMeta().equals(obj);
		}
	}
}
