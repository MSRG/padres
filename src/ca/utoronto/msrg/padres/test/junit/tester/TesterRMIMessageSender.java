package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIAddress;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIMessageListenerInterfce;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIMessageSender;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap RMIMessageSender's main public methods to inform the
 * IBrokerTester object of occurrence of specific events within the object:
 * 
 *   - RMIMessageSender.send(): IBrokerTester.msgSent() is appropriately
 *     called.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
class TesterRMIMessageSender extends RMIMessageSender implements ITestMessageSender {
	
	public final IBrokerTester _brokerTester;
	public final String _brokerURI;

	public TesterRMIMessageSender(
			IBrokerTester brokerTester, String brokerURI,
			RMIAddress remoteAddress) throws CommunicationException {
		super(remoteAddress);
		
		_brokerTester = brokerTester;
		_brokerURI = brokerURI;
	}
	
	public TesterRMIMessageSender(
			IBrokerTester brokerTester, String brokerURI,
			RMIMessageListenerInterfce msgListener) {
		super(msgListener);
		
		_brokerTester = brokerTester;
		_brokerURI = brokerURI;
	}

	@Override
	public String send(Message msg, HostType sendingHostType) throws CommunicationException {
		String ret = super.send(msg, sendingHostType);
		_brokerTester.msgSent(this, msg);
		
		return ret;
	}
	
	MessageDestination _destinaion;
	
	public void setDestination(MessageDestination destinaion) {
		_destinaion = destinaion;
	}

	@Override
	public String getBrokerURI() {
		return _brokerURI;
	}

	@Override
	public String getRemoteURI() {
		if(remoteServerAddress == null)
			return _destinaion.toString();
		else
			return remoteServerAddress.toString();
	}
	
	@Override
	public void connect() throws CommunicationException {
		super.connect();
	}
}