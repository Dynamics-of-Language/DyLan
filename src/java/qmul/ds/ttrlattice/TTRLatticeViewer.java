package qmul.ds.ttrlattice;

import ptolemy.graph.Edge;
import ptolemy.graph.Node;

import java.awt.*;  
import java.io.File;

import javax.swing.*;  


/**
 * An simple JFrame to display {@link TTRLattice}s
 * 
 * @author julianhough
 */
@SuppressWarnings("serial") 
public class TTRLatticeViewer extends JFrame {   

	TTRLattice lattice;
	
   private void printGraphVizToFile(String filename)
   {
	   //System.out.println(lattice);
	   GraphViz gv = new GraphViz();
		gv.addln(gv.start_graph());
		
		for (Object o : lattice.edges()){
			Edge edge = (Edge) o;
			String connection = Integer.toString(lattice.nodeLabel(((Node) edge.sink()))) + " -> " + Integer.toString(lattice.nodeLabel(((Node) edge.source())));
			gv.addln(connection);
			
		}
		
		//gv.addln("A -> B;");
		//gv.addln("A -> C;");
		gv.addln(gv.end_graph());
		//System.out.println(gv.getDotSource());

		gv.increaseDpi();   // 106 dpi

		//String type = "gif";
		//      String type = "dot";
		//      String type = "fig";    // open with xfig
		//      String type = "pdf";
		//      String type = "ps";
		//      String type = "svg";    // open with inkscape
		String type = "png";
		//      String type = "plain";
		
		String repesentationType= "dot";
		//		String repesentationType= "neato";
		//		String repesentationType= "fdp";
		//		String repesentationType= "sfdp";
		// 		String repesentationType= "twopi";
		// 		String repesentationType= "circo";
		
		File out = new File(filename);   // Linux
		//      File out = new File("c:/eclipse.ws/graphviz-java-api/out." + type);    // Windows
		gv.writeGraphToFile( gv.getGraph(gv.getDotSource(), type, repesentationType), out );
	}
	
	
  public TTRLatticeViewer(TTRLattice new_lattice,String argx) { 
    if ( argx == null ) {
      argx = "a.png";
    }   
    lattice = new_lattice;
    this.printGraphVizToFile(argx);
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
    this.setSize(1000,1000);
    JPanel panel = new JPanel();  
    //panel.setSize(500,640);
    panel.setBackground(Color.CYAN);  
    System.out.println("loading "+ argx);
    ImageIcon icon = new ImageIcon(argx);  
    JLabel label = new JLabel();  
    label.setIcon(icon);  
    panel.add(label); 
    this.getContentPane().add(panel);    
    this.setVisible(true);
    
  }
  
  
  public static void main(String[] args) { 
      new TTRLatticeViewer(null,args.length == 0 ? null : args[0]);
  } 

  
}