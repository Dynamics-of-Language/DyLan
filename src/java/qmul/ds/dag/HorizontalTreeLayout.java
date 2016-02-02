package qmul.ds.dag;

import java.awt.Dimension;
import java.awt.geom.Point2D;

import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.Forest;

/**
 * a tree layout that makes the graph horizontal.
 * 
 * @author arash
 * 
 * @param <V>
 *            vertex type
 * @param <E>
 *            edge type
 */
public class HorizontalTreeLayout<V, E> extends TreeLayout<V, E> {

	public HorizontalTreeLayout(Forest<V, E> graph) {
		super(graph, 50, 120);

	}

	public Dimension getSize() {
		return new Dimension((int) this.size.getHeight(),
				(int) this.size.getWidth());
	}

	@Override
	protected void buildTree() {
		super.buildTree();

		for (V vertex : locations.keySet()) {
			Point2D point = locations.get(vertex);

			point.setLocation(point.getY(), point.getX());
		}

	}
}
