/**
 * 
 */
package qmul.ds.formula.rdf;
//package qmul.ds.gui;

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
 * @author angus
 *
 */

public class DSMViewer<T extends RDFJungNode, E extends RDFJungEdge> implements GraphMouseListener<T> {

	private static final long serialVersionUID = 1L;

	RDFJungGraph<T, E> dsm=null;
	VisualizationViewer<T,E> vv;

	protected Logger logger=Logger.getLogger(DSMViewer.class);
	
	public DSMViewer()
	{
		// TODO Auto-generated constructor stub
	}
	
	public DSMViewer(final RDFJungGraph<T,E> dsm)
	{
		this.dsm=dsm;
		vv=new VisualizationViewer<T,E>(new HorizontalTreeLayout<T,E>(this.dsm.getInContextSubgraph()));
		
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
						
						return dsm.getNodeLabel(t);
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
		vv.setPreferredSize(new Dimension(1200, 200));
		
	}
	
	public DSMViewer(Forest<T,E> dsm) {
		
		vv=new VisualizationViewer<T,E>(new HorizontalTreeLayout<T,E>(dsm));
		
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
						return t.toString();
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
		vv.setPreferredSize(new Dimension(1200, 200));

	}

	public void setDSM(Forest<T,E> dsm) {
		
		update(dsm);

	}

	public void update(RDFJungGraph<T,E> dsm)
	{
		
		this.dsm=dsm;
		update(dsm.getInContextSubgraph());
	}
	
	public void update(Forest<T,E> dsm) {
		vv.setGraphLayout(new HorizontalTreeLayout<T, E>(
				dsm));
	}
	
	@Override
	public void graphClicked(T arg0, MouseEvent arg1) {
		// TODO Auto-generated method stub

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
