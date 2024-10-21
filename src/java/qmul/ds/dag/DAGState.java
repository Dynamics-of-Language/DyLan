package qmul.ds.dag;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.tree.Tree;
/**
 * @author arash
 */
public class DAGState extends DAG<DAGTuple, DAGEdge> {

	
	public DAGState() {
		super();
	}

	public DAGState(List<UtteredWord> words) {
		super(new Tree(), words);
	}
	
	public DAGState(Tree start) {
		super(start, new ArrayList<UtteredWord>());
	}

	public DAGState(Tree start, List<UtteredWord> words) {
		super(start, words);
	}


	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(DAGState.class);
	

	
	@Override
	public DAGEdge getNewEdge(List<Action> actions, UtteredWord word) {
		long newID = idPoolEdges.size() + 1;
		DAGEdge result = new DAGEdge(actions, word, newID);
		idPoolEdges.add(newID);
		return result;
	}


	@Override
	public DAGTuple getNewTuple(Tree t) {
		long newID = idPoolNodes.size() + 1;
		DAGTuple result = new DAGTuple(t,newID);
		idPoolNodes.add(newID);
		return result;
	}


	@Override
	public DAGTuple execAction(Action a, UtteredWord w) {
		Tree res = a.exec(cur.getTree().clone(), context);
		logger.debug("DAGState: executing "+a+" on" +cur.getTree());
		logger.debug("Result:"+res);
		if (res == null)
			return null;

		if (loopDetected(res)) {
			logger.warn("Detected infinite branch. Not extending DAG.");
			logger.trace("History:");
			for(Tree t:lastN)
			{
				logger.trace(t);
			}
			return null;
		}
		DAGEdge edge = getNewEdge(a.instantiate(), w);
		DAGTuple tuple = getNewTuple(res);

		addEdge(edge, cur, tuple);
		tuple.setDepth(cur.getDepth()+1);
		return tuple;
	}


	public void resetToFirstTupleAfterLastWord() {
		if (getFirstTupleAfterLastWord() != null) {
			setCurrentTuple(getFirstTupleAfterLastWord());
			setExhausted(false);
			wordStack().clear();
			resetLastN();// reset the loop detection list
			// removeChildren();
		}
	}



	@Override
	public BacktrackingEdge getNewBacktrackingEdge(List<GroundableEdge> backtrackedOver, String speaker) {
		throw new UnsupportedOperationException();
	}


	@Override
	public void groundToClauseRootFor(String speaker, DAGTuple cur) {
		throw new UnsupportedOperationException();
	}


	@Override
	public void ungroundToClauseRootFor(String speaker, DAGTuple cur) {
		throw new UnsupportedOperationException();
	}
	
	
	public DAGTuple addAxiom(List<Action> actions, UtteredWord word) {
			DAGTuple axiom=this.getNewTuple(new Tree());
			DAGEdge edge=this.getNewEdge(actions, word);
			this.addChild(axiom, edge);
			return axiom;
	}


	@Override
	public VirtualRepairingEdge getNewRepairingEdge(List<GroundableEdge> backtrackedOver,
			List<Action> repairingActions, DAGTuple midTuple,
			UtteredWord repairingWord) {
		throw new UnsupportedOperationException("Repairing edges not supported in this DAG class");
	}


	@Override
	public RepairingWordEdge getNewRepairingWordEdge(List<Action> actions,
			UtteredWord word) {
		throw new UnsupportedOperationException("Repairing Word Edge not supported in this DAG class");
	}


	@Override
	public boolean rollBack(int n) {
		throw new UnsupportedOperationException("Operation not yet supported");
	}

}
