package qmul.ds.dag;

import java.util.List;

import org.apache.log4j.Logger;

import qmul.ds.ParserTuple;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.tree.Tree;

public class TypeTuple {

	private static Logger logger = Logger.getLogger(TypeTuple.class);

	private Long id = 0L;

	TTRRecordType type;
	TTRRecordType incrementSoFar = new TTRRecordType();

	public static TypeTuple getNewTuple(TTRRecordType type, List<Long> idPool) {

		long newID = idPool.size() + 1;
		TypeTuple result = new TypeTuple(type, idPool.size() + 1);
		idPool.add(newID);
		return result;
	}

	public static TypeTuple getNewTuple(List<Long> idPool) {
		long newID = idPool.size() + 1;
		TypeTuple result = new TypeTuple(new TTRRecordType(), idPool.size() + 1);
		idPool.add(newID);
		return result;

	}

	public static TypeTuple getNewTuple(TypeTuple dest, List<Long> idPool) {
		long newID = idPool.size() + 1;
		TypeTuple result = new TypeTuple(dest.type, idPool.size() + 1);
		result.incrementSoFar = dest.incrementSoFar;
		idPool.add(newID);
		return result;

	}

	public TypeTuple(TTRRecordType t, long id) {
		super();
		this.type = t;
		this.id = id;
	}

	public TypeTuple() {
		super();
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof TypeTuple))
			return false;

		TypeTuple other = (TypeTuple) o;
		return id == other.id;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id.hashCode();
		return result;
	}

	public String toString() {
		return type.toString();

	}

	public TTRRecordType getType() {
		return type;
	}

	public TTRRecordType getIncrementSoFar() {
		return incrementSoFar;
	}

	public void setType(TTRRecordType from) {
		this.type = from;

	}

}
