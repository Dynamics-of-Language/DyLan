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

/**
 * this represents a 'indef' label as used in action specs. This can take the form Indef(-) or Indef(+); the latter
 * means that the type(e) formula on the pointed node is an indefinite noun phrase. The former means that the ty(e)
 * formula on the pointed node is a non-indefinite noun phrase.
 * 
 * @author Arash
 * 
 */
public class IndefLabel extends Label {

	public static final String FUNCTOR = "Indef";
	public static final Pattern INDEF_LABEL_PATTERN = Pattern.compile(FUNCTOR + "\\((\\+|\\-)\\)");
	private boolean positive;

	public IndefLabel(String s, IfThenElse ite) {
		super(ite);

		Matcher m = INDEF_LABEL_PATTERN.matcher(s);
		if (m.matches()) {
			if (m.group(1).equals("+")) {
				positive = true;
			} else
				positive = false;
		} else
			throw new IllegalArgumentException("Unrecognised Indef Label:" + s);
	}

	public IndefLabel(String string) {
		this(string, null);
	}

	public String toString() {
		if (positive)
			return FUNCTOR + "(+)";
		else
			return FUNCTOR + "(-)";
	}

	public String toUnicodeString() {
		return toString();
	}

	public int hashCode() {
		return positive ? 11 : 13;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		IndefLabel other;
		if (o instanceof IndefLabel) {
			other = (IndefLabel) o;
		} else
			return false;

		return positive == other.positive;
	}

	public boolean isIndefinite() {
		return positive;
	}

	public static void main(String a[]) {
		IndefLabel id = new IndefLabel("Indef(+)");

	}

}
