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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A LOFT tree modality basic operator down-0, up etc
 * 
 * @author mpurver
 */
public class BasicOperator implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String ARROW_UP = "/\\";
	public static final String ARROW_DOWN = "\\/";
	public static final String UNICODE_ARROW_UP = "\u2191";
	public static final String UNICODE_ARROW_DOWN = "\u2193";

	public static final String PATH_EMPTY = "";
	public static final String PATH_0 = "0";
	public static final String PATH_1 = "1";
	public static final String PATH_LINK = "L";
	public static final String PATH_UNFIXED = "*";
	public static final String PATH_LOCAL_UNFIXED = "U";
	public static final String PATH_CONTEXT = "C";
	public static final String UNICODE_PATH_0 = "\u2080";
	public static final String UNICODE_PATH_1 = "\u2081";
	public static final String UNICODE_PATH_LOCAL_UNFIXED = "\u03BB";

	public static final BasicOperator UP = new BasicOperator(ARROW_UP, PATH_EMPTY);
	public static final BasicOperator UP_0 = new BasicOperator(ARROW_UP, PATH_0);
	public static final BasicOperator UP_1 = new BasicOperator(ARROW_UP, PATH_1);
	public static final BasicOperator UP_STAR = new BasicOperator(ARROW_UP, PATH_UNFIXED);
	public static final BasicOperator DOWN_0 = new BasicOperator(ARROW_DOWN, PATH_0);
	public static final BasicOperator DOWN_1 = new BasicOperator(ARROW_DOWN, PATH_1);
	public static final BasicOperator DOWN_STAR = new BasicOperator(ARROW_DOWN, PATH_UNFIXED);
	public static final BasicOperator DOWN_LOCAL_UNFIXED = new BasicOperator(ARROW_DOWN, PATH_LOCAL_UNFIXED);
	public static final BasicOperator UP_LOCAL_UNFIXED = new BasicOperator(ARROW_UP, PATH_LOCAL_UNFIXED);
	private String direction;
	private String path;

	/**
	 * @param direction
	 * @param path
	 */
	public BasicOperator(String direction, String path) {
		this.direction = direction;
		this.path = path;
	}

	public static final Pattern OP_PATTERN = Pattern.compile("(/\\\\|\\\\/)([01L\\*UC]*)");

	/**
	 * @param string
	 *            a {@link String} representation of an operator e.g. /\1 as used in lexicon specs
	 */
	public BasicOperator(String string) {
		Matcher m = OP_PATTERN.matcher(string);
		if (m.matches()) {
			direction = m.group(1);
			path = m.group(2);
		} else {
			throw new IllegalArgumentException("unrecognised operator string " + string);
		}
	}

	/**
	 * @param string
	 *            a {@link String} representation of many operators e.g. \/0/\1 as used in lexicon specs
	 * @return a list of {@link BasicOperator}s
	 */
	public static ArrayList<BasicOperator> create(String string) {
		ArrayList<BasicOperator> ops = new ArrayList<BasicOperator>();
		Matcher m = OP_PATTERN.matcher(string);
		while (m.find()) {
			ops.add(new BasicOperator(m.group(0)));
		}
		return ops;
	}

	/**
	 * @return the direction
	 */
	public String getDirection() {
		return direction;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return false if the path is a Kleene star, true otherwise
	 */
	public boolean isFixed() {
		return !(path.equals(PATH_UNFIXED) || path.equals(PATH_LOCAL_UNFIXED));
	}

	/**
	 * @return true if the path is a LINK relation, false otherwise
	 */
	public boolean isLink() {
		return path.equals(PATH_LINK);
	}

	/**
	 * @return true if direction is up
	 */
	public boolean isUp() {
		return direction.equals(ARROW_UP);
	}

	/**
	 * @return true if direction is down
	 */
	public boolean isDown() {
		return direction.equals(ARROW_DOWN);
	}

	/**
	 * @return the same operator in the inverse direction
	 */
	public BasicOperator inverse() {
		return new BasicOperator(isUp() ? ARROW_DOWN : ARROW_UP, path);
	}

	public boolean isU() {
		return path.equals(PATH_LOCAL_UNFIXED);
	}

	public boolean isStar() {
		return path.equals(PATH_UNFIXED);
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
		result = prime * result + ((direction == null) ? 0 : direction.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
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
		BasicOperator other = (BasicOperator) obj;
		if (direction == null) {
			if (other.direction != null)
				return false;
		} else if (!direction.equals(other.direction))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
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
		return direction + path;
	}

	/**
	 * @return replacing \/ arrows with their Unicode versions
	 */
	public String toUnicodeString() {
		return toString().replaceAll(Pattern.quote(ARROW_UP), UNICODE_ARROW_UP)
				.replaceAll(Pattern.quote(ARROW_DOWN), UNICODE_ARROW_DOWN)
				.replaceAll(Pattern.quote(PATH_0), UNICODE_PATH_0).replaceAll(Pattern.quote(PATH_1), UNICODE_PATH_1);
	}

}
