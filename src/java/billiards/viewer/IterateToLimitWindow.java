package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.InvalidCodeSequence;
import billiards.codeseq.Storage;
import billiards.database.Database;
import billiards.geometry.ConvexPolygon;
import billiards.utils.BatchLoadStorage;
import billiards.utils.Polygon;
import billiards.wrapper.ConnectionPool;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import javaslang.Tuple3;
import javaslang.control.Either;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Zhao Yu Li, Jun 06, 2025.
 * <p>
 * Iterate to the limit. Given a ConvexPolygon, for each code sequence (single or triple) - iteration pattern pair in a
 * list, we iterate forward (addition) and backward (subtraction) until one of the following two conditions occurred:
 * </p>
 * <ol>
 *     <li>The number of iterations reached a user specified limit.</li>
 *     <li>New code sequences produced from the iteration no longer intersect with the user specified polygon.</li>
 * </ol>
 * <p>
 * All produced codes are (optionally) drawn on the screen and added to the cover.
 * </p>
 */
public class IterateToLimitWindow {
    private final String contentFileName = "iterToLimit.txt";

    private final TextArea polygonTextArea = new TextArea();
    private final TextArea stablesTextArea = new TextArea();
    private final TextArea pmStablesTextArea = new TextArea();  // "pm" stands for +/-
    private final TextArea unstablesTextArea = new TextArea();
    private final TextArea pmUnstablesTextArea = new TextArea();  // "pm" stands for +/-
    private final TextArea triplesTextArea = new TextArea();

    private final HashMap<String, TextArea> textAreaMap = new HashMap<>();

    private final TextField limitTextField = new TextField();

    private final CheckBox drawCheckbox = new CheckBox();
    private final CheckBox coverCheckbox = new CheckBox();

    private final Stage stage = new Stage();

    private final ConnectionPool pool;

    // The IterateToLimitWindow uses this Observable Boolean Property to notify the Viewer when the iterate-to-limit
    // task is finished
    private SimpleBooleanProperty finish = null;

    // This array is used to store the result from the iterate-to-limit task
    private ArrayList<Tuple3<ArrayList<Storage>, ArrayList<ArrayList<Storage>>, ArrayList<ArrayList<Storage>>>> results = null;

    // This boolean value ensures that only one iterate-to-limit task is running at any time, for this task can be very
    // resource consuming
    private boolean running = false;

    private CodeAndPatternLookupWindow codeAndPatternLookupWindow = null;

