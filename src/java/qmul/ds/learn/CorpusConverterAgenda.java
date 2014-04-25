package qmul.ds.learn;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.formula.TTRField;
import qmul.ds.formula.TTRRecordType;

/**
 * Graph-based agenda for converting CHILDES formulae to TTR record types
 * 
 * @author Julian
 * 
 */
public class CorpusConverterAgenda {


	public static int[] graphNodeNumbers = new int[100]; // All nodes need to be added in order
	public static String[] childesStrings = new String[100];
	public static TTRField[] ttrFields = new TTRField[100];
	public static boolean[] resolveds = new boolean[100];
	public static int[] parentNodeNumbers = new int[100];
	
	public static int newNode = -1; // keeps track of all nodes, starts at root

	public CorpusConverterAgenda() {
		init();
	}

	public int addtoAgenda(String childesString, String TTRString, int parentNode) {
		newNode++;
		childesStrings[newNode] = childesString;
		if (TTRString.contains("==")) {
			String label = TTRString.substring(0, TTRString.indexOf("=="));
			String rest = TTRString.substring(TTRString.indexOf("=="));
			String ending = "";
			if (rest.contains("(")) {
				ending = rest.substring(rest.indexOf("("));
				rest = rest.substring(0, rest.indexOf("("));
			}

			Matcher m = Pattern.compile("(.*)([0-9])(.*)").matcher(rest);
			if (m.matches() && !label.startsWith("head") && !label.startsWith("r")) {
				// System.out.println("matching");
				TTRString = label + numberReplace(rest) + ending;
			}
		}
		ttrFields[newNode] = TTRField.parse(TTRString.toLowerCase().replace('-', '_').replace('&', '_'));
		resolveds[newNode] = false;
		parentNodeNumbers[newNode] = parentNode;
		System.out.println("added to agenda :" + newNode + " " + childesString + "," + ttrFields[newNode] + ","
				+ resolveds[newNode] + "," + parentNodeNumbers[newNode]);
		// pause();
		return newNode;
	}

	public List<TTRField> resolvedTTRs() { // returns all the resolved TTR strings/record type
		List<TTRField> ttrs = new ArrayList<TTRField>();
		for (int j = newNode; j >= 0; j--) { // backwards iteration through nodes (children > parents)
			System.out.println("Node " + j);
			if (ttrFields[j] == null && resolveds[j] == true) { // for placeholders
				continue;
			}
			if (resolveds[j] == true) {
				System.out.println(ttrFields[j]);
				ttrs.add(ttrFields[j]);// if ttrAdded parent, need to make sure this is on the
			} else if (allDaughtersResolved(j) == true && ttrFields[j] != null) {
				resolveds[j] = true;
				ttrs.add(ttrFields[j]);
			}
		}
		System.out.println(ttrs.toString());
		return ttrs;
	}

