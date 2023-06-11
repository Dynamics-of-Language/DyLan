package qmul.ds;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
    
    private static String[] interreg = { "sorry", "sorry err", "sorry uhm", "uh I mean"};
    private static String[] hesits = {"uhh", "errm", "err", "er", "uh", "erm", "uhm", "um"};
    
	public static List<String> interregna = Arrays.asList(interreg);
	public static List<String> hesitations = Arrays.asList(hesits);


    protected DAGParser<T, E> parser;

    protected TTRFormula goal;

    public Utterance getGenerated() {
        return generated;
    }

   

    public String agentName = "Dylan";
    
    //This can be changed to an object of Utterance
    protected Utterance generated = new Utterance(agentName, "");

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
     * @return the {@link DAGParser} associated with this generator
     */
    public DAGParser<T, E> getParser() {
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
     *
     * @param w
     * @param goal
     * @return resulting DAG; null if the word cannot be generated.
     */
    public DAG<T, E> generateWord(String w, TTRFormula goal) {
    	
    	if (!getState().getCurrentTuple().getSemantics().subsumes(goal))
    	{
    		logger.info("Cannot generate '" + w + "': current tuple does not subsume goal");
    		return null;
    	}
    	
        UtteredWord word = new UtteredWord(w.toLowerCase(), agentName);

        DAG<T, E> dag = parser.generateWord(word, goal);
        return dag;
    }


    /**
     * Generate to goal from the current context. Default implementation is to call generateNextWord until goal is reached.
     * <p>
     * Contract with generateNextWord: it will return false if no option subsumes goal, else the generation fails for some
     * other reason (e.g. no words parsable). It is generateNextWord that needs to add the word to {@link generated},
     * as this method does not do that. Risk is getting duplicates.
     *
     * @return true if goal is reached. {@link generated} will contain the list of words generated, even when
     * the goal was not reached, and the generation is partial.
     */
    public boolean generate() {
        logger.debug("Generating to goal: " + goal);
        Context<T, E> context = parser.getContext();
        T curTuple = context.getCurrentTuple();
       
        //if (!curTuple.getSemantics().subsumes(goal))
        //    return false;
        //AE: commented out the above, because subsumption will be checked in generateNextWord()

        //generate until goal is reached.
        while (!(goal.subsumes(curTuple.getSemantics()))) {
            logger.info("Current tuple semantics: " + curTuple.getSemantics());
            logger.info("Goal: " + goal);
            if (!generateNextWord())
            {
            	logger.warn("Generation failed prematurely. Generated: "+this.generated);
                return false;
            }
            curTuple = context.getCurrentTuple();
        }

        return true;
    }


    /**
     * Contract: it is generateNextWord() and NOT generate() that adds words to the list {@link generated}
     *
     * @return boolean if the next word was successfully generate; false otherwise
     */
    public abstract boolean generateNextWord();

    public void init() {
        parser.init();
        generated = new Utterance(agentName, "");
    }

}
