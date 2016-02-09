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
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.boundvariable.BoundPredicateArgumentVariable;
import qmul.ds.action.meta.Meta;
import qmul.ds.action.meta.MetaPredicateArgument;

public class UnaryPredicateLabel extends Label implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public final static Pattern FUNCTOR = Pattern.compile("([Tt]ense|[Cc]lass|[pP]erson|[aA]ccept)");
	public final static Pattern UNARY_PREDICATE_PATTERN = Pattern.compile(FUNCTOR.pattern() + "\\((.+)\\)");

	private PredicateArgument arg;
	private String predicate;

	/**
	 * @param type
	 */
	public UnaryPredicateLabel(String predicate, PredicateArgument arg, IfThenElse ite) {
		super(ite);
		this.predicate = predicate;
		this.arg = arg;
	}

	public UnaryPredicateLabel(String s) {
		super(null);
		Matcher m = UNARY_PREDICATE_PATTERN.matcher(s);
		if (m.matches()) {
			this.predicate = m.group(1).substring(0, 1).toUpperCase() + m.group(1).substring(1, m.group(1).length());
			if (LabelFactory.METAVARIABLE_PATTERN.matches(m.group(2))) {
				this.arg = MetaPredicateArgument.get(m.group(2));
			} else if (LabelFactory.VAR_PATTERN.matches(m.group(2)))
				this.arg = new BoundPredicateArgumentVariable(m.group(2));
			else
				this.arg = new PredicateArgument(m.group(2));

		} else
			throw new IllegalArgumentException("Invalid Unary Predicate String: " + s);

	}

	public UnaryPredicateLabel(String s, IfThenElse ite) {
		super(ite);
		Matcher m = UNARY_PREDICATE_PATTERN.matcher(s);
		if (m.matches()) {
			this.predicate = m.group(1).substring(0, 1).toUpperCase() + m.group(1).substring(1, m.group(1).length());
			if (LabelFactory.METAVARIABLE_PATTERN.matches(m.group(2))) {
				this.arg = MetaPredicateArgument.get(m.group(2));
			} else if (LabelFactory.VAR_PATTERN.matches(m.group(2)))
				this.arg = new BoundPredicateArgumentVariable(m.group(2));
			else
				this.arg = new PredicateArgument(m.group(2));

		} else
			throw new IllegalArgumentException("Invalid Unary Predicate String: " + s);
	}

	public UnaryPredicateLabel(String string, PredicateArgument pa) {
		this(string, pa, null);
	}

	public static UnaryPredicateLabel parse(String s) {
		String p;
		PredicateArgument pa;
		Matcher m = UNARY_PREDICATE_PATTERN.matcher(s);
		if (m.matches()) {
			p = m.group(1).substring(0, 1).toUpperCase() + m.group(1).substring(1, m.group(1).length());
			if (m.group(2).matches(LabelFactory.METAVARIABLE_PATTERN)) {

				pa = MetaPredicateArgument.get(m.group(2));
			} else {
				pa = new PredicateArgument(m.group(2));
			}

			return new UnaryPredicateLabel(p, pa);
		} else
			return null;

	}

	/**
	 * @return the type
	 */
	public PredicateArgument getArgument() {
		return arg;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#getMetas()
	 */
	@Override
	public ArrayList<Meta<?>> getMetas() {
		ArrayList<Meta<?>> metas = super.getMetas();

		if (arg instanceof MetaPredicateArgument) {
			metas.addAll(((MetaPredicateArgument) arg).getMetas());
		}
		return metas;
	}

	@Override
	public ArrayList<Meta<?>> getBoundMetas() {
		ArrayList<Meta<?>> metas = super.getBoundMetas();

		if (arg instanceof BoundPredicateArgumentVariable) {
			metas.addAll(((BoundPredicateArgumentVariable) arg).getBoundMetas());
		}
		return metas;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#instantiate()
	 */
	@Override
	public Label instantiate() {
		return new UnaryPredicateLabel(this.predicate, arg.instantiate());
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
		result = prime * result + ((arg == null) ? 0 : arg.hashCode());
		result = prime * result + ((predicate == null) ? 0 : predicate.hashCode());
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
		UnaryPredicateLabel other = (UnaryPredicateLabel) obj;
		if (arg == null) {
			if (other.arg != null)
				return false;
		} else if (!arg.equals(other.arg)) {
			return false;
		}
		if (!predicate.equalsIgnoreCase(other.predicate)) {
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
		return predicate + "(" + arg + ")";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#toUnicodeString()
	 */
	@Override
	public String toUnicodeString() {
		return predicate + "(" + arg.toUnicodeString() + ")";
	}

	public String getPredicate() {
		return predicate;
	}

}
