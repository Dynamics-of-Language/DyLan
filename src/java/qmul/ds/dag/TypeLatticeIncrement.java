package qmul.ds.dag;

import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.formula.ttr.TTRLabel;
import qmul.ds.formula.ttr.TTRRecordType;

public class TypeLatticeIncrement {

	private static Logger logger = Logger.getLogger(TypeLatticeIncrement.class);
	
	//this is for lattice traversal,... if an edge is positive we go forward, otherwise we go back	
	boolean positive=true;
	static TypeLatticeIncrement getNewEdge(TTRRecordType i, TTRLabel incrementOn, List<Long> idPool) {
		long newID = idPool.size() + 1;
		TypeLatticeIncrement result = new TypeLatticeIncrement(i, incrementOn, idPool.size() + 1);
		idPool.add(newID);
		return result;

	}

	static TypeLatticeIncrement getNewEdge(TypeLatticeIncrement edge, List<Long> idPool) {
		long newID = idPool.size() + 1;
		TypeLatticeIncrement result = new TypeLatticeIncrement(edge.getIncrement(), edge.incrementOn, idPool.size() + 1);
		idPool.add(newID);
		return result;
	}

	protected Long id = 0L;
	protected TTRRecordType increment;
	protected boolean seen = false;
	protected TTRLabel incrementOn;

	public TypeLatticeIncrement(TypeLatticeIncrement other)
	{
		this.id=other.id;
		this.increment=other.increment;
		this.seen=other.seen;
		this.incrementOn=other.incrementOn;
		this.positive=other.positive;
	}
	
	public TypeLatticeIncrement(TTRRecordType increment, TTRLabel incrementOn) {
		this.incrementOn = incrementOn;
		this.increment = increment;
	}

	public TypeLatticeIncrement(TTRRecordType i, TTRLabel incrementOn, long id) {
		this(i, incrementOn);
		this.id = id;
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
		if (!(o instanceof TypeLatticeIncrement))
			return false;
		TypeLatticeIncrement other = (TypeLatticeIncrement) o;
		return this.id.equals(other.id);
	}

	public int hashCode() {
		return id.hashCode();
	}

	public String toString() {
		return (positive?"+":"-")+increment.toString();
	}
	
	public boolean isPositive()
	{
		return positive;
	}

}
