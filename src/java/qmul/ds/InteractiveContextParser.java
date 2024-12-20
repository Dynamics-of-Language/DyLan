package qmul.ds;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.stanford.nlp.util.Pair;
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
import qmul.ds.formula.Formula;
import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.gui.ParserPanel;
import qmul.ds.tree.Tree;

/**
 * This is the Dynamic Syntax dialogue parser as per <a href=
 * "https://drive.google.com/open?id=1iG1aD2_42UE9TWyT7_hACPg-OQH0hrkg">Eshghi
 * et. al (2015)</a>.
 * 
 * The parser is best-first and constructs an explicit Directed Asyclic Graph
 * (implemented as {@link WordLevelContextDAG}) which acts both as
 * the current parse state, as well as the dialogue context.
 * 
 * The parser currently supports Self-Repair processing, Restarts, Communicative
 * Grounding, Short-Answers and other forms of ellipsis.
 * 
 * 
 * 
 * @author Arash
 *
 */

public class InteractiveContextParser extends DAGParser<DAGTuple, GroundableEdge> {

	public static final String DEFAULT_NAME = Utterance.defaultSpeaker;

	public static final String repair_init_prefix = qmul.ds.dag.BacktrackingEdge.repair_init_prefix;

	/**
	 * List of action types that cannot be repaired, e.g. cannot do: John like Mary
	 * . sorry ?
	 * 
	 */
	static String[] non_repairing = { "accept", "reject", "assert", "question" };
	public static final List<String> non_repairing_action_types = Arrays.asList(non_repairing);
	private static Logger logger = Logger.getLogger(InteractiveContextParser.class);

	/**
	 * the dialogue context. See {@link Context} for more info.
	 */

	String[] acksa = { "uhu" };
	List<String> acks = Arrays.asList(acksa);
	String[] repairand = { "uhh", "errm", "err", "er", "uh", "erm", "uhm", "um", "oh" };
	String[] restarter = { "yeah" };
	List<String> repairanda = Arrays.asList(repairand);
	String[] forceRepairand = { "sorry", "oops", "wait", "erm" };
	List<String> forcedRepairanda = Arrays.asList(forceRepairand);

	List<String> restarters = Arrays.asList(restarter);
	String[] punct = { ".", "?", "!" };
	List<String> rightEdgeIndicators = new ArrayList<String>();// Arrays.asList(punct);

	private boolean forcedRestart = false;
	private boolean forcedRepair = false;

	public static final String RELEASE_TURN = Utterance.RELEASE_TURN_TOKEN;
	public static final String WAIT = Utterance.WAIT;

	/**
	 * Determines the maximum number of previous positions that the parser
	 * backtracks to for repair; so in e.g. I like john um mary, mary can only
	 * repair john with a max depth of 1. If this were 2 it could also repair 'I'.
	 * With this set to 1, the repair is always local.
	 */
	public static final int max_repair_depth = 1;

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";

	public InteractiveContextParser(File resourceDir) {
		super(resourceDir);

		context = new Context<DAGTuple, GroundableEdge>(new WordLevelContextDAG(), this.sa_grammar, DEFAULT_NAME);

	}

	/**
	 * A new InteractiveContext Parser.
	 * 
	 * @param resourceDirNameOrURL name of folder containing the grammar
	 * @param repairing            whether repair processing is enabled
	 * @param participants         the participants in the conversation to be
	 *                             processed by the parser.
	 */
	public InteractiveContextParser(String resourceDirNameOrURL, boolean repairing, String... participants) {
		super(resourceDirNameOrURL);
		if (participants.length > 0)
			context = new Context<DAGTuple, GroundableEdge>(new WordLevelContextDAG(), this.sa_grammar, participants);
		else
			context = new Context<DAGTuple, GroundableEdge>(new WordLevelContextDAG(), this.sa_grammar, DEFAULT_NAME);

		context.setRepairProcessing(repairing);
	}

