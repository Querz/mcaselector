package net.querz.mcaselector.config.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.IOException;

public class FileAdapter extends TypeAdapter<File> {

	private final String userDir;

	public FileAdapter(String userDir) {
		this.userDir = userDir;
	}

	@Override
	public void write(JsonWriter out, File value) throws IOException {
		if (value == null) {
			out.value((String) null);
			return;
		}
		String absolutePath = value.getAbsolutePath();
		if (absolutePath.startsWith(userDir)) {
			absolutePath = absolutePath.replace(userDir, "{user.dir}");
		}
		out.value(absolutePath);
	}

	@Override
	public File read(JsonReader in) throws IOException {
		return new File(in.nextString().replace("{user.dir}", userDir));
	}
}