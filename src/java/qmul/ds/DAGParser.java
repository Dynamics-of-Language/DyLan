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
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.dag.UtteredWord;
import qmul.ds.formula.FormulaMetavariable;
import qmul.ds.tree.Tree;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.util.Pair;

/**
 * A generic parser with a Directed Asyclic Graph  as its parse state and context ({@link DAG<T,E>}, see Eshghi et. al. (2013)).
 * 
 * @author arash, mpurver
 */
public abstract class DAGParser<T extends DAGTuple, E extends DAGEdge>
		implements DSParser {

	private static Logger logger = Logger.getLogger(DAGParser.class);
	/**
	 * The parse state, which is a directed acyclic graph. This acts as the DS context.
	 * See e.g. Eshghi et. al. (2015); Eshghi et. al. (2013)
	 * 
	 */
	protected DAG<T, E> state;
	protected Lexicon lexicon;
	/**
	 * The non-optional set of computational actions.
	 */
	protected Grammar nonoptionalGrammar;// as determined by the prefix * in action files
	protected Context<T, E> context;
	
	/**
	 * The optional set of computational actions
	 */
	protected Grammar optionalGrammar;
	/**
	 * The computational actions used for completing a tree: completion, beta-reduction, thinning (anticipation?)
	 */

	protected Grammar completionGrammar;

	
	boolean repair_processing=true;
	
	public DAGParser(Lexicon lexicon, Grammar grammar) {
		this.lexicon = lexicon;
		separateGrammars(grammar);
	}
	/** This method divides the set of computational actions into optional and non-optional ones.
	 * 
	 * @param grammar
	 */
	private void separateGrammars(Grammar grammar) {
		this.nonoptionalGrammar = new Grammar();
		this.optionalGrammar = new Grammar();
		this.completionGrammar = new Grammar();
		for (ComputationalAction a : grammar.values()) {
			if (a.getName().equalsIgnoreCase("completion")
					|| a.getName().equalsIgnoreCase("merge")||a.getName().startsWith("anticipation"))
				this.completionGrammar.put(a.getName(), a);

			if (a.isAlwaysGood()) {
				this.nonoptionalGrammar.put(a.getName(), a);
			} else
				this.optionalGrammar.put(a.getName(), a);
		}
	}

	/**
	 * @param resourceDir
	 *            the dir containing computational-actions.txt,
	 *            lexical-actions.txt, lexicon.txt
	 */
	public DAGParser(File resourceDir) {
		this(new Lexicon(resourceDir), new Grammar(resourceDir));
	}
	
	

	/**
	 * @param resourceDirNameOrURL
	 *            the dir containing computational-actions.txt,
	 *            lexical-actions.txt, lexicon.txt
	 */
	public DAGParser(String resourceDirNameOrURL) {
		this(new Lexicon(resourceDirNameOrURL), new Grammar(
				resourceDirNameOrURL));
	}
	
	public DAGParser(String resourceDirNameOrURL, boolean repairing)
	{
		this(resourceDirNameOrURL);
		this.repair_processing=repairing;
	}
	
	
	/** BEGIN METHODS FOR RERUNNING ACTIONS________________________________________________________________________ 
	 * The following methods are used for re-running actions having backtracked over them to do repair.
	 * 
	 */
	
	protected List<E> extractEdges(List<Action> actions, List<E> edges)
	{
		List<E> result=new ArrayList<E>();
		int edgeI=edges.size()-1;
		logger.trace("Actions:"+actions);
		String debug="Edges:";
		for(E edge:edges)
			debug+=edge+":"+edge.getActions()+"|";
		logger.trace(debug);
		for(int j=actions.size()-1;j>=0;j=j-edges.get(edgeI--).getActions().size())
		{
			
			Action lastActionInActions=actions.get(j);
			Action lastActionInEdge=edges.get(edgeI).getActions().get(edges.get(edgeI).getActions().size()-1);
			logger.trace("Last Action in actions:"+lastActionInActions);
			logger.trace("Last Action in edge number "+edgeI+ ":"+lastActionInEdge);
			if (actions.get(j).equals(edges.get(edgeI).getActions().get(edges.get(edgeI).getActions().size()-1)))
			{
				result.add(0, edges.get(edgeI));
			}
			else
				break;
			
		}
		
		return result;
		
	}
	protected Pair<List<E>, Tree> trimUntilApplicable(Tree t,
			List<E> edges, List<Action> acts) {
		
		if (edges.isEmpty())
			return new Pair<List<E>,Tree>(new ArrayList<E>(), t);
		
		List<Action> actions=state.getAsActionList(edges);
		for (int i = 0; i < actions.size(); i++) {
			Pair<List<Action>, Tree> res = completeTupleUntilApplicable(t, actions.subList(i, actions.size()));
			
			if (res != null)
			{
				logger.debug("complete tuple returned:"+res.first);
				logger.debug("with tree"+res.second);
				
				
				List<E> result=extractEdges(res.first, edges);
				acts.addAll(res.first);
				return new Pair<List<E>, Tree>(result
						, res.second);
				
			}
			logger.debug("complete tuple returned null");

		}
		return null;

	}
	
	/**
	 * 
	 * @param start
	 * @param edges
	 * @return edge, expanded on the left with completion actions that need applying before edge is applicable to start.
	 */
	protected Pair<List<Action>,Tree> completeTupleUntilApplicable(Tree start, List<Action> edge)
	{
		
		logger.debug("Completing tuple:"+start);
		logger.debug("until applicable"+edge);
		Pair<List<Action>, Tree> init=new Pair<List<Action>,Tree>(new ArrayList<Action>(),start.clone());
		
		Tree completer;
		List<Tree> seen=new ArrayList<Tree>();
		do
		{
			if (seen.contains(init.second))
				break;
			logger.trace("trying actions: "+edge);
			logger.trace("on:"+init.second);
			completer=this.applyActions(init.second,edge);
			if (completer!=null)
			{
				ArrayList<Action> actions=new ArrayList<Action>(init.first);
				actions.addAll(edge);
				
				logger.debug("completetuple until applicable returns actions:"+actions);
				return new Pair<List<Action>,Tree>(actions,completer);
			}
			logger.trace("no success");

			init=completeOnce(init.second);
			seen.add(init.second);
		}while(!init.first.isEmpty());
		
		logger.debug("completetuple until applicable returns false");
		return null;
		
	}
	
	//END OF methods for rerunning actions-------------------------------------------------------
	

	//BEGIN methods for completing a tree --------------------------------------------------------
	/** Completes the current tuple in the state (the right most node), and adds an edge to the dag which 
	 * is composed of all the computational actions applied to do the completion, followed by word (e.g. "."): this is 
	 * the word that will be associated with the edge.
	 * 
	 * @param word is a right-edge indicator, "." or "?".
	 * @return
	 */
	public T complete(UtteredWord word)
	{
		Pair<List<Action>,Tree> completed=complete(state.getCurrentTuple().getTree());
		
		
		
		E replayEdge = state.getNewEdge(completed.first, word);
		
		T res = state.getNewTuple(completed.second);
		state.addChild(res, replayEdge);
		return res;
		// replayEdge.setInContext(true);
		// setCurrentTuple(res);

	}
	
	/**
	 * Will complete the current tuple, as much as possible, using the
	 * non-optional grammar and Completion.
	 * 
	 * @return The sequence of computational actions applied paired with the
	 *         resulting tuple.
	 */
	protected Pair<List<Action>, Tree> complete(Tree res) {

		// Pair<ParserTuple, List<Action>> initPair=new Pair<ParserTuple,
		// List<Action>>(new ParserTuple(t.getTree()), new ArrayList<Action>());
		ArrayList<Action> actions = new ArrayList<Action>();
		// Tree res = initPair.first.getTree();
		Pair<List<Action>,Tree> cur =new Pair<List<Action>,Tree>(actions,res.clone());
		Pair<List<Action>,Tree> result= new Pair<List<Action>,Tree>(new ArrayList<Action>(), res);
		List<Tree> seen=new ArrayList<Tree>();
		do {
			result.second=cur.second;
			result.first.addAll(cur.first);
			if (result.second.isComplete())
				return result;
			cur=completeOnce(result.second);
			
		} while (!cur.first.isEmpty());
	
		
		logger.trace("result:" + result.first);
		return result;
	}
	
	/**
	 *  Helper method. Used by the {@link complete(Tree)} method.
	 *  This implements on step of tree completion: a sequence of non-optional computational actions, followed possibly, by 
	 *  a single optional one (e.g. completion).
	 *  
	 * @param t
	 * @return the partially completed tree
	 */

	protected Pair<List<Action>,Tree> completeOnce(Tree t)
	{
		
		
		Pair<List<Action>,Tree> init=this.adjustWithNonOptionalGrammar(new Pair<List<Action>,Tree>(new ArrayList<Action>(),t));
		if (!init.first.isEmpty())
			return init;
		
		for(Action a: completionGrammar.values())
		{
			Tree completedOnce=a.exec(init.second, context);
			
			if (completedOnce!=null)
			{
				init.second=completedOnce;
				init.first.add(a);
				return init;
			}
				
		}
		
		return init;
	}

	//END OF methods for completing a tree ------------------------------------------
	

	/**
	 * helper method. applies all actions in edge to t.
	 * 
	 * @param t
	 * @param actions
	 * @return the resulting tuple
	 */
	protected Tree applyActions(Tree t, List<Action> actions) {
		Tree clone = new Tree(t);

		for (Action a : actions) {
			clone = a.exec(clone, context);
			if (clone == null)
				return null;

		}
		return clone;
	}

	
	/**
	 * 
	 * @param initPair
	 * @return
	 */
	protected Pair<List<Action>,Tree> adjustWithNonOptionalGrammar(
			Pair<List<Action>,Tree> initPair) {
		logger.debug("adjusting non-optionally: " + initPair.second);
		ArrayList<Action> actions = new ArrayList<Action>(initPair.first);

		Tree res = initPair.second;
		Tree clone = null;
		DO: do {

			for (ComputationalAction a : nonoptionalGrammar.values()) {
				
				clone = res.clone();
				logger.debug("applying "+a.getName()+ " to "+clone);
				clone = a.exec(clone, context);
				
				logger.debug("result was "+clone);
				if (clone != null) {
					actions.add(a.instantiate());
					res = clone;
					continue DO;

				}
			}

		} while (clone != null);
		Pair<List<Action>,Tree> result = new Pair<List<Action>,Tree>(
				actions,res);
		logger.debug("Adjusted with" + result.first);
		logger.debug("Resulting tree:"+result.second);
		return result;
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
	 * @return a {@link Generator} initialised from this parser with its current
	 *         state
	 */
	/*
	 * public abstract DAGGenerator<T> getGenerator();
	 */
	/**
	 * Reset the parse state to the initial (axiom) state
	 */
	public void init() {
		FormulaMetavariable.resetPool();
		state.init();

	}

	/**
	 * Tell the parser we're beginning a new sentence. By default, this just
	 * resets to the initial (axiom) state
	 * 
	 * @see init()
	 */
	public void newSentence() {
		state.init();

	}

	public void setState(DAG<T, E> state) {
		this.state = state;
	}

	/**
	 * @return a shallow copy of the current state
	 */
	public DAG<T, E> getState() {
		if (state == null) {
			return null;
		}
		return state;
	}

	@Override
	public TreeSet<DAGTuple> getStateWithNBestTuples(int N) {

		TreeSet<DAGTuple> result = new TreeSet<DAGTuple>();
		result.add(state.getCurrentTuple());
		for (int i = 0; i < N; i++) {
			if (parse())
				result.add(state.getCurrentTuple());
			else
				break;
		}
		return result;
	}

	@Override
	public ParserTuple getBestTuple() {

		return state.getCurrentTuple();
	}

	@Override
	public Generator<? extends ParserTuple> getGenerator() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();

	}

	public abstract boolean parse();

	// public abstract DAG<T, E> parseWord(String word, String speaker);

	public abstract DAG<T, E> parseWord(UtteredWord word);

	/**
	 * @param words
	 * @return the resulting (possibly empty) state, or null if the state became
	 *         empty before seeing the last word
	 */
	public DAG<T, E> parseWords(List<UtteredWord> words) {
		for (UtteredWord word : words) {
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
	 * @see edu.stanford.nlp.parser.Parser#parse(java.util.List,
	 * java.lang.String)
	 */
	@Override
	public boolean parse(List<? extends HasWord> words, String goal) {
		throw new UnsupportedOperationException();
	}

	public abstract boolean replayBacktrackedActions(UtteredWord w);

	public Tree complete() {
		return this.complete(state.getCurrentTuple().tree).second;
	}
	
	public abstract void initiateRepair();
	
	public boolean isRepairProcessingEnabled()
	{
		return this.repair_processing;
	}
	
	

}
