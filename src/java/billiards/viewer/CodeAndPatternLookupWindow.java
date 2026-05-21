package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.InvalidCodeSequence;
import billiards.database.Admin;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javaslang.control.Either;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Zhao Yu Li, Jun 12, 2025.
 * A window that looks in the garbage database for code sequence - iteration pattern pairs. It is currently designed to
 * work only with an IterateToLimitWindow as the parent window. In regard to implementation, this window works with any
 * class that gives access to its Stage, and implements the addToContent method.
 */
public class CodeAndPatternLookupWindow {
    private static final int INITIAL_LOAD = 20;
    private static final int BATCH_SIZE = 5;
    private final ObservableList<CodeAndPattern> originalOrder = FXCollections.observableArrayList();
    private final ObservableList<CodeAndPattern> data = FXCollections.observableArrayList();
    private final SortedList<CodeAndPattern> sortedData = new SortedList<>(data);
    private boolean isLoading = false;
    private int currentOffset = 0;
    private boolean scrollBarInitialized = false;

    private final Stage stage = new Stage();
    TableView<CodeAndPattern> tableView = new TableView<>(sortedData);

    private static final int CELL_WIDTH = 200;
    private static final int EXPANDED_WIDTH = 400;

    public static class ScrollableTableCell<S, T> extends TextFieldTableCell<S, T> {
        private final Tooltip tooltip = new Tooltip();
        private final ScrollPane scrollPane = new ScrollPane();
        private final Label contentLabel = new Label();

        // Jun 12, 2025. DeepSeek, a table cell that is horizontally scrollable and shows full content on mouse hover
        public ScrollableTableCell() {
            scrollPane.setContent(contentLabel);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setMaxHeight(30);
            scrollPane.setPrefViewportWidth(CELL_WIDTH);
            scrollPane.getStyleClass().add("cell-scroll-pane");

            HBox.setHgrow(scrollPane, Priority.ALWAYS);
            this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            // Show full content on hover
            this.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
                String text = getItem() != null ? getItem().toString() : "";
                tooltip.setText(text);
                Tooltip.install(this, tooltip);
                scrollPane.setPrefViewportWidth(EXPANDED_WIDTH);
            });

