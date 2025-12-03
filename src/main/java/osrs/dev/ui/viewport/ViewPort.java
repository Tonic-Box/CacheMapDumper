package osrs.dev.ui.viewport;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.Setter;
import osrs.dev.Main;
import osrs.dev.graph.Graph;
import osrs.dev.graph.GraphEdge;
import osrs.dev.graph.GraphNode;
import osrs.dev.reader.TileType;
import osrs.dev.ui.ViewerMode;
import osrs.dev.util.WorldPoint;

import java.awt.*;
import java.awt.image.BufferedImage;

import static osrs.dev.ui.viewport.RenderConfig.*;

/**
 * The ViewPort class is responsible for rendering the game world to the screen.
 * Uses chunk-based caching for efficient rendering at all zoom levels.
 */
public class ViewPort {
    @Getter
    private BufferedImage canvas;
    private WorldPoint base;
    private int cellDim;
    private int lastPlane;
    private int displayPlane;
    private int lastWidth = 0;
    private int lastHeight = 0;
    private ViewerMode viewerMode = ViewerMode.COLLISION;

    // Chunk-based rendering system
    private final ChunkRenderer chunkRenderer = new ChunkRenderer();

    @Setter
    private Graph graph;
    @Setter
    private int selectedNodePacked = -1;  // -1 means no selection
    @Setter
    private long selectedEdgeKey = -1;    // -1 means no selection
    @Setter
    private int pendingEdgeSourcePacked = -1; // For edge creation highlight

    // Static colors for graph elements (app-managed, not stored in data)
    private static final Color NODE_COLOR = new Color(0, 255, 0);         // Green
    private static final Color NODE_SELECTED_COLOR = Color.WHITE;
    private static final Color NODE_PENDING_COLOR = Color.CYAN;
    private static final Color EDGE_COLOR = new Color(255, 255, 0);       // Yellow
    private static final Color EDGE_SELECTED_COLOR = Color.WHITE;

    /**
     * Immutable map of tile types to their rendering colors.
     * Public for use by GraphPaletteFrame legend.
     */
    @Getter
    private static final ImmutableMap<Byte, Color> TILE_TYPE_COLORS = ImmutableMap.<Byte, Color>builder()
            .put(TileType.WATER, new Color(0, 100, 200))                   // Blue
            .put(TileType.CRANDOR_SMEGMA_WATER, new Color(100, 150, 100))  // Greenish
            .put(TileType.TEMPOR_STORM_WATER, new Color(80, 80, 150))      // Dark blue
            .put(TileType.DISEASE_WATER, new Color(100, 180, 80))          // Sickly green
            .put(TileType.KELP_WATER, new Color(0, 150, 100))              // Teal
            .put(TileType.SUNBAKED_WATER, new Color(200, 180, 100))        // Sandy
            .put(TileType.JAGGED_REEFS_WATER, new Color(100, 80, 80))      // Brown
            .put(TileType.SHARP_CRYSTAL_WATER, new Color(180, 100, 200))   // Purple
            .put(TileType.ICE_WATER, new Color(150, 200, 220))             // Light blue/cyan
            .put(TileType.NE_PURPLE_GRAY_WATER, new Color(140, 120, 160))  // Purple-gray
            .put(TileType.NW_GRAY_WATER, new Color(120, 120, 130))         // Gray-blue
            .put(TileType.SE_PURPLE_WATER, new Color(160, 100, 180))       // Purple
            .build();

    private static final Color DEFAULT_TILE_TYPE_COLOR = new Color(150, 150, 150);  // Unknown - Gray

    /**
     * Constructs a new ViewPort object.
     */
    public ViewPort() {
        lastPlane = 0;
        displayPlane = 0;
    }

    /**
     * Invalidates the chunk cache. Call when underlying data changes
     * or when colors are updated in settings.
     */
    public void invalidateCache() {
        chunkRenderer.invalidateCache();
    }

    /**
     * Updates colors from config and invalidates cache.
     */
    public void updateColors() {
        chunkRenderer.updateColors();
    }

    /**
     * Renders the game world to the screen.
     *
     * @param base       The base point of the game world.
     * @param width      The width of the screen.
     * @param height     The height of the screen.
     * @param cellDim    The dimension of the cells (number of visible tiles).
     * @param viewerMode The viewer mode (Collision, TileType, or Combined).
     */
    public void render(WorldPoint base, int width, int height, int cellDim, ViewerMode viewerMode) {
        try {
            this.base = base;
            this.cellDim = cellDim;
            this.viewerMode = viewerMode;

            // Update display plane
            if (lastPlane != base.getPlane()) {
                lastPlane = base.getPlane();
                displayPlane = base.getPlane();
            }

            // Recreate canvas if size changed
            if (lastWidth != width || lastHeight != height) {
                lastWidth = width;
                lastHeight = height;
                this.canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            }

            // Clear with background color
            Graphics2D g2d = canvas.createGraphics();
            g2d.setColor(Main.getConfigManager().bgColor());
            g2d.fillRect(0, 0, width, height);
            g2d.dispose();

            // Render tiles using chunk-based system
            chunkRenderer.render(canvas, base, cellDim, displayPlane, viewerMode);

            // Render overlays on top
            renderOverlays(width, height);

        } catch (Exception e) {
            // Log but don't crash
            e.printStackTrace();
        }
    }

