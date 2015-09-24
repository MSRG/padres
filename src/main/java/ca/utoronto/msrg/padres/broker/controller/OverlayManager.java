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
 * Created on 21-Jul-2003
 *
 */
package ca.utoronto.msrg.padres.broker.controller;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.ConnectionListenerInterface;
import ca.utoronto.msrg.padres.common.comm.MessageSender;
import ca.utoronto.msrg.padres.common.comm.OutputQueue;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageDestination.DestinationType;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;

/**
 * The Manager to handle overlay network connection/disconnection.
 * 
 * @author eli
 */
public class OverlayManager implements Manager, ConnectionListenerInterface {

	public static final String MANAGER_NAME = "OVERLAY";

	protected BrokerCore brokerCore;

	private final Set<String> connecting;

	private OverlayRoutingTable routingTable;
	
	static Logger overlayLogger = Logger.getLogger(OverlayManager.class);

	static Logger brokerCoreLogger = Logger.getLogger(BrokerCore.class);

	static Logger exceptionLogger = Logger.getLogger("Exception");

	static Logger messagePathLogger = Logger.getLogger("MessagePath");

	/**
	 * Constructor. Set up singleton references.
	 * 
	 * @throws BrokerCoreException
	 */
	public OverlayManager(BrokerCore broker) throws BrokerCoreException {
		brokerCore = broker;
		connecting = new HashSet<String>();
		routingTable = new OverlayRoutingTable();
		try {
			MessageSender.setConnectRetryLimit(brokerCore.getBrokerConfig().getConnectionRetryLimit());
			MessageSender.setConnectRetryPauseTime(brokerCore.getBrokerConfig().getConnectionRetryPause());
			broker.getCommSystem().addConnectionListener(broker.getBrokerURI(), this);
			overlayLogger.info("OverlayManager is fully started.");
			brokerCoreLogger.info("OverlayManager is fully started.");
		} catch (CommunicationException e) {
			throw new BrokerCoreException("Error instantiating OverlayManager: " + e.getMessage(),
					e);
		}
	}

