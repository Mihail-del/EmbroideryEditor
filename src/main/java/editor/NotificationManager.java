package editor;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * Manages toast notifications and overlay status alerts in the application.
 * Follows the Single Responsibility Principle by isolating notification display and animation logic from the controller.
 */
public class NotificationManager {

    private final Label autoSaveLabel;
    private Timeline autoSaveNotificationTimeline;

    public NotificationManager(Label autoSaveLabel) {
        this.autoSaveLabel = autoSaveLabel;
    }

    /**
     * Triggers a smooth slide-and-fade notification animation for the autosave label.
     */
    public void showAutoSaveNotification() {
        if (autoSaveLabel == null) return;

        if (autoSaveNotificationTimeline != null) {
            autoSaveNotificationTimeline.stop();
        }

        autoSaveLabel.setOpacity(0.0);
        autoSaveLabel.setTranslateY(-10);
        autoSaveLabel.setVisible(true);

        autoSaveNotificationTimeline = new Timeline(
            new KeyFrame(Duration.millis(300),
                new KeyValue(autoSaveLabel.opacityProperty(), 1.0),
                new KeyValue(autoSaveLabel.translateYProperty(), 0.0)
            ),
            new KeyFrame(Duration.millis(2300),
                new KeyValue(autoSaveLabel.opacityProperty(), 1.0),
                new KeyValue(autoSaveLabel.translateYProperty(), 0.0)
            ),
            new KeyFrame(Duration.millis(2800),
                new KeyValue(autoSaveLabel.opacityProperty(), 0.0),
                new KeyValue(autoSaveLabel.translateYProperty(), -10.0)
            )
        );

        autoSaveNotificationTimeline.setOnFinished(e -> autoSaveLabel.setVisible(false));
        autoSaveNotificationTimeline.play();
    }
}
