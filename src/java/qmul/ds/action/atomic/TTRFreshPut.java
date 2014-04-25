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

import org.apache.log4j.Logger;

import qmul.ds.ParserTuple;
import qmul.ds.formula.Formula;
import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRLambdaAbstract;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.Label;

/**
 * The <tt>ttrput</tt> action... puts a TTRFormula (TTRRecordType or TTRLambdaAbstract..0
 * 
 * @author arash
 */
public class TTRFreshPut extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static Logger logger = Logger.getLogger(TTRFreshPut.class);
	public static final String FUNCTOR = "ttrput";

	private TTRFormula ttrF;

	public TTRFreshPut(TTRFormula f) {
		
		this.ttrF = f;
	}

	private static final Pattern TTR_FRESH_PUT_PATTERN = Pattern.compile("(?i)" + FUNCTOR + "\\((.+)\\)");

	/**
	 * @param string
	 *            a {@link String} representation e.g. put(ty(t)) as used in lexicon specs
	 */
	public TTRFreshPut(String string) {
		Matcher m = TTR_FRESH_PUT_PATTERN.matcher(string);
		if (m.matches()) {
			Formula f = Formula.create(m.group(1));
			if (f == null || !(f instanceof TTRFormula))
				throw new IllegalArgumentException("could not parse ttr rec type or lambda abstract:" + m.group(1));
			this.ttrF = (TTRFormula)f;
		} else {
			throw new IllegalArgumentException("unrecognised ttr freshput string");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.atomic.Effect#exec(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	@Override
	public <T extends Tree> T exec(T tree, ParserTuple context) {
		Node node = tree.getPointedNode();
		FormulaLabel l = null;
		for (Label label : node) {
			if (label instanceof FormulaLabel) {
				l = (FormulaLabel) label;
				break;
			}
		}
		Formula fresh = ttrF.freshenVars(tree);
		if (l != null) {
			logger.warn("trying to add ttr formula:" + fresh + " to node with already existing formula:" + node);
			logger.warn("returning tree intact");
			return tree;
		}
		/*
		 * FormulaLabel conjoined = null; if ((l != null) && (l.getFormula() != null)) { node.remove(l); Formula onNode
		 * = l.getFormula(); conjoined = new FormulaLabel(onNode.conjoin(fresh));
		 * 
		 * } else { conjoined = new FormulaLabel(fresh); }
		 */
		node.add(new FormulaLabel(fresh));
		return tree;
	}

	/**
	 * @return the TTR Formula to put
	 */
	public Formula getTTRFormula() {
		return ttrF;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR + "(" + ttrF + ")";
	}

	public Effect instantiate() {
		return new TTRFreshPut(this.ttrF.instantiate());
	}

	public boolean equals(Object o)
	{
		if (!(o instanceof TTRFreshPut))
			return false;
		
		TTRFreshPut oth=(TTRFreshPut)o;
		if(this.ttrF==null)
			return oth.ttrF==null;
		
		if (oth.ttrF==null)
			return false;
		
		return this.ttrF.subsumes(oth.ttrF)&&oth.ttrF.subsumes(this.ttrF);
		
	}
	
	public static void main(String a[])
	{
		Formula e=Formula.create("(R2 ++ (R1 ++ [e1==eq : es|p2==subj(e1, R1.head) : t|head==e1 : es|p3==obj(e1, R2.head) : t]))");
		Formula e1=Formula.create("(R2 ++ (R1 ++ [e2==eq : es|p2==subj(e2, R1.head) : t|p3==obj(e2, R2.head) : t|head==e2 : es]))");
		System.out.println(e.subsumes(e1)&&e1.subsumes(e));
	}
}
