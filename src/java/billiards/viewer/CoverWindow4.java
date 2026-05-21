package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.InvalidCodeSequence;
import billiards.utils.Polygon;
import billiards.wrapper.ConnectionPool;
import billiards.wrapper.Wrapper;
import com.google.common.base.Splitter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

import static billiards.utils.Polygon.cleanPolygon;

public class CoverWindow4 {

    // WARNING: Global mutable state
    // ------------------------------------------------------------
    private static String polygonString = Utils.readFromFile(Viewer.tmpDir + "/cover_polygon.txt");
    //private static String stablesString = Utils.readFromFile(Viewer.tmpDir + "/cover_stables.txt");
    //private static String triplesString = Utils.readFromFile(Viewer.tmpDir + "/cover_triples.txt");
    //private static String unstableString = Utils.readFromFile(Viewer.tmpDir + "/cover_unstables.txt");
    private static String digitsString = Utils.readFromFile(Viewer.tmpDir + "/cover_digits.txt");
    private static String emptyString = Utils.readFromFile(Viewer.tmpDir + "/cover_empty.txt");
    private static String magnificationsString = Utils.readFromFile(Viewer.tmpDir + "/cover_magnifications.txt");
    private static String halfTripleString = Utils.readFromFile(Viewer.tmpDir + "/cover_half_triples.txt");

    // ------------------------------------------------------------

    private final HBox inputHBox = new HBox();
    private final Button calcBtn = new Button();
    private final Button autoVaryBtn = new Button();
    private final TextField digitsTextField = new TextField();
    private final TextField emptyTextField = new TextField();
    private final TextField magnificationsTextField = new TextField();
    private final TextField labelTextField = new TextField();
    private final RadioButton allRdoBtn = new RadioButton();
    private final RadioButton mrrRdoBtn = new RadioButton();
    //private final CheckBox diagonalCheckBox = new CheckBox();
    private final ToggleGroup calcGroup = new ToggleGroup();
    //private final RadioButton unstableRdoBtn = new RadioButton();
    //private final RadioButton halfTripleRdoBtn = new RadioButton();
    //private final RadioButton cornerRdoBtn = new RadioButton();
    //private final ToggleGroup inputGroup = new ToggleGroup();


    private final VBox base = new VBox();
    private final TextArea topText = new TextArea();
    //private final TextArea bottomText = new TextArea();
    //private final TextArea triplesText = new TextArea();
    private final TextArea halfTriplesText = new TextArea();


    private final Stage stage = new Stage();
    private final Scene scene = new Scene(base);

