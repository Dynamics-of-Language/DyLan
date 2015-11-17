package qmul.ds.ttrlattice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ptolemy.graph.DirectedAcyclicGraph;
import ptolemy.graph.Edge;
import ptolemy.graph.GraphActionException;
import ptolemy.graph.Node;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.formula.Variable;

public class TTRLattice extends DirectedAcyclicGraph {

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
		System.out.println("removing Edge from child ");
		System.out.print(this.nodeLabel(node1));
		System.out.print(" to parent ");
		System.out.print(this.nodeLabel(node2));
		System.out.println("");
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
	 * TODO This is a lowest common ancestor algorithm
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
		
		Collection<Node> upperBound = this.backwardReachableNodes(node1); //relies on this being ordered for max efficiency
		for (Node child : upperBound){
			if (child.equals(this.node(this.bottom()))){
				continue;
			}
			if (child.equals(node2)){
				return node2;
			}
			
			if (this.edgeExists(child, node2)&&!child.equals(this.bottom())){ //we've found it
				return child;
			} 
		}
		
		
		/*Collection<Node> predec = this.predecessors(node1);
		System.out.println(predec);
		int count = predec.size();
		if (count==1&&predec.toArray()[0]==this.node(this.bottom())){
			if (!)return meetNode(node2,node1)
		}
		int i = 0;
		for (Node child : predec){
			i+=1;
			System.out.println(child);
			if (child.equals(node2)){
				return node2;
			}
			
			if (this.edgeExists(child, node2)&&!child.equals(this.bottom())){ //we've found it
				return child;
			} 
			
			if ((!child.equals(this.bottom()))&&i==count){ //search all branches first- kind of breadth first
				//System.out.println("Recurse");
				return this.meetNode(child, node2);
			}
		
			
		}
		*/
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
		Node mynode = null;
		if (this.nodeFromTTRRecordType(condition)==null){
			if (condition.subsumes(result)){
				//System.out.println("No conditioning node, but supertype of " + result.toString()); //TODO this isn't technically true
				return 1.0;
			}
			mynode = this.addTypeJudgement(condition, new HashSet<TTRAustinianProp>(), this.node(this.bottom()), false); //have to find its place..
			//System.out.println(mynode);
		} else if (this.nodeFromTTRRecordType(result)==null){
			if (result.subsumes(condition)){
				System.out.println("No resulting node, but supertype of " + condition.toString());
				return 1.0;
			}
			mynode = this.addTypeJudgement(result, new HashSet<TTRAustinianProp>(), this.node(this.bottom()), false); //have to find its place..
			//System.out.println(mynode);
			//System.out.println(this);
		}
		Node meetNode = this.meetNode(this.nodeFromTTRRecordType(condition), this.nodeFromTTRRecordType(result));
		System.out.println(meetNode);
		Double conditionalMass = ((TTRLatticeNode) this.nodeFromTTRRecordType(condition).getWeight()).getProbabilityMass();
		if (mynode!=null){
			this.removeNode(mynode); //Only temporary node for this
		}
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
		//System.out.println("ADDDING PROPS-----000000");
		//System.out.println(mynode);
		//System.out.println(props);
		Set<TTRAustinianProp> newweight = ((TTRLatticeNode) mynode.getWeight()).getProps();
		if (newweight == null){
			newweight = props;
		} else {
			newweight.addAll(props);
		}
		TTRRecordType ttr = ((TTRLatticeNode) mynode.getWeight()).getTTR();
		mynode.setWeight(new TTRLatticeNode(ttr,newweight));
		if (mynode.equals(this.top())){
			return;
		}
		
