package net.querz.mcaselector;

import java.util.HashMap;
import java.util.Map;

public class Anvil112ColorMapping implements ColorMapping {
	@Override
	public int getRGB(int blockID) {
		return Mapping.getByIDAndData(blockID).getRGB();
	}

	private enum Mapping {
		DEFAULT(0, 0, 0x000000),
		STONE(1, 0, 0x999999),
		GRASS(2, 0, 0x33cc33),
		DIRT(3, 0, 0x994d00),
		COARSE_DIRT(3, 1, 0x994d00),
		PODZOL(3, 2, 0x994d00),
		COBBLESTONE(4, 0, 0x808080),
		OAK_PLANKS(5, 0, 0xbf8040),
		SPRUCE_PLANKS(5, 1, 0x734d26),
		BIRCH_PLANKS(5, 2, 0xffd480),
		JUNGLE_PLANKS(5, 3, 0xe67300),
		ACACIA_PLANKS(5, 4, 0xff8000),
		DAR_OAK_PLANKS(5, 5, 0x4d2600),
		GRAVEL(13, 0, 0xcccccc),
		IRON_ORE(15, 0, 0x3b2612),
		COAL_ORE(16, 0, 0x333333),
		LEAVES(18, 0x336600),
		WATER(8, 0, 0x0066ff),
		STATIONARY_WATER(9, 0, 0x0066ff),
		SAND(12, 0, 0xffcc00),
		TALL_GRASS(31, 1, 0x00e600),
		WHITE_WOOL(35, 0, 0xffffff),
		ORANGE_WOOL(35, 1, 0xff6600),
		MAGENTA_WOOL(35, 2, 0xff00ff),
		LIGHT_BLUE_WOOL(35, 3, 0x66ccff),
		YELLOW_WOOL(35, 4, 0xffff00),
		LIME_WOOL(35, 5, 0x99ff33),
		PINK_WOOL(35, 6, 0xff99ff),
		GREY_WOOL(35, 7, 0x404040),
		LIGHT_GREY_WOOL(35, 8, 0x8c8c8c),
		CYAN_WOOL(35, 9, 0x33cccc),
		PURPLE_WOOL(35, 10, 0x990099),
		BLUE_WOOL(35, 11, 0x0000cc),
		BROWN_WOOL(35, 12, 0x994d00),
		GREEN_WOOL(35, 13, 0x336600),
		RED_WOOL(35, 14, 0xe60000),
		BLACK_WOOL(35, 15, 0x0d0d0d),

		STONE_BRICK_SLAB(44, 5, 0x737373),
		STONE_BRICK_SLAB_INVERTED(44, 13, 0x737373),

		STONE_BRICK_STAIRS(109, 0x737373),
		SPRUCE_STAIRS(134, 0x734d26),
		BIRCH_STAIRS(135, 0xffd480),
		JUNGLE_STAIRS(136, 0xe67300),
		DOUBLE_TALL_GRASS(175, 2, 0x00e600);

		private int id;
		private int data;
		private int rgb;

		private static Map<Integer, Mapping> map = new HashMap<>();

		static {
			for (Mapping m : Mapping.values()) {
				if (m.data == -1) {
					for (int i = 0; i < 16; i++) {
						map.put(m.idAndDataToInt(m.id, i), m);
					}
				} else {
					map.put(m.idAndDataToInt(), m);
				}
			}
		}

		Mapping(int id, int rgb) {
			this.id = id;
			this.rgb = rgb;
			data = -1;
		}

		Mapping(int id, int data, int rgb) {
			this.id = id;
			this.data = data;
			this.rgb = rgb;
		}

		private int idAndDataToInt() {
			return (id << 4) + data;
		}


		private int idAndDataToInt(int id, int data) {
			return (id << 4) + data;
		}

		public int getId() {
			return id;
		}

		public int getRGB() {
			return rgb;
		}

		public static Mapping getByIDAndData(int idAndData) {
			Mapping m = map.get(idAndData);
			if (m == null) {
				return Mapping.DEFAULT;
			}
			return m;
		}


	}
}
