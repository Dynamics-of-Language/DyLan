package qmul.ds.learn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.formula.Formula;
import qmul.ds.formula.PredicateArgumentFormula;
import qmul.ds.formula.TTRField;
import qmul.ds.formula.TTRLabel;
import qmul.ds.formula.TTRPath;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.formula.TTRRelativePath;
import qmul.ds.formula.Variable;

/**
 * A class to evaluate the performance of a ttr parser in terms of precision and recall of test corpus
 * 
 * @author Julian
 * 
 */
public class Evaluation {

	protected static Logger logger = Logger.getLogger(TTRRecordType.class);
	public static final String corpusSourceFolder = "corpus" + File.separator + "CHILDES" + File.separator
			+ "eveTrainPairs";
	public static RecordTypeCorpus corpus = new RecordTypeCorpus();
	public static List<TTRRecordType[]> testGoalPairs = new ArrayList<TTRRecordType[]>();

	/**
	 * A class for evaluation results including calculating F-score
	 * 
	 * @author mpurver
	 */
	public class EvaluationResult {
		private float recall;
		private float precision;

		public EvaluationResult(float hypTotal, float goalTotal, float nodesMapped) {
			this.recall = nodesMapped == 0 ? 0 : (float) nodesMapped / (float) goalTotal;
			this.precision = nodesMapped == 0 ? 0 : (float) nodesMapped / (float) hypTotal;
		}

		public float getRecall() {
			return recall;
		}

		public float getPrecision() {
			return precision;
		}

		public float getFScore() {
			if (recall==0||precision==0){
				return 0;
			} 
			return (float) (2.0 * recall * precision) / (recall + precision);
		}

	}

	/**
	 * Just copied here not to interfere with TTRRecordType class where it's protected
	 * 
	 * @param l
	 * @return new record type with label/field l removed. It is expensive since the whole record type is copied.
	 */
	public TTRRecordType removeLabel(TTRRecordType t, TTRLabel l) {
		TTRRecordType result = new TTRRecordType();
		for (TTRField f : t.getFields()) {
			if (!f.getLabel().equals(l)) {
				result.add(f);
			}
		}
		return result;
	}
	
	/**
	 * Just ordering on fields to give precedence to manifest fields, expensive unfort
	 * @param t
	 * @return
	 */
	public TTRRecordType orderByManifest(TTRRecordType t, boolean m){
		TTRRecordType result = new TTRRecordType();
		List<TTRField> manifest = new ArrayList<TTRField>();
		List<TTRField> unmanifest = new ArrayList<TTRField>();
		List<TTRField> predArg = new ArrayList<TTRField>();
		List<TTRField> epsilons = new ArrayList<TTRField>();
		for (int f=0; f<t.getFields().size(); f++){
			TTRField myField = t.getFields().get(f);
			if (myField.getType()==null||myField.getDSType()==null){
				unmanifest.add(myField);
			} else if (myField.getType() instanceof PredicateArgumentFormula||myField.getLabel().toString().equals("head")&&myField.getType()!=null){
				if (myField.getType()instanceof PredicateArgumentFormula&&myField.getType().toString().contains(".")){
					epsilons.add(myField);
				} else {
				predArg.add(myField);}
			} else {
				manifest.add(myField);
			}
		}
		if (m==true){
			unmanifest.addAll(manifest);
			unmanifest.addAll(epsilons);
			unmanifest.addAll(predArg);
			logger.info(unmanifest);
			for (int a=unmanifest.size()-1; a>=0; a--){
				TTRField field = unmanifest.get(a);
				logger.info("adding" + field);
				if (field.getDSType()==null){
					result.addAtTop(field.getLabel(), field.getType(), null);
				}else if (field.getType()==null){
					result.addAtTop(field.getLabel(), null, field.getDSType());
				} else {
				result.addAtTop(field.getLabel(),field.getType(),field.getDSType());}
				System.out.println(field);
			}
		} else {
			manifest.addAll(unmanifest);
			for (TTRField field : manifest){
				result.add(field);
			}
			
		}
		System.out.println("::::::");
		return result;
	}


