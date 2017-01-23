package qmul.ds.test;

import java.io.Serializable;

import org.apache.log4j.Logger;

import csli.util.Pair;
import qmul.ds.InteractiveContextParser;
import qmul.ds.Utterance;
import qmul.ds.babi.BabiDialogue;

public class TestDialogue implements Serializable {
	private static Logger logger = Logger.getLogger(TestSentence.class);

	/**
	 * Run the specified {@link Parser} over this sentence
	 * 
	 * @param parser
	 * @return parse score of best successful complete parse (or 1.0 if {@link Parser} doesn't score), or NaN if parse
	 *         failed; and parse score of parse which matches semantic formula (or 1.0 if {@link Parser} doesn't score),
	 *         or NaN if none matches. If sentence is ungrammatical, failure to parse returns 1.0 and 1.0 (so as to keep
	 *         overall stats happy) - otherwise, NaN and NaN.
	 */
	public static Pair<Integer, Integer> test(InteractiveContextParser parser, BabiDialogue inDialogue) {
		int parsed = 0;
		int overall = 0;
		for (Utterance[] turn: inDialogue.getTurns()) {
			for (Utterance utterance: turn) {
				if (parser.parseUtterance(utterance)) {
					++parsed;
				}
				++overall;
			}
		}
		return new Pair<Integer, Integer>(parsed, overall);
	}
}
