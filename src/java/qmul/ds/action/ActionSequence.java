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

import qmul.ds.action.meta.MetaActionSequence;

/**
 * A sequence of {@link Action}s
 * 
 * @author mpurver, arash
 */
@SuppressWarnings("serial")
public class ActionSequence extends ArrayList<Action> {

	/**
	 * @return an instantiated version of this {@link ActionSequence}, with all meta-elements replaced by their values.
	 *         By default, just return this {@link ActionSequence} unchanged. This will be overridden by
	 *         {@link MetaActionSequence}s and the like
	 */
	public ActionSequence instantiate() {
		return this;
	}

	public String toString() {
		String result = "<<\n";
		for (Action a : this) {
			result += a.toString() + "\n";
		}
		// if (isEmpty())
		// result += "null";
		result += "\n>>";
		return result;
	}

}
