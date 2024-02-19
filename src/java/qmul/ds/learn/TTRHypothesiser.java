package qmul.ds.learn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.ParserTuple;
import qmul.ds.action.Action;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.EffectFactory;
import qmul.ds.action.atomic.Put;
import qmul.ds.action.atomic.TTRFreshPut;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGInductionState;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.TypeLattice;
import qmul.ds.dag.TypeLatticeIncrement;
import qmul.ds.dag.UtteredWord;
import qmul.ds.formula.Formula;
import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRLabel;
import qmul.ds.formula.TTRPath;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.tree.BasicOperator;
import qmul.ds.tree.Node;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.Label;
import qmul.ds.type.DSType;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;

public class TTRHypothesiser extends Hypothesiser {
	/**
	 * Hypthesiser to hypothesise action sequences that lead from the axiom tree
	 * to the target semantics. difference from before: no target tree - there
	 * are target treeS, which are hypothesised incrementally upon
	 * link-adjunction, and of course at the very beginning.
	 * 
	 * NOTE: Unlike the Hypothesiser class this class assumes all words are
	 * unknown... does not interleave parsing....
	 * 
	 * @author arash
	 */
	protected final static Logger logger = Logger
			.getLogger(TTRHypothesiser.class);

	TypeLattice lattice;
	TTRRecordType targetType;
	int wordDepth = 0;

	// Stack<ParserTuple> curTreeTargets=new Stack<ParserTuple>();

	public TTRHypothesiser(String resourceDirOrURL, TTRRecordType rt,
			String sent) {
		super(resourceDirOrURL);
		loadTrainingExample(sent, rt);
	}

	public TTRHypothesiser(String seedResourceDir) {
		super(seedResourceDir);
	}

	public void loadTrainingExample(String sentence, TTRRecordType target) {
		if (this.seedLexicon == null || this.optionalGrammar == null
				|| this.nonoptionalGrammar == null) {
			throw new IllegalStateException("Hypothesiser not initialised");
		}
		logger.info("loading example:" + sentence + "::" + target);
		String[] sent = sentence.trim().split("\\s");

		this.state = new DAGInductionState(UtteredWord.getAsUtteredWords(sentence));
		this.targetType = target;
		lattice = new TypeLattice(target);

		// logger.debug(targetType);
		this.hypotheses.clear();

		initialise();
	}

	public void initialise() {
		Set<List<TypeLatticeIncrement>> incSet = lattice
				.getIncrements(targetType.getHeadField().getLabel());
		logger.info("incset is: "+incSet);
		wordDepth = 0;
		logger.info("initialising state, with initial increments:");
		for (List<TypeLatticeIncrement> inc : incSet) {
			TTRRecordType wholeInc = flatten(inc);
			// filtering trees according to subj. obj, etc being in their right
			// positions
			// only if the head element is an event.....
			boolean filtered = wholeInc.getHeadField().getDSType()
					.equals(DSType.es);
			logger.info("\nIncrement:" + wholeInc);
			logger.info("Now the abstraction trees:");
			List<Tree> trees = wholeInc.getFilteredAbstractions(state
					.getCurrentTuple().getTree().getPointer(), DSType.t,
					filtered);
			for (Tree tree : trees) {
				logger.info(tree);
				TreeHypothesis treeHyp = new TreeHypothesis(inc, tree);
				logger.debug("adding tree hyp child " + treeHyp);
				DAGInductionTuple child = new DAGInductionTuple(state
						.getCurrentTuple().getTree().clone());
				Tree mergedInc = state.getCurrentTuple().getTargetTree()
						.merge(treeHyp.getTree());
				Tree mergedNonHead = state.getCurrentTuple().getNonHeadTarget()
						.merge(treeHyp.getTree());
				DAGInductionTuple childAdded = state.addChild(child, treeHyp,
						null);
				childAdded.setTarget(mergedInc);
				childAdded.setNonHeadTarget(mergedNonHead);
			}

		}

	}

