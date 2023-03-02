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
	
	public DAG<T,E> generateWord(String w, TTRFormula goal)
	{
		UtteredWord word = new UtteredWord(w.toLowerCase(), agentName);
		
		DAG<T,E> dag = parser.generateWord(word, goal);
		return dag;
	}

	public abstract boolean generate();
	
	
	public void init()
	{
		parser.init();
	}

//	public boolean generate() {
//		if (parser.getState().isExhausted()) {
//			logger.info("state exhausted");
//			return false;
//		}
//
//		do {
//
//			if (!adjustOnce()) {
//				logger.info("wordstack:" + parser.getState().wordStack());
//				logger.info("depth:" + parser.getState().getDepth());
//				parser.getState().setExhausted(true);
//				return false;
//			}
//
//		} while (!(parser.getState().getCurrentTuple().isComplete()
//				&& goal.subsumes(parser.getState().getCurrentTuple().getSemantics(parser.getContext()))));
//
//		return true;
//	}

//
//	private boolean adjustOnce() {
//
//		if (parser.getState().outDegree(parser.getState().getCurrentTuple()) == 0)
//			applyAllOptions();
//
//		E result;
//		do {
//
//			result = parser.getState().goFirst();
//
//			if (result != null) {
//
//				break;
//			}
//		} while (parser.getState().attemptBacktrack());
//
//		return (result != null);
//
//	}

}
