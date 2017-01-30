package qmul.ds.dag;

import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.formula.TTRLabel;
import qmul.ds.formula.TTRRecordType;

public class GenericLatticeEdge {

	private static Logger logger = Logger.getLogger(GenericLatticeEdge.class);
	protected Long id = 0L;
	protected TTRRecordType increment;
	protected boolean seen = false;
	
	
	static GenericLatticeEdge getNewEdge(TTRRecordType i, List<Long> idPool) {
		
		long newID = idPool.size() + 1;
		GenericLatticeEdge result = new GenericLatticeEdge(i, newID);
		
		idPool.add(newID);
		return result;

	}
	
	static GenericLatticeEdge getNewEdge(List<Long> idPool) {
		return getNewEdge(null, idPool);

	}
	

	

	public GenericLatticeEdge(GenericLatticeEdge other)
	{
		this.id=other.id;
		this.increment=other.increment;
		this.seen=other.seen;
	}
	
	public GenericLatticeEdge(TTRRecordType increment, long id) {
		this.id=id;
		this.increment = increment;
	}

	public GenericLatticeEdge(long id) {
		this(null, id);
	}

	public void setID(long id) {
		this.id = id;
	}

	public TTRRecordType getIncrement() {
		return this.increment;
	}

	public boolean hasBeenSeen() {
		return seen;
	}

	public void setSeen(boolean b) {
		seen = b;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof GenericLatticeEdge))
			return false;
		GenericLatticeEdge other = (GenericLatticeEdge) o;
		return this.id.equals(other.id);
	}

	public int hashCode() {
		return id.hashCode();
	}

	public String toString() {
		return id+":"+increment;
	}





	
	
	

}
