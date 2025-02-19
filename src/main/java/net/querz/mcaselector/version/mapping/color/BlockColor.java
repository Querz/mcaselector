package net.querz.mcaselector.version.mapping.color;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Objects;

public class BlockColor {

	public static final BlockColor MISSING = new BlockColor(0xFFFFFFFF);

	public int color;

	public int properties;

	public static final int TRANSPARENT =      0b00000001;
	public static final int GRASS_TINT =       0b00000010;
	public static final int FOLIAGE_TINT =     0b00000100;
	public static final int WATER =            0b00001000;
	public static final int FOLIAGE =          0b00010000;
	public static final int STATIC_TINT =      0b00100000;
	public static final int STATIC_COLOR =     0b01000000;
	public static final int DRY_FOLIAGE_TINT = 0b10000000;

	public static final int TINTED = GRASS_TINT | FOLIAGE_TINT | WATER | DRY_FOLIAGE_TINT;

	private BlockColor() {}

	public BlockColor(int color) {
		this(color, 0);
	}

	public BlockColor(int color, int properties) {
		this.color = color;
		this.properties = properties;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BlockColor c
				&& color == c.color
				&& properties == c.properties;
	}

	@Override
	public int hashCode() {
		return Objects.hash(color, properties);
	}

	@Override
	public String toString() {
		return String.format("{%06x, %s, %s, %s, %s, %s, %s, %s, %s}", color,
				properties & TRANSPARENT,
				properties & GRASS_TINT,
				properties & FOLIAGE_TINT,
				properties & WATER,
				properties & FOLIAGE,
				properties & STATIC_TINT,
				properties & STATIC_COLOR,
				properties & DRY_FOLIAGE_TINT);
	}

	public static class BlockColorAdapter extends TypeAdapter<BlockColor> {

		@Override
		public void write(JsonWriter out, BlockColor value) throws IOException {
			if (value.properties != 0) {
				out.beginObject();
				out.name("color");
				out.value(String.format("%06x", value.color));
				out.name("properties");
				out.value(value.properties);
				out.endObject();
			} else {
				out.value(String.format("%06x", value.color));
			}
		}

		@Override
		public BlockColor read(JsonReader in) throws IOException {
			BlockColor blockColor = new BlockColor();
			if (in.peek() == JsonToken.BEGIN_OBJECT) {
				in.beginObject();
				while (in.hasNext()) {
					switch (in.nextName()) {
						case "color":
							blockColor.color = Integer.parseInt(in.nextString(), 16);
							break;
						case "properties":
							blockColor.properties = in.nextInt();
							break;
					}
				}
				in.endObject();
			} else {
				blockColor.color = Integer.parseInt(in.nextString(), 16);
			}
			return blockColor;
		}
	}


}