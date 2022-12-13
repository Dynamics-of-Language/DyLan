/**
 * 
 */
package qmul.ds.learn;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

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
	protected HashMap<String, HashMap<TTRRecordType, Double>> conditionalProbTable = new HashMap<String, HashMap<TTRRecordType, Double>>(); // TODO attention: these are NOT being globally updated.
// what was this doing?
	static final String corpusPath = "dsttr/corpus/CHILDES/eveTrainPairs/CHILDESconversion400Final.txt".replaceAll("/", Matcher.quoteReplacement(File.separator));
	static final String grammarPath = "dsttr/resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
	
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
//		Lexicon l = new Lexicon();
//		l.loadLearntLexiconTxt(corpusPath, 3);
//		Grammar g = new Grammar(parserPath);
//		InteractiveContextParser p = new InteractiveContextParser(l, g);
//		this.parser = p;
//		InteractiveContextParser generatorParser = 
		this.parser = new InteractiveContextParser(parserPath);
	}

/**
 * Computes "goldSem-curSem"; minus operator had been defined over RTs.
 * 
 * @param curSem   the semantics of what we have generated so far.
 * @param goldSem  the complete semantics of the sentence we want to generate to.
 * @return rInc    (Incremental RT) the semantics to be generated, calculated as `goldSem-curSem`
 */
	public TTRRecordType computeRInc(TTRRecordType goldSem, TTRRecordType curSem) {
		TTRRecordType rInc = goldSem.minus(curSem).first(); // there's first() and second() which second kind of means `curSem-goldSem`.
		return rInc;
	}
	
	/**
	 * Loads the learnt grammar by Eshghi et al. 2013 from a text file.
	 * 
	 * @return generatorParser  an InterActiveContextParser instantiated from previously learnt model.
	 */
//	public InteractiveContextParser loadParser(String grammarPath)
//	{
//		// TODO which grammar is it loading? Top N-wise? 1,2,3?
////		String grammarPath = "resource/2013-ttr-learner-output/".replaceAll("/",
////				Matcher.quoteReplacement(File.separator));
//		InteractiveContextParser generatorParser = new InteractiveContextParser(grammarPath);
//		return generatorParser;
//	}
	
	/**
	 * Loads the CHILDES corpus used in Eshghi et al. 2013.
	 * 
	 * @return
	 */
