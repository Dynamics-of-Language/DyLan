package qmul.ds.ttrlattice;


//GraphViz.java - a simple API to call dot from Java programs

/*$Id$*/
/*
******************************************************************************
*                                                                            *
*                    (c) Copyright Laszlo Szathmary                          *
*                                                                            *
* This program is free software; you can redistribute it and/or modify it    *
* under the terms of the GNU Lesser General Public License as published by   *
* the Free Software Foundation; either version 2.1 of the License, or        *
* (at your option) any later version.                                        *
*                                                                            *
* This program is distributed in the hope that it will be useful, but        *
* WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY *
* or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public    *
* License for more details.                                                  *
*                                                                            *
* You should have received a copy of the GNU Lesser General Public License   *
* along with this program; if not, write to the Free Software Foundation,    *
* Inc., 675 Mass Ave, Cambridge, MA 02139, USA.                              *
*                                                                            *
******************************************************************************
*/


/**
 * <dl>
 * <dt>Purpose: GraphViz Java API
 * <dd>
 *
 * <dt>Description:
 * <dd> With this Java class you can simply call dot
 *      from your Java programs.
 * <dt>Example usage:
 * <dd>
 * <pre>
 *    GraphViz gv = new GraphViz();
 *    gv.addln(gv.start_graph());
 *    gv.addln("A -> B;");
 *    gv.addln("A -> C;");
 *    gv.addln(gv.end_graph());
 *    System.out.println(gv.getDotSource());
 *
 *    String type = "gif";
 *    String representationType="dot";
 *    File out = new File("out." + type);   // out.gif in this example
 *    gv.writeGraphToFile( gv.getGraph(gv.getDotSource(), type, representationType), out );
 * </pre>
 * </dd>
 *
 * </dl>
 *
 * @version v0.6, 2013/11/28 (November) -- Patch of Olivier Duplouy is added. Now you 
 * can specify the representation type of your graph: dot, neato, fdp, sfdp, twopi, circo
 * @version v0.5.1, 2013/03/18 (March) -- Patch of Juan Hoyos (Mac support)
 * @version v0.5, 2012/04/24 (April) -- Patch of Abdur Rahman (OS detection + start subgraph + 
 * read config file)
 * @version v0.4, 2011/02/05 (February) -- Patch of Keheliya Gallaba is added. Now you
 * can specify the type of the output file: gif, dot, fig, pdf, ps, svg, png, etc.
 * @version v0.3, 2010/11/29 (November) -- Windows support + ability to read the graph from a text file
 * @version v0.2, 2010/07/22 (July) -- bug fix
 * @version v0.1, 2003/12/04 (December) -- first release
 * @author  Laszlo Szathmary (<a href="jabba.laci@gmail.com">jabba.laci@gmail.com</a>)
 */


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import ptolemy.graph.Edge;
import ptolemy.graph.Node;

public class GraphViz
{
	/**
	 * Detects the client's operating system.
	 */
	private final static String osName = System.getProperty("os.name").replaceAll("\\s","");

	/**
	 * Load the config.properties file.
	 */
	//private final static String cfgProp = "/Applications/graphviz-java-api-master/config/config.properties"; //Note make sure this is where the graphviz-java-api is
	private final static String cfgProp = "C:\\Program Files\\graphviz-java-api-master\\config\\config.properties";
	private final static Properties configFile = new Properties() {
		private final static long serialVersionUID = 1L; {
			try {
				load(new FileInputStream(cfgProp));
			} catch (Exception e) {
				System.err.println(e);
				System.err.println("Set the variable cfgProp to the appropriate graphviz location on your machine.");
			}
		}
	};

	/**
	 * The dir. where temporary files will be created.
	 */
	private static String TEMP_DIR = System.getProperty("user.dir") + "\\..\\..\\..\\..\\Temp"; //windows
			//configFile.getProperty("tempDirFor" + osName); //linux/mac

	/**
	 * Where is your dot program located? It will be called externally.
	 */
	private static String DOT = configFile.getProperty("dotFor" + osName);

	/**
	 * The image size in dpi. 96 dpi is normal size. Higher values are 10% higher each.
	 * Lower values 10% lower each.
	 * 
	 * dpi patch by Peter Mueller
	 */
	private int[] dpiSizes = {46, 51, 57, 63, 70, 78, 86, 96, 106, 116, 128, 141, 155, 170, 187, 206, 226, 249};

	/**
	 * Define the index in the image size array.
	 */
	private int currentDpiPos = 7;

	/**
	 * Increase the image size (dpi).
	 */
	public void increaseDpi() {
		if ( this.currentDpiPos < (this.dpiSizes.length - 1) ) {
			++this.currentDpiPos;
		}
	}

