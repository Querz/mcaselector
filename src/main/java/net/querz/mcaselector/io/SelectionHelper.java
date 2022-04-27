package net.querz.mcaselector.io;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public final class SelectionHelper {

	private SelectionHelper() {}

	public static void exportSelection(SelectionData data, File file) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			if (data.inverted()) {
				bw.write("inverted\n");
			}
			for (Long2ObjectMap.Entry<LongOpenHashSet> entry : data.selection().long2ObjectEntrySet()) {
				Point2i r = new Point2i(entry.getLongKey());
				if (entry.getValue() != null) {
					for (long chunk : entry.getValue()) {
						Point2i c = new Point2i(chunk);
						bw.write(r.getX() + ";" + r.getZ() + ";" + c.getX() + ";" + c.getZ() + "\n");
					}
				} else {
					bw.write(r.getX() + ";" + r.getZ() + "\n");
				}
			}
		} catch (IOException ex) {
			Debug.dumpException("failed to export selection", ex);
		}
	}

	public static SelectionData importSelection(File file) {
		Long2ObjectOpenHashMap<LongOpenHashSet> chunks = new Long2ObjectOpenHashMap<>();
		boolean inverted = false;
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			int num = 0;
			while ((line = br.readLine()) != null) {
				num++;
				if (num == 1 && "inverted".equals(line)) {
					inverted = true;
					continue;
				}
				String[] elements = line.split(";");
				if (elements.length == 2 || elements.length == 4) {
					Integer x = TextHelper.parseInt(elements[0], 10);
					Integer z = TextHelper.parseInt(elements[1], 10);
					if (x == null || z == null) {
						Debug.error("could not read region in selection import: " + line);
						continue;
					}
					Integer cx = null, cz = null;
					if (elements.length == 4) {
						cx = TextHelper.parseInt(elements[2], 10);
						cz = TextHelper.parseInt(elements[3], 10);
						if (cx == null || cz == null) {
							Debug.error("could not read chunk in selection import: " + line);
							continue;
						}
					}
					long region = new Point2i(x, z).asLong();
					if (cx == null) {
						// don't overwrite possibly selected chunks in this region with null
						if (!chunks.containsKey(region)) {
							chunks.put(region, null);
						}
					} else {
						chunks.computeIfAbsent(region, k -> new LongOpenHashSet());
						chunks.get(region).add(new Point2i(cx, cz).asLong());
					}
				}
			}
		} catch (IOException ex) {
			Debug.dumpException("failed to import selection", ex);
		}
		return new SelectionData(chunks, inverted);
	}

	public static Long2ObjectOpenHashMap<LongOpenHashSet> getTrueSelection(SelectionData selection) {
		Long2ObjectOpenHashMap<LongOpenHashSet> sel = selection == null ? null : selection.selection();
		if (selection != null && selection.inverted()) {
			sel = new Long2ObjectOpenHashMap<>();
			LongOpenHashSet allRegions = FileHelper.parseAllMCAFileNames(Config.getWorldDir());
			LongOpenHashSet allPoi = FileHelper.parseAllMCAFileNames(Config.getWorldDirs().getPoi());
			LongOpenHashSet allEntities = FileHelper.parseAllMCAFileNames(Config.getWorldDirs().getEntities());
			allRegions.addAll(allPoi);
			allRegions.addAll(allEntities);
			for (long region : allRegions) {
				if (selection.isRegionSelected(region)) {
					if (!selection.selection().containsKey(region)) {
						sel.put(region, null);
					} else if (selection.selection().get(region) != null) {
						sel.put(region, SelectionData.createInvertedRegionSet(new Point2i(region), selection.selection().get(region)));
					}
				}
			}
		}
		return sel;
	}

	public static SelectionInfo getSelectionInfo(Long2ObjectOpenHashMap<LongOpenHashSet> selection) {
		int minX, maxX, minZ, maxZ;
		minX = minZ = Integer.MAX_VALUE;
		maxX = maxZ = Integer.MIN_VALUE;
		for (Long2ObjectMap.Entry<LongOpenHashSet> entry : selection.long2ObjectEntrySet()) {
			if (entry.getValue() == null) {
				Point2i min = new Point2i(entry.getLongKey()).regionToChunk();
				Point2i max = new Point2i(entry.getLongKey()).regionToChunk().add(31);
				if (min.getX() < minX) {
					minX = min.getX();
				}
				if (min.getZ() < minZ) {
					minZ = min.getZ();
				}
				if (max.getX() > maxX) {
					maxX = max.getX();
				}
				if (max.getZ() > maxZ) {
					maxZ = max.getZ();
				}
			} else {
				for (long chunk : entry.getValue()) {
					Point2i c = new Point2i(chunk);
					if (c.getX() < minX) {
						minX = c.getX();
					}
					if (c.getZ() < minZ) {
						minZ = c.getZ();
					}
					if (c.getX() > maxX) {
						maxX = c.getX();
					}
					if (c.getZ() > maxZ) {
						maxZ = c.getZ();
					}
				}
			}
		}
		return new SelectionInfo(new Point2i(minX, minZ), new Point2i(maxX, maxZ));
	}
}
