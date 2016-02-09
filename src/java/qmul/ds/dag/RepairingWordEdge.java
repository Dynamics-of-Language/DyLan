package qmul.ds.dag;

import java.util.List;

import qmul.ds.action.Action;
/**
 * TODO: implement traverse and backtrack methods.
 * @author Arash
 *
 */
public class RepairingWordEdge extends GroundableEdge {
	
	VirtualRepairingEdge overarchingRepairingEdge=null; //is set when the overarching repairing edge is created. These edges are not created directly.
	
	public RepairingWordEdge(List<Action> repairingActions, UtteredWord word, long id)
	{
		super(repairingActions, word, id);
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

}