	private void applyTreeHypothesesEntity() {

		logger.debug("cur tree:" + state.getCurrentTuple().getTree());
		logger.debug("target tree:" + state.getCurrentTuple().getTargetTree());
		Node pointedOnTarget = state.getCurrentTuple().getTargetTree()
				.get(state.getCurrentTuple().getTargetTree().getPointer());

		TTRFormula pointedType = (TTRFormula) pointedOnTarget.getFormula();
		TTRLabel headLabel = pointedType.getHeadField().getLabel();
		Set<List<TypeLatticeIncrement>> incSet = lattice
				.getIncrements(headLabel);

		for (List<TypeLatticeIncrement> inc : incSet) {
			TTRRecordType wholeInc = flatten(inc);
			logger.debug("whole inc:" + wholeInc);
			TTRLabel incHeadLabel = wholeInc.getHeadField().getLabel();
			TTRRecordType headIncrement;
			if (incHeadLabel.equals(TTRRecordType.HEAD))
				headIncrement = wholeInc;
			else {
				wholeInc.remove(TTRRecordType.HEAD);
				headIncrement = wholeInc.substitute(incHeadLabel,
						TTRRecordType.HEAD);
			}
			logger.debug("increment=" + headIncrement);
			logger.debug(headIncrement.getRecord());
			List<Tree> trees = headIncrement.getFilteredAbstractions(state
					.getCurrentTuple().getTree().getPointer().up(), DSType.t,
					false);
			logger.debug("we get here");
			for (Tree tree : trees) {
				TreeHypothesis treeHyp = new TreeHypothesis(inc, tree);
				logger.debug(headIncrement);
				logger.debug("adding tree hyp child" + treeHyp);
				DAGInductionTuple child = new DAGInductionTuple(state
						.getCurrentTuple().getTree().clone());
				Tree mergedInc = state.getCurrentTuple().getTargetTree()
						.merge(treeHyp.getTree());
				Tree mergedNonHead = state.getCurrentTuple().getNonHeadTarget()
						.merge(treeHyp.getTree());
				DAGInductionTuple childAdded = state.addChild(child, treeHyp,
						null);
				childAdded.setTarget(mergedInc);
				childAdded.setNonHeadTarget(mergedNonHead);

			}

		}
	}

	private void applyTreeHypothesesEvent() {
		logger.debug("lattice cur:" + lattice.getCurrentTuple());
		logger.debug("cur target:" + state.getCurrentTuple().getTargetTree());
		logger.debug("cur tree:" + state.getCurrentTuple().getTree());
		logger.debug("cur nonhead:"
				+ state.getCurrentTuple().getNonHeadTarget());
		Tree curTree = state.getCurrentTuple().getTree();
		Tree curNonHeadTarget = state.getCurrentTuple().getNonHeadTarget();

		Node pointedOnTarget = curNonHeadTarget.get(curTree.getPointer().up());

		TTRFormula pointedType = (TTRFormula) pointedOnTarget.getFormula();

		// this is when on the target we don't have an event... e.g. for just
		// [x==john:e|head==x:e]
		TTRLabel headLabel;
		if (pointedType == null)
			headLabel = TTRRecordType.HEAD;
		else
			headLabel = pointedType.getHeadField().getLabel();

		Set<List<TypeLatticeIncrement>> incSet = lattice
				.getIncrements(headLabel);
		// logger.debug("cur lattice label:"+lattice.getCurIncLabel());
		logger.debug("increments on headLabel:" + headLabel);
		for (List<TypeLatticeIncrement> inc : incSet) {
			TTRRecordType wholeInc = flatten(inc);
			List<Tree> nonHeadTrees = wholeInc.getFilteredAbstractions(state
					.getCurrentTuple().getTree().getPointer(), DSType.t, false);
			TTRLabel incHeadLabel = wholeInc.getHeadField().getLabel();
			TTRRecordType headIncrement;
			if (incHeadLabel.equals(TTRRecordType.HEAD))
				headIncrement = wholeInc;
			else {
				headIncrement = wholeInc.removeHead().substitute(incHeadLabel,
						TTRRecordType.HEAD);
			}
			logger.debug(headIncrement);
			List<Tree> trees = headIncrement.getFilteredAbstractions(state
					.getCurrentTuple().getTree().getPointer(), DSType.t, false);

			for (int i = 0; i < trees.size(); i++) {
				Tree tree = trees.get(i);
				Tree nonHeadTree = nonHeadTrees.get(i);
				TreeHypothesis treeHyp = new TreeHypothesis(inc, tree);
				logger.debug("adding tree hyp child " + treeHyp);
				DAGInductionTuple child = new DAGInductionTuple(state
						.getCurrentTuple().getTree().clone());
				Tree mergedInc = state.getCurrentTuple().getTargetTree()
						.merge(tree);
				Tree mergedNonHead = state.getCurrentTuple().getNonHeadTarget()
						.merge(nonHeadTree);
				DAGInductionTuple childAdded = state.addChild(child, treeHyp,
						null);
				childAdded.setTarget(mergedInc);
				childAdded.setNonHeadTarget(mergedNonHead);

			}

		}
	}

