package ca.utoronto.msrg.padres.client;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.common.comm.CommSystem;
import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.comm.MessageSender;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Unadvertisement;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Uncompositesubscription;
import ca.utoronto.msrg.padres.common.message.UncompositesubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Unsubscription;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogException;
import ca.utoronto.msrg.padres.common.util.LogSetup;

/**
 * @author Bala Maniymaran
 * 
 *         Created: 2010-07-09
 * 
 *         This is universal client that uses a unified communication layer. It can talk to brokers
 *         that implements any type of communication protocol (e.g. RMI, socket, etc.) All the
 *         future clients must to be extended from this class or use this underneath.
 */
public class Client {

	/**
	 * Configuration object of the client
	 */
	protected ClientConfig clientConfig;

	/**
	 * ID of the client; it must be unique respective to the connected brokers
	 */
	protected String clientID;

	/**
	 * Communication layer via which messages are conveyed/received
	 */
	protected CommSystem commSystem;

	/**
	 * The default broker to which the broker is connected. When the client is connected it can not
	 * be null
	 */
	protected NodeAddress defaultBrokerAddress;

	/**
	 * The default MessageDestination of teh client. It is connected to the defaultBrokerAddress
	 * above
	 */
	protected MessageDestination defaultClientDest;

	/**
	 * List of CommandHandlers that accept textual commands from user interface and act upon them
	 */
	protected List<CommandHandler> cmdHandlers;

	/**
	 * Internal states of the client, maintained per connected broker. The size of the map has to be
	 * at least one when the client is connected to the overlay
	 */
	protected Map<NodeAddress, BrokerState> brokerStates = new HashMap<NodeAddress, BrokerState>();

	/**
	 * The message listener that hooks itself to the communication layer so that it can receive
	 * notification from the connected brokers. What to do with the received messages must be
	 * implemented in this class.
	 */
	protected MessageQueueManager msgListener;

	/**
	 * Total number of the messages, irrespective of the type of message, produced by the client
	 */
	protected long msgCount;

	/**
	 * Total number of publications sent from this client
	 */
	protected long pubCount = 0;

	protected PublicationMessage receivedPubMsg;

	protected static Logger exceptionLogger;

	protected static Logger clientLogger;

	protected static Logger messageLogger;

	/**
	 * This constructor is provided for backward compatibility. Don't use this.
	 * 
	 * @throws ClientException
	 */
	public Client() throws ClientException {
		System.out.println("Warning: Try to avoid this empty constructor for "
				+ this.getClass().getName());
		initialize(new ClientConfig());
	}

	/**
	 * Client constructor. Creates a client with the specified ID. It also initialize the client
	 * with the default client configuration (read from default client config file.)
	 * 
	 * @param id
	 * @throws ClientException
	 */
	public Client(String id) throws ClientException {
		clientID = id;
		initialize(new ClientConfig());
	}

	/**
	 * Client constructor that accepts a client ID and a user-defined client configuration object.
	 * 
	 * @param id
	 * @param newConfig
	 * @throws ClientException
	 */
	public Client(String id, ClientConfig newConfig) throws ClientException {
		clientID = id;
		initialize(newConfig);
	}

	/**
	 * Client constructor that accepts only the user-defined client configuration object. Client ID
	 * is to be found within this configuration object.
	 * 
	 * @param newConfig
	 * @throws ClientException
	 */
	public Client(ClientConfig newConfig) throws ClientException {
		initialize(newConfig);
	}

	/**
	 * Initializes the client object: (a) adds the default command handler (b) initializes the
	 * logging system (c) creates a message listener (d) assign client ID if not already assigned
	 * (e) if the configuration object specifies list of brokers, connects to those brokers and hook
	 * the message listener to those broker connections
	 * 
	 * This method is always called internally from the constructors
	 * 
	 * @param newConfig
	 * @throws ClientException
	 */
	protected void initialize(ClientConfig newConfig) throws ClientException {
		clientConfig = newConfig;
		cmdHandlers = new ArrayList<CommandHandler>();
		addCommandHandler(new BaseCommandHandler(this));
		// initialize logging
		initLog(clientConfig.logLocation);
		// start a message listener in a thread who listens to messages from server
		msgListener = new MessageQueueManager(this);
		if (clientID == null)
			clientID = clientConfig.clientID;
		try {
			commSystem = new CommSystem();
			MessageSender.setConnectRetryLimit(clientConfig.connectionRetries);
			MessageSender.setConnectRetryPauseTime(clientConfig.retryPauseTime);
			if (clientConfig.connectBrokerList != null) {
				String[] brokerList = clientConfig.connectBrokerList;
				for (String brokerURI : brokerList) {
					connect(brokerURI);
				}
			}
		} catch (CommunicationException e) {
			throw new ClientException(e.getMessage());
		}
	}

	/**
	 * Initializes the logging system; to be called from {@link #initialize(ClientConfig)} method
	 * 
	 * @param logPath
	 * @throws ClientException
	 */
	protected void initLog(String logPath) throws ClientException {
		if (!Logger.getRootLogger().getAllAppenders().hasMoreElements()) {
			try {
				new LogSetup(logPath);
			} catch (LogException e) {
				throw new ClientException("Initialization of Logger failed: ", e);
			}
		}
		exceptionLogger = Logger.getLogger("Exception");
		clientLogger = Logger.getLogger(Client.class);
		messageLogger = Logger.getLogger("MessagePath");
	}

	/**
	 * Returns the configuration object of the client. Depending on the subclass the actual returned
	 * object can be subclass of {@link ClientConfig} class.
	 * 
	 * @return the client's configuration object.
	 * 
	 * @see ClientConfig
	 */
	public ClientConfig getClientConfig() {
		return clientConfig;
	}

