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
import qmul.ds.tree.Modality;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.TypeLabel;

/**
 * The <tt>put</tt> action
 * 
 * @author mpurver
 */
public class CopyContent extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String FUNCTOR = "copy_content";

	private Modality mod;

	/**
	 * @param label
	 */
	public CopyContent(Modality mod) {
		this.mod = mod;
	}

	private static final Pattern PUT_PATTERN = Pattern.compile("(?i)" + FUNCTOR + "\\((.+)\\)");

	/**
	 * @param string
	 *            a {@link String} representation e.g. put(ty(t)) as used in lexicon specs
	 */
	public CopyContent(String string) {
		Matcher m = PUT_PATTERN.matcher(string);
		if (m.matches()) {
			mod = Modality.parse(m.group(1));
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
		Node n = tree.getNode(this.mod.instantiate());
		if (!n.hasType())
			return null;

		for (Label l : n) {
			if (l instanceof TypeLabel || l instanceof FormulaLabel)
				tree.put(l.instantiate());
		}
		return tree;
	}

	/**
	 * @return the label to put
	 */
	public Modality getModality() {
		return mod;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR + "(" + mod + ")";
	}

	public Effect instantiate() {
		return new CopyContent(this.mod.instantiate());
	}

}
