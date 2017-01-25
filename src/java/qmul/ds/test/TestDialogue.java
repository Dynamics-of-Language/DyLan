package qmul.ds.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.InteractiveContextParser;
import qmul.ds.Utterance;
import qmul.ds.babi.BabiDialogue;
import qmul.ds.dag.UtteredWord;

public class TestDialogue implements Serializable {
	private static final long serialVersionUID = 3255682835234980378L;
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
	public static List<String[]> test(InteractiveContextParser parser, BabiDialogue inDialogue) {
		List<String[]> errors = new ArrayList<>();

		Utterance previousUtterance = new Utterance();
		for (Utterance[] turn: inDialogue.getTurns()) {
			for (Utterance utterance: turn) {
				String[] resultAsIs = parseUtterance(parser, utterance);
				if (resultAsIs.length != 0) {
					String[] resultGivenContext = parseUtteranceGivenContext(parser, utterance, previousUtterance);
					if (resultGivenContext.length != 0) {
						errors.add(resultGivenContext);
					}
				}
				previousUtterance = utterance;
			}
		}
		return errors;
	}

	private static String[] parseUtterance(InteractiveContextParser parser, Utterance utterance) {
		parser.init();
		for(UtteredWord word: utterance.getWords()) {
			if (parser.parseWord(word) == null) {
				return new String[]{utterance.getText(), word.word()};
			}
		}
		return new String[]{};
	}

	private static String[] parseUtteranceGivenContext(
		InteractiveContextParser parser,
		Utterance utterance,
		Utterance context
	) {
		String[] contextParseResult = parseUtterance(parser, context);
		if (contextParseResult.length != 0) {
			return new String[]{utterance.getText(), "<context parse error>"};
		}
		for(UtteredWord word: utterance.getWords()) {
			if (parser.parseWord(word) == null) {
				return new String[]{utterance.getText(), word.word()};
			}
		}
		return new String[]{};
	}
}