	protected Set<LexicalHypothesis> localLexicalHyps(Tree target,
			NodeAddress fixedOnTarget) {
		// Set<LexicalHypothesis> hyps = super.localLexicalHyps(target,
		// fixedOnTarget);
		Tree t = state.getCurrentTuple().getTree();
		if (!target.containsKey(fixedOnTarget)) {
			// logger.info("address not on target:" + target);
			return new HashSet<LexicalHypothesis>();
		}

		// set pointer on target to the address of the hypothesised merge point,
		// to be later used by adjunction
		// this is specific to the TTRVersion
		logger.trace("setting target pointer to:" + fixedOnTarget);
		state.getCurrentTuple().getTargetTree().setPointer(fixedOnTarget);
		state.getCurrentTuple().getNonHeadTarget().setPointer(fixedOnTarget);

		// ////////////////
		NodeAddress pointer = t.getPointer();
		Node node = t.get(pointer);

		Node targetNode = target.get(fixedOnTarget);
		Set<LexicalHypothesis> set = new HashSet<LexicalHypothesis>(
				targetIndependentHyps);
		// logger.info("Supposedly fixed pointer address on target:"+fixedOnTarget);
		if (node.hasType() || !isTerminalIn(t, t.getPointer())) {

			return set;
		}

		if (isTerminalIn(target, fixedOnTarget)) {

			// only if terminal will we hypothesise copying action
			if (copyHyp != null) {
				logger.debug("terminal in target, adding copy_hyp");
				set.add(this.copyHyp);
			}

			List<Effect> putList = new ArrayList<Effect>();
			boolean manifest = false;
			Formula f = null;
			for (Label l : targetNode) {
				if (node.hasLabel(l))
					continue;
				if (l instanceof FormulaLabel) {
					FormulaLabel fl = (FormulaLabel) l;
					f = fl.getFormula();
					manifest = f.hasManifestContent();
					putList.add(new TTRFreshPut((TTRFormula) f));
				} else
					putList.add(EffectFactory.create(Put.FUNCTOR + "(" + l
							+ ")"));
			}

			if (putList.isEmpty()) {
				logger.info("no unification hyps");
				// return new HashSet<LexicalHypothesis>();
			} else {
				set.add(new LexicalHypothesis("hyp-sem(" + f + ")", node
						.getTypeRequirement(), putList, manifest));
				// return set;
			}

		} else {
			logger.debug("was not terminal in target " + target);
		}

		return set;

	}