    public IterateToLimitWindow(ConnectionPool pool) {
        this.pool = pool;

        String fileContent = Utils.readFromFile(contentFileName);
        String[] contents = fileContent.split("-----");

        Button findPatternButton = new Button();
        Button lookupButton = new Button();
        Button runButton = new Button();
        VBox root = new VBox(
                10,
                getRoot(),
                new HBox(10, findPatternButton, lookupButton, limitTextField, drawCheckbox, coverCheckbox, runButton)
        );

        if (contents.length > 0) polygonTextArea.setText(contents[0].trim());
        if (contents.length > 1) stablesTextArea.setText(contents[1].trim());
        if (contents.length > 2) pmStablesTextArea.setText(contents[2].trim());
        if (contents.length > 3) unstablesTextArea.setText(contents[3].trim());
        if (contents.length > 4) pmStablesTextArea.setText(contents[4].trim());
        if (contents.length > 5) triplesTextArea.setText(contents[5].trim());

        final Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("Iterate To Limit Window");
        stage.setHeight(800);
        stage.setOnCloseRequest(event -> {
            this.results = null;
            markFinishedIfPresent(this.finish);

            saveContentsToFile();

            stage.close();
        });
        stage.setOnHiding(event -> saveContentsToFile());

        root.setPadding(new Insets(10));

//        setUpTooltip(polygonTextArea, new Tooltip(
//                "Enter each (x,y) coordinate in a separate line, with a whitespace separating the x and y coordinates.")
//        );
        polygonTextArea.setWrapText(true);
        polygonTextArea.setMaxHeight(130);
        polygonTextArea.setFont(new Font("monospace", 16));

//        setUpTooltip(stablesTextArea, getTooltip(
//                "Enter each stable - iteration pattern pair in a separate line, with a semicolon " +
//                        "separating the stable and the pattern. Stables in this text area will be given an " +
//                        "all-positive iteration pattern when you use the \"Find Pattern\" button."
//        ));
        stablesTextArea.setWrapText(true);
        stablesTextArea.setFont(new Font("monospace", 16));

//        setUpTooltip(pmStablesTextArea, getTooltip(
//                "Enter each stable - iteration pattern pair in a separate line, with a semicolon " +
//                        "separating the stable and the pattern. Stables in this text area will be given an " +
//                        "plus-minus iteration pattern when you use the \"Find Pattern\" button."
//        ));
        pmStablesTextArea.setWrapText(true);
        pmStablesTextArea.setFont(new Font("monospace", 16));

//        setUpTooltip(unstablesTextArea, getTooltip(
//                "Enter each unstable - iteration pattern pair in a separate line, with a semicolon " +
//                        "separating the unstable and the pattern. Unstables in this text area will be given an " +
//                        "all-positive iteration pattern when you use the \"Find Pattern\" button."
//        ));
        unstablesTextArea.setWrapText(true);
        unstablesTextArea.setFont(new Font("monospace", 16));

//        setUpTooltip(pmUnstablesTextArea, getTooltip(
//                "Enter each unstable - iteration pattern pair in a separate line, with a semicolon " +
//                        "separating the unstable and the pattern. Unstables in this text area will be given an " +
//                        "plus-minus iteration pattern when you use the \"Find Pattern\" button."
//        ));
        pmUnstablesTextArea.setWrapText(true);
        pmUnstablesTextArea.setFont(new Font("monospace", 16));

//        setUpTooltip(triplesTextArea, getTooltip(
//                "Enter each triple - iteration pattern pair in a separate line, with a semicolon " +
//                        "separating the triple and the pattern. Different components of the triple should be" +
//                        "separated by a comma."
//        ));
        triplesTextArea.setWrapText(true);
        triplesTextArea.setFont(new Font("monospace", 16));

        textAreaMap.put("Polygon", polygonTextArea);
        textAreaMap.put("+ Stables", stablesTextArea);
        textAreaMap.put("+/- Stables", pmStablesTextArea);
        textAreaMap.put("+ Unstables", unstablesTextArea);
        textAreaMap.put("+/- Unstables", pmUnstablesTextArea);
        textAreaMap.put("Triples", triplesTextArea);

        findPatternButton.setText("Find Pattern");
        findPatternButton.setOnAction(event -> findIterationPattern());

        lookupButton.setText("Lookup");
        lookupButton.setOnAction(event -> {
            if (codeAndPatternLookupWindow == null) codeAndPatternLookupWindow = new CodeAndPatternLookupWindow(this);
            codeAndPatternLookupWindow.show();
        });

        limitTextField.setPrefColumnCount(4);
        limitTextField.setPromptText("Limit");
        limitTextField.setText(contents.length > 6 ? contents[6].trim() : "2");

        drawCheckbox.setSelected(true);
        drawCheckbox.setText("Draw");

        coverCheckbox.setSelected(true);
        coverCheckbox.setText("Add To Cover");

        runButton.setText("Run");
        runButton.setOnAction(event -> {
            if (run()) stage.close();
        });
    }

//    private static Tooltip getTooltip(String content) {
//        Tooltip tooltip = new Tooltip(content);
//        tooltip.setWrapText(true);
//        tooltip.setPrefWidth(400);
//        return tooltip;
//    }

//    private static void setUpTooltip(Control element, Tooltip tooltip) {
//        Tooltip.install(element, tooltip);
//
//        // Set up a timer to hide the tooltip
//        element.setOnMouseEntered(event -> {
//            tooltip.show(element,
//                    element.localToScreen(element.getBoundsInLocal()).getMinX(),
//                    element.localToScreen(element.getBoundsInLocal()).getMinY() - 30);
//
//            PauseTransition delay = new PauseTransition(Duration.seconds(5));
//            delay.setOnFinished(e -> tooltip.hide());
//            delay.play();
//        });
//
//        element.setOnMouseExited(event -> tooltip.hide());
//    }

    private HBox getTextAreaHBox(String labelText) {
        final Label label = new Label(labelText + ":");
        final Button clearButton = new Button("Clear");

        label.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        clearButton.setOnAction(event -> textAreaMap.get(labelText).setText(""));

        return new HBox(10, label, clearButton);
    }