	/**
	 * Sorts a record type acc its dependencies
	 */
	public static String sort(List<TTRField> fields, boolean headmove) {
		System.out.println("sorting :");
		for (TTRField field : fields) {
			System.out.print(field.toString()+"|");
		}
		boolean swapped = true;
		while (swapped == true) {
			swapped = false;
			for (int i = 0; i < fields.size() - 1; i++) {
				int dependent = i; // i is dependent on itself, will leave in place, unless
				TTRField field = fields.get(i);
				if (!field.isManifest()) { // only moving predicates, or head
					if (field.getLabel().toString().equalsIgnoreCase("head")&&headmove==true) {
						System.out.println("orig" +fields.toString());
						System.out.println("swapping"+field.toString() +"in" + fields.get(i+1).toString());
						fields.add(i + 2, field);
						fields.remove(i);
						System.out.println(fields.toString());
						swapped = true;
					} else {
						continue;
					}
				} else {
					List<String> arguments = new ArrayList<String>();
					String body = field.getType().toString().substring(field.getType().toString().indexOf("(") + 1,
									field.getType().toString().length() - 1);
					String arg = "";
					for (char c : body.toCharArray()) {
						if (c == ',') {
							arguments.add(arg);
							arg = "";
						} else if (c != ' ') {
							arg += c;
						}
					}
					arguments.add(arg); // add final conjunct
					// now iterate over j
					for (int j = i + 1; j < fields.size(); j++) {
						for (String myarg : arguments) {
							String label = fields.get(j).getLabel().toString();
							if (label.equals(myarg)
									|| (myarg.contains(".") && label.equals(myarg.substring(0, myarg.indexOf("."))))) {
								dependent = j + 1;
								swapped = true;
							}
						}
					}
					if (swapped == true) {
						fields.add(dependent, field);
						fields.remove(i);
						break;

					}
				}
			}
		}

		String ttrString = "[";
		for (int f = 0; f < fields.size(); f++) {
			System.out.println("f" + f + " " + fields.get(f).toString());
			ttrString += fields.get(f).toString();
			if (f == fields.size() - 1) {
				ttrString += "]";
			} else {
				ttrString += "|";
			}
		}
		System.out.println(ttrString);
		return ttrString;
	}

	public static void putTTRstring(String TTRString, int node) {
		if (TTRString == null) {
			ttrFields[node] = null;
			System.out.println("putting " + TTRString + " on " + node);
			return;
		}

		if (TTRString.contains("==")) {
			String label = TTRString.substring(0, TTRString.indexOf("=="));
			String rest = TTRString.substring(TTRString.indexOf("=="));
			String ending = "";
			if (rest.contains("(")) {
				ending = rest.substring(rest.indexOf("("));
				rest = rest.substring(0, rest.indexOf("("));
			}

			Matcher m = Pattern.compile("(.*)([0-9])(.*)").matcher(rest);
			if (m.matches() && !label.startsWith("head") && !label.startsWith("r")) {
				// System.out.println("matching");
				TTRString = label + numberReplace(rest) + ending;
			}
		}
		ttrFields[node] = TTRField.parse(TTRString.toLowerCase().replace('-', '_').replace('&', '_'));
		System.out.println("putting " + TTRString + " on " + node);
	}

	public static void makeResolved(int node) {
		resolveds[node] = true;
		System.out.println("making Resolved node" + node + ":" + ttrFields[node]);
	}

	/**
	 * Resolves this node iff all other nodes are resolved otherwise leaves it as false
	 */
	public static void resolveEventRestrictor(int eventRestrnode) {
		if (newNode == -1) { // i.e. only have the event node, not an utterance
			System.out.print("not complete");
			return;
		}
		if (getDaughters(eventRestrnode).size() > 0) {
			if (allDaughtersResolved(eventRestrnode) == true) {
				if (ttrFields[eventRestrnode].getLabel().toString().startsWith("r")) {
					resolveRestrictor(eventRestrnode);
				} else {
					resolveds[eventRestrnode] = true;
					System.out.print("Making restrictor node " + eventRestrnode + " resolved");
				}
				return;
			}
		}

		// no daughters, see if all others are resolved
		for (int i = 0; i <= newNode; i++) {
			if (i == eventRestrnode) { // should never be this as -1, safety
				continue;
			}
			if (resolveds[i] == false) {
				System.out.print("not complete");
				return;
			}
		}
		System.out.print("Making restrictor node " + eventRestrnode + " resolved");
		resolveds[eventRestrnode] = true;

	}

	/**
	 * Compiles resolved node daughters into it and makes itself resolved Should only be applied if all children's
	 * children committed too
	 */
	public static void resolveRestrictor(int node) {
		TTRRecordType restrictor = TTRRecordType.parse(ttrFields[node].getType().toString());
		System.out.println(restrictor.toString() + "at" + node);
		for (int i : getDaughters(node)) {
			if (ttrFields[i] == null) {
				continue;
			}
			System.out.println("adding" + ttrFields[i].toString());
			restrictor.add(ttrFields[i]);
			ttrFields[i] = null; // get rid of the content to avoid adding twice
		}
		// hopefully won't need to sort it as TTRRecordType class will take care of it
		ttrFields[node] = TTRField.parse(ttrFields[node].getLabel().toString() + ":" + restrictor.toString());
		resolveds[node] = true;
		System.out.println("Resolved restrictor at " + node + " : " + ttrFields[node].toString());
	}

