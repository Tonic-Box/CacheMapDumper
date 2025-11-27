package osrs.dev.dumper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfigurableCoordIndexer Tests")
class ConfigurableCoordIndexerTest {

    @Test
    @DisplayName("Constructor should calculate correct masks and shifts")
    void testConstructorCalculatesMasksAndShifts() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, false, 12, 0, 14, 0, 2, 0);

        assertEquals(4095, indexer.getXMask());      // (1 << 12) - 1
        assertEquals(0, indexer.getXShift());
        assertEquals(16383, indexer.getYMask());     // (1 << 14) - 1
        assertEquals(12, indexer.getYShift());       // xBits
        assertEquals(3, indexer.getPlaneMask());     // (1 << 2) - 1
        assertEquals(26, indexer.getPlaneShift());   // xBits + yBits
    }

    @Test
    @DisplayName("Constructor should calculate correct maxDataBitIndex")
    void testConstructorCalculatesMaxDataBitIndex() {
        // 32-bit capacity, 12+14+2=28 bits used, should have 4 data bits left
        ConfigurableCoordIndexer indexer32 = new ConfigurableCoordIndexer(32, false, 12, 0, 14, 0, 2, 0);
        assertEquals(4, indexer32.getMaxDataBitIndex());

        // 31-bit capacity (signed), 12+14+2=28 bits used, should have 3 data bits left
        ConfigurableCoordIndexer indexer31 = new ConfigurableCoordIndexer(31, false, 12, 0, 14, 0, 2, 0);
        assertEquals(3, indexer31.getMaxDataBitIndex());
    }

    @Test
    @DisplayName("Constructor should throw when bits exceed capacity")
    void testConstructorThrowsWhenExceedsCapacity() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            new ConfigurableCoordIndexer(32, false, 15, 0, 15, 0, 4, 0)
        );
        assertTrue(exception.getMessage().contains("exceeds maximum capacity"));
    }

    @Test
    @DisplayName("Constructor should set correct min/max coordinate bounds")
    void testConstructorSetsCoordinateBounds() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, false, 12, 960, 14, 1000, 2, 0);

        assertEquals(960, indexer.getMinX());
        assertEquals(960 + 4095, indexer.getMaxX());
        assertEquals(1000, indexer.getMinY());
        assertEquals(1000 + 16383, indexer.getMaxY());
        assertEquals(0, indexer.getMinPlane());
        assertEquals(3, indexer.getMaxPlane());
    }

    @Test
    @DisplayName("packToBitmapIndex with validation enabled should validate X coordinate")
    void testPackToBitmapIndexValidatesX() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, true, 12, 960, 14, 0, 2, 0);

        // Valid coordinate
        assertDoesNotThrow(() -> indexer.packToBitmapIndex(960, 0, 0, 0));

        // X below minimum
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () ->
            indexer.packToBitmapIndex(959, 0, 0, 0)
        );
        assertTrue(exception1.getMessage().contains("X coordinate"));

        // X above maximum
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () ->
            indexer.packToBitmapIndex(960 + 4096, 0, 0, 0)
        );
        assertTrue(exception2.getMessage().contains("X coordinate"));
    }

    @Test
    @DisplayName("packToBitmapIndex with validation enabled should validate Y coordinate")
    void testPackToBitmapIndexValidatesY() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, true, 12, 0, 14, 100, 2, 0);

        // Valid coordinate
        assertDoesNotThrow(() -> indexer.packToBitmapIndex(0, 100, 0, 0));

        // Y below minimum
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () ->
            indexer.packToBitmapIndex(0, 99, 0, 0)
        );
        assertTrue(exception1.getMessage().contains("Y coordinate"));

        // Y above maximum
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () ->
            indexer.packToBitmapIndex(0, 100 + 16384, 0, 0)
        );
        assertTrue(exception2.getMessage().contains("Y coordinate"));
    }

    @Test
    @DisplayName("packToBitmapIndex with validation enabled should validate plane coordinate")
    void testPackToBitmapIndexValidatesPlane() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, true, 12, 0, 14, 0, 2, 0);

        // Valid plane
        assertDoesNotThrow(() -> indexer.packToBitmapIndex(0, 0, 0, 0));
        assertDoesNotThrow(() -> indexer.packToBitmapIndex(0, 0, 3, 0));

        // Plane below minimum
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () ->
            indexer.packToBitmapIndex(0, 0, -1, 0)
        );
        assertTrue(exception1.getMessage().contains("Plane coordinate"));

        // Plane above maximum
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () ->
            indexer.packToBitmapIndex(0, 0, 4, 0)
        );
        assertTrue(exception2.getMessage().contains("Plane coordinate"));
    }

    @Test
    @DisplayName("packToBitmapIndex with validation enabled should validate data bit position")
    void testPackToBitmapIndexValidatesDataBitPosition() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, true, 12, 0, 14, 0, 2, 0);

        // Valid data bit positions (0 to 4)
        assertDoesNotThrow(() -> indexer.packToBitmapIndex(0, 0, 0, 0));
        assertDoesNotThrow(() -> indexer.packToBitmapIndex(0, 0, 0, 4));

        // Data bit below minimum
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () ->
            indexer.packToBitmapIndex(0, 0, 0, -1)
        );
        assertTrue(exception1.getMessage().contains("Data bit"));

        // Data bit above maximum
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () ->
            indexer.packToBitmapIndex(0, 0, 0, 5)
        );
        assertTrue(exception2.getMessage().contains("Data bit"));
    }

    @Test
    @DisplayName("packToBitmapIndex without validation should not throw")
    void testPackToBitmapIndexWithoutValidation() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, false, 12, 0, 14, 0, 2, 0);

        // Should not throw even with invalid values when validation disabled
        assertDoesNotThrow(() -> indexer.packToBitmapIndex(-1, -1, -1, -1));
        assertDoesNotThrow(() -> indexer.packToBitmapIndex(10000, 10000, 10, 10));
    }

    @Test
    @DisplayName("packToBitmapIndex should correctly pack coordinates")
    void testPackToBitmapIndexPacksCorrectly() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, false, 4, 0, 4, 0, 2, 0);
        // Layout: [SPARE(22)][PLANE(2)][Y(4)][X(4)]
        // X=0b1111 (15), Y=0b0101 (5), PLANE=0b11 (3)
        // Expected: (3 << 8) | (5 << 4) | 15 = 0b00000011_01011111 = 0x35F = 863

        int packed = indexer.packToBitmapIndex(15, 5, 3, 0);
        assertEquals(863, packed);
    }

    @Test
    @DisplayName("packToBitmapIndex should set correct data bit for position 0")
    void testPackToBitmapIndexDataBitPosition0() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, false, 4, 0, 4, 0, 2, 0);

        // Data bit 0 should return 0
        int result = indexer.packToBitmapIndex(0b1111, 0b0101, 0b11, 0);
        assertEquals(0b00000011_01011111, result);
    }

    @Test
    @DisplayName("packToBitmapIndex should set correct data bit for higher positions")
    void testPackToBitmapIndexDataBitPositions() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(16, false, 4, 0, 4, 0, 2, 0);
        // 16-bit capacity: 4+4+2=10 bits used, 6 data bits available
        // Coordinate value: 0b00000011_01011111 (10 bits used)

        int coord = indexer.packToBitmapIndex(0b1111, 0b0101, 0b11, 0);
        int dataIdx0 = indexer.packToBitmapIndex(0b1111, 0b0101, 0b11, 0);
        int dataIdx1 = indexer.packToBitmapIndex(0b1111, 0b0101, 0b11, 1);

        assertEquals(coord, dataIdx0);
        assertEquals(coord | (1 << 10), dataIdx1);
    }

    @Test
    @DisplayName("withValidationEnabled should return same instance if already enabled")
    void testWithValidationEnabledReturnsSameInstanceIfAlreadyEnabled() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, true, 12, 0, 14, 0, 2, 0);
        ConfigurableCoordIndexer result = indexer.withValidationEnabled();

        assertSame(indexer, result);
    }

    @Test
    @DisplayName("withValidationEnabled should create new instance if disabled")
    void testWithValidationEnabledCreatesNewInstanceIfDisabled() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, false, 12, 0, 14, 0, 2, 0);
        ConfigurableCoordIndexer result = indexer.withValidationEnabled();

        assertNotSame(indexer, result);
        assertTrue(result.isAdditionalValidationEnabled());
        assertFalse(indexer.isAdditionalValidationEnabled());
    }

    @Test
    @DisplayName("withValidationDisabled should return same instance if already disabled")
    void testWithValidationDisabledReturnsSameInstanceIfAlreadyDisabled() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, false, 12, 0, 14, 0, 2, 0);
        ConfigurableCoordIndexer result = indexer.withValidationDisabled();

        assertSame(indexer, result);
    }

    @Test
    @DisplayName("withValidationDisabled should create new instance if enabled")
    void testWithValidationDisabledCreatesNewInstanceIfEnabled() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, true, 12, 0, 14, 0, 2, 0);
        ConfigurableCoordIndexer result = indexer.withValidationDisabled();

        assertNotSame(indexer, result);
        assertFalse(result.isAdditionalValidationEnabled());
        assertTrue(indexer.isAdditionalValidationEnabled());
    }

    @Test
    @DisplayName("withMaxBits should return same instance if capacity unchanged")
    void testWithMaxBitsReturnsSameInstanceIfUnchanged() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, false, 12, 0, 14, 0, 2, 0);
        ConfigurableCoordIndexer result = indexer.withMaxBits(32);

        assertSame(indexer, result);
    }

    @Test
    @DisplayName("withMaxBits should create new instance with different capacity")
    void testWithMaxBitsCreatesNewInstanceWithDifferentCapacity() {
        ConfigurableCoordIndexer indexer32 = new ConfigurableCoordIndexer(32, false, 12, 0, 14, 0, 2, 0);
        ConfigurableCoordIndexer indexer31 = indexer32.withMaxBits(31);

        assertNotSame(indexer32, indexer31);
        assertEquals(32, indexer32.getMaxBitCapacity());
        assertEquals(31, indexer31.getMaxBitCapacity());
        assertEquals(4, indexer32.getMaxDataBitIndex());
        assertEquals(3, indexer31.getMaxDataBitIndex());
    }

    @Test
    @DisplayName("Builder should create correct indexer")
    void testBuilderCreatesCorrectIndexer() {
        ConfigurableCoordIndexer indexer = ConfigurableCoordIndexer.builder()
                .maxBits(32)
                .xBits(12)
                .xBase(960)
                .yBits(14)
                .yBase(1000)
                .planeBits(2)
                .build();

        assertEquals(32, indexer.getMaxBitCapacity());
        assertEquals(960, indexer.getMinX());
        assertEquals(1000, indexer.getMinY());
        assertEquals(4, indexer.getMaxDataBitIndex());
    }

    @Test
    @DisplayName("Builder useAdditionalValidation should enable validation")
    void testBuilderEnablesValidation() {
        ConfigurableCoordIndexer indexer = ConfigurableCoordIndexer.builder()
                .maxBits(32)
                .xBits(12)
                .yBits(14)
                .planeBits(2)
                .useAdditionalValidation()
                .build();

        assertTrue(indexer.isAdditionalValidationEnabled());
    }

    @Test
    @DisplayName("ROARINGBITMAP_5BIT_DATA_COORD_INDEXER should have 5 data bits")
    void testOptimizedPackingRoaringBitmap() {
        ConfigurableCoordIndexer indexer = ConfigurableCoordIndexer.ROARINGBITMAP_5BIT_DATA_COORD_INDEXER;

        assertEquals(32, indexer.getMaxBitCapacity());
        assertEquals(4, indexer.getMaxDataBitIndex());
    }

    @Test
    @DisplayName("SPARSEBITSET_4BIT_DATA_COORD_INDEXER should have 4 data bits")
    void testOptimizedPackingSparsebitset() {
        ConfigurableCoordIndexer indexer = ConfigurableCoordIndexer.SPARSEBITSET_4BIT_DATA_COORD_INDEXER;

        assertEquals(31, indexer.getMaxBitCapacity());
        assertEquals(3, indexer.getMaxDataBitIndex());
    }

    @Test
    @DisplayName("Chained copy methods should work correctly")
    void testChainedCopyMethods() {
        ConfigurableCoordIndexer original = new ConfigurableCoordIndexer(32, false, 12, 0, 14, 0, 2, 0);

        ConfigurableCoordIndexer modified = original
                .withValidationEnabled()
                .withMaxBits(31);

        assertTrue(modified.isAdditionalValidationEnabled());
        assertEquals(31, modified.getMaxBitCapacity());
        assertEquals(3, modified.getMaxDataBitIndex());

        assertFalse(original.isAdditionalValidationEnabled());
        assertEquals(32, original.getMaxBitCapacity());
    }

    @Test
    @DisplayName("Coordinate with base offset should pack correctly")
    void testCoordinateWithBaseOffset() {
        ConfigurableCoordIndexer indexer = new ConfigurableCoordIndexer(32, false, 8, 100, 8, 200, 2, 0);

        // Pack coordinate at base
        int result = indexer.packToBitmapIndex(100, 200, 0, 0);
        assertEquals(0, result); // (0 << 8) | (0 << 8) | 0 = 0

        // Pack coordinate one unit above base
        int result2 = indexer.packToBitmapIndex(101, 201, 0, 0);
        assertEquals((1 << 8) | 1, result2); // Y offset 1 at shift 8, X offset 1 at shift 0
    }

}
