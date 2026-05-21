package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.InvalidCodeSequence;
import billiards.cover.CoverStuff;
import billiards.geometry.ConvexPolygon;
import billiards.wrapper.ConnectionPool;
import billiards.wrapper.Wrapper;
import com.google.common.base.Splitter;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.control.Either;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.IntList;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;

import static billiards.viewer.Utils.readFromFile;

public final class SmallCoverWindow {

    // ------------------------------------------------------------
    private static String polygonString = Utils.readFromFile(Viewer.tmpDir + "/small_cover_polygon.txt");
    static String stablesString = Utils.readFromFile(Viewer.tmpDir + "/small_cover_stables.txt");
    static String triplesString = Utils.readFromFile(Viewer.tmpDir + "/small_cover_triples.txt");
    static String digitsString = Utils.readFromFile(Viewer.tmpDir + "/small_cover_digits.txt");
    private static String emptyString = Utils.readFromFile(Viewer.tmpDir + "/small_cover_empty.txt");
    static String magnificationsString = Utils.readFromFile(Viewer.tmpDir + "/small_cover_magnifications.txt");
    //private static String halfTripleString = Utils.readFromFile(Viewer.tmpDir + "/cover_half_triples.txt");

    // WARNING: Global mutable state
    // Other classes which need to synchronize their polygon with the cover should listen to this property
    public static SimpleObjectProperty<String> polyStringProperty = new SimpleObjectProperty<>(polygonString);

    // ------------------------------------------------------------

    private final TextField digitsTextField = new TextField();
    final TextField emptyTextField = new TextField();
    final TextField magnificationsTextField = new TextField();
    private final RadioButton allRdoBtn = new RadioButton();
    private final RadioButton mrrRdoBtn = new RadioButton();
    private final CheckBox printInfoCb = new CheckBox();

    private final TextArea topText = new TextArea();
    private final TextArea bottomText = new TextArea();
    private final TextArea triplesText = new TextArea();
    //private final TextArea halfTriplesText = new TextArea();

    // Zhao Yu Li, Jul 29, 2025.
    // Added an option to render squares or not.
    // Updated Ju 30, 2025.
    // Removed this check box because the execution time of calculating the cover is not significantly impacted by the
    // rendering of the squares
    // private final CheckBox squaresCheckBox = new CheckBox("Squares");

    private final Stage stage = new Stage();

