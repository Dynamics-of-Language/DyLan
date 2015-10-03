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
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.LabelFactory;

/**
 * The <tt>delete</tt> action
 * 
 * @author mpurver
 */
public class Delete extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String FUNCTOR = "delete";

	Label label;

	/**
	 * @param label
	 */
	public Delete(Label label) {
		this.label = label;
	}

	private static final Pattern DELETE_PATTERN = Pattern.compile("(?i)" + FUNCTOR + "\\((.+)\\)");

	/**
	 * @param string
	 *            a {@link String} representation e.g. delete(ty(t)) as used in lexicon specs
	 */
	public Delete(String string) {
		Matcher m = DELETE_PATTERN.matcher(string);
		if (m.matches()) {
			label = LabelFactory.create(m.group(1), null);
		} else {
			throw new IllegalArgumentException("unrecognised delete string");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.atomic.Effect#exec(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	@Override
	public <T extends Tree> T execTupleContext(T tree, ParserTuple context) {
		logger.debug("deleting " + label.instantiate());
		tree.delete(label.instantiate());
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
		return new Delete(label.instantiate());
	}

}
