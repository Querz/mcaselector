package net.querz.mcaselector.io.net.server;

import net.querz.mcaselector.io.net.client.Client;
import net.querz.mcaselector.io.net.event.MessageEventHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {

	public static void main(String[] args) throws IOException, InterruptedException {
		MessageEventHandler eventHandler = new MessageEventHandler();
		Server server = new Server("localhost", 1234, eventHandler);
		server.start();

		Client client = new Client("localhost", 1234, eventHandler);
		client.start();
		client.send(1, "testing".getBytes());
		client.send(5, "foo bar".getBytes());

		server.broadcast(0, "server message".getBytes());


		Thread.sleep(Long.MAX_VALUE);
	}

	private final String host;
	private final int port;

	private final MessageEventHandler eventHandler;

	private boolean running;
	private ServerSocket serverSocket;

	private final Map<Integer, ClientHandler> clients = new HashMap<>();

	private int currentClientId = 0;

	public Server(String host, int port, MessageEventHandler eventHandler) {
		this.host = host;
		this.port = port;
		this.eventHandler = eventHandler;
	}

	public void start() throws IOException {
		running = true;
		serverSocket = new ServerSocket(port, 0, InetAddress.getByName(host));

		Thread thread = new Thread(() -> {
			while (running) {
				Socket clientSocket;
				ClientHandler client;
				final int id = currentClientId;

				try {
					clientSocket = serverSocket.accept();
					client = new ClientHandler(id, clientSocket, eventHandler, () -> clients.remove(id));
				} catch (IOException e) {
					if (!serverSocket.isClosed()) {
						System.out.println("failed to accept client");
						e.printStackTrace();
					}
					System.out.println("server closed");
					return;
				}

				clients.put(id, client);
				Thread clientThread = new Thread(client, "clientHandler-" + client.getId());
				clientThread.start();

				currentClientId++;
			}
		}, "server");
		thread.start();
		System.out.println("server started");
	}

	public void stop() throws IOException {
		running = false;
		for (Map.Entry<Integer, ClientHandler> entry : clients.entrySet()) {
			entry.getValue().close();
		}
		serverSocket.close();
	}

	public void broadcast(int id, byte[] data) {
		for (Map.Entry<Integer, ClientHandler> entry : clients.entrySet()) {
			ClientHandler client = entry.getValue();
			try {
				client.send(id, data);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
