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

import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.tree.Node;

/**
 * @deprecated An existentially quantified label e.g. Ex.Ty(x) - currently only one quantified variable allowed, and
 *             must be x
 * 
 * @author mpurver
 * 
 * 
 */
public class ExistentialLabel extends EmbeddedLabel {

	@SuppressWarnings("unused")
	private String variable;
	private String regex;

	public static final String FUNCTOR = "Ex.";

	/**
	 * @param variable
	 * @param label
	 */
	public ExistentialLabel(String variable, Label label) {
		this(variable, label, null);
	}

	public ExistentialLabel(String variable, Label label, IfThenElse ite) {
		super(ite);
		this.variable = variable;
		this.label = label;
		if (label instanceof ExistentialLabel) {
			throw new RuntimeException("currently only one existentially quantified label variable allowed");
		}
		this.regex = Pattern.quote(label.toString()).replaceAll(variable, "\\\\E.*\\\\Q");
	}

	private static final Pattern EXIST_LABEL_PATTERN = Pattern.compile("E(x).((.*)\\bx\\b(.*))");

	/**
	 * @param string
	 *            a {@link String} representation of an existential label, as used in lexicon specs
	 */

	public ExistentialLabel(String string) {
		super();
		Matcher m = EXIST_LABEL_PATTERN.matcher(string);
		if (m.matches()) {
			this.variable = new String(m.group(1));
			this.label = LabelFactory.create(m.group(2), null);
			this.regex = "(?i)" + Pattern.quote(m.group(3)) + ".*" + Pattern.quote(m.group(4));
		} else {
			throw new IllegalArgumentException("unrecognised modal label string " + string);
		}
	}

	public ExistentialLabel(String string, IfThenElse ite) {
		super(ite);
		Matcher m = EXIST_LABEL_PATTERN.matcher(string);
		if (m.matches()) {
			this.variable = new String(m.group(1));
			this.label = LabelFactory.create(m.group(2), null);
			this.regex = "(?i)" + Pattern.quote(m.group(3)) + ".*" + Pattern.quote(m.group(4));
		} else {
			throw new IllegalArgumentException("unrecognised modal label string " + string);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#check(qmul.ds.tree.Node)
	 */
	@Override
	public boolean check(Node node) {
		// if exactly the same (existentially quantified) label present, OK
		if (super.check(node)) {
			return true;
		}
		// otherwise check for matching label
		// currently just doing this by string pattern matching! TODO properly
		for (Label label : node) {
			if (label.toString().matches(regex)) {
				return true;
			}
		}
		// or if we're checking for an existing address "label", check we have a
		// fixed address
		if (label instanceof AddressLabel) {
			return node.isLocallyFixed();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.EmbeddedLabel#instantiate()
	 */
	@Override
	public Label instantiate() {
		return new ExistentialLabel(variable, label.instantiate(), this.embeddingITE);
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
		result = prime * result + ((regex == null) ? 0 : regex.hashCode());
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
		ExistentialLabel other = (ExistentialLabel) obj;
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
