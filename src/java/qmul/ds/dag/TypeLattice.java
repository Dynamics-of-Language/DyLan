package qmul.ds.dag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import qmul.ds.formula.TTRField;
import qmul.ds.formula.TTRLabel;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.formula.TTRRelativePath;
import qmul.ds.formula.Variable;
import qmul.ds.tree.NodeAddress;
import qmul.ds.tree.Tree;
import qmul.ds.type.DSType;
import edu.uci.ics.jung.graph.DelegateTree;

public class TypeLattice extends DelegateTree<TypeTuple, TypeLatticeIncrement> {

	/**
	 * Given a record type, and a ds type, creates a lattice
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(TypeLattice.class);

	// specifies the dsType of the head of all the types within this lattice.. one per lattice, e or t...
	// DSType dsType;
	TTRRecordType recType;
	private List<Long> idPoolNodes = new ArrayList<Long>();
	private List<Long> idPoolEdges = new ArrayList<Long>();
	private int depth = 0;
	private List<DSType> entityTypes = new ArrayList<DSType>();
	TypeTuple cur;
	// TTRRelativePath prefix;
	//Stack<TTRLabel> incLabels=new Stack<TTRLabel>();
	static List<TTRRecordType> priorityTemplates = new ArrayList<TTRRecordType>();

	public TypeLattice() {
		super();
		cur = TypeTuple.getNewTuple(idPoolNodes);
		addVertex(cur);
		initTemplates();
		// this.dsType=type;
	}

	public TypeLattice(TTRRecordType to) {
		this();
		this.recType = to;
		entityTypes.add(DSType.e);
		entityTypes.add(DSType.es);
		seenTypes = new HashSet<TTRRecordType>();
		this.priorityFields = getPriorityFields();
		initialise(to);

	}

	public TypeLattice(TTRRecordType from, TTRRecordType to) {
		this();
		this.recType = to;

		entityTypes.add(DSType.e);
		entityTypes.add(DSType.es);
		seenTypes = new HashSet<TTRRecordType>();
		this.priorityFields = getPriorityFields();
		cur.setType(from);
		cur.incrementSoFar = new TTRRecordType();

		populate(cur);

	}

	
	public void initTemplates() {
		priorityTemplates.add(TTRRecordType
				.parse("[e1:es|e2:es|x1:e|x2:e|p1==subj(e1,x1):t|p2==obj(e1,x2):t|p3==ind_obj(e1, e2):t]"));
		priorityTemplates.add(TTRRecordType.parse("[e1:es|x1:e|x2:e|p1==subj(e1,x1):t|p2==obj(e1,x2):t]"));
		priorityTemplates.add(TTRRecordType.parse("[e1:es|x1:e|p1==subj(e1,x1):t]"));
		initPriorityFields();

	}

	private List<TTRField> getEntityFields(TTRRecordType t) {
		ArrayList<TTRField> result = new ArrayList<TTRField>();
		for (TTRField f : t.getFields()) {
			if ((f.getDSType() != null && entityTypes.contains(f.getDSType())) || f.getDSType() == null) {
				// Suggested by IntelliJ: this is just equivalent to :
				// (f.getDSType() == null || entityTypes.contains(f.getDSType()))
				result.add(f);
			}

		}
		return result;
	}

	public void init() {
		seenTypes = new HashSet<TTRRecordType>();
		cur = getRoot();
		init(cur);
	}

	private void init(TypeTuple tt) {

		for (TypeLatticeIncrement edge : getOutEdges(tt)) {
			edge.setSeen(false);

			TypeTuple child = getDest(edge);
			init(child);

		}

	}

	// initialise the lattice to a set of starting points from the main clause
	// constrained to be a subtype of certain types... having specific predicates in them...
	// assumes head

	/**
	 * 
	 * @return true if successful, false if we're at root without any more exploration possibilities
	 */
	public boolean attemptBacktrack() {

		while (!moreUnseenEdges()) {
			if (isRoot(cur))
				return false;
			TypeLatticeIncrement backOver = goUpOnce();
			// System.out.println(backOver);
			backOver.setSeen(true);
		}
		logger.debug("Backtrack succeeded");

		return true;
	}

