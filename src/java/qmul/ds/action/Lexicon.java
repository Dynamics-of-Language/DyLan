
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import qmul.ds.action.atomic.Effect;
import qmul.ds.action.atomic.EffectFactory;
import qmul.ds.action.atomic.FreshPut;
import qmul.ds.action.atomic.IfThenElse;
import qmul.ds.formula.TTRRecordType;

/**
 * A map from words to {@link LexicalAction}s
 * 
 * @author mpurver
 */
public class Lexicon extends HashMap<String, Collection<LexicalAction>> implements Serializable {

	private static Logger logger = Logger.getLogger(Lexicon.class);
	public int lexiconsize = 0;

	/**
	 * A template for creating lexical actions for a particular syntactic class
	 * 
	 * @author mpurver
	 */
	protected class LexicalTemplate {

		private String name;
		private List<String> metavars;
		private List<String> lines;
		private boolean noLeftAdjustment = false;

		/**
		 * @param name
		 */
		protected LexicalTemplate(String name, List<String> metavars, List<String> lines, boolean noLeftAdjustment) {
			this.name = name;
			this.metavars = new ArrayList<String>(metavars);
			this.lines = new ArrayList<String>(lines);
			this.noLeftAdjustment = noLeftAdjustment;
		}

		/**
		 * @param word
		 * @param metavals
		 * @return a new {@link LexicalAction} instantiation for this word and
		 *         metavariable values
		 */
		protected LexicalAction create(String word, List<String> metavals) { // TODO should work for all versions
			ArrayList<String> lines = new ArrayList<String>();
			logger.trace("metavars for word : " + word + " of type " + name);

			logger.info("creating lexical action for " + word + " using template " + name);
			for (String line : this.lines) {
				for (int i = 0; i < metavals.size(); i++) {
					line = line.replaceAll(metavars.get(i), metavals.get(i));
				}
				lines.add(line);
			}

			lexiconsize++;
			// logger.info("lexicon size = " + lexiconsize);

			return new LexicalAction(word, lines, this.name, this.noLeftAdjustment);
		}

	}

	private static final long serialVersionUID = -470754367073236462L;

	public static final String WORD_FILE_NAME = "lexicon.txt";
	public static final String ACTION_FILE_NAME = "lexical-actions.txt";
	public static final String MACRO_FILE_NAME = "lexical-macros.txt";

	public static final String LINE_COMMENT = "//";
	public static final String BEGIN_COMMENT = "//*"; // can't use /* as it can
	// occur in e.g. <\/*>
	public static final String END_COMMENT = "*//";

	public static final Pattern TEMPLATE_SPEC_PATTERN = Pattern.compile("(.+?)\\((.+)\\)");
	public static final Pattern MACRO_SPEC_PATTERN = Pattern.compile("(.+?)(\\(.*\\))*");

	private HashMap<String, LexicalTemplate> actionTemplates = new HashMap<String, LexicalTemplate>();

	/**
	 * Read a set of {@link LexicalAction}s from file
	 * 
	 * @param dir containing at least the lexical-actions.txt and lexicon.txt files
	 */
	public Lexicon(File dir) {
		File file = new File(dir, MACRO_FILE_NAME);
		try {
			BufferedReader reader1 = null;
			if (!file.exists()) {
				logger.debug("no lexical-macro file. expecting no macro calls in lexical action file");

			} else {
				reader1 = new BufferedReader(new FileReader(file, Charset.forName("utf-8")));
			}

			initMacroTemplates(reader1);
			file = new File(dir, ACTION_FILE_NAME);

			BufferedReader macroReader = new BufferedReader(new FileReader(file, Charset.forName("utf-8")));
			initMacroTemplates(macroReader);
			// Matt change: initLexicalTemplates(macroReader);
			file = new File(dir, WORD_FILE_NAME);
			BufferedReader reader2 = new BufferedReader(new FileReader(file, Charset.forName("utf-8")));
			readWords(reader2);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error reading lexical actions file " + file.getAbsolutePath());
		}
	}


    /**
     * @author Arash Ash & Arash Eshghi
     * This constructor is added to provide a default value of topN=3 for
     * the constructor with (String dirNameOrURL, int topN) parameters.
     *
     * @param dirNameOrURL containing at least the lexical-actions.txt and lexicon.txt files,
     *                     or newly, lexicon.lex-top-N.txt files.
     */
    public Lexicon(String dirNameOrURL) {
        this(dirNameOrURL, 3);
    }


