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
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.tree.Tree;

/**
 * A simple {@link Parser} with plain vanilla {@link Tree}s
 * 
 * @author mpurver
 */
public class SimpleParser extends Parser<ParserTuple> {

	private static Logger logger = Logger.getLogger(SimpleParser.class);

	public SimpleParser(Lexicon lexicon, Grammar grammar) {
		super(lexicon, grammar);
	}

	/**
	 * @param resourceDir
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public SimpleParser(File resourceDir) {
		super(resourceDir);
	}

	/**
	 * @param resourceDirNameOrURL
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public SimpleParser(String resourceDirNameOrURL) {
		super(resourceDirNameOrURL);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.Parser#getAxiom()
	 */
	@Override
	protected ParserTuple getAxiom() {
		return new ParserTuple();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.Parser#getGenerator()
	 */
	@Override
	public Generator<ParserTuple> getGenerator() {
		return new SimpleTTRGenerator(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.Parser#execAction(qmul.ds.ParserTuple, qmul.ds.action.Action, java.lang.String)
	 */
	@Override
	protected ParserTuple execAction(ParserTuple tuple, Action action, String word) {
		return tuple.execAction(action, word);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SimpleParser sp = new SimpleParser(new File(args[0]));
		sp.init();
		// String[] sent = "john".split("\\s+");
		String[] sent = "john likes mary".split("\\s+");
		sp.parseWords(Arrays.asList(sent));
	}

	@Override
	protected List<ParserTuple> execExhaustively(ParserTuple tuple, Action action, String word) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ParseState<ParserTuple> getStateWithNBestTuples(int N) {
		// TODO Auto-generated method stub
		return null;
	}

}
