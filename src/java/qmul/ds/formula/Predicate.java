/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.formula;

/**
 * A predicate within a {@link Formula}
 * 
 * @author mpurver
 */
public class Predicate extends AtomicFormula {

	private static final long serialVersionUID = 1L;

	/**
	 * @param formula
	 */
	public Predicate(String formula) {
		super(formula);
	}

	public Predicate(Predicate predicate) {
		super(predicate.name);
	}

}
