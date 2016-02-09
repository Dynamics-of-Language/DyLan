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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.boundvariable.BoundFormulaVariable;
import qmul.ds.action.meta.Meta;
import qmul.ds.action.meta.MetaFormula;
import qmul.ds.formula.Formula;
import qmul.ds.tree.Node;

public class FormulaEqualityLabel extends Label implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final Pattern EQUALITY_PATTERN = Pattern.compile("(.+)\\s*=\\s*(.+)");
	Formula left;
	Formula right;

	public FormulaEqualityLabel(String s, IfThenElse ite) {
		super(ite);
		Matcher m = EQUALITY_PATTERN.matcher(s);
		if (m.matches()) {
			left = Formula.create(m.group(1));
			right = Formula.create(m.group(2));

		} else
			throw new IllegalArgumentException("Unrecognized equality label: " + s);
	}

	public FormulaEqualityLabel(String s) {
		this(s, null);
	}

	public boolean check(Node n) {

		return left.equals(right) || right.equals(left);

	}

	public String toString() {
		return "(" + left + "=" + right + ")";
	}

	public String toUnicodeString() {
		return left.toUnicodeString() + "=" + right.toUnicodeString();
	}

	public ArrayList<Meta<?>> getMetas() {
		ArrayList<Meta<?>> result = new ArrayList<Meta<?>>();
		if (left instanceof MetaFormula)
			result.addAll(((MetaFormula) left).getMetas());
		if (right instanceof MetaFormula)
			result.addAll(((MetaFormula) right).getMetas());

		return result;
	}

	public ArrayList<Meta<?>> getBoundMetas() {
		ArrayList<Meta<?>> result = new ArrayList<Meta<?>>();
		if (left instanceof BoundFormulaVariable)
			result.addAll(((BoundFormulaVariable) left).getBoundMetas());
		if (right instanceof BoundFormulaVariable)
			result.addAll(((BoundFormulaVariable) right).getBoundMetas());

		return result;
	}

}
