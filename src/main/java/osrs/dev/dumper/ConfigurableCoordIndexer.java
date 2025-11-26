package osrs.dev.dumper;

import lombok.Getter;

/**
 * Configurable coordinate packing with customizable bit layouts.
 * Allows different X/Y/plane bit widths for encoding coordinates into a single int.
 */
public class ConfigurableCoordIndexer implements ICoordIndexer {
    /*
=== COORDINATE BOUNDS ===
X range: 960 to 4223 (span: 3264)
Y range: 1216 to 12607 (span: 11392)
Z range: 0 to 3 (span: 4)
*/

    /**
     * Optimized full packing: 13 bits X, 14 bits Y, 2 bits plane
     * using minimal amount of bits that can pack the full coordinate ranges of the game world.
     * Can index up to 4 bits of data per coordinate (RoaringBitmap with 32 bit indexing supported).
     */
    public static final ConfigurableCoordIndexer ROARINGBITMAP_4BIT_DATA_COORD_INDEXER = new Builder()
            .maxBits(32)
            .xBits(13)
            .xBase(700)
            .yBits(14) // No Y base needed as the span fits into 14 bits regardless
            .planeBits(2)
            .build();

    /**
     * Optimized full packing: 13 bits X, 14 bits Y, 2 bits plane
     * using minimal amount of bits that can pack the full coordinate ranges of the game world.
     * Can index up to 3 bits of data per coordinate for unsigned int (SparseBitSet with only 31 bit indexing supported).
     */
    public static final ConfigurableCoordIndexer SPARSEBITSET_3BIT_DATA_COORD_INDEXER = new Builder()
            .maxBits(31)
            .xBits(13)
            .xBase(700)
            .yBits(14) // No Y base needed as the span fits into 14 bits regardless
            .planeBits(2)
            .build();

    /**
     * 13 bits X, 14 bits Y, 1 bits plane
     * by leaving out 1 bit for plane (only supports planes 0 and 1).
     * we index up to 4 bits of data per coordinate for unsigned int (SparseBitSet with only 31 bit indexing supported).
     */
    public static final ConfigurableCoordIndexer SPARSEBITSET_4BIT_DATA_COORD_INDEXER_PLANES01 = new Builder()
            .maxBits(31)
            .xBits(13)
            .xBase(700)
            .yBits(14) // No Y base needed as the span fits into 14 bits regardless
            .planeBits(1)
            .build();


    @Getter
    private final int xMask;
    @Getter
    private final int xShift;
    @Getter
    private final int xBase;
    @Getter
    private final int yMask;
    @Getter
    private final int yShift;
    @Getter
    private final int yBase;
    @Getter
    private final int planeMask;
    @Getter
    private final int planeShift;
    @Getter
    private final int planeBase;
    /**
     * -- GETTER --
     * Gets the number of free data bits available in the packed integer
     * after accounting for bits taken for X, Y, and plane coordinates.
     */
    @Getter
    private final int maxDataBitIndex;
    @Getter
    private final int minX;
    @Getter
    private final int maxX;
    @Getter
    private final int minY;
    @Getter
    private final int maxY;
    @Getter
    private final int minPlane;
    @Getter
    private final int maxPlane;
    @Getter
    private final int totalCoordBits;

    /**
     * -- GETTER --
     * Maximum bit capacity for the packed integer (typically 32 for int, 64 for long).
     */
    @Getter
    private final int maxBitCapacity;

    @Getter
    private final boolean isAdditionalValidationEnabled;

