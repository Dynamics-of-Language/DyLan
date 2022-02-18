/**
 * 
 */
package qmul.ds.formula.rdf;
//package qmul.ds.gui;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Stroke;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;

import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.HorizontalTreeLayout;
import edu.uci.ics.jung.algorithms.layout.DAGLayout;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.GraphMouseListener;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

/**
 * @author angus
 *
 */

public class DSMViewer {

	private static final long serialVersionUID = 1L;

	RDFJungGraph dsm=null;
	VisualizationViewer<RDFJungNode,RDFJungEdge> vv;

	protected Logger logger=Logger.getLogger(DSMViewer.class);
	
	public DSMViewer()
	{
		// TODO Auto-generated constructor stub
	}
	
	public DSMViewer(final RDFJungGraph dsm)
	{
		this.dsm=dsm;
		vv=new VisualizationViewer<RDFJungNode,RDFJungEdge>(new ISOMLayout<RDFJungNode,RDFJungEdge>(dsm));
		
		vv.getRenderContext().setEdgeLabelTransformer(
				new Transformer<RDFJungEdge, String>() {
					public String transform(RDFJungEdge edge) {
						return edge.getEdgeLabel();
					}
				});
		vv.getRenderContext().setEdgeStrokeTransformer(new Transformer<RDFJungEdge, Stroke>(){
			public Stroke transform(RDFJungEdge edge)
			{
				return edge.getEdgeStroke();
			}
			
			
			
		});
		vv.getRenderContext().setVertexLabelTransformer(
				new Transformer<RDFJungNode, String>() {
					public String transform(RDFJungNode t) {
						
						return dsm.getNodeLabel(t);
					}
				});

		
		vv.getRenderContext().setEdgeShapeTransformer(
				new EdgeShape.Line<RDFJungNode, RDFJungEdge>());
		vv.getRenderContext()
				.setEdgeArrowPredicate(
						new Predicate<Context<Graph<RDFJungNode, RDFJungEdge>, RDFJungEdge>>() {
							@Override
							public boolean evaluate(
									Context<Graph<RDFJungNode, RDFJungEdge>, RDFJungEdge> arg0) {

								return true;
							}
						});

		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.S);
//		vv.addGraphMouseListener(this);
		vv.setPreferredSize(new Dimension(1200, 200));
		
	}
	
	public DSMViewer(Forest<RDFJungNode,RDFJungEdge> dsm) {
		
		vv=new VisualizationViewer<RDFJungNode,RDFJungEdge>(new HorizontalTreeLayout<RDFJungNode,RDFJungEdge>(dsm));
		
		vv.getRenderContext().setEdgeLabelTransformer(
				new Transformer<RDFJungEdge, String>() {
					public String transform(RDFJungEdge edge) {
						return edge.getEdgeLabel();
					}
				});
		vv.getRenderContext().setEdgeStrokeTransformer(new Transformer<RDFJungEdge, Stroke>(){
			public Stroke transform(RDFJungEdge edge)
			{
				return edge.getEdgeStroke();
			}
			
			
			
		});
		vv.getRenderContext().setVertexLabelTransformer(
				new Transformer<RDFJungNode, String>() {
					public String transform(RDFJungNode t) {
						return t.toString();
					}
				});

		
		vv.getRenderContext().setEdgeShapeTransformer(
				new EdgeShape.Line<RDFJungNode, RDFJungEdge>());
		vv.getRenderContext()
				.setEdgeArrowPredicate(
						new Predicate<Context<Graph<RDFJungNode, RDFJungEdge>, RDFJungEdge>>() {
							@Override
							public boolean evaluate(
									Context<Graph<RDFJungNode, RDFJungEdge>, RDFJungEdge> arg0) {

								return true;
							}
						});

		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.S);
//		vv.addGraphMouseListener(this);
		vv.setPreferredSize(new Dimension(1200, 200));

	}

	public void setDSM(Forest<RDFJungNode,RDFJungEdge> dsm) {
		
		update(dsm);

	}

	public void update(RDFJungGraph dsm)
	{
		
		this.dsm=dsm;
		update(dsm);
	}
	
	public void update(Forest<RDFJungNode,RDFJungEdge> dsm) {
		vv.setGraphLayout(new HorizontalTreeLayout<RDFJungNode, RDFJungEdge>(
				dsm));
	}
	
//	@Override
//	public void graphClicked(RDFJungNode arg0, MouseEvent arg1) {
//		// TODO Auto-generated method stub
//
//	}
//	
//	@Override
//	public void graphPressed(RDFJungNode arg0, MouseEvent arg1) {
//		// TODO Auto-generated method stub
//
//	}
//	
//	@Override
//	public void graphReleased(RDFJungNode arg0, MouseEvent arg1) {
//		// TODO Auto-generated method stub
//
//	}
	
	public static void main(String[] args) {
		String jLikesJ = 
				  "{var:x "
				+ "a schema:Person;"
				+ "rdfs:label \"Jane\"@en ."
				+ "var:y "
				+ "a schema:Person;"
				+ "rdfs:label \"John\"@en ."
				+ "var:e "
				+ "a schema:Action;"
				+ "rdfs:label \"like\"@en;"
				+ "a dsrdf:Head;"
				+ "schema:agent var:x;"
				+ "schema:object var:y.}";
		
		RDFGraph jGraph = new RDFGraph(jLikesJ);
		RDFJungGraph rjg = new RDFJungGraph(jGraph.getModel());
		
		JFrame mf = new JFrame();
		DSMViewer viewer = new DSMViewer(rjg);
		GraphZoomScrollPane scrollPane = new GraphZoomScrollPane(viewer.vv);
		mf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mf.setSize(500,500);
		mf.add(scrollPane);
		mf.pack();
		mf.setVisible(true);
		
//		RDFDataMgr.write(System.out, jGraph.getModel(), Lang.TURTLE);
//		System.out.println(rjg.dsm);
	}

}
