package qmul.ds.learn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import qmul.ds.ParserTuple;
import qmul.ds.action.Action;
import qmul.ds.action.atomic.Abort;
import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.Requirement;
import edu.stanford.nlp.util.Pair;

public class LexicalHypothesis extends Action {
	/**
	 * 
	 * For compatibility with the implementation of the DAGParser and DAGParseState, this class just wraps a sequence of
	 * atomic actions into the THEN block of an IfThenElse action with an empty array of conditions (Labels) such that
	 * the action always succeeds, by just running this list of atomic actions in order. The ELSE block is just
	 * instantiated to a single Abort() action.
	 * 
	 * @param effects
	 */

	boolean hasSemanticContent = false;

	public LexicalHypothesis(String name, List<Effect> effects, boolean hasSem) {
		this.hasSemanticContent = hasSem;
		Label[] IF = {};
		Effect[] THEN = new Effect[effects.size()];
		for (int i = 0; i < effects.size(); i++)
			THEN[i] = effects.get(i);
		Effect[] ELSE = { new Abort() };
		IfThenElse ite = new IfThenElse(IF, THEN, ELSE);
		this.action = ite;
		this.name = name;
	}
	public LexicalHypothesis(String name, Requirement r, List<Effect> effects, boolean hasSem) {
		this.hasSemanticContent = hasSem;
		Label[] IF = new Label[1];
		IF[0]=r;
		Effect[] THEN = new Effect[effects.size()];
		for (int i = 0; i < effects.size(); i++)
			THEN[i] = effects.get(i);
		Effect[] ELSE = { new Abort() };
		IfThenElse ite = new IfThenElse(IF, THEN, ELSE);
		this.action = ite;
		this.name = name;
	}
	

	public LexicalHypothesis(String name, Effect a, boolean hasSem, boolean backtrack) {
		super(name, a);
		this.hasSemanticContent = hasSem;
		this.backtrackOnSuccess = backtrack;
	}

	public LexicalHypothesis(Action a, boolean hasSem) {
		this.name = a.getName();
		IfThenElse iteA = (IfThenElse) a.getEffect();

		IfThenElse ite = new IfThenElse(iteA.getIFClause(), iteA.getTHENClause(), iteA.getELSEClause());
		this.action = ite;
		this.backtrackOnSuccess = a.backtrackOnSuccess();
		this.hasSemanticContent = hasSem;
	}

	public String toString() {
		return this.getName();
		
	}

	public String toDebugString() {

		IfThenElse ite = (IfThenElse) this.action;
		String s = getName() + ":";
		for (int i = 0; i < ite.getTHENClause().length; i++) {
			s += ite.getTHENClause()[i] + ";";

		}
		return s.substring(0, s.length() - 1);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.Action#hashCode()
	 */
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = result * prime + name.hashCode();
		result = result * prime + Arrays.hashCode(((IfThenElse) action).getTHENClause());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		logger.debug("equals called in LexicalHypothesis");
		if (this == o)
			return true;
		if (o == null)
			return false;
		else if (this.getClass() != o.getClass())
			return false;
		else {

			LexicalHypothesis a = (LexicalHypothesis) o;
			IfThenElse aite = (IfThenElse) a.getEffect();
			IfThenElse ite = (IfThenElse) this.action;
			// TODO: beware of metavariable instantiation! For now lexical hypotheses do not contain any.
			if (aite.getTHENClause().length != ite.getTHENClause().length)
				return false;

			for (int i = 0; i < aite.getTHENClause().length; i++) {
				if (!aite.getTHENClause()[i].equals(ite.getTHENClause()[i]))
					return false;

			}

			return true;
		}
	}

	/**
	 * determines whether this hypothesis contains a semantic content formula decoration. This excludes decoration with
	 * fresh bound variables
	 * 
	 * @return true if this hypothesis contains a semantic content decoration
	 */
	public boolean containsContentDecoration() {
		/*
		 * Effect[] THEN=((IfThenElse)action).getTHENClause(); for(Effect e: THEN) { if (e instanceof Put) { Label
		 * l=((Put) e).getLabel(); if (l instanceof FormulaLabel) return true; } }
		 */
		return this.hasSemanticContent;
	}

	public LexicalHypothesis instantiate() {

		return new LexicalHypothesis(this.name, this.action, this.hasSemanticContent, this.backtrackOnSuccess);
	}

	public <T extends Tree> Collection<Pair<? extends Action, T>> execExhaustively(T tree, ParserTuple context) {
		IfThenElse ite = (IfThenElse) action;

		Collection<Pair<IfThenElse, T>> all = ite.execExhaustively(tree, context);
		if (all == null)
			return null;
		if (all.isEmpty())
			return null;
		Collection<Pair<? extends Action, T>> allA = new ArrayList<Pair<? extends Action, T>>();
		for (Pair<IfThenElse, T> p : all) {
			allA.add(new Pair<LexicalHypothesis, T>(new LexicalHypothesis(getName(), p.first(),
					this.hasSemanticContent, this.backtrackOnSuccess), p.second()));
		}

		return allA;
	}

}