	/**
	 * 
	 * @return true if successful, false if we're at root without any more exploration possibilities
	 */
	public boolean attemptBacktrack(TTRLabel l) {

		while (!moreUnseenEdges(l)) {
			if (isRoot(cur))
				return false;
			TypeLatticeIncrement backOver = goUpOnce();
			// xSystem.out.println(backOver);
			backOver.setSeen(true);
		}
		logger.debug("Backtrack succeeded");

		return true;
	}

	public TTRRecordType nextIncrement() {
		TypeLatticeIncrement edge;
		do {
			edge = goFirst();
			if (edge != null)
				break;

		} while (attemptBacktrack());

		if (edge != null)
			return (TTRRecordType) cur.getIncrementSoFar();

		return null;
	}

	public void initialise(TTRRecordType rt) {
		logger.debug("initialising from");
		TTRLabel headLabel = rt.getHeadField().getLabel();
		TTRRecordType firstInc = rt.getMinimalIncrementWith(rt.getField(headLabel),headLabel);
		
		TypeLattice lattice = new TypeLattice(firstInc, rt);
		//this.incLabels.push(headLabel);
		logger.debug("initialised generic lattice with " + firstInc);
		for (TTRRecordType template : priorityTemplates) {
			logger.debug("looking for subtype of:" + template);
			lattice.init();
			TTRRecordType inc = lattice.nextIncrement(headLabel);
			TTRRecordType curSubtype = lattice.getCurSubtype();
			while (inc != null) {
				logger.debug("Testing " + template + " subsumes:\n" + curSubtype);
				if (template.subsumes(curSubtype)) {
					logger.debug("found subtupe of template:" + template);
					logger.debug("it was:" + curSubtype);
					// inc.deemHead(headLabel);
					// TTRRecordType subtype = (TTRRecordType) cur.getType().asymmetricMerge(inc);

					// TypeTuple child=this.addChild(cur, subtype, inc, headLabel);
					TypeTuple belowRoot = TypeTuple.getNewTuple(curSubtype, idPoolNodes);
					TypeLatticeIncrement edge = TypeLatticeIncrement.getNewEdge(curSubtype, headLabel, idPoolEdges);
					belowRoot.incrementSoFar = curSubtype;
					getRoot().incrementSoFar = new TTRRecordType();
					addEdge(edge, getRoot(), belowRoot);

					logger.debug("attachind sublattice:" + template);
					mergeLatticeAt(belowRoot, lattice.cur, lattice);
					return;

				}
				inc = lattice.nextIncrement(headLabel);
				curSubtype = lattice.getCurSubtype();

			}
		}
		// if we are here, none of the templates matched... just initialise this with lattice (minimal supertype with head)
		//TypeLattice orig = new TypeLattice(new TTRRecordType(), rt);
		TypeTuple belowRoot = TypeTuple.getNewTuple(lattice.getRoot(), idPoolNodes);
		firstInc.deemHead(headLabel);
		TypeLatticeIncrement edge = TypeLatticeIncrement.getNewEdge(firstInc, headLabel, idPoolEdges);
		belowRoot.incrementSoFar = firstInc;
		getRoot().incrementSoFar = new TTRRecordType();
		addEdge(edge, getRoot(), belowRoot);

		
		mergeLatticeAt(belowRoot, lattice.cur, lattice);
		
	}

	public TTRRecordType getCurSubtype() {
		return cur.getType();
	}

