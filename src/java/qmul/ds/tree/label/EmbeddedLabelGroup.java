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
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.meta.Meta;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;

/**
 * A label which embeds a group of labels to be checked together somewhere, e.g. a modal label <X>(L1, L2, L3, . . )
 * 
 * @author arash
 */

public abstract class EmbeddedLabelGroup extends Label implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected List<Label> labels;

	public static final Pattern LABEL_GROUP_PATTERN = Pattern.compile("\\((.+)\\)");

	protected EmbeddedLabelGroup(IfThenElse ite) {
		super(ite);
		labels = new ArrayList<Label>();
	}

	/**
	 * @param label
	 */
	protected EmbeddedLabelGroup(List<Label> labels) {
		this(labels, null);
	}

	protected EmbeddedLabelGroup(Set<Label> labels) {
		this(labels, null);
	}
	protected EmbeddedLabelGroup(List<Label> labels, IfThenElse ite) {
		super(ite);
		this.labels = labels;
	}

	public EmbeddedLabelGroup(Set<Label> disjunct, IfThenElse ite) {
		super(ite);
		this.labels=new ArrayList<Label>();
		for(Label l:disjunct)
			this.labels.add(l);
		
		
	}

	/**
	 * @return the label
	 */
	public List<Label> getLabels() {
		return labels;
	}

	/**
	 * @param label
	 *            the label to set
	 */
	protected void setLabel(List<Label> labels) {
		this.labels = labels;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#getMetas()
	 */
	@Override
	public ArrayList<Meta<?>> getMetas() {
		ArrayList<Meta<?>> metas = super.getMetas();
		for (Label l : labels)
			metas.addAll(l.getMetas());

		return metas;
	}

	@Override
	public ArrayList<Meta<?>> getBoundMetas() {
		ArrayList<Meta<?>> metas = super.getBoundMetas();
		for (Label l : labels)
			metas.addAll(l.getBoundMetas());

		return metas;
	}

	/**
	 * @param node
	 * @return true if the node is decorated with all the labels in this group
	 */
	protected boolean checkLabelsConj(Node node) {

		ArrayList<Meta<?>> uninstantiatedBeforeCheck = getUninstantiatedMetas();

		for (Label l : labels) {

			if (!l.check(node)) {
				partialResetMetas(uninstantiatedBeforeCheck);
				//System.out.println("Label "+l+"failed"+"on"+node);
				return false;
			}
		}
		return true;
	}

	/**
	 * @param node
	 * @return true if the node is decorated with one of the labels in this group
	 */
	protected boolean checkLabelsDisj(Node node) {

		ArrayList<Meta<?>> uninstantiatedBeforeCheck = getUninstantiatedMetas();

		for (Label l : labels) {

			if (l.check(node))
				return true;

		}
		partialResetMetas(uninstantiatedBeforeCheck);
		return false;
	}

	protected boolean checkLabelsConj(Tree t, ParserTuple context) {
		ArrayList<Meta<?>> uninstantiatedBeforeCheck = getUninstantiatedMetas();

		for (Label l : labels) {
			if (!l.checkWithTupleAsContext(t, context)) {
				partialResetMetas(uninstantiatedBeforeCheck);
				return false;
			}
		}
		return true;

	}

	protected boolean checkLabelsDisj(Tree t, ParserTuple context) {
		ArrayList<Meta<?>> uninstantiatedBeforeCheck = getUninstantiatedMetas();

		for (Label l : labels) {
			if (l.checkWithTupleAsContext(t, context))
				return true;
		}
		partialResetMetas(uninstantiatedBeforeCheck);
		return false;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#instantiate()
	 */
	@Override
	public abstract Label instantiate();

	public String toString() {
		String result = "(";
		for (Label l : labels)
			result += l.toString() + "& ";

		result = result.substring(0, result.length() - 2) + ")";
		return result;
	}

	public String toUnicodeString() {
		String result = "(";
		for (Label l : labels)
			result += l.toUnicodeString() + "& ";

		result = result.substring(0, result.length() - 2) + ")";
		return result;
	}
	
	
	public static void main(String[] args)
	{
		Label l=LabelFactory.create("</\\0\\/1>(ty(e>t) || ?ty(e>t))");
		System.out.println(l);
		
	}

}
