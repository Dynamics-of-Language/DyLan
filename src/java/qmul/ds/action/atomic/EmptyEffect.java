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
import qmul.ds.tree.Tree;

/**
 * The <tt>abort</tt> action
 * 
 * @author mpurver
 */
public class EmptyEffect extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String FUNCTOR = "do_nothing";

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.atomic.Effect#exec(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	@Override
	public <T extends Tree> T exec(T tree, ParserTuple context) {
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

	public int hashCode() {
		return FUNCTOR.hashCode();
	}

	public boolean equals(Object o) {
		if (o instanceof EmptyEffect)
			return true;

		return false;
	}

}
