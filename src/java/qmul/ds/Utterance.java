package qmul.ds;

import java.io.CharArrayReader;
import java.util.ArrayList;
import java.util.List;

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
	String speaker;
	List<UtteredWord> words;

	public Utterance(String speaker, String utt) {
		this(speaker+SPEAKER_SEP+utt);
	}

	public Utterance(String text) {
		
	
		String[] split=text.split(SPEAKER_SEP);
		String utt=null;
		String spk=null;
		if (split.length>1)
		{
			spk=split[0];
			utt=split[1];
		}
		else
		{
			spk=defaultSpeaker;
			utt=split[0];
		}
		speaker=spk;
		utt=utt.toLowerCase();
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		Tokenizer<? extends HasWord> toke = tlp.getTokenizerFactory()
				.getTokenizer(new CharArrayReader(utt.toCharArray()));
		List<? extends HasWord> ws = toke.tokenize();
		words=new ArrayList<UtteredWord>();
		for(HasWord w:ws)
		{
			this.words.add(new UtteredWord(w.word(),spk));
		}

	}

	public static void main(String a[]) {
		Utterance utt = new Utterance("A: john likes mary");
		System.out.println(utt);
		
		
	}

	public boolean isEmpty() {
		
		return words.isEmpty();
	}
	
	public String toString()
	{
		String result=this.speaker+": ";
		for(HasWord w:this.words)
			result+=w.word()+" ";
		
		return result.substring(0,result.length()-1);
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
		String result="";
		for(UtteredWord w:this.words)
			result+=w.word()+" ";
		
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
	
	public List<UtteredWord> getwords()
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

}
