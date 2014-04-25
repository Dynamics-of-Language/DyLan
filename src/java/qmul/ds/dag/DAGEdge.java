package qmul.ds.dag;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.action.Action;

public class DAGEdge implements Comparable<DAGEdge> {

	private static Logger logger = Logger.getLogger(DAGEdge.class);

	public static DAGEdge getNewEdge(List<Long> idPool, Action a, String w) {
		long newID = idPool.size() + 1;
		DAGEdge result = new DAGEdge(a, w, idPool.size() + 1);
		idPool.add(newID);
		return result;

	}

	public static DAGEdge getNewEdge(List<Long> idPool, Action a) {
		long newID = idPool.size() + 1;
		DAGEdge result = new DAGEdge(a, null, idPool.size() + 1);
		idPool.add(newID);
		return result;
	}

	protected Long id = 0L;
	protected Action action;
	protected String word;
	protected boolean seen = false;
	protected double weight = 0.5;

	public DAGEdge(Action a, String w) {
		this.word = w;
		this.action = a;
	}

	public DAGEdge(Action a, String w, long id) {
		this(a, w);
		this.id = id;
	}

	public DAGEdge(Action a) {
		action = a;
		word = null;
	}

	public void setID(long id) {
		this.id = id;
	}

	public Action getAction() {
		return this.action;
	}

	public boolean hasBeenSeen() {
		return seen;
	}

	public String word() {
		return this.word;
	}

	public void setSeen(boolean b) {
		seen = b;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof DAGEdge))
			return false;
		DAGEdge other = (DAGEdge) o;
		return this.id.equals(other.id);
	}

	public int hashCode() {
		
		return id.hashCode();
	}

	public void setWeight(double w) {
		this.weight = w;
	}

	@Override
	public int compareTo(DAGEdge other) {
		if (this.weight > other.weight)
			return 1;
		else if (this.weight < other.weight)
			return -1;

		return this.hashCode() - other.hashCode();
	}

	public String toString() {
		return "[" + action.getName() + ":" + word + "]";
	}

}
