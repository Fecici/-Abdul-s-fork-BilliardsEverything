package billiards.viewer;

import billiards.geometry.Vector2;

import javafx.scene.text.Text;
import javaslang.Tuple3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javaslang.Tuple;
import javaslang.Tuple7;

import static billiards.utils.Polygon.cleanPolygon;

public final class VaryWindowL {
    // WARNING: Global mutable state
    // ------------------------------------------------------------
    public static String fullContent = "";
    public static Integer BoundCSMax = 300;
    public static Integer BoundOSOMax = 100;
    public static Integer BoundOSNOMax = 36;
    public static Integer BoundCSMaxSS = 800;
    public static Integer BoundOSOMaxSS = 300;
    public static Integer BoundOSNOMaxSS = 150;
    public static Boolean Draw = true;
    public static Boolean Override = false;
    public static Boolean AutoCover = true;
    // ------------------------------------------------------------

    private final TextArea text = new TextArea();
    private final Label codel = new Label();
    private final Label CSl = new Label();
    private final Label OSOl = new Label();
    private final Label OSNOl = new Label();
    private final TextField CSbox = new TextField();
    private final TextField OSObox = new TextField();
    private final TextField OSNObox = new TextField();
    private final Label ssuml = new Label();
    private final Label CSsl = new Label();
    private final Label OSOsl = new Label();
    private final Label OSNOsl = new Label();
    private final TextField CSsbox = new TextField();
    private final TextField OSOsbox = new TextField();
    private final TextField OSNOsbox = new TextField();
    private final CheckBox overrideBox = new CheckBox();
    private final CheckBox autoCoverBox = new CheckBox();
    private final CheckBox firstLastBox = new CheckBox();
    private final CheckBox drawCB = new CheckBox();
    private final Button loadButton = new Button();
    private final VBox root = new VBox();
    private final VBox typeVBox = new VBox(30);
    private final VBox maxVBox = new VBox(10);
    private final VBox controlVBox = new VBox(20);
    private final HBox instructHBox = new HBox();
    private final HBox bottomHBox = new HBox();
    private final HBox maxHBox = new HBox(10);
    private final HBox maxOptHBox = new HBox(10);
    private final HBox overrideHBox = new HBox(10);
    private final HBox loadHBox = new HBox(10);
    public final Stage stage = new Stage();
    private final Scene scene = new Scene(root);
    private final Label instruct = new Label();
    private final CheckBox addToAllPositiveCB = new CheckBox();
    private final CheckBox addToPlusMinusCB = new CheckBox();
    final TextField lineNumTextField = new TextField();
    private final HBox controlHBox;
    private final Viewer viewer;
    private final String windowTitle;
    private Integer lineNumber = null;

    private final CheckBox autoSmallCoverBox = new CheckBox();

    private Optional<Tuple7<MutableList<Vector2>, Integer, Integer, Integer, Integer, Integer, Integer>> result;

