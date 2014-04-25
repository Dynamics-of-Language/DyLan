package qmul.ds.dag;

import java.util.List;
import java.util.Stack;

import qmul.ds.action.Action;
import qmul.ds.tree.Tree;
/**
 * 
 * @author arash
 *
 */
public class DAGState extends DAG<DAGTuple, DAGEdge> {

	
	private static final long serialVersionUID = 1L;

	public DAGState() {
		super();
		
		cur = DAGTuple.getNewTuple(idPoolNodes);
		addVertex(cur);
		lastN.add(cur.getTree());

		thisIsFirstTupleAfterLastWord();
	}

	
	
	public DAGState(List<String> words) {
		super(words);
		
		cur = DAGTuple.getNewTuple(idPoolNodes);
		addVertex(cur);
		lastN.add(cur.getTree());

		thisIsFirstTupleAfterLastWord();
		
	}
	
	public DAGState(Tree start) {
		super();
		cur = DAGTuple.getNewTuple(idPoolNodes, start);
		addVertex(cur);
		lastN.add(cur.getTree());
		thisIsFirstTupleAfterLastWord();
	}

	public DAGState(Tree start, List<String> words) {
		super(words);
		cur = DAGTuple.getNewTuple(idPoolNodes, start);
		addVertex(cur);
		lastN.add(cur.getTree());
		thisIsFirstTupleAfterLastWord();
		
	}
	
	
	@Override
	public DAGTuple execAction(Action a, String w) {
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
		DAGTuple tuple = DAGTuple.getNewTuple(idPoolNodes, res);

		addEdge(edge, cur, tuple);
		tuple.setDepth(cur.getDepth()+1);
		return tuple;
	}




}
