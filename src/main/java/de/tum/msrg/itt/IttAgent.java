package de.tum.msrg.itt;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.common.util.Sleep;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.log4j.Logger;

import static de.tum.msrg.itt.IttUtility.ZK.*;

/**
 * Created by pxsalehi on 01.02.16.
 * Incremental topology transformation client,
 * sends stats to planner server and waits for operations
 */
public class IttAgent implements Watcher, AsyncCallback.DataCallback, AsyncCallback.StatCallback {
    private IttBrokerCore broker;
    private NodeURI thisNodeUri;
    private StatsCollectorThread statsCollectorThread;
    private volatile boolean stopped = false;
    private static final int SERVER_SOCKET_TIMEOUT_SEC = 120;
    private static final int MAX_CONNECT_RETRY = 20;
    private static final int PAUSE_LEN_MSEC = 200;
    private ScheduledExecutorService executor;
    // zoo keeper
    private String zkAdrs;
    public static final int ZK_SESSION_TIMEOUT = 10*60*1000;
    public static final String ZK_ROOT = "/itt/brokers";
    private String brokerZNode;
    private String brokerOpsZNode;
    private String brokerStatsZNode;
    private String brokerOpsStatusZnode;
    private ZooKeeper zk;
    private static Logger ittLogger = Logger.getLogger("Itt");
    private static Logger exceptionLogger = Logger.getLogger("Exception");


    public IttAgent(IttBrokerCore broker, NodeURI uri, String zkAdrs) throws IOException {
        this.broker = broker;
        thisNodeUri = uri;
        this.zkAdrs = zkAdrs;

    }

    public IttAgent(IttBrokerCore broker, String uri, String zkAdrs) throws BrokerCoreException {
        this.broker = broker;
        thisNodeUri = NodeURI.parse(uri);
        this.zkAdrs = zkAdrs;
    }

    public void start() throws IttException {
        ittLogger.info("Starting Itt agent on " + thisNodeUri.getURI());
        executor = Executors.newScheduledThreadPool(1);
        brokerZNode = ZK_ROOT + "/" + thisNodeUri.getID();
        brokerOpsZNode = brokerZNode + "/ops";
        brokerOpsStatusZnode = brokerOpsZNode + "/status";
        brokerStatsZNode = brokerZNode + "/stats";
        // create zk connection and entry for broker on zk including ops and stats subtree
        // before starting to listen to deployer and sending stats
        initZK();
        // start a thread listening for shift operation from deployer (e.g. ZooKeeper)
        // furthermore periodically send stats to plan deployer
        ittLogger.info("Starting statsCollector thread on " + thisNodeUri.getID());
        statsCollectorThread = new StatsCollectorThread(this, brokerStatsZNode, zk);
        statsCollectorThread.start();
    }

    public void stop() {
        if(stopped)
            return;
        ittLogger.info("Stopping itt agent on " + thisNodeUri.getID());
        stopped = true;
        executor.shutdown();
        // flag statsCollectorThread to stop
        statsCollectorThread.setStopped(true);
        try {
            statsCollectorThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            exceptionLogger.warn(e);
        }
        // remove ops and stats entries from zk
//        IttUtility.ZK.tryDeleteAllChildren(zk, brokerOpsZNode);
//        IttUtility.ZK.tryDeleteAllChildren(zk, brokerStatsZNode);
//        IttUtility.ZK.tryDeleteAllChildren(zk, brokerZNode);
//        IttUtility.ZK.tryDelete(zk, brokerZNode);
    }

    private void initZK() throws IttException {
        try {
            ittLogger.info(String.format("Initializing Zookeeper client on broker %s, ZK_HOST: %s, brokerZnode: %s, " +
                    "brokerOpsZnode: %s, brokerOpsStatusZnode: %s, brokerStats: %s", thisNodeUri.getID(),
                    zkAdrs, brokerZNode, brokerOpsZNode, brokerOpsStatusZnode, brokerStatsZNode));
            zk = new ZooKeeper(zkAdrs, ZK_SESSION_TIMEOUT, this);
            // assumes root exists
            IttUtility.ZK.createIfNotExists(zk, brokerZNode, null, CreateMode.PERSISTENT);
            IttUtility.ZK.createIfNotExists(zk, brokerOpsZNode, null, CreateMode.PERSISTENT);
            IttUtility.ZK.createIfNotExists(zk, brokerOpsStatusZnode,
                                            StringToByte(ZkIttCmd.Status.NONE.name()),
                                            CreateMode.PERSISTENT);
            IttUtility.ZK.createIfNotExists(zk, brokerStatsZNode, null, CreateMode.PERSISTENT);
            IttUtility.ZK.createIfNotExists(zk, brokerZNode + "/alive", null, CreateMode.EPHEMERAL);
            // set a watch on ops znode
            zk.exists(brokerOpsZNode, true, this, null);
        } catch (KeeperException | InterruptedException | IOException e) {
            String msg = "Error while initializing Zookeeper on " + thisNodeUri.getID();
            exceptionLogger.fatal(msg, e);
            throw new IttException(msg, e);
        }
    }



