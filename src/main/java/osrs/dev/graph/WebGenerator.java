package osrs.dev.graph;

import osrs.dev.Main;
import osrs.dev.reader.CollisionMap;
import osrs.dev.reader.TileTypeMap;

import java.util.*;
import java.util.function.Consumer;

/**
 * Generates a sparse navigation web across all connected water.
 * Creates a skeleton of nodes spaced far apart for efficient pathfinding,
 * not dense coverage.
 */
public class WebGenerator {

    private final CollisionMap collision;
    private final TileTypeMap tileTypeMap;
    private final int plane;
    private final Consumer<ProgressUpdate> progressCallback;
    private volatile boolean cancelled = false;

    // Configuration
    private int nodeSpacing = 50;
    private int maxNeighbors = 8;       // More connections per node
    private int collisionBuffer = 5;
    private double maxEdgeDistanceMultiplier = 3.0;  // Connect nodes up to 3x spacing apart

    private static final int PROGRESS_INTERVAL = 50000;

    public WebGenerator(int plane, Consumer<ProgressUpdate> progressCallback) {
        this.collision = Main.getCollision();
        this.tileTypeMap = Main.getTileTypeMap();
        this.plane = plane;
        this.progressCallback = progressCallback;
    }

    public void setNodeSpacing(int spacing) {
        this.nodeSpacing = Math.max(10, spacing);
    }

    public void setCollisionBuffer(int buffer) {
        this.collisionBuffer = Math.max(0, buffer);
    }

    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Generates a sparse navigation web starting from the seed point.
     * Places nodes at regular spacing intervals during exploration.
     */
    public Graph generate(int seedX, int seedY) {
        cancelled = false;

        // Verify seed is on water
        if (tileTypeMap == null || tileTypeMap.getTileType(seedX, seedY, plane) == 0) {
            reportProgress(0, "Error", "Seed point is not on water!");
            return null;
        }

        // Phase 1 & 2 combined: Explore water and place sparse nodes
        reportProgress(0, "Exploring", "Building sparse web...");
        List<GraphNode> nodes = exploreAndPlaceNodes(seedX, seedY);
        if (cancelled) return null;

        if (nodes.isEmpty()) {
            reportProgress(0, "Error", "No nodes created!");
            return null;
        }

        reportProgress(60, "Placing Nodes", "Created " + nodes.size() + " nodes");

        // Phase 3: Generate edges between nearby nodes
        reportProgress(60, "Generating Edges", "Connecting nodes...");
        List<GraphEdge> edges = generateEdges(nodes);
        if (cancelled) return null;

        reportProgress(100, "Complete", nodes.size() + " nodes, " + edges.size() + " edges");

        // Build graph
        Graph graph = new Graph();
        graph.getNodes().addAll(nodes);
        graph.getEdges().addAll(edges);

        return graph;
    }

