package net.querz.mcaselector.io;

import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SelectionExporter {

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
			ex.printStackTrace();
		}
	}

	public static Map<Point2i, Set<Point2i>> importSelection(File file) {
		Map<Point2i, Set<Point2i>> chunks = new HashMap<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] elements = line.split(";");
				if (elements.length == 2 || elements.length == 4) {
					Integer x = Helper.parseInt(elements[0], 10);
					Integer z = Helper.parseInt(elements[1], 10);
					if (x == null || z == null) {
						System.out.println("could not read region in selection import: " + line);
						continue;
					}
					Integer cx = null, cz = null;
					if (elements.length == 4) {
						cx = Helper.parseInt(elements[2], 10);
						cz = Helper.parseInt(elements[3], 10);
						if (cx == null || cz == null) {
							System.out.println("could not read chunk in selection import: " + line);
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
			ex.printStackTrace();
		}
		return chunks;
	}

	public static void exportSelectedChunks(Map<Point2i, Set<Point2i>> chunks, File dir) {
		for (Map.Entry<Point2i, Set<Point2i>> entry : chunks.entrySet()) {
			File file = Helper.createMCAFilePath(entry.getKey());
			if (file.exists()) {
				File to = new File(dir, Helper.createMCAFileName(entry.getKey()));
				if (to.exists()) {
					System.out.println(to.getAbsolutePath() + " exists, not overwriting");
					continue;
				}
				try {
					Files.copy(file.toPath(), to.toPath());
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
				//invert selected chunks and tell the MCALoader to delete them
				Set<Point2i> c = new HashSet<>(Tile.CHUNKS - entry.getValue().size());
				for (int x = Helper.regionToChunk(entry.getKey()).getX(); x < Tile.SIZE_IN_CHUNKS; x++) {
					for (int z = Helper.regionToChunk(entry.getKey()).getY(); z < Tile.SIZE_IN_CHUNKS; z++) {
						Point2i cp = new Point2i(x, z);
						if (!entry.getValue().contains(cp)) {
							c.add(cp);
						}
					}
				}
				Map<Point2i, Set<Point2i>> inverted = new HashMap<>(1);
				inverted.put(entry.getKey(), c);
				MCALoader.deleteChunks(inverted, dir);
			}
		}
	}
}
