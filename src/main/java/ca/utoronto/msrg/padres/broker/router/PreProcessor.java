package ca.utoronto.msrg.padres.broker.router;

import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;

public interface PreProcessor {

	public void preprocess(Message m);

	public void process(PublicationMessage pubMsg);

	public void process(SubscriptionMessage subMsg);

	public void process(CompositeSubscriptionMessage csMsg);

	public void process(AdvertisementMessage advMsg);

}
