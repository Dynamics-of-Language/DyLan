package qmul.ds.dag;

import java.util.ArrayList;
import java.util.List;

import qmul.ds.InteractiveContextParser;

public class BacktrackingEdge extends GroundableEdge {

	private List<GroundableEdge> backtrackedOver;
	public static String repair_init_prefix="init-repair";
	VirtualRepairingEdge overarchingRepairingEdge;
	
	public BacktrackingEdge(List<GroundableEdge> a, String speaker, long id) {
		super.actions=null;
		this.backtrackedOver=a;
		this.id=id;
		this.word=new UtteredWord(repair_init_prefix, speaker);
		
	}
	
	public BacktrackingEdge(List<GroundableEdge> a, String speaker, VirtualRepairingEdge repairing, long id)
	{
		this(a,speaker,id);
		this.overarchingRepairingEdge=repairing;
	}
	
	
	public String toString()
	{
		String s= "BacktrackingEdge (id="+id+",word="+word+"): ["+this.overarchingRepairingEdge.edgePair.second.word()+"]";
		return s;
		//		for(DAGEdge e: this.backtrackedOver)
//		{
//			s+=e.word()+";";
//		}
//		return s+"]";
		
	}


	public List<GroundableEdge> getReplayableBacktrackedEdges() {
		List<GroundableEdge> replayable=new ArrayList<GroundableEdge>();
		for(GroundableEdge edge:this.backtrackedOver)
		{
			if (edge.isRepairable())
				replayable.add(edge);
			
		}
		return replayable;
	}
	
	public void markRepairedEdges()
	{
		for(DAGEdge edge: backtrackedOver)
		{
			edge.setRepaired(true);
		
		}
	}


	public void unmarkRepairedEdges() {
		for(DAGEdge edge: backtrackedOver)
		{
			edge.setRepaired(false);
		
		}
	}
	
	public void traverse(WordLevelContextDAG dag)
	{
//		DAGTuple source = dag.getSource(this);
//		if (!source.equals(dag.getCurrentTuple())) {
//			logger.error("Didn't traverse edge:" + this);
//			logger.error("Because its source isn't the pointed tuple on the dag.");
//			throw new IllegalStateException();
//		}
//		DAGTuple dest = dag.getDest(this);
		
//		if (!dag.wordStack.isEmpty()&&dag.wordStack.peek().equals(this.word()))
//			dag.wordStack().pop();
//		else if (this.word()!=null) {
//			logger.error("Trying to pop word off word stack when going along "
//					+ this);
//			logger.error("but word on stack is:" + dag.wordStack().peek());
//			throw new IllegalStateException("top of stack is not the same as word on this edge, or stack is empty");
//		}
		logger.info("Going forward along:" + this);
		super.traverse(dag);
		if (!dag.wordStack().isEmpty()&&!dag.wordStack().peek().equals(this.word))
			throw new IllegalStateException("Popping "+dag.wordStack().peek() +" off stack when traversing:"+this);
		else if (!dag.wordStack().isEmpty())
			dag.wordStack().pop();
		/**
		 * For these edges the popping is not done by goFirst, but by the corresponding traverse methods....
		 * Because two words need to be popped off (init-repair, and the repair word).
		 */
		
		markRepairedEdges();
		//
		dag.actionReplay.addAll(getReplayableBacktrackedEdges());
		//commented out for now.
		//dag.groundToClauseRootFor(word.speaker(), dag.getDest(this));
	
	}
	
	public void backtrack(WordLevelContextDAG dag)
	{
		if (!dag.getDest(this).equals(dag.getCurrentTuple()))
		{
			throw new IllegalStateException("the edge to be backtracked does not have its destination as the current dag tuple");
		}
		unmarkRepairedEdges();
		dag.actionReplay.clear();
		
		logger.debug("going back (forward) over backtrakcing edge: "+this);
		dag.setCurrentTuple(dag.getSource(this));
		
		setSeen(true);
		logger.info("Backtracked over: "+this);
		logger.info("adding word to stack now: "+word);
		dag.wordStack().push(word);
		
	}
	
	public void setSeen(boolean seen)
	{
		super.setSeen(seen);
		overarchingRepairingEdge.setSeen(seen);
	}
	
	public boolean hasBeenSeen()
	{
		return overarchingRepairingEdge.hasBeenSeen();
	}

	public void setInContext(boolean b)
	{
		overarchingRepairingEdge.setInContext(b);
	}
	
	public boolean inContext()
	{
		return overarchingRepairingEdge.inContext();
	}
}
