package qmul.ds.ttrlattice;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.util.Pair;
import ptolemy.graph.DirectedAcyclicGraph;
import ptolemy.graph.Edge;
import ptolemy.graph.Node;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.formula.Variable;
import qmul.ds.learn.RecordTypeCorpus;

public class TTRLattice extends DirectedAcyclicGraph {

	protected static Logger logger = Logger.getLogger(TTRLattice.class);
	
	public static void pause() {
		System.out.println("Press enter to continue...");
		try {
			System.in.read();
		} catch (Exception e) {
		}

	}

	public TTRLattice() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Node nodeFromTTRRecordType(TTRRecordType ttr) {
		for (Object i : this.nodes()) {
			//System.out.println(((TTRLatticeNode) ((Node) i).getWeight()).getTTR());
			if (((TTRLatticeNode) ((Node) i).getWeight()).getTTR() == null) {
				continue;
			}

			if (((TTRLatticeNode) ((Node) i).getWeight()).ttr.equals(ttr)) {
				return (Node) i;
			}
		}
		return null;
	}

	public boolean containsTTRRecordType(TTRRecordType ttr) {
		for (Object i : this.nodes()) {
			// System.out.println(((TTRLatticeNode)i).ttr);
			if (((TTRLatticeNode) ((Node) i).getWeight()).ttr == null) {
				continue;
			}

			if (((TTRLatticeNode) ((Node) i).getWeight()).ttr.equals(ttr)) {
				return true;
			}
		}
		return false;
	}
	
	public Collection<Node> getParents(Node node){
		Collection<Node> parents = this.successors(node); 
		parents.retainAll(this.neighbors(node));//gets immediate parents
		return parents;
	}


	/**
	 * Uses the node IDs to remove edges, rather than edge IDs
	 * @param node1
	 * @param node2
	 */
	public void removeEdge(Node node1, Node node2){
		logger.debug("removing Edge from child " + String.valueOf(this.nodeLabel(node1)) + " to parent " + String.valueOf(this.nodeLabel(node2)));
		Object[] myedges = this.outputEdges(node1).toArray(); //TODO need a copy TODO make this a getEdge function
		Collection<Object> edges = new HashSet<Object>(Arrays.asList(myedges));
		edges.retainAll(this.incidentEdges(node2)); //TODO deep copy?? check
		this.removeEdge((Edge) edges.toArray()[0]); //disconnect
	}
	
	public Collection<Node> upperBound(Node node1, Node node2){
		Collection<Node> mynodes = this.reachableNodes(node1);
		mynodes.retainAll(this.reachableNodes(node2));
		return mynodes;
	}
	
	public Collection<Node> lowerBound(Node node1, Node node2){
		Collection<Node> mynodes = this.backwardReachableNodes(node1);
		mynodes.retainAll(this.backwardReachableNodes(node2));
		return mynodes;
	}
	
	/**
	 * Simply checks the lower bound of node1 and looks for edges to or from node 2. is a lowest common ancestor algorithm
	 * @param node1
	 * @param node2
	 * @return
	 */
	public Node meetNode(Node node1, Node node2){
		if (node1.equals(node2)){
			return node1;
		}
		if (edgeExists(node1,node2)){
			return node1;
		}
		if (edgeExists(node2,node1)){
			return node2;
		}
		
		Collection<Node> lowerBound = this.backwardReachableNodes(node1); //relies on this being ordered for max efficiency
		for (Node child : lowerBound){
			//System.out.println(child);
			if (child.equals(this.node(this.bottom()))){
				continue;
			}
			if (child.equals(node2)){
				return node2;
			}
			
			if ((this.edgeExists(child, node2))){ //we've found it
				return child;
			}
			
			if ((this.edgeExists(node2, child))){ //we've found it
				return node2;
			}
			
		}
		
		return this.node(this.bottom());
	}
	
	public Double probability(TTRRecordType ttr){
		return ((TTRLatticeNode) this.nodeFromTTRRecordType(ttr).getWeight()).getProbabilityMass()/
				((TTRLatticeNode) this.top()).getProbabilityMass();
	}
	
