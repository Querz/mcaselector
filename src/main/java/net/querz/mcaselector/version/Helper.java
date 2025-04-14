package net.querz.mcaselector.version;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.nbt.*;
import java.util.Random;

public final class Helper {

	private static final Range maxRange = new Range(-127, 126);

	private static final Random random = new Random();

	private Helper() {}

	public static CompoundTag levelFromRoot(Tag root) {
		return tagFromCompound(root, "Level", null);
	}

	public static <T extends Tag> T tagFromLevelFromRoot(Tag root, String key) {
		return tagFromLevelFromRoot(root, key, null);
	}

	public static <T extends Tag> T tagFromLevelFromRoot(Tag root, String key, T def) {
		CompoundTag level = levelFromRoot(root);
		return tagFromCompound(level, key, def);
	}

	public static <T extends Tag> T tagFromCompound(Tag compound, String key) {
		return tagFromCompound(compound, key, null);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Tag> T tagFromCompound(Tag compound, String key, T def) {
		CompoundTag c;
		if (!(compound instanceof CompoundTag) || !(c = (CompoundTag) compound).containsKey(key)) {
			return def;
		}
		Tag tag = c.get(key);
		return (T) tag;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Tag> T getSectionsFromCompound(Tag compound, String name) {
		Tag section = tagFromCompound(compound, name, null);
		if (section == null || section.getType() == Tag.Type.LONG_ARRAY) {
			return null;
		}
		return (T) section;
	}

	public static <T extends Tag> T getSectionsFromLevelFromRoot(Tag root, String name) {
		return getSectionsFromCompound(levelFromRoot(root), name);
	}

	public static Point2i point2iFromCompound(Tag compound, String xKey, String zKey) {
		IntTag x = tagFromCompound(compound, xKey, null);
		IntTag z = tagFromCompound(compound, zKey, null);
		if (x == null || z == null) {
			return null;
		}
		return new Point2i(x.asInt(), z.asInt());
	}

	public static Number numberFromCompound(Tag compound, String key, Number def) {
		NumberTag t = tagFromCompound(compound, key, null);
		if (t == null) {
			return def;
		}
		return ((CompoundTag) compound).getInt(key);
	}

	public static Byte byteFromCompound(Tag compound, String key) {
		ByteTag tag = tagFromCompound(compound, key, null);
		if (tag != null) {
			return tag.asByte();
		}
		return null;
	}

	public static Short shortFromCompound(Tag compound, String key) {
		ShortTag tag = tagFromCompound(compound, key, null);
		if (tag != null) {
			return tag.asShort();
		}
		return null;
	}

	public static Integer intFromCompound(Tag compound, String key) {
		IntTag tag = tagFromCompound(compound, key, null);
		if (tag != null) {
			return tag.asInt();
		}
		return null;
	}

	public static int intFromCompound(Tag compound, String key, int def) {
		IntTag tag = tagFromCompound(compound, key, null);
		if (tag != null) {
			return tag.asInt();
		}
		return def;
	}

	public static Long longFromCompound(Tag compound, String key) {
		LongTag tag = tagFromCompound(compound, key, null);
		if (tag != null) {
			return tag.asLong();
		}
		return null;
	}

	public static Double doubleFromCompound(Tag compound, String key) {
		DoubleTag tag = tagFromCompound(compound, key, null);
		if (tag != null) {
			return tag.asDouble();
		}
		return null;
	}

	public static Float floatFromCompound(Tag compound, String key) {
		FloatTag tag = tagFromCompound(compound, key, null);
		if (tag != null) {
			return tag.asFloat();
		}
		return null;
	}

	public static byte[] byteArrayFromCompound(Tag compound, String key) {
		ByteArrayTag tag = tagFromCompound(compound, key, null);
		if (tag != null) {
			return tag.getValue();
		}
		return null;
	}

	public static int[] intArrayFromCompound(Tag compound, String key) {
		IntArrayTag tag = tagFromCompound(compound, key, null);
		if (tag != null) {
			return tag.getValue();
		}
		return null;
	}

	public static long[] longArrayFromCompound(Tag compound, String key) {
		LongArrayTag tag = tagFromCompound(compound, key, null);
		if (tag != null) {
			return tag.getValue();
		}
		return null;
	}

	public static String stringFromCompound(Tag compound, String key) {
		StringTag tag = tagFromCompound(compound, key, null);
		if (tag != null) {
			return tag.getValue();
		}
		return null;
	}

	public static String stringFromCompound(Tag compound, String key, String def) {
		StringTag tag = tagFromCompound(compound, key, null);
		if (tag != null) {
			return tag.getValue();
		}
		return def;
	}

	public static void applyShortOffsetIfRootPresent(CompoundTag root, String xKey, String yKey, String zKey, Point3i offset) {
		if (root != null) {
			applyShortIfPresent(root, xKey, offset.getX());
			applyShortIfPresent(root, yKey, offset.getY());
			applyShortIfPresent(root, zKey, offset.getZ());
		}
	}

	public static void applyShortIfPresent(CompoundTag root, String key, int offset) {
		Short value;
		if ((value = shortFromCompound(root, key)) != null) {
			root.putShort(key, (short) (value + offset));
		}
	}

	public static void applyIntOffsetIfRootPresent(CompoundTag root, String xKey, String yKey, String zKey, Point3i offset) {
		if (root != null) {
			applyIntIfPresent(root, xKey, offset.getX());
			applyIntIfPresent(root, yKey, offset.getY());
			applyIntIfPresent(root, zKey, offset.getZ());
		}
	}

	public static void applyIntIfPresent(CompoundTag root, String key, int offset) {
		Integer value;
		if ((value = intFromCompound(root, key)) != null) {
			root.putInt(key, value + offset);
		}
	}

	public static void applyOffsetToIntListPos(ListTag pos, Point3i offset) {
		if (pos != null && pos.size() == 3) {
			pos.set(0, IntTag.valueOf(pos.getInt(0) + offset.getX()));
			pos.set(1, IntTag.valueOf(pos.getInt(1) + offset.getY()));
			pos.set(2, IntTag.valueOf(pos.getInt(2) + offset.getZ()));
		}
	}

	public static void applyOffsetToIntArrayPos(int[] pos, Point3i offset) {
		if (pos != null && pos.length == 3) {
			pos[0] += offset.getX();
			pos[1] += offset.getY();
			pos[2] += offset.getZ();
		}
	}

	public static void applyOffsetToIntArrayPos(IntArrayTag pos, Point3i offset) {
		if (pos != null && pos.getValue().length == 3) {
			pos.getValue()[0] += offset.getX();
			pos.getValue()[1] += offset.getY();
			pos.getValue()[2] += offset.getZ();
		}
	}

	public static void applyOffsetToBB(int[] bb, Point3i offset) {
		if (bb == null || bb.length != 6) {
			return;
		}
		bb[0] += offset.getX();
		bb[1] += offset.getY();
		bb[2] += offset.getZ();
		bb[3] += offset.getX();
		bb[4] += offset.getY();
		bb[5] += offset.getZ();
	}

	public static void fixEntityUUID(CompoundTag entity) {
		if (entity.containsKey("UUIDMost")) {
			entity.putLong("UUIDMost", random.nextLong());
		}
		if (entity.containsKey("UUIDLeast")) {
			entity.putLong("UUIDLeast", random.nextLong());
		}
		if (entity.containsKey("UUID")) {
			int[] uuid = entity.getIntArray("UUID");
			if (uuid.length == 4) {
				for (int i = 0; i < 4; i++) {
					uuid[i] = random.nextInt();
				}
			}
		}
	}

	public static void applyOffsetToListOfShortTagLists(CompoundTag root, String key, Point3i offset) {
		if (offset.getY() == 0 || !root.containsKey(key)) {
			return;
		}

		ListTag list = tagFromCompound(root, key, null);
		if (list != null) {
			ListTag copy = list.copy();
			list.forEach(e -> ((ListTag) e).clear());

			for (int i = 0; i < copy.size(); i++) {
				int y = i + offset.getY();
				if (y >= 0 && y < copy.size()) {
					list.set(y, copy.get(i));
				}
			}
		}
	}

	public static Range findSectionRange(CompoundTag root, ListTag sections) {
		if (sections == null) {
			return null;
		}
		int max = Integer.MIN_VALUE;
		int min = Integer.MAX_VALUE;
		int current;
		for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
			// ignore empty section
			if (section.size() == 1 || !section.containsKey("Y")) {
				continue;
			}
			current = section.getInt("Y");
			if (current > max) {
				max = current;
			}
			if (current < min) {
				min = current;
			}
		}
		if (root.containsNumber("yPos")) {
			min = root.getInt("yPos");
		}
		return new Range(min, max).limit(maxRange);
	}

	public static int findHighestSection(ListTag sections, int lowest) {
		int max = lowest;
		int current;
		for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
			if ((current = section.getInt("Y")) > max) {
				max = current;
			}
		}
		return max;
	}

