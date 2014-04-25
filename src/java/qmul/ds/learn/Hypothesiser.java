package qmul.ds.learn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.ContextParser;
import qmul.ds.action.Action;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.Grammar;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.Lexicon;
import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.EffectFactory;
import qmul.ds.action.atomic.Put;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGState;
import qmul.ds.dag.DAGTuple;
import qmul.ds.formula.Formula;
import qmul.ds.tree.Node;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.Label;
import qmul.ds.type.DSType;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;

/**
 * 
 * @author arash
 * 
 */

public class Hypothesiser {

	public static final String HYP_ACTION_PREFIX = "hyp";
	public static final String HYP_ADJUNCTION_PREFIX = "hyp-adj";
	public static final String HYP_SEM_PREFIX = "hyp-sem";
	protected static final String HYP_ADJ_T_PREFIX = "hyp-adj-t";
	private static Logger logger = Logger.getLogger(Hypothesiser.class);

	
	protected Lexicon seedLexicon;
	protected Grammar nonoptionalGrammar;// as determined by the prefix * in
	protected Grammar grammar; // action spec files.
	protected Grammar optionalGrammar;
	protected Set<LexicalHypothesis> targetIndependentHyps;
	protected DAGInductionState state;
	protected Tree target;
	protected String curUnknownSubstring = "";
	protected LexicalHypothesis copyHyp;

	public Hypothesiser(Lexicon seedLexicon, Grammar grammar, Tree start, Tree target) {
		this.target = target;
		this.state = new DAGInductionState(start);
		this.seedLexicon = seedLexicon;
		separateGrammars(grammar);

	}

	public Hypothesiser(String resourceDirOrURL) {
		this.seedLexicon = new Lexicon(resourceDirOrURL);
		separateGrammars(new Grammar(resourceDirOrURL));
		this.state = new DAGInductionState(new Tree());

	}

	public void loadTrainingExample(String sentence, Tree target) {
		if (this.seedLexicon == null || this.optionalGrammar == null || this.nonoptionalGrammar == null) {
			throw new IllegalStateException("Hypothesiser not initialised");
		}

		String[] sent = sentence.trim().split("\\s");

		this.state = new DAGInductionState(Arrays.asList(sent));
		this.target = target;
		this.curUnknownSubstring = "";
		this.hypotheses.clear();

	}

	public void loadTrainingExample(Sentence<Word> sentence, Tree target) {
		logger.info("loading Training Example: " + sentence);
		String sent = "";
		for (HasWord w : sentence)
			sent += w.word() + " ";
		loadTrainingExample(sent.trim(), target);
	}

	protected LexicalHypothesis hypAdjT = null;

	private void separateGrammars(Grammar grammar) {
		this.grammar=new Grammar();
		this.nonoptionalGrammar = new Grammar();
		this.optionalGrammar = new Grammar();
		this.targetIndependentHyps = new HashSet<LexicalHypothesis>();
		for (ComputationalAction a : grammar) {
			if (a.isAlwaysGood())
				this.nonoptionalGrammar.add(a);
			else if (a.getName().startsWith(HYP_ACTION_PREFIX)) {
				if (a.getName().contains("copy"))
					this.copyHyp = new LexicalHypothesis(a, true);
				else if (a.getName().equalsIgnoreCase("hyp-adj-t-generic"))
				{
					this.hypAdjT=new LexicalHypothesis(a, false);
					
				} else if(a.getName().startsWith(HYP_ADJUNCTION_PREFIX)&&!a.getName().startsWith(HYP_ADJ_T_PREFIX))
				{
					this.optionalGrammar.add(a);						
				}
				else
					this.targetIndependentHyps.add(new LexicalHypothesis(a, false));
			} else
				this.optionalGrammar.add(a);

		}
		grammar.addAll(optionalGrammar);
		grammar.addAll(nonoptionalGrammar);
		logger.info("loaded non-optional grammar:" + this.nonoptionalGrammar.size() + " entries");
		logger.info("loaded optional grammar:" + this.optionalGrammar.size() + " entries");
		logger.info("loaded target independent hypotheses:" + this.targetIndependentHyps.size() + " entries");
	}