	/**
	 * Add an command handler. Note that a default command handler is added to all the clients
	 * during the initialization process.
	 * 
	 * @param cmdHandler
	 */
	protected void addCommandHandler(CommandHandler cmdHandler) {
		cmdHandlers.add(cmdHandler);
	}

	/**
	 * Returns the command handler that handles the given cmd
	 * 
	 * @param cmd
	 * @return
	 */
	protected CommandHandler getCommandHandler(String cmd) {
		for (CommandHandler handler : cmdHandlers) {
			if (handler.supportCommand(cmd))
				return handler;
		}
		return null;
	}

	/**
	 * Create a new broker state for the given broker URI and add it to the list managed by this
	 * client. If the a non-null message sender is pass on to this method, it will be associated
	 * with the broker state being added
	 * 
	 * @param brokerAddress
	 * @param msgSender
	 * @return
	 */
	protected BrokerState addBrokerState(NodeAddress brokerAddress, MessageSender msgSender) {
		BrokerState newBrokerState = createBrokerState(brokerAddress);
		if (msgSender != null)
			newBrokerState.setMsgSender(msgSender);
		brokerStates.put(brokerAddress, newBrokerState);
		return newBrokerState;
	}

	protected BrokerState createBrokerState(NodeAddress brokerAddress) {
		return new BrokerState(brokerAddress);
	}

