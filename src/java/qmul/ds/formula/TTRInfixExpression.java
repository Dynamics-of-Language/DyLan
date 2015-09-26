package qmul.ds.formula;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import qmul.ds.tree.Tree;
import qmul.ds.type.BasicType;
import edu.stanford.nlp.util.Pair;

public class TTRInfixExpression extends TTRFormula {

	protected static Logger logger = Logger.getLogger(TTRInfixExpression.class);
	private static final long serialVersionUID = 1L;
	public static final Predicate ASYM_MERGE_FUNCTOR = new Predicate("++");
	public static final Predicate TTR_DISJUNTION_FUNCTOR = new Predicate("||");
	public static final Predicate RESTRICTOR_CONJ_FUNCTOR = new Predicate("+restr");

	public static final Pattern REC_TYPE_FUNCTOR_PATTERN = Pattern.compile("\\s"
			+ Pattern.quote(ASYM_MERGE_FUNCTOR.toString()) + "\\s" + "|" + "\\s"
			+ Pattern.quote(TTR_DISJUNTION_FUNCTOR.toString()) + "\\s" + "|" + "\\s"
			+ Pattern.quote(RESTRICTOR_CONJ_FUNCTOR.toString() + "\\s"));

	protected Predicate predicate;
	protected Formula arg1;
	protected Formula arg2;

	public TTRInfixExpression(Predicate p, Formula arg1, Formula arg2) {
		this.predicate = p;
		this.arg1 = arg1;
		this.arg2 = arg2;
		this.getVariables().addAll(arg1.getVariables());
		this.getVariables().addAll(arg2.getVariables());
	}

	public TTRInfixExpression(Formula arg1, Formula arg2) {
		this.arg1 = arg1;
		this.arg2 = arg2;
		this.getVariables().addAll(arg1.getVariables());
		this.getVariables().addAll(arg2.getVariables());
	}

	public TTRInfixExpression(TTRInfixExpression f) {
		this(f.predicate, f.arg1.clone(), f.arg2.clone());
	}

	public static TTRInfixExpression parse(String exp1) {

		String exp = exp1.trim();
		if (!exp.startsWith("(") || !exp.endsWith(")"))
			return null;
		Pair<Integer, Predicate> predPair = findFunctorIndex(exp);
		if (predPair == null)
			return null;
		String arg1S = exp.substring(1, predPair.first).trim();
		String arg2S = exp.substring(predPair.first() + predPair.second().getName().length() + 2, exp.length() - 1)
				.trim();

		Formula arg1 = Formula.create(arg1S);
		Formula arg2 = Formula.create(arg2S);

		if (arg1 == null || arg2 == null)
			return null;
		if (predPair.second.equals(TTR_DISJUNTION_FUNCTOR))
			return new DisjunctiveType(arg1, arg2);
		return new TTRInfixExpression(predPair.second(), arg1, arg2);

	}

	private static Pair<Integer, Predicate> findFunctorIndex(String s) {
		int depth = 0;
		int recTypeDepth = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == ')')
				depth--;
			else if (s.charAt(i) == '(')
				depth++;
			else if (s.charAt(i) == ']')
				recTypeDepth--;
			else if (s.charAt(i) == '[')
				recTypeDepth++;

