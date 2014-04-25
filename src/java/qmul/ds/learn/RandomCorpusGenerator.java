package qmul.ds.learn;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.ContextParser;
import qmul.ds.ContextParserTuple;
import qmul.ds.ParseState;
import qmul.ds.Parser;
import qmul.ds.ParserTuple;
import qmul.ds.action.Action;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.Lexicon;
import qmul.ds.tree.Tree;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;

public class RandomCorpusGenerator<T extends ParserTuple> {

	private static Logger logger = Logger.getLogger(RandomCorpusGenerator.class);

	private Parser<T> parser;
	static Corpus<Tree> corpus = new Corpus();
	static Corpus<Tree> test = new Corpus();
	public Lexicon lexicon;
	public Lexicon trainingLexicon = new Lexicon();
	public HashMap<String, int[]> distributions = new HashMap<String, int[]>(); // this
																				// will
																				// eventually
																				// be
																				// map
																				// from
																				// a
																				// string
																				// of
																				// POS
																				// type
																				// to
																				// a
																				// quadruple
																				// of
																				// {%types,%tokens,No.types,No.tokens};
	public HashMap<String, String[]> POStypes = new HashMap<String, String[]>();
	public static final String corpusFolder = "corpus/";
	// specify the first two elements of these arrays for the type and token
	// ratios of each POS respectively
	public int[] nounDistribution = { 58, 13, 0, 0 };
	public int[] verbDistribution = { 13, 16, 0, 0 };
	public int[] adjectiveDistribution = { 11, 3, 0, 0 };
	public int[] adverbDistribution = { 3, 8, 0, 0 };
	public int[] pronounDistribution = { 6, 21, 0, 0 }; // should be 6,21
	public int[] determinerDistribution = { 1, 6, 0, 0 }; // should be 1,6, but
															// add a few more in
															// for small corpus
	public int[] prepositionDistribution = { 1, 5, 0, 0 };
	public int[] conjunctionDistribution = { 1, 2, 0, 0 };
	public int[] othersDistribution = { 10, 24, 0, 0 };

	// specify which distributions you want included, the following two lists
	// must match
	public String[] typeList = { "noun", "verb", "pronoun", "determiner" };
	public int[][] allDistributions = { nounDistribution, verbDistribution, pronounDistribution, determinerDistribution };

	public ArrayList<String[]> wordTokenDistributions;// this will be generated
														// in the
														// generateDistributionTokens
														// method below
	public HashMap<String, Collection<String>> typeUseList; /*
															 * for now just simple occurrences of types that occur;
															 * should be a map between words and an array of integers
															 * representing the number of times the word has appeared in
															 * each position in the string in the corpus
															 */

	public int[] sentenceLengthRange = { 2, 6 }; // minimum and maximum sentence
													// length
	public float meanTargetSentenceLength = (float) 4;
	public float meanSentenceLength = 0; // this will be calculated
											// incrementally
	public float marginForError = (float) 0.30; // a bit like a standard
												// deviation
	public int lexiconSize = 200; // target lexicon size (number of different
									// types)
	public int corpusLength = 200; // target number of sentences in corpus

	public int beamWidth = 15; // the number of tuples we keep in a state after
								// parsing each word

	public RandomCorpusGenerator(Parser<T> parser) {

		this.parser = parser;
		/* initialise useList */
		typeUseList = new HashMap<String, Collection<String>>();

		// populate the POS types with suffixes
		POStypes.put("noun", new String[] { "count", "mass", "proper" });
		POStypes.put("verb", new String[] { "v_" });
		POStypes.put("adjective", new String[] { "adj" });
		POStypes.put("pronoun", new String[] { "pron_" });
		POStypes.put("determiner", new String[] { "det_" });
		POStypes.put("preposition", new String[] { "prep_" });
		// POStypes.put("conjunction", new String[] {"conj"}); //what are the
		// tags for conjunctions?

		/*
		 * Lexicon lexicon=parser.getLexicon(); for (String word : lexicon.keySet()) { int[] posArray = new
		 * int[10];//assuming ten word sentences for now in the position array useList.put(word, posArray); }
		 */
	}

