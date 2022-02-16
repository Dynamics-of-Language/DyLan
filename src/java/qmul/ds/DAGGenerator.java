package qmul.ds;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.dag.DAG;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.formula.Formula;
import qmul.ds.formula.ttr.TTRFormula;
import qmul.ds.tree.Tree;

public abstract class DAGGenerator<T extends DAGTuple, E extends DAGEdge> {
	

		private static Logger logger = Logger.getLogger(DAGGenerator.class);


		protected DAGParser<T,E> parser;
		
		protected Formula goal;
		
		
		
		protected List<String> generated=new ArrayList<String>();
		
		public static String myName="self";
		
		
		
		
		public String[] interregna = { "uh", "I mean", "sorry", "rather" };

		
		public DAGGenerator(Lexicon lexicon, Grammar grammar, String sem_form) {
			
			parser = getParser(lexicon, grammar, sem_form);
			
			
		}
		
		public DAGGenerator(Lexicon lexicon, Grammar grammar) {
			
			parser = getParser(lexicon, grammar, Formula.TTR);
			
			
		}
		
		

		public abstract DAG<T, E> getNewState(Tree start);
		
		public void setGoal(Formula goal) {
			this.goal=goal;
		}
		
		public DAGGenerator(DAGParser<T,E> parser)
		{
			this.parser=parser;
		}
		

		/**
		 * @param lexicon
		 * @param grammar
		 * @return a {@link Parser} suitable for this implementation
		 */
		public DAGParser<T,E> getParser(Lexicon lexicon, Grammar grammar)
		{
			return getParser(lexicon, grammar, Formula.TTR);
		}
		
		/**
		 * @param lexicon
		 * @param grammar
		 * @return a {@link Parser} suitable for this implementation
		 */
		public abstract DAGParser<T,E> getParser(Lexicon lexicon, Grammar grammar, String sem_form);
		
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
		
		public DAGGenerator(String resourceDirNameOrURL, String sem_form) {
			this(new Lexicon(resourceDirNameOrURL), new Grammar(resourceDirNameOrURL), sem_form);
			
		}
		
		

		
		

		

		
		

		/**
		 * @return a shallow copy of the current state
		 */
		public DAG<T, E> getState() {
			
			return parser.getState();
		}

		
		
		protected abstract void applyAllOptions();
		

		public boolean generate() {
			if (parser.getState().isExhausted()) {
				logger.info("state exhausted");
				return false;
			}

			do {

				if (!adjustOnce()) {
					logger.info("wordstack:" + parser.getState().wordStack());
					logger.info("depth:" + parser.getState().getDepth());
					parser.getState().setExhausted(true);
					return false;
				}
				
			} while (!(parser.getState().getCurrentTuple().isComplete()&&goal.subsumes(parser.getState().getCurrentTuple().getSemantics(parser.getContext()))));
			
			return true;
		}
		
		
		private boolean adjustOnce() {

			if (parser.getState().outDegree(parser.getState().getCurrentTuple()) == 0)
				applyAllOptions();

			E result;
			do {

				result = parser.getState().goFirst();

				if (result != null) {

					break;
				}
			} while (parser.getState().attemptBacktrack());

			return (result != null);

		}
		
		
		
		
	

	
	

}
