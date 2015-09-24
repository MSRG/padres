package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.controller.OverlayManager;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.MessageSender;
import ca.utoronto.msrg.padres.common.comm.OutputQueue;
import ca.utoronto.msrg.padres.common.message.MessageDestination;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap OverlayManager's main public methods and construct
 * TesterOutputQueue that can interact with the IBrokerTester implementing
 * object used for testing.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
class TesterOverlayManager extends OverlayManager {
	
	public final IBrokerTester _brokerTester;
	public final TesterBrokerCore _brokerCore;
	
	public TesterOverlayManager(IBrokerTester brokerTester,
			TesterBrokerCore brokerCore) throws BrokerCoreException {
		super(brokerCore);
		
		_brokerTester = brokerTester;
		_brokerCore = brokerCore;
	}

	@Override
	protected OutputQueue createOutputQueue(MessageDestination clientDest,
			MessageSender msgSender) {
		return new TesterOutputQueue(
				_brokerTester, _brokerCore, clientDest, msgSender);
	}

	@Override
	protected MessageSender createMessageSenderAndConnect(String toBrokerURI) throws CommunicationException {
		try {
			MessageSender msgSender =
				brokerCore.getCommSystem().getMessageSender(toBrokerURI);
			msgSender.connect();
			return msgSender;
		} catch (CommunicationException x) {
			_brokerTester.connectFailed(_brokerCore.getBrokerURI(), toBrokerURI);
			throw x;
		}
	}

	@Override
	protected boolean checkRemoteBrokerURItoConnect(String toBrokerURI) throws CommunicationException {
		try {
			super.checkRemoteBrokerURItoConnect(toBrokerURI);
		} catch (CommunicationException x) {
			_brokerTester.connectFailed(_brokerCore.getBrokerURI(), toBrokerURI);
			throw x;
		}
		
		return true;
	}
}