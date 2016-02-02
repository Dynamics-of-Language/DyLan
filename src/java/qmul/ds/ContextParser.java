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
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;

/**
 * A {@link Parser} which uses {@link ContextParserTuple}s to implement a word/action-sequence context
 * 
 * @author mpurver
 */
public class ContextParser extends Parser<ContextParserTuple> {

	private static Logger logger = Logger.getLogger(ContextParser.class);

	public ContextParser(Lexicon lexicon, Grammar grammar) {
		super(lexicon, grammar);
	}

	/**
	 * @param resourceDir
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public ContextParser(File resourceDir) {
		super(resourceDir);
	}

	/**
	 * @param resourceDirNameOrURL
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public ContextParser(String resourceDirNameOrURL) {
		super(resourceDirNameOrURL);
	}

	/**
	 * this will return a new instance of Parser, which is initialised from resourceDir, but with the lexicon coming
	 * from an object file, rather than a text file
	 * 
	 * @param resourceDir
	 * @return
	 */
	public static ContextParser getParser(String resourceDir) {
		Lexicon lex = null;
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(resourceDir + "lexicon.lex"));

			lex = (Lexicon) in.readObject();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ContextParser(lex, new Grammar(resourceDir));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.Parser#getAxiom()
	 */
	@Override
	protected ContextParserTuple getAxiom() {
		return new ContextParserTuple();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.Parser#addAxiom()
	 */
	@Override
	protected void addAxiom() {
		// now that we have a context, must add each complete tree to the
		// context before restarting

		ParseState<ContextParserTuple> oldState = state.clone();
		state.clear();
		for (ContextParserTuple tuple : oldState) {
			if (tuple.getTree().isComplete()) {
				logger.info("Found complete tuple. Adding to context:" + tuple);
				state.add(new ContextParserTuple(tuple));
				// state.add(tuple);
				logger.trace("Actions leading to complete tuple are:");
				ArrayList<Action> actions = tuple.getActions();
				for (Action a : actions)
					logger.trace(a);
			}
		}

		if (state.isEmpty()) {

			state.add(getAxiom());
		}

	}

	@Override
	public ParseState<ContextParserTuple> getStateWithNBestTuples(int number) {
		// TODO Auto-generated method stub
		// T best = getBestTuple();
		ParseState<ContextParserTuple> bestOnlyState = new ParseState<ContextParserTuple>();
		int i = 0;
		for (ContextParserTuple tuple : state.clone()) {
			// logger.info(tuple.toString());
			if (i == number) {
				break;
			}
			bestOnlyState.add((ContextParserTuple) tuple);
			i++;
		}
		return bestOnlyState;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.Parser#getGenerator()
	 */
	@Override
	public Generator<ContextParserTuple> getGenerator() {
		return new ContextGenerator(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.Parser#execAction(qmul.ds.ParserTuple, qmul.ds.action.Action, java.lang.String)
	 */
	@Override
	protected ContextParserTuple execAction(ContextParserTuple tuple, Action action, String word) {
		return tuple.execAction(action, word);
	}

	protected Collection<ContextParserTuple> execExhaustively(ContextParserTuple tuple, Action action, String word) {
		return tuple.execExhaustively(action, word);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ContextParser sp = new ContextParser("resource/2009-english");
		sp.init();
		ParseState<ContextParserTuple> oldState = sp.getState().clone();
		String[] sent = "john likes mary".split("\\s+");
		sp.parseWords(Arrays.asList(sent));

		for (ParserTuple tuple : oldState) {
			for (ParserTuple complete : sp.getState().complete()) {
				if (tuple.getTree().subsumes(complete.getTree()))
					System.out.println(tuple.getTree() + "\n subsumes \n" + complete.getTree());
				else
					System.out.println(tuple.getTree() + "\n NOT subsumes \n" + complete.getTree());
			}
		}
	}

	@Override
	public void setRepairProcessing(boolean repair) {
		throw new UnsupportedOperationException("Repair processing is not supported in Context Parser");
		
	}

}
