package ca.utoronto.msrg.padres.demo.webclient.client;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/*
 * Holds events occurring on the client until the browser requests them
 */
public class EventQueue {
	private List<ClientEvent> queue;
	
	public EventQueue() {
		queue = new LinkedList<ClientEvent>();
	}
	
	public synchronized void put(ClientEvent event) {
//		System.out.println("put: " + event);
		queue.add(event);
		notify();
	}
	
	public synchronized ClientEvent blockingGet() {
		while (queue.size() == 0) { 
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		ClientEvent event = queue.remove(0);
//		System.out.println("get: " + event);
		return event;
	}
	
	public synchronized ClientEvent get() {
		if (queue.size() > 0) {
			ClientEvent event = queue.remove(0);
			return event;
		}
		return null;
	}
	
	/*
	 * TODO: support other clearing methods
	 */
	public synchronized void clearByIDPrefix(String idPrefix) {
		for (Iterator<ClientEvent> iter=queue.iterator(); iter.hasNext();) {
			ClientEvent event = iter.next();
			if (event.getId().startsWith(idPrefix))
				iter.remove();
		}
	}
}