	public void SplitForTesting() {
		// just for testing split a given corpus of record types into a map between one and the next one until no more
		Collections.shuffle(corpus);
		int missed = 0;
		for (int i = 0; i < this.corpus.size(); i++) {
			if (i == this.corpus.size() - 1) {
				break; // uneven
			}
			try {
				TTRRecordType[] pair = new TTRRecordType[2];
				pair[0] = corpus.get(i).second();
				pair[1] = corpus.get(i + 1).second();
				this.testGoalPairs.add(pair);
			} catch (Exception e) {
				logger.error("couldn't parse either" + corpus.get(i).second() + " or " + corpus.get(i + 1).second());
				missed = missed + 2;
			}
			i++;
		}
		logger.info("testGoldPairs size " + this.testGoalPairs.size());
		logger.info("missed " + missed);
		pause();pause();
	}

	public HashMap<Variable, Variable> maximalMapping(TTRRecordType hypttr, TTRRecordType o,
			HashMap<Variable, Variable> map) {
		// Integer myTotal = total; // number of nodes successfully mapped to o
		logger.debug("TOP LEVEL checking max mapping" + hypttr + " is subsumed by " + o);
		if (hypttr.isEmpty()) {
			logger.info("final mapping");
			for (Variable v : map.keySet()) {
				System.out.println(v + ":" + map.get(v));
			}
			return map;
		}
		if (!(o instanceof TTRRecordType))
			return map;
		TTRRecordType other = (TTRRecordType) o;
		//order fields in hypttr to give precedence to manifest fields first..
		logger.info(hypttr);
		logger.info(other);
		other = orderByManifest(other,true); //not for other, need to check these first
		hypttr = orderByManifest(hypttr,true);
		logger.info(hypttr);
		logger.info(other);
		TTRField last = hypttr.getFields().get(hypttr.getFields().size() - 1);
		logger.info("OUR OUTER testing subsumption for field:" + last);
		//pause();pause();
		for (int j = other.getFields().size() - 1; j >= 0; j--) {
			
			TTRField otherField = other.getFields().get(j);
			logger.info("inner checking " + otherField.toString());
			//pause();pause();
			// special case for restrictors
			if (last.getType() != null && otherField.getType() != null && last.getType() instanceof TTRRecordType
					&& otherField.getType() instanceof TTRRecordType) {
				logger.info(last + " and " + otherField + " both RT types");
				// look for some subsumption (assume we don't have dominant referenced fields outside of scope):
				HashMap<Variable, Variable> embeddedmap = maximalMapping(((TTRRecordType) last.getType()),
						((TTRRecordType) otherField.getType()), new HashMap<Variable, Variable>());
				if (embeddedmap.keySet().size() > 0||
						((TTRRecordType) last.getType()).toString().equals("[]")&&((TTRRecordType) otherField.getType()).toString().equals("[]")) {
					logger.debug(last + " internal subsumes " + otherField);
					map.put(last.getLabel(), otherField.getLabel()); // put the link between labels there?
					logger.debug("map is now:" + map);
					return maximalMapping(removeLabel(hypttr, last.getLabel()),
							removeLabel(other, otherField.getLabel()), map);
				}
		} else if ((last.getType()==null||!(last.getType() instanceof PredicateArgumentFormula)) && (otherField.getType()==null||!(otherField.getType() instanceof PredicateArgumentFormula))
				&&last.subsumesMapped(otherField, map)) { //problem is it maps all arguments, don't really want that?
				//just for unmaninfest/singleton types
				logger.info(last + " subsumes " + otherField);
				logger.info("map is now:" + map);
				//pause();pause();
				return maximalMapping(removeLabel(hypttr, last.getLabel()), removeLabel(other, otherField.getLabel()),
						map);
			} else {
				logger.debug("checking partial subsumption for " + last + " against " + otherField);
				boolean partiallySubsumesMapped = false;
				if (last.getLabel().subsumesMapped(otherField.getLabel(), map)
						&& ((last.getDSType() == null && otherField.getDSType() == null) || (last.getDSType() != null && last
								.getDSType().equals(otherField.getDSType())))) {
					logger.info("field labels subsume " + last + otherField);
					// look for partial mapping here for predicates only, if we get first arg fine..?
					if (last.getType() != null && otherField.getType() != null) {
						if (last.getType() instanceof PredicateArgumentFormula
								&& otherField.getType() instanceof PredicateArgumentFormula) {
							logger.debug("both pred types " + last + otherField);
							if (((PredicateArgumentFormula) last.getType()).getPredicate().equals(
									((PredicateArgumentFormula) otherField.getType()).getPredicate())) {
								partiallySubsumesMapped = true;
								logger.debug("pred match!" + last + otherField);
								// now only make false if arity is wrong in terms of order? arg1 == Targ1 etc..

								int k = 0; // number of args in right position and subsuming
								List<Formula> myargs = ((PredicateArgumentFormula) last.getType()).getArguments();
								List<Formula> otherargs = ((PredicateArgumentFormula) otherField.getType())
										.getArguments();
								if (myargs.size() > otherargs.size()) {
									myargs = myargs.subList(0, otherargs.size()); // if myargs is longer, still ok..
								}

								argloop: for (int i = 0; i < myargs.size(); i++) {
									logger.info("arg" + i + "  " + myargs.get(i));
									logger.info(hypttr);
									logger.info(other);
									if ((myargs.get(i).toString().contains(".") || myargs.get(i).toString()
											.startsWith("r"))
											&& (otherargs.get(i).toString().contains(".") || otherargs.get(i)
													.toString().startsWith("r"))) { // path
										logger.info("both paths or restrictors");
										// both will be embedded RTs, so let's get those
										String myPathString = !myargs.get(i).toString().contains(".") ? "."
												+ myargs.get(i).toString() : myargs.get(i).toString();
										String otherPathString = !otherargs.get(i).toString().contains(".") ? "."
												+ otherargs.get(i).toString() : otherargs.get(i).toString();

										TTRRelativePath mypath = (TTRRelativePath) TTRPath.parse(myPathString);
										TTRRelativePath otherPath = (TTRRelativePath) TTRPath.parse(otherPathString);
										logger.info("both now paths  " + myPathString + " and " + otherPathString);
										// check whether one subsumes t'other?
										//if (hypttr.isEmpty()){
										//	continue argloop;
										//}
										mypath.setParentRecType(hypttr);
										otherPath.setParentRecType(other);
										logger.debug("mypath" + mypath.getMinimalSuperTypeWith());
										try {
											logger.debug("otherpath" + otherPath.getMinimalSuperTypeWith());
										} catch (Exception e) {
											// TODO restrictor dependence
											logger.error("already removed  restrictor as it doesn't match..");
											continue argloop;
										}
										if (mypath.getLabels().size() > otherPath.getLabels().size()
												|| !otherPath.getMinimalSuperTypeWith().subsumes(
														mypath.getMinimalSuperTypeWith())) {
											continue argloop; // doesn't subsume, size mismatch
										} else {
											k++;
										}
									} else if (hypttr
											.getRecord()
											.get(new TTRLabel(myargs.get(i).toString()))
											.subsumes(
													other.getRecord().get(new TTRLabel(otherargs.get(i).toString())))) {
										logger.info(hypttr
											.getRecord()
											.get(new TTRLabel(myargs.get(i).toString())));
										logger.info(other.getRecord().get(new TTRLabel(otherargs.get(i).toString())));
										logger.info(map);
										//pause();pause();
										k++; // number of args correct, only needs one?
										break;
									}
								}

								if (k == -1) { // no correct args //ok let's allow it..
									// TODO we're not permitting less than one subsuming arg in correct position- ok I
									// guess?
									// needs to be over 1 arg to make it partially mapped
									partiallySubsumesMapped = false;
									map.remove(last.getLabel());
								}
							} else { // don't add
								partiallySubsumesMapped = false;
								//map.remove(last.getLabel());
								logger.info("Failed partial subsumption for: " + otherField);
								logger.info("map is now:" + map);
							}
							// pause();pause();
						} else if (last.getType() instanceof TTRRecordType
								&& otherField.getType() instanceof TTRRecordType) {
							logger.debug(last + " and " + otherField + " both RT types");
						}
					}

					if (partiallySubsumesMapped == true) {
						logger.info("Partially subsumed for :" + otherField);
						logger.info("map is now:" + map);
						// recurse
						return maximalMapping(removeLabel(hypttr, last.getLabel()),
								removeLabel(other, otherField.getLabel()), map);
					} else { // don't add
						map.remove(last.getLabel());
						logger.info("Failed partial subsumption for: " + otherField);
						logger.info("map is now:" + map);
					}
				} else {
					map.remove(last.getLabel());
					logger.info("Failed subsumption for:" + otherField);
					logger.info("map is now:" + map);
				}
			}
		}
		// no subsumption for any of them or has gone through the loop, either way need to remove and recurse
		hypttr = removeLabel(hypttr, last.getLabel());
		return maximalMapping(hypttr, o, map);
	}

