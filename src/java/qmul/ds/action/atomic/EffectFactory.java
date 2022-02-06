/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.action.atomic;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

import qmul.ds.action.LexicalAction;
import qmul.ds.action.LexicalMacro;
import qmul.ds.action.Lexicon;

/**
 * A factory to create {@link Effect}s from their {@link String} specs
 * 
 * @author mpurver
 */
public class EffectFactory {

	private static Logger logger = Logger.getLogger(EffectFactory.class);

	private static HashMap<String, MacroTemplate> macroTemplates = new HashMap<String, MacroTemplate>();

	public static Effect create(List<String> lines) {

		if (lines.size() == 1) {
			return create(lines.get(0));
		} else if (lines.get(0).toLowerCase().startsWith(IfThenElse.IF_FUNCTOR.toLowerCase())) {
			return new IfThenElse(lines);
		}
		throw new IllegalArgumentException("unrecognised action lines " + lines);
	}

	public static Effect[] createMultiple(List<String> lines, List<Integer> ifIndeces) {

		if (ifIndeces.isEmpty())
			throw new IllegalArgumentException(
					"Bad IfThenElse lines. Likely due to top level IF functors not being at the beginning of the line");

		Effect[] result = new IfThenElse[ifIndeces.size()];

		for (int i = 0; i < ifIndeces.size() - 1; i++) {
			result[i] = create(lines.subList(ifIndeces.get(i), ifIndeces.get(i + 1)));

		}
		result[ifIndeces.size() - 1] = create(lines.subList(ifIndeces.get(ifIndeces.size() - 1), lines.size()));

		return result;

	}

