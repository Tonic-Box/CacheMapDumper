package osrs.dev.ui.viewport;

import osrs.dev.ui.ViewerMode;
import osrs.dev.util.WorldPoint;

import java.awt.*;
import java.awt.image.BufferedImage;

import static osrs.dev.ui.viewport.RenderConfig.*;

/**
 * Renders the map by compositing cached tile chunks.
 * Handles LOD selection and chunk positioning.
 */
public class ChunkRenderer {
    private final TileCache cache = new TileCache();

    /**
     * Updates colors from config (call when settings change).
     */
    public void updateColors() {
        cache.updateColors();
        cache.invalidate(); // Re-render chunks with new colors
    }

    /**
     * Invalidates all cached chunks (call when data changes).
     */
    public void invalidateCache() {
        cache.invalidate();
    }

    /**
     * Gets the tile cache for direct access if needed.
     */
    public TileCache getCache() {
        return cache;
    }

    /**
     * Renders visible chunks onto the canvas.
     *
     * @param canvas      Target BufferedImage
     * @param base        Bottom-left world coordinate of visible area
     * @param visibleTiles Number of tiles visible (width/height)
     * @param plane       Current plane
     * @param mode        Viewer mode
     */
    public void render(BufferedImage canvas, WorldPoint base, int visibleTiles, int plane, ViewerMode mode) {
        Graphics2D g2d = canvas.createGraphics();

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        // Calculate pixels per tile for both dimensions (canvas may not be square)
        float pixelsPerTileX = (float) canvasWidth / visibleTiles;
        float pixelsPerTileY = (float) canvasHeight / visibleTiles;

        // Use minimum for LOD calculation
        float minPixelsPerTile = Math.min(pixelsPerTileX, pixelsPerTileY);

        // Use nearest-neighbor when zoomed in (scaling up) to avoid blur
        // Use bilinear when zoomed out (scaling down) for smoother appearance
        if (minPixelsPerTile >= 1.0f) {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        } else {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }

        // Determine LOD level based on minimum pixels per tile
        int lod = calculateLOD(minPixelsPerTile);
        int lodScale = getLODScale(lod);
        int chunkPixelSize = getChunkPixelSize(lod);

        // Calculate visible chunk range
        int startChunkX = Math.floorDiv(base.getX(), CHUNK_SIZE);
        int startChunkY = Math.floorDiv(base.getY(), CHUNK_SIZE);
        int endChunkX = Math.floorDiv(base.getX() + visibleTiles, CHUNK_SIZE) + 1;
        int endChunkY = Math.floorDiv(base.getY() + visibleTiles, CHUNK_SIZE) + 1;

        // Render each visible chunk
        for (int chunkY = startChunkY; chunkY <= endChunkY; chunkY++) {
            for (int chunkX = startChunkX; chunkX <= endChunkX; chunkX++) {
                BufferedImage chunk = cache.getChunk(chunkX, chunkY, plane, lod, mode);

                // Calculate where this chunk appears on screen
                // World coordinates of chunk's bottom-left corner
                int chunkWorldX = chunkX * CHUNK_SIZE;
                int chunkWorldY = chunkY * CHUNK_SIZE;

                // Offset from view base (in tiles)
                float offsetX = chunkWorldX - base.getX();
                float offsetY = chunkWorldY - base.getY();

                // Screen position (Y is flipped: world Y up = screen Y down)
                // Use separate X and Y pixels per tile for non-square canvases
                int screenX = Math.round(offsetX * pixelsPerTileX);
                int screenY = canvasHeight - Math.round((offsetY + CHUNK_SIZE) * pixelsPerTileY);

                // Screen size of this chunk (width and height may differ for non-square canvas)
                int screenWidth = Math.round(CHUNK_SIZE * pixelsPerTileX);
                int screenHeight = Math.round(CHUNK_SIZE * pixelsPerTileY);

                // Draw the chunk (scaled, possibly with different width/height)
                g2d.drawImage(chunk, screenX, screenY, screenWidth, screenHeight, null);
            }
        }

        g2d.dispose();
    }

    /**
     * Renders visible chunks with additional debug info.
     */
    public void renderDebug(BufferedImage canvas, WorldPoint base, int visibleTiles, int plane, ViewerMode mode) {
        render(canvas, base, visibleTiles, plane, mode);

        Graphics2D g2d = canvas.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));

        float pixelsPerTile = (float) canvas.getWidth() / visibleTiles;
        int lod = calculateLOD(pixelsPerTile);

        String debugInfo = String.format("LOD: %d | Tiles: %d | px/tile: %.1f | Cache: %d",
                lod, visibleTiles, pixelsPerTile, cache.getCacheSize());

        g2d.drawString(debugInfo, 10, 20);
        g2d.dispose();
    }
}
