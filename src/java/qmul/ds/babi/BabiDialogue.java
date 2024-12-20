package qmul.ds.babi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import qmul.ds.Dialogue;
import qmul.ds.Utterance;

public class BabiDialogue extends Dialogue {
	private static final long serialVersionUID = 1L;
	public static final String[] AGENTS = new String[]{"usr", "sys"};
	public static Map<String, String> replacements=new HashMap<>();
	static {
		Map<String, String> map = new HashMap<>();

		map.put("which price range are looking for", "which price range are you looking for");
		map.put("good morning", "goodmorning");
		map.put("how many", "howmany");
		map.put("i'm on it", "sure");
		map.put("you know", "uhm");
		map.put("any preference on a type of cuisine", "any preference on a type of cuisine?");

		replacements = Collections.unmodifiableMap(map);
	}

	public BabiDialogue() {
		super(AGENTS);
	}

	
	private static String applyReplacements(String s)
	{
		String init = s;
		
		for(String replacement: replacements.keySet())
		{
			init=init.replace(replacement, replacements.get(replacement));
			
		}
		
		return init;
	}

	public static List<BabiDialogue> loadFromBabiFile(
		String inBabiFilename,
		Map<String, Boolean> inConfig
	) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(new File(inBabiFilename)));
		List<BabiDialogue> result = new ArrayList<>();
		result.add(new BabiDialogue());
		List<Utterance> dialogueTurns = new ArrayList<>();
		String line;
		while ((line = in.readLine()) != null) {
			if (line.isEmpty()) {
				result.add(buildBabiDialogue(dialogueTurns, inConfig));
				dialogueTurns.clear();
				continue;
			}
			if (inConfig.get("apply_replacements")) {
				line = applyReplacements(line.toLowerCase());
			}
			String[] lineParts = line.split(" ", 2);
			String[] utterances = lineParts[1].split("\t");
			assert 2 == utterances.length: "Invalid bAbI data: " + line;

			for (int index: new int[]{0, 1}) {
				String
					agentName = AGENTS[index],
					utterance = utterances[index];
				if (utterance.startsWith("api_call") && inConfig.get("process_api_calls").equals(false)) {
					continue;
				}
				dialogueTurns.add(new Utterance(agentName, utterance));
			}
		}
		in.close();
		result = result.stream().filter(x -> !x.isEmpty()).collect(Collectors.toList());

		return result;
	}

	private static BabiDialogue buildBabiDialogue(List<Utterance> inTurns, Map<String, Boolean> inConfig) {
		List<Utterance> processedUtterances = new ArrayList<>();
		boolean turnToBeAppended = false;
		for (Utterance turn: inTurns) {
			if (turnToBeAppended) {
				assert processedUtterances.size() != 0 : "Invalid bAbI idalogue: <silence> as the start of the dialogue";
				Utterance previousTurn = processedUtterances.get(processedUtterances.size() - 1);
				processedUtterances.set(
					processedUtterances.size() - 1,
					new Utterance(previousTurn.getSpeaker(), previousTurn.getText() + " "+ turn.getText())
				);
				turnToBeAppended = false;
				continue;
			}
			if (turn.getText().equals("<silence>")) {
				if (!inConfig.get("remove_silence")) {
					processedUtterances.add(new Utterance(turn.getSpeaker(), " "));
				}
				else {
					// current turn is just silence - skipping it completely and appending next one to the previous one
					turnToBeAppended = true;
				}
			}
			else {
				processedUtterances.add(turn);
			}
		}
		BabiDialogue result = new BabiDialogue();
		for (Utterance utterance: processedUtterances) {
			String utteranceWithRelease = utterance.getText() + " " + Utterance.RELEASE_TURN_TOKEN;
			result.add(new Utterance(utterance.getSpeaker(), utteranceWithRelease));
		}
		return result;
	}

	public static List<BabiDialogue> loadFromBabbleFile(String srcFile) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(new File(srcFile)));
		List<BabiDialogue> result = new ArrayList<>();
		result.add(new BabiDialogue());

		String line;
		while ((line = in.readLine()) != null) {
			if (line.trim().isEmpty()) {
				if (!result.get(result.size() - 1).isEmpty()) {
					 result.add(new BabiDialogue());
				}
				continue;
			}
			if (line.trim().startsWith("//")) {
				continue;
			}
			line = line.toLowerCase();
			String[] lineParts = line.split(":", 2);
			assert 1 < lineParts.length: "Utterance \"" + line.trim() + "\" apparently is not annotated with agent ID";
			String
				agentName = lineParts[0].trim(),
				utterance = lineParts[1].trim();
			result.get(result.size() - 1).add(new Utterance(agentName, utterance));
		}
		in.close();
		return result.stream().filter(x -> !x.isEmpty()).collect(Collectors.toList());
	}

	public static void convertCorpus(String inBabiRoot, String inBabbleRoot) throws IOException {
		File babbleRootFile = new File(inBabbleRoot);
		if (!(babbleRootFile.exists())) {
			babbleRootFile.mkdir();
		}
		int fileCounter = 0;
		for (File file: new File(inBabiRoot).listFiles()) {
			String fileName = file.getName();
			if (!fileName.startsWith("dialog-babi-task1")) {
				continue;
			}
			++fileCounter;
			List<BabiDialogue> dialogues = loadFromBabiFile(file.getAbsolutePath(), getDefaultConfig());
			int counter = 0;
			for (BabiDialogue dialogue: dialogues) {
				String outFileName = inBabbleRoot + File.separator + String.format("%s.%d", fileName, counter + 1);
				dialogue.save(outFileName);
				++counter;
			}
		}
		System.out.println(String.format("Successfully converted %d files", fileCounter));
	}

	public static Map<String, Boolean> getDefaultConfig() {
		Map<String, Boolean> config = new HashMap<>();
		config.put("apply_replacements", true);
		config.put("remove_silence", true);
		config.put("process_api_calls", false);
		return config;
	}

	public static void collectSlotValuesFromCorpus(String inBabiRoot, String inDstFileName) throws IOException {
		List<Set<String>> result = new ArrayList<>();
		for (File file: new File(inBabiRoot).listFiles()) {
			String fileName = file.getName();
			if (!fileName.startsWith("dialog-babi-task1")) {
				continue;
			}
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line;
			while ((line = in.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				String[] lineParts = line.split(" ", 2);
				String[] utterances = lineParts[1].split("\t");
				assert 2 == utterances.length: "Invalid bAbI data: " + line;

				for (int index: new int[]{0, 1}) {
					String utterance = utterances[index];
					if (utterance.startsWith("api_call")) {
						List<String> values = extractSlotValues(utterance);
						for (int valueIndex = 0; valueIndex != values.size(); ++valueIndex) {
							if (result.size() <= valueIndex) {
								result.add(new HashSet<>());
							}
							result.get(valueIndex).add(values.get(valueIndex));
						}
					}
				}
			}
			in.close();
		}

		BufferedWriter out = new BufferedWriter(new FileWriter(new File(inDstFileName)));
		for (int slotIndex = 0; slotIndex != result.size(); ++slotIndex) {
			out.write(String.format("slot_%d:\t%s\n", slotIndex + 1, String.join(", ", result.get(slotIndex))));
		}
		out.close();
	}

	public static List<String> extractSlotValues(String inUtterance) {
		String[] values = inUtterance.split(" ");
		return Arrays.asList(values).subList(1, values.length);
	}

	public static void main(String[] args) {
		try {
			String
				srcPath = "corpus/bAbI-dialogue/dialog-bAbI+-tasks",
				dstPath = "corpus/bAbI-dialogue/bAbI+-babble-format";
			if (args.length == 2) {
				srcPath = args[0];
				dstPath = args[1];
			}
			convertCorpus(srcPath, dstPath);
			collectSlotValuesFromCorpus(srcPath, dstPath + "/slot-values");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void save(String inFileName) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(inFileName)));

		for (Utterance turn: this) {
			out.write(String.format("%s:\t%s\n", turn.getSpeaker(), turn.getText()));
		}
		out.close();
	}
}

