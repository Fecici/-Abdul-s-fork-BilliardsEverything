package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.CodeSequence;
import billiards.codeseq.InvalidCodeSequence;
import billiards.database.Database;
import billiards.database.Info;
import billiards.database.LeftRight;
import billiards.geometry.Vector2;
import billiards.math.Equation;
import billiards.math.LinCom;
import billiards.math.XYEta;
import billiards.math.XYZ;
import billiards.wrapper.ConnectionPool;
import billiards.wrapper.Wrapper;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javaslang.Tuple2;
import javaslang.control.Either;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class GradientWindow {
    private final TextArea resultArea = new TextArea();
    private final Button calculateButton = new Button();
    private final ToggleGroup optionGroup = new ToggleGroup();
    private final RadioButton infoButton = new RadioButton();
    private final RadioButton databaseButton = new RadioButton();
    private final VBox root = new VBox();
    private final Stage stage = new Stage();
    private final Scene scene = new Scene(root);
    private final HBox inputHBox = new HBox();
    private final HBox actionHBox = new HBox();
    private final TextField equationField = new TextField();
    private final TextField xField = new TextField();
    private final TextField yField = new TextField();

    // TODO put infos in a list so we can close them all after
    public GradientWindow(final String windowTitle) {
        stage.setScene(scene);
        stage.setTitle(windowTitle);
        stage.setOnCloseRequest(event -> stage.close());

        resultArea.setPrefColumnCount(60);
        resultArea.setPrefRowCount(20);
        resultArea.setWrapText(true);
        resultArea.setEditable(false);
        resultArea.setFont(Font.font("Monaco", 16));
        // We want the text to expand as we make the window bigger
        VBox.setVgrow(resultArea, Priority.ALWAYS);

        inputHBox.getChildren().addAll(equationField, xField, yField);
        inputHBox.setSpacing(10);
        equationField.setPromptText("Equation");
        xField.setPromptText("x (in degrees)");
        yField.setPromptText("y (in degrees)");

        actionHBox.getChildren().addAll(databaseButton, infoButton, calculateButton);
        actionHBox.setSpacing(10);
        calculateButton.setText("Calculate");
        calculateButton.setOnAction(event -> showResult());
        databaseButton.setText("database (e.g.[cos 1 1 1])");
        databaseButton.setTooltip(Utils.toolTip("the input is in the format of [cos 1 1 1]"));
        databaseButton.setSelected(true);
        databaseButton.setToggleGroup(optionGroup);
        infoButton.setText("info (e.g.[cos(x+y)])");
        infoButton.setTooltip(Utils.toolTip("the input is in the format of [cos(x+y)]"));
        infoButton.setToggleGroup(optionGroup);

        root.getChildren().addAll(inputHBox, actionHBox, resultArea);
        root.setSpacing(10);
        root.setPadding(new Insets(10));
    }

    public void close() {
        stage.close();
    }

    public void show() {
        stage.show();
    }

    private void showResult() {
        try {
            String equation_str = equationField.getText();
            String x_str = xField.getText().trim();
            String y_str = yField.getText().trim();
            if (databaseButton.isSelected()) {
                resultArea.setText(Wrapper.calculateGradient(equation_str, x_str, y_str, true));
            } else {
                resultArea.setText(Wrapper.calculateGradient(equation_str, x_str, y_str, false));
            }
        } catch (Exception exception) {
            System.out.println(exception.toString());
        }
        /*
        final String codeNumsString = codeNumbersTextField.getText();

        if (codeNumsString.isEmpty()) {
            final Alert alert = new Alert(Alert.AlertType.ERROR);

            alert.setTitle("Enter a Code Sequence");
            alert.setHeaderText("Enter a Code Sequence");
            alert.setContentText("Please enter a code sequence.");
            alert.showAndWait();
            return;
        }

        final Optional<ImmutableIntList> optional = Utils.splitString(codeNumsString);

        if (!optional.isPresent()) {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Invalid Input");
            alert.setContentText(String.format("Input %s is invalid.", codeNumsString));

            alert.showAndWait();
            return;
        }

        final ImmutableIntList codeNums = optional.get();

        final Either<InvalidCodeSequence, ClassifiedCodeSequence> either =
                ClassifiedCodeSequence.create(codeNums);
        if (either.isLeft()) {
            final InvalidCodeSequence errorCode = either.getLeft();
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Code Sequence");
            alert.setHeaderText("Invalid Code Sequence");
            alert.setContentText(InvalidCodeSequence.errorMessage(codeNums, errorCode));

            alert.showAndWait();
            return;
        }

        final ClassifiedCodeSequence codeSeq = either.get();

        final Optional<Info> opt = Wrapper.loadInfo(codeSeq, pool);

        if (opt.isPresent()) {

            final Info info = opt.get();

            final String str = infoString(codeSeq, info);

            text.setText(str);
        } else {
            // empty region, so show info box for that
            final Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Code Sequence Reduced to Empty Set");
            alert.setHeaderText("Code Sequence Reduced to Empty Set");
            alert.setContentText(String.format(
                    "Code sequence %s was reduced to empty set.", codeSeq.toString()));

            alert.showAndWait();
        }*/
    }

    private static String infoString(final ClassifiedCodeSequence codeSeq, final Info info) {
        final StringBuffer buff = new StringBuffer();

        buff.append(String.format("code sequence = %s%n", codeSeq.toString()));
        buff.append(String.format("code length = %d%n", codeSeq.codeLength));
        buff.append(String.format("code sum = %d%n", codeSeq.codeSum));
        buff.append(String.format("code type = %s%n", codeSeq.codeType));

        final String evenOdd = CodeSequence.evenOddSequence(codeSeq.codeSequence.codeNumbers);

        buff.append(String.format("even odd = %s%n", evenOdd));

        buff.append('\n');

        final Tuple2<XYZ, XYZ> initialAngles = Database.parseInitialAngles(info.initialAngles);

        buff.append("initial angles = " + initialAngles._1 + initialAngles._2);
        buff.append("\n\n");

        final LinCom<XYEta> constraint = Database.findConstraintEta(codeSeq.codeSequence.codeNumbers, initialAngles._1, initialAngles._2);
        buff.append("constraint = " + formatConstraint(constraint));
        buff.append("\n\n");

        buff.append("MRR region\n");

        final ImmutableList<Vector2> points = Database.parsePoints(info.points);

        for (final Vector2 point : points) {

            final double degX = Math.toDegrees(point.x);
            final double degY = Math.toDegrees(point.y);

            buff.append(String.format("(%f, %f)%n", degX, degY));
        }

        buff.append('\n');

        // Empty is a sentinel value to indicate there is nothing there
        if (!info.codeSeqLR.isEmpty()) {
            buff.append(String.format("LR code sequence = %s%n%n", info.codeSeqLR));
        }

        buff.append("Left Right\n");

        final ImmutableList<LeftRight> leftRights = Database.parseLeftRights(info.leftRights);



        for (final LeftRight leftRight : leftRights) {
            buff.append(leftRight);
            buff.append('\n');
        }

        buff.append('\n');

        buff.append("Bounds and MRR equations\n");

        final ImmutableList<Equation> equations = Database.parseEquations(info.equations);

        for (final Equation equation : equations) {
            buff.append(String.format("%d, %s\n\n", (int) equation.bound, equation.toString()));
        }

        return buff.toString();
    }

    private static String toString(final XYEta symbol) {
        switch (symbol) {
            case X:
                return "x";
            case Y:
                return "y";
            case Eta:
                return "90";
            default:
                throw new RuntimeException("unknown symbol " + symbol);
        }
    }

    // Sadly, there is no generic way of implementing this in Java, so we will just hard code
    // it for this special case
    private static String formatConstraint(final LinCom<XYEta> constraint) {

        if (constraint.isZero()) {
            return "0";
        }

        final EnumMap<XYEta, Integer> coeffMap = new EnumMap<>(XYEta.class);
        coeffMap.put(XYEta.X, constraint.coeff(XYEta.X));
        coeffMap.put(XYEta.Y, constraint.coeff(XYEta.Y));
        coeffMap.put(XYEta.Eta, constraint.coeff(XYEta.Eta));

        boolean front = true;

        final StringBuilder builder = new StringBuilder();

        for (final Map.Entry<XYEta, Integer> entry : coeffMap.entrySet()) {

            final XYEta symbol = entry.getKey();
            final Integer coeff = entry.getValue();

            if ((coeff > 0) && !front) {
                builder.append('+');
            }

            if (coeff == -1) {
                builder.append('-');
            } else if ((coeff != 0) && (coeff != 1)) {
                builder.append(coeff);

                if (symbol == XYEta.Eta) {
                    builder.append('*');
                }
            }

            if (coeff != 0) {
                builder.append(toString(symbol));
                front = false;
            }
        }

        return builder.toString();
    }
}
