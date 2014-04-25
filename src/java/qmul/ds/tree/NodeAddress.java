/*******************************************************************************
 * Copyright (c) 2001, 2010 Matthew Purver
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "LICENSE" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *******************************************************************************/
package qmul.ds.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * A 010*-style node address
 * 
 * @author mpurver
 */
public class NodeAddress implements Comparable<NodeAddress>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(NodeAddress.class);

	private static final String ROOT = "0";
	private static final String SUFFIX_0 = BasicOperator.PATH_0;
	private static final String SUFFIX_1 = BasicOperator.PATH_1;
	private static final String SUFFIX_STAR = BasicOperator.PATH_UNFIXED;
	private static final String SUFFIX_LINK = BasicOperator.PATH_LINK;
	private static final String SUFFIX_LOCAL_UNFIXED = BasicOperator.PATH_LOCAL_UNFIXED;

	private String address;

	/**
	 * A new ROOT address
	 */
	public NodeAddress() {
		this.address = ROOT;
	}

	/**
	 * A new address based on the supplied {@link String}
	 * 
	 * @param address
	 */
	public NodeAddress(String address) {
		this.address = address;
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @return true if this is the root address
	 */
	public boolean isRoot() {
		return address.equals(ROOT);
	}

	/**
	 * @return true if this address is fixed to some upper node
	 */
	public boolean isLocallyFixed() {
		return !(address.endsWith(SUFFIX_LOCAL_UNFIXED) || address.endsWith(SUFFIX_STAR));

	}

	/**
	 * 
	 * @return true if the address is not within some unfixed subtree
	 */
	public boolean isFixed() {
		return !(address.contains(SUFFIX_LOCAL_UNFIXED) || address.contains(SUFFIX_STAR));
	}

	public NodeAddress down(String path) {
		// TODO checking
		return new NodeAddress(getAddress() + path);
	}

	public NodeAddress down0() {
		return new NodeAddress(getAddress() + SUFFIX_0);
	}

	public NodeAddress down1() {
		return new NodeAddress(getAddress() + SUFFIX_1);
	}

	public NodeAddress downLink() {
		return new NodeAddress(getAddress() + SUFFIX_LINK);
	}

	public NodeAddress downStar() {
		return new NodeAddress(getAddress() + SUFFIX_STAR);
	}

	public NodeAddress downLocalUnfixed() {
		return new NodeAddress(getAddress() + SUFFIX_LOCAL_UNFIXED);
	}

	public NodeAddress up(String path) {
		if (!getAddress().endsWith(path)) {
			return null;
		}
		return new NodeAddress(getAddress().substring(0, getAddress().lastIndexOf(path)));
	}

	public NodeAddress up() {
		if (getAddress().length() < 2) {
			return null;
		}
		return new NodeAddress(getAddress().substring(0, getAddress().length() - 1));
	}

	public NodeAddress up0() {
		if (getAddress().length() < 2) {
			return null;
		}
		if (!getAddress().endsWith(SUFFIX_0)) {
			return null;
		}
		return new NodeAddress(getAddress().substring(0, getAddress().length() - 1));
	}

	public NodeAddress upNonLink() {
		if (getAddress().length() < 2) {
			return null;
		}
		if (!getAddress().endsWith(SUFFIX_0) && !getAddress().endsWith(SUFFIX_1)
				&& !getAddress().endsWith(SUFFIX_LOCAL_UNFIXED) && !getAddress().endsWith(SUFFIX_STAR)) {
			return null;
		}
		return new NodeAddress(getAddress().substring(0, getAddress().length() - 1));

	}

	public NodeAddress up1() {
		if (getAddress().length() < 2) {
			return null;
		}
		if (!getAddress().endsWith(SUFFIX_1)) {
			return null;
		}
		return new NodeAddress(getAddress().substring(0, getAddress().length() - 1));
	}

	public NodeAddress upLink() {
		if (getAddress().length() < 2) {
			return null;
		}
		if (!getAddress().endsWith(SUFFIX_LINK)) {
			return null;
		}
		return new NodeAddress(getAddress().substring(0, getAddress().length() - 1));
	}

	/**
	 * @param other
	 * @param op
	 * @return can we get from this node to other via op?
	 */

	public boolean to(NodeAddress other, BasicOperator op) {
		if (op.isFixed()) {
			return go(op).equals(other);
		} else if (op.equals(BasicOperator.UP_STAR)) {
			return address.startsWith(other.address);
		} else if (op.equals(BasicOperator.DOWN_STAR)) {
			return other.address.startsWith(address);
		} else {
			throw new RuntimeException("unexpected operator " + op);
		}
	}

	/**
	 * @param other
	 * @param modality
	 * @return can we get from this node to other via modality?
	 */
	public boolean to(Tree t, NodeAddress other, Modality modality) {
		return to(t, other, modality.getOps());
	}

	/**
	 * @param other
	 * @param meta
	 * @return can we get from this node to other via ops? (treating operators as internal i.e. literal) now deprecated
	 *         and commented out.
	 */

	// can only use method if this nodeaddress is fixed. otherwise expansion
	// meaningless
	private ArrayList<NodeAddress> goUpStarExpand() {
		logger.debug("running goUpStarExpand");
		logger.debug("At node:" + this.address);
		if (!this.isLocallyFixed()) {

			return null; // can only use method if this nodeaddress is fixed
		}

		ArrayList<NodeAddress> result = new ArrayList<NodeAddress>();
		String subAddress = address;

		while (!subAddress.isEmpty() && !subAddress.endsWith(SUFFIX_LINK)) {

			result.add(new NodeAddress(subAddress));
			subAddress = subAddress.substring(0, subAddress.length() - 1);
		}

		if (subAddress.endsWith(SUFFIX_LINK))
			result.add(new NodeAddress(subAddress));

		return result;
	}

	public boolean isLocallyUnfixed() {
		return address.endsWith(SUFFIX_LOCAL_UNFIXED);
	}

	public boolean isStarUnfixed() {
		return address.endsWith(SUFFIX_STAR);
	}

	/**
	 * @param other
	 * @param list
	 *            of modal operators
	 * @return can we get from this node to other via ops? this now also expands /\* and \/* if the node we are
	 *         currently on when reaching the /\*, is fixed, or if there are fixed nodes below \/*. This currently
	 *         disregards directionality. Not yet implemented the expansion of \/U nor /\U. This probably will not be
	 *         needed.
	 */

	private boolean to(Tree t, NodeAddress other, List<BasicOperator> ops) {

		if (ops.isEmpty()) {
			// logger.debug("Addresses equal, returning true from 'to' method");
			return address.equals(other.address);
		}
		List<BasicOperator> opsLeft = new ArrayList<BasicOperator>(ops);
		BasicOperator op = opsLeft.remove(0);
		if (op.isUp()) {
			if (op.isFixed()) {
				NodeAddress n = go(op);
				return (n != null) && n.to(t, other, opsLeft);
			} else {
				if (!this.isLocallyFixed()) {
					NodeAddress n;
					if (op.isStar()) {
						if (this.isLocallyUnfixed())
							n = go(BasicOperator.UP_LOCAL_UNFIXED);
						else
							n = go(op);// op is /\*
					} else
						n = go(op);// op is /\U

					return (n != null) && n.to(t, other, opsLeft);

				} else {
					ArrayList<NodeAddress> nodesHereAndAbove = goUpStarExpand();
					// logger.debug("upStarExpand returned"+nodesHereAndAbove.size()+
					// "nodes");
					for (NodeAddress n : nodesHereAndAbove) {
						if (n.to(t, other, opsLeft))
							return true;

					}
					return false;

				}

			}
		} else if (!op.getPath().isEmpty()) {
			NodeAddress n = go(op);
			if (op.isFixed() || t.containsKey(n)) {

				return (n != null) && n.to(t, other, opsLeft);
			} else if (op.isStar() && t.containsKey(go(BasicOperator.DOWN_LOCAL_UNFIXED))) {

				n = go(BasicOperator.DOWN_LOCAL_UNFIXED);
				return (n != null) && n.to(t, other, opsLeft);

			} else

			{
				// if we are here, op is not fixed, go(op) is not on the tree
				// op is either \/U or \/* and none of these addresses have been
				// throw exception if there are any more ops to the right of these
				// in this situation
				ArrayList<NodeAddress> nodesBelow;
				if (op.isU())
					nodesBelow = goDownLocalUnfixedExpand(t);
				else
					nodesBelow = goDownStarExpand(t);

				if (!opsLeft.isEmpty())
					throw new UnsupportedOperationException();
				for (NodeAddress na : nodesBelow) {
					if (na.equals(other.address))
						return true;

				}
				return false;

			}

		} else {
			throw new RuntimeException("not implemented yet: checking down operators with empty path");
			// NodeAddress n = other.go(op.inverse());
			// return (n != null) && to(n, opsLeft);
		}
	}

	private ArrayList<NodeAddress> goDownStarExpand(Tree t) {
		ArrayList<NodeAddress> result = new ArrayList<NodeAddress>();
		for (NodeAddress na : t.keySet()) {
			if (na.getAddress().startsWith(this.address)) {
				String rest = "";
				if (na.getAddress().length() > this.address.length()) {
					rest = na.getAddress().substring(this.address.length(), na.getAddress().length());

				} else
					continue;

				if (!rest.contains(SUFFIX_LINK)) {
					result.add(na);
				}

			}
		}
		return result;
	}

	private ArrayList<NodeAddress> goDownLocalUnfixedExpand(Tree t) {
		ArrayList<NodeAddress> result = new ArrayList<NodeAddress>();
		for (NodeAddress na : t.keySet()) {
			if (na.getAddress().startsWith(this.address)) {
				String rest = "";
				if (na.getAddress().length() > this.address.length()) {
					rest = na.getAddress().substring(this.address.length(), na.getAddress().length());

				} else
					continue;

				if (!rest.contains(SUFFIX_LINK) && rest.endsWith(SUFFIX_0)) {
					result.add(na);
				}

			}
		}
		return result;
	}

	/**
	 * @param op
	 * @return the address we arrive at by going op from this address
	 */
	public NodeAddress go(BasicOperator op) {
		if (op.isDown()) {
			if (op.getPath().isEmpty()) {
				throw new RuntimeException("must specify down path");
			}
			return down(op.getPath());
		}
		if (op.isUp()) {
			if (op.getPath().isEmpty()) {
				return up();
			}
			return up(op.getPath());
		}
		return null;
	}

	/**
	 * @param modality
	 * @return the address we arrive at by going modality from this address
	 */
	public NodeAddress go(Modality modality) {
		NodeAddress na = this;
		for (BasicOperator op : modality.getOps()) {
			na = na.go(op);
		}
		return na;
	}

	/**
	 * @param other
	 * @return true if this {@link NodeAddress} subsumes other i.e. is equal to it or can be made equal to it by
	 *         instantiating any unfixed Kleene star portions of the path
	 */
	public boolean subsumes(NodeAddress other) {
		return other.address.matches(unfixedRegex());
	}

	/**
	 * @return a regular expression representing this address, where Kleene stars can match any [01] sequence (i.e.
	 *         restricting to same subtree - LINK relations will not match)
	 */
	private String unfixedRegex() {

		String result = address.replaceAll(Pattern.quote(SUFFIX_STAR), "([01]+|\\\\*)");
		result = result.replaceAll(Pattern.quote(SUFFIX_LOCAL_UNFIXED), "(1*0|U)");
		return result;
	}

	/**
	 * @param other
	 * @return a {@link List} of {@link BasicOperator}s which will take us from this {@link NodeAddress} to other;
	 *         treating all operators as internal
	 */
	public List<BasicOperator> pathTo(NodeAddress other) {
		String addr = address;
		ArrayList<BasicOperator> ops = new ArrayList<BasicOperator>();
		while (!other.address.startsWith(addr)) {
			String last = addr.substring(addr.length() - 1, addr.length());
			ops.add(new BasicOperator(BasicOperator.ARROW_UP, last));
			addr = addr.substring(0, addr.length() - 1);
		}
		while (!other.address.equals(addr)) {
			String next = other.address.substring(addr.length(), addr.length() + 1);
			ops.add(new BasicOperator(BasicOperator.ARROW_DOWN, next));
			addr += next;
		}
		return ops;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	
	public int compareTo(NodeAddress other) {
		return address.compareTo(other.address);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeAddress other = (NodeAddress) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return address;
	}

	public NodeAddress instantiate() {
		return this;
	}

	public static void main(String a[]) {
		NodeAddress ad1 = new NodeAddress("0U");
		NodeAddress ad2 = new NodeAddress("0110");
		System.out.println(ad1.subsumes(ad2));

	}

}