//	public RecordTypeCorpus loadCorpus(String path)
//	{
////		String corpusPath = "corpus/CHILDES/eveTrainPairs/CHILDESconversion400Final.txt".replaceAll("/",
////				Matcher.quoteReplacement(File.separator));
////		File corpusFile = new File();
//		RecordTypeCorpus rtcorpus = new RecordTypeCorpus();
//		try {
//			rtcorpus.loadCorpus(new File(path));
//			return rtcorpus;
//		
//		}
//		catch(Exception e) {
//			e.printStackTrace();
//		}
//
//	}


	/**
	 * Normalises a table by dividing elements in a column by their sum.
	 * 
	 * @param table				     the table to be normalised
	 * @return probTable  the normalised table
	 */
	public HashMap<String, HashMap<TTRRecordType, Double>> normaliseCountTable(HashMap<String, HashMap<TTRRecordType, Integer>> table)
	{
		HashMap<String, HashMap<TTRRecordType, Double>> probTable = new  HashMap<String, HashMap<TTRRecordType, Double>>();
		HashMap<TTRRecordType, Double> total = new HashMap<TTRRecordType, Double>(); // Don't have to init to zero since I'm using getOrDefault method.
		// First find total of each column in this loop
		for (HashMap<TTRRecordType, Integer> row : table.values())
		{
			for (TTRRecordType col : row.keySet()) {
				Integer count = row.get(col);
				total.put(col, total.getOrDefault(col, 0.0) + count); // If `feature` was already in `total`, add `count` to the previous value.
													 // If not, add `count` to 0, which means just put `count`. Used because key might not be available.
			}
		}

		for (String word : table.keySet()) // Divide columns by the corresponding `total` to get probabilities and save them in `conditionalProbTable`.
		{
			HashMap<TTRRecordType, Integer> row = table.get(word);
			HashMap<TTRRecordType, Double> probRow = new HashMap<TTRRecordType, Double>();
			for (TTRRecordType rt : row.keySet()) {
				Double prob = ((double) row.get(rt)) / total.get(rt); // do I need to do casting?
				probRow.put(rt, prob);
			}
			probTable.put(word, probRow);
		}
		return probTable;
	}

	/**
	 * Saves a HashMap<String, HashMap<TTRRecordType, Double>> to a csv file.
	 *
	 * @param model The 2D HashMap to be saved.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void saveModelToFile(HashMap<String, HashMap<TTRRecordType, Double>> model)
			throws FileNotFoundException, IOException // eclipse recommended it so I said yes.
	{
		// TODO write features: WORDS, feature1, feature2, ...
		// TODO Make more efficient
		// First, clear the file if it was already there. RF: https://stackoverflow.com/questions/6994518/how-to-delete-the-content-of-text-file-without-deleting-itself
		File f = new File(grammarPath + "model.csv");
		if (f.exists() && !f.isDirectory()) {
			PrintWriter writer = new PrintWriter(f);
			writer.print("");
			writer.close();
		}
		// Writing features row
		ArrayList<String> featuresStr = new ArrayList<String>();
		for (String wo: model.keySet()){ // This is not clean code.
			HashMap<TTRRecordType, Double> row = model.get(wo);
			ArrayList<TTRRecordType> features = new ArrayList<>(row.keySet());
			for (TTRRecordType feature : features) // todo make this more efficient
				featuresStr.add(feature.toString());
			break;
		}
		String strFeatures = String.join(",", featuresStr);
		strFeatures = "WORDS\\FEATURES" + "," + strFeatures + "\n";
		FileWriter writer1 = new FileWriter(grammarPath + "model.csv", true);
		writer1.write(strFeatures);
		writer1.close();

		// Writing words and probs
		for (var entry : model.entrySet()) // REF: https://stackoverflow.com/questions/46898/how-do-i-efficiently-iterate-over-each-entry-in-a-java-map
		{
			String word = entry.getKey();
			HashMap<TTRRecordType, Double> row = entry.getValue();
			ArrayList<Double> probs = new ArrayList<>(row.values());
			ArrayList<String> probsStr = new ArrayList<String>();
			for (Double prob : probs) // todo make this more efficient
				probsStr.add(Double.toString(prob));

			// Converts the list to a string joined by comma
			String strRow = String.join(",", probsStr); //REF: https://mkyong.com/java/java-how-to-join-list-string-with-commas/
			strRow = word + "," + strRow;
			// write to file
			try { // Do I need to use BufferedWriter as in https://stackoverflow.com/a/1625263/6306387 ?
				FileWriter writer = new FileWriter(grammarPath + "model.csv", true);
				writer.write(strRow);
				writer.write(System.lineSeparator()); // To go to next line. REF: https://stackoverflow.com/questions/18549704/create-a-new-line-in-javas-filewriter
				writer.close();
				System.out.println("Successfully wrote row the file."); // todo add logs + add what row!
			} catch (IOException e) {
				System.out.println("An error occurred.");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Learn to generate by:
	 * - Loading the CHILDES corpus.
	 * - Parsing <sentence, TTR-RT> pairs.
	 * - Matching the parse with the gold TTR-RT semantics.
	 * - Compute `rInc`.
	 * - Decompose `rInc`.
	 * - Populate `conditionalCountTable`.
	 * - Create `conditionalProbTable` by normalising `conditionalCountTable`.
	 * - Save `conditionalProbTable` to a csv file.
	 * 
	 */
	public void learn()
	{
		
//		RecordTypeCorpus corpus = loadCorpus(corpusPath);
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
	 * `main` is only used to test the methods here. This is not a runnable class.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
//		Boolean parsedCorpus = parse(); // WHATTTTTTTTT AM I SUPPOSED OT DO?
		
		// Testing `normaliseCountTable`
		// REF: https://stackoverflow.com/questions/11491750/cannot-make-a-static-reference-to-the-non-static-method-fxnint-from-the-type-t?noredirect=1&lq=1
//		GeneratorLearner g = new GeneratorLearner(corpusPath, grammarPath);
//		HashMap<String, HashMap<TTRRecordType, Double>> conditionalProbTable = g.normaliseCountTable(g.conditionalCountTable);
		
//		try {
//			saveModelToFile(conditionalProbTable);
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} // what?

		// how am I supposed to generate now?
		// for state in generator_state:
		// calculate potential states' probs (depending on possible actions) and execute
		// topBeam actions

	}
}
