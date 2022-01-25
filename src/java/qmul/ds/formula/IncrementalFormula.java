package qmul.ds.formula;

public abstract class IncrementalFormula extends Formula {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7139447672308706890L;

	@Override
	public abstract Formula substitute(Formula f1, Formula f2);

	@Override
	public abstract Formula clone();

	@Override
	public abstract int toUniqueInt();

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
