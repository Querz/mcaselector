package net.querz.mcaselector.io.net.server;

import net.querz.mcaselector.io.net.event.MessageEventHandler;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public class ClientHandler implements Runnable, Closeable {

	private final DataInputStream inputStream;
	private final DataOutputStream outputStream;
	private final Socket clientSocket;
	private final MessageEventHandler eventHandler;

	private final int id;

	private boolean closed;
	private final Runnable onCloseFunc;

	public ClientHandler(int id, Socket clientSocket, MessageEventHandler eventHandler, Runnable onCloseFunc) throws IOException {
		this.id = id;
		this.clientSocket = clientSocket;
		inputStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
		outputStream = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
		this.eventHandler = eventHandler;
		this.onCloseFunc = onCloseFunc;
	}

	@Override
	public void close() throws IOException {
		closed = true;
		try {
			inputStream.close();
			outputStream.close();
			clientSocket.close();
		} finally {
			onCloseFunc.run();
		}
	}

	@Override
	public void run() {
		try {
			while (!closed) {
				int id = inputStream.readInt();
				int length = inputStream.readInt();

				byte[] data = new byte[length];
				inputStream.read(data);

				byte[] response = null;
				try {
					response = eventHandler.execute(id, data);
				} catch (Exception ex) {
					System.out.println("error handling message: " + ex.getMessage());
				}
				if (response != null) {
					send(id, response);
				}
			}
		} catch (Exception ex) {
			System.out.println("client " + id + " lost connection");
		} finally {
			closed = true;
			onCloseFunc.run();
		}
	}

	public synchronized void send(int id, byte[] data) throws IOException {
		outputStream.writeInt(id);
		outputStream.writeInt(data.length);
		outputStream.write(data);
		outputStream.flush();
	}

	public int getId() {
		return id;
	}
}
