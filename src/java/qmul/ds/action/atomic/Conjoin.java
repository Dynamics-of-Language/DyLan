/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action.atomic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.ParserTuple;
import qmul.ds.formula.Formula;
import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.Label;

/**
 * The <tt>conjoin</tt> action
 * 
 * @author mpurver
 */
public class Conjoin extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String FUNCTOR = "conjoin";

	Formula formula;

	/**
	 * @param modality
	 */
	public Conjoin(Formula modality) {
		this.formula = modality;
	}

	private static final Pattern CONJOIN_PATTERN = Pattern.compile("(?i)" + FUNCTOR + "\\((.+)\\)");

	/**
	 * @param string
	 *            a {@link String} representation e.g. addfo(iota(x,man(x))) as used in lexicon specs
	 */
	public Conjoin(String string) {
		Matcher m = CONJOIN_PATTERN.matcher(string);
		if (m.matches()) {
			formula = Formula.create(m.group(1));
		} else {
			throw new IllegalArgumentException("unrecognised conjoin string");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.atomic.Effect#exec(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	@Override
	public <T extends Tree> T execTupleContext(T tree, ParserTuple context) {
		Node node = tree.getPointedNode();
		FormulaLabel l = null;
		for (Label label : node) {
			if (label instanceof FormulaLabel) {
				l = (FormulaLabel) label;
				break;
			}
		}
		Formula f = null;
		if ((l == null) || (l.getFormula() == null)) {
			if (formula instanceof TTRFormula)
				f = new TTRRecordType();
			else
				return null;
		} else
			f = l.getFormula();

		node.remove(l);

		Formula instance = formula.instantiate();

		// if (instance instanceof TTRFormula)
		// instance=((TTRFormula)instance).freshenVars(tree);

		FormulaLabel conjoined = new FormulaLabel(f.conjoin(instance));

		node.add(conjoined);
		return tree;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR + "(" + formula + ")";
	}

	public Effect instantiate() {
		return new Conjoin(formula.instantiate());
	}

}
