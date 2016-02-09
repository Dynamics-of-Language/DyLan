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
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.LabelFactory;

/**
 * The <tt>abort</tt> action
 * 
 * @author mpurver
 */
public class SetAccPointer extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String FUNCTOR = "assert";

	public <E extends DAGEdge, U extends DAGTuple, T extends Tree> T exec(T tree, Context<U,E> context)
	{
		logger.info("setting acc pointer");
		
		
		context.setAcceptancePointer();
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
		throw new UnsupportedOperationException();
	}

	public static void main(String a[])
	{
		Label l=LabelFactory.create("accept(arash)");
		
		System.out.println(l+":"+l.getClass());
	}
}