	private void populate(TypeTuple curTuple) {

		if (curTuple.getType().equalsIgnoreHeads(recType)) {
			return;
		}
		if (curTuple.getType().isEmpty()) {
			// initialisation
			for (TTRField f : recType.getFields()) {
				if (f.getDSType() != null && recType.hasDependent(f)) {
					TTRRecordType increment = recType.getSuperTypeWithParents(f);
					increment.deemHead(f.getLabel());
					TTRRecordType subtype = (TTRRecordType) increment.asymmetricMerge(curTuple.getType());

					populate(this.addChild(curTuple, subtype, increment, f.getLabel()));

				}
			}
			return;
		}
		entities: for (TTRField entityField : getEntityFields(curTuple.getType())) {
			if (!entityField.isHead() && (entityField.getDSType() == null || entityField.getDSType().equals(DSType.cn))) {
				logger.debug("getting cur restrictor for:" + entityField);
				TTRRecordType targetRestr = (TTRRecordType) recType.get(entityField.getLabel());
				TTRRecordType curRestr = (TTRRecordType) curTuple.getType().get(entityField.getLabel());

				
				TypeLattice lowerLattice = new TypeLattice(curRestr, targetRestr);

				mergeLatticeAt(curTuple, lowerLattice, entityField.getLabel());

			} else {

				for (TTRField prior : priorityFields) {
					// System.out.println("checking for priority field:"+prior);
					if (!curTuple.getType().hasField(prior) && prior.dependsOn(entityField)) {
						// System.out.println("adding priority child:"+prior);
						TTRRecordType increment = recType.getMinimalIncrementWith(prior, entityField.getLabel());
						logger.debug("minimal increment with "+prior+" on "+entityField.getLabel());
						logger.debug("is "+increment);
						// System.out.println("prior was:"+prior);
						// System.out.println("entity field was:"+entityField);
						// System.out.println("in increment:"+increment);
						increment.deemHead(entityField.getLabel());
						TTRRecordType subtype = (TTRRecordType) increment.asymmetricMerge(curTuple.getType());
						//if (!seenTypes.contains(subtype)) {
//							seenTypes.add(subtype);
//							populate(this.addChild(curTuple, subtype, increment, entityField.getLabel()));
						//}
						populate(this.addChild(curTuple, subtype, increment, entityField.getLabel()));
						continue entities;

					}

				}
				for (TTRField f : recType.getFields()) {
					if (f.getLabel().equals(TTRRecordType.HEAD))
						continue;

					if (!curTuple.getType().hasField(f) && f.dependsOn(entityField)) {
						logger.debug("adding field:"+f);
						
						TTRRecordType increment = recType.getMinimalIncrementWith(f,entityField.getLabel());
						
						increment.deemHead(entityField.getLabel());
						TTRRecordType subtype = (TTRRecordType) curTuple.getType().asymmetricMerge(increment);

						populate(this.addChild(curTuple, subtype, increment, entityField.getLabel()));

					}

				}
			}

		}

	}



	private TTRRecordType createEmbedding(TTRRelativePath prefix, TTRRecordType finalType, DSType type)
	{
		if (prefix.getLabels().size()==1)
		{
			TTRRecordType result=new TTRRecordType();
			result.add(new TTRField(prefix.getFirstLabel(), type, finalType));
		
			return result;
		}
		TTRRecordType result=new TTRRecordType();
		result.add(new TTRField(prefix.getFirstLabel(), type, createEmbedding(prefix.removeFirst(), finalType, type)));
		return result;
	}
	List<TTRField> priorityFields = new ArrayList<TTRField>();

	public void initPriorityFields() {
		priorityFields.add(TTRField.parse("p==subj(e,x):t"));
		priorityFields.add(TTRField.parse("p==obj(e,x):t"));
		priorityFields.add(TTRField.parse("p==ind_obj(e1,e2):t"));

	}

	public List<TTRField> getPriorityFields() {
		List<TTRField> result = new ArrayList<TTRField>();
		for (TTRField f : recType.getFields()) {
			for (TTRField template : priorityFields) {
				if (template.subsumes(f))
					result.add(f);

			}
		}

		return result;
	}

