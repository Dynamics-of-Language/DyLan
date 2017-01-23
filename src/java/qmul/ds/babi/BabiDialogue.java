package qmul.ds.babi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import qmul.ds.InteractiveContextParser;
import qmul.ds.Utterance;
import qmul.ds.formula.TTRRecordType;

public class BabiDialogue {
	public static final String[] AGENTS = new String[]{"usr", "sys"};
	private List<Utterance[]> turns = new ArrayList<>();

	public static List<BabiDialogue> loadFromBabiFile(String inBabiFilename) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(new File(inBabiFilename)));
		List<BabiDialogue> result = new ArrayList<>();
		result.add(new BabiDialogue());
		String line;
		while ((line = in.readLine()) != null) {
			if (line.isEmpty()) {
				result.add(new BabiDialogue());
				continue;
			}
			line = line.toLowerCase().replace("<silence>", "<wait>");
			String[] lineParts = line.split(" ", 2);
			String[] utterances = lineParts[1].split("\t");
			assert 2 == utterances.length: "Invalid bAbI data: " + line;

			Utterance[] turn = new Utterance[]{null, null};
			for (int index: new int[]{0, 1}) {
				String
					agentName = AGENTS[index],
					utterance = utterances[index];
				if (utterance.startsWith("api_call")) {
					utterance = "done";
				}
				turn[index] = new Utterance(agentName, utterance);
			}
			result.get(result.size() - 1).addTurn(turn);
		}
		return result;
	}

	public static BabiDialogue loadFromBabbleFile(String srcFile) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(new File(srcFile)));
		BabiDialogue result = new BabiDialogue();

		String line;
		Utterance[] currentTurn = new Utterance[]{null, null};
		int lineCounter = 0;
		while ((line = in.readLine()) != null) {
			if (line.isEmpty() || line.trim().startsWith("//")) {
				continue;
			}
			line = line.toLowerCase();
			String[] lineParts = line.split(":", 2);
			assert 1 < lineParts.length: "Utterance \"" + line.trim() + "\" apparently is not annotated with agent ID";
			String
				agentName = lineParts[0].trim(),
				utterance = lineParts[1].trim();
			currentTurn[lineCounter % 2] = new Utterance(agentName, utterance);
			if (lineCounter % 2 == 1) {
				result.addTurn(currentTurn);
			}
		}
		return result;
	}

	public static void convertCorpus(String inBabiRoot, String inBabbleRoot) throws IOException {
		File babbleRootFile = new File(inBabbleRoot);
		if (!(babbleRootFile.exists())) {
			babbleRootFile.mkdir();
		}
		for (File file: new File(inBabiRoot).listFiles()) {
			String fileName = file.getName();
			if (!fileName.startsWith("dialog-babi-task1")) {
				continue;
			}
			 
			List<BabiDialogue> dialogues = loadFromBabiFile(file.getAbsolutePath());
			int counter = 0;
			for (BabiDialogue dialogue: dialogues) {
				String outFileName = inBabbleRoot + File.separator + String.format("%s.%d", fileName, counter + 1);
				dialogue.save(outFileName);
				++counter;
			}
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Usage: BabiDialogue.java <bAbI folder> <Babble folder>");
			return;
		}
		convertCorpus(args[0], args[1]);
	}

	public void addTurn(Utterance[] inUtterances) {
		assert checkTurnWellformedness(inUtterances) : "Invalid turn";
		turns.add(inUtterances);
	}

	public void save(String inFileName) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(inFileName)));

		String line;
		for (Utterance[] turn: turns) {
			for (Utterance utterance: turn) {
				out.write(String.format("%s:\t%s\n", utterance.getSpeaker(), utterance.getText()));
			}
		}
		out.close();
	}

	public List<Utterance[]> getTurns() {
		return turns;
	}

	private boolean checkTurnWellformedness(Utterance[] inTurn) {
		boolean result = inTurn.length == 2;
		for (int index: new int[]{0, 1}) {
			result &= inTurn[index].getSpeaker().equals(AGENTS[index]);
		}
		return result;
	}
}
