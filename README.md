# DyLan: Parser for Dynamic Syntax

Copyright (c) 2001-2010 Matthew Purver,
All Rights Reserved.

Copyright (c) 2011-2019 Arash Eshghi,
All Rights Reserved.

Original parser author: Matthew Purver
Later Code Contributions: Arash Eshghi, Julian Hough, Yo Sato
Current release prepared by: Arash Eshghi

This package contains a Java implementation of a word by word incremental NL-parser & generator in the Dynamic Syntax (DS)
framework, including prototype grammatical rules and a few lexicons for the English language. The lexicons are
small but easy to extend as the implementation covers a broad range of structures including relative
clauses and tense. It also includes code for learning DS grammars from data.



# QUICKSTART
-----------------------------------------------

See the [QUICKSTART Wiki Page](https://bitbucket.org/dylandialoguesystem/dsttr/wiki/Dylan%20Quick%20Setup%20Guide)

# CONTENTS
-----------------------------------------------
README.txt

  This file.

LICENSE.txt

  DyLan Dynamic Syntax Parser is licensed under the GNU LESSER GENERAL PUBLIC LICENSE (version 3+)

src

  A directory containing the Java 1.8 source code for the distribution.


resources

  A directory containing a number of grammars and lexicons for the English language, 
at the different developmental stages of the Dynamic Syntax framework. All resources 
include at least the following files:

* _computational-actions.txt_: contains the grammar of the language. This is a specification
of a set of procedural grammatical rules, in the (meta-)language of Dynamic Syntax.

* _lexical-actions.txt_: contains the lexical actions corresponding to the words of the language 
as specified in the lexicon (3 below). 

* _lexicon.txt_: this is a list of words, mapped onto syntactic categories (verb, noun, determiner etc.).

later resources include also:

* _lexical-macros.txt_: a set of procedures/macros used by lexical rule specifications in lexical-actions.txt.

* _speech-act-inference-grammar.txt_: a set of procedures used to map semantics to speech act specification within a particular domain.

javadoc

  Javadocs for the distribution. Some of these are as yet incomplete. Work is under-way to improve the documentation.

log
  The programme uses log4j to make debugging easier. This directory contains the output of the logger statements 
(for tracing or debugging) found in various places in the code, in addition to the gui.out file which contains all
standard outputs (e.g. System.err or System.out as well as logger statements).

# THANKS
-----------------------------------------------

Thanks to the members of the Dynamics of Conversational Dialogue (DynDial) project
for great collaborative work on what is here implemented.

  http://www.kcl.ac.uk/research/groups/ds/projects.html

# LICENSE
-----------------------------------------------

DyLan Dynamic Syntax Parser
Copyright (c) 2001-2010 Matthew Purver,
All Rights Reserved.

Copyright (c) 2011-2019 Arash Eshghi,
All Rights Reserved.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the LESSER GENERAL PUBLIC LICENSE 
 as published by the Free Software Foundation; either version 3
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 Lesser GNU General Public License for more details.

 You should have received a copy of the Lesser GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

	

# CONTACT
-----------------------------------------------

For more information, bug reports & fixes, contact:

	Arash Eshghi
	School of Mathematical and Computer Sciences
	Heriot-Watt University
    	Edinburgh E14 4AS

    Email: a.eshghi@hw.ac.uk
    
    Or
    
    Julian Hough
    School of Electronic Engineering and Computer Science
    Queen Mary University of London
    London E1 4NS
    
    Email: j.hough@qmul.ac.uk
    
    Or 
    
    Matthew Purver
    School of Electronic Engineering and Computer Science
    Queen Mary University of London
    London E1 4NS
    
    Email: m.purver@qmul.ac.uk
    
    
