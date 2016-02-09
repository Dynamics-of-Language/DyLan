package qmul.ds.action.meta;

public interface Meta<X> {
	
	
	public boolean backtrack();
	
	public void unbacktrack();
	
	public void reset();
	
	
	public void partialReset();
	
	public X getValue();
	
	//public void setValue(X value);

}
