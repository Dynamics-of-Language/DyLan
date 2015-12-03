/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.formula;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import qmul.ds.action.boundvariable.BoundFormulaVariable;
import qmul.ds.action.meta.MetaFormula;
import qmul.ds.action.meta.MetaTTRRecordType;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.ExistentialLabelConjunction;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.LabelFactory;

/**
 * An abstract class for semantic formulae as used in {@link FormulaLabel}s
 * 
 * @author mpurver
 */
public abstract class Formula implements Serializable {

	protected static Logger logger = Logger.getLogger(Formula.class);

	private static final long serialVersionUID = 1L;

	public static final String EPSILON_FUNCTOR = "eps";
	public static final String UNICODE_EPSILON_FUNCTOR = "\u03B5"; // epsilon; also 03F5, 025B, 1D6C6
	public static final String TAU_FUNCTOR = "tau";
	public static final String UNICODE_TAU_FUNCTOR = "\u03C4"; // tau; also 1D6D5
	public static final String IOTA_FUNCTOR = "iota";
	public static final String UNICODE_IOTA_FUNCTOR = "\u0269"; // iota; also 03B9, 1D6CA
	public static final String ETA_FUNCTOR = "eta";
	public static final String UNICODE_ETA_FUNCTOR = "\u03B7"; // eta; also 1D6C8
	public static final String EPSILON_FUNCTORS = "(eps|tau|iota|notexists)";
	public static final String CONJUNCTION_OPERATOR = "&";
	public static final String DISJUNCTION_OPERATOR = "\\|";
	public static final String UNICODE_CONJUNCTION_OPERATOR = "\u2227"; // logical and; also 22C0
	public static final String UNICODE_SUBSET_OPERATOR = "\u2286";
	public static final String UNICODE_OVERLAP_OPERATOR = "\u229D";
	public static final String OVERLAP_OPERATOR = "overlap";
	public static final String SUBSET_OPERATOR = "subset";
	public static final String BINARY_FOL_OPERATOR = "(" + CONJUNCTION_OPERATOR + "|" + DISJUNCTION_OPERATOR + ")";
	public static final Pattern VARIABLE_PATTERN = Pattern.compile("[a-zR&&[^i]][0-9]*|reftime|head"); // bound/free Formula
																									// variable
	public static final Pattern METAVARIABLE_PATTERN = Pattern.compile("[S-U]"); // Formula metavariable
	public static final Pattern REC_METAVARIABLE_PATTERN = Pattern.compile("REC\\d*");
	public static final Pattern LAMBDA_ABSTRACT_PATTERN = Pattern.compile("(" + VARIABLE_PATTERN + ")"
			+ Pattern.quote(FOLLambdaAbstract.LAMBDA_FUNCTOR) + "(.*)");

	public static final Pattern FRESH_VARIABLE_PATTERN = Pattern.compile("VAR");
	public static final Pattern FRESH_EVENT_VARIABLE_PATTERN = Pattern.compile("EVENTVAR");
	public static final Pattern FRESH_PROPOSITION_VARIABLE_PATTERN = Pattern.compile("PROPVAR");
	public static final Pattern CN_ORDERED_PAIR_PATTERN = Pattern.compile("\\s*(" + VARIABLE_PATTERN
			+ ")\\s*,\\s*(.+)\\s*");
	public static final Pattern EPSILON_TERM_PATTERN = Pattern.compile("\\(" + EPSILON_FUNCTORS + "\\s*,\\s*(.+)\\)");
	public static final Pattern PREDICATE_PATTERN = Pattern.compile("[a-z][a-z][a-z_0-9]*|<|>|="); // 2 alpha chars or
																									// more
	public static final Pattern PRED_ARG_PATTERN = Pattern.compile("([a-z][a-z][a-z_0-9]*)\\((.+)\\)");

	public static String ATOMIC_FORMULA_PATTERN = "[a-z]+[a-z_0-9]*";

	private HashSet<Variable> variables = new HashSet<Variable>();
	protected TTRRecordType parentRecType = null;// the record type in one of whose fields this formula is directly
													// embedded.... all Formulas have it....
													// null if this is a root record type, or if we are using
													// Epsilon-Calculus ds.

