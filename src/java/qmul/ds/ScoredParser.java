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

/**
 * An example of how a {@link Parser} can use {@link ScoredParserTuple}s to implement conditional action probabilities
 * and a beam width
 * 
 * @author mpurver
 */
public class ScoredParser extends Parser<ScoredParserTuple> {

	private static Logger logger = Logger.getLogger(ScoredParser.class);

	private double beamWidth;

	public ScoredParser(Lexicon lexicon, Grammar grammar) {
		super(lexicon, grammar);
	}

	/**
	 * @param resourceDir
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public ScoredParser(File resourceDir) {
		super(resourceDir);
	}

	/**
	 * @param resourceDirNameOrURL
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public ScoredParser(String resourceDirNameOrURL) {
		super(resourceDirNameOrURL);
	}

	/**
	 * @return the beamWidth
	 */
	public double getBeamWidth() {
		return beamWidth;
	}

	/**
	 * @param beamWidth
	 *            the beamWidth to set
	 */
	public void setBeamWidth(double beamWidth) {
		this.beamWidth = beamWidth;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.Parser#getAxiom()
	 */
	@Override
	protected ScoredParserTuple getAxiom() {
		return new ScoredParserTuple();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.Parser#getGenerator()
	 */
	@Override
	public Generator<ScoredParserTuple> getGenerator() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.Parser#execAction(qmul.ds.action.Action, qmul.ds.ParserTuple)
	 */
	@Override
	protected ScoredParserTuple execAction(ScoredParserTuple tuple, Action action, String word) {
		ScoredParserTuple result = tuple.execAction(action, word);
		if (result.getScore() < (getState().first().getScore() - beamWidth)) {
			return null;
		}
		return result;
	}

	/**
	 * @return the score of the "best" tree in the current state (where "best" is defined by the natural ordering of the
	 *         {@link ParserTuple} implementation used), or NaN if the state is empty
	 */
	public double getBestScore() {
		if (!successful()) {
			return Double.NaN;
		}
		return state.first().getScore();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ScoredParser sp = new ScoredParser(new File(args[0]));
		sp.init();
		String[] sent = "john likes mary".split("\\s+");
		sp.parseWords(Arrays.asList(sent));
	}

	@Override
	protected List<ScoredParserTuple> execExhaustively(ScoredParserTuple tuple, Action action, String word) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ParseState<ScoredParserTuple> getStateWithNBestTuples(int N) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRepairProcessing(boolean repair) {
		throw new UnsupportedOperationException("Repair processing not supported");
		
	}

}
