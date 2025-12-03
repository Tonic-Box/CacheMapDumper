package osrs.dev.graph;

import lombok.Data;
import osrs.dev.Main;
import osrs.dev.reader.GraphMap;

import java.io.*;
import java.util.*;

/**
 * Container for the navigation graph with nodes and edges.
 * Handles binary persistence and tile type calculation.
 * Nodes are identified by packed coordinates (one node per tile).
 * Edges are bidirectional and identified by sorted source+target coords.
 */
@Data
public class Graph {
    private List<GraphNode> nodes = new ArrayList<>();
    private List<GraphEdge> edges = new ArrayList<>();

    // Transient lookup maps for fast access (rebuilt on load)
    private transient Map<Integer, GraphNode> nodeByPacked = new HashMap<>();
    private transient Set<Long> edgeKeys = new HashSet<>();

    /**
     * Saves the graph to a binary file.
     */
    public void save(String path) {
        try {
            GraphMap.save(this, path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a graph from a binary file.
     */
    public static Graph load(String path) {
        try {
            Graph graph = GraphMap.load(path);
            graph.rebuildLookups();
            return graph;
        } catch (IOException e) {
            return new Graph();
        }
    }

    /**
     * Rebuilds transient lookup maps after loading or modification.
     */
    public void rebuildLookups() {
        nodeByPacked = new HashMap<>();
        for (GraphNode node : nodes) {
            nodeByPacked.put(node.getPacked(), node);
        }

        edgeKeys = new HashSet<>();
        for (GraphEdge edge : edges) {
            edgeKeys.add(edge.getEdgeKey());
        }
    }

    /**
     * Adds a new node at the specified world coordinates.
     * Returns null if a node already exists at this location.
     */
    public GraphNode addNode(int x, int y, int plane) {
        int packed = GraphNode.pack(x, y, plane);

        // Check for existing node
        if (nodeByPacked.containsKey(packed)) {
            return nodeByPacked.get(packed);
        }

        GraphNode node = new GraphNode(x, y, plane);
        nodes.add(node);
        nodeByPacked.put(packed, node);
        return node;
    }

    /**
     * Removes a node and all edges connected to it.
     */
    public void removeNode(int packed) {
        GraphNode node = nodeByPacked.remove(packed);
        if (node != null) {
            nodes.remove(node);
            // Cascade delete edges
            edges.removeIf(e -> {
                if (e.getSourcePacked() == packed || e.getTargetPacked() == packed) {
                    edgeKeys.remove(e.getEdgeKey());
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Removes a node by reference.
     */
    public void removeNode(GraphNode node) {
        if (node != null) {
            removeNode(node.getPacked());
        }
    }

    /**
     * Finds a node at or near the specified coordinates within tolerance.
     */
    public GraphNode findNodeAt(int x, int y, int plane, int tolerance) {
        // Check exact match first
        int packed = GraphNode.pack(x, y, plane);
        if (nodeByPacked.containsKey(packed)) {
            return nodeByPacked.get(packed);
        }

        // Search within tolerance
        for (GraphNode node : nodes) {
            if (node.getPlane() == plane) {
                int dx = Math.abs(node.getX() - x);
                int dy = Math.abs(node.getY() - y);
                if (dx <= tolerance && dy <= tolerance) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * Gets a node by its packed coordinate.
     */
    public GraphNode getNodeByPacked(int packed) {
        return nodeByPacked.get(packed);
    }

    /**
     * Adds an edge between two nodes.
     * Returns null if edge already exists or nodes don't exist.
     */
    public GraphEdge addEdge(int sourcePacked, int targetPacked) {
        // Create edge (constructor normalizes order)
        GraphEdge edge = new GraphEdge(sourcePacked, targetPacked);

        // Check for duplicate
        if (edgeKeys.contains(edge.getEdgeKey())) {
            return null;
        }

        // Verify both nodes exist
        if (!nodeByPacked.containsKey(edge.getSourcePacked()) ||
            !nodeByPacked.containsKey(edge.getTargetPacked())) {
            return null;
        }

        calculateEdgeTileTypes(edge);
        edges.add(edge);
        edgeKeys.add(edge.getEdgeKey());
        return edge;
    }

    /**
     * Adds an edge between two nodes by reference.
     */
    public GraphEdge addEdge(GraphNode source, GraphNode target) {
        if (source == null || target == null) return null;
        return addEdge(source.getPacked(), target.getPacked());
    }

    /**
     * Removes an edge by its key.
     */
    public void removeEdge(long edgeKey) {
        edgeKeys.remove(edgeKey);
        edges.removeIf(e -> e.getEdgeKey() == edgeKey);
    }

    /**
     * Removes an edge by reference.
     */
    public void removeEdge(GraphEdge edge) {
        if (edge != null) {
            removeEdge(edge.getEdgeKey());
        }
    }

    /**
     * Gets all edges connected to a node.
     */
    public List<GraphEdge> getEdgesForNode(int nodePacked) {
        List<GraphEdge> result = new ArrayList<>();
        for (GraphEdge edge : edges) {
            if (edge.getSourcePacked() == nodePacked || edge.getTargetPacked() == nodePacked) {
                result.add(edge);
            }
        }
        return result;
    }

    /**
     * Gets all edges connected to a node by reference.
     */
    public List<GraphEdge> getEdgesForNode(GraphNode node) {
        if (node == null) return new ArrayList<>();
        return getEdgesForNode(node.getPacked());
    }

    /**
     * Calculates which tile types an edge traverses using Bresenham's line algorithm.
     */
    public void calculateEdgeTileTypes(GraphEdge edge) {
        GraphNode source = getNodeByPacked(edge.getSourcePacked());
        GraphNode target = getNodeByPacked(edge.getTargetPacked());
        if (source == null || target == null) return;
        if (Main.getTileTypeMap() == null) return;

        short mask = 0;
        int plane = source.getPlane();

        // Bresenham's line algorithm
        int x0 = source.getX(), y0 = source.getY();
        int x1 = target.getX(), y1 = target.getY();
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            byte tileType = Main.getTileTypeMap().getTileType(x0, y0, plane);
            if (tileType > 0 && tileType <= 16) {
                mask |= (1 << (tileType - 1));
            }
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }

        edge.setTileTypeMask(mask);
    }

    /**
     * Clears all nodes and edges.
     */
    public void clear() {
        nodes.clear();
        edges.clear();
        nodeByPacked.clear();
        edgeKeys.clear();
    }

    /**
     * Replaces this graph's contents with another graph's contents.
     */
    public void replaceWith(Graph other) {
        clear();
        if (other != null) {
            nodes.addAll(other.getNodes());
            edges.addAll(other.getEdges());
            rebuildLookups();
        }
    }

    /**
     * Finds an edge near a point within tolerance.
     */
    public GraphEdge findEdgeNear(int x, int y, int plane, int tolerance) {
        for (GraphEdge edge : edges) {
            GraphNode source = getNodeByPacked(edge.getSourcePacked());
            GraphNode target = getNodeByPacked(edge.getTargetPacked());
            if (source == null || target == null) continue;
            if (source.getPlane() != plane) continue;

            // Check if point is near the line segment
            double dist = pointToLineDistance(x, y, source.getX(), source.getY(), target.getX(), target.getY());
            if (dist <= tolerance) {
                return edge;
            }
        }
        return null;
    }

    /**
     * Calculates the distance from a point to a line segment.
     */
    private double pointToLineDistance(int px, int py, int x1, int y1, int x2, int y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;

        if (lengthSquared == 0) {
            // Line segment is a point
            return Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));
        }

        // Parameter t for the projection onto the line
        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / lengthSquared));

        // Closest point on line segment
        double projX = x1 + t * dx;
        double projY = y1 + t * dy;

        return Math.sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY));
    }
}
