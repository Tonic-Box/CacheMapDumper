package osrs.dev.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an edge connecting two nodes in the navigation graph.
 * Edges track which tile types they traverse via a bitmask.
 * Identity is determined by sorted pair of source+target packed coordinates.
 * Edges are bidirectional (A-B == B-A).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphEdge {
    private int sourcePacked;  // Packed coordinate of source node
    private int targetPacked;  // Packed coordinate of target node
    private short tileTypeMask; // Bitmask of tile types (bit 0 = type 1, bit 1 = type 2, etc.)

    /**
     * Creates an edge with normalized ordering (lower packed coord first).
     */
    public GraphEdge(int sourcePacked, int targetPacked) {
        // Normalize: always store lower packed coord as source
        if (sourcePacked <= targetPacked) {
            this.sourcePacked = sourcePacked;
            this.targetPacked = targetPacked;
        } else {
            this.sourcePacked = targetPacked;
            this.targetPacked = sourcePacked;
        }
        this.tileTypeMask = 0;
    }

    /**
     * Creates a unique 64-bit key for this edge (for deduplication).
     */
    public long getEdgeKey() {
        return ((long) sourcePacked << 32) | (targetPacked & 0xFFFFFFFFL);
    }

    /**
     * Converts tile type list to bitmask.
     * Each tile type (1-16) sets bit (type-1).
     */
    public static short listToMask(List<Integer> tileTypes) {
        short mask = 0;
        if (tileTypes != null) {
            for (int type : tileTypes) {
                if (type >= 1 && type <= 16) {
                    mask |= (1 << (type - 1));
                }
            }
        }
        return mask;
    }

    /**
     * Converts bitmask to tile type list.
     */
    public static List<Integer> maskToList(short mask) {
        List<Integer> list = new ArrayList<>();
        for (int bit = 0; bit < 16; bit++) {
            if ((mask & (1 << bit)) != 0) {
                list.add(bit + 1);
            }
        }
        return list;
    }

    /**
     * Sets tile types from a list.
     */
    public void setTileTypesFromList(List<Integer> tileTypes) {
        this.tileTypeMask = listToMask(tileTypes);
    }

    /**
     * Gets tile types as a list.
     */
    public List<Integer> getTileTypesAsList() {
        return maskToList(tileTypeMask);
    }

    /**
     * Adds a tile type to the mask.
     */
    public void addTileType(int type) {
        if (type >= 1 && type <= 16) {
            tileTypeMask |= (1 << (type - 1));
        }
    }

    /**
     * Checks if edge contains a specific tile type.
     */
    public boolean hasTileType(int type) {
        if (type >= 1 && type <= 16) {
            return (tileTypeMask & (1 << (type - 1))) != 0;
        }
        return false;
    }
}
