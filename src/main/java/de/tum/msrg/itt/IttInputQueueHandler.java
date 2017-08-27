package de.tum.msrg.itt;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageType;

/**
 * Created by pxsalehi on 15.07.16.
 */
public class IttInputQueueHandler extends InputQueueHandler {

    public IttInputQueueHandler(BrokerCore broker) {
        super(broker);
    }

    @Override
    protected IttMessageQueue createMessageQueue() {
        return new IttMessageQueue();
    }

    public void pauseNormalTraffic() {
        ((IttMessageQueue)msgQueue).setPauseNormalTraffic(true);
    }

    public void resumeNormalTraffic() {
        ((IttMessageQueue)msgQueue).setPauseNormalTraffic(false);
    }

    public boolean isNormalTrafficPaused() {
        return ((IttMessageQueue)msgQueue).isPauseNormalTraffic();
    }
}
