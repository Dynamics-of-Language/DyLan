package qmul.ds.learn;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import qmul.ds.formula.Formula;
import qmul.ds.formula.PredicateArgumentFormula;
import qmul.ds.formula.ttr.TTRField;
import qmul.ds.formula.ttr.TTRRecordType;

public class CorpusConverter {

	private static Logger logger = Logger.getLogger(Corpus.class);
	public static final String corpusSourceFolder = "corpus" + File.separator + "CHILDES" + File.separator
			+ "eveTrainPairs";
	public static CorpusConverterAgenda agenda;
	public static List<String[]> corpusSource = new ArrayList<String[]>();
	public static List<String[]> missed = new ArrayList<String[]>(); // strings not converted successfully
	
	//variable pools
	public static int p = -1;
	public static int x = -1;
	public static int ev = -1;//maybe head prob, set to 0
	public static int r = -1;
	
	//regexes
	public static final String tensePredicatePattern = "^([A-P,R-Z]+)(\\()(.*\\))(\\))$"; // PAST(..																	// question?
	public static final String conjunctPredicatePattern = "^(and\\()(.*)(\\))$";
	public static final String lambdaPattern = "^(lambda[\\s])(\\$[0-9])(\\_\\{)(e|ev)(\\})$";
	public static final String variableInitPattern = "(\\()(\\$[0-9])(\\,)"; // ($2,..
	public static final String finalVariablePattern = "(.*)(\\,\\$[0-9]\\))";
	public static final String boundVariablePattern = "(.*)(\\()(.*)(\\,\\$[0-9]\\))";
	public static final String predicatePOSPattern = "^([^\\|\\,\\)\\(]*)(\\|)([^\\|\\,\\)\\(]*)(\\()(.*)(\\))$";
	public static final String predicatePOSstartPattern = "^(.*[^|])(.*\\|)(.*[^|]\\()";
	public static final String predicatePOSFinalVariablePattern = "^([^\\|\\,\\(]+)(\\|)+([^\\|\\,\\(]+)(\\()+(.*\\,)*(\\$[0-9])+(\\))+$";
	public static final String predicatePOSmultipleTypes = "^([^\\|\\,])(\\|)((\\+[^\\|\\,]*\\|[^\\|\\,]*)+)(\\()(\\$[0-9])(\\))$";
	public static final String predicatePOSmultipleTypesPred = "^([^\\|\\,])(\\|)((\\+[^\\|\\,]*\\|[^\\|\\,]*)+)(\\()(.*\\|.*)(\\))$";
	public static final String atomicPOSPattern = "^([^\\|\\,\\(\\)]*)(\\|)([^\\|\\,\\)\\(]*)$";
	public static final String duplicateTypes = "(\\+)([^\\|\\,\\+])*(\\|)([^\\|\\,\\+]*)";
	public static final String detQuantPattern = "^([^\\|\\+\\(]*)(\\|)([^\\|\\+\\(]*)(\\()(\\$[0-9])(\\,)(.+\\(\\$[0-9].*)(\\))$"; // groups
	
	
	public static String utterance; //the current utterance
	public static HashMap<String, String> variables = new HashMap<String, String>();
	public static String eventVar; //the current eventVar
	public static String internalEvent; //when using more complex event, internal event var
	public static String[] subject;
	public static String head;
	public static int eventRestrictorNode;
	public static boolean controlVerb = false;
	
	
	/**
	 * returns a string like x, x1, p, p1 etc. a fresh var for given type
	 * 
	 * @param type
	 * @return newVar or null
	 */
	public static String freshVar(String type, boolean New) {
		String newVar = null;
		if (type.equalsIgnoreCase("p") || (type.equalsIgnoreCase("t"))) {
			if (New) {
				p++;}
			newVar = p == 0 ? "p" : "p" + Integer.toString(p);
		} else if (type.equalsIgnoreCase("ev") || (type.equalsIgnoreCase("es"))) {
			if (New) {
				ev++;}
			newVar = ev == 0 ? "e" : "e" + Integer.toString(ev);
		} else if ((type.equalsIgnoreCase("x")) || (type.equalsIgnoreCase("e"))) {
			if (New) {
				x++;}
			newVar = x == 0 ? "x" : "x" + Integer.toString(x);
		} else if (type.equalsIgnoreCase("r")) {
			if (New) {
				r++;}
			newVar = r == 0 ? "r" : "r" + Integer.toString(r);
		}
		if (newVar == null) {
			logger.error("COULD NOT PRODUCE FRESH VAR FOR " + type); pause();pause();
		}
		return newVar;
	}

	public static void initVar() {
		p = -1;
		ev = -1;
		x = -1;
		r = -1;
	}

	public static void pause() {
		System.out.println("Press enter to continue...");
		try {
			System.in.read();
		} catch (Exception e) {}
	}

	public static ArrayList<String> conjunctSplit(String myString) {
		logger.info("splitting " + myString);
		ArrayList<String> conjuncts = new ArrayList<String>();
		int popped = 0;
		int splitPoint = 0;
		for (int c = 0; c < myString.length(); c++) {
			if (myString.charAt(c) == '(') {
				popped++;
			} else if (myString.charAt(c) == ')') {
				popped--;
			}
			if (myString.charAt(c) == ',' && popped == 0) {
				conjuncts.add(myString.substring(splitPoint, c));
				splitPoint = c + 1;
			} else if (c == myString.length() - 1) {
				conjuncts.add(myString.substring(splitPoint, myString.length()));
			}
		}
		if (splitPoint == 0) {
			logger.error("Could not find conjunct split point!");
			if (popped != 0) {
				logger.error("invalid expression in left conjunct! brackets : " + Integer.toString(popped));
				pause();pause();
			} else {
				logger.info("no split point, single pred? : " + myString);
			}
		}
		if (popped != 0) {
			logger.error("invalid expression in left conjunct! brackets = " + Integer.toString(popped));
			pause();pause();
		}
		System.out.println("conjuncts = " + conjuncts.toString());
		return conjuncts;
	}
	
	
	/*
	 *Different Methods for conversion //
	 * 
	 * 
	 */

