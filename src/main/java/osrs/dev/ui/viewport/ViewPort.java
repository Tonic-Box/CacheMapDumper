package osrs.dev.ui.viewport;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import osrs.dev.Main;
import osrs.dev.reader.TileType;
import osrs.dev.ui.ViewerMode;
import osrs.dev.util.WorldPoint;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * The ViewPort class is responsible for rendering the game world to the screen.
 */
public class ViewPort
{
    @Getter
    private BufferedImage canvas;
    private WorldPoint base;
    private int cellDim;
    private int lastPlane;
    private int displayPlane;
    private int lastWidth = 0;
    private int lastHeight = 0;
    private ViewerMode viewerMode = ViewerMode.COLLISION;

    /**
     * Immutable map of tile types to their rendering colors.
     */
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
    public ViewPort()
    {
        lastPlane = 0;
        displayPlane = 0;
    }

    /**
     * Renders the game world to the screen.
     *
     * @param base The base point of the game world.
     * @param width The width of the screen.
     * @param height The height of the screen.
     * @param cellDim The dimension of the cells.
     * @param viewerMode The viewer mode (Collision or TileType).
     */
    public void render(WorldPoint base, int width, int height, int cellDim, ViewerMode viewerMode)
    {

        try
        {
            this.base = base;
            this.cellDim = cellDim;
            this.viewerMode = viewerMode;
            if(lastWidth != width || lastHeight != height)
            {
                lastWidth = width;
                lastHeight = height;
                this.canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            }

            Graphics2D g2d = canvas.createGraphics();

            // Set the background color
            g2d.setColor(Main.getConfigManager().bgColor());
            g2d.fillRect(0, 0, width, height);

            // Draw the grid
            g2d.setColor(Main.getConfigManager().gridColor());
            float cellWidth = (float) width / cellDim;
            float cellHeight = (float) height / cellDim;
            if(cellWidth >= 10)
            {
                for (int i = 0; i <= cellDim; i++) {
                    g2d.drawLine(Math.round(i * cellWidth), 0, Math.round(i * cellWidth), height);
                    g2d.drawLine(0, Math.round(i * cellHeight), width, Math.round(i * cellHeight));
                }
            }

            if (viewerMode == ViewerMode.COLLISION) {
                renderCollisionMode(g2d, width, height);
            } else if (viewerMode == ViewerMode.TILE_TYPE) {
                renderTileTypeMode(g2d, width, height);
            } else if (viewerMode == ViewerMode.COMBINED) {
                renderCombinedMode(g2d, width, height);
            }
        }
        catch(Exception ignored) {
        }
    }

    /**
     * Renders collision data.
     */
    private void renderCollisionMode(Graphics2D g2d, int width, int height) {
        if (Main.getCollision() == null) return;

        Cell[][] cells = buildCells();
        for(Cell[] row : cells)
        {
            for(Cell cell : row)
            {
                cell.render(g2d, cellDim, width, height);
            }
        }
    }

    /**
     * Renders tile type data with color-coded tiles.
     */
    private void renderTileTypeMode(Graphics2D g2d, int width, int height) {
        if (Main.getTileTypeMap() == null) return;

        if(lastPlane != base.getPlane())
        {
            lastPlane = base.getPlane();
            displayPlane = base.getPlane();
        }

        float cellWidth = (float) width / cellDim;
        float cellHeight = (float) height / cellDim;

        for(int x = 0; x < cellDim; x++)
        {
            for(int y = 0; y < cellDim; y++)
            {
                byte tileType = Main.getTileTypeMap().getTileType(base.getX() + x, base.getY() + y, displayPlane);
                if (tileType > 0) {
                    Color color = TILE_TYPE_COLORS.getOrDefault(tileType, DEFAULT_TILE_TYPE_COLOR);
                    g2d.setColor(color);
                    int screenX = Math.round(x * cellWidth);
                    int screenY = height - Math.round((y + 1) * cellHeight);
                    g2d.fillRect(screenX, screenY, Math.round(cellWidth) + 1, Math.round(cellHeight) + 1);
                }
            }
        }
    }

    /**
     * Renders combined mode: tile type colors as background, collision overlaid on top.
     */
    private void renderCombinedMode(Graphics2D g2d, int width, int height) {
        if (Main.getCollision() == null) return;

        if(lastPlane != base.getPlane())
        {
            lastPlane = base.getPlane();
            displayPlane = base.getPlane();
        }

        float cellWidth = (float) width / cellDim;
        float cellHeight = (float) height / cellDim;

        for(int x = 0; x < cellDim; x++)
        {
            for(int y = 0; y < cellDim; y++)
            {
                int worldX = base.getX() + x;
                int worldY = base.getY() + y;

                // First: render tile type color as background (instead of white)
                boolean hasTileType = false;
                if (Main.getTileTypeMap() != null) {
                    byte tileType = Main.getTileTypeMap().getTileType(worldX, worldY, displayPlane);
                    if (tileType > 0) {
                        hasTileType = true;
                        Color color = TILE_TYPE_COLORS.getOrDefault(tileType, DEFAULT_TILE_TYPE_COLOR);
                        g2d.setColor(color);
                        int screenX = Math.round(x * cellWidth);
                        int screenY = height - Math.round((y + 1) * cellHeight);
                        g2d.fillRect(screenX, screenY, Math.round(cellWidth) + 1, Math.round(cellHeight) + 1);
                    }
                }

                // Second: render collision on top (walls, blocked areas)
                // If tile has a tile type, skip red fill but still draw walls
                byte flag = Main.getCollision().all((short)worldX, (short)worldY, (byte)displayPlane);
                Point cellPoint = new Point(x, y);
                Cell cell = new Cell(flag, cellPoint);
                cell.render(g2d, cellDim, width, height, hasTileType);
            }
        }
    }

    /**
     * Builds the cells for the game world tiles
     *
     * @return The cells for the game world.
     */
    private Cell[][] buildCells()
    {
        if(lastPlane != base.getPlane())
        {
            lastPlane = base.getPlane();
            displayPlane = base.getPlane();
        }
        Cell[][] cells = new Cell[cellDim][cellDim];
        byte flag;
        Point cellPoint;
        for(int x = 0; x < cellDim; x++)
        {
            for(int y = 0; y < cellDim; y++)
            {
                cellPoint = new Point(x, y);
                flag = Main.getCollision().all((short)(base.getX() + x), (short)(base.getY() + y), (byte)displayPlane);
                cells[x][y] = new Cell(flag, cellPoint);
            }
        }

        return cells;
    }
}