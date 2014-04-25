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

public class PredicateArgument implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String content;

	public PredicateArgument(String value) {
		this.content = value;
	}

	public PredicateArgument() {
	}

	public PredicateArgument instantiate() {

		return this;
	}

	public String getContent()

	{
		return content;
	}

	public String toString() {
		return content;
	}

	public String toUnicodeString() {
		return content;
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj.getClass() != this.getClass())
			return false;
		PredicateArgument upa = (PredicateArgument) obj;
		if (!upa.getContent().equals(content))
			return false;

		return true;
	}

}
