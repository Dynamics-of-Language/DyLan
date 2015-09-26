/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action.atomic;

import qmul.ds.Context;
import qmul.ds.ParserTuple;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.tree.Tree;

/**
 * The <tt>abort</tt> action
 * 
 * @author mpurver
 */
public class GroundToRoot extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String FUNCTOR = "ground";

	public <E extends DAGEdge, U extends DAGTuple, T extends Tree> T exec(T tree, Context<U,E> context)
	{
		
		context.groundToRoot();
		return tree;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR;
	}

	public Effect instantiate() {
		return this;
	}

	@Override
	public <T extends Tree> T execTupleContext(T tree, ParserTuple context) {
		// TODO Auto-generated method stub
		return null;
	}

}