	/**
	 * Decrease the image size (dpi).
	 */
	public void decreaseDpi() {
		if (this.currentDpiPos > 0) {
			--this.currentDpiPos;
		}
	}

	public int getImageDpi() {
		return this.dpiSizes[this.currentDpiPos];
	}

	/**
	 * The source of the graph written in dot language.
	 */
	private StringBuilder graph = new StringBuilder();

	/**
	 * Constructor: creates a new GraphViz object that will contain
	 * a graph.
	 */
	public GraphViz() {
		// empty
	}

	/**
	 * Returns the graph's source description in dot language.
	 * @return Source of the graph in dot language.
	 */
	public String getDotSource() {
		return this.graph.toString();
	}

	/**
	 * Adds a string to the graph's source (without newline).
	 */
	public void add(String line) {
		this.graph.append(line);
	}

	/**
	 * Adds a string to the graph's source (with newline).
	 */
	public void addln(String line) {
		this.graph.append(line + "\n");
	}

	/**
	 * Adds a newline to the graph's source.
	 */
	public void addln() {
		this.graph.append('\n');
	}

	public void clearGraph(){
		this.graph = new StringBuilder();
	}

	/**
	 * Returns the graph as an image in binary format.
	 * @param dot_source Source of the graph to be drawn.
	 * @param type Type of the output image to be produced, e.g.: gif, dot, fig, pdf, ps, svg, png.
	 * @param representationType Type of how you want to represent the graph:
	 * <ul>
	 * 	<li>dot</li>
	 * 	<li>neato</li>
	 * 	<li>fdp</li>
	 * 	<li>sfdp</li>
	 * 	<li>twopi</li>
	 * 	<li>circo</li>
	 * </ul>
	 * @see http://www.graphviz.org under the Roadmap title
	 * @return A byte array containing the image of the graph.
	 */
	public byte[] getGraph(String dot_source, String type, String representationType)
	{
		File dot;
		byte[] img_stream = null;

		try {
			dot = writeDotSourceToFile(dot_source);
			if (dot != null)
			{
				img_stream = get_img_stream(dot, type, representationType);
				if (dot.delete() == false) 
					System.err.println("Warning: " + dot.getAbsolutePath() + " could not be deleted!");
				return img_stream;
			}
			System.out.println("Failed to convert graph");
			return null;
		} catch (java.io.IOException ioe) { return null; }
	}

	/**
	 * Writes the graph's image in a file.
	 * @param img   A byte array containing the image of the graph.
	 * @param file  Name of the file to where we want to write.
	 * @return Success: 1, Failure: -1
	 */
	public int writeGraphToFile(byte[] img, String file)
	{
		File to = new File(file);
		return writeGraphToFile(img, to);
	}

	/**
	 * Writes the graph's image in a file.
	 * @param img   A byte array containing the image of the graph.
	 * @param to    A File object to where we want to write.
	 * @return Success: 1, Failure: -1
	 */
	public int writeGraphToFile(byte[] img, File to)
	{
		try {
			FileOutputStream fos = new FileOutputStream(to);
			fos.write(img);
			fos.close();
		} catch (java.io.IOException ioe) { return -1; }
		return 1;
	}

	/**
	 * It will call the external dot program, and return the image in
	 * binary format.
	 * @param dot Source of the graph (in dot language).
	 * @param type Type of the output image to be produced, e.g.: gif, dot, fig, pdf, ps, svg, png.
	 * @param representationType Type of how you want to represent the graph:
	 * <ul>
	 * 	<li>dot</li>
	 * 	<li>neato</li>
	 * 	<li>fdp</li>
	 * 	<li>sfdp</li>
	 * 	<li>twopi</li>
	 * 	<li>circo</li>
	 * </ul>
	 * @see http://www.graphviz.org under the Roadmap title
	 * @return The image of the graph in .gif format.
	 */
	private byte[] get_img_stream(File dot, String type, String representationType)
	{
		File img;
		byte[] img_stream = null;

		try {
			img = File.createTempFile("graph_", "."+type, new File(GraphViz.TEMP_DIR));
			Runtime rt = Runtime.getRuntime();

			// patch by Mike Chenault
			// representation type with -K argument by Olivier Duplouy
			String[] args = {DOT, "-T"+type, "-K"+representationType, "-Gdpi="+dpiSizes[this.currentDpiPos], dot.getAbsolutePath(), "-o", img.getAbsolutePath()};
			Process p = rt.exec(args);
			p.waitFor();

			FileInputStream in = new FileInputStream(img.getAbsolutePath());
			img_stream = new byte[in.available()];
			in.read(img_stream);
			// Close it if we need to
			if( in != null ) in.close();

			if (img.delete() == false) 
				System.err.println("Warning: " + img.getAbsolutePath() + " could not be deleted!");
		}
		catch (java.io.IOException ioe) {
			System.err.println("Error:    in I/O processing of tempfile in dir " + GraphViz.TEMP_DIR+"\n");
			System.err.println("       or in calling external command");
			ioe.printStackTrace();
		}
		catch (java.lang.InterruptedException ie) {
			System.err.println("Error: the execution of the external program was interrupted");
			ie.printStackTrace();
		}

		return img_stream;
	}

