package qmul.ds.formula;

import java.util.HashMap;

/**
 * A lambda abstract X^F(...X...) where X is a {@link Variable} and F(...) is a {@link Formula}
 * 
 * @author mpurver
 */
/**
 * @author mpurver
 * 
 */
public class FOLLambdaAbstract extends Formula implements LambdaAbstract {

	private static final long serialVersionUID = 1L;

	protected static final String LAMBDA_FUNCTOR = "^";
	private static final String UNICODE_LAMBDA_FUNCTOR = "\u03BB"; // lambda; also 1D6CC

	protected Variable variable;
	protected Formula formula;

	public FOLLambdaAbstract(String variable, String formula) {
		this(new Variable(variable), Formula.create(formula));
	}

	public FOLLambdaAbstract(Variable variable, Formula formula) {
		this.variable = variable;
		getVariables().add(variable);
		this.formula = formula;
		getVariables().addAll(formula.getVariables());
	}

	public FOLLambdaAbstract(FOLLambdaAbstract other) {
		this((Variable) other.variable.clone(), other.formula);
	}

	/**
	 * @return the body being abstracted over
	 */
	public Formula getFormula() {
		return this.formula;
	}

	/**
	 * @return the abstracted variable
	 */
	public Variable getVariable() {
		return this.variable;
	}

	/**
	 * @param argument
	 * @return the result of beta-reduction with argument
	 */
	public Formula betaReduce(Formula argument) {
		if (argument instanceof FOLLambdaAbstract) {
			throw new RuntimeException("Not allowing higher-order argument: " + argument);
		}

		return formula.substitute(variable, argument);
	}

	/**
	 * @return the core formula within the lambda operators
	 */
	public Formula getCore() {
		if (formula instanceof FOLLambdaAbstract) {
			return ((FOLLambdaAbstract) formula).getCore();
		} else {
			return formula;
		}
	}

	public Formula replaceCore(Formula f) {
		if (formula instanceof FOLLambdaAbstract) {
			return new FOLLambdaAbstract(this.variable, ((FOLLambdaAbstract) formula).replaceCore(f));
		} else {
			return new FOLLambdaAbstract(this.variable, f);
		}
	}

