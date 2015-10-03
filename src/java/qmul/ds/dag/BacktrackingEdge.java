package qmul.ds.dag;

import java.util.ArrayList;
import java.util.List;

import qmul.ds.InteractiveContextParser;

public class BacktrackingEdge<E extends DAGEdge> extends GroundableEdge {

	private List<E> backtrackedOver;
	
	public BacktrackingEdge(List<E> a, String speaker, long id) {
		super.actions=null;
		this.backtrackedOver=a;
		this.id=id;
		this.word=new UtteredWord(InteractiveContextParser.repair_init_prefix, speaker);
		
	}
	
	
	public String toString()
	{
		String s= "BacktrackingEdge (id="+id+",word="+word+"): [";
		for(DAGEdge e: this.backtrackedOver)
		{
			s+=e.word()+";";
		}
		return s+"]";
		
	}


	public List<E> getReplayableBacktrackedEdges() {
		List<E> replayable=new ArrayList<E>();
		for(E edge:this.backtrackedOver)
		{
			if (edge.isReplayable())
				replayable.add(edge);
			
		}
		return replayable;
	}
	
	public void markRepairedEdges()
	{
		for(E edge: backtrackedOver)
		{
			edge.setRepaired(true);
		
		}
	}


	public void unmarkRepairedEdges() {
		for(E edge: backtrackedOver)
		{
			edge.setRepaired(false);
		
		}
	}

}
