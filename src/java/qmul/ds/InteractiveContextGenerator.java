/**
 * 
 */
package qmul.ds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import edu.stanford.nlp.util.Pair;
import qmul.ds.action.Action;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.Grammar;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.dag.UtteredWord;
import qmul.ds.dag.WordLevelContextDAG;
import qmul.ds.formula.TTRFormula;
import qmul.ds.tree.Tree;

/**
 * @author Arash
 *
 */
public class InteractiveContextGenerator extends
		DAGGenerator<DAGTuple, GroundableEdge> {

	protected static Logger logger=Logger.getLogger(InteractiveContextGenerator.class);
	
	public InteractiveContextGenerator(DAGParser<DAGTuple, GroundableEdge> parser, TTRFormula goal)
	{
		super(parser);
		setGoal(goal);
		
	}
	public InteractiveContextGenerator(TTRFormula goal, String resourceDir)
	{
		super(resourceDir);
		setGoal(goal);
		
	}
	
	
	public InteractiveContextGenerator(
			DAGParser<DAGTuple, GroundableEdge> interactiveContextParser) {
		super(interactiveContextParser);
		
	}
	@Override
	public DAG<DAGTuple, GroundableEdge> getNewState(Tree start) {
		
		return new WordLevelContextDAG(start);
	}

	@Override
	public DAGParser<DAGTuple, GroundableEdge> getParser(Lexicon lexicon,
			Grammar grammar) {
		return new InteractiveContextParser(lexicon, grammar);
	}

	
	
	
	
	
	public void applyAllOptions()
	{
		List<Pair<GroundableEdge, DAGTuple>> genPairs=getLocalGenerationOptions();
		for(Pair<GroundableEdge, DAGTuple> genPair: genPairs)
		{
			parser.getState().addChild(genPair.second, genPair.first);
		}
	}
	
	
	private List<Pair<GroundableEdge, DAGTuple>> getLocalGenerationOptions() {
		List<Pair<GroundableEdge, DAGTuple>> result = new ArrayList<Pair<GroundableEdge, DAGTuple>>();

		for (String word : parser.lexicon.keySet())
			for (LexicalAction la : parser.lexicon.get(word)) {
				Pair<List<Action>, Tree> res = this.leftAdjustAndApply(la);
				if (res != null) {
					GroundableEdge wordEdge;
					UtteredWord w = new UtteredWord(word, myName);
					if (parser.getIndexOfTRP(res.first) >= 0)
						wordEdge = parser.getState().getNewNewClauseEdge(res.first, w);
					else
						wordEdge = parser.getState().getNewEdge(res.first, w);

					logger.debug("created word edge with word:" + w);
					logger.debug("edge before adding:" + wordEdge);
					if (InteractiveContextParser.non_repairing_action_types.contains(la
							.getLexicalActionType()))
						wordEdge.setRepairable(false);

					DAGTuple newTuple = parser.getState().getNewTuple(res.second);
					result.add(new Pair<GroundableEdge, DAGTuple>(wordEdge,
							newTuple));

				}

			}

		return result;

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
	private Pair<List<Action>, Tree> leftAdjustAndApply(LexicalAction la) {
		
		

		
		Pair<List<Action>, Tree> initPair = new Pair<List<Action>, Tree>(
				new ArrayList<Action>(), parser.getState().getCurrentTuple().tree.clone());

		initPair = parser.adjustWithNonOptionalGrammar(initPair);

		List<Pair<List<Action>, Tree>> global = new ArrayList<Pair<List<Action>, Tree>>();
		global.add(initPair);
		// this is to detect loops. loops are there because of things like
		// AnticipationL, Completion....
		HashMap<ComputationalAction, HashSet<Tree>> tried = new HashMap<ComputationalAction, HashSet<Tree>>();
		for (ComputationalAction action : parser.optionalGrammar.values()) {
			tried.put(action, new HashSet<Tree>());
		}

		for (int i = 0; i < global.size(); i++) {
			Pair<List<Action>, Tree> cur = global.get(i);

			for (ComputationalAction ca : parser.optionalGrammar.values()) {
				if (tried.get(ca).contains(cur.second))
					continue;

				tried.get(ca).add(cur.second);
				Tree res = ca.exec(cur.second.clone(), parser.context);
				logger.trace("Applying ca: " + ca);
				logger.trace("to: " + cur.second);
				logger.trace("result: " + res);
				if (res != null) {
					List<Action> newActions = new ArrayList<Action>(cur.first);
					newActions.add(ca.instantiate());
					Pair<List<Action>, Tree> newPair = new Pair<List<Action>, Tree>(
							newActions, res);
					Pair<List<Action>, Tree> adjusted = parser.adjustWithNonOptionalGrammar(newPair);
					global.add(adjusted);
				}
			}

		}

		for (Pair<List<Action>, Tree> pair : global) {

			boolean repairable = true;
			logger.debug("executing " + la + " on " + pair.second);
			//TODO: for lex action context is local
			//but for computational action context is parser context
			//this is a hack to make substitution and Speaker label work. Don't have time now.
			Tree res = la.exec(pair.second.clone(), parser.context);

			if (res == null) {
				logger.debug("Failed");
				continue;
			}
			TTRFormula maxSem = res.getMaximalSemantics(parser.getContext());
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
	
	
	
	public static void main(String[] a)
	{
		InteractiveContextParser parser = new InteractiveContextParser(
				"resource/2016-english-ttr-attribute-learning");
		Utterance utt = new Utterance("A: this is a yellow square.");
		TTRFormula goal;
		if (parser.parseUtterance(utt))
		{
			
		
			goal = parser.getState().getCurrentTuple().getSemantics(parser.getContext());
			System.out.println("Goal constructed:"+goal);
		}
		else
		{
			System.out.println("Failed to construct goal from:"+utt);
			System.out.println("Terminating....");
			return;
		}
		
		

		
		InteractiveContextGenerator generator=new InteractiveContextGenerator(goal, "resource/2016-english-ttr-attribute-learning");
		
		if (generator.generate())
		{
			System.out.println("Success:"+generator.getState().wordStack());
			
		}
		else
		{
			System.out.println("Failed!");
			
		}
	}

}
