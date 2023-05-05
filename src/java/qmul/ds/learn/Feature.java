package qmul.ds.learn;

import qmul.ds.formula.TTRRecordType;
import qmul.ds.type.DSType;

import java.util.TreeSet;

// todo fix hashcode
// all of the above for `isRequirement`

public class Feature implements Comparable<Feature>{
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

        throw new NullPointerException("RT and DSType are both null!");
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

    @Override
    public int compareTo(Feature o) {
        // Attention: The feature comperator, reverses the natural ordering of TTRRecordType comparator for use in our learner probTable.
        if (this.equals(o))
            return 0;
        else if (this.rt != null && o.rt != null)
            return -this.rt.compareTo(o.rt);
        else if (this.dsType != null && o.dsType != null) // TODO not sure fo this is correct.
//            return 1;
//            return this.dsType.compareTo(o.dsType);
            return this.toString().compareTo(o.toString()); // AA: I commented out the line above and used the same trick AE used in TTRRecordType compareTo.
        else if (rt != null)
            return 1;
        else if (dsType != null)
            return -1;
        else
            throw new NullPointerException("Either this feature or the other feature are null.");

//        if (this.rt != null)
//            return -1;
//            return 1;
//        else
//            return -1;

 //from here on this Feature object is invalid
        //returning arbitrary order
//        return -1;
    }


//    public int compareTo(Feature o) {
//        if (rt!=null && o.rt!=null)
//            return rt.compareTo(o.rt);
//
//        return toString().compareTo(o.toString());
//    }

    public static void main(String[] args) {
        TreeSet<Feature> feature = new TreeSet<Feature>();
        feature.add(new Feature(DSType.e));
        feature.add(new Feature(DSType.t));
        feature.add(new Feature(DSType.et));
        feature.add(new Feature(DSType.t, true));

        feature.add(new Feature(TTRRecordType.parse("[e : es]")));
        feature.add(new Feature(TTRRecordType.parse("[x==john : e]")));
        feature.add(new Feature(TTRRecordType.parse("[e15==go : es]")));
        feature.add(new Feature(TTRRecordType.parse("[e : es|p==go(e) : t]")));

        System.out.println(feature);
    }
}
