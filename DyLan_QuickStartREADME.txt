DyLan QUICK START

1. Clone the latest versions of dsttr and dylan_util from BitButcket into your workspace from:

https://bitbucket.org/dylandialoguesystem/dsttr.git
https://bitbucket.org/dylandialoguesystem/dylan_util.git

2. We recommend using Eclipse to work with DyLan. If using Eclipse, create two projects from the two folders dsttr and dylan_util. dsttr is the main repository you will be working from. In the build path make sure it depends on dylan_util (i.e. in Eclipse right-click on dsttr then go “Configure Build Path”, switch to the “Projects” tab, then if dylan_util is not there add it through “Add..” Remove any other projects in the Projects tab for now, which may be incorrect references to this folder.)

3. Make sure you are using UTF-8 as your default text encoding as certain characters can cause problems. In Eclipse go Preferences > General > Content Types, set UTF-8 as the default encoding for all content types.

4. Test the DSTTR parser: From the source folders run src/java/qmul.ds.gui.ParserGUI as a java application. From that parser window, you can select ‘Load Grammar/Lexicon’, from which you must select a sub-folder from the ‘resource’ folder, e.g. 2013-english-ttr and press OK. Then you can type words into the top text bar and press ‘Parse’. You can choose between ‘Depth First’ and ‘Breadth First’ modes, where the former is faster and the latter more robust. You can view the Tree or the Semantics (TTR formulae) as they grow as you parse each word.

5. You can add as arguments to the Eclipse project the grammar folder you prefer to avoid having to select from scratch each time.