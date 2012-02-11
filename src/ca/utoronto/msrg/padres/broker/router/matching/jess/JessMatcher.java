package ca.utoronto.msrg.padres.broker.router.matching.jess;

import java.io.CharArrayWriter;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import jess.Defrule;
import jess.Deftemplate;
import jess.Fact;
import jess.HasLHS;
import jess.JessEvent;
import jess.JessException;
import jess.RU;
import jess.Rete;
import jess.Value;
import jess.ValueVector;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.router.Router;
import ca.utoronto.msrg.padres.broker.router.matching.DuplicateMsgFoundException;
import ca.utoronto.msrg.padres.broker.router.matching.Matcher;
import ca.utoronto.msrg.padres.broker.router.matching.jess.userfunction.After;
import ca.utoronto.msrg.padres.broker.router.matching.jess.userfunction.Before;
import ca.utoronto.msrg.padres.broker.router.matching.jess.userfunction.EqualTo;
import ca.utoronto.msrg.padres.broker.router.matching.jess.userfunction.GreaterThan;
import ca.utoronto.msrg.padres.broker.router.matching.jess.userfunction.GreaterThanOrEqualTo;
import ca.utoronto.msrg.padres.broker.router.matching.jess.userfunction.IsPresent;
import ca.utoronto.msrg.padres.broker.router.matching.jess.userfunction.LessThan;
import ca.utoronto.msrg.padres.broker.router.matching.jess.userfunction.LessThanOrEqualTo;
import ca.utoronto.msrg.padres.broker.router.matching.jess.userfunction.NotEqualTo;
import ca.utoronto.msrg.padres.broker.router.matching.jess.userfunction.Overlaps;
import ca.utoronto.msrg.padres.broker.router.matching.rete.ReteMatcher;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeNode;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionOPs;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.UncompositesubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Unsubscription;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

public class JessMatcher implements Matcher {

	// Constants for properties file
	// private static final String DEFAULT_ADV_COVERING = "OFF";

	private Rete SRT;

	private Rete PRT;

	// private Context SRTContext;

	// private Context PRTContext;

	private CharArrayWriter forwardingTargetsBuffer;

	private Set<String> appGIDPubMsgToPRTFactSet;

	// for flush partial matching pub in the PRT
	private Map<String, Set<Integer>> pubClassNameToPRTFactMap;

	// For use in unadvertising, will need a mapping from advertisement message ID to fact ID, and
	// vice-versa
	private Map<String, Fact> advMessageIDToSRTFactMap;

	private Map<Fact, String> SRTFactToAdvMessageIDMap;

	private long variableCounter;

	private long factCounter;

	private BrokerCore brokerCore;

	private Router router;

	private static Logger reteMatcherLogger = Logger.getLogger(ReteMatcher.class);

	private static Logger exceptionLogger = Logger.getLogger("Exception");

	public JessMatcher(BrokerCore broker, Router router) {

		// Construct the two Retes and get their context (used in certain Jess
		// functions)
		SRT = new Rete();
		// SRTContext = SRT.getGlobalContext();
		SRT.getGlobalContext();
		PRT = new Rete();
		// PRTContext = PRT.getGlobalContext();
		PRT.getGlobalContext();

		forwardingTargetsBuffer = new CharArrayWriter();

		advMessageIDToSRTFactMap = new HashMap<String, Fact>();
		SRTFactToAdvMessageIDMap = new HashMap<Fact, String>();
		appGIDPubMsgToPRTFactSet = new HashSet<String>();
		pubClassNameToPRTFactMap = new HashMap<String, Set<Integer>>();

		variableCounter = 0;
		factCounter = 0;

		// Add listeners for events
		SRT.addJessListener(new MatchEventHandler());
		PRT.addJessListener(new MatchEventHandler());
		PRT.setEventMask(PRT.getEventMask() | JessEvent.CLEAR | JessEvent.RESET
				| JessEvent.DEFTEMPLATE | JessEvent.DEFRULE | JessEvent.DEFRULE_FIRED
				| JessEvent.FACT);
		// Define useful functions
		defineUsefulFunctions();
		brokerCore = broker;
		this.router = router;

		reteMatcherLogger.debug("ReteMatcher is fully started.");
	}

	public void flushPRTByClassName(String className) {
		if (pubClassNameToPRTFactMap.containsKey(className)) {
			for (Integer factid : pubClassNameToPRTFactMap.get(className)) {
				Fact oldFact = null;
				try {
					oldFact = PRT.findFactByID(factid.intValue());
				} catch (JessException e) {
					reteMatcherLogger.error("Failed to find fact by ID : " + e);
					exceptionLogger.error("Failed to find fact by ID : " + e);
				}
				try {
					PRT.retract(oldFact);
				} catch (JessException e) {
					reteMatcherLogger.error("Failed to retract fact from PRT : " + e);
					exceptionLogger.error("Failed to retract fact from PRT : " + e);
				}
			}
			pubClassNameToPRTFactMap.remove(className);

		} else {
			reteMatcherLogger.warn("No such kind of publication in PRT, class name is : "
					+ className);
			exceptionLogger.warn("Here is an exception, ", new Exception(
					"No such kind of publication in PRT, class name is : " + className));
		}

	}

	public Set<String> add(AdvertisementMessage m) throws DuplicateMsgFoundException {
		Set<String> matchingResult = new HashSet<String>();
		// This will be the set representing what messages get added to the
		// matching engine(s)
		Set<Message> messageWithNextHopIDSet = new HashSet<Message>();
		try {
			// Make sure the buffer that catches the forwarding targets is empty
			forwardingTargetsBuffer.reset();

			try {
				addAdvertisementToSRT((AdvertisementMessage) m);
			} catch (JessException e) {
				reteMatcherLogger.error("Failed to add advertisement to SRT: " + e);
				exceptionLogger.error("Failed to add advertisement to SRT: " + e);
			}
			try {
				addAdvertisementToPRT((AdvertisementMessage) m);
			} catch (JessException e) {
				reteMatcherLogger.error("Failed to add advertisement to PRT: " + e);
				exceptionLogger.error("Failed to add advertisement to PRT: " + e);
			}

			// Now run the SRT to check if any new rules fired. This is to catch subscriptions that
			// had come in but hadn't been advertised, and also serves to deal with the bug we had
			// before where old rules would fire when new subscriptions came in, giving old
			// forwarding targets that didn't apply
			int numberOfSRTRulesFired = SRT.run();
			if (numberOfSRTRulesFired > 0) {
				// Must now parse the stream
				StringTokenizer forwardingTargetsTokenizer = new StringTokenizer(
						forwardingTargetsBuffer.toString());
				readInBufferSubAdv(forwardingTargetsTokenizer, MessageType.ADVERTISEMENT,
						messageWithNextHopIDSet, matchingResult);

				// If any rules fired in the SRT, then add those subscription messages to the PRT
				forwardingTargetsBuffer.reset();
				for (Message msg : messageWithNextHopIDSet) {
					// gli: if messageID is not sub_ID then added in, else do not added in PRT
					SubscriptionMessage tmp = (SubscriptionMessage) msg;
					if (tmp.getMessageID().indexOf("-s") == -1) {
						try {
							reteMatcherLogger.debug("Adding subscription that matches new coming advertisement to PRT : "
									+ tmp.toString());
							addSubscriptionToPRT(tmp);
						} catch (JessException e) {
							reteMatcherLogger.error("Failed to add subscription to PRT: " + e);
							exceptionLogger.error("Failed to add subscription to PRT: " + e);
						}
					}
				}

				// Once the rules are added, now run the PRT to check if any new messages match
				forwardingTargetsBuffer.reset();

				// new subscription and old publication for primitive subscriptions.
				try {
					reteMatcherLogger.debug("Running PRT.");
					PRT.run();
				} catch (JessException e) {
					reteMatcherLogger.error("Failed to run PRT: " + e);
					exceptionLogger.error("Failed to run PRT: " + e);
				}
				forwardingTargetsBuffer.reset();
			}

			return matchingResult;

		} catch (ParseException e) {
			reteMatcherLogger.error("Something went wrong in advertisement conversion: " + e);
			exceptionLogger.error("Something went wrong in advertisement conversion: " + e);
		} catch (JessException e) {
			reteMatcherLogger.error("Something went wrong in advertisement conversion: " + e);
			exceptionLogger.error("Something went wrong in advertisement conversion: " + e);
		}
		return null;
	}

	public Set<String> add(SubscriptionMessage subMsg) {
		Set<String> matchingResult = new HashSet<String>();
		try {
			// This will be the set representing what messages get added to the matching engine(s)
			Set<Message> messageWithNextHopIDSet = new HashSet<Message>();

			// Make sure the buffer that catches the forwarding targets is empty
			forwardingTargetsBuffer.reset();

			try {
				reteMatcherLogger.debug("Add subscription to SRT : " + subMsg.toString());
				addSubscriptionToSRT(subMsg);
			} catch (JessException e) {
				reteMatcherLogger.error("Failed to add subscription to SRT : " + e);
				exceptionLogger.error("Failed to add subscription to SRT : " + e);
			}

			// Run the SRT, which will return the number of rules fired
			int numberOfSRTRulesFired = 0;
			try {
				reteMatcherLogger.debug("Running SRT.");
				numberOfSRTRulesFired = SRT.run();
			} catch (JessException e) {
				reteMatcherLogger.error("Failed to run SRT : " + e);
				exceptionLogger.error("Failed to run SRT : " + e);
			}

			// Nothing needs to be done unless a rule fired in the SRT
			if (numberOfSRTRulesFired > 0) {
				// Must now parse the stream (there should only be subscriptions in the buffer), and
				// add it to the set of messages
				StringTokenizer forwardingTargetsTokenizer = new StringTokenizer(
						forwardingTargetsBuffer.toString());
				readInBufferSubAdv(forwardingTargetsTokenizer, MessageType.SUBSCRIPTION,
						messageWithNextHopIDSet, matchingResult);

				// Once all the subscription messages are retreived from the buffer, reset it
				forwardingTargetsBuffer.reset();

				// Add all subscriptions which were retreived to the PRT
				reteMatcherLogger.debug("Adding subscriptions to PRT.");
				try {
					for (Message msg : messageWithNextHopIDSet) {
						addSubscriptionToPRT((SubscriptionMessage) msg);
					}
				} catch (JessException e) {
					reteMatcherLogger.error("Failed to add subscription to PRT : " + e);
					exceptionLogger.error("Failed to add subscription to PRT : " + e);
				}

				// Once the rules are added, now run the PRT to check if any new messages match
				// Those publications do not need to route, since they are old publications;
				try {
					reteMatcherLogger.debug("Running PRT.");
					PRT.run();
				} catch (JessException e) {
					reteMatcherLogger.error("Failed to run PRT : " + e);
					exceptionLogger.error("Failed to run PRT : " + e);
				}
				forwardingTargetsBuffer.reset();
			}

			// Now return the set of subscription messages w/ next hop IDs
			if (reteMatcherLogger.isDebugEnabled())
				reteMatcherLogger.debug("Set of messages need to be routed out: "
						+ matchingResult.toString());

			return matchingResult;

		} catch (Exception e) {
			reteMatcherLogger.error("Something went wrong in subscription conversion: " + e);
			exceptionLogger.error("Something went wrong in subscription conversion: " + e);
		}
		return matchingResult;
	}

