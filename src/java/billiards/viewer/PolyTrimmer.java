package billiards.viewer;

import billiards.geometry.ConvexPolygon;
import billiards.geometry.Rectangle;
import billiards.codeseq.Storage;
import billiards.database.Database;
import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.CodeSequence;
import billiards.wrapper.Wrapper;
import billiards.wrapper.ConnectionPool;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.impl.set.sorted.mutable.TreeSortedSet;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javaslang.Tuple2;
import javaslang.Tuple3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Optional;

import static billiards.utils.Polygon.cleanPolygon;
import static billiards.utils.Polygon.createConvexPolygon;

public final class PolyTrimmer {

    private static final String fileName = Viewer.tmpDir + "PolyTrim.txt";

    private final TextArea text = new TextArea();
    private final Button trimRegion = new Button();
    private final Button trimFile = new Button();
    private final Button trimCover = new Button();

    private final VBox root = new VBox();
    private final HBox trimHBox = new HBox(10);
    private final Stage stage = new Stage();
    private final Scene scene = new Scene(root);
    private final Label instruct = new Label();

    public PolyTrimmer(final Rectangle fullScreen, final ConnectionPool pool, final LinkedHashMap<Storage, Color> onScreenSequences, final boolean saveColors) {

        stage.setScene(scene);

        stage.setTitle("Trim");
        stage.setOnCloseRequest(event -> stage.close());

        text.setPrefColumnCount(40);
        text.setPrefRowCount(10);
        text.setWrapText(true);
        text.setEditable(true);
        text.setFont(Font.font("Monaco", 16));
        text.setText(Utils.readFromFile(fileName));

        instruct.setText("Enter points on separate lines, with the coordinates separated by a space.");
        instruct.setPadding(new Insets(5, 5, 5, 10));

        // We want the text to expand as we make the window bigger
        VBox.setVgrow(text, Priority.ALWAYS);

        trimHBox.getChildren().addAll(trimRegion, trimFile, trimCover);
        trimHBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(instruct, trimHBox, text);
        root.setSpacing(10);
        root.setPadding(new Insets(10));

        trimRegion.setText("Trim Screen");
        trimRegion.setTooltip(Utils.toolTip("Poly trim the on-screen regions. They are saved to square.txt"));
        Utils.colorButton(trimRegion, Color.SKYBLUE, Color.GOLD);
        trimRegion.setOnAction(event -> {

            final String fullContent = text.getText().trim();

            Utils.writeToFile(fileName, fullContent);

            final ConvexPolygon poly = parseConvexPolygon(fullContent, fullScreen);

            trim(poly, onScreenSequences, saveColors);

            stage.close();
        });

        trimFile.setText("Trim File");
        trimFile.setTooltip(Utils.toolTip("Poly trim a file. The result is saved the old file name + PolyTrim.txt"));
        Utils.colorButton(trimFile, Color.SKYBLUE, Color.GOLD);
        trimFile.setOnAction(event -> {

            final String fullContent = text.getText().trim();

            Utils.writeToFile(fileName, fullContent);

            final ConvexPolygon poly = parseConvexPolygon(fullContent, fullScreen);

            trim(poly, pool, stage);

            stage.close();
        });

        trimCover.setText("Trim Cover");
        trimCover.setTooltip(Utils.toolTip("Poly trim a cover. The result is saved to the cover folder."));
        Utils.colorButton(trimCover, Color.SKYBLUE, Color.GOLD);
        trimCover.setOnAction(event -> {

            final String fullContent = text.getText().trim();

            Utils.writeToFile(fileName, fullContent);

            final String polyStr = cleanPolygon(fullContent);

            trim(polyStr, stage);

            stage.close();
        });

        stage.showAndWait();
    }

    private static ConvexPolygon parseConvexPolygon(final String content, final Rectangle fullScreen) {

        final ConvexPolygon poly;
        if (content.isEmpty()) {
            poly = fullScreen.toConvexPolygon();
        } else {
            final String cleaned = cleanPolygon(content);

            poly = createConvexPolygon(cleaned);
        }

        return poly;
    }

    private static void trim(final ConvexPolygon poly, final ConnectionPool pool, final Stage stage) {

        // Trim a file so that it contains only codes which intersect a specified polygon
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load File");
        final File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            final Tuple2<Tuple3<
                    Optional<Rectangle>,
                    Map<ClassifiedCodeSequence, Optional<Color>>,
                    Map<ClassifiedCodeSequence, Optional<String[]>>
                    >,
                    ArrayList<ClassifiedCodeSequence[]>> tup = Viewer.parseFile(file.toPath(), false);

            final Map<ClassifiedCodeSequence, Optional<Color>> map = tup._1._2;

            final MutableSortedSet<ClassifiedCodeSequence> classCodeSeqs = new TreeSortedSet<>(map.keySet());

            final String newTitle = file.getAbsolutePath().split(".txt")[0] + "_PolyTrim.txt";

            final MutableList<String> lines = new FastList<>();

            classCodeSeqs.forEach(code -> {
                final Storage storage = Database.loadStorage(code, pool).get();
                if (storage.intersects(poly)) {
                    final String toPad =
                        code.codeType + " (" + code.codeLength + ", " + code.codeSum + ") ";
                    final String evenOdd = CodeSequence.evenOddSequence(code.codeSequence.codeNumbers);

                    final String lineStr = String.format("%1$-16s", toPad) + code + " " + evenOdd;
                    lines.add(lineStr);
                }
            });

            Utils.printToFile(newTitle, lines);
        }
    }

    private static void trim(final ConvexPolygon poly, final LinkedHashMap<Storage, Color> onScreenSequences, final boolean saveColors) {

        final MutableList<String> lines = new FastList<>();

        onScreenSequences.forEach((storage, color) -> {
            if (storage.intersects(poly)) {
                final String string;
                if (saveColors) {
                    string = storage + ", " + color;
                } else {
                    string = storage.toString();
                }
                lines.add(string);
            }
        });

        final double minX = poly.projectX().min;
        final double maxX = poly.projectX().max;
        final double minY = poly.projectY().min;
        final double maxY = poly.projectY().max;

        try (final PrintWriter writer = new PrintWriter("square.txt")) {
            writer.println("rectangle: " + minX + " " + maxX + " " + minY + " " + maxY);
            lines.forEach(writer::println);
        } catch (final FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void trim(final String polyStr, final Stage stage) {

        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose a Cover Directory");

        final File inDir = chooser.showDialog(stage);

        if (inDir != null) {

            final File outDir = new File("cover");

            outDir.mkdir();

            Wrapper.trimCover(polyStr, inDir.getPath(), outDir.getPath());
        }
    }
}
