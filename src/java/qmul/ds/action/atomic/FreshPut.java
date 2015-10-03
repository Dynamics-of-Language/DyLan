/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action.atomic;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.ParserTuple;
import qmul.ds.action.meta.MetaFormula;
import qmul.ds.formula.Formula;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.FormulaLabel;

/**
 * This represents the DS freshput action, taking 2 arguments, i) A meta-variable and ii)a variable type. The type
 * should be one of 'event', 'entity' or 'prop'. Upon execution the metavariable gets set to a fresh formula(variable)
 * of the specified type. This is so that the same variable can later participate in e.g. a scope requirement which is
 * to be put on the tree. In an action spec, e.g.
 * 
 * freshput(entity, S) . . put(?Sc(S))
 * 
 * @author Arash
 * 
 */

public class FreshPut extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String FUNCTOR = "freshput";
	public final static String FRESHPUT_METAVARIABLE_PATTERN = "[S-U]";
	public static final Pattern FRESH_PUT_PATTERN = Pattern.compile(FUNCTOR + "\\((" + FRESHPUT_METAVARIABLE_PATTERN
			+ ")\\s*,\\s*(.+)\\)");
	public static final String[] possibleVarTypes = { "event", "prop", "entity" };
	MetaFormula var;
	String type;

	public FreshPut(String Type, Formula f) {
		this.type = type;
		this.var = var;

	}

	public FreshPut(String line) {

		Matcher m = FRESH_PUT_PATTERN.matcher(line);
		if (m.matches()) {
			List<String> types = Arrays.asList(possibleVarTypes);

			var = (MetaFormula) Formula.create(m.group(1));
			type = m.group(2);
			if (!types.contains(type)) {
				type = null;
				throw new IllegalArgumentException("Unrecognised Variable Type in freshput pattern:" + line);
			}

		} else
			throw new IllegalArgumentException("Unrecognised freshput pattern: " + line);

	}

	@Override
	public <T extends Tree> T execTupleContext(T tree, ParserTuple context) {

		Formula freshVar;
		if (type.equalsIgnoreCase("event")) {
			freshVar = tree.getFreshEventVariable();
		} else if (type.equalsIgnoreCase("prop")) {
			freshVar = tree.getFreshPropositionVariable();

		} else {
			freshVar = tree.getFreshEntityVariable();
		}
		var.getMeta().reset();
		var.equals(freshVar);

		tree.put(new FormulaLabel(var.instantiate()));

		return tree;
	}

	public String toString() {
		return FUNCTOR + "(" + var.getMeta().getName() + "," + type + ")";

	}

	public Effect instantiate() {

		var.getMeta().reset();
		return this;
	}

	public int hashCode() {
		int prime = 17;
		int result = 1;
		result = prime * result + this.type.hashCode();
		result = prime * result + this.var.getMeta().getName().hashCode();
		return result;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof FreshPut))
			return false;

		FreshPut other = (FreshPut) o;
		return this.type.equals(other.type) && this.var.getMeta().getName().equals(other.var.getMeta().getName());
	}

}
