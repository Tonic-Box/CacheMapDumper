package osrs.dev.graph;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an edge connecting two nodes in the navigation graph.
 * Edges track which tile types they traverse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphEdge {
    private String id;
    private String sourceId;
    private String targetId;
    private List<Integer> tileTypes = new ArrayList<>();
    private String color = "#FFFF00"; // Default yellow

    public GraphEdge(String id, String sourceId, String targetId) {
        this.id = id;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.tileTypes = new ArrayList<>();
        this.color = "#FFFF00";
    }
}
