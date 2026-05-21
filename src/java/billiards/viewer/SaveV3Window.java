package billiards.viewer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SaveV3Window {

    // This class is created for the functionality of saving Vary 3 matching pairs when intended.

    private final Button browseBtn = new Button();
    private final Button saveMBtn = new Button();
    private final Button saveLBtn = new Button();
    private final Button clearBtn = new Button();
    private final TextField countField = new TextField("1");
    private final TextField saveToField = new TextField();
    private final HBox root = new HBox();
    private final VBox labelVBox = new VBox();
    private final HBox saveHBox = new HBox();
    private final VBox btnVBox = new VBox(10);
    private final VBox fieldVBox = new VBox();
    private final HBox upperBtnHBox = new HBox();
    public final Stage stage = new Stage();
    private final Scene scene = new Scene(root);
    private final Label saveToLabel = new Label();
    private final Label countLabel = new Label();
    private FileChooser fileChooser = new FileChooser();

    //----------------------------------------------//
    // Variables required to store default location (if specified) and count for the no of pairs to be stored.
    public int count = Integer.parseInt(countField.getText());
    public static String defaultLocation = ""; // default location is the last used
    public String location = defaultLocation;
    //----------------------------------------------//

    public SaveV3Window(final String windowTitle) {

        // Creating objects for graphical interface
        stage.setScene(scene);
        stage.setTitle(windowTitle);
        stage.setOnCloseRequest(event -> {
            stage.close();
        });

        countField.setMaxWidth(50);
        countField.setEditable(true);
        countField.setFont(Font.font("Monaco", 16));


        saveToField.setPrefColumnCount(50);
        saveToField.setEditable(true);
        saveToField.setFont(Font.font("Monaco", 16));
        saveToField.setText(defaultLocation);

        saveToLabel.setText("Save to: ");
        saveToLabel.setPadding(new Insets(5,5,10,10));

        countLabel.setText("Count: ");
        countLabel.setPadding(new Insets(5,5,5,10));

        VBox.setVgrow(saveToField, Priority.ALWAYS);

        labelVBox.getChildren().addAll(saveToLabel, countLabel);
        labelVBox.setSpacing(10);
        saveHBox.getChildren().addAll(saveMBtn, saveLBtn);
        saveHBox.setSpacing(5);
        saveHBox.setPadding(new Insets(5,5,5,5));
        upperBtnHBox.getChildren().addAll(browseBtn, clearBtn);
        upperBtnHBox.setSpacing(5);
        upperBtnHBox.setPadding(new Insets(5,5,5,5));
        btnVBox.getChildren().addAll(upperBtnHBox, saveHBox);
        btnVBox.setSpacing(10);
        btnVBox.setPadding(new Insets(0, 10, 10, 0));
        btnVBox.setAlignment(Pos.CENTER);
        fieldVBox.getChildren().addAll(saveToField, countField);
        fieldVBox.setSpacing(10);
        fieldVBox.setPadding(new Insets(0, 10, 10, 0));

        root.getChildren().addAll(labelVBox, fieldVBox, btnVBox);
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        // Browse button functionality
        browseBtn.setText("Browse");
        Utils.colorButton(browseBtn, Color.SKYBLUE, Color.GOLD);
        browseBtn.setOnAction(event -> {browse();});

        // Clear button
        clearBtn.setText("Clear file");
        Utils.colorButton(clearBtn, Color.WHITE, Color.GOLD);
        clearBtn.setOnAction(event -> {clear();});

        // Save Matching button functionality
        saveMBtn.setText("Save Matching");
        Utils.colorButton(saveMBtn, Color.SKYBLUE, Color.GOLD);
        saveMBtn.setOnAction(event -> {saveM();});

        // Save Latest button functionality
        saveLBtn.setText("Save Latest Vary");
        Utils.colorButton(saveLBtn, Color.SKYBLUE, Color.GOLD);
        saveLBtn.setOnAction(event -> {saveL();});

    }

    public void show() {
        stage.show();
    }

    public void close() {
        stage.close();
    }

    private void browse() {
        // browse function in the event of browse button pressed.
        // raises Error alert if file selected is not a text file.
        final File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            String path = file.getAbsolutePath();
            if (path.endsWith(".txt")) {
                location = path;
                saveToField.setText(path);
            }
            else {
                final Alert select = new Alert(AlertType.ERROR);
                select.setContentText("Please select a valid text file.");
                select.show();
            }

        }
    }

    private void saveM() {
        // save function in the event of save button pressed
        // Handles the exceptions where codes are not saved in the case of IO exception
        // or file not selected.
        try {
            count = Integer.parseInt(countField.getText());
            if (!location.endsWith(".txt")) {
                final Alert select = new Alert(AlertType.ERROR);
                select.setContentText("File not selected.");
                select.show();
            }
            else {
                defaultLocation = location;
                File toSave = new File(location);
                FileWriter writer = new FileWriter(location, true);
                if (toSave.createNewFile()) {
                    for (String code:BoyanMenu.savePairs){
                        writer.write(code + "\n");
                    }
                }
                else {
                    if (count <= BoyanMenu.savePairs.size()) {
                        for (int i = 0; i < count; i++) {
                            String code = BoyanMenu.savePairs.get(i);
                            writer.append(code + "\n");
                        }
                    }
                    else {
                        writer.close();
                        throw new IOException("Count is more than number of codes.");
                    }
                }
                writer.close();
                stage.close();
            }
        }
        catch (IOException e) {
            final Alert select = new Alert(AlertType.ERROR);
            String message = "Code sequences not saved.";
            if (e.getMessage() != "") {
                message += "\n" + e.getMessage();
            }
            select.setContentText(message);
            select.show();
        }
    }

    private void saveL() {
        try {
            count = Integer.parseInt(countField.getText());
            if (!location.endsWith(".txt")) {
                final Alert select = new Alert(AlertType.ERROR);
                select.setContentText("File not selected.");
                select.show();
            }
            else {
                defaultLocation = location;
                File toSave = new File(location);
                FileWriter writer = new FileWriter(location, true);
                if (toSave.createNewFile()) {
                    for (String code:BoyanMenu.varySeq){
                        writer.write(code + "\n");
                    }
                }
                else {
                    if (count <= BoyanMenu.varySeq.size()) {
                        for (int i = 0; i < count; i++) {
                            String code = BoyanMenu.varySeq.get(i);
                            writer.append(code + "\n");
                        }
                    }
                    else {
                        writer.close();
                        throw new IOException("Count is more than number of codes.");
                    }
                }
                writer.close();
                stage.close();
            }
        }

        catch(IOException e) {
            final Alert select = new Alert(AlertType.ERROR);
            String message = "Code sequences not saved.";
            if (e.getMessage() != "") {
                message += "\n" + e.getMessage();
            }
            select.setContentText(message);
            select.show();
        }
    }

    private void clear() {
        try {
            if (!location.endsWith(".txt")) {
                final Alert select = new Alert(AlertType.ERROR);
                select.setContentText("File not selected.");
                select.show();
            } else {
                defaultLocation = location;
                File toSave = new File(location);
                FileWriter writer = new FileWriter(location, false);
                writer.close();
                stage.close();
            }

        }
        catch (IOException e){
            final Alert select = new Alert(AlertType.ERROR);
            String message = "Code sequences not saved.";
            if (e.getMessage() != "") {
                message += "\n" + e.getMessage();
            }
            select.setContentText(message);
            select.show();
        }
    }

}
