package billiards.viewer;

import billiards.wrapper.ConnectionPool;

import javaslang.collection.Array;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// Input checking and sanitation
// Always trim text input, and then
// check if it is
// - empty
// - non empty and invalid. If so, show an error alert and
// cancel

public final class QueryStage {
    private final Label leftRightLabel = new Label();
    private final TextField leftRightTextField = new TextField();
    private final HBox leftRightHBox = new HBox();

    private final Label codeSumLabel = new Label();
    private final TextField codeSumTextField = new TextField();
    private final HBox codeSumHBox = new HBox();

    private final Label evenOddLabel = new Label();
    private final TextField evenOddTextField = new TextField();
    private final HBox evenOddHBox = new HBox();

    private final CheckBox osoCheckBox = new CheckBox();
    private final CheckBox osnoCheckBox = new CheckBox();
    private final CheckBox onsCheckBox = new CheckBox();
    private final CheckBox csCheckBox = new CheckBox();
    private final CheckBox cnsCheckBox = new CheckBox();

    private final Array<CheckBox> stableCheckBoxes =
        Array.of(osoCheckBox, osnoCheckBox, csCheckBox);
    private final Array<CheckBox> unstableCheckBoxes = Array.of(onsCheckBox, cnsCheckBox);

    private final HBox codeTypeHBox = new HBox();

    private final Button queryButton = new Button();
    private final VBox root = new VBox();

    private final Scene scene = new Scene(root);
    private final Stage stage = new Stage();

    public QueryStage(final String stageTitle, final ConnectionPool pool, final Viewer viewer) {
        leftRightLabel.setText("Left Right Code Sequence");
        leftRightHBox.getChildren().addAll(leftRightLabel, leftRightTextField);
        leftRightHBox.setSpacing(10);

        codeSumLabel.setText("Max Code Sequence Sum:");

        codeSumHBox.getChildren().addAll(codeSumLabel, codeSumTextField);
        codeSumHBox.setSpacing(10);

        evenOddLabel.setText("Even Odd Pattern:");

        evenOddHBox.getChildren().addAll(evenOddLabel, evenOddTextField);
        evenOddHBox.setSpacing(10);

        osoCheckBox.setText("OSO");
        osnoCheckBox.setText("OSNO");
        onsCheckBox.setText("ONS");
        csCheckBox.setText("CS");
        cnsCheckBox.setText("CNS");

        codeTypeHBox.getChildren().addAll(
            osoCheckBox, osnoCheckBox, onsCheckBox, csCheckBox, cnsCheckBox);
        codeTypeHBox.setSpacing(10);

        queryButton.setText("Search");
        queryButton.setOnAction(event -> query(pool, viewer));

        root.getChildren().addAll(
            leftRightHBox, codeSumHBox, evenOddHBox, codeTypeHBox, queryButton);
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        stage.setScene(scene);
        stage.setTitle(stageTitle);
        stage.setOnCloseRequest(event -> stage.close());
    }

