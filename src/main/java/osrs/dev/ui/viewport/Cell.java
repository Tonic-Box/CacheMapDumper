package osrs.dev.ui.viewport;

import lombok.Data;
import osrs.dev.reader.Flags;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Data
public class Cell {
    private final byte flag;
    private final Point point;
    private boolean hasLocalPlayer = false;
    private boolean selected = false;

    /**
     * no blocking
     */
    public boolean none()
    {
        return flag == Flags.ALL;
    }

    /**
     * full blocking
     */
    public boolean full()
    {
        return flag == Flags.NONE;
    }

    public boolean westWall()
    {
        return (flag & Flags.WEST) == 0;
    }

    public boolean eastWall()
    {
        return (flag & Flags.EAST) == 0;
    }

    public boolean southWall()
    {
        return (flag & Flags.SOUTH) == 0;
    }

    public boolean northWall()
    {
        return (flag & Flags.NORTH) == 0;
    }

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
            g2d.setColor(Color.BLACK);
            if(full())
            {
                g2d.fillRect(x, y, Math.round(cellWidth) + 1, Math.round(cellHeight) + 1);
            }
        }

        if(selected)
        {
            int w = Math.round(cellWidth);
            int h = Math.round(cellHeight);
            if(w < 8)
            {
                w = 8;
                h = 8;
            }
            g2d.setColor(Color.CYAN);
            g2d.fillRect(x, y, w, h);
        }

        if(hasLocalPlayer)
        {
            int w = Math.round(cellWidth);
            int h = Math.round(cellHeight);
            if(w < 8)
            {
                w = 8;
                h = 8;
            }
            g2d.setColor(Color.WHITE);
            g2d.fillRect(x, y, w + 1, h + 1);
        }

        if(!none())
        {
            drawWalls(g2d, cells, width, height);
        }
    }

    private void drawWalls(Graphics2D g2d, int cells, int width, int height) {
        float cellWidth = (float) width / cells;
        float cellHeight = (float) height / cells;

        g2d.setColor(Color.BLACK);
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