package ca.utoronto.msrg.padres.common.comm.socket;

import java.io.IOException;
import java.net.ServerSocket;

import ca.utoronto.msrg.padres.common.comm.CommunicationException;

public class SocketConnectionListener extends Thread {

	private ServerSocket serverSocket;

	private SocketServer parentServer;

	public SocketConnectionListener(SocketAddress serverAddress, SocketServer parentServer)
			throws CommunicationException {
		try {
			serverSocket = new ServerSocket(serverAddress.getPort());
		} catch (IOException e) {
			throw new CommunicationException("Error in starting socket server (" + serverAddress
					+ "): " + e.getMessage());
		}
		this.parentServer = parentServer;
	}

	public void run() {
		// Create a server socket to accept client connection requests
		// create a thread group for incomming connections
		ThreadGroup connectionGroup = null;
		try {
			connectionGroup = new ThreadGroup(parentServer.getServerURI());
		} catch (CommunicationException e) {
			e.printStackTrace();
			System.exit(1);
		}
		// Run forever, accepting and servicing connections
		while (!this.isInterrupted()) {
			// Get client connection
			try {
				SocketPipe clientPipe = new SocketPipe(serverSocket.accept());
				// create a thread to handle communication with incoming connection
				SocketClientConnection clientConnection = createSocketClientConnection(parentServer,
						clientPipe, connectionGroup);
				clientConnection.start();
			} catch (IOException e) {
				// it is ok to get this exception if the thread is interrupted
				if (!Thread.currentThread().isInterrupted()) {
					e.printStackTrace();
					System.exit(1);
				}
			} catch (CommunicationException e) {
				// TODO handle exception
				e.printStackTrace();
			}
		}
		connectionGroup.interrupt();
	}

	protected SocketClientConnection createSocketClientConnection(
			SocketServer parentServer2, SocketPipe clientPipe,
			ThreadGroup connectionGroup) throws CommunicationException {
		return new SocketClientConnection(parentServer2, clientPipe, connectionGroup);
	}

	/**
	 * Force the connection listener to stop accepting any more connections
	 * 
	 * @throws IOException
	 *             Error in closing the server socket
	 */
	public void closeSocket() throws IOException {
		serverSocket.close();
	}

}
