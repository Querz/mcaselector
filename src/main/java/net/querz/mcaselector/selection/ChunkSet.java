package net.querz.mcaselector.selection;

import it.unimi.dsi.fastutil.ints.IntConsumer;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.shorts.ShortPredicate;
import net.querz.mcaselector.point.Point2i;
import java.io.Serializable;

public class ChunkSet implements IntIterable, Serializable, Cloneable {

	long[] words = new long[16];
	short setBits = 0;

	public static final ChunkSet EMPTY_SET = new ChunkSet().immutable();

	public void set(int index) {
		if (!get(index)) {
			setBits++;
		}
		words[index >> 6] |= (1L << index);
	}

	public void clear(int index) {
		if (get(index)) {
			setBits--;
		}
		words[index >> 6] &= ~(1L << index);
	}

	public void clear() {
		for (int i = 0; i < 16; i++) {
			words[i] = 0L;
		}
		setBits = 0;
	}

	public boolean get(int index) {
		return (words[index >> 6] & (1L << index)) != 0;
	}

	public void or(ChunkSet other) {
		for (short i = 0; i < 1024; i++) {
			if (other.get(i)) {
				set(i);
			}
		}
	}

	// turns all chunks to be selected if the chunk in this ChunkSet is selected and the one in the other ChunkSet is not.
	public void otherNotAnd(ChunkSet other) {
		for (short i = 0; i < 1024; i++) {
			if (!get(i) || other.get(i)) {
				clear(i);
			}
		}
	}

	public ChunkSet flip() {
		ChunkSet result = new ChunkSet();
		for (int i = 0; i < 16; i++) {
			result.words[i] = ~words[i];
		}
		result.setBits = (short) (1024 - setBits);
		return result;
	}

	@Override
	public ChunkSet clone() {
		ChunkSet clone;
		try {
			clone = (ChunkSet) super.clone();
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
		clone.words = words.clone();
		clone.setBits = setBits;
		return clone;
	}

	public short size() {
		return setBits;
	}

	public boolean isEmpty() {
		return setBits == 0;
	}

	public void fill() {
		for (int i = 0; i < 16; i++) {
			words[i] = 0xFFFFFFFFFFFFFFFFL;
		}
		setBits = 1024;
	}

	@Override
	public IntIterator iterator() {
		return new ChunkIterator();
	}

	@Override
	public void forEach(IntConsumer action) {
		for (int i = 0; i < 16; i++) {
			if (words[i] == 0L) {
				continue;
			}
			int c = i << 6;
			int l = c + 64;
			for (; c < l; c++) {
				if (get(c)) {
					action.accept(c);
				}
			}
		}
	}

	public void forEachFrom(short index, IntConsumer action) {
		for (int i = index >> 6; i < 16; i++) {
			if (words[i] == 0) {
				continue;
			}
			int c = i << 6;
			int l = c + 64;
			for (; c < l; c++) {
				if (get(c)) {
					action.accept(c);
				}
			}
		}
	}

	public void forEachFromInverse(short index, IntConsumer action) {
		for (int i = index >> 6; i >= 0; i--) {
			if (words[i] == 0) {
				continue;
			}
			int l = i << 6;
			int c = l + 64;
			for (; c >= l; c--) {
				if (get(c)) {
					action.accept(c);
				}
			}
		}
	}

	public int getMinX(int max) {
		for (int x = 0; x < max; x++) {
			for (int z = 0; z < 32; z++) {
				if (get(z << 5 | x)) {
					return x;
				}
			}
		}
		return max;
	}

	public int getMaxX(int min) {
		for (int x = 31; x > min; x--) {
			for (int z = 31; z >= 0; z--) {
				if (get(z << 5 | x)) {
					return x;
				}
			}
		}
		return min;
	}

	public int getMinZ(int max) {
		int l = max >> 1;
		for (int i = 0; i <= l; i++) {
			if (words[i] == 0) {
				continue;
			}
			if ((words[i] & 0xFFFFFFFFL) == 0) {
				return i * 2 + 1;
			}
			return i * 2;
		}
		return max;
	}

	public int getMaxZ(int min) {
		int l = min >> 1;
		for (int i = 15; i >= l; i--) {
			if (words[i] == 0) {
				continue;
			}
			if ((words[i] & 0xFFFFFFFF00000000L) == 0) {
				return i * 2;
			}
			return i * 2 + 1;
		}
		return min;
	}

	public void removeIf(ShortPredicate predicate) {
		short c = 0, l = 64;
		for (int i = 0; i < 16; i++) {
			if (words[i] == 0) {
				continue;
			}
			for (; c < l; c++) {
				if (predicate.test(c)) {
					clear(c);
				}
			}
			l += 64;
		}
	}

	private class ChunkIterator implements IntIterator {

		short index = 0;

		@Override
		public int nextInt() {
			return index - 1;
		}

		@Override
		public boolean hasNext() {
			while (index < 1024) {
				if (get(index++)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void forEachRemaining(IntConsumer action) {
			for (; index < 1024; index++) {
				if (get(index)) {
					action.accept(index);
				}
			}
		}
	}

	public ChunkSet immutable() {
		return new ImmutableChunkSet(this);
	}

	private static class ImmutableChunkSet extends ChunkSet {

		ImmutableChunkSet(ChunkSet chunkSet) {
			words = chunkSet.words.clone();
			setBits = chunkSet.setBits;
		}

		@Override
		public void set(int index) {
			throw new UnsupportedOperationException("cannot modify immutable ChunkSet");
		}

		@Override
		public void clear(int index) {
			throw new UnsupportedOperationException("cannot modify immutable ChunkSet");
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("cannot modify immutable ChunkSet");
		}

		@Override
		public void removeIf(ShortPredicate predicate) {
			throw new UnsupportedOperationException("cannot modify immutable ChunkSet");
		}

		@Override
		public void or(ChunkSet other) {
			throw new UnsupportedOperationException("cannot modify immutable ChunkSet");
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("   0  1  2  3  4  5  6  7  8  9  10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31\n");
		for (int z = 0; z < 32; z++) {
			sb.append(z);
			sb.append(z > 9 ? "" : " ");
			for (int x = 0; x < 32; x++) {
				sb.append(" ");
				sb.append(get(new Point2i(x, z).asChunkIndex()) ? 1 : 0);
				sb.append(" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}