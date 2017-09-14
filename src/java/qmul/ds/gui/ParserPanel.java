/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
// StanfordLexicalizedParser -- a probabilistic lexicalized NL CFG parser
// Copyright (c) 2002, 2003, 2004, 2005 The Board of Trustees of
// The Leland Stanford Junior University. All Rights Reserved.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// For more information, bug reports, fixes, contact:
//    Christopher Manning
//    Dept of Computer Science, Gates 4A
//    Stanford CA 94305-9040
//    USA
//    parser-support@lists.stanford.edu
//    http://nlp.stanford.edu/downloads/lex-parser.shtml
package qmul.ds.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.log4j.Logger;

import edu.stanford.nlp.ling.BasicDocument;
import edu.stanford.nlp.ling.Document;
import edu.stanford.nlp.process.DocumentProcessor;
import edu.stanford.nlp.process.StripTagsProcessor;
import edu.stanford.nlp.swing.FontDetector;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.international.pennchinese.ChineseTreebankLanguagePack;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import qmul.ds.ContextParser;
import qmul.ds.DSParser;
import qmul.ds.Generator;
import qmul.ds.InteractiveContextParser;
import qmul.ds.ParserTuple;
import qmul.ds.Utterance;
import qmul.ds.dag.DAGTuple;
import qmul.ds.dag.GroundableEdge;
import qmul.ds.formula.TTRFormula;

/**
 * Provides a simple GUI Panel for Parsing. Allows a user to load a parser
 * created using lexparser.LexicalizedParser, load a text data file or type in
 * text, parse sentences within the input text, and view the resultant parse
 * tree.
 * 
 * @author Huy Nguyen (htnguyen@cs.stanford.edu)
 */
@SuppressWarnings("serial")
public class ParserPanel extends JPanel {

	private static Logger logger = Logger.getLogger(ParserPanel.class);

	// constants for language specification
	public static final int ENGLISH = 0;
	public static final int JAPANESE = 1;
	// default locations for pre-packaged grammars
	public static final String BASE_URL = "http://www.dcs.qmul.ac.uk/~mpurver/ds/";
	public static final String ENGLISH_2001_URL = BASE_URL + "2001-english/";
	public static final String JAPANESE_2001_URL = BASE_URL + "2001-japanese/";
	public static final String ENGLISH_2005_URL = BASE_URL + "2005-english/";
	public static final String ENGLISH_2009_URL = BASE_URL + "2009-english/";
	public static final String ENGLISH_TTR_URL = BASE_URL + "2009-english-ttr/";
	public static final String[] PARSER_TYPES = {
			LoadParserThread.breadthFirst, LoadParserThread.interactive };
	private static TreebankLanguagePack tlp;
	private String encoding = "UTF-8";
	private boolean segmentWords = false;
	private boolean isApplet = false;

	// one second in milliseconds
	private static final int ONE_SECOND = 1000;
	// parser takes approximately a minute to load
	private static final int PARSER_LOAD_TIME = 60;
	// parser takes 5-60 seconds to parse a sentence
	private static final int PARSE_TIME = 30;
	// generator takes 5-60 seconds to parse a sentence
	private static final int GENERATE_TIME = 30;

	// constants for finding nearest sentence boundary
	private static final int SEEK_FORWARD = 1;
	private static final int SEEK_BACK = -1;

	private JFileChooser jfc = null;
	private OpenPageDialog pageDialog;
	private String defaultParser = "";

	// for highlighting
	private SimpleAttributeSet normalStyle, highlightStyle;
	private int startIndex, endIndex;
	private JTabbedPane tabbedTuplePanel;

	private ParserTupleViewer tupleViewer;
	// private TreePanel treePanel;

	// private FormulaPanel semPanel;
	private DAGViewer<DAGTuple, GroundableEdge> conPanel;
	private DSParser parser;
	private Generator<?> generator;

	// worker threads to handle long operations
	private LoadParserThread lpThread;
	private ParseThread parseThread;
	private GenerateThread generateThread;

	// to monitor progress of long operations
	private javax.swing.Timer timer;
	// private ProgressMonitor progressMonitor;
	private int count; // progress count
	// use glass pane to block input to components other than progressMonitor
	private Component glassPane;

	/** Whether to scroll one sentence forward after parsing. */
	private boolean scrollWhenDone;
	private JFrame containingFrame;

	/**
	 * Creates new form ParserPanel
	 */
	public ParserPanel(boolean applet, JFrame containing) {
		this.containingFrame = containing;
		initComponents();
		setApplet(applet);

		// create dialogs for file selection
		jfc = new JFileChooser();
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jfc.setCurrentDirectory(new File("."));
		pageDialog = new OpenPageDialog(new Frame(), true, false);
		pageDialog.setFileChooser(jfc);

		setLanguage(ENGLISH);

		// create a timer
		timer = new javax.swing.Timer(ONE_SECOND, new TimerListener());

		// for (un)highlighting text
		highlightStyle = new SimpleAttributeSet();
		normalStyle = new SimpleAttributeSet();
		StyleConstants.setBackground(highlightStyle, Color.yellow);
		StyleConstants.setBackground(normalStyle, pTextPane.getBackground());
	}

	/**
	 * Scrolls back one sentence in the text
	 */
	public void scrollBack() {
		highlightSentence(startIndex - 1);
		// scroll to highlight location
		pTextPane.setCaretPosition(startIndex);
	}

	/**
	 * Scrolls forward one sentence in the text
	 */
	public void scrollForward() {
		highlightSentence(endIndex + 1);
		// scroll to highlight location
		pTextPane.setCaretPosition(startIndex);
	}

	/**
	 * Highlights specified text region by changing the character attributes
	 */
	private void highlightText(int start, int end, SimpleAttributeSet style) {
		if (start < end) {
			pTextPane.getStyledDocument().setCharacterAttributes(start,
					end - start + 1, style, false);
		}
	}

	/**
	 * Finds the sentence delimited by the closest sentence delimiter preceding
	 * start and closest period following start.
	 */
	private void highlightSentence(int start) {
		highlightSentence(start, -1);
	}

