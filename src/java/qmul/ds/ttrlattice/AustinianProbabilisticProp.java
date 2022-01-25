package qmul.ds.ttrlattice;

import qmul.ds.formula.ttr.TTRRecordType;
/**
 * Simple record class like Cooper et al 2014 with just a ttr record type and probability of that type
 * @author Julian
 *
 */
public class AustinianProbabilisticProp extends AustinianProp{
	
	
	Double prob;
	

	
	

	
	public Double getProb() {
		return prob;
	}

	public void setProb(Double prob) {
		this.prob = prob;
	}

	public AustinianProbabilisticProp(TTRRecordType ttr,Double prob,int id) {
		super(ttr);
		recordNumber=id;
		this.prob=prob;
	}
	
	
	
	public String toString(){
		return  this.recordNumber + "=" + this.rt.toString() + " : " + this.prob.toString();
	}
	
	@Override
    public AustinianProbabilisticProp clone() {
		
       return new AustinianProbabilisticProp(this.rt,this.prob,this.recordNumber);
     
       
    }




}
