package osrs.dev.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a node in the navigation graph.
 * Nodes are placed at specific world coordinates.
 * Identity is determined by packed coordinate (one node per tile).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphNode {
    private int x;
    private int y;
    private int plane;

    /**
     * Packs x, y, plane into a single 32-bit integer.
     * x: 13 bits (0-8191), y: 15 bits (0-32767), plane: 4 bits (0-15)
     */
    public static int pack(int x, int y, int plane) {
        return (x & 8191) | ((y & 32767) << 13) | ((plane & 15) << 28);
    }

    /**
     * Unpacks a 32-bit packed coordinate into [x, y, plane].
     */
    public static int[] unpack(int packed) {
        int x = packed & 8191;
        int y = (packed >> 13) & 32767;
        int plane = (packed >> 28) & 15;
        return new int[]{x, y, plane};
    }

    /**
     * Returns the packed coordinate for this node.
     */
    public int getPacked() {
        return pack(x, y, plane);
    }
}
