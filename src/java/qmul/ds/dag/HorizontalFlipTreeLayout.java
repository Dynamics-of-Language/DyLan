package qmul.ds.dag;

import java.awt.Dimension;
import java.awt.geom.Point2D;

import qmul.ds.learn.WordHypothesis;

import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.Forest;

/**
 * a tree layout that makes the graph horizontal and flipped, i.e. left to right instead of right to left.
 * 
 * @author arash
 * 
 * @param <V>
 *            vertex type
 * @param <E>
 *            edge type
 */
public class HorizontalFlipTreeLayout<V, E> extends TreeLayout<V, E> {

	public HorizontalFlipTreeLayout(Forest<V, E> graph) {
		super(graph, 50, 180);

	}

	public Dimension getSize() {
		return new Dimension((int) this.size.getHeight(), (int) this.size.getWidth());
	}

	@Override
	protected void buildTree() {
		super.buildTree();

		for (V vertex : locations.keySet()) {
			Point2D point = locations.get(vertex);

			point.setLocation(getSize().getWidth() - point.getY(), point.getX());
		}

	}
}
