package de.tum.msrg.itt.tester;

import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.integration.tester.IBrokerTester;
import ca.utoronto.msrg.padres.integration.tester.IMessageQueueTester;
import de.tum.msrg.itt.IttMessageQueue;

/**
 * Created by pxsalehi on 25.07.16.
 */
public class TesterIttMessageQueue extends IttMessageQueue implements IMessageQueueTester {
    protected IBrokerTester _brokerTester;
    protected String _brokerURI;
    protected MessageDestination _myDestination;

    public void setBrokerTester(IBrokerTester brokerTester) {
        _brokerTester = brokerTester;
    }

    public void setMyMessageDestination(
            MessageDestination myDestination) {
        _myDestination = myDestination;
    }

    public void setBrokerURI(String brokerURI) {
        _brokerURI = brokerURI;
    }

    @Override
    public MessageDestination getMyDestination() {
        return _myDestination;
    }

    @Override
    public String getBrokerURI() {
        return _brokerURI;
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
