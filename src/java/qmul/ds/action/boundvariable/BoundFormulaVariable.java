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
import qmul.ds.action.meta.MetaFormula;
import qmul.ds.formula.Formula;

/**
 * Represents a bound variable of type {@link Formula}, as used in existential labels, e.g. Ex.fo(x)
 * 
 * @author Arash
 * 
 */
public class BoundFormulaVariable extends MetaFormula {

	private static final long serialVersionUID = 1L;

	String name;

	public BoundFormulaVariable(String name) {
		super(MetaElement.getBoundMeta(Formula.class));
		this.name = name;
	}

	public String toString() {
		return super.toString();
		// return name;
	}

	public BoundFormulaVariable instantiate() {
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
		if (!(obj instanceof Formula))
			return false;
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)
		logger.debug("calling meta.equals");
		if (obj instanceof BoundFormulaVariable) {
			if (getMeta().getValue() == null && ((BoundFormulaVariable) obj).getMeta().getValue() == null)
				return true;

			return super.getMeta().equals(((BoundFormulaVariable) obj).getMeta().getValue());
		} else {
			return getMeta().equals(obj);
		}
	}

}
