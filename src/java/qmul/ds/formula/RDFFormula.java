/**
 * 
 */
package qmul.ds.formula;

/**
 * @author ae187
 * 
 * Abstract superclass of RDF formulae, namely RDFGraph and RDFLambdaAbstract
 *
 */
public abstract class RDFFormula extends Formula {

	/**
	 *  TODO: not finished ... 
	 */
	private static final long serialVersionUID = -2738227532139543921L;

	
	
	@Override
	public abstract RDFFormula substitute(Formula f1, Formula f2);

	@Override
	public abstract RDFFormula clone();



}
