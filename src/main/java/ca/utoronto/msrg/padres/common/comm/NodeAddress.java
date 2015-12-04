package ca.utoronto.msrg.padres.common.comm;

/**
 * Created by chris on 25.09.15.
 */
public interface NodeAddress {
    CommSystem.CommSystemType getType();

    String getNodeURI();

    String getNodeID();

    String getHost();

    int getPort();
}
