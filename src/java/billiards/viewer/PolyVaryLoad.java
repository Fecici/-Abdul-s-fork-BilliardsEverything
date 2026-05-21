package billiards.viewer;

import billiards.geometry.ConvexPolygon;
import billiards.geometry.Rectangle;
import billiards.geometry.Vector2;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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

public final class PolyVaryLoad {
    // WARNING: Global mutable state
    // ------------------------------------------------------------
    private static String polygonString = "";
    public static Integer BoundCSMax = 300;
    public static Integer BoundOSOMax = 50;
    public static Integer BoundOSNOMax = 36;
    public static Integer BoundCSMaxSS = 800;
    public static Integer BoundOSOMaxSS = 300;
    public static Integer BoundOSNOMaxSS = 150;
    public static Boolean Override = false;
    public static Boolean AutoCover = true;

    // ------------------------------------------------------------

    private final String polygonFileName;
    private final String boundsFileName;
    private final TextArea text = new TextArea();
    private final Button loadButton = new Button();
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
    private final CheckBox autoSmallCoverBox = new CheckBox();
    private final VBox root = new VBox();
    private final VBox maxVBox = new VBox(10);
    private final VBox controlVBox = new VBox(20);
    private final HBox instructHBox = new HBox();
    private final HBox bottomHBox = new HBox();
    private final HBox maxHBox = new HBox(10);
    private final HBox maxOptHBox = new HBox(10);
    public final Stage stage = new Stage();
    private final Scene scene = new Scene(root);
    private final Label instruct = new Label();

    private Optional<Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer>> result;

    public PolyVaryLoad(final String windowTitle, final String buttonText, final String polyFileName,
                    final String boundsFileName, final Rectangle fullScreen) {
        this.polygonFileName = polyFileName;
        this.boundsFileName = boundsFileName;

        this.updateBounds();

        stage.setScene(scene);

        stage.setTitle(windowTitle);
        stage.setOnCloseRequest(event -> {
            this.result = Optional.empty();
            stage.close();
        });

        text.setPrefColumnCount(40);
        text.setPrefRowCount(6);
        text.setWrapText(true);
        text.setEditable(false);
        text.setFont(Font.font("Monaco", 16));
        text.setText(polygonString);

        // Sync the polygon with the cover polygon
        CoverWindow.polyStringProperty.addListener((o, oldValue, newValue) -> {
            polygonString = newValue;
            text.setText(polygonString);
        });

        instruct.setText("The following polygon is synced with the current cover");
        instruct.setPadding(new Insets(5, 5, 5, 10));

        // We want the text to expand as we make the window bigger
        VBox.setVgrow(text, Priority.ALWAYS);

        // Get seperate bounds on each code
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
        autoSmallCoverBox.setSelected(false);
        autoSmallCoverBox.setText("Add codes to small cover");

        instructHBox.getChildren().add(instruct);

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
        maxVBox.setAlignment(Pos.TOP_RIGHT);
        controlVBox.getChildren().addAll(loadButton, overrideBox, autoCoverBox, autoSmallCoverBox);
        controlVBox.setPadding(new Insets(0, 10, 10, 0));
        controlVBox.setAlignment(Pos.CENTER_LEFT);

        bottomHBox.getChildren().addAll(maxVBox, controlVBox);

        root.getChildren().addAll(instructHBox, text, bottomHBox);
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        loadButton.setText(buttonText);
        Utils.colorButton(loadButton, Color.SKYBLUE, Color.GOLD);
        loadButton.setOnAction(event -> {
            Override = overrideBox.isSelected();
            AutoCover = autoCoverBox.isSelected();
    		try {
    			BoundCSMax = Integer.parseInt(CSbox.getText().trim());
            	BoundOSOMax = Integer.parseInt(OSObox.getText().trim());
            	BoundOSNOMax = Integer.parseInt(OSNObox.getText().trim());
    			BoundCSMaxSS = Integer.parseInt(CSsbox.getText().trim());
            	BoundOSOMaxSS = Integer.parseInt(OSOsbox.getText().trim());
            	BoundOSNOMaxSS = Integer.parseInt(OSNOsbox.getText().trim());
    		} catch (NumberFormatException e) {
    			final Alert alert = new Alert(AlertType.ERROR);
        		alert.setTitle("PolyVary Error");
        		alert.setHeaderText("Non-integer value in input box");
        		alert.setContentText("Please enter a single integer into each of the '[SequenceType] Max' boxes.");
                alert.showAndWait();
                return;
            }
            polygonString = text.getText();
            if (polygonString.isEmpty()) {
                this.result = Optional.of(Tuple.of(fullScreen.toConvexPolygon(), BoundCSMax, BoundOSOMax, BoundOSNOMax, BoundCSMaxSS, BoundOSOMaxSS, BoundOSNOMaxSS));

            } else {
                final String lines = cleanPolygon(polygonString);
                final ConvexPolygon poly = createConvexPolygon(lines);
                this.result = Optional.of(Tuple.of(poly, BoundCSMax, BoundOSOMax, BoundOSNOMax, BoundCSMaxSS, BoundOSOMaxSS, BoundOSNOMaxSS));
            }
            //Utils.writeToFile(polyFileName, polygonString);
        	Utils.writeToFile(boundsFileName, String.format("%d %d %d %d %d %d", BoundCSMax, BoundOSOMax, BoundOSNOMax, BoundCSMaxSS, BoundOSOMaxSS, BoundOSNOMaxSS));
            stage.close();
        });
    }

    public void updateBounds() {
        polygonString = Utils.readFromFile(polygonFileName);
    	String[] boundTokens = Utils.readFromFile(boundsFileName).trim().split(" ");
    	if (boundTokens.length >= 6) {
    		try {
    			BoundCSMax = Integer.parseInt(boundTokens[0]);
    			BoundOSOMax = Integer.parseInt(boundTokens[1]);
    			BoundOSNOMax = Integer.parseInt(boundTokens[2]);
    			BoundCSMaxSS = Integer.parseInt(boundTokens[3]);
    			BoundOSOMaxSS = Integer.parseInt(boundTokens[4]);
    			BoundOSNOMaxSS = Integer.parseInt(boundTokens[5]);
    		} catch (NumberFormatException e) {
    		}
    	}

    }

    public Optional<Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer>> getPolyVaryLoad() {
        stage.showAndWait();
        return this.result;
    }

    public boolean getAutoSmallCover() {
        return autoSmallCoverBox.isSelected();
    }
}
