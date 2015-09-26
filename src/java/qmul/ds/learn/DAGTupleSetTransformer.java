package qmul.ds.learn;

import org.apache.commons.collections15.Transformer;

import qmul.ds.dag.DAGTupleSet;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.Requirement;
import qmul.ds.tree.label.TypeLabel;

public class DAGTupleSetTransformer implements Transformer<DAGTupleSet, String> {

	@Override
	public String transform(DAGTupleSet ts) {
		// return "type";

		if (ts.isEmpty())
			return "";
		Tree t = ts.get(0).getTree();
		Node pointed = t.getPointedNode();
		String result = "{";
		for (Label l : pointed) {

			if (l instanceof TypeLabel) {
				result += l.toString() + ",";
			}
			if (l instanceof Requirement) {
				Requirement r = (Requirement) l;
				if (r.getLabel() instanceof TypeLabel)
					result += l.toString() + ",";
			}
		}
		return result.substring(0, result.length() - 1) + "}[" + ts.size() + "]";
	}

}