	/**
	 * Total number of fields for a TTRrecord type, including the embedded ones
	 * 
	 * @param ttr
	 * @return
	 */
	public int fieldTotal(TTRRecordType ttr) {
		int fieldTotal = 0;
		for (TTRField f : ttr.getFields()) {
			fieldTotal++;
			if (f.getType() != null && f.getType() instanceof TTRRecordType) {
				fieldTotal += fieldTotal(((TTRRecordType) f.getType())); // gets embedded ones too
			}
		}
		// logger.info("field total for " + ttr +" : " + fieldTotal);
		return fieldTotal;
	}

	public float totalNodesMapped(TTRRecordType hypttr, TTRRecordType goalttr) {
		// exhaustive mapping rather than failing, points for nodes:
		// - unmanifest field in goalttr- maximum 1 point
		// - manifest field in goalttr- 1 point for right type,
		// 1 point for right value if "john"/in preds for right predName("go").
		// 1 point for every arg with right type and position p1==go(e, x):e (here max points =4)
		// - manifest field embedded rec types- 1 point for right type (i.e. record type),
		// extra points as above for all fields within it, works recursively
		if (hypttr==null||goalttr==null){
			return 0;
		}
		HashMap<Variable, Variable> mapping = maximalMapping(hypttr, goalttr, new HashMap<Variable, Variable>());
		logger.info("above is mapping for \n" + hypttr + " and gold \n" + goalttr);
		// pause();pause();pause();

		HashMap<Variable, Float> fieldScoreMap = new HashMap<Variable, Float>();

		float totalMappedNodes = 0;

		for (Variable var : mapping.keySet()) {
			if (!(hypttr.getRecord().containsKey(new TTRLabel(var.toString())) && goalttr.getRecord().containsKey(
					new TTRLabel(mapping.get(var).toString())))) {
				continue;
			}
			logger.debug("Var mapping= " + var + ":" + mapping.get(var));
			int nodesMapped = 0; // will do a simple calc over each one
			int totalNodes = 0; // will do a simple calc over each one
			nodesMapped++;
			totalNodes++; // every mapped field gets one point
			TTRField myfield = (TTRField) hypttr.getRecord().get(new TTRLabel(var.toString()));
			TTRField otherfield = (TTRField) goalttr.getRecord().get(new TTRLabel(mapping.get(var).toString()));
			// extra points for internal stuff in manifest fields
			if (myfield.getType() != null) {

				// now if pred, deal with args
				if (myfield.getType() instanceof PredicateArgumentFormula
						&& (otherfield.getType() != null && otherfield.getType() instanceof PredicateArgumentFormula)) {
					totalNodes++;
					if (((PredicateArgumentFormula) myfield.getType()).getPredicate().equals(
							((PredicateArgumentFormula) otherfield.getType()).getPredicate())) {
						// TODO still might have incorrect pred.., should it just get nothing?
						nodesMapped++; // get a point for getting right pred name or manifest value, will have been
										// mapped from subsumption
					} else {
						// could just ignore this as wrong pred type?? or give some cred?
						continue;
					}

					List<Formula> args = ((PredicateArgumentFormula) myfield.getType()).getArguments();
					List<Formula> otherargs = ((PredicateArgumentFormula) otherfield.getType()).getArguments();
					totalNodes += otherargs.size(); // possible max score of all target arguments, not quite, extras,
													// below for subsumption
					argloop: for (int a = 0; a < args.size(); a++) {
						if (a >= otherargs.size()) {
							// TODO doesn't get punished for having more args...
							// could still get more points for paths
							continue argloop;
						}
						// has to be in right position to get the point i.e. mapping args(a) = otherargs(a)
						//TODO reftime any other path name doesn't work
						if ((args.get(a).toString().contains(".") || (args.get(a).toString().startsWith("r")&&args.get(a).toString().length()<3))
								&& (otherargs.get(a).toString().contains(".") || (otherargs.get(a).toString().startsWith("r"))&&otherargs.get(a).toString().length()<3)) { // path
							logger.info("both paths or restrictors");
							// both will be embedded RTs, so let's get those
							String myPathString = !args.get(a).toString().contains(".") ? "." + args.get(a).toString()
									: args.get(a).toString();
							String otherPathString = !otherargs.get(a).toString().contains(".") ? "."
									+ otherargs.get(a).toString() : otherargs.get(a).toString();
							logger.info("both now paths  " + myPathString + " and " + otherPathString);
							TTRRelativePath mypath = (TTRRelativePath) TTRPath.parse(myPathString);
							TTRRelativePath otherPath = (TTRRelativePath) TTRPath.parse(otherPathString);
							// check whether one susumes t'other?
							logger.info(hypttr);
							logger.info(goalttr);
							mypath.setParentRecType(hypttr);
							otherPath.setParentRecType(goalttr);
							// TODO At the moment, no points for common start of path if result doesn't strictly subsume
							// we do have points for number of sub-paths subsuming though:
							logger.debug("mypath" + mypath.getMinimalSuperTypeWith());
							logger.debug("otherpath" + otherPath.getMinimalSuperTypeWith());
							totalNodes += otherPath.getLabels().size() - 1; // extra for length of path on top of extra

							if (mypath.getLabels().size() > otherPath.getLabels().size()
									|| !otherPath.getMinimalSuperTypeWith().subsumes(mypath.getMinimalSuperTypeWith())) {
								continue argloop; // doesn't subsume, size mismatch
							} else {
								nodesMapped++; // one for the arg subsuming
								nodesMapped += mypath.getLabels().size() - 1; // how deep the path is
							}

						} else {
							TTRLabel myArg = new TTRLabel(args.get(a).toString());
							TTRLabel otherArg = new TTRLabel(otherargs.get(a).toString());
							//System.out.println(myArg);
							//System.out.println(otherArg);
							// make sure the labels return fields first..
							//TODO for robust version:
							if (!hypttr.getRecord().containsKey(myArg)&&!goalttr.getRecord().containsKey(otherArg)){
								continue argloop;
							}
							if ((((TTRField) hypttr.getRecord().get(myArg))).subsumes((((TTRField) goalttr.getRecord()
									.get(otherArg))))) {
								logger.debug(args.get(a).toString() + "subsumes!");
								nodesMapped++;
							}
						}
					}
				} else if (myfield.getType() instanceof TTRRecordType
						&& (otherfield.getType() != null && otherfield.getType() instanceof TTRRecordType)) {
					logger.debug("part of embedded " + myfield.getType() + " and " + otherfield.getType());
					nodesMapped += totalNodesMapped(((TTRRecordType) myfield.getType()),
							((TTRRecordType) otherfield.getType()));
					totalNodes += totalNodesMapped(((TTRRecordType) otherfield.getType()),
							((TTRRecordType) otherfield.getType()));
					totalMappedNodes += totalNodesMapped(((TTRRecordType) myfield.getType()),
							((TTRRecordType) otherfield.getType()));// add to total too..
				} else { 
					// should just be atomic or head..
					if (myfield.getLabel().equals(new TTRLabel("head"))&&myfield.getLabel().equals(new TTRLabel("head"))){
						TTRLabel hyphead = new TTRLabel(myfield.getType().toString());
						TTRLabel goalhead = otherfield.getType()==null ? otherfield.getLabel() : new TTRLabel(otherfield.getType().toString());
						if (hypttr.getRecord().get(hyphead)!=null&&goalttr.getRecord().get(goalhead)!=null){
							if (hypttr.getRecord().get(hyphead).subsumes(goalttr.getRecord().get(goalhead))){
								nodesMapped++;
							}
						}
						
					} else if (myfield.getType().equals(otherfield.getType())) {
						nodesMapped++;
					}
					totalNodes++;
				}
			} else if (otherfield.getType() != null) {
				// TODO give half marks, or less, by considering all the other stucture?
				// more possible total nodes here:
				totalNodes++; // gets another one for sure
				if (otherfield.getType() instanceof PredicateArgumentFormula) {

					for (Formula v : ((PredicateArgumentFormula) otherfield.getType()).getArguments()) {
						totalNodes++; // one for each arg, some more for paths
						if (v.toString().contains(".")) {
							totalNodes += ((TTRRelativePath) TTRRelativePath.parse(v.toString())).getLabels().size() - 1;
						}
					}

				}

			}
			logger.debug("field total mapped " + nodesMapped); // would we do p + r for each in instead?
			logger.debug("field total possible " + totalNodes); // now normalise
			totalMappedNodes += ((float) (((float) nodesMapped) / ((float) totalNodes)));
			fieldScoreMap.put(var, ((float) (((float) nodesMapped) / ((float) totalNodes))));
		}
		System.out.println(fieldScoreMap); // doesn't include embedded rec types in this map, recursion
		System.out.println(hypttr);
		System.out.println(goalttr);
		return totalMappedNodes;

	}

