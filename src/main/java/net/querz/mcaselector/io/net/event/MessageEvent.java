package net.querz.mcaselector.io.net.event;

import net.querz.mcaselector.io.ByteArrayReader;

public abstract class MessageEvent {

	private final int id;

	public MessageEvent(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public abstract byte[] getData();

	public abstract byte[] execute(ByteArrayReader ptr);
}
