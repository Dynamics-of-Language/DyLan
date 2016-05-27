package qmul.ds;

import java.util.ArrayList;
import java.util.List;

import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.Variable;
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

	public static final String ENTITY_VARIABLE_ROOT = qmul.ds.tree.Tree.ENTITY_VARIABLE_ROOT;
	public static final String EVENT_VARIABLE_ROOT = qmul.ds.tree.Tree.EVENT_VARIABLE_ROOT;
	public static final String PROPOSITION_VARIABLE_ROOT = qmul.ds.tree.Tree.PROPOSITION_VARIABLE_ROOT;
	public static final String REC_TYPE_VARIABLE_ROOT = qmul.ds.tree.Tree.REC_TYPE_VARIABLE_ROOT;
	public static final String PREDICATE_VARIABLE_ROOT = qmul.ds.tree.Tree.PREDICATE_VARIABLE_ROOT;
	
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
	
	
	public TTRFormula getGroundedContent()
	{
		return dag.getGroundedContent(participants);
		
	}
	
	public TTRFormula getGroundedContent(String speaker)
	{
		return dag.getGroundedContent(speaker);
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

	
	
	
	
	
	
	/**
	 * Methods for getting fresh Record Type variables relative to this context
	 */
	
	private ArrayList<Variable> entityPool = new ArrayList<Variable>();
	private ArrayList<Variable> eventPool = new ArrayList<Variable>();
	private ArrayList<Variable> propositionPool = new ArrayList<Variable>();
	private ArrayList<Variable> recordTypePool = new ArrayList<Variable>();
	private ArrayList<Variable> predicatePool = new ArrayList<Variable>();
	/**
	 * A fresh entity variable x1, x2 etc
	 */
	public Variable getFreshEntityVariable() {
		Variable v = new Variable(ENTITY_VARIABLE_ROOT
				+ (entityPool.size() + 1));
		entityPool.add(v);
		return v;
	}

	/**
	 * A fresh event variable e1, e2 etc
	 */
	public Variable getFreshEventVariable() {
		
		Variable v = new Variable(EVENT_VARIABLE_ROOT + (eventPool.size() + 1));
		
		eventPool.add(v);
		return v;
	}

	/**
	 * A fresh proposition variable p1, p2 etc
	 */
	public Variable getFreshPropositionVariable() {
		Variable v = new Variable(PROPOSITION_VARIABLE_ROOT
				+ (propositionPool.size() + 1));
		propositionPool.add(v);
		return v;
	}

	/**
	 * A fresh record type variable r1, r2 etc
	 */
	public Variable getFreshRecTypeVariable() {
		Variable v = new Variable(REC_TYPE_VARIABLE_ROOT
				+ (recordTypePool.size() + 1));
		recordTypePool.add(v);
		return v;
	}

	public Variable getFreshPredicateVariable() {
		Variable v = new Variable(PREDICATE_VARIABLE_ROOT
				+ (predicatePool.size() + 1));
		predicatePool.add(v);
		return v;

	}

	
	
}