    public CoverWindow4(final String windowTitle, final ConnectionPool pool,
                        final TextField mainLabel, final Runnable loadCover) {
        stage.setScene(scene);
        stage.setTitle(windowTitle);

        base.setOnMouseExited(event -> saveToFile());

        topText.setText(polygonString);
        //bottomText.setText(unstableString);
        //triplesText.setText(triplesString);
        halfTriplesText.setText(halfTripleString);

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

        labelTextField.setPromptText("label");
        labelTextField.textProperty().bindBidirectional(mainLabel.textProperty());

        mrrRdoBtn.setSelected(true);
        mrrRdoBtn.setToggleGroup(calcGroup);
        allRdoBtn.setToggleGroup(calcGroup);
        mrrRdoBtn.setText("MRR");
        allRdoBtn.setText("All");
        //diagonalCheckBox.setText("Diagonal");
/*      halfTripleRdoBtn.setText("half triple");
        halfTripleRdoBtn.setSelected(true);
        halfTripleRdoBtn.setToggleGroup(inputGroup);
        unstableRdoBtn.setText("unstable");
        unstableRdoBtn.setToggleGroup(inputGroup);
        cornerRdoBtn.setText("corner");
        cornerRdoBtn.setToggleGroup(inputGroup);*/
        allRdoBtn.setOnAction(event -> {
            final Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
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
            final File cover = new File("cover2");
            cover.mkdir();

            if (mrr) {
                final String cleanedPolygon = cleanPolygon(polygonString);
                //final String cleanedStablesPre = cleanStables(stablesString, pool);
                //final Tuple2<String, String> cleanedTriplesPre = cleanTriples(triplesString, pool);
                final Tuple2<String , String> cleanedHalfTriplePre = cleanHalfTriples(halfTripleString, pool);
                //ArrayLit<Tuple2<tring,String>> temp = cleanedHalfTriplePre._1.split("\n");
                //if (!halfTripleString.isEmpty()) {
                final String cleanedHalfTriples = cleanedHalfTriplePre._1;
                final String cleanedStables = (cleanedHalfTriplePre._2).trim();
                //final String cleanedStables = cleanedStablesPre.trim();
                // Stables in the half triple
                //final String cleanedStableshalf = (cleanedHalfTriplePre._2).trim();
                // Stables
                //final String cleanedStables = (cleanedStablesPre).trim();
                //final String[] cleanedStables_str = cleanedStables.split("\n");
                //final String[] cleanedStablesHalf_str = cleanedStableshalf.split("\n");
                //final String[] cleanedHalftriples_str = cleanedHalfTriples.split("\n");

                /*if (cornerRdoBtn.isSelected()) {
                    final String cleanedPolygon = cleanPolygon(polygonString);
                    //final String cleanedStablesPre = cleanStables(stablesString, pool);
                    //final Tuple2<String, String> cleanedTriplesPre = cleanTriples(triplesString, pool);
                    final Tuple2<String , String> cleanedHalfTriplePre = cleanHalfTriples(halfTripleString, pool);
                    //ArrayLit<Tuple2<tring,String>> temp = cleanedHalfTriplePre._1.split("\n");
                    //if (!halfTripleString.isEmpty()) {
                    final String cleanedHalfTriples = cleanedHalfTriplePre._1;
                    final String cleanedStables = (cleanedHalfTriplePre._2).trim();
                    Wrapper.coverWrapperDiagonal(cleanedPolygon, cleanedStables, cleanedHalfTriples, digits, magnifications, empty, mrr, pool);
                }
                else if (halfTripleRdoBtn.isSelected()){
                    final String cleanedPolygon = cleanPolygon(polygonString);
                    //final String cleanedStablesPre = cleanStables(stablesString, pool);
                    //final Tuple2<String, String> cleanedTriplesPre = cleanTriples(triplesString, pool);
                    final Tuple2<String , String> cleanedHalfTriplePre = cleanHalfTriples(halfTripleString, pool);
                    //ArrayLit<Tuple2<tring,String>> temp = cleanedHalfTriplePre._1.split("\n");
                    //if (!halfTripleString.isEmpty()) {
                    final String cleanedHalfTriples = cleanedHalfTriplePre._1;
                    final String cleanedStables = (cleanedHalfTriplePre._2).trim();
                    Wrapper.coverWrapperHalf(cleanedPolygon, cleanedStables, cleanedHalfTriples, digits, magnifications, empty, mrr, pool);
                }
                else if (unstableRdoBtn.isSelected()) {
                    final String cleanedPolygon = cleanPolygon(polygonString);
                    //final String cleanedStablesPre = cleanStables(stablesString, pool);
                    //final Tuple2<String, String> cleanedTriplesPre = cleanTriples(triplesString, pool);
                    final Tuple2<String , String> cleanedHalfTriplePre = cleanHalfTriples("1 1 1, " + halfTripleString, pool);
                    //ArrayLit<Tuple2<tring,String>> temp = cleanedHalfTriplePre._1.split("\n");
                    //if (!halfTripleString.isEmpty()) {
                    final String cleanedHalfTriples = cleanedHalfTriplePre._1;
                    //final String cleanedStables = (cleanedHalfTriplePre._2).trim();
                    final String cleanedStables = "";
                    Wrapper.coverWrapperUnstable(cleanedPolygon, cleanedStables, cleanedHalfTriples, digits, magnifications, empty, mrr, pool);
                }
                }
                else {
                    final String cleanedTriples = cleanedTriplesPre._1;
                    final String cleanedStables = (cleanedStablesPre + '\n' + cleanedTriplesPre._2).trim();


                    Wrapper.coverWrapper(cleanedPolygon, cleanedStables, cleanedTriples, digits, magnifications, empty, mrr, pool);
                }*/
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

            loadCover.run();

            System.out.println(windowTitle.replace("Cover", "BilliardViewer"));

            saveToFile();
            redoInfo();

            stage.close();
        });

        autoVaryBtn.setText("AutoVary");
        autoVaryBtn.setOnAction(event -> {
            //final AutoVary vary = new AutoVary(0, 10, 10, ConvexPolygon.create(new FastList().toImmutable()));
            //final MutableList<ClassifiedCodeSequence> autoCode = vary.fireaway();
        });

        //inputHBox.getChildren().addAll(mrrRdoBtn, allRdoBtn, digitsTextField, magnificationsTextField,
        //        calcBtn, emptyTextField, labelTextField, diagonalCheckBox);
        inputHBox.getChildren().addAll(mrrRdoBtn, allRdoBtn, digitsTextField, magnificationsTextField,
                calcBtn, emptyTextField, labelTextField);
        inputHBox.setSpacing(10);
        inputHBox.setAlignment(Pos.CENTER);

        topText.setPromptText("polygon");
        topText.setPrefColumnCount(60);
        topText.setPrefRowCount(10);
        topText.setWrapText(true);
        topText.setEditable(true);
        topText.setFont(Font.font("Monaco", 16));
        topText.setFocusTraversable(false);
        topText.setText(polygonString);
/*
        bottomText.setPromptText("unstables");
        bottomText.setPrefColumnCount(60);
        bottomText.setPrefRowCount(10);
        bottomText.setWrapText(true);
        bottomText.setEditable(true);
        bottomText.setFont(Font.font("Monaco", 16));
        bottomText.setFocusTraversable(false);
        bottomText.setText(unstableString);
*/
        halfTriplesText.setPromptText("corner");
        halfTriplesText.setPrefColumnCount(60);
        halfTriplesText.setPrefRowCount(10);
        halfTriplesText.setWrapText(true);
        halfTriplesText.setEditable(true);
        halfTriplesText.setFont(Font.font("Monaco", 16));
        halfTriplesText.setFocusTraversable(false);
        halfTriplesText.setText(halfTripleString);

        //base.getChildren().addAll(topText, bottomText, halfTriplesText, inputHBox);
        base.getChildren().addAll(topText, halfTriplesText, inputHBox);
        base.setSpacing(10);
        base.setPadding(new Insets(10));
    }

    private void saveToFile() {

        polygonString = topText.getText().trim();
        //unstableString = bottomText.getText().trim();
        halfTripleString = halfTriplesText.getText().trim();

        digitsString = digitsTextField.getText().trim();
        magnificationsString = magnificationsTextField.getText().trim();
        emptyString = emptyTextField.getText().trim();

        Utils.writeToFile(Viewer.tmpDir + "/cover_polygon.txt", polygonString);
        //Utils.writeToFile(Viewer.tmpDir + "/cover_unstables.txt", unstableString);
        //Utils.writeToFile(Viewer.tmpDir + "/cover_triples.txt", triplesString);
        Utils.writeToFile(Viewer.tmpDir + "/cover_digits.txt", digitsString);
        Utils.writeToFile(Viewer.tmpDir + "/cover_magnifications.txt", magnificationsString);
        Utils.writeToFile(Viewer.tmpDir + "/cover_empty.txt", emptyString);
        Utils.writeToFile(Viewer.tmpDir + "/cover_half_triples.txt", halfTripleString);

    }

    private static Tuple2<String, String> cleanTriples(final String string, final ConnectionPool pool) {

        final Iterable<String> lines = Splitter.onPattern("\\R")
                .trimResults()
                .omitEmptyStrings()
                .split(string);

        final StringBuilder triples = new StringBuilder();
        final StringBuilder stables = new StringBuilder();

        for (final String liney : lines) {

            if (liney.split("#")[0].trim().isEmpty()) continue;
            if (liney.split("//")[0].trim().isEmpty()) continue;

            final String line = liney.split("#")[0].split("//")[0];

            final String[] split = line.split(",");

            if (split.length != 3) {
                throw new RuntimeException("incorrect number of commas: " + line);
            }

            final IntList stableNegList = Utils.splitString(split[0].trim()).get();
            final IntList unstableList = Utils.splitString(split[1].trim()).get();
            final IntList stablePosList = Utils.splitString(split[2].trim()).get();

            final ClassifiedCodeSequence stableNeg = ClassifiedCodeSequence.create(stableNegList).get();
            final ClassifiedCodeSequence unstable = ClassifiedCodeSequence.create(unstableList).get();
            final ClassifiedCodeSequence stablePos = ClassifiedCodeSequence.create(stablePosList).get();

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

            final String triple = String.format("%s,%s,%s\n", stableNeg, unstable, stablePos);

            triples.append(triple);

            final String stable = String.format("%s\n%s\n", stableNeg, stablePos);

            stables.append(stable);
        }

        return Tuple.of(triples.toString().trim(), stables.toString().trim());
    }
    private static Tuple2<String, String> cleanHalfTriples(final String string, final ConnectionPool pool) {
        ArrayList<String> unstables= new ArrayList<>();
        final Iterable<String> lines = Splitter.onPattern("\\R")
                .trimResults()
                .omitEmptyStrings()
                .split(string);

        final StringBuilder triples = new StringBuilder();
        final StringBuilder stables = new StringBuilder();

        for (final String liney : lines) {

            if (liney.split("#")[0].trim().isEmpty()) continue;
            if (liney.split("//")[0].trim().isEmpty()) continue;

            final String line = liney.split("#")[0].split("//")[0];

            final String[] split = line.split(",");

            if (split.length != 2) {
                if (split.length == 3) {
                    final IntList stableNegList = Utils.splitString(split[0].trim()).get();
                    final IntList unstableList = Utils.splitString(split[1].trim()).get();
                    final IntList unstableList2 = Utils.splitString(split[2].trim()).get();


                    final ClassifiedCodeSequence stableNeg = ClassifiedCodeSequence.create(stableNegList).get();
                    final ClassifiedCodeSequence unstable = ClassifiedCodeSequence.create(unstableList).get();
                    final ClassifiedCodeSequence unstable2 = ClassifiedCodeSequence.create(unstableList2).get();


                    if (!stableNeg.stable) {
                        throw new RuntimeException(stableNeg + " is unstable");
                    }

                    if (unstable.stable) {
                        throw new RuntimeException(unstable + " is stable");
                    }

                    if (unstable2.stable) {
                        throw new RuntimeException(unstable + " is stable");
                    }

                    if (!Wrapper.saveToDatabase(stableNeg, pool)) {
                        throw new RuntimeException(stableNeg + " is empty");
                    }

                    if (!Wrapper.saveToDatabase(unstable, pool)) {
                        throw new RuntimeException(unstable + " is empty");
                    }

                    if (!Wrapper.saveToDatabase(unstable2, pool)) {
                        throw new RuntimeException(unstable + " is empty");
                    }


                    final String triple = String.format("%s,%s,%s\n", stableNeg, unstable, unstable2);

                    triples.append(triple);

                    final String stable = String.format("%s\n", stableNeg);

                    stables.append(stable);

                    return Tuple.of(triples.toString().trim(), stables.toString().trim());


                }

                throw new RuntimeException("incorrect number of commas: " + line);


            }

            final IntList stableNegList = Utils.splitString(split[0].trim()).get();
            final IntList unstableList = Utils.splitString(split[1].trim()).get();

            final ClassifiedCodeSequence stableNeg = ClassifiedCodeSequence.create(stableNegList).get();
            final ClassifiedCodeSequence unstable = ClassifiedCodeSequence.create(unstableList).get();

            if (!stableNeg.stable) {
                throw new RuntimeException(stableNeg + " is unstable");
            }

            if (unstable.stable) {
                throw new RuntimeException(unstable + " is stable");
            }

            if (!Wrapper.saveToDatabase(stableNeg, pool)) {
                throw new RuntimeException(stableNeg + " is empty");
            }

            if (!Wrapper.saveToDatabase(unstable, pool)) {
                throw new RuntimeException(unstable + " is empty");
            }


            final String triple = String.format("%s,%s\n", stableNeg, unstable);

            triples.append(triple);

            final String stable = String.format("%s\n", stableNeg);

            stables.append(stable);
        }

        return Tuple.of(triples.toString().trim(), stables.toString().trim());
    }
    private static Tuple2<String, String> cleanHalfTriplesCorner(final String string, final ConnectionPool pool) {
        ArrayList<String> unstables= new ArrayList<>();
        final Iterable<String> lines = Splitter.onPattern("\\R")
                .trimResults()
                .omitEmptyStrings()
                .split(string);

        final StringBuilder triples = new StringBuilder();
        final StringBuilder stables = new StringBuilder();

        for (final String liney : lines) {

            if (liney.split("#")[0].trim().isEmpty()) continue;
            if (liney.split("//")[0].trim().isEmpty()) continue;

            final String line = liney.split("#")[0].split("//")[0];

            final String[] split = line.split(",");

            if (split.length != 3) {

                throw new RuntimeException("incorrect number of commas: " + line);

            }

            final IntList stableNegList = Utils.splitString(split[0].trim()).get();
            final IntList unstableList = Utils.splitString(split[1].trim()).get();
            final IntList unstableList2 = Utils.splitString(split[2].trim()).get();


            final ClassifiedCodeSequence stableNeg = ClassifiedCodeSequence.create(stableNegList).get();
            final ClassifiedCodeSequence unstable = ClassifiedCodeSequence.create(unstableList).get();
            final ClassifiedCodeSequence unstable2 = ClassifiedCodeSequence.create(unstableList).get();


            if (!stableNeg.stable) {
                throw new RuntimeException(stableNeg + " is unstable");
            }

            if (unstable.stable) {
                throw new RuntimeException(unstable + " is stable");
            }

            if (unstable2.stable) {
                throw new RuntimeException(unstable + " is stable");
            }

            if (!Wrapper.saveToDatabase(stableNeg, pool)) {
                throw new RuntimeException(stableNeg + " is empty");
            }

            if (!Wrapper.saveToDatabase(unstable, pool)) {
                throw new RuntimeException(unstable + " is empty");
            }

            if (!Wrapper.saveToDatabase(unstable2, pool)) {
                throw new RuntimeException(unstable + " is empty");
            }


            final String triple = String.format("%s,%s,%s\n", stableNeg, unstable, unstable2);

            triples.append(triple);

            final String stable = String.format("%s\n", stableNeg);

            stables.append(stable);
        }

        return Tuple.of(triples.toString().trim(), stables.toString().trim());
    }

    private static String cleanStables(final String string, final ConnectionPool pool) {

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
                throw new RuntimeException("unstable code " + codeSeq + " put in stables");
            }

            if (!Wrapper.saveToDatabase(codeSeq, pool)) {
                throw new RuntimeException(codeSeq + " is empty");
            }

            stables.append(codeSeq).append('\n');
        }

