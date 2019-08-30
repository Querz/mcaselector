package net.querz.mcaselector.io;

import javafx.scene.image.PixelWriter;
import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.DoubleTag;
import net.querz.nbt.IntArrayTag;
import net.querz.nbt.IntTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.LongArrayTag;
import net.querz.nbt.Tag;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public class MCAChunkData {

	private long offset; //in actual bytes
	private int timestamp;
	private byte sectors;
	private int length; //length without padding
	private CompressionType compressionType;
	private CompoundTag data;

	//offset in 4KiB chunks
	public MCAChunkData(int offset, int timestamp, byte sectors) {
		this.offset = ((long) offset) * MCAFile.SECTION_SIZE;
		this.timestamp = timestamp;
		this.sectors = sectors;
	}

	public boolean isEmpty() {
		return offset == 0 && timestamp == 0 && sectors == 0;
	}

	public void readHeader(ByteArrayPointer ptr) throws Exception {
		ptr.seek(offset);
		length = ptr.readInt();
		compressionType = CompressionType.fromByte(ptr.readByte());
	}

	public void loadData(ByteArrayPointer ptr) throws Exception {
		//offset + length of length (4 bytes) + length of compression type (1 byte)
		ptr.seek(offset + 5);
		DataInputStream nbtIn = null;

		switch (compressionType) {
		case GZIP:
			nbtIn = new DataInputStream(new BufferedInputStream(new GZIPInputStream(ptr)));
			break;
		case ZLIB:
			nbtIn = new DataInputStream(new BufferedInputStream(new InflaterInputStream(ptr)));
			break;
		case NONE:
			data = null;
			return;
		}
		Tag tag = Tag.deserialize(nbtIn, Tag.DEFAULT_MAX_DEPTH);

		if (tag instanceof CompoundTag) {
			data = (CompoundTag) tag;
		} else {
			throw new Exception("Invalid chunk data: tag is not of type CompoundTag");
		}
	}

	//saves to offset provided by raf, because it might be different when data changed
	//returns the number of bytes that were written to the file
	public int saveData(RandomAccessFile raf) throws Exception {
		DataOutputStream nbtOut;

		ByteArrayOutputStream baos;

		switch (compressionType) {
		case GZIP:
			nbtOut = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(baos = new ByteArrayOutputStream()), sectors * MCAFile.SECTION_SIZE));
			break;
		case ZLIB:
			nbtOut = new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(baos = new ByteArrayOutputStream()), sectors * MCAFile.SECTION_SIZE));
			break;
		default:
			return 0;
		}

		data.serialize(nbtOut, Tag.DEFAULT_MAX_DEPTH);
		nbtOut.close();

		byte[] rawData = baos.toByteArray();

		raf.writeInt(rawData.length);
		raf.writeByte(compressionType.getByte());
		raf.write(rawData);

		return rawData.length + 5;
	}

	public void changeData(List<Field<?>> fields, boolean force) {
		for (Field field : fields) {
			try {
				if (force) {
					field.force(data);
				} else {
					field.change(data);
				}
			} catch (Exception ex) {
				Debug.dumpf("error trying to update field: %s", ex.getMessage());
			}
		}
	}

	public void drawImage(int x, int z, PixelWriter writer) {
		if (data == null) {
			return;
		}
		int dataVersion = data.getInt("DataVersion");
		try {
			VersionController.getChunkDataProcessor(dataVersion).drawChunk(
					data,
					VersionController.getColorMapping(dataVersion),
					x, z,
					writer
			);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public long getOffset() {
		return offset;
	}

	void setOffset(int sectorOffset) {
		this.offset = ((long) sectorOffset) * MCAFile.SECTION_SIZE;
	}

	public int getLength() {
		return length;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public byte getSectors() {
		return sectors;
	}

	public CompressionType getCompressionType() {
		return compressionType;
	}

	public CompoundTag getData() {
		return data;
	}

	public Point2i getLocation() {
		if (data == null || !data.containsKey("Level") || !data.getCompoundTag("Level").containsKey("xPos") || !data.getCompoundTag("Level").containsKey("zPos")) {
			return null;
		}
		return new Point2i(data.getCompoundTag("Level").getInt("xPos"), data.getCompoundTag("Level").getInt("zPos"));
	}

	public boolean setLocation(Point2i location) {
		if (data == null || !data.containsKey("Level")) {
			return false;
		}

		data.getCompoundTag("Level").putInt("xPos", location.getX());
		data.getCompoundTag("Level").putInt("zPos", location.getY());
		return true;
	}

	// offset is in blocks
	public boolean applyOffset(Point2i offset) {
		if (data == null || !data.containsKey("Level")) {
			return false;
		}

		CompoundTag level = data.getCompoundTag("Level");

		// adjust chunk position
		level.putInt("xPos", level.getInt("xPos") + offset.blockToChunk().getX());
		level.putInt("zPos", level.getInt("zPos") + offset.blockToChunk().getY());

		// adjust entity positions

		ListTag<CompoundTag> entities = level.getListTag("Entities").asCompoundTagList();
		entities.forEach(v -> applyOffsetToEntity(v, offset));

		// adjust tile entity positions

		ListTag<CompoundTag> tileEntities = level.getListTag("TileEntities").asCompoundTagList();
		tileEntities.forEach(v -> applyOffsetToTileEntity(v, offset));

		// adjust tile ticks

		ListTag<CompoundTag> tileTicks = level.getListTag("TileTicks").asCompoundTagList();
		tileTicks.forEach(v -> applyOffsetToTileTick(v, offset));

		// adjust structures

		CompoundTag structures = level.getCompoundTag("Structures");
		applyOffsetToStructures(structures, offset);

		return true;
	}

	private void applyOffsetToStructures(CompoundTag structures, Point2i offset) {
		Point2i chunkOffset = offset.blockToChunk();

		// update references

		CompoundTag references = structures.getCompoundTag("References");
		for (Map.Entry<String, Tag<?>> entry : references) {
			long[] reference = ((LongArrayTag) entry.getValue()).getValue();
			for (int i = 0; i < reference.length; i++) {
				int x = (int) (reference[i]);
				int z = (int) (reference[i] >> 32);
				reference[i] = (long) (x + chunkOffset.getX()) + ((long) (z + chunkOffset.getY()) << 32);
			}
		}

		// update starts

		CompoundTag starts = structures.getCompoundTag("Starts");
		for (Map.Entry<String, Tag<?>> entry : starts) {
			CompoundTag structure = (CompoundTag) entry.getValue();
			if (structure.getString("id").equals("INVALID")) {
				continue;
			}
			structure.putInt("ChunkX", structure.getInt("ChunkX") + chunkOffset.getX());
			structure.putInt("ChunkZ", structure.getInt("ChunkZ") + chunkOffset.getY());

			applyOffsetToBB(structure.getIntArray("BB"), offset);

			ListTag<CompoundTag> processed = structure.getListTag("Processed").asCompoundTagList();
			for (CompoundTag chunk : processed) {
				chunk.putInt("X", chunk.getInt("X") + chunkOffset.getX());
				chunk.putInt("Z", chunk.getInt("Z") + chunkOffset.getY());
			}

			ListTag<CompoundTag> children = structure.getListTag("Children").asCompoundTagList();
			for (CompoundTag child : children) {
				applyIntIfPresent(child, "TPX", offset.getX());
				applyIntIfPresent(child, "TPZ", offset.getY());
				applyIntIfPresent(child, "PosX", offset.getX());
				applyIntIfPresent(child, "PosZ", offset.getY());
				applyOffsetToBB(child.getIntArray("BB"), offset);

				if (child.containsKey("Entrances")) {
					for (IntArrayTag entrance : child.getListTag("Entrances").asIntArrayTagList()) {
						applyOffsetToBB(entrance.getValue(), offset);
					}
				}

				if (child.containsKey("junctions")) {
					for (CompoundTag junction : child.getListTag("junctions").asCompoundTagList()) {
						junction.putInt("source_x", junction.getInt("source_x") + offset.getX());
						junction.putInt("source_z", junction.getInt("source_z") + offset.getY());
					}
				}
			}
		}
	}

	private void applyOffsetToBB(int[] bb, Point2i offset) {
		bb[0] += offset.getX();
		bb[2] += offset.getY();
		bb[3] += offset.getX();
		bb[5] += offset.getY();
	}

	private void applyOffsetToTileTick(CompoundTag tileTick, Point2i offset) {
		tileTick.putInt("x", tileTick.getInt("x") + offset.getX());
		tileTick.putInt("z", tileTick.getInt("z") + offset.getY());
	}

	private void applyOffsetToTileEntity(CompoundTag tileEntity, Point2i offset) {
		tileEntity.putInt("x", tileEntity.getInt("x") + offset.getX());
		tileEntity.putInt("z", tileEntity.getInt("z") + offset.getY());

		switch (tileEntity.getString("id")) {
			case "beehive":
				CompoundTag flowerPos = tileEntity.getCompoundTag("FlowerPos");
				flowerPos.putInt("X", flowerPos.getInt("X") + offset.getX());
				flowerPos.putInt("Z", flowerPos.getInt("Z") + offset.getY());
				break;
			case "end_gateway":
				CompoundTag exitPortal = tileEntity.getCompoundTag("ExitPortal");
				exitPortal.putInt("X", exitPortal.getInt("X") + offset.getX());
				exitPortal.putInt("Z", exitPortal.getInt("Z") + offset.getY());
				break;

		}
	}

	private void applyOffsetToEntity(CompoundTag entity, Point2i offset) {
		ListTag<DoubleTag> entityPos = entity.getListTag("Pos").asDoubleTagList();
		entityPos.set(0, new DoubleTag(entityPos.get(0).asDouble() + offset.getX()));
		entityPos.set(2, new DoubleTag(entityPos.get(2).asDouble() + offset.getY()));

		// leashed entities

		if (entity.containsKey("Leash")) {
			CompoundTag leash = entity.getCompoundTag("Leash");
			if (leash.containsKey("X")) {
				leash.putInt("X", leash.getInt("X") + offset.getX());
				leash.putInt("Z", leash.getInt("Z") + offset.getY());
			}
		}

		// projectiles

		applyIntIfPresent(entity, "xTile", offset.getX());
		applyIntIfPresent(entity, "zTile", offset.getY());

		// entities that have a sleeping place

		applyIntIfPresent(entity, "SleepingX", offset.getX());
		applyIntIfPresent(entity, "SleepingZ", offset.getY());

		// positions for specific entity types

		switch (entity.getString("id")) {
			case "dolphin":
				if (entity.getBoolean("CanFindTreasure")) {
					entity.putInt("TreasurePosX", entity.getInt("TreasurePosX") + offset.getX());
					entity.putInt("TreasurePosZ", entity.getInt("TreasurePosZ") + offset.getY());
				}
				break;
			case "phantom":
				applyIntIfPresent(entity, "AX", offset.getX());
				applyIntIfPresent(entity, "AZ", offset.getY());
				break;
			case "shulker":
				applyIntIfPresent(entity, "APX", offset.getX());
				applyIntIfPresent(entity, "APZ", offset.getY());
				break;
			case "turtle":
				applyIntIfPresent(entity, "HomePosX", offset.getX());
				applyIntIfPresent(entity, "HomePosZ", offset.getY());
				applyIntIfPresent(entity, "TravelPosX", offset.getX());
				applyIntIfPresent(entity, "TravelPosZ", offset.getY());
				break;
			case "vex":
				applyIntIfPresent(entity, "BoundX", offset.getX());
				applyIntIfPresent(entity, "BoundZ", offset.getY());
				break;
			case "wandering_trader":
				if (entity.containsKey("WanderTarget")) {
					CompoundTag wanderTarget = entity.getCompoundTag("WanderTarget");
					wanderTarget.putInt("X", wanderTarget.getInt("X") + offset.getX());
					wanderTarget.putInt("Z", wanderTarget.getInt("Z") + offset.getY());
				}
				break;
			case "shulker_bullet":
				CompoundTag owner = entity.getCompoundTag("Owner");
				owner.putInt("X", owner.getInt("X") + offset.getX());
				owner.putInt("Z", owner.getInt("Z") + offset.getY());
				CompoundTag target = entity.getCompoundTag("Target");
				target.putInt("X", target.getInt("X") + offset.getX());
				target.putInt("Z", target.getInt("Z") + offset.getY());
				break;
			case "end_crystal":
				CompoundTag beamTarget = entity.getCompoundTag("BeamTarget");
				beamTarget.putInt("X", beamTarget.getInt("X") + offset.getX());
				beamTarget.putInt("Z", beamTarget.getInt("Z") + offset.getY());
				break;
			case "item_frame":
			case "painting":
				applyIntIfPresent(entity, "TileX", offset.getX());
				applyIntIfPresent(entity, "TileZ", offset.getY());
				break;
			case "villager":
				if (entity.containsKey("Brain") && entity.getCompoundTag("Brain").containsKey("memories")) {
					CompoundTag memories = entity.getCompoundTag("Brain").getCompoundTag("memories");
					if (memories.size() > 0) {
						if (memories.containsKey("minecraft:meeting_point")) {
							CompoundTag meetingPoint = memories.getCompoundTag("minecraft:meeting_point");
							applyOffsetToIntListPos(meetingPoint.getListTag("pos").asIntTagList(), offset);
						}
						if (memories.containsKey("minecraft:home")) {
							CompoundTag home = memories.getCompoundTag("minecraft:home");
							applyOffsetToIntListPos(home.getListTag("pos").asIntTagList(), offset);
						}
						if (memories.containsKey("minecraft:job_site")) {
							CompoundTag jobSite = memories.getCompoundTag("minecraft:job_site");
							applyOffsetToIntListPos(jobSite.getListTag("pos").asIntTagList(), offset);
						}
					}
				}
				break;
			case "pillager":
			case "witch":
			case "vindicator":
			case "ravager":
			case "illusioner":
			case "evoker":
				if (entity.containsKey("PatrolTarget")) {
					CompoundTag patrolTarget = entity.getCompoundTag("PatrolTarget");
					patrolTarget.putInt("X", patrolTarget.getInt("X") + offset.getX());
					patrolTarget.putInt("X", patrolTarget.getInt("X") + offset.getY());
				}
				break;
		}

		// recursively update passengers

		if (entity.containsKey("Passenger")) {
			applyOffsetToEntity(entity.getCompoundTag("Passenger"), offset);
		}
	}

	private void applyIntIfPresent(CompoundTag root, String key, int offset) {
		if (root.containsKey(key)) {
			root.putInt(key, root.getInt(key) + offset);
		}
	}

	private void applyOffsetToIntListPos(ListTag<IntTag> pos, Point2i offset) {
		pos.set(0, new IntTag(pos.get(0).asInt() + offset.getX()));
		pos.set(2, new IntTag(pos.get(2).asInt() + offset.getY()));
	}
}