	/**
	 * @param resourceDir
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public Hypothesiser(String resourceDirOrURL, Tree start, Tree target, List<String> sent) {
		this.seedLexicon = new Lexicon(resourceDirOrURL);
		separateGrammars(new Grammar(resourceDirOrURL));
		this.state = new DAGInductionState(start, sent);
		this.target = target;
	}

	public Hypothesiser(String resourceDir, Tree target, List<String> sent) {
		this.seedLexicon = new Lexicon(resourceDir);
		separateGrammars(new Grammar(resourceDir));
		this.state = new DAGInductionState(sent);
		this.target = target;

	}

	public static String compactPrintSequence(List<DAGEdge> sequence) {
		String s = "";
		for (DAGEdge a : sequence) {
			s += a + " | ";
		}
		return s;
	}

	public static String compactPrintActionSequence(List<Action> actionSequence) {
		String s = "";
		for (Action a : actionSequence) {
			if (a instanceof LexicalHypothesis)
				s += a + " | ";
			else
				s += a.getName() + "|";
		}
		return s;

	}

	public Collection<CandidateSequence> hypothesise() {
		while (hypothesiseOnce())
			;

		return this.hypotheses;

	}

	protected Collection<CandidateSequence> hypotheses = new ArrayList<CandidateSequence>();

	protected void extractCandidateSequenceNow() {
		logger.debug("extracting sequence");
		DAGInductionTuple current = state.getCurrentTuple();
		String curHypWords = "";
		List<Action> curHypActions = new ArrayList<Action>();
		while (!state.isRoot(current)) {
			if (state.getParentAction(current) instanceof LexicalAction) {
				if (!curHypWords.equals("")) {
					System.out.println("Adding new Candidate Sequence");
					hypotheses.add(new CandidateSequence(current, new ArrayList<Action>(curHypActions), curHypWords));

				}
				curHypActions.clear();
				curHypWords = "";
			}

			if ((state.getParentAction(current) instanceof ComputationalAction && !state.getPrevWord(current)
					.equals("")) || (state.getParentAction(current) instanceof LexicalHypothesis)) {
				curHypWords = state.getPrevWord(current);
				curHypActions.add(0, state.getParentAction(current));

			}
			logger.debug("extracting:" + state.getParentAction(current) + ":"
					+ state.getParentAction(current).getClass());
			logger.debug("prev word was" + state.getPrevWord());
			// System.out.println("prev word was"+state.getPrevWord());
			current = state.getParent(current);
		}

		if (!curHypWords.equals("")) {
			hypotheses.add(new CandidateSequence(current, new ArrayList<Action>(curHypActions), curHypWords));
		}

	}

	protected boolean hypothesiseOnce() {
		if (!(state.getCurrentTuple().getTree().subsumes(this.target) && this.target.subsumes(state.getCurrentTuple()
				.getTree()))) {
			if (state.getCurrentTuple().getTree().isComplete()) {
				logger.info("got to complete tree:" + state.getCurrentTuple().getTree());
				logger.info("no equality. target is:" + this.target);
			}

			if (!state.wordStack().isEmpty()) {
				if (this.seedLexicon.containsKey(state.wordStack().peek())) {
					// applying lexical actions for top(wordstack)
					this.applyKnownLexical();

				}

				if (!this.seedLexicon.containsKey(state.wordStack().peek())) {
					while (!state.wordStack().isEmpty() && !this.seedLexicon.containsKey(state.wordStack().peek())) {
						this.curUnknownSubstring += state.wordStack().pop() + " ";
						logger.debug("Unknown word boundary. Setting CurUnknown to:" + this.curUnknownSubstring);
					}
					this.curUnknownSubstring = this.curUnknownSubstring.trim();
					this.applyLexicalHypotheses(target);

				} else if (!this.curUnknownSubstring.isEmpty())
					this.applyLexicalHypotheses(target);

			} else if (!this.curUnknownSubstring.isEmpty())
				this.applyLexicalHypotheses(target);

			// optional grammar always applies
			this.applyOptionalGrammar(target);

		} else if (state.wordStack().isEmpty()) {
			logger.info("Got to target tree with empty word stack");
			logger.info("Successful sequence was:" + compactPrintSequence(state.getSequenceToRoot()));
			logger.info("Will now backtrack");
			this.extractCandidateSequenceNow();
		} else {
			logger.info("Got to target tree with word stack unempty. It is: " + state.wordStack());
			logger.info("Action Sequence was:" + compactPrintSequence(state.getSequenceToRoot()));
			logger.info("Will now backtrack");
		}

		// ParserTuple result=null;
		do {
			DAGEdge traversed = state.goFirst();
			if (traversed != null) {
				if (traversed.getAction() instanceof LexicalAction) {
					this.curUnknownSubstring = "";

					while (!state.wordStack().isEmpty() && !this.seedLexicon.containsKey(state.wordStack().peek())) {
						this.curUnknownSubstring += state.wordStack().pop() + " ";
						logger.debug("Unknown word boundary. Setting CurUnknown to:" + this.curUnknownSubstring);
					}
					this.curUnknownSubstring = this.curUnknownSubstring.trim();

				}
				// non-optional grammar always applies without branching to ANY
				// new DAG tuple
				applyNonOptionalGrammar(target);
				logger.debug("Cur Unknown Words are:" + this.curUnknownSubstring);
				return true;
			}
		} while (attemptBacktrack());

		logger.info("DAG Exhausted");
		return false;
	}

	// assumes word stack non-empty. applies top(wordStack)
	public void applyKnownLexical() {
		for (Action a : this.seedLexicon.get(state.wordStack().peek())) {
			// if a non-optional action can be carried out, it has to be, with
			// no other computational possibilities
			// on this node
			DAGTuple cur = state.getCurrentTuple();
			Tree t = cur.getTree();
			Tree result = a.exec(t.clone(), cur);

			if (result == null) {
				logger.debug("Action " + a + " failed at tree: " + cur.getTree());

			} else if (!result.subsumes(target)) {
				logger.debug("Action " + a + " to " + t);
				logger.debug("failed subsumption result was:" + result);
				logger.debug("Target:" + target);
			} else {
				logger.debug("applied action " + a + " to " + t);
				logger.debug("result was:" + result);
				state.addChild(result, a.instantiate(), state.wordStack().peek());
			}
		}

	}

	/**
	 * 
	 * @return true if successful, false if we're at root without any more exploration possibilities
	 */
	public boolean attemptBacktrack() {
		logger.info("backtracking...");
		while (!state.moreUnseenEdges()) {
			if (this.state.atRoot()) {
				logger.debug("can't backtrack");
				return false;
			}
			Action backAlong = state.getPrevAction();
			if (backAlong instanceof LexicalAction) {
				// if backtracking over known lexical edge, then you're out of
				// hypothesis space. push all unknown words onto stack again.
				// also push the known word that you're backtracking over onto
				// stack.
				if (!this.curUnknownSubstring.isEmpty()) {
					String[] words = this.curUnknownSubstring.split("\\s");
					for (int i = words.length - 1; i >= 0; i--) {
						state.wordStack().push(words[i]);
					}
				}
				state.wordStack().push(((LexicalAction) backAlong).getWord());
				logger.debug("adding words to stack, now:" + state.wordStack());
			} else if (backAlong instanceof ComputationalAction || backAlong instanceof LexicalHypothesis) {
				// if backtracking into hypothesis space, set unknown words.
				this.curUnknownSubstring = state.getParentEdge().word();

			}

			DAGEdge backOver = this.state.goUpOnce();
			// mark edge that we're back over as seen (already explored)...
			this.state.markOutEdgeAsSeen(backOver);

		}
		logger.info("Backtrack succeeded");

		return true;
	}