	private void go(TypeLatticeIncrement inc)
	{
		if (inc.positive&&getOutEdges().contains(inc)){
			cur=getDest(inc);
			return;
		}
		else if (!inc.positive&&getParentEdge().equals(inc))
		{
			cur=getParent(cur);
			return;
		}
		throw new IllegalStateException("cannot traverse the edge "+inc+" at current tuple="+cur);
		
		
	}
	private void backtrack(TypeLatticeIncrement inc)
	{
		if (!inc.positive&&getOutEdges().contains(inc)){
			cur=getDest(inc);
			return;
		}
		else if (inc.positive&&getParentEdge().equals(inc))
		{
			cur=getParent(cur);
			return;
		}
		throw new IllegalStateException("cannot traverse the edge "+inc+" at current tuple="+cur);
		
	}
	public Set<List<TypeLatticeIncrement>> getIncrements(TTRLabel l) {
		if(isRoot(cur))
			return getIncrements(cur, l);
		
		TypeTuple current=cur;
		seenTypes.clear();
		
		
		List<TypeLatticeIncrement> negativeIncs=new ArrayList<TypeLatticeIncrement>();
		while(!isRoot(current)){
			if (current.getType().hasLabel(l)&&!getParentEdge(current).increment.isEmpty())
			{
				
				Set<List<TypeLatticeIncrement>> result=addAtBegin(negativeIncs, getIncrements(current, l));
				return result;
			}
			
			TypeLatticeIncrement negative=new TypeLatticeIncrement(getParentEdge(current));
			negative.positive=false;
			negativeIncs.add(negative);
			current=getParent(current);			
		}
		return new HashSet<List<TypeLatticeIncrement>>();
		
			
		
		
	}
	private Set<List<TypeLatticeIncrement>> addAtBegin(List<TypeLatticeIncrement> list, Set<List<TypeLatticeIncrement>> set)
	{
		
		for(List<TypeLatticeIncrement> l:set)
		{
			l.addAll(0,list);
		}
		return set;
	}

	private Set<List<TypeLatticeIncrement>> getIncrements(TypeTuple cur, TTRLabel l) {
		
		if (getChildren(cur).isEmpty())
			return new HashSet<List<TypeLatticeIncrement>>();

		Set<List<TypeLatticeIncrement>> result = new HashSet<List<TypeLatticeIncrement>>();
		for (TypeLatticeIncrement edge1 : getOutEdges(cur)) {
			TypeLatticeIncrement edge=new TypeLatticeIncrement(edge1);
			if (!edge.incrementOn.equals(l))
			{
				continue;
			}
			if (seenTypes.contains(getDest(edge).getType()))
				continue;
			else
				seenTypes.add(getDest(edge).getType());
			
			Set<List<TypeLatticeIncrement>> childInc = edge.getIncrement().isEmpty() ? getHeadIncrements(getDest(edge))
					: getIncrements(getDest(edge), l);
			List<TypeLatticeIncrement> singleInc = new ArrayList<TypeLatticeIncrement>();
			singleInc.add(edge);
			if (!edge.getIncrement().isEmpty())// if not a transition edge, add it...
				result.add(singleInc);
			
			for (List<TypeLatticeIncrement> childList : childInc) {
				
				childList.add(0, edge);
				result.add(childList);
			}

		}
		return result;
	}


	private Set<List<TypeLatticeIncrement>> getHeadIncrements(TypeTuple current) {
		return getIncrements(current, current.getType().getHeadField().getLabel());
		
	}
	public Set<List<TypeLatticeIncrement>> getHeadIncrements() {
		
		return getIncrements(cur.getType().getHeadField().getLabel());
		
	}

	

	public boolean go(List<TypeLatticeIncrement> incs) {
		for (TypeLatticeIncrement inc : incs) {
			go(inc);
		}		
		return true;
	}

	public int getChildCount() {
		return getChildCount(cur);
	}

	public TypeTuple getParent() {
		return getParent(cur);
	}

	public TypeTuple addChild(TypeTuple curTuple, TTRRecordType rec, TTRRecordType increment, TTRLabel l) {
		logger.debug("Adding child:" + rec);
		logger.debug("inc:" + increment);
		logger.debug("On:"+l);

		TypeLatticeIncrement edge = TypeLatticeIncrement.getNewEdge(increment, l, idPoolEdges);
		// edge.localIncrement=localInc;
		TypeTuple target = TypeTuple.getNewTuple(rec, idPoolNodes);
		target.incrementSoFar = (TTRRecordType) curTuple.incrementSoFar.asymmetricMerge(increment);

		addEdge(edge, curTuple, target);
		return target;
	}

