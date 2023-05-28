package net.querz.mcaselector.config.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.querz.mcaselector.ui.Color;
import java.io.IOException;

public class ColorAdapter extends TypeAdapter<Color> {

	@Override
	public void write(JsonWriter out, Color value) throws IOException {
		out.value(value == null ? null : value.toString());
	}

	@Override
	public Color read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			return Color.BLACK;
		}
		return new Color(in.nextString());
	}
}