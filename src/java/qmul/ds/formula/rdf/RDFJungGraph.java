/**
 * 
 */
package qmul.ds.formula.rdf;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;

/**
 * @author angus
 *
 */
public class RDFJungGraph <T extends RDFJungNode, E extends RDFJungEdge> extends DirectedSparseMultigraph<T, E> {
	
	public RDFJungGraph(Model m) {
		
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
		
		RDFDataMgr.write(System.out, jGraph.getModel(), Lang.TURTLE);
	}

}