	/**
	 * Method to create a lexicon from file based on distributions defined in the global distribution variables above,
	 * For the lexicon only the first value of the distribution arrays (the type count) is important
	 * 
	 */
	public Lexicon createLexicon(String dirNameOrURL) {

		adjustTypesTokenTargets(); // adjust the distributions
		// Populate the lexicon now and create file from the big list
		return writeLexicon(dirNameOrURL);

	}

	/**
	 * Method to write a lexicon.txt file and return a @Lexicon object, based on the target numbers in
	 * this.distributions for each type Tries to instantiate all the lexical items it samples from the large file
	 * 
	 * @param dirNameOrURL
	 *            the location of the file containing the lexical and computational actions files
	 */
	public Lexicon writeLexicon(String dirNameOrURL) {
		return new Lexicon(dirNameOrURL, this.POStypes, this.distributions, this.lexiconSize);
	}

	/**
	 * Method to generate the token distribution list from a lexicon and the token distribution (i.e. generate the
	 * appropriate number of tokens for each type, using random lexical actions) ambiguity a slight problem here? Store
	 * the word, its type and its semantics (bank, bank) (drive, drive)
	 * 
	 * @param lexicon
	 */
	public void generateTokenDistributions(Lexicon lexicon) {
		ArrayList<String[]> finalList = new ArrayList<String[]>();
		HashMap<String, ArrayList<String[]>> POSwords = new HashMap<String, ArrayList<String[]>>();

		adjustTypesTokenTargets(); // only needed for types, but won't harm

		for (String POStype : this.distributions.keySet()) {
			POSwords.put(POStype, new ArrayList<String[]>());
		}

		for (Collection<LexicalAction> lex : lexicon.values()) {
			for (LexicalAction lexicalAct : lex) {
				POS: for (String POS : this.distributions.keySet()) {
					for (String prefix : this.POStypes.get(POS)) {
						logger.info((lexicalAct.getLexicalActionType()));
						if (lexicalAct.getLexicalActionType().startsWith(prefix)) {
							String[] word = { lexicalAct.getWord(), lexicalAct.getLexicalActionType() };
							POSwords.get(POS).add(word);
							break POS;
						}
					}
				}
			}
		}

		// now we've got all the different words and action names in their
		// correct lists and have the correct target distributions!
		// now generate the list, just by adding words from initial random
		// shuffle of lexicon continually until targets reached
		// could sample even more randomly, but this ok for now
		String POSinfo = "";
		float massNounPercentage = 0.4F; // add hoc way of controlling for too many count nouns...
		float countNounPercentage = 0.4F;
		float properNounPercentage = 0.2F;
		int countNounTokens = 0;
		POSLoop: for (String POS : POSwords.keySet()) {
			int total = 0;
			logger.info(POS);
			logger.info("target = " + this.distributions.get(POS)[1]);
			// pause("");
			while (total < this.distributions.get(POS)[1]) {
				List<String[]> keylist = POSwords.get(POS);
				logger.info(keylist.size());
				if (keylist.size() == 0) {
					logger.info("no types for " + POS);
					pause("");
					continue POSLoop;
				}
				Collections.shuffle(keylist);

				for (String[] wordAction : keylist) {
					if (POS == "noun") {
						if (wordAction[1] == "count") {
							if (countNounTokens >= Math.round(countNounPercentage * this.distributions.get(POS)[1])) {
								continue;
							}
							countNounTokens++;
						}

					}
					if (total == this.distributions.get(POS)[1]) {
						logger.info("TARGET REACHED FOR" + POS);
						// pause("");
						POSinfo += POS + " :" + total + "\n";
						break;
					}
					finalList.add(wordAction);
					total++;
				}
			}
		}

		// pause(POSinfo);

		this.wordTokenDistributions = finalList;

	}

