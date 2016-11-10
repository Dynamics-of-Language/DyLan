/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.tree.label;

import java.util.HashMap;

import qmul.ds.action.ActionSequence;
import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.EffectFactory;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.boundvariable.BoundLabelVariable;
import qmul.ds.action.meta.MetaLabel;
import qmul.ds.formula.Formula;
import qmul.ds.tree.BasicOperator;
import qmul.ds.tree.Modality;
import qmul.ds.tree.NodeAddress;
import qmul.ds.type.DSType;

public class LabelFactory {
	// REC reserved for MetaTTRFormulae
	public final static String METAVARIABLE_PATTERN = "[V-Z]|" + ExistentialLabelConjunction.metaVarReplacement;
	public final static String VAR_PATTERN = "[x-z]";
	private static HashMap<DSType, Label> typeLabels = new HashMap<DSType, Label>();

	/**
	 * @param type
	 * @return
	 */
	public static Label get(DSType type) {
		Label label = typeLabels.get(type);
		if (label == null) {
			label = new TypeLabel(type);
			typeLabels.put(type, label);
		}
		return label;
	}

	private static HashMap<Formula, Label> formulaLabels = new HashMap<Formula, Label>();

	/**
	 * @param formula
	 * @return
	 */
	public static Label get(Formula formula) {
		Label label = formulaLabels.get(formula);
		if (label == null) {
			label = new FormulaLabel(formula);
			formulaLabels.put(formula, label);
		}
		return label;
	}

	private static HashMap<String, Label> featureLabels = new HashMap<String, Label>();

	/**
	 * @param feature
	 * @return
	 */
	public static Label get(String feature) {
		Label label = featureLabels.get(feature);
		if (label == null) {
			label = new FeatureLabel(feature);
			featureLabels.put(feature, label);
		}
		return label;
	}

	private static HashMap<Label, Requirement> reqLabels = new HashMap<Label, Requirement>();

	/**
	 * @param label
	 * @return
	 */
	public static Requirement getRequirement(Label label) {
		Requirement req = reqLabels.get(label);
		if (req == null) {
			req = new Requirement(label);
			reqLabels.put(label, req);
		}
		return req;
	}

	public static Requirement getRequirement(DSType type) {
		return getRequirement(get(type));
	}

	public static Requirement getRequirement(Formula formula) {
		return getRequirement(get(formula));
	}

	public static Requirement getRequirement(String feature) {
		return getRequirement(get(feature));
	}

	public static Requirement getRequirement(Modality modality, DSType type) {
		return getRequirement(new ModalLabel(modality, get(type)));
	}

	public static Requirement getRequirement(Modality modality, Formula formula) {
		return getRequirement(new ModalLabel(modality, get(formula)));
	}

	public static Requirement getRequirement(Modality modality, String feature) {
		return getRequirement(new ModalLabel(modality, get(feature)));
	}

