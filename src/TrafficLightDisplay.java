import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.*;

/**
 * TrafficLightDisplay.java
 * @author John Leckie (original), refactored for 2D grid
 * CMSC335, Dec 2023, Project 3 — 2D Grid Edition
 *
 * A Runnable that controls the color-cycling behavior of one TrafficLight.
 * Each intersection gets its own TrafficLightDisplay thread.
 *
 * Light timing is randomized per instance so that the 36 lights on the
 * 6x6 grid are not all in sync with each other. This makes the simulation
 * more realistic and prevents all cars from stopping or moving together.
 *
 * Color cycle: GREEN → YELLOW → RED → GREEN (repeating)
 *
 * The display thread also attempts to load PNG icons from the resources
 * folder and sets them on the TrafficLight JLabel. If the PNGs are not
 * present (e.g., on first run before assets are added), the icon is simply
 * skipped — the GridCanvas renders light color independently via color string.
 */
public class TrafficLightDisplay implements Runnable {

    // -----------------------------------------------------------------------
    // Inner enum — light color state machine
    // -----------------------------------------------------------------------

    /**
     * LightColors - enum representing the three possible traffic light states.
     * Provides a utility to pick a random starting color so all lights don't
     * begin the simulation in the same state.
     */
    enum LightColors {
        GREEN, YELLOW, RED;

        /**
         * getRandomColor - returns a random LightColors value.
         * Used to stagger light phases at simulation start.
         *
         * @return random LightColors constant
         */
        public static LightColors getRandomColor() {
            Random random = new Random();
            return values()[random.nextInt(values().length)];
        }
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /** Current color state of this light. */
    private LightColors lColor;

    /** The TrafficLight model object this display controls. */
    private final TrafficLight tLight;

    /** Icon loaded from resources PNG, applied to the JLabel (optional). */
    private ImageIcon lightImg;

    /** How long (ms) to hold the red phase — randomized per instance. */
    private final int redWaitTime;

    /** How long (ms) to hold the green phase — randomized per instance. */
    private final int greenWaitTime;

    /** Loop control flags. */
    private volatile boolean isRunning;
    private volatile boolean isStopped;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * TrafficLightDisplay - parameterized constructor.
     * Randomizes timing and picks a starting color for this light.
     *
     * @param tLight (TrafficLight) the model object to drive
     */
    public TrafficLightDisplay(TrafficLight tLight) {
        this.tLight   = tLight;
        this.isRunning = true;
        this.isStopped = false;

        // Randomize timing so lights are not synchronized across the grid
        redWaitTime   = ThreadLocalRandom.current().nextInt(6000, 10000);
        greenWaitTime = ThreadLocalRandom.current().nextInt(10000, 16000);

        // Pick a random starting color to stagger the 36 lights
        setLightColor();
        setLightImage();
    }

    // -----------------------------------------------------------------------
    // Runnable implementation
    // -----------------------------------------------------------------------

    /**
     * run - main loop for this light thread.
     * Cycles the light color and sleeps for the appropriate duration
     * based on which phase is active.
     */
    @Override
    public void run() {
        while (!isStopped) {
            checkIfPaused();

            if (isRunning) {
                changeColor();
                try {
                    // Hold each phase for the appropriate duration
                    if (lColor == LightColors.GREEN) {
                        Thread.sleep(greenWaitTime);
                    } else if (lColor == LightColors.YELLOW) {
                        Thread.sleep(3000);      // yellow is always 3 seconds
                    } else {
                        Thread.sleep(redWaitTime);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                // While paused, sleep briefly to avoid spinning on the CPU
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }

            checkIfStopped();
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * changeColor - advances the light to the next phase in the cycle
     * (GREEN → YELLOW → RED → GREEN) and updates the model and icon.
     */
    private void changeColor() {
        switch (lColor) {
            case GREEN:  lColor = LightColors.YELLOW; break;
            case YELLOW: lColor = LightColors.RED;    break;
            case RED:    lColor = LightColors.GREEN;  break;
        }
        setLightImage();
        updateColor();
    }

    /**
     * setLightColor - picks a random starting color for this light
     * and pushes it to the model.
     */
    private void setLightColor() {
        lColor = LightColors.getRandomColor();
        updateColor();
    }

    /**
     * updateColor - writes the current color string to the TrafficLight model.
     * Car threads read this string to decide how to react at the intersection.
     */
    private void updateColor() {
        switch (lColor) {
            case GREEN:  tLight.setColor("green");  break;
            case YELLOW: tLight.setColor("yellow"); break;
            case RED:    tLight.setColor("red");    break;
        }
    }

    /**
     * setLightImage - attempts to load a PNG icon for the current color
     * from the resources folder and applies it to the JLabel.
     * Silently skips if the resource is not found — the GridCanvas
     * renders light color independently using the color string.
     */
    private void setLightImage() {
        String resourceName;
        switch (lColor) {
            case GREEN:  resourceName = "GreenLight.png";  break;
            case YELLOW: resourceName = "YellowLight.png"; break;
            case RED:    resourceName = "redLight.png";    break;
            default:     return;
        }
        try {
            java.net.URL url = TrafficAnalysisGUI.class.getResource("resources/" + resourceName);
            if (url != null) {
                lightImg = new ImageIcon(url);
                tLight.setIcon(lightImg);
            }
        } catch (Exception ignored) {
            // PNG not found — GridCanvas will render the color directly
        }
    }

    /**
     * checkIfPaused - reads the pause flag from the TrafficLight model
     * and sets isRunning accordingly.
     */
    private void checkIfPaused() {
        isRunning = !tLight.isPaused;
    }

    /**
     * checkIfStopped - reads the stop flag from the TrafficLight model.
     * If stopped, clears the icon on the JLabel and sets isStopped
     * to exit the run() loop.
     */
    private void checkIfStopped() {
        isStopped = tLight.isStopped();
        if (isStopped) {
            tLight.setIcon(null);
        }
    }
}
