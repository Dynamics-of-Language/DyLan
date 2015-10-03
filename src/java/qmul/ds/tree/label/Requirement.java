/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.tree.label;

import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.tree.Tree;

/**
 * A requirement ?X for some arbitrary label X
 * 
 * @author mpurver
 */
public class Requirement extends EmbeddedLabel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String PREFIX = "?";

	/**
	 * @param label
	 */

	public Requirement(Label label) {
		this(label, null);
	}

	public Requirement(Label label, IfThenElse ite) {
		super(ite);
		this.label = label;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#subsumes(qmul.ds.tree.label.Label)
	 */
	@Override
	public boolean subsumes(Label other) {
		// TODO HACK - for ?Ex.Tn(x) we're always returning true (as per super.subsumes(other))
		// we need to implement subsumes for ExistentialLabelConjunction with Tn(x) as a special case. ....
		if (super.subsumes(other)) {
			return true;
		}
		// TODO HACK - we need to check modal requirements at OTHER nodes ...
		// for now just let them go
		if (label instanceof ModalLabel) {
			return true;
		}

		
		return label.subsumes(other);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.EmbeddedLabel#instantiate()
	 */
	@Override
	public Label instantiate() {
		return new Requirement(label.instantiate());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// logger.debug("Testing requirement equality " + this + "=" + obj);
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		// logger.debug("reached here");
		Requirement other = (Requirement) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return PREFIX + label;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#toUnicodeString()
	 */
	@Override
	public String toUnicodeString() {
		return PREFIX + label.toUnicodeString();
	}
	
	
	

}
