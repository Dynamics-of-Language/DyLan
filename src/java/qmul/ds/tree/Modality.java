/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.action.boundvariable.BoundModalityVariable;
import qmul.ds.action.meta.MetaModality;
import qmul.ds.tree.label.LabelFactory;

/**
 * A LOFT tree modality [\/0/\1] etc etc
 * 
 * @author mpurver
 */
public class Modality implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String FORALL_LEFT = "[";
	public static final String FORALL_RIGHT = "]";
	public static final String EXIST_LEFT = "<";
	public static final String EXIST_RIGHT = ">";
	public static final String UNICODE_EXIST_LEFT = "\u2039"; // left angle
	// quote: also
	// 3008, 27E8,
	// 2329
	public static final String UNICODE_EXIST_RIGHT = "\u203A"; // right angle
	// quote: also
	// 3009, 27E9,
	// 232A

	private boolean required;
	private ArrayList<BasicOperator> ops;

	public Modality(boolean required, List<BasicOperator> ops) {
		this.required = required;
		this.ops = new ArrayList<BasicOperator>(ops);
	}

	public static final Pattern MODALITY_PATTERN = Pattern.compile("(" + Pattern.quote(FORALL_LEFT) + "|"
			+ Pattern.quote(EXIST_LEFT) + ")?((" + BasicOperator.OP_PATTERN + ")+)(" + Pattern.quote(FORALL_RIGHT)
			+ "|" + Pattern.quote(EXIST_RIGHT) + ")?");
	public static final Pattern META_MODALITY_PATTERN = Pattern.compile("(" + Pattern.quote(FORALL_LEFT) + "|"
			+ Pattern.quote(EXIST_LEFT) + ")?(" + LabelFactory.METAVARIABLE_PATTERN + ")("
			+ Pattern.quote(FORALL_RIGHT) + "|" + Pattern.quote(EXIST_RIGHT) + ")?");
	public static final Pattern MODALITY_VARIABLE_PATTERN = Pattern.compile("(" + Pattern.quote(FORALL_LEFT) + "|"
			+ Pattern.quote(EXIST_LEFT) + ")?(" + LabelFactory.VAR_PATTERN + ")(" + Pattern.quote(FORALL_RIGHT) + "|"
			+ Pattern.quote(EXIST_RIGHT) + ")?");
	public static final Pattern ANY_MODALITY_PATTERN = Pattern.compile("(" + Pattern.quote(FORALL_LEFT) + "|"
			+ Pattern.quote(EXIST_LEFT) + ")?((" + BasicOperator.OP_PATTERN + ")+|" + LabelFactory.METAVARIABLE_PATTERN
			+ ")(" + Pattern.quote(FORALL_RIGHT) + "|" + Pattern.quote(EXIST_RIGHT) + ")?");

	/**
	 * Just for use by {@link MetaModality}
	 */
	protected Modality() {
	}

	/**
	 * @param string
	 *            a {@link String} representation of a modality e.g. [\/0/\1] as used in lexicon specs
	 */
	private Modality(String string) {
		Matcher m = MODALITY_PATTERN.matcher(string);
		if (m.matches()) {
			required = ((m.group(1) != null) && (m.group(1).contains(FORALL_LEFT)));
			ops = BasicOperator.create(m.group(2));
		} else {
			throw new IllegalArgumentException("unrecognised modality string " + string);
		}
	}

	/**
	 * @param string
	 *            a {@link String} representation of a modality e.g. [\/0/\1] as used in lexicon specs
	 * @return a new modality
	 */
	public static Modality parse(String string) {
		Matcher m = MODALITY_PATTERN.matcher(string);
		if (m.matches()) {
			return new Modality(string);
		}
		m = META_MODALITY_PATTERN.matcher(string);
		if (m.matches()) {
			return MetaModality.get(m.group(2));
		}
		m = MODALITY_VARIABLE_PATTERN.matcher(string);
		if (m.matches())
			return new BoundModalityVariable(m.group(2));

		throw new IllegalArgumentException("unrecognised modality string " + string);
	}

	/**
	 * @return the required
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * @return the ops
	 */
	public List<BasicOperator> getOps() {
		return new ArrayList<BasicOperator>(ops);
	}

	/**
	 * @return false if the path contains a Kleene star, true otherwise
	 */
	public boolean isFixed() {
		for (BasicOperator op : ops) {
			if (!op.isFixed()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return true if the path length is 1
	 */
	public boolean isAtomic() {
		return (ops.size() == 1);
	}

	/**
	 * @return the same modality in the inverse direction
	 */
	public Modality inverse() {
		ArrayList<BasicOperator> ops = new ArrayList<BasicOperator>();
		for (BasicOperator op : this.ops) {
			ops.add(op.inverse());
		}
		Collections.reverse(ops);
		return new Modality(required, ops);
	}

	public boolean relates(Tree t, Node from, Node to) {
		return relates(t, from.getAddress(), to.getAddress());
	}

	public boolean relates(Tree t, NodeAddress from, NodeAddress to) {
		return from.to(t, to, this);
	}

	/**
	 * @return this {@link Modality} with the first {@link BasicOperator} removed
	 */
	public Modality pop() {
		return new Modality(required, ops.subList(1, ops.size()));
	}

	/**
	 * @return an instantiated version of this {@link Modality}, with all meta-elements replaced by their values. By
	 *         default, just return this {@link Modality} unchanged. This will be overridden by {@link MetaModality}s
	 *         and the like
	 */
	public Modality instantiate() {
		return this;
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
		result = prime * result + ((ops == null) ? 0 : ops.hashCode());
		result = prime * result + (required ? 1231 : 1237);
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
		Modality other = (Modality) obj;
		if (ops == null) {
			if (other.ops != null)
				return false;
		} else if (!ops.equals(other.ops))
			return false;
		if (required != other.required)
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
		String path = "";
		for (BasicOperator op : ops) {
			path += op.toString();
		}
		return (required ? FORALL_LEFT : EXIST_LEFT) + path + (required ? FORALL_RIGHT : EXIST_RIGHT);
	}

	/**
	 * @return replacing EXIST brackets & arrows with their Unicode versions
	 */
	public String toUnicodeString() {
		String path = "";
		for (BasicOperator op : ops) {
			path += op.toUnicodeString();
		}
		return (required ? FORALL_LEFT : UNICODE_EXIST_LEFT) + path + (required ? FORALL_RIGHT : UNICODE_EXIST_RIGHT);
	}

}
