package qmul.ds.formula;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.learn.TreeFilter;
import qmul.ds.tree.BasicOperator;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.LabelFactory;
import qmul.ds.tree.label.Requirement;
import qmul.ds.tree.label.TypeLabel;
import qmul.ds.type.BasicType;
import qmul.ds.type.DSType;
import edu.stanford.nlp.util.Pair;

/**
 * A TTR record type
 * 
 * @author arash
 */
public class TTRRecordType extends TTRFormula {

	private static final long serialVersionUID = 1L;

	protected static Logger logger = Logger.getLogger(TTRRecordType.class);
	public static final String TTR_OPEN = "[";
	public static final String TTR_LABEL_SEPARATOR = ":";
	public static final String TTR_FIELD_SEPARATOR = "|";
	public static final String TTR_CLOSE = "]";

	public static final String TTR_HEAD = "*";
	public static final String TTR_LINE_BREAK = "TTRBR";
	public static final String TTR_TYPE_SEPARATOR = "==";
	// WARNING: Type separator is now "==", NOT "=" in order not to clash with Formula identity e.g. "e=e1"
	// perhaps makes sense if this was the other way around... "=" is more like assignment and "==" equality
	// predicate...
	// at the moment we should have e.g. [x==john:e]
	public static final TTRLabel HEAD = new TTRLabel("head");
	private static final TTRLabel REF_TIME = new TTRLabel("reftime");

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

	public int numFields() {
		return fields.size();
	}

