package qmul.ds.formula;

import java.util.HashMap;

public class DisjunctiveType extends TTRInfixExpression {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7419851471581483609L;

	public DisjunctiveType(Formula arg1, Formula arg2) {
		super(arg1, arg2);
		this.predicate=TTR_DISJUNTION_FUNCTOR;	
		
	}
	
	public boolean subsumesMapped(Formula other, HashMap<Variable, Variable> map)
	{
		if (other instanceof TTRRecordType)
		{
			HashMap<Variable, Variable> copy = new HashMap<Variable, Variable>(map);
			if (arg1.subsumesMapped(other, map))
				return true;
			
			map.clear();
			map.putAll(copy);
			if (arg2.subsumesMapped(other, map))
				return true;
			
			map.clear();
			map.putAll(copy);
			return false;
			
		}
		else if (other instanceof DisjunctiveType)
		{
			throw new UnsupportedOperationException();
		}
		
		return super.subsumesMapped(other, map);
	}
	
	public DisjunctiveType removeHead()
	{
		if (!(arg1 instanceof TTRFormula && arg2 instanceof TTRFormula))
			throw new UnsupportedOperationException();
		
		TTRFormula arg1=(TTRFormula)this.arg1;
		TTRFormula arg2=(TTRFormula)this.arg2;
		
		return new DisjunctiveType(arg1.removeHead(), arg2.removeHead());
		
	}
	
	public DisjunctiveType sortFieldsBySpecificity()
	{
		if (!(this.arg1 instanceof TTRRecordType && this.arg2 instanceof TTRRecordType))
			throw new UnsupportedOperationException();
		
		TTRRecordType left=(TTRRecordType)arg1;
		TTRRecordType right=(TTRRecordType)arg2;
		
		return new DisjunctiveType(left.sortFieldsBySpecificity(), right.sortFieldsBySpecificity());
		
	}
	
	public TTRRecordType evaluate()
	{
		if (!(this.arg1 instanceof TTRRecordType && this.arg2 instanceof TTRRecordType))
			throw new UnsupportedOperationException();
		
		TTRRecordType left=(TTRRecordType)arg1;
		TTRRecordType right=(TTRRecordType)arg2;
		
		return left.mostSpecificCommonSuperType(right, new HashMap<Variable, Variable>());
		
	}

}