	public void applyNonOptionalGrammar(Tree target) {
		do {
			for (Action a : this.nonoptionalGrammar) {
				DAGTuple cur = this.state.getCurrentTuple();
				Tree t = cur.getTree();
				Tree result = a.exec(t.clone(), cur);
				if (result == null) {
					logger.debug("Action " + a.getName() + " failed at tree: " + cur.getTree());

				} else if (!result.subsumes(target)) {
					logger.debug("Action " + a.getName() + " failed subsumption at tree: " + cur.getTree());
					logger.debug("result was:" + result);
					logger.debug("target was:" + target);
				} else {
					logger.debug("applied action " + a + " to " + t);
					logger.debug("result was:" + result);
					state.addChild(result, a.instantiate(), this.curUnknownSubstring);
					break;
				}
			}
		} while (state.goFirst() != null);

	}

	public void applyLexicalHypotheses(Tree target) {
		for (LexicalHypothesis a : this.localLexicalHyps(target)) {
			// if a non-optional action can be carried out, it has to be, with
			// no other computational possibilities
			// on this node
			DAGTuple cur = this.state.getCurrentTuple();
			Tree t = cur.getTree();

			Collection<Pair<? extends Action, Tree>> results = null;
			if (a.backtrackOnSuccess()) {
				results = a.execExhaustively(t.clone(), cur);
				logger.debug("Action " + a + "(exhaustive) to " + t);

			} else {
				Tree result = a.exec(t.clone(), cur);
				logger.debug("Action " + a + " to " + t);
				if (result != null) {
					results = new ArrayList<Pair<? extends Action, Tree>>();
					results.add(new Pair<LexicalHypothesis, Tree>(a.instantiate(), result));
				}

			}

			if (results == null) {
				logger.debug("Action " + a + " failed at tree: " + cur.getTree());

			} else {
				for (Pair<? extends Action, Tree> pair : results) {

					if (!pair.second().subsumes(target)) {

						logger.debug("failed subsumption result was:" + pair.second());
						logger.debug("Action instance was:" + pair.first);
					} else {

						logger.debug("Success, result was:" + pair.second());
						logger.debug("Action instance was:" + pair.first());
						state.addChild(pair.second(), pair.first(), this.curUnknownSubstring);

					}
				}
			}
			/*
			 * Tree result = a.exec(t.clone(), cur);
			 * 
			 * if (result == null) { logger.info("Action " + a + " failed at tree: " + cur.getTree());
			 * 
			 * } else if (!result.subsumes(target)) { logger.info("Action " + a + " to " + t);
			 * logger.info("failed subsumption result was:" + result); } else { logger.info("applied action " + a +
			 * " to " + t); logger.info("result was:" + result); logger.info("added child:" + state.addChild(result, a,
			 * this.curUnknownSubstring)); }
			 */
		}

	}

