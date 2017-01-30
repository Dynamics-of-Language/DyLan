package qmul.ds.dag;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import qmul.ds.InteractiveContextParser;
import qmul.ds.Utterance;
import qmul.ds.formula.TTRField;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.formula.Variable;
import qmul.ds.type.DSType;

public class GenericTypeLattice extends DirectedSparseMultigraph<TypeTuple, GenericLatticeEdge> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected Logger logger=Logger.getLogger(GenericTypeLattice.class);
	
	TTRRecordType recType;
	
	private List<Long> idPoolNodes = new ArrayList<Long>();
	private List<Long> idPoolEdges = new ArrayList<Long>();
	private int depth = 0;
	private TypeTuple cur;
	private TypeTuple top;
	private TypeTuple bottom;
	
	private List<DSType> entityTypes = new ArrayList<DSType>();//these behave differently in (under)specification (type set to null
	//rather than removal... or type specified first.. rather than just addition.
	
	public GenericTypeLattice() {
		super();
		cur = TypeTuple.getNewTuple(idPoolNodes);
		top=cur;
		addVertex(top);
		entityTypes.add(DSType.e);
		entityTypes.add(DSType.es);
		
	}
	
	public GenericTypeLattice(TTRRecordType rec)
	{
		this();
		this.recType=rec;
		
		populate(cur);
	}
	
	/** populate the lattice below cur (all subtypes of cur)
	 * Assumes nothing below cur already.... 
	 * 
	 * @param cur
	 * @param alreadyOnNextLevel
	 */
	
	public void populate(TypeTuple cur)
	{
		
		List<TypeTuple> nextLevel=populateNextLevel(cur, new ArrayList<TypeTuple>());
		while(!nextLevel.isEmpty())
		{
			List<TypeTuple> added=new ArrayList<TypeTuple>();
			for(TypeTuple tuple: nextLevel)
			{
				added.addAll(populateNextLevel(tuple, added));
			}
			nextLevel=added;
			if (!nextLevel.isEmpty())
			{
				bottom=nextLevel.get(nextLevel.size()-1);
			}
		}
	}
	
	
	/**
	 * populate (add edges and nodes) the lattice level below cur
	 * 
	 * returns the added children
	 * 
	 * @param parent
	 */
	
	public List<TypeTuple> populateNextLevel(TypeTuple parent, List<TypeTuple> alreadyOnNextLevel)
	{
		TTRRecordType curRT=parent.getType();
		List<TypeTuple> nextLevelTuples=new ArrayList<TypeTuple>();
		outer:
		for(TTRField f: recType.getFields())
		{
			if (curRT.hasField(f))
				continue;
			
			for(Variable v: f.getVariables())
			{
				if (!curRT.hasLabel(v))
					continue outer;
			}
			
			
			TTRRecordType newRT=new TTRRecordType(curRT);
			//if we are here, all dependencies of f are satisfied.
			//is it an entity type? if so:
			if (this.entityTypes.contains(f.getDSType()))
			{
				if (f.getType()==null)
					newRT.add(new TTRField(f));
				else {
					TTRField nulledType=new TTRField(f);
					nulledType.setType(null);
					if (curRT.hasField(nulledType))
					{
						newRT.getField(f.getLabel()).setType(f.getType());
					}
					else
						newRT.add(nulledType);
					
				}
			}
			else
			{
				//if not entity type just add it
				newRT.add(new TTRField(f));
			}
			
			//now add it to curRT
			
			
			TypeTuple child=TypeTuple.getNewTuple(newRT, idPoolNodes);
			
			GenericLatticeEdge edge=GenericLatticeEdge.getNewEdge(idPoolEdges);
			
			TypeTuple nextLevelTuple=findTuple(child, alreadyOnNextLevel);
			if (nextLevelTuple==null)
			{
				//child isn't already there
				addEdge(edge, parent, child);
				nextLevelTuples.add(child);
				logger.debug("Added edge from:"+parent);
				logger.debug("to:"+child);
				
			}
			else
			{
				this.addEdge(edge, parent, nextLevelTuple);
				logger.debug("Added edge from:"+parent);
				logger.debug("to existing child:"+nextLevelTuple);
			}
			
			
		}
		return nextLevelTuples;
			
	}
	/**
	 * works not by equality, but by checking two way (basic) subsumption
	 * @param tuple
	 * @param list
	 * @return the matched tuple in the list
	 */
	private TypeTuple findTuple(TypeTuple tuple, List<TypeTuple> list)
	{
		for(TypeTuple t: list)
		{
			if (t.twoWaySubsumes(tuple))
				return t;
		}
		return null;
		
		
	}
	
	public TypeTuple getTop()
	{
		return top;
	}
	
	public TypeTuple getBottom()
	{
		return bottom;
	}
	
	public TTRRecordType maximallySpecificCommonSuperType(TTRRecordType r, HashMap<Variable, Variable> map)
	{
		if (bottom.getType().subsumesMapped(r, map))
			return bottom.getType();
		//traverse bottom to top, in order (breadth first)
		TypeTuple cur=bottom;
		
		Set<TypeTuple> upperLevel=new HashSet<TypeTuple>(this.getPredecessors(cur));
		while(!upperLevel.isEmpty())
		{
			for(TypeTuple upperTuple:upperLevel)
			{
				if (upperTuple.getType().subsumesMapped(r, map))
					return upperTuple.getType();
				
				map.clear();
			}
			
			Set<TypeTuple> prevLevel=new HashSet<TypeTuple>(upperLevel);
			upperLevel.clear();
			for(TypeTuple prevTuple:prevLevel)
			{
				upperLevel.addAll(getPredecessors(prevTuple));
			}
		}
		
		return new TTRRecordType();
	}
	public static void main(String[] a)
	{
		
		InteractiveContextParser parser=new InteractiveContextParser("resource/2016-english-ttr-restaurant-search");
		
		Utterance utt=new Utterance("sys: what can I help you with today?");
		parser.parseUtterance(utt);
		
		TTRRecordType rt=(TTRRecordType)parser.getContext().getCurrentTuple().getSemantics(parser.getContext());
		
		long before=new Date().getTime();
		GenericTypeLattice lattice= new GenericTypeLattice(rt);
		long after=new Date().getTime();
		
		System.out.println("Lattice construction took "+(after-before)+" milliseconds");
		TTRRecordType rt2=TTRRecordType.parse("[e2 : es|x5 : e|x1 : e|p5==question(x1) : t|p2==modal(e2) : t|p3==subj(e2, x5) : t]");
		System.out.println("Bottom:"+lattice.getBottom());
		HashMap<Variable,Variable> map=new HashMap<Variable,Variable>();
		TTRRecordType mcs=lattice.maximallySpecificCommonSuperType(rt2, map);
		
		System.out.println("mcs: "+mcs);
		System.out.println("map: "+map);
	}
		
		
	

}
