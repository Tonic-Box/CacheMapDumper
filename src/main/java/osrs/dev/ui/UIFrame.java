package osrs.dev.ui;

import osrs.dev.dumper.Dumper;
import osrs.dev.Main;
import osrs.dev.graph.Graph;
import osrs.dev.graph.GraphEdge;
import osrs.dev.graph.GraphNode;
import osrs.dev.reader.TileType;
import osrs.dev.ui.viewport.ViewPort;
import osrs.dev.util.ImageUtil;
import osrs.dev.util.ThreadPool;
import osrs.dev.util.WorldPoint;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import static osrs.dev.ui.Components.*;

/**
 * The main UI frame for the Collision Viewer.
 */
public class UIFrame extends JFrame {
    private final JLabel mapView;
    private final JSlider zoomSlider;
    private final JSlider speedSlider;
    private final JButton upButton;
    private JTextField pathField;
    private JTextField objectPathField;
    private JTextField tileTypePathField;
    private JCheckBox downloadCacheCheckBox;
    private final ViewPort viewPort;
    private final WorldPoint base = new WorldPoint(3207, 3213, 0);
    private final WorldPoint center = new WorldPoint(0,0,0);
    private Future<?> current;
    private SettingsFrame settingsFrame;
    private JTextField worldPointField;
    private JComboBox<ViewerMode> viewerModeComboBox;
    private ViewerMode currentViewerMode = ViewerMode.COLLISION;
    private Point dragStart;

    // Graph editing state
    private GraphPaletteFrame graphPaletteFrame;
    private JToggleButton webButton;
    private boolean graphEditMode = false;
    private GraphNode pendingEdgeSource = null;  // First node for edge creation
    private GraphNode selectedNode = null;       // Currently selected node
    private GraphEdge selectedEdge = null;       // Currently selected edge
    private GraphNode draggingNode = null;       // Node being dragged
    private boolean nodeWasMoved = false;        // Flag to track if node actually moved during drag
    private GraphNode ctrlPressedNode = null;    // Node that was Ctrl+pressed (for edge creation vs drag)