    private ScrollPane getRoot() {
        final HBox polygonHBox = getTextAreaHBox("Polygon");
        final HBox stablesHBox = getTextAreaHBox("+ Stables");
        final HBox pmStablesHBox = getTextAreaHBox("+/- Stables");
        final HBox unstablesHBox = getTextAreaHBox("+ Unstables");
        final HBox pmUnstablesHBox = getTextAreaHBox("+/- Unstables");
        final HBox triplesHBox = getTextAreaHBox("Triples");

        VBox vbox = new VBox(10,
                polygonHBox, polygonTextArea,
                stablesHBox, stablesTextArea,
                triplesHBox, triplesTextArea,
                pmStablesHBox, pmStablesTextArea,
                unstablesHBox, unstablesTextArea,
                pmUnstablesHBox, pmUnstablesTextArea
        );

        ScrollPane scrollPane = new ScrollPane(vbox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);

        return scrollPane;
    }

    /**
     * The per code sequence - iteration pattern pair iterate-to-limit task to be submitted to the executor.
     * @param codePattern The code sequence - iteration pattern pair as a string.
     * @param polygon The ConvexPolygon to check for intersection.
     * @param limit The user specified limit of how many codes to produce (separately for each direction)
     * @return A 3-tuple: the Storages of the base code sequence, the list of Storages from iterating forward, and the list of Storages from iterating backward.
     */
    private Tuple3<
            ArrayList<Storage>,
            ArrayList<ArrayList<Storage>>,
            ArrayList<ArrayList<Storage>>
            > iterateTask(
                    String codePattern,
                    ConvexPolygon polygon,
                    int limit
    ) {
        // The code sequence - iteration pattern pairs are entered by the user and retrieved as strings. Users can make
        // mistakes, so we must check very carefully to make sure that everything is okay before we proceed.
        String trimmedCodePattern = codePattern.trim();

        // Ignore comments and empty lines
        if (trimmedCodePattern.startsWith("//") || trimmedCodePattern.isEmpty()) return null;

        String[] codeAndPattern = trimmedCodePattern.split("&");
        codeAndPattern[0] = Utils.trimCodeLine(codeAndPattern[0]);

        // The line must be the code and pattern separated by a semicolon
        if (codeAndPattern.length != 2) {
            System.out.println("Skipping '" + trimmedCodePattern + "' because it is not a valid code-pattern pair.");
            return null;
        }

        // Assume the section before the semicolon is the code sequence, and the one after is the iteration pattern
        String codeString = codeAndPattern[0].trim();
        String patternString = codeAndPattern[1].trim();

        String[] codes = codeString.split(",");
        String[] patterns = patternString.split(",");

        if (codes.length != 1 && codes.length != 3) {
            System.out.println("Skipping '" + trimmedCodePattern + "' because it is not a single nor a triple.");
            return null;
        }

        if (codes.length != patterns.length) {
            System.out.println("Skipping '" + trimmedCodePattern + "' because its code and iteration pattern are not both singles or not both triples.");
            return null;
        }

        ArrayList<ImmutableIntList> codeNumbersList = new ArrayList<>();
        ArrayList<ImmutableIntList> patternNumbersList = new ArrayList<>();

        for (int i = 0; i < codes.length; i++) {
            Optional<ImmutableIntList> codeOptional = Utils.splitString(codes[i]);

            codeOptional.ifPresent(codeNumbersList::add);

            Optional<ImmutableIntList> patternOptional = Utils.splitString(patterns[i]);

            patternOptional.ifPresent(patternNumbersList::add);
        }

        if (codeNumbersList.size() != codes.length || patternNumbersList.size() != patterns.length) {
            System.out.println("Skipping '" + trimmedCodePattern + "' because its code sequence or iteration pattern contains invalid values.");
            return null;
        }

        // We only iterate if the original code sequence is valid, and intersects with the polygon.
        ArrayList<ClassifiedCodeSequence> classCodeSequences = new ArrayList<>();

        for (ImmutableIntList immutableIntList : codeNumbersList) {
            IntArrayList codeNumbers = IntArrayList.newList(immutableIntList);

            Either<InvalidCodeSequence, ClassifiedCodeSequence> classCodeSequence = ClassifiedCodeSequence.create(codeNumbers);

            if (classCodeSequence.isRight()) classCodeSequences.add(classCodeSequence.get());
        }

        if (classCodeSequences.size() != codeNumbersList.size()) {
            System.out.println("Skipping '" + trimmedCodePattern + "' because it contains invalid code sequences.");
            return null;
        }

        // Check if this is a valid triple.
        if (classCodeSequences.size() == 3) {
            if (!(classCodeSequences.get(0).stable && !classCodeSequences.get(1).stable && classCodeSequences.get(2).stable)) {
                System.out.println("Skipping '" + trimmedCodePattern + "' because it is not a valid triple.");
                return null;
            }
        }

        // Jun 12, 2025. Save the code sequence - iteration pattern pairs to the garbage database so we can look it up
        // in the future.
        addIterPatToGarbage(patternString, classCodeSequences);

        ArrayList<Storage> originalStorages = BatchLoadStorage.batchLoadStorage(classCodeSequences, pool);

        int numOfIntersects = 0;
        for (Storage storage : originalStorages) {
            if (storage.intersects(polygon)) numOfIntersects++;
        }

        if (numOfIntersects != originalStorages.size()) {
            System.out.println("Skipping '" + trimmedCodePattern + "' because the base code sequence does not intersection with the polygon.");
            return null;
        }

        // At this point, the original code sequence is valid, the iteration pattern is valid given the original code
        // sequence, and the original code sequence intersects with the polygon. Thus, we can iterate.
        ArrayList<ArrayList<Storage>> forwardResult = iterate(codeNumbersList, patternNumbersList, polygon, 1, limit);
        ArrayList<ArrayList<Storage>> backwardResult = iterate(codeNumbersList, patternNumbersList, polygon, -1, limit);

        return new Tuple3<>(originalStorages, forwardResult, backwardResult);
    }

