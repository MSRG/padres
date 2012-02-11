package ca.utoronto.msrg.padres.broker.topk;

import java.util.ArrayList;
import java.util.List;

import ca.utoronto.msrg.padres.common.message.PublicationMessage;

public class Chunk {
	
	private String chunkId;
	private String subscriptionId;
	private int count; 
	private int subCount; // only need at the source broker
	private List<PublicationMessage> list;
	
	public Chunk(int subCount, String subscriptionId){
		this.chunkId = subscriptionId + "-" + subCount;
		this.subCount = subCount;
		this.subscriptionId = subscriptionId;
		this.count = 0;
		this.list = null;
	}
	
	public Chunk(String chunkId, String subscriptionId){
		this.chunkId = chunkId;
		this.subCount = -1; // not necessary at the edge broker
		this.subscriptionId = subscriptionId;
		this.count = 0;
		this.list = null;
	}

	public String getChunkId() {
		return chunkId;
	}

	public void setChunkId(String chunkId) {
		this.chunkId = chunkId;
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public int getCount() {
		return count;
	}

	public int getSubCount() {
		return subCount;
	}

	public void incrementCount() {
		this.count++;		
	}

	public void addPublication(PublicationMessage pub) {
		if(list == null)
			list = new ArrayList<PublicationMessage>();
		
		list.add(pub);
	}

	public List<PublicationMessage> getPublications() {
		return list;
	}
	
	
}
