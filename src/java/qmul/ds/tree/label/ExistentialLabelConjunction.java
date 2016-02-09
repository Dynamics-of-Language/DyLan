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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.meta.Meta;
import qmul.ds.action.meta.MetaElement;
import qmul.ds.action.meta.MetaNodeAddress;
import qmul.ds.tree.Node;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;

/**
 * This represents an existentially quantified conjunction of labels, e.g. Ex.(DOM(x) & scope(x<Y)) The class supports
 * internal backtracking within the conjoined labels. It exploits the fact that rule meta-variables have an implicit
 * existential meaning. It does this by replacing the variable that is quantified over by a temporary meta-variable.
 * 
 * This means that {@link ExistentialLabel} is now deprecated.
 * 
 * TODO: we need to treat these properly as FOL formulae (rather than just a conjunction of labels with the existential
 * quantifier), i.e. treat each IF step as an FOL formula; is there a java package that handles FOL formulae, I wonder.
 * YES there is JTP.
 * 
 * 
 * 
 * @author Arash
 * 
 */

public class ExistentialLabelConjunction extends EmbeddedLabelGroup implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// Variable is/should always be 'x' under this implementation.
	public static final String FUNCTOR = "Ex.";
	public static final Pattern EXISTENTIAL_LABEL_PATTERN = Pattern.compile(FUNCTOR + "(.+)");
	// private List<String> labelStrings=new ArrayList<String>();
	public static final String metaVarReplacement = "META";
	private transient Backtracker backtracker;

	public ExistentialLabelConjunction(List<Label> sl, IfThenElse ite) {
		super(ite);
		labels = sl;
	}

	public ExistentialLabelConjunction(String s, IfThenElse ite) {
		super(ite);
		Matcher m = EXISTENTIAL_LABEL_PATTERN.matcher(s);
		if (m.matches()) {

			Matcher m1 = EmbeddedLabelGroup.LABEL_GROUP_PATTERN.matcher(m.group(1));
			if (!m1.matches()) {

				String label = m.group(1);
				labels.add(LabelFactory.create(label.trim(), null));

			} else {
				String labelSt = m1.group(1);
				String[] labelS = labelSt.split("&");
				for (String l : labelS) {
					labels.add(LabelFactory.create(l.trim(), null));
				}
			}
		} else
			throw new IllegalArgumentException("Unrecognised Existential Label string: " + s);
	}

	public ExistentialLabelConjunction(List<Label> result) {
		this(result, null);
	}

	@Override
	public ExistentialLabelConjunction instantiate() {
		List<Label> result = new ArrayList<Label>();
		for (Label l : labels) {
			result.add(l.instantiate());
		}
		return new ExistentialLabelConjunction(result);
	}

	public boolean equals(Object o) {
		// logger.debug("testing exist label equality");
		if (o == null)
			return false;
		if (this == o)
			return true;

		if (!(o instanceof ExistentialLabelConjunction)) {

			return false;
		}
		ExistentialLabelConjunction other = (ExistentialLabelConjunction) o;
		if (other.getLabels().size() != getLabels().size())
			return false;

		for (int i = 0; i < labels.size(); i++) {
			Label self = labels.get(i);
			Label oth = other.labels.get(i);
			if (!self.equals(oth)) {
				logger.debug(self + " was not equal to " + oth + "\nreturning false from equals in ELC");

				return false;
			}
		}
		return true;
	}

	public ArrayList<Meta<?>> getBoundMetas() {
		ArrayList<Meta<?>> r = new ArrayList<Meta<?>>();
		for (Label l : labels) {

			r.addAll(l.getBoundMetas());
		}
		return r;
	}

	public boolean checkWithTupleAsContext(Tree tree, ParserTuple context) {

		this.setupBacktracker();
		// __________________________ now the label checking:

		boolean success;
		logger.debug("checking existential label " + this + " at " + tree);
		do {
			// check each label present/inferable in turn
			success = true;
			for (int i = backtracker.index; i < labels.size(); i++) {
				Label label = this.labels.get(i);
				backtracker.setIndex(i);
				logger.debug("lab check " + label + " at " + tree);
				if (label.checkWithTupleAsContext(tree, context)) {
					if (label instanceof AddressLabel) {
						AddressLabel al = (AddressLabel) label;
						logger.debug("An Address Label: " + al);
						NodeAddress na = al.getAddress();
						if (na instanceof MetaNodeAddress) {
							MetaNodeAddress mna = (MetaNodeAddress) na;
							if (mna.getValue() == null) {
								success = false;
								break;
							}
							if (!mna.getValue().isLocallyFixed()) {
								logger.debug("fail lab check " + label + " at tree " + tree);
								logger.debug("address not fixed");
								success = false;
								break;
							}
						} else if (!na.isLocallyFixed()) {
							logger.debug("fail lab check " + label + " at node " + tree);
							logger.debug("address not fixed");
							success = false;
							break;
						}
					}
					logger.debug("pass lab check " + label + " at " + tree);
				} else {
					logger.debug("fail lab check " + label + " at " + tree);
					success = false;
					break;
				}

			}
		} while (!success && backtracker.canBacktrack(tree, context));

		MetaElement.resetBoundMetas();
		return success;

	}

	@Override
	public int hashCode() {
		final int prime = 17;
		int result = 1;
		for (Label label : labels)
			result = prime * result + ((label == null) ? 0 : label.hashCode());

		result = prime * result + FUNCTOR.hashCode();

		return result;
	}

	public boolean check(Node n) {

		this.setupBacktracker();
		// __________________________ now the label checking:

		boolean success;
		logger.debug("checking existential label " + this + " at node " + n);
		do {
			// check each label present/inferable in turn
			success = true;
			for (int i = backtracker.index; i < labels.size(); i++) {
				Label label = this.labels.get(i);
				backtracker.setIndex(i);
				logger.debug("lab check " + label + " at node " + n);

				if (label.check(n)) {
					if (label instanceof AddressLabel) {
						AddressLabel al = (AddressLabel) label;
						logger.debug("An Address Label: " + al);
						NodeAddress na = al.getAddress();
						if (na instanceof MetaNodeAddress) {
							MetaNodeAddress mna = (MetaNodeAddress) na;
							if (mna.getValue() == null) {
								success = false;
								break;
							}
							if (!mna.getValue().isLocallyFixed()) {
								logger.debug("fail lab check " + label + " at node " + n);
								logger.debug("address not fixed");
								success = false;
								break;
							}
						} else if (!na.isLocallyFixed()) {
							logger.debug("fail lab check " + label + " at node " + n);
							logger.debug("address not fixed");
							success = false;
							break;
						}
					}
					logger.debug("pass lab check " + label + " at node " + n);
				} else {
					logger.debug("fail lab check " + label + " at node " + n);
					success = false;
					break;
				}

			}
		} while (!success && backtracker.canBacktrack(n));

		MetaElement.resetBoundMetas();
		return success;

	}

	/*
	 * private static String replaceVarWithMeta(String l, String string) { String result=l.substring(0,1); for(int
	 * i=1;i<l.length();i++) { String before=l.substring(i-1, i); String self=l.substring(i,i+1); String
	 * after=(i<l.length()-1)?l.substring(i+1,i+2):null; if (self.equals(string)&&!before.matches("[a-zA-Z]")) { if
	 * (after==null) result+=metaVarReplacement; else if(!after.matches("[a-zA-Z]")) result+=metaVarReplacement; else
	 * result+=self; } else result+=self; } return result; }
	 */
	

	private void setupBacktracker() {
		this.backtracker = new Backtracker();
		for (Label l : labels) {

			ArrayList<Meta<?>> metas = new ArrayList<Meta<?>>();
			metas.addAll(l.getMetas());
			metas.addAll(l.getBoundMetas());
			if (this.embeddingITE == null) {
				logger.debug("embedding ITE is null");
				logger.debug("no exceptions in adding metas to backtracker");
				logger.debug("Adding and resetting:" + metas);
				backtracker.addAndResetExcept(metas, new ArrayList<Meta<?>>());
			} else {
				List<Meta<?>> exceptions = this.embeddingITE.getMetasAboveLabel(this);
				// logger.debug("Adding and resetting " + metas + " except:" + exceptions);

				backtracker.addAndResetExcept(metas, this.embeddingITE.getMetasAboveLabel(this));
			}
		}

		backtracker.setIndex(0);

	}

	public String toString() {
		if (labels.size() == 0)
			return FUNCTOR;
		if (labels.size() > 1)
			return FUNCTOR + super.toString();
		else
			return FUNCTOR + labels.get(0);
	}

	public String toUnicodeString() {
		return FUNCTOR + super.toUnicodeString();
	}

	/**
	 * A private class to handle backtracking when trying to find successful combinations of {@link MetaElement}
	 * instantiation
	 * 
	 * @author mpurver
	 */
	private class Backtracker {

		private HashMap<Meta<?>, Integer> whenIntroduced;
		private ArrayList<Meta<?>> metas;
		private int index;

		private Backtracker() {
			whenIntroduced = new HashMap<Meta<?>, Integer>();
			metas = new ArrayList<Meta<?>>();
			index = 0;
		}

		public HashSet<Meta<?>> getMetas() {
			HashSet<Meta<?>> res = new HashSet<Meta<?>>();
			res.addAll(metas);
			return res;

		}

		/**
		 * Initialise the {@link MetaElement}s for a single label in the conjunction (making sure they're
		 * uninstantiated)
		 * 
		 * @param metas
		 */
		private void add(List<Meta<?>> metas) {
			for (Meta<?> meta : metas) {
				if (!this.metas.contains(meta)) {
					meta.reset();
					this.metas.add(meta);
					whenIntroduced.put(meta, index);
				}
			}
			index++;
		}

		/**
		 * Initialise the {@link MetaElement}s for a single label in the conjunction, making sure it resets only the
		 * metas not included in the second parameter
		 * 
		 * @param metas
		 *            to be added and reset
		 * @param exceptions
		 *            those that should be added but not reset
		 */
		private void addAndResetExcept(List<Meta<?>> metas, List<Meta<?>> exceptions) {
			for (Meta<?> meta : metas) {
				if (!this.metas.contains(meta)) {
					if (!exceptions.contains(meta)) {
						meta.reset();

						this.metas.add(meta);
						whenIntroduced.put(meta, index);
					}
				}
			}
			index++;
		}

		/**
		 * @param index
		 *            the step currently being attempted
		 */
		private void setIndex(int index) {
			this.index = index;
		}

		/**
		 * @param tree
		 * @param context
		 * @return true if we can backtrack to a point where a previously instantiated {@link MetaElement} can be given
		 *         a new value which also succeeds. Sets index to the IF step where this happened; uninstantiates all
		 *         {@link MetaElement}s introduced after this step so that they can get new values too
		 */
		private boolean canBacktrack(Tree tree, ParserTuple context) {
			for (int i = metas.size() - 1; i >= 0; i--) {
				Meta<?> meta = metas.get(i);
				// don't bother with steps we haven't got to yet
				if (whenIntroduced.get(meta) <= index) {
					// uninstantiate this meta-element, remembering its value
					index = whenIntroduced.get(meta);
					Label label = labels.get(index);
					logger.debug("Backtrack attempt at conjunction step " + index + " " + meta);
					if (meta.backtrack()) {
						// logger.debug("after backtrack:"+meta.backtrack);
						if (label.checkWithTupleAsContext(tree, context)) {
							// if it can succeed with a new value, we're good to
							// go; but must uninstantiate all those
							// introduced later
							// logger.debug("check success in canBacktrack. Label after check:"+label);
							for (int j = i + 1; j < metas.size(); j++) {
								Meta<?> meta2 = metas.get(j);
								if (whenIntroduced.get(meta2) >= index) {
									meta2.reset();
								}
							}
							logger.debug("Backtracking at conjunction step " + index + " " + meta);
							return true;
						} else {
							// if not, re-instantiate it TODO and remember not
							// to backtrack again?

							meta.unbacktrack();
							logger.debug("unbacktracked:" + meta);
						}
					}
				}
			}
			return false;
		}

		/**
		 * 
		 * @param n
		 *            Node
		 * @return true if we can backtrack to a point where a previously instantiated {@link MetaElement} can be given
		 *         a new value which also succeeds. Sets index to the IF step where this happened; uninstantiates all
		 *         {@link MetaElement}s introduced after this step so that they can get new values too
		 */
		private boolean canBacktrack(Node n) {
			for (int i = metas.size() - 1; i >= 0; i--) {
				Meta<?> meta = metas.get(i);
				// don't bother with steps we haven't got to yet
				if (whenIntroduced.get(meta) <= index) {
					// uninstantiate this meta-element, remembering its value
					index = whenIntroduced.get(meta);
					Label label = labels.get(index);
					logger.debug("Backtrack attempt at IF step " + index + " " + meta);
					if (meta.backtrack()) {
						// logger.debug("after backtrack:"+meta.backtrack);
						if (label.check(n)) {
							// if it can succeed with a new value, we're good to
							// go; but must uninstantiate all those
							// introduced later
							// logger.debug("check success in canBacktrack. Label after check:"+label);
							for (int j = i + 1; j < metas.size(); j++) {
								Meta<?> meta2 = metas.get(j);
								if (whenIntroduced.get(meta2) >= index) {
									meta2.reset();
								}
							}
							logger.debug("Backtracking at IF step " + index + " " + meta);
							return true;
						} else {
							// if not, re-instantiate it TODO and remember not
							// to backtrack again?
							// logger.debug("unbacktracking:"+meta);
							meta.unbacktrack();
						}
					}
				}
			}
			return false;
		}

	}

}