	public static List<Integer> getIfIndices(List<String> lines) {
		List<Integer> indices = new ArrayList<Integer>();
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).toLowerCase().startsWith(IfThenElse.IF_FUNCTOR.toLowerCase()))
				indices.add(i);
		}
		return indices;
	}

	public static Effect create(String line) {
		if (line.toLowerCase().startsWith(Abort.FUNCTOR.toLowerCase())) {
			return new Abort();
		} else if (line.toLowerCase().startsWith(AddAxiom.FUNCTOR.toLowerCase())) {
			return new AddAxiom();
		}else if (line.toLowerCase().startsWith(InferSpeechAct.FUNCTOR.toLowerCase())) {
			return new InferSpeechAct();
		}else if (line.toLowerCase().startsWith(OpenFloor.FUNCTOR.toLowerCase())) {
			return new OpenFloor();
		}else if (line.toLowerCase().startsWith(Unreduce.FUNCTOR.toLowerCase())) {
			return new Unreduce();
		}
		else if (line.toLowerCase().startsWith(Unassert.FUNCTOR.toLowerCase())) {
			return new Unassert();
		}
		else if (line.toLowerCase().startsWith(GroundToRoot.FUNCTOR.toLowerCase())) {
			return new GroundToRoot();
		}else if (line.toLowerCase().startsWith(Make.FUNCTOR.toLowerCase())) {
			return new Make(line);
		} else if (line.toLowerCase().startsWith(EmptyEffect.FUNCTOR.toLowerCase())) {
			return new EmptyEffect();
		} else if (line.toLowerCase().startsWith(CopyContent.FUNCTOR)) {
			return new CopyContent(line);
		} else if (line.toLowerCase().startsWith(Put.FUNCTOR.toLowerCase())) {
			return new Put(line);
		} else if (line.toLowerCase().startsWith(Delete.FUNCTOR.toLowerCase())) {
			return new Delete(line);
		} else if (line.toLowerCase().startsWith(GoFirst.FUNCTOR.toLowerCase())) {
			return new GoFirst(line);
		} else if (line.toLowerCase().startsWith(GoLocalEvent.FUNCTOR.toLowerCase())) {
			return new GoLocalEvent(line);
		} else if (line.toLowerCase().startsWith(Go.FUNCTOR.toLowerCase())) {
			return new Go(line);
		} else if (line.toLowerCase().startsWith(BetaReduce.FUNCTOR.toLowerCase())) {
			return new BetaReduce();
		} else if (line.toLowerCase().startsWith(Merge.FUNCTOR.toLowerCase())) {
			return new Merge(line);
		} else if (line.toLowerCase().startsWith(Conjoin.FUNCTOR.toLowerCase())) {
			return new Conjoin(line);
		} else if (line.toLowerCase().startsWith(Do.FUNCTOR.toLowerCase())) {
			return new Do(line);
		} else if (line.toLowerCase().startsWith(FreshPut.FUNCTOR.toLowerCase())) {
			return new FreshPut(line);
		} else if (line.toLowerCase().startsWith(SaturateScopeDep.FUNCTOR.toLowerCase())) {
			return new SaturateScopeDep(line);
		} else if (line.toLowerCase().startsWith(TTRFreshPut.FUNCTOR.toLowerCase())) {
			return new TTRFreshPut(line);
		} else if (line.toLowerCase().startsWith(RDFFreshPut.FUNCTOR.toLowerCase())) {
			return new RDFFreshPut(line);
		} else {
			//System.out.println("line didn't match:" + line);
			return createLexicalMacro(line);

		}
		// throw new IllegalArgumentException("unrecognised action line " +
		// line);
	}

	private static LexicalMacro createLexicalMacro(String line) {

		if (macroTemplates.containsKey(line.trim())) {

			return new LexicalMacro(line.trim(), macroTemplates.get(line.trim()).getLines());
		}
		String name;
		Matcher m = Lexicon.TEMPLATE_SPEC_PATTERN.matcher(line);
		List<String> argValues = new ArrayList<String>();
		if (m.matches()) {
			name = m.group(1);
			for (String s : m.group(2).split(",")) {
				argValues.add(s);
			}
		} else {
			throw new IllegalArgumentException("ERROR: syntax error in macro call " + line);
		}
		if (macroTemplates.containsKey(name)) {
			MacroTemplate template = macroTemplates.get(name);
			logger.debug("found template:" + name);
			logger.debug("argValues are:" + argValues);
			return template.create(argValues);
		} else {

			throw new IllegalArgumentException("ERROR: Unrecognised macro " + name + " in line:" + line);

		}
	}

	public static void initMacroTemplates(BufferedReader reader) {

		macroTemplates = new HashMap<String, MacroTemplate>();
		try {

			String line;
			String name = null;
			List<String> metavars = new ArrayList<String>();
			List<String> lines = new ArrayList<String>();
			while ((line = reader.readLine()) != null) {
				line = Lexicon.comment(line.trim());
				if ((line == null) || (line.isEmpty() && lines.isEmpty())) {
					continue;
				}
				if (line.isEmpty() && !lines.isEmpty()) {
					macroTemplates.put(name, new MacroTemplate(name, metavars, lines));
					logger.debug("Added Macro Template for " + name);
					lines.clear();
					name = null;
					metavars.clear();
				} else if (name == null) {
					Matcher m = Lexicon.MACRO_SPEC_PATTERN.matcher(line);
					if (m.matches()) {
						name = m.group(1);
						if (m.groupCount() == 2 && !m.group(2).isEmpty()) {
							boolean hasMeta = false;
							for (String s : m.group(2).substring(1, m.group(2).length() - 1).split(",")) {
								if (s.matches("[A-Z]+")) {
									metavars.add(s);
									hasMeta = true;
								} else if (hasMeta) {
									throw new IllegalArgumentException(
											"cannot mix macro meta variable with instantiated ones at the moment "
													+ line);

								}

							}
							if (!hasMeta)
								name = line.trim();
						} else {
							name = line.trim();

						}
					} else {
						throw new IllegalArgumentException("unrecognised template spec " + line);
					}
					logger.debug("New Macro template: " + name);
				} else {
					lines.add(line);
				}
			}
			if (!lines.isEmpty()) {
				macroTemplates.put(name, new MacroTemplate(name, metavars, lines));
				logger.debug("Added template for " + name);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Error reading templates from stream " + reader);
		}

	}

	public static void clearMacroTemplates() {
		macroTemplates.clear();

	}
}

class MacroTemplate {

	private String name;
	private List<String> metavars;
	private List<String> lines;

	/**
	 * @param name
	 */
	public MacroTemplate(String name, List<String> metavars, List<String> lines) {
		this.name = name;
		this.metavars = new ArrayList<String>(metavars);
		this.lines = new ArrayList<String>(lines);
	}

	public List<String> getLines() {
		return lines;
	}

	/**
	 * @param word
	 * @param metavals
	 * @return a new {@link LexicalAction} instantiation for this word and metavariable values
	 */
	public LexicalMacro create(List<String> metavals) {
		ArrayList<String> lines = new ArrayList<String>();
		for (String line : this.lines) {
			for (int i = 0; i < metavars.size(); i++) {
				line = line.replaceAll(metavars.get(i), metavals.get(i));
			}
			lines.add(line);
		}
		return new LexicalMacro(name, lines);
	}
	

}
