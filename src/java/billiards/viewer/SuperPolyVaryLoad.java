package billiards.viewer;

import billiards.geometry.Vector2;

import javafx.scene.text.Text;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.Optional;

import billiards.geometry.ConvexPolygon;
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
import static billiards.utils.Polygon.createConvexPolygon;

public class SuperPolyVaryLoad {
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
    public static Integer Reps = 0;
    public static Boolean ColorCycle = true;
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
    private final Label stepl = new Label();
    private final Label CSstepl = new Label();
    private final Label OSOstepl = new Label();
    private final Label OSNOstepl = new Label();
    private final TextField CSstepbox = new TextField();
    private final TextField OSOstepbox = new TextField();
    private final TextField OSNOstepbox = new TextField();
    private final CheckBox colorCycleBox = new CheckBox();
    private final CheckBox autoCoverBox = new CheckBox();
	private final CheckBox autoSmallCoverBox = new CheckBox("Add codes to small cover");
    private final Label repl = new Label();
    private final TextField repBox = new TextField();
    private final Button loadButton = new Button();
    private final VBox root = new VBox();
    private final VBox maxVBox = new VBox(10);
    private final VBox controlVBox = new VBox(20);
    private final VBox repVBox = new VBox(10);
    private final HBox instructHBox = new HBox();
    private final HBox bottomHBox = new HBox();
    private final HBox maxHBox = new HBox(10);
    private final HBox maxOptHBox = new HBox(10);
    private final HBox maxStepHBox = new HBox(10);
    private final HBox loadHBox = new HBox(10);
    public final Stage stage = new Stage();
    private final Scene scene = new Scene(root);
    private final Label instruct = new Label();
	private final CheckBox magnifyCheckBox = new CheckBox();
	private final TextField magnifyTextField = new TextField();
    
    private Optional<Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer>> result;
    
    public SuperPolyVaryLoad(final String windowTitle, final String buttonText, final String fileName, final String boundsFileName, final String stepFileName) {
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

    	stage.setScene(scene);
    	stage.setTitle(windowTitle);
    	stage.setOnCloseRequest(e -> {
    		this.result = Optional.empty();
    		stage.close();
    	});
    	
    	text.setPrefColumnCount(40);
    	text.setPrefRowCount(10);
    	text.setWrapText(true);
    	text.setEditable(false);
    	text.setFont(Font.font("Monaco", 16));
    	text.setText(polygonString);
    	VBox.setVgrow(text, Priority.ALWAYS);
    	
        // Sync the polygon with the cover polygon
        CoverWindow.polyStringProperty.addListener((o, oldValue, newValue) -> {
            polygonString = newValue;
            text.setText(polygonString);
        });

    	instruct.setText("The following polygon is synced with the current cover");
    	instruct.setPadding(new Insets(5, 5, 5, 10));
    	
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

        stepl.setText("SS step:");
        CSstepbox.setPrefWidth(150);
    	CSstepbox.setText(BoundCSstep.toString());
    	CSstepl.setText("CS step:");
    	OSOstepbox.setPrefWidth(150);
    	OSOstepbox.setText(BoundOSOstep.toString());
    	OSOstepl.setText("OSO step:");
    	OSNOstepbox.setPrefWidth(150);
    	OSNOstepbox.setText(BoundOSNOstep.toString());
    	OSNOstepl.setText("OSNO step:");

        repl.setText("Reps");
        repBox.setPrefWidth(50);
        repBox.setText(Reps.toString());


        colorCycleBox.setIndeterminate(false);
        colorCycleBox.setAllowIndeterminate(false);
        colorCycleBox.setSelected(ColorCycle);
        colorCycleBox.setText("Cycle colors");

        autoCoverBox.setIndeterminate(false);
        autoCoverBox.setAllowIndeterminate(false);
        autoCoverBox.setSelected(AutoCover);
        autoCoverBox.setText("Add codes to cover");

		autoSmallCoverBox.setIndeterminate(false);
		autoSmallCoverBox.setAllowIndeterminate(false);
		autoSmallCoverBox.setSelected(false);

    	instructHBox.getChildren().add(instruct);

    	maxHBox.getChildren().addAll(codel, CSl, CSbox, OSOl, OSObox, OSNOl, OSNObox);
    	maxHBox.setPadding(new Insets(0, 10, 10, 0));
    	maxHBox.setAlignment(Pos.CENTER);

        maxOptHBox.getChildren().addAll(ssuml, CSsl, CSsbox, OSOsl, OSOsbox, OSNOsl, OSNOsbox);
        maxOptHBox.setPadding(new Insets(0, 10, 10, 0));
        maxOptHBox.setAlignment(Pos.CENTER);

        maxStepHBox.getChildren().addAll(stepl, CSstepl, CSstepbox, OSOstepl, OSOstepbox, OSNOstepl, OSNOstepbox);
        maxStepHBox.setPadding(new Insets(0, 10, 10, 0));
        maxStepHBox.setAlignment(Pos.CENTER);

        maxVBox.getChildren().addAll(maxHBox, maxOptHBox, maxStepHBox);
        repVBox.getChildren().addAll(repl, repBox);
        loadHBox.getChildren().addAll(loadButton, repVBox);
        loadHBox.setAlignment(Pos.CENTER);

		// Zhao Yu Li, Jul 8, 2025.
		// Optional magnification after every rep, and arbitrary magnification
		magnifyTextField.setPrefColumnCount(3);
		magnifyTextField.setText("2");

		magnifyCheckBox.setText("Magnification:");

		HBox magnifyHBox = new HBox(10, magnifyCheckBox, magnifyTextField);
		magnifyHBox.setAlignment(Pos.CENTER_LEFT);

        //controlVBox.getChildren().addAll(loadHBox, overrideBox, autoCoverBox);
        controlVBox.getChildren().addAll(loadHBox, autoCoverBox, autoSmallCoverBox, colorCycleBox);
        controlVBox.setPadding(new Insets(0, 10, 10, 0));
        controlVBox.setAlignment(Pos.CENTER_LEFT);

        bottomHBox.getChildren().addAll(maxVBox, controlVBox);

    	root.getChildren().addAll(instructHBox, text, bottomHBox, magnifyHBox);
    	root.setSpacing(10);
    	root.setPadding(new Insets(10));
    
    	loadButton.setText(buttonText);
    	Utils.colorButton(loadButton, Color.SKYBLUE, Color.GOLD);
    	loadButton.setOnAction(event -> {
            ColorCycle = colorCycleBox.isSelected();
            AutoCover = autoCoverBox.isSelected();
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
    		polygonString = text.getText();
    		final String lines = cleanPolygon(polygonString);
    		final ConvexPolygon poly = createConvexPolygon(lines);
        	this.result = Optional.of(Tuple.of(poly, BoundCSMax, BoundOSOMax, BoundOSNOMax, BoundCSMaxSS, BoundOSOMaxSS, BoundOSNOMaxSS));
        	//Utils.writeToFile(fileName, polygonString);
        	Utils.writeToFile(boundsFileName, String.format("%d %d %d %d %d %d %d %d %d", BoundCSMax, BoundOSOMax, BoundOSNOMax, BoundCSMaxSS, BoundOSOMaxSS, BoundOSNOMaxSS, BoundCSstep, BoundOSOstep, BoundOSNOstep));
        	stage.close();
    	});
    }
    
    public Optional<Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer>> getLoad() {
    	stage.showAndWait();
    	return this.result;
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

	public boolean getAutoSmallCover() {
		return autoSmallCoverBox.isSelected();
	}
}
