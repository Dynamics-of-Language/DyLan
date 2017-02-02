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

import qmul.ds.action.meta.Meta;

/**
 * A constructed type e.g. e>t, (e>t)>t
 * 
 * @author mpurver
 */
public class ConstructedType extends DSType {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DSType from;
	private DSType to;

	/**
	 * A new constructed type from>to e.g. e>t, (e>t)>t
	 * 
	 * @param from
	 * @param to
	 */
	protected ConstructedType(DSType from, DSType to) {
		this.from = from;
		this.to = to;
	}

	/**
	 * @return the from type
	 */
	public DSType getFrom() {
		return from;
	}

	/**
	 * @return the to type
	 */
	public DSType getTo() {
		return to;
	}

	public DSType instantiate() {
		return new ConstructedType(from.instantiate(), to.instantiate());
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
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
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
		ConstructedType other = (ConstructedType) obj;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
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
		return (from instanceof ConstructedType ? "(" + from + ")" : from) + TYPE_SEP
				+ (to instanceof ConstructedType ? "(" + to + ")" : to);
	}

	public DSType getFinalType() {
		return to.getFinalType();
	}

	public List<BasicType> getTypesSubjFirst() {
		List<BasicType> list = new ArrayList<BasicType>();
		list.addAll(this.to.getTypesSubjFirst());
		list.addAll(this.from.getTypesSubjFirst());
		return list;
	}
	
	@Override
	public int toUniqueInt()
	{
		return from.toUniqueInt()+to.toUniqueInt();
	}
	
	public ArrayList<Meta<?>> getMetas()
	{
		ArrayList<Meta<?>> metas=new ArrayList<Meta<?>>();
		if (from==null||to==null)
		{
			throw new NullPointerException("Either from or to are null in this constructed DS type:"+this);
		}
		metas.addAll(from.getMetas());
		metas.addAll(to.getMetas());
		return metas;
		
		
	}

}
