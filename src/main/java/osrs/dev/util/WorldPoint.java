package osrs.dev.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorldPoint {

    /**
     * X-axis coordinate.
     */
    int x;

    /**
     * Y-axis coordinate.
     */
    int y;

    /**
     * The plane level of the Tile, also referred as z-axis coordinate.
     */
    int plane;

    /**
     * Gets the ID of the region containing this tile.
     *
     * @return the region ID
     */
    public int getRegionID()
    {
        return ((x >> 6) << 8) | (y >> 6);
    }

    public static short getCompressedX(int compressed)
    {
        return (short) (compressed & 0x3FFF);
    }

    public static short getCompressedY(int compressed)
    {
        return (short) ((compressed >>> 14) & 0x7FFF);
    }

    public static byte getCompressedPlane(int compressed)
    {
        return (byte)((compressed >>> 29) & 7);
    }

    @Override
    public String toString()
    {
        return "(x=" + x + ", y=" + y + ", plane=" + plane + ")";
    }

    public void north(int n)
    {
        y += n;
    }

    public void south(int n)
    {
        y -= n;
    }

    public void east(int n)
    {
        x += n;
    }

    public void west(int n)
    {
        x -= n;
    }
}