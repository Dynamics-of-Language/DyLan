package qmul.ds.dag;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;

/**
 * A generic Edge in a context DAG. Can be subclassed to include more contextual info.
 * 
 * WARNING: Do NOT use the constructors to create a new edge object. Use the getNew[edge type]Edge methods provided in the DAG class. 
 * (i.e. the {@link DAG} class is its own edge factory (and tuple (node) factory)).
 * 
 * 
 * @author Arash
 *
 */
public class DAGEdge implements Comparable<DAGEdge> {

	protected static Logger logger = Logger.getLogger(DAGEdge.class);
	
	public static final int SEEN = 1;
	public static final int REPAIRED = 2;
	public static final int IN_CONTEXT = 3;
	public static final int GROUNDED = 4;

	/**
	 * The edge's unique id. Hashcode just returns id. Equals method works based on this.
	 */
	protected Long id = 0L;
	
	
	protected List<Action> actions = null;
	protected UtteredWord word = null;
	
	protected double weight = 0.5;
	
	protected Set<Integer> edge_properties = new HashSet<Integer>();
	
	

	DAGEdge() {
		this.actions = new ArrayList<Action>();
	}

	DAGEdge(Action a, UtteredWord w) {
		this.word = w;
		this.actions = new ArrayList<Action>();
		this.actions.add(a);
	}

	DAGEdge(Action a, UtteredWord w, long id) {
		this(a, w);
		this.id = id;
	}

	DAGEdge(Action a) {
		this(a, null);
	}

	DAGEdge(List<Action> a, UtteredWord w) {
		this.word = w;
		this.actions = a;
	}

	DAGEdge(List<Action> a, UtteredWord w, long id) {
		this(a, w);
		this.id = id;
	}

	
	DAGEdge(List<Action> a) {
		this(a, null);
	}

	public DAGEdge(UtteredWord w, long id) {
		this.word=w;
		this.id=id;
		this.actions=null;
	}

	public void setID(long id) {
		this.id = id;
	}

//	public void setParentEdgeId(long id)
//	{
//		this.pid=id;
//	}
	/**
	 * WARNING: only valid of DAGEdge contains one action only - e.g. doesn't
	 * work with ContextualWordEdge
	 * 
	 * @return
	 */
	public Action getAction() {
		return this.actions.get(0);
	}

	public List<Action> getActions() {
		return actions;
	}

	public boolean hasBeenSeen() {
		return edge_properties.contains(SEEN);
	}

	// public boolean hasBeenBacktracked(){
	// return edge_properties.contains(BACKTRACKED);
	// }

	public UtteredWord word() {
		return this.word;
	}

	public void setSeen(boolean b) {
		if (b) {
			edge_properties.add(SEEN);
		} else if (edge_properties.contains(SEEN))
			edge_properties.remove(SEEN);
	}

	public void setRepaired(boolean b) {
		if (b)
			edge_properties.add(REPAIRED);
		else if (edge_properties.contains(REPAIRED))
			edge_properties.remove(REPAIRED);

	}

	public void setInContext(boolean b) {
		if (b)
			edge_properties.add(IN_CONTEXT);
		else if (edge_properties.contains(IN_CONTEXT))
			edge_properties.remove(IN_CONTEXT);

	}
	
	public void ground() {
		edge_properties.add(GROUNDED);
		
	}

	public void unground() {
		if (edge_properties.contains(GROUNDED))
			edge_properties.remove(GROUNDED);
		
		
	}
	public boolean inContext() {
		return edge_properties.contains(IN_CONTEXT);
	}

	public boolean isRepaired() {
		return edge_properties.contains(REPAIRED);
	}

	/*
	 * public void setBacktracked(boolean b) { if (b)
	 * edge_properties.add(BACKTRACKED); else if
	 * (edge_properties.contains(BACKTRACKED))
	 * edge_properties.remove(BACKTRACKED);
	 * 
	 * }
	 */

	public boolean isGrounded()
	{
		return edge_properties.contains(GROUNDED);
	}
	
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof DAGEdge))
			return false;
		DAGEdge other = (DAGEdge) o;
		return this.id.equals(other.id);
	}

	public int hashCode() {

		return id.hashCode();
	}

	public void setWeight(double w) {
		this.weight = w;
	}

	@Override
	public int compareTo(DAGEdge other) {
		if (this.weight > other.weight)
			return -1;
		else if (this.weight < other.weight)
			return 1;

		return this.hashCode() - other.hashCode();
	}

	public String toString() {
		String res = "[";
		for (int i = 0; i < actions.size(); i++)
			res += actions.get(i).getName() + ";";

		return res + "]";
	}

	/**
	 * 
	 * @param t
	 * @return the label for this edge when visualised using Jung
	 *         VisualizationViewer. Subclasses can override this method.
	 */
	public String getEdgeLabel() {
		return word!=null?word.toString():toString();
	}

	public Stroke getEdgeStroke() {

		if (isRepaired()) {
			float dash[] = { 10.0f };
			final Stroke edgeStroke = new BasicStroke(1.0f,
					BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash,
					0.0f);
			return edgeStroke;
		} else
			return new BasicStroke();

	}
	
	public void setActions(List<Action> actions)
	{
		this.actions=actions;
	}
	
	public String toDebugString()
	{
		return toString();
	}

	

	public boolean initiatesNewClause()
	{
		
		return getActions().get(0).getName().equals("trp");//WARNING: this is sensitive to the name of the new clause initiating computational action
	}
	

}
