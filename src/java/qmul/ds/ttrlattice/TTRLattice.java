package qmul.ds.ttrlattice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ptolemy.graph.DirectedAcyclicGraph;
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
	
	public Node nodeFromTTRRecordType(TTRRecordType ttr){
		for (Object i : this.nodes()){
			//System.out.println(((TTRLatticeNode)i).ttr);
			if (((TTRLatticeNode)((Node)i).getWeight()).ttr==null){
				continue;
			}
			
			if (((TTRLatticeNode)((Node)i).getWeight()).ttr.equals(ttr)){
				return (Node)i;
			}
		}
		return null;
	}
	
	public boolean containsTTRRecordType(TTRRecordType ttr){
		for (Object i : this.nodes()){
			//System.out.println(((TTRLatticeNode)i).ttr);
			if (((TTRLatticeNode)((Node)i).getWeight()).ttr==null){
				continue;
			}
			
			if (((TTRLatticeNode)((Node)i).getWeight()).ttr.equals(ttr)){
				return true;
			}
		}
		return false;
	}
	
	/***
	 * The main ordering relation computation for two record type nodes that are already in the lattice
	 * @param node1
	 * @param node2
	 */
	public void order(Node node1, Node node2){
		//TODO need the most efficient way of ordering two nodes
		//make it recursive..
		//case 1- it's a proper supertype (not the same) as current node- order it in the subtype relation
		//find the supertypes it is a subtype of (if any) and order
		TTRLatticeNode ttrLat = (TTRLatticeNode) agenda.get(0);
		if (node2.getWeight()) {
			
		}
		//case 2- it's not a type of current node- just return (should stop the recursion)
		//case 3- it's a proper subtype of 
		return;
	}
	
	/***
	 * The ordering operation for a new node which hasn't been ordered yet to other nodes in the lattice
	 * @param node1
	 * @throws GraphActionException 
	 */
	public void orderNode(Node node1) throws GraphActionException{
		//TODO should be top down search
		//Go from top then downward path retuning upon hitting bottom
		//recurse and also keep list of checked nodes, only need to order this one node with each node once
		//List<Object> obs = this.topologicalSort(this.nodes());
		
		order((Node)this.bottom(), (Node) node1); //recurse from bottom??
		
		//for (Object node : obs){
			//TODO
		//	order((Node) node, node1);
			
		//}
		
		
	}


	
	public void constructFromAtoms(List<TTRRecordType> myttr, List<Set<TTRAustinianProp>> myprops){
		/*
		 * Constructs the TTR lattice from the bottom up as in Hough and Purver 2014 TTNLS
		 */
		TTRLatticeNode abottom = new TTRLatticeNode();
		abottom.setBottom();
		Node bottom = new Node(abottom);
		this.addNode(bottom);
	
		//atoms
		for (int i=0; i<myttr.size(); i++){
			Node n = new Node(new TTRLatticeNode(myttr.get(i),myprops.get(i)));
			this.addNode(n);
			this.addEdge(n,bottom);			
		}
		//implement algorithm as in paper
		List<Object> agenda = new ArrayList<Object>();
		for (Object obj :	this.backwardReachableNodes(abottom)){
			agenda.add(obj); //access the bottom through its weight?
		}
		//System.out.println(agenda.get(0));
		int count = 0;
		while (agenda.size()>0){
			TTRLatticeNode ttrLat = (TTRLatticeNode) agenda.get(0);
			//Node ntop = node(agenda.get(0));
			agenda.remove(0);
			//pop top one from agenda //bit different to the paper as this is much more like concept lattice stuff
			//System.
			
			List<Object> subagenda = new ArrayList<Object>();
			for (Object obj : agenda){
				
				//create minimal common supertype/join element (could be empty, in which case it's top).
				TTRRecordType ttr = ((TTRLatticeNode)obj).ttr.minimumCommonSuperTypeBasic(ttrLat.ttr, new HashMap<Variable,Variable>());
				TTRLatticeNode nweight = new TTRLatticeNode(ttr,new HashSet<TTRAustinianProp>()); //need to start anew!
				nweight.props.addAll(ttrLat.getProps());
				nweight.props.addAll(((TTRLatticeNode)obj).getProps());

				System.out.println("NWEIGHT!");
				 
			
				//order ttrLat appropriately to the supertype created here (and the others in the graph)??
				if (containsTTRRecordType(nweight.ttr)){ // already in here, just order (and inherit, should have happened above?)
					nweight.props.addAll(((TTRLatticeNode)this.nodeFromTTRRecordType(nweight.ttr).getWeight()).getProps());
					System.out.println("contains!");
					System.out.println(this.nodeFromTTRRecordType(nweight.ttr));
					this.nodeFromTTRRecordType(nweight.ttr).setWeight(nweight);
					//need to search to order.. i.e. go up as high as possible
					order(this.nodeFromTTRRecordType(nweight.ttr),node(obj));
					
					if (this.reachableNodes(this.nodeFromTTRRecordType(nweight.ttr)).contains(node(obj))){
						continue;
					}
					
					if (!node(obj).equals(this.nodeFromTTRRecordType(nweight.ttr))){
						addEdge(this.nodeFromTTRRecordType(nweight.ttr),node(obj));
					}
					
					if (this.reachableNodes(this.nodeFromTTRRecordType(nweight.ttr)).contains(node(ttrLat))){
						continue;
					}
					
					if (!node(ttrLat).equals(this.nodeFromTTRRecordType(nweight.ttr))){
						addEdge(this.nodeFromTTRRecordType(nweight.ttr),node(ttrLat));
					}
					continue;
				} 
				//otherwise new node, order it to its supertype
				Node n = new Node(nweight);
				addNode(n);
				addEdge(n,node(obj));
				addEdge(n,node(ttrLat));
				//add it to the agenda for further supertyping
				if (!agenda.contains(nweight)){
					subagenda.add(nweight);
				}
			}
			//add all the new ones to the agenda for further subtype checking
			agenda.addAll(subagenda);
			System.out.println("agenda");
			for (Object i : agenda){
				System.out.println(i);
			}
			System.out.println("sss");
			System.out.println(this.toString());
			count+=1;
			pause();
			//if (count>0){break;}
		}
		
	}
	
	/*public void addNode(TTRRecordType ttr, Set probs){
		
	}*/

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TTRLattice lattice = new TTRLattice();
		double t = 1.0/3.0;
		TTRRecordType ttr = TTRRecordType.parse("[ x : e |p1==square(x) : t|p==purple(x) : t]");
		TTRRecordType ttr2 = TTRRecordType.parse("[ x : e |p1==square(x) : t|p==yellow(x) : t]");
		TTRRecordType ttr3 = TTRRecordType.parse("[ x : e |p1==circle(x) : t|p==yellow(x) : t]");
		Set<TTRAustinianProp> s = new HashSet(Arrays.asList(new TTRAustinianProp(ttr,t,1)));
		Set<TTRAustinianProp> s2 = new HashSet(Arrays.asList(new TTRAustinianProp(ttr2,t,2)));
		Set<TTRAustinianProp> s3 = new HashSet(Arrays.asList(new TTRAustinianProp(ttr3,t,3)));
		List<TTRRecordType> myttr =  Arrays.asList(ttr,ttr2,ttr3);
		List<Set<TTRAustinianProp>> myprops = new ArrayList<Set<TTRAustinianProp>>();
		myprops.add(s);
		myprops.add(s2);
		myprops.add(s3);
		lattice.constructFromAtoms(myttr, myprops);
		
		/*
		Node n = new Node(new TTRLatticeNode(ttr,s));
		lattice.addNode(n);
		TTRRecordType ttr1 = new TTRRecordType();
		TTRLatticeNode tn1 = new TTRLatticeNode(ttr1,s);
		Node n1 = new Node(tn1);
		lattice.addNode(n1);
		lattice.addEdge(n, n1);
		*/
		//lattice.
		//lattice.addEdge(n1,lattice.bottom());
		//System.out.println(lattice.downSet(tn1));
		for (Object i : lattice.weightArray(lattice.nodes())){
			System.out.println(i.toString());
			//System.out.println(((TTRLatticeNode) i));
			//System.out.println(lattice.downSet((TTRLatticeNode)i));
			//System.out.println(((TTRLatticeNode)i).ttr);
		}
		System.out.println("edges");
		for (Object e : lattice.edges()){
			System.out.println(e.toString());
		}
		
		
		

	}

}
