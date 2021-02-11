package net.querz.mcaselector.io;

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
import java.util.*;

public class SelectionHelper {

	private SelectionHelper() {}

	public static void exportSelection(SelectionData data, File file) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			if (data.isInverted()) {
				bw.write("inverted\n");
			}
			for (Map.Entry<Point2i, Set<Point2i>> entry : data.getSelection().entrySet()) {
				Point2i r = entry.getKey();
				if (entry.getValue() != null) {
					for (Point2i c : entry.getValue()) {
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
		Map<Point2i, Set<Point2i>> chunks = new HashMap<>();
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
					Point2i region = new Point2i(x, z);
					if (cx == null) {
						//don't overwrite possibly selected chunks in this region with null
						if (!chunks.containsKey(region)) {
							chunks.put(region, null);
						}
					} else {
						chunks.computeIfAbsent(region, k -> new HashSet<>());
						chunks.get(region).add(new Point2i(cx, cz));
					}
				}
			}
		} catch (IOException ex) {
			Debug.dumpException("failed to import selection", ex);
		}
		return new SelectionData(chunks, inverted);
	}

	public static Map<Point2i, Set<Point2i>> getTrueSelection(SelectionData selection) {
		Map<Point2i, Set<Point2i>> sel = selection == null ? null : selection.getSelection();
		if (selection != null && selection.isInverted()) {
			sel = new HashMap<>();
			Set<Point2i> allRegions = FileHelper.parseAllMCAFileNames(Config.getWorldDir());
			Set<Point2i> allPoi = FileHelper.parseAllMCAFileNames(Config.getWorldDirs().getPoi());
			Set<Point2i> allEntities = FileHelper.parseAllMCAFileNames(Config.getWorldDirs().getEntities());
			allRegions.addAll(allPoi);
			allRegions.addAll(allEntities);
			for (Point2i region : allRegions) {
				if (selection.isRegionSelected(region)) {
					if (!selection.getSelection().containsKey(region)) {
						sel.put(region, null);
					} else if (selection.getSelection().get(region) != null) {
						sel.put(region, SelectionData.createInvertedRegionSet(region, selection.getSelection().get(region)));
					}
				}
			}
		}
		return sel;
	}
}
