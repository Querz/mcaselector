package net.querz.mcaselector.version.mapping.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

public class HexColorAdapter extends TypeAdapter<Integer> {

	@Override
	public void write(JsonWriter out, Integer value) throws IOException {
		out.value(String.format("%06X", value));
	}

	@Override
	public Integer read(JsonReader in) throws IOException {
		return Integer.parseInt(in.nextString(), 16);
	}
}
