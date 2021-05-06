package net.querz.mcaselector.io.net.client;

import net.querz.mcaselector.io.net.event.MessageEvent;
import net.querz.mcaselector.io.net.event.MessageEventHandler;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client {

	private final String host;
	private final int port;

	private Socket socket;

	private DataInputStream inputStream;
	private DataOutputStream outputStream;

	private final MessageEventHandler eventHandler;

	private boolean closed;

	public Client(String host, int port, MessageEventHandler eventHandler) {
		this.host = host;
		this.port = port;
		this.eventHandler = eventHandler;
	}

	public MessageEventHandler getMessageEventHandler() {
		return eventHandler;
	}

	public void start() throws IOException {
		socket = new Socket(host, port);
		inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

		Thread clientThread = new Thread(() -> {
			try {
				while (!closed) {
					int id = inputStream.readInt();
					System.out.println("received id");
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
			} catch (IOException ex) {
				System.out.println("lost connection to server");
			} finally {
				closed = true;
			}
		}, "client");
		clientThread.start();
		System.out.println("client started");
	}

	public void stop() throws IOException {
		closed = true;
		inputStream.close();
		outputStream.close();
		socket.close();
	}

	public void sendEvent(MessageEvent event) {
		try {
			send(event.getId(), event.getData());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void send(int id, byte[] data) throws IOException {
		outputStream.writeInt(id);
		outputStream.writeInt(data.length);
		outputStream.write(data);
		outputStream.flush();
	}
}
