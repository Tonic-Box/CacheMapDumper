package osrs.dev.ui;

import osrs.dev.Main;
import osrs.dev.graph.Graph;
import osrs.dev.graph.GraphEdge;
import osrs.dev.graph.GraphNode;
import osrs.dev.reader.TileType;
import osrs.dev.ui.viewport.ViewPort;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

/**
 * Palette window for editing the navigation graph.
 * Displays nodes, edges, tile type legend, and provides editing controls.
 */
public class GraphPaletteFrame extends JFrame {

    private final Consumer<Object> selectionCallback; // Called when selection changes
    private final Runnable updateCallback;            // Called to refresh the map
    private BiConsumer<Integer, Integer> pickerCallback;  // Called when user picks a point (x, y)

    private JTextField pathField;
    private JList<String> nodeList;
    private JList<String> edgeList;
    private DefaultListModel<String> nodeListModel;
    private DefaultListModel<String> edgeListModel;
    private JLabel selectedLabel;
    private JComboBox<String> colorComboBox;
    private JButton deleteButton;
    private JPanel nodesPanel;  // Keep reference for updating title
    private JPanel edgesPanel;  // Keep reference for updating title

    private GraphNode selectedNode;
    private GraphEdge selectedEdge;

    // Auto-generate controls
    private JSpinner nodeSpacingSpinner;
    private JSpinner collisionBufferSpinner;
    private JButton pickStartButton;
    private boolean pickerMode = false;

    private static final String[] COLORS = {
            "#00FF00", // Green
            "#FF0000", // Red
            "#0000FF", // Blue
            "#FFFF00", // Yellow
            "#FF00FF", // Magenta
            "#00FFFF", // Cyan
            "#FFA500", // Orange
            "#FFFFFF", // White
    };

    private static final String[] COLOR_NAMES = {
            "Green", "Red", "Blue", "Yellow", "Magenta", "Cyan", "Orange", "White"
    };

