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

import qmul.ds.action.ActionSequence;
import qmul.ds.action.meta.MetaActionSequence;
import qmul.ds.action.meta.MetaElement;

/**
 * Represents a bound variable of type {@link ActionSequence}, as used in existential labels, e.g. Ex.triggered_by(x,
 * ty(e))
 * 
 * @author Arash
 * 
 */
@SuppressWarnings("serial")
public class BoundActionSequenceVariable extends MetaActionSequence {

	String name;

	public BoundActionSequenceVariable(String name) {
		super(MetaElement.getBoundMeta(ActionSequence.class));
		this.name = name;

	}

	/*
	 * public boolean equals(Object o) { if (o == null) return false; if (o instanceof ActionSequenceVariable) { return
	 * ((ActionSequenceVariable) o).name.equals(name); } else return false; }
	 */

	public String toString() {
		return name;
	}

	public BoundActionSequenceVariable instantiate() {
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
		if (!(obj instanceof ActionSequence))
			return false;
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)

		if (obj instanceof BoundActionSequenceVariable) {
			if (getMeta().getValue() == null && ((BoundActionSequenceVariable) obj).getMeta().getValue() == null)
				return true;

			return super.getMeta().equals(((BoundFormulaVariable) obj).getMeta().getValue());
		} else {
			return getMeta().equals(obj);
		}
	}
}
