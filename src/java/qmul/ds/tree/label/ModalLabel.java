/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.tree.label;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import qmul.ds.Context;
import qmul.ds.ContextParserTuple;
import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.boundvariable.BoundModalityVariable;
import qmul.ds.action.meta.Meta;
import qmul.ds.action.meta.MetaModality;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.tree.BasicOperator;
import qmul.ds.tree.Modality;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;

/**
 * A modal label <X>L for some modality X, some label L
 * 
 * @author mpurver
 */
public class ModalLabel extends EmbeddedLabelGroup {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Modality modality;
	private static final Logger logger=Logger.getLogger(ModalLabel.class);
	/**
	 * @param modality
	 * @param label
	 */

	public ModalLabel(Modality modality, Label label) {
		this(modality, label, null);
	}

	public ModalLabel(Modality modality, Label label, IfThenElse ite) {
		super(ite);
		this.modality = modality;
		this.labels.add(label);
		logger.debug("created modal label:" + this);
		if (label instanceof ModalLabel) {
			throw new RuntimeException("multiple label modalities");
		}
	}

	public ModalLabel(Modality modality, List<Label> labels, IfThenElse ite) {
		super(ite);
		this.modality = modality;
		this.labels = labels;
		logger.debug("created modal label:" + this);
	}

	public ModalLabel(Modality modality, List<Label> labels) {
		this(modality, labels, null);
	}

	private static final Pattern MODAL_LABEL_PATTERN = Pattern.compile("("
			+ Modality.ANY_MODALITY_PATTERN.pattern() + ")\\s*(.*)");

	/**
	 * @param string
	 *            a {@link String} representation of a modal label, as used in
	 *            lexicon specs
	 */
	public ModalLabel(String string) {
		this(string, null);
	}

