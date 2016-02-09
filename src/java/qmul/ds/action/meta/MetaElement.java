/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action.meta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import qmul.ds.tree.label.Label;
import qmul.ds.type.DSType;

/**
 * A {@link Label}, {@link DSType} etc. metavariable as used in rule specs e.g. X, ?Y
 * 
 * @author mpurver
 */
public class MetaElement<X> implements Serializable, Meta<X> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(MetaElement.class);
	private static final String BOUND_META_NAME = "META";
	private String name;
	private X value;
	public HashSet<String> backtrack;
	private X last;
	private Class<X> cls;

	/**
	 * @param name
	 *            a String name e.g. "X", "Y"
	 */
	private MetaElement(String name, Class<X> cls) {
		this.name = name;
		this.value = null;
		this.backtrack = new HashSet<String>();
		this.last = null;
		this.cls = cls;
	}

	protected Class<X> getCls() {
		return cls;
	}

	protected static HashMap<String, MetaElement<?>> pool = new HashMap<String, MetaElement<?>>();

	/**
	 * @param name
	 * @return the existing metavariable of this name (with its value), a new one otherwise
	 */
	@SuppressWarnings("unchecked")
	public static <Y> MetaElement<Y> get(String name, Class<Y> cls) {

		String key = cls.toString() + name;
		if (!pool.containsKey(key)) {
			pool.put(key, new MetaElement<Y>(name, cls));
		}
		return (MetaElement<Y>) pool.get(key);
	}

	public static <Y> MetaElement<Y> getBoundMeta(Class<Y> cls) {

		String key = cls.toString() + BOUND_META_NAME;
		if (!pool.containsKey(key)) {
			pool.put(key, new MetaElement<Y>(BOUND_META_NAME, cls));
		}
		return (MetaElement<Y>) pool.get(key);
	}

	/**
	 * this will return the metaElements associated with the given name, i.e. those whose keys in the pool end with that
	 * name.
	 * 
	 * @param name
	 * @return
	 */
	public static Collection<MetaElement<?>> get(String name) {
		Collection<MetaElement<?>> result = new ArrayList<MetaElement<?>>();
		for (String key : pool.keySet()) {
			if (key.endsWith(name))
				result.add(pool.get(key));
		}
		return result;

	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the value
	 */
	public X getValue() {
		return value;
	}

	/**
	 * @param value
	 */
	protected void setValue(X value) {
		this.value = value;
	}

	/**
	 * Un-instantiate completely
	 */
	public void reset() {
		value = null;
		backtrack.clear();
		last = null;
	}

	/**
	 * Un-instantiate value, but don't forget backtracking history
	 */
	public void partialReset() {
		value = null;
	}

	/**
	 * Un-instantiate the instantiated value, remembering it to prevent instantiation to this value again
	 * 
	 * @return true if it can be backtracked (i.e. is instantiated)
	 */
	public boolean backtrack() {
		// can't backtrack if not instantiated
		if ((value == null) || backtrack.contains(value.toString())) {
			return false;
		}
		backtrack.add(value.toString());
		last = value;
		value = null;
		logger.trace("Backtracked from " + last + " to " + this);
		return true;
	}

	/**
	 * Put things back the way they were before trying (unsuccessfully) to backtrack
	 */
	public void unbacktrack() {
		value = last;
		backtrack.remove(last.toString());
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
		if (obj == null) {
			// logger.debug("obj is null");
			return false;
		}
		if (!cls.isInstance(obj))
			return false;
		// SIDE-EFFECT: checking equality sets metavariable value! (no hashCode)
		X other = (X) obj;
		if (value == null) {
			// logger.debug(backtrack);
			if (backtrack.contains(other.toString())) {
				// logger.debug("Can't inst MetaEl, already used " + other);
				return false;
			}
			value = other;
			// logger.debug("Inst MetaEl for " + cls + " value=" + value);
		}
		return value.equals(other);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return name + ((value==null)?"":"=" + value + "[" + hashCode() + "]");

	}

	public String toDebugString()
	{
		return name + "=" + value + "[" + hashCode() + "]";
	}
	
	public static void resetPool() {
		pool.clear();
	}

	public static void removeFromPool(String metaName) {
		Set<String> keysToRemove = new TreeSet<String>();
		for (String key : pool.keySet()) {
			if (key.endsWith(metaName))
				keysToRemove.add(key);
		}
		for (String key : keysToRemove)
			pool.remove(key);

	}

	public static void resetBoundMetas() {

		for (String key : pool.keySet()) {
			if (key.endsWith(BOUND_META_NAME))
				pool.get(key).reset();
		}

	}

}
