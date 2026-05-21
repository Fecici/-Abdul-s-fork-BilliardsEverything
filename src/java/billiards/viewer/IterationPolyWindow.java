package billiards.viewer;

import billiards.geometry.ConvexPolygon;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import static billiards.utils.Polygon.cleanPolygon;
import static billiards.utils.Polygon.createConvexPolygon;

/**
 * Zhao Yu Li, May 27, 2025.
 * A window for entering the coordinates of a polygon. The polygon will be used to check for intersections with code
 * sequences that result from iteration calculations.
 */
public final class IterationPolyWindow {
    // WARNING: Global mutable state
    // ------------------------------------------------------------
    private static String fullContent = "";
    private static final String fileName = "iterationPoly.txt";
    // ------------------------------------------------------------

    private final TextArea text = new TextArea();
    private final Stage stage = new Stage();

    public IterationPolyWindow() {
        fullContent = Utils.readFromFile(fileName);

        VBox root = new VBox();
        Scene scene = new Scene(root);
        stage.setScene(scene);

        stage.setTitle("Iteration Polygon");
        stage.setOnCloseRequest(event -> {
            Utils.writeToFile(fileName, text.getText());
            stage.close();
        });

        text.setPrefColumnCount(40);
        text.setPrefRowCount(10);
        text.setWrapText(true);
        text.setEditable(true);
        text.setFont(Font.font("Monaco", 16));
        text.setText(fullContent);

        Label instruct = new Label();
        instruct.setText(
                "Enter points on separate lines, with the coordinates separated by a space.");
        instruct.setPadding(new Insets(5, 5, 5, 10));

        // We want the text to expand as we make the window bigger
        VBox.setVgrow(text, Priority.ALWAYS);

        HBox loadHBox = new HBox();
        loadHBox.getChildren().addAll(instruct);

        root.getChildren().addAll(loadHBox, text);
        root.setSpacing(10);
        root.setPadding(new Insets(10));
    }

    public void show() {
        this.stage.show();
    }

    public Optional<ConvexPolygon> getPolygon() {
        fullContent = text.getText();
        Optional<ConvexPolygon> result;

        if (fullContent.isEmpty()) {
            result = Optional.empty();
        } else {
            final String cleaned = cleanPolygon(fullContent);
            result = Optional.of(createConvexPolygon(cleaned));
        }
        Utils.writeToFile(fileName, fullContent);

        return result;
    }
}
