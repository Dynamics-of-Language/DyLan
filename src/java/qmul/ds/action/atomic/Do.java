/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action.atomic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.ParserTuple;
import qmul.ds.action.Action;
import qmul.ds.action.meta.MetaActionSequence;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.LabelFactory;

public class Do extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String FUNCTOR = "do";
	String metaActionSequenceName;

	private static final Pattern DO_PATTERN = Pattern.compile(FUNCTOR + "\\((" + LabelFactory.METAVARIABLE_PATTERN
			+ ")\\)");

	public Do(String s) {
		Matcher m = DO_PATTERN.matcher(s);
		if (m.matches()) {
			this.metaActionSequenceName = m.group(1);
		} else
			throw new IllegalArgumentException("unrecognised do action: " + s);
	}

	@Override
	public <T extends Tree> T exec(T tree, ParserTuple context) {

		logger.debug("rerunning actions from context");
		logger.debug("getting metasequence named:" + this.metaActionSequenceName);
		ArrayList<Action> actions = MetaActionSequence.get(this.metaActionSequenceName).instantiate();
		logger.debug("got back:" + actions);

		Iterator<Action> i = actions.iterator();

		while (i.hasNext()) {
			Action a = i.next();
			logger.debug("rerunning: " + a);
			a.exec(tree, context);
			if (tree == null) {
				logger.debug("Action in do failed:" + a);
			} else {
				logger.debug("result: " + tree);
			}

		}

		return tree;
	}

	public String toString() {
		return FUNCTOR + "(" + this.metaActionSequenceName + ")";
	}

	public Effect instantiate() {
		// TODO:for now just return this. but really should instantitate all those actions in the MetaActionSequence's
		// value
		return this;
	}
}
