/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import qmul.ds.action.meta.MetaType;
import qmul.ds.tree.label.LabelFactory;
import edu.stanford.nlp.util.Pair;

public class DSType implements Serializable {

	/**
	 * 
	 * 
	 */
	public static Logger logger = Logger.getLogger(DSType.class);
	private static final long serialVersionUID = 1L;
	public final static String TYPE_LEFT = "(";
	public final static String TYPE_RIGHT = ")";
	public final static String TYPE_SEP = ">";
	public final static String UNICODE_TYPE_SEP = "\u2192"; // short right arrow

	public final static BasicType e = new BasicType("e");
	public final static BasicType t = new BasicType("t");
	public final static BasicType cn = new BasicType("cn");
	public final static BasicType es = new BasicType("es");

	public final static DSType et = new ConstructedType(e, t);
	public final static DSType eet = new ConstructedType(e, et);

	public final static String BASIC_TYPE_PATTERN = "e|es|cn|t";

	/**
	 * @param type
	 *            the basic type string e.g. "e", "t"
	 * @return a new basic type e.g. e, t
	 */
	public static BasicType create(String type) {
		return new BasicType(type.trim());
	}

	/**
	 * @param from
	 * @param to
	 * @return a new constructed type from>to e.g. e>t, (e>t)>t
	 */
	public static ConstructedType create(DSType from, DSType to) {
		return new ConstructedType(from, to);
	}

	/**
	 * @param string
	 *            a {@link String} representation e.g. "e", "e>t", "e>(e>t)" as used in lexicon specs
	 * @return a new type
	 */
	public static DSType parse(String string) {
		string = string.trim();
		if (string.contains(TYPE_SEP)) {
			Pair<String, String> fromTo = split(string);
			if (fromTo.first() == null || fromTo.second() == null)
				return null;
			if (fromTo.second().isEmpty()) {
				return new BasicType(fromTo.first());
			}
			return new ConstructedType(parse(fromTo.first()), parse(fromTo.second()));
		} else {
			// upper-case single letter - type metavariable
			if (string.matches("^" + LabelFactory.METAVARIABLE_PATTERN + "$")) {
				return MetaType.get(string);
			} /*
			 * else if (string.matches("^" + LabelFactory.VAR_PATTERN + "$")) { return new BoundTypeVariable(string); }
			 */else if (string.matches("^" + BASIC_TYPE_PATTERN + "$"))
				return new BasicType(string);
			else {
				logger.debug("string was " + string + " bad type spec");
				return null;
			}

		}
	}

	/**
	 * @param string
	 *            "X,Y" where X and/or Y may be complex e.g. "(e>t)>(e>(e>t))"
	 * @return a {@link Pair} of X and Y
	 */
	private static Pair<String, String> split(String string) {
		int n = 0;
		for (int i = 0; i < string.length(); i++) {
			String remaining = string.substring(i);
			if (remaining.startsWith(TYPE_LEFT)) {
				n++;
			} else if (remaining.startsWith(TYPE_RIGHT)) {
				n--;
			} else if (remaining.startsWith(TYPE_SEP) && (n == 0)) {
				return new Pair<String, String>(string.substring(0, i), remaining.substring(TYPE_SEP.length()));
			}
		}
		// didn't find TYPE_SEP? check for entire thing enclosed in brackets
		// e.g. "(e>t)"
		if (string.startsWith(TYPE_LEFT) && string.endsWith(TYPE_RIGHT)) {
			return split(string.substring(TYPE_LEFT.length(), string.length() - TYPE_RIGHT.length()));
		}
		// still didn't find it? give up
		return new Pair<String, String>(string, "");
	}

	/**
	 * @return an instantiated version of this {@link DSType}, with all meta-elements replaced by their values. By
	 *         default, just return this {@link DSType} unchanged. This will be overridden by {@link MetaType}s and the
	 *         like
	 */
	public DSType instantiate() {
		return this;
	}

	/**
	 * @return replacing TYPE_SEP with its Unicode version
	 */
	public String toUnicodeString() {
		return toString().replaceAll(Pattern.quote(TYPE_SEP), UNICODE_TYPE_SEP);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return toString().hashCode();
	}

	public DSType clone() {
		return parse(toString());
	}

	public DSType getFinalType() {
		return this;
	}

	public List<BasicType> getTypesSubjFirst() {
		List<BasicType> list = new ArrayList<BasicType>();

		return list;
	}

	public static void main(String a[]) {
		DSType type = DSType.parse("es>(e>(e>t))");
		System.out.println(type.getTypesSubjFirst());
	}

	public int toUniqueInt() {
		
		return 0;
	}
}
