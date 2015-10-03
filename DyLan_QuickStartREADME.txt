DyLan QUICK START

1. Clone the latest versions of dsttr and dylan_util from BitButcket into your workspace from:

https://bitbucket.org/dylandialoguesystem/dsttr.git
https://bitbucket.org/dylandialoguesystem/dylan_util.git

2. We recommend using Eclipse to work with DyLan. If using Eclipse, create two projects from the two folders dsttr and dylan_util. dsttr is the main repository you will be working from. In the build path make sure it depends on dylan_util (i.e. in Eclipse right-click on dsttr then go “Configure Build Path”, switch to the “Projects” tab, then if dylan_util is not there add it through “Add..” Remove any other projects in the Projects tab for now, which may be incorrect references to this folder.)

3. Make sure you are using UTF-8 as your default text encoding as certain characters can cause problems. In Eclipse go Preferences > General > Content Types, set UTF-8 as the default encoding for all content types.

4. Test the DSTTR parser: From the source folders run src/java/qmul.ds.gui.ParserGUI as a java application. From that parser window, you can select ‘Load Grammar/Lexicon’, from which you must select a sub-folder from the ‘resource’ folder, e.g. 2013-english-ttr and press OK. Then you can type words into the top text bar and press ‘Parse’. You can choose between ‘Interactive (Best-First)’ and ‘Breadth First’ modes, where the former is a best-first parser, capable of parsing dialogues (see below, but more documentation forthcoming) and the latter is a breadth-first incremental parser. You can view the Tree or the Semantics (TTR formulae) as they grow as you parse each word.

5. You can add as arguments to the Eclipse project the grammar folder you prefer to avoid having to select from scratch each time.


THE INTERACTIVE PARSER

Load this by selecting the Interactive (Best-First) mode. To parse normally, just type words into the big box, press enter or press parse. You will see the words you parse appear in the lower left box incrementally as you parse them, but you’ll notice that the words you parse will have a default speaker, “S”. 

To parse dialogue turns, prefix the words in the turn by the speaker followed by a colon, e.g. “A: john snores”. If you don’t provide the speaker, the parser will assume words are coming from the previously provided speaker.

The Context Tab in the middle will show the DS-TTR context, which is a Directed Acyclic Graph (DAG), (see Eshghi et. al. 2015) where edges are words, and nodes are tuples with each tuple containing a DS tree and a TTR Record Type semantics. To inspect a tuple, just click on it. This will open a new window, with two tabs, one for the tree and another for the semantics. Edges are annotated with words, and grounding (including speaker) information.

The Tuple Tab in the middle will show the current best parse (the right-most node of the context DAG). To see more parses, click on the “Step Through” button. 

Fuller documentation forthcoming, but see Eshghi et. al. (2011); Eshghi et. al. (2015).

REFERENCES

Arash Eshghi, Matthew Purver and Julian Hough. DyLan: Parser for Dynamic Syntax. Technical Report EECSRR-11-05, School of Electronic Engineering and Computer Science, Queen Mary University of London, October 2011.

Arash Eshghi, Christine Howes, Eleni Gregoromichelaki, Julian Hough and Matthew Purver. Feedback in Conversation as Incremental Semantic Update. In Proceedings of the 11th International Conference on Computational Semantics (IWCS), London, April 2015.