    /**
     * Renders overlays (grid, graph) on top of the tile layer.
     */
    private void renderOverlays(int width, int height) {
        Graphics2D g2d = canvas.createGraphics();

        float cellWidth = (float) width / cellDim;
        float cellHeight = (float) height / cellDim;

        // Draw grid lines (only when zoomed in enough)
        if (cellWidth >= GRID_MIN_PIXELS_PER_TILE) {
            renderGrid(g2d, width, height, cellWidth, cellHeight);
        }

        // Render graph overlay in COMBINED mode
        if (viewerMode == ViewerMode.COMBINED) {
            renderGraphOverlay(g2d, width, height, cellWidth, cellHeight);
        }

        g2d.dispose();
    }

    /**
     * Renders the grid lines.
     */
    private void renderGrid(Graphics2D g2d, int width, int height, float cellWidth, float cellHeight) {
        g2d.setColor(Main.getConfigManager().gridColor());
        for (int i = 0; i <= cellDim; i++) {
            g2d.drawLine(Math.round(i * cellWidth), 0, Math.round(i * cellWidth), height);
            g2d.drawLine(0, Math.round(i * cellHeight), width, Math.round(i * cellHeight));
        }
    }

    /**
     * Renders the graph overlay (nodes and edges) on top of the map.
     */
    private void renderGraphOverlay(Graphics2D g2d, int width, int height, float cellWidth, float cellHeight) {
        if (graph == null) return;

        // Enable anti-aliasing for smooth edges
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw edges first (behind nodes)
        for (GraphEdge edge : graph.getEdges()) {
            GraphNode source = graph.getNodeByPacked(edge.getSourcePacked());
            GraphNode target = graph.getNodeByPacked(edge.getTargetPacked());
            if (source == null || target == null) continue;
            if (source.getPlane() != displayPlane) continue;

            Point p1 = worldToScreen(source.getX(), source.getY(), cellWidth, cellHeight, height);
            Point p2 = worldToScreen(target.getX(), target.getY(), cellWidth, cellHeight, height);

            // Skip if both points are off screen
            if (!isOnScreen(p1, width, height) && !isOnScreen(p2, width, height)) continue;

            // Use static color
            g2d.setColor(EDGE_COLOR);

            // Selected edge is thicker
            boolean isSelected = edge.getEdgeKey() == selectedEdgeKey;
            g2d.setStroke(new BasicStroke(isSelected ? 4 : 2));
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);

            // Draw selection highlight
            if (isSelected) {
                g2d.setColor(EDGE_SELECTED_COLOR);
                g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 5}, 0));
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

        // Draw nodes on top
        int nodeRadius = 6;
        for (GraphNode node : graph.getNodes()) {
            if (node.getPlane() != displayPlane) continue;

            Point p = worldToScreen(node.getX(), node.getY(), cellWidth, cellHeight, height);

            // Skip if off screen
            if (!isOnScreen(p, width, height)) continue;

            // Use static color
            g2d.setColor(NODE_COLOR);
            g2d.fillOval(p.x - nodeRadius, p.y - nodeRadius, nodeRadius * 2, nodeRadius * 2);

            // Draw border
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawOval(p.x - nodeRadius, p.y - nodeRadius, nodeRadius * 2, nodeRadius * 2);

            // Selection highlight (white ring)
            int nodePacked = node.getPacked();
            boolean isSelected = nodePacked == selectedNodePacked;
            boolean isPendingEdgeSource = nodePacked == pendingEdgeSourcePacked;
            if (isSelected || isPendingEdgeSource) {
                g2d.setColor(isPendingEdgeSource ? NODE_PENDING_COLOR : NODE_SELECTED_COLOR);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(p.x - nodeRadius - 3, p.y - nodeRadius - 3,
                        (nodeRadius + 3) * 2, (nodeRadius + 3) * 2);
            }
        }

        // Reset stroke
        g2d.setStroke(new BasicStroke(1));
    }

    /**
     * Converts world coordinates to screen coordinates.
     */
    private Point worldToScreen(int worldX, int worldY, float cellWidth, float cellHeight, int height) {
        int cellX = worldX - base.getX();
        int cellY = worldY - base.getY();
        int screenX = Math.round((cellX + 0.5f) * cellWidth);
        int screenY = height - Math.round((cellY + 0.5f) * cellHeight);
        return new Point(screenX, screenY);
    }

    /**
     * Checks if a point is within the visible screen bounds (with margin).
     */
    private boolean isOnScreen(Point p, int width, int height) {
        int margin = 50;
        return p.x >= -margin && p.x <= width + margin && p.y >= -margin && p.y <= height + margin;
    }
}