	/**
	 * @param string
	 *            a {@link String} representation of a label as used in lexicon specs, e.g. Fo(john),
	 *            ?[\/1]Ty(&lt;e,t&gt;) etc
	 * @return the label
	 */
	public static Label create(String string, IfThenElse ite) {
		string = string.trim();
		UnaryPredicateLabel upa = UnaryPredicateLabel.parse(string);
		if (string.matches(METAVARIABLE_PATTERN)) {
			return MetaLabel.get(string);
		} else if (string.matches(VAR_PATTERN)) {
			return new BoundLabelVariable(string);
		} else if (upa != null) {
			return upa;
		}else if (string.toLowerCase().startsWith(CompleteTreeLabel.PREFIX)) {
			return new CompleteTreeLabel();
		} else if (string.toLowerCase().startsWith(ScopeStatement.FUNCTOR.toLowerCase())) {
			return new ScopeStatement(string, ite);
		} else if (string.toLowerCase().startsWith(ScopeDepSaturationLabel.FUNCTOR.toLowerCase())) {
			return new ScopeDepSaturationLabel(string, ite);
		} else if (string.toLowerCase().startsWith(DOMPlusLabel.FUNCTOR.toLowerCase())) {
			return new DOMPlusLabel(string, ite);
		} else if (string.toLowerCase().startsWith(DOMLabel.FUNCTOR.toLowerCase())) {
			return new DOMLabel(string, ite);
		} else if (string.toLowerCase().startsWith(IndefLabel.FUNCTOR.toLowerCase())) {
			return new IndefLabel(string, ite);
		} else if (string.toLowerCase().startsWith(SpeakerLabel.FUNCTOR.toLowerCase())) {
			return new SpeakerLabel(Formula.create(
					string.substring(SpeakerLabel.FUNCTOR.length() + 1, string.length() - 1), true), ite);
		} else if (string.toLowerCase().startsWith(AssertionLabel.FUNCTOR.toLowerCase())) {
			return new AssertionLabel(Formula.create(
					string.substring(AssertionLabel.FUNCTOR.length() + 1, string.length() - 1), true), ite);
		} else if (string.toLowerCase().startsWith(AddresseeLabel.FUNCTOR.toLowerCase())) {
			return new AddresseeLabel(Formula.create(
					string.substring(AddresseeLabel.FUNCTOR.length() + 1, string.length() - 1), true), ite);
		}  else if (string.toLowerCase().startsWith(PrevSpeakerLabel.FUNCTOR.toLowerCase())) {
			return new PrevSpeakerLabel(Formula.create(
					string.substring(PrevSpeakerLabel.FUNCTOR.length() + 1, string.length() - 1), true), ite);
		} else if (string.toLowerCase().startsWith(ContextualActionLabel.FUNCTOR)) {
			return new ContextualActionLabel(string, ite);
		} else if (string.toLowerCase().startsWith(FeatureLabel.PREFIX.toLowerCase())) {
			return new FeatureLabel(string.substring(FeatureLabel.PREFIX.length()), ite);
		} else if (string.toLowerCase().startsWith(ContextualTreeLabel.FUNCTOR.toLowerCase())) {
			return new ContextualTreeLabel(string, ite);
		} else if (string.toLowerCase().startsWith(Requirement.PREFIX.toLowerCase())) {
			return new Requirement(create(string.substring(Requirement.PREFIX.length()), ite));
		} else if (string.toLowerCase().startsWith(AddressLabel.FUNCTOR.toLowerCase())) {
			return new AddressLabel(string.substring(AddressLabel.FUNCTOR.length() + 1, string.length() - 1), ite);
		} else if (string.toLowerCase().startsWith(ScopeLabel.FUNCTOR.toLowerCase())) {
			return new ScopeLabel(string, ite);
		} else if (string.toLowerCase().startsWith(ScopeStatement.FUNCTOR.toLowerCase())) {
			return new ScopeStatement(string, ite);
		} else if (string.toLowerCase().startsWith(TypeLabel.FUNCTOR.toLowerCase())) {
			return new TypeLabel(DSType.parse(string.substring(TypeLabel.FUNCTOR.length() + 1, string.length() - 1)),
					ite);
		} else if (string.toLowerCase().startsWith(AddressSubsumptionLabel.FUNCTOR.toLowerCase())) {
			return new AddressSubsumptionLabel(string.substring(AddressSubsumptionLabel.FUNCTOR.length() + 1,
					string.length() - 1), ite);
		} else if (string.toLowerCase().startsWith(FormulaLabel.FUNCTOR.toLowerCase())) {
			return new FormulaLabel(Formula.create(
					string.substring(FormulaLabel.FUNCTOR.length() + 1, string.length() - 1), true), ite);
		} else if (string.toLowerCase().startsWith(Modality.FORALL_LEFT.toLowerCase())
				|| string.toLowerCase().startsWith(Modality.EXIST_LEFT.toLowerCase())
				|| string.toLowerCase().startsWith(BasicOperator.ARROW_UP.toLowerCase())
				|| string.toLowerCase().startsWith(BasicOperator.ARROW_DOWN.toLowerCase())) {
			return new ModalLabel(string, ite);
		} else if (string.toLowerCase().startsWith(ExistentialLabelConjunction.FUNCTOR.toLowerCase())) {
			return new ExistentialLabelConjunction(string, ite);
		} else if (string.toLowerCase().startsWith(NegatedLabel.FUNCTOR.toLowerCase())) {
			return new NegatedLabel(string, ite);
		} else if (string.toLowerCase().startsWith(BottomLabel.BOTTOM_FUNCTOR.toLowerCase())) {
			return new BottomLabel(ite);
		} else if (string.matches(FormulaEqualityLabel.EQUALITY_PATTERN.pattern())) {
			return new FormulaEqualityLabel(string, ite);
		} else if (string.equals("x")) {
			// special case for existential check of entire labels e.g. Ex.?x
			return new ArbitraryLabel("x", ite);
		} else {
			LabelDisjunction ld = LabelDisjunction.parse(string, ite);
			if (ld != null)
				return ld;

			throw new IllegalArgumentException("Unrecognised label specification " + string);
		}
	}

