package qmul.ds.dag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;

import org.apache.log4j.Logger;

import qmul.ds.DAGParser;
import qmul.ds.action.Action;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.UnaryPredicateLabel;

/** This is a context DAG as per Eshghi et. al. (2015), where edges are instances of {@link GrounableEdge}, sequences of computational actions followed by a single lexical action - so each edge corresponds to a word.
 *  Nodes contain trees (and semantics) as usual.
 *  
 *  There are special edges: BacktrackingEdge, NewClauseEdge, and ActionReplayEdge that only this type of DAG properly supports.
 * 
 * @author Arash
 *
 */
public class WordLevelContextDAG extends DAG<DAGTuple, GroundableEdge> {

	public WordLevelContextDAG(DAGParser<DAGTuple, GroundableEdge> p) {
		super(p);
	}

	/**
	 * 
	 * 
	 * 
	 */
	protected static Logger logger = Logger.getLogger(WordLevelContextDAG.class);
	private static final long serialVersionUID = -8765365147853341079L;

	//protected Map<String, Set<DAGTuple>> acceptance_pointers = new HashMap<String, Set<DAGTuple>>();

	public void resetToFirstTupleAfterLastWord() {
		if (getFirstTupleAfterLastWord() != null) {
			GroundableEdge parent = getParentEdge();

			logger.debug("resetting to first tuple after last word");
			logger.debug("it is:" + cur);
			// first go back to root from where you are, undoing everything you
			// did including setting grounding states, and marking
			// repaired edges.. also remove all from context
			while (parent != null && !(parent instanceof NewClauseEdge && ((NewClauseEdge)parent).isGrounded())) {
				logger.debug(parent + "(inCont:" + parent.inContext() + ", id="
						+ parent.id + ")");
				parent.setSeen(false);
				parent.setInContext(false);

				if (parent.word() != null) {

					GroundableEdge prevPrevEdge = getParentEdge(getSource(parent));
					if (prevPrevEdge != null
							&& !parent.word().speaker()
									.equals(prevPrevEdge.word().speaker())) {
						ungroundToClauseRootFor(parent.word().speaker(),
								getSource(parent));
					}
				}
				if (parent instanceof BacktrackingEdge<?>) {
					BacktrackingEdge<GroundableEdge> backEdge = (BacktrackingEdge) parent;
					backEdge.unmarkRepairedEdges();
					actionReplay.clear();
					// ungroundToRootFor(backover.word().speaker(), cur);
					// logger.debug("going back (forward) over backtrakcing edge");
					// logger.debug(":"+backEdge);
				} else if (parent instanceof ActionReplayEdge) {
					((ActionReplayEdge) parent).ungroundReplayedEdgesFor(parent
							.word().speaker());

				}

				DAGTuple parentNode = getSource(parent);

				parent = getParentWithId(parentNode, parent.pid);
			}
			// now set the tuple to the first tuple after last word.
			setCurrentTuple(getFirstTupleAfterLastWord());
			setExhausted(false);
			wordStack().clear();

			// now go back to root setting all edges as unseen and in context...
			Stack<UtteredWord> stack = new Stack<UtteredWord>();
			parent = getParentEdge();
			GroundableEdge parentLast = parent;
			while (parent != null &&!(parent instanceof NewClauseEdge && ((NewClauseEdge)parent).isGrounded())) {
				logger.debug(parent + "(inCont:" + parent.inContext() + ")");
				markEdgeAsUnseenAndAboveItSeen(parent);
				stack.push(parent.word());

				DAGTuple parentNode = getSource(parent);
				parentLast = parent;
				parent = getParentWithId(parentNode, parent.pid);
			}
			logger.debug("stack after :" + stack);
			if (parentLast != null) {
				setCurrentTuple(getDest(parentLast));
				GroundableEdge current = parentLast;
				stack.pop();
				while (current != null) {

					current = goFirst(stack, current);
				}
			}

		}

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
	public BacktrackingEdge<GroundableEdge> getNewBacktrackingEdge(
			List<GroundableEdge> backtrackedOver, String speaker) {

		long newID = idPoolEdges.size() + 1;
		BacktrackingEdge<GroundableEdge> result = new BacktrackingEdge<GroundableEdge>(
				backtrackedOver, speaker, newID);
		idPoolEdges.add(newID);
		return result;

	}

	/**
	 * the difference between this and that of the overridden goFirst is that
	 * this one will traverse the first edge forward that is in accordance with
	 * the top of the stack. And it has the same parent id, as the most recent
	 * active edge (where we just came from).
	 * 
	 */
	public GroundableEdge goFirst() {
		if (outDegree(cur) == 0) {
			System.out.println("out degree of cur is 0");
			return null;
		}
		if (wordStack.isEmpty()) {
			System.out.println("GoFirst: wordstack is empty");
			return null;
		}
		UtteredWord word = wordStack.peek();
		SortedSet<GroundableEdge> edges = getOutEdges(cur,
				getActiveParentEdgeId());

		logger.trace("out edges of cur:" + edges);

		for (GroundableEdge e : edges) {
			DAGTuple child = getDest(e);

			if (!e.hasBeenSeen()) {
				logger.info("Going forward (first) along: " + e);
				if (e instanceof BacktrackingEdge<?>) {
					BacktrackingEdge<GroundableEdge> backEdge = (BacktrackingEdge) e;

					backEdge.markRepairedEdges();

					this.actionReplay.addAll(backEdge
							.getReplayableBacktrackedEdges());

					this.groundToClauseRootFor(word.speaker(), child);
				} else if (e instanceof ActionReplayEdge) {
					((ActionReplayEdge) e).groundReplayedEdgesFor(word
							.speaker());
					e.setInContext(true);
				} else if (e instanceof NewClauseEdge) {
					e.setInContext(true);
				} else {
					GroundableEdge prevEdge = getParentEdge();
					if (prevEdge != null
							&& prevEdge.word() != null
							&& !prevEdge.word().speaker()
									.equals(e.word().speaker()))
						groundToRootFor(e.word().speaker());

					e.setInContext(true);
					
					
				}

				this.cur = child;

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
				markAllOutEdgesAsUnseen();
				return e;
			}
		}
		return null;
	}

	public GroundableEdge goFirst(Stack<UtteredWord> stack, GroundableEdge from) {
		if (outDegree(cur) == 0) {
			logger.info("reached right edge of graph");
			return null;
		}
		if (stack.isEmpty()) {
			logger.info("GoFirst(stack): wordstack is empty");
			return null;
		}
		UtteredWord word = stack.peek();
		SortedSet<GroundableEdge> edges = getOutEdges(cur, from.id);

		logger.debug("out edges of cur:" + edges);

		for (GroundableEdge e : edges) {
			DAGTuple child = getDest(e);

			if (!e.hasBeenSeen() && e.word().equals(stack.peek())) {
				logger.info("Going forward (first) along: " + e);
				if (e instanceof BacktrackingEdge<?>) {
					BacktrackingEdge<GroundableEdge> backEdge = (BacktrackingEdge) e;

					backEdge.markRepairedEdges();

					this.actionReplay.addAll(backEdge
							.getReplayableBacktrackedEdges());

					this.groundToClauseRootFor(word.speaker(), child);
				} else if (e instanceof ActionReplayEdge) {
					((ActionReplayEdge) e).groundReplayedEdgesFor(word
							.speaker());
				} else {
					GroundableEdge prevEdge = getParentEdge();
					if (prevEdge != null
							&& prevEdge.word() != null
							&& !prevEdge.word().speaker()
									.equals(e.word().speaker()))
						groundToRootFor(e.word().speaker());
					e.setInContext(true);
				}

				this.cur = child;

				logger.info("depth is now:" + getDepth());

				if (e.word().word() != null) {
					if (e.word().equals(stack.peek()))
						stack.pop();
					else {
						logger.error("Trying to pop word off word stack when going along "
								+ e);
						logger.error("but word on stack is:" + stack.peek());
					}

				}
				// markAllOutEdgesAsUnseen();
				return e;
			}
		}
		return null;
	}

	private void markAllOutEdgesAsUnseen() {
		logger.trace("out edges of cur" + getOutEdges(cur));
		for (GroundableEdge edge : getOutEdges(cur)) {
			if (edge.pid == getActiveParentEdgeId())
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
		for (DAGEdge outEdge : this.getOutEdges(cur, seenEdge.pid)) {
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
		SortedSet<GroundableEdge> edges = this.getOutEdges(cur,
				getActiveParentEdgeId());
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
	/*
	@Override
	public void setSelfPointer() {
		this.setAcceptancePointer("self");

	}

	
	public void setAcceptancePointer(String other, DAGTuple cur) {
		if (!acceptance_pointers.containsKey(other))
			acceptance_pointers.put(other, new HashSet<DAGTuple>());
		
		acceptance_pointers.get(other).add(cur);
	}
	
	public void setAcceptancePointer(String other)
	{
		setAcceptancePointer(other, cur);
	}*/

	public String getTupleLabel(DAGTuple t) {
		Set<String> pointers = new HashSet<String>();
		for (String spkr: this.acceptance_pointers.keySet()){
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
			logger.trace("grounding" + parent + " for " + speaker);
			parent.groundFor(speaker);
			DAGTuple parentNode = getSource(parent);
			parent = getParentEdge(parentNode);
		}
		if (parent!=null&& parent instanceof NewClauseEdge)
		{
			((NewClauseEdge)parent).ground();
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

	}

	
	public DAGTuple addAxiom(List<Action> actions) {
		DAGTuple axiom = this.getNewTuple(new Tree());
		NewClauseEdge edge = this.getNewNewClauseEdge(actions);
		this.addChild(axiom, edge);
		return axiom;

	}

	/**
	 * 
	 * @return true if successful, false if we're at root without any more
	 *         exploration possibilities
	 */
	public boolean attemptBacktrack() {

		while (!moreUnseenEdges()) {
			if (!canBacktrack()) {
				return false;
			}

			GroundableEdge backover = getActiveParentEdge();
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

			} 
			else if (backover.word() != null) {

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

				if (backover instanceof BacktrackingEdge<?>) {
					BacktrackingEdge<GroundableEdge> backEdge = (BacktrackingEdge) backover;
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
			if (backover instanceof NewClauseEdge) {
				parser.initiateRepair();
			}

		}
		logger.debug("Backtrack succeeded");
		return true;
	}
	
	

}
