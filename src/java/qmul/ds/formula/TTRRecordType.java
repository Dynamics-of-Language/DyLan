package qmul.ds.formula;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

import edu.stanford.nlp.util.Pair;
import qmul.ds.Context;
import qmul.ds.InteractiveContextParser;
import qmul.ds.Utterance;
import qmul.ds.action.meta.Meta;
import qmul.ds.action.meta.MetaElement;
import qmul.ds.action.meta.MetaFormula;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.learn.TreeFilter;
import qmul.ds.tree.BasicOperator;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.Requirement;
import qmul.ds.tree.label.TypeLabel;
import qmul.ds.type.BasicType;
import qmul.ds.type.DSType;

/**
 * A TTR record type
 * 
 * @author arash
 */
public class TTRRecordType extends TTRFormula implements Meta<TTRRecordType> {

	private static final long serialVersionUID = 1L;

	protected static Logger logger = Logger.getLogger(TTRRecordType.class);
	public static final String TTR_OPEN = "[";
	public static final String TTR_LABEL_SEPARATOR = ":";
	public static final String TTR_FIELD_SEPARATOR = "|";
	public static final String TTR_CLOSE = "]";

	public static final String TTR_HEAD = "*";
	public static final String TTR_LINE_BREAK = "TTRBR";
	public static final String TTR_TYPE_SEPARATOR = "==";
	// WARNING: Type separator is now "==", NOT "=" in order not to clash with
	// Formula identity e.g. "e=e1"
	// perhaps makes sense if this was the other way around... "=" is more like
	// assignment and "==" equality
	// predicate...
	// at the moment we should have e.g. [x==john:e]
	public static final TTRLabel HEAD = new TTRLabel("head");
	private static final TTRLabel REF_TIME = new TTRLabel("reftime");

	//pool of this record type's MetaTTRLabels
	//pool used to be static. That's wrong. Meta variables are bound, and their scope is the rec type within which 
	//they are embedded.
	
	protected HashMap<String, MetaElement<?>> metaVariablePool = new HashMap<String, MetaElement<?>>();
	
	
	
	private ArrayList<TTRField> fields = new ArrayList<TTRField>();

	private HashMap<TTRLabel, TTRField> record = new HashMap<TTRLabel, TTRField>();

	public static TTRRecordType parse(String s1) {
		TTRRecordType newRT = new TTRRecordType();
		String s = s1.trim();
		if (!s.startsWith(TTR_OPEN) || !s.endsWith(TTR_CLOSE))
			return null;
		if (s.substring(1, s.length() - 1).trim().isEmpty())
			return new TTRRecordType();

		String fieldsS = s.substring(TTR_OPEN.length(), s.length() - TTR_CLOSE.length());

		ArrayList<String> fieldStrings = splitFields(fieldsS);

		for (String fieldS : fieldStrings) {
			// logger.info("Field: " + fieldS);

			TTRField cur = TTRField.parse(fieldS);
			if (cur == null) {
				logger.error("Bad field, " + fieldS + ", in record type:" + s1);
				logger.error("This will probably result in a nonsensical atomic formula being created!");
				return null;
			}
			newRT.add(cur);
		}
		return newRT;
	}

	public static TTRRecordType parseStrictFieldOrder(String s1) {
		TTRRecordType newRT = new TTRRecordType();
		String s = s1.trim();
		if (!s.startsWith(TTR_OPEN) || !s.endsWith(TTR_CLOSE))
			return null;
		if (s.substring(1, s.length() - 1).trim().isEmpty())
			return new TTRRecordType();

		String fieldsS = s.substring(TTR_OPEN.length(), s.length() - TTR_CLOSE.length());

		ArrayList<String> fieldStrings = splitFields(fieldsS);

		for (String fieldS : fieldStrings) {
			// logger.info("Field: " + fieldS);

			TTRField cur = TTRField.parse(fieldS);
			if (cur == null) {
				logger.error("Bad field, " + fieldS + ", in record type:" + s1);
				logger.error("This will probably result in a nonsensical atomic formula being created!");
				return null;
			}
			newRT.addAtEnd(cur);
		}
		return newRT;
	}

	public int numFields() {
		return fields.size();
	}

	public List<TTRField> getFields() {
		return fields;
	}

	public List<TTRField> getFieldsbyType(String type) {
		List<TTRField> result = new ArrayList<TTRField>();
		for (TTRField f : this.fields) {
			if (f.toString().contains(type)) {
				result.add(f);
			}
		}
		return result;
	}

	public boolean hasFieldbyType(String type) {
		List<TTRField> result = new ArrayList<TTRField>();
		for (TTRField f : this.fields) {
			if (f.toString().contains(type)) {
				return true;
			}
		}
		return false;
	}

	public TTRField getHeadField() {
		if (head() == null)
			return null;
		if (head().getType() == null || head().getType() instanceof TTRAbsolutePath)
			return head();

		return record.get(new TTRLabel((Variable) head().getType()));

	}

	private static ArrayList<String> splitFields(String s) {

		int depth = 0;
		int openIndex = 0;
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < s.length(); i++) {
			if (s.substring(i, i + TTR_OPEN.length()).equals(TTR_OPEN))
				depth++;
			else if (s.substring(i, i + TTR_CLOSE.length()).equals(TTR_CLOSE))
				depth--;

			if (depth == 0 && s.substring(i, i + TTR_FIELD_SEPARATOR.length()).equals(TTR_FIELD_SEPARATOR)
					&& depth == 0) {
				result.add(s.substring(openIndex, i).trim());
				openIndex = i + TTR_FIELD_SEPARATOR.length();

			}

		}
		result.add(s.substring(openIndex, s.length()));

		if (depth != 0)
			logger.error("TTR open and close brackets not balanced in record type" + s);