	/**
	 * Remember to adjust the state upon first call, i.e. state should not be empty initially.
	 * 
	 * @param <T>
	 * @param soFar
	 * @param maxLength
	 */
	public void generate(String soFar, int maxLength) {
		logger.info("generate called");

		ParseState<T> prevState = this.parser.getState().clone();

		Collections.shuffle(this.wordTokenDistributions); // shuffles words in
															// lexicon
		int relativeClauseLimit = 1; // (number of relative clauses,
										// essentially...)
		int relativeClause = 0;

		lexicalLoop: for (String[] wordActionPair : this.wordTokenDistributions) {
			LexicalAction myAction = null;
			Boolean relativeWord = false;
			String candidateAction = wordActionPair[1];
			if (candidateAction.contains("rel") && relativeClause == relativeClauseLimit) {
				continue lexicalLoop; // only allow one relative clause,\TODO
										// could make this a parameter
			}

			String word = wordActionPair[0];
			// if (useList.get(word)[soFar.split("\\s+").length-1]<3){} //to
			// ensure a word only appears in that position 3 times in the corpus
			if (corpus.size() >= this.corpusLength) { // we've reached
																// the target
				logger.info("CORPUS COMPLETE");
				return;
			}

			prevState = parser.getState();
			ParseState<T> currentState = parser.parseWord(word);
			parser.setState(parser.getStateWithNBestTuples(this.beamWidth)); // beam
																				// parse
																				// width

			if (currentState == null) {
				continue lexicalLoop;
			}

			// we've got a parse //parser.getState()!=null &&
			if (!parser.getState().isEmpty()) {
				for (Action action : ((ContextParserTuple) parser.getState().first()).getActions()) {
					if (action instanceof LexicalAction) {
						myAction = (LexicalAction) action;
						if (myAction.getLexicalActionType().contains("rel")) { // if last word parse is a
																				// relative clause word..
							relativeClause++;
							relativeWord = true;
							break; // the word just parsed is a relative
						}
					}
				}

				ParseState<T> complete = parser.getState().complete();

				if (!complete.isEmpty()) // i.e. if we have a complete tree..
				{
					String completeSent = (soFar + " " + word).trim();

					if (!complete.first().getTree().hasLink()) // checking for
																// linked tree
					{
						Random rand = new Random();
						float test = rand.nextFloat() * 100; // gets new random
						if (test > 5) { // this acts as a likelihood of allowing
										// links, higher = less likely to allow
										// link, only really for modifiers? John
										// likes Mary who snores.
							soFar = completeSent;
							logger.info("So far: " + soFar); // added word
																// successfully
							// parser.setStateToBestTuple();
							logger.info("Best Tree: " + parser.getState().first());
							continue lexicalLoop;// random test to see if it
													// should continue parsing
													// the rest of the sentence
													// or not (i.e. trying to
													// link off this tree)
						}
					}

					// check for distributions and violations of range now

					int mylength = completeSent.split("\\s").length;
					float aggregate = meanSentenceLength * corpus.size(); // should actually an int
					float candidateMean = (aggregate + ((float) mylength)) / (corpus.size() + 1);
					float candidateAggregate = candidateMean * (corpus.size() + 1);
					float remainTargetAggregate = meanTargetSentenceLength
							* (corpusLength - (corpus.size() + 1));
					/*
					 * logger.info("currentmean " + meanSentenceLength); logger.info("corpuslength" +
					 * corpus.keySet().size()); logger.info(candidateMean); logger.info("candidate aggregate = " +
					 * candidateAggregate); logger.info("remain aggregate = " + remainTargetAggregate); pause("");
					 */
					if (((candidateAggregate + remainTargetAggregate) / corpusLength) < (meanTargetSentenceLength - marginForError)
							|| mylength < this.sentenceLengthRange[0]) {// we're
																		// too
																		// low,
																		// need
																		// to
																		// remove
																		// a
																		// word
																		// and
																		// carry
																		// on to
																		// try
																		// for
																		// longer
																		// sentences
						parser.setState(prevState);
						if (relativeWord == true) {
							relativeClause--;
						}
						continue lexicalLoop;
					} else if (((candidateAggregate + remainTargetAggregate) / corpusLength) > (meanTargetSentenceLength + marginForError)
							|| mylength > this.sentenceLengthRange[1]) {
						// we're too high, we need to back track quite a bit?
						// Maybe start again..
						init();
						soFar = "";
						relativeClause = 0;
						continue lexicalLoop;
					}

					logger.info("=====" + completeSent + "====");
					/*
					 * //for each word in the completeSent, increment its count for its position in the uselist for (int
					 * i = 1; i<completeSent.split("\\s+").length; i++) { String w = completeSent.split("\\s+")[i];
					 * useList.get(w)[i-1]++; //adds one to the appropriate position in the word's posArray }
					 */
					String[] sentArray = completeSent.split("\\s");

					List<String> sentList = Arrays.asList(sentArray);
					Sentence<Word> sent = Sentence.toSentence(sentList);
					Pair<Sentence<Word>, Tree> item=new Pair<Sentence<Word>, Tree>(sent, complete.first().getTree());
					corpus.add(item); // add the sentence to the corpus

					this.meanSentenceLength = candidateMean;

					System.out.println("Found Complete Sentence: " + completeSent + "\n Content: "
							+ item.second());
					System.out.println("average sentence length = " + candidateMean);
					// pause("");

					// add it to the totals:
					// add all the lexical actions to the training lexicon:
					for (Action action : ((ContextParserTuple) complete.first()).getActions()) {
						if (action instanceof LexicalAction) {
							String myWord = ((LexicalAction) action).getWord();
							if (!this.trainingLexicon.keySet().contains(myWord)) {
								this.trainingLexicon.put(myWord, new HashSet<LexicalAction>());
							}
							this.trainingLexicon.get(myWord).add((LexicalAction) action);
							addPOStokenCount(((LexicalAction) action).getLexicalActionType());
							addPOStypeCount(myWord, ((LexicalAction) action).getLexicalActionType());
						}
					}

					init(); // Initialise the parser, keep looping through, may
							// result in two words in same sentence
					relativeClause = 0;
					soFar = "";
					continue lexicalLoop;

				} else {
					if (soFar.split("\\s+").length >= maxLength) { // have
																	// already
																	// gone over
																	// or up to
																	// the range
																	// limit
						relativeClause = 0;
						soFar = ""; // should really back-track here, could save
									// state before these words and use this?
						break;

					} else {
						soFar += " " + word;
						logger.info("So far: " + soFar); // added word
															// successfully
						logger.info("Best Tree: " + parser.getState().first());
						// useList.get(word)[soFar.split("\\s+").length-1]++;
						// //adds one to that index in the array

					}

				}

			} else { // returning null from a parse- need to reset or not?

				init();
				relativeClause = 0;
				soFar = "";
				continue lexicalLoop;

			}

			/*
			 * System.out.println("EXCEEDED USAGE: " + word + " after soFar: " + soFar + ":at position:" +
			 * (soFar.split("\\s+").length-1) + "useage" + useList.get(word)[soFar.split("\\s+").length-1] + ":"); for
			 * (int o : useList.get(word)) { System.out.print(o + ","); } for(String sent: corpus.keySet()) {
			 * System.out.println(sent);
			 * 
			 * } pause("");
			 */
		}

		generate(soFar, maxLength);

	}