	/**
	 * @param string
	 *            a formula spec as used in lexicon specs e.g. john, x^y^like(x,y)
	 * @return a {@link AtomicFormula} for atomic strings, {@link FOLLambdaAbstract} otherwise
	 */
	public static Formula create(String string) {
		return create(string, false);
	}

	public TTRRecordType getParentRecType() {
		return this.parentRecType;
	}

	public void setParentRecType(TTRRecordType r) {
		this.parentRecType = r;
	}

	public List<TTRPath> getTTRPaths() {
		return new ArrayList<TTRPath>();
	}

	/**
	 * @param string
	 *            a formula spec as used in lexicon specs e.g. john, X^Y^like(X,Y)
	 * @param inExConj
	 *            whether we're inside an {@link ExistentialLabelConjunction} - a temporary (I hope) hack to allow
	 *            existentially quantified label variables to be written lower-case letters, the same as standard
	 *            {@link Formula} variables
	 * @return a {@link AtomicFormula} for atomic strings, {@link FOLLambdaAbstract} otherwise
	 */
	public static Formula create(String string1, boolean inExConj) {
		logger.debug("creating formula from string: " + string1);
		String string = string1.trim();
		Formula v = null;
		// standard Formula variable

		if (!inExConj && VARIABLE_PATTERN.matcher(string).matches()) {
			v = new Variable(string);
			
		}
		// lexical/computational action rule metavariable (as in e.g. IF fo(X)
		// THEN ...)
		else if (string.matches("^" + LabelFactory.METAVARIABLE_PATTERN + "$")) {
			v = MetaFormula.get(string);

		} //else if (string.matches("^" + FRESHPUT_METAVARIABLE_PATTERN + "$")) {
		//	v = MetaFormula.get(string);

	//	} commented out because of redundancy.
		else if (string.matches("^" + REC_METAVARIABLE_PATTERN + "$")) {
			v = MetaTTRRecordType.get(string);
		}
		// formula bound variable as used in e.g. existential labels, e.g.
		// Ex.fo(x)
		else if (inExConj && string.matches("^" + LabelFactory.VAR_PATTERN + "$")) {
			v = new BoundFormulaVariable(string);

		}
		// formula metavariable (as in e.g. put(fo(A)))
		else if (string.matches("^" + METAVARIABLE_PATTERN + "$")) {
			v = FormulaMetavariable.get(string);

		}
		// fresh (bound) formula variable of type es
		else if (string.matches("^" + FRESH_EVENT_VARIABLE_PATTERN + "$")) {
			v = Variable.getFreshEventVariable();
		}
		// fresh (bound) formula variable of type e
		else if (string.matches("^" + FRESH_VARIABLE_PATTERN + "$")) {
			v = Variable.getFreshEntityVariable();
		}
		// fresh (bound) formula variable of type t
		else if (string.matches("^" + FRESH_PROPOSITION_VARIABLE_PATTERN + "$")) {
			v = Variable.getFreshPropositionVariable();
		}

		if (v != null)
		{
			logger.debug("Created:"+v+":"+v.getClass());
			return v;
		}
		TTRPath path = TTRPath.parse(string);
		if (path != null) {

			return path;
		}
		TTRRecordType rt = TTRRecordType.parse(string);
		if (rt != null) {
			logger.debug("created record type, " + rt + ", from: " + string);

			return rt;
		}

		Matcher m = LAMBDA_ABSTRACT_PATTERN.matcher(string);
		if (m.matches()) {

			Formula la;
			if (m.group(1).trim().matches(TTRPath.REC_TYPE_NAME_PATTERN))
				la = new TTRLambdaAbstract(m.group(1), m.group(2));
			else
				la = new FOLLambdaAbstract(m.group(1), m.group(2));

			return la;
		}

		// epsilon term
		m = EPSILON_TERM_PATTERN.matcher(string);
		if (m.matches()) {
			EpsilonTerm term = new EpsilonTerm(m.group(1), create(m.group(2)));
			return term;
		}

		// cn ordered pair
		CNOrderedPair pair = CNOrderedPair.parse(string);
		if (pair != null)
			return pair;

		Formula f = TTRInfixExpression.parse(string);
		if (f != null)
			return f;

		f = InfixPredicateArgumentFormula.parse(string);
		if (f != null)
			return f;
		
		f=TTRInfixExpression.parse(string);
		if(f!=null)
			return f;
		
		// predicate formula
		m = PRED_ARG_PATTERN.matcher(string);
		if (m.matches()) {
			Predicate p = new Predicate(m.group(1));
			String[] args = m.group(2).split(",");
			Formula[] fargs = new Formula[args.length];
			for (int i = 0; i < args.length; i++) {
				fargs[i] = create(args[i]);
			}
			f = new PredicateArgumentFormula(p, fargs);

			return f;
		}
		if (string.matches(ATOMIC_FORMULA_PATTERN))
			return new AtomicFormula(string);
		logger.error("could not parse formula string:" + string);
		return null;

	}
	/**
	 * true by default... variables will return false.. also record types without any manifest content on the head label 
	 * will return false... e.g. [x:e|head:x] will return false... while [x:e|head:x|p==man(x):t] or [x==john:e|head:x] will not.
	 * @return
	 */
	public boolean hasManifestContent()
	{
		return true;
		
	}
	/**
	 * @return the set of variables (free or bound) involved
	 */
	protected Set<Variable> getVariables() {
		return variables;
	}

