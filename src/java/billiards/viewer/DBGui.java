package billiards.viewer;

import billiards.database.Admin;

import java.util.List;
import java.util.Optional;

import javax.naming.ldap.Rdn;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class DBGui {
    // Make all the UI variables non-static data members, and initialize
    // them all with their basic constructors. Now that they are all declared
    // and initialized, we can freely set all of their properties without
    // worrying about the order they were declared in and stuff.
    private final ListView<String> listView = new ListView<>();
    private final Button newButton = new Button();
    private final Button deleteButton = new Button();
    private final Button clearButton = new Button();
    private final Button selectButton = new Button();
    private final Region spacer = new Region();
    private final HBox programBox = new HBox();
    private final HBox buttonBox = new HBox();
    private final VBox root = new VBox();
    private final Scene scene = new Scene(root);
    private final Stage stage = new Stage();
    
    private final RadioButton vwrRadio = new RadioButton();
    private final RadioButton patRadio = new RadioButton();
    private final ToggleGroup programsGrp = new ToggleGroup();

    private Optional<String> databaseName = Optional.empty();

    // We are going to make the data members non-static, because static mutable
    // data is horrible and a disaster waiting to happen. Also, initialization
    // blocks are ugly
    public DBGui() {
        // All the GUI components have been declared and initialized. Now we
        // describe their properties
        // Declarative part
        // This is the natural place to initialize stuff, because it is a constructor!
        // Now, what the best logical ordering of the initialization code is is still
        // up for grabs, but that's just fine.

        stage.setScene(scene);
        stage.setTitle("Select a Database");

        vwrRadio.setText("Viewer");
        vwrRadio.setToggleGroup(programsGrp);
        vwrRadio.setSelected(true);
        patRadio.setText("Pattern Finder");
        patRadio.setToggleGroup(programsGrp);
        
        
        newButton.setText("New");
        newButton.setOnAction(event -> newDatabase());

        deleteButton.setText("Delete");
        deleteButton.setOnAction(event -> deleteDatabase());

        clearButton.setText("Clear");
        clearButton.setOnAction(event -> clearDatabase());

        selectButton.setText("Select");
        selectButton.setOnAction(event -> selectDatabase());

        HBox.setHgrow(spacer, Priority.ALWAYS);

        //programBox.getChildren().addAll(vwrRadio, patRadio);
        //programBox.getChildren().addAll(vwrRadio);
        programBox.setSpacing(10);
        programBox.setAlignment(Pos.CENTER);
        
        buttonBox.getChildren().addAll(deleteButton, newButton, clearButton, spacer, selectButton);
        buttonBox.setSpacing(10);

        root.getChildren().addAll(listView, programBox, buttonBox);
        root.setSpacing(10);
        root.setPadding(new Insets(10));
    }

    // One option is to make these variables static, and then reinitialize them
    // each time when we open the DBGui. That is a bad idea. Global mutable state
    // is the most horrible thing ever. So, we make them private, and create a new
    // one each time we open it up.

    public Optional<String> getDbName() {
        Admin.initDatabaseDirectory();
        final List<String> dbs = Admin.listDatabases();
        listView.getItems().setAll(dbs);

        // On one hand, it's nice to make everything members of the class, because
        // then we don't have to worry about what the order they are declared in
        // (For example, setting the action of the doneButton, we can just reference
        // the stage). So there's a thought. We make all the GUI stuff class members
        // (I'm not sure whether static or not yet), initialize them in the beginning
        // and then provide the imperative logic in each of the functions. Hmmm, me like
        // I think I'll make these non-static. Global mutable state is very bad. For example,
        // we make the stage and listView static, use it once, and then try again. Is
        // everything properly initialized again? Not a priori.
        // We want to have a nice separation between the declarative layout and the
        // imperative code. You know?

        // Waits for the stage to close, and then continues
        stage.showAndWait();

        return this.databaseName;
    }
    
    // not sure if this is the best way to do it, we will want to combine this function
    // into getDBname to get the best solution I think. This *will* work just fine though.
    public boolean getProgram() {
    	return vwrRadio.isSelected();
    }

    // Prefer static functions that don't access global state when possible
    // AKA make it as pure as possible

    private void newDatabase() {
        final TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle("New Database");
        dialog.setHeaderText("New Database");
        dialog.setContentText("Enter the name of the new database:");

        dialog.showAndWait().ifPresent(untrimmedDbName -> {
            final String dbName = untrimmedDbName.trim();

            if (dbName.isEmpty()) {
                final Alert error = new Alert(AlertType.ERROR);
                error.setTitle("Invalid Database Name");
                error.setHeaderText("Invalid Database Name");
                error.setContentText("Database name cannot be empty or composed of just whitespace.");
                error.showAndWait();
            } else {

                if (listView.getItems().contains(dbName)) {
                    // Database already exists
                    final Alert error = new Alert(AlertType.ERROR);
                    error.setTitle("Database Already Exists");
                    error.setHeaderText("Database Already Exists");
                    error.setContentText(String.format("Database \"%s\" already exists.", dbName));
                    error.showAndWait();

                } else {
                    Admin.newDatabase(dbName);
                    final List<String> dbs = Admin.listDatabases();
                    listView.getItems().setAll(dbs);
                }
            }
        });
    }

    // Now, what about this? Should we make functions like this static
    // I don't think it would hurt. Passing in a parameter sort of
    // indicates which parts of the GUI we need to access to do this.

    private void deleteDatabase() {

        final String dbName = listView.getSelectionModel().getSelectedItem();

        if (dbName == null) {

            final Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("No Database Selected");
            alert.setHeaderText("No Database Selected");
            alert.setContentText("Please select a database to delete.");

            alert.showAndWait();
        } else {

            // Confirm database deletion
            final Alert confirmation = new Alert(AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm Database Delete");
            confirmation.setHeaderText("Confirm Database Delete");
            confirmation.setContentText(String.format("Delete database \"%s\"?", dbName));

            confirmation.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .ifPresent(response -> {
                    Admin.deleteDatabase(dbName);
                    final List<String> dbs = Admin.listDatabases();
                    listView.getItems().setAll(dbs);
                });
        }
    }

    // Essentially the same as deleteDatabase, but we clear instead
    private void clearDatabase() {

        final String dbName = listView.getSelectionModel().getSelectedItem();

        if (dbName == null) {

            final Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("No Database Selected");
            alert.setHeaderText("No Database Selected");
            alert.setContentText("Please select a database to clear.");

            alert.showAndWait();
        } else {

            // Confirm clearing
            final Alert confirmation = new Alert(AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm Database Clear");
            confirmation.setHeaderText("Confirm Database Clear");
            confirmation.setContentText(String.format("Clear database \"%s\"?", dbName));

            confirmation.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .ifPresent(response -> {
                    Admin.clearDatabase(dbName);
                });
        }
    }

    private void selectDatabase() {
        final String dbName = listView.getSelectionModel().getSelectedItem();

        // If dbName == null, then nothing was selected, so show an alert box
        if (dbName == null) {
            final Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("No Database Selected");
            alert.setHeaderText("No Database Selected");
            alert.setContentText("Please select a database.");

            alert.showAndWait();
        } else {
            this.databaseName = Optional.of(dbName);
            stage.close();
        }
    }
}
