package qmul.ds.test.rules;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.log4j.Logger;

import qmul.ds.Context;
import qmul.ds.Dialogue;
import qmul.ds.Utterance;
import qmul.ds.action.Action;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.SpeechActInferenceGrammar;
import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.formula.Formula;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.test.rules.DyLanParser.ParseForm;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.LabelFactory;

public class SpeechActInferenceGenerator {
	static Logger logger = Logger.getLogger(SpeechActInferenceGenerator.class);
	
	private static final String dir = "corpus/";
	private static final String parsed_file = "/analysis/parsable_dialogues.txt";
	public static final String ENGLISHTTRURL = "resource/2017-english-ttr-copula-simple/";
	public static final String ACTMAP = "act-mappings.txt";
	private List<Dialogue> dlgList;
	private SpeechActInferenceGrammar sa_gammar_templates;
	private SpeechActInferenceGrammar sa_inference_map;
	private Map<String, List<String>> act_map;
	private String corpus;
	
	private DyLanParser dlParser;
	
	private String rescource_dir;
	
	public SpeechActInferenceGenerator(String corpus, String english_ttr_url){
		this.loadDialogues(corpus);
		
		if(english_ttr_url == null)
			english_ttr_url = this.ENGLISHTTRURL;

		this.rescource_dir = english_ttr_url;
				
		this.sa_gammar_templates = new SpeechActInferenceGrammar(english_ttr_url, "speech-act-grammar-template.txt");
//		logger.debug("sa_gammar_templates: " +sa_gammar_templates + "--"+ sa_gammar_templates.get("info-color").getEffect().toString());
		
		this.sa_inference_map = new SpeechActInferenceGrammar();
		
		this.act_map = this.loadActMappings(english_ttr_url);

		this.initialDyLanParser(english_ttr_url);
	}
	
	private Map<String, List<String>> loadActMappings(String english_ttr_url) {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		
		BufferedReader br = null;
	    String line = "";
	    String splitBy = ">>";
	        
	    try {
			String path = english_ttr_url + this.ACTMAP;
			br = new BufferedReader(new FileReader(path));
			List<String> curDialogue = new ArrayList<String>();
			while ((line = br.readLine()) != null) {
				line = line.trim().toLowerCase();
				if(!line.isEmpty() && !line.startsWith("//")){
					String[] items = line.split(splitBy);
					String key = items[0].trim();
					
					String value = items[1].trim();
					value = value.replace("[", "").replace("]", "");
					String[] arr = value.split(",");
					
					map.put(key, Arrays.asList(arr));
				}
			}
			return map;
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
		
		return null;
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
			
			boolean is_parse_successful = true;
			ParseForm result = null;
			
			for(Utterance utt: dlg){
				for(int i=0; i< utt.getTotalNumberOfSegments(); i++){
//					logger.debug("utt.getUttSegment("+i+"): " + utt.getUttSegment(i));
					String text = utt.getUttSegment(i);
					text = text.replaceAll("%colorvalue", "red").replaceAll("%shapevalue", "square");
					String act = utt.getDAt(i);
					
					if(!text.contains("<rt>") && !text.contains(".") && !text.contains("?"))
						text += ".";
					
					String[] words = text.split(" ");
					for(int j=0; j < words.length; j++){
//						String word = words[j];
//						
//						if(i==0 && j==0)
						String word = utt.getSpeaker() + ": " + words[j];
						
						result = dlParser.parse(word);
					}
					

					Tree resultTree = result.getContxtalTree().clone();
					Context<DAGTuple, GroundableEdge> context = result.getContext();
					Node rootNode = resultTree.getRootNode().clone();
					Node pointedNode = resultTree.getPointedNode().clone();
					Formula f = pointedNode.getFormula();

					TTRRecordType ttr = (TTRRecordType) pointedNode.getFormula();
					logger.debug("act => " + act + " ==== " + pointedNode + " ==== " + ttr);
					
					Tree newTree = null;
					Iterator<Entry<String, ComputationalAction>> iterator = sa_inference_map.entrySet().iterator();
					while(iterator.hasNext()){
						Entry<String, ComputationalAction> entry = iterator.next();
						String key = entry.getKey();

						String sub = key.replace(act+"-", "");
						if(this.isInteger(sub)){
							logger.debug("act(" + act + ") ==== key(" + key + ")");
							ComputationalAction cAct = entry.getValue();
							logger.info("cAct: " + cAct.getEffect());
							
							newTree = cAct.exec(resultTree, context);
						}
					}

					if(newTree != null){
						if(!hasSpeechActOn(newTree.getPointedNode(), act)){
							List<ComputationalAction> actions = this.findComputationalAction(this.sa_gammar_templates, act);
							
							for(ComputationalAction action: actions){
								 Effect effect = this.addNewFormula(action.getEffect(), f);
								
								this.sa_inference_map.addNewComputationalAction(act, effect);
								logger.debug("new Effect at (" + act + "): " + effect);
							}
						}
					}
					else{
						List<ComputationalAction> actions = this.findComputationalAction(this.sa_gammar_templates, act);
						
						for(ComputationalAction action: actions){
							 Effect effect = this.addNewFormula(action.getEffect(), f);
							
							this.sa_inference_map.addNewComputationalAction(act, effect);
							logger.debug("new Effect at (" + act + "): " + effect);
						}
					}
				}
			}
		}
		this.sa_inference_map.exportToFile(this.rescource_dir, "learned-speech-act-inference-grammar.txt");
	}
	