		//pause();
		if (addUpSet==true){ //TODO could optimize by making it truly recursive
			Collection<Node> upSet = this.reachableNodes(mynode); //nB successors just parents
			System.out.println(upSet);
			for (Node node : upSet){
				this.addAustinianJudgements(node, props, true);
			}
		}
		//System.out.println(this);
		//System.out.println("0000000");
	}

	/**
	 * Function inspired by Van der Merwe et al. (2004) 'AddIntent: A New Incremental Algorithm for Constructing Concept Lattices'.
	 * Adds the intent (Record Type) and the extents (objects, which are Austinian propositions which are judgements of situations being of that type with a given probability)
	 */
	public Node addTypeJudgement(TTRRecordType ttr,Set<TTRAustinianProp> props, Node child, boolean addProps){
		//Turn @ttr record type and @props of that type into a new Node if needed, else adding to existing node
		//Takes as given that the @child node is a child of the new Node
		//Get the maximal intent (all minimal common supertypes/upper bound) of @ttr in lattice
		System.out.println("addTypeJudgement===========:");
		System.out.println("ttr:" + ttr.toString());
		System.out.println("props:" + props.toString());
		System.out.println("child:" + child.toString());
		System.out.println(addProps);
		
		Collection<Node> parents = this.getParents(child);
		//System.out.print("PARENTS OF " + child.toString() + " are : ");
		//System.out.println(parents);
		Node myNode = new Node(new TTRLatticeNode(ttr, props)); //It's position/edges will be instantiated below if needed

		this.addNode(myNode);
		this.addEdge(child,myNode); //always add up
		System.out.print("adding Node ");
		System.out.println(this.nodeLabel(myNode));
		for (Node parent: parents){

			TTRRecordType parentTTR = ((TTRLatticeNode) parent.getWeight()).getTTR();
			if (parentTTR.equals(ttr)){ // we have a match, add props to its up set and return
				System.out.println("already in here as parent" + myNode.toString());
				System.out.print("preserving outgoing edges and removing Node ");
				System.out.println(this.nodeLabel(myNode));
				
				Collection<Edge> collectedEdges = this.outputEdges(myNode); //preserving edges made to parents, if any
				for (Edge edge : collectedEdges){
					if (!this.edgeExists(parent, edge.sink())){
						this.addEdge(parent,edge.sink());
					} else {
						System.out.println("Edge exists!");
					}
					
				}
				
				this.removeNode(myNode); 
				myNode = parent;
				break; //no need to search any further, nor connect at bottom
			}
			if (parentTTR.subsumes(ttr)){ 
				//if myNode subsumes parent, reorder myNode between child and parent
				System.out.println("parent" + parentTTR.toString() + " subsumes ttr " + ttr);
				this.removeEdge(child, parent);
				System.out.println("adding edge from " + myNode.toString() + " to parent " + parent.toString());
				if (!this.edgeExists(myNode, parent)){
					this.addEdge(myNode,parent); //connect up to parent
				} else {
					System.out.println("Edge exists!");
				}
				
			} else { 
				//parent doesn't subsume myNode, needs to generate minimal common supertype (join), then search bottom up for this RT if in the lattice
				
				TTRRecordType minCommonSuper = ttr.minimumCommonSuperTypeBasic(parentTTR, new HashMap<Variable,Variable>());
				System.out.println("Making new supertype between " + ttr.toString() + parentTTR.toString());
				Set<TTRAustinianProp> commonProps = new HashSet<TTRAustinianProp>(((TTRLatticeNode) myNode.getWeight()).getProps());
				Set<TTRAustinianProp> parentProps = new HashSet<TTRAustinianProp>(((TTRLatticeNode) parent.getWeight()).getProps());;
				commonProps.retainAll(parentProps);
				//TODO it might be in here already, need to bottom up search for it...
				
				if (minCommonSuper.equals(ttr)){
					
					System.out.println("Matched min common super");
					this.removeEdge(child,myNode);
					
					//boolean removeNode = false;
					if (!this.successors(parent).isEmpty()&!this.successors(child).isEmpty()){
						System.out.print("Checking upper bound! ");
						System.out.print(parent);
						System.out.println(child);

						Collection<Node> nodes = this.upperBound(child,parent);
						//System.out.println(nodes);
						for (Node grandparent : nodes){
							if (((TTRLatticeNode) grandparent.getWeight()).getTTR().equals(minCommonSuper)){
								System.out.println("Removing myNode " + this.nodeLabel(myNode));
								//removeNode = true;
								this.removeNode(myNode);
								myNode = grandparent;
								//return myNode;
								break;
							}
							
						}
					} 

					System.out.print("Matched min common super adding Edge from child ");
					System.out.print(this.nodeLabel(parent));
					System.out.print(" to parent ");
					System.out.print(this.nodeLabel(myNode));
					//this.addTypeJudgement(ttr, parentProps, myNode, true); 
					if (!this.edgeExists(parent, myNode)){
						this.addEdge(parent,myNode); //connect up to parent
					} else {
						System.out.println("Edge exists!");
					}
						
					break;
					
				}
				//Not the same, is a supertype, need to check if it's in the lattice already, if not, needs to be added
				System.out.println("RECURSIVE CALL");
				boolean addPropsHere = commonProps.isEmpty() ? true: false;
				Node newparent = this.addTypeJudgement(minCommonSuper, parentProps, parent, addPropsHere); //This has to add the new one
				System.out.println("recusive call end");
				System.out.print("min common super adding Edge from child ");
				System.out.print(this.nodeLabel(myNode));
				System.out.print(" to parent ");
				System.out.println(this.nodeLabel(newparent));
				//NB only add if no intervening edges
				if (!this.reachableNodes(myNode).contains(newparent)){
					//this.addAustinianJudgements(newparent, commonProps, true);
					if (!this.edgeExists(myNode,newparent)){
						
						this.addEdge(myNode,newparent); //connect up to parent
					} else {
						System.out.println("Edge exists!");
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
							System.out.println("subsumed min common super");
							this.removeEdge(myNode, parent_of_myNode);

						}
						
					} 
				}
				
				
			}
				
		}

		if (addProps==true){
			this.addAustinianJudgements(myNode,props,true); //Add the type judgements to the child node as this has been checked before calling this- initially we know this is ok in the first call where child==bottom
		}
		

		return myNode;
		
	}
	
	/**
	 * The createLatticeIncrementally using the AddIntent Function from Van der Merwe et al. (2004) 'AddIntent: A New Incremental Algorithm for Constructing Concept Lattices'
	 */
	public void constructIncrementally(List<TTRRecordType> ttrAtoms, List<Set<TTRAustinianProp>> propAtoms) {

		//Initialize a lattice with just a top (empty record type) and bottom (absurdity)
		//new Node(new TTRLatticeNode());
		
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
		
		for (int i = 0; i < ttrAtoms.size(); i++) {
			//addTypeJudgement can be called during online learning too
			//parent Candidate always a parent of the bottom node (initially top, but then other atoms)
			System.out.println("ADDING ATOM-----------");
			System.out.println(propAtoms.get(i));
			addTypeJudgement(ttrAtoms.get(i), propAtoms.get(i), bottom, true);
			System.out.println(this);
			//pause();
		}

		
	}
	
	
	/*
	 * public void addNode(TTRRecordType ttr, Set probs){
	 * 
	 * }
	 */

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		TTRLattice lattice = new TTRLattice();
		double t = 1.0;
		TTRRecordType ttr3 = TTRRecordType.parse("[ x : e |p1==circle(x) : t|p==yellow(x) : t]");
		TTRRecordType ttr2 = TTRRecordType.parse("[ x : e |p1==square(x) : t|p==yellow(x) : t]");
		TTRRecordType ttr = TTRRecordType.parse("[ x : e |p1==square(x) : t|p==purple(x) : t]");
		Set<TTRAustinianProp> s = new HashSet(Arrays.asList(new TTRAustinianProp(ttr, t, 1)));
		Set<TTRAustinianProp> s2 = new HashSet(Arrays.asList(new TTRAustinianProp(ttr2, t, 2)));
		Set<TTRAustinianProp> s3 = new HashSet(Arrays.asList(new TTRAustinianProp(ttr3, t, 3)));
		List<TTRRecordType> myttr = Arrays.asList(ttr, ttr2, ttr3);
		List<Set<TTRAustinianProp>> myprops = new ArrayList<Set<TTRAustinianProp>>();
		myprops.add(s);
		myprops.add(s2);
		myprops.add(s3);
		//lattice.constructFromAtoms(myttr, myprops);
		lattice.constructIncrementally(myttr, myprops);
		//System.out.println(lattice.nodeFromTTRRecordType(TTRRecordType.parse("[]")));
		System.out.println(lattice.conditionalProbability(TTRRecordType.parse("[ x : e ]"),TTRRecordType.parse("[ x : e |p1==square(x) : t|p==purple(x) : t]")));
		System.out.println(lattice.probability(TTRRecordType.parse("[ x : e |p1==square(x) : t]")));
		System.out.println(lattice.probability(TTRRecordType.parse("[ x : e |p1==square(x) : t|p==purple(x) : t]")));
		
		

	}

}