	public OverlayRoutingTable getORT() {
		return routingTable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see controller.Manager#handleCommand(java.util.Map)
	 */
	public void handleCommand(Map<String, Serializable> pairs, Serializable payload) {
		String command = (String) pairs.get("command");
		command = command.substring(command.indexOf('-') + 1);
		overlayLogger.debug("OverlayManager receives command : " + command + ".");
		if (command.equalsIgnoreCase("CONNECT")) {
			// got connection request from admin, initiate connection request to neighbour
			String toBrokerURI = (String) pairs.get("broker");
			if (overlayLogger.isDebugEnabled())
				overlayLogger.debug("The broker got CONNECT command from admin, and try to connect the broker "
						+ toBrokerURI + ".");
			if (toBrokerURI.equals("")) {
				overlayLogger.error("The broker " + brokerCore.getBrokerID()
						+ " tries to connect to null remote broker.");
				exceptionLogger.error(new Exception("The broker " + brokerCore.getBrokerID()
						+ " tries to connect to null remote broker."));
			} else {
				// added by Shuang, for the scenario that the broker tries to connect itself
				try {
					if(checkRemoteBrokerURItoConnect(toBrokerURI)) {
						try {
							// create an output queue for the neighbour and start it
							overlayLogger.debug("Broker " + brokerCore.getBrokerID()
									+ " is creating MessageSender for broker " + toBrokerURI);
							MessageSender msgSender = createMessageSenderAndConnect(toBrokerURI);
							String toBrokerID = msgSender.getID();
							// add to the overlay routing table
							overlayLogger.info("Adding broker " + toBrokerID
									+ " as new neighbour into ORT.");
							MessageDestination remoteDest = new MessageDestination(toBrokerID,
									DestinationType.BROKER);
							OutputQueue remoteOutputQueue = createOutputQueue(remoteDest, msgSender);
							routingTable.addBroker(remoteOutputQueue);
							brokerCore.registerQueue(remoteOutputQueue);
							remoteOutputQueue.start();
							connecting.add(toBrokerURI);
							// send CONNECT_REQ
							sendConnectRequest(toBrokerID, remoteDest);
							brokerCoreLogger.info(String.format(
									"Connection to remote broker %s(%s) is complete", toBrokerID,
									toBrokerURI));
						} catch (CommunicationException e) {
							overlayLogger.error("Could not connect to " + toBrokerURI);
							exceptionLogger.error(e);
						}
					}
				} catch (CommunicationException e) {
					overlayLogger.error(e.getMessage());
					exceptionLogger.error(e);
				}
			}
		} else if (command.equalsIgnoreCase("CONNECT_REQ")) {
			// got connection request from neighbour, reply and initiate connection request along
			// opposite channel if necessary
			String remoteBrokerID = (String) pairs.get("fromID");
			MessageDestination remoteDest = new MessageDestination(remoteBrokerID,
					DestinationType.BROKER);
			String remoteBrokerURI = (String) pairs.get("fromURI");
			overlayLogger.debug("The broker got CONNECT_REQ from broker " + remoteBrokerID + " ("
					+ remoteBrokerURI + ").");
			if (!connecting.contains(remoteBrokerURI) && !routingTable.isNeighbor(remoteDest)) {
				// get communication object for sending messages
				try {
					overlayLogger.debug("Broker " + brokerCore.getBrokerID()
							+ " is creating MessageSender for broker " + remoteBrokerURI);
					MessageSender msgSender = createMessageSenderAndConnect(remoteBrokerID);
					String remoteID = msgSender.getID();
					if (!remoteID.equals(remoteBrokerID))
						overlayLogger.error("Remote broker ID " + remoteBrokerID
								+ " did not match the ID given by the remote server " + remoteID);
					// add to the overlay routing table
					overlayLogger.info("Adding broker " + remoteID + " as new neighbour into ORT.");
					OutputQueue remoteOutputQueue = createOutputQueue(remoteDest, msgSender);
					routingTable.addBroker(remoteOutputQueue);
					brokerCore.registerQueue(remoteOutputQueue);
					remoteOutputQueue.start();
					connecting.add(remoteBrokerURI);
					// send its own CONNECT_REQ
					sendConnectRequest(remoteBrokerID, remoteDest);
					brokerCoreLogger.info(String.format(
							"Connection to remote broker %s(%s) is complete", remoteBrokerID,
							remoteBrokerURI));
				} catch (CommunicationException e) {
					overlayLogger.error("Could not connect to " + remoteBrokerID);
					exceptionLogger.error(e);
				}
			}
			if (routingTable.isNeighbor(remoteDest)) {
				// send OVERLAY-CONNECT_ACK
				try {
					sendConnectRequestAck(remoteBrokerID, remoteDest);
				} catch (CommunicationException e) {
					overlayLogger.error("Error in send REQUEST-ACK to " + remoteBrokerID);
					exceptionLogger.error(e);
				}
			}
		} else if (command.equalsIgnoreCase("CONNECT_ACK")) {
			String fromURI = (String) pairs.get("fromURI");
			overlayLogger.debug("The broker got CONNECT_ACK from broker " + fromURI + ".");
			connecting.remove(fromURI);
			// forward all the currently maintained advertisements to the new neighbour
			forwardAllAdvToNeighbor((String) pairs.get("fromID"));
		} else if (command.equalsIgnoreCase("DISCONNECT")) {
			// TODO: disconnect from overlay network
			// bindings.remove(....);
		} else if (command.equalsIgnoreCase("STOP")) {
			stopOutputQueues();
		} else if (command.equalsIgnoreCase("RESUME")) {
			resumeOutputQueues();
		} else if (command.equalsIgnoreCase("SHUTDOWN")) {
			shutdownOutputQueues();
//			connecting = null;
			overlayLogger.info("MessageSenders for broker " + brokerCore.getBrokerID()
					+ " are shut down.");
			brokerCore = null;
		} else if (command.equalsIgnoreCase("SHUTDOWN_REMOTEBROKER")) {
			String fromID = (String) pairs.get("fromID");
			MessageDestination remoteDest = new MessageDestination(fromID, DestinationType.BROKER);
			if (routingTable.isNeighbor(remoteDest)) {
				routingTable.removeBroker(remoteDest);
				brokerCore.removeQueue(remoteDest);
			}
		} else {
			// add by shuang, for handling unrecognized command for overlay
			// manager
			overlayLogger.warn("Invalid command for overlayManager.");
			exceptionLogger.warn(new Exception("Invalid command for overlayManager."));
		}
	}

	protected boolean checkRemoteBrokerURItoConnect(String toBrokerURI) throws CommunicationException {
		if (!brokerCore.getCommSystem().isEqualServerURI(toBrokerURI)) {
			return true;
		} else {
			overlayLogger.error("The broker " + brokerCore.getBrokerID()
					+ " tries to connect to itself.");
			exceptionLogger.error(new Exception("The broker "
					+ brokerCore.getBrokerID() + " tries to connect to itself."));
			return false;
		}
	}

	protected MessageSender createMessageSenderAndConnect(String toBrokerURI) throws CommunicationException {
		MessageSender msgSender =
			brokerCore.getCommSystem().getMessageSender(toBrokerURI);
		msgSender.connect();
		return msgSender;
	}

	private void sendConnectRequest(String remoteBrokerID, MessageDestination remoteDest)
			throws CommunicationException {
		Advertisement adv = new Advertisement();
		adv.addPredicate("class", new Predicate("eq", "BROKER_CONTROL"));
		adv.addPredicate("brokerID", new Predicate("isPresent", ""));
		adv.addPredicate("command", new Predicate("eq", "OVERLAY-CONNECT_REQ"));
		adv.addPredicate("fromID", new Predicate("eq", brokerCore.getBrokerID()));
		adv.addPredicate("fromURI", new Predicate("eq", brokerCore.getBrokerURI()));
		AdvertisementMessage advmsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				brokerCore.getBrokerDestination());
		advmsg.setTTL(0);
		overlayLogger.debug("Sending advertisement for overlay CONNECT_REQ message.");
		brokerCore.routeMessage(advmsg, remoteDest);

		Publication pub = MessageFactory.createEmptyPublication();
		pub.addPair("class", "BROKER_CONTROL");
		pub.addPair("command", "OVERLAY-CONNECT_REQ");
		pub.addPair("brokerID", remoteBrokerID);
		pub.addPair("fromID", brokerCore.getBrokerID());
		pub.addPair("fromURI", brokerCore.getBrokerURI());
		PublicationMessage pubmsg = new PublicationMessage(pub, brokerCore.getNewMessageID(),
				brokerCore.getBrokerDestination());
		overlayLogger.debug("The broker " + brokerCore.getBrokerID()
				+ " is sending CONNECT_REQ message to broker " + remoteBrokerID + ".");
		brokerCore.routeMessage(pubmsg, remoteDest);
	}

