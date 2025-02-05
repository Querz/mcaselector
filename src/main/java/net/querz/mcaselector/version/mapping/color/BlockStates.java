package net.querz.mcaselector.version.mapping.color;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.StringTag;
import java.io.IOException;
import java.util.*;

public class BlockStates {

	private final Map<String, Map<String, Integer>> states;
	private final transient int size;
	private final transient int waterlogged;

	public BlockStates(Map<String, Map<String, Integer>> states, int size) {
		this.states = states;
		this.size = size;
		this.waterlogged = states.get("waterlogged").get("true");
	}

	public BlockStates(Map<String, Map<String, Integer>> states) {
		this.states = states;
		size = states.values().stream().mapToInt(Map::size).sum();
		this.waterlogged = states.get("waterlogged").get("true");
	}

	public BitSet getState(CompoundTag properties) {
		if (properties == null) {
			return null;
		}
		BitSet state = new BitSet(size);
		properties.forEach((k, v) -> {
			Map<String, Integer> map;
			if ((map = states.get(k)) != null) {
				Integer i;
				if ((i = map.get(((StringTag) v).getValue())) != null) {
					state.set(i);
				}
			}
		});
		return state;
	}

	public BitSet getState(Map<String, String> properties) {
		BitSet state = new BitSet();
		properties.forEach((k, v) -> {
			Map<String, Integer> map;
			if ((map = states.get(k)) != null) {
				Integer i;
				if ((i = map.get(v)) != null) {
					state.set(i);
				}
			}
		});
		return state;
	}

	public BitSet getState(String properties) {
		if (properties.isEmpty()) {
			return null;
		}
		BitSet state = new BitSet();
		String[] split = properties.split("[,=]");
		if ((split.length & 1) == 1) {
			return null;
		}
		for (int i = 0; i < split.length; i += 2) {
			state.set(states
					.getOrDefault(split[i], Collections.emptyMap())
					.getOrDefault(split[i + 1], 0));
		}
		return state;
	}

	public boolean isWaterlogged(BitSet state) {
		return state.get(waterlogged);
	}

	public boolean isWaterlogged(CompoundTag properties) {
		if (properties == null) {
			return false;
		}
		String w = properties.getStringOrDefault("waterlogged", null);
		return w != null && w.equals("true");
	}

	public int size() {
		return size;
	}

	public static class BlockStatesTypeAdapter extends TypeAdapter<BlockStates> {

		@Override
		public void write(JsonWriter out, BlockStates value) throws IOException {
			out.beginObject();
			TreeMap<String, TreeMap<String, Integer>> states = new TreeMap<>();
			value.states.forEach((k, v) -> states.put(k, new TreeMap<>(v)));
			for (Map.Entry<String, TreeMap<String, Integer>> entry : states.entrySet()) {
				out.name(entry.getKey());
				out.beginObject();
				for (Map.Entry<String, Integer> stateEntry : entry.getValue().entrySet()) {
					out.name(stateEntry.getKey());
					out.value(stateEntry.getValue());
				}
				out.endObject();
			}
			out.endObject();
		}

		@Override
		public BlockStates read(JsonReader in) throws IOException {
			Map<String, Map<String, Integer>> states = new HashMap<>();
			in.beginObject();
			while(in.hasNext()) {
				String name = in.nextName();
				Map<String, Integer> map = new HashMap<>();
				states.put(name, map);
				in.beginObject();
				while (in.hasNext()) {
					String value = in.nextName();
					int index = in.nextInt();
					map.put(value, index);
				}
				in.endObject();
			}
			in.endObject();
			return new BlockStates(states);
		}
	}
}