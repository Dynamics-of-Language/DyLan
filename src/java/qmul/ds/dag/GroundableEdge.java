package qmul.ds.dag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import qmul.ds.action.Action;

public class GroundableEdge extends DAGEdge {
	
	
	protected Set<String> grounded_for=new HashSet<String>();
	boolean repairable=true;
	public GroundableEdge()
	{
		
	}

	public GroundableEdge(Action a, UtteredWord w) {
		super(a,w);
		if (w!=null)
			this.grounded_for.add(w.speaker());
	}

	public GroundableEdge(Action a, UtteredWord w, long id) {
		this(a, w);
		this.id = id;
	}

	public GroundableEdge(List<Action> a, UtteredWord w) {
		super(a,w);
		if (w!=null)
			this.grounded_for.add(w.speaker());
	}

	public GroundableEdge(List<Action> a, UtteredWord w, long id) {
		this(a, w);
		this.id = id;
	}

	
	public GroundableEdge(List<Action> a) {
		this(a, null);
	}
	
	public void groundFor(String speaker)
	{
		this.grounded_for.add(speaker);
	}
	
	public String toString()
	{
		return grounded_for+":"+word.word();
	}
	
	public String getEdgeLabel() {
		return toString();
	}

	public boolean isGroundeFor(String speaker) {
		return grounded_for.contains(speaker);
	}

	public void ungroundFor(String speaker) {
		grounded_for.remove(speaker);
		
	}
	
	public boolean isRepairable()
	{
		return repairable;
	}
	
	public void setRepairable(boolean b)
	{
		this.repairable=b;
		
	}

}
