/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.test;

import java.io.CharArrayReader;
import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.Parser;
import qmul.ds.ParserTuple;
import qmul.ds.ScoredParser;
import qmul.ds.ScoredParserTuple;
import qmul.ds.formula.Formula;
import csli.util.Pair;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;

/**
 * For parser testing: a sentence paired with a desired {@link Formula} semantic representation
 * 
 * @author mpurver
 */
public class TestSentence implements Serializable {

	private static final long serialVersionUID = -2019602484791313714L;

	private static Logger logger = Logger.getLogger(TestSentence.class);

	private static final String UNGRAMMATICAL_MARKER = "*";

	private List<Word> words;
	private Formula formula;
	private boolean ungrammatical = false;

	private static TokenizerFactory<Word> tf = new PennTreebankLanguagePack().getTokenizerFactory();

	/**
	 * @param sentenceString
	 *            beginning with a '*' if it's supposed to be ungrammatical
	 * @param formulaString
	 *            or the empty string if it's supposed to be ungrammatical
	 */
	public TestSentence(String sentenceString, String formulaString) {
		if (sentenceString.startsWith(UNGRAMMATICAL_MARKER)) {
			sentenceString = sentenceString.replace(UNGRAMMATICAL_MARKER, "");
			ungrammatical = true;
		}
		Tokenizer<Word> toke = tf.getTokenizer(new CharArrayReader(sentenceString.toCharArray()));
		words = toke.tokenize();
		if (ungrammatical) {
			formula = null;
		} else {
			formula = Formula.create(formulaString);
		}
	}

	/**
	 * Run the specified {@link Parser} over this sentence
	 * 
	 * @param parser
	 * @return parse score of best successful complete parse (or 1.0 if {@link Parser} doesn't score), or NaN if parse
	 *         failed; and parse score of parse which matches semantic formula (or 1.0 if {@link Parser} doesn't score),
	 *         or NaN if none matches. If sentence is ungrammatical, failure to parse returns 1.0 and 1.0 (so as to keep
	 *         overall stats happy) - otherwise, NaN and NaN.
	 */
	public Pair<Double, Double> test(Parser<?> parser) {
		double parsed = Double.NaN;
		double matched = Double.NaN;
		logger.info("TEST sentence " + (ungrammatical ? UNGRAMMATICAL_MARKER : "") + words + " ... ");
		if (parser.parse(words) && !parser.getState().complete().isEmpty()) {
			if (ungrammatical) {
				logger.warn("wrongly parsed.");
			} else {
				logger.info("parsed ... ");
				if (parser instanceof ScoredParser) {
					parsed = ((ScoredParser) parser).getState().complete().first().getScore();
				} else {
					parsed = 1.0;
				}
				for (ParserTuple tuple : parser.getState().complete()) {
					if (tuple.getTree().getRootNode().getFormula().equals(formula)) {
						logger.info("matched " + formula);
						if (tuple instanceof ScoredParserTuple) {
							matched = ((ScoredParserTuple) tuple).getScore();
						} else {
							matched = 1.0;
						}
						break;
					}
				}
				if (Double.isNaN(matched)) {
					logger.warn("failed match.");
				}
			}
		} else {
			if (ungrammatical) {
				logger.info("correctly failed parse.");
				parsed = 1.0;
				matched = 1.0;
			} else {
				logger.warn("failed parse.");
			}
		}
		return new Pair<Double, Double>(parsed, matched);
	}

	/**
	 * @return the words
	 */
	public List<Word> getWords() {
		return words;
	}

	/**
	 * @return the formula
	 */
	public Formula getFormula() {
		return formula;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return words + " -> " + formula;
	}

}
