package de.tum.msrg.itt;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Executing shift(i, j, k) includes the following messages:
 *
 *      --5)fin->     --6)fin->
 *      <-4)ack--     <-3)ack--
 *      --1)init->    --2)init->
 *   i ============ j =========== k
 *
 *   The following implementation is based on i being the coordinator which means
 *   j and k exchange messages together via i rather than directly. This simplifies the
 *   shift implementation
 */

public class ShiftLeader {
    private NodeURI iUri;
    private NodeURI jUri;
    private NodeURI kUri;
    private IttBrokerCore broker;
    private ObjectInputStream jInStream;
    private ObjectOutputStream jOutStream;
    private ObjectInputStream kInStream;
    private ObjectOutputStream kOutStream;
    private Shift.STATE curState;
    private ScheduledExecutorService executor;
    public static final int SHIFT_TIMEOUT_SEC = 60;
    private ScheduledFuture<?> timer = null;
    // if true, no shift is if progress. If current shift could not finish successfully,
    // coordinator has already sent proper error messages to let everyone know
    private boolean terminated = false;
    // if SRT and PRT have been modified in the ACK stage, this is flagged
    // required to know if it is possible to recover from unsuccessful shifts
    private boolean tablesModified = false;
    private static Logger ittLogger = Logger.getLogger("Itt");

    public ShiftLeader(IttBrokerCore broker, NodeURI iUri, NodeURI jUri, NodeURI kUri,
                       InputStream jInStream, OutputStream jOutStream,
                       InputStream kInStream, OutputStream kOutStream,
                       ScheduledExecutorService executorService) throws IOException, IttException {
        this.broker = broker;
        this.iUri = iUri;
        this.jUri = jUri;
        this.kUri = kUri;
        this.jOutStream = new ObjectOutputStream(jOutStream);
        this.kOutStream = new ObjectOutputStream(kOutStream);
        this.jInStream = new ObjectInputStream(jInStream);
        this.kInStream = new ObjectInputStream(kInStream);
        this.executor = executorService;
        // check this broker is i and one of the shift operands
        NodeURI thisNodeUri = broker.getNodeURI();
        if(!thisNodeUri.getID().equalsIgnoreCase(iUri.getID()))
            throw new IttException("Shift leader must be the i operand of a shift!");
        curState = Shift.STATE.INIT;
    }

    // blocking
    public void start() throws IttException {
        // keep stats during operation
        int startQueueSize = -1, endQueueSize = -1;
        long startTime = -1, endTime = -1;
        // setup timeout for this shift
        timer = startShiftTimer();
        try {
            startTime = new Date().getTime();
            ittLogger.info(String.format("Pausing traffic on %s.", broker.getBrokerID()));
            broker.pauseNormalTraffic();
            startQueueSize = broker.getInputQueueSize();
            // send init to j
            Shift.Msg req = new Shift.Msg(iUri, jUri, kUri, Shift.STATE.INIT).setSender(iUri);
            ittLogger.info(String.format("Sending init request to %s (b_j): %s", jUri, req));
            jOutStream.writeUnshared(req);
            jOutStream.flush();
            curState = Shift.STATE.PRE_ACK;
            // wait for pre-ack from j
            ittLogger.info(String.format("Waiting for pre-ack from j on %s.", broker.getBrokerID()));
            Shift.Msg resp = (Shift.Msg) jInStream.readUnshared();
            verifyResponseFromJ(resp, Shift.STATE.PRE_ACK);
            ittLogger.info(String.format("Received pre-ack from j on %s.", broker.getBrokerID()));
            if(resp.getAdvIdsReceivedFromI() == null || resp.getSubIdsReceivedFromI() == null || resp.getSubsOnJ() == null) {
                String errorMsg = "Set of advs/subs ids on j cannot be null!";
                sendError(Shift.STATE.ERROR, errorMsg, jOutStream);
                sendError(Shift.STATE.ERROR, errorMsg, kOutStream);
                terminated = true;
                throw new IttException(errorMsg);
            }
            req.setAdvIdsReceivedFromI(resp.getAdvIdsReceivedFromI());
            req.setSubIdsReceivedFromI(resp.getSubIdsReceivedFromI());
            req.setSubsOnJ(resp.getSubsOnJ());
            // send init to k
            ittLogger.info(String.format("Sending init request to %s (b_k): %s", kUri, req));
            kOutStream.writeUnshared(req);
            req.setAdvIdsReceivedFromI(null).setSubIdsReceivedFromI(null).setSubsOnJ(null);
            // wait for ack from k
            // to make sure we read a new object not a back reference (stale) one use readUnshared
            ittLogger.info(String.format("Waiting for ack from k on %s.", broker.getBrokerID()));
            resp = (Shift.Msg) kInStream.readUnshared();
            verifyResponseFromK(resp, Shift.STATE.ACK);
            ittLogger.info(String.format("Received ack from k on %s.", broker.getBrokerID()));
            // send k's ack to j
            req.setState(Shift.STATE.ACK);
            ittLogger.info(String.format("Sending k's ack to %s (b_j): %s", jUri, req));
            // to avoid back referencing either create a new request or use writeUnshared, or reset the outputStream
            jOutStream.writeUnshared(req);
            // wait for ack from j
            ittLogger.info(String.format("Waiting for ack from j on %s.", broker.getBrokerID()));
            resp = (Shift.Msg) jInStream.readUnshared();
            verifyResponseFromJ(resp, Shift.STATE.ACK);
            ittLogger.info(String.format("Received ack from j on %s.", broker.getBrokerID()));
            // TODO: should you wait until output queues are empty?
            // for all queue where destination is broker, wait until it is empty
            // change routing table
            ittLogger.info(String.format("Modifying routing tables on %s.", broker.getBrokerID()));
            broker.modifySRT(jUri, kUri);
            broker.modifyPRT(jUri, kUri);
            tablesModified = true;
            // change links
            ittLogger.info(String.format("Changing topology links on %s.", broker.getBrokerID()));
            broker.disconnectFromNeighbor(jUri);
            broker.connectToNeighbor(kUri);
            curState = Shift.STATE.ACK;
            // send finish to j
            ittLogger.info(String.format("Sending finish request to %s (b_j): %s", jUri, req));
            req.setState(Shift.STATE.FINISH);
            jOutStream.writeUnshared(req);
            // wait for finish from j
            resp = (Shift.Msg) jInStream.readUnshared();
            verifyResponseFromJ(resp, Shift.STATE.FINISH);
            // send finish to k
            ittLogger.info(String.format("Sending finish request to %s (b_k): %s", kUri, req));
            kOutStream.writeUnshared(req);
            // wait for finish from k
            resp = (Shift.Msg) kInStream.readUnshared();
            verifyResponseFromK(resp, Shift.STATE.FINISH);
        } catch (IOException | ClassNotFoundException e) {
            // timeout can already terminate the shift, which can cause pending i/o to throw
            // therefore, no need for termination if already timed out
            ittLogger.error("Error on leader! " + e.getMessage(), e);
            if(!terminated) {
                sendError(Shift.STATE.ERROR, "Error on broker i. " + e.getMessage(), jOutStream);
                sendError(Shift.STATE.ERROR, "Error on broker i. " + e.getMessage(), kOutStream);
                terminated = true;
            }
            throw new IttException(e);
        } finally {
            endTime = new Date().getTime();
            // in any case resume processing messages
            ittLogger.info("Resuming traffic");
            endQueueSize = broker.getInputQueueSize();
            broker.resumeNormalTraffic();
            broker.writeStatistics(String.format("itt_shift_queue_size:{%d:%d, %d:%d}",
                    startTime, startQueueSize, endTime, endQueueSize));
            // no need for a timer anymore, if it is still running
            if(!timer.isDone()) {
                ittLogger.info("Cancelling shift timer");
                timer.cancel(true);
            }
            closeAllStreams();
            terminated = true;
        }
    }