	private void applyTreeHypothesesCN() {

		// first extract restrictor label from the current target tree
		logger.debug("incrementing cn. Current target tree:"
				+ state.getCurrentTuple().getTargetTree());
		logger.debug("current tree:" + state.getCurrentTuple().getTree());
		Set<List<TypeLatticeIncrement>> incSet;
		NodeAddress pointer = state.getCurrentTuple().getTree().getPointer();
		NodeAddress upOneUpLup0 = pointer.go(BasicOperator.UP);
		upOneUpLup0 = upOneUpLup0.go(BasicOperator.UP);
		upOneUpLup0 = upOneUpLup0.go(BasicOperator.UP);

		DSType upOneUpLup0Type = state.getCurrentTuple().getTree()
				.get(upOneUpLup0).getType();
		if (upOneUpLup0Type == null)
			upOneUpLup0Type = state.getCurrentTuple().getTree()
					.get(upOneUpLup0).getRequiredType();
		logger.info("grandmother node type:" + upOneUpLup0Type);
		if (upOneUpLup0Type.equals(DSType.e)
				|| upOneUpLup0Type.equals(DSType.es)) {
			// we have just linked off restrictor...

			Node mother = state
					.getCurrentTuple()
					.getTargetTree()
					.get(state.getCurrentTuple().getTargetTree().getPointer()
							.up());

			TTRRecordType eType = (TTRRecordType) mother.getFormula();
			logger.debug("cn mother formula:" + eType);
			List<TTRPath> paths = eType.getHeadField().getTTRPaths();
			TTRPath rDotHead = paths.get(0);
			incSet = lattice.getIncrements(rDotHead.getFirstLabel());
			if (incSet.isEmpty()) {
				logger.warn("lattice has no increments on CN node");
				logger.debug("lattice is currently on:"
						+ lattice.getCurrentTuple().getType());
				// logger.debug("head of label stack:"+lattice.getCurIncLabel());
			}
		} else {
			// logger.debug("getting lattice current increments... the current label is:"+lattice.getCurIncLabel());
			incSet = lattice.getHeadIncrements();
		}
		// extracted rN.head
		// now move lattice into sub-lattice, i.e. into the restrictor
		// TypeLatticeIncrement
		// transition=lattice.goFirst(rDotHead.getFirstLabel());

		logger.debug("increments:");
		for (List<TypeLatticeIncrement> inc : incSet) {
			TTRRecordType wholeInc = flatten(inc);
			logger.debug(wholeInc);

			List<Tree> trees = wholeInc.getFilteredAbstractions(state
					.getCurrentTuple().getTree().getPointer().up(), DSType.cn,
					false);
			for (Tree tree : trees) {
				TreeHypothesis treeHyp = new TreeHypothesis(inc, tree);
				logger.debug("adding tree hyp child" + treeHyp);
				DAGInductionTuple child = new DAGInductionTuple(state
						.getCurrentTuple().getTree().clone());
				Tree mergedInc = state.getCurrentTuple().getTargetTree()
						.merge(treeHyp.getTree());
				logger.info("merged:" + mergedInc);
				DAGInductionTuple childAdded = state.addChild(child, treeHyp,
						null);
				Tree mergedNonHead = state.getCurrentTuple().getNonHeadTarget()
						.merge(treeHyp.getTree());
				childAdded.setTarget(mergedInc);
				childAdded.setNonHeadTarget(mergedNonHead);
			}

		}
		logger.info("added " + state.getChildCount() + " tree hypothesis edges");
	}

	private static TTRRecordType flatten(List<TypeLatticeIncrement> incs) {
		TTRRecordType result = new TTRRecordType();
		for (TypeLatticeIncrement inc : incs) {
			if (inc.isPositive())
				result = (TTRRecordType) inc.getIncrement().asymmetricMerge(
						result);
		}
		return result;
	}

	public CandidateSequence extractSequence() {
		logger.debug("extracting sequence");
		DAGInductionTuple current = state.getCurrentTuple();
		ArrayList<Action> sequence = new ArrayList<Action>();
		while (!state.isRoot(current)) {
			DAGEdge behind = state.getParentEdge(current);
			if (!(behind.getAction() instanceof TreeHypothesis))
				sequence.add(0, behind.getAction());
			// logger.debug("adding:"+behind.getAction());
			current = state.getParent(current);
		}
		List<HasWord> words = new ArrayList<HasWord>();
		for (UtteredWord w : state.wordStack())
			words.add(0, new Word(w.word()));

		return new CandidateSequence(new ParserTuple(), sequence, words);
	}

