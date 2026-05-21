package billiards.viewer;

import billiards.geometry.ConvexPolygon;
import billiards.geometry.Rectangle;
import billiards.geometry.Vector2;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import static billiards.utils.Polygon.cleanPolygon;
import static billiards.utils.Polygon.createConvexPolygon;

public final class PolyLoad {
    // WARNING: Global mutable state
    // ------------------------------------------------------------
    private static String fullContent = "";
    // ------------------------------------------------------------

    private final TextArea text = new TextArea();
    private final Button loadButton = new Button();
    private final VBox root = new VBox();
    private final HBox loadHBox = new HBox();
    private final Stage stage = new Stage();
    private final Scene scene = new Scene(root);
    private final Label instruct = new Label();

    private Optional<ConvexPolygon> result;

    public PolyLoad(final String windowTitle, final String buttonText, final String fileName,
                    final Rectangle fullScreen) {

        fullContent = Utils.readFromFile(fileName);

        stage.setScene(scene);

        stage.setTitle(windowTitle);
        stage.setOnCloseRequest(event -> {
            this.result = Optional.empty();
            stage.close();
        });

        text.setPrefColumnCount(40);
        text.setPrefRowCount(10);
        text.setWrapText(true);
        text.setEditable(true);
        text.setFont(Font.font("Monaco", 16));
        text.setText(fullContent);

        instruct.setText(
            "Enter points on separate lines, with the coordinates separated by a space.");
        instruct.setPadding(new Insets(5, 5, 5, 10));

        // We want the text to expand as we make the window bigger
        VBox.setVgrow(text, Priority.ALWAYS);

        loadHBox.getChildren().addAll(loadButton, instruct);

        root.getChildren().addAll(loadHBox, text);
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        loadButton.setText(buttonText);
        Utils.colorButton(loadButton, Color.SKYBLUE, Color.GOLD);
        loadButton.setOnAction(event -> {
            fullContent = text.getText();
            if (fullContent.isEmpty()) {
                this.result = Optional.of(fullScreen.toConvexPolygon());

            } else {
                final String cleaned = cleanPolygon(fullContent);
                final ConvexPolygon poly = createConvexPolygon(cleaned);
                this.result = Optional.of(poly);
            }
            Utils.writeToFile(fileName, fullContent);

            stage.close();
        });
    }

    public Optional<ConvexPolygon> getPolyLoad() {
        stage.showAndWait();
        return this.result;
    }
}
