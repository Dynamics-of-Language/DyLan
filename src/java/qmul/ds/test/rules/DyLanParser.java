package qmul.ds.test.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import qmul.ds.Context;
import qmul.ds.ContextParser;
import qmul.ds.DSParser;
import qmul.ds.InteractiveContextParser;
import qmul.ds.ParserTuple;
import qmul.ds.Utterance;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.formula.Formula;
import qmul.ds.formula.TTRField;
import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRLabel;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.tree.Tree;

/**
 * Description: this class is a mini class of the DS-TTR parser,
 * which will be able to load the particular lexicon and parse 
 * the utterances word by word like the GUI
 * 
 * this class is applied to assist the class of "AnalyseDialogue" 
 * to report the grammar coverage 
 * 
 * @author Yanchao Yu
 */
public class DyLanParser {
	Logger logger = Logger.getLogger(DyLanParser.class);
	
	public static final String ENGLISHTTRURL = "resource/2017-english-ttr-copula-simple";
	public static final int ENGLISH = 0;
	private String encoding = "UTF-8";
	private DSParser parser;
	private String defaultParser = "";
	private static TreebankLanguagePack tlp;
	public static final String[] PARSER_TYPES = {LoadParserThread.breadthFirst,LoadParserThread.interactive};
	
	private LoadParserThread lpThread;

	private ArrayList<ParserTuple> tuples = new ArrayList<ParserTuple>();
	
	public DyLanParser(String parserFilename, String dataFilename) {
		this.setLanguage(ENGLISH);
		this.loadParser(parserFilename);
		this.loadParser(dataFilename);
	}
	
	public void loadParser(String filename) {
		if (filename == null)
			return;

		// set default for next time
		defaultParser = filename;

		// check if file exists before we start the worker thread and progress
		String pType = PARSER_TYPES[1].toString();
		lpThread = new LoadParserThread(filename, pType);
		lpThread.start();
	}
	
	public void setLanguage(int language) {
		switch (language) {
			case ENGLISH:
				tlp = new PennTreebankLanguagePack();
				encoding = tlp.getEncoding();
				break;
		}
	}


	Utterance prevUtterance=null;
	/**
	 * Re-initialises the current parser
	 */
	public void initParser() {
		if (parser != null) {
			tuples.clear();
			parser.init();
			prevUtterance=null;
			
			if (parser instanceof InteractiveContextParser) {
				InteractiveContextParser p = (InteractiveContextParser) parser;
				tuples.add(p.getState().getCurrentTuple());
			}
		}
	}
	
	public ParseForm parse(String text) {
		 logger.debug("got text " + text);

		if (parser != null && text.length() > 0) {
			// Tokenizer<? extends HasWord> toke = tlp.getTokenizerFactory()
			// .getTokenizer(new CharArrayReader(text.toCharArray()));
			// List<? extends HasWord> wordList = toke.tokenize();

			Utterance utt = new Utterance(text);

			if (prevUtterance != null
					&& utt.getSpeaker().equals(Utterance.defaultSpeaker)) {
				utt.setSpeaker(prevUtterance.getSpeaker());
			}

			// parse the utterance
			boolean successful = false;
			try {
				if (utt == null || utt.isEmpty()) {
					successful = true;
				} else
					successful = parser.parseUtterance(utt);
			} catch (Exception e) {
//				logger.debug("^^ exception: " + e);
				return new ParseForm(utt, e);
			}

			if (successful) {
				if (parser instanceof InteractiveContextParser) {
					InteractiveContextParser p = (InteractiveContextParser) parser;
					tuples.clear();

					tuples.add(p.getState().getCurrentTuple());
					
					Context<DAGTuple, GroundableEdge> context = p.getContext();
					
					return this.displayBestParse(utt, context);
				}
			} else {
				return new ParseForm(utt, new Exception("Cannot parse the utterance at "));
			}
		}
		return new ParseForm(null, new Exception("Parser is unavaliable."));
	}

	private int tupleNumber = 0;
	private ParseForm displayBestParse(Utterance utt, Context<DAGTuple, GroundableEdge> context) {
		if (parser instanceof qmul.ds.Parser)
			tuples = new ArrayList<ParserTuple>(
					((qmul.ds.Parser) parser).getState());
		
		// tuples.addAll(ttrtuples);
		tupleNumber = tuples.size() - 1;
		ParserTuple tuple = tuples.get(tupleNumber);
		tuple.getTree();
		Tree tree = (tuple == null ? null : tuple.getTree());
		TTRFormula sem = (tuple == null ? null : tuple.getSemantics());
		return new ParseForm(utt, sem, tree, context);
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		edu.stanford.nlp.util.DisabledPreferencesFactory.install();
		String dataFilename = null;
		String parserFilename = DyLanParser.ENGLISHTTRURL;
		DyLanParser parser = new DyLanParser(parserFilename, dataFilename);
	}
	
	/**
	 * Worker thread for loading the parser. Loading a parser usually takes ~2
	 * min
	 */
	private class LoadParserThread extends Thread {
		String filename;
		String parserType = interactive;// either 'depth-first' or
		
