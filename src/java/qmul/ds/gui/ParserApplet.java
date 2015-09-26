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

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JApplet;

/**
 * A simple {@link JApplet} for web-based parsing. Based on {@link ParserGUI}. Allows a user to load a parser created
 * using lexparser.LexicalizedParser, load a text data file or type in text, parse sentences within the input text, and
 * view the resultant parse tree.
 * <p/>
 * 
 * @author mpurver
 * @author Huy Nguyen (htnguyen@cs.stanford.edu)
 * @see {@link ParserPanel}, {@link ParserGUI}
 */
@SuppressWarnings("serial")
public class ParserApplet extends JApplet {

	private ParserGUI parserGUI;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.applet.Applet#init()
	 */
	public void init() {
		parserGUI = new ParserGUI(null, null, true);
		setContentPane(parserGUI.getContentPane());
		setJMenuBar(parserGUI.getJMenuBar());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.applet.Applet#start()
	 */
	public void start() {
		String grammar = getParameter("grammar");
		if (grammar != null) {
			grammar = getFileURL(grammar).toString();
			parserGUI.getParserPanel().loadParser(grammar);
		}
		String corpus = getParameter("corpus");
		if (corpus != null) {
			corpus = getFileURL(corpus).toString();
			parserGUI.getParserPanel().loadFile(corpus);
		}
	}

	private URL getFileURL(String filename) {
		try {
			return new URL(getCodeBase(), filename);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

}
