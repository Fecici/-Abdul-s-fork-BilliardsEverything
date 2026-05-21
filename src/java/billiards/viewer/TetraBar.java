package billiards.viewer;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javaslang.Tuple2;
import javaslang.Tuple6;

import java.util.ArrayList;
import java.util.List;

/**
 * Zhao Yu Li, May 15, 2025.
 * Opens a new window that allows the input of the (x, y) coordinates of a list of point, and calculates the
 * tetrahedrons created from each point.
 * Updated May 16, 2025.
 * Allows the input of multiple coordinates. The x and y values of each coordinate should be in the same line and
 * separated by a singular whitespace character. Different coordinates should be separated by a newline character.
 * Updated May 20, 2025.
 * Added new fields for how many of the results to print, and whether we are calculating a Bar or Tetrahedron.
 * Updated Jun 4, 2025.
 * When this window is opened, can close main window without causing a exception.
 * Added an option to add codes to cover.
 */
public final class TetraBar {
    // WARNING: Global mutable state
    // ------------------------------------------------------------
    private static String coordsDefault = "";
    private static String epsDefault = "0.00000001";
    private static String printCountDefault = "1";
    // ------------------------------------------------------------

    private final TextArea coordsTextArea = new TextArea();
    private final TextField epsTextField = new TextField();
    private final TextField printCountTextField = new TextField();

    private final RadioButton tetraRadio = new RadioButton("Tetra");
    private final RadioButton barRadio = new RadioButton("Bar");

    private final CheckBox drawCheckBox = new CheckBox("Draw");
    private final CheckBox addToCoverCheckBox = new CheckBox("Add to cover");

    private final Stage stage = new Stage();

    private final List<Tuple2<Double, Double>> originalPoints = new ArrayList<>();

    // Just keep tetrahedron points for all the coordinates as a list of tuples. Every three consecutive points belong
    // to a different coordinate.
    private final List<Tuple2<Double, Double>> points = new ArrayList<>();

    private boolean clickedLoad = false;

    public TetraBar(Stage parentStage) {
        if (coordsDefault.isEmpty()) coordsDefault = Utils.readFromFile("tetrahedron.txt");

        epsTextField.setPromptText("epsilon");
        epsTextField.setMaxWidth(100);

        printCountTextField.setPromptText("print count");
        printCountTextField.setMaxWidth(50);
        printCountTextField.setText(printCountDefault);

        coordsTextArea.setPromptText("Coordinates");
        coordsTextArea.setText(coordsDefault);
        coordsTextArea.setPrefHeight(300);
        epsTextField.setText(epsDefault);

        ToggleGroup group = new ToggleGroup();
        tetraRadio.setToggleGroup(group);
        tetraRadio.setSelected(true);
        barRadio.setToggleGroup(group);

        drawCheckBox.setSelected(true);
        addToCoverCheckBox.setSelected(true);

        final VBox root = new VBox(10);
        final Scene scene = new Scene(root);
        final Button loadButton = new Button();
        final HBox hbox = new HBox(10, epsTextField, printCountTextField, tetraRadio, barRadio, drawCheckBox, addToCoverCheckBox, loadButton);

        stage.setScene(scene);

        // Zhao Yu Li, Jun 4, 2025.
        // When closing the main window with this window open, close this window first.
        // Without these two lines, JavaFX will throw an IllegalStateException.
        stage.initModality(Modality.NONE);
        stage.initOwner(parentStage);

        stage.setOnCloseRequest(event -> stage.close());

        Label label = new Label("Enter coordinates on each line. The x and y coordinates should be separated by a whitespace.");
        root.getChildren().addAll(
                label, coordsTextArea, hbox);
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        loadButton.setText("Calculate");
        loadButton.setOnAction(event -> {
            int printCount;

            try {
                printCount = Integer.parseInt(printCountTextField.getText());
            } catch (Exception e) {
                printCount = -1;
            }

            if (printCount < 0) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("TetraBar Invalid Print Count");
                alert.setHeaderText(null);
                alert.setContentText("Print count must be a non-negative integer.");
                alert.show();
                return;
            }

            originalPoints.clear();
            points.clear();

            coordsDefault = coordsTextArea.getText();
            epsDefault = epsTextField.getText();
            printCountDefault = printCountTextField.getText();

            final String[] coords = coordsTextArea.getText().split("\n");
            final double eps = Double.parseDouble(epsTextField.getText());

            for (String coord : coords) {
                if (coord.startsWith("//") || coord.trim().isEmpty()) continue;

                String[] coordValue = coord.trim().split(" ");

                assert coordValue.length == 2;

                final double x = Double.parseDouble(coordValue[0]);
                final double y = Double.parseDouble(coordValue[1]);

                originalPoints.add(new Tuple2<>(x, y));

                // Coordinate calculations for Tetrahedron
                if (tetraRadio.isSelected()) {
                    final double x1_out = x - (eps * Math.sqrt(3)/2);
                    final double y1_out = y + (eps / 2);
                    final double x2_out = x + (eps * Math.sqrt(3)/2);
                    final double y2_out = y + (eps / 2);
                    final double y3_out = y - eps;

                    points.add(new Tuple2<>(x1_out, y1_out));
                    points.add(new Tuple2<>(x2_out, y2_out));
                    points.add(new Tuple2<>(x, y3_out));
                }

                // Coordinate calculations for Bar
                if (barRadio.isSelected()) {
                    final double x1_out = x - eps;
                    final double x2_out = x + eps;

                    points.add(new Tuple2<>(x1_out, y));
                    points.add(new Tuple2<>(x2_out, y));
                }
            }

            Utils.writeToFile("tetrabar.txt", coordsDefault);

            this.clickedLoad = true;
            stage.close();
        });
    }

    public Tuple6<List<Tuple2<Double, Double>>, List<Tuple2<Double, Double>>, Integer, Integer, Boolean, Boolean> getVaryParams() {
        // Wait till the stage is closed
        stage.showAndWait();
        int step = 0;

        if (clickedLoad) {
            if (tetraRadio.isSelected()) step = 3;
            if (barRadio.isSelected()) step = 2;
            this.clickedLoad = false;
            return new Tuple6<>(this.originalPoints, this.points, step,
                    Integer.parseInt(printCountTextField.getText()), drawCheckBox.isSelected(), addToCoverCheckBox.isSelected());
        } else {
            return new Tuple6<>(this.originalPoints, this.points, -1, -1, false, false);
        }
    }
}
