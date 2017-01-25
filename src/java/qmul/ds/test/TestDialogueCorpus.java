package qmul.ds.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.InteractiveContextParser;
import qmul.ds.babi.BabiDialogue;

public class TestDialogueCorpus implements Serializable {
	private static final long serialVersionUID = -5694785894427522351L;
	private static Logger logger = Logger.getLogger(TestDialogueCorpus.class);
	private InteractiveContextParser parser;

	public TestDialogueCorpus(String resourceFolder) {
		parser = new InteractiveContextParser(resourceFolder, BabiDialogue.AGENTS);
	}

	public void test(String inCorpusRoot, BufferedWriter out) throws IOException {
		int
			errorUtterances = 0,
			totalUtterances = 0,
			errorDialogues = 0,
			totalDialogues = 0;
		File[] filesToProcess = new File(inCorpusRoot).listFiles();
		for (File file: filesToProcess) {
			BabiDialogue dialogue = BabiDialogue.loadFromBabbleFile(file.getAbsolutePath());
			parser.init();
			List<String[]> errors = TestDialogue.test(parser, dialogue);
			for (String[] error: errors) {
				out.write(String.format("%s\t%s\t%s\n", file.getName(), error[0], error[1]));
			}
			totalUtterances += dialogue.getTurns().size() * BabiDialogue.AGENTS.length;
			errorUtterances += errors.size();
			++totalDialogues;
			errorDialogues += errors.isEmpty() ? 0 : 1;
			if (totalDialogues % 10 == 0) {
				logger.info(String.format("%d out of %d dialogues processed", totalDialogues, filesToProcess.length));
			}
		}
		out.write(String.format(
			"\n\nParser per-utterance coverage: %.3f\n",
			1.0 - (double)(errorUtterances) / totalUtterances
		));
		out.write(String.format(
			"Parser per-dialogue coverage: %.3f\n",
			1.0 - (double)(errorDialogues) / totalDialogues
		));
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.out.println(
				"Usage: TestDialogueCorpus.java <grammar resource> <Babble dialogues root> <report filename>"
			);
			return;
		}
		TestDialogueCorpus test = new TestDialogueCorpus(args[0]);
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(args[2])));
		test.test(args[1], out);
		out.close();
	}
}
