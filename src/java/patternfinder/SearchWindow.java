package patternfinder;

import java.util.Optional;

import billiards.codeseq.CodeType;
import billiards.viewer.Utils;
import billiards.wrapper.Wrapper;
import billiards.wrapper.ConnectionPool;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class SearchWindow {

	private final Button searchBtn = new Button();
	private final TextField lengthField = new TextField();
    private final TextField evenOddField = new TextField();
    private final TextArea resultText = new TextArea();

    private final RadioButton OSOrb = new RadioButton();
    private final RadioButton OSNOrb = new RadioButton();
    private final RadioButton ONSrb = new RadioButton();
    private final RadioButton CSrb = new RadioButton();
    private final RadioButton CNSrb = new RadioButton();
    private final ToggleGroup typeGroup = new ToggleGroup();

    private final VBox root = new VBox();
    private final Stage stage = new Stage();
    private final Scene scene = new Scene(root);

    public SearchWindow(final String windowTitle, final ConnectionPool pool) {

        stage.setScene(scene);
        stage.setTitle(windowTitle);
        stage.setOnCloseRequest(event -> stage.close());

        resultText.setPrefSize(600, 500);

        lengthField.setPrefWidth(100);
        lengthField.setPromptText("Length");
        lengthField.setTooltip(Utils.toolTip("The search will look for codes only of exactly this length"));

        evenOddField.setPrefWidth(500);
        evenOddField.setPromptText("Even Odd");
        evenOddField.setTooltip(Utils.toolTip("The search will look for codes of this even odd type"));

        CSrb.setText("CS");
        CSrb.setSelected(true);
        CSrb.setTooltip(Utils.toolTip("The search will look for codes only of chosen type"));
        CSrb.setToggleGroup(typeGroup);
    	OSOrb.setText("OSO");
        OSOrb.setTooltip(Utils.toolTip("The search will look for codes only of chosen type"));
        OSOrb.setToggleGroup(typeGroup);
        OSNOrb.setText("OSNO");
        OSNOrb.setTooltip(Utils.toolTip("The search will look for codes only of chosen type"));
        OSNOrb.setToggleGroup(typeGroup);
        CNSrb.setText("CNS");
        CNSrb.setTooltip(Utils.toolTip("The search will look for codes only of chosen type"));
        CNSrb.setToggleGroup(typeGroup);
        ONSrb.setText("ONS");
        ONSrb.setTooltip(Utils.toolTip("The search will look for codes only of chosen type"));
        ONSrb.setToggleGroup(typeGroup);

    	searchBtn.setText("Search");
    	Utils.colorButton(searchBtn, Color.LIGHTBLUE, Color.GOLD);
    	searchBtn.setOnAction(event -> {

            final CodeType type;
            if (CSrb.isSelected()) {
                type = CodeType.CS;
            } else if (OSOrb.isSelected()) {
                type = CodeType.OSO;
            } else if (OSNOrb.isSelected()) {
                type = CodeType.OSNO;
            } else if (CNSrb.isSelected()) {
                type = CodeType.CNS;
            } else if (ONSrb.isSelected()) {
                type = CodeType.ONS;
            } else {
                throw new RuntimeException("no type selected");
            }

            final String lengthText = lengthField.getText().trim();
            final String evenOdd = evenOddField.getText().trim();

            final String contents;
            if (evenOdd.isEmpty()) {
                // Search by length instead
                final int length = Integer.parseInt(lengthText);

                contents = Wrapper.search(type, length, pool);
            } else {
                // Search by even odd
            	final Optional<String> eoOpt = parseEvenOdds(evenOdd);
            	if (eoOpt.isPresent()) {
            		contents = Wrapper.search(type, eoOpt.get(), pool);
            	} else {
            		contents = "";
            		final Alert alert = new Alert(AlertType.INFORMATION);
		            alert.setTitle("Search");
		            alert.setHeaderText("Search Error");
		            alert.setContentText(String.format("Incorrect even-odd formatting"));
		            alert.showAndWait();
            	}
            }

            resultText.setText(contents);
    	});

    	final HBox controlHBox = new HBox(10);
    	controlHBox.setPadding(new Insets(10));
    	controlHBox.setAlignment(Pos.CENTER);
    	controlHBox.getChildren().addAll(CSrb, OSOrb, OSNOrb, CNSrb, ONSrb, lengthField, searchBtn);

        root.getChildren().addAll(controlHBox, evenOddField, resultText);
        root.setSpacing(10);
        root.setPadding(new Insets(10));
        VBox.setVgrow(resultText, Priority.ALWAYS);
    }
    
    private static Optional<String> parseEvenOdds(final String code) {
    	if (code.replace("O", "").replace("E", "").trim().isEmpty()) {
    		return Optional.of(code);
    	}
    	
    	final StringBuilder result = new StringBuilder();
    	final String[] numbers = code.trim().split(" ");
    	for (String number : numbers) {
    		final int integer;
    		try {
    			integer = Integer.parseInt(number);
    		} catch (NumberFormatException e) {
				return Optional.empty();
			}
    		if ((integer % 2) == 1) {
    			result.append("O");
    		} else {
    			result.append("E");
    		}
    	}
    	return Optional.of(result.toString());
    }

    public void close() {
        stage.close();
    }

    public void show() {
        stage.show();
    }

}
