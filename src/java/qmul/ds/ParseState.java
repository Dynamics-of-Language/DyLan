/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds;

import java.util.Collection;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.tree.Tree;

/**
 * A parse state: a set of {@link ParserTuple}s. Members should be ordered (by their natural ordering) best-first
 * 
 * @author mpurver
 * 
 * @param <T>
 */
@SuppressWarnings("serial")
public class ParseState<T extends ParserTuple> extends TreeSet<T> implements Cloneable {

	private static Logger logger = Logger.getLogger(ParseState.class);
	//private static boolean match = false;

	public ParseState() {
		super();
	}

	public ParseState(Collection<T> c) {
		super(c);
	}

	/**
	 * @return true if this state contains at least one complete {@link Tree}
	 */
	public boolean hasCompleteTree() {
		for (T tuple : this) {
			if (tuple.getTree().isComplete()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the subset of this state containing only the complete {@link Tree}s
	 */
	public ParseState<T> complete() {
		ParseState<T> s = new ParseState<T>();
		for (T tuple : this) {
			if (tuple.getTree().isComplete()) {
				s.add(tuple);
			}
		}
		return s;
	}

	/**
	 * @param goal
	 * @return the subset of this state containing only those {@link Tree}s which subsume goal (either a tree or a record type)
	 */
	public ParseState<T> subsumes(ParserTuple goal) {
		ParseState<T> s = new ParseState<T>();
		for (T tuple : this) {
			logger.debug("Check subsume: " + tuple + " subsumes " + goal);
			if (tuple.subsumes(goal)) {
				s.add(tuple);
			} else {
				logger.debug("subsume fail");
			}
		}
		return s;
	}

	

	/* 
	 * 
	 * /* (non-Javadoc)
	 * 
	 * @see java.util.HashSet#clone()
	 */
	public ParseState<T> clone() {
		return new ParseState<T>(this);
	}

}
