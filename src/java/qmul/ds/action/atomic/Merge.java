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
import qmul.ds.tree.Modality;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;

/**
 * The <tt>merge</tt> action
 * 
 * @author mpurver
 */
public class Merge extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String FUNCTOR = "merge";
	public static Logger logger=Logger.getLogger(Merge.class);
	Modality modality;

	/**
	 * @param modality
	 */
	public Merge(Modality modality) {
		this.modality = modality;
	}

	private static final Pattern MERGE_PATTERN = Pattern.compile("(?i)" + FUNCTOR + "\\((.+)\\)");

	/**
	 * @param string
	 *            a {@link String} representation e.g. merge(/\0/\1\/*) as used in lexicon specs
	 */
	public Merge(String string) {
		Matcher m = MERGE_PATTERN.matcher(string);
		if (m.matches()) {
			modality = Modality.parse(m.group(1));
		} else {
			throw new IllegalArgumentException("unrecognised merge string");
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
		if (!node.isLocallyFixed()) {
			return null;
		}
		Node other = tree.getNode(modality.instantiate());
		if ((other == null) || other.isLocallyFixed()) {
			return null;
		}

		if (!node.isUnifiable(other)) {
			logger.debug("unification failed");
			return null;
		}

		tree.merge(modality.instantiate());
		return tree;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR + "(" + modality + ")";
	}

	public Effect instantiate() {
		return new Merge(modality.instantiate());
	}
}
