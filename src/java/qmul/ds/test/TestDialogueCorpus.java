package qmul.ds.test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.log4j.Logger;

import csli.util.Pair;
import qmul.ds.InteractiveContextParser;
import qmul.ds.babi.BabiDialogue;

public class TestDialogueCorpus implements Serializable {
	private static Logger logger = Logger.getLogger(TestDialogueCorpus.class);
	private InteractiveContextParser parser;

	public TestDialogueCorpus(String resourceFolder) {
		parser = new InteractiveContextParser(resourceFolder, BabiDialogue.AGENTS);
	}

	public Pair<Integer, Integer> test(String inCorpusRoot) throws IOException {
		int
			parsed = 0,
			overall = 0;
		for (File file: new File(inCorpusRoot).listFiles()) {
			BabiDialogue dialogue = BabiDialogue.loadFromBabbleFile(file.getAbsolutePath());
			Pair<Integer, Integer> dialogueParseResult = TestDialogue.test(parser, dialogue);
			if (!dialogueParseResult.a.equals(dialogueParseResult.b)) {
				logger.error("Failed parsing for file " + file.getAbsolutePath());
			}
			else {
				++parsed;
			}
			++overall;
		}
		logger.info(String.format("%d out of %d dialogues parsed", parsed, overall));
		return new Pair<>(parsed, overall);
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("Usage: TestDialogueCorpus.java <grammar resource> <Babble dialogues root>");
			return;
		}
		TestDialogueCorpus test = new TestDialogueCorpus(
			"resource" + File.separator + "2016-english-ttr-restaurant-search"
		);
		test.test(args[0]);
	}
}
