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
		return parse(in.nextString());
	}

	public static Locale parse(String localeString) {
		if (localeString == null) {
			return null;
		}
		String[] split = localeString.split("_");
		if (split.length != 2) {
			return null;
		}
		return Locale.of(split[0], split[1]);
	}
}
