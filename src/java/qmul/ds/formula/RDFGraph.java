package qmul.ds.formula;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDF;

import qmul.ds.tree.Tree;
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
	
	public RDFGraph(String turtle, String prefixes)
	{
		// THIS IS TURTLE SPECIFIC
		Model turtleModel = ModelFactory.createDefaultModel();
		String combined = prefixes==null?turtle:prefixes+"\n"+turtle;
		this.rdfModel = turtleModel.read(new ByteArrayInputStream(combined.getBytes()), null, "TTL");
		
		storeVariables(turtleModel);
		
	
	}
	
	public RDFGraph(String turtle)
	{
		this(turtle,null);
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
			Resource subj = stmt.getSubject();
			RDFNode obj = stmt.getObject();
			
			Resource objR = obj.isResource()?obj.asResource():null;
			
			String subjS = subj.toString();
			
			if (subjS.startsWith(RDFVariable.VAR_NAME_SPACE))
			{
				getVariables().add(new RDFVariable(subjS));
				
				
			}
			
			if (objR!=null && objR.toString().startsWith(RDFVariable.VAR_NAME_SPACE))
			{
				getVariables().add(new RDFVariable(objR.toString()));
			}
			
			//System.out.println("subj is: "+subj);
			//System.out.println("obj is: "+objR);
			
			
			//getVariables().add(new RDFVariable(stmt.getSubject().toString()));
			//getVariables().add(new RDFVariable(stmt.getObject().toString()));
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
	
	
	public RDFGraph freshenVars(Tree t)
	{
		
		//TODO: Arash
		
		return null;
		
	}
	
	
	public static void main(String args[])
	{
		// one triple stating that a URI (in this case a UUID) is a person.
		String prefix = "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
				+ "@prefix schema: <http://schema.org/> .\n"
				+ "@prefix dsrdf: <http://wallscope.co.uk/ontology/dsrdf/> .\n"
				+ "@prefix var: <http://wallscope.co.uk/ontology/var/> .";
		
		String janeTurtle = 
				  "var:x\n"
				+ "a schema:Person;\n"
				+ "rdfs:label \"Jane\"@en .\n"
				+ "\n"
				+ "var:y\n"
				+ "a schema:Person;\n"
				+ "rdfs:label \"John\"@en .\n"
				+ "\n"
				+ "var:e\n"
				+ "a schema:Action;\n"
				+ "rdfs:label \"run\"@en;\n"
				+ "a dsrdf:Head;\n"
				+ "schema:agent var:x;\n"
				+ "schema:object var:y .";
		
		RDFGraph janeGraph = new RDFGraph(janeTurtle, prefix);
		
		RDFDataMgr.write(System.out, janeGraph.getModel(), Lang.TURTLE);
		
		System.out.println("Variables are:");
		System.out.println(janeGraph.getVariables());
		
	}
	
	
	
	
}