	/**
	 * The method is used in Existential label to determine the type of the quantified variable
	 * 
	 * @param f
	 *            this is a string that should correspond to the functor of some ds predicate in the implementation,
	 *            e.g. ty, fo etc.
	 * 
	 * @return the class which represents the type of variable that the predicate takes as argument
	 */
	public static Class<?> getClassForPredicate(String functor) {

		functor = functor.trim();

		if (functor.matches(UnaryPredicateLabel.FUNCTOR.pattern())) {
			return PredicateArgument.class;
		} else if (functor.equalsIgnoreCase(ScopeStatement.FUNCTOR)) {
			return Formula.class;
		} else if (functor.equalsIgnoreCase(ContextualActionLabel.FUNCTOR)) {
			return ActionSequence.class;
		} else if (functor.equalsIgnoreCase(AddressLabel.FUNCTOR)) {
			return NodeAddress.class;
		} else if (functor.equalsIgnoreCase(TypeLabel.FUNCTOR)) {
			return DSType.class;
		} else if (functor.equalsIgnoreCase(AddressSubsumptionLabel.FUNCTOR)) {
			return Modality.class;
		} else if (functor.toLowerCase().startsWith(FormulaLabel.FUNCTOR)) {
			return Formula.class;
		} else if (functor.toLowerCase().startsWith(Modality.FORALL_LEFT.toLowerCase())
				|| functor.toLowerCase().startsWith(Modality.EXIST_LEFT.toLowerCase())
				|| functor.toLowerCase().startsWith(BasicOperator.ARROW_UP.toLowerCase())
				|| functor.toLowerCase().startsWith(BasicOperator.ARROW_DOWN.toLowerCase())) {
			return Modality.class;
		} else
			return null;

	}

	/**
	 * This method takes a single string representation of a label, and a bound variable, e.g. x, and returns the type
	 * (class) corresponding to that variable in the label. E.g. fo(x) will result in Formula, and <x> Modality.
	 * 
	 * The method is used in ExistentialLabelConjunction to determine the type of the meta-variable replacing the bound
	 * variable upon label checking.
	 * 
	 * @param labelS
	 * @param var
	 * @return the type or class in the label, of the variable passed as argument.
	 */

	public static Class<?> findVariableType(String labelS, String var) {
		String label = labelS.trim();
		int varIndex = label.lastIndexOf(var);
		if (varIndex < 0)
			return null;
		if (varIndex > 0 && labelS.substring(varIndex - 1, varIndex).matches("[a-zA-Z]"))
			return null;
		else if (varIndex == 0 && labelS.substring(varIndex, varIndex + 1).matches("[a-zA-Z]"))
			return null;
		int leftModE = label.indexOf(Modality.EXIST_LEFT);
		int rightModE = label.indexOf(Modality.EXIST_RIGHT);
		int leftModF = label.indexOf(Modality.FORALL_LEFT);
		int rightModF = label.indexOf(Modality.FORALL_RIGHT);
		if (leftModE >= 0 && rightModE >= 0) {
			if (leftModE < varIndex && rightModE > varIndex)
				return Modality.class;
		}
		if (leftModF >= 0 && rightModF >= 0) {
			if (leftModF < varIndex && rightModF > varIndex)
				return Modality.class;
		}

		int i = varIndex - 1;

		int firstLeft = -1;
		int secondLeft = -1;
		// System.out.println("X index:"+i);
		while (i >= 0) {
			if (label.charAt(i) == '(' && firstLeft < 0) {
				// functor=label.substring(0, i);
				firstLeft = i;
			} else if (label.charAt(i) == '(' || label.charAt(i) == ' ' || label.charAt(i) == '&'
					|| label.charAt(i) == '|' || label.charAt(i) == ',') {
				secondLeft = i;
				break;
			}

			i--;
		}
		// System.out.println("firstLeft:"+firstLeft+" SecondLeft:"+secondLeft);
		if (firstLeft < 0)
			return Label.class;
		else if (secondLeft < 0)
			return getClassForPredicate(label.substring(0, firstLeft));
		else
			return getClassForPredicate(label.substring(secondLeft + 1, firstLeft));
	}

	public static void main(String a[]) {

		FormulaLabel l=(FormulaLabel)LabelFactory.create("Fo(X)");
		System.out.println(l.getFormula().getClass());

	}

	public static Label create(String string) {
		return create(string, null);
	}
}
