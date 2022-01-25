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
import java.util.Collection;

import qmul.ds.action.Action;
import qmul.ds.formula.Formula;
import qmul.ds.formula.ttr.TTRFormula;
import qmul.ds.formula.ttr.TTRRecordType;
import qmul.ds.tree.Tree;
import edu.stanford.nlp.util.Pair;

/**
 * A parser tuple (member of a {@link ParseState}). At its simplest, just a {@link Tree}, but more complex
 * {@link Parser}s may add context information, probabilities etc
 * 
 * @author mpurver
 */
public class TTRParserTuple extends ParserTuple {

	

	protected TTRFormula semantics;

	public TTRFormula getSemantics() {
		if (this.semantics != null)
			return semantics;

		semantics = tree.getMaximalSemantics();
		return semantics;
	}

	public void setMaximalSemantics(TTRFormula maximalSemantics) {
		this.semantics = maximalSemantics;
	}

	public TTRParserTuple(Tree t, TTRFormula f) {
		super(t);
		this.semantics = f;
	}

	/**
	 * A new tuple containing an AXIOM tree
	 */
	public TTRParserTuple() {
		super();
	}

	/**
	 * A new tuple containing a cloned copy of the given tree
	 * 
	 * @param tree
	 */
	public TTRParserTuple(Tree tree) {
		super(tree);
	}

	/**
	 * A new tuple containing a cloned copy of the given tree
	 * 
	 * @param tree
	 */
	public TTRParserTuple(TTRParserTuple tuple) {
		this.tree = (tuple.tree==null)?null:new Tree(tuple.tree);
		this.semantics=tuple.semantics==null?null:tuple.semantics.clone();
	}

	public TTRParserTuple(TTRFormula ttrRecordType) {
		this.semantics=ttrRecordType;
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public TTRParserTuple clone() {
		return new TTRParserTuple(this);
	}

	


	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s= (tree==null)?"Tree:null":tree.toString();
		s+="\nSem:"+(getSemantics()==null?"null":getSemantics());
		return s;
	}

	public boolean subsumes(TTRParserTuple t)
	{
		if (getSemantics()!=null)
		{
			if (t.getSemantics()==null)
				return false;
			return getSemantics().subsumes(t.getSemantics());
		}
		if (this.tree==null)
		{
			if (t.tree==null)
				return true;
			return false;
		}
		
		return tree.subsumes(t.tree);
	}
}
		
