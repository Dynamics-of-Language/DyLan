package qmul.ds.dag;

import java.util.List;

import qmul.ds.action.Action;

public class NewClauseEdge extends GroundableEdge {

	boolean grounded=false;
	
	public NewClauseEdge(List<Action> actions, long newID) {
		super.actions=actions;
		super.word=null;
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
		return "New-Cl: "+(grounded?"gr":"ungr");
	}
	
	public boolean isGrounded()
	{
		return grounded;
	}
	
	
}
