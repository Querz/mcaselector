package net.querz.mcaselector.config.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Locale;

public class LocaleAdapter extends TypeAdapter<Locale> {

	@Override
	public void write(JsonWriter out, Locale value) throws IOException {
		out.value(value == null ? null : value.toString());
	}

	@Override
	public Locale read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			return null;
		}
		String[] split = in.nextString().split("_");
		return Locale.of(split[0], split[1]);
	}
}