    /**
     * Iterate, either forwards or backwards for a single code sequence - iteration pattern pair.
     * @param codeNumbersList The code numbers of the code sequence to iterate.
     * @param patternNumbersList The iteration pattern.
     * @param polygon The ConvexPolygon to check for intersection.
     * @param direction 1 to iterate forwards; -1 to iterate backwards.
     * @param limit The user-specified maximum number of codes to produce.
     * @return A list of Storages which represents all the produced code sequences that intersect with the polygon.
     */
    private ArrayList<ArrayList<Storage>> iterate(
            ArrayList<ImmutableIntList> codeNumbersList,
            ArrayList<ImmutableIntList> patternNumbersList,
            ConvexPolygon polygon,
            int direction,
            int limit
    ) {
        boolean limitNotReached = true;
        int iterationCount = 0;
        ArrayList<ArrayList<Storage>> iterationResults = new ArrayList<>();

        // First, iteration forward
        while (limitNotReached) {
            if (++iterationCount > limit) break;

            ArrayList<ClassifiedCodeSequence> classCodeSequences = new ArrayList<>();

            for (int i = 0; i < codeNumbersList.size(); i++) {
                IntArrayList codeNumbers = calcCodeNumbers(codeNumbersList, patternNumbersList, i, iterationCount, direction);

                Either<InvalidCodeSequence, ClassifiedCodeSequence> classCodeSequence = ClassifiedCodeSequence.create(codeNumbers);

                if (classCodeSequence.isRight()) classCodeSequences.add(classCodeSequence.get());
            }

            // This code sequence is invalid; we can assume the rest of the code sequences in the iteration will also be
            // invalid.
            if (classCodeSequences.size() != codeNumbersList.size()) break;

            // Check if this is a valid triple.
            if (classCodeSequences.size() == 3) {
                if (!(classCodeSequences.get(0).stable && !classCodeSequences.get(1).stable && classCodeSequences.get(2).stable)) break;
            }

            ArrayList<Storage> storages = BatchLoadStorage.batchLoadStorage(classCodeSequences, pool);

            // ClassifiedCodeSequence reduced to an empty set, we have possibly reached the limit.
            if (storages.size() != classCodeSequences.size()) limitNotReached = false;

            int numOfIntersects = 0;
            for (Storage storage : storages) {
                if (storage.intersects(polygon)) numOfIntersects++;
            }

            if (numOfIntersects == storages.size()) {
                iterationResults.add(storages);
            } else {
                limitNotReached = false;
            }
        }

        return iterationResults;
    }

