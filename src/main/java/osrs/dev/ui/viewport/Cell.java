package osrs.dev.ui.viewport;

import lombok.Data;
import osrs.dev.Main;
import osrs.dev.reader.Flags;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a cell in the viewport.
 */
@Data
public class Cell {
    private final byte flag;
    private final Point point;

    /**
     * no blocking
     * @return true if there are no walls
     */
    public boolean none()
    {
        return flag == Flags.ALL;
    }

    /**
     * full blocking
     * @return true if all walls are blocking
     */
    public boolean full()
    {
        return flag == Flags.NONE;
    }

    /**
     * west wall blocking
     */
    public boolean westWall()
    {
        return (flag & Flags.WEST) == 0;
    }

    /**
     * east wall blocking
     */
    public boolean eastWall()
    {
        return (flag & Flags.EAST) == 0;
    }

    /**
     * north wall blocking
     * @return true if there is a wall to the north
     */
    public boolean southWall()
    {
        return (flag & Flags.SOUTH) == 0;
    }

    /**
     * south wall blocking
     * @return true if there is a wall to the south
     */
    public boolean northWall()
    {
        return (flag & Flags.NORTH) == 0;
    }

    /**
     * Get the walls for this cell
     * @return a list of walls
     */
    public java.util.List<Wall> getWalls()
    {
        final List<Wall> walls = new ArrayList<>();
        if(eastWall())
        {
            walls.add(new Wall(point.x, point.y, Wall.Direction.EAST));
        }
        if(southWall())
        {
            walls.add(new Wall(point.x, point.y, Wall.Direction.SOUTH));
        }
        if(westWall())
        {
            walls.add(new Wall(point.x, point.y, Wall.Direction.WEST));
        }
        if(northWall())
        {
            walls.add(new Wall(point.x, point.y, Wall.Direction.NORTH));
        }
        return walls;
    }

    /**
     * Render the cell
     * @param g2d the graphics object
     * @param cells the number of cells
     * @param width the width of the viewport
     * @param height the height of the viewport
     */
    public void render(Graphics2D g2d, int cells, int width, int height)
    {
        float cellWidth = (float) width / cells;
        float cellHeight = (float) height / cells;

        int cellX = getPoint().x;
        int cellY = getPoint().y;
        int x = Math.round(cellX * cellWidth);
        int y = height - Math.round((cellY + 1) * cellHeight);

        if(!none())
        {
            if(cellWidth > 10)
                g2d.setColor(Main.getConfigManager().collisionColor());
            else
                g2d.setColor(Main.getConfigManager().wallColor());
            if(full())
            {
                g2d.fillRect(x, y, Math.round(cellWidth) + 1, Math.round(cellHeight) + 1);
            }
            drawWalls(g2d, cells, width, height);
        }
    }

    /**
     * Draw the walls for this cell
     * @param g2d the graphics object
     * @param cells the number of cells
     * @param width the width of the viewport
     * @param height the height of the viewport
     */
    private void drawWalls(Graphics2D g2d, int cells, int width, int height) {
        float cellWidth = (float) width / cells;
        float cellHeight = (float) height / cells;

        g2d.setColor(Main.getConfigManager().wallColor());
        int lineThickness = 2;

        for (Wall wall : getWalls()) {
            int x = Math.round(wall.getCellX() * cellWidth);
            int y = height - Math.round((wall.getCellY() + 1) * cellHeight);

            switch (wall.getDirection()) {
                case WEST:
                    g2d.fillRect(x - lineThickness, y, lineThickness, Math.round(cellHeight));
                    break;
                case EAST:
                    g2d.fillRect(x + Math.round(cellWidth) - (lineThickness / 2), y, lineThickness, Math.round(cellHeight));
                    break;
                case NORTH:
                    g2d.fillRect(x, y - lineThickness, Math.round(cellWidth), lineThickness);
                    break;
                case SOUTH:
                    g2d.fillRect(x, y + Math.round(cellHeight) - (lineThickness / 2), Math.round(cellWidth), lineThickness);
                    break;
                case DIAGONAL_NORTH_EAST:
                    g2d.setStroke(new BasicStroke(lineThickness));
                    g2d.drawLine(x, y + Math.round(cellHeight) + (lineThickness / 2), x + Math.round(cellWidth), y);
                    break;
                case DIAGONAL_NORTH_WEST:
                    g2d.setStroke(new BasicStroke(lineThickness));
                    g2d.drawLine(x + Math.round(cellWidth), y + Math.round(cellHeight), x, y);
                    break;
            }
        }
    }
}