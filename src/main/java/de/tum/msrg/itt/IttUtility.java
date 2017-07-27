package de.tum.msrg.itt;

import ca.utoronto.msrg.padres.common.message.*;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by pxsalehi on 22.06.16.
 */
public class IttUtility {
    public static boolean isControlMsg(Message msg) {
        /**
         * List of control messages:
         *      HEARTBEAT_MANAGER, BROKER_CONTROL, GLOBAL_FD, NETWORK_DISCOVERY,
         *      BROKER_MONITOR, TRACEROUTE_MESSAGE, BROKER_INFO
         */
        String msgClass = null;
        if(msg.getType().equals(MessageType.ADVERTISEMENT))
            msgClass = ((AdvertisementMessage) msg).getAdvertisement().getClassVal();
        else if(msg.getType().equals(MessageType.SUBSCRIPTION))
            msgClass = ((SubscriptionMessage) msg).getSubscription().getClassVal();
        else if(msg.getType().equals(MessageType.PUBLICATION))
            msgClass = ((PublicationMessage) msg).getPublication().getClassVal();
        if(msgClass != null && (msgClass.equalsIgnoreCase("BROKER_CONTROL")
                || msgClass.equalsIgnoreCase("BROKER_MONITOR") || msgClass.equalsIgnoreCase("TRACEROUTE_MESSAGE")
                || msgClass.equalsIgnoreCase("HEARTBEAT_MANAGER") || msgClass.equalsIgnoreCase("NETWORK_DISCOVERY")
                || msgClass.equalsIgnoreCase("BROKER_INFO") || msgClass.equalsIgnoreCase("GLOBAL_FD")))
                return true;
        return false;
    }

    public static void modifyRoutingTable(Map<String, ? extends Message> table,
                                          NodeURI curLastHop, NodeURI newLastHop) {
        MessageDestination curMsgDest =
                new MessageDestination(curLastHop.getURI(), MessageDestination.DestinationType.BROKER);
        MessageDestination newMsgDest =
                new MessageDestination(newLastHop.getURI(), MessageDestination.DestinationType.BROKER);
        // find entries
        Set<Message> entries = table.values().stream()
                .filter(advMsg -> advMsg.getLastHopID().equals(curMsgDest))
                .collect(Collectors.toSet());
        // change to new last hop
        entries.stream().forEach(advMsg -> advMsg.setLastHopID(newMsgDest));
    }

    public static void modifyRoutingTable(Map<String, ? extends Message> table,
                                          NodeURI curLastHop, NodeURI newLastHop,
                                          Set<String> ids) {
        MessageDestination curMsgDest =
                new MessageDestination(curLastHop.getURI(), MessageDestination.DestinationType.BROKER);
        MessageDestination newMsgDest =
                new MessageDestination(newLastHop.getURI(), MessageDestination.DestinationType.BROKER);
        table.entrySet().stream()
                .filter(entry -> entry.getValue().getLastHopID().equals(curMsgDest) && ids.contains(entry.getKey()))
                .forEach(entry -> entry.getValue().setLastHopID(newMsgDest));
    }

    public static Set<String> getMsgIdsWithLastHopFrom(Map<String, ? extends Message> table, NodeURI from) {
        MessageDestination lastHop =
                new MessageDestination(from.getURI(), MessageDestination.DestinationType.BROKER);
        return table.entrySet().stream()
                .filter(entry -> entry.getValue().getLastHopID().equals(lastHop))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public static class ZK {
        private static final Charset charset = Charset.forName("UTF-8");

        public static void createIfNotExists(ZooKeeper zk, String path, byte[] data, CreateMode createMode)
                throws KeeperException, InterruptedException {
            if (zk.exists(path, false) == null)
                zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
        }

        public static void deleteIfExists(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
            Stat stat = zk.exists(path, false);
            if (stat != null) {
                try {
                    zk.delete(path, stat.getVersion());
                } catch (KeeperException e) {
                    if (!e.code().equals(KeeperException.Code.NOTEMPTY))
                        throw e;
                }
            }
        }

        // try delete without checking for existence first and regardless of version, no guarantee
        public static void tryDelete(ZooKeeper zk, String path) {
            try {
                zk.delete(path, -1);
            } catch (KeeperException|InterruptedException e) {
                System.err.println(String.format("Error while deleting %s : %s", path, e.getMessage()));
            }
        }

        // try deleting all children of the path, ignore errors, no guarantee
        public static void tryDeleteAllChildren(ZooKeeper zk, String path) {
            try {
                List<String> children = zk.getChildren(path, false);
                for(String child: children) {
                    try {
                        zk.delete(path + "/" + child, -1);
                    } catch (KeeperException|InterruptedException e) {
                        System.err.println(String.format("Error while deleting %s : %s", child, e.getMessage()));
                    }
                }
            } catch (KeeperException|InterruptedException e) {
                System.err.println(String.format("Error while geting children of %s : %s", path, e.getMessage()));
            }
        }

        public static String byteToString(byte[] bytes) {
            return new String(bytes, charset);
        }

        public static byte[] StringToByte(String str) {
            return str.getBytes(charset);
        }
    }
}
