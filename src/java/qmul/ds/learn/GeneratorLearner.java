/**
 * 
 */
package qmul.ds.learn;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;

import qmul.ds.DAGGenerator;
import qmul.ds.DAGParser;
import qmul.ds.Dialogue;
import qmul.ds.InteractiveContextParser;
import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.dag.UtteredWord;
import qmul.ds.formula.TTRField;
import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRRecordType;

/**
 * @author Ash      
 */

// TODO: save the hashmap as a csv file so it's human readable, or do it using Properties like this:
// https://stackoverflow.com/questions/4738162/java-writing-reading-a-map-from-disk
// TODO: review supervision transcriptions for ideas: weight? generation?
// TODO: ADD DOCS to methods/classes
// TODO: implement the following:
// parse, parseWord, generate, getDialogueHistory, isExhausted, 
// TODO: save modelPath as a constant string so I can access it in BFG class by importing it from here.
// TODO: ADD METHOD `LEARN`


public class GeneratorLearner {

	protected DAGParser<? extends DAGTuple, ? extends DAGEdge> parser;
	RecordTypeCorpus corpus = new RecordTypeCorpus();
	// `Object` is used because feature types can be TTRRecordType or ?
	// but how to deal with it???
	protected HashMap<String, HashMap<TTRRecordType, Integer>> conditionalCountTable = new HashMap<String, HashMap<TTRRecordType, Integer>>();
	protected HashMap<String, HashMap<TTRRecordType, Double>> conditionalProbTable = new HashMap<String, HashMap<TTRRecordType, Double>>();
// what was this doing?
	static final String corpusPath = "corpus/CHILDES/eveTrainPairs/CHILDESconversion400Final.txt".replaceAll("/", Matcher.quoteReplacement(File.separator));
	 
	 // --------------------------------------------------------------------------------------------------------------
	 
	 /**
	  * Constructor from parser and corpus objects
	  * 
	  * @param parser
	  * @param corpus
	  */
	public GeneratorLearner(DAGParser<? extends DAGTuple, ? extends DAGEdge> parser, RecordTypeCorpus corpus)
	{
		this.parser = parser;
		this.corpus = corpus;
	}
	
