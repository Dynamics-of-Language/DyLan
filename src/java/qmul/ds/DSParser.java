package qmul.ds;

import java.util.List;
import java.util.TreeSet;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.Parser;
import qmul.ds.dag.UtteredWord;

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
	 * 
	 * returns upon a word that is not parsable.
	 * @param utt
	 * @return the starting sub-utterance successfully parsed.
	 */
	public boolean parseUtterance(Utterance utt);

	/**
	 * Sets repair processing on and off
	 * 
	 */
	public void setRepairProcessing(boolean repairing);
	
	/**
	 * 
	 * @return if the parser is ready, i.e. is not in the middle of performing any updates, e.g. parsing, resettting the state, etc.
	 */
	public boolean isReady();
	
	/**
	 * removes the last n words from the context DAG. Used when a past ASR hypothesis is rolled back.
	 * @param n
	 * @return whether operation was successful
	 */
	public boolean rollBack(int n);
	
	
	public Dialogue getDialogueHistory();
	
	
	public boolean isExhausted();
	
	
	
	

}