    public SmallCoverWindow(final String windowTitle, final ConnectionPool pool, final Runnable loadCover, final CoverWindow coverWindow, final ArrayList<ConvexPolygon> smallCoverAreas) {
        VBox base = new VBox();
        Scene scene = new Scene(base);
        stage.setScene(scene);
        stage.setTitle(windowTitle);

        stage.setOnCloseRequest(event -> saveToFile());

        base.setOnMouseExited(event -> saveToFile());

        topText.setText(polygonString);
        bottomText.setText(stablesString);
        triplesText.setText(triplesString);
        //halfTriplesText.setText(halfTripleString);

        digitsTextField.setPromptText("decimals");
        digitsTextField.setText(digitsString);
        digitsTextField.setPrefWidth(100);

        emptyTextField.setPromptText("empty squares");
        emptyTextField.setText(emptyString);

        if (emptyTextField.getText().isEmpty()) {
            emptyTextField.setText("100");
        }

        emptyTextField.setPrefWidth(100);

        magnificationsTextField.setPromptText("magnifications");
        magnificationsTextField.setText(magnificationsString);
        magnificationsTextField.setPrefWidth(100);

        // squaresCheckBox.setSelected(true);

        mrrRdoBtn.setSelected(true);
        //private final CheckBox diagonalCheckBox = new CheckBox();
        ToggleGroup calcGroup = new ToggleGroup();
        mrrRdoBtn.setToggleGroup(calcGroup);
        allRdoBtn.setToggleGroup(calcGroup);
        mrrRdoBtn.setText("MRR");
        allRdoBtn.setText("All");
        //diagonalCheckBox.setText("Diagonal");
        allRdoBtn.setOnAction(event -> {
            final Alert confirmation = new Alert(AlertType.CONFIRMATION);
            confirmation.setTitle("All Equations");
            confirmation.setHeaderText("All Equations");
            confirmation.setContentText("Are you sure you want all equations?");

            final Optional<ButtonType> response = confirmation.showAndWait();
            if (!(response.isPresent() && response.get() == ButtonType.OK)) {
                allRdoBtn.setSelected(false);
                mrrRdoBtn.setSelected(true);
            } else {
                allRdoBtn.setSelected(true);
            }
        });

        Button calcBtn = new Button();
        calcBtn.setText("Calculate");
        calcBtn.setOnAction(event -> {

            saveToFile();

            final int digits;
            final int magnifications;
            final int empty;
            try {
                digits = Integer.parseInt(digitsString);
                magnifications = Integer.parseInt(magnificationsString);
                empty = Integer.parseInt(emptyString);
            } catch (final NumberFormatException e) {
                throw new RuntimeException(e);
            }

            final boolean mrr;
            if (mrrRdoBtn.isSelected()) {
                mrr = true;
            } else if (allRdoBtn.isSelected()) {
                mrr = false;
            } else {
                throw new RuntimeException("all or mrr not selected");
            }

            // Ensure the cover folder exists
            final File cover = new File("small_cover");
            cover.mkdir();

            final StringBuilder newEmpties = new StringBuilder();

            if (mrr) {
                final ArrayList<ConvexPolygon> newSmallCoverAreas = new ArrayList<>();
                smallCoverAreas.clear();  // Clear old ones before adding new ones
                coverWindow.appendStablesInfo("// Small Cover");
                coverWindow.appendTriplesInfo("// Small Cover");
                String[] squares = polygonString.split("\n");

                for (int i = 0; i < squares.length; i++) {
                    String square = squares[i];
                    String[] coordinates = square.split(" ");

                    if (coordinates.length != 4) continue;

                    final String cleanedPolygon = coordinates[0] + " " + coordinates[1] + " " + coordinates[2] + " " + coordinates[3];
                    final String cleanedStablesPre = cleanStables(stablesString, pool);
                    final Tuple2<String, String> cleanedTriplesPre = cleanTriples(triplesString, pool);
                    final String cleanedTriples = cleanedTriplesPre._1;
                    final String cleanedStables = (cleanedStablesPre + '\n' + cleanedTriplesPre._2).trim();

                    String result = Wrapper.smallCoverWrapper(cleanedPolygon, cleanedStables, cleanedTriples, digits, magnifications, empty, true, pool, printInfoCb.isSelected());

                    if (!result.isEmpty()) {
                        String[] newCodes = result.split("-----");
                        if (newCodes.length > 0 && !newCodes[0].trim().isEmpty()) coverWindow.appendStablesInfo(newCodes[0].trim());
                        if (newCodes.length > 1 && !newCodes[1].trim().isEmpty()) coverWindow.appendTriplesInfo(newCodes[1].trim());
                        if (newCodes.length > 2 && !newCodes[2].trim().isEmpty()) newEmpties.append(newCodes[2].trim()).append("\n");

                        final String polygonString = readFromFile( "small_cover/polygon.txt").trim();
                        final ConvexPolygon polygon = CoverStuff.parsePolygon(polygonString);
                        newSmallCoverAreas.add(polygon);

                        // Zhao Yu Li, Aug 13, 2025.
                        // Add all new cover areas at the end, so all of them will be drawn once only.
                        if (i == squares.length - 1) smallCoverAreas.addAll(newSmallCoverAreas);

                        loadCover.run();
                    }
                }
            } else {

                final DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle("Choose an MRR Cover Directory");

                final File dir = chooser.showDialog(stage);

                if (dir == null) {
                    // User did not select a directory, so just cancel
                    return;
                }

                Wrapper.coverWrapperAll(dir.getPath(), pool, magnifications);
            }

            if (printInfoCb.isSelected()) System.out.println(windowTitle.replace("Cover", "BilliardViewer"));

            saveToFile();

            System.out.print(newEmpties);

            stage.close();
        });

        printInfoCb.setSelected(false);
        printInfoCb.setText("Print Info");

        HBox inputHBox = new HBox();
        inputHBox.getChildren().addAll(mrrRdoBtn, allRdoBtn, digitsTextField, magnificationsTextField,
                calcBtn, emptyTextField, printInfoCb);
        inputHBox.setSpacing(10);
        inputHBox.setAlignment(Pos.CENTER);

        topText.setPromptText("Squares, represented as lower x bound, upper x bound, lower y bound, upper x bound. Each number should be a fractional value of Pi. \nFor example, 0 1 0 1 covers then entire viewer.");
        topText.setPrefColumnCount(60);
        topText.setPrefRowCount(10);
        topText.setWrapText(true);
        topText.setEditable(true);
        topText.setFont(Font.font("Monaco", 16));
        topText.setFocusTraversable(false);
        topText.setText(polygonString);

        bottomText.setPromptText("stables");
        bottomText.setPrefColumnCount(60);
        bottomText.setPrefRowCount(10);
        bottomText.setWrapText(true);
        bottomText.setEditable(true);
        bottomText.setFont(Font.font("Monaco", 16));
        bottomText.setFocusTraversable(false);
        bottomText.setText(stablesString);

        triplesText.setPromptText("triples");
        triplesText.setPrefColumnCount(60);
        triplesText.setPrefRowCount(10);
        triplesText.setWrapText(true);
        triplesText.setEditable(true);
        triplesText.setFont(Font.font("Monaco", 16));
        triplesText.setFocusTraversable(false);
        triplesText.setText(triplesString);

        base.getChildren().addAll(topText, bottomText, triplesText, inputHBox);
        base.setSpacing(10);
        base.setPadding(new Insets(10));
    }

