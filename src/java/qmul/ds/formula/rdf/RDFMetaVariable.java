package qmul.ds.formula.rdf;

import java.util.HashMap;

import org.apache.jena.rdf.model.Resource;

import qmul.ds.formula.Formula;
import qmul.ds.formula.Variable;


/**
 * An RDF Meta variable, akin to MetaTTRLabel. An RDF meta-variables will have an RDFVariable as its value. This class does not implement 
 * Meta<.> because it does not need to: IfThenElse actions do not interact directly with RDFMetaVariables, but only indirectly through the
 * RDFGraph that contains them.
 * 
 * Meta-var names should be the RDF Var prefix + a capital letter followed by digits. The letter should, by contract, correspond to the DS type 
 * of the entity in question: E for event terms, X for entity terms; e.g. E12, X3, etc.
 * 
 * If unassigned, RDFMetaVariables will instantiate on equality checking against a normal RDFVariable.
 *  
 * @author arash
 *
 */
public class RDFMetaVariable extends RDFVariable {

	public static String META_RDFVARIABLE_PATTERN = "[EX]\\d*";
	
	private static final long serialVersionUID = 1L;
	
	private RDFVariable value=null;
	
	public RDFMetaVariable(RDFMetaVariable o)
	{
		super(o.name);
		this.value = new RDFVariable(o.value);
	}

	public RDFMetaVariable(Resource var) {
		super(var);
		
	}
	
	public RDFMetaVariable(String var) {		
		super(var);
		
	}
	
	//uses super.hashCode which is based on the name only.
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + ((value == null) ? 0 : value.hashCode());
//		result = prime * result + ((name == null) ? 0 : name.hashCode());
//		return result;
//	}
	
	public void set(Variable value)
	{
		if (!(value instanceof RDFVariable))
			throw new IllegalArgumentException();
		
		this.value = (RDFVariable)value;
		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#clone()
	 */
	@Override
	public RDFMetaVariable clone() {
		return new RDFMetaVariable(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (value == null) {
			return getName();
		} else {
			return "("+getName() + "=" + value+")";
		}
	}

	@Override
	public int toUniqueInt() {
		return this.value==null?name.hashCode():name.hashCode()+value.toUniqueInt();
		
	}
	
	public RDFVariable getValue()
	{
		return value;
	}
	
	//TODO: possibly add its own subsumesMapped. Currently going with the default one in Variable.
	
	public RDFVariable instantiate()
	{
		if (value == null)
			return this;
		
		return value;
	}
	

}
