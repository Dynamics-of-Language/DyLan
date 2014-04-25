/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.formula;

/**
 * A {@link PredicateArgumentFormula} with a suitable (eps/tau/iota) functor, and a {@link CNOrderedPair} as the single
 * argument (although it could be a {@link Variable} of course ...)
 * 
 * @author mpurver
 */
public class EpsilonTerm extends PredicateArgumentFormula {

	private static final long serialVersionUID = 1L;

	public EpsilonTerm(String functor, Formula pair) {
		super(functor, pair);
	}

	public EpsilonTerm(Predicate functor, Formula pair) {
		super(functor, pair);
	}

	public EpsilonTerm(EpsilonTerm et) {
		super(et);
	}

	/**
	 * @return the eps/tau/iota functor
	 */
	public Predicate getFunctor() {
		return getPredicate();
	}

	/**
	 * @return the ordered pair (or maybe variable) argument
	 */
	public Formula getOrderedPair() {
		return getArguments().get(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#conjoin(qmul.ds.formula.Formula)
	 */
	@Override
	public EpsilonTerm conjoin(Formula f) {
		if (!(f instanceof EpsilonTerm)) {
			logger.fatal("Cannot conjoin Epsilon term with formula of another type.");
		}
		EpsilonTerm ef = (EpsilonTerm) f;
		if (!ef.getFunctor().equals(getFunctor())) {
			logger.fatal("Cannot conjoin Epsilon term with one with a different functor.");
		}

		return new EpsilonTerm(getFunctor(), this.getOrderedPair().conjoin(ef.getOrderedPair()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.mp.Formula#substitute(qmul.ds.formula.mp.Formula, qmul.ds.formula.mp.Formula)
	 */
	@Override
	public EpsilonTerm substitute(Formula f1, Formula f2) {
		Formula[] args = new Formula[getArity()];
		for (int i = 0; i < getArity(); i++) {
			args[i] = arguments.get(i).substitute(f1, f2);
		}
		return new EpsilonTerm(this.predicate, args[0]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.PredicateArgumentFormula#evaluate()
	 */
	public EpsilonTerm evaluate() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.PredicateArgumentFormula#clone()
	 */
	public EpsilonTerm clone() {
		return new EpsilonTerm(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.PredicateArgumentFormula#toString()
	 */
	public String toString() {
		return "(" + predicate + ", " + arguments.get(0) + ")";
	}

	public static void main(String a[]) {
		TTRLambdaAbstract abs = (TTRLambdaAbstract) Formula.create("R^[r1:R|x1==(eps, r1.head, r1):e]");
		TTRRecordType rec = TTRRecordType.parse("[x3:e|p==man(x3):t|head:x3]");

		System.out.println(abs.betaReduce(rec));

	}
}
