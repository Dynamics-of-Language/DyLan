/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action.atomic;

import qmul.ds.ParserTuple;
import qmul.ds.formula.FOLLambdaAbstract;
import qmul.ds.formula.Formula;
import qmul.ds.formula.LambdaAbstract;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.TypeLabel;
import qmul.ds.type.ConstructedType;
import qmul.ds.type.DSType;

/**
 * The <tt>beta-reduce</tt> action
 * 
 * @author mpurver
 */
public class BetaReduce extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String FUNCTOR = "beta-reduce";

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.atomic.Effect#exec(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */

	@Override
	public <T extends Tree> T exec(T tree, ParserTuple context) {
		Node node = tree.getPointedNode();
		Node d0 = tree.get(node.getAddress().down0());
		DSType t0 = d0.getType();
		Formula f0 = d0.getFormula();
		Node d1 = tree.get(node.getAddress().down1());
		DSType t1 = d1.getType();
		Formula f1 = d1.getFormula();
		logger.debug("Down 1 is:" + d1);
		if (!(t1 instanceof ConstructedType)) {
			throw new RuntimeException("unsuitable functor type " + t1);
		}
		ConstructedType ct1 = (ConstructedType) t1;
		if (!ct1.getFrom().equals(t0)) {
			throw new RuntimeException("unsuitable types " + t0 + " " + ct1);
		}
		if (!(f1 instanceof LambdaAbstract)) {
			logger.error("F1 not lambda:" + f1);
			throw new RuntimeException("unsuitable formula " + f1 + " " + f1.getClass());
		}
		logger.debug("down0 before betareduction:" + f0);
		Formula f = ((LambdaAbstract) f1).betaReduce(f0);
		logger.debug("down0 after betareduction" + f0);
		if (f == null) {
			throw new RuntimeException("failed beta-reduction");
		}

		node.addLabel(new TypeLabel(ct1.getTo()));
		node.addLabel(new FormulaLabel(f));
		return tree;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR;
	}

	public Effect instantiate() {
		return this;
	}

}
