package osrs.dev.reader;

import com.google.common.collect.ImmutableMap;

/**
 * Constants for the different types of water tiles and their sprite ID mappings.
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

    /**
     * Maps overlay sprite/texture IDs to tile type constants.
     */
    public static final ImmutableMap<Integer, Byte> SPRITE_ID_TO_TILE_TYPE = ImmutableMap.<Integer, Byte>builder()
            // Standard water
            .put(1, WATER)
            .put(130, WATER)
            .put(131, WATER)
            .put(132, WATER)
            .put(133, WATER)
            // Tempoross storm water
            .put(136, TEMPOR_STORM_WATER)
            .put(137, TEMPOR_STORM_WATER)
            .put(138, TEMPOR_STORM_WATER)
            .put(139, TEMPOR_STORM_WATER)
            // Sunbaked water
            .put(140, SUNBAKED_WATER)
            .put(141, SUNBAKED_WATER)
            .put(142, SUNBAKED_WATER)
            .put(143, SUNBAKED_WATER)
            .put(144, SUNBAKED_WATER)
            // SE purple water
            .put(145, SE_PURPLE_WATER)
            .put(146, SE_PURPLE_WATER)
            .put(147, SE_PURPLE_WATER)
            .put(148, SE_PURPLE_WATER)
            .put(149, SE_PURPLE_WATER)
            // NE purple-gray water
            .put(155, NE_PURPLE_GRAY_WATER)
            .put(156, NE_PURPLE_GRAY_WATER)
            .put(157, NE_PURPLE_GRAY_WATER)
            .put(158, NE_PURPLE_GRAY_WATER)
            .put(159, NE_PURPLE_GRAY_WATER)
            // Ice water
            .put(160, ICE_WATER)
            .put(161, ICE_WATER)
            .put(163, ICE_WATER)
            .put(164, ICE_WATER)
            // NW gray water
            .put(165, NW_GRAY_WATER)
            .put(166, NW_GRAY_WATER)
            .put(167, NW_GRAY_WATER)
            .put(168, NW_GRAY_WATER)
            .put(169, NW_GRAY_WATER)
            // Sharp crystal water
            .put(170, SHARP_CRYSTAL_WATER)
            .put(171, SHARP_CRYSTAL_WATER)
            .put(172, SHARP_CRYSTAL_WATER)
            .put(173, SHARP_CRYSTAL_WATER)
            .put(174, SHARP_CRYSTAL_WATER)
            // Disease water
            .put(175, DISEASE_WATER)
            .put(176, DISEASE_WATER)
            .put(177, DISEASE_WATER)
            .put(178, DISEASE_WATER)
            .put(179, DISEASE_WATER)
            // Kelp water
            .put(180, KELP_WATER)
            .put(181, KELP_WATER)
            .put(182, KELP_WATER)
            .put(183, KELP_WATER)
            .put(184, KELP_WATER)
            // Crandor water
            .put(185, CRANDOR_SMEGMA_WATER)
            // Jagged reefs water
            .put(186, JAGGED_REEFS_WATER)
            .put(187, JAGGED_REEFS_WATER)
            .put(188, JAGGED_REEFS_WATER)
            .build();

    /**
     * Gets the display name for a tile type.
     * @param tileType the tile type byte
     * @return the display name, or null if unknown
     */
    public static String getName(byte tileType) {
        switch (tileType) {
            case WATER: return "Water";
            case CRANDOR_SMEGMA_WATER: return "Crandor Water";
            case TEMPOR_STORM_WATER: return "Tempoross Storm Water";
            case DISEASE_WATER: return "Disease Water";
            case KELP_WATER: return "Kelp Water";
            case SUNBAKED_WATER: return "Sunbaked Water";
            case JAGGED_REEFS_WATER: return "Jagged Reefs Water";
            case SHARP_CRYSTAL_WATER: return "Sharp Crystal Water";
            case ICE_WATER: return "Ice Water";
            case NE_PURPLE_GRAY_WATER: return "Purple-Gray Water";
            case NW_GRAY_WATER: return "Gray Water";
            case SE_PURPLE_WATER: return "Purple Water";
            default: return null;
        }
    }
}