	/**
	 * Gracefully shuts down the client. Disconnects from all the brokers first.
	 * 
	 * @throws ClientException
	 */
	public void shutdown() throws ClientException {
		for (BrokerState brokerState : brokerStates.values()) {
			disconnect(brokerState);
		}
		
		try {
			commSystem.shutDown();
		} catch (CommunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * Returns the unique (respective to a connected broker) of the client.
	 * 
	 * @return
	 */
	public String getClientID() {
		return clientID;
	}

	/**
	 * To set the ID of the client. Note the presence of a unique/invariant client ID is important
	 * for the correct operation of a client. Therefore, if you are using this method, make sure
	 * that you are using it before any of the pub/sub operation is used.
	 * 
	 * To be on the same side, don't use this method if you are not absolutely sure of what you are
	 * doing.
	 * 
	 * @param id
	 */
	public void setClientID(String id) {
		clientID = id;
		if (defaultBrokerAddress != null)
			defaultClientDest = MessageDestination.formatClientDestination(clientID,
					defaultBrokerAddress.getNodeURI());
	}

	/**
	 * Returns the MessageDestination of the client which acts as a sign post for pub/sub messages.
	 * 
	 * @return
	 */
	public MessageDestination getClientDest() {
		return defaultClientDest;
	}

	/**
	 * Returns the node address of the default broker the client is connected to. Generally, the
	 * first broker the client connects to becomes the default broker, unless specified otherwise.
	 * 
	 * @see {@link #setDefaultBrokerAddress(NodeAddress)}
	 * 
	 * @return
	 */
	public NodeAddress getDefaultBrokerAddress() {
		return defaultBrokerAddress;
	}

	/**
	 * To specify the default broker of the client. When no broker is specified, all pub/sub
	 * operations will be carried out through this broker.
	 * 
	 * @param brokerAddress
	 */
	public void setDefaultBrokerAddress(NodeAddress brokerAddress) {
		defaultBrokerAddress = brokerAddress;
		defaultClientDest = MessageDestination.formatClientDestination(clientID,
				defaultBrokerAddress.getNodeURI());
	}

	/**
	 * Adds a message queue to the message listener. When a pub/sub message is received by the
	 * message listener, it duplicates it and provides a copy to all the message queues added to it.
	 * Different message queue can implement different functionalities to handle this message.
	 * 
	 * @param msgQueue
	 */
	public void addMessageQueue(MessageQueue msgQueue) {
		msgListener.addMessageQueue(msgQueue);
	}

	/**
	 * Provided a broker URI, this method returns the associated broker state. The broker state
	 * contains the details about the broker connection as well as, depending on the configuration,
	 * the adv/sub messages sent to the specified broker.
	 * 
	 * @param brokerURI
	 * @return
	 * @throws ClientException
	 *             If the given broker URI is mal-formatted
	 */
	public BrokerState getBrokerState(String brokerURI) throws ClientException {
		try {
			NodeAddress brokerAddress = NodeAddress.getAddress(brokerURI);
			return brokerStates.get(brokerAddress);
		} catch (CommunicationException e) {
			throw new ClientException("Could not get broker status: " + e.getMessage(), e);
		}
	}

	public void clearBrokerStates(NodeAddress brokerAddress) {
		brokerStates.get(brokerAddress).clear();
	}

	/**
	 * Returns the map of brokerID -> brokerURI of all the existing broker connections.
	 * 
	 * TODO: This method wastes memory because it now maps brokerURI to brokerURI; it got changed on
	 * the way; remove this method and use {@link #getBrokerURIList()} instead
	 * 
	 * @return
	 */
	public Map<String, String> getBrokerConnections() {
		Map<String, String> idURIMap = new HashMap<String, String>();
		for (BrokerState brokerState : brokerStates.values()) {
			idURIMap.put(brokerState.getBrokerAddress().getNodeURI(),
					brokerState.getBrokerAddress().getNodeURI());
		}
		return idURIMap;
	}

	/**
	 * Returns all the subscription messages the client has sent in the past. This method is
	 * operational only when the store_detail_state option is switched ON.
	 * 
	 * @return
	 * @throws ClientException
	 *             If the client.store_detail_state option is OFF
	 */
	public Map<String, SubscriptionMessage> getSubscriptions() throws ClientException {
		if (!clientConfig.detailState)
			throw new ClientException(
					"getSubscriptions() not supported with client.store_detail_state=OFF");
		HashMap<String, SubscriptionMessage> idMsgMap = new HashMap<String, SubscriptionMessage>();
		for (BrokerState brokerState : brokerStates.values()) {
			Set<SubscriptionMessage> subs = brokerState.getSubMessages();
			for (SubscriptionMessage subMsg : subs)
				idMsgMap.put(subMsg.getMessageID(), subMsg);
		}
		return idMsgMap;
	}

	/**
	 * Returns all the composite subscription messages the client has sent in the past. This method
	 * is operational only when the store_detail_state option is switched ON.
	 * 
	 * @return
	 * @throws ClientException
	 *             If the client.store_detail_state option is OFF
	 */
	public Map<String, CompositeSubscriptionMessage> getCompositeSubscriptions()
			throws ClientException {
		if (!clientConfig.detailState)
			throw new ClientException(
					"getCompositeSubscriptions() not supported with client.store_detail_state=OFF");
		HashMap<String, CompositeSubscriptionMessage> idMsgMap = new HashMap<String, CompositeSubscriptionMessage>();
		for (BrokerState brokerState : brokerStates.values()) {
			Set<CompositeSubscriptionMessage> cSubs = brokerState.getCSMessages();
			for (CompositeSubscriptionMessage csMsg : cSubs)
				idMsgMap.put(csMsg.getMessageID(), csMsg);
		}
		return idMsgMap;
	}

	/**
	 * Returns all the advertisement messages the client has sent in the past. This method is
	 * operational only when the store_detail_state option is switched ON.
	 * 
	 * @return
	 * @throws ClientException
	 *             If the client.store_detail_state option is OFF
	 */
	public Map<String, AdvertisementMessage> getAdvertisements() throws ClientException {
		if (!clientConfig.detailState)
			throw new ClientException(
					"getAdvertisements() not supported with client.store_detail_state=OFF");
		HashMap<String, AdvertisementMessage> idMsgMap = new HashMap<String, AdvertisementMessage>();
		for (BrokerState brokerState : brokerStates.values()) {
			Set<AdvertisementMessage> advs = brokerState.getAdvMessages();
			for (AdvertisementMessage advMsg : advs)
				idMsgMap.put(advMsg.getMessageID(), advMsg);
		}
		return idMsgMap;
	}

	/**
	 * Returns all the available commands a user can issue through a client's
	 * {@link #handleCommand(String)} method. The returned set depends on the command handlers added
	 * to this client. At least the commands enabled by the BaseCommandHandler will be returned by
	 * this command, because this command handler is always added to any client by default.
	 * 
	 * @return
	 */
	public Set<String> getAvailableCommands() {
		Set<String> cmdList = new HashSet<String>();
		for (CommandHandler handler : cmdHandlers) {
			cmdList.addAll(handler.getCommandList());
		}
		return cmdList;
	}

	/**
	 * Returns the help text for a particular command. For this command to work properly, the
	 * command should be supported by one of the command handlers enabled in the client and the
	 * command handler who supports this command should also include a help text for that command.
	 * 
	 * @see #addCommandHandler(CommandHandler), {@link CommandHandler}
	 * 
	 * @param cmd
	 *            The command for which help needed.
	 * @return The help text for the specified command, null if the command is not found in any of
	 *         the command handlers.
	 */
	public String getCommandHelp(String cmd) {
		for (CommandHandler handler : cmdHandlers) {
			if (handler.supportCommand(cmd))
				return handler.getHelp(cmd);
		}
		return null;
	}

	/**
	 * Whenever a message is received by the message listener, this method is called. The child
	 * classes extended from Client can overwrite this method to implement their own
	 * functionalities.
	 * 
	 * @param msg
	 *            The new message received from the communication interface (from Broker.)
	 */
	public void processMessage(Message msg) {
		receivedPubMsg = (PublicationMessage) msg;
		if (clientConfig.detailState) {
			// TODO: the below line works because we now assume the clients connects to only one
			// broker. Modify this so that the received publication is added to the correct
			// broker state.
			BrokerState bState = brokerStates.get(defaultBrokerAddress);
			if(bState != null)
				bState.addReceivedPub(receivedPubMsg);
		}
		messageLogger.info("Message received at Client " + clientID + ": " + msg);
	}

	public Publication getCurrentPub() {
		if (receivedPubMsg != null)
			return receivedPubMsg.getPublication();
		return null;
	}

	public boolean checkForReceivedPub(Publication pubToCheck) {
		return brokerStates.get(defaultBrokerAddress).checkReceivedPub(pubToCheck);
	}

	/**
	 * Runs a particular command via appropriate command handler. The result is returned in the
	 * {@link CommandResult} data structure.
	 * 
	 * @param commandString
	 *            The command string to be run. It includes command, its options, and arguments all
	 *            separated by space. It is upto the relavant command handler to distinguish between
	 *            comand, options, and arguments.
	 * @return A {@link CommandResult} data structure which includes the result of running the
	 *         command. In case of an error, it as well is stored in the returned data strucute.
	 * @throws ParseException 
	 * 
	 * @see CommandResult
	 */
	public CommandResult handleCommand(String commandString) throws ParseException {
		CommandResult cmdResult = new CommandResult(commandString);
		if (!cmdResult.isError()) {
			CommandHandler handler = getCommandHandler(cmdResult.command);
			if (handler == null) {
				cmdResult.errMsg = "Command " + cmdResult.command + " is not supported\n"
						+ "Type 'help' for more info";
			} else {
				handler.runCommand(cmdResult);
			}
		}
		return cmdResult;
	}

	/**
	 * Connects to a broker with the given URI. The given URI should conform to an accepted
	 * communication protocol format.
	 * 
	 * @param brokerURI
	 *            URI of the broker to connect to.
	 * @return A BrokerState data structure to keep track of the state of the connection and related
	 *         operation
	 * @throws ClientException
	 *             In case the given URI is malformated, a connection already exists to the
	 *             specified broker, or a communication error is occurred.
	 * 
	 * @see BrokerState, NodeAddress
	 */
	public BrokerState connect(String brokerURI) throws ClientException {
		try {
			NodeAddress brokerAddr = NodeAddress.getAddress(brokerURI);
			if (brokerStates.containsKey(brokerAddr)) {
				throw new ClientException("Server connection already exists");
			} else {
				if (brokerStates.size() == 0)
					setDefaultBrokerAddress(brokerAddr);
				MessageSender msgSender = commSystem.getMessageSender(brokerURI);
				BrokerState bState = addBrokerState(brokerAddr, msgSender);
				
				msgSender.connect(
						MessageDestination.formatClientDestination(clientID,
								brokerAddr.getNodeURI()), msgListener);
				return bState;
			}
		} catch (CommunicationException e) {
			exceptionLogger.error("Could not connect to broker: " + e);
			throw new ClientException("Could not connect to broker: " + e.getMessage(), e);
		}
	}

	/**
	 * To check whether the client is connected to any broker.
	 * 
	 * @return true if it is connected to a broker; false otherwise.
	 */
	public boolean isConnected() {
		return (!brokerStates.isEmpty());
	}

	/**
	 * To check whether an active connection exists to a given broker.
	 * 
	 * @param brokerURI
	 *            The URI of the broker to check for the connection.
	 * @return true is the client is connected to the given broker; false otherwise.
	 * @throws ClientException
	 *             If the given URI is malformatted.
	 * 
	 * @see NodeAddress, {@link #connect(String)}
	 */
	public boolean connectionIsActive(String brokerURI) throws ClientException {
		try {
			NodeAddress brokerAddr = NodeAddress.getAddress(brokerURI);
			return brokerStates.containsKey(brokerAddr);
		} catch (CommunicationException e) {
			throw new ClientException(e.getMessage(), e);
		}
	}

	/**
	 * Disconnects from all the connected brokers. The client first unsubscribe/unadvertise all the
	 * subscriptions/advertisements before disconnecting from brokers.
	 * 
	 * @return A String message that describes the success/failure of the operation.
	 */
	public String disconnectAll() {
		String outStr = "";
		for (BrokerState brokerState : new HashMap<NodeAddress, BrokerState>(brokerStates).values()) {
			try {
				disconnect(brokerState);
				outStr += "disconnected from " + brokerState.getBrokerAddress() + "\n";
			} catch (ClientException e) {
				outStr += e.getMessage() + "\n";
			}
		}
		return outStr;
	}

	/**
	 * Disconnects from the broker with a specific URI. The client first unsubscribe/unadvertise all
	 * the subscriptions/advertisements before disconnecting from brokers.
	 * 
	 * @param brokerURI
	 *            The URI of the broker to disconnect from.
	 * @return The BrokerState assosiated with the broker.
	 * @throws ClientException
	 *             If the given broker URI is malformatted, no connection exists to the specified
	 *             broker, or some other communication exception during disconnection.
	 */
	public BrokerState disconnect(String brokerURI) throws ClientException {
		BrokerState brokerState = getBrokerState(brokerURI);
		if (brokerState == null)
			throw new ClientException("Not connected to broker " + brokerURI);
		return disconnect(brokerState);
	}

	/**
	 * Disconnects from a broker with a specified BrokerState. The client first
	 * unsubscribe/unadvertise all the subscriptions/advertisements before disconnecting from
	 * brokers.
	 * 
	 * @param brokerState
	 *            the BrokerState of the broker to disconnect from
	 * @return The given broker state.
	 * @throws ClientException
	 *             Some exeception is unsubscribing/unadvertising or some other communication error.
	 */
	protected BrokerState disconnect(BrokerState brokerState) throws ClientException {
		try {
			// Withdraw all the messages
			if (clientConfig.detailState) {
				unsubscribeAll(brokerState);
				unsubscribeCSAll(brokerState);
				unAdvertiseAll(brokerState);
			}
			// disconnect
			MessageSender msgSender = brokerState.getMsgSender();
			msgSender.disconnect(MessageDestination.formatClientDestination(clientID,
					brokerState.getBrokerAddress().getNodeURI()));
			// remove the broker state
			brokerStates.remove(brokerState.getBrokerAddress());
		} catch (CommunicationException e) {
			throw new ClientException(String.format("Problem disconnecting from Broker %s: %s",
					brokerState.getBrokerAddress(), e.getMessage()));
		}
		return brokerState;
	}

	/**
	 * Send an advertisement to a broker. The advertisemnet is given as a String comforming the
	 * PADRES message format. The broker is specified with its URI.
	 * 
	 * @param advStr
	 *            The advertisement string conforming PADRES message format.
	 * @param brokerURI
	 *            The URI of the broker to send the advertisement to.
	 * @return The {@link AdvertisementMessage} produced by the given advertisement string. The
	 *         message ID of the message is returned by the broker.
	 * @throws ClientException
	 *             Either their is a syntax error the advertisement string or other error during
	 *             advertisement.
	 * @throws ParseException 
	 * @see #advertise(Advertisement, String)
	 */
	public AdvertisementMessage advertise(String advStr, String brokerURI) throws ClientException, ParseException {
		Advertisement newAdv = MessageFactory.createAdvertisementFromString(advStr);
		if (newAdv.getClassVal() == null) {
			throw new ClientException("Advertisement syntax error");
		}
		return advertise(newAdv, brokerURI);
	}

	/**
	 * Sends an advertisement to the default broker.
	 * 
	 * @param adv
	 *            The advertisement to be sent. It is an {@link Advertisement} object.
	 * @return The {@link AdvertisementMessage} produced by the given advertisement. The message ID
	 *         of the message is returned by the broker.
	 * @throws ClientException
	 *             An error occurred during advertisement.
	 * @see #advertise(Advertisement, String)
	 */
	public AdvertisementMessage advertise(Advertisement adv) throws ClientException {
		return advertise(adv, null);
	}

	/**
	 * Sends an advertisement to a given broker.
	 * 
	 * @param adv
	 *            The advertisement to be sent. It is an {@link Advertisement} object.
	 * @param brokerURI
	 *            The URI of the broker to where the advertisement is sent.
	 * @return The {@link AdvertisementMessage} produced by the given advertisement. The message ID
	 *         of the message is returned by the broker.
	 * @throws ClientException
	 *             If the given URI is malformatted, the client is not connected to the broker, or
	 *             there is a communication error while sending the advertisement.
	 */
	public AdvertisementMessage advertise(Advertisement adv, String brokerURI)
			throws ClientException {
		if (!isConnected())
			throw new ClientException("Not connected to any broker");
		try {
			if (brokerURI == null || brokerURI.equals(""))
				brokerURI = defaultBrokerAddress.getNodeURI();
			BrokerState brokerState = getBrokerState(brokerURI);
			if (brokerState == null) {
				throw new ClientException("Not connected to broker " + brokerURI);
			}
			MessageDestination clientDest = MessageDestination.formatClientDestination(clientID,
					brokerState.getBrokerAddress().getNodeURI());
			AdvertisementMessage advMsg = new AdvertisementMessage(adv,
					getNextMessageID(brokerState.getBrokerAddress().getNodeURI()), clientDest);
			String msgID = brokerState.getMsgSender().send(advMsg, HostType.CLIENT);
			advMsg.setMessageID(msgID);
			if (clientConfig.detailState)
				brokerState.addAdvMsg(advMsg);
			return advMsg;
		} catch (CommunicationException e) {
			throw new ClientException(e.getMessage());
		}
	}

	/**
	 * Send a subscription represented by the String subStr to the broker with URI brokerURI.
	 * 
	 * @param subStr
	 *            Subscription in String format. Check the PADRES message format for the syntax.
	 * @param brokerURI
	 *            The URI of the broker to which the subscription is to be sent.
	 * @return The SubscriptionMessage sent to the broker containing the given subscription.
	 * @throws ClientException
	 *             If there is a synatx error in the given subscription string or an thrown by the
	 *             {@link #subscribe(Subscription, String)} method called by this method.
	 * @throws ParseException 
	 */
	public SubscriptionMessage subscribe(String subStr, String brokerURI) throws ClientException, ParseException {
		Subscription newSub = MessageFactory.createSubscriptionFromString(subStr);
		if (newSub.getClassVal() == null) {
			throw new ClientException("Subscription syntax error");
		}
		return subscribe(newSub, brokerURI);
	}

	/**
	 * Send a subscription to the default broker. Calls {@link #subscribe(Subscription, String)}
	 * method internally.
	 * 
	 * @param sub
	 *            The subscription to be sent.
	 * @return The SubscriptionMessage sent containing the given subscription.
	 * @throws ClientException
	 *             When the {@link #subscribe(Subscription, String)} call throws an exception.
	 */
	public SubscriptionMessage subscribe(Subscription sub) throws ClientException {
		return subscribe(sub, null);
	}

	/**
	 * Send a subscription to a broker with the given URI. In case the brokerURI is null, the
	 * subscription will be sent to the default broker.
	 * 
	 * @param sub
	 *            The subscription to be sent.
	 * @param brokerURI
	 *            The broker to which the subscription is to be sent.
	 * @return The SubscriptionMessage containing the given subscription.
	 * @throws ClientException
	 *             Upon the following situations:
	 *             <ul>
	 *             <li>The client is not connected to the specified broker.</li>
	 *             <li>Given brokerURI is badly formated.</li>
	 *             <li>There is an error in sending the subscription message.</li>
	 *             </ul>
	 */
	public SubscriptionMessage subscribe(Subscription sub, String brokerURI) throws ClientException {
		if (!isConnected())
			throw new ClientException("Not connected to any broker");
		try {
			if (brokerURI == null || brokerURI.equals(""))
				brokerURI = defaultBrokerAddress.getNodeURI();
			BrokerState brokerState = getBrokerState(brokerURI);
			if (brokerState == null) {
				throw new ClientException("Not connected to broker " + brokerURI);
			}
			MessageDestination clientDest = MessageDestination.formatClientDestination(clientID,
					brokerState.getBrokerAddress().getNodeURI());
			SubscriptionMessage subMsg = new SubscriptionMessage(sub,
					getNextMessageID(brokerState.getBrokerAddress().getNodeURI()), clientDest);

			// TODO: fix this hack for historic queries
			Map<String, Predicate> predMap = subMsg.getSubscription().getPredicateMap();
			if (predMap.get("_start_time") != null) {
				SimpleDateFormat timeFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
				try {
					Date startTime = timeFormat.parse((String) (predMap.get("_start_time")).getValue());
					predMap.remove("_start_time");
					subMsg.setStartTime(startTime);
				} catch (java.text.ParseException e) {
					exceptionLogger.error("Fail to convert Date format : " + e);
				}
			}
			if (predMap.get("_end_time") != null) {
				SimpleDateFormat timeFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
				try {
					Date endTime = timeFormat.parse((String) (predMap.get("_end_time")).getValue());
					predMap.remove("_end_time");
					subMsg.setEndTime(endTime);
				} catch (java.text.ParseException e) {
					exceptionLogger.error("Fail to convert Date format : " + e);
				}
			}
			
			String msgID = brokerState.getMsgSender().send(subMsg, HostType.CLIENT);
			subMsg.setMessageID(msgID);
			if (clientConfig.detailState)
				brokerState.addSubMsg(subMsg);
			return subMsg;
		} catch (CommunicationException e) {
			throw new ClientException(e.getMessage());
		}
	}

	public CompositeSubscriptionMessage subscribeCS(String subStr, String brokerID)
			throws ClientException {
		// create the subscription
		CompositeSubscription newCS = new CompositeSubscription(subStr);
		if (newCS.getSubscriptionMap() == null || newCS.getSubscriptionMap().size() == 0) {
			throw new ClientException("Composite Subscription syntax error");
		}
		return subscribeCS(newCS, brokerID);
	}

	public CompositeSubscriptionMessage subscribeCS(CompositeSubscription cs)
			throws ClientException {
		return subscribeCS(cs, null);
	}

	public CompositeSubscriptionMessage subscribeCS(CompositeSubscription cs, String brokerURI)
			throws ClientException {
		if (!isConnected())
			throw new ClientException("Not connected to any broker");
		try {
			if (brokerURI == null || brokerURI.trim().equals(""))
				brokerURI = defaultBrokerAddress.getNodeURI();
			BrokerState brokerState = getBrokerState(brokerURI);
			if (brokerState == null) {
				throw new ClientException("Not connected to broker " + brokerURI);
			}
			MessageDestination clientDest = MessageDestination.formatClientDestination(clientID,
					brokerState.getBrokerAddress().getNodeURI());
			CompositeSubscriptionMessage newCSMsg = new CompositeSubscriptionMessage(cs,
					getNextMessageID(brokerState.getBrokerAddress().getNodeURI()), clientDest);
			String msgID = brokerState.getMsgSender().send(newCSMsg, HostType.CLIENT);
			newCSMsg.setMessageID(msgID);
			if (clientConfig.detailState)
				brokerState.addCSSubMsg(newCSMsg);
			return newCSMsg;
		} catch (CommunicationException e) {
			throw new ClientException(e.getMessage());
		}
	}

	public PublicationMessage publish(String pubStr, String brokerID) throws ClientException {
		try {
			Publication newPub = MessageFactory.createPublicationFromString(pubStr);
			if (newPub.getClassVal() == null) {
				throw new ClientException("Publication syntax error");
			}
			return publish(newPub, brokerID);
		} catch (ParseException e) {
			throw new ClientException(e);
		}
	}

	public PublicationMessage publish(String pubStr, Serializable payload, String brokerID)
			throws ClientException {
		try {
			Publication newPub = MessageFactory.createPublicationFromString(pubStr);
			if (newPub.getClassVal() == null) {
				throw new ClientException("Publication syntax error");
			}
			newPub.setPayload(payload);
			return publish(newPub, brokerID);
		} catch (ParseException e) {
			throw new ClientException(e);
		}
	}

	public PublicationMessage publish(String pubStr, ConcurrentHashMap<Object, Object> payload,
			String brokerID) throws ClientException {
		return publish(pubStr, (Serializable) payload, brokerID);
	}

	public PublicationMessage publish(Publication pub) throws ClientException {
		return publish(pub, null);
	}

	public PublicationMessage publish(Publication pub, String brokerURI) throws ClientException {
		if (!isConnected())
			throw new ClientException("Not connected to any broker");
		try {
			if (brokerURI == null || brokerURI.trim().length() == 0)
				brokerURI = defaultBrokerAddress.getNodeURI();
			BrokerState brokerState = getBrokerState(brokerURI);
			if (brokerState == null) {
				throw new ClientException("Not connected to broker " + brokerURI);
			}
			MessageDestination clientDest = MessageDestination.formatClientDestination(clientID,
					brokerState.getBrokerAddress().getNodeURI());
			PublicationMessage pubMsg = new PublicationMessage(pub,
					getNextMessageID(brokerState.getBrokerAddress().getNodeURI()), clientDest);
			pubCount++;
			String msgID = brokerState.getMsgSender().send(pubMsg, HostType.CLIENT);
			pubMsg.setMessageID(msgID);
			return pubMsg;
		} catch (CommunicationException e) {
			throw new ClientException(e.getMessage());
		}
	}

	public void unAdvertiseAll() throws ClientException {
		for (BrokerState brokerState : brokerStates.values())
			unAdvertiseAll(brokerState);
	}

	public void unAdvertiseAll(String brokerURI) throws ClientException {
		if (!isConnected())
			throw new ClientException("Not connected to any broker");
		BrokerState brokerState = getBrokerState(brokerURI);
		if (brokerState == null) {
			throw new ClientException("Not connected to broker " + brokerURI);
		}
		unAdvertiseAll(brokerState);
	}

	protected void unAdvertiseAll(BrokerState brokerState) throws ClientException {
		if (!clientConfig.detailState)
			throw new ClientException(
					"unAdertiseAll() not supported with client.store_detail_state=OFF");
		MessageDestination clientDest = MessageDestination.formatClientDestination(clientID,
				brokerState.getBrokerAddress().getNodeURI());
		MessageSender msgSender = brokerState.getMsgSender();
		if (msgSender == null)
			throw new ClientException("Connection not found for broker "
					+ brokerState.getBrokerAddress());
		AdvertisementMessage[] advMsgArray = brokerState.getAdvMessages().toArray(
				new AdvertisementMessage[0]);
		for (AdvertisementMessage advMsg : advMsgArray) {
			Unadvertisement unAdv = new Unadvertisement(advMsg.getMessageID());
			UnadvertisementMessage unAdvMsg = new UnadvertisementMessage(unAdv,
					getNextMessageID(brokerState.getBrokerAddress().getNodeURI()), clientDest);
			try {
				msgSender.send(unAdvMsg, HostType.CLIENT);
				brokerState.removeAdvMsg(advMsg);
			} catch (CommunicationException e) {
				throw new ClientException(e.getMessage());
			}
		}
	}

	public UnadvertisementMessage unAdvertise(String advID) throws ClientException {
		UnadvertisementMessage resultMsg = null;
		boolean sendRequest = true;
		for (BrokerState brokerState : brokerStates.values()) {
			sendRequest = true;
			if (clientConfig.detailState)
				sendRequest = brokerState.containsAdv(advID);
			if (sendRequest) {
				resultMsg = unAdvertise(advID, brokerState);
			}
		}
		if (!sendRequest)
			throw new ClientException("Advertisement not found");
		return resultMsg;
	}

	public List<UnadvertisementMessage> unAdvertise(String[] advIDList) throws ClientException {
		ArrayList<UnadvertisementMessage> unAdvMsgs = new ArrayList<UnadvertisementMessage>();
		List<String> foundIDs = new ArrayList<String>(Arrays.asList(advIDList));
		for (BrokerState brokerState : brokerStates.values()) {
			if (clientConfig.detailState)
				foundIDs = brokerState.containsAdvs(advIDList);
			for (String advID : foundIDs) {
				unAdvMsgs.add(unAdvertise(advID, brokerState));
			}
		}
		return unAdvMsgs;
	}

	protected UnadvertisementMessage unAdvertise(String advID, BrokerState brokerState)
			throws ClientException {
		try {
			Unadvertisement unAdv = new Unadvertisement(advID);
			MessageDestination clientDest = MessageDestination.formatClientDestination(clientID,
					brokerState.getBrokerAddress().getNodeURI());
			UnadvertisementMessage unAdvMsg = new UnadvertisementMessage(unAdv,
					getNextMessageID(brokerState.getBrokerAddress().getNodeURI()), clientDest);
			String msgID = brokerState.getMsgSender().send(unAdvMsg, HostType.CLIENT);
			unAdvMsg.setMessageID(msgID);
			if (clientConfig.detailState)
				brokerState.removeAdvMsg(advID);
			return unAdvMsg;
		} catch (CommunicationException e) {
			throw new ClientException(e.getMessage());
		}
	}

	public void unsubscribeAll() throws ClientException {
		for (BrokerState brokerState : brokerStates.values())
			unsubscribeAll(brokerState);
	}

	public void unsubscribeAll(String brokerURI) throws ClientException {
		if (!isConnected())
			throw new ClientException("Not connected to any broker");
		BrokerState brokerState = getBrokerState(brokerURI);
		if (brokerState == null) {
			throw new ClientException("Not connected to broker " + brokerURI);
		}
		unsubscribeAll(brokerState);
	}

	protected void unsubscribeAll(BrokerState brokerState) throws ClientException {
		if (!clientConfig.detailState)
			throw new ClientException(
					"unsubscribeAll() not supported with client.store_detail_state=OFF");
		MessageDestination clientDest = MessageDestination.formatClientDestination(clientID,
				brokerState.getBrokerAddress().getNodeURI());
		MessageSender msgSender = brokerState.getMsgSender();
		if (msgSender == null)
			throw new ClientException("Connection not found for broker "
					+ brokerState.getBrokerAddress());
		SubscriptionMessage[] subMsgArray = brokerState.getSubMessages().toArray(
				new SubscriptionMessage[0]);
		for (SubscriptionMessage subMsg : subMsgArray) {
			Unsubscription unSub = new Unsubscription(subMsg.getMessageID());
			UnsubscriptionMessage unSubMsg = new UnsubscriptionMessage(unSub,
					getNextMessageID(brokerState.getBrokerAddress().getNodeURI()), clientDest);
			try {
				msgSender.send(unSubMsg, HostType.CLIENT);
				if (clientConfig.detailState)
					brokerState.removeSubMsg(subMsg);
			} catch (CommunicationException e) {
				throw new ClientException(e.getMessage());
			}
		}
	}

	public UnsubscriptionMessage unSubscribe(String subID) throws ClientException {
		UnsubscriptionMessage resultMsg = null;
		boolean sendRequest = true;
		for (BrokerState brokerState : brokerStates.values()) {
			sendRequest = true;
			if (clientConfig.detailState)
				sendRequest = brokerState.containsSub(subID);
			if (sendRequest) {
				resultMsg = unSubscribe(subID, brokerState);
			}
		}
		if (!sendRequest)
			throw new ClientException("Subscription not found");
		return resultMsg;
	}

	public List<UnsubscriptionMessage> unSubscribe(String[] subIDList) throws ClientException {
		List<UnsubscriptionMessage> unSubMsgIDs = new ArrayList<UnsubscriptionMessage>();
		List<String> foundIDs = new ArrayList<String>(Arrays.asList(subIDList));
		for (BrokerState brokerState : brokerStates.values()) {
			if (clientConfig.detailState)
				foundIDs = brokerState.containsSubs(subIDList);
			for (String advID : foundIDs) {
				unSubMsgIDs.add(unSubscribe(advID, brokerState));
			}
		}
		return unSubMsgIDs;
	}

	protected UnsubscriptionMessage unSubscribe(String subID, BrokerState brokerState)
			throws ClientException {
		try {
			Unsubscription unSub = new Unsubscription(subID);
			MessageDestination clientDest = MessageDestination.formatClientDestination(clientID,
					brokerState.getBrokerAddress().getNodeURI());
			UnsubscriptionMessage unSubMsg = new UnsubscriptionMessage(unSub,
					getNextMessageID(brokerState.getBrokerAddress().getNodeURI()), clientDest);
			String msgID = brokerState.getMsgSender().send(unSubMsg, HostType.CLIENT);
			unSubMsg.setMessageID(msgID);
			if (clientConfig.detailState)
				brokerState.removeSubMsg(subID);
			return unSubMsg;
		} catch (CommunicationException e) {
			throw new ClientException(e.getMessage());
		}
	}

	public void unsubscribeCSAll() throws ClientException {
		for (BrokerState brokerState : brokerStates.values())
			unsubscribeCSAll(brokerState);
	}

	public void unsubscribeCSAll(String brokerURI) throws ClientException {
		if (!isConnected())
			throw new ClientException("Not connected to any broker");
		BrokerState brokerState = getBrokerState(brokerURI);
		if (brokerState == null) {
			throw new ClientException("Not connected to broker " + brokerURI);
		}
		unsubscribeCSAll(brokerState);
	}

	protected void unsubscribeCSAll(BrokerState brokerState) throws ClientException {
		if (!clientConfig.detailState)
			throw new ClientException(
					"unsubscribeCSAll() not supported with client.store_detail_state=OFF");
		MessageDestination clientDest = MessageDestination.formatClientDestination(clientID,
				brokerState.getBrokerAddress().getNodeURI());
		MessageSender msgSender = brokerState.getMsgSender();
		if (msgSender == null)
			throw new ClientException("Connection not found for broker "
					+ brokerState.getBrokerAddress());
		CompositeSubscriptionMessage[] csMsgArray = brokerState.getCSMessages().toArray(
				new CompositeSubscriptionMessage[0]);
		for (CompositeSubscriptionMessage csMsg : csMsgArray) {
			Uncompositesubscription unCS = new Uncompositesubscription(csMsg.getMessageID());
			UncompositesubscriptionMessage unCSMsg = new UncompositesubscriptionMessage(unCS,
					getNextMessageID(brokerState.getBrokerAddress().getNodeURI()), clientDest);
			try {
				msgSender.send(unCSMsg, HostType.CLIENT);
				brokerState.removeCSMsg(csMsg);
			} catch (CommunicationException e) {
				throw new ClientException(e.getMessage());
			}
		}
	}

	public UncompositesubscriptionMessage unSubscribeCS(String csID) throws ClientException {
		UncompositesubscriptionMessage resultMsg = null;
		boolean sendRequest = true;
		for (BrokerState brokerState : brokerStates.values()) {
			sendRequest = true;
			if (clientConfig.detailState)
				sendRequest = brokerState.containsCS(csID);
			if (sendRequest) {
				resultMsg = unSubscribeCS(csID, brokerState);
			}
		}
		if (!sendRequest)
			throw new ClientException("Composite subscription not found");
		return resultMsg;
	}

	public List<UncompositesubscriptionMessage> unSubscribeCS(String[] csIDList)
			throws ClientException {
		ArrayList<UncompositesubscriptionMessage> unCSMsgIDs = new ArrayList<UncompositesubscriptionMessage>();
		List<String> foundIDs = new ArrayList<String>(Arrays.asList(csIDList));
		for (BrokerState brokerState : brokerStates.values()) {
			if (clientConfig.detailState)
				foundIDs = brokerState.containsSubs(csIDList);
			for (String csID : foundIDs) {
				unCSMsgIDs.add(unSubscribeCS(csID, brokerState));
			}
		}
		return unCSMsgIDs;
	}

	protected UncompositesubscriptionMessage unSubscribeCS(String csID, BrokerState brokerState)
			throws ClientException {
		try {
			Uncompositesubscription unCS = new Uncompositesubscription(csID);
			MessageDestination clientDest = MessageDestination.formatClientDestination(clientID,
					brokerState.getBrokerAddress().getNodeURI());
			UncompositesubscriptionMessage unCSMsg = new UncompositesubscriptionMessage(unCS,
					getNextMessageID(brokerState.getBrokerAddress().getNodeURI()), clientDest);
			String msgID = brokerState.getMsgSender().send(unCSMsg, HostType.CLIENT);
			unCSMsg.setMessageID(msgID);
			if (clientConfig.detailState)
				brokerState.removeCSMsg(csID);
			return unCSMsg;
		} catch (CommunicationException e) {
			throw new ClientException(e.getMessage());
		}
	}


	/**
	 * Print the incremental results of a batch command.
	 */
	public void printResults(CommandResult results) {
		// Do nothing. Derived class can override this.
	}

	/**
	 * Returns an array of the URI strings of all the connected brokers.
	 * 
	 * @return Array of broker URI strings of all the connected brokers.
	 */
	protected String[] getBrokerURIList() {
		String[] brokerList = new String[brokerStates.size()];
		int i = 0;
		for (NodeAddress addr : brokerStates.keySet()) {
			brokerList[i++] = addr.getNodeURI();
		}
		return brokerList;
	}

	/**
	 * Produce a unique message ID for the next message to be generated. It is unique because of the
	 * message serial number. The ID has the format of
	 * CM-<client_ID>-<broker_URI>-<msg_serial_number>
	 * 
	 * Every call to this method will increment the message serial number.
	 * 
	 * @param brokerURI
	 *            The URI of the broker where the message is to be sent. Broker URI is not necessary
	 *            to gaurantee the uniqueness of the message ID. It is just for convinience.
	 * @return A String unique message ID for the next message to be generated.
	 */
	protected String getNextMessageID(String brokerURI) {
		return String.format("CM-%s-%s-%d", clientID, brokerURI, msgCount++);
	}

	public String toString() {
		return "Client-" + clientID;
	}

}
