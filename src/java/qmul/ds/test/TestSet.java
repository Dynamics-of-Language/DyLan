/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import qmul.ds.Parser;
import qmul.ds.SimpleParser;
import csli.util.FileUtils;
import csli.util.Pair;

/**
 * A dataset for parser testing, consisting of a sequence of {@link TestContext} s
 * 
 * @author mpurver
 */
public class TestSet extends ArrayList<TestContext> {

	private static final long serialVersionUID = -7725120981202361740L;

	private static Logger logger = Logger.getLogger(TestSet.class);

	public static final String CONTEXT_SEPARATOR = "--";

	/**
	 * @param file
	 *            a text file containing a sequence of sentence/formula pairs. First line = sentence; all subsequent
	 *            non-empty lines = formula (as text). Empty line = new sentence. CONTEXT_SEPARATOR = new context.
	 */
	public TestSet(File file) {
		super();
		try {
			String sentenceString = null;
			String formulaString = "";
			TestContext context = new TestContext();
			for (String line : FileUtils.getFileLines(file, new ArrayList<String>())) {
				line = line.trim();
				if (line.isEmpty() || line.equals(CONTEXT_SEPARATOR)) {
					if (sentenceString != null) {
						context.add(new TestSentence(sentenceString, formulaString));
						logger.debug("Added test sentence " + context.get(context.size() - 1));
						sentenceString = null;
						formulaString = "";
					}
					if (line.equals(CONTEXT_SEPARATOR) && !context.isEmpty()) {
						add(context);
						logger.debug("Added test context, " + context.size() + " sentences.");
						context = new TestContext();
					}
				} else {
					if (sentenceString == null) {
						sentenceString = line.toLowerCase();
					} else {
						formulaString += line;
					}
				}
			}
			// missing a final empty line?
			if (sentenceString != null) {
				context.add(new TestSentence(sentenceString, formulaString));
				logger.debug("Added test sentence " + context.get(context.size() - 1));
			}
			// missing a final context separator?
			if (!context.isEmpty()) {
				add(context);
				logger.debug("Added test context, " + context.size() + " sentences.");
			}
			logger.info("Created new test set with " + size() + " context sequences.");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * Run the specified {@link Parser} over the test set
	 * 
	 * @param parser
	 * @return proportion successfully parsed, and proportion successfully parsed and matched semantic formula
	 */
	public Pair<Double, Double> test(Parser<?> parser) {
		double nSent = 0.0;
		double nParsed = 0.0;
		double nMatched = 0.0;

		for (TestContext context : this) {
			parser.init();
			nSent += context.size();
			Pair<Double, Double> r = context.test(parser);
			nParsed += r.a;
			nMatched += r.b;
		}

		return new Pair<Double, Double>((nSent == 0.0) ? 0.0 : (nParsed / nSent), (nSent == 0.0) ? 0.0
				: (nMatched / nSent));
	}

	public static void main(String[] args) {
		String dir = args.length > 0 ? args[0] : "resource/2001-english".replaceAll("/", File.separator);
		TestSet set = new TestSet(new File(dir + File.separator + "test-set.txt"));
		Pair<Double, Double> res = set.test(new SimpleParser(dir));
		logger.info("Tested set size " + set.size() + " parse " + res.a + " match " + res.b);
	}

}
