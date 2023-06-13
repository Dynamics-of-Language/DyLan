package qmul.ds;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.action.Grammar;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.dag.UtteredWord;
import qmul.ds.dag.VirtualRepairingEdge;
import qmul.ds.dag.WordLevelContextDAG;
import qmul.ds.formula.TTRFormula;
import qmul.ds.tree.Tree;

/**
 * An abstract best first DAG generator with a beam whose corresponding parser
 * is {@link InteractiveContextParser} This class is agnostic about how the beam
 * is populated (e.g. probabilistically, neurally, etc.)
 *
 * @author arash
 */
public abstract class BestFirstGenerator extends DAGGenerator<DAGTuple, GroundableEdge> {
	protected int beam = 3;

	protected static Logger logger = Logger.getLogger(BestFirstGenerator.class);
	// ---------------------------------- Constructors
	// ----------------------------------

	/**
	 * @param resourceDir the dir containing computational-actions.txt,
	 *                    lexical-actions.txt, lexicon.txt
	 */
	public BestFirstGenerator(File resourceDir, int beam) {
		this(new Lexicon(resourceDir), new Grammar(resourceDir));
		this.beam = beam;
	}

	/**
	 * @param resourceDir the dir containing computational-actions.txt,
	 *                    lexical-actions.txt, lexicon.txt
	 */
	public BestFirstGenerator(File resourceDir) {
		this(new Lexicon(resourceDir), new Grammar(resourceDir));
	}

	/**
	 * @param resourceDir the dir containing computational-actions.txt,
	 *                    lexical-actions.txt, lexicon.txt
	 */
	public BestFirstGenerator(String resourceDir) {
		this(new Lexicon(resourceDir), new Grammar(resourceDir));
	}

	/**
	 * @param lexicon
	 * @param grammar
	 */
	public BestFirstGenerator(Lexicon lexicon, Grammar grammar) {
		super(lexicon, grammar);
	}

	// ---------------------------------- Methods ----------------------------------
	@Override
	public DAG<DAGTuple, GroundableEdge> getNewState(Tree start) {
		return new WordLevelContextDAG(start);
	}

	@Override
	public DAGParser<DAGTuple, GroundableEdge> getParser(Lexicon lexicon, Grammar grammar) {
		return new InteractiveContextParser(lexicon, grammar);
	}

	/**
	 * Subclasses should implement this method
	 *
	 * @return a list of words of length beam of words / tokens.
	 */
	public abstract List<String> populateBeam(); // AA: modified.

	public boolean generateNextWord() { // AA: Doesn't seem to be a good name.
		logger.info("Generating next word");
		
		TTRFormula cur = getState().getCurrentTuple().getSemantics().removeHead();
		//goal is headless
		
		if (cur.subsumes(goal))
		{
			//if we are here, we will attempt to generate forward
			List<String> beamWords = this.populateBeam();// AA: modified.
			logger.info("Beam is:"+beamWords);

			for (String word : beamWords) {
				DAG<DAGTuple, GroundableEdge> result = this.generateWord(word, goal);
				if (result != null) {
					logger.info("generated:"+word);
					this.generated.addWord(word);
					return true;
				}
			}
		}

		if (!this.repairGeneration)
			return false;
			
		// if we are here, we failed to generate forward . Could be due to:
		// (a) goal change; or (b) no suitable words in beam.
		// either way, we'll attempt to generate repair. Failing this, generateNextWord
		// will fail completely.
		logger.info("Could not generate forward. Will now attempt to generate next word as a self-repair");

		if (!attemptRepair()) {
			logger.info("Could not generate next word as repair either. generateNextWord() is failing completely.");
			return false;
		}

		return true;
	}

