/**
 * 
 */
package qmul.ds.formula.rdf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import qmul.ds.formula.Formula;
import qmul.ds.type.DSType;

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

	
	/**
	 * @param f1
	 * @param f2
	 * @return a new {@link Formula} resulting from substituting sub-formula f1 by
	 *         f2 in this {@link Formula}.
	 */
	public abstract RDFFormula substitute(Formula f1, Formula f2);
	
	
	
	public abstract RDFFormula union(RDFGraph g);
	
	public RDFFormula instantiate()
	{
		return this;
	}
	
	public abstract RDFFormula conjoin(Formula f);
	
	public static Map<DSType, RDFFormula> typeMap;
	static{
		Map<DSType, RDFFormula> map=new HashMap<DSType, RDFFormula>();
		map.put(DSType.e, (RDFFormula)Formula.create("{var:x a dsrdf:Head, schema:Thing.}"));

		map.put(DSType.cn, (RDFFormula) Formula.create("{var:x a dsrdf:Head, schema:Thing.}"));
		map.put(DSType.t, (RDFFormula) Formula.create("{var:e a dsrdf:Head.}"));
		// for underspec VP
		
		map.put(DSType.parse("e>cn"), (RDFFormula) Formula.create("G1^{var:G1 a dsrdf:Head.}"));
		map.put(DSType.parse("e>t"), (RDFFormula) Formula.create("G1^{var:e "
				+ "a schema:Action, dsrdf:Head;"
				+ "schema:agent var:G1.}"));
				
		
		map.put(DSType.parse("e>(e>t)"), (RDFFormula) Formula.create("G1^G2^{var:e "
				+ "a schema:Action, dsrdf:Head;"
				+ "schema:agent var:G2;"
				+ "schema:object var:G1.}"));
		
		
		//We won't need the following, for now ... needed for "I gave john the book"
		//map.put(DSType.parse("e>(e>(e>t))"), Formula.create("R3^R2^R1^(R1 ++ (R2 ++ (R3 ++ [head:es])))"));
		// for underspec adjunct e>t, see below, special case
		
		//For now, we don't need the below (two lines) (will need it for Grammar induction)
		//The reason we don't need them is that the determiner node never needs to be underspecified
		//while parsing.
		//map.put(DSType.parse("cn>e"), Formula.create("R1^[r:R1|x:e|head==x:e]"));
		//map.put(DSType.parse("cn>es"), Formula.create("R1^[r:R1|e1:es|head==e1:es]"));
		typeMap=Collections.unmodifiableMap(map);
		
	}
	
	


}
