/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.tree.label;

import java.io.Serializable;

import qmul.ds.action.atomic.IfThenElse;

/**
 * The leaf node label "down-bottom"
 * 
 * @author mpurver
 */
public class BottomLabel extends Label implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String BOTTOM_FUNCTOR = "!";
	public static final String BOTTOM_UNICODE = "\u22A5";

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return BOTTOM_FUNCTOR;
	}

	public String toUnicodeString() {
		return BOTTOM_UNICODE;
	}

	public BottomLabel(IfThenElse ite) {
		super(ite);
	}

	public BottomLabel() {
		this(null);
	}

}
