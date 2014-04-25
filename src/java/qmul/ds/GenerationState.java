/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds;

import java.util.Collection;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import qmul.ds.formula.TTRFormula;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.tree.Tree;

/**
 * A generation state: a goal {@link ParserTuple} (i.e. containing a tree or a semantics) and a set of {@link GeneratorTuple}s. Members should be ordered (by their
 * natural ordering) best-first
 * 
 * @author mpurver
 * 
 * @param <T>
 */
@SuppressWarnings("serial")
public class GenerationState<T extends ParserTuple> extends TreeSet<GeneratorTuple<T>> implements Cloneable {

	private static Logger logger = Logger.getLogger(GenerationState.class);	
	
	
	protected ParserTuple goal;
	
	public GenerationState() {
		super();
	}

	public GenerationState(Collection<GeneratorTuple<T>> c) {
		super(c);
	}

	public GenerationState(Tree goalTree) {
		super();
		this.goal = new ParserTuple(goalTree); 
	}

	public GenerationState(Tree goalTree, Collection<GeneratorTuple<T>> c) {
		super(c);
		this.goal = new ParserTuple(goalTree);
	}

	/**
	 * @return the goalTree
	 */
	public ParserTuple getGoal() {
		return goal;
	}

	/**
	 * @param goalTree
	 *            the goalTree to set
	 */
	public void setGoal(Tree goalTree) {
		this.goal = new ParserTuple(goalTree);
	}

	public GenerationState(TTRRecordType goalTTR) {
		super();
		this.goal = new ParserTuple(goalTTR);
	}

	public GenerationState(TTRRecordType goalTTR, Collection<GeneratorTuple<T>> c) {
		super(c);
		this.goal = new ParserTuple(goalTTR);
	}

	

	private GenerationState(ParserTuple goal2, GenerationState<T> generationState) {
		super(generationState);
		this.goal=new ParserTuple(goal2);
	}

	/**
	 * @param goalTTR
	 *            the goalTTR to set
	 */
	public void setGoal(TTRRecordType goalTTR) {
		this.goal = new ParserTuple(goalTTR);
	}

	/**
	 * @return true if this state contains at least one complete {@link GeneratorTuple}
	 */
	public boolean hasCompleteTuple() {
		for (GeneratorTuple<T> tuple : this) {
			if (tuple.hasCompleteTree()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.HashSet#clone()
	 */
	public GenerationState<T> clone() {
		return new GenerationState<T>(goal, this);
	}

	public void setGoal(ParserTuple tuple) {
		this.goal=goal;
		
	}

	

}