	/**
	 * Constructor from parser and corpus paths
	 * 
	 * @param parserPath
	 * @param corpusPath
	 */
	public GeneratorLearner(String parserPath, String corpusPath)
	{
		try { // I think this needs to be changed to RTcorpus
			this.corpus.loadCorpus(new File(corpusPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// TODO: load parser from learnt files / resources.
		Lexicon l = new Lexicon();
		l.loadLearntLexiconTxt(corpusPath, 3);
		Grammar g = new Grammar(parserPath);
		InteractiveContextParser p = new InteractiveContextParser(l, g);
		this.parser = p;
	}

/**
 * Computes "goldSem-curSem"; minus operator had been defined over RTs.
 * 
 * @param curSem   the semantics of what we have generated so far.
 * @param goldSem  the complete semantics of the sentence we want to generate to.
 * @return rInc    (Incremental RT) the semantics to be generated, calculated as `goldSem-curSem`
 */
	public static TTRRecordType computeRInc(TTRRecordType curSem, TTRRecordType goldSem) {
		TTRRecordType rInc = goldSem.minus(curSem).first(); // there's first() and second() which second kind of means `curSem-goldSem`.
		return rInc;
	}

	/**
	 * Normalises a table by dividing elements in a column by their sum.
	 * 
	 * @param table				     the table to be normalised
	 * @return conditionalProbTable  the normalised table
	 */
	public HashMap<String, HashMap<TTRRecordType, Double>> normaliseCountTable(HashMap<String, HashMap<TTRRecordType, Integer>> table)
	{
		HashMap<TTRRecordType, Double> total = new HashMap<TTRRecordType, Double>(); // Don't have to init to zero since I'm using getOrDefault method.
		// First find total of each column in this loop
		for (HashMap<TTRRecordType, Integer> row : table.values())
		{
			for (TTRRecordType feature : row.keySet()) {
				Integer count = row.get(feature);
				total.put(feature, total.getOrDefault(feature, 0.0) + count); // If `feature` was already in `total`, add `count` to the previous value.
													 // If not, add `count` to 0, which means just put `count`. Used because key might not be available.
			}
		}

		for (String word : table.keySet()) // Divide columns by the corresponding `total` to get probabilities and save them in `conditionalProbTable`.
		{
			HashMap<TTRRecordType, Integer> row = table.get(word);
			for (TTRRecordType rt : row.keySet()) {
				Double prob = ((double) row.get(rt)) / total.get(rt); // do I need to do casting?
				conditionalProbTable.get(word).put(rt, prob); // is this right?
			}
		}
		return conditionalProbTable;
	}

	public static void saveModelToFile(HashMap<String, HashMap<TTRRecordType, Double>> model)
			throws FileNotFoundException, IOException // eclipse recommended it so I said yes.
	{
		String modelPath = "resource/2022-DSProbNLG/model.properties".replaceAll("/", // ?
				Matcher.quoteReplacement(File.separator));

		FileOutputStream file = new FileOutputStream("model.txt");
		ObjectOutputStream oos = new ObjectOutputStream(file);
		oos.writeObject(model);
		oos.close();
	}

	/**
	 * TODO: 
	 */
	public void learn()
	{
		// parsing
		for (Pair<Sentence<Word>, TTRRecordType> pair : corpus) {
			Sentence<Word> sentence = pair.first();
			TTRRecordType goldSem = pair.second();
//			List<TTRRecordType> decomposedRTs = goldSem.decompose(); // have to test to see what's in there
			// Arash was saying sth about making RTs headless ???
			
			// this now has to be parseUtterance on a sentence converted to an utterance.
			for (Word word : sentence) {
				UtteredWord utteredWord = new UtteredWord(word.word());
				DAG<DAGTuple, GroundableEdge> parserState = parser.parseWord(utteredWord);
				TTRFormula curSem = parserState.getCurrentTuple().getSemantics(); // Q: is this the sem for the last
																					// word or all words until now??? i
																					// ASSUME IT'S FOR THE ALST WORD
				// which is probably not good because I want the accumulated sem for the whole
				// sentence (Context?)
				TTRRecordType rInc = computeRInc((TTRRecordType) curSem, goldSem);
				// have to make the decompositions headless? what's head tho?
				List<TTRRecordType> decomposedRInc = rInc.decompose();//decompose(rInc); // 9check what this returns *******
				// what do I have to sort here? decompose itself is sorting by sth.
				if (conditionalCountTable.containsKey(word.toString())) {
					for (TTRRecordType r_i : decomposedRInc) // this is definitely wrong.
					{
						Integer newCount = conditionalCountTable.get(word.toString()).getOrDefault(r_i, 0) + 1;
						conditionalCountTable.get(word.toString()).put(r_i, newCount);
					}
				} else {
					conditionalCountTable.put(word.toString(), null); // idk what to do here
					for (TTRRecordType r_i : decomposedRInc) // this is definitely wrong
						conditionalCountTable.get(word.toString()).put(r_i, 1);
				}
			}
		}
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String grammarPath = "resource/2013-ttr-learner-output/".replaceAll("/",
				Matcher.quoteReplacement(File.separator));

		Integer topN = 2; // why we don't need this!? what does it do anyway?

		InteractiveContextParser parser = new InteractiveContextParser(grammarPath); // LoadLearntGrammar.testFromText(grammarPath,
																						// corpusPath, 2);//new
																						// TestParser(grammarPath,
																						// corpusPath, topN);
//		learn();
//		Boolean parsedCorpus = parse(); // WHATTTTTTTTT AM I SUPPOSED OT DO?

		conditionalProbTable = normaliseCountTable(conditionalCountTable);
		try {
			saveModelToFile(conditionalProbTable);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // what?

		// how am I supposed to generate now?
		// for state in generator_state:
		// calculate potential states' probs (depending on possible actions) and execute
		// topBeam actions

	}
}