	/**
	 * @param f1
	 * @param f2
	 * @return a new {@link Formula} resulting from substituting sub-formula f1 by f2 in this {@link Formula}.
	 */
	public abstract Formula substitute(Formula f1, Formula f2);

	/**
	 * Evaluates the formula, e.g. if it is an expression, such as 1+2, it evaluates to 3... by default just return the
	 * formula itself..... for paths this will return the pointed type. For asymmetric merge it returns the resulting
	 * rec type.. for epsilon terms, perhaps it will return the chosen entity itself (with the restrictor having been
	 * checked etc..).... for propositions, well it should just return true or false.....
	 * 
	 * TODO: this should take a context argument to evaluate the formula against... but for now this is enough...
	 * 
	 * @return
	 */
	public Formula evaluate() {
		return this;
	}

	/**
	 * @param f
	 * @return a new {@link Formula} which is the conjunction of this {@link Formula} with f
	 */
	public Formula conjoin(Formula f) {
		// by default just return the conjunction
		// individual formula subclasses should override this
		return new InfixPredicateArgumentFormula(new Predicate(CONJUNCTION_OPERATOR), this, f);
	}

	/**
	 * to be overridden by individual formula classes. The method is to return the same formula, with all variables
	 * replaced such that they are all unique with respect to tree t.
	 * 
	 * For now only TTR related classes override this method. TODO: do it for all classes
	 * 
	 * @param t
	 * @return
	 */
	public Formula freshenVars(Tree t) {
		return this;
	}

	/**
	 * @return an instantiated version of this {@link Formula}, with all meta-elements replaced by their values. By
	 *         default, just return this {@link Formula} unchanged. This will be overridden by {@link MetaFormula}e and
	 *         the like
	 */
	public Formula instantiate() {
		return this;
	}

	/**
	 * @param other
	 * @return true if this subsumes other, false otherwise. Don't override this, override subsumesBasic and/or
	 *         subsumesMapped
	 */
	public final boolean subsumes(Formula other) {
		if (this.toString().equals(other.toString())) {
			return true;
		}
		if (this.subsumesBasic(other)) {
			return true;
		}
		return subsumesMapped(other, new HashMap<Variable, Variable>());
	}

	/**
	 * A quick check without bothering with recursion & variable substitution, if available. By default, an equality
	 * check
	 * 
	 * @param other
	 * @return true if this subsumes other, false otherwise
	 */
	protected boolean subsumesBasic(Formula other) {
		return this.equals(other);
	}

