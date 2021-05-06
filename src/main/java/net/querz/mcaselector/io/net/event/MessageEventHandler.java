package net.querz.mcaselector.io.net.event;

import net.querz.mcaselector.io.ByteArrayReader;

import java.util.HashMap;
import java.util.Map;

public class MessageEventHandler {

	private final Map<Integer, MessageEvent> events = new HashMap<>();

	public void registerEvent(MessageEvent event) {
		if (events.containsKey(event.getId())) {
			throw new IllegalArgumentException("a MessageEvent with the id " + event.getId() + " is already registered");
		}
		events.put(event.getId(), event);
	}

	public byte[] execute(int id, byte[] data) {
		MessageEvent event = events.get(id);
		if (event == null) {
			throw new IllegalArgumentException("unknown event id " + id);
		}
		return event.execute(new ByteArrayReader(data));
	}
}