    // There is a lot of mutable state going on
    // That is how all this is done
    private void query(final ConnectionPool pool, final Viewer viewer) {
        throw new RuntimeException("searching is currently disabled");
        /*
        // Always trim text input
        final String leftRightString = leftRightTextField.getText().trim();
        final Optional<ImmutableIntList> codeNumbersOpt = Utils.splitString(leftRightString);

        if (!codeNumbersOpt.isPresent()) {
            // The user entered some input that couldn't be parsed into an integer

            final Alert alert = new Alert(AlertType.ERROR);

            alert.setTitle("Invalid Input");
            alert.setHeaderText("Invalid Input");
            alert.setContentText(String.format("Input %s is invalid.", leftRightString));
            alert.showAndWait();
            return;
        }

        final ImmutableIntList codeNumbers = codeNumbersOpt.get();

        final ArrayList<Storage> storages = new ArrayList<>();

        if (codeNumbers.isEmpty()) {
            // Didn't enter any code numbers, so just use everything else
            final String codeSumString = codeSumTextField.getText().trim();

            final Optional<Long> codeSumOpt;
            if (codeSumString.isEmpty()) {
                // no restriction on the code sum
                codeSumOpt = Optional.empty();
            } else {
                try {
                    final long codeSum = Long.parseLong(codeSumString);
                    codeSumOpt = Optional.of(codeSum);
                } catch (final NumberFormatException e) {
                    final Alert error = new Alert(AlertType.ERROR);
                    error.setTitle("Invalid Code Sum");
                    error.setHeaderText("Invalid Code Sum");
                    error.setContentText(String.format("%s is not a number.", codeSumString));

                    error.showAndWait();
                    // Let the user put in a different input and try again
                    return;
                }
            }

            final String evenOddString = evenOddTextField.getText().trim();

            final Optional<String> evenOddOpt;
            if (evenOddString.isEmpty()) {
                // There are no even odd restrictions
                evenOddOpt = Optional.empty();
            } else {
                // Do a verification that the even odd
                // is done properly
                final boolean valid = verifyEvenOdd(evenOddString);
                if (valid) {
                    evenOddOpt = Optional.of(evenOddString);
                } else {
                    final Alert error = new Alert(AlertType.ERROR);
                    error.setTitle("Invalid Even Odd");
                    error.setHeaderText("Invalid Even Odd");
                    error.setContentText(
                        String.format("%s is invalid even-odd sequence.", evenOddString));

                    error.showAndWait();
                    // Let the user put in a different input and try again
                    return;
                }
            }

            final boolean codeSumPresent = codeSumOpt.isPresent();
            final boolean evenOddPresent = evenOddOpt.isPresent();

            // That's what I'm talking about
            final Array<String> stableTypes = stableCheckBoxes.filter(CheckBox::isSelected)
                                                  .map(CheckBox::getText)
                                                  .map(String::toLowerCase);

            final Array<String> unstableTypes = unstableCheckBoxes.filter(CheckBox::isSelected)
                                                    .map(CheckBox::getText)
                                                    .map(String::toLowerCase);

            if (codeSumPresent && evenOddPresent) {
                final long codeSum = codeSumOpt.get();
                final String evenOdd = evenOddOpt.get();

                stableTypes.forEach(codeType -> {
                    final ArrayList<Storage> stable = Search.searchStableCodeSumEvenOdd(
                        codeSum, evenOdd, pool, codeType);
                    storages.addAll(stable);
                });

                unstableTypes.forEach(codeType -> {
                    final ArrayList<Storage> unstable = Search.searchUnstableCodeSumEvenOdd(
                        codeSum, evenOdd, pool, codeType);
                    storages.addAll(unstable);
                });

            } else if (!codeSumPresent && evenOddPresent) {
                final String evenOdd = evenOddOpt.get();

                stableTypes.forEach(codeType -> {
                    final ArrayList<Storage> stable =
                        Search.searchStableEvenOdd(evenOdd, pool, codeType);
                    storages.addAll(stable);
                });

                unstableTypes.forEach(codeType -> {
                    final ArrayList<Storage> unstable =
                        Search.searchUnstableEvenOdd(evenOdd, pool, codeType);
                    storages.addAll(unstable);
                });

            } else if (codeSumPresent && !evenOddPresent) {
                final long codeSum = codeSumOpt.get();

                stableTypes.forEach(codeType -> {
                    final ArrayList<Storage> stable =
                        Search.searchStableCodeSum(codeSum, pool, codeType);
                    storages.addAll(stable);
                });

                unstableTypes.forEach(codeType -> {
                    final ArrayList<Storage> unstable =
                        Search.searchUnstableCodeSum(codeSum, pool, codeType);
                    storages.addAll(unstable);
                });

            } else if (!codeSumPresent && !evenOddPresent) {
                stableTypes.forEach(codeType -> {
                    final ArrayList<Storage> stable = Search.searchStable(pool, codeType);
                    storages.addAll(stable);
                });

                unstableTypes.forEach(codeType -> {
                    final ArrayList<Storage> unstable = Search.searchUnstable(pool, codeType);
                    storages.addAll(unstable);
                });

            } else {
                throw new RuntimeException("unknown options in query");
            }

        } else {
            // Did enter some numbers, so we need to check them
            final Either<InvalidCodeSequence, ClassifiedCodeSequence> either =
                ClassifiedCodeSequence.create(codeNumbers);
            if (either.isLeft()) {
                final InvalidCodeSequence errorCode = either.getLeft();

                final Alert alert = new Alert(AlertType.ERROR);

                alert.setTitle("Invalid Code Sequence");
                alert.setHeaderText("Invalid Code Sequence");
                alert.setContentText(InvalidCodeSequence.errorMessage(codeNumbers, errorCode));
                alert.showAndWait();
                return;
            } else {
                final ClassifiedCodeSequence codeSeq = either.get();

                // Check if the code sequence is in the database. If not,
                // show an error message and stop
                final boolean exists = Database.exists(codeSeq, pool);

                if (!exists) {
                    final Alert alert = new Alert(AlertType.ERROR);

                    alert.setTitle("Code Sequence Not In Database");
                    alert.setHeaderText("Code Sequence Not In Database");
                    alert.setContentText(String.format(
                        "Code sequence %s is not in the database.", codeSeq.toString()));
                    alert.showAndWait();
                    return;
                }

                final String codeType = codeSeq.codeType.toString().toLowerCase();

                if (codeSeq.stable) {
                    // stable
                    final LeftRight[] leftRights =
                        Search.loadStableLeftRight(codeSeq, pool);

                    final ArrayList<Storage> stable =
                        Search.searchStableLeftRight(leftRights, pool, codeType);

                    storages.addAll(stable);
                } else {
                    // unstable
                    final Tuple2<LeftRight, LeftRight> leftRights =
                        Search.loadUnstableLeftRight(codeSeq, pool);

                    final ArrayList<Storage> unstable =
                        Search.searchUnstableLeftRight(leftRights, pool, codeType);
                    storages.addAll(unstable);
                }
            }
        }

        storages.forEach(System.out::println);
        Utils.printToFile("search.txt", storages);
        viewer.drawSearch(storages);
        */
    }

    public void show() {
        this.stage.show();
    }

    private static boolean verifyEvenOdd(final String evenOdd) {
        for (int i = 0; i < evenOdd.length(); ++i) {
            final char ch = evenOdd.charAt(i);
            if (ch != 'E' && ch != 'O') {
                return false;
            }
        }

        return true;
    }
}
