package qmul.ds.gui;

import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import qmul.ds.formula.rdf.RDFGraph;

@SuppressWarnings("serial")
public class RDFGraphPanel extends GraphZoomScrollPane {

	RDFViewer viewer;
	public RDFGraphPanel(RDFViewer viewer) {
		super(viewer.vv);
		this.viewer = viewer;
		
	}
	
	public RDFGraphPanel()
	{
		this(new RDFViewer());
	}

	public void setGraph(RDFGraph semantics) {
		viewer.update(semantics);
		
	}
	

}
