package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.version.ColorMapping;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Anvil112ColorMapping implements ColorMapping {

	private final Map<Integer, Integer> mapping = new HashMap<>();

	public Anvil112ColorMapping() {
		// noinspection ConstantConditions
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Anvil112ColorMapping.class.getClassLoader().getResourceAsStream("mapping/112/colors.txt")))) {
			String line;
			while ((line = bis.readLine()) != null) {
				String[] elements = line.split(";");
				if (elements.length != 3) {
					Debug.dumpf("invalid line in color file: \"%s\"", line);
					continue;
				}
				Integer id = TextHelper.parseInt(elements[0], 10);
				if (id == null || id < 0 || id > 255) {
					Debug.dumpf("invalid block id in color file: \"%s\"", elements[0]);
					continue;
				}
				Integer data = TextHelper.parseInt(elements[1], 10);
				if (data == null || data < 0 || data > 15) {
					Debug.dumpf("invalid block data in color file: \"%s\"", elements[1]);
					continue;
				}
				Integer color = TextHelper.parseInt(elements[2], 16);
				if (color == null || color < 0x0 || color > 0xFFFFFF) {
					Debug.dumpf("invalid color code in color file: \"%s\"", elements[2]);
				}
				mapping.put((id << 4) + data, color);
			}
		} catch (IOException ex) {
			throw new RuntimeException("failed to read mapping/112/colors.txt");
		}
	}

	@Override
	public int getRGB(Object blockID) {
		//noinspection SuspiciousMethodCalls
		return mapping.getOrDefault(blockID, 0x000000);
	}
}
