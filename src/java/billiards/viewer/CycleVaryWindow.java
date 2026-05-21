package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.CodeType;
import billiards.codeseq.Storage;
import billiards.geometry.Interval;
import billiards.wrapper.Wrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.text.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import billiards.geometry.ConvexPolygon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.Tuple3;
import javaslang.Tuple7;
import javaslang.collection.Array;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.sorted.mutable.TreeSortedSet;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import static billiards.codeseq.CodeType.OSNO;
import static billiards.utils.Polygon.cleanPolygon;
import static billiards.utils.Polygon.createConvexPolygon;
import static billiards.viewer.Viewer.parseOBOFile;

public class CycleVaryWindow {
    // WARNING: Global mutable state
    // ------------------------------------------------------------
    public static String polygonString = "";
    public static Integer BoundCSMax = 300;
    public static Integer BoundOSOMax = 50;
    public static Integer BoundOSNOMax = 36;
    public static Integer BoundCSMaxSS = 800;
    public static Integer BoundOSOMaxSS = 300;
    public static Integer BoundOSNOMaxSS = 150;
    public static Integer BoundCSstep = 0;
    public static Integer BoundOSOstep = 0;
    public static Integer BoundOSNOstep = 0;
    public static Integer Reps = 1;
    public static Boolean ColorCycle = true;
    // ------------------------------------------------------------

    private final TextArea polygonText = new TextArea();
    private final TextField CSbox = new TextField();
    private final TextField OSObox = new TextField();
    private final TextField OSNObox = new TextField();
    private final TextField CSsbox = new TextField();
    private final TextField OSOsbox = new TextField();
    private final TextField OSNOsbox = new TextField();
    private final TextField CSstepbox = new TextField();
    private final TextField OSOstepbox = new TextField();
    private final TextField OSNOstepbox = new TextField();
    private final CheckBox colorCycleBox = new CheckBox();
    private final TextField repBox = new TextField();
    public final Stage stage = new Stage();
    private final CheckBox magnifyCheckBox = new CheckBox();
    private final TextField magnifyTextField = new TextField();
    private final CheckBox useRepsCheckBox = new CheckBox();

    private final RadioButton regularModeRadioButton = new RadioButton("Regular");
    private final RadioButton middleModeRadioButton = new RadioButton("Middle");
    private final RadioButton firstMidLastModeRadioButton = new RadioButton("First, Middle, Last");
    private final ToggleGroup modesToggleGroup = new ToggleGroup();
    private final TextField numToPrintTextField = new TextField();

    private final CheckBox addToAllPositiveCheckbox = new CheckBox();
    private final CheckBox addToPlusMinusCheckbox = new CheckBox();

    private final CodeArea coordinateCodeArea = new CodeArea();

    private final TextField lineNumTextField = new TextField();
    private Integer lineNumber = null;

    private final TextField startTextField = new TextField();
    private final TextField stepTextField = new TextField();
    private final TextField endTextField = new TextField();

    private final TextField cyclesTextfield = new TextField();
    private final TextField emptySquaresTextfield = new TextField();

    private final CheckBox CSCb = new CheckBox("CS");
    private final CheckBox OSNOCb = new CheckBox("OSNO");
    private final CheckBox OSOCb = new CheckBox("OSO");

    private final TextField subdivisionsTextfield = new TextField();
    private final TextField subdivisionsStepTextfield = new TextField();
    private final TextField shotsText = new TextField();

    private final TextField changeEmptySquaresText = new TextField();
    private final TextField changeMagnificationText = new TextField();

    private final Viewer viewer;

    private final String coordsFileName;

