package net.querz.mcaselector.version.mapping.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

public class HexColorAdapter extends TypeAdapter<Integer> {

	@Override
	public void write(JsonWriter out, Integer value) throws IOException {
		out.value(String.format("%06X", value));
	}

	@Override
	public Integer read(JsonReader in) throws IOException {
		switch (in.peek()) {
			case JsonToken.NUMBER -> {
				return in.nextInt();
			}
			case JsonToken.STRING -> {
				String s = in.nextString();
				if (s.startsWith("#")) {
					return Integer.parseInt(s.substring(1), 16);
				}
				return Integer.parseInt(s, 16);
			}
		}
		throw new IOException("invalid color format");
	}
}