    /**
     * Modified by Arash Ash -> added support for loading lexical actions learnt by Eshghi et al. (2013b)
     * Read a set of {@link LexicalAction}s from file
     *
     * @param dirNameOrURL containing at least the lexical-actions.txt and lexicon.txt files,
     *                     or newly, lexicon.lex-top-N.txt files.
     * @param topN the number of most probable lexical actions to be read from the learnt lexicon files.
     */
    public Lexicon(String dirNameOrURL, int topN) {
        BufferedReader reader;
        try {
            // Adds support for initialising a lexicon object from the learnt lexical actions by
            // Eshghi et al. (2013b) instead of using macro files as templates.
            File f = new File(dirNameOrURL + File.separator + "lexicon.txt");
            if (f.exists() && !f.isDirectory()) { // If lexicon.txt exists, load normally. Otherwise use learnt files.
                if (dirNameOrURL.matches("(https?|file):.*")) {
                    reader = new BufferedReader(new InputStreamReader(
                            new URL(dirNameOrURL.replaceAll("/?$", "/") + MACRO_FILE_NAME).openStream()));
                } else {
                    File macroFile = new File(dirNameOrURL, MACRO_FILE_NAME);
                    if (!macroFile.exists()) {
                        logger.debug("no lexical-macro file. expecting no macro calls in lexical action file");
                        reader = null;
                    } else
                        reader = new BufferedReader(new FileReader(macroFile, Charset.forName("utf-8")));
                }
                initMacroTemplates(reader);
                if (dirNameOrURL.matches("(https?|file):.*")) {
                    reader = new BufferedReader(new InputStreamReader(
                            new URL(dirNameOrURL.replaceAll("/?$", "/") + ACTION_FILE_NAME).openStream()));
                } else {
                    reader = new BufferedReader(new FileReader(new File(dirNameOrURL, ACTION_FILE_NAME), Charset.forName("utf-8")));
                }
                initLexicalTemplates(reader);

                if (dirNameOrURL.matches("(https?|file):.*")) {
                    reader = new BufferedReader(new InputStreamReader(
                            new URL(dirNameOrURL.replaceAll("/?$", "/") + WORD_FILE_NAME).openStream()));
                } else {
                    reader = new BufferedReader(new FileReader(new File(dirNameOrURL, WORD_FILE_NAME), Charset.forName("utf-8")));
                }
                readWords(reader);
            } else
                loadLearntLexiconTxt(dirNameOrURL, topN);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error reading lexical actions file " + dirNameOrURL);
        }
    }

	/**
	 * Read a set of {@link LexicalAction}s from file and create lexicon.txt on the
	 * fly
	 * 
	 * @param dirNameOrURL containing at least the lexical-actions.txt file, not the
	 *                     lexicon.txt
	 * @param distribution a list of String-int pairs which is the POS and the
	 *                     target number of instances of each corresponding lexical
	 *                     action in the lexicon
	 * @param lexiconSize  will stop when lexicon has this many entries
	 */
	public Lexicon(String dirNameOrURL, HashMap<String, String[]> POStypes, HashMap<String, int[]> targetDistribution,
			int lexiconSize) {
		BufferedReader reader;
		try {
			if (dirNameOrURL.matches("(https?|file):.*")) {
				reader = new BufferedReader(new InputStreamReader(
						new URL(dirNameOrURL.replaceAll("/?$", "/") + MACRO_FILE_NAME).openStream()));
			} else {
				File macroFile = new File(dirNameOrURL, MACRO_FILE_NAME);
				if (!macroFile.exists()) {
					logger.debug("no lexical-macro file. expecting no macro calls in lexical action file");
					reader = null;
				} else
					reader = new BufferedReader(new FileReader(macroFile, Charset.forName("utf-8")));
			}
			initMacroTemplates(reader);
			if (dirNameOrURL.matches("(https?|file):.*")) {
				reader = new BufferedReader(new InputStreamReader(
						new URL(dirNameOrURL.replaceAll("/?$", "/") + ACTION_FILE_NAME).openStream()));
			} else {
				reader = new BufferedReader(new FileReader(new File(dirNameOrURL, ACTION_FILE_NAME), Charset.forName("utf-8")));
			}
			initLexicalTemplates(reader);

			if (dirNameOrURL.matches("(https?|file):.*")) {
				reader = new BufferedReader(new InputStreamReader(
						new URL(dirNameOrURL.replaceAll("/?$", "/") + WORD_FILE_NAME).openStream()));
			} else {
				reader = new BufferedReader(new FileReader(new File(dirNameOrURL, WORD_FILE_NAME), Charset.forName("utf-8")));
			}

			if (dirNameOrURL.matches("(https?|file):.*")) {
				reader = new BufferedReader(new InputStreamReader(
						new URL(dirNameOrURL.replaceAll("/?$", "/") + "lexicon_Complete.txt").openStream()));
			} else {
				reader = new BufferedReader(new FileReader(new File(dirNameOrURL, "lexicon_Complete.txt"), Charset.forName("utf-8")));
			}
			readWordsDistribution(reader, POStypes, targetDistribution, lexiconSize);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error reading lexical actions file " + dirNameOrURL);
		}
	}