    /**
     * Perform one iteration for the i-th component of a code sequence.
     * @param codeNumbersList The code numbers for the code sequence.
     * @param patternNumbersList The pattern numbers for the iteration pattern.
     * @param i The i-th component to iterate.
     * @param iteration The iteration number we are at. On the n-th iteration, we add/subtract n * 2 to the base code numbers.
     * @param direction The direction of iteration (forward/addition or backward/subtraction).
     * @return The code numbers which are the result of the iteration.
     */
    private static IntArrayList calcCodeNumbers(
            ArrayList<ImmutableIntList> codeNumbersList,
            ArrayList<ImmutableIntList> patternNumbersList,
            int i,
            int iteration,
            int direction
    ) {
        IntArrayList codeNumbers = IntArrayList.newList(codeNumbersList.get(i));
        ImmutableIntList patternNumbers = patternNumbersList.get(i);

        for (int j = 0; j < patternNumbers.size(); j++) {
            int index = patternNumbers.get(j);

            // Allows negative index notation, which means to subtract two instead of adding 2.
            int value = index < 0 ? -2 : 2;
            value = value * iteration * direction;
            index = index < 0 ? -index : index;
            index = index - 1;
            codeNumbers.set(index, codeNumbers.get(index) + value);
        }
        return codeNumbers;
    }

    /**
     * Checks all the inputs from the window and runs the iterate-to-limit task for all the code sequence - iteration
     * pattern pairs.
     * @return True if user input has no errors (the task is running); false if there are errors in the user input.
     */
    private boolean run() {
        if (running) {
            Alert alert = getInfoAlertDialogue(
                    "Task already running",
                    "You can run only run a single instance of iterate-to-limit task at any time."
            );
            alert.showAndWait();

            return false;
        }

        running = true;

        if (polygonTextArea.getText().trim().isEmpty() ||
                (stablesTextArea.getText().trim().isEmpty()
                        && pmStablesTextArea.getText().trim().isEmpty()
                        && unstablesTextArea.getText().trim().isEmpty()
                        && pmUnstablesTextArea.getText().trim().isEmpty()
                        && triplesTextArea.getText().trim().isEmpty())
        ) {
            running = false;

            Alert alert = getInfoAlertDialogue(
                    "Empty polygon and/or code sequence - iteration pattern pairs",
                    "Please enter a valid polygon and and at least one code sequence - iteration pattern pair."
            );
            alert.showAndWait();

            return false;
        }

        String cleanedPolygonString = Polygon.cleanPolygon(this.polygonTextArea.getText());
        ConvexPolygon polygon = Polygon.createConvexPolygon(cleanedPolygonString);

        int limit;

        // Iteration can go on indefinitely, so the user must enter a limit
        if (limitTextField.getText().trim().isEmpty()) {
            running = false;

            Alert alert = getInfoAlertDialogue(
                    "Limit not provided",
                    "Iterations can go on indefinitely; Please enter a valid limit (a non-zero, positive integer)."
            );
            alert.showAndWait();

            return false;
        } else {
            try {
                limit = Integer.parseInt(limitTextField.getText());
            } catch (NumberFormatException e) {
                running = false;
                throw new RuntimeException(e);
            }

            if (limit <= 0) {
                running = false;

                Alert alert = getInfoAlertDialogue(
                        "Limit cannot be negative or zero",
                        "Limit must be a positive, non-zero integer."
                );
                alert.showAndWait();

                return false;
            }
        }

        String[][] codePatterns = getPatterns();

        ExecutorService executor = Executors.newFixedThreadPool(Utils.numThreads);
        ArrayList<Tuple3<ArrayList<Storage>, ArrayList<ArrayList<Storage>>, ArrayList<ArrayList<Storage>>>> results = new ArrayList<>();

        for (String[] codePattern : codePatterns) {
            if (codePattern[0].isEmpty() && codePattern.length == 1) continue;

            final MutableList<
                    Future<
                            Tuple3<ArrayList<Storage>, ArrayList<ArrayList<Storage>>, ArrayList<ArrayList<Storage>>>
                            >
                    > futures = new FastList<>();

            for (String pair : codePattern) {
                futures.add(executor.submit(() -> iterateTask(pair, polygon, limit)));
            }

            for (int i = 0; i < futures.size(); i++) {
                try {
                    Tuple3<ArrayList<Storage>, ArrayList<ArrayList<Storage>>, ArrayList<ArrayList<Storage>>> result = futures.get(i).get();
                    if (result != null) results.add(result);  // Only add results from tasks that ran to completion successfully
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println("An exception occurred for the code sequence - iteration pattern pair '"
                            + codePattern[i] + "':  " + e.getMessage());
                }
            }
        }

        executor.shutdown();

        // We are done. Set the results, and notify the observer.
        running = false;
        this.results = results;
        markFinishedIfPresent(this.finish);

        return true;
    }

