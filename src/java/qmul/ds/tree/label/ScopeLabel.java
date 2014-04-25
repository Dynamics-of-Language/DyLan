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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.boundvariable.BoundFormulaVariable;
import qmul.ds.action.meta.MetaElement;
import qmul.ds.action.meta.MetaFormula;
import qmul.ds.formula.Formula;
import qmul.ds.tree.Node;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;

/**
 * This represents a scope label, e.g. Sc(x). This is usually only embedded in a requirement. e.g. ?Sc(x), and not used
 * on its own.
 * 
 * 
 * @author Arash
 * 
 */
public class ScopeLabel extends Label {

	public static String FUNCTOR = "Sc";
	private static final Pattern SCOPE_LABEL_PATTERN = Pattern.compile(FUNCTOR + "\\((.+)\\)");

	private Formula var;

	public ScopeLabel(Formula a, IfThenElse ite) {
		super(ite);
		this.var = a;

	}

	public ScopeLabel(Formula a) {
		this(a, null);
	}

	public ScopeLabel(String s, IfThenElse ite) {
		super(ite);
		Matcher m = SCOPE_LABEL_PATTERN.matcher(s);

		if (m.matches()) {
			var = Formula.create(m.group(1));
		} else
			throw new IllegalArgumentException("unrecognised Scope Label String: " + s);
	}

	public ScopeLabel(String s) {
		this(s, null);
	}

	public boolean check(Tree t, ParserTuple context) {

		Node n = t.getPointedNode();

		Label tyT = LabelFactory.create("ty(t)", null);
		Label reqTyT = LabelFactory.create("?ty(t)", null);
		NodeAddress curAddress = t.getPointer();
		// find ty(t) node above. Not going across Links.
		while (!(tyT.check(n) && reqTyT.check(n))) {
			curAddress = curAddress.upNonLink();
			if (curAddress == null) {
				logger.debug("couldn't find ty(t) or ?ty(t) node above when checking ScopeLabel");
				return false;
			} else
				n = t.get(curAddress);

		}
		// now check if there's a scope statement involving the variable
		for (Label l : n) {
			if (l instanceof ScopeStatement) {
				ScopeStatement sc = (ScopeStatement) l;
				if (sc.involves(this.var))
					return true;
			}
		}
		return false;
	}

	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o == this)
			return true;
		if (!(o instanceof ScopeLabel))
			return false;
		ScopeLabel other = (ScopeLabel) o;
		return this.var.equals(other.var);
	}

	public ScopeLabel instantiate() {
		return new ScopeLabel(this.var.instantiate());
	}

	public String toString() {
		return FUNCTOR + "(" + var + ")";
	}

	public String toUnicodeString() {
		return FUNCTOR + "(" + var.toUnicodeString() + ")";
	}

	public ArrayList<MetaElement<?>> getMetas() {
		ArrayList<MetaElement<?>> result = new ArrayList<MetaElement<?>>();
		if (var instanceof MetaFormula)
			result.addAll(((MetaFormula) var).getMetas());

		return result;
	}

	public ArrayList<MetaElement<?>> getBoundMetas() {
		ArrayList<MetaElement<?>> result = new ArrayList<MetaElement<?>>();
		if (var instanceof BoundFormulaVariable)
			result.addAll(((BoundFormulaVariable) var).getBoundMetas());

		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((var == null) ? 0 : var.hashCode());
		return result;
	}
}
