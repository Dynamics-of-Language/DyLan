/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action.atomic;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.Context;
import qmul.ds.ParserTuple;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.SpeechActInferenceGrammar;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.SpeechActLabel;

/**
 * Atomic action for speech act inference according to rules specified (optionally) in speech-act-inference-grammar.txt
 * under each grammar resource folder.
 * 
 * This action works only with recent versions of the implementations that pass Context objects to the exec method.
 * 
 * @author arash.
 */
public class InferSpeechAct extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String FUNCTOR = "infer-sa";
	protected static Logger logger=Logger.getLogger(InferSpeechAct.class);

	

	/**
	 * @param modality
	 */
	public InferSpeechAct() {
		
	}

	

	/**
	 * @param string
	 *            a {@link String} representation e.g. go(/\1) as used in lexicon specs
	 */
	public InferSpeechAct(String string) {
		
		if (!string.toLowerCase().equals(FUNCTOR.toLowerCase())) 
			throw new IllegalArgumentException("unrecognised infer-sa string:" + string);
		
	}

	
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.atomic.Effect#exec(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	@Override
	public <T extends Tree> T execTupleContext(T tree, ParserTuple context) {
		throw new IllegalArgumentException("Operation Not supported with tuple as context");
	}
	
	@Override
	public <E extends DAGEdge, U extends DAGTuple, T extends Tree> T exec(T tree, Context<U,E> context)
	{
		SpeechActInferenceGrammar sag=context.getSAGrammar();
		
		Tree clone=tree.clone();
		
		removeSAAnnotations(clone);
		for(ComputationalAction action: sag.values())
		{
			logger.debug("trying speech act rule:"+action.getName());
			logger.debug("on tree:"+clone);
			
			Tree result=action.exec(clone, context);
			
			
			if (result!=null)
			{
				logger.debug("success: "+result);
				
				return (T)result;
			}
			else
				logger.debug("failed");
			
		}
		
		
		return (T)tree.clone();
	}
	
	

	private void removeSAAnnotations(Tree tree) {
		
		Node pointed=tree.getPointedNode();
		Set<Label> resultLabels=new HashSet<Label>();
		for(Label l: pointed)
		{
			if (!(l instanceof SpeechActLabel))
				resultLabels.add(l);
				
				
		}
		
		pointed.clear();
		pointed.addAll(resultLabels);
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR;
	}

	public Effect instantiate() {
	
		return new InferSpeechAct();
	}
}
