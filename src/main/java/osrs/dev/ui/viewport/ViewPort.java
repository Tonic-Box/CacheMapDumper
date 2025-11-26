package osrs.dev.ui.viewport;

import lombok.Getter;
import osrs.dev.Main;
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
                    Color color = getTileTypeColor(tileType);
                    g2d.setColor(color);
                    int screenX = Math.round(x * cellWidth);
                    int screenY = height - Math.round((y + 1) * cellHeight);
                    g2d.fillRect(screenX, screenY, Math.round(cellWidth) + 1, Math.round(cellHeight) + 1);
                }
            }
        }
    }

    /**
     * Gets a color for a tile type.
     */
    private Color getTileTypeColor(byte tileType) {
        switch (tileType) {
            case 1: return new Color(0, 100, 200);       // WATER - Blue
            case 2: return new Color(100, 150, 100);     // CRANDOR_SMEGMA_WATER - Greenish
            case 3: return new Color(80, 80, 150);       // TEMPOR_STORM_WATER - Dark blue
            case 4: return new Color(100, 180, 80);      // DISEASE_WATER - Sickly green
            case 5: return new Color(0, 150, 100);       // KELP_WATER - Teal
            case 6: return new Color(200, 180, 100);     // SUNBAKED_WATER - Sandy
            case 7: return new Color(100, 80, 80);       // JAGGED_REEFS_WATER - Brown
            case 8: return new Color(180, 100, 200);     // SHARP_CRYSTAL_WATER - Purple
            default: return new Color(150, 150, 150);    // Unknown - Gray
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
                flag = Main.getCollision().all(base.getX() + x, base.getY() + y, displayPlane);
                cells[x][y] = new Cell(flag, cellPoint);
            }
        }

        return cells;
    }
}