package qmul.ds.dag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import qmul.ds.DAGGenerator;
import qmul.ds.action.Action;
import qmul.ds.tree.Tree;

/**
 * This is a context DAG as per Eshghi et. al. (2015), where edges are instances
 * of {@link GrounableEdge}, sequences of computational actions followed by a
 * single lexical action - so each edge corresponds to a word. Nodes contain
 * trees (and semantics) as usual.
 * 
 * There are special edges: BacktrackingEdge, NewClauseEdge, and
 * ActionReplayEdge that only this type of DAG properly supports.
 * 
 * @author Arash
 *
 */
public class WordLevelContextDAG extends DAG<DAGTuple, GroundableEdge> {

	/**
	 * 
	 * 
	 * 
	 */
	protected static Logger logger = Logger.getLogger(WordLevelContextDAG.class);
	private static final long serialVersionUID = -8765365147853341079L;

	// protected Map<String, Set<DAGTuple>> acceptance_pointers = new
	// HashMap<String, Set<DAGTuple>>();

	public WordLevelContextDAG() {
		super();
	}

	public WordLevelContextDAG(Tree start) {
		super(start);
	}

	public boolean removeChild(DAGTuple child) {
		if (!containsVertex(child)) {
			logger.warn("asked to remove child, but graph doesn't contain it:" + child);
			return false;
		}
		logger.trace("removing child recursively:" + child);
		logger.trace("Depth:" + child.getDepth());
		if (getOutEdgesForTraversal(child).size() == 0) {
			logger.trace("removing single vertex: " + child);
			return removeVertex(child);
		}

		for (GroundableEdge outEdge : getOutEdgesForTraversal(child)) {

			if (outEdge instanceof VirtualRepairingEdge) {
				VirtualRepairingEdge e = (VirtualRepairingEdge) outEdge;
				this.removeEdge(e.edgePair.first);
				this.removeChild(getDest(e.edgePair.second));
			} else
				this.removeChild(getDest(outEdge));
		}

		logger.trace("removing single vertex: " + child);
		return removeVertex(child);
	}

	public void removeChildren(DAGTuple current) {
		logger.trace("removing children of:" + current);

		logger.trace("depth:" + getDepth());
		for (GroundableEdge outEdge : getOutEdgesForTraversal(current)) {

			if (outEdge instanceof VirtualRepairingEdge) {
				VirtualRepairingEdge e = (VirtualRepairingEdge) outEdge;
				this.removeEdge(e.edgePair.first);
				this.removeChild(getDest(e.edgePair.second));
			} else
				this.removeChild(getDest(outEdge));
		}

	}

	/**
	 * Helper method. Recursively (best-first) traverses the N first edges (ordered) forward.
	 * @param N
	 * @return
	 */
	private DAGTuple goFirstN(DAGTuple cur,int N)
	{
		
		if (N==0)
			return cur;
		
		SortedSet<GroundableEdge> edges = getOutEdgesForTraversal(cur);
		logger.info("edges:" + edges);

		for (GroundableEdge e : edges) {

			
			DAGTuple dest=this.getDest(e);
			
			logger.info("now on" + cur);
			
			//logger.info("Depth is now:" + getDepth());
			DAGTuple target=goFirstN(dest,N-1);
			if (target!=null)
				return target;
				
			

		}
		
		return null;	
	}
	
	public boolean rollBack(int n)
	{
		logger.debug("Rolling Back "+n);
		if (n<0)
		{
			logger.error("n<0");
			return false;
		}
		
		if (isExhausted())
			resetToFirstTupleAfterLastWord();
		
		
		
		
		List<UtteredWord> result=new ArrayList<UtteredWord>();
		DAGTuple clauseRoot=getCurrentClauseRoot(result);
		
		int clauseLength = result.size();
		logger.debug("Rolling back:"+result);
		if (n>clauseLength)
		{
			logger.error("Cannot roll back "+ n + "steps. Reached clause root");
			return false;
		}
		
		DAGTuple nth=goFirstN(clauseRoot,clauseLength-n);
		
		
		this.firstTupleAfterLastWord=nth;
		this.resetToFirstTupleAfterLastWord();
		logger.debug("Rolled back dag");
		return true;
		
		
	}