            this.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
                Tooltip.uninstall(this, tooltip);
                scrollPane.setPrefViewportWidth(CELL_WIDTH);
            });
        }

        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                contentLabel.setText(null);
                this.setGraphic(null);
            } else {
                contentLabel.setText(item.toString());
                this.setGraphic(scrollPane);

                // Jun 12, 2025.
                // ChatGPT, delay styling to ensure scrollbars are created; Makes the scrollbar thin.
                Platform.runLater(() -> {
                    scrollPane.setPrefHeight(8);
                    scrollPane.lookupAll(".scroll-bar").forEach(node -> {
                        if (node instanceof ScrollBar) {
                            ScrollBar sb = (ScrollBar) node;
                            if (sb.getOrientation() == Orientation.HORIZONTAL) {
                                sb.setPrefHeight(8);
                                sb.setStyle("-fx-background-color: transparent;");
                                sb.lookupAll(".thumb").forEach(thumb ->
                                        thumb.setStyle("-fx-background-color: rgba(100,100,100,0.6);"
                                                + "-fx-background-radius: 3px;"));
                            }
                        }
                    });
                });
            }
        }
    }

    // The wrapper class for a code sequence - iteration pattern pair that allows a TableView to display its contents
    public static class CodeAndPattern {
        private final String codeSequence;
        private final String iterationPattern;
        private final String type;
        private final int numOfCodeComponents;
        private final ArrayList<ClassifiedCodeSequence> classifiedCodeSequences = new ArrayList<>();

        public CodeAndPattern(String column1, String column2) {
            this.codeSequence = column1;
            this.iterationPattern = column2;

            String[] codeComponents = column1.trim().split(",");
            numOfCodeComponents = codeComponents.length;

            for (String codeComponent : codeComponents) {
                Optional<ImmutableIntList> codeNumbers = Utils.splitString(codeComponent);

                if (codeNumbers.isPresent()) {
                    Either<InvalidCodeSequence, ClassifiedCodeSequence> either = ClassifiedCodeSequence.create(codeNumbers.get());

                    if (either.isRight()) {
                        classifiedCodeSequences.add(either.get());
                    }
                }
            }

            this.type = calcType();
        }

        public String calcType() {
            if (classifiedCodeSequences.size() != numOfCodeComponents) return "N/A";

            if (classifiedCodeSequences.size() == 1) return classifiedCodeSequences.get(0).codeType.toString();

            if (classifiedCodeSequences.size() == 3) {
                if (classifiedCodeSequences.get(0).stable &&
                        !classifiedCodeSequences.get(1).stable &&
                        classifiedCodeSequences.get(2).stable) return "Triple";
            }

            return "N/A";
        }

        public String getCodeSequence() { return codeSequence; }
        public String getIterationPattern() { return iterationPattern; }
        public String getType() { return type; }
    }

    public CodeAndPatternLookupWindow(IterateToLimitWindow iterateToLimitWindow) {
        // Jun 12, 2025. DeepSeek, create columns with custom cell factory
        Callback<TableColumn<CodeAndPattern, String>, TableCell<CodeAndPattern, String>> cellFactory =
                col -> new ScrollableTableCell<>();

        // Create columns
        TableColumn<CodeAndPattern, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setCellFactory(cellFactory);
        typeCol.setPrefWidth(CELL_WIDTH);

        TableColumn<CodeAndPattern, String> codeCol = new TableColumn<>("Code Sequence");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("codeSequence"));
        codeCol.setCellFactory(cellFactory);
        codeCol.setPrefWidth(CELL_WIDTH);

        TableColumn<CodeAndPattern, String> patternCol = new TableColumn<>("Iteration Pattern");
        patternCol.setCellValueFactory(new PropertyValueFactory<>("iterationPattern"));
        patternCol.setCellFactory(cellFactory);
        patternCol.setPrefWidth(CELL_WIDTH);

        tableView.getColumns().add(typeCol);
        tableView.getColumns().add(codeCol);
        tableView.getColumns().add(patternCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableView.setPrefSize(CELL_WIDTH * 3, CELL_WIDTH * 2);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getSelectionModel().selectFirst();

        // Jun 13, 2025.
        // ChatGPT, maintain a list of items in their insertion order so that we can return to this order if we don't
        // want to sort the items anymore.
        data.addListener((ListChangeListener<CodeAndPattern>) change -> {
            while (change.next()) {
                if (change.wasAdded()) originalOrder.addAll(change.getAddedSubList());
                if (change.wasRemoved()) originalOrder.removeAll(change.getRemoved());
            }
        });

        sortedData.comparatorProperty().bind(tableView.comparatorProperty());

        // This allows the items to be put back in the insertion order after clicking the column header a third time.
        Runnable applySort = () -> {
            if (tableView.getSortOrder().isEmpty()) {
                // user reached UNSORTED -> restore insertion order
                sortedData.comparatorProperty().unbind();
                sortedData.setComparator(Comparator.comparingInt(originalOrder::indexOf));
                sortedData.comparatorProperty().bind(tableView.comparatorProperty());
            } else {
                // user wants a sort -> follow it
                sortedData.comparatorProperty().bind(tableView.comparatorProperty());
            }
        };

        // Bind our new sorting method to the TableView and its columns
        tableView.getSortOrder().addListener((ListChangeListener.Change<? extends TableColumn<CodeAndPattern, ?>> c) -> applySort.run());
        for (TableColumn<?,?> col : tableView.getColumns()) {
            col.sortTypeProperty().addListener((obs, oldV, newV) -> applySort.run());
        }

        VBox vbox = getVBox(iterateToLimitWindow);
        Scene scene = new Scene(vbox, CELL_WIDTH * 3, CELL_WIDTH * 2);
        stage.setScene(scene);

        stage.initModality(Modality.NONE);
        stage.initOwner(iterateToLimitWindow.getStage());
    }

    private VBox getVBox(IterateToLimitWindow iterateToLimitWindow) {
        final Button addButton = new Button("Add");
        addButton.setOnAction(e -> {
            ObservableList<CodeAndPattern> codeAndPatterns = tableView.getSelectionModel().getSelectedItems();

            for (CodeAndPattern codeAndPattern : codeAndPatterns)
                iterateToLimitWindow.addToContent(codeAndPattern.getCodeSequence(), codeAndPattern.getIterationPattern());
        });

        // Set up the scene
        VBox vbox = new VBox(10, tableView, addButton);
        vbox.setPadding(new Insets(10));
        return vbox;
    }

    /**
     * Zhao Yu Li, Jun 12, 2025.
     * Looks up code sequence - iteration pattern pairs in the garbage database starting from (but excluding) the
     * OFFSET'th entry, and retrieving at most LIMIT entries.
     * @param limit The maximum number of entries to retrieve.
     * @param offset The number of entries to skip.
     */
    private void lookUpIterPat(int limit, int offset) {
        isLoading = true;
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            final String selectPatternQuery = "SELECT code_sequence,iter_pattern FROM main.iteration_pattern ORDER BY last_used DESC LIMIT ? OFFSET ?;";

            String dbName = "garbage";
            try (Connection conn = DriverManager.getConnection(Admin.getUrl(dbName));
                 PreparedStatement stmt = conn.prepareStatement(selectPatternQuery)) {
                stmt.setInt(1, limit);
                stmt.setInt(2, offset);

                ResultSet rs = stmt.executeQuery();

                ObservableList<CodeAndPattern> newCodesAndPatterns = FXCollections.observableArrayList();

                while (rs.next()) {
                    newCodesAndPatterns.add(new CodeAndPattern(rs.getString(1), rs.getString(2)));
                }

                // Update UI on JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {
                    data.addAll(newCodesAndPatterns);
                    currentOffset += newCodesAndPatterns.size();
                    isLoading = false;
                });
            } catch (SQLException e) {
                javafx.application.Platform.runLater(() -> isLoading = false);
                throw new RuntimeException(e);
            }

            executor.shutdown();
        });
    }

    private ScrollBar getVerticalScrollbar(TableView<?> tableView) {
        for (Node n : tableView.lookupAll(".scroll-bar")) {
            if (n instanceof ScrollBar) {
                ScrollBar bar = (ScrollBar) n;
                if (bar.getOrientation() == Orientation.VERTICAL) {
                    return bar;
                }
            }
        }
        return null;
    }

    public void show() {
        // Initially loads INITIAL_LOAD entries from the database, and loads BATCH_SIZE more each time we scroll to the
        // bottom of the list.
        if (data.isEmpty()) lookUpIterPat(INITIAL_LOAD, 0);  // Load data from database

        this.stage.show();

        // Enables load on-demand based on the position of the scrollbar.
        if (!scrollBarInitialized) {
            Platform.runLater(() -> {
                ScrollBar scrollBar = getVerticalScrollbar(tableView);

                if (scrollBar != null) {
                    scrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal.doubleValue() >= 0.95 && !isLoading) {
                            lookUpIterPat(BATCH_SIZE, currentOffset);
                        }
                    });
                }

                scrollBarInitialized = true;
            });
        }
    }
}