    private String[][] getPatterns() {
        String[] stablePatterns = stablesTextArea.getText().trim().split("\n");
        String[] pmStablePatterns = pmStablesTextArea.getText().trim().split("\n");
        String[] unstablePatterns = unstablesTextArea.getText().trim().split("\n");
        String[] pmUnstablePatterns = pmUnstablesTextArea.getText().trim().split("\n");
        String[] triplePatterns = triplesTextArea.getText().trim().split("\n");
        return new String[][]{stablePatterns, pmStablePatterns, unstablePatterns, pmUnstablePatterns, triplePatterns};
    }

    /**
     * Returns an observable boolean property for an observer that will use the result of the iterate-to-limit task.
     * @return A SimpleBooleanProperty that will be set true once the iterate-to-limit task is finished.
     */
    public SimpleBooleanProperty execute() {
        stage.show();
        this.finish = new SimpleBooleanProperty(false);
        return this.finish;
    }

    public boolean getDraw() {
        return this.drawCheckbox.isSelected();
    }

    public boolean getAddToCover() {
        return this.coverCheckbox.isSelected();
    }

    public ArrayList<Tuple3<ArrayList<Storage>, ArrayList<ArrayList<Storage>>, ArrayList<ArrayList<Storage>>>> getResults() {
        return this.results;
    }

    public void nullifyFinish() {
        this.finish = null;
    }

    public void nullifyResult() {
        this.results = null;
    }

    public Stage getStage() {
        return this.stage;
    }

    public boolean isShowing() {
        return this.stage.isShowing();
    }

    public void toFront() {
        this.stage.toFront();
    }

    public void close() {
        // This window can be created as an AutoVary result sink without execute(),
        // so no finish observer necessarily exists during main-window shutdown.
        this.results = null;
        markFinishedIfPresent(this.finish);
        saveContentsToFile();
        this.stage.close();
    }

    static void markFinishedIfPresent(final SimpleBooleanProperty finish) {
        if (finish != null) {
            finish.set(true);
        }
    }

    private Alert getInfoAlertDialogue(String header, String content) {
        Text alertText = new Text(content);
        alertText.setWrappingWidth(350);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Iterate To Limit");
        alert.setHeaderText(header);
        alert.getDialogPane().setContent(alertText);
        alert.getDialogPane().setPadding(new Insets(10));
        alert.getDialogPane().setMaxWidth(400);
        return alert;
    }

    private void saveContentsToFile() {
        Utils.writeToFile(contentFileName,
                polygonTextArea.getText().trim() + "\n-----\n"
                        + stablesTextArea.getText().trim() + "\n-----\n"
                        + pmStablesTextArea.getText().trim() + "\n-----\n"
                        + unstablesTextArea.getText().trim() + "\n-----\n"
                        + pmUnstablesTextArea.getText().trim() + "\n-----\n"
                        + triplesTextArea.getText().trim() + "\n-----\n"
                        + limitTextField.getText().trim());
    }

    public void addToContent(String code, String pattern) {
        addToContent(code, pattern, true);
    }