	public static int getDataVersion(CompoundTag root) {
		if (root == null) {
			return 100;
		}
		if (!root.containsKey("DataVersion") && root.contains("Level", Tag.Type.COMPOUND)) {
			Integer d = intFromCompound(root.getCompound("Level"), "DataVersion");
			return d == null ? 100 : d;
		}
		return root.getIntOrDefault("DataVersion", 100);
	}

	public static IntTag getDataVersionTag(CompoundTag root) {
		if (root == null) {
			return null;
		}
		Tag d;
		if (!root.containsKey("DataVersion") && root.contains("Level", Tag.Type.COMPOUND)) {
			d = tagFromCompound(root.getCompound("Level"), "DataVersion");
		} else {
			d = root.get("DataVersion");
		}
		return d == null || d.getType() != Tag.Type.INT ? null : (IntTag) d;
	}

	public static void setDataVersion(CompoundTag root, int dataVersion) {
		if (root == null) {
			return;
		}
		// DataVersion was added on root level in 15w33a, before it was inside the Level tag
		if (dataVersion < 111) {
			if (root.contains("Level", Tag.Type.COMPOUND)) {
				root.getCompound("Level").putInt("DataVersion", dataVersion);
			}
		}
		root.putInt("DataVersion", dataVersion);
	}

	public static CompoundTag getRegion(ChunkData data) {
		if (data == null || data.region() == null) {
			return null;
		}
		return data.region().getData();
	}

	public static CompoundTag getPOI(ChunkData data) {
		if (data == null || data.poi() == null) {
			return null;
		}
		return data.poi().getData();
	}

	public static CompoundTag getEntities(ChunkData data) {
		if (data == null || data.entities() == null) {
			return null;
		}
		return data.entities().getData();
	}

}
