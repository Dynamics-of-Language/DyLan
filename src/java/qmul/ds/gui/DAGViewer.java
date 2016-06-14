package qmul.ds.gui;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Stroke;
import java.awt.event.MouseEvent;

import javax.swing.JDialog;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;

import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.HorizontalTreeLayout;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.GraphMouseListener;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
/**
 * A generic DAGViewer.
 * @author Arash
 *
 * @param <T>
 * @param <E>
 */
public class DAGViewer<T extends DAGTuple, E extends DAGEdge> implements GraphMouseListener<T> {

	private static final long serialVersionUID = 1L;

	DAG<T, E> dag=null;
	VisualizationViewer<T,E> vv;

	protected Logger logger=Logger.getLogger(DAGViewer.class);
	
	public DAGViewer()
	{
		
	}
	
	public DAGViewer(DAG<T,E> dag)
	{
		this.dag=dag;
		vv=new VisualizationViewer<T,E>(new HorizontalTreeLayout<T,E>(this.dag.getInContextSubgraph()));
		
		vv.getRenderContext().setEdgeLabelTransformer(
				new Transformer<E, String>() {
					public String transform(E edge) {
						return edge.getEdgeLabel();
					}
				});
		vv.getRenderContext().setEdgeStrokeTransformer(new Transformer<E, Stroke>(){
			public Stroke transform(E edge)
			{
				return edge.getEdgeStroke();
			}
			
			
			
		});
		vv.getRenderContext().setVertexLabelTransformer(
				new Transformer<T, String>() {
					public String transform(T t) {
						
						return dag.getTupleLabel(t);
					}
				});

		
		vv.getRenderContext().setEdgeShapeTransformer(
				new EdgeShape.Line<T, E>());
		vv.getRenderContext()
				.setEdgeArrowPredicate(
						new Predicate<Context<Graph<T, E>, E>>() {
							@Override
							public boolean evaluate(
									Context<Graph<T, E>, E> arg0) {

								return true;
							}
						});

		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.S);
		vv.addGraphMouseListener(this);
		// vv.getRenderer().get
		vv.setPreferredSize(new Dimension(1200, 200));
		
	}
	
	public DAGViewer(Forest<T,E> dag) {
		
		vv=new VisualizationViewer<T,E>(new HorizontalTreeLayout<T,E>(dag));
		
		vv.getRenderContext().setEdgeLabelTransformer(
				new Transformer<E, String>() {
					public String transform(E edge) {
						return edge.getEdgeLabel();
					}
				});
		vv.getRenderContext().setEdgeStrokeTransformer(new Transformer<E, Stroke>(){
			public Stroke transform(E edge)
			{
				return edge.getEdgeStroke();
			}
			
			
			
		});
		vv.getRenderContext().setVertexLabelTransformer(
				new Transformer<T, String>() {
					public String transform(T t) {
						return t.transform();
					}
				});

		
		vv.getRenderContext().setEdgeShapeTransformer(
				new EdgeShape.Line<T, E>());
		vv.getRenderContext()
				.setEdgeArrowPredicate(
						new Predicate<Context<Graph<T, E>, E>>() {
							@Override
							public boolean evaluate(
									Context<Graph<T, E>, E> arg0) {

								return true;
							}
						});

		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.S);
		vv.addGraphMouseListener(this);
		// vv.getRenderer().get
		vv.setPreferredSize(new Dimension(1200, 200));
		// GraphZoomScrollPane scrollPane = new GraphZoomScrollPane(vv);
		// scrollPane.getHorizontalScrollBar().setMaximum(1000);
		// scrollPane.getHorizontalScrollBar().setMinimum(0);
		// scrollPane.setPreferredSize(new Dimension(1000,200));
		// scrollPane.setSize(200, 200);
		// scrollPane.setSize(new Dimension(800,300));
		// add(scrollPane);
		// hypDisplay.setPreferredSize(new Dimension(800,300));
		// pack();
		// setVisible(true);

	}

	public void setDAG(Forest<T,E> dag) {
		
		update(dag);

	}

	public void update(DAG<T,E> dag)
	{
		this.dag=dag;
		update(dag.getInContextSubgraph());
	}
	public void update(Forest<T,E> dag) {
		
		vv.setGraphLayout(new HorizontalTreeLayout<T, E>(
				dag));
	}

	@Override
	public void graphClicked(T arg0, MouseEvent arg1) {
		
		logger.debug("Vertex Clicked:" + arg0);
		ParserTupleViewer viewer=new ParserTupleViewer(arg0, dag.getContext());
		JDialog hypDisplay = new JDialog();
		hypDisplay.setModalityType(ModalityType.MODELESS);
		hypDisplay.add(viewer);
		// hypDisplay.setPreferredSize(new Dimension(800,300));
		hypDisplay.pack();
		hypDisplay.setVisible(true);

	}

	@Override
	public void graphPressed(T arg0, MouseEvent arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void graphReleased(T arg0, MouseEvent arg1) {
		// TODO Auto-generated method stub

	}

}
