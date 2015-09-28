package ca.utoronto.msrg.padres.common.comm.zero;

import ca.utoronto.msrg.padres.common.comm.*;

/**
 * Created by chris on 25.09.15.
 */
public class ZeroSocketServer extends CommServer {


    /**
     * Constructor for the server. It parse the given serverURI into the {@link #serverAddress}, but
     * it does not actually create the server that accepts the connections and messages. It has to
     * be handled in the constructors of the subclasses.
     *
     * @param serverAddress @throws CommunicationException
     *                             When there is error is parsing the given URI
     * @param commSystem
     * @see ConnectionHelper#getAddress(String)
     */
    public ZeroSocketServer(NodeAddress serverAddress, CommSystem commSystem) throws CommunicationException {
        super(serverAddress);

    }

    @Override
    public void shutDown() throws CommunicationException {

    }
}
