package qmul.ds.formula;

import java.io.ByteArrayInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
/**
 * 
 * @author Arash, Angus
 *
 */
public class RDFGraph extends RDFFormula {
	
	
	private static final long serialVersionUID = 1L;
	protected Model rdfModel;
//	protected Resource HEAD;
	
	
	public RDFGraph(Model rdfm)
	{
		this.rdfModel = rdfm;

		storeVariables(rdfm);
	}
	
	public RDFGraph(String turtle)
	{
		// THIS IS TURTLE SPECIFIC
		Model turtleModel = ModelFactory.createDefaultModel();
		this.rdfModel = turtleModel.read(new ByteArrayInputStream(turtle.getBytes()), null, "TTL");
		
		storeVariables(turtleModel);
	}
	
	public RDFGraph(RDFGraph rdf)
	{
		ModelFactory.createDefaultModel().add(rdf.rdfModel);
	}
	
	
	@Override
	public RDFFormula substitute(Formula f1, Formula f2)
	{
		RDFGraph newGraph = new RDFGraph(this);
		
		ResourceUtils.renameResource(newGraph.findResource(f1.toString()), f2.toString());
		
		return newGraph;
	}
	
	public Resource findResource(String nodeID)
	{
		Resource node = this.rdfModel.getResource(nodeID);
		return node;
	}
	
	public void storeVariables(Model m)
	{
		StmtIterator mIter = m.listStatements();
		while (mIter.hasNext()) {
			Statement stmt = mIter.nextStatement();
			getVariables().add(new RDFVariable(stmt.getSubject().toString()));
			getVariables().add(new RDFVariable(stmt.getObject().toString()));
		}
	}

	/**
	 * 
	 * @param graph
	 * @return
	 */
	public RDFGraph union(RDFGraph graph)
	{
		return new RDFGraph(this.rdfModel.union(graph.getModel()));
	}
	
	public String getHeadID()
	{
		String dsrdf = "http://wallscope.co.uk/ontology/dsrdf/";
		
		Resource mainHead;
		Resource mainHeadType = this.rdfModel.getResource(dsrdf + "Head");
		Selector headSelector = new SimpleSelector(null, RDF.type, mainHeadType);
		StmtIterator headIter = this.rdfModel.listStatements(headSelector);
		if (!headIter.hasNext()) {
			return null;
		} else {
		Statement headStmt = headIter.nextStatement();
		mainHead = headStmt.getSubject();
		}
		return mainHead.getURI();
	}

	@Override
	public int toUniqueInt()
	{
		// TODO 
		return 0;
	}
	
	@Override
	public boolean subsumesMapped(Formula other, HashMap<Variable, Variable> map)
	{
		// TODO
		return false;
	}
	
	
	public String toString()
	{
		// TODO
		return rdfModel.toString();
	}
	
	public Model getModel()
	{
		// TODO
		return rdfModel;
	}
	
	public int hashCode()
	{
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
		// one triple stating that a URI (in this case a UUID) is a person.
		String testString = "@prefix schema: <http://schema.org/> . <urn:uuid:51da23d8-68ed-4be7-b55a-221534dc142b> a schema:Person .";
		
		RDFGraph testGraph = new RDFGraph(testString);
		
		RDFDataMgr.write(System.out, testGraph.getModel(), Lang.TURTLE);
		
	}
	
	
}