	private void sendConnectRequestAck(String remoteBrokerID, MessageDestination remoteDest)
			throws CommunicationException {
		Advertisement adv = new Advertisement();
		adv.addPredicate("class", new Predicate("eq", "BROKER_CONTROL"));
		adv.addPredicate("brokerID", new Predicate("isPresent", ""));
		adv.addPredicate("command", new Predicate("eq", "OVERLAY-CONNECT_ACK"));
		adv.addPredicate("fromID", new Predicate("eq", brokerCore.getBrokerID()));
		adv.addPredicate("fromURI", new Predicate("eq", brokerCore.getBrokerURI()));
		AdvertisementMessage advmsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				brokerCore.getBrokerDestination());
		advmsg.setTTL(0);

		overlayLogger.debug("Sending advertisement for overlay CONNECT_ACK message.");
		messagePathLogger.info(brokerCore.getBrokerID() + " is sending message: " + advmsg);
		brokerCore.routeMessage(advmsg, remoteDest);

		Publication pub = MessageFactory.createEmptyPublication();
		pub.addPair("class", "BROKER_CONTROL");
		pub.addPair("brokerID", remoteBrokerID);
		pub.addPair("command", "OVERLAY-CONNECT_ACK");
		pub.addPair("fromID", brokerCore.getBrokerID());
		pub.addPair("fromURI", brokerCore.getBrokerURI());
		PublicationMessage pubmsg = new PublicationMessage(pub, brokerCore.getNewMessageID(),
				brokerCore.getBrokerDestination());

