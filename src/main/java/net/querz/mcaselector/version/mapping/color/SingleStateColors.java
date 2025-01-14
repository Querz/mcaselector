package net.querz.mcaselector.version.mapping.color;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.BitSet;

public class SingleStateColors extends BlockColor implements StateColors {

	public SingleStateColors(BlockColor color) {
		super(color.color, color.properties);
	}

	@Override
	public BlockColor getColor(BitSet state) {
		return this;
	}

	@Override
	public boolean hasColor(BitSet state) {
		return true;
	}

	@Override
	public void setColor(BitSet state, BlockColor color) {
		this.color = color.color;
		this.properties = color.properties;
	}

	public static class SingleStateColorsAdapter extends TypeAdapter<SingleStateColors> {

		private static final BlockColorAdapter blockColorAdapter = new BlockColorAdapter();

		@Override
		public void write(JsonWriter out, SingleStateColors value) throws IOException {
			blockColorAdapter.write(out, value);
		}

		@Override
		public SingleStateColors read(JsonReader in) throws IOException {
			return new SingleStateColors(blockColorAdapter.read(in));
		}
	}
}