	/**
	 * Moves DAG pointer to the root node of the current clause.
	 * @return the clause root
	 */
	protected DAGTuple getCurrentClauseRoot(List<UtteredWord> rolledBack)
	{
		if (this.atClauseRoot())
			return cur;
		
		
		GroundableEdge parentEdge = getUniqueParentEdge(cur);
		DAGTuple curSource=this.getSource(parentEdge);
		rolledBack.add(0, new UtteredWord(parentEdge.word()));
		
		while (!this.isClauseRoot(curSource)) {
			
			logger.debug("going over:" + parentEdge);

			parentEdge = getUniqueParentEdge(curSource);
			curSource=this.getSource(parentEdge);
			rolledBack.add(0, new UtteredWord(parentEdge.word()));

		}
		return curSource;
		
		
	}
	
	/**
	 * 
	 */
	public void resetToFirstTupleAfterLastWord() {
		// if (!this.isExhausted()) {
		// logger.error("can only reset to last word when the state is exhausted, and
		// wants to get ready for repairing");
		// return;
		// }

		this.setCurrentTuple(firstTupleAfterLastWord);
		this.wordStack.clear();
		removeChildren();
		if (atGroundedClauseRoot()) {
			setExhausted(false);
			return;

		}

		GroundableEdge parentEdge = getUniqueParentEdge();

		while (!atGroundedClauseRoot()) {
			setCurrentTuple(getUniqueParent());
			parentEdge.setSeen(false);
			parentEdge.setInContext(false);
			logger.debug("going up:" + parentEdge);

			parentEdge = getUniqueParentEdge();

		}

		while (goFirstReset() != null)
			;

		setExhausted(false);

	}

	@Override
	public DAGTuple execAction(Action a, UtteredWord w) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DAGTuple getNewTuple(Tree t) {
		long newID = idPoolNodes.size() + 1;
		DAGTuple result = new DAGTuple(t, newID);
		idPoolNodes.add(newID);
		return result;
	}

	@Override
	public GroundableEdge getNewEdge(List<Action> actions, UtteredWord word) {
		long newID = idPoolEdges.size() + 1;
		GroundableEdge result = new GroundableEdge(actions, word, newID);
		idPoolEdges.add(newID);
		return result;
	}

	@Override
	public BacktrackingEdge getNewBacktrackingEdge(List<GroundableEdge> backtrackedOver, String speaker) {

		long newID = idPoolEdges.size() + 1;
		BacktrackingEdge result = new BacktrackingEdge(backtrackedOver, speaker, newID);
		idPoolEdges.add(newID);
		return result;

	}

	protected SortedSet<GroundableEdge> getOutEdgesForTraversal() {
		return getOutEdgesForTraversal(cur);
	}

	protected SortedSet<GroundableEdge> getOutEdgesForTraversal(DAGTuple cur) {
		TreeSet<GroundableEdge> result = new TreeSet<GroundableEdge>(this.edgeComparator);
		for (GroundableEdge edge : super.getOutEdges(cur)) {
			logger.trace("out edges:" + edge);
			if (edge instanceof BacktrackingEdge)
				result.add(((BacktrackingEdge) edge).overarchingRepairingEdge);
			else if (edge instanceof RepairingWordEdge) {
				logger.error("out edge is a reparing word edge. This shouldn't happen. Edge:" + edge);
			} else
				result.add(edge);
		}
		return result;
	}

	public GroundableEdge goFirstReset() {
		if (outDegree(cur) == 0) {
			logger.debug("out degree of cur is 0");
			return null;
		}
		
		// if (wordStack.isEmpty()) {
		// logger.debug("GoFirst: wordstack is empty");
		// return null;
		// }
		SortedSet<GroundableEdge> edges = getOutEdgesForTraversal();
		logger.debug("edges:" + edges);

		for (GroundableEdge e : edges) {

			if (e.hasBeenSeen())
				continue;
			markAllEdgesBelowAsUnseen(e);
			markAllEdgesBelowAsNotInContext(e);
			logger.debug("Going forward first reset along: " + e);
			e.traverse(this);
			logger.debug("now on" + getCurrentTuple());

			logger.debug("Depth is now:" + getDepth());
			return e;

		}
		return null;
	}

