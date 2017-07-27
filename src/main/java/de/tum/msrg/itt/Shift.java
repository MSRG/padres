package de.tum.msrg.itt;

import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;

import java.io.Serializable;
import java.util.Set;

public class Shift {
    public enum STATE {
        PRE_INIT,   // waiting for an init, open to a new shift and not busy
        INIT,       // initialized, starting a shift
        PRE_ACK,    // waiting for an ack
        ACK,        // received ack and waiting for finish
        FINISH,     // finish shift and resume message processing
        ERROR,
        TIMEOUT
    }

    public static class Msg implements Serializable {
        private STATE state = STATE.PRE_INIT;
        private String errorMsg;
        private NodeURI biURI;
        private NodeURI bjURI;
        private NodeURI bkURI;
        private NodeURI sender;
        // id of advs on j which came from i
        private Set<String> advIdsReceivedFromI = null;
        // id of subs on j which came from i
        private Set<String> subIdsReceivedFromI = null;
        // set of subs on j which k will go through and make sure has all of them
        // otherwise add missing ones to its table
        private Set<SubscriptionMessage> subsOnJ = null;

        public Msg(NodeURI biURI, NodeURI bjURI, NodeURI bkURI) {
            this.biURI = biURI;
            this.bjURI = bjURI;
            this.bkURI = bkURI;
        }

        public Msg(NodeURI biURI, NodeURI bjURI, NodeURI bkURI, STATE state) {
            this(biURI, bjURI, bkURI);
            this.state = state;
        }

        public Msg createErrorMsg(NodeURI receiver, String errorMsg) {
            state = STATE.ERROR;
            this.errorMsg = errorMsg;
            return this;
        }

        public NodeURI getBiURI() {
            return biURI;
        }

        public NodeURI getBjURI() {
            return bjURI;
        }

        public NodeURI getBkURI() {
            return bkURI;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        public Msg setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
            return this;
        }

        public NodeURI getSender() {
            return sender;
        }

        public Msg setSender(NodeURI sender) {
            this.sender = sender;
            return this;
        }

        public STATE getState() {
            return state;
        }

        public Msg setState(STATE state) {
            this.state = state;
            return this;
        }

        public Set<String> getAdvIdsReceivedFromI() {
            return advIdsReceivedFromI;
        }

        public Msg setAdvIdsReceivedFromI(Set<String> advIdsReceivedFromI) {
            this.advIdsReceivedFromI = advIdsReceivedFromI;
            return this;
        }

        public Set<String> getSubIdsReceivedFromI() {
            return subIdsReceivedFromI;
        }

        public Msg setSubIdsReceivedFromI(Set<String> subIdsReceivedFromI) {
            this.subIdsReceivedFromI = subIdsReceivedFromI;
            return this;
        }

        public Set<SubscriptionMessage> getSubsOnJ() {
            return subsOnJ;
        }

        public Msg setSubsOnJ(Set<SubscriptionMessage> subsOnJ) {
            this.subsOnJ = subsOnJ;
            return this;
        }

        @Override
        public String toString() {
            return "ShiftMsg{" +
                    "biURI=" + biURI +
                    ", bjURI=" + bjURI +
                    ", bkURI=" + bkURI +
                    ", state=" + state +
                    ", sender=" + sender +
                    ", errorMsg='" + errorMsg + '\'' +
                    '}';
        }
    }
}