	public static void pause() {
		System.out.println("Press enter to continue...");
		try {
			System.in.read();
		} catch (Exception e) {
		}

	}

	/**
	 * Simple average of all precisionRecallFScore
	 * 
	 * @param mylist
	 * @return
	 */
	public List<Float> precisionRecallMacro(List<TTRRecordType[]> mylist) {
		float overallMassPrecision = 0;
		float overallMassRecall = 0;
		float overallMassFScore = 0;
		for (TTRRecordType[] pair : mylist) {
			TTRRecordType hypttr = pair[0];
			TTRRecordType goalttr = pair[1];
			//logger.info("checking " + hypttr + " verses " + goalttr);
			if (hypttr==null){
				hypttr = TTRRecordType.parse("[]");
			} 
			if (goalttr==null){
				goalttr = TTRRecordType.parse("[]");
			}
			try {
			
				TTRLabel head = new TTRLabel("head"); //dehead manifest heads
				if (hypttr.getRecord().containsKey(head)&&
						hypttr.getRecord().get(head).getType()!=null){
					hypttr = removeLabel(hypttr,head);
					
				}
				if (goalttr.getRecord().containsKey(head)&&
						goalttr.getRecord().get(head).getType()!=null){
					goalttr = removeLabel(goalttr,head);
				}
				EvaluationResult pr = precisionRecall(hypttr, goalttr);
				overallMassPrecision+=  pr.getPrecision();
				overallMassRecall+= pr.getRecall();
				overallMassFScore+= pr.getFScore();
				//pause();
				
			} catch (Exception e) {
				logger.info("COULD not evaluate " + hypttr + " and " + goalttr);
				pause();
				pause();
			}

		}
		List<Float> result = new ArrayList<Float>();
		float precision = overallMassPrecision / ((float) mylist.size());
		float recall = overallMassRecall / ((float) mylist.size());
		float fscore = overallMassFScore / ((float) mylist.size());
		result.add(precision);
		result.add(recall);
		result.add(fscore);
		logger.info("OVERALL MACRO precision = " + precision);
		logger.info("OVERALL MACRO recall = " + recall);
		logger.info("OVERALL MACRO f-score = " + fscore);
		return result;

	}