	public float getAverageWordRep() {
		Set<Word> wordSet = new HashSet<Word>();
		float allwords = 0.0f;
		for (Pair<Sentence<Word>, Tree> item : corpus) {
			Sentence<Word> s=item.first();
			for (Word w : s) {
				wordSet.add(w);
				allwords++;
			}
		}
		return allwords / (float) wordSet.size();
	}

	/*
	 * Adds a seen token to a POS type if it's the appropriate type
	 */
	public void addPOStokenCount(String lexicalActionName) {
		boolean added = false;
		POS: for (String POStype : this.POStypes.keySet()) {
			for (String prefix : this.POStypes.get(POStype)) {
				if (lexicalActionName.startsWith(prefix)) {
					added = true;
					this.distributions.get(POStype)[3]++;
					break POS;
				}
			}
		}
		if (added == false) {
			logger.warn("POS TYPE NOT PRESENT FOR " + lexicalActionName);
			pause("");
		}
		return;

	}

	public void addPOStypeCount(String word, String lexicalActionName) {

		boolean added = false;
		POS: for (String POStype : this.POStypes.keySet()) {
			for (String prefix : this.POStypes.get(POStype)) {
				if (lexicalActionName.startsWith(prefix)) {
					if (this.typeUseList.containsKey(POStype)) {
						if (!this.typeUseList.get(POStype).contains(word)) {
							this.typeUseList.get(POStype).add(word);
							this.distributions.get(POStype)[2]++;
						}
						added = true;
						break POS; // if it's there, should stop searching
									// anyway
					} else {
						Collection<String> collec = new HashSet<String>();
						collec.add(word);
						this.typeUseList.put(POStype, collec);
						this.distributions.get(POStype)[2]++;
						added = true;
						break POS;
					}
				}
			}
		}
		if (added == false) {
			logger.warn("POS TYPE NOT PRESENT FOR " + lexicalActionName);
			pause("");
		}
		return;

	}

