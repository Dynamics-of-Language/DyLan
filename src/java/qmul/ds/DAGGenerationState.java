package qmul.ds;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.action.LexicalAction;
import qmul.ds.dag.DAGEdge;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.tree.Tree;
import edu.stanford.nlp.ling.Word;

public class DAGGenerationState extends GenerationState<ParserTuple> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DAGParseState state;
	private List<Word> words;
	protected static Logger logger=Logger.getLogger(DAGGenerationState.class);
	public DAGParseState getParseState() {
		return state;
	}
	public void setParseState(DAGParseState state) {
		this.state = state;
	}
	public ParserTuple getGoal() {
		return goal;
	}
	public void setGoal(ParserTuple goal) {
		this.goal = goal;
	}
	public List<Word> getWords() {
		return words;
	}
		
	public DAGGenerationState()
	{
		this.state=new DAGParseState();
		this.words=new ArrayList<Word>();
	}
	public DAGGenerationState(DAGParseState state)
	{
		this.state=state;
		this.words=new ArrayList<Word>();
	}
	public DAGGenerationState(DAGParseState state, TTRRecordType goal)
	{
		this.goal=new ParserTuple(goal);
		this.state=state;
		words=new ArrayList<Word>();
	}
	
	public DAGGenerationState(TTRRecordType goal)
	{
		this();
		this.goal=new ParserTuple(goal);
	}
	
	public DAGGenerationState(Tree goal)
	{
		this();
		this.goal=new ParserTuple(goal);
	}
	public boolean subsumed() {
		
		return state.getCurrentTuple().subsumes(goal);
	}
	
	public boolean matched() {
		return state.getCurrentTuple().subsumes(goal) && goal.subsumes(state.getCurrentTuple());
	}
	public void generate(String w) {
		this.words.add(new Word(w));
		
	}
	
	public String toString()
	{
		
		
		String result = this.state.getCurrentTuple().toString();
		result+="\nWords: "+this.words;
		return result;
	}
	
	public void backtrackLastWord()
	{
		Action prev=state.getParentAction();
		if (prev==null)
			return;
		/*
		 * find the first lexical action behind current tuple
		 */
		while(!(state.atRoot()||prev instanceof LexicalAction))
		{
			state.goUpOnce();
			prev=state.getParentAction();			
		}
		/*
		 * now backtrack to the previous lexical action
		 */
		if (state.atRoot())
			return;
		
		DAGEdge edge=state.goUpOnce();//go back over it.
		state.markOutEdgeAsSeen(edge);
		logger.debug("Now Going back over "+edge.getAction().getName());
		logger.debug("at "+state.getCurrentTuple());
		//now find the previous and stay there.
		while(!(state.atRoot()||prev instanceof LexicalAction))
		{
			state.goUpOnce();
			prev=state.getParentAction();
			
		}
		state.thisIsFirstTupleAfterLastWord();
		
		
	}
		
		
	
	
	

}
