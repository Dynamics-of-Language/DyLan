/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action.atomic;

import java.io.Serializable;

import org.apache.log4j.Logger;

import qmul.ds.Context;
import qmul.ds.ParserTuple;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.tree.Tree;

/**
 * An effect (atomic action) on a {@link Tree}
 * 
 * @author mpurver
 */
public abstract class Effect implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static Logger logger = Logger.getLogger(Effect.class);

	public abstract <T extends Tree> T execTupleContext(T tree, ParserTuple context);

	public abstract Effect instantiate();

	public <E extends DAGEdge, U extends DAGTuple, T extends Tree> T exec(T tree, Context<U,E> context)
	{
		//by default assume null context
		return execTupleContext(tree, null);
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
		result = prime * result + toString().hashCode();
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		else if (this.getClass() != o.getClass())
			return false;
		return this.toString().equals(o.toString());
	}
}
