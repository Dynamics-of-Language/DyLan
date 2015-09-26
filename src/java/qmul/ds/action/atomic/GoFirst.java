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
import qmul.ds.tree.Node;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.LabelFactory;

/**
 * Objects of this class correspond to the gofirst(LABEL) action which finds the closest node up the tree from the
 * pointer with the label LABEL, and moves the pointer there. If no such node is found null is returned so that
 * backtracking can ensue.
 * 
 * @author Arash
 * 
 */
public class GoFirst extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String FUNCTOR = "gofirst";

	Label label;

	public GoFirst(Label l) {
		this.label = l;
	}

	private static final Pattern GOFIRST_PATTERN = Pattern.compile("(?i)" + FUNCTOR + "\\((.+)\\)");

	public GoFirst(String string) {
		Matcher m = GOFIRST_PATTERN.matcher(string);
		if (m.matches()) {
			label = LabelFactory.create(m.group(1), null);
		} else {
			throw new IllegalArgumentException("unrecognised go string:" + string);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.atomic.Effect#exec(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	@Override
	public <T extends Tree> T execTupleContext(T tree, ParserTuple context) {
		Node currentNode = tree.getPointedNode();
		NodeAddress curAddress = tree.getPointer();
		while (!this.label.check(currentNode)) {
			curAddress = curAddress.upNonLink();
			if (curAddress == null) {
				logger.debug("GoFirst action failed. Couldn't find node with required label above. Returning null");
				return null;
			} else
				currentNode = tree.get(curAddress);

		}
		tree.setPointer(curAddress);
		return tree;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR + "(" + label + ")";
	}

	public Effect instantiate() {
		return new GoFirst(this.label.instantiate());
	}

}
