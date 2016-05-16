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
		
		
		
		protected List<String> generated=new ArrayList<String>();
		
		public static String myName="self";
		
		
		
		
		public String[] interregna = { "uh", "I mean", "sorry", "rather" };

		
		public DAGGenerator(Lexicon lexicon, Grammar grammar) {
			
			parser = getParser(lexicon, grammar);
			
			
		}

		public abstract DAG<T, E> getNewState(Tree start);
		
		public void setGoal(TTRFormula goal) {
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
			
			return parser.state;
		}

		
		
		protected abstract void applyAllOptions();
		

		public boolean generate() {
			if (parser.state.isExhausted()) {
				logger.info("state exhausted");
				return false;
			}

			do {

				if (!adjustOnce()) {
					logger.info("wordstack:" + parser.state.wordStack());
					logger.info("depth:" + parser.state.getDepth());
					parser.state.setExhausted(true);
					return false;
				}
				
			} while (!(parser.state.getCurrentTuple().isComplete()&&goal.subsumes(parser.state.getCurrentTuple().getSemantics())));
			
			return true;
		}
		
		
		private boolean adjustOnce() {

			if (parser.state.outDegree(parser.state.getCurrentTuple()) == 0)
				applyAllOptions();

			E result;
			do {

				result = parser.state.goFirst();

				if (result != null) {

					break;
				}
			} while (parser.state.attemptBacktrack());

			return (result != null);

		}
		
		
		
		
	

	
	

}
