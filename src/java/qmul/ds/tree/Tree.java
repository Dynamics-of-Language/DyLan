package qmul.ds.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.action.Grammar;
import qmul.ds.formula.Formula;
import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRInfixExpression;
import qmul.ds.formula.TTRLambdaAbstract;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.formula.Variable;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.Requirement;
import qmul.ds.tree.label.TypeLabel;
import qmul.ds.type.DSType;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.TreeFactory;

/**
 * A DS tree
 * 
 * @author mpurver
 */
public class Tree extends TreeMap<NodeAddress, Node> implements Cloneable,
		Serializable {

	private final String ENTITY_VARIABLE_ROOT = "x";
	private final String EVENT_VARIABLE_ROOT = "e";
	private final String PROPOSITION_VARIABLE_ROOT = "p";
	private final String REC_TYPE_VARIABLE_ROOT = "r";
	private final String PREDICATE_VARIABLE_ROOT = "pred";
	protected static Logger logger = Logger.getLogger(Tree.class);

	private static final long serialVersionUID = 1L;

	private NodeAddress pointer;
	private NodeAddress root;
	private static final String UNICODE_POINTER = "\u25C6"; // black diamond;
	// also white
	// diamond 25CA

	private int numRequirements = 1; // only a rough count for sorting, use

	// isComplete() for reliable check

	/**
	 * A new AXIOM tree
	 */
	public Tree() {
		super();
		pointer = new NodeAddress();
		Node node = new Node(pointer);
		node.addLabel(new Requirement(TypeLabel.t, null));
		put(pointer, node);
		root = pointer;
	}

	public Tree(NodeAddress root) {
		super();
		this.root = root;
		pointer = root;
		Node node = new Node(pointer);
		put(pointer, node);
	}

	/**
	 * A new copy of tree, with (shallow) cloned {@link Node}s
	 * 
	 * @param tree
	 */
	public Tree(Tree tree) {
		super();
		this.root = tree.root;
		for (NodeAddress key : tree.keySet()) {
			put(key, tree.get(key).clone());
		}
		setPointer(tree.pointer);
		numRequirements = tree.numRequirements;
		this.entityPool = new ArrayList<Variable>(tree.entityPool);
		this.eventPool = new ArrayList<Variable>(tree.eventPool);
		this.propositionPool = new ArrayList<Variable>(tree.propositionPool);
		this.recordTypePool = new ArrayList<Variable>(tree.recordTypePool);
		this.predicatePool = new ArrayList<Variable>(tree.predicatePool);
	}

	public Tree(NodeAddress prefix, DSType t) {
		super();
		pointer = prefix;
		Node node = new Node(pointer);
		node.addLabel(new Requirement(TypeLabel.cn, null));
		put(pointer, node);
		root = prefix;
	}

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

	/**
	 * @return the root node
	 */
	public Node getRootNode() {
		return get(root);
	}

	/**
	 * @return the pointer
	 */
	public NodeAddress getPointer() {
		return pointer;
	}

	/**
	 * search for node labelled with the set of labels passed as argument
	 * 
	 * @param Treeset
	 *            of labels the pointer to set
	 * @return Node the node containing all the labels
	 */
	public Node getNodeLabelledWith(TreeSet<Label> labelSet) {
		Node: for (NodeAddress na : this.keySet()) {
			Node cur = this.get(na);
			for (Label l : labelSet) {
				if (!cur.hasLabel(l))
					continue Node;
			}
			return cur;
		}
		return null;
	}

	/**
	 * search for node labelled with all the labels passed as argument
	 * 
	 * @param labels
	 *            labels to search for on each node
	 * @return boolean true if tree contains such a node; false otherwise
	 */
	public boolean hasNodeLabelledWith(Collection<Label> labels) {
		for (Node node : getNodes()) {
			if (node.hasLabels(labels)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param pointer
	 *            the pointer to set
	 */
	public void setPointer(NodeAddress pointer) {
		this.pointer = pointer;
	}

	/**
	 * @return the nodes
	 */
	public Collection<Node> getNodes() {
		return values();
	}

	/**
	 * @return the {@link Node} at the current pointer
	 */
	public Node getPointedNode() {
		return get(getPointer());
	}

	/**
	 * @param modality
	 * @return the {@link Node} we arrive at by going via modality, or null if
	 *         none exists
	 */
	public Node getNode(Modality modality) {
		return get(getPointer().go(modality));
	}

	/**
	 * Move the pointer
	 * 
	 * @param modality
	 */
	public void go(Modality modality) {
		NodeAddress addr = getPointer().go(modality);
		if ((addr == null) || !containsKey(addr)) {
			throw new RuntimeException("Can't go to non-existent node " + addr);
		}
		setPointer(addr);
	}

	/**
	 * Make a new daughter node below the pointed node
	 * 
	 * @param op
	 */
	public void make(BasicOperator op) {

		if (!(op.isDown())) {
			throw new RuntimeException("Can't make non-daughter node " + op);
		}
		NodeAddress addr = getPointer().go(op);
		if (!containsKey(addr)) {
			put(addr, new Node(addr));
		}
		// logger.debug("Made node in tree" + this);
		// logger.debug("With op:" + op);
	}

	/**
	 * Merge the node at modality with the pointed node, and remove it
	 * 
	 * @param node
	 */
	public void merge(Modality modality) {
		Node node = getNode(modality);
		// move daughters from merged node
		moveDaughters(getDaughters(node), node.getAddress(), pointer);
		// merge labels
		getPointedNode().merge(node);
		// remove merged node
		remove(node.getAddress());
	}

	public void merge(Node node) {
		// move daughters from merged node
		moveDaughters(getDaughters(node), node.getAddress(), pointer);
		// merge labels
		getPointedNode().merge(node);
		// remove merged node
		remove(node.getAddress());

	}

	/**
	 * Move daughters (and their daughters) from from to to
	 * 
	 * @param dtrs
	 * @param from
	 * @param to
	 */
	private void moveDaughters(ArrayList<Node> dtrs, NodeAddress from,
			NodeAddress to) {
		for (Node dtr : dtrs) {
			moveDaughters(getDaughters(dtr), from, to);
			NodeAddress newAddr = new NodeAddress(dtr
					.getAddress()
					.getAddress()
					.replaceFirst(Pattern.quote(from.getAddress()),
							to.getAddress()));
			Node newDtr = new Node(newAddr);
			newDtr.addAll(dtr);
			put(newAddr, newDtr);
			remove(dtr.getAddress());
		}
	}

	/**
	 * Add a label at the pointed node
	 * 
	 * @param label
	 */
	public void put(Label label) {
		boolean added = getPointedNode().addLabel(label);
		if (added && (label instanceof Requirement)) {
			numRequirements++;
		}
	}

	/**
	 * Delete a label at the pointed node
	 * 
	 * @param label
	 */
	public void delete(Label label) {
		boolean removed = getPointedNode().removeLabel(label);
		if (!removed) {
			logger.warn("Failed to delete:" + label + " on " + getPointedNode());
		}

		if (removed && (label instanceof Requirement)) {
			numRequirements--;
		}
	}

	/**
	 * @param node
	 * @return all existing daughter nodes in order 0,1,LINK,*
	 */
	public ArrayList<Node> getDaughters(Node node) {
		ArrayList<Node> dtrs = new ArrayList<Node>();
		Node d0 = get(node.getAddress().down0());
		if (d0 != null) {
			dtrs.add(d0);
		}
		Node d1 = get(node.getAddress().down1());
		if (d1 != null) {
			dtrs.add(d1);
		}
		Node dL = get(node.getAddress().downLink());
		if (dL != null) {
			dtrs.add(dL);
		}
		Node dU = get(node.getAddress().downStar());
		if (dU != null) {
			dtrs.add(dU);
		}
		Node dLU = get(node.getAddress().downLocalUnfixed());
		if (dLU != null) {
			dtrs.add(dLU);
		}
		return dtrs;
	}

	public ArrayList<Node> getDaughters(Node node, String order) {
		ArrayList<Node> dtrs = new ArrayList<Node>();
		for (NodeAddress address : possibleDaughters(node, order)) {
			Node d = get(address);
			if (d != null) {
				dtrs.add(d);
			}
		}
		return dtrs;
	}

	public ArrayList<NodeAddress> possibleDaughters(Node node, String order) {
		ArrayList<NodeAddress> addresses = new ArrayList<NodeAddress>();
		for (int i = 0; i < order.length(); i++) {
			addresses.add(node.getAddress().down(order.substring(i, i + 1)));

		}
		return addresses;
	}

	/**
	 * @return true if this tree is complete i.e. no outstanding requirements at
	 *         any node AND pointer at root node
	 */
	public boolean isComplete() {
		if (!pointer.isRoot()) {
			return false;
		}
		for (Node node : values()) {
			if (!node.isComplete()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return true if this tree has a linked tree on it
	 */
	public boolean hasLink() {
		for (Node node : values()) {
			if (!node.isLinked()) {

				return true;
			}
		}
		return false;
	}

	/**
	 * @return the number of unsatisfied {@link Requirement}s. NOT A RELIABLE
	 *         COUNT - just for tree ordering in displays. Use isComplete() for
	 *         a reliable check.
	 */
	public int numRequirements() {
		return numRequirements;
	}

	/**
	 * @deprecated
	 * @param other
	 * @return
	 */
	public boolean subsumesOld(Tree other) {
		List<Node> otherTyCMatched = new ArrayList<Node>();
		node: for (Node thisNode : values()) {
			for (Node otherNode : other.values()) {
				if (thisNode.subsumes(otherNode)) {
					if (thisNode.hasType()
							&& otherTyCMatched.contains(otherNode)) {
						logger.info("Node in other already used: " + otherNode);
						continue;
					}
					if (thisNode.hasType())
						otherTyCMatched.add(otherNode);

					continue node;
				}
			}
			logger.info("susbume fail for node " + thisNode.getAddress()
					+ " No node found in other");
			return false;
		}
		return true;

		/*
		 * if (!thisNode.isLocallyFixed()) {
		 * System.out.println("subsume pass for node " + thisNode.getAddress());
		 * System.out.println("against other node:"+otherNode); }
		 */
	}

	/**
	 * Checks tree subsumption depth first, recursively instantiating unfixed
	 * addresses by hypothesising merge points in the other tree and then
	 * checking as fixed... it also keeps track of already used (matched) nodes,
	 * so that not more than one type complete node in this tree is matched to a
	 * single type complete node in the other. Merge points are also controlled
	 * in this manner.
	 * 
	 * @param other
	 * @return true if this tree subsumes the other tree
	 */

	public boolean subsumes(Tree other) {

		return subsumes(other, get(new NodeAddress("0")),
				other.get(new NodeAddress("0")), new HashSet<Node>()) != null;

	}

	private Set<Node> subsumes(Tree other, Node thisSubtreeRoot,
			Node otherSubtreeRoot, Set<Node> usedNodes) {
		if (usedNodes.contains(otherSubtreeRoot))
			return null;
		if (!thisSubtreeRoot.subsumes(otherSubtreeRoot)) {
			logger.debug("failed Subsumption, this Root:" + thisSubtreeRoot
					+ " vs. " + otherSubtreeRoot);
			logger.debug("but this root on this tree:"
					+ get(thisSubtreeRoot.getAddress()));
			return null;
		}
		logger.debug("subsumed, this root:" + thisSubtreeRoot + " vs. "
				+ otherSubtreeRoot);
		logger.debug("but this root on this tree:"
				+ get(thisSubtreeRoot.getAddress()));
		Set<Node> used = new HashSet<Node>(usedNodes);
		if (thisSubtreeRoot.hasType())
			used.add(otherSubtreeRoot);
		else if (!thisSubtreeRoot.isLocallyFixed())
			used.add(otherSubtreeRoot);

		for (Node fixedDaughter : getDaughters(thisSubtreeRoot, "01L")) {
			String daughterAddress = fixedDaughter.getAddress().getAddress();
			String suffix = daughterAddress.substring(
					daughterAddress.length() - 1, daughterAddress.length());
			NodeAddress otherAddress = new NodeAddress(otherSubtreeRoot
					.getAddress().getAddress() + suffix);
			if (!other.containsKey(otherAddress))
				return null;
			Set<Node> daughterUsed = subsumes(other, fixedDaughter,
					other.get(otherAddress), used);
			if (daughterUsed == null)
				return null;
			used.addAll(daughterUsed);
		}

		outer: for (Node unfixedDaughter : getDaughters(thisSubtreeRoot, "*U")) {
			for (Node otherNode : other.values()) {
				if (used.contains(otherNode))
					continue;
				if (otherNode.getAddress().getAddress()
						.startsWith(otherSubtreeRoot.getAddress().getAddress())) {
					Set<Node> unfixedDaughterUsed = subsumes(other,
							unfixedDaughter, otherNode, used);
					if (unfixedDaughterUsed != null) {
						used.addAll(unfixedDaughterUsed);
						continue outer;
					}
				}
			}
			return null;
		}
		return used;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.TreeMap#clone()
	 */
	@Override
	public Tree clone() {
		return new Tree(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((pointer == null) ? 0 : pointer.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		Tree other = (Tree) obj;
		if (pointer == null) {
			if (other.pointer != null)
				return false;
		} else if (!pointer.equals(other.pointer))
			return false;
		if (!super.equals(other)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String partiality = (isComplete() ? "(C)" : "(P)");
		return "Tree " + partiality + " " + pointer + ":" + values() + "]";
	}

	public edu.stanford.nlp.trees.Tree toStanfordTree() {
		return toStanfordTree(new LabeledScoredTreeFactory(), getRootNode());
	}

	private edu.stanford.nlp.trees.Tree toStanfordTree(TreeFactory tf, Node node) {
		List<edu.stanford.nlp.trees.Tree> kids = new ArrayList<edu.stanford.nlp.trees.Tree>();
		String label = node.toUnicodeString();

		if (node.getAddress().equals(pointer)) {
			label = label.replaceAll(
					"("
							+ Pattern.quote(node.getAddress()
									+ Node.ADDRESS_SEPARATOR) + ")",
					UNICODE_POINTER + "$1");
		}
		edu.stanford.nlp.trees.Tree t = tf.newTreeNode(label, kids);
		for (Node n : getDaughters(node)) {
			t.addChild(toStanfordTree(tf, n));
		}
		return t;
	}

	/**
	 * returns the node that is the root of this tree, that may not neccessarily
	 * be the matrix tree (i.e. could be linked tree) TODO only assumes linked
	 * trees have complex structure, in line with DS theory
	 * 
	 * 
	 * @param node
	 * @return
	 */
	public Node getLocalRoot(Node node) {
		String root = "0";
		if (node.getAddress().toString().contains("L")) {
			String linkModality = node
					.getAddress()
					.toString()
					.substring(0,
							node.getAddress().toString().lastIndexOf('L') + 1);
			root = linkModality;

		}
		logger.debug("local root is " + root);
		return get(new NodeAddress(root));
	}

	public static void main(String args[]) {

		Tree t = new Tree();
		t.make(BasicOperator.DOWN_1);

		Tree t1 = new Tree();
		t1.make(BasicOperator.DOWN_1);
		t1.go(BasicOperator.DOWN_1);
		System.out.println(t.subsumes(t1));
	}

	/**
	 * 
	 * Assumes grammar with event terms... @see{resource/2013-english-ttr}
	 * 
	 * Should make sure that, when doing induction, the getMaximalSemantics
	 * method is only called after decorating a new node with a new hypothesis,
	 * and not immediately after the node is created.... (?) maybe not TODO
	 * 
	 * @param t
	 * @return
	 */
	private void addUnderspecifiedFormulae() {
		HashMap<DSType, Formula> typeMap = new HashMap<DSType, Formula>();
		typeMap.put(DSType.e, Formula.create("[x:e|head==x:e]"));
		typeMap.put(DSType.es, Formula.create("[e1:es|head==e1:es]"));
		// typeMap.put(DSType.cn,
		// Formula.create("[x:e|head==x:e]").freshenVars(this));
		typeMap.put(DSType.cn, Formula.create("[x:e|head==x:e]"));
		// typeMap.put(DSType.t, Formula.create("[p:t]"));
		// for underspec VP
		typeMap.put(DSType.parse("e>(es>cn)"),
				Formula.create("R2^R1^(R1 ++ (R2 ++ [head==R1.head:es]))"));
		typeMap.put(DSType.parse("es>cn"),
				Formula.create("R1^(R1 ++ [head==R1.head:es])"));
		typeMap.put(DSType.parse("e>cn"),
				Formula.create("R1^(R1 ++ [head==R1.head:e])"));
		typeMap.put(DSType.parse("e>t"), Formula.create("R1^(R1 ++ [head:es])"));
		typeMap.put(DSType.parse("e>(e>t)"),
				Formula.create("R2^R1^(R1 ++ (R2 ++ [head:es]))"));
		// typeMap.put(DSType.parse("e>(e>(e>t))"), Formula
		// .create("R3^R2^R1^(R1 ++ (R2 ++ (R3 ++ [head:es])))"));
		typeMap.put(DSType.parse("es>(e>(e>t))"),
				Formula.create("R3^R2^R1^(R1 ++ (R2 ++ (R3 ++ [head:es])))"));
		typeMap.put(DSType.parse("e>(e>(e>t))"),
				Formula.create("R3^R2^R1^(R1 ++ (R2 ++ (R3 ++ [head:es])))"));
		// for underspec adjunct e>t, see below, special case

		typeMap.put(DSType.parse("cn>e"),
				Formula.create("R1^[r:R1|x:e|head==x:e]"));
		typeMap.put(DSType.parse("cn>es"),
				Formula.create("R1^[r:R1|e1:es|head==e1:es]"));
		for (Node n : values()) {
			if (!getDaughters(n, "01").isEmpty())
				continue;
			DSType dsType = n.getRequiredType() != null ? n.getRequiredType()
					: n.getType();
			Formula f = n.getFormula();
			if (dsType != null && f == null) {
				if (typeMap.containsKey(dsType)) {
					n.addLabel(new FormulaLabel(typeMap.get(dsType)
							.freshenVars(this)));
				} else if (!dsType.equals(DSType.t))
					logger.warn("could not add underspecified formula to node; ds type is not listed as underspecifiable:"
							+ n);
			}
		}

	}

	public Set<Node> getUnfixedNodes() {
		Set<Node> result = new HashSet<Node>();
		for (Node n : values())
			if (n.getAddress().isStarUnfixed()
					|| n.getAddress().isStarUnfixed())
				result.add(n);

		return result;
	}

	private List<Tree> mergeUnfixed() {

		List<Tree> result = new ArrayList<Tree>();

		for (Node unfixed : getUnfixedNodes()) {
			logger.debug("found unfixed node:"+unfixed);
			for (Node mergePoint : values()) {
				if (!mergePoint.isLocallyFixed()) {
					continue;
				}
				DSType mergePointType = mergePoint.getType() == null ? mergePoint
						.getRequiredType() : mergePoint.getType();
				DSType unfixedType = unfixed.getType() == null ? unfixed
						.getRequiredType() : unfixed.getType();
				Formula mergePointF = mergePoint.getFormula();
				if (getDaughters(mergePoint, "01").isEmpty()
						&& unfixed.getAddress().subsumes(
								mergePoint.getAddress()) && mergePointF == null
						&& mergePointType.equals(unfixedType)) {
					Tree t = clone();
					t.setPointer(mergePoint.getAddress());
					t.merge(unfixed);
					result.add(t);
				}

			}

		}
		if (result.isEmpty())
			result.add(clone());

		return result;

	}

	/**
	 * Semantic Node Decorations in TTR - otherwise should return empty set, or
	 * throw pointer exception!
	 * 
	 * The maximal semantics will not contain a head field.
	 * 
	 * @return the maximal semantics of this tree
	 */
	public TTRFormula getMaximalSemantics() {

		List<Tree> merged = mergeUnfixed();
		logger.debug("Merging unfixed if possible,");
		logger.debug("after merge:"+merged.get(0));
		if (merged.isEmpty())
			return null;
		if (merged.size() < 2) {
			merged.get(0).addUnderspecifiedFormulae();
			logger.debug("After adding underspecified formulae, tree is:"
					+ merged.get(0));
			TTRFormula f = merged.get(0).getMaximalSemantics(
					merged.get(0).getRootNode());

			return f;
		}
		merged.get(0).addUnderspecifiedFormulae();
		merged.get(1).addUnderspecifiedFormulae();
		TTRRecordType first = (TTRRecordType) merged.get(0)
				.getMaximalSemantics();
		TTRRecordType second = (TTRRecordType) merged.get(1)
				.getMaximalSemantics();

		TTRInfixExpression disjunction = new TTRInfixExpression(
				TTRInfixExpression.TTR_DISJUNTION_FUNCTOR, first, second);

		for (int i = 2; i < merged.size(); i++) {
			merged.get(i).addUnderspecifiedFormulae();
			TTRRecordType cur = (TTRRecordType) merged.get(i)
					.getMaximalSemantics();

			disjunction = new TTRInfixExpression(
					TTRInfixExpression.TTR_DISJUNTION_FUNCTOR, disjunction, cur);
		}

		return disjunction;
	}

	/**
	 * Preconditions: all mergeable unfixed nodes are merged already
	 * 
	 * @return a record type expressing the maximal semantics of this tree
	 */
	public TTRFormula getMaximalSemantics(Node root) {
		// ignore unfixed.
		if (getDaughters(root, "01").size() == 1) {
			logger.error("node with only one fixed daughter.." + root);
			return null;
		}
		logger.debug("getting semantics of tree rooted at:" + root);
		Node unfixed = get(root.getAddress().downStar());
		Node localUnfixed = get(root.getAddress().downLocalUnfixed());
		TTRFormula unfixedReduced = null;
		if (unfixed != null) {
			unfixedReduced = getMaximalSemantics(unfixed);

		}
		TTRFormula localUnfixedReduced = null;
		if (localUnfixed != null) {
			localUnfixedReduced = getMaximalSemantics(localUnfixed);

		}
		TTRFormula rootReduced = null;
		// if (getDaughters(root).isEmpty())
		rootReduced = root.getFormula() == null ? new TTRRecordType()
				: (TTRFormula) root.getFormula();

		if (getDaughters(root, "01").size() == 2) {
			// at local root

			TTRFormula argMax = getMaximalSemantics(get(root.getAddress()
					.down0()));
			TTRLambdaAbstract functMax = (TTRLambdaAbstract) getMaximalSemantics(get(root
					.getAddress().down1()));
			logger.debug("beta-reducing. Funct:" + functMax);
			logger.debug("beta-reducing. Arg:" + argMax);
			rootReduced = functMax.betaReduce(argMax);

			if (unfixedReduced != null) {

				rootReduced = rootReduced.conjoin(unfixedReduced);

			}
			if (localUnfixedReduced != null) {
				rootReduced = rootReduced.conjoin(localUnfixedReduced);
			}
		}
		else
		{
			// no fixed daughters.. only happens when we are at root of tree with
			// unfixed nodes
				if (unfixedReduced != null && localUnfixedReduced != null)
					rootReduced = unfixedReduced
					.conjoin(localUnfixedReduced);

				else if (unfixedReduced != null && localUnfixedReduced == null)
					rootReduced = unfixedReduced;
				else if (localUnfixedReduced != null)
					rootReduced = localUnfixedReduced;
		}
		
		if (!getDaughters(root, "L").isEmpty()) {

			Formula maxSemL = getMaximalSemantics(get(root.getAddress()
					.downLink()));

			rootReduced = rootReduced.conjoin(maxSemL);
		}
		logger.debug("done: " + rootReduced);
		return rootReduced;

	}

	public boolean atFunctor() {
		if (pointer.getAddress().endsWith("1"))
			return true;
		else
			return false;
	}

	public void go(BasicOperator op) {

		NodeAddress addr = getPointer().go(op);
		setPointer(addr);
	}

	/**
	 * attaches other onto this tree at pointer the pointer, if pointed node of
	 * this tree has the same address as the root of the other..
	 * 
	 * @param other
	 * @return
	 */
	public Tree merge(Tree other) {
		Tree copy = new Tree(this);
		// if (!other.getRootNode().getAddress().equals(copy.getPointer())) {
		// logger.warn("trees are not mergeable... address mismatch. This tree:"
		// + this + "\nother tree:" + other);
		// return null;
		// }
		copy.putAll(other);

		return copy;
	}

	public boolean isAxiom() {
		return equals(new Tree());
	}

}