    /**
     * BFS exploration that places nodes at regular spacing intervals.
     * Creates a sparse skeleton across all connected water.
     * Uses collision to prevent crossing into landlocked water bodies.
     * Prefers node positions with buffer distance from collision.
     */
    private List<GraphNode> exploreAndPlaceNodes(int seedX, int seedY) {
        List<GraphNode> nodes = new ArrayList<>();
        Set<Long> visitedTiles = new HashSet<>();
        Set<Long> nodeGridCells = new HashSet<>();  // Track which grid cells have nodes
        // Track best candidate per grid cell: gridCell -> [x, y, bufferDistance]
        Map<Long, int[]> gridCellCandidates = new HashMap<>();

        Queue<long[]> frontier = new LinkedList<>();
        frontier.add(new long[]{seedX, seedY});
        visitedTiles.add(packCoords(seedX, seedY));

        // Place first node at seed only if it meets buffer requirement
        int seedBufferDist = calculateCollisionBuffer(seedX, seedY);
        if (seedBufferDist >= collisionBuffer) {
            GraphNode seedNode = new GraphNode(seedX, seedY, plane);
            nodes.add(seedNode);
            nodeGridCells.add(getGridCell(seedX, seedY));
        }

        int tilesExplored = 0;

        while (!frontier.isEmpty() && !cancelled) {
            long[] current = frontier.poll();
            int x = (int) current[0];
            int y = (int) current[1];

            // Check each direction with collision
            if (canMove(x, y, 0, 1)) tryExplore(x, y + 1, visitedTiles, frontier, gridCellCandidates, nodeGridCells);
            if (canMove(x, y, 0, -1)) tryExplore(x, y - 1, visitedTiles, frontier, gridCellCandidates, nodeGridCells);
            if (canMove(x, y, 1, 0)) tryExplore(x + 1, y, visitedTiles, frontier, gridCellCandidates, nodeGridCells);
            if (canMove(x, y, -1, 0)) tryExplore(x - 1, y, visitedTiles, frontier, gridCellCandidates, nodeGridCells);

            tilesExplored++;
            if (tilesExplored % PROGRESS_INTERVAL == 0) {
                reportProgress((int) (50.0 * tilesExplored / (tilesExplored + frontier.size())),
                        "Exploring", tilesExplored + " tiles explored...");
            }
        }

        if (cancelled) return nodes;

        // Now create nodes from the best candidates per grid cell
        reportProgress(40, "Placing Nodes", "Selecting optimal positions...");
        int processed = 0;
        int totalCandidates = gridCellCandidates.size();
        for (Map.Entry<Long, int[]> entry : gridCellCandidates.entrySet()) {
            if (cancelled) break;

            int[] candidate = entry.getValue();
            GraphNode node = new GraphNode(candidate[0], candidate[1], plane);
            nodes.add(node);

            processed++;
            if (processed % 100 == 0) {
                reportProgress(40 + (int) (20.0 * processed / totalCandidates),
                        "Placing Nodes", nodes.size() + " nodes placed...");
            }
        }

        return nodes;
    }

    /**
     * Checks if movement is possible from (x,y) in the given direction.
     */
    private boolean canMove(int x, int y, int dx, int dy) {
        if (collision == null) return true;

        if (dx == 1) return collision.e((short) x, (short) y, (byte) plane) == 1;
        if (dx == -1) return collision.w((short) x, (short) y, (byte) plane) == 1;
        if (dy == 1) return collision.n((short) x, (short) y, (byte) plane) == 1;
        if (dy == -1) return collision.s((short) x, (short) y, (byte) plane) == 1;
        return false;
    }

    /**
     * Tries to explore a tile and track it as a node candidate.
     */
    private void tryExplore(int nx, int ny, Set<Long> visitedTiles, Queue<long[]> frontier,
                            Map<Long, int[]> gridCellCandidates, Set<Long> nodeGridCells) {
        long packed = packCoords(nx, ny);
        if (visitedTiles.contains(packed)) return;

        // Must be water
        if (tileTypeMap.getTileType(nx, ny, plane) == 0) return;

        visitedTiles.add(packed);
        frontier.add(new long[]{nx, ny});

        // Check if this could be a node candidate for its grid cell
        long gridCell = getGridCell(nx, ny);
        if (nodeGridCells.contains(gridCell)) return;  // Already has a node (seed)

        // Calculate buffer distance from collision
        int bufferDist = calculateCollisionBuffer(nx, ny);

        // Only consider this position if it meets the minimum buffer requirement
        if (bufferDist >= collisionBuffer) {
            // Check if this is a better candidate than existing one
            int[] existing = gridCellCandidates.get(gridCell);
            if (existing == null || bufferDist > existing[2]) {
                gridCellCandidates.put(gridCell, new int[]{nx, ny, bufferDist});
            }
        }
    }

    /**
     * Calculates minimum distance to collision from a point.
     * Returns the buffer distance (capped at collisionBuffer).
     * Checks for non-water tiles and tiles that block ALL directions (full collision).
     */
    private int calculateCollisionBuffer(int x, int y) {
        if (collisionBuffer == 0) return collisionBuffer;

        // Check expanding rings around the point
        for (int dist = 1; dist <= collisionBuffer; dist++) {
            // Check all tiles at this distance (square ring)
            for (int dx = -dist; dx <= dist; dx++) {
                for (int dy = -dist; dy <= dist; dy++) {
                    // Only check tiles on the ring perimeter
                    if (Math.abs(dx) != dist && Math.abs(dy) != dist) continue;

                    int checkX = x + dx;
                    int checkY = y + dy;

                    // Check if this tile is a collision obstacle
                    if (isCollisionTile(checkX, checkY)) {
                        return dist - 1;  // Return distance to nearest collision minus 1
                    }
                }
            }
        }

        return collisionBuffer;  // No collision found within buffer
    }

