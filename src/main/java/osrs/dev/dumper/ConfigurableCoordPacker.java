package osrs.dev.dumper;

/**
 * Configurable coordinate packing with customizable bit layouts.
 * Allows different X/Y/plane bit widths for encoding coordinates into a single int.
 */
public class ConfigurableCoordPacker implements ICoordPacker {
    /**
     * Jagex format: 14 bits X, 14 bits Y, 2 bits plane
     * leaving 3 bits left over for data - enough for collision
     */
    public static final ConfigurableCoordPacker JAGEX_PACKING = new ConfigurableCoordPacker(
            16383, 0,   // xMask, xShift
            16383, 14,  // yMask, yShift
            0b11, 28     // planeMask, planeShift
    );
    /**
     * Compact format: 13 bits X, 13 bits Y, 2 bits plane,
     * leaving 5 bits left over for data - enough for tile types (and technically collision at the same time)
     */
    public static final ConfigurableCoordPacker COMPACT_13BIT_PACKING = new ConfigurableCoordPacker(
            8191, 0,   // xMask, xShift
            8191, 13,  // yMask, yShift
            0b11, 26     // planeMask, planeShift
    );
    /**
     * Compact format: 13 bits X, 13 bits Y, plane unsupported,
     * leaving enough bits for tile type data in a SparseBitSetTileTypeMap
     */
    public static final ConfigurableCoordPacker SPARSE_TILETYPE_MAP_PACKING_NO_PLANE = new ConfigurableCoordPacker(
                    8191, 0,   // xMask, xShift
                    8191, 13,  // yMask, yShift
                    0, 26     // planeMask, planeShift
            );

    private final int xMask;
    private final int xShift;
    private final int yMask;
    private final int yShift;
    private final int planeMask;
    private final int planeShift;

    /**
     * Creates a packing configuration with specified bit layouts.
     *
     * @param xMask      bitmask for X coordinate (e.g., 16383 for 14 bits)
     * @param xShift     bit position where X starts (typically 0)
     * @param yMask      bitmask for Y coordinate (e.g., 16383 for 14 bits)
     * @param yShift     bit position where Y starts (e.g., 14 if X uses 14 bits)
     * @param planeMask  bitmask for plane (e.g., 0b1111 for 4 bits)
     * @param planeShift bit position where plane starts (e.g., 28)
     */
    public ConfigurableCoordPacker(int xMask, int xShift, int yMask, int yShift, int planeMask, int planeShift) {
        this.xMask = xMask;
        this.xShift = xShift;
        this.yMask = yMask;
        this.yShift = yShift;
        this.planeMask = planeMask;
        this.planeShift = planeShift;
    }

    @Override
    public int pack(int x, int y, int plane) {
        return ((x & xMask) << xShift) | ((y & yMask) << yShift) | ((plane & planeMask) << planeShift);
    }

    @Override
    public int unpackX(int packed) {
        return (packed >> xShift) & xMask;
    }

    @Override
    public int unpackY(int packed) {
        return (packed >> yShift) & yMask;
    }

    @Override
    public int unpackPlane(int packed) {
        return (packed >> planeShift) & planeMask;
    }
}
