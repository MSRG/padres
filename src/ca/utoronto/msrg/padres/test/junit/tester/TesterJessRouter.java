package ca.utoronto.msrg.padres.test.junit.tester;

import java.util.Set;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.router.JessRouter;
import ca.utoronto.msrg.padres.broker.router.matching.AdvMsgNotFoundException;
import ca.utoronto.msrg.padres.broker.router.matching.DuplicateMsgFoundException;
import ca.utoronto.msrg.padres.broker.router.matching.MatcherException;
import ca.utoronto.msrg.padres.broker.router.matching.PubMsgNotConformedException;
import ca.utoronto.msrg.padres.broker.router.matching.SubMsgNotFoundException;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap JessRouter's main public methods to inform the
 * IBrokerTester object of occurrence of specific events within the object:
 * 
 *   - JessRouter.handleMessage(*): IBrokerTester.routerHandlingMessage()
 *   is appropriately called.
 *   
 *   - JessRouter.addSubWorkingMemory(): IBrokerTester.routerAddSubscription()
 *   is appropriately called.
 *   
 *   - JessRouter.addAdvWorkingMemory(): IBrokerTester.routerAddAdvertisement()
 *   is appropriately called.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
public class TesterJessRouter extends JessRouter {

	public final IBrokerTester _brokerTester;
	public final BrokerCore _broker;
	
	public TesterJessRouter(IBrokerTester brokerTester, BrokerCore broker) {
		super(broker);
		
		_brokerTester = brokerTester;
		_broker = broker;
	}
	
	@Override
	protected Set<Message> handleMessage(AdvertisementMessage advMsg) throws DuplicateMsgFoundException, MatcherException {
		Set<Message> ret = super.handleMessage(advMsg);
		_brokerTester.routerHandlingMessage(_broker.getBrokerURI(), advMsg);
		return ret;
	}
	
	@Override
	protected Set<Message> handleMessage(SubscriptionMessage subMsg) throws MatcherException {
		Set<Message> ret = super.handleMessage(subMsg);
		_brokerTester.routerHandlingMessage(_broker.getBrokerURI(), subMsg);
		return ret;
	}
	
	@Override
	protected Set<Message> handleMessage(CompositeSubscriptionMessage csMsg) throws MatcherException {
		Set<Message> ret = super.handleMessage(csMsg);
		_brokerTester.routerAddCompositeSubscription(_broker.getBrokerURI(), csMsg);
		return ret;
	}
	
	@Override
	protected Set<Message> handleMessage(PublicationMessage pubMsg) throws PubMsgNotConformedException {
		Set<Message> ret = super.handleMessage(pubMsg);
		_brokerTester.routerHandlingMessage(_broker.getBrokerURI(), pubMsg);
		return ret;
	}
	
	@Override
	protected Set<Message> handleMessage(UnsubscriptionMessage unsubMsg) throws SubMsgNotFoundException {
		Set<Message> ret = super.handleMessage(unsubMsg);
		_brokerTester.routerHandlingMessage(_broker.getBrokerURI(), unsubMsg);
		return ret;
	}

	@Override
	protected Set<Message> handleMessage(UnadvertisementMessage unAdvMsg) throws AdvMsgNotFoundException, DuplicateMsgFoundException {
		Set<Message> ret = super.handleMessage(unAdvMsg);
		_brokerTester.routerHandlingMessage(_broker.getBrokerURI(), unAdvMsg);
		return ret;
	}
	
	@Override
	protected void addSubWorkingMemory(SubscriptionMessage subMsg) {
		super.addSubWorkingMemory(subMsg);
		_brokerTester.routerAddSubscription(_broker.getBrokerURI(), subMsg);
	}
	
	@Override
	protected void addAdvWorkingMemory(AdvertisementMessage advMsg) {
		super.addAdvWorkingMemory(advMsg);
		_brokerTester.routerAddAdvertisement(_broker.getBrokerURI(), advMsg);
	}
}
