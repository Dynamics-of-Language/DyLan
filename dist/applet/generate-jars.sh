#*******************************************************************************
# Copyright (c) 2001, 2010 Matthew Purver
# All Rights Reserved.  Use is subject to license terms.
#
# See the file "LICENSE" for information on usage and
# redistribution of this file, and for a DISCLAIMER OF ALL
# WARRANTIES.
#*******************************************************************************
#!/bin/bash

cd bin
/cygdrive/c/Program\ Files/Java/jdk1.6.0_16/bin/jar cvfm ../Parser.jar ../manifest-standalone.txt qmul
/cygdrive/c/Program\ Files/Java/jdk1.6.0_16/bin/jarsigner -storepass fatbast ../Parser.jar qmul
chmod 755 ../Parser.jar
/cygdrive/c/Program\ Files/Java/jdk1.6.0_16/bin/jar cvfm ../ParserApplet.jar ../manifest-applet.txt qmul
/cygdrive/c/Program\ Files/Java/jdk1.6.0_16/bin/jarsigner -storepass fatbast ../ParserApplet.jar qmul
chmod 755 ../ParserApplet.jar