    public GraphPaletteFrame(Consumer<Object> selectionCallback, Runnable updateCallback) {
        this.selectionCallback = selectionCallback;
        this.updateCallback = updateCallback;

        setTitle("Graph Web Editor");
        setSize(350, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        initComponents();
        refresh();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // File path section
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        pathPanel.setBorder(BorderFactory.createTitledBorder("Graph File"));
        pathField = new JTextField(Main.getConfigManager().graphOutputPath());
        JButton browseButton = new JButton("...");
        browseButton.setPreferredSize(new Dimension(30, 25));
        browseButton.addActionListener(e -> browseForFile());
        pathPanel.add(new JLabel("Path:"), BorderLayout.WEST);
        pathPanel.add(pathField, BorderLayout.CENTER);
        pathPanel.add(browseButton, BorderLayout.EAST);
        pathPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        mainPanel.add(pathPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // Nodes section
        nodesPanel = new JPanel(new BorderLayout(5, 5));
        nodesPanel.setBorder(BorderFactory.createTitledBorder("Nodes"));
        nodeListModel = new DefaultListModel<>();
        nodeList = new JList<>(nodeListModel);
        nodeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        nodeList.addListSelectionListener(this::onNodeSelected);
        JScrollPane nodeScroll = new JScrollPane(nodeList);
        nodeScroll.setPreferredSize(new Dimension(300, 100));
        nodesPanel.add(nodeScroll, BorderLayout.CENTER);

        JButton clearNodesButton = new JButton("Clear All");
        clearNodesButton.addActionListener(e -> clearAll());
        JPanel nodeButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        nodeButtonPanel.add(clearNodesButton);
        nodesPanel.add(nodeButtonPanel, BorderLayout.SOUTH);
        nodesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        mainPanel.add(nodesPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // Edges section
        edgesPanel = new JPanel(new BorderLayout(5, 5));
        edgesPanel.setBorder(BorderFactory.createTitledBorder("Edges"));
        edgeListModel = new DefaultListModel<>();
        edgeList = new JList<>(edgeListModel);
        edgeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        edgeList.addListSelectionListener(this::onEdgeSelected);
        JScrollPane edgeScroll = new JScrollPane(edgeList);
        edgeScroll.setPreferredSize(new Dimension(300, 100));
        edgesPanel.add(edgeScroll, BorderLayout.CENTER);
        edgesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        mainPanel.add(edgesPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // Selection panel
        JPanel selectionPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        selectionPanel.setBorder(BorderFactory.createTitledBorder("Selected"));
        selectedLabel = new JLabel("None");
        colorComboBox = new JComboBox<>(COLOR_NAMES);
        colorComboBox.addActionListener(e -> onColorChanged());
        deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteSelected());
        deleteButton.setEnabled(false);

        selectionPanel.add(new JLabel("Item:"));
        selectionPanel.add(selectedLabel);
        selectionPanel.add(new JLabel("Color:"));
        selectionPanel.add(colorComboBox);
        selectionPanel.add(new JLabel());
        selectionPanel.add(deleteButton);
        selectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        mainPanel.add(selectionPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // Auto-Generate section
        JPanel autoGenPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        autoGenPanel.setBorder(BorderFactory.createTitledBorder("Auto-Generate Web"));

        autoGenPanel.add(new JLabel("Node Spacing:"));
        nodeSpacingSpinner = new JSpinner(new SpinnerNumberModel(50, 10, 200, 10));
        nodeSpacingSpinner.setToolTipText("Distance between nodes in tiles");
        autoGenPanel.add(nodeSpacingSpinner);

        autoGenPanel.add(new JLabel("Collision Buffer:"));
        collisionBufferSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 20, 1));
        collisionBufferSpinner.setToolTipText("Minimum tiles away from collision for node placement");
        autoGenPanel.add(collisionBufferSpinner);

        autoGenPanel.add(new JLabel(""));  // spacer
        pickStartButton = new JButton("Pick Start Point");
        pickStartButton.addActionListener(e -> startPickerMode());
        autoGenPanel.add(pickStartButton);

        autoGenPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        mainPanel.add(autoGenPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // Tile type legend
        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));
        legendPanel.setBorder(BorderFactory.createTitledBorder("Tile Types Legend"));

        Map<Byte, Color> colors = ViewPort.getTILE_TYPE_COLORS();
        for (byte i = 1; i <= 12; i++) {
            String name = TileType.getName(i);
            Color color = colors.getOrDefault(i, Color.GRAY);
            if (name != null) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
                JLabel colorBox = new JLabel("  ");
                colorBox.setOpaque(true);
                colorBox.setBackground(color);
                colorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                colorBox.setPreferredSize(new Dimension(20, 15));
                row.add(colorBox);
                row.add(new JLabel(name + " (" + i + ")"));
                legendPanel.add(row);
            }
        }
        JScrollPane legendScroll = new JScrollPane(legendPanel);
        legendScroll.setPreferredSize(new Dimension(300, 150));
        legendScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        mainPanel.add(legendScroll);

        add(mainPanel, BorderLayout.CENTER);

        // Save path on change
        pathField.addActionListener(e -> {
            Main.getConfigManager().setGraphOutputPath(pathField.getText());
            saveGraph();
        });
    }

