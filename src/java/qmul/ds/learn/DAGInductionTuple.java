package qmul.ds.learn;


import qmul.ds.ParserTuple;
import qmul.ds.dag.DAGTuple;
import qmul.ds.tree.Tree;

public class DAGInductionTuple extends DAGTuple{
	
	private Tree curTarget = new Tree();  //used in induction only.... this is hacky.
	private Tree curNonHeadTarget = new Tree();  //used in induction only ... it is hacky.

	
	public DAGInductionTuple(long id)
	{
		super(id);
	}
	

	public DAGInductionTuple(Tree t, long i) {
		super(t, i);
	}


	public DAGInductionTuple(ParserTuple t, long i) {
		super(t, i);
	}


	public DAGInductionTuple(Tree clone) {
		super(clone);
	}


	public Tree getTargetTree() {
		return curTarget;
	}


	public void setTarget(Tree curTarget) {
		this.curTarget = curTarget;
	}


	public Tree getNonHeadTarget() {
		return curNonHeadTarget;
	}


	public void setNonHeadTarget(Tree curNonHeadTarget) {
		this.curNonHeadTarget = curNonHeadTarget;
	}

}