	public void applyOptionalGrammar(Tree target) {
		// this.state.getCurrentTuple().getChildren().clear();
		for (ComputationalAction a : this.optionalGrammar) {
			// if a non-optional action can be carried out, it has to be, with
			// no other computational possibilities
			// on this node
			DAGTuple cur = this.state.getCurrentTuple();

			Tree t = cur.getTree();
			Collection<Pair<? extends Action, Tree>> results = null;
			if (a.backtrackOnSuccess()) {
				results = a.execExhaustively(t.clone(), cur);
				logger.debug("Action " + a + "(exhaustive) to " + t);

			} else {
				Tree result = a.exec(t.clone(), cur);
				logger.debug("Action " + a + " to " + t);
				if (result != null) {
					results = new ArrayList<Pair<? extends Action, Tree>>();
					results.add(new Pair<ComputationalAction, Tree>(a.instantiate(), result));
				}
			}

			if (results == null) {
				logger.debug("Action " + a + " failed at tree: " + cur.getTree());

			} else {
				for (Pair<? extends Action, Tree> pair : results) {

					if (!pair.second().subsumes(target)) {

						logger.debug("failed subsumption result was:" + pair.second());
						logger.debug("Action instance was:" + pair.first);
					} else {

						logger.debug("Success, result was:" + pair.second());
						logger.debug("Action instance was:" + pair.first());
						state.addChild(pair.second(), pair.first(), this.curUnknownSubstring);

					}
				}
			}
		}
	}

	protected boolean isTerminalIn(Tree target, NodeAddress address) {
		if (target.containsKey(address)) {
			if (target.containsKey(address.down0()) || target.containsKey(address.down1()))
				return false;
			else
				return true;
		} else
			throw new IllegalArgumentException("Address does not exist in goal tree");
	}

