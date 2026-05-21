package billiards.viewer;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import billiards.wrapper.Wrapper;

public final class ProgressWithStatus {
    private final HBox root = new HBox();
    private final VBox leftPane = new VBox();
    private final Button cancelButton = new Button();
    private final ProgressBar progressBar = new ProgressBar();
    private final Label progressText = new Label();
    private final Scene scene = new Scene(root);
    private final Stage stage = new Stage();
    
    private final Task<?> task;
    private final ReadOnlyDoubleProperty progressProperty;
    private final long offset;
    private final String formatString;
    
    // We don't care what task return type is
    public ProgressWithStatus(final Task<?> task, String formatString, long offset) {
    	progressBar.setMaxWidth(Double.MAX_VALUE);
    	progressText.setMaxWidth(Double.MAX_VALUE);
    	leftPane.setSpacing(10);
    	leftPane.setPrefWidth(200);
    	leftPane.getChildren().addAll(progressBar, progressText);
    	cancelButton.setMaxHeight(Double.MAX_VALUE);
    	Utils.colorButton(cancelButton, Color.TOMATO, Color.SALMON);
        root.setSpacing(10);
        root.getChildren().addAll(leftPane, cancelButton);
        root.setPadding(new Insets(10));

        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            task.cancel();
            stage.close();
        });

        // Block from accessing the main window until this stage is closed
        stage.initModality(Modality.APPLICATION_MODAL);

        this.task = task;
        this.offset = offset;
        this.formatString = formatString;
        this.progressProperty = task.progressProperty();
        this.progressProperty.addListener(new ChangeListener<Number>() {
        	@Override
        	public void changed(ObservableValue<? extends Number> observableValue, Number n1, Number n2) {
        		syncProgress();
        	}
        });
        
        cancelButton.setText("Cancel");
        cancelButton.setOnAction(event -> {
            Wrapper.backend_cancel();
            task.cancel();
            stage.close();
        });
        
        progressText.setText("No status provided by task");
    }

    private void syncProgress() {
    	if (progressProperty == null) {
    		progressText.setText("N/A");
    		progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
    	} else {
    		long curChunk = Math.round(progressProperty.get() * task.getTotalWork()) + offset;
    		long endChunk = Math.round(task.getTotalWork()) + offset;
    		progressText.setText(String.format(formatString, curChunk, endChunk));
    		progressBar.setProgress(progressProperty.get());
    	}
    }
    
    public void close() {
        stage.close();
    }

    public void show() {
        stage.centerOnScreen();
        stage.show();
    }
}
