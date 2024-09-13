package osrs.dev.ui;

import osrs.dev.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SettingsFrame extends JFrame {

    private final JTextField bgColorField;
    private final JTextField gridColorField;
    private final JTextField collisionColorField;
    private final JTextField wallColorField;
    private final Runnable callback;

    public SettingsFrame(Runnable callback) {
        setTitle("Settings");
        setSize(400, 300);
        setLocationRelativeTo(null);

        this.callback = callback;

        // Create the layout
        setLayout(new GridLayout(5, 2));

        // Labels
        JLabel bgColorLabel = new JLabel("Background Color (Hex):");
        JLabel gridColorLabel = new JLabel("Grid Color (Hex):");
        JLabel collisionColorLabel = new JLabel("Collision Color (Hex):");
        JLabel wallColorLabel = new JLabel("Wall Color (Hex):");

        // Text fields
        bgColorField = new JTextField();
        gridColorField = new JTextField();
        collisionColorField = new JTextField();
        wallColorField = new JTextField();

        bgColorField.setText(Main.getConfigManager().bgColorText());
        gridColorField.setText(Main.getConfigManager().gridColorText());
        collisionColorField.setText(Main.getConfigManager().collisionColorText());
        wallColorField.setText(Main.getConfigManager().wallColorText());

        // Apply button
        JButton applyButton = new JButton("Apply Settings");
        applyButton.addActionListener(new ApplyButtonListener());

        // Add components to the frame
        add(bgColorLabel);
        add(bgColorField);
        add(gridColorLabel);
        add(gridColorField);
        add(collisionColorLabel);
        add(collisionColorField);
        add(wallColorLabel);
        add(wallColorField);
        add(new JLabel());  // Empty cell
        add(applyButton);
    }

    // Action listener for the apply button
    private class ApplyButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String bgColor = bgColorField.getText();
            String gridColor = gridColorField.getText();
            String collisionColor = collisionColorField.getText();
            String wallColor = wallColorField.getText();

            Main.getConfigManager().setBgColor(bgColor);
            Main.getConfigManager().setGridColor(gridColor);
            Main.getConfigManager().setCollisionColor(collisionColor);
            Main.getConfigManager().setWallColor(wallColor);
            System.out.println("Settings applied.");
            callback.run();
            setVisible(false);
        }
    }
}