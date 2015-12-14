/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.Grammar;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.Lexicon;
import qmul.ds.formula.Formula;
import qmul.ds.formula.FormulaMetavariable;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;

/**
 * A generic parser
 * 
 * @author mpurver
 */
public abstract class Parser<T extends ParserTuple> implements DSParser {

	private static Logger logger = Logger.getLogger(Parser.class);

	protected ParseState<T> state;
	protected Lexicon lexicon;
	protected Grammar grammar;

	private boolean allowSpeedUps = true;

	public Parser(Lexicon lexicon, Grammar grammar) {
		state = new ParseState<T>();
		this.lexicon = lexicon;
		this.grammar = grammar;
	}

	/**
	 * @param resourceDir
	 *            the dir containing computational-actions.txt,
	 *            lexical-actions.txt, lexicon.txt
	 */
	public Parser(File resourceDir) {
		this(new Lexicon(resourceDir), new Grammar(resourceDir));
	}

	/**
	 * @param resourceDirNameOrURL
	 *            the dir containing computational-actions.txt,
	 *            lexical-actions.txt, lexicon.txt
	 */
	public Parser(String resourceDirNameOrURL) {
		this(new Lexicon(resourceDirNameOrURL), new Grammar(
				resourceDirNameOrURL));
	}

	/**
	 * @return the lexicon
	 */
	public Lexicon getLexicon() {
		return lexicon;
	}

	/**
	 * @return the grammar
	 */
	public Grammar getGrammar() {
		return grammar;
	}

	/**
	 * @return a {@link Generator} initialised from this parser with its current
	 *         state
	 */
	public abstract Generator<T> getGenerator();

	/**
	 * Reset the parse state to the initial (axiom) state
	 */
	public void init() {
		FormulaMetavariable.resetPool();

		// logger.info("We are here.");
		state.clear();
		addAxiom();
		adjust();
		// logger.info(state.toString());
	}

	/**
	 * Tell the parser we're beginning a new sentence. By default, this just
	 * resets to the initial (axiom) state
	 * 
	 * @see init()
	 */
	public void newSentence() {
		addAxiom();
		adjust();

	}

	public void setState(ParseState<T> state) {
		this.state = state;
	}

	/**
	 * Add a new AXIOM {@link ParserTuple} to the state, by default ensuring
	 * that the state is empty. Subclasses may override this to e.g. move the
	 * current state to the context
	 */
	protected void addAxiom() {
		logger.trace("Running addAxiom in Parser.");
		state.clear();
		state.add(getAxiom());
	}

	/**
	 * @return the AXIOM {@link ParserTuple} for this {@link Parser}
	 */
	protected abstract T getAxiom();

	/**
	 * @return a shallow copy of the current state
	 */
	public ParseState<T> getState() {
		if (state == null) {
			return null;
		}
		return state.clone();
	}

	/**
	 * Returns a state containing the @param N best tuples of current ParseState
	 * 
	 * @return
	 */
	public abstract ParseState<T> getStateWithNBestTuples(int N);

	/**
	 * Add a new AXIOM {@link ParserTuple} to the state, by default ensuring
	 * that the state is empty. Subclasses may override this to e.g. move the
	 * current state to the context
	 */

	/**
	 * print the current state
	 */
	public void printState() {
		logger.info("\nNew state (" + state.size() + "):");
		for (T tuple : state) {
			logger.info(tuple);
		}
		logger.info("");
	}

	/**
	 * @param allowSpeedUps
	 *            if true, allow speed-ups e.g. removing LHS of alwaysGood
	 *            computational actions
	 */
	public void setAllowSpeedUps(boolean allowSpeedUps) {
		this.allowSpeedUps = allowSpeedUps;
	}

	public boolean parse() {
		return false;
	}

	/**
	 * @param tuple
	 * @param action
	 * @param word
	 * @return the result of applying action to a copy of tuple; this will be
	 *         null if the action aborts. Word may be null if this is a
	 *         non-lexical action
	 */
	protected abstract T execAction(T tuple, Action action, String word);

	protected abstract Collection<T> execExhaustively(T tuple, Action action,
			String word);

	/**
	 * Extend the current state by applying all possible (sequences of)
	 * computational actions
	 */
	private void adjust() {
		adjust(state);
	}

	/*
	 * public ParseState<T> execLexicalActionSequence(ArrayList<Action>
	 * lexicalActions) {
	 * 
	 * }
	 */