	public Set<String> getMatchingAdvs(PublicationMessage pubMsg) {
		// we will convert it to a fake subscription
		SubscriptionMessage subMessage = convertPubMsgToSubMsg(pubMsg);
		forwardingTargetsBuffer.reset();

		// insert this fake subscription to SRT and run SRT, to find if any rules fired
		try {
			reteMatcherLogger.debug("Add fake subscription to SRT : " + subMessage.toString());
			addSubscriptionToSRT(subMessage);
		} catch (JessException e) {
			reteMatcherLogger.error("Failed to add fake subscription to SRT : " + e);
			exceptionLogger.error("Failed to add fake subscription to SRT : " + e);
		}

		int numberOfSRTRulesFired = 0;
		try {
			reteMatcherLogger.debug("Running SRT.");
			numberOfSRTRulesFired = SRT.run();
		} catch (JessException e) {
			reteMatcherLogger.error("Failed to run SRT : " + e);
			exceptionLogger.error("Failed to run SRT : " + e);
		}

		try {
			reteMatcherLogger.debug("Remove the fake subscription from SRT.");
			SRT.unDefrule(subMessage.getMessageID());
		} catch (JessException e) {
			reteMatcherLogger.error("Failed to remove the fake subscription from SRT: " + e);
			exceptionLogger.error("Failed to remove the fake subscription from SRT: " + e);
		}

		Set<String> matchedPubAdvs = new HashSet<String>();
		// Nothing needs to be done unless a rule fired in the SRT
		if (numberOfSRTRulesFired > 0) {
			// Must now parse the stream (there should only be subscriptions in the buffer), and add
			// it to the set of messages
			StringTokenizer tempForwardingTargetsTokenizer = new StringTokenizer(
					forwardingTargetsBuffer.toString());
			if (reteMatcherLogger.isDebugEnabled())
				reteMatcherLogger.debug("forwardingTargetsBuffer is: "
						+ forwardingTargetsBuffer.toString());

			while (tempForwardingTargetsTokenizer.hasMoreElements()) {
				// last hop ID
				tempForwardingTargetsTokenizer.nextToken(" ").toString();
				// consume and neglect advMsgID
				String advMsgID = tempForwardingTargetsTokenizer.nextToken(" ");
				// next token is TID, if cyclic feature is enabled
				if (brokerCore.isCycle()) {
					tempForwardingTargetsTokenizer.nextToken(" ").toString();
				}
				// message string
				tempForwardingTargetsTokenizer.nextToken("\n").toString().trim();
				matchedPubAdvs.add(advMsgID);
			}
		} else {
			// no rule fired, this fake subscription(publication) is not qualified.
		}

		return matchedPubAdvs;
	}

	public Map<PublicationMessage, Set<String>> getMatchingSubs(PublicationMessage pubMsg) {
		Map<PublicationMessage, Set<String>> matchingResult = new HashMap<PublicationMessage, Set<String>>();
		try {
			// Make sure the buffer that catches the forwarding targets is empty
			forwardingTargetsBuffer.reset();
			// Convert publication message to a Jess fact
			Fact factToAssert = null;
			try {
				factToAssert = publicationToFact(pubMsg);
				if (reteMatcherLogger.isDebugEnabled())
					reteMatcherLogger.debug("The publication is transformed to fact : "
							+ factToAssert.toString());
			} catch (JessException e) {
				if (e.getMessage().indexOf("invalid slotname") != -1) {
					reteMatcherLogger.error("You probably didn't advertise before you published : "
							+ e);
					exceptionLogger.error("You probably didn't advertise before you published : "
							+ e);
				} else {
					reteMatcherLogger.error("Failed to transform publication to the fact : " + e);
					exceptionLogger.error("Failed to transform publication to the fact : " + e);
				}
			}
			// Now try and assert the fact
			Fact insertFact = null;
			try {
				insertFact = PRT.assertFact(factToAssert);
				reteMatcherLogger.debug("Assert the fact into the PRT.");
			} catch (JessException e) {
				reteMatcherLogger.error("Failed to assert the fact to the PRT : " + e);
				exceptionLogger.error("Failed to assert the fact to the PRT : " + e);
			}

			int factID = insertFact.getFactId();
			Map<String, Serializable> attrPairMap = pubMsg.getPublication().getPairMap();
			String className = attrPairMap.get("class").toString();
			if (pubClassNameToPRTFactMap.containsKey(className)) {
				Set<Integer> factIDs = pubClassNameToPRTFactMap.get(className);
				factIDs.add(new Integer(factID));
			} else {
				Set<Integer> factIDs = new HashSet<Integer>();
				factIDs.add(new Integer(factID));
				pubClassNameToPRTFactMap.put(className, factIDs);
			}
			// For JOB Scheduling ONLY
			// store the application-GID-factID in a set for later remove;
			if (className.equals("JOBSTATUS")) {
				if (factID != -1) {
					String key = "" + (String) attrPairMap.get("applname")
							+ (String) attrPairMap.get("GID") + ":" + factID;
					appGIDPubMsgToPRTFactSet.add(key);
				}
			}

			// Run the publication matching engine. Output SHOULD be in the forwarding targets
			// buffer
			try {
				reteMatcherLogger.debug("Running PRT.");
				PRT.run();
			} catch (JessException e) {
				reteMatcherLogger.error("Failed to run PRT : " + e);
				exceptionLogger.error("Failed to run PRT : " + e);
			}

			// Check the class of publications, if it is system control publication, then retract it
			// out. For network construction, publications of BROKER_CTL, BROKER_MONITOR and
			// NETWORK_DISCOVERY should be retracted from PRT;
			if (className.equals("BROKER_CONTROL") || className.equals("BROKER_MONITOR")
					|| className.equals("NETWORK_DISCOVERY") || className.equals("BROKER_INFO")) {
				try {
					reteMatcherLogger.debug("Retract fact from PRT.");
					PRT.retract(factToAssert);
				} catch (JessException e) {
					reteMatcherLogger.error("Failed to retract fact from PRT : " + e);
					exceptionLogger.error("Failed to retract fact from PRT : " + e);
				}
			}

			// Must now parse the stream
			StringTokenizer forwardingTargetsTokenizer = new StringTokenizer(
					forwardingTargetsBuffer.toString());
			readInBufferPub(forwardingTargetsTokenizer, matchingResult);

			// Payload in publication is lost because it was not converted into a String in the
			// toString() method. There is no need to do this since payload never needs to be
			// matched. We just need to re-attach the payload to the set of forwarding publication
			// messages
			Serializable payload = pubMsg.getPublication().getPayload();
			attachPayload(payload, matchingResult);

			// If an APPL_END is received, then remove all the publications of
			// this generation;
			if ((className.equals("JOBSTATUS")) && (attrPairMap.get("jobname").equals("APPL_END"))) {
				String str = "" + (String) attrPairMap.get("applname")
						+ (String) attrPairMap.get("GID");
				Set<String> remove_Set = new HashSet<String>();
				for (String i_str : appGIDPubMsgToPRTFactSet) {
					if (i_str.indexOf(str) != -1) {
						int col_index = i_str.indexOf(":");
						Integer factid = Integer.valueOf(i_str.substring(col_index + 1));
						Fact oldFact = null;
						try {
							oldFact = PRT.findFactByID(factid.intValue());
						} catch (JessException e) {
							reteMatcherLogger.error("Failed to find fact by ID : " + e);
							exceptionLogger.error("Failed to find fact by ID : " + e);
						}
						try {
							PRT.retract(oldFact);
						} catch (JessException e) {
							reteMatcherLogger.error("Failed to retract fact from PRT : " + e);
							exceptionLogger.error("Failed to retract fact from PRT : " + e);
						}
						remove_Set.add(i_str);
						reteMatcherLogger.debug("Remove factID : " + factid + " from PRT.");
					}
				}
				// remove old publication factID from appGIDPubMsgToPRTFactSet
				appGIDPubMsgToPRTFactSet.removeAll(remove_Set);
			}

			if (reteMatcherLogger.isDebugEnabled())
				reteMatcherLogger.debug("Set of messages need to be routed out: "
						+ matchingResult.toString());
			return matchingResult;
		} catch (Exception e) {
			reteMatcherLogger.error("Something went wrong in publication conversion: " + e);
			exceptionLogger.error("Something went wrong in publication conversion: " + e);
		}
		return matchingResult;
	}

	public Map<SubscriptionMessage, Set<String>> add(CompositeSubscriptionMessage csSubMessage) {
		Map<SubscriptionMessage, Set<String>> matchingResult = new HashMap<SubscriptionMessage, Set<String>>();
		// This will be the set representing what messages get added to the matching engine(s)
		Set<Message> messageWithNextHopIDSet = new HashSet<Message>();

		// add CS into PRT
		String messageID = csSubMessage.getMessageID();
		reteMatcherLogger.debug("The ID of compositeSubscription is : " + messageID);
		Map<String, Subscription> subs = csSubMessage.getSubscription().getSubscriptionMap();
		for (String subName : subs.keySet()) {
			Set<String> tempMatchingResult = new HashSet<String>();
			reteMatcherLogger.debug("The sub-compositeSubscription is : " + subName);
			Subscription sub = csSubMessage.getSubscription().getAtomicSubscription(subName);
			SubscriptionMessage tmpSubMsg = new SubscriptionMessage(sub, messageID + "-" + subName,
					csSubMessage.getLastHopID());
			try {
				addSubscriptionToSRT(tmpSubMsg);
				int numOfFiredRules = SRT.run();
				if (numOfFiredRules > 0) {
					StringTokenizer tmpForwardingTargetsTokenizer = new StringTokenizer(
							forwardingTargetsBuffer.toString());
					readInBufferSubAdv(tmpForwardingTargetsTokenizer, MessageType.SUBSCRIPTION,
							messageWithNextHopIDSet, tempMatchingResult);
					forwardingTargetsBuffer.reset();
				} else {
					reteMatcherLogger.warn("The destination of sub-compositeSubscription is null.");
					exceptionLogger.warn("Here is an exception : ", new Exception(
							"The destination of sub-compositeSubscription is null."));
				}
				// we can remove this sentence;
				// SRT.unDefrule(messageID+"-"+subName);
			} catch (ParseException e) {
				reteMatcherLogger.error("Something went wrong in compositeSubscription conversion: "
						+ e);
				exceptionLogger.error("Something went wrong in compositeSubscription conversion: "
						+ e);
			} catch (JessException e) {
				reteMatcherLogger.error("Something went wrong in compositeSubscription conversion: "
						+ e);
				exceptionLogger.error("Something went wrong in compositeSubscription conversion: "
						+ e);
			}
			matchingResult.put(tmpSubMsg, tempMatchingResult);
		}

		try {
			reteMatcherLogger.debug("Adding compositeSubscription into PRT.");
			addCompositeSubscriptionMessageToPRT(csSubMessage);
		} catch (JessException e) {
			reteMatcherLogger.error("Failed to add compositeSubscription into PRT: " + e);
			exceptionLogger.error("Failed to add compositeSubscription into PRT: " + e);
		}

		// CompositeSubscriptions only get new sub-publications.
		try {
			reteMatcherLogger.debug("Running PRT.");
			PRT.run();
		} catch (JessException e) {
			reteMatcherLogger.error("Failed to run PRT: " + e);
			exceptionLogger.error("Failed to run PRT: " + e);
		}
		forwardingTargetsBuffer.reset();

		if (reteMatcherLogger.isDebugEnabled())
			reteMatcherLogger.debug("Set of messages need to be routed out: "
					+ matchingResult.toString());

		return matchingResult;

	}

