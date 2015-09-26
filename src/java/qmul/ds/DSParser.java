package qmul.ds;

import java.util.TreeSet;

import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.Parser;

/**
 * Interface containing all methods that should be available for a Dynamic
 * Syntax parser in addition to those of {@link edu.stanford.nlp.parser.Parser}.
 * This is mainly to unify the operation of parsers whose parse states are
 * set-based (e.g. {@link Parser}s) and those that are DAG-based (e.g.
 * {@link InteractiveContextParser}).
 * 
 * @author Arash
 *
 */
public interface DSParser extends edu.stanford.nlp.parser.Parser {
	/**
	 * Reset the parse state to the initial (axiom) state
	 */
	public void init();

	/**
	 * Tell the parser we're beginning a new sentence. By default, this just
	 * resets to the initial (axiom) state
	 * 
	 * @see init()
	 */
	public void newSentence();

	/**
	 * get N best parses
	 * 
	 * @param N
	 * @return an ordered set of parser tuples
	 */
	public TreeSet<? extends ParserTuple> getStateWithNBestTuples(int N);

	/**
	 * @return the "best" tuple in the current state (where "best" is defined by
	 *         the natural ordering of the {@link ParserTuple} implementation
	 *         used), or null if the state is empty
	 */
	public ParserTuple getBestTuple();

	public Generator<? extends ParserTuple> getGenerator();

	/**
	 * An utterance is a sequence of words ({@link HasWord}) spoken by an
	 * individual. This method parses an utterance. The difference from the
	 * parse method is that the words are spoken/owned by SOMEONE in dialogue.
	 * Later this should include addressee info, but in dyadic dialogue this
	 * isn't required.
	 * 
	 * @param utt
	 * @return
	 */
	public boolean parseUtterance(Utterance utt);
	
	
	

}
