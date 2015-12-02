package qmul.ds.dag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import qmul.ds.Context;
import qmul.ds.DAGParser;
import qmul.ds.ParserTuple;
import qmul.ds.action.Action;
import qmul.ds.tree.Tree;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Forest;

/**
 * A generic DS (parse) Directed Acyclic Graph. Edges correspond minimally to
 * actions, but could be sequences of actions (see e.g. @ContextualWordEdge).
 * Nodes to to trees. But these could be subclassed, e.g. to include contextual
 * info like speaker/hearer, info about repaired edges, saliency etc.
 * 
 * This class now supports self/other-repair (CRs, corrections) structures, where backtracking cycles are introduced into the graph.
 * These cycles are implicit and internal to graph search. For all intents and purposes, this graph behaves as a directed asyclic graph.
 * 
 * 
 * 
 * @author arash
 * @param <T>
 *            Type of nodes in the dag
 * @param <E>
 *            Type of edges in the dag
 */
public abstract class DAG<T extends DAGTuple, E extends DAGEdge> extends
		DirectedSparseMultigraph<T, E> implements Forest<T, E>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	

	protected static Logger logger = Logger.getLogger(DAG.class);
	
	//------------------------------------------------
	protected Context<T,E> context=null;
	
	//protected Grammar completionGrammar;
	protected T root;
	protected T cur;
	protected Stack<UtteredWord> wordStack;
	private T firstTupleAfterLastWord;
	private boolean exhausted = false;
	protected List<Long> idPoolNodes = new ArrayList<Long>();
	protected List<Long> idPoolEdges = new ArrayList<Long>();

	protected List<E> actionReplay=new ArrayList<E>();
	
	protected DAGParser<T,E> parser;
	
	
	protected Map<String, Set<T>> acceptance_pointers = new HashMap<String, Set<T>>();
	
	public abstract T getNewTuple(Tree t);

	public abstract E getNewEdge(List<Action> actions, UtteredWord word);
	
	
	public abstract BacktrackingEdge<E> getNewBacktrackingEdge(List<E> backtrackedOver, String speaker);
	
	public ActionReplayEdge getNewActionReplayEdge(List<Action> actions, UtteredWord w, List<GroundableEdge> edges)
	{
		long newID = idPoolEdges.size() + 1;
		ActionReplayEdge result = new ActionReplayEdge(actions, w, edges, newID);
		idPoolEdges.add(newID);
		return result;
		
		
	}
	
	
	public void setSelfPointer() {
		this.setAcceptancePointer("self");

	}

	
	public void setAcceptancePointer(String other, T cur) {
		if (!acceptance_pointers.containsKey(other))
			acceptance_pointers.put(other, new HashSet<T>());
		
		acceptance_pointers.get(other).add(cur);
	}
	
	public void setAcceptancePointer(String other)
	{
		setAcceptancePointer(other, cur);
	}
	
	public boolean atRoot()
	{
		return cur==root;
	}

	public E getNewEdge(Action action) {
		List<Action> l = new ArrayList<Action>();
		l.add(action);
		return getNewEdge(l);
	}
	
	
	
	public NewClauseEdge getNewNewClauseEdge(List<Action> actions) {
		long newID = idPoolEdges.size() + 1;
		NewClauseEdge cl=new NewClauseEdge(actions, newID);
		idPoolEdges.add(newID);
		return cl;
		
	}

	
	public GroundableEdge getNewGroundingEdge(ArrayList<Action> newActs,
			UtteredWord w) {
		long newID = idPoolEdges.size() + 1;
		GroundingEdge cl=new GroundingEdge(newActs, w, newID);
		idPoolEdges.add(newID);
		return cl;
	}
	
	public E getNewEdge(Action action, UtteredWord word) {
		List<Action> l = new ArrayList<Action>();
		l.add(action);
		return getNewEdge(l, word);
	}

	public E getNewEdge(List<Action> actions) {
		return getNewEdge(actions, null);
	}

	public T getNewTuple(ParserTuple t) {
		return getNewTuple(t.getTree());
	}

	
	
	
	public DAG(Tree start, List<UtteredWord> words, DAGParser<T,E> parser)
	{
		this.parser=parser;
		wordStack = new Stack<UtteredWord>();
		for (int i = words.size() - 1; i >= 0; i--) {
			wordStack.push(words.get(i));
		}
		cur = getNewTuple(start);
		addVertex(cur);
		root = cur;
		//lastN.add(cur.getTree());
		thisIsFirstTupleAfterLastWord();
		
		//this.completionGrammar=completionGrammar;
		
		
	}
	
	
	public DAG(DAGParser<T,E> p)
	{
		this(new Tree(), new ArrayList<UtteredWord>(), p);
	}
	
	
	public void setContext(Context<T,E> context)
	{
		this.context=context;
	}
	

	public List<E> getBacktrackedEdges()
	{
		return this.actionReplay;
	}
	
	public List<Action> getAsActionList(List<E> edges)
	{
		ArrayList<Action> result=new ArrayList<Action>();
		for(E edge:edges)
			result.addAll(edge.getActions());
		return result;
	}
	
	
	
	
	
	
	public T addAxiom()
	{
		return addAxiom(new ArrayList<Action>());
	}

	
	
	
	
	
	/**
	 * helper method. applies all actions in edge to t.
	 * 
	 * @param t
	 * @param edge
	 * @return the resulting tuple
	 */
	protected Tree applyActions(Tree t, List<Action> edge) {
		Tree clone = new Tree(t);

		for (Action a : edge) {
			clone = a.exec(clone, null);
			if (clone == null)
				return null;

		}
		return clone;
	}

	
	
		
	public boolean isRoot(T t)
	{
		return t==root;
	}

	public T getRoot() {
		return root;
	}

	public void init() {
		cur = getRoot();
		removeChildren();
		cur.setTree(new Tree());
		cur.setMaximalSemantics(null);
		wordStack.clear();
		exhausted = false;
		idPoolEdges.clear();
		idPoolNodes.clear();
		idPoolNodes.add(cur.id);
		//lastN.clear();
		//lastN.add(cur.getTree());
		thisIsFirstTupleAfterLastWord();
		actionReplay.clear();
		acceptance_pointers.clear();
	}

	

	public DAG(List<UtteredWord> words, DAGParser<T,E> p) {
		this(new Tree(), words, p);

	}

	

	/**
	 * Reset state to the first state immediately after last successful parse.
	 * 
	 */
	public abstract void resetToFirstTupleAfterLastWord();
		
	
	public E getParentWithId(T parent, long id)
	{
		for(E edge: getInEdges(parent))
		{
			if (edge.id==id)
				return edge;
		}
		return null;
	}
	
	public void markEdgeAsUnseenAndAboveItSeen(E seen)
	{
		SortedSet<E> edges=getOutEdges(getSource(seen), seen.pid);
		boolean found=false;
		for(E edge:edges)
		{
			if(edge==seen)
			{
				edge.setSeen(false);
				if (!(edge instanceof BacktrackingEdge))
					edge.setInContext(true);
				
				found=true;
				continue;
			}
			
			if (!found)
			{
				edge.setSeen(true);
			}
			else
			{
				edge.setSeen(false);
				edge.setInContext(false);
			}
				
		}
		
	}
	/**
	 * returns the outgoing edges of cur that are compatible with parsing word.
	 * These are either edges associated with word, or edges with no word, e.g.
	 * computational action edges.
	 * 
	 * @param cur
	 * @param word
	 * @return see above.
	 */
	protected SortedSet<E> getOutEdges(T cur, long activeParentID) {

		TreeSet<E> result = new TreeSet<E>();
		for (E edge : getOutEdges(cur)) {
			if (edge.pid==activeParentID)
				result.add(edge);
		}
		return result;
	}
	

	public boolean isComplete() {
		return getCurrentTuple().isComplete();
	}

	public void thisIsFirstTupleAfterLastWord() {
		this.firstTupleAfterLastWord = this.cur;
	}

	public Stack<UtteredWord> wordStack() {
		return this.wordStack;
	}

	public int getChildCount() {
		return getSuccessorCount(cur);
	}

	public T getActiveParent(T child)
	{
		E edge=getActiveParentEdge(child);
		if (edge==null)
			return null;
		
		
		return getSource(edge);
	}
	
	protected List<E> getActiveInEdges(T child) {
		List<E> result=new ArrayList<E>();
		for(E edge: getInEdges(child))
		{
			if (!edge.hasBeenSeen())
				result.add(edge);
		}
				
		
		return result;
	}
	

	
	public boolean removeChild(T child)
	{
		if (!containsVertex(child))
	        return false;
		
		if (getChildCount(child)==0)
			return removeVertex(child);
		
		
		for(T v : getChildren(child)) {
			
            removeChild(v);
		}
		
		return removeVertex(child);
	}

	protected E getMostRecentParentEdgeNotIn(T node, Set<E> seen)
	{
		
		List<E> incoming = new ArrayList<E>(getInEdges(node));
		if (incoming.isEmpty())
		{
			
			return null;
		}
		Collections.sort(incoming, new Comparator<E>() {

			@Override
			public int compare(E e1, E e2) {
				if (e1.incidenceNumber > e2.incidenceNumber)
					return -1;
				else
					return 1;
			}

		});
		
		for(E edge: incoming)
			if (!seen.contains(edge))
			{
				return edge;
			}
		return null;
		
	}
	
	public T getActiveParent() {
		return getActiveParent(cur);

	}

	public abstract DAGTuple execAction(Action a, UtteredWord w);

	public void addChild(T child, E edge) {
		
		addChild(cur, child, edge);
	}
	
	protected long getActiveParentEdgeId(T cur) {
		E active=getActiveParentEdge(cur);
		return active==null?-1:active.id;
	}

	protected long getActiveParentEdgeId() {
		return getActiveParentEdgeId(cur);
	}

	public void addChild(T from, T to, E edge)
	{
		if (!this.containsVertex(from))
			throw new IllegalArgumentException("Cannot add child. The parent doesn't exist");
		
		if (this.containsVertex(to))
		{
			 
			edge.incidenceNumber=inDegree(to);
			
		}
		else
			to.setDepth(from.getDepth() + 1);
		//System.out.println(this.getActiveParent(from));
		if (this.getActiveParent(from)==null)
			edge.setParentEdgeId(-1L);
		else
			edge.setParentEdgeId(this.getActiveParentEdge(from).id);
		
		addEdge(edge, from, to);
		
	}

	public long getDepth() {
		return cur.getDepth();
	}