	protected Set<LexicalHypothesis> localLexicalHyps(Tree target) {
		logger.info("Hypothesising local lexical actions");
		Tree t = state.getCurrentTuple().getTree();
		NodeAddress pointer = t.getPointer();

		if (!pointer.isFixed()) {
			logger.debug("on unfixed tree, pointer:" + pointer);
			Set<LexicalHypothesis> result = new HashSet<LexicalHypothesis>();
			Node node = t.get(pointer);
			// if node already has a type, nothing to do locally
			// if (node.hasType()) {
			// logger.info("unfixed node already has type, nothing to do locally");
			// return new HashSet<LexicalHypothesis>();
			// }

			DSType curNodeType = node.getRequiredType() == null ? node.getType() : node.getRequiredType();
			if (node.getType() == null && node.getRequiredType() == null)
				throw new IllegalArgumentException("unfixed node has neither required type, nor type");
			for (NodeAddress a : target.keySet()) {
				if (pointer.subsumes(a) && curNodeType.equals(target.get(a).getType())) {
					result.addAll(localLexicalHyps(target, a));
				}
			}
			return result;

		} else
			return localLexicalHyps(target, pointer);

	}

	/**
	 * 
	 * Hypothesise assuming that fixedOnTarget is the address of the (t-under-construction) pointed node on the target
	 * tree. e.g. if the pointed node is fixed this will just be the same address on target. but if the pointed node is
	 * unfixed this will be a hypothesised merge point, i.e. a compatible node in terms of address and type on the
	 * target tree.
	 * 
	 */

	protected Set<LexicalHypothesis> localLexicalHyps(Tree target, NodeAddress fixedOnTarget) {

		Tree t = state.getCurrentTuple().getTree();
		if (!target.containsKey(fixedOnTarget)) {
			logger.warn("address not on target:" + target);
			return new HashSet<LexicalHypothesis>();
		}
		NodeAddress pointer = t.getPointer();
		Node node = t.get(pointer);

		Node targetNode = target.get(fixedOnTarget);
		Set<LexicalHypothesis> set = new HashSet<LexicalHypothesis>(targetIndependentHyps);
		// logger.info("Supposedly fixed pointer address on target:"+fixedOnTarget);
		if (node.hasType() || !isTerminalIn(t, t.getPointer())) {
			return set;
		}

		if (isTerminalIn(target, fixedOnTarget)) {

			// only if terminal will we hypothesise copying action
			if (copyHyp != null) {
				logger.debug("terminal in target, adding copy_hyp");
				set.add(this.copyHyp);
			}

			List<Effect> putList = new ArrayList<Effect>();
			boolean manifest = false;
			Formula f = null;
			for (Label l : targetNode) {
				if (node.hasLabel(l))
					continue;
				if (l instanceof FormulaLabel) {
					FormulaLabel fl = (FormulaLabel) l;
					f = fl.getFormula();
					manifest = f.hasManifestContent();
				}

				putList.add(EffectFactory.create(Put.FUNCTOR + "(" + l + ")"));
			}

			if (putList.isEmpty()) {
				logger.info("no unification hyps");
				// return new HashSet<LexicalHypothesis>();
			} else {
				set.add(new LexicalHypothesis("hyp-sem(" + f + ")", putList, manifest));
				// return set;
			}

		} else {
			logger.debug("was not terminal in target " + target);
		}

		return set;

	}

	public static void main(String args[]) {

		ContextParser parser = new ContextParser("resource/2009-english-test-induction");
		parser.init();
		// String[] sentArray = {"a", "swearer", "who", "a", "swearer", "reflected", "reflected", "a", "swearer"};
		String[] sentArray = { "a", "swearer", "reflected", "a", "swearer" };
		List<String> sent = Arrays.asList(sentArray);
		parser.parseWords(sent);
		Tree complete = parser.getBestParse();
		System.out.println(complete);

		// String[] sentArrayL = {"a", "swearer", "who", "a", "swearer", "reflected", "reflected", "a", "swearer"};
		String[] sentArrayL = { "a", "swearer", "reflected", "a", "swearer" };
		List<String> sentLearn = Arrays.asList(sentArrayL);

		Hypothesiser h = new Hypothesiser("resource/2009-english-test-induction-seed", complete, sentLearn);
		Collection<CandidateSequence> hyps = h.hypothesise();
		System.out.println(hyps.size() + " candidate sequences returned");
		printHypMap(hyps);

	}

	public static String replaceLast(String text, String regex, String replacement) {
		return text.replaceFirst("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
	}

	public static void printHypMap(Collection<CandidateSequence> hyps) {

		int num = 0;
		for (CandidateSequence seq : hyps) {
			System.out.println("Candidate Sequence " + (num++) + ":");
			System.out.println(seq);
		}

	}

}