	public Dimension getDimensionsWhenDrawn(Graphics2D g2) {
		FontMetrics metrics = g2.getFontMetrics();
		int height = metrics.getHeight();
		// g2.drawString(toUnicodeString(), x, y);
		Dimension d = new Dimension();

		d.setSize(metrics.stringWidth(toUnicodeString()), height);
		return d;

	}

	public Dimension draw(Graphics2D g2, float x, float y) {
		FontMetrics metrics = g2.getFontMetrics();
		int height = metrics.getHeight();
		g2.drawString(toUnicodeString(), x, y);
		Dimension d = new Dimension();

		d.setSize(metrics.stringWidth(toUnicodeString()), height);
		return d;
	}

	/**
	 * @param other
	 * @param map
	 * @return true if this subsumes other, modulo the variable substitutions in map - possibly adding to map if a new
	 *         substitution is needed. Can assume that subsumesBasic has been called first and failed (so the default is
	 *         for this to fail)
	 */
	protected boolean subsumesMapped(Formula other, HashMap<Variable, Variable> map) {
		return false;
	}

	// protected boolean subsumes(List<Formula> l1, List<Formula> l2) {
	// // must be same length
	// if (l1.size() != l2.size()) {
	// return false;
	// }
	// // base case of recursion: empty list means we've checked everything
	// if (l1.isEmpty()) {
	// return true;
	// }
	// // check head, recurse
	// if (l1.get(0).subsumes(l2.get(0))) {
	// return subsumes(l1.subList(1, l1.size()), l2.subList(1, l2.size()));
	// }
	// // if head fails, fail
	// return false;
	// }

	/**
	 * @param l1
	 * @param l2
	 * @param map
	 * @return true if l1 subsumes l2 modulo the variable substitutions in map, optionally adding to map as new
	 *         substitutions are required. Calls subsumesBasic first, then subsumesMapped on failure, on each element in
	 *         turn
	 */
	protected boolean subsumesMapped(List<Formula> l1, List<Formula> l2, HashMap<Variable, Variable> map) {
		// must be same length
		if (l1.size() != l2.size()) {
			return false;
		}
		// base case of recursion: empty list means we've checked everything
		if (l1.isEmpty()) {
			return true;
		}
		// check head, recurse

		if (l1.get(0).subsumesBasic(l2.get(0)) || l1.get(0).subsumesMapped(l2.get(0), map)) {
			return subsumesMapped(l1.subList(1, l1.size()), l2.subList(1, l2.size()), map);
		}
		// if head fails, fail
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return toString().hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public abstract Formula clone();

	/**
	 * @return the string with special characters replaced by Unicode versions
	 */
	public String toUnicodeString() {
		String s = toString();
		s = s.replaceAll(EPSILON_FUNCTOR, UNICODE_EPSILON_FUNCTOR);
		s = s.replaceAll(TAU_FUNCTOR, UNICODE_TAU_FUNCTOR);
		s = s.replaceAll(IOTA_FUNCTOR, UNICODE_IOTA_FUNCTOR);
		s = s.replaceAll(ETA_FUNCTOR, UNICODE_ETA_FUNCTOR);
		s = s.replaceAll(CONJUNCTION_OPERATOR, UNICODE_CONJUNCTION_OPERATOR);
		s = s.replaceAll(SUBSET_OPERATOR, UNICODE_SUBSET_OPERATOR);
		s = s.replaceAll(OVERLAP_OPERATOR, UNICODE_OVERLAP_OPERATOR);

		return s;
	}

	public static void main(String a[]) {
		Label l = LabelFactory.create("Ex.(fo(x) & manifest(x))");
		System.out.println(l);

	}
	/**
	 * Convert this formula to a unique integer, but such that, a.toUniqueIntRespectSubsumption() = b.toUniqueIntRespectSubsumption()
	 * iff a.subsumes(b) && b.subsumes(a)
	 * 
	 * Used to translate Trees and Record types of TTR (effectively ParserTuples) to (atomic) states in an MDP. 
	 * @return Unique integer constructed from this formula.
	 */
	public abstract int toUniqueInt();
	

	

}