	// this is because this graph isn't really a lattice it's a tree... so we might have been on the same
	// rectype before
	Set<TTRRecordType> seenTypes = new HashSet<TTRRecordType>();

	public TypeLatticeIncrement goFirst() {
		if (getChildCount(cur) == 0)
			return null;

		Collection<TypeLatticeIncrement> edges = getOutEdges(cur);
		for (TypeLatticeIncrement e : edges) {

			if (!e.hasBeenSeen()) {

				TypeTuple child = getDest(e);
				if (seenTypes.contains(child.getType())) {
					e.setSeen(true);
					continue;
				}

				logger.debug("Going forward (first) along " + e.getIncrement());
				// child.incrementSoFar = (TTRRecordType) cur.incrementSoFar.asymmetricMerge(e.getIncrement());
				this.cur = child;
				seenTypes.add(child.getType());
				depth++;

				return e;
			}
		}
		return null;
	}

	public TypeLatticeIncrement goFirst(TTRLabel l) {
		if (getChildCount(cur) == 0)
			return null;

		Collection<TypeLatticeIncrement> edges = getOutEdges(cur);
		for (TypeLatticeIncrement e : edges) {

			if (!e.hasBeenSeen() && e.incrementOn.equals(l)) {

				TypeTuple child = getDest(e);
				if (seenTypes.contains(child.getType())) {
					e.setSeen(true);
					continue;
				}

				logger.debug("Going forward (first) along " + e.getIncrement());
				// child.incrementSoFar = (TTRRecordType) cur.incrementSoFar.asymmetricMerge(e.getIncrement());
				this.cur = child;
				seenTypes.add(child.getType());
				depth++;
				logger.debug("depth: " + depth);
				return e;
			}
		}
		return null;
	}

	public TypeLatticeIncrement getParentEdge() {
		return getParentEdge(cur);
	}

	/**
	 * 
	 * @return the edge we're going back over.
	 */
	public TypeLatticeIncrement goUpOnce() {
		if (isRoot(cur)) {
			logger.debug("At root, can't go up.");
			return null;
		}

		TypeLatticeIncrement result = getParentEdge();
		logger.debug("going back along: " + result.getIncrement());

		this.cur = getParent();
		logger.debug("Child Count" + getChildCount());
		depth--;

		return result;
	}

	public int getDepth() {
		return getDepth(cur);
	}

	public TypeTuple getCurrentTuple() {

		return cur;
	}

	public Collection<TypeLatticeIncrement> getOutEdges() {
		return getOutEdges(cur);
	}

	public boolean moreUnseenEdges() {
		for (TypeLatticeIncrement edge : this.getOutEdges()) {
			if (!edge.hasBeenSeen())
				return true;
		}
		return false;
	}

	public boolean moreUnseenEdges(TTRLabel l) {
		for (TypeLatticeIncrement edge : this.getOutEdges()) {
			if (edge.incrementOn.equals(l) && !edge.hasBeenSeen())
				return true;
		}
		return false;
	}

	
	public void setCurrentTuple(TypeTuple result) {
		this.cur = result;

	}

	public void removeChildren() {
		for (TypeTuple t : getChildren(cur)) {
			removeChild(t);
		}
	}
	public static void printIncs(Set<List<TypeLatticeIncrement>> list)
	{
		System.out.println("there are:"+list.size());
		for(List<TypeLatticeIncrement> inc:list)
		{
			System.out.println(inc);
			TTRRecordType flat=flatten(inc);
			System.out.println("flat:"+flat);
			System.out.println("trees:");
			List<Tree> abs=flat.getFilteredAbstractions(new NodeAddress("0"), DSType.t, true);
			for(Tree t:abs)
				System.out.println(t);
		}
		
	}
	private static TTRRecordType flatten(List<TypeLatticeIncrement> incs) {
		TTRRecordType result = new TTRRecordType();
		for (TypeLatticeIncrement inc : incs) {
			if (inc.isPositive())
				result = (TTRRecordType) inc.getIncrement().asymmetricMerge(result);
		}
		return result;
	}
	public static void main(String a[]) {




		TTRRecordType target = TTRRecordType
				.parse("[r:[x:e|p==glass(x):t|head==x:e]|x1==iota(r.head,r):e|head==x1:e]");

		System.out.println("target:"+target);
		TTRRecordType target2 = TTRRecordType
				.parse("[e==go:es|head==e:es|x==john:e|p==subject(e,x):t]");

		TypeLattice lattice=new TypeLattice(target);

		for(List<?> l: lattice.getIncrements(target.getHeadField().getLabel())) {
			System.out.println(l);
		}


;//		TTRRecordType inc=lattice.nextIncrement();
//		while(inc!=null){
//			
//			System.out.println(inc);
//			inc=lattice.nextIncrement();
//		}
	
	}

