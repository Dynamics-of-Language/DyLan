package qmul.ds.tree.label;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;

/**
 * A disjunction of labels, e.g. (?ty(e) || ?ty(t) || ty(t))
 * 
 * True if one of them is true.
 * 
 * Brackets compulsory...
 * 
 * @author arash
 */
public class LabelDisjunction extends EmbeddedLabelGroup {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6933030081622609556L;
	public static String DISJ_FUNCTOR = "\\|\\|";

	public LabelDisjunction(List<Label> sl, IfThenElse ite) {
		super(sl, ite);

	}

	public LabelDisjunction(List<Label> sl) {
		super(sl);

	}
	public LabelDisjunction(Set<Label> disjunct) {
		super(disjunct);
		
	}

	public static LabelDisjunction parse(String s1, IfThenElse ite) {

		String s = s1.trim();
		if (!(s.startsWith("(") && s.endsWith("")))
			return null;
		s = s.substring(1, s.length() - 1);

		String labelS[] = s.split(DISJ_FUNCTOR);
		List<Label> labels = new ArrayList<Label>();
		for (String l : labelS) {
			labels.add(LabelFactory.create(l.trim(), ite));
		}

		return new LabelDisjunction(labels, ite);

	}

	@Override
	public Label instantiate() {
		
		List<Label> result = new ArrayList<Label>();
		for (Label l : labels) {
			result.add(l.instantiate());
		}
		return new LabelDisjunction(result);
	}
	

	public boolean check(Node n) {
		return super.checkLabelsDisj(n);
	}

	public boolean checkWithTupleAsContext(Tree t, ParserTuple context) {
		return super.checkLabelsDisj(t, context);
	}

	public int hashCode() {
		final int prime = 17;
		int result = 1;
		for (Label label : labels)
			result = prime * result + ((label == null) ? 0 : label.hashCode());

		result = prime * result + DISJ_FUNCTOR.hashCode();

		return result;
	}

	public boolean equals(Object o) {
		if (!(o instanceof LabelDisjunction))
			return false;

		LabelDisjunction other = (LabelDisjunction) o;

		if (other.labels.size() != this.labels.size())
			return false;

		for (int i = 0; i < labels.size(); i++) {
			Label thisL = labels.get(i);

			if (!other.labels.contains(thisL)) {
				logger.debug(thisL + " was not in  " + other + "\nreturning false from equals in ELC");

				return false;
			}
		}
		return true;
	}

	public String toString() {
		String s = "(";
		for (Label l : labels)
			s += l + " || ";

		return s.substring(0, s.length() - 4) + ")";
	}

}
