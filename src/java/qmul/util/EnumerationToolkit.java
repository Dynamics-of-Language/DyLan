/*
 * EnumerationToolkit.java
 *
 * Created on 16 January 2008, 18:14
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package qmul.util;

import java.util.Enumeration;
import java.util.Vector;

/**
 * 
 * @author user
 */
public class EnumerationToolkit {
	public static Object[] convertEnumerationToArray(Enumeration e) {
		Vector v = new Vector();
		while (e.hasMoreElements()) {
			Object o = e.nextElement();
			v.addElement(o);
		}
		return v.toArray();
	}
}
