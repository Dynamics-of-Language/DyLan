package qmul.ds.formula;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import qmul.ds.action.meta.Meta;

/**
 * An epsilon calculus {@link Formula}
 * 
 * @author mpurver
 */
public class PredicateArgumentFormula extends Formula {

	private static final long serialVersionUID = 1L;

	protected Predicate predicate;
	protected ArrayList<Formula> arguments = new ArrayList<Formula>();

	public PredicateArgumentFormula(String predicate, Formula... arguments) {
		this(new Predicate(predicate), arguments);
	}

	public PredicateArgumentFormula(Predicate predicate, Formula... arguments) {
		this.predicate = predicate;
		this.arguments = new ArrayList<Formula>();
		for (int i = 0; i < arguments.length; i++) {
			this.arguments.add(arguments[i]);
			this.getVariables().addAll(arguments[i].getVariables());
		}
	}

	public PredicateArgumentFormula(Predicate predicate, ArrayList<Formula> arguments) {
		this.predicate = new Predicate(predicate.name);
		this.arguments = arguments;
	}

	public PredicateArgumentFormula(PredicateArgumentFormula f) {
		
		this.predicate = f.predicate.clone();

		for (Formula fo : f.arguments) {
			this.arguments.add(fo.clone());
		}
		getVariables().addAll(f.getVariables());
	}

	public List<TTRPath> getTTRPaths() {
		ArrayList<TTRPath> result = new ArrayList<TTRPath>();
		for (Formula arg : this.arguments) {
			result.addAll(arg.getTTRPaths());

		}
		return result;
	}

	/**
	 * @return the predicate
	 */
	public Predicate getPredicate() {
		return predicate;
	}

	/**
	 * @return the argument
	 */
	public ArrayList<Formula> getArguments() {
		return arguments;
	}

	/**
	 * @return the number of arguments
	 */
	public int getArity() {
		return arguments.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.mp.Formula#substitute(qmul.ds.formula.mp.Formula, qmul.ds.formula.mp.Formula)
	 */
	@Override
	public PredicateArgumentFormula substitute(Formula f1, Formula f2) {
		if (this.equals(f1)) {
			PredicateArgumentFormula result = (PredicateArgumentFormula) f2;
			return result;
		}
		Formula[] args = new Formula[getArity()];
		for (int i = 0; i < getArity(); i++) {
			args[i] = arguments.get(i).substitute(f1, f2 instanceof TTRLabel?new Variable((TTRLabel)f2):f2);
		}
		// check for higher-order lambda abstraction e.g. lambdaP.P(x)
		if (predicate.equals(f1) && (f2 instanceof LambdaAbstract)) {
			PredicateArgumentFormula result = (PredicateArgumentFormula) ((LambdaAbstract) f2).betaReduce(args[0]);
			return result;
		}
		PredicateArgumentFormula result = new PredicateArgumentFormula((Predicate) predicate.substitute(f1, f2), args);

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#setParentRecType(qmul.ds.formula.TTRRecordType)
	 */
	public void setParentRecType(TTRRecordType r) {
		this.parentRecType = r;
		predicate.parentRecType = r;
		for (Formula f : arguments) {
			f.setParentRecType(r);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#subsumes(qmul.ds.formula.Formula, java.util.HashMap)
	 */
	@Override
	protected boolean subsumesMapped(Formula other, HashMap<Variable, Variable> map) {
		if (!(other instanceof PredicateArgumentFormula)) {
			return false;
		}
		PredicateArgumentFormula paf = (PredicateArgumentFormula) other;
		if (!predicate.subsumesBasic(paf.predicate) && !predicate.subsumesMapped(paf.predicate, map)) {
			return false;
		}
		return subsumesMapped(arguments, paf.arguments, map);
	}
	
	public ArrayList<Meta<?>> getMetas()
	{
		ArrayList<Meta<?>> metas=new ArrayList<Meta<?>>();
		
		for(Formula f: arguments)
		{
			metas.addAll(f.getMetas());
		}
		metas.addAll(predicate.getMetas());
		return metas;
		
		
	}
	
	public boolean subsumesBasic(Formula f)
	{
		if (!(f instanceof PredicateArgumentFormula))
			return false;
		
		PredicateArgumentFormula other=(PredicateArgumentFormula)f;
		
		if (arguments.size()!=other.arguments.size())
			return false;
		
		if (!this.predicate.subsumesBasic(other.predicate))
			return false;
		
		for(int i=0;i<arguments.size();i++)
		{
			//System.out.println(arguments.get(i)+" and "+other.arguments.get(i)+"\n");
			
			if (!arguments.get(i).equals(other.arguments.get(i)))
				return false;
		}
		return true;
		
	}
	public static void main(String[] a)
	{
		PredicateArgumentFormula f=(PredicateArgumentFormula)Formula.create("class(L1,L2)");
		PredicateArgumentFormula f1=(PredicateArgumentFormula)Formula.create("class(x1,x2)");
		
		System.out.println(f.subsumesBasic(f1));
		System.out.println(f.instantiate().toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#evaluate()
	 */
	public Formula evaluate() {
		Predicate p = new Predicate(predicate.name);
		ArrayList<Formula> arguments = new ArrayList<Formula>();
		Set<Variable> newVars = new HashSet<Variable>();
		for (Formula argument : this.arguments) {
			if (argument instanceof TTRRelativePath)
				arguments.add(argument.clone());
			else
			{
				Formula argEval = argument.evaluate();
				arguments.add(argEval);
				newVars.addAll(argEval.getVariables());
			}

		}
		PredicateArgumentFormula result = new PredicateArgumentFormula(p, arguments);
		result.getVariables().addAll(newVars);
		return result;
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
		result = prime * result + ((arguments == null) ? 0 : arguments.hashCode());
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
		PredicateArgumentFormula other = (PredicateArgumentFormula) obj;
		if (arguments == null) {
			if (other.arguments != null)
				return false;
		} else if (!arguments.equals(other.arguments))
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
	 * @see qmul.ds.formula.Formula#clone()
	 */
	public PredicateArgumentFormula clone() {
		return new PredicateArgumentFormula(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (arguments.isEmpty()) {
			return predicate.toString();
		} else {
			String args = arguments.toString();
			return predicate + "(" + args.substring(1, args.length() - 1) + ")";
		}
	}

	public String toDebugString()
	{
		String s=predicate.toString()+"["+predicate.getClass()+"]";
		if (arguments.isEmpty()) {
			
			return s;
		} else {
			//String args = arguments.toString();
			s+="(";
			for(Formula argument:arguments)
				s+=argument.toDebugString()+"["+argument.getClass()+"],";
			s+=")";
			return s;
		}
		
	}
	

	@Override
	public int toUniqueInt() {
		int result=0;
		for(int i=0;i<arguments.size();i++)
		{
			Formula f=arguments.get(i);
			result+=(arguments.size()-i)*f.toUniqueInt();
		}
			
		
		return result+predicate.toUniqueInt();
	}
	
	public PredicateArgumentFormula instantiate()
	{
		//System.out.println("instantiate called in paform");
		
		ArrayList<Formula> insArgs=new ArrayList<Formula>();
		for(Formula f: this.arguments)
		{
			insArgs.add(f.instantiate());
		}
		return new PredicateArgumentFormula(this.predicate.instantiate(), insArgs);
		
	}
	
	
}