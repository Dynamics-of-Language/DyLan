/*******************************************************************************
 Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;

import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.tree.Tree;


/**
 * A simple {@link Generator} with context-based {@link ContextParserTuple}s
 * 
 * @author mpurver
 */
public class ContextGenerator extends Generator<ContextParserTuple> {

	private static Logger logger = Logger.getLogger(ContextGenerator.class);

	public ContextGenerator(Parser<ContextParserTuple> parser) {
		super(parser);
	}

	public ContextGenerator(Lexicon lexicon, Grammar grammar) {
		super(lexicon, grammar);
	}

	public ContextGenerator(File resourceDir) {
		super(resourceDir);
	}

	public ContextGenerator(String resourceDirNameOrURL) {
		super(resourceDirNameOrURL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.Generator#getParser(qmul.ds.action.Lexicon, qmul.ds.action.Grammar)
	 */
	@Override
	public Parser<ContextParserTuple> getParser(Lexicon lexicon, Grammar grammar) {
		return new ContextParser(lexicon, grammar);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File resource = new File(args[0]);
		ContextParser cp = new ContextParser(resource);
		cp.init();
		// String[] sent = "john".split("\\s+");
		String[] sent = "john likes mary".split("\\s+");
		cp.parseWords(Arrays.asList(sent));
		Tree t = cp.getBestParse();
		ContextGenerator cg = new ContextGenerator(cp);
		cg.init();
		boolean r = cg.generate(t);
		if (r) {
			logger.info("best string: " + cg.getBestString());
		}
	}

}
