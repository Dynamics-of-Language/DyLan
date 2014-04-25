/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.test;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import qmul.ds.Parser;
import csli.util.Pair;

/**
 * An {@link ArrayList} of {@link TestSentence}s which should be parsed sequentially, maintaining context. (e.g.
 * examples where first sentence provides antecedent for later ellipsis)
 * 
 * @author mpurver
 */
public class TestContext extends ArrayList<TestSentence> {

	private static final long serialVersionUID = -6708421411355226456L;

	private static Logger logger = Logger.getLogger(TestContext.class);

	public TestContext() {
		super();
	}

	/**
	 * Run the specified {@link Parser} over this sequence of sentences
	 * 
	 * @param parser
	 * @return proportion successfully parsed, and proportion successfully parsed and matched semantic formula
	 */
	public Pair<Double, Double> test(Parser<?> parser) {
		double nSent = 0.0;
		double nParsed = 0.0;
		double nMatched = 0.0;

		for (TestSentence sentence : this) {
			nSent++;
			Pair<Double, Double> r = sentence.test(parser);
			if (!Double.isNaN(r.a)) {
				nParsed++;
			}
			if (!Double.isNaN(r.b)) {
				nMatched++;
			}
			parser.newSentence();
		}

		return new Pair<Double, Double>((nSent == 0.0) ? 0.0 : (nParsed / nSent), (nSent == 0.0) ? 0.0
				: (nMatched / nSent));
	}

}