		overlayLogger.debug("The broker is sending CONNECT_ACK message to broker " + remoteBrokerID
				+ ".");
		messagePathLogger.info(brokerCore.getBrokerID() + " is sending message: " + pubmsg);
		brokerCore.routeMessage(pubmsg, remoteDest);
	}

	private void forwardAllAdvToNeighbor(String remoteBrokerID) {
		if(brokerCore == null)
			return; // silently drop
		Map<String, AdvertisementMessage> advertisementsMap = brokerCore.getRouter().getAdvertisements();
		synchronized (advertisementsMap) {
			for (AdvertisementMessage tempAdvMsg : advertisementsMap.values()) {
				AdvertisementMessage advMsg = tempAdvMsg.duplicate();
				MessageDestination currentBroker = brokerCore.getBrokerDestination();
				int index1 = advMsg.getMessageID().indexOf("-");
				String fromDestName = advMsg.getMessageID().substring(0, index1);
				MessageDestination fromDest = new MessageDestination(remoteBrokerID,
						DestinationType.BROKER);
				if (!fromDestName.equals(fromDest.getDestinationID())) {
					advMsg.setNextHopID(fromDest);
					advMsg.setLastHopID(currentBroker);
					// Update TTL and only forward message if TTL is still valid
					if (!advMsg.updateTTLAndExpired()) {
						overlayLogger.info("Forward old advs from " + brokerCore.getBrokerID()
								+ " to " + remoteBrokerID + " : " + advMsg.toString());
						messagePathLogger.info(brokerCore.getBrokerID() + " is sending message: "
								+ advMsg);
						brokerCore.routeMessage(advMsg);
					}
				}
				advMsg = null;
			}
		}
	}

	/**
	 * Stop all the transport bindings in the broker. Even those that are still connecting
	 */
	private void stopOutputQueues() {
		overlayLogger.debug("Stopping output queues");
		Map<MessageDestination, OutputQueue> neighbours = routingTable.getBrokerQueues();
		synchronized (neighbours) {
			for (OutputQueue outQueue : neighbours.values()) {
				overlayLogger.debug("Stopping output queue for destination : "
						+ outQueue.getDestination());
				outQueue.stopOperation();
			}
		}
	}

	/**
	 * Resume all the transport bindings in the broker.
	 */
	private void resumeOutputQueues() {
		overlayLogger.debug("Resuming output queues.");
		Map<MessageDestination, OutputQueue> neighbours = routingTable.getBrokerQueues();
		synchronized (neighbours) {
			for (OutputQueue outQueue : neighbours.values()) {
				overlayLogger.debug("Resuming output queue for destination : "
						+ outQueue.getDestination());
				outQueue.resumeOperation();
			}
		}
	}

	/**
	 * Terminate all the transport bindings in the broker.
	 */
	private void shutdownOutputQueues() {
		overlayLogger.debug("Shutting down output queues.");
		Map<MessageDestination, OutputQueue> neighbours = routingTable.getBrokerQueues();
		synchronized (neighbours) {
			for (OutputQueue outQueue : neighbours.values()) {
				overlayLogger.debug("Shutting down output queue for destination : "
						+ outQueue.getDestination());
				outQueue.shutdown();
			}
		}
		
		neighbours = routingTable.getClientQueues();
		synchronized (neighbours) {
			for (OutputQueue outQueue : neighbours.values()) {
				overlayLogger.debug("Shutting down output queue for destination : "
						+ outQueue.getDestination());
				outQueue.shutdown();
			}
		}
	}
	
	@Override
	public void connectionMade(MessageDestination clientDest, MessageSender msgSender) {
		/* Add the client to the overlay network */
		overlayLogger.info("Add client : " + clientDest + " as new client into ORT.");
		OutputQueue clientQueue = createOutputQueue(clientDest, msgSender);
		routingTable.addClient(clientQueue);
		clientQueue.start();
		brokerCoreLogger.info(String.format("Client %s established a connection", clientDest));
		brokerCore.registerQueue(clientQueue);
	}

	@Override
	public void connectionBroke(MessageDestination clientDest) {
		routingTable.getOutputQueue(clientDest);
		routingTable.removeClient(clientDest);
		brokerCore.removeQueue(clientDest);
	}

	protected OutputQueue createOutputQueue(MessageDestination clientDest,
			MessageSender msgSender) {
		return new OutputQueue(clientDest, msgSender);
	}

	@Override
	public void shutdown() {
		shutdownOutputQueues();
	}
}
