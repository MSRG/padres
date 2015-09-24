package ca.utoronto.msrg.padres.broker.topk.count;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import ca.utoronto.msrg.padres.broker.router.Router;
import ca.utoronto.msrg.padres.broker.topk.RandomComparator;
import ca.utoronto.msrg.padres.broker.topk.TopkEngine;
import ca.utoronto.msrg.padres.broker.topk.TopkInfo;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;

public abstract class CountTopkEngine extends TopkEngine {

	protected Router router;
	protected TopkInfo info;
	protected HashMap<String, Vector<PublicationMessage>> events;
	
	public CountTopkEngine(Router router, TopkInfo info) {
		this.router = router;
		this.info = info;
		this.events = new HashMap<String, Vector<PublicationMessage>>();
	}


	@Override
	protected Collection<PublicationMessage> sortAndRemove(
			Vector<PublicationMessage> events, int start, int seed) {
		
		int k = info.getTopKSize();
		int W = info.getWindowSize();
		int shift = info.getShift();

		List<PublicationMessage> list = new Vector<PublicationMessage>(events.subList(start, W));
		events.removeAll(list.subList(0, shift));
		
		Collections.sort(list, new RandomComparator(seed));
				
		return new Vector<PublicationMessage>(list.subList(0, k));
	}
}
