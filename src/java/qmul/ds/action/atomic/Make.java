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
import qmul.ds.tree.BasicOperator;
import qmul.ds.tree.Node;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.BottomLabel;

/**
 * The <tt>make</tt> action
 * 
 * @author mpurver
 */
public class Make extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String FUNCTOR = "make";

	BasicOperator op;

	/**
	 * @param op
	 */
	public Make(BasicOperator op) {
		this.op = op;
	}

	private static final Pattern MAKE_PATTERN = Pattern.compile("(?i)" + FUNCTOR + "\\((.+)\\)");

	/**
	 * @param string
	 *            a {@link String} representation e.g. make(\/1) as used in lexicon specs
	 */
	public Make(String string) {
		Matcher m = MAKE_PATTERN.matcher(string);
		if (m.matches()) {
			op = new BasicOperator(m.group(1));
		} else {
			throw new IllegalArgumentException("unrecognised make string");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.atomic.Effect#exec(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	@Override
	public <T extends Tree> T execTupleContext(T tree, ParserTuple context) {
		// logger.debug("Making " + this.op);
		// logger.debug("At:" + tree);
		if (tree.getPointedNode().contains(new BottomLabel()) && !op.isLink()) {
			logger.debug("Cannot make node. BottomLabel present");
			return null;
		}
		// currently not failing if node already exists
		// but nothing done if so...
		NodeAddress addr = tree.getPointer().go(op);
		if (!tree.containsKey(addr)) {
			tree.make(op);
		} else
			logger.warn("request to make already existing node");

		return tree;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR + "(" + op + ")";
	}

	public Effect instantiate() {
		return this;
	}
}
