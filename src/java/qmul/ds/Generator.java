/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import qmul.ds.action.Grammar;
import qmul.ds.action.LexicalAction;
import qmul.ds.action.Lexicon;
import qmul.ds.gui.TTRGeneratorGUI;
import qmul.ds.gui.ParserPanel;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.FormulaLabel;
import qmul.ds.formula.*;

/**
 * A generic generator (surface realiser)
 * 
 * @author mpurver
 */
public abstract class Generator<T extends ParserTuple> {

	private static Logger logger = Logger.getLogger(Generator.class);

	protected GenerationState<T> state;
	protected Parser<T> parser;
	
	
	protected ParserPanel gui;
	protected TTRGeneratorGUI genGui;
	
	
	
	
	public String[] interregna = { "uh", "I mean", "sorry", "rather" };

	public Generator() {
		// dummy constructor
	}

	public void setGoal(TTRRecordType goal) {
		state.setGoal(goal);
	}
	public Generator(Parser<T> parser) {
		state = new GenerationState<T>();
		state.add(new GeneratorTuple<T>(parser.getState()));
		//stateHistory = new Vector<GenerationState<T>>();
		this.parser = parser;
	}

	public Generator(Lexicon lexicon, Grammar grammar) {
		state = new GenerationState<T>();
		parser = getParser(lexicon, grammar);
	}

	/**
	 * @param resourceDir
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public Generator(File resourceDir) {
		this(new Lexicon(resourceDir), new Grammar(resourceDir));
	}

	/**
	 * @param resourceDirNameOrURL
	 *            the dir containing computational-actions.txt, lexical-actions.txt, lexicon.txt
	 */
	public Generator(String resourceDirNameOrURL) {
		this(new Lexicon(resourceDirNameOrURL), new Grammar(resourceDirNameOrURL));
		
	}

	/**
	 * @param gui
	 *            the gui to set
	 */
	
	public void setGui(ParserPanel gui) {
		this.gui = gui;
	}

	

	/**
	 * Reset the generator state to the initial (axiom) state
	 */
	public void init() {
		state.clear();
		parser.init();
		state.add(new GeneratorTuple<T>(parser.getState()));
	}

	/**
	 * @param lexicon
	 * @param grammar
	 * @return a {@link Parser} suitable for this implementation
	 */
	public abstract Parser<T> getParser(Lexicon lexicon, Grammar grammar);

	/**
	 * @return a shallow copy of the current state
	 */
	public GenerationState<T> getState() {
		
		return state;
	}

	/**
	 * print the current state
	 */
	public void printState() {
		logger.info("\nNew state (" + state.size() + "):");
		for (GeneratorTuple<T> tuple : state) {
			logger.info(tuple);
		}
		logger.info("");
	}

	// essentially a breadth-first algorithm with incremental semantic filter, returns a generator state with all
	// possible trees, not just the best
	public boolean generateNextWord() {
		GenerationState<T> oldState = state.clone();
		
		boolean success=false;
		state.clear();
		int count = 0;
		// each tuple is a candidate partial string with its state
		for (GeneratorTuple<T> tuple : oldState) {
			logger.debug("Checking tuple " + tuple);
			
			for (String word : parser.getLexicon().keySet()) { // for now forgetting about sublexicon... iterating through whole lexicon

				ParseState<T> oldPState = tuple.getParseState().clone();
				logger.debug("Extending tuple with word " + word + " = " + oldPState.size());
				// logger.info("LEXICON" + parser.getLexicon());
				parser.parseWord(oldPState, word);
				logger.debug("Extension with word " + word + " = " + oldPState.size());
				
				ParseState<T> newPState = oldPState.subsumes(state.getGoal());
				logger.debug("Subsumption with word " + word + " = " + newPState.size());
				if (!newPState.isEmpty()) {
					logger.debug("Extended tuple with word " + word);
					ArrayList<String> newString = new ArrayList<String>(tuple.getString());
					newString.add(word);
					state.add(new GeneratorTuple<T>(newString, newPState));
					logger.debug("Added new tuple " + newString + " " + newPState.size());
					success=true;
					
					JOptionPane.showMessageDialog(null, newString);
					
					count++;
					if (gui != null) {
						gui.addGeneratorOutput(newString.toString());

					}
					try {
						if (genGui != null) {
							genGui.addGeneratorOutput(" \" " + word + " \" ");
							genGui.addGeneratorOutput(newString.toString() + " \" " + word + " \" ");
							float l = 50 / newString.size();
							String s = "";
							for (int x = 0; x < (int) l; x++) {
								s = s + "    ";
							}
							// genGui.addGeneratorOutput(s);
							// genGui.update();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					/*
					if (newPState.matched()) {

						logger.info("\n\n MATCH! :" + newString.toString() + System.currentTimeMillis());
						// JOptionPane.showMessageDialog(null, "MATCH!" + newString.toString());

						setMatch(true);
						newPState.setMatched(false);
						// state.clear();
						// break;

					}*/
					
				} else {
					logger.debug("Failed to extend tuple with word " + word);

				}
			}
		}
		//genGui.addGeneratorOutput("\n");
		return success;
	}

	/**
	 * Generate a sentence for a given goal tree
	 * 
	 * @param tree
	 * @return true if successful
	 */
	public boolean generate(Tree tree) {
		state.setGoal(tree);
		logger.info("Starting generation with goal tree:");
		logger.info(tree);

		int i = 1;

		while (generateNextWord()) {			
			logger.info("\n\n" + "word number " + i + "--------------------");
			logger.info("Gen next word");			
			i++;
		}

		logger.info("Finished generation.");
		return successful();
	}
	
	/**
	 * Generate a sentence for a given goal tree
	 * 
	 * @param tree
	 * @return true if successful
	 */
	public boolean generate() {
		
		logger.info("Starting generation with goal:");
		logger.info(state.getGoal());

		int i = 1;

		while (generateNextWord()) {			
			logger.info("\n\n" + "word number " + i + "--------------------");
			logger.info("Gen next word");			
			i++;
		}

		logger.info("Finished generation.");
		return successful();
	}

	
	
	/**
	 * @return true if the current state is non-null and non-empty
	 */
	protected boolean successful() {
		return ((state != null) && !state.isEmpty());
	}
	
	

	
	

	/**
	 * @return the "best" tree in the current state (where "best" is defined by the natural ordering of the {@link Tree}
	 *         implementation used), or null if the state is empty
	 */
	public List<String> getBestString() {
		if (!successful()) {
			return null;
		}
		return state.first().getString();
	}

	public void init(ParserTuple tuple) {
		init();
		state.setGoal(tuple);		
	}

}
