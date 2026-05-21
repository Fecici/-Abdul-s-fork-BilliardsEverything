package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.CodeType;
import billiards.codeseq.InvalidCodeSequence;
import billiards.wrapper.ConnectionPool;
import billiards.wrapper.Wrapper;

import com.google.common.base.Splitter;

import javafx.scene.control.*;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.control.Either;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.IntList;

import java.io.File;
import java.util.ArrayList;
import java.util.List; // added jul31,2025 marco
import java.util.Optional;//added oct 15,2017 george
import java.util.concurrent.ForkJoinPool;

import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;//added oct 15,2017 george
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import static billiards.utils.Polygon.cleanPolygon;

public final class CoverWindow {

    // ------------------------------------------------------------
    private static String polygonString = Utils.readFromFile(Viewer.tmpDir + "/cover_polygon.txt");
    static String stablesString = Utils.readFromFile(Viewer.tmpDir + "/cover_stables.txt");
    static String triplesString = Utils.readFromFile(Viewer.tmpDir + "/cover_triples.txt");
    static String digitsString = Utils.readFromFile(Viewer.tmpDir + "/cover_digits.txt");
    private static String emptyString = Utils.readFromFile(Viewer.tmpDir + "/cover_empty.txt");
    static String magnificationsString = Utils.readFromFile(Viewer.tmpDir + "/cover_magnifications.txt");
    //private static String halfTripleString = Utils.readFromFile(Viewer.tmpDir + "/cover_half_triples.txt");
 
    // WARNING: Global mutable state
    // Other classes which need to synchronize their polygon with the cover should listen to this property
    public static SimpleObjectProperty<String> polyStringProperty = new SimpleObjectProperty<>(polygonString);

    // ------------------------------------------------------------

    private final HBox inputHBox = new HBox();
    private final Button calcBtn = new Button();
    private final Button autoVaryBtn = new Button();
    private final TextField digitsTextField = new TextField();
    final TextField emptyTextField = new TextField();
    final TextField magnificationsTextField = new TextField();
    private final TextField labelTextField = new TextField();
    private final RadioButton allRdoBtn = new RadioButton();
    private final RadioButton mrrRdoBtn = new RadioButton();
    //private final CheckBox diagonalCheckBox = new CheckBox();
    private final ToggleGroup calcGroup = new ToggleGroup();

    private final VBox base = new VBox();
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
    private final CheckBox addToSmallCoverCB = new CheckBox("Add to Small Cover");

    private final Stage stage = new Stage();
    private final Scene scene = new Scene(base);