//-----------------------------------------------------------------------------
	/**
	 * How many tuples to look back when checking for infinite branches. and
	 * associated list and methods below it...
	 */
	
	int lastNCapacity = 5;
	boolean loopDetection_enabled=false;
	protected List<Tree> lastN = new ArrayList<Tree>();

	protected boolean loopDetected(Tree res) {

		return lastN.contains(res);
	}

	protected void updateLastN()
	{
		lastN.clear();
		T current=cur;
		int n=0;
		while(current!=null && n<lastNCapacity)
		{
			lastN.add(current.getTree());
			current=getParent(current);
			n++;
		}
	}
	
	public void resetLastN() {
		T current = cur;
		this.lastN.clear();
		int i = 0;
		while (!isRoot(current) && i < lastNCapacity) {
			this.lastN.add(0, current.getTree());
			i++;
			current = getParent(current);
		}

	}

	
	// ------------------End of LastN Methods
	
	public SortedSet<E> getOutEdges(T node)
	{
		return new TreeSet<E>(super.getOutEdges(node));
	}
	
	
	public E goFirst() {
		if (outDegree(cur) == 0)
			return null;
		SortedSet<E> edges = getOutEdges(cur);

		for (E e : edges) {
			T child = getDest(e);

			if (!e.hasBeenSeen()) {
				logger.info("Going forward (first) along: " + e);
				if (e instanceof BacktrackingEdge<?>)
				{
					BacktrackingEdge<E> backEdge=(BacktrackingEdge)e;
					
					backEdge.markRepairedEdges();
					this.actionReplay.addAll(backEdge.getReplayableBacktrackedEdges());
				}
				else
					e.setInContext(true);
				
				
				this.cur = child;
				
				updateLastN();

				logger.info("depth is now:" + getDepth());

				if (e.word() != null) {
					if (e.word().equals(this.wordStack().peek()))
						this.wordStack.pop();
					else {
						logger.error("Trying to pop word off word stack when going along "
								+ e);
						logger.error("but word on stack is:"
								+ this.wordStack().peek());
					}

				}
				return e;
			}
		}
		return null;
	}
	
	
	

	/** Simulates DAG behaviour ignoring {@link BacktrackingEdge}s.
	 *  Assumption: each node has one unique incoming edge that is not a BacktrackingEdge. Nodes may have 1 or more incoming
	 *  BacktrackingEdge's, and they will be ordered by their incidence number.
	 * @param node
	 * @return parent edge of node
	 */
	
	public E getParentEdge(T node)
	{
		for(E edge: getInEdges(node))
		{
			if (!(edge instanceof BacktrackingEdge))
				return edge;
		}
		return null;
	}
	
	/** Simulates DAG behaviour. Ignores {@link BacktrackingEdge}s.
	 *  Assumption: each node has one unique parent, and, at most one incoming Backtracking edge.
	 * @param node
	 * @return parent of node
	 */
	
	public T getParent(T node)
	{
		E edge=getParentEdge(node);
		if (edge==null)
			return null;
		return getSource(edge);
	}
	
	public T getParent()
	{
		return getParent(cur);
	}
	
	public E getParentEdge()
	{
		return getParentEdge(cur);
	}
	
	
	public E getActiveParentEdge(T node) {
		
		List<E> incomingNonBacktracked = getActiveInEdges(node);
		if (incomingNonBacktracked.isEmpty())
		{
			//System.out.println("incoming nonbacktracking is empty");
			return null;
		}
		Collections.sort(incomingNonBacktracked, new Comparator<E>() {

			@Override
			public int compare(E e1, E e2) {
				if (e1.incidenceNumber > e2.incidenceNumber)
					return -1;
				else
					return 1;
			}

		});
		
		return incomingNonBacktracked.get(0);
		
	}

	/**
	 * 
	 * @return the edge we're going back over.
	 */
	public E goUpOnce() {
		

		E result = getActiveParentEdge();
		if (result==null)
		{
			logger.debug("At root, can't go up.");
			logger.debug("here:"+cur);
			return null;
		}
		logger.info("going back along: " + result);
		logger.info("depth was:" + getDepth());
		
		this.cur = getActiveParent();
		updateLastN();
		logger.info("depth is now:" + getDepth());
		return result;
	}

	public E getActiveParentEdge() {
		return getActiveParentEdge(cur);
	}

	public boolean isExhausted() {
		return this.exhausted;

	}

	public T getCurrentTuple() {

		return cur;
	}

	public Action getPrevAction() {
		if (getActiveParentEdge()==null)
			return null;
		
		return getActiveParentEdge().getAction();
	}

	public UtteredWord getPrevWord() {
		if (getActiveParentEdge()==null)
			return null;
		return getActiveParentEdge().word();
	}

	public ArrayList<Action> getActionSequence(T current) {

		ArrayList<Action> result = new ArrayList<Action>();
		if (getParentEdge(current)==null) {

			return result;
		}
		// T current = this.cur;
		E parentE;
		do {
			parentE = getParentEdge(current);
			result.add(0, parentE.getAction());
			current = getParent(current);
		} while (current!=null);

		return result;
	}

	public ArrayList<Action> getActionSequence() {
		return getActionSequence(this.cur);
	}

	public List<E> getSequenceToRoot() {
		List<E> result = new ArrayList<E>();
		if (getParent()==null)
			return result;
		T current = this.cur;
		E parentE;
		do {
			parentE = getParentEdge(current);
			result.add(0, parentE);
			current = getParent(current);
		} while (current!=null);

		return result;

	}

	public SortedSet<E> getOutEdges() {
		return getOutEdges(cur);
	}

	public boolean moreUnseenEdges() {
		for (DAGEdge edge : this.getOutEdges()) {
			if (!edge.hasBeenSeen())
			{
				logger.info("found unseen edge:"+edge);
				return true;
			}
		}
		logger.info("didn't find unseen edge");
		return false;
	}

	public void markEdgeAsSeenAndBelowItUnseen(E seenEdge) {
		logger.debug("trying to mark "+seenEdge + " as seen");
		logger.debug("all out edges:"+this.getOutEdges());
		boolean done = false;
		for (E outEdge : this.getOutEdges()) {
			if (done) {
				outEdge.setSeen(false);
				continue;
			}
			if (outEdge == seenEdge) {
				logger.debug("marked "+outEdge+" as seen");
				outEdge.setSeen(true);
				done = true;
			}

		}

	}

	public List<Action> getParentActions(T current) {

		if (getActiveParentEdge(current) == null)
			return null;
		return getActiveParentEdge(current).getActions();
	}

	public static void main(String a[]) {

	}

	public UtteredWord getPrevWord(T current) {
		if (getActiveParentEdge(current)!=null)
			return null;
		return getActiveParentEdge(current).word();
	}

	public void setExhausted(boolean a) {
		this.exhausted = a;

	}

	public void setCurrentTuple(T result) {
		this.cur = result;

	}

	public T getFirstTupleAfterLastWord() {

		return this.firstTupleAfterLastWord;
	}

	public void removeChildren() {
		removeChildren(cur);

	}

	public void removeChildren(T current) {
		logger.debug("removing children of:" + current);
		logger.debug("depth:" + getDepth());
		for (T t : getChildren(cur)) {
			removeChild(t);
			logger.trace("removed:" + t);
		}

	}

	public boolean canBacktrack()
	{
		if (getActiveParent()==null)
			return false;
		E parent=getActiveParentEdge();
		if (parent instanceof NewClauseEdge)
		{
			NewClauseEdge cl=(NewClauseEdge)parent;
			if (cl.isGrounded())
				return false;
			
		}
		else if (parent instanceof GroundingEdge)
			return false;
		
		
		return true;
	}

	
	
	/**
	 * 
	 * @return true if successful, false if we're at root without any more
	 *         exploration possibilities
	 */
	public boolean attemptBacktrack() {

		
		while (!moreUnseenEdges()) {
			if (!canBacktrack())
			{
				return false;
			}
				
			E backover = getActiveParentEdge();
			
			if (backover.word() != null) {

				wordStack.push(backover.word());
				logger.debug("adding word to stack, now:" + wordStack);
				E prevPrevEdge=getParentEdge(getSource(backover));
				if (prevPrevEdge!=null&&!backover.word().speaker().equals(prevPrevEdge.word().speaker()))
				{
					ungroundToClauseRootFor(backover.word().speaker(),getSource(backover));
				}
			}
			if (backover instanceof BacktrackingEdge<?>)
			{
				BacktrackingEdge<E> backEdge=(BacktrackingEdge)backover;
				backEdge.unmarkRepairedEdges();
				actionReplay.clear();
				//ungroundToRootFor(backover.word().speaker(), cur);
				logger.debug("going back (forward) over backtrakcing edge");
				logger.debug(":"+backEdge);
			}else if (backover instanceof ActionReplayEdge)
			{
				((ActionReplayEdge)backover).ungroundReplayedEdgesFor(backover.word().speaker());
				
			}
			E backOver = goUpOnce();
			//backOver.setBacktracked(true);
			// remove edge from context
			// context.goUpDelete();
			// mark edge that we're back over as seen (already explored)...
			
			//markOutEdgeAsSeen(backOver);
			backOver.setSeen(true);
			backOver.setInContext(false);
			

		}
		logger.debug("Backtrack succeeded");
		return true;
	}
	
	public boolean isBranching(T t)
	{
		logger.debug("checking branching:"+t);
		Collection<E> out=getOutEdges(t);
		boolean seenOne=false;
		for(E edge:out)
		{
			logger.debug("outedge:"+edge+" in context:"+edge.inContext());
			if (edge.inContext() && seenOne)
				return true;
			if (edge.inContext())
				seenOne=true;
		}
		return false;
	}
	
	@Override
	public int getChildCount(T t) {
		
		return getChildren(t).size();
	}

	@Override
	public Collection<E> getChildEdges(T t) {
		Collection<E> out=new TreeSet<E>();
		for(E edge: getOutEdges(t))
		{
			if (!(edge instanceof BacktrackingEdge))
			{
				out.add(edge);
				
			}
		}
		return out;
	}

	@Override
	public Collection<T> getChildren(T t) {
		Collection<T> out=new TreeSet<T>();
		for(E edge: getOutEdges(t))
		{
			if (!(edge instanceof BacktrackingEdge))
			{
				out.add(getDest(edge));
				
			}
		}
		return out;
	
		
	}

	@Override
	public Collection<edu.uci.ics.jung.graph.Tree<T, E>> getTrees() {
		
		edu.uci.ics.jung.graph.DelegateTree<T, E> tree=new DelegateTree<T, E>();
		tree.addVertex(root);
		addOutEdgesDeep(tree, root);
		Collection<edu.uci.ics.jung.graph.Tree<T, E>> trees=new HashSet<edu.uci.ics.jung.graph.Tree<T, E>>();
		trees.add(tree);
		return trees;
		
	}
	
	private void addOutEdgesDeep(DelegateTree<T,E> tree, T v)
	{
		if (getChildCount(v)==0)
			return;
		
		for(E edge:getChildEdges(v))
		{
			tree.addEdge(edge, v, getDest(edge));
			addOutEdgesDeep(tree, getDest(edge));
		}
		
		
	}
	
	private void addInContextEdges(DelegateTree<T,E> tree, T v)
	{
		if (getChildCount(v)==0)
			return;
		
		logger.trace("adding child edges of:"+v);
		for(E edge:getChildEdges(v))
		{
			
			if (edge.inContext())
			{
				logger.trace("Adding edge:"+edge);
				tree.addEdge(edge, v, getDest(edge));
				addInContextEdges(tree, getDest(edge));
			}
		}
		
	}
	
	public edu.uci.ics.jung.graph.Tree<T, E> getInContextSubgraph()
	{
		
		edu.uci.ics.jung.graph.DelegateTree<T, E> tree=new DelegateTree<T, E>();
		tree.addVertex(root);
		addInContextEdges(tree, root);
		
		
		
		return tree;
		
	}
	
	
	/** WARNING: only works with DAGs whose edges are associated with single DS actions - not action sequences -  e.g. it works with
	 * {@link DAGState}, and not {@link WordLevelContextDAG}
	 * 
	 * @param current
	 * @return the action associated with the parent edge of current
	 */
	
	public Action getParentAction(T current) {

		if (getParentEdge(current) == null)
			return null;
		return getParentEdge(current).getAction();
	}
	
	
	public Set<String> getAcceptancePointers(T tuple)
	{
		Set<String> pointers = new HashSet<String>();
		for (String spkr: this.acceptance_pointers.keySet()){
			if (this.acceptance_pointers.get(spkr).contains(tuple))
				pointers.add(spkr);
		}
		return pointers;
		
	}
	
	

	public String getTupleLabel(T tuple)
	{
		//by default just the id. subclassses should override this.
		
		return tuple.transform();
	}
	
	/**
	 * grounds all edges back to root, from cur for speaker
	 * currently only WordLevelDAGState implements this method
	 * @param speaker
	 */
	public abstract void groundToClauseRootFor(String speaker, T cur);
	

	public void groundToRootFor(String speaker)
	{
		groundToClauseRootFor(speaker,getCurrentTuple());
	}
	
	

	
	public abstract void ungroundToClauseRootFor(String speaker, DAGTuple cur);

	public boolean isClauseRoot(T current) {
		
		return current.getTree().isAxiom();
	}

	
	public abstract T addAxiom(List<Action> list);
	

	public qmul.ds.tree.Tree complete() {
		return parser.complete();
	}
	
	public DAGParser<T,E> getParser()
	{
		return parser;
	}

	
	
}
