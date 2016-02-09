package qmul.ds.dag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import qmul.ds.DAGParser;
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
	protected static Logger logger = Logger
			.getLogger(WordLevelContextDAG.class);
	private static final long serialVersionUID = -8765365147853341079L;

	// protected Map<String, Set<DAGTuple>> acceptance_pointers = new
	// HashMap<String, Set<DAGTuple>>();

	/**
	 * 
	 */
	public void resetToFirstTupleAfterLastWord() {
		if (!this.isExhausted())
		{
			logger.error("can only reset to last word when the state is exhausted, and wants to get ready for repairing");
			return;
		}
		
	
		
		while(goFirstReset()!=null);
		
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
	public BacktrackingEdge getNewBacktrackingEdge(
			List<GroundableEdge> backtrackedOver, String speaker) {

		long newID = idPoolEdges.size() + 1;
		BacktrackingEdge result = new BacktrackingEdge(backtrackedOver,
				speaker, newID);
		idPoolEdges.add(newID);
		return result;

	}

	protected SortedSet<GroundableEdge> getOutEdgesForTraversal()
	{
		return getOutEdgesForTraversal(cur);
	}
	
	protected SortedSet<GroundableEdge> getOutEdgesForTraversal(DAGTuple cur)
	{
		TreeSet<GroundableEdge> result = new TreeSet<GroundableEdge>();
		for (GroundableEdge edge : super.getOutEdges(cur)) {
			if (edge instanceof BacktrackingEdge)
				result.add(((BacktrackingEdge)edge).overarchingRepairingEdge);
			else if (edge instanceof RepairingWordEdge)
			{
				
			}else
				result.add(edge);
		}
		return result;
	}
	
	public GroundableEdge goFirstReset()
	{
		if (outDegree(cur) == 0) {
			logger.debug("out degree of cur is 0");
			return null;
		}
//		if (wordStack.isEmpty()) {
//			logger.debug("GoFirst: wordstack is empty");
//			return null;
//		}
		SortedSet<GroundableEdge> edges = getOutEdgesForTraversal();
		logger.info("edges:"+edges);

		for (GroundableEdge e : edges) {

			
			logger.info("Going forward first reset along: "+e);
			markAllOutEdgesAsUnseen();
			e.traverse(this);
			logger.info("now on"+getCurrentTuple());
			
			
			logger.info("Depth is now:"+getDepth());
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
//		if (wordStack.isEmpty()) {
//			logger.debug("GoFirst: wordstack is empty");
//			return null;
//		}
		SortedSet<GroundableEdge> edges = getOutEdgesForTraversal();

		for (GroundableEdge e : edges) {

			if (!e.hasBeenSeen()) {
				logger.info("Going forward first along: "+e);
				e.traverse(this);
				markAllOutEdgesAsUnseen();
				logger.info("Depth is now:"+getDepth());
				return e;
			}
		}
		return null;
	}

	

	private void markAllOutEdgesAsUnseen() {
		logger.trace("out edges of cur" + getOutEdges(cur));
		for (GroundableEdge edge : getOutEdges(cur)) {
			edge.setSeen(false);
		}

	}

	/**
	 * 
	 * @param seenEdge
	 */
	public void markEdgeAsSeenAndBelowItUnseen(GroundableEdge seenEdge) {
		// logger.debug("trying to mark "+seenEdge + " as seen");
		// logger.debug("all out edges:"+this.getOutEdges());
		// logger.debug("top of stack:"+wordStack.peek());
		boolean done = false;
		for (DAGEdge outEdge : this.getOutEdges(cur)) {
			// if (!seenEdge.word().equals(wordStack.peek()))
			// continue;
			if (done) {
				outEdge.setSeen(false);
				continue;
			}
			if (outEdge == seenEdge) {
				logger.debug("marked " + outEdge + " as seen");
				outEdge.setSeen(true);
				done = true;
			}

		}

	}

	/**
	 * the difference from overridden method is that this operates for the word
	 * at the top of the stack.
	 */
	public boolean moreUnseenEdges() {
		logger.trace("looking for unseen edge at:" + cur);
		SortedSet<GroundableEdge> edges = this.getOutEdges(cur);
		logger.trace("out edges:" + edges);
		for (DAGEdge edge : edges) {
			if (!edge.hasBeenSeen()) {
				logger.trace("found unseen edge:" + edge);

				return true;
			}
		}
		logger.trace("no unseen edge");
		return false;
	}

	

	public String getTupleLabel(DAGTuple t) {
		Set<String> pointers = new HashSet<String>();
		for (String spkr : this.acceptance_pointers.keySet()) {
			if (this.acceptance_pointers.get(spkr).contains(t))
				pointers.add(spkr);
		}
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
		while (parent != null && !(parent instanceof NewClauseEdge)) {
			logger.debug("grounding" + parent + " for " + speaker);
			parent.groundFor(speaker);
			DAGTuple parentNode = getSource(parent);
			parent = getParentEdge(parentNode);
		}
		if (parent != null && parent instanceof NewClauseEdge) {
			((NewClauseEdge) parent).ground();
			parent.groundFor(speaker);
		}

	}

	public void ungroundToClauseRootFor(String speaker, DAGTuple cur) {
		GroundableEdge parent = getParentEdge(cur);
		while (parent != null && !(parent instanceof NewClauseEdge)) {
			// logger.debug("ungrounding"+ parent + " for "+speaker);
			parent.ungroundFor(speaker);
			DAGTuple parentNode = getSource(parent);
			parent = getParentEdge(parentNode);
		}
		
		if (parent!=null&&parent instanceof NewClauseEdge)
		{
			parent.ungroundFor(speaker);
			
		}

	}

	public DAGTuple addAxiom(List<Action> actions, UtteredWord word) {
		DAGTuple axiom = this.getNewTuple(new Tree());
		NewClauseEdge edge = this.getNewNewClauseEdge(actions, word);
		this.addChild(axiom, edge);
		return axiom;

	}
	
	public DAGTuple addChild(DAGTuple from, DAGTuple to, GroundableEdge edge)
	{
		if (!(edge instanceof VirtualRepairingEdge))
			return super.addChild(from, to, edge);
		
		VirtualRepairingEdge redge=(VirtualRepairingEdge)edge;
		
		if (!this.containsVertex(from))
			throw new IllegalArgumentException("Cannot add child. The parent doesn't exist");
		
		if (this.containsVertex(to))
			throw new IllegalArgumentException("Adding already existing child");
		
		
		super.addChild(from, redge.getMidTuple(),redge.getBacktrackingEdge());
		super.addChild(redge.getMidTuple(), to, redge.getWordEdge());
		return to;
		
		
		
	}

	
	
	public GroundableEdge getActiveParentEdge(DAGTuple cur)
	{
		
		for(GroundableEdge edge: getInEdges(cur))
		{
			if (edge.hasBeenSeen())
			{
				logger.info("edge "+edge+ " has been seen");
				continue;
			}
			
			if (edge instanceof RepairingWordEdge)
			{
				logger.info("returning overarching edge"+((RepairingWordEdge)edge).overarchingRepairingEdge);
				return ((RepairingWordEdge)edge).overarchingRepairingEdge;
			}
			else if (edge instanceof BacktrackingEdge)
			{
				logger.error("This shouldn't really happen");
				logger.error("Backtracking edge:"+edge);
				logger.error("parent of:"+cur);
				
			}
			else return edge;
		}
		return null;
	}
	
	public GroundableEdge getActiveParentEdge()
	{
		return getActiveParentEdge(cur);
		
	}
	
	
	
	public boolean canBacktrack()
	{
		if (getActiveParent()==null)
		{
			logger.info("no active parent");
			logger.info("in edges:"+getInEdges(cur));
		
			return false;
		}
		if (atClauseRoot())
			return false;
		
		return true;
	}

	


	public DAGTuple getActiveParent(DAGTuple cur)
	{
		GroundableEdge activeParent=getActiveParentEdge(cur);
		if (activeParent instanceof VirtualRepairingEdge)
		{
			return getSource(((VirtualRepairingEdge)activeParent).getBacktrackingEdge());
		}
		
		return getSource(activeParent);
	}
	
	public DAGTuple getActiveParent()
	{
		return getActiveParent(cur);
	}
	
		/**
	 * 
	 * @return true if successful, false if we're at root without any more
	 *         exploration possibilities
	 */
	public boolean attemptBacktrack() {

		while (!moreUnseenEdges()) {
			logger.info("attempting to backtrack");
			if (!canBacktrack()) {
				logger.info("canbacktrack says cannot backtrack");
				return false;
			}
			
			

			GroundableEdge backover = getActiveParentEdge();
			backover.backtrack(this);
			
//			if (backover instanceof NewClauseEdge) {
//				
//				initiateLocalRepair();
//			}
			
			/*
			if (backover instanceof ActionReplayEdge) {
				((ActionReplayEdge) backover).ungroundReplayedEdgesFor(backover
						.word().speaker());

				GroundableEdge prevPrevEdge = getParentEdge(getSource(backover));
				if (prevPrevEdge != null
						&& !backover.word().speaker()
								.equals(prevPrevEdge.word().speaker())) {
					ungroundToClauseRootFor(backover.word().speaker(),
							getSource(backover));
				}

			} else if (backover.word() != null) {

				wordStack.push(backover.word());
				logger.debug("adding word to stack, now:" + backover.word);
				GroundableEdge prevPrevEdge = getParentEdge(getSource(backover));
				if (prevPrevEdge instanceof NewClauseEdge)
					prevPrevEdge = getParentEdge(getSource(prevPrevEdge));

				if (prevPrevEdge != null
						&& !backover.word().speaker()
								.equals(prevPrevEdge.word().speaker())) {
					ungroundToClauseRootFor(backover.word().speaker(),
							getSource(backover));
				}

				if (backover instanceof BacktrackingEdge) {
					BacktrackingEdge backEdge = (BacktrackingEdge) backover;
					backEdge.unmarkRepairedEdges();
					actionReplay.clear();
					// ungroundToRootFor(backover.word().speaker(), cur);
					logger.debug("going back (forward) over backtrakcing edge");
					logger.debug(":" + backEdge);
				}
			}

			GroundableEdge backOver = goUpOnce();
			// backOver.setBacktracked(true);
			// remove edge from context
			// context.goUpDelete();
			// mark edge that we're back over as seen (already explored)...

			// markOutEdgeAsSeen(backOver);
			backOver.setSeen(true);
			backOver.setInContext(false);
			*/
			

		}
		logger.debug("Backtrack succeeded");
		return true;
	}

	/*
	 * public DAGTuple getDest(GroundableEdge edge) { if (!(edge instanceof
	 * RepairingEdge)) return super.getDest(edge);
	 * 
	 * RepairingEdge repairingEdge=(RepairingEdge) edge; return
	 * super.getDest(repairingEdge.edgePair.second); }
	 * 
	 * public T getSource(E edge) { if (!(edge instanceof RepairingEdge)) return
	 * super.getSource(edge); RepairingEdge<E> repairingEdge=(RepairingEdge<E>)
	 * edge; return super.getSource(repairingEdge.edgePair.first); } public
	 * DAGTuple getSource(GroundableEdge edge) { if (!(edge instanceof
	 * RepairingEdge)) return super.getSource(edge);
	 * 
	 * RepairingEdge repairingEdge=(RepairingEdge) edge; return
	 * super.getSource(repairingEdge.edgePair.first);
	 * 
	 * }
	 */

	

	public VirtualRepairingEdge getNewRepairingEdge(List<GroundableEdge> backtrackedOver,
			List<Action> repairingActions, DAGTuple midTuple,
			UtteredWord repairingWord) {
		BacktrackingEdge newBackEdge = getNewBacktrackingEdge(backtrackedOver,
				repairingWord.speaker());
		RepairingWordEdge repairingWordEdge = getNewRepairingWordEdge(repairingActions,
				repairingWord);
		long newID = idPoolEdges.size() + 1;
		VirtualRepairingEdge repairingEdge = new VirtualRepairingEdge(newBackEdge,
				repairingWordEdge, midTuple, newID);
		newBackEdge.overarchingRepairingEdge=repairingEdge;
		repairingWordEdge.overarchingRepairingEdge=repairingEdge;
		
		return repairingEdge;

	}

	
	public GroundableEdge getParentEdge(DAGTuple node)
	{
		Collection<GroundableEdge> edges=getInEdges(node);
		if (edges.size()==0)
			return null;
		
		for(GroundableEdge edge: edges)
		{
			if (!(edge instanceof BacktrackingEdge))
				return edge;
		}
		return null;
	}
	@Override
	public RepairingWordEdge getNewRepairingWordEdge(List<Action> actions,
			UtteredWord word) {
		
		long newID = idPoolEdges.size() + 1;
		RepairingWordEdge result = new RepairingWordEdge(actions, word, newID);
		idPoolEdges.add(newID);
		return result;
	}
	
	public String getSpeakerOfPreviousWord() {
		
		if (atRoot())
			return null;
		GroundableEdge parent=getActiveParentEdge();
		if (parent==null)
			return null;
		
		return parent.word.speaker();
		
	}

}
