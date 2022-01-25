/**
 * 
 */
package qmul.ds.formula.rdf;

import qmul.ds.formula.Formula;
import qmul.ds.formula.IncrementalFormula;

/**
 * @author ae187
 * 
 * Abstract superclass of RDF formulae, namely RDFGraph and RDFLambdaAbstract
 *
 */
public abstract class RDFFormula extends IncrementalFormula {

	/**
	 *  TODO: not finished ... 
	 */
	private static final long serialVersionUID = -2738227532139543921L;

	
	
	@Override
	public abstract RDFFormula substitute(Formula f1, Formula f2);

	@Override
	public abstract RDFFormula clone();
	
	@Override
	public abstract String toString();

	public abstract RDFFormula union(RDFGraph g);
	
	
	public abstract RDFFormula conjoin(Formula g);


}