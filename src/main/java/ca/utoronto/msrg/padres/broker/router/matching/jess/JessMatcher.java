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

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.router.Router;
import ca.utoronto.msrg.padres.broker.router.matching.AdvMsgNotFoundException;
import ca.utoronto.msrg.padres.broker.router.matching.DuplicateMsgFoundException;
import ca.utoronto.msrg.padres.broker.router.matching.Matcher;
import ca.utoronto.msrg.padres.broker.router.matching.MatcherException;
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

	public JessMatcher(BrokerCore broker, Router router) {

	}

	public void flushPRTByClassName(String className) {
	}

	@Override
	public Set<String> add(AdvertisementMessage m)
			throws DuplicateMsgFoundException, MatcherException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> add(SubscriptionMessage m) throws MatcherException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<SubscriptionMessage, Set<String>> add(
			CompositeSubscriptionMessage m) throws MatcherException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getMatchingAdvs(PublicationMessage pubMsg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<PublicationMessage, Set<String>> getMatchingSubs(
			PublicationMessage m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> add(UnsubscriptionMessage m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<UnsubscriptionMessage, Set<String>> add(
			UncompositesubscriptionMessage m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void add(UnadvertisementMessage m) throws AdvMsgNotFoundException,
			DuplicateMsgFoundException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isPartialMatch() {
		// TODO Auto-generated method stub
		return false;
	}

	public String runPRTJessCommand(String jessCommand) throws jess.JessException {
		// TODO Auto-generated method stub
		return null;
	}

	public String runSRTJessCommand(String jessCommand) throws jess.JessException {
		// TODO Auto-generated method stub
		return null;
	}

}
