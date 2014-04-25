package qmul.ds.learn;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import qmul.ds.ContextParser;
import qmul.ds.ContextParserTuple;
import qmul.ds.ParseState;
import qmul.ds.action.Grammar;
import qmul.ds.action.Lexicon;
import qmul.ds.formula.Formula;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.learn.Evaluation.EvaluationResult;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;

public class TestParser extends ContextParser {

	RecordTypeCorpus testCorpus;
	
	public static void pause() {
		System.out.println("Press enter to continue...");
		try {
			System.in.read();
		} catch (Exception e) {
		}

	}

	public TestParser(File resourceDir) {
		super(resourceDir);
		// TODO Auto-generated constructor stub
	}

	
	public static TestParser getParser(String resourceDir) {
		Lexicon lex = null;
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(resourceDir+"lexicon.lex-top-3"));
			//ObjectInputStream in = new ObjectInputStream(new FileInputStream(resourceDir + "lexicon.3.lex"));
			lex = (Lexicon) in.readObject();
			in.close();
			//ContextParser p = new ContextParser(resourceDir);
			//lex = p.getLexicon();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("loaded lexicon with " + lex.size());
		return new TestParser(lex, new Grammar(resourceDir));

	}

	public TestParser(Lexicon l, Grammar g) {
		super(l, g);
	}

	public void loadTestCorpus(RecordTypeCorpus c) {
		this.testCorpus = c;
	}

	public void loadTestCorpus(String corpusFile) {
		try {
			//ObjectInputStream in = new ObjectInputStream(new FileInputStream(corpusFile));
			//this.testCorpus = (Corpus) in.readObject();
			//in.close();
			this.testCorpus = new RecordTypeCorpus();
			this.testCorpus.loadCorpus(new File(corpusFile));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public boolean containsUnknown(Sentence<Word> s) {
		for (Word w : s)
			if (!lexicon.containsKey(w.word()))
				return true;
		return false;
	}

	public void test() {
		int parsed = 0;
		int sameF = 0;
		int total = 0;
		List<TTRRecordType[]> myttrs = new ArrayList<TTRRecordType[]>();
		Evaluation e = new Evaluation();
		
		if (testCorpus == null)
			return;
		Iterator<Pair<Sentence<Word>, TTRRecordType>> corpusIt = testCorpus.iterator();
		corpusLoop : while (corpusIt.hasNext()) {
			
			Pair<Sentence<Word>, TTRRecordType> entry = corpusIt.next();
			//if (this.containsUnknown(entry.first()))
				{
				//System.out.println(entry.first());
			//	pause();pause();
			//	continue;
				}//skipping unknown words?
			total++;
			init();
			//Tree t = entry.second();
			//Formula rootF = t.getMaximalSemantics();
			
			TTRRecordType[] pair = new TTRRecordType[2];
			TTRRecordType targetRT = entry.second();
			pair[1] = targetRT;
			
			System.out.println("Parsing " + entry.first);
			
			float mostNodesMapped = 0;
			try { if (parse(entry.first)) {
				System.out.println("parsed: " + entry.first);
				parsed++;
				ParseState<ContextParserTuple> twoBest = getStateWithNBestTuples(10);
				Iterator<ContextParserTuple> iter = twoBest.iterator();
				//Formula f = iter.next().getTree().get(new NodeAddress("0")).getFormula();
				while (iter.hasNext()) {
					TTRRecordType ttr = (TTRRecordType) iter.next().getTree().getMaximalSemantics();
					if (ttr==null){ttr = TTRRecordType.parse("[]");}
					if (ttr.subsumes(targetRT) && targetRT.subsumes(ttr)) {
						System.out.println("same formula/maximal match");
						pair[0] = ttr;
						myttrs.add(pair);
						sameF++;
						continue corpusLoop;
					} else {
						float mapped = e.totalNodesMapped(ttr, targetRT);
						if (mapped>=mostNodesMapped){
							mostNodesMapped = mapped; 
							pair[0] = ttr;//always gets the highest mapped of the top two parses
						}
					}
				}

			} else {
				pair[0] = TTRRecordType.parse("[]");
			}
			} catch (Exception f) {
				pair[0] = TTRRecordType.parse("[]");
			}
			
			myttrs.add(pair);
		}
		
		
		List<Float> macro = e.precisionRecallMacro(myttrs);
		EvaluationResult micro = e.precisionRecallMicro(myttrs);
		System.out.println(parsed);
		System.out.println(total);
		System.out.println("parsed:" + (float) parsed / (float) total);
		System.out.println("same formula:" + (float) sameF / (float) parsed);
		System.out.println("\nMACRO scores:");
		System.out.println("precision: " + macro.get(0));
		System.out.println("recall: " + macro.get(1));
		System.out.println("f-score " + macro.get(2));
		System.out.println("\nMICRO scores:");
		System.out.println("precision: " + micro.getPrecision());
		System.out.println("recall: " + micro.getRecall());
		System.out.println("f-score: " + micro.getFScore());
	}

	public static void main(String a[]) {
		TestParser tp = getParser("resource/2013-ttr-learner-output/");
		tp.loadTestCorpus("corpus/CHILDES/eveTrainPairs/CHILDESconversion100TestFinal.txt");
		tp.test();
	}

}
