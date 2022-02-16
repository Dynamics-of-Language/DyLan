/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action.atomic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import qmul.ds.Context;
import qmul.ds.ParserTuple;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.formula.Formula;
import qmul.ds.formula.rdf.RDFFormula;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.Label;

/**
 * The <tt>ttrput</tt> action... puts a TTRFormula (TTRRecordType or TTRLambdaAbstract..0
 * 
 * @author arash
 */
public class RDFFreshPut extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static Logger logger = Logger.getLogger(RDFFreshPut.class);
	public static final String FUNCTOR = "rdfput";

	private RDFFormula rdfF;

	public RDFFreshPut(RDFFormula f) {
		
		this.rdfF = f;
	}

	private static final Pattern TTR_FRESH_PUT_PATTERN = Pattern.compile("(?i)" + FUNCTOR + "\\((.+)\\)");

	/**
	 * @param string
	 *            a {@link String} representation e.g. put(ty(t)) as used in lexicon specs
	 */
	public RDFFreshPut(String string) {
		Matcher m = TTR_FRESH_PUT_PATTERN.matcher(string);
		if (m.matches()) {
			Formula f = Formula.create(m.group(1));
			if (f == null || !(f instanceof RDFFormula))
				throw new IllegalArgumentException("could not parse rdf or rdf lambda abstract:" + m.group(1));
			this.rdfF = (RDFFormula)f;
		} else {
			throw new IllegalArgumentException("unrecognised rdf freshput string");
		}
	}

	public <E extends DAGEdge, U extends DAGTuple, T extends Tree> T exec(T tree, Context<U,E> context)
	{
		Node node = tree.getPointedNode();
		FormulaLabel l = null;
		for (Label label : node) {
			if (label instanceof FormulaLabel) {
				l = (FormulaLabel) label;
				break;
			}
		}
		logger.debug("Before freshening vars:"+rdfF);
		Formula fresh = rdfF.freshenVars(context);
		logger.debug("After freshening vars:"+fresh);
		logger.debug("");
		if (l != null) {
			logger.warn("trying to add rdf formula:" + fresh + " to node with already existing formula:" + node);
			logger.warn("returning tree intact");
			return tree;
		}
		
		node.add(new FormulaLabel(fresh.instantiate()));
		return tree;
		
		
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.atomic.Effect#exec(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	@Override
	public <T extends Tree> T execTupleContext(T tree, ParserTuple context) {
		Node node = tree.getPointedNode();
		FormulaLabel l = null;
		for (Label label : node) {
			if (label instanceof FormulaLabel) {
				l = (FormulaLabel) label;
				break;
			}
		}
		Formula fresh = rdfF.freshenVars(tree);
		if (l != null) {
			logger.warn("trying to add rdf formula:" + fresh + " to node with already existing formula:" + node);
			logger.warn("returning tree intact");
			return tree;
		}
		
		node.add(new FormulaLabel(fresh.instantiate()));
		return tree;
	}

	/**
	 * @return the RDF Formula to put
	 */
	public Formula getRDFFormula() {
		return rdfF;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR + "(" + rdfF + ")";
	}

	public Effect instantiate() {
		return new RDFFreshPut(this.rdfF.instantiate());
	}

	public boolean equals(Object o)
	{
		if (!(o instanceof RDFFreshPut))
			return false;
		
		RDFFreshPut oth=(RDFFreshPut)o;
		if(this.rdfF==null)
			return oth.rdfF==null;
		
		if (oth.rdfF==null)
			return false;
		
		return this.rdfF.subsumes(oth.rdfF)&&oth.rdfF.subsumes(this.rdfF);
		
	}
	
	public static void main(String a[])
	{
		Formula e=Formula.create("[x==X:e|head==x:e]");
		
		System.out.println(e);
	}
}
