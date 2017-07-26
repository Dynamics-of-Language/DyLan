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
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import qmul.ds.Context;
import qmul.ds.Dialogue;
import qmul.ds.Utterance;
import qmul.ds.action.ComputationalAction;
import qmul.ds.action.SpeechActInferenceGrammar;
import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.formula.AtomicFormula;
import qmul.ds.formula.Formula;
import qmul.ds.formula.Predicate;
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
	private final String SPEECHACT_GRAMMAR_TEMPLATE = "speech-act-grammar-template.txt";
	public final String ENGLISHTTRURL = "resource/2017-english-ttr-copula-simple/";
	public final String ACTMAP = "act-mappings.txt";
	private List<Dialogue> dlgList;
	private TreeMap<String, ComputationalAction> sa_gammar_templates;

//	private HashMap<String, List<String>> sa_gammar_templates;
	private HashMap<String, List<ComputationalAction>> sa_inference_map;
	private Map<String, List<String>> act_map;
	private Map<String, Set<String>> slot_values;
	private Map<Formula, Formula> meta_replacements;
	
	private String corpus;
	
	private DyLanParser dlParser;
	
	private String rescource_dir;
	
	@SuppressWarnings("unchecked")
	public SpeechActInferenceGenerator(String corpus, String english_ttr_url){
		if(english_ttr_url == null)
			english_ttr_url = this.ENGLISHTTRURL;
		this.rescource_dir = english_ttr_url;

		this.initialDyLanParser(english_ttr_url);
		this.act_map = this.loadActMappings(english_ttr_url);
		logger.debug("act_map: " + act_map);
		this.loadDialogues(corpus);
		this.loadSlotValues();

		this.sa_gammar_templates = (TreeMap<String, ComputationalAction>) new SpeechActInferenceGrammar(english_ttr_url, SPEECHACT_GRAMMAR_TEMPLATE);
		logger.debug("sa_gammar_templates: " +sa_gammar_templates.size());
		
		this.sa_inference_map = new HashMap<String, List<ComputationalAction>>();
	}
	
	
	/************************ Class Initialiasation *******************************/
	private void initialDyLanParser(String english_ttr_url) {
		if(english_ttr_url == null)
			english_ttr_url = this.ENGLISHTTRURL;
		
		dlParser = new DyLanParser(english_ttr_url, null);
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
			
			logger.debug("slot_values: " + slot_values);
			
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
	
	/************************ ENF of Class Initialiasation *******************************/

	private void start() {
		String prev_act = null;
		for(Dialogue dlg: this.dlgList){
			logger.debug("----------------- New Dialogue -----------------");
			// reset the dylan parser for new dialogue
			this.dlParser.initParser();
			ParseForm result = null;
			
			for(Utterance utt: dlg){
				for(int i=0; i< utt.getTotalNumberOfSegments(); i++){
//					logger.debug("utt.getUttSegment("+i+"): " + utt.getUttSegment(i));
					String text = utt.getUttSegment(i);
					text = text.replaceAll("%colorvalue", "red").replaceAll("%shapevalue", "square");
					String act = utt.getDAt(i);
					
					if(prev_act != null && prev_act.contains("polar")){
						logger.debug("--- prev_act: " + prev_act + "; current_act: "+ act);
						if(act.equals("accept")){
							act = prev_act.replace("polar", "info");
						}
						
						else if(act.equals("reject")){
							act = prev_act.replace("polar", "info-neg");
						}
						
						logger.debug("--- new current_act: "+ act);
					}

					if(act.equals("accept") && (text.contains("it is") || text.contains("we do")))
						act = prev_act.replace("polar", "info");
					
					if(act.contains("accept-info")){
						logger.debug("--- original current_act: "+ act);
						act = act.replace("accept-", "");
						logger.debug("--- new current_act: "+ act);
					}
					
					if(!text.contains("<rt>") && !text.contains(".") && !text.contains("?"))
						text += ".";
					
					logger.info("utt: " + utt.getSpeaker() + ": " + text + " --- act: " + act );
					
					
					String[] words = text.split(" ");
					for(int j=0; j < words.length; j++){
						String word = utt.getSpeaker() + ": " + words[j];
						result = dlParser.parse(word);
					}

					Tree resultTree = result.getContxtalTree().clone();
					Context<DAGTuple, GroundableEdge> context = result.getContext();
					Node pointedNode = resultTree.getPointedNode().clone();
					Formula f = pointedNode.getFormula();
					
					TTRRecordType ttr = this.abstractOutSlotValues((TTRRecordType)f);
					if(act.contains("ask") || act.equals("accept"))
					logger.info(" ---> pointed Node: " + pointedNode);
					logger.debug(" ---> abstracted TTR: " + ttr);
					logger.debug(" ---> meta_replacements: " + this.meta_replacements);
					

//					logger.info("££££££££ check for the parser itself");
					if(hasSpeechActOn(pointedNode, act)){
//						logger.info("££££££££ attached with " + act);
						this.find_speech_act(resultTree.getPointedNode());
						break;
					}

					Tree newTree = null;
					List<ComputationalAction> action_list = new ArrayList<ComputationalAction>();
					for(String key: this.sa_inference_map.keySet()){
						
						String index = key.replace(act, "");
						
						if(this.isInteger(index))
							action_list.addAll(this.sa_inference_map.get(key));
					}
					
					if(action_list != null){
						// go through all existing grammars under the particular action
						for(ComputationalAction cAct: action_list){
							logger.debug("Exec sa template: " + cAct.getName());
							logger.debug("On tree:"+resultTree);
							newTree = cAct.exec(resultTree, context);
							logger.debug("result:"+newTree);
							Effect effect = cAct.getEffect();
							if(effect instanceof IfThenElse){
								((IfThenElse)effect).resetMetas();
								logger.debug("getMetas: " + ((IfThenElse)effect).getMetas());
							}
						}
					}

					if((newTree != null && !hasSpeechActOn(newTree.getPointedNode(), act)) || newTree == null){
						//cannot find any similar sa in the tree
						logger.debug("cannot find correct sa tag in the tree");
												
						// creat new Computational Action with new ttr formula
						List<ComputationalAction> actions = this.findComputationalAction(this.sa_gammar_templates, act);
						logger.debug("actions(" + actions.size() + ")");
						
						ComputationalAction selected_template = null;
						for(ComputationalAction action: actions){
							selected_template = action;
							
							// try to execute the action for particular  
							Tree tree = selected_template.exec(resultTree, context);
							
							Effect effect_template = selected_template.getEffect();
							if(effect_template instanceof IfThenElse){
								((IfThenElse)effect_template).resetMetas();
								logger.debug("getMetas: " + ((IfThenElse)effect_template).getMetas());
							}

							logger.debug("££££££££ check for the template");
							if(tree != null && hasSpeechActOn(tree.getPointedNode(), act))
								break;
							else
								continue;
						}

						if(selected_template != null){
						
							// Check whether the ttr subsumes to one of the exisiting formula using the same template
							
							List<ComputationalAction> existing_actions = this.sa_inference_map.get(selected_template.getName());
							if(existing_actions != null){
								List<ComputationalAction> newList = new ArrayList<ComputationalAction>();
								
								// go through all existing grammars under the particular action
								for(ComputationalAction cAct: existing_actions){
									Effect effect_template = cAct.getEffect();
									logger.debug("effect_template: " + effect_template);
									
									if(effect_template instanceof IfThenElse){
										Label[] if_labels = ((IfThenElse)effect_template).getIFClause().clone();
										
										for(int j=0; j < if_labels.length; j++){
											Label label = if_labels[j];
											if(label.toString().contains("W1<<")){
												TTRRecordType exist_ttr = TTRRecordType.parse(label.toString().substring(label.toString().indexOf("W1<<")+4));
												exist_ttr.resetMetas();
																								
												if(!ttr.subsumes(exist_ttr))
													newList.add(cAct);
												
//												else if (ttr.subsumes(exist_ttr) && exist_ttr.subsumes(ttr))
//													newList.add(cAct);
											}
										}
									}
								}
								this.sa_inference_map.put(selected_template.getName(), newList);
							}

						
							logger.debug("selected action template: " + selected_template.getName() + "\r\n" + selected_template.getEffect());

							if(selected_template.getName().trim().contains("ask-") || selected_template.getName().trim().contains("info-neg-")){
								logger.info("+++ prev_act = " + prev_act + "; selected action = " + selected_template.getName() );
								logger.info(selected_template.getEffect());
							}
							
							Effect effect_template = selected_template.getEffect();
							IfThenElse effect = this.addNewFormula(effect_template, ttr);
							effect = this.replaceMetaVariable(effect, this.meta_replacements);

							if(selected_template.getName().trim().contains("ask-") || selected_template.getName().trim().contains("info-neg-"))
							logger.info("new Effect at (" + act + "): \r\n" + effect);
							logger.debug("check on the action template: \r\n" + selected_template.getEffect());
							
							List<ComputationalAction> actList = new ArrayList<ComputationalAction>();
							if(this.sa_inference_map.containsKey(selected_template.getName()))
								actList = this.sa_inference_map.get(selected_template.getName());
							
							actList.add(new ComputationalAction(act, effect));
							this.sa_inference_map.put(selected_template.getName(), actList);
						}
					}
					else
						logger.debug("this action [" + act + "] has been pick up sucessfully");
					
					prev_act = act;
				}
			}
		}
		this.exportToFile(sa_inference_map, this.rescource_dir, "speech-act-inference-grammar.txt");
	}

	private void find_speech_act(Node pointedNode) {
		List<String> speech_act = new ArrayList<String>();
		if(pointedNode != null){
			for(Label l: pointedNode){
				String label_str = l.toString().trim();
				if(label_str.startsWith("sa:"))
					speech_act.add(label_str.trim());
			}
		}
		logger.debug("existing speech_act tags: " + speech_act);
	}


	private void test() throws Exception {
		String prev_act = null;
		int correctness = 0;
		int total = 0;
		
		
		Set<String> error_set = new TreeSet<String>();
		for(Dialogue dlg: this.dlgList){
			logger.debug("----------------- New Dialogue -----------------");
			// reset the dylan parser for new dialogue
			this.dlParser.initParser();
			ParseForm result = null;
			
			for(Utterance utt: dlg){
				for(int i=0; i< utt.getTotalNumberOfSegments(); i++){
					total ++;
					
//					logger.debug("utt.getUttSegment("+i+"): " + utt.getUttSegment(i));
					String text = utt.getUttSegment(i);
					text = text.replaceAll("%colorvalue", "red").replaceAll("%shapevalue", "square");
					String act = utt.getDAt(i);
					
					if(prev_act != null && prev_act.contains("polar")){
						logger.debug("--- prev_act: " + prev_act + "; current_act: "+ act);
						if(act.equals("accept")){
							act = prev_act.replace("polar", "info");
						}
						
						else if(act.equals("reject")){
							act = prev_act.replace("polar", "info-neg");
						}
						
						logger.debug("--- new current_act: "+ act);
					}
					
					if(act.contains("accept-info"))
						act = act.replace("accept-", "");

					if(act.equals("accept") && (text.contains("it is") || text.contains("we do")))
						act = prev_act.replace("polar", "info");
					
					logger.info("utt: " + utt.getSpeaker() + ": " + text + " --- act(" + act + ")");
					
					if(!text.contains("<rt>") && !text.contains(".") && !text.contains("?"))
						text += ".";
					
					logger.info("  --> " + utt.getSpeaker() + ": " + text + " :: act("+act+")");
					
					String[] words = text.split(" ");
					for(int j=0; j < words.length; j++){
						String word = utt.getSpeaker() + ": " + words[j];
						result = dlParser.parse(word);
					}

					Tree resultTree = result.getContxtalTree().clone();
					logger.debug("  --> " + resultTree.getPointedNode());

					try {
						String speech_act = null;
						
						this.exportToFile("speech_tag.txt", utt.getSpeaker() + ": " + text + " -- " + act);
						this.exportToFile("speech_tag.txt", "  --> " + resultTree.getPointedNode());
						
						if(resultTree.getPointedNode() != null){
							for(Label l: resultTree.getPointedNode()){
								String label_str = l.toString().trim();
								if(label_str.startsWith("sa:") && label_str.contains(utt.getSpeaker())){
									speech_act= label_str.trim();
									break;
								}
							}
						}
						
						this.exportToFile("speech_tag.txt", "  --> " + speech_act);
						
						// check the correctness of the tagged speech act from the parser:
						
						if(act.equals("null")){
							if(speech_act == null)
									correctness ++;
						}
						
						else{
							logger.info("act = " + act +" | speech_act = " +speech_act); 
							List<String> pattern_list = act_map.get(act);
							if (this.checkEquality(speech_act, pattern_list, act))
								correctness++;
							else{
								this.exportToFile("speech_tag.txt", "  --> ERROR!!!");
								error_set.add("act(" + act + ")" + " -- " + "speech_act(" + speech_act + ")");
							}
						}

						this.exportToFile("speech_tag.txt", "");
						
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					prev_act = act;
				}
			}
		}
		
		
		try {
			this.exportToFile("speech_tag.txt", "				Number of Utterances:  " + (double)total);
			this.exportToFile("speech_tag.txt", "			   Number of Correctness:  " + (double)correctness);
			this.exportToFile("speech_tag.txt", "Overall Accuracy of Speech Act Tags:  " + ((double)correctness/(double)total));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		int i=0;
		for(String str: error_set){
			try {
				this.exportToFile("speech_tag_error.txt", "("+(i++)+"): " + str);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void test(Dialogue dlg) throws Exception {
		String prev_act = null;

		if(dlg != null){
			// reset the dylan parser for new dialogue
			this.dlParser.initParser();
			ParseForm result = null;
			
			for(Utterance utt: dlg){
				for(int i=0; i< utt.getTotalNumberOfSegments(); i++){
					
//					logger.debug("utt.getUttSegment("+i+"): " + utt.getUttSegment(i));
					String text = utt.getUttSegment(i);
					text = text.replaceAll("%colorvalue", "red").replaceAll("%shapevalue", "square");
					String act = utt.getDAt(i);
					
					if(prev_act != null && prev_act.contains("polar")){
						logger.debug("--- prev_act: " + prev_act + "; current_act: "+ act);
						if(act.equals("accept")){
							act = prev_act.replace("polar", "info");
						}
						
						else if(act.equals("reject")){
							act = prev_act.replace("polar", "info-neg");
						}
						
						logger.debug("--- new current_act: "+ act);
					}
					
					if(act.contains("accept-info"))
						act = act.replace("accept-", "");

					if(act.equals("accept") && (text.contains("it is") || text.contains("we do")))
						act = prev_act.replace("polar", "info");
					
					
					if(!text.contains("<rt>") && !text.contains(".") && !text.contains("?"))
						text += ".";

					logger.info("utt: " + utt.getSpeaker() + ": " + text + " --- act(" + act + ")");
					
					String[] words = text.split(" ");
					for(int j=0; j < words.length; j++){
						String word = utt.getSpeaker() + ": " + words[j];
						result = dlParser.parse(word);
					}

					Tree resultTree = result.getContxtalTree().clone();
					logger.debug("  --> " + resultTree.getPointedNode());

						
					String speech_act = null;
						
					logger.info(utt.getSpeaker() + ": " + text + " :: act("+act+")");
					logger.info("  --> " + resultTree.getPointedNode());
						
					if(resultTree.getPointedNode() != null){
						for(Label l: resultTree.getPointedNode()){
							String label_str = l.toString().trim();
							if(label_str.startsWith("sa:") && label_str.contains(utt.getSpeaker())){
								speech_act= label_str.trim();
								break;
							}
						}
					}
						
					logger.info("  --> " + speech_act);
						
					prev_act = act;
				}
			}
		}
	}
	

	private boolean checkEquality(String speech_act, List<String> pattern_list, String act) throws Exception {
		if(speech_act != null && !speech_act.trim().isEmpty()){
			if(pattern_list != null && !pattern_list.isEmpty()){
				for(String pattern: pattern_list){
					if(!speech_act.contains(pattern))
						return false;
				}
				
				return true;
			}
			else 
				throw new Exception("pattern_list is unavailable for " + act);
		}
		
		return false;
	}


	private void exportToFile(HashMap<String, List<ComputationalAction>> map, String dir,
			String fileName) {
		
//		logger.info("------------ START Export SpeechAct to File -----------");
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
						
						Effect effect = action.getEffect();
						if(effect instanceof IfThenElse){
							((IfThenElse)effect).resetMetas();
							logger.debug("getMetas: " + ((IfThenElse)effect).getMetas());
						}
						
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

	private void exportToFile(String filename, String line) throws IOException{
		String path = this.dir + corpus + "/" + filename;

		File newfile = new File(path);
		FileWriter fileWriter = new FileWriter(newfile, true);
		if(!newfile.exists()){
			newfile.createNewFile(); 
			fileWriter.write(line+"\r\n");
		}
		
		else
	        fileWriter.append(line+"\r\n");

        fileWriter.close();
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
			logger.info("--- act: " + act + " -- Pointed Node: " + pointedNode);
			
			for(Label l: pointedNode){
				String label_str = l.toString().trim();
				if(label_str.startsWith("sa:")){
					label_str = label_str.replace("sa:", "");
					logger.info("--- Label String: " + label_str);
					
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
		this.meta_replacements = new HashMap<Formula, Formula>();
		
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
						logger.debug("------------------ I found something I have in af : " + af.getName() + "; Fresh Atomic Meta Variable: " + result.getFreshAtomicMetaVariable());
						Formula replaced = result.getFreshAtomicMetaVariable();
						replaced.resetMetas();
						logger.debug("------------------ replaced : " + replaced + " -- getMetas: " + replaced.getMetas());
						newF.setType(replaced);
						
						String key = entry.getKey().trim();
						key = key.equals("%colorvalue") ? "P8" : "P9";
						Formula key_f = Formula.create(key);
						meta_replacements.put(key_f, replaced);
					}
				}


			} else if (f.getType() != null && f.getType() instanceof PredicateArgumentFormula) {
				PredicateArgumentFormula paf = (PredicateArgumentFormula) f.getType();
				logger.debug("------ paf.getPredicate().getName(): " + paf.getPredicate().getName());
				
				Iterator<Entry<String, Set<String>>> iterator1 = this.slot_values.entrySet().iterator();
				while(iterator1.hasNext()){
					Entry<String, Set<String>> entry = iterator1.next();
					Set<String> values = entry.getValue();
					if (values.contains(paf.getPredicate().getName())) {
						logger.debug("------ I found something I have in paf : " + paf.getPredicate().getName() + "; Arguments : " + paf.getArguments());
						logger.debug("------ result.getFreshPredicateMetaVariable() : " + result.getFreshPredicateMetaVariable());
						Predicate meta  = result.getFreshPredicateMetaVariable();
						Formula replaced = new PredicateArgumentFormula(meta, paf.getArguments());
						replaced.resetMetas();
						logger.debug("------ replaced : " + replaced + " -- getMetas: " + replaced.getMetas());
						newF.setType(replaced);

						String key = entry.getKey().trim();
						key = key.equals("%colorvalue") ? "P8" : "P9";
						Formula key_f = Formula.create(key);
						meta_replacements.put(key_f, meta);
					}
				}
			}

			result.add(newF);
		}
		
		logger.debug("new TTR Record Type: " + result.removeHead());

		return result.removeHead();
	}

	public static void main(String[] args){
		SpeechActInferenceGenerator learner = new SpeechActInferenceGenerator("BURCHAK", null);
		
		System.out.println("IS IT READY?");
		Scanner scanInput = new Scanner(System.in);
		String comman  = scanInput.nextLine();
			
		if(comman.trim().equals("1"))
			learner.start();
		else if(comman.trim().equals("2")){
			try {
				learner.test();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if(comman.trim().equals("3")){
			List<String> curDialogue = new ArrayList<String>();
			
			// didn't solve:
			// (1)
//			curDialogue.add("sys: so a red square? <rt> -- polar-color-shape");
//			curDialogue.add("usr: no. -- info-neg-color-shape");
			
//			curDialogue.add("sys: square <rt> -- info-shape");
//			curDialogue.add("usr: no. -- reject");
			
//			curDialogue.add("sys: red square <rt> -- info-color-shape");
//			curDialogue.add("usr: close. -- reject");

			// (2)
//			curDialogue.add("usr: this object is? <rt> -- openask");
//			curDialogue.add("sys: but what is it called? <rt> -- openask");
//			curDialogue.add("usr: this is? <rt> -- openask");
//			curDialogue.add("usr: color is? <rt> -- ask-color");
			
			// (3)
//			curDialogue.add("usr: got that? <rt> -- check");
//			curDialogue.add("sys: ok <rt> -- accept");
			

			curDialogue.add("sys: red square <rt> -- info-color-shape");
			curDialogue.add("usr: close. -- reject");
			
			Dialogue dlg = new Dialogue(curDialogue);
			try {
				learner.test(dlg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if(comman.trim().equals("n"))
			System.exit(0);
		
		
//		TTRRecordType ttr1 = TTRRecordType.parse("[x24 : e|e17==eq : es|x21 : e|p44==wrong(x24) : t|pred6==color(x21) : cn|p38==attr(pred6) : t|p41==pres(e17) : t|p43==subj(e17, x21) : t|p42==obj(e17, x24) : t]");
//		TTRRecordType ttr2 = TTRRecordType.parse("[x24 : e|e17==eq : es|x21 : e|p44==wrong(x24) : t|head==e17 : es|pred6==color(x21) : cn|p38==attr(pred6) : t|p41==pres(e17) : t|p43==subj(e17, x21) : t|p42==obj(e17, x24) : t]");
//		System.out.println("subsumes: " + ttr1.subsumes(ttr2));
//		System.out.println("subsumes: " + ttr2.subsumes(ttr1));
	}
	
	
	/************************ Old Functions ************************/
	private IfThenElse replaceMetaVariable(IfThenElse original, Map<Formula, Formula> replacements) {
		original.resetMetas();
		logger.debug("this.toString().split('\n'): " + Arrays.asList(original.toString().split("\n")));
		IfThenElse new_ite = new IfThenElse(Arrays.asList(original.toString().split("\n")));
		
		new_ite.replaceVariables(replacements);
		return new_ite;
	}

	private IfThenElse addNewFormula(Effect effect, Formula f) {
		if(effect instanceof IfThenElse){
			((IfThenElse)effect).resetMetas();

			logger.debug("this.toString().split('\n'): " + Arrays.asList(((IfThenElse)effect).toString().split("\n")));
			IfThenElse new_ite = new IfThenElse(Arrays.asList(((IfThenElse)effect).toString().split("\n")));
			
			Label label1 = LabelFactory.create("Fo(W1)");
			new_ite.addNewLabelintoIF(label1);
			
			f.resetMetas();
			Label label2 = LabelFactory.create("W1<<"+f+"");
			new_ite.addNewLabelintoIF(label2);
			
			return new_ite;
		}
		return null;
	}

	private List<ComputationalAction> findComputationalAction(TreeMap<String, ComputationalAction> map, String act) {
		logger.debug("------ act : " + act);
		logger.debug("------ ComputationalAction MAP : " + map);
		
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
}
