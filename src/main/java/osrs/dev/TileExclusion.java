package osrs.dev;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TileExclusion
{
    arch_1593(2791, 2979, 0),
    arch_1593_2(2790, 2979, 0),

    ;
    private final int x, y, z;

    public boolean matches(int x, int y, int z)
    {
        return this.x == x && this.y == y && this.z == z;
    }

    public static boolean isExcluded(int x, int y, int z)
    {
        for (TileExclusion exclusion : values())
        {
            if (exclusion.matches(x, y, z))
            {
                return true;
            }
        }
        return false;
    }
}