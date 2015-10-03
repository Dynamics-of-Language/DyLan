package qmul.ds.formula;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class TTRRelativePath extends TTRPath {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static Logger logger = Logger.getLogger(TTRRelativePath.class);

	public TTRRelativePath(List<TTRLabel> list) {
		super(list);
		if (!list.isEmpty())
			getVariables().add(list.get(0));
	}

	public TTRRelativePath(TTRRelativePath ttrRelativePath) {
		this(new ArrayList<TTRLabel>(ttrRelativePath.labels));
		this.parentRecType = ttrRelativePath.parentRecType;
	}

	public TTRRelativePath() {
		super();

	}

	public TTRRelativePath removeFirst() {
		TTRRelativePath path = new TTRRelativePath(this);

		if (path.labels.size() == 1)
			return null;
		TTRLabel l = path.labels.remove(0);
		path.parentRecType = path.parentRecType!=null?(TTRRecordType) path.parentRecType.get(l):null;
		return path;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#substitute(qmul.ds.formula.Formula, qmul.ds.formula.Formula)
	 */
	@Override
	public Formula substitute(Formula f1, Formula f2) {
		if (!(f1 instanceof Variable && f2 instanceof Variable))
			return this;
		logger.debug("Substituting " + f2 + " for " + f1 + " in path:" + this);
		List<TTRLabel> newLabels = new ArrayList<TTRLabel>();

		for (int i = 0; i < labels.size(); i++) {
			if (f1.equals(labels.get(i)))
				newLabels.add(new TTRLabel((Variable) f2));
			else
				newLabels.add(labels.get(i));
		}
		TTRRelativePath path = new TTRRelativePath(newLabels);
		return path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.TTRPath#evaluate()
	 */
	@Override
	public Formula evaluate() {
		TTRRecordType cur = this.parentRecType;

		while (!evaluateAgainst(cur) && cur != null) {
			cur = cur.getParentRecType();
		}

		if (cur == null) {
			logger.warn("trying to get the pointed type of a bad TTR Path: " + this + " in rec type:"
					+ getParentRecType());
			return null;
		}

		for (int i = 0; i < labels.size() - 1; i++) {
			TTRLabel l = labels.get(i);
			Formula type = cur.getType(l);
			if (type != null && (type instanceof TTRRecordType))
				cur = (TTRRecordType) type;
			else
				throw new IllegalArgumentException("Bad path: " + this + " in rec type: " + parentRecType);
		}

		Formula pointedType = cur.getType(labels.get(labels.size() - 1));
		return pointedType == null ? null : pointedType.evaluate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#clone()
	 */
	public TTRPath clone() {
		return new TTRRelativePath(this);
	}

	public static void main(String a[]) {
		TTRRelativePath path = (TTRRelativePath) TTRPath.parse("r.r1.x");
		TTRRecordType type = TTRRecordType.parse("[r:[r1:[x:e|p==man(x):t]]|x==john:e]");
		path.parentRecType = type;
		System.out.println(path.getMinimalSuperTypeWith());

	}

	public TTRRecordType getMinimalSuperTypeWith() {
		if (labels.size() == 1) {

			return parentRecType.getSuperTypeWithParents(parentRecType.getField(labels.get(0)));
		}

		TTRRecordType result = new TTRRecordType();
		TTRRelativePath sub = this.removeFirst();

		result.add(new TTRField(labels.get(0), null, sub.getMinimalSuperTypeWith()));
		return result;
	}

	public TTRRelativePath add(TTRLabel l) {
		TTRRelativePath copy = new TTRRelativePath(this);
		copy.labels.add(l);
		return copy;
	}

	public List<TTRLabel> getLabels() {
		// TODO Auto-generated method stub
		return labels;
	}

	public boolean isEmpty() {
		return labels.isEmpty();
		
	}

	@Override
	public int toUniqueInt() {
		Formula eval=evaluate();
		return eval==null?0:eval.toUniqueInt();
	}
}
