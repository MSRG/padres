package ca.utoronto.msrg.padres.broker.topk;

import java.util.Comparator;
import java.util.Random;

import ca.utoronto.msrg.padres.common.message.PublicationMessage;

public class RandomComparator implements Comparator<PublicationMessage> {

	private int seed;
	
	public RandomComparator(int seed){
		this.seed = seed;
	}
	
	@Override
	public int compare(PublicationMessage p1, PublicationMessage p2) {
		Random gen = new Random(seed+p1.getMessageID().hashCode());
		
		int s1 = gen.nextInt();
		
		gen.setSeed(seed+p2.getMessageID().hashCode());
		
		int s2 = gen.nextInt();
		
		if(s1 == s2)
			return 0;
		else
			return s1 < s2 ? -1 : 1;
	}	
}
