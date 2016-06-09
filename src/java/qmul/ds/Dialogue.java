package qmul.ds;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

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

	public Dialogue(List<String> lines) {
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
	 * dialogue.
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
			if (line.trim().isEmpty()) {
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
	
	public static void main(String a[])
	{
		try
		{
			List<Dialogue> dialogues=Dialogue.loadDialoguesFromFile("../babble/data/Domain-Dialogues/shopping-mall-artificial");
	
			for(Dialogue d: dialogues)
			{
				System.out.println(d.toDebugString());
			}
			
		}catch(Exception e)
		{
			System.out.println(e);
		}
	}

	public List<String> getParticiapnts() {
		
		return participants;
	}
}
