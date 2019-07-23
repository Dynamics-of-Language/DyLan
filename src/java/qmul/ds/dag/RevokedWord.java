package qmul.ds.dag;

public class RevokedWord extends UtteredWord {

	
	public RevokedWord(String word, String speaker)
	{
		super(word,speaker);
	}
	
	public RevokedWord(UtteredWord w)
	{
		super(w);
	}
	
	public RevokedWord(String word) {
		super(word);
		
	}
	
	
	public String toString()
	{
		return "-"+super.toString()+"-";
	}

}
