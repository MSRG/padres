package de.tum.msrg.itt.tester;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.integration.tester.IBrokerTester;
import ca.utoronto.msrg.padres.integration.tester.TesterInputQueueHandler;
import ca.utoronto.msrg.padres.integration.tester.TesterMessageQueue;
import de.tum.msrg.itt.IttMessageQueue;

/**
 * Created by pxsalehi on 25.07.16.
 */
public class TesterIttInputQueueHandler extends TesterInputQueueHandler {

    public TesterIttInputQueueHandler(IBrokerTester brokerTester, BrokerCore broker) {
        super(brokerTester, broker);
    }

    @Override
    protected MessageQueue createMessageQueue() {
        TesterIttMessageQueue mQueue = new TesterIttMessageQueue();
        mQueue.setBrokerTester(_brokerTester);
        mQueue.setBrokerURI(_brokerURI);
        return mQueue;
    }

    public void pauseNormalTraffic() {
        ((TesterIttMessageQueue)msgQueue).setPauseNormalTraffic(true);
    }

    public void resumeNormalTraffic() {
        ((TesterIttMessageQueue)msgQueue).setPauseNormalTraffic(false);
    }

    public boolean isNormalTrafficPaused() {
        return ((TesterIttMessageQueue)msgQueue).isPauseNormalTraffic();
    }
}
