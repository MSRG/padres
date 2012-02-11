/*
 * This demo client class can be created in any package as long as the server
 * is able to locate the class
 */
package ca.utoronto.msrg.padres.demo.webclient.demo.examples;

import java.util.Properties;

import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;
import ca.utoronto.msrg.padres.demo.webclient.client.ClientEvent;
import ca.utoronto.msrg.padres.demo.webclient.client.WebClientException;
import ca.utoronto.msrg.padres.demo.webclient.client.EventQueue;
import ca.utoronto.msrg.padres.demo.webclient.client.WebUIClient;

/*
 * Example of how to write Java code to support custom demos
 */
public class ExampleDemo {

	private WebUIClient client;

	public ExampleDemo() throws ClientException {
		client = WebUIClient.getClient();
	}

	/*
	 * Ignore properties. The entire demo is self-contained in this method.
	 */
	public Properties demo1(Properties props) throws WebClientException, ClientException {
		NodeAddress bid;
		try {
			bid = client.handleConnect("socket://localhost:1098/Broker1");
			client.handleConnect("localhost:1097");
			client.handleConnect("localhost:1096");
			client.handleConnect("localhost:1095");
		} catch (ClientException ex) {
			bid = client.handleConnect("localhost:1099");
		}

		// use the "handle*" methods if the UI should be
		// aware of the operations being performed
		for (int ii = 0; ii < 5; ii++) {
			String adv = "[class,eq,adv" + ii + "]";
			client.handleAdvertise(adv, bid.getNodeURI());
			String sub = "[class,eq,sub" + ii + "]";
			client.handleSubscribe(sub, bid.getNodeURI());
		}

		// TODO: Allow public access to these methods to hide messages from UI?
		// client.advertise(new Advertisement("[class,eq,invisible_adv]"), bid);
		// client.subscribe(new Subscription("[class,eq,invisible_sub]"), bid);

		return props;
	}

	/*
	 * Get some properties sent from the UI and use them to generate messages
	 */
	public Properties demo2(Properties props) throws WebClientException, ClientException {
		String broker = props.getProperty("broker", null);
		String adv = props.getProperty("adv", null);
		String sub = props.getProperty("sub", null);
		String pub1 = props.getProperty("pub1", null);
		String pub2 = props.getProperty("pub2", null);
		int delay = Integer.parseInt(props.getProperty("delay", "0"));

		// Exception text will be given to the error handler
		// function inside xmlHttp.responseText as plain text
		if (broker == null || sub == null || pub1 == null || pub2 == null)
			throw new WebClientException("Missing argument(s) to Demo #2");

		// run the demo
		NodeAddress bid = client.handleConnect(broker);
		client.handleAdvertise(adv, bid.getNodeURI());
		client.handleSubscribe(sub, bid.getNodeURI());
		client.handlePublish(pub1, bid.getNodeURI());
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		client.handlePublish(pub2, bid.getNodeURI());

		// create a new Properties object to avoid resending
		// input arguments back to client (unless that is the intention)
		props = new Properties();
		props.setProperty("example_reply", "Demo #2 Success");
		return props;
	}

	/*
	 * Streaming publications
	 */
	private static PubStream stream = null;

	public Properties startDemo3(Properties props) throws WebClientException, ClientException {
		if (stream != null)
			throw new WebClientException("Demo 3 Already Started");

		int delay = 0;
		try {
			delay = Integer.parseInt(props.getProperty("delay", String.valueOf(1000)));
		} catch (NumberFormatException ex) {
			throw new WebClientException("\"delay\" must be an integer value.");
		}

		String addr = props.getProperty("broker", "rmi://localhost:1099/Broker1");
		NodeAddress bid = client.handleConnect(addr);
		client.handleAdvertise("[class,eq,Demo3],[counter,>=,0]", bid.getNodeURI());
		stream = new PubStream(delay, bid.getNodeURI());
		stream.start();

		return new Properties();
	}

	public Properties togglePauseDemo3(Properties props) throws WebClientException {
		props = new Properties();
		if (stream != null) {
			if (stream.isPaused()) {
				stream.unpause();
			} else {
				stream.pause();
			}
			props.setProperty("paused", stream.isPaused() ? "true" : "false");
		} else {
			throw new WebClientException("Demo3 has not been started.");
		}
		return props;
	}

	public Properties stopDemo3(Properties props) throws WebClientException {
		if (stream != null) {
			stream.stopStream();
			stream = null;
		} else {
			throw new WebClientException("Demo3 has not been started.");
		}
		return new Properties();
	}

	// TODO: Write method to return paused status - in case user refreshes page

	/*
	 * Helper class for streaming
	 */
	private class PubStream extends Thread {

		private static final String DEMO3_PREFIX = "demo3-ex-";

		private int delay;

		private String bid;

		public PubStream(int delay, String bid) {
			this.delay = delay;
			this.bid = bid;
		}

		private boolean isPaused = false;

		private boolean stopStream = false;

		public void run() {
			long startTime = System.currentTimeMillis();
			int counter = 0;
			while (!stopStream) {
				try {
					sleep(Math.max(0, (startTime + delay) - System.currentTimeMillis()));
					startTime = System.currentTimeMillis();
					// NOTE: This is just an example and yield() is not a good way to control
					// threads.
					// See StreamingCustomerDemo for non-resource intensive thread control.
					synchronized (this) {
						while (isPaused) {
							wait();
						}
					}
					if (!stopStream)
						client.handlePublish("[class,Demo3],[counter," + (counter++) + "]", bid);
				} catch (Exception e) {
					// push exception event to UI
					EventQueue q = client.getEventQueue("demo3_qid");
					if (q != null)
						q.put(new ClientEvent(ClientEvent.TYPE_EXCEPTION, DEMO3_PREFIX + counter,
								"Demo3: " + e.getMessage()));
				}
			}
		}

		public synchronized void stopStream() {
			stopStream = true;
			// clear publication events from this streaming client
			EventQueue q = client.getEventQueue("#default_webui");
			q.clearByIDPrefix(DEMO3_PREFIX);
			notifyAll();
		}

		public synchronized boolean isPaused() {
			return isPaused;
		}

		public synchronized void pause() {
			isPaused = true;
		}

		public synchronized void unpause() {
			isPaused = false;
			notifyAll();
		}
	}
}
