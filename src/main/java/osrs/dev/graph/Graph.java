package osrs.dev.graph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import osrs.dev.Main;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Container for the navigation graph with nodes and edges.
 * Handles JSON persistence and tile type calculation.
 */
@Data
public class Graph {
    private List<GraphNode> nodes = new ArrayList<>();
    private List<GraphEdge> edges = new ArrayList<>();

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Saves the graph to a JSON file.
     */
    public void save(String path) {
        try {
            File file = new File(path);
            file.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads a graph from a JSON file.
     */
    public static Graph load(String path) {
        try (Reader reader = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) {
            Graph graph = gson.fromJson(reader, Graph.class);
            return graph != null ? graph : new Graph();
        } catch (IOException e) {
            return new Graph();
        }
    }

    /**
     * Adds a new node at the specified world coordinates.
     */
    public GraphNode addNode(int x, int y, int plane) {
        String id = UUID.randomUUID().toString();
        GraphNode node = new GraphNode(id, x, y, plane);
        nodes.add(node);
        return node;
    }

    /**
     * Removes a node and all edges connected to it.
     */
    public void removeNode(String id) {
        nodes.removeIf(n -> n.getId().equals(id));
        // Cascade delete edges
        edges.removeIf(e -> e.getSourceId().equals(id) || e.getTargetId().equals(id));
    }

    /**
     * Finds a node at or near the specified coordinates within tolerance.
     */
    public GraphNode findNodeAt(int x, int y, int plane, int tolerance) {
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
     * Gets a node by its ID.
     */
    public GraphNode getNodeById(String id) {
        for (GraphNode node : nodes) {
            if (node.getId().equals(id)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Adds an edge between two nodes.
     */
    public GraphEdge addEdge(String sourceId, String targetId) {
        // Don't create duplicate edges
        for (GraphEdge edge : edges) {
            if ((edge.getSourceId().equals(sourceId) && edge.getTargetId().equals(targetId)) ||
                (edge.getSourceId().equals(targetId) && edge.getTargetId().equals(sourceId))) {
                return null;
            }
        }

        String id = UUID.randomUUID().toString();
        GraphEdge edge = new GraphEdge(id, sourceId, targetId);
        calculateEdgeTileTypes(edge);
        edges.add(edge);
        return edge;
    }

    /**
     * Removes an edge by ID.
     */
    public void removeEdge(String id) {
        edges.removeIf(e -> e.getId().equals(id));
    }

    /**
     * Gets all edges connected to a node.
     */
    public List<GraphEdge> getEdgesForNode(String nodeId) {
        List<GraphEdge> result = new ArrayList<>();
        for (GraphEdge edge : edges) {
            if (edge.getSourceId().equals(nodeId) || edge.getTargetId().equals(nodeId)) {
                result.add(edge);
            }
        }
        return result;
    }

    /**
     * Calculates which tile types an edge traverses using Bresenham's line algorithm.
     */
    public void calculateEdgeTileTypes(GraphEdge edge) {
        GraphNode source = getNodeById(edge.getSourceId());
        GraphNode target = getNodeById(edge.getTargetId());
        if (source == null || target == null) return;
        if (Main.getTileTypeMap() == null) return;

        Set<Integer> tileTypes = new LinkedHashSet<>();
        int plane = source.getPlane();

        // Bresenham's line algorithm
        int x0 = source.getX(), y0 = source.getY();
        int x1 = target.getX(), y1 = target.getY();
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            byte tileType = Main.getTileTypeMap().getTileType(x0, y0, plane);
            if (tileType > 0) {
                tileTypes.add((int) tileType);
            }
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }

        edge.setTileTypes(new ArrayList<>(tileTypes));
    }

    /**
     * Clears all nodes and edges.
     */
    public void clear() {
        nodes.clear();
        edges.clear();
    }

    /**
     * Replaces this graph's contents with another graph's contents.
     */
    public void replaceWith(Graph other) {
        nodes.clear();
        edges.clear();
        if (other != null) {
            nodes.addAll(other.getNodes());
            edges.addAll(other.getEdges());
        }
    }

    /**
     * Finds an edge near a line between two screen points.
     */
    public GraphEdge findEdgeNear(int x, int y, int plane, int tolerance) {
        for (GraphEdge edge : edges) {
            GraphNode source = getNodeById(edge.getSourceId());
            GraphNode target = getNodeById(edge.getTargetId());
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
