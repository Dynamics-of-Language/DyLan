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

import org.apache.log4j.Logger;

import qmul.ds.Context;
import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.meta.MetaElement;
import qmul.ds.action.meta.MetaLabel;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;

/**
 * A {@link Node} label
 * 
 * @author mpurver
 */
public abstract class Label implements Comparable<Label>, Serializable {

	protected static Logger logger = Logger.getLogger(Label.class);

	protected transient IfThenElse embeddingITE = null;

	protected void partialResetMetas(ArrayList<MetaElement<?>> uninstantiated) {
		for (MetaElement<?> meta : uninstantiated) {
			meta.partialReset();
		}

	}

	public Label(IfThenElse ite) {
		this.embeddingITE = ite;
	}

	public Label() {

	}

	protected ArrayList<MetaElement<?>> getUninstantiatedMetas() {
		ArrayList<MetaElement<?>> metas = new ArrayList<MetaElement<?>>();
		for (MetaElement<?> meta : getMetas()) {
			if (meta.getValue() == null)
				metas.add(meta);
		}
		return metas;
	}

	/**
	 * @param tree
	 * @param context
	 *            (can be null)
	 * @return true if the pointed node is decorated with this label
	 */
	public boolean checkWithTupleAsContext(Tree tree, ParserTuple context) {
		return check(tree.getPointedNode());
	}


	/**
	 * @param tree
	 * @param context (now a DAG). By default, check with null context, using old tuple context methods.
	 *            
	 * @return true if the pointed node is decorated with this label
	 */
	public <E extends DAGEdge, U extends DAGTuple> boolean check(Tree t, Context<U,E> context)
	{
		return checkWithTupleAsContext(t, null);
		
	}
	
	
	
	
	/**
	 * @param node
	 * @return true if the node is decorated with this label
	 */
	public boolean check(Node node) {
		return node.hasLabel(this);
	}

	/**
	 * @param other
	 * @return true if this {@link Label} subsumes other; by default, only if they are equal, but this can be overridden
	 *         by e.g. {@link Requirement}
	 */
	public boolean subsumes(Label other) {
		if (this.toString().equals("?Ex.Tn(x)")) {
			// TODO how does a negated label mean subsumption?
			logger.debug("EXISTENTIAL requirement label, ok for subsumtion??");
			return true;
		}
		return this.toString().equals(other.toString());
	}

	/**
	 * @return an instantiated version of this {@link Label}, with all meta-elements replaced by their values. By
	 *         default, just return this {@link Label} unchanged. This will be overridden by {@link MetaLabel}s and the
	 *         like
	 */
	public Label instantiate() {
		return this;
	}

	/**
	 * @return in order, the {@link MetaElement}s contained within this {@link Label}. By default, none; this should be
	 *         overridden by {@link MetaLabel}s and the like.
	 */
	public ArrayList<MetaElement<?>> getMetas() {
		return new ArrayList<MetaElement<?>>();
	}

	public ArrayList<MetaElement<?>> getBoundMetas() {
		return new ArrayList<MetaElement<?>>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Label arg0) {

		if (equals(arg0) || arg0.equals(this)) {
			logger.debug(this + " equals " + arg0);
			return 0;
		}
		logger.debug(this + " notequals " + arg0 + " by " + (this.hashCode() - arg0.hashCode()));
		return this.hashCode() - arg0.hashCode();
	}

	/**
	 * @return by default, the ordinary string - to be overridden by labels who use characters which have nicer Unicode
	 *         equivalents
	 */
	public String toUnicodeString() {
		return toString();
	}

	public void resetMetas() {
		for (MetaElement<?> meta : getMetas()) {
			meta.reset();

		}
	}

}
