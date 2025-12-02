package osrs.dev.ui;

import osrs.dev.Main;
import osrs.dev.graph.Graph;
import osrs.dev.graph.WebGenerator;
import osrs.dev.util.ThreadPool;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Progress dialog for water web generation.
 * Shows progress bar, phase info, and cancel button.
 */
public class WebGeneratorDialog extends JDialog {

    private final JProgressBar progressBar;
    private final JLabel phaseLabel;
    private final JLabel detailLabel;
    private final JButton cancelButton;

    private WebGenerator generator;
    private Consumer<Graph> onComplete;
    private Runnable onCancelled;

    public WebGeneratorDialog(Frame owner) {
        super(owner, "Generating Water Web...", true);
        setSize(400, 180);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(350, 25));
        mainPanel.add(progressBar, BorderLayout.NORTH);

        // Status panel
        JPanel statusPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        phaseLabel = new JLabel("Initializing...");
        phaseLabel.setFont(phaseLabel.getFont().deriveFont(Font.BOLD));
        detailLabel = new JLabel(" ");
        statusPanel.add(phaseLabel);
        statusPanel.add(detailLabel);
        mainPanel.add(statusPanel, BorderLayout.CENTER);

        // Cancel button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancelGeneration());
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * Starts the web generation process.
     */
    public void startGeneration(int seedX, int seedY, int plane, int nodeSpacing,
                                int collisionBuffer, Consumer<Graph> onComplete, Runnable onCancelled) {
        this.onComplete = onComplete;
        this.onCancelled = onCancelled;

        // Create generator with progress callback
        generator = new WebGenerator(plane, this::updateProgress);
        generator.setNodeSpacing(nodeSpacing);
        generator.setCollisionBuffer(collisionBuffer);

        // Run in background thread
        ThreadPool.submit(() -> {
            try {
                Graph result = generator.generate(seedX, seedY);

                SwingUtilities.invokeLater(() -> {
                    dispose();
                    if (result != null && onComplete != null) {
                        onComplete.accept(result);
                    } else if (result == null && onCancelled != null) {
                        onCancelled.run();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    dispose();
                    JOptionPane.showMessageDialog(getOwner(),
                            "Error during generation: " + e.getMessage(),
                            "Generation Error",
                            JOptionPane.ERROR_MESSAGE);
                    if (onCancelled != null) {
                        onCancelled.run();
                    }
                });
            }
        });

        // Show dialog (blocks until disposed)
        setVisible(true);
    }

    /**
     * Updates the progress display.
     */
    private void updateProgress(WebGenerator.ProgressUpdate update) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(update.percent);
            phaseLabel.setText("Phase: " + update.phase);
            detailLabel.setText(update.detail);
        });
    }

    /**
     * Cancels the generation process.
     */
    private void cancelGeneration() {
        if (generator != null) {
            generator.cancel();
        }
        cancelButton.setEnabled(false);
        cancelButton.setText("Cancelling...");
    }
}
