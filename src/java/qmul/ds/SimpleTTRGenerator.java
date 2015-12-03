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
		SimpleTTRGenerator gen=new SimpleTTRGenerator("resource/2015-english-ttr-shape-colour");
		gen.init();
		//gen.parser.parseWord("this");
		//System.out.println(gen.parser.getState());
		TTRRecordType goal=TTRRecordType.parse("[x==this:e|p==red(x):t|head==x:e]");
	//	gen.setGoal(goal);
		gen.generate(goal);
		
		System.out.println("Gen State after generation:\n"+gen.getState());
		
	}

	
	

}
