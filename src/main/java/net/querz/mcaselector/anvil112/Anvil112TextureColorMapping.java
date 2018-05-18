package net.querz.mcaselector.anvil112;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import net.querz.mcaselector.ColorMapping;
import net.querz.mcaselector.util.Helper;

import javax.imageio.ImageIO;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Anvil112TextureColorMapping implements ColorMapping {

	@Override
	public int getRGB(Object blockID) {
		return Mapping.getByIDAndData((int) blockID).getRGB();
	}

	private enum FoliageType {
		GRASS, FOLIAGE
	}

	private enum Mapping {

		DEFAULT(0, 0, 0x000000),
		STONE(1, 0, "stone"),
		GRANITE(1, 1, "stone_granite"),
		POLISHED_GRANITE(1, 2, "stone_granite_smooth"),
		DIORITE(1, 3, "stone_diorite"),
		POLISHED_DIORITE(1, 4, "stone_diorite_smooth"),
		ANDESITE(1, 5, "stone_andesite"),
		POLISHED_ANDESITE(1, 6, "stone_andesite_smooth"),
		GRASS(2, 0, "grass_top", FoliageType.GRASS), //grass_top
		DIRT(3, 0, "dirt"),
		COARSE_DIRT(3, 1, "coarse_dirt"),
		PODZOL(3, 2, "dirt_podzol_top"),
		COBBLESTONE(4, 0, "cobblestone"),
		OAK_WOOD_PLANKS(5, 0, "planks_oak"),
		SPRUCE_WOOD_PLANKS(5, 1, "planks_spruce"),
		BIRCH_WOOD_PLANKS(5, 2, "planks_birch"),
		JUNGLE_WOOD_PLANKS(5, 3, "planks_jungle"),
		ACACIA_WOOD_PLANKS(5, 4, "planks_acacia"),
		DARK_OAK_WOOD_PLANKS(5, 5, "planks_big_oak"),
		OAK_SAPLING(6, new int[]{0, 8}, "sapling_oak"),
		SPRUCE_SAPLING(6, new int[]{1, 9}, "sapling_spruce"),
		BIRCH_SAPLING(6, new int[]{2, 10}, "sapling_birch"),
		JUNGLE_SAPLING(6, new int[]{3, 11}, "sapling_jungle"),
		ACACIA_SAPLING(6, new int[]{4, 12}, "sapling_acacia"),
		DARK_OAK_SAPLING(6, new int[]{5, 13}, "sapling_roofed_oak"),
		BEDROCK(7, 0, "bedrock"),
		WATER(8, "water_still"),
		STATIONARY_WATER(9, "water_still"),
		LAVA(10, "lava_still"),
		STATIONARY_LAVA(11, "lava_still"),
		SAND(12, 0, "sand"),
		RED_SAND(12, 1, "red_sand"),
		GRAVEL(13, 0, "gravel"),
		GOLD_ORE(14, 0, "gold_ore"),
		IRON_ORE(15, 0, "iron_ore"),
		COAL_ORE(16, 0, "coal_ore"),
		OAK_WOOD(17, 0, "log_oak_top"),
		SPRUCE_WOOD(17, 1, "log_spruce_top"),
		BIRCH_WOOD(17, 2, "log_birch_top"),
		JUNGLE_WOOD(17, 3, "log_jungle_top"),
		OAK_WOOD_SIDE(17, new int[]{4, 8, 12}, "log_oak"),
		SPRUCE_WOOD_NORTH(17, new int[]{5, 9, 13}, "log_spruce"),
		BIRCH_WOOD_NORTH(17, new int[]{6, 10, 14}, "log_birch"),
		JUNGLE_WOOD_NORTH(17, new int[]{7, 11, 15}, "log_jungle"),
		OAK_LEAVES(18, new int[]{0, 4, 8, 12}, "leaves_oak", FoliageType.FOLIAGE),
		SPRUCE_LEAVES(18, new int[]{1, 5, 9, 13}, "leaves_spruce", FoliageType.FOLIAGE),
		BIRCH_LEAVES(18, new int[]{2, 6, 10, 14}, "leaves_birch", FoliageType.FOLIAGE),
		JUNGLE_LEAVES(18, new int[]{3, 7, 11, 15}, "leaves_jungle", FoliageType.FOLIAGE),
		SPONGE(19, 0, "sponge"),
		WET_SPONGE(19, 1, "sponge_wet"),
		GLASS(20, 0, "glass"),
		LAPIS_LAZULI_ORE(21, 0, "lapis_ore"),
		LAPIS_LAZULI(22, 0, "lapis_block"),
		DISPENSER(23, new int[]{0, 1, 2, 3, 4, 5}, "dispenser_front_vertical"),
		SANDSTONE(24, 0, "sandstone_top"),
		CHISELED_SANDSTONE(24, 1, "sandstone_top"),
		SMOOTH_SANDSTONE(24, 2, "sandstone_top"),
		NOTEBLOCK(25, 0, "noteblock"),
		BED(26, new int[]{0, 1, 2, 3, 8, 9, 10, 11}, "wool_colored_red"), //TODO: use real bed texture
		POWERED_RAIL(27, new int[]{0, 1, 2, 3, 4, 5}, "rail_golden"),
		POWERED_RAIL_POWERED(27, new int[]{8, 9, 10, 11, 12, 13}, "rail_golden_powered"),
		DETECTOR_RAIL(28, new int[]{0, 1, 2, 3, 4, 5}, "rail_detector"),
		DETECTOR_RAIL_POWERED(28, new int[]{8, 9, 10, 11, 12, 13}, "rail_detector_powered"),
		STICKY_PISTON(29, new int[]{0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13}, "piston_side"),
		COBWEB(30, 0, "web"),
		DEAD_BUSH(31, 0, "deadbush"),
		TALL_GRASS(31, 1, "tallgrass", FoliageType.GRASS),
		FERN(31, 2, "fern", FoliageType.GRASS),
		DAED_BUSH_2(32, 0, "deadbush"),
		PISTON(33, new int[]{0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 12, 13}, "piston_side"),
		STICKY_PISTON_HEAD(34, new int[]{0, 1, 2, 3, 4, 5}, "piston_top_sticky"),
		PISTON_HEAD(34, new int[]{8, 9, 10, 11, 12, 13}, "piston_top_normal"),
		WHITE_WOOL(35, 0, "wool_colored_white"),
		ORANGE_WOOL(35, 1, "wool_colored_orange"),
		MAGENTA_WOOL(35, 2, "wool_colored_magenta"),
		LIGHT_BLUE_WOOL(35, 3, "wool_colored_blue"),
		YELLOW_WOOL(35, 4, "wool_colored_yellow"),
		LIME_WOOL(35, 5, "wool_colored_lime"),
		PINK_WOOL(35, 6, "wool_colored_pink"),
		GREY_WOOL(35, 7, "wool_colored_gray"),
		LIGHT_GREY_WOOL(35, 8, "wool_colored_silver"),
		CYAN_WOOL(35, 9, "wool_colored_cyan"),
		PURPLE_WOOL(35, 10, "wool_colored_purple"),
		BLUE_WOOL(35, 11, "wool_colored_blue"),
		BROWN_WOOL(35, 12, "wool_colored_brown"),
		GREEN_WOOL(35, 13, "wool_colored_green"),
		RED_WOOL(35, 14, "wool_colored_red"),
		BLACK_WOOL(35, 15, "wool_colored_black"),
		PISTON_EXTENSION(36, "piston_top_normal"),
		DANDELION(37, 0, "flower_dandelion"),
		POPPY(38, 0, "flower_rose"),
		BLUE_ORCHID(38, 1, "flower_blue_orchid"),
		ALLIUM(38, 2, "flower_allium"),
		AZURE_BLUET(38, 3, "flower_paeonia"),
		RED_TULIP(38, 4, "flower_tulip_red"),
		ORANGE_TULIP(38, 5, "flower_tulip_orange"),
		WHITE_TULIP(38, 6, "flower_tulip_white"),
		PINK_TULIP(38, 7, "flower_tulip_pink"),
		OXEYE_DAISY(38, 8, "flower_oxeye_daisy"),
		BROWN_MUSHROOM(39, 0, "mushroom_brown"),
		RED_MUSHROOM(40, 0, "mushroom_red"),
		GOLD_BLOCK(41, 0, "gold_block"),
		IRON_BLOCK(42, 0, "iron_block"),
		DOUBLE_STONE_SLAB(43, 0, "stone_slab_top"),
		DOUBLE_SANDSTONE_SLAB(43, 1, "sandstone_top"),
		DOUBLE_OAK_WOOD_SLAB(43, 2, "planks_oak"),
		DOUBLE_COBBLESTONE_SLAB(43, 3, "cobblestone"),
		DOUBLE_BRICK_SLAB(43, 4, "brick"),
		DOUBLE_STONE_BRICK_SLAB(43, 5, "stonebrick"),
		DOUBLE_NETHER_BRICK_SLAB(43, 6, "nether_brick"),
		DOUBLE_QUARTZ_SLAB(43, 7, "quartz_block_top"),
		STONE_SLAB(44, new int[]{0, 8}, "stone_slab_top"),
		SANDSTONE_SLAB(44, new int[]{1, 9}, "sandstone_top"),
		OAK_WOOD_SLAB(44, new int[]{2, 10}, "planks_oak"),
		COBBLESTONE_SLAB(44, new int[]{3, 11}, "cobblestone"),
		BRICK_SLAB(44, new int[]{4, 12}, "brick"),
		STONE_BRICK_SLAB(44, new int[]{5, 13}, "stonebrick"),
		NETHER_BRICK_SLAB(44, new int[]{6, 14}, "nether_brick"),
		QUARTZ_SLAB(44, new int[]{7, 15}, "quartz_block_top"),
		BRICKS(45, 0, "brick"),
		TNT(46, 0, "tnt_top"),
		BOOKSHELF(47, 0, "planks_oak"),
		MOSSY_COBBLESTONE(48, 0, "cobblestone_mossy"),
		OBSIDIAN(49, 0, "obsidian"),
		TORCH(50, new int[]{0, 1, 2, 3, 4, 5}, "torch_on"),
		FIRE(51, "fire_layer_0"),
		MONSTER_SPAWNER(52, 0, "mob_spawner"),
		OAK_WOOD_STAIRS(53, "planks_oak"),
		CHEST(54, new int[]{0, 1, 2, 3, 4, 5}, "planks_oak"), //TODO: use real chest texture
		REDSTONE_WIRE(55, "redstone_block"),
		DIAMOND_ORE(56, 0, "diamond_ore"),
		DIAMOND_BLOCK(57, 0, "diamond_block"),
		CRAFTING_TABLE(58, 0, "crafting_table_top"),
		WHEAT_CROPS(59, new int[]{0, 1, 2, 3, 4, 5, 6}, "wheat_stage_7"),
		DRY_FARMLAND(60, new int[]{0, 1, 2, 3, 4, 5, 6}, "farmland_dry"),
		WET_FARMLAND(60, 7, "farmland_wet"),
		FURNACE(61, new int[]{0, 1, 2, 3}, "furnace_top"),
		BURNING_FURNACE(62, new int[]{0, 1, 2, 3}, "furnace_top"),
		STANDING_SIGN(63, "planks_oak"), //TODO: use real sign texture
		OAK_WOOD_DOOR(64, "door_wood_upper"),
		LADDER(65, new int[]{0, 1, 2, 3}, "ladder"),
		RAIL(66, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, "rail_normal"),
		COBBLESTONE_STAIRS(67, "cobblestone"),
		WALL_SIGN(68, "planks_oak"), //TODO: user real sign texture
		LEVER(69, "lever"),
		STONE_PRESSURE_PLATE(70, new int[]{0, 1}, "stone"),
		IRON_DOOR(71, "door_iron_upper"),
		OAK_WOOD_PRESSURE_PLATE(72, new int[]{0, 1}, "planks_oak"),
		REDSTONE_ORE(73, 0, "redstone_ore"),
		GLOWING_REDSTONE_ORE(74, 0, "redstone_ore"),
		REDSTONE_TORCH_OFF(75, "redstone_torch_off"),
		REDSTONE_TORCH_ON(76, "redstone_torch_on"),
		STONE_BUTTON(77, new int[]{0, 1, 2, 3, 4, 5}, "stone"),
		SNOW_LAYER(78, new int[]{0, 1, 2, 3, 4, 5, 6, 7}, "snow"),
		ICE(79, 0, "ice"),
		SNOW_BLOCK(80, 0, "snow"),
		CACTUS(81, "cactus_top"),
		CLAY(82, 0, "clay"),
		SUGAR_CANES(83, "reeds"),
		JUKEBOX(84, 0, "jukebox_top"),
		OAK_WOOD_FENCE(85, 0, "planks_oak"),
		PUMPKIN(86, new int[]{0, 1, 2, 3}, "pumpkin_top"),
		NETHERRACK(87, 0, "netherrack"),
		SOUL_SAND(88, 0, "soul_sand"),
		GLOWSTONE(89, 0, "glowstone"),
		NETHER_PORTAL(90, 0, "portal"),
		JACK_O_LANTERN(91, new int[]{0, 1, 2, 3}, "pumpkin_top"),
		CAKE(92, "cake_top"),
		REPEATER_OFF(93, "repeater_off"),
		REPEATER_ON(94, "repeater_on"),
		WHITE_STAINED_GLASS(95, 0, "glass_white"),
		ORANGE_STAINED_GLASS(95, 1, "glass_orange"),
		MAGENTA_STAINED_GLASS(95, 2, "glass_magenta"),
		LIGHT_BLUE_STAINED_GLASS(95, 3, "glass_blue"),
		YELLOW_STAINED_GLASS(95, 4, "glass_yellow"),
		LIME_STAINED_GLASS(95, 5, "glass_lime"),
		PINK_STAINED_GLASS(95, 6, "glass_pink"),
		GREY_STAINED_GLASS(95, 7, "glass_gray"),
		LIGHT_GREY_STAINED_GLASS(95, 8, "glass_silver"),
		CYAN_STAINED_GLASS(95, 9, "glass_cyan"),
		PURPLE_STAINED_GLASS(95, 10, "glass_purple"),
		BLUE_STAINED_GLASS(95, 11, "glass_blue"),
		BROWN_STAINED_GLASS(95, 12, "glass_brown"),
		GREEN_STAINED_GLASS(95, 13, "glass_green"),
		RED_STAINED_GLASS(95, 14, "glass_red"),
		BLACK_STAINED_GLASS(95, 15, "glass_black"),
		WOODEN_TRAPDOOR(96, "trapdoor"),
		STONE_MONSTER_EGG(97, 0, "stone"),
		COBBLESTONE_MOSTER_EGG(97, 1, "cobblestone"),
		STONE_BRICK_MONSTER_EGG(97, 2, "stonebrick"),
		MOSSY_STONE_BRICK_MONSTER_EGG(97, 3, "stonebrick_mossy"),
		CRACKED_STONE_BRICK_MONSTER_EGG(97, 4, "stonebrick_cracked"),
		CHISELED_STONE_BRICK_MONSTER_EGG(97, 5, "stonebrick_carved"),
		STONE_BRICKS(98, 0, "stonebrick"),
		MOSSY_STONE_BRICKS(98, 1, "stonebrick_mossy"),
		CRACKED_STONE_BRICKS(98, 2, "stonebrick_cracked"),
		CHISELED_STONE_BRICKS(98, 3, "stonebrick_carved"),
		BROWN_MUSHROOM_BLOCK(99, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, "mushroom_block_skin_brown"),
		RED_MUSHROOM_BLOCK(100, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, "mushroom_block_skin_red"),
		BROWN_MUSHROOM_STEM(99, 10, "mushroom_block_inside"),
		RED_MUSHROOM_STEM(100, 10, "mushroom_block_inside"),
		IRON_BARS(101, 0, "iron_bars"),
		GLASS_PANE(102, 0, "glass"),
		MELON(103, 0, "melon_top"),
		PUMPKIN_STEM(104, new int[]{0, 1, 2, 3, 4, 5, 6, 7}, "pumpkin_stem_connected", FoliageType.FOLIAGE),
		MELON_STEM(105, new int[]{0, 1, 2, 3, 4, 5, 6, 7}, "melon_stem_connected", FoliageType.FOLIAGE),
		VINES(106, "vine", FoliageType.FOLIAGE),
		OAK_WOOD_FENCE_GATE(107, "planks_oak"),
		BRICK_STAIRS(108, "brick"),
		STONE_BRICK_STAIRS(109, "stonebrick"),
		MYCELIUM(110, 0, "mycelium_top"),
		LILYPAD(111, 0, "waterlily", FoliageType.FOLIAGE),
		NETHER_BRICKS(112, 0, "nether_brick"),
		NETHER_BRICK_FENCE(113, 0, "nether_brick"),
		NETHER_BRICK_STAIRS(114, "nether_brick"),
		NETHER_WART(115, "nether_wart_stage_2"),
		ENCHANTMENT_TABLE(116, 0, "enchanting_table_top"),
		BREWING_STAND(117, 0, "brewing_stand_base"),
		CAULDRON(118, 0, "cauldron_top"),
		CAULDRON_FULL(118, new int[]{1, 2, 3}, "water_still"),
		END_PORTAL(119, 0, 0x000000), //???
		END_PORTAL_FRAME(120, "endframe_top"),
		END_STONE(121, 0, "end_stone"),
		DRAGON_EGG(122, 0, "dragon_egg"),
		REDSTONE_LAMP_OFF(123, 0, "redstone_lamp_off"),
		REDSTONE_LAMP_ON(124, 0, "redstone_lamp_on"),
		DOUBLE_OAK_WOOD_SLAB_2(125, 0, "planks_oak"),
		DOUBLE_SPRUCE_WOOD_SLAB(125, 1, "planks_spruce"),
		DOUBLE_BIRCH_WOOD_SLAB(125, 2, "planks_birch"),
		DOUBLE_JUNGLE_WOOD_SLAB(125, 3, "planks_jungle"),
		DOUBLE_ACACIA_WOOD_SLAB(125, 4, "planks_acacia"),
		DOUBLE_DARK_OAK_WOOD_SLAB(125, 5, "planks_big_oak"),
		OAK_WOOD_SLAB_2(126, new int[]{0, 8}, "planks_oak"),
		SPRUCE_WOOD_SLAB(126, new int[]{1, 9}, "planks_spruce"),
		BIRCH_WOOD_SLAB(126, new int[]{2, 10}, "planks_birch"),
		JUNGLE_WOOD_SLAB(126, new int[]{3, 11}, "planks_jungle"),
		ACACIA_WOOD_SLAB(126, new int[]{4, 12}, "planks_jungle"),
		DARK_OAK_WOOD_SLAB(126, new int[]{5, 13}, "planks_big_oak"),
		COCOA_POD(127, new int[]{0, 1, 2}, "cocoa_stage_2"),
		SANDSTONE_STAIRS(128, "sandstone_top"),
		EMERALD_ORE(129, 0, "emerald_ore"),
		ENDER_CHEST(130, new int[]{0, 1, 2, 3, 4, 5}, "obsidian"), //TODO: use real enderchest texture
		TRIPWIRE_HOOK(131, "trip_wire_source"),
		TRIPWIRE(132, "trip_wire"),
		EMERALD_BLOCK(133, 0, "emerald_block"),
		SPRUCE_WOOD_STAIRS(134, "planks_spruce"),
		BIRCH_WOOD_STAIRS(135, "planks_birch"),
		JUNGLE_WOOD_STAIRS(136, "planks_jungle"),
		COMMAND_BLOCK(137, "command_block_back"),
		BEACON(138, 0, "beacon"),
		COBBLESTONE_WALL(139, 0, "cobblestone"),
		MOSSY_COBBLESTONE_WALL(139, 1, "cobblestone_mossy"),
		FLOWER_POT(140, "flower_pot"),
		CARROTS(141, "carrots_stage_3"),
		POTATOES(142, "potatoes_stage_3"),
		OAK_WOOD_BUTTON(143, new int[]{0, 1, 2, 3, 4, 5}, "planks_oak"),
		MOB_HEAD(144, "bone_block_side"),
		ANVIL(145, "anvil_base"),
		TRAPPED_CHEST(146, new int[]{0, 1, 2, 3, 4, 5}, "planks_oak"),
		GOLD_PRESSURE_PLATE(147, new int[]{0, 1}, "gold_block"),
		IRON_PRESSURE_PLATE(148, new int[]{0, 1}, "iron_block"),
		COMPARATOR_OFF(149, "comparator_off"),
		COMPARATOR_ON(150, "comparator_on"),
		DAYLIGHT_SENSOR(151, 0, "daylight_detector_top"),
		REDSTONE_BLOCK(152, 0, "redstone_block"),
		NETHER_QUARTZ_ORE(153, 0, "quartz_ore"),
		HOPPER(154, new int[]{0, 2, 3, 4, 5}, "hopper_inside"),
		QUARTZ_BLOCK(155, 0, "quartz_block_bottom"),
		CHISELED_QUARTZ_BLOCK(155, 1, "quartz_block_chiseled_top"),
		PILLAR_QUARTZ_BLOCK(155, 2, "quartz_block_lines_top"),
		PILLAR_QUARTZ_BLOCK_SIDE(155, new int[]{3, 4}, "quartz_block_lines"),
		QUARTZ_STAIRS(156, "quartz_block_bottom"),
		ACTIVATOR_RAIL(157, new int[]{0, 1, 2, 3, 4, 5}, "rail_activator"),
		ACTIVATOR_RAIL_POWERED(157, new int[]{8, 9, 10, 11, 12, 13}, "rail_activator_powered"),
		DROPPER(158, new int[]{0, 1, 2, 3, 4, 5}, "dropper_front_vertical"),
		WHITE_STAINED_CLAY(159, 0, "hardened_clay_stained_white"),
		ORANGE_STAINED_CLAY(159, 1, "hardened_clay_stained_orange"),
		MAGENTA_STAINED_CLAY(159, 2, "hardened_clay_stained_magenta"),
		LIGHT_BLUE_STAINED_CLAY(159, 3, "hardened_clay_stained_blue"),
		YELLOW_STAINED_CLAY(159, 4, "hardened_clay_stained_yellow"),
		LIME_STAINED_CLAY(159, 5, "hardened_clay_stained_lime"),
		PINK_STAINED_CLAY(159, 6, "hardened_clay_stained_pink"),
		GREY_STAINED_CLAY(159, 7, "hardened_clay_stained_gray"),
		LIGHT_GREY_STAINED_CLAY(159, 8, "hardened_clay_stained_silver"),
		CYAN_STAINED_CLAY(159, 9, "hardened_clay_stained_cyan"),
		PURPLE_STAINED_CLAY(159, 10, "hardened_clay_stained_purple"),
		BLUE_STAINED_CLAY(159, 11, "hardened_clay_stained_blue"),
		BROWN_STAINED_CLAY(159, 12, "hardened_clay_stained_brown"),
		GREEN_STAINED_CLAY(159, 13, "hardened_clay_stained_green"),
		RED_STAINED_CLAY(159, 14, "hardened_clay_stained_red"),
		BLACK_STAINED_CLAY(159, 15, "hardened_clay_stained_black"),
		WHITE_STAINED_GLASS_PANE(160, 0, "glass_white"),
		ORANGE_STAINED_GLASS_PANE(160, 1, "glass_orange"),
		MAGENTA_STAINED_GLASS_PANE(160, 2, "glass_magenta"),
		LIGHT_BLUE_STAINED_GLASS_PANE(160, 3, "glass_blue"),
		YELLOW_STAINED_GLASS_PANE(160, 4, "glass_yellow"),
		LIME_STAINED_GLASS_PANE(160, 5, "glass_lime"),
		PINK_STAINED_GLASS_PANE(160, 6, "glass_pink"),
		GREY_STAINED_GLASS_PANE(160, 7, "glass_gray"),
		LIGHT_GREY_STAINED_GLASS_PANE(160, 8, "glass_silver"),
		CYAN_STAINED_GLASS_PANE(160, 9, "glass_cyan"),
		PURPLE_STAINED_GLASS_PANE(160, 10, "glass_purple"),
		BLUE_STAINED_GLASS_PANE(160, 11, "glass_blue"),
		BROWN_STAINED_GLASS_PANE(160, 12, "glass_brown"),
		GREEN_STAINED_GLASS_PANE(160, 13, "glass_green"),
		RED_STAINED_GLASS_PANE(160, 14, "glass_red"),
		BLACK_STAINED_GLASS_PANE(160, 15, "glass_black"),
		ACACIA_LEAVES(161, new int[]{0, 4, 8, 12}, "leaves_acacia", FoliageType.FOLIAGE),
		DARK_OAK_LEAVES(161, new int[]{1, 5, 9, 13}, "leaves_big_oak", FoliageType.FOLIAGE),
		ACACIA_WOOD(162, new int[]{0, 4, 8, 12}, "log_acacia_top"),
		DARK_OAK_WOOD(162, new int[]{1, 5, 9, 13}, "log_big_oak_top"),
		ACACIA_WOOD_STAIRS(163, "planks_acacia"),
		DARK_OAK_WOOD_STAIRS(164, "planks_big_oak"),
		SLIME_BLOCK(165, 0, "slime"),
		BARRIER(166, 0, 0x000000), //???
		IRON_TRAPDOOR(167, "iron_trapdoor"),
		PRISMARINE(168, 0, "prismarine_rough"),
		PRISMARINE_BRICKS(168, 1, "prismarine_bricks"),
		DARK_PRISMARINE(168, 2, "prismarine_dark"),
		SEA_LANTERN(169, 0, "sea_lantern"),
		HAY_BALE(170, 0, "hay_block_top"),
		HAY_BALE_NORTH(170, new int[]{1, 2}, "hay_block_side"),
		WHITE_CARPET(171, 0, "wool_colored_white"),
		ORANGE_CARPET(171, 1, "wool_colored_orange"),
		MAGENTA_CARPET(171, 2, "wool_colored_magenta"),
		LIGHT_BLUE_CARPET(171, 3, "wool_colored_blue"),
		YELLOW_CARPET(171, 4, "wool_colored_yellow"),
		LIME_CARPET(171, 5, "wool_colored_lime"),
		PINK_CARPET(171, 6, "wool_colored_pink"),
		GREY_CARPET(171, 7, "wool_colored_gray"),
		LIGHT_GREY_CARPET(171, 8, "wool_colored_silver"),
		CYAN_CARPET(171, 9, "wool_colored_cyan"),
		PURPLE_CARPET(171, 10, "wool_colored_purple"),
		BLUE_CARPET(171, 11, "wool_colored_blue"),
		BROWN_CARPET(171, 12, "wool_colored_brown"),
		GREEN_CARPET(171, 13, "wool_colored_green"),
		RED_CARPET(171, 14, "wool_colored_red"),
		BLACK_CARPET(171, 15, "wool_colored_black"),
		HARDENED_CLAY(172, 0, "hardened_clay"),
		COAL_BLOCK(173, 0, "coal_block"),
		PACKED_ICE(174, 0, "ice_packed"),
		SUNFLOWER(175, "double_plant_sunflower_front"),
		DOUBLE_TALL_GRASS(175, 8, "double_plant_grass_top", FoliageType.GRASS),
		WHITE_FREE_BANNER(176, 0, "wool_colored_white"),
		ORANGE_FREE_BANNER(176, 1, "wool_colored_orange"),
		MAGENTA_FREE_BANNER(176, 2, "wool_colored_magenta"),
		LIGHT_BLUE_FREE_BANNER(176, 3, "wool_colored_blue"),
		YELLOW_FREE_BANNER(176, 4, "wool_colored_yellow"),
		LIME_FREE_BANNER(176, 5, "wool_colored_lime"),
		PINK_FREE_BANNER(176, 6, "wool_colored_pink"),
		GREY_FREE_BANNER(176, 7, "wool_colored_gray"),
		LIGHT_GREY_FREE_BANNER(176, 8, "wool_colored_silver"),
		CYAN_FREE_BANNER(176, 9, "wool_colored_cyan"),
		PURPLE_FREE_BANNER(176, 10, "wool_colored_purple"),
		BLUE_FREE_BANNER(176, 11, "wool_colored_blue"),
		BROWN_FREE_BANNER(176, 12, "wool_colored_brown"),
		GREEN_FREE_BANNER(176, 13, "wool_colored_green"),
		RED_FREE_BANNER(176, 14, "wool_colored_red"),
		BLACK_FREE_BANNER(176, 15, "wool_colored_black"),
		WHITE_WALL_BANNER(177, 0, "wool_colored_white"),
		ORANGE_WALL_BANNER(177, 1, "wool_colored_orange"),
		MAGENTA_WALL_BANNER(177, 2, "wool_colored_magenta"),
		LIGHT_BLUE_WALL_BANNER(177, 3, "wool_colored_blue"),
		YELLOW_WALL_BANNER(177, 4, "wool_colored_yellow"),
		LIME_WALL_BANNER(177, 5, "wool_colored_lime"),
		PINK_WALL_BANNER(177, 6, "wool_colored_pink"),
		GREY_WALL_BANNER(177, 7, "wool_colored_gray"),
		LIGHT_GREY_WALL_BANNER(177, 8, "wool_colored_silver"),
		CYAN_WALL_BANNER(177, 9, "wool_colored_cyan"),
		PURPLE_WALL_BANNER(177, 10, "wool_colored_purple"),
		BLUE_WALL_BANNER(177, 11, "wool_colored_blue"),
		BROWN_WALL_BANNER(177, 12, "wool_colored_brown"),
		GREEN_WALL_BANNER(177, 13, "wool_colored_green"),
		RED_WALL_BANNER(177, 14, "wool_colored_red"),
		BLACK_WALL_BANNER(177, 15, "wool_colored_black"),
		DAYLIGHT_SENSOR_INVERTED(178, 0, "daylight_detector_inverted_top"),
		RED_SANDSTONE(179, new int[]{0, 1, 2}, "red_sandstone_top"),
		RED_SANDSTONE_STAIRS(180, "red_sandstone_top"),
		DOUBLE_RED_SANDSTONE_SLAB(181, 0, "red_sandstone_top"),
		RED_SANDSTONE_SLAB(182, new int[]{0, 1}, "red_sandstone_top"),
		SPRUCE_WOOD_FENCE_GATE(183, new int[]{0, 1, 2, 3, 4, 5, 6, 7}, "planks_spruce"),
		BIRCH_WOOD_FENCE_GATE(184, new int[]{0, 1, 2, 3, 4, 5, 6, 7}, "planks_birch"),
		JUNGLE_WOOD_FENCE_GATE(185, new int[]{0, 1, 2, 3, 4, 5, 6, 7}, "planks_jungle"),
		ACACIA_WOOD_FENCE_GATE(186, new int[]{0, 1, 2, 3, 4, 5, 6, 7}, "planks_acacia"),
		DARK_OAK_WOOD_FENCE_GATE(187, new int[]{0, 1, 2, 3, 4, 5, 6, 7}, "planks_big_oak"),
		SPRUCE_WOOD_FENCE(188, 0, "planks_spruce"),
		BIRCH_WOOD_FENCE(189, 0, "planks_birch"),
		JUNGLE_WOOD_FENCE(190, 0, "planks_jungle"),
		ACACIA_WOOD_FENCE(191, 0, "planks_acacia"),
		DARK_OAK_WOOD_FENCE(192, 0, "planks_big_oak"),
		SPRUCE_WOOD_DOOR(193, "door_spruce_upper"),
		BIRCH_WOOD_DOOR(194, "door_birch_upper"),
		JUNGLE_WOOD_DOOR(195, "door_jungle_upper"),
		ACACIA_WOOD_DOOR(196, "door_acacia_upper"),
		DARK_OAK_WOOD_DOOR(197, "door_dark_oak_upper"),
		END_ROD(198, new int[]{0, 1, 2, 3, 4, 5}, "end_rod"),
		CHORUS_PLANT(199, 0, "chorus_plant"),
		CHORUS_FLOWER(200, 0, "chorus_flower"),
		PURPUR_BLOCK(201, 0, "purpur_block"),
		PURPUR_PILLAR(202, 0, "purpur_pillar"),
		PURPUR_STAIRS(203, "purpur_block"),
		PURPUR_DOUBLE_SLAB(204, 0, "purpur_block"),
		PURPUR_SLAB(205, new int[]{0, 1}, "purpur_block"),
		END_BRICKS(206, 0, "end_bricks"),
		BEETROOT(207, new int[]{0, 1, 2, 3}, "beetroots_stage_3"),
		GRASS_PATH(208, 0, "grass_path_top"),
		END_GATEWAY(209, 0, 0x000000),
		REPEATING_COMMAND_BLOCK(210, new int[]{0, 1, 2, 3, 4, 5}, "repeating_command_block_back"),
		CHAIN_COMMAND_BLOCK(211, new int[]{0, 1, 2, 3, 4, 5}, "chain_command_block_back"),
		FROSTED_ICE(212, new int[]{0, 1, 2, 3}, "frosted_ice_0"),
		MAGMA_BLOCK(213, 0, "magma"),
		NETHER_WART_BLOCK(214, 0, "nether_wart_block"),
		RED_NETHER_BRICK(215, 0, "red_nether_brick"),
		BONE_BLOCK(216, 0, "bone_block_top"),
		BONE_BLOCK_SIDE(216, new int[]{1, 2}, "bone_block_side"),
		STRUCTURE_VOID(217, 0, 0x000000), //???
		OBSERVER(218, new int[]{0, 1, 2, 3, 4, 5}, "observer_front"),
		WHITE_SHULKER_BOX(219, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_white"),
		ORANGE_SHULKER_BOX(220, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_orange"),
		MAGENTA_SHULKER_BOX(221, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_magenta"),
		LIGHT_BLUE_SHULKER_BOX(222, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_blue"),
		YELLOW_SHULKER_BOX(223, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_yellow"),
		LIME_SHULKER_BOX(224, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_lime"),
		PINK_SHULKER_BOX(225, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_pink"),
		GREY_SHULKER_BOX(226, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_gray"),
		LIGHT_GREY_SHULKER_BOX(227, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_silver"),
		CYAN_SHULKER_BOX(228, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_cyan"),
		PURPLE_SHULKER_BOX(229, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_purple"),
		BLUE_SHULKER_BOX(230, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_blue"),
		BROWN_SHULKER_BOX(231, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_brown"),
		GREEN_SHULKER_BOX(232, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_green"),
		RED_SHULKER_BOX(233, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_red"),
		BLACK_SHULKER_BOX(234, new int[]{0, 1, 2, 3, 4, 5}, "shulker_top_black"),
		WHITE_GLAZED_TERRACOTTA(235, new int[]{0, 1, 2, 3}, "glazed_terracotta_white"),
		ORANGE_GLAZED_TERRACOTTA(236, new int[]{0, 1, 2, 3}, "glazed_terracotta_orange"),
		MAGENTA_GLAZED_TERRACOTTA(237, new int[]{0, 1, 2, 3}, "glazed_terracotta_magenta"),
		LIGHT_BLUE_GLAZED_TERRACOTTA(238, new int[]{0, 1, 2, 3}, "glazed_terracotta_blue"),
		YELLOW_GLAZED_TERRACOTTA(239, new int[]{0, 1, 2, 3}, "glazed_terracotta_yellow"),
		LIME_GLAZED_TERRACOTTA(240, new int[]{0, 1, 2, 3}, "glazed_terracotta_lime"),
		PINK_GLAZED_TERRACOTTA(241, new int[]{0, 1, 2, 3}, "glazed_terracotta_pink"),
		GREY_GLAZED_TERRACOTTA(242, new int[]{0, 1, 2, 3}, "glazed_terracotta_gray"),
		LIGHT_GREY_GLAZED_TERRACOTTA(243, new int[]{0, 1, 2, 3}, "glazed_terracotta_silver"),
		CYAN_GLAZED_TERRACOTTA(244, new int[]{0, 1, 2, 3}, "glazed_terracotta_cyan"),
		PURPLE_GLAZED_TERRACOTTA(245, new int[]{0, 1, 2, 3}, "glazed_terracotta_purple"),
		BLUE_GLAZED_TERRACOTTA(246, new int[]{0, 1, 2, 3}, "glazed_terracotta_blue"),
		BROWN_GLAZED_TERRACOTTA(247, new int[]{0, 1, 2, 3}, "glazed_terracotta_brown"),
		GREEN_GLAZED_TERRACOTTA(248, new int[]{0, 1, 2, 3}, "glazed_terracotta_green"),
		RED_GLAZED_TERRACOTTA(249, new int[]{0, 1, 2, 3}, "glazed_terracotta_red"),
		BLACK_GLAZED_TERRACOTTA(250, new int[]{0, 1, 2, 3}, "glazed_terracotta_black"),
		WHITE_CONCRETE(251, 0, "concrete_white"),
		ORANGE_CONCRETE(251, 1, "concrete_orange"),
		MAGENTA_CONCRETE(251, 2, "concrete_magenta"),
		LIGHT_BLUE_CONCRETE(251, 3, "concrete_blue"),
		YELLOW_CONCRETE(251, 4, "concrete_yellow"),
		LIME_CONCRETE(251, 5, "concrete_lime"),
		PINK_CONCRETE(251, 6, "concrete_pink"),
		GREY_CONCRETE(251, 7, "concrete_gray"),
		LIGHT_GREY_CONCRETE(251, 8, "concrete_silver"),
		CYAN_CONCRETE(251, 9, "concrete_cyan"),
		PURPLE_CONCRETE(251, 10, "concrete_purple"),
		BLUE_CONCRETE(251, 11, "concrete_blue"),
		BROWN_CONCRETE(251, 12, "concrete_brown"),
		GREEN_CONCRETE(251, 13, "concrete_green"),
		RED_CONCRETE(251, 14, "concrete_red"),
		BLACK_CONCRETE(251, 15, "concrete_black"),
		WHITE_CONCRETE_POWDER(252, 0, "concrete_powder_white"),
		ORANGE_CONCRETE_POWDER(252, 1, "concrete_powder_orange"),
		MAGENTA_CONCRETE_POWDER(252, 2, "concrete_powder_magenta"),
		LIGHT_BLUE_CONCRETE_POWDER(252, 3, "concrete_powder_blue"),
		YELLOW_CONCRETE_POWDER(252, 4, "concrete_powder_yellow"),
		LIME_CONCRETE_POWDER(252, 5, "concrete_powder_lime"),
		PINK_CONCRETE_POWDER(252, 6, "concrete_powder_pink"),
		GREY_CONCRETE_POWDER(252, 7, "concrete_powder_gray"),
		LIGHT_GREY_CONCRETE_POWDER(252, 8, "concrete_powder_silver"),
		CYAN_CONCRETE_POWDER(252, 9, "concrete_powder_cyan"),
		PURPLE_CONCRETE_POWDER(252, 10, "concrete_powder_purple"),
		BLUE_CONCRETE_POWDER(252, 11, "concrete_powder_blue"),
		BROWN_CONCRETE_POWDER(252, 12, "concrete_powder_brown"),
		GREEN_CONCRETE_POWDER(252, 13, "concrete_powder_green"),
		RED_CONCRETE_POWDER(252, 14, "concrete_powder_red"),
		BLACK_CONCRETE_POWDER(252, 15, "concrete_powder_black"),
		STRUCTURE_BLOCK(255, "structure_block");

		private int id;
		private int data;
		private int[] datas;
		private int rgb;
		private String texture;

		private static final int foliageColor = 0x1abf00;
		private static final int grassColor = 0x6b9793;

		private static Map<Integer, Mapping> map = new HashMap<>();

		static {
			for (Mapping m : Mapping.values()) {
				if (m.data == -1) {
					for (int i = 0; i < 16; i++) {
						map.put(m.idAndDataToInt(m.id, i), m);
						System.out.printf("%d;%d;%s\n", m.id, i, hexString(m.rgb));
					}
				} else if (m.datas != null) {
					for (int i = 0; i < m.datas.length; i++) {
						map.put(m.idAndDataToInt(m.id, m.datas[i]), m);
						System.out.printf("%d;%d;%s\n", m.id, m.datas[i], hexString(m.rgb));
					}
				} else {
					map.put(m.idAndDataToInt(), m);
					System.out.printf("%d;%d;%s\n", m.id, m.data, hexString(m.rgb));
				}
			}
		}

		private static String hexString(int c) {
			StringBuilder hex = new StringBuilder(Integer.toHexString(c));
			for (int i = hex.length(); i < 6; i++) {
				hex.insert(0, "0");
			}
			return hex.toString();
		}

		Mapping(int id, String texture, FoliageType foliage) {
			this.id = id;
			this.texture = texture;
			data = -1;
			loadRGB(foliage);
		}

		Mapping(int id, int[] data, String texture, FoliageType foliage) {
			this.id = id;
			this.datas = data;
			this.texture = texture;
			loadRGB(foliage);
		}

		Mapping(int id, int data, String texture, FoliageType foliage) {
			this.id = id;
			this.data = data;
			this.texture = texture;
			loadRGB(foliage);
		}

		Mapping(int id, String texture) {
			this.id = id;
			this.texture = texture;
			data = -1;
			loadRGB(null);
		}

		Mapping(int id, int[] data, String texture) {
			this.id = id;
			this.datas = data;
			this.texture = texture;
			loadRGB(null);
		}

		Mapping(int id, int data, String texture) {
			this.id = id;
			this.data = data;
			this.texture = texture;
			loadRGB(null);
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

		private short toByteColor(double d) {
			short c = (short) (d * 255);
//			System.out.println("byte color: " + c);
			return c;
		}

		private void loadRGB(FoliageType foliage) {
//			System.out.println("loadRGB");
			try (InputStream inputStream = new FileInputStream(new File(Helper.getWorkingDir(), "src/main/resources/1.12.2/assets/minecraft/textures/blocks/" +texture + ".png"))) {

				Image image = new Image(inputStream);
				PixelReader pr;

				if (foliage != null) {
					pr = image.getPixelReader();
					BufferedImage bi = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
					Graphics graphics = bi.getGraphics();
					int fc = foliage == FoliageType.FOLIAGE ? foliageColor : grassColor;
					for (int x = 0; x < image.getWidth(); x++) {
						for (int y = 0; y < image.getHeight(); y++) {
							Color c = pr.getColor(x, y);
//							System.out.printf("template color: %f %f %f\n", c.getRed(), c.getGreen(), c.getBlue());
//							System.out.println("foliageColor: " + Integer.toHexString(foliageColor));
							int nr = (fc >> 16) * toByteColor(c.getRed()) / 255;
							int ng = (fc >> 8 & 0x00FF) * toByteColor(c.getGreen()) / 255;
							int nb = (fc & 0x0000FF) * toByteColor(c.getBlue()) / 255;
							java.awt.Color nc = new java.awt.Color(nr, ng, nb, toByteColor(c.getOpacity()));
//							Color nc = new Color(nr / 256,  ng / 256, nb / 256, 1);
//							System.out.printf("FOLIAGE %d %d %d\n", nr, ng, nb);
							graphics.setColor(nc);
							graphics.fillRect(x, y, 1, 1);
						}
					}
					image = SwingFXUtils.toFXImage(bi, null);
					ImageIO.write(bi, "png", new File(Helper.getWorkingDir(), "src/main/resources/out/" + this + ".png"));
				}

				pr = image.getPixelReader();


				double r = 0, g = 0, b = 0;
				int c = 0;
				for (int x = 0; x < image.getWidth(); x++) {
					for (int y = 0; y < image.getHeight(); y++) {
						Color p = pr.getColor(x, y);
						if (p.getOpacity() != 0) {
							r += p.getRed();
							g += p.getGreen();
							b += p.getBlue();
							c++;
						}
					}
				}
				r /= c;
				g /= c;
				b /= c;
				int ir = (int) (r * 256d);
				int ig = (int) (g * 256d);
				int ib = (int) (b * 256d);
				rgb = (ir << 16) + (ig << 8) + ib;
//				System.out.printf("%s / %d %d %d / %s\n", this, ir, ig, ib, Integer.toHexString(rgb));
			} catch (IOException ex) {
				System.out.println("texture " + texture + " doesn't exist: " + ex.getMessage());
			}
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
				System.out.println("could not find mapping for " + (idAndData >> 4) + ":" + (idAndData & 0xF));
				return Mapping.DEFAULT;
			}
			return m;
		}
	}
}
