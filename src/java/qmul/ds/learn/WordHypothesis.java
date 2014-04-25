package qmul.ds.learn;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.ParserTuple;
import qmul.ds.action.Action;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.atomic.Abort;
import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTupleSet;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.LabelDisjunction;
import qmul.ds.tree.label.Requirement;
import qmul.ds.tree.label.TypeLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * This class represents the intersection of two or more candidate sequences ({@link CandidateSequence}), as required in
 * hypothesis generalisation and refinement, as per Eshghi et al. (2012). This is represented as a tree, whose nodes are
 * sets of {@link ParserTuple}'s, and whose edges are DAGEdges, containing DS {@link Action}s. Branching reflects
 * differences between the {@link CandidateSequence}s that are intersected. The class supports the sequence intersection
 * operation ({@link intersectInto}). This is used incrementally as new candidate sequences for the same word become
 * available from the {@link Hypothesiser}. So this class represents the generalisation over candidate sequence
 * hypotheses over the course of training. It will also, eventually, specify the conditional probability distribution
 * p(f(t)|a)..
 * 
 * @author arash
 */

@SuppressWarnings("serial")
public class WordHypothesis extends DelegateTree<DAGTupleSet, DAGEdge> {
	private static Logger logger = Logger.getLogger(WordHypothesis.class);

	// private String word;
	private int howmany = 0;
	private List<Long> idPoolNodes = new ArrayList<Long>();
	private List<Long> idPoolEdges = new ArrayList<Long>();
	private HasWord word = null;

	int id = 0;

	private double logProb = 1.0;

	public WordHypothesis(int id) {
		super();
		this.id = id;

	}

	public Collection<DAGTupleSet> getLeaves() {
		Collection<DAGTupleSet> result = new HashSet<DAGTupleSet>();
		if (this.getVertexCount() == 0)
			throw new IllegalStateException("Graph is empty when trying to get roots");
		for (DAGTupleSet v : this.getVertices()) {
			if (outDegree(v) == 0)
				result.add(v);
		}
		return result;
	}

	private void addTuple(ParserTuple tuple, DAGTupleSet tupleSet) {
		for (ParserTuple t : tupleSet) {
			if (tuple.getTree().subsumes(t.getTree()) && t.getTree().subsumes(tuple.getTree()))
				return;
		}
		tupleSet.add(tuple);
	}

	/**
	 * returns the maximal common path as a (non-fuzzy/normal) lexical action
	 * 
	 * @return the core
	 */
	public LexicalAction getCoreAction() {
		ArrayList<Action> actions=new ArrayList<Action>(); 
		List<Effect> THENClause = new ArrayList<Effect>();
		Label[] IFClause = new Label[1];

		DAGTupleSet root = getRoot();
		while (getChildCount(root) == 1 && hasNonComputationalDescendant(root)) {
			Action childAction = getChildEdges(root).iterator().next().getAction();
			//IfThenElse ite = (IfThenElse) childAction.getEffect();
			//THENClause.addAll(0, Arrays.asList(ite.getTHENClause()));
			actions.add(0, childAction);
			root = getChildren(root).iterator().next();
		}

		return new LexicalAction(this.word.toString(), actions);
//		Effect[] ELSEClause = new Effect[1];
//		ELSEClause[0] = new Abort();
//		Set<Label> disjunct=new HashSet<Label>();
//		for(ParserTuple tuple:root)
//		{
//			Label typeReq = tuple.getTree().getPointedNode().getTypeRequirement();
//			if (typeReq==null)
//				typeReq=new TypeLabel(tuple.getTree().getPointedNode().getType());
//			disjunct.add(typeReq);
//				
//		}
//		if (disjunct.size()<1)
//			throw new IllegalStateException("no type or type requirement in start tuple");
//		Label disjunction=null;
//		
//		if (disjunct.size()==1)
//			disjunction=disjunct.iterator().next();
//		else
//			disjunction=new LabelDisjunction(disjunct);		
//		
//		
//		IFClause[0] = disjunction;
	
	}

