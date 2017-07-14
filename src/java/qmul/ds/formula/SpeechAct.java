package qmul.ds.formula;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qmul.ds.action.meta.Meta;
/**
 * A Speech Act Specification. Consists of speech act force, and (optional) arguments.
 * 
 * e.g. info(color:red,shape:square)
 * 
 * Represented internally as a record type with meaningful, predefined labels, viz.
 * 
 * [act:info|actor:arash|color:red|shape:square]
 * 
 * @author arash
 *
 */

public class SpeechAct extends Formula {

	
	private static final long serialVersionUID = -5968193016795429419L;

	private TTRRecordType  content;
	
	public static final String SPEECH_ACT_PATTERN="(.+)\\(([a-zA-Z1-9]+)(\\s*,\\s*.+)*\\)";
	
	public SpeechAct(String spec)
	{
		Matcher m = Pattern.compile(SPEECH_ACT_PATTERN).matcher(spec);
		if (m.matches()) {
			String content;
			String force = m.group(1);
			String actor = m.group(2);
			
			if (force==null)
			{
				throw new IllegalArgumentException("Bad speech act specification:"+spec);
			}
			else if (actor==null)
			{
				throw new IllegalArgumentException("Bad speech act specification:"+spec);
			}
			
			String args = m.group(3);
			
			if (args==null)
			{
				content="[act:"+force+"|"+"actor:"+actor+"]";
			}
			else
			{
				args=args.trim();
				args=args.substring(1, args.length());
				content="[act:"+force+"|"+"actor:"+actor+"|"+args.replace(',', '|')+"]";
				
			}
			
			
			
			this.content=TTRRecordType.parse(content);

		} else
			throw new IllegalArgumentException("Unrecognized speech act: " + spec);
	}
	
	public SpeechAct(TTRRecordType content)
	{
		this.content=content;
	}
	
	public SpeechAct(SpeechAct other)
	{
		this.content=other.content.clone();
	}
	
	@Override
	public SpeechAct substitute(Formula f1, Formula f2) {
		
		return new SpeechAct(this.content.substitute(f1, f2));
	}

	@Override
	public SpeechAct clone() {
		
		return new SpeechAct(this);
	}

	@Override
	public int toUniqueInt() {
		
		return 0;
	}
	
	
	public String toString()
	{
		String result=content.get(new TTRLabel("act")).toString()+"("+content.get(new TTRLabel("actor")).toString()+", ";
		for(TTRField f:content.getFields())
		{
			if (f.getLabel().getName().equals("act")||f.getLabel().getName().equals("actor"))
				continue;
			result+=f.toString()+", ";
			
		}
		
		result=result.substring(0, result.length()-2)+")";
		return result;
	}
	
	
	
	
	public ArrayList<Meta<?>> getMetas()
	{
		return content.getMetas();
		
		
		
	}
	
	public boolean subsumesBasic(Formula other)
	{
		if (!(other instanceof SpeechAct))
			return false;
		
		return content.subsumesBasic(((SpeechAct)other).content);
	}
	
	public Formula getForce()
	{
		return content.get(new TTRLabel("act"));
	}
	
	public Formula getArgValue(String argVar)
	{
		TTRLabel l=new TTRLabel(argVar);
		return content.get(l);
	}
	
	public SpeechAct instantiate()
	{
		return new SpeechAct(content.instantiate());
	}
	
	public SpeechAct evaluate()
	{
		return new SpeechAct(content.evaluate());
	}
	
	public static void main(String[] args)
	{
		SpeechAct a=new SpeechAct("info(V,color:%colorvalue,shape:%shapevalue)");
		System.out.println("before substitution:"+a.content.toDebugString());
		SpeechAct subst=a.substitute(Formula.create("%colorvalue"), Formula.create("P1"));
		
		System.out.println(a);
		System.out.println(subst.content);
		//System.out.println(a.content.get(new TTRLabel("actor")));
		System.out.println(subst);

		SpeechAct subst2=subst.substitute(Formula.create("%shapevalue"), Formula.create("P2"));
		
		System.out.println(subst);
		System.out.println(subst2.content);
		//System.out.println(a.content.get(new TTRLabel("actor")));
		System.out.println(subst2);
		
		//System.out.println(sa);
		
		
	}

}
