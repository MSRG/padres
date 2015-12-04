package ca.utoronto.msrg.padres.broker.router;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.router.matching.Matcher;
import ca.utoronto.msrg.padres.broker.router.matching.rete.ReteMatcher;

public class ReteRouter extends Router {
	
	public ReteRouter(BrokerCore broker) {
		super(broker);
	}

	protected Matcher createMatcher(BrokerCore broker) {
		return new ReteMatcher(broker, this);
	}

	public PreProcessor createPreProcessor(BrokerCore broker) {
		return new PreProcessorImpl(broker);
	}

	public Forwarder createForwarder(Router router, BrokerCore broker) {
		return new ForwarderImpl(router, broker);
	}

	public PostProcessor createPostProcessor(BrokerCore broker) {
		return new PostProcessorImpl(broker);
	}

}