	protected boolean hypothesiseOnce() {
		logger.info("Hypothesising once");
		TTRFormula maxSem = state.getCurrentTuple().getSemantics();
		boolean doneWithBranch = false;
		if (maxSem.subsumes(targetType) && targetType.subsumes(maxSem)) {
			logger.info("GOT TO THE SEMANTICS and ...");

			if (wordDepth < state.wordStack().size()) {
				// logger.debug("got the semantics but didn't get to complete tree yet");
				logger.warn("...too few semantics hyps. will continue hypothesising");
				logger.warn("wordDepth=" + wordDepth);
				logger.warn("words:" + state.wordStack());
			} else {
				logger.info("have seen enough semantic hyps");
				logger.info("extracting candidate sequence now");
				// System.out.print(".");
				CandidateSequence result = this.extractSequence();
				logger.info(ANSI_PURPLE + "got sequence:\n" + result + ANSI_RESET);
				// if (hypotheses.contains(result))
				// logger.error("Sequence seen before:"+result);

				this.hypotheses.add(result);
				logger.info(this.hypotheses.size() + ":" + result);  // This is the same as the log above!!
				if (this.hypotheses.size() > 300) {  // what is this hardcoded number?
					System.out.println("sequences exceeded 500");  // Is this 300 or 500?
					System.out.println("stopping");
					return false;
				}
				System.out.print(this.hypotheses.size() + " ");
				doneWithBranch = true;
			}

		} else if (state.getCurrentTuple().getTree().isComplete()) {  // TODO I think this is not efficient: getCurrentTuple() is called three times!
			logger.warn("got to complete tree, but no subsumption: "
					+ state.getCurrentTuple().getTree());
			logger.warn("maxSem:" + maxSem);
			logger.warn("Target sem:" + targetType);
			logger.warn("current target tree is:"
					+ state.getCurrentTuple().getTargetTree());
		}
		else {
			logger.debug("current tree is not complete and there was no two way subsumption");
			logger.warn("maxSem:" + maxSem);
			logger.warn("Target sem:" + targetType);


		}
		if (!state.atRoot()
				&& !state.getPrevAction().getName()
						.startsWith(HYP_ADJUNCTION_PREFIX) && !doneWithBranch) {
			this.applyLexicalHypotheses(state.getCurrentTuple().getTargetTree());
			this.applyOptionalGrammar(state.getCurrentTuple().getTargetTree());
		}
		// ParserTuple result=null;
		do {
			DAGEdge traversed = state.goFirst();
			logger.debug(traversed);
			if (traversed != null) {
				logger.debug("traversed:" + traversed.getAction());
				// non-optional grammar always applies without branching to ANY
				// new DAG tuple

				if (traversed.getAction() instanceof TreeHypothesis) {

					TreeHypothesis treeHyp = (TreeHypothesis) traversed
							.getAction();
					if (!lattice.go(treeHyp.increments))
						continue;
				} else if (traversed.getAction().getName()
						.startsWith(HYP_ADJUNCTION_PREFIX)) {
					logger.debug("traversed hyp-adj.. incrementing with:");
					// no target trees hypothesised
					// have to wait till we go across TreeHypothesis edge.

					DAGTuple prev = state.getParent(state.getCurrentTuple());
					Node pointed = prev.getTree().getPointedNode();
					DSType nodeType = pointed.getType() == null ? pointed
							.getRequiredType() : pointed.getType();
					if (nodeType.equals(DSType.cn)) {
						applyTreeHypothesesCN();
					} else if (nodeType.equals(DSType.e)) {
						applyTreeHypothesesEntity();
					} else
						applyTreeHypothesesEvent();

					return true;
				} else if (traversed.getAction().getName()
						.startsWith(HYP_SEM_PREFIX)) {
					LexicalHypothesis semHyp = (LexicalHypothesis) traversed
							.getAction();
					if (semHyp.hasSemanticContent)
						this.wordDepth++;

					if (wordDepth > state.wordStack().size()) {
						logger.debug("exceeding number of words.. backtracking");
						logger.debug("the action sequence was:");
						logger.debug(extractSequence().toString());
						continue;
					}
				}
				applyNonOptionalGrammar(state.getCurrentTuple().getTargetTree());
				return true; // }
			}
		} while (attemptBacktrack());

		logger.info(ANSI_RED + "DAG Exhausted." + ANSI_RESET);
		return false;
	}

