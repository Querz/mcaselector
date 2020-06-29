package net.querz.mcaselector.io;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.DoubleTag;
import net.querz.nbt.tag.IntArrayTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import static net.querz.mcaselector.validation.ValidationHelper.*;

public class MCAChunkData {

	private final long offset; //in actual bytes
	private final int timestamp;
	private final byte sectors;
	private int length; //length without padding
	private CompressionType compressionType;
	private CompoundTag data;

	private final Point2i absoluteLocation;

	@Override
	public String toString() {
		return  "offset=" + offset +
				"\ntimestamp=" + timestamp +
				"\nsectors=" + sectors +
				"\nlength=" + length +
				"\ncompressionType=" + compressionType +
				"\ndata=" + data +
				"\nabsoluteLocation=" + absoluteLocation;
	}

	//offset in 4KiB chunks
	public MCAChunkData(Point2i absoluteLocation, int offset, int timestamp, byte sectors) {
		this.absoluteLocation = absoluteLocation;
		this.offset = ((long) offset) * MCAFile.SECTION_SIZE;
		this.timestamp = timestamp;
		this.sectors = sectors;
	}

	static MCAChunkData newEmptyLevelMCAChunkData(Point2i absoluteLocation, int dataVersion) {
		MCAChunkData mcaChunkData = new MCAChunkData(absoluteLocation, 0, 0, (byte) 1);
		CompoundTag root = new CompoundTag();
		CompoundTag level = new CompoundTag();
		level.putInt("xPos", absoluteLocation.getX());
		level.putInt("zPos", absoluteLocation.getY());
		level.putString("Status", "full");
		root.put("Level", level);
		root.putInt("DataVersion", dataVersion);
		mcaChunkData.data = root;
		mcaChunkData.compressionType = CompressionType.ZLIB;
		return mcaChunkData;
	}

	public boolean isEmpty() {
		return offset == 0 && timestamp == 0 && sectors == 0 || data == null;
	}

	public void readHeader(ByteArrayPointer ptr) {
		ptr.seek(offset);
		length = ptr.readInt();
		compressionType = CompressionType.fromByte(ptr.readByte());
	}

	public void readHeader(RandomAccessFile raf) throws IOException {
		raf.seek(offset);
		length = raf.readInt();
		compressionType = CompressionType.fromByte(raf.readByte());
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
		NamedTag tag = new NBTDeserializer(false).fromStream(nbtIn);

		if (tag.getTag() instanceof CompoundTag) {
			data = (CompoundTag) tag.getTag();
		} else {
			throw new Exception("Invalid chunk data: tag is not of type CompoundTag");
		}
	}

