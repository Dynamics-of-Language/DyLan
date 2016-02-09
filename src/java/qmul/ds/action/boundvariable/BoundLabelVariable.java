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

import qmul.ds.action.meta.Meta;
import qmul.ds.action.meta.MetaElement;
import qmul.ds.action.meta.MetaLabel;
import qmul.ds.tree.label.Label;

/**
 * Represents a bound variable of type {@link Label}, as used in existential labels, e.g. Ex.?x
 * 
 * @author Arash
 * 
 */
public class BoundLabelVariable extends MetaLabel {

	private static final long serialVersionUID = 1L;

	String name;

	public BoundLabelVariable(String name) {
		super(MetaElement.getBoundMeta(Label.class));
		this.name = name;
	}

	public String toString() {
		return name;
	}

	public BoundLabelVariable instantiate() {
		getMeta().reset();
		return this;
	}

	public String toUnicodeString() {
		return name;
	}

	public ArrayList<Meta<?>> getMetas() {
		return new ArrayList<Meta<?>>();
	}
	

	public ArrayList<Meta<?>> getBoundMetas() {
		return super.getMetas();

	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Label))
			return false;
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)

		if (obj instanceof BoundLabelVariable) {
			if (getMeta().getValue() == null && ((BoundLabelVariable) obj).getMeta().getValue() == null)
				return true;

			return super.getMeta().equals(((BoundLabelVariable) obj).getMeta().getValue());
		} else {
			return getMeta().equals(obj);
		}
	}

}
