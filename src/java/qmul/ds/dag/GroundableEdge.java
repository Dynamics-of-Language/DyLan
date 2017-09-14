package qmul.ds.dag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.InteractiveContextParser;
import qmul.ds.action.Action;
import qmul.ds.action.LexicalAction;

public class GroundableEdge extends DAGEdge {

	protected Logger logger=Logger.getLogger(GroundableEdge.class);
	protected Set<String> grounded_for = new HashSet<String>();
	boolean repairable = true;
	boolean grounded=false;//currently only used when this edge initiates a new clause.

	public GroundableEdge() {

		
	}

	public GroundableEdge(Action a, UtteredWord w) {
		super(a, w);
		if (w.word().equals(InteractiveContextParser.RELEASE_TURN))
			this.grounded_for.add(w.speaker());
		//if (w != null)
		//	this.grounded_for.add(w.speaker());
	}

	public GroundableEdge(Action a, UtteredWord w, long id) {
		this(a, w);
		this.id = id;
	}

	public GroundableEdge(List<Action> a, UtteredWord w) {
		super(a, w);
		//TODO: treating <rt> edges as grounded. They are the self grounding edges.... 
		if (w.word().equals(InteractiveContextParser.RELEASE_TURN))
			this.grounded_for.add(w.speaker());
		//if (w != null)
		//	this.grounded_for.add(w.speaker());
	}

	public GroundableEdge(List<Action> a, UtteredWord w, long id) {
		this(a, w);
		this.id = id;
	}

	public GroundableEdge(List<Action> a) {
		this(a, null);
	}

	public GroundableEdge(UtteredWord w, long id) {
		super(w, id);
	}

	public void groundFor(String speaker) {
		this.grounded_for.add(speaker);
	}

	public String toString() {
		return this.id+ (initiatesNewClause()?"(NC"+(isGrounded()?"-g":"-u")+")":"")+ ":"+ (grounded_for + ":" + word.word());
	}
	public String toDebugString()
	{
		return toString()+":"+this.actions;
	}

	public String getEdgeLabel() {
		return toString();
	}

	public boolean isGroundeFor(String speaker) {
		return grounded_for.contains(speaker);
	}
	
	public boolean isGroundedFor(Set<String> speakers)
	{
		return grounded_for.equals(speakers);
	}

	public void ungroundFor(String speaker) {
		grounded_for.remove(speaker);

	}

	public boolean isRepairable() {
		return repairable;
	}

	public void setRepairable(boolean b) {
		this.repairable = b;

	}

	public void traverse(WordLevelContextDAG dag) {
		DAGTuple source = dag.getSource(this);
		if (!source.equals(dag.getCurrentTuple())) {
			logger.error("Didn't traverse edge:" + this);
			logger.error("Because its source isn't the pointed tuple on the dag.");
			throw new IllegalStateException();
		}
		DAGTuple dest = dag.getDest(this);

		
		logger.debug("Going forward along: " + this);
		setInContext(true);
		

		
		
		dag.setCurrentTuple(dest);

	}

	public void backtrack(WordLevelContextDAG dag) {
		if (!dag.getDest(this).equals(dag.getCurrentTuple())) {
			throw new IllegalStateException(
					"the edge to be backtracked does not have its destination as the current dag tuple");
		}
		logger.debug("backtracking over groundable edge:"+this);
		
		/**
		 * IMPORTANT: commented out because I am enforcing grounding of the previous turn upon continuation.
		 * i.e. if I want to continue what you say, I PICK a particular interpretation, and then continue. I won't be able to
		 * backtrack over/into it anymore.
		 * TODO
		**/
		//GroundableEdge prevPrevEdge = dag.getActiveParentEdge(dag
		//		.getSource(this));
		
		
//		if (prevPrevEdge != null
//				&& !word().speaker().equals(prevPrevEdge.word().speaker())) {
//			dag.ungroundToClauseRootFor(word().speaker(), dag.getSource(this));
//		}
		
		
		setSeen(true);
		setInContext(false);
		
		dag.setCurrentTuple(dag.getSource(this));
		logger.debug("Backtracked over: " + this);

	}
	
	public String getLexicalActionType()
	{
		return ((LexicalAction)actions.get(actions.size()-1)).getLexicalActionType();
	}
	
	
	
	
	
}
