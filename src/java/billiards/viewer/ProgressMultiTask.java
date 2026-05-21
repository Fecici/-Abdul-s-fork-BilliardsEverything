package billiards.viewer;

import billiards.wrapper.Wrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

// Progress bar which can be used across multiple (zero or more) tasks. 
// Must be updated manually, as it is not synced to the task it currently tracks
// Main use is to provide a way to monitor the progress of a list of tasks, and cancel the current one
public final class ProgressMultiTask {
    private final HBox root = new HBox();
    private final VBox leftPane = new VBox();
    private final Button cancelButton = new Button();
    private final ProgressBar progressBar = new ProgressBar();
    private final Label progressText = new Label();
    private final Scene scene = new Scene(root);
    private final Stage stage = new Stage();
    
    
    private Task<?> task; // We don't care what task return type is
    private boolean boundTask = false;
    private long completed;
    private boolean cancelled = false;
    private final long total;
    private final String formatString;

    
    public ProgressMultiTask(String formatString, boolean allowCancel, long offset, long total) {
    	progressBar.setMaxWidth(Double.MAX_VALUE);
    	progressText.setMaxWidth(Double.MAX_VALUE);
    	leftPane.setSpacing(10);
    	leftPane.setPrefWidth(200);
    	leftPane.getChildren().addAll(progressBar, progressText);
    	cancelButton.setMaxHeight(Double.MAX_VALUE);
    	Utils.colorButton(cancelButton, Color.TOMATO, Color.SALMON);
        root.setSpacing(10);
        if(allowCancel) {
            root.getChildren().addAll(leftPane, cancelButton);
        } else {
            root.getChildren().add(leftPane);
        }
        root.setPadding(new Insets(10));

        stage.setScene(scene);
        // IMPORTANT: This progress must be closed from the enclosing scope, not from here.
        stage.setOnCloseRequest(event -> {
            cancelled = true;
            if(boundTask) this.task.cancel();
            //stage.close();
        });

        // Block from accessing the main window until this stage is closed
        stage.initModality(Modality.APPLICATION_MODAL);

        this.completed = offset;
        this.total = total;
        this.formatString = formatString;
        
        // IMPORTANT: This progress must be closed from the enclosing scope, not from here. This allows us to block until cancellation is complete
        cancelButton.setText("Cancel");
        cancelButton.setOnAction(event -> {
            cancelled = true;
            Wrapper.backend_cancel();
            if(boundTask) this.task.cancel();
            //stage.close();
        });
        
        syncProgress();
    }

    private void syncProgress() {
        progressText.setText(String.format(formatString, completed, total));
        progressBar.setProgress((double) completed / total);
    }
    
    public void changeTask(final Task<?> task) {
        this.task = task;
        this.boundTask = true;
    }

    public void increment(final long step) {
        completed += step;
        completed = Math.min(total, completed);
        syncProgress();
    }

    public void resetProgress() {
        completed = 0;
        syncProgress();
    }

    public void close() {
        stage.close();
    }

    public void show() {
        stage.centerOnScreen();
        stage.show();
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
    