			Matcher m = REC_TYPE_FUNCTOR_PATTERN.matcher(s.substring(i));
			if (recTypeDepth == 0 && depth == 1 && m.find() && m.start() == 0) {
				return new Pair<Integer, Predicate>(new Integer(i), new Predicate(m.group().trim()));
			}

		}
		return null;
	}

	/**
	 * @return the predicate
	 */
	public Predicate getPredicate() {
		return predicate;
	}

	/**
	 * @return the second argument
	 */
	public Formula getArg1() {
		return arg1;
	}

	/**
	 * @return the first argument
	 */
	public Formula getArg2() {
		return arg2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.InfixPredicateArgumentFormula#evaluate()
	 */
	public TTRFormula evaluate() {

		Formula eval1 = this.arg1.evaluate();
		Formula eval2 = this.arg2.evaluate();

		if (eval1 instanceof TTRInfixExpression || eval1 instanceof Variable || eval2 instanceof Variable
				|| eval2 instanceof TTRInfixExpression) {
			return new TTRInfixExpression(predicate, eval1, eval2);
		}

		TTRFormula f1 = (TTRFormula) eval1;
		TTRFormula f2 = (TTRFormula) eval2;
		TTRFormula result = null;

		if (this.predicate.equals(ASYM_MERGE_FUNCTOR))
			result = f1.asymmetricMerge(f2);
		else if (this.predicate.equals(TTR_DISJUNTION_FUNCTOR))
			return new TTRInfixExpression(TTR_DISJUNTION_FUNCTOR, f1, f2);
		// add else ifs for other functor types (e.g. merge, disjunction, etc etc.)

		if (result != null)
			return result;

		return this;
	}

	@Override
	public TTRFormula freshenVars(Tree t) {
		return new TTRInfixExpression(this.predicate, this.arg1.freshenVars(t), this.arg2.freshenVars(t));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.mp.Formula#substitute(qmul.ds.formula.mp.Formula, qmul.ds.formula.mp.Formula)
	 */
	@Override
	public TTRFormula substitute(Formula f1, Formula f2) {

		return new TTRInfixExpression(this.predicate, arg1.substitute(f1, f2), arg2.substitute(f1, f2));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#setParentRecType(qmul.ds.formula.TTRRecordType)
	 */
	public void setParentRecType(TTRRecordType r) {

		//System.out.println("setting parent rectype of " + this + " to " + r);
		this.parentRecType = r;
		predicate.parentRecType = r;

		arg1.setParentRecType(r);
		arg2.setParentRecType(r);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#subsumes(qmul.ds.formula.Formula, java.util.HashMap)
	 */
	@Override
	protected boolean subsumesMapped(Formula other1, HashMap<Variable, Variable> map) {
		if (!(other1 instanceof TTRFormula)) {
			return false;
		}
		if (!(other1 instanceof TTRInfixExpression))
			return false;
		TTRInfixExpression other = (TTRInfixExpression) other1;

		return this.predicate.equals(other.predicate) && this.arg1.subsumesMapped(other.arg1, map)
				&& this.arg2.subsumesMapped(other.arg2, map);
	}
		
////		TTRFormula otherEval = ((TTRFormula) other1).evaluate();
////		TTRFormula thisEval = evaluate();
////		if (otherEval instanceof TTRRecordType) {
////			if (!(thisEval instanceof TTRInfixExpression))
////				return thisEval.subsumesMapped(otherEval, map);
////
////			TTRInfixExpression thisInfix = (TTRInfixExpression) thisEval;
////			if (!thisInfix.predicate.equals(DISJUNTION_FUNCOR))
////				throw new UnsupportedOperationException();
////			// this formula is evaluated to a disjunctive TTR expression
////			// check first argument, but leaving map intact by taking a copy
////			HashMap<Variable, Variable> copy = new HashMap<Variable, Variable>(map);
////			if (arg1.subsumesMapped(otherEval, copy)) {
////				map.clear();
////				map.putAll(copy);
////				return true;
////			}
////
////			return arg2.subsumesMapped(otherEval, map);
//
//		}
//		// checking this, we know that the predicate of the this formula must have been disjunction
//		// and from before we know that the other formula is not a record type
//		if (thisEval instanceof TTRInfixExpression)
//			return false;
//
//		// TODO: this is not covering all the cases.... but we won't need it for TTR induction at the moment...
//		return thisEval.subsumesMapped(otherEval, map);
	

	@Override
	public TTRFormula asymmetricMerge(TTRFormula ttrf) {
		if (ttrf instanceof TTRLambdaAbstract) {
			TTRLambdaAbstract abs = (TTRLambdaAbstract) ttrf;
			return abs.replaceCore(this.asymmetricMerge(abs.getCore()));
		}

		return new TTRInfixExpression(ASYM_MERGE_FUNCTOR, this, ttrf);
	}

	@Override
	public TTRFormula clone() {
		// TODO Auto-generated method stub
		return new TTRInfixExpression(this);
	}

	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((arg1 == null) ? 0 : arg1.hashCode());
		result = prime * result + ((arg2 == null) ? 0 : arg2.hashCode());
		result = prime * result + ((predicate == null) ? 0 : predicate.hashCode());
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
		TTRInfixExpression other = (TTRInfixExpression) obj;
		if (arg1 == null) {
			if (other.arg1 != null)
				return false;
		} else if (arg2 == null) {
			if (other.arg2 != null)
				return false;
		} else if (!arg1.equals(other.arg1))
			return false;
		else if (!arg2.equals(other.arg2))
			return false;

		if (predicate == null) {
			if (other.predicate != null)
				return false;
		} else if (!predicate.equals(other.predicate))
			return false;

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.PredicateArgumentFormula#toString()
	 */
	public String toString() {
		String result = "(" + this.arg1 + " " + predicate + " " + this.arg2 + ")";
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#toUnicodeString()
	 */
	public String toUnicodeString() {
		String result = "(" + this.arg1.toUnicodeString() + " " + predicate.toUnicodeString() + " "
				+ this.arg2.toUnicodeString() + ")";
		return result;
	}

	public TTRFormula instantiate() {
		Formula arg1I = arg1.instantiate();
		Formula arg2I = arg2.instantiate();
		TTRInfixExpression instance = new TTRInfixExpression(predicate, arg1I, arg2I);
		return instance;
	}

	public static void main(String a[]) {
		TTRInfixExpression f = TTRInfixExpression.parse("(R ++ [p==here(R.head):t])");

		System.out.println(f);

		// TTRRecordType rec=TTRRecordType.parse("[y==mary:e|e5:es|p5==run(e5):t|p10==obj(e5,y):t]");
		// HashMap<Variable, Variable> map=new HashMap<Variable, Variable>();
		// System.out.println("Result="+f.subsumesMapped(rec, map)+"::"+map);
	}

	/**
	 * this currently assumes that there's a single record type core to the infix expression
	 * 
	 */
	public List<Pair<TTRRecordType, TTRLambdaAbstract>> getAbstractions(BasicType dsType, int newVarSuffix) {
		List<Pair<TTRRecordType, TTRLambdaAbstract>> result = new ArrayList<Pair<TTRRecordType, TTRLambdaAbstract>>();

		List<TTRRecordType> types = getTypes();
		if (types.size() > 1)
			throw new UnsupportedOperationException();

		TTRRecordType core = types.get(0);
		List<Pair<TTRRecordType, TTRLambdaAbstract>> coreAbstractions = core.getAbstractions(dsType, newVarSuffix);
		for (Pair<TTRRecordType, TTRLambdaAbstract> pair : coreAbstractions) {
			TTRInfixExpression infix = new TTRInfixExpression(this);
			TTRRecordType newCore = infix.getTypes().get(0);
			if (pair.second.getCore() instanceof TTRRecordType)
				newCore.replaceContent((TTRRecordType) pair.second.getCore());
			else
				newCore.replaceContent(pair.second.getCore().getTypes().get(0));

			Variable v = new Variable("R" + newVarSuffix);

			TTRFormula coreFinal = new TTRInfixExpression(TTRInfixExpression.ASYM_MERGE_FUNCTOR, v, infix);
			TTRLambdaAbstract lambdaAbs = new TTRLambdaAbstract(v, coreFinal);
			// Pair<TTRRecordType, TTRLambdaAbstract> abs=new Pair<TTRRecordType, TTRLambdaAbstract>(argument,
			// lambdaAbs);

			result.add(new Pair<TTRRecordType, TTRLambdaAbstract>(pair.first(), lambdaAbs));

		}

		return result;
	}

	protected List<TTRRecordType> getTypes() {
		List<TTRRecordType> result = new ArrayList<TTRRecordType>();

		if (arg1 instanceof Variable && arg2 instanceof TTRFormula)
			result.addAll(((TTRFormula) arg2).getTypes());
		else if (arg2 instanceof Variable && arg1 instanceof TTRFormula)
			result.addAll(((TTRFormula) arg1).getTypes());
		else if (arg1 instanceof TTRFormula && arg2 instanceof TTRFormula) {
			result.addAll(((TTRFormula) arg1).getTypes());
			result.addAll(((TTRFormula) arg2).getTypes());
		}
		return result;
	}

	@Override
	public TTRField getHeadField() {
		if (!this.predicate.equals(ASYM_MERGE_FUNCTOR))
			return null;

		if (this.arg2 instanceof Variable)
			return null;
		if (this.arg2 instanceof TTRRecordType)
			return ((TTRRecordType) this.arg2).getHeadField();

		return ((TTRInfixExpression) this.arg2).getHeadField();
	}

	@Override
	public int toUniqueInt() {
		throw new UnsupportedOperationException("Shouldn't need to turn infix expressions to integers for now. They only appear in lambda abstracts");
		
		
	}

	@Override
	public TTRFormula asymmetricMergeSameType(TTRFormula f) {
		throw new UnsupportedOperationException();
	}
}
