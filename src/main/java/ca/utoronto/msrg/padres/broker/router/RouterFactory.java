package ca.utoronto.msrg.padres.broker.router;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.router.matching.MatcherException;

public class RouterFactory {

	public enum MatcherType {
		Jess, NewRete;
	}

	protected static final boolean ALLOW_JESS = false;

	public static Router createRouter(MatcherType matcherType, BrokerCore broker)
			throws MatcherException {
		if (matcherType == MatcherType.Jess) {
			if (ALLOW_JESS) {
				return new JessRouter(broker);
			} else {
				throw new MatcherException("Jess Router is not available; Use 'NewRete'");
			}
		}
		return new ReteRouter(broker);
	}
}
