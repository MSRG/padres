package ca.utoronto.msrg.padres.common.comm.socket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.socket.message.SocketMessage;

public class SocketPipe {

	private static final int BUFF_SIZE = 8 * 1024;

	private static final int RESET_COUNT = 1000;

	private Socket socket;

	private ObjectInputStream objInputStream;

	private ObjectOutputStream objOutputStream;

	private int writeCount = 0;

	public SocketPipe(Socket socket) throws CommunicationException {
		this.socket = socket;
		try {
			socket.setSendBufferSize(BUFF_SIZE);
			socket.setReceiveBufferSize(BUFF_SIZE);
			objOutputStream = new ObjectOutputStream(new BufferedOutputStream(
					socket.getOutputStream(), BUFF_SIZE));
			objOutputStream.flush();
			objInputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream(),
					BUFF_SIZE));
		} catch (IOException e) {
			throw new CommunicationException(e.getMessage());
		}
		writeCount = 0;
	}

	public SocketMessage read() throws CommunicationException {
		try {
			return (SocketMessage) objInputStream.readObject();
		} catch (EOFException e) {
			return null;
		} catch (IOException e) {
			throw new CommunicationException(e.getMessage());
		} catch (ClassNotFoundException e) {
			throw new CommunicationException(e.getMessage());
		}
	}

	public void write(SocketMessage msg) throws CommunicationException {
		try {
			objOutputStream.writeObject(msg);
			writeCount++;
			if (writeCount == RESET_COUNT) {
				objOutputStream.reset();
				writeCount = 0;
			}
			objOutputStream.flush();
		} catch (IOException e) {
			throw new CommunicationException(e.getMessage());
		}
	}

	public void close() throws CommunicationException {
		try {
			objInputStream.close();
			objOutputStream.close();
			if (!socket.isClosed())
				socket.close();
		} catch (IOException e) {
			throw new CommunicationException(e.getMessage());
		}
	}

	public String toString() {
		return String.format("%s:%d", socket.getInetAddress().getHostAddress(), socket.getPort());
	}

}
