package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.router.Router;
import ca.utoronto.msrg.padres.broker.router.RouterFactory;
import ca.utoronto.msrg.padres.broker.router.matching.MatcherException;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap RouterFactory's main public methods and construct
 * TesterReteRouter that can interact with the IBrokerTester implementing
 * object used for testing.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */

public class TesterRouterFactory extends RouterFactory {

	public static Router createTesterRouter(
			IBrokerTester brokerTester, MatcherType matcherType,
			BrokerCore broker) throws MatcherException {
		if (matcherType == MatcherType.Jess) {
			if (ALLOW_JESS) {
				return new TesterJessRouter(brokerTester, broker);
			} else {
				throw new MatcherException("Jess Router is not available; Use 'NewRete'");
			}
		}
		return new TesterReteRouter(brokerTester, broker);
	}
}