	public void mergeLatticeAt(TypeTuple node, TypeLattice lattice, TTRLabel l) {
		TypeLatticeIncrement transition = TypeLatticeIncrement.getNewEdge(new TTRRecordType(), l, idPoolEdges);
		TypeTuple root = TypeTuple.getNewTuple(lattice.root, idPoolNodes);
		addEdge(transition, node, root);
		mergeLatticeAt(root, lattice.getRoot(), lattice);
	}

	public void mergeLatticeAt(TypeTuple node, TypeLattice lattice) {

		mergeLatticeAt(root, lattice.getRoot(), lattice);
	}

	public void mergeLatticeAt(TypeTuple thisRoot, TypeTuple otherRoot, TypeLattice lattice) {

		for (TypeLatticeIncrement edge : lattice.getOutEdges(otherRoot)) {

			TypeLatticeIncrement edgeCopy = TypeLatticeIncrement.getNewEdge(edge, this.idPoolEdges);

			TypeTuple childCopy = TypeTuple.getNewTuple(lattice.getDest(edge), this.idPoolNodes);
			childCopy.incrementSoFar = (TTRRecordType) thisRoot.incrementSoFar.asymmetricMerge(edgeCopy.increment);
			addEdge(edgeCopy, thisRoot, childCopy);

			mergeLatticeAt(childCopy, lattice.getDest(edge), lattice);

		}

	}

	public TTRRecordType getIncrementSoFar() {

		return cur.incrementSoFar;
	}

	public TTRRecordType nextIncrement(TTRLabel label) {
		TypeLatticeIncrement edge;
		do {
			edge = goFirst(label);
			if (edge != null)
				break;

		} while (attemptBacktrack(label));

		if (edge != null)
			return cur.getIncrementSoFar();

		return null;
	}

	public void backtrack(List<TypeLatticeIncrement> increments) {
		//System.out.println("backtracking lattice over:"+increments);
		//System.out.println("lattice current:"+cur.getType());
		
		for (int i=increments.size()-1; i>-1;i--) {
			TypeLatticeIncrement inc=increments.get(i);
			backtrack(inc);

		}
		
	}

	
	public TypeLatticeIncrement getFirstIncrement() {
		cur = getRoot();
		if (getChildCount() != 1)
		{
			logger.fatal("more than one child to root:");
			System.out.println(getChildren(cur));
			throw new IllegalStateException();
		}

		TypeLatticeIncrement firstInc = getOutEdges().iterator().next();
		return firstInc;
	}
	public Set<List<TypeLatticeIncrement>> getFirstIncrements() {
		cur = getRoot();
		if (getChildCount() != 1)
		{
			logger.fatal("more than one child to root:");
			System.out.println(getChildren(cur));
			throw new IllegalStateException();
		}

		cur=getDest(getOutEdges().iterator().next());
		return getHeadIncrements();
		
	}

	List<TypeLatticeIncrement> lastInc=null;
	public void backTrackLast() {
		backtrack(lastInc);
		
	}
	TTRLabel curIncrementOn;
	private TTRLabel getLastIncLabel()
	{
		TypeTuple current=cur;
		TypeLatticeIncrement parent=getParentEdge(current);
		while(parent!=null)
		{
			 if(!parent.increment.isEmpty()&&parent.incrementOn!=getParentEdge(cur).incrementOn)
				 return getParentEdge(current).incrementOn;
				 
			current=getParent(current);
		}
		return null;
	}

}