	public static String questionNegation(String myString, int i){
		String pred = myString.startsWith("Q") ? "question_feature" : "not_feature";
		pred = freshVar("p", true) + "==" + pred + "(";

		// now check well-formedness, remove final bracket and $0 etc. at the end
		Pattern p = Pattern.compile(finalVariablePattern);
		Matcher m = p.matcher(myString);

		if (m.matches()) { // first match will get the $0 at end of string or no?
			String myVar = myString.substring(myString.lastIndexOf("$"), myString.length() - 1);
			if (myVar.equalsIgnoreCase(eventVar)) {
				logger.info("EVENT VAR matches, good neg/Q form:" + eventVar);
			} else {
				logger.error("MY FINAL VARIABLE IS NOT EVENT VAR.." + myString);
				pause();pause();
			}
		} else {
			logger.error("not well formed Q/NOT: " + myString);
			pause();pause();
		}

		String headType = "es"; // assume head is the internal event var, unless only noun thing left
		String internalString = myString.substring(myString.indexOf("(") + 1, myString.lastIndexOf(",$"));
		//check if remaining substring contains event var 
		//if it doesn't and should, add, else make headtype e
		if (!internalString.contains(eventVar)) { // only a 'not' event or an adjectival/nominal thing

			logger.info("I don't contain event var :" + internalString);
			p = Pattern.compile(predicatePOSPattern);
			m = p.matcher(internalString);
			if (m.matches()) {
				logger.info("adj or noun predicate!"); // should we do this for nouns too?
				if (m.group(1).startsWith("adj")) { // adjective type
					logger.info("adj predicate! " + m.group(1));
					head = internalEvent;
					headType = "es";
				} else {
					Matcher m1 = Pattern.compile(detQuantPattern).matcher(internalString);
					if (m1.matches()) {
						logger.info("detquant!" + internalString);
						head = freshVar("x", true);
						headType = "e";
					} else {
						logger.info("noun predicate!" + m.group(1)); // noun- should we make this a type t?
						head = internalEvent;
						headType = "es";
					}
				}
			} else {
				// TODO this will not cover: "(and(man(john), green(john))" cases- none in Eve
				// will only cover dets/quant phrases, pro's and and(john, mary) compounds
				logger.info("NO PREDICATE IN " + myString);
				logger.info("dem/atomic/NP:" + myString); // for now leave as pred type, could be different!
				if (!head.startsWith("x")) {
					head = freshVar("e", true);
				}
				headType = "e";
			}
			//non-eventive
			if (headType.equals("e")) {
				for (String var : variables.keySet()) {
					if (var.equals(eventVar)) {
						logger.info("removing var" + var);
						variables.remove(var);
					}
					agenda.removeNode(eventRestrictorNode);
				}
			}
		} else { // if it does contain the eventVar..
			// TODO unfort an embedded copy of this Q/not method pretty much :(
			if (internalString.startsWith("Q()") || internalString.startsWith("not(")) {
				internalString = questionNegation(internalString, i);
				logger.info(internalString);
			} else { // it's an event, any conjunction should be eventive,
				head = internalEvent;
				headType = "es";
			}
		}
		pred += head + ") : t";
		if (head.equalsIgnoreCase(internalEvent)) { // i.e. it's just binding event var
			agenda.makeResolved(agenda.addtoAgenda(myString, pred, eventRestrictorNode));
		} else { // it's an atomic question ??
			agenda.makeResolved(agenda.addtoAgenda(myString, pred, i));//?
			agenda.putTTRstring(head + ":" + headType, i); // only for type e things
		}
		myString = internalString;
		agenda.childesStrings[i] = myString; // gets rid of not/Q outer pred binding
		return myString;
	}
	
	
	public static void eqEqLoc(String myString, int i){
		// special case for eq/eqloc( , ,$0) //sometimes doesn't have final eventvar!!
		Pattern p = Pattern.compile(finalVariablePattern);
		Matcher m = p.matcher(myString);

		if (m.matches()) { // first match will get the $0 at end of string? Should do..
			logger.info("finalVariablePattern matches" + myString);
			String finalVar = myString.substring(myString.lastIndexOf("$"), myString.length() - 1);
			if (finalVar.equals(eventVar)) {
				myString = myString.substring(0, myString.lastIndexOf(",$")); // delete final var
			} else if (variables.containsKey(finalVar)) {
				logger.info("Question?" + myString);
				myString = myString.substring(0, myString.length() - 1);
			} else {
				logger.error("NO FINAL VARIABLE!");
				System.exit(0);
			}
			logger.info(myString);

		} else {
			logger.error("NO FINAL VARIABLE FOR EQ/EQLOC! FOR " + myString);
			System.exit(0);
		}

		// now find the split point (,) of the two conjuncts (pop for internal brackets)
		String pred = myString.substring(0, myString.indexOf('(')).toLowerCase(); // eq or eqloc?
		myString = myString.substring(myString.indexOf('(') + 1);
		logger.info(myString);
		String mainPred = variables.get(eventVar) + "==" + pred;
		ArrayList<String> conjuncts = conjunctSplit(myString);
		if (conjuncts.size() != 2) {
			logger.error("irregular eq size = " + conjuncts.size());
			pause();pause();
		}
		// both conjuncts should be 'e' type things in eq, might not be so luck with and()
		// 1st version is eq(x,x1)
		// 2nd version is eq(e)[e=eq:es], subj(e,x1) etc
		// VERSION WITH ARGUMENT SWAPPING for where/who is that etc..
		/*
		boolean secondVersion = true; // turn this off or on
		if (secondVersion == true) {
			mainPred += ":es";
			agenda.removeNode(eventRestrictorNode);
			// agenda.putTTRstring(null, eventRestrictorNode);
			// agenda.makeResolved(eventRestrictorNode);
		}
		String swapOrder = "";
		for (int c = 0; c < conjuncts.size(); c++) {
			String conj = conjuncts.get(c);
			boolean varBool = false;
			if (variables.containsKey(conj)) {
				varBool = true;
			}
			// adding variable if not there already
			String conj1 = varBool == true ? variables.get(conj) : freshVar("e", true); 
			int myNode = 0;
			// variables for second version:
			String argType = c == 0 ? "subj" : "obj";
			int argNode = 0;
			if (secondVersion == false) {
				myNode = agenda.addtoAgenda(conj, conj1 + ":e", i);
			} else {
				if (!swapOrder.equals("")) {
					argType = "subj"; // obj already been put there
				}
				argNode = agenda.addtoAgenda(conj, "", i); // formula will be added below
				myNode = agenda.addtoAgenda(conj, conj1 + ":e", argNode);
			}
			if (varBool == true) {
				// variable, so can be resolved right there.
				agenda.putTTRstring(null, myNode); // don't add the variable
				agenda.makeResolved(myNode);
				// for "who"/"what" the order is debatably consistently incorrect- if what is that? == who
				// is she? then conjuncts of these eq need to be swapped..
				// so if it's a question:
				if (c == 0) {
					logger.info("question variable " + conj + " first arg in eq type: " + myString);
					swapOrder = conj1;
					argType = "obj"; // object ,only applicable for second version
					if (secondVersion == false) {
						continue;
					}
				}
			}

			if (secondVersion == false) {
				mainPred += conj1;
				if (c < conjuncts.size() - 1) {
					mainPred += ",";
				} else {
					if (!swapOrder.equals("")) {
						mainPred += "," + swapOrder;
					}
					mainPred += "):t";
				}
			} else { // second version true, do this subj(, obj( etc..
				agenda.putTTRstring(freshVar("p", true) + "==" + argType + "(" + variables.get(eventVar)
						+ "," + conj1 + "):t", argNode);
			}
		}
		agenda.putTTRstring(mainPred, i);		
		*/
		
		
		//VERSION WITHOUT ARGUMENT SWAPING normally, but always puts a variable thing in first position
		boolean secondVersion = true; // turn this off or on
		if (secondVersion == true) {
			mainPred += ":es";
			agenda.removeNode(eventRestrictorNode);

		}
		
		if (variables.containsKey(conjuncts.get(1))){
			if (pred.equalsIgnoreCase("eqLoc")){
				//TODO in corpus, where always fronted
				//where always placed second, need to swap
				conjuncts.add(0, conjuncts.get(1));
				conjuncts.remove(2);		
			}else if (!utterance.endsWith("what")&&!utterance.endsWith("who")&&!utterance.endsWith("where")){
				conjuncts.add(0, conjuncts.get(1));
				conjuncts.remove(2);
			}
		}
		
		for (int c = 0; c < conjuncts.size(); c++) {
			String conj = conjuncts.get(c);
			boolean varBool = false;
			if (variables.containsKey(conj)) {
				varBool = true;
			}
			// adding variable if not there already
			String conj1 = varBool == true ? variables.get(conj) : freshVar("e", true); 
			int myNode = 0;
			// variables for second version:
			String argType = c == 0 ? "subj" : "obj";
			int argNode = 0;
			if (secondVersion == false) {
				myNode = agenda.addtoAgenda(conj, conj1 + ":e", i);
			} else {
				argNode = agenda.addtoAgenda(conj, "", i); // formula will be added below
				myNode = agenda.addtoAgenda(conj, conj1 + ":e", argNode);
			}
			if (varBool == true) {
				// variable, so can be resolved right there.
				agenda.putTTRstring(null, myNode); // don't add the variable
				agenda.makeResolved(myNode);
				if (c == 0) {
					if (secondVersion == false) {
						continue;
					}
				}
			}
			if (secondVersion == false) {
				mainPred += conj1;
				if (c < conjuncts.size() - 1) {
					mainPred += ",";
				} else {
					mainPred += "):t";
				}
			} else { // second version true, do this subj(, obj( etc..
				agenda.putTTRstring(freshVar("p", true) + "==" + argType + "(" + variables.get(eventVar)
						+ "," + conj1 + "):t", argNode);
			}
		}
		agenda.putTTRstring(mainPred, i);	
		
	}
	
	
	public static void and(String myString, int i){
		ArrayList<String> conjuncts = conjunctSplit(myString);
		String pred = "";
		boolean nouns = false; // compound nouns Jack and Jill
		boolean PUT = false; // put($0),on($0)
		String gonnaConjunct = ""; // 'going/went to tell jill', 'want to make it'
		Pattern p = Pattern.compile(predicatePOSPattern);
		Pattern predP = Pattern.compile(predicatePOSFinalVariablePattern);

		for (int c = 0; c < conjuncts.size(); c++) {

			String conjunct = conjuncts.get(c);
			Matcher m = p.matcher(conjunct);
			Matcher predM = predP.matcher(conjunct);
			logger.info("conjunct" + c + ":" + conjunct);
			Matcher predM2 = Pattern.compile(tensePredicatePattern).matcher(conjunct);

			// never have embedded and(.. by the looks
			if (predM.matches() || predM2.matches()) {
				// any more, embedded and(.. ?
				logger.info("predicatePOSFinalVariablePattern or TENSE pred MATCH:" + conjunct);
				// put these into two separate predicates for later matching, replacing the first one
				String POStype = "";
				String predName = "";
				String predBody = "";
				if (predM.matches()) {
					POStype = predM.group(1);
					predName = predM.group(3);
					if (predM.group(5) != null) {
						predBody = predM.group(5);
					} // otherwise no bodY
				} else if (predM2.matches()) {
					predName = predM2.group(1);
					predBody = predM2.group(3);
				}
				if (predName.startsWith("put") || predBody.contains("|put")) {
					PUT = true;
					logger.info("PUT type thing: " + conjunct);
				}
				boolean gonnaTryto = false;
				
				//gonna/going to is easier, as always go/part in first conjunct, use look ahead to next conjunct
				//try is a bit different as not part
				if (c==0&(conjuncts.get(1).startsWith("v")||(POStype.equals("aux")&predBody.startsWith("and(")))) {
					logger.info("control verb!");
					gonnaTryto =true;
					//TODO All adjuncts seem to be put in the second conjunct, 
				} 
					
				if (gonnaTryto == true && gonnaConjunct.equals("") && c ==0) {
					logger.info("control verb " + conjunct);
					// pause();pause();
					gonnaConjunct = conjunct;
					logger.info("gonna");
					gonnaTryto = false; //just resetting
					//pause();pause();
				} 

				if (c == 0) { // if first one isn't a gonna conjunct, just add?
					if (PUT == true) {
						agenda.childesStrings[i] = conjunct; // should this work in all cases?
						break; // i.e. don't add the second conjunct, will go to put subroutine below
					}
				} else if (c == 1) {
					if (!gonnaConjunct.equals("")) { // if there is gonna conjunct and we're in the second
						logger.info("gonna conjunct: !!" + gonnaConjunct);
						logger.info("main or control verb : " + conjunct + "on " + i);
						agenda.childesStrings[i] = conjunct;
						agenda.addtoAgenda(gonnaConjunct, freshVar("es", true) + ":es",
								agenda.parentNodeNumbers[i]);
						//add another event to the variables
						variables.put(freshVar("es",false), freshVar("es",false)); //maps to itself in case of multiple
						agenda.printState();
						// pause(); pause();
					} else { // should be a pp!
						if (POStype.startsWith("adv") || POStype.startsWith("prep")) {
							logger.info("adjunct : " + conjunct);
							agenda.addtoAgenda(conjunct, freshVar("p", true) + " : t", eventRestrictorNode);
							agenda.childesStrings[i] = conjuncts.get(0); // switch it back
						} else { //if not, could be an embedded gonna in second conjunct
							logger.info("possible control verb sentence main verb: " + conjunct);
							agenda.childesStrings[i] = conjuncts.get(0); // switch it back
							agenda.addtoAgenda(conjunct, freshVar("es", true) + ":es",
									agenda.parentNodeNumbers[i]);
							//add another event to the variables
							variables.put(freshVar("es",false), freshVar("es",false)); //maps to itself in case of multiple
							agenda.printState();
							//pause();pause();
						}
					}
				}
			} else { // pos predicate, no final variable?, should be (proper) nouny thing
				logger.info("POSPREDmatch (no final variable):" + conjunct);
				if (conjunct.startsWith("n") || conjunct.startsWith("pro")) { // we've got two more nouns																							
					if (nouns == false) {
						pred = agenda.ttrFields[i] == null ? freshVar("x", true) : agenda.ttrFields[i]
								.getLabel().toString();
						pred += "==and(";// for conjunctions of noun phrases Jack and Jill
						nouns = true;
					}
					agenda.addtoAgenda(conjunct, freshVar("x", true) + " : e", i); // each conjunct
					pred = pred + freshVar("x", false);
					pred = c < conjuncts.size() - 1 ? pred + "," : pred + ") : e"; // on last one close off
				} else if (variables.containsKey(conjunct)) {
					nouns = true; // not actually nouns but still want this to fire
					logger.info("variable binder");
					if (c == 0) {
						pred = agenda.ttrFields[i] == null ? freshVar("p", true) : agenda.ttrFields[i]
								.getLabel().toString();
						pred += "==" + "and(";
					}
					pred += variables.get(conjunct);
					pred = c < conjuncts.size() - 1 ? pred + "," : pred + ") : t";
				}
			}
		}

		if (nouns == true) {
			logger.info("CONJUNCT NOUN MATCH!");
			agenda.putTTRstring(pred, i); // puts the new compound ttr at the node
			return;
		}
		// if we get to here we've got verby stuff going on, shared predicates.
		// i.e. "on" or "in the box" etc here, leave it to later on to get the rest of the arguments
		if (PUT == true) { // always the second conjunct that is the oblique/indobj arg..
			myString = conjuncts.get(1); // assume the second conjunct is the one we want for oblique object
			// should be an adverb or preposition, either binding an event or noun phrase and event
			p = Pattern.compile(predicatePOSPattern); // need a way of getting to the irreducibility
			Matcher m = p.matcher(myString);
			// shoud be adv or prep, might have adv:loc, prep:loc? features of the restrictor?
			if (m.matches()) {
				logger.info("predicatePOSPatternMatches" + myString + " : node " + i);
				// pause();
				pred = freshVar("p", true) + "==";
				pred += "ind_obj(" + variables.get(eventVar) + ",";
				String POStype = m.group(1);
				String predName = m.group(3);
				String body = m.group(5);
				// get the conjuncts, either just event or nounphrase?
				String restrictorLabel = freshVar("r", true);

				if (predName.startsWith("put")) {
					logger.info("odd PUT pred, in second conjunct " + myString);
					// just don't process them as should have been done..
					agenda.printState();
					logger.info("conjunct in node " + i);
					return;
				}

				logger.info("MY PUT EPSILON");
				int child = agenda.addtoAgenda(myString, freshVar("es", true) + "==epsilon("
						+ restrictorLabel + "," + restrictorLabel + ".head):es",
						agenda.parentNodeNumbers[i]);
				agenda.makeResolved(child);

				pred += freshVar("ev", false) + "):t";
				child = agenda.addtoAgenda(myString, pred, agenda.parentNodeNumbers[i]);
				agenda.makeResolved(child);
				TTRRecordType putInternalRestrictor = new TTRRecordType();
				putInternalRestrictor.add(TTRField.parse(freshVar("ev", true) + ":es"));
				putInternalRestrictor.add(TTRField.parse("head==" + freshVar("ev", false) + ":es"));
				if (POStype.contains(":")) {
					putInternalRestrictor.add(TTRField.parse(freshVar("p", true) + "=="
							+ POStype.substring(POStype.indexOf(":") + 1) + "(" + freshVar("ev", false)
							+ "):t"));
				}
				pred = freshVar("p", true) + "==" + predName + "(" + freshVar("ev", false);
				// new conjuncts within the adv/prep
				// add everything else except the event variable in conjuncts of the body:
				conjuncts = conjunctSplit(body);
				boolean eventPred = false;
				if (conjuncts.get(conjuncts.size() - 1).equalsIgnoreCase(eventVar)) {
					logger.info("Event var at end of conjuncts in: " + body);
					conjuncts.remove(conjuncts.size() - 1); // get rid of last one
				} else {
					logger.error("NO FINAL EVENT VAR " + eventVar + " in:" + body);pause();
				}
				if (conjuncts.size() == 0) {
					eventPred = true; // just an event pred? p==there(e):t //could put these in the
										// restrictor with _feature..
					pred += "):t"; // e.g. there(e23)
					putInternalRestrictor.add(TTRField.parse(pred));
					agenda.makeResolved(agenda.addtoAgenda(myString, restrictorLabel + ":"
							+ putInternalRestrictor.toString(), agenda.parentNodeNumbers[i]));
					agenda.printState();
					logger.info("conjunct PUT");
					return; // that is all that's needed here or not?
				} else {
					// TODO efficiently get the event and nounphrases 
					// within the scope of the oblique object phrase
					// now into conjuncts to get the arguments
					p = Pattern.compile(detQuantPattern);
					Pattern p1 = Pattern.compile(atomicPOSPattern);

					for (int c = 0; c < conjuncts.size(); c++) { // should only be one other conjunct than
																	// the event node..?
						String conj = conjuncts.get(c);
						logger.info("CONJUNCT" + c + ": " + conj);
						m = p.matcher(conj);
						Matcher m1 = p1.matcher(conj);
						if (m.matches() || m1.matches()) {
							pred += "," + freshVar("e", true) + "):t";
							putInternalRestrictor.add(TTRField.parse(pred));
							int myRestr = agenda.addtoAgenda(myString, restrictorLabel + ":"
									+ putInternalRestrictor.toString(), agenda.parentNodeNumbers[i]);
							agenda.addtoAgenda(conj, freshVar("e", false) + ":e", myRestr);// put det on the
						}
						if (c > 0) {
							logger.error("more than 1 argument in prep phrase! in " + myString);
							logger.info(conj);
							pause();pause();
						}
					}
				}
			}
		}

	}
	