	public ModalLabel(String string, IfThenElse ite) {
		super(ite);
		Matcher m = MODAL_LABEL_PATTERN.matcher(string);
		if (m.matches()) {
			this.modality = Modality.parse(m.group(1));
			Matcher m1 = EmbeddedLabelGroup.LABEL_GROUP_PATTERN.matcher(m
					.group(m.groupCount()));
			if (!m1.matches())
				this.labels.add(LabelFactory.create(m.group(m.groupCount()),
						ite));
			else {
				String labelSt = m1.group(1);
				String[] labels = labelSt.split("&");
				for (String l : labels) {
					this.labels.add(LabelFactory.create(l.trim(), ite));
				}
			}
			logger.debug("created modal label:" + this + " from string:"
					+ string);
		} else {
			throw new IllegalArgumentException(
					"unrecognised modal label string " + string);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#check(qmul.ds.tree.Tree,
	 * qmul.ds.ParserTuple)
	 */
	@Override
	public boolean checkWithTupleAsContext(Tree tree, ParserTuple context) {
		logger.debug("Checking model label " + this + " on " + tree);
		// check if the actual modal label e.g. [\/1]?Ty(t) is present here
		if (super.checkWithTupleAsContext(tree, context)) {
			return true;
		}
		// if not, check if the core label is present at the required modality
		// e.g. ?Ty(t) at [\/1]
		// first, if this is the "context" modality, check in context *only*
		if (!(modality instanceof MetaModality)
				&& (modality.getOps().size() == 1)
				&& (modality.getOps().get(0).getPath()
						.equals(BasicOperator.PATH_CONTEXT))
				&& (context instanceof ContextParserTuple)) {
			ContextParserTuple previous = ((ContextParserTuple) context)
					.getPrevious();
			// TODO option to restrict context search depth to 1,2 etc here - or
			// could be in modality path quantifier?
			while (previous != null) {
				ModalLabel contextlessLabel = new ModalLabel(modality.pop(),
						labels, this.embeddingITE);
				if (contextlessLabel.checkWithTupleAsContext(
						previous.getTree(), previous)) {
					return true;
				}
				previous = previous.getPrevious();
			}
			logger.debug("modal label check failed");
			return false;
		}
		// otherwise, check within this tree as per normal
		Node pointedNode = tree.getPointedNode();
		// check if we are testing with an uninstantiated modality metavariable
		// - if so, must only instantiate on
		// success (i.e. must uninstantiate on failure)
		boolean meta = ((modality instanceof MetaModality) && (((MetaModality) modality)
				.getValue() == null));
		for (Node node : tree.getNodes()) {

			if (modality.relates(tree, pointedNode, node)) {

				if (checkLabelsConj(node)) {
					logger.debug("Modal Label check succeeded");
					return true;
				}
			}
			// if we're here, the check failed, so uninstantiate previously
			// uninstantiated modality metavariables
			// (without losing track of backtracking history)
			if (meta) {
				((MetaModality) modality).getMeta().partialReset();
			}
		}
		logger.debug("Modal Label check failed");
		return false;
	}

	@Override
	public <E extends DAGEdge, U extends DAGTuple> boolean check(Tree tree,
			Context<U, E> context) {
		logger.debug("Checking model label " + this + " on " + tree);
		// check if the actual modal label e.g. [\/1]?Ty(t) is present here
		if (super.checkWithTupleAsContext(tree, null)) {
			return true;
		}
		
		// if not, check if the core label is present at the required modality
		// e.g. ?Ty(t) at [\/1]
		// first, if this is the "context" modality, check in context *only*
		if (!(modality instanceof MetaModality)
				&& !modality.getOps().isEmpty()&&modality.getOps().get(0).isUp()
				&& modality.getOps().get(0).getPath()
						.equals(BasicOperator.PATH_CONTEXT)) {
			U previous = context.getCurrentTuple();
			// TODO option to restrict context search depth to 1,2 etc here - or
			// could be in modality path quantifier?
			// currently searching whole trees for labels
			while (previous != null) {
				logger.debug("checking "+super.toString()+" on tree:"+previous.getTree());
				for(Node n:previous.getTree().values())
					if (checkLabelsConj(n))
						return true;
				previous = context.getParent(previous);
			}
			logger.debug("modal label check failed");
			return false;
		}
		// otherwise, check within this tree as per normal
		Node pointedNode = tree.getPointedNode();
		// check if we are testing with an uninstantiated modality metavariable
		// - if so, must only instantiate on
		// success (i.e. must uninstantiate on failure)
		boolean meta = ((modality instanceof MetaModality) && (((MetaModality) modality)
				.getValue() == null));
		for (Node node : tree.getNodes()) {

			if (modality.relates(tree, pointedNode, node)) {

				if (checkLabelsConj(node)) {
					logger.debug("Modal Label check succeeded");
					return true;
				}
			}
			// if we're here, the check failed, so uninstantiate previously
			// uninstantiated modality metavariables
			// (without losing track of backtracking history)
			if (meta) {
				((MetaModality) modality).getMeta().partialReset();
			}
		}
		logger.debug("Modal Label check failed");
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#getMetas()
	 */
	@Override
	public ArrayList<Meta<?>> getMetas() {
		ArrayList<Meta<?>> metas = super.getMetas();
		if (modality instanceof MetaModality) {
			metas.addAll(((MetaModality) modality).getMetas());
		}

		return metas;
	}

	@Override
	public ArrayList<Meta<?>> getBoundMetas() {
		ArrayList<Meta<?>> metas = super.getBoundMetas();
		if (modality instanceof BoundModalityVariable) {
			metas.addAll(((BoundModalityVariable) modality).getBoundMetas());
		}

		return metas;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.EmbeddedLabel#instantiate()
	 */
	@Override
	public Label instantiate() {
		List<Label> set = new ArrayList<Label>();
		for (Label l : labels)
			set.add(l.instantiate());
		return new ModalLabel(modality.instantiate(), set);
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
		for (Label label : labels)
			result = prime * result + ((label == null) ? 0 : label.hashCode());

		result = prime * result
				+ ((modality == null) ? 0 : modality.hashCode());
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
		ModalLabel other = (ModalLabel) obj;
		if (labels == null) {
			if (other.labels != null)
				return false;
		} else if (!labels.equals(other.labels)) {
			// should be careful here
			// failure could mean that some of the labels were instantiated.
			// Ideally we should do the set equality checking ourselves and
			// uninstatiate if neccessary
			return false;
		}
		if (modality == null) {
			if (other.modality != null)
				return false;
		} else if (!modality.equals(other.modality))
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
		if (labels.size() == 1)
			return modality.toString() + labels.iterator().next().toString();
		else
			return modality.toString() + super.toString();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#toUnicodeString()
	 */
	@Override
	public String toUnicodeString() {
		if (labels.size() == 1)
			return modality.toUnicodeString()
					+ labels.iterator().next().toUnicodeString();
		else
			return modality.toString() + super.toUnicodeString();
	}

	public static void main(String a[]) {
		ModalLabel l = (ModalLabel) LabelFactory.create("</\\L>ty(Y)", null);
		logger.debug(l);
	}

}