package qmul.ds.dag;

import java.util.List;

import qmul.ds.action.Action;

public class GroundingEdge extends GroundableEdge {
	
	boolean yes=true;

	public GroundingEdge(List<Action> a, UtteredWord w, long id) {
		super(a,w,id);
		this.repairable=false;
		this.replayable=false;
	}

	public void setYes(boolean yes)
	{
		this.yes=yes;
	}

}
