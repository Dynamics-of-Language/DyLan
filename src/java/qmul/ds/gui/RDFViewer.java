/**
 * 
 */
package qmul.ds.gui;
//package qmul.ds.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import javax.swing.JFrame;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import qmul.ds.formula.rdf.RDFGraph;
import qmul.ds.formula.rdf.RDFJungEdge;
import qmul.ds.formula.rdf.RDFJungGraph;
import qmul.ds.formula.rdf.RDFJungNode;
import qmul.ds.tree.Tree;

/**
 * @author angus
 *
 */

public class RDFViewer {

	private static final long serialVersionUID = 1L;

	RDFJungGraph dsm=null;
	VisualizationViewer<RDFJungNode,RDFJungEdge> vv;
	
	int width = 1000;
	int height = 400;

	protected Logger logger=Logger.getLogger(RDFViewer.class);
	
	public RDFViewer()
	{
		this(new RDFGraph((RDFGraph)new Tree("rdf").getMaximalSemantics()));
	}
	
	public RDFViewer(final RDFGraph g)
	{
		this.dsm=new RDFJungGraph(g);
		vv=new VisualizationViewer<RDFJungNode,RDFJungEdge>(new FRLayout<RDFJungNode,RDFJungEdge>(dsm, new Dimension(this.width, this.height)));
		
		vv.getRenderContext().setEdgeLabelTransformer(
				new Transformer<RDFJungEdge, String>() {
					public String transform(RDFJungEdge edge) {
						
						return edge.getEdgeLabel();
					}
				});
		
		
		vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<RDFJungEdge>());
//	    float dash[] = {10.0f};
//		   final Stroke edgeStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
//		             BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
//		   
//		vv.getRenderContext().setEdgeStrokeTransformer(new Transformer<RDFJungEdge, Stroke>(){
//			public Stroke transform(RDFJungEdge edge)
//			{
//				return edgeStroke;
//			}
//			
//			
//			
//		});
		   
		   
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

		 Transformer<RDFJungNode,Paint> vertexPaint = new Transformer<RDFJungNode,Paint>() {
	            public Paint transform(RDFJungNode i) {
	                return Color.WHITE;
	            } };
	            
	    Transformer<RDFJungNode,Shape> vertexSize = new Transformer<RDFJungNode,Shape>(){
	                public Shape transform(RDFJungNode i){
	                	
	                	int stringWidth = vv.getRenderContext().getGraphicsContext().getFontMetrics().stringWidth(i.toString())+10; 
	                	int width = (stringWidth < 30)?30:stringWidth;
	                	
	                    Ellipse2D circle = new Ellipse2D.Double(-15, -15, width, 30);
	                    
	                    return circle;
	                }
	            };
	   
	    vv.getRenderContext().setVertexShapeTransformer(vertexSize);
		vv.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
		
		vv.setPreferredSize(new Dimension(1200, 200));
		
	}
	

	public void setGraph(RDFJungGraph dsm) {
		
		update(dsm);

	}

	public void update(RDFGraph graph)
	{
		
		this.dsm=new RDFJungGraph(graph);
		
		update(dsm);
	}
	
	public void update(RDFJungGraph g) {
		vv.setGraphLayout(new FRLayout<RDFJungNode,RDFJungEdge>(g, new Dimension(this.width,this.height)));
	}
	

	
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
	
		
		JFrame mf = new JFrame();
		RDFViewer viewer = new RDFViewer(jGraph);
		GraphZoomScrollPane scrollPane = new GraphZoomScrollPane(viewer.vv);
		mf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mf.setSize(500,500);
		mf.add(scrollPane);
		mf.pack();
		mf.setVisible(true);
		

	}

}
