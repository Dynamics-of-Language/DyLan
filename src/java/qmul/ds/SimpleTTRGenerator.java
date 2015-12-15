/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.formula.TTRRecordType;

/**
 * A simple {@link Generator} with plain vanilla {@link ParserTuple}s
 * 
 * @author mpurver
 */
public class SimpleTTRGenerator extends TTRGenerator<ParserTuple> {

	private static Logger logger = Logger.getLogger(SimpleTTRGenerator.class);

	public SimpleTTRGenerator(Parser<ParserTuple> parser) {
		super(parser);
	}

	public SimpleTTRGenerator(Lexicon lexicon, Grammar grammar) {
		super(lexicon, grammar);
	}

	public SimpleTTRGenerator(File resourceDir) {
		super(resourceDir);
	}

	public SimpleTTRGenerator(String resourceDirNameOrURL) {
		super(resourceDirNameOrURL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.Generator#getParser(qmul.ds.action.Lexicon, qmul.ds.action.Grammar)
	 */
	@Override
	public Parser<ParserTuple> getParser(Lexicon lexicon, Grammar grammar) {
		return new SimpleParser(lexicon, grammar);
	}
	
	public static void main(String[] a)
	{
		SimpleTTRGenerator gen=new SimpleTTRGenerator("resource/2015-english-ttr-shape-colourNew");
		gen.init();
		//gen.parser.parseWord("this");
		//System.out.println(gen.parser.getState());
		
		List<TTRRecordType> goals = new ArrayList<TTRRecordType>(); 
		goals.add(TTRRecordType.parse("[x0==this : e|pred1==red : cn|p4==color(pred1) : t|p3==subj(pred1, x0) : t]"));
//		goals.add(TTRRecordType.parse("[x==this : e | pred0 : cn | p0==class(x,pred0) : t]"));
//		goals.add(TTRRecordType.parse("[x1==this:e|head==x1:e]"));
//		goals.add(TTRRecordType.parse("[x1==this:e|p2==circle(x1)|head==x1:e]"));
//		goals.add(TTRRecordType.parse("[x1==this:e|p1==red(x1)|p2==circle(x1)|head==x1:e]"));
		//gen.setGoal(goal);
		
		
		for(TTRRecordType goal: goals){
			gen.generate(goal);
		}
		
		System.out.println("Gen State after generation:\n"+gen.getState());
		
		System.out.println("Best string:"+gen.getBestString());
		
	}

	
}