    /**
     * Creates a packing configuration with automatic mask and shift calculation.
     * Bit layout (LSB to MSB): X, Y, PLANE, SPARE_BITS
     *
     * @param maxBitCapacity                maximum bit capacity (e.g., 32 for int, 31 for signed int)
     * @param isAdditionalValidationEnabled whether to validate inputs
     * @param xBits                         number of bits for X coordinate
     * @param xBase                         base offset for X coordinate
     * @param yBits                         number of bits for Y coordinate
     * @param yBase                         base offset for Y coordinate
     * @param planeBits                     number of bits for plane/level coordinate
     * @param planeBase                     base offset for plane coordinate
     */
    public ConfigurableCoordIndexer(int maxBitCapacity, boolean isAdditionalValidationEnabled,
                                    int xBits, int xBase, int yBits, int yBase,
                                    int planeBits, int planeBase) {
        this.totalCoordBits = xBits + yBits + planeBits;
        if (this.totalCoordBits > maxBitCapacity) {
            throw new IllegalArgumentException(
                    String.format("Bit packing layout uses %d bits (X:%d + Y:%d + PLANE:%d), which exceeds maximum capacity of %d bits",
                            this.totalCoordBits, xBits, yBits, planeBits, maxBitCapacity)
            );
        }

        // Compute masks and shifts based on bit widths
        // Layout: [SPARE][PLANE][Y][X]
        this.xMask = (1 << xBits) - 1;
        this.xShift = 0;
        this.xBase = xBase;

        this.yMask = (1 << yBits) - 1;
        this.yShift = xBits;
        this.yBase = yBase;

        this.planeMask = (1 << planeBits) - 1;
        this.planeShift = xBits + yBits;
        this.planeBase = planeBase;

        this.isAdditionalValidationEnabled = isAdditionalValidationEnabled;
        this.maxBitCapacity = maxBitCapacity;

        this.maxDataBitIndex = maxBitCapacity - this.totalCoordBits;
        this.minX = xBase;
        this.maxX = xBase + xMask;
        this.minY = yBase;
        this.maxY = yBase + yMask;
        this.minPlane = planeBase;
        this.maxPlane = planeBase + planeMask;
    }

    @Override
    public int packToBitmapIndex(int x, int y, int plane, int dataBitPosition) {
        return packCoordinate(x, y, plane) | packDataBitOffset(dataBitPosition);
    }

    private int packCoordinate(int x, int y, int plane) {
        return packX(x) | packY(y) | packPlane(plane);
    }

    private int packDataBitOffset(int dataBitPosition) {
        if (isAdditionalValidationEnabled) {
            if (dataBitPosition < 0 || dataBitPosition > maxDataBitIndex) {
                throw new IllegalArgumentException(
                        String.format("Data bit %d is outside available range [0, %d]", dataBitPosition, maxDataBitIndex)
                );
            }
        }

        if (dataBitPosition == 0) return 0;
        int indexMarkerBit = 1 << (totalCoordBits + dataBitPosition - 1);
        // Returns an additional flag bit that's to be set to index data in a position past 0
        return indexMarkerBit;
    }

    private int packX(int x) {
        if (isAdditionalValidationEnabled) {
            validatePackableX(x);
        }
        int xOffset = x - xBase;
        return (xOffset & xMask) << xShift;
    }

    private int packY(int y) {
        if (isAdditionalValidationEnabled) {
            validatePackableY(y);
        }
        int yOffset = y - yBase;
        return (yOffset & yMask) << yShift;
    }

    private int packPlane(int plane) {
        if (isAdditionalValidationEnabled) {
            validatePackablePlane(plane);
        }
        int planeOffset = plane - planeBase;
        return (planeOffset & planeMask) << planeShift;
    }

    private void validatePackableX(int x) {
        if (x < minX || x > maxX) {
            throw new IllegalArgumentException(
                    String.format("X coordinate %d is outside valid range [%d, %d]", x, minX, maxX)
            );
        }
    }

    private void validatePackableY(int y) {
        if (y < minY || y > maxY) {
            throw new IllegalArgumentException(
                    String.format("Y coordinate %d is outside valid range [%d, %d]", y, minY, maxY)
            );
        }
    }

    private void validatePackablePlane(int plane) {
        if (plane < minPlane || plane > maxPlane) {
            throw new IllegalArgumentException(
                    String.format("Plane coordinate %d is outside valid range [%d, %d]", plane, minPlane, maxPlane)
            );
        }
    }