	public void applyNonOptionalGrammar(Tree target) {
		DAGEdge traversed = null;
		do {
			for (Action a : this.nonoptionalGrammar.values()) {
				DAGTuple cur = this.state.getCurrentTuple();
				Tree t = cur.getTree();
				Tree result = a.execTupleContext(t.clone(), cur);
				if (result == null) {
					logger.debug("Action " + a.getName() + " failed at tree: "
							+ cur.getTree());

				} else if (!result.getMaximalSemantics().subsumes(targetType)) {
					logger.debug("Action " + a.getName()
							+ " failed subsumption at tree: " + cur.getTree());
					logger.debug("result maxsem was:"
							+ result.getMaximalSemantics());
					// logger.debug("target was:" + target);
				} else {
					logger.debug("applied action " + a + " to " + t);
					logger.debug("result was:" + result);
					state.addChild(result, a, new UtteredWord(this.curUnknownSubstring));
					break;
				}
			}
			traversed = state.goFirst();
		} while (traversed != null);
	}

	public void applyOptionalGrammar(Tree target) {
		// this.state.getCurrentTuple().getChildren().clear();
		for (ComputationalAction a : this.optionalGrammar.values()) {
			// if a non-optional action can be carried out, it has to be, with
			// no other computational possibilities
			// on this node
			DAGTuple cur = this.state.getCurrentTuple();

			Tree t = cur.getTree();
			Collection<Pair<? extends Action, Tree>> results = null;
			if (a.backtrackOnSuccess()) {
				results = a.execExhaustively(t.clone(), cur);
				logger.debug("Action " + a + "(exhaustive) to " + t);

			} else {
				Tree result = a.execTupleContext(t.clone(), cur);
				logger.debug("Action " + a + " to " + t);
				if (result != null) {
					results = new ArrayList<Pair<? extends Action, Tree>>();
					results.add(new Pair<ComputationalAction, Tree>(a, result));
				}
			}

			if (results == null) {
				logger.debug("Action " + a + " failed at tree: "
						+ cur.getTree());

			} else {
				for (Pair<? extends Action, Tree> pair : results) {

					if (!pair.second().getMaximalSemantics()
							.subsumes(targetType)) {
						logger.debug("failed subsumption result max sem was:"
								+ pair.second().getMaximalSemantics());
						logger.debug("target was:" + targetType);
						logger.debug("Action instance was:" + pair.first);
					} else {
						logger.debug("Success, result was:" + pair.second());
						logger.debug("Action instance was:" + pair.first());
						state.addChild(pair.second(), pair.first(),
								new UtteredWord(this.curUnknownSubstring));
					}
				}
			}
		}
	}

	/**
	 * 
	 * @return true if successful, false if we're at root without any more
	 *         exploration possibilities
	 */
	public boolean attemptBacktrack() {
		logger.info("backtracking...");
		while (!state.moreUnseenEdges()) {
			if (this.state.atRoot()) {
				logger.debug("can't backtrack");
				return false;
			}
			Action backAlong = state.getPrevAction();
			if (backAlong instanceof TreeHypothesis) {
				lattice.backtrack(((TreeHypothesis) backAlong).increments);
				logger.debug("backtracked lattice");
			} else if (backAlong.getName().startsWith(HYP_SEM_PREFIX)) {
				LexicalHypothesis hyp = (LexicalHypothesis) backAlong;
				if (hyp.hasSemanticContent)
					wordDepth--;
			}
			DAGEdge backOver = this.state.goUpOnce();
			logger.debug("now at:" + state.getCurrentTuple());
			// mark edge that we're back over as seen (already explored)...
			this.state.markEdgeAsSeenAndBelowItUnseen(backOver);

		}
		logger.info("Backtrack succeeded");
		// logger.info("top of stack:"+curTreeTargets.peek());

		return true;
	}