	/**
	 * TODO:
	 * 
	 */
	public GroundableEdge goFirst() {
		if (outDegree(cur) == 0) {
			logger.debug("out degree of cur is 0");
			return null;
		}

		SortedSet<GroundableEdge> edges = getOutEdgesForTraversal();

		for (GroundableEdge e : edges) {

			if (e.hasBeenSeen()) {

				continue;
			}
			
			
			logger.info("Going forward first along: " + e.toDebugString());
			//logger.debug("Going forward first along: " + e.toDebugString());
			
			if (e instanceof VirtualRepairingEdge)
			{
				//for virtual repairing edges do nothing as it is the traverse method that will pop words off of stack
			}
			else if (!(e instanceof CompletionEdge) && !wordStack.isEmpty() && wordStack.peek().equals(e.word()))
				wordStack().pop();
			else if (!(e instanceof CompletionEdge)&&!wordStack.isEmpty()) {
				logger.error("Trying to pop word " + wordStack.peek() + " off the stack when going along " + e);
				throw new IllegalStateException("top of stack is not the same as word on this edge, or stack is empty");
			}
			
			e.traverse(this);
			markAllOutEdgesAsUnseen();
			logger.info("Depth is now:" + getDepth());
			return e;

		}

		return null;
	}

	private void markAllOutEdgesAsUnseen() {
		logger.trace("out edges of cur" + getOutEdges(cur));
		for (GroundableEdge edge : getOutEdges(cur)) {
			edge.setSeen(false);
		}

	}

	public void markAllEdgesBelowAsUnseen(GroundableEdge seenEdge) {

		boolean done = false;
		for (DAGEdge outEdge : this.getOutEdges(cur)) {

			if (done) {
				outEdge.setSeen(false);
				continue;
			}
			if (outEdge == seenEdge) {
				done = true;
			}

		}

	}

	public void markAllEdgesBelowAsNotInContext(GroundableEdge seenEdge) {

		boolean done = false;
		for (DAGEdge outEdge : this.getOutEdges(cur)) {

			if (done) {
				outEdge.setInContext(false);
				continue;
			}
			if (outEdge == seenEdge) {
				done = true;
			}

		}

	}

	/**
	 * the difference from overridden method is that this operates for the word at
	 * the top of the stack.
	 */
	public boolean moreUnseenEdges() {
		logger.debug("looking for unseen edge at:" + cur);
		SortedSet<GroundableEdge> edges = this.getOutEdges(cur);
		logger.debug("out edges:" + edges);
		for (DAGEdge edge : edges) {
			if (!edge.hasBeenSeen()) {
				logger.debug("found unseen edge:" + edge);

				return true;
			}
		}
		logger.debug("no unseen edge");
		return false;
	}

	public String getTupleLabel(DAGTuple t) {
		Set<String> pointers = getAcceptancePointers(t);

		String result = "";
		if (pointers.isEmpty())
			return result;

		for (String participant : pointers) {
			result += participant.substring(0, 1) + "-";
		}
		return result.substring(0, result.length() - 1);

	}

	public void groundToClauseRootFor(String speaker, DAGTuple cur) {
		GroundableEdge parent = getParentEdge(cur);
		while (parent != null && !parent.initiatesNewClause()) {
			logger.debug("grounding" + parent + " for " + speaker);
			parent.groundFor(speaker);
			DAGTuple parentNode = getSource(parent);
			parent = getParentEdge(parentNode);
		}
		if (parent != null && parent.initiatesNewClause()) {
			logger.debug("grounding clause root:"+parent);
			parent.ground();
			parent.groundFor(speaker);
		}

	}

	public void ungroundToClauseRootFor(String speaker, DAGTuple cur) {
		GroundableEdge parent = getParentEdge(cur);
		while (parent != null && !parent.initiatesNewClause()) {
			// logger.debug("ungrounding"+ parent + " for "+speaker);
			parent.ungroundFor(speaker);
			DAGTuple parentNode = getSource(parent);
			parent = getParentEdge(parentNode);
		}

		if (parent != null && parent.initiatesNewClause()) {
			parent.ungroundFor(speaker);

		}

	}

