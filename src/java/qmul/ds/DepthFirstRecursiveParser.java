package qmul.ds;

import java.io.File;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.Grammar;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.Lexicon;

/**
 * A Depth-First parser. Searches parse DAG depth first, returns the first successful parse. Needs to be sub-classed, as
 * per {@link Parser}, e.g. see {@link DepthFirstContextParser}.
 * 
 * 
 * :(: The problem here is that this implementation is not properly incremental in lacking a parseWord method which
 * takes a parse state, parses a word and returns a parse state. . . the reason is that we don't here have a
 * straightforward way of holding a parse state . .. the parse state is implicit: it's the state of the recursion (call
 * stack/heap), since backtracking is here simulated through recursion . . . . .
 * 
 * TODO: need to implement DAGParser, DAGParseState, DAGParserTuple, make backtracking explicit
 * 
 * @deprecated for now, due to the above!
 * 
 * @author arash
 * 
 * @param <T>
 */

public abstract class DepthFirstRecursiveParser<T extends ParserTuple> {

	private static Logger logger = Logger.getLogger(Parser.class);
	private Lexicon lexicon;
	private Grammar grammar;
	public static String endOfSentPattern = "\\s*[\\.!\\?]\\s*";

	public DepthFirstRecursiveParser(Lexicon l, Grammar g) {
		this.lexicon = l;
		this.grammar = g;
	}

	/**
	 * @param resourceDir
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public DepthFirstRecursiveParser(File resourceDir) {
		this(new Lexicon(resourceDir), new Grammar(resourceDir));
	}

	/**
	 * @param resourceDirNameOrURL
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public DepthFirstRecursiveParser(String resourceDirNameOrURL) {
		this(new Lexicon(resourceDirNameOrURL), new Grammar(resourceDirNameOrURL));
	}

	/**
	 * @param tuple
	 * @param action
	 * @param word
	 * @return the result of applying action to a copy of tuple; this will be null if the action aborts. Word may be
	 *         null if this is a non-lexical action
	 */
	protected abstract T execAction(T tuple, Action action, String word);

	/**
	 * @return the AXIOM {@link ParserTuple} for this {@link Parser}
	 */
	protected abstract T getAxiom();

	public T parse(List<String> words) {
		return parse(words, getAxiom());

	}

	/**
	 * Depth first search for a parse. All sequences of computational actions will eventually be searched. The first
	 * successful parse will be returned. TODO: use ScoredParserTuple. Parse fails if score is less than a certain
	 * minimum??
	 * 
	 * @param word
	 * @param oldTuple
	 * @return
	 */
	public T parse(List<String> words, T tuple) {
		if (tuple == null)
			return null;
		if (words.isEmpty())
			return tuple;

		for (LexicalAction lexAction : lexicon.get(words.get(0))) {
			logger.info("Executing Lex action: " + lexAction.getWord());
			T t0 = execAction(tuple, lexAction, lexAction.getWord());

			if (t0 == null) {
				for (ComputationalAction ca : grammar) {
					T t1 = execAction(tuple, ca, null);
					logger.info("Executed Comp Action: " + ca.getName() + " on tree: " + tuple.getTree());
					logger.info("Resulting Tree: " + t1);
					T t2 = parse(words, t1);
					logger.info("returned from parse same words:" + words);
					logger.info("result: " + (t2 == null ? t2 : t2.getTree()));
					if (t2 != null) {
						return t2;
					}

				}
				logger.info("Out of CA loop------------");

			} else {
				logger.info("Executed Lex action " + lexAction.getWord());
				logger.info("Resulting Tree:" + t0.getTree());
				T t3 = parse(words.subList(1, words.size()), t0);
				logger.info("returned from parse: " + words.subList(1, words.size()));
				return t3;
			}

		}
		logger.info("Returning null----");
		return null;
	}

	/**
	 * Best first search for a parse. All sequences of computational actions will eventually be searched. The best local
	 * successful parse will be returned. TODO: use ScoredParserTuple. Parse fails if score is less than a certain
	 * minimum??
	 * 
	 * @param word
	 * @param oldTuple
	 * @return
	 */
	public T bestParse(List<String> words, T tuple) {
		// The base cases for recursion.
		// 1) tuple is null
		// 2) no more words to parse
		// 3) first word is end of sentence marker
		if (tuple == null)
			return null;
		if (words.isEmpty())
			return tuple;
		if (words.get(0).matches(endOfSentPattern)) {
			// seen end of sent marker (!?.) attempt to complete tree.
			T complete = this.attemptCompletion(tuple);
			// complete is possibly null. will return this anyway so that backtracking can take place.
			// if it isn't null, then we're done.
			return complete;

		}
		//

		for (LexicalAction lexAction : lexicon.get(words.get(0))) {
			logger.info("Executing Lex action: " + lexAction.getWord());
			T t0 = execAction(tuple, lexAction, lexAction.getWord());

			if (t0 == null) {
				TreeSet<T> nextCompTuples = this.getNextComputationalTuples(tuple);
				for (T t1 : nextCompTuples) {

					logger.info("Going from tuple " + tuple.getTree());
					logger.info("To resulting Tree: " + t1);
					T t2 = bestParse(words, t1);
					logger.info("returned from parse same words:" + words);
					logger.info("result: " + (t2 == null ? t2 : t2.getTree()));
					if (t2 != null) {
						return t2;
					}

				}
				logger.info("Out of CA loop------------");

			} else {
				logger.info("Executed Lex action " + lexAction.getWord());
				logger.info("Resulting Tree:" + t0.getTree());
				T t3 = bestParse(words.subList(1, words.size()), t0);
				logger.info("returned from parse: " + words.subList(1, words.size()));
				return t3;
			}

		}
		logger.info("Returning null----");
		return null;
	}

	public T bestParse(List<String> words) {
		return bestParse(words, getAxiom());

	}

	public ParseState<T> parseExhaustively(List<String> words, T start) {
		// TODO
		return null;
	}

	/**
	 * This will return the set of tuples reachable from t0 by applying all computational actions . The set will be
	 * ordered according to the natural ordering (best first) of its elements, e.g. for ContextParserTuples, this is
	 * based on how complete the tuple's tree is. For ScoredParserTuples this will be based on the score assigned to the
	 * tuple's tree.
	 * 
	 * @param t0
	 *            tuple to apply all computational actions to.
	 * @return the set of tuples reachable from t0 by applying all computational actions
	 */
	private TreeSet<T> getNextComputationalTuples(T t0) {
		TreeSet<T> result = new TreeSet<T>();
		for (ComputationalAction ca : grammar) {
			T compTuple = execAction(t0, ca, null);
			if (compTuple != null)
				result.add(compTuple);
		}
		return result;
	}

	/**
	 * This will attempt to complete the tuple by applying computational actions. This will be a best first search too .
	 * . .
	 * 
	 * @param tuple
	 * @return the complete tuple if reachable by computational actions, null if not.
	 */

	private T attemptCompletion(T tuple) {
		if (tuple.tree.isComplete())
			return tuple;

		TreeSet<T> nextCompTuples = this.getNextComputationalTuples(tuple);
		for (T t1 : nextCompTuples) {

			logger.info("Going from tuple " + tuple.getTree());
			logger.info("To resulting Tree: " + t1);
			T t2 = attemptCompletion(t1);
			logger.info("returned from completion attempt");
			logger.info("result: " + (t2 == null ? t2 : t2.getTree()));
			if (t2 != null) {
				return t2;
			}

		}
		return null;

	}

}
