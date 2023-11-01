package net.querz.mcaselector.overlay.overlays;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.querz.mcaselector.config.adapter.OverlayAdapter;
import net.querz.mcaselector.exception.ParseException;
import net.querz.mcaselector.io.mca.Chunk;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.overlay.AmountParser;
import net.querz.mcaselector.overlay.OverlayType;
import net.querz.mcaselector.text.TextHelper;
import net.querz.nbt.CollectionTag;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.NumberTag;
import net.querz.nbt.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomOverlay extends AmountParser {

	private List<Node> path = new ArrayList<>();
	private String root;
	private boolean size;

	private final Pattern indexPattern = Pattern.compile("^\\[(?<index>\\d+)]$");

	public CustomOverlay() {
		super(OverlayType.CUSTOM);
		setMultiValues(new String[0]);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		return switch (root) {
			case "region" -> getValue(chunkData.region());
			case "poi" -> getValue(chunkData.poi());
			case "entities" -> getValue(chunkData.entities());
			default -> 0;
		};
	}

	private int getValue(Chunk chunk) {
		if (chunk == null || chunk.getData() == null) {
			return 0;
		}
		Tag current = chunk.getData();
		for (Node node : path) {
			if (node instanceof Name name) {
				if (current instanceof CompoundTag t) {
					current = t.get(name.name);
				} else {
					return 0;
				}
			} else if (node instanceof Index index) {
				if (current instanceof ListTag l) {
					current = l.get(index.index);
				} else {
					return 0;
				}
			} else {
				return 0;
			}
		}
		if (current instanceof NumberTag number && !size) {
			return number.asInt();
		} else if (current instanceof CompoundTag compound && size) {
			return compound.size();
		} else if (current instanceof CollectionTag<?> collection && size) {
			return collection.size();
		}
		return 0;
	}

	@Override
	public String name() {
		return "Custom";
	}

	@Override
	public boolean setMultiValuesString(String raw) {
		path.clear();
		size = false;
		root = "";
		if (raw == null) {
			setMultiValues(new String[0]);
			return false;
		}
		setRawMultiValues(raw);
		String[] elements;
		try {
			elements = TextHelper.splitWithEscaping(raw, '/', '\\');
			setMultiValues(elements);
		} catch (ParseException ex) {
			setMultiValues(new String[0]);
			return false;
		}

		if (elements.length < 2) {
			setMultiValues(new String[0]);
			return false;
		}

		root = elements[0];

		for (int i = 1; i < elements.length - 1; i++) {
			if (!parseElement(elements[i])) {
				return false;
			}
		}

		try {
			String[] last = TextHelper.splitWithEscaping(elements[elements.length - 1], '.', '\\');
			if (last.length > 1 && last[last.length - 1].equals("size")) {
				size = true;
				parseElement(elements[elements.length - 1].substring(0, elements[elements.length - 1].length() - 5));
			} else {
				parseElement(elements[elements.length - 1]);
			}
		} catch (ParseException e) {
			setMultiValues(new String[0]);
			return false;
		}
		return true;
	}

	private boolean parseElement(String element) {
		Matcher m = indexPattern.matcher(element);
		if (m.find()) {
			String g = m.group("index");
			try {
				int index = Integer.parseInt(g);
				path.add(new Index(index));
			} catch (NumberFormatException ex) {
				setMultiValues(new String[0]);
				return false;
			}
		} else {
			path.add(new Name(element));
		}
		return true;
	}

	@Override
	public String toString() {
		return "root: " + root + ", path: " + path + ", size: " + size;
	}

	@Override
	public void writeCustomJSON(JsonObject obj) {
		JsonArray path = new JsonArray();
		this.path.forEach(n -> path.add(n.toString()));
		obj.add("path", path);
		obj.addProperty("root", root);
		obj.addProperty("size", size);
	}

	@Override
	public void readCustomJSON(JsonObject obj) {
		JsonArray p = OverlayAdapter.get(obj, "path", null, JsonElement::getAsJsonArray);
		if (p != null) {
			p.forEach(e -> parseElement(e.getAsString()));
		}
		root = OverlayAdapter.get(obj, "root", "", JsonElement::getAsString);
		size = OverlayAdapter.get(obj, "size", false, JsonElement::getAsBoolean);
	}

	private abstract static class Node {}

	private static class Name extends Node {
		String name;

		Name(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private static class Index extends Node {
		int index;

		Index(int index) {
			this.index = index;
		}

		@Override
		public String toString() {
			return "[" + index + "]";
		}
	}
}