	public DAGTuple addAxiom(List<Action> actions, UtteredWord word) {
		DAGTuple axiom = this.getNewTuple(new Tree());
		GroundableEdge edge = this.getNewEdge(actions, word);
		this.addChild(axiom, edge);
		return axiom;

	}

	public DAGTuple addChild(DAGTuple from, DAGTuple to, GroundableEdge edge) {
		if (!(edge instanceof VirtualRepairingEdge))
			return super.addChild(from, to, edge);

		VirtualRepairingEdge redge = (VirtualRepairingEdge) edge;

		if (!this.containsVertex(from))
			throw new IllegalArgumentException("Cannot add child. The parent doesn't exist");

		if (this.containsVertex(to))
			throw new IllegalArgumentException("Adding already existing child");

		super.addChild(from, redge.getMidTuple(), redge.getBacktrackingEdge());
		super.addChild(redge.getMidTuple(), to, redge.getWordEdge());
		return to;

	}
	
	@Override
	public Collection<DAGTuple> getChildren(DAGTuple t) {
		Collection<DAGTuple> out = new TreeSet<DAGTuple>();
		for (GroundableEdge edge : getOutEdges(t)) {
			if (!(edge instanceof BacktrackingEdge)) {
				out.add(getDest(edge));

			}
		}
		return out;

	}

	public DAGTuple getUniqueParent() {
		return getUniqueParent(cur);
	}

	public DAGTuple getUniqueParent(DAGTuple cur) {
		GroundableEdge parent = getUniqueParentEdge(cur);
		if (parent == null)
			return null;

		if (parent instanceof VirtualRepairingEdge) {
			return getSource(((VirtualRepairingEdge) parent).getBacktrackingEdge());
		}

		return getSource(parent);

	}

	public GroundableEdge getUniqueParentEdge() {
		return getUniqueParentEdge(cur);
	}

	/**
	 * like getActiveParentEdge, except it is irrespective of whether the parent is
	 * seen or not.
	 * 
	 * @param cur
	 * @return
	 */
	public GroundableEdge getUniqueParentEdge(DAGTuple cur) {

		for (GroundableEdge edge : getInEdges(cur)) {

			if (edge instanceof RepairingWordEdge) {
				logger.info("returning overarching edge" + ((RepairingWordEdge) edge).overarchingRepairingEdge);
				return ((RepairingWordEdge) edge).overarchingRepairingEdge;
			} else if (edge instanceof BacktrackingEdge) {
				logger.error("This shouldn't really happen");
				logger.error("Backtracking edge:" + edge);
				logger.error("parent of:" + cur);

			} else
				return edge;
		}
		return null;
	}

	public GroundableEdge getActiveParentEdge(DAGTuple cur) {

		for (GroundableEdge edge : getInEdges(cur)) {
			if (edge.hasBeenSeen()) {
				logger.debug("edge " + edge + " has been seen");
				continue;
			}

			if (edge instanceof RepairingWordEdge) {
				logger.debug("returning overarching edge: " + ((RepairingWordEdge) edge).overarchingRepairingEdge);
				return ((RepairingWordEdge) edge).overarchingRepairingEdge;
			} else if (edge instanceof BacktrackingEdge) {
				logger.error("This shouldn't really happen");
				logger.error("Backtracking edge:" + edge);
				logger.error("parent of:" + cur);

			} else
				return edge;
		}
		return null;
	}

	public GroundableEdge getActiveParentEdge() {
		return getActiveParentEdge(cur);

	}

	public boolean canBacktrack() {
		GroundableEdge parent = getActiveParentEdge();
		if (parent == null) {
			logger.debug("no active parent");
			logger.debug("in edges:" + getInEdges(cur));

			return false;
		}

		if (this.wordStack().isEmpty())
			return true;

		// if (parent.isGroundeFor(this.wordStack().peek().speaker()))
		// return false;
		if (parent.isGroundedFor(this.context.getParticipants())) {
			return false;
		} else {
			logger.trace("can backtrack over:" + parent);
			logger.trace("participants:" + this.context.getParticipants());
			logger.trace("grounded for:" + parent.grounded_for);
		}

		if (atGroundedClauseRoot()) {
			logger.debug("Cannot backtrack. At grounded clause root.");
			return false;

		}
		return true;
	}

