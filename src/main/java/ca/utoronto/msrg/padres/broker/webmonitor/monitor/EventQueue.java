package ca.utoronto.msrg.padres.broker.webmonitor.monitor;

import java.util.LinkedList;
import java.util.List;

public class EventQueue {
	private List<MonitorEvent> queue;
	
	public EventQueue() {
		queue = new LinkedList<MonitorEvent>();
	}
	
	public synchronized void put(MonitorEvent event) {
//		System.out.println("put: " + event);
		queue.add(event);
		notify();
	}
	
	public synchronized MonitorEvent blockingGet() {
		while (queue.size() == 0) { 
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		MonitorEvent event = queue.remove(0);
		return event;
	}
	
	public synchronized MonitorEvent get() {
		if (queue.size() > 0) {
			MonitorEvent event = queue.remove(0);
			return event;
		}
		return null;
	}
}