	public Set<String> add(UnsubscriptionMessage unsubMessage) {
		Set<String> matchingResult = new HashSet<String>();
		String messageID = unsubMessage.getUnsubscription().getSubID();
		try {
			forwardingTargetsBuffer.reset();
			Set<String> tempMessageIDSet = new HashSet<String>();
			tempMessageIDSet.add(messageID);
			for (String msgId : tempMessageIDSet) {
				reteMatcherLogger.debug("Finding if the unsubscription : " + msgId + " was in SRT.");
				// The message IDs are unique, and therefore can be used to
				// identify the rule to be removed.
				HasLHS leftHandSide = SRT.findDefrule(msgId);

				if (leftHandSide instanceof Defrule) {
					// Before removing the rule, we need to figure out where that subscription had
					// went, so that we can send those nodes the unsubscription
					SubscriptionMessage subMessageToRemove = SRTDefruleToSubscriptionMessage((Defrule) leftHandSide);
					// To figure out where the subscription had been sent, we just need to re-add it
					// to the SRT (which basically will just overwrite that rule with the same rule,
					// but reset the activation, allowing it to fire again).
					String SRTRuleToReAdd = subscriptionMessageToSRTDefrule(subMessageToRemove);
					// Now add it to the SRT
					forwardingTargetsBuffer.reset();
					SRT.executeCommand(SRTRuleToReAdd);
					int numberOfSRTRulesFired = SRT.run();
					String tempMsgId = msgId;

					if (numberOfSRTRulesFired > 0) {
						// Must now parse the stream
						StringTokenizer forwardingTargetsTokenizer = new StringTokenizer(
								forwardingTargetsBuffer.toString());
						readInBufferUnSub(forwardingTargetsTokenizer, msgId, matchingResult);
					}
					try {
						reteMatcherLogger.debug("Remove unsub from SRT.");
						SRT.unDefrule(tempMsgId);
					} catch (JessException e) {
						reteMatcherLogger.error("Failed to remove unsubsription from SRT: " + e);
						exceptionLogger.error("Failed to remove unsubsription from SRT: " + e);
					}

					try {
						reteMatcherLogger.debug("Remove unsub from PRT.");

						PRT.unDefrule(msgId);

					} catch (JessException e) {
						reteMatcherLogger.error("Failed to remove unsubsription from PRT: " + e);
						exceptionLogger.error("Failed to remove unsubsription from PRT: " + e);
					}
				} else {
					reteMatcherLogger.warn("The unsubscription was not in SRT.");
					exceptionLogger.warn("Here is an exception: ", new Exception(
							"The unsubscription was not in SRT."));
				}

			}

			if (reteMatcherLogger.isDebugEnabled())
				reteMatcherLogger.debug("Set of messages need to be routed out: "
						+ matchingResult.toString());
			return matchingResult;

		} catch (ParseException e) {
			reteMatcherLogger.error("Something went wrong in unsubscribe: " + e);
			exceptionLogger.error("Something went wrong in unsubscribe: " + e);
		} catch (JessException e) {
			reteMatcherLogger.error("Something went wrong in unsubscribe: " + e);
			exceptionLogger.error("Something went wrong in unsubscribe: " + e);
		}
		return matchingResult;
	}

	public Map<UnsubscriptionMessage, Set<String>> add(UncompositesubscriptionMessage m) {
		Map<UnsubscriptionMessage, Set<String>> matchingResult = new HashMap<UnsubscriptionMessage, Set<String>>();
		// This will be the set representing what messages get added to the
		// matching engine(s)
		// StringTokenizer forwardingTargetsTokenizer;

		UncompositesubscriptionMessage uncompositesubMessage = m;
		String messageID = uncompositesubMessage.getUncompositesubscription().getSubID();

		CompositeSubscriptionMessage csSubMessage = router.getCompositeSubscription(messageID);

		try {
			forwardingTargetsBuffer.reset();
			reteMatcherLogger.debug("Finding if the unsubscription : " + messageID + " was in PRT.");

			// The message IDs are unique, and therefore can be used to identify
			// the rule to be removed.
			HasLHS leftHandSide = PRT.findDefrule(messageID);
			if (leftHandSide instanceof Defrule) {
				Map<String, Subscription> subs = csSubMessage.getSubscription().getSubscriptionMap();
				for (String subName : subs.keySet()) {
					Set<String> tempMatchingResult = new HashSet<String>();
					Subscription sub = csSubMessage.getSubscription().getAtomicSubscription(subName);
					SubscriptionMessage tmpSubMsg = new SubscriptionMessage(sub, "" + messageID
							+ "-" + subName, csSubMessage.getLastHopID());

					// To figure out where the subscription had been sent, we just need to re-add it
					// to the SRT (which basically will just overwrite that rule with the same rule,
					// but reset the activation, allowing it to fire again).
					String SRTRuleToReAdd = subscriptionMessageToSRTDefrule(tmpSubMsg);

					// Now add it to the SRT
					SRT.executeCommand(SRTRuleToReAdd);
					forwardingTargetsBuffer.reset();
					int numberOfSRTRulesFired = SRT.run();

					UnsubscriptionMessage tempMessage = new UnsubscriptionMessage(
							new Unsubscription(tmpSubMsg.getMessageID()), tmpSubMsg.getMessageID(),
							tmpSubMsg.getLastHopID());

					if (numberOfSRTRulesFired > 0) {
						// Must now parse the stream
						StringTokenizer tmpForwardingTargetsTokenizer = new StringTokenizer(
								forwardingTargetsBuffer.toString());
						readInBufferUnSub(tmpForwardingTargetsTokenizer, tmpSubMsg.getMessageID(),
								tempMatchingResult);
					}
					try {
						reteMatcherLogger.debug("Remove unsub from SRT.");
						SRT.unDefrule(messageID + "-" + subName);
					} catch (JessException e) {
						reteMatcherLogger.error("Failed to remove unsubsription from SRT: " + e);
						exceptionLogger.error("Failed to remove unsubsription from SRT: " + e);
					}
					matchingResult.put(tempMessage, tempMatchingResult);

				}

				try {
					reteMatcherLogger.debug("Remove uncompositesub from PRT.");
					PRT.unDefrule(messageID);
				} catch (JessException e) {
					reteMatcherLogger.error("Failed to remove uncompositesub from PRT: " + e);
					exceptionLogger.error("Failed to remove uncompositesub from PRT: " + e);
				}

			} else {
				reteMatcherLogger.warn("The uncompositesub was not in PRT.");
				exceptionLogger.warn("Here is an exception: ", new Exception(
						"The uncompositesub was not in PRT."));
			}

			return matchingResult;

		} catch (ParseException e) {
			reteMatcherLogger.error("Something went wrong in uncompositesubscription: " + e);
			exceptionLogger.error("Something went wrong in uncompositesubscription: " + e);
		} catch (JessException e) {
			reteMatcherLogger.error("Something went wrong in uncompositesubscription: " + e);
			exceptionLogger.error("Something went wrong in uncompositesubscription: " + e);
		}
		return null;
	}

	public void add(UnadvertisementMessage m) throws DuplicateMsgFoundException {
		// Right now, we did not retract any template in SRT and PRT, we only retract fact(adv) from
		// SRT, and rule(sub) from SRT and PRT.

		Map<String, UnadvertisementMessage> fullUnAdvertisementMessageIDMap = router.getUnAdvertisements();
		if (fullUnAdvertisementMessageIDMap.containsKey(m.getMessageID())) {
			// to avoid the unadvertisement loop in the network.
			throw new DuplicateMsgFoundException("Duplicate unadvertisement message is found : "
					+ m);
		} else {
			fullUnAdvertisementMessageIDMap.put(m.getMessageID(), m);
		}

		UnadvertisementMessage unadvMessage = m;
		String advMessageID = unadvMessage.getUnadvertisement().getAdvID();

		try {
			forwardingTargetsBuffer.reset();
			Fact factToRetract = advMessageIDToSRTFactMap.get(advMessageID);
			// WATCH OUT FOR UNADVERTISEMENTS TO THINGS NOT PREVIOUSLY ADVERTISED (or things already
			// unadvertised)!!! THE MATCHING ENGINE WILL PROCEED QUIETLY.
			if (factToRetract != null) {
				removeAdvAndSubMessages(factToRetract);
			} else {
				reteMatcherLogger.warn("The unadvertisement was not in SRT.");
				exceptionLogger.warn("Here is an exception: ", new Exception(
						"The unadvertisement was not in SRT."));
			}
		} catch (JessException e) {
			reteMatcherLogger.error("Something went wrong in unadvertise: " + e);
			exceptionLogger.error("Something went wrong in unadvertise: " + e);
		} catch (ParseException e) {
			reteMatcherLogger.error("Something went wrong in unadvertise: " + e);
			exceptionLogger.error("Something went wrong in unadvertise: " + e);
		}
	}

