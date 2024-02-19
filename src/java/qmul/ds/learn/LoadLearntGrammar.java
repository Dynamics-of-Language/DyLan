package qmul.ds.learn;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

// Instead of these, I probably can inherit from ContextParser
import qmul.ds.action.Lexicon;
import qmul.ds.action.Grammar;


// I should:
// Load the learnt lexical actions
// load the computational actions file
// load modified CHILDES corpus
// parse it with the grammar 

public class LoadLearntGrammar extends Lexicon { // why extend lexicon?
	
	
	private static Logger logger = Logger.getLogger(LoadLearntGrammar.class);
//	public static final Pattern TEMPLATE_SPEC_PATTERN = Pattern.compile("(.+?)\\((.+)\\)");
//	private static final long serialVersionUID = 1L; // Eclipse asked me to do it :(


	/*
	 * Loads learnt computational actions from binary files -> class version error?
	 */
	private static TestParser testFromBinary(String grammarPath, int topN) {
		try {
			// Loading the learnt lexical actions
			Lexicon learntLex = Lexicon.loadLexicon(grammarPath + "lexicon.lex-top-" + topN);

			// Loading the computational actions (grammar)
			Grammar compActions = new Grammar(grammarPath);

			// Initializing TestParser object
			TestParser tp = new TestParser(learntLex, compActions);
			return tp;

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null; // what am I supposed to do with this :(
	}
	
	/*
	 * Loads learnt computational actions from text files
	 */
	public static TestParser testFromText(String grammarPath, int topN) { // just changed `private` to `public`. not sure what will happen.

			// Loading the learnt lexical actions
//			Lexicon learntLex = Lexicon.loadLexicon(grammarPath + "lexicon.lex-top-" + topN);
			Lexicon lex = new Lexicon(grammarPath);

			// Loading the computational actions (grammar)
			Grammar compActions = new Grammar(grammarPath);

			// Initializing TestParser object
			
			TestParser tp = new TestParser(lex, compActions);
			return tp;

			}

	
	public static void main(String[] args) {
		String grammarPath = "dsttr/resource/2022-learner2013-output/".replaceAll("/",
				Matcher.quoteReplacement(File.separator));
		String corpusPath = "dsttr/corpus/CHILDES/eveTrainPairs/LC-CHILDESconversion396FinalCopy.txt".replaceAll("/",
				Matcher.quoteReplacement(File.separator));
		Integer topN = 1;
		
//		TestParser tp = testFromBinary(grammarPath, topN);
		TestParser tp = testFromText(grammarPath, topN);

//		 Loading CHILDES corpus
		tp.loadTestCorpus(corpusPath, true);
		
		// Testing the loaded parser
		tp.parseCorpusToFile(grammarPath + "RTsIncremental.txt", grammarPath + "Errors.txt", 30, false, false);
	}

}
