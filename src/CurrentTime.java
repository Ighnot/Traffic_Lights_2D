import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;

/**
 * CurrentTime.java
 * @author John Leckie (original)
 * CMSC335, Dec 2023, Project 3 — 2D Grid Edition
 *
 * A Runnable that updates a JLabel with the current system time
 * every second. Runs in its own daemon thread for the lifetime of
 * the simulation.
 *
 * Format: H:mm:ss (24-hour, no leading zero on hour)
 */
public class CurrentTime implements Runnable {

    /** Date formatter — H:m:s = 24-hour without zero-padding. */
    private final SimpleDateFormat df = new SimpleDateFormat("H:mm:ss");

    /** The label to update on each tick. */
    private final JLabel lblTimeStamp;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * CurrentTime - parameterized constructor.
     * Creates a time-stamp label and adds it to the provided content pane.
     *
     * @param contentPane (JPanel) parent panel that will display the label
     */
    public CurrentTime(JPanel contentPane) {
        lblTimeStamp = new JLabel();
        lblTimeStamp.setHorizontalAlignment(SwingConstants.CENTER);
        lblTimeStamp.setForeground(Color.WHITE);
        lblTimeStamp.setFont(new Font("SansSerif", Font.PLAIN, 13));
        contentPane.add(lblTimeStamp, BorderLayout.CENTER);
    }

    // -----------------------------------------------------------------------
    // Runnable
    // -----------------------------------------------------------------------

    /**
     * run - updates the time label every 1000 ms.
     * Runs indefinitely; the thread is a daemon so it exits with the JVM.
     */
    @Override
    public void run() {
        while (true) {
            final String timeStr = "System time: " + df.format(new Date());

            // Push label update onto the Event Dispatch Thread
            SwingUtilities.invokeLater(() -> lblTimeStamp.setText(timeStr));

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
