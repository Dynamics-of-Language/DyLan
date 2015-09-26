package qmul.ds;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.Grammar;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.UtteredWord;
import qmul.ds.tree.Tree;
import edu.stanford.nlp.ling.HasWord;

/**
 * @deprecated replaced by InteractiveContextParser - which is also a
 *             depth-first parser, and does what this class does and more.
 *             
 *             
 */
public class DepthFirstParser extends Parser<ParserTuple> implements
		edu.stanford.nlp.parser.Parser {

	private static Logger logger = Logger.getLogger(DepthFirstParser.class);

	protected DAGParseState state;
	protected Grammar nonoptionalGrammar;// as determined by the prefix * in
											// action spec files.
	protected Grammar optionalGrammar;

	public DepthFirstParser(Lexicon lexicon, Grammar grammar) {
		super(lexicon, grammar);
		super.state = null;
		this.state = new DAGParseState();
		separateGrammars();
	}

	private void separateGrammars() {
		this.nonoptionalGrammar = new Grammar();
		this.optionalGrammar = new Grammar();
		for (ComputationalAction a : grammar.values()) {
			if (a.isAlwaysGood())
				this.nonoptionalGrammar.put(a.getName(),a);
			else
				this.optionalGrammar.put(a.getName(),a);
		}
	}

	/**
	 * @param resourceDir
	 *            the dir containing computational-actions.txt,
	 *            lexical-actions.txt, lexicon.txt
	 */
	public DepthFirstParser(File resourceDir) {
		super(new Lexicon(resourceDir), new Grammar(resourceDir));
		super.state = null;
		this.state = new DAGParseState();
		separateGrammars();
	}

	public DAGParseState getState() {

		return this.state;
	}
	
	

	/**
	 * @param resourceDirNameOrURL
	 *            the dir containing computational-actions.txt,
	 *            lexical-actions.txt, lexicon.txt
	 */
	public DepthFirstParser(String resourceDirNameOrURL) {
		super(new Lexicon(resourceDirNameOrURL), new Grammar(
				resourceDirNameOrURL));
		super.state = null;
		this.state = new DAGParseState();
		separateGrammars();
	}

	protected ParserTuple getAxiom() {

		return new ParserTuple();
	}

	public void init() {
		addAxiom();
	}

	/**
	 * Add a new AXIOM {@link ParserTuple} to the state, by default ensuring
	 * that the state is empty. Subclasses may override this to e.g. move the
	 * current state to the context
	 */
	protected void addAxiom() {

		state.init();
	}

	/**
	 * Tell the parser we're beginning a new sentence. By default, this just
	 * resets to the initial (axiom) state
	 * 
	 * @see init()
	 */
	public void newSentence() {
		// TODO: currently just resetting, i.e. no context.
		addAxiom();

	}

	private ParserTuple adjustOnce() {
		applyAllActions();
		DAGEdge result;
		do {
			result = state.goFirst();
			if (result != null) {
				applyNonOptionalGrammar();
				break;
			}

		} while (attemptBacktrack());

		if (result != null)
			return state.getCurrentTuple();
		else
			return null;
	}

	private ParserTuple adjustOnce(ParserTuple goal) {
		applyAllActions(goal);
		DAGEdge result;
		do {
			result = state.goFirst();
			if (result != null) {
				applyNonOptionalGrammar(goal);
				break;
			}

		} while (attemptBacktrack());

		if (result != null)
			return state.getCurrentTuple();
		else
			return null;
	}

	public DAGParseState parseWord(UtteredWord word) {
		logger.info("Parsing word: " + word);
		state.resetToFirstTupleAfterLastWord();
		logger.debug("after reset cur is" + state.getCurrentTuple());
		Collection<LexicalAction> actions = this.lexicon.get(word.word());
		if (actions == null || actions.isEmpty()) {
			logger.error("Word not in Lexicon: " + word);
			return null;
		}

		state.wordStack().push(word);
		state.clear();
		if (!parse()) {
			logger.trace("ParseWord returning null");
			return null;
		}
		this.state.thisIsFirstTupleAfterLastWord();

		return this.state;
	}

	public DAGParseState parseWord(UtteredWord word, ParserTuple goal) {
		logger.info("Parsing word: " + word);
		state.resetToFirstTupleAfterLastWord();
		logger.debug("after reset cur is" + state.getCurrentTuple());
		Collection<LexicalAction> actions = this.lexicon.get(word);
		if (actions == null || actions.isEmpty()) {
			logger.error("Word not in Lexicon: " + word);
			return null;
		}
		if (state.hasOutWordEdge(word.word()))
			return null;

		state.wordStack().push(word);
		state.clear();
		if (!parse(goal)) {
			logger.trace("ParseWord returning null");
			state.resetToFirstTupleAfterLastWord();

			return null;
		}
		this.state.thisIsFirstTupleAfterLastWord();

		return this.state;
	}

	public boolean parse() {
		if (state.isExhausted()) {
			logger.info("state exhausted");
			return false;
		}
		ParserTuple cur = null;
		do {
			cur = adjustOnce();
			if (cur == null) {
				state.setExhausted(true);
				return false;
			}

		} while (!state.wordStack().isEmpty());
		state.add(cur);
		return true;
	}

	public boolean parse(ParserTuple goal) {
		if (state.isExhausted()) {
			logger.info("state exhausted");
			return false;
		}
		ParserTuple cur = null;
		do {
			cur = adjustOnce(goal);
			if (cur == null) {
				state.setExhausted(true);
				return false;
			}

		} while (!state.wordStack().isEmpty());
		state.add(cur);
		return true;
	}

	/**
	 * applies all available actions (except non-optional grammar which are
	 * applied separately), i.e. computational and the lexical actions
	 * associated with the top of the words stack, to the current tuple.
	 * 
	 */
	private void applyAllActions() {
		// TODO: Exhaustive application of actions
		state.removeChildren();
		if (!state.wordStack().isEmpty()) {

			for (LexicalAction la : lexicon.get(state.wordStack().peek().word())) {
				ParserTuple t = this.state.execAction(la, new UtteredWord(la.getWord()));
				if (t == null) {
					logger.debug("Action " + la.getName() + " failed at tree "
							+ state.getCurrentTuple().getTree());
				} else {
					logger.debug("Action " + la.getName()
							+ " succeeded at tree "
							+ state.getCurrentTuple().getTree());
					logger.debug("Result was:" + t.getTree());
				}

			}

		}

		for (ComputationalAction a : this.optionalGrammar.values()) {
			ParserTuple t = this.state.execAction(a, null);
			if (t == null) {
				logger.debug("Action " + a + " failed at tree "
						+ state.getCurrentTuple().getTree());
			} else {
				logger.debug("Action " + a + " succeeded at tree "
						+ state.getCurrentTuple().getTree());
				logger.debug("Result was:" + t.getTree());
			}
		}

	}

	public void applyNonOptionalGrammar(ParserTuple goal) {
		do {
			for (ComputationalAction a : this.nonoptionalGrammar.values()) {
				// if a non-optional action can be carried out, it has to be,
				// with no other computational possibilities
				// on this node

				DAGTuple t = this.state.execAction(a, null);
				if (t == null) {
					logger.debug("Action " + a + " failed at tree "
							+ state.getCurrentTuple().getTree());
				} else if (!t.subsumes(goal)) {
					logger.debug("Action " + a + " failed subsumption at tree "
							+ state.getCurrentTuple().getTree());
					logger.debug("Result was:" + t);
					state.removeChild(t);
				} else {
					logger.debug("Action " + a + " succeeded at tree "
							+ state.getCurrentTuple().getTree());
					logger.debug("Result was:" + t.getTree());
				}

			}
		} while (state.goFirst() != null);

	}

	public void applyNonOptionalGrammar() {
		do {
			for (ComputationalAction a : this.nonoptionalGrammar.values()) {
				// if a non-optional action can be carried out, it has to be,
				// with no other computational possibilities
				// on this node

				DAGTuple t = this.state.execAction(a, null);
				if (t == null) {
					logger.debug("Action " + a + " failed at tree "
							+ state.getCurrentTuple().getTree());
				} else {
					logger.debug("Action " + a + " succeeded at tree "
							+ state.getCurrentTuple().getTree());
					logger.debug("Result was:" + t.getTree());
				}

			}
		} while (state.goFirst() != null);

	}

	/**
	 * applies all available actions, i.e. computational and the lexical actions
	 * associated with the top of the words stack, to the current tuple.
	 * 
	 * USED in GENERATION... also checks for subsumption of goal... only adds
	 * edges that subsume
	 */
	private void applyAllActions(ParserTuple goal) {
		// TODO: Exhaustive application of actions.
		// state.removeChildren();
		if (!state.wordStack().isEmpty()) {

			for (LexicalAction la : lexicon.get(state.wordStack().peek().word())) {
				DAGTuple t = this.state.execAction(la, new UtteredWord(la.getWord()));
				if (t == null) {
					logger.debug("Action " + la.getName() + " failed at tree "
							+ state.getCurrentTuple().getTree());
				} else if (!t.subsumes(goal)) {
					logger.debug("Action " + la.getName()
							+ " failed subsumption at tree "
							+ state.getCurrentTuple().getTree());
					logger.debug("Result was:" + t);
					state.removeChild(t);
				} else {
					logger.debug("Action " + la.getName()
							+ " succeeded at tree "
							+ state.getCurrentTuple().getTree());
					logger.debug("Result was:" + t.getTree());
				}

			}

		}

		for (ComputationalAction a : this.optionalGrammar.values()) {
			DAGTuple t = this.state.execAction(a, null);
			if (t == null) {
				logger.debug("Action " + a + " failed at tree "
						+ state.getCurrentTuple().getTree());
			} else if (!t.subsumes(goal)) {
				logger.debug("Action " + a + " failed subsumption at tree "
						+ state.getCurrentTuple().getTree());
				logger.debug("Result was:" + t);
				state.removeChild(t);
			} else {
				logger.debug("Action " + a + " succeeded at tree "
						+ state.getCurrentTuple().getTree());
				logger.debug("Result was:" + t.getTree());
			}
		}

	}

	/**
	 * 
	 * @return true if successful, false if we're at root without any more
	 *         exploration possibilities
	 */
	public boolean attemptBacktrack() {

		while (!state.moreUnseenEdges()) {
			if (this.state.atRoot())
				return false;

			Action backAlong = state.getParentAction();
			if (backAlong instanceof LexicalAction) {

				state.wordStack().push(new UtteredWord(((LexicalAction) backAlong).getWord()));
				logger.debug("adding word to stack, now:" + state.wordStack());
			}
			DAGEdge backOver = this.state.goUpOnce();
			// mark edge that we're back over as seen (already explored)...
			this.state.markOutEdgeAsSeen(backOver);

		}
		logger.debug("Backtrack succeeded");

		return true;
	}

	public boolean completeOnce() {
		do {
			if (!parse())
				return false;

		} while (!state.isComplete());

		return true;
	}

	public ParseState<ParserTuple> complete() {
		ParseState<ParserTuple> complete = new ParseState<ParserTuple>();
		do {
			if (state.getCurrentTuple().getTree().isComplete()) {
				complete.add(state.getCurrentTuple());
			}

		} while (parse());
		state.resetToFirstTupleAfterLastWord();
		return complete;
	}

	@Override
	public boolean parse(List<? extends HasWord> words) {

		for (int i = 0; i < words.size(); i++) {
			ParseState<ParserTuple> state = parseWord(words.get(i).word());
			if (state == null)
				return false;

		}

		return true;
	}

	public boolean parse(String[] words) {
		for (int i = 0; i < words.length; i++) {
			ParseState<ParserTuple> state = parseWord(words[i]);
			if (state == null)
				return false;
		}

		return true;

	}

	@Override
	public boolean parse(List<? extends HasWord> arg0, String arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Generator<ParserTuple> getGenerator() {

		return new DepthFirstGenerator(this);
	}

	@Override
	protected Collection<ParserTuple> execExhaustively(ParserTuple tuple,
			Action action, String word) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ParserTuple execAction(ParserTuple tuple, Action action,
			String word) {

		return tuple.execAction(action, word);
	}

	public void exhaust() {
		while (parse())
			;

	}

	public List<LexicalAction> getLeftAdjustingActions(String word, Tree t) {
		List<LexicalAction> result = new ArrayList<LexicalAction>();
		if (!lexicon.containsKey(word))
			return result;

		state.init(t);
		exhaust();
		for (ParserTuple tuple : state) {
			Collection<LexicalAction> LAs = lexicon.get(word);
			for (LexicalAction la : LAs) {
				Tree r = la.execTupleContext(tuple.getTree(), tuple);
				if (r != null) {
					ArrayList<Action> actions = state.getActionSequence(tuple);
					actions.add(la);
					result.add(new LexicalAction(word, actions));
				}

			}

		}
		return result;
	}

	public Grammar getNonOptionalGrammar() {

		return this.nonoptionalGrammar;
	}

	public Grammar getOptionalGrammar() {

		return this.optionalGrammar;
	}

	@Override
	public ParseState<ParserTuple> getStateWithNBestTuples(int N) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param words
	 * @return the resulting (possibly empty) state, or null if the state became
	 *         empty before seeing the last word
	 */
	public ParseState<ParserTuple> parseWords(List<String> words) {
		for (String word : words) {
			parseWord(new UtteredWord(word));
		}
		return getState();
	}
	public static void main(String[] a) {

		DepthFirstParser parser = new DepthFirstParser(
				"resource/2013-english-ttr");
		String sent = "john likes mary";
		List<String> words = Arrays.asList(sent.split(" "));
		parser.parseWords(words);
		do{
			try{
				System.out.println("Final Tuple:"+parser.getState().getCurrentTuple());
				System.out.println("Press any key to continue.");
				System.in.read();
			} catch(Exception e)
			{}
		}while(parser.parse());
		 
	}

}
