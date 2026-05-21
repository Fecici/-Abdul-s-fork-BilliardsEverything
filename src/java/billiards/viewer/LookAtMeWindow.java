package billiards.viewer;

import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public final class LookAtMeWindow {
    private final String extra_info="some instruction.....";
    private final VBox root = new VBox();
    private final TextArea text = new TextArea();
    private final Stage stage = new Stage();
    private final Scene scene = new Scene(root);
    public LookAtMeWindow(final String windowTitle){
        stage.setScene(scene);
        stage.setTitle(windowTitle);
        stage.setOnCloseRequest(event -> stage.close());
        text.setPrefColumnCount(60);
        text.setPrefRowCount(20);
        text.setWrapText(true);
        text.setEditable(false);
        text.setFont(Font.font("Monaco", 16));

        VBox.setVgrow(text, Priority.ALWAYS);
        text.setText(extra_info);
        root.getChildren().addAll( text);

    }
    public void close() {
        stage.close();
    }
    public void show() {
        stage.show();
    }
}
