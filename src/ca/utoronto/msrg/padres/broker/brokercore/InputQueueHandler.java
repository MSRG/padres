// =============================================================================
// This file is part of The PADRES Project.
//
// For more information, see http://www.msrg.utoronto.ca
//
// Copyright (c) 2003 Middleware Systems Research Group, University of Toronto
// =============================================================================
// $Id$
// =============================================================================
/*
 * Created on 29-Jul-2003
 *
 */
package ca.utoronto.msrg.padres.broker.brokercore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.monitor.SystemMonitor;
import ca.utoronto.msrg.padres.broker.router.Router;
import ca.utoronto.msrg.padres.common.comm.QueueHandler;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageComparator;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

/**
 * The handler for the broker input queue. Message are retrieved from the input queue and sent to
 * the matching engine.
 * 
 * @author eli
 */
public class InputQueueHandler extends QueueHandler {

	protected BrokerCore brokerCore;

	protected Router router;

	protected SystemMonitor systemMonitor;

	// add by shuang, for testing
	protected Set<Message> messagesToRoute;

	protected Map<MessageDestination, Integer> msgRate = new HashMap<MessageDestination, Integer>();

	protected static Logger messagePathLogger = Logger.getLogger("MessagePath");

	/**
	 * 
	 */
	public InputQueueHandler(BrokerCore broker) {
		super(MessageDestination.INPUTQUEUE);
		brokerCore = broker;
		router = broker.getRouter();

		// issue initial Advertisement for BROKER_CONTROL messages
		Advertisement initialAdv;
		try {
			initialAdv = MessageFactory.createAdvertisementFromString(
					"[class,eq,'BROKER_CONTROL'],[brokerID,isPresent,''],[command,str-contains,'-'],[broker,isPresent,''],[fromID,isPresent,''],[fromURI,isPresent,'']");
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
			return;
		}
		AdvertisementMessage initialAdvMsg = new AdvertisementMessage(initialAdv,
				brokerCore.getNewMessageID());
		initialAdvMsg.setTTL(1);
		messagePathLogger.debug("InputQueueHandler is sending initial advertisement : "
				+ initialAdvMsg.toString() + " to the matchingEngine.");
		router.handleMessage(initialAdvMsg);
		messagesToRoute = new TreeSet<Message>(new MessageComparator());
	}

	public void registerSystemMonitor(SystemMonitor systemMonitor) {
		this.systemMonitor = systemMonitor;
	}

	public void processMessage(Message msg) {
		// NOTE: do the processing through router first before clearing the messagesToRoute data
		// structure. It is necessary for test cases.
		Set<Message> routedMessages = router.handleMessage(msg);
		messagesToRoute.clear();
		messagesToRoute.addAll(routedMessages);

		if (brokerCore.isCycle()) {
			if (msg.getLastHopID().isBroker() && (!msg.getLastHopID().isInternalQueue())) {
				if (msgRate.containsKey(msg.getLastHopID())) {
					Integer tempI = (Integer) msgRate.get(msg.getLastHopID());
					Integer newI = new Integer(tempI.intValue() + 1);
					msgRate.put(msg.getLastHopID(), newI);
				} else {
					Integer msgNum = new Integer(1);
					msgRate.put(msg.getLastHopID(), msgNum);
				}
			}
		}
		// Must place this here to get accurate message timing above
		messagePathLogger.info(brokerCore.getBrokerID() + " got message from INPUTQUEUE:" + msg);

		// for testing, add by Shuang Hou
		// sometimes, there are more than one message routed, e.g., for composite subscription, and
		// for routing stuff
		boolean first = true;
		for (Message msgToRoute : messagesToRoute) {
			// Update TTL and only forward message if TTL is still valid
			if (!msgToRoute.updateTTLAndExpired()) {
				msgToRoute.setLastHopID(brokerCore.getBrokerDestination());
				messagePathLogger.info(brokerCore.getBrokerID() + " is sending message: "
						+ msgToRoute);

				if (brokerCore.isCycle()) {
					if (msgToRoute.getNextHopID().isBroker()
							&& (!msg.getNextHopID().isInternalQueue())) {
						if (msgRate.containsKey(msgToRoute.getNextHopID())) {
							Integer tempI = (Integer) msgRate.get(msgToRoute.getNextHopID());
							Integer newI = new Integer(tempI.intValue() + 1);
							msgRate.put(msgToRoute.getNextHopID(), newI);
						} else {
							Integer msgNum = new Integer(1);
							msgRate.put(msgToRoute.getNextHopID(), msgNum);
						}
					}
				}

				if (msgToRoute.getClass() == PublicationMessage.class) {
					// This message need to be traced If the original msg is sent from client to
					// broker, then the first message in the set of messages will generate two
					// trace route messages. One is for itself, another is for the original
					// message.
					systemMonitor.notifyOfTracerouteMessage(msgToRoute, first);
					if (first)
						first = false;
				}

				// TODO: hack here to handle BROKER_INFO, HEARTBEAT_MANAGER and
				// TRACEROUTE_MESSAGE message when the broker is stopped.
				boolean flag = true;
				String toBrokerID = "";
				if (!brokerCore.isRunning()) {
					if (msgToRoute.getClass() == PublicationMessage.class) {
						String msgClass = ((PublicationMessage) msgToRoute).getPublication().getClassVal();
						toBrokerID = ((PublicationMessage) msgToRoute).getNextHopID().getDestinationID();
						if (msgClass.equals("BROKER_INFO")) {
							String brokerID = (String) ((PublicationMessage) msgToRoute).getPublication().getPairMap().get(
									"brokerID");
							if (!brokerID.equals(brokerCore.getBrokerID()))
								flag = false;
						} else if (msgClass.equals("TRACEROUTE_MESSAGE")) {
							flag = false;
						} else if (msgClass.equals("HEARTBEAT_MANAGER")) {
							Map<String, Serializable> temp = ((PublicationMessage) msgToRoute).getPublication().getPairMap();
							if (temp.containsKey("fromID")) {
								String fromID = (String) temp.get("fromID");
								if (!fromID.equals(brokerCore.getBrokerID()))
									flag = false;
							} else if (temp.containsKey("detectorID")) {
								String detectorID = (String) temp.get("detectorID");
								if (!detectorID.equals(brokerCore.getBrokerID()))
									flag = false;
							}
						}
					} else if (msgToRoute.getClass() == SubscriptionMessage.class) {
						String msgClass = ((SubscriptionMessage) msgToRoute).getSubscription().getClassVal();
						toBrokerID = ((SubscriptionMessage) msgToRoute).getNextHopID().getDestinationID();
						if (msgClass.equals("BROKER_INFO")) {
							flag = false;
						} else if (msgClass.equals("TRACEROUTE_MESSAGE")) {
							flag = false;
						}
					}

					if (!flag) {
						if (brokerCore.getOverlayManager().getORT().isNeighbor(toBrokerID)) {
							brokerCore.routeMessage(msgToRoute);
						} else {
							flag = true;
						}
					}
				}

				if (flag) {
					brokerCore.routeMessage(msgToRoute);
				}
			}
		}
	}

	/**
	 * @return
	 */
	public Router getRouter() {
		return router;
	}

	public boolean containsDest(MessageDestination md) {
		return (msgRate.containsKey(md));
	}

	public Integer getNum(MessageDestination md) {
		return ((Integer) msgRate.get(md));
	}

	public void setNum(MessageDestination md, Integer in) {
		msgRate.put(md, in);
	}

	public Set<Message> getCurrentMessagesToRoute() {
		return messagesToRoute;
	}
}
