package qmul.ds.formula;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

import qmul.ds.action.meta.Meta;
import qmul.ds.type.DSType;

/**
 * A field (label + type pair) in a {@link TTRRecordType} - with type specified
 * as both possibly manifest type {@link Formula} and definitely not manifest DS
 * {@link DSType}
 * 
 * @author arash, mpurver
 */
public class TTRField extends Formula{

	private static Logger logger = Logger.getLogger(TTRField.class);
	private static final long serialVersionUID = 1L;
	// x1=formula:dstype

	private TTRLabel label;
	private Formula type; // can be PAFormula, Variable, TTRRecordType,
							// LambdaAbstract ...
	private DSType dsType;

	public static TTRField parse(String s)
	{
		TTRLabel label = null;
		Formula type = null;
		DSType dsType = null;
		int labSepIndex = indexOfLabelSep(s);
		int typeSepIndex = s.indexOf(TTRRecordType.TTR_TYPE_SEPARATOR);

		if (labSepIndex < 0) {

			return null;
		}

		if (typeSepIndex > 0 && typeSepIndex < labSepIndex) {

			String labelS = s.substring(0, typeSepIndex).trim();

			Matcher m = TTRLabel.LABEL_PATTERN.matcher(labelS);
			Matcher meta = TTRLabel.META_LABEL_PATTERN.matcher(labelS);
			if (m.matches()) {
				label = new TTRLabel(labelS);

			} else if (meta.matches()){
				//can't have meta-Label, but have a type
				//logger.warn("illegal field string: the type should always be null/empty initially when using meta-label. Field String:"+s);
				//return null;
				//allow meta-label to have manifest value.
				

				
				label = new MetaTTRLabel(labelS);
			}
			else
				return null;

			String typeS = s.substring(
					typeSepIndex + TTRRecordType.TTR_TYPE_SEPARATOR.length(),
					labSepIndex).trim();
			String dsTypeS = s.substring(
					labSepIndex + TTRRecordType.TTR_LABEL_SEPARATOR.length(),
					s.length()).trim();
			logger.trace("typeString:" + typeS);
			logger.trace("dsTypeString:" + dsTypeS);
			type = Formula.create(typeS);

			dsType = DSType.parse(dsTypeS);

			// have made DSType.parse() return null for invalid dstype...
			// but since we have seen a type separator it means we have to have
			// both type and DSType.. and so we return
			// null for the field and
			// thus reject the whole string as a record type if the dsType is
			// null.
			if (dsType == null) {
				logger.debug("dsType is null");
				return null;
			}

		} else {
			// no type separator, so one of two cases, either we have a DSType
			// to the right of the label sep, or we have
			// type (Formula)
			String labelS = s.substring(0, labSepIndex).trim();
			Matcher m = TTRLabel.LABEL_PATTERN.matcher(labelS);
			Matcher meta = TTRLabel.META_LABEL_PATTERN.matcher(labelS);
			if (m.matches()) {
				label = new TTRLabel(labelS);

			} else if (meta.matches())
			{
				
				
				label = new MetaTTRLabel(labelS);
			}
			else
				return null;

			String dsTypeS = s.substring(
					labSepIndex + TTRRecordType.TTR_LABEL_SEPARATOR.length(),
					s.length()).trim();
			if (dsTypeS.isEmpty() && label instanceof MetaTTRLabel)
				return new TTRField(label, null, null);
			else if (dsTypeS.isEmpty())
			{
				logger.warn("Illegal Field string. Can only have both empty type and empty dsType if the label is Meta. Field String:"+s);
				return null;
			}
			
			dsType = DSType.parse(dsTypeS);
			// if we can parse this as a DS type then we have unmanifest field
			// such as x:e
			// otherwise we just have a type (Formula) to the right of the label
			// sep (:), with dsType remaining null.
			// So:
			if (dsType == null) {
				type = Formula.create(dsTypeS);
			}

		}
		TTRField result = new TTRField(label, dsType, type);

		return result;
		
	}
	
	

	private static int indexOfLabelSep(String s) {
		int depth = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.substring(i, i + TTRRecordType.TTR_OPEN.length()).equals(
					TTRRecordType.TTR_OPEN))
				depth++;
			else if (s.substring(i, i + TTRRecordType.TTR_CLOSE.length())
					.equals(TTRRecordType.TTR_CLOSE))
				depth--;