	/**
	 * Finds the sentence delimited by the closest sentence delimiter preceding
	 * start and closest period following end. If end is less than start (or
	 * -1), sets right boundary as closest period following start. Actually
	 * starts search for preceding sentence delimiter at (start-1)
	 */
	private void highlightSentence(int start, int end) {
		// clears highlight. paints over entire document because the document
		// may have changed
		highlightText(0, pTextPane.getText().length(), normalStyle);

		// if start<1 set startIndex to 0, otherwise set to index following
		// closest preceding period
		startIndex = (start < 1) ? 0 : nearestDelimiter(pTextPane.getText(),
				start - 1, SEEK_BACK) + 1;

		// if end<startIndex, set endIndex to closest period following
		// startIndex
		// else, set it to closest period following end
		endIndex = nearestDelimiter(pTextPane.getText(),
				(end < startIndex) ? startIndex : end, SEEK_FORWARD);
		if (endIndex == -1) {
			endIndex = pTextPane.getText().length() - 1;
		}

		highlightText(startIndex, endIndex, highlightStyle);

		// enable/disable scroll buttons as necessary
		backButton.setEnabled(startIndex != 0);
		forwardButton.setEnabled(endIndex != pTextPane.getText().length() - 1);
		parseNextButton.setEnabled(forwardButton.isEnabled() && parser != null);
	}

	/**
	 * Finds the nearest delimiter starting from index start. If
	 * <tt>seekDir</tt> is SEEK_FORWARD, finds the nearest delimiter after
	 * start. Else, if it is SEEK_BACK, finds the nearest delimiter before
	 * start.
	 */
	private int nearestDelimiter(String text, int start, int seekDir) {
		int curIndex = start;
		int textLeng = text.length();
		String[] puncWords = tlp.sentenceFinalPunctuationWords();
		while (curIndex >= 0 && curIndex < textLeng) {
			for (int i = 0; i < puncWords.length; i++) {
				if (puncWords[i].equals(Character.toString(text
						.charAt(curIndex)))) {
					return curIndex;
				}
			}
			curIndex += seekDir;
		}
		return -1;
	}

	/**
	 * Highlights the sentence that is currently being selected by user (via
	 * mouse highlight)
	 */
	private void highlightSelectedSentence() {
		highlightSentence(pTextPane.getSelectionStart(),
				pTextPane.getSelectionEnd());
	}

	/**
	 * Highlights the sentence that is currently being edited
	 */
	private void highlightEditedSentence() {
		highlightSentence(pTextPane.getCaretPosition());

	}

	/**
	 * Sets the status text at the bottom of the ParserPanel.
	 */
	public void setStatus(String status) {
		statusLabel.setText(status);
	}

	/**
	 * Sets the language used by the ParserPanel to tokenize, parse, and display
	 * sentences.
	 * 
	 * @param language
	 *            One of several predefined language codes. e.g.
	 *            <tt>UNTOKENIZED_ENGLISH</tt>, <tt>TOKENIZED_CHINESE</tt>, etc.
	 */
	public void setLanguage(int language) {
		switch (language) {
		case ENGLISH:
			tlp = new PennTreebankLanguagePack();
			encoding = tlp.getEncoding();
			setEnglishFont();
			break;
		case JAPANESE:
			segmentWords = false;
			tlp = new ChineseTreebankLanguagePack();
			encoding = "UTF-8"; // we support that not GB18030 currently....
			setChineseFont();
			break;
		}
	}

	private void setEnglishFont() {
		pTextPane.setFont(new Font("Sans Serif", Font.PLAIN, 14));
		tupleViewer.setFont(new Font("Sans Serif", Font.PLAIN, 14));
	}

	private void setChineseFont() {
		java.util.List fonts = FontDetector
				.supportedFonts(FontDetector.CHINESE);
		if (fonts.size() > 0) {
			Font font = new Font(((Font) fonts.get(0)).getName(), Font.PLAIN,
					14);
			pTextPane.setFont(font);
			tupleViewer.setFont(font);
			logger.debug("Selected font " + font);
		} else if (FontDetector.hasFont("Watanabe Mincho")) {
			pTextPane.setFont(new Font("Watanabe Mincho", Font.PLAIN, 14));
			tupleViewer.setFont(new Font("Watanabe Mincho", Font.PLAIN, 14));
		}
	}

	/**
	 * @return is this running as a web applet (i.e. shouldn't try to access
	 *         local files)?
	 */
	public boolean isApplet() {
		return isApplet;
	}

	/**
	 * @param isApplet
	 *            is this running as a web applet (i.e. shouldn't try to access
	 *            local files)?
	 */
	public void setApplet(boolean isApplet) {
		this.isApplet = isApplet;
		if (loadFileButton != null) {
			loadFileButton.setEnabled(!isApplet);
		}
		if (loadParserButton != null) {
			loadParserButton.setEnabled(true);
		}
	}

	/**
	 * Tokenizes the highlighted text (using a tokenizer appropriate for the
	 * selected language, and initiates the ParseThread to parse the tokenized
	 * text.
	 * @param turn_released indicates whether to parse a <release-turn> token
	 */
	public void parse(boolean turn_released) {

		if (pTextPane.getText().length() == 0) {
			logger.info("nothing to parse");
			return;
		}

		// use endIndex+1 because substring subtracts 1
		String text = pTextPane.getText().trim()+(turn_released?" "+Utterance.RELEASE_TURN_TOKEN:"");
		// logger.debug("got text " + text);

		if (parser != null && text.length() > 0) {

			// Tokenizer<? extends HasWord> toke = tlp.getTokenizerFactory()
			// .getTokenizer(new CharArrayReader(text.toCharArray()));
			// List<? extends HasWord> wordList = toke.tokenize();

			Utterance utt = new Utterance(text);

			if (prevUtterance != null
					&& utt.getSpeaker().equals(Utterance.defaultSpeaker)) {
				utt.setSpeaker(prevUtterance.getSpeaker());

			}
			parseThread = new ParseThread(utt);
			parseThread.start();
			startProgressMonitor("Parsing", PARSE_TIME);
		}
	}

