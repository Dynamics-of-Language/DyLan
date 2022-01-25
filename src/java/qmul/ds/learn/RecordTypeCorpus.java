package qmul.ds.learn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;
import qmul.ds.formula.ttr.TTRRecordType;

/**
 * 
 * Corpus of sentences mapped to their corresponding complete DS semantic tree
 * 
 * @author arash
 */

public class RecordTypeCorpus extends Corpus<TTRRecordType> implements Serializable {
	private static Logger logger = Logger.getLogger(RecordTypeCorpus.class);

	/**
	 * 
	 */

	public final static String CORPUS_FOLDER = "corpus/CHILDES/eveTrainPairs/";
	public final static String WORD_SEP_PATTERN = "\\s";
	public ArrayList<String> sentenceIndices;
	private static final long serialVersionUID = 4914176393669845762L;

	public RecordTypeCorpus() {
		super();
		sentenceIndices = new ArrayList<String>();
	}

	
	
	public void addIndex(String index){
		this.sentenceIndices.add(index);
	}


	public void loadCorpus(File fileName) throws IOException {
		System.out.println("loading TTR corpus");
	
		BufferedReader reader=new BufferedReader(new FileReader(fileName));
		int i=0;
		String line=reader.readLine();
		do{
			
			if (line.trim().isEmpty())
			{
				line=reader.readLine();
				continue;
			}
			List<String> lines=new ArrayList<String>();
			
			if (line.trim().startsWith("END"))
				break;
				
			
			
			while(line!=null&&!line.trim().isEmpty())
			{
				//System.out.println("reading line:"+line);
				lines.add(line);
				line=reader.readLine();
			}
			List<String> sentList = Arrays.asList(lines.get(0).substring(lines.get(0).indexOf(":")+1, lines.get(0).length()).trim().split(WORD_SEP_PATTERN, -1));
			
			Sentence<Word> sent = Sentence.toSentence(sentList);
			TTRRecordType target=TTRRecordType.parse(lines.get(1).substring(lines.get(1).indexOf(":")+1, lines.get(1).length()).trim());
			add(new Pair<Sentence<Word>, TTRRecordType>(sent, target));
			i++;
			
		}
		while(line!=null);
		
		reader.close();
		System.out.println("loaded TTR corpus with "+i+" entries");

	}
	
	public String getIndexNumber(int i){
		/**
		 * returns the index string from its position in the corpus
		 */
		return this.sentenceIndices.get(i);
	}


	public void loadCorpusNoRecordTypes(File fileName) throws IOException {
		/**
		 * Loads corpus with 'nimm das Teil (d1_113.15)' each line
		 * Should really given the diff? i.e. +s -s etc.
		 * 
		 */
		System.out.println("loading TTR corpus with utterances but no record types (just a list of utterances with an identifier)");
	
		BufferedReader reader=new BufferedReader(new FileReader(fileName));
		int i=0;
		String line=reader.readLine();
		do{
			
			if (line.trim().isEmpty()){
				line=reader.readLine();
				continue;
			}
	
			List<String> sentList = Arrays.asList(line.toLowerCase().replace("ä","ae").replace("ö", "oe").replace("ü", "ue").trim().split(WORD_SEP_PATTERN));
			String ID = sentList.get(sentList.size()-1);
			System.out.println(sentList);
			sentList = sentList.subList(0,sentList.size()-1);

			
			Sentence<Word> sent = Sentence.toSentence(sentList);
			//TTRRecordType target=TTRRecordType.parse(lines.get(1).substring(lines.get(1).indexOf(":")+1, lines.get(1).length()).trim());
			TTRRecordType target = TTRRecordType.parse("[]");
			add(new Pair<Sentence<Word>, TTRRecordType>(sent, target));
			addIndex(ID); //simply adds an index string at the moment
			i++;
			line=reader.readLine();
			
		}
		while(line!=null);
		
		reader.close();
		System.out.println("loaded TTR corpus with "+i+" entries and all empty RTs");

	}



}
