package billiards.viewer;

import billiards.database.Database;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.eclipse.collections.api.list.primitive.MutableIntList;

import java.util.ArrayList;

/**
 * Zhao Yu Li, May 27, 2025.
 * A window for looking up patterns by code sequence or odd-even pattern. This window is launched from the iterations window.
 */
public class PatternLookupWindow {
    private final String codeSequence;
    private String oddEvenPattern = "";
    private String selectedPattern = "";

    ObservableList<String> options = FXCollections.observableArrayList(
            "Code Sequence",
            "Odd-Even Pattern"
    );

    // List of patterns found in the database
    private final ListView<String> patternsListView = new ListView<>();
    private ObservableList<String> patternsList = FXCollections.observableArrayList();  // This content of this list will be displayed in the ListView

    // These two lists are used to store query results from the database
    private ArrayList<String> codeSequencePatterns = new ArrayList<>();
    private ArrayList<String> oddEvenPatterns = new ArrayList<>();

    // System clipboard so we can copy patterns to the clipboard
    private final Clipboard clipboard = Clipboard.getSystemClipboard();

    public PatternLookupWindow(String codeSequence) {
        this.codeSequence = codeSequence.trim();
        this.oddEvenPattern = getOEPattern(this.codeSequence);

        ComboBox<String> lookupMethodComboBox = new ComboBox<>(options);
        lookupMethodComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
           if (newValue.equals("Code Sequence")) {
               if (this.codeSequencePatterns.isEmpty()) this.codeSequencePatterns = retrievePatternByCodeSequence();

               patternsList = FXCollections.observableArrayList(this.codeSequencePatterns);
           } else if (newValue.equals("Odd-Even Pattern")) {
               if (this.oddEvenPatterns.isEmpty()) this.oddEvenPatterns = retrievePatternsByOEPattern();

               patternsList = FXCollections.observableArrayList(this.oddEvenPatterns);
           }
           patternsListView.setItems(patternsList);
           patternsListView.getSelectionModel().selectFirst();
           patternsListView.scrollTo(0);

           selectedPattern = patternsListView.getSelectionModel().getSelectedItem();
        });

        lookupMethodComboBox.getSelectionModel().selectFirst();
        selectedPattern = patternsListView.getSelectionModel().getSelectedItem();

        patternsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
           if (newValue != null) selectedPattern = newValue;
        });

        Button copyButton = new Button();
        copyButton.setText("Copy");
        copyButton.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(selectedPattern);
            clipboard.setContent(content);
        });

        // Either lookup by code sequence or odd-even pattern
        Label lookupMethodLabel = new Label("Lookup Patterns By:");
        HBox lookupMethodHbox = new HBox();
        lookupMethodHbox.getChildren().addAll(lookupMethodLabel, lookupMethodComboBox, copyButton);
        lookupMethodHbox.setSpacing(10);

        VBox root = new VBox();
        root.getChildren().addAll(lookupMethodHbox, patternsListView);
        root.setSpacing(10);

        Stage stage = new Stage();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        String windowTitle = "Pattern Lookup";
        stage.setTitle(windowTitle);
        stage.show();
    }

    private String getOEPattern(String codeSequence) {
        final String[] codeSequences = codeSequence.split(", ");
        final StringBuilder OEString = new StringBuilder();

        for (int i = 0; i < codeSequences.length; i++) {
            if (codeSequences[i].isEmpty()) continue;
            if (i > 0) OEString.append(",");

            final String[] codeNumbers = codeSequences[i].split(" ");

            for (String codeNumber : codeNumbers) OEString.append(Integer.parseInt(codeNumber) % 2 == 0 ? "E" : "O");
        }

        return OEString.toString();
    }

    private ArrayList<String> retrievePatternByCodeSequence() {
        return Database.lookUpIterPatByCodeSeq(this.codeSequence, "garbage");
    }

    private ArrayList<String> retrievePatternsByOEPattern() {
        return Database.lookUpIterPatByOEPat(this.oddEvenPattern, "garbage");
    }

    public static String codeNumbersToString(MutableIntList[] currentCodeNumbers) {
        final StringBuilder codeNumbersString = new StringBuilder();

        for (int i = 0; i < currentCodeNumbers.length; i++) {
            if (currentCodeNumbers[i].isEmpty()) continue;
            if (i > 0)
                codeNumbersString.append(", ");

            for (int j = 0; j < currentCodeNumbers[i].size(); j++) {
                if (j > 0)
                    codeNumbersString.append(" ");

                codeNumbersString.append(currentCodeNumbers[i].get(j));
            }
        }

        return codeNumbersString.toString();
    }
}
