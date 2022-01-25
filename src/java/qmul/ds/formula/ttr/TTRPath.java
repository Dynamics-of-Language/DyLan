package qmul.ds.formula.ttr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.action.meta.MetaTTRRecordType;
import qmul.ds.formula.Formula;
import qmul.ds.formula.Variable;

/**
 * A TTRPath, such as a.arg, in [a=[arg=john:e]:RecType;p=run(a.arg):t] Two subclasses: {@link TTRAbsolutePath} and
 * {@link TTRRelativePath}, which implement the {@link #evaluate() getPointerType()} and
 * {@link #substitute(Formula, Formula) substitute} methods separately.
 * 
 * In order to distinguish paths with a single label, such as 'a' in x==a:e, in R=[a:e|x==a:e], from atomic formulae
 * (e.g. john) we always precede such paths with a '.' ; so R should read: [a:e|x==.a:e]
 * 
 * 
 * @author arash
 */

public abstract class TTRPath extends Formula {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String PATH_SEP = ".";

	public static final String REC_TYPE_NAME_PATTERN = "R\\d*";
	protected List<TTRLabel> labels = new ArrayList<TTRLabel>();

	public TTRPath() {
	}

	public TTRPath(List<TTRLabel> labels) {
		this.labels = labels;
	}

	public TTRPath(TTRPath p) {
		this(p.labels);
	}

	public TTRLabel getFinalLabel() {
		return this.labels.get(labels.size() - 1);

	}

	public List<TTRPath> getTTRPaths() {
		ArrayList<TTRPath> result = new ArrayList<TTRPath>();
		result.add(this);
		return result;
	}

	public static TTRPath parse(String path) {

		ArrayList<TTRLabel> labels = new ArrayList<TTRLabel>();
		ArrayList<String> labelStrings = new ArrayList<String>(
				Arrays.asList(path.trim().split(Pattern.quote(PATH_SEP))));
		if (labelStrings.size() <= 1)
			return null;
		String rtName = null;
		MetaTTRRecordType meta = null;
		if (labelStrings.get(0).isEmpty()) {
			// starts with '.', relative path.
			labelStrings.remove(0);
		} else if (labelStrings.get(0).matches(REC_TYPE_NAME_PATTERN)) {
			// absolute path
			rtName = labelStrings.get(0);
			labelStrings.remove(0);
		} else if (Formula.REC_METAVARIABLE_PATTERN.matcher(labelStrings.get(0)).matches()) {
			// absolute path
			meta = MetaTTRRecordType.get(labelStrings.get(0));
			labelStrings.remove(0);
		}

		for (String label : labelStrings) {

			Matcher m1 = TTRLabel.LABEL_PATTERN.matcher(label);
			if (!m1.matches())
				return null;

			labels.add(new TTRLabel(label));
		}

		if (rtName == null) {
			if (meta == null)
				return new TTRRelativePath(labels);

			return new TTRAbsolutePath(new TTRLabel(meta.getMeta().getName()), meta, labels);
		} else {
			TTRAbsolutePath result = new TTRAbsolutePath(new TTRLabel(rtName), null, labels);
			return result;
		}

	}

	public boolean subsumesMapped(Formula f, HashMap<Variable, Variable> map) {
		// assumes that the path is good (evaluable)
		// if the path is bad, and evaluation returns null
		// evaluate() will give warning if this is the case
		
		Formula eval = evaluate();
		// if pointed type is null, it subsumes anything
		if (eval == null)
			return true;

		if (f instanceof TTRPath)
			return eval.subsumesMapped(f.evaluate(), map);

		return eval.subsumesMapped(f, map);
	}

	/**
	 * evaluates path in the context of the record type passed as argument
	 * 
	 * @param r
	 * @return whether this path is valid
	 */
	public boolean evaluateAgainst(TTRRecordType r) {
		if (r == null)
			return false;
		TTRRecordType cur = r;
		for (int i = 0; i < labels.size() - 1; i++) {
			TTRLabel l = labels.get(i);
			Formula type = cur.getType(l);
			if (type != null && (type instanceof TTRRecordType))
				cur = (TTRRecordType) type;
			else
				return false;
		}

		return cur.hasLabel(labels.get(labels.size() - 1));
	}

	/**
	 * 
	 * @return the type that the path is pointing to, either within some other rec type in the case of TTRAbsolutePaths
	 *         or within the rec type in which the path is embedded as in the case of TTRRelativePaths
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#evaluate()
	 */
	public abstract Formula evaluate();

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#hashCode()
	 */
	public int hashCode() {
		return labels.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object oth) {
		if (!(oth instanceof TTRPath))
			return false;
		TTRPath other = (TTRPath) oth;
		return this.labels.equals(other.labels);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if (labels.isEmpty())
			return "";
		String s = labels.size() == 1 ? "." : "";
		for (TTRLabel l : this.labels)
			s += l + ".";

		return s.substring(0, s.length() - 1);
	}

	

	public static void main(String a[]) {
		TTRPath path = TTRPath.parse("d");
		System.out.println(path + ":" + path.getClass());
	}

	public TTRLabel getFirstLabel() {
		
		return labels.get(0);
	}

}