	public void adjustTypesTokenTargets() {
		int totalTypeSize = 0;
		int totalTokenSize = 0; // tokens not really relevant here, but may be
								// in future development
		int myNewTypeTotal = 0;
		int myNewTokenTotal = 0;

		if (this.typeList.length != allDistributions.length) {
			logger.info("MISMATCH between type list and distribution list");
			System.exit(0);
		}

		for (int[] distribution : this.allDistributions) {
			totalTypeSize += distribution[0];
			totalTokenSize += distribution[1];
		}
		// now get the target numbers of each type and token i.e. from frequency
		// > target real numbers
		// append target numbers
		for (int[] distribution : this.allDistributions) {
			if (distribution[0] == 0 || distribution[1] == 0) {
				continue;
			}
			float typesPercentage = (float) distribution[0] / totalTypeSize;
			float tokensPercentage = (float) distribution[1] / totalTokenSize;
			int types = (typesPercentage * this.lexiconSize) < 0.5 ? 1 : Math.round(typesPercentage * this.lexiconSize);
			int tokens = (tokensPercentage * this.corpusLength * this.meanTargetSentenceLength) < 0.5 ? 1 : Math
					.round(tokensPercentage * this.corpusLength * this.meanTargetSentenceLength);
			distribution[0] = types;
			distribution[1] = tokens;

			myNewTypeTotal += types;
			myNewTokenTotal += tokens;

		}

		// if the tokens or types don't quite add up to the target size, we need
		// to adjust one or more of the individual distributions
		// first for types
		if (myNewTypeTotal != this.lexiconSize) {
			boolean positiveDifference = myNewTypeTotal < this.lexiconSize ? true : false;
			// adjust one or more of the distributions at random to get the
			// correct target lexicon size
			while (myNewTypeTotal != this.lexiconSize) {
				Integer[] randomNumbers = new Integer[this.allDistributions.length];
				for (int i = 0; i < this.allDistributions.length; i++) {
					randomNumbers[i] = i;
				}
				Collections.shuffle(Arrays.asList(randomNumbers)); // not
																	// shuffling
																	// list
																	// directly
																	// to avoid
																	// complications
				int randomNum = randomNumbers[0];
				logger.info(randomNum);
				int i = 0;
				for (int[] distribution : allDistributions) {
					if (i != randomNum) {
						i = i + 1;
						continue;
					}
					if (distribution[0] > 1) {
						if (positiveDifference) {
							distribution[0] += 1;
							myNewTypeTotal += 1;
						} else {
							distribution[0] -= 1;
							myNewTypeTotal -= 1;
						}
					}
					i = i + 1;
				}
			}
		}

		// now for tokens
		if (myNewTokenTotal != this.corpusLength * this.meanTargetSentenceLength) { // not quite right, as this
																					// suggests all 6 word
																					// sentences, but helps
																					// scale up
			boolean positiveDifference = myNewTokenTotal < (this.corpusLength * this.meanTargetSentenceLength) ? true
					: false;
			// adjust one or more of the distributions at random to get the
			// correct target corpus length
			while (myNewTokenTotal != this.corpusLength * this.meanTargetSentenceLength) {
				Integer[] randomNumbers = new Integer[this.allDistributions.length];
				for (int i = 0; i < this.allDistributions.length; i++) {
					randomNumbers[i] = i;
				}
				Collections.shuffle(Arrays.asList(randomNumbers)); // not
																	// shuffling
																	// list
																	// directly
																	// to avoid
																	// complications
				int randomNum = randomNumbers[0];
				logger.info(randomNum);
				int i = 0;
				for (int[] distribution : allDistributions) {
					if (i != randomNum) {
						i = i + 1;
						continue;
					}
					if (distribution[1] > 1) {
						if (positiveDifference) {
							distribution[1] += 1;
							myNewTokenTotal += 1;
						} else {
							distribution[1] -= 1;
							myNewTokenTotal -= 1;
						}
					}
					i = i + 1;
				}
			}
		}

		// now all the different type and token numbers should be correct.
		// populate distributions hash map
		int targetLexiconSize = 0;
		int targetCorpusSize = 0;
		String targetInfo = "";
		for (int i = 0; i < allDistributions.length; i++) {
			distributions.put(typeList[i], allDistributions[i]);
			targetInfo += typeList[i] + ": ";
			for (int size : allDistributions[i]) {
				targetInfo += size + ",";
			}
			targetInfo += "\n";
			targetLexiconSize += allDistributions[i][0];
			targetCorpusSize += allDistributions[i][1];
		}
		logger.info(targetInfo);
		logger.info("target total lexicon size = " + targetLexiconSize); // to
																			// check
																			// we've
																			// got
																			// the
																			// right
																			// target
																			// numbers
		logger.info("target total Corpus size = " + targetCorpusSize);
		// pause("");

	}

