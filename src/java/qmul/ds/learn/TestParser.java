package qmul.ds.learn;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import qmul.ds.ContextParser;
import qmul.ds.ContextParserTuple;
import qmul.ds.ParseState;
import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.learn.Evaluation.EvaluationResult;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;

public class TestParser extends ContextParser {

	RecordTypeCorpus testCorpus;
	List<Word> unknownWordsEncountered;

	public static void pause() {
		System.out.println("Press enter to continue...");
		try {
			System.in.read();
		} catch (Exception e) {
		}

	}

	public TestParser(File resourceDir) {
		super(resourceDir);
		unknownWordsEncountered = new ArrayList<Word>();
	}

	public TestParser(String resourceDir) {
		super(resourceDir);
		unknownWordsEncountered = new ArrayList<Word>();
	}

	/**
	 * Gets parser from:
	 * 
	 * @param resourcePath
	 * @return
	 */
	public static TestParser getParser(String resourcePath) {
		Lexicon lex = null;
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(resourcePath));
			// ObjectInputStream in = new ObjectInputStream(new FileInputStream(resourceDir + "lexicon.3.lex"));
			lex = (Lexicon) in.readObject();
			in.close();
			// ContextParser p = new ContextParser(resourceDir);
			// lex = p.getLexicon();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("loaded lexicon with " + lex.size());
		return new TestParser(lex, new Grammar(resourcePath));
	}

	public TestParser(Lexicon l, Grammar g) {
		super(l, g);
		unknownWordsEncountered = new ArrayList<Word>();
	}

	public void loadTestCorpus(RecordTypeCorpus c) {
		this.testCorpus = c;
	}

	/**
	 * @param corpusFile
	 *            file of utterances to read in
	 * @param hasTargetFormulae
	 *            whether the file has TTR record types or not
	 */
	public void loadTestCorpus(String corpusFile, boolean hasTargetFormulae) {
		try {
			this.testCorpus = new RecordTypeCorpus();
			if (hasTargetFormulae) {
				this.testCorpus.loadCorpus(new File(corpusFile));
			} else {
				this.testCorpus.loadCorpusNoRecordTypes(new File(corpusFile));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean containsUnknown(Sentence<Word> s) {
		boolean unknown = false;
		for (Word w : s)
			if (!lexicon.containsKey(w.word()))
				if (!unknownWordsEncountered.contains(w)) {
					unknownWordsEncountered.add(w);
				}
		unknown = true;
		return unknown;
	}

	/**
	 * Method to write the incremental ttr output from parsing corpus, i.e. the word-by-word maximal semantics, to a
	 * file. Also outputs (additive) difference between output at previous word and that at the current one, including
	 * relevant dependent fields. e.g.:
	 * 
	 * uttnumber : (UTTID) [word] [currentTTR] [diff] take [x|take(x)] [x|take(x)] the [x|def(x)|take(x)] [x|def(x)] red
	 * [x|red(x)|def(x)|take(x)] [x|red(x)] cross [x|cross(x)|red(x)|def(x)|take(x)] [x|cross(x)]
	 * 
	 * @param outputFile
	 *            the file to write the corpus to
	 * @param errorFile
	 *            the file to write all the parsing errors and summary at the end
	 * @param beam
	 *            the number of parses to maintain in the parse state after each word, puts ceiling on decoding time
	 * @param hasUtteranceIndices
	 *            boolean as to whether this file has an utterance index at the end of each line or not
	 * @param incrementalOutput
	 * 			  boolean of whether to produce incremental output file or just final record type
	 **/
	public void parseCorpusToFile(String outputFile, String errorFile, int beam, boolean hasUtteranceIndices, boolean incrementalOutput) {

		// set the file up
		FileWriter errorFileStream = null;
		FileWriter outputFileStream = null;
		BufferedWriter out;

		try {
			errorFileStream = new FileWriter(errorFile);
			outputFileStream = new FileWriter(outputFile);
			out = new BufferedWriter(errorFileStream);
		} catch (IOException e) {
			System.out.println("Could not create text files");
		}

		int notParsed = 0; // number of successful parses without crashing
		Iterator<Pair<Sentence<Word>, TTRRecordType>> corpusIt = testCorpus.iterator();
		int count = 0;
		corpusLoop: while (corpusIt.hasNext()) {
			Pair<Sentence<Word>, TTRRecordType> entry = corpusIt.next();
			String ID = (hasUtteranceIndices ? testCorpus.getIndexNumber(count) : "" + count);
			count += 1;
			try {
				containsUnknown(entry.first);
				init();
				String successfulPrefix = "";
				TTRRecordType[] wordTTRoutput = new TTRRecordType[entry.first.length()];
				int prefixPosition = 0;
				List<ParseState<ContextParserTuple>> stateHistory = new ArrayList<ParseState<ContextParserTuple>>();
				stateHistory.add(getStateWithNBestTuples(30));
				boolean parsed = true;
				int stateMarker = 1;
				for (HasWord word : entry.first) {
					// System.out.println(word.word());
					// System.out.println(stateHistory);
					parseWord(word.word());
					if (!successful()) {
						// need to store after which word it broke down
						parsed = false;
						try {
							out = new BufferedWriter(errorFileStream);
							out.write(ID);
							out.newLine();
							out.write(successfulPrefix);
							out.newLine();
							out.write(entry.first.toString());
							out.newLine();
							out.flush();
						} catch (IOException e) {
							System.out.println("Could not write to error file");
						}
						// simple repair backtrack
						while (stateMarker >= 0) {
							stateMarker = stateMarker - 1;
							setState(stateHistory.get(stateMarker));
							parseWord(word.word());
							if (successful()) {
								// setSuccessful(true);
								break;
							}
						}
					}

					// check to see if it hasn't managed a parse at all, despite backtracking. Shouldn't it output the
					// previous one?
					if (!successful()) {
						wordTTRoutput[prefixPosition] = TTRRecordType.parse("[]");
						prefixPosition += 1;
						continue;
					}
					// get the top n states (beam)
					ParseState<ContextParserTuple> nBest = getStateWithNBestTuples(beam); // retains the top beamwidth
					setState(nBest);
					stateHistory.add(nBest);
					stateMarker += 1;
					ParseState<ContextParserTuple> Best = getStateWithNBestTuples(1); // just outputs current best TTR
					Iterator<ContextParserTuple> iter = Best.iterator();
					while (iter.hasNext()) {
						TTRRecordType ttr = (TTRRecordType) iter.next().getTree().getMaximalSemantics();
						if (ttr == null) {
							ttr = TTRRecordType.parse("[]");
						}
						wordTTRoutput[prefixPosition] = ttr;
					}
					successfulPrefix += word.toString() + " ";
					prefixPosition += 1;
				}
				// check success of parse and write the word-by-word TTR outputs to the file
				if (parsed == false) {
					notParsed++;
				}
				TTRRecordType currentTTRrec = TTRRecordType.parse("[]");
				int start = incrementalOutput == true ? 0 : wordTTRoutput.length-1;
				for (int r = start; r < wordTTRoutput.length; r++) {
					try {
						out = new BufferedWriter(outputFileStream);
						out.write(ID + "\t");
						if (incrementalOutput==true){
							out.write(entry.first.get(r).toString() + "\t");
						}
						else{ //Write the whole utt in non-incremental version
							out.write(successfulPrefix + "\t");
						}
						out.write(wordTTRoutput[r].toString() + "\t");
						// get the diff if incremental
						if (incrementalOutput==true){
							out.write(wordTTRoutput[r].minus(currentTTRrec).first.toString());
						}
						currentTTRrec = wordTTRoutput[r];
						out.newLine();
						out.flush();
					} catch (IOException e) {
						System.out.println("Could not write to ouput file");
					}
				}

			} catch (Exception f) {
				System.out.println("could not parse utterance!");
				f.printStackTrace();
			}
		}

		// print parse errors summary to error File
		try {
			out = new BufferedWriter(errorFileStream);
			out.write("Number of unknown words = " + String.valueOf(this.unknownWordsEncountered.size()));
			out.newLine();
			for (Word word : this.unknownWordsEncountered) {
				out.write(word.toString());
				out.newLine();
			}
			out.newLine();
			out.write("Number of bad parses = " + String.valueOf(notParsed));
			out.newLine();
			out.write("Proportion of bad parses = " + String.valueOf((float) notParsed / count));
			out.flush();
			out.close();
		} catch (IOException e) {
			System.out.println("Could not write to errorfile");
		}

	}

	/**
	 * Tests the test corpus and outputs evaluation metrics
	 */
	public void test() {
		int parsed = 0;
		int sameF = 0;
		int total = 0;
		List<TTRRecordType[]> myttrs = new ArrayList<TTRRecordType[]>();
		Evaluation e = new Evaluation();

		if (testCorpus == null)
			return;
		Iterator<Pair<Sentence<Word>, TTRRecordType>> corpusIt = testCorpus.iterator();
		corpusLoop: while (corpusIt.hasNext()) {

			Pair<Sentence<Word>, TTRRecordType> entry = corpusIt.next();
			/*
			 * //skipping utterances with unknown words at test time or not? if (this.containsUnknown(entry.first())){
			 * System.out.println(entry.first()); pause();pause(); continue; }
			 */
			total++;
			init();

			TTRRecordType[] pair = new TTRRecordType[2];
			TTRRecordType targetRT = entry.second();
			pair[1] = targetRT;

			System.out.println("Parsing " + entry.first);

			float mostNodesMapped = 0;
			try {
				if (parse(entry.first)) {
					System.out.println("parsed: " + entry.first);
					parsed++;
					ParseState<ContextParserTuple> twoBest = getStateWithNBestTuples(10);
					Iterator<ContextParserTuple> iter = twoBest.iterator();

					while (iter.hasNext()) {
						TTRRecordType ttr = (TTRRecordType) iter.next().getTree().getMaximalSemantics();
						if (ttr == null) {
							ttr = TTRRecordType.parse("[]");
						}
						if (ttr.subsumes(targetRT) && targetRT.subsumes(ttr)) {
							System.out.println("same formula/maximal match");
							pair[0] = ttr;
							myttrs.add(pair);
							sameF++;
							continue corpusLoop;
						} else {
							float mapped = e.totalNodesMapped(ttr, targetRT);
							if (mapped >= mostNodesMapped) {
								mostNodesMapped = mapped;
								pair[0] = ttr;// always gets the highest mapped of the top two parses
							}
						}
					}

				} else {
					pair[0] = TTRRecordType.parse("[]");
				}
			} catch (Exception f) {
				pair[0] = TTRRecordType.parse("[]");
			}

			myttrs.add(pair);
		}

		List<Float> macro = e.precisionRecallMacro(myttrs);
		EvaluationResult micro = e.precisionRecallMicro(myttrs);
		System.out.println(parsed);
		System.out.println(total);
		System.out.println("parsed:" + (float) parsed / (float) total);
		System.out.println("same formula:" + (float) sameF / (float) parsed);
		System.out.println("\nMACRO scores:");
		System.out.println("precision: " + macro.get(0));
		System.out.println("recall: " + macro.get(1));
		System.out.println("f-score " + macro.get(2));
		System.out.println("\nMICRO scores:");
		System.out.println("precision: " + micro.getPrecision());
		System.out.println("recall: " + micro.getRecall());
		System.out.println("f-score: " + micro.getFScore());
	}

	public static void main(String a[]) {
		if (a.length > 0 && a[0].toLowerCase().startsWith("en")) {
			TestParser tp = new TestParser("resource/2013-english-ttr".replaceAll("/", File.separator));
			// pause();
			tp.loadTestCorpus("corpus/CHILDES/eveTrainPairs/CHILDESconversion.txt".replaceAll("/", File.separator),
					true);
			tp.parseCorpusToFile("TestCHILDESErrors.txt", "TestCHILDESRTs.txt", 30, false, false);
		} else {
			TestParser tp = new TestParser("resource/2015-english-ttr-robot".replaceAll("/", File.separator));
			// pause();
			tp.loadTestCorpus("corpus/Robot/Famula.txt".replaceAll("/", File.separator), false);
			tp.parseCorpusToFile("TestTTRRobotCorpusRTs.txt", "TestTTRRobotCorpusErrors.txt", 30, true, false);
		}
	}
}
