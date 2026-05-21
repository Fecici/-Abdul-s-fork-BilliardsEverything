package billiards.viewer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * <b>Zhao Yu Li</b><br>
 * <b>Jul 07, 2025</b>
 * <p>
 *     Calculates the iteration pattern between two code sequences.
 * </p>
 */
public class PatternCalculator {
    public PatternCalculator() {
        final Stage stage = new Stage();
        stage.setTitle("Code Sequence Pattern Calculator");


        final VBox root = new VBox(10);
        final Scene scene = new Scene(root, 1000, 600);
        root.setAlignment(Pos.CENTER);

        stage.setScene(scene);

        // Create a multi-line text area with top-aligned cursor
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefWidth(980);
        textArea.setPrefHeight(580);
        textArea.setFont(new Font("monospace", 16));

        // Put the text area inside a scroll pane
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);

        // Create button
        Button button = new Button("Calculate Pattern");

        root.setPadding(new Insets(10));
        root.getChildren().addAll(scrollPane, button);

        // Action listener
        button.setOnAction(e -> calcCodePattern(textArea));

        stage.show();
    }

    private static String getSubstringAfterPattern(String string, String pattern) {
        int index = string.indexOf(") ");
        if (index != -1) {
            return string.substring(index + pattern.length());
        } else {
            return string;
        }
    }

    private static void calcCodePattern(TextArea textArea) {
        final String[] codes = textArea.getText().trim().split("\n");

        if (codes.length != 2) {
            showAlert("Please enter exactly two code sequences separated by a new line.");
            return;
        }

        final String pattern = ") ";
        final String string1 = getSubstringAfterPattern(codes[0], pattern);
        final String string2 = getSubstringAfterPattern(codes[1], pattern);

        if (string1.isEmpty()) {
            showAlert(
                    "The first code sequence does not contain the pattern '" + pattern +
                            "'\nPlease enter a valid code sequence, e.g. starting with '1 - CS(x, y)'.");
            return;
        }

        if (string2.isEmpty()) {
            showAlert(
                    "The second code sequence does not contain the pattern '" + pattern +
                            "'\nPlease enter a valid code sequence, e.g. starting with '1 - CS(x, y)'.");
            return;
        }

        StringBuilder codePattern = new StringBuilder();

        final String[] subSequence1 = string1.split(",");
        final String[] subSequence2 = string2.split(",");

        if (subSequence1.length != subSequence2.length) {
            showAlert("The number of subsequences do not match.");
            return;
        }

        if (subSequence1.length != 1 && subSequence1.length != 3) {
            System.out.println("Unorthodox number of subsequences. i.e. The code sequences are neither a single nor a triple.");
        }

        for (int i = 0; i < subSequence1.length; i++) {
            final String subPattern = calcSequencePattern(subSequence1[i].trim(), subSequence2[i].trim());
            codePattern.append(subPattern).append(", ");
        }

        System.out.println(codePattern.toString().trim().substring(0, codePattern.length() - 2));
    }

    private static String calcSequencePattern(String string1, String string2) {
        final String[] code1 = string1.split(" ");
        final String[] code2 = string2.split(" ");

        if (code1.length != code2.length) showAlert("The length of the code sequences do not match.");

        StringBuilder codePattern = new StringBuilder();

        for (int i = 0; i < code1.length; i++) {
            int code1Val;
            int code2Val;
            try {
                code1Val = Integer.parseInt(code1[i]);
                code2Val = Integer.parseInt(code2[i]);
            } catch (Exception e) {
                showAlert("An exception occurred while converting code sequence at index " + (i + 1) + " into integers: " + e);
                return "";
            }

            final int difference = code2Val - code1Val;

            if (difference % 2 != 0) {
                showAlert("Difference between the code value at index " + (i + 1) + " is not a multiple of 2.");
                return "";
            }

            if (difference > 0) {
                for (int k = 0; k < difference / 2; k++) {
                    codePattern.append((i + 1)).append(" ");
                }
            } else if (difference < 0) {
                for (int k = 0; k < -difference / 2; k++) {
                    codePattern.append(-(i + 1)).append(" ");
                }
            }
        }

        return codePattern.toString().trim();
    }

    private static void showAlert(String content) {
        Text text = new Text(content);
        text.setWrappingWidth(350);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Pattern Calculator");
        alert.getDialogPane().setContent(text);
        alert.getDialogPane().setPrefWidth(400);
        alert.getDialogPane().setPadding(new Insets(10));
        alert.showAndWait();
    }
}

