package net.querz.mcaselector.version.mapping.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Base64;
import java.util.BitSet;
import java.util.stream.IntStream;

// encodes and decodes a BitSet to Base64 for json
public class BitSetAdapter extends TypeAdapter<BitSet> {

	@Override
	public void write(JsonWriter out, BitSet value) throws IOException {
		out.value(new String(Base64.getEncoder().encode(value.toByteArray())));
	}

	@Override
	public BitSet read(JsonReader in) throws IOException {
		return BitSet.valueOf(Base64.getDecoder().decode(in.nextString().getBytes()));
	}

	public static String bitsetToString(BitSet bitset) {
		StringBuilder buffer = new StringBuilder(bitset.size());
		IntStream.range(0, bitset.size()).mapToObj(i -> bitset.get(i) ? '1' : '0').forEach(buffer::append);
		return buffer.toString();
	}

	public static BitSet reverseBitSet(BitSet bitset) {
		BitSet reversedBitSet = new BitSet(bitset.size());
		int j = 0;
		for (int i = bitset.size() - 1; i >= 0; i--) {
			if (bitset.get(i)) {
				reversedBitSet.set(j);
			}
			j++;
		}
		return reversedBitSet;
	}
}