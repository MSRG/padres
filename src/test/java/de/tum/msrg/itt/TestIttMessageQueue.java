package de.tum.msrg.itt;

import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by pxsalehi on 20.07.16.
 */
public class TestIttMessageQueue {
    private AdvertisementMessage advMsg =
            new AdvertisementMessage(MessageFactory.createEmptyAdvertisement(), "adv1");
    private PublicationMessage pubMsg1 =
            new PublicationMessage(MessageFactory.createEmptyPublication().addPair("class", "p1"), "pub1");
    private PublicationMessage pubMsg2 =
            new PublicationMessage(MessageFactory.createEmptyPublication().addPair("class", "p2"), "pub2");
    private PublicationMessage ctrlMsg1 =
            new PublicationMessage(MessageFactory.createEmptyPublication().addPair("class", "BROKER_CONTROL"), "ctrl");
    private PublicationMessage ctrlMsg2 =
            new PublicationMessage(MessageFactory.createEmptyPublication().addPair("class", "BROKER_INFO"), "info");
    private IttMessageQueue q;
    private static final int WAIT_MSEC = 2000;
    private static final int TIMEOUT_MSEC = 20000;

    @Test(timeout = TIMEOUT_MSEC)
    public void testNotPaused() {
        q = new IttMessageQueue();
        q.add(advMsg);
        q.add(ctrlMsg1);
        assertEquals(q.blockingRemove(), advMsg);
        assertEquals(q.blockingRemove(), ctrlMsg1);
    }

    @Test(timeout = TIMEOUT_MSEC)
    public void testPaused() {
        q = new IttMessageQueue();
        q.add(pubMsg1);
        q.add(ctrlMsg1);
        q.setPauseNormalTraffic(true);
        assertEquals(q.blockingRemove(), ctrlMsg1);
    }

    @Test(timeout = TIMEOUT_MSEC)
    public void testPausedTimeout() throws InterruptedException {
        q = new IttMessageQueue();
        q.add(pubMsg1);
        q.add(pubMsg2);
        q.setPauseNormalTraffic(true);
        Thread t = new Thread(() -> q.blockingRemove());
        t.start();
        // make sure thread is waiting for the queue
        t.join(WAIT_MSEC);
        assertTrue(t.isAlive());
        assertEquals(q.size(), 2);
    }

    @Test(timeout = TIMEOUT_MSEC)
    public void testPauseUnpause() throws InterruptedException {
        q = new IttMessageQueue();
        q.add(pubMsg1);
        q.add(pubMsg2);
        q.setPauseNormalTraffic(true);
        Thread t = new Thread(() -> q.blockingRemove());
        t.start();
        t.join(WAIT_MSEC);
        assertTrue(t.isAlive());
        assertEquals(q.size(), 2);
        q.setPauseNormalTraffic(false);
        t.join(WAIT_MSEC);
        assertFalse(t.isAlive());
        assertEquals(q.size(), 1);
    }

    @Test(timeout = TIMEOUT_MSEC)
    public void testPauseUnpauseAddNew() throws InterruptedException {
        q = new IttMessageQueue();
        q.add(pubMsg1);
        q.add(pubMsg2);
        q.setPauseNormalTraffic(true);
        Thread t = new Thread(() -> q.blockingRemove());
        t.start();
        t.join(WAIT_MSEC);
        assertTrue(t.isAlive());
        assertEquals(q.size(), 2);
        q.setPauseNormalTraffic(false);
        q.add(ctrlMsg1);
        t.join(WAIT_MSEC);
        assertFalse(t.isAlive());
        assertEquals(q.size(), 2);
        assertEquals(q.blockingRemove(), pubMsg2);
        assertEquals(q.blockingRemove(), ctrlMsg1);
    }

    @Test(timeout = TIMEOUT_MSEC)
    public void testPauseAddNew() throws InterruptedException {
        q = new IttMessageQueue();
        q.add(pubMsg1);
        q.add(ctrlMsg1);
        q.add(pubMsg2);
        q.setPauseNormalTraffic(true);
        Thread t = new Thread(() -> {
            q.blockingRemove();
            q.blockingRemove();
        });
        t.start();
        t.join(WAIT_MSEC);
        assertTrue(t.isAlive());
        assertEquals(q.size(), 2);
        q.add(ctrlMsg2);
        t.join(WAIT_MSEC);
        assertFalse(t.isAlive());
        assertEquals(q.size(), 2);
        assertEquals(q.removeFirst(), pubMsg1);
        assertEquals(q.removeFirst(), pubMsg2);
    }
}