	public Corpus<Tree> getCorpus() {
		return corpus;
	}

	public void init() {
		this.parser.init();
	}

	public void setParser(Parser myParser) {
		this.parser = myParser;
	}

	public static void pause(String string) {
		System.out.println(string);
		System.out.println("Press enter to continue...");
		try {
			System.in.read();
		} catch (Exception e) {
		}

	}

	public Lexicon getLexicon() {

		return this.parser.getLexicon();

	}

	public void splitCorpus(float percent) {
		if (corpus.isEmpty())
			return;
		float howmany = percent * corpus.size();
		logger.info(percent + " of corpus=" + howmany);
		int i = 0;
	
		ArrayList<Pair<Sentence<Word>, Tree>> corpusCopy=new ArrayList<Pair<Sentence<Word>, Tree>>(corpus);
		Collections.shuffle(corpusCopy);
		
		for (Pair<Sentence<Word>,Tree> item : corpusCopy) {
			if (i > howmany)
				break;
			Sentence<Word> s=item.first();
			test.add(new Pair<Sentence<Word>, Tree>(s, item.second()));
			i++;
			corpus.remove(item);
		}
	
	}

	/**
	 * 
	 * @param split
	 *            the proportion/1 of the number of test sentences we want
	 * @param SameActionsInTesting
	 *            true if we want to use the same actions we've seen in training, if false this is uncontrolled
	 */
	public void generateCorpus(float split, boolean SameActionsInTesting) {
		int testCorpusLength = Math.round(split * this.corpusLength);
		this.corpusLength = this.corpusLength - testCorpusLength;
		generateTokenDistributions(this.parser.getLexicon());
		init(); // initialises parser
		logger.info("target size = " + this.corpusLength);
		pause("");
		generate("", sentenceLengthRange[1]); // will generate until we've got training corpus
		ArrayList<Pair<Sentence<Word>, Tree>> corpusCopy=new ArrayList<Pair<Sentence<Word>, Tree>>(corpus);
		Collections.shuffle(corpusCopy); // shuffling to make sure order of generation has no effect
		Corpus<Tree> trainingCorpus = new Corpus<Tree>();
		for (Pair<Sentence<Word>, Tree> item : corpusCopy) {
			trainingCorpus.add(new Pair<Sentence<Word>,Tree>(item.first(), item.second()));
		}

		pause("Training corpus complete");
		if (split > 0) { // if we have a test corpus setting
			corpus = new Corpus(); // reset the corpus
			corpusLength = testCorpusLength;

			if (SameActionsInTesting == true) {
				Parser<ContextParserTuple> cp = new ContextParser(this.trainingLexicon, this.parser.getGrammar());
				setParser(cp);
			}

			adjustTypesTokenTargets();
			generateTokenDistributions(this.parser.getLexicon());
			init();

			logger.info("Target size = " + this.corpusLength);
			pause("");
			generate("", sentenceLengthRange[1]);

			Corpus<Tree> testCorpus = getCorpus();
			ArrayList<Pair<Sentence<Word>, Tree>> testShuffled = new ArrayList(testCorpus);
			Collections.shuffle(testShuffled);
			for (Pair<Sentence<Word>,Tree> s : testShuffled) {
				test.add(new Pair<Sentence<Word>, Tree>(s.first(), s.second()));
			}
			corpusLength = corpus.size() + test.size();
			pause("Test corpus complete");

		}

		corpus = trainingCorpus;

	}

