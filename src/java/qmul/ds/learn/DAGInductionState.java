package qmul.ds.learn;

import java.util.List;

import qmul.ds.action.Action;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.tree.Tree;
/**
 * 
 * @author arash
 *
 */


public class DAGInductionState extends DAG<DAGInductionTuple, DAGEdge> {

	
	
	
	private static final long serialVersionUID = 1L;

	
	public DAGInductionState() {
		super();
		
		cur = DAGTuple.getNewInductionTuple(idPoolNodes);
		addVertex(cur);
		lastN.add(cur.getTree());

		thisIsFirstTupleAfterLastWord();
	}

	
	
	public DAGInductionState(List<String> words) {
		super(words);
		
		cur = DAGTuple.getNewInductionTuple(idPoolNodes);
		addVertex(cur);
		lastN.add(cur.getTree());

		thisIsFirstTupleAfterLastWord();
		
	}
	
	public DAGInductionState(Tree start) {
		super();
		cur = DAGTuple.getNewInductionTuple(idPoolNodes, start);
		addVertex(cur);
		lastN.add(cur.getTree());
		thisIsFirstTupleAfterLastWord();
	}

	public DAGInductionState(Tree start, List<String> words) {
		super(words);
		cur = DAGTuple.getNewInductionTuple(idPoolNodes, start);
		addVertex(cur);
		lastN.add(cur.getTree());
		thisIsFirstTupleAfterLastWord();
		
	}

	@Override
	public DAGInductionTuple execAction(Action a, String w) {
		Tree res = a.exec(cur.getTree().clone(), cur);

		if (res == null)
			return null;

		if (loopDetected(res)) {
			logger.warn("Detected infinite branch. Not extending DAG.");
			logger.warn("History:");
			for(Tree t:lastN)
			{
				logger.warn(t);
			}
			return null;
		}
		DAGEdge edge = DAGEdge.getNewEdge(idPoolEdges, a.instantiate(), w);
		DAGInductionTuple p = DAGTuple.getNewInductionTuple(idPoolNodes, res);
		
		addEdge(edge, cur, p);
		p.setDepth(cur.getDepth()+1);
		return p;
	}
	
	public DAGInductionTuple addChild(DAGInductionTuple t, Action a, String word)
	{
		if (t == null)
			return null;
		if (!(a instanceof TreeHypothesis)&&loopDetected(t.getTree())) {
			logger.info("Detected infinite branch. Not extending DAG.");
			return null;
		}
		
		DAGEdge edge = DAGEdge.getNewEdge(idPoolEdges, a, word);
		DAGInductionTuple target = DAGTuple.getNewInductionTuple(idPoolNodes, t);
		target.setTarget(cur.getTargetTree());
		target.setNonHeadTarget(cur.getNonHeadTarget());
		addEdge(edge, cur, target);
		return target;
	}
	
	public boolean addChild(Tree t, Action a, String word) {
		if (t == null)
			return false;
		if (!(a instanceof TreeHypothesis) && loopDetected(t)) {
			logger.info("Detected infinite branch. Not extending DAG.");
			logger.debug("Action was:"+a);
			logger.debug("tree was:"+t);
			return false;
		}
		DAGEdge edge = DAGEdge.getNewEdge(idPoolEdges, a.instantiate(), word);
		DAGInductionTuple target = DAGTuple.getNewInductionTuple(idPoolNodes, t);
		target.setTarget(cur.getTargetTree().clone());
		target.setNonHeadTarget(cur.getNonHeadTarget());
		target.setDepth(cur.getDepth());
		return addEdge(edge, cur, target);
	}
}