	public List<TTRField> getFields() {
		return fields;
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

	/**
	 * @param label
	 * @param formula
	 */
	public TTRRecordType(String label, String formula) {
		TTRLabel ttrlabel = new TTRLabel(label);
		Formula ttrformula = (formula == null ? null : Formula.create(formula));
		add(ttrlabel, ttrformula, null);
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
				 * if (core.head() == null) { // artificially head it with event label if possible else leave it
				 * headless.... for (TTRField coref : core.fields) { if (coref.getDSType() != null &&
				 * coref.getDSType().equals(DSType.es)) { core.deemHead(coref.getLabel()); } } }
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

	private void removeFields(TTRRecordType argument) {
		for (TTRField fi : argument.fields) {
			remove(fi.getLabel());
		}

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
	 * If a mapping for label exists, change its value to formula; if not, add a new mapping [label = formula : dsType]
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
	 * If a mapping for f.label exists, change its value to f.formula; if not, add a new mapping [f.label = f.formula :
	 * f.dsType]
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
	 * @param label
	 *            e.g. x
	 * @return the label x unchanged if it is not used in this record; the next available unused x1,x2 etc otherwise
	 */
	public TTRLabel getFreeLabel(TTRLabel label) {
		while (record.containsKey(label)) {
			label = label.next();
		}
		return label;
	}

	/**
	 * Add (at bottom) this label & formula, renaming label if necessary to avoid clashing with existing labels
	 * 
	 * @param label
	 * @param formula
	 * @return the new label - same as original label unless renaming happened
	 */
	public TTRLabel add(TTRLabel label, Formula formula, DSType dsType) {
		return addAt(fields.size(), label, formula, dsType);
	}

	/**
	 * Add this field at an index where all its variable dependencies are satisfied. Add at end if they cannot be
	 * satisfied.
	 * 
	 * @param f
	 *            field to be added
	 */
	public void add(TTRField f) {
		if (record.containsKey(f.getLabel()))
			throw new IllegalArgumentException("Coinciding labels in:" + this + " when adding" + f);
		ArrayList<TTRField> list = new ArrayList<TTRField>();
		Set<Variable> variables = new HashSet<Variable>(f.getVariables());
		int i = 0;
		for (; i < fields.size(); i++) {
			if (variables.isEmpty())
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

	/**
	 * Add (at top) this label & formula, renaming label if necessary to avoid clashing with existing labels
	 * 
	 * @param label
	 * @param formula
	 * @return the new label - same as original label unless renaming happened
	 */
	public TTRLabel addAtTop(TTRLabel label, Formula formula, DSType dsType) {
		return addAt(0, label, formula, dsType);
	}

	/**
	 * Add (at index) this label & formula, renaming label if necessary to avoid clashing with existing labels
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

	/**
	 * 
	 * @param l
	 * @return new record type with label/field l removed. It is expensive since the whole record type is copied.
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

	public boolean subsumesMapped(Formula o, HashMap<Variable, Variable> map) {
		logger.debug("----------------------------");
		logger.debug("testing " + this + " subsumes " + o);
		logger.debug("with map " + map);

		// List<TTRPath> paths=o.getTTRPaths();
		// for(TTRPath p:paths)
		// System.out.println("path "+p+" parent:"+p.getParentRecType());
		if (isEmpty())
			return true;
		if (!(o instanceof TTRRecordType))
			return false;
		TTRRecordType other = (TTRRecordType) o;
		TTRField last = fields.get(fields.size() - 1);
		logger.debug("testing subsumption for field:" + last);
		for (int j = other.fields.size() - 1; j >= 0; j--) {

			TTRField otherField = other.fields.get(j);
			HashMap<Variable, Variable> copy = new HashMap<Variable, Variable>(map);
			if (last.subsumesMapped(otherField, map)) {
				logger.debug("Subsumed " + otherField);
				logger.debug("map is now:" + map);
				if (this.removeLabel(last.getLabel()).subsumesMapped(other.removeLabel(otherField.getLabel()), map)) {
					return true;
				} else {
					map.clear();
					map.putAll(copy);
				}
			} else {
				logger.debug(last + " failed against:" + otherField + " map:" + map);
				map.clear();
				map.putAll(copy);
			}
		}

		return false;
	}

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
		logger.debug("Substituting " + f2 + " for " + f1 + " in rec type:" + this);

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
	 * Conjoins/intersects this rec type with r2, by adding all the fields in r2, at the end of r1.
	 * 
	 * The merge is right-asymmetrical in the sense that identical labels take their associated type from the argument
	 * record type (i.e. the right hand side of the asymmetrical merge operator).
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
	 * adds f to the end of this record type.. if field with same label exists it is overwritten by f, but f is added to
	 * the end of this record type, rather than at the position of the original label.
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
			s += f + TTR_FIELD_SEPARATOR;

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

		return s/* .substring(0, s.length()-TTR_LINE_BREAK.length()) */+ TTR_CLOSE;
	}

	public static void main(String[] a) {

		//TTRRecordType target =	TTRRecordType.parse("[r1 : [x3 : e|head==x3 : e|p3==man(x3):t|p6==fat(x3):t]|x4==(eps, r1.head, r1) : e|head==snore : es|p1==subj(head, x4) : t]");
		//TTRRecordType r =       TTRRecordType.parse("[r : [x : e|p1==fat(x) : t|p==man(x) : t|head==x : e]|x2==(eps, r.head, r) : e|e1==snore : es|p3==subj(e1, x2) : t]");
		//System.out.println("target: "+target.toUniqueInt());
		//System.out.println("r: "+r.toUniqueInt());
		TTRRecordType target =	TTRRecordType.parse("[x1 : e|x : e|p==yellow(x) : t|p1==circle(x) : t]");
		TTRRecordType r =       TTRRecordType.parse("[x1 : e|x : e|p==yellow(x) : t|p1==square(x) : t]");
		System.out.println(target.minimumCommonSuperTypeBasic(r, new HashMap<Variable,Variable>()));
		System.out.println(target.minus(r));
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
			Dimension fieldD = f.draw(g2, (float) (x + lineDistance + maxWidth - f.getDimensionsWhenDrawn(g2)
					.getWidth()), y + (float) heightSoFar);

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
		// for now assuming meta variables not used inside rec types
		return this;
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

	public TTRRecordType getSuperTypeWithParents(TTRField f) {
		if (!this.hasField(f))
			return new TTRRecordType();
		List<TTRField> fields = getParents(f);
		fields.add(f);
		return new TTRRecordType(fields);
	}

	private List<TTRField> getParents(TTRField field) {

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
	 * the fields that field depends on minimally, i.e. if field depends on some r, the only fields within r that are
	 * relevant are the ones pointed to by field, e.g. head in r.head, or x in r.x...
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
							result.addAll(((TTRRelativePath) path).getMinimalSuperTypeWith().getFields());
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

	@Override
	public int toUniqueInt() {
		int result=0;
		for(TTRField f: fields)
		{
			result+=f.toUniqueInt();
		}
		return result;
	}
	
	
	public TTRRecordType minimumCommonSuperTypeBasic(TTRRecordType o, HashMap<Variable,Variable> map) {
	/**
	 * Simple version returns the most specific common supertype to comparator ttr
	 * In a syntactic fashion maps all possible fields and only returns those with a mapping from one to the other
	 * with strict label matching and the higher type in those cases. Recurses for embedded record types
	 */
		logger.debug("----------------------------");
		logger.debug("getting minimum common super type of " + this + " and " + o);
		logger.debug("with map " + map);
	
		TTRRecordType myttr = new TTRRecordType();
		//o.removeHead(); 
		if (this.equals(o))
			return this;
		if (isEmpty())
			return this;
		if (!(o instanceof TTRRecordType)){
			logger.error("NOT RECORD TYPE!");
			return this;			
		}	
		TTRRecordType other = (TTRRecordType) o;
		//simple n x m iteration over all fields
		for (int i = this.fields.size()-1; i>=0; i--)
		{
			TTRField last = fields.get(i);
			logger.debug("testing subsumption for field:" + last);
			for (int j = other.fields.size() - 1; j >= 0; j--) {
				TTRField otherField = other.fields.get(j);
				HashMap<Variable, Variable> copy = new HashMap<Variable, Variable>(map);
				if (!last.getLabel().equals(otherField.getLabel())){
					continue;
				}
				//TODO simple assumption of label mapping, gets much more complex without this
				if (last.subsumesMapped(otherField, map)) {
					//add this field as its the most general
					logger.debug("Subsumed " + otherField);
					logger.debug("map is now:" + map);
					myttr.add(last);
				} else if (otherField.subsumesMapped(last, map)){
					//we add the otherField
					myttr.add(otherField);
				} 
				else if (last.getDSType() == otherField.getDSType()){
					if (last.getType() instanceof TTRRecordType &
							otherField.getType() instanceof TTRRecordType){
						//recursively find the minimal common supertype of the embedded record type
						myttr.add(new TTRField(otherField.getLabel(),
								((TTRRecordType)otherField.getType()).minimumCommonSuperTypeBasic(((TTRRecordType)last.getType()), map)));
					}
					else {
						//just add the abstract ds type if not record type
						myttr.add(new TTRField(otherField.getLabel(),last.getDSType()));			
					}
				}		
			}
		}
		return myttr;
	}
	
	public Pair<TTRRecordType,TTRRecordType> minus(TTRRecordType ttr){
		/**
		 * Simple difference between this record type and the argument record type ttr
		 * Returns a simple pair of the fields in this and not in ttr (addition)
		 * and the fields in ttr but not in this one (subtraction)
		 */
		TTRRecordType addition = parse("[]");
		TTRRecordType subtract = parse("[]");
		List<TTRLabel> matched = new ArrayList<TTRLabel>();
		for (TTRField f : this.fields){
			for (TTRField fother : ttr.fields){
				if (!f.getLabel().equals(fother.getLabel())){
					continue;
				}
				if (this.get(f.getLabel()) instanceof TTRRecordType){
					if (f.getType().equals(fother.getType())){
						matched.add(f.getLabel());
					} else {
						addition.add(f);
						subtract.add(fother);
						matched.add(f.getLabel());
					}
					continue;
				}
				if (!f.getDSType().equals(fother.getDSType())||
						(!f.isManifest()&fother.isManifest())||
						(f.isManifest()&!fother.isManifest())){
					addition.add(f);
					subtract.add(fother);
					matched.add(f.getLabel());
				} else if (f.isManifest()&&fother.isManifest()){
				
					  if (!f.getType().equals(fother.getType())){
						addition.add(f);
						subtract.add(fother);
						matched.add(f.getLabel());
					
					  } else {
						 matched.add(f.getLabel());
					  }
				} else {
						matched.add(f.getLabel());
				}
				
			}
			if (!matched.contains(f.getLabel())){
				addition.add(f);
			}
		}
		for (TTRField fother : ttr.fields){
			if (!matched.contains(fother.getLabel())){
				subtract.add(fother);
			}
		}
		
		return new Pair<>(addition,subtract);
	}


}
