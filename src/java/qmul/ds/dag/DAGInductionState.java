package qmul.ds.dag;

import java.util.ArrayList;
import java.util.List;

import qmul.ds.DAGParser;
import qmul.ds.action.Action;
import qmul.ds.learn.DAGInductionTuple;
import qmul.ds.learn.TreeHypothesis;
import qmul.ds.tree.Tree;

/**
 * 
 * @author arash
 *
 */

public class DAGInductionState extends DAG<DAGInductionTuple, DAGEdge> {

	private static final long serialVersionUID = 1L;

	
	


	public DAGInductionState(DAGParser<DAGInductionTuple, DAGEdge> p) {
		super(p);
	}

	public DAGInductionState(List<UtteredWord> words,
			DAGParser<DAGInductionTuple, DAGEdge> p) {
		super(words, p);
		
		
	}

	public DAGInductionState(Tree start, List<UtteredWord> words,
			DAGParser<DAGInductionTuple, DAGEdge> parser) {
		super(start, words, parser);
		
	}

	public DAGInductionState(Tree start) {
		this(start, new ArrayList<UtteredWord>(), null);
	}

	public DAGInductionState(List<UtteredWord> asUtteredWords) {
		this(new Tree(), asUtteredWords, null);
	}

	public DAGInductionState(Tree start, List<UtteredWord> asUtteredWords) {
		this(start,asUtteredWords,null);
	}

	@Override
	public DAGInductionTuple execAction(Action a, UtteredWord w) {
		Tree res = a.execTupleContext(cur.getTree().clone(), cur);

		if (res == null)
			return null;

		if (loopDetected(res)) {
			logger.warn("Detected infinite branch. Not extending DAG.");
			logger.warn("History:");
			for (Tree t : lastN) {
				logger.warn(t);
			}
			return null;
		}
		DAGEdge edge = getNewEdge(a.instantiate(), w);
		DAGInductionTuple p = getNewTuple(res);

		addEdge(edge, cur, p);
		p.setDepth(cur.getDepth() + 1);
		return p;
	}

	public DAGInductionTuple addChild(DAGInductionTuple t, Action a, UtteredWord word) {
		if (t == null)
			return null;
		if (!(a instanceof TreeHypothesis) && loopDetected(t.getTree())) {
			logger.info("Detected infinite branch. Not extending DAG.");
			return null;
		}

		DAGEdge edge = getNewEdge(a, word);
		DAGInductionTuple target = getNewTuple(t);
		target.setTarget(cur.getTargetTree());
		target.setNonHeadTarget(cur.getNonHeadTarget());
		addEdge(edge, cur, target);
		return target;
	}

	public boolean addChild(Tree t, Action a, UtteredWord word) {
		if (t == null)
			return false;
		if (!(a instanceof TreeHypothesis) && loopDetected(t)) {
			logger.info("Detected infinite branch. Not extending DAG.");
			logger.debug("Action was:" + a);
			logger.debug("tree was:" + t);
			return false;
		}
		DAGEdge edge = getNewEdge(a.instantiate(), word);
		DAGInductionTuple target = getNewTuple(t);
		target.setTarget(cur.getTargetTree().clone());
		target.setNonHeadTarget(cur.getNonHeadTarget());
		target.setDepth(cur.getDepth());
		return addEdge(edge, cur, target);
	}

	@Override
	public DAGInductionTuple getNewTuple(Tree t) {
		long newID = idPoolNodes.size() + 1;
		DAGInductionTuple result = new DAGInductionTuple(t, newID);
		idPoolNodes.add(newID);
		return result;
	}

	@Override
	public DAGEdge getNewEdge(List<Action> a, UtteredWord w) {
		long newID = idPoolEdges.size() + 1;
		DAGEdge result = new DAGEdge(a, w, newID);
		idPoolEdges.add(newID);
		return result;

	}


	@Override
	public BacktrackingEdge<DAGEdge> getNewBacktrackingEdge(
			List<DAGEdge> backtrackedOver, String speaker) {
		throw new UnsupportedOperationException();
	}

	

	


	

	@Override
	public void groundToClauseRootFor(String speaker, DAGInductionTuple cur)
	{
		throw new UnsupportedOperationException();
	}
	
	

	
	@Override
	public void ungroundToClauseRootFor(String speaker, DAGTuple cur) {
		throw new UnsupportedOperationException();
		
	}

	

	@Override
	public DAGInductionTuple addAxiom(List<Action> list) {
		DAGInductionTuple axiom=this.getNewTuple(new Tree());
		NewClauseEdge edge=this.getNewNewClauseEdge(list);
		this.addChild(axiom, edge);
		return axiom;
		
	}

	@Override
	public void resetToFirstTupleAfterLastWord() {
		if (getFirstTupleAfterLastWord() != null) {

			setCurrentTuple(getFirstTupleAfterLastWord());
			setExhausted(false);
			wordStack().clear();
			resetLastN();// reset the loop detection list
			// removeChildren();
		}
		
	}

	
}
