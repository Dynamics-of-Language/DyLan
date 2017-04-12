package qmul.ds.dag;

import edu.stanford.nlp.util.Pair;

/**
 * This edge is used only for dag traversal and is never actually added to the
 * DAG. It is comprised of two real edges which are added to the dag, a
 * {@link BacktrackingEdge} and a {@link RepairingWordEge}.
 * 
 * 
 * @author Arash
 *
 */
public class VirtualRepairingEdge extends GroundableEdge {

	Pair<BacktrackingEdge, GroundableEdge> edgePair;
	DAGTuple midTuple;
	int length=-1;
	public VirtualRepairingEdge(BacktrackingEdge backEdge,
			GroundableEdge repairingWordEdge, DAGTuple midTuple, long id) {
		super(repairingWordEdge.getActions(), repairingWordEdge.word, id);
		this.midTuple = midTuple;
		edgePair=new Pair<BacktrackingEdge, GroundableEdge>(backEdge, repairingWordEdge);
		
	}
	
	public VirtualRepairingEdge(BacktrackingEdge backEdge,
			GroundableEdge repairingWordEdge, DAGTuple midTuple, long id, int length)
	{
		this(backEdge, repairingWordEdge, midTuple, id);
		this.length=length;
	}

	

	public void traverse(WordLevelContextDAG dag) {
		edgePair.first.traverse(dag);
		edgePair.second.traverse(dag);

	}

	public void backtrack(WordLevelContextDAG dag) {
		edgePair.second.backtrack(dag);
		edgePair.first.backtrack(dag);
	}

	public BacktrackingEdge getBacktrackingEdge() {
		return edgePair.first;
	}

	public GroundableEdge getWordEdge() {
		return edgePair.second;
	}

	public DAGTuple getMidTuple() {
		return this.midTuple;
	}
	
	public String toString(){
		return "repair-"+super.toString();
	}
	/*
	public UtteredWord word()
	{
		return edgePair.first.word();
	}
	*/
}
