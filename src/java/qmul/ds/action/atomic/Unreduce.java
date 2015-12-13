/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action.atomic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.Context;
import qmul.ds.ParserTuple;
import qmul.ds.dag.DAGEdge;
import qmul.ds.dag.DAGTuple;
import qmul.ds.tree.Modality;
import qmul.ds.tree.Node;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.tree.label.Requirement;
import qmul.ds.tree.label.TypeLabel;
import qmul.ds.type.DSType;

/**
 * The <tt>go</tt> action
 * 
 * @author mpurver
 */
public class Unreduce extends Effect {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String FUNCTOR = "unreduce";

	/**
	 * @param modality
	 */
	public Unreduce() {
		
	}

	private static final Pattern UNREDUCE_PATTERN = Pattern.compile("(?i)" + FUNCTOR);

	/**
	 * @param string
	 *            a {@link String} representation e.g. go(/\1) as used in lexicon specs
	 */
	public Unreduce(String string) {
		Matcher m = UNREDUCE_PATTERN.matcher(string);
		if (!m.matches()) {
		
			throw new IllegalArgumentException("unrecognised unreduce string:" + string);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see qmul.ds.action.atomic.Effect#exec(qmul.ds.tree.Tree, qmul.ds.ParserTuple)
	 */
	public <E extends DAGEdge, U extends DAGTuple, T extends Tree> T exec(T tree, Context<U,E> context)
	{
		
		NodeAddress cur=tree.getPointer();
		Modality up=Modality.parse("</\\>");
		do{
			cur=cur.go(up);
			Node curNode=tree.get(cur);
			DSType type=curNode.getType();
			if (type==null)
			{
				return tree;
			}
			TypeLabel tyl=curNode.getTypeLabel();
			FormulaLabel fol=curNode.getFormulaLabel();
			if (tyl!=null)
				curNode.remove(tyl);
			if (fol!=null)
				curNode.remove(fol);
			curNode.add(new Requirement(new TypeLabel(type)));
				
			
		}while(!cur.equals(new NodeAddress("0")));
		
		return tree;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return FUNCTOR;
	}

	public Effect instantiate() {
		return this;
	}

	@Override
	public <T extends Tree> T execTupleContext(T tree, ParserTuple context) {
		return this.exec(tree, null);
	}
}