			if (depth == 0
					&& s.substring(i,
							i + TTRRecordType.TTR_LABEL_SEPARATOR.length())
							.equals(TTRRecordType.TTR_LABEL_SEPARATOR)
					&& depth == 0)
				return i;

		}
		return -1;
	}

	public TTRField(TTRLabel label, DSType type) {
		this(label, type, null);
	}

	public TTRField(TTRLabel label, DSType type, Formula manifestType) {
		this.label = label;
		this.dsType = type;
		this.type = manifestType;
		if (this.type != null)
			getVariables().addAll(this.type.getVariables());
	}

	public TTRField(TTRLabel label, TTRRecordType type) {
		this(label, null, type);
	}

	public TTRField(TTRField ttrField) {

		this(new TTRLabel(ttrField.getLabel().name),
				ttrField.dsType == null ? null : ttrField.dsType.clone(),
				ttrField.type == null ? null : ttrField.type.clone());

	}

	/**
	 * @return the label
	 */
	public TTRLabel getLabel() {
		return label;
	}

	/**
	 * @param newLabel
	 */
	public void setLabel(TTRLabel newLabel) {
		this.label = newLabel;
	}

	/**
	 * @return the type
	 */
	public DSType getDSType() {
		return dsType;
	}

	/**
	 * @return the manifestType
	 */
	public Formula getType() {
		return type;
	}

	/**
	 * @return
	 */
	public boolean isManifest() {
		return type != null;
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#substitute(qmul.ds.formula.Formula,
	 * qmul.ds.formula.Formula)
	 */
	@Override
	public TTRField substitute(Formula f1, Formula f2) {
		if (type != null) {
			if (type.equals(f1)) {

				TTRField newF = new TTRField(new TTRLabel(label), dsType, f2);

				return newF;
			}
		}
		return new TTRField(label.substitute(f1, f2), dsType,
				(type != null) ? type.substitute(f1, f2) : type);

	}

	//
	// if (type == null) {
	// return new TTRField((TTRLabel) label.substitute(f1, f2), dsType, type);
	// } else if (type instanceof TTRRecordType) {
	// return new TTRField((TTRLabel) label.substitute(f1, f2), (TTRRecordType)
	// ((TTRRecordType) type).substitute(
	// f1, f2));
	// } else if (type instanceof TTRLambdaAbstract) {
	// return new TTRField((TTRLabel) label.substitute(f1, f2),
	// (TTRLambdaAbstract) ((TTRLambdaAbstract) type).substitute(f1, f2));
	// } else {
	// return new TTRField((TTRLabel) label.substitute(f1, f2), dsType,
	// (PredicateArgumentFormula) type.substitute(f1, f2));
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#subsumesBasic(qmul.ds.formula.Formula)
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#subsumesMapped(qmul.ds.formula.Formula,
	 * java.util.HashMap)
	 */
	@Override
	public boolean subsumesMapped(Formula other, HashMap<Variable, Variable> map) {
		if (!(other instanceof TTRField))
			return false;
		
		logger.debug("Checking "+this+" subsumes "+other);
		TTRField otherField = (TTRField) other;
		HashMap<Variable, Variable> copy = new HashMap<Variable, Variable>(map);
		if (label.subsumesMapped(otherField.label, map)) {
			if ((dsType == null && otherField.dsType == null)
					|| (dsType != null && dsType.equals(otherField.dsType))) {
				logger.debug("ds type matched");

				if (type == null) {
					//set type from other if meta
					logger.debug("Success, type is null");
					if (label instanceof MetaTTRLabel)
					{
						logger.debug("instantiating meta to "+otherField.type);
						this.type=otherField.type;
					}
					
					return true;
				}
				if (type instanceof TTRRecordType) {
					HashMap<Variable, Variable> newMap = new HashMap<Variable, Variable>();
					return type.subsumesMapped(otherField.type, newMap);
				} else {
					// System.out.println("Checking "+type+" subsumes "+otherField.type+" with map "+map);
					if (!type.subsumesMapped(otherField.type, map))
					{
						logger.debug("type subsumption failed.");
						logger.debug("uninstantiating meta and resetting map");
						map.clear();
						map.putAll(copy);
						//don't want to have instantiated metalabel if the field is failing to subsume
						
						partialResetMetas();
						return false;
					}
					
					return true;
				}
			}
			else
			{
				
				map.clear();
				map.putAll(copy);
				
				partialResetMetas();//resets metas
				logger.debug("DS type failed subsume:" + this);
				return false;
			}
			// failure.. don't want to have changed map if I'm returning
			// false
			// but map could have changed. reset it.
			// TODO: this is a bit hacky, leading to more time complexity
			// than necessary.

		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#evaluate()
	 */
	public TTRField evaluate() {
		if (type == null)
			return new TTRField(this);

		return new TTRField(this.label, dsType, type.evaluate());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * qmul.ds.formula.Formula#setParentRecType(qmul.ds.formula.TTRRecordType)
	 */
	public void setParentRecType(TTRRecordType r) {
		this.parentRecType = r;
		if (type == null)
			return;
		type.setParentRecType(r);
	}

	public TTRField instantiate() {
		return new TTRField(this.label.instantiate(),
				(dsType != null ? this.dsType.instantiate() : null),
				(this.type != null ? this.type.instantiate() : null));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = prime;
		result = prime * result + ((dsType == null) ? 0 : dsType.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
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
		TTRField other = (TTRField) obj;
		if (dsType == null) {
			if (other.dsType != null)
				return false;
		} else if (!dsType.equals(other.dsType))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	/**
	 * @return the set of variables (free or bound) involved in the type of this
	 *         field
	 */
	protected Set<Variable> getVariables() {

		return type == null ? new HashSet<Variable>() : type.getVariables();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.formula.Formula#clone()
	 */
	public TTRField clone() {
		return new TTRField(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {

		if (dsType != null)
			return label + (type == null ? "" : "==" + type) + " "
					+ TTRRecordType.TTR_LABEL_SEPARATOR + " " + dsType;
		else
			return label + " " + TTRRecordType.TTR_LABEL_SEPARATOR + " " + (type==null?"":type);
		// return
		// s+(type==null?"(typenull)":(type.parentRecType==this.parentRecType &&
		// type.parentRecType!=null)?"(linked)":"notLinked");

	}

	public String toDebugString() {

		if (dsType != null)
			return label
					+ (type == null ? "" : "==" + type.toDebugString() + "("
							+ type.getClass() + ")") + " "
					+ TTRRecordType.TTR_LABEL_SEPARATOR + " " + dsType;
		else
			return label + " " + TTRRecordType.TTR_LABEL_SEPARATOR + " "
					+ type.toDebugString() + "(" + type.getClass() + ")";

	}

	public static void main(String[] a) {
		TTRField one = parse("x15==usr:e");
		TTRField two = parse("x15==usr:e");
		System.out.println(one.subsumesBasic(two));
		
		

	}

	@Override
	public boolean subsumesBasic(Formula other) {
		if (!(other instanceof TTRField))
			return false;

		TTRField otherField = (TTRField) other;

		if ((dsType == null && otherField.dsType == null)
				|| (dsType != null && dsType.equals(otherField.dsType))) {

			if ((type == null) || type.subsumesBasic(otherField.type)) {
				//logger.debug("label subsumption "+label+"and"+otherField.label);
				return label.subsumesBasic(otherField.label);
			}

		}

		return false;
	}

	public Dimension getDimensionsWhenDrawn(Graphics2D g) {
		FontMetrics fm = g.getFontMetrics();
		int lineHeight = fm.getHeight();
		int labelWidth = (dsType == null ? fm.stringWidth(this.label + " "
				+ TTRRecordType.TTR_LABEL_SEPARATOR + " ") : fm
				.stringWidth(this.label + " "
						+ TTRRecordType.TTR_TYPE_SEPARATOR + " "));

		if (type == null) {

			return new Dimension(fm.stringWidth(this.label + " : " + dsType),
					lineHeight);
		}
		Dimension typeD = type.getDimensionsWhenDrawn(g);

		if (dsType != null) {

			Dimension d = new Dimension();
			double width = labelWidth + typeD.getWidth()
					+ fm.stringWidth(" : " + dsType);
			double height = typeD.getHeight();
			d.setSize(width, height);
			return d;
		}

		Dimension d = new Dimension();
		double width = labelWidth + typeD.getWidth();
		double height = typeD.getHeight();

		d.setSize(width, height);
		return d;

	}

	public boolean dependsOn(TTRField f) {
		if (f == null)
			return false;
		if (f.getLabel().equals(label))
			return false;
		if (getVariables().contains(f.getLabel()))
			return true;

		if (f.getType() == null || !(f.getType() instanceof TTRPath))
			return false;

		return getTTRPaths().contains((TTRPath) f.getType());
	}

	public boolean dependsOn(TTRPath path) {
		return this.getTTRPaths().contains(path);
	}

	public Dimension draw(Graphics2D g2, float x, float y) {
		FontMetrics fm = g2.getFontMetrics();
		int lineHeight = fm.getHeight();
		int labelWidth = (dsType == null ? fm.stringWidth(this.label + " : ")
				: fm.stringWidth(this.label + " == "));

		if (type == null) {
			g2.drawString(this.label + " : " + dsType, x, y);
			return new Dimension(fm.stringWidth(this.label + " : " + dsType),
					lineHeight);
		}
		Dimension typeD = type.draw(g2, x + labelWidth, y);

		if (dsType != null) {
			g2.drawString(this.label + " == ", x, y);
			g2.drawString(
					" : " + dsType,
					x + labelWidth + (float) typeD.getWidth(),
					type instanceof TTRRecordType ? (float) y
							+ (float) typeD.getHeight() / 2 : y);
			Dimension d = new Dimension();
			double width = labelWidth + typeD.getWidth()
					+ fm.stringWidth(" : " + dsType);
			double height = typeD.getHeight();
			d.setSize(width, height);
			return d;
		}
		g2.drawString(
				this.label + " : ",
				x,
				type instanceof TTRRecordType ? (float) y
						+ (float) typeD.getHeight() / 2 : y);
		Dimension d = new Dimension();
		double width = labelWidth + typeD.getWidth()
				+ fm.stringWidth(" : " + dsType);
		double height = typeD.getHeight();

		d.setSize(width, height);
		return d;

	}

	public boolean dependsOn(Variable v) {

		return getVariables().contains(v);
	}

	public void setType(Formula variable) {
		this.type = variable;

	}

	public boolean equalsIgnoreHeads(TTRField otherF) {
		if (type != null && type instanceof TTRRecordType) {
			if (otherF.type == null)
				return false;
			if (!(otherF.type instanceof TTRRecordType))
				return false;

			if (dsType == null) {
				if (otherF.dsType != null)
					return false;
			} else if (!dsType.equals(otherF.dsType))
				return false;
			if (label == null) {
				if (otherF.label != null)
					return false;
			} else if (!label.equals(otherF.label))
				return false;

			TTRRecordType otherType = (TTRRecordType) otherF.type;
			TTRRecordType thisType = (TTRRecordType) type;

			return thisType.equalsIgnoreHeads(otherType);

		}
		return this.equals(otherF);

	}

	public List<TTRPath> getTTRPaths() {
		if (type == null)
			return new ArrayList<TTRPath>();
		return type.getTTRPaths();

	}

	public boolean isHead() {

		return label.equals(TTRRecordType.HEAD);
	}

	@Override
	public int toUniqueInt() {
		int typeInt = (type == null ? 0 : type.toUniqueInt());
		int dsTypeInt = (dsType == null ? 0 : dsType.toUniqueInt());
		return typeInt + dsTypeInt;
	}

	public ArrayList<Meta<?>> getMetas() {
		ArrayList<Meta<?>> metas = new ArrayList<Meta<?>>();
		//metas.addAll(label.getMetas());
		if (type==null)
			return metas;
		metas.addAll(type.getMetas());
		return metas;

	}

	
	public void resetMetaLabel() {
		if (label instanceof MetaTTRLabel)
		{
			
			((MetaTTRLabel) label).reset();
			this.type=null;
		}
		
	}

	public void partialResetMetas()
	{
		super.partialResetMetas();
		partialResetMetaLabel();
	}
	
	private void partialResetMetaLabel() {
		if (label instanceof MetaTTRLabel)
		{
			((MetaTTRLabel) label).partialReset();
			this.type=null;
		}
		
	}

	public boolean backtrackMetas() {
		if (label instanceof MetaTTRLabel)
		{
			MetaTTRLabel meta=(MetaTTRLabel)label;
			if (meta.backtrack())
			{
				this.type=null;
				return true;
			}
			else return false;
			
			
		}
		for(Meta<?> meta:getMetas())
		{
			if (!meta.backtrack())
				return false;
		}
		
		return true;
		
	}

	public void unbacktrack() {
		if (label instanceof MetaTTRLabel)
		{
			
		}
		
	}

	public boolean canBacktrack() {
		if (label instanceof MetaTTRLabel)
		{
			return ((MetaTTRLabel)label).canBacktrack();
		}
		
		return false;
	}

	/**
	 * 
	 * @return true if this field is meta, i.e. if its label is a {@link MetaTTRLabel}.
	 */
	public boolean isMeta() {
		return label instanceof MetaTTRLabel;
	}

}
