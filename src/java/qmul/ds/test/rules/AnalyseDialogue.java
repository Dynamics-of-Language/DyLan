package qmul.ds.test.rules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import qmul.ds.Dialogue;
import qmul.ds.Utterance;
import qmul.ds.test.rules.DyLanParser.ParseForm;
import qmul.ds.tree.Node;

/**
 * Description: this class is applied to test the coverage of the existing grammar onto dialogues:
 * system will generate 3 text files in a folder of "analysis":
 * 1) parsable dialogues -- record all parsable dialogues with annotated actions
 * 2) parsable dialogues (ttr) -- record all parsable dialogues with annotated actions and ttr 
 * 								  record type on the pointed node on the tree
 * 3) unparsable dialogues -- record all unparsable dialogues with annotated actions, including 
 *                            the exceptions from the parser and existing ttr semantics
 * 
 * @author Yanchao Yu
 */

public class AnalyseDialogue{
	static Logger logger = Logger.getLogger(AnalyseDialogue.class);
	
	private static final String dir = "corpus/";
	private static final String filename = "/dialogues.txt";
	private static final String slot_values_file = "/slot-values.txt";
	private static final String analysis_fir = "analysis/";
	private static final String exception_file = "unparsable_dialogues.txt";
	private static final String parsed_file = "parsable_dialogues_ttr.txt";
	private static final String parsable_file = "parsable_dialogues.txt";
	private static final String action_map_file = "act-mapping.txt";
	public static final String ENGLISHTTRURL = "resource/2017-english-ttr-copula-simple";
	
	private Set<String> actSet;
	private List<Dialogue> dlgList;
	private Map<String, Set<String>> slot_values;
	private String corpus;
	
	private DyLanParser dlParser;
	
	public AnalyseDialogue(String domain, String english_ttr_url){
		this.initialDyLanParser(english_ttr_url);
		this.loadDialogues(domain);
		this.loadSlotValues();
	}

	private void initialDyLanParser(String english_ttr_url) {
		if(english_ttr_url == null)
			english_ttr_url = this.ENGLISHTTRURL;
		
		dlParser = new DyLanParser(english_ttr_url, null);
	}
	
	private void loadSlotValues() {
		if(slot_values == null)
			slot_values = new HashMap<String, Set<String>>();
		else
			slot_values.clear();
		
		BufferedReader br = null;
	    String line = "";
	    String splitBy = ":";
	        
	    try {
			String path = this.dir + corpus + this.slot_values_file;
			br = new BufferedReader(new FileReader(path));
			List<String> curDialogue = new ArrayList<String>();
			while ((line = br.readLine()) != null) {
				line = line.trim().toLowerCase();
				
				if(!line.trim().isEmpty() && !line.trim().startsWith("//")){
					
					String[] items = line.trim().split(splitBy);
					
					String slot = items[0];
					String[] values = items[1].trim().split(",");
					slot_values.put(slot, new HashSet<String>(Arrays.asList(values)));
				}
			}
			
			logger.info("slot_values: " + slot_values);
			
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
		Queue<String> utt_queue = new LinkedList<String>();
		Queue<String> utt_ttr_queue = new LinkedList<String>();
		actSet = new TreeSet<String>();
		
		if(this.dlgList != null && !this.dlgList.isEmpty()){
			for(Dialogue dlg: this.dlgList){

				// reset the dylan parser for new dialogue
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
					String text = "";
					for(int i=0; i< utt.getTotalNumberOfSegments(); i++){
//						logger.info("utt.getUttSegment("+i+"): " + utt.getUttSegment(i));
						String act = utt.getDAt(i);
						act = this.convert_act_to_sa_name(act, text);
						actSet.add(act);

						text = utt.getUttSegment(i);
						text = text.replaceAll("%colorvalue", this.slot_values.get("%colorvalue").iterator().next()).replaceAll("%shapevalue", this.slot_values.get("%shapevalue").iterator().next());
						
						// parse the text using DyLan module
						// if throw an exception, print the entire dialogue into a separate .txt file, otherwise, keep parsing
						if(!text.contains("<rt>") && !text.contains(".") && !text.contains("?"))
							text += ".";
						logger.info("AFTER:: text: "+ text);
						
						String[] words = text.split(" ");
						for(int j=0; j < words.length; j++){
//							String word = words[j];
//							
//							if(i==0 && j==0)
							String word = utt.getSpeaker() + ": " + words[j];
							
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

								utt_queue.clear();
								utt_ttr_queue.clear();
								break outofdialgloop;
							}
						}
						Node rootNode = result.getContxtalTree().getRootNode().clone();
						Node pointedNode = result.getContxtalTree().getPointedNode().clone();
						
						String str = utt.getSpeaker() + ": " + text +" -- " + act;
						utt_queue.add(str);
						str += "--" + pointedNode+"\n";
						utt_ttr_queue.add(str);
					}
				}
				
				if(is_parse_successful){
					try {
						if(utt_ttr_queue != null && !utt_ttr_queue.isEmpty()){
							while(!utt_ttr_queue.isEmpty()){
								String str = utt_ttr_queue.poll();
								appendExceptionToFile(parsed_file,  str);
							}

							appendExceptionToFile(parsed_file,  "\r\n");
						}
					} catch (IOException e1) {
						logger.error(e1.getMessage());
					}
					
					try {
						if(utt_queue != null && !utt_queue.isEmpty()){
							while(!utt_queue.isEmpty()){
								String str = utt_queue.poll();
								appendExceptionToFile(parsable_file,  str+"\n");
							}

							appendExceptionToFile(parsable_file,  "\r\n");
						}
					} catch (IOException e1) {
						logger.error(e1.getMessage());
					}
				}
			}
		}

		if(!actSet.isEmpty()){
			for(String act: actSet){
				
				try {
					appendExceptionToFile(this.action_map_file, act+"\r\n");
				} catch (IOException e1) {
					logger.error(e1.getMessage());
				}
			}
		}
	}
	
	private void appendExceptionToFile(String filename, String error) throws IOException{
		String dir_path = this.dir + corpus + "/" + analysis_fir;
		
		File dir = new File(dir_path);
		if(!dir.exists())
			dir.mkdir();
		
		String path = dir_path + filename;

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
	
	private String convert_act_to_sa_name(String action, String utt){
		String sa_name = action.toLowerCase().replaceAll("=%colorvalue", "").replaceAll("=%shapevalue", "");
		
		if(sa_name.contains("ackrepeat"))
			sa_name = sa_name.replace("ackrepeat", "accept-info");
		
		if(sa_name.contains("ack"))
			sa_name = sa_name.replace("ack", "accept");
		
		if(sa_name.contains("()"))
			sa_name = sa_name.replace("()", "");
		else
			sa_name = sa_name.replace(")", "").replace("(", "-").replaceAll("&", "-");
		
		if(sa_name.contains("*"))
			sa_name = sa_name.replace("*", "-info-");
		
		logger.info("before: " + action +" -- after: " + sa_name);
		
		return sa_name + " >> ";
	}
	
	/*********************** Main Function **************************/
	public static void main(String[] args){
		AnalyseDialogue learner = new AnalyseDialogue("BURCHAK", null); 
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
