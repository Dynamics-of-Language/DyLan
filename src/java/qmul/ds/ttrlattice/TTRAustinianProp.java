package qmul.ds.ttrlattice;

import qmul.ds.formula.TTRRecordType;
/**
 * Simple record class like Cooper et al 2014 with just a ttr record type and probability of that type
 * @author Julian
 *
 */
public class TTRAustinianProp {
	
	TTRRecordType ttr;
	Double prob;
	int recordNumber; //an individual id for a record indicating a situation/record/token

	public int getRecordNumber() {
		return recordNumber;
	}

	public void setRecordNumber(int recordNumber) {
		this.recordNumber = recordNumber;
	}

	public TTRRecordType getTtr() {
		return ttr;
	}

	public void setTtr(TTRRecordType ttr) {
		this.ttr = ttr;
	}

	public Double getProb() {
		return prob;
	}

	public void setProb(Double prob) {
		this.prob = prob;
	}

	public TTRAustinianProp(TTRRecordType ttr,Double prob,int id) {
		this.ttr = ttr;
		this.prob = prob;
		this.recordNumber = id;
	}
	
	public String toString(){
		return  this.recordNumber + "=" + this.ttr.toString() + " : " + this.prob.toString();
	}
	
	@Override
    public TTRAustinianProp clone() {
		//TTRAustinianProp clone = null;
        //try{
        //    clone = (TTRAustinianProp) super.clone();
        //   
        //}catch(CloneNotSupportedException e){
        //    throw new RuntimeException(e); // won't happen
       // }
       return new TTRAustinianProp(this.ttr,this.prob,this.recordNumber);
      //  return TTRAustinianPropclone;
       
    }




}