		return result;
	}

	
	public TTRRecordType(String label, String formula, String dsTypeS) {
		TTRLabel ttrlabel = new TTRLabel(label);
		Formula ttrformula = (formula == null ? null : Formula.create(formula));
		DSType dsType = DSType.parse(dsTypeS);
		add(ttrlabel, ttrformula, dsType);
	}

	/**
	 * @param formula
	 */
	public TTRRecordType(TTRRecordType rt) {
		for (TTRField f : rt.fields) {
			TTRField newF = new TTRField(f);
			newF.setParentRecType(this);
			fields.add(newF);
			record.put(newF.getLabel(), newF);
		}

	}

	public TTRRecordType(List<TTRField> fields) {
		for (TTRField f : fields) {
			TTRField newF = new TTRField(f);
			newF.setParentRecType(this);
			this.fields.add(newF);
			this.record.put(newF.getLabel(), newF);
		}

	}

	public TTRRecordType() {

	}

	public Formula getType(TTRLabel l) {
		if (record.containsKey(l))
			return record.get(l).getType();

		return null;
	}

	public void deemHead(TTRLabel label) {
		if (label.equals(HEAD))
			return;
		if (head() != null) {
			head().setType(new Variable(label));
			return;
		}
		TTRField head = new TTRField(HEAD, this.record.get(label).getDSType(), new Variable(label));
		add(head);
	}

	public List<Pair<TTRRecordType, TTRLambdaAbstract>> getAbstractions(BasicType basicDSType, int newVarSuffix) {
		List<Pair<TTRRecordType, TTRLambdaAbstract>> result = new ArrayList<Pair<TTRRecordType, TTRLambdaAbstract>>();
		logger.debug("extracting " + basicDSType + " from" + this);
		TTRField head = getHeadField();

		for (TTRField f : fields) {
			if (basicDSType.equals(DSType.cn) && (f.getDSType() == null || f.getDSType().equals(DSType.cn))) {
				// abstracting cn out of e
				TTRRecordType argument = (TTRRecordType) f.getType();
				Variable v = new Variable("R" + newVarSuffix);
				TTRRecordType core = new TTRRecordType(this);

				TTRRecordType coreSubst = core.substitute(argument, v);

				TTRLambdaAbstract lambdaAbs = new TTRLambdaAbstract(v, coreSubst);
				Pair<TTRRecordType, TTRLambdaAbstract> abs = new Pair<TTRRecordType, TTRLambdaAbstract>(argument,
						lambdaAbs);
				result.add(abs);

			} else if (f.getDSType() != null && f.getDSType().equals(basicDSType)) {
				if (f.getLabel().equals(HEAD) && f.getType() != null)
					continue;
				logger.debug("extracting field:" + f);
				TTRRecordType argument = getSuperTypeWithParents(f);
				logger.debug("argument before adding dependents" + argument);
				logger.debug("head is:" + head);
				for (int i = fields.indexOf(f) + 1; i < fields.size(); i++) {
					TTRField cur = fields.get(i);

					if (cur.dependsOn(f)) {
						if (!cur.dependsOn(head) && !cur.getLabel().equals(HEAD)) {
							argument.putAtEnd(new TTRField(cur));
						}

					}

				}
				logger.debug("argument:" + argument);
				Variable v = new Variable("R" + newVarSuffix);
				TTRRecordType core = new TTRRecordType(this);

				core.removeFields(argument);
				// argument.deemHead(f.getLabel());
				if (core.isEmpty() || (core.numFields() == 1 && core.hasLabel(HEAD))) {
					logger.debug("constructed empty core");
					continue;
				}
				logger.debug("core:" + core);

				/*
				 * if (core.head() == null) { // artificially head it with event
				 * label if possible else leave it headless.... for (TTRField
				 * coref : core.fields) { if (coref.getDSType() != null &&
				 * coref.getDSType().equals(DSType.es)) {
				 * core.deemHead(coref.getLabel()); } } }
				 */

				TTRRecordType substCore = core.substitute(f.getLabel(), TTRPath.parse(v.getName() + ".head"));

				TTRFormula coreFinal = new TTRInfixExpression(TTRInfixExpression.ASYM_MERGE_FUNCTOR, v, substCore);
				TTRLambdaAbstract lambdaAbs = new TTRLambdaAbstract(v, coreFinal);
				argument.deemHead(f.getLabel());
				Pair<TTRRecordType, TTRLambdaAbstract> abs = new Pair<TTRRecordType, TTRLambdaAbstract>(argument,
						lambdaAbs);
				result.add(abs);

			}
		}
		logger.debug("result:" + result);
		return result;
	}

	public TTRRecordType removeSpecificField(TTRField _field) {
		TTRRecordType rt = new TTRRecordType(this);
		rt.removeField(_field);

		return rt;
	}

	private void removeFields(TTRRecordType argument) {
		for (TTRField fi : argument.fields) {
			remove(fi.getLabel());
		}
	}

	public TTRRecordType removeField(TTRField field) {
		if (field != null)
			return removeLabel(field.getLabel());

		return null;
	}

	public TTRRecordType removeField(String _type)// inclusive
	{
		TTRRecordType rt = new TTRRecordType(this);
		TTRField _field = rt.getFieldsbyType(_type).get(0);
		rt.removeField(_field);

		return rt;
	}

	private TTRRecordType removeFieldsUpToIndex(int i)// inclusive
	{
		TTRRecordType rt = new TTRRecordType(this);
		for (int j = 0; j <= i; j++)
			rt.remove(rt.fields.get(j).getLabel());

		return rt;
	}

	/**
	 * @return the labels
	 */
	public Set<TTRLabel> getLabels() {
		return record.keySet();
	}

	/**
	 * @return the record
	 * 
	 */
	public HashMap<TTRLabel, TTRField> getRecord() {
		return record;
	}

	/**
	 * @return the high-level (DS) type of the final field
	 */
	public DSType getDSType() {
		if (!record.containsKey(HEAD))
			return null;

		return record.get(HEAD).getDSType();
	}

	/**
	 * @param label
	 * @return the type associated with label
	 */
	public Formula get(TTRLabel label) {
		return record.get(label) == null ? null : record.get(label).getType();
	}

	/**
	 * If a mapping for label exists, change its value to formula; if not, add a
	 * new mapping [label = formula : dsType]
	 * 
	 * @param label
	 * @param formula
	 */

	public void put(TTRLabel label, Formula formula, DSType dsType) {
		if (record.containsKey(label)) {
			TTRField field = new TTRField(label, dsType, formula);
			fields.set(fields.indexOf(record.get(label)), field);
			record.put(label, field);
		} else {
			add(new TTRField(label, dsType, formula));
		}
	}

	/**
	 * If a mapping for f.label exists, change its value to f.formula; if not,
	 * add a new mapping [f.label = f.formula : f.dsType]
	 * 
	 * @param f
	 *            : a TTR field to be added to this record type
	 */

	public void put(TTRField f) {
		if (record.containsKey(f.getLabel())) {
			fields.set(fields.indexOf(record.get(f.getLabel())), f);

		} else {
			add(f);
		}
		// record.put(f.getLabel(), f);

	}

	/**
	 * Change the label for a mapping
	 * 
	 * @param oldLabel
	 * @param newLabel
	 */
	public void relabel(TTRLabel oldLabel, TTRLabel newLabel) {
		if (!oldLabel.equals(newLabel) && record.containsKey(newLabel))
			throw new IllegalArgumentException("Label " + newLabel + " already in use in record type:\n" + this);

		record.get(oldLabel).setLabel(newLabel);
		record.put(newLabel, record.get(oldLabel));
		record.remove(oldLabel);

	}

	/**
	 * relabel whole record type using map
	 * 
	 * @param map
	 * @return new relabelled record type
	 */
	public TTRRecordType relabel(Map<Variable, Variable> map) {
		TTRRecordType mapped = new TTRRecordType();
		List<TTRField> mappedFields = new ArrayList<TTRField>();

		for (TTRField f : this.fields) {

			Variable curLabel = new Variable(f.getLabel());

			Set<Variable> variables = f.getVariables();
			Formula type = f.getType();
			for (Variable var : variables) {
				if (map.containsKey(var))
					type = type.substitute(var, map.get(var));

			}
			TTRLabel newLabel;
			// what if this record type already has the target variable as
			// label?
			if (map.containsKey(curLabel)) {
				newLabel = new TTRLabel(map.get(curLabel));
				//System.out.println("mapping label to target.");
			} else if (map.containsValue(curLabel)) {

				Variable newVar = this.getFreshVariable(map.values(), curLabel.name.substring(0, 1));
				map.put(curLabel, newVar);
				newLabel = new TTRLabel(newVar);
				//System.out.println("mapping label to fresh label");

			} else {
				//System.out.println("not mapping label");
				newLabel = new TTRLabel(curLabel);
			}

			TTRField mappedField = new TTRField(newLabel, f.getDSType(), type);

			mappedFields.add(mappedField);
			//System.out.println("Mapped " + f + "->" + mappedField);
		}

		for (TTRField f : mappedFields) {
			mapped.add(f);
		}

		return mapped;

	}

	
	/**
	 * collapses duplicate super-types, i.e. super-types which are isomorphic.
	 * Should Reduce to a minimum....
	 * 
	 * We can collapse entity variables with the same manifest content. But not
	 * event terms, because their manifest content is really a type, not an
	 * entity.
	 * 
	 * @return
	 */
	public void collapseIsomorphicSuperTypes(HashMap<Variable, Variable> map) {

		
		for (int i = 0; i < fields.size(); i++) {
			TTRField first = fields.get(i);
			logger.debug("testing "+first);
			logger.debug("map:"+map);
			if (first.getDSType().equals(DSType.es))
			{
				logger.debug("Skipping "+first);
				continue;
			}
			
			for (Variable v : first.getVariables()) {
				logger.debug("testing variable:"+v);
				
				Variable v1=v;
				Formula subst=first.getType();//type is non-empty
				
				while(map.containsKey(v1))
				{
					logger.debug(v1+"->"+map.get(v1));
					subst=subst.substitute(v1, map.get(v1));
					v1=map.get(v1);
					
				}
				//remove(first.getLabel());
				//add(new TTRField(first.getLabel(), first.getDSType(), subst));
				first.setType(subst);
			}
			
			for (int j = i+1; j < fields.size(); j++) {
				TTRField next = fields.get(j);
				logger.debug("against "+next);
				if (first.getDSType().equals(next.getDSType())&&first.getType() != null && first.getType().subsumesBasic(next.getType())) {
					logger.debug("type subsumption.");
					map.put(new Variable(first.getLabel()), new Variable(next.getLabel()));
					logger.debug("removing"+first);
					if (this.remove(first.getLabel()))
					{
						i--;
						break;
						
					}
					

				}
			}

		}

	}

	/**
	 * A field is more specific than another if the maximal super type with that
	 * field has more fields. (getMaximalSuperTypeWith method) This is used in
	 * mostSpecificCommonSuperType to explore the super types in order from most
	 * specific to least specific.
	 * 
	 * @return
	 */
	public TTRRecordType sortFieldsBySpecificity() {

		ArrayList<TTRField> fields = new ArrayList<TTRField>();
		fields.addAll(this.fields);
		Collections.sort(fields, new FieldSpecificityComparator(this));
		return new TTRRecordType(fields);

	}

	private class FieldSpecificityComparator implements Comparator<TTRField> {

		TTRRecordType rt;

		FieldSpecificityComparator(TTRRecordType rt) {
			this.rt = rt;
		}

		@Override
		public int compare(TTRField f1, TTRField f2) {

			
			if (rt.getSpecificity(f1) < rt.getSpecificity(f2))
				return -1;
			else if (rt.getSpecificity(f1) > rt.getSpecificity(f2))
				return 1;

			return 0;
		}

	}

	
	/**
	 * Assumes @this already subsumes subtype. 
	 * Strict label matching.
	 * @param subtype
	 * @return
	 */
	private List<TTRRecordType> makeOneStepMoreSpecific(TTRRecordType subtype)
	{
		List<TTRRecordType> result=new ArrayList<TTRRecordType>();
		outer:
		for(TTRField f: subtype.getFields())
		{
			TTRRecordType cur=new TTRRecordType(this);
			
			//logger.debug("checking:" + f);
			if (this.hasLabel(f.getLabel()))
			{
				if (f.getType()!=null&&this.getType(f.getLabel())==null)
				{
					cur.getField(f.getLabel()).setType(f.getType());
					result.add(cur);
				}
				
				
			}
			else
			{//check to see if parents are there already, if so add the field
				List<TTRField> parents=subtype.getParents(f);
				
				for(TTRField parent:parents)
				{
					if (!this.hasLabel(parent.getLabel()))
						continue outer;
				}
				
				TTRField newF=new TTRField(f);
				
				if (f.getDSType().equals(DSType.e)||f.getDSType().equals(DSType.es))
				{
					newF.setType(null);
				}
				//logger.debug("adding "+newF);
				cur.add(newF);
				result.add(cur);
				
			}
		}
		
		return result;
		
	}
	
	public TTRRecordType mcs(TTRRecordType rt, HashMap<Variable, Variable> map)
	{
		return this.mcsRecurse(new TTRRecordType(), rt, map);
	}
	
	/**this.mcsRecurse([], rt, map)
	 * @param rt
	 * @param map
	 * @return
	 */
	private TTRRecordType mcsRecurse(TTRRecordType start, TTRRecordType rt, HashMap<Variable,Variable> map)
	{
		HashMap<Variable,Variable> copy=new HashMap<Variable,Variable>();
		logger.trace("testing subsumption of:"+start);
		long before=new Date().getTime();
		if (!start.subsumesMapped(rt, map))
		{
			return null;
		}
		long after=new Date().getTime();
		logger.trace("subsumed. it took:"+(after-before));
		copy.putAll(map);
		logger.trace("mcsRecurse from "+start);
		logger.trace("with map "+map);
	
		List<TTRRecordType> morespecs=start.makeOneStepMoreSpecific(this);
		logger.trace("subs:"+morespecs);
		for(TTRRecordType morespec:morespecs)
		{
			map.clear();
			logger.trace("testing subtype:"+morespec);
			TTRRecordType localMCS=mcsRecurse(morespec, rt, map);
			if (localMCS!=null)
			{
				logger.trace("got result back:"+localMCS);
				return localMCS;
			}
			logger.trace("Failed path");
			
			
				
		}
		map.clear();
		map.putAll(copy);
		return start;
		
	}
	
	/**
	 * Makes @this one step less specific.
	 *
	 * @return All record types resulting from this operation.
	 */
	private List<TTRRecordType> makeOneStepLessSpecific()
	{
		List<TTRRecordType> result=new ArrayList<TTRRecordType>();
		
		//make rec one step less specific and check subsumption
		for(int i=0;i<fields.size();i++)
		{
			TTRField f=fields.get(i);
			
			if(f.getDSType().equals(DSType.e)||f.getDSType().equals(DSType.es))
			{
				if (f.getType()!=null)
				{
					TTRRecordType underspec=new TTRRecordType(this);
					underspec.getField(f.getLabel()).setType(null);
					result.add(underspec);
					
				} else if (!this.hasDependent(f))
				{
					TTRRecordType underspec=new TTRRecordType(this);
					underspec.remove(f.getLabel());
					result.add(underspec);
				}
			}
			else
			{
				//assumes proof types have no dependents (i.e. that there are no higher order proof types)
				TTRRecordType underspec=new TTRRecordType(this);
				underspec.remove(f.getLabel());
				result.add(underspec);
			}
		}
		return result;
	}
	

	
	public static void main(String[] a) {

		TTRRecordType r1=TTRRecordType.parse("[x==john:e]");
		TTRRecordType r2=TTRRecordType.parse("[x:e]");
		
		System.out.println(r2.conjoin(r1));
		
//		InteractiveContextParser parser = new InteractiveContextParser(
//				"resource/2016-english-ttr-restaurant-search");
//		Utterance utt=new Utterance("arash: what can i help you with today?");
//		parser.parseUtterance(utt);
		
		
		//TTRRecordType rt2=(TTRRecordType)parser.getContext().getCurrentTuple().getSemantics(parser.getContext()).removeHead();
//		TTRRecordType rt2=TTRRecordType.parse("[x6==arash : e|e6 : es|x9==dylan : e|x1 : e|p2==modal(e6) : t|head==e6 : es|"
//				+ "p32==question(x1) : t|p27==today(e6) : t|p9==obj(e6, x9) : t|p10==subj(e6, x6) : t|p21==with(e6, x1) : t]");
//		
//		
//		//TTRRecordType rt1=new TTRRecordType();
//		TTRRecordType rt1=TTRRecordType.parse("[x22 : e|e6==help : es|x9==dylan : e|"
//				+ "x1 : e|p2==modal(e6) : t|p32==question(x1) : t|"
//				+ "p27==today(e6) : t|p9==obj(e6, x9) : t|p10==subj(e6, x22) : t|p21==with(e6, x1) : t|p34==person(x22):t]");
//		
//		System.out.println("RT1:"+rt1);
//		System.out.println("RT2:"+rt2);
////		
//		HashMap<Variable, Variable> map=new HashMap<Variable, Variable>();
//		long before=new Date().getTime();
//		TTRRecordType mcs=rt2.mostSpecificCommonSuperType(rt1, map);
//		long after=new Date().getTime();
//	
//		System.out.println("MCS(RT1, RT2)="+mcs);
//		System.out.println("map: "+map);
//		System.out.println("Mcs run time:"+(after-before));
//		

	}
	
	
	
	
	
		


	/**
	 * @param label
	 *            e.g. x
	 * @return the label x unchanged if it is not used in this record; the next
	 *         available unused x1,x2 etc otherwise
	 */
	public TTRLabel getFreeLabel(TTRLabel label) {
		while (record.containsKey(label)) {
			Matcher m = TTR_LABEL_PATTERN.matcher(toString());
			Matcher meta= META_LABEL_PATTERN.matcher(toString());
			
			if (m.matches()) {
				
			
			int myNum = (m.group(2).isEmpty() ? 0 : Integer.parseInt(m.group(2)));
			return new TTRLabel(m.group(1) + ++myNum);
			}
			else if (meta.matches())
			{
				int myNum = (m.group(2).isEmpty() ? 0 : Integer.parseInt(m.group(2)));
				return new MetaTTRLabel(m.group(1) + ++myNum);
			}
			else
				throw new RuntimeException("strange TTRLabel " + this);
		}
		return label;
	}

	/**
	 * Add (at bottom) this label & formula, renaming label if necessary to
	 * avoid clashing with existing labels
	 * 
	 * @param label
	 * @param formula
	 * @return the new label - same as original label unless renaming happened
	 */
	public TTRLabel add(TTRLabel label, Formula formula, DSType dsType) {
		return addAt(fields.size(), label, formula, dsType);
	}

	public void addAtEnd(TTRField f) {
		if (record.containsKey(f.getLabel()))
			throw new IllegalArgumentException("Coinciding labels in:" + this + " when adding:" + f);

		record.put(f.getLabel(), f);
		this.fields.add(f);
		f.setParentRecType(this);

	}

	/**
	 * Add this field at an index where all its variable dependencies are
	 * satisfied. Add at end if they cannot be satisfied.
	 * 
	 * @param f
	 *            field to be added
	 */
	public void add(TTRField f) {
		if (record.containsKey(f.getLabel()))
			throw new IllegalArgumentException("Coinciding labels in:" + this + " when adding:" + f);

		if (!f.getMetas().isEmpty() && f.isMeta())
			throw new IllegalArgumentException("Illegal field:"+f+" - cannot have meta-label at the same time as meta-vairables in the type");
		
		ArrayList<TTRField> list = new ArrayList<TTRField>();
		Set<Variable> variables = new HashSet<Variable>(f.getVariables());
		int i = 0;
		boolean dep_satisfied = false;
		for (; i < fields.size(); i++) {
			if (variables.isEmpty())
				dep_satisfied = true;

			if (dep_satisfied && f.getVariables().size() <= fields.get(i).getVariables().size())
				break;

			TTRField field = fields.get(i);
			if (variables.contains(field.getLabel()))
				variables.remove(field.getLabel());

		}

		list.addAll(fields.subList(0, i));

		list.add(f);
		if (i < fields.size())
			list.addAll(fields.subList(i, fields.size()));

		record.put(f.getLabel(), f);
		this.fields = list;
		f.setParentRecType(this);
	}

	public boolean hasFieldOfType(TTRField f) {

		if (f.getType() instanceof MetaFormula)
			return false;

		if (f.getType() == null && f.getDSType() == null) {
			logger.warn("DS type and type both null when adding:" + f);
			return false;
		}

		for (TTRField field : this.getFields()) {
			if (field.getType() instanceof MetaFormula)
				continue;

			if ((f.getDSType() == null && field.getDSType() == null) || f.getDSType().equals(field.getDSType())) {

				if (f.getType() != null && f.getType().subsumesBasic(field.getType())

						&& field.getType().subsumesBasic(f.getType()))
					return true;

			}

		}

		return false;
	}

	/**
	 * Decomposes this record type into its constituent record types, R_1..R_N
	 * such that the constinuents have minimal commonality, and, that, R1 ^ R2 ^
	 * .. ^ R_N = @this
	 * 
	 * 
	 * @return
	 */
	public List<TTRRecordType> decompose() {

		
		List<TTRRecordType> decomposedList = new ArrayList<TTRRecordType>();

		for(TTRField f: fields)
		{
			
			if (f.getType()!=null && !f.getDSType().equals(DSType.es) && f.getType().getVariables().isEmpty())
			{
				TTRRecordType singlton=new TTRRecordType();
				singlton.add(new TTRField(f));
				decomposedList.add(singlton);
			}
			else if (f.getType()!=null&&!f.getVariables().isEmpty())
			{
				TTRRecordType dependents=this.getSuperTypeWithParents(f);
				decomposedList.add(dependents);
			}
		}
		
		return decomposedList;

	}

	/**
	 * Add (at top) this label & formula, renaming label if necessary to avoid
	 * clashing with existing labels
	 * 
	 * @param label
	 * @param formula
	 * @return the new label - same as original label unless renaming happened
	 */
	public TTRLabel addAtTop(TTRLabel label, Formula formula, DSType dsType) {
		return addAt(0, label, formula, dsType);
	}

	/**
	 * Add (at index) this label & formula, renaming label if necessary to avoid
	 * clashing with existing labels
	 * 
	 * @param index
	 * @param label
	 * @param formula
	 * @return the new label - same as original label unless renaming happened
	 */
	private TTRLabel addAt(int index, TTRLabel label, Formula formula, DSType dsType) {
		TTRLabel newLabel = getFreeLabel(label);
		if (!label.equals(newLabel)) {
			formula = formula.substitute(label, newLabel);
		}
		TTRField field = new TTRField(newLabel, dsType, formula);
		fields.add(index, field);
		record.put(newLabel, field);
		return newLabel;
	}

	public boolean hasLabel(TTRLabel l) {
		return this.record.containsKey(l);
	}

	public boolean hasLabel(Variable v) {
		return this.record.containsKey(new TTRLabel(v));
	}

	public boolean hasLabels(Set<Variable> variables) {
		for (Variable v : variables)
			if (!hasLabel(v))
				return false;

		return true;
	}

	public TTRRecordType removeHeadIfManifest() {
		TTRField head = head();
		if (head == null || head.getType() == null)
			return new TTRRecordType(this);

		return removeHead();
	}

	/**
	 * removes the head label if it exists
	 * 
	 * @return
	 */
	public TTRRecordType removeHead() {
		return removeLabel(HEAD);
	}

	public boolean remove(TTRLabel l) {
		if (record.containsKey(l)) {
			fields.remove(record.get(l));
			record.remove(l);
			return true;
		}
		return false;
	}

	public boolean remove(Variable v) {
		return this.remove(new TTRLabel(v));
	}

	/**
	 * 
	 * @param l
	 * @return new record type with label/field l removed. It is expensive since
	 *         the whole record type is copied.
	 */
	private TTRRecordType removeLabel(TTRLabel l) {
		TTRRecordType result = new TTRRecordType();
		for (TTRField f : this.fields) {
			if (!f.getLabel().equals(l)) {
				result.add(new TTRField(f));
			}
		}
		return result;
	}

	public boolean isEmpty() {
		return fields.isEmpty();
	}

	public ArrayList<Meta<?>> getMetas() {
		ArrayList<Meta<?>> metas = new ArrayList<Meta<?>>();
		for (TTRField f : fields)
			metas.addAll(f.getMetas());

		return metas;

	}

	/**
	 * all metas should be backtracked together if possible. Shouldn't be done
	 * partially.
	 * 
	 * @return
	 */
	private boolean canBackTrackMetas() {
		for (TTRField f : fields) {
			if (f.getLabel() instanceof MetaTTRLabel) {
				MetaTTRLabel meta = (MetaTTRLabel) f.getLabel();
				if (!meta.canBacktrack()) {

					return false;
				}
			}
			
		}
		return true;
	}

	/**
	 * Backtracks all metas.
	 * 
	 * @return true if all metas can be backtracked.
	 */
	public boolean backtrackMetas() {
//		if (!canBackTrackMetas())
//			return false;

		for (TTRField f : fields) {
			if (!f.backtrackMetas())
			{
				logger.warn("Couldn't backtrack "+f);
				return false;
			}
		}
		return true;
	}

	public boolean subsumesBasic(Formula other) {
		if (other == null || !(other instanceof TTRRecordType))
			return false;
		TTRRecordType othType = (TTRRecordType) other;
		Set<Integer> all = new HashSet<Integer>();
		for (int i = 0; i < othType.getFields().size(); i++) {
			all.add(i);
		}
		return subsumesBasic((TTRRecordType) other, 0, all);
	}

	public boolean subsumesBasic(TTRRecordType other, int thisIndex, Set<Integer> remainingOtherIndeces) {

		if (thisIndex == fields.size())
			return true;

		for (Integer i : remainingOtherIndeces) {
			TTRField field = other.fields.get(i);
			logger.trace(fields.get(thisIndex) + " subsumes " + field);
			if (fields.get(thisIndex).subsumesBasic(field)) {
				logger.trace("success");
				Set<Integer> remaining = new HashSet<Integer>(remainingOtherIndeces);
				remaining.remove(i);
				if (subsumesBasic(other, thisIndex + 1, remaining)) {
					logger.trace("recursion succeeded");

					return true;
				}
				logger.trace("recursion failed");
				
				

			} else
				logger.trace("failed");
			//if we are here, subsumption for thisIndex failed.
			fields.get(thisIndex).partialResetMetas();
		}

		return false;
	}

	/**
	 * Returns the most specific common supertype of @this and @other, modulo
	 * relabeling. Upon return, @map will contain the relabellings used. NOTE:
	 * The MCS is not in general unique. The one returned here is maximal, but
	 * by chance: it depends on the order of the fields in @this and @other
	 * 
	 * TODO: extend to cover embedded record type. Currently no support for
	 * those.
	 * 
	 * @param other
	 * @return the most specific common supertype, expressed in terms of @this's
	 *         labels.
	 */
	public TTRRecordType mostSpecificCommonSuperType(TTRFormula other, HashMap<Variable, Variable> map) {
		
		TTRRecordType sorted1 = this.sortFieldsBySpecificity();
		TTRFormula sorted2 = other.sortFieldsBySpecificity();
		
		if (sorted2 instanceof TTRRecordType)
			return sorted1.mcs((TTRRecordType)sorted2, map);
		else if (!(sorted2 instanceof DisjunctiveType))
			throw new UnsupportedOperationException();
		
		DisjunctiveType disjunction=(DisjunctiveType)sorted2;
		//will throw error if not both arg1 and arg2 are rec types
		TTRRecordType right=(TTRRecordType)disjunction.getArg2();
		TTRRecordType left=(TTRRecordType)disjunction.getArg1();
		
		//deem the MCS of a rec type and a disjunctive type to be the one with the higher number of fields:
		//(1) MCS of this and right
		//(2) MCS of this and left
		
		TTRRecordType thisLeft=this.mcs(left, map);
		logger.debug("MCS(this, left):"+thisLeft);
		map.clear();
		TTRRecordType thisRight=this.mcs(right, map);
		logger.debug("MCS(this, right):"+thisRight);
		
		
		/**
		 * Assumes they won't have the same number of fields
		 */
		if (thisLeft.numFields()>thisRight.numFields())
			return thisLeft;
		else return thisRight;
		
		
		
	}

	/**
	 * 
	 * WARNING: Do not use this directly. It changes @this. Use
	 * mostSpecificCommonSuperType instead.
	 * THIS IS WRONG. Use mcs (not MCS).
	 * @deprecated
	 */
	private TTRRecordType MCS(TTRRecordType other, HashMap<Variable, Variable> map) {
		logger.debug("MCS of:" + this);
		logger.debug("and:" + other);
		logger.debug("Cur map:" + map);
		HashMap<Variable, Variable> copy = new HashMap<Variable, Variable>(map);

		if (this.subsumesMapped(other, map)) {
			logger.debug("subsumed: it is: " + this);
			return this;
		}

		map.clear();
		map.putAll(copy);
		// first deal with dependent fields
		for (int i = fields.size() - 1; i >= 0; i--) {

			TTRField f = fields.get(i);
			Set<Variable> variables = f.getVariables();
			if (!variables.isEmpty()) {
				TTRRecordType fRemoved = this.removeField(f).sortFieldsBySpecificity();
				// !map.values().contains(new Variable(otherF.getLabel()))&&
				for (int j = other.fields.size() - 1; j >= 0; j--) {
					TTRField otherF = other.fields.get(j);
					if (f.subsumesMapped(otherF, map)) {
						logger.debug(f + " subsumed " + otherF);
						map.remove(f.getLabel());
						TTRRecordType subMCS = fRemoved.MCS(other, map);
						logger.debug("RETURN Submcs removing " + f + "=" + subMCS);
						logger.debug("map=" + map);
						if (!subMCS.hasLabels(variables) || map.values().contains(new Variable(otherF.getLabel()))) {
							map.clear();
							map.putAll(copy);
							continue;
						}

						subMCS.add(f);
						map.put(f.getLabel(), otherF.getLabel());
						logger.debug(".. is: " + subMCS);
						return subMCS;
					}
				}
				logger.debug(f + " didn't subsume anything");
				// if we are here f didn't subsume any of the other fields, so
				// just
				// ignore it and recurse on the remainder

				TTRRecordType subMCS = fRemoved.MCS(other, map);
				logger.debug(".. is: " + subMCS);
				return subMCS;
			}
		}

		// if we are here, we didn't find any dependent field, i.e. only the
		// parent labels remain , e.g.
		// [x:e|e==run:es ...] etc. now take MCS of this made one step less
		// specific, with other.
		TTRRecordType clone = new TTRRecordType(this);
		for (TTRField f : fields) {
			TTRField cloneF = clone.getField(f.getLabel());
			if (map.containsKey(f.getLabel())) {
				if (!f.subsumesMapped(other.getField(map.get(f.getLabel())), map)) {

					if (f.getType() == null) {
						// the lack of subsumption is because of dsType. Just
						// remove the field.
						clone.remove(f.getLabel());
						return clone.MCS(other, map);
					}
//					 else if (f.getDSType().equals(DSType.es)) {
//						// we don't want to underspecify an event type. Just
//						// remove the field.
//						clone.remove(f.getLabel());
//						return clone.MCS(other, map);
//					}

					cloneF.setType(null);
					return clone.MCS(other, map);
				}
			} else {
				TTRRecordType loneField = new TTRRecordType();
				loneField.add(f);
				if (!loneField.subsumesMapped(other, map)) {
					if (f.isManifest() && !f.getDSType().equals(DSType.es))
						cloneF.setType(null);
					else
						clone.remove(f.getLabel());

					return clone.MCS(other, map);
				}

			}
		}

		return new TTRRecordType();

	}

	public TTRRecordType makeUnmanifest(TTRField f) {

		if (!fields.contains(f))
			throw new IllegalArgumentException("Trying make a non-existant field:" + f + " unfanifest in" + this);
		TTRRecordType copy = new TTRRecordType(this);
		copy.getField(f.getLabel()).setType(null);
		return copy;
	}

	
	
	public boolean subsumesMapped(TTRRecordType other, int thisIndex, HashMap<Variable, Variable> map) {

		if (thisIndex == fields.size())
			return true;

		HashMap<Variable, Variable> copy = new HashMap<Variable, Variable>(map);
		//logger.debug("testing subsumption for field:" + fields.get(thisIndex));

		// is map already telling us we should map the field at thisIndex to a
		// particular field in other?
		TTRLabel labelOfThisIndex = this.fields.get(thisIndex).getLabel();
		if (map.containsKey(labelOfThisIndex)) {
			if (fields.get(thisIndex).subsumesMapped(other.getField(map.get(labelOfThisIndex)), map)) {
				if (subsumesMapped(other, thisIndex + 1, map))
					return true;

		
				fields.get(thisIndex).partialResetMetas();
				map.clear();
				map.putAll(copy);
			}
			
		}

		for (int i = 0; i < other.fields.size(); i++) {
			TTRField field = other.fields.get(i);
			if (map.values().contains(field.getLabel()))
				continue;

			if (fields.get(thisIndex).subsumesMapped(field, map)) {
				//logger.debug("Subsumed " + field);
				//logger.debug("map is now:" + map);

				if (subsumesMapped(other, thisIndex + 1, map))
					return true;

			
				fields.get(thisIndex).partialResetMetas();
				map.clear();
				map.putAll(copy);

			} else {
				 //logger.debug(fields.get(thisIndex) + " failed against:" + field + " map:" + map);
				//fields.get(thisIndex).partialResetMetas();
				map.clear();
				map.putAll(copy);
			}
		}

		return false;
	}

	public boolean subsumesMapped(Formula o, HashMap<Variable, Variable> map) {

		if (isEmpty())
			return true;

		if (!(o instanceof TTRRecordType))
			return false;

		TTRRecordType other = (TTRRecordType) o;

		return subsumesMapped(other, 0, map);

	}

	public boolean subsumesMappedStrictLabelIdentity(TTRRecordType other, int thisIndex,
			HashMap<Variable, Variable> map) {

		if (thisIndex == fields.size())
			return true;

		HashMap<Variable, Variable> copy = new HashMap<Variable, Variable>(map);
		logger.debug("testing subsumption for field:" + fields.get(thisIndex));

		for (int i = 0; i < other.fields.size(); i++) {
			TTRField field = other.fields.get(i);
			if (map.values().contains(field.getLabel()))
				continue;

			if (fields.get(thisIndex).getLabel().equals(field.getLabel())
					&& fields.get(thisIndex).subsumesMapped(field, map)) {
				logger.debug("Subsumed " + field);
				logger.debug("map is now:" + map);

				if (subsumesMappedStrictLabelIdentity(other, thisIndex + 1, map))
					return true;

				fields.get(thisIndex).partialResetMetas();
				map.clear();
				map.putAll(copy);

			} else {
				logger.debug(fields.get(thisIndex) + " failed against:" + field + " map:" + map);
				map.clear();
				map.putAll(copy);
			}
		}

		return false;
	}

	public boolean subsumesMappedStrictLabelIdentity(Formula o, HashMap<Variable, Variable> map) {

		if (isEmpty())
			return true;

		if (!(o instanceof TTRRecordType))
			return false;

		TTRRecordType other = (TTRRecordType) o;

		return subsumesMappedStrictLabelIdentity(other, 0, map);

	}

	public boolean subsumesStrictLabelIdentity(Formula o) {
		return subsumesMappedStrictLabelIdentity(o, new HashMap<Variable, Variable>());
	}

	// public boolean subsumesMapped(Formula o, HashMap<Variable, Variable> map)
	// {
	// logger.debug("----------------------------");
	// logger.debug("testing " + this + " subsumes " + o);
	// logger.debug("with map " + map);
	//
	// // List<TTRPath> paths=o.getTTRPaths();
	// // for(TTRPath p:paths)
	// // System.out.println("path "+p+" parent:"+p.getParentRecType());
	// if (isEmpty())
	// return true;
	// if (!(o instanceof TTRRecordType))
	// return false;
	// TTRRecordType other = (TTRRecordType) o;
	// TTRField first = fields.get(0);
	// logger.debug("testing subsumption for field:" + first);
	// for (int j = 0; j < other.fields.size(); j++) {
	//
	// TTRField otherField = other.fields.get(j);
	// HashMap<Variable, Variable> copy = new HashMap<Variable, Variable>(
	// map);
	// if (first.subsumesMapped(otherField, map)) {
	// logger.debug("Subsumed " + otherField);
	// logger.debug("map is now:" + map);
	// if (this.removeLabel(first.getLabel()).subsumesMapped(
	// other.removeLabel(otherField.getLabel()), map)) {
	// return true;
	// } else {
	// map.clear();
	// map.putAll(copy);
	// first.partialResetMeta();
	// }
	// } else {
	// logger.debug(first + " failed against:" + otherField + " map:"
	// + map);
	// map.clear();
	// map.putAll(copy);
	// }
	// }
	//
	// return false;
	// }

	public <T extends DAGTuple, E extends DAGEdge> TTRFormula freshenVars(Context<T, E> c) {
		TTRRecordType fresh = new TTRRecordType(this);
		for (TTRField f : this.fields) {
			if (f.getLabel().equals(HEAD) || f.getLabel().equals(REF_TIME)) {
				continue;
			}

			DSType dsType = f.getDSType();
			if (dsType == null) {
				TTRLabel newLabel = new TTRLabel(c.getFreshRecTypeVariable());
				while (hasLabel(newLabel))
					newLabel = new TTRLabel(c.getFreshRecTypeVariable());
				fresh = fresh.substitute(f.getLabel(), newLabel);
			} else if (dsType.equals(DSType.e)) {
				TTRLabel newLabel = new TTRLabel(c.getFreshEntityVariable());
				while (hasLabel(newLabel))
					newLabel = new TTRLabel(c.getFreshEntityVariable());
				fresh = fresh.substitute(f.getLabel(), newLabel);

			} else if (dsType.equals(DSType.es)) {
				TTRLabel newLabel = new TTRLabel(c.getFreshEventVariable());
				while (hasLabel(newLabel))
					newLabel = new TTRLabel(c.getFreshEventVariable());
				fresh = fresh.substitute(f.getLabel(), newLabel);
			} else if (dsType.equals(DSType.t)) {
				TTRLabel newLabel = new TTRLabel(c.getFreshPropositionVariable());
				while (hasLabel(newLabel))
					newLabel = new TTRLabel(c.getFreshPropositionVariable());
				fresh = fresh.substitute(f.getLabel(), newLabel);
			} else {
				TTRLabel newLabel = new TTRLabel(c.getFreshPredicateVariable());
				while (hasLabel(newLabel))
					newLabel = new TTRLabel(c.getFreshPredicateVariable());
				fresh = fresh.substitute(f.getLabel(), newLabel);
			}
		}
		fresh.updateParentLinks();
		return fresh;
	}

	/**
	 * A fresh variable with a root (e.g. e, x, y, etc) - with respect to this
	 * rectype and the set toAvoid
	 */
	public Variable getFreshVariable(Collection<Variable> toAvoid, String root) {
		int i = 1;
		Variable v = new Variable(root + i);
		while (this.hasLabel(v) || toAvoid.contains(v))
			v = new Variable(root + (++i));

		return v;
	}
	
	//public MetaElement<?> getFreshVa

	public TTRFormula freshenVars(Tree t) {
		TTRRecordType fresh = new TTRRecordType(this);
		for (TTRField f : this.fields) {
			if (f.getLabel().equals(HEAD) || f.getLabel().equals(REF_TIME)) {
				continue;
			}

			DSType dsType = f.getDSType();
			if (dsType == null) {
				TTRLabel newLabel = new TTRLabel(t.getFreshRecTypeVariable());
				while (hasLabel(newLabel))
					newLabel = new TTRLabel(t.getFreshRecTypeVariable());
				fresh = fresh.substitute(f.getLabel(), newLabel);
			} else if (dsType.equals(DSType.e)) {
				TTRLabel newLabel = new TTRLabel(t.getFreshEntityVariable());
				while (hasLabel(newLabel))
					newLabel = new TTRLabel(t.getFreshEntityVariable());
				fresh = fresh.substitute(f.getLabel(), newLabel);

			} else if (dsType.equals(DSType.es)) {
				TTRLabel newLabel = new TTRLabel(t.getFreshEventVariable());
				while (hasLabel(newLabel))
					newLabel = new TTRLabel(t.getFreshEventVariable());
				fresh = fresh.substitute(f.getLabel(), newLabel);
			} else if (dsType.equals(DSType.t)) {
				TTRLabel newLabel = new TTRLabel(t.getFreshPropositionVariable());
				while (hasLabel(newLabel))
					newLabel = new TTRLabel(t.getFreshPropositionVariable());
				fresh = fresh.substitute(f.getLabel(), newLabel);
			} else {
				TTRLabel newLabel = new TTRLabel(t.getFreshPredicateVariable());
				while (hasLabel(newLabel))
					newLabel = new TTRLabel(t.getFreshPredicateVariable());
				fresh = fresh.substitute(f.getLabel(), newLabel);
			}
		}
		fresh.updateParentLinks();
		return fresh;
	}

	public TTRRecordType evaluate() {

		TTRRecordType result = new TTRRecordType();
		for (TTRField f : fields) {
			TTRField eval = f.evaluate();
			result.fields.add(eval);
			result.record.put(eval.getLabel(), eval);
		}
		result.updateParentLinks();
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#toUnicodeString()
	 */
	@Override
	public TTRRecordType substitute(Formula f1, Formula f2) {
		logger.trace("Substituting " + f2 + " for " + f1 + " in rec type:" + this);

		TTRRecordType result = new TTRRecordType();
		for (TTRField cur : fields) {

			TTRField subst = cur.substitute(f1, f2);

			result.record.put(subst.getLabel(), subst);
			result.fields.add(subst);

		}

		result.updateParentLinks();

		return result;
	}

	public void updateParentLinks() {
		// only root rec type updates its childern's parent links
		// if (this.parentRecType!=null) return;
		for (TTRField cur : fields) {
			cur.setParentRecType(this);
		}
	}

	/**
	 * Conjoins/intersects this rec type with r2, by adding all the fields in
	 * r2, at the end of r1.
	 * 
	 * The merge is right-asymmetrical in the sense that identical labels take
	 * their associated type from the argument record type (i.e. the right hand
	 * side of the asymmetrical merge operator). 1
	 * 
	 * @param r2
	 * @return
	 */
	public TTRFormula asymmetricMerge(TTRFormula r2) {
		if (r2 instanceof TTRLambdaAbstract)
			return ((TTRLambdaAbstract) r2).replaceCore(this.asymmetricMerge(((TTRLambdaAbstract) r2).getCore()))
					.evaluate();
		else if (r2 instanceof TTRInfixExpression)
			return new TTRInfixExpression(TTRInfixExpression.ASYM_MERGE_FUNCTOR, this, r2).evaluate();

		TTRRecordType other = (TTRRecordType) r2;
		TTRRecordType merged = new TTRRecordType(this);
		for (TTRField f : other.fields) {
			TTRField newF;
			if (merged.hasLabel(f.getLabel())
					&& (merged.get(f.getLabel()) != null && merged.get(f.getLabel()) instanceof TTRRecordType)
					&& (f.getType() != null && f.getType() instanceof TTRRecordType)) {
				TTRRecordType thisRestr = (TTRRecordType) merged.get(f.getLabel());
				TTRRecordType otherRestr = (TTRRecordType) f.getType();
				newF = new TTRField(new TTRLabel(f.getLabel()), f.getDSType(),
						(TTRRecordType) thisRestr.asymmetricMerge(otherRestr));
			}

			// --deprecated, we don't use epsilon terms any longer:
			else if (merged.hasLabel(f.getLabel())
					&& (merged.get(f.getLabel()) != null && merged.get(f.getLabel()) instanceof EpsilonTerm)
					&& (f.getType() != null && f.getType() instanceof EpsilonTerm)) {

				EpsilonTerm termThis = (EpsilonTerm) merged.get(f.getLabel());
				EpsilonTerm termOther = (EpsilonTerm) f.getType();
				Predicate thisFunct = termThis.getFunctor();
				Predicate otherFunct = termOther.getFunctor();
				Predicate eps = new Predicate(EPSILON_FUNCTOR);
				Predicate iota = new Predicate(IOTA_FUNCTOR);
				Predicate tau = new Predicate(TAU_FUNCTOR);
				if (thisFunct.equals(tau) || otherFunct.equals(tau))
					throw new UnsupportedOperationException();
				// assuming tau overrides eps in conjunction
				Predicate funct = (thisFunct.equals(eps) && otherFunct.equals(eps)) ? eps : iota;
				newF = new TTRField(f.getLabel(), f.getDSType(), new EpsilonTerm(funct, termOther.getOrderedPair()));

			} else {
				newF = new TTRField(f);
				

			}

			merged.remove(f.getLabel());

			TTRField eval = newF.evaluate();
			merged.add(eval);
		}

		merged.updateParentLinks();
		return merged.evaluate();
	}

	/**
	 * adds f to the end of this record type.. if field with same label exists
	 * it is overwritten by f, but f is added to the end of this record type,
	 * rather than at the position of the original label.
	 * 
	 * @param f
	 */
	public void putAtEnd(TTRField f) {
		if (record.containsKey(f.getLabel())) {

			fields.remove(record.get(f.getLabel()));
		}
		fields.add(f);
		record.put(f.getLabel(), f);
		f.setParentRecType(this);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */

	@Override
	public int hashCode() {
		return record.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TTRRecordType other = (TTRRecordType) obj;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!record.equals(other.record))
			return false;
		return true;
	}

	public boolean hasManifestContent() {
		TTRField head = this.getHeadField();
		if (head == null)
			throw new IllegalStateException("only headed rec types can have manifest content. this rectype:" + this);

		if (head.getType() != null)
			return true;

		TTRRecordType headLess = this.removeHead();

		return headLess.hasDependent(head);

	}

	public boolean hasHead() {

		return record.containsKey(HEAD);
	}

	public TTRField head() {
		return record.get(HEAD);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#clone()
	 */
	public TTRRecordType clone() {
		return new TTRRecordType(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (isEmpty())
			return TTR_OPEN + TTR_CLOSE;
		String s = TTR_OPEN;
		for (TTRField f : fields)
			s += f +TTR_FIELD_SEPARATOR;
//+ "<"+getSpecificity(f)
		return s.substring(0, s.length() - TTR_FIELD_SEPARATOR.length()) + TTR_CLOSE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#toUnicodeString()
	 */

	@Override
	public String toUnicodeString() {
		String s = TTR_OPEN;
		for (TTRField f : fields)
			s += f.toUnicodeString() + TTR_FIELD_SEPARATOR + TTR_LINE_BREAK;

		return s/* .substring(0, s.length()-TTR_LINE_BREAK.length()) */
				+ TTR_CLOSE;
	}

	public List<Tree> getEmptyAbstractions(NodeAddress prefix) {
		List<Tree> result = new ArrayList<Tree>();
		if (this.head().getDSType().equals(DSType.es)) {
			if (!this.hasDependent(this.getHeadField())) {
				Tree local = new Tree(prefix);
				logger.debug("reached base.. returning one node tree");
				local.put(new Requirement(new TypeLabel(DSType.t)));
				logger.debug("constructed:" + local);
				result.add(local);
				return result;
			} else {
				Tree local = new Tree(prefix);
				logger.debug("reached base.. returning one node tree");
				local.put(new TypeLabel(DSType.t));
				local.put(new FormulaLabel(this));
				logger.debug("constructed:" + local);
				result.add(local);
				return result;

			}
		} else if (this.head().getDSType().equals(DSType.e)) {

			Tree local = new Tree(prefix);
			local.put(new Requirement(new TypeLabel(DSType.t)));
			local.make(BasicOperator.DOWN_0);
			local.go(BasicOperator.DOWN_0);
			local.put(new TypeLabel(DSType.e));
			local.put(new FormulaLabel(this));
			List<Pair<TTRRecordType, TTRLambdaAbstract>> argumentAbstracts = this.getAbstractions(DSType.cn, 1);
			if (argumentAbstracts.size() == 1) {
				local.make(BasicOperator.DOWN_1);
				local.go(BasicOperator.DOWN_1);
				local.put(new FormulaLabel(argumentAbstracts.get(0).second()));
				DSType abstractedType = DSType.create(DSType.cn, DSType.e);
				local.put(new TypeLabel(abstractedType));
				local.go(BasicOperator.UP_1);
				local.make(BasicOperator.DOWN_0);
				local.go(BasicOperator.DOWN_0);
				local.put(new TypeLabel(DSType.cn));
				local.put(new FormulaLabel(argumentAbstracts.get(0).first()));

			}
			result.add(local);
			return result;
		}

		return null;
	}

	public String toLatex() {
		String s = "\\ttrnode{}{";
		TTRField head = null;
		for (TTRField f : fields) {
			if (f.getLabel().toString().equals("head") && f.getType() != null) {
				head = f;
				continue;
			}
			s += f.getLabel();
			if (f.getType() instanceof TTRRecordType) {
				s += "&" + ((TTRRecordType) f.getType()).toLatex() + "\\\\\n";
				continue;
			} else if (f.getType() != null) {
				s += "_{=" + f.getType().toString().replace("_", "-") + "}";
			}
			s += "&" + f.getDSType() + "\\\\\n";
		}
		if (head != null) { // only works for proper head==e1 : es cases
			s += head.getLabel() + "_{=" + head.getType().toString() + "}" + "&" + head.getDSType() + "\\\\\n";
		}
		s = s.substring(0, s.length() - 3) + "}";
		return s;
	}

	public List<Tree> getFilteredAbstractions(NodeAddress prefix, DSType type, boolean filtering) {
		logger.debug("getting abstractions for " + this);
		ArrayList<Tree> result = new ArrayList<Tree>();
		TreeFilter filter = new TreeFilter(this);
		List<DSType> list = new ArrayList<DSType>();
		if (type.equals(DSType.t)) {
			list.add(DSType.parse("e>(e>(e>t))"));
			list.add(DSType.parse("es>(e>(e>t))"));
			list.add(DSType.parse("e>(e>t)"));
			list.add(DSType.parse("e>t"));

		} else if (type.equals(DSType.cn)) {
			list.add(DSType.parse("e>(es>cn)"));
			list.add(DSType.parse("es>cn"));
			list.add(DSType.parse("e>cn"));
		}

		for (DSType dsType : list) {
			List<Tree> curTrees = getAbstractions(dsType, prefix);
			// System.out.println("for " + dsType + ":" + curTrees);
			if (!filtering && !curTrees.isEmpty()) {

				result.addAll(curTrees);
				return result;
			}

			List<Tree> filtered = filter.filter(curTrees);

			if (!filtered.isEmpty()) {
				result.addAll(filtered);
				return result;
			}

		}
		if (result.isEmpty())
			return getEmptyAbstractions(prefix);

		return result;
	}

	private double maxFieldWidth(Graphics2D g) {
		double maxWidth = 0;
		for (TTRField f : fields) {
			Dimension d = f.getDimensionsWhenDrawn(g);
			if (d.getWidth() > maxWidth)
				maxWidth = d.getWidth();
		}
		return maxWidth;

	}

	public Dimension getDimensionsWhenDrawn(Graphics2D g2) {
		FontMetrics fm = g2.getFontMetrics();
		int fieldDistance = 0;
		int lineDistance = fm.getHeight();
		int topMargin = fm.getHeight();
		double heightSoFar = topMargin;
		double maxWidth = maxFieldWidth(g2);

		for (TTRField f : fields) {

			Dimension fieldD = f.getDimensionsWhenDrawn(g2);

			heightSoFar += fieldD.getHeight() + fieldDistance;

		}
		double height = heightSoFar - fieldDistance;
		double width = maxWidth + 2 * lineDistance;

		Dimension d = new Dimension();
		d.setSize(width, height + fm.getHeight());
		return d;

	}

	public Dimension draw(Graphics2D g2, float x, float y) {
		FontMetrics fm = g2.getFontMetrics();
		int fieldDistance = 0;
		int lineDistance = fm.getHeight();
		int topMargin = fm.getHeight();
		double heightSoFar = topMargin;
		double maxWidth = maxFieldWidth(g2);

		for (int i = 0; i < fields.size(); i++) {
			TTRField f = fields.get(i);
			Dimension fieldD = f.draw(g2,
					(float) (x + lineDistance + maxWidth - f.getDimensionsWhenDrawn(g2).getWidth()),
					y + (float) heightSoFar);

			heightSoFar += fieldD.getHeight() + fieldDistance;

		}
		double height = heightSoFar - fieldDistance;
		double width = maxWidth + 2 * lineDistance;

		g2.drawLine((int) x, (int) y, (int) x, (int) (y + height));
		g2.drawLine((int) (x + width), (int) y, (int) (x + width), (int) (y + height));
		Dimension d = new Dimension();
		d.setSize(width, height + fm.getHeight());
		return d;
	}

	public TTRRecordType instantiate() {
		TTRRecordType result = new TTRRecordType();
		for (TTRField f : fields) {
			TTRField instance = f.instantiate();
			logger.trace("instatiating " + f);
			logger.trace("instantiated:" + instance);
			result.fields.add(instance);
			result.record.put(instance.getLabel(), instance);
		}
		result.updateParentLinks();
		return result;
	}

	public TTRRecordType getMinimalIncrementWith(TTRField f, TTRLabel on) {
		if (!this.hasField(f))
			return new TTRRecordType();

		List<TTRField> fields = getMinimalParents(f, on);
		fields.add(f);
		return new TTRRecordType(fields);
	}

	public List<TTRPath> getTTRPaths() {
		List<TTRPath> result = new ArrayList<TTRPath>();
		for (TTRField f : this.fields) {
			result.addAll(f.getTTRPaths());
		}
		return result;
	}

	public boolean hasDependent(TTRField f) {
		for (TTRField fi : fields) {
			if (fi.dependsOn(f)) {
				return true;
			}
		}
		return false;

	}

	

	public List<TTRField> getDependents(TTRField field) {
		List<TTRField> result = new ArrayList<TTRField>();
		for (int i = 0; i < fields.size(); i++) {
			TTRField f = fields.get(i);
			// logger.debug("Field: " + f);

			if (f.dependsOn(field)) {
				// logger.debug(" dependsOn Field: " + f);

				List<TTRField> _parents = getParents(f);
				for (TTRField _parent : _parents) {
					// logger.debug(" _parent Field: " + f);
					if (!result.contains(_parent))
						result.add(_parent);
				}

				if (!result.contains(f))
					result.add(f);
			}
		}
		return result;
	}

	public TTRRecordType getSuperTypeWithParents(TTRField f) {
		if (!this.hasField(f))
			return new TTRRecordType();
		List<TTRField> fields = getParents(f);
		fields.add(f);
		return new TTRRecordType(fields);
	}

	private List<TTRField> getMaximalSuperTypeWith(TTRField f) {
		List<TTRField> parents = getParents(f);
		
		List<TTRField> result = new ArrayList<TTRField>();
		if (parents.isEmpty())
		{
			
			result.add(f);
			return result;
		}
		
		result.addAll(parents);
		for (TTRField parent : parents)
		{
			List<TTRField> deps=getDependents(parent);
			for(TTRField dep:deps)
			{
				if (!result.contains(dep))
				{
					result.add(dep);
					List<TTRField> depParents=getParents(dep);
					for(TTRField p: depParents)
					{
						if (!result.contains(p))
							result.add(p);
					}
				}
			}
		}

		return result;
	}

	public List<TTRField> getParents(TTRField field) {

		List<TTRField> result = new ArrayList<TTRField>();
		for (int i = fields.indexOf(field) - 1; i >= 0; i--) {
			TTRField f = fields.get(i);
			if (field.dependsOn(f)) {

				result.addAll(getParents(f));
				result.add(f);

			}
		}

		return result;
	}

	/**
	 * the fields that field depends on minimally, i.e. if field depends on some
	 * r, the only fields within r that are relevant are the ones pointed to by
	 * field, e.g. head in r.head, or x in r.x...
	 * 
	 * @param field
	 * @return the fields that field depends on...
	 */
	private List<TTRField> getMinimalParents(TTRField field, TTRLabel on) {

		List<TTRField> result = new ArrayList<TTRField>();
		for (int i = fields.indexOf(field) - 1; i >= 0; i--) {
			TTRField f = fields.get(i);

			if (field.dependsOn(f)) {

				List<TTRPath> paths = field.getTTRPaths();

				if (paths.isEmpty()) {
					List<TTRField> fResult = new ArrayList<TTRField>();

					fResult = getMinimalParents(f, on);

					if (f.getLabel().equals(on) && f.getDSType() != null) {

						TTRField newF = new TTRField(f);
						newF.setType(null);
						result.add(newF);
					} else {

						result.addAll(fResult);
						result.add(f);
					}
				} else {
					for (TTRPath path : paths) {
						if (path instanceof TTRRelativePath) {
						}
					}
				}

			}
		}

		return result;
	}

	public boolean hasField(TTRField f) {

		return fields.contains(f);
	}

	/**
	 * gets minimal super type of this type with f. Just brings in the parents. Manifest values will all be null in result.
	 * @param f
	 * @return
	 */
	public TTRRecordType getMinimalSuperTypeWith(TTRField f)
	{
		TTRRecordType result=new TTRRecordType();
		
		if (f.getVariables().isEmpty())
		{
			result.add(new TTRField(f));
			return result;
		}
		
		List<TTRField> parents=getParents(f);
		for(TTRField parent: parents)
		{
			TTRField newF=new TTRField(parent);
			newF.setType(null);
			result.add(newF);
		}
		result.add(new TTRField(f));
		return result;
	}
	
	@Override
	protected List<TTRRecordType> getTypes() {
		ArrayList<TTRRecordType> list = new ArrayList<TTRRecordType>();
		list.add(this);
		return list;
	}

	public void replaceContent(TTRRecordType core) {
		fields.clear();
		record.clear();
		for (TTRField f : core.fields) {
			TTRField newF = new TTRField(f);
			newF.setParentRecType(this);
			fields.add(newF);
			record.put(newF.getLabel(), newF);
		}

	}

	public boolean equalsIgnoreHeads(TTRRecordType recType) {

		int n = this.numFields();
		if (this.hasHead())
			n--;
		int m = recType.numFields();
		if (recType.hasHead())
			m--;

		if (m != n)
			return false;

		for (TTRLabel l : this.record.keySet()) {
			if (l.equals(HEAD))
				continue;
			TTRField thisF = this.record.get(l);
			if (!recType.record.containsKey(l))
				return false;

			TTRField otherF = recType.record.get(l);
			if (!thisF.equalsIgnoreHeads(otherF))
				return false;

		}
		return true;
	}

	public TTRField getField(TTRLabel l) {

		return record.get(l);
	}

	public TTRField getField(Variable v) {
		return record.get(new TTRLabel(v));
	}

	@Override
	public int toUniqueInt() {
		int result = 0;
		for (TTRField f : fields) {
			result += f.toUniqueInt();
		}
		return result;
	}

	public TTRFormula conjoin(Formula other) {
		// System.out.println("Trying to conjoin:"+this);
		// System.out.println("with:"+other);
		if (other == null)
			return this;

		if (!(other instanceof TTRFormula))
			throw new UnsupportedOperationException();

		TTRFormula o = (TTRFormula) other;

		if (!(o instanceof TTRRecordType))
			return o.asymmetricMerge(this);

		TTRRecordType ttr = (TTRRecordType) o;

		if (this.isEmpty())
			return ttr;
		if (ttr.isEmpty())
			return this;

		// if (this.getHeadField() == null || ttr.getHeadField() == null)
		// throw new UnsupportedOperationException(
		// "Cannot conjoin rec types with no head field");
		//
		// DSType thisDSType = getHeadField().getDSType();
		// DSType otherDSType = ttr.getHeadField().getDSType();
		// if (thisDSType.equals(DSType.e) && thisDSType.equals(otherDSType))
		// return asymmetricMergeSameType(ttr);

		return ttr.asymmetricMerge(this);
	}

	/**
	 * 
	 * this is for conjoining two record types of the same head field ds type (e
	 * or es). This is used by LinkEvaluation same type in modelling appositions
	 * and short answers ("Ruth, the professor, just left" Or
	 * "A: Who did you meet? B: Ruth"). The assumptions are:
	 * 
	 * (1) that the record types are both headed; (2) the heads have the same ds
	 * type. (both e, or both es) (3) the heads should be determined (i.e.
	 * cannot have head:e. should have e.g. head=x:e) (4) the head fields either
	 * both have epsilon terms as types, or one or both have null types (e.g.
	 * x:e). WARNING: proper nouns should not be modelled as [x=john:e] but
	 * rather as iota terms. [r:[x:e; p==john(x)]; x1=iota(r.x, r):e]
	 * 
	 * Proceeds by unifying their heads labels, and their restrictor labels. and
	 * their restrictors' head labels. And then calling Assymetric merge as
	 * normal
	 */
	@Override
	public TTRFormula asymmetricMergeSameType(TTRFormula fo) {
		if (!(fo instanceof TTRRecordType))
			throw new UnsupportedOperationException();

		TTRRecordType other = (TTRRecordType) fo;
		TTRField thisHead = head();
		TTRField otherHead = other.head();
		if (thisHead == null || otherHead == null)
			throw new UnsupportedOperationException("Both record types must be headed");

		Variable thisHeadLabel = (Variable) thisHead.getType();
		Variable otherHeadLabel = (Variable) otherHead.getType();
		if (thisHeadLabel == null || otherHeadLabel == null)
			return asymmetricMerge(fo);
		// throw new
		// UnsupportedOperationException("Both record types must be headed and
		// the heads must be determined (have manifest value)");

		thisHead = getHeadField();
		otherHead = other.getHeadField();

		if (thisHead.getDSType() == null || otherHead.getDSType() == null
				|| !thisHead.getDSType().equals(otherHead.getDSType()))
			throw new UnsupportedOperationException("both record types must have head types with the same ds type");

		TTRField thisRestrictor = getRestrictorField();
		TTRField otherRestrictor = other.getRestrictorField();

		TTRRecordType headUnified = this.substitute(thisHeadLabel, otherHeadLabel);

		if (thisRestrictor == null || otherRestrictor == null) {

			return headUnified.asymmetricMerge(other);
		}

		// we know that both have a restrictor at this point (i.e. both have
		// head epsilon terms)
		EpsilonTerm thisEps = (EpsilonTerm) headUnified.getHeadField().getType();
		EpsilonTerm otherEps = (EpsilonTerm) otherHead.getType();

		Variable thisEpsVar = thisEps.getVariable();
		Variable otherEpsVar = otherEps.getVariable();
		TTRPath thisEpsPath = (TTRPath) thisEps.getOrderedPair().getArguments().get(0);
		TTRPath otherEpsPath = (TTRPath) otherEps.getOrderedPair().getArguments().get(0);

		Variable restrHeadThis = (Variable) thisEpsPath.evaluate();
		Variable restrHeadOther = (Variable) otherEpsPath.evaluate();

		TTRRecordType thisNewRestr = (restrHeadThis != null && restrHeadOther != null
				&& restrHeadThis instanceof Variable && restrHeadOther instanceof Variable)
						? (TTRRecordType) thisRestrictor.getType().substitute(restrHeadThis, restrHeadOther)
						: (TTRRecordType) thisRestrictor.getType().substitute(thisEpsVar, otherEpsVar);

		headUnified.getRestrictorField().setType(thisNewRestr);
		headUnified.getRestrictorField().setLabel(new TTRLabel(otherRestrictor.getLabel()));
		thisEps.setOrderedPair(otherEps.getOrderedPair());

		return headUnified.asymmetricMerge(other);

	}

	/**
	 * 
	 * @return the restrictor record type if this record type is headed, and the
	 *         head field has an epsilon term as its type
	 */

	public TTRField getRestrictorField() {
		if (!this.hasHead())
			return null;

		TTRField head = this.head();
		if (head.getType() == null)
			return null;

		TTRLabel headLabel = new TTRLabel((Variable) head.getType());
		if (!(getType(headLabel) instanceof EpsilonTerm))
			return null;

		EpsilonTerm headTerm = (EpsilonTerm) getType(headLabel);
		return record.get(new TTRLabel((Variable) headTerm.getRestrictor()));
	}

	public TTRRecordType minimumCommonSuperTypeBasic(TTRRecordType o, HashMap<Variable, Variable> map) {
		/**
		 * Simple version returns the most specific common supertype to
		 * comparator ttr. In a syntactic fashion maps all possible fields and
		 * only returns those with a mapping from one to the other with strict
		 * label matching and the higher type in those cases. Recurses for
		 * embedded record types
		 */
		logger.debug("----------------------------");
		logger.debug("getting minimum common super type of " + this + " and " + o);
		logger.debug("with map " + map);

		TTRRecordType myttr = new TTRRecordType();
		// o.removeHead();
		if (this.equals(o))
			return this;
		if (isEmpty())
			return this;
		if (!(o instanceof TTRRecordType)) {
			logger.error("NOT RECORD TYPE!");
			return this;
		}
		TTRRecordType other = (TTRRecordType) o;
		// simple n x m iteration over all fields
		// looks for simple label matching
		// TODO simple assumption of label mapping string identity for now, gets
		// much more
		// complex without this
		for (int i = this.fields.size() - 1; i >= 0; i--) {
			TTRField last = fields.get(i);
			logger.debug("testing subsumption for field:" + last);
			for (int j = other.fields.size() - 1; j >= 0; j--) {
				TTRField otherField = other.fields.get(j);
				HashMap<Variable, Variable> copy = new HashMap<Variable, Variable>(map);
				if (!last.getLabel().equals(otherField.getLabel())) {
					continue;
				}

				if (last.subsumesBasic(otherField)) {
					// add this field as its the most general, with the fields
					// it depends on
					logger.debug("Subsumed " + otherField);
					logger.debug("map is now:" + map);
					if (!myttr.hasField(last)) {
						myttr.add(last);
					}
					List<TTRField> parents = this.getParents(last);
					boolean checked = true;
					for (TTRField f : parents) {
						if (!other.hasField(f)) { // if not matched exactly,
													// remove all
							checked = false;
							break;
						}
						if (!myttr.hasField(f)) {
							myttr.add(f);
						}
					}
					if (checked == false) {
						for (TTRField parent : myttr.getParents(otherField)) {
							myttr.removeField(parent);
						}
						myttr.removeField(last);
					}
				} else if (otherField.subsumesBasic(last)) {
					// we add the otherField and the fields it depends on
					if (!myttr.hasField(last)) {
						myttr.add(otherField);
					}
					List<TTRField> parents = other.getParents(otherField);
					boolean checked = true;
					for (TTRField f : parents) {
						if (!this.hasField(f)) { // if not matched exactly,
													// remove all
							checked = false;
							break;
						}
						if ((!myttr.hasField(f))) {
							myttr.add(f);
						}
					}
					if (checked == false) {
						for (TTRField parent : myttr.getParents(otherField)) {
							myttr.removeField(parent);
						}
						myttr.removeField(otherField);
					}
				} else if (last.getDSType() == otherField.getDSType()) {
					if (last.getType() instanceof TTRRecordType & otherField.getType() instanceof TTRRecordType) {
						// recursively find the minimal common supertype of the
						// embedded record type, can override the parents added
						// already
						myttr.put(new TTRField(otherField.getLabel(),
								((TTRRecordType) otherField.getType()).minimumCommonSuperTypeBasic(
										((TTRRecordType) last.getType()), new HashMap<Variable, Variable>())));
					} else {
						// just add the abstract ds type if not record type
						myttr.add(new TTRField(otherField.getLabel(), last.getDSType()));
					}
				}
			}
		}
		return myttr;
	}

	public Pair<TTRRecordType, TTRRecordType> minus(TTRRecordType ttr) {
		/**
		 * As in Hough 2015 Thesis. Chapter 6. Simple difference between this
		 * record type and the argument record type ttr Returns a simple pair
		 * (conjunction) of : addition: the fields in @this and not in @ttr and,
		 * subtraction: the fields in @ttr but not in @this
		 */
		TTRRecordType addition = parse("[]"); // initialise the difference
												// conjuncts
		TTRRecordType subtract = parse("[]");
		List<TTRLabel> matched = new ArrayList<TTRLabel>();
		for (TTRField f : this.fields) { // check each field in @this against
											// all in @ttr
			for (TTRField fother : ttr.fields) {
				if (!f.getLabel().equals(fother.getLabel())) { // not matched?
																// carry on
					continue;
				}
				matched.add(f.getLabel()); // matched field names
				if (this.get(f.getLabel()) instanceof TTRRecordType) {// one of
																		// them
																		// is a
																		// record
																		// type
					if (!f.getType().equals(fother.getType())) { // not the same
																	// record
																	// type
						addition.add(f); // add to appropriate conjunct
						subtract.add(fother);
					}
				} else if (!f.getDSType().equals(fother.getDSType())
						|| (!(f.isManifest() && f.getType() instanceof PredicateArgumentFormula)
								& (fother.isManifest() && fother.getType() instanceof PredicateArgumentFormula))
						|| (f.isManifest() & f.getType() instanceof PredicateArgumentFormula
								&& !(fother.isManifest() && fother.getType() instanceof PredicateArgumentFormula))) { // difference
					// in
					// either
					// DS
					// type
					// or
					// manifestness
					addition.add(f);
					subtract.add(fother);
				} else if (f.isManifest() && f.getType() instanceof PredicateArgumentFormula && fother.isManifest()
						&& fother.getType() instanceof PredicateArgumentFormula) { // otherwise
					// both of
					// right
					// type and
					// manifestness
					if (!f.getType().equals(fother.getType())) { // manifest
																	// fields
																	// are
																	// different
						addition.add(f);
						subtract.add(fother);
					}
				}

			}
			if (!matched.contains(f.getLabel())) { // no match for this label,
													// must be addition
				addition.add(f);
			}
		}
		for (TTRField fother : ttr.fields) { // final pass to add unmatched
												// fields in other to subtract
												// conjunct
			if (!matched.contains(fother.getLabel())) {
				subtract.add(fother);
			}
		}
		// return difference conjuncts
		return new Pair<TTRRecordType, TTRRecordType>(addition, subtract);
	}

	/**
	 * Assuming that this rec type is a context (linguistic or non-linguistic),
	 * e.g.
	 * 
	 * [x1==o1:e|p1==square(x):t|p2==red(x):t|x2==o2:e|p3==circle(x):t] , this
	 * method takes a restrictor record type e.g. [x:e|p==square(x)|head==x:e]
	 * and returns a set of record types that are supertypes of this context,
	 * and that are compatible with the restrictor, in this case just:
	 * [x1==o1:e|p1==square(x):t]. The method is intended to be used both for
	 * retrieving an answer to a question, from a context (or knowledge base),
	 * and, as a way of resolving pronouns, or definite references from context
	 * (both involving restricted metavariables -to be implemented!).
	 * 
	 * more on this later. This might be hacky. This is inference.
	 * 
	 * @param restrictor
	 * @return a collection of record types.
	 */
	public Collection<TTRRecordType> leastSpecificCompatibleSuperTypes(TTRRecordType restrictor) {
		Collection<TTRRecordType> result = new ArrayList<TTRRecordType>();
		HashMap<Variable, Variable> map = new HashMap<Variable, Variable>();
		TTRRecordType rest = restrictor.removeHead();
		TTRRecordType current = new TTRRecordType(this);
		while (rest.subsumesMapped(current, map)) {
			TTRRecordType cur = new TTRRecordType();
			for (Variable v : map.keySet()) {
				cur.add(new TTRField(current.getField(map.get(v))));
				current.remove(map.get(v));
			}
			// System.out.println(cur.getFields());
			System.out.println("Found:" + cur);
			result.add(cur);
			map.clear();
		}

		return result;
	}

	/**
	 * 
	 * @param str
	 * @return
	 */
	public List<TTRLabel> getLabelsByStr(String str) {
		List<TTRLabel> labelList = new ArrayList<TTRLabel>();

		Iterator<TTRLabel> iterator = this.getLabels().iterator();
		while (iterator.hasNext()) {
			TTRLabel label = iterator.next();
			if (label.toString().contains(str))
				labelList.add(label);
		}

		return labelList;
	}

	public String toDebugString() {
		if (isEmpty())
			return TTR_OPEN + TTR_CLOSE;
		String s = TTR_OPEN;
		for (TTRField f : fields)
			s += f.toDebugString() + TTR_FIELD_SEPARATOR;

		return s.substring(0, s.length() - TTR_FIELD_SEPARATOR.length()) + TTR_CLOSE;
	}

	// backtracking index
	protected int bindex;

	@Override
	public boolean backtrack() {
		if (fields.get(bindex).canBacktrack())
			return fields.get(bindex).backtrackMetas();

		if (bindex == 0)
			return false;
		// can't backtrack last meta
		// reset all below, and backtrack the previous one.
		for (int i = bindex; i < fields.size(); i++) {
			fields.get(i).resetMetas();
		}

		for (int i = bindex - 1; i >= 0; i--) {
			if (fields.get(i).isMeta()) {
				bindex = i;

				return fields.get(i).backtrackMetas();
			}

		}

		return false;
	}

	@Override
	public void unbacktrack() {
		for (TTRField field : this.fields)
			field.unbacktrack();

	}

	@Override
	public void reset() {
		bindex = fields.size() - 1;
		this.resetMetas();

	}

	@Override
	public void partialReset() {
		this.partialResetMetas();

	}

	@Override
	public TTRRecordType getValue() {
		// TODO Auto-generated method stub
		return null;
	}

	public Formula findFormulaByStr(String string) {
		List<TTRField> _fList = this.getFields();
		for (TTRField _f : _fList) {
			TTRLabel label = _f.getLabel();
			if (label.toString().contains(string))
				return _f.getType();
		}

		return null;
	}
	
	
	int lastIndexOfAtomicFormulaMeta=0;
	
	public FormulaMetavariable getFreshAtomicMetaVariable() {
		lastIndexOfAtomicFormulaMeta++;
		
		return FormulaMetavariable.get(Formula.FORMULA_METAVARIABLE_ROOT+lastIndexOfAtomicFormulaMeta);
	}
	
	int lastIndexOfPredicateMeta=0;
	public MetaPredicate getFreshPredicateMetaVariable() {
		lastIndexOfPredicateMeta++;
		
		return MetaPredicate.get(Formula.METAPREDICATE_ROOT+lastIndexOfPredicateMeta);
		
	}

	public int getSpecificity(TTRField field) {
		List<TTRField> maxType=this.getMaximalSuperTypeWith(field);
		
		return maxType.size();
	}

	

}
