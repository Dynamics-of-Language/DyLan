package qmul.ds.dag;

import java.util.List;

import qmul.ds.action.Action;

public class CompletionEdge extends GroundableEdge {

	
	
	public CompletionEdge(List<Action> actions, long newID) {
		super.actions=actions;
		super.word=null;
		super.id=newID;
		super.weight=0.0;
		
	}
	
	
	
	
	
	public String toString() {
		return this.id+ "(comp):"+grounded_for;
	}
	
}