	private void defineUsefulFunctions() {
		try {
			// It would be nice if I could figure out how to construct these functions using the
			// Funcall class... maybe later These are all standard "<", "<=", ">", ">=" operations,
			// only for strings. Not too hard to decipher
			PRT.executeCommand("(deffunction str-lt (?a ?b) (if (< (str-compare ?a ?b) 0) then (return TRUE) else (return FALSE)))");
			PRT.executeCommand("(deffunction str-le (?a ?b) (if (<= (str-compare ?a ?b) 0) then (return TRUE) else (return FALSE)))");
			PRT.executeCommand("(deffunction str-gt (?a ?b) (if (> (str-compare ?a ?b) 0) then (return TRUE) else (return FALSE)))");
			PRT.executeCommand("(deffunction str-ge (?a ?b) (if (>= (str-compare ?a ?b) 0) then (return TRUE) else (return FALSE)))");

			// This is a bit odd. Jess's "str-index" answers the question
			// "where is 'a' present in 'b'?". We want "is 'b' within 'a'?" So I just define a
			// function to switch the order
			PRT.executeCommand("(deffunction str-contains (?a ?b) return (str-index ?b ?a))");

			// Standard prefix operation,
			// "if 'b' is present in 'a' at position one, then return true"
			PRT.executeCommand("(deffunction str-prefix (?a ?b) (if (eq (str-index ?b ?a) 1) then (return TRUE) else (return FALSE)))");

			// I realized that the postfix operation is not simply,
			// "if 'b' comes at the end of 'a', then return true" because Jess starts counting from
			// the start of the string, so "ab" will not be recognized as a postfix for "abcab". So
			// I define a reverse command, reverse both strings, and test if reverse of 'b' comes at
			// the start of reverse of 'a'.
			PRT.executeCommand("(deffunction str-reverse (?a) (bind ?i 1) (bind ?x \"\") (while (< ?i (+ (str-length ?a) 1)) do (bind ?x (str-cat ?x (sub-string (+ (- (str-length ?a) ?i) 1) (+ (- (str-length ?a) ?i) 1) ?a))) (bind ?i (+ ?i 1))) (return ?x))");
			PRT.executeCommand("(deffunction str-postfix (?a ?b) (if (eq (str-index (str-reverse ?b) (str-reverse ?a)) 1) then (return TRUE) else (return FALSE)))");

			PRT.addUserfunction(new IsPresent());
			PRT.addUserfunction(new Before());
			PRT.addUserfunction(new After());

			// The following functions are basically overloading the Jess numerical operators
			// (<,<=,=,>=,>,<>). THIS IS NOT A VERY GOOD IDEA, but I don't have another workaround
			// to the problem of trying to compare numbers and "nil". Without this, as soon as that
			// sort of comparison comes up, Jess will crap out, and miss potential matches.
			PRT.addUserfunction(new LessThan());
			PRT.addUserfunction(new LessThanOrEqualTo());
			PRT.addUserfunction(new EqualTo());
			PRT.addUserfunction(new GreaterThanOrEqualTo());
			PRT.addUserfunction(new GreaterThan());
			PRT.addUserfunction(new NotEqualTo());

			// The all-important overlaps function.
			SRT.addUserfunction(new Overlaps());
		} catch (JessException e) {
			reteMatcherLogger.error("Failed to define useful functions: " + e);
			exceptionLogger.error("Failed to define useful functions: " + e);
		}
	}

	/**
	 * This converts a publication message to a fake subscription message
	 */
	private SubscriptionMessage convertPubMsgToSubMsg(PublicationMessage pubMsg) {
		Publication tempPub = pubMsg.getPublication();
		Map<String, Serializable> pubPairMap = tempPub.getPairMap();
		Subscription tempSub = MessageFactory.createEmptySubscription();
		for (String attribute : pubPairMap.keySet()) {
			Serializable value = pubPairMap.get(attribute);
			String op = null;
			if (value.getClass().equals(String.class)) {
				op = "eq";
			} else {
				// value type is Long or Double
				op = "=";
			}
			Predicate tempPre = new Predicate(op, value);
			tempSub.addPredicate(attribute, tempPre);
		}
		String messageId = "Broker-M";
		SubscriptionMessage subMsg = new SubscriptionMessage(tempSub, messageId);
		subMsg.setLastHopID(pubMsg.getLastHopID());
		subMsg.setNextHopID(pubMsg.getNextHopID());
		return subMsg;
	}

	/**
	 * This method converts an advertisement to a fact and adds it to the SRT, defining a template
	 * for the fact if necessary.
	 * 
	 * @param advMessage
	 *            The advertisement to be converted and added
	 * @throws JessException
	 *             If something goes wrong in the Rete
	 */
	private void addAdvertisementToSRT(AdvertisementMessage advMessage) throws JessException {
		Map<String, Predicate> predicateMap = advMessage.getAdvertisement().getPredicateMap();
		String advMessageID = advMessage.getMessageID();

		boolean reassert = true;

		// Get the overall class of the advertisement
		String advertisementClass = (String) predicateMap.get("class").getValue();

		// Check if this advertisement class has already been used to define a template. If so, then
		// all facts associated with that class need to be reasserted, as Jess does not handle added
		// slots nicely.
		if ((getNewDeftemplate(advertisementClass, SRT)).equals(null))
			reassert = false;

		// Now convert the advertisement to a template, with multi-valued slots
		// (multislotted = true)
		Deftemplate newSRTTemplate = predicateMapToDeftemplate(predicateMap, SRT, true);

		// Need an extra slot to hold the last hop ID
		newSRTTemplate.addSlot("advertisementMsgID", new Value("msgID", RU.STRING), "STRING");
		newSRTTemplate.addSlot("advertisementLastHopID", new Value("lastHopIDNotSet", RU.STRING),
				"STRING");
		reteMatcherLogger.debug("Advertisment is converted to template : "
				+ newSRTTemplate.toString());

		// Add the template to the SRT
		try {
			reteMatcherLogger.debug("Adding template: " + newSRTTemplate.toString() + " to SRT.");
			SRT.addDeftemplate(newSRTTemplate);
		} catch (JessException e) {
			reteMatcherLogger.error("Failed to add template to PRT: " + e);
			exceptionLogger.error("Failed to add template to PRT: " + e);
		}

		if (reassert) {
			// This is where the facts are reasserted, if necessary
			reassertFactList(newSRTTemplate, SRT);

			// Unless the Rete is run again, and the forwarding targets buffer reset, this
			// reassertion will cause the rules to re-fire, and old forwarding targets to get into
			// the buffer, which we don't want.
			SRT.run();
			forwardingTargetsBuffer.reset();
		}

		// Convert advertisement message to Jess fact for the SRT
		Fact advertisementFact = null;
		try {
			reteMatcherLogger.debug("Converting advertisement to Jess fact.");
			advertisementFact = advertisementToFact(advMessage);
		} catch (JessException e) {
			reteMatcherLogger.error("Failed to convert advertisement to Jess fact: " + e);
			exceptionLogger.error("Failed to convert advertisement to Jess fact: " + e);
		}
		reteMatcherLogger.debug("Advertisement is converted to the fact: "
				+ advertisementFact.toString());

		// Assert advertisement fact to the SRT
		try {
			reteMatcherLogger.debug("Asserting fact into SRT.");
			SRT.assertFact(advertisementFact);
		} catch (JessException e) {
			reteMatcherLogger.error("Failed to assert fact to SRT: " + e);
			exceptionLogger.error("Failed to assert fact to SRT: " + e);
		}

		// Keep track of fact's message ID
		advMessageIDToSRTFactMap.put(advMessageID, advertisementFact);
		SRTFactToAdvMessageIDMap.put(advertisementFact, advMessageID);
	}

	/**
	 * This method converts an advertisement to a deftemplate and adds it to the PRT. Note that the
	 * deftemplate defined is for facts converted from publications
	 * 
	 * @param advMessage
	 *            The advertisement to be converted and added
	 * @throws JessException
	 *             If something goes wrong in the Rete
	 */
	private void addAdvertisementToPRT(AdvertisementMessage advMessage) throws JessException {
		Map<String, Predicate> predicateMap = advMessage.getAdvertisement().getPredicateMap();
		boolean reassert = true;

		// Get the overall class of the advertisement
		String advertisementClass = (String) predicateMap.get("class").getValue();

		// Check if this advertisement class has already been used to define a template. If so, then
		// all facts associated with that class need to be reasserted, as Jess does not handle added
		// slots nicely.
		if ((getNewDeftemplate(advertisementClass, PRT)).equals(null))
			reassert = false;

		// Now convert the advertisement to a template, with single-valued slots
		// (multislotted = false)
		Deftemplate newPRTTemplate = predicateMapToDeftemplate(predicateMap, PRT, false);

		// Need to add a slot for the message ID so that we can set this value in the fact
		newPRTTemplate.addSlot("_publicationMessageString", new Value("nil", RU.ATOM), "STRING");
		reteMatcherLogger.debug("Advertisment is converted to template : "
				+ newPRTTemplate.toString());
		// Add the template to the PRT
		try {
			reteMatcherLogger.debug("Adding template: " + newPRTTemplate.toString() + " to PRT.");
			PRT.addDeftemplate(newPRTTemplate);
		} catch (JessException e) {
			reteMatcherLogger.error("Failed to add template to PRT: " + e);
			exceptionLogger.error("Failed to add template to PRT: " + e);
		}

		if (reassert) {
			// This is where the facts are reasserted, if necessary
			reassertFactList(newPRTTemplate, PRT);

			// Unless the Rete is run again, and the forwarding targets buffer reset, this
			// reassertion will cause the rules to re-fire, and old forwarding targets to get into
			// the buffer, which we don't want.
			PRT.run();
			forwardingTargetsBuffer.reset();
		}
	}

	/**
	 * Adds a subscription message to the SRT as a rule, creating a template if necessary
	 * 
	 * @param subMessage
	 *            The subscription message to add
	 * @throws JessException
	 *             If anything goes wrong in Jess
	 */
	private void addSubscriptionToSRT(SubscriptionMessage subMessage) throws JessException {
		Map<String, Predicate> predicateMap = subMessage.getSubscription().getPredicateMap();
		boolean reassert = true;

		// Get the overall class of the advertisement
		String subscriptionClass = (String) predicateMap.get("class").getValue();

		// Check if this subscription class has already been used to define a template. If so, then
		// all facts associated with that class need to be reasserted, as Jess does not handle added
		// slots nicely.
		if ((getNewDeftemplate(subscriptionClass, SRT)).equals(null))
			reassert = false;

		// Now convert the subscription to a template, with multi-valued slots
		// (multislotted = true)
		Deftemplate newSRTTemplate = predicateMapToDeftemplate(predicateMap, SRT, true);

		// Need an extra slot to hold the last hop ID
		newSRTTemplate.addSlot("advertisementLastHopID", new Value("lastHopIDNotSet", RU.STRING),
				"STRING");
		newSRTTemplate.addSlot("advertisementMsgID", new Value("msgID", RU.STRING), "STRING");
		// Add the template to the SRT
		SRT.addDeftemplate(newSRTTemplate);

		if (reassert) {
			// This is where the facts are reasserted, if necessary
			reassertFactList(newSRTTemplate, SRT);

			// Unless the Rete is run again, and the forwarding targets buffer reset, this
			// reassertion will cause the rules to re-fire, and old forwarding targets to get into
			// the buffer, which we don't want.
			SRT.run();
			forwardingTargetsBuffer.reset();
		}

		String SRTRule = subscriptionMessageToSRTDefrule(subMessage);
		reteMatcherLogger.debug("The subscription is converted to the rule : " + SRTRule);
		reteMatcherLogger.debug("Inserting this rule to SRT.");
		SRT.executeCommand(SRTRule);
	}

