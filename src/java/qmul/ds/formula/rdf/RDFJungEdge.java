/**
 * 
 */
package qmul.ds.formula.rdf;

import java.awt.BasicStroke;
import java.awt.Stroke;

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
	
	public String toString() {
		return p.toString();
	}
	
	public String getEdgeLabel() {
		return p.getLocalName();
	}

	public Stroke getEdgeStroke() {
		return new BasicStroke();

	}

}
