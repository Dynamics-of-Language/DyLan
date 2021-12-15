package qmul.ds.formula;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	
	/**
	 * Assuming that RDFGraph is specified in Turtle, and that the spec is enclosed in {}
	 */
	public static final String RDFGraphPattern = "\\{((.|\\R)+)\\}";
	 
	
	
	public static final String DSRDF_NAMESPACE = "http://wallscope.co.uk/ontology/dsrdf/";
	public static final String DSRDF_PREFIX = "@prefix dsrdf: <"+DSRDF_NAMESPACE+">";
	public static final String VAR_PREFIX = "@prefix var: <"+RDFVariable.VAR_NAMESPACE+">";
	
	public static final String DEFAULT_RDF_PREFIX = "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> ."
			+ "@prefix schema: <http://schema.org/> ."
			+ DSRDF_PREFIX
			+ VAR_PREFIX;
	
	
	public RDFGraph(Model rdfm)
	{
		this.rdfModel = rdfm;

		storeVariables(rdfm);
	}
	
	/**
	 * Create an RDFGraph object from Turtle specification. Assumes that the Turtle specification
	 * is enclosed in {...} 
	 * @param turtle
	 * @param prefixes
	 */
	public RDFGraph(String turtle, String prefixes)
	{
		
		Model turtleModel = ModelFactory.createDefaultModel();
		
		
		Pattern p = Pattern.compile(RDFGraphPattern);
		Matcher m = p.matcher(turtle);
		
		
		if (m.matches())
		{
			String combined = (prefixes == null || prefixes.isEmpty())?m.group(1):prefixes+"\n"+m.group(1);
			this.rdfModel = turtleModel.read(new ByteArrayInputStream(combined.getBytes()), null, "TTL");
			storeVariables(turtleModel);
		}
		else
		{
			throw new IllegalArgumentException("Turtle RDF formula should be enclosed in {}");
		}
		
	
	}
	
	public RDFGraph(String turtle)
	{
		this(turtle, DEFAULT_RDF_PREFIX);
	}
	
	public RDFGraph(RDFGraph rdf)
	{
		this.rdfModel = ModelFactory.createDefaultModel().add(rdf.rdfModel);
		storeVariables(this.rdfModel);
	}
	
	
	
	
	@Override
	public RDFFormula substitute(Formula f1, Formula f2)
	{
		RDFGraph newGraph = new RDFGraph(this);
		
		if (!(f1 instanceof RDFVariable && f2 instanceof RDFVariable))
		{
			throw new IllegalArgumentException("Only RDFVariable substitutions are supported");
		}
		RDFVariable v1 = (RDFVariable) f1;
		RDFVariable v2 = (RDFVariable) f2;
		
		ResourceUtils.renameResource(newGraph.findResource(v1.getFullName()), v2.getFullName());
		
		
		
		newGraph.variables.remove(v1);
		
		
		
		newGraph.variables.add(v2);
	
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
			
			if (subjS.startsWith(RDFVariable.VAR_NAMESPACE))
			{
				getVariables().add(new RDFVariable(subjS.substring(RDFVariable.VAR_NAMESPACE.length(), subjS.length())));
				
				
			}
			
			if (objR!=null && objR.toString().startsWith(RDFVariable.VAR_NAMESPACE))
			{
				String objRS=objR.toString();
				getVariables().add(new RDFVariable(objRS.substring(RDFVariable.VAR_NAMESPACE.length(), objRS.length()).toString()));
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
	
	
	public RDFGraph conjoin(Formula g)
	{
		if (g instanceof RDFGraph)
		{
			return this.union((RDFGraph) g);
		}
		else
		{
			throw new UnsupportedOperationException("Can only conjoin with RDFGraph");
			
		}
		
	}
	
	
	
	
	public RDFVariable getHead()
	{
		
		Resource mainHead;
		Resource mainHeadType = this.rdfModel.getResource(DSRDF_NAMESPACE + "Head");
		Selector headSelector = new SimpleSelector(null, RDF.type, mainHeadType);
		StmtIterator headIter = this.rdfModel.listStatements(headSelector);
		if (!headIter.hasNext()) {
			return null;
		} else {
			Statement headStmt = headIter.nextStatement();
			mainHead = headStmt.getSubject();
		}
		return new RDFVariable(mainHead);
	}
	
	
	public RDFGraph removeHead()
	{
		
		RDFGraph g = new RDFGraph(this);
		
		Resource mainHeadType = g.rdfModel.getResource(DSRDF_NAMESPACE + "Head");
		Selector headSelector = new SimpleSelector(null, RDF.type, mainHeadType);
		StmtIterator headIter = g.rdfModel.listStatements(headSelector);
		Statement headStmt;
		if (!headIter.hasNext()) {
			throw new IllegalStateException("No head in this graph:"+this);
		} else {
			headStmt = headIter.nextStatement();
			//mainHead = headStmt.getSubject();
		}
		
		g.rdfModel.remove(headStmt);
		
		return g;
		
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
	
	protected String removePrefix(String turtle)
	{
		
		String[] lines = turtle.split("\n");
		String result="";
		for(String line: lines)
		{
			if (line.startsWith("@prefix"))
				continue;
			result+=line+"\n";
			
		}
		return result;
		
	}
	
	public String toString()
	{
		StringWriter writer = new StringWriter();
		
		RDFDataMgr.write(writer, this.rdfModel, Lang.TURTLE);
		
		return "{"+removePrefix(writer.toString())+"}";
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
		
		//TODO: Problem (maybe not serious): RDF statements don't have DS types .... 
		
		return null;
		
	}
	
	
	public static void main(String args[])
	{
		
		String jLikesJ = 
				  "{var:x "
				+ "a schema:Person;"
				+ "rdfs:label \"Jane\"@en ."
				+ "var:y "
				+ "a schema:Person;"
				+ "rdfs:label \"John\"@en ."
				+ "var:e "
				+ "a schema:Action;"
				+ "rdfs:label \"like\"@en;"
				+ "a dsrdf:Head;"
				+ "schema:agent var:x;"
				+ "schema:object var:y.}";
		
		//String jane = "{var:x a schema:Person;rdfs:label \"Jane\"@en.}";
		
		//RDFGraph janeGraph = new RDFGraph(janeTurtle, prefix);
		RDFGraph janeGraph = (RDFGraph) Formula.create(jLikesJ);
		
		
		System.out.println(janeGraph.removeHead());
		
//		System.out.println("Head is: "+janeGraph.getHead());
//		
//		RDFFormula subst = janeGraph.substitute(new RDFVariable("x"), new RDFVariable("x1"));
//		
//		RDFDataMgr.write(System.out, janeGraph.getModel(), Lang.TURTLE); //write graph
//		
//		System.out.println(subst);
//		
//		System.out.println("Variables are:");
//		System.out.println(subst.getVariables());
		
	}
	
	
	
	
}
