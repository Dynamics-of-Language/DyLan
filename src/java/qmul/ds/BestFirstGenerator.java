package qmul.ds;

import java.io.File;
import java.util.List;

import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.dag.UtteredWord;
import qmul.ds.dag.WordLevelContextDAG;
import qmul.ds.learn.WordLogProb;
import qmul.ds.tree.Tree;

/**
 * An abstract best first DAG generator with a beam whose corresponding parser is {@link InteractiveContextParser}
 * This class is agnostic about how the beam is populated (e.g. probabilistically, neurally, etc.)
 *
 * @author arash
 */
public abstract class BestFirstGenerator extends DAGGenerator<DAGTuple, GroundableEdge> {
    protected int beam = 3;

    // ---------------------------------- Constructors ----------------------------------

    /**
     * @param resourceDir the dir containing computational-actions.txt,
     *                    lexical-actions.txt, lexicon.txt
     */
    public BestFirstGenerator(File resourceDir, int beam) {
        this(new Lexicon(resourceDir), new Grammar(resourceDir));
        this.beam = beam;
    }

    /**
     * @param resourceDir the dir containing computational-actions.txt,
     *                    lexical-actions.txt, lexicon.txt
     */
    public BestFirstGenerator(File resourceDir) {
        this(new Lexicon(resourceDir), new Grammar(resourceDir));
    }


    /**
     * @param resourceDir the dir containing computational-actions.txt,
     *                    lexical-actions.txt, lexicon.txt
     */
    public BestFirstGenerator(String resourceDir) {
        this(new Lexicon(resourceDir), new Grammar(resourceDir));
    }

    /**
     * @param lexicon
     * @param grammar
     */
    public BestFirstGenerator(Lexicon lexicon, Grammar grammar) {
        super(lexicon, grammar);
    }


    // ---------------------------------- Methods ----------------------------------
    @Override
    public DAG<DAGTuple, GroundableEdge> getNewState(Tree start) {
        return new WordLevelContextDAG(start);
    }


    @Override
    public DAGParser<DAGTuple, GroundableEdge> getParser(Lexicon lexicon, Grammar grammar) {
        return new InteractiveContextParser(lexicon, grammar);
    }


    /**
     * Subclasses should implement this method
     *
     * @return a list of words of length beam of words / tokens.
     */
    public abstract List<String> populateBeam(); // AA: modified.


    public boolean generateNextWord() { // AA: Doesn't seem to be a good name.
        List<String> beamWords = this.populateBeam();// AA: modified.

        for (String word : beamWords) {
            DAG<DAGTuple, GroundableEdge> result = this.generateWord(word, goal);
            if (result != null) {
                this.generated.addWord(word);
                return true;
            }
        }
        return false;
    }

}
