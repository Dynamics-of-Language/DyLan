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
 * The <tt>put</tt> action
 * 
 * @author mpurver
 */
public class Put extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String FUNCTOR = "put";

	private Label label;

	/**
	 * @param label
	 */
	public Put(Label label) {
		this.label = label;
	}

	private static final Pattern PUT_PATTERN = Pattern.compile("(?i)" + FUNCTOR + "\\((.+)\\)");

	/**
	 * @param string
	 *            a {@link String} representation e.g. put(ty(t)) as used in lexicon specs
	 */
	public Put(String string) {
		Matcher m = PUT_PATTERN.matcher(string);
		if (m.matches()) {
			label = LabelFactory.create(m.group(1), null);
		} else {
			throw new IllegalArgumentException("unrecognised put string");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.atomic.Effect#exec(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	@Override
	public <T extends Tree> T exec(T tree, ParserTuple context) {
		// logger.debug("putting " + label.instantiate());
		Label instance = label.instantiate();
		if (tree.getPointedNode().contains(instance)) {
			logger.warn("putting already existing label: " + label.instantiate());
			return tree;
		}
		tree.put(instance);
		return tree;
	}

	/**
	 * @return the label to put
	 */
	public Label getLabel() {
		return label;
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
		return new Put(this.label.instantiate());
	}

}