	public void applyLexicalHypotheses(Tree target) {
		for (LexicalHypothesis a : this.localLexicalHyps(target)) {
			// if a non-optional action can be carried out, it has to be, with
			// no other computational possibilities
			// on this node
			DAGTuple cur = this.state.getCurrentTuple();
			Tree t = cur.getTree();

			Collection<Pair<? extends Action, Tree>> results = null;
			if (a.backtrackOnSuccess()) {
				results = a.execExhaustively(t.clone(), cur);
				logger.debug("Action " + a + "(exhaustive) to " + t);

			} else {
				Tree result = a.execTupleContext(t.clone(), cur);
				logger.debug("Action " + a + " to " + t);
				if (result != null) {
					results = new ArrayList<Pair<? extends Action, Tree>>();
					results.add(new Pair<LexicalHypothesis, Tree>(a, result));
				}

			}

			if (results == null) {
				logger.debug("Action " + a + " failed at tree: "
						+ cur.getTree());

			} else {
				for (Pair<? extends Action, Tree> pair : results) {
					// if it is adjunction just let it happen (don't check for
					// subsumption), will fail if there are no
					// more increments
					if (pair.first().getName()
							.startsWith(HYP_ADJUNCTION_PREFIX)) {
						logger.debug("Success, result was:" + pair.second());
						logger.debug("Action instance was:" + pair.first());
						if (pair.first().getName().startsWith(HYP_ADJ_T_PREFIX)
								&& this.hypAdjT != null)
							state.addChild(pair.second(), this.hypAdjT,
									new UtteredWord(this.curUnknownSubstring));
						else
							state.addChild(pair.second(), pair.first(),
									new UtteredWord(this.curUnknownSubstring));
						continue;
					}
					TTRFormula maxSem = pair.second().getMaximalSemantics();
					if (!maxSem.subsumes(targetType)) {

						logger.debug("failed subsumption, resulting tree: "
								+ pair.second());
						logger.debug("maxSem:" + maxSem);
						logger.debug("target:" + targetType);
					} else {

						logger.debug("Success, result was:" + pair.second());
						logger.debug("Action instance was:" + pair.first());
						state.addChild(pair.second(), pair.first(),
								new UtteredWord(this.curUnknownSubstring));

					}
				}
			}

		}

	}

	public static void main(String a[]) {

		TTRRecordType target = TTRRecordType
				.parse("[x2==you : e|r : [x : e|p4==juice(x) : t|head==x : e]|x1==your(r.head, r) : e|e1==finish : es|p3==with(e1, x1) : t|p9==subj(e1, x2) : t|head==e1 : es]");

		// TTRRecordType target = TTRRecordType
		// .parse("[head:es|p==there(head):t]");
		// System.out.println(target.getFilteredAbstractions(new
		// NodeAddress("0"), DSType.t));
		TTRHypothesiser h = new TTRHypothesiser(
				"resource/2013-english-ttr-induction-seed/");
		h.loadTrainingExample("you finish with your juice", target);
		Collection<CandidateSequence> hyps = h.hypothesise();

		for (CandidateSequence hyp : hyps) {
			System.out.println(hyp + "\n");
		}
		System.out.println("there were " + hyps.size() + " sequences");
	}

	public void loadTrainingExample(Sentence<Word> sentence,
			TTRRecordType target) {
		logger.info(ANSI_CYAN+ "loading Training Example: " + sentence + ANSI_RESET);
		String sent = "";
		for (HasWord w : sentence)
			sent += w.word() + " ";

		loadTrainingExample(sent.trim(), target);
	}

}
