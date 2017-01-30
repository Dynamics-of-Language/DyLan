package qmul.ds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.uci.ics.jung.graph.Tree;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.formula.Variable;
import qmul.ds.ttrlattice.AustinianProp;

/**
 * This class encodes the dialogue context, containing a Directed Asyclic Graph, which
 * as per Eshghi et. al. (2015), encodes the DS procedural context with information about 
 * speaker/hearer coordination, and grounding. It also encodes information about dialogue participants,
 * floor status, etc. (could include time/place). It also provides methods for getting the grounded content of the 
 * conversation (optimistically or otherwise, akin to Ginzburgh (2009)'s FACTS), or content asserted by a particular speaker.
 * 
 * This is where the grammar would interface with the non-linguistic, situational context. 
 * In situated dialogue, subclasses of this class can encode non-linguistic,
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
	@SuppressWarnings("unused")
	private static final long serialVersionUID = -8765365147853341079L;
	
	protected Logger logger=Logger.getLogger(Context.class);


	
	/**
	 * this is a map from dialogue participants to the content asserted by them. This should is maintained as the dialogue goes 
	 * forward. 
	 * 
	 * Currently this is ONLY used to maintain the participants themselves. Asserted contents are as per 
	 * annotations on the trees in the DAG.
	 * 
	 */
	protected Map<String, TreeSet<AustinianProp>> asserted_contents=null;
	protected DAG<T,E> dag;
	
	protected String myName;
	protected String whoHasFloor=null;
	
	public void initParticipantContents(Set<String> participants)
	{
		asserted_contents=new HashMap<String, TreeSet<AustinianProp>>();
		for(String s:participants)
		{
			asserted_contents.put(s, new TreeSet<AustinianProp>());
		}
		
	}
	
	
	public Context(DAG<T,E> dag, String... participants)
	{
		this.dag=dag;
		dag.setContext(this);
		this.myName=participants[0];
		initParticipantContents(new HashSet<String>(Arrays.asList(participants)));
	}
	
	/**
	 * Returns the grounded content of the conversation with a strict grounding strategy 
	 * (requires explicit acceptance from other). Contrast {@link getCautiouslyOptimisticGroundedContent}
	 * @return 
	 */
	public TTRFormula getGroundedContent()
	{
	
		TTRFormula accepted=dag.getGroundedContent(asserted_contents.keySet()).removeHead();
		if (!(accepted instanceof TTRRecordType))
			throw new UnsupportedOperationException("accepted content not a record type");
		
		((TTRRecordType)accepted).collapseIsomorphicSuperTypes(new HashMap<Variable, Variable>());
		return accepted;
		
	}
	
	
	/**Returns the set of AustinianProps asserted by speaker. The set is ordered by assertion time. Latest first.
	 * 
	 * @param speaker
	 * @return
	 */
	public TreeSet<AustinianProp> getAssertions(String speaker)
	{
		return dag.getAssertions(speaker);
	}
	/**
	 * Gets all Austinian Props asserted in the conversation. 
	 * Doesn't care whether these were grounded. This is a hacky version of an implementation for 
	 * cautiously optimistic grounding.
	 * Is not guaranteed to work for all domains..... 
	 * 
	 * We do not yet have a proper model of this.... 
	 * 
	 * @return
	 */
	public TreeSet<AustinianProp> getAllAssertions()
	{
		return dag.getAssertions(this.getParticipants());
	
	}
	
	
	
	
	
	/**
	 * get cautiously optimistic grounded content - all asserted content on the path back to root.
	 * Assumes all rejections/corrections are on repaired paths (and so not on the path back to root).
	 * @param speaker
	 * @return Cautiously optimistic grounded content
	 */
	
	public TTRFormula getCautiouslyOptimisticGroundedContent()
	{
		TTRFormula result=new TTRRecordType();
		for(String participant: this.asserted_contents.keySet())
		{
			logger.debug("conjoining content for "+participant);
			
			TTRFormula assertedContent=dag.getGroundedContent(participant);
			result=result.conjoin(assertedContent);
			
		}
		TTRFormula headless=result.removeHead();
		if (!(headless instanceof TTRRecordType))
			throw new UnsupportedOperationException("accepted content not a record type");
		
		((TTRRecordType)headless).collapseIsomorphicSuperTypes(new HashMap<Variable, Variable>());
		return headless;
	}
	
	public boolean floorIsOpen()
	{
		return whoHasFloor==null;
	}
	
	public String whoHasFloor()
	{
		return whoHasFloor;
	}

	
	
	public void addParticipant(String name)
	{
		if (asserted_contents.keySet().contains(name))
			return;
		
		asserted_contents.put(name, new TreeSet<AustinianProp>());
	}	
	
	public void removeParticipant(String name)
	{
		asserted_contents.remove(name);
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
	 * WARNING: this isn't going work in generation as the stack will be empty.
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
	


	public Set<String> getParticipants()
	{
		return asserted_contents.keySet();
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
	
	public void resetVariablePools()
	{
		entityPool.clear();
		eventPool.clear();
		propositionPool.clear();
		recordTypePool.clear();
		predicatePool.clear();
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

	/** A fresh predicate variable (of cn type)
	 * 
	 * @return
	 */
	public Variable getFreshPredicateVariable() {
		Variable v = new Variable(PREDICATE_VARIABLE_ROOT
				+ (predicatePool.size() + 1));
		predicatePool.add(v);
		return v;

	}

	public String getCurrentAddressee() {
		return (dag.wordStack().isEmpty())?null:dag.wordStack().peek().addressee();
	}

	
	
	public void init()
	{
		dag.init();
		resetVariablePools();
		initParticipantContents(getParticipants());
	}
	
	public void init(List<String> participants)
	{
		dag.init();
		resetVariablePools();
		initParticipantContents(new HashSet<String>(participants));
	}

	public void setRepairProcessing(boolean repairing) {
		dag.setRepairProcessing(repairing);
		
	}

	public boolean repairInitiated() {
		return dag.repairInitiated();
	}
	
	
	
	public String printAcceptedContents()
	{
		String result="";
		for(String speaker:this.asserted_contents.keySet())
			result+=speaker+":"+asserted_contents.get(speaker)+"\n";
		
		return result;
			
	}
	public void openFloor() {
		this.whoHasFloor=null;
		
	}
	
	
	public void setWhoHasFloor(String speaker) {
		this.whoHasFloor=speaker;		
	}


	public String getName() {
		return myName;
	}
	/**
	 * 
	 * @return 0 if floor is open, 1 if I have floor, 2 if another has floor
	 */
	public int floorStatus()
	{
		if (floorIsOpen())
			return 0;
		
		if (myName.equals(this.whoHasFloor))
			return 1;
		else 
			return 2;
	}


	public TTRFormula conjoinAllTurnContent() {
		TTRFormula allContent=dag.conjoinAllTurnContent().removeHead();
		
		if (!(allContent instanceof TTRRecordType))
			throw new UnsupportedOperationException("accepted content not a record type");
		
		//return allContent;
		((TTRRecordType)allContent).collapseIsomorphicSuperTypes(new HashMap<Variable, Variable>());
		return allContent;
	}

	
	
}