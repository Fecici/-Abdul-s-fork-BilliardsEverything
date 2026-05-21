package billiards.viewer;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class ColorPicker {
    private static final int ROWS = 12;
    private static final int COLS = 12;
    private static final int RECT_SIZE = 20; // rect size in pixels

    private static final Color[][] color = {

        // first row
        {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.MAGENTA, Color.CHOCOLATE,
         Color.ORANGE, Color.PINK, Color.LIME, Color.GOLD, Color.PURPLE,
         Color.TURQUOISE},

        // second row
        {Color.TRANSPARENT, Color.WHITE, Color.WHITESMOKE, Color.GAINSBORO, Color.LIGHTGRAY,
         Color.SILVER, Color.DARKGRAY, Color.GRAY, Color.DIMGRAY, Color.BLACK,
         Color.BLACK, Color.BLACK},

        // rows 3 through 12
        {Color.LIGHTGREEN, Color.MEDIUMAQUAMARINE, Color.SEAGREEN, Color.CORAL,
         Color.PERU, Color.SIENNA, Color.ORANGERED, Color.DARKORANGE,
         Color.MEDIUMVIOLETRED, Color.VIOLET, Color.DARKVIOLET, Color.INDIGO},
        {Color.HONEYDEW, Color.MINTCREAM, Color.LIGHTCYAN, Color.LIGHTSKYBLUE,
         Color.PALETURQUOISE, Color.POWDERBLUE, Color.CORNFLOWERBLUE, Color.STEELBLUE,
         Color.TEAL, Color.CADETBLUE, Color.DARKCYAN, Color.DARKSEAGREEN},
        {Color.OLDLACE, Color.WHEAT, Color.PAPAYAWHIP, Color.PALEVIOLETRED, Color.PEACHPUFF,
         Color.LINEN, Color.TAN, Color.LIGHTSALMON, Color.PINK, Color.FUCHSIA,
         Color.ORCHID, Color.DARKORCHID},
        {Color.LEMONCHIFFON, Color.LIGHTYELLOW, Color.LIGHTGOLDENRODYELLOW, Color.CORNSILK,
         Color.BISQUE, Color.KHAKI, Color.PALEGOLDENROD, Color.YELLOW, Color.GOLD,
         Color.GOLDENROD, Color.DARKGOLDENROD, Color.OLIVE},
        {Color.LAVENDERBLUSH, Color.MISTYROSE, Color.DARKSALMON, Color.LIGHTCORAL, Color.SALMON,
         Color.INDIANRED, Color.TOMATO, Color.CRIMSON, Color.RED, Color.FIREBRICK,
         Color.MAROON, Color.DARKRED},
        {Color.THISTLE, Color.LIGHTPINK, Color.PLUM, Color.HOTPINK, Color.MAGENTA,
         Color.DEEPPINK, Color.MEDIUMORCHID, Color.MEDIUMSLATEBLUE, Color.MEDIUMPURPLE,
         Color.SLATEBLUE, Color.BLUEVIOLET, Color.DARKMAGENTA},
        {Color.AZURE, Color.ANTIQUEWHITE, Color.BEIGE, Color.BLANCHEDALMOND, Color.MOCCASIN,
         Color.NAVAJOWHITE, Color.BURLYWOOD, Color.DARKKHAKI, Color.SANDYBROWN,
         Color.ROSYBROWN, Color.BROWN, Color.SADDLEBROWN},
        {Color.PALEGREEN, Color.GREENYELLOW, Color.YELLOWGREEN, Color.SPRINGGREEN,
         Color.LIMEGREEN, Color.CHARTREUSE, Color.LAWNGREEN, Color.MEDIUMSEAGREEN,
         Color.FORESTGREEN, Color.OLIVEDRAB, Color.DARKOLIVEGREEN, Color.DARKGREEN},
        {Color.AQUA, Color.CYAN, Color.AQUAMARINE, Color.DARKTURQUOISE, Color.TURQUOISE,
         Color.MEDIUMSPRINGGREEN, Color.MEDIUMTURQUOISE, Color.ROYALBLUE, Color.SKYBLUE,
         Color.DODGERBLUE, Color.DEEPSKYBLUE, Color.BLUE},
        {Color.LAVENDER, Color.LIGHTBLUE, Color.LIGHTSTEELBLUE, Color.LIGHTSLATEGRAY,
         Color.SLATEGRAY, Color.DARKSLATEGRAY, Color.DARKSLATEBLUE, Color.MEDIUMBLUE,
         Color.INDIGO, Color.DARKBLUE, Color.MIDNIGHTBLUE, Color.NAVY}

    };

    private final GridPane grid = new GridPane();
    private final Stage stage = new Stage();

    private Optional<Color> selectedColor = Optional.empty();

    public ColorPicker(final int x, final int y) {
        stage.setX(x);
        stage.setY(y);

        stage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                stage.hide();
            }
        });

        stage.setScene(new Scene(grid));
        stage.setTitle("SO COLORFUL");
        grid.setPadding(new Insets(10));

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                final Color currentColor = color[row][col];

                final Rectangle rect = new Rectangle(RECT_SIZE, RECT_SIZE, currentColor);

                rect.setOnMouseClicked(event -> {
                    selectedColor = Optional.of(currentColor);
                    stage.close();
                });

                Tooltip.install(rect, new Tooltip(Colors.colorMap.get(currentColor).get()));

                grid.add(rect, col, row);
            }
        }
    }

    public static Color next(final Color inColor) {
        if (inColor.equals(Color.BLACK)) {
            return Color.RED;
        }
        for (int i = 0; i < 12; i++) {
            if (color[0][i].equals(inColor)) {
                return color[0][(i + 1) % 12];
            }
        }
        return Color.BLACK;
    }

    public Optional<Color> pickColor() {
        stage.showAndWait();
        return selectedColor;
    }
}
