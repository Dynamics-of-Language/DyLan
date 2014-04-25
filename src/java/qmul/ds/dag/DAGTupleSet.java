package qmul.ds.dag;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.ParserTuple;
import qmul.ds.tree.Tree;

@SuppressWarnings("serial")
public class DAGTupleSet extends ArrayList<ParserTuple> {
	private static Logger logger = Logger.getLogger(DAGTuple.class);

	private Long id = 0L;

	// private static ArrayList<Long> idPool=new ArrayList<Long>();

	public static DAGTupleSet getNewTupleSet(List<Long> idPool) {
		long newID = idPool.size() + 1;
		DAGTupleSet result = new DAGTupleSet(idPool.size() + 1);
		idPool.add(newID);
		return result;

	}

	public static DAGTupleSet getNewTupleSet(List<Long> idPool, Tree t) {
		long newID = idPool.size() + 1;
		DAGTupleSet result = new DAGTupleSet(idPool.size() + 1);
		idPool.add(newID);
		result.add(new ParserTuple(t));
		return result;

	}

	public DAGTupleSet(long id) {
		super();
		this.id = id;
	}

	public DAGTupleSet(ParserTuple start) {
		super();
		add(start);
	}

	public void add(Tree t) {
		add(new ParserTuple(t));
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof DAGTupleSet))
			return false;

		DAGTupleSet other = (DAGTupleSet) o;
		return id == other.id;// &&super.equals(o);
	}

	public int hashCode() {
		return id.hashCode();
	}

	public String toString() {
		return super.toString();

	}

}