    public void saveToFile() {
        polygonString = topText.getText().trim();
        stablesString = bottomText.getText().trim();
        triplesString = triplesText.getText().trim();
        //halfTripleString = halfTriplesText.getText().trim();
        polyStringProperty.setValue(polygonString);

        digitsString = digitsTextField.getText().trim();
        magnificationsString = magnificationsTextField.getText().trim();
        emptyString = emptyTextField.getText().trim();

        Utils.writeToFile(Viewer.tmpDir + "/small_cover_polygon.txt", polygonString);
        Utils.writeToFile(Viewer.tmpDir + "/small_cover_stables.txt", stablesString);
        Utils.writeToFile(Viewer.tmpDir + "/small_cover_triples.txt", triplesString);
        Utils.writeToFile(Viewer.tmpDir + "/small_cover_digits.txt", digitsString);
        Utils.writeToFile(Viewer.tmpDir + "/small_cover_magnifications.txt", magnificationsString);
        Utils.writeToFile(Viewer.tmpDir + "/small_cover_empty.txt", emptyString);
        //Utils.writeToFile(Viewer.tmpDir + "/cover_half_triples.txt", halfTripleString);

    }

    public void appendStablesInfo(String stable) {
        String currentText = bottomText.getText();
        bottomText.setText(stable + "\n" + currentText);
    }

