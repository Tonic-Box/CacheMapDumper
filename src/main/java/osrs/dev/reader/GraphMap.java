package osrs.dev.reader;

import com.tonic.services.pathfinder.collision.SparseBitSet;
import osrs.dev.graph.Graph;
import osrs.dev.graph.GraphEdge;
import osrs.dev.graph.GraphNode;
import osrs.dev.util.VarInt;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a navigation graph from binary format.
 * Format:
 *   Header: "GWEB" (4 bytes) + version (1 byte)
 *   Nodes: SparseBitSet serialized via ObjectInputStream
 *   Edges: VarInt count + (source_packed:4, target_packed:4, tile_mask:2) per edge
 */
public class GraphMap {
    private static final byte[] MAGIC = {'G', 'W', 'E', 'B'};
    private static final byte VERSION = 1;

    /**
     * Loads a graph from the specified file path.
     * Returns a new Graph object with nodes and edges populated.
     */
    public static Graph load(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return new Graph();
        }

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {

            // Read and verify header
            byte[] magic = new byte[4];
            dis.readFully(magic);
            if (magic[0] != MAGIC[0] || magic[1] != MAGIC[1] ||
                magic[2] != MAGIC[2] || magic[3] != MAGIC[3]) {
                throw new IOException("Invalid graph file magic number");
            }

            byte version = dis.readByte();
            if (version != VERSION) {
                throw new IOException("Unsupported graph file version: " + version);
            }

            // Read SparseBitSet
            int nodeByteLen = dis.readInt();
            byte[] nodeBytes = new byte[nodeByteLen];
            dis.readFully(nodeBytes);

            SparseBitSet nodeSet;
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(nodeBytes))) {
                nodeSet = (SparseBitSet) ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException("Failed to deserialize node set", e);
            }

            // Build node list from SparseBitSet
            List<GraphNode> nodes = new ArrayList<>();
            Map<Integer, GraphNode> packedToNode = new HashMap<>();

            // Iterate through all set bits
            for (int packed = nodeSet.nextSetBit(0); packed >= 0; packed = nodeSet.nextSetBit(packed + 1)) {
                int[] coords = GraphNode.unpack(packed);
                GraphNode node = new GraphNode(coords[0], coords[1], coords[2]);
                nodes.add(node);
                packedToNode.put(packed, node);
            }

            // Read edges
            int edgeCount = VarInt.readVarInt(dis);
            List<GraphEdge> edges = new ArrayList<>(edgeCount);

            for (int i = 0; i < edgeCount; i++) {
                int sourcePacked = dis.readInt();
                int targetPacked = dis.readInt();
                short tileTypeMask = dis.readShort();

                GraphEdge edge = new GraphEdge(sourcePacked, targetPacked, tileTypeMask);
                edges.add(edge);
            }

            // Build and return graph
            Graph graph = new Graph();
            graph.getNodes().addAll(nodes);
            graph.getEdges().addAll(edges);
            return graph;
        }
    }

    /**
     * Saves a Graph object to binary format.
     */
    public static void save(Graph graph, String filePath) throws IOException {
        File file = new File(filePath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {

            // Write header
            dos.write(MAGIC);
            dos.writeByte(VERSION);

            // Build SparseBitSet from nodes
            SparseBitSet nodeSet = new SparseBitSet();
            for (GraphNode node : graph.getNodes()) {
                nodeSet.set(node.getPacked());
            }

            // Serialize SparseBitSet
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(nodeSet);
            }
            byte[] nodeBytes = baos.toByteArray();
            dos.writeInt(nodeBytes.length);
            dos.write(nodeBytes);

            // Write edges
            List<GraphEdge> edges = graph.getEdges();
            VarInt.writeVarInt(dos, edges.size());

            for (GraphEdge edge : edges) {
                dos.writeInt(edge.getSourcePacked());
                dos.writeInt(edge.getTargetPacked());
                dos.writeShort(edge.getTileTypeMask());
            }
        }
    }
}