    public CycleVaryWindow(final String windowTitle, final String buttonText, final String fileName, final String boundsFileName, final String stepFileName, final String coordsFileName, final Viewer viewer) {
        this.viewer = viewer;
        this.coordsFileName = coordsFileName;
        polygonString = Utils.readFromFile(fileName);
        String[] boundTokens = Utils.readFromFile(boundsFileName).trim().split(" ");
        String[] stepTokens = Utils.readFromFile(stepFileName).trim().split(" ");
        if (boundTokens.length >= 9) {
            try {
                BoundCSstep = Integer.parseInt(boundTokens[6]);
                BoundOSOstep = Integer.parseInt(boundTokens[7]);
                BoundOSNOstep = Integer.parseInt(boundTokens[8]);
            } catch (NumberFormatException e) {
                BoundCSstep = 0;
                BoundOSOstep = 0;
                BoundOSNOstep = 0;
            }
        }
        if (boundTokens.length >= 6) {
            try {
                BoundCSMaxSS = Integer.parseInt(boundTokens[3]);
                BoundOSOMaxSS = Integer.parseInt(boundTokens[4]);
                BoundOSNOMaxSS = Integer.parseInt(boundTokens[5]);
            } catch (NumberFormatException e) {
                BoundCSMaxSS = 222;
                BoundOSOMaxSS = 222;
                BoundOSNOMaxSS = 222;
            }
        }
        if (stepTokens.length >= 3) {
            try {
                BoundCSstep = Integer.parseInt(boundTokens[0]);
                BoundOSOstep = Integer.parseInt(boundTokens[1]);
                BoundOSNOstep = Integer.parseInt(boundTokens[2]);
            } catch (NumberFormatException e) {
                BoundCSstep = 0;
                BoundOSOstep = 0;
                BoundOSNOstep = 0;
            }
        }

        VBox root = new VBox();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle(windowTitle);

        polygonText.setPrefColumnCount(40);
        polygonText.setPrefRowCount(5);
        polygonText.setWrapText(true);
        polygonText.setEditable(false);
        polygonText.setFont(Font.font("Monaco", 16));
        polygonText.setText(polygonString);
        VBox.setVgrow(polygonText, Priority.ALWAYS);

        HBox coordinatesHBox = getCoordinatesHBox();

        coordinateCodeArea.setWrapText(true);
        coordinateCodeArea.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 16px;");
        coordinateCodeArea.replaceText(Utils.readFromFile(coordsFileName));
        coordinateCodeArea.setPrefHeight(300);

        coordinateCodeArea.setParagraphGraphicFactory(LineNumberFactory.get(coordinateCodeArea));

        VirtualizedScrollPane<CodeArea> vsPane = new VirtualizedScrollPane<>(coordinateCodeArea);

        // Sync the polygon with the cover polygon
        CoverWindow.polyStringProperty.addListener((o, oldValue, newValue) -> {
            polygonString = newValue;
            polygonText.setText(polygonString);
        });

        Label instruct = new Label();
        instruct.setText("The following polygon is synced with the current cover");
        instruct.setPadding(new Insets(5, 5, 5, 10));

        Label codel = new Label();
        codel.setText("Code length:");
        CSbox.setPrefWidth(150);
        CSbox.setText(BoundCSMax.toString());
        Label CSl = new Label();
        CSl.setText("CS max:");
        OSObox.setPrefWidth(150);
        OSObox.setText(BoundOSOMax.toString());
        Label OSOl = new Label();
        OSOl.setText("OSO max:");
        OSNObox.setPrefWidth(150);
        OSNObox.setText(BoundOSNOMax.toString());
        Label OSNOl = new Label();
        OSNOl.setText("OSNO max:");

        Label ssuml = new Label();
        ssuml.setText("Side sum:");
        CSsbox.setPrefWidth(150);
        CSsbox.setText(BoundCSMaxSS.toString());
        Label CSsl = new Label();
        CSsl.setText("CS max:");
        OSOsbox.setPrefWidth(150);
        OSOsbox.setText(BoundOSOMaxSS.toString());
        Label OSOsl = new Label();
        OSOsl.setText("OSO max:");
        OSNOsbox.setPrefWidth(150);
        OSNOsbox.setText(BoundOSNOMaxSS.toString());
        Label OSNOsl = new Label();
        OSNOsl.setText("OSNO max:");

        Label stepl = new Label();
        stepl.setText("SS step:");
        CSstepbox.setPrefWidth(150);
        CSstepbox.setText(BoundCSstep.toString());
        Label CSstepl = new Label();
        CSstepl.setText("CS step:");
        OSOstepbox.setPrefWidth(150);
        OSOstepbox.setText(BoundOSOstep.toString());
        Label OSOstepl = new Label();
        OSOstepl.setText("OSO step:");
        OSNOstepbox.setPrefWidth(150);
        OSNOstepbox.setText(BoundOSNOstep.toString());
        Label OSNOstepl = new Label();
        OSNOstepl.setText("OSNO step:");

        Label repl = new Label();
        repl.setText("Reps");
        repBox.setPrefWidth(50);
        repBox.setText(Reps.toString());

        colorCycleBox.setIndeterminate(false);
        colorCycleBox.setAllowIndeterminate(false);
        colorCycleBox.setSelected(ColorCycle);
        colorCycleBox.setText("Cycle colors");

        addToAllPositiveCheckbox.setIndeterminate(false);
        addToAllPositiveCheckbox.setAllowIndeterminate(false);
        addToAllPositiveCheckbox.setSelected(false);
        addToAllPositiveCheckbox.setText("Add to all-positive");

        addToPlusMinusCheckbox.setIndeterminate(false);
        addToPlusMinusCheckbox.setAllowIndeterminate(false);
        addToPlusMinusCheckbox.setSelected(false);
        addToPlusMinusCheckbox.setText("Add to plus/minus");

        HBox instructHBox = new HBox();
        instructHBox.getChildren().add(instruct);

        HBox maxHBox = new HBox(10);
        maxHBox.getChildren().addAll(codel, CSl, CSbox, OSOl, OSObox, OSNOl, OSNObox);
        maxHBox.setPadding(new Insets(0, 10, 10, 0));
        maxHBox.setAlignment(Pos.CENTER);

        HBox maxOptHBox = new HBox(10);
        maxOptHBox.getChildren().addAll(ssuml, CSsl, CSsbox, OSOsl, OSOsbox, OSNOsl, OSNOsbox);
        maxOptHBox.setPadding(new Insets(0, 10, 10, 0));
        maxOptHBox.setAlignment(Pos.CENTER);

        useRepsCheckBox.setIndeterminate(false);
        useRepsCheckBox.setAllowIndeterminate(false);
        useRepsCheckBox.setSelected(false);
        useRepsCheckBox.setText("Use Reps");

        HBox maxStepHBox = new HBox(10);
        maxStepHBox.getChildren().addAll(stepl, CSstepl, CSstepbox, OSOstepl, OSOstepbox, OSNOstepl, OSNOstepbox);
        maxStepHBox.setPadding(new Insets(0, 10, 10, 0));
        maxStepHBox.setAlignment(Pos.CENTER);

        // Zhao Yu Li, Jul 8, 2025.
        // Optional magnification after every rep, and arbitrary magnification
        magnifyTextField.setPrefColumnCount(3);
        magnifyTextField.setText("2");

        magnifyCheckBox.setText("Magnification:");

        Label subdivisionsLabel = new Label("Subdivisions:");
        Label subdivisionsStepLabel = new Label("Subdivisions Step:");
        subdivisionsTextfield.setPrefColumnCount(3);
        subdivisionsTextfield.setText("3");
        subdivisionsStepTextfield.setPrefColumnCount(3);
        subdivisionsStepTextfield.setText("1");

        HBox repsHBox = new HBox(10, useRepsCheckBox, repl, repBox, magnifyCheckBox, magnifyTextField, subdivisionsLabel, subdivisionsTextfield, subdivisionsStepLabel, subdivisionsStepTextfield);
        repsHBox.setAlignment(Pos.CENTER_LEFT);

        Label superAustinControlLabel = new Label("Super Austin Vary Controls");
        VBox superAustinControlVBox = new VBox(10, superAustinControlLabel, repsHBox, maxStepHBox);

        VBox maxVBox = new VBox(10);
        maxVBox.getChildren().addAll(maxHBox, maxOptHBox, superAustinControlVBox);
        Button loadButton = new Button();

        //controlVBox.getChildren().addAll(loadHBox, overrideBox, autoCoverBox);
        HBox controlHBox = new HBox(20);
        controlHBox.getChildren().addAll(colorCycleBox, addToAllPositiveCheckbox, addToPlusMinusCheckbox);
        controlHBox.setPadding(new Insets(0, 10, 10, 0));
        controlHBox.setAlignment(Pos.CENTER_LEFT);

        Label cyclesLabel = new Label("Cycles:");
        cyclesTextfield.setPromptText("Cycles");
        cyclesTextfield.setPrefWidth(40);
        cyclesTextfield.setText("1");

        Label emptySquaresLabel = new Label("Empty Squares Per Cycle");
        emptySquaresTextfield.setPrefWidth(40);
        emptySquaresTextfield.setText("100");

        Label changeDigitsLabel = new Label("Change Empty Squares By");
        changeEmptySquaresText.setPrefWidth(40);
        changeEmptySquaresText.setText("-10");

        Label changeMagnificationLabel = new Label("Change Magnification By");
        changeMagnificationText.setPrefWidth(40);
        changeMagnificationText.setText("2");

        CSCb.setSelected(true);
        shotsText.setPrefWidth(35);
        shotsText.setText("4");
        HBox codeTypeHBox = new HBox(10, CSCb, OSNOCb, OSOCb, shotsText, new Label("Shots"));
        codeTypeHBox.setAlignment(Pos.CENTER_LEFT);

        HBox cyclesHBox = new HBox(10, cyclesLabel, cyclesTextfield, emptySquaresLabel, emptySquaresTextfield, changeDigitsLabel, changeEmptySquaresText, changeMagnificationLabel, changeMagnificationText, loadButton);
        cyclesHBox.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(instructHBox, polygonText, coordinatesHBox, vsPane, getLineNavigateHBox(), maxVBox, getModesHBox(), controlHBox, codeTypeHBox, cyclesHBox);
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        loadButton.setText(buttonText);
        Utils.colorButton(loadButton, Color.SKYBLUE, Color.GOLD);
        loadButton.setOnAction(event -> {
            ColorCycle = colorCycleBox.isSelected();
            try {
                Reps = Integer.parseInt(repBox.getText().trim());
                BoundCSMax = Integer.parseInt(CSbox.getText().trim());
                BoundOSOMax = Integer.parseInt(OSObox.getText().trim());
                BoundOSNOMax = Integer.parseInt(OSNObox.getText().trim());
                BoundCSMaxSS = Integer.parseInt(CSsbox.getText().trim());
                BoundOSOMaxSS = Integer.parseInt(OSOsbox.getText().trim());
                BoundOSNOMaxSS = Integer.parseInt(OSNOsbox.getText().trim());
                BoundCSstep = Integer.parseInt(CSstepbox.getText().trim());
                BoundOSOstep = Integer.parseInt(OSOstepbox.getText().trim());
                BoundOSNOstep = Integer.parseInt(OSNOstepbox.getText().trim());
            } catch (NumberFormatException e) {
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("AutoPolyVary Error");
                alert.setHeaderText("Non-integer value in input box");
                alert.setContentText("Please enter a single integer into each of the '[SequenceType] Max' boxes.");
                alert.showAndWait();
                return;
            }
            polygonString = polygonText.getText();
            final String lines = cleanPolygon(polygonString);
            final ConvexPolygon poly = createConvexPolygon(lines);
            //Utils.writeToFile(fileName, polygonString);
            Utils.writeToFile(boundsFileName, String.format("%d %d %d %d %d %d %d %d %d", BoundCSMax, BoundOSOMax, BoundOSNOMax, BoundCSMaxSS, BoundOSOMaxSS, BoundOSNOMaxSS, BoundCSstep, BoundOSOstep, BoundOSNOstep));

            CycleVaryFunction(poly);

            stage.close();
        });
    }

