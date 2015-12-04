package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import java.util.HashSet;
import java.util.Set;

import ca.utoronto.msrg.padres.common.message.*;

public class PassObject {
	private Publication pub;
	private Advertisement adv;
	private Set<MemoryUnit> memUnits = null;
	

	
	public PassObject(){
		memUnits = new HashSet<MemoryUnit>();
		pub = null;
		adv = null;
	}
	
	public void addMemUnit(MemoryUnit mu){
		
		memUnits.add(mu);
	}
	
	public void addMemUnits(Set<MemoryUnit> sets){
		
		memUnits.addAll(sets);
	}
	
	public void addPub(Publication p){
		pub = p;
	}
	
	public void addAdv(Advertisement a){
		adv = a;
	}
	
	public boolean isPublication(){
		if (pub != null) {
			return true;
		}else{
			return false;
		}
	}
	
	public boolean isAdvertisement(){
		if (adv != null) {
			return true;
		}else{
			return false;
		}
	}
	
	public boolean isMemUnits(){
		if (memUnits != null) {
			return true;
		}else{
			return false;
		}
	}
}