    private void browseForFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(pathField.getText()));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
            Main.getConfigManager().setGraphOutputPath(pathField.getText());
            saveGraph();
        }
    }

    private void onNodeSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;

        int index = nodeList.getSelectedIndex();
        if (index >= 0 && Main.getGraph() != null) {
            List<GraphNode> nodes = Main.getGraph().getNodes();
            if (index < nodes.size()) {
                selectedNode = nodes.get(index);
                selectedEdge = null;
                edgeList.clearSelection();
                updateSelectionPanel();
                selectionCallback.accept(selectedNode);
            }
        }
    }

    private void onEdgeSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;

        int index = edgeList.getSelectedIndex();
        if (index >= 0 && Main.getGraph() != null) {
            List<GraphEdge> edges = Main.getGraph().getEdges();
            if (index < edges.size()) {
                selectedEdge = edges.get(index);
                selectedNode = null;
                nodeList.clearSelection();
                updateSelectionPanel();
                selectionCallback.accept(selectedEdge);
            }
        }
    }

    private void updateSelectionPanel() {
        if (selectedNode != null) {
            selectedLabel.setText("Node (" + selectedNode.getX() + ", " + selectedNode.getY() + ", " + selectedNode.getPlane() + ")");
            selectColorInComboBox(selectedNode.getColor());
            deleteButton.setEnabled(true);
        } else if (selectedEdge != null) {
            // Get tile type names for the edge
            StringBuilder types = new StringBuilder();
            for (int type : selectedEdge.getTileTypes()) {
                String name = TileType.getName((byte) type);
                if (name != null) {
                    if (types.length() > 0) types.append(", ");
                    types.append(name);
                }
            }
            selectedLabel.setText("Edge: " + (types.length() > 0 ? types.toString() : "No tile types"));
            selectColorInComboBox(selectedEdge.getColor());
            deleteButton.setEnabled(true);
        } else {
            selectedLabel.setText("None");
            deleteButton.setEnabled(false);
        }
    }

    private void selectColorInComboBox(String hexColor) {
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i].equalsIgnoreCase(hexColor)) {
                colorComboBox.setSelectedIndex(i);
                return;
            }
        }
        colorComboBox.setSelectedIndex(0);
    }

    private void onColorChanged() {
        int index = colorComboBox.getSelectedIndex();
        if (index < 0 || index >= COLORS.length) return;

        String newColor = COLORS[index];
        if (selectedNode != null) {
            selectedNode.setColor(newColor);
            saveGraph();
            updateCallback.run();
        } else if (selectedEdge != null) {
            selectedEdge.setColor(newColor);
            saveGraph();
            updateCallback.run();
        }
    }

    private void deleteSelected() {
        Graph graph = Main.getGraph();
        if (graph == null) return;

        if (selectedNode != null) {
            graph.removeNode(selectedNode.getId());
            selectedNode = null;
        } else if (selectedEdge != null) {
            graph.removeEdge(selectedEdge.getId());
            selectedEdge = null;
        }

        saveGraph();
        refresh();
        selectionCallback.accept(null);
        updateCallback.run();
    }

    private void clearAll() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear all nodes and edges?",
                "Clear All",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            Graph graph = Main.getGraph();
            if (graph != null) {
                graph.clear();
                saveGraph();
                refresh();
                selectionCallback.accept(null);
                updateCallback.run();
            }
        }
    }

    /**
     * Refreshes the node and edge lists from the current graph.
     */
    public void refresh() {
        nodeListModel.clear();
        edgeListModel.clear();
        selectedNode = null;
        selectedEdge = null;

        Graph graph = Main.getGraph();
        if (graph == null) return;

        // Populate nodes
        int nodeIndex = 1;
        for (GraphNode node : graph.getNodes()) {
            nodeListModel.addElement("Node " + nodeIndex + " (" + node.getX() + ", " + node.getY() + ", " + node.getPlane() + ")");
            nodeIndex++;
        }

        // Populate edges with tile type info
        for (GraphEdge edge : graph.getEdges()) {
            GraphNode source = graph.getNodeById(edge.getSourceId());
            GraphNode target = graph.getNodeById(edge.getTargetId());
            if (source == null || target == null) continue;

            // Get node indices
            int sourceIdx = graph.getNodes().indexOf(source) + 1;
            int targetIdx = graph.getNodes().indexOf(target) + 1;

            // Get tile type names
            StringBuilder types = new StringBuilder();
            for (int type : edge.getTileTypes()) {
                String name = TileType.getName((byte) type);
                if (name != null) {
                    if (types.length() > 0) types.append(", ");
                    types.append(name);
                }
            }

            String edgeText = sourceIdx + " -> " + targetIdx;
            if (types.length() > 0) {
                edgeText += ": " + types;
            }
            edgeListModel.addElement(edgeText);
        }

        // Update node count in border title
        ((TitledBorder) nodesPanel.getBorder()).setTitle("Nodes (" + graph.getNodes().size() + ")");
        ((TitledBorder) edgesPanel.getBorder()).setTitle("Edges (" + graph.getEdges().size() + ")");

        updateSelectionPanel();
        repaint();
    }

    /**
     * Selects a node in the list by its object reference.
     */
    public void selectNode(GraphNode node) {
        if (node == null || Main.getGraph() == null) {
            nodeList.clearSelection();
            selectedNode = null;
            return;
        }

        int index = Main.getGraph().getNodes().indexOf(node);
        if (index >= 0) {
            nodeList.setSelectedIndex(index);
            selectedNode = node;
            selectedEdge = null;
            edgeList.clearSelection();
            updateSelectionPanel();
        }
    }

    /**
     * Selects an edge in the list by its object reference.
     */
    public void selectEdge(GraphEdge edge) {
        if (edge == null || Main.getGraph() == null) {
            edgeList.clearSelection();
            selectedEdge = null;
            return;
        }

        int index = Main.getGraph().getEdges().indexOf(edge);
        if (index >= 0) {
            edgeList.setSelectedIndex(index);
            selectedEdge = edge;
            selectedNode = null;
            nodeList.clearSelection();
            updateSelectionPanel();
        }
    }

    /**
     * Clears the selection in both lists.
     */
    public void clearSelection() {
        nodeList.clearSelection();
        edgeList.clearSelection();
        selectedNode = null;
        selectedEdge = null;
        updateSelectionPanel();
    }

    private void saveGraph() {
        Graph graph = Main.getGraph();
        if (graph != null) {
            graph.save(Main.getConfigManager().graphOutputPath());
        }
    }

    public GraphNode getSelectedNode() {
        return selectedNode;
    }

    public GraphEdge getSelectedEdge() {
        return selectedEdge;
    }

    // ==================== Auto-Generate Methods ====================

    /**
     * Sets the callback for picker mode completion.
     */
    public void setPickerCallback(BiConsumer<Integer, Integer> callback) {
        this.pickerCallback = callback;
    }

    /**
     * Enters picker mode - user should click on map to select seed point.
     */
    private void startPickerMode() {
        // Commit any pending spinner edits
        try {
            nodeSpacingSpinner.commitEdit();
            collisionBufferSpinner.commitEdit();
        } catch (java.text.ParseException e) {
            // Ignore - use current value
        }

        pickerMode = true;
        pickStartButton.setText("Click on water...");
        pickStartButton.setEnabled(false);
    }

    /**
     * Called when user picks a point on the map while in picker mode.
     */
    public void onPointPicked(int worldX, int worldY, int plane) {
        if (!pickerMode) return;

        pickerMode = false;
        pickStartButton.setText("Pick Start Point");
        pickStartButton.setEnabled(true);

        // Validate it's water
        if (Main.getTileTypeMap() == null || Main.getTileTypeMap().getTileType(worldX, worldY, plane) == 0) {
            JOptionPane.showMessageDialog(this,
                    "Selected point is not on water. Please select a water tile.",
                    "Invalid Seed Point",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Show progress dialog and start generation
        int nodeSpacing = (Integer) nodeSpacingSpinner.getValue();
        int collisionBuffer = (Integer) collisionBufferSpinner.getValue();

        WebGeneratorDialog dialog = new WebGeneratorDialog((Frame) getOwner());
        dialog.startGeneration(worldX, worldY, plane, nodeSpacing, collisionBuffer,
                this::onGenerationComplete,
                () -> {}  // On cancelled - do nothing
        );
    }

    /**
     * Called when generation completes successfully.
     */
    private void onGenerationComplete(Graph generatedGraph) {
        Graph currentGraph = Main.getGraph();
        if (currentGraph != null && generatedGraph != null) {
            currentGraph.replaceWith(generatedGraph);
            saveGraph();
            refresh();
            updateCallback.run();
            JOptionPane.showMessageDialog(this,
                    "Generated " + generatedGraph.getNodes().size() + " nodes and " +
                            generatedGraph.getEdges().size() + " edges.",
                    "Generation Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Returns true if currently in picker mode.
     */
    public boolean isPickerMode() {
        return pickerMode;
    }

    /**
     * Cancels picker mode.
     */
    public void cancelPickerMode() {
        if (pickerMode) {
            pickerMode = false;
            pickStartButton.setText("Pick Start Point");
            pickStartButton.setEnabled(true);
        }
    }
}
