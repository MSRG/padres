package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import java.util.Map;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.router.matching.rete.ReteMatcher;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;

/**
 * Terminal node in the Rete network. It creates the event listener that listens to terminal
 * activations.
 */
public class NodeTerminal extends Node1 {

	private static final long serialVersionUID = 1L;

	private static Logger reteLogger = Logger.getLogger(ReteMatcher.class);

	private String msgID;

	public NodeTerminal(ReteNetwork rn, String id) {
		super(rn);
		msgID = new String(id);
	}

	public String getMsgID() {
		return msgID;
	}

	public boolean callNodeLeft(Object p, int matchCount) {
		int reqCount = -1;
		if (p instanceof Publication) {
			reqCount = ((Publication) p).getPairMap().size();
		} else if (p instanceof Subscription) {
			reqCount = ((Subscription) p).getPredicateMap().size();
		} else if (p instanceof Advertisement) {
			reqCount = ((Advertisement) p).getPredicateMap().size();
		}
		if ((reteNW.getNWType() == ReteNetwork.NWType.SUB_TREE)
				|| ((reteNW.getNWType() == ReteNetwork.NWType.ADV_TREE) && (reqCount == matchCount))) {
			eventHappened(p);
			return true;
		}
		return false;
	}

	private void eventHappened(Object data) {
		if (data instanceof Publication) {
			// a publication reached a terminal node
			reteLogger.info(msgID + " satisfied by pub: " + (Publication) data);
			reteNW.collectPubMatchingSubs((Publication) data, msgID);
		} else if (data instanceof Advertisement) {
			// an advertisement reached a terminal node.
			reteLogger.info(msgID + " satisfied by adv: " + (Advertisement) data);
			reteNW.collectSubs(msgID);
		} else if (data instanceof Subscription) {
			// a subscription reached a terminal node.
			reteLogger.info(msgID + " satisfied by sub: " + (Subscription) data);
			reteNW.collectAdvs(msgID);
		} else if (data instanceof Map) {
			// publication list??
			reteLogger.info(msgID + " satisfied by " + data);
			reteNW.collectPubMatchingSubs(((Map) data).keySet(), msgID);
		}else if (data instanceof MemoryUnit) {
			reteLogger.info(msgID + " satisfied by " + data);
			reteNW.collectPubMatchingSubs(((MemoryUnit)data).getPids(), msgID);
		}
	}

	public String toString() {
		return this.getClass().getSimpleName() + " " + msgID;
	}

}
