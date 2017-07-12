package qmul.ds.test.rules;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.log4j.Logger;

import qmul.ds.Dialogue;
import qmul.ds.Utterance;
import qmul.ds.action.SpeechActInferenceGrammar;
import qmul.ds.test.rules.DyLanParser.ParseForm;
import qmul.ds.tree.Node;

public class SpeechActInferenceGenerator {
	static Logger logger = Logger.getLogger(SpeechActInferenceGenerator.class);
	
	private static final String dir = "corpus/";
	private static final String parsed_file = "/analysis/parsable_dialogues.txt";
	public static final String ENGLISHTTRURL = "resource/2017-english-ttr-copula-simple/";
	private List<Dialogue> dlgList;
	private SpeechActInferenceGrammar sa_inference_map;
	private String corpus;
	
	private DyLanParser dlParser;
	
	public SpeechActInferenceGenerator(String corpus, String english_ttr_url){
		this.loadDialogues(corpus);
		
		if(english_ttr_url == null)
			english_ttr_url = this.ENGLISHTTRURL;
		sa_inference_map = new SpeechActInferenceGrammar(english_ttr_url);
		logger.info("sa_inference_map: " + sa_inference_map);

		this.initialDyLanParser(english_ttr_url);
	}
	
	private void initialDyLanParser(String english_ttr_url) {
		if(english_ttr_url == null)
			english_ttr_url = this.ENGLISHTTRURL;
		
		dlParser = new DyLanParser(english_ttr_url, null);
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
			String path = this.dir + corpus + this.parsed_file;
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

	private void start() {
		for(Dialogue dlg: this.dlgList){
			// reset the dylan parser for new dialogue
			this.dlParser.initParser();
			
			for(Utterance utt: dlg){
				for(int i=0; i< utt.getTotalNumberOfSegments(); i++){
//					logger.info("utt.getUttSegment("+i+"): " + utt.getUttSegment(i));
					String text = utt.getUttSegment(i);
					text = text.replaceAll("%colorvalue", "red").replaceAll("%shapevalue", "square");
					String act = utt.getDAt(i);
					
					ParseForm context = this.dlParser.parse(text);
					
					Node pointedNode = context.getContxtalTree().getPointedNode().clone();
					logger.info("act => " + act + " ==== " + pointedNode);
				}
			}
		}
	}
	
	public static void main(String[] args){
		SpeechActInferenceGenerator learner = new SpeechActInferenceGenerator("BURCHAK", null);
		
		System.out.println("IS IT READY?");
		Scanner scanInput = new Scanner(System.in);
		String comman  = scanInput.nextLine();
			
		if(comman.trim().equals("y")){
			learner.start();
		}
		else if(comman.trim().equals("n"))
			System.exit(0);
	}
}