	/*
	 * addSubscriptionToPRT
	 */

	private void addSubscriptionToPRT(SubscriptionMessage subMessage) throws JessException {
		// Convert subscription message to Jess rule
		String PRTRule = subscriptionToPRTDefrule(subMessage.getSubscription(),
				subMessage.getMessageID(), subMessage.getLastHopID(), "");
		// Now try and assert the rule to the publication matching engine
		PRT.executeCommand(PRTRule);

	}

	private void addCompositeSubscriptionMessageToPRT(CompositeSubscriptionMessage csSubMsg)
			throws JessException {

		Map<String, Subscription> subscriptions = csSubMsg.getSubscription().getSubscriptionMap();
		boolean reassert;
		for (Subscription atomSub : subscriptions.values()) {
			Map<String, Predicate> predicateMap = atomSub.getPredicateMap();
			reassert = true;

			// Get the overall class of the subscription
			String subscriptionClass = (String) predicateMap.get("class").getValue();

			// Check if this subscription class has already been used to define a template. If so,
			// then all facts associated with that class need to be reasserted, as Jess does not
			// handle added slots nicely.
			if ((getNewDeftemplate(subscriptionClass, PRT)).equals(null))
				reassert = false;

			// Now convert the advertisement to a template, with single-valued slots (multislotted =
			// false)
			Deftemplate newPRTTemplate = predicateMapToDeftemplate(predicateMap, PRT, false);
			// Need to add a slot for the message ID so that we can set this value in the fact
			newPRTTemplate.addSlot("_publicationMessageString", new Value("nil", RU.ATOM), "STRING");
			reteMatcherLogger.debug("Subscription is converted to template : "
					+ newPRTTemplate.toString());

			// Add the template to the PRT
			try {
				reteMatcherLogger.debug("Adding template: " + newPRTTemplate.toString()
						+ " to PRT.");
				PRT.addDeftemplate(newPRTTemplate);
			} catch (JessException e) {
				reteMatcherLogger.error("Failed to add template to PRT: " + e);
				exceptionLogger.error("Failed to add template to PRT: " + e);
			}

			if (reassert) {
				// This is where the facts are reasserted, if necessary
				reassertFactList(newPRTTemplate, PRT);

				// Unless the Rete is run again, and the forwarding targets buffer reset, this
				// reassertion will cause the rules to re-fire, and old forwarding targets to get
				// into the buffer, which we don't want.
				PRT.run();
				forwardingTargetsBuffer.reset();
			}

		}
		String PRTRule = compositeSubscriptionToPRTDefrule(csSubMsg);
		reteMatcherLogger.debug("The composite subscription is converted to the PRTRule : "
				+ PRTRule.toString());
		reteMatcherLogger.debug("Inserting this rule to PRT.");
		PRT.executeCommand(PRTRule);
	}

	/**
	 * This function is used to convert advertisements to facts for use in subscription routing
	 * (SRT)
	 * 
	 * @param advMessage
	 *            The advertisement to convert
	 * @return The fact that should be asserted to the SRT
	 * @throws JessException
	 */
	private Fact advertisementToFact(AdvertisementMessage advMessage) throws JessException {
		String advertisementLastHopID = (advMessage.getLastHopID()).getDestinationID();

		String predicateOp;
		Object predicateValue;

		Map<String, Predicate> predicateMap = advMessage.getAdvertisement().getPredicateMap();
		String advertisementClass = (String) predicateMap.get("class").getValue();
		Fact factToAssert = new Fact(advertisementClass, SRT);
		if (brokerCore.isCycle()) {
			String advTID = (String) predicateMap.get("tid").getValue();
			factToAssert.setSlotValue("tid", new Value(advTID, RU.STRING));
		}

		for (String predicateAttribute : predicateMap.keySet()) {
			predicateOp = (String) predicateMap.get(predicateAttribute).getOp();
			predicateValue = predicateMap.get(predicateAttribute).getValue();

			if ((!predicateAttribute.equals("class")) && (!predicateAttribute.equals("tid"))) {
				ValueVector opValue = new ValueVector();
				opValue.add(new Value(predicateOp, RU.ATOM));

				if (predicateValue.getClass().equals(Byte.class)) {
					opValue.add(new Value(((Byte) predicateValue).intValue(), RU.INTEGER));
				} else if (predicateValue.getClass().equals(Short.class)) {
					opValue.add(new Value(((Short) predicateValue).intValue(), RU.INTEGER));
				} else if (predicateValue.getClass().equals(Integer.class)) {
					opValue.add(new Value(((Integer) predicateValue).intValue(), RU.INTEGER));
				} else if (predicateValue.getClass().equals(Long.class)) {
					opValue.add(new Value(((Long) predicateValue).intValue(), RU.INTEGER));
				} else if (predicateValue.getClass().equals(Float.class)) {
					opValue.add(new Value(((Float) predicateValue).doubleValue(), RU.FLOAT));
				} else if (predicateValue.getClass().equals(Double.class)) {
					opValue.add(new Value(((Double) predicateValue).doubleValue(), RU.FLOAT));
				} else if (predicateValue.getClass().equals(String.class)) {
					// This is to deal with the Jess not having a dedicated Boolean type.
					if (((String) predicateValue).equals("TRUE")
							|| ((String) predicateValue).equals("FALSE")) {
						opValue.add(new Value((String) predicateValue, RU.ATOM));
					} else {
						opValue.add(new Value((String) predicateValue, RU.STRING));
					}
				} else if (predicateValue.getClass().equals(Boolean.class)) {
					opValue.add(new Value(((Boolean) predicateValue).booleanValue()));
				} else if (predicateValue.getClass().equals(Date.class)) {
					opValue.add(new Value(((Date) predicateValue).toString(), RU.STRING));
				} else if (predicateValue.getClass().equals(Object.class)) {
					opValue.add(new Value(predicateValue));
				}

				factToAssert.setSlotValue(predicateAttribute, new Value(opValue, RU.LIST));
			}
		}

		factToAssert.setSlotValue("advertisementMsgID", new Value(advMessage.getMessageID(),
				RU.STRING));
		factToAssert.setSlotValue("advertisementLastHopID", new Value(advertisementLastHopID,
				RU.STRING));

		// Facts need a unique ID to ensure that all rules fire, even when a duplicate publication
		// comes in.
		factToAssert.setSlotValue("_uniqueID", new Value(factCounter++, RU.INTEGER));

		return factToAssert;
	}

	/**
	 * This converts a predicate map, which is a set of attribute, operator, value triplets, to a
	 * template for future facts
	 * 
	 * @param predicateMap
	 *            The predicate map you wish to convert
	 * @param reteOfInterest
	 *            The Rete in which you wish to define the template
	 * @param multislotted
	 *            A boolean that should be set to true if the template is to have multislots, false
	 *            otherwise
	 * @return A template to define. Jess has a construct for this (unlike rules)
	 * @throws JessException
	 */
	private Deftemplate predicateMapToDeftemplate(Map<String, Predicate> predicateMap,
			Rete reteOfInterest, boolean multislotted) throws JessException {
		// Get the overall class of the advertisement
		String classOfPredicateMap = (String) predicateMap.get("class").getValue();

		// This is a nice way to enable more than one person to advertise the same class of event.
		Deftemplate template = getNewDeftemplate(classOfPredicateMap, reteOfInterest);

		// Iterate through the predicates and add the attributes as slots. If slot exists, nothing
		// really happens. Something would probably break if two people had the same attribute name,
		// but a different value type
		for (String predicateAttribute : predicateMap.keySet()) {
			Object predicateValue = predicateMap.get(predicateAttribute).getValue();

			if ((!predicateAttribute.equals("class")) && (!predicateAttribute.equals("tid"))) {
				Class<?> predicateValueClass = predicateValue.getClass();
				if (predicateValueClass.equals(String.class)) {
					predicateValueClass = getPredicateValueClass("" + predicateValue);
				}
				// If it's a whole number, add a slot with default value nil,
				// default type integer
				if (predicateValueClass.equals(Integer.class)
						|| predicateValueClass.equals(Byte.class)
						|| predicateValueClass.equals(Short.class)) {
					if (multislotted) {
						template.addMultiSlot(predicateAttribute, new Value("nil", RU.ATOM));
					} else {
						template.addSlot(predicateAttribute, new Value("nil", RU.ATOM), "INTEGER");
					}
				} else if (predicateValueClass.equals(Long.class)) {
					if (multislotted) {
						template.addMultiSlot(predicateAttribute, new Value("nil", RU.ATOM));
					} else {
						template.addSlot(predicateAttribute, new Value("nil", RU.ATOM), "INTEGER");
					}
				} else if (predicateValueClass.equals(Double.class)
						|| predicateValueClass.equals(Float.class)) {
					// If it's a floating point number, add a slot with default value nil, default
					// type float
					if (multislotted) {
						template.addMultiSlot(predicateAttribute, new Value("nil", RU.ATOM));
					} else {
						template.addSlot(predicateAttribute, new Value("nil", RU.ATOM), "FLOAT");
					}
				} else if (predicateValueClass.equals(String.class)) {
					// If it's a String, check if it's really a boolean. If so, give it a default
					// value nil, default type atom. If not, give it a default value nil, default
					// type String
					if (((String) predicateValue).equals("TRUE")
							|| ((String) predicateValue).equals("FALSE")) {
						if (multislotted) {
							template.addMultiSlot(predicateAttribute, new Value("nil", RU.ATOM));
						} else {
							template.addSlot(predicateAttribute, new Value("nil", RU.ATOM), "ATOM");
						}
					} else {
						if (multislotted) {
							template.addMultiSlot(predicateAttribute, new Value("nil", RU.ATOM));
						} else {
							template.addSlot(predicateAttribute, new Value("nil", RU.ATOM),
									"STRING");
						}
					}
				} else if (predicateValueClass.equals(Boolean.class)) {
					if (multislotted) {
						template.addMultiSlot(predicateAttribute, new Value("nil", RU.ATOM));
					} else {
						template.addSlot(predicateAttribute, new Value("nil", RU.ATOM), "ATOM");
					}
					// When all else fails, just pass it an object.
				} else if (predicateValueClass.equals(Date.class)) {
					if (multislotted) {
						template.addMultiSlot(predicateAttribute, new Value("nil", RU.ATOM));
					} else {
						template.addSlot(predicateAttribute, new Value("nil", RU.ATOM), "STRING");
					}
				} else {
					if (multislotted) {
						template.addMultiSlot(predicateAttribute, new Value("nil", RU.ATOM));
					} else {
						template.addSlot(predicateAttribute, new Value("nil", RU.ATOM), "ANY");
					}
				}
			} else if (predicateAttribute.equals("tid")) {
				template.addSlot("tid", new Value("nil", RU.STRING), "STRING");
			}
		}

		// Facts will need a unique field to ensure that all rules fire, so they need a slot
		template.addSlot("_uniqueID", new Value("nil", RU.ATOM), "INTEGER");
		return template;
	}