    /**
     * <b>Jun 24, 2025</b>
     * <p>
     *     Adds the CODE sequence - iteration PATTERN pair to the appropriate text area. If the pair to add is a triple,
     *     then ALLPOSITIVE is ignored.
     * </p>
     * @param code The code sequence to add.
     * @param pattern The corresponding iteration pattern of CODE.
     * @param allPositive If true, add to the appropriate (stable vs. unstable) all-positive text area; else, add the plus/minus text area.
     */
    public void addToContent(String code, String pattern, boolean allPositive) {
        String[] codeComponents = code.trim().split(",");

        if (codeComponents.length == 3) {
            triplesTextArea.setText(
                    triplesTextArea.getText().trim() + "\n" +
                            code.trim() + " & " + pattern.trim()
            );
        }

        if (codeComponents.length == 1) {
            Optional<ImmutableIntList> codeNumbers = Utils.splitString(code.trim());

            if (codeNumbers.isPresent()) {
                Either<InvalidCodeSequence, ClassifiedCodeSequence> either = ClassifiedCodeSequence.create(codeNumbers.get());

                if (either.isRight()) {
                    ClassifiedCodeSequence codeSequence = either.get();
                    String coverCodeString = Utils.getCoverCodeString(codeSequence);
                    String stringToAdd = coverCodeString + " & " + pattern.trim();

                    final TextArea textArea;

                    if (codeSequence.stable) textArea = allPositive ? stablesTextArea : pmStablesTextArea;
                    else textArea = allPositive ? unstablesTextArea : pmUnstablesTextArea;

                    if (!textArea.getText().trim().isEmpty())
                        stringToAdd = textArea.getText().trim() + "\n" + stringToAdd;
                    textArea.setText(stringToAdd);
                }
            }
        }
    }

    private void addIterPatToGarbage(String iterationPattern, ArrayList<ClassifiedCodeSequence> codeSequences) {
        StringBuilder codeSequence = new StringBuilder(codeSequences.get(0).toString());
        StringBuilder oddEvenPattern = new StringBuilder(codeSequences.get(0).oddEvenPattern);

        for (int i = 1; i < codeSequences.size(); i++) {
            codeSequence.append(", ").append(codeSequences.get(i).toString());
            oddEvenPattern.append(",").append(codeSequences.get(i).oddEvenPattern);
        }

        Database.saveIterationPatternToDatabase(
                codeSequence.toString(),
                oddEvenPattern.toString(),
                iterationPattern,
                "garbage"
        );
    }

    /**
     * <p>Finds either an all-positive or plus/minus iteration pattern for CODESTRING.</p>
     * <b>Jun 18, 2025.</b>
     * <p>
     * The method used to find an iteration pattern for a code sequence is not backed by any theorem; this is simply a
     * pattern discovery. Based on the code numbers of a given code sequence, all numbers greater than 2 are grouped
     * based on their nearest largest digit place value.
     * </p>
     * <p>
     * For all-positive iteration patterns, a double precision floating point is calculated between the smallest number
     * of from largest number groups and the largest number from the smallest number group. Then we get the ratio by
     * rounding that floating point to the nearest integer:
     * </p>
     * <ul>
     * <li> If the ratio is 2, this ratio is how many times we add 2 to all numbers belonging to the largest number
     * group, while only adding 2 once to all other numbers greater than 2.
     * <li> If the ratio is not 2, this ratio is how many times we add 2 to all numbers belonging to the smallest number
     * group, while only adding 2 once to all other numbers greater than 2.
     * </ul>
     * <p>
     * For plus/minus patterns: add 2 once to all numbers belonging to the smallest or largest number group, and
     * subtract 2 from all other numbers greater 2.
     * </p>
     * @param codeString The code sequence to find an iteration pattern for.
     * @param allPositive If true, find an all-positive pattern; else, find a plus/minus pattern.
     * @return The iteration pattern as a string, or an empty string if no valid iteration pattern can be found.
     */
    public static String getIterationPattern(String codeString, boolean allPositive) {
        String[] codeSections = Utils.trimCodeLine(codeString).split(",");
        StringBuilder iterationPattern = new StringBuilder();
        int patternsCreated = 0;  // Keeps track of whether the number of iteration patterns found is the same as the number of code sections.

        for (int k = 0; k < codeSections.length; k++) {
            String code = codeSections[k].trim();

            Optional<ImmutableIntList> codeNumbers = Utils.splitString(code);

            if (!codeNumbers.isPresent()) continue;

            Map<Integer, List<Integer>> grouped = new TreeMap<>();
            int[] codeNumbersArray = codeNumbers.get().toArray();

            for (Integer codeNumber : codeNumbersArray) {
                if (codeNumber < 3) continue;

                int nearestLargestPlace = roundToLargestPlace(codeNumber);

                grouped.computeIfAbsent(nearestLargestPlace, k1 -> new ArrayList<>());
                grouped.get(nearestLargestPlace).add(codeNumber);
            }

            List<Integer> groupList = new ArrayList<>(grouped.keySet());  // Preserves ascending order of keys from the tree map...

            // We may fail to find number groups or very small code sequences (e.g., ones that only contain 1's and 2's)
            if (groupList.isEmpty()) {
                // System.out.println("Skipping " + codeString + " because its code numbers are too small to form number groups.");
                continue;
            }

            int minGroup = groupList.get(0);  // so the first element is the smallest...
            int maxGroup = groupList.get(groupList.size() - 1);  // and the last element is the largest

            if (allPositive) {  // All-positive iteration pattern
                int max = grouped.get(maxGroup).stream().min(Integer::compareTo).orElse(0);
                int min = grouped.get(minGroup).stream().max(Integer::compareTo).orElse(0);
                long ratio = Math.min(Math.round((double) max / min), 4);

                for (int i = 0; i < codeNumbersArray.length; i++) {
                    int codeNumber = codeNumbersArray[i];

                    if (codeNumber < 3) continue;

                    if (ratio == 2) {
                        if (roundToLargestPlace(codeNumber) == maxGroup)
                            for (int j = 0; j < ratio; j++) iterationPattern.append(i + 1).append(" ");
                        else iterationPattern.append(i + 1).append(" ");
                    } else {
                        if (roundToLargestPlace(codeNumber) == minGroup) iterationPattern.append(i + 1).append(" ");
                        else for (int j = 0; j < ratio; j++) iterationPattern.append(i + 1).append(" ");
                    }
                }
            } else {  // Plus-minus iteration pattern
                for (int i = 0; i < codeNumbersArray.length; i++) {
                    int codeNumber = codeNumbersArray[i];

                    if (codeNumber < 3) continue;

                    int group = roundToLargestPlace(codeNumber);
                    int index = group == minGroup || group == maxGroup ? i + 1 : -(i + 1);

                    iterationPattern.append(index).append(" ");
                }
            }

            if (k < codeSections.length - 1) iterationPattern.append(",");
            patternsCreated++;
        }

        return patternsCreated == codeSections.length ? iterationPattern.toString().trim() : "";
    }

