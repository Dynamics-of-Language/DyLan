package qmul.ds.dag;

import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.ParserTuple;
import qmul.ds.learn.DAGInductionTuple;
import qmul.ds.tree.Tree;
/**
 * 
 * @author arash
 *
 */
public class DAGTuple extends ParserTuple {

	private static Logger logger = Logger.getLogger(DAGTuple.class);

	protected Long id = 0L;	
	private long depth=0;
	
	
	public static DAGTuple getNewTuple(List<Long> idPool) {

		long newID = idPool.size() + 1;
		DAGTuple result = new DAGTuple(idPool.size() + 1);
		idPool.add(newID);
		return result;
	}

	public static DAGInductionTuple getNewInductionTuple(List<Long> idPool) {

		long newID = idPool.size() + 1;
		DAGInductionTuple result = new DAGInductionTuple(idPool.size() + 1);
		idPool.add(newID);
		return result;
	}
	
	public static DAGTuple getNewTuple(List<Long> idPool, ParserTuple t) {
		long newID = idPool.size() + 1;
		DAGTuple result = new DAGTuple(t, idPool.size() + 1);
		idPool.add(newID);
		return result;
		
	}
	public static DAGInductionTuple getNewInductionTuple(List<Long> idPool, ParserTuple t) {
		long newID = idPool.size() + 1;
		DAGInductionTuple result = new DAGInductionTuple(t, idPool.size() + 1);
		idPool.add(newID);
		return result;
		
	}


	public static DAGTuple getNewTuple(List<Long> idPool, Tree t) {
		long newID = idPool.size() + 1;
		DAGTuple result = new DAGTuple(t, idPool.size() + 1);
		idPool.add(newID);
		return result;

	}
	
	public static DAGInductionTuple getNewInductionTuple(List<Long> idPool, Tree t) {
		long newID = idPool.size() + 1;
		DAGInductionTuple result = new DAGInductionTuple(t, idPool.size() + 1);
		idPool.add(newID);
		return result;

	}
	
	public void setDepth(long d)
	{
		depth=d;
	}
	public long getDepth()
	{
		return depth;
	}
	
	
	

	public DAGTuple(ParserTuple p, long id)
	{
		super(p.getTree());
		this.id=id;
	}
	public DAGTuple(Tree t, long id) {
		super(t);
		this.id = id;
	}

	public DAGTuple(Tree t) {
		super(t);
	}

	public DAGTuple(long id) {
		super();
		this.id = id;
	}

	public DAGTuple() {
		super();
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof DAGTuple))
			return false;

		DAGTuple other = (DAGTuple) o;
		return id == other.id;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id.hashCode();
		return result;
	}

	public String toString() {
		return "{" + this.id + " : " + super.toString() + "}";

	}

	
}
