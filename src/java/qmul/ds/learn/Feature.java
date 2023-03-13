package qmul.ds.learn;

import qmul.ds.formula.TTRRecordType;
import qmul.ds.type.DSType;

// todo fix hashcode
// all of the above for `isRequirement`

public class Feature {
    public TTRRecordType rt = null;
    public DSType dsType = null;
    public boolean isRequirement = false;


    public Feature(TTRRecordType t) {
        this.rt = t;
    }

    public Feature(DSType n) {
        this(n, false);
    }

    public Feature(DSType n, boolean b){  // to see if we have a requirement type or not
        this.dsType = n;
        this.isRequirement = b;
        // getRequiredType -> Node
        // call this
    }

    @Override
    public int hashCode(){ // because we're using a hashtable
        if(rt != null)
            return rt.hashCode();
        else if (dsType != null){
            return toString().hashCode(); // what is this?
        }
        else
            return 0;
    }

    public boolean equals(Object o){ // todo fix
        if(!(o instanceof Feature))
            return false;
        Feature other = (Feature) o;

        if(rt != null)
            return rt.equals(other.rt);

        if(dsType != null)
            return dsType.equals(other.dsType) && (isRequirement == other.isRequirement);

        if(other.rt != null || other.dsType != null)
            return false;

        return true;
    }

    public String toString(){
        if(rt != null)
            return rt.toString();

        if(dsType != null) {
            if (isRequirement)
                return "?" + dsType.toString();
            return dsType.toString();
        }
        throw new NullPointerException();
    }

}