	/**
	 * This function checks if there's an existing deftemplate by this class name. If yes, it
	 * returns it, if no, it returns a new deftemplate
	 * 
	 * @param advertisementClass
	 * @return
	 * @throws JessException
	 */
	private Deftemplate getNewDeftemplate(String classOfPredicateMap, Rete reteOfInterest)
			throws JessException {

		Deftemplate dt = reteOfInterest.findDeftemplate(classOfPredicateMap);

		if (dt == null) {
			return new Deftemplate(classOfPredicateMap, classOfPredicateMap, reteOfInterest);
		} else {
			return dt;
		}
	}

	/**
	 * 
	 * @param sub
	 *            A subscription in the native binding language
	 * @return A string fit to execute as a Jess command
	 * @throws JessException
	 */
	private String subscriptionToPRTDefrule(Subscription subMessage, String messageID,
			MessageDestination lastHopID, String csFlag) throws JessException {

		PRT.addOutputRouter("forwardingTargetsBuffer", forwardingTargetsBuffer);

		// Get the list of all predicates in the simple subscription
		Map<String, Predicate> predicateMap = subMessage.getPredicateMap();

		// Get the info necessary to name the rule (message ID and last hop ID)
		// String messageID = subMessage.getMessageID();
		// MessageDestination lastHopID = subMessage.getLastHopID();

		// Get the class (for simple subscription, there's only one class)
		String subscriptionClass = (String) predicateMap.get("class").getValue();

		if (brokerCore.isCycle()) {
			// we have to distinguish rules with the same messageID, e.g., BROKER_INF subscriptions,
			// by simply attaching the tid value of this subscription
			String tidValue = (String) predicateMap.get("tid").getValue();
			if (!tidValue.startsWith("$S$Tid")) {
				// messageID = messageID + "_" + tidValue;
			}
		}

		// Define a string to store the command, which is a rule named messageID with a class
		// subscriptionClass
		String rule = "(defrule " + messageID + " (" + subscriptionClass;

		// Define a string to store the publication, once it comes in, so that
		// we can print it out to the buffer
		// String publicationString = "[class," + subscriptionClass + "]";

		// Iterate through all predicates, and concatenate the appropriate slots
		for (String predicateAttribute : predicateMap.keySet()) {
			String predicateOp = (String) predicateMap.get(predicateAttribute).getOp();
			Object predicateValue = predicateMap.get(predicateAttribute).getValue();

			if (!predicateAttribute.equals("class")) {
				String variableName = "?x_" + variableCounter;
				variableCounter++;
				// If the value is a string, we have to be careful that Jess interprets it as a
				// string, and not an atom. This is, of course, unless it's the string TRUE or
				// FALSE, in which case we do want it to treat it as an atom. This wouldn't be such
				// an issue if there were a good API for creating rules, but there isn't (yet)
				if (predicateValue.getClass().equals(String.class)) {
					if (!(predicateValue.equals("TRUE") || predicateValue.equals("FALSE"))) {
						String predicateString = predicateValue.toString();
						if (isVariable(predicateString)) {
							int dolar_index = predicateString.lastIndexOf(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_VARIBLES);
							predicateValue = "" + predicateString.substring(dolar_index + 1);
							if ((brokerCore.isCycle()) && predicateAttribute.equals("tid")) {
								rule = rule + " (tid ?Tid" + variableCounter + ") ";
								variableCounter++;
							} else {
								rule = rule + " (" + predicateAttribute + " " + "?"
										+ predicateValue + ")";
							}
						} else {
							predicateValue = "\"" + predicateValue + "\"";
							rule = rule + " (" + predicateAttribute + " " + variableName + "&:("
									+ predicateOp + " " + variableName + " " + predicateValue
									+ "))";
						}
					}
				} else {
					rule = rule + " (" + predicateAttribute + " " + variableName + "&:("
							+ predicateOp + " " + variableName + " " + predicateValue + "))";
				}

			}
		}

		rule = rule + " (_publicationMessageString ?" + messageID + "pubMessageString" + csFlag
				+ ")";

		rule = rule + ") => (printout forwardingTargetsBuffer " + messageID + "\" \" ?" + messageID
				+ "pubMessageString crlf) )";

		return rule;
	}

	/**
	 * This function converts a subscription to a defrule to be asserted to the SRT. This takes a
	 * different form than the defrule that is asserted to the PRT. The range defined by the
	 * operator and value part of each predicate is checked against the range defined by the
	 * operator and value of the incoming advertisement
	 * 
	 * @param subMessage
	 * @return
	 * @throws JessException
	 */
	private String subscriptionMessageToSRTDefrule(SubscriptionMessage subMessage)
			throws JessException {

		SRT.addOutputRouter("forwardingTargetsBuffer", forwardingTargetsBuffer);

		// Get the list of all predicates in the simple subscription
		Map<String, Predicate> predicateMap = subMessage.getSubscription().getPredicateMap();

		// Get the info necessary to name the rule (message ID and last hop ID)
		String messageID = subMessage.getMessageID();

		// Get the class (for simple subscription, there's only one class)
		String subscriptionClass = (String) predicateMap.get("class").getValue();

		// Define a string to store the command, which is a rule named messageID with a class
		// subscriptionClass
		String rule = "(defrule " + messageID + " (" + subscriptionClass;

		boolean tidFlag = false;
		// Iterate through all predicates, and concatenate the appropriate slots
		for (String predicateAttribute : predicateMap.keySet()) {
			String predicateOp = (String) predicateMap.get(predicateAttribute).getOp();
			Object predicateValue = predicateMap.get(predicateAttribute).getValue();

			// If the attribute is not class, add it on, with the initial value of "present". This
			// is because when facts are asserted, if there are missing slots, Jess will append
			// those slots with default value "nil", so a keyword is necessary to tell the matching
			// engine if the slot is filled. Any word would do (other than nil), as long as it
			// matches the word used in advertisementToFact below
			if (isVariable(predicateValue.toString())) {
				if (!predicateAttribute.equals("tid")) {
					predicateOp = "isPresent";
				}
				// predicateValue = setValueClass(valueClass);
			}

			boolean tempFlag = false;
			if (brokerCore.isCycle() && (predicateAttribute.equals("tid"))) {
				if (((String) predicateValue).startsWith("$S$Tid")) {
					// the tid attribute need to be transfered to different rule
					tidFlag = true;
					tempFlag = true;
				}
			}

			String predicateValueString;
			if (!predicateAttribute.equals("class") && (!predicateAttribute.equals("tid"))) {
				if ((predicateValue.getClass()).equals(String.class)) {
					if (!(predicateValue.equals("TRUE") || predicateValue.equals("FALSE") || isVariable(predicateValue.toString()))) {
						predicateValueString = "\"" + predicateValue.toString() + "\"";
					} else if (isVariable(predicateValue.toString())) {
						Class<?> valueClass = getPredicateValueClass(predicateValue.toString());
						if (valueClass.equals(String.class)) {
							predicateValueString = "\"any\"";
						} else {
							predicateValueString = "0";
						}
					} else {
						predicateValueString = predicateValue.toString();
					}
				} else if ((predicateValue.getClass()).equals(Date.class)) {
					predicateValueString = "\"" + predicateValue.toString() + "\"";
				} else {
					predicateValueString = predicateValue.toString();
				}

				rule = rule + " (" + predicateAttribute + " ?x" + variableCounter + " ?y"
						+ variableCounter + "&:(overlaps ?x" + variableCounter + " ?y"
						+ variableCounter + " " + predicateOp + " " + predicateValueString + ")) ";
				variableCounter++;
			} else if (predicateAttribute.equals("tid") && (!tempFlag)) {
				predicateValueString = "\"" + predicateValue.toString() + "\"";
				rule = rule + " (" + predicateAttribute + " " + predicateValueString + ") ";
			}
		}

		if (tidFlag) {
			rule = rule + " (tid ?tid" + variableCounter + ") ";
			variableCounter++;
		}
		rule = rule + "(advertisementMsgID ?advMsgID" + variableCounter + ")";
		variableCounter++;
		rule = rule + "(advertisementLastHopID ?advLastHopID" + variableCounter + ")";

		String subMessageString = subMessage.toString().replaceAll("\"", "'");

		// Now concatenate the RHS of the rule. Play around with Jess's printout command before
		// changing this line, as it's not immediately obvious why it behaves as it does. Try
		// (printout t dave dave crlf) then (printout t dave " dave" crlf).
		if (brokerCore.isCycle()) {
			rule = rule + ") => (printout forwardingTargetsBuffer ?advLastHopID" + variableCounter
					+ "\" \"" + "?advMsgID" + (variableCounter - 1) + "\" \"" + " ?tid"
					+ (variableCounter - 2) + "\" " + subMessageString + "\" crlf) )";
		} else {
			rule = rule + ") => (printout forwardingTargetsBuffer ?advLastHopID" + variableCounter
					+ "\" \"" + "?advMsgID" + (variableCounter - 1) + "\" " + subMessageString
					+ "\" crlf) )";
		}
		variableCounter++;

		return rule;
	}

	/**
	 * 
	 * @param subMessage
	 * @param messageID
	 * @param lastHopID
	 * @param csFlag
	 * @return
	 * @throws JessException
	 */
	private String compositeSubscriptionToPRTDefrule(CompositeSubscriptionMessage csSubMes) {
		String messageID = csSubMes.getMessageID();
		// MessageDestination lastHopID = csSubMes.getLastHopID();
		CompositeSubscription csSub = csSubMes.getSubscription();
		Map<String, Subscription> subscriptions = csSub.getSubscriptionMap();
		PRT.getGlobalContext();

		String rule = "( defrule " + messageID + " ";

		rule = rule + treeToString(csSub.getRoot(), subscriptions, messageID)
				+ " => (printout forwardingTargetsBuffer ";

		int index = csSub.getAtomicSubscriptionNumber();

		for (int i = 1; i <= index; i++) {
			rule = rule + messageID + "\" \"  " + "?" + messageID + "pubMessageStrings" + i
					+ " crlf ";
		}

		rule = rule + " ) )";

		return rule;
	}

