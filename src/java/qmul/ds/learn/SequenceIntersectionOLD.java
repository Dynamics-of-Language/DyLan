package qmul.ds.learn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.ParserTuple;
import qmul.ds.action.Action;
import qmul.ds.action.atomic.Abort;
import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.Requirement;

/**
 * This class represents the intersection of two or more candidate sequences ({@link CandidateSequence}), as required in
 * hypothesis generalisation and refinement, as per Eshghi et al. (2012). It supports the sequence intersection
 * operation ({@link intersectInto}) as defined in the same paper. This is used incrementally as new candidate sequences
 * for the same word are obtained from the {@link Hypothesiser}. So this class represents the generalisation over
 * candidate sequence hypotheses over the course of training.
 * 
 * @author arash
 * @deprecated
 */

public class SequenceIntersectionOLD extends ArrayList<Action> {
	private static Logger logger = Logger.getLogger(SequenceIntersectionOLD.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = -2228876059216267408L;
	/**
	 * 
	 */

	private Set<ParserTuple> start;
	// private String word;
	private int howmany = 0;

	public SequenceIntersectionOLD() {
		super();
		start = new HashSet<ParserTuple>();
	}

	private void addTupleToStart(ParserTuple tuple) {
		for (ParserTuple t : start) {
			if (tuple.getTree().subsumes(t.getTree()) && t.getTree().subsumes(tuple.getTree()))
				return;
		}
		start.add(tuple);
	}

	public boolean intersectInto(CandidateSequence cs) {
		if (isEmpty()) {
			start.add(cs.getStart());
			// this.word=cs.getWord();
			addAll(cs);
			howmany++;
			return true;
		}

		List<CandidateSequence> eq = cs.getEquivalenceClass();// these will be in order
		// from high length to low.

		for (CandidateSequence seq : eq) {
			if (seq.size() > size())
				continue;
			// slide seq over this intersection until you find
			// subsequence of this equal to seq
			outer: for (int i = 0; i <= size() - seq.size(); i++) {
				// Action a = get(i);
				inner: for (int offset = 0; offset < seq.size(); offset++) {
					Action aSeq = seq.get(offset);
					Action a = get(i + offset);
					if (!aSeq.equals(a))
						continue outer;

					// offset++;

				}
				for (int j = 0; j < i; j++) {
					trimLeft();
				}
				for (int j = seq.size(); j < size(); j++)
					remove(j);

				this.addTupleToStart(seq.getStart());
				howmany++;
				return true;
			}

		}

		return false;

	}

	private void trimLeft() {
		if (isEmpty())
			throw new IllegalArgumentException("Can't trim empty sequence");
		// Set<Tree> curStart=this.start;

		Action a = this.get(0);
		Set<ParserTuple> newStart = new HashSet<ParserTuple>();
		for (ParserTuple tuple : start) {
			logger.debug(start.size() + " tuples in Start.");
			logger.debug("Applying action " + a + " to " + tuple);
			ParserTuple t = new ParserTuple(a.execTupleContext(tuple.getTree().clone(), tuple));
			logger.debug("Result: " + t);
			newStart.add(t);

		}
		start = new HashSet<ParserTuple>(newStart);
		this.remove(0);

	}

	public String toString() {
		String s = "Start:\n";
		;

		for (ParserTuple tuple : start)
			s += tuple + "\n";

		s += "\nSequence (" + howmany + "): ";
		for (Action a : this) {
			if (a instanceof LexicalHypothesis)
				s += a + " | ";
			else
				s += a.getName() + "|";
		}
		return s;

	}

	public IfThenElse getITE() {
		List<Effect> effects = new ArrayList<Effect>();
		for (Action a : this) {
			IfThenElse ite = (IfThenElse) a.getEffect();
			for (Effect e : ite.getTHENClause())
				effects.add(e);
		}
		// List<Requirement> r=null;
		Requirement r = null;
		for (ParserTuple tuple : this.start) {
			Tree t = tuple.getTree();
			Node n = t.getPointedNode();
			r = n.getTypeRequirement();

		}
		Label[] IF = new Label[1];
		IF[0] = r;
		Effect[] ELSE = new Effect[1];
		ELSE[0] = new Abort();

		return new IfThenElse(IF, (Effect[]) effects.toArray(), ELSE);
	}

}
