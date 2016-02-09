package qmul.ds.dag;

import java.util.List;

import qmul.ds.action.Action;

public class NewClauseEdge extends GroundableEdge {

	boolean grounded=false;
	
	public NewClauseEdge(List<Action> actions, UtteredWord word, long newID) {
		super.actions=actions;
		super.word=word;
		super.id=newID;
		super.weight=0.0;
		
	}
	
	
	public void ground()
	{
		grounded=true;
	}
	public void unground()
	{
		grounded=false;
	}
	
	public String toString()
	{
		return super.toString()+"(NC"+(grounded?"-gr":"-ugr")+")";
	}
	
	public boolean isGrounded()
	{
		return grounded;
	}
	
	
	
	
}
