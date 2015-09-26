/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * A set of {@link ComputationalAction}s
 * 
 * @author mpurver
 */
public class Grammar extends HashMap<String, ComputationalAction> implements Serializable {

	private static Logger logger = Logger.getLogger(Grammar.class);

	private static final long serialVersionUID = 1L;

	public static final String FILE_NAME = "computational-actions.txt";

	private static final String ALWAYS_GOOD_PREFIX = "*";

	private static final String BACKTRACK_ON_SUCCESS_PREFIX = "+";

	/**
	 * Read a set of {@link ComputationalAction}s from file
	 * 
	 * @param dir
	 *            containing the computational-actions.txt file
	 */
	public Grammar(File dir) {
		File file = new File(dir, FILE_NAME);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			initActions(reader);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			logger.error("Error reading computational actions file " + file.getAbsolutePath());
		}
	}

	/**
	 * Read a set of {@link ComputationalAction}s from file
	 * 
	 * @param dirNameOrURL
	 *            containing the computational-actions.txt file
	 */
	public Grammar(String dirNameOrURL) {
		BufferedReader reader;
		try {
			if (dirNameOrURL.matches("(https?|file):.*")) {
				reader = new BufferedReader(new InputStreamReader(new URL(dirNameOrURL.replaceAll("/?$", "/")
						+ FILE_NAME).openStream()));
			} else {
				File file = new File(dirNameOrURL, FILE_NAME);
				reader = new BufferedReader(new FileReader(file));
			}
			initActions(reader);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e);
			logger.error("Error reading computational actions file " + dirNameOrURL);
		}
	}

	public Grammar() {
		super();
	}

	public Grammar(Grammar nonoptionalGrammar) {
		super(nonoptionalGrammar);
	}

	/**
	 * Read a set of {@link ComputationalAction}s from file
	 * 
	 * @param reader
	 *            containing the computational-actions.txt file data
	 */
	private void initActions(BufferedReader reader) {
		try {
			String line;
			String name = null;
			boolean alwaysGood = false;
			boolean backtrackOnSuccess = false;
			List<String> lines = new ArrayList<String>();
			while ((line = reader.readLine()) != null) {
				line = Lexicon.comment(line.trim());
				if ((line == null) || (line.isEmpty() && lines.isEmpty())) {
					continue;
				}
				if (line.isEmpty() && !lines.isEmpty()) {
					ComputationalAction action = new ComputationalAction(name, lines);
					action.setAlwaysGood(alwaysGood);
					action.setBacktrackOnSuccess(backtrackOnSuccess);
					put(name,action);
					logger.debug("Added as: " + action);
					lines.clear();
					name = null;
					alwaysGood = false;
				} else if (name == null) {
					name = line;
					alwaysGood = name.startsWith(ALWAYS_GOOD_PREFIX);
					if (alwaysGood) {
						name = name.substring(ALWAYS_GOOD_PREFIX.length());
					}
					backtrackOnSuccess = name.startsWith(BACKTRACK_ON_SUCCESS_PREFIX);
					if (backtrackOnSuccess) {
						name = name.substring(BACKTRACK_ON_SUCCESS_PREFIX.length());
					}
					logger.debug("New computational action: " + name + " (" + alwaysGood + "," + backtrackOnSuccess
							+ ")");
				} else {
					lines.add(line);
				}
			}
			if (!lines.isEmpty()) {
				ComputationalAction action = new ComputationalAction(name, lines);
				put(name,action);
				action.setAlwaysGood(alwaysGood);
				action.setBacktrackOnSuccess(backtrackOnSuccess);
				logger.debug("Added as: " + action + "(always good:" + action.isAlwaysGood() + ", exhaustive:"
						+ action.backtrackOnSuccess() + ")");
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e);
			logger.error("Error reading computational actions from " + reader);
		}
		logger.info("Loaded grammar with " + size() + " computational action entries.");
		logger.trace(this);
	}
}
