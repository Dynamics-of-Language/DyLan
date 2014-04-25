package qmul.ds.dag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import edu.uci.ics.jung.graph.DelegateTree;

public class OrderedDelegateTree<V, E extends Comparable<? super E>> extends DelegateTree<V, E> {

	private static Logger logger = Logger.getLogger(OrderedDelegateTree.class);

	private static final long serialVersionUID = -5925774062108430939L;

	@Override
	public List<E> getOutEdges(V vertex) {
		List<E> edges = new ArrayList<E>(super.getOutEdges(vertex));
		Collections.sort(edges);
		return edges;
	}

}
