package de.tum.msrg.itt.tester;/**
 * Auxiliary class used as part of test framework. The class
 * implements methods that wrap BrokerCore's main public
 * methods and construct other subclassed components that can
 * interact with the IBrokerTest implementing object used for testing.
 *
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */

import ca.utoronto.msrg.padres.broker.brokercore.*;
import ca.utoronto.msrg.padres.broker.monitor.SystemMonitor;
import ca.utoronto.msrg.padres.broker.router.matching.MatcherException;
import ca.utoronto.msrg.padres.common.comm.CommSystem;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.integration.tester.*;
import de.tum.msrg.itt.IttBrokerCore;
import de.tum.msrg.itt.IttInputQueueHandler;

public class TesterIttBrokerCore extends IttBrokerCore {

	public final IBrokerTester _brokerTester;

	public TesterIttBrokerCore(IBrokerTester brokerTester, String[] args) throws BrokerCoreException {
		super(args);
		_brokerTester = brokerTester;
	}

	public TesterIttBrokerCore(IBrokerTester brokerTester, BrokerConfig brokerConfig) throws BrokerCoreException {
		super(brokerConfig);
		_brokerTester = brokerTester;
	}

	@Override
	protected QueueManager createQueueManager() throws BrokerCoreException {
		TesterQueueManager qManager = new TesterQueueManager(_brokerTester, this);
		return qManager;
	}

	@Override
	protected InputQueueHandler createInputQueueHandler() {
		return new TesterIttInputQueueHandler(_brokerTester, this);
	}

	@Override
	protected TesterController createController() {
		return new TesterController(_brokerTester, this);
	}

	@Override
	protected CommSystem createCommSystem() throws CommunicationException {
		return new TesterCommSystem(_brokerTester, getBrokerURI());
	}

	@Override
	protected void initRouter() throws BrokerCoreException {
		try {
			router = TesterRouterFactory.createTesterRouter(_brokerTester, brokerConfig.getMatcherName(), this);
			router.initialize();
			brokerCoreLogger.info("Router/Matching Engine is initialized");
		} catch (MatcherException e) {
			brokerCoreLogger.error("Router failed to instantiate: " + e);
			exceptionLogger.error("Router failed to instantiate: " + e);
			throw new BrokerCoreException("Router failed to instantiate: " + e);
		}
	}

	@Override
	public void registerQueue(MessageDestination msgDest, MessageQueue msgQueue) {
		super.registerQueue(msgDest, msgQueue);
		((IMessageQueueTester)msgQueue).setMyMessageDestination(msgDest);
	}

	@Override
	protected HeartbeatSubscriber createHeartbeatSubscriber() {
		return new TesterHeartbeatSubscriber(_brokerTester, this);
	}

	@Override
	protected SystemMonitor createSystemMonitor() {
		return new TesterSystemMonitor(_brokerTester, this);
	}

	@Override
	public void pauseNormalTraffic() {
		((TesterIttInputQueueHandler)inputQueue).pauseNormalTraffic();
	}

	@Override
	public void resumeNormalTraffic() {
		((TesterIttInputQueueHandler)inputQueue).resumeNormalTraffic();
	}
}