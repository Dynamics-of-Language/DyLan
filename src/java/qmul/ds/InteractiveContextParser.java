package qmul.ds;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.Grammar;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.ActionReplayEdge;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.dag.UtteredWord;
import qmul.ds.dag.VirtualRepairingEdge;
import qmul.ds.dag.WordLevelContextDAG;
import qmul.ds.formula.TTRFormula;
import qmul.ds.tree.Tree;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;

/**
 * An interactive parsing agent with a DAG context as per Eshghi et. al (2015).
 * 
 * Self-repair processing. CR processing. Acknowledgements. Question/Answer
 * pairs....
 * 
 * TODO: (1) Turn repair processing on/off TODO: (2) Fix how tree completion
 * works. Currently it is right-edge tokens like . or ? that take care of it. ??
 * TODO: (3) Add generation: Given current context, i.e. the DAG constructed so
 * far, and a goal concept, generate turn/string such that the maximal semantics
 * of the right-most node on the DAG equals the goal. Generally, there will be
 * two options: (a) local extension including repair/correction; and (b)
 * starting a new clause. At this point, we can force (a) to be preferred and
 * (b) only tried if (a) fails.
 * 
 * 
 * @author Arash
 *
 */
public class InteractiveContextParser extends
		DAGParser<DAGTuple, GroundableEdge> {

	public static final String repair_init_prefix = qmul.ds.dag.BacktrackingEdge.repair_init_prefix;

	static String[] non_repairing = { "accept", "reject", "assert", "question" };
	public static final List<String> non_repairing_action_types = Arrays
			.asList(non_repairing);
	private static Logger logger = Logger
			.getLogger(InteractiveContextParser.class);

	/**
	 * the dialogue context. See {@link Context} for more info.
	 */

	String[] acksa = { "uhu" };
	List<String> acks = Arrays.asList(acksa);
	String[] repairand = { "uhh", "errm", "sorry", "err", "er", "well", "oh",
			"uh" };
	List<String> repairanda = Arrays.asList(repairand);
	String[] punct = { ".", "?", "!" };
	List<String> rightEdgeIndicators = Arrays.asList(punct);

	public InteractiveContextParser(File resourceDir) {
		super(resourceDir, false);
		
		context = new Context<DAGTuple, GroundableEdge>(new WordLevelContextDAG());
		

	}

	/**
	 * @param resourceDirNameOrURL
	 *            the dir containing computational-actions.txt,
	 *            lexical-actions.txt, lexicon.txt
	 */
	public InteractiveContextParser(String resourceDirNameOrURL,
			boolean repairing) {
		super(resourceDirNameOrURL, repairing);
		context = new Context<DAGTuple, GroundableEdge>(new WordLevelContextDAG());
		context.setRepairProcessing(repairing);

	}

	public InteractiveContextParser(String resourceDirOrURL) {
		this(resourceDirOrURL, false);
	}

	public InteractiveContextParser(Lexicon lexicon, Grammar grammar) {
		super(lexicon, grammar);
		
		context = new Context<DAGTuple, GroundableEdge>(new WordLevelContextDAG());
		
	}

	protected boolean repairInitiated() {
		return context.repairInitiated();
	}

	private boolean adjustOnce() {

		if (repairInitiated()) {
			UtteredWord repairWord = getState().wordStack().pop();
			backtrackAndParse(getState().wordStack().peek());
			getState().wordStack().push(repairWord);

		} else if (getState().outDegree(getState().getCurrentTuple()) == 0)
			applyAllPermutations();

		DAGEdge result;
		do {

			result = getState().goFirst();

			if (result != null) {

				break;
			}
		} while (getState().attemptBacktrack());

		return (result != null);

	}

	public boolean parse() {
		if (getState().isExhausted()) {
			logger.info("state exhausted");
			return false;
		}

		do {

			if (!adjustOnce()) {
				logger.info("wordstack:" + getState().wordStack());
				logger.info("depth:" + getState().getDepth());
				getState().setExhausted(true);
				return false;
			}

		} while (!getState().wordStack().isEmpty());
		// commitToContext();
		return true;
	}

	private void applyAllPermutations() {
		if (getState().wordStack().isEmpty())
			return;
		UtteredWord word = getState().wordStack().peek();
		// logger.debug("apply all for word:"+word);
		if (this.rightEdgeIndicators.contains(word.word())) {
			replayBacktrackedActions(word);
		}

		else if (this.acks.contains(word.word())) {
			String lastSpkr = getState().getParentEdge().word().speaker();
			DAGTuple completed = complete(word);
			getState().getParentEdge(completed).groundFor(lastSpkr);
			return;
		}

		Pair<List<Action>, Tree> initPair = new Pair<List<Action>, Tree>(
				new ArrayList<Action>(), getState().getCurrentTuple().tree.clone());

		initPair = adjustWithNonOptionalGrammar(initPair);

		List<Pair<List<Action>, Tree>> global = new ArrayList<Pair<List<Action>, Tree>>();
		global.add(initPair);
		// this is to detect loops. loops are there because of things like
		// AnticipationL, Completion....
		HashMap<ComputationalAction, HashSet<Tree>> tried = new HashMap<ComputationalAction, HashSet<Tree>>();
		for (ComputationalAction action : optionalGrammar.values()) {
			tried.put(action, new HashSet<Tree>());
		}

		for (int i = 0; i < global.size(); i++) {
			Pair<List<Action>, Tree> cur = global.get(i);

			for (ComputationalAction ca : optionalGrammar.values()) {
				if (tried.get(ca).contains(cur.second))
					continue;

				tried.get(ca).add(cur.second);
				Tree res = ca.exec(cur.second.clone(), context);
				logger.debug("Applying ca: " + ca);
				logger.debug("to: " + cur.second);
				logger.debug("result: " + res);
				if (res != null) {
					List<Action> newActions = new ArrayList<Action>(cur.first);
					newActions.add(ca.instantiate());
					Pair<List<Action>, Tree> newPair = new Pair<List<Action>, Tree>(
							newActions, res);
					Pair<List<Action>, Tree> adjusted = adjustWithNonOptionalGrammar(newPair);
					global.add(adjusted);
				}
			}

		}

		for (Pair<List<Action>, Tree> pair : global) {

			UtteredWord w = getState().wordStack().peek();
			logger.trace("top of stack:" + getState().wordStack().peek());
			for (LexicalAction la : lexicon.get(w.word())) {
				// set right-edge indicators (e.g. '.' or '?') and acceptances
				// to not replayable
				// TODO: should be part of the lexical entry? Need to think
				// about this.

				boolean repairable = true;
				logger.debug("executing " + la + " on " + pair.second);
				Tree res = la.exec(pair.second.clone(), context);

				if (res != null) {
					logger.debug("success:"+res);

					ArrayList<Action> newActs = null;

					newActs = new ArrayList<Action>(pair.first);
					GroundableEdge wordEdge;
					newActs.add(la.instantiate());
					if (getIndexOfTRP(newActs) >= 0)
						wordEdge = getState().getNewNewClauseEdge(newActs, w);
					else
						wordEdge = getState().getNewEdge(newActs, w);

					logger.debug("created word edge with word:" + w);
					logger.debug("edge before adding:" + wordEdge);
					if (non_repairing_action_types.contains(la
							.getLexicalActionType()))
						wordEdge.setRepairable(repairable);

					DAGTuple newTuple = getState().getNewTuple(res);

					getState().addChild(newTuple, wordEdge);

					logger.debug("Added Edge:" + wordEdge.toDebugString());
					logger.debug("Child:" + newTuple);

					/**
					 * if the lexical action was acceptance/rejection, manually
					 * set acceptance pointers.
					 * 
					 */
					if (la.getLexicalActionType().equals("reject"))
						getState().setAcceptancePointer(w.speaker(), newTuple);
					else if (la.getLexicalActionType().equals("accept")
							|| la.getLexicalActionType().equals("assert") || la.getLexicalActionType().equals("yes")) {
						getState().setAcceptancePointer(w.speaker(), newTuple);
						for (String spkr : getState().getAcceptancePointers(getState()
								.getParent(newTuple))) {
							logger.info("setting acceptance pointer for:"
									+ spkr);
							getState().setAcceptancePointer(spkr, newTuple);
						}
					}

					// TODO: the problem with doing this in the lexical actions
					// is that
					// the acceptance pointer is a tuple level annotation,
					// rather than a tree-level one.
					// so it cannot be done in the lexical actions, but only
					// after the tuple has been
					// constructed here.
					// Solution: have ds atomic actions return tuples??
					// (probably NOT!) Or make acceptance
					// an annotation on the trees? Or in a ctxt field in the rec
					// types... the latter! (Matt... )

				} else
					logger.trace("unsuccessful");
			}
		}

	}

	public boolean replayBacktrackedActions(UtteredWord word) {

		Tree start = getState().getCurrentTuple().getTree().clone();
		List<Action> acts = new ArrayList<Action>();// this will be the actions
													// that will be replayed..
													// it will be added to as a
													// result of call to
													// trimuntil
		logger.debug("Edges to be replayed:" + getState().getBacktrackedEdges());
		Pair<List<GroundableEdge>, Tree> edgeTreePair = trimUntilApplicable(
				start, getState().getBacktrackedEdges(), acts);

		// logger.debug("got edges back from trim:"
		// + (edgeTreePair == null ? null : edgeTreePair.first));
		// logger.debug("got actions back from trim:" + acts);
		if (edgeTreePair == null || edgeTreePair.first.isEmpty()) {
			logger.debug("Replay: didn't rerun anything from context");
			logger.debug("trimUntilApplicable returned:" + edgeTreePair);
			return false;

		}
		ActionReplayEdge replayEdge = getState().getNewActionReplayEdge(acts,
				new UtteredWord(null, word.speaker()), edgeTreePair.first);
		logger.info("adding replayEdge with actions:" + acts);
		DAGTuple res = getState().getNewTuple(edgeTreePair.second);
		getState().addChild(res, replayEdge);
		getState().getBacktrackedEdges().clear();
		return true;

	}

	

	@Override
	public void newSentence() {

		getState().addAxiom();

	}

	public void init() {
		context.init();


	}
	
	

	public List<Pair<GroundableEdge, DAGTuple>> getLocalGenerationOptions(
			TTRFormula goal) {
		List<Pair<GroundableEdge, DAGTuple>> result = new ArrayList<Pair<GroundableEdge, DAGTuple>>();

		for (String word : lexicon.keySet())
			for (LexicalAction la : lexicon.get(word)) {
				Pair<List<Action>, Tree> res = this.leftAdjustAndApply(la,
						"self", goal);
				if (res != null) {
					GroundableEdge wordEdge;
					UtteredWord w = new UtteredWord(word, "self");
					if (getIndexOfTRP(res.first) >= 0)
						wordEdge = getState().getNewNewClauseEdge(res.first, w);
					else
						wordEdge = getState().getNewEdge(res.first, w);

					logger.debug("created word edge with word:" + w);
					logger.debug("edge before adding:" + wordEdge);
					if (non_repairing_action_types.contains(la
							.getLexicalActionType()))
						wordEdge.setRepairable(false);

					DAGTuple newTuple = getState().getNewTuple(res.second);
					result.add(new Pair<GroundableEdge, DAGTuple>(wordEdge,
							newTuple));

				}

			}

		return result;

	}

	/**
	 * Only generating to propositional semantics. And not incrementally - incremental generation
	 * inevitably involves repair processing (not done yet for generation), and probabilistic, best first parsing/generation,
	 * so that the best path is taken locally. We aren't there yet.
	 * @param goal
	 * @return true of generation to propositional goal is successful.
	 */
	public List<UtteredWord> generateTo(TTRFormula goal)
	{
		ArrayList<UtteredWord> result=new ArrayList<UtteredWord>();
		DAGGenerator<DAGTuple, GroundableEdge> gen=getDAGGenerator();
		gen.setGoal(goal);
		
		if (gen.generate())
		{
			result.addAll(getState().wordStack());
			getState().wordStack().clear();
			getState().thisIsFirstTupleAfterLastWord();
			return result;
		}
		getState().resetToFirstTupleAfterLastWord();
		return null;
	}
	
	public DAGGenerator<DAGTuple, GroundableEdge> getDAGGenerator()
	{
		return new InteractiveContextGenerator(this);
	}
	
	public List<String> generated=new ArrayList<String>();
	private TTRFormula goal;
	
	

	/**
	 * @param word
	 *            , speaker
	 * @return the state which results from extending the current state with all
	 *         possible lexical actions corresponding to the given word; or null
	 *         if the word is not parsable
	 */
	public DAG<DAGTuple, GroundableEdge> parseWord(UtteredWord word) {
		logger.info("Parsing word " + word);
		// state.resetToFirstTupleAfterLastWord();
		// logger.debug("after reset cur is" + state.getCurrentTuple());

		if (this.repairanda.contains(word.word())) {
			this.getState().thisIsFirstTupleAfterLastWord();
			return this.getState();
		}

		Collection<LexicalAction> actions = this.lexicon.get(word.word());
		if (actions == null || actions.isEmpty()) {
			logger.error("Word not in Lexicon: " + word);
			return null;
		}

		getState().wordStack().push(word);
		// state.clear();
		if (!parse()) {
			logger.info("OOPS! Cannot parse:" + word.word()+". Resetting to the state after the last parsable word");
			logger.debug("stack:" + getState().wordStack());
			//state.wordStack().remove(0);
			getState().resetToFirstTupleAfterLastWord();
			if (!getState().repairProcessingEnabled()) {
				logger.info("repair processing is disabled. Reset to state after last parsable word.");
				return null;
			}
			logger.info("Now initiating local repair.");
			getState().wordStack().push(word);
			getState().initiateLocalRepair();

			if (!parse()) {
				logger.info("OOPS! Couldn't parse word as local repair either");

				logger.info("now attempting repair of previous clause");
				return null;
				// state.initiateClauseRepair();
			}
		}

		this.getState().thisIsFirstTupleAfterLastWord();
		logger.info("Parsed " + word);
		logger.info("Final Tuple:" + getState().getCurrentTuple());

		return this.getState();
	}

	public Context<DAGTuple, GroundableEdge> getContext() {
		return context;
	}

	public DAG<DAGTuple, GroundableEdge> generateWord(String word) {
		parseWord(new UtteredWord(word, "self"));
		return getState();
	}

	private GroundableEdge getFirstRepairableEdge() {
		logger.debug("finding first repairable edge:");

		GroundableEdge parent = getState().getParentEdge();
		while (parent != null) {
			if (parent.isRepairable())
				return parent;

			parent = getState().getParentEdge(getState().getSource(parent));
			logger.debug("inside loop");

		}
		return null;
	}

	private void backtrackAndParse(UtteredWord word) {

		for (LexicalAction la : lexicon.get(word.word())) {
			if (non_repairing_action_types.contains(la.getLexicalActionType()))
				return;

			DAGTuple current = getState().getCurrentTuple();
			if (getState().isClauseRoot(current))
				return;

			// GroundableEdge firstRepairable = getFirstRepairableEdge();
			// if (firstRepairable == null) {
			// logger.debug("didn't find repairable edge.");
			// return;
			// }
			// logger.debug("first repariable edge:" + firstRepairable);
			// String speakerOfFirstRepairable =
			// firstRepairable.word().speaker();
			// we want to repair only either the current turn, or the previous.

			GroundableEdge repairableEdge;
			List<GroundableEdge> backtracked = new ArrayList<GroundableEdge>();

			do {
				repairableEdge = getState().getParentEdge(current);
				current = getState().getSource(repairableEdge);
				// shouldn't repair right edges like ? . ! <eot> etc.
				backtracked.add(0, repairableEdge);

				if (!repairableEdge.isRepairable()) {
					logger.debug("edge not repairable:" + repairableEdge);
					continue;
				}

				
				/**
				 * extracting the computational actions from repairable edge.
				 * The same ones should be applicable before the repairing
				 * lexical action.
				 * 
				 */
				List<Action> actions = new ArrayList<Action>(repairableEdge
						.getActions().subList(0,
								repairableEdge.getActions().size() - 1));

				actions.add(la);

				Tree result = applyActions(current.getTree(), actions);

				if (result != null) {
					// now add backtracking edge

					VirtualRepairingEdge repairing = getState().getNewRepairingEdge(
							new ArrayList<GroundableEdge>(backtracked),
							actions, current, word);
					DAGTuple to = getState().getNewTuple(result);
					getState().addChild(to, repairing);

				} else
					logger.debug("could not apply:" + actions + "\n at:"
							+ current.getTree());

			} while (!getState().isClauseRoot(current)
					&& !getState().isBranching(current));
		}

	}



	public static void main(String[] a) {
		InteractiveContextParser parser = new InteractiveContextParser(
				"resource/2015-english-ttr");
		Utterance utt1 = new Utterance("A: john likes mary.");
		Utterance utt2 = new Utterance("B: okay what do you want?");
		Utterance utt3 = new Utterance("A: a ticket.");
		Utterance utt4 = new Utterance("B: okay.");
		parser.parseUtterance(utt1);
		parser.parseUtterance(utt2);
		parser.parseUtterance(utt3);
		parser.parseUtterance(utt4);
		
		System.out.println(parser.context.getGroundedContent("B"));
		
	}
		
		/**
		 * 
		 * this following bit was testing generation..... not finished yet!
		 
		TTRFormula goal;
		if (parser.parseUtterance(utt))
			goal = parser.getState().getCurrentTuple().getSemantics();
		else {
			System.out.println("Failed to construct goal from:" + utt);
			System.out.println("Terminating....");
			return;
		}

		System.out.println("Goal constructed:"+goal);

		parser.init();
		//Utterance firstHalf=new Utterance("A: what is this?");
		//parser.parseUtterance(firstHalf);
		
		List<UtteredWord> generated=parser.generateTo(goal);
		
		System.out.println("Generated:"+generated);
		/**
	


		

	}

	/**
	 * used in generation
	 * 
	 * Currently assumes that a single lexical action is only applicable in one
	 * left-context (returns the first one)
	 * 
	 * @param la
	 * @param goal
	 * @return
	 */
	private Pair<List<Action>, Tree> leftAdjustAndApply(LexicalAction la,
			String selfName, TTRFormula goal) {
		UtteredWord word = new UtteredWord(la.getWord(), selfName);
		if (this.rightEdgeIndicators.contains(word.word())) {
			replayBacktrackedActions(word);
		}

		// else if (this.acks.contains(word.word())) {
		// String lastSpkr = state.getParentEdge().word().speaker();
		// DAGTuple completed = complete(word);
		// state.getParentEdge(completed).groundFor(lastSpkr);
		// return;
		// }

		Pair<List<Action>, Tree> initPair = new Pair<List<Action>, Tree>(
				new ArrayList<Action>(), getState().getCurrentTuple().tree.clone());

		initPair = adjustWithNonOptionalGrammar(initPair);

		List<Pair<List<Action>, Tree>> global = new ArrayList<Pair<List<Action>, Tree>>();
		global.add(initPair);
		// this is to detect loops. loops are there because of things like
		// AnticipationL, Completion....
		HashMap<ComputationalAction, HashSet<Tree>> tried = new HashMap<ComputationalAction, HashSet<Tree>>();
		for (ComputationalAction action : optionalGrammar.values()) {
			tried.put(action, new HashSet<Tree>());
		}

		for (int i = 0; i < global.size(); i++) {
			Pair<List<Action>, Tree> cur = global.get(i);

			for (ComputationalAction ca : optionalGrammar.values()) {
				if (tried.get(ca).contains(cur.second))
					continue;

				tried.get(ca).add(cur.second);
				Tree res = ca.exec(cur.second.clone(), context);
				logger.trace("Applying ca: " + ca);
				logger.trace("to: " + cur.second);
				logger.trace("result: " + res);
				if (res != null) {
					List<Action> newActions = new ArrayList<Action>(cur.first);
					newActions.add(ca.instantiate());
					Pair<List<Action>, Tree> newPair = new Pair<List<Action>, Tree>(
							newActions, res);
					Pair<List<Action>, Tree> adjusted = adjustWithNonOptionalGrammar(newPair);
					global.add(adjusted);
				}
			}

		}

		for (Pair<List<Action>, Tree> pair : global) {

			boolean repairable = true;
			logger.debug("executing " + la + " on " + pair.second);
			Tree res = la.exec(pair.second.clone(), context);

			if (res == null) {
				logger.debug("Failed");
				continue;
			}
			TTRFormula maxSem = res.getMaximalSemantics();
			// ------------------- successful parse

			logger.debug("success");

			logger.debug("Checking " + maxSem + " subsumes " + goal);
			if (!maxSem.subsumes(goal)) {
				logger.debug("Failed");
				continue;
			}
			logger.debug("succeeded");

			ArrayList<Action> newActs = null;

			newActs = new ArrayList<Action>(pair.first);

			newActs.add(la.instantiate());

			return new Pair<List<Action>, Tree>(newActs, res);

		}

		return null;

	}
	
	/**Resets state and parses the dialogue d
	 * @param d
	 * @return the resulting context
	 */
	public Context<DAGTuple, GroundableEdge> parseDialogue(Dialogue d)
	{
		context.init(d.getParticiapnts());
		for(Utterance utt: d)
		{
			if (!parseUtterance(utt))
			{
				return null;
			}
		}
		
		return context;
	}



}
