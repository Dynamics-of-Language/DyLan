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
import qmul.ds.action.meta.MetaPredicateArgument;
import qmul.ds.tree.label.PredicateArgument;

/**
 * Represents a bound variable of type {@link PredicateArgument}, as used in existential labels, e.g. Ex.class(x)
 * 
 * @author Arash
 * 
 */
@SuppressWarnings("serial")
public class BoundPredicateArgumentVariable extends MetaPredicateArgument {

	String name;

	public BoundPredicateArgumentVariable(String name) {
		super(MetaElement.getBoundMeta(PredicateArgument.class));
		this.name = name;
	}

	public String toString() {
		return name;
	}

	public String toUnicodeString() {
		return name;
	}

	public BoundPredicateArgumentVariable instantiate() {
		getMeta().reset();
		return this;
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
		if (!(obj instanceof PredicateArgument))
			return false;
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)

		if (obj instanceof BoundPredicateArgumentVariable) {
			if (getMeta().getValue() == null && ((BoundPredicateArgumentVariable) obj).getMeta().getValue() == null)
				return true;

			return super.getMeta().equals(((BoundPredicateArgumentVariable) obj).getMeta().getValue());
		} else {
			return getMeta().equals(obj);
		}
	}
}
