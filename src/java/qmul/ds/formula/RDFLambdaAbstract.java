package qmul.ds.formula;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;

/**
 * 
 * @author arash, angus
 * 
 * A recursive RDF lambda function implementation with {@link Variable} 'var', and body 'body' of type {@link RDFFormula}.
 *
 */
public class RDFLambdaAbstract extends RDFFormula implements LambdaAbstract {
	



	private static final long serialVersionUID = 1268780767927342960L;

	private static Logger logger = Logger.getLogger(RDFLambdaAbstract.class);

	protected static final String LAMBDA_FUNCTOR = "^";
	private static final String UNICODE_LAMBDA_FUNCTOR = "\u03BB"; // lambda;
		
	public static final String RDF_VARIABLE_PATTERN = "G\\d*";//e.g. G1, G2, etc
	
	
	//RECURSIVE definition
	
	protected RDFVariable var; // this is the ID of the node which is supposed to collapse onto the argument
	protected RDFFormula body;																

	
	
	public RDFLambdaAbstract(RDFLambdaAbstract la)
	{
		if (la.body instanceof RDFGraph) {
			new RDFLambdaAbstract(la.var, new RDFGraph((RDFGraph) la.body));
		} else {
			new RDFLambdaAbstract(la.var, new RDFLambdaAbstract((RDFLambdaAbstract) la.body));
		}
	}

	/**
	 * 
	 * @param variable
	 * @param body
	 */
	
	public RDFLambdaAbstract(String variable, String formula)
	{
		this(new RDFVariable(variable), (RDFFormula) Formula.create(formula));
	}

	public RDFLambdaAbstract(RDFVariable variable, RDFFormula formula)
	{
		this.var = variable;
		getVariables().add(variable);
		this.body = formula;
		getVariables().addAll(formula.getVariables());
	}


	@Override
	public Formula betaReduce(Formula argument)
	{
		// Step 1: extract head id from argument = argHead
		RDFGraph argGraph = (RDFGraph) argument;
		String argHeadID = argGraph.getHeadID();
		RDFVariable argHead = new RDFVariable(argHeadID);
		
		// Step 2: change the ID of node @var to be argHead
		RDFLambdaAbstract substituted = (RDFLambdaAbstract) substitute(this.var, argHead);
		
		// Step 3: return this.union(argument)
		return substituted.body.union(argGraph);
	}
	
	public RDFGraph extractBody()
	{
		if (body instanceof RDFGraph) {
			return (RDFGraph) body;
		}
		return ((RDFLambdaAbstract) body).extractBody();
	}

	@Override
	public RDFFormula substitute(Formula f1, Formula f2)
	{
		return new RDFLambdaAbstract(this.var, this.body.substitute(f1, f2));
	}
	
	@Override
	public RDFFormula clone()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public RDFFormula union(RDFGraph g)
	{
		return new RDFLambdaAbstract(this.var, this.body.union(g));
	}

	@Override
	public int toUniqueInt()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	public String toString()
	{
		// G1^G2^[rdf model of the body]
		if (this.body instanceof RDFGraph) {
			return this.var + "^[" + this.body.toString() + "]";
		} else {
			return this.var + "^" + this.body.toString();
		}
	}
	
	public boolean equals(Object other)
	{
		
		//TODO
		return super.equals(other);
	}
	
	public int hashCode()
	{
		//TODO
		return super.hashCode();
		
	}
	
	public static void main (String array[])
	{
		RDFGraph jane = new RDFGraph(RDFDataMgr.loadModel("/home/angus/projects/DS-RDF/ds-examples/data/ex2-j.ttl"));
		RDFLambdaAbstract run = new RDFLambdaAbstract("G1", "{dsrdf:run\n"
				+ "a schema:Action ;\n"
				+ "a dsrdf:Head ;\n"
				+ "rdfs:label \"run\"@en ;\n"
				+ "schema:agent <G1> .}");
		
		RDFDataMgr.write(System.out, jane.rdfModel, Lang.TURTLE);
		System.out.print(run.body);
		RDFGraph reduced = (RDFGraph) run.betaReduce(jane);
		RDFDataMgr.write(System.out, reduced.getModel(), Lang.TURTLE);
	}
	

}
