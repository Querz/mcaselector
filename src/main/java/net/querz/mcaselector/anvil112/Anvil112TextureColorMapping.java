package net.querz.mcaselector.anvil112;

import net.querz.mcaselector.ColorMapping;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Anvil112TextureColorMapping implements ColorMapping {

	private static Map<Integer, Integer> mapping = new HashMap<>();

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Anvil112TextureColorMapping.class.getClass().getResourceAsStream("/colors.csv")))) {
			String line;
			while ((line = bis.readLine()) != null) {
				String[] elements = line.split(";");
				if (elements.length != 3) {
					System.out.println("invalid line in color file: \"" + line + "\"");
					continue;
				}
				int id = parseInt(elements[0], 10);
				if (id < 0 || id > 255) {
					System.out.println("Invalid block id in color file: \"" + elements[0] + "\"");
					continue;
				}
				int data = parseInt(elements[1], 10);
				if (data < 0 || data > 15) {
					System.out.println("Invalid block data in color file: \"" + elements[1] + "\"");
					continue;
				}
				int color = parseInt(elements[2], 16);
				if (color < 0x0 || color > 0xFFFFFF) {
					System.out.println("Invalid color code in color file: \"" + elements[2] + "\"");
				}
				mapping.put((id << 4) + data, color);
			}
		} catch (IOException ex) {
			throw new RuntimeException("Unable to open color file");
		}
	}

	private static int parseInt(String s, int radix) {
		try {
			return Integer.parseInt(s, radix);
		} catch (NumberFormatException ex) {
			return -1;
		}
	}

	@Override
	public int getRGB(Object blockID) {
		return mapping.getOrDefault(blockID, 0x000000);
	}
}
