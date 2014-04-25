/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action.atomic;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.ParserTuple;
import qmul.ds.action.meta.MetaFormula;
import qmul.ds.formula.Formula;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.ScopeStatement;

public class SaturateScopeDep extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String FUNCTOR = "saturate_scope_dep";
	public static final Pattern SATURATE_SCOPE_PATTERN = Pattern.compile(FUNCTOR + "\\(\\s*(.+)\\s*,\\s*(.+)\\s*\\)");
	private Formula f1;
	private Formula f2;

	public SaturateScopeDep(String s) {

		Matcher m = SATURATE_SCOPE_PATTERN.matcher(s);
		if (m.matches()) {
			f1 = Formula.create(m.group(1));
			f2 = Formula.create(m.group(2));

		} else
			throw new IllegalArgumentException("Unrecognised Saturate Scope Dep action spec: " + s);

	}

	@Override
	public <T extends Tree> T exec(T tree, ParserTuple context) {

		Node n = tree.getPointedNode();
		ArrayList<ScopeStatement> toBeAdded = new ArrayList<ScopeStatement>();
		Formula f1Instance = f1.instantiate();
		Formula f2Instance = f2.instantiate();
		if (f1Instance instanceof MetaFormula || f2Instance instanceof MetaFormula) {
			logger.debug("returning tree intact");
			return tree;
		}
		for (Label l : n) {
			if (l instanceof ScopeStatement) {
				ScopeStatement ss = (ScopeStatement) l;
				Formula narrowScoped = ss.getNarrowest();
				Formula wideScoped = ss.getWidest();

				if (wideScoped.equals(f1Instance)) {
					if (!(f2Instance.equals(narrowScoped)))
						toBeAdded.add(new ScopeStatement(f2Instance, narrowScoped));
				}

			}
		}

		for (ScopeStatement ss : toBeAdded)
			tree.put(ss.instantiate());

		return tree;
	}

	public String toString() {
		return FUNCTOR + "(" + f1 + "," + f2 + ")";
	}

	public String toUnicodeString() {
		return FUNCTOR + "(" + f1.toUnicodeString() + "," + f2.toUnicodeString() + ")";
	}

	public Effect instantiate() {
		// for now.. no metas involved here??
		return this;
	}

}