	/**
	 * This method will search backwards on the DAG, checking if any of the words
	 * returned by {@link populateBeam()} can be generated from prior DAG nodes.
	 * if yes, it will perform the repair by making the appropriate modifications to
	 * the DAG, leaving the DAG pointer at the rightmost edge of repair path -- at
	 * the end of the VirtualRepairingEdge that is added.
	 * 
	 * @return true if successful. False if no repair point can be found -- this
	 *         shouldn't happen!
	 */
	public boolean attemptRepair() {

		logger.debug("Attempting to generate repair ...");
		DAGTuple rightMostDAGNode = getState().getCurrentTuple();

		DAGTuple current = getState().getCurrentTuple();
		if (getState().isClauseRoot(current))
			return false;

		GroundableEdge repairableEdge;
		List<GroundableEdge> backtracked = new ArrayList<GroundableEdge>();

		do {

			repairableEdge = getState().getParentEdge(current);

			logger.debug("back over:" + repairableEdge);
			current = getState().getSource(repairableEdge);

			

			backtracked.add(0, repairableEdge);
			// shouldn't repair right edges like ? . ! <eot> etc.
			if (!repairableEdge.isRepairable()) {
				logger.debug("edge not repairable:" + repairableEdge);
				continue;
			}

			// need to set the current tuple, because populateBeam depdends on it
			// MUST SET THIS BACK TO rightMostDAGNode before we add the repairing virtual
		    // edge.
		    getState().setCurrentTuple(current);
		    
			List<String> beamWords = populateBeam();
			logger.debug("Beam is:" + beamWords);

			for (String word : beamWords) {
				logger.debug("Attempting to generate '"+word+"' from tuple: "+current);
				
				for (LexicalAction la : parser.getLexicon().get(word)) {
					/**
					 * extracting the computational actions from repairable edge. The same ones
					 * should be applicable before the repairing lexical action.
					 * 
					 */
					List<Action> actions = new ArrayList<Action>(
							repairableEdge.getActions().subList(0, repairableEdge.getActions().size() - 1));

					actions.add(la);
					
					//set current tuple to current, so actions are applied in that context
					
					getState().setCurrentTuple(current);
					Tree result = parser.applyActions(current.getTree(), actions);
					
					//now the current tuple back set it back to the original 
					getState().setCurrentTuple(rightMostDAGNode);
					
					if (result != null) {
						// now add backtracking edge
						DAGTuple to = getState().getNewTuple(result);
						
						TTRFormula cur = to.getSemantics(parser.getContext()).removeHead();
						
						
						if (!cur.subsumes(goal))
						{
							logger.debug("Applied la successfully, but result didn't subsume goal");
							logger.debug("result was:"+cur);
							continue;
						}
						
						
						logger.debug("Succeeded. Adding VirtualReparingEdge");
						logger.debug("from " + current);
						logger.debug("to " + to);
						UtteredWord repairWord = new UtteredWord(word,agentName);
						VirtualRepairingEdge repairing = getState().getNewRepairingEdge(
								new ArrayList<GroundableEdge>(backtracked), actions, current, repairWord);
						
						

						
						// first initiate repair
						getState().setRepairProcessing(true);
						getState().wordStack().push(new UtteredWord(word, agentName));// first push the repairing word onto the stack
						getState().initiateLocalRepair();// this will push "repair-init" onto stack
						// first add the repairing edge
						
						getState().addChild(to, repairing);

						if (getState().goFirst() == null) {
							logger.error(
									"goFirst returned null after adding virtual repairing edge. This MUST not happen.");
							throw new IllegalStateException(
									"goFirst returned false after adding virtual repairing edge. This MUST not happen.");
						}
						Random r = new Random();
						String interregnum = interregna.get(r.nextInt(interregna.size()));
						generated.addWord(interregnum);
						generated.addWord(word);
						
						logger.info("Succeeded: generated: "+interregnum + " "+word);
						
						this.getState().thisIsFirstTupleAfterLastWord();
						this.getState().setRepairProcessing(false);
						
						//System.out.println("After goFirst; cur is:"+getState().getCurrentTuple());
						//System.out.println("out degree:"+getState().outDegree(getState().getCurrentTuple()));
						
						this.repairGeneration = false;
						return true;

					} else
						logger.debug("could not apply:" + actions + "\n at:" + current.getTree());
				}
				
				// if we are here, word w failed to be generated from current
				logger.debug("Could not generate '"+word+"' from tuple:"+current);
				
			}
			
			//if we are here, we could not generate at all from current
			logger.debug("Could not generate from tuple: "+current);
			logger.debug("Now going further back along DAG path.");

		} while (!getState().isClauseRoot(current) && !getState().isBranching(current));

		// if we are out here we couldn't find a repair point from which to generate.
		logger.info("Could perform repair.");
		getState().setCurrentTuple(rightMostDAGNode);
		return false;

	}

}
