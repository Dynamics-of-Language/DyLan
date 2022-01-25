package qmul.ds;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import qmul.ds.action.Grammar;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.Lexicon;
import qmul.ds.formula.ttr.TTRFormula;
import qmul.ds.formula.ttr.TTRRecordType;

/**
 * @author arash
 * 
 * @param <T>
 */
public abstract class TTRGenerator<T extends ParserTuple> extends Generator<T> {

	public HashMap<String, Collection<LexicalAction>> subLexicon = new HashMap<String, Collection<LexicalAction>>();
	public List<GenerationState<T>> stateHistory;// for repair
	public static Logger logger=Logger.getLogger(TTRGenerator.class);
	public TTRGenerator(Parser<T> parser) {
		super(parser);
		stateHistory = new ArrayList<GenerationState<T>>();

	}

	public TTRGenerator(Lexicon lexicon, Grammar grammar) {
		super(lexicon, grammar);
		stateHistory = new ArrayList<GenerationState<T>>();
	}

	/**
	 * @param resourceDir
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public TTRGenerator(File resourceDir) {
		super(resourceDir);
		this.stateHistory = new ArrayList<GenerationState<T>>();

	}

	/**
	 * @param resourceDirNameOrURL
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public TTRGenerator(String resourceDirNameOrURL) {
		super(resourceDirNameOrURL);
		this.stateHistory = new ArrayList<GenerationState<T>>();

	}

	public boolean generateNextWord()
	{
		if (state!=null) stateHistory.add(state.clone());
		return super.generateNextWord();
	}
	
	
	public boolean generate(TTRRecordType ttr) {
		state.setGoal(ttr);
		logger.info("Starting generation with goal TTR:");
		logger.info(ttr);
		// logger.info("TIME BEFORE sublexicalise: " + System.currentTimeMillis());
		//subLexicon();

		// Vector<ParseState<T>> parses = new Vector<ParseState<T>>();
		int i = 1;
		while (generateNextWord()) {
			
			logger.info("\n\n" + "word number " + i + "--------------------");
			logger.info("Gen next word");
			//logger.info("TIME BEFORE WORD " + i + ": " + System.currentTimeMillis());
			//generateNextWord();
			/*
			if (!successful()) {
				// logger.info("stateHistory = ");
				// for (GenerationState<T> genstate : stateHistory) {
				// logger.info(genstate.toString());

				// }
				String interregWord = interregna[new Random().nextInt(interregna.length)];
				if (genGui!=null)
					genGui.addGeneratorOutput("," + interregWord + ", \n");
				// JOptionPane.showMessageDialog(null, "uhh");
				repair();
			}*/
			//logger.info("TIME AFTER WORD " + i + ": " + System.currentTimeMillis());
			i++;

		}
		logger.info("Finished generation.");
		
		return successful();
	}
	/**
	 * Backtracks by one parse state in the stateHistory at a time until a departure point for a successful parse path
	 * is found (i.e. one in which the top tree's TTR subsumes the current goal TTR concept)
	 * 
	 */
	public void repair() {

		int i = stateHistory.size();
		TTRRecordType goal = (TTRRecordType) state.getGoal().getSemantics();
		logger.info("repairing , attempting to generate with " + goal);
		while (!successful() && i > 0) {
			GenerationState<T> previous = stateHistory.get(i - 1);
			previous.setGoal(goal);
			state = previous;
			state.setGoal(goal);
			// subLexicon(); //will now need to get new sublexicon
			generateNextWord();
			i--;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.Generator#init()
	 */
	public void init() {
		super.init();		
		stateHistory.clear();		
	}
}
