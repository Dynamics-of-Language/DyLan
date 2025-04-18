package qmul.ds.learn;

import java.io.*;
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

	public final static String CORPUS_FOLDER = "corpus/CHILDES/eveTrainPairs/";
	public final static String WORD_SEP_PATTERN = "\\s";
	public ArrayList<String> sentenceIndices;
	private static final long serialVersionUID = 4914176393669845762L;
	public String corpusName;  // Added by Arash A.

	public RecordTypeCorpus() {
		super();
		sentenceIndices = new ArrayList<String>();
	}

	/**
	 * Added by Arash A. for BabyDS
	 * @param corpusName the name of the corpus (for reading and writing purposes, I hope)
	 */
	public RecordTypeCorpus(String corpusName) {
		super();
		this.corpusName = corpusName;
		sentenceIndices = new ArrayList<String>();
	}
	
	
	public void addIndex(String index){
		this.sentenceIndices.add(index);
	}


	public void loadCorpus(File fileName) throws IOException {
		logger.info("Loading TTR corpus \"" + fileName + "\"...");
		try {
		this.corpusName = fileName.getName().substring(0, fileName.getName().lastIndexOf('.'));  // Added by Arash A.
		} catch (Exception e) {
			logger.error("Couldn't extract corpus name from the file name! forcing name `default`");
			this.corpusName = "default";
		}
		BufferedReader reader=new BufferedReader(new FileReader(fileName));
		int i=0;
		String line=reader.readLine();
		do{
			if (line.trim().isEmpty()) {
				line=reader.readLine();
				continue;
			}
			if(line.startsWith("//")) { // AA: Arash Eshghi suggested to add this to skip comments at the beginning of some files.
				line = reader.readLine();
				continue;
			}
			List<String> lines=new ArrayList<String>();
			
			if (line.trim().startsWith("END"))
				break;
			while(line!=null&&!line.trim().isEmpty()) {
				//System.out.println("reading line:"+line);
				lines.add(line);
				line=reader.readLine();
			}
			List<String> sentList = Arrays.asList(lines.get(0).substring(lines.get(0).indexOf(":")+1, lines.get(0).length()).trim().split(WORD_SEP_PATTERN, -1));
			
			Sentence<Word> sent = Sentence.toSentence(sentList);
			TTRRecordType target=TTRRecordType.parse(lines.get(1).substring(lines.get(1).indexOf(":")+1, lines.get(1).length()).trim());
			add(new Pair<Sentence<Word>, TTRRecordType>(sent, target));
			i++;
		} while(line!=null);
		
		reader.close();
		logger.info("Successfully loaded TTR corpus with " + i + " entries.");
	}

	/**
	 * Saves the corpus to a file.
	 * @param fileDir The directory to save the corpus to.
	 * @author Arash A.
	 */
	public void saveCorpus(String fileDir) throws IOException {
		logger.debug("Saving TTR corpus to \"" + fileDir + "\"...");
		CorpusStats corpusStats = new CorpusStats();
		// for all sentences in the corpus, add them to the stats.
		// Then write the corpus to file, and write the stats at the end. copied from generation work.
		PrintStream out = null;
        FileOutputStream fileOpen;
        try {
            fileOpen = new FileOutputStream(fileDir);
            out = new PrintStream(fileOpen);
            for (Pair<Sentence<Word>, TTRRecordType> pair : this) {
				Sentence<Word> sent = pair.first();
				TTRRecordType target = pair.second();
				out.print("GoldSent : " + sent + "\nSem : " + target + "\n\n");  // TODO add File-Level maybe?
				corpusStats.addSentence(sent);
				}
            out.print(corpusStats.statReporter()); // If the corpus is empty, this will throw an error. Have to handle it properly.
			logger.info("Successfully saved TTR corpus (size: " + this.size() + ") to \"" + fileDir + "\".");
        } catch (Exception e) {
            logger.error("Couldn't write to \"" + fileDir + "\"!");
        } finally {
            if (out != null)
                out.close();
        }
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


	/**
	 * Merges two corpora into one.
	 * @param other The other corpus to merge with this one.
	 * @return The merged corpus.
	 */
	public RecordTypeCorpus mergeCorpora(RecordTypeCorpus other){
		RecordTypeCorpus merged = new RecordTypeCorpus();
		merged.addAll(this);
		merged.addAll(other);
		return merged;
	}


	/**
	 * Merges a list of corpora into one.
	 * @param corpora The list of corpora to merge.
	 * @return The merged corpus.
	 */
	public static RecordTypeCorpus mergeAllCorpora(List<RecordTypeCorpus> corpora) {
    RecordTypeCorpus merged = new RecordTypeCorpus();
    for (RecordTypeCorpus corpus : corpora)
        merged = merged.mergeCorpora(corpus);
	return merged;
	}


	/**
	 * Returns a subset of the corpus, from a start index to an end index.
	 * @param start The start index.
	 * @param end The end index.
	 * @return The subset of the corpus specified by the start and end indices.
	 */
	public RecordTypeCorpus getSubCorpus(int start, int end){
		RecordTypeCorpus subset = new RecordTypeCorpus();
		subset.addAll(this.subList(start, end));
		return subset;
	}



}