        return stables.toString().trim();
    }

    /* compare stables.txt to the backup stables file which has all the comments
    private static void stableCommentPairings() {
        final String info = Utils.readFromFile("cover/stables.txt");
        final String preInfo = Utils.readFromFile(Viewer.tmpDir + "/cover_stables.txt");
        final StringBuilder postInfo = new StringBuilder();

        for (final String line : info.split("\\r?\\n")) {
            if (line.startsWith("//")) postInfo.append(line);
            else if (line.trim().isEmpty()) continue;

            else {
                final String code = line.split(",")[0].split(":")[1];
                for (final String check : preInfo.split("\\r?\\n")) {
                    final String split = Utils.trimCodeLine(check);
                    if (split.equals(code.trim())) {
                        postInfo.append(code.trim());
                        final String[] patty = check.split("#");

                        if (patty.length > 1) postInfo.append(" # " + patty[1].trim() + "\n");
                        else postInfo.append("\n");
                        break;
                    }
                }
            }
        }
        Utils.writeToFile("stablecoverpats.txt", postInfo.toString());
    }

    private static void tripleCommentPairings() {
        final String info = Utils.readFromFile("cover/triples.txt");
        final String preInfo = Utils.readFromFile(Viewer.tmpDir + "/cover_triples.txt");
        final StringBuilder postInfo = new StringBuilder();

        for (final String line : info.split("\\r?\\n")) {
            if (line.startsWith("//")) postInfo.append(line);
            else if (line.trim().isEmpty()) continue;

            else {

                final String codewithcomma = line.split(":")[1].replace("; ", "")
                          .replace("x", "").replace("y", "").replace("z", "");
                final String code = (codewithcomma + "~").replace(", ~", "").trim();
                for (final String check : preInfo.split("\\r?\\n")) {
                    final String split = check.split("//")[0].split("#")[0].trim();

                    if (split.equals(code.trim())) {
                        postInfo.append(code.trim());
                        final String[] patty = check.split("#");

                        if (patty.length == 2) postInfo.append(" # " + patty[1].trim() + "\n");
                        else postInfo.append("\n");
                        break;
                    }
                }
            }
        }
        Utils.writeToFile("triplecoverpats.txt", postInfo.toString());
    }
    */

    private static void redoInfo() {
        final String info = Utils.readFromFile("cover/info.txt");
        final String preInfo = Utils.readFromFile(Viewer.tmpDir + "/cover_triples.txt") + "\n" +
                Utils.readFromFile(Viewer.tmpDir + "/cover_stables.txt");
        final StringBuilder postInfo = new StringBuilder();

        for (final String line : info.split("\\r?\\n")) {
            postInfo.append(line.trim());
            if (line.startsWith("//") || line.trim().isEmpty()) {
                postInfo.append("\n");
                continue;
            }

            String code = line;

            if (code.contains(" - ")) {
                if (code.startsWith("-")) code = code.split(" - ")[2];
                else code = code.split(" - ")[1];
            }

            for (String preLine : preInfo.split("\\r?\\n")) {
                if (Utils.tripleTrimmer(preLine.split("#")[0].trim()).equals(code)) {

                    if (preLine.contains("#")) postInfo.append(" # " + preLine.split("#")[1].trim());

                    break;
                }
            }
            postInfo.append("\n");
        }

        Utils.writeToFile("cover/info.txt", postInfo.toString());
    }

    void show() {
        this.stage.show();
        this.stage.toFront();
    }
}