	public DAGTuple getActiveParent(DAGTuple cur) {
		GroundableEdge activeParent = getActiveParentEdge(cur);
		if (activeParent == null)
			return null;

		if (activeParent instanceof VirtualRepairingEdge) {
			return getSource(((VirtualRepairingEdge) activeParent).getBacktrackingEdge());
		}

		return getSource(activeParent);
	}

	public DAGTuple getActiveParent() {
		return getActiveParent(cur);
	}

	/**
	 * 
	 * @return true if successful, false if we're at root without any more
	 *         exploration possibilities
	 */
	public boolean attemptBacktrack() {

		while (!moreUnseenEdges()) {
			logger.info("Attempting to backtrack. . .");
			if (!canBacktrack()) {
				logger.debug("Cannot backtrack: at grounded root");
				return false;
			}

			GroundableEdge backover = getActiveParentEdge();
			if (backover.word() != null) {
//				if (backover.word().speaker().equals(DAGGenerator.agentName)) {
//					//AE@1 March 2023: I do not get this! why did I do this?
//					//Why would I want to pop a word if I'm generating?!!!
//					//Commenting this out for now
//					if (!wordStack.peek().equals(backover.word()))
//						throw new IllegalStateException(
//								"top of stack is " + wordStack.peek() + " but edge is " + backover);
//
//					UtteredWord word = wordStack.pop();
//					logger.info("Backtrack: popped word off stack:" + word);
//					logger.debug("Backtrack: stack now:" + wordStack);
//				} else {
				wordStack.push(backover.word());
				logger.info("Backtrack: adding word to stack:" + backover.word());
				logger.debug("Backtracked over:+ " + backover + "|stack now:" + wordStack);
				//}

			}

			backover.backtrack(this);

		}
		logger.info("Backtrack succeeded");
		return true;
	}

	public VirtualRepairingEdge getNewRepairingEdge(List<GroundableEdge> backtrackedOver, List<Action> repairingActions,
			DAGTuple midTuple, UtteredWord repairingWord) {
		BacktrackingEdge newBackEdge = getNewBacktrackingEdge(backtrackedOver, repairingWord.speaker());
		GroundableEdge repairingWordEdge;
		VirtualRepairingEdge repairingEdge;
		long newID = idPoolEdges.size() + 1;

		repairingWordEdge = getNewRepairingWordEdge(repairingActions, repairingWord);
		repairingEdge = new VirtualRepairingEdge(newBackEdge, repairingWordEdge, midTuple, newID,
				backtrackedOver.size());
		newBackEdge.overarchingRepairingEdge = repairingEdge;
		((RepairingWordEdge) repairingWordEdge).overarchingRepairingEdge = repairingEdge;

		return repairingEdge;

	}

	public GroundableEdge getParentEdge(DAGTuple node) {
		Collection<GroundableEdge> edges = getInEdges(node);
		if (edges.size() == 0)
			return null;

		for (GroundableEdge edge : edges) {
			if (!(edge instanceof BacktrackingEdge))
				return edge;
		}
		return null;
	}

	@Override
	public RepairingWordEdge getNewRepairingWordEdge(List<Action> actions, UtteredWord word) {

		long newID = idPoolEdges.size() + 1;
		RepairingWordEdge result = new RepairingWordEdge(actions, word, newID);
		idPoolEdges.add(newID);
		return result;
	}

	public String getSpeakerOfPreviousWord() {

		if (atRoot())
			return null;
		GroundableEdge parent = getActiveParentEdge();
		if (parent == null)
			return null;

		return parent.word.speaker();

	}

	public DAGTuple getDest(GroundableEdge e) {

		if (e instanceof VirtualRepairingEdge) {
			VirtualRepairingEdge vr = (VirtualRepairingEdge) e;
			return super.getDest(vr.getWordEdge());
		}

		return super.getDest(e);
	}

	

}
