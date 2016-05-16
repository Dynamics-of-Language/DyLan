package qmul.ds.dag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import qmul.ds.action.Action;

public class GroundableEdge extends DAGEdge {

	protected Set<String> grounded_for = new HashSet<String>();
	boolean repairable = true;

	public GroundableEdge() {

	}

	public GroundableEdge(Action a, UtteredWord w) {
		super(a, w);
		if (w != null)
			this.grounded_for.add(w.speaker());
	}

	public GroundableEdge(Action a, UtteredWord w, long id) {
		this(a, w);
		this.id = id;
	}

	public GroundableEdge(List<Action> a, UtteredWord w) {
		super(a, w);
		if (w != null)
			this.grounded_for.add(w.speaker());
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
		return grounded_for + ":" + word.word();
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

		
		logger.info("Going forward along: " + this);
		setInContext(true);
		
		GroundableEdge prevPrevEdge = dag.getActiveParentEdge(source);
		if (prevPrevEdge != null
				&& !word().speaker().equals(prevPrevEdge.word().speaker())) {
			dag.groundToClauseRootFor(word().speaker(), dag.getSource(this));
		}
		
		
		dag.setCurrentTuple(dest);

	}

	public void backtrack(WordLevelContextDAG dag) {
		if (!dag.getDest(this).equals(dag.getCurrentTuple())) {
			throw new IllegalStateException(
					"the edge to be backtracked does not have its destination as the current dag tuple");
		}

		
		
		GroundableEdge prevPrevEdge = dag.getActiveParentEdge(dag
				.getSource(this));
		if (prevPrevEdge != null
				&& !word().speaker().equals(prevPrevEdge.word().speaker())) {
			dag.ungroundToClauseRootFor(word().speaker(), dag.getSource(this));
		}

		setSeen(true);
		setInContext(false);

		dag.setCurrentTuple(dag.getSource(this));
		logger.info("Backtracked over: " + this);

	}

}