	/**
	 * Micro-averaged precision and recall over set of results Uses results of each case rather than simple average for
	 * each case (macro-average)
	 * 
	 * @param map
	 * @return
	 */
	public EvaluationResult precisionRecallMicro(List<TTRRecordType[]> mylist) {
		float overallTotalNodes = 0;
		float overallGoalNodes = 0;
		float overallNodesMapped = 0;

		for (TTRRecordType[] pair : mylist) {
			TTRRecordType hypttr = pair[0];
			TTRRecordType goalttr = pair[1];
			logger.info("checking " + hypttr + " verses " + goalttr);
			try {
				TTRLabel head = new TTRLabel("head"); //dehead manifest heads
				if (hypttr.getRecord().containsKey(head)&&
						hypttr.getRecord().get(head).getType()!=null){
					hypttr = removeLabel(hypttr,head);
				}
				if (goalttr.getRecord().containsKey(head)&&
						goalttr.getRecord().get(head).getType()!=null){
					goalttr = removeLabel(goalttr,head);
				}
				overallTotalNodes += totalNodesMapped(hypttr, hypttr);
				overallGoalNodes += totalNodesMapped(goalttr, goalttr); // fieldTotal(goalttr);
				overallNodesMapped += totalNodesMapped(hypttr, goalttr);

			} catch (Exception e) {
				logger.info("COULD not do MiCRO P AND R ON " + hypttr + " and " + goalttr);
				pause();
				pause();
			}

		}
		logger.info("OVERALL MICRO precision results: ");
		EvaluationResult res = new EvaluationResult(overallTotalNodes, overallGoalNodes, overallNodesMapped);
		logger.info("precision = " + res.getPrecision());
		logger.info("recall = " + res.getRecall());
		logger.info("f-score = " + res.getFScore());
		return res;

	}