	public Double meetProbability(TTRRecordType ttr, TTRRecordType ttr1){
		Node meetNode = this.meetNode(this.nodeFromTTRRecordType(ttr), this.nodeFromTTRRecordType(ttr1));
		System.out.println(meetNode);
		return ((TTRLatticeNode) meetNode.getWeight()).getProbabilityMass()/
				((TTRLatticeNode) this.top()).getProbabilityMass();
	}
	
	public Double joinProbability(TTRRecordType ttr, TTRRecordType ttr1){
		return ((TTRLatticeNode) this.leastUpperBound(this.nodeFromTTRRecordType(ttr), this.nodeFromTTRRecordType(ttr1))).getProbabilityMass()/
				((TTRLatticeNode) this.top()).getProbabilityMass();
	}
	
	public Double conditionalProbability(TTRRecordType condition, TTRRecordType result){
		Node conditionNode = this.nodeFromTTRRecordType(condition);
		Node resultNode = this.nodeFromTTRRecordType(result);
		
		if (conditionNode==null){
			logger.debug("No condition node");
			this.addTypeJudgement(condition, new HashSet<TTRAustinianProp>(), this.node(this.bottom()), false); //have to find its place..
			conditionNode = this.nodeFromTTRRecordType(condition);
			for (Object node : this.predecessors(conditionNode)){
				this.addAustinianJudgements(conditionNode, ((TTRLatticeNode) ((Node) node).getWeight()).getProps(), true);
			}

		}
		if (this.nodeFromTTRRecordType(result)==null){
			logger.debug("No result node");
			if (result.subsumes(condition)){
				logger.debug("No resulting node, but supertype of " + condition.toString());
				return 1.0;
			}
			this.addTypeJudgement(result, new HashSet<TTRAustinianProp>(), this.node(this.bottom()), false); //have to find its place..
			resultNode = this.nodeFromTTRRecordType(result);
			for (Object node : this.predecessors(resultNode)){
				this.addAustinianJudgements(resultNode, ((TTRLatticeNode) ((Node) node).getWeight()).getProps(), true);
			}
		}
		Node meetNode = this.meetNode(conditionNode, resultNode);
		logger.debug(meetNode);
		Double conditionalMass = ((TTRLatticeNode) conditionNode.getWeight()).getProbabilityMass();

		return ((TTRLatticeNode) meetNode.getWeight()).getProbabilityMass()/
				conditionalMass;
	}
	
	
	public void constructFromAtoms(List<TTRRecordType> myttr, List<Set<TTRAustinianProp>> myprops) {
		/*
		 * Constructs the TTR lattice from the bottom up as in Hough and Purver 2014 TTNLS
		 */
		TTRLatticeNode abottom = new TTRLatticeNode();
		abottom.setBottom();
		Node bottom = new Node(abottom);
		this.addNode(bottom);

		// atoms
		for (int i = 0; i < myttr.size(); i++) {
			Node n = new Node(new TTRLatticeNode(myttr.get(i), myprops.get(i)));
			this.addNode(n);
			this.addEdge(n, bottom);
		}
		// implement algorithm as in paper
		List<Object> agenda = new ArrayList<Object>();
		for (Object obj : this.backwardReachableNodes(abottom)) {
			agenda.add(obj); // access the bottom through its weight?
		}
		// System.out.println(agenda.get(0));
		int count = 0;
		while (agenda.size() > 0) {
			TTRLatticeNode ttrLat = (TTRLatticeNode) agenda.get(0);
			// Node ntop = node(agenda.get(0));
			agenda.remove(0);
			// pop top one from agenda //bit different to the paper as this is much more like concept lattice stuff
			// System.

			List<Object> subagenda = new ArrayList<Object>();
			for (Object obj : agenda) {

				// create minimal common supertype/join element (could be empty, in which case it's top).
				TTRRecordType ttr = ((TTRLatticeNode) obj).ttr.minimumCommonSuperTypeBasic(ttrLat.ttr,
						new HashMap<Variable, Variable>());
				TTRLatticeNode nweight = new TTRLatticeNode(ttr, new HashSet<TTRAustinianProp>()); // need to start
																									// anew!
				nweight.props.addAll(ttrLat.getProps());
				nweight.props.addAll(((TTRLatticeNode) obj).getProps());

				System.out.println("NWEIGHT!");

				// order ttrLat appropriately to the supertype created here (and the others in the graph)??
				if (containsTTRRecordType(nweight.ttr)) { // already in here, just order (and inherit, should have
															// happened above?)
					nweight.props.addAll(((TTRLatticeNode) this.nodeFromTTRRecordType(nweight.ttr).getWeight())
							.getProps());
					System.out.println("contains!");
					System.out.println(this.nodeFromTTRRecordType(nweight.ttr));
					this.nodeFromTTRRecordType(nweight.ttr).setWeight(nweight);
					// need to search to order.. i.e. go up as high as possible
					//order(this.nodeFromTTRRecordType(nweight.ttr), node(obj));

					if (this.reachableNodes(this.nodeFromTTRRecordType(nweight.ttr)).contains(node(obj))) {
						continue;
					}

					if (!node(obj).equals(this.nodeFromTTRRecordType(nweight.ttr))) {
						addEdge(this.nodeFromTTRRecordType(nweight.ttr), node(obj));
					}

					if (this.reachableNodes(this.nodeFromTTRRecordType(nweight.ttr)).contains(node(ttrLat))) {
						continue;
					}

					if (!node(ttrLat).equals(this.nodeFromTTRRecordType(nweight.ttr))) {
						addEdge(this.nodeFromTTRRecordType(nweight.ttr), node(ttrLat));
					}
					continue;
				}
				// otherwise new node, order it to its supertype
				Node n = new Node(nweight);
				addNode(n);
				addEdge(n, node(obj));
				addEdge(n, node(ttrLat));
				// add it to the agenda for further supertyping
				if (!agenda.contains(nweight)) {
					subagenda.add(nweight);
				}
			}
			// add all the new ones to the agenda for further subtype checking
			agenda.addAll(subagenda);
			System.out.println("agenda");
			for (Object i : agenda) {
				System.out.println(i);
			}
			System.out.println("sss");
			System.out.println(this.toString());
			count += 1;
			pause();
			// if (count>0){break;}
		}

	}
	
