package de.tum.msrg.itt;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by pxsalehi on 13.07.16.
 */
public class ShiftFollower {
    // shift operands
    private NodeURI iUri;
    private NodeURI jUri;
    private NodeURI kUri;
    private IttBrokerCore broker;
    private NodeURI thisNodeUri;
    private ObjectInputStream iInStream;
    private ObjectOutputStream iOutStream;
    private Shift.STATE curState;
    private ScheduledExecutorService executor;
    public static final int SHIFT_TIMEOUT_SEC = 60;
    private ScheduledFuture<?> timer = null;
    // if true, no shift is in progress. If could not finish successfully, follower
    // has already sent proper error messages to let coordinator know
    private boolean terminated = false;
    // if SRT and PRT have been modified in the ACK stage, this is flagged
    // required to know if it is possible to recover from unsuccessful shifts
    private boolean tablesModified = false;
    private static Logger ittLogger = Logger.getLogger("Itt");

    public ShiftFollower(IttBrokerCore broker, NodeURI iUri, NodeURI jUri, NodeURI kUri,
                         InputStream iInStream, OutputStream iOutStream,
                         ScheduledExecutorService executorService) throws IOException, IttException {
        this.broker = broker;
        this.iUri = iUri;
        this.jUri = jUri;
        this.kUri = kUri;
        this.iInStream = new ObjectInputStream(iInStream);
        this.iOutStream = new ObjectOutputStream(iOutStream);
        this.executor = executorService;
        // check this broker is j or k and is one of shift operands
        thisNodeUri = broker.getNodeURI();
        if(!thisNodeUri.getID().equalsIgnoreCase(jUri.getID()) && !thisNodeUri.getID().equalsIgnoreCase(kUri.getID())) {
            throw new IttException("Shift follower must be the j or k operand of a shift! " +
                    " thisNode:" + thisNodeUri +
                    " juri: " + jUri + " kUri: " + kUri);
        }
        curState = Shift.STATE.PRE_INIT;
    }

