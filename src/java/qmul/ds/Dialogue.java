package qmul.ds;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.dag.RevokedWord;
import qmul.ds.dag.UtteredWord;

/**
 * 
 *	A dialogue: a sequence of utterances. Each of the form: "speaker:utterance",
 * e.g. "Arash: What is this?"
 * 
 * 
 * @author Arash
 *
 */
public class Dialogue extends ArrayList<Utterance> {

	private static final long serialVersionUID = -8253079104724886968L;
	protected static final Logger logger=Logger.getLogger(Dialogue.class);
	private List<String> participants = new ArrayList<String>();
	
	
	
	
	public Dialogue(String... participants)
	{
		super();
		this.participants.addAll(Arrays.asList(participants));
		
	}
	
	public Dialogue(List<String> lines) {
		super();
		// first extract particpants and add utterances
		for (String line : lines) {
			logger.debug("Reading line:"+line);
			Utterance cur = new Utterance(line);
			if (!participants.contains(cur.speaker))
				this.participants.add(cur.speaker);
			
			
			add(cur);
		}
		// TODO: add support for addressee marking in a dialogue corpus

		// set addressees if inferrable (i.e. if dialogue is dyadic)
		if (participants.size() != 2)
			return;

		for (Utterance utt : this) {
			if (participants.indexOf(utt.speaker) == 0)
				utt.setAddressee(participants.get(1));
			else
				utt.setAddressee(participants.get(0));
		}
	}

	/**
	 * Reads a bunch of dialogues from text file. Empty line marks the end of a
	 * dialogue. Lines beginning  with // are ignored.
	 * 
	 * @param file
	 * @return
	 */
	public static List<Dialogue> loadDialoguesFromFile(String file)
			throws IOException {
		List<Dialogue> result = new ArrayList<Dialogue>();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		List<String> curDialogue = new ArrayList<String>();
		while ((line = reader.readLine()) != null) {
			if (line.trim().startsWith("//"))
				continue;
			else if (line.trim().isEmpty()) {
				if (!curDialogue.isEmpty())
					result.add(new Dialogue(curDialogue));

				curDialogue.clear();
			} else
				curDialogue.add(line);
		}
		
		if (!curDialogue.isEmpty())
			result.add(new Dialogue(curDialogue));
		
		reader.close();
		logger.debug("Read "+result.size()+" dialogues");
		return result;

	}
	
	public String toString()
	{
		String result="";
		for(Utterance utt: this)
		{
			result+=utt+"\n";
		}
		return result;
	}
	
	public String toDebugString()
	{
		String result="";
		for(Utterance utt: this)
		{
			result+=utt.toDebugString()+"\n";
		}
		return result;
		
	}
	
	public boolean add(Utterance u)
	{
		if (!participants.contains(u.getSpeaker()))
			participants.add(u.getSpeaker());
		
		return super.add(u);
	}
	
	public static void main(String a[])
	{
		Utterance u=new Utterance("A: bill");
		Dialogue d=new Dialogue();
		d.add(u);
		System.out.println(d.getParticiapnts());
		d.append(new UtteredWord("is","A"));
		System.out.println(d);
	}

	public List<String> getParticiapnts() {
		
		return participants;
	}

	public UtteredWord lastWord()
	{
		return this.lastUtterance().lastWord();
	}
	
	public Utterance lastUtterance()
	{
		if (isEmpty())
			return null;
		return this.get(this.size()-1);
	}
	
	public void append(UtteredWord w) {
		logger.debug("Appending:"+w);
		if (isEmpty()||!w.speaker().equals(lastUtterance().getSpeaker()))
		{
			Utterance utt=new Utterance(w);
			add(utt);
			
		}
		else
		{
			lastUtterance().append(w);
			
		}
		
		if (!this.participants.contains(w.speaker()))
			this.participants.add(w.speaker());

		
		
	}
	
	public boolean rollBack(int n)
	{
		Utterance lastUtt=lastUtterance();
		if (lastUtt.getLength()<n)
		{
			logger.warn("Cannot rollback into a previous utterance... for now.");
			return false;
		}
		
		int i=lastUtt.words.size()-1;
		int rev=0;
		while(rev<n)
		{
			
			UtteredWord revoked=lastUtt.words.get(i);
			if (revoked instanceof RevokedWord)
			{	System.out.println("Found revoked:"+revoked);
				i--;
				continue;
			}
			System.out.println("Revoking:"+lastUtt.words.get(i));
			lastUtt.words.set(i, new RevokedWord(lastUtt.words.get(i)));
			rev++;
			i--;
		}
		return true;
	}
	
	
}
