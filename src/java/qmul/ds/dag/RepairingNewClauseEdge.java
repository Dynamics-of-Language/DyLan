package qmul.ds.dag;

import java.util.List;

import qmul.ds.action.Action;

public class RepairingNewClauseEdge extends NewClauseEdge {

	VirtualRepairingEdge overarchingRepairingEdge=null;
	
	public RepairingNewClauseEdge(List<Action> actions, UtteredWord word, long newID) {
		super(actions, word, newID);
		
	}
	
	public void setSeen(boolean seen)
	{
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
	
	public void traverse(WordLevelContextDAG dag)
	{
		super.traverse(dag);
		if (!dag.wordStack().isEmpty()&&!dag.wordStack().peek().equals(this.word))
			throw new IllegalStateException("Popping "+dag.wordStack().peek() +" off stack when traversing:"+this);
		else if (!dag.wordStack().isEmpty())
			dag.wordStack().pop();
		
	}

}