	/**
	 * 
	 * @param oldnode the node to add the new judgement(s) to
	 * @param props the new judgements
	 * @param addUpSet whether to add the props to upset of the node too
	 */
	public void addAustinianJudgements(Node mynode, Set<TTRAustinianProp> props, boolean addUpSet){
		Set<TTRAustinianProp> newweight = ((TTRLatticeNode) mynode.getWeight()).getProps();
		if (newweight == null){
			newweight = props;
		} else {
			newweight.addAll(props);
		}
		TTRRecordType ttr = ((TTRLatticeNode) mynode.getWeight()).getTTR();
		mynode.setWeight(new TTRLatticeNode(ttr,newweight));
		if (mynode.equals(this.nodeFromTTRRecordType(TTRRecordType.parse("[]")))){
			return;
		}
		
		if (addUpSet==true){ //TODO could optimize by making it truly recursive
			Collection<Node> upSet = this.reachableNodes(mynode); //nB successors just parents
			//logger.error(upSet);
			for (Node node : upSet){
				this.addAustinianJudgements(node, props, true);
			}
		}

	}

	/**
	 * Function inspired by Van der Merwe et al. (2004) 'AddIntent: A New Incremental Algorithm for Constructing Concept Lattices'.
	 * Adds the intent (Record Type) and the extents (objects, which are Austinian propositions which are judgements of situations being of that type with a given probability)
	 */
	public Node addTypeJudgement(TTRRecordType ttr,Set<TTRAustinianProp> props, Node child, boolean addProps){
		//Turn @ttr record type and @props of that type into a new Node if needed, else adding to existing node
		//Takes as given that the @child node is a child of the new Node
		//Get the maximal intent (all minimal common supertypes/upper bound) of @ttr in lattice
		logger.debug("addTypeJudgement===========:");
		logger.debug("ttr:" + ttr.toString());
		logger.debug("props:" + props.toString());
		logger.debug("child:" + child.toString());
		logger.debug(addProps);
		
		Collection<Node> parents = this.getParents(child);
		//System.out.print("PARENTS OF " + child.toString() + " are : ");
		//System.out.println(parents);
		Node myNode = new Node(new TTRLatticeNode(ttr, props)); //It's position/edges will be instantiated below if needed

		this.addNode(myNode);
		this.addEdge(child,myNode); //always add up
		logger.debug("adding Node ");
		logger.debug(this.nodeLabel(myNode));
		for (Node parent: parents){

			TTRRecordType parentTTR = ((TTRLatticeNode) parent.getWeight()).getTTR();
			if (parentTTR.equals(ttr)){ // we have a match, add props to its up set and return
				logger.debug("already in here as parent" + myNode.toString());
				logger.debug("preserving outgoing edges and removing Node ");
				logger.debug(this.nodeLabel(myNode));
				
				Collection<Edge> collectedEdges = this.outputEdges(myNode); //preserving edges made to parents, if any
				for (Edge edge : collectedEdges){
					if (!this.edgeExists(parent, edge.sink())){
						this.addEdge(parent,edge.sink());
					} else {
						logger.debug("Edge exists!");
					}
					
				}
				
				this.removeNode(myNode); 
				myNode = parent;
				break; //no need to search any further, nor connect at bottom
			}
			if (parentTTR.subsumes(ttr)){ 
				//if myNode subsumes parent, reorder myNode between child and parent
				logger.debug("parent" + parentTTR.toString() + " subsumes ttr " + ttr);
				this.removeEdge(child, parent);
				logger.debug("adding edge from " + myNode.toString() + " to parent " + parent.toString());
				if (!this.edgeExists(myNode, parent)){
					this.addEdge(myNode,parent); //connect up to parent
				} else {
					logger.debug("Edge exists!");
				}
				continue;
				
			} 
			//If we got here
			//parent doesn't subsume myNode, needs to generate minimal common supertype (join), then search bottom up for this RT if in the lattice
			
			TTRRecordType minCommonSuper = ttr.minimumCommonSuperTypeBasic(parentTTR, new HashMap<Variable,Variable>());
			logger.debug("Making new supertype between " + ttr.toString() + parentTTR.toString());
			logger.debug(minCommonSuper);
			Set<TTRAustinianProp> commonProps = new HashSet<TTRAustinianProp>(((TTRLatticeNode) myNode.getWeight()).getProps());
			Set<TTRAustinianProp> parentProps = new HashSet<TTRAustinianProp>(((TTRLatticeNode) parent.getWeight()).getProps());;
			commonProps.retainAll(parentProps);
			//TODO it might be in here already, need to bottom up search for it...
			
			if (minCommonSuper.equals(ttr)){
				
				logger.debug("Matched min common super");
				if (this.edgeExists(child,myNode)){
						this.removeEdge(child,myNode);
				}
				
				//boolean removeNode = false;
				boolean foundIntent = false;
				if (!this.successors(parent).isEmpty()&!this.successors(child).isEmpty()){
					logger.debug("Checking upper bound! ");
					logger.debug(parent);
					logger.debug(child);

					Collection<Node> nodes = this.upperBound(child,parent);
					//System.out.println(nodes);
					
					for (Node grandparent : nodes){
						if (((TTRLatticeNode) grandparent.getWeight()).getTTR().equals(minCommonSuper)){
							logger.debug("Removing myNode " + this.nodeLabel(myNode));
							//removeNode = true;
							this.removeNode(myNode);
							myNode = grandparent;
							foundIntent = true;
							//return myNode;
							break;
						}
						
					}
				} 
				
				if (foundIntent){ //myNode is removed, so don't go on
					break;
				}
				logger.debug("Matched min common super adding Edge from child ");
				logger.debug(this.nodeLabel(parent));
				logger.debug(" to parent ");
				logger.debug(this.nodeLabel(myNode));
				//this.addTypeJudgement(ttr, parentProps, myNode, true); 
				if (!this.edgeExists(parent, myNode)){
					this.addEdge(parent,myNode); //connect up to parent
				} else {
					logger.debug("Edge exists!");
				}
				
				
				//break;
				
			} else {
				//Not the same, is just a supertype, need to check if it's in the lattice already, if not, needs to be added
				logger.debug("RECURSIVE CALL");
				boolean addPropsHere = commonProps.isEmpty() ? true: false;
				Node newparent = this.addTypeJudgement(minCommonSuper, parentProps, parent, addPropsHere); //This has to add the new one
				logger.debug("recusive call end");
				logger.debug("min common super adding Edge from child ");
				logger.debug(this.nodeLabel(myNode));
				logger.debug(" to parent ");
				logger.debug(this.nodeLabel(newparent));
				//NB only add if no intervening edges
				if (!this.reachableNodes(myNode).contains(newparent)){
					//this.addAustinianJudgements(newparent, commonProps, true);
					if (!this.edgeExists(myNode,newparent)){
						
						this.addEdge(myNode,newparent); //connect up to parent
					} else {
						logger.debug("Edge exists!");
					}
				}
				
				
				//TODO bit hacky, but need to remove any unnecessary parents from myNode which are now redundant with newparent
				//If it finds another node that isn't the new parent, remove the new parent?
				Collection<Node> reachables = this.reachableNodes(myNode); //this is the upset
				//System.out.println(reachables);
				for (Node parent_of_myNode : reachables){ //check through parents
					TTRRecordType testTTR = ((TTRLatticeNode) parent_of_myNode.getWeight()).getTTR();
					if (testTTR.subsumes(minCommonSuper)&!parent_of_myNode.equals(newparent)){
				  		//Remove the link between parent and child
						if (this.edgeExists(myNode, parent_of_myNode)){
							logger.debug("subsumed min common super");
							this.removeEdge(myNode, parent_of_myNode);

						}		
					} 
				} //end for
			} //end else
			
		}

		if (addProps==true){
			if (!this.cycleNodeCollection().isEmpty()){
				logger.error(this.cycleNodeCollection());
				logger.error(this);
				pause();
			}
			this.addAustinianJudgements(myNode,props,true); //Add the type judgements to the child node as this has been checked before calling this- initially we know this is ok in the first call where child==bottom
		}
		

		return myNode;
		
	}
	
