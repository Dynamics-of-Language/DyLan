package qmul.ds;

import java.util.ArrayList;
import java.util.List;

import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.WordLevelContextDAG;
import edu.uci.ics.jung.graph.Tree;

/**
 * This class encodes the dialogue context, containing a Directed Asyclic Graph, which
 * as per Eshghi et. al. (2015), encodes the DS procedural context with information about 
 * speaker/hearer coordination, and grounding. Importantly, this is where the grammar
 * would interface with the non-linguistic, situational context. In situated dialogue, subclasses of this class can encode non-linguistic
 * contextual information, e.g. sets of individuated, typed objects in a scene.
 * 
 * 
 * @author Arash
 */

public class Context<T extends DAGTuple, E extends DAGEdge> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8765365147853341079L;


	protected List<String> participants;
	protected DAG<T,E> dag;
	
	public Context(DAG<T,E> dag)
	{
		this(dag, new ArrayList<String>());
	}
	
	public Context(DAG<T,E> dag, List<String> part)
	{
		this.dag=dag;
		participants=part;
		
	}
	
	
	
	
	
	public void addParticipant(String name)
	{
		participants.add(name);
	}	
	
	public void removeParticipant(String name)
	{
		participants.remove(name);
	}
	
	public Tree<T,E> getActiveDAG()
	{
		return dag.getInContextSubgraph();
	}
	
	public DAG<T,E> getDAG()
	{
		return dag;
	}

	public void setDAG(DAG<T, E> state) {
		this.dag=state;
		
	}


	public void groundToRoot() {
		dag.groundToRootFor(getCurrentSpeaker());
		
	}
	

	/**
	 * TODO: this isn't going work in generation as the stack will be empty.
	 * @return
	 */
	public String getCurrentSpeaker()
	{
		return (dag.wordStack().isEmpty())?null:dag.wordStack().peek().speaker();
	}
	
	public String getPrevSpeaker()
	{
		return dag.getSpeakerOfPreviousWord();
	}
	
	

	public T getCurrentTuple() {
		return dag.getCurrentTuple();
	}
	
	public void addAxiom()
	{
		dag.addAxiom();
	}

	public T getParent(T cur) {
		return dag.getParent(cur);
	}
	
	/**
	 * deems the current tuple to have been asserted by the last speaker.
	 */
	public void setAcceptancePointer()
	{
		dag.setAcceptancePointer(dag.getSpeakerOfPreviousWord());
	}
	
	
}