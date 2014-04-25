package qmul.ds.formula;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.stanford.nlp.util.Pair;

public class InfixPredicateArgumentFormula extends PredicateArgumentFormula {

	/**
	 * A binary predicate argument formula where the predicate appears between the two arguments, e.g. conjunction,
	 * disjunction, subset, etc: a & b, a or b, a subset b....
	 * 
	 * @author arash
	 */
	private static final long serialVersionUID = 1L;
	protected static Logger logger = Logger.getLogger(InfixPredicateArgumentFormula.class);
	public static final Pattern FOL_FUNCTOR_PATTERN = Pattern.compile("&|\\||->");
	public static final Pattern MISC_INFIX_FUNCTOR_PATTERN = Pattern.compile(">|<|=|subset|overlap");

	public InfixPredicateArgumentFormula(Predicate p, Formula arg1, Formula arg2) {
		super(p);
		this.arguments = new ArrayList<Formula>();
		this.arguments.add(arg1);

		this.getVariables().addAll(arg1.getVariables());
		this.arguments.add(arg2);

		this.getVariables().addAll(arg2.getVariables());

	}

	public InfixPredicateArgumentFormula(PredicateArgumentFormula f) {
		super(f);
	}

	public boolean validArgTypes(Class<?> clazz) {
		if (!(clazz.isInstance(this.arguments.get(0)) || this.arguments.get(0) instanceof Variable || this.arguments
				.get(0) instanceof InfixPredicateArgumentFormula)) {
			return false;
		}
		if (!(clazz.isInstance(this.arguments.get(1)) || this.arguments.get(1) instanceof Variable || this.arguments
				.get(1) instanceof InfixPredicateArgumentFormula)) {
			return false;
		}
		return true;
	}

	public static InfixPredicateArgumentFormula parse(String exp1) {

		String exp = exp1.trim();
		if (!exp.startsWith("(") || !exp.endsWith(")"))
			return null;
		Pair<Integer, Predicate> predPair = findFunctorIndex(exp);
		if (predPair == null)
			return null;
		String arg1S = exp.substring(1, predPair.first).trim();
		String arg2S = exp.substring(predPair.first() + predPair.second().getName().length(), exp.length() - 1).trim();

		Formula arg1 = Formula.create(arg1S);
		Formula arg2 = Formula.create(arg2S);

		if (arg1 == null || arg2 == null)
			return null;

		if (predPair.second().getName().matches(FOL_FUNCTOR_PATTERN.toString())) {
			// once the classes are written for FOL formulae, this should be replaced by an instance of that class
			return new InfixPredicateArgumentFormula(predPair.second(), arg1, arg2);
		} else if (predPair.second().getName().matches(MISC_INFIX_FUNCTOR_PATTERN.toString())) {
			return new InfixPredicateArgumentFormula(predPair.second(), arg1, arg2);
		}
		return null;
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

			Matcher m = FOL_FUNCTOR_PATTERN.matcher(s.substring(i));
			if (recTypeDepth == 0 && depth == 1 && m.find() && m.start() == 0) {
				return new Pair<Integer, Predicate>(new Integer(i), new Predicate(m.group()));
			}
			m = MISC_INFIX_FUNCTOR_PATTERN.matcher(s.substring(i));
			if (recTypeDepth == 0 && depth == 1 && m.find() && m.start() == 0) {
				return new Pair<Integer, Predicate>(new Integer(i), new Predicate(m.group()));
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.mp.Formula#substitute(qmul.ds.formula.mp.Formula, qmul.ds.formula.mp.Formula)
	 */
	@Override
	public InfixPredicateArgumentFormula substitute(Formula f1, Formula f2) {
		Formula[] args = new Formula[getArity()];
		for (int i = 0; i < getArity(); i++) {
			args[i] = arguments.get(i).substitute(f1, f2);
		}
		return new InfixPredicateArgumentFormula(this.predicate, args[0], args[1]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.PredicateArgumentFormula#evaluate()
	 */
	public Formula evaluate() {
		Predicate p = new Predicate(predicate.name);
		ArrayList<Formula> arguments = new ArrayList<Formula>();
		for (Formula argument : this.arguments) {
			arguments.add(argument.evaluate());
		}
		InfixPredicateArgumentFormula result = new InfixPredicateArgumentFormula(p, arguments.get(0), arguments.get(1));
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.PredicateArgumentFormula#clone()
	 */
	public InfixPredicateArgumentFormula clone() {
		return new InfixPredicateArgumentFormula(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.PredicateArgumentFormula#toString()
	 */
	public String toString() {
		String result = "(" + this.arguments.get(0) + " " + predicate + " " + this.arguments.get(1) + ")";
		return result;
	}

	public static void main(String a[]) {
		InfixPredicateArgumentFormula f = InfixPredicateArgumentFormula.parse("([x:e|y==john:e] ++ [z==today:es])");
		System.out.println(f);
	}

}
