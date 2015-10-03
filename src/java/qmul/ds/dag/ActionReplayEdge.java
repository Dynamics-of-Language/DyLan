package qmul.ds.dag;

import java.util.ArrayList;
import java.util.List;

import qmul.ds.action.Action;

public class ActionReplayEdge extends GroundableEdge {

	private List<GroundableEdge> replayedEdges;
	public ActionReplayEdge(List<Action> actions, UtteredWord word, List<GroundableEdge> edges, long newID) {
		super(actions, word, newID);
		replayedEdges=edges;
		repairable=false;
		//this is a hack. but probably sufficient for dyadic dialogue.
		this.grounded_for.add(edges.get(0).word().speaker());
	}
	public void groundReplayedEdgesFor(String speaker) {
		for(GroundableEdge edge:replayedEdges)
			edge.groundFor(speaker);
		
	}
	
	public void ungroundReplayedEdgesFor(String speaker)
	{
		for(GroundableEdge edge:replayedEdges)
			edge.ungroundFor(speaker);
	}
	
	public List<GroundableEdge> getReplayedEdges()
	{
		List<GroundableEdge> res=new ArrayList<GroundableEdge>();
		for(GroundableEdge e: replayedEdges)
		{
			if (e instanceof ActionReplayEdge)
				res.addAll(((ActionReplayEdge) e).getReplayedEdges());
			else
				res.add(e);
				
		}
		return res;
	}
	
	public String toString()
	{
		String result="replay"+this.grounded_for+":";
		for(GroundableEdge e: getReplayedEdges())
		{
			result+=e.word().word()+";";
		}
		
		return result;
		
	}
	
	
	
	
}
