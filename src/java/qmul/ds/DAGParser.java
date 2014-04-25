/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.Grammar;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.formula.Formula;
import qmul.ds.formula.FormulaMetavariable;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import edu.stanford.nlp.ling.HasWord;

/**
 * A generic parser
 * 
 * @author mpurver
 */
public abstract class DAGParser<T extends DAGTuple, E extends DAGEdge> implements edu.stanford.nlp.parser.Parser {

	private static Logger logger = Logger.getLogger(DAGParser.class);

	protected DAG<T, E> state;
	protected Lexicon lexicon;
	protected Grammar nonoptionalGrammar;// as determined by the prefix * in action spec files.
	protected Grammar optionalGrammar;

	public DAGParser(Lexicon lexicon, Grammar grammar) {
		this.lexicon = lexicon;
		separateGrammars(grammar);
	}

	private void separateGrammars(Grammar grammar) {
		this.nonoptionalGrammar = new Grammar();
		this.optionalGrammar = new Grammar();
		for (ComputationalAction a : grammar) {
			if (a.isAlwaysGood())
				this.nonoptionalGrammar.add(a);
			else
				this.optionalGrammar.add(a);
		}
	}
	/**
	 * @param resourceDir
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public DAGParser(File resourceDir) {
		this(new Lexicon(resourceDir), new Grammar(resourceDir));
	}

	/**
	 * @param resourceDirNameOrURL
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public DAGParser(String resourceDirNameOrURL) {
		this(new Lexicon(resourceDirNameOrURL), new Grammar(resourceDirNameOrURL));
	}

	/**
	 * @return the lexicon
	 */
	public Lexicon getLexicon() {
		return lexicon;
	}

	/**
	 * @return the optional grammar
	 */
	public Grammar getOptionalGrammar() {
		return optionalGrammar;
	}

	/**
	 * @return the non-optional grammar
	 */
	public Grammar getNonOptionalGrammar() {
		return nonoptionalGrammar;
	}
	
	/**
	 * @return a {@link Generator} initialised from this parser with its current state
	 */
	/*
	public abstract DAGGenerator<T> getGenerator();
	*/
	/**
	 * Reset the parse state to the initial (axiom) state
	 */
	public void init() {
		FormulaMetavariable.resetPool();
		state.init();
		
	}

	/**
	 * Tell the parser we're beginning a new sentence. By default, this just resets to the initial (axiom) state
	 * 
	 * @see init()
	 */
	public void newSentence() {
		addAxiom();

	}

	public void setState(DAG<T,E> state) {
		this.state = state;
	}

	/**
	 * create new DAG, setting its root as the axiom tuple, i.e. tuple with axiom tree.
	 */
	protected abstract void addAxiom();

	

	/**
	 * @return a shallow copy of the current state
	 */
	public DAG<T,E> getState() {
		if (state == null) {
			return null;
		}
		return state;
	}

	/**
	 * Returns a state containing the @param N best tuples of current ParseState
	 * 
	 * @return
	 */
	public abstract DAG<T,E> getStateWithNBestTuples(int N);

	/**
	 * Add a new AXIOM {@link ParserTuple} to the state, by default ensuring that the state is empty. Subclasses may
	 * override this to e.g. move the current state to the context
	 */
	
	

	
	public boolean parse() {
		return false;
	}



	

	


	
	/**
	 * @param word
	 * @return the state which results from extending the current state with all possible lexical
	 *         actions corresponding to the given word; or null if the word is not parsable
	 */
	public abstract DAG<T,E> parseWord(String word);

	
	

	/**
	 * @param words
	 * @return the resulting (possibly empty) state, or null if the state became empty before seeing the last word
	 */
	public DAG<T,E> parseWords(List<String> words) {
		for (String word : words) {
			parseWord(word);
		}
		return getState();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.nlp.parser.Parser#parse(java.util.List)
	 */
	@Override
	public abstract boolean parse(List<? extends HasWord> words);

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.nlp.parser.Parser#parse(java.util.List, java.lang.String)
	 */
	@Override
	public boolean parse(List<? extends HasWord> words, String goal) {
		throw new UnsupportedOperationException();
	}




	

	

	
}
