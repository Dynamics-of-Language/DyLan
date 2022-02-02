/**
 * 
 */
package qmul.ds.formula.rdf;

import org.apache.jena.rdf.model.Property;

/**
 * @author angus
 *
 */
public class RDFJungEdge {
	
	private Property p=null;

	/**
	 * 
	 */
	public RDFJungEdge(Property p) {
		this.p=p;
	}

	public Property getProperty() {
		return p;
	}

	public void setProperty(Property p) {
		this.p = p;
	}

}
