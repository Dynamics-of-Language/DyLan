package qmul.ds.test.rules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.Context;
import qmul.ds.Dialogue;
import qmul.ds.Utterance;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.SpeechActInferenceGrammar;
import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.meta.Meta;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.formula.AtomicFormula;
import qmul.ds.formula.Formula;
import qmul.ds.formula.PredicateArgumentFormula;
import qmul.ds.formula.TTRField;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.test.rules.DyLanParser.ParseForm;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.LabelFactory;

public class SpeechActInferenceGenerator {
	static Logger logger = Logger.getLogger(SpeechActInferenceGenerator.class);
	
	private final String dir = "corpus/";
	private final String slot_values_file = "/slot-values.txt";
	private final String parsed_file = "/analysis/parsable_dialogues.txt";
	public final String ENGLISHTTRURL = "resource/2017-english-ttr-copula-simple/";
	public final String ACTMAP = "act-mappings.txt";
	private List<Dialogue> dlgList;
	private HashMap<String, ComputationalAction> sa_gammar_templates;
//	private SpeechActInferenceGrammar sa_inference_map;
	
	private HashMap<String, List<ComputationalAction>> sa_inference_map;
	private Map<String, List<String>> act_map;
	private Map<String, Set<String>> slot_values;
	private Map<String, Formula> meta_replacements;
	
	private String corpus;
	
	private DyLanParser dlParser;
	
	private String rescource_dir;
	
	@SuppressWarnings("unchecked")
	public SpeechActInferenceGenerator(String corpus, String english_ttr_url){
		if(english_ttr_url == null)
			english_ttr_url = this.ENGLISHTTRURL;
		this.rescource_dir = english_ttr_url;
		
		this.loadDialogues(corpus);
		this.loadSlotValues();

		this.initialDyLanParser(english_ttr_url);
				
		this.sa_gammar_templates = (HashMap<String, ComputationalAction>) new SpeechActInferenceGrammar(english_ttr_url, "speech-act-grammar-template.txt").clone();
//		logger.info("sa_gammar_templates: " +sa_gammar_templates + "--"+ sa_gammar_templates.get("info-color").getEffect().toString());
		
//		this.sa_inference_map = new SpeechActInferenceGrammar();
		this.sa_inference_map = new HashMap<String, List<ComputationalAction>>();
		this.act_map = this.loadActMappings(english_ttr_url);
	}
	
