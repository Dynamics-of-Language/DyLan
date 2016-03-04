/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.tree;

import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import qmul.ds.action.meta.MetaElement;
import qmul.ds.formula.Formula;
import qmul.ds.formula.Variable;
import qmul.ds.tree.label.ExistentialLabelConjunction;
import qmul.ds.tree.label.FeatureLabel;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.LabelFactory;
import qmul.ds.tree.label.Requirement;
import qmul.ds.tree.label.TypeLabel;
import qmul.ds.type.DSType;

/**
 * A DS {@link Tree} node
 * 
 * @author mpurver
 */
@SuppressWarnings("serial")
public class Node extends TreeSet<Label> {

	private static Logger logger = Logger.getLogger(Node.class);

	public static final String ADDRESS_SEPARATOR = ":";

	private NodeAddress address;

	public Node(NodeAddress address) {
		super();
		this.address = address;
	}

	public Node(Node node) {
		super(node);
		this.address = node.address;
	}

	/**
	 * @return the address
	 */
	public NodeAddress getAddress() {
		return address;
	}

	/**
	 * @param label
	 * @return true if this {@link Node} is labelled with this {@link Label} (allowing {@link MetaElement} matching)
	 */
	public boolean hasLabel(Label label) {

		return contains(label);

	}

	public boolean contains(Object label) {

		for (Label l : this) {

			if (label.equals(l) || l.equals(label)) {
				return true;
			}

		}
		return false;

	}

	/**
	 * @param labels
	 * @return true if this {@link Node} is labelled with every {@link Label} (allowing {@link MetaElement} matching)
	 */
	public boolean hasLabels(Collection<Label> labels) {
		for (Label l : labels) {
			if (!hasLabel(l)) {
				return false;
			}
		}
		return true;
	}

	public boolean hasRequirement(Label label) {
		return contains(LabelFactory.getRequirement(label));
	}

	public boolean hasType(DSType type) {
		return contains(LabelFactory.get(type));
	}

	public boolean hasType() {
		for (Label l : this) {
			if (l instanceof TypeLabel)
				return true;
		}
		return false;
	}

	public boolean hasRequirement(DSType type) {
		return contains(LabelFactory.getRequirement(type));
	}

	/**
	 * @param label
	 * @return false if the label was already present, overwrites existing formula and type labels with warning.
	 */
	public boolean addLabel(Label label) {

		if (!(label instanceof FormulaLabel || label instanceof TypeLabel))
			return add(label);

		Label present = labelOfType(label);
		if (present != null) {
			logger.warn("overwriting node with label " + label + "; " + present + " was removed");
			remove(present);
		}
		return add(label);
	}

	public Label labelOfType(Label label) {
		for (Label l : this) {
			if (l.getClass().equals(label.getClass()))
				return l;
		}
		return null;
	}

	/**
	 * Overriding TreeMap add(Label). The label to be added needs to be compared with every element in the set via
	 * equals() as this is what instantiates Labels involving meta variables.
	 * 
	 * @param label
	 * @return false if the label was already present
	 */
	@Override
	public boolean add(Label label) {
		for (Label l : this) {
			if (l.equals(label) || label.equals(l))
				return false;
		}
		super.add(label);
		return true;
	}

	/**
	 * @param label
	 * @return false if the label was not already present
	 */
	public boolean removeLabel(Label label) {

		return remove(label);
	}

	public boolean remove(Label label) {
		for (Label l : this) {
			if (l.equals(label) || label.equals(l))
				return super.remove(l);
		}
		return false;
	}

	/**
	 * @param label
	 * @return false if the requirement was not already present
	 */
	public boolean removeRequirement(Label label) {
		return remove(LabelFactory.getRequirement(label));
	}

	/**
	 * Add a new type requirement label
	 * 
	 * @param type
	 * @return false if the {@link Requirement} was already present, true otherwise
	 */
	public boolean addRequirement(DSType type) {
		return add(LabelFactory.getRequirement(type));
	}

	/**
	 * Remove the requirement for this type
	 * 
	 * @param type
	 * @return false if the requirement was not already present
	 */
	public boolean removeRequirement(DSType type) {
		return remove(LabelFactory.getRequirement(type));
	}

	/**
	 * @param other
	 * @param modality
	 * @return can we get from this node to other via modality?
	 */
	/*
	 * public boolean to(Node other, Modality modality) { return getAddress().to(other.getAddress(), modality); }
	 */
	/**
	 * @return the type of this node if specified, null otherwise
	 */
	public DSType getType() {
		for (Label l : this) {
			if (l instanceof TypeLabel) {
				return ((TypeLabel) l).getType();
			}
		}
		return null;
	}

	public Requirement getTypeRequirement() {
		for (Label l : this) {
			if (l instanceof Requirement) {
				Requirement r = (Requirement) l;
				if (r.getLabel() instanceof TypeLabel)
					return r;

			}
		}
		return null;

	}

	/**
	 * @return the type requirement for this node if specified, null otherwise
	 */
	public DSType getRequiredType() {
		for (Label l : this) {
			if (l instanceof Requirement) {
				Requirement r = (Requirement) l;
				if (r.getLabel() instanceof TypeLabel) {
					return ((TypeLabel) r.getLabel()).getType();
				}
			}
		}
		return null;
	}

	/**
	 * @return the formula of this node if specified, null otherwise
	 */
	public Formula getFormula() {
		for (Label l : this) {

			if (l instanceof FormulaLabel) {
				return ((FormulaLabel) l).getFormula();
			}
		}
		return null;
	}

