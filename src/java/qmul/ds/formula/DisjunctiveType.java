package qmul.ds.formula;

import java.util.HashMap;

public class DisjunctiveType extends TTRInfixExpression {

	public DisjunctiveType(Formula arg1, Formula arg2) {
		super(arg1, arg2);
		this.predicate=TTR_DISJUNTION_FUNCTOR;	
		
	}
	
	public boolean subsumesMapped(Formula other, HashMap<Variable, Variable> map)
	{
		if (other instanceof TTRRecordType)
		{
			HashMap<Variable, Variable> copy = new HashMap<Variable, Variable>(map);
			if (arg1.subsumesMapped(other, copy)) {
				map.clear();
				map.putAll(copy);
				return true;
			}

			return arg2.subsumesMapped(other, map);
		}
		return super.subsumesMapped(other, map);
	}
	
	

}
