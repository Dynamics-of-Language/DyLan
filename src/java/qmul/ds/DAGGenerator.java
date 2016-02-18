package qmul.ds;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.stanford.nlp.util.Pair;
import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.tree.Tree;

public abstract class DAGGenerator<T extends DAGTuple, E extends DAGEdge> {
	

		private static Logger logger = Logger.getLogger(DAGGenerator.class);


		protected DAGParser<T,E> parser;
		
		protected TTRFormula goal;
		
		protected DAG<T,E> state;
		
		protected List<String> generated=new ArrayList<String>();
		
		protected String myName="self";
		
		protected Context<T,E> localContext;
		
		
		public String[] interregna = { "uh", "I mean", "sorry", "rather" };

		
		public DAGGenerator(Lexicon lexicon, Grammar grammar) {
			
			parser = getParser(lexicon, grammar);
			state = getNewState(new Tree());
			
		}

		public abstract DAG<T, E> getNewState(Tree start);
		
		public void setGoal(TTRFormula goal) {
			this.goal=goal;
		}
		
		public DAGGenerator(DAGParser<T,E> parser)
		{
			this.parser=parser;
			state = getNewState(parser.getState().getCurrentTuple().getTree());
		}
		

		/**
		 * @param lexicon
		 * @param grammar
		 * @return a {@link Parser} suitable for this implementation
		 */
		public abstract DAGParser<T,E> getParser(Lexicon lexicon, Grammar grammar);
		
		/**
		 * @param resourceDir
		 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
		 */
		public DAGGenerator(File resourceDir) {
			this(new Lexicon(resourceDir), new Grammar(resourceDir));
		}

		/**
		 * @param resourceDirNameOrURL
		 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
		 */
		public DAGGenerator(String resourceDirNameOrURL) {
			this(new Lexicon(resourceDirNameOrURL), new Grammar(resourceDirNameOrURL));
			
		}

		
		

		

		
		

		/**
		 * @return a shallow copy of the current state
		 */
		public DAG<T, E> getState() {
			
			return state;
		}

		
		
		protected abstract void applyAllOptions();
		

		public boolean generate() {
			if (state.isExhausted()) {
				logger.info("state exhausted");
				return false;
			}

			do {

				if (!adjustOnce()) {
					logger.info("wordstack:" + state.wordStack());
					logger.info("depth:" + state.getDepth());
					state.setExhausted(true);
					return false;
				}
				
			} while (!(state.getCurrentTuple().isComplete()&&goal.subsumes(state.getCurrentTuple().getSemantics())));
			
			return true;
		}
		
		
		private boolean adjustOnce() {

			if (state.outDegree(state.getCurrentTuple()) == 0)
				applyAllOptions();

			E result;
			do {

				result = state.goFirstGen();

				if (result != null) {

					break;
				}
			} while (state.attemptBacktrackGen());

			return (result != null);

		}
		
		
		
		
	

	
	

}