    // blocking
    public void start() throws IttException {
        int startQueueSize = -1, endQueueSize = -1;
        long startTime = -1, endTime = -1;
        // setup timeout for this shift
        timer = startShiftTimer();
        try {
            startTime = new Date().getTime();
            if(curState != Shift.STATE.PRE_INIT) {
                String errorMsg = "Follower " + thisNodeUri.getID() + " is not in PRE_INIT state!";
                sendError(Shift.STATE.ERROR, errorMsg);
                terminated = true;
                throw new IttException(errorMsg);
            }
            // wait for init req from coordinator
            ittLogger.info(String.format("Waiting for init from i on %s.", broker.getBrokerID()));
            Shift.Msg req = (Shift.Msg) iInStream.readUnshared();
            verifyCoordinatorRequest(req, Shift.STATE.INIT);
            ittLogger.info(String.format("Received init from i on %s.", broker.getBrokerID()));
            // do init stuff
            ittLogger.info(String.format("Pausing traffic on %s.", broker.getBrokerID()));
            broker.pauseNormalTraffic();
            startQueueSize = broker.getInputQueueSize();
            curState = Shift.STATE.INIT;
            // if this node is j, send pre_ack and wait for ack
            Shift.Msg resp = new Shift.Msg(iUri, jUri, kUri).setSender(thisNodeUri);
            if(thisNodeUri.getID().equalsIgnoreCase(jUri.getID())) {
                resp.setState(Shift.STATE.PRE_ACK);
                // send back id of advs and subs coming from i, needed for k's routing table update
                resp.setAdvIdsReceivedFromI(broker.getAdvIdsWithLastHopFrom(iUri));
                resp.setSubIdsReceivedFromI(broker.getSubIdsWithLastHopFrom(iUri));
                // send back set of subs not coming from k, which might be new to k
                resp.setSubsOnJ(broker.getAllSubsWithLastHopNotFrom(kUri));
                ittLogger.info(String.format("Sending pre_ack to %s (b_i): %s", iUri, req));
                iOutStream.writeUnshared(resp);
                resp.setAdvIdsReceivedFromI(null).setSubIdsReceivedFromI(null);
                // wait for ack from i
                ittLogger.info(String.format("Waiting for ack from i on %s.", broker.getBrokerID()));
                req = (Shift.Msg) iInStream.readUnshared();
                verifyCoordinatorRequest(req, Shift.STATE.ACK);
                ittLogger.info(String.format("Received ack from i on %s.", broker.getBrokerID()));
                curState = Shift.STATE.PRE_ACK;
                ittLogger.info("Modifying SRT");
                broker.modifySRT(iUri, kUri);
                broker.modifyPRT(iUri, kUri);
                // change links
                ittLogger.info("Changing topology links. Disconnecting from " + iUri.getID());
                broker.disconnectFromNeighbor(iUri);
                ittLogger.info("Finished changing topology links.");
            } else {  // modifying k's routing tables
                assert thisNodeUri.getID().equalsIgnoreCase(kUri.getID());
                ittLogger.info("Modifying SRT and PRT");
                broker.modifySRT(jUri, iUri, req.getAdvIdsReceivedFromI());
                broker.modifyPRT(jUri, iUri, req.getSubIdsReceivedFromI());
                //make sure any sub on j missing from k, is added to k
                broker.addMissingSubsToPRT(req.getSubsOnJ(), iUri, jUri);
                // change links
                ittLogger.info("Changing topology links. Connecting to " + iUri.getID());
                broker.connectToNeighbor(iUri);
            }
            tablesModified = true;
            curState = Shift.STATE.ACK;
            // send back ack to coordinator
            resp.setState(Shift.STATE.ACK);
            ittLogger.info(String.format("Sending ack to %s (b_i): %s", iUri, resp));
            iOutStream.writeUnshared(resp);
            // wait for finish
            ittLogger.info(String.format("Waiting for finish from i on %s.", broker.getBrokerID()));
            req = (Shift.Msg) iInStream.readUnshared();
            verifyCoordinatorRequest(req, Shift.STATE.FINISH);
            ittLogger.info(String.format("Received finish from i on %s.", broker.getBrokerID()));
            curState = Shift.STATE.FINISH;
            // send back finish
            resp.setState(Shift.STATE.FINISH);
            ittLogger.info(String.format("Sending finish to %s (b_i): %s", iUri.getID(), resp));
            iOutStream.writeUnshared(resp);
        } catch (IOException | ClassNotFoundException e) {
            // timeout can already terminate the shift, which can cause pending i/o to throw
            // therefore, no need for termination if already timed out
            ittLogger.error("Error on follower! " + e.getMessage(), e);
            if (!terminated) {
                sendError(Shift.STATE.ERROR, "Error on follower " + thisNodeUri.getID() + ". " + e.getMessage());
                terminated = true;
            }
            throw new IttException(e);
        } finally {
            endTime = new Date().getTime();
            // in any case resume message processing
            ittLogger.info("Resuming traffic");
            endQueueSize = broker.getInputQueueSize();
            broker.resumeNormalTraffic();
            broker.writeStatistics(String.format("itt_shift_queue_size:{%d:%d, %d:%d}",
                    startTime, startQueueSize, endTime, endQueueSize));
            // no need for a timer anymore, if it is still running
            if (!timer.isDone()) {
                ittLogger.info("Cancelling shift timer");
                timer.cancel(true);
            }
            closeStream(iInStream);
            closeStream(iOutStream);
            terminated = true;
        }
    }

    private ScheduledFuture<?> startShiftTimer() {
        return executor.schedule(() -> {
            // send timeout to coordinator
            sendError(Shift.STATE.TIMEOUT, "shift timed out on follower " + thisNodeUri.getID());
            // close streams to stop any pending i/o which might cause the timeout
            closeStream(iInStream);
            closeStream(iOutStream);
            terminated = false;
        }, SHIFT_TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    private void sendError(Shift.STATE error, String msg) {
        // create an error message
        Shift.Msg errorMsg = new Shift.Msg(iUri, jUri, kUri, error).setSender(thisNodeUri).setErrorMsg(msg);
        // send it to coordinator
        try {
            iOutStream.writeUnshared(errorMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void verifyCoordinatorRequest(Shift.Msg req, Shift.STATE expected) throws IttException {
        if (req.getState() == Shift.STATE.ERROR || req.getState() == Shift.STATE.TIMEOUT) {
            // forward error to coordinator and terminate shift
            sendError(req.getState(), req.getErrorMsg());
            terminated = true;
            throw new IttException("Cannot execute shift. Received error on follower "
                    + thisNodeUri.getID() + ". " + req.getErrorMsg());
        } else if (req.getState() != expected) {
            // wrong state, send error to coordinator and terminate
            String errorMsg = String.format("Follower %s expected %s but received %s from %s.",
                    thisNodeUri.getID(), expected, req.getState(), req.getSender().getID());
            sendError(Shift.STATE.ERROR, errorMsg);
            terminated = true;
            throw new IttException(errorMsg);
        }
        assert req.getState() == expected;
    }

    private void closeStream(Closeable stream) {
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isTablesModified() {
        return tablesModified;
    }

    public static int getListenerPort(NodeURI uri) {
        return uri.getPort() + 1;
    }
}