/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.type;

import java.util.ArrayList;
import java.util.List;

/**
 * A basic type e.g. t, e, es, cn
 * 
 * @author mpurver
 */
public class BasicType extends DSType {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String type;

	/**
	 * @param type
	 *            a String representing type e.g. "t", "e", "cn"
	 */
	protected BasicType(String type) {
		this.type = type;
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
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		BasicType other = (BasicType) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
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
		return type;
	}

	public List<BasicType> getTypesSubjFirst() {
		List<BasicType> list = new ArrayList<BasicType>();
		list.add(this);
		return list;
	}
	
	@Override
	public int toUniqueInt()
	{
		return type.hashCode();
		
	}

}
