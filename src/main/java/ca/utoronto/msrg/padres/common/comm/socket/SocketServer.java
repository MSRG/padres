package ca.utoronto.msrg.padres.common.comm.socket;

import java.io.IOException;

import ca.utoronto.msrg.padres.common.comm.CommServer;
import ca.utoronto.msrg.padres.common.comm.CommSystem;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;

public class SocketServer extends CommServer {

	protected SocketConnectionListener connectionListener;

	public SocketServer(SocketAddress serverAddress, CommSystem commSystem)
			throws CommunicationException {
		super(serverAddress);
		// Start listening for clients
		connectionListener = createNewSocketConnectionListener(serverAddress);
		connectionListener.start();
		// Wait for the server to get set up
		try {
			Thread.sleep(100);
		} catch (Exception e) {
		}
	}
	
	protected SocketConnectionListener createNewSocketConnectionListener(
			SocketAddress serverAddress) throws CommunicationException {
		return new SocketConnectionListener(serverAddress, this);
	}

	@Override
	public void shutDown() throws CommunicationException {
		connectionListener.interrupt();
		try {
			connectionListener.closeSocket();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			// wait a bit for the socket to be released
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
	}

}
