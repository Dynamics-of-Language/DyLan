package qmul.ds.learn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.ContextParser;
import qmul.ds.ContextParserTuple;
import qmul.ds.ParseState;
import qmul.ds.action.Action;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.Lexicon;
import qmul.ds.tree.Tree;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;

/**
 * 
 * Corpus of sentences mapped to their corresponding complete DS semantic tree
 * 
 * @author arash
 */


public class Corpus<T extends Object> extends ArrayList<Pair<Sentence<Word>, T>> implements Serializable {
	private static Logger logger = Logger.getLogger(Corpus.class);

	public final static String CORPUS_FOLDER = "corpus/";
	public final static String WORD_SEP_PATTERN = "\\s";
	private static final long serialVersionUID = 4914176393669845762L;

	public Corpus() {
		super();
	}

	public void loadAndParseCorpusFromFile(String sentencesFileName, String resourceDir) throws IOException {

		BufferedReader reader = new BufferedReader(new FileReader(sentencesFileName));
		ContextParser parser = new ContextParser(resourceDir);
		Lexicon trainingLexicon = new Lexicon();
		HashMap<String, Integer> wordActionCount = new HashMap<String, Integer>();
		int totalActions = 0;

		clear();
		String sentence;
		while ((sentence = reader.readLine()) != null) {
			if (sentence.trim().isEmpty() || sentence.trim().startsWith("//"))
				continue;
			;
			parser.init();
			String[] sentArray = sentence.trim().split("\\s");

			List<String> sentList = Arrays.asList(sentArray);
			Sentence<Word> sent = Sentence.toSentence(sentList);

			parser.parseWords(sentList);
			Tree complete = parser.getBestParse();
			logger.info("Parsed, complete tree:\n" + complete);
			if (complete == null || !complete.isComplete()) {
				logger.warn("Could not parse corpus sentence: " + sentence + " Skipping ....");
				continue;
			}
			ParseState<ContextParserTuple> completeState = parser.getState().complete();
			for (Action action : ((ContextParserTuple) completeState.first()).getActions()) {
				if (action instanceof LexicalAction) {
					String myWord = ((LexicalAction) action).getWord();
					if (!trainingLexicon.keySet().contains(myWord)) {
						trainingLexicon.put(myWord, new HashSet<LexicalAction>());
						trainingLexicon.get(myWord).add((LexicalAction) action);
					} else {
						// look to see if its same sense has been added:
						boolean SameSenseAddedAlready = false;
						for (LexicalAction act : trainingLexicon.get(myWord)) {
							if (act.getLexicalActionType().equals(((LexicalAction) action).getLexicalActionType())) {
								SameSenseAddedAlready = true;
							}
						}
						if (SameSenseAddedAlready == false) {
							trainingLexicon.get(myWord).add((LexicalAction) action);
						}
					}

					String wordAction = myWord + "-" + ((LexicalAction) action).getLexicalActionType();
					int newCount = 1;
					if (wordActionCount.containsKey(wordAction)) {
						newCount = wordActionCount.get(wordAction) + 1;
					}
					// wordActionCount.remove(myWord);
					wordActionCount.put(wordAction, newCount);
					totalActions++;

				}
			}
			// put(sent, complete);

		}
		for (String word : trainingLexicon.keySet()) {
			for (LexicalAction lexAct : trainingLexicon.get(word)) {
				System.out.println(word + "," + lexAct.getLexicalActionType());
			}
		}
		System.out.println("%%%%%%%%%%%%%%%%%");
		for (String word : wordActionCount.keySet()) {
			System.out.println(word + "," + wordActionCount.get(word));
		}

		System.out.println("%%%%%%%%% total actions = " + totalActions);

		reader.close();
	}

	public void loadAndParseCorpusFromFile(File sentencesFile, String resourceDir) throws IOException {
		loadAndParseCorpusFromFile(sentencesFile.getName(), resourceDir);

	}

	public static void parseCorpusToFile(String sentencesFileName, String outputFileName, String resourceDir)
			throws IOException {
		Corpus<Tree> c = new Corpus<Tree>();
		BufferedReader reader = new BufferedReader(new FileReader(CORPUS_FOLDER + sentencesFileName));
		ContextParser parser = new ContextParser(resourceDir);

		c.clear();
		String sentence;
		while ((sentence = reader.readLine()) != null) {
			parser.init();
			String[] sentArray = sentence.split("\\s");

			List<String> sentList = Arrays.asList(sentArray);
			Sentence<Word> sent = Sentence.toSentence(sentList);

			parser.parseWords(sentList);
			Tree complete = parser.getBestParse();
			if (complete == null || !complete.isComplete()) {
				logger.warn("Could not parse corpus sentence: " + sentence + " Skipping ....");
				continue;
			}
			c.add(new Pair(sent, complete));

		}
		FileOutputStream outF = new FileOutputStream(CORPUS_FOLDER + outputFileName);
		ObjectOutputStream out = new ObjectOutputStream(outF);
		out.writeObject(c);
		out.flush();
		out.close();
		reader.close();

	}

	public static void main(String a[]) {
		Corpus<Tree> c = new Corpus<Tree>();
		try {
			// Corpus.parseCorpusToFile("sentences.txt", "testCorpus.corpus", "resource/2009-english-test-induction");
			// c.loadCorpus("testCorpus.corpus");
			c.loadAndParseCorpusFromFile("corpus.txt", "resource" + File.separator + "2009-english-test-induction");
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(c);
	}

	public void loadCorpus(File fileName) throws IOException {
		File file = new File(CORPUS_FOLDER + fileName);
		FileInputStream f = new FileInputStream(file);
		ObjectInputStream s = new ObjectInputStream(f);
		Corpus c = new Corpus();
		
		try {
			c = (Corpus) s.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.out.println("Probably not a corpus object on file");
		}

		s.close();
		this.addAll(c);

	}

	public String toString() {
		String s = "";
		for (Pair<Sentence<Word>, T> sentence : this) {
			T target = sentence.second();
			s += sentence + " -> " + target + "\n";

		}

		return s;
	}

}
