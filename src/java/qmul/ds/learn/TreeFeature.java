package qmul.ds.learn;

import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;
import edu.stanford.nlp.util.Pair;

public abstract class TreeFeature {

	public abstract Pair<Label, NodeAddress> extract(Tree t);

}
