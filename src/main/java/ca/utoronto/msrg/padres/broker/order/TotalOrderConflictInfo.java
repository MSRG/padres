package ca.utoronto.msrg.padres.broker.order;

import java.io.Serializable;

public class TotalOrderConflictInfo implements Serializable {

	private static final long serialVersionUID = 3300931811866950277L;
	
	private String pubId;
//	private Set<String> adIds;
//	private Set<String> subIds;
//	private Set<String> waitSubs;
	private boolean wait;
	private boolean request;
	private boolean ack;
	
	public TotalOrderConflictInfo(){
	}
	
	public TotalOrderConflictInfo(String pubId){
		this.pubId = pubId;
//		this.adIds = new HashSet<String>();
//		this.subIds = new HashSet<String>();
//		this.waitSubs = null;
		this.wait = false;
		this.request = false;
		this.ack = false;
	}
	
	public TotalOrderConflictInfo(TotalOrderConflict conflict){
		this.pubId = conflict.getPubId();
		this.wait = false;
		this.request = false;
		this.ack = false;
	}

	public String getPubId() {
		return pubId;
	}

//	public Set<String> getAdIds() {
//		return adIds;
//	}
//
//	public void addAdId(String adId){
//		this.adIds.add(adId);
//	}
//	
//	public void addSubId(String subId){
//		this.subIds.add(subId);
//	}
//	
//	public void addAdIds(Set<String> adIds){
//		this.adIds.addAll(adIds);
//	}
//	
//	public void addSubIds(Set<String> subIds){
//		this.subIds.addAll(subIds);
//	}
//	
//	public void removeAdId(String adId) {
//		this.adIds.remove(adId);
//	}
//
//	public Set<String> getSubIds() {
//		return subIds;
//	}
//
//	public void removeSubId(String subId) {
//		this.subIds.remove(subId);
//	}

	public boolean isWait() {
		return wait;
	}

	public void setWait(boolean wait) {
		this.wait = wait;
	}

	public boolean isRequest() {
		return request;
	}

	public void setRequest(boolean request) {
		this.request = request;
	}

//	public void setAdIds(Set<String> adIds) {
//		this.adIds = adIds;
//	}
//
//	public void setSubIds(Set<String> subIds) {
//		this.subIds = subIds;
//	}
	
	public void clear(){
//		this.adIds = null;
//		this.subIds = null;
	}

	public void setAck(boolean ack) {
		this.ack = ack;
	}
	
	public boolean isAck(){
		return ack;
	}

	public void empty() {
//		this.adIds.clear();
//		this.subIds.clear();		
	}

//	public Set<String> getWaitSubs() {
//		return waitSubs;
//	}
//
//	public void addWaitSubs(String waitSub) {
//		if(waitSubs == null)
//			waitSubs = new HashSet<String>();
//		this.waitSubs.add(waitSub);
//	}

	public TotalOrderConflictInfo duplicate() {
		TotalOrderConflictInfo newInfo = new TotalOrderConflictInfo();
		newInfo.pubId = this.pubId;
//		if(this.adIds != null)
//			newInfo.adIds = new HashSet<String>(this.adIds);
//		else
//			newInfo.adIds = null;
//		if(this.subIds != null)
//			newInfo.subIds = new HashSet<String>(this.subIds);
//		else
//			newInfo.subIds = null;
//		if(this.waitSubs != null)
//			newInfo.waitSubs = new HashSet<String>(this.waitSubs);
//		else
//			newInfo.waitSubs = null;
		newInfo.wait = this.wait;
		newInfo.request = this.request;
		newInfo.ack = this.ack;
		return newInfo;
	}
}