    public VaryWindowL(final String windowTitle, final String buttonText, final String fileName,
                       final String varyBoundFileName, final boolean middle, final Viewer viewer) {
        this.viewer = viewer;
        this.windowTitle = windowTitle;

        fullContent = Utils.readFromFile(fileName);
    	String[] boundTokens = Utils.readFromFile(varyBoundFileName).trim().split(" ");
    	if (boundTokens.length >= 6) {
    		try {
    			BoundCSMax = Integer.parseInt(boundTokens[0]);
    			BoundOSOMax = Integer.parseInt(boundTokens[1]);
    			BoundOSNOMax = Integer.parseInt(boundTokens[2]);
    			BoundCSMaxSS = Integer.parseInt(boundTokens[3]);
    			BoundOSOMaxSS = Integer.parseInt(boundTokens[4]);
    			BoundOSNOMaxSS = Integer.parseInt(boundTokens[5]);
    		} catch (NumberFormatException e) {
    			BoundCSMax = 300;
    			BoundOSOMax = 100;
    			BoundOSNOMax = 36;
                BoundCSMaxSS = 800;
                BoundOSOMaxSS = 300;
                BoundOSNOMaxSS = 200;
    		}
    	}
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

        codel.setText("Code length:");
        CSbox.setPrefWidth(150);
        CSbox.setText(BoundCSMax.toString());
        CSl.setText("CS max:");
        OSObox.setPrefWidth(150);
        OSObox.setText(BoundOSOMax.toString());
        OSOl.setText("OSO max:");
        OSNObox.setPrefWidth(150);
        OSNObox.setText(BoundOSNOMax.toString());
        OSNOl.setText("OSNO max:");


        ssuml.setText("Side sum:");
        CSsbox.setPrefWidth(150);
    	CSsbox.setText(BoundCSMaxSS.toString());
    	CSsl.setText("CS max:");
    	OSOsbox.setPrefWidth(150);
    	OSOsbox.setText(BoundOSOMaxSS.toString());
    	OSOsl.setText("OSO max:");
    	OSNOsbox.setPrefWidth(150);
    	OSNOsbox.setText(BoundOSNOMaxSS.toString());
    	OSNOsl.setText("OSNO max:");

        overrideBox.setIndeterminate(false);
        overrideBox.setAllowIndeterminate(false);
        overrideBox.setSelected(Override);
        overrideBox.setText("Override side sum");

        autoCoverBox.setIndeterminate(false);
        autoCoverBox.setAllowIndeterminate(false);
        autoCoverBox.setSelected(AutoCover);
        autoCoverBox.setText("Add codes to cover");

        autoSmallCoverBox.setIndeterminate(false);
        autoSmallCoverBox.setAllowIndeterminate(false);
        autoSmallCoverBox.setSelected(AutoCover);
        autoSmallCoverBox.setText("Add codes to small cover");

        firstLastBox.setIndeterminate(false);
        firstLastBox.setAllowIndeterminate(false);
        firstLastBox.setSelected(false);
        firstLastBox.setText("Include first and last");

        drawCB.setSelected(Draw);//changed true to false george oct 5,2017
        drawCB.setText("Draw"); 

        instructHBox.getChildren().addAll(instruct);

        /*
        typeVBox.getChildren().addAll(codel, ssuml);
        typeVBox.setPadding(new Insets(0, 10, 10, 0));
        typeVBox.setAlignment(Pos.CENTER);
        */

        maxHBox.getChildren().addAll(codel, CSl, CSbox, OSOl, OSObox, OSNOl, OSNObox);
        maxHBox.setPadding(new Insets(0, 10, 10, 0));
        maxHBox.setAlignment(Pos.CENTER);

        
        maxOptHBox.getChildren().addAll(ssuml, CSsl, CSsbox, OSOsl, OSOsbox, OSNOsl, OSNOsbox);
        maxOptHBox.setPadding(new Insets(0, 10, 10, 0));
        maxOptHBox.setAlignment(Pos.CENTER);

        maxVBox.getChildren().addAll(maxHBox, maxOptHBox);
        loadHBox.getChildren().addAll(loadButton, drawCB);
        overrideHBox.getChildren().addAll(overrideBox);
        controlVBox.getChildren().addAll(loadHBox, overrideBox);

        addToAllPositiveCB.setIndeterminate(false);
        addToAllPositiveCB.setAllowIndeterminate(false);
        addToAllPositiveCB.setSelected(false);
        addToAllPositiveCB.setText("Add to all-positive");

        addToPlusMinusCB.setIndeterminate(false);
        addToPlusMinusCB.setAllowIndeterminate(false);
        addToPlusMinusCB.setSelected(false);
        addToPlusMinusCB.setText("Add to plus-minus");

        controlHBox = new HBox(10, autoCoverBox, autoSmallCoverBox, addToAllPositiveCB, addToPlusMinusCB);

        if (middle) controlHBox.getChildren().add(2, firstLastBox);

        controlVBox.setPadding(new Insets(0, 10, 10, 0));
        controlVBox.setAlignment(Pos.CENTER_LEFT);

        bottomHBox.getChildren().addAll(maxVBox, controlVBox);

        final VBox bottomVBox = getBottomVBox();

        root.getChildren().addAll(instructHBox, text, bottomVBox);
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        loadButton.setText(buttonText);
        Utils.colorButton(loadButton, Color.SKYBLUE, Color.GOLD);
        loadButton.setOnAction(event -> {
            Override = overrideBox.isSelected();
            Draw = drawCB.isSelected();
            AutoCover =autoCoverBox.isSelected();
        	try {
        		BoundCSMax = Integer.parseInt(CSbox.getText().trim());
        		BoundOSOMax = Integer.parseInt(OSObox.getText().trim());
        		BoundOSNOMax = Integer.parseInt(OSNObox.getText().trim());
    			BoundCSMaxSS = Integer.parseInt(CSsbox.getText().trim());
            	BoundOSOMaxSS = Integer.parseInt(OSOsbox.getText().trim());
            	BoundOSNOMaxSS = Integer.parseInt(OSNOsbox.getText().trim());
        	} catch (NumberFormatException e) {
        		final Alert alert = new Alert(AlertType.ERROR);
        		alert.setTitle("VaryL Error");
        		alert.setHeaderText("Non-integer value in input box");
        		alert.setContentText("Please enter a single integer into each of the '[SequenceType] Max' boxes.");
        		alert.showAndWait();
    			return;
        	}
            fullContent = text.getText();
            
            final String cleaned = cleanPolygon(fullContent);

            final String[] lines = cleaned.split("\n");
            final MutableList<Vector2> pointList = new FastList<>();

            for (final String line : lines) {
                final String[] coords = line.split(" ");
                final double x = Double.parseDouble(coords[0]);
                final double y = Double.parseDouble(coords[1]);
                pointList.add(Vector2.create(x, y));
            }
            this.result = Optional.of(Tuple.of(pointList, BoundCSMax, BoundOSOMax, BoundOSNOMax, BoundCSMaxSS, BoundOSOMaxSS, BoundOSNOMaxSS));
            Utils.writeToFile(fileName, fullContent);
            Utils.writeToFile(varyBoundFileName, String.format("%d %d %d %d %d %d", BoundCSMax, BoundOSOMax, BoundOSNOMax, BoundCSMaxSS, BoundOSOMaxSS, BoundOSNOMaxSS));
            stage.close();
        });
    }

