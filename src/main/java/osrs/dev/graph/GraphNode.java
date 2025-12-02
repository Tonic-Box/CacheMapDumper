package osrs.dev.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a node in the navigation graph.
 * Nodes are placed at specific world coordinates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphNode {
    private String id;
    private int x;
    private int y;
    private int plane;
    private String color = "#00FF00"; // Default green

    public GraphNode(String id, int x, int y, int plane) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.plane = plane;
        this.color = "#00FF00";
    }
}