	/**
	 * Added by AA to support specifying parameter top-N for learned lexical actions.
	 */
	public InteractiveContextParser(String resourceDirNameOrURL, boolean repairing, int topN,String... participants) {
		super(resourceDirNameOrURL, topN);  // The difference is here (calls the constructor with topN).
		if (participants.length > 0)
			context = new Context<DAGTuple, GroundableEdge>(new WordLevelContextDAG(), this.sa_grammar, participants);
		else
			context = new Context<DAGTuple, GroundableEdge>(new WordLevelContextDAG(), this.sa_grammar, DEFAULT_NAME);

		context.setRepairProcessing(repairing);
	}

	public InteractiveContextParser(String resourceDirOrURL, String... participants) {
		this(resourceDirOrURL, false, participants);
	}

	public InteractiveContextParser(String resourceDirOrURL) {
		this(resourceDirOrURL, false);
	}


	/**
	 * Added by AA to support specifying parameter top-N for learned lexical actions.
	 */
	public InteractiveContextParser(String resourceDirOrURL, int topN) {
		this(resourceDirOrURL, false, topN);
	}


	public InteractiveContextParser(Lexicon lexicon, Grammar grammar) {
		super(lexicon, grammar);
		context = new Context<DAGTuple, GroundableEdge>(new WordLevelContextDAG(), DEFAULT_NAME);
	}



	public InteractiveContextParser(Lexicon lexicon, Grammar grammar, ParserPanel p) {
		super(lexicon, grammar);

		context = new Context<DAGTuple, GroundableEdge>(new WordLevelContextDAG(), DEFAULT_NAME);

	}

	public String getName() {
		return context.getName();
	}

	protected synchronized boolean repairInitiated() {
		return context.repairInitiated();
	}

	private boolean adjustOnce(Formula goal) {

		if (repairInitiated()) {
			logger.info("Repair initiated");
			if (this.forcedRestart) {
				logger.debug("restarting");
				UtteredWord repairWord = getState().wordStack().pop();
				logger.debug("repair word:" + repairWord);

				restart(getState().wordStack().peek());
				getState().wordStack().push(repairWord);
			} else {
				logger.debug("stack now:" + getState().wordStack());
				UtteredWord repairWord = getState().wordStack().pop();
				logger.debug("repair word:" + repairWord);

				backtrackAndParse(getState().wordStack().peek());
				getState().wordStack().push(repairWord);
			}
		} else if (getState().outDegree(getState().getCurrentTuple()) == 0)
			applyAllPermutations(goal);

		DAGEdge result;
		do {

			result = getState().goFirst();

			if (result != null) {

				break;
			}
		} while (getState().attemptBacktrack());

		return (result != null);

	}

	public synchronized boolean parse(Formula goal) {
		if (getState().isExhausted()) {
			logger.debug("state exhausted");
			return false;
		}

		do {

			if (!adjustOnce(goal)) {
				logger.debug("wordstack:" + getState().wordStack());
				logger.debug("depth:" + getState().getDepth());
				getState().setExhausted(true);
				return false;
			}

		} while (!getState().wordStack().isEmpty());
		// commitToContext();
		return true;
	}

