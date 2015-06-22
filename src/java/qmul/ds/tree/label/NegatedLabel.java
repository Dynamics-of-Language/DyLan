/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.tree.label;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.tree.Tree;

/**
 * A negated label �L
 * 
 * @author mpurver
 */
public class NegatedLabel extends EmbeddedLabel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String FUNCTOR = "¬";

	/**
	 * @param label
	 */
	public NegatedLabel(Label label) {
		this(label, null);
	}

	public NegatedLabel(Label label, IfThenElse ite) {
		super(ite);
		this.label = label;
		if (label instanceof NegatedLabel) {
			throw new RuntimeException("multiple negation");
		}
	}

	private static final Pattern NEG_LABEL_PATTERN = Pattern.compile(Pattern.quote(FUNCTOR) + "(.*)");

	/**
	 * @param string
	 *            a {@link String} representation of a negated label, e.g. �Ex.Ty(x) as used in lexicon specs
	 */
	public NegatedLabel(String string, IfThenElse ite) {
		super(ite);
		Matcher m = NEG_LABEL_PATTERN.matcher(string);
		if (m.matches()) {
			this.label = LabelFactory.create(m.group(1), ite);
		} else {
			throw new IllegalArgumentException("unrecognised negated label string " + string);
		}
	}

	public NegatedLabel(String string) {
		this(string, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#check(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	@Override
	public boolean check(Tree tree, ParserTuple context) {
		return !label.check(tree, context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.EmbeddedLabel#instantiate()
	 */
	@Override
	public Label instantiate() {
		return new NegatedLabel(label.instantiate());
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
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NegatedLabel other = (NegatedLabel) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
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
		return FUNCTOR + label;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#toUnicodeString()
	 */
	@Override
	public String toUnicodeString() {
		return FUNCTOR + label.toUnicodeString();
	}

}
