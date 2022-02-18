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
			this.l=n.asLiteral();
		}
		else {
			this.r=n.asResource();
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
	
	public String toString() {
		if (this.l != null) {
			return this.l.toString();
		}
		else if (this.r != null) {
			return this.r.getLocalName();
		}
		else {
			return null;
		}
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof RDFJungNode)) {
			return false;
		}
		RDFJungNode other = (RDFJungNode) o;
		if (l!= null) {
			return l.equals(other.l);
		}
		if (r!=null) {
			return r.equals(other.r);
		}
		return false;
	}
	
	public int hashCode() {
		if (l!=null) {
			return l.hashCode();
		}
		if (r!=null) {
			return r.hashCode();
		}
		return 0;
	}
	

}