	/**
	 * @return number of abstracted variables
	 */
	public int numVariables() {
		if (!(formula instanceof FOLLambdaAbstract))
			return 1;
		else
			return ((FOLLambdaAbstract) formula).numVariables() + 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#conjoin(qmul.ds.formula.Formula)
	 */
	public Formula conjoin(Formula f) {
		if (!(f instanceof FOLLambdaAbstract)) {

			return this.replaceCore(getCore().conjoin(f));
		}

		FOLLambdaAbstract other = (FOLLambdaAbstract) f;
		if (numVariables() != other.numVariables()) {
			throw new IllegalArgumentException("Tried to conjoin:" + this + " with :" + f
					+ "\nCan only conjoin a Lambda Abstract with another one of the same type.");
		}

		FOLLambdaAbstract otherReplaced = other.substitute(other.variable, this.variable);
		return new FOLLambdaAbstract(this.variable, this.formula.conjoin(otherReplaced.formula));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#substitute(qmul.ds.formula.Formula, qmul.ds.formula.Formula)
	 */
	@Override
	public FOLLambdaAbstract substitute(Formula f1, Formula f2) {
		if (this.equals(f1)) {
			return (FOLLambdaAbstract) f2;
		}
		return new FOLLambdaAbstract((Variable) variable.substitute(f1, f2), formula.substitute(f1, f2));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#subsumes(qmul.ds.formula.Formula, java.util.HashMap)
	 */
	@Override
	protected boolean subsumesMapped(Formula other, HashMap<Variable, Variable> map) {
		if (!(other instanceof FOLLambdaAbstract)) {
			return false;
		}
		FOLLambdaAbstract eps = (FOLLambdaAbstract) other;
		if (variable.subsumesBasic(eps.variable) && formula.subsumesBasic(eps.formula)) {
			return true;
		}
		return (variable.subsumesMapped(eps.variable, map) && formula.subsumesMapped(eps.formula, map));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#evaluate()
	 */
	public Formula evaluate() {
		return new FOLLambdaAbstract(this.variable, formula.evaluate());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((formula == null) ? 0 : formula.hashCode());
		result = prime * result + ((variable == null) ? 0 : variable.hashCode());
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
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FOLLambdaAbstract other = (FOLLambdaAbstract) obj;
		if (formula == null) {
			if (other.formula != null)
				return false;
		} else if (!formula.equals(other.formula))
			return false;
		if (variable == null) {
			if (other.variable != null)
				return false;
		} else if (!variable.equals(other.variable))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#clone()
	 */
	public FOLLambdaAbstract clone() {
		return new FOLLambdaAbstract(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#toUnicodeString()
	 */
	@Override
	public String toUnicodeString() {
		// return super.toUnicodeString().replaceAll("(\\w)" +
		// Pattern.quote(LAMBDA_FUNCTOR),
		// UNICODE_LAMBDA_FUNCTOR + "$1.");
		return UNICODE_LAMBDA_FUNCTOR + variable + "." + formula.toUnicodeString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return variable + LAMBDA_FUNCTOR + formula;
	}

	public static void main(String a[]) {
		Formula f1 = Formula.create("x^y^like(x,y)");
		Formula f2 = Formula.create("z^z2^dislike(z2,z,z2)");
		Formula v1 = Formula.create("x1");
		Formula v2 = Formula.create("y2");
		System.out.println(f1);
		System.out.println(f1.getVariables());

		Formula reduced = ((FOLLambdaAbstract) f1).betaReduce(v1);
		System.out.println(reduced);
		System.out.println(reduced.getVariables());
		reduced = ((FOLLambdaAbstract) reduced).betaReduce(v2);
		System.out.println(reduced);
		System.out.println(reduced.getVariables());

		Formula conjoined = f1.conjoin(f2);
		System.out.println(conjoined);
		System.out.println(conjoined.getVariables());

		// TTRRecordType f3 = new TTRRecordType("e1", "e5");
		// f3.add(new TTRLabel("r"), Formula.create("reftime"));
		// f3.add(new TTRLabel("p"), Formula.create("e overlap r & r=now"));
		// f3.add(new TTRLabel("e"), Formula.create("e = e1"));
		// f3.setHead(new TTRLabel("e"));
		//
		// logger.debug(f3);
		//
		// TTRRecordType f4 = new TTRRecordType("x2", "add");
		// f4.add(new TTRLabel("e1"), Formula.create("e4"));
		// f4.add(new TTRLabel("r"), Formula.create("reftime2"));
		// f4.add(new TTRLabel("p3"), Formula.create("e overlap r & now<e<r"));
		// f4.add(new TTRLabel("e"), Formula.create("e = e1"));
		// f4.add(new TTRLabel("x"), Formula.create("add"));
		// f4.add(new TTRLabel("p1"), Formula.create("go_to(e,x)"));
		// f4.add(new TTRLabel("e2"), Formula.create("e4"));
		// f4.add(new TTRLabel("x1"), Formula.create("unknown"));
		// f4.add(new TTRLabel("p2"), Formula.create("loc(e1,x1)"));
		// f4.add(new TTRLabel("p"), Formula.create("want(Y,x2,p1)"));
		// f4.setHead(new TTRLabel("p"));
		// LambdaAbstract function = new LambdaAbstract("Y", f4);
		//
		// logger.debug("applying betareduction from " + function.toString() + "\n to" + f3.toString());
		//
		// Formula reduced = function.betaReduce(f3);
		//
		// logger.debug("betaredction result = " + reduced.toString());

	}
	
	@Override
	public int toUniqueInt() {
		throw new UnsupportedOperationException("Shouldn't need to turn lambda abstracts to unique integers. for now.");
		
	}

}