	/**
	 * @return the formula requirement for this node if specified, null otherwise
	 */
	public Formula getRequiredFormula() {
		for (Label l : this) {
			if (l instanceof Requirement) {
				Requirement r = (Requirement) l;
				if (r.getLabel() instanceof FormulaLabel) {
					return ((FormulaLabel) r.getLabel()).getFormula();
				}
			}
		}
		return null;
	}

	/**
	 * @return true if this node is complete i.e. no outstanding requirements
	 */
	public boolean isComplete() {
		for (Label l : this) {
			if (l instanceof Requirement) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return true if this node is on a linked tree
	 */
	public boolean isLinked() {
		if (this.getAddress().toString().contains("L")) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @return true if this node's address is fully fixed, i.e. contains no U or * in its {@link NodeAddress}
	 */
	public boolean isFixed() {
		return address.isFixed();
	}

	/**
	 * @return true if this node is fixed to some mother, i.e. that its {@link NodeAddress} does not end with * or U
	 */
	public boolean isLocallyFixed() {
		return address.isLocallyFixed();
	}

	/**
	 * @return true if this is the root node
	 */
	public boolean isRoot() {
		return address.isRoot();
	}

	/**
	 * @param other
	 * @return true if this node subsumes other i.e. nodeaddress subsumes other's address and each {@link Label}
	 *         subsumes some label in other
	 */
	public boolean subsumes(Node other) {
		if (other == null) {
			logger.error("other node is null when checking node subsumption");
		}
		if (!address.subsumes(other.address)) {

			logger.debug("Address failed in subsumption");
			logger.debug("Address " + address + " does not subsume " + other.address);

			return false;
		}
		
		logger.debug("checking " + this + " subsumes " + other);
		label: for (Label thisLabel : this) {

			for (Label otherLabel : other) {
				//

				if (thisLabel.subsumes(otherLabel)) {
					continue label;
				}

				else {
					if (thisLabel instanceof FormulaLabel && otherLabel instanceof FormulaLabel) {
						FormulaLabel fl = (FormulaLabel) thisLabel;
						FormulaLabel otherF = (FormulaLabel) otherLabel;
						logger.debug("This formula:" + fl.getFormula() + ":" + fl.getFormula().getClass() + " vs "
								+ otherF.getFormula() + ":" + otherF.getFormula().getClass());
					}
				}
			}
			// if this label is an existential requirement, then it subsumes no particular label on other, but
			// this node will subsume other if the existential requirement is actually satisfied on the other (node) and
			// all the rest of the labels here subsume some label on the other node.
			if (thisLabel instanceof Requirement) {
				Requirement require = (Requirement) thisLabel;
				if (require.getLabel() instanceof ExistentialLabelConjunction) {
					ExistentialLabelConjunction exLabel = (ExistentialLabelConjunction) require.getLabel();
					if (exLabel.check(other)) {
						// System.out.println("Label "+exLabel+" true on "+other);
						continue label;
					}
				}
				if (require.getLabel() instanceof FeatureLabel)
				{
					//this is a HACK to make ?+eval work with induction.....
					continue label;
				}
			}
			logger.debug("subsumption fail at " + address + " for label " + thisLabel + " vs " + other);
			if (thisLabel instanceof FormulaLabel) {
				FormulaLabel fl = (FormulaLabel) thisLabel;
				// System.out.println("This formula:" + fl.getFormula() + ":" + fl.getFormula().getClass()+" vs " +
				// other);
			}
			return false;
		}
		return true;
	}

	/**
	 * @param other
	 * @return true if this node is unifiable with other i.e. the addresses are compatible, and there are no
	 *         incompatible {@link DSType} or {@link Formula} labels TODO add semantic feature checks too
	 */
	public boolean isUnifiable(Node other) {
		
		if (!other.getAddress().subsumes(getAddress()))
		{
			
			logger.debug("address subsumption failed. this:"+this.getAddress()+"other:"+other.getAddress());
			return false;
		}else logger.debug(other.getAddress()+" subsumes "+getAddress());
		DSType t = getType();
		if (t == null)
			t = getRequiredType();
		
		if (t != null) {
			DSType ot = other.getType();
			if (ot == null)
				ot = other.getRequiredType();
			if ((ot != null) && !t.equals(ot)) {
				return false;
			}
		}
		
//		Formula f = getFormula();
//		if (f == null)
//			f = getRequiredFormula();
//		
//		if (f != null) {
//			Formula of = other.getFormula();
//			if (of == null)
//				of = other.getRequiredFormula();
//			if ((of != null) && !f.subsumes(of)) {
//				System.out.println("Formula subsumption failed");
//				return false;
//			}
//		}
		return true;
	}
	
	
	public TypeLabel getTypeLabel()
	{
		for(Label l:this)
		{
			if (l instanceof TypeLabel)
				return (TypeLabel)l;
		}
		return null;
	}
	
	public FormulaLabel getFormulaLabel()
	{
		for(Label l:this)
		{
			if (l instanceof FormulaLabel)
				return (FormulaLabel)l;
		}
		return null;
	}
	/**
	 * Merge this node with other, i.e. add all its labels
	 * 
	 * @param other
	 */
	public void merge(Node other) {
		
		
		
		addAll(other);
	}
	
	
	
	public void removeFormulaLabel()
	{
		for(Label l: this)
		{
			if (l instanceof FormulaLabel)
				remove(l);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Node clone() {
		return new Node(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((address == null) ? 0 : address.hashCode());
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
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return address + ADDRESS_SEPARATOR + super.toString();
	}

	/**
	 * @return a prettier string for use in GUIs
	 */
	public String toUnicodeString() {
		String prefix = "";
		String str = "{";
		for (Label l : this) {

			str += (str.length() > 1 ? ", " : "") + l.toUnicodeString();

		}
		str += "}";
		return prefix + address + ADDRESS_SEPARATOR + str;
	}

	

}
