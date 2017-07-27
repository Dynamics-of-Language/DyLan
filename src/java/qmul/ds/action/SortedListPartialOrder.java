package qmul.ds.action;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;

public class SortedListPartialOrder<T> extends LinkedList<T> {

	
	Comparator<T> comparator;
	
	public SortedListPartialOrder(Comparator<T> c)
	{
		this.comparator=c;
		
	}
	
	
	
	
	/**
	 * assumes list already partial-sorted
	 */
	
	public boolean add(T x)
	{
		
		for(int i=this.size()-1;i>=0;i--)
		{
			T cur=get(i);
			if (comparator.compare(x, cur)>0)
			{
				super.add(i+1, x);
				return true;
			}
			
			
			
		}
		
		super.add(0,x);
		return true;
	}
	
	public boolean addAll(Collection<? extends T> col)
	{
		for(T cur:col)
		{
			add(cur);
		}
		
		
		return true;
	}
	
	
	public static void main(String a[])
	{
		SortedListPartialOrder<Integer> list=new SortedListPartialOrder<Integer>(new IntComparator());
		
		list.add(5);
		list.add(1);
		list.add(7);
		list.add(2);
		list.add(0);
		
		System.out.println(list);
	}
	
}


class IntComparator implements Comparator<Integer>
{

	@Override
	public int compare(Integer i1, Integer i2) {
		
		if (i1>i2)
			return 1;
		if (i1<i2)
			return -1;
		
		return 0;
	}
	
	
}

