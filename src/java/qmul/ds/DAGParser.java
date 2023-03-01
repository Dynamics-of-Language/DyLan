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

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.util.Pair;
import qmul.ds.action.Action;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.action.SpeechActInferenceGrammar;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.RevokedWord;
import qmul.ds.dag.UtteredWord;
import qmul.ds.formula.Formula;
import qmul.ds.formula.FormulaMetavariable;
import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.tree.Tree;

/**
 * A generic parser with a Directed Asyclic Graph  as its parse context.getDAG() and context ({@link DAG<T,E>}, see Eshghi et. al. (2013)).
 * 
 * @author arash, mpurver
 */
public abstract class DAGParser<T extends DAGTuple, E extends DAGEdge>
		implements DSParser {

	private static Logger logger = Logger.getLogger(DAGParser.class);
	
	//protected DAG<T, E> state;
	protected Lexicon lexicon;
	/**
	 * The non-optional set of computational actions.
	 */
	protected Grammar nonoptionalGrammar;// as determined by the prefix * in action files
	/**
	 * The context including the state as directed acyclic graph.
	 * See e.g. Eshghi et. al. (2015); Eshghi et. al. (2013)
	 * 
	 */
	protected Context<T, E> context;
	
	/**
	 * The optional set of computational actions
	 */
	protected Grammar optionalGrammar;
	

	/**
	 * The computational actions used for completing a tree: completion, beta-reduction, thinning (anticipation?)
	 */
	protected Grammar completionGrammar;
	
	/**
	 * The Speech/Dialogue Act Inference Grammar. This is optional.
	 */
	protected SpeechActInferenceGrammar sa_grammar;

	protected boolean ready=false;
	
	public DAGParser(Lexicon lexicon, Grammar grammar, SpeechActInferenceGrammar sa)
	{
		this.lexicon=lexicon;
		separateGrammars(grammar);
		this.sa_grammar=sa;
		ready=true;
		
	}
	
	public DAGParser(Lexicon lexicon, Grammar grammar) {
		this(lexicon,grammar,new SpeechActInferenceGrammar());
		
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

	public Context<T,E> getContext()
	{
		return context;
	}
	
	/**
	 * @param resourceDir
	 *            the dir containing computational-actions.txt,
	 *            lexical-actions.txt, lexicon.txt
	 */
	public DAGParser(File resourceDir) {
		this(new Lexicon(resourceDir), new Grammar(resourceDir), new SpeechActInferenceGrammar(resourceDir));
		
	}
	
	
	
	

	/**
	 * @param resourceDirNameOrURL
	 *            the dir containing computational-actions.txt,
	 *            lexical-actions.txt, lexicon.txt
	 */
	public DAGParser(String resourceDirNameOrURL) {
		this(new Lexicon(resourceDirNameOrURL), new Grammar(
				resourceDirNameOrURL), new SpeechActInferenceGrammar(resourceDirNameOrURL));
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
	protected int getIndexOfTRP(List<Action> actions) {
		for (int i = 0; i < actions.size(); i++) {
			if (actions.get(i).getName().equals("trp"))
				return i;
		}
		return -1;
	}
	
	protected Pair<List<E>, Tree> trimUntilApplicable(Tree t,
			List<E> edges, List<Action> acts) {
		
		if (edges.isEmpty())
			return new Pair<List<E>,Tree>(new ArrayList<E>(), t);
		
		List<Action> actions=context.getDAG().getAsActionList(edges);
		int trpIndex=getIndexOfTRP(actions);
		if (trpIndex>=0)
			actions=new ArrayList<Action>(actions.subList(trpIndex+1, actions.size()));
		
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
			
			logger.debug("trying actions: "+edge);
			logger.debug("on:"+init.second);
			completer=this.applyActions(init.second,edge);
			if (completer!=null)
			{
				ArrayList<Action> actions=new ArrayList<Action>(init.first);
				actions.addAll(edge);
				
				logger.debug("completetuple until applicable returns actions:"+actions);
				return new Pair<List<Action>,Tree>(actions,completer);
			}
			logger.debug("no success");
			seen.add(init.second.clone());
			logger.debug("adding to seen:"+init.second);
			init=completeOnce(init.second);
			if (seen.contains(init.second))
			{
				logger.debug("seen:"+init.second);
				logger.debug(seen);
				break;
			}
	
			
		}while(!init.first.isEmpty());
		
		logger.debug("complete tuple until applicable returns false");
		return null;
		
	}
	
	//END OF methods for rerunning actions-------------------------------------------------------
	

	public TTRRecordType getFinalSemantics()
	{
		return (TTRRecordType)context.getCurrentTuple().getSemantics(context).evaluate();
	}
	
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
		Pair<List<Action>,Tree> completed=complete(context.getDAG().getCurrentTuple().getTree());
		
		
		
		E replayEdge = context.getDAG().getNewEdge(completed.first, word);
		
		T res = context.getDAG().getNewTuple(completed.second);
		context.getDAG().addChild(res, replayEdge);
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
			Tree completedOnce=a.exec(init.second.clone(), context);
			
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
		logger.trace("Adjusted with" + result.first);
		logger.trace("Resulting tree:"+result.second);
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
	public synchronized void init() {
		FormulaMetavariable.resetPool();
		context.init();

	}

	/**
	 * Tell the parser we're beginning a new sentence. By default, this just
	 * resets to the initial (axiom) state
	 * 
	 * @see init()
	 */
	public synchronized void newSentence() {
		context.getDAG().init();

	}

	public synchronized void setState(DAG<T, E> state) {
		this.context.setDAG(state);
	}

	/**
	 * @return a shallow copy of the current state
	 */
	public synchronized DAG<T, E> getState() {
		if (context == null) {
			return null;
		}
		return context.getDAG();
	}

	@Override
	public synchronized TreeSet<DAGTuple> getStateWithNBestTuples(int N) {
		getState().resetToFirstTupleAfterLastWord();
		TreeSet<DAGTuple> result = new TreeSet<DAGTuple>();
		result.add(context.getDAG().getCurrentTuple());
		for (int i = 0; i < N; i++) {
			if (parse())
				result.add(context.getDAG().getCurrentTuple());
			else
				break;
		}
		getState().resetToFirstTupleAfterLastWord();
		return result;
	}
	
	
	public synchronized List<TTRRecordType> getNBestFinalSemantics(int n)
	{
		TreeSet<DAGTuple> set=getStateWithNBestTuples(n);
		
		List<TTRRecordType> result=new ArrayList<TTRRecordType>();
		for(DAGTuple t:set)
			result.add((TTRRecordType)t.getSemantics(context).evaluate());
		
		return result;
	}

	@Override
	public synchronized ParserTuple getBestTuple() {

		return context.getDAG().getCurrentTuple();
	}

	@Override
	public Generator<? extends ParserTuple> getGenerator() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();

	}
	
	public abstract DAGGenerator<T, E> getDAGGenerator();
	
	public abstract List<UtteredWord> generateTo(TTRFormula goal);

	/** successive calls to this after adding a word on the stack will step through the different parses of
	 * a (partial) sentence  
	 * 
	 * @param goal
	 * @return true if parse is successful
	 */
	public boolean parse()
	{
		return parse((Formula)null);
	}
	
	/** Used in generation. Like parse() but always checks that any update to the dag subsumes the provided formula.
	 * No new tuples are added unless they subsume the goal. 
	 * 
	 * @param goal
	 * @return true if successfully parsed and subsumed goal. False if unsuccessful; any interim changes to the dag 
	 * will be backtracked / undone. 
	 */
	public abstract boolean parse(Formula goal);

	

	public abstract DAG<T, E> parseWord(UtteredWord word);
	
	/**
	 * Attempt to parse word in addition checking checking subsumption of goal. Returns null if unsuccessful. 
	 */
	public abstract DAG<T, E> generateWord(UtteredWord word, Formula goal);


	

	
	
	public synchronized boolean parseUtterance(Utterance utt)
	{
		ready=false;
		logger.info("Parsing Utterance \""+utt+"\"");
		//add speaker to conversation if not already there
//		if (!context.getParticipants().contains(utt.speaker))
//			context.addParticipant(utt.speaker);
		
		//set addressee of utterance if inferrable (in the dyadic case):
		List<String> participants=new ArrayList<String>(context.getParticipants());
		if (context.getParticipants().size()==2)
		{
			if (participants.indexOf(utt.speaker) == 0)
				utt.setAddressee(participants.get(1));
			else
				utt.setAddressee(participants.get(0));
		}
		boolean success=true;
		for (int i = 0; i < utt.words.size(); i++) {
			UtteredWord word=utt.words.get(i);
			
			DAG<T,E> result = parseWord(
					word);
			
			if (result == null)
			{
				logger.error("Failed to parse "+utt.words.get(i));
				logger.error("Skipping it");
				success=false;
				context.appendWord(new RevokedWord(word));
			}
			else
				context.appendWord(new UtteredWord(word));

		}
		ready=true;
		return success;
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.stanford.nlp.parser.Parser#parse(java.util.List)
	 */
	@Override
	public synchronized boolean parse(List<? extends HasWord> words)
	{
		ready=false;
		for (int i = 0; i < words.size(); i++) {
			DAG<T,E> result = parseWord(new UtteredWord(
					words.get(i).word()));
			if (result == null)
				return false;

		}
		ready=true;

		return true;
	}

	
	
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
		return this.complete(context.getDAG().getCurrentTuple().tree).second;
	}
	
	public boolean isRepairProcessingEnabled()
	{
		return context.getDAG().repairProcessingEnabled();
	}
	
	public void setRepairProcessing(boolean b)
	{
		
		this.context.getDAG().setRepairProcessing(b);
	}
	
	
	public boolean isReady()
	{
		return this.ready;
	}
	

}
