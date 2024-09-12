package osrs.dev.util;

/**
 * A simple cli progress bar that can be used to display the progress of a task.
 */
public class ProgressBar {
    private final int total;
    private final int barLength;

    /**
     * Creates a new progress bar with the given total and bar length.
     * @param total The total number of steps in the task.
     * @param barLength The length of the progress bar.
     */
    public ProgressBar(int total, int barLength) {
        this.total = total;
        this.barLength = barLength;
        update(0);
    }

    /**
     * Updates the progress bar with the current progress.
     * @param currentProgress The current progress of the task.
     */
    public void update(int currentProgress) {
        if (currentProgress > total) {
            currentProgress = total;
        }
        int filledLength = (int) ((double) currentProgress / total * barLength);
        int percentage = (int) ((double) currentProgress / total * 100);
        String bar = "=".repeat(filledLength) + " ".repeat(barLength - filledLength);

        System.out.print("\rProgress: [" + bar + "] " + percentage + "%");
        if (currentProgress == total) {
            System.out.print("\n");
        }
    }
}