	/**
	 * Opens dialog to load a text data file
	 */
	public void loadFile() {
		// centers dialog in panel
		pageDialog.setLocation(getLocationOnScreen().x
				+ (getWidth() - pageDialog.getWidth()) / 2,
				getLocationOnScreen().y
						+ (getHeight() - pageDialog.getHeight()) / 2);
		pageDialog.setTitle("Load corpus to parse");
		pageDialog.setVisible(true);

		if (pageDialog.getStatus() == OpenPageDialog.APPROVE_OPTION) {
			loadFile(pageDialog.getPage());
		}
	}

	/**
	 * Loads a text or html file from a file path or URL. Treats anything
	 * beginning with <tt>http:\\</tt>,<tt>.htm</tt>, or <tt>.html</tt> as an
	 * html file, and strips all tags from the document
	 */
	public void loadFile(String filename) {
		if (filename == null) {
			return;
		}

		File file = new File(filename);

		String urlOrFile = filename;
		// if file can't be found locally, try prepending http:// and looking on
		// web
		if (!file.exists() && filename.indexOf("://") == -1) {
			urlOrFile = "http://" + filename;
		}
		// else prepend file:// to handle local html file urls
		else if (filename.indexOf("://") == -1) {
			urlOrFile = "file://" + filename;
		}

		// load the document
		Document doc;
		try {
			if (urlOrFile.startsWith("http://") || urlOrFile.endsWith(".htm")
					|| urlOrFile.endsWith(".html")) {
				// strip tags from html documents
				Document docPre = new BasicDocument().init(new URL(urlOrFile));
				DocumentProcessor noTags = new StripTagsProcessor();
				doc = noTags.processDocument(docPre);
			} else {
				doc = new BasicDocument(tlp.getTokenizerFactory())
						.init(new InputStreamReader(new FileInputStream(
								filename), encoding));
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Could not load file "
					+ filename + "\n" + e, null, JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			setStatus("Error loading document");
			return;
		}

		// load the document into the text pane
		StringBuilder docStr = new StringBuilder();
		for (Iterator it = doc.iterator(); it.hasNext();) {
			if (docStr.length() > 0) {
				docStr.append(' ');
			}
			docStr.append(it.next().toString());
		}
		pTextPane.setText(docStr.toString());
		dataFileLabel.setText(urlOrFile);

		highlightSentence(0);
		forwardButton.setEnabled(endIndex != pTextPane.getText().length() - 1);
		// scroll to top of document
		pTextPane.setCaretPosition(0);

		setStatus("Done");
	}

	/**
	 * Opens dialog to load a parser grammar/lexicon
	 */
	public void loadParser() {
		// centers dialog in panel
		pageDialog.setLocation(getLocationOnScreen().x
				+ (getWidth() - pageDialog.getWidth()) / 2,
				getLocationOnScreen().y
						+ (getHeight() - pageDialog.getHeight()) / 2);
		pageDialog.setTitle("Load grammar/lexicon");
		pageDialog.setValue(defaultParser);
		pageDialog.setVisible(true);

		if (pageDialog.getStatus() == OpenPageDialog.APPROVE_OPTION) {
			loadParser(pageDialog.getPage());
		}
	}

	/**
	 * Loads a parser grammar/lexicon specified by given path
	 */
	public void loadParser(String filename) {
		if (filename == null) {
			return;
		}

		// set default for next time
		defaultParser = filename;

		// check if file exists before we start the worker thread and progress
		// monitor
		// File file = new File(filename);
		// if (file.exists()) {
		System.out.println("loading parser:"
				+ this.parserTypeBox.getSelectedItem());
		String pType = (String) this.parserTypeBox.getSelectedItem();
		lpThread = new LoadParserThread(filename, pType);
		lpThread.start();
		startProgressMonitor("Loading Parser", PARSER_LOAD_TIME);
		// } else {
		// JOptionPane.showMessageDialog(this, "Could not find file " +
		// filename, null, JOptionPane.ERROR_MESSAGE);
		// setStatus("Error loading parser");
		// }
	}

	/**
	 * Re-initialises the current parser
	 */
	public void initParser() {
		if (parser != null) {
			tuples.clear();
			parser.init();
			parsedTextPane.setText("");
			prevUtterance = null;

			if (parser instanceof InteractiveContextParser) {
				InteractiveContextParser p = (InteractiveContextParser) parser;
				if (conPanel == null) {
					conPanel = new DAGViewer<DAGTuple, GroundableEdge>(
							p.getState());
					GraphZoomScrollPane scrollPane = new GraphZoomScrollPane(
							conPanel.vv);
					this.tabbedTuplePanel.addTab("Context", scrollPane);
				} else {
					conPanel.setDAG(p.getState());
					this.tabbedTuplePanel.setEnabledAt(1, true);
				}
				tuples.add(p.getState().getCurrentTuple());
			} else if (conPanel != null) {

				this.tabbedTuplePanel.setEnabledAt(1, false);
				this.tabbedTuplePanel.setSelectedIndex(0);
			}

			displayBestParse();
		}
	}

	/**
	 * Tells the current parser to prepare for a new sentence
	 */
	public void turnParser() {
		if (parser != null) {
			parser.newSentence();
			displayBestParse();
		}
		nextButton.setEnabled(false);
	}

	private void displayTuple(ParserTuple tuple) {
		Tree tree = (tuple == null ? null : tuple.getTree().toStanfordTree());
		// tree.pennPrint();
		treeNumberLabel.setText("Tree: " + (tupleNumber + 1) + " of "
				+ tuples.size());
		tupleViewer.setTuple(tuple);

		treePrevButton.setEnabled(tupleNumber > 0);

		treeNextButton.setEnabled(tupleNumber < (tuples.size() - 1));
		// adding 2*
		if (this.parser instanceof InteractiveContextParser)
		{
			InteractiveContextParser p=(InteractiveContextParser)this.parser;
			genToButton.setEnabled(tuple.isComplete()
				|| tuple.getSemantics(p.getContext()) != null);
		}
	}

	private ArrayList<ParserTuple> tuples = new ArrayList<ParserTuple>();
	private int tupleNumber = 0;

	public int beamWidth = 10;

	/**
	 * Display the best parse if available
	 */
	private void displayBestParse() {
		// tuples = new ArrayList<ParserTuple>(parser.getState());
		// ArrayList<ParserTuple> ttrtuples
		// TODO this is TTR specific, it shouldn't be in the gui
		// tuples = new ArrayList<ParserTuple>(parser.getTTRState()); //just
		// displaying the ttr representations...
		if (parser instanceof qmul.ds.Parser)
			tuples = new ArrayList<ParserTuple>(
					((qmul.ds.Parser) parser).getState());
		// tuples.addAll(ttrtuples);
		tupleNumber = tuples.size() - 1;
		displayTuple(tuples.get(tupleNumber));

	}

	private void exhaust() {
		parseThread = new ParseThread();
		parseThread.start();
		startProgressMonitor("Exhausting state", PARSE_TIME);
	}

	private void displayPrevTree() {
		displayTuple(tuples.get(--tupleNumber));
	}

	private void adjustOnce(ActionEvent evt) {
		if (parser instanceof InteractiveContextParser) {
			InteractiveContextParser dagParser = (InteractiveContextParser) parser;
			if (dagParser.parse()) {
				tuples.add(dagParser.getState().getCurrentTuple());
				conPanel.update(dagParser.getState());
				displayBestParse();
			}

		}

	}

	private void displayNextTree() {
		displayTuple(tuples.get(++tupleNumber));
	}

	private void generateFromThisTree() {
		if (generator == null) {
			generator = parser.getGenerator();
			generator.setGui(this);
		}
		generateThread = new GenerateThread(tuples.get(tupleNumber).getTree());
		generateThread.start();
		startProgressMonitor("Generating", GENERATE_TIME);
	}

	private void generateFromThisSemantics() {

		if (generator == null) {
			generator = parser.getGenerator();
			generator.setGui(this);
		}
		generateThread = new GenerateThread(tuples.get(tupleNumber)
				.getSemantics());
		//TODO: this is using getSemantics relative to tree, rather than context.
		generateThread.start();
		startProgressMonitor("Generating", GENERATE_TIME);

	}

	private void clearGeneratorOutput() {
		gTextPane.setText("");
	}

	public void setGeneratorOutput(String string) {
		gTextPane.setText(string);
	}

	public void addGeneratorOutput(String string) {
		gTextPane.setText(gTextPane.getText() + "\n" + string);
	}

	/**
	 * Initializes the progress bar with the status text, and the expected
	 * number of seconds the process will take, and starts the timer.
	 */
	private void startProgressMonitor(String text, int maxCount) {
		if (glassPane == null) {
			if (getRootPane() != null) {
				glassPane = getRootPane().getGlassPane();
				glassPane.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent evt) {
						Toolkit.getDefaultToolkit().beep();
					}
				});
			}
		}
		if (glassPane != null) {
			glassPane.setVisible(true); // block input to components
		}

		setStatus(text);
		progressBar.setMaximum(maxCount);
		progressBar.setValue(0);
		count = 0;
		timer.start();
		progressBar.setVisible(true);
	}

	/**
	 * At the end of a task, shut down the progress monitor
	 */
	private void stopProgressMonitor() {
		timer.stop();
		/*
		 * if(progressMonitor!=null) {
		 * progressMonitor.setProgress(progressMonitor.getMaximum());
		 * progressMonitor.close(); }
		 */
		progressBar.setVisible(false);
		if (glassPane != null) {
			glassPane.setVisible(false); // restore input to components
		}
		lpThread = null;
		parseThread = null;
	}

	/**
	 * Worker thread for loading the parser. Loading a parser usually takes ~2
	 * min
	 */
	private class LoadParserThread extends Thread {
		String filename;
		String parserType = interactive;// either 'depth-first' or
		
		// 'breadth-first'
		static final String breadthFirst = "Breadth First";
		static final String interactive = "Interactive (Best-First)";

		LoadParserThread(String filename, String parserType) {
			this.filename = filename;
			this.parserType = parserType;
			
		}

		@Override
		public void run() {
			try {

				if (this.parserType.equalsIgnoreCase(breadthFirst)) {
					parser = new ContextParser(filename);
					resetToFTALW.setEnabled(false);
					repairingOption.setEnabled(false);
					if (repairingOption.isSelected())
						logger.warn("Repair is not supported for the breadth first parser");

				} else if (this.parserType.equalsIgnoreCase(interactive)) {
					parser = new InteractiveContextParser(filename, repairingOption.isSelected());
					resetToFTALW.setEnabled(true);
					repairingOption.setEnabled(true);
					tupleViewer.setContext(((InteractiveContextParser)parser).getContext());

				} else {
					JOptionPane.showMessageDialog(ParserPanel.this,
							"Could not load parser. Invalid parser type.",
							null, JOptionPane.ERROR_MESSAGE);
					setStatus("Error loading parser");
					parser = null;

				}
				logger.info("loaded parser");
				// parser = new SimpleParser(filename);
				initParser();
				logger.info("Initialised Parser");
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(ParserPanel.this,
						"Error loading parser: " + filename + " " + ex, null,
						JOptionPane.ERROR_MESSAGE);
				setStatus("Error loading parser");
				parser = null;

			} catch (OutOfMemoryError e) {
				JOptionPane.showMessageDialog(ParserPanel.this,
						"Could not load parser. Out of memory.", null,
						JOptionPane.ERROR_MESSAGE);
				setStatus("Error loading parser");
				parser = null;
			}

			stopProgressMonitor();
			if (parser != null) {
				setStatus("Loaded parser.");
				containingFrame.setTitle("Parser: " + filename);
				parseButton.setEnabled(true);
				parseNextButton.setEnabled(true);
			}
		}
	}

	/**
	 * Worker thread for parsing.
	 */
	private class ParseThread extends Thread {

		Utterance utterance;

		public ParseThread(Utterance sentence) {

			this.utterance = sentence;

		}

		public ParseThread() {
			this.utterance = null;
		}

		@Override
		public void run() {
			boolean successful = false;
			try {
				if (utterance == null || utterance.isEmpty()) {
					JOptionPane.showMessageDialog(ParserPanel.this,
							"Nothing to parse!", null,
							JOptionPane.ERROR_MESSAGE);
					successful = true;
				} else
					successful = parser.parseUtterance(utterance);
			} catch (Exception e) {
				e.printStackTrace();
				stopProgressMonitor();
				JOptionPane
						.showMessageDialog(
								ParserPanel.this,
								"Could not parse selected sentence - exception thrown by parser",
								null, JOptionPane.ERROR_MESSAGE);
				setStatus("Error parsing");
				return;
			}

			stopProgressMonitor();
			setStatus("Done");
			if (successful) {
				if (parser instanceof InteractiveContextParser) {
					InteractiveContextParser p = (InteractiveContextParser) parser;
					tuples.clear();

					tuples.add(p.getState().getCurrentTuple());
					conPanel.update(p.getState());
				}

				displayParsedUtterance(utterance);
				displayBestParse();
				clearButton.setEnabled(true);
				nextButton.setEnabled(true);
			} else {
				JOptionPane.showMessageDialog(ParserPanel.this,
						"Could not parse selected sentence", null,
						JOptionPane.ERROR_MESSAGE);
				setStatus("Error parsing");
				tupleViewer.setTuple(null);
				clearButton.setEnabled(false);
				nextButton.setEnabled(false);
			}
			if (scrollWhenDone) {
				scrollForward();
			}
		}
	}

	/**
	 * Worker thread for generation. Generating a sentence usually takes ~5-60
	 * sec
	 */
	private class GenerateThread extends Thread {

		qmul.ds.ParserTuple tuple;

		public GenerateThread(qmul.ds.tree.Tree tree) {
			this.tuple = new ParserTuple(tree);
		}

		public GenerateThread(TTRFormula semantics) {
			this.tuple = new ParserTuple(semantics);
		}

		@Override
		public void run() {
			clearGeneratorOutput();
			boolean successful;
			try {
				generator.init(tuple);

				successful = generator.generate();
			} catch (Exception e) {
				e.printStackTrace();
				stopProgressMonitor();
				JOptionPane
						.showMessageDialog(
								ParserPanel.this,
								"Could not generate from selected tree\n(sentence probably too long)",
								null, JOptionPane.ERROR_MESSAGE);
				setStatus("Error generating");
				return;
			}

			stopProgressMonitor();
			setStatus("Done");
			if (successful) {
				clearButton.setEnabled(true);
				nextButton.setEnabled(true);
			} else {
				JOptionPane.showMessageDialog(ParserPanel.this,
						"Could not generate from selected tree", null,
						JOptionPane.ERROR_MESSAGE);
				setStatus("Error generating");
				setGeneratorOutput(null);
				clearButton.setEnabled(false);
				nextButton.setEnabled(false);
			}
		}
	}

	/**
	 * Simulates a timer to update the progress monitor
	 */
	private class TimerListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			// progressMonitor.setProgress(Math.min(count++,progressMonitor.getMaximum()-1));
			progressBar
					.setValue(Math.min(count++, progressBar.getMaximum() - 1));
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	private void initComponents()// GEN-BEGIN:initComponents
	{
		this.tabbedTuplePanel = new JTabbedPane();
		this.repairingOption = new JCheckBox("Repair Processing");
		splitPane = new javax.swing.JSplitPane();
		topPanel = new javax.swing.JPanel();
		buttonsAndFilePanel = new javax.swing.JPanel();
		loadButtonPanel = new javax.swing.JPanel();
		loadFileButton = new javax.swing.JButton();
		loadParserButton = new javax.swing.JButton();
		initParserButton = new javax.swing.JButton();
		parserTypeBox = new JComboBox(PARSER_TYPES);
		buttonPanel = new javax.swing.JPanel();
		backButton = new javax.swing.JButton();
		if (getClass().getResource("/edu/stanford/nlp/parser/ui/leftarrow.gif") != null) {
			backButton.setIcon(new javax.swing.ImageIcon(getClass()
					.getResource("/edu/stanford/nlp/parser/ui/leftarrow.gif")));
		} else {
			backButton.setText("< Prev");
		}
		forwardButton = new javax.swing.JButton();
		if (getClass()
				.getResource("/edu/stanford/nlp/parser/ui/rightarrow.gif") != null) {
			forwardButton
					.setIcon(new javax.swing.ImageIcon(getClass().getResource(
							"/edu/stanford/nlp/parser/ui/rightarrow.gif")));
		} else {
			forwardButton.setText("Next >");
		}
		parseButton = new javax.swing.JButton();
		parseNextButton = new javax.swing.JButton();
		clearButton = new javax.swing.JButton();
		nextButton = new javax.swing.JButton();
		resetToFTALW = new javax.swing.JButton();
		dataFilePanel = new javax.swing.JPanel();
		dataFileLabel = new javax.swing.JLabel();
		pTextScrollPane = new javax.swing.JScrollPane();
		gTextScrollPane = new javax.swing.JScrollPane();
		parsedTextScrollPane = new javax.swing.JScrollPane();
		pTextPane = new javax.swing.JTextPane();
		gTextPane = new javax.swing.JTextPane();
		parsedTextPane = new javax.swing.JTextPane();
		treeContainer = new javax.swing.JPanel();
		infoControlPanel = new javax.swing.JPanel();
		parserFilePanel = new javax.swing.JPanel();
		// parserFileLabel = new javax.swing.JLabel();
		treeSelectPanel = new javax.swing.JPanel();
		treeNumberLabel = new javax.swing.JLabel();
		treeNextButton = new javax.swing.JButton();
		treePrevButton = new javax.swing.JButton();
		genToButton = new javax.swing.JButton();
		treeGenFromButton = new javax.swing.JButton();
		adjustOnceButton = new javax.swing.JButton();
		exhaustButton = new javax.swing.JButton();
		statusPanel = new javax.swing.JPanel();
		statusLabel = new javax.swing.JLabel();
		progressBar = new javax.swing.JProgressBar();
		progressBar.setVisible(false);

		setLayout(new java.awt.BorderLayout());

		splitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
		topPanel.setLayout(new java.awt.BorderLayout());

		buttonsAndFilePanel.setLayout(new javax.swing.BoxLayout(
				buttonsAndFilePanel, javax.swing.BoxLayout.Y_AXIS));

		loadButtonPanel.setLayout(new java.awt.FlowLayout(
				java.awt.FlowLayout.LEFT));

		loadFileButton.setText("Load Corpus");
		loadFileButton.setToolTipText("Load a data file to parse.");
		loadFileButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				loadFileButtonActionPerformed(evt);
			}
		});
		loadFileButton.setEnabled(!isApplet);

		loadButtonPanel.add(loadFileButton);

		loadParserButton.setText("Load Grammar/Lexicon");
		loadParserButton
				.setToolTipText("Load a set of lexical and computational actions.");
		loadParserButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				loadParserButtonActionPerformed(evt);
			}
		});
		loadParserButton.setEnabled(true);

		loadButtonPanel.add(loadParserButton);

		initParserButton.setText("Reset Parser");
		initParserButton.setToolTipText("Reset the current parser state.");
		initParserButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				initParserButtonActionPerformed(evt);
			}
		});

		loadButtonPanel.add(initParserButton);
		parserTypeBox.setToolTipText("Type of Parser");
		parserTypeBox.setSelectedIndex(1);
		parserTypeBox.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				parserTypeChanged(evt);
			}

		});
		loadButtonPanel.add(parserTypeBox);

		repairingOption
				.setToolTipText("Check to enable self/other-repair via contextual backtrack and parse");
		repairingOption.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AbstractButton abstractButton = (AbstractButton) e.getSource();
		        boolean selected = abstractButton.getModel().isSelected();
				if (parser!=null)
				{
					logger.info("Setting repair processing to:"+selected);
					parser.setRepairProcessing(selected);
				}
			}
		});
		loadButtonPanel.add(repairingOption);

		buttonsAndFilePanel.add(loadButtonPanel);

		buttonPanel
				.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

		backButton.setToolTipText("Scroll backward one sentence.");
		backButton.setEnabled(false);
		backButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				backButtonActionPerformed(evt);
			}
		});

		buttonPanel.add(backButton);

		forwardButton.setToolTipText("Scroll forward one sentence.");
		forwardButton.setEnabled(false);
		forwardButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				forwardButtonActionPerformed(evt);
			}
		});

		buttonPanel.add(forwardButton);

		parseButton.setText("Parse");
		parseButton.setToolTipText("Parse selected sentence.");
		parseButton.setEnabled(false);
		parseButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				parseButtonActionPerformed(evt);
			}
		});

		buttonPanel.add(parseButton);

		parseNextButton.setText("Parse >");
		parseNextButton
				.setToolTipText("Parse selected sentence and then scrolls forward one sentence.");
		parseNextButton.setEnabled(false);
		parseNextButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				parseNextButtonActionPerformed(evt);
			}
		});

		buttonPanel.add(parseNextButton);

		clearButton.setText("Clear");
		clearButton.setToolTipText("Clears parse tree.");
		clearButton.setEnabled(false);
		clearButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				clearButtonActionPerformed(evt);
			}
		});

		// buttonPanel.add(clearButton);

		nextButton.setText("Next");
		nextButton
				.setToolTipText("Tells the parser to prepare for the next sentence.");
		nextButton.setEnabled(false);
		nextButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				nextButtonActionPerformed(evt);
			}
		});

		resetToFTALW.setText("Reset to FRALW");
		resetToFTALW
				.setToolTipText("Tells the parser to reset the state to the FIRST state after the last parsable word");
		resetToFTALW.setEnabled(false);
		resetToFTALW.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				if (parser instanceof InteractiveContextParser) {
					InteractiveContextParser p = (InteractiveContextParser) parser;
					
					p.getState().resetToFirstTupleAfterLastWord();
					tuples.clear();
					
					tuples.add(p.getState().getCurrentTuple());
					

					conPanel.update(p.getState());
					displayBestParse();
				}
			}
		});

		buttonPanel.add(nextButton);
		buttonPanel.add(resetToFTALW);
		buttonsAndFilePanel.add(buttonPanel);

		dataFilePanel.setLayout(new java.awt.FlowLayout(
				java.awt.FlowLayout.LEFT));

		dataFilePanel.add(dataFileLabel);

		buttonsAndFilePanel.add(dataFilePanel);

		topPanel.add(buttonsAndFilePanel, java.awt.BorderLayout.NORTH);

		pTextPane.setPreferredSize(new java.awt.Dimension(500, 100));
		pTextPane.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusLost(java.awt.event.FocusEvent evt) {
				textPaneFocusLost(evt);
			}
		});

		pTextPane.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				textPaneMouseClicked(evt);
			}
		});

		pTextPane
				.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
					@Override
					public void mouseDragged(java.awt.event.MouseEvent evt) {
						textPaneMouseDragged(evt);
					}
				});

		InputMap textPaneMap=pTextPane.getInputMap();
		KeyStroke enter = KeyStroke.getKeyStroke("ENTER");
	    KeyStroke shiftEnter = KeyStroke.getKeyStroke("shift ENTER");
	    textPaneMap.put(shiftEnter, "parse-release-turn");  // input.get(enter)) = "insert-break"
	    textPaneMap.put(enter, "parse");
	    ActionMap actions = pTextPane.getActionMap();
	    actions.put("parse-release-turn", new AbstractAction() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	        	parse(true);
				pTextPane.setText("");
	        }
	    });
	    actions.put("parse", new AbstractAction() {
	        @Override
	        public void actionPerformed(ActionEvent e) {
	        	parse(false);
				pTextPane.setText("");
	        }
	    });
	    
