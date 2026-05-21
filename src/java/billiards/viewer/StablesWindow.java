package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.wrapper.ConnectionPool;
import billiards.wrapper.Wrapper;
import com.google.common.base.Splitter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javaslang.Tuple;
import javaslang.Tuple2;
import org.eclipse.collections.api.list.primitive.IntList;

import javax.sound.midi.SysexMessage;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class StablesWindow {

    private static String topStablesString = Utils.readFromFile(Viewer.tmpDir + "/stables_topStables.txt");

    private static String unStablesString = Utils.readFromFile(Viewer.tmpDir + "/stables_unstables.txt");

    private static String bottomStablesString = Utils.readFromFile(Viewer.tmpDir + "/stables_bottomStables.txt");

    private static String fullTriplesString = Utils.readFromFile(Viewer.tmpDir + "/triples_full.txt");
    private ConnectionPool pool = null;
    private final Color buttonShowColor = Color.MAGENTA;
    private final Color buttonClickColor = Color.GOLD;
    private final Font labelFont = new Font("Arial", 20);//Font for labels and text prompts
    private final Stage   stage = new Stage();

    private final CheckBox showFactorCheckBox = new CheckBox();

    private final VBox generalLayoutBox = new VBox();//For the overall vertical layout of the window
    private final HBox inputHBox = new HBox();//For the Buttons at the bottom

    private final TextArea topStablesText = new TextArea();
    private final TextArea middleUnstablesText = new TextArea();
    private final TextArea bottomStablesText = new TextArea();
    private final TextArea fullTriplesText = new TextArea();
    private final TextField numberOfPairsTextField = new TextField();

    private final Label topStablesLabel = new Label("Stables");
    private final Label middleUnstablesLabel = new Label("Unstables");
    private final Label bottomStablesLabel = new Label("Stables");
    private final Label fullTriplesLabel = new Label("Full Triples Or Half Triples");
    private final Button calculateButton = new Button();
//    private final Button calculateHalfTriplesButton = new Button();
    private final Button getTriplesButton = new Button();
    private final Scene scene = new Scene(generalLayoutBox);

    private final CoverWindow coverWindow;



    public StablesWindow(CoverWindow coverWindow){
        this.coverWindow = coverWindow;
        //Setting the stage and title
        stage.setScene(scene);
        stage.setTitle("Stables Window");

        //Setting the three text boxes
        topStablesText.setPromptText("Stables");
        middleUnstablesText.setPromptText("Unstables");
        bottomStablesText.setPromptText("Stables");
        fullTriplesText.setPromptText("Full Triples");

        //Setting the text of the boxes
        topStablesText.setText(topStablesString);
        middleUnstablesText.setText(unStablesString);
        bottomStablesText.setText(bottomStablesString);
        fullTriplesText.setText(fullTriplesString);

        //Setting the number of pairs textfield
        numberOfPairsTextField.setTooltip(Utils.toolTip("Sets the number of pairs to send to the covers window, leave empty for all"));
        numberOfPairsTextField.setPrefWidth(40);

        //Setting the two button properties and lambdas
        calculateButton.setText("Calculate");
        calculateButton.setTooltip(Utils.toolTip("Calculates based on the selected parameters"));
        Utils.colorButton(calculateButton, buttonShowColor, buttonClickColor);
        calculateButton.setOnAction(event -> {calculateAction();});//Action for calculate triples button

        //Setting up show factors checkbox
        showFactorCheckBox.setText("Find Factors");
        topStablesLabel.disableProperty().bind((showFactorCheckBox.selectedProperty()));
        topStablesText.disableProperty().bind((showFactorCheckBox.selectedProperty()));
        middleUnstablesLabel.disableProperty().bind((showFactorCheckBox.selectedProperty()));
        middleUnstablesText.disableProperty().bind((showFactorCheckBox.selectedProperty()));
        bottomStablesLabel.disableProperty().bind((showFactorCheckBox.selectedProperty()));
        bottomStablesText.disableProperty().bind(showFactorCheckBox.selectedProperty());
        getTriplesButton.disableProperty().bind((showFactorCheckBox.selectedProperty()));
        numberOfPairsTextField.disableProperty().bind((showFactorCheckBox.selectedProperty()));

        fullTriplesLabel.disableProperty().bind((showFactorCheckBox.selectedProperty().not()));
        fullTriplesText.disableProperty().bind((showFactorCheckBox.selectedProperty().not()));

        getTriplesButton.setText("Print Triple Combinations");
        getTriplesButton.setTooltip(Utils.toolTip("Gets Triple Combinations From Given Stables and Unstables"));
        Utils.colorButton(getTriplesButton, buttonShowColor, buttonClickColor);
        getTriplesButton.setOnAction(event -> {getTriplesAction();});//Action for the triples button

        //Setting font for labels
        topStablesLabel.setFont(labelFont);
        middleUnstablesLabel.setFont(labelFont);
        bottomStablesLabel.setFont(labelFont);
        fullTriplesLabel.setFont(labelFont);

        topStablesText.setFont(labelFont);
        middleUnstablesText.setFont(labelFont);
        bottomStablesText.setFont(labelFont);
        fullTriplesText.setFont(labelFont);

        //Setting the inputHBox properties
        inputHBox.getChildren().addAll(showFactorCheckBox, getTriplesButton, calculateButton, numberOfPairsTextField);
        inputHBox.setSpacing(30);
        inputHBox.setAlignment(Pos.CENTER);

        //Setting the general layout properties
        generalLayoutBox.getChildren().addAll(topStablesLabel, topStablesText, middleUnstablesLabel,
                middleUnstablesText, bottomStablesLabel, bottomStablesText, fullTriplesLabel, fullTriplesText, inputHBox);
        generalLayoutBox.setSpacing(10);
        generalLayoutBox.setPadding(new Insets(10));
        generalLayoutBox.setOnMouseExited(event -> saveToFile());//Saves to file when the window is closed
    }

    public void setConnectionPool(final ConnectionPool pool){
        this.pool = pool;
    }

    //Action for calculate triples button
    private void calculateAction(){
        boolean showFactor = showFactorCheckBox.isSelected();

        if (!showFactor){
            if (bottomStablesText.getText().equals("")){// if no second stable is entered then assume it's a half triple
                calculateHalfTriplesAction();
            } else {
                calculateTriplesAction();
            }
        } else if (showFactor){
            for (String item:fullTriplesText.getText().split("\n")) {
                if (item.split(",").length == 3){
                    showFactorTriple(item.trim());
                } else {
                    showFactorHalfTriple(item.trim());
                }
            }
        }
    }

    private void calculateTriplesAction(){
        //Action if triple is entered
        ArrayList<String> topStables, unStables, bottomStables, workingTriples, stablesForWorkingTriples;
        topStables = new ArrayList<>();
        unStables = new ArrayList<>();
        bottomStables = new ArrayList<>();
        workingTriples = new ArrayList<>();
        stablesForWorkingTriples = new ArrayList<>();

        //Setting fake variables needed for calculations
        final String cleanedPolygon = "0 0\n0 90\n90 0";
        final int digits = 0;
        final int magnifications = 0;
        final int empty = 0;
        final boolean mrr = true;
        int equations = 0;

        //Getting the text as elements split by commas
        topStables.addAll(Arrays.asList(topStablesText.getText().trim().split("\n")));
        unStables.addAll(Arrays.asList(middleUnstablesText.getText().trim().split("\n")));
        bottomStables.addAll(Arrays.asList(bottomStablesText.getText().trim().split("\n")));

        //Trimming to remove additional info at the start
        topStables = trimArrayList(topStables);
        unStables = trimArrayList(unStables);
        bottomStables = trimArrayList(bottomStables);

        ArrayList<String> combinations = getCombinations(topStables, unStables, bottomStables);//Gets all triple combinations

        //Doing the calculations to get the equations
        for (int i = 0; i < combinations.size(); i++) {
            if (checkTriples(combinations.get(i), pool) == 0){
                String stables = getStables(combinations.get(i));
                equations = Wrapper.coverWrapperDuplicateStables(cleanedPolygon, stables, combinations.get(i), digits, magnifications, empty, mrr, pool, false);
                if (equations == 2){
                    workingTriples.add(combinations.get(i));
                }
            }
        }

        //Printing the working triples(if any)
        if (workingTriples.size() == 0){
            System.out.println("No Triple Combinations produce two equations");
        } else { //Are equations wanted at any point if they are being sent directly to covers
            System.out.println("---Working Triples Below---");
            for (String triple : workingTriples) {
                stablesForWorkingTriples.add(getStables(triple));
                System.out.println(triple);
            }
            System.out.println("---End Of Working Triples---");
            int numberOfPairs = 0;
            boolean all = false;
            try {
                numberOfPairs = Integer.parseInt(numberOfPairsTextField.getText().trim());
                if (numberOfPairs * 2 > workingTriples.size()){
                    all = true;
                }
            } catch(Exception e){
                all = true;
            }
            if (all){
                coverWindow.setTriplesInfo(workingTriples);
                coverWindow.show();
            } else {
                ArrayList<String> triplesToSend = new ArrayList<>();
                for (int i = 0; i < numberOfPairs * 2; i++) {
                    triplesToSend.add(workingTriples.get(i));
                }
                coverWindow.setTriplesInfo(triplesToSend);
                coverWindow.show();
            }

        }
    }

    private void calculateHalfTriplesAction(){
        final String cleanedPolygon = "0 0\n0 90\n90 0";
        final int digits = 0;
        final int magnifications = 0;
        final int empty = 0;
        final boolean mrr = true;

        ArrayList<String> workingHalfTriples = new ArrayList<>();
        ArrayList<String> combinations = new ArrayList<>();
        ArrayList<String> stables = new ArrayList<>();
        ArrayList<String> unstables = new ArrayList<>();

        stables.addAll(Arrays.asList(topStablesText.getText().split("\n")));
        unstables.addAll(Arrays.asList(middleUnstablesText.getText().split("\n")));

        stables = trimArrayList(stables);
        unstables = trimArrayList(unstables);

        //Creating Combinations
        for (String stable : stables) {
            for (String unstable : unstables) {
                combinations.add(stable + ", " + unstable);
            }
        }

        for (String halfTripleString : combinations) {
            final Tuple2<String , String> cleanedHalfTriplePre = cleanHalfTriples(halfTripleString, pool);
            final String cleanedHalfTriples = cleanedHalfTriplePre._1;
            final String cleanedStables = (cleanedHalfTriplePre._2).trim();
            if (Wrapper.coverWrapperHalfDuplicateStables(cleanedPolygon, cleanedStables, cleanedHalfTriples, digits, magnifications, empty, mrr, pool)){
                workingHalfTriples.add(halfTripleString);
            }
        }

        if (workingHalfTriples.size() == 0){
            System.out.println("No Working Half Triples");
        } else {
            System.out.println("--------Working Half Triples--------");
            for (String working : workingHalfTriples) {
                System.out.println(working);
            }
            System.out.println("--------End Of Working Half Triples--------");
        }

    }

    private void showFactorTriple(String triple){
        //Setting fake variables needed for calculations
        final String cleanedPolygon = "0 0\n0 90\n90 0";
        final int digits = 0;
        final int magnifications = 0;
        final int empty = 0;
        final boolean mrr = true;

        System.out.println("--------");

        final Tuple2<String, String> cleanedTriplesPre = cleanTriples(triple, pool);
        final String cleanedTriples = cleanedTriplesPre._1;

        String stables = getStables(triple);
        Wrapper.coverWrapperDuplicateStables(cleanedPolygon, stables, cleanedTriples, digits, magnifications, empty, mrr, pool, true);

        System.out.println("--------");
    }

    private Tuple2<String, String> cleanTriples(final String string, final ConnectionPool pool) {

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


            final String tripleOneWay = String.format("%s,%s,%s\n", stableNeg, unstable, stablePos);

            triples.append(tripleOneWay);

            final String stable = String.format("%s\n%s\n", stableNeg, stablePos);

            stables.append(stable);
        }

        return Tuple.of(triples.toString().trim(), stables.toString().trim());
    }

    private void showFactorHalfTriple(String item){
        //Action if half triple is selected
        //Fake variables
        final String cleanedPolygon = "0 0\n0 90\n90 0";
        final int digits = 0;
        final int magnifications = 28;
        final int empty = 0;
        final boolean mrr = true;

        ArrayList<String> stables = new ArrayList<>();
        ArrayList<String> unstables = new ArrayList<>();

        stables.add(item.split(",")[0]);
        unstables.add(item.split(",")[1]);

        //Trimming to remove additional info at the start
        stables = trimArrayList(stables);
        unstables = trimArrayList(unstables);

        for (String stable: stables) {
            for (String unstable: unstables) {
                System.out.println("--------\nStable: " + stable + "\nUnstable: " + unstable);
                String halfTriplesText = stable + ", " + unstable;
                try {
                    final Tuple2<String , String> cleanedHalfTriplePre = cleanHalfTriples(halfTriplesText, pool);
                    final String cleanedHalfTriples = cleanedHalfTriplePre._1;
                    final String cleanedStables = (cleanedHalfTriplePre._2).trim();
                    Wrapper.coverWrapperHalfDuplicateStables(cleanedPolygon, cleanedStables, cleanedHalfTriples, digits, magnifications, empty, mrr, pool);
                    System.out.println("--------");
                } catch(Exception e){
                    System.out.println("Half Triple Doesn't Work\n--------");
                }
            }
        }
    }

    //Trims not needed information from stables and unstable strings
    public ArrayList<String> trimArrayList(ArrayList<String> stringList){
        ArrayList<String> toReturn = new ArrayList<>();
        for (String element:stringList) {
            toReturn.add(Utils.tripleTrimmer(element.trim()));
        }
        return toReturn;
    }

    //Function to build all combinations of stables and return them in an arrayList of Strings
    private ArrayList<String> getCombinations(ArrayList<String> topStables, ArrayList<String> unStables, ArrayList<String> bottomStables){
        ArrayList<String> combinations = new ArrayList<>();
        for (String unstable: unStables) {
            for (String topStable: topStables) {
                for (String bottomStable:bottomStables) {
                    combinations.add(topStable + "," + unstable + "," + bottomStable);
                    //combinations.add(bottomStable + "," + unstable + "," + topStable);
                    //jaime july31,2023 makes the triple print once not twice by commenting out the above line
                }
            }
        }

        return combinations;
    }

    //Function to get the combinations of stables needed for coverWrapper function
    private String getStables(String triple){
        String stables = triple.split(",")[0] + "\n" + triple.split(",")[2];
        return stables;
    }

    private static int checkTriples(final String string, final ConnectionPool pool) {

        final Iterable<String> lines = Splitter.onPattern("\\R")
                .trimResults()
                .omitEmptyStrings()
                .split(string);

        for (final String liney : lines) {

            if (liney.split("#")[0].trim().isEmpty()) continue;
            if (liney.split("//")[0].trim().isEmpty()) continue;

            final String line = liney.split("#")[0].split("//")[0];

            final String[] split = line.split(",");

            if (split.length != 3) {
                System.out.println("Triple Doesn't Work: Incorrect Number of commas" + line);//Decision, do you still want this error message
                return -1;
            }

            final IntList stableNegList = Utils.splitString(split[0].trim()).get();
            final IntList unstableList = Utils.splitString(split[1].trim()).get();
            final IntList stablePosList = Utils.splitString(split[2].trim()).get();

            final ClassifiedCodeSequence stableNeg = ClassifiedCodeSequence.create(stableNegList).get();
            final ClassifiedCodeSequence unstable = ClassifiedCodeSequence.create(unstableList).get();
            final ClassifiedCodeSequence stablePos = ClassifiedCodeSequence.create(stablePosList).get();

            if (!stableNeg.stable) {
                System.out.println("Triple Doesn't Work: " + stableNeg + "is unstable");
                return -1;
            }

            if (unstable.stable) {
                System.out.println("Triple Doesn't Work: " + unstable + "is stable");
                return -1;
            }

            if (!stablePos.stable) {
                System.out.println("Triple Doesn't Work: " + stablePos + " is unstable");
                return -1;
            }

            if (!Wrapper.saveToDatabase(stableNeg, pool)) {
                System.out.println("Triple Doesn't Work: " + stableNeg + " is empty");
                return -1;
            }

            if (!Wrapper.saveToDatabase(unstable, pool)) {
                System.out.println(unstable + " is empty");
                return -1;
            }

            if (!Wrapper.saveToDatabase(stablePos, pool)) {
                System.out.println("Triple Doesn't Work: " + stablePos + " is empty");
                return -1;
            }
        }
        return 0; //Returns 0 if no error was encountered
    }

    private void getTriplesAction(){
        ArrayList<String> topStables, unStables, bottomStables;
        topStables = new ArrayList<>();
        unStables = new ArrayList<>();
        bottomStables = new ArrayList<>();

        //Getting the text as elements split by commas
        topStables.addAll(Arrays.asList(topStablesText.getText().toString().trim().split("\n")));
        unStables.addAll(Arrays.asList(middleUnstablesText.getText().toString().trim().split("\n")));
        bottomStables.addAll(Arrays.asList(bottomStablesText.getText().toString().trim().split("\n")));

        topStables = trimArrayList(topStables);
        unStables = trimArrayList(unStables);
        bottomStables = trimArrayList(bottomStables);

        //Getting the triples from the top stables, unstables, and bottom stables
        ArrayList<String> triples = getCombinations(topStables, unStables, bottomStables);

        //Printing all combinations
        for (int i = 0; i < triples.size(); i++) {
            System.out.println("Triple " + (i + 1) + ": " + triples.get(i));
        }
    }

    private void saveToFile() {

        topStablesString = topStablesText.getText().trim();
        unStablesString = middleUnstablesText.getText().trim();
        bottomStablesString = bottomStablesText.getText().trim();
        fullTriplesString = fullTriplesText.getText().trim();

        Utils.writeToFile(Viewer.tmpDir + "/stables_topStables.txt", topStablesString);
        Utils.writeToFile(Viewer.tmpDir + "/stables_unstables.txt", unStablesString);
        Utils.writeToFile(Viewer.tmpDir + "/stables_bottomStables.txt", bottomStablesString);
        Utils.writeToFile(Viewer.tmpDir + "/triples_full.txt", fullTriplesString);
    }

    //Show method that shows the new screen
    public void show() {
        this.stage.show();
        this.stage.toFront();
    }

    private static Tuple2<String, String> cleanHalfTriples(final String string, final ConnectionPool pool) {
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
}
