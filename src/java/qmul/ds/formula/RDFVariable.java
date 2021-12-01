/**
 * 
 */
package qmul.ds.formula;



/**
 * @author angus
 *
 */
public class RDFVariable extends Variable {
	
	//Name Space for RDF Variables
	
	protected static final String VAR_NAME_SPACE = "http://wallscope.co.uk/ontology/var/";
	
	
	public RDFVariable (String nodeID) {
		super.name = nodeID;
	}
	
	public String toString()
	{
		return super.toString();
		
	}
	
	
}