	private Map<String, List<String>> loadActMappings(String english_ttr_url) {
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		
		BufferedReader br = null;
	    String line = "";
	    String splitBy = ">>";
	        
	    try {
			String path = english_ttr_url + this.ACTMAP;
			br = new BufferedReader(new FileReader(path));
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
			logger.info("----------------- New Dialogue -----------------");
			// reset the dylan parser for new dialogue
			this.dlParser.initParser();
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
						String word = utt.getSpeaker() + ": " + words[j];
						result = dlParser.parse(word);
					}

					Tree resultTree = result.getContxtalTree().clone();
					Context<DAGTuple, GroundableEdge> context = result.getContext();
					Node rootNode = resultTree.getRootNode().clone();
					Node pointedNode = resultTree.getPointedNode().clone();
					Formula f = pointedNode.getFormula();
					
					TTRRecordType ttr = this.abstractOutSlotValues((TTRRecordType)f);
					logger.debug("act => " + act + " ==== " + pointedNode);
					logger.info("@@@@@@@@@@@@@@@@@@@@@ act => " + act + " ==== " + ttr);
					logger.info("@@@@@@@@@@@@@@@@@@@@@ meta replacements: " + this.meta_replacements);
					
					Tree newTree = null;
					if(this.sa_inference_map.containsKey(act)){
						List<ComputationalAction> action_list = this.sa_inference_map.get(act);
						
						// go through all existing grammars under the particular action
						for(ComputationalAction cAct: action_list){
							logger.info("cAct: " + cAct.getName());

							newTree = cAct.exec(resultTree, context);
							
							Effect effect_template = cAct.getEffect();
							if(effect_template instanceof IfThenElse){
								IfThenElse ifthenelse = (IfThenElse)effect_template;
								ifthenelse.setupBacktrackers(new ArrayList<Meta<?>>());
								
								Label[] if_labels = ifthenelse.getIFClause().clone();
								
								for(int j=0; j < if_labels.length; j++){
									Label label = if_labels[j];
									if(label.toString().contains("W1<<")){
										TTRRecordType exist_ttr = TTRRecordType.parse(label.toString().substring(label.toString().indexOf("W1<<")+4));

										boolean issubsumed = exist_ttr.subsumes(ttr);
										logger.info("/////////// " + exist_ttr +" -- " + ttr + " = " + issubsumed);
									}
								}
							}
						}
					}
					
					if(newTree != null){
						if(!hasSpeechActOn(newTree.getPointedNode(), act)){
							logger.info("cannot find any clues on : " + act);
							List<ComputationalAction> actions = this.findComputationalAction(this.sa_gammar_templates, act);
							logger.info("actions(" + actions.size() + ") -- " + actions);
							
							for(ComputationalAction action: actions){
								Effect effect_template = action.getEffect();
								logger.debug("old Effect:  \r\n" + effect_template);
								
								Effect effect = this.addNewFormula(effect_template, ttr);
								effect = this.replaceMetaVariable(effect, this.meta_replacements);
								logger.info("new Effect at (" + act + "): \r\n" + effect);
								
								List<ComputationalAction> actList = new ArrayList<ComputationalAction>();
								if(this.sa_inference_map.containsKey(act))
									actList = this.sa_inference_map.get(act);
								
								actList.add(new ComputationalAction(act, effect));
								this.sa_inference_map.put(act, actList);
							}
						}
					}
					else{
						logger.info("cannot complie a new tree on " + act);
						List<ComputationalAction> actions = this.findComputationalAction(this.sa_gammar_templates, act);
						
						logger.info("actions" + actions.size() + " -- " + actions);
						
						for(ComputationalAction action: actions){
							Effect effect_template = action.getEffect();
							logger.debug("old Effect:  \r\n" + effect_template);
							
							Effect effect = this.addNewFormula(effect_template, ttr);
							effect = this.replaceMetaVariable(effect, this.meta_replacements);
							logger.info("new Effect at (" + act + "): \r\n" + effect);
							
//							this.sa_inference_map.addNewComputationalAction(act, effect);
							List<ComputationalAction> actList = new ArrayList<ComputationalAction>();
							if(this.sa_inference_map.containsKey(act))
								actList = this.sa_inference_map.get(act);
							
							actList.add(new ComputationalAction(act, effect));
							this.sa_inference_map.put(act, actList);
						}
					}
				}
			}
		}
		
		this.exportToFile(sa_inference_map, this.rescource_dir, "speech-act-inference-grammar-learned.txt");
	}
	
	private void exportToFile(HashMap<String, List<ComputationalAction>> map, String dir,
			String fileName) {
		if(map != null){
			File file = new File(dir, fileName);
			try {
				if(!file.exists())
					file.createNewFile();
				
				FileWriter fileWriter = new FileWriter(file, true);
		        
				Iterator<Entry<String, List<ComputationalAction>>> iterator = map.entrySet().iterator();
				while(iterator.hasNext()){
					Entry<String, List<ComputationalAction>> entry = iterator.next();
					String key = entry.getKey();
					List<ComputationalAction> actionList = entry.getValue();
					
					for(int i=0; i < actionList.size(); i++){
						String action_name = key+"-"+i;
						ComputationalAction action = actionList.get(i);
						action.setName(action_name);
						fileWriter.append(action_name+"\n");
						fileWriter.append(action.getEffect()+"\n");
						
						fileWriter.append("\r\n");
					}
				}
		        fileWriter.close();
			} catch (FileNotFoundException e) {
				logger.warn("No speech act inference file " + file.getAbsolutePath());
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
	}

	private Effect replaceMetaVariable(Effect effect, Map<String, Formula> replacements) {
		if(effect instanceof IfThenElse){
			IfThenElse ite = ((IfThenElse)effect).clone();
			ite.substitues(replacements);
			return ite;
		}
		return effect;
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

	private List<ComputationalAction> findComputationalAction(HashMap<String, ComputationalAction> map, String act) {
		logger.info("++++++++++ act : " + act);
		logger.debug("++++++++++ ComputationalAction MAP : " + map);
		
		List<ComputationalAction> action_list = new ArrayList<ComputationalAction>();
		
		Iterator<Entry<String, ComputationalAction>> iterator = map.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<String, ComputationalAction> entry = iterator.next();
			String key = entry.getKey();
			ComputationalAction action = entry.getValue();
			
			if(key.trim().equals(act)){
				action_list.add(entry.getValue());
			}
			else {
				String sub = key.replace(act+"-", "");
				if(this.isInteger(sub)){
					action_list.add(action);
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
	
	/**
	 * Abstracts out the slot values according to slot_values
	 * 
	 * @param rec
	 * @return new record type with slot values abstracted
	 */

	public TTRRecordType abstractOutSlotValues(TTRRecordType rec) {
		this.meta_replacements = new HashMap<String, Formula>();
		
		TTRRecordType result = new TTRRecordType();

		for (TTRField f : rec.getFields()) {
			TTRField newF = new TTRField(f);
			if (f.getType() != null && f.getType() instanceof AtomicFormula) {
				AtomicFormula af = (AtomicFormula) f.getType();
				logger.debug("------------------ af.getName(): " + af.getName());

				Iterator<Entry<String, Set<String>>> iterator1 = this.slot_values.entrySet().iterator();
				while(iterator1.hasNext()){
					Entry<String, Set<String>> entry = iterator1.next();
					Set<String> values = entry.getValue();
					if (values.contains(af.getName())) {
						logger.debug("------------------ I found something I have in values : " + af.getName());
						Formula replaced=result.getFreshAtomicMetaVariable();
						newF.setType(replaced);
						
						String key = entry.getKey().trim().equals("%colorvalue") ? "P8" : "P9";
						meta_replacements.put(key, replaced);
					}
				}


			} else if (f.getType() != null && f.getType() instanceof PredicateArgumentFormula) {
				PredicateArgumentFormula paf = (PredicateArgumentFormula) f.getType();
				logger.debug("------------------ paf.getPredicate().getName(): " + paf.getPredicate().getName());
				
				Iterator<Entry<String, Set<String>>> iterator1 = this.slot_values.entrySet().iterator();
				while(iterator1.hasNext()){
					Entry<String, Set<String>> entry = iterator1.next();
					Set<String> values = entry.getValue();
					if (values.contains(paf.getPredicate().getName())) {
						logger.debug("------------------ I found something I have in values : " + paf.getPredicate().getName());
						Formula replaced = new PredicateArgumentFormula(result.getFreshPredicateMetaVariable(), paf.getArguments());
						newF.setType(replaced);
						
						String key = entry.getKey().trim().equals("%colorvalue") ? "P8" : "P9";
						meta_replacements.put(key, replaced);
					}
				}
			}

			result.add(newF);
		}

		return result.removeHead();

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