    /**
     * Checks if a tile is a collision obstacle (non-water or blocks all movement).
     * This is different from edge-of-water tiles which may block some directions.
     */
    private boolean isCollisionTile(int x, int y) {
        // Check if it's not a water tile
        if (tileTypeMap != null && tileTypeMap.getTileType(x, y, plane) == 0) {
            return true;  // Non-water tile is considered collision
        }

        // Check if it's a fully blocked tile (blocks all 4 directions)
        if (collision != null) {
            boolean blocksN = collision.n((short) x, (short) y, (byte) plane) == 0;
            boolean blocksS = collision.s((short) x, (short) y, (byte) plane) == 0;
            boolean blocksE = collision.e((short) x, (short) y, (byte) plane) == 0;
            boolean blocksW = collision.w((short) x, (short) y, (byte) plane) == 0;

            // Only consider it collision if it blocks most directions (3+ blocked = obstacle)
            int blockedCount = (blocksN ? 1 : 0) + (blocksS ? 1 : 0) + (blocksE ? 1 : 0) + (blocksW ? 1 : 0);
            if (blockedCount >= 3) {
                return true;  // Likely an obstacle (rock, etc.) in the water
            }
        }

        return false;
    }

    /**
     * Gets the grid cell identifier for a coordinate.
     */
    private long getGridCell(int x, int y) {
        int gridX = x / nodeSpacing;
        int gridY = y / nodeSpacing;
        return ((long) gridX << 32) | (gridY & 0xFFFFFFFFL);
    }

    /**
     * Generates edges by connecting nearby nodes that pass validation.
     * More aggressive cross-connection while respecting water/path checks.
     */
    private List<GraphEdge> generateEdges(List<GraphNode> nodes) {
        List<GraphEdge> edges = new ArrayList<>();
        Set<Long> edgeSet = new HashSet<>(); // Prevent duplicates using packed edge keys

        double maxEdgeDistance = nodeSpacing * maxEdgeDistanceMultiplier;

        int processed = 0;
        for (GraphNode node : nodes) {
            if (cancelled) break;

            // Find all neighbors within max edge distance
            List<GraphNode> neighbors = findNeighborsWithinDistance(node, nodes, maxEdgeDistance);

            int connectedCount = 0;
            for (GraphNode neighbor : neighbors) {
                // Soft cap on connections per node, but don't hard stop
                // Allow more edges for nodes that have good connections available
                if (connectedCount >= maxNeighbors) {
                    // Still allow very close nodes (within 1.5x spacing)
                    double dist = distance(node, neighbor);
                    if (dist > nodeSpacing * 1.5) continue;
                }

                // Create edge (constructor normalizes order)
                GraphEdge edge = new GraphEdge(node.getPacked(), neighbor.getPacked());

                // Skip if edge already exists
                if (edgeSet.contains(edge.getEdgeKey())) continue;

                // Validate edge (water percentage + BFS path check)
                if (!isValidEdge(node, neighbor)) continue;

                // Calculate tile types along edge
                edge.setTileTypesFromList(calculateTileTypes(node, neighbor));

                edges.add(edge);
                edgeSet.add(edge.getEdgeKey());
                connectedCount++;
            }

            processed++;
            if (processed % 50 == 0) {
                reportProgress(60 + (int) (40.0 * processed / nodes.size()),
                        "Generating Edges", "Created " + edges.size() + " edges...");
            }
        }

        return edges;
    }

    /**
     * Finds all neighbors within a given distance, sorted by distance.
     */
    private List<GraphNode> findNeighborsWithinDistance(GraphNode node, List<GraphNode> allNodes, double maxDist) {
        List<GraphNode> neighbors = new ArrayList<>();

        for (GraphNode other : allNodes) {
            if (other == node) continue;
            if (other.getPlane() != node.getPlane()) continue;

            double dist = distance(node, other);
            if (dist <= maxDist) {
                neighbors.add(other);
            }
        }

        // Sort by distance (closest first)
        neighbors.sort((a, b) -> Double.compare(distance(node, a), distance(node, b)));

        return neighbors;
    }

