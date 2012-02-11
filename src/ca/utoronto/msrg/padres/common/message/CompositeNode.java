package ca.utoronto.msrg.padres.common.message;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

//import message.CompositeSubscriptionOPs;
//import message.Subscription;

public class CompositeNode implements Serializable {

	private static final long serialVersionUID = -6230191778613144247L;

	public Set<String> destinations;

	public String content;

	public CompositeNode parentNode;

	public CompositeNode leftNode;

	public CompositeNode rightNode;

	public CompositeNode(String root, CompositeNode ln, CompositeNode rn, CompositeNode pn) {
		content = root;
		leftNode = ln;
		rightNode = rn;
		parentNode = pn;
	}

	public CompositeNode() {
		destinations = new HashSet<String>();
		content = null;
		leftNode = null;
		rightNode = null;
		parentNode = null;
	}

	public void setContent(String root) {
		content = root;
	}

	public void setLeftNode(CompositeNode ln) {
		leftNode = ln;
	}

	public void setRightNode(CompositeNode rn) {
		rightNode = rn;
	}

	public void setParentNode(CompositeNode pn) {
		parentNode = pn;
	}

	public void addDestinations(String lastHopID) {
		destinations.add(lastHopID);

	}

	public CompositeNode duplicate() {
		CompositeNode newCompositeNode = new CompositeNode();
		newCompositeNode.destinations.addAll(this.destinations);
		newCompositeNode.content = this.content;
		newCompositeNode.parentNode = this.parentNode.duplicate();
		newCompositeNode.leftNode = this.leftNode.duplicate();
		newCompositeNode.rightNode = this.rightNode.duplicate();
		return newCompositeNode;
	}

	public String toString() {
		String str = "";
		str = this.content;
		return str;
	}

}