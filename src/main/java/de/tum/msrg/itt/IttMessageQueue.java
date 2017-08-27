package de.tum.msrg.itt;

import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.message.Message;

import java.util.Iterator;

/**
 * Created by pxsalehi on 15.07.16.
 *
 * A message queue that can be set to return only control messages among the queued messages
 */
public class IttMessageQueue extends MessageQueue {
    private volatile boolean pauseNormalTraffic = false;

    public IttMessageQueue() {
        super();
    }

    @Override
    public synchronized Message blockingRemove() {
        while (true) {
            if (list.size() > 0) {
                if (!pauseNormalTraffic) {
                    // return first message
                    return list.remove(0);
                } else {  // normal traffic is paused
                    // return first control message
                    for(Iterator<Message> msgIt = list.iterator(); msgIt.hasNext();) {
                        Message msg = msgIt.next();
                        if(IttUtility.isControlMsg(msg)) {
                            msgIt.remove();
                            return msg;
                        }
                    }
                    try {
                        wait();
                    } catch (InterruptedException e) {}
                }
            } else {
                // block until notified that an element is in the queue
                try {
                    wait();
                } catch (InterruptedException ie) {}
            }
        }
    }

    public boolean isPauseNormalTraffic() {
        return pauseNormalTraffic;
    }

    public synchronized void setPauseNormalTraffic(boolean pauseNormalTraffic) {
        // notify when unpaused
        if((this.pauseNormalTraffic = pauseNormalTraffic) == false)
            notify();
    }
}
