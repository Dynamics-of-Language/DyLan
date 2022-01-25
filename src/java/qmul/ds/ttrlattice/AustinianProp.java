package qmul.ds.ttrlattice;

import java.util.ArrayList;

import javax.print.attribute.standard.MediaSize.Other;

import qmul.ds.formula.ttr.TTRRecordType;

/**
 * Simple record class like Cooper et al 2014 with just a ttr record type
 * 
 * (there is a probabilistic subclass of this too: AustinianProbabilisticProp)
 * 
 * AustinianProps are comparable. Right now, it's just time of creation, hence their ids that are compared.
 * Subclasses could order them differently, e.g. by probability.
 * 
 * @author arash
 *
 */
public class AustinianProp implements Comparable<AustinianProp> {

	TTRRecordType rt;
	long id = 1;// the id number of this proposition
	int recordNumber; // an individual id for a record indicating a
						// situation/record/token

	public AustinianProp(TTRRecordType ttr) {
		this.rt = ttr;
		this.recordNumber = -1;
	}

	public AustinianProp(TTRRecordType ttr, long id) {
		this.rt = ttr;
		this.recordNumber = -1;
		this.id = id;
	}

	public AustinianProp getFreshProp(TTRRecordType ttr, ArrayList<Long> pool) {
		long newID = pool.size() + 1;
		AustinianProp prop = new AustinianProp(ttr, newID);
		pool.add(newID);
		return prop;
	}

	public int getRecordNumber() {
		return recordNumber;
	}

	public void setRecordNumber(int recordNumber) {
		this.recordNumber = recordNumber;
	}

	public TTRRecordType getTtr() {
		return rt;
	}

	public void setTtr(TTRRecordType ttr) {
		this.rt = ttr;
	}

	@Override
	public AustinianProp clone() {

		return new AustinianProp(this.rt);

	}

	/**
	 * warning: equality is by id. So is hashcode.
	 */
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof AustinianProp))
			return false;

		return ((AustinianProp) o).id == id;
	}

	public int hashCode() {
		return new Long(id).hashCode();
	}

	@Override
	public int compareTo(AustinianProp o) {
		if (this.id > o.id)
			return -1;
		else if (o.id > this.id)
			return +1;
		else
			return 0;
	}

}
