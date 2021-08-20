package qmul.ds.formula;

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
	
	protected Variable var;
	protected RDFFormula body;																

	
	
	public RDFLambdaAbstract(RDFLambdaAbstract la)
	{
		//TODO
	}

	/**
	 * 
	 * @param variable
	 * @param body
	 */
	public RDFLambdaAbstract(String variable, String body) {
		// TODO Auto-generated constructor stub
	}


	@Override
	public Formula betaReduce(Formula argument) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RDFLambdaAbstract substitute(Formula f1, Formula f2) {
		
		return null;
	}

	
//	public static void main(String a[])
//	{
//		RDFLambdaAbstract rla = "run";
//		Variable x = new Variable("x");
//		
//		RDFGraph john = new RDFGraph("equivalant of 'john' in RDF");
//		
//		RDFGraph result = rla.substitute(x, john);
//		
//		
//	}
	
	@Override
	public RDFFormula clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int toUniqueInt() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	public String toString()
	{
		// TODO
		return null;
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
	

}
