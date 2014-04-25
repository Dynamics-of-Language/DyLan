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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.ContextParser;
import qmul.ds.ParserTuple;
import qmul.ds.action.meta.MetaModality;
import qmul.ds.tree.BasicOperator;
import qmul.ds.tree.Modality;
import qmul.ds.tree.Node;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;
import qmul.ds.type.DSType;

/**
 * The <tt>goLocalEvent</tt> action.. format: goLocalEvent(Some modality metavariable), e.g. goLocalEvent(Z) will take
 * pointer to the local (not across link) event node.... will instantiate its meta variable to the modality that would
 * take the pointer back to where it was from the event node... so if we are at e.g. 010L110, the pointer will go to,
 * 010L0, and the meta will be instantiated to </\0\/1\/1\/0>
 * 
 * @author arash
 */
public class GoLocalEvent extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String FUNCTOR = "goLocalEvent";

	MetaModality meta;

	/**
	 * @param modality
	 */
	public GoLocalEvent(MetaModality m) {
		this.meta = m;
	}

	private static final Pattern GO_LOCAL_EVENT_PATTERN = Pattern.compile("(?i)" + FUNCTOR + "\\((.+)\\)");

	/**
	 * @param string
	 *            a {@link String} representation e.g. go(/\1) as used in lexicon specs
	 */
	public GoLocalEvent(String string) {
		Matcher m = GO_LOCAL_EVENT_PATTERN.matcher(string);
		if (m.matches()) {
			Matcher m1 = Modality.META_MODALITY_PATTERN.matcher(m.group(1));
			if (m1.matches())
				meta = MetaModality.get(m.group(1));
			else
				throw new IllegalArgumentException("unrecognised metamodality var:" + m.group(1) + " in goLocalEvent");
		} else {
			throw new IllegalArgumentException("unrecognised goLocalEvent string:" + string);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.atomic.Effect#exec(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	@Override
	public <T extends Tree> T exec(T tree, ParserTuple context) {
		Node currentNode = tree.getPointedNode();
		String curAddress = tree.getPointer().getAddress();
		ArrayList<BasicOperator> opList = new ArrayList<BasicOperator>();
		DSType type = currentNode.getRequiredType() != null ? currentNode.getRequiredType() : currentNode.getType();

		while (!type.equals(DSType.t)) {
			if (curAddress.endsWith(BasicOperator.PATH_LINK))
				return null;

			opList.add(new BasicOperator(BasicOperator.ARROW_UP + curAddress.substring(curAddress.length() - 1)));
			curAddress = curAddress.substring(0, curAddress.length() - 1);
			currentNode = tree.get(new NodeAddress(curAddress));
			type = currentNode.getRequiredType() != null ? currentNode.getRequiredType() : currentNode.getType();
		}
		opList.add(BasicOperator.DOWN_0);
		curAddress = curAddress + BasicOperator.PATH_0;
		Modality m = new Modality(false, opList);
		Modality inverse = m.inverse();
		this.meta.equals(inverse);
		inverse.equals(meta);
		tree.setPointer(new NodeAddress(curAddress));
		return tree;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR + "(" + meta + ")";
	}

	public Effect instantiate() {
		meta.getMeta().reset();
		return this;
	}

	public static void main(String a[]) {
		GoLocalEvent goLocal = new GoLocalEvent("goLocalEvent(Z)");
		ContextParser parser = new ContextParser("resource/2013-english-ttr");
		parser.init();
		String sent = "a man";
		String[] sentArray = sent.split("\\s");
		List<String> sentList = Arrays.asList(sentArray);
		parser.parseWords(sentList);
		Tree complete = parser.getBestParse();
		System.out.println(complete);
		goLocal.exec(complete, null);
		System.out.println(goLocal);
	}
}
