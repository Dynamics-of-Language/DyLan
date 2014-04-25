package qmul.ds.learn;

import java.util.List;

import qmul.ds.ParserTuple;
import qmul.ds.action.Action;
import qmul.ds.dag.TypeLatticeIncrement;
import qmul.ds.dag.TypeTuple;
import qmul.ds.tree.Tree;

public class TreeHypothesis extends Action{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	List<TypeLatticeIncrement> increments;
	TypeTuple last;
	Tree tree=null;
	
	public TreeHypothesis(List<TypeLatticeIncrement> increments, Tree t)
	{
		super("tree-hyp");
		this.tree=t;
		this.increments=increments;		
	}
	
		
	public <T extends Tree> T exec(T tree, ParserTuple context) {
		return tree;
	}

	public Tree getTree()
	{
		return this.tree;
	}

	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		logger.debug("using this");
		if (this == o)
			return true;
		if (o == null)
			return false;
		else if (this.getClass() != o.getClass())
			return false;
		else {
			// by default (and for computational actions), name is unique
			TreeHypothesis a = (TreeHypothesis) o;
			return a.tree.equals(tree);
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tree == null) ? 0 : tree.hashCode());
		return result;
	}
	
	public String toString()
	{
		String s=this.name+":"+this.tree;
		return s;
		
	}

}