    /**
     * Creates a copy of this packer with validation enabled.
     */
    public ConfigurableCoordIndexer withValidationEnabled() {
        if (isAdditionalValidationEnabled) {
            return this;
        }
        int xBits = Integer.bitCount(xMask);
        int yBits = Integer.bitCount(yMask);
        int planeBits = Integer.bitCount(planeMask);
        return new ConfigurableCoordIndexer(maxBitCapacity, true, xBits, xBase, yBits, yBase, planeBits, planeBase);
    }

    /**
     * Creates a copy of this packer with validation disabled.
     */
    public ConfigurableCoordIndexer withValidationDisabled() {
        if (!isAdditionalValidationEnabled) {
            return this;
        }
        int xBits = Integer.bitCount(xMask);
        int yBits = Integer.bitCount(yMask);
        int planeBits = Integer.bitCount(planeMask);
        return new ConfigurableCoordIndexer(maxBitCapacity, false, xBits, xBase, yBits, yBase, planeBits, planeBase);
    }

    /**
     * Creates a copy of this packer with a different maximum bit capacity.
     *
     * @param newMaxBitCapacity the new maximum bit capacity (e.g., 32 for int, 64 for long)
     * @return a new ConfigurableCoordIndexer with the specified capacity
     */
    public ConfigurableCoordIndexer withMaxBits(int newMaxBitCapacity) {
        if (this.maxBitCapacity == newMaxBitCapacity) {
            return this;
        }
        int xBits = Integer.bitCount(xMask);
        int yBits = Integer.bitCount(yMask);
        int planeBits = Integer.bitCount(planeMask);
        return new ConfigurableCoordIndexer(newMaxBitCapacity, isAdditionalValidationEnabled, xBits, xBase, yBits, yBase, planeBits, planeBase);
    }

    /**
     * Creates a new builder for ConfigurableCoordPacker with fluent API.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for creating ConfigurableCoordPacker instances.
     */
    public static class Builder {
        private int xBits = 0;
        private int yBits = 0;
        private int planeBits = 0;
        private int xBase = 0;
        private int yBase = 0;
        private int planeBase = 0;
        private boolean isAdditionalValidationEnabled = false;
        private int maxBitCapacity = 32;

        /**
         * Sets the number of bits for X coordinate.
         */
        public Builder xBits(int bits) {
            this.xBits = bits;
            return this;
        }

        /**
         * Sets the number of bits for Y coordinate.
         */
        public Builder yBits(int bits) {
            this.yBits = bits;
            return this;
        }

        /**
         * Sets the number of bits for plane coordinate.
         */
        public Builder planeBits(int bits) {
            this.planeBits = bits;
            return this;
        }

        /**
         * Sets the base value for X coordinate.
         */
        public Builder xBase(int base) {
            this.xBase = base;
            return this;
        }

        /**
         * Sets the base value for Y coordinate.
         */
        public Builder yBase(int base) {
            this.yBase = base;
            return this;
        }

        /**
         * Sets the base value for plane coordinate.
         */
        public Builder planeBase(int base) {
            this.planeBase = base;
            return this;
        }

        /**
         * Sets the maximum bit capacity for the packed integer (default: 32).
         * Use 64 for long integers, 16 for short integers, etc.
         */
        public Builder maxBits(int capacity) {
            this.maxBitCapacity = capacity;
            return this;
        }

        /**
         * Sets whether additional validation should be performed when packing/unpacking coordinates.
         */
        public Builder useAdditionalValidation() {
            this.isAdditionalValidationEnabled = true;
            return this;
        }

        /**
         * Builds the ConfigurableCoordIndexer with the configured parameters.
         */
        public ConfigurableCoordIndexer build() {
            return new ConfigurableCoordIndexer(maxBitCapacity, isAdditionalValidationEnabled,
                    xBits, xBase, yBits, yBase, planeBits, planeBase);
        }
    }
}
