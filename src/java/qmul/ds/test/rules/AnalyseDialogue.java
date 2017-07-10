package qmul.ds.test.rules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;

import qmul.ds.Dialogue;
import qmul.ds.Utterance;
import qmul.ds.test.rules.DyLanParser.ParseForm;

public class AnalyseDialogue{
	static Logger logger = Logger.getLogger(AnalyseDialogue.class);
	
	private static final String dir = "corpus/";
	private static final String filename = "/dialogues.txt";
	private static final String exception_file = "analysis/unparsable_dialogues.txt";
	private static final String parsed_file = "analysis/parsable_dialogues.txt";
	public static final String ENGLISHTTRURL = "resource/2017-english-ttr-copula-simple";
	private List<Dialogue> dlgList;
	private String corpus;
	
	private DyLanParser dlParser;
	
	public AnalyseDialogue(String domain){
		this.initialDyLanParser();
		this.loadDialogues(domain);
	}
	
	private void initialDyLanParser() {
		dlParser = new DyLanParser(this.ENGLISHTTRURL, null);
	}

	private void loadDialogues(String corpus){
		if(dlgList == null)
			dlgList = new ArrayList<Dialogue>();
		else
			dlgList.clear();
		
		this.corpus = corpus;
		
		BufferedReader br = null;
	    String line = "";
	    String splitBy = ";";
	        
	    try {
			String path = this.dir + corpus + filename;
			br = new BufferedReader(new FileReader(path));
			List<String> curDialogue = new ArrayList<String>();
			while ((line = br.readLine()) != null) {
				line = line.trim().toLowerCase();
				
				if(!line.isEmpty() && !line.contains("<none> <rt> -- listen()")){
					curDialogue.add(line);
				}
				
				if(line.trim().isEmpty() && !curDialogue.isEmpty()){
					dlgList.add(new Dialogue(curDialogue));
					curDialogue.clear();
				}
			}
		} catch (IOException e) {logger.error("Error: " + e.getMessage());}
		finally{
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
		               e.printStackTrace();
		           }
		       }
		}
	}
	
	private Dialogue current_dialog = null;
	private String curTTRContxt = null;
	private String prevTTRContxt = null;
	public void start(){
		if(this.dlgList != null && !this.dlgList.isEmpty()){
			for(Dialogue dlg: this.dlgList){
				if(this.dlParser != null)
					dlParser.initParser();
				this.curTTRContxt = null;
				this.prevTTRContxt = null;
				
				this.current_dialog = dlg;
				logger.debug("current_dialog: " + current_dialog.size());
				
				boolean is_parse_successful = true;
				ParseForm result = null;
				
				outofdialgloop:
				
				for(Utterance utt: this.current_dialog){
//					logger.debug("utt: " + utt);
					for(int i=0; i< utt.getTotalNumberOfSegments(); i++){
//						logger.info("utt.getUttSegment("+i+"): " + utt.getUttSegment(i));
						
						String text = utt.getUttSegment(i);
						text = text.replaceAll("%colorvalue", "red").replaceAll("%shapevalue", "square");
						String act = utt.getDAt(i);

//						logger.info("text["+act+"]: " + text);
//						
						// parse the text using DyLan module
						// if throw an exception, print the entire dialogue into a separate .txt file, otherwise, keep parsing
						System.err.println("text: "+ text);
						String[] words = text.split(" ");
						for(int j=0; j < words.length; j++){
							String word = words[j];
							
							if(i==0 && j==0)
								word = utt.getSpeaker() + ": " + word;
							
							result = dlParser.parse(word);
							
							if(result.hasException()){
								is_parse_successful = false;
									
								System.err.println("this dialogue is unable to parse...");
								String error_msg = current_dialog.toString();
								error_msg += "@@ Exception: " + result.getException() + " -- text: "+ text + "\r\n";
								error_msg += "@@ existing TTR context: " + curTTRContxt + "\r\n\n";
									
								try {
									appendExceptionToFile(exception_file,error_msg);
								} catch (IOException e1) {
									logger.error(e1.getMessage());
								}
									
								break outofdialgloop;
							}
						}
//						result = dlParser.parse(text);
						
	//					logger.info(utt + " -- " + result.getContxtalTree());
						curTTRContxt = "[" + act + "]: " + result.getContxtalTree().toString();
						logger.info("curTTRContxt: " + curTTRContxt);
					}
				}
				
				if(is_parse_successful){
					try {
						appendExceptionToFile(parsed_file,  current_dialog.toString()+"\r\n");
					} catch (IOException e1) {
						logger.error(e1.getMessage());
					}
				}
			}
		}
	}
	
	private void appendExceptionToFile(String filename, String error) throws IOException{
		String path = this.dir + corpus + "/" + filename;

		File newfile = new File(path);
		FileWriter fileWriter = new FileWriter(newfile, true);
		if(!newfile.exists()){
			newfile.createNewFile(); 
			fileWriter.write(error);
		}
		
		else
	        fileWriter.append(error);

        fileWriter.close();
	}

	private void printDialogue() {
		if(this.dlgList != null && !this.dlgList.isEmpty()){
			int index = 0;
			for(Dialogue dlg: this.dlgList){
				logger.info("["+(index++)+"]: " + dlg);
			}
		}
	}
	
	/*********************** Main Function **************************/
	public static void main(String[] args){
		AnalyseDialogue learner = new AnalyseDialogue("BURCHAK"); 
		System.out.println("IS IT READY?");
		Scanner scanInput = new Scanner(System.in);
		String comman  = scanInput.nextLine();
			
		if(comman.trim().equals("y")){
//			learner.printDialogue();
			learner.start();
		}
		else if(comman.trim().equals("n"))
			System.exit(0);
	}
}