    private ScheduledFuture<?> startShiftTimer() {
        return executor.schedule(() -> {
            // send timeout to j and k
            sendError(Shift.STATE.TIMEOUT, "shift timed out on i!", jOutStream);
            sendError(Shift.STATE.TIMEOUT, "shift timed out on i!", kOutStream);
            // close streams to stop any pending i/o which might cause the timeout
            closeAllStreams();
            terminated = false;
        }, SHIFT_TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    private void sendError(Shift.STATE error, String msg, ObjectOutputStream followerOutStream) {
        // create an error message
        Shift.Msg errorMsg = new Shift.Msg(iUri, jUri, kUri, error).setSender(iUri).setErrorMsg(msg);
        // send it to follower
        try {
            followerOutStream.writeUnshared(errorMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: merge following two methods
    private void verifyResponseFromJ(Shift.Msg resp, Shift.STATE expected) throws IttException {
        if(resp.getState() == Shift.STATE.ERROR || resp.getState() == Shift.STATE.TIMEOUT) {
            // forward error to k and terminate shift
            sendError(resp.getState(), resp.getErrorMsg(), kOutStream);
            terminated = true;
            throw new IttException("Cannot execute shift, error on j. " + resp.getErrorMsg());
        } else if(resp.getState() != expected) {
            // wrong state, send error to j and k and terminate
            String errorMsg = String.format("Expected %s from j but received %s.", expected, resp.getState());
            sendError(Shift.STATE.ERROR, errorMsg, jOutStream);
            sendError(Shift.STATE.ERROR, errorMsg, kOutStream);
            terminated = true;
            throw new IttException(errorMsg);
        }
        assert resp.getState() == expected;
    }

    private void verifyResponseFromK(Shift.Msg resp, Shift.STATE expected) throws IttException {
        if(resp.getState() == Shift.STATE.ERROR || resp.getState() == Shift.STATE.TIMEOUT) {
            // forward error to j and terminate shift
            sendError(resp.getState(), resp.getErrorMsg(), jOutStream);
            terminated = true;
            throw new IttException("Cannot execute shift, error on k. " + resp.getErrorMsg());
        } else if(resp.getState() != expected) {
            // wrong state, send error to j and k and terminate
            String errorMsg = String.format("Expected %s from k but received %s.", expected, resp.getState());
            sendError(Shift.STATE.ERROR, errorMsg, jOutStream);
            sendError(Shift.STATE.ERROR, errorMsg, kOutStream);
            terminated = true;
            throw new IttException(errorMsg);
        }
        assert resp.getState() == expected;
    }

    private void closeStream(Closeable stream) {
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeAllStreams() {
        closeStream(jOutStream);
        closeStream(jInStream);
        closeStream(kOutStream);
        closeStream(kInStream);
    }

    public boolean isTablesModified() {
        return tablesModified;
    }
}
