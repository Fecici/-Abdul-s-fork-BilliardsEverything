package billiards.viewer;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;


import java.util.concurrent.atomic.AtomicInteger;

import billiards.wrapper.Wrapper;

public final class Progress {
    private final HBox root = new HBox();
    private final Button cancelButton = new Button();
    private final ProgressBar progressBar = new ProgressBar();
    private final Scene scene = new Scene(root);
    private final Stage stage = new Stage();

    // We don't care what task return type is
    public Progress(final Task<?> task) {
        root.setSpacing(10);
        root.getChildren().addAll(progressBar, cancelButton);
        root.setPadding(new Insets(10));

        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            task.cancel();
            stage.close();
        });

        // Block from accessing the main window until this stage is closed
        stage.initModality(Modality.APPLICATION_MODAL);

        progressBar.progressProperty().bind(task.progressProperty());

        cancelButton.setText("Cancel");
        cancelButton.setOnAction(event -> {
            Wrapper.backend_cancel();
            task.cancel();
            stage.close();
        });
    }

    public void close() {
        stage.close();
    }

    public void show() {
        stage.show();
    }

    public void incrementWindowCount(AtomicInteger count) {
        stage.setOnCloseRequest(event -> count.incrementAndGet());
    }
}
