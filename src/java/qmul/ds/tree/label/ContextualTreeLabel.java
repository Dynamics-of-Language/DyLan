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
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.ContextParserTuple;
import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;

/**
 * A contextual label context(X)
 * 
 * @author mpurver
 */
public class ContextualTreeLabel extends EmbeddedLabelGroup {

	// private String type;//action or node

	public static final String FUNCTOR = "context_tree";

	/**
	 * @param type
	 * @param label
	 */
	public ContextualTreeLabel(List<Label> labels, IfThenElse ite) {
		super(ite);
		this.labels = labels;
	}

	private static final Pattern CONTEXT_LABEL_PATTERN = Pattern.compile(FUNCTOR
			+ EmbeddedLabelGroup.LABEL_GROUP_PATTERN.pattern());

	/**
	 * @param string
	 *            a {@link String} representation of a contextual label, as used in lexicon specs
	 */
	public ContextualTreeLabel(String string, IfThenElse ite) {
		super(ite);
		Matcher m = CONTEXT_LABEL_PATTERN.matcher(string);
		if (m.matches()) {
			String labelSt = m.group(1);
			String[] labels = labelSt.split("&");
			for (String l : labels) {
				this.labels.add(LabelFactory.create(l.trim(), ite));
			}

		} else
			throw new IllegalArgumentException("unrecognised contextual label string " + string);

	}

	public ContextualTreeLabel(String string) {
		this(string, null);
	}

	public ContextualTreeLabel(List<Label> labels) {
		this(labels, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#check(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	@Override
	public boolean checkWithTupleAsContext(Tree tree, ParserTuple contextTuple) {

		if (super.checkWithTupleAsContext(tree, contextTuple)) {
			return true;
		}
		ContextParserTuple context = ((ContextParserTuple) contextTuple).getPrevious();
		// ContextParserTuple context = (ContextParserTuple) contextTuple;
		while (context != null) {

			Tree contextTree = context.getTree();

			Collection<Node> nodes = contextTree.getNodes();
			for (Node n : nodes) {
				if (super.checkLabelsConj(n)) {
					return true;
				}
			}

			context = context.getPrevious();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.EmbeddedLabel#instantiate()
	 */
	@Override
	public Label instantiate() {
		List<Label> labels = new ArrayList<Label>();
		for (Label l : labels)
			labels.add(l.instantiate());
		return new ContextualTreeLabel(labels);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */

	/*
	 * @Override public int hashCode() { final int prime = 31; int result = 1; result = prime * result + ((type == null)
	 * ? 0 : type.hashCode()); return result; }
	 */

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
		ContextualTreeLabel other = (ContextualTreeLabel) obj;
		if (other.getLabels().size() != this.getLabels().size())
			return false;

		return this.getLabels().equals(other.getLabels());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR + super.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#toUnicodeString()
	 */
	@Override
	public String toUnicodeString() {
		return FUNCTOR + super.toUnicodeString();
	}

	public static void main(String a[]) {
		ContextualTreeLabel cl = new ContextualTreeLabel(FUNCTOR + "(ty(e), fo(X))", null);

		logger.debug(cl.toString());

	}

}