	private Effect addNewFormula(Effect effect, Formula f) {
		if(effect instanceof IfThenElse){
			IfThenElse ite = ((IfThenElse)effect).clone();
			
			Label label1 = LabelFactory.create("Fo(W1)");
			ite.addNewLabelintoIF(label1);
			
			Label label2 = LabelFactory.create("W1<<"+f+"");
			ite.addNewLabelintoIF(label2);
			return ite;
		}
		return effect;
	}

	private List<ComputationalAction> findComputationalAction(SpeechActInferenceGrammar map, String act) {
		List<ComputationalAction> action_list = new ArrayList<ComputationalAction>();
		
		Iterator<Entry<String, ComputationalAction>> iterator = map.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<String, ComputationalAction> entry = iterator.next();
			String key = entry.getKey();
			
			if(key.trim().equals(act)){
				action_list.add(entry.getValue());
			}
			else {
				String sub = key.replace(act+"-", "");
				if(this.isInteger(sub)){
					action_list.add(entry.getValue());
				}
			}
		}
		
		return action_list;
	}

	private boolean isInteger(String s) {
	    try { 
	        Integer.parseInt(s); 
	    } catch(NumberFormatException e) { 
	        return false; 
	    } catch(NullPointerException e) {
	        return false;
	    }
	    // only got here if we didn't return false
	    return true;
	}
	
	// check if the pointed Node contains particular speech-act
	private boolean hasSpeechActOn(Node pointedNode, String act){
		if(pointedNode != null){
			logger.info("@@@@@@@@@@@@@ act: " + act + " -- Pointed Node: " + pointedNode);
			
			for(Label l: pointedNode){
				String label_str = l.toString().trim();
				if(label_str.startsWith("sa:")){
					label_str = label_str.replace("sa:", "");
					logger.info("@@@@@@@@@@@@@ Label String: " + label_str);
					
					if(act_map != null && act_map.containsKey(act)){
						
						boolean anyMatch = true;
						List<String> itemList = act_map.get(act);
						for(String item: itemList){
							if(!label_str.contains(item)){
								anyMatch = false;
								break;
							}
						}
						
						if(anyMatch){
							logger.info("++++++++++ label_str: " + label_str + ";   act: " + act);
							return true;
						}
					}
				}
			}
		}
		
		return false;
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