    /**
     * Creates a new UI frame for the Collision Viewer.
     */
    public UIFrame() {
        setIconImage(ImageUtil.loadImageResource(UIFrame.class, "icon.png"));
        setTitle("Collision Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(800, 600);
        setLocationRelativeTo(null);

        viewPort = new ViewPort();

        // Main panel for image display
        JPanel imagePanel = new JPanel(new BorderLayout());
        mapView = createMapView();
        imagePanel.add(mapView, BorderLayout.CENTER);
        add(imagePanel, BorderLayout.CENTER);

        // Control panel on the right
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setPreferredSize(new Dimension(150, 300));

        // Navigation panel (buttons in plus shape)
        JPanel navigationPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Set up button size and add the Up button (use ^ for up)
        upButton = createDirectionButton(Direction.NORTH, e -> moveImage(Direction.NORTH));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        navigationPanel.add(upButton, gbc);

        // Add the Left button (use < for left)
        JButton leftButton = createDirectionButton(Direction.WEST, e -> moveImage(Direction.WEST));
        gbc.gridx = 0;
        gbc.gridy = 1;
        navigationPanel.add(leftButton, gbc);

        // Add the Right button (use > for right)
        JButton rightButton = createDirectionButton(Direction.EAST, e -> moveImage(Direction.EAST));
        gbc.gridx = 2;
        gbc.gridy = 1;
        navigationPanel.add(rightButton, gbc);

        // Add the Down button (use v for down)
        JButton downButton = createDirectionButton(Direction.SOUTH, e -> moveImage(Direction.SOUTH));
        gbc.gridx = 1;
        gbc.gridy = 2;
        navigationPanel.add(downButton, gbc);

        controlPanel.add(navigationPanel, BorderLayout.CENTER);

        // Zoom slider
        zoomSlider = createZoomSlider(e -> {
            calculateBase();
            calculateCenter();
            update();
        });
        controlPanel.add(zoomSlider, BorderLayout.EAST);

        // Radio buttons for plane selection
        JPanel planeSelectionPanel = new JPanel(new GridLayout(5, 1));
        planeSelectionPanel.setBorder(BorderFactory.createTitledBorder("Select Plane"));

        ButtonGroup planeGroup = new ButtonGroup();
        JRadioButton plane1 = new JRadioButton("Plane 0", true);
        JRadioButton plane2 = new JRadioButton("Plane 1");
        JRadioButton plane3 = new JRadioButton("Plane 2");
        JRadioButton plane4 = new JRadioButton("Plane 3");

        planeGroup.add(plane1);
        planeGroup.add(plane2);
        planeGroup.add(plane3);
        planeGroup.add(plane4);

        planeSelectionPanel.add(plane1);
        planeSelectionPanel.add(plane2);
        planeSelectionPanel.add(plane3);
        planeSelectionPanel.add(plane4);

        // Action listeners for plane buttons
        ActionListener planeActionListener = e -> {
            JRadioButton source = (JRadioButton) e.getSource();
            if (source == plane1) {
                setPlane(0);
            } else if (source == plane2) {
                setPlane(1);
            } else if (source == plane3) {
                setPlane(2);
            } else if (source == plane4) {
                setPlane(3);
            }
        };

        plane1.addActionListener(planeActionListener);
        plane2.addActionListener(planeActionListener);
        plane3.addActionListener(planeActionListener);
        plane4.addActionListener(planeActionListener);

        controlPanel.add(planeSelectionPanel, BorderLayout.SOUTH);

        add(controlPanel, BorderLayout.EAST);

        // Speed slider
        speedSlider = createSpeedSlider();
        add(speedSlider, BorderLayout.NORTH);

        // Create bottom panel with controls and tabbed paths
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Top row: checkbox + update button
        JPanel controlRow = new JPanel(new BorderLayout(10, 0));
        downloadCacheCheckBox = new JCheckBox("Download Fresh Cache");
        downloadCacheCheckBox.setSelected(Main.getConfigManager().freshCache());
        downloadCacheCheckBox.addItemListener(e -> Main.getConfigManager().setFreshCache(downloadCacheCheckBox.isSelected()));
        controlRow.add(downloadCacheCheckBox, BorderLayout.CENTER);
        controlRow.add(createUpdateAllButton(), BorderLayout.EAST);
        bottomPanel.add(controlRow, BorderLayout.NORTH);

        // Tabbed pane for path configuration
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Collision", createCollisionPathPanel());
        tabbedPane.addTab("Objects", createObjectPathPanel());
        tabbedPane.addTab("Tile Types", createTileTypePathPanel());
        bottomPanel.add(tabbedPane, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        calculateCenter();
        setupKeyBindings(imagePanel);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                upButton.requestFocusInWindow();
            }
        });
        mapView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                upButton.requestFocusInWindow();
                if (graphEditMode) {
                    // Check if in picker mode first
                    if (graphPaletteFrame != null && graphPaletteFrame.isPickerMode()) {
                        handlePickerClick(e);
                    } else {
                        handleGraphClick(e);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (busy()) return;

                // Reset ctrl press tracking
                ctrlPressedNode = null;

                // Check for Ctrl+click node drag in graph edit mode
                if (graphEditMode && e.isControlDown() && SwingUtilities.isLeftMouseButton(e)) {
                    Graph graph = Main.getGraph();
                    if (graph != null) {
                        WorldPoint worldPoint = screenToWorld(e.getPoint());
                        // Use a larger tolerance for easier grabbing
                        int tolerance = Math.max(5, zoomSlider.getValue() / 10);
                        GraphNode node = graph.findNodeAt(worldPoint.getX(), worldPoint.getY(), base.getPlane(), tolerance);
                        if (node != null) {
                            // Track that we Ctrl+pressed on this node
                            ctrlPressedNode = node;
                            draggingNode = node;
                            selectedNode = node;
                            selectedEdge = null;
                            viewPort.setSelectedNodePacked(node.getPacked());
                            viewPort.setSelectedEdgeKey(-1);
                            mapView.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            if (graphPaletteFrame != null) {
                                graphPaletteFrame.selectNode(node);
                            }
                            return;
                        }
                    }
                }

                // Map panning: right-click always, or left-click when not in graph edit mode
                if (SwingUtilities.isRightMouseButton(e) || !graphEditMode) {
                    dragStart = e.getPoint();
                    mapView.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Finalize node drag
                if (draggingNode != null) {
                    // Only save and recalculate if node was actually moved
                    if (nodeWasMoved) {
                        Graph graph = Main.getGraph();
                        if (graph != null) {
                            for (GraphEdge edge : graph.getEdgesForNode(draggingNode)) {
                                graph.calculateEdgeTileTypes(edge);
                            }
                        }
                        saveGraph();
                        if (graphPaletteFrame != null) {
                            graphPaletteFrame.refresh();
                        }
                    }
                    draggingNode = null;
                    // Don't clear ctrlPressedNode here - mouseClicked needs it
                    // nodeWasMoved stays set so mouseClicked knows if drag happened
                }

                dragStart = null;
                mapView.setCursor(Cursor.getDefaultCursor());
            }
        });

