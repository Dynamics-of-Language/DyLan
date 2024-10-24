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
import qmul.ds.action.meta.Meta;
import qmul.ds.action.meta.MetaFormula;
import qmul.ds.formula.Formula;
import qmul.ds.tree.Node;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;

public class DOMPlusLabel extends Label {

	public static final String FUNCTOR = "DOM+";
	public static final Pattern DOM_PATTERN = Pattern.compile("DOM\\+\\s*\\((.+)\\)");

	private Formula variable;

	public DOMPlusLabel(String s, IfThenElse ite) {
		super(ite);
		Matcher m = DOM_PATTERN.matcher(s);
		if (m.matches()) {
			variable = Formula.create(m.group(1));
		} else
			throw new IllegalArgumentException("Unrecognised DOM+ Label:" + s);
	}

	public ArrayList<Meta<?>> getMetas() {
		ArrayList<Meta<?>> metas = super.getMetas();
		if (this.variable instanceof MetaFormula) {
			metas.addAll(((MetaFormula) this.variable).getMetas());

		}
		return metas;
	}

	public boolean checkWithTupleAsContext(Tree t, ParserTuple context) {
		Node n = t.getPointedNode();
		Label typeTReq = LabelFactory.create("?ty(t)", null);
		if (!typeTReq.check(n)) {
			logger.debug("check for ?ty(t) failed when checking DOM+ label " + this + "\n at node:" + n);
			return false;
		}

		FormulaLabel fl = new FormulaLabel(this.variable);
		NodeAddress cur = n.getAddress();// .down1();

		IndefLabel indefN = new IndefLabel("Indef(-)");
		while (t.containsKey(cur)) {
			Node current;
			if (t.containsKey(cur.down0()))
				current = t.get(cur.down0());
			else {
				cur = cur.down1();
				continue;
			}

			Label typeE = LabelFactory.create("ty(e)", null);
			Label typeES = LabelFactory.create("ty(es)", null);
			Label reqTypeES = LabelFactory.create("?ty(es)", null);
			if (typeE.check(current)) {

				if (indefN.check(current)) {
					NodeAddress freshVarAddress = current.getAddress().down0().down0();
					if (!t.containsKey(freshVarAddress)) {
						logger.error("non-existant fresh variable node when checking DOM+");
					} else {
						if (fl.check(t.get(freshVarAddress))) {
							logger.debug("DOM+ check succeeded");
							return true;

						} else {
							logger.debug("DOM+ check failed for node:"
									+ t.get(freshVarAddress)
									+ "\nVar node did not have the required formula value. Searching the rest of the tree");

						}

					}

				} else
					logger.error("ty(e) but not indef(-) when checking DOM+");

			} else if (typeES.check(current) || reqTypeES.check(current)) {
				NodeAddress eventVarAddress = current.getAddress().down0().down0();
				if (t.containsKey(eventVarAddress)) {
					Node eventVarNode = t.get(eventVarAddress);
					if (fl.check(eventVarNode)) {
						logger.debug("DOM+ check succeeded. Matched to event variable");
						return true;

					} else {
						logger.debug("DOM+ check failed for node:"
								+ eventVarNode
								+ "\nEvent var node did not have required formula value. Searching the rest of the tree.");

					}

				} else {
					logger.error("nonexistant event var node while event node exists");
				}

			}
			cur = cur.down1();
		}
		return false;

	}

	public String toString() {
		return FUNCTOR + "(" + variable.toString() + ")";
	}

	public String toUnicodeString() {
		return FUNCTOR + "(" + variable.toUnicodeString() + ")";
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o == null)
			return false;
		DOMPlusLabel other;
		if (o instanceof DOMPlusLabel)
			other = (DOMPlusLabel) o;
		else
			return false;

		if (!variable.equals(other.variable)) {
			if (other.variable.equals(variable))
				return true;
			else
				return false;

		}
		return true;
	}

	public ArrayList<Meta<?>> getBoundMetas() {
		ArrayList<Meta<?>> metas = super.getBoundMetas();
		if (this.variable instanceof BoundFormulaVariable) {
			metas.addAll(((BoundFormulaVariable) this.variable).getBoundMetas());

		}
		return metas;
	}

	public static void main(String a[]) {
		DOMPlusLabel l = new DOMPlusLabel("DOM+(Y)", null);
		System.out.println(l);
	}

}
