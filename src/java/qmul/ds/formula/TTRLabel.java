package qmul.ds.formula;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A label in a TTR record type, as implemented in {@link TTRRecordType}
 * 
 * @author mpurver
 */
public class TTRLabel extends Variable {

	private static final long serialVersionUID = 1L;

	public final static Pattern LABEL_PATTERN = Pattern.compile("([a-z]+?)(\\d*)");

	public TTRLabel(String formula) {
		super(formula);
	}

	public TTRLabel(Variable l) {
		super(l);
	}

	/**
	 * @return the next label with the same basic root e.g. x -> x1, x42 -> x43 etc
	 */
	public TTRLabel next() {
		Matcher m = LABEL_PATTERN.matcher(toString());
		if (!m.matches()) {
			throw new RuntimeException("strange TTRLabel " + this);
		}
		int myNum = (m.group(2).isEmpty() ? 0 : Integer.parseInt(m.group(2)));
		return new TTRLabel(m.group(1) + ++myNum);
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

	public static void main(String a[]) {
		Matcher m = LABEL_PATTERN.matcher("p=run(x)");
		if (m.matches())
			System.out.println("group 1:" + m.group(1) + "\ngroup 2:" + m.group(2));
	}

}