	public static void POSpred(String myString, int i){
		Pattern p = Pattern.compile(predicatePOSPattern); // need a way of getting to the irreducibility of predicates..
		Matcher m = p.matcher(myString);
		Pattern p2 = Pattern.compile(predicatePOSmultipleTypes); // +n washing +n machine etc..
		Matcher m2 = p2.matcher(myString);
		
		String pred = "";
		String POStype = ""; // n+n
		String predName = ""; // washing_machine, like, etc..
		String body = "";

		if (m.matches()) {
			POStype = m.group(1);
			predName = m.group(3);
			body = m.group(5);
		} else if (m2.matches()) {
			POStype = m2.group(1);
			predName = m2.group(3);
			Pattern p1 = Pattern.compile(duplicateTypes);
			Matcher m1 = p1.matcher(predName);
			body = m2.group(5); // not sure if this is right or not...

			while (m1.find()) {

				// could just concatenate for now.. washing_machine, take it as one word/as in corpus?
				System.out.println("%% pos:" + m1.group(2));
				System.out.println("%% pred:" + m1.group(4));
				predName += m1.group(4);

			}
		}

		// restrLabel = restrLabel.equalsIgnoreCase("") ? freshVar("r",true) : restrLabel;
		// internalEvent = internalEvent.equalsIgnoreCase("") ? freshVar("ev",true) : internalEvent;

		// eventRestrictorNode = eventRestrictorNode == -2 ?
		// agenda.addtoAgenda("",restrLabel+":["+internalEvent+":es|head : "+internalEvent+"]",agenda.newNode)
		// : eventRestrictorNode;

		if (POStype.startsWith("v") || POStype.startsWith("aux") || POStype.startsWith("part")
				|| POStype.startsWith("adv") || POStype.startsWith("prep")) {
			pred += predName;

			// every thing except verb in restrictor, or not? I think this is ok, all restricting the event
			// time/or refttime before main clause- only verbs can be main clauses?
			boolean modify = true;
			String splitSymbol = "";
			if (pred.contains("-")) {
				splitSymbol = "-";
			} else if (pred.contains("&")) {
				splitSymbol = "&";
			}

			String boundEvent = internalEvent;  //I think this is always the case, don't change it
			//TODO means that try to give 'try' the head.
			//only for adverbs, as these link off the controlled verb:
			//for (String var : variables.keySet()){
			//	if (variables.get(var).startsWith("e")&&!variables.get(var).equals(internalEvent)){
			//		boundEvent = variables.get(var);
			//		logger.info("switching boundEvent to " + boundEvent);
			//		//pause();pause();
			//	}
			//}
			if (!splitSymbol.equals("")) {
				// for tense/aspect/agreement features
				logger.info("matching -:" + m.group(3));
				int ag = agenda.addtoAgenda(
						myString,
						freshVar("p", true)
								+ "=="
								+ myString.substring(myString.indexOf(splitSymbol) + 1,
										myString.indexOf("(")) + "_feature(" + boundEvent + ") : t",
						eventRestrictorNode);
				agenda.makeResolved(ag);
				pred = pred.substring(0, pred.indexOf(splitSymbol));

			}

			if (POStype.equals("aux")) {
				pred = freshVar("p",true) + "==" + pred +  "_" + POStype + "_feature(" + boundEvent + "):t"; // be/will etc... _feature
																				// means it'll be caught in
																				// eventRestrictor
				int ag2 = agenda.addtoAgenda(myString, pred, eventRestrictorNode);
				agenda.makeResolved(ag2);
				body += ")";
				p = Pattern.compile(finalVariablePattern);
				m = p.matcher(body);
				if (m.matches()) {
					logger.info("FINALVARIABLEPATTERN MATCH body: " + body);
					myString = m.group(1);
					agenda.putTTRstring(freshVar("t", true) + ":t", i);
					agenda.childesStrings[i] = myString;
					return; // or break?
				} else {
					logger.error("unknown pred in aux body : " + body);
				}

			} else {
				// should be a part?, these apart from gonna, should be main verbs, gonna treat like a
				// control verb for now.
				// pred +="("+variables.get(eventVar);
			}

			// add everything else except the event variable in conjuncts of the body:
			List<String> conjuncts = conjunctSplit(body);
			boolean eventPred = false;

			if (conjuncts.get(conjuncts.size() - 1).equalsIgnoreCase(eventVar)) {
				logger.info("Event var at end of conjuncts in: " + body);
				conjuncts.remove(conjuncts.size() - 1); // get rid of last one
			} else {
				logger.error("NO FINAL EVENT VAR " + eventVar + " in:" + body);
				pause();
			}

			if (conjuncts.size() == 0) {
				eventPred = true; // just an event pred? p==there(e):t //could put these in the restrictor
									// with _feature..
			}

			// add conjuncts (not event var), if doesn't match variables, then add to agenda
			// neo-davisonian p==pred(e) : t \ first arg= p==subj(e,x) :t \second arg p==obj(e,x1) : t\
			// third arg (give) p=indobj(e,x2) :t

			if (POStype.startsWith("adv") || POStype.startsWith("prep")) {
				//only for adverbs, as these link off the controlled verb:
				for (String var : variables.keySet()){
					if (variables.get(var).startsWith("e")&&!variables.get(var).equals(internalEvent)){
						boundEvent = variables.get(var);
						logger.info("switching boundEvent to " + boundEvent);
						//pause();pause();
					}
				}
				logger.info("adv or prep match : " + myString);
				// change father to eventrestrictor
				agenda.parentNodeNumbers[i] = eventRestrictorNode;
				if (eventPred == true) {
					logger.info("event pred is true for prep/adv " + pred);
					pred = freshVar("p", true) + "==" + pred + "(" + boundEvent + "):t"; // this will be caught later on in clean up of
															// eventRestrictor
					// eventRestrictor.add(TTRField.parse(pred)); //add it directly to event Restrictor?
					// What about "with John(e)/on Friday(e)"

					agenda.putTTRstring(pred, i);
					agenda.makeResolved(i);
					agenda.printState();
					// pause();pause();
					return;
				} else {
					// adding to the event restrictor by making them children of the event restrictor node
					pred = freshVar("p",true) + "==" + pred + "(" + boundEvent + ",";

					for (int c = 0; c < conjuncts.size(); c++) {
						String conj = conjuncts.get(c);
						logger.info("CONJUNCT" + c + ": " + conj);

						if (variables.containsKey(conj) && conjuncts.size() > 1) { // eek, could scope back
																					// over previous term or
																					// bound variable 'John
																					// ran, with himself.'
							logger.error("possible problem with adjunct linking out to variable, reflexive?"
									+ myString);
							pause();pause();
							pred += variables.get(conj) + "):t"; // add the variable name
							agenda.makeResolved(agenda.addtoAgenda(conj, pred, eventRestrictorNode));
							// can resolve as variable only argument
						} else {
							pred += freshVar("e", true);
							// int conjInt = agenda.addtoAgenda(conj, pred, i);
							agenda.addtoAgenda(conj, freshVar("e", false) + ":e", eventRestrictorNode); 

						}

						if (c < conjuncts.size() - 1) {
							pred += ",";
						} else {
							pred += "):t";
						}

					}
					agenda.putTTRstring(pred, i);
					agenda.makeResolved(i); // its constituents may not be resolved yet though, as they need
											// to on eventRestrictorNode

					// put the conjuncts in the pred with commas
					// put the conjuncts on daughter nodes, these should resolve with the restrictor
					// agenda.addtoAgenda(myString,
					// restrLabel+":["+internalEvent+":e|head :"+internalEvent+"|"+pred+"]",
					// eventRestrictorNode);
					// this should be being done anyway..

				}
			} else { // should be a verb, normal pred!

				String mainpred = "==" + pred + ":es";
				boundEvent = internalEvent; //this may change if control verb
				
				boolean subjectCopy = false;
				// now into conjuncts, each arg you add a new field with subj, obj etc..
				for (int c = 0; c < conjuncts.size(); c++) {
					String conj = conjuncts.get(c);
					logger.info("CONJUNCT" + c + ": " + conj);
					pred = freshVar("p", true) + "==";
					if (c == 0) {
						pred += "subj("; // doesn't add a second time if already there.. try to/gonna 
						//though 'want me to' is different
						if (subject[0]==null) {
							subjectCopy = true; // if first time subject, make a copy of it..

						} else { // change the event, as we're in a control sit.. gonna/try to
							controlVerb = true;
							logger.info("control, changing boundEvent");
							for (String var : variables.keySet()) {
								if (variables.get(var).startsWith("e")&&!variables.get(var).equals(internalEvent)){
									boundEvent = variables.get(var);
								}
								System.out.println(var + ": " + variables.get(var));
							}
							// as these normally resolved last, will work
							if (boundEvent.equals(internalEvent)){
							
								//internalEvent = freshVar("es", true);
								variables.put(freshVar("es", true),freshVar("es", false));
								boundEvent = freshVar("es",false);
								//TODO tries to/gonna always have head as try/gonna, kind of correct..
							}
							// could change the head, or not..
							//head = boundEvent;
							for (String var : variables.keySet()) {
								System.out.println(var + ": " + variables.get(var));
							}
							//pause();pause();
						}
					} else if (c == 1) {
						pred += "obj(";
						//TODO could do with copying objects too for "Do you want it to eat ?"
					} else if (c == 2) {
						pred += "ind_obj(";
					} else {
						logger.error(myString + " has over 3 arguments!");
					}

					pred += boundEvent + ","; //either main one or control one
					if (variables.containsKey(conj) && conjuncts.size() > 1) {
						pred += variables.get(conj) + "):t"; // add the variable name
						int conjInt = agenda.addtoAgenda(conj, pred, i);
						agenda.makeResolved(conjInt); // can resolve as variable only argument
					} else {
						logger.info("subject = " + subject);
						boolean variable = false;
						String eArg = "";
						if (variables.containsKey(conj)) {
							eArg = variables.get(conj);
							variable = true;
						} else {
							if (c == 0) {
								eArg = (subject[0]==null||!subject[0].equals(conj)) ? freshVar("e", true) : subject[1];
							} else {
								eArg = freshVar("e", true);
							}
						}

						pred += eArg + "):t";
						int conjInt = agenda.addtoAgenda(conj, pred, i);
						if ((c == 0) && (subject[0]!=null&&conj.equals(subject[0]))) {
							agenda.makeResolved(conjInt);
						} else {
							if (variable == false) {
								agenda.addtoAgenda(conj, eArg + ":e", conjInt);
							} else {
								agenda.makeResolved(conjInt);
							}
						}
						subject[0] = subjectCopy==true ? conj : subject[0];
						subject[1] = subjectCopy==true ? eArg : subject[1];
						subjectCopy = false;
					}
				}// end of arg loop

				mainpred = boundEvent + mainpred; // will change pred if needs be
				agenda.removeNode(eventRestrictorNode);
				logger.info("\n MAIN VERB POS PRED TYPE ADDING!! " + mainpred.toLowerCase());
				agenda.putTTRstring(mainpred, i); // need the prep with the component parts?
				agenda.makeResolved(i);
			}

		} else if (m.group(1).startsWith("adj") || m.group(1).startsWith("n")) {
			// predictival adjectives/nominals now, determiners should be already be caught in above
			// templates
			logger.info("adjectival or nominal pred match!"); // should we add event term or not?
			// going for eventive adjectives with no tense?
			if (eventVar.equals("")) {
				eventVar = "$dummy";
				variables.put(eventVar, internalEvent);
			}
			
			if (m.group(1).startsWith("adj")){
				pred = freshVar("p",true) + "==" + m.group(3) + "(" + variables.get(eventVar) + "):t"; // p2=green(e) :t
				//pred = variables.get(eventVar) + "==" + m.group(3) + ":es";
			} else { //noun, use equality instead es==eq, p==subj(e,x) and p==obj(e,x1) where x is m.group(3)!
				pred = variables.get(eventVar) + "==" + "eq" + ":es";
				agenda.removeNode(eventRestrictorNode);
				int myObj = agenda.addtoAgenda(m.group(3), freshVar("p",true) + "==obj(" + variables.get(eventVar) + ","+ freshVar("e",true) +")"+ ":t", i);
				agenda.addtoAgenda(m.group(1)+"|"+m.group(3), freshVar("e",false)+":e", myObj);
				//should always be a mass noun like sugar/home etc..
			}
			agenda.putTTRstring(pred, i);
			// this should become resolved once daughters are through arg, below:

			String arg = "";
			if (variables.containsKey(m.group(5))) {
				arg = variables.get(m.group(5)); // should never be event term, this is added above
			} else {
				arg = freshVar("e", true);
				agenda.addtoAgenda(m.group(5), arg + ":e", i);
			}

			int subjInt = agenda.addtoAgenda(m.group(5),
					freshVar("p", true) + "==subj(" + variables.get(eventVar) + "," + arg + "):t", i);
			agenda.makeResolved(subjInt);
			head = internalEvent;

		} else {
			logger.error("unknown pred type in " + m.group(1));
			pause();pause();
		}
	
	}
	
	
	public static void detQuant(String myString, int i){
		Pattern p = Pattern.compile(detQuantPattern);
		Matcher m = p.matcher(myString);
		m.matches(); //we know it does already
		
		String myrestrictor = freshVar("r", true) + ":[";
		String myDetTTR = (agenda.ttrFields[i] == null || agenda.ttrFields[i].getLabel().toString()
				.startsWith("p")) ? freshVar("e", true) + "==" : agenda.ttrFields[i].getLabel() + "==";
		String pred = "";
		if (m.group(3).equalsIgnoreCase("a")) {
			pred = "epsilon";
		} else if (m.group(3).equalsIgnoreCase("the")) {
			pred = "iota";
		} else if (m.group(3).equalsIgnoreCase("all") || m.group(3).equalsIgnoreCase("every")
				|| m.group(3).equalsIgnoreCase("each")) {
			pred = "tau";
		} else {
			pred = m.group(3);
		} // generalized quantifier approach? the/a/all/most..
		myDetTTR += pred + "(" + freshVar("r", false) + ".head, " + freshVar("r", false) + ") : e";
		String myVariable = "";
		if (variables.containsKey(m.group(5))) {
			myVariable = variables.get(m.group(5));
		} else {
			logger.error("no variable for %%" + m.group(5) + "%%");
			for (String var : variables.keySet()) {
				logger.info(var + " : " + variables.get(var));
			}
		}

		if (myVariable.equals("")) {
			logger.error("NO VARIABLE FOR QUANT/DET! %%" + m.group(5) + "%%%");
			System.out.println(myString);
			pause(); pause();
			variables.put(m.group(5), freshVar("e", true));
			myVariable = variables.get(m.group(5));
			// System.exit(0);
		}
		myrestrictor += myVariable + " :e|head==" + myVariable + ":e"; // find binding statements(s) below
		logger.info("removing variable " + m.group(5));
		variables.remove(m.group(5));// get rid of it as internally bound?
		
		String body = m.group(7);

		// check for conjunction and(...,...) type
		if (body.startsWith("and(")) {
			body = body.substring(body.indexOf("(") + 1, body.length() - 1);
		}
		logger.info(body);

		// we need to iterate over possible conjuncts in body- yes, adj + n etc..
		for (String conjunct : body.split(",")) {
			p = Pattern.compile(predicatePOSmultipleTypes); // +n washing +n machine etc..
			m = p.matcher(conjunct);
			if (m.matches()) { // +n
				System.out.println("Multiple head = " + m.group(1));
				conjunct = m.group(3);
				p = Pattern.compile(duplicateTypes);
				m = p.matcher(conjunct);
				// put the variable $1 (e3) etc.. in the record type, unique? guess it's got to, but more
				// important that r is unique!
				// now get each part of the epsilon restrictor
				// these are restricted within epsilon restrictor, r: [p:] both of type t
				myrestrictor += "|" + freshVar("p", true) + "==";
				while (m.find()) {

					// could just concatenate for now.. washingmachine, take it as one word/as in corpus?
					System.out.println("%% pos:" + m.group(2));
					System.out.println("%% pred:" + m.group(4));
					myrestrictor += m.group(4);

				}
				// finish restrictor
				myrestrictor += "(" + myVariable + ") : t"; // head type could change?
				logger.info(myrestrictor);

			} else { // should just be normal type

				p = Pattern.compile(predicatePOSPattern);
				m = p.matcher(conjunct);
				if (m.matches()) {
					System.out.println("normal pos");
					System.out.println(m.group(1)); // n or adj
					System.out.println(m.group(3)); // the actual binder
					myrestrictor += "|" + freshVar("p", true) + "==" + m.group(3) + "(" + myVariable
							+ ") : t";
					System.out.println(m.group(5));// should be variable?
				}
			}

		}
		myrestrictor += "]";
		// should now have restrictor and det binder, add to ttr
		agenda.putTTRstring(myDetTTR.toLowerCase(), i);
		agenda.makeResolved(i); // i.e. we should be able to exhaustively resolve binder and internal
								// structure from above??
		int restrInt = agenda.addtoAgenda(body, myrestrictor.toLowerCase(), agenda.parentNodeNumbers[i]);
		agenda.makeResolved(restrInt);
		
	}
	
	