	private SubscriptionMessage SRTDefruleToSubscriptionMessage(Defrule defruleToConvert) throws ParseException {

		String rightHandSide = defruleToConvert.getAction(0).toString();

		// The right hand side will always have the subscription message surrounded by quotes. The +
		// 2 is so that the string doesn't have the quote and the space at the start.
		int start = brokerCore.isCycle() ? rightHandSide.indexOf("tid")
				: rightHandSide.indexOf("advMsgID");
		rightHandSide = rightHandSide.substring(start);
		start = rightHandSide.indexOf("\"") + 2;
		int end = rightHandSide.lastIndexOf("\"");

		SubscriptionMessage subMessageToReturn = new SubscriptionMessage(rightHandSide.substring(
				start, end));

		return subMessageToReturn;
	}

	/**
	 * This is to check if any facts were asserted using an old template. If so, they need to be
	 * reasserted using the new template. This is a peculiarity of Jess, which I believe is a bug.
	 * 
	 * @param newTemplate
	 * @param reteOfInterest
	 * @throws JessException
	 */
	@SuppressWarnings("unchecked")
	private void reassertFactList(Deftemplate newTemplate, Rete reteOfInterest)
			throws JessException {
		Runtime.getRuntime();
		for (Iterator i = reteOfInterest.listFacts(); i.hasNext();) {
			Fact f = (Fact) i.next();
			String advMessageID = " ";
			if (((f.getDeftemplate()).getBaseName()).equals(newTemplate.getBaseName())) {
				// If we're retracting and reasserting facts in the SRT, we have to update the
				// advertisement message ID to fact mapping
				if (reteOfInterest.equals(SRT)) {
					advMessageID = SRTFactToAdvMessageIDMap.get(f);
				}
				reteOfInterest.retract(f);
				Fact newFact = new Fact(f.getName(), reteOfInterest);
				for (int j = 0; j < f.size(); j++) {
					newFact.set(f.get(j), j);
				}
				reteOfInterest.assertFact(newFact);
				if (reteOfInterest.equals(SRT)) {
					advMessageIDToSRTFactMap.put(advMessageID, newFact);
					SRTFactToAdvMessageIDMap.put(newFact, advMessageID);
					SRTFactToAdvMessageIDMap.remove(f);
				}
			}
		}
		// Now's probably a good time to garbage collect, except it goes slooooooowww
		// rc.gc();
	}

	/**
	 * A variable in PADRES should be $S$X; S is the type of the variable, X is the name of
	 * variable;
	 * 
	 * @param value
	 * @return
	 */
	private Class<?> getPredicateValueClass(String value) {
		Class<?> valueClass = String.class;
		value = value.trim();

		String valueTmp;
		int index_1 = value.indexOf(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_VARIBLES);

		valueTmp = value.substring(index_1 + 1);
		int index_2 = valueTmp.indexOf(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_VARIBLES);

		if ((index_1 == 0) && ((index_2 == 1) || (index_2 == 2))) {
			String op = valueTmp.substring(0, index_2);

			valueTmp.substring(index_2 + 1);

			if (op.equals(CompositeSubscriptionOPs.VARIBLE_CLASS_BYTE)) {
				valueClass = Byte.class;
			} else if (op.equals(CompositeSubscriptionOPs.VARIBLE_CLASS_DOUBLE)) {
				valueClass = Double.class;
			} else if (op.equals(CompositeSubscriptionOPs.VARIBLE_CLASS_FLOAT)) {
				valueClass = Float.class;
			} else if (op.equals(CompositeSubscriptionOPs.VARIBLE_CLASS_INTEGER)) {
				valueClass = Integer.class;
			} else if (op.equals(CompositeSubscriptionOPs.VARIBLE_CLASS_LONG)) {
				valueClass = Long.class;
			} else if (op.equals(CompositeSubscriptionOPs.VARIBLE_CLASS_SHORT)) {
				valueClass = Short.class;
			} else if (op.equals(CompositeSubscriptionOPs.VARIBLE_CLASS_STRING)) {
				valueClass = String.class;
			} else if (op.equals(CompositeSubscriptionOPs.VARIBLE_CLASS_DATE)) {
				valueClass = Date.class;
			}

		}

		return valueClass;
	}

	private boolean isVariable(String value) {
		boolean flag = false;
		String valueTmp;
		int index_1 = value.indexOf(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_VARIBLES);
		valueTmp = value.substring(index_1 + 1);
		int index_2 = valueTmp.indexOf(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_VARIBLES);
		if ((index_1 == 0) && ((index_2 == 1) || (index_2 == 2))) {
			flag = true;
		}

		return flag;
	}

	/**
	 * Converts a publication message to a Jess fact
	 * 
	 * @param pub
	 *            A publication in our native binding language
	 * @return A Jess fact
	 * @throws JessException
	 */
	private Fact publicationToFact(PublicationMessage pub) throws JessException {
		Map<String, Serializable> pairMap = pub.getPublication().getPairMap();
		String publicationClass = (String) pairMap.get("class");

		Fact factToAssert = new Fact(publicationClass, PRT);

		for (String pairAttribute : pairMap.keySet()) {
			Object pairValue = pairMap.get(pairAttribute);
			if (!pairAttribute.equals("class")) {
				// Need to know the type of the value, otherwise cannot do comparison operations.
				// Note that Jess's FLOAT is actually a double float
				if (pairValue.getClass().equals(Byte.class)) {
					factToAssert.setSlotValue(pairAttribute,
							new Value(((Byte) pairValue).intValue(), RU.INTEGER));
				} else if (pairValue.getClass().equals(Short.class)) {
					factToAssert.setSlotValue(pairAttribute,
							new Value(((Short) pairValue).intValue(), RU.INTEGER));
				} else if (pairValue.getClass().equals(Integer.class)) {
					factToAssert.setSlotValue(pairAttribute,
							new Value(((Integer) pairValue).intValue(), RU.INTEGER));
				} else if (pairValue.getClass().equals(Long.class)) {
					factToAssert.setSlotValue(pairAttribute,
							new Value(((Long) pairValue).intValue(), RU.INTEGER));
				} else if (pairValue.getClass().equals(Float.class)) {
					factToAssert.setSlotValue(pairAttribute,
							new Value(((Float) pairValue).doubleValue(), RU.FLOAT));
				} else if (pairValue.getClass().equals(Double.class)) {
					factToAssert.setSlotValue(pairAttribute,
							new Value(((Double) pairValue).doubleValue(), RU.FLOAT));
				} else if (pairValue.getClass().equals(String.class)) {
					// This is to deal with the Jess not having a dedicated Boolean type.
					if (((String) pairValue).equals("TRUE") || ((String) pairValue).equals("FALSE")) {
						factToAssert.setSlotValue(pairAttribute, new Value((String) pairValue,
								RU.ATOM));
					} else {
						factToAssert.setSlotValue(pairAttribute, new Value((String) pairValue,
								RU.STRING));
					}
				} else if (pairValue.getClass().equals(Boolean.class)) {
					factToAssert.setSlotValue(pairAttribute,
							new Value(((Boolean) pairValue).booleanValue()));
				} else if (pairValue.getClass().equals(Date.class)) {
					factToAssert.setSlotValue(pairAttribute,
							new Value(((Date) pairValue).toString(), RU.STRING));
				} else if (pairValue.getClass().equals(Object.class)) {
					factToAssert.setSlotValue(pairAttribute, new Value(pairValue));
				}
			}
		}

		// Facts need a unique ID to ensure that all rules fire, even when a duplicate publication
		// comes in.
		factToAssert.setSlotValue("_uniqueID", new Value(factCounter++, RU.INTEGER));
		factToAssert.setSlotValue("_publicationMessageString", new Value(pub.toString(), RU.STRING));

		return factToAssert;
	}

	private String treeToString(CompositeNode root, Map<String, Subscription> subMap, String msgID) {
		String subRule = "";
		MessageDestination lastHopID = null;
		CompositeNode workNode = root;
		if (workNode.leftNode == null && workNode.rightNode == null) {
			String subName = workNode.content;
			Subscription sub = (Subscription) subMap.get(subName);
			try {
				subRule = subscriptionToPRTDefrule(sub, msgID, lastHopID, subName);
				subRule = getSubscriptionDefrule(subRule);
			} catch (JessException e) {
				e.printStackTrace();
			}
		} else if (workNode.content.equals(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_AND)) {
			subRule = " (and" + " " + treeToString(workNode.leftNode, subMap, msgID) + " "
					+ treeToString(workNode.rightNode, subMap, msgID) + ")";
		} else if (workNode.content.equals(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_OR)) {

			subRule = " (or" + " " + treeToString(workNode.leftNode, subMap, msgID) + " "
					+ treeToString(workNode.rightNode, subMap, msgID) + ")";
		}

		return subRule;
	}

	private String getSubscriptionDefrule(String defRule) {
		int index_1 = defRule.indexOf("(");
		int index_last = defRule.lastIndexOf("=>");
		String newRule = defRule.substring(index_1 + 1, index_last);
		index_1 = newRule.indexOf("(");
		newRule = newRule.substring(index_1);

		return newRule;
	}

	private void readInBufferSubAdv(StringTokenizer forwardingTargetsTokenizer, MessageType type,
			Set<Message> messageWithNextHopIDSet, Set<String> matchingResult) throws JessException, ParseException {

		if (reteMatcherLogger.isDebugEnabled())
			reteMatcherLogger.debug("forwardingTargetsBuffer is: "
					+ forwardingTargetsBuffer.toString());

		while (forwardingTargetsTokenizer.hasMoreElements()) {
			forwardingTargetsTokenizer.nextToken(" ").toString(); // ignore lasthop
			String advMsgID = forwardingTargetsTokenizer.nextToken(" ").toString();
			if (brokerCore.isCycle()) {
				// under cycles, forwardingTargetsTokenizer is different only for subscription, one
				// more ?tid field is added; ignore it
				forwardingTargetsTokenizer.nextToken(" ").toString();
			}

			String messageString = forwardingTargetsTokenizer.nextToken("\n").toString().trim();
			Message tempMessage = new SubscriptionMessage(messageString);
			messageWithNextHopIDSet.add(tempMessage);
			if (type.equals(MessageType.SUBSCRIPTION)) {
				// matchingResult.add(advMsgID);
				matchingResult.add(advMsgID);
			} else if (type.equals(MessageType.ADVERTISEMENT)) {
				matchingResult.add(tempMessage.getMessageID());
			}
		}
		if (reteMatcherLogger.isDebugEnabled())
			reteMatcherLogger.debug("messageWithNextHopIDSet is: "
					+ messageWithNextHopIDSet.toString());

	}

