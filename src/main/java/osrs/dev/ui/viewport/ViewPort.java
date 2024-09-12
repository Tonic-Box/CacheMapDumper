package osrs.dev.ui.viewport;

import lombok.Getter;
import osrs.dev.Main;
import osrs.dev.util.WorldPoint;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * The ViewPort class is responsible for rendering the game world to the screen.
 */
public class ViewPort
{
    private WorldPoint base;
    private int cellDim;
    private int lastPlane;
    private int displayPlane;
    @Getter
    private BufferedImage canvas;
    private int lastWidth = 0;
    private int lastHeight = 0;


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
     */
    public void render(WorldPoint base, int width, int height, int cellDim)
    {

        try
        {
            this.base = base;
            this.cellDim = cellDim;
            if(lastWidth != width || lastHeight != height)
            {
                lastWidth = width;
                lastHeight = height;
                this.canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            }

            Cell[][] cells = buildCells();
            Graphics2D g2d = canvas.createGraphics();

            // Set the background color to white
            g2d.setColor(new Color(128, 128, 128));
            g2d.fillRect(0, 0, width, height);

            // Draw the grid
            g2d.setColor(Color.CYAN);
            float cellWidth = (float) width / cellDim;
            float cellHeight = (float) height / cellDim;
            if(cellWidth >= 10)
            {
                for (int i = 0; i <= cellDim; i++) {
                    g2d.drawLine(Math.round(i * cellWidth), 0, Math.round(i * cellWidth), height);
                    g2d.drawLine(0, Math.round(i * cellHeight), width, Math.round(i * cellHeight));
                }
            }

            Cell playerCell = null;
            Cell selectedCell = null;

            for(Cell[] row : cells)
            {
                for(Cell cell : row)
                {
                    cell.render(g2d, cellDim, width, height);
                    if(cell.isHasLocalPlayer())
                    {
                        playerCell = cell;
                    }
                    else if(cell.isSelected())
                    {
                        selectedCell = cell;
                    }
                }
            }

            if(selectedCell != null)
            {
                selectedCell.render(g2d, cellDim, width, height);
            }
            if(playerCell != null)
            {
                playerCell.render(g2d, cellDim, width, height);
            }
        }
        catch(Exception ignored) {
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