package qmul.ds.ttrlattice;

import java.util.HashSet;
import java.util.Set;

import qmul.ds.formula.TTRRecordType;

public class TTRLatticeNode {

	TTRRecordType ttr = null;
	Set<TTRAustinianProp> props = null;
	boolean bottom = false;
	boolean top = false;
	
	public TTRLatticeNode(TTRRecordType ttr, Set<TTRAustinianProp> props) {
		this.ttr = ttr;
		this.props = props;
	}

	public Double getProbabilityMass(){
		/**
		 * The unnormalised probability mass of this particular node
		 */
		Double prob = 0.0;
		for (TTRAustinianProp austin : props){
			prob+=austin.prob;
		}
		return prob;
	}
	
	@Override
	public String toString() {
		if (bottom == true)
			return "bottom";
		String propstring = "empty_props";
		if (!props.isEmpty()&&!props.equals(null)){
			propstring = "";
			for (TTRAustinianProp prop : props){
				propstring+=String.valueOf(prop.recordNumber)+",";
			}
			
		}
			
		return "TTRLatticeNode [ttr= " + ttr + "\n\t, props= " + propstring + "]";
		//return "TTRLatticeNode [ttr=" + ttr + "]";
	}

	public TTRRecordType getTTR() {
		return ttr;
	}

	public void setTtr(TTRRecordType ttr) {
		this.ttr = ttr;
	}

	public Set<TTRAustinianProp> getProps() {
		return props;
	}

	public void setProps(Set<TTRAustinianProp> props) {
		this.props = props;
	}

	public TTRLatticeNode() {
		this.props = new HashSet<TTRAustinianProp>();
		this.ttr  = new TTRRecordType();
		// TODO Auto-generated constructor stub
	}
	
	public void setBottom(){
		this.bottom = true;
		this.ttr = null;
	}
	
	public void setTop(){
		this.top = true;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