    /**
     * Validates an edge using bounded BFS and water percentage check.
     * Rejects if:
     * - 40% or more of tiles along the line are non-water
     * - Actual BFS path length exceeds straight-line distance by more than 30%
     */
    private boolean isValidEdge(GraphNode a, GraphNode b) {
        if (collision == null && tileTypeMap == null) return true;

        int startX = a.getX(), startY = a.getY();
        int endX = b.getX(), endY = b.getY();

        // First check: water percentage along straight line
        if (!hasEnoughWater(startX, startY, endX, endY)) {
            return false;
        }

        // Second check: bounded BFS to verify traversability
        double straightDist = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));

        // Max steps allowed (30% tolerance for minor detours)
        int maxSteps = (int) (straightDist * 1.3);

        // BFS to find if we can reach B from A within maxSteps
        Set<Long> visited = new HashSet<>();
        Queue<long[]> frontier = new LinkedList<>();  // [x, y, steps]

        frontier.add(new long[]{startX, startY, 0});
        visited.add(packCoords(startX, startY));

        // Tolerance for reaching target (within 2 tiles)
        int targetTolerance = 2;

        while (!frontier.isEmpty()) {
            long[] current = frontier.poll();
            int x = (int) current[0];
            int y = (int) current[1];
            int steps = (int) current[2];

            // Check if we reached the target
            if (Math.abs(x - endX) <= targetTolerance && Math.abs(y - endY) <= targetTolerance) {
                return true;  // Path found within bounds
            }

            // Stop if we've exceeded max steps
            if (steps >= maxSteps) continue;

            // Try all 4 directions
            tryMove(x, y, 0, 1, steps + 1, visited, frontier);   // North
            tryMove(x, y, 0, -1, steps + 1, visited, frontier);  // South
            tryMove(x, y, 1, 0, steps + 1, visited, frontier);   // East
            tryMove(x, y, -1, 0, steps + 1, visited, frontier);  // West
        }

        // Couldn't reach target within max steps
        return false;
    }

    /**
     * Checks if at least 60% of tiles along straight line are water.
     */
    private boolean hasEnoughWater(int x0, int y0, int x1, int y1) {
        if (tileTypeMap == null) return true;

        int waterTiles = 0;
        int totalTiles = 0;

        // Bresenham's line algorithm
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            totalTiles++;
            if (tileTypeMap.getTileType(x0, y0, plane) > 0) {
                waterTiles++;
            }

            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }

        // Reject if 40% or more is non-water (require at least 60% water)
        double waterPercent = (totalTiles > 0) ? (waterTiles * 100.0 / totalTiles) : 100.0;
        return waterPercent >= 60.0;
    }

    /**
     * Tries to move in a direction for edge validation BFS.
     */
    private void tryMove(int x, int y, int dx, int dy, int newSteps,
                         Set<Long> visited, Queue<long[]> frontier) {
        int nx = x + dx;
        int ny = y + dy;
        long packed = packCoords(nx, ny);

        if (visited.contains(packed)) return;

        // Check collision
        if (!canMove(x, y, dx, dy)) return;

        // Check water (must be water tile)
        if (tileTypeMap != null && tileTypeMap.getTileType(nx, ny, plane) == 0) return;

        visited.add(packed);
        frontier.add(new long[]{nx, ny, newSteps});
    }

    /**
     * Calculates distance between two nodes.
     */
    private double distance(GraphNode a, GraphNode b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
    }

    /**
     * Calculates tile types along an edge path.
     */
    private List<Integer> calculateTileTypes(GraphNode a, GraphNode b) {
        Set<Integer> types = new LinkedHashSet<>();

        int x0 = a.getX(), y0 = a.getY();
        int x1 = b.getX(), y1 = b.getY();
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            byte tileType = tileTypeMap.getTileType(x0, y0, plane);
            if (tileType > 0) {
                types.add((int) tileType);
            }

            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }

        return new ArrayList<>(types);
    }

    private long packCoords(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    private int[] unpackCoords(long packed) {
        return new int[]{(int) (packed >> 32), (int) packed};
    }

    private void reportProgress(int percent, String phase, String detail) {
        if (progressCallback != null) {
            progressCallback.accept(new ProgressUpdate(percent, phase, detail));
        }
    }

    /**
     * Progress update data class.
     */
    public static class ProgressUpdate {
        public final int percent;
        public final String phase;
        public final String detail;

        public ProgressUpdate(int percent, String phase, String detail) {
            this.percent = percent;
            this.phase = phase;
            this.detail = detail;
        }
    }
}
