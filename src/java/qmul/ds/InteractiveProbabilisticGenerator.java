/**
 * 
 */
package qmul.ds;

import java.io.File;

import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.dag.UtteredWord;
import qmul.ds.formula.TTRFormula;
import qmul.ds.tree.Tree;

/**
 * @author Arash Ashrafzadeh, Arash Eshghi
 *
 */
public class InteractiveProbabilisticGenerator extends DAGGenerator<DAGTuple, GroundableEdge> {

	/**
	 * @param lexicon
	 * @param grammar
	 */
	public InteractiveProbabilisticGenerator(Lexicon lexicon, Grammar grammar) {
		super(lexicon, grammar);
		
	}

	/**
	 * @param parser
	 */
	public InteractiveProbabilisticGenerator(DAGParser<DAGTuple,GroundableEdge> parser) {
		super(parser);
		
	}

	/**
	 * @param resourceDir
	 */
	public InteractiveProbabilisticGenerator(File resourceDir) {
		super(resourceDir);
		
	}

	/**
	 * @param resourceDirNameOrURL
	 */
	public InteractiveProbabilisticGenerator(String resourceDirNameOrURL) {
		super(resourceDirNameOrURL);
		
	}

	@Override
	public DAG<DAGTuple, GroundableEdge> getNewState(Tree start) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DAGParser<DAGTuple, GroundableEdge> getParser(Lexicon lexicon, Grammar grammar) {
		
		return new InteractiveContextParser(lexicon, grammar);
	}

	@Override
	public boolean generate()
	{
		//TODO
		return false;
	}
	
	
	public static void main(String[] args)
	{
		
		
		InteractiveProbabilisticGenerator ipg = new InteractiveProbabilisticGenerator("resource/2017-english-ttr");
		//InteractiveContextParser p = new InteractiveContextParser("resource/2017-english-ttr");
		Utterance u = new Utterance("I see a square.");
		if (!ipg.getParser().parseUtterance(u))
		{
			System.out.println("Parse not successful.");
		}
		
		TTRFormula goal = ipg.getParser().getFinalSemantics();
		
		ipg.init();
		
		ipg.generateWord("I", goal);
		//ipg.generateWord("see", goal);
		ipg.generateWord("recognise", goal);
		//this will fail. to test whether we are now able to generate the correct word (that all changes were undone properly) 
		ipg.generateWord("see", goal);
		ipg.generateWord("a", goal);
		//again, this should fail. But the next ('square') should succeed.
		ipg.generateWord("circle", goal);
		ipg.generateWord("square", goal);
		
	
		//goal = [x5 : e|e5==see : es|x1==Arash : e|pred1==square(x5) : cn|p8==shape(pred1) : t|head==e5 : es|p3==pres(e5) : t|p5==subj(e5, x1) : t|p4==obj(e5, x5) : t]
		
		
	}
	

}
