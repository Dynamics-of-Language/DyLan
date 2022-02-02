/**
 * 
 */
package qmul.ds.formula.rdf;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

/**
 * @author angus
 *
 */
public class RDFJungNode {
	
	private Resource r=null;
	private Literal l=null;
	
	/**
	 * 
	 */
	
	public RDFJungNode(Resource r) {
		this.r=r;
	}
	
	public RDFJungNode(Literal l) {
		this.l=l;
	}
	
	public RDFJungNode(RDFNode n) {
		if (n.isLiteral()) {
			new RDFJungNode(n.asLiteral());
		}
		else {
			new RDFJungNode(n.asResource());
		}
	}

	public Resource getResource() {
		return r;
	}

	public void setResource(Resource r) {
		this.r = r;
	}

	public Literal getLiteral() {
		return l;
	}

	public void setLiteral(Literal l) {
		this.l = l;
	}
	

}
