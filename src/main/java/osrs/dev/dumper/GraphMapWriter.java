package osrs.dev.dumper;

import com.tonic.services.pathfinder.collision.SparseBitSet;
import osrs.dev.graph.GraphEdge;
import osrs.dev.graph.GraphNode;
import osrs.dev.util.VarInt;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Writes a navigation graph to binary format.
 * Format:
 *   Header: "GWEB" (4 bytes) + version (1 byte)
 *   Nodes: SparseBitSet serialized via ObjectOutputStream
 *   Edges: VarInt count + (source_packed:4, target_packed:4, tile_mask:2) per edge
 */
public class GraphMapWriter {
    private static final byte[] MAGIC = {'G', 'W', 'E', 'B'};
    private static final byte VERSION = 1;

    private final SparseBitSet nodeSet;
    private final List<GraphEdge> edges;

    public GraphMapWriter() {
        this.nodeSet = new SparseBitSet();
        this.edges = new ArrayList<>();
    }

    /**
     * Adds a node at the specified coordinates.
     */
    public void addNode(int x, int y, int plane) {
        int packed = GraphNode.pack(x, y, plane);
        nodeSet.set(packed);
    }

    /**
     * Adds a node from a GraphNode object.
     */
    public void addNode(GraphNode node) {
        nodeSet.set(node.getPacked());
    }

    /**
     * Adds an edge between two nodes.
     */
    public void addEdge(GraphEdge edge) {
        edges.add(edge);
    }

    /**
     * Adds an edge between two packed coordinates with tile type mask.
     */
    public void addEdge(int sourcePacked, int targetPacked, short tileTypeMask) {
        GraphEdge edge = new GraphEdge(sourcePacked, targetPacked);
        edge.setTileTypeMask(tileTypeMask);
        edges.add(edge);
    }

    /**
     * Saves the graph to the specified file path.
     */
    public void save(String filePath) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {

            // Write header
            dos.write(MAGIC);
            dos.writeByte(VERSION);

            // Write nodes as SparseBitSet
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(nodeSet);
            }
            byte[] nodeBytes = baos.toByteArray();
            dos.writeInt(nodeBytes.length);
            dos.write(nodeBytes);

            // Sort edges by edge key for consistency
            edges.sort(Comparator.comparingLong(GraphEdge::getEdgeKey));

            // Write edge count
            VarInt.writeVarInt(dos, edges.size());

            // Write edges
            for (GraphEdge edge : edges) {
                dos.writeInt(edge.getSourcePacked());
                dos.writeInt(edge.getTargetPacked());
                dos.writeShort(edge.getTileTypeMask());
            }
        }
    }

    /**
     * Returns the number of nodes added.
     */
    public int getNodeCount() {
        return nodeSet.cardinality();
    }

    /**
     * Returns the number of edges added.
     */
    public int getEdgeCount() {
        return edges.size();
    }
}
