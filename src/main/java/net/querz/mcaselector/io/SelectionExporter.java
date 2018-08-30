package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.ui.ProgressTask;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SelectionExporter {

	private SelectionExporter() {}

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
					Integer x = Helper.parseInt(elements[0], 10);
					Integer z = Helper.parseInt(elements[1], 10);
					if (x == null || z == null) {
						Debug.error("could not read region in selection import: " + line);
						continue;
					}
					Integer cx = null, cz = null;
					if (elements.length == 4) {
						cx = Helper.parseInt(elements[2], 10);
						cz = Helper.parseInt(elements[3], 10);
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

	public static void exportSelectedChunks(Map<Point2i, Set<Point2i>> chunks, File dir, ProgressTask progressChannel) {
		int filesCount = chunks.size();
		int i = 0;
		for (Map.Entry<Point2i, Set<Point2i>> entry : chunks.entrySet()) {
			File file = Helper.createMCAFilePath(entry.getKey());

			progressChannel.updateProgress(file.getName(), i, filesCount);

			if (file.exists()) {
				File to = new File(dir, Helper.createMCAFileName(entry.getKey()));
				if (to.exists()) {
					Debug.dump(to.getAbsolutePath() + " exists, not overwriting");
					continue;
				}
				try {
					Files.copy(file.toPath(), to.toPath());
				} catch (IOException ex) {
					Debug.error(ex);
					continue;
				}
				if (entry.getValue() == null) {
					continue;
				}
				//invert selected chunks and tell the MCALoader to delete them
				Set<Point2i> c = new HashSet<>(Tile.CHUNKS - entry.getValue().size());
				Point2i origin = Helper.regionToChunk(entry.getKey());
				for (int x = origin.getX(); x < origin.getX() + Tile.SIZE_IN_CHUNKS; x++) {
					for (int z = origin.getY(); z < origin.getY() + Tile.SIZE_IN_CHUNKS; z++) {
						Point2i cp = new Point2i(x, z);
						if (!entry.getValue().contains(cp)) {
							c.add(cp);
						}
					}
				}
				Map<Point2i, Set<Point2i>> inverted = new HashMap<>(1);
				inverted.put(entry.getKey(), c);
				MCALoader.deleteChunks(inverted, new ProgressTask.Dummy(), dir, false);
			}
			i++;
		}
		progressChannel.updateProgress("Done", 1, filesCount);
	}

	public static void exportFilteredChunks(GroupFilter filter, File dir, ProgressTask progressChannel) {
		File[] files = Config.getWorldDir().listFiles((d, n) -> n.matches("^r\\.-?\\d+\\.-?\\d+\\.mca$"));
		if (files == null) {
			return;
		}
		int filesCount = files.length;
		for (int i = 0; i < files.length; i++) {
			File file = files[i];

			progressChannel.updateProgress(file.getName(), i, filesCount);

			Pattern p = Pattern.compile("^r\\.(?<regionX>-?\\d+)\\.(?<regionZ>-?\\d+)\\.mca$");
			Matcher m = p.matcher(file.getName());
			if (m.find()) {
				int regionX = Integer.parseInt(m.group("regionX"));
				int regionZ = Integer.parseInt(m.group("regionZ"));

				if (!filter.appliesToRegion(new Point2i(regionX, regionZ))) {
					Debug.dump("filter does not apply to file " + file);
					continue;
				}

				//copy file to new directory
				File to = new File(dir, file.getName());
				if (to.exists()) {
					Debug.dump(to.getAbsolutePath() + " exists, not overwriting");
					continue;
				}
				try {
					Files.copy(file.toPath(), to.toPath());
				} catch (IOException ex) {
					Debug.error(ex);
					continue;
				}

				filter.setInverted(true);
				MCALoader.deleteChunks(filter, to, false);
				filter.setInverted(false);

			} else {
				Debug.dump("skipping " + file + ", could not parse file name");
			}
		}
		progressChannel.updateProgress("Done", 1, filesCount);
	}
}
