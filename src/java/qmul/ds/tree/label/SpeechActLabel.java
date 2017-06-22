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

import org.apache.log4j.Logger;

import qmul.ds.Context;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.meta.Meta;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.formula.SpeechAct;
import qmul.ds.tree.Tree;

/**
 * Speech Act Label. Consists of speech act force, and (optional) arguments.
 * 
 * e.g. sa:[force:info|color:red|shape:square]
 * 
 * Represented internally as a record type with meaningful, predefined labels, viz.
 * 
 * [act:info|color:red]
 * 
 * @author arash
 */
public class SpeechActLabel extends Label implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(SpeechActLabel.class);
	

	public final static String FUNCTOR="sa:";
	//public final static String SPEECH_ACT_LABEL_PATTERN = FUNCTOR+"\\*("+SpeechAct.SPEECH_ACT_PATTERN+")";
	private SpeechAct sa;
	
	
	public SpeechActLabel(String s, IfThenElse ite)
	{
		super(ite);
		
		
		
		if (s.startsWith(FUNCTOR))
		{
			this.sa=new SpeechAct(s.substring(FUNCTOR.length(), s.length()));
			
		}
		else
			throw new IllegalArgumentException("Bad SpeechActLabel string: "+s);
		
		
		
	}

	/**
	 * @param formula
	 */
	public SpeechActLabel(SpeechAct sa, IfThenElse ite) {
		super(ite);
		this.sa = sa;
	}

	public SpeechActLabel(SpeechAct sa) {
		this(sa, null);
	}

	public SpeechActLabel(SpeechActLabel sal) {
		this(sal.sa.clone(), null);
	}

	/**
	 * @return the formula
	 */
	public SpeechAct getSpeechAct() {
		return sa;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#getMetas()
	 */
	@Override
	public ArrayList<Meta<?>> getMetas() {
		return sa.getMetas();
	}

	
	
	/**
	 * @param tree
	 * @param context (now a DAG). By default, check with null context, using old tuple context methods.
	 *            
	 * @return true if the pointed node is decorated with this label
	 */
	public <E extends DAGEdge, U extends DAGTuple> boolean check(Tree t, Context<U,E> context)
	{
		return this.check(t.getPointedNode());
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#instantiate()
	 */
	@Override
	public SpeechActLabel instantiate() {
		return new SpeechActLabel(sa.instantiate().evaluate());
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
		result = prime * result + ((sa == null) ? 0 : sa.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// logger.debug("testing formula equality");
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SpeechActLabel other = (SpeechActLabel) obj;
		if (sa == null) {
			if (other.sa != null)
				return false;
		} else if (!sa.equals(other.sa)) {
			logger.debug("This formula: " + sa + " deemed unequal to:" + other.sa);
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
		return FUNCTOR + sa.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#toUnicodeString()
	 */
	@Override
	public String toUnicodeString() {
		return FUNCTOR + sa.toUnicodeString();
	}

	public boolean subsumes(Label l) {
		if (!(l instanceof SpeechActLabel))
			return false;

		SpeechActLabel la = (SpeechActLabel) l;
		return this.sa.subsumes(la.sa);
	}

	
	

	public SpeechActLabel clone() {
		return new SpeechActLabel(this);
	}
	
	public static void main(String a[])
	{
		SpeechActLabel sal=new SpeechActLabel("sa:info(color:Q)", null);
		
		System.out.println(sal);
	}

}
