package osrs.dev.ui;

import osrs.dev.ui.viewport.ViewPort;
import osrs.dev.util.WorldPoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class UIFrame extends JFrame {
    private final JLabel imageLabel;
    private final JSlider zoomSlider;
    private final JSlider speedSlider;
    private final ViewPort viewPort;
    private final WorldPoint base = new WorldPoint(3207, 3213, 0);
    private final WorldPoint center = new WorldPoint(0,0,0);

    public UIFrame() {
        setTitle("Collision Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(800, 600);
        setLocationRelativeTo(null); // Center on screen

        viewPort = new ViewPort();

        // Main panel for image display
        JPanel imagePanel = new JPanel(new BorderLayout());
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        add(imagePanel, BorderLayout.CENTER);

        // Control panel on the right
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setPreferredSize(new Dimension(150, 300));

        // Navigation panel (buttons in plus shape)
        JPanel navigationPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Set up button size and add the Up button (use ^ for up)
        JButton upButton = new JButton("^");
        upButton.setPreferredSize(new Dimension(50, 50));  // Make buttons more square
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        navigationPanel.add(upButton, gbc);

        // Add the Left button (use < for left)
        JButton leftButton = new JButton("<");
        leftButton.setPreferredSize(new Dimension(50, 50)); // Square size
        gbc.gridx = 0;
        gbc.gridy = 1;
        navigationPanel.add(leftButton, gbc);

        // Add the Right button (use > for right)
        JButton rightButton = new JButton(">");
        rightButton.setPreferredSize(new Dimension(50, 50)); // Square size
        gbc.gridx = 2;
        gbc.gridy = 1;
        navigationPanel.add(rightButton, gbc);

        // Add the Down button (use v for down)
        JButton downButton = new JButton("v");
        downButton.setPreferredSize(new Dimension(50, 50));  // Square size
        gbc.gridx = 1;
        gbc.gridy = 2;
        navigationPanel.add(downButton, gbc);

        controlPanel.add(navigationPanel, BorderLayout.CENTER);

        // Zoom slider
        zoomSlider = new JSlider(JSlider.VERTICAL, 5, 500, 100);
        zoomSlider.setMajorTickSpacing(50);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(true);
        zoomSlider.addChangeListener(e -> {
            calculateBase();
            calculateCenter();
            update();
        });
        controlPanel.add(zoomSlider, BorderLayout.EAST);

        // Zoom slider
        speedSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 5);
        speedSlider.setMajorTickSpacing(5);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        add(speedSlider, BorderLayout.NORTH);

        add(controlPanel, BorderLayout.EAST);

        // Button actions (You can add the necessary functionality here)
        upButton.addActionListener(e -> moveImage(Direction.NORTH));
        downButton.addActionListener(e -> moveImage(Direction.SOUTH));
        rightButton.addActionListener(e -> moveImage(Direction.EAST));
        leftButton.addActionListener(e -> moveImage(Direction.WEST));
        calculateCenter();
        setupKeyBindings(imagePanel);

        addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0) {
                int val = zoomSlider.getValue() - 10;
                val = val > 0 ? val : 1;
                zoomSlider.setValue(val);
            } else {
                int val = zoomSlider.getValue() + 10;
                val = val < 501 ? val : 500;
                zoomSlider.setValue(val);
            }
        });
    }

    public void update() {
        viewPort.render(base, imageLabel.getWidth(), imageLabel.getHeight(), zoomSlider.getValue());
        ImageIcon imageIcon = new ImageIcon(viewPort.getCanvas());
        imageLabel.setIcon(imageIcon);
    }

    private void moveImage(Direction direction) {
        switch (direction)
        {
            case NORTH:
                base.north(speedSlider.getValue());
                break;
            case SOUTH:
                base.south(speedSlider.getValue());
                break;
            case EAST:
                base.east(speedSlider.getValue());
                break;
            case WEST:
                base.west(speedSlider.getValue());
                break;

        }
        update();
    }

    private void calculateBase()
    {
        int baseX = center.getX() - zoomSlider.getValue() / 2;
        int baseY = center.getY() - zoomSlider.getValue() / 2;
        base.setX(baseX);
        base.setY(baseY);
    }


    private void calculateCenter()
    {
        int centerX = base.getX() + zoomSlider.getValue() / 2;
        int centerY = base.getY() + zoomSlider.getValue() / 2;
        center.setX(centerX);
        center.setY(centerY);
        center.setPlane(base.getPlane());
    }

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

        // Bind the arrow keys to actions
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("UP"), "upAction");
        component.getActionMap().put("upAction", upAction);

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("DOWN"), "downAction");
        component.getActionMap().put("downAction", downAction);

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("LEFT"), "leftAction");
        component.getActionMap().put("leftAction", leftAction);

        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("RIGHT"), "rightAction");
        component.getActionMap().put("rightAction", rightAction);
    }

    private enum Direction
    {
        NORTH,
        SOUTH,
        EAST,
        WEST
    }
}