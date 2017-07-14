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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import qmul.ds.Context;
import qmul.ds.ParserTuple;
import qmul.ds.action.meta.Meta;
import qmul.ds.action.meta.MetaElement;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.LabelFactory;
import edu.stanford.nlp.util.Pair;

/**
 * The <tt>IF ... THEN ... ELSE</tt> conditional action
 * 
 * @author mpurver, arash
 */
public class IfThenElse extends Effect implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(IfThenElse.class);
	public static final String IF_FUNCTOR = "IF";
	public static final String THEN_FUNCTOR = "THEN";
	public static final String ELSE_FUNCTOR = "ELSE";

	private Label[] IF;
	private Effect[] THEN;
	private Effect[] ELSE;
	private int embeddingLevel = 0;
	private IfThenElse parent;

	/**
	 * @param IF
	 * @param THEN
	 * @param ELSE
	 */
	public IfThenElse(Label[] IF, Effect[] THEN, Effect[] ELSE, int embedding, IfThenElse parent) {
		this.IF = IF.clone();
		this.THEN = THEN.clone();
		this.ELSE = ELSE.clone();
		this.embeddingLevel = embedding;
		this.parent = parent;

	}

	public IfThenElse(Label[] IF, Effect[] THEN, Effect[] ELSE) {
		this.IF = IF.clone();
		this.THEN = THEN.clone();
		this.ELSE = ELSE.clone();
		this.embeddingLevel = 0;
		parent = null;

	}

	private static final Pattern ITE_PATTERN = Pattern.compile("(?i)(" + IF_FUNCTOR + "|" + THEN_FUNCTOR + "|"
			+ ELSE_FUNCTOR + ")?\\s*(.*)");

	public IfThenElse(List<String> strings) {
		this(strings, 0, null);

	}

	/**
	 * @param string
	 *            a {@link String} representation e.g. IF X THEN Y ELSE Z as used in lexicon specs. This will now deal
	 *            with the embedded IF THEN ELSE format. However, it is a requirement that no effect be specified after
	 *            an embedded IF THEN ELSE. TODO: indentation will be essential if we want to specify another effect
	 *            after an IF-THEN-ELSE one.
	 * 
	 */

	public IfThenElse(List<String> strings, int embeddingLevel, IfThenElse parent) {
		this.parent = parent;
		logger.debug("creating IfThenElse from:"+strings);
		List<String> stringsCopy = deepCopy(strings);
		this.embeddingLevel = embeddingLevel;
		ArrayList<Label> IF = new ArrayList<Label>();
		ArrayList<Effect> THEN = new ArrayList<Effect>();
		ArrayList<Effect> ELSE = new ArrayList<Effect>();
		String current = IF_FUNCTOR;
		int i = 0;
		// logger.info("CALLED. Size is:"+strings.size());
		while (i < stringsCopy.size()) {
			String string = stringsCopy.get(i).trim();
			// logger.info("processing ITE line:" + string);
			Matcher m = ITE_PATTERN.matcher(string);
			if (m.matches()) {
				if (m.group(1) != null) {

					// logger.info("matched ite: "+m.group(1));
					// logger.info("previous ite: "+current);
					if ((m.group(1).equals(THEN_FUNCTOR) && current.equals(IF_FUNCTOR))
							|| (m.group(1).equals(ELSE_FUNCTOR) && current.equals(THEN_FUNCTOR))) {
						current = m.group(1);
					}
					// logger.info("ite after match: "+current);

				}
				if (current.equals(IF_FUNCTOR)) {
					// logger.info("adding to IF: "+m.group(2));
					IF.add(LabelFactory.create(m.group(2), this));
					i++;
					continue;
				} else if (current.equals(THEN_FUNCTOR)) {
					int indexOfIF = string.toLowerCase().indexOf(IF_FUNCTOR.toLowerCase());

					if (indexOfIF >= 0) {
						int j = findEndIndexOfEmbeddedITE(stringsCopy, i);
						// logger.info("end of if index was:"+j+" And the start was"+
						// i);
						List<String> embeddedITELines = stringsCopy.subList(i, j);

						embeddedITELines.set(0, string.substring(indexOfIF, string.length()));
						// logger.info(embeddedITELines);
						THEN.add(new IfThenElse(embeddedITELines, this.embeddingLevel + 1, this));
						i = j;
						// logger.info("set i to "+j+"size is"+strings.size());
						continue;

					} else {
						// logger.debug("adding to THEN simple action:"+m.group(2));
						THEN.add(EffectFactory.create(m.group(2)));
						i++;
						continue;
					}
				} else if (current.equals(ELSE_FUNCTOR)) {
					int indexOfIF = string.toLowerCase().indexOf(IF_FUNCTOR.toLowerCase());
					if (indexOfIF >= 0) {
						int j = findEndIndexOfEmbeddedITE(stringsCopy, i);
						// logger.info("end of if index was:"+j+" And the start was"+
						// i);
						List<String> embeddedITELines = stringsCopy.subList(i, j);
						embeddedITELines.set(0, string.substring(indexOfIF, string.length()));
						// logger.info(embeddedITELines);
						ELSE.add(new IfThenElse(embeddedITELines, this.embeddingLevel + 1, this));
						i = j;
						// logger.info("set i to "+j+"size is"+strings.size());
						continue;
					} else {
						// logger.debug("adding to ELSE simple action:"+m.group(2));
						ELSE.add(EffectFactory.create(m.group(2)));
						i++;
					}
				}
			} else {
				throw new IllegalArgumentException("unrecognised if-then-else string " + string);
			}
		}
		this.IF = IF.toArray(new Label[IF.size()]);
		this.THEN = THEN.toArray(new Effect[THEN.size()]);
		this.ELSE = ELSE.toArray(new Effect[ELSE.size()]);
		// logger.info("returning");
	}

	private List<String> deepCopy(List<String> strings1) {
		ArrayList<String> a = new ArrayList<String>();
		for (String s : strings1) {
			a.add(new String(s));
		}
		return a;
	}

	public static void main(String a[]) {
		// test for ite initialisation.
		ArrayList<String> lines = new ArrayList<String>();
		lines.add("IF		ty(t)");
		lines.add("			¬<\\/L>Ex.x");		
		lines.add("THEN		put(?+eval)");
		lines.add("			make(\\/L)");
		lines.add("			go(\\/L)");
		lines.add("		    put(?ty(t))");
		lines.add("			make(\\/1)");
		lines.add("			IF	  </\\0/\\L\\/*>?Ex.tn(x)");
		lines.add("				  </\\0/\\L\\/*>ty(e)");
		lines.add("			THEN  merge(</\\0/\\L\\/*>)");
		lines.add("				  delete(?ty(e))");
		lines.add("			ELSE  do_nothing");
		lines.add("ELSE		abort");

		IfThenElse copy = new IfThenElse(lines);
		for(Effect e: copy.THEN)
		System.out.println(e);
		
	}

	@Override
	public IfThenElse clone(){
		IfThenElse ite = new IfThenElse(this.IF.clone(), this.THEN.clone(), this.ELSE.clone());
		return ite;
	}
	private static int findEndIndexOfEmbeddedITE(List<String> strings, int indexOfIF) {
		int embedLevel = 1;
		int j = indexOfIF + 1;
		for (; j < strings.size(); j++) {
			String cur = strings.get(j);
			if (cur.toLowerCase().indexOf(ELSE_FUNCTOR.toLowerCase()) >= 0)
				embedLevel--;
			if (embedLevel < 0)
				break;
			if (cur.toLowerCase().indexOf(IF_FUNCTOR.toLowerCase()) >= 0)
				embedLevel++;
		}
		return j;

	}

	protected Backtracker backtracker = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.atomic.Effect#exec(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	/**
	 * this will associate this ITE and each ITE embedded within it with its own fresh backtracker, reset and add to it
	 * the corresponding set of metas.
	 * 
	 * */
	public void setupBacktrackers(List<Meta<?>> exceptions) {
		this.backtracker = new Backtracker();

		for (Label label : IF) {
			
			backtracker.addAndResetExcept(label.getMetas(), exceptions);
		}
		for (Effect effect : THEN) {
			if (effect instanceof IfThenElse) {
				IfThenElse ite = (IfThenElse) effect;
				ite.setupBacktrackers(getMetasHereAndAbove());

			}
		}
		for (Effect effect : ELSE) {
			if (effect instanceof IfThenElse) {
				IfThenElse ite = (IfThenElse) effect;
				ite.setupBacktrackers(getMetasHereAndAbove());

			}
		}
	}

	private List<Meta<?>> getMetasHereAndAbove() {
		List<Meta<?>> result = getMetas();

		IfThenElse current = parent;
		while (current != null) {
			// logger.debug("Parent Metas:"+current.getMetas());
			result.addAll(current.getMetas());
			current = current.parent;
		}

		return result;

	}

	public List<Meta<?>> getMetas() {
		List<Meta<?>> result = new ArrayList<Meta<?>>();
		for (Label l : IF) {
			result.addAll(l.getMetas());
		}

		return result;
	}

	public List<Meta<?>> getMetasAboveLabel(Label l) {
		List<Meta<?>> result = new ArrayList<Meta<?>>();
		List<Label> triggers = Arrays.asList(IF);
		int i = triggers.indexOf(l);
		logger.debug("index of exist label in embedding IF:" + i);
		for (int j = 0; j < i; j++) {
			result.addAll(IF[j].getMetas());
		}
		IfThenElse current = parent;
		while (current != null) {
			// logger.debug("Parent Metas:"+current.getMetas());
			result.addAll(current.getMetas());
			current = current.parent;
		}

		return result;
	}

	@Override
	public <T extends Tree> T execTupleContext(T tree, ParserTuple context) {
		// get & reset metavariables for this application

		// if (embeddingLevel==0) addAndResetMetas(null);
		MetaElement.resetBoundMetas();
		if (this.embeddingLevel == 0)
			setupBacktrackers(new ArrayList<Meta<?>>());

		logger.debug("IF Labels after reset:" + Arrays.asList(IF));

		backtracker.setIndex(0);

		boolean success;
		T result = null;
		do {
			// check each label present/inferable in turn
			success = true;
			for (int i = backtracker.index; i < IF.length; i++) {
				Label label = IF[i];
				backtracker.setIndex(i);
				logger.debug("lab check " + label + " at " + tree);
				if (label.checkWithTupleAsContext(tree, context)) {
					logger.debug("pass lab check " + label + " at " + tree);
				} else {
					logger.debug("fail lab check " + label + " at " + tree);
					success = false;
					break;
				}
			}

			for (Effect effect : (success ? THEN : ELSE)) {
				if (effect instanceof IfThenElse) {
					IfThenElse ite = ((IfThenElse) effect);
					ite.backtracker.resetMetas();
				}
				logger.debug("Executing effect:" + effect);
				result = effect.execTupleContext(tree, context);
				logger.debug("result was:" + result);
				if (result == null) {
					logger.debug("Null result after executing effect:" + effect);
					break;
				}

			}
			// if one fails, backtrack if possible and try again from there
		} while (result == null && backtracker.canBacktrackTupleContext(tree, context));

		return result;
	}
	
	@Override
	public <E extends DAGEdge, U extends DAGTuple, T extends Tree> T exec(T tree, Context<U,E> context) {
		// get & reset metavariables for this application

		//if (embeddingLevel==0) addAndResetMetas(null);
		logger.debug("Executing ITE:\n"+this);
		MetaElement.resetBoundMetas();
		if (this.embeddingLevel == 0)
			setupBacktrackers(new ArrayList<Meta<?>>());

		logger.debug("IF Labels after reset:" + Arrays.asList(IF));

		backtracker.setIndex(0);

		boolean success;
		T result = null;
		do {
			// check each label present/inferable in turn
			success = true;
			for (int i = backtracker.index; i < IF.length; i++) {
				Label label = IF[i];
				backtracker.setIndex(i);
				logger.debug("lab check " + label + " at " + tree);
				if (label.check(tree, context)) {
					logger.debug("pass lab check " + label + " at " + tree);
				} else {
					logger.debug("fail lab check " + label + " at " + tree);
					success = false;
					break;
				}
			}

			for (Effect effect : (success ? THEN : ELSE)) {
				if (effect instanceof IfThenElse) {
					IfThenElse ite = ((IfThenElse) effect);
					ite.backtracker.resetMetas();
				}
				logger.debug("Executing effect:" + effect);
				result = effect.exec(tree, context);
				logger.debug("result was:" + result);
				if (result == null) {
					logger.debug("Null result after executing effect:" + effect);
					break;
				}

			}
			// if one fails, backtrack if possible and try again from there
		} while (result == null && backtracker.canBacktrack(tree, context));

		return result;
	}


	public <T extends Tree> Collection<Pair<IfThenElse, T>> execExhaustively(T tree, ParserTuple context) {

		// T clone=(T) tree.clone();
		MetaElement.resetBoundMetas();
		if (this.embeddingLevel == 0)
			setupBacktrackers(new ArrayList<Meta<?>>());

		logger.debug("IF Labels after reset:" + Arrays.asList(IF));

		backtracker.setIndex(0);
		Collection<Pair<IfThenElse, T>> result = new ArrayList<Pair<IfThenElse, T>>();
		T cur;
		T t;
		do {
			boolean success;
			cur = null;
			t = (T) tree.clone();
			do {
				// check each label present/inferable in turn
				success = true;
				for (int i = backtracker.index; i < IF.length; i++) {
					Label label = IF[i];
					backtracker.setIndex(i);
					logger.debug("lab check " + label + " at " + t);
					if (label.checkWithTupleAsContext(t, context)) {
						logger.debug("pass lab check " + label + " at " + t);
					} else {
						logger.debug("fail lab check " + label + " at " + t);
						success = false;
						break;
					}
				}

				for (Effect effect : (success ? THEN : ELSE)) {
					if (effect instanceof IfThenElse) {
						IfThenElse ite = ((IfThenElse) effect);
						ite.backtracker.resetMetas();
					}

					cur = effect.execTupleContext(t, context);
					logger.debug("executed atomic:" + effect);

					if (cur == null) {
						logger.debug("null result");
						break;
					}
					logger.debug("success");
				}
				// if one fails, backtrack if possible and try again from there
			} while (cur == null && backtracker.canBacktrackTupleContext(t, context));

			if (cur != null)
				result.add(new Pair<IfThenElse, T>(this.instantiate(), cur));
		} while (cur != null && backtracker.canBacktrackTupleContext(t, context));

		if (result.isEmpty())
			return null;

		return result;
	}

	/*
	 * public <T extends Tree> Collection<T> execExhaustively(T tree, ParserTuple context) {
	 * 
	 * HashSet<T> allResults = new HashSet<T>(); // get & reset metavariables for this application, only if this is the
	 * // top level // IfThenElse. This will apply recursively to all the embedded // IfThenElse's.
	 * MetaElement.resetBoundMetas(); if (this.embeddingLevel == 0) setupBacktrackers(new ArrayList<Meta<?>>());
	 * 
	 * logger.debug("Applying action exhaustively:\n" + this); backtracker.setIndex(0);
	 * 
	 * boolean success;
	 * 
	 * do { // check each label present/inferable in turn
	 * 
	 * T treeCopy = (T) tree.clone();// this cast could potentially be // dangerous. // it wouldn't be if any subclass
	 * of Tree also implemented Clonable // properly. // In that case there wouldn't really be any casting involved.
	 * 
	 * success = true; for (int i = backtracker.index; i < IF.length; i++) { Label label = IF[i];
	 * backtracker.setIndex(i); logger.debug("lab check " + label + " at " + treeCopy); if (label.check(treeCopy,
	 * context)) { logger.debug("pass lab check " + label + " at " + treeCopy); } else { logger.debug("fail lab check "
	 * + label + " at " + treeCopy); success = false; break; } } HashSet<T> localResults = new HashSet<T>(); HashSet<T>
	 * curResults = new HashSet<T>(); localResults.add(treeCopy);
	 * 
	 * for (Effect effect : (success ? THEN : ELSE)) { if (effect instanceof Abort) { localResults.clear(); break; } for
	 * (T t : localResults) {
	 * 
	 * if (effect instanceof IfThenElse) { IfThenElse ite = (IfThenElse) effect; ite.backtracker.resetMetas();
	 * Collection<T> iteRes = ite.execExhaustively(t, context); if (iteRes == null) {
	 * 
	 * continue; } curResults.addAll(iteRes); } else { logger.debug("running action: " + effect); T result =
	 * effect.exec(t, context); logger.debug("result was:" + result); if (result == null) {
	 * 
	 * continue; }
	 * 
	 * curResults.add(result);
	 * 
	 * }
	 * 
	 * } localResults.clear(); if (curResults.isEmpty()) break;
	 * 
	 * localResults.addAll(curResults); curResults.clear();
	 * 
	 * } if (!localResults.isEmpty()) { logger.debug("Success. Adding resulting trees to the list returned:\n" +
	 * localResults); allResults.addAll(localResults); } else { logger.debug("Failed on this instantiation of metas.");
	 * } // even if there's no failure, backtrack if possible and try again // from there } while
	 * (backtracker.canBacktrack(tree, context)); logger.debug("execd exhaustively. Returning " + allResults.size() +
	 * " resulting trees."); if (allResults.isEmpty()) return null; else return allResults; }
	 */

	public Label[] getTriggers() {
		return IF;
	}

	public boolean hasTrigger(Label l) {
		for (int i = 0; i < IF.length; i++)
			if (l.equals(IF[i]))
				return true;
		return false;
	}

	public IfThenElse instantiate() {
		Label[] ifs = new Label[this.IF.length];
		for (int i = 0; i < IF.length; i++) {
			Label l = IF[i];
			ifs[i] = l.instantiate();
		}
		Effect[] THEN = new Effect[this.THEN.length];
		for (int i = 0; i < THEN.length; i++) {
			Effect l = this.THEN[i];
			THEN[i] = l.instantiate();
		}
		Effect[] ELSE = new Effect[this.ELSE.length];
		for (int i = 0; i < ELSE.length; i++) {
			Effect l = this.ELSE[i];
			ELSE[i] = l.instantiate();
		}
		return new IfThenElse(ifs, THEN, ELSE, this.embeddingLevel, this.parent);
	}

	public Label[] getIFClause() {
		return this.IF;

	}
	
	public void addNewLabelintoIF(Label label) {
		Label[] ifs = new Label[this.IF.length + 1];
		for (int i = 0; i < IF.length; i++) {
			Label l = IF[i];
			ifs[i] = l;
		}
		ifs[this.IF.length] = label;
		
		this.IF = ifs;
	}
	
	/**
	 * 
	 * @return all metas at this ite plus all those at all higher ite's in which this ite is embedded
	 */
	/*
	 * public HashSet<Meta<?>> getAllMetasInScope() { if (this.parent == null) return backtracker.getMetas();
	 * else { HashSet<Meta<?>> res = new HashSet<Meta<?>>(); res.addAll(backtracker.getMetas());
	 * res.addAll(parent.getAllMetasInScope()); return res; } }
	 */
	/**
	 * A private class to handle backtracking when trying to find successful combinations of {@link MetaElement}
	 * instantiation
	 * 
	 * @author mpurver
	 */
	private class Backtracker implements Serializable{

		private HashMap<Meta, Integer> whenIntroduced;
		private ArrayList<Meta> metas;
		private int index;

		private Backtracker() {
			whenIntroduced = new HashMap<Meta, Integer>();
			metas = new ArrayList<Meta>();
			index = 0;
		}

		public void resetMetas() {
			for (Meta meta : metas) {
				meta.reset();
			}

		}

		public HashSet<Meta> getMetas() {
			HashSet<Meta> res = new HashSet<Meta>();
			res.addAll(metas);
			return res;

		}

		/**
		 * Initialise the {@link MetaElement}s for a single IF condition (making sure they're uninstantiated)
		 * 
		 * @param metas
		 */
		private void add(ArrayList<Meta<?>> metas) {
			for (Meta<?> meta : metas) {
				if (!this.metas.contains(meta)) {
					meta.reset();
					this.metas.add(meta);
					whenIntroduced.put(meta, index);
				}
			}
			index++;
		}

		private void addAndResetExcept(ArrayList<Meta<?>> arrayList, List<Meta<?>> exceptions) {
			for (Meta<?> meta : arrayList) {
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
		 *            the IF step currently being attempted
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
		private boolean canBacktrackTupleContext(Tree tree, ParserTuple context) {
			for (int i = metas.size() - 1; i >= 0; i--) {
				Meta meta = metas.get(i);
				// don't bother with steps we haven't got to yet
				// logger.debug("whenIntro:" + whenIntroduced);
				// logger.debug("Meta:" + meta);
				// logger.debug(whenIntroduced.containsKey(meta));
				if (whenIntroduced.get(meta) <= index) {
					// uninstantiate this meta-element, remembering its value
					index = whenIntroduced.get(meta);
					Label label = IF[index];
					logger.trace("Backtrack attempt at IF step " + index + " " + meta);
					if (meta.backtrack()) {
						//logger.trace("after backtrack:" + meta.backtrack);
						if (label.checkWithTupleAsContext(tree, context)) {
							// if it can succeed with a new value, we're good to
							// go; but must uninstantiate all those
							// introduced later
							logger.trace("check success in canBacktrack. Label after check:" + label);
							for (int j = i + 1; j < metas.size(); j++) {
								Meta meta2 = metas.get(j);
								if (whenIntroduced.get(meta2) >= index) {
									meta2.reset();
								}
							}
							logger.debug("Backtracking at IF step " + index + " " + meta);
							return true;
						} else {
							// if not, re-instantiate it TODO and remember not
							// to backtrack again?
							logger.trace("unbacktracking:" + meta);
							meta.unbacktrack();
						}
					}
				}
			}
			return false;
		}
		
		/**
		 * @param tree
		 * @param context
		 * @return true if we can backtrack to a point where a previously instantiated {@link MetaElement} can be given
		 *         a new value which also succeeds. Sets index to the IF step where this happened; uninstantiates all
		 *         {@link MetaElement}s introduced after this step so that they can get new values too
		 */
		private <U extends DAGTuple, E extends DAGEdge> boolean canBacktrack(Tree tree, Context<U,E> context) {
			for (int i = metas.size() - 1; i >= 0; i--) {
				Meta meta = metas.get(i);
				// don't bother with steps we haven't got to yet
				// logger.debug("whenIntro:" + whenIntroduced);
				// logger.debug("Meta:" + meta);
				// logger.debug(whenIntroduced.containsKey(meta));
				if (whenIntroduced.get(meta) <= index) {
					// uninstantiate this meta-element, remembering its value
					index = whenIntroduced.get(meta);
					Label label = IF[index];
					logger.trace("Backtrack attempt at IF step " + index + " " + meta);
					if (meta.backtrack()) {
						//logger.trace("after backtrack:" + meta.backtrack);
						if (label.check(tree, context)) {
							// if it can succeed with a new value, we're good to
							// go; but must uninstantiate all those
							// introduced later
							logger.trace("check success in canBacktrack. Label after check:" + label);
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
							logger.trace("unbacktracking:" + meta);
							meta.unbacktrack();
						}
					}
				}
			}
			return false;
		}
	}
	
	public static final int tabSizeForPrinting = 6;// min tab size is 4

	public String toString() {
		String tab = "";
		for (int i = 0; i < tabSizeForPrinting; i++)
			tab += " ";
		String tabs = "";
		for (int i = 0; i < this.embeddingLevel; i++)
			tabs += tab;
		String result = IF_FUNCTOR + tab.substring(0, tab.length() - IF_FUNCTOR.length());
		for (Label l : IF) {
			result += l;
			if (l != IF[IF.length - 1])
				result += "\n" + tabs + tab;
			else
				result += "\n";
		}
		result += tabs + THEN_FUNCTOR + tab.substring(0, tab.length() - THEN_FUNCTOR.length());
		for (Effect e : THEN) {
			result += e;
			if (e != THEN[THEN.length - 1])
				result += "\n" + tabs + tab;
			else
				result += "\n";

		}
		result += tabs + ELSE_FUNCTOR + tab.substring(0, tab.length() - ELSE_FUNCTOR.length());
		for (Effect e : ELSE) {
			result += e;
			if (e != ELSE[ELSE.length - 1])
				result += "\n" + tabs + tab;
			else
				result += "\n";

		}
		return result.substring(0, result.length() - 1);
	}

	public Effect[] getTHENClause() {
		return this.THEN;
	}

	public Effect[] getELSEClause() {

		return ELSE;
	}
}
