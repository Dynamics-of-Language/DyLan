/**
 * 
 */
package qmul.ds.formula.rdf;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * @author angus
 *
 */
public class RDFJungGraph <T extends RDFJungNode, E extends RDFJungEdge> extends DirectedSparseMultigraph<T, E> {
	
	private DirectedSparseMultigraph dsm=null;
	
	public RDFJungGraph(Model m) {
		
		DirectedSparseMultigraph dsm = new DirectedSparseMultigraph();
		
		StmtIterator iter = m.listStatements();
		while (iter.hasNext()) {
			Statement stmt = iter.next();
			RDFJungNode subj = new RDFJungNode(stmt.getSubject());
			RDFJungEdge prop = new RDFJungEdge(stmt.getPredicate());
			RDFJungNode obj = new RDFJungNode(stmt.getObject());
			
			Pair p = new Pair(subj, obj);
			
			dsm.addEdge(prop, p, EdgeType.DIRECTED);
			}
		this.dsm=dsm;
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
		RDFJungGraph rjg = new RDFJungGraph(jGraph.getModel());
		
//		RDFDataMgr.write(System.out, jGraph.getModel(), Lang.TURTLE);
		System.out.println(rjg.dsm);
	}

}