	/**
	 * Reads in the string buffer, which is next hop ID - message string pairs, and puts the
	 * resulting messages in messageWithNextHopIDSet, if they are to be added to other matching
	 * engines, and messagesToRoute, if they are messages which are to be forwarded on
	 * 
	 * @param forwardingTargetsTokenizer
	 * @param type
	 * @param messageWithNextHopIDSet
	 * @param messagesToRoute
	 * @throws JessException
	 */
	private void readInBufferPub(StringTokenizer forwardingTargetsTokenizer,
			Map<PublicationMessage, Set<String>> matchingResult) throws JessException {
		Map<String, Set<String>> tmpMatchingResult = new HashMap<String, Set<String>>();
		if (reteMatcherLogger.isDebugEnabled())
			reteMatcherLogger.debug("forwardingTargetsBuffer is: "
					+ forwardingTargetsBuffer.toString());
		while (forwardingTargetsTokenizer.hasMoreElements()) {
			String messageID = (forwardingTargetsTokenizer.nextToken(" ")).toString();
			if (messageID.charAt(0) == '\n') {
				messageID = messageID.substring(1);
			}
			String messageString = (forwardingTargetsTokenizer.nextToken("\n")).toString().trim();
			if (!messageString.equals("nil")) {
				if (tmpMatchingResult.containsKey(messageString)) {
					Set<String> subMsgIDs = tmpMatchingResult.get(messageString);
					subMsgIDs.add(messageID);
				} else {
					Set<String> subMsgIDs = new HashSet<String>();
					subMsgIDs.add(messageID);
					tmpMatchingResult.put(messageString, subMsgIDs);
				}
			}
		}
		for (String pubMsg : tmpMatchingResult.keySet()) {
			Set<String> ids = tmpMatchingResult.get(pubMsg);
			try {
				PublicationMessage tempMessage = new PublicationMessage(pubMsg);
				matchingResult.put(tempMessage, ids);
			} catch (ParseException e) {
				exceptionLogger.error(e.getMessage());
			}
		}
	}

	private void readInBufferUnSub(StringTokenizer forwardingTargetsTokenizer, String msgId,
			Set<String> matchingResult) throws JessException, ParseException {
		if (reteMatcherLogger.isDebugEnabled())
			reteMatcherLogger.debug("forwardingTargetsBuffer is: "
					+ forwardingTargetsBuffer.toString());

		// we need info about the unsubscription and the subscription to
		// properly construct which messages to route
		while (forwardingTargetsTokenizer.hasMoreElements()) {
			String lastHopID = (forwardingTargetsTokenizer.nextToken(" ")).toString();
			String advMsgID = (forwardingTargetsTokenizer.nextToken(" ")).toString();
			String tid = "";
			if (brokerCore.isCycle()) {
				tid = (forwardingTargetsTokenizer.nextToken(" ")).toString();
			}
			String messageString = (forwardingTargetsTokenizer.nextToken("\n")).toString().trim();
			SubscriptionMessage tempSubMessage = new SubscriptionMessage(messageString);
			// HACK: for the unsubscription not issued by original subscriber
			if (!tempSubMessage.getLastHopID().equals(new MessageDestination(lastHopID))) {
				matchingResult.add(advMsgID);
			}
			if (brokerCore.isCycle()) {
				if (!tid.equals("nil")) {
					msgId = msgId + "_" + tid;
				}
			}
		}
	}

	/*
	 * Attaches the given payload to a set of messages
	 */
	private void attachPayload(Serializable payload, Map<PublicationMessage, Set<String>> messages) {
		if (messages != null && payload != null) {
			for (PublicationMessage pubMsg : messages.keySet()) {
				pubMsg.getPublication().setPayload(payload);
			}
		}
	}

	/**
	 * This function is to deal with the problem of un-propagating subscriptions which were
	 * propagated when the advertisement had come in. It finds all subscriptions which match ONLY
	 * the advertisement given, and removes them from the SRT and PRT
	 * 
	 * @param advertisementFact
	 *            The advertisement
	 * @return A set of UnsubscriptionMessages
	 * @throws JessException
	 *             If something goes wrong in the Rete
	 * @throws ParseException 
	 */
	private void removeAdvAndSubMessages(Fact advertisementFact) throws JessException, ParseException {
		// Should already be reset, but just in case
		forwardingTargetsBuffer.reset();
		SRT.retract(advertisementFact);
		SRT.assertFact(advertisementFact);

		int numberOfMatches = SRT.run();
		SRT.retract(advertisementFact);

		if (numberOfMatches > 0) {
			// Must now parse the stream
			StringTokenizer forwardingTargetsTokenizer = new StringTokenizer(
					forwardingTargetsBuffer.toString());
			Set<SubscriptionMessage> messagesToRoute = new HashSet<SubscriptionMessage>();

			if (brokerCore.isCycle()) {
				while (forwardingTargetsTokenizer.hasMoreElements()) {
					String lastHopID = (forwardingTargetsTokenizer.nextToken(" ")).toString();
					(forwardingTargetsTokenizer.nextToken(" ")).toString();
					String tid = (forwardingTargetsTokenizer.nextToken(" ")).toString();
					String messageString = (forwardingTargetsTokenizer.nextToken("\n")).toString().trim();
					SubscriptionMessage tempMessage = new SubscriptionMessage(messageString);
					MessageDestination tempNextHopID = new MessageDestination(lastHopID);

					boolean nextHopIDNone = tempNextHopID.getDestinationID().equals("none");
					boolean nextHopSameAsLastHop = tempMessage.getLastHopID().equals(tempNextHopID);
					tempMessage.setNextHopID(tempNextHopID);
					if (!(nextHopIDNone || nextHopSameAsLastHop)) {
						reteMatcherLogger.debug("Adding message: " + tempMessage.toString()
								+ " to messagesToRoute.");
						String messageID = tempMessage.getMessageID();
						if (tid.equals("nil")) {
							// this sub only match the given adv, since they have the same tid we
							// need to delete it from both SRT and PRT
							SRT.unDefrule(messageID);
						} else {
							// this sub is at first broker where the sub entered; delete it only
							// from PRT
							messageID = messageID + "_" + tid;
						}
						PRT.unDefrule(messageID);
					} else {
						reteMatcherLogger.debug("Not adding message: " + tempMessage.toString()
								+ " to messagesToRoute.");
					}
				}

			} else {
				// messagesToRoute stores all subscriptions that really match this advertisement.
				while (forwardingTargetsTokenizer.hasMoreElements()) {
					String lastHopID = (forwardingTargetsTokenizer.nextToken(" ")).toString();
					(forwardingTargetsTokenizer.nextToken(" ")).toString();
					String messageString = (forwardingTargetsTokenizer.nextToken("\n")).toString().trim();
					SubscriptionMessage tempMessage = new SubscriptionMessage(messageString);
					MessageDestination tempNextHopID = new MessageDestination(lastHopID);

					boolean nextHopIDNone = tempNextHopID.getDestinationID().equals("none");
					boolean nextHopSameAsLastHop = tempMessage.getLastHopID().equals(tempNextHopID);
					tempMessage.setNextHopID(tempNextHopID);
					if (!(nextHopIDNone || nextHopSameAsLastHop)) {
						reteMatcherLogger.debug("Adding message: " + tempMessage.toString()
								+ " to messagesToRoute.");
						messagesToRoute.add(tempMessage);
					} else {
						reteMatcherLogger.debug("Not adding message: " + tempMessage.toString()
								+ " to messagesToRoute.");
					}
				}
				// THIS IS A REALLY EXPENSIVE OPERATION HERE. It iterates through the list of all
				// matching subscriptions, and resubmits them to the rule engine to see if any match
				// without the advertisement fact asserted. If they do, they do not get propagated
				// as an unsubscription.
				for (SubscriptionMessage subMessage : messagesToRoute) {
					String SRTDefrule = subscriptionMessageToSRTDefrule(subMessage);
					forwardingTargetsBuffer.reset();
					// Note: defining a rule already present in the matching
					// engine resets its activation, allowing it to fire
					// again
					SRT.executeCommand(SRTDefrule);
					int numberOfTimesFired = SRT.run();
					try {
						if (numberOfTimesFired == 0) {
							// If nothing fires, then we have a winner, and we need to remove that
							// subscription
							String messageID = subMessage.getMessageID();
							if (!subMessage.getLastHopID().isBroker()) {
								// this subscription message comes from a client side, we only
								// remove it from PRT.
								PRT.unDefrule(messageID);
							} else {
								// this subscription message come from a broker, we need to remove
								// it from both SRT and PRT.
								SRT.unDefrule(messageID);
								PRT.unDefrule(messageID);
							}
						} else {
							forwardingTargetsTokenizer = new StringTokenizer(
									forwardingTargetsBuffer.toString());
							// this subscription also fired another advtisement, check if they are
							// from the same lastHop, if yes, then they are not really matched, we
							// need to remove this sub. if no, we could not remove this sub.
							String messageID = subMessage.getMessageID();
							while (forwardingTargetsTokenizer.hasMoreElements()) {
								String lastHopID = (forwardingTargetsTokenizer.nextToken(" ")).toString();
								(forwardingTargetsTokenizer.nextToken(" ")).toString();
								String messageString = (forwardingTargetsTokenizer.nextToken("\n")).toString().trim();
								SubscriptionMessage tempMessage = new SubscriptionMessage(
										messageString);
								MessageDestination tempNextHopID = new MessageDestination(lastHopID);

								boolean lastHopNotABroker = !tempMessage.getLastHopID().isBroker();
								boolean nextHopSameAsLastHop = tempMessage.getLastHopID().equals(
										tempNextHopID);

								tempMessage.setNextHopID(tempNextHopID);

								if (nextHopSameAsLastHop) {
									// we need to remove it.
									if (lastHopNotABroker) {
										PRT.unDefrule(messageID);
									} else {
										SRT.unDefrule(messageID);
										PRT.unDefrule(messageID);
									}
								}
							}
						}
					} catch (JessException e) {
						reteMatcherLogger.error("Failed to remove this subscription: " + e);
						exceptionLogger.error("Failed to remove this subscription: " + e);
					}
				}
			}
		}
	}

	/** * Wrapper methods for running Jess commands (wun@eecg) ** */
	public String runSRTJessCommand(String cmd) throws JessException {
		return runJessCommand(cmd, SRT);
	}

	public String runPRTJessCommand(String cmd) throws JessException {
		return runJessCommand(cmd, PRT);
	}

	private String runJessCommand(String cmd, Rete rete) throws JessException {
		CharArrayWriter buff = new CharArrayWriter();

		rete.addOutputRouter("WSTDOUT", buff);
		rete.executeCommand("(" + cmd + ")");

		return buff.toString();
	}

	@Override
	public boolean isPartialMatch() {
		return false;
	}

	public String toString() {
		return "JessMatcher in " + brokerCore.getBrokerID();
	}

}
