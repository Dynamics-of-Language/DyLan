/**
 * 
 */
package qmul.ds.formula.rdf;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import qmul.ds.formula.Variable;
import qmul.ds.tree.Tree;
import qmul.ds.type.DSType;

/**
 * @author angus, arash
 *
 */
public class RDFVariable extends Variable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3542878573997978134L;
	//Name Space for RDF Variables
	
	protected static final String VAR_NAMESPACE = "http://dsrdf.com/ontology/var/";
	
	public RDFVariable(Variable v)
	{
		super(VAR_NAMESPACE + v.getName());
		
	}
	
	public RDFVariable(String var) {		
		
		super.name = VAR_NAMESPACE + var;
	}
	
	public RDFVariable(RDFNode variable)
	{
		if (!(variable instanceof Resource))
			throw new IllegalArgumentException("Cannot create an RDFVariable from a non-resource (e.g. literal):"+ variable);
		
		Resource var = (Resource) variable;
		
		if (var.getURI().startsWith(VAR_NAMESPACE))
			super.name = var.getURI();
		else
			throw new IllegalArgumentException("Doesn't seem to be a variable resource:"+var.getURI());
	}
	
	/**
	 * Variable factory; 
	 * @param variable
	 * @return
	 */
	public static RDFVariable getNewVariable(RDFNode variable)
	{
		if (!(variable instanceof Resource))
			throw new IllegalArgumentException("Cannot create an RDFVariable from a non-resource (e.g. literal):"+ variable);
		
		Resource var = (Resource) variable;
		
		if (var.getURI().startsWith(VAR_NAMESPACE) && var.getURI().substring(VAR_NAMESPACE.length(), var.getURI().length()).matches(RDFMetaVariable.META_RDFVARIABLE_PATTERN))
			return new RDFMetaVariable(var);
		else if (var.getURI().startsWith(VAR_NAMESPACE))
			return new RDFVariable(var);
		else
			throw new IllegalArgumentException("Doesn't seem to be a variable resource:"+var.getURI());
		
	}
	
	public String toString()
	{
		return getName();
	}
	
	
	public String getName()
	{
	
		String prefixed=super.getName();
		return prefixed.substring(VAR_NAMESPACE.length(), prefixed.length());
		
	}

	public String getFullName() {
		return this.name;
	}
	
	public static boolean isRDFVariable(RDFNode r)
	{
		return r.toString().startsWith(VAR_NAMESPACE);
		
	}
	
	public static boolean isRDFMetaVariable(RDFNode r)
	{
		String nameAndPrefix = r.toString();
		
		return nameAndPrefix.startsWith(VAR_NAMESPACE) 
				&& nameAndPrefix.substring(VAR_NAMESPACE.length(), nameAndPrefix.length()).matches(RDFMetaVariable.META_RDFVARIABLE_PATTERN);
		
	}
	
	/**
	 * 
	 * @return the DSType of this variable based on its name - currently only entity & event variables allowed
	 */
	public DSType getDSType() {
		
		if (getName().startsWith(Tree.ENTITY_VARIABLE_ROOT))
			return DSType.e;
		else if (getName().startsWith(Tree.EVENT_VARIABLE_ROOT))
			return DSType.es;
		else
			throw new IllegalStateException("RDFVariable must have either DS Type e or es.");
		
	}
	
	
	
	
}
