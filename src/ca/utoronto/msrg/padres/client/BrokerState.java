package ca.utoronto.msrg.padres.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.utoronto.msrg.padres.common.comm.MessageSender;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;

public class BrokerState {

	private NodeAddress brokerAddress;

	private MessageSender msgSender;

	private Set<SubscriptionMessage> subMessages;

	private Set<CompositeSubscriptionMessage> csMessages;

	private Set<AdvertisementMessage> advMessages;

	private Set<PublicationMessage> receivedPubMessages;

	public BrokerState(NodeAddress brokerAddress) {
		this.brokerAddress = brokerAddress;
		subMessages = new HashSet<SubscriptionMessage>();
		csMessages = new HashSet<CompositeSubscriptionMessage>();
		advMessages = new HashSet<AdvertisementMessage>();
		receivedPubMessages = new HashSet<PublicationMessage>();
	}

	public NodeAddress getBrokerAddress() {
		return brokerAddress;
	}

	public MessageSender getMsgSender() {
		return msgSender;
	}

	public void setMsgSender(MessageSender msgSender) {
		this.msgSender = msgSender;
	}

	public Set<SubscriptionMessage> getSubMessages() {
		return subMessages;
	}

	public Set<CompositeSubscriptionMessage> getCSMessages() {
		return csMessages;
	}

	public Set<AdvertisementMessage> getAdvMessages() {
		return advMessages;
	}

	public void addSubMsg(SubscriptionMessage subMsg) {
		subMessages.add(subMsg);
	}

	public void addCSSubMsg(CompositeSubscriptionMessage csMsg) {
		csMessages.add(csMsg);
	}

	public void addAdvMsg(AdvertisementMessage advMsg) {
		advMessages.add(advMsg);
	}

	public void clear() {
		clearAdvMessages();
		clearSubMessages();
		clearCSMessages();
		clearReceivedPubs();
	}

	public void clearSubMessages() {
		subMessages.clear();
	}

	public void clearCSMessages() {
		csMessages.clear();
	}

	public void clearAdvMessages() {
		advMessages.clear();
	}

	public boolean removeSubMsg(SubscriptionMessage subMsg) {
		return subMessages.remove(subMsg);
	}

	public SubscriptionMessage removeSubMsg(String subMsgID) {
		SubscriptionMessage remSubMsg = null;
		for (SubscriptionMessage subMsg : subMessages) {
			if (subMsg.getMessageID().equals(subMsgID))
				remSubMsg = subMsg;
		}
		if (remSubMsg != null)
			subMessages.remove(remSubMsg);
		return remSubMsg;
	}

	public boolean removeCSMsg(CompositeSubscriptionMessage csMsg) {
		return csMessages.remove(csMsg);
	}

	public CompositeSubscriptionMessage removeCSMsg(String csMsgID) {
		CompositeSubscriptionMessage remCSMsg = null;
		for (CompositeSubscriptionMessage csMsg : csMessages) {
			if (csMsg.getMessageID().equals(csMsgID))
				remCSMsg = csMsg;
		}
		if (remCSMsg != null)
			csMessages.remove(remCSMsg);
		return remCSMsg;
	}

	public boolean removeAdvMsg(AdvertisementMessage advMsg) {
		return advMessages.remove(advMsg);
	}

	public AdvertisementMessage removeAdvMsg(String advMsgID) {
		AdvertisementMessage remAdvMsg = null;
		for (AdvertisementMessage advMsg : advMessages) {
			if (advMsg.getMessageID().equals(advMsgID))
				remAdvMsg = advMsg;
		}
		if (remAdvMsg != null)
			advMessages.remove(remAdvMsg);
		return remAdvMsg;
	}

	public boolean containsSub(String subMsgID) {
		for (SubscriptionMessage subMsg : subMessages) {
			if (subMsg.getMessageID().equals(subMsgID))
				return true;
		}
		return false;
	}

	public List<String> containsSubs(String[] subMsgIDs) {
		List<String> foundIDs = new ArrayList<String>();
		for (SubscriptionMessage subMsg : subMessages) {
			if (Arrays.binarySearch(subMsgIDs, subMsg.getMessageID()) >= 0) {
				foundIDs.add(subMsg.getMessageID());
			}
		}
		return foundIDs;
	}

	public boolean containsCS(String csMsgID) {
		for (CompositeSubscriptionMessage csMsg : csMessages) {
			if (csMsg.getMessageID().equals(csMsgID))
				return true;
		}
		return false;
	}

	public List<String> containsCS(String[] csMsgIDs) {
		List<String> foundIDs = new ArrayList<String>();
		for (CompositeSubscriptionMessage csMsg : csMessages) {
			if (Arrays.binarySearch(csMsgIDs, csMsg.getMessageID()) > 0) {
				foundIDs.add(csMsg.getMessageID());
			}
		}
		return foundIDs;
	}

	public boolean containsAdv(String advMsgID) {
		for (AdvertisementMessage advMsg : advMessages) {
			if (advMsg.getMessageID().equals(advMsgID))
				return true;
		}
		return false;
	}

	public List<String> containsAdvs(String[] advMsgIDs) {
		List<String> foundIDs = new ArrayList<String>();
		for (AdvertisementMessage advMsg : advMessages) {
			if (Arrays.binarySearch(advMsgIDs, advMsg.getMessageID()) > 0) {
				foundIDs.add(advMsg.getMessageID());
			}
		}
		return foundIDs;
	}

	public synchronized void addReceivedPub(PublicationMessage pubMsg) {
		receivedPubMessages.add(pubMsg);
	}

	public synchronized void clearReceivedPubs() {
		receivedPubMessages.clear();
	}

	public synchronized boolean checkReceivedPub(Publication pubToCheck) {
		for (PublicationMessage pubMsg : receivedPubMessages) {
			if (pubToCheck.equalVals(pubMsg.getPublication()))
				return true;
		}
		return false;
	}
}
