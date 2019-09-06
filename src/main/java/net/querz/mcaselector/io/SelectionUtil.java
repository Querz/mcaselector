package net.querz.mcaselector.io;

import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SelectionUtil {

	private SelectionUtil() {}

	public static void exportSelection(Map<Point2i, Set<Point2i>> chunks, File file) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			for (Map.Entry<Point2i, Set<Point2i>> entry : chunks.entrySet()) {
				Point2i r = entry.getKey();
				if (entry.getValue() != null) {
					for (Point2i c : entry.getValue()) {
						bw.write(r.getX() + ";" + r.getY() + ";" + c.getX() + ";" + c.getY() + "\n");
					}
				} else {
					bw.write(r.getX() + ";" + r.getY() + "\n");
				}
			}
		} catch (IOException ex) {
			Debug.error(ex);
		}
	}

	public static Map<Point2i, Set<Point2i>> importSelection(File file) {
		Map<Point2i, Set<Point2i>> chunks = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
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
			Debug.error(ex);
		}
		return chunks;
	}
}