	/**
	 * Extend the given state by applying all possible (sequences of)
	 * computational actions
	 * 
	 * @param state
	 */
	public void adjust(ParseState<T> state) {
		// set up map of combinations we've already tried, to save duplicating
		// effort
		HashMap<ComputationalAction, HashMap<Tree, Boolean>> tried = new HashMap<ComputationalAction, HashMap<Tree, Boolean>>();
		for (ComputationalAction action : grammar.values()) {
			tried.put(action, new HashMap<Tree, Boolean>());
		}
		logger.debug("Start with " + state.size() + " tuples in state");
		logger.debug("First Tuple is:" + state.first());
		boolean changed;
		do {
			changed = false;
			ACTION: for (ComputationalAction action : grammar.values()) {
				TUPLE: for (T tuple : state.clone()) {
					// see if we've already tried this action/tree combination
					// ...
					if (tried.get(action).containsKey(tuple.getTree())) {
						// if so ...
						if (tried.get(action).get(tuple.getTree())
								.booleanValue()
								&& allowSpeedUps && action.isAlwaysGood()) {
							// ... and it succeeded, and we're allowing
							// speedups: remove the tuple
							state.remove(tuple);
							logger.trace("Removed LHS of " + action.getName()
									+ " = " + tuple);
							logger.trace("Now " + state.size()
									+ " tuples in state");
						}
						// ... either way, don't bother trying again
						continue TUPLE;
					}
					logger.debug("testing " + action.getName() + " with "
							+ state.size() + " tuples in state");
					// printState();
					if (!action.backtrackOnSuccess()) {
						T newTuple = execAction(tuple, action, null);
						logger.debug("old tuple " + tuple);
						logger.debug("new tuple " + newTuple);
						if (newTuple == null) {
							// remember that this action/tree combination failed
							tried.get(action).put(tuple.getTree(),
									Boolean.FALSE);
						} else {
							// remember that this action/tree combination
							// succeeded
							tried.get(action)
									.put(tuple.getTree(), Boolean.TRUE);
							// if allowing speedups, remove the old tuple
							if (allowSpeedUps && action.isAlwaysGood()) {
								state.remove(tuple);
								logger.debug("Removed LHS of "
										+ action.getName() + " = " + tuple);
								logger.debug("Now " + state.size()
										+ " tuples in state");
							}
							// if we now have an unseen tuple, add it and start
							// again
							if (!state.contains(newTuple)) {
								// logger.info("test tuple " +
								// newTuple.equals(tuple));
								// logger.info("Contains 0 " +
								// state.contains(newTuple));
								state.add(newTuple);
								changed = true;
								logger.debug("Applied CA " + action.getName()
										+ " to " + tuple);
								logger.trace(action);
								logger.debug("Now " + state.size()
										+ " tuples in state");
								break ACTION;
							}
						}
					} else {
						Collection<T> newTuples = execExhaustively(tuple,
								action, null);
						if (newTuples == null || newTuples.isEmpty()) {
							// remember that this action/tree combination failed
							tried.get(action).put(tuple.getTree(),
									Boolean.FALSE);
						} else {
							// remember that this action/tree combination
							// succeeded
							tried.get(action)
									.put(tuple.getTree(), Boolean.TRUE);
							// if allowing speedups, remove the old tuple
							if (allowSpeedUps && action.isAlwaysGood()) {
								state.remove(tuple);
								logger.trace("Removed LHS of "
										+ action.getName() + " = " + tuple);
								logger.trace("Now " + state.size()
										+ " tuples in state");
							}
							// if we now have unseen tuples, add them and start
							// again
							boolean somethingAdded = false;
							for (T tu : newTuples) {
								if (!state.contains(tu)) {
									// logger.info("test tuple " +
									// newTuple.equals(tuple));
									// logger.info("Contains 0 " +
									// state.contains(newTuple));
									state.add(tu);
									changed = true;
									logger.debug("Applied CA "
											+ action.getName() + " to " + tuple);
									logger.trace(action);
									logger.debug("Now " + state.size()
											+ " tuples in state");
									somethingAdded = true;
								}
							}
							if (somethingAdded)
								break ACTION;
						}

					}
				}
			}
		} while (changed);

		for (ParserTuple pt : state) {
			if (pt instanceof ContextParserTuple) {
				ContextParserTuple context = ((ContextParserTuple) pt)
						.getPrevious();
				logger.debug(pt + "-->" + context);
			}
		}

		// printState();
	}

	/**
	 * @param word
	 * @return the (possibly empty) state which results from extending the
	 *         current state with all possible lexical actions corresponding to
	 *         the given word; or null if the state was already empty
	 */
	public ParseState<T> parseWord(HasWord word) {
		return parseWord(state, word.word());
	}

