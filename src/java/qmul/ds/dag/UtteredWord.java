package qmul.ds.dag;

import java.util.ArrayList;
import java.util.List;

import qmul.ds.Utterance;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.util.Pair;

public class UtteredWord extends Pair<String,String> implements HasWord{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5203636230819258201L;

	public UtteredWord(String word, String speaker)
	{
		super(word,speaker);
	}
	
	public UtteredWord(String word)
	{
		this(word,Utterance.defaultSpeaker);
	}
	

	@Override
	public void setWord(String arg0) {
		first=arg0;
		
	}

	@Override
	public String word() {
		
		return first;
	}
	
	public String speaker()
	{
		return second;
	}
	
	public String toString()
	{
		return second==null?first:second+": "+ first;
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
		if (this.first==null&&this.second==null)
		{
			return other.first==null&&other.second==null;
				
		}
		else if (this.first==null&&this.second!=null)
		{
			return other.first==null&&this.second.equals(other.second);
		}if (this.first!=null&&this.second==null)
		{
			return other.second==null&&this.first.equals(other.first);
				
		}
		
		
		return this.word().equals(other.word())&&this.speaker().equals(other.speaker());
	}

	public void setSpeaker(String speaker) {
		setSecond(speaker);
		
	}

}
