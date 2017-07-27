package de.tum.msrg.itt;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.broker.controller.OverlayRoutingTable;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.message.*;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by pxsalehi on 24.06.16.
 *
 * adds a few more primitives to the broker that can be used for executing shifts and moves
 */
public class IttBrokerCore extends BrokerCore {
    private NodeURI nodeURI = null;
    private IttAgent ittAgent;
//    private static final String ZK_ADRS_CMD = "-z";
//    private String zkAdrs;
    // statistics file
    private FileWriter statOutFile;

    public IttBrokerCore(String arg) throws BrokerCoreException {
        this(arg.split("\\s+"));
    }

    public IttBrokerCore(String[] args) throws BrokerCoreException {
        super(args);
    }

    public IttBrokerCore(String[] args, boolean def) throws BrokerCoreException {
        super(args, def);
    }

    public IttBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
        super(brokerConfig);
    }

//    private String[] extractIttConfig(String[] args) throws BrokerCoreException {
//        List<String> ret = new ArrayList<>();
//        for(int i = 0; i < args.length; i++) {
//            if(args[i].equalsIgnoreCase(ZK_ADRS_CMD)) {
//                if(i == args.length - 1)
//                    throw new BrokerCoreException("Parameter " + ZK_ADRS_CMD + " has no value!");
//                zkAdrs = args[++i];
//            } else {
//                ret.add(args[i]);
//            }
//        }
//        return ret.toArray(new String[0]);
//    }

    @Override
    protected InputQueueHandler createInputQueueHandler() {
        return new IttInputQueueHandler(this);
    }

    private void initIttBrokerCore() throws BrokerCoreException {
        nodeURI = NodeURI.parse(getBrokerURI());
        try {
            statOutFile = new FileWriter(new File(brokerConfig.getIttStatsOutputFile()));
            ittAgent = new IttAgent(this, nodeURI, brokerConfig.getZookeeperAddress());
        } catch (IOException e) {
            throw new BrokerCoreException(e);
        }
    }

    @Override
    public void initialize() throws BrokerCoreException {
        super.initialize();
        initIttBrokerCore();
        try {
            ittAgent.start();
        } catch (IttException e) {
            e.printStackTrace();
            throw new BrokerCoreException(e);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        ittAgent.stop();
        try {
            statOutFile.close();
        } catch (IOException e) {
            e.printStackTrace();
            exceptionLogger.warn(e);
        }
    }

    public void pauseNormalTraffic() {
        ((IttInputQueueHandler)inputQueue).pauseNormalTraffic();
    }

    public void resumeNormalTraffic() {
        ((IttInputQueueHandler)inputQueue).resumeNormalTraffic();
    }

    // modifies existing set of advertisements (SRT) so that advs coming
    // from curLastHop point to newLastHop as their origin
    public void modifySRT(NodeURI curLastHop, NodeURI newLastHop) {
        synchronized (router.getAdvertisements()) {
            IttUtility.modifyRoutingTable(router.getAdvertisements(), curLastHop, newLastHop);
        }
    }

    // modifies existing set of advertisements (SRT) so that advs coming
    // from curLastHop and their id is present in ids, point to newLastHop as their origin
    public void modifySRT(NodeURI curLastHop, NodeURI newLastHop, Set<String> ids) {
        synchronized (router.getAdvertisements()) {
            IttUtility.modifyRoutingTable(router.getAdvertisements(), curLastHop, newLastHop, ids);
        }
    }

    public Set<String> getAdvIdsWithLastHopFrom(NodeURI from) {
        synchronized (router.getAdvertisements()) {
            return IttUtility.getMsgIdsWithLastHopFrom(router.getAdvertisements(), from);
        }
    }

    public Set<SubscriptionMessage> getAllSubsWithLastHopNotFrom(NodeURI from) {
        synchronized (router.getSubscriptions()) {
            Set<SubscriptionMessage> subs = new HashSet<>();
            MessageDestination lastHop =
                    new MessageDestination(from.getURI(), MessageDestination.DestinationType.BROKER);
            for(SubscriptionMessage sub: router.getSubscriptions().values())
                if(!sub.getLastHopID().equals(from))
                    subs.add(sub);
            return subs;
        }
    }

    // modifies existing set of subscriptions (PRT) so that subs coming
    // from curLastHop point to newLastHop as their origin
    public void modifyPRT(NodeURI curLastHop, NodeURI newLastHop) {
        synchronized (router.getSubscriptions()) {
            IttUtility.modifyRoutingTable(router.getSubscriptions(), curLastHop, newLastHop);
        }
    }

    // modifies existing set of subscriptions (PRT) so that subs coming
    // from curLastHop and their id is present in ids, point to newLastHop as their origin
    public void modifyPRT(NodeURI curLastHop, NodeURI newLastHop, Set<String> ids) {
        synchronized (router.getSubscriptions()) {
            IttUtility.modifyRoutingTable(router.getSubscriptions(), curLastHop, newLastHop, ids);
        }
    }

    // goes through the set of subs and if they are missing from current broker's PRT, adds them
    // to the PRT. As lasthop, subs coming from i are left unchanged, otherwise their last hop is
    // changed to j
    // The broker calling this method is supposed to be k on a shift(i, j, k)
    public void addMissingSubsToPRT(Set<SubscriptionMessage> recvdSubs, NodeURI i, NodeURI j) {
        synchronized (router.getSubscriptions()) {
            MessageDestination iLastHop =
                    new MessageDestination(i.getURI(), MessageDestination.DestinationType.BROKER);
            MessageDestination jLastHop =
                    new MessageDestination(j.getURI(), MessageDestination.DestinationType.BROKER);
            MessageDestination kLastHop =
                    new MessageDestination(getBrokerURI(), MessageDestination.DestinationType.BROKER);
            Map<String, SubscriptionMessage> PRT = router.getSubscriptions();
            for(SubscriptionMessage sub: recvdSubs)
                if(!PRT.containsKey(sub.getMessageID())) {
                    // if missing sub comes from i, add to PRT and keep lasthop as i
                    if(sub.getLastHopID().equals(iLastHop)) {
                        router.handleMessage(sub);
                        //PRT.put(sub.getMessageID(), sub);
                    } else if(!sub.getLastHopID().equals(kLastHop)) {
                        // otherwise change so that sub comes from j
                        sub.setLastHopID(jLastHop);
                        router.handleMessage(sub);
                        //PRT.put(sub.getMessageID(), sub);
                    }
                }
        }
    }

    public Set<String> getSubIdsWithLastHopFrom(NodeURI from) {
        synchronized (router.getSubscriptions()) {
            return IttUtility.getMsgIdsWithLastHopFrom(router.getSubscriptions(), from);
        }
    }

    public NodeURI getNodeURI() {
        return nodeURI;
    }

    // if connect/disconnect through overlay didn't work, try only changing local ORT
    // for each shift operand, hoping, once in-queues are resumed, the connections establish
    public void connectToNeighbor(NodeURI neighborUri) {
        // send OVERLAY-CONNECT(s) to controller
        Publication p = MessageFactory.createEmptyPublication();
        p.addPair("class", "BROKER_CONTROL");
        p.addPair("brokerID", getBrokerID());
        p.addPair("command", "OVERLAY-CONNECT");
        p.addPair("broker", neighborUri.getURI());
        PublicationMessage pm = new PublicationMessage(p, "initial_connect");
        if (brokerCoreLogger.isDebugEnabled())
            brokerCoreLogger.debug("Broker " + getBrokerID() + " is sending initial connection to broker "
                    + neighborUri.getURI());
        queueManager.enQueue(pm, MessageDestination.INPUTQUEUE);
    }

    public void disconnectFromNeighbor(NodeURI neighborUri) throws IttException {
        try {
            OverlayRoutingTable ort = getOverlayManager().getORT();
            MessageDestination neighbor = new MessageDestination(neighborUri.getURI(),
                    MessageDestination.DestinationType.BROKER);
            // check if neighbor is really a neighbor
            if(!ort.isNeighbor(neighbor)) {
                // just print warning, and return
//                throw new IttException("Trying to disconnect from " + neighborUri.getID() +
//                        "which does not exist in the overlay routing table!");
                String warnMsg = "Trying to disconnect from " + neighborUri.getID() +
                        "which does not exist in the overlay routing table!";
                System.out.println(warnMsg);
                exceptionLogger.warn(warnMsg);
            }
            // send neighbor a shutdown remotebroker control message which causes the recipient to close
            // its connection with this broker and clean up its overlay routing table
            Publication sdPub = MessageFactory.createPublicationFromString(
                    "[class,BROKER_CONTROL]," + "[brokerID,'" + neighbor.getDestinationID() + "']," +
                    "[fromID,'" + getBrokerID() + "'],[command,'SHUTDOWN_REMOTEBROKER']");
            PublicationMessage sdPubMsg = new PublicationMessage(sdPub, getNewMessageID(), getBrokerDestination());
            sdPubMsg.setNextHopID(neighbor);
            routeMessage(sdPubMsg);
            // make sure out queue to neighbor is empty
            MessageQueue outQueueToNeighbor = queueManager.getQueue(neighbor);
            if(outQueueToNeighbor != null) {
                while (outQueueToNeighbor.size() > 0)
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
            }
            // remove neighbor from your own ORT
            ort.removeBroker(neighbor);
            removeQueue(neighbor);
        } catch (ParseException e) {
            exceptionLogger.error(e.getMessage());
        }
    }

    public IttAgent getIttAgent() {
        return ittAgent;
    }

    public void writeStatistics(String s) {
        try {
            statOutFile.write(s + "\n");
            statOutFile.flush();
        } catch (IOException e) {
            exceptionLogger.error(e);
        }
    }

    public static void main(String[] args) {
        try {
            IttBrokerCore brokerCore = new IttBrokerCore(args);
            brokerCore.initialize();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    brokerCore.shutdown();
                }
            });
        } catch (Exception e) {
            // log the error the system error log file and exit
            Logger sysErrLogger = Logger.getLogger("SystemError");
            if (sysErrLogger != null)
                sysErrLogger.fatal(e.getMessage() + ": " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
