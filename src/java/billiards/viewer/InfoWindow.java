package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.CodeSequence;
import billiards.codeseq.InvalidCodeSequence;
import billiards.database.Database;
import billiards.database.Info;
import billiards.database.InfoAll;
import billiards.database.LeftRight;
import billiards.geometry.Vector2;
import billiards.math.Equation;
import billiards.math.LinCom;
import billiards.math.XYEta;
import billiards.math.XYZ;
import billiards.wrapper.ConnectionPool;
import billiards.wrapper.Wrapper;

import javaslang.Tuple2;
import javaslang.control.Either;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public final class InfoWindow {
    private final TextArea text = new TextArea();
    private final Button showButton = new Button();
    private final VBox root = new VBox();
    private final Stage stage = new Stage();
    private final Scene scene = new Scene(root);
    private final HBox inputHBox = new HBox();
    private final TextField codeNumbersTextField = new TextField();

    // TODO put infos in a list so we can close them all after
    public InfoWindow(final String windowTitle, final ConnectionPool pool) {
        stage.setScene(scene);
        stage.setTitle(windowTitle);
        stage.setOnCloseRequest(event -> stage.close());

        text.setPrefColumnCount(60);
        text.setPrefRowCount(20);
        text.setWrapText(true);
        text.setEditable(false);
        text.setFont(Font.font("Monaco", 16));

        // We want the text to expand as we make the window bigger
        VBox.setVgrow(text, Priority.ALWAYS);

        inputHBox.getChildren().addAll(codeNumbersTextField, showButton);
        inputHBox.setSpacing(10);

        codeNumbersTextField.setPromptText("Code Sequence");

        showButton.setText("Show");
        showButton.setOnAction(event -> showInfo(pool));

        root.getChildren().addAll(inputHBox, text);
        root.setSpacing(10);
        root.setPadding(new Insets(10));
    }

    public void close() {
        stage.close();
    }

    public void show() {
        stage.show();
    }

    private void showInfo(final ConnectionPool pool) {
        final String codeNumsString = codeNumbersTextField.getText();

        if (codeNumsString.isEmpty()) {
            final Alert alert = new Alert(AlertType.ERROR);

            alert.setTitle("Enter a Code Sequence");
            alert.setHeaderText("Enter a Code Sequence");
            alert.setContentText("Please enter a code sequence.");
            alert.showAndWait();
            return;
        }

        final Optional<ImmutableIntList> optional = Utils.splitString(Utils.trimCodeLine(codeNumsString));

        if (!optional.isPresent()) {
            final Alert alert = new Alert(AlertType.ERROR);
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
            final Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Invalid Code Sequence");
            alert.setHeaderText("Invalid Code Sequence");
            alert.setContentText(InvalidCodeSequence.errorMessage(codeNums, errorCode));

            alert.showAndWait();
            return;
        }

        final ClassifiedCodeSequence codeSeq = either.get();

        final Optional<Info> opt = Wrapper.loadInfo(codeSeq, pool);
        
        //start
        //part1/3
        //george Oct1st2021 turn the all equations on in the info window.

        /*
         final Optional<InfoAll> opt2 = Wrapper.loadAllEquation(codeSeq,pool);

         if (opt.isPresent()&& opt2.isPresent()) {

            final Info info = opt.get();
            final InfoAll all = opt2.get();

            String allSin= all.leftRights;
            if (!allSin.equals("")){
                allSin=allSin.replace("\n","\nsin ");
                allSin="sin "+allSin;
            }
            String allCos= all.codeSeqLR;
            if (!allCos.equals("")){
                allCos=allCos.replace("\n","\ncos ");
                allCos="cos "+allCos;
            }
            final String str = infoString(codeSeq, info,allSin,allCos);
            text.setText(str);
        }*/
         
        //end
        
        //comment below out
        //part2/3 start
        if (opt.isPresent()) {

        	final Info info = opt.get();
            String allSin= "";
           	String allCos="";
            final String str = infoString(codeSeq, info,allSin,allCos);
           text.setText(str);
         }
         //part2 end
         else {
            // empty region, so show info box for that
            final Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Code Sequence Reduced to Empty Set");
            alert.setHeaderText("Code Sequence Reduced to Empty Set");
            alert.setContentText(String.format(
                "Code sequence %s was reduced to empty set.", codeSeq.toString()));

            alert.showAndWait();
        }
    }


    private static String infoString(final ClassifiedCodeSequence codeSeq, final Info info,final String sin,final String cos) {
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
        //System.out.println(info.equations);
        final ImmutableList<Equation> equations = Database.parseEquations(info.equations);
        


        for (final Equation equation : equations) {
            buff.append(String.format("%d, %s\n\n", (int) equation.bound, equation.toString()));
        }
        // george aug20 2021 to shut off the all equations in infowindow 
        //part3/3
        //start
        /*
        final ImmutableList<Equation> allSin = Database.parseEquations(sin);
        final ImmutableList<Equation> allCos = Database.parseEquations(cos);
        buff.append("All equations\n");
        for (final Equation equation : allCos) {
            buff.append(String.format("%d, %s\n\n", (int) equation.bound, equation.toString()));
        }
        for (final Equation equation : allSin) {
            buff.append(String.format("%d, %s\n\n", (int) equation.bound, equation.toString()));
        }*/
        //end

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
