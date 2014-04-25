package qmul.ds.formula;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.tree.BasicOperator;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.Requirement;
import qmul.ds.tree.label.TypeLabel;
import qmul.ds.type.BasicType;
import qmul.ds.type.ConstructedType;
import qmul.ds.type.DSType;
import edu.stanford.nlp.util.Pair;

/**
 * 
 * @author arash
 * 
 */
public class TTRLambdaAbstract extends TTRFormula implements LambdaAbstract {

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(TTRLambdaAbstract.class);

	protected static final String LAMBDA_FUNCTOR = "^";
	private static final String UNICODE_LAMBDA_FUNCTOR = "\u03BB"; // lambda; also 1D6CC

	/**
	 * @return the high-level (DS) type of the final field of the body
	 */
	public DSType getDSType() {
		return ((TTRRecordType) getFormula()).getDSType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#freshenVars(qmul.ds.tree.Tree)
	 */
	public TTRFormula freshenVars(Tree t) {
		return this.replaceCore(getCore().freshenVars(t));
	}

	protected Variable variable;
	protected TTRFormula formula;

	public TTRLambdaAbstract(String variable, String formula) {
		this(new Variable(variable), (TTRFormula) Formula.create(formula));
	}

	public TTRLambdaAbstract(Variable variable, TTRFormula formula) {
		this.variable = variable;
		getVariables().add(variable);
		this.formula = formula;
		getVariables().addAll(formula.getVariables());
	}

	public TTRLambdaAbstract(TTRLambdaAbstract other) {
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
	public TTRFormula betaReduce(Formula argument) {
		if (argument instanceof FOLLambdaAbstract) {
			throw new RuntimeException("Not allowing higher-order argument: " + argument);
		}

		return formula.substitute(variable, argument).evaluate();
	}

	/**
	 * @return the core formula within the lambda operators
	 */
	public TTRFormula getCore() {
		if (formula instanceof TTRLambdaAbstract) {
			return ((TTRLambdaAbstract) formula).getCore();
		} else {
			return formula;
		}
	}

	public TTRFormula replaceCore(TTRFormula f) {
		if (formula instanceof TTRLambdaAbstract) {
			return new TTRLambdaAbstract(this.variable, ((TTRLambdaAbstract) formula).replaceCore(f));
		} else {
			return new TTRLambdaAbstract(this.variable, f);
		}
	}

	/**
	 * @return number of abstracted variables
	 */
	public int numVariables() {
		if (!(formula instanceof TTRLambdaAbstract))
			return 1;
		else
			return ((TTRLambdaAbstract) formula).numVariables() + 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#substitute(qmul.ds.formula.Formula, qmul.ds.formula.Formula)
	 */
	@Override
	public TTRFormula substitute(Formula f1, Formula f2) {
		if (this.equals(f1)) {
			return (TTRLambdaAbstract) f2;
		}
		return new TTRLambdaAbstract((Variable) variable.substitute(f1, f2), formula.substitute(f1, f2));
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
		TTRLambdaAbstract eps = (TTRLambdaAbstract) other;
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
	public TTRFormula evaluate() {
		return new TTRLambdaAbstract(this.variable, formula.evaluate());
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
		TTRLambdaAbstract other = (TTRLambdaAbstract) obj;
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
	public TTRLambdaAbstract clone() {
		return new TTRLambdaAbstract(this);
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
		return UNICODE_LAMBDA_FUNCTOR + variable.toUnicodeString() + "." + formula.toUnicodeString();
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

	@Override
	public TTRFormula asymmetricMerge(TTRFormula rt) {
		if (rt == null) {
			logger.warn("trying to merge a null formula with ttr lambda abstract:" + this);
			return this;
		}

		return replaceCore(getCore().asymmetricMerge(rt));
	}

	@Override
	public List<Pair<TTRRecordType, TTRLambdaAbstract>> getAbstractions(BasicType dsType, int newVarSuffix) {
		List<Pair<TTRRecordType, TTRLambdaAbstract>> coreAbstractions = getCore().getAbstractions(dsType,
				this.numVariables() + 1);
		List<Pair<TTRRecordType, TTRLambdaAbstract>> result = new ArrayList<Pair<TTRRecordType, TTRLambdaAbstract>>();
		for (Pair<TTRRecordType, TTRLambdaAbstract> pair : coreAbstractions) {
			TTRLambdaAbstract newAbs = new TTRLambdaAbstract(pair.second().getVariable(), this.replaceCore(pair
					.second().getCore()));
			result.add(new Pair<TTRRecordType, TTRLambdaAbstract>(pair.first(), newAbs));

		}
		return result;
	}

	/*
	 * public List<Tree> getAbstractions(DSType funcType) { NodeAddress root=new NodeAddress(); for(int
	 * i=0;i<numVariables();i++) root=root.down1();
	 * 
	 * return getAbstractions(funcType, root); }
	 */

	@Override
	protected List<TTRRecordType> getTypes() {
		List<TTRRecordType> list = new ArrayList<TTRRecordType>();
		list.addAll(getCore().getTypes());
		return list;
	}

	@Override
	public TTRField getHeadField() {
		
		return getCore().getHeadField();
	}
	
	@Override
	public int toUniqueInt() {
		throw new UnsupportedOperationException("Shouldn't need to turn lambda abstracts to integers for now. They don't appear within record types.");
		
	}

}
