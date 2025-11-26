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


    public static ImmutableMap<Integer, Byte> SPRITE_ID_TO_TILE_TYPE = ImmutableMap.<Integer, Byte>builder()
            .put(1, WATER)
            .put(130, WATER)
            .put(131, WATER)
            .put(132, WATER)
            .put(133, WATER)
            .put(136, (TEMPOR_STORM_WATER))
            .put(137, (TEMPOR_STORM_WATER))
            .put(138, (TEMPOR_STORM_WATER))
            .put(139, (TEMPOR_STORM_WATER))
            .put(140, (SUNBAKED_WATER))
            .put(141, (SUNBAKED_WATER))
            .put(142, (SUNBAKED_WATER))
            .put(143, (SUNBAKED_WATER))
            .put(144, (SUNBAKED_WATER))
            .put(170, (SHARP_CRYSTAL_WATER))
            .put(171, (SHARP_CRYSTAL_WATER))
            .put(172, (SHARP_CRYSTAL_WATER))
            .put(173, (SHARP_CRYSTAL_WATER))
            .put(174, (SHARP_CRYSTAL_WATER))
            .put(175, (DISEASE_WATER))
            .put(176, (DISEASE_WATER))
            .put(177, (DISEASE_WATER))
            .put(178, (DISEASE_WATER))
            .put(179, (DISEASE_WATER))
            .put(180, (KELP_WATER))
            .put(181, (KELP_WATER))
            .put(182, (KELP_WATER))
            .put(183, (KELP_WATER))
            .put(184, (KELP_WATER))
            .put(185, (CRANDOR_SMEGMA_WATER))
            .put(186, (JAGGED_REEFS_WATER))
            .put(187, (JAGGED_REEFS_WATER))
            .put(188, (JAGGED_REEFS_WATER))
            .build();
}