	/**
	 * Converts CHILDES formula to TTR string
	 * 
	 * @param childes
	 * 
	 */
	public static String TTRconvert(String origChildes, String myUtt) {
		String childes = origChildes;
		agenda = new CorpusConverterAgenda();
		utterance = myUtt;
		eventVar = "";
		initVar();
		String ttr = "";// only for final output
		controlVerb = false;
		subject = new String[2];
		variables = new HashMap<String, String>();
		// List<TTRField> eventRestrictor = new ArrayList<TTRField>();
		// String eventrestrLabel = freshVar("r", true); //turn on or off dep. on internal event..
		internalEvent = freshVar("ev", true); // may not be used, but always initiated
		eventRestrictorNode = 1000; // dummy value, will get changed later if there is an event term here

		// get lambdas first, if extant
		Pattern p = Pattern.compile(lambdaPattern);
		Matcher m = null;
		if (childes.startsWith("lambda")) {
			for (int i = 0; i < childes.split("\\.").length - 1; i++) { // goes through each lambda abstract until body
				String sub = childes.split("\\.")[i];
				m = p.matcher(sub);
				while (m.find()) {
					String variable = m.group(2).trim(); // should be $0 etc..
					String type = m.group(4).trim(); // should be e or ev?
					variables.put(variable, freshVar(type, true)); // adds variable number, type, ttrvar triple
					if (type.equalsIgnoreCase("e")) { // we have a question
						variables.put("", freshVar("p", true) + "==question_feature(" + freshVar("e", false) + ")");
					}
				}
			}
			childes = childes.split("\\.")[childes.split("\\.").length - 1]; // removes lambdas from string
		}

		// now dealing with non lambda declared variables, i.e. in the body.
		// May have lambda variables stored from above algorithm
		// might still be further variables for internally declared ones ($2,n|table($2)), need to store these too..
		p = Pattern.compile(variableInitPattern); // for internally declared variables
		m = p.matcher(childes);
		// puts internal variables in there..
		internal: while (m.find()) {
			String variable = m.group(2).trim();
			for (String var : variables.keySet()) {
				if (var.equalsIgnoreCase(variable)) {
					continue internal;
				}
			}
			variables.put(variable, freshVar("e", true)); // adds variable number, type, ttrvar triple
		}

		// should now have all variables, declared in lambdas, or internally, 
		head = ""; // head gets instantiated as below, but if eventRest
		// get eventVar first, should be one per string
		for (String var : variables.keySet()) {
			if (variables.get(var).startsWith("e")) {
				if (!eventVar.equalsIgnoreCase("")) {
					logger.error("MORE THAN ONE EVENT VAR!!!");
					pause();pause();
				}
				eventVar = var;
			}
		}

		if (eventVar == "") {
			m = Pattern.compile(detQuantPattern).matcher(childes);
			Matcher m1 = Pattern.compile(atomicPOSPattern).matcher(childes);
			if (m.matches() || m1.matches()) {
				// TODO won't get and(man(john),green(john)- though none in CHILDES..
				logger.info("atomic or det/quant main bod, type e headed utt:" + childes);
			} else {
				logger.info("adj/nom predicate:" + childes);
				// could do this further down
				eventVar = "$dummy"; //creates dummy
				variables.put(eventVar, freshVar("ev", true));
				internalEvent = variables.get(eventVar);
				eventRestrictorNode = agenda.addtoAgenda(origChildes, internalEvent + ":es", agenda.newNode);
				head = internalEvent;
			}
		} else {
			// can make the internal event var same as external in one version:
			internalEvent = variables.get(eventVar);
			eventRestrictorNode = agenda.addtoAgenda(origChildes, internalEvent + ":es", agenda.newNode);
			head = internalEvent;
		}

		// or we can do it via embedded restrictor
		/*
		 * // event restrictor node can be resolved either when all daughters resolved // OR if no daughters, when
		 * removed the rest is complete! eventRestrictorNode = agenda.addtoAgenda(origChildes, eventrestrLabel + ":[" +
		 * internalEvent + ":es|head : " + internalEvent + "]", agenda.newNode); //
		 * agenda.resolved(eventRestrictorNode);
		 */
		// pause();pause();
		logger.info("variables:");
		for (String var : variables.keySet()) {
			logger.info(var + "=" + variables.get(var));
		}
		logger.info("head=" + head);
		// main loop for converting the remainder of CHILDES string
		while (!(childes.length() == 0 && agenda.isComplete() == true)) { // needs to reduce all of childes and pop the
																			// whole agenda?
			// these initial things done without agenda?
			logger.info("\n WHILE LOOP:");
			logger.info("orig childes = " + origChildes);
			logger.info("childes = " + childes);
			logger.info("\n VARIABLES:");
			for (String var : variables.keySet()) {
				System.out.println(var + "=" + variables.get(var));
			}
			logger.info("head=" + head);
			agenda.printState();

			if (childes != "") {
				// if we have childes stuff left
				if (eventRestrictorNode == 1000) {
					logger.info("should have no event node " + origChildes);
					agenda.addtoAgenda(childes, freshVar("x", true) + ":e", agenda.newNode);
					head = freshVar("x", false); // sets the head as an np
				} else {
					agenda.addtoAgenda(childes, freshVar("p", true) + ":t",
							agenda.parentNodeNumbers[eventRestrictorNode]);
					head = internalEvent;
				}
				// make rest of predicate sister to the eventRestrictorNode
				childes = "";
				continue;

			}

			// now everything should be on agenda, no childes string left.
			// now iterate through nodes for matching unresolved strings
			for (int i = agenda.newNode; i >= 0; i--) {
				// check to see if it's resolved or not, or check daughters and resolve
				String myString = agenda.childesStrings[i];
				System.out.println("mystring : " + myString);
				System.out.println("node:" + i);
				agenda.printNode(i);
				
				if (agenda.resolved(i) == false) {
					if (i == eventRestrictorNode) {
						break;
					}
					if (agenda.allDaughtersResolved(i)&&agenda.ttrFields[i].getType()!=null || agenda.ttrFields[i].getLabel().toString().startsWith("r")) {
						if (agenda.ttrFields[i].getLabel().toString().startsWith("r")) { 									
							logger.info("RESOLVING RESTRICTOR");
							agenda.resolveRestrictor(i); 
						}
						logger.info("RESOLVED NODE:" + i);
						// pause();pause();
						agenda.makeResolved(i);
						continue;
					}
				} else {
					continue; // resolved node, carry on
				}

				// possible predicates to apply exhaustively:
				// (1) NOT( or Q(
				// (2) eq( or eqLoc(
				// (3) tense PAST(.......) //needs to be changed so it binds event VAR
				// (4) conjunction and(... ,... ), these could also be embedded // and() is special in that if conjuncts
				// of nounphrases, needs to be x3={x1,x2}
				// (5) det/quant type
				// (6) postype...¦..pred...(....,....,) //this will be the normal case, two or three placed preds..
				// (7) pro|john etc. atomic types
				// (8) $0 a variable in the variables
				// (9) n+/n+ atom

				// if no eventVar declared, should be added in 'that's sugar' type examples, hopefully would've happened
				// before

				// (1) Q or negation
				if ((myString.startsWith("Q(")) || (myString.startsWith("not("))) {
					logger.info("QUESTION OR NEGATION MATCH!: " + myString);
					questionNegation(myString, i);
					break;
				}

				// (2) eq/eqloc
				if (myString.startsWith("eq")) {
					logger.info("eq or eqloc MATCH!: " + myString);
					eqEqLoc(myString, i);
					break;
				}

				// (3) PAST(..)
				p = Pattern.compile(tensePredicatePattern);
				m = p.matcher(myString);
				if (m.matches()) {
					logger.info("TENSE MATCH:" + myString);
					String pred = m.group(1); // Should be PAST
					String predHead = freshVar("p", true);
					agenda.makeResolved(agenda.addtoAgenda(myString, predHead + "==" + pred + "_feature" + "("
							+ internalEvent + ") : t", eventRestrictorNode));
					myString = m.group(3); // reduce myString to inner string of just the predicate stuff without tense
					agenda.childesStrings[i] = myString; // put it on the agenda
					break;
				}

				// (4) and( , ) conjunction, more complicated as could have different types of conjunct, i.e. p/adverbs
				// or e, noun compounds
				p = Pattern.compile(conjunctPredicatePattern);
				m = p.matcher(myString);
				if (m.matches()) {
					logger.info("CONJUNCTION MATCH:" + myString);
					myString = m.group(2);// gets the X from and(X)
					and(myString, i);
					break;
				}

				// (5) Now for dets/quant predicates- espilon restrictor
				p = Pattern.compile(detQuantPattern);
				m = p.matcher(myString);
				if (m.matches() && !m.group(1).contains("v") && !m.group(1).contains("part")
						&& !m.group(1).contains("aux") && !m.group(1).contains("adv") && !m.group(1).contains("prep")) { // no
					logger.info("matches detQuant:" + myString);
					detQuant(myString, i);
					break; // why continue?
				}

				// (6) Normal predicate verb/aux/prep/adv or adj/n, with possibly more than one arg inside..
				p = Pattern.compile(predicatePOSPattern); // need a way of getting to the irreducibility of predicates..
				m = p.matcher(myString);
				Pattern p2 = Pattern.compile(predicatePOSmultipleTypes); // +n washing +n machine etc..
				Matcher m2 = p2.matcher(myString);
				if (m.matches() || m2.matches()) {
					logger.info("predicatePOSPatternMatches" + myString + " : node " + i);
					POSpred(myString, i);
					break;
				}

				// (7) atomic units dem/pro etc. // all variable refs should have been removed?//these should be type t?
				p = Pattern.compile(atomicPOSPattern);
				m = p.matcher(myString);
				if (m.matches()) {
					logger.info("ATOMIC MATCH :" + myString + i);
					String xish = freshVar("e", true);
					if (agenda.ttrFields[i] != null && agenda.ttrFields[i].getLabel().toString().startsWith("x")) {
						xish = agenda.ttrFields[i].getLabel().toString();
					}
					agenda.putTTRstring(xish + "==" + m.group(3).toLowerCase() + ":e", i);
					logger.info(xish + "==" + m.group(3).toLowerCase() + ":e");
					logger.info(TTRField.parse(xish + "==" + m.group(3).toLowerCase() + ":e"));
					agenda.makeResolved(i);
					// pause();pause();
					break;
				}

				// (8) $0 raw variable, clean up?
				if (myString.startsWith("$")) {
					logger.info("should be event var " + myString);
					if (variables.containsKey(myString)) {
						String varType = variables.get(myString).startsWith("ev") ? "es" : "e";
						// agenda.putTTRstring(variables.get(myString) + ":" + varType, i); //should add this variable?
						agenda.putTTRstring("", i); // no variable there as will be included in final adding if event/else removed
						agenda.makeResolved(i);
						break;
					} else {
						logger.error("no matching var in " + myString);
						pause(); pause();
					}
					break;
				}

				// (9) Could be n+n predicate
				if (myString.startsWith("n|+n")) {
					logger.info("match n+n+");
					String pred = "";
					String predName = "";
					//NB actually doesn't do atoms
					p = Pattern.compile(predicatePOSmultipleTypesPred);
					m = p.matcher(myString);
					if (m.matches()) { // +n
						System.out.println("Multiple head = " + m.group(1));
						String body = m.group(3);
						String arg = myString.substring(myString.indexOf("(")+1, myString.lastIndexOf(")"));
						logger.info("n+|n+ nominal pred match!"); // should we add event term or not?
						// going for eventive adjectives with no tense?
						if (eventVar.equals("")) {
							eventVar = "$dummy";
							variables.put(eventVar, internalEvent);
						}
						
						p = Pattern.compile(duplicateTypes);
						m = p.matcher(body);
						//m.matches();
						// put the variable $1 (e3) etc.. in the record type, unique? guess it's got to, but more
						while (m.find()) {
							// could just concatenate for now.. washingmachine, take it as one word/as in corpus?
							logger.info("%% pos:" + m.group(2) + "/n" + "%% pred:" + m.group(4));
							predName += m.group(4);
						}
						// these will be eventive args..
						pred = variables.get(eventVar) + "==" + "eq" + ":es";
						agenda.putTTRstring(pred, i);
						agenda.makeResolved(i);
						agenda.removeNode(eventRestrictorNode);
						int myObj = agenda.addtoAgenda(myString, freshVar("p",true) + "==obj(" + variables.get(eventVar) + ","+ freshVar("e",true) +")"+ ":t", agenda.parentNodeNumbers[i]);
						agenda.makeResolved(agenda.addtoAgenda(myString, freshVar("e",false)+"=="+predName+ ":e", myObj));
						int mySubj = agenda.addtoAgenda(myString, freshVar("p",true) + "==subj(" + variables.get(eventVar) + ","+ freshVar("e",true) +")"+ ":t", agenda.parentNodeNumbers[i]);
						agenda.addtoAgenda(arg, freshVar("e",false)+":e", mySubj);
						//should always be a mass noun like sugar/home etc..
						//agenda.addtoAgenda(arg, freshVar("e", false) + ":e", i);
					}
					break;
				}
			}
			// try to resolve event restrictor node each cycle, if present
			if (eventRestrictorNode != 1000 && agenda.ttrFields[eventRestrictorNode] != null) {
				agenda.resolveEventRestrictor(eventRestrictorNode);
			}
			logger.info("\nVARIABLES:");
			for (String var : variables.keySet()) {
				System.out.println(var + "=" + variables.get(var));
			}
			logger.info("head is " + head);
			agenda.printState();
			//pause();pause();
		}

		// if there is contentful instantiation
		List<TTRField> ttrs = agenda.resolvedTTRs();
		System.out.println("FINAL STATE before variables added: ");
		agenda.printState();

		// add event variables, change to restrictor type for _feature(ev)- this should now all be done above
		// need to do this through out really, as we might bind the event term before
		// adding all variables except event term? This will be question(x) x:e etc..
		for (String var : variables.keySet()) {
			System.out.println(var + "=" + variables.get(var));
			ttr = variables.get(var);
			String dstype = null;
			String type = null;
			String label = null;
			if (ttr.startsWith("e")) { // should only be one event var
				// restrictor with internal event
				// label = ttr + "==epsilon(" + restrLabel + ".head," + restrLabel + ")";
				// dstype = "es";
				continue;
				// }
			} else if (ttr.startsWith("x")) {
				label = ttr;
				dstype = "e";
			} else if (ttr.startsWith("p")) {
				dstype = "t";
				if (ttr.contains("==")) {
					label = ttr.substring(0, ttr.indexOf('='));
					type = ttr.substring(ttr.indexOf("==") + 2);
					label += "==" + type;
				}
			}
			ttrs.add(TTRField.parse(label + ":" + dstype));
		}
		String headType = head.startsWith("x") ? "e" : "es"; // add head
		//TODO hack for now removing all question_feature, and one_s_ three_s_ etc. then making x=what/who/where
		//get the "what's what" type things..
		//replacing lone x:e etc with x=what, x=who etc.., could be two in what's what..
		String[] utt = utterance.split("\\s+");
		List<String> whWords = new ArrayList<String>();
		for (String word : utt){
			if (word.equalsIgnoreCase("what")||word.equalsIgnoreCase("who")||word.equalsIgnoreCase("where")){
				whWords.add(word);
			}
		}
		int tLength= 0;
		int mySize = 1;
		while (tLength<ttrs.size()){
			mySize = ttrs.size();
			for (int f=0; f<mySize; f++){
				System.out.println("f" + f);
				System.out.println(ttrs.size());
				System.out.println(ttrs.get(f));
				if ((ttrs.get(f).getType()!=null)&&(ttrs.get(f).getType().toString().contains("_feature"))){
					Formula mytype= ttrs.get(f).getType();
					if (mytype.toString().startsWith("question_")||mytype.toString().contains("zero")||mytype.toString().contains("one")||
							mytype.toString().contains("two")||mytype.toString().contains("three")){
						ttrs.remove(f);
						if (mytype.toString().startsWith("question_")){
							List<Formula> args = ((PredicateArgumentFormula) mytype).getArguments();
							//go through in order of possible wh's and remove
							if (args.get(0).toString().startsWith("e")){
								break;//don't delete event, otherwise carry on
							}
							int t= 0;
							for (TTRField myfield : ttrs){ //loop within loop to find correct label
								if (myfield.getLabel().toString().equals(args.get(0).toString())&&myfield.getType()==null){
									myfield = TTRField.parse(myfield.getLabel().toString() + "==" + whWords.get(0) + ":e");
									whWords.remove(0);
									ttrs.add(myfield);
									ttrs.remove(t);
									break;
								}
								t++;
							}
						}
						break;
					} 
					else if (!(mytype.toString().contains("_aux_"))&&!(mytype.toString().contains("not_"))){
						ttrs.remove(f);
						break;
					}
				}
			 	tLength=f+1;
			}
		}
		
		//final sweep of lone x :e, in theory should have some whWords left each time..
		for (int f=0; f<ttrs.size(); f++){
			TTRField myfield = ttrs.get(f);
			if (myfield.getType()==null){
				if (myfield.getDSType()!=null&&myfield.getDSType().toString().equals("e")){
					myfield = TTRField.parse(myfield.getLabel().toString() + "==" + whWords.get(0) + ":e");
					ttrs.remove(f);
					ttrs.add(myfield);
					break;
				}
			}
		}
		
		//TODO make more principalled- want lone adverbs to not have a type es var, just "head"
		boolean loneadverb = true;
		for (TTRField myfield : ttrs){
			if (myfield.getType()!=null&&myfield.getType().toString().startsWith("subj(")){
				loneadverb = false; break;
			}
		}
		if (head.startsWith("x")){
			loneadverb=false; //single noun phrases
		}
		if (loneadverb==true){
			logger.info("loneadverb");
			List<TTRField> newttrs = new ArrayList<TTRField>();
			for (int t = 0; t<ttrs.size(); t++){
				//replace all instances of the head say e1 with 'head'
				String myfield = ttrs.get(t).toString();
				if (myfield.contains(head)){
					myfield = myfield.replace(head, "head");
				}
				System.out.println(myfield);
				newttrs.add(TTRField.parse(myfield));
			}
			ttrs = newttrs;
			ttr = agenda.sort(ttrs,false).toString(); //head needs to be above
		}
		else { //normally just add
			ttrs.add(TTRField.parse("head==" + head + ":" + headType)); // turn this off an on
			if (controlVerb==true){

				//TODO could add a goal(e1,e4)
				//TODO hack for now, changing to go(e) etc.. and removing object control
				String newhead = "";
				for (TTRField afield : ttrs){
					if (afield.getLabel().toString().startsWith("e")&&!afield.getLabel().toString().equals(head)){
						newhead = afield.getLabel().toString();
					}
				}
				if (newhead.equals("")){
					logger.error("no new head!");
					pause();pause();
				}
				String subject = "";
				List<Integer> remove = new ArrayList<Integer>();
				List<TTRField> additions = new ArrayList<TTRField>();
				for (int f=0; f<ttrs.size(); f++){
					TTRField myfield = ttrs.get(f);
					if (myfield.getType()!=null){
						if (myfield.getType().toString().startsWith("subj")){
							List<Formula> args = ((PredicateArgumentFormula) myfield.getType()).getArguments();
							String test = args.get(1).toString();
							System.out.println(test);
							if (subject.equals("")){
								subject = test;
							} else if (!subject.equals(test)){
								throw new IllegalArgumentException("ttr"); //different subject, ob control, remove
							} else if (subject.equals(test)){
								//only remove the one with the head
								if (args.get(0).toString().equals(head)){
									remove.add(f);
								}
							}
						} else if (myfield.getLabel().toString().equals("head")){
							remove.add(f);
							additions.add(TTRField.parse("head==" + newhead + ":es"));
						} else if (myfield.getLabel().toString().equals(head)){
							remove.add(f);
							additions.add(TTRField.parse(freshVar("p",true)+"=="+myfield.getType().toString()+"("+newhead+"):t"));
						} else if (myfield.getType()!=null&&myfield.getType() instanceof PredicateArgumentFormula){
							//replace any arguments that have head in them
							List<Formula> myargs = ((PredicateArgumentFormula) myfield.getType()).getArguments();
							List<String> newargs = new ArrayList<String>();
							if (myargs.contains(Formula.create(head))){
								for (int a=0; a<myargs.size(); a++){
									if (myargs.get(a).equals(Formula.create(head))){
										newargs.add(a, newhead);
									} else {
										newargs.add(a, myargs.get(a).toString());
									}
								}
								String newPred = ((PredicateArgumentFormula) myfield.getType()).getPredicate().toString() + "(";
								for (int a=0; a<newargs.size(); a++){
									newPred+=newargs.get(a);
									if (a<newargs.size()-1){
										newPred+=",";
									}else {
										newPred+=")";
									}
								}
								remove.add(f);
								additions.add(TTRField.parse(myfield.getLabel().toString()+"=="+newPred+":"+myfield.getDSType().toString()));
							} else {
								continue;
							}
						}
					}
				}
				int removelist = 0;
				for(int r : remove){
					ttrs.remove(r-removelist);
					removelist++;
				}
				for (TTRField a : additions){
					ttrs.add(a);
				}
			}
			ttr = agenda.sort(ttrs,true).toString();
		}
		
		// check for TTR rec type parse:
		try {
			logger.info(TTRRecordType.parse(ttr));
		} catch (Exception e) {
			logger.error(ttr + " not valid TTR RT! " + e.getMessage());
			pause();pause();
		}
		logger.info("ORIGINAL = " + origChildes);
		logger.info("FINAL = " + ttr);
		//if (controlVerb == true){
		//	return ttr;
		//} else {
		//	throw new IllegalArgumentException("ttr");
		//}
		return ttr;
	}