		// 'breadth-first'
		static final String breadthFirst = "Breadth First";
		static final String interactive = "Interactive (Best-First)";

		LoadParserThread(String filename, String parserType) {
			this.filename = filename;
			this.parserType = parserType;
			
		}

		@Override
		public void run() {
			try {

				if (this.parserType.equalsIgnoreCase(breadthFirst)) 
					parser = new ContextParser(filename);
				else if (this.parserType.equalsIgnoreCase(interactive))
					parser = new InteractiveContextParser(filename);
				else
					parser = null;

				logger.debug("loaded parser");
				// parser = new SimpleParser(filename);
				initParser();
				logger.debug("Initialised Parser");
			} catch (Exception ex) {
				logger.error(ex.getMessage());
				parser = null;

			} catch (OutOfMemoryError e) {
				logger.error(e.getMessage());
				parser = null;
			}
		}
	}
	
	public Context<DAGTuple, GroundableEdge>  getContext(){
		if (parser != null && parser instanceof InteractiveContextParser) {
			InteractiveContextParser p = (InteractiveContextParser) parser;
			tuples.clear();

			tuples.add(p.getState().getCurrentTuple());
			
			return p.getContext();
		}
		return null;
	}
	
	public Tree getTree(){
		if (parser != null && parser instanceof InteractiveContextParser) {
			InteractiveContextParser p = (InteractiveContextParser) parser;
			
			return p.getBestTuple().getTree();
		}
		return null;
	}

	public class ParseForm {
		private Utterance utt;
		private TTRFormula ttr = null;
		private Map<OntologyType, String> groundMap = new HashMap<OntologyType, String>();
		private boolean isGrounded = false;
		private boolean hasException = false;
		private Tree tree;
		private Exception exception;
		private Context<DAGTuple, GroundableEdge> context;
	
	
		public ParseForm(Utterance utt, TTRFormula ttr, qmul.ds.tree.Tree tree, boolean isGrounded){
			this.utt = utt;
			this.ttr = ttr;
			this.isGrounded = isGrounded;
			
			if(ttr != null && ttr instanceof TTRRecordType){
				TTRRecordType context = (TTRRecordType)ttr.clone();
				
				List<TTRField> fieldList = context.getFields();
				for(TTRField field : fieldList){
					Formula type = field.getType();
					if(type != null){
						String typeStr = type.toString();
						String requestTTRlabel = null;
						OntologyType focusOntology = null;
						
						if(typeStr.contains("color")){
							focusOntology = OntologyType.COLOR;
							requestTTRlabel = typeStr.replace("color"+ "(", "");
							requestTTRlabel = requestTTRlabel.replace(")", "");
						}
						else if(typeStr.contains("shape") ){
							focusOntology = OntologyType.SHAPE;
							requestTTRlabel = typeStr.replace("shape"+ "(", "");
							requestTTRlabel = requestTTRlabel.replace(")", "");
						}
						else if(typeStr.contains("class") ){
							focusOntology = OntologyType.SHAPE;
							requestTTRlabel = typeStr.replace("class"+ "(", "");
							requestTTRlabel = requestTTRlabel.replace(")", "");
						}
						
						if(requestTTRlabel != null){
							logger.debug("requestTTRlabel at "+focusOntology+": " + requestTTRlabel);
							
							TTRField f = context.getField(new TTRLabel(requestTTRlabel));
							if(f != null && f.getType() != null){
								groundMap.put(focusOntology, f.getType().toString());
							}
						}
					}
				}
			}
		}
		
		public ParseForm(Utterance utt, TTRFormula ttr, Tree tree, Context<DAGTuple, GroundableEdge> context){
			this.utt = utt;
			this.ttr = ttr;
			this.tree = tree;
			this.context = context;
		}
		
		public ParseForm(Utterance utt, Exception e){
			this.utt = utt;
			this.exception = e;
//			logger.debug("e: " + e);
//			logger.debug("this.exception: " + this.exception);
			this.hasException = true;
		}
		
		public Utterance getUtterance(){
			return this.utt;
		}
		
		public TTRFormula getSemanticTTR(){
			return this.ttr;
		}
		
		public boolean checkGround(){
			return this.isGrounded;
		}
		
		public Tree getContxtalTree(){
			return this.tree;
		}
		
		public String getException(){
			return this.exception.getMessage() + ": " + this.utt;
		}
		
		public boolean hasException(){
			return this.hasException;
		}
	
		public void getUtterance(Utterance utt){
			this.utt = utt;
		}
		
		public void getSemanticTTR(TTRFormula ttr){
			this.ttr = ttr;
		}
		
		public void setGround(boolean isGrounded){
			this.isGrounded = isGrounded;
		}
		
		public void setTree(Tree tree){
			this.tree = tree;
		}
		
		public Map<OntologyType, String> getGroundMap(){
			return this.groundMap;
		}
		
		public Context<DAGTuple, GroundableEdge> getContext(){
			return this.context;
		}
	}
	
	public enum OntologyType{
		COLOR,
		SHAPE;
	}
}