	public boolean intersectInto(CandidateSequence cs) {
		logger.debug("_________________________________________________");
		logger.debug("Intersecting:" + cs);
		logger.debug("into " + this);

		if (cs.getWords().size() != 1)
			throw new IllegalArgumentException("Illegal operation: Cannot intersect an unsplit candidate sequence.");
		HasWord word = cs.getWords().get(0);

		if (this.word != null && !word.equals(this.word))
			throw new IllegalArgumentException(
					"Illegal operation: trying to intersect sequences corresponding to different words");
		this.word = word;
		// logger.debug("into " + getName());
		if (getVertexCount() == 0) {
			DAGTupleSet tuple = DAGTupleSet.getNewTupleSet(idPoolNodes);
			// add the root
			addVertex(tuple);
			// logger.debug("added root:");
			// Collection<DAGEdge> children=getChildEdges(tuple);
			// logger.debug("Children are: "+children);
			for (int i = cs.size() - 1; i >= 0; i--) {
				Action a = cs.get(i);
				DAGTupleSet cur = DAGTupleSet.getNewTupleSet(idPoolNodes);
				DAGEdge childEdge = DAGEdge.getNewEdge(idPoolEdges, a);
				addChild(childEdge, tuple, cur, EdgeType.DIRECTED);

				tuple = cur;
			}
			ParserTuple startTuple = cs.getStart();
			tuple.add(startTuple);
			this.forwardPopulate(tuple, startTuple);
			howmany++;
			logger.debug("Intersected, init successful");
			return true;
		}
		int firstLexicalIndex = cs.getFirstLexicalIndex();

		Collection<DAGEdge> rootChildEdges = getChildEdges(root);
		if (!rootChildEdges.iterator().hasNext())
			throw new IllegalStateException("attempting to intersect cs into empty SI");
		// if (rootChildEdges.size()>1) throw new IllegalStateException("root has more than one child edge");
		DAGTupleSet curTuple = root;
		outer: for (int i = cs.size() - 1; i >= 0; i--) {
			Action curAction = cs.get(i);
			logger.debug("current action to be matched is:" + curAction);
			logger.debug("Current tuple is:" + curTuple);
			logger.debug("curTuple has " + getChildCount(curTuple) + " children");
			Collection<DAGEdge> childEdges = getChildEdges(curTuple);
			// logger.debug("Child Edges:\n"+childEdges);

			for (DAGEdge childEdge : childEdges) {
				Action childAction = childEdge.getAction();
				if (curAction.equals(childAction)) {
					logger.debug("going back along " + childAction);
					curTuple = getDest(childEdge);
					continue outer;
				}

			}
			// if we are here it means that we couldn't find any path through this existing SI to fit
			// the current action of the cs. We branch and thus create a new path from where we are
			// only if there are no more non-computational actions on cs and on this SI (searching right to left)
			if (i < firstLexicalIndex && !hasNonComputationalDescendant(curTuple)) {
				// here we b
				DAGTupleSet newVertex = DAGTupleSet.getNewTupleSet(idPoolNodes);
				DAGEdge newEdge = DAGEdge.getNewEdge(idPoolEdges, curAction);
				addChild(newEdge, curTuple, newVertex, EdgeType.DIRECTED);
				logger.debug("Branching with " + newEdge.getAction() + "and going forward");
				curTuple = newVertex;
			} else {
				logger.debug("INTERSECTION FAILED, cannot branch here");
				return false;
			}

		}

		if (!hasNonComputationalDescendant(curTuple)) {
			logger.debug("INTERSECTION SUCCESS, adding curTuple to set");
			logger.debug("before addition curTuple has " + getChildCount(curTuple) + " children");

			ParserTuple startTuple = cs.getStart();
			curTuple.add(startTuple);
			logger.debug("curTuple now has " + getChildCount(curTuple) + " children");
			this.forwardPopulate(curTuple, startTuple);
			howmany++;
			return true;
		} else {
			logger.debug("INTERSECTION FAILED, matched whole sequence but not gone far enough");
			return false;
		}

	}

	/**
	 * 
	 * assumes that v is on the common part of this SI... i.e. before the graph branches
	 */
	private boolean hasNonComputationalDescendant(DAGTupleSet v) {
		DAGTupleSet cur = v;
		while (getChildCount(cur) > 0) {
			Collection<DAGEdge> childEdges = getChildEdges(cur);
			if (childEdges.size() > 1)
				break;
			DAGEdge childEdge = childEdges.iterator().next();
			if (!(childEdge.getAction() instanceof ComputationalAction))
				return true;
			cur = getDest(childEdge);

		}
		return false;

	}

	public void forwardPopulate(DAGTupleSet tuple, ParserTuple t) {
		if (!tuple.contains(t))
			throw new UnsupportedOperationException("The tuple set does not contain the parser tuple");
		DAGTupleSet cur = tuple;
		ParserTuple curt = t;
		if (getParent(cur) == null)
			return;
		while (getParent(cur) != null) {
			DAGTupleSet parent = getParent(cur);
			Action parentAction = getParentEdge(cur).getAction();
			Tree result = parentAction.exec(curt.getTree().clone(), curt);
			logger.debug("Action:" + parentAction.getName());
			logger.debug("on tree:" + curt);
			logger.debug("result:" + result);
			if (result == null) {
				logger.error("Action:" + parentAction.getName() + " failed ");
				logger.debug("Action instance:"+parentAction.toDebugString());
				logger.error("on tree:" + curt);
				
				throw new RuntimeException();
			}
			ParserTuple newt = new ParserTuple(result);
			parent.add(newt);
			curt = newt;
			cur = parent;
		}

	}

	public Collection<List<Action>> extractMaximalActionSequences() {

		Collection<DAGTupleSet> leaves = getLeaves();
		Collection<List<Action>> result = new HashSet<List<Action>>();
		for (DAGTupleSet leaf : leaves) {
			DAGTupleSet cur = leaf;
			DAGEdge parentEdge;
			List<Action> curList = new ArrayList<Action>();
			while ((parentEdge = getParentEdge(cur)) != null) {
				curList.add(parentEdge.getAction());
				cur = getSource(parentEdge);
			}
			result.add(curList);
		}
		return result;

	}

	public HasWord getWord() {

		return word;
	}

	public int getCount() {
		return howmany;
	}

	public boolean equals(Object other) {
		if (this == other)
			return true;
		WordHypothesis oth = (WordHypothesis) other;
		return this.id == oth.id && this.word.equals(oth.word);

	}

	public String getName() {
		return this.word + "_" + id;
	}

	public String toString() {
		DecimalFormat df = new DecimalFormat("#.###");
		return this.getName() + ":" + df.format(getProb());
	}

	public int hashCode() {
		int prime = 17;
		int hash = 1;
		hash = prime * hash + id;
		hash = prime * hash + this.word.hashCode();
		return hash;
	}

	public void setLogProb(double p) {
		this.logProb = p;
	}

	public void setProb(double p) {
		this.logProb = Math.log(p);
	}

	public double getLogProb() {
		return this.logProb;
	}

	public double getProb() {
		if (logProb > 0)
			return 0;

		return Math.exp(logProb);
	}
}
