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
import qmul.ds.action.LexicalAction;
import qmul.ds.dag.ActionReplayEdge;
import qmul.ds.dag.BacktrackingEdge;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.dag.UtteredWord;
import qmul.ds.dag.WordLevelContextDAG;
import qmul.ds.formula.TTRFormula;
import qmul.ds.tree.Tree;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.util.Pair;

/**
 * An interactive parsing/generation agent with a DAG context as per Eshghi et.
 * al (2015).
 * 
 * Self-repair processing. CR processing. Acknowledgements. Question/Answer
 * pairs....
 * 
 * @author Arash
 *
 */
public class InteractiveContextParser extends
		DAGParser<DAGTuple, GroundableEdge> {

	public static final String repair_init_prefix = "init-repair";

	private static Logger logger = Logger
			.getLogger(InteractiveContextParser.class);

	/**
	 * the dialogue context. See {@link Context} for more info.
	 */

	// String currentSpeaker = "self";
	String[] acksa = {"uhu"};
	List<String> acks = Arrays.asList(acksa);
	String accept="yes";
	String[] repairand = { "uhh", "errm", "sorry", "err", "er", "well", "oh", "uh"};
	List<String> repairanda = Arrays.asList(repairand);
	String[] punct = { ".", "?", "!" };
	List<String> rightEdgeIndicators = Arrays.asList(punct);

	public InteractiveContextParser(File resourceDir) {
		super(resourceDir);
		state = new WordLevelContextDAG(this);
		context = new Context<DAGTuple, GroundableEdge>(state);
		state.setContext(context);

	}

	/**
	 * @param resourceDirNameOrURL
	 *            the dir containing computational-actions.txt,
	 *            lexical-actions.txt, lexicon.txt
	 */
	public InteractiveContextParser(String resourceDirNameOrURL) {
		super(resourceDirNameOrURL);
		state = new WordLevelContextDAG(this);
		context = new Context<DAGTuple, GroundableEdge>(state);
		state.setContext(context);

	}

	private boolean adjustOnce() {
		if (state.outDegree(state.getCurrentTuple()) == 0)
			applyAllPermutations();

		DAGEdge result;
		do {
			result = state.goFirst();

			if (result != null) {

				break;
			}
		} while (state.attemptBacktrack());

		return (result != null);

	}

	public boolean parse() {
		if (state.isExhausted()) {
			logger.info("state exhausted");
			return false;
		}

		do {

			if (!adjustOnce()) {
				state.setExhausted(true);
				return false;
			}

		} while (!state.wordStack().isEmpty());
		// commitToContext();
		return true;
	}
	
	
	private void applyAllPermutations() {
		if (state.wordStack().isEmpty())
			return;
		UtteredWord word = state.wordStack().peek();
		// logger.debug("apply all for word:"+word);
		if (this.rightEdgeIndicators.contains(word.word())) {
			replayBacktrackedActions(word);
		}else if (word.word().equals(repair_init_prefix))
		{
			state.wordStack().pop();
			initiateRepair();
			return;
			
		}
		else if (this.acks.contains(word.word())) {
			String lastSpkr = state.getParentEdge().word().speaker();
			// System.out.println("last speaker was:"+lastSpkr);
			DAGTuple completed = complete(word);

			state.getParentEdge(completed).groundFor(lastSpkr);
			return;
		} 
		
		Pair<List<Action>, Tree> initPair = new Pair<List<Action>, Tree>(
				new ArrayList<Action>(), state.getCurrentTuple().tree.clone());

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

			UtteredWord w = state.wordStack().peek();
			
			
			for (LexicalAction la : lexicon.get(w.word())) {
				//set right edge to not replayable
				//TODO: should be part of the lexical entry? Need to think about this.
				//for later... now must demo!
				boolean replayable=true;
				boolean repairable=true;
				if (this.rightEdgeIndicators.contains(w.word())||la.getLexicalActionType().equals("accept")||la.getLexicalActionType().equals("reject"))
				{
					repairable=false;
					replayable=false;
				}
				Tree res = la.exec(pair.second.clone(), context);
				if (res != null) {
					int TRPIndex = getIndexOfTRP(pair.first);

					if (TRPIndex > -1) {
						List<Action> axiomActs = new ArrayList<Action>(
								pair.first.subList(0, TRPIndex + 1));
						DAGTuple axiom = state.addAxiom(axiomActs);
						logger.debug("Added axiom with actions:" + axiomActs);
						ArrayList<Action> newActs = TRPIndex == 0 ? new ArrayList<Action>()
								: new ArrayList<Action>(pair.first.subList(
										TRPIndex + 1, pair.first.size()));
						newActs.add(la.instantiate());
						GroundableEdge wordEdge = state.getNewEdge(newActs, w);
						logger.debug("created word edge with word:" + w);
						logger.debug("with actions:" + wordEdge.getActions());
						wordEdge.setReplayable(replayable);
						wordEdge.setRepairable(repairable);
						DAGTuple newTuple = state.getNewTuple(res);
						state.addChild(axiom, newTuple, wordEdge);

						logger.debug("Added Edge:" + wordEdge);
						logger.debug("Child:" + newTuple);
						continue;

					}
					ArrayList<Action> newActs = new ArrayList<Action>(
							pair.first);
					newActs.add(la.instantiate());
					GroundableEdge wordEdge;
					if (la.getLexicalActionType().equals("accept"))
					{
						wordEdge=state.getNewGroundingEdge(newActs,w);
						
						//if (state.getCurrentTuple().tree.getRootNode())
						//state.setAcceptancePointer(state.getParentEdge().word().speaker());
					}
					else
					    wordEdge = state.getNewEdge(newActs, w);
					
					
					logger.debug("created word edge with word:" + w);
					logger.debug("edge before adding:" + wordEdge);
					wordEdge.setReplayable(replayable);
					wordEdge.setRepairable(repairable);
					DAGTuple newTuple = state.getNewTuple(res);
					
					state.addChild(newTuple, wordEdge);
					
					logger.debug("Added Edge:" + wordEdge);
					logger.debug("Child:" + newTuple);
					
					if (la.getLexicalActionType().equals("reject"))
						state.setAcceptancePointer(w.speaker(), newTuple);
					else if (la.getLexicalActionType().equals("accept")||la.getLexicalActionType().equals("assert"))
					{
						state.setAcceptancePointer(w.speaker(),newTuple);
						for(String spkr: state.getAcceptancePointers(state.getParent(newTuple)))
						{
							logger.info("setting acceptance pointer for:"+spkr);
							state.setAcceptancePointer(spkr, newTuple);
						}
					}
					//if the lexical action was acceptance/rejection, manually set acceptance pointers.
					//this need to go into the lexical actions.
					//TODO
					
						
				}
			}
		}

	}

	protected int getIndexOfTRP(List<Action> actions) {
		for (int i = 0; i < actions.size(); i++) {
			if (actions.get(i).getName().equals("trp"))
				return i;
		}
		return -1;
	}

	public boolean replayBacktrackedActions(UtteredWord word) {

		Tree start = state.getCurrentTuple().getTree().clone();
		List<Action> acts = new ArrayList<Action>();// this will be the actions
													// that will be replayed..
													// it will be added to as a
													// result of call to
													// trimuntil

		Pair<List<GroundableEdge>, Tree> edgeTreePair = trimUntilApplicable(
				start, state.getBacktrackedEdges(), acts);

		//logger.debug("got edges back from trim:"
			//	+ (edgeTreePair == null ? null : edgeTreePair.first));
		//logger.debug("got actions back from trim:" + acts);
		if (edgeTreePair == null || edgeTreePair.first.isEmpty()) {
			logger.info("Replay: didn't rerun anything from context");
			return false;
			// edgeTreePair = new Pair<List<GroundableEdge>, Tree>(new
			// ArrayList<GroundableEdge>(),
			// start);

		}
		ActionReplayEdge replayEdge = state.getNewActionReplayEdge(acts,
				new UtteredWord(null, word.speaker()), edgeTreePair.first);
		logger.info("adding replayEdge with actions:" + acts);
		DAGTuple res = state.getNewTuple(edgeTreePair.second);
		state.addChild(res, replayEdge);
		state.getBacktrackedEdges().clear();
		return true;
		
	}

	@Override
	public boolean parse(List<? extends HasWord> words) {
		for (int i = 0; i < words.size(); i++) {
			DAG<DAGTuple, GroundableEdge> result = parseWord(new UtteredWord(
					words.get(i).word()));
			if (result == null)
				return false;

		}

		return true;
	}

	@Override
	public void newSentence() {

		state.addAxiom();

	}

	public void init() {
		state.init();
		if (context == null)
			context = new Context<DAGTuple, GroundableEdge>(state);
		else
			context.setDAG(state);

	}

	public UtteredWord generateNextWord(TTRFormula goal)
	{
		
		
		
		return null;
	}
	
	
	public boolean parseWord(TTRFormula goal, UtteredWord word)
	{
		
		
		
		
		return false;
	}
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

		// if (this.acks.contains(word.word()))
		// {
		// logger.debug("parsing an acknowledgement");
		// this.state.thisIsFirstTupleAfterLastWord();
		// this.state.groundToRootFor(word.speaker());
		// return this.state;
		// }
		if (this.repairanda.contains(word.word())) {
			this.state.thisIsFirstTupleAfterLastWord();
			return this.state;
		}

		Collection<LexicalAction> actions = this.lexicon.get(word.word());
		if (actions == null || actions.isEmpty()) {
			logger.error("Word not in Lexicon: " + word);
			return null;
		}

		state.wordStack().push(word);
		// state.clear();
		if (!parse()) {
			logger.info("OOPS! Cannot parse " + word);
			logger.info("Resetting state to the state after the last parsable word");
			logger.info("Now initiating repair.");
			state.resetToFirstTupleAfterLastWord();
			// logger.debug("tuple after last word:"+state.getCurrentTuple());

			state.wordStack().push(word);
			initiateRepair();

			if (!parse())
			{
				logger.info("OOPS! Couldn't parse word as repair either");
				return null;
			}
		}

		this.state.thisIsFirstTupleAfterLastWord();
		//this.state.setAcceptancePointer(word.speaker());
		logger.info("Parsed " + word);
		logger.info("Final Tuple:" + state.getCurrentTuple());

		return this.state;
	}

	public Context<DAGTuple, GroundableEdge> getContext() {
		return context;
	}

	public DAG<DAGTuple, GroundableEdge> generateWord(String word) {
		parseWord(new UtteredWord(word, "self"));
		return getState();
	}

	public void initiateRepair() {

		backtrackAndParse(state.wordStack().peek());

		state.wordStack().push(
				new UtteredWord(repair_init_prefix, state.wordStack().peek()
						.speaker()));

	}

	private GroundableEdge getFirstRepairableEdge()
	{
		logger.debug("finding first repairable edge:");
		
		GroundableEdge parent=state.getParentEdge();
		while(parent!=null)
		{
			if (parent.isRepairable())
				return parent;
			
			parent=state.getParentEdge(state.getSource(parent));
			logger.debug("inside loop");
			
		}
		return null;
	}
	
	
	private void backtrackAndParse(UtteredWord word) {

		if (this.rightEdgeIndicators.contains(word.word()))
			return;
		
		logger.debug("backtrack and parsing:"+word);
		
		for (LexicalAction la : lexicon.get(word.word())) {
			if (la.getLexicalActionType().equals("accept")||la.getLexicalActionType().equals("reject"))
				return;
			
			DAGTuple current = state.getCurrentTuple();
			if (state.isClauseRoot(current))
				return;
			
			
			
			GroundableEdge firstRepairable=getFirstRepairableEdge();
			if (firstRepairable==null)
			{
				logger.debug("didn't find repairable edge.");
				return;
			}
			logger.debug("first repariable edge:"+firstRepairable);
			String speakerOfFirstRepairable=firstRepairable.word().speaker();
			//we want to repair only either the current turn, or the previous.
			
			GroundableEdge repairableEdge;
			List<GroundableEdge> backtracked = new ArrayList<GroundableEdge>();

			do {
				repairableEdge = state.getParentEdge(current);
			    current=state.getSource(repairableEdge);
				//shouldn't repair right edges like ? . ! <eot> etc.
			    backtracked.add(0, repairableEdge);
			    logger.debug("checking repairable"+repairableEdge);
			    if (!repairableEdge.isRepairable())
			    {
			    	logger.debug("edge not repairable");
			    	continue;
			    }
			    
			    if (!speakerOfFirstRepairable.equals(repairableEdge.word().speaker()))
			    	break;
				List<Action> actions = new ArrayList<Action>(repairableEdge
						.getActions().subList(0,
								repairableEdge.getActions().size() - 1));

				actions.add(la);

				Tree result = applyActions(current.getTree(), actions);

				if (result != null) {
					// now add backtracking edge

					BacktrackingEdge<GroundableEdge> back = state
							.getNewBacktrackingEdge(
									new ArrayList<GroundableEdge>(backtracked),
									word.speaker());
					logger.debug("Adding backtracking edge:" + back);
					logger.debug("from: " + state.getCurrentTuple());
					logger.debug("to: " + current);
					state.addChild(current, back);
					GroundableEdge repairingEdge = state.getNewEdge(actions,
							word);
					DAGTuple to = state.getNewTuple(result);
					state.addChild(current, to, repairingEdge);
					logger.debug("Adding repairing edge:" + repairingEdge);
					logger.debug("from: " + current);
					logger.debug("to: " + to);

					
					
				} else
					logger.debug("could not apply:"+actions+"\n at:"+current.getTree());
				
				

			} while (!state.isClauseRoot(current)
					&& !state.isBranching(current));
		}

	}

	public boolean parseUtterance(Utterance utt) {

		return parseWords(utt.words) != null;
	}

	public static void main(String[] a) {
		InteractiveContextParser parser = new InteractiveContextParser(
				"resource/2015-english-ttr-shape-colour");
		Utterance utt = new Utterance("A: what do you");
		parser.parseUtterance(utt);
		Tree finalT=parser.getState().getCurrentTuple().getTree();
		System.out.println("Final tree" + finalT);
		System.out.println("Sem: "+finalT.getMaximalSemantics());

	}

}