	public void init(){
		TTRLatticeNode abottom = new TTRLatticeNode();
		abottom.setBottom();
		Node bottom = new Node(abottom);
		this.addNode(bottom);
		TTRLatticeNode mytop = new TTRLatticeNode();
		mytop.setTop();
		mytop.setTtr(TTRRecordType.parse("[]"));//the empty record type at the top
		Node top = new Node(mytop);
		this.addNode(top);
		this.addEdge(bottom,top); //always go outwards from bottom 'is a subtype of' relation
		
	}
	
	/**
	 * The createLatticeIncrementally using the AddIntent Function from Van der Merwe et al. (2004) 'AddIntent: A New Incremental Algorithm for Constructing Concept Lattices'
	 */
	public void constructIncrementally(List<TTRRecordType> ttrAtoms, List<Set<TTRAustinianProp>> propAtoms) {

		//Initialize a lattice with just a top (empty record type) and bottom (absurdity)
		this.init();
		for (int i = 0; i < ttrAtoms.size(); i++) {
			//addTypeJudgement can be called during online learning too
			//parent Candidate always a parent of the bottom node (initially top, but then other atoms)
			System.out.println("ADDING ATOM-----------");
			System.out.println(propAtoms.get(i));
			addTypeJudgement(ttrAtoms.get(i), propAtoms.get(i), this.node(this.bottom()), true);
			System.out.println(this);
			//pause();
		}

		
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		boolean shapes = true;//checking shapes or dialogue rules		
		TTRRecordType ttr = null;
		TTRRecordType ttr2 = null;
		TTRRecordType ttr3 = null;
		
		if (shapes){
			//small test on three shapes of equal probability
			ttr = TTRRecordType.parse("[ x : e |p1==circle(x) : t|p==yellow(x) : t]");
			ttr2 = TTRRecordType.parse("[ x : e |p1==square(x) : t|p==yellow(x) : t]");
			ttr3 = TTRRecordType.parse("[ x : e |p1==square(x) : t|p==purple(x) : t]");
		} else {
			//simple dialogue rules
			ttr = TTRRecordType.parse("[ pre : [ m1==hello : e ] | eff : [ m2==bye : e ] ]");
			ttr2 = TTRRecordType.parse("[ pre : [ m==hi : e ] | eff : [ m1==bye : e ] ]");
			ttr3 = TTRRecordType.parse("[ pre : [ m==hi : e ] | eff : [ m1==byebye : e ] ]");
		}
		
		//create simple type judgements with probability = 1 for each one
		//double t = 1.0;
		TTRLattice lattice = new TTRLattice();
		Set<TTRAustinianProp> s = new HashSet(Arrays.asList(new TTRAustinianProp(ttr, 1.0, 1)));
		Set<TTRAustinianProp> s2 = new HashSet(Arrays.asList(new TTRAustinianProp(ttr2, 1.0, 2)));
		Set<TTRAustinianProp> s3 = new HashSet(Arrays.asList(new TTRAustinianProp(ttr3, 1.0, 3)));
		List<TTRRecordType> myttr = Arrays.asList(ttr, ttr2, ttr3);
		List<Set<TTRAustinianProp>> myprops = new ArrayList<Set<TTRAustinianProp>>();
		myprops.add(s);
		myprops.add(s2);
		myprops.add(s3);
		//build lattice
		lattice.constructIncrementally(myttr, myprops);
		
		if (shapes){
			System.out.println(lattice.conditionalProbability(TTRRecordType.parse("[ x : e |p1==square(x) : t]"),TTRRecordType.parse("[ x : e |p1==square(x) : t|p==purple(x) : t]")));
			System.out.println(lattice.conditionalProbability(TTRRecordType.parse("[ x : e |p==purple(x) : t]"),TTRRecordType.parse("[ x : e |p1==square(x) : t|p==purple(x) : t]")));
			System.out.println(lattice.probability(TTRRecordType.parse("[ x : e |p1==square(x) : t]")));
			System.out.println(lattice.probability(TTRRecordType.parse("[ x : e |p1==square(x) : t|p==purple(x) : t]")));
		} else {
			System.out.println(lattice.probability(TTRRecordType.parse("[ pre : [ m1==hello : e ] | eff : [ m2==bye : e ] ]")));
			System.out.println(lattice.conditionalProbability(TTRRecordType.parse("[ pre : [ m==hi : e ]]"),TTRRecordType.parse("[ eff : [ m1==bye : e ] ]")));
			
		}
		
		//Bigger experiment with 100+ RTs
		RecordTypeCorpus corpus = new RecordTypeCorpus();
		try {
			corpus.loadCorpus(new File("/Users/julianhough/git/dsttr/corpus/CHILDES/eveTrainPairs/CHILDESconversion100TestFinal.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		lattice = new TTRLattice();
		lattice.init();
		int i = 1;
		for (Pair<Sentence<Word>, TTRRecordType> p : corpus){
			ttr = p.second().removeHead();
			System.out.println("adding " + ttr.toString());
			Set<TTRAustinianProp> s4 = new HashSet(Arrays.asList(new TTRAustinianProp(ttr, 1.0, i)));
			lattice.addTypeJudgement(ttr, s4, lattice.node(lattice.bottom()), true);
			i+=1;
			
		}
		System.out.println(lattice.edgeCount());
		System.out.println(lattice.nodeCount());
		//probability of the subordinate event being cooking:
		System.out.println(lattice.conditionalProbability(TTRRecordType.parse("[]"),TTRRecordType.parse("[ e2==cook : es ]")));
		//probability that the speaker is the subject/agent, cycle through variations.. TODO labels should be determined
		System.out.println(lattice.conditionalProbability(TTRRecordType.parse("[]"),TTRRecordType.parse("[ x==i : e|e1 : es|p1==subj(e1, x) : t ]")));
		System.out.println(lattice.conditionalProbability(TTRRecordType.parse("[]"),TTRRecordType.parse("[ x1==i : e|e1 : es|p1==subj(e1, x1) : t ]")));
		System.out.println(lattice.conditionalProbability(TTRRecordType.parse("[]"),TTRRecordType.parse("[ x2==i : e|e1 : es|p1==subj(e1, x2) : t ]")));

	}

}
