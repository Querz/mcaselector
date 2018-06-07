package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.ColorMapping;
import net.querz.mcaselector.util.Helper;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Anvil112ColorMapping implements ColorMapping {

	private Map<Integer, Integer> mapping = new HashMap<>();

	public Anvil112ColorMapping() {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Anvil112ColorMapping.class.getClass().getResourceAsStream("/colors112.csv")))) {
			String line;
			while ((line = bis.readLine()) != null) {
				String[] elements = line.split(";");
				if (elements.length != 3) {
					System.out.println("invalid line in color file: \"" + line + "\"");
					continue;
				}
				Integer id = Helper.parseInt(elements[0], 10);
				if (id == null || id < 0 || id > 255) {
					System.out.println("Invalid block id in color file: \"" + elements[0] + "\"");
					continue;
				}
				Integer data = Helper.parseInt(elements[1], 10);
				if (data == null || data < 0 || data > 15) {
					System.out.println("Invalid block data in color file: \"" + elements[1] + "\"");
					continue;
				}
				Integer color = Helper.parseInt(elements[2], 16);
				if (color == null || color < 0x0 || color > 0xFFFFFF) {
					System.out.println("Invalid color code in color file: \"" + elements[2] + "\"");
				}
				mapping.put((id << 4) + data, color);
			}
		} catch (IOException ex) {
			throw new RuntimeException("Unable to open color file");
		}
	}

	@Override
	public int getRGB(Object blockID) {
		return mapping.getOrDefault(blockID, 0x000000);
	}
}
