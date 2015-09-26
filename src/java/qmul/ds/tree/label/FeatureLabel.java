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

import qmul.ds.action.atomic.IfThenElse;

/**
 * A feature label e.g. +Q
 * 
 * @author mpurver
 */
public class FeatureLabel extends Label implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "+";

	private String feature;

	/**
	 * @param feature
	 */

	public FeatureLabel(String feature) {
		this(feature, null);
	}

	public FeatureLabel(String feature, IfThenElse ite) {
		super(ite);
		this.feature = feature;
	}

	/**
	 * @return the feature
	 */
	public String getFeature() {
		return feature;
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
		result = prime * result + ((feature == null) ? 0 : feature.hashCode());
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
		FeatureLabel other = (FeatureLabel) obj;
		if (feature == null) {
			if (other.feature != null)
				return false;
		} else if (!feature.equals(other.feature))
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
		return PREFIX + feature;
	}

}
