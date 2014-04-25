package qmul.ds;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;
import qmul.ds.action.Grammar;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.Lexicon;
import qmul.ds.formula.TTRRecordType;

/**
 * A depth first generator that uses the DAG parse state as context and shares this with the corresponding depth first
 * parser. It does away with Generator Tuples (and thereby generation state) used in breadth first generation.
 * 
 * @author arash
 * 
 */

public class DepthFirstGenerator extends Generator<ParserTuple> {
	
	protected static Logger logger=Logger.getLogger(DepthFirstGenerator.class);
	private DepthFirstParser parser;
	

	public DepthFirstGenerator(DepthFirstParser parser) {
		this.parser = parser;
		state=new DAGGenerationState(parser.getState());
	}

	public DepthFirstGenerator(String resourceDirNameOrURL) {
		parser=new DepthFirstParser(resourceDirNameOrURL);
		parser.init();
		this.state=new DAGGenerationState(parser.getState());
	}
	public DepthFirstGenerator(TTRRecordType goal, String resourceDirNameOrURL)
	{
		
		parser=new DepthFirstParser(resourceDirNameOrURL);
		parser.init();
		this.state=new DAGGenerationState(parser.getState(), goal);
		
		
	}
	
	public void init()
	{
		parser.init();
		
	}	
	
	
	public DepthFirstParser getParser(Lexicon lexicon, Grammar grammar) {

		return parser;
	}
	
	
	
	

	public boolean generateNextWord() {

		
		if (state.getGoal()==null)
			logger.error("Goal not set");
		
		if (!getState().subsumed())
			return false;
		
		for (String w : parser.getLexicon().keySet()) {
			logger.debug("Testing word for generation:"+w);
			DAGParseState s = parser.parseWord(w, state.getGoal());
			if (s == null)
			{
				logger.debug("Parsing "+w+" failed");
				continue;
			}
			logger.debug("generating:"+w);
			System.out.print(w+" ");
			getState().generate(w);
			if (gui!=null)
			{
				gui.addGeneratorOutput(w);
			}
			return true;
		}

		return false;
	}

	public void repair() {
		boolean repaired=false;
		while(!getState().subsumed())
		{
			getState().backtrackLastWord();
			repaired=true;
		}
		if (repaired)
		{
			getState().generate(interregna[2]);
			System.out.print(interregna[2]+" ");
		}

	}

	public boolean generate() {
		if (!getState().subsumed())
			repair();
			
		while (!getState().matched()) {
			
			if (!generateNextWord())
			{
				return false;
				
			}
		}
		return true;
	}
	
	public boolean next()
	{
		getState().backtrackLastWord();
		generateNextWord();
		return false;
	}
	
	public DAGGenerationState getState()
	{
		return (DAGGenerationState)state;
	}
	
	public static void main(String a[])
	{
		
		TTRRecordType goal=TTRRecordType.parse("[x==john:e|e1==fly:es|p==pres(e1):t|p2==subj(e1,x):t|head==e1:es|x1==mary:e|p3==with(e1,x1):t]");
		DepthFirstGenerator gen=new DepthFirstGenerator(goal, "resource/2013-english-ttr");
		gen.generate();
		//System.out.println(gen.getState());
		TTRRecordType goal2=TTRRecordType.parse("[x==john:e|e1==fly:es|p==pres(e1):t|p2==subj(e1,x):t|head==e1:es|x1==bill:e|p3==with(e1,x1):t]");
		gen.setGoal(goal2);
		gen.generate();
		//System.out.println(gen.generate());
	
		//System.out.println(gen.getState().subsumed());
		
		/*
		TTRRecordType goal=TTRRecordType.parse("[e1==run:es|head==e1:es|p2==subj(e1, R1.head):t]");
		System.out.println(goal.getTTRPaths());
		
		TTRRecordType goal2=TTRRecordType.parse("[e1==run:es|x==john:e|head==e1:es|p2==subj(e1, x):t]");
		System.out.println(goal.subsumes(goal2));
		*/
	
		
	}

	
	
	
	

}