    static Tuple2<String, String> cleanTriples(final String string, final ConnectionPool pool) {

        final Iterable<String> lines = Splitter.onPattern("\\R")
                                           .trimResults()
                                           .omitEmptyStrings()
                                           .split(string);

        final StringBuilder triples = new StringBuilder();
        final StringBuilder stables = new StringBuilder();

        for (final String liney : lines) {

            if (liney.split("#")[0].trim().isEmpty()) continue;
            if (liney.split("//")[0].trim().isEmpty()) continue;

            final String line;
            if(liney.contains("-")) {
                line = liney.split("#")[0].split("//")[0].split("-")[1].trim(); // Triple may be of form X - XXXX,XXXX,XXXX
            } else {
                line = liney.split("#")[0].split("//")[0];
            }

            final String[] split = line.split(",");

            if (split.length != 3) {
                throw new RuntimeException("incorrect number of commas: " + line);
            }

            final Optional<ImmutableIntList> stableNegListOptional = Utils.splitString(split[0].trim());
            final Optional<ImmutableIntList> unstableListOptional = Utils.splitString(split[1].trim());
            final Optional<ImmutableIntList> stablePosListOptional = Utils.splitString(split[2].trim());

            if (!stableNegListOptional.isPresent()) throw new RuntimeException(split[0] + " contains invalid code numbers");
            if (!unstableListOptional.isPresent()) throw new RuntimeException(split[1] + " contains invalid code numbers");
            if (!stablePosListOptional.isPresent()) throw new RuntimeException(split[2] + " contains invalid code numbers");

            final IntList stableNegList = stableNegListOptional.get();
            final IntList unstableList = unstableListOptional.get();
            final IntList stablePosList = stablePosListOptional.get();

            final Either<InvalidCodeSequence, ClassifiedCodeSequence> stableNegEither = ClassifiedCodeSequence.create(stableNegList);
            final Either<InvalidCodeSequence, ClassifiedCodeSequence> unstableEither = ClassifiedCodeSequence.create(unstableList);
            final Either<InvalidCodeSequence, ClassifiedCodeSequence> stablePosEither = ClassifiedCodeSequence.create(stablePosList);

            if (stableNegEither.isLeft()) throw new RuntimeException(InvalidCodeSequence.errorMessage(stableNegList, stableNegEither.getLeft()));
            if (unstableEither.isLeft()) throw new RuntimeException(InvalidCodeSequence.errorMessage(unstableList, unstableEither.getLeft()));
            if (stablePosEither.isLeft()) throw new RuntimeException(InvalidCodeSequence.errorMessage(stablePosList, stablePosEither.getLeft()));

            final ClassifiedCodeSequence stableNeg = stableNegEither.get();
            final ClassifiedCodeSequence unstable = unstableEither.get();
            final ClassifiedCodeSequence stablePos = stablePosEither.get();

            if (!stableNeg.stable) {
                throw new RuntimeException(stableNeg + " is unstable");
            }

            if (unstable.stable) {
                throw new RuntimeException(unstable + " is stable");
            }

            if (!stablePos.stable) {
                throw new RuntimeException(stablePos + " is unstable");
            }

            if (!Wrapper.saveToDatabase(stableNeg, pool)) {
                throw new RuntimeException(stableNeg + " is empty");
            }

            if (!Wrapper.saveToDatabase(unstable, pool)) {
                throw new RuntimeException(unstable + " is empty");
            }

            if (!Wrapper.saveToDatabase(stablePos, pool)) {
                throw new RuntimeException(stablePos + " is empty");
            }
//july 31,2023 jaime made the triples not put it two ways in the cover start
	    final String tripleOneWay = String.format("%s,%s,%s\n", stableNeg, unstable, stablePos);
            final String tripleOtherWay = String.format("%s,%s,%s\n", stablePos, unstable, stableNeg);

            triples.append(tripleOneWay);
            triples.append(tripleOtherWay);
//july 31,2023 jaime made the triples not put it two ways in the cover end            
	    /*
            final String triple = String.format("%s,%s,%s\n", stableNeg, unstable, stablePos);

            triples.append(triple);
	    */
	    
            final String stable = String.format("%s\n%s\n", stableNeg, stablePos);

            stables.append(stable);
        }

        return Tuple.of(triples.toString().trim(), stables.toString().trim());
    }

    static String cleanStables(final String string, final ConnectionPool pool) {
        final Iterable<String> lines = Splitter.onPattern("\\R")
                                           .trimResults()
                                           .omitEmptyStrings()
                                           .split(string);

        final StringBuilder stables = new StringBuilder();

        for (final String line : lines) {
            if (line.split("#")[0].trim().isEmpty()) continue;
            if (line.split("//")[0].trim().isEmpty()) continue;

            final Optional<ImmutableIntList> optList = Utils.splitString(Utils.trimCodeLine(line));
            if (!optList.isPresent()) {
            	throw new RuntimeException(line + " is an invalid line");
            }
            final IntList list = optList.get();
            final Either<InvalidCodeSequence, ClassifiedCodeSequence> either
            								= ClassifiedCodeSequence.create(list);
            if (either.isLeft()) {
            	throw new RuntimeException(line + " is an invalid code sequence");
            }
            final ClassifiedCodeSequence codeSeq = either.get();
            if (!codeSeq.stable) {
                throw new RuntimeException("unstable code " + codeSeq + " from line " + line + " put in stables");
            }

            if (!Wrapper.saveToDatabase(codeSeq, pool)) {
                throw new RuntimeException(codeSeq + " is empty");
            }

            stables.append(codeSeq).append('\n');
        }

        return stables.toString().trim();
    }

    void show() {
        this.stage.show();
        this.stage.toFront();
    }

    void addPolygons(String newPolygons) {
        this.topText.setText(newPolygons.trim() + "\n" + topText.getText());
    }
}
