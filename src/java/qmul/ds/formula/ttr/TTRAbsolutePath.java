package qmul.ds.formula.ttr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.formula.Formula;
import qmul.ds.formula.Variable;

/**
 * An absolute path of labels, e.g. R.x.y, mean
 * 
 * @author arash
 * 
 */

public class TTRAbsolutePath extends TTRPath {

	private static Logger logger = Logger.getLogger(TTRAbsolutePath.class);
	private static final long serialVersionUID = 1L;
	public static final String META_TTRF_VARIABLE = "REC";
	// the record type from which this absolute path is to be interpreted
	TTRRecordType domain;
	// name of the domain record type, e.g. r, r1, r2 etc.
	TTRLabel name;

	public TTRAbsolutePath(TTRLabel name, TTRRecordType domain, List<TTRLabel> labels) {
		super(labels);
		this.domain = domain;
		this.name = name;
	}

	public TTRAbsolutePath(TTRAbsolutePath p) {
		this((TTRLabel) p.name.clone(), p.domain, new ArrayList<TTRLabel>(p.labels));

		// p.domain==null?null:p.domain.clone()
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#substitute(qmul.ds.formula.Formula, qmul.ds.formula.Formula)
	 */
	public Formula substitute(Formula f1, Formula f2) {
		// System.out.println("we are here");
		if (f1.equals(this))
			return f2;
		if (this.name.equals(f1) && f2 instanceof TTRRecordType) {
			logger.debug("instantiating domain record type in absolute path, " + this + " with: " + f2);
			return new TTRAbsolutePath(this.name.clone(), (TTRRecordType) f2, new ArrayList<TTRLabel>(this.labels));
		}

		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.TTRPath#subsumesMapped(qmul.ds.formula.Formula, java.util.HashMap)
	 */
	public boolean subsumesMapped(Formula other, HashMap<Variable, Variable> map) {
		if (other == null)
			return false;
		Formula thisEval = evaluate();
		if (thisEval == null)
			return true;

		if (other instanceof TTRAbsolutePath) {
			Formula otherEval = other.evaluate();
			if (thisEval instanceof TTRAbsolutePath && otherEval instanceof TTRAbsolutePath) {

				return ((TTRAbsolutePath) thisEval).name.subsumesMapped(((TTRAbsolutePath) otherEval).name, map)
						&& ((TTRAbsolutePath) thisEval).labels.equals(((TTRAbsolutePath) otherEval).labels);
			} else if (thisEval instanceof TTRAbsolutePath)
				return false;
			else
				return thisEval.subsumesMapped(otherEval, map);

		}

		if (thisEval instanceof TTRAbsolutePath)
			return false;

		return thisEval.subsumesMapped(other, map);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.TTRPath#evaluate()
	 */
	@Override
	public Formula evaluate() {
		if (domain == null) {
			logger.debug("trying to get pointed type of Absolute TTRPath, while the domain rec type has not been instantiated (substituted for)");
			return this;
		}
		if (!evaluateAgainst(domain)) {
			logger.error("bad absolute path: " + this + "for record type " + name + "=" + domain);
			return null;
		}
		TTRRecordType cur = domain;
		for (int i = 0; i < labels.size() - 1; i++) {
			TTRLabel l = labels.get(i);
			Formula type = cur.getType(l);
			if (type != null && (type instanceof TTRRecordType))
				cur = (TTRRecordType) type;
			else
				throw new IllegalArgumentException("Bad path: " + this + " in rec type: " + parentRecType);
		}
		// logger.debug("Found pointed label/type in TTRAbsolute path evaluation: "+labels.get(labels.size() -
		// 1)+": "+cur.getType(labels.get(labels.size() - 1)));
		Formula pointedType = cur.getType(labels.get(labels.size() - 1));
		if (pointedType==null) 
			return labels.get(labels.size()-1);
		Formula result =/* pointedType == null ? null : */pointedType.evaluate();
		if (result instanceof Variable && getParentRecType() == null)
			return domain.get(new TTRLabel((Variable) result));

		return result;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.TTRPath#hashCode()
	 */
	public int hashCode() {
		int prime = 17;
		int result = super.hashCode();
		result = result * prime + (name == null ? 0 : name.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.TTRPath#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (!(o instanceof TTRAbsolutePath))
			return false;

		TTRAbsolutePath other = (TTRAbsolutePath) o;
		return super.equals(other) && name.equals(other.name);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#clone()
	 */
	public TTRPath clone() {
		return new TTRAbsolutePath(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.TTRPath#toString()
	 */
	public String toString() {
		String s = this.name.toString();
		for (TTRLabel l : this.labels)
			s += "." + l;

		return s;
	}

	public TTRAbsolutePath instantiate() {
		return this;
	}

	public static void main(String a[]) {
		Formula f = Formula.create("[head:x]");
		System.out.println(f);
	}

	@Override
	public int toUniqueInt() {
		throw new UnsupportedOperationException("Shouldn't need to turn absolute paths to integers for now. They only appear in lambda abstracts");
		
	}
}