	/**
	 * A method to read in CHILDES formulae/sentence pairs and return TTRRecordType(string)/sentence pairs
	 */
	public static void CHILDESconvert() {
		CorpusReaderWriter rw = new CorpusReaderWriter(corpusSourceFolder);
		List<String[]> done = new ArrayList<String[]>();
		CorpusStatistics corpusStats = new CorpusStatistics();
		int test = 0;
		boolean testPartition = false;
		List<String> testableWords = new ArrayList<String>();
		corpusLoop: for (int i = 0; i < rw.corpusSource.size(); i++) {
			String[] pair = rw.corpusSource.get(i);
			String[] mypair = new String[3];
			try {
				if (testPartition==true){
					for (String word : pair[0].split("\\s+")){
						if (!testableWords.contains(word)){
							continue corpusLoop;
						}
					}
				}
				mypair[0] = pair[0];
				mypair[1] = TTRconvert(pair[1],pair[0]);
				mypair[2] = pair[2];
				done.add(mypair);
				corpusStats.addUtterance(pair[0]);
				
				if (testPartition ==true) {test++;}
				if (done.size()==400){
					break;/*
					testPartition = true;
					done.clear(); //begin again!
					testableWords.addAll(corpusStats.occurences.keySet());
					corpusStats.occurences.clear();
					corpusStats.uttLengths.clear();
					 */
				}
				if (test == 800){ //size of test
					break;
				}		
				
			} catch (Exception e) {
				logger.error("COULDN'T CONVERT " + pair[0]);
				rw.missed.add(pair);
			}
		}
		
		rw.writeToFile(corpusSourceFolder, done, corpusStats);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CorpusConverter c = new CorpusConverter();

		c.CHILDESconvert();

		// debugging
		// eq problems
		List<String> TTRS = new ArrayList<String>();
		//question problem:
		//TTRS.add(c.TTRconvert("lambda $1_{ev}.eq(n|+n|peanut+n|butter,pro:dem|that,$1)", ""));
		///TTRS.add(c.TTRconvert("lambda $0_{ev}.and(aux|be&1S(and(part|go-PROG(pro|I,$0),v|go(pro|I,$0)),$0),prep|to(det|the($1,n|basement($1)),$0))", "I 'm going_to go the basement"));
		//TTRS.add(c.TTRconvert("lambda $0_{ev}.adv:tem|three_o_clock($0)",""));
		//TTRS.add(c.TTRconvert("lambda $0_{ev}.Q(n|time(pro|it),$0)","is it time"));
		//TTRS.add(c.TTRconvert("lambda $0_{ev}.adv:loc|here($0)"));
		//TTRS.add(c.TTRconvert("n|home(n:prop|Becky)"));
		//TTRS.add(c.TTRconvert("adj|good(pro|you)"));
		//TTRS.add(c.TTRconvert("lambda $0_{ev}.not(adj|sure(pro|I),$0)"));
		//TTRS.add(c.TTRconvert("lambda $0_{e}.eqLoc(pro:poss:det|your($1,n|cup($1)),$0)"));
		//TTRS.add(c.TTRconvert("lambda $0_{ev}.and(aux|be&PRES(part|go-PROG(pro|you,$0),$0),adv:loc|backwards($0))"));
		//TTRS.add(c.TTRconvert("lambda $0_{ev}.and(aux|be&1S(and(part|go-PROG(pro|I,$0),v|run(pro|I,$0)),$0),prep|to(det|the($1,n|basement($1)),$0))"));
		//TTRS.add(c.TTRconvert("lambda $0_{ev}.and(aux|be&1S(part|go-PROG(pro|I,$0),$0),adv:loc|upstairs($0))"));
		//TTRS.add(c.TTRconvert("lambda $0_{e}.lambda $1_{ev}.aux|be&3S(part|sit-PROG($0,$1),$1)"));
		//TTRS.add(c.TTRconvert("lambda $0_{ev}.Q(not(pro:wh|what,$0),$0)"));
		//TTRS.add(c.TTRconvert("lambda $0_{ev}.Q(not(adj|good(pro|it),$0),$0)","isn 't it good"));
		//TTRS.add(c.TTRconvert("lambda $0_{e}.lambda $1_{ev}.v|read&ZERO($0,det|the($2,n|book($2)),$1)"));
		//TTRS.add(c.TTRconvert("lambda $0_{ev}.Q(qn|more($1,n|juice($1)),$0)"));
		/*
		 * TTRS.add(c.TTRconvert("lambda $0_{e}.lambda $1_{ev}.eq($0,pro:dem|that,$1)"));
		 * TTRS.add(c.TTRconvert("lambda $0_{ev}.eq(pro:dem|that,and(n:prop|Jack,n:prop|Jill),$0)"));
		 * TTRS.add(c.TTRconvert("lambda $0_{ev}.eq(pro:dem|this,pro:poss:det|your($1,n|pumpkin($1)),$0)"));
		 * TTRS.add(c.TTRconvert("lambda $0_{e}.lambda $1_{ev}.eq($0,det|that($2,n|man($2)),$1)"));
		 */
		// multiple noun head problems and Q problems..
		//TTRS.add(c.TTRconvert("n|+n|peanut+n|butter(pro:dem|that)", ""));
		 //TTRS.add(c
		 //.TTRconvert("lambda $0_{ev}.Q(aux|will&COND(v|like(pro|you,det|a($1,n|+n|graham+n|cracker($1)),$0),$0),$0)",""));
		
		 //TTRS.add(c
		 //.TTRconvert("lambda $0_{ev}.and(v|read&ZERO(pro|you,$0),prep|about(det|the($1,n|+n|choo+n|choo($1)),$0))"));
		 //TTRS.add(c
		 //.TTRconvert("lambda $0_{ev}.Q(aux|will&COND(v|like(pro|you,qn|more($1,and(n|grape($1),n|juice($1))),$0),$0),$0)"));
		// different, not concat

		// Q(/not( PROBLEMS
		// TTRS.add(c.TTRconvert("lambda $0_{ev}.Q(not(det|a($1,n|cracker($1)),$0),$0)"));
		// TTRS.add(c.TTRconvert("lambda $0_{ev}.Q(qn|no($1,n|picture-PL($1)),$0)"));
		// TTRS.add(c.TTRconvert("pro:poss:det|your($0,n|pencil($0))"));
		// "put" here too
		// TTRS.add(c
		// .TTRconvert("lambda $0_{e}.lambda $1_{ev}.and(aux|do(v|put&ZERO(pro|you,$0,$1),$1),prep|on(pro|it,$1))"));
		// "put" and "gonna" and "not/Q" here..
		 //TTRS.add(c
		 //.TTRconvert("lambda $0_{ev}.not(and(aux|be&3S(and(part|go-PROG(pro|he,$0),v|put&ZERO(pro|he,pro|them,$0)),$0),adv|away($0)),$0)"));
		// "try to"/ "gonna", control verb types..
		 //TTRS.add(c.TTRconvert("lambda $0_{ev}.and(aux|be&3S(and(part|try-PROG(pro|he,$0),v|put&ZERO(pro|he,det|the($1,n|toy-PL($1)),$0)),$0),prep|on(det|the($2,n|girl($2)),$0))"));
		 //TTRS.add(c.TTRconvert("lambda $0_{ev}.and(aux|be&3S(and(part|try-PROG(n:prop|Lassie,$0),v|get(n:prop|Lassie,pro|him,$0)),$0),adv:loc|out($0))"));
		 //want object control
		 //TTRS.add(c.TTRconvert("lambda $0_{ev}.Q(and(v|want(pro|you,$0),v|have(n:prop|Mommy,det|a($1,n|letter($1)),$0)),$0)"));
		 
		 
		// atomic probs
		// TTRS.add(c.TTRconvert("lambda $0_{e}.lambda $1_{ev}.eq($0,pro:dem|that,$1)"));
		// TTRS.add(c.TTRconvert("lambda $0_{ev}.Q(pro:wh|who,$0)"));
		// TTRS.add(c.TTRconvert("lambda $0_{ev}.Q(n:prop|Neil,$0)"));
		// TTRS.add(c.TTRconvert("n:prop|Sambo"));
		// TTRS.add(c.TTRconvert("pro:dem|that"));

		for (String t : TTRS) {
			System.out.println(t);
		}

	}

}
