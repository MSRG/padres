package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;

/**
 * Auxiliary class used as part of test framework. The class
 * implements methods that wrap MessageQueue's main public
 * methods and inform the an IBrokerTest implementing object
 * about occurrence of MessageQueue-specific events.
 * 
 * Key overwritten methods include:
 * 
 *   - MessageQueue.blockingRemove(): IBrokerTester.msgDequeued() is
 *     appropriately called.
 *     
 *   - MessageQueue.removeFirst(): IBrokerTester.msgDequeued() is
 *     appropriately called.
 *     
 *   - MessageQueue.add(): IBrokerTester.msgEnqueued() is
 *     appropriately called.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */

class TesterMessageQueue extends MessageQueue {
	
	private static final long serialVersionUID = -7378211408067992715L;
	protected IBrokerTester _brokerTester;
	protected String _brokerURI;
	protected MessageDestination _myDestination;

	protected void setBrokerTester(IBrokerTester brokerTester) {
		_brokerTester = brokerTester;
	}
	
	protected void setMyMessageDestination(
			MessageDestination myDestination) {
		_myDestination = myDestination;
	}
	
	protected void setBrokerURI(String brokerURI) {
		_brokerURI = brokerURI;
	}
	
	protected MessageDestination getMyDestination() {
		return _myDestination;
	}
	
	@Override
	public void add(Message msg) {
		super.add(msg);
		_brokerTester.msgEnqueued(this, msg);
		
		return;
	}
	
	@Override
	public Message blockingRemove() {
		Message msg = super.blockingRemove();
		_brokerTester.msgDequeued(this, msg);
		
		return msg;
	}
	
	@Override
	public synchronized Message removeFirst() {
		Message msg = super.removeFirst();
		_brokerTester.msgDequeued(this, msg);
		
		return msg;
	}
	
	@Override
	public String toString() {
		return _brokerURI + "->" + _myDestination;
	}
}