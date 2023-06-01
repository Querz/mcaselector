package net.querz.mcaselector.config.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.querz.mcaselector.io.WorldDirectories;

import java.io.IOException;

public class WorldDirectoriesAdapter extends TypeAdapter<WorldDirectories> {

	@Override
	public void write(JsonWriter out, WorldDirectories value) throws IOException {
		out.value(value == null ? null : value.toString());
	}

	@Override
	public WorldDirectories read(JsonReader in) throws IOException {
		return null;
	}
}
