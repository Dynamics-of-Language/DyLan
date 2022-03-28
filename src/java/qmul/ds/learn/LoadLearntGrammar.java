package qmul.ds.learn;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;

// Instead of these, I probably can inherit from ContextParser
import qmul.ds.action.Lexicon;
import qmul.ds.learn.TestParser; // what? I'm using it!
import qmul.ds.action.Grammar;
//import qmul.ds.learn.WordHypothesisBase;

// I should:
// Load the learnt lexical actions
// load the computational actions file
// load childes corpus
// parse it with the grammar 

public class LoadLearntGrammar {

	public static void main(String[] args) {

		String grammarPath = "resource/2013-ttr-learner-output/".replaceAll("/",
				Matcher.quoteReplacement(File.separator));
		String corpusPath = "corpus/CHILDES/eveTrainPairs/CHILDESconversion.txt".replaceAll("/",
				Matcher.quoteReplacement(File.separator));
		Integer topN = 2;
		TestParser tp = null;

		try {
			// Loading the learnt lexical actions
			Lexicon learntLex = Lexicon.loadLexicon(grammarPath + "lexicon.lex-top-" + topN);

			// Loading the computational actions (grammar)
			Grammar compActions = new Grammar(grammarPath);

			// Initializing TestParser object
			tp = new TestParser(learntLex, compActions);

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Loading CHILDES corpus
		tp.loadTestCorpus(corpusPath,
				true);

		// Testing the loaded parser
		tp.parseCorpusToFile(grammarPath + "RTsIncremental.txt", grammarPath + "Errors.txt", 30, true, true);
	}

}
