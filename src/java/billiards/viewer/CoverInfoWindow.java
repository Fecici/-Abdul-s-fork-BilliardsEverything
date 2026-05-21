package billiards.viewer;

import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public final class CoverInfoWindow {
    private final String extra_info="Cover information";
    private final VBox root = new VBox();
    private final TextArea text = new TextArea();
    private final Stage stage = new Stage();
    private final Scene scene = new Scene(root);
    final String dir= "cover";
    public CoverInfoWindow(final String windowTitle){

        final String polygonString = Utils.readFromFile(dir + "/polygon.txt").trim();
        final String squareString = Utils.readFromFile(dir + "/square.txt").trim();
        final String stablesString = Utils.readFromFile(dir + "/stables.txt").trim();
        final String triplesString = Utils.readFromFile(dir + "/triples.txt").trim();
        final String coverString = Utils.readFromFile(dir + "/cover.txt").trim();
        //final String halfTripleString = Utils.readFromFile(dir + "/half_triples.txt").trim();
        final String infoString = Utils.readFromFile(dir + "/info.txt").trim();
        final String unused = Utils.readFromFile(dir + "/unused.txt").trim();
        final String precisionString = Utils.readFromFile(dir + "/precision.txt").trim();


        stage.setScene(scene);
        stage.setTitle(windowTitle);
        stage.setOnCloseRequest(event -> stage.close());
        text.setPrefColumnCount(60);
        text.setPrefRowCount(20);
        text.setWrapText(true);
        text.setEditable(false);
        text.setFont(Font.font("Monaco", 16));

        VBox.setVgrow(text, Priority.ALWAYS);
        text.setText(infoString+"\n"+polygonString+"\n"+squareString+"\n"+stablesString+"\n"+triplesString+"\n"+"\n"+coverString+"\n"+unused+"\n"+precisionString);
        root.getChildren().addAll( text);

    }
    public void close() {
        stage.close();
    }
    public void show() {
        stage.show();
    }
}