	public void loadData(RandomAccessFile raf) throws IOException {
		raf.seek(offset + 5);
		DataInputStream nbtIn = null;

		switch (compressionType) {
			case GZIP:
				nbtIn = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(raf.getFD()))));
				break;
			case ZLIB:
				nbtIn = new DataInputStream(new BufferedInputStream(new InflaterInputStream(new FileInputStream(raf.getFD()))));
				break;
			case NONE:
				data = null;
				return;
		}

		NamedTag tag = new NBTDeserializer(false).fromStream(nbtIn);

		if (tag.getTag() instanceof CompoundTag) {
			data = (CompoundTag) tag.getTag();
		} else {
			throw new IOException("Invalid chunk data: tag is not of type CompoundTag");
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

		new NBTSerializer(false).toStream(new NamedTag(null, data), nbtOut);

		nbtOut.close();

		byte[] rawData = baos.toByteArray();

		raf.writeInt(rawData.length + 1); // length includes the compression type byte
		raf.writeByte(compressionType.getByte());
		raf.write(rawData);

		return rawData.length + 5;
	}

	public void changeData(List<Field<?>> fields, boolean force) {
		for (Field<?> field : fields) {
			try {
				if (force) {
					field.force(data);
				} else {
					field.change(data);
				}
			} catch (Exception ex) {
				Debug.dumpf("failed to change field %s in chunk %s: %s", field.getType(), absoluteLocation, ex);
			}
		}
	}

	public long getOffset() {
		return offset;
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

	public void setData(CompoundTag data) {
		this.data = data;
	}

	public void setCompressionType(CompressionType compressionType) {
		this.compressionType = compressionType;
	}

	public Point2i getAbsoluteLocation() {
		return absoluteLocation;
	}

	// offset is in blocks
	public boolean relocate(Point2i offset) {
		if (data == null || !data.containsKey("Level")) {
			return false;
		}

		CompoundTag level = catchClassCastException(() -> data.getCompoundTag("Level"));
		if (level == null) {
			return true;
		}

		// adjust or set chunk position
		level.putInt("xPos", level.getInt("xPos") + offset.blockToChunk().getX());
		level.putInt("zPos", level.getInt("zPos") + offset.blockToChunk().getY());

		// adjust entity positions
		if (level.containsKey("Entities") && level.get("Entities").getID() != CompoundTag.ID) {
			ListTag<CompoundTag> entities = catchClassCastException(() -> level.getListTag("Entities").asCompoundTagList());
			if (entities != null) {
				entities.forEach(v -> applyOffsetToEntity(v, offset));
			}
		}

		// adjust tile entity positions
		if (level.containsKey("TileEntities") && level.get("TileEntities").getID() == CompoundTag.ID) {
			ListTag<CompoundTag> tileEntities = catchClassCastException(() -> level.getListTag("TileEntities").asCompoundTagList());
			if (tileEntities != null) {
				tileEntities.forEach(v -> applyOffsetToTileEntity(v, offset));
			}
		}

		// adjust tile ticks
		if (level.containsKey("TileTicks")) {
			ListTag<CompoundTag> tileTicks = catchClassCastException(() -> level.getListTag("TileTicks").asCompoundTagList());
			if (tileTicks != null) {
				tileTicks.forEach(v -> applyOffsetToTick(v, offset));
			}
		}

		// adjust liquid ticks
		if (level.containsKey("LiquidTicks")) {
			ListTag<CompoundTag> liquidTicks = catchClassCastException(() -> level.getListTag("LiquidTicks").asCompoundTagList());
			if (liquidTicks != null) {
				liquidTicks.forEach(v -> applyOffsetToTick(v, offset));
			}
		}

		// adjust structures
		if (level.containsKey("Structures")) {
			CompoundTag structures = catchClassCastException(() -> level.getCompoundTag("Structures"));
			if (structures != null) {
				applyOffsetToStructures(structures, offset);
			}
		}

		return true;
	}

	private void applyOffsetToStructures(CompoundTag structures, Point2i offset) {
		Point2i chunkOffset = offset.blockToChunk();

		// update references
		if (structures.containsKey("References")) {
			CompoundTag references = catchClassCastException(() -> structures.getCompoundTag("References"));
			if (references != null) {
				for (Map.Entry<String, Tag<?>> entry : references) {
					long[] reference = catchClassCastException(() -> ((LongArrayTag) entry.getValue()).getValue());
					if (reference != null) {
						for (int i = 0; i < reference.length; i++) {
							int x = (int) (reference[i]);
							int z = (int) (reference[i] >> 32);
							reference[i] = ((long) (z + chunkOffset.getY()) & 0xFFFFFFFFL) << 32 | (long) (x + chunkOffset.getX()) & 0xFFFFFFFFL;
						}
					}
				}
			}
		}

		// update starts
		if (structures.containsKey("Starts")) {
			CompoundTag starts = catchClassCastException(() -> structures.getCompoundTag("Starts"));
			if (starts != null) {
				for (Map.Entry<String, Tag<?>> entry : starts) {
					CompoundTag structure = catchClassCastException(() -> (CompoundTag) entry.getValue());
					if (structure == null || "INVALID".equals(catchClassCastException(() -> structure.getString("id")))) {
						continue;
					}
					applyIntIfPresent(structure, "ChunkX", chunkOffset.getX());
					applyIntIfPresent(structure, "ChunkZ", chunkOffset.getY());
					applyOffsetToBB(catchClassCastException(() -> structure.getIntArray("BB")), offset);

					if (structure.containsKey("Processed")) {
						ListTag<CompoundTag> processed = catchClassCastException(() -> structure.getListTag("Processed").asCompoundTagList());
						if (processed != null) {
							for (CompoundTag chunk : processed) {
								applyIntIfPresent(chunk, "X", chunkOffset.getX());
								applyIntIfPresent(chunk, "Z", chunkOffset.getY());
							}
						}
					}

					if (structure.containsKey("Children")) {
						ListTag<CompoundTag> children = catchClassCastException(() -> structure.getListTag("Children").asCompoundTagList());
						if (children != null) {
							for (CompoundTag child : children) {
								applyIntIfPresent(child, "TPX", offset.getX());
								applyIntIfPresent(child, "TPZ", offset.getY());
								applyIntIfPresent(child, "PosX", offset.getX());
								applyIntIfPresent(child, "PosZ", offset.getY());
								applyOffsetToBB(catchClassCastException(() -> child.getIntArray("BB")), offset);

								if (child.containsKey("Entrances")) {
									ListTag<IntArrayTag> entrances = catchClassCastException(() -> child.getListTag("Entrances").asIntArrayTagList());
									if (entrances != null) {
										entrances.forEach(e -> applyOffsetToBB(e.getValue(), offset));
									}
								}

								if (child.containsKey("junctions")) {
									ListTag<CompoundTag> junctions = catchClassCastException(() -> child.getListTag("junctions").asCompoundTagList());
									if (junctions != null) {
										for (CompoundTag junction : junctions) {
											applyIntIfPresent(junction, "source_x", offset.getX());
											applyIntIfPresent(junction, "source_z", offset.getY());
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void applyOffsetToBB(int[] bb, Point2i offset) {
		if (bb == null || bb.length != 6) {
			return;
		}
		bb[0] += offset.getX();
		bb[2] += offset.getY();
		bb[3] += offset.getX();
		bb[5] += offset.getY();
	}

	private void applyOffsetToTick(CompoundTag tick, Point2i offset) {
		applyIntIfPresent(tick, "x", offset.getX());
		applyIntIfPresent(tick, "z", offset.getY());
	}

	private void applyOffsetToTileEntity(CompoundTag tileEntity, Point2i offset) {
		if (tileEntity == null) {
			return;
		}

		applyIntIfPresent(tileEntity, "x", offset.getX());
		applyIntIfPresent(tileEntity, "z", offset.getY());

		String id = catchClassCastException(() -> tileEntity.getString("id"));
		if (id != null) {
			switch (id) {
				case "minecraft:bee_nest":
				case "minecraft:beehive":
					CompoundTag flowerPos = catchClassCastException(() -> tileEntity.getCompoundTag("FlowerPos"));
					applyIntOffsetIfRootPresent(flowerPos, "X", "Z", offset);
					if (tileEntity.containsKey("Bees")) {
						ListTag<CompoundTag> bees = catchClassCastException(() -> tileEntity.getListTag("Bees").asCompoundTagList());
						if (bees != null) {
							for (CompoundTag bee : bees) {
								if (bee.containsKey("EntityData")) {
									applyOffsetToEntity(catchClassCastException(() -> bee.getCompoundTag("EntityData")), offset);
								}
							}
						}
					}
					break;
				case "minecraft:end_gateway":
					CompoundTag exitPortal = catchClassCastException(() -> tileEntity.getCompoundTag("ExitPortal"));
					applyIntOffsetIfRootPresent(exitPortal, "X", "Z", offset);
					break;
				case "minecraft:structure_block":
					applyIntIfPresent(tileEntity, "posX", offset.getX());
					applyIntIfPresent(tileEntity, "posX", offset.getY());
					break;
				case "minecraft:jukebox":
					CompoundTag recordItem = catchClassCastException(() -> tileEntity.getCompoundTag("RecordItem"));
					applyOffsetToItem(recordItem, offset);
					break;
				case "minecraft:lectern":
					CompoundTag book = catchClassCastException(() -> tileEntity.getCompoundTag("Book"));
					applyOffsetToItem(book, offset);
					break;
				case "minecraft:mob_spawner":
					if (tileEntity.containsKey("SpawnPotentials")) {
						ListTag<CompoundTag> spawnPotentials = catchClassCastException(() -> tileEntity.getListTag("SpawnPotentials").asCompoundTagList());
						if (spawnPotentials != null) {
							for (CompoundTag spawnPotential : spawnPotentials) {
								CompoundTag entity = catchClassCastException(() -> spawnPotential.getCompoundTag("Entity"));
								if (entity != null) {
									applyOffsetToEntity(entity, offset);
								}
							}
						}
					}
			}
		}

		if (tileEntity.containsKey("Items")) {
			ListTag<CompoundTag> items = catchClassCastException(() -> tileEntity.getListTag("Items").asCompoundTagList());
			if (items != null) {
				items.forEach(i -> applyOffsetToItem(i, offset));
			}
		}
	}

	private void applyOffsetToEntity(CompoundTag entity, Point2i offset) {
		if (entity == null) {
			return;
		}
		if (entity.containsKey("Pos")) {
			ListTag<DoubleTag> entityPos = catchClassCastException(() -> entity.getListTag("Pos").asDoubleTagList());
			if (entityPos != null && entityPos.size() == 3) {
				entityPos.set(0, new DoubleTag(entityPos.get(0).asDouble() + offset.getX()));
				entityPos.set(2, new DoubleTag(entityPos.get(2).asDouble() + offset.getY()));
			}
		}

		// leashed entities
		if (entity.containsKey("Leash")) {
			CompoundTag leash = catchClassCastException(() -> entity.getCompoundTag("Leash"));
			applyIntOffsetIfRootPresent(leash, "X", "Z", offset);
		}

		// projectiles
		applyIntIfPresent(entity, "xTile", offset.getX());
		applyIntIfPresent(entity, "zTile", offset.getY());

		// entities that have a sleeping place
		applyIntIfPresent(entity, "SleepingX", offset.getX());
		applyIntIfPresent(entity, "SleepingZ", offset.getY());

		// positions for specific entity types
		String id = catchClassCastException(() -> entity.getString("id"));
		if (id != null) {
			switch (id) {
				case "minecraft:dolphin":
					if (entity.getBoolean("CanFindTreasure")) {
						applyIntIfPresent(entity, "TreasurePosX", offset.getX());
						applyIntIfPresent(entity, "TreasurePosZ", offset.getY());
					}
					break;
				case "minecraft:phantom":
					applyIntIfPresent(entity, "AX", offset.getX());
					applyIntIfPresent(entity, "AZ", offset.getY());
					break;
				case "minecraft:shulker":
					applyIntIfPresent(entity, "APX", offset.getX());
					applyIntIfPresent(entity, "APZ", offset.getY());
					break;
				case "minecraft:turtle":
					applyIntIfPresent(entity, "HomePosX", offset.getX());
					applyIntIfPresent(entity, "HomePosZ", offset.getY());
					applyIntIfPresent(entity, "TravelPosX", offset.getX());
					applyIntIfPresent(entity, "TravelPosZ", offset.getY());
					break;
				case "minecraft:vex":
					applyIntIfPresent(entity, "BoundX", offset.getX());
					applyIntIfPresent(entity, "BoundZ", offset.getY());
					break;
				case "minecraft:wandering_trader":
					if (entity.containsKey("WanderTarget")) {
						CompoundTag wanderTarget = catchClassCastException(() -> entity.getCompoundTag("WanderTarget"));
						applyIntOffsetIfRootPresent(wanderTarget, "X", "Z", offset);
					}
					break;
				case "minecraft:shulker_bullet":
					CompoundTag owner = catchClassCastException(() -> entity.getCompoundTag("Owner"));
					applyIntOffsetIfRootPresent(owner, "X", "Z", offset);
					CompoundTag target = catchClassCastException(() -> entity.getCompoundTag("Target"));
					applyIntOffsetIfRootPresent(target, "X", "Z", offset);
					break;
				case "minecraft:end_crystal":
					CompoundTag beamTarget = catchClassCastException(() -> entity.getCompoundTag("BeamTarget"));
					applyIntOffsetIfRootPresent(beamTarget, "X", "Z", offset);
					break;
				case "minecraft:item_frame":
				case "minecraft:painting":
					applyIntIfPresent(entity, "TileX", offset.getX());
					applyIntIfPresent(entity, "TileZ", offset.getY());
					break;
				case "minecraft:villager":
					if (entity.containsKey("Brain")) {
						CompoundTag brain = catchClassCastException(() -> entity.getCompoundTag("Brain"));
						if (brain != null && brain.containsKey("memories")) {
							CompoundTag memories = catchClassCastException(() -> brain.getCompoundTag("memories"));
							if (memories != null && memories.size() > 0) {
								CompoundTag meetingPoint = catchClassCastException(() -> memories.getCompoundTag("minecraft:meeting_point"));
								if (meetingPoint != null && meetingPoint.containsKey("pos")) {
									if (meetingPoint.get("pos") instanceof IntArrayTag) {
										int[] pos = catchClassCastException(() -> meetingPoint.getIntArray("pos"));
										applyOffsetToIntArrayPos(pos, offset);
									} else if (meetingPoint.get("pos") instanceof ListTag) {
										ListTag<IntTag> pos = catchClassCastException(() -> meetingPoint.getListTag("pos").asIntTagList());
										applyOffsetToIntListPos(pos, offset);
									}
								}
								CompoundTag home = catchClassCastException(() -> memories.getCompoundTag("minecraft:home"));
								if (home != null && home.containsKey("pos")) {
									if (home.get("pos") instanceof IntArrayTag) {
										int[] pos = catchClassCastException(() -> home.getIntArray("pos"));
										applyOffsetToIntArrayPos(pos, offset);
									} else if (home.get("pos") instanceof ListTag) {
										ListTag<IntTag> pos = catchClassCastException(() -> home.getListTag("pos").asIntTagList());
										applyOffsetToIntListPos(pos, offset);
									}
								}
								CompoundTag jobSite = catchClassCastException(() -> memories.getCompoundTag("minecraft:job_site"));
								if (jobSite != null && jobSite.containsKey("pos")) {
									if (jobSite.get("pos") instanceof IntArrayTag) {
										int[] pos = catchClassCastException(() -> jobSite.getIntArray("pos"));
										applyOffsetToIntArrayPos(pos, offset);
									} else if (jobSite.get("pos") instanceof ListTag) {
										ListTag<IntTag> pos = catchClassCastException(() -> jobSite.getListTag("pos").asIntTagList());
										applyOffsetToIntListPos(pos, offset);
									}
								}
							}
						}
					}
					break;
				case "minecraft:pillager":
				case "minecraft:witch":
				case "minecraft:vindicator":
				case "minecraft:ravager":
				case "minecraft:illusioner":
				case "minecraft:evoker":
					CompoundTag patrolTarget = catchClassCastException(() -> entity.getCompoundTag("PatrolTarget"));
					if (patrolTarget != null) {
						applyIntOffsetIfRootPresent(patrolTarget, "X", "Z", offset);
					}
					break;
				case "minecraft:falling_block":
					CompoundTag tileEntityData = catchClassCastException(() -> entity.getCompoundTag("TileEntityData"));
					applyOffsetToTileEntity(tileEntityData, offset);
			}
		}

		// recursively update passengers

		if (entity.containsKey("Passengers")) {
			ListTag<CompoundTag> passengers = catchClassCastException(() -> entity.getListTag("Passengers").asCompoundTagList());
			if (passengers != null) {
				passengers.forEach(p -> applyOffsetToEntity(p, offset));
			}
		}

		if (entity.containsKey("Item")) {
			CompoundTag item = catchClassCastException(() -> entity.getCompoundTag("Item"));
			applyOffsetToItem(item, offset);
		}

		if (entity.containsKey("Items")) {
			ListTag<CompoundTag> items = catchClassCastException(() -> entity.getListTag("Items").asCompoundTagList());
			if (items != null) {
				items.forEach(i -> applyOffsetToItem(i, offset));
			}
		}

		if (entity.containsKey("HandItems")) {
			ListTag<CompoundTag> items = catchClassCastException(() -> entity.getListTag("HandItems").asCompoundTagList());
			if (items != null) {
				items.forEach(i -> applyOffsetToItem(i, offset));
			}
		}

		if (entity.containsKey("ArmorItems")) {
			ListTag<CompoundTag> items = catchClassCastException(() -> entity.getListTag("ArmorItems").asCompoundTagList());
			if (items != null) {
				items.forEach(i -> applyOffsetToItem(i, offset));
			}
		}
	}

	private void applyOffsetToItem(CompoundTag item, Point2i offset) {
		if (item != null) {
			String id = catchClassCastException(() -> item.getString("id"));
			CompoundTag tag = catchClassCastException(() -> item.getCompoundTag("tag"));

			if (id != null && tag != null) {
				// using a switch-case here for the future
				// noinspection SwitchStatementWithTooFewBranches
				switch (id) {
					case "minecraft:compass":
						CompoundTag lodestonePos = catchClassCastException(() -> tag.getCompoundTag("LodestonePos"));
						applyIntOffsetIfRootPresent(lodestonePos, "X", "Z", offset);
						break;
				}

				// recursively update all items in child containers

				CompoundTag blockEntityTag = catchClassCastException(() -> tag.getCompoundTag("BlockEntityTag"));
				if (blockEntityTag != null) {
					if (blockEntityTag.containsKey("Items")) {
						ListTag<CompoundTag> items = catchClassCastException(() -> blockEntityTag.getListTag("Items").asCompoundTagList());
						if (items != null) {
							items.forEach(i -> applyOffsetToItem(i, offset));
						}
					}
				}
			}
		}
	}

	private void applyIntOffsetIfRootPresent(CompoundTag root, String xKey, String zKey, Point2i offset) {
		if (root != null) {
			applyIntIfPresent(root, xKey, offset.getX());
			applyIntIfPresent(root, zKey, offset.getY());
		}
	}

	private void applyIntIfPresent(CompoundTag root, String key, int offset) {
		Integer value;
		if (root.containsKey(key) && (value = catchClassCastException(() -> root.getInt(key))) != null) {
			root.putInt(key, value + offset);
		}
	}

	private void applyOffsetToIntListPos(ListTag<IntTag> pos, Point2i offset) {
		if (pos != null && pos.size() == 3) {
			pos.set(0, new IntTag(pos.get(0).asInt() + offset.getX()));
			pos.set(2, new IntTag(pos.get(2).asInt() + offset.getY()));
		}
	}

	private void applyOffsetToIntArrayPos(int[] pos, Point2i offset) {
		if (pos != null && pos.length == 3) {
			pos[0] += offset.getX();
			pos[2] += offset.getY();
		}
	}
}