    public boolean isStopped() {
        return stopped;
    }

    /**
     * Executes a blocking shift between i, j and k
     * removes the link i-j, adds the link i-k
     * Throws exception if cannot perform shift
     *
     * @throws IttException
     */
    public void shift(String iUriStr, String jUriStr, String kUriStr) throws IttException, BrokerCoreException {
        NodeURI iUri = NodeURI.parse(iUriStr);
        NodeURI jUri = NodeURI.parse(jUriStr);
        NodeURI kUri = NodeURI.parse(kUriStr);
        int tries = 1;
        boolean done = false;

        if (thisNodeUri.getID().equalsIgnoreCase(iUri.getID())) {  // shift coordinator
            ittLogger.info(String.format("Executing shift(%s, %s, %s) on %s as leader.",
                    iUri.getURI(), jUri.getURI(), kUri.getURI(), thisNodeUri.getID()));
            while(!done) {  // once reached retry limit throws exception
                Sleep.sleep(tries * PAUSE_LEN_MSEC);
                try (Socket jSock = new Socket(jUri.getHost(), ShiftFollower.getListenerPort(jUri));
                     Socket kSock = new Socket(kUri.getHost(), ShiftFollower.getListenerPort(kUri));
                     InputStream jInStream = jSock.getInputStream();
                     OutputStream jOutStream = jSock.getOutputStream();
                     InputStream kInStream = kSock.getInputStream();
                     OutputStream kOutStream = kSock.getOutputStream()) {
                    // create a shift coordinator
                    ittLogger.info("Creating shift leader");
                    ShiftLeader leader = new ShiftLeader(broker, iUri, jUri, kUri, jInStream, jOutStream,
                            kInStream, kOutStream, executor);
                    ittLogger.info("Starting shift leader");
                    leader.start();
                    done = true;
                    ittLogger.info(String.format("shift(%s, %s, %s) on %s as leader finished successfully.",
                            iUri.getURI(), jUri.getURI(), kUri.getURI(), thisNodeUri.getID()));
                } catch (ConnectException e) {
                    String msg = "Shift coordinator cannot connect to follower(s)! " + e.getMessage();
                    ittLogger.error(msg, e);
                    System.err.println(msg);
                    if (tries >= MAX_CONNECT_RETRY) {
                        msg = "Reached retry limit to connect to shift followers!";
                        ittLogger.fatal(msg + e.getMessage());
                        exceptionLogger.fatal(msg, e);
                        throw new IttException(e);
                    } else {
                        int pause = ++tries * PAUSE_LEN_MSEC;
                        ittLogger.info("trying again after " + pause + " msec!");
                        System.err.println("trying again after " + pause + " seconds!");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    exceptionLogger.fatal(e);
                    throw new IttException(e);
                }
            }
        } else if (thisNodeUri.getID().equalsIgnoreCase(jUri.getID())
                || thisNodeUri.getID().equalsIgnoreCase(kUri.getID())) {  // shift follower
            ittLogger.info(String.format("Executing shift(%s, %s, %s) on %s as follower.",
                    iUri.getURI(), jUri.getURI(), kUri.getURI(), thisNodeUri.getID()));
            // listen for connection from coordinator
            try (ServerSocket serverSocket = new ServerSocket(ShiftFollower.getListenerPort(thisNodeUri))) {
                // set timer on accept
                serverSocket.setSoTimeout(SERVER_SOCKET_TIMEOUT_SEC * 1000);
                try (Socket iSock = serverSocket.accept();
                     InputStream iInStream = iSock.getInputStream();
                     OutputStream iOutStream = iSock.getOutputStream()) {
                    // create a follower
                    ShiftFollower follower = new ShiftFollower(broker, iUri, jUri, kUri, iInStream, iOutStream, executor);
                    follower.start();
                    ittLogger.info(String.format("shift(%s, %s, %s) on %s as follower finished successfully.",
                            iUri.getURI(), jUri.getURI(), kUri.getURI(), thisNodeUri.getID()));
                } catch (SocketTimeoutException e) {
                    ittLogger.error("Follower's socket.accept() timed out on " + thisNodeUri.getID(), e);
                    throw new IttException("Follower's socket.accept() timed out!", e);
                }
            } catch (IOException e) {
                exceptionLogger.fatal(e);
                e.printStackTrace();
            }
        } else {
            ittLogger.error(thisNodeUri.toString() + " is not part of this shift!");
            ittLogger.error("This node's uri " + thisNodeUri);
            ittLogger.error("Shift uris are: " + iUriStr + " " + jUri + " " + kUri);
            throw new IttWrongOperandException(thisNodeUri.toString() + " is not part of this shift!");
        }
    }

    public IttBrokerCore getBroker() {
        return broker;
    }

    public NodeURI getNodeUri() {
        return thisNodeUri;
    }

    private ZkIttCmd.Status getCurrentOperationStatus() throws IttException {
        try {
            byte[] statusData = zk.getData(brokerOpsStatusZnode, true, null);
            ZkIttCmd.Status curStatus = ZkIttCmd.Status.valueOf(byteToString(statusData).toUpperCase());
            return curStatus;
        } catch (KeeperException | InterruptedException e) {
            ittLogger.error("Cannot read current operation status on " + thisNodeUri.getID() + ". " + e.getMessage(), e);
            throw new IttException("Cannot read current operation status! " + e.getMessage(), e);
        }
    }

    private void setCurrentOperationStatus(ZkIttCmd.Status status) throws IttException {
        try {
            zk.setData(brokerOpsStatusZnode, StringToByte(status.name()), -1);
        } catch (KeeperException | InterruptedException e) {
            ittLogger.error("Cannot write current operation status on " + thisNodeUri.getID() + ". " + e.getMessage(), e);
            throw new IttException("Cannot write current operation status! " + e.getMessage(), e);
        }
    }

    // gets called when there is a a change to the znode /itt/brokers/broker_id/ops
    @Override
    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
        if(rc != KeeperException.Code.OK.intValue()) {
            ittLogger.error(String.format("Error while receiving data from ZK path %s, error_code:%d", path, rc));
            return;
        }
        if(data.length == 0) {
            ittLogger.warn(String.format("Empty data while receiving data from ZK path %s, error_code:%d", path, rc));
            return;
        }
        try {
            // get status of current operation
            ZkIttCmd.Status curStatus = getCurrentOperationStatus();
            if (curStatus == ZkIttCmd.Status.FINISHED || curStatus == ZkIttCmd.Status.NONE) {
                // broker is free to start a new operation
                try {
                    ZkIttCmd ittCmd = ZkIttCmd.parse(data);
                    ittLogger.info(String.format("Executing itt command %s on %s", ittCmd, thisNodeUri.getID()));
                    if (ittCmd.type == ZkIttCmd.Type.SHIFT) {
                        setCurrentOperationStatus(ZkIttCmd.Status.STARTED);
                        shift(ittCmd.operands.get(0), ittCmd.operands.get(1), ittCmd.operands.get(2));
                        setCurrentOperationStatus(ZkIttCmd.Status.FINISHED);
                    } else if (ittCmd.type == ZkIttCmd.Type.MOVE) {
                        RuntimeException e = new RuntimeException("Move is not implemented yet!");
                        ittLogger.error(e);
                        throw e;
                    }
                } catch (IttException | BrokerCoreException e) {
                    setCurrentOperationStatus(ZkIttCmd.Status.ERROR);
                    ittLogger.error("Error while executing shift on " + thisNodeUri.getID() + ". " + e.getMessage(), e);
                }
            } else if (curStatus == ZkIttCmd.Status.STARTED) {
                System.out.println("There is a operation running, will not call shift!");
                ittLogger.error("Received itt command on " + thisNodeUri.getID() + ", but current op is not finished!");
            } else {
                System.out.println("Last operation encountered an error! Restart/recover broker first!");
                ittLogger.error("Last operation encountered an error! Restart/recover broker first!");
            }
        } catch (IttException e) {
            System.err.println(e.getMessage());
        }
    }

    // exists callback
    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        if(rc == KeeperException.Code.OK.intValue() && path.equals(brokerOpsZNode))
            if(stat.getDataLength() > 0)
                zk.getData(brokerOpsZNode, true, this, null);
    }

    // watcher callback
    @Override
    public void process(WatchedEvent event) {
        if(event.getType() == Event.EventType.NodeDataChanged && event.getPath().equals(brokerOpsZNode)) {
            zk.getData(brokerOpsZNode, true, this, null);
        }
    }

    public String getBrokerOpsZNode() {
        return brokerOpsZNode;
    }

    public String getBrokerOpsStatusZnode() {
        return brokerOpsStatusZnode;
    }
}
