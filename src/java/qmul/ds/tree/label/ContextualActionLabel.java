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
import java.util.ListIterator;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.ContextParserTuple;
import qmul.ds.ParserTuple;
import qmul.ds.action.Action;
import qmul.ds.action.ActionSequence;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.boundvariable.BoundActionSequenceVariable;
import qmul.ds.action.meta.Meta;
import qmul.ds.action.meta.MetaActionSequence;
import qmul.ds.tree.Tree;

public class ContextualActionLabel extends Label {

	private TreeSet<Label> requiredTriggers = new TreeSet<Label>();
	public static final String FUNCTOR = "triggered_by";
	private ActionSequence actionSequence;

	private static final Pattern CONTEXTUAL_META_ACTION_LABEL_PATTERN = Pattern.compile(FUNCTOR + "\\(\\s*("
			+ LabelFactory.METAVARIABLE_PATTERN + ")\\s*,\\s*(.+)\\)");

	private static final Pattern CONTEXTUAL_ACTION_VARIABLE_PATTERN = Pattern.compile(FUNCTOR + "\\(\\s*("
			+ LabelFactory.VAR_PATTERN + ")\\s*,\\s*(.+)\\)");

	public ContextualActionLabel(String string, IfThenElse ite) {
		super(ite);
		logger.debug("got string for actionseqstuff:" + string);
		Matcher m = CONTEXTUAL_META_ACTION_LABEL_PATTERN.matcher(string);
		if (m.matches()) {
			logger.debug("first group:" + m.group(1));
			logger.debug("second group:" + m.group(2));
			this.actionSequence = MetaActionSequence.get(m.group(1));
			this.requiredTriggers.add(LabelFactory.create(m.group(2).trim(), ite));

		} else {
			m = CONTEXTUAL_ACTION_VARIABLE_PATTERN.matcher(string);
			if (m.matches()) {
				this.actionSequence = new BoundActionSequenceVariable(m.group(1));
				this.requiredTriggers.add(LabelFactory.create(m.group(2).trim(), ite));

			} else
				throw new IllegalArgumentException("unrecognised contextual action label string " + string);

		}

	}

	/**
	 * @param string
	 *            a {@link String} representation of a contextual label, as used in lexicon specs
	 */
	public ContextualActionLabel(String string) {
		this(string, null);
	}

	public ContextualActionLabel(TreeSet<Label> labels, ActionSequence instantiate) {
		this.requiredTriggers = labels;
		this.actionSequence = instantiate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#check(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	@Override
	public boolean checkWithTupleAsContext(Tree tree, ParserTuple contextTuple) {

		ContextParserTuple context = ((ContextParserTuple) contextTuple).getPrevious();
		while (context != null) {

			ListIterator<Action> i = context.getActionsByRecency();

			ActionSequence subsequence = new ActionSequence();
			boolean success = false;
			WHILE: while (i.hasPrevious()) {
				Action a = i.previous();

				logger.debug("Checking triggers for Action:" + a);
				if (a instanceof LexicalAction) {
					LexicalAction la = (LexicalAction) a;
					Effect[] effects = la.getEffects();
					FOR: for (int j = effects.length - 1; j >= 0; j--) {
						IfThenElse ite = (IfThenElse) effects[j];
						for (Label l : this.requiredTriggers) {
							logger.debug("Looking in triggers for " + l);
							if (!ite.hasTrigger(l)) {
								logger.debug("check failed");
								subsequence.add(0, new LexicalAction(la.getWord(), effects[j]));
								continue FOR;
							}

						}
						subsequence.add(0, new LexicalAction(la.getWord(), effects[j]));
						success = true;
						break WHILE;

					}
				} else
					subsequence.add(0, a);

			}
			if (success) {
				logger.debug("check succeeded");
				logger.debug("instatiating metaactionsequence with subsequence length:" + subsequence.size());
				return this.actionSequence.equals(subsequence);
			}

			if (actionSequence instanceof MetaActionSequence) {
				((MetaActionSequence) actionSequence).getMeta().reset();
			}
			// for action labels (sloppy substitution etc), something like
			// if (check(t.getActions()))
			// go one step further back in context and try again
			context = context.getPrevious();
		}
		return false;
	}

	public ArrayList<Meta<?>> getMetas() {
		ArrayList<Meta<?>> result = new ArrayList<Meta<?>>();
		for (Label l : requiredTriggers)
			result.addAll(l.getMetas());
		if (this.actionSequence instanceof MetaActionSequence)
			result.addAll(((MetaActionSequence) actionSequence).getMetas());
		return result;
	}

	public ArrayList<Meta<?>> getBoundMetas() {
		ArrayList<Meta<?>> result = new ArrayList<Meta<?>>();
		for (Label l : requiredTriggers)
			result.addAll(l.getBoundMetas());
		if (this.actionSequence instanceof BoundActionSequenceVariable)
			result.addAll(((BoundActionSequenceVariable) actionSequence).getBoundMetas());
		return result;

	}

	public ActionSequence getActionSequence() {
		return this.actionSequence;
	}

	public TreeSet<Label> getRequiredTriggers() {
		return this.requiredTriggers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.EmbeddedLabel#instantiate()
	 */
	@Override
	public Label instantiate() {
		// we shouldn't need this, as this is not the kind of label that gets
		// put on a tree, but just in case:
		TreeSet<Label> labels = new TreeSet<Label>();
		for (Label l : this.requiredTriggers) {
			labels.add(l.instantiate());
		}
		return new ContextualActionLabel(labels, this.actionSequence.instantiate());
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
		ContextualActionLabel other = (ContextualActionLabel) obj;
		if (other.getRequiredTriggers().size() != getRequiredTriggers().size())
			return false;
		boolean triggerEquality = this.requiredTriggers.equals(other.getRequiredTriggers());
		boolean actionSeqEquality = this.actionSequence.equals(other.getActionSequence());
		return triggerEquality && actionSeqEquality;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String result = ContextualActionLabel.FUNCTOR + "(\n";
		result += this.actionSequence.toString();
		result += ",{";
		for (Label l : requiredTriggers) {
			result += l + ",";
		}
		return result.substring(0, result.length() - 1) + "})";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.tree.label.Label#toUnicodeString()
	 */
	/*
	 * @Override public String toUnicodeString() { String result="context_tree("; for(Label l:requiredNodeLabels) {
	 * result+=l.toUnicodeString()+","; } return result.substring(0, result.length()-1)+")"; }
	 */
	public static void main(String a[]) {
		ContextualActionLabel cl = new ContextualActionLabel("triggered_by(Y, ty(t))", null);
		for (Label l : cl.getRequiredTriggers()) {
			logger.debug(l);
		}
		logger.debug("\n");
		logger.debug(cl);

	}

}