	/**
	 * Writes the source of the graph in a file, and returns the written file
	 * as a File object.
	 * @param str Source of the graph (in dot language).
	 * @return The file (as a File object) that contains the source of the graph.
	 */
	private File writeDotSourceToFile(String str) throws java.io.IOException
	{
		File temp;
		try {
			temp = File.createTempFile("graph_", ".dot.tmp", new File(GraphViz.TEMP_DIR));
			temp.deleteOnExit();
			FileWriter fout = new FileWriter(temp);
			fout.write(str);
			fout.close();
		}
		catch (Exception e) {
			System.err.println("Error: I/O error while writing the dot source to temp file!");
			return null;
		}
		return temp;
	}

	/**
	 * Returns a string that is used to start a graph.
	 * @return A string to open a graph.
	 */
	public String start_graph() {
		return "digraph G {";
	}

	/**
	 * Returns a string that is used to end a graph.
	 * @return A string to close a graph.
	 */
	public String end_graph() {
		return "}";
	}

	/**
	 * Takes the cluster or subgraph id as input parameter and returns a string
	 * that is used to start a subgraph.
	 * @return A string to open a subgraph.
	 */
	public String start_subgraph(int clusterid) {
		return "subgraph cluster_" + clusterid + " {";
	}

	/**
	 * Returns a string that is used to end a graph.
	 * @return A string to close a graph.
	 */
	public String end_subgraph() {
		return "}";
	}

	/**
	 * Read a DOT graph from a text file.
	 * 
	 * @param input Input text file containing the DOT graph
	 * source.
	 */
	public void readSource(String input)
	{
		StringBuilder sb = new StringBuilder();

		try
		{
			FileInputStream fis = new FileInputStream(input);
			DataInputStream dis = new DataInputStream(fis);
			BufferedReader br = new BufferedReader(new InputStreamReader(dis));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			dis.close();
		} 
		catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}

		this.graph = sb;
		System.out.println(sb);
		System.out.println("loaded");
	}
	
	public static void main(String[] args){
			//String dotfile = "/Users/julianhough/git/simple_rnn_disf/rnn_disf_detection/disf_decoder/models/disfluency_trp_simple.dot";
			//String filename = "/Users/julianhough/git/simple_rnn_disf/rnn_disf_detection/disf_decoder/models/disfluency_trp_simple.png";
			String dotfile = "C:\\Users\\Julian\\git\\simple_rnn_disf\\rnn_disf_detection\\disf_decoder\\models\\disfluency_trp_simple.dot"; 
			String filename = "C:\\Users\\Julian\\git\\simple_rnn_disf\\rnn_disf_detection\\disf_decoder\\models\\disfluency_trp_simple_new.png"; 
			String adir = System.getProperty("user.dir");
	        System.out.println("current dir = " + adir);
			try {
				System.out.println(GraphViz.TEMP_DIR);
				File dir = new File(GraphViz.TEMP_DIR);
				File temp = File.createTempFile("graph_", ".dot.tmp", dir);
				temp.deleteOnExit();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			   
			
			
			
			
			
			//System.out.println(lattice);
			   GraphViz gv = new GraphViz();
			   //gv.readSource(dotfile);
			   System.out.println(gv.TEMP_DIR);
			   System.out.println(gv.configFile);
			   System.out.println(gv.osName);
			   System.out.println(gv.DOT);
	
				gv.addln(gv.start_graph());
				
				//for (Object o : lattice.edges()){
				//	Edge edge = (Edge) o;
				//	String connection = Integer.toString(lattice.nodeLabel(((Node) edge.sink()))) + " -> " + Integer.toString(lattice.nodeLabel(((Node) edge.source())));
				//	gv.addln(connection);
					
				//}
				
				gv.addln("A -> B;");
				gv.addln("A -> C;");
				gv.addln(gv.end_graph());
				System.out.println(gv.getDotSource());

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
				System.out.println(gv.getGraph(gv.getDotSource(), type, repesentationType));
				gv.writeGraphToFile( gv.getGraph(gv.getDotSource(), type, repesentationType), out );
			
	}

} // end of class GraphViz
