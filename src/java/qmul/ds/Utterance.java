package qmul.ds;

import java.io.CharArrayReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import qmul.ds.dag.UtteredWord;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TreebankLanguagePack;

public class Utterance {

	protected static Logger logger=Logger.getLogger(Utterance.class);
	public static final String SPEAKER_SEP=":";
	public static final String defaultSpeaker = "Dylan";
	public static final String defaultAddressee = "you";
	public static final String RELEASE_TURN_TOKEN= "<rt>";
	private static final String[] delimitersArray = { ".", "?", "!" };
	public static final List<String> SENTENCE_DELIMITERS = Arrays.asList(delimitersArray);
	public static final String WAIT = "<wait>";
	String speaker;
	List<UtteredWord> words;
	
	public Map<Integer,String> uttSegment_map;
	public Map<Integer,String> dAtSegment_map;

	public Utterance(String speaker, String utt) {
		this(speaker+SPEAKER_SEP+utt);
	}

	public Utterance(String text) {
		
	
		String[] split=text.split(SPEAKER_SEP);
		String content=null;
		String spk=null;
		if (split.length>1)
		{
			spk=split[0];
			content=split[1];
		}
		else
		{
			spk=defaultSpeaker;
			content=split[0];
		}
		speaker=spk;
		content=content.toLowerCase();
		
		String utt = content;
		if(content.contains("--")){
			if(this.uttSegment_map == null)
				this.uttSegment_map = new TreeMap<Integer, String>();
			if(this.dAtSegment_map == null)
				this.dAtSegment_map = new TreeMap<Integer, String>();
			
			String[] items = content.split("--");
			utt = items[0].trim();
			String dat = items[1].trim();
			
		//TODO: keep working on this utterance updates to get dats and utterances
		
			utt = utt.replace(". <rt>", " <rt>").replaceAll("\\.\\.\\.", "");
			String[] utt_segments = utt.split("\\.");
			String[] dAts = dat.split("&&");
//			logger.info("??? utt: " + utt);
//			logger.info("??? num of utt_segments: " + utt_segments.length + " :: " + "num of dAts: " + dAts.length+ ".");
		
			if(utt_segments.length == dAts.length){
				for(int i=0; i< utt_segments.length; i++){
					String segment = utt_segments[i];
					if(!segment.contains("<rt>") && !segment.contains(".") && !segment.contains("?"))
						segment += ".";
					logger.debug("AFTER:: segment: "+ segment);
					
					this.uttSegment_map.put(i, utt_segments[i].trim());
					this.dAtSegment_map.put(i, dAts[i].trim());
				}
			}
			else{
				logger.error("cannot find the matched utt segments("+utt_segments.length+") and dAts("+dAts.length+"):");
				logger.error(content);
			}
		}
		
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		Tokenizer<? extends HasWord> toke = tlp.getTokenizerFactory()
				.getTokenizer(new CharArrayReader(utt.toCharArray()));
		List<? extends HasWord> ws = toke.tokenize();
		words=new ArrayList<UtteredWord>();
		for(HasWord w:ws)
		{
			if(w.word().equals("no.")){
				this.words.add(new UtteredWord("no",spk));
				this.words.add(new UtteredWord(".",spk));
			}
			
			else
				this.words.add(new UtteredWord(w.word(),spk));
		}

		//add release-turn token
	}

	

	public Utterance() {
		this.speaker=null;
		this.words=new ArrayList<UtteredWord>();
	}

	public static void main(String a[]) {
		Utterance utt = new Utterance("A: no. it is");
		System.out.println("words: "+utt.words);
		System.out.println(utt);
		
		
	}

	public boolean isEmpty() {
		
		return words.isEmpty();
	}
	
	public String toString()
	{
		return this.speaker+": "+this.getText();
	}
	/**
	 * puts space between each word and returns the resulting string.
	 * @return
	 */
	public String getAsString()
	{
		String result=this.speaker+": ";
		for(HasWord w:this.words)
			result+=w.word()+" ";
		
		return result.substring(0, result.length()-1);
	}
	
	public String toDebugString()
	{
		String result=this.speaker+": ";
		for(HasWord w:this.words)
			result+=w+" ";
		
		return result.substring(0,result.length()-1);
		
	}
	
	public String getSpeaker()
	{
		return this.speaker;
	}
	
	public String getText()
	{
		if (words.isEmpty())
			return "";
		
		if (this.words.size()<2)
			return this.words.get(0).word();
		
		String result="";
		for(UtteredWord w:this.words)
		{
			if (SENTENCE_DELIMITERS.contains(w.word())&&!result.isEmpty())
			{
				result=result.substring(0,result.length()-1)+w.word()+" ";
				
			}
			else
				result+=w.word()+" ";
		}
		
		return result.substring(0,result.length()-1);
		
	}

	public void setSpeaker(String speaker) {
		this.speaker=speaker;
		for(UtteredWord w: this.words)
			w.setSpeaker(speaker);
		
	}

	/**
	 * Set addressee of words from begin to end (exclusive), to string 
	 * @param string
	 * @param begin
	 * @param end
	 */
	public void setAddressee(String string, int begin, int end) {
		for(int i=begin;i<end;i++)
			this.words.get(i).setAddressee(string);
		
	}
	/**
	 * Set addressee of all words in this utterance to Addressee
	 * @param addressee
	 */
	public void setAddressee(String addressee)
	{
		setAddressee(addressee, 0, words.size());
		
		
	}
	
	public List<UtteredWord> getWords()
	{
		return this.words;
	}
	
	
	
	public UtteredWord lastWord()
	{
		return this.words.get(this.words.size()-1);
	}
	
	public void append(UtteredWord w)
	{
		if (w==null|| w.speaker()==null||!speaker.equals(w.speaker()))
			throw new IllegalArgumentException("Trying to append a word by a different speaker than the speaker of this utterance, or speaker or utterance null");
		
		this.words.add(w);
	}



	public UtteredWord pollWordAndPop() {
		if (this.isEmpty())
			return null;
		
		UtteredWord word=this.words.get(0);
		this.words.remove(0);
		return word;
	}
	
	public boolean equals(Object o)
	{
		if (o==this)
			return true;
		
		if (!(o instanceof Utterance))
			return false;
		
		Utterance other=(Utterance)o;
		
		return this.speaker.equals(other.speaker)&&this.words.equals(other.words);
	}
	
	public int getLength()
	{
		return this.words.size();
	}
	
	public int getTotalNumberOfSegments() {
		return this.uttSegment_map.size();
	}

	public String getUttSegment(int i) {
		if(this.uttSegment_map != null){
//			logger.info("UttSegment["+i+"]: " + this.uttSegment_map.get(i));
			return this.uttSegment_map.get(i);
		}
		return null;
	}

	public String getDAt(int i) {
		if(this.dAtSegment_map != null)
			return this.dAtSegment_map.get(i);
		return null;
	}
	
	

}
