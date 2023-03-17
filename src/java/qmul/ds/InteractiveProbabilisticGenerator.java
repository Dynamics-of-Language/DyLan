/**
 * 
 */
package qmul.ds;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;
import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.dag.UtteredWord;
import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.learn.RecordTypeCorpus;
import qmul.ds.tree.Tree;

/**
 * @author Arash Ashrafzadeh, Arash Eshghi
 *
 */
public class InteractiveProbabilisticGenerator extends DAGGenerator<DAGTuple, GroundableEdge> {

	final static String grammarPath = "dsttr/resource/2022-learner2013-output/".replaceAll("/", Matcher.quoteReplacement(File.separator));
	static final String corpusFolderPath = "dsttr/corpus/CHILDES/eveTrainPairs/".replaceAll("/", Matcher.quoteReplacement(File.separator));
	static final String corpusPath = corpusFolderPath + "AAtrain-72.txt";

	/**
	 * @param lexicon
	 * @param grammar
	 */
	public InteractiveProbabilisticGenerator(Lexicon lexicon, Grammar grammar) {
		super(lexicon, grammar);
		
	}

	/**
	 * @param parser
	 */
	public InteractiveProbabilisticGenerator(DAGParser<DAGTuple,GroundableEdge> parser) {
		super(parser);
		
	}

	/**
	 * @param resourceDir
	 */
	public InteractiveProbabilisticGenerator(File resourceDir) {
		super(resourceDir);
		
	}

	/**
	 * @param resourceDirNameOrURL
	 */
	public InteractiveProbabilisticGenerator(String resourceDirNameOrURL) {
		super(resourceDirNameOrURL);
		
	}

	@Override
	public DAG<DAGTuple, GroundableEdge> getNewState(Tree start) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DAGParser<DAGTuple, GroundableEdge> getParser(Lexicon lexicon, Grammar grammar) {
		
		return new InteractiveContextParser(lexicon, grammar);
	}


	public HashMap<String, HashMap<TTRRecordType, Double>> loadModelFromFile(String grammarPath) throws IOException, ClassNotFoundException
	{
		HashMap<String, HashMap<TTRRecordType, Double>> table = new HashMap<>();
		int lineNumber = 0;
		FileReader reader = new FileReader(grammarPath+"model.csv");
		BufferedReader stream = new BufferedReader(reader);
		ArrayList<TTRRecordType> features = new ArrayList<TTRRecordType>();
		String line;
		while((line = stream.readLine()) != null){

			String lineList[] = line.split(" , ");
			if (lineNumber == 0) // Features line // todo make this mor efficient since it only happens when reawding line one
			{

				for (int i = 1; i < lineList.length; i++) // Ignoring the first element since it's "WORDS\FEATURES"
//					String feature = lineList[i];
					features.add(TTRRecordType.parse(lineList[i]));

				lineNumber++;
				continue;
			}
			// Otherwise we have a <WORD,PROBS> line.
			String word = lineList[0];
			HashMap<TTRRecordType, Double> row = new HashMap<>();
			ArrayList<Double> probs = new ArrayList<>();
			for(int i=1; i<lineList.length; i++){
				Double prob = Double.parseDouble(lineList[i]);
				probs.add(prob);
			}
			for(int i=0; i<features.size(); i++)
				row.put(features.get(i), probs.get(i));
			table.put(word, row);
			System.out.println(word +  row);
			lineNumber++;
		}
		return table;
	}


	/**
	 * The goal is to:
	 *
	 * @return
	 */
	@Override
	public boolean generate()
	{
		//TODO
		return false;
	}


    public static void main(String[] args) {
		// main method only for testing purposes.

        InteractiveProbabilisticGenerator ipg = new InteractiveProbabilisticGenerator("dsttr/resource/2017-english-ttr");
        //InteractiveContextParser p = new InteractiveContextParser("resource/2017-english-ttr");
        Utterance u = new Utterance("I see a square.");
        if (!ipg.getParser().parseUtterance(u))
            System.out.println("Parse not successful.");

        TTRFormula goal = ipg.getParser().getFinalSemantics();
        ipg.init();
        ipg.generateWord("I", goal);
        //ipg.generateWord("see", goal);
        ipg.generateWord("recognise", goal);
        //this will fail. to test whether we are now able to generate the correct word (that all changes were undone properly)
        ipg.generateWord("see", goal);
        ipg.generateWord("a", goal);
        //again, this should fail. But the next ('square') should succeed.
        ipg.generateWord("circle", goal);
        ipg.generateWord("square", goal);

        //goal = [x5 : e|e5==see : es|x1==Arash : e|pred1==square(x5) : cn|p8==shape(pred1) : t|head==e5 : es|p3==pres(e5) : t|p5==subj(e5, x1) : t|p4==obj(e5, x5) : t]
    }
}
