package osrs.dev.ui.viewport;

import osrs.dev.Main;
import osrs.dev.ui.ViewerMode;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static osrs.dev.ui.viewport.RenderConfig.*;

/**
 * Caches pre-rendered tile chunks at multiple LOD levels.
 * Each chunk represents CHUNK_SIZE x CHUNK_SIZE tiles.
 */
public class TileCache {
    private final Map<Long, CachedChunk> chunkCache = new ConcurrentHashMap<>();

    // Colors (can be updated from config)
    private int bgColor = DEFAULT_BG_COLOR;
    private int collisionColor = DEFAULT_COLLISION_COLOR;
    private int wallColor = DEFAULT_WALL_COLOR;

    /**
     * Updates colors from config manager.
     */
    public void updateColors() {
        if (Main.getConfigManager() != null) {
            bgColor = DirectPixelRenderer.colorToRGB(Main.getConfigManager().bgColor());
            collisionColor = DirectPixelRenderer.colorToRGB(Main.getConfigManager().collisionColor());
            wallColor = DirectPixelRenderer.colorToRGB(Main.getConfigManager().wallColor());
        }
    }

    /**
     * Gets or renders a chunk at the specified location and LOD.
     * @param chunkX Chunk X coordinate (world X / CHUNK_SIZE)
     * @param chunkY Chunk Y coordinate (world Y / CHUNK_SIZE)
     * @param plane World plane
     * @param lod Level of detail (0 = highest)
     * @param mode Viewer mode
     * @return Cached or newly rendered chunk image
     */
    public BufferedImage getChunk(int chunkX, int chunkY, int plane, int lod, ViewerMode mode) {
        long key = chunkKey(chunkX, chunkY, plane, lod, mode);

        CachedChunk cached = chunkCache.get(key);
        if (cached != null) {
            cached.lastAccess = System.currentTimeMillis();
            return cached.image;
        }

        // Render new chunk
        BufferedImage image = renderChunk(chunkX, chunkY, plane, lod, mode);
        chunkCache.put(key, new CachedChunk(image));

        // Evict old entries if cache is too large
        if (chunkCache.size() > CHUNK_CACHE_MAX_SIZE) {
            evictOldest();
        }

        return image;
    }

    /**
     * Renders a single chunk.
     */
    private BufferedImage renderChunk(int chunkX, int chunkY, int plane, int lod, ViewerMode mode) {
        int scale = getLODScale(lod);
        int chunkPixelSize = getChunkPixelSize(lod);

        BufferedImage image = new BufferedImage(chunkPixelSize, chunkPixelSize, BufferedImage.TYPE_INT_RGB);
        int[] pixels = DirectPixelRenderer.getPixels(image);

        // Fill with background color
        java.util.Arrays.fill(pixels, bgColor);

        int baseWorldX = chunkX * CHUNK_SIZE;
        int baseWorldY = chunkY * CHUNK_SIZE;

        // Pixels per tile in this chunk image
        int pixelsPerTile = chunkPixelSize / CHUNK_SIZE;
        if (pixelsPerTile < 1) pixelsPerTile = 1;

        // Render tiles - iterate by scale to handle LOD
        for (int ty = 0; ty < CHUNK_SIZE; ty += scale) {
            for (int tx = 0; tx < CHUNK_SIZE; tx += scale) {
                int worldX = baseWorldX + tx;
                int worldY = baseWorldY + ty;

                // Screen position within chunk image
                // In chunk image: (0,0) is top-left, Y increases downward
                // World coords: higher Y = north
                // ChunkRenderer positions chunk images such that:
                //   - World Y=chunkY*CHUNK_SIZE (south edge) appears at bottom of screen
                //   - World Y=chunkY*CHUNK_SIZE+CHUNK_SIZE (north edge) appears at top
                // So in chunk image: high world Y (north) → low image Y (top of image)
                int screenX = (tx / scale) * Math.max(1, pixelsPerTile);
                int screenY = ((CHUNK_SIZE - scale - ty) / scale) * Math.max(1, pixelsPerTile);

                int tilePixelSize = Math.max(1, pixelsPerTile);

                // Use LOD-aware rendering: shows black for collision when zoomed out,
                // red fill only when zoomed in (LOD 0)
                DirectPixelRenderer.renderTileWithLOD(
                        pixels, chunkPixelSize, chunkPixelSize,
                        screenX, screenY, tilePixelSize,
                        worldX, worldY, plane,
                        mode,
                        bgColor, collisionColor, wallColor,
                        lod
                );
            }
        }

        return image;
    }

    /**
     * Creates a unique key for a chunk.
     */
    private long chunkKey(int chunkX, int chunkY, int plane, int lod, ViewerMode mode) {
        // Pack: chunkX (16 bits) | chunkY (16 bits) | plane (4 bits) | lod (4 bits) | mode (4 bits)
        return ((long) (chunkX & 0xFFFF) << 28)
                | ((long) (chunkY & 0xFFFF) << 12)
                | ((plane & 0xF) << 8)
                | ((lod & 0xF) << 4)
                | (mode.ordinal() & 0xF);
    }

    /**
     * Invalidates all cached chunks.
     * Call when underlying data changes.
     */
    public void invalidate() {
        chunkCache.clear();
    }

    /**
     * Invalidates chunks for a specific plane.
     */
    public void invalidatePlane(int plane) {
        chunkCache.keySet().removeIf(key -> ((key >> 8) & 0xF) == plane);
    }

    /**
     * Evicts the oldest accessed chunks to stay under cache size limit.
     */
    private void evictOldest() {
        long now = System.currentTimeMillis();
        int toRemove = chunkCache.size() - CHUNK_CACHE_MAX_SIZE + 100; // Remove extra to avoid frequent eviction

        // Remove oldest entries
        chunkCache.entrySet().stream()
                .sorted((a, b) -> Long.compare(a.getValue().lastAccess, b.getValue().lastAccess))
                .limit(toRemove)
                .map(Map.Entry::getKey)
                .forEach(chunkCache::remove);
    }

    /**
     * Evicts chunks that haven't been accessed recently.
     */
    public void evictStale() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Long, CachedChunk>> it = chunkCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, CachedChunk> entry = it.next();
            if (now - entry.getValue().lastAccess > CHUNK_MAX_AGE_MS) {
                it.remove();
            }
        }
    }

    /**
     * Returns the current cache size.
     */
    public int getCacheSize() {
        return chunkCache.size();
    }

    /**
     * Internal class to track chunk access times.
     */
    private static class CachedChunk {
        final BufferedImage image;
        long lastAccess;

        CachedChunk(BufferedImage image) {
            this.image = image;
            this.lastAccess = System.currentTimeMillis();
        }
    }
}