    private HBox getCoordinatesHBox() {
        Label coordinateLabel = new Label("Coordinates:");
        Button loadOBO = new Button("Load OBO");
        loadOBO.setOnAction(event -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load One By One File");
            final File file = fileChooser.showOpenDialog(stage);

            if (file != null) {
                ArrayList<String> fileCodeSequences = parseOBOFile(file.toPath());
                StringBuilder content = new StringBuilder();

                for (String line : fileCodeSequences) {
                    content.append(line).append('\n');
                }

                coordinateCodeArea.replaceText(content + coordinateCodeArea.getText());
            }
        });

        Button clearCoordinatesButton = new Button("Clear");
        clearCoordinatesButton.setOnAction(event -> {
            coordinateCodeArea.clear();
        });

        HBox coordinatesHBox = new HBox(10, coordinateLabel, loadOBO, clearCoordinatesButton);
        coordinatesHBox.setAlignment(Pos.CENTER_LEFT);
        return coordinatesHBox;
    }

    public void show() {
        stage.show();
    }

    public boolean getMagnificationIsSelected() {
        return this.magnifyCheckBox.isSelected();
    }

    public Double getMagnification() {
        double magnification;

        try {
            magnification = Double.parseDouble(magnifyTextField.getText().trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }

        if (magnification <= 0) {
            Text alertText = new Text("Magnification must be a positive value.");
            alertText.setWrappingWidth(350);

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.getDialogPane().setContent(alertText);
            alert.getDialogPane().setPadding(new Insets(10));
            alert.getDialogPane().setPrefWidth(400);
            alert.showAndWait();
            return null;
        }

        return magnification;
    }

    public boolean getUseReps() {
        return useRepsCheckBox.isSelected();
    }

    public boolean allPositiveIsSelected() {
        return this.addToAllPositiveCheckbox.isSelected();
    }

    public boolean plusMinusIsSelected() {
        return this.addToPlusMinusCheckbox.isSelected();
    }

    private HBox getModesHBox() {
        regularModeRadioButton.setToggleGroup(modesToggleGroup);
        middleModeRadioButton.setToggleGroup(modesToggleGroup);
        firstMidLastModeRadioButton.setToggleGroup(modesToggleGroup);

        regularModeRadioButton.setSelected(true);

        numToPrintTextField.setPrefColumnCount(3);
        numToPrintTextField.setText("2");

        HBox modesHBox = new HBox(10, regularModeRadioButton, middleModeRadioButton, firstMidLastModeRadioButton, numToPrintTextField);
        modesHBox.setAlignment(Pos.CENTER_LEFT);

        return modesHBox;
    }

    public int getMode() {
        if (regularModeRadioButton.isSelected()) return 0;
        if (middleModeRadioButton.isSelected()) return 1;
        if (firstMidLastModeRadioButton.isSelected()) return 2;
        return -1;
    }

    public Integer getNumGroupToPrint() {
        if (numToPrintTextField.getText().trim().isEmpty()) {
            Text text = new Text("Please enter a non-negative integer");
            text.setWrappingWidth(350);

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("AutoPolyVary Error");
            alert.setHeaderText("Empty field");
            alert.getDialogPane().setContent(text);
            alert.getDialogPane().setPrefWidth(400);
            alert.showAndWait();
            return null;
        }

        int numGroupToPrint;

        try {
            numGroupToPrint =  Integer.parseInt(numToPrintTextField.getText());
        } catch (NumberFormatException e) {
            throw new NumberFormatException();
        }

        if (numGroupToPrint < 0) {
            Text text = new Text("Please enter a non-negative integer");
            text.setWrappingWidth(350);

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("AutoPolyVary Error");
            alert.setHeaderText("Negative integer value for the number of groups to print");
            alert.getDialogPane().setContent(text);
            alert.getDialogPane().setPrefWidth(400);
            alert.showAndWait();
            return null;
        }

        return numGroupToPrint;
    }

    private HBox getLineNavigateHBox() {
        final Button backwardButton = new Button("Backward");
        final Button forwardButton = new Button("Forward");
        final Button goToLineButton = new Button("Go");

        lineNumTextField.setPromptText("Line");
        lineNumTextField.setPrefColumnCount(3);

        backwardButton.setOnAction(event -> {
            if (coordinateCodeArea.getText().trim().isEmpty()) {
                showMoveScreenAlert("Please enter at least one coordinate.");
                return;
            }

            if (lineNumber == null) lineNumber = 1;
            else {
                Tuple3<Integer, Integer, Integer> startStepEnd = getStartStepEnd(getCoordinatesListLength());
                Integer start = startStepEnd._1;
                Integer step = startStepEnd._2;
                Integer end = startStepEnd._3;

                if (start == null || step == null || end == null) return;

                lineNumber -= step;

                if (lineNumber < start) lineNumber = end;
            }

            lineNumTextField.setText(Integer.toString(lineNumber));
            moveScreenToLine(lineNumber - 1);
        });

        forwardButton.setOnAction(event -> {
            if (coordinateCodeArea.getText().trim().isEmpty()) {
                showMoveScreenAlert("Please enter at least one coordinate.");
                return;
            }

            if (lineNumber == null) lineNumber = 1;
            else {
                Tuple3<Integer, Integer, Integer> startStepEnd = getStartStepEnd(getCoordinatesListLength());
                Integer start = startStepEnd._1;
                Integer step = startStepEnd._2;
                Integer end = startStepEnd._3;

                if (start == null || step == null || end == null) return;

                lineNumber += step;

                if (lineNumber > end) lineNumber = start;
            }

            lineNumTextField.setText(Integer.toString(lineNumber));
            moveScreenToLine(lineNumber - 1);
        });

        goToLineButton.setOnAction(event -> {
            if (coordinateCodeArea.getText().trim().isEmpty()) {
                showMoveScreenAlert("Please enter at least one coordinate.");
                return;
            }

            int userLineNumber = getLineNumber();

            lineNumber = userLineNumber;

            lineNumTextField.setText(Integer.toString(userLineNumber));
            moveScreenToLine(userLineNumber - 1);
        });

        startTextField.setPromptText("Start");
        startTextField.setPrefWidth(60);
        stepTextField.setPromptText("Step");
        stepTextField.setPrefWidth(60);
        endTextField.setPromptText("End");
        endTextField.setPrefWidth(60);

        return new HBox(10, startTextField, stepTextField, endTextField, backwardButton, lineNumTextField, goToLineButton, forwardButton);
    }

    /**
     * <b>Zhao Yu Li</b><br>
     * <b>Jul 2, 2025</b>
     * <p>
     *     Extracts the integer entered in <code>textField</code>. Throws a <code>NumberFormatException</code> if what
     *     was entered cannot be parsed as an integer.
     * </p>
     * @param textField The <code>TextField</code> to extract the integer from.
     * @return The extracted integer from <code>textField</code>.
     */
    private int extractNumberFromTextField(TextField textField) {
        int userNumber;
        String numberString = textField.getText().trim();

        if (numberString.isEmpty()) numberString = "1";

        try {
            userNumber = Integer.parseInt(numberString);
        } catch (NumberFormatException e) {
            throw new NumberFormatException();
        }

        return userNumber;
    }

    /**
     * <b>Zhao Yu Li</b><br>
     * <b>Jun 29, 2025</b>
     * <p>
     *     Gets the user entered line number. Defaults to 1 if the user did not enter anything.
     * </p>
     * @return The line number entered by the user, or 1 if the user did not enter anything.
     */
    private int getLineNumber() {
        return extractNumberFromTextField(lineNumTextField);
    }

    /**
     * <b>Zhao Yu Li</b><br>
     * <b>Jun 29, 2025</b>
     * <p>
     *     Displays an information alert dialogue with <code>content</code>.
     * </p>
     * @param content The message to display to the user.
     */
    private void showMoveScreenAlert(String content) {
        final Text alertText = new  Text(content);
        alertText.setWrappingWidth(350);

        final Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Cycle Vary");
        alert.setHeaderText("Move Screen");
        alert.getDialogPane().setContent(alertText);
        alert.getDialogPane().setPadding(new Insets(10));
        alert.getDialogPane().setMaxWidth(400);

        alert.showAndWait();
    }

    /**
     * <b>Zhao Yu Li</b><br>
     * <b>Jun 29, 2025</b>
     * <p>
     *     Center the screen to the <code>index</code>'th coordinate.
     * </p>
     * @param index The index of the coordinate to move to.
     */
    private void moveScreenToLine(int index) {
        if (index < 0) {
            showMoveScreenAlert("Line number must be a positive, non-zero integer.");
            return;
        }

        String[] coordinateStrings = coordinateCodeArea.getText().trim().split("\n");

        if (coordinateStrings.length <= index) {
            showMoveScreenAlert("Line number must be between 1 and " + coordinateStrings.length);
            return;
        }

        String[] coordinateString = coordinateStrings[index].trim().split(" ");

        viewer.moveScreen(coordinateString[0], coordinateString[1]);
    }

    /**
     * <b>Zhao Yu Li</b><br>
     * <b>Jun 29, 2025</b>
     * <p>
     *     Sets the line number text field and the internal state to <code>lineNumber</code>.
     * </p>
     * @param lineNumber The line number to set to.
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
        lineNumTextField.setText(Integer.toString(lineNumber));
    }

    private int getCoordinatesListLength() {
        return coordinateCodeArea.getText().trim().split("\n").length;
    }

    public Tuple3<Integer, Integer, Integer> getStartStepEnd(int defaultEnd) {
        final String lineStartText = startTextField.getText();
        final String lineStepText = stepTextField.getText();
        final String lineEndText = endTextField.getText();
        // The indexes that the user sees (will be converted later)
        int startIdxUser;
        int stepIdxUser;
        int endIdxUser;
        // 2024-05-06 Fixed broken logic (Not all fields filled was not detected properly)
        if (lineStartText.isEmpty() && lineEndText.isEmpty() && lineStepText.isEmpty()) { // All fields empty
            startIdxUser = 1;
            stepIdxUser = 1;
            endIdxUser = defaultEnd;
        } else if (lineStartText.isEmpty() || lineEndText.isEmpty() || lineStepText.isEmpty()) { // At least 1, but not all fields are empty
            showEnterLineNumberErrorAutoVary();
            return Tuple.of(null, null, null);
        } else { // All fields filled
            try {
                startIdxUser = Integer.parseInt(lineStartText);
            } catch (final NumberFormatException e) {
                showInvalidNumberError(lineStartText);
                return Tuple.of(null, null, null);
            }
            try {
                endIdxUser = Integer.parseInt(lineEndText);
            } catch (final NumberFormatException e) {
                showInvalidNumberError(lineEndText);
                return Tuple.of(null, null, null);
            }
            try {
                stepIdxUser = Math.min(defaultEnd, Integer.parseInt(lineStepText)); // Max step is all elements
            } catch (final NumberFormatException e) {
                showInvalidNumberError(lineStepText);
                return Tuple.of(null, null, null);
            }
            if (!(1 <= startIdxUser && startIdxUser <= endIdxUser)) {
                showInvalidLineRangeError(defaultEnd);
                return Tuple.of(null, null, null);
            }
            if(stepIdxUser < 1) {
                showStepErrorAutoVary();
                return Tuple.of(null, null, null);
            }
        }

        return Tuple.of(startIdxUser, stepIdxUser, Math.min(endIdxUser, defaultEnd));
    }

    private static void showEnterLineNumberErrorAutoVary() {
        final Alert alert = new Alert(AlertType.ERROR);

        alert.setTitle("Enter Line Numbers");
        alert.setHeaderText("Enter Line Numbers");
        alert.setContentText("Please enter start and end line numbers for AutoPolyVary.");
        alert.showAndWait();
    }

    private static void showStepErrorAutoVary() {
        final Alert alert = new Alert(AlertType.ERROR);

        alert.setTitle("Bad Step Value");
        alert.setHeaderText("Bad Step Value");
        alert.setContentText("AutoPolyVary step value must be >= 1");
        alert.showAndWait();

    }

    private static void showInvalidLineNumberError(final int max) {
        final Alert alert = new Alert(AlertType.ERROR);

        alert.setTitle("Invalid Line Number");
        alert.setHeaderText("Invalid Line Number");
        alert.setContentText(String.format("Line number must be between 1 and %d.", max));
        alert.showAndWait();
    }

    private static void showInvalidLineRangeError(final int max) {
        final Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Invalid Line Range");
        alert.setHeaderText("Invalid Line Range");
        alert.setContentText(String.format("Must have 1 <= Start <= End <= %d.", max));
        alert.showAndWait();
    }

    private static void showInvalidNumberError(final String invalidNumber) {
        final Alert alert = new Alert(AlertType.ERROR);

        alert.setTitle("Invalid Number");
        alert.setHeaderText("Invalid Number");
        alert.setContentText(String.format("Input %s is an invalid number.", invalidNumber));
        alert.showAndWait();
    }

    private void CycleVaryFunction(ConvexPolygon polygon) {
        final int cycles = extractNumberFromTextField(cyclesTextfield);
        final int originalSubdivision = extractNumberFromTextField(subdivisionsTextfield);
        final SimpleObjectProperty<Integer> step = new SimpleObjectProperty<>();
        final ProgressMultiTask cyclesProgress = new ProgressMultiTask("CycleVary Cycles %d out of %d", false, 0, cycles);
        final ProgressMultiTask repsProgress = new ProgressMultiTask("CycleVary Reps %d out of %d", false, 0, useRepsCheckBox.isSelected() ? Reps : 1);
        final Array<Color> cycleColors = Array.of(
                Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA,
                Color.CHOCOLATE, Color.ORANGE, Color.PINK, Color.LIME,
                Color.PURPLE, Color.TURQUOISE);

        final ExecutorService executor = Executors.newFixedThreadPool(Utils.numThreads);
        final double originalScale = viewer.map.getScale();

        step.setValue(-1);
        step.addListener((o, oldVal, newVal) -> {
            if (newVal == -1) {
                shutdown(cyclesProgress, repsProgress, executor);
                return;
            }

            final int rep = newVal % Reps;

            if (newVal != 0 && rep == 0) {
                final int empties = extractNumberFromTextField(emptySquaresTextfield);
                final int deltaEmpties = extractNumberFromTextField(changeEmptySquaresText);
                final int deltaMagnification = extractNumberFromTextField(changeMagnificationText);

                viewer.coverWindow.saveToFile();

                final int digits;
                final int magnifications;
                try {
                    digits = Integer.parseInt(CoverWindow.digitsString);
                    magnifications = Integer.parseInt(CoverWindow.magnificationsString);
                } catch (final NumberFormatException e) {
                    throw new RuntimeException(e);
                }

                final String cleanedPolygon = cleanPolygon(polygonString);
                final String cleanedStablesPre = CoverWindow.cleanStables(CoverWindow.stablesString, viewer.pool);
                final Tuple2<String, String> cleanedTriplesPre = CoverWindow.cleanTriples(CoverWindow.triplesString, viewer.pool);

                final String cleanedTriples = cleanedTriplesPre._1;
                final String cleanedStables = (cleanedStablesPre + '\n' + cleanedTriplesPre._2).trim();

                String newCoordinates = Wrapper.getNotFilledCoordinates(cleanedPolygon, cleanedStables, cleanedTriples, digits, magnifications, empties, true, viewer.pool.pointer, newVal >= Reps * cycles);

                coordinateCodeArea.replaceText(newCoordinates);

                // Zhao Yu Li, Jul 29, 2025.
                // Prints out "Covered" if all squares have been filled.
                // Move exit check after updating the code area so we can see the coordinates of the newly found empty
                // squares after the end of the last cycle. (Whereas before we would see the coordinates from the second
                // last cycle.)
                if (newVal >= Reps * cycles || newCoordinates.isEmpty()) {
                    if (newCoordinates.isEmpty()) System.out.println("Covered");

                    viewer.loadCover("cover", executor);
                    shutdown(cyclesProgress, repsProgress, executor);
                    return;
                } else {
                    System.out.println("Not covered");
                }

                cyclesProgress.increment(1);
                repsProgress.resetProgress();
                viewer.map.setScale(originalScale);
                subdivisionsTextfield.setText(originalSubdivision + "");
                emptySquaresTextfield.setText(empties + deltaEmpties + "");
                viewer.coverWindow.magnificationsTextField.setText(magnifications + deltaMagnification + "");

                viewer.coverWindow.saveToFile();
            }

            repsProgress.increment(1);
            final Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer> curVals = Tuple.of(polygon, BoundCSMax, BoundOSOMax, BoundOSNOMax,
                    Math.max(0, BoundCSMaxSS + BoundCSstep * rep),
                    Math.max(0, BoundOSOMaxSS + BoundOSOstep * rep),
                    Math.max(0, BoundOSNOMaxSS + BoundOSNOstep * rep));
            final Optional<Color> curCol;

            if(ColorCycle) curCol = Optional.of(cycleColors.get(rep % cycleColors.length()));
            else curCol = Optional.empty();

            autoCycleVaryFunction(curVals, Optional.of(step), curCol, true, true, executor);
        });
        cyclesProgress.show();
        repsProgress.show();
        step.setValue(0);
    }

    public void shutdown(final ProgressMultiTask cyclesProgress, final ProgressMultiTask repsProgress, final ExecutorService executor) {
        executor.shutdown();
        Utils.writeToFile(coordsFileName, coordinateCodeArea.getText());
        cyclesProgress.close();
        repsProgress.close();
    }

    public void autoCycleVaryFunction(final Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer> polyVals,
                                      final Optional<SimpleObjectProperty<Integer>> step, final Optional<Color> colorOpt,
                                      final boolean overrideSS, final boolean autoCover, final ExecutorService executor
    ) {

        // Zhao Yu Li, Jun 27, 2025.
        // Replaced code block with function call.
        Tuple3<Integer, Integer, Integer> startStepEnd = getStartStepEnd(getCoordinatesListLength());
        if (startStepEnd._1 == null) return;

        // Order is CSmax, OSOmax, OSNOmax, CSmaxSS, OSOmaxSS, OSNOmaxSS
        final int[] maxList = {polyVals._2, polyVals._3, polyVals._4, polyVals._5, polyVals._6, polyVals._7};
        ConvexPolygon area = polyVals._1;

        final int startIdx = startStepEnd._1 - 1;
        final int stepIdx = startStepEnd._2;
        final int endIdx = startStepEnd._3 - 1;

        final int subdivisions = Integer.parseInt(subdivisionsTextfield.getText());
        final int subdivisionsStep = Integer.parseInt(subdivisionsStepTextfield.getText());
        subdivisionsTextfield.setText((subdivisions + subdivisionsStep) + "");
        final int shots = Integer.parseInt(shotsText.getText());
        System.out.printf(
                "+---------- CycleVary running on %d hole(s): %d shots, and %d subdivisions----------+%n",
                (int) Math.ceil((double) (endIdx - startIdx + 1) / stepIdx),
                shots,
                subdivisions
        );
        if(overrideSS) {
            System.out.printf("Override Side Sum maximums: CS - %d, OSO - %d, OSNO - %d%n", maxList[3], maxList[4], maxList[5]);
        }
        final ProgressMultiTask progress = new ProgressMultiTask("Line: %d, Stopping at: %d", true, startIdx+1, endIdx+1);
        progress.show();
        final ExecutorService storageExecutor = new PriorityExecutor(Utils.numThreads);
        final ExecutorService shotExecutor = new PriorityExecutor(Utils.numThreads);

        if(autoCover) viewer.coverWindow.appendStablesInfo("// Start CycleVary");

        drawCycleVary(maxList, subdivisions, autoCover, overrideSS, startIdx, endIdx, stepIdx, area, progress, step, colorOpt, executor, storageExecutor, shotExecutor);
    }

    private void drawCycleVary(final int[] max, final int maxSubdivisions, final boolean autoCover, final boolean overrideSS,
                               final int currIdx, final int endIdx, final int stepIdx, final ConvexPolygon area, final ProgressMultiTask overallProgress,
                               final Optional<SimpleObjectProperty<Integer>> step, final Optional<Color> colorOpt,
                               final ExecutorService drawExecutor, final ExecutorService storageExecutor, final ExecutorService shotExecutor) {
        // Move the screen
        lineNumTextField.setText(Integer.toString(currIdx + 1));
        moveScreenToLine(currIdx);

        final double xMin = Math.max(area.projectX().min, viewer.map.getViewRectangle().intervalX.min);
        final double xMax = Math.min(area.projectX().max, viewer.map.getViewRectangle().intervalX.max);
        final double yMin = Math.max(area.projectY().min, viewer.map.getViewRectangle().intervalY.min);
        final double yMax = Math.min(area.projectY().max, viewer.map.getViewRectangle().intervalY.max);

        final MutableList<Double> points = new FastList<>();
        final MutableList<Double> pointsFiltered = new FastList<>();
        viewer.autoRecurse(xMin, xMax, yMin, yMax, 0, maxSubdivisions, area, points);
        final Image image = viewer.regionsImageView.getImage();
        final PixelReader reader = image.getPixelReader();

        // Filter out filled pixels
        for(int i = 0; i < points.size(); i += 2) {
            final int midX = (int) viewer.map.pixelX(points.get(i));
            final int midY = (int) viewer.map.pixelY(points.get(i+1));
            int color = reader.getArgb(midX, midY);
            if(color == 0) {
                pointsFiltered.add(points.get(i));
                pointsFiltered.add(points.get(i+1));
            }
        }

        // We want to filter the codes to avoid recalculating any codes that are already drawn on the screen
        final MutableSortedSet<ClassifiedCodeSequence> onScreenCodes = new TreeSortedSet<>();
        viewer.onScreenSequences.keySet().forEach(storage -> {onScreenCodes.add(storage.classCodeSeq);});

        int mode = getMode();
        Integer numGroupToPrint = getNumGroupToPrint();

        if (numGroupToPrint == null) return;

        // Create the task
        final CycleVaryTask task = new CycleVaryTask(pointsFiltered, onScreenCodes, Array.ofAll(max), viewer.pool, storageExecutor, extractNumberFromTextField(shotsText), shotExecutor, viewer.regionsImageView, viewer.map, mode, numGroupToPrint, CSCb.isSelected(), OSOCb.isSelected(), OSNOCb.isSelected());
        final ObservableList<Storage> partials = task.getPartials();
        //final ProgressWithStatus progress = new ProgressWithStatus(task, "%d / %d", 0);
        overallProgress.changeTask(task);

        // Zhao Yu Li, Jun 25, 2025.viewer.
        // Determines whether to add vary results to the IterateToLimitWindow Cover
        final boolean addToAllPositive = allPositiveIsSelected();  // Add code with all-positive iteration patterns
        final boolean addToPlusMinus = plusMinusIsSelected();  // Add code with plus/minus patterns

        if ((addToAllPositive || addToPlusMinus) && viewer.iterateToLimitWindow == null) viewer.iterateToLimitWindow = new IterateToLimitWindow(viewer.pool);

        // Update screen when change detected
        partials.addListener((ListChangeListener.Change<? extends Storage> c) -> {
            if(overallProgress.isCancelled()) return; // Don't update after cancel received. This prevents codes being printed after the ending line
            while (c.next()) {
                if(!c.wasAdded()) continue;
                // Draw all new additions
                c.getAddedSubList().forEach(storage -> {
                    if(!viewer.onScreenSequences.containsKey(storage)) {
                        final Color color;
                        if(colorOpt.isPresent()) {
                            color = colorOpt.get();
                        } else {
                            final int index = viewer.cycle.get();
                            color = viewer.comboBoxColors.get(index);
                        }
                        viewer.addToOnScreenSequences(storage, color);
                        viewer.renderRegion(storage, (WritableImage) viewer.regionsImageView.getImage(), color);

                        if (mode == 0 || autoCover) {
                            // print the code
                            final String msg;
                            final CodeType type = storage.codeType();

                            String codeStr = "" + type;
                            // String codeStr = "xxx " + type; //george july 26 2017 -
                            // type whatever you want between the quotes in the line above
                            // make sure to add a space after the xxx
                            if (type.equals(CodeType.CS)) {
                                codeStr += "  ";
                            } else if (!type.equals(OSNO)) {
                                codeStr += " ";
                            }
                            msg = codeStr + " (" + storage.codeLength() + ", " + storage.codeSum() + ") " + storage.toString();

                            if (mode == 0) System.out.println(msg);
                            if(autoCover) viewer.coverWindow.appendStablesInfo(msg);
                        }

                        // Zhao Yu Li, Jun 25, 2025.
                        // Add the code sequence - iteration pattern pair to the IterateToLimitWindow Cover
                        Viewer.addToIterToLimitCover(storage.toString(), addToAllPositive, addToPlusMinus, viewer.iterateToLimitWindow);
                    }
                });
            }
        });

        task.setOnSucceeded(e -> {

            final ObservableList<Storage> storages;
            try {
                storages = task.get();
            } catch (InterruptedException | ExecutionException exception) {
                throw new RuntimeException(exception);
            }

            // This takes care of the very last codes completed, in case the listChangeListener doesn't catch them in time
            storages.forEach(storage -> {
                if(!viewer.onScreenSequences.containsKey(storage)) {
                    final Color color;
                    final int index = viewer.cycle.get();
                    color = viewer.comboBoxColors.get(index);
                    viewer.addToOnScreenSequences(storage, color);

                    if (mode == 0 || autoCover) {
                        // print the code
                        final String msg;
                        final CodeType type = storage.codeType();

                        String codeStr = "" + type;
                        // String codeStr = "xxx " + type; //george july 26 2017 -
                        // type whatever you want between the quotes in the line above
                        // make sure to add a space after the xxx
                        if (type.equals(CodeType.CS)) {
                            codeStr += "  ";
                        } else if (!type.equals(OSNO)) {
                            codeStr += " ";
                        }
                        msg = codeStr + " (" + storage.codeLength() + ", " + storage.codeSum() + ") " + storage.toString();

                        if (mode == 0) System.out.println(msg);
                        if(autoCover) viewer.coverWindow.appendStablesInfo(msg);
                    }

                    // Zhao Yu Li, Jun 25, 2025.
                    // Add the code sequence - iteration pattern pair to the IterateToLimitWindow Cover
                    Viewer.addToIterToLimitCover(storage.toString(), addToAllPositive, addToPlusMinus, viewer.iterateToLimitWindow);
                }
            });
            overallProgress.increment(Math.abs(stepIdx));
            // Run at the next hole
            if(overallProgress.isCancelled()) { // It is possible for cancel to occur before the task is created
                viewer.callRenderRegions();
                Utils.safeShutdownExecutor(storageExecutor);
                Utils.safeShutdownExecutor(shotExecutor);
                if(overrideSS) {
                    System.out.printf("Override Side Sum maximums: CS - %d, OSO - %d, OSNO - %d%n", max[3], max[4], max[5]);
                }
                System.out.println("+------------------------------ CycleVary Cancelled ------------------------------+");
                overallProgress.close();
                if(autoCover) viewer.coverWindow.show();
                // Propagate cancellation for Super
                step.ifPresent(integerSimpleObjectProperty -> integerSimpleObjectProperty.setValue(-1));
            } else if((currIdx + stepIdx <= endIdx && !AutoPolyVaryLoad.Reverse) || (currIdx + stepIdx >= endIdx && AutoPolyVaryLoad.Reverse)) {
                drawCycleVary(max, maxSubdivisions, autoCover, overrideSS, currIdx + stepIdx, endIdx, stepIdx, area, overallProgress, step, colorOpt, drawExecutor, storageExecutor, shotExecutor);
            } else {
                viewer.callRenderRegions();
                Utils.safeShutdownExecutor(storageExecutor);
                Utils.safeShutdownExecutor(shotExecutor);

                if(overrideSS) {
                    System.out.printf("Override Side Sum maximums: CS - %d, OSO - %d, OSNO - %d%n", max[3], max[4], max[5]);
                }

                if(autoCover) {
                    viewer.coverWindow.show();
                    System.out.println("+-------------- CycleVary finished successfully, CODES ARE IN COVER --------------+");
                } else {
                    System.out.println("+------------------------ CycleVary finished successfully ------------------------+");
                }

                overallProgress.close();

                // Zhao Yu Li, Jul 08, 2025.
                // Scale only if the user selected to scale
                boolean magnificationIsSelected = getMagnificationIsSelected();

                if (magnificationIsSelected) {
                    // Zhao Yu Li, Jul 08, 2025.
                    // Try to get the user enter scale factor
                    double scaleFactor = getMagnification();

                    // Zhao Yu Li, Jul 7, 2025.
                    // Scale after every rep
                    Interval oldXInterval = viewer.map.getViewRectangle().intervalX;
                    Interval oldYInterval = viewer.map.getViewRectangle().intervalY;
                    double oldCenterX = (oldXInterval.min + oldXInterval.max) / 2;
                    double oldCenterY = (oldYInterval.min + oldYInterval.max) / 2;

                    viewer.map.scaleBy(scaleFactor);

                    Interval newXInterval = viewer.map.getViewRectangle().intervalX;
                    Interval newYInterval = viewer.map.getViewRectangle().intervalY;
                    double newCenterX = (newXInterval.min + newXInterval.max) / 2;
                    double newCenterY = (newYInterval.min + newYInterval.max) / 2;

                    viewer.map.translateXBy(oldCenterX - newCenterX);
                    viewer.map.translateYBy(oldCenterY - newCenterY);

                    viewer.viewRectangleBF.add(viewer.map.getViewRectangle());
                }

                // Increment for superPoly
                step.ifPresent(integerSimpleObjectProperty -> integerSimpleObjectProperty.setValue(integerSimpleObjectProperty.getValue() + 1));
            }
        });

        task.setOnCancelled(e -> {
            partials.forEach(storage -> {
                if(!viewer.onScreenSequences.containsKey(storage)) {
                    final Color color;
                    final int index = viewer.cycle.get();
                    color = viewer.comboBoxColors.get(index);
                    viewer.addToOnScreenSequences(storage, color);

                    // print the code
                    final String msg;
                    final CodeType type = storage.codeType();

                    String codeStr = "" + type;
                    // String codeStr = "xxx " + type; //george july 26 2017 -
                    // type whatever you want between the quotes in the line above
                    // make sure to add a space after the xxx
                    if (type.equals(CodeType.CS)) {
                        codeStr += "  ";
                    } else if (!type.equals(OSNO)) {
                        codeStr += " ";
                    }
                    msg = codeStr + " (" + storage.codeLength() + ", " + storage.codeSum() + ") " + storage.toString();
                    System.out.println(msg);
                    if(autoCover) viewer.coverWindow.appendStablesInfo(msg);
                }
            });
            viewer.callRenderRegions();
            Utils.safeShutdownExecutor(storageExecutor);
            Utils.safeShutdownExecutor(shotExecutor);
            if(overrideSS) {
                System.out.printf("Override Side Sum maximums: CS - %d, OSO - %d, OSNO - %d%n", max[3], max[4], max[5]);
            }
            System.out.println("+------------------------------ CycleVary Cancelled ------------------------------+");
            overallProgress.close();
            if(autoCover) viewer.coverWindow.show();
            // Propagate cancellation for Super
            step.ifPresent(integerSimpleObjectProperty -> integerSimpleObjectProperty.setValue(-1));
        });

        task.setOnFailed(e -> {
            //progress.close();
            Utils.safeShutdownExecutor(storageExecutor);
            Utils.safeShutdownExecutor(shotExecutor);
            overallProgress.close();
            // Propagate cancellation for Super
            step.ifPresent(integerSimpleObjectProperty -> integerSimpleObjectProperty.setValue(-1));
            throw new RuntimeException(task.getException());
        });
        drawExecutor.execute(task);
    }
}
