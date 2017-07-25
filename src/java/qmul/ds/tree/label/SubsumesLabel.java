/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.tree.label;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.action.boundvariable.BoundFormulaVariable;
import qmul.ds.action.meta.Meta;
import qmul.ds.action.meta.MetaFormula;
import qmul.ds.formula.Formula;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.tree.Node;

public class SubsumesLabel extends Label implements Serializable {

	/**
	 * Label for checking formula subsumption in actions
	 * X << Y means Y subsumes X
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String FUNCTOR="<<";
	public static final Pattern SUBSUMES_PATTERN = Pattern.compile("(.+)\\s*"+FUNCTOR+"\\s*(.+)");
	Formula left;
	Formula right;

	public SubsumesLabel(String s, IfThenElse ite) {
		super(ite);
		
		Matcher m = SUBSUMES_PATTERN.matcher(s);
		if (m.matches()) {
			
			left = Formula.create(m.group(1));
			right = Formula.create(m.group(2));

		} else
			throw new IllegalArgumentException("Unrecognized equality label: " + s);
	}

	public SubsumesLabel(String s) {
		this(s, null);
	}


	public boolean check(Node n) {

		
		boolean success=right.subsumes(left);
		

		return success;
	}

	public String toString() {
		return left + FUNCTOR + right;
	}

	public String toUnicodeString() {
		return left.toUnicodeString() + FUNCTOR + right.toUnicodeString();
	}

	public ArrayList<Meta<?>> getMetas() {
		//TODO: this is a hack. To deal with it after TTRRecordType as a meta element is 
		//fully integrated in IfThenElse
		
		ArrayList<Meta<?>> result = new ArrayList<Meta<?>>();
		if (left instanceof MetaFormula)
			result.addAll(((MetaFormula) left).getMetas());
		if (right instanceof MetaFormula)
			result.addAll(((MetaFormula) right).getMetas());

		return result;
	}
	
	public void resetMetas()
	{
		left.resetMetas();
		right.resetMetas();
	}

	public ArrayList<Meta<?>> getBoundMetas() {
		ArrayList<Meta<?>> result = new ArrayList<Meta<?>>();
		if (left instanceof BoundFormulaVariable)
			result.addAll(((BoundFormulaVariable) left).getBoundMetas());
		if (right instanceof BoundFormulaVariable)
			result.addAll(((BoundFormulaVariable) right).getBoundMetas());

		return result;
	}
	
	public static void main(String s[])
	{
		TTRRecordType rt=TTRRecordType.parse("[x2==this : e|e4==eq : es|x4 : e|head==e4 : es|p6==red(x4) : t|p7==color(p6) : t|p4==obj(e4, x4) : t|p5==subj(e4, x2) : t]");
		TTRRecordType rt1=TTRRecordType.parse("[x:e|e1==eq:es|x2:e|p2==Q(x2):t|p3==color(p2):t|p==subj(e1,x):t|p1==obj(e1,x2):t]");
		
		
		System.out.println(rt1.subsumesBasic(rt));
		
		System.out.println(rt1);
		System.out.println(rt1.subsumes(rt));
		System.out.println(rt1);
		
		
		
	}
	
	

}
