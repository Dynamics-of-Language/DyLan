package qmul.ds;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGState;
import qmul.ds.dag.DAGTuple;
import qmul.ds.tree.Tree;

public class DAGParseState extends ParseState<ParserTuple> {

	/**
	 * 
	 */
	
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(DAGParseState.class);

	private DAGState state;

	public DAGParseState() {
		super();
		state = new DAGState();
		add(state.getCurrentTuple());
	}

	public DAGParseState(List<String> words) {
		super();
		state = new DAGState(words);
		add(state.getCurrentTuple());
	}

	public DAGParseState(Tree start) {
		super();
		state = new DAGState(start);
		add(state.getCurrentTuple());
	}

	public DAGParseState(Tree start, List<String> words) {
		super();
		state = new DAGState(start, words);
		add(state.getCurrentTuple());
	}

	public void resetToFirstTupleAfterLastWord() {
		if (state.getFirstTupleAfterLastWord() != null) {
			clear();
			state.setCurrentTuple(state.getFirstTupleAfterLastWord());
			add(state.getCurrentTuple());
			state.setExhausted(false);
			wordStack().clear();
			state.resetLastN();//reset the loop detection list
			//removeChildren();
		}

	}
	
	
	public void thisIsFirstTupleAfterLastWord() {
		state.thisIsFirstTupleAfterLastWord();
	}

	public void setExhausted(boolean a) {
		state.setExhausted(a);
	}

	public void setCurrentTuple(DAGTuple result) {
		state.setCurrentTuple(result);

	}

	public DAGTuple getCurrentTuple() {
		return state.getCurrentTuple();
	}

	// this will return null if action returns null or if it's already been tried and failed.
	public DAGTuple execAction(Action a, String word) {
		return state.execAction(a, word);

	}

	public Action getParentAction() {

		return state.getPrevAction();
	}

	public boolean atRoot() {
		return state.atRoot();
	}

	/**
	 * Moves pointer to first child. If the action edge is lexical pops the corresponding word off the remaining words
	 * stack.
	 * 
	 * @return
	 */

	public DAGEdge goFirst() {
		return state.goFirst();

	}

	public DAGEdge goUpOnce() {
		return state.goUpOnce();
	}

	public Stack<String> wordStack() {
		return state.wordStack();
	}

	public boolean isComplete() {
		return state.getCurrentTuple().isComplete();
	}

	public ArrayList<Action> getActionSequence() {
		return state.getActionSequence();
	}
	public ArrayList<Action> getActionSequence(ParserTuple cur) {
		return state.getActionSequence((DAGTuple)cur);
	}

	public long getDepth() {
		return state.getDepth();
	}

	public void init() {
		clear();
		state = null;
		state = new DAGState();
		add(state.getCurrentTuple());
	}
	
	public void init(Tree t)
	{
		clear();
		state=null;
		state=new DAGState(t);
		add(state.getCurrentTuple());
	}
	public boolean isExhausted() {
		return state.isExhausted();

	}

	public void removeChildren() {
		state.removeChildren();

	}

	public boolean moreUnseenEdges() {

		return state.moreUnseenEdges();
	}

	public void markOutEdgeAsSeen(DAGEdge backOver) {
		state.markOutEdgeAsSeen(backOver);

	}

	public void removeChild(DAGTuple t) {
		state.removeChild(t);
		
	}

	public boolean hasOutWordEdge(String w)
	{
		for (Action a:getOutActions())
		{
			if (a.getName().equals(w))
				return true;
		}
		return false;
	}
	public List<Action> getOutActions() {
		List<Action> actions=new ArrayList<Action>();
		for(DAGEdge edge: state.getOutEdges())
		{
			actions.add(edge.getAction());
		}
		return actions;
	}

}