    // Zhao Yu Li, Jun 29, 2025.
    // These buttons implement moving the screen from one coordinate to the next.
    private VBox getBottomVBox() {
        final Button backwardButton = new Button("Backward");
        final Button forwardButton = new Button("Forward");
        final Button goToLineButton = new Button("Go");

        lineNumTextField.setPromptText("Line");
        lineNumTextField.setPrefColumnCount(3);

        backwardButton.setOnAction(event -> {
            if (text.getText().trim().isEmpty()) {
                showMoveScreenAlert("Please enter at least one coordinate.");
                return;
            }

            if (lineNumber == null) lineNumber = 1;
            else {
                Tuple3<Integer, Integer, Integer> startStepEnd = viewer.getStartStepEnd(getCoordinatesListLength());
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
            if (text.getText().trim().isEmpty()) {
                showMoveScreenAlert("Please enter at least one coordinate.");
                return;
            }

            if (lineNumber == null) lineNumber = 1;
            else {
                Tuple3<Integer, Integer, Integer> startStepEnd = viewer.getStartStepEnd(getCoordinatesListLength());
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
            if (text.getText().trim().isEmpty()) {
                showMoveScreenAlert("Please enter at least one coordinate.");
                return;
            }

            int userLineNumber = getLineNumber();

            lineNumber = userLineNumber;

            lineNumTextField.setText(Integer.toString(userLineNumber));
            moveScreenToLine(userLineNumber - 1);
        });

        final HBox lineNavigateHBox = new HBox(10, backwardButton, lineNumTextField, goToLineButton, forwardButton);

        return new VBox(10, bottomHBox, controlHBox, lineNavigateHBox);
    }

    public Optional<Tuple7<MutableList<Vector2>, Integer, Integer, Integer, Integer, Integer, Integer>> 
    	   getPoints(final String x, final String y, final boolean onePoint) {
    	if (onePoint) {
    		fullContent = x + " " + y;
            text.setText(fullContent);
            loadButton.fire();
    	}
    	else {
    		stage.showAndWait();
    	}
        return this.result;
    }
    public boolean getOverride() {
        return Override;
    }

    public boolean getFirstLastSelected() {
        return this.firstLastBox.isSelected();
    }

    public boolean getAddToAllPositiveSelected() {
        return this.addToAllPositiveCB.isSelected();
    }

    public boolean getAddToPlusMinusSelected() {
        return this.addToPlusMinusCB.isSelected();
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
        alert.setTitle(windowTitle);
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

        String[] coordinateStrings =  text.getText().trim().split("\n");

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
        return text.getText().trim().split("\n").length;
    }

    public boolean getDraw() {
        return drawCB.isSelected();
    }

    public boolean getAddToSmallCover() {
        return autoSmallCoverBox.isSelected();
    }
}