	/**
	 * @param state
	 * @param word
	 * @return the (possibly empty) state which results from extending the given
	 *         state with all possible lexical actions corresponding to the
	 *         given word; or null if the state was already empty
	 */
	public ParseState<T> parseWord(ParseState<T> state, String word) {
		if (state.isEmpty()) {
		logger.debug("State empty at " + word);
			return null;
		}
		if (!lexicon.containsKey(word)) {
			//logger.info("Skipping word " + word);
			return null;
		}
		logger.debug("Parsing word:(" + word + ")");
		// words may be ambiguous i.e. have more than one possible action
		ParseState<T> oldState = state.clone();
		state.clear();
		lexloop: for (LexicalAction action : lexicon.get(word)) {

			for (T oldTuple : oldState) {
				T newTuple = null;
				try {
					newTuple = execAction(oldTuple, action, word);
				} catch (Exception e) {
					continue lexloop;
				}
				if (newTuple != null) {
					state.add(newTuple);
					logger.debug("Applied LA " + action.getName() + " to "
							+ oldTuple);
					logger.trace(action);

				}
			}
		}
		if (state.isEmpty()) {
			logger.debug("Empty state, stopping!");
		} else {
			adjust(state);

		}
		// printState();
		return state;
	}

	/**
	 * Returns a copy of a parseState, but every tree has a compiled TTR
	 * formulae at its root
	 * 
	 * 
	 */
	/*
	 * uncomment after TTR integration public ParseState<T>
	 * TTRState(ParseState<T> oldState){ ParseState<T> ttrState = new
	 * ParseState<T>(); ParseState<T> oldishState = oldState.clone(); for (T
	 * tuple : oldishState) { tuple.getTree().compileMaximalSemantics();
	 * ttrState.add(tuple);
	 * 
	 * } return ttrState;
	 * 
	 * }
	 */
	/**
	 * @param words
	 * @return the resulting (possibly empty) state, or null if the state became
	 *         empty before seeing the last word
	 */
	public ParseState<T> parseWords(List<String> words) {
		for (String word : words) {
			parseWord(word);
		}
		return getState();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.nlp.parser.Parser#parse(java.util.List)
	 */
	@Override
	public boolean parse(List<? extends HasWord> words) {
		for (HasWord word : words) {
			parseWord(word);
		}
		return successful();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.nlp.parser.Parser#parse(java.util.List,
	 * java.lang.String)
	 */
	@Override
	public boolean parse(List<? extends HasWord> words, String goal) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @return true if the current state is non-null and non-empty
	 */
	protected boolean successful() {
		return ((state != null) && !state.isEmpty());
	}

	/**
	 * @return the "best" tuple in the current state (where "best" is defined by
	 *         the natural ordering of the {@link ParserTuple} implementation
	 *         used), or null if the state is empty
	 */
	public T getBestTuple() {
		if (!successful()) {
			return null;
		}
		return state.first();
	}

	/**
	 * @return the "best" tree in the current state (where "best" is defined by
	 *         the natural ordering of the {@link ParserTuple} implementation
	 *         used), or null if the state is empty
	 */
	public Tree getBestParse() {
		if (!successful()) {
			return null;
		}
		return state.first().getTree();
	}

	/**
	 * @return the root node {@link Formula} if present, other wise the first
	 *         one found via breadth-first search
	 */
	public Formula getBestFormula(T t) {
		Formula f = t.getTree().getRootNode().getFormula();
		// if (f == null) {
		// // try filling at pointed node
		// if ((t.getTree().getPointedNode().getType() != null) &&
		// (t.getTree().getPointedNode().getFormula() == null))
		// {
		// t.getTree().getPointedNode().add(new
		// FormulaLabel(Formula.create("[v:x]")));
		// ParseState<T> state = new ParseState<T>();
		// state.add(t);
		// adjust(state);
		// f = state.first().getTree().getRootNode().getFormula();
		// }
		// }
		if (f == null) {
			// search daughters
			for (Node node : t.getTree()
					.getDaughters(t.getTree().getRootNode())) {
				Formula fd = node.getFormula();
				if (fd != null) {
					f = fd;
				}
			}
		}
		return f;
	}

	public ParseState<T> parseWord(String word) {
		HasWord w = new Word(word);
		return parseWord(w);
	}
	
	public boolean parseUtterance(Utterance utt)
	{
		logger.info("disregarding speaker of utterance.");
		return parse(utt.words);
	}
	
	public static void main(String a[])
	{
		Utterance u=new Utterance("what colour");
		System.out.println(u);
	}

}