	public Lexicon() {
		super();
	}

	/**
	 * for loading lexicon from a lexicon object file, rather than text file
	 * 
	 * @param file
	 */
	public static Lexicon loadLexicon(String file) throws ClassNotFoundException, IOException {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
		Lexicon lex = (Lexicon) in.readObject();
		in.close();

		return lex;
	}

	public void writeToTexlearntFile(String fileName) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));

		for (String word : keySet()) {
			for (LexicalAction la : get(word)) {
				out.write("[" + la.getProb() + "," + la.getRank() + "]");
				out.newLine();
				out.write(la.toString());
				out.newLine();
			}
			out.flush();

		}
		out.close();
	}

	public static void main(String a[]) {

	}

	/**
	 * Read a set of {@link LexicalAction} templates from file
	 * 
	 * @param reader containing the lexical-actions.txt file
	 */
	private void initLexicalTemplates(BufferedReader reader) {
		try {
			String line;
			String name = null;
			boolean noLeftAdjustment = false;
			List<String> metavars = new ArrayList<String>();
			List<String> lines = new ArrayList<String>();
			while ((line = reader.readLine()) != null) {
				line = comment(line.trim());
				if ((line == null) || (line.isEmpty() && lines.isEmpty())) {
					continue;
				}
				if (line.isEmpty() && !lines.isEmpty()) {
					actionTemplates.put(name, new LexicalTemplate(name, metavars, lines, noLeftAdjustment));
					logger.info((noLeftAdjustment) ? "Added Lexical Template for " + "*" + name
							: "Added Lexical Template for " + name);
					lines.clear();
					name = null;
					metavars.clear();
					noLeftAdjustment = false;
				} else if (name == null) {
					Matcher m = TEMPLATE_SPEC_PATTERN.matcher(line);
					if (m.matches()) {
						if (m.group(1).startsWith("*")) {
							name = m.group(1).substring(1, m.group(1).length());
							noLeftAdjustment = true;
						} else
							name = m.group(1);
						for (String s : m.group(2).split(",")) {
							metavars.add(s);
						}
					} else {
						throw new IllegalArgumentException("unrecognised template spec " + line);
					}
					logger.info((noLeftAdjustment) ? "New Lexical Template: " + "*" + name
							: "Added Lexical Template for " + name);
				} else {
					lines.add(line);
				}
			}
			if (!lines.isEmpty()) {
				actionTemplates.put(name, new LexicalTemplate(name, metavars, lines, noLeftAdjustment));
				logger.debug("Added Lexical Template for " + name);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Error reading templates from stream " + reader);
		}
		logger.info("Read " + actionTemplates.size() + " lexical action templates");
	}

	private void initMacroTemplates(BufferedReader reader) {
		if (reader == null)
			EffectFactory.clearMacroTemplates();
		else
			EffectFactory.initMacroTemplates(reader);

	}

	/**
	 * Read a set of {@link LexicalAction}s from file
	 * 
	 * @param reader containing the lexicon.txt file
	 */
	private void readWords(BufferedReader reader) {
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				line = comment(line.trim());
				if ((line == null) || line.isEmpty()) {
					continue;
				}
				List<String> fields = Arrays.asList(line.split("\\s+"));
				String word = fields.get(0);
				String template = fields.get(1);
				if (actionTemplates.get(template) == null) {
					logger.debug("No template " + template + ", skipping word " + word);
				} else {

					logger.debug("Using template " + template + " for word " + word);
					try {
						LexicalAction action = actionTemplates.get(template).create(word,
								fields.subList(2, fields.size()));
						if (!containsKey(word)) {
							put(word, new HashSet<LexicalAction>());
						}
						get(word).add(action);
						logger.debug("Added lexical action " + action);

					} catch (IllegalArgumentException e) {
						// logger.warn(e);
						logger.warn("Macros used in lexical template could not be instatiated. Template:" + template
								+ "; Word:" + word + " Skipping this");
						// e.printStackTrace();

						continue;
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Error reading lexical entries from " + reader);
		}
		logger.info("Read lexicon with " + size() + " words.");
		logger.trace(this);
	}

	/**
	 * Read a set of {@link LexicalAction}s from file
	 * 
	 * @param reader       containing the lexiconComplete.txt file
	 * @param distribution a list of String-int pairs which is the POS and the
	 *                     target number of instances of each corresponding lexical
	 *                     action in the lexicon
	 * @param lexiconSize  will stop when lexicon has this many entries
	 */
	private void readWordsDistribution(BufferedReader reader, HashMap<String, String[]> POStypes,
			HashMap<String, int[]> targetDistribution, int lexiconSize) {

		HashMap<String, ArrayList<List<String>>> mytypes = new HashMap<String, ArrayList<List<String>>>();
		int ambiguityTarget = Math.round((float) 0.1 * lexiconSize); // number of words you want to be ambiguous 0.N
																		// where N= %(i.e. those that share two or more
																		// lexical actions)
		int ambiguityCount = 0;

		try {
			ArrayList<String> seen = new ArrayList<String>();
			; // record of all words we see, for ambiguous to work
			HashMap<String, ArrayList<List<String>>> ambiguous = new HashMap<String, ArrayList<List<String>>>();
			HashMap<String, ArrayList<List<String>>> POSLinesMap = new HashMap<String, ArrayList<List<String>>>();
			int ambigCount = 0; // number of ambiguous words
			int numberOfSensesLimit = 2; // limited number of ambiguous senses for a word
			String line;
			int totalLexLength = 0;

			// get the lines for each relevant lexical action
			while ((line = reader.readLine()) != null) {
				line = comment(line.trim());
				if ((line == null) || line.isEmpty()) {
					continue;
				}
				List<String> fields = Arrays.asList(line.split("\\s+"));
				String word = fields.get(0);
				String template = fields.get(1);

				if (!this.actionTemplates.containsKey(template)) {
					continue;
				}

				POSloop: for (String myPOS : POStypes.keySet()) {
					for (String lexicalLabel : POStypes.get(myPOS)) {
						if (template.startsWith(lexicalLabel)) {
							if (POSLinesMap.containsKey(myPOS)) {
								POSLinesMap.get(myPOS).add(fields);
							} else {
								ArrayList<List<String>> newArray = new ArrayList<List<String>>();
								newArray.add(fields);
								POSLinesMap.put(myPOS, newArray);
							}
							if (seen.contains(word)) {
								if (ambiguous.containsKey(word)) {
									ambiguous.get(word).add(fields);
								} else {
									ArrayList<List<String>> newArray = new ArrayList<List<String>>();
									newArray.add(fields);
									ambiguous.put(word, newArray);
								}
								ambigCount++;
							}
							totalLexLength++;
							break POSloop;
						}
					}
				}
				seen.add(word);
			}

			// now sample lexical actions from each type randomly and then look for
			// ambiguous words..
			POSloop: for (String POS : POSLinesMap.keySet()) { // loops through noun,verb etc..

				Collections.shuffle(POSLinesMap.get(POS)); // shuffles lexical actions for randomness
				// now loop through candidate lexical entries
				lexicalLoop: for (List<String> fields : POSLinesMap.get(POS)) {

					if (targetDistribution.get(POS)[2] == targetDistribution.get(POS)[0]) // if target reached
					{
						continue POSloop;
					}

					String word = fields.get(0);
					String template = fields.get(1);
					String semantics = fields.get(2); // should always have at least these three
					// check if they match any string in the POStypes list, if so, add a count to
					// the relevant list
					int numberOfSenses = 1;
					boolean addWord = true;
					boolean addedFirstAmbig = false;

					// try to create template
					if (actionTemplates.get(template) == null) {
						logger.debug("No template " + template + ", skipping word " + word);
						addWord = false;
					} else {

						logger.debug("Using template " + template + " for word " + word);
						try {
							LexicalAction action = actionTemplates.get(template).create(word,
									fields.subList(2, fields.size()));
							if (!containsKey(word)) {
								put(word, new HashSet<LexicalAction>());
							} else {
								numberOfSenses += get(word).size();
								addedFirstAmbig = true;
								if (numberOfSenses > numberOfSensesLimit) {
									continue lexicalLoop; // don't add this word as we've reached the target number of
															// senses
								}
							}
							get(word).add(action);
							logger.info("Added lexical action " + action);
							targetDistribution.get(POS)[2]++;

						} catch (IllegalArgumentException e) {
							logger.error(e);
							logger.error("Macros used in lexical template could not be instatiated. \nTemplate:"
									+ template + "\nWord:" + word + " Skipping this");
							addWord = false;
							continue lexicalLoop;
						}
					}

					if (addWord == false) {
						continue lexicalLoop;
					} // keep going as we're not adding this word

					// if we've got this far, add to the types list with the fields
					if (!mytypes.containsKey(template)) {
						ArrayList<List<String>> strings = new ArrayList<List<String>>();
						strings.add(fields);
						mytypes.put(template, strings);
					} else {
						mytypes.get(template).add(fields);
					}

					// now look for ambiguous words and add them with the given frequency we want,
					// specified at top of
					// this method
					if (ambiguous.containsKey(word)) {
						if (ambiguityCount >= ambiguityTarget) { // reached the target
							logger.info("Reached ambiguity limit!");
							System.out.println(" REACHED AMBIGUITY LIMIT! Press enter to continue...");
							// try{
							// System.in.read();
							// }catch(Exception e){}
							continue lexicalLoop;
						}

						String ambigPOS = "";
						for (List<String> fieldsAmbig : ambiguous.get(word)) {

							if (numberOfSenses == numberOfSensesLimit) { // reached limit of number of senses
								break;
							}

							String templateAmbig = fieldsAmbig.get(1);
							String semanticsAmbig = fieldsAmbig.get(2);
							if (templateAmbig == template && semanticsAmbig == semantics) {
								continue; // this is the same action added above??
							}

							AmbigPOS: for (String myambigPOS : POSLinesMap.keySet()) {
								for (List<String> lines : POSLinesMap.get(myambigPOS)) {
									if (lines.equals(fieldsAmbig)) {
										ambigPOS = myambigPOS;
										break AmbigPOS;
									}
								}
							}

							logger.debug("Using template " + templateAmbig + " for word " + word);
							try {
								LexicalAction action = actionTemplates.get(templateAmbig).create(word,
										fieldsAmbig.subList(2, fieldsAmbig.size()));
								// if (!containsKey(word)) {
								// put(wordAmbig, new HashSet<LexicalAction>());
								// } // shouldn't need these as have it above

								get(word).add(action);
								logger.info("Added lexical action " + action);
								targetDistribution.get(ambigPOS)[2]++;
								ambiguityCount = addedFirstAmbig == false ? ambiguityCount + 2 : ambiguityCount + 1; // add
																														// 2
																														// the
																														// first
																														// time
																														// round
								numberOfSenses++; // just for this word
								addedFirstAmbig = true;
								if (!mytypes.containsKey(templateAmbig)) {
									ArrayList<List<String>> strings = new ArrayList<List<String>>();
									strings.add(fieldsAmbig);
									mytypes.put(templateAmbig, strings);
								} else {
									mytypes.get(templateAmbig).add(fieldsAmbig);
								}

							} catch (IllegalArgumentException e) {
								logger.error(e);
								logger.error("Macros used in lexical template could not be instatiated. \nTemplate:"
										+ templateAmbig + "\nWord:" + word + " Skipping this");
								// addWord = false;
								continue lexicalLoop;
							}

						} // end of ambiguity loop/ adding different senses

					} // end of ambiguity bit

				} // end of lexicalLoop

			} // end of POSloop

			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Error reading lexical entries from " + reader);
		}

		try {
			// Create file with the lexicon itself
			FileWriter fstream = new FileWriter(
					"resource" + File.separator + "2009-english-test-induction" + File.separator + "lexicon.txt");
			BufferedWriter out = new BufferedWriter(fstream);
			for (String key : mytypes.keySet()) {
				logger.debug(mytypes.get(key));
				for (List<String> fields : mytypes.get(key)) {
					out.write(fields.get(0));
					int length = fields.get(0).length();
					for (String field : fields.subList(1, fields.size())) {
						for (int i = length; i < 30; i++) { // observing lexicon.txt format
							out.write(" ");
						}
						length = field.length();
						out.write(field);
					}
					out.write("\n");
				}
			}
			// Close the output stream
			out.close();
			logger.info("written to " + "lexicon.txt file");
			logger.info("ambiguityCount = " + ambiguityCount);

		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}

		logger.info("Read lexicon with " + size() + " words.");
		logger.trace(this);
	}

	private BufferedReader readLexText(File lexFile, int topN) {
		BufferedReader reader = null;
		try {

			if (!lexFile.exists())
				logger.debug("No file \"lexicon.lex-top-" + topN + "\" found.");

			else
				reader = new BufferedReader(new FileReader(lexFile));

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			logger.error("Error reading lexical actions file " + lexFile.getAbsolutePath());
		}
		return reader;
	}

    /**
     * Loads learnt lexical actions by Eshghi et al. (2013b) from a text file.
     * The format is different as it includes probabilities and does not use lexical templates.
     *
     * @author Arash Ash
     *
     * TODO what's exactly in `prob`? add necessary logs/docs
     * TODO initLexicalTemplates add necessary try-catches
     *
     * @param grammarPath path that contains lexicon.lex-top-N.txt files.
     * @param topN the number of most probable lexical actions to be read from the learnt lexicon files.
     */
    public void loadLearntLexiconTxt(String grammarPath, int topN) {

        File lexFile = new File(grammarPath + File.separator + "lexicon.lex-top-" + topN + ".txt");
        BufferedReader reader = readLexText(lexFile, topN);

        try {
            String line;
            List<String> lines = new ArrayList<String>(); // A String for lexical actions associated to a word

            while ((line = reader.readLine()) != null) {
                line = comment(line.trim());

                if ((line == null) || (line.isEmpty() && lines.isEmpty()))
                    continue;

                if (line.isEmpty() && !lines.isEmpty()) { // means the end of a lexical action
                    String prob = lines.get(0); // Not being used anywhere, for now.
                    String word = lines.get(1);
                    List<String> actionStr = lines.subList(2, lines.size());
                    LexicalAction lexAct = new LexicalAction(word, actionStr);
                    // TODO add log "created lexical action lexAct"

                    if (this.containsKey(word))
                        this.get(word).add(lexAct);
                        // TODO log here
                    else {
                        HashSet<LexicalAction> lexActs = new HashSet<LexicalAction>();
                        lexActs.add(lexAct);
                        this.put(word, lexActs);
                        logger.info("Added lexical action " + lexAct.toString());
                    }
                    lines.clear();
                } else
                    lines.add(line);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Error reading lexical actions from " + reader);
        }
        logger.info("Loaded lexicon with: " + this.keySet().size() + " words.");
    }

	private static boolean commented = false;

	/**
	 * @param line
	 * @return null if the line is entirely commented out, all uncommented parts
	 *         (including empty line) otherwise
	 */
	public static String comment(String line) {
		// return empty lines unchanged
		if (line.isEmpty()) {
			return line;
		}
		// remove all /*stuff*/ within line
		line = line.replaceAll(Pattern.quote(BEGIN_COMMENT) + ".*?" + Pattern.quote(END_COMMENT), "");
		// if already in a comment, drop everything until after */
		if (commented) {
			if (line.contains(END_COMMENT)) {
				commented = false;
				line = line.substring(line.indexOf(END_COMMENT) + END_COMMENT.length());
			} else {
				return null;
			}
		}
		// drop everything after lonely /* and remember we're in a comment
		if (line.contains(BEGIN_COMMENT)) {
			commented = true;
			line = line.substring(0, line.indexOf(BEGIN_COMMENT));
		}
		// drop everything after //
		if (line.contains(LINE_COMMENT)) {
			line = line.substring(0, line.indexOf(LINE_COMMENT));
		}
		if (line.isEmpty()) {
			return null;
		}
		return line;
	}

	/**
	 * Precondition: only works with lexicons in TTR Goes through each lexical
	 * action and extracts the contributed semantics from it. When the semantics is
	 * functional, the semantics is beta-reduced with underspecified arguments.
	 * 
	 * @return the list of all possible semantic increments by this lexicon
	 */
	public List<TTRRecordType> extractSemanticAtoms() {
		ArrayList<TTRRecordType> result = new ArrayList<TTRRecordType>();

		for (String word : keySet()) {
			for (LexicalAction action : get(word)) {

				Effect[] ites = action.getEffects();

				for (Effect ifte : ites) {
					IfThenElse ite = (IfThenElse) ifte;

					for (Effect atomic : ite.getTHENClause()) {
//						if (atomic instanceof TTRFreshPut)
//						{
//							
//						}
					}
				}
			}

		}
		return null;
	}

}