//		pTextPane.addKeyListener(new KeyListener() {
//
//			@Override
//			public void keyTyped(KeyEvent e) {
//				System.out.println(e);
//				System.out.println(e.getModifiers());
//				System.out.println("Enter:"+KeyEvent.VK_ENTER);
//				System.out.println("keycode:"+e.getKeyCode());
//				
//				if (e.getKeyChar() == KeyEvent.VK_ENTER && e.getModifiers()==1) 
//				{
//					//highlightEditedSentence();
//					parse(false);
//					pTextPane.setText("");
//					
//				}
//			}
//
//			@Override
//			public void keyReleased(KeyEvent e) {
//			}
//
//			@Override
//			public void keyPressed(KeyEvent e) {
//			}
//		});

		pTextScrollPane.setViewportView(pTextPane);

		topPanel.add(pTextScrollPane, java.awt.BorderLayout.CENTER);

		splitPane.setLeftComponent(topPanel);

		treeContainer.setLayout(new java.awt.BorderLayout());

		treeContainer.setBackground(new java.awt.Color(255, 255, 255));
		treeContainer.setBorder(new javax.swing.border.BevelBorder(
				javax.swing.border.BevelBorder.RAISED));
		treeContainer.setForeground(new java.awt.Color(0, 0, 0));
		treeContainer.setPreferredSize(new java.awt.Dimension(600, 500));

		
		tupleViewer = new ParserTupleViewer();

		this.tabbedTuplePanel.addTab("Tuple", tupleViewer);

		// semPanel = new FormulaPanel();
		// JScrollPane fs = new JScrollPane(semPanel);
		// semPanel.setContainer(fs);
		// this.tabbedTuplePanel.addTab("Semantics", fs);

		treeContainer.add("Center", this.tabbedTuplePanel);
		tupleViewer.setBackground(Color.white);
		parserFilePanel.setLayout(new java.awt.FlowLayout(
				java.awt.FlowLayout.LEFT));
		treeSelectPanel.setLayout(new java.awt.FlowLayout(
				java.awt.FlowLayout.RIGHT));

		parserFilePanel.setBackground(new java.awt.Color(255, 255, 255));
		// parserFileLabel.setText("Parser: None");
		// parserFilePanel.add(parserFileLabel);
		infoControlPanel.add(parserFilePanel, java.awt.BorderLayout.WEST);

		treeSelectPanel.setBackground(new java.awt.Color(255, 255, 255));
		treePrevButton.setText("<");
		treePrevButton.setToolTipText("View previous tree.");
		treePrevButton.setEnabled(false);
		treePrevButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				treePrevActionPerformed(evt);
			}
		});
		treeSelectPanel.add(treePrevButton);
		treeSelectPanel.add(treeNumberLabel);
		treeNextButton.setText(">");
		treeNextButton.setToolTipText("View previous tree.");
		treeNextButton.setEnabled(false);
		treeNextButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				treeNextActionPerformed(evt);
			}
		});
		treeSelectPanel.add(treeNextButton);
		genToButton.setText("Generate To");
		genToButton.setToolTipText("Generate a sentence to this tree.");
		genToButton.setEnabled(false);
		genToButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				genActionPerformed(evt);
			}
		});
		treeGenFromButton.setText("Generate From");
		treeGenFromButton
				.setToolTipText("Generate to given goal from this tuple");
		treeGenFromButton.setEnabled(false);
		treeGenFromButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						genActionPerformed(evt);
					}
				});
		treeSelectPanel.add(genToButton);
		adjustOnceButton
				.setToolTipText("Press to expand the parse state by one tree");
		adjustOnceButton.setText("Step Through");
		if (parserTypeBox.getSelectedItem().equals(PARSER_TYPES[0])) {
			adjustOnceButton.setEnabled(false);

		}
		adjustOnceButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				adjustOnce(evt);
			}
		});
		treeSelectPanel.add(adjustOnceButton);

		exhaustButton
				.setToolTipText("Press to exhaust parse state given the words so far");
		exhaustButton.setText("Exhaust");
		if (parserTypeBox.getSelectedItem().equals(PARSER_TYPES[0])) {
			exhaustButton.setEnabled(false);

		}
		exhaustButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				exhaust();
			}
		});

		treeSelectPanel.add(exhaustButton);

		infoControlPanel.add(treeSelectPanel, java.awt.BorderLayout.EAST);

		treeContainer.add(infoControlPanel, java.awt.BorderLayout.NORTH);
		parsedTextPane.setPreferredSize(new java.awt.Dimension(250, 170));
		parsedTextPane.setEditable(false);
		gTextPane.setPreferredSize(new java.awt.Dimension(250, 170));
		gTextScrollPane.setViewportView(gTextPane);
		parsedTextScrollPane.setViewportView(parsedTextPane);
		JPanel parsedGenPanel = new JPanel();
		parsedGenPanel
				.setLayout(new BoxLayout(parsedGenPanel, BoxLayout.X_AXIS));

		parsedGenPanel.add(parsedTextScrollPane);
		parsedGenPanel.add(gTextScrollPane);
		treeContainer.add(parsedGenPanel, java.awt.BorderLayout.SOUTH);

		splitPane.setRightComponent(treeContainer);

		add(splitPane, java.awt.BorderLayout.CENTER);

		statusPanel
				.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

		statusLabel.setText("Ready");
		statusPanel.add(statusLabel);

		progressBar.setName("");
		statusPanel.add(progressBar);

		add(statusPanel, java.awt.BorderLayout.SOUTH);

		// Roger -- test to see if I can get a bit of a fix with new font

	}// GEN-END:initComponents

	Utterance prevUtterance = null;

	public void displayParsedUtterance(Utterance utterance) {
		StyledDocument doc = parsedTextPane.getStyledDocument();
		SimpleAttributeSet simple = new SimpleAttributeSet();
		try {
			if (prevUtterance == null)
				doc.insertString(0, utterance + " ", simple);
			else if (utterance.getSpeaker().equals(prevUtterance.getSpeaker()))
				doc.insertString(doc.getLength(), utterance.getText() + " ",
						simple);
			else
				doc.insertString(doc.getLength(), "\n" + utterance + " ",
						simple);

			prevUtterance = utterance;
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

	}

	private void textPaneFocusLost(java.awt.event.FocusEvent evt)// GEN-FIRST:event_textPaneFocusLost
	{// GEN-HEADEREND:event_textPaneFocusLost
		// highlights the sentence containing the current location of the cursor
		// note that the cursor is set to the beginning of the sentence when
		// scrolling
		highlightEditedSentence();
	}// GEN-LAST:event_textPaneFocusLost

	private void parseNextButtonActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_parseNextButtonActionPerformed
	{// GEN-HEADEREND:event_parseNextButtonActionPerformed
		parse(false);
		scrollWhenDone = true;
	}// GEN-LAST:event_parseNextButtonActionPerformed

	private void clearButtonActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_clearButtonActionPerformed
	{// GEN-HEADEREND:event_clearButtonActionPerformed
		tupleViewer.setTuple(null);
		clearButton.setEnabled(false);
	}// GEN-LAST:event_clearButtonActionPerformed

	private void textPaneMouseDragged(java.awt.event.MouseEvent evt)// GEN-FIRST:event_textPaneMouseDragged
	{// GEN-HEADEREND:event_textPaneMouseDragged
		highlightSelectedSentence();
	}// GEN-LAST:event_textPaneMouseDragged

	private void textPaneMouseClicked(java.awt.event.MouseEvent evt)// GEN-FIRST:event_textPaneMouseClicked
	{// GEN-HEADEREND:event_textPaneMouseClicked
		pTextPane.setText("");
		// highlightSelectedSentence();
	}// GEN-LAST:event_textPaneMouseClicked

	private void repairingStateChanged(ChangeEvent e) {
		loadParser(defaultParser);
		if (parserTypeBox.getSelectedItem().equals(PARSER_TYPES[1])) {
			adjustOnceButton.setEnabled(true);
			exhaustButton.setEnabled(true);
		} else {
			adjustOnceButton.setEnabled(false);
			exhaustButton.setEnabled(false);
		}
	}

	private void parseButtonActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_parseButtonActionPerformed
	{// GEN-HEADEREND:event_parseButtonActionPerformed
		parse(false);
		scrollWhenDone = false;
	}// GEN-LAST:event_parseButtonActionPerformed

	private void nextButtonActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_parseButtonActionPerformed
	{// GEN-HEADEREND:event_parseButtonActionPerformed
		turnParser();
	}// GEN-LAST:event_parseButtonActionPerformed

	private void loadParserButtonActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_loadParserButtonActionPerformed
	{// GEN-HEADEREND:event_loadParserButtonActionPerformed
		loadParser();
	}// GEN-LAST:event_loadParserButtonActionPerformed

	private void initParserButtonActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_loadParserButtonActionPerformed
	{// GEN-HEADEREND:event_loadParserButtonActionPerformed
		initParser();
	}// GEN-LAST:event_loadParserButtonActionPerformed

	private void parserTypeChanged(ActionEvent evt) {
		loadParser(defaultParser);
		if (parserTypeBox.getSelectedItem().equals(PARSER_TYPES[1])) {
			adjustOnceButton.setEnabled(true);
			exhaustButton.setEnabled(true);
		} else {
			adjustOnceButton.setEnabled(false);
			exhaustButton.setEnabled(false);
		}

	}

	private void loadFileButtonActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_loadFileButtonActionPerformed
	{// GEN-HEADEREND:event_loadFileButtonActionPerformed
		loadFile();
	}// GEN-LAST:event_loadFileButtonActionPerformed

	private void backButtonActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_backButtonActionPerformed
	{// GEN-HEADEREND:event_backButtonActionPerformed
		scrollBack();
	}// GEN-LAST:event_backButtonActionPerformed

	private void forwardButtonActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_forwardButtonActionPerformed
	{// GEN-HEADEREND:event_forwardButtonActionPerformed
		scrollForward();
	}// GEN-LAST:event_forwardButtonActionPerformed

	private void treePrevActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_treePrevActionPerformed
	{// GEN-HEADEREND:event_treePrevActionPerformed
		displayPrevTree();
	}// GEN-LAST:event_treePrevActionPerformed

	private void treeNextActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_treeNextActionPerformed
	{// GEN-HEADEREND:event_treeNextActionPerformed
		displayNextTree();
	}// GEN-LAST:event_treeNextActionPerformed

	private void genActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_treeNextActionPerformed
	{// GEN-HEADEREND:event_treeNextActionPerformed
		// System.out.println(tabbedTuplePanel.getSelectedComponent().get);
		if (tabbedTuplePanel.getTitleAt(tabbedTuplePanel.getSelectedIndex())
				.equals("Semantics"))
			generateFromThisSemantics();
		else
			generateFromThisTree();
	}// GEN-LAST:event_treeNextActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel dataFileLabel;
	private javax.swing.JPanel treeContainer;
	private javax.swing.JPanel topPanel;
	private javax.swing.JScrollPane pTextScrollPane;
	private javax.swing.JScrollPane gTextScrollPane;
	private javax.swing.JScrollPane parsedTextScrollPane;
	private javax.swing.JTextPane parsedTextPane;
	private javax.swing.JButton backButton;
	private javax.swing.JLabel statusLabel;
	private javax.swing.JButton loadFileButton;
	private javax.swing.JPanel loadButtonPanel;
	private javax.swing.JPanel buttonsAndFilePanel;
	private javax.swing.JButton parseButton;
	private javax.swing.JButton parseNextButton;
	private javax.swing.JButton forwardButton;
	// private javax.swing.JLabel parserFileLabel;
	private javax.swing.JLabel treeNumberLabel;
	private javax.swing.JButton clearButton;
	private javax.swing.JButton nextButton;
	private javax.swing.JButton resetToFTALW;
	private javax.swing.JSplitPane splitPane;
	private javax.swing.JPanel statusPanel;
	private javax.swing.JPanel dataFilePanel;
	private javax.swing.JPanel buttonPanel;
	private javax.swing.JTextPane pTextPane;
	private javax.swing.JTextPane gTextPane;
	private javax.swing.JProgressBar progressBar;
	private javax.swing.JPanel infoControlPanel;
	private javax.swing.JPanel parserFilePanel;
	private javax.swing.JPanel treeSelectPanel;
	private javax.swing.JButton loadParserButton;
	private javax.swing.JButton initParserButton;
	private javax.swing.JButton treePrevButton;
	private javax.swing.JButton treeNextButton;
	private javax.swing.JButton genToButton;
	private javax.swing.JButton treeGenFromButton;
	private javax.swing.JComboBox parserTypeBox;
	private javax.swing.JButton adjustOnceButton;
	private javax.swing.JButton exhaustButton;
	private javax.swing.JCheckBox repairingOption;
	// End of variables declaration//GEN-END:variables
	
	public DSParser getParser() {
		return this.parser;
	}

	public javax.swing.JLabel getStatusLabel() {
		return this.statusLabel;
	}

	public javax.swing.JTextPane getpTextPane() {
		return pTextPane;
	}

}
