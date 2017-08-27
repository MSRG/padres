package ca.utoronto.msrg.padres.integration.tester;

import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;

import java.io.Serializable;

/**
 * Created by pxsalehi on 25.07.16.
 */
public interface IMessageQueueTester extends Serializable {
	void setBrokerTester(IBrokerTester brokerTester);

	void setMyMessageDestination(
			MessageDestination myDestination);

	void setBrokerURI(String brokerURI);

	void add(Message msg);

	Message blockingRemove();

	Message removeFirst();

	@Override
	String toString();

	MessageDestination getMyDestination();

	String getBrokerURI();
}