    /**
     * <b>Jun 16, 2025.</b>
     * <p>
     *     Deterministically finds an iteration pattern for all the code sequences that does not already have one in all
     *     the text areas. For each code sequence, its line in the text area is unchanged if it already has an iteration
     *     pattern. Otherwise, the iteration pattern will be appended at the end of the code sequence.
     * </p>
     * <ul>
     *     <li>Code sequences in the all-positive text areas are given all-positive iteration patterns.</li>
     *     <li>Code sequences in the +/- text areas are given plus/minus iteration patterns.</li>
     *     <li>All components of a triple are given all-positive iteration patterns.</li>
     * </ul>
     */
    private void findIterationPattern() {
        TextArea[] textAreas = {stablesTextArea, unstablesTextArea, triplesTextArea, pmStablesTextArea, pmUnstablesTextArea};

        int textAreaIndex = 0;
        for (TextArea textArea : textAreas) {
            textAreaIndex++;
            String content = textArea.getText().trim();

            if (content.isEmpty()) continue;

            StringBuilder newContent = new StringBuilder();

            for (String line : content.split("\n")) {
                if (line.trim().isEmpty()) continue;

                String[] codeAndPattern = line.split("&");

                if (codeAndPattern.length > 1) {
                    newContent.append(line).append("\n");
                    continue;
                }

                newContent.append(line);

                final String iterationPattern = getIterationPattern(codeAndPattern[0], textAreaIndex <= 3);

                // Zhao Yu Li, Jun 24, 2025.
                // Post a warning for code sequences that we cannot find a valid iteration pattern for.
                if (iterationPattern.isEmpty()) {
                    System.out.println(
                            "WARNING: We cannot find a valid iteration pattern for " + codeAndPattern[0] + "."
                    );
                }
                else newContent.append(" & ").append(iterationPattern);

                newContent.append("\n");
            }

            textArea.setText(newContent.toString().trim());  // Trims the trailing newline character
            textArea.setScrollTop(Double.MAX_VALUE);  // Scroll to the bottom
        }
    }

    // ChatGPT, Jun 17, 2025.
    // Round to the nearest largest digit place.
    private static int roundToLargestPlace(int number) {
        if (number == 0) return 0;

        int abs = Math.abs(number);
        int magnitude = (int) Math.pow(10, (int) Math.log10(abs));
        return Math.round(number / (float) magnitude) * magnitude;
    }
}
