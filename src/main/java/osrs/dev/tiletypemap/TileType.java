package osrs.dev.tiletypemap;

import com.google.common.collect.ImmutableMap;

/**
 * Flags for the different types of water tiles.
 */
public class TileType {
    public static final byte WATER = 1;
    public static final byte CRANDOR_SMEGMA_WATER = 2;
    public static final byte TEMPOR_STORM_WATER = 3;
    public static final byte DISEASE_WATER = 4;
    public static final byte KELP_WATER = 5;
    public static final byte SUNBAKED_WATER = 6;
    public static final byte JAGGED_REEFS_WATER = 7;
    public static final byte SHARP_CRYSTAL_WATER = 8;
    public static final byte ICE_WATER = 9;
    public static final byte NE_PURPLE_GRAY_WATER = 10;
    public static final byte NW_GRAY_WATER = 11;
    public static final byte SE_PURPLE_WATER = 12;

    public static ImmutableMap<Integer, Byte> SPRITE_ID_TO_TILE_TYPE = ImmutableMap.<Integer, Byte>builder()
            .put(1, WATER)
            .put(130, WATER)
            .put(131, WATER)
            .put(132, WATER)
            .put(133, WATER)
            .put(136, TEMPOR_STORM_WATER)
            .put(137, TEMPOR_STORM_WATER)
            .put(138, TEMPOR_STORM_WATER)
            .put(139, TEMPOR_STORM_WATER)
            .put(140, SUNBAKED_WATER)
            .put(141, SUNBAKED_WATER)
            .put(142, SUNBAKED_WATER)
            .put(143, SUNBAKED_WATER)
            .put(144, SUNBAKED_WATER)
            .put(145, SE_PURPLE_WATER)
            .put(146, SE_PURPLE_WATER)
            .put(147, SE_PURPLE_WATER)
            .put(148, SE_PURPLE_WATER)
            .put(149, SE_PURPLE_WATER)
            .put(155, NE_PURPLE_GRAY_WATER)
            .put(156, NE_PURPLE_GRAY_WATER)
            .put(157, NE_PURPLE_GRAY_WATER)
            .put(158, NE_PURPLE_GRAY_WATER)
            .put(159, NE_PURPLE_GRAY_WATER)
            .put(160, ICE_WATER)
            .put(161, ICE_WATER)
            .put(163, ICE_WATER)
            .put(164, ICE_WATER)
            .put(165, NW_GRAY_WATER)
            .put(166, NW_GRAY_WATER)
            .put(167, NW_GRAY_WATER)
            .put(168, NW_GRAY_WATER)
            .put(169, NW_GRAY_WATER)
            .put(170, SHARP_CRYSTAL_WATER)
            .put(171, SHARP_CRYSTAL_WATER)
            .put(172, SHARP_CRYSTAL_WATER)
            .put(173, SHARP_CRYSTAL_WATER)
            .put(174, SHARP_CRYSTAL_WATER)
            .put(175, DISEASE_WATER)
            .put(176, DISEASE_WATER)
            .put(177, DISEASE_WATER)
            .put(178, DISEASE_WATER)
            .put(179, DISEASE_WATER)
            .put(180, KELP_WATER)
            .put(181, KELP_WATER)
            .put(182, KELP_WATER)
            .put(183, KELP_WATER)
            .put(184, KELP_WATER)
            .put(185, CRANDOR_SMEGMA_WATER)
            .put(186, JAGGED_REEFS_WATER)
            .put(187, JAGGED_REEFS_WATER)
            .put(188, JAGGED_REEFS_WATER)
            .build();
}
