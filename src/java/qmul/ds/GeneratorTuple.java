/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.tree.Tree;

/**
 * A generator tuple (member of a generator state) - a pair of partial string and related parser state.
 * 
 * @author mpurver
 * 
 * @param <T>
 */
public class GeneratorTuple<T extends ParserTuple> implements Comparable<GeneratorTuple<T>>, Cloneable {

	private static Logger logger = Logger.getLogger(GeneratorTuple.class);

	private ArrayList<String> string;
	private ParseState<T> state;

	public GeneratorTuple(ParseState<T> state) {
		this.string = new ArrayList<String>();
		this.state = state.clone();
	}

	public GeneratorTuple(List<String> string, ParseState<T> state) {
		this.string = new ArrayList<String>(string);
		this.state = state.clone();
	}

	/**
	 * @return the string
	 */
	public ArrayList<String> getString() {
		return string;
	}

	public void setString(ArrayList<String> strings) {
		string = strings;
	}

	/**
	 * @return the parse state
	 */
	public ParseState<T> getParseState() {
		return state;
	}

	public void setParseState(ParseState<T> pstate) {
		state = pstate;
	}

	/**
	 * @return true if the associated parse state has at least one complete {@link Tree}
	 */
	public boolean hasCompleteTree() {
		return state.hasCompleteTree();
	}

	/**
	 * @return the tuple with the same string and a state which is the subset of the current state containing only
	 *         complete {@link Tree}s
	 */
	public GeneratorTuple<T> complete() {
		return new GeneratorTuple<T>(string, state.complete());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public GeneratorTuple<T> clone() {
		return new GeneratorTuple<T>(new ArrayList<String>(string), state.clone());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(GeneratorTuple<T> other) {
		if (this.hasCompleteTree()) {
			if (other.hasCompleteTree()) {
				return (other.hashCode() - this.hashCode());
			} else {
				return -1;
			}
		} else {
			if (other.hasCompleteTree()) {
				return 1;
			} else {
				return (other.hashCode() - this.hashCode());
			}
		}
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
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result + ((string == null) ? 0 : string.hashCode());
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
		GeneratorTuple<T> other = (GeneratorTuple<T>) obj;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		if (string == null) {
			if (other.string != null)
				return false;
		} else if (!string.equals(other.string))
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
		return string + ":" + state.size();
	}

}
