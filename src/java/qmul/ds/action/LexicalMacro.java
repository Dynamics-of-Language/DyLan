/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action;

import java.util.ArrayList;
import java.util.List;

import qmul.ds.ParserTuple;
import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.EffectFactory;
import qmul.ds.tree.Tree;

public class LexicalMacro extends Effect {

	String name;
	List<Effect> actions;

	public LexicalMacro(String name, ArrayList<Effect> l) {
		this.name = name;
		this.actions = l;
	}

	// lines here have been instantiated
	public LexicalMacro(String name, List<String> lines) {
		actions = new ArrayList<Effect>();
		// logger.debug("creating lexical macro from:"+lines);
		this.name = name;
		for (String line : lines) {
			actions.add(EffectFactory.create(line.trim()));

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.atomic.Effect#exec(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	@Override
	public <T extends Tree> T execTupleContext(T tree, ParserTuple context) {

		for (Effect e : actions) {
			e.execTupleContext(tree, context);
			if (tree == null) {
				logger.debug("Action in macro call" + name + " failed:" + e);
			} else {
				logger.debug("result: " + tree);
			}
		}
		return tree;
	}

	public String toString() {
		return name;
	}

	public List<Effect> getActions() {
		return actions;
	}

	public Effect instantiate() {
		ArrayList<Effect> as = new ArrayList<Effect>();
		for (Effect e : this.actions) {
			as.add(e.instantiate());
		}
		return new LexicalMacro(this.name, as);
	}
}
