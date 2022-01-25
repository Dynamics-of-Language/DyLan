package qmul.ds.formula.ttr;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.formula.Formula;
import qmul.ds.formula.Variable;

/**
 * A label in a TTR record type, as implemented in {@link TTRRecordType}
 * 
 * @author mpurver
 */
public class TTRLabel extends Variable {

	private static final long serialVersionUID = 1L;

	public final static Pattern LABEL_PATTERN = Pattern.compile("([a-z]+?)(\\d*)");

	public final static Pattern META_LABEL_PATTERN = Pattern.compile("(L+?|P+?|PRED+?)(\\d*)");
	public TTRLabel(String formula) {
		super(formula);
	}

	public TTRLabel(Variable l) {
		super(l);
	}
	
	
	/**
	 * Just for use by {@link MetaTTRLabel}
	 */
	protected TTRLabel() {
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.AtomicFormula#substitute(qmul.ds.formula.Formula, qmul.ds.formula.Formula)
	 */
	@Override
	public TTRLabel substitute(Formula f1, Formula f2) {
		if (this.equals(f1)) {
			return new TTRLabel((Variable) f2);
		} else {
			return this;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Variable#clone()
	 */
	@Override
	public TTRLabel clone() {
		return new TTRLabel(this);
	}

	
	public TTRLabel instantiate()
	{
		return this;
	}
	
	public HashSet<Variable> getVariables()
	{
		HashSet<Variable> res=new HashSet<Variable>();
		res.add(this);
		return res;
	}

}
