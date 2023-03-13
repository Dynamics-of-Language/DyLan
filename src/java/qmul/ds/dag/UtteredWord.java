package qmul.ds.dag;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.Word;
import qmul.ds.Utterance;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.util.Pair;

public class UtteredWord implements HasWord{

	private String word;
	private String speaker;
	private String addressee;
	private static final long serialVersionUID = 5203636230819258201L;

	public UtteredWord(String word, String speaker)
	{
		this(word, speaker,Utterance.defaultAddressee);
	}
	
	public UtteredWord(String word)
	{
		this(word,Utterance.defaultSpeaker);
	}
	
	public UtteredWord(String word, String speaker, String addressee)
	{
		this.word=word;
		this.speaker=speaker;
		this.addressee=addressee;
		
	}
	

	public UtteredWord(UtteredWord w) {
		this.word=new String(w.word);
		this.speaker=new String(w.speaker);
		this.addressee=new String(w.addressee);
	}

	public UtteredWord(Word w){
		this(w.toString());
	}

	@Override
	public void setWord(String arg0) {
		word=arg0;
		
	}

	@Override
	public String word() {
		
		return word;
	}
	
	public String speaker()
	{
		return speaker;
	}
	
	public String toString()
	{
		String result = speaker==null?speaker:speaker+": "+ word;
		result+=addressee==null?"":"@"+addressee;
		return result;
	}
	
	public static List<UtteredWord> getAsUtteredWords(String sentence)
	{
		String[] sent = sentence.trim().split("\\s");
		List<UtteredWord> r=new ArrayList<UtteredWord>();
		for(String w:sent)
		{
			r.add(new UtteredWord(w));
		}
		return r;
	}
	
	public static List<UtteredWord> getAsUtteredWords(List<String> sentence)
	{
		
		List<UtteredWord> r=new ArrayList<UtteredWord>();
		for(String w:sentence)
		{
			r.add(new UtteredWord(w));
		}
		return r;
	}
	
	
	public boolean equals(Object o)
	{
		if (!(o instanceof UtteredWord))
			return false;
		
		UtteredWord other=(UtteredWord)o;
		
		
		
		return this.word().equals(other.word())&&this.speaker().equals(other.speaker())&&this.addressee().equals(other.addressee());
	}

	public void setSpeaker(String speaker) {
		this.speaker=speaker;
		
	}

	public String addressee() {
		return addressee;
	}

	public void setAddressee(String string) {
		this.addressee=string;
		
	}

}
