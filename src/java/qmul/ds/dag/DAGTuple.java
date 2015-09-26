package qmul.ds.dag;

import java.util.List;

import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;

import qmul.ds.ParserTuple;
import qmul.ds.learn.DAGInductionTuple;
import qmul.ds.tree.Node;
import qmul.ds.tree.Tree;
import qmul.ds.tree.label.Label;
import qmul.ds.tree.label.Requirement;
import qmul.ds.tree.label.TypeLabel;

/**
 * 
 * @author arash
 *
 */
public class DAGTuple extends ParserTuple {

	private static Logger logger = Logger.getLogger(DAGTuple.class);

	protected Long id = 0L;
	private long depth = 0;

	/*
	 * public static Transformer<DAGTuple, String> getVertexLabelTransformer() {
	 * return new Transformer<DAGTuple, String>() { public String
	 * transform(DAGTuple ts) { // return "type";
	 * 
	 * return ""; } }; }
	 */
	public void setDepth(long d) {
		depth = d;
	}

	public long getDepth() {
		return depth;
	}

	public DAGTuple(ParserTuple p, long id) {
		super(p.getTree());
		this.id = id;
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
	
	public String transform() {
		
		return id+"";
	}

}
