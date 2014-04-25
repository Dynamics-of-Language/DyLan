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

import qmul.ds.formula.TTRRecordType;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;

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
	private static final long serialVersionUID = 4914176393669845762L;

	public RecordTypeCorpus() {
		super();
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


}
