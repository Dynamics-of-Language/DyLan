package qmul.ds;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.stanford.nlp.util.Pair;
import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.dag.UtteredWord;
import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.tree.Tree;

public abstract class DAGGenerator<T extends DAGTuple, E extends DAGEdge> {

	private static Logger logger = Logger.getLogger(DAGGenerator.class);

	protected DAGParser<T, E> parser;

	protected TTRFormula goal;

	protected List<String> generated = new ArrayList<String>();

	public static String agentName = "Dylan";

	public String[] interregna = { "uh", "I mean", "sorry", "rather" };

	public DAGGenerator(Lexicon lexicon, Grammar grammar) {

		parser = getParser(lexicon, grammar);

	}

	public abstract DAG<T, E> getNewState(Tree start);

	public void setGoal(TTRFormula goal) {
		this.goal = goal;
	}

	public DAGGenerator(DAGParser<T, E> parser) {
		this.parser = parser;
	}

	/**
	 * @param lexicon
	 * @param grammar
	 * @return a {@link DAGParser} suitable for this implementation
	 */
	public abstract DAGParser<T, E> getParser(Lexicon lexicon, Grammar grammar);
	
	/**
	 * 
	 * @return the {@link DAGParser} associated with this generator
	 */
	public DAGParser<T,E> getParser()
	{
		return parser;
	}
	
	/**
	 * @param resourceDir the dir containing computational-actions.txt,
	 *                    lexical-actions.txt, lexicon.txt
	 */
	public DAGGenerator(File resourceDir) {
		this(new Lexicon(resourceDir), new Grammar(resourceDir));
	}

	/**
	 * @param resourceDirNameOrURL the dir containing computational-actions.txt,
	 *                             lexical-actions.txt, lexicon.txt
	 */
	public DAGGenerator(String resourceDirNameOrURL) {
		this(new Lexicon(resourceDirNameOrURL), new Grammar(resourceDirNameOrURL));

	}

	/**
	 * @return a shallow copy of the current state
	 */
	public DAG<T, E> getState() {

		return parser.getState();
	}
	
	/**
	 * will use parser.generateWord to generate word w in the current context. This will fail when:
	 * (a) the word is not parsable; or (b) that no parse path can be found such that the resulting tuple
	 * subsumes goal. 
	 * @param w
	 * @param goal
	 * @return resulting DAG; null if the word cannot be generated.
	 */
	public DAG<T,E> generateWord(String w, TTRFormula goal)
	{
		UtteredWord word = new UtteredWord(w.toLowerCase(), agentName);
		
		DAG<T,E> dag = parser.generateWord(word, goal);
		return dag;
	}

	/**
	 * Generate to goal from the current context. Default implementation is to call generateNextWord until goal is reached.
	 * 
	 * Contract with generateNextWord: it will return false if no option subsumes goal.
	 * 
	 * @return true if goal is reached. {@link generated} will contain the list of words generated, even when
	 * the goal was not reached, and the generation is partial.
	 */
	public boolean generate()
	{
		Context<T,E> context = parser.getContext();
		T curTuple = context.getCurrentTuple();
		if (!curTuple.getSemantics().subsumes(goal))
			return false;
		
		//generate until goal is reached.
		//we know that curTuple subsumes goal. Generate until the reverse is also true.
		while(!(goal.subsumes(curTuple.getSemantics())))
		{
			String nextWord = generateNextWord();
			
			if (nextWord == null)
				return false;
			
			generated.add(nextWord);
		}
		
		return true;
		
	}
	
	/**
	 * 
	 * @return the next word; null if none can be generated
	 */
	public abstract String generateNextWord();
	
	public void init()
	{
		parser.init();
	}

	

}