	public static void main(String args[]) {
		String filename = "resource" + File.separator + "2009-english-test-induction";

		Parser<ContextParserTuple> p = new ContextParser(filename);

		RandomCorpusGenerator<ContextParserTuple> gen = new RandomCorpusGenerator<ContextParserTuple>(p);
		// Lexicon lex = gen.createLexicon(filename);
		// need to do this
		// separately to generating at the moment.

		gen.generateCorpus(0.2f, true);

		// gen.splitCorpus(0.2f);
		try {
			FileWriter fstream = new FileWriter(corpusFolder + "corpus.txt");
			BufferedWriter out = new BufferedWriter(fstream);
			for (Pair<Sentence<Word>, Tree> item : gen.corpus) {
				out.write(item.first().toString() + "\n");
			}
			out.close();

		} catch (Exception e) {
			logger.error("Could not write the corpus to file " + corpusFolder + "corpus.txt");
		}

		try {
			FileWriter fstream = new FileWriter(corpusFolder + "testCorpus.txt");

			BufferedWriter out = new BufferedWriter(fstream);
			for (Pair<Sentence<Word>, Tree> sentence : gen.test) {
				out.write(sentence.first().toString() + "\n");
			}
			out.close();

		} catch (Exception e) {
			logger.error("Could not write the corpus to file " + corpusFolder + "testCorpus.txt");
		}

		int typeTotal = 0;
		int tokenTotal = 0;
		String corpusInfo = "";
		corpusInfo += "%%%%%%%%% CORPUS INFO \n Types and Tokens (target types, target tokens, actual types, actual tokens,) \n";
		for (String key : gen.distributions.keySet()) {
			corpusInfo += key + " : ";
			typeTotal += gen.distributions.get(key)[2];
			tokenTotal += gen.distributions.get(key)[3];
			for (int me : gen.distributions.get(key)) {
				corpusInfo += me + ",";
			}
			corpusInfo += "\n";

		}
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(corpusFolder + "stats.txt"));
			writer.write(corpusInfo);
			writer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}

		corpusInfo = " \n %%%%%%%%% Statistics (types, tokens) %%%%%%%%% \n";
		for (String key : gen.distributions.keySet()) {
			corpusInfo += key + " : ";
			corpusInfo += String.valueOf((float) gen.distributions.get(key)[2] / typeTotal * 100) + ", ";
			corpusInfo += String.valueOf((float) gen.distributions.get(key)[3] / tokenTotal * 100) + ", ";
			corpusInfo += "\n";

		}
		corpusInfo += "\n mean sentence length = " + gen.meanSentenceLength;
		corpusInfo += "\n average word repetition = " + gen.getAverageWordRep();
		try {
			writer.write(corpusInfo);
			writer.flush();
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		Corpus<Tree> corpus = gen.getCorpus();
		try {
			FileOutputStream fout = new FileOutputStream(corpusFolder + "corpus.corpus");
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(corpus);
			oos.close();
			logger.info("\n Written to " + corpusFolder + "corpus.corpus");
		} catch (IOException e) {
			e.printStackTrace();
		}
		Corpus<Tree> testcorpus = gen.test;
		try {
			FileOutputStream fout = new FileOutputStream(corpusFolder + "testCorpus.corpus");
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(testcorpus);
			oos.close();
			logger.info("\n Written to " + corpusFolder + "testCorpus.corpus");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