	private void applyAllPermutations(Formula goal) {
		if (getState().wordStack().isEmpty())
			return;
		
		//in this general version of the method we are passing the potential generation goal as argument
		// if it's null then it's parsing
		// if it not null: ONLY when we parse (generate) the final word on the stack (i.e. the word we were
		// originally going to parse / generate, and not anything we may have pushed onto it because of backtracking)
		// we need to check that the result subsumes the goal
		
		// TTRRecordType goal = g!=null ? (TTRRecordType)g : null;

		UtteredWord word = getState().wordStack().peek();
		
		
		
		// logger.debug("apply all for word:"+word);
		if (this.rightEdgeIndicators.contains(word.word())) {
			replayBacktrackedActions(word);
		} else if (this.acks.contains(word.word())) {
			String lastSpkr = getState().getParentEdge().word().speaker();
			DAGTuple completed = complete(word);
			getState().getParentEdge(completed).groundFor(lastSpkr);
			return;
		}
		
		/**
		 * First we are going to apply the actions that don't have any left adjustment
		 * 
		 */
		Collection<LexicalAction> allActions = lexicon.get(word.word());
		Collection<LexicalAction> leftAdjustActions = new ArrayList<LexicalAction>();

		Tree current = getState().getCurrentTuple().getTree().clone();
		for (LexicalAction la : allActions) {
			if (la.requiresLeftAdjustment()) {
				leftAdjustActions.add(la);
			} else {

				logger.debug("applying " + la + " without left adjustment");
				logger.debug("to tree: " + current);
				Tree res = la.exec(current, context);
				logger.debug("result: " + res);
				if (res != null) {

					DAGTuple tuple = getState().getNewTuple(res);
					// check subsumption to goal if it's not null
					// if not subsumed continue
					TTRFormula cur = tuple.getSemantics(this.context);
					TTRFormula headLess = cur.removeHead();
					
					if (goal != null && getState().wordStack().size()==1 && !headLess.subsumes(goal))
						continue;

					List<Action> edgeActs = new ArrayList<Action>();
					edgeActs.add(la);
					GroundableEdge edge = getState().getNewEdge(edgeActs, word);
					getState().addChild(tuple, edge);

				}

			}
		}

		if (leftAdjustActions.isEmpty())
			return;

		/**
		 * Now we do the left adjustment:
		 */
		Pair<List<Action>, Tree> initPair = new Pair<List<Action>, Tree>(new ArrayList<Action>(),
				getState().getCurrentTuple().tree.clone());

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
				logger.debug("Applying ca: " + ca);
				logger.debug("to: " + cur.second);

				Tree res = ca.exec(cur.second.clone(), context);
				logger.debug("result: " + res);
				if (res != null) {
					List<Action> newActions = new ArrayList<Action>(cur.first);
					newActions.add(ca.instantiate());
					Pair<List<Action>, Tree> newPair = new Pair<List<Action>, Tree>(newActions, res);
					Pair<List<Action>, Tree> adjusted = adjustWithNonOptionalGrammar(newPair);
					global.add(adjusted);
				}
			}

		}
		logger.debug("Now attempting to apply lexical action for:" + getState().wordStack().peek());
		for (Pair<List<Action>, Tree> pair : global) {

			logger.debug("top of stack:" + word);
			for (LexicalAction la : leftAdjustActions) {
				// set right-edge indicators (e.g. '.' or '?') and acceptances
				// to not replayable
				// TODO: should be part of the lexical entry? Need to think
				// about this.

				// boolean repairable = true;
				logger.debug("applying la '" + la + "':" + la.getLexicalActionType() + " on " + pair.second);
				logger.debug("top of stack:" + getState().wordStack().peek());
				Tree res = la.exec(pair.second.clone(), context);
				

				logger.debug("Floor is open:" + context.floorIsOpen());
				if (res != null) {
					logger.debug("success:" + res);
					TTRFormula f = res.getMaximalSemantics(context);
					TTRFormula headLess = f.removeHead();
					
					/**
					 * The significance of getState().wordStack().size == 1 is that we only want to check goal subsumption
					 * when parsing the final word on the stack, and not anything before that which we may have pushed 
					 * onto the stack because of backtracking. The problem is when processing repair sequences, and where
					 * the goal has changed.
					 */
					if (goal != null && getState().wordStack().size()==1 && !headLess.subsumes(goal)) {
						logger.debug("Oops. Cur semantics:" + f);
						logger.debug("does not subsume goal:" + goal);
						logger.debug("Stack is:"+getState().wordStack());
						continue;
					}
					
					ArrayList<Action> newActs = null;

					newActs = new ArrayList<Action>(pair.first);
					GroundableEdge wordEdge;
					newActs.add(la.instantiate());
					//
					int indexOfTRP = getIndexOfTRP(newActs);
					if (indexOfTRP > 0) {

						logger.debug("Found TRP in the middle of sequence");
						Tree beforeTRP = null;
						// create two edges, one before trp, and one after
						for (Pair<List<Action>, Tree> pair1 : global) {
							logger.debug("Looking for before trp:" + pair1.first);
							if (pair1.first().equals(pair.first.subList(0, indexOfTRP))) {
								logger.debug("found it:" + pair1.second());
								beforeTRP = pair1.second();
								break;
							}

						}
						if (beforeTRP == null)
							throw new IllegalStateException("Couldn't find sublist");

						DAGTuple beforeTRPTuple = getState().getNewTuple(beforeTRP);
						// UtteredWord completionWord=new UtteredWord(".", w.speaker());

						GroundableEdge completionEdge = getState()
								.getNewCompletionEdge(new ArrayList<Action>(pair.first.subList(0, indexOfTRP)));
						completionEdge.setRepairable(false);
						getState().addChild(beforeTRPTuple, completionEdge);

						logger.debug("Added Completion Edge:" + completionEdge);
						logger.debug("Child:" + beforeTRPTuple);
						logger.debug("going forward along it");
						getState().setCurrentTuple(getState().getDest(completionEdge));

						wordEdge = getState().getNewEdge(newActs.subList(indexOfTRP, newActs.size()), word);
						if (non_repairing_action_types.contains(la.getLexicalActionType()))
							wordEdge.setRepairable(false);

						DAGTuple newTuple = getState().getNewTuple(res);

						getState().addChild(newTuple, wordEdge);

						logger.debug("Added Edge:" + wordEdge.toDebugString());
						logger.debug("Child:" + newTuple);
						getState().setCurrentTuple(getState().getSource(completionEdge));

						continue;

					} else
						wordEdge = getState().getNewEdge(newActs, word);

					logger.debug("created word edge with word:" + word);
					logger.debug("edge before adding:" + wordEdge);

					if (non_repairing_action_types.contains(la.getLexicalActionType()))
						wordEdge.setRepairable(false);

					DAGTuple newTuple = getState().getNewTuple(res);

					getState().addChild(newTuple, wordEdge);

					logger.debug("Added Edge:" + wordEdge.toDebugString());
					logger.debug("Child:" + newTuple);

				} else
					logger.debug("unsuccessful");
			}
		}

	}

	/**
	 * Action replqy not working... TODO TODO
	 * 
	 * commenting it out in applyAllPermutaions.
	 * 
	 * 
	 */
	public boolean replayBacktrackedActions(UtteredWord word) {

		Tree start = getState().getCurrentTuple().getTree().clone();
		List<Action> acts = new ArrayList<Action>();// this will be the actions
													// that will be replayed..
													// it will be added to as a
													// result of call to
													// trimuntil
		logger.debug("Edges to be replayed:" + getState().getBacktrackedEdges());
		Pair<List<GroundableEdge>, Tree> edgeTreePair = trimUntilApplicable(start, getState().getBacktrackedEdges(),
				acts);

		// logger.debug("got edges back from trim:"
		// + (edgeTreePair == null ? null : edgeTreePair.first));
		// logger.debug("got actions back from trim:" + acts);
		if (edgeTreePair == null || edgeTreePair.first.isEmpty()) {
			logger.debug("Replay: didn't rerun anything from context");
			logger.debug("trimUntilApplicable returned:" + edgeTreePair);
			return false;

		}
		ActionReplayEdge replayEdge = getState().getNewActionReplayEdge(acts, new UtteredWord(null, word.speaker()),
				edgeTreePair.first);
		logger.debug("adding replayEdge with actions:" + acts);
		DAGTuple res = getState().getNewTuple(edgeTreePair.second);
		getState().addChild(res, replayEdge);
		getState().getBacktrackedEdges().clear();
		return true;

	}

	@Override
	public synchronized void newSentence() {
		getState().addAxiom();
	}

	public synchronized void init() {
		this.forcedRepair = false;
		this.forcedRestart = false;
		context.init();
	}

	public synchronized void init(List<String> participants) {
		context.init(participants);
	}

	public synchronized List<TTRRecordType> getTopNPending(int i) {
		List<TTRRecordType> result = new ArrayList<TTRRecordType>();

		Tree current = context.getCurrentTuple().getTree();

		if (!current.getAsserters().isEmpty()) {
			result.add(new TTRRecordType());
			return result;
		} else
			return getNBestFinalSemantics(i);

	}



	public synchronized DAG<DAGTuple, GroundableEdge> generateWord(UtteredWord word, Formula goal)
	{
		logger.info("Attempting to generate word: "+word);
		logger.info("With goal: " + goal);
		
		Collection<LexicalAction> actions = this.lexicon.get(word.word());
		if (actions == null || actions.isEmpty()) {
			logger.error("Word not in Lexicon: " + word);
			return null;
		}

		getState().wordStack().push(word);

		if (!parse(goal)) {
			logger.info("Cannot generate:" + word.word() + ". Resetting to the state after the last parsable word");
			logger.debug("stack:" + getState().wordStack());
			// state.wordStack().remove(0);
			getState().resetToFirstTupleAfterLastWord();
			return null;
			
		

		}

		if (word.word().equals(RELEASE_TURN))
			this.context.openFloor();
		else
			this.context.setWhoHasFloor(word.speaker());

		this.getState().thisIsFirstTupleAfterLastWord();
		this.getState().setRepairProcessing(false);
		logger.info("Generated " + word);
		logger.debug("Final Tuple:" + getState().getCurrentTuple());
		logger.info("Sem:" + getState().getCurrentTuple().getSemantics(context));

		return this.getState();
	}
	
	
	/**
	 * @param w , speaker
	 * @return the state which results from extending the current state with all
	 *         possible lexical actions corresponding to the given word; or null if
	 *         the word is not parsable
	 * 
	 */
	public synchronized DAG<DAGTuple, GroundableEdge> parseWord(UtteredWord w) {
		UtteredWord word = new UtteredWord(w.word().toLowerCase(), w.speaker());
		logger.info("Parsing word: " + word);
		// set addressee of utterance if inferrable (in the dyadic case):
		List<String> participants = new ArrayList<String>(context.getParticipants());
		if (context.getParticipants().size() == 2) {
			if (participants.indexOf(word.speaker()) == 0)
				word.setAddressee(participants.get(1));
			else
				word.setAddressee(participants.get(0));
		}

		if (word.word().equals(WAIT)) {
			return getState();
		}

		if (this.forcedRestart || this.forcedRepair) {

			logger.info("initiating restart or repair");
			getState().wordStack().push(word);
			getState().initiateLocalRepair();

			if (!parse()) {
				logger.info("OOPS! Couldn't parse word as restart or repair");
				logger.error(ANSI_PURPLE + "OOPS! Couldn't parse word as restart or repair" + ANSI_RESET);
				this.forcedRestart = false;
				this.forcedRepair = false;
				return null;
			}

			this.forcedRestart = false;
			this.forcedRepair = false;
			this.getState().setRepairProcessing(false);
			this.getState().thisIsFirstTupleAfterLastWord();
			this.getState().setRepairProcessing(false);
			logger.info("Parsed " + word);
			logger.debug("Final Tuple:" + getState().getCurrentTuple());
			logger.info("Sem:" + getState().getCurrentTuple().getSemantics(context));

			return this.getState();

		}

		if (this.repairanda.contains(word.word())) {
			logger.info("repair possible");
			this.getState().thisIsFirstTupleAfterLastWord();
			this.getState().setRepairProcessing(true);
			return this.getState();
		} else if (this.restarters.contains(word.word()) && getState().repairProcessingEnabled()) {
			logger.info("forcing restart on next word");
			this.forcedRestart = true;
			this.getState().thisIsFirstTupleAfterLastWord();
			return this.getState();
		} else if (this.forcedRepairanda.contains(word.word())) {
			logger.info("forcing repair on next word");
			this.forcedRepair = true;
			this.getState().thisIsFirstTupleAfterLastWord();
			this.getState().setRepairProcessing(true);
			return this.getState();
		}

		Collection<LexicalAction> actions = this.lexicon.get(word.word());
		if (actions == null || actions.isEmpty()) {
			logger.error(ANSI_YELLOW + "Word not in Lexicon: " + word + ANSI_RESET);
			return null;
		}

		getState().wordStack().push(word);

		if (!parse()) {
//			logger.info("OOPS! Cannot parse:" + word.word() + ". Resetting to the state after the last parsable word"); // AA commented out
			logger.error("OOPS! Cannot parse: " + word.word() + ". Resetting to the state after the last parsable word...");
			logger.debug("stack:" + getState().wordStack());
			// state.wordStack().remove(0);
			getState().resetToFirstTupleAfterLastWord();
			if (!getState().repairProcessingEnabled()) {
				logger.debug("repair processing is disabled. Reset to state after last parsable word.");
				return null;
			}

			logger.info("Now initiating local repair with:" + word);

			getState().wordStack().push(word);
			getState().initiateLocalRepair();

			if (!parse()) {
				logger.info("OOPS! Couldn't parse word as local repair either");
				logger.error("OOPS! Couldn't parse word as local repair either");
				logger.info("Trying to return to state after the last parsable word");
				getState().resetToFirstTupleAfterLastWord();
				return null;
				// state.initiateClauseRepair();
			}
		}

		if (word.word().equals(RELEASE_TURN))
			this.context.openFloor();
		else
			this.context.setWhoHasFloor(word.speaker());

		this.getState().thisIsFirstTupleAfterLastWord();
		this.getState().setRepairProcessing(false);
		logger.info("Parsed: " + word);
		logger.debug("Final Tuple: " + getState().getCurrentTuple());
		logger.info("Sem: " + getState().getCurrentTuple().getSemantics(context));

		return this.getState();
	}

	public Context<DAGTuple, GroundableEdge> getContext() {
		return context;
	}

	public synchronized DAG<DAGTuple, GroundableEdge> generateWord(String word) {
		parseWord(new UtteredWord(word, "self"));
		return getState();
	}

	private void restart(UtteredWord word) {
		logger.debug("restarting with: " + word);
		DAGTuple before = getState().getCurrentTuple();
		logger.debug("dag pointer on:" + before);
		for (LexicalAction la : lexicon.get(word.word())) {
			if (non_repairing_action_types.contains(la.getLexicalActionType()))
				return;

			DAGTuple current = getState().getCurrentTuple();
			if (getState().isClauseRoot(current))
				return;

			GroundableEdge repairableEdge;
			List<GroundableEdge> backtracked = new ArrayList<GroundableEdge>();

			do {

				repairableEdge = getState().getParentEdge(current);

				// if the repairable is grounded for the repairing speaker we
				// can't repair it.
				if (repairableEdge.isGroundedFor(word.speaker()))
					break;

				logger.debug("back over:" + repairableEdge);
				current = getState().getSource(repairableEdge);
				// shouldn't repair right edges like ? . ! <eot> etc.
				backtracked.add(0, repairableEdge);

				if (!repairableEdge.isRepairable()) {
					logger.debug("edge not repairable:" + repairableEdge);
					continue;
				}

				UtteredWord repairableWord = repairableEdge.word();

				if (repairableWord.word().startsWith("good") && word.word().startsWith("good")
						&& repairableWord.speaker().equals(word.speaker())) {

				} else if (!repairableWord.equals(word))
					continue;

				/**
				 * extracting the computational actions from repairable edge. The same ones
				 * should be applicable before the repairing lexical action.
				 * 
				 */
				List<Action> actions = new ArrayList<Action>(
						repairableEdge.getActions().subList(0, repairableEdge.getActions().size() - 1));

				actions.add(la);

				getState().setCurrentTuple(current);
				Tree result = applyActions(current.getTree(), actions);
				getState().setCurrentTuple(before);
				if (result != null) {
					// now add backtracking edge
					logger.debug("Adding VirutualReparingEdge");
					logger.debug("from " + current);
					VirtualRepairingEdge repairing = getState()
							.getNewRepairingEdge(new ArrayList<GroundableEdge>(backtracked), actions, current, word);
					DAGTuple to = getState().getNewTuple(result);
					getState().addChild(to, repairing);
					logger.debug("to " + to);
					break;
				} else {
					logger.debug("could not apply:" + actions + "\n at:" + current.getTree());
					logger.error("this shouldn't happen. Same word... ");
				}
			} while (!getState().isClauseRoot(current));
		}
		getState().setCurrentTuple(before);
	}


	private void backtrackAndParse(UtteredWord word) {
		logger.info("backtrack and parsing " + word);
		DAGTuple before = getState().getCurrentTuple();
		for (LexicalAction la : lexicon.get(word.word())) {
			if (non_repairing_action_types.contains(la.getLexicalActionType()))
				return;

			logger.debug("trying: " + la + ":" + la.getLexicalActionType());
			DAGTuple current = getState().getCurrentTuple();
			if (getState().isClauseRoot(current))
				return;

			int depth = 0;
			GroundableEdge repairableEdge;
			List<GroundableEdge> backtracked = new ArrayList<GroundableEdge>();

			do {

				repairableEdge = getState().getParentEdge(current);

				// if the repairable is grounded for the repairing speaker we
				// can't repair it.
				if (repairableEdge.isGroundedFor(word.speaker()))
					break;

				logger.debug("back over:" + repairableEdge);
				current = getState().getSource(repairableEdge);
				// shouldn't repair right edges like ? . ! <eot> etc.
				backtracked.add(0, repairableEdge);

				if (!repairableEdge.isRepairable()) {
					logger.debug("edge not repairable:" + repairableEdge);
					continue;
				}

				/**
				 * extracting the computational actions from repairable edge. The same ones
				 * should be applicable before the repairing lexical action.
				 * 
				 */
				List<Action> actions = new ArrayList<Action>(
						repairableEdge.getActions().subList(0, repairableEdge.getActions().size() - 1));

				actions.add(la);
				getState().setCurrentTuple(current);
				Tree result = applyActions(current.getTree(), actions);
				getState().setCurrentTuple(before);
				if (result != null) {
					// now add backtracking edge
					logger.info("Adding VirutualReparingEdge");
					logger.info("from " + current);
					VirtualRepairingEdge repairing = getState()
							.getNewRepairingEdge(new ArrayList<GroundableEdge>(backtracked), actions, current, word);
					DAGTuple to = getState().getNewTuple(result);
					getState().addChild(to, repairing);
					logger.debug("to " + to);
					depth++;
					logger.debug("depth is:" + depth);

				} else
					logger.debug("could not apply:" + actions + "\n at:" + current.getTree());

			} while (depth < max_repair_depth && !getState().isClauseRoot(current) && !getState().isBranching(current));

			logger.debug("finished trying:" + la + ":" + la.getLexicalActionType());
		}

	}

	public static void main(String[] a) throws IOException {

		InteractiveContextParser parser = new InteractiveContextParser("resource/2016-english-ttr-restaurant-search");

		// File folder=new File("corpus/bAbI+");

		List<Dialogue> dialogues = Dialogue
				.loadDialoguesFromFile("../babble/data/Domains/restaurant-search/training_dialogues");

		for (Dialogue d : dialogues) {
			if (parser.parseDialogue(d) == null) {
				System.out.println("Failed to parse:\n" + d);
				break;
			} else {
				System.out.println("parsed dialogue successfully");
				System.out.println("Grounded content is:");
				System.out.println(parser.getContext().getCautiouslyOptimisticGroundedContent());
			}

		}

	}

	/**
	 * 
	 * this following bit was testing generation..... not finished yet!
	 * 
	 * TTRFormula goal; if (parser.parseUtterance(utt)) goal =
	 * parser.getState().getCurrentTuple().getSemantics(); else {
	 * System.out.println("Failed to construct goal from:" + utt);
	 * System.out.println("Terminating...."); return; }
	 * 
	 * System.out.println("Goal constructed:"+goal);
	 * 
	 * parser.init(); //Utterance firstHalf=new Utterance("A: what is this?");
	 * //parser.parseUtterance(firstHalf);
	 * 
	 * List<UtteredWord> generated=parser.generateTo(goal);
	 * 
	 * System.out.println("Generated:"+generated); /**
	 * 
	 * 
	 */

	/**
	 * Resets state and parses the dialogue d
	 * 
	 * @param d
	 * @return the resulting context
	 */
	public Context<DAGTuple, GroundableEdge> parseDialogue(Dialogue d) {
		context.init(d.getParticiapnts());
		for (Utterance utt : d) {
			if (!parseUtterance(utt)) {
				logger.warn("couldn't parse utterance" + utt);
				return null;
			}
		}

		return context;
	}

	/**
	 * used in checking if a word is parsable without changing the state. (used in
	 * MDP exploration)
	 * 
	 * @param la
	 * @return true if lexical action is applicable to the current (right-most)
	 *         tuple modulo left adjustment
	 */
	private boolean leftAdjustAndApply(LexicalAction la) {

		Pair<List<Action>, Tree> initPair = new Pair<List<Action>, Tree>(new ArrayList<Action>(),
				getState().getCurrentTuple().tree.clone());

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
					Pair<List<Action>, Tree> newPair = new Pair<List<Action>, Tree>(newActions, res);
					Pair<List<Action>, Tree> adjusted = adjustWithNonOptionalGrammar(newPair);
					global.add(adjusted);
				}
			}

		}

		for (Pair<List<Action>, Tree> pair : global) {

			logger.debug("executing " + la + " on " + pair.second);
			// TODO: for lex action context is local
			// but for computational action context is parser context
			// this is a hack to make substitution and Speaker label work. Don't
			// have time now.
			Tree res = la.exec(pair.second.clone(), context);

			if (res == null) {
				logger.debug("Failed");
				continue;
			}
			return true;

		}

		return false;

	}

	/**
	 * Used in MDP exploration/policy optimisation.
	 * 
	 * @return The sublexicon (list of words as strings) that is parsable at the
	 *         right-most tuples of the state. This method should not ultimately
	 *         change the parse state.
	 */
	public Set<String> getLocalGenerationOptions() {
		HashSet<String> result = new HashSet<String>();
		logger.info("Getting local Generation options");

		do {
			outer: for (String word : lexicon.keySet()) {
				if (result.contains(word))
					continue;
				logger.info("trying " + word + " at " + getState().getCurrentTuple());
				for (LexicalAction la : lexicon.get(word)) {

					if (leftAdjustAndApply(la)) {
						result.add(word);
						continue outer;
					}

				}
			}
		} while (parse());

		context.getDAG().resetToFirstTupleAfterLastWord();
		return result;

	}

	@Override
	public boolean rollBack(int n) {

		return this.context.rollBack(n);
	}

	@Override
	public Dialogue getDialogueHistory() {
		return context.getDialogueHistory();
	}

	@Override
	public boolean isExhausted() {
		return context.dag.isExhausted();
	}

}
