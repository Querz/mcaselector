package net.querz.mcaselector.version;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.range.Range;
import net.querz.nbt.*;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

public final class NbtHelper {

	private static final Random random = new Random();

	private NbtHelper() {}

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
		if (section == null || section.getID() == Tag.LONG_ARRAY) {
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

	public static int findHighestSection(ListTag sections, int lowest) {
		int max = lowest;
		int current;
		for (CompoundTag section : sections.iterateType(CompoundTag.TYPE)) {
			if ((current = section.getInt("Y")) > max) {
				max = current;
			}
		}
		return max;
	}

	public static ListTag mergeLists(ListTag source, ListTag destination, List<Range> ranges, Function<Tag, Integer> ySupplier, int yOffset) {
		ListTag result = new ListTag();
		for (Tag dest : destination) {
			int y = ySupplier.apply(dest);
			for (Range range : ranges) {
				if (!range.contains(y)) {
					result.add(dest);
				}
			}
		}

		for (Tag sourceElement : source) {
			int y = ySupplier.apply(sourceElement);
			for (Range range : ranges) {
				if (range.contains(y - yOffset)) {
					result.add(sourceElement);
					break;
				}
			}
		}

		return result;
	}

	public static void mergeListTagLists(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name) {
		ListTag sourceList = tagFromLevelFromRoot(source, name);
		ListTag destinationList = tagFromLevelFromRoot(destination, name, sourceList);

		if (sourceList == null || destinationList == null || sourceList.size() != destinationList.size()) {
			return;
		}

		for (Range range : ranges) {
			int m = Math.min(range.getTo() + yOffset, sourceList.size() - 1);
			for (int i = Math.max(range.getFrom() + yOffset, 0); i <= m; i++) {
				destinationList.set(i, sourceList.get(i));
			}
		}

		initLevel(destination).put(name, destinationList);
	}

	public static void mergeCompoundTagListsFromLevel(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name, Function<Tag, Integer> ySupplier) {
		ListTag sourceElements = tagFromLevelFromRoot(source, name, new ListTag());
		ListTag destinationElements = tagFromLevelFromRoot(destination, name, new ListTag());

		initLevel(destination).put(name, mergeLists(sourceElements, destinationElements, ranges, ySupplier, yOffset));
	}

	public static void mergeCompoundTagLists(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name, Function<Tag, Integer> ySupplier) {
		ListTag sourceElements = tagFromCompound(source, name, new ListTag());
		ListTag destinationElements = tagFromCompound(destination, name, new ListTag());

		destination.put(name, mergeLists(sourceElements, destinationElements, ranges, ySupplier, yOffset));
	}

	// merge based on compound tag keys, assuming compound tag keys are ints
	public static void mergeCompoundTags(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name) {
		CompoundTag sourceElements = tagFromCompound(source, name, new CompoundTag());
		CompoundTag destinationElements = tagFromCompound(destination, name, new CompoundTag());

		for (Map.Entry<String, Tag> sourceElement : sourceElements) {
			if (sourceElement.getKey().matches("^-?[0-9]{1,2}$")) {
				int y = Integer.parseInt(sourceElement.getKey());
				for (Range range : ranges) {
					if (range.contains(y - yOffset)) {
						destinationElements.put(sourceElement.getKey(), sourceElement.getValue());
						break;
					}
				}
			}
		}
	}

	public static CompoundTag initLevel(CompoundTag c) {
		CompoundTag level = levelFromRoot(c);
		if (level == null) {
			c.put("Level", level = new CompoundTag());
		}
		return level;
	}

	// XXX what's going on with the colliding fixEntityUUID* methods? What do they actually do? How can they be named better?
	public static void fixEntityUUIDsMerger(CompoundTag root) {
		// MAINTAINER shouldn't this be VersionController'ed?
		ListTag entities = tagFromCompound(root, "Entities", null);
		if (entities != null) {
			entities.forEach(e -> fixEntityUUIDMerger((CompoundTag) e));
		}
	}

	private static void fixEntityUUIDMerger(CompoundTag entity) {
		fixEntityUUID(entity);
		if (entity.containsKey("Passengers")) {
			ListTag passengers = tagFromCompound(entity, "Passengers", null);
			if (passengers != null) {
				passengers.forEach(e -> fixEntityUUIDMerger((CompoundTag) e));
			}
		}
	}

}