	/**
	 * Calculates eval metrics based on number of fields mappable to goalTTR
	 * 
	 * @param hypttr
	 * @param goalttr
	 * @return an {@link EvaluationResult}
	 */
	public EvaluationResult precisionRecall(TTRRecordType hypttr, TTRRecordType goalttr) {
		logger.info("calculating p and r for " + hypttr  + "against gold \n" + goalttr);
		float totalNodes = totalNodesMapped(hypttr,hypttr);
		float goalNodes = totalNodesMapped(goalttr,goalttr);
		float nodesMapped = totalNodesMapped(hypttr, goalttr);
		logger.info("nodesMapped " + nodesMapped);
		logger.info("totalNodes " + totalNodes);
		logger.info("goalNodes " + goalNodes);
		if (totalNodes != ((float) fieldTotal(hypttr)) || goalNodes != ((float) fieldTotal(goalttr))) {
			logger.warn("SIZE PROBLEM : totalNodes " + totalNodes + " or goalNodes " + goalNodes);
			logger.warn("field total hypttr " + (float) fieldTotal(hypttr));
			logger.warn("field total goalttr" + (float) fieldTotal(goalttr));
			logger.warn(hypttr);
			logger.warn(goalttr);
			pause();
			pause();
		}
		EvaluationResult res = new EvaluationResult(totalNodes, goalNodes, nodesMapped);
		logger.info("precision = " + res.getPrecision());
		logger.info("recall = " + res.getRecall());
		logger.info("f-score = " + res.getFScore());
		
		return res;

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Evaluation e = new Evaluation();
		e.corpus = new RecordTypeCorpus();
		
		try {
			e.corpus.loadCorpus(new File(corpusSourceFolder+ File.separator
					+ "CHILDESconversion.txt"));
			e.SplitForTesting();
			e.precisionRecallMacro(e.testGoalPairs);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//TTRRecordType r = TTRRecordType.parse("[r : [x : e|head==x : e|p4==letter(x) : t]|x2==epsilon(r.head, r) : e|x1==eve : e|e1==have : es|p3==obj(e1, x2) : t|p2==subj(e1, x1) : t]");
		//TTRRecordType r1 = TTRRecordType.parse("[x1==it : e|x==i : e|e1==get : es|p2==obj(e1, x1) : t|p1==subj(e1, x) : t]");
		//TTRField headf = TTRField.parse("head==x1:e");
		
		//e.maximalMapping(r, r1, new HashMap<Variable,Variable>());
		/*
		TTRRecordType r = TTRRecordType.parse("[x2 : e|e2==eq : es|p4==obj(e2, x2) : t|x1==mommy : e|p3==subj(e2, x1) : t]");
		TTRRecordType r1 = TTRRecordType.parse("[x==mommy : e|e1 : es|p2==subj(e1, x) : t|p1==busy(e1) : t]");
		e.precisionRecall(r, r1);
		*/
	}

}
