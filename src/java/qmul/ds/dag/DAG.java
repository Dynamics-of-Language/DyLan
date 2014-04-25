package qmul.ds.dag;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

import qmul.ds.ParserTuple;
import qmul.ds.action.Action;
import qmul.ds.action.LexicalAction;
import qmul.ds.learn.TreeHypothesis;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
/**
 *  A generic DS (parse) Directed Asyclic Graph. Edges correspond minimally to actions. Nodes to to trees. But these
 *	could be subclassed, e.g. to include contextual info like speaker/hearer, info about repaired edges, saliency, etc.
 * @author arash 
 * @param <T> Type of nodes in the dag
 * @param <E> Type of edges in the dag
 */
public abstract class DAG<T extends DAGTuple, E extends DAGEdge> extends OrderedDelegateTree<T, E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static Logger logger = Logger.getLogger(DAG.class);

	protected T cur;
	private Stack<String> wordStack;
	private T firstTupleAfterLastWord;
	private boolean exhausted = false;
	protected List<Long> idPoolNodes = new ArrayList<Long>();
	protected List<Long> idPoolEdges = new ArrayList<Long>();

	
	public DAG(T cur) {
		this();
		
		this.cur = cur;//DAGTuple.getNewTuple(idPoolNodes);
		addVertex(cur);
		lastN.add(cur.getTree());
		thisIsFirstTupleAfterLastWord();
	}
	
	public void init()
	{
		cur=getRoot();
		removeChildren();
		cur.setTree(new Tree());
		cur.setMaximalSemantics(null);
		wordStack.clear();
		exhausted=false;
		idPoolEdges.clear();
		idPoolNodes.clear();
		thisIsFirstTupleAfterLastWord();
	}
	
	public DAG()
	{
		super();
		wordStack = new Stack<String>();
		
	}
	public DAG(List<String> words)
	{
		this();
		for (int i = words.size() - 1; i >= 0; i--) {
			wordStack.push(words.get(i));
		}
	}
		
	public void thisIsFirstTupleAfterLastWord() {
		this.firstTupleAfterLastWord = this.cur;
	}
	
	public Stack<String> wordStack() {
		return this.wordStack;
	}

	public int getChildCount() {
		return getChildCount(cur);
	}

	public T getParent() {
		return getParent(cur);
	}
	public abstract T execAction(Action a, String w);
	
	
	protected boolean loopDetected(Tree res) {

		return lastN.contains(res);
	}

	public long getDepth()
	{
		return cur.getDepth();
	}
	/**
	 * How many tuples to look back when checking for infinite branches. and associated list and methods below it...
	 */
	int lastNCapacity = 5;
	protected List<Tree> lastN = new ArrayList<Tree>();

	private void backtrackLastN() {
		lastN.remove(lastN.get(lastN.size() - 1));
		if(getDepth()>=lastNCapacity)			
			lastN.add(0, getTreeBackN(lastNCapacity));
		

	}
	private Tree getTreeBackN(int n)
	{
		T current=this.cur;
		for(int i=0;i<n;i++)
		{
			current=getParent(current);
		}
		return current.getTree();
	}

	private void pushLastN() {
		lastN.add(cur.getTree());
		if (lastN.size() > lastNCapacity) {
			lastN.remove(0);
		}

	}

	public E goFirst() {
		if (getChildCount(cur) == 0)
			return null;
		List<E> edges = getOutEdges(cur);
		
		for (E e : edges) {
			T child = getDest(e);
			

			if (!e.hasBeenSeen()) {
				logger.debug("Going forward (first) along " + e.getAction().getName());
				this.cur = child;
				
				pushLastN();
				
				logger.debug("depth is now:" + getDepth());
				Action a = e.getAction();
				if (a instanceof LexicalAction) {
					this.wordStack.pop();
				}
				return e;
			}
		}
		return null;
	}

	public boolean atRoot() {
		return isRoot(cur);
	}

	public E getParentEdge() {
		return getParentEdge(cur);
	}

	/**
	 * 
	 * @return the edge we're going back over.
	 */
	public DAGEdge goUpOnce() {
		if (atRoot()) {
			logger.debug("At root, can't go up.");
			return null;
		}

		E result = getParentEdge();
		logger.info("going back along: " + result.getAction().getName());
		
		
		
		backtrackLastN();
		this.cur = getParent();
		
		logger.info("depth is now:" + getDepth());
		return result;
	}

	

	public boolean isExhausted() {
		return this.exhausted;

	}

	public T getCurrentTuple() {

		return cur;
	}

	public Action getPrevAction() {
		if (atRoot())
			return null;
		return getParentEdge().getAction();
	}

	public String getPrevWord() {
		if (atRoot())
			return null;
		return getParentEdge().word();
	}

	public ArrayList<Action> getActionSequence(T current) {

		ArrayList<Action> result = new ArrayList<Action>();
		if (isRoot(current))		{
			
			return result;
		}
		//T current = this.cur;
		E parentE;
		do {
			parentE = getParentEdge(current);
			result.add(0, parentE.getAction());
			current = getParent(current);
		} while (!isRoot(current));

		return result;
	}
	public ArrayList<Action> getActionSequence() {
		return getActionSequence(this.cur);
	}

	public List<E> getSequenceToRoot() {
		List<E> result = new ArrayList<E>();
		if (atRoot())
			return result;
		T current = this.cur;
		E parentE;
		do {
			parentE = getParentEdge(current);
			result.add(0, parentE);
			current = getParent(current);
		} while (!isRoot(current));

		return result;

	}

	public List<E> getOutEdges() {
		return getOutEdges(cur);
	}

	public boolean moreUnseenEdges() {
		for (DAGEdge edge : this.getOutEdges()) {
			if (!edge.hasBeenSeen())
				return true;
		}
		return false;
	}

	public void markOutEdgeAsSeen(E seenEdge) {
		boolean done = false;
		for (E outEdge : this.getOutEdges()) {
			if (done) {
				outEdge.setSeen(false);
				continue;
			}
			if (outEdge == seenEdge) {

				outEdge.setSeen(true);
				done = true;
			}

		}

	}

	public Action getParentAction(T current) {

		if (getParentEdge(current) == null)
			return null;
		return getParentEdge(current).getAction();
	}

	public static void main(String a[]) {

	}

	public String getPrevWord(T current) {
		if (isRoot(current))
			return null;
		return getParentEdge(current).word();
	}

	public void setExhausted(boolean a) {
		this.exhausted = a;

	}

	public void setCurrentTuple(T result) {
		this.cur = result;

	}

	public DAGTuple getFirstTupleAfterLastWord() {

		return this.firstTupleAfterLastWord;
	}

	public void removeChildren() {
		logger.debug("removing children of:"+cur);
		logger.debug("depth:"+getDepth());
		for (T t : getChildren(cur)) {
			removeChild(t);
			logger.trace("removing:"+t);
		}

	}

	public void resetLastN() {
		T current=cur;
		this.lastN.clear();
		int i=0;
		while(!isRoot(current) && i<lastNCapacity)
		{
			this.lastN.add(0, current.getTree());
			i++;
			current=getParent(current);
		}
		
	}

}