    public CoverWindow(final String windowTitle, final ConnectionPool pool,
    				   final TextField mainLabel, final Runnable loadCover, final Viewer viewer) {
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

        labelTextField.setPromptText("label");
        labelTextField.textProperty().bindBidirectional(mainLabel.textProperty());
        labelTextField.setPrefColumnCount(3);

        // squaresCheckBox.setSelected(true);
        addToSmallCoverCB.setSelected(false);

        mrrRdoBtn.setSelected(true);
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
            final File cover = new File("cover");
            cover.mkdir();

            if (mrr) {

                final String cleanedPolygon = cleanPolygon(polygonString);
                final String cleanedStablesPre = cleanStables(stablesString, pool);
                final Tuple2<String, String> cleanedTriplesPre = cleanTriples(triplesString, pool);
                //final Tuple2<String , String> cleanedHalfTriplePre = cleanHalfTriples(halfTripleString, pool);
                //ArrayLit<Tuple2<tring,String>> temp = cleanedHalfTriplePre._1.split("\n");
                /*if (!halfTripleString.isEmpty()) {
                    final String cleanedHalfTriples = cleanedHalfTriplePre._1;
                    final String cleanedStables = (cleanedStablesPre + '\n' + cleanedHalfTriplePre._2).trim();
                    //final String cleanedStables = cleanedStablesPre.trim();
                    // Stables in the half triple
                    //final String cleanedStableshalf = (cleanedHalfTriplePre._2).trim();
                    // Stables
                    //final String cleanedStables = (cleanedStablesPre).trim();
                    //final String[] cleanedStables_str = cleanedStables.split("\n");
                    //final String[] cleanedStablesHalf_str = cleanedStableshalf.split("\n");
                    //final String[] cleanedHalftriples_str = cleanedHalfTriples.split("\n");

                    if (diagonalCheckBox.isSelected()) {
                        Wrapper.coverWrapperDiagonal(cleanedPolygon, cleanedStables, cleanedHalfTriples, digits, magnifications, empty, mrr, pool);
                    }
                    else {
                        Wrapper.coverWrapperHalf(cleanedPolygon, cleanedStables, cleanedHalfTriples, digits, magnifications, empty, mrr, pool);
                    }
                }*/
                //else {
                final String cleanedTriples = cleanedTriplesPre._1;
                final String cleanedStables = (cleanedStablesPre + '\n' + cleanedTriplesPre._2).trim();
//                System.out.println(cleanedStables);

                String res = Wrapper.coverWrapper(cleanedPolygon, cleanedStables, cleanedTriples, digits, magnifications, empty, mrr, pool);
                if (viewer.smallCoverWindow != null && addToSmallCoverCB.isSelected()) viewer.smallCoverWindow.addPolygons(res);
                //}
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
/*
        inputHBox.getChildren().addAll(mrrRdoBtn, allRdoBtn, digitsTextField, magnificationsTextField,
                                       calcBtn, emptyTextField, labelTextField, diagonalCheckBox);
*/
        inputHBox.getChildren().addAll(mrrRdoBtn, allRdoBtn, digitsTextField, magnificationsTextField,
                calcBtn, emptyTextField, labelTextField, addToSmallCoverCB);
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

/*
        halfTriplesText.setPromptText("half the triples");
        halfTriplesText.setPrefColumnCount(60);
        halfTriplesText.setPrefRowCount(10);
        halfTriplesText.setWrapText(true);
        halfTriplesText.setEditable(true);
        halfTriplesText.setFont(Font.font("Monaco", 16));
        halfTriplesText.setFocusTraversable(false);
        halfTriplesText.setText(halfTripleString);
*/
        //base.getChildren().addAll(topText, bottomText, triplesText,halfTriplesText, inputHBox);
        base.getChildren().addAll(topText, bottomText, triplesText, inputHBox);
        base.setSpacing(10);
        base.setPadding(new Insets(10));
    }
    
    // Allows other classes to update the list of triples
    public void setTriplesInfo(ArrayList<String> triples){
        String currentText = triplesText.getText().toString();
        //Filling triple string
        for (int i = triples.size() - 1; i >= 0; --i) {
            currentText = triples.get(i) + "\n" + currentText;
        }
        triplesText.setText(currentText);
    }

    // Allows other classes to update the list of stables
    public void appendTriplesInfo(String triple) {
        String currentText = triplesText.getText().toString();
        triplesText.setText(triple + "\n" + currentText);
    }
    
    // Allows other classes to update the list of stables 
    public void appendStablesInfo(String stable) {
        String currentText = bottomText.getText().toString();
        bottomText.setText(stable + "\n" + currentText);
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

        Utils.writeToFile(Viewer.tmpDir + "/cover_polygon.txt", polygonString);
        Utils.writeToFile(Viewer.tmpDir + "/cover_stables.txt", stablesString);
        Utils.writeToFile(Viewer.tmpDir + "/cover_triples.txt", triplesString);
        Utils.writeToFile(Viewer.tmpDir + "/cover_digits.txt", digitsString);
        Utils.writeToFile(Viewer.tmpDir + "/cover_magnifications.txt", magnificationsString);
        Utils.writeToFile(Viewer.tmpDir + "/cover_empty.txt", emptyString);
        //Utils.writeToFile(Viewer.tmpDir + "/cover_half_triples.txt", halfTripleString);

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

  /* 2025,jul,31
   * This function is updated to calcualte new code parallel at the same time
   */
   public static String cleanStables(final String string, final ConnectionPool pool) {

        final Iterable<String> lines = Splitter.onPattern("\\R")
                                           .trimResults()
                                           .omitEmptyStrings()
                                           .split(string);

        final StringBuilder stables = new StringBuilder();
        // This list holds only the valid, classified code sequences (not just lines!)
        final List<ClassifiedCodeSequence> validSequences_fast = new ArrayList<>();
        final List<ClassifiedCodeSequence> validSequences_slow = new ArrayList<>();
        
        // reference data 
        //OSNO (114, 1142) 1.7GB
        //OSNO (194, 1894) 7GB
        // OSNO (236, 2238) 19.3GB
        //CS   (526, 4860) 1.8GB
        //CS   (762, 7900) 5.7GB

        // default limit(guessing a system has 8GB memory)
        int threadholdOSNO = 3000;
        int threadholdCS = 6000;
        //detect system memory
        try{
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();       // Max memory JVM will attempt to use
            // OperatingSystemMXBean osBean = (OperatingSystemMXBean)
            // ManagementFactory.getOperatingSystemMXBean();
            // long totalBytes3 = osBean.getTotalPhysicalMemorySize();
            int totalGB = (int) (maxMemory /(1073741824.0)); // Convert to GB
            // System.out.println("JVM System Memory: " + totalGB + " GB");
            threadholdOSNO = 500+totalGB/4*50;
            threadholdCS = 1500+totalGB/4*300;
            // threadholdOSNO
        } catch(Exception e1){
            System.out.println("using default task limit");
        }


        // First loop: Filter, parse, and classify
        // and determain which code are small and can be compute at same time
        for (final String line : lines) {
            if (line.split("#")[0].trim().isEmpty()) continue;
            if (line.split("//")[0].trim().isEmpty()) continue;

            final Optional<ImmutableIntList> optList = Utils.splitString(Utils.trimCodeLine(line));
            if (!optList.isPresent()) {
                throw new RuntimeException(line + " is an invalid line");
            }
            final IntList list = optList.get();
            final Either<InvalidCodeSequence, ClassifiedCodeSequence> either = ClassifiedCodeSequence.create(list);
            if (either.isLeft()) {
                throw new RuntimeException(line + " is an invalid code sequence");
            }

            // determine which code are fast compuation
            // threadhold decide by computer performence
            final ClassifiedCodeSequence codeSeq = either.get();
            if ((codeSeq.codeType == CodeType.OSNO || codeSeq.codeType == CodeType.OSO)  &  codeSeq.codeSum <threadholdOSNO){
                validSequences_fast.add(codeSeq);
            }else if (codeSeq.codeType == CodeType.CS &codeSeq.codeSum < threadholdCS){
                validSequences_fast.add(codeSeq);
            } else {
                validSequences_slow.add(codeSeq);
            }                
        }
  
        // Make sure Utils.numThreads is set to less than total of thread, 
        // otherwise not all thread are able to connect the database 
        ForkJoinPool customPool = new ForkJoinPool(Utils.numThreads);
        try {
            customPool.submit(() ->
                validSequences_fast.parallelStream().forEach(codeSeq -> {
                    if (!codeSeq.stable) {
                        throw new RuntimeException("unstable code " + codeSeq + " put in stables");
                    }
                    if (!Wrapper.saveToDatabase(codeSeq, pool)) {
                        throw new RuntimeException(codeSeq + " is empty");
                    }
                    synchronized (stables) {
                        stables.append(codeSeq).append('\n');
                    }
                })
            ).get();
        } catch (Exception e) {
            throw new RuntimeException("Parallel processing failed", e);
        } finally {
            customPool.shutdown();
        }

        // Second loop: Only handle large stable, valid code sequences
        for (final ClassifiedCodeSequence codeSeq : validSequences_slow) {
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

    /*
    public boolean getRenderSquares() {
        return squaresCheckBox.isSelected();
    }
    */
}