        mapView.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (busy()) return;

                // Handle node dragging
                if (draggingNode != null) {
                    WorldPoint worldPoint = screenToWorld(e.getPoint());
                    // Check if position actually changed
                    if (draggingNode.getX() != worldPoint.getX() || draggingNode.getY() != worldPoint.getY()) {
                        draggingNode.setX(worldPoint.getX());
                        draggingNode.setY(worldPoint.getY());
                        nodeWasMoved = true;
                        update();
                    }
                    return;
                }

                // Handle map panning
                if (dragStart == null) return;

                Point current = e.getPoint();
                int deltaX = current.x - dragStart.x;
                int deltaY = current.y - dragStart.y;

                // Convert screen pixels to world tiles
                float pixelsPerCellX = (float) mapView.getWidth() / zoomSlider.getValue();
                float pixelsPerCellY = (float) mapView.getHeight() / zoomSlider.getValue();

                int worldDeltaX = Math.round(-deltaX / pixelsPerCellX);
                int worldDeltaY = Math.round(deltaY / pixelsPerCellY);  // Y inverted

                if (worldDeltaX != 0 || worldDeltaY != 0) {
                    base.setX(base.getX() + worldDeltaX);
                    base.setY(base.getY() + worldDeltaY);
                    calculateCenter();
                    update();
                    dragStart = current;  // Reset for continuous drag
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateTileTypeTooltip(e.getPoint());
                // Update cursor based on picker mode
                if (graphEditMode && graphPaletteFrame != null && graphPaletteFrame.isPickerMode()) {
                    mapView.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                } else if (dragStart == null) {
                    mapView.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        imagePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                upButton.requestFocusInWindow();
            }
        });

        addMouseWheelListener(e -> {
            if(busy())
                return;
            if (e.getWheelRotation() < 0) {
                int val = zoomSlider.getValue() - 10;
                val = val > 0 ? val : 1;
                zoomSlider.setValue(val);
            } else {
                int val = zoomSlider.getValue() + 10;
                val = val < 501 ? val : 1000;
                zoomSlider.setValue(val);
            }
        });

        createMenuBar();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                update();
            }
        });
    }

    private void createMenuBar()
    {
        // Create the menu bar
        JMenuBar menuBar = new JMenuBar();

        // Add a button to the menu bar
        JButton button = new JButton("Settings");
        button.addActionListener(e -> {
            if(settingsFrame == null)
                settingsFrame = new SettingsFrame(this::update);
            settingsFrame.setVisible(true);
        });

        // Add the button and combo box to the menu bar
        menuBar.add(button);

        worldPointField = new JTextField();

        //do something when you hit enter key in text field
//        worldPointField.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                //verify enter key
//                String text = worldPointField.getText();
//                String[] split = text.split(",");
//                if(split.length != 3)
//                    return;
//                try {
//                    int x = Integer.parseInt(split[0].trim());
//                    int y = Integer.parseInt(split[1].trim());
//                    int z = Integer.parseInt(split[2].trim());
//                    center.setX(x);
//                    center.setY(y);
//                    center.setPlane(z);
//                    calculateBase();
//                    update();
//                } catch (NumberFormatException ex) {
//                    ex.printStackTrace();
//                }
//            }
//        });

        //run lambda only when you hit enter key in text field
        worldPointField.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enter");
        worldPointField.getActionMap().put("enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = worldPointField.getText();
                String[] split = text.split(",");
                if(split.length != 3)
                    return;
                try {
                    int x = Integer.parseInt(split[0].trim());
                    int y = Integer.parseInt(split[1].trim());
                    int z = Integer.parseInt(split[2].trim());
                    center.setX(x);
                    center.setY(y);
                    center.setPlane(z);
                    calculateBase();
                    update();
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        });


        //add a little spacing
        menuBar.add(Box.createHorizontalStrut(10));

        menuBar.add(worldPointField);

        // Add viewer mode dropdown
        menuBar.add(Box.createHorizontalStrut(10));
        menuBar.add(new JLabel("View:"));
        menuBar.add(Box.createHorizontalStrut(5));
        viewerModeComboBox = new JComboBox<>(ViewerMode.values());
        viewerModeComboBox.setSelectedItem(currentViewerMode);
        viewerModeComboBox.setMaximumSize(new Dimension(100, 25));
        viewerModeComboBox.addActionListener(e -> {
            currentViewerMode = (ViewerMode) viewerModeComboBox.getSelectedItem();
            updateWebButtonState();
            update();
        });
        menuBar.add(viewerModeComboBox);

        // Add Web toggle button for graph editing (only active in COMBINED mode)
        menuBar.add(Box.createHorizontalStrut(10));
        webButton = new JToggleButton("Web");
        webButton.setToolTipText("Toggle graph editing mode (COMBINED view only)");
        webButton.addActionListener(e -> toggleGraphEditMode());
        webButton.setEnabled(currentViewerMode == ViewerMode.COMBINED);
        menuBar.add(webButton);

        // Set the menu bar for the JFrame
        setJMenuBar(menuBar);
    }

    private JPanel createCollisionPathPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel pathRow = new JPanel(new BorderLayout(5, 0));
        pathRow.add(new JLabel("Path:"), BorderLayout.WEST);
        pathField = new JTextField(Main.getConfigManager().outputPath());
        pathRow.add(pathField, BorderLayout.CENTER);
        panel.add(pathRow, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createObjectPathPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel pathRow = new JPanel(new BorderLayout(5, 0));
        pathRow.add(new JLabel("Path:"), BorderLayout.WEST);
        objectPathField = new JTextField(Main.getConfigManager().objectOutputPath());
        pathRow.add(objectPathField, BorderLayout.CENTER);
        panel.add(pathRow, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTileTypePathPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel pathRow = new JPanel(new BorderLayout(5, 0));
        pathRow.add(new JLabel("Path:"), BorderLayout.WEST);
        tileTypePathField = new JTextField(Main.getConfigManager().tileTypeOutputPath());
        pathRow.add(tileTypePathField, BorderLayout.CENTER);
        panel.add(pathRow, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates the single update button for dumping all maps.
     */
    private JButton updateAllButton;
    private JButton createUpdateAllButton() {
        updateAllButton = new JButton("Update Maps");
        updateAllButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                updateAllButton.setText("Updating...");
                updateAllButton.setEnabled(false);
                revalidate();
                repaint();
            });

            // Save all paths to config
            Main.getConfigManager().setOutputPath(pathField.getText());
            Main.getConfigManager().setObjectOutputPath(objectPathField.getText());
            Main.getConfigManager().setTileTypeOutputPath(tileTypePathField.getText());

            List<String> options = new ArrayList<>();
            options.add("-path");
            options.add(pathField.getText());
            options.add("-objectPath");
            options.add(objectPathField.getText());
            options.add("-tileTypePath");
            options.add(tileTypePathField.getText());
            options.add("-fresh");
            options.add(downloadCacheCheckBox.isSelected() ? "y" : "n");

            ThreadPool.submit(() -> {
                try {
                    Dumper.main(options.toArray(new String[0]));
                    Main.load();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                SwingUtilities.invokeLater(() -> {
                    updateAllButton.setText("Update Maps");
                    updateAllButton.setEnabled(true);
                    revalidate();
                    repaint();
                    update();
                });
            });
        });
        return updateAllButton;
    }

    /**
     * Requests initial focus for the up button (Purpose is to draw focus off of the sliders)
     */
    public void requestInitialFocus()
    {
        upButton.requestFocusInWindow();
    }

    /**
     * Updates the map view based on the current viewer mode.
     */
    public void update() {
        // Check if the appropriate data is available for the current viewer mode
        if (currentViewerMode == ViewerMode.COLLISION && Main.getCollision() == null) {
            return;
        }
        if (currentViewerMode == ViewerMode.TILE_TYPE && Main.getTileTypeMap() == null) {
            return;
        }

        if(busy())
            return;

        worldPointField.setText(center.getX() + "," + center.getY() + "," + center.getPlane());

        current = ThreadPool.submit(() -> {
            viewPort.render(base, mapView.getWidth(), mapView.getHeight(), zoomSlider.getValue(), currentViewerMode);
            ImageIcon imageIcon = new ImageIcon(viewPort.getCanvas());
            mapView.setIcon(imageIcon);
        });
    }

    /**
     * checks if its currently busy rendering a map frame
     * @return true if busy, false otherwise
     */
    private boolean busy()
    {
        return current != null && !current.isDone();
    }

    /**
     * Moves the image in the specified direction.
     * @param direction The direction to move the image.
     */
    private void moveImage(Direction direction) {
        if(busy())
            return;
        switch (direction)
        {
            case NORTH:
                base.north(speedSlider.getValue());
                calculateCenter();
                break;
            case SOUTH:
                base.south(speedSlider.getValue());
                calculateCenter();
                break;
            case EAST:
                base.east(speedSlider.getValue());
                calculateCenter();
                break;
            case WEST:
                base.west(speedSlider.getValue());
                calculateCenter();
                break;

        }
        update();
    }

    /**
     * Calculates the base point of the image.
     */
    private void calculateBase()
    {
        int baseX = center.getX() - zoomSlider.getValue() / 2;
        int baseY = center.getY() - zoomSlider.getValue() / 2;
        base.setX(baseX);
        base.setY(baseY);
    }

    /**
     * Calculates the center point of the image.
     */
    private void calculateCenter()
    {
        int centerX = base.getX() + zoomSlider.getValue() / 2;
        int centerY = base.getY() + zoomSlider.getValue() / 2;
        center.setX(centerX);
        center.setY(centerY);
        center.setPlane(base.getPlane());
    }

    /**
     * Sets up key bindings for the specified component.
     * @param component The component to set up key bindings for.
     */
    private void setupKeyBindings(JComponent component) {
        // Define actions for each arrow key
        Action upAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveImage(Direction.NORTH);
            }
        };

        Action downAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveImage(Direction.SOUTH);
            }
        };

        Action leftAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveImage(Direction.WEST);
            }
        };

        Action rightAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveImage(Direction.EAST);
            }
        };

        Action zeroAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setPlane(0);
            }
        };

        Action oneAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setPlane(1);
            }
        };

        Action twoAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setPlane(2);
            }
        };

        Action threeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setPlane(3);
            }
        };

        Action deleteAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedGraphElement();
            }
        };

        Action escapeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Cancel picker mode
                if (graphPaletteFrame != null && graphPaletteFrame.isPickerMode()) {
                    graphPaletteFrame.cancelPickerMode();
                    return;
                }
                // Cancel pending edge creation
                if (pendingEdgeSource != null) {
                    pendingEdgeSource = null;
                    viewPort.setPendingEdgeSourcePacked(-1);
                    update();
                }
            }
        };

        // Bind the arrow keys to actions
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("UP"), "upAction");
        component.getActionMap().put("upAction", upAction);

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DOWN"), "downAction");
        component.getActionMap().put("downAction", downAction);

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("LEFT"), "leftAction");
        component.getActionMap().put("leftAction", leftAction);

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("RIGHT"), "rightAction");
        component.getActionMap().put("rightAction", rightAction);

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("0"), "zeroAction");
        component.getActionMap().put("zeroAction", zeroAction);

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("1"), "oneAction");
        component.getActionMap().put("oneAction", oneAction);

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("2"), "twoAction");
        component.getActionMap().put("twoAction", twoAction);

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("3"), "threeAction");
        component.getActionMap().put("threeAction", threeAction);

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DELETE"), "deleteAction");
        component.getActionMap().put("deleteAction", deleteAction);

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "escapeAction");
        component.getActionMap().put("escapeAction", escapeAction);
    }

    /**
     * Sets the plane (floor) of the display.
     * @param plane The plane to set the display to.
     */
    private void setPlane(int plane)
    {
        if(busy())
            return;

        if(plane > 3 || plane < 0)
            return;
        SwingUtilities.invokeLater(() -> {
            base.setPlane(plane);
            center.setPlane(plane);
            update();
        });
    }

    /**
     * Updates the tooltip to show tile type info when in Tile Type or Combined mode.
     * @param mousePoint the mouse position in component coordinates
     */
    private void updateTileTypeTooltip(Point mousePoint) {
        // Only show tooltip in Tile Type or Combined mode
        if (currentViewerMode != ViewerMode.TILE_TYPE && currentViewerMode != ViewerMode.COMBINED) {
            mapView.setToolTipText(null);
            return;
        }

        if (Main.getTileTypeMap() == null) {
            mapView.setToolTipText(null);
            return;
        }

        // Convert screen coordinates to world coordinates
        float pixelsPerCellX = (float) mapView.getWidth() / zoomSlider.getValue();
        float pixelsPerCellY = (float) mapView.getHeight() / zoomSlider.getValue();

        int cellX = (int) (mousePoint.x / pixelsPerCellX);
        int cellY = (int) ((mapView.getHeight() - mousePoint.y) / pixelsPerCellY);  // Y inverted

        int worldX = base.getX() + cellX;
        int worldY = base.getY() + cellY;

        byte tileType = Main.getTileTypeMap().getTileType(worldX, worldY, base.getPlane());
        String typeName = TileType.getName(tileType);

        if (typeName != null) {
            mapView.setToolTipText(typeName);
        } else {
            mapView.setToolTipText(null);
        }
    }

    // ==================== Graph Editing Methods ====================

    /**
     * Updates the Web button enabled state based on current viewer mode.
     */
    private void updateWebButtonState() {
        boolean enabled = currentViewerMode == ViewerMode.COMBINED;
        webButton.setEnabled(enabled);
        if (!enabled && graphEditMode) {
            toggleGraphEditMode(); // Turn off if switching away from COMBINED
        }
    }

    /**
     * Toggles graph editing mode on/off.
     */
    private void toggleGraphEditMode() {
        graphEditMode = webButton.isSelected();

        if (graphEditMode) {
            // Open palette window
            if (graphPaletteFrame == null) {
                graphPaletteFrame = new GraphPaletteFrame(
                        this::onPaletteSelectionChanged,
                        this::update
                );
            }
            graphPaletteFrame.refresh();
            graphPaletteFrame.setVisible(true);

            // Set graph in viewport
            viewPort.setGraph(Main.getGraph());
        } else {
            // Hide palette
            if (graphPaletteFrame != null) {
                graphPaletteFrame.setVisible(false);
            }
            // Clear selection state
            pendingEdgeSource = null;
            selectedNode = null;
            selectedEdge = null;
            viewPort.setSelectedNodePacked(-1);
            viewPort.setSelectedEdgeKey(-1);
            viewPort.setPendingEdgeSourcePacked(-1);
        }
        update();
    }

    /**
     * Called when selection changes in the palette.
     */
    private void onPaletteSelectionChanged(Object selection) {
        if (selection instanceof GraphNode) {
            selectedNode = (GraphNode) selection;
            selectedEdge = null;
            viewPort.setSelectedNodePacked(selectedNode.getPacked());
            viewPort.setSelectedEdgeKey(-1);
        } else if (selection instanceof GraphEdge) {
            selectedEdge = (GraphEdge) selection;
            selectedNode = null;
            viewPort.setSelectedEdgeKey(selectedEdge.getEdgeKey());
            viewPort.setSelectedNodePacked(-1);
        } else {
            selectedNode = null;
            selectedEdge = null;
            viewPort.setSelectedNodePacked(-1);
            viewPort.setSelectedEdgeKey(-1);
        }
        update();
    }

    /**
     * Handles mouse clicks on the map when in graph edit mode.
     */
    private void handleGraphClick(MouseEvent e) {
        if (!graphEditMode || busy()) return;

        Graph graph = Main.getGraph();
        if (graph == null) return;

        // If we just finished dragging a node, skip click handling
        if (nodeWasMoved) {
            nodeWasMoved = false;
            ctrlPressedNode = null;
            return;
        }

        // Convert screen to world coordinates
        Point screenPoint = e.getPoint();
        WorldPoint worldPoint = screenToWorld(screenPoint);
        int worldX = worldPoint.getX();
        int worldY = worldPoint.getY();
        int plane = base.getPlane();

        // Calculate click tolerance based on zoom
        int tolerance = Math.max(3, Math.min(10, zoomSlider.getValue() / 20));

        if (e.isControlDown()) {
            // Ctrl+Click: Edge creation mode
            // Use ctrlPressedNode if we clicked on a node (from mousePressed)
            GraphNode clickedNode = ctrlPressedNode;
            ctrlPressedNode = null;  // Clear for next interaction

            // If no node was pressed, try to find one at current position
            if (clickedNode == null) {
                clickedNode = graph.findNodeAt(worldX, worldY, plane, tolerance);
            }

            if (clickedNode != null) {
                if (pendingEdgeSource == null) {
                    // First node selected - start edge creation
                    pendingEdgeSource = clickedNode;
                    viewPort.setPendingEdgeSourcePacked(clickedNode.getPacked());
                    update();
                } else {
                    // Second node selected - create edge
                    if (pendingEdgeSource.getPacked() != clickedNode.getPacked()) {
                        GraphEdge newEdge = graph.addEdge(pendingEdgeSource, clickedNode);
                        if (newEdge != null) {
                            saveGraph();
                            if (graphPaletteFrame != null) {
                                graphPaletteFrame.refresh();
                                graphPaletteFrame.selectEdge(newEdge);
                            }
                            selectedEdge = newEdge;
                            selectedNode = null;
                            viewPort.setSelectedEdgeKey(newEdge.getEdgeKey());
                            viewPort.setSelectedNodePacked(-1);
                        }
                    }
                    // Clear pending state
                    pendingEdgeSource = null;
                    viewPort.setPendingEdgeSourcePacked(-1);
                    update();
                }
            }
        } else {
            // Clear ctrl state on regular click
            ctrlPressedNode = null;
            // Regular click: Select existing or create new node
            GraphNode existingNode = graph.findNodeAt(worldX, worldY, plane, tolerance);

            if (existingNode != null) {
                // Select existing node
                selectedNode = existingNode;
                selectedEdge = null;
                viewPort.setSelectedNodePacked(existingNode.getPacked());
                viewPort.setSelectedEdgeKey(-1);
                if (graphPaletteFrame != null) {
                    graphPaletteFrame.selectNode(existingNode);
                }
            } else {
                // Check if clicking near an edge
                GraphEdge existingEdge = graph.findEdgeNear(worldX, worldY, plane, tolerance);
                if (existingEdge != null) {
                    selectedEdge = existingEdge;
                    selectedNode = null;
                    viewPort.setSelectedEdgeKey(existingEdge.getEdgeKey());
                    viewPort.setSelectedNodePacked(-1);
                    if (graphPaletteFrame != null) {
                        graphPaletteFrame.selectEdge(existingEdge);
                    }
                } else {
                    // Create new node
                    GraphNode newNode = graph.addNode(worldX, worldY, plane);
                    saveGraph();
                    selectedNode = newNode;
                    selectedEdge = null;
                    viewPort.setSelectedNodePacked(newNode.getPacked());
                    viewPort.setSelectedEdgeKey(-1);
                    if (graphPaletteFrame != null) {
                        graphPaletteFrame.refresh();
                        graphPaletteFrame.selectNode(newNode);
                    }
                }
            }
            // Clear pending edge state on regular click
            pendingEdgeSource = null;
            viewPort.setPendingEdgeSourcePacked(-1);
            update();
        }
    }

    /**
     * Converts screen coordinates to world coordinates.
     */
    private WorldPoint screenToWorld(Point screenPoint) {
        float pixelsPerCellX = (float) mapView.getWidth() / zoomSlider.getValue();
        float pixelsPerCellY = (float) mapView.getHeight() / zoomSlider.getValue();

        int cellX = (int) (screenPoint.x / pixelsPerCellX);
        int cellY = (int) ((mapView.getHeight() - screenPoint.y) / pixelsPerCellY);

        int worldX = base.getX() + cellX;
        int worldY = base.getY() + cellY;

        return new WorldPoint(worldX, worldY, base.getPlane());
    }

    /**
     * Deletes the currently selected graph element.
     */
    private void deleteSelectedGraphElement() {
        if (!graphEditMode) return;

        Graph graph = Main.getGraph();
        if (graph == null) return;

        if (selectedNode != null) {
            graph.removeNode(selectedNode);
            selectedNode = null;
            viewPort.setSelectedNodePacked(-1);
        } else if (selectedEdge != null) {
            graph.removeEdge(selectedEdge);
            selectedEdge = null;
            viewPort.setSelectedEdgeKey(-1);
        }

        saveGraph();
        if (graphPaletteFrame != null) {
            graphPaletteFrame.refresh();
            graphPaletteFrame.clearSelection();
        }
        update();
    }

    /**
     * Saves the graph to file.
     */
    private void saveGraph() {
        Graph graph = Main.getGraph();
        if (graph != null) {
            graph.save(Main.getConfigManager().graphOutputPath());
        }
    }

    /**
     * Handles mouse clicks when in picker mode for auto-generation.
     */
    private void handlePickerClick(MouseEvent e) {
        if (graphPaletteFrame == null || !graphPaletteFrame.isPickerMode()) return;

        // Convert screen to world coordinates
        Point screenPoint = e.getPoint();
        WorldPoint worldPoint = screenToWorld(screenPoint);

        // Pass to palette for validation and generation
        graphPaletteFrame.onPointPicked(worldPoint.getX(), worldPoint.getY(), base.getPlane());
    }
}