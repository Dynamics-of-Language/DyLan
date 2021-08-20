package qmul.ds.formula;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
/**
 * 
 * @author Arash, Angus
 *
 */
public class RDFGraph extends RDFFormula {
	
	
	private static final long serialVersionUID = 1L;
	protected Model rdfModel;
	protected Resource HEAD;
	
	
	public RDFGraph(Model rdfm, Resource h)
	{
		this.rdfModel = rdfm;
		this.HEAD = h;
	}
	
	public RDFGraph(String turtle)
	{
		this.rdfModel = ModelFactory.createDefaultModel();
		InputStream i = new ByteArrayInputStream(turtle.getBytes(StandardCharsets.UTF_8));
		rdfModel.read(i, "TURTLE");
		try {
			i.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public RDFGraph(RDFGraph rdf)
	{
		//TODO a deep copy of rdf
		
	}
	
	
	@Override
	public RDFFormula substitute(Formula f1, Formula f2) {
		// TODO 
		return null;
	}

	

	@Override
	public int toUniqueInt() {
		// TODO 
		return 0;
	}
	
	@Override
	public boolean subsumesMapped(Formula other, HashMap<Variable, Variable> map) {
		// TODO
		return false;
	}
	
	
	public String toString() {
		// TODO
		return null;
	}
	
	public int hashCode() {
		//TODO	Currently just toString().hashCode()
		return super.hashCode();
		
	}
	
	public boolean equals(Object other)
	{
		//TODO
		return super.equals(other);
		
	}

	public static RDFGraph parse(String string) {
		// TODO Note that our Formula factory, Formula.create relies on this method returning null 
		// if the string argument is not a valid TUTLE RDF formula string
		return null;
	}
	

	@Override
	public RDFFormula clone() {
		return new RDFGraph(this);
	}
	
	public static void main(String args[])
	{
		RDFGraph g = RDFGraph.parse("TURTLE String .... ");

		
		
	}
	
	
}
