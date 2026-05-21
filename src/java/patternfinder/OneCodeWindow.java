package patternfinder;

import java.util.Optional;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;

import billiards.viewer.Utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class OneCodeWindow {

	private final Button searchBtn = new Button();
    private final TextArea inputField = new TextArea();
    private final TextArea resultField = new TextArea();

    private final TextField[][] fields = {{new TextField(), new TextField(), new TextField()},
                                          {new TextField(), new TextField(), new TextField()},
                                          {new TextField(), new TextField(), new TextField()},
                                          {new TextField(), new TextField(), new TextField()},
                                          {new TextField(), new TextField(), new TextField()}};

    private final GridPane grid = new GridPane();

    private final TextField[] coefFields = {new TextField(), new TextField(), new TextField()};

    private final VBox root = new VBox();
    private final Stage stage = new Stage();
    private final Scene scene = new Scene(root);

    public OneCodeWindow(final String windowTitle) {

        stage.setScene(scene);
        stage.setTitle(windowTitle);
        stage.setOnCloseRequest(event -> stage.close());

        for (int i = 0; i < 3; i++) {
            coefFields[i].setText("1");
            coefFields[i].setPromptText("Coef " + i);
            coefFields[i].setPrefWidth(60);
        }

        inputField.setPrefWidth(700);
        inputField.setPromptText("Enter a code sequence here");
        resultField.setPrefWidth(700);
        resultField.setEditable(false);
        resultField.setPromptText("The result will appear here");

        for (int i = 0; i < fields.length; i++) {
            for (int j = 0; j < 3; j++) {
                grid.add(fields[i][j], j, i);
                GridPane.setHgrow(fields[i][j], Priority.ALWAYS);
            }
            fields[i][0].setPromptText("Min interval " + (i + 1));
            fields[i][0].setText("" + (1 + (i+1) * 100));
            fields[i][1].setPromptText("Max interval " + (i + 1));
            fields[i][1].setText("" + (100 + (i+1) * 100));
            fields[i][2].setPromptText("Coefficient " + (i + 1));
            fields[i][2].setText("" + (int) (2 + 2 * Math.floor(i/2)));

        }
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);

    	searchBtn.setText("Calculate");
    	Utils.colorButton(searchBtn, Color.LIGHTBLUE, Color.GOLD);
    	searchBtn.setOnAction(event -> {

    	    final String[] lines = inputField.getText().split("\\r?\\n");
            final StringBuilder fullResult = new StringBuilder();

    	    for (String dirtyLine : lines) {

                final String line = Utils.tripleTrimmer(dirtyLine.split("//")[0]);

                final String[] codeStrs = line.split(",");

    	        final StringBuilder codeBuilder = new StringBuilder();

                for (int i = 0; i < codeStrs.length; i++) {
                    final String codeStr = codeStrs[i];

                    final Optional<ImmutableIntList> codeOpt = PatUtils.splitString(codeStr);
                    if (codeOpt.isPresent()) {
                        ImmutableIntList code = codeOpt.get();
                        final StringBuilder builder = new StringBuilder();
                        for (int j = 0; j < code.size(); j++) {
                            for (TextField[] rule : fields) {
                                if (!rule[0].getText().trim().isEmpty()
                                 && !rule[1].getText().trim().isEmpty()
                                 && !rule[2].getText().trim().isEmpty()) {

                                    final int min = Integer.parseInt(rule[0].getText().trim());
                                    final int max = Integer.parseInt(rule[1].getText().trim());
                                    int coef = Integer.parseInt(rule[2].getText().trim());

                                    if (min <= code.get(j) && code.get(j) <= max) {
                                        coef = coef / 2;

                                        while(coef > 0) {
                                            if (i > 2) {
                                                builder.append((j + 1) + " ");
                                            } else {
                                                for (int k = 0; k < Integer.parseInt(coefFields[i]
                                                     .getText()); k++) builder.append((j + 1) + " ");
                                            }
                                            coef -= 1;
                                        }
                                    }

                                }
                            }
                        }
                        codeBuilder.append(builder.toString().trim());
                        if (i < codeStrs.length - 1) {
                            codeBuilder.append(", ");
                        }
                    }
                }
                fullResult.append("+" + line.trim() + " # " + codeBuilder.toString() + "\n");
    	    }


            resultField.setText(fullResult.toString());
    	});

    	final HBox controlHBox = new HBox(10);
    	controlHBox.setPadding(new Insets(10));
    	controlHBox.setAlignment(Pos.CENTER);
    	controlHBox.getChildren().addAll(searchBtn, coefFields[0], coefFields[1], coefFields[2]);

        root.getChildren().addAll(controlHBox, grid, inputField, resultField);
        root.setSpacing(10);
        root.setPadding(new Insets(10));
        VBox.setVgrow(resultField, Priority.SOMETIMES);
        VBox.setVgrow(inputField, Priority.SOMETIMES);
    }


    public void close() {
        stage.close();
    }

    public void show() {
        stage.show();
    }

}
