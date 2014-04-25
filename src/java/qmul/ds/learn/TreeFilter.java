package qmul.ds.learn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qmul.ds.formula.PredicateArgumentFormula;
import qmul.ds.formula.TTRField;
import qmul.ds.formula.TTRLabel;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.formula.Variable;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;

public class TreeFilter {
	
	static Map<NodeAddress, TTRField> nodeFieldMap=new HashMap<NodeAddress, TTRField>();
	
	Map<NodeAddress, TTRRecordType> minimalSubTypes=new HashMap<NodeAddress, TTRRecordType>();
	TTRRecordType rootType;
	public TreeFilter(TTRRecordType t)
	{
		init();
		rootType=t;
		for(TTRField f:rootType.getFields())
		{
			for(NodeAddress n:nodeFieldMap.keySet())
				
			{
				TTRField field=nodeFieldMap.get(n);
				if (f.subsumes(field))
				{
					TTRRecordType subtype=rootType.getMinimalIncrementWith(f, null);
					
					minimalSubTypes.put(n, subtype);
				}
			}
		
		}	
		//System.out.println("initialised minimal subtypes:"+minimalSubTypes);
	}
	public boolean matches(Tree t)
	{
		
		if (minimalSubTypes.isEmpty())
			return true;
		for(NodeAddress address:nodeFieldMap.keySet())
		{
			if(t.keySet().contains(address))
			{
				TTRRecordType argument=(TTRRecordType)t.get(address).getFormula();
				
				TTRLabel argumentHead=new TTRLabel((Variable)argument.head().getType());
				
				if (!minimalSubTypes.containsKey(address))
					return false;
				TTRLabel secondArg=getSecondArg(minimalSubTypes.get(address), nodeFieldMap.get(address));
				if (!minimalSubTypes.get(address).hasLabel(argumentHead)||!argumentHead.equals(secondArg))
				{
					return false;
				}
			}
			
		}
		
		return true;
	}
	
	private TTRLabel getSecondArg(TTRRecordType minimal, TTRField template) {
		for(TTRField f:minimal.getFields())
		{
			if (f.subsumes(template))
			{
				PredicateArgumentFormula paf=(PredicateArgumentFormula)f.getType();
				return new TTRLabel((Variable)paf.getArguments().get(1));
				
			}
		}
		return null;
	}
	public static void init()
	{
		nodeFieldMap.put(new NodeAddress("00"), TTRField.parse("p==subj(e,x):t"));
		nodeFieldMap.put(new NodeAddress("010"), TTRField.parse("p==obj(e,x):t"));
		nodeFieldMap.put(new NodeAddress("0110"), TTRField.parse("p==ind_obj(e1,e2):t"));
		
		
		
	}
	public List<Tree> filter(List<Tree> curTrees) {
		
		
		ArrayList<Tree> result=new ArrayList<Tree>();
		for(Tree t:curTrees)
			if(matches(t))
				result.add(t);
		
		return result;
	}
	
	

}