	public TTRField getTTRField(int node) {
		return ttrFields[node];
	}

	/**
	 * just turn into a place holder that doesn't add content
	 */
	public static void removeNode(int node) {
		childesStrings[node] = null;
		ttrFields[node] = null;
		resolveds[node] = true;
		parentNodeNumbers[node] = 0;

	}

	public boolean resolved(int node) {
		return resolveds[node];
	}

	public boolean isComplete() {

		if (newNode == -1) {
			System.out.print("not complete");
			return false;
		}

		for (int i = 0; i <= newNode; i++) {
			if (resolveds[i] == false) {
				System.out.print("not complete");
				return false;

			}
		}
		System.out.print("COMPLETE!");
		return true;
	}

	public static String numberReplace(String withNumbers) {
		String[] numbers = { "zero_", "one_", "two_", "three_", "four_", "five_", "six_", "seven_", "eight_", "nine_" };

		for (int i = 0; i < 10; i++) {
			// System.out.println(String.valueOf(i));
			if (withNumbers.contains(String.valueOf(i))) {
				withNumbers = withNumbers.replaceAll(String.valueOf(i), numbers[i]);
			}
		}
		return withNumbers;
	}

	public static boolean allDaughtersResolved(int node) {
		boolean hasDaughters = false;
		for (int i = node; i <= newNode; i++) {
			if (parentNodeNumbers[i] == node) {
				hasDaughters = true;
				if (resolveds[i] == false) {
					System.out.print("has daughters false node " + node);
					return false;
				}
			}
		}
		if (hasDaughters == true) {
			System.out.print("has daughters true " + node);
			return true;
		}
		System.out.print("has daughters false " + node);
		return false;
	}

	public static List<Integer> getDaughters(int node) {
		List<Integer> daughters = new ArrayList<Integer>();
		for (int i = node; i <= newNode; i++) {
			if (parentNodeNumbers[i] == node) {
				daughters.add(i);
			}
		}

		return daughters;
	}

	public static void printState() {
		// System.out.println("new node = " + newNode);
		String state = "\nSTATE: ";
		int currentParent = -1;
		for (int i = 0; i <= newNode; i++) {
			// System.out.println("parent node = " + parentNodeNumbers[i]);

			if (parentNodeNumbers[i] != currentParent) {
				currentParent = parentNodeNumbers[i];
				state += "\n"; // new level on graph/tree
			}
			state = state + parentNodeNumbers[i] + "," + i + " : " + "%%%" + childesStrings[i] + "%%%," + "["
					+ ttrFields[i] + "]" + "," + resolveds[i] + "; ";

		}
		System.out.println(state);

	}

	public static void pause() {
		System.out.println("Press enter to continue...");
		try {
			System.in.read();
		} catch (Exception e) {
		}

	}

	public static void printNode(int i) {
		String mynode = parentNodeNumbers[i] + "," + i + " : " + "%%%" + childesStrings[i] + "%%%," + "["
				+ ttrFields[i] + "]" + "," + resolveds[i] + "; ";
		System.out.println(mynode);
	}

	public static void init() {
		childesStrings = new String[100];
		ttrFields = new TTRField[100];
		resolveds = new boolean[100];
		parentNodeNumbers = new int[100]; 
		newNode = -1;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		CorpusConverterAgenda ag = new CorpusConverterAgenda();
		ag.addtoAgenda("blah", "p1==3S_feature(r1,r1.head):t", ag.newNode);

	}

}
