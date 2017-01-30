package qmul.ds.ttrlattice;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import qmul.ds.formula.TTRField;
import qmul.ds.formula.TTRRecordType;
import qmul.ds.formula.Variable;
import qmul.ds.learn.RecordTypeCorpus;



public class TTRLattice extends DirectedAcyclicGraph {
	
	public int counter = 0;
	public TTRLatticeViewer latticeViewer = null;
	
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
		counter = 0;
		latticeViewer = new TTRLatticeViewer(this);
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
	
	public TTRLatticeNode getTop(){
		for (Object node : this.nodes()){
			TTRLatticeNode myNode = (TTRLatticeNode) ((Node) node).getWeight();
			if (myNode.top==true){
				return myNode;
			}
		}
		return null;
	}

	/**
	 * Uses the node IDs to remove edges, rather than edge IDs
	 * @param node1
	 * @param node2
	 */
	public void removeEdge(Node node1, Node node2){
		logger.debug("Removing Edge from child Node" + String.valueOf(this.nodeLabel(node1)) + " to parent Node" + String.valueOf(this.nodeLabel(node2)));
		boolean node2top = false;
		TTRLatticeNode newnode = null;
		if (((TTRLatticeNode) node1.getWeight()).top==true){
			logger.debug("removing link from top!!");
		}
		
		if (((TTRLatticeNode) node2.getWeight()).top==true){
			logger.debug("removing link to top!!");
			node2top = true;
			//Node topnode = this.node(node2.getWeight()); //problem is it might not be in the graph at this point
			newnode = (TTRLatticeNode) node2.getWeight();
			newnode.setTop();
		}
		Object[] myedges = this.outputEdges(node1).toArray(); //TODO need a copy TODO make this a getEdge function
		Collection<Object> edges = new HashSet<Object>(Arrays.asList(myedges));
		edges.retainAll(this.incidentEdges(node2)); //TODO deep copy?? check
		this.removeEdge((Edge) edges.toArray()[0]); //disconnect
		if (node2top){
			logger.debug("Re-making top due to removal");
			logger.debug("top" + this.top());
			logger.debug("top other" + this.getTop());
			logger.debug(node2);

			//this.node(node2.getWeight()).setWeight(newnode); //problem is it might not be in the graph
			node2.setWeight(newnode);
		}
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
		System.out.println("top");
		System.out.println(this.top());
		Node node =  this.nodeFromTTRRecordType(ttr);
		if (node==null){
			logger.debug("No node, adding supertype");
			this.addTypeJudgement(ttr, new HashSet<AustinianProbabilisticProp>(), this.node(this.bottom()), false); //have to find its place..
			node = this.nodeFromTTRRecordType(ttr);
			for (Object pnode : this.predecessors(node)){
				this.addAustinianJudgements(node, ((TTRLatticeNode) ((Node) pnode).getWeight()).getProps(), true);
			}
			logger.debug(this);
		}
		Double p = ((TTRLatticeNode) (node.getWeight())).getProbabilityMass();
		logger.debug(p);
		logger.debug(this.top());
		return p/((TTRLatticeNode) this.top()).getProbabilityMass();
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
			this.addTypeJudgement(condition, new HashSet<AustinianProbabilisticProp>(), this.node(this.bottom()), false); //have to find its place..
			conditionNode = this.nodeFromTTRRecordType(condition);
			for (Object node : this.predecessors(conditionNode)){
				this.addAustinianJudgements(conditionNode, ((TTRLatticeNode) ((Node) node).getWeight()).getProps(), true);
			}

		}
		if (this.nodeFromTTRRecordType(result)==null){
			logger.debug("No result node");
			if (result.subsumesStrictLabelIdentity(condition)){
				logger.debug("No resulting node, but supertype of " + condition.toString());
				return 1.0;
			}
			this.addTypeJudgement(result, new HashSet<AustinianProbabilisticProp>(), this.node(this.bottom()), false); //have to find its place..
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
	
	
	public void constructFromAtoms(List<TTRRecordType> myttr, List<Set<AustinianProbabilisticProp>> myprops) {
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
				TTRLatticeNode nweight = new TTRLatticeNode(ttr, new HashSet<AustinianProbabilisticProp>()); // need to start
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
	public void addAustinianJudgements(Node mynode, Set<AustinianProbabilisticProp> props, boolean addUpSet){
		Set<AustinianProbabilisticProp> newweight = ((TTRLatticeNode) mynode.getWeight()).getProps();
		if (newweight == null){
			newweight = props;
		} else {
			newweight.addAll(props);
		}
		TTRRecordType ttr = ((TTRLatticeNode) mynode.getWeight()).getTTR();
		boolean istop = (((TTRLatticeNode) mynode.getWeight()).top) ? true : false;
		mynode.setWeight(new TTRLatticeNode(ttr,newweight,istop));
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
	public Node addTypeJudgement(TTRRecordType ttr,Set<AustinianProbabilisticProp> props, Node child, boolean addProps){
		//Turn @ttr record type and @props of that type into a new Node if needed, else adding to existing node
		//Takes as given that the @child node is a child of the new Node
		//Get the maximal intent (all minimal common supertypes/upper bound) of @ttr in lattice
		logger.debug("addTypeJudgement===========:");
		logger.debug("ttr:" + ttr.toString());
		logger.debug("props:" + props.toString());
		logger.debug("child:" + Integer.toString(this.nodeLabel(child)));
		logger.debug("addprops:" + addProps);
		logger.debug("currentTop:" + this.top());
		logger.debug("currentTop other:" + this.getTop());
		
		Collection<Node> parents = this.getParents(child);
		Node myNode = new Node(new TTRLatticeNode(ttr, props)); //It's position/edges will be instantiated below if needed

		this.addNode(myNode); //add new node
		this.addEdge(child,myNode); //connect to the child
		
		logger.debug("adding new Node " + this.nodeLabel(myNode));
		logger.debug("parents of child " + this.nodeLabel(child) + " " + child.toString() + " are :\n " + parents);
		
		boolean foundPredecessors = false; //point is to find the predecessors (all minimal common supertypes) of this new node
		
		for (Node parent: parents){
			logger.debug("Parent loop: " + this.nodeLabel(parent));
			TTRRecordType parentTTR = ((TTRLatticeNode) parent.getWeight()).getTTR();
			if (parentTTR.subsumesStrictLabelIdentity(ttr) && ttr.subsumesStrictLabelIdentity(parentTTR)){ // we have a match, add props to its up set and return
				logger.debug("CASE1: parent matches " + myNode.toString());
				logger.debug("preserving outgoing edges and removing Node "+ this.nodeLabel(myNode));
				
				//TODO not sure about this- how can we guarantee all edges are still there?
				Collection<Edge> collectedEdges = this.outputEdges(myNode); //preserving edges made to parents, if any
				for (Edge edge : collectedEdges){
					if (!this.edgeExists(parent, edge.sink())){
						this.addEdge(parent,edge.sink());
					} else {
						logger.debug("Edge exists!");
						//pause();
					}
				}
				
				if (((TTRLatticeNode) myNode.getWeight()).top==true){
					logger.debug("REMOVING TOP!");
					break;
				}
				this.removeNode(myNode); 
				myNode = parent;
				foundPredecessors = true;
				//continue;

				break; //TODO no need to search any further??, nor connect at bottom- have found intent
			}
			if (parentTTR.subsumesStrictLabelIdentity(ttr)){ 
				//Case 2:
				//if myNode subsumes parent, reorder myNode between child and parent
				logger.debug("CASE 2: parent" + parentTTR.toString() + " subsumes ttr " + ttr);
				this.removeEdge(child, parent); //removing existing edge first
				logger.debug("adding edge from " + myNode.toString() + " to parent " + parent.toString());
				if (!this.edgeExists(myNode, parent)){
					this.addEdge(myNode,parent); //connect up to parent
				} else {
					logger.debug("Edge exists, not adding!");
				}
				logger.debug("adding edge from " + child.toString() + " to parent " + myNode.toString());
				if (!this.edgeExists(child, myNode)){
					this.addEdge(child,myNode); //connect up to parent
				} else {
					logger.debug("Edge exists, not adding!");
				}
				
				if (((TTRLatticeNode) parent.getWeight()).top==true){
					break; //TODO because we've found the predecessors
				}
				
				//logger.debug("RERCURSIVE CALL upwards");
				//myNode = this.addTypeJudgement(parentTTR, props, parent, addProps); //will be the eventual position of this node
				//logger.debug("END RECURSIVE CALL upwards");
				
				foundPredecessors = true;
				

				//continue;
				break;
				
			} 
			//If we get here
			//parent doesn't subsume myNode, generate minimal common supertype (join), then RECURSE search bottom up for this RT if in the lattice
			
			TTRRecordType minCommonSuper = ttr.minimumCommonSuperTypeBasic(parentTTR, new HashMap<Variable,Variable>());
			logger.debug("Making new supertype between " + ttr.toString() + " and node " + this.nodeLabel(parent) + " " + parentTTR.toString() + " \n\tgiving : " + minCommonSuper);
			Set<AustinianProbabilisticProp> commonProps = new HashSet<AustinianProbabilisticProp>(((TTRLatticeNode) myNode.getWeight()).getProps());
			Set<AustinianProbabilisticProp> parentProps = new HashSet<AustinianProbabilisticProp>(((TTRLatticeNode) parent.getWeight()).getProps());;
			commonProps.retainAll(parentProps);
			
			if (minCommonSuper.subsumesStrictLabelIdentity(ttr)&&ttr.subsumesStrictLabelIdentity(minCommonSuper)){
				//Case 3:
				logger.debug("Case 3: min common supertype with parent's ttr IS the ttr record type of myNode");
				//foundPredecessors = true;
				//logger.debug("preserving outgoing edges and removing Node " + this.nodeLabel(myNode));
				//TODO not sure about this- how can we guarantee all edges are still there?
				
				
				//swap the order by detaching from child then reattaching to parent
				if (this.edgeExists(child,myNode)){
					this.removeEdge(child,myNode);
				}
				
				Collection<Edge> collectedEdges = this.inputEdges(myNode); //preserving edges made to children, if any
				
				//if (!this.edgeExists(parent, myNode)){
				//	this.addEdge(parent,myNode);
				//}
				
				if (((TTRLatticeNode) myNode.getWeight()).top==true){
					continue;
				}
				this.removeNode(myNode);
				logger.debug("RERCURSIVE CALL upwards");
				myNode = this.addTypeJudgement(ttr, commonProps, parent, addProps); //will be the eventual position of this node
				logger.debug("END RECURSIVE CALL upwards");
				for (Edge edge : collectedEdges){
					if (!this.predecessors(myNode).contains(edge.source())){
						this.addEdge(edge.source(),myNode);
					} else {
						logger.debug("Path exists!");
						//pause();
					}
				}
				//continue;
				if (((TTRLatticeNode) myNode.getWeight()).top==true){
					break; //TODO because we've found the predecessors
				}
				//continue;
				break;//TODO because we've found the predecessors
			} else if (minCommonSuper.subsumesStrictLabelIdentity(parentTTR)&&parentTTR.subsumesStrictLabelIdentity(minCommonSuper)){
				//Case 4:
				logger.debug("Case 4: min common supertype with parent's ttr IS the parentTTR");
				this.addAustinianJudgements(parent, props, true);
				
				
			}
			//Not the same, if just a supertype, need to check if it's in the lattice already and that it has an intent, if not, needs to be added
			logger.debug("Case 5: min common supertype with parent's ttr could be novel type in lattice");	
			
			logger.debug("RECURSIVE CALL start");
				boolean addPropsHere = commonProps.isEmpty() ? true: false;
				Node newparent = this.addTypeJudgement(minCommonSuper, parentProps, (Node) this.node(this.bottom()), addPropsHere); //This has to add the new one
				logger.debug("RECURSIVE CALL end");
				logger.debug("min common supertype. Adding Edge from myNode " + this.nodeLabel(myNode) + " to new parent " + this.nodeLabel(newparent));
				if (myNode.equals(newparent)){
					continue;
				}
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
				Collection<Node> reachables = this.reachableNodes(newparent); //this is the upset
				logger.debug("Getting reachables of " + this.nodeLabel(newparent));
				logger.debug(reachables);
				for (Node parent_of_myNode : reachables){ //check through parents
					TTRRecordType testTTR = ((TTRLatticeNode) parent_of_myNode.getWeight()).getTTR();
					if (testTTR.subsumesStrictLabelIdentity(minCommonSuper)&!parent_of_myNode.equals(newparent)){
				  		//Remove the link between parent and child
						if (this.edgeExists(myNode, parent_of_myNode)){
							logger.debug("subsumed min common super inner loop, removing edge!");
							this.removeEdge(myNode, parent_of_myNode);

						}		
					} 
				} //end for
			//} //end else
			
		} //end parent loop
		
		//add the propositions to the node
		if (addProps==true){
			if (!this.cycleNodeCollection().isEmpty()){
				logger.error(this.cycleNodeCollection());
				logger.error(this);
				pause();
			}
			logger.debug("Adding austinian props " + props);
			this.addAustinianJudgements(myNode,props,true); //Add the type judgements to the child node as this has been checked before calling this- initially we know this is ok in the first call where child==bottom
		}
		
		logger.debug("returning node " + this.nodeLabel(myNode) + " " + myNode);
		
		/*if (counter>2710){
			for (Object o : this.nodes()){
				Node node = (Node) o;
				logger.debug(this.nodeLabel(node) + " " + node);
			}
			latticeViewer.displayLattice("test_lattice" + Integer.toString(counter) + ".png");
			//pause();
		}
		*/
		//latticeViewer.displayLattice("test_lattice" + Integer.toString(counter) + ".png");
		counter++;
		
		
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
	public void constructIncrementally(List<TTRRecordType> ttrAtoms, List<Set<AustinianProbabilisticProp>> propAtoms) {

		//Initialize a lattice with just a top (empty record type) and bottom (absurdity)
		this.init();
		for (int i = 0; i < ttrAtoms.size(); i++) {
			//addTypeJudgement can be called during online learning too
			//parent Candidate always a parent of the bottom node (initially top, but then other atoms)
			System.out.println("ADDING ATOM-----------");
			System.out.println(propAtoms.get(i));
			addTypeJudgement(ttrAtoms.get(i), propAtoms.get(i), this.node(this.bottom()), true);
			logger.debug(this);
			logger.debug("currentTop after atom:" + this.top());
			logger.debug("currentTop other after atom:" + this.getTop());
			
			
	
		}

		
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		boolean shapes = false;//checking shapes or dialogue rules		
		TTRRecordType ttr = null;
		TTRRecordType ttr2 = null;
		TTRRecordType ttr3 = null;
		
		TTRRecordType ttr4 = TTRRecordType.parse("[onet : e|coacheevaprogress==inprogress : e|coachda==acknowledgeme : e]");
		
		
		if (shapes){
			//small test on three shapes of equal probability
			ttr = TTRRecordType.parse("[ x : e |p1==circle(x) : t|p==yellow(x) : t]");
			ttr2 = TTRRecordType.parse("[ x : e |p1==square(x) : t|p==yellow(x) : t]");
			ttr3 = TTRRecordType.parse("[ x : e |p1==square(x) : t|p==purple(x) : t]");
		} else {
			//simple dialogue rules
			TTRRecordType ttrtest = TTRRecordType.parse("[ pre : [ m1==hello : e ] | eff : [ m2==bye : e ] ]");
			//ttr = TTRRecordType.parse("[ coachutt : [ hallo : e | complete: e ] | coachda : [ question : e | complete : e ] | skud : [ skillpastexperience : skud | sportexperience : skud ] | skillsraised : [] ]");
			//ttr2 = TTRRecordType.parse("[ coachutt : [ handballokay : coachutt | complete : eventstatus ] | coachda : [ acknowledgeverbal : coachda | complete : event_status ] | skillsraised : [ skillpastexperience : skillsraised | sportexperience : skillsraised ] ]");
			//ttr3 = TTRRecordType.parse("[ coachutt : [ handballokay : coachutt | complete : eventstatus ] | coachda : [ acknowledgeverbal : coachda | complete : event_status ] | skillsraised : [ skillpastexperience : skillsraised | sportexperience : skillsraised ] ]");
			
			
			ttr = TTRRecordType.parse("[ pre : [ h2==hello : e ] | eff : [ x2==bye : e ] ]");
			ttr2 = TTRRecordType.parse("[ pre : [ h2==hello : e ] | eff : [ x1==bye : e ] ]");
			ttr3 = TTRRecordType.parse("[ pre : [ m==hi : e ] | eff : [ m1==byebye : e ] ]");
		}
		
		TTRRecordType t1 = TTRRecordType.parse("[r : [x : e|head==x : e|p5==milk(x) : t]|x1==you : e|e1==drink : es|x2==your(r.head, r) : e|p3==subj(e1, x1) : t|p4==obj(e1, x2) : t]");
		TTRRecordType t2 = TTRRecordType.parse("[r : [x : e|head==x : e]|x2==your(r.head, r) : e|p3==subj(e1, x1) : t|p4==obj(e1, x2) : t]");
		TTRRecordType t3 = t1.minimumCommonSuperTypeBasic(t2, new HashMap<Variable,Variable>());
		//System.out.println(t2);
		//System.out.println(t3);
		//System.out.println(t2.subsumesStrictLabelIdentity(t2));
		//System.out.println(t2.subsumesStrictLabelIdentity(t3));
		//System.out.println(t3.subsumesStrictLabelIdentity(t2));
		
		//pause();
		
		//create simple type judgements with probability = 1 for each one
		//double t = 1.0;
		TTRLattice lattice = new TTRLattice();
		Set<AustinianProbabilisticProp> s = new HashSet(Arrays.asList(new AustinianProbabilisticProp(ttr, 1.0, 1)));
		Set<AustinianProbabilisticProp> s2 = new HashSet(Arrays.asList(new AustinianProbabilisticProp(ttr2, 1.0, 2)));
		Set<AustinianProbabilisticProp> s3 = new HashSet(Arrays.asList(new AustinianProbabilisticProp(ttr3, 1.0, 3)));
		List<TTRRecordType> myttr = Arrays.asList(ttr, ttr2, ttr3);
		List<Set<AustinianProbabilisticProp>> myprops = new ArrayList<Set<AustinianProbabilisticProp>>();
		myprops.add(s);
		myprops.add(s2);
		myprops.add(s3);
		//build lattice
		//lattice.constructIncrementally(myttr, myprops);
		
		
		
		//pause();
		
		if (shapes){
			System.out.println(lattice.conditionalProbability(TTRRecordType.parse("[ x : e |p1==square(x) : t]"),TTRRecordType.parse("[ x : e |p1==square(x) : t|p==purple(x) : t]")));
			System.out.println(lattice.conditionalProbability(TTRRecordType.parse("[ x : e |p==purple(x) : t]"),TTRRecordType.parse("[ x : e |p1==square(x) : t|p==purple(x) : t]")));
			System.out.println(lattice.probability(TTRRecordType.parse("[ x : e |p1==square(x) : t]")));
			System.out.println(lattice.probability(TTRRecordType.parse("[ x : e |p1==square(x) : t|p==purple(x) : t]")));
		} else {
			//lattice.addTypeJudgement(TTRRecordType.parse("[ pre : [ m1==hello : e ] ]"), s, lattice.node(lattice.bottom()), false);
			
			//System.out.println(lattice.probability(TTRRecordType.parse("[ pre : [ m1==hello : e ] ]")));
			//System.out.println(lattice.conditionalProbability(TTRRecordType.parse("[ pre : [ m==hi : e ]]"),TTRRecordType.parse("[ eff : [ m1==bye : e ] ]")));
			//Double p1 = lattice.probability(TTRRecordType.parse("[ coachutt : [ handballokay : coachutt ]]"));
			//System.out.println(p1);
			//pause();
			//System.exit(0);
			
		}
		
		//Bigger experiment with 100+ RTs
		RecordTypeCorpus corpus = new RecordTypeCorpus();
		try {
			//String corpusloc = "/Users/julianhough/git/dsttr/corpus/CHILDES/eveTrainPairs/CHILDESconversion100TestFinal.txt";
			//String corpusloc = "C:\\Users\\Julian\\git\\dsttr\\corpus\\CHILDES\\eveTrainPairs\\CHILDESconversion100TestFinal.txt";
			//String corpusloc = "C:\\Users\\Julian\\git\\icspace-corpus-analysis\\information_state_generation_2016\\code\\predicting_next_dialogue_move\\infostates.txt";
			String corpusloc = "/home/julian/git/icspace-corpus-analysis/information_state_generation_2016/code/predicting_next_dialogue_move/infostates.txt";
			corpus.loadCorpus(new File(corpusloc));
			System.out.println("loaded");
			List<TTRRecordType> l = new ArrayList<TTRRecordType>();
			for (Pair<Sentence<Word>, TTRRecordType> p : corpus){
				ttr = p.second();
				l.add(ttr);
			}
			
			
			
			
			TTRRecordType sub = TTRRecordType.parse("[ pre : [ coachutt : [ coachutt : uttthree9four | eventstatus : complete ] | coachda : [ instructdirective : da | complete : event_status ] | coacheeda : null | coachnva : null | coacheenva : null | skud : [ stroke : skill | squat : skill | chestforward : skill ] | skillsraised : [ kneebehindtoes : skill | stroke : skill | kneeangle : skill | squatexperience : skill | stancewidth : skill | slow : skill | practicetechnique : skill | hipstrajectory : skill | sessiongoal : skill | armsinfront : skill | gaze : skill | retract : skill | prep : skill | feetangle : skill | torsotension : skill | torsoposition : skill | armposition : skill | squat : skill | hold : skill | standinfrontofcoach : skill | skillpastexperience : skill | chestforward : skill | shouldersback : skill | depth : skill | kneetrajectory : skill | sessionmanagement : skill | positionincoachingspace : skill ] ] | eff : [ coachutt : [ coachutt : utt50four | eventstatus : complete ] | coachda : [ adjust : da | complete : event_status ] | skud : [ depth : skill ] ] ]");
			
			TTRRecordType supertestrec = TTRRecordType.parse("[ eff : [ coachnva : [ deictic : nva | inprogress : event_status ] ] ]");
			System.out.println(supertestrec);
			System.out.println(supertestrec.subsumesStrictLabelIdentity(sub));
			System.out.println(sub.subsumesStrictLabelIdentity(supertestrec));
			pause();
			
			long t = System.currentTimeMillis();
			int m = Collections.frequency(l,sub);
			int post = 0;
			int post2 = 0;
			for (TTRRecordType rec : l){
				if (supertestrec.subsumesStrictLabelIdentity(rec)){
					System.out.println("**");
					for (TTRField f : rec.getFields()){
						System.out.println(f.getLabel());
						for (TTRField smf : ((TTRRecordType) f.getType()).getFields()){
							System.out.println(smf);
						}
					}
					post++;
				}
			}
			float p = (float) m/ (float) post;
			System.out.println(System.currentTimeMillis()-t);
			System.out.println(p);
			System.out.println(m);
			System.out.println(post);
			System.out.println(post2);
			
			System.exit(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		lattice = new TTRLattice();
		lattice.init();
		int i = 1;
		for (Pair<Sentence<Word>, TTRRecordType> p : corpus){
			ttr = p.second().removeHead();
			System.out.println("adding " + i + " " + ttr.toString());
			Set<AustinianProbabilisticProp> s4 = new HashSet(Arrays.asList(new AustinianProbabilisticProp(ttr, 1.0, i)));
			try {
				if (lattice.top()==null){
					throw new Exception("No lattice top!");
				}
				lattice.addTypeJudgement(ttr, s4, lattice.node(lattice.bottom()), true);
				//TTRLatticeViewer ttrview = new TTRLatticeViewer(lattice,"test_lattice" + Integer.toString(lattice.counter) + ".png");
				//pause();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				System.out.println(lattice.top());
				System.out.println(lattice.bottom());
				//TTRLatticeViewer ttrview = new TTRLatticeViewer(lattice,"test_lattice" + Integer.toString(lattice.counter) + ".png");
				//pause();
				System.exit(0);
			}
			if (i % 10 == 0){
				System.out.println(lattice.counter);
				//lattice.latticeViewer.displayLattice("test_lattice" + Integer.toString(lattice.counter) + ".png");
				//pause();
			}
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
