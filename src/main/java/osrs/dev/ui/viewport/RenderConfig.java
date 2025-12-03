package osrs.dev.ui.viewport;

/**
 * Configuration constants for the rendering system.
 */
public final class RenderConfig {
    private RenderConfig() {} // Prevent instantiation

    /**
     * Size of each cached chunk in tiles (64x64 tiles per chunk).
     */
    public static final int CHUNK_SIZE = 64;

    /**
     * Number of LOD (Level of Detail) levels.
     * LOD 0 = full detail with red fills and wall lines
     * LOD 1 = reduced detail (black fills, wall lines)
     * LOD 2 = minimal detail (black fills only, no wall lines)
     *
     * All LODs use full resolution chunks (64x64 pixels) for sharpness.
     * LOD only controls what details are rendered, not resolution.
     */
    public static final int LOD_LEVELS = 3;

    /**
     * Pixels per tile thresholds for LOD selection.
     * Higher LOD = less detail drawn (but same resolution).
     * LOD 0: >= 4 pixels per tile (zoomed in, full detail with red fills)
     * LOD 1: >= 1 pixel per tile (medium zoom, black fills + walls)
     * LOD 2: < 1 pixel per tile (zoomed out, black fills only)
     */
    public static final float[] LOD_THRESHOLDS = {4.0f, 1.0f, 0.0f};

    /**
     * Minimum pixels per tile to draw grid lines.
     */
    public static final float GRID_MIN_PIXELS_PER_TILE = 10.0f;

    /**
     * Minimum pixels per tile to draw wall details.
     */
    public static final float WALLS_MIN_PIXELS_PER_TILE = 3.0f;

    /**
     * Maximum age (ms) for cached chunks before eviction.
     */
    public static final long CHUNK_MAX_AGE_MS = 60_000;

    /**
     * Maximum number of chunks to keep in cache.
     */
    public static final int CHUNK_CACHE_MAX_SIZE = 1000;

    /**
     * Wall line thickness in pixels (for full detail rendering).
     */
    public static final int WALL_THICKNESS = 2;

    /**
     * Background color for empty/passable tiles (as RGB int).
     */
    public static final int DEFAULT_BG_COLOR = 0xF8F8F8;

    /**
     * Collision/blocked tile color (as RGB int).
     */
    public static final int DEFAULT_COLLISION_COLOR = 0xFF0000;

    /**
     * Wall color (as RGB int).
     */
    public static final int DEFAULT_WALL_COLOR = 0x000000;

    /**
     * Calculates the appropriate LOD level based on pixels per tile.
     * @param pixelsPerTile Current zoom level as pixels per tile
     * @return LOD level (0 = highest detail)
     */
    public static int calculateLOD(float pixelsPerTile) {
        for (int lod = 0; lod < LOD_THRESHOLDS.length; lod++) {
            if (pixelsPerTile >= LOD_THRESHOLDS[lod]) {
                return lod;
            }
        }
        return LOD_LEVELS - 1;
    }

    /**
     * Gets the scale factor for a given LOD level.
     * Always returns 1 - chunks are always rendered at full resolution
     * for maximum sharpness. LOD only controls detail level, not resolution.
     * @param lod Level of detail (unused, kept for API compatibility)
     * @return Always 1 (full resolution)
     */
    public static int getLODScale(int lod) {
        return 1; // Always full resolution for sharpness
    }

    /**
     * Gets the chunk pixel size for a given LOD level.
     * Always returns CHUNK_SIZE (64) for full resolution chunks.
     * @param lod Level of detail (unused, kept for API compatibility)
     * @return Always CHUNK_SIZE (64 pixels)
     */
    public static int getChunkPixelSize(int lod) {
        return CHUNK_SIZE; // Always full resolution
    }
}
