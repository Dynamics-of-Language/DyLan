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
import qmul.ds.tree.Tree;

/**
 * The <tt>go</tt> action
 * 
 * @author mpurver
 */
public class Go extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String FUNCTOR = "go";

	Modality modality;

	/**
	 * @param modality
	 */
	public Go(Modality modality) {
		this.modality = modality;
	}

	private static final Pattern GO_PATTERN = Pattern.compile("(?i)" + FUNCTOR + "\\((.+)\\)");

	/**
	 * @param string
	 *            a {@link String} representation e.g. go(/\1) as used in lexicon specs
	 */
	public Go(String string) {
		Matcher m = GO_PATTERN.matcher(string);
		if (m.matches()) {
			modality = Modality.parse(m.group(1));
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
		tree.go(modality.instantiate());
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
	
		return new Go(modality.instantiate());
	}
}
