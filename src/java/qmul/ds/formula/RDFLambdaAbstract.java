package qmul.ds.formula;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
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
		
	public static final String RDF_ABSTRACT_VARIABLE_PATTERN = "G\\d*";//e.g. G1, G2, etc
	
	public static final String RDF_LAMBDA_ABSTRACT_PATTERN = "(" + RDF_ABSTRACT_VARIABLE_PATTERN + ")" 
	+ Pattern.quote(FOLLambdaAbstract.LAMBDA_FUNCTOR) + "(.*)";
	
	
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

	/**
	 * String instantiation of an @RDFLambdaAbstract, according to the regex @RDF_LAMBDA_ABSTRACT_PATTERN
	 * @param spec
	 */
	public RDFLambdaAbstract(String spec)
	{
		Matcher m = Pattern.compile(RDF_LAMBDA_ABSTRACT_PATTERN).matcher(spec);
		
		if (m.matches()) {

			this.var = new RDFVariable(m.group(1));
			getVariables().add(this.var);
			this.body = (RDFFormula)Formula.create(m.group(2));
			getVariables().addAll(this.body.getVariables());
			
		}
		else
			throw new IllegalArgumentException("Illegal RDFLambdaAbstract Specification: "+spec);
		
	}
	@Override
	public RDFFormula betaReduce(Formula argument)
	{
		// Step 1: extract head id from argument = argHead
		RDFGraph argGraph = (RDFGraph) argument;
	
		RDFVariable argHead = argGraph.getHead();
		
		System.out.println("Arg head is:"+argHead);
		// Step 2: change the ID of node @var to be argHead
		RDFLambdaAbstract substituted = (RDFLambdaAbstract) substitute(this.var, argHead);
		
		System.out.println("After local head substitution:"+substituted);
		
		// Step 3: return this.union(argument)
		return substituted.body.union(argGraph.removeHead());
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
		return this.var + "^" + this.body.toString();
		
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
		RDFLambdaAbstract like = new RDFLambdaAbstract("G1^G2^{var:e "
				+ "a schema:Action;"
				+ "a dsrdf:Head;"
				+ "rdfs:label \"like\"@en;"
				+ "schema:agent var:G2;"
				+ "schema:object var:G1.}");
		
		
		String jane = "{var:x "
				+ "a schema:Person;"
				+ "rdfs:label \"Jane\"@en;"
				+ "a dsrdf:Head.}";
		
		String john = "{var:y "
				+ "a schema:Person;"
				+ "rdfs:label \"John\"@en;"
				+ "a dsrdf:Head.}";
		
		
		RDFGraph janeGraph = new RDFGraph(jane);
		RDFGraph johnGraph = new RDFGraph(john);
		
		
		RDFLambdaAbstract reduced = (RDFLambdaAbstract)like.betaReduce(janeGraph);
		
		RDFFormula reduced2 = reduced.betaReduce(johnGraph);
		
		System.out.println("reduced is: \n"+reduced2);
		
		//RDFDataMgr.write(System.out, jane.rdfModel, Lang.TURTLE);
		//System.out.print(run.body);
		//RDFGraph reduced = (RDFGraph) run.betaReduce(jane);
		//RDFDataMgr.write(System.out, reduced.getModel(), Lang.TURTLE);
	}
	

}
