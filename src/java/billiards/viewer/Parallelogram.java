package billiards.viewer;

import billiards.geometry.ConvexPolygon;
import billiards.geometry.Vector2;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public final class Parallelogram {
    // WARNING: Global mutable state
    // ------------------------------------------------------------
    private static String xMinDefault = "";
    private static String xMaxDefault = "";

    private static String zMinDefault = "";
    private static String zMaxDefault = "";
    // ------------------------------------------------------------

    private final TextField xMinTextField = new TextField();
    private final TextField xMaxTextField = new TextField();

    private final TextField zMinTextField = new TextField();
    private final TextField zMaxTextField = new TextField();
    private final Button loadButton = new Button();

    private final Stage stage = new Stage();
    private final HBox root = new HBox();
    private final Scene scene = new Scene(root);

    private Optional<ConvexPolygon> result;

    public Parallelogram() {
        xMinTextField.setPromptText("X min");
        xMaxTextField.setPromptText("X max");

        zMinTextField.setPromptText("Z min");
        zMaxTextField.setPromptText("Z max");

        xMinTextField.setText(xMinDefault);
        xMaxTextField.setText(xMaxDefault);

        zMinTextField.setText(zMinDefault);
        zMaxTextField.setText(zMaxDefault);

        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            this.result = Optional.empty();
            stage.close();
        });

        root.getChildren().addAll(
            xMinTextField, xMaxTextField, zMinTextField, zMaxTextField, loadButton);
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        loadButton.setText("Load");
        loadButton.setOnAction(event -> {

            xMinDefault = xMinTextField.getText();
            xMaxDefault = xMaxTextField.getText();

            zMinDefault = zMinTextField.getText();
            zMaxDefault = zMaxTextField.getText();

            final double xMin = Math.toRadians(Double.parseDouble(xMinTextField.getText()));
            final double xMax = Math.toRadians(Double.parseDouble(xMaxTextField.getText()));

            final double zMin = Math.toRadians(Double.parseDouble(zMinTextField.getText()));
            final double zMax = Math.toRadians(Double.parseDouble(zMaxTextField.getText()));

            final double xMinzMin = Math.PI - xMin - zMin;
            final double xMinzMax = Math.PI - xMin - zMax;
            final double xMaxzMin = Math.PI - xMax - zMin;
            final double xMaxzMax = Math.PI - xMax - zMax;

            final ImmutableList<Vector2> points = FastList.newListWith(Vector2.create(xMin, xMinzMin),
                                                                       Vector2.create(xMin, xMinzMax),
                                                                       Vector2.create(xMax, xMaxzMax),
                                                                       Vector2.create(xMax, xMaxzMin))
                                                      .toImmutable();

            final ConvexPolygon poly = ConvexPolygon.create(points);

            this.result = Optional.of(poly);
            stage.close();
        });
    }

    public Optional<ConvexPolygon> getParallelogram() {
        // Wait till the stage is closed
        stage.showAndWait();
        return this.result;
    }
}
