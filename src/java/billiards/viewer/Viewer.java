/**
 * Note: if you want to go to the corresponding function calls, search for the labels
 * 1. Calculation of Expando:
 *     a. Calculate code sequences: label 1, label 7
 *        call getMultipleExpandos (label 1) to get an Array of ClassifiedCodeSequence
 *          - you can uncomment laabe 7 to print the code sequences
 *
 *     b. Calculate left rights: label 2, label 3
 *        call subtractLeftRight (label 2) to get the pattern from the 2 input left rights
 *        call addLeftRight (label 3) to get an Array of left rights (in string type) from pattern
 *
 *     c. Calculate and store the {Equations, Points, Left rights} from the (code sequences, left rights) calculated above: label 4
 *        call Wrapper.loadPictureLR (label 4, note that this function uses a native interface (in Wrapper.java) to use C++ function)
 *
 *     d. Draw the regions: label 5, label 6
 *        use the class DrawPictureTask (label 5, in billiards/java/viewer/DrawPictureTask.java) to get a task
 *        if task succeeds, call renderRegion (label 6) to draw the region, otherwise throw runtime exception
 *
 * 2. Calculation of Iteration:
 *     a. Call iterateAction (label 8), note that in parameter
 *
 *     b. TODO
 *
 * */


package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.CodeSequence;
import billiards.codeseq.CodeType;
import billiards.codeseq.InvalidCodeSequence;
import billiards.codeseq.Storage;
import billiards.cover.CoverStuff;
import billiards.cover.HalfTriple;
import billiards.cover.Triple;
import billiards.database.*;
import billiards.geometry.*;
import billiards.geometry.Rectangle;

import billiards.geometry.Vector2;
import billiards.math.XYPi;
import billiards.utils.BatchLoadStorage;
import billiards.utils.PrintMid;
import billiards.wrapper.ConnectionPool;
import billiards.wrapper.Wrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TextArea;

import javaslang.*;
import javaslang.collection.Array;
import javaslang.control.Either;

import java.sql.*;

import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.impl.bimap.mutable.HashBiMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.set.sorted.mutable.TreeSortedSet;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.transform.Affine;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import static billiards.codeseq.CodeType.OSNO;
import static billiards.viewer.Utils.getCoverCodeString;
import static billiards.viewer.Utils.readFromFile;
//Suryansh
import billiards.viewer.Updater;

// Places for input
// btnIterate in other window
// calculateCurrentCodeNumbers
// load file
// iterate on the left window

// open a java file and write strings to it
// use JavaCompiler to compile those files
// use classloader to load the class files

// All the gui stuff is going to be in a viewer file, which we then
// instantiate, and run the main code from here (as well as database and
// thread pool stuff)
public final class Viewer {
    public static final String tmpDir = "tmp/";
    public static String dbname = null;

    // the time the tool tips take to open and close, in seconds
    private static final double TipOpenDelay = 2;
    private static final double TipCloseDelay = 20;

    // how many pixels wide is the Hitbox for unstables on screen
    private static final double unstableDetect = 6;

    // when using classify, this makes it group the codes according to their type
    private static final boolean splitUp = true;

    // the size of the square viewer window, should be 75% as tall as the screen?
    private static final int SIDE = 600;

    private static final int BTNPADBOTTOM = 4;

    // IMPORTANT: This is the color of the 90 and 80 lines and any additional lines
    private static final Color lineColor = Color.MAGENTA;
    private static final Color screenFillsColor = Color.MIDNIGHTBLUE;

    AtomicInteger progressWindows = new AtomicInteger(5);

    final Color clickColor = Color.GOLD;
    final String textBoxColor = Utils.hex(Color.MISTYROSE);
    final Color plusColor = Color.LIGHTGREEN;
    final Color minusColor = Color.LIGHTCORAL;
    final Color panColor = Color.MAROON;
    final Color polyBoundColor = Color.BLACK;
    final Color fillBoundColor = Color.YELLOW;
    final Color coverPolyBoundColor = Color.LIME;
    final Color coverAreaColor = Color.DARKORANGE;

    // All the JavaFX gui components are global, but the arrays and maps and such
    // that we use to help them out are created in the constructor and then passed
    // to the handlers as necessary
    // 01/05/18 changed this to a list of 3, so we can put triples here

    MutableIntList[] currentCodeNumbers = {new IntArrayList(), new IntArrayList(), new IntArrayList()};

    final ConnectionPool pool;

    final PixelRadianMap map = new PixelRadianMap(SIDE);  // QUESTION: figure out how this works

    // The map has the default viewing rectangle, and that is the beginning place to go
    final BackwardForward<Rectangle> viewRectangleBF =
            BackwardForward.create(map.getViewRectangle());

    // these are the code sequences that are currently on screen
    // we want to remember the ordering everything gets
    final LinkedHashMap<Storage, Color> onScreenSequences = new LinkedHashMap<>();
    boolean trim = true;
    // Cycle between two colors
    final Cycle cycle = new Cycle(2);//george changed the 2 to 3 may 2,2019
    // the colors in the drop down menus
    // We might add more colors later, so this abstraction is nice
    final Map<Integer, Color> comboBoxColors = new HashMap<>();

    // Zhao Yu Li, May 14, 2025.
    // Storages the codes from the tetrahedron vary tasks, so that all vary tasks from the tetrahedron have a single
    // place to store their codes. This list can then be used to do a final comparison to find the intersection.
    ArrayList<MutableSortedSet<ClassifiedCodeSequence>> tetrahedronCodes = new ArrayList<>();

    final Map<ConvexPolygon, Color> mrrBounds = new HashMap<>();

    // No files by default
    ArrayList<String> fileCodeSequences = new ArrayList<>();

    ArrayList<Rectangle> screenFills = new ArrayList<>();
    final HashTriple coverRects = new HashTriple();
    ArrayList<ConvexPolygon> innerPolyBounds = new ArrayList<>();
    ArrayList<ConvexPolygon> outerPolyBounds = new ArrayList<>();
    Optional<ConvexPolygon> coverPolyBound1 = Optional.empty();
    Optional<ConvexPolygon> coverPolyBound2 = Optional.empty();
    Optional<ConvexPolygon> coverArea = Optional.empty();
    Optional<ConvexPolygon> autoVaryArea = Optional.empty();

    // Zhao Yu Li, Aug 13, 2025.
    // The small cover (LiCover) can handle multiple empty squares at the same time. Therefore, we need to be able to
    // remember all of them and draw them.
    ArrayList<ConvexPolygon> smallCoverAreas = new ArrayList<>();

    // the current storage and color from the OBO file
    Storage currentOBOStorage = null;
    Color currentOBOColor = Color.RED;

    final ArrayList<MutableBiMap<Button, Integer>> plusButtonsBiMap = new ArrayList<>();
    final ArrayList<MutableBiMap<Button, Integer>> minusButtonsBiMap = new ArrayList<>();

    // GUI components

    // This gets passed in the constructor, and we initialize it there
    final Stage mainWindow;

    // iteration window
    final TextField txtCodeSequence2 = new TextField();

    final HBox[] plusButtons = {new HBox(), new HBox(), new HBox()};
    final HBox[] minusButtons = {new HBox(), new HBox(), new HBox()};
    final VBox btnsVBox = new VBox();

    // Zhao Yu Li, May 27, 2025.
    final Button lookupButton = new Button();  // Button to look up iteration patterns from the database
    final CheckBox intersectCheckBox = new CheckBox();  // Checkbox to optionally specify a polygon to intersect
    final Button intersectPolygonButton = new Button();  // Opens a new window to specify the polygon
    final HBox iterationToolsHBox = new HBox();

    // Zhao Yu Li, May 29, 2025.
    // There can be a large number of codes generated from an iteration calculation. Use this to optionally limit the
    // the number of code sequences drawn and added to the cover.
    final Label intersectionLimitLabel = new Label();
    final TextField intersectionLimitTextField = new TextField();

    final IterationPolyWindow iterationPolyWindow = new IterationPolyWindow();

    final TextField box1 = new TextField();
    final Button increaseBox1 = new Button();
    final Button decreaseBox1 = new Button();

    final TextField box2 = new TextField();
    final Button increaseBox2 = new Button();
    final Button decreaseBox2 = new Button();

    final Button stablesButton = new Button();

    final TextField box3 = new TextField();
    final Button addSubtractBox3 = new Button();
    final Button addSubtractReverseBox3 = new Button();

    final HBox manualIncrementHBox = new HBox();

    // First Pattern
    final Label firstPatternLabel = new Label();
    final TextField firstPatternTextField = new TextField();
    final Label firstPatternIterationsLabel = new Label();
    final TextField firstPatternIterationsTextField = new TextField();
    final Label firstPatternIncrementLabel = new Label();
    final TextField firstPatternIncrementTextField = new TextField();

    final HBox drawSquareHBox = new HBox();
    final TextField squareCodeSequenceField = new TextField();
    final TextField squareMagnificationField = new TextField();
    final Button drawSquareButton = new Button();

    // Second Pattern
    final Label secondPatternLabel = new Label();
    final TextField secondPatternTextField = new TextField();
    final Label secondPatternIterationsLabel = new Label();
    final TextField secondPatternIterationsTextField = new TextField();
    final Label secondPatternIncrementLabel = new Label();
    final TextField secondPatternIncrementTextField = new TextField();

    // Third Pattern
    final Label thirdPatternLabel = new Label();
    final TextField thirdPatternTextField = new TextField();
    final Label thirdPatternIterationsLabel = new Label();
    final TextField thirdPatternIterationsTextField = new TextField();
    final Label thirdPatternIncrementLabel = new Label();
    final TextField thirdPatternIncrementTextField = new TextField();

    // Fourth Pattern
    final Label fourthPatternLabel = new Label();
    final TextField fourthPatternTextField = new TextField();
    final Label fourthPatternIterationsLabel = new Label();
    final TextField fourthPatternIterationsTextField = new TextField();
    final Label fourthPatternIncrementLabel = new Label();
    final TextField fourthPatternIncrementTextField = new TextField();

    // Add/Subtract Pattern
    final Label addSubtractPatternLabel = new Label();
    final TextField addSubtractPatternTextField = new TextField();
    final Label addSubtractPatternIterationsLabel = new Label();
    final TextField addSubtractPatternIterationsTextField = new TextField();
    final Label addSubtractPatternIncrementLabel = new Label();
    final TextField addSubtractPatternIncrementTextField = new TextField();
    final Button addSubtractIterationsButton = new Button();

    final Label leftrightsLabel= new Label();
    final Label leftrightsLabel2= new Label();
    final Label leftrightsLabel1=new Label();

    final Label sequence=new Label();
    final TextArea leftrightsTextArea = new TextArea();
    final TextArea leftrightsTextArea2 = new TextArea();
    final TextArea leftrightsTextArea1 = new TextArea();

    final TextField expandoCodeSequce = new TextField();
    final Button expandoButton=new Button();

    final Label expandoPatternLabel = new Label();
    final TextField expandoPatten = new TextField();
    final Label expandoIterationsLabel = new Label();
    final TextField expandoIterations=new TextField();
    final Label expandoElementsLabel = new Label();
    final TextField expandoElements=new TextField();
    //caculate expando button
    final Button expandoCalculateButton= new Button();

    final GridPane iterationsGridPane = new GridPane();

    final Button iterationsCalculateButton = new Button();

    final HBox calculateIterationsHBox = new HBox();
    final Button btnCalculate2 = new Button();

    final TextField labelCodeWindow = new TextField();

    final RadioButton uselrRdoBtn = new RadioButton();
    final RadioButton showlrRdoBtn = new RadioButton();
    final RadioButton nolrRdoBtn = new RadioButton();
    final RadioButton uselrTestBtn = new RadioButton();
    final ToggleGroup lrGroup = new ToggleGroup();

    final VBox codeWindowVBox = new VBox();

    // This has to be passed to the constructor here
    final Scene codeWindowScene = new Scene(codeWindowVBox);

    final Stage codeWindow = new Stage();

    IterateToLimitWindow iterateToLimitWindow = null;

    // main window
    final Button iterateToLimitBtn = new Button();
    final GridPane codeSequencesGPane = new GridPane();

    final RadioButton marinovRdoBtn = new RadioButton();
    final RadioButton boyanRdoBtn = new RadioButton();
    final ToggleGroup menuGroup = new ToggleGroup();

    // the viewer image
    final StackPane imageStack = new StackPane();

    // static method, since the background is just white
    final ImageView backgroundImageView = renderColor(Color.WHITE);
    final ImageView guideLinesImageView = new ImageView();
    final ImageView regionsImageView = new ImageView();
    final ImageView boundsImageView = new ImageView();

    // the oboImageView is kept separate, because it allows us to redraw this
    // one without redrawing everything else
    final ImageView oboImageView = new ImageView();

    // This one is transparent, and goes on top to capture all the mouse events
    final ImageView topImageView = renderColor(Color.TRANSPARENT);

    // right side box with mouse coordinates and code info
    // Degree
    final HBox textXHBox = new HBox();
    final Label textXLabel = new Label();
    final TextField textXField = new TextField();

    final HBox textYHBox = new HBox();
    final Label textYLabel = new Label();
    final TextField textYField = new TextField();

    // Lock
    final HBox textXLockHBox = new HBox();
    final Label textXLockLabel = new Label();
    final TextField textXLockField = new TextField();

    final HBox textYLockHBox = new HBox();
    final Label textYLockLabel = new Label();
    final TextField textYLockField = new TextField();

    // Radian
    final HBox textXRadianHBox = new HBox();
    final Label textXRadianLabel = new Label();
    final TextField textXRadianField = new TextField();

    final HBox textYRadianHBox = new HBox();
    final Label textYRadianLabel = new Label();
    final TextField textYRadianField = new TextField();

    // the navigation menu
    final TextField xMinTextField = new TextField();
    final TextField xMaxTextField = new TextField();
    final TextField yMinTextField = new TextField();
    final TextField yMaxTextField = new TextField();


    final Button zoomButton = new Button();
    //Color zoomColor = Color.RED;// george july15th 2021 change zoom color red
    Color zoomColor = Color.SIENNA;


    final RadioButton selectRdoBtn = new RadioButton();
    final RadioButton magnifyRdoBtn = new RadioButton();
    final RadioButton centerBtn = new RadioButton();
    final RadioButton demagnifyRdoBtn = new RadioButton();
    final ToggleGroup magnifyGroup = new ToggleGroup();

    final Label zoomScaleLabel = new Label();
    final TextField zoomScaleText = new TextField();
    final Button backwardSquareButton = new Button();
    final Button forwardSquareButton = new Button();

    // the Marinov menu
    final Button clearBtn = new Button();
    final Button resetBtn = new Button();
    final Button loadDirectoryButton = new Button();
    final Button btnLoadFile = new Button();
    final Button infoButton = new Button();
    final Button gradientButton = new Button();
    final Button lookAtMeButton=new Button();
    final Button classifyBtn = new Button();

    final CheckBox saveColors = new CheckBox();
    final CheckBox saveRegionsCheckBox = new CheckBox();
    final CheckBox drawPictureCheckBox = new CheckBox();
    final CheckBox loadLRCheckBox = new CheckBox();

    final TextField txtCodeSequence = new TextField();
    final Button btnCalculate = new Button();
    final CheckBox calculateAddToCoverCheckBox = new CheckBox();
    final ComboBox<String> calculateChooser = new ComboBox<>();

    final Button cboxRegionColor0 = new Button();
    final Button cboxRegionColor1 = new Button();
    final Button cboxRegionColor2 = new Button();//george may 2,2019
    final TextField iterationStart = new TextField();
    final TextField iterationEnd = new TextField();

    final CheckBox reflectCheckBox = new CheckBox();
    private final Affine reflectionTransform = new Affine();
    final CheckBox allCheckBox = new CheckBox();
    final Button queryButton = new Button();
    final Button polyLoadButton = new Button();
    final Button polyLoadDBButton = new Button();
    final Button parallelogramButton = new Button();
    final Button mergeButton = new Button();

    final Button newPolyTrimBtn = new Button();

    final Button zoomRegionButton = new Button();
    final Button zoomColorButton = new Button();

    final Button fillScreenBtn = new Button();
    final Button clearFillsBtn = new Button();
    final Button saveFillBtn = new Button();
    final Button loadFillBtn = new Button();
    final CheckBox showFillsCheckBox = new CheckBox();

    final Button tetrabarButton = new Button();
    final Button btnLoadOBOFile = new Button();
    final TextField lineNumberTxt = new TextField();
    final Button btnGo = new Button();

    final Button btnOBOForward = new Button();
    final Button btnOBOBackward = new Button();
    final TextField fieldOBOStep = new TextField();
    final Button oboCBoxColor = new Button();

    final CheckBox proverCheckBox = new CheckBox();
    final TextField offsetTextField = new TextField();
    final Button holeFinderButton = new Button();

    final Button loadCoverButton = new Button();

    final CheckBox autoFillerCheckBox = new CheckBox();
    final TextField labelMainWindow = new TextField();
    final Button coverBtn = new Button();
    final Button loadHolesBtn = new Button();
//    final Button halfTripleBtn = new Button();
//    final Button unstableBtn = new Button();
//    final Button cornerBtn = new Button();
    final Button covRectsColorBox = new Button();
    Color coverColor = Color.BLACK;

    final CheckBox compareCheckBox = new CheckBox();
    final Button saveV3Btn = new Button();

    final Button coverInfoBtn = new Button();

    final ComboBox<String> keepPolys = new ComboBox<>();
    final CheckBox coverColorCycle = new CheckBox();

    // when using 2 polygons on the cover, you need to keep track of the most recently clicked one
    Optional<Tuple2<ConvexPolygon, Color>> secondPoly = Optional.empty();

    final CheckBox boundsCheckBox = new CheckBox(); // where to put this in the menu?

    ExecutorService executorService;

    // Full image redraws are expensive and can be requested repeatedly by zoom,
    // toggles, and cover/vary updates. This pair lets renderRegions discard a
    // stale redraw before it overwrites a newer JavaFX image.
    private volatile Future<?> renderFuture;
    private final AtomicLong renderGeneration = new AtomicLong();
    QueryStage queryStage;

    // the Boyan Menu
    final Button autoVaryBtn = new Button();
    final Button polyVaryBtn = new Button();
    final Button varyLBtn = new Button();

    // Zhao Yu Li, May 06, 2025.
    // New MiddleVaryL button.
    final Button middleVaryLBtn = new Button();

    // Zhao Yu Li, Jul 17, 2025.
    final Button cycleVaryButton = new Button();
    CycleVaryWindow cycleVaryWindow = null;

    // Fields for the AutoPolyVary button
    final TextField lineStartField = new TextField();
    final TextField lineStepField = new TextField(); // 2024-05-06 Step interval for auto
    final TextField lineEndField = new TextField();
    final Button autoPolyVaryBtn = new Button();

    final Button superPolyVaryBtn = new Button();
    final CheckBox superAutoCb = new CheckBox();

    final BoyanMenu boyanMenu = new BoyanMenu(cycleVaryButton, middleVaryLBtn, polyVaryBtn, varyLBtn, autoPolyVaryBtn, lineStartField, lineStepField, lineEndField, superPolyVaryBtn, superAutoCb, TipOpenDelay, TipCloseDelay);

    final Button smallCoverButton = new Button("LiCover");
    final SmallCoverWindow smallCoverWindow;
    final CoverWindow coverWindow;
    final StablesWindow stablesWindow;

    VaryWindowL varyWindow =  null;
    VaryWindowL middleVaryWindow = null;
    AutoPolyVaryLoad autoPolyVaryWindow = null;
    SuperPolyVaryLoad superPolyVaryWindow = null;

    //Suryansh Aug 11 2025
    private final String versionNumber; 
    final Button updateButton = new Button("Check for Updates"); 

    // Zhao Yu Li, Jul 7, 2025.
    // Pattern calculator
    final Button patternCalculatorBtn = new Button();

    public Viewer(final Stage primaryStage, final String version, final ExecutorService executor,
                  final ConnectionPool pool, final String dbName) {
        Viewer.dbname=dbName;
        this.versionNumber = version;

        try {
            if (!Files.exists(Paths.get(tmpDir))) {
                Files.createDirectory(Paths.get(tmpDir));
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        // This gets passed in from the outside world
        mainWindow = primaryStage;
        this.pool = pool;

        final String windowTitle = String.format("Billiards Everything %s (%s)", version, dbName);

        queryStage = new QueryStage(windowTitle, pool, this);

        coverWindow = new CoverWindow(
                String.format("Cover %s", version), pool,
                labelMainWindow, () -> loadCover("cover", executor), this);

        smallCoverWindow = new SmallCoverWindow(
                String.format("Small Cover %s", version), pool,
                () -> loadCover("small_cover", executor, true), coverWindow, smallCoverAreas);

        stablesWindow = new StablesWindow(coverWindow);

//        final CoverWindow2 coverWindow2 = new CoverWindow2(
//                String.format("Cover %s", version), pool,
//                labelMainWindow, () -> loadCover("cover2", executor));
//        final CoverWindow3 coverWindow3 = new CoverWindow3(
//                String.format("Cover %s", version), pool,
//                labelMainWindow, () -> loadCover("cover2", executor));
//        final CoverWindow4 coverWindow4 = new CoverWindow4(
//                String.format("Cover %s", version), pool,
//                labelMainWindow, () -> loadCover("cover2", executor));

        executorService = executor;

        Utils.setupCustomTooltipBehavior((int) (TipOpenDelay * 1000), (int) (TipCloseDelay * 1000), 200);

        for (int i=0; i < 3; i++) {
            plusButtons[i].setSpacing(10);
            minusButtons[i].setSpacing(10);
            plusButtons[i].setAlignment(Pos.CENTER);
            minusButtons[i].setAlignment(Pos.CENTER);
            btnsVBox.getChildren().add(plusButtons[i]);
            btnsVBox.getChildren().add(minusButtons[i]);
        }
        btnsVBox.setPrefHeight(228);
        btnsVBox.setPadding(new Insets(10, 0, 10, 10));
        btnsVBox.setSpacing(10);

        plusButtonsBiMap.addAll(Arrays.asList(new HashBiMap<>(), new HashBiMap<>(), new HashBiMap<>()));
        minusButtonsBiMap.addAll(Arrays.asList(new HashBiMap<>(), new HashBiMap<>(), new HashBiMap<>()));


        iterationStart.setPrefColumnCount(8);
        iterationStart.setTooltip(Utils.toolTip("See instructions for how to use 'Load File'. This is"
                + " the iteration start when you load a file containing iterations"));
        iterationStart.setText("0");
        iterationStart.setPrefWidth(30);
        iterationStart.setStyle(textBoxColor);

        iterationEnd.setPrefColumnCount(8);
        iterationEnd.setTooltip(Utils.toolTip("See instructions for how to use 'Load File'. This is"
                + " the iteration end when you load a file containing iterations"));
        iterationEnd.setText("0");
        iterationEnd.setPrefWidth(40);
        iterationEnd.setStyle(textBoxColor);

        // Suryansh Aug 11 2025
        updateButton.setOnAction(e -> {
            new Thread(() -> {
                try {
                    Updater.checkForUpdates(versionNumber);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        // Zhao Yu Li, May 27, 2025.
        // Add the pattern lookup button and the polygon intersection pattern to the iteration window
        lookupButton.setText("Lookup Patterns");
        Utils.colorButton(lookupButton, Color.SKYBLUE, clickColor);
        lookupButton.setOnAction(event -> new PatternLookupWindow(
                PatternLookupWindow.codeNumbersToString(currentCodeNumbers)
        ));

        intersectCheckBox.setSelected(false);
        intersectCheckBox.setText("Intersect Polygon");
        intersectCheckBox.setOnAction(event -> {
            intersectPolygonButton.setDisable(!intersectCheckBox.isSelected());
        });

        intersectPolygonButton.setText("Polygon");
        Utils.colorButton(intersectPolygonButton, Color.SKYBLUE, clickColor);
        intersectPolygonButton.setDisable(!intersectCheckBox.isSelected());
        intersectPolygonButton.setOnAction(event -> iterationPolyWindow.show());

        intersectionLimitLabel.setText("Limit:");
        intersectionLimitLabel.setTooltip(Utils.toolTip(
                "Limit the number of code sequences to draw and put into the cover. This value is only used if " +
                        "the \"Intersect Polygon\" checkbox is checked. Leave the field blank if you want to draw" +
                        "and put all code sequences into the cover."
        ));

        intersectionLimitTextField.setPrefColumnCount(3);

        iterationToolsHBox.getChildren().addAll(lookupButton, intersectCheckBox, intersectPolygonButton, intersectionLimitLabel, intersectionLimitTextField);
        iterationToolsHBox.setSpacing(10);

        increaseBox1.setText("Add 2");
        Utils.colorButton(increaseBox1, Color.SKYBLUE, clickColor);
        increaseBox1.setOnAction(event -> increase(box1, pool));

        decreaseBox1.setText("Subtract 2");
        Utils.colorButton(decreaseBox1, Color.SKYBLUE, clickColor);
        decreaseBox1.setOnAction(event -> decrease(box1, pool));

        increaseBox2.setText("Add 2");
        Utils.colorButton(increaseBox2, Color.SKYBLUE, clickColor);
        increaseBox2.setOnAction(event -> increase(box2, pool));

        decreaseBox2.setText("Subtract 2");
        Utils.colorButton(decreaseBox2, Color.SKYBLUE, clickColor);
        decreaseBox2.setOnAction(event -> decrease(box2, pool));

        addSubtractBox3.setText("Add/Subtract 2");
        Utils.colorButton(addSubtractBox3, Color.SKYBLUE, clickColor);
        addSubtractBox3.setOnAction(event -> addSubtract(box3, pool));

        addSubtractReverseBox3.setText("Add/Subtract -2");
        Utils.colorButton(addSubtractReverseBox3, Color.SKYBLUE, clickColor);
        addSubtractReverseBox3.setOnAction(event -> addSubtractReverse(box3, pool));

        manualIncrementHBox.getChildren().addAll(box1, increaseBox1, decreaseBox1, box2,
                increaseBox2, decreaseBox2, box3, addSubtractBox3,
                addSubtractReverseBox3);
        manualIncrementHBox.setSpacing(10);

        // First Pattern
        firstPatternLabel.setText("First Pattern:");
        firstPatternTextField.setPrefColumnCount(24);

        firstPatternIterationsLabel.setText("Iterations:");
        firstPatternIterationsTextField.setText("0");
        firstPatternIterationsTextField.setPrefColumnCount(6);

        firstPatternIncrementLabel.setText("Increment:");
        firstPatternIncrementTextField.setText("2");
        firstPatternIncrementTextField.setPrefColumnCount(6);

        squareCodeSequenceField.setPromptText("stable");
        squareMagnificationField.setPromptText("mag");
        squareMagnificationField.setPrefWidth(60);//george set 40 instead of 60
        drawSquareButton.setText("draw");
        Utils.colorButton(drawSquareButton, Color.SKYBLUE, Color.GOLD);
        drawSquareButton.setOnAction(event -> {
            ArrayList<Double> xy = boyanMenu.getRadianCoord();
            double x = xy.get(0);
            double y = xy.get(1);
            int mag = Integer.parseInt(squareMagnificationField.getText());
            Rectangle square = CoverStuff.calculateRectangle(x, y, mag);

            String line = squareCodeSequenceField.getText().trim();

            //final String[] comps = StringUtils.split(line, ',');

            final String codeString = line.trim();
            //final String unstableString = comps[1].trim();

            final IntList codeList = Utils.splitString(codeString).get();
            //final IntList unstableList = Utils.splitString(unstableString).get();

            final ClassifiedCodeSequence codeSequence = ClassifiedCodeSequence.create(codeList).get();
            //final ClassifiedCodeSequence unstable = ClassifiedCodeSequence.create(unstableList).get();
            if (ClassifiedCodeSequence.isStableCodeType(codeSequence.codeType)) {
                //final HalfTriple half_triple = new HalfTriple(stableNeg, unstable);
                coverRects.put(square, codeSequence);
                coverRects.put(square, Color.BLACK);
                renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
                //renderRect(square, (WritableImage) regionsImageView.getImage(), Color.BLACK, Color.FIREBRICK);
            } else {
                final IntList fakeList = Utils.splitString("1 1 1").get();
                final ClassifiedCodeSequence fakeSequence = ClassifiedCodeSequence.create(fakeList).get();
                final HalfTriple unstable = new HalfTriple(fakeSequence, codeSequence);
                coverRects.put(square, unstable);
                coverRects.put(square, Color.BLACK);
                renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
            }
        });
        //drawSquareHBox.getChildren().addAll(squareCodeSequenceField, squareMagnificationField, drawSquareButton);
        drawSquareHBox.setSpacing(10);

        // Second Pattern
        secondPatternLabel.setText("Second Pattern:");
        secondPatternTextField.setPrefColumnCount(24);

        secondPatternIterationsLabel.setText("Iterations:");
        secondPatternIterationsTextField.setText("0");
        secondPatternIterationsTextField.setPrefColumnCount(6);

        secondPatternIncrementLabel.setText("Increment:");
        secondPatternIncrementTextField.setText("2");
        secondPatternIncrementTextField.setPrefColumnCount(6);

        // Third Pattern
        thirdPatternLabel.setText("Third Pattern:");
        thirdPatternTextField.setPrefColumnCount(24);

        thirdPatternIterationsLabel.setText("Iterations:");
        thirdPatternIterationsTextField.setText("0");
        thirdPatternIterationsTextField.setPrefColumnCount(6);

        thirdPatternIncrementLabel.setText("Increment:");
        thirdPatternIncrementTextField.setText("2");
        thirdPatternIncrementTextField.setPrefColumnCount(6);

        // Fourth Pattern
        fourthPatternLabel.setText("Fourth Pattern:");
        fourthPatternTextField.setPrefColumnCount(24);

        fourthPatternIterationsLabel.setText("Iterations:");
        fourthPatternIterationsTextField.setText("0");
        fourthPatternIterationsTextField.setPrefColumnCount(6);

        fourthPatternIncrementLabel.setText("Increment:");
        fourthPatternIncrementTextField.setText("2");
        fourthPatternIncrementTextField.setPrefColumnCount(6);

        // Add/Subtract Iteration Pattern
        addSubtractPatternLabel.setText("Add/Subtract Pattern:");
        addSubtractPatternTextField.setPrefColumnCount(24);

        addSubtractPatternIterationsLabel.setText("Iterations:");
        addSubtractPatternIterationsTextField.setText("0");
        addSubtractPatternIterationsTextField.setPrefColumnCount(6);

        addSubtractPatternIncrementLabel.setText("Increment:");
        addSubtractPatternIncrementTextField.setText("2");
        addSubtractPatternIncrementTextField.setPrefColumnCount(6);

        // Zhao Yu Li, May 01, 2025.
        // Add/Subtract in iterations
        // Positive index means adding the increment; negative index means subtracting the increment
        // Updated May 29, 2025.
        // New polygon intersect feature: can optionally choose to check whether codes produced from iterations
        // intersect with the specified polygon or not. If they do intersect, draw the code sequence and add it to the
        // cover.
        addSubtractIterationsButton.setText("Calculate Add/Subtract Iterations");
        Utils.colorButton(addSubtractIterationsButton, Color.SKYBLUE, clickColor);
        addSubtractIterationsButton.setOnAction(event -> {
            int size = 0;
            for (MutableIntList list : currentCodeNumbers) {
                if (!list.isEmpty()) {
                    size += 1;
                }
            }

            final int size1 = addSubtractPatternTextField.getText().trim().split(",").length;
            final int size_lr = leftrightsTextArea.getText().length();

            // the 1st box should have input of the correct size, else what's the point of running it?
            if ((size1 != size) && (size_lr == 0)) {
                final Alert alert = new Alert(AlertType.ERROR);

                alert.setTitle("Iteration");
                alert.setHeaderText("Invalid Input");
                alert.setContentText("Please check the input");
                alert.showAndWait();
                return;
            }

            Optional<ConvexPolygon> polygon = iterationPolyWindow.getPolygon();

            if (intersectCheckBox.isSelected() && !polygon.isPresent()) return;

            int limit = -1;

            if (intersectCheckBox.isSelected() && !intersectionLimitTextField.getText().trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(intersectionLimitTextField.getText());

                    if (limit < 0) {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Iteration");
                        alert.setHeaderText("Invalid Input");
                        alert.setContentText("Please enter an integer greater than 0 for the limit");
                        alert.showAndWait();
                        return;
                    }
                } catch (final Exception e) {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Iteration");
                    alert.setHeaderText("Invalid Input");
                    alert.setContentText("Please enter an integer for the limit");
                    alert.showAndWait();
                    return;
                }
            }

            ArrayList<ArrayList<Storage>> storages = new ArrayList<>();
            ArrayList<Array<ClassifiedCodeSequence>> codeSequences = new ArrayList<>();

            // Zhao Yu Li, May 22, 2025.
            // Used to add the code sequence - iteration pattern pair to the database
            final StringBuilder patternString = new StringBuilder();

            final String[] pat1 = addSubtractPatternTextField.getText().trim().split(",");

            // Zhao Yu Li, May 22, 2025.
            // Used to add the code sequence - iteration pattern pair to the database
            // Updated May 23, 2025.
            // Use the current code numbers as-is instead of using the normalized form.
            final Tuple2<String, String> codeSeqAndOEString = getCodeSeqAndOEString();
            final String codeSeqString = codeSeqAndOEString._1;

            for (int i = 0; i < size; i++) {
                final MutableIntList workingNumbers =
                        IntArrayList.newWithNValues(currentCodeNumbers[i].size(), 0);

                patternString.append(pat1[i].trim()).append(",");

                final String[] vectors = {Utils.ifGet(pat1, i)};

                final int[] starts = new int[4];
                Arrays.fill(starts, 0);

                final int[] ends = {Integer.parseInt(addSubtractPatternIterationsTextField.getText().trim())};

                final int[] steps = {Integer.parseInt(addSubtractPatternIncrementTextField.getText().trim())};

                Tuple2<Array<ClassifiedCodeSequence>, ArrayList<Storage>> res =
                        iterateActionWithPolyIntersect(workingNumbers, vectors, starts, ends, steps, i, false, executor);
                codeSequences.add(res._1);
                storages.add(res._2);
            }

            patternString.deleteCharAt(patternString.length() - 1);

            int storageIdx = 0;
            int codeSequenceIdx = 0;
            int count = 0;

            if (intersectCheckBox.isSelected() && limit == -1) limit = codeSequences.get(0).size();

            if (size1 == 3) {
                while (codeSequenceIdx < codeSequences.get(0).size()) {
                    String tripleString = codeSequences.get(0).get(codeSequenceIdx).toString() + ", "
                            + codeSequences.get(1).get(codeSequenceIdx).toString() + ", "
                            + codeSequences.get(2).get(codeSequenceIdx).toString();

                    if (storageIdx < storages.get(0).size()
                            && codeSequences.get(0).get(codeSequenceIdx).toString().equals(storages.get(0).get(storageIdx).toString())
                            && codeSequences.get(1).get(codeSequenceIdx).toString().equals(storages.get(1).get(storageIdx).toString())
                            && codeSequences.get(2).get(codeSequenceIdx).toString().equals(storages.get(2).get(storageIdx).toString())
                    ) {
                        System.out.println(tripleString);

                        if (intersectCheckBox.isSelected()) {
                            if (storages.get(0).get(storageIdx).intersects(polygon.get())
                                    && storages.get(1).get(storageIdx).intersects(polygon.get())
                                    && storages.get(2).get(storageIdx).intersects(polygon.get())
                                    && count++ < limit
                            ) {
                                final int index = cycle.get();
                                final Color color = comboBoxColors.get(index);
                                addToOnScreenSequences(storages.get(0).get(storageIdx), color);
                                addToOnScreenSequences(storages.get(1).get(storageIdx), color);
                                addToOnScreenSequences(storages.get(2).get(storageIdx), color);

                                if (storages.get(0).get(storageIdx).classCodeSeq.stable
                                        && !storages.get(1).get(storageIdx).classCodeSeq.stable
                                        && storages.get(2).get(storageIdx).classCodeSeq.stable)
                                    coverWindow.appendTriplesInfo(tripleString + "  // " + patternString);
                            }
                        }

                        // If we don't choose to intersect with a polygon, then the same as before: draw, but don't add
                        // to cover
                        if (!intersectCheckBox.isSelected()) {
                            final int index = cycle.get();
                            final Color color = comboBoxColors.get(index);
                            addToOnScreenSequences(storages.get(0).get(storageIdx), color);
                            addToOnScreenSequences(storages.get(1).get(storageIdx), color);
                            addToOnScreenSequences(storages.get(2).get(storageIdx), color);
                        }

                        storageIdx++;
                    } else {
                        System.out.println("// empty set " + tripleString);
                    }

                    codeSequenceIdx++;
                }
            } else {
                while (codeSequenceIdx < codeSequences.get(0).size()) {
                    if (storageIdx < storages.get(0).size()
                        && codeSequences.get(0).get(codeSequenceIdx).toString().equals(storages.get(0).get(storageIdx).toString())) {
                        System.out.println(codeSequences.get(0).get(codeSequenceIdx).toString());

                        if (intersectCheckBox.isSelected()
                                && storages.get(0).get(storageIdx).intersects(polygon.get())
                                && count++ < limit
                        ) {
                            final int index = cycle.get();
                            final Color color = comboBoxColors.get(index);
                            addToOnScreenSequences(storages.get(0).get(storageIdx), color);

                            if (storages.get(0).get(storageIdx).classCodeSeq.stable) {
                                coverWindow.appendStablesInfo(
                                        getCoverCodeString(storages.get(0).get(storageIdx)) + "  // " + patternString
                                );
                            }
                        }

                        // Zhao Yu Li, May 29, 2025.
                        // If we don't choose to intersect with a polygon, then the same as before: draw, but don't add
                        // to cover
                        if (!intersectCheckBox.isSelected()) {
                            final int index = cycle.get();
                            final Color color = comboBoxColors.get(index);
                            addToOnScreenSequences(storages.get(0).get(storageIdx), color);
                        }

                        storageIdx++;
                    } else {
                        System.out.println("// empty set " + codeSequences.get(0).get(codeSequenceIdx));
                    }

                    codeSequenceIdx++;
                }
            }

            Database.saveIterationPatternToDatabase(codeSeqString, codeSeqAndOEString._2, patternString.toString(), "garbage");

            renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
        });

        //LeftRights
        leftrightsLabel.setText("Left rights:");
        leftrightsLabel1.setText("Left rights 1:");
        leftrightsLabel2.setText("Left rights 2:");

        sequence.setText("Code Sequence:");
        leftrightsTextArea.setEditable(true);
        leftrightsTextArea.setPrefColumnCount(14);
        leftrightsTextArea.setPrefRowCount(10);
        leftrightsTextArea.setWrapText(true);
        leftrightsTextArea1.setEditable(true);
        leftrightsTextArea1.setPrefColumnCount(14);
        leftrightsTextArea1.setPrefRowCount(10);
        leftrightsTextArea1.setWrapText(true);
        leftrightsTextArea2.setEditable(true);
        leftrightsTextArea2.setPrefColumnCount(14);
        leftrightsTextArea2.setPrefRowCount(10);
        leftrightsTextArea2.setWrapText(true);
        nolrRdoBtn.setText("No Left Rights");
        nolrRdoBtn.setSelected(true);
        showlrRdoBtn.setText("Show Left Rights");
        uselrRdoBtn.setText("Use Left Rights without test");
        uselrTestBtn.setText("Use Left Rights with test ");
        uselrRdoBtn.setTooltip(Utils.toolTip("only functioning with the stable Code and WARNING it is better to do a manual check for it"));
        uselrTestBtn.setTooltip(Utils.toolTip("only functioning with the stable Code"));
        Button checkButton= new Button();
        checkButton.setText("manual check");
        checkButton.setStyle(textBoxColor);
        checkButton.setTooltip(Utils.toolTip( "use no left rights as the standard and check the answer after using the without or with test and put the number of the iterations you want to check in the iteration box"));

        nolrRdoBtn.setStyle(textBoxColor);
        showlrRdoBtn.setStyle(textBoxColor);
        uselrRdoBtn.setStyle(textBoxColor);
        uselrTestBtn.setStyle(textBoxColor);


        nolrRdoBtn.setToggleGroup(lrGroup);
        showlrRdoBtn.setToggleGroup(lrGroup);
        uselrRdoBtn.setToggleGroup(lrGroup);
        uselrTestBtn.setToggleGroup(lrGroup);


        expandoPatternLabel.setText("Expando Patterns:");
        expandoPatternLabel.setTooltip(Utils.toolTip("example: 1 1 2 2 A 1 1 2 2 B 1 1 2 2 A "));
        expandoIterationsLabel.setText("Iterations:");
        expandoElementsLabel.setText("Elements:");
        expandoElementsLabel.setTooltip(Utils.toolTip("example:'2 2,2 2' with A being 2 2 and B being 2 2"));

        expandoCalculateButton.setText("Calculate expando Iteration");
        expandoCalculateButton.setTooltip(Utils.toolTip("Input the Expando Patterns with the pattern substituted with the capital letters and input the pattern in the elements seperated by comma."));

        expandoIterations.setText("0");

        iterationsGridPane.setPadding(new Insets(10));
        iterationsGridPane.setHgap(10);
        iterationsGridPane.setVgap(10);

        // First Pattern
        iterationsGridPane.addRow(0, firstPatternLabel, firstPatternTextField,
                firstPatternIterationsLabel, firstPatternIterationsTextField,
                firstPatternIncrementLabel, firstPatternIncrementTextField);

        // Second Pattern
        iterationsGridPane.addRow(1, secondPatternLabel, secondPatternTextField,
                secondPatternIterationsLabel, secondPatternIterationsTextField,
                secondPatternIncrementLabel, secondPatternIncrementTextField);

        // Zhao Yu Li, May 01, 2025.
        // Add/Subtract Pattern
        iterationsGridPane.addRow(2, addSubtractPatternLabel, addSubtractPatternTextField,
                addSubtractPatternIterationsLabel, addSubtractPatternIterationsTextField,
                addSubtractPatternIncrementLabel, addSubtractPatternIncrementTextField,
                addSubtractIterationsButton);

        // Third Pattern
//        iterationsGridPane.addRow(2, thirdPatternLabel, thirdPatternTextField,
//                                  thirdPatternIterationsLabel, thirdPatternIterationsTextField,
//                                  thirdPatternIncrementLabel, thirdPatternIncrementTextField);

        // Fourth Pattern
        /*iterationsGridPane.addRow(3, fourthPatternLabel, fourthPatternTextField,
                                  fourthPatternIterationsLabel, fourthPatternIterationsTextField,
                                  fourthPatternIncrementLabel, fourthPatternIncrementTextField);*/
        expandoButton.setText("Calculate expando");

        Utils.colorButton(expandoButton, Color.SKYBLUE, clickColor);
        Utils.colorButton(expandoCalculateButton, Color.SKYBLUE, clickColor);



        //iterationsGridPane.addRow(4, leftrightsLabel,leftrightsTextArea,sequence,expandoCodeSequce,expandoButton);
        //iterationsGridPane.addRow(5, leftrightsLabel1,leftrightsTextArea1,leftrightsLabel2,leftrightsTextArea2);
        iterationsGridPane.addRow(6, expandoPatternLabel,expandoPatten,expandoIterationsLabel,expandoIterations,expandoElementsLabel,expandoElements,expandoCalculateButton);



        iterationsCalculateButton.setText("Calculate Iterations");
        Utils.colorButton(iterationsCalculateButton, Color.SKYBLUE, clickColor);
        expandoButton.setOnAction(event -> {
            String lr = leftrightsTextArea.getText();
            final Optional<ImmutableIntList> optional = Utils.splitString(expandoCodeSequce.getText());
            MutableIntList codeSeq = IntArrayList.newList(optional.get());

            final Either<InvalidCodeSequence, ClassifiedCodeSequence> either = ClassifiedCodeSequence.create(codeSeq);
            final ClassifiedCodeSequence classCodeSeq = either.get();

            final Optional<ImmutableIntList> optional_base = Utils.splitString(txtCodeSequence2.getText());
            MutableIntList codeSeq_base = IntArrayList.newList(optional_base.get());

            final Either<InvalidCodeSequence, ClassifiedCodeSequence> either_base = ClassifiedCodeSequence.create(codeSeq_base);
            final ClassifiedCodeSequence classCodeSeq_base = either_base.get();
            Wrapper.loadPictureLR(classCodeSeq_base, classCodeSeq, pool, lr);
        });
        //for expando calculation
        expandoCalculateButton.setOnAction(event -> {

            final int iter = Integer.parseInt(expandoIterations.getText().trim());
            //String lr1= leftrightsTextArea1.getText().trim();
            //String lr2= leftrightsTextArea2.getText().trim();

            //String[] lrs= new String[iter];
            //lrs[0]=lr2;// initialize the first element of the leftright array

            //Label 1
            Array<ClassifiedCodeSequence> codes = getMultipleExpandos(iter, expandoPatten.getText(), expandoElements.getText());

            //get the pattern but subtracting the first lr from second lr.
            //Label 2
            /*
            String pattern = subtractLeftRight(lr1,lr2);
            // add the pattern to  each left-rights
            for (int i=0; i < iter - 1; ++i) {
                //Label 3
                lrs[i + 1] = addLeftRight(lrs[i], pattern);
            }
            */

            // if you want to print all the left rights calculated using the pattern uncomment below
            //System.out.println(lrs);

            //base does not matter here as long as it is in the sqlite
            final Optional<ImmutableIntList> optional_base = Utils.splitString(txtCodeSequence2.getText());
            MutableIntList codeSeq_base = IntArrayList.newList(optional_base.get());

            final Either<InvalidCodeSequence, ClassifiedCodeSequence> either_base = ClassifiedCodeSequence.create(codeSeq_base);
            final ClassifiedCodeSequence classCodeSeq_base = either_base.get();
            boolean is_alert = false;
            // calculate the information using each code sequeces and left rights
            //Array<ClassifiedCodeSequence> LegalCodes = new Array<ClassifiedCodeSequence>();
            //Optional<Picture> isLegal;
            if (uselrRdoBtn.isSelected()){// without test
                String pattern = null;
                String prev_lr = null;
                for(int i = 0; i < codes.length();++i) {
                    if (prev_lr != null && pattern != null && isleftRightLegal(pattern)) {
                        String guess_lr = addLeftRight(pattern, prev_lr);
                        Optional<Storage> opt = Database.loadStorageUseLR(guess_lr,  classCodeSeq_base, codes.get(i), pool);
                        if (!opt.isPresent()) {
                            Optional<Info> opt_info = Wrapper.loadInfo(codes.get(i), pool);
                            if (opt_info.isPresent()) {
                                Info info = opt_info.get();
                                System.out.println(codes.get(i) + " left right changed");
                                if (prev_lr != null) {
                                    pattern = subtractLeftRight(prev_lr, info.leftRights);
                                }
                                prev_lr = info.leftRights;
                            }
                            else {
                                System.out.println(codes.get(i) + " empty set");
                                prev_lr = null;
                                pattern = null;
                            }
                        }
                        else {
                            System.out.println(codes.get(i));
                            prev_lr = guess_lr;
                        }
                    }
                    else {
                        Optional<Info> opt_info = Wrapper.loadInfo(codes.get(i), pool);
                        if (opt_info.isPresent()) {
                            Info info = opt_info.get();
                            System.out.println(codes.get(i) + " using the normal way to calculate");
                            if (prev_lr != null) {
                                pattern = subtractLeftRight(prev_lr, info.leftRights);
                            }
                            prev_lr = info.leftRights;
                        }
                        else {
                            System.out.println(codes.get(i) + " empty set");
                            prev_lr = null;
                            pattern = null;
                        }
                    }

                }
            }
            else if (uselrTestBtn.isSelected()){
                for(int i = 0; i < codes.length();++i)
                {
                    Optional<Info> opt_info = Wrapper.loadInfo(codes.get(i), pool);
                    if (opt_info.isPresent()) {
                        System.out.println(codes.get(i) + " using the normal way to calculate");
                    }
                    else {
                        System.out.println(codes.get(i) + " empty set");
                    }
                }
            }
            else{
                is_alert=true;
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setHeaderText("Invalid choice");
                alert.setContentText("Please check the Use Left Rights with test or without test");
                alert.showAndWait();
            }
            if (!is_alert){
                // drawing the regions
                final Task<Array<Storage>> task;

                // Label 5
                final ExecutorService drawExecutor = Executors.newFixedThreadPool(Utils.numThreads);
                task = new DrawPictureTask(codes, pool, drawExecutor, false, false);
                task.setOnSucceeded(e -> {

                    final Array<Storage> storages;
                    try {
                        storages = task.get();
                    } catch (InterruptedException | ExecutionException exception) {
                        throw new RuntimeException(exception);
                    }

                    storages.forEach(storage -> {
                        final int index = cycle.get();
                        final Color color = comboBoxColors.get(index);
                        addToOnScreenSequences(storage, color);
                    });
                    Utils.printToFile("iterations.txt", storages);
                    try {
                        synchronize();
                    } catch (final NullPointerException exception) {
                        // this is when were iterating from a file, and the iterations window isn't open
                    }
                    // Label 6
                    renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
                    Utils.shutdownExecutorAsync(drawExecutor);
                });
                task.setOnCancelled(e -> {
                    Utils.shutdownExecutorAsync(drawExecutor);
                });
                task.setOnFailed(e -> {
                    Utils.shutdownExecutorAsync(drawExecutor);
                    throw new RuntimeException(task.getException());
                });

                Utils.runAndWait(task);
            }


            // Label 7
            // uncommment below to print
            //for(int k = 0;k < codes.size();k++)
            //{
            // System.out.println(codes.get(k).codeSequence.toString());
            //}
        });
        ArrayList<Array<ClassifiedCodeSequence>> codes2 = new ArrayList<>();

        // Zhao Yu Li, updated May 29, 2025.
        // New polygon intersect feature: can optionally choose to check whether codes produced from iterations
        // intersect with the specified polygon or not. If they do intersect, draw the code sequence and add it to the
        // cover.
        iterationsCalculateButton.setOnAction(event -> {
            codes2.clear();
            int size = 0;
            for (MutableIntList list : currentCodeNumbers) {
                if (!list.isEmpty()) {
                    size += 1;
                }
            }

            final int size1 = firstPatternTextField.getText().trim().split(",").length;
            final int size2 = secondPatternTextField.getText().trim().split(",").length;
            final int size3 = thirdPatternTextField.getText().trim().split(",").length;
            final int size4 = fourthPatternTextField.getText().trim().split(",").length;
            final int size_lr = leftrightsTextArea.getText().length();

            // the 1st box should have input of the correct size, else what's the point of running it?
            // the 2nd, 3rd and 4th boxes should be correct size or empty
            if (((size1 != size) || (size2 != size && !secondPatternTextField.getText().trim().isEmpty())
                    || (size3 != size && !thirdPatternTextField.getText().trim().isEmpty())
                    || (size4 != size && !fourthPatternTextField.getText().trim().isEmpty())
                    || (size != 1 && size != 3)) && (size_lr == 0)) {
                final Alert alert = new Alert(AlertType.ERROR);

                alert.setTitle("Iteration");
                alert.setHeaderText("Invalid Input");
                alert.setContentText("Please check the input");
                alert.showAndWait();
                return;
            }
            //System.out.println(size+"\n");
            // Shiyu, get left right string

            //String lr = leftrightsTextArea.getText();
            //System.out.println("asdsadas"+lr+"x");

            Optional<ConvexPolygon> polygon = iterationPolyWindow.getPolygon();

            if (intersectCheckBox.isSelected() && !polygon.isPresent()) return;

            if (intersectCheckBox.isSelected() && !polygon.isPresent()) return;

            int limit = -1;

            if (intersectCheckBox.isSelected() && !intersectionLimitTextField.getText().trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(intersectionLimitTextField.getText());

                    if (limit < 0) {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Iteration");
                        alert.setHeaderText("Invalid Input");
                        alert.setContentText("Please enter an integer greater than 0 for the limit");
                        alert.showAndWait();
                        return;
                    }
                } catch (final Exception e) {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Iteration");
                    alert.setHeaderText("Invalid Input");
                    alert.setContentText("Please enter an integer for the limit");
                    alert.showAndWait();
                    return;
                }
            }

            ArrayList<ArrayList<Storage>> storages = new ArrayList<>();
            ArrayList<Array<ClassifiedCodeSequence>> codeSequences = new ArrayList<>();

            // Zhao Yu Li, May 22, 2025.
            // Used to add the code sequence - iteration pattern pair to the database
            final StringBuilder patternString1 = new StringBuilder();
            final StringBuilder patternString2 = new StringBuilder();
            final StringBuilder patternString3 = new StringBuilder();
            final StringBuilder patternString4 = new StringBuilder();

            final String[] pat1 = firstPatternTextField.getText().trim().split(",");
            final String[] pat2 = secondPatternTextField.getText().trim().split(",");
            final String[] pat3 = thirdPatternTextField.getText().trim().split(",");
            final String[] pat4 = fourthPatternTextField.getText().trim().split(",");

            // Zhao Yu Li, May 22, 2025.
            // Used to add the code sequence - iteration pattern pair to the database
            // Updated May 23, 2025.
            // Use the current code numbers as-is instead of using the normalized form.
            final Tuple2<String, String> codeSeqAndOEString = getCodeSeqAndOEString();
            final String codeSeqString = codeSeqAndOEString._1;

            for (int i = 0; i < size; i++) {
                final MutableIntList workingNumbers =
                        IntArrayList.newWithNValues(currentCodeNumbers[i].size(), 0);

                if (!firstPatternTextField.getText().trim().isEmpty()) patternString1.append(pat1[i].trim()).append(",");
                if (!secondPatternTextField.getText().trim().isEmpty()) patternString2.append(pat2[i].trim()).append(",");
                if (!thirdPatternTextField.getText().trim().isEmpty()) patternString3.append(pat3[i].trim()).append(",");
                if (!fourthPatternTextField.getText().trim().isEmpty()) patternString4.append(pat4[i].trim()).append(",");

                final String[] vectors = {Utils.ifGet(pat1, i), Utils.ifGet(pat2, i),
                        Utils.ifGet(pat3, i), Utils.ifGet(pat4, i)};

                final int[] starts = new int[4];
                Arrays.fill(starts, 0);

                final int[] ends = {Integer.parseInt(firstPatternIterationsTextField.getText().trim()),
                        Integer.parseInt(secondPatternIterationsTextField.getText().trim()),
                        Integer.parseInt(thirdPatternIterationsTextField.getText().trim()),
                        Integer.parseInt(fourthPatternIterationsTextField.getText().trim())};

                final int[] steps = {Integer.parseInt(firstPatternIncrementTextField.getText().trim()),
                        Integer.parseInt(secondPatternIncrementTextField.getText().trim()),
                        Integer.parseInt(thirdPatternIncrementTextField.getText().trim()),
                        Integer.parseInt(fourthPatternIncrementTextField.getText().trim())};
                //System.out.print("workingNumbers1" + workingNumbers + "\n");//workingNumbers[0, 0, 0]
                // Ugly fix

                // Label 8
//                long start = System.currentTimeMillis();
                Tuple2<Array<ClassifiedCodeSequence>, ArrayList<Storage>> res =
                        iterateActionWithPolyIntersect(workingNumbers, vectors, starts, ends, steps, i, false, executor);
                codeSequences.add(res._1);
                codes2.add(res._1);
                storages.add(res._2);
//                long end = System.currentTimeMillis();


                // String[] iterations = Utils.readFromFile("iterations.txt").trim().split("\n");
                /*ConnectionPool newPool = Admin.getConnectionPool(dbName, Utils.numThreads);

                for (ClassifiedCodeSequence code : codes.get(0)){
                    //CodeSequence code= ClassifiedCodeSequence(c)
                    Optional<Info> opt_infoAll_1 = Wrapper.loadInfo(code, newPool);
                    String c = opt_infoAll_1.get().leftRights;
                    Wrapper.deleteFromDatabase(code, newPool);
                    Database.loadStorage(code, newPool);
                    Optional<Info> opt_infoAll_2 = Wrapper.loadInfo(code, newPool);
                    String t = opt_infoAll_2.get().leftRights;
                    if (!t.equals(c)){
                        System.out.println("test failed for "+code);
                        System.out.println("The correct left rights are "+ "\n"+ t+ "\nthe calculated one are "+ "\n"+c);
                        break;
                    }
                }*/

//                System.out.println("Total time: " + Long.toString(end - start) + "ms");
                // Label 9
                //george aug 26,2019 using 1 3 3
                //System.out.print("workingNumbers" + workingNumbers + "\n");//workingNumbers[1, 3, 3]
                //System.out.print("vectors" + vectors + "\n");//vectors[Ljava.lang.String;@3f6ca756
                //System.out.print("starts" + starts + "\n");//starts[I@39fcfd27
                //System.out.print("ends" + ends + "\n");//ends[I@137af616
            }

            if (!patternString1.toString().isEmpty()) {
                patternString1.deleteCharAt(patternString1.length() - 1);
            }

            if (!patternString2.toString().isEmpty()) {
                patternString2.deleteCharAt(patternString2.length() - 1);
            }

            if (!patternString3.toString().isEmpty()) {
                patternString3.deleteCharAt(patternString3.length() - 1);
            }

            if (!patternString4.toString().isEmpty()) {
                patternString4.deleteCharAt(patternString4.length() - 1);
            }

            int storageIdx = 0;
            int codeSequenceIdx = 0;
            int count = 0;

            if (intersectCheckBox.isSelected() && limit == -1) limit = codeSequences.get(0).size();

            // Zhao Yu Li, May 29, 2025.
            if (size1 == 3) {
                while (codeSequenceIdx < codeSequences.get(0).size()) {
                    String tripleString = codeSequences.get(0).get(codeSequenceIdx).toString() + ", "
                            + codeSequences.get(1).get(codeSequenceIdx).toString() + ", "
                            + codeSequences.get(2).get(codeSequenceIdx).toString();

                    if (storageIdx < storages.get(0).size()
                            && codeSequences.get(0).get(codeSequenceIdx).toString().equals(storages.get(0).get(storageIdx).toString())
                            && codeSequences.get(1).get(codeSequenceIdx).toString().equals(storages.get(1).get(storageIdx).toString())
                            && codeSequences.get(2).get(codeSequenceIdx).toString().equals(storages.get(2).get(storageIdx).toString())
                    ) {
                        System.out.println(tripleString);

                        if (intersectCheckBox.isSelected()) {
                            if (storages.get(0).get(storageIdx).intersects(polygon.get())
                                    && storages.get(1).get(storageIdx).intersects(polygon.get())
                                    && storages.get(2).get(storageIdx).intersects(polygon.get())
                                    && count++ < limit
                            ) {
                                final int index = cycle.get();
                                final Color color = comboBoxColors.get(index);
                                addToOnScreenSequences(storages.get(0).get(storageIdx), color);
                                addToOnScreenSequences(storages.get(1).get(storageIdx), color);
                                addToOnScreenSequences(storages.get(2).get(storageIdx), color);

                                if (storages.get(0).get(storageIdx).classCodeSeq.stable
                                        && !storages.get(1).get(storageIdx).classCodeSeq.stable
                                        && storages.get(2).get(storageIdx).classCodeSeq.stable) {
                                    StringBuilder coverString = new StringBuilder(tripleString);
                                    coverString.append("  // ");

                                    if (!patternString1.toString().isEmpty()) {
                                        coverString.append("pat1: ").append(patternString1).append("; ");
                                    }

                                    if (!patternString2.toString().isEmpty()) {
                                        coverString.append("pat2: ").append(patternString2).append("; ");
                                    }

                                    if (!patternString3.toString().isEmpty()) {
                                        coverString.append("pat3: ").append(patternString3).append("; ");
                                    }

                                    if (!patternString4.toString().isEmpty()) {
                                        coverString.append("pat4: ").append(patternString4);
                                    }

                                    coverWindow.appendTriplesInfo(coverString.toString());
                                }
                            }
                        }

                        // If we don't choose to intersect with a polygon, then the same as before: draw, but don't add
                        // to cover
                        if (!intersectCheckBox.isSelected()) {
                            final int index = cycle.get();
                            final Color color = comboBoxColors.get(index);
                            addToOnScreenSequences(storages.get(0).get(storageIdx), color);
                            addToOnScreenSequences(storages.get(1).get(storageIdx), color);
                            addToOnScreenSequences(storages.get(2).get(storageIdx), color);
                        }

                        storageIdx++;
                    } else {
                        System.out.println("// empty set " + tripleString);
                    }

                    codeSequenceIdx++;
                }
            } else {
                while (codeSequenceIdx < codeSequences.get(0).size()) {
                    if (storageIdx < storages.get(0).size()
                            && codeSequences.get(0).get(codeSequenceIdx).toString().equals(storages.get(0).get(storageIdx).toString())) {
                        System.out.println(codeSequences.get(0).get(codeSequenceIdx).toString());

                        if (intersectCheckBox.isSelected()
                                && storages.get(0).get(storageIdx).intersects(polygon.get())
                                && count++ < limit
                        ) {
                            final int index = cycle.get();
                            final Color color = comboBoxColors.get(index);
                            addToOnScreenSequences(storages.get(0).get(storageIdx), color);

                            if (storages.get(0).get(storageIdx).classCodeSeq.stable) {
                                StringBuilder coverString = new StringBuilder(getCoverCodeString(storages.get(0).get(storageIdx)));
                                coverString.append("  // ");

                                if (!patternString1.toString().isEmpty()) {
                                    coverString.append("pat1: ").append(patternString1).append("; ");
                                }

                                if (!patternString2.toString().isEmpty()) {
                                    coverString.append("pat2: ").append(patternString2).append("; ");
                                }

                                if (!patternString3.toString().isEmpty()) {
                                    coverString.append("pat3: ").append(patternString3).append("; ");
                                }

                                if (!patternString4.toString().isEmpty()) {
                                    coverString.append("pat4: ").append(patternString4);
                                }

                                coverWindow.appendStablesInfo(coverString.toString());
                            }
                        }

                        // Zhao Yu Li, May 29, 2025.
                        // If we don't choose to intersect with a polygon, then same as before: draw, but don't add to
                        // cover
                        if (!intersectCheckBox.isSelected()) {
                            final int index = cycle.get();
                            final Color color = comboBoxColors.get(index);
                            addToOnScreenSequences(storages.get(0).get(storageIdx), color);
                        }

                        storageIdx++;
                    } else {
                        System.out.println("// empty set " + codeSequences.get(0).get(codeSequenceIdx));
                    }

                    codeSequenceIdx++;
                }
            }

            renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);

            if (!patternString1.toString().isEmpty()) {
                Database.saveIterationPatternToDatabase(codeSeqString, codeSeqAndOEString._2, patternString1.toString(), "garbage");
            }

            if (!patternString2.toString().isEmpty()) {
                Database.saveIterationPatternToDatabase(codeSeqString, codeSeqAndOEString._2, patternString2.toString(), "garbage");
            }

            if (!patternString3.toString().isEmpty()) {
                Database.saveIterationPatternToDatabase(codeSeqString, codeSeqAndOEString._2, patternString3.toString(), "garbage");
            }

            if (!patternString4.toString().isEmpty()) {
                Database.saveIterationPatternToDatabase(codeSeqString, codeSeqAndOEString._2, patternString4.toString(), "garbage");
            }
        });
        checkButton.setOnAction(event -> {
            int iter = Integer.parseInt(firstPatternIterationsTextField.getText().trim());
            boolean isRight = true;
            ArrayList<ClassifiedCodeSequence> wrongOne = new ArrayList<>();
            if (codes2.isEmpty()){
                System.out.println("empty input");
            }
            else if (iter > codes2.get(0).size()){
                System.out.println("iteration number is too large");
            }
            else{

                for(int i = 0;i < iter; i++){
                    if ((uselrRdoBtn.isSelected()||uselrTestBtn.isSelected()))
                    {
                        ClassifiedCodeSequence code= codes2.get(0).get(i);
                        Optional<Info> opt_infoAll_1 = Wrapper.loadInfo(code, pool);
                        if (opt_infoAll_1.isPresent()){
                            String c = opt_infoAll_1.get().leftRights;
                            Wrapper.deleteFromDatabase(code, pool);
                            //Database.loadStorage(code, newPool);
                            Optional<Info> opt_infoAll_2 = Wrapper.loadInfo(code, pool);
                            if (opt_infoAll_2.isPresent()){
                                String t = opt_infoAll_2.get().leftRights;
                                if (!t.equals(c)){
                                    isRight= false;
                                    wrongOne.add(code);
                                    System.out.println("check failed for "+code);
                                    System.out.println("The correct left rights are "+ "\n"+ t+ "\nthe calculated one are "+ "\n"+c);

                                }
                                else{
                                    System.out.println("manual check is successful for "+code);
                                }
                            }
                            else{
                                System.out.println("check failed for "+code+ " becuase it is empty");
                            }
                        }
                        else{
                            System.out.println("check failed for "+code+ "becuase it is empty");
                        } }
                }
                if (!isRight){
                    for (ClassifiedCodeSequence wrong_code : wrongOne){
                        Wrapper.deleteFromDatabase(wrong_code, pool);
                    }
                }
            }

        });

        codeWindowVBox.setPadding(new Insets(10));
        codeWindowVBox.getChildren().addAll(btnsVBox, iterationToolsHBox, manualIncrementHBox,
                iterationsGridPane, calculateIterationsHBox);
        codeWindowVBox.setSpacing(10);

        calculateIterationsHBox.getChildren().addAll(txtCodeSequence2, btnCalculate2,
                labelCodeWindow, iterationsCalculateButton,
                nolrRdoBtn, showlrRdoBtn, uselrRdoBtn, uselrTestBtn);
        calculateIterationsHBox.setSpacing(10);

        // Zhao Yu Li, Jun 09, 2025.
        // Iterate-to-Limit feature.
        iterateToLimitBtn.setText("LiPattern");
        Utils.colorButton(iterateToLimitBtn, Color.SKYBLUE, clickColor);
        iterateToLimitBtn.setOnAction(event -> {
            // Lazy initialization
            if (iterateToLimitWindow == null) this.iterateToLimitWindow = new IterateToLimitWindow(pool);

            if (iterateToLimitWindow.isShowing()) iterateToLimitWindow.toFront();

            // Get the finish flag from the IterateToLimitWindow.
            AtomicReference<SimpleBooleanProperty> finish = new AtomicReference<>(iterateToLimitWindow.execute());

            // Once the iterate-to-limit task is finish, we draw the codes and add the codes to the cover.
            finish.get().addListener((observable, oldValue, newValue) -> {
                if (oldValue != newValue && newValue) {
                    ArrayList<Tuple3<ArrayList<Storage>, ArrayList<ArrayList<Storage>>, ArrayList<ArrayList<Storage>>>> results = iterateToLimitWindow.getResults();

                    if (results == null) {
                        iterateToLimitWindow.nullifyFinish();
                        finish.set(null);
                        return;
                    }

                    boolean draw = iterateToLimitWindow.getDraw();
                    boolean addToCover = iterateToLimitWindow.getAddToCover();

                    for (Tuple3<ArrayList<Storage>, ArrayList<ArrayList<Storage>>, ArrayList<ArrayList<Storage>>> result : results) {
                        ArrayList<Storage> originalCode = result._1;
                        ArrayList<ArrayList<Storage>> forwardResult = result._2;
                        ArrayList<ArrayList<Storage>> backwardResult = result._3;

                        drawAndAddToCover(draw, addToCover, originalCode);

                        for (ArrayList<Storage> storages : forwardResult) drawAndAddToCover(draw, addToCover, storages);
                        for (ArrayList<Storage> storages : backwardResult) drawAndAddToCover(draw, addToCover, storages);
                    }

                    renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);

                    // Nullify the results and the flags for the next execution.
                    iterateToLimitWindow.nullifyFinish();
                    iterateToLimitWindow.nullifyResult();
                }
            });
        });

        zoomScaleLabel.setText("Zoom Scale:");
        zoomScaleText.setText("2");
        zoomScaleText.setTooltip(Utils.toolTip("The scale that you magnify and demagnify by"));
        zoomScaleText.setPrefWidth(55);
        zoomScaleText.setStyle(textBoxColor);

        backwardSquareButton.setText("Backward");
        backwardSquareButton.setTooltip(Utils.toolTip("Go to the last screen view you were at"));

        Utils.colorButton(backwardSquareButton, Color.SKYBLUE, clickColor);

        backwardSquareButton.setOnAction(event -> {

            viewRectangleBF.backward().ifPresent(rect -> {
                map.setViewRectangle(rect);
                renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
            });
        });

        forwardSquareButton.setText("Forward");
        forwardSquareButton.setTooltip(Utils.toolTip("Go to the next screen view you were at"));
        Utils.colorButton(forwardSquareButton, Color.SKYBLUE, clickColor);

        forwardSquareButton.setOnAction(event -> {

            viewRectangleBF.forward().ifPresent(rect -> {
                map.setViewRectangle(rect);
                renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
            });
        });

        /*
        final Button btnSaveImage = new Button("Save Image");
        btnSaveImage.setOnAction(event
            -> {

                final FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Image");
                final File file = fileChooser.showSaveDialog(mainWindow);

                if (file != null) {
                    final WritableImage image = stackPane.snapshot(null, null);
                    try {
                        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                    } catch (IOException except) {
                        System.out.println(except.getMessage());
                    }
                }
        });
        */

        // Zhao Yu Li, May 19, 2025.
        // Tetrahedron/Bar window.
        tetrabarButton.setText("LiBainT/B");
        tetrabarButton.setTooltip(Utils.toolTip("Creates a tetrahedron (bar) out of each input coordinate, and finds the intersection of the result of Vary3 on all three (two) points."));
        Utils.colorButton(tetrabarButton, Color.LIGHTPINK, clickColor);
        tetrabarButton.setOnAction(event -> {
            Tuple6<List<Tuple2<Double, Double>>, List<Tuple2<Double, Double>>, Integer, Integer, Boolean, Boolean> varyParams = new TetraBar(mainWindow).getVaryParams();

            if (varyParams._3 == -1) return;

            ExecutorService executorService = Executors.newFixedThreadPool(Utils.numThreads);
            queuedVaryTask(varyParams._1, varyParams._2, 0, varyParams._2.size(), executorService, varyParams._3, varyParams._4, varyParams._5, varyParams._6);
        });

        lineNumberTxt.setPromptText("Line");
        lineNumberTxt.setTooltip(Utils.toolTip("The OBO file line number you are on"));
        lineNumberTxt.setStyle(textBoxColor);
        lineNumberTxt.setPrefWidth(45);
        lineNumberTxt.setPrefColumnCount(8);

        // Zhao Yu Li, Aug 1, 2025.
        // Small Cover
        Utils.colorButton(smallCoverButton, Color.PALEVIOLETRED, clickColor);
        smallCoverButton.setOnAction(event -> smallCoverWindow.show());

        // Zhao Yu Li, Jul 7, 2025.
        // Pattern Calculator
        patternCalculatorBtn.setText("LiPattern Calc.");
        patternCalculatorBtn.setTooltip(Utils.toolTip("Opens the Pattern Calculator"));
        Utils.colorButton(patternCalculatorBtn, Color.SKYBLUE, clickColor);
        patternCalculatorBtn.setOnAction(event -> new PatternCalculator());

        btnLoadOBOFile.setText("Load One By One File");
        btnLoadOBOFile.setTooltip(Utils.toolTip("Load a file, and go through the contents one by one"));
        Utils.colorButton(btnLoadOBOFile, Color.LIGHTPINK, clickColor);

        btnLoadOBOFile.setOnAction(event -> {

            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load One By One File");
            final File file = fileChooser.showOpenDialog(mainWindow);

            if (file != null) {
                fileCodeSequences = parseOBOFile(file.toPath());
                if (!fileCodeSequences.isEmpty()) {
                    // if the thing is nonempty, run on the first entry
                    lineNumberTxt.setText("1");
                    setOBO(0, pool, executor);
                }
            }

        });

        btnGo.setText("Go");
        btnGo.setTooltip(Utils.toolTip("Go to the line in the OBO file specified"));
        Utils.colorButton(btnGo, Color.SKYBLUE, clickColor);

        btnGo.setOnAction(event -> {
            final String indexString = lineNumberTxt.getText();

            if (indexString.isEmpty()) {
                showEnterLineNumberError();
            } else {
                try {
                    final int index = Integer.parseInt(indexString) - 1;

                    if ((index < 0) || (index > fileCodeSequences.size() - 1)) {
                        showInvalidLineNumberError(fileCodeSequences.size());
                    } else {
                        setOBO(index, pool, executor);
                    }
                } catch (final NumberFormatException e) {
                    showInvalidNumberError(indexString);
                }
            }
        });

        autoVaryBtn.setText("Auto Vary3");
        autoVaryBtn.setTooltip(Utils.toolTip("Search for code sequences which cover points on this "
                + "whole screen. See instructions for details"));
        Utils.colorButton(autoVaryBtn, Color.SKYBLUE, Color.GOLD);
        autoVaryBtn.setOnAction(event -> {
            if (!boyanMenu.CScb.isSelected() && !boyanMenu.CNScb.isSelected() && !boyanMenu.ONScb.isSelected()
                    && !boyanMenu.OSNOcb.isSelected() && !boyanMenu.OSOcb.isSelected()) {
                final Alert alert = new Alert(AlertType.ERROR);

                alert.setTitle("Vary");
                alert.setHeaderText("No CodeTypes");
                alert.setContentText("Please Select at least one codetype.");
                alert.showAndWait();
            }
            else {
                final ConvexPolygon screen = map.getViewRectangle().toConvexPolygon();
                polyVaryFunction(Tuple.of(screen, 800, 300, 150, 800, 100, 100), Optional.empty(), Optional.empty(), false, false, executor);
            }
        });

        polyVaryBtn.setText("BoyanVary");
        polyVaryBtn.setTooltip(Utils.toolTip("Search for code sequences which cover points in a "
                + "specified polygon. See instructions for details"));
        Utils.colorButton(polyVaryBtn, Color.LIGHTPINK, Color.GOLD);
        polyVaryBtn.setOnAction(event -> {
            if (compareCheckBox.isSelected()) {
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("PolyVary");
                alert.setHeaderText("MatchV3 is on");
                alert.setContentText("MatchV3 does not go with PolyVary.");
                alert.showAndWait();
                return;
            }
            if (!boyanMenu.CScb.isSelected() && !boyanMenu.CNScb.isSelected() && !boyanMenu.ONScb.isSelected()
                    && !boyanMenu.OSNOcb.isSelected() && !boyanMenu.OSOcb.isSelected() && !PolyVaryLoad.Override) {
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("PolyVary");
                alert.setHeaderText("No CodeTypes");
                alert.setContentText("Please Select at least one codetype.");
                alert.showAndWait();
                return;
            }
            if (boyanMenu.CNScb.isSelected() || boyanMenu.ONScb.isSelected()
                    || boyanMenu.OSNOcb.isSelected() || boyanMenu.OSOcb.isSelected()) {
                final Alert alert = new Alert(AlertType.WARNING);
                alert.setHeaderText("CodeTypes other than CS are chosen");
                alert.setContentText("Make sure you have the correct CodeTypes chosen.");
                alert.showAndWait();
                // return;  <-- Don't exit from this call, because PolyVary can still run with
            }
            if (boyanMenu.CScb.isSelected() || boyanMenu.CNScb.isSelected() || boyanMenu.ONScb.isSelected()
                    || boyanMenu.OSNOcb.isSelected() || boyanMenu.OSOcb.isSelected()) {
                final Rectangle screen = map.getViewRectangle();
                final PolyVaryLoad polyVaryLoad = new PolyVaryLoad("Poly Vary", "Vary", tmpDir + "cover_polygon.txt", tmpDir + "PolyAutoVaryBounds.txt", screen);
                final Optional<Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer>> polyOpt = polyVaryLoad.getPolyVaryLoad();
                if (polyOpt.isPresent()) {
                    final Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer> polyVals = polyOpt.get();
                    autoVaryArea = Optional.of(polyVals._1);
                    polyVaryFunction(polyVals, Optional.empty(), Optional.empty(), PolyVaryLoad.Override, PolyVaryLoad.AutoCover, polyVaryLoad.getAutoSmallCover(), executor);
                    renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
                }
            }
        });

        varyLBtn.setText("varyL");
        varyLBtn.setTooltip(Utils.toolTip("Search for codes at a list of points"));
        Utils.colorButton(varyLBtn, Color.LIGHTPINK, Color.GOLD);
        varyLBtn.setOnAction(event -> {
            if (!boyanMenu.CScb.isSelected() && !boyanMenu.CNScb.isSelected() && !boyanMenu.ONScb.isSelected()
                    && !boyanMenu.OSNOcb.isSelected() && !boyanMenu.OSOcb.isSelected() && !VaryWindowL.Override) {
                final Alert alert = new Alert(AlertType.ERROR);

                alert.setTitle("Vary");
                alert.setHeaderText("No CodeTypes");
                alert.setContentText("Please select at least one codetype.");
                alert.showAndWait();
            }
            else {
                if (varyWindow == null) varyWindow = new VaryWindowL("varyL", "varyL", tmpDir + "VaryPoints4.txt", tmpDir + "VaryLBounds.txt", false, this);

                final String x = textXLockField.getText();
                final String y = textYLockField.getText();

                if (varyWindow.stage.isShowing()) {
                    varyWindow.stage.toFront();
                    if (!boyanMenu.varyOnePoint.isSelected()) {
                        return;
                    }
                }

                final Optional<Tuple7<MutableList<Vector2>, Integer, Integer, Integer, Integer, Integer, Integer>> pointOpt =
                        varyWindow.getPoints(x, y, boyanMenu.varyOnePoint.isSelected());

                if (pointOpt.isPresent()) {
                    final Tuple7<MutableList<Vector2>, Integer, Integer, Integer, Integer, Integer, Integer> point = pointOpt.get();
                    final MutableList<Vector2> pointList = point._1;

                    // Zhao Yu Li, Jun 27, 2025.
                    // Attempt to read start, step, and end from the user
                    Tuple3<Integer, Integer, Integer> startStepEnd = getStartStepEnd(pointList.size());
                    if (startStepEnd._1 == null) return;

                    System.out.println(
                            "//~~~~~~~~~~~~~~~~~~~~~~~ varyL with " + point._1.size() + " points ~~~~~~~~~~~~~~~~~~~~~~~"); //added // george sept27,2017

                    // CSmax, OSOmax, OSNOmax, CSmaxSS, OSOmaxSS, OSNOmaxSS
                    final int[] maximums = {point._2, point._3, point._4, point._5, point._6, point._7};
                    final boolean draw = varyWindow.getDraw();
                    final boolean overrideSS = VaryWindowL.Override;
                    final boolean autoCover = VaryWindowL.AutoCover;
                    final boolean autoSmallCover = varyWindow.getAddToSmallCover();
                    final int maxPrint = Integer.parseInt(boyanMenu.maxPrinting.getText());

                    System.out.printf("Max code length: CS-%d OSO-%d OSNO-%d\n", maximums[0], maximums[1], maximums[2]);
                    if(overrideSS) {
                        System.out.printf("Override side sums: CS-%d OSO-%d OSNO-%d\n", maximums[3], maximums[4], maximums[5]);
                    }

                    final ExecutorService storageExecutor = new PriorityExecutor(Utils.numThreads);
                    final ExecutorService shotExecutor = Executors.newFixedThreadPool(Utils.numThreads); // This can be a default executor
                    drawVaryL(pointList, maximums, draw, overrideSS, autoCover, autoSmallCover, maxPrint, executor, storageExecutor, shotExecutor, false, false);
                }
            }
        });

        // Zhao Yu Li, Jul 17, 2025.
        cycleVaryButton.setText("LiCycle");
        Utils.colorButton(cycleVaryButton, Color.PALEVIOLETRED, Color.GOLD);
        cycleVaryButton.setOnAction(event -> {
            if (cycleVaryWindow == null) cycleVaryWindow = new CycleVaryWindow("CycleVary", "CycleVary", tmpDir + "cover_polygon.txt", tmpDir + "CycleVaryBounds.txt", tmpDir + "CycleVaryStep.txt", tmpDir + "CycleVaryCoords.txt", this);

            if (cycleVaryWindow.stage.isShowing()) cycleVaryWindow.stage.toFront();
            else cycleVaryWindow.show();
        });

        middleVaryLBtn.setText("LiMVL");
        middleVaryLBtn.setTooltip(Utils.toolTip("Search for codes at a list of points, but only prints the middle one " +
                "of each (code length, code type, odd-even pattern) group."));
        Utils.colorButton(middleVaryLBtn, Color.LIGHTPINK, Color.GOLD);
        middleVaryLBtn.setOnAction(event -> {
            if (!boyanMenu.CScb.isSelected() && !boyanMenu.CNScb.isSelected() && !boyanMenu.ONScb.isSelected()
                    && !boyanMenu.OSNOcb.isSelected() && !boyanMenu.OSOcb.isSelected() && !VaryWindowL.Override) {
                final Alert alert = new Alert(AlertType.ERROR);

                alert.setTitle("Middle Vary");
                alert.setHeaderText("No CodeTypes");
                alert.setContentText("Please select at least one codetype.");
                alert.showAndWait();
            }
            else {
                if (middleVaryWindow == null) middleVaryWindow = new VaryWindowL("MiddleVaryL", "MVaryL", tmpDir + "MiddleVaryPoints4.txt", tmpDir + "MiddleVaryLBounds.txt", true, this);

                final String x = textXLockField.getText();
                final String y = textYLockField.getText();

                if (middleVaryWindow.stage.isShowing()) {
                    middleVaryWindow.stage.toFront();
                    if (!boyanMenu.varyOnePoint.isSelected()) {
                        return;
                    }
                }

                final Optional<Tuple7<MutableList<Vector2>, Integer, Integer, Integer, Integer, Integer, Integer>> pointOpt =
                        middleVaryWindow.getPoints(x, y, boyanMenu.varyOnePoint.isSelected());

                if (pointOpt.isPresent()) {
                    final Tuple7<MutableList<Vector2>, Integer, Integer, Integer, Integer, Integer, Integer> point = pointOpt.get();
                    final MutableList<Vector2> pointList = point._1;

                    // Zhao Yu Li, Jun 27, 2025.
                    // Attempt to read start, step, and end from the user
                    Tuple3<Integer, Integer, Integer> startStepEnd = getStartStepEnd(pointList.size());
                    if (startStepEnd._1 == null) return;

                    System.out.println(
                            "//~~~~~~~~~~~~~~~~~~~~~~~ middleVaryL with " + point._1.size() + " points ~~~~~~~~~~~~~~~~~~~~~~~"); //added // george sept27,2017

                    // CSmax, OSOmax, OSNOmax, CSmaxSS, OSOmaxSS, OSNOmaxSS
                    final int[] maximums = {point._2, point._3, point._4, point._5, point._6, point._7};
                    final boolean draw = middleVaryWindow.getDraw();
                    final boolean overrideSS = VaryWindowL.Override;
                    final boolean autoCover = VaryWindowL.AutoCover;
                    final boolean autoSmallCover = middleVaryWindow.getAddToSmallCover();
                    final int maxPrint = Integer.parseInt(boyanMenu.maxPrinting.getText());

                    System.out.printf("Max code length: CS-%d OSO-%d OSNO-%d\n", maximums[0], maximums[1], maximums[2]);
                    if(overrideSS) {
                        System.out.printf("Override side sums: CS-%d OSO-%d OSNO-%d\n", maximums[3], maximums[4], maximums[5]);
                    }

                    final ExecutorService storageExecutor = new PriorityExecutor(Utils.numThreads);
                    final ExecutorService shotExecutor = Executors.newFixedThreadPool(Utils.numThreads); // This can be a default executor
                    final boolean firstLastSelected = middleVaryWindow.getFirstLastSelected();
                    drawVaryL(pointList, maximums, draw, overrideSS, autoCover, autoSmallCover, maxPrint, executor, storageExecutor, shotExecutor, true, firstLastSelected);
                }
            }
        });

        lineStartField.setPromptText("Start");
        lineStartField.setPrefWidth(60);
        lineStepField.setPromptText("Step");
        lineStepField.setPrefWidth(60);
        lineEndField.setPromptText("End");
        lineEndField.setPrefWidth(60);

        //autoPolyVaryBtn.setText("AutoPolyVary");
        autoPolyVaryBtn.setText("LiLuMaxVary");
        autoPolyVaryBtn.setTooltip(Utils.toolTip("Automatically Call PolyVary on existing holes."));
        Utils.colorButton(autoPolyVaryBtn, Color.GREEN, Color.GOLD);
        autoPolyVaryBtn.setOnAction(event -> {
            if (fileCodeSequences.isEmpty()) {
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("AutoPolyVary");
                alert.setHeaderText("No OBO File Loaded");
                alert.setContentText("Either your OBO file is empty, or you did not load one in the first place. Use the 'Load One By One File' button.");
                alert.showAndWait();
                return;
            }
            if (compareCheckBox.isSelected()) {
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("AutoPolyVary");
                alert.setHeaderText("MatchV3 Selected");
                alert.setContentText("MatchV3 and AutoPolyVary are incompatible.");
                alert.showAndWait();
                return;
            }

            // Zhao Yu Li, Jun 27, 2025.
            // Replaced code block with function call.
            Tuple3<Integer, Integer, Integer> startStepEnd = getStartStepEnd();
            if (startStepEnd._1 == null) return;

            // The following block was ripped from the polyAutoBtn's action event.
            if (!boyanMenu.CScb.isSelected() && !boyanMenu.CNScb.isSelected() && !boyanMenu.ONScb.isSelected()
                    && !boyanMenu.OSNOcb.isSelected() && !boyanMenu.OSOcb.isSelected() && !AutoPolyVaryLoad.Override) {
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("AutoPolyVary");
                alert.setHeaderText("No CodeTypes");
                alert.setContentText("Please Select at least one codetype.");
                alert.showAndWait();
                return;
            }
            if (boyanMenu.CNScb.isSelected() || boyanMenu.ONScb.isSelected()
                    || boyanMenu.OSNOcb.isSelected() || boyanMenu.OSOcb.isSelected()) {
                final Alert alert = new Alert(AlertType.WARNING);
                alert.setHeaderText("CodeTypes other than CS are chosen");
                alert.setContentText("Make sure you have the correct CodeTypes chosen.");
                alert.showAndWait();
                // return;  <-- Don't exit from this call, because PolyVary can still run with
            }
            // Ask the user to input a polygon, similar to PolyVary:
            //final Optional<ConvexPolygon> polyOpt = new PolyLoad("AutoPolyVary", "AutoVary", tmpDir + "PolyAutoVary.txt", screen).getPolyLoad();

            if (autoPolyVaryWindow == null) autoPolyVaryWindow = new AutoPolyVaryLoad("AutoPolyVary", "AutoVary", tmpDir + "cover_polygon.txt", tmpDir + "PolyAutoVaryBounds.txt");

            if (autoPolyVaryWindow.stage.isShowing()) {
                autoPolyVaryWindow.stage.toFront();
            }
            final Optional<Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer>> polyOpt = autoPolyVaryWindow.getLoad();
            if (!polyOpt.isPresent()) {
//        		final Alert alert = new Alert(AlertType.ERROR);
//        		alert.setTitle("AutoPolyVary");
//        		alert.setHeaderText("Operation Aborted");
//        		alert.setContentText("AutoPolyVary was aborted because the polygon selection window was closed.");
//        		alert.showAndWait();
                return;
            }
            final Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer> polyVals = polyOpt.get();

            autoPolyVaryFunction(polyVals, Optional.empty(), Optional.empty(), AutoPolyVaryLoad.Override, AutoPolyVaryLoad.AutoCover, autoPolyVaryWindow.getAutoSmallCover(), executor);
            // Finally, put the new polygons behind the existing cover, in the case that the final PolyVary
            // invocation found some new covers.
            //renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
        });

        superAutoCb.setText("Use LiLuMaxVary");
        superAutoCb.setSelected(false);

        superPolyVaryBtn.setText("SuperLiLuVary");
        superPolyVaryBtn.setTooltip(Utils.toolTip("Repeat PolyVary or AutoPolyVary a set number of times"));
        Utils.colorButton(superPolyVaryBtn, Color.GREEN, Color.GOLD);
        superPolyVaryBtn.setOnAction(event -> {
            if (fileCodeSequences.size() == 0 && superAutoCb.isSelected()) {
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("SuperPolyVary using Auto");
                alert.setHeaderText("No OBO File Loaded");
                alert.setContentText("Either your OBO file is empty, or you did not load one in the first place. Use the 'Load One By One File' button.");
                alert.showAndWait();
                return;
            }
            if (compareCheckBox.isSelected()) {
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("SuperPolyVary");
                alert.setHeaderText("MatchV3 is on");
                alert.setContentText("MatchV3 does not go with SuperPolyVary.");
                alert.showAndWait();
                return;
            }
            if (!boyanMenu.CScb.isSelected() && !boyanMenu.CNScb.isSelected() && !boyanMenu.ONScb.isSelected()
                    && !boyanMenu.OSNOcb.isSelected() && !boyanMenu.OSOcb.isSelected()) {
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("SuperPolyVary");
                alert.setHeaderText("No CodeTypes");
                alert.setContentText("Please Select at least one codetype.");
                alert.showAndWait();
                return;
            }
            if (boyanMenu.CNScb.isSelected() || boyanMenu.ONScb.isSelected()
                    || boyanMenu.OSNOcb.isSelected() || boyanMenu.OSOcb.isSelected()) {
                final Alert alert = new Alert(AlertType.WARNING);
                alert.setHeaderText("CodeTypes other than CS are chosen");
                alert.setContentText("Make sure you have the correct CodeTypes chosen.");
                alert.showAndWait();
                // return;  <-- Don't exit from this call, because PolyVary can still run with
            }

            // Zhao Yu Li, Jul 08, 2025.
            // Lazy initialization of superPolyVaryWindow.
            if (superPolyVaryWindow == null) superPolyVaryWindow = new SuperPolyVaryLoad("SuperPolyVary", "SuperVary", tmpDir + "cover_polygon.txt", tmpDir + "PolyAutoVaryBounds.txt", tmpDir + "SuperVaryStep.txt");

            if (superPolyVaryWindow.stage.isShowing()) {
                superPolyVaryWindow.stage.toFront();
            }

            final Optional<Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer>> polyOpt = superPolyVaryWindow.getLoad();
            if (!polyOpt.isPresent()) {
//        		final Alert alert = new Alert(AlertType.ERROR);
//        		alert.setTitle("AutoPolyVary");
//        		alert.setHeaderText("Operation Aborted");
//        		alert.setContentText("AutoPolyVary was aborted because the polygon selection window was closed.");
//        		alert.showAndWait();
                return;
            }
            superPolyVaryFunction(polyOpt.get(), executor);
        });


        fillScreenBtn.setText("Fill Screen");
        fillScreenBtn.setTooltip(Utils.toolTip("Color in the area currently on screen"));
        Utils.colorButton(fillScreenBtn, Color.SKYBLUE, clickColor);

        fillScreenBtn.setOnAction(event -> {

            final Image image = regionsImageView.getImage();
            final PixelReader reader = image.getPixelReader();

            int numHoles = 0;

            for (int pixelX = 0; pixelX < SIDE; pixelX += 1) {
                for (int pixelY = 0; pixelY < SIDE; pixelY += 1) {
                    final int color = reader.getArgb(pixelX, pixelY);

                    if (color == 0) {
                        numHoles += 1;
                    }
                }
            }

            if (numHoles > 0) {
                final Alert alert = new Alert(AlertType.CONFIRMATION);

                alert.setTitle("Fill Screen");
                alert.setHeaderText("Fill Screen");
                alert.setContentText(
                        "This area contains holes. Are you sure you want to fill the screen?");
                final Optional<ButtonType> response = alert.showAndWait();

                if (response.isPresent() && response.get() == ButtonType.OK) {
                    screenFills.add(map.getViewRectangle());
                    setImageColor((WritableImage) regionsImageView.getImage(), screenFillsColor);
                }
            } else {
                screenFills.add(map.getViewRectangle());
                setImageColor((WritableImage) regionsImageView.getImage(), screenFillsColor);
            }
        });

        clearFillsBtn.setText("Clear Fills");
        clearFillsBtn.setTooltip(Utils.toolTip("Clear all screen fills"));
        Utils.colorButton(clearFillsBtn, Color.SKYBLUE, clickColor);

        clearFillsBtn.setOnAction(event -> {
            final Alert alert = new Alert(AlertType.CONFIRMATION);

            alert.setTitle("Clear Screen Fills");
            alert.setHeaderText("Clear Screen Fills");
            alert.setContentText("Are you sure you want to clear all screen fills?");
            final Optional<ButtonType> response = alert.showAndWait();

            if (response.isPresent() && response.get() == ButtonType.OK) {
                screenFills.clear();
                renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
            }
        });

        saveFillBtn.setText("S");
        saveFillBtn.setTooltip(Utils.toolTip("Save the current screen fills to the file 'fills.txt"));
        Utils.colorButton(saveFillBtn, Color.SKYBLUE, clickColor);

        saveFillBtn.setOnAction(event -> {

            final MutableList<String> lines = new FastList<>();

            for (final Rectangle rect : screenFills) {
                final double x1 = rect.intervalX.min;
                final double x2 = rect.intervalX.max;
                final double y1 = rect.intervalY.min;
                final double y2 = rect.intervalY.max;

                final String line = x1 + " " + x2 + " " + y1 + " " + y2;
                lines.add(line);
            }

            try (final PrintWriter writer = new PrintWriter("fills.txt")) {
                lines.forEach(writer::println);
            } catch (final FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        loadFillBtn.setText("L");
        loadFillBtn.setTooltip(Utils.toolTip("Load all screen fills saved in 'fills.txt'"));
        Utils.colorButton(loadFillBtn, Color.SKYBLUE, clickColor);

        loadFillBtn.setOnAction(event -> {

            try (final BufferedReader br = new BufferedReader(new FileReader("fills.txt"))) {
                String line = br.readLine();

                while (line != null) {
                    final String[] corners = line.split("\\s");

                    screenFills.add(Rectangle.create(Double.parseDouble(corners[0]),
                            Double.parseDouble(corners[1]),
                            Double.parseDouble(corners[2]),
                            Double.parseDouble(corners[3])));
                    line = br.readLine();
                }

                renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

        });

        calculateChooser.getItems().addAll("Region", "MRR", "Bound", "All");
        calculateChooser.setTooltip(Utils.toolTip("Set this to the feature that you want to be drawn. "
                + "'Region' draws the region, 'MRR' draws the MRR polygon of the region, and "
                + "'Bound' draws the bounding polygon of that region."));
        calculateChooser.setValue("Region");
        calculateChooser.setStyle(textBoxColor);
        calculateChooser.setPrefWidth(93);

        btnOBOForward.setText("OBO Forward");
        btnOBOForward.setTooltip(Utils.toolTip("Go to the next code in the OBO file"));
        Utils.colorButton(btnOBOForward, Color.SKYBLUE, clickColor);

        btnOBOForward.setOnAction(event -> {
            // Ensure that the user has told us to take a positive integer step size
            int stepSize;
            try {
                stepSize = Integer.parseInt(fieldOBOStep.getText());
                if (stepSize <= 0) {
                    final Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("OBO Forward");
                    alert.setHeaderText("Positive Integer Step Size Required");
                    alert.setContentText("Enter a positive integer into the step size field between the OBO Forward and OBO Backward buttons");
                    alert.showAndWait();
                    return;
                }
            } catch (NumberFormatException e) {
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("OBO Forward");
                alert.setHeaderText("Positive Integer Step Size Required");
                alert.setContentText("Enter a positive integer into the step size field between the OBO Forward and OBO Backward buttons");
                alert.showAndWait();
                return;
            }

            // The TextField has a setOnAction method that is called
            // when the user presses enter, but the problem is that
            // users don't expect to press enter after entering text
            // They want to enter text, and then push a button or whatever
            // to do the thing they want to do
            // With the setOnAction/press-enter, we could
            final String indexString = lineNumberTxt.getText();

            if (indexString.isEmpty()) {
                showEnterLineNumberError();
            } else {
                try {
                    final int index = (Integer.parseInt(indexString) - 1) + stepSize;

                    if (index > fileCodeSequences.size() - 1) {
                        showInvalidLineNumberError(fileCodeSequences.size());
                    } else {
                        final String lineNum = Integer.toString(index + 1);
                        lineNumberTxt.setText(lineNum);
                        setOBO(index, pool, executor);
                    }

                } catch (final NumberFormatException e) {
                    showInvalidNumberError(indexString);
                }
            }
        });

        //fieldOBOStep.setAlignment(Pos.CENTER);
        fieldOBOStep.setText("1");
        fieldOBOStep.setPromptText("Step");
        fieldOBOStep.setPrefWidth(50);

        //Setting the pool for the stables window and button properties
        stablesWindow.setConnectionPool(pool);

        //stablesButton.setText("Triples");
        stablesButton.setText("TokarskyTriples");
        stablesButton.setTooltip(Utils.toolTip("Brings up a window to test multiple stables and unstables"));
        Utils.colorButton(stablesButton, Color.MAGENTA, Color.GOLD);
        stablesButton.setOnAction(event -> {stablesWindow.show();});//Opens a new stables window

        btnOBOBackward.setText("OBO Backward");
        btnOBOBackward.setTooltip(Utils.toolTip("Go to the previous code in the OBO file"));
        Utils.colorButton(btnOBOBackward, Color.SKYBLUE, clickColor);

        btnOBOBackward.setOnAction(event -> {
            // Ensure that the user has told us to take a positive integer step size
            int stepSize;
            try {
                stepSize = Integer.parseInt(fieldOBOStep.getText());
                if (stepSize <= 0) {
                    final Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("OBO Forward");
                    alert.setHeaderText("Positive Integer Step Size Required");
                    alert.setContentText("Enter a positive integer into the step size field between the OBO Forward and OBO Backward buttons");
                    alert.showAndWait();
                    return;
                }
            } catch (NumberFormatException e) {
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("OBO Forward");
                alert.setHeaderText("Positive Integer Step Size Required");
                alert.setContentText("Enter a positive integer into the step size field between the OBO Forward and OBO Backward buttons");
                alert.showAndWait();
                return;
            }

            final String indexString = lineNumberTxt.getText();

            if (indexString.isEmpty()) {
                showEnterLineNumberError();
            } else {
                try {
                    final int index = (Integer.parseInt(indexString) - 1) - stepSize;

                    if (index < 0) {
                        showInvalidLineNumberError(fileCodeSequences.size());
                    } else {
                        final String lineNum = Integer.toString(index + 1);
                        lineNumberTxt.setText(lineNum);

                        setOBO(index, pool, executor);
                    }
                } catch (final NumberFormatException e) {
                    showInvalidNumberError(indexString);
                }
            }
        });

        classifyBtn.setText("Classify");
        classifyBtn.setTooltip(Utils.toolTip("Take a file of bare codes and format it properly"));
        Utils.colorButton(classifyBtn, Color.LIGHTPINK, clickColor);

        classifyBtn.setOnAction(event -> {

            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load File");

            final File file = fileChooser.showOpenDialog(mainWindow);

            if (file != null) {
                final Tuple2<Tuple3<
                        Optional<Rectangle>,
                        Map<ClassifiedCodeSequence, Optional<Color>>,
                        Map<ClassifiedCodeSequence, Optional<String[]>>
                        >,
                        ArrayList<ClassifiedCodeSequence[]>> tup =
                        parseFile(file.toPath(), false);

                final Map<ClassifiedCodeSequence, Optional<Color>> map = tup._1._2;

                final MutableSortedSet<ClassifiedCodeSequence> classCodeSeqs =
                        new TreeSortedSet<>(map.keySet());

                final ArrayList<ClassifiedCodeSequence> organizedCodes = new ArrayList<>();
                if (splitUp) {
                    final CodeType[] codeTypes = {CodeType.CS, CodeType.OSO, OSNO, CodeType.CNS, CodeType.ONS};
                    for (final CodeType type : codeTypes) {
                        for (final ClassifiedCodeSequence code : classCodeSeqs) {
                            if (code.codeType.equals(type)) {
                                organizedCodes.add(code);
                            }
                        }
                    }
                } else {
                    organizedCodes.addAll(classCodeSeqs);
                }
                final String newTitle = file.getAbsolutePath().split(".txt")[0] + "_classified.txt";

                final MutableList<String> lines = new FastList<>();

                for (final ClassifiedCodeSequence line : organizedCodes) {
                    final String toPad =
                            line.codeType + " (" + line.codeLength + ", " + line.codeSum + ") ";
                    final String evenOdd = CodeSequence.evenOddSequence(line.codeSequence.codeNumbers);

                    final String lineStr = String.format("%1$-16s", toPad) + line + " " + evenOdd;
                    lines.add(lineStr);
                }

                Utils.printToFile(newTitle, lines);
            }
        });

        btnLoadFile.setText("Load File");
        btnLoadFile.setTooltip(Utils.toolTip("Loads and draws code sequences from a file. See"
                + " instructions for how to properly format files to use with this button"));
        Utils.colorButton(btnLoadFile, Color.LIGHTPINK, clickColor);

        btnLoadFile.setOnAction(event -> {
            final ConvexPolygon screen = map.getViewRectangle().toConvexPolygon();
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load File");
            final File file = fileChooser.showOpenDialog(mainWindow);
            if (file != null) {
                System.out.println("\n// loading file: " + iterationStart.getText()
                        + " to " + iterationEnd.getText() + " in "+ file);
            }
            LoadFileAction(screen, true, file, executor, false, true);
        });

        loadLRCheckBox.setText("lr");
        loadLRCheckBox.selectedProperty().bindBidirectional(uselrRdoBtn.selectedProperty());
        loadLRCheckBox.setTooltip(Utils.toolTip("Use left rights when loading iterations"));
        loadLRCheckBox.setOnAction(event -> {
            if (!loadLRCheckBox.isSelected()) {
                nolrRdoBtn.setSelected(true);
            }
        });

        // Create a new info window and show it
        infoButton.setText("Info");
        infoButton.setTooltip(Utils.toolTip("Brings up a window that will show you information about"
                + " a code sequence"));
        Utils.colorButton(infoButton, Color.LIGHTPINK, clickColor);

        infoButton.setOnAction(event -> new InfoWindow(windowTitle, pool).show());

        // Create a new gradient window and show it
        gradientButton.setText("gradient");
        gradientButton.setTooltip(Utils.toolTip("Brings up a window that will calculate the gradient of a given equation"));
        Utils.colorButton(gradientButton, Color.SKYBLUE, clickColor);
        gradientButton.setOnAction(event -> new GradientWindow(windowTitle).show());


        // show extra information
        lookAtMeButton.setText("LookatMe");
        Utils.colorButton(lookAtMeButton, Color.LIGHTPINK, clickColor);
        lookAtMeButton.setOnAction(event -> new LookAtMeWindow(windowTitle).show());


        coverBtn.setText("Cover");
        coverBtn.setTooltip(Utils.toolTip("Brings up a window that allows you to check if some code"
                + " sequences cover a specified polygon. See instructions for details"));
        Utils.colorButton(coverBtn, Color.LIGHTPINK, clickColor);
        coverBtn.setOnAction(e -> coverWindow.show());

        loadHolesBtn.setText("Load Holes");
        loadHolesBtn.setTooltip(Utils.toolTip("Load uncovered cover coordinates from tmp/holes.txt"));
        Utils.colorButton(loadHolesBtn, Color.LIGHTPINK, clickColor);
        loadHolesBtn.setOnAction(e -> loadHolesFromFile());

//        halfTripleBtn.setText("Half Triple");
//        halfTripleBtn.setTooltip(Utils.toolTip("Brings up a window that allows you to check if some code"
//                + " sequences cover a specified polygon. See instructions for details"));
//        Utils.colorButton(halfTripleBtn, Color.LIGHTPINK, clickColor);
//        halfTripleBtn.setOnAction(e -> {
//            coverWindow2.show();
//            //trim = true;
//        });

//        unstableBtn.setText("Unstable");
//        unstableBtn.setTooltip(Utils.toolTip("Brings up a window that allows you to check if some code"
//                + " sequences cover a specified polygon. See instructions for details"));
//        Utils.colorButton(unstableBtn, Color.LIGHTPINK, clickColor);
//        unstableBtn.setOnAction(e -> {
//            coverWindow3.show();
//            //trim = true;
//        });

//        cornerBtn.setText("Corner");
//        cornerBtn.setTooltip(Utils.toolTip("Brings up a window that allows you to check if some code"
//                + " sequences cover a specified polygon. See instructions for details"));
//        Utils.colorButton(cornerBtn, Color.LIGHTPINK, clickColor);
//        cornerBtn.setOnAction(e -> {
//            coverWindow4.show();
//            //trim = true;
//        });

        txtCodeSequence.setPromptText("Code Sequence");
        txtCodeSequence.setTooltip(Utils.toolTip("here you put in a code sequence that you want to"
                + " calculate"));
        txtCodeSequence.setStyle(textBoxColor);
        txtCodeSequence.setPrefColumnCount(10);
        txtCodeSequence.textProperty().bindBidirectional(
                new SimpleObjectProperty<MutableIntList[]>(currentCodeNumbers),
                new StringConverter<MutableIntList[]>() {
                    @Override
                    public MutableIntList[] fromString(String str) {
                        final String[] strs = str.split(",");
                        final MutableIntList[] result = new MutableIntList[strs.length];
                        for (int i = 0; i < strs.length; i++) {
                            final Optional<ImmutableIntList> opt = Utils.splitString(strs[i]);
                            if (opt.isPresent()) {
                                final MutableIntList list = new IntArrayList();
                                list.addAll(opt.get());
                                result[i] = list;
                            } else {
                                result[i] = new IntArrayList();
                            }
                        }
                        return result;
                    }

                    @Override
                    public String toString(MutableIntList[] codes) {
                        final StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < codes.length; i++) {
                            if (!codes[i].isEmpty()) {
                                if (i != 1) {
                                    builder.append(", ");
                                }
                                builder.append(codes[i].toString());
                            }
                        }
                        return builder.toString();
                    }
                }
        );

        txtCodeSequence2.setPromptText("Code Sequence");
        // make them always have the same text stuff
        txtCodeSequence2.textProperty().bindBidirectional(txtCodeSequence.textProperty());

        proverCheckBox.setText("Prover");
        proverCheckBox.setTooltip(Utils.toolTip("pixels on screen that are filled in while this is "
                + "selected have been proven to completely fill that pixel"));
        proverCheckBox.setStyle(textBoxColor);
        proverCheckBox.setOnAction(event -> {
            // Redraw the regions when you check and uncheck the box
            renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
        });

        offsetTextField.setPromptText("Offset");
        offsetTextField.setPrefWidth(100);

        loadCoverButton.setText("Load Cover");
        loadCoverButton.setOnAction(e -> {
            final DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choose a Cover Directory");

            final File dir = chooser.showDialog(mainWindow);

            if (dir != null) {
                loadCoverWithoutTrim(dir.getPath(), executor);
                trim = false;
            }

        });

        holeFinderButton.setText("Find Holes");
        holeFinderButton.setTooltip(Utils.toolTip(
                "Finds how many uncovered pixels are on screen and reports them"));
        Utils.colorButton(holeFinderButton, Color.SKYBLUE, clickColor);

        holeFinderButton.setOnAction(event -> {
            final ConvexPolygon screen = map.getViewRectangle().toConvexPolygon();

            final FastList<Vector2> list = findHoles(screen);

            Utils.printToFile("holes.txt", list);

            final Alert alert = new Alert(AlertType.INFORMATION);

            alert.setTitle("Hole Finder");
            alert.setHeaderText("Hole Finder");
            alert.setContentText(String.format("Found %d holes.", list.size()));
            alert.showAndWait();
        });

        labelMainWindow.setPrefWidth(80);

        labelMainWindow.setPromptText("Label");
        labelCodeWindow.setPromptText("Label");
        labelMainWindow.setStyle(textBoxColor);

        labelCodeWindow.textProperty().bindBidirectional(labelMainWindow.textProperty());

        // The center BorderPane can offer more vertical room than the image
        // itself. Keep the stack fixed to the 600x600 pixel image size so the
        // reflect transform flips the drawing surface, not a taller layout box.
        imageStack.setMinSize(SIDE, SIDE);
        imageStack.setPrefSize(SIDE, SIDE);
        imageStack.setMaxSize(SIDE, SIDE);
        imageStack.getChildren().addAll(backgroundImageView, regionsImageView, guideLinesImageView,
                boundsImageView, oboImageView, topImageView);

        reflectCheckBox.setText("Reflect");
        reflectCheckBox.setTooltip(Utils.toolTip("Reflects the map into usual cartesian coordinates"));
        reflectCheckBox.setStyle(textBoxColor);

        //Affine startReflect = new Affine();
        //BorderPane
        //startReflect.setMyy(-1);
        //startReflect.setTy(imageStack.getBoundsInLocal().getHeight());
        //imageStack.getTransforms().clear();
        //imageStack.getTransforms().add(startReflect);
        // Based on https://gist.github.com/jewelsea/1436935
        reflectCheckBox.setSelected(true);

        reflectCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            updateReflection();
        });

        Platform.runLater(() -> {
    // If you want it checked on boot:
            updateReflection();
            
            // OR, if you want it to process its default state (even if false):
            // Just extract to a method like in Option 2 and call it here.
        });

        allCheckBox.setText("All");
        allCheckBox.setTooltip(Utils.toolTip(
                "Show all regions covered by a code sequence, instead of just the basic one. See "
                        + "instructions for more explanation."));
        allCheckBox.setStyle(textBoxColor);
        allCheckBox.setOnAction(event
                -> renderRegions(
                onScreenSequences, guideLinesImageView, regionsImageView, executor));

        boundsCheckBox.setText("Bounds");
        boundsCheckBox.setStyle(textBoxColor);
        boundsCheckBox.setOnAction(event
                -> renderRegions(
                onScreenSequences, guideLinesImageView, regionsImageView, executor));

        queryButton.setText("Search");
        queryButton.setTooltip(Utils.toolTip(
                "Search for a code sequence that has some specified characteristics"));
        Utils.colorButton(queryButton, Color.LIGHTPINK, clickColor);

        queryButton.setOnAction(event -> queryStage.show());

        codeWindow.setTitle(windowTitle);
        codeWindow.setOnCloseRequest(event -> codeWindow.close());
        codeWindow.setScene(codeWindowScene);

        // For input validation, you have two things to check
        // Check if the string is empty. This means the user entered no input
        // Then check if the string is valid

        btnCalculate.setText("Calculate");
        btnCalculate.setTooltip(Utils.toolTip("Calculate the code sequence entered"));
        Utils.colorButton(btnCalculate, Color.SKYBLUE, clickColor);

        // Zhao Yu Li, May 29, 2025.
        // Opens code calculation/iteration window if code sequence is empty.
        // So that we can use the tools in the window before even actually calculating any iterations.
        btnCalculate.setOnAction(event -> {
            if (txtCodeSequence.getText().isEmpty()) codeWindow.show();

            btnCalculateAction(pool);
        });

        btnCalculate2.setText("Calculate");
        btnCalculate.setTooltip(Utils.toolTip("Calculate the code sequence entered"));
        Utils.colorButton(btnCalculate2, Color.SKYBLUE, clickColor);

        btnCalculate2.setOnAction(event -> btnCalculateAction(pool));

        calculateAddToCoverCheckBox.setText("Add to Cover");
        calculateAddToCoverCheckBox.setTooltip(Utils.toolTip(
                "When selected, the existing Calculate button also writes a successful stable code or "
                        + "stable-unstable-stable triple into the Cover window. Unstable singles and incomplete "
                        + "or empty triples are skipped because the cover format cannot use them directly."));
        calculateAddToCoverCheckBox.setStyle(textBoxColor);

        resetBtn.setText("Reset");
        resetBtn.setTooltip(Utils.toolTip("Change the zoom level back to the default, like it was"
                + " when you first opened the program."));
        Utils.colorButton(resetBtn, Color.SKYBLUE, clickColor);

        resetBtn.setOnAction(event -> {
            map.reset();
            viewRectangleBF.add(map.getViewRectangle());
            renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
        });

        clearBtn.setText("Clear");
        clearBtn.setTooltip(Utils.toolTip("Clear everything from the screen  (The guidelines will be all that remains)"));

        Utils.colorButton(clearBtn, Color.SKYBLUE, clickColor);

        clearBtn.setOnAction(event -> {
            // reset the boxes on the right
            codeSequencesGPane.getChildren().clear();

            screenFills.clear();
            onScreenSequences.clear();
            innerPolyBounds.clear();
            outerPolyBounds.clear();
            coverRects.clear();
            mrrBounds.clear();
            coverArea = Optional.empty();
            autoVaryArea = Optional.empty();
            regionsImageView.setImage(new WritableImage(SIDE, SIDE));
            boundsImageView.setImage(new WritableImage(SIDE, SIDE));

            currentOBOStorage = null;
            lineNumberTxt.setText("");
            oboImageView.setImage(new WritableImage(SIDE, SIDE));
        });

        saveRegionsCheckBox.setText("Save Regions");
        saveRegionsCheckBox.setTooltip(Utils.toolTip("If this is not selected, each time you load a "
                + "code it will get rid of the other ones on screen. So, this is used when you want "
                + "to look at one code at a time."));
        saveRegionsCheckBox.setSelected(true);
        saveRegionsCheckBox.setStyle(textBoxColor);
        // When the user deselects this box, we show a confirmation box
        saveRegionsCheckBox.setOnAction(event -> {
            if (!saveRegionsCheckBox.isSelected()) {
                final Alert confirmation = new Alert(AlertType.CONFIRMATION);
                confirmation.setTitle("Disable Save Regions");
                confirmation.setHeaderText("Disable Save Regions");
                confirmation.setContentText("Are you sure you want to disable Save Regions?");

                // If the user pushes OK, then yes we disable, so don't do anything
                // If they cancel, then we need to change the button back
                final Optional<ButtonType> response = confirmation.showAndWait();
                // The thing inside the ! is the only case when we would keep it not selected
                if (!(response.isPresent() && response.get() == ButtonType.OK)) {
                    saveRegionsCheckBox.setSelected(true);
                }
            }

        });

        drawPictureCheckBox.setText("Draw Picture");
        drawPictureCheckBox.setTooltip(Utils.toolTip("if this is not selected, the pictures will "
                + "not be drawn when you press calculate or load file"));
        drawPictureCheckBox.setSelected(true);
        drawPictureCheckBox.setStyle(textBoxColor);
        drawPictureCheckBox.setOnAction(event -> {
            if (!drawPictureCheckBox.isSelected()) {
                final Alert confirmation = new Alert(AlertType.CONFIRMATION);
                confirmation.setTitle("Disable Draw Picture");
                confirmation.setHeaderText("Disable Draw Picture");
                confirmation.setContentText("Are you sure you want to disable Draw Picture?");

                // If the user pushes OK, then yes we disable, so don't do anything
                // If they cancel, then we need to change the button back
                final Optional<ButtonType> response = confirmation.showAndWait();
                // The thing inside the ! is the only case when we would keep it not selected
                if (!(response.isPresent() && response.get() == ButtonType.OK)) {
                    drawPictureCheckBox.setSelected(true);
                }
            }
        });

        showFillsCheckBox.setText("Show Fills");
        showFillsCheckBox.setTooltip(Utils.toolTip("Show the screen fills"));

        showFillsCheckBox.setSelected(true);
        showFillsCheckBox.setStyle(textBoxColor);
        showFillsCheckBox.setOnAction(event -> {
            renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
        });

        autoFillerCheckBox.setText("AutoFill");
        autoFillerCheckBox.setTooltip(Utils.toolTip("If this is selected, whenever there are no holes"
                + " on screen, a screen fill will be made there"));
        autoFillerCheckBox.setSelected(false);
        autoFillerCheckBox.setStyle(textBoxColor);

        xMinTextField.setPromptText("X min");
        xMinTextField.setText("0");
        xMinTextField.setPrefColumnCount(8);
        xMinTextField.setStyle(textBoxColor);

        xMaxTextField.setPromptText("X max");
        xMaxTextField.setText("180");
        xMaxTextField.setPrefColumnCount(8);
        xMaxTextField.setStyle(textBoxColor);

        yMinTextField.setPromptText("Y min");
        yMinTextField.setText("0");
        yMinTextField.setPrefColumnCount(8);
        yMinTextField.setStyle(textBoxColor);

        yMaxTextField.setPromptText("Y max");
        yMaxTextField.setText("180");
        yMaxTextField.setPrefColumnCount(8);
        yMaxTextField.setStyle(textBoxColor);

        saveColors.setText("Save Colors");
        saveColors.setTooltip(Utils.toolTip("When using PolySave, you can save the colors the regions"
                + " were at the time of saving"));
        saveColors.setSelected(true);
        saveColors.setStyle(textBoxColor);

        polyLoadButton.setText("Polygon");
        polyLoadButton.setTooltip(Utils.toolTip("Load code sequences from a file, but only draw them"
                + " if they intersect a specified polygon"));
        Utils.colorButton(polyLoadButton, Color.LIGHTPINK, clickColor);
        polyLoadButton.setOnAction(event -> {
            final Rectangle screen = map.getViewRectangle();
            final Optional<ConvexPolygon> polyOpt = new PolyLoad(
                    "Polygonal Load", "Load", tmpDir + "PolyLoad.txt", screen)
                    .getPolyLoad();

            if (!polyOpt.isPresent()) {
                return;
            }

            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Polygon File");
            final File file = fileChooser.showOpenDialog(mainWindow);

            LoadFileAction(polyOpt.get(), false, file, executor);
        });

        polyLoadDBButton.setText("PolygonDB");
        polyLoadDBButton.setTooltip(Utils.toolTip("Load code sequences from a SQLite database file, but only " +
                "draw them if they intersect a specified polygon"));
        Utils.colorButton(polyLoadDBButton, Color.LIGHTPINK, clickColor);
        polyLoadDBButton.setOnAction(event -> {
            final Rectangle screen = map.getViewRectangle();
            final Optional<ConvexPolygon> polyOpt = new PolyLoad(
                    "Polygonal DB Load", "Load", tmpDir + "PolyLoadDB.txt", screen)
                    .getPolyLoad();

            if (!polyOpt.isPresent()) {
                return;
            }

            final FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("SQLite DB", "*.db", "*.sqlite", "*.sqlite3"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            fileChooser.setTitle("Load Polygon SQL DB File");
            final File file = fileChooser.showOpenDialog(mainWindow);

            LoadFileAction(polyOpt.get(), false, file, executor, true, false);
        });

        loadDirectoryButton.setText("Load Directory");
        loadDirectoryButton.setTooltip(Utils.toolTip("Recursively searches through a directory for all filed named" +
                "info.txt, and loads the stables and triples within into the cover if they intersect with the " +
                "specified polygon."));
        Utils.colorButton(loadDirectoryButton, Color.LIGHTPINK, clickColor);
        loadDirectoryButton.setOnAction(event -> {
            final Rectangle screen = map.getViewRectangle();
            final Optional<ConvexPolygon> polyOpt = new PolyLoad(
                    "Load Directory", "Load", tmpDir + "loadDirectory.txt", screen)
                    .getPolyLoad();

            if (!polyOpt.isPresent()) {
                return;
            }

            // Zhao Yu Li, May 13, 2025.
            // Extract stables and triples from a directory
            final DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Load stables and triples from directories");
            final File directory = directoryChooser.showDialog(mainWindow);

            // The two files that we look for are info.txt and unused.txt
            if (directory != null) {
                Path startPath = directory.toPath();
                System.out.println("Searching in: " + startPath);

                AtomicInteger readState = new AtomicInteger(0);

                try (Stream<Path> stream = Files.walk(startPath)) {
                    stream.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().equals("info.txt"))
                            .forEach(path -> {
                                final LinkedHashMap<ClassifiedCodeSequence, Optional<Color>> map = new LinkedHashMap<>();
                                final LinkedHashMap<ClassifiedCodeSequence, Optional<String[]>> optIter =
                                        new LinkedHashMap<>();

                                final ArrayList<ClassifiedCodeSequence[]> triples = new ArrayList<>();

                                boolean readStables = true;
                                boolean readTriples = true;

                                try (final Stream<String> lines = Files.lines(path)) {
                                    for (String line : (Iterable<String>) lines::iterator) {
                                        if (line.contains("following triples")
                                                || line.contains("following stables")
                                                || line.contains("squares were"))
                                        {
                                            int state = readState.incrementAndGet();

                                            if (state == 1) {
                                                if (boyanMenu.CScb.isSelected()
                                                        || boyanMenu.OSNOcb.isSelected()
                                                        || boyanMenu.OSOcb.isSelected()) {
                                                    System.out.println("// Reading stables");
                                                } else {
                                                    readStables = false;
                                                    System.out.println("// Skip reading stables");
                                                }
                                            } else if (state == 2) {
                                                if (boyanMenu.Triplescb.isSelected()) {
                                                    System.out.println("// Reading triples");
                                                } else {
                                                    readTriples = false;
                                                    System.out.println("// Skip reading triples");
                                                }
                                            } else if (line.contains("squares were")) {
                                                System.out.println("// Finished reading");
                                            }
                                            continue;
                                        }

                                        if (line.startsWith("//") || line.trim().isEmpty()) continue;

                                        if (readStables && readState.get() == 1) {
                                            final String[] sections = Utils.trimCodeLine(line).split(",");

                                            final String sequenceString = sections[0].trim();
                                            final ImmutableIntList sequence = Utils.splitString(sequenceString).get();

                                            final ClassifiedCodeSequence codeSeq =
                                                    ClassifiedCodeSequence.create(sequence).get();

                                            if ((codeSeq.codeType == CodeType.CS && boyanMenu.CScb.isSelected())
                                                    || (codeSeq.codeType == OSNO && boyanMenu.OSNOcb.isSelected())
                                                    || (codeSeq.codeType == CodeType.OSO && boyanMenu.OSOcb.isSelected())) {
                                                final Optional<Color> optColor;
                                                if (sections.length == 2) {
                                                    final String colorString = sections[1].trim();
                                                    final Color color = Color.web(colorString);
                                                    optColor = Optional.of(color);
                                                } else {
                                                    optColor = Optional.empty();
                                                }

                                                map.put(codeSeq, optColor);

                                                final Optional<String[]> lineOptIter = Optional.empty();
                                                optIter.put(codeSeq, lineOptIter);
                                            }
                                        } else if (readState.get() == 2) {
                                            final String[] sections = Utils.trimCodeLine(line).split(",");
                                            ClassifiedCodeSequence[] triple = new ClassifiedCodeSequence[3];

                                            for (int i = 0; i < 3; i++) {
                                                final String sequenceString = sections[i].trim();
                                                final ImmutableIntList sequence = Utils.splitString(sequenceString).get();

                                                final ClassifiedCodeSequence codeSeq =
                                                        ClassifiedCodeSequence.create(sequence).get();

                                                // Zhao Yu Li, May 13, 2025.
                                                // We may not be reading triples, but the stable components of a
                                                // triple may be useful to us, and should be added to the stables text
                                                // box of the cover.
                                                if (!readTriples && readStables && ((codeSeq.codeType == CodeType.CS && boyanMenu.CScb.isSelected())
                                                        || (codeSeq.codeType == OSNO && boyanMenu.OSNOcb.isSelected())
                                                        || (codeSeq.codeType == CodeType.OSO && boyanMenu.OSOcb.isSelected()))) {
                                                    final Optional<Color> optColor = Optional.empty();

                                                    map.put(codeSeq, optColor);

                                                    final Optional<String[]> lineOptIter = Optional.empty();
                                                    optIter.put(codeSeq, lineOptIter);
                                                }

                                                // On the other hand, if we are reading triples, we need a structure
                                                // that preserves the triples relationship of its three components
                                                if (readTriples) triple[i] = codeSeq;
                                            }

                                            if (readTriples) triples.add(triple);
                                        } else if (readState.get() == 3) {
                                            break;
                                        }
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }

                                final Tuple3<Optional<Rectangle>, Map<ClassifiedCodeSequence, Optional<Color>>,
                                        Map<ClassifiedCodeSequence, Optional<String[]>>> tup =
                                        new Tuple3<>(Optional.empty(), map, optIter);

                                drawCodes(tup, triples, executor, false, polyOpt.get(), true);
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Finished loading stables from directory.");

                try (Stream<Path> stream = Files.walk(startPath)) {
                    stream.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().equals("unused.txt"))
                            .forEach(path -> {
                                final LinkedHashMap<ClassifiedCodeSequence, Optional<Color>> map = new LinkedHashMap<>();
                                final LinkedHashMap<ClassifiedCodeSequence, Optional<String[]>> optIter =
                                        new LinkedHashMap<>();

                                final ArrayList<ClassifiedCodeSequence[]> triples = new ArrayList<>();

                                try (final Stream<String> lines = Files.lines(path)) {
                                    for (String line : (Iterable<String>) lines::iterator) {
                                        if (line.startsWith("//") || line.trim().isEmpty()) continue;

                                        final String[] sections = Utils.trimCodeLine(line).split(",");

                                        if (sections.length == 1) {
                                            final String sequenceString = sections[0].trim();
                                            final ImmutableIntList sequence = Utils.splitString(sequenceString).get();

                                            final ClassifiedCodeSequence codeSeq =
                                                    ClassifiedCodeSequence.create(sequence).get();

                                            if ((codeSeq.codeType == CodeType.CS && boyanMenu.CScb.isSelected())
                                                    || (codeSeq.codeType == OSNO && boyanMenu.OSNOcb.isSelected())
                                                    || (codeSeq.codeType == CodeType.OSO && boyanMenu.OSOcb.isSelected())) {
                                                final Optional<Color> optColor;
                                                optColor = Optional.empty();

                                                map.put(codeSeq, optColor);

                                                final Optional<String[]> lineOptIter = Optional.empty();
                                                optIter.put(codeSeq, lineOptIter);
                                            }
                                        } else if (sections.length == 3 && boyanMenu.Triplescb.isSelected()) {
                                            ClassifiedCodeSequence[] triple = new ClassifiedCodeSequence[3];

                                            for (int i = 0; i < 3; i++) {
                                                final String sequenceString = sections[i].trim();
                                                final ImmutableIntList sequence = Utils.splitString(sequenceString).get();

                                                final ClassifiedCodeSequence codeSeq =
                                                        ClassifiedCodeSequence.create(sequence).get();

                                                // Zhao Yu Li, May 13, 2025.
                                                // We may not be reading triples, but the stable components of a
                                                // triple may be useful to us, and should be added to the stables text
                                                // box of the cover.
                                                if ((codeSeq.codeType == CodeType.CS && boyanMenu.CScb.isSelected())
                                                        || (codeSeq.codeType == OSNO && boyanMenu.OSNOcb.isSelected())
                                                        || (codeSeq.codeType == CodeType.OSO && boyanMenu.OSOcb.isSelected())) {
                                                    final Optional<Color> optColor = Optional.empty();

                                                    map.put(codeSeq, optColor);

                                                    final Optional<String[]> lineOptIter = Optional.empty();
                                                    optIter.put(codeSeq, lineOptIter);
                                                }

                                                // On the other hand, if we are reading triples, we need a structure
                                                // that preserves the triples relationship of its three components
                                                triple[i] = codeSeq;
                                            }

                                            triples.add(triple);
                                        }
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }

                                final Tuple3<Optional<Rectangle>, Map<ClassifiedCodeSequence, Optional<Color>>,
                                        Map<ClassifiedCodeSequence, Optional<String[]>>> tup =
                                        new Tuple3<>(Optional.empty(), map, optIter);

                                drawCodes(tup, triples, executor, false, polyOpt.get(), true);
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Finished loading triples from directory.");
            } else {
                System.out.println("No directory selected.");
            }
        });

        parallelogramButton.setText("Para");
        parallelogramButton.setTooltip(Utils.toolTip("Load code sequences from a file, but only draw"
                + " them if they intersect a specified parallelogram"));
        Utils.colorButton(parallelogramButton, Color.LIGHTPINK, clickColor);

        parallelogramButton.setOnAction(event -> {

            final Optional<ConvexPolygon> polyOpt = new Parallelogram().getParallelogram();

            if (!polyOpt.isPresent()) {
                return;
            }
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Parallelogram File");
            final File file = fileChooser.showOpenDialog(mainWindow);

            LoadFileAction(polyOpt.get(), false, file, executor);
        });

        mergeButton.setText("Merge");
        mergeButton.setOnAction(e -> {

            final DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choose a Directory of Covers");

            final File dir = chooser.showDialog(mainWindow);

            if (dir != null) {

                final List<String> coverDirs = new FastList<>();

                for (final File file : dir.listFiles()) {
                    if (file.isDirectory()) {
                        coverDirs.add(file.getPath());
                    }
                }

                final File cover = new File("cover");

                // Create it if it doesn't exist
                cover.mkdir();

                // Delete everything in the cover dir before hand
                for (final File file : cover.listFiles()) {
                    file.delete();
                }

                // Just save it to the cover directory for now
                Wrapper.mergeCovers(cover.getPath(), coverDirs, pool);

                // Loading is sloooooow, so don't do that now
                //loadCover("cover", executor);
            }

        });

        newPolyTrimBtn.setText("Trimmer");
        newPolyTrimBtn.setTooltip(Utils.toolTip("Open up a window that allows you to trim codes and covers and files"));
        Utils.colorButton(newPolyTrimBtn, Color.LIGHTPINK, clickColor);
        newPolyTrimBtn.setOnAction(event -> {

            final Rectangle screen = map.getViewRectangle();
            new PolyTrimmer(screen, pool, onScreenSequences, saveColors.isSelected());
        });

        covRectsColorBox.setText("Black");
        covRectsColorBox.setTooltip(Utils.toolTip("The color used by the cover squares"));
        covRectsColorBox.setPrefWidth(95);
        covRectsColorBox.setOnAction(event -> {
            final int mouseY = MouseInfo.getPointerInfo().getLocation().y + 20;
            final int mouseX = MouseInfo.getPointerInfo().getLocation().x;
            final ColorPicker picker = new ColorPicker(mouseX, mouseY);
            final Optional<Color> opt = picker.pickColor();
            opt.ifPresent(color -> {
                coverColor = color;
                covRectsColorBox.setText(Colors.colorMap.get(color).get());
            });
        });

        zoomRegionButton.setText("Zoom To");
        zoomRegionButton.setTooltip(Utils.toolTip(
                "Zoom to fit the last region loaded, and changs that region's color to highlight it"));
        Utils.colorButton(zoomRegionButton, Color.SKYBLUE, clickColor);

        zoomRegionButton.setOnAction(event -> {
            boyanRdoBtn.setSelected(false);
            // get the code sequence associated with the current code numbers
            // find the min/max x and y for each of the things in there, and
            // then set
            final ClassifiedCodeSequence codeSeq =
                    ClassifiedCodeSequence.create(currentCodeNumbers[0]).get();

            try {
                final Storage storage = Database.loadStorage(codeSeq, pool).get();
                if (storage.classCodeSeq.stable) {
                    if (calculateChooser.getValue().equals("Region")) {
                        addToOnScreenSequences(storage, zoomColor);
                        renderRegion(
                                storage, (WritableImage) regionsImageView.getImage(), zoomColor);
                    }
                } else {
                    addToOnScreenSequences(storage, zoomColor);
                    renderRegion(storage, (WritableImage) regionsImageView.getImage(), zoomColor);
                }

                final String xMin = String.valueOf(Math.toDegrees(storage.getMinX()));
                final String xMax = String.valueOf(Math.toDegrees(storage.getMaxX()));
                final String yMin = String.valueOf(Math.toDegrees(storage.getMinY()));
                final String yMax = String.valueOf(Math.toDegrees(storage.getMaxY()));

                xMinTextField.setText(xMin);
                xMaxTextField.setText(xMax);

                yMinTextField.setText(yMin);
                yMaxTextField.setText(yMax);

                zoomAction(executor);
            } catch (final NoSuchElementException e) {
                throw new RuntimeException("Error: pressed zoom region on an empty set");
            }

        });

        zoomColorButton.setText("Red");
        zoomColorButton.setTooltip(Utils.toolTip("The color used by the zoom region button"));
        zoomColorButton.setPrefWidth(100);
        zoomColorButton.setOnAction(event -> {
            final int mouseY = MouseInfo.getPointerInfo().getLocation().y + 20;
            final int mouseX = MouseInfo.getPointerInfo().getLocation().x;
            final ColorPicker picker = new ColorPicker(mouseX, mouseY);
            final Optional<Color> opt = picker.pickColor();
            opt.ifPresent(color -> {
                zoomColor = color;
                zoomColorButton.setText(Colors.colorMap.get(color).get());
            });
        });

        zoomButton.setText("Zoom");
        zoomButton.setTooltip(Utils.toolTip(
                "Zoom to the interval specified. Note, if the interval is not a square, it will zoom"
                        + " to the best fitting square of that interval. You may set the minX, maxX equal and"
                        + " minY, maxY equal. If so, the program just centers those coordinates"));
        Utils.colorButton(zoomButton, Color.SKYBLUE, clickColor);
        //zoomButton.setOnAction(event -> boyanRdoBtn.setSelected(true));
        zoomButton.setOnAction(event -> {
            boyanRdoBtn.setSelected(true);//george fix the zoom to the center aug13th,2021
            zoomAction(executor);
        });

        keepPolys.getItems().addAll("None", "One", "Two", "Any");
        keepPolys.setPrefWidth(85);
        keepPolys.setStyle(textBoxColor);

        keepPolys.setTooltip(Utils.toolTip("When using the cover, you can have more than "
                + "one polygon bound shown."));
        keepPolys.setValue("Any");


        coverInfoBtn.setText("CoverInfo");
        Utils.colorButton(lookAtMeButton, Color.LIGHTPINK, clickColor);
        coverInfoBtn.setOnAction(event -> new CoverInfoWindow(windowTitle).show());

        final HBox V3Hbox = new HBox();
        V3Hbox.setSpacing(10);
        V3Hbox.setPadding(new Insets(0, 10, BTNPADBOTTOM, 0));
        V3Hbox.setAlignment(Pos.CENTER);
        V3Hbox.getChildren().addAll(compareCheckBox, saveV3Btn);

        compareCheckBox.setText("Match V3");
        compareCheckBox.setTooltip(Utils.toolTip("Use to compare codes printed after doing Vary3 on left and right of a line."));
        compareCheckBox.setOnAction(event -> {
            if (compareCheckBox.isSelected()) {
                BoyanMenu.compare = true;
                BoyanMenu.cList.clear();
            }
            else {
                BoyanMenu.compare = false;
            }
        });
        saveV3Btn.setText("Save V3");
        saveV3Btn.setTooltip(Utils.toolTip("Click to save the matching code sequence to a file"));
        Utils.colorButton(saveV3Btn, Color.LIGHTBLUE, clickColor);
        saveV3Btn.setOnAction(event -> new SaveV3Window("Save matching pairs").show());


        coverColorCycle.setText("Cycle");
        coverColorCycle.setTooltip(Utils.toolTip("Cycle throug the top row of colors when clicking "
                + "on the cover rectangles"));
        coverColorCycle.setStyle(textBoxColor);

        selectRdoBtn.setText("Select");
        selectRdoBtn.setSelected(true);
        magnifyRdoBtn.setText("Magnify");
        demagnifyRdoBtn.setText("Demagnify");
        centerBtn.setText("Center");
        selectRdoBtn.setStyle(textBoxColor);
        magnifyRdoBtn.setStyle(textBoxColor);
        demagnifyRdoBtn.setStyle(textBoxColor);
        centerBtn.setStyle(textBoxColor);
        centerBtn.setTooltip(Utils.toolTip("If you click the screen while this is selected, it will "
                + "pan so that the point you clicked is the new center of the screen"));

        selectRdoBtn.setToggleGroup(magnifyGroup);
        magnifyRdoBtn.setToggleGroup(magnifyGroup);
        demagnifyRdoBtn.setToggleGroup(magnifyGroup);
        centerBtn.setToggleGroup(magnifyGroup);

        // The topImageView is transparent and intercepts all the mouse events. This method allows
        // us to click on the transparent parts
        topImageView.setPickOnBounds(true);

        // handle panning and clicking events here
        topImageView.setOnMousePressed(event -> {
            final double initX = event.getX();
            final double initY = event.getY();
            final ImageView initLine = new ImageView();
            imageStack.getChildren().add(4, initLine);

            topImageView.setOnMouseDragged(event2 -> {
                final double finX = event2.getX();
                final double finY = event2.getY();
                final Line panLine = new Line(initX, initY, finX, finY);
                panLine.setStroke(panColor);
                imageStack.getChildren().remove(4);
                imageStack.getChildren().add(4, panLine);
                imageStack.getChildren().get(4).setTranslateX((finX + initX - SIDE) / 2);
                imageStack.getChildren().get(4).setTranslateY((finY + initY - SIDE) / 2);
                boyanMenu.dragIntend = true;
            });
            topImageView.setOnMouseReleased(event3 -> {
                imageStack.getChildren().remove(4);
                pan(initX, initY, event3.getX(), event3.getY(), executor);
                if (BoyanMenu.compare && selectRdoBtn.isSelected() && !boyanMenu.dragIntend) {
                    boyanMenu.vary3Btn.fire();
                }
                boyanMenu.dragIntend = false; // Disable matchv3 on drag Austin 2024-05-03
            });
        });

        textXLabel.setText("X:");
        textXField.setEditable(false);

        textYLabel.setText("Y:");
        textYField.setEditable(false);

        // Lock
        textXLockLabel.setText("X:");
        textXLockField.setEditable(false);

        textYLockLabel.setText("Y:");
        textYLockField.setEditable(false);

        textXRadianLabel.setText("X:");
        textXRadianField.setEditable(false);

        textYRadianLabel.setText("Y:");
        textYRadianField.setEditable(false);


        topImageView.setOnMouseMoved(event -> {

            final double radianX = map.radianX(event.getX() + 0.5);
            final double radianY = map.radianY(event.getY() + 0.5);

            final double degreeX = Math.toDegrees(radianX);
            final double degreeY = Math.toDegrees(radianY);

            textXField.setText(Double.toString(degreeX));
            textYField.setText(Double.toString(degreeY));

            //textXRadianField.setText(Double.toString(degreeX / 90));
            //textYRadianField.setText(Double.toString(degreeY / 90));
            textXRadianField.setText(Double.toString(radianX));
            textYRadianField.setText(Double.toString(radianY));

        });

        // 'boundsCheckBox', 'labelMainWindow', autofiller have no place right now

        // These next 9 blocks are for the navigation menu, they are always shown

        final HBox whatMenuHBox = new HBox(10, marinovRdoBtn, boyanRdoBtn);
        whatMenuHBox.setPadding(new Insets(10, 10, 0, 0));
        whatMenuHBox.setAlignment(Pos.CENTER);

        final HBox minHBox = new HBox();
        minHBox.setSpacing(0);
        minHBox.getChildren().addAll(xMinTextField, yMinTextField);
        minHBox.setPadding(new Insets(0, 10, BTNPADBOTTOM, 0));
        minHBox.setAlignment(Pos.CENTER);

        final HBox maxHBox = new HBox();
        maxHBox.setSpacing(0);
        maxHBox.getChildren().addAll(xMaxTextField, yMaxTextField);
        maxHBox.setPadding(new Insets(0, 10, 0, 0));
        maxHBox.setAlignment(Pos.CENTER);

        final VBox zoomFeildsVBox = new VBox();
        zoomFeildsVBox.setSpacing(10);
        zoomFeildsVBox.getChildren().addAll(zoomButton,minHBox, maxHBox);
        zoomFeildsVBox.setPadding(new Insets(0, 0, 0, 0));
        zoomFeildsVBox.setAlignment(Pos.CENTER);


        //zoomHBox.setAlignment(Pos.CENTER);

        final HBox boyanZoomHBox = new HBox();
        boyanZoomHBox.setSpacing(10);
        boyanZoomHBox.setPadding(new Insets(0, 10, 0, 0));
        boyanZoomHBox.setAlignment(Pos.CENTER);

        final HBox boyanMenuExtra = new HBox();
        boyanMenuExtra.setSpacing(10);
        boyanMenuExtra.setPadding(new Insets(0, 10, 0, 0));
        boyanMenuExtra.setAlignment(Pos.CENTER);

        final HBox coverExtraHBox = new HBox();
        coverExtraHBox.setSpacing(10);
        coverExtraHBox.setPadding(new Insets(0, 10, 0, 0));
        coverExtraHBox.setAlignment(Pos.CENTER);

        final HBox clickActionHBox = new HBox();
        clickActionHBox.setSpacing(10);
        clickActionHBox.getChildren().addAll(selectRdoBtn, magnifyRdoBtn, demagnifyRdoBtn, centerBtn);
        clickActionHBox.setPadding(new Insets(0, 0, BTNPADBOTTOM, 0));
        clickActionHBox.setAlignment(Pos.CENTER);

        final VBox zoomHBox = new VBox();
        zoomHBox.setSpacing(10);
        zoomHBox.getChildren().addAll(zoomFeildsVBox,clickActionHBox);
        zoomHBox.setPadding(new Insets(0, 10, 0, 50));
        //zoomHBox.setAlignment(Pos.CENTER);


        final HBox backForthHBox = new HBox();
        backForthHBox.setSpacing(10);
        backForthHBox.getChildren().addAll(
                zoomScaleLabel, zoomScaleText, backwardSquareButton, forwardSquareButton);
        backForthHBox.setPadding(new Insets(0, 10, BTNPADBOTTOM, 0));
        backForthHBox.setAlignment(Pos.CENTER);

        final HBox twoHBox = new HBox();
        twoHBox.setSpacing(10);
        //twoHBox.getChildren().addAll(txtCodeSequence, btnCalculate, calculateChooser);
        twoHBox.setPadding(new Insets(10, 10, BTNPADBOTTOM, 0));
        twoHBox.setAlignment(Pos.CENTER);

        final HBox colorsHBox1 = new HBox();
        colorsHBox1.setSpacing(10);
        //colorsHBox1.getChildren().addAll(cboxRegionColor0, cboxRegionColor1, cboxRegionColor2, clearBtn, resetBtn);//george may 2,2019
        colorsHBox1.setPadding(new Insets(0, 10, BTNPADBOTTOM, 0));
        colorsHBox1.setAlignment(Pos.CENTER);

        // the Marinov menu:
        final HBox zeroHBox = new HBox();
        zeroHBox.setSpacing(10);
        //zeroHBox.getChildren().addAll(
        //infoButton,lookAtMeButton, classifyBtn, btnLoadFile, loadLRCheckBox, iterationStart, iterationEnd);
        //zeroHBox.getChildren().addAll(
        //      infoButton,lookAtMeButton, classifyBtn, btnLoadFile, iterationStart, iterationEnd);
        //zeroHBox.getChildren().addAll(
        // lookAtMeButton, classifyBtn, btnLoadFile, iterationStart, iterationEnd); // george july 13th remove the row part1 /3
        zeroHBox.setPadding(new Insets(0, 10, BTNPADBOTTOM, 0));
        zeroHBox.setAlignment(Pos.CENTER);

        final HBox oneHBox = new HBox();
        oneHBox.setSpacing(10);
        //oneHBox.getChildren().addAll(saveColors, saveRegionsCheckBox, drawPictureCheckBox);
        oneHBox.setPadding(new Insets(0, 10, BTNPADBOTTOM, 0));
        oneHBox.setAlignment(Pos.CENTER);

        final HBox checkHBox = new HBox();
        checkHBox.setSpacing(10);
        //checkHBox.getChildren().addAll(
        //  reflectCheckBox, allCheckBox, polyLoadButton, parallelogramButton, mergeButton);
        //checkHBox.getChildren().addAll(
        // polyLoadButton, parallelogramButton, mergeButton);
        checkHBox.getChildren().addAll(
                mergeButton);// george july13th get rid of two button
        checkHBox.setPadding(new Insets(0, 10, BTNPADBOTTOM, 0));
        checkHBox.setAlignment(Pos.CENTER);

        final HBox zoomRegionHBox = new HBox();
        zoomRegionHBox.setSpacing(10);
        //zoomRegionHBox.getChildren().addAll(
        //newPolyTrimBtn, zoomRegionButton, zoomColorButton, autoFillerCheckBox);
        //zoomRegionHBox.getChildren().addAll(
        //   newPolyTrimBtn, zoomRegionButton, zoomColorButton);
        final Button info2Button= new Button();
        info2Button.setText("Info2");
        Utils.colorButton(info2Button, Color.LIGHTPINK, clickColor);
        info2Button.setOnAction(event -> new InfoWindow2(windowTitle, pool).show());
        zoomRegionHBox.setPadding(new Insets(0, 10, BTNPADBOTTOM, 0));
        zoomRegionHBox.setAlignment(Pos.CENTER);

        /*final HBox fillsHBox = new HBox();
        fillsHBox.setSpacing(10);
        fillsHBox.getChildren().addAll(
            fillScreenBtn, clearFillsBtn, saveFillBtn, loadFillBtn, showFillsCheckBox);
        fillsHBox.setPadding(new Insets(0, 10, 10, 0));
        fillsHBox.setAlignment(Pos.CENTER);*/

        final HBox oboHBox = new HBox();
        oboHBox.setSpacing(10);
        //oboHBox.getChildren().addAll(btnLoadOBOFile, lineNumberTxt, btnGo, holeFinderButton);
        oboHBox.getChildren().addAll(infoButton, btnLoadOBOFile, lineNumberTxt, btnGo);


        oboHBox.setPadding(new Insets(0, 10, BTNPADBOTTOM, 0));
        oboHBox.setAlignment(Pos.CENTER);

        // final HBox backForOBOHBox = new HBox(10, btnOBOBackward, fieldOBOStep, btnOBOForward);
        final HBox backForOBOHBox = new HBox(10, stablesButton, btnOBOBackward, fieldOBOStep, btnOBOForward);

        backForOBOHBox.setPadding(new Insets(0, 10, BTNPADBOTTOM, 0));
        backForOBOHBox.setAlignment(Pos.CENTER);

        final HBox hbox1 = new HBox();
        hbox1.setSpacing(10);
        hbox1.setPadding(new Insets(0, 10, BTNPADBOTTOM, 0));
        hbox1.setAlignment(Pos.CENTER);

        final HBox formulaBox = new HBox();
        formulaBox.setSpacing(10);
        formulaBox.setPadding(new Insets(0, 10, BTNPADBOTTOM, 0));
        formulaBox.setAlignment(Pos.CENTER);

        final TextField radiusBox =new TextField();
        final TextField centerBox =new TextField();
        final TextField codeBox =new TextField();
        centerBox.setPrefWidth(60);
        radiusBox.setPrefWidth(60);
        final Button cal =new Button();

        //boolean isChecked=false;

        radiusBox.setPromptText("radius");
        centerBox.setPromptText("center");
        codeBox.setPromptText("code sequence");
        cal.setText("calculate");
        //formulaBox.getChildren().addAll(codeBox,radiusBox,centerBox,cal);
        cal.setOnAction(event -> {
            String codeString = codeBox.getText();
            final Optional<ImmutableIntList> optional = Utils.splitString(codeString);
            if (optional.isPresent()){
                ImmutableIntList numList = optional.get();
                ClassifiedCodeSequence code = ClassifiedCodeSequence.create(numList).get();
                final Optional<Storage> opt1 = Database.loadStorage(code, pool);
                if (opt1.isPresent()){
                    final Optional<InfoAll> opt2 = Wrapper.loadAllEquation(code,pool);
                    if (opt2.isPresent()){
                        final InfoAll all = opt2.get();
                        String allSin = all.leftRights;
                        String allCos = all.codeSeqLR;
                        String[] center= centerBox.getText().split(" ");

                        double radianx = Math.toRadians(Double.parseDouble(center[0]));
                        double radiany = Math.toRadians(Double.parseDouble(center[1]));
                        Vector2 centerList= Vector2.create(radianx,radiany);
                        double radius_r =Math.toRadians(Double.parseDouble(radiusBox.getText()));
                        Utils.calculate_formula(allSin,allCos,radius_r,centerList);
                    }
                }

            }

        });



        final HBox hbox2 = new HBox();
        hbox2.setSpacing(10);
        hbox2.setPadding(new Insets(0, 10, 10, 0));
        hbox2.setAlignment(Pos.CENTER);

        //final HBox proverHBox = new HBox(10, proverCheckBox, offsetTextField, loadCoverButton);
        //final HBox proverHBox = new HBox(10 , loadCoverButton); //george july13th get rid of the prover and offset

        //proverHBox.setPadding(new Insets(0, 10, 10, 0));
        //proverHBox.setAlignment(Pos.CENTER);

        final HBox labelMainWindowHBox = new HBox();
        labelMainWindowHBox.setSpacing(10);
        //labelMainWindowHBox.getChildren().addAll(keepPolys, covRectsColorBox, coverColorCycle, coverBtn);
        //labelMainWindowHBox.getChildren().addAll(keepPolys, covRectsColorBox, coverColorCycle);
        labelMainWindowHBox.setPadding(new Insets(0, 10, 10, 0));
        labelMainWindowHBox.setAlignment(Pos.CENTER);



        final VBox marinovMenuVBox = new VBox();
        marinovMenuVBox.setSpacing(8);
        //marinovMenuVBox.getChildren().addAll(zeroHBox, oneHBox, checkHBox,
        //                                   zoomRegionHBox, fillsHBox, proverHBox, labelMainWindowHBox);
        // marinovMenuVBox.getChildren().addAll(zeroHBox, oneHBox, checkHBox,
        //       zoomRegionHBox, proverHBox, labelMainWindowHBox);// george july 13th remove the row part2/3
        //marinovMenuVBox.getChildren().addAll(oneHBox, checkHBox,
        //      zoomRegionHBox, proverHBox, labelMainWindowHBox);
        //marinovMenuVBox.getChildren().addAll(oneHBox,
        //           zoomRegionHBox, proverHBox, labelMainWindowHBox);
        // marinovMenuVBox.getChildren().addAll(oneHBox,
        //                  proverHBox, labelMainWindowHBox);
        marinovMenuVBox.getChildren().addAll(oneHBox,
                labelMainWindowHBox);

        final HBox varyMenuPane = new HBox();
        varyMenuPane.setAlignment(Pos.CENTER);
        varyMenuPane.setPrefWidth(375);
        varyMenuPane.setPrefHeight(292);
        varyMenuPane.getChildren().add(marinovMenuVBox);

        //final VBox leftVBox = new VBox(10, whatMenuHBox, twoHBox, colorsHBox1, varyMenuPane, oboHBox,
        // backForOBOHBox, zoomHBox, clickActionHBox, backForthHBox);
        //final VBox leftVBox = new VBox(10, whatMenuHBox, twoHBox, colorsHBox1, varyMenuPane, oboHBox,
        //      backForOBOHBox,zoomHBox, clickActionHBox, backForthHBox);
        hbox2.getChildren().addAll(reflectCheckBox, allCheckBox, infoButton, polyLoadButton, polyLoadDBButton, parallelogramButton);

        final VBox leftVBox = new VBox(10, twoHBox,hbox2, colorsHBox1, varyMenuPane, oboHBox,
                backForOBOHBox,zoomHBox, clickActionHBox, backForthHBox);
        colorsHBox1.getChildren().clear();
        backForOBOHBox.getChildren().clear();
        clickActionHBox.getChildren().clear();
        zoomHBox.getChildren().clear();
        backForthHBox.getChildren().clear();
        oboHBox.getChildren().clear();
        twoHBox.getChildren().clear();


        final Label mouseCoordinatesLabel = new Label("Mouse Coordinates");

        textXHBox.getChildren().addAll(textXLabel, textXField);
        textXHBox.setSpacing(10);

        textYHBox.getChildren().addAll(textYLabel, textYField);
        textYHBox.setSpacing(10);

        textXLockHBox.getChildren().addAll(textXLockLabel, textXLockField);
        textXLockHBox.setSpacing(10);

        textYLockHBox.getChildren().addAll(textYLockLabel, textYLockField);
        textYLockHBox.setSpacing(10);

        textXRadianHBox.getChildren().addAll(textXRadianLabel, textXRadianField);
        textXRadianHBox.setSpacing(10);

        textYRadianHBox.getChildren().addAll(textYRadianLabel, textYRadianField);
        textYRadianHBox.setSpacing(10);



        final ScrollPane seqScroll = new ScrollPane();
        seqScroll.setPrefSize(170, 465);
        seqScroll.setContent(codeSequencesGPane);
        final VBox rightVBox = new VBox(10, mouseCoordinatesLabel, textXHBox, textYHBox,
                textXLockHBox, textYLockHBox, textXRadianHBox,
                textYRadianHBox, seqScroll);
        rightVBox.setMinWidth(200);

        // There are sort of two layers. There are all the gui elements that the
        // user interacts with. However, you can't directly use these when programming
        // So, you have variables behind the scene that represent the state of the gui.
        // Whenever the gui changes, these variables are updated automatically. Note
        // that all these changes must occur in one thread, since gui elements can
        // only be modified within the application thread

        marinovRdoBtn.setText("Marinov Menu");
        marinovRdoBtn.setSelected(true);
        marinovRdoBtn.setOnAction(event -> {
            zoomFeildsVBox.getChildren().clear();
            minHBox.getChildren().clear();
            maxHBox.getChildren().clear();
            zoomHBox.getChildren().clear();
            labelMainWindowHBox.getChildren().clear();
            zeroHBox.getChildren().clear();
            colorsHBox1.getChildren().clear();
            twoHBox.getChildren().clear();
            oboHBox.getChildren().clear();


            minHBox.getChildren().addAll(xMinTextField, yMinTextField);
            maxHBox.getChildren().addAll(xMaxTextField, yMaxTextField);
            zoomFeildsVBox.getChildren().addAll(minHBox, maxHBox);
            //zoomHBox.getChildren().addAll(zoomButton, zoomFeildsVBox);
            //labelMainWindowHBox.getChildren().addAll(keepPolys, covRectsColorBox, coverColorCycle, coverBtn);
            //labelMainWindowHBox.getChildren().addAll(keepPolys, covRectsColorBox, coverColorCycle);

            //zeroHBox.getChildren().addAll(infoButton, classifyBtn, btnLoadFile, loadLRCheckBox, iterationStart, iterationEnd);
            //zeroHBox.getChildren().addAll( classifyBtn, btnLoadFile, iterationStart, iterationEnd); //// george july 13th remove the row part3/3

            varyMenuPane.getChildren().clear();
            varyMenuPane.getChildren().add(marinovMenuVBox);
        });
        boyanRdoBtn.setText("Boyan Menu");
        /*boyanRdoBtn.setOnAction(event -> {

        	zoomFeildsVBox.getChildren().clear();
        	boyanZoomHBox.getChildren().clear();
        	boyanMenuExtra.getChildren().clear();
        	twoHBox.getChildren().clear();


            boyanZoomHBox.getChildren().addAll(zoomButton, xMinTextField, yMinTextField);
            //boyanMenuExtra.getChildren().addAll(coverBtn, btnLoadFile, loadLRCheckBox);

            boyanMenuExtra.getChildren().addAll(keepPolys, covRectsColorBox, coverColorCycle, coverBtn, loadHolesBtn, btnLoadFile);
            zoomFeildsVBox.getChildren().addAll(boyanZoomHBox, boyanMenuExtra);

            zoomHBox.getChildren().clear();
            clickActionHBox.getChildren().clear();
            backForthHBox.getChildren().clear();
            oboHBox.getChildren().clear();
            backForOBOHBox.getChildren().clear();
            colorsHBox1.getChildren().clear();
            zoomRegionHBox.getChildren().clear();

            zoomRegionHBox.getChildren().addAll(
                    mergeButton,loadCoverButton,newPolyTrimBtn, zoomRegionButton, zoomColorButton);
            backForOBOHBox.getChildren().addAll(btnOBOBackward, btnOBOForward, oboCBoxColor);
            clickActionHBox.getChildren().addAll(selectRdoBtn, magnifyRdoBtn, demagnifyRdoBtn, centerBtn);
            twoHBox.getChildren().addAll(txtCodeSequence, btnCalculate, calculateChooser);

            backForthHBox.getChildren().addAll(
                    zoomScaleLabel, zoomScaleText, backwardSquareButton, forwardSquareButton);
            oboHBox.getChildren().addAll(reflectCheckBox, allCheckBox,infoButton,btnLoadOBOFile, lineNumberTxt, btnGo);
            colorsHBox1.getChildren().addAll(cboxRegionColor0, cboxRegionColor1, cboxRegionColor2, clearBtn, resetBtn);//george may 2,2019

            zoomHBox.getChildren().addAll( zoomRegionHBox,backForOBOHBox,oboHBox,zoomFeildsVBox,clickActionHBox,backForthHBox);

            varyMenuPane.getChildren().clear();
            varyMenuPane.getChildren().add(boyanMenu.wrapper);
        });*/

        zoomFeildsVBox.getChildren().clear();
        boyanZoomHBox.getChildren().clear();
        boyanMenuExtra.getChildren().clear();
        coverExtraHBox.getChildren().clear();
        twoHBox.getChildren().clear();
        zoomHBox.getChildren().clear();
        clickActionHBox.getChildren().clear();
        backForthHBox.getChildren().clear();
        oboHBox.getChildren().clear();
        backForOBOHBox.getChildren().clear();
        colorsHBox1.getChildren().clear();
        zoomRegionHBox.getChildren().clear();

        // Zhao Yu Li, May 06, 2025.
        // Moved buildPolyCheckBox from BoyanMenu.java to here.
        hbox1.getChildren().addAll(boyanMenu.buildPolyCheckBox, keepPolys, covRectsColorBox, coverColorCycle);
        //zoomRegionHBox.getChildren().addAll(
        // mergeButton,loadCoverButton,newPolyTrimBtn, zoomRegionButton, zoomColorButton);
        //zoomRegionHBox.getChildren().addAll(
        //      mergeButton,loadCoverButton, zoomRegionButton, zoomColorButton);//george july15th hide the trimmer button
        zoomRegionHBox.getChildren().addAll(tetrabarButton,  // Zhao Yu Li, May 15, 2025. Added tetrahdron button to the viewer
                mergeButton,loadCoverButton,calculateChooser);//george july15th hide the trimmer button and red button
        backForOBOHBox.getChildren().addAll(stablesButton, btnOBOBackward, fieldOBOStep, btnOBOForward);
        clickActionHBox.getChildren().addAll(selectRdoBtn, magnifyRdoBtn, demagnifyRdoBtn, centerBtn);
        twoHBox.getChildren().addAll(txtCodeSequence, btnCalculate, calculateAddToCoverCheckBox,
                zoomRegionButton, iterateToLimitBtn);
        boyanZoomHBox.getChildren().addAll(zoomButton, xMinTextField, yMinTextField);
        boyanMenuExtra.getChildren().addAll(loadDirectoryButton, coverBtn, loadHolesBtn, btnLoadFile, compareCheckBox, saveV3Btn);
        //coverExtraHBox.getChildren().addAll(halfTripleBtn, cornerBtn, unstableBtn);
        zoomFeildsVBox.getChildren().addAll(boyanZoomHBox, boyanMenuExtra);
        backForthHBox.getChildren().addAll(
                zoomScaleLabel, zoomScaleText, backwardSquareButton, forwardSquareButton);
        oboHBox.getChildren().addAll(smallCoverButton, patternCalculatorBtn, btnLoadOBOFile, lineNumberTxt, btnGo, updateButton);
        //colorsHBox1.getChildren().addAll(cboxRegionColor0, cboxRegionColor1, cboxRegionColor2, clearBtn, resetBtn);//george may 2,2019
        colorsHBox1.getChildren().addAll(cboxRegionColor0, cboxRegionColor1, clearBtn, resetBtn);//george july15th remove the third color option

        zoomHBox.getChildren().addAll( hbox1, zoomRegionHBox,oboHBox,backForOBOHBox,zoomFeildsVBox,clickActionHBox,backForthHBox);

        varyMenuPane.getChildren().clear();
        varyMenuPane.getChildren().add(boyanMenu.wrapper);
        marinovRdoBtn.setToggleGroup(menuGroup);
        boyanRdoBtn.setToggleGroup(menuGroup);

        comboBoxColors.put(0, Color.BLACK);
        comboBoxColors.put(1, Color.BLACK);
        //comboBoxColors.put(2, Color.BLACK);//george may 2,2019

        cboxRegionColor0.setText("Black");
        cboxRegionColor0.setTooltip(Utils.toolTip("Which color you want the regions to be drawn in. "
                + "The color of the regions drawn will alternate between the two colors chosen."));
        cboxRegionColor0.setPrefWidth(110);
        cboxRegionColor0.setOnAction(event -> {
            final int mouseY = MouseInfo.getPointerInfo().getLocation().y + 20;
            final int mouseX = MouseInfo.getPointerInfo().getLocation().x;
            final ColorPicker picker = new ColorPicker(mouseX, mouseY);
            final Optional<Color> opt = picker.pickColor();
            opt.ifPresent(color -> {
                comboBoxColors.put(0, color);
                cboxRegionColor0.setText(Colors.colorMap.get(color).get());
            });
        });

        cboxRegionColor1.setText("Black");
        cboxRegionColor1.setTooltip(Utils.toolTip("Which color you want the regions to be drawn in. "
                + "The color of the regions drawn will alternate between the two colors chosen."));
        cboxRegionColor1.setPrefWidth(110);
        cboxRegionColor1.setOnAction(event -> {
            final int mouseY = MouseInfo.getPointerInfo().getLocation().y + 20;
            final int mouseX = MouseInfo.getPointerInfo().getLocation().x;
            final ColorPicker picker = new ColorPicker(mouseX, mouseY);
            final Optional<Color> opt = picker.pickColor();
            opt.ifPresent(color -> {
                comboBoxColors.put(1, color);
                cboxRegionColor1.setText(Colors.colorMap.get(color).get());
            });
        });
        /*////george may 2,2019 start
        cboxRegionColor2.setText("Black");
        cboxRegionColor2.setTooltip(Utils.toolTip("Which color you want the regions to be drawn in. "
                                                  + "The color of the regions drawn will alternate between the two colors chosen."));
        cboxRegionColor2.setPrefWidth(110);
        cboxRegionColor2.setOnAction(event -> {
            final int mouseY = MouseInfo.getPointerInfo().getLocation().y + 20;
            final int mouseX = MouseInfo.getPointerInfo().getLocation().x;
            final ColorPicker picker = new ColorPicker(mouseX, mouseY);
            final Optional<Color> opt = picker.pickColor();
            opt.ifPresent(color -> {
                comboBoxColors.put(2, color);
                cboxRegionColor2.setText(Colors.colorMap.get(color).get());
            });
        });////george may 2,2019 end*/

        oboCBoxColor.setText("Red");
        oboCBoxColor.setTooltip(Utils.toolTip("The color used when you load an OBO file"));
        oboCBoxColor.setPrefWidth(100);
        oboCBoxColor.setOnAction(event -> {
            final int mouseY = MouseInfo.getPointerInfo().getLocation().y + 20;
            final int mouseX = MouseInfo.getPointerInfo().getLocation().x;
            final ColorPicker picker = new ColorPicker(mouseX, mouseY);
            final Optional<Color> opt = picker.pickColor();
            opt.ifPresent(color -> {
                currentOBOColor = color;
                oboCBoxColor.setText(Colors.colorMap.get(color).get());
            });
        });

        codeSequencesGPane.setPadding(new Insets(10));
        codeSequencesGPane.setHgap(10);
        codeSequencesGPane.setVgap(10);


        final BorderPane bpane = new BorderPane();
        bpane.setLeft(leftVBox);
        bpane.setCenter(imageStack);
        bpane.setRight(rightVBox);

        BorderPane.setAlignment(leftVBox, Pos.CENTER);
        BorderPane.setAlignment(rightVBox, Pos.CENTER);

        // BorderPane.setMargin(imageStack, new Insets(10));

        // Scene
        final Scene scene = new Scene(bpane);

        // Stage
        mainWindow.setTitle(windowTitle);
        mainWindow.setOnCloseRequest(event -> {
            try {
                closeChildWindowsForExit();
            } catch (final RuntimeException e) {
                e.printStackTrace();
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Save Failed");
                alert.setHeaderText("Could not save window state");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
                event.consume();
                return;
            }

            // close all the windows
            System.out.println("Send close request");
            Platform.exit();
        });
        mainWindow.setScene(scene);
    }

    private void closeChildWindowsForExit() {
        // JavaFX does not run destructors for child windows. Close the viewer-owned stages explicitly so their text
        // buffers are saved before Platform.exit() tears down the application.
        coverWindow.close();
        smallCoverWindow.close();
        stablesWindow.close();
        iterationPolyWindow.close();
        queryStage.close();
        closeStage(codeWindow);

        if (iterateToLimitWindow != null) iterateToLimitWindow.close();
        if (cycleVaryWindow != null) cycleVaryWindow.close();
        if (varyWindow != null) varyWindow.close();
        if (middleVaryWindow != null) middleVaryWindow.close();
        if (autoPolyVaryWindow != null) autoPolyVaryWindow.close();
        if (superPolyVaryWindow != null) superPolyVaryWindow.close();

        // Some utility windows are intentionally created as throwaway objects, so the Viewer does not keep references
        // to them. Hide anything still open after the persistent windows have saved their state.
        for (final javafx.stage.Window window : new ArrayList<>(javafx.stage.Window.getWindows())) {
            if (window != mainWindow && window.isShowing()) {
                window.hide();
            }
        }
    }

    private static void closeStage(final Stage stage) {
        if (stage != null) {
            stage.close();
        }
    }

    // Do initial rendering
    public void start(final ExecutorService executor) {
        renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
        mainWindow.show();
        Platform.runLater(() -> {
            
        // If you used Option 2 (The Best Practice) from the previous answer:
        updateReflection();
        
        /* * OR, if you used Option 1 (The Quick Fix) and didn't make a new method:
            * reflectCheckBox.getOnAction().handle(null);
            */
    });
    }
    private void updateReflection() {
        if (reflectCheckBox.isSelected()) {
            reflectionTransform.setMyy(-1);
            reflectionTransform.setTy(SIDE);
            if (!imageStack.getTransforms().contains(reflectionTransform)) {
                imageStack.getTransforms().add(reflectionTransform);
            }
        } else {
            imageStack.getTransforms().remove(reflectionTransform);
        }
    }

    //expando code sequance
    public Array<ClassifiedCodeSequence> getExpandos(final MutableIntList workingNumbers,final int iteration,final int[] position,final String[] repeated_elements){

        final Array<ClassifiedCodeSequence> expandoCodeSeqs;
        final LinkedHashSet<ClassifiedCodeSequence> todo = new LinkedHashSet<>();

        for (int i = 0; i < iteration; ++i){
            for (int j = 0; j < position.length; ++j) {
                int k=position[j];
                for (String single_element : repeated_elements) {
                    workingNumbers.addAtIndex(k, Integer.parseInt(single_element));
                    k++;
                }
                for (int q = j+1; q < position.length; ++q){
                    position[q]=position[q]+repeated_elements.length;
                }
            }
            //increment whole position array by the length of the repeated_elements each iterations
            for (int f = 0; f < position.length; f++) {
                position[f]=position[f]+repeated_elements.length;
            }
            //increment whole position array by the length of the repeated_elements each iterations
            for (int f = 0; f < position.length; f++) {
                position[f]=position[f]+repeated_elements.length;
            }
            final Either<InvalidCodeSequence, ClassifiedCodeSequence> either =
                    ClassifiedCodeSequence.create(workingNumbers);

            if (either.isLeft()) {
                final InvalidCodeSequence errorCode = either.getLeft();
                if (errorCode != InvalidCodeSequence.NEGATIVE_OR_ZERO_NUMBERS) {
                    throw new RuntimeException("error code " + errorCode + " in iterations");
                }
            } else {
                final ClassifiedCodeSequence classCodeSeq = either.get();
                todo.add(classCodeSeq);
            }

        }
        //get the final codesequences Array and return it
        expandoCodeSeqs = Array.ofAll(todo);
        return expandoCodeSeqs ;
    }

    public String subtractLeftRight(String lr1,String lr2) {
        // Assumption: lr1 and lr2 have the same size
        String[] leftRights1 = lr1.split("\n");
        String[] leftRights2 = lr2.split("\n");
        String result = "";
        if (leftRights1.length != leftRights2.length){//xiu attemp to fix Crash #1 OSO example bug
            return null;
        }
        for (int i = 0; i < leftRights1.length ; i++) {
            String[] leftRight1 = leftRights1[i].split(" ");
            String[] leftRight2 = leftRights2[i].split(" ");
            for (int j = 0; j < leftRight1.length; j++) {
                int tempInt = Integer.parseInt(leftRight2[j]) - Integer.parseInt(leftRight1[j]);
                result += (Integer.toString(tempInt) + " ");
            }
            result += "~";
            result=result.replace(" ~", "");
            result += "\n";
        }
        result += "~";
        result=result.replace("\n~", "");
        return result;
    }

    public String addLeftRight(String lr1, String lr2) {
        // Assumption: lr1 and lr2 have the same size
        String[] leftRights1 = lr1.split("\n");
        String[] leftRights2 = lr2.split("\n");
        String result = "";
        for (int i = 0; i < leftRights1.length; i++) {
            String[] leftRight1 = leftRights1[i].split(" ");
            String[] leftRight2 = leftRights2[i].split(" ");
            for (int j = 0; j < leftRight1.length; j++) {
                int tempInt = Integer.parseInt(leftRight2[j]) + Integer.parseInt(leftRight1[j]);
                result += (Integer.toString(tempInt) + " ");
            }
            result += "~";
            result=result.replace(" ~", "");
            result += "\n";
        }
        result += "~";
        result=result.replace("\n~", "");
        return result;
    }

    public boolean isleftRightLegal(String leftRights) {
        // Assumption: lr1 and lr2 have the same size
        String[] leftRights1 = leftRights.split("\n");
        for (int i = 0; i < leftRights1.length; i++) {
            String[] leftRight2 = leftRights1[i].split(" ");
            for (int j = 0; j < leftRight2.length; j++) {
                int tempInt = Integer.parseInt(leftRight2[j]);
                if (tempInt < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public Array<ClassifiedCodeSequence> getMultipleExpandos(int iteration, String position, final String repeated_elements){
        final Array<ClassifiedCodeSequence> expandoCodeSeqs;
        final LinkedHashSet<ClassifiedCodeSequence> todo = new LinkedHashSet<>();

        final String[] elements = repeated_elements.trim().split(",");

        // Assumption 1: the length of elements is equal to the number of kinds of "position", and they are in corresponding ordering
        // (i.e., size({A, B, C}) == length(["6 6", "1 2 1 2", "1 3 3"]), and A = "6 6", ...)
        String firstCodeSeqStr = position;
        MutableIntList firstCodeSeq = new IntArrayList();
        String toBeReplaced;
        String toBeReplaced2;
        for (int i = 0; i < elements.length; i++) {
            toBeReplaced2= " " + Character.toString(((char) ('A' + i)));
            toBeReplaced = Character.toString(((char) ('A' + i))) + " ";
            firstCodeSeqStr = firstCodeSeqStr.replace(toBeReplaced, "");
            firstCodeSeqStr = firstCodeSeqStr.replace(toBeReplaced2, "");

        }
        for (String numberStr : firstCodeSeqStr.split(" ")) {
            firstCodeSeq.add(Integer.parseInt(numberStr));
        }
        final Either<InvalidCodeSequence, ClassifiedCodeSequence> either_first =
                ClassifiedCodeSequence.create(firstCodeSeq);
        if (either_first.isLeft()) {
            final InvalidCodeSequence errorCode = either_first.getLeft();
            if (errorCode != InvalidCodeSequence.NEGATIVE_OR_ZERO_NUMBERS) {
                throw new RuntimeException("error code " + errorCode + " in expandos");
            }
        } else {
            final ClassifiedCodeSequence classCodeSeq = either_first.get();
            todo.add(classCodeSeq);
        }


        while (iteration > 0) {
            MutableIntList codeSeq = new IntArrayList();
            String currentCodeSeqStr = position;

            for (int i = 0; i < elements.length; i++) {
                toBeReplaced = Character.toString(((char) ('A' + i)));

                String element = elements[i];
                String elementContinue = element + " " + toBeReplaced;

                currentCodeSeqStr = currentCodeSeqStr.replace(toBeReplaced, element);
                position = position.replace(toBeReplaced, elementContinue);
            }
            for (String numberStr : currentCodeSeqStr.split(" ")) {
                codeSeq.add(Integer.parseInt(numberStr));
            }

            final Either<InvalidCodeSequence, ClassifiedCodeSequence> either =
                    ClassifiedCodeSequence.create(codeSeq);

            if (either.isLeft()) {
                final InvalidCodeSequence errorCode = either.getLeft();
                if (errorCode != InvalidCodeSequence.NEGATIVE_OR_ZERO_NUMBERS) {
                    throw new RuntimeException("error code " + errorCode + " in expandos");
                }
            } else {
                final ClassifiedCodeSequence classCodeSeq = either.get();
                todo.add(classCodeSeq);
            }

            iteration--;
        }
        expandoCodeSeqs = Array.ofAll(todo);
        return expandoCodeSeqs;
/*
        final Array<ClassifiedCodeSequence> expandoCodeSeqs;
        final LinkedHashSet<ClassifiedCodeSequence> todo = new LinkedHashSet<>();

        for (int i = 0; i < iteration; ++i){
            for (int j = 0; j < position.length; ++j) {
                int k=position[j];
                for (String single_element : repeated_elements) {
                    workingNumbers.addAtIndex(k, Integer.parseInt(single_element));
                    k++;
                }
                for (int q = j+1; q < position.length; ++q){
                    position[q]=position[q]+repeated_elements.length;
                }
            }
            //increment whole position array by the length of the repeated_elements each iterations
            for (int f = 0; f < position.length; f++) {
                position[f]=position[f]+repeated_elements.length;
            }
            final Either<InvalidCodeSequence, ClassifiedCodeSequence> either =
                    ClassifiedCodeSequence.create(workingNumbers);

            if (either.isLeft()) {
                final InvalidCodeSequence errorCode = either.getLeft();
                if (errorCode != InvalidCodeSequence.NEGATIVE_OR_ZERO_NUMBERS) {
                    throw new RuntimeException("error code " + errorCode + " in iterations");
                }
            } else {
                final ClassifiedCodeSequence classCodeSeq = either.get();
                todo.add(classCodeSeq);
            }

        }
        //get the final codesequences Array and return it
        expandoCodeSeqs = Array.ofAll(todo);
        return expandoCodeSeqs ;*/
    }

    private Tuple2<Array<ClassifiedCodeSequence>, ArrayList<Storage>> iterateActionWithPolyIntersect(
            final MutableIntList workingNumbers, final String[] vectors, final int[] starts, final int[] ends,
            final int[] steps, final int num, final boolean print, final ExecutorService executor
    ) {
        Utils.copyInto(workingNumbers, currentCodeNumbers[num]);

        final ArrayList<ClassifiedCodeSequence> todo =
                iterateThru(workingNumbers, vectors, starts, ends, steps, 0, num);
        final Array<ClassifiedCodeSequence> classCodeSeqs = Array.ofAll(todo);

        final Task<Array<Storage>> task;
        final ExecutorService drawExecutor = Executors.newFixedThreadPool(Utils.numThreads);

        if (nolrRdoBtn.isSelected()) {
            task = new DrawPictureTask(classCodeSeqs, pool, drawExecutor, print, false);

        } else if (showlrRdoBtn.isSelected()) {
            task = new DrawPictureTaskShowLR(classCodeSeqs, pool);

        } else if (uselrRdoBtn.isSelected()) {
            task = new DrawPictureTaskUseLR(classCodeSeqs, pool);

        } else if (uselrTestBtn.isSelected()){
            task = new DrawPictureTaskUseLRTest(classCodeSeqs,pool);
        }
        else {
            throw new RuntimeException("No selected left right button");
        }

        ArrayList<Storage> storagesToReturn = new ArrayList<>();

        task.setOnSucceeded(e -> {
            try {
                synchronize();
            } catch (final NullPointerException exception) {
                // this is when were iterating from a file, and the iteration window isn't open
            }
            Utils.shutdownExecutorAsync(drawExecutor);
        });
        task.setOnCancelled(e -> {
            Utils.shutdownExecutorAsync(drawExecutor);
        });

        // If the task throws an exception during the call phase,
        // simply close the window and throw the exception
        task.setOnFailed(e -> {
            //progress.close();
            Utils.shutdownExecutorAsync(drawExecutor);
            throw new RuntimeException(task.getException());
        });

        Utils.runAndWait(task);

        // Zhao Yu Li, May 29, 2025.
        // Since TASK was executed synchronously, it is safe to GET the result of the task outside the onSucceed method
        final Array<Storage> storages;

        try {
            storages = task.get();
        } catch (InterruptedException | ExecutionException exception) {
            throw new RuntimeException(exception);
        }

        storages.forEach(storagesToReturn::add);

        Utils.printToFile("iterations.txt", storages);

        return Tuple.of(classCodeSeqs, storagesToReturn);
    }

    private Array<ClassifiedCodeSequence> iterateAction(final MutableIntList workingNumbers,
                                                            final String[] vectors, final int[] starts, final int[] ends, final int[] steps,
                                                            final int num, final boolean print, final ExecutorService executor) {

        Utils.copyInto(workingNumbers, currentCodeNumbers[num]);

        final ArrayList<ClassifiedCodeSequence> todo =
                iterateThru(workingNumbers, vectors, starts, ends, steps, 0, num);
        final Array<ClassifiedCodeSequence> classCodeSeqs = Array.ofAll(todo);
        //for (int i=0;i<classCodeSeqs.size();++i){
        //codeSequence.codeSequence.getClass();
        //insertRepeatedElement(workingNumbers.addAtIndex(i,););
        //System.out.println("xiuxiu"+classCodeSeqs.get(i).codeSequence);

        //}
        final Task<Array<Storage>> task;
        final ExecutorService drawExecutor = Executors.newFixedThreadPool(Utils.numThreads);

        if (nolrRdoBtn.isSelected()) {
            task = new DrawPictureTask(classCodeSeqs, pool, drawExecutor, print, false);

        } else if (showlrRdoBtn.isSelected()) {
            task = new DrawPictureTaskShowLR(classCodeSeqs, pool);

        } else if (uselrRdoBtn.isSelected()) {
            task = new DrawPictureTaskUseLR(classCodeSeqs, pool);

        } else if (uselrTestBtn.isSelected()){
            task = new DrawPictureTaskUseLRTest(classCodeSeqs,pool);
        }
        else {
            throw new RuntimeException("No selected left right button");
        }

        task.setOnSucceeded(e -> {

            final Array<Storage> storages;
            try {
                storages = task.get();
            } catch (InterruptedException | ExecutionException exception) {
                throw new RuntimeException(exception);
            }
            //ConnectionPool newPool = Admin.getConnectionPool(this.dbname, Utils.numThreads);

            storages.forEach(storage -> {

                final int index = cycle.get();
                final Color color = comboBoxColors.get(index);
                ClassifiedCodeSequence code= storage.classCodeSeq;
                addToOnScreenSequences(storage, color);
            });

            Utils.printToFile("iterations.txt", storages);

            // progress.close();

            try {
                synchronize();
            } catch (final NullPointerException exception) {
                // this is when were iterating from a file, and the iterations window isn't open
            }
            Utils.shutdownExecutorAsync(drawExecutor);
            renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
        });
        task.setOnCancelled(e -> {
            Utils.shutdownExecutorAsync(drawExecutor);
        });

        // If the task throws an exception during the call phase,
        // simply close the window and throw the exception
        task.setOnFailed(e -> {
            //progress.close();
            Utils.shutdownExecutorAsync(drawExecutor);
            throw new RuntimeException(task.getException());
        });

        Utils.runAndWait(task);

        //test for correctness with the no left rights as the standard
       /*for (ClassifiedCodeSequence code : classCodeSeqs){
            Optional<Info> opt_infoAll_1 = Wrapper.loadInfo(code, pool);
            String c = opt_infoAll_1.get().leftRights;
            Wrapper.deleteFromDatabase(code, pool);
            Database.loadStorage(code, pool);
            Optional<Info> opt_infoAll_2 = Wrapper.loadInfo(code, pool);
            String t = opt_infoAll_2.get().leftRights;
            if (!t.equals(c)){
                System.out.println("test failed for "+code);
                System.out.println("The correct left rights are "+ "\n"+ t+ "\nthe calculated one are "+ "\n"+c);
                break;
            }
        }*/

        //progress.show();

        return classCodeSeqs;
    }

    private ArrayList<ClassifiedCodeSequence> iterateThru(
            final MutableIntList workingNumbers, final String[] vectors, final int[] starts,
            final int[] ends, final int[] steps, final int depth, final int num) {

        final ArrayList<ClassifiedCodeSequence> todo = new ArrayList<>();
        if (depth >= vectors.length || vectors[depth].isEmpty()) {
            final Either<InvalidCodeSequence, ClassifiedCodeSequence> either =
                    ClassifiedCodeSequence.create(workingNumbers);

            if (either.isLeft()) {
                final InvalidCodeSequence errorCode = either.getLeft();
                if (errorCode != InvalidCodeSequence.NEGATIVE_OR_ZERO_NUMBERS) {
                    throw new RuntimeException("error code " + errorCode + " in iterations");
                }
            } else {
                final ClassifiedCodeSequence classCodeSeq = either.get();
                todo.add(classCodeSeq);
            }

        } else {
            final ImmutableIntList vector =
                    createVector(Utils.splitString(vectors[depth].trim()).get(), steps[depth],
                            currentCodeNumbers[num].size());

            for (int i = starts[depth]; i <= ends[depth]; i++) {
                addMultiple(workingNumbers, i, vector);
                todo.addAll(iterateThru(workingNumbers, vectors, starts, ends, steps, depth + 1, num));
                addMultiple(workingNumbers, -i, vector);
            }
        }
        return todo;
    }

    // Zhao Yu Li, Jun 27, 2025.
    // The Structure is similar to drawAutoPolyVary. It uses recursion, so that we can jump to the next point before
    // starting the Vary task. It May cause a slight performance overhead because we are drawing more often (after
    // finishing the vary task of the current point and before starting the next one).
    private void recurseDrawVaryL(final MutableList<Vector2> points, final int[] max, List<String> codeList,
                                  final boolean draw, final boolean overrideSS, final boolean autoCover, final boolean autoSmallCover, final int maxPrint,
                                  final ExecutorService executor, final ExecutorService storageExecutor,
                                  final ExecutorService shotExecutor, final boolean printMid, final boolean firstLast,
                                  final boolean addToAllPositive, final boolean addToPlusMinus, final int idx,
                                  final int step, final int end, final int codesFound, final ProgressMultiTask overallProgress,
                                  final ArrayList<Storage> previousCodes) {

        // Zhao Yu Li, Jun 27, 2025.
        // Move the screen to the point we are working on
        Vector2 point = points.get(idx);
        moveScreen(point.x, point.y);

        // Zhao Yu Li, Jun 29, 2025.
        // Set the line number of (Middle)VaryWindowL
        if (printMid) middleVaryWindow.setLineNumber(idx + 1);
        else varyWindow.setLineNumber(idx + 1);

        // Zhao Yu Li, Jul 31, 2025.
        // To save time, we check if the current coordinate is inside any of the polygons formed the codes found from
        // the previous coordinate. If yes, then we don't need to run Vary for this coordinate because a code from the
        // last coordinate fills the square.
        for (Storage storage : previousCodes) {
            if (storage.classCodeSeq.stable) {
                final Storage.Stable stable = (Storage.Stable) storage;
                double rx = Math.toRadians(point.x);
                double ry = Math.toRadians(point.y);
                final Location location = stable.polygon.location(rx, ry);

                if (location == Location.INSIDE) {
                    System.out.println("\n//------------- working on point " + (idx + 1) + "-------------\nThis coordinate was filled by a code from the previous coordinate.");
                    System.out.println(Utils.standard(storage.classCodeSeq, 1));

                    overallProgress.increment(Math.abs(step));

                    if (overallProgress.isCancelled()) { // It is possible for cancel to occur before the task is created
                        renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
                        Utils.shutdownExecutorAsync(storageExecutor);
                        Utils.shutdownExecutorAsync(shotExecutor);
                        overallProgress.close();
                        if(autoCover) coverWindow.show();
                    } else if (idx + step < end) {
                        recurseDrawVaryL(points, max, codeList, draw, overrideSS, autoCover, autoSmallCover, maxPrint, executor, storageExecutor,
                                shotExecutor, printMid, firstLast, addToAllPositive, addToPlusMinus, idx + step, step, end,
                                codesFound, overallProgress, previousCodes);
                    } else {
                        overallProgress.close();

                        Utils.shutdownExecutorAsync(storageExecutor);
                        Utils.shutdownExecutorAsync(shotExecutor);

                        // only render the screen after everything has been loaded
                        renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);

                        if (autoSmallCover) {
                            smallCoverWindow.show();
                            System.out.println("+---- " + (printMid ? "MiddleVaryL" : "VaryL") + " Completed, CODES ARE IN SMALL COVER ----+");
                            System.out.println();
                        }

                        if(autoCover) {
                            coverWindow.show();
                            System.out.println("+---- " + (printMid ? "MiddleVaryL" : "VaryL") + " Completed, CODES ARE IN COVER ----+");
                            System.out.println();
                        } else {
                            System.out.println("+-------------- " + (printMid ? "MiddleVaryL" : "VaryL") + " Completed --------------+");
                            System.out.println();
                        }
                    }

                    return;
                }
            }
        }

        // Create the task
        final VaryLTask task = new VaryLTask(
                Array.ofAll(points),
                codeList, boyanMenu,
                Array.ofAll(max),
                pool,
                overrideSS,
                draw,
                maxPrint,
                storageExecutor,
                shotExecutor,
                printMid,
                firstLast,
                addToAllPositive,
                addToPlusMinus,
                iterateToLimitWindow,
                idx, step, end, codesFound
        );
        //final ObservableList<Storage> partials = task.getPartialProperty().get();
        //final MutableSortedSet<String> codeStrings = new TreeSortedSet<>();
        // Count the number of holes we start with
        if (autoCover) coverWindow.appendStablesInfo("// Start " + (printMid ? "MiddleVaryL" : "VaryL"));
        if (autoSmallCover) smallCoverWindow.appendStablesInfo("// Start " + (printMid ? "MiddleVaryL" : "VaryL"));

        // Update screen when change detected
        task.getPartialProperty().get().addListener((ListChangeListener.Change<? extends Storage> c) -> {
            while (c.next()) {
                if(!c.wasAdded()) continue;
                // Draw all new additions
                c.getAddedSubList().forEach(storage -> {
                    if (draw) {
                        final Color color;
                        final int index = cycle.get();
                        color = comboBoxColors.get(index);
                        addToOnScreenSequences(storage, color);
                        renderRegion(storage, (WritableImage) regionsImageView.getImage(), color);
                    }
                });
            }
        });

        task.setOnSucceeded(e -> {
            final ObservableList<Storage> storages;
            try {
                storages = task.get();
            } catch (InterruptedException | ExecutionException exception) {
                throw new RuntimeException(exception);
            }

            storages.forEach(storage -> {
                if(!onScreenSequences.containsKey(storage)) {
                    if (draw) {
                        final Color color;
                        final int index = cycle.get();
                        color = comboBoxColors.get(index);
                        addToOnScreenSequences(storage, color);
                    }
                }

                if (autoCover && storage.classCodeSeq.stable) coverWindow.appendStablesInfo(getCoverCodeString(storage));
                if (autoSmallCover && storage.classCodeSeq.stable) smallCoverWindow.appendStablesInfo(getCoverCodeString(storage));
            });

            overallProgress.increment(Math.abs(step));

            if (overallProgress.isCancelled()) { // It is possible for cancel to occur before the task is created
                renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
                Utils.shutdownExecutorAsync(storageExecutor);
                Utils.shutdownExecutorAsync(shotExecutor);
                overallProgress.close();
                if (autoCover) coverWindow.show();
                if (autoSmallCover) smallCoverWindow.show();
            } else if (idx + step < end) {
                recurseDrawVaryL(points, max, codeList, draw, overrideSS, autoCover, autoSmallCover, maxPrint, executor, storageExecutor,
                        shotExecutor, printMid, firstLast, addToAllPositive, addToPlusMinus, idx + step, step, end,
                        storages.size() + codesFound, overallProgress, new ArrayList<>(storages));
            } else {
                overallProgress.close();

                Utils.shutdownExecutorAsync(storageExecutor);
                Utils.shutdownExecutorAsync(shotExecutor);

                // only render the screen after everything has been loaded
                renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);

                /*
                if(draw) {
                    System.out.println("Printing drawn Codes:");
                    codeStrings.forEach(System.out::println);
                }
                 */

                if (autoSmallCover) {
                    smallCoverWindow.show();
                    System.out.println("+---- " + (printMid ? "MiddleVaryL" : "VaryL") + " Completed, CODES ARE IN SMALL COVER ----+");
                    System.out.println();
                }

                if (autoCover) {
                    coverWindow.show();
                    System.out.println("+---- " + (printMid ? "MiddleVaryL" : "VaryL") + " Completed, CODES ARE IN COVER ----+");
                    System.out.println();
                } else {
                    System.out.println("+-------------- " + (printMid ? "MiddleVaryL" : "VaryL") + " Completed --------------+");
                    System.out.println();
                }
            }
        });

        task.setOnCancelled(e -> {
            task.getPartialProperty().get().forEach(storage -> {
                if(!onScreenSequences.containsKey(storage)) {
                    final Color color;
                    final int index = cycle.get();
                    color = comboBoxColors.get(index);
                    addToOnScreenSequences(storage, color);
                }
            });

            overallProgress.close();

            // Wait for orderly cancellation of unfinished tasks
            Utils.shutdownExecutorAsync(shotExecutor);
            Utils.shutdownExecutorAsync(storageExecutor);

            // only render the screen after everything has been loaded
            renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);

            /*
            if(draw) {
                System.out.println("Printing drawn Codes:");
                codeStrings.forEach(System.out::println);
            }
             */

            if (autoSmallCover) {
                smallCoverWindow.show();
                System.out.println("+---- " + (printMid ? "MiddleVaryL" : "VaryL") + " Cancelled, CODES ARE IN SMALL COVER ----+");
            }

            if (autoCover) {
                coverWindow.show();
                System.out.println("+---- " + (printMid ? "MiddleVaryL" : "VaryL") + " Cancelled, CODES ARE IN COVER ----+");
            } else {
                System.out.println("+-------------- " + (printMid ? "MiddleVaryL" : "VaryL") + " Cancelled --------------+");

            }
        });

        task.setOnFailed(e -> {
            overallProgress.close();
            throw new RuntimeException(task.getException());
        });

        executor.execute(task);
    }

    private void drawVaryL(final MutableList<Vector2> points, final int[] max, final boolean draw,
                           final boolean overrideSS, final boolean autoCover, final boolean autoSmallCover, final int maxPrint,
                           final ExecutorService executor, final ExecutorService storageExecutor, final ExecutorService shotExecutor,
                           final boolean printMid, final boolean firstLast) {
        // Zhao Yu Li, Jun 27, 2025.
        // Attempt to read start, step, and end from the user.
        Tuple3<Integer, Integer, Integer> startStepEnd = getStartStepEnd(points.size());
        if (startStepEnd._1 == null) return;
        final int start = startStepEnd._1;
        final int step = startStepEnd._2;
        final int end = startStepEnd._3;

        // Zhao Yu Li, Jun 24, 2025.
        // Whether to add results to the IterateToLimitWindow Cover
        boolean addToAllPositive = printMid ? middleVaryWindow.getAddToAllPositiveSelected() : varyWindow.getAddToAllPositiveSelected();
        boolean addToPlusMinus = printMid ? middleVaryWindow.getAddToPlusMinusSelected() : varyWindow.getAddToPlusMinusSelected();

        if ((addToAllPositive || addToPlusMinus) && iterateToLimitWindow == null) iterateToLimitWindow = new IterateToLimitWindow(pool);

        List <String> codeList = Arrays.asList(readFromFile(Viewer.tmpDir + "/cover_stables.txt").split(System.lineSeparator()));
        codeList.replaceAll(Utils::tripleTrimmer);

        final ProgressMultiTask progress = new ProgressMultiTask("Line: %d, Stopping at: %d", true, start, end);
        progress.show();

        // Zhao Yu Li, Jun 27, 2025.
        // Changed from a single call to a recursive call. This is to facilitate moving the screen from one point to the
        // next.
        recurseDrawVaryL(points, max, codeList, draw, overrideSS, autoCover, autoSmallCover, maxPrint, executor, storageExecutor,
                shotExecutor, printMid, firstLast, addToAllPositive, addToPlusMinus, start-1, step, end, 0, progress, new ArrayList<>());
    }

    private void LoadFileAction(
            final ConvexPolygon poly, final boolean all, final File file, final ExecutorService executor) {
        LoadFileAction(poly, all, file, executor, false, false);
    }

    private void LoadFileAction(
            final ConvexPolygon poly, final boolean all, final File file, final ExecutorService executor,
            final boolean loadDB, final boolean addToGarbage
    ) {
        if (file != null) {
            // Zhao Yu Li, May 08, 2025.
            // Load codes from a SQLite DB. Functionality is similar to that of handling singles in the parseFile
            // function. We only need to handle singles when loading from DB because the DB separates the codes only by
            // codeType (i.e. cs, cns, oso, osno, ons).
            if (loadDB) {
                String dbPath = file.getAbsolutePath();
                System.out.println("Loading from DB: " + dbPath);

                String url = "jdbc:sqlite:" + dbPath;
                final ArrayList<String> codeTypes = new ArrayList<>();

                if (boyanMenu.CScb.isSelected()) codeTypes.add("cs");

                if (boyanMenu.CNScb.isSelected()) codeTypes.add("cns");

                if (boyanMenu.ONScb.isSelected()) codeTypes.add("ons");

                if (boyanMenu.OSOcb.isSelected()) codeTypes.add("oso");

                if (boyanMenu.OSNOcb.isSelected()) codeTypes.add("osno");

                if (!boyanMenu.CScb.isSelected() && !boyanMenu.CNScb.isSelected() && !boyanMenu.ONScb.isSelected()
                    && !boyanMenu.OSOcb.isSelected() && !boyanMenu.OSNOcb.isSelected()) {
                    final Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setHeaderText("Polygon DB");
                    alert.setContentText("Please select at least one code type");
                    alert.showAndWait();
                    return;
                }

                for (String codeType : codeTypes) {
                    String query = "SELECT code_sequence FROM " + codeType;

                    try (Connection conn = DriverManager.getConnection(url);
                         Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(query)) {

                        Optional<Rectangle> optRect = Optional.empty();
                        LinkedHashMap<ClassifiedCodeSequence, Optional<Color>> map = new LinkedHashMap<>();
                        LinkedHashMap<ClassifiedCodeSequence, Optional<String[]>> optIter = new LinkedHashMap<>();
                        int count = 0;

                        while (rs.next()) {
                            count++;
                            String line = Utils.trimCodeLine(rs.getString("code_sequence"));

                            final ImmutableIntList sequence = Utils.splitString(line.trim()).get();

                            final ClassifiedCodeSequence codeSeq =
                                    ClassifiedCodeSequence.create(sequence).get();

                            map.put(codeSeq, Optional.empty());
                            optIter.put(codeSeq, Optional.empty());

                            if (count == 10000) {
                                count = 0;
                                final Tuple3<Optional<Rectangle>, Map<ClassifiedCodeSequence, Optional<Color>>,
                                        Map<ClassifiedCodeSequence, Optional<String[]>>> tup = new Tuple3<>(optRect, map, optIter);
                                drawCodes(tup, executor, all, poly, true);
                                map = new LinkedHashMap<>();
                                optIter = new LinkedHashMap<>();
                            }
                        }

                        if (!map.isEmpty() && !optIter.isEmpty()) {
                            final Tuple3<Optional<Rectangle>, Map<ClassifiedCodeSequence, Optional<Color>>,
                                    Map<ClassifiedCodeSequence, Optional<String[]>>> tup = new Tuple3<>(optRect, map, optIter);
                            drawCodes(tup, executor, all, poly, true);
                        }
                    } catch (SQLException ex) {
                        System.err.println("Error: " + ex.getMessage());
                    }
                }

                coverWindow.appendStablesInfo("// Load from DB");
            } else {
                final Tuple2<Tuple3<
                        Optional<Rectangle>,
                        Map<ClassifiedCodeSequence, Optional<Color>>,
                        Map<ClassifiedCodeSequence, Optional<String[]>>
                        >,
                        ArrayList<ClassifiedCodeSequence[]>> tup = parseFile(file.toPath(), false, true);

                drawCodes(tup._1, tup._2, executor, all, poly, false);
            }
        }
    }

    private void drawCodes(final Tuple3<Optional<Rectangle>,
            Map<ClassifiedCodeSequence, Optional<Color>>,
            Map<ClassifiedCodeSequence, Optional<String[]>>> tup,
                           final ExecutorService executor, final boolean all, final ConvexPolygon poly) {
        drawCodes(tup, new ArrayList<>(),executor, all, poly, false);
    }

    private void drawCodes(final Tuple3<Optional<Rectangle>,
                                   Map<ClassifiedCodeSequence, Optional<Color>>,
                                   Map<ClassifiedCodeSequence, Optional<String[]>>> tup,
                           final ExecutorService executor, final boolean all, final ConvexPolygon poly, final boolean autoCover) {
        drawCodes(tup, new ArrayList<>(),executor, all, poly, autoCover);
    }

    private void drawCodes(final Tuple3<Optional<Rectangle>,
            Map<ClassifiedCodeSequence, Optional<Color>>,
            Map<ClassifiedCodeSequence,
            Optional<String[]>>> tup,
                           ArrayList<ClassifiedCodeSequence[]> triples, final ExecutorService executor,
                           final boolean all, final ConvexPolygon poly, final boolean autoCover) {
        final Map<ClassifiedCodeSequence, Optional<Color>> map = tup._2;
        final Map<ClassifiedCodeSequence, Optional<String[]>> iterMap = tup._3;
        final ArrayList<ClassifiedCodeSequence> allCodes = new ArrayList<>(map.keySet());

        for (ClassifiedCodeSequence code : iterMap.keySet()) {
            if (iterMap.get(code).isPresent()) {
                final int[] starts = new int[iterMap.get(code).get().length];
                Arrays.fill(starts, Integer.parseInt(iterationStart.getText().trim()));
                final int[] ends = new int[iterMap.get(code).get().length];
                Arrays.fill(ends, Integer.parseInt(iterationEnd.getText().trim()));
                final int[] steps = new int[iterMap.get(code).get().length];
                Arrays.fill(steps, 2);

                currentCodeNumbers[0] = code.codeSequence.codeNumbers.toList();

                iterateAction(code.codeSequence.codeNumbers.toList(),
                        iterMap.get(code).get(), starts, ends, steps, 0, false, executor);
            }
        }

        final Array<ClassifiedCodeSequence> classCodeSeqs = Array.ofAll(allCodes);

        if (drawPictureCheckBox.isSelected()) {
            final ExecutorService drawExecutor = Executors.newFixedThreadPool(Utils.numThreads);
            final DrawPictureTask task = new DrawPictureTask(classCodeSeqs, pool, drawExecutor, false, false);
            final Progress progress = new Progress(task);

            task.setOnSucceeded(e -> {

                final Array<Storage> storages;
                try {
                    storages = task.get();
                } catch (InterruptedException | ExecutionException exception) {
                    throw new RuntimeException(exception);
                }

                storages.forEach(storage -> {
                    if (all || storage.intersects(poly)) {

                        final Optional<Color> opt = map.get(storage.classCodeSeq);
                        final Color color;
                        if (opt.isPresent()) {
                            color = opt.get();
                        } else {
                            final int index = cycle.get();
                            color = comboBoxColors.get(index);
                        }

                        addToOnScreenSequences(storage, color);

                        // Zhao Yu Li, Jun 23, 2025.
                        // Removed printing to console to save time
                        // System.out.println(storage.classCodeSeq);

                        // Zhao Yu Li, May 09, 2025.
                        // Automatically add stables to cover when loading from DB.
                        if (autoCover && (
                                storage.codeType() == CodeType.CS ||
                                        storage.codeType() == OSNO ||
                                        storage.codeType() == CodeType.OSO))
                        {
                            String codeStr = "" + storage.codeType();

                            if (codeStr.equals("CS")) {
                                codeStr += "  ";
                            } else if (!codeStr.equals("OSNO")) {
                                codeStr += " ";
                            }
                            final String msg = codeStr + " (" + storage.codeLength() + ", " + storage.codeSum() + ") " + storage;

                            // This line adds msg to the stable text box of the cover window
                            coverWindow.appendStablesInfo(msg);
                        }
                    }
                });

                Utils.shutdownExecutorAsync(drawExecutor);
                progress.close();

                if (tup._1.isPresent()) {
                    final Rectangle rect = tup._1.get();

                    final String xMin = String.valueOf(Math.toDegrees(rect.intervalX.min));
                    final String xMax = String.valueOf(Math.toDegrees(rect.intervalX.max));
                    final String yMin = String.valueOf(Math.toDegrees(rect.intervalY.min));
                    final String yMax = String.valueOf(Math.toDegrees(rect.intervalY.max));

                    xMinTextField.setText(xMin);
                    xMaxTextField.setText(xMax);

                    yMinTextField.setText(yMin);
                    yMaxTextField.setText(yMax);

                    zoomAction(executor);
                } else {
                    // only render the screen after everything has been loaded
                    // There should be a way to do a "diff" so to speak
                    // We render the things we added, which get put on top
                    renderRegions(onScreenSequences, guideLinesImageView, regionsImageView,
                            executor);
                }
            });

            task.setOnCancelled(e -> {
                Utils.shutdownExecutorAsync(drawExecutor);
                progress.close();
            });

            task.setOnFailed(e -> {
                Utils.shutdownExecutorAsync(drawExecutor);
                progress.close();
                throw new RuntimeException(task.getException());
            });

            executor.execute(task);

            progress.incrementWindowCount(progressWindows);
            showProgressWindow(progress);

            // Zhao Yu Li, May 13, 2025.
            // Handles the drawing and adding the triple to the cover if all three components of the triple intersects
            // with the specified polygon.
            if (!triples.isEmpty()) {
                final DrawPictureTaskTriples taskTriples = new DrawPictureTaskTriples(Array.ofAll(triples), pool, drawExecutor, false, false);
                final Progress progressTriples = new Progress(taskTriples);

                taskTriples.setOnSucceeded(e -> {

                    final Array<Storage[]> storages;
                    try {
                        storages = taskTriples.get();
                    } catch (InterruptedException | ExecutionException exception) {
                        throw new RuntimeException(exception);
                    }

                    if (autoCover) coverWindow.appendTriplesInfo("// Load triples");

                    storages.forEach(triple -> {
                        final StringBuilder tripleStr = new StringBuilder();
                        int count = 0;

                        final int index = cycle.get();
                        final Color color= comboBoxColors.get(index);

                        for (Storage storage : triple) {
                            if (all || storage.intersects(poly)) {
                                count++;
                                tripleStr.append(storage.classCodeSeq.toString());

                                if (count < 3) tripleStr.append(", ");

                                // Zhao Yu Li, May 09, 2025.
                                // Automatically add stables to cover when loading from DB.
                                if ((boyanMenu.CScb.isSelected() && storage.codeType() == CodeType.CS) ||
                                        (boyanMenu.OSNOcb.isSelected() && storage.codeType() == OSNO) ||
                                        (boyanMenu.OSOcb.isSelected() && storage.codeType() == CodeType.OSO))
                                {
                                    addToOnScreenSequences(storage, color);

                                    System.out.println(storage.classCodeSeq);

                                    if (autoCover) {
                                        String codeStr = "" + storage.codeType();

                                        if (codeStr.equals("CS")) {
                                            codeStr += "  ";
                                        } else if (!codeStr.equals("OSNO")) {
                                            codeStr += " ";
                                        }
                                        final String msg = codeStr + " (" + storage.codeLength() + ", " + storage.codeSum() + ") " + storage;

                                        // This line adds msg to the stable text box of the cover window
                                        coverWindow.appendStablesInfo(msg);
                                    }
                                }
                            }
                        }

                        if (count == 3) {
                            if (autoCover) coverWindow.appendTriplesInfo(tripleStr.toString());
                            System.out.println(tripleStr);

                            for (Storage storage : triple) addToOnScreenSequences(storage, color);
                        }
                    });

                    Utils.shutdownExecutorAsync(drawExecutor);
                    progressTriples.close();

                    if (tup._1.isPresent()) {
                        final Rectangle rect = tup._1.get();

                        final String xMin = String.valueOf(Math.toDegrees(rect.intervalX.min));
                        final String xMax = String.valueOf(Math.toDegrees(rect.intervalX.max));
                        final String yMin = String.valueOf(Math.toDegrees(rect.intervalY.min));
                        final String yMax = String.valueOf(Math.toDegrees(rect.intervalY.max));

                        xMinTextField.setText(xMin);
                        xMaxTextField.setText(xMax);

                        yMinTextField.setText(yMin);
                        yMaxTextField.setText(yMax);

                        zoomAction(executor);
                    } else {
                        // only render the screen after everything has been loaded
                        // There should be a way to do a "diff" so to speak
                        // We render the things we added, which get put on top
                        renderRegions(onScreenSequences, guideLinesImageView, regionsImageView,
                                executor);
                    }
                });

                taskTriples.setOnCancelled(e -> {
                    Utils.shutdownExecutorAsync(drawExecutor);
                    progressTriples.close();
                });

                taskTriples.setOnFailed(e -> {
                    Utils.shutdownExecutorAsync(drawExecutor);
                    progressTriples.close();
                    throw new RuntimeException(taskTriples.getException());
                });

                executor.execute(taskTriples);

                progressTriples.incrementWindowCount(progressWindows);
                showProgressWindow(progressTriples);
            }
        } else {
            final DontDrawPictureTask task =
                    new DontDrawPictureTask(classCodeSeqs, pool);
            final Progress progress = new Progress(task);

            // task.messageProperty().addListener((property, oldMsg, newMsg) ->
            // System.out.println(newMsg));

            task.setOnSucceeded(e -> progress.close());

            task.setOnFailed(e -> {
                progress.close();
                throw new RuntimeException(task.getException());
            });

            // Imperative
            executor.execute(task);

            // Imperative
            progress.incrementWindowCount(progressWindows);
            showProgressWindow(progress);
        }
    }

    private void showProgressWindow(Progress progress) {
        if (this.progressWindows.get() > 0) {
            this.progressWindows.decrementAndGet();
            progress.show();
        }
    }

    private void zoomAction(final ExecutorService executor) {
        final double xMin = Math.toRadians(Double.parseDouble(xMinTextField.getText()));
        final double xMax = Math.toRadians(Double.parseDouble(xMaxTextField.getText()));
        final double yMin = Math.toRadians(Double.parseDouble(yMinTextField.getText()));
        final double yMax = Math.toRadians(Double.parseDouble(yMaxTextField.getText()));
        if (0 <= xMin && xMin <= xMax && xMax <= Math.PI
                && 0 <= yMin && yMin <= yMax && yMax <= Math.PI) {
            zoom(xMax, xMin, yMax, yMin, executor);
        }
    }

    private void zoom(final double xMax, final double xMin, final double yMax,
                      final double yMin, final ExecutorService executor) {

        positionZoom(xMax, xMin, yMax, yMin);
        renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
    }

    private void zoomAfterRender(final double xMax, final double xMin, final double yMax,
                                 final double yMin, final ExecutorService executor,
                                 final Runnable afterCommit,
                                 final Consumer<RuntimeException> onFailure) {
        positionZoom(xMax, xMin, yMax, yMin);
        renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor, afterCommit, onFailure);
    }

    private void positionZoom(final double xMax, final double xMin, final double yMax, final double yMin) {

        if (((xMin == xMax) && (yMin == yMax)) || boyanRdoBtn.isSelected()) {
            final double size = map.pixelSize();
            map.setTranslateX(xMin - (SIDE / 2.0) * size);
            map.setTranslateY(yMin - (SIDE / 2.0) * size);

        } else {
            final double width = xMax - xMin;
            final double height = yMax - yMin;

            final double largest = Math.max(height, width);

            // a scale of 1 gives us a width of pi
            final double scale = Math.PI / largest;

            map.setScale(scale);

            final double size = map.pixelSize();
            map.setTranslateX((xMax + xMin) / 2 - (SIDE / 2.0) * size);
            map.setTranslateY((yMax + yMin) / 2 - (SIDE / 2.0) * size);
        }
        viewRectangleBF.add(map.getViewRectangle());
    }

    private void btnCalculateAction(final ConnectionPool pool) {
        final String textCodeSeq = Utils.tripleTrimmer(txtCodeSequence.getText());
        if (textCodeSeq.split(",").length != 1 && textCodeSeq.split(",").length != 3) {
            final Alert alert = new Alert(AlertType.ERROR);

            alert.setTitle("Calculate");
            alert.setHeaderText("Invalid Input");
            alert.setContentText("Please calculate only a triple or single code.");
            alert.showAndWait();
            return;
        }
        currentCodeNumbers[0].clear();
        currentCodeNumbers[1].clear();
        currentCodeNumbers[2].clear();
        if (textCodeSeq.isEmpty()) {
            final Alert alert = new Alert(AlertType.ERROR);

            alert.setTitle("Enter a Code Sequence");
            alert.setHeaderText("Enter a Code Sequence");
            alert.setContentText("Please enter a code sequence.");
            alert.showAndWait();
        } else if (textCodeSeq.contains(",")) {
            // it's a triple

            Utils.runAndWait(() -> {
                String print = "";
                int i = 0;
                for (String tripCode : textCodeSeq.split(",")) {
                    print += buttonCalulator(tripCode, pool, i) + ", ";
                    i++;
                }
                print += "~";
                if (print.contains("empty set") && !print.startsWith("//")) {
                    System.out.println("// " + print.replace(", ~", ""));

                } else {
                    System.out.println(print.replace(", ~", ""));
                }
            });
            appendCalculatedCodeToCoverIfRequested(pool);
        } else {
            // it's a single code
            System.out.println(buttonCalulator(textCodeSeq, pool, 0));
            setupButtons(pool, 1);
            setupButtons(pool, 2);
            appendCalculatedCodeToCoverIfRequested(pool);
        }
    }

    private void appendCalculatedCodeToCoverIfRequested(final ConnectionPool pool) {
        if (!calculateAddToCoverCheckBox.isSelected()) {
            return;
        }

        final ArrayList<Storage> storages = new ArrayList<>();
        final StringBuilder tripleString = new StringBuilder();
        for (int i = 0; i < currentCodeNumbers.length; i++) {
            if (currentCodeNumbers[i].isEmpty()) {
                continue;
            }

            final Either<InvalidCodeSequence, ClassifiedCodeSequence> either =
                    ClassifiedCodeSequence.create(currentCodeNumbers[i]);
            if (either.isLeft()) {
                return;
            }

            final Optional<Storage> storage = Database.loadStorage(either.get(), pool);
            if (storage.isEmpty()) {
                return;
            }

            if (tripleString.length() > 0) {
                tripleString.append(", ");
            }
            tripleString.append(storage.get().classCodeSeq);
            storages.add(storage.get());
        }

        // The Cover window stores stable singles and stable-unstable-stable
        // triples in different text areas. The main Calculate button can draw
        // unstable singles, but those are not directly useful cover entries.
        if (storages.size() == 1) {
            final Storage storage = storages.get(0);
            if (storage.classCodeSeq.stable) {
                final String stableEntry = getCoverCodeString(storage);
                if (!coverWindow.containsStableInfo(stableEntry)) {
                    coverWindow.appendStablesInfo(stableEntry);
                }
            }
        } else if (storages.size() == 3
                && storages.get(0).classCodeSeq.stable
                && !storages.get(1).classCodeSeq.stable
                && storages.get(2).classCodeSeq.stable) {
            final String tripleEntry = tripleString.toString();
            if (!coverWindow.containsTripleInfo(tripleEntry)) {
                coverWindow.appendTriplesInfo(tripleEntry);
            }
        }
    }

    // does the calculating for the btnCalculateAction
    private String buttonCalulator(final String code, final ConnectionPool pool, final int n) {
        final Optional<ImmutableIntList> optional = Utils.splitString(code);
        if (optional.isPresent()) {
            currentCodeNumbers[n] = IntArrayList.newList(optional.get());
            setupButtons(pool, n);

            return calculateCurrentCodeNumbers(pool, n);

        } else {
            final Alert alert = new Alert(AlertType.ERROR);

            alert.setTitle("Invalid Input");
            alert.setHeaderText("Invalid Input");
            alert.setContentText(String.format("Input %s is invalid.", code));
            alert.showAndWait();
            return "";
        }
    }

    private static void showEnterLineNumberError() {
        final Alert alert = new Alert(AlertType.ERROR);

        alert.setTitle("Enter a Number");
        alert.setHeaderText("Enter a Number");
        alert.setContentText("Please enter a line number.");
        alert.showAndWait();
    }

    private static void showEnterLineNumberErrorAutoVary() {
        final Alert alert = new Alert(AlertType.ERROR);

        alert.setTitle("Enter Line Numbers");
        alert.setHeaderText("Enter Line Numbers");
        alert.setContentText("Please enter start and end line numbers for AutoPolyVary.");
        alert.showAndWait();
    }

    private static void showStepErrorAutoVary() {
        final Alert alert = new Alert(AlertType.ERROR);

        alert.setTitle("Bad Step Value");
        alert.setHeaderText("Bad Step Value");
        alert.setContentText("AutoPolyVary step value must be >= 1");
        alert.showAndWait();

    }

    private static void showInvalidLineNumberError(final int max) {
        final Alert alert = new Alert(AlertType.ERROR);

        alert.setTitle("Invalid Line Number");
        alert.setHeaderText("Invalid Line Number");
        alert.setContentText(String.format("Line number must be between 1 and %d.", max));
        alert.showAndWait();
    }

    private static void showInvalidLineRangeError(final int max) {
        final Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Invalid Line Range");
        alert.setHeaderText("Invalid Line Range");
        alert.setContentText(String.format("Must have 1 <= Start <= End <= %d.", max));
        alert.showAndWait();
    }

    private static void showInvalidNumberError(final String invalidNumber) {
        final Alert alert = new Alert(AlertType.ERROR);

        alert.setTitle("Invalid Number");
        alert.setHeaderText("Invalid Number");
        alert.setContentText(String.format("Input %s is an invalid number.", invalidNumber));
        alert.showAndWait();
    }

    private void pan(final double initX, final double initY, final double finX, final double finY,
                     final ExecutorService executor) {
        boolean panned = false;
        if (Math.abs(finX - initX) > 5 || Math.abs(finY - initY) > 5) {
            panned = true;
            map.translateXBy(map.radianX(initX) - map.radianX(finX));
            map.translateYBy(map.radianY(initY) - map.radianY(finY));

            viewRectangleBF.add(map.getViewRectangle());
            renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);


        } else {
            click(initX, initY, executor);
        }
        if (autoFillerCheckBox.isSelected() && (panned || !selectRdoBtn.isSelected())) {
            final ConvexPolygon screen = map.getViewRectangle().toConvexPolygon();

            final FastList<Vector2> list = findHoles(screen);

            if (list.size() == 0) {
                screenFills.add(map.getViewRectangle());
                renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);

            }
        }
    }

    private void click(final double pixelX, final double pixelY, final ExecutorService executor) {
        // System.out.println(regionsImageView.getImage().getPixelReader().getArgb((int)pixelX,
        // (int)pixelY));

        final double oldRadianX = map.radianX(pixelX + 0.5);
        final double oldRadianY = map.radianY(pixelY + 0.5);

        final double oldDegreeX = Math.toDegrees(oldRadianX);
        final double oldDegreeY = Math.toDegrees(oldRadianY);

        textXLockField.setText(Double.toString(oldDegreeX));
        textYLockField.setText(Double.toString(oldDegreeY));

        /*if (boyanRdoBtn.isSelected()) {
            boyanMenu.click(oldDegreeX, oldDegreeY);
        }*/
        boyanMenu.click(oldDegreeX, oldDegreeY);

        if (selectRdoBtn.isSelected()) {

            boolean reloadFromCoverPolys = false;

            if (!mrrBounds.isEmpty()) {
                reloadFromCoverPolys = true;
                if (keepPolys.getValue().equals("Two")) {

                    mrrBounds.clear();
                    if (secondPoly.isPresent()) {
                        mrrBounds.put(secondPoly.get()._1, secondPoly.get()._2);
                    }

                } else {
                    secondPoly = Optional.empty();
                }

                if (keepPolys.getValue().equals("One") || keepPolys.getValue().equals("None")) {
                    mrrBounds.clear();
                }
            }
            // now we get the current point
            double rx = map.radianX(pixelX + 0.5);
            double ry = map.radianY(pixelY + 0.5);

            // all feature part 3/3 shows code sequence when clicked
            if (allCheckBox.isSelected()) {
                final List<Double> subList = Arrays.asList(rx, ry, Math.PI - (rx + ry));
                Collections.sort(subList);
                rx = subList.get(0);
                ry = subList.get(1);
            }
            final double radianX = rx;
            final double radianY = ry;

            // iterate over the onScreenSequences, and check which ones are positive
            // for the point. We want the codes on the right to be sorted from smallest
            // to largest, since that makes life much easier
            // map and filter would be really nice right now
            final SortedSet<Storage> selectedStorages = new TreeSet<>();

            final double halfWidth = map.pixelSize() / 2;
            final double offset = getOffset();

            for (final Storage storage : onScreenSequences.keySet()) {
                if (onScreenSequences.get(storage).equals(Color.TRANSPARENT)) {
                    onScreenSequences.remove(storage);
                } else if (storage.classCodeSeq.stable) {
                    final Storage.Stable stable = (Storage.Stable) storage;

                    final Location location = stable.polygon.location(radianX, radianY);
                    if (location == Location.INSIDE) {
                        if (proverCheckBox.isSelected()) {
                            if (stable.isPositiveProver(radianX, radianY, halfWidth, offset)) {
                                selectedStorages.add(stable);
                            }
                        } else {
                            if (stable.isPositive(radianX, radianY)) {
                                selectedStorages.add(stable);
                            }
                        }
                    }
                } else if (!storage.classCodeSeq.stable) {
                    final Storage.Unstable unstable = (Storage.Unstable) storage;

                    final MutableList<Vector2> points = new FastList<>();
                    final Vector2 start = unstable.lineSegment.start;
                    final Vector2 end = unstable.lineSegment.end;
                    final Vector2 direct = end.sub(start);
                    final double size = (unstableDetect / 2) * map.pixelSize();
                    final Vector2 perp = direct.perp(direct).scale(size);
                    points.addAll(Arrays.asList(start.add(perp), start.sub(perp), end.sub(perp), end.add(perp)));
                    final ConvexPolygon unstPoly = ConvexPolygon.create(points.toImmutable());

                    final Location location = unstPoly.location(radianX, radianY);
                    if (location == Location.INSIDE) {
                        selectedStorages.add(unstable);
                    }
                }
            }

            for (final Rectangle rect : coverRects.stableEntrySet()) {
                if (rect.contains(radianX, radianY)) {
                    final ClassifiedCodeSequence codeSeq = coverRects.getStable(rect);
                    final Optional<Storage> optional = Database.loadStorage(codeSeq, pool);
                    if (optional.isPresent()) {
                        final Storage.Stable stable = (Storage.Stable) optional.get();
                        //if (stable.polygon.location(radianX, radianY) == Location.OUTSIDE && !stable.polygon.intersects(rect)) {
                        if (rect.trimable && stable.polygon.location(radianX, radianY) == Location.OUTSIDE) {
                            continue;
                        }
                        if (!rect.trimable) {
                            continue;
                        }
                        selectedStorages.add(stable);
                        if (coverColorCycle.isSelected() && !coverColor.equals(Color.TRANSPARENT)) {
                            coverColor = ColorPicker.next(coverColor);
                            covRectsColorBox.setText(Colors.colorMap.get(coverColor).get());
                        }
                        final Color color;
                        if (coverColor.equals(Color.TRANSPARENT)) {
                            color = coverRects.getColor(rect);
                        } else {
                            color = coverColor;
                        }

                        if (!keepPolys.getValue().equals("None")) {
                            mrrBounds.put(stable.polygon, color);
                        }
                        if (keepPolys.getValue().equals("Two")) {
                            secondPoly = Optional.of(Tuple.of(stable.polygon, color));
                        }
                        reloadFromCoverPolys = true;

                        for (final Rectangle rect2 : coverRects.stableEntrySet()) {
                            final ClassifiedCodeSequence codeSeq2 = coverRects.getStable(rect2);
                            final Color covColor;
                            if (color.equals(Color.TRANSPARENT)) {
                                covColor = coverRects.getColor(rect2);
                            } else {
                                covColor = color;
                            }
                            if (codeSeq.equals(codeSeq2)) {
                                coverRects.put(rect2, covColor);
                            }
                        }

                    }
                    break;
                }
            }
            for (final Rectangle rect : coverRects.tripleEntrySet()) {
                if (rect.contains(radianX, radianY)) {
                    final ClassifiedCodeSequence codeSeq = coverRects.getTriple(rect).unstable;
                    final Optional<Storage> optional = Database.loadStorage(codeSeq, pool);
                    if (optional.isPresent()) {
                        if (coverColorCycle.isSelected() && !coverColor.equals(Color.TRANSPARENT)) {
                            coverColor = ColorPicker.next(coverColor);
                            covRectsColorBox.setText(Colors.colorMap.get(coverColor).get());
                        }

                        final Storage.Unstable unstable = (Storage.Unstable) optional.get();
                        selectedStorages.add(unstable);

                        final Color color;
                        if (coverColor.equals(Color.TRANSPARENT)) {
                            color = coverRects.getColor(rect);
                        } else {
                            color = coverColor;
                        }
                        final MutableList<Vector2> points = new FastList<>();
                        points.add(unstable.lineSegment.end);
                        points.add(unstable.lineSegment.end);
                        points.add(unstable.lineSegment.start);
                        points.add(unstable.lineSegment.start);
                        final ConvexPolygon unstablePoly = ConvexPolygon.create(points.toImmutable());
                        if (!keepPolys.getValue().equals("None")) {
                            mrrBounds.put(unstablePoly, color.invert());
                        }
                        if (keepPolys.getValue().equals("Two")) {
                            secondPoly = Optional.of(Tuple.of(unstablePoly, color.invert()));
                        }
                        reloadFromCoverPolys = true;

                        for (final Rectangle rect2 : coverRects.tripleEntrySet()) {
                            final ClassifiedCodeSequence codeSeq2 = coverRects.getTriple(rect2).unstable;
                            final Color covColor;
                            if (color.equals(Color.TRANSPARENT)) {
                                covColor = coverRects.getColor(rect2);
                            } else {
                                covColor = color;
                            }
                            if (codeSeq.equals(codeSeq2)) {
                                coverRects.put(rect2, covColor);
                            }
                        }
                    }
                    break;
                }
            }
            for (final Rectangle rect : coverRects.HalfTripleEntrySet()) {
                if (rect.contains(radianX, radianY)) {
                    final ClassifiedCodeSequence codeSeq = coverRects.getHalfTriple(rect).unstable;
                    final ClassifiedCodeSequence halfTripleStableCodeSeq = coverRects.getHalfTriple(rect).stableNeg;
                    IntList intList = Utils.splitString("1 1 1").get();
                    ClassifiedCodeSequence fakeStable = ClassifiedCodeSequence.create(intList).get();
                    boolean trim = true;
                    if (fakeStable.equals(halfTripleStableCodeSeq)) {
                        trim = false;
                    }
                    final Optional<Storage> optional = Database.loadStorage(codeSeq, pool);
                    if (trim) {
                        final Optional<Storage> optional2 = Database.loadStorage(halfTripleStableCodeSeq, pool);
                        if (optional2.isPresent()) {
                            Storage.Stable stable = (Storage.Stable) optional2.get();
                            if (!rect.trimable || stable.polygon.location(radianX, radianY) == Location.OUTSIDE) {
                                continue;
                            }
                        }
                    }
                    if (optional.isPresent()) {
                        if (coverColorCycle.isSelected() && !coverColor.equals(Color.TRANSPARENT)) {
                            coverColor = ColorPicker.next(coverColor);
                            covRectsColorBox.setText(Colors.colorMap.get(coverColor).get());
                        }

                        final Storage.Unstable unstable = (Storage.Unstable) optional.get();
                        selectedStorages.add(unstable);

                        final Color color;
                        if (coverColor.equals(Color.TRANSPARENT)) {
                            color = coverRects.getColor(rect);
                        } else {
                            color = coverColor;
                        }
                        final MutableList<Vector2> points = new FastList<>();
                        points.add(unstable.lineSegment.end);
                        points.add(unstable.lineSegment.end);
                        points.add(unstable.lineSegment.start);
                        points.add(unstable.lineSegment.start);
                        final ConvexPolygon unstablePoly = ConvexPolygon.create(points.toImmutable());
                        if (!keepPolys.getValue().equals("None")) {
                            mrrBounds.put(unstablePoly, color.invert());
                        }
                        if (keepPolys.getValue().equals("Two")) {
                            secondPoly = Optional.of(Tuple.of(unstablePoly, color.invert()));
                        }
                        reloadFromCoverPolys = true;

                        for (final Rectangle rect2 : coverRects.HalfTripleEntrySet()) {
                            final ClassifiedCodeSequence codeSeq2 = coverRects.getHalfTriple(rect2).unstable;
                            final Color covColor;
                            if (color.equals(Color.TRANSPARENT)) {
                                covColor = coverRects.getColor(rect2);
                            } else {
                                covColor = color;
                            }
                            if (codeSeq.equals(codeSeq2)) {
                                coverRects.put(rect2, covColor);
                            }
                        }
                    }
                    break;
                }
            }
            //TODO calculate the formula


            makeRightScrollPane(selectedStorages, executor);

            if (reloadFromCoverPolys) {
                renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
            }

        } else {
            final double zoom = Double.parseDouble(zoomScaleText.getText());

            if (magnifyRdoBtn.isSelected()) {
                map.scaleBy(zoom);
            } else if (demagnifyRdoBtn.isSelected()) {
                map.scaleBy(1 / zoom);
            } else if (centerBtn.isSelected()) {
                map.scaleBy(1);
            } else {
                throw new RuntimeException("wrong button");
            }

            final double newRadianX = map.radianX(SIDE / 2 + 0.5);
            final double newRadianY = map.radianY(SIDE / 2 + 0.5);

            map.translateXBy(oldRadianX - newRadianX);
            map.translateYBy(oldRadianY - newRadianY);

            // we want map(mouse click) == zoomed map(center)

            viewRectangleBF.add(map.getViewRectangle());
            renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);

        }
    }

    private void makeRightScrollPane(
            final SortedSet<Storage> selectedStorages, final ExecutorService executor) {
        // remove any listings already there
        codeSequencesGPane.getChildren().clear();
        int row = 0;

        for (final Storage storage : selectedStorages) {
            final String codeString = storage.toString();
            final TextField lblCodeSequence = new TextField(codeString);
            final Label codeInfo = new Label();
            codeInfo.setText(storage.classCodeSeq.codeType + " (" + storage.classCodeSeq.codeLength + "," + storage.classCodeSeq.codeSum + ")");
            codeInfo.setPadding(new Insets(5, 5, 5, 0));
            lblCodeSequence.setPrefWidth(100);
            lblCodeSequence.setEditable(false);
            if (!boyanMenu.buildPolyCheckBox.isSelected()) {
                System.out.println(storage);
            }

            final Button cboxCodeSequence = new Button();
            try {
                cboxCodeSequence.setText(Colors.colorMap.get(onScreenSequences.get(storage)).get());
            } catch (final NoSuchElementException e) {
                cboxCodeSequence.setText("Transparent");
            }
            cboxCodeSequence.setPrefWidth(151);
            cboxCodeSequence.setOnAction(event -> {
                final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                final int mouseY = MouseInfo.getPointerInfo().getLocation().y + 20;
                final int mouseX = Math.min(MouseInfo.getPointerInfo().getLocation().x,
                        (int) screenSize.getWidth() - 270);
                final ColorPicker picker = new ColorPicker(mouseX, mouseY);
                final Optional<Color> opt = picker.pickColor();
                opt.ifPresent(color -> {
                    if (!color.equals(Color.TRANSPARENT)) {
                        addToOnScreenSequences(storage, color);
                        renderRegion(storage, (WritableImage) regionsImageView.getImage(), color);
                    } else {
                        onScreenSequences.remove(storage);
                        renderRegions(
                                onScreenSequences, guideLinesImageView, regionsImageView, executor);
                    }
                    cboxCodeSequence.setText(Colors.colorMap.get(color).get());
                });
            });
            final Button xBtn = new Button();
            xBtn.setText("X");
            xBtn.setPrefWidth(25);
            xBtn.setOnAction(event -> {
                for (final ConvexPolygon poly : mrrBounds.keySet()) {
                    if (storage.classCodeSeq.stable) {
                        if (((Storage.Stable) storage).polygon.equals(poly)) {
                            mrrBounds.remove(poly);
                            break;
                        }
                    }
                }
                onScreenSequences.remove(storage);
                selectedStorages.remove(storage);
                renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
                makeRightScrollPane(selectedStorages, executor);
            });


            final HBox actionsHBox = new HBox();
            actionsHBox.getChildren().addAll(cboxCodeSequence, xBtn);
            final HBox codeInfoHBox = new HBox();
            codeInfoHBox.getChildren().addAll(codeInfo, lblCodeSequence);
            final VBox fullCodeVBox = new VBox();
            fullCodeVBox.getChildren().addAll(actionsHBox, codeInfoHBox);

            codeSequencesGPane.addRow(row, fullCodeVBox);
            row += 1;
        }
    }

    // returns the first location of a hole which is not in the already list
    private Optional<Vector2> findHole(
            final int x0, final int x1, final int y0, final int y1, final ConvexPolygon area) {
        return findHole(x0, x1, y0, y1, area, new FastList<>());
    }

    private Optional<Vector2> findHole(final int xMin, final int xMax, final int yMin,
                                       final int yMax, final ConvexPolygon area, final FastList<Vector2> already) {
        final Image image = regionsImageView.getImage();
        final PixelReader reader = image.getPixelReader();
        for (int pixelX = xMin; pixelX < xMax; pixelX += 1) {
            for (int pixelY = yMin; pixelY < yMax; pixelY += 1) {
                final int color = reader.getArgb(pixelX, pixelY);
                if (color == 0) {
                    final double rx = map.radianX(pixelX + 0.5);
                    final double ry = map.radianY(pixelY + 0.5);
                    if (area.location(rx, ry).equals(Location.INSIDE)) {
                        final Vector2 vect = Vector2.create(rx, ry);
                        if (!already.contains(vect)) {
                            return Optional.of(vect);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private FastList<Vector2> findHoles(final ConvexPolygon area) {

        final Image image = regionsImageView.getImage();
        final PixelReader reader = image.getPixelReader();

        final FastList<Vector2> list = new FastList<>();

        for (int pixelX = 0; pixelX < SIDE; pixelX += 1) {
            for (int pixelY = 0; pixelY < SIDE; pixelY += 1) {
                final int color = reader.getArgb(pixelX, pixelY);

                if (color == 0) {
                    final double rx = map.radianX(pixelX + 0.5);
                    final double ry = map.radianY(pixelY + 0.5);
                    if (area.location(rx, ry).equals(Location.INSIDE)) {
                        final Vector2 coords =
                                Vector2.create(Math.toDegrees(rx), Math.toDegrees(ry));
                        list.add(coords);
                    }
                }
            }
        }
        return list;
    }

    private static void setImageColor(final WritableImage image, final Color color) {
        final PixelWriter pixelWriter = image.getPixelWriter();

        for (int pixelX = 0; pixelX < SIDE; pixelX += 1) {
            for (int pixelY = 0; pixelY < SIDE; pixelY += 1) {
                pixelWriter.setColor(pixelX, pixelY, color);
            }
        }
    }

    private static ImageView renderColor(final Color color) {
        final WritableImage image = new WritableImage(SIDE, SIDE);

        setImageColor(image, color);

        final ImageView imageView = new ImageView(image);

        return imageView;
    }

    // NOTE: static functions don't rely on UI elements, since all the UI elements are non-static
    // the storages list already has newest first
    // Static functions are good, because they are more thread-safer

    private Color color(final List<Storage.Stable> storages,
                        final LinkedHashMap<Storage, Color> colors, final double rx, final double ry,
                        final double halfWidth, final double offset, final boolean proverSelected) {
        // only return the color of the first thing that works
        for (final Storage.Stable stable : storages) {
            final Location location = stable.polygon.location(rx, ry);
            if (location == Location.INSIDE) {
                if (proverSelected) {
                    if (stable.isPositiveProver(rx, ry, halfWidth, offset)) {
                        return colors.get(stable);
                    }
                } else {
                    if (stable.isPositive(rx, ry)) {
                        return colors.get(stable);
                    }
                }
            }
        }
        return Color.TRANSPARENT;
    }

    private WritableImage redoFromScratch(
            final LinkedHashMap<Storage, Color> regions,
            final ExecutorService executor,
            final RenderOptions options) {
        // only depends on map
        final Rectangle viewRectangle = map.getViewRectangle();

        final List<Storage.Stable> stableRegions = new ArrayList<>();
        final List<Storage.Unstable> unstableRegions = new ArrayList<>();

        for (final Storage storage : regions.keySet()) {
            if (storage.intersects(viewRectangle) || options.allSelected) {
                if (storage.classCodeSeq.stable) {
                    stableRegions.add((Storage.Stable) storage);
                } else {
                    unstableRegions.add((Storage.Unstable) storage);
                }
            }
        }

        // Regions added later (at the end of the list) should be drawn first
        Collections.reverse(stableRegions);
        Collections.reverse(unstableRegions);

        // iterate across each axis and get the x and y radian coordinates for the pixels
        final double[] rxs = new double[SIDE];
        for (int pixelX = 0; pixelX < SIDE; pixelX += 1) {
            final double rx = map.radianX(pixelX + 0.5);
            rxs[pixelX] = rx;
        }

        final double[] rys = new double[SIDE];
        for (int pixelY = 0; pixelY < SIDE; pixelY += 1) {
            final double ry = map.radianY(pixelY + 0.5);
            rys[pixelY] = ry;
        }

        final double halfWidth = map.pixelSize() / 2;
        final double offset = getOffset();

        final Color[][] colors = new Color[SIDE][SIDE];

        // The old renderer submitted one Future per pixel, which means about
        // SIDE*SIDE tasks for a normal redraw. Batch by image row instead: each
        // worker still computes independent pixels, but the executor and heap
        // only see SIDE jobs.
        @SuppressWarnings({"unchecked", "rawtypes"})
        final Future<Color[]>[] rowFutures = new Future[SIDE];
        for (int pixelX = 0; pixelX < SIDE; pixelX += 1) {
            final double rx = rxs[pixelX];
            rowFutures[pixelX] = executor.submit(() -> {
                final Color[] row = new Color[SIDE];
                for (int pixelY = 0; pixelY < SIDE; pixelY += 1) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new CancellationException("Render row cancelled");
                    }
                    final double ry = rys[pixelY];
                    if (options.allSelected) { // all feature part 2/3
                        final List<Double> subList = Arrays.asList(rx, ry, Math.PI - (rx + ry));
                        Collections.sort(subList);
                        row[pixelY] = color(stableRegions, regions, subList.get(0),
                                subList.get(1), halfWidth, offset, options.proverSelected);
                    } else {
                        row[pixelY] = color(stableRegions, regions, rx, ry, halfWidth, offset,
                                options.proverSelected);
                    }
                }
                return row;
            });
        }

        for (int pixelX = 0; pixelX < SIDE; pixelX += 1) {
            try {
                colors[pixelX] = rowFutures[pixelX].get();
            } catch (InterruptedException e) {
                // The coalescing renderer cancels old redraws by interrupting
                // this parent job. Cancel the row jobs too so old pixel work
                // does not keep competing with the newest redraw.
                Utils.cancelFutures(Arrays.asList(rowFutures));
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (CancellationException e) {
                Utils.cancelFutures(Arrays.asList(rowFutures));
                throw e;
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        final WritableImage regionImage = new WritableImage(SIDE, SIDE);
        final PixelWriter writer = regionImage.getPixelWriter();
        for (int pixelX = 0; pixelX < SIDE; pixelX += 1) {
            for (int pixelY = 0; pixelY < SIDE; pixelY += 1) {
                if (options.boundsSelected) {
                    double rx = map.radianX(pixelX + 0.5);
                    double ry = map.radianY(pixelY + 0.5);
                    if (options.allSelected) {
                        final List<Double> subList = Arrays.asList(rx, ry, Math.PI - (rx + ry));
                        Collections.sort(subList);
                        rx = subList.get(0);
                        ry = subList.get(1);
                    }
                    for (final Storage.Stable stable : stableRegions) {
                        final Location location = stable.polygon.location(rx, ry);
                        if (location == Location.INSIDE) {
                            writer.setColor(pixelX, pixelY, fillBoundColor);
                        }
                    }
                }
                writer.setColor(pixelX, pixelY, colors[pixelX][pixelY]);
            }
        }

        if (options.showFillsSelected) {
            drawFills(writer);
        }

        // Now draw the unstable ones in serial
        unstableRegions.forEach(
                unstable -> renderUnstable(unstable, writer, viewRectangle, regions.get(unstable), options));

        return regionImage;
    }

    private void drawFills(final PixelWriter writer) {

        for (final Rectangle rect : screenFills) {

            // This is the intersection of the boxes
            final int minX = Math.max((int) Math.ceil(map.pixelX(rect.intervalX.min)), 0);
            final int maxX = Math.min((int) Math.floor(map.pixelX(rect.intervalX.max)), SIDE);

            final int minY = Math.max((int) Math.ceil(map.pixelY(rect.intervalY.min)), 0);
            final int maxY = Math.min((int) Math.floor(map.pixelY(rect.intervalY.max)), SIDE);

            for (int x = minX; x < maxX; ++x) {
                for (int y = minY; y < maxY; ++y) {
                    writer.setColor(x, y, screenFillsColor);
                }
            }
        }
    }

    void addToOnScreenSequences(final Storage storage, final Color color) {
        // removes the storage if it is present
        onScreenSequences.remove(storage);
        // This makes sure it is at the end
        onScreenSequences.put(storage, color);
    }

    void callRenderRegions() {
        renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executorService);
    }

    void renderRegions(final LinkedHashMap<Storage, Color> regions,
                       final ImageView guideLinesImageView, final ImageView regionsImageView,
                       final ExecutorService executor) {
        renderRegions(regions, guideLinesImageView, regionsImageView, executor, null, null);
    }

    private void renderRegions(final LinkedHashMap<Storage, Color> regions,
                               final ImageView guideLinesImageView, final ImageView regionsImageView,
                               final ExecutorService executor, final Runnable afterCommit,
                               final Consumer<RuntimeException> onFailure) {
        final RenderOptions options = snapshotRenderOptions();

        // Coalescing is deliberately limited to the long-lived viewer executor.
        // Several AutoVary/cover paths pass short-lived draw executors and then
        // shut them down as part of task cleanup; those renders must stay
        // synchronous so progress-saving cancel paths still finish deterministically.
        if (!Utils.renderCoalescing || executor != executorService) {
            try {
                commitRenderedImages(buildRenderedImages(regions, executor, options), guideLinesImageView, regionsImageView);
                if (afterCommit != null) {
                    // Coalescing is disabled for --threads=1. Defer the
                    // continuation so a long run of covered/empty coordinates
                    // cannot recurse synchronously through thousands of lines.
                    Platform.runLater(afterCommit);
                }
            } catch (final RuntimeException e) {
                if (onFailure == null) {
                    throw e;
                }
                onFailure.accept(e);
            }
            return;
        }

        final long generation = renderGeneration.incrementAndGet();
        final Future<?> previousRender = renderFuture;
        if (previousRender != null) {
            previousRender.cancel(true);
        }

        // Snapshot the map because UI callbacks may add/remove regions while a
        // background redraw is still computing pixels for the previous view.
        final LinkedHashMap<Storage, Color> regionsCopy = new LinkedHashMap<>(regions);
        try {
            renderFuture = executor.submit(() -> {
                final RenderedImages images;
                try {
                    images = buildRenderedImages(regionsCopy, executor, options);
                } catch (final RuntimeException e) {
                    if (Thread.currentThread().isInterrupted()
                            || generation != renderGeneration.get()
                            || e instanceof CancellationException) {
                        return;
                    }
                    if (onFailure != null) {
                        Platform.runLater(() -> onFailure.accept(e));
                        return;
                    }
                    throw e;
                }

                if (Thread.currentThread().isInterrupted() || generation != renderGeneration.get()) {
                    return;
                }

                Platform.runLater(() -> {
                    if (generation == renderGeneration.get()) {
                        try {
                            commitRenderedImages(images, guideLinesImageView, regionsImageView);
                            if (afterCommit != null) {
                                afterCommit.run();
                            }
                        } catch (final RuntimeException e) {
                            if (onFailure == null) {
                                throw e;
                            }
                            onFailure.accept(e);
                        }
                    }
                });
            });
        } catch (final RejectedExecutionException e) {
            // The app is usually closing if the persistent executor rejects a
            // redraw. Rendering a final stale image is not worth reviving work.
            if (onFailure != null) {
                onFailure.accept(e);
            }
        }
    }

    private RenderOptions snapshotRenderOptions() {
        // Read JavaFX controls once before drawing. The renderer may run on a
        // worker thread, and the pixel loop should use a stable view of these
        // toggles instead of repeatedly touching scene controls.
        return new RenderOptions(
                allCheckBox.isSelected(),
                boundsCheckBox.isSelected(),
                showFillsCheckBox.isSelected(),
                proverCheckBox.isSelected());
    }

    private RenderedImages buildRenderedImages(
            final LinkedHashMap<Storage, Color> regions,
            final ExecutorService executor,
            final RenderOptions options) {
        // Image 1: Guidelines
        final WritableImage guideLinesImage = renderGuideLines();

        // Image 2: Regions
        final WritableImage regionImage = redoFromScratch(regions, executor, options);

        // Zhao Yu Li, Jul 29, 2025.
        // Squares are optionally rendered (whereas they were unconditionally rendered before).
        // Updated Jul 30, 205.
        // It is okay to unconditionally render the squares as it does not significantly impact the execution time
        // if (renderSquares) {
        ArrayList<Rectangle> rectangles = new ArrayList<>();
        rectangles.addAll(coverRects.tripleEntrySet());
        rectangles.addAll(coverRects.HalfTripleEntrySet());
        rectangles.addAll(coverRects.stableEntrySet());
        rectangles.sort(Comparator.comparingDouble(o -> o.intervalX.max - o.intervalX.min));
        for (Rectangle rect : rectangles) {
            renderRectLoad(rect, regionImage, coverRects.getColor(rect), Color.FIREBRICK, options);
        }
        // }

        // Image 3: Bounds
        final WritableImage boundsImage = new WritableImage(SIDE, SIDE);
        for (final ConvexPolygon poly : innerPolyBounds) {
            renderPolygon(poly, boundsImage, polyBoundColor);
        }
        for (final ConvexPolygon poly : outerPolyBounds) {
            renderPolygon(poly, boundsImage, polyBoundColor);
        }
        coverArea.ifPresent(convexPolygon -> renderPolygon(convexPolygon, boundsImage, coverAreaColor));
        smallCoverAreas.forEach(convexPolygon -> renderPolygon(convexPolygon, boundsImage, coverAreaColor));
        autoVaryArea.ifPresent(convexPolygon -> renderPolygon(convexPolygon, boundsImage, coverAreaColor));
        for (final Entry<ConvexPolygon, Color> poly : mrrBounds.entrySet()) {
            final Color color;
            if (poly.getValue().equals(Color.BLACK) || poly.getValue().equals(Color.TRANSPARENT)) {
                color = coverPolyBoundColor;
            } else {
                color = poly.getValue();
            }
            renderPolygon(poly.getKey(), boundsImage, color);
        }

        // Image 4: One-by-One
        final WritableImage oboImage = new WritableImage(SIDE, SIDE);
        if (currentOBOStorage != null) {
            renderRegion(currentOBOStorage, oboImage, currentOBOColor, options);
        }

        return new RenderedImages(guideLinesImage, regionImage, boundsImage, oboImage);
    }

    private void commitRenderedImages(
            final RenderedImages images, final ImageView guideLinesImageView, final ImageView regionsImageView) {
        // Update all the images at once to avoid jarring rendering. Only this
        // method touches the JavaFX ImageViews; buildRenderedImages can run in
        // the background while it prepares detached WritableImages.
        guideLinesImageView.setImage(images.guideLinesImage);
        regionsImageView.setImage(images.regionImage);
        boundsImageView.setImage(images.boundsImage);
        oboImageView.setImage(images.oboImage);
    }

    private static final class RenderOptions {
        final boolean allSelected;
        final boolean boundsSelected;
        final boolean showFillsSelected;
        final boolean proverSelected;

        RenderOptions(
                final boolean allSelected,
                final boolean boundsSelected,
                final boolean showFillsSelected,
                final boolean proverSelected) {
            this.allSelected = allSelected;
            this.boundsSelected = boundsSelected;
            this.showFillsSelected = showFillsSelected;
            this.proverSelected = proverSelected;
        }
    }

    private static final class RenderedImages {
        final WritableImage guideLinesImage;
        final WritableImage regionImage;
        final WritableImage boundsImage;
        final WritableImage oboImage;

        RenderedImages(
                final WritableImage guideLinesImage,
                final WritableImage regionImage,
                final WritableImage boundsImage,
                final WritableImage oboImage) {
            this.guideLinesImage = guideLinesImage;
            this.regionImage = regionImage;
            this.boundsImage = boundsImage;
            this.oboImage = oboImage;
        }
    }


    // use this when the length of the code numbers changes
    private void setupButtons(final ConnectionPool pool, final int n) {
        plusButtons[n].getChildren().clear();
        minusButtons[n].getChildren().clear();

        plusButtonsBiMap.get(n).clear();
        minusButtonsBiMap.get(n).clear();

        for (int i = 0; i < currentCodeNumbers[n].size(); i += 1) {
            final int codeNumber = currentCodeNumbers[n].get(i);

            final Button plusButton = new Button(Integer.toString(codeNumber));
            final Button minusButton = new Button(Integer.toString(codeNumber));

            plusButtons[n].getChildren().add(plusButton);
            minusButtons[n].getChildren().add(minusButton);

            plusButtonsBiMap.get(n).put(plusButton, i);
            minusButtonsBiMap.get(n).put(minusButton, i);

            Utils.colorButton(plusButton, plusColor, clickColor);
            Utils.colorButton(minusButton, minusColor, clickColor);

            plusButton.setOnAction(event -> {

                final int index = plusButtonsBiMap.get(n).get(plusButton);
                final int newNumber = currentCodeNumbers[n].get(index) + 2;
                currentCodeNumbers[n].set(index, newNumber);

                synchronize();
                System.out.println(calculateCurrentCodeNumbers(pool, n));
            });

            minusButton.setOnAction(event -> {
                final int index = minusButtonsBiMap.get(n).get(minusButton);
                final int newNumber = currentCodeNumbers[n].get(index) - 2;
                currentCodeNumbers[n].set(index, newNumber);

                synchronize();
                System.out.println(calculateCurrentCodeNumbers(pool, n));
            });
        }
    }

    // TODO replace this with some actions or property binds
    private void synchronize() {
        // synchronize the UI elements based on the currentCodeNumbers

        String string = "";
        for(int i = 0; i < 3; i++) {

            // set the text in the code box
            if (!currentCodeNumbers[i].isEmpty()) {
                string += currentCodeNumbers[i].makeString(" ");
                if (i < 2) {
                    if (!currentCodeNumbers[i + 1].isEmpty()) {
                        string += ", ";
                    }
                }
            }
        }
        txtCodeSequence.setText(string);

        for(int i = 0; i < 3; i++) {
            // now iterate over the buttons and reassign them
            for (int j = 0; j < currentCodeNumbers[i].size(); j ++) {
                final int codeNumber = currentCodeNumbers[i].get(j);
                final Button plusButton = plusButtonsBiMap.get(i).inverse().get(j);
                final Button minusButton = minusButtonsBiMap.get(i).inverse().get(j);
                plusButton.setText(Integer.toString(codeNumber));
                minusButton.setText(Integer.toString(codeNumber));
            }
        }
    }

    // use when you get a new code sequence that you want to get info for
    private String calculateCurrentCodeNumbers(final ConnectionPool pool, final int i) {

        String result = "";

        if (!saveRegionsCheckBox.isSelected()) {
            // remove any code sequences
            onScreenSequences.clear();
            regionsImageView.setImage(new WritableImage(SIDE, SIDE));
        }

        final Either<InvalidCodeSequence, ClassifiedCodeSequence> either = ClassifiedCodeSequence
                .create(currentCodeNumbers[i]);
        if (either.isLeft()) {
            final InvalidCodeSequence errorCode = either.getLeft();

            final Alert alert = new Alert(AlertType.ERROR);

            alert.setHeaderText("Invalid Code Sequence");
            alert.setContentText(InvalidCodeSequence.errorMessage(currentCodeNumbers[i], errorCode));
            alert.showAndWait();
        } else {
            // need to get the storage and compile the equations
            final ClassifiedCodeSequence codeSeq = either.get();

            final Optional<Storage> optional = Database.loadStorage(codeSeq, pool);

			if (optional.isPresent()) {
				final Storage storage = optional.get();
				result = codeSeq.toString();
				final String value = calculateChooser.getValue();
				if (value.equals("Region") || value.equals("All")) {
					final int index = cycle.get();
					final Color color = comboBoxColors.get(index);
					addToOnScreenSequences(storage, color);
					renderRegion(storage, (WritableImage) regionsImageView.getImage(), color);
				}
				if (storage.classCodeSeq.stable) {
					final Storage.Stable stable = (Storage.Stable) storage;
					if (value.equals("MRR") || value.equals("All")) {
						final ConvexPolygon innerPoly = stable.polygon;
						innerPolyBounds.add(innerPoly);
						renderPolygon(innerPoly, (WritableImage) boundsImageView.getImage(), polyBoundColor);
					}
					if (value.equals("Bound") || value.equals("All")) {
						// Change outerPoly to the stable.outerPolygon when that exists
						final ConvexPolygon outerPoly = ConvexPolygon.create(Database.parsePoints(Wrapper.boundingPolygon(stable.classCodeSeq, pool)));
						outerPolyBounds.add(outerPoly);
						renderPolygon(outerPoly, (WritableImage) boundsImageView.getImage(), polyBoundColor);
					}
				}
			} else {
                // Zhao Yu Li, May 02, 2025.
                // Add '//' before 'empty set' so the cover can ignore it
				result = "// empty set " + codeSeq;
			}
			codeWindow.show();
		}
        return result;
    }

    private void drawHorizontalLine(final double y, final double x1, final double x2,
                                    final PixelWriter pixelWriter, final Color color) {
        final int pixelY = (int) map.pixelY(y);

        if (0 <= pixelY && pixelY < SIDE) {
            for (int pixelX = 0; pixelX < SIDE; pixelX += 1) {
                final double radianX = map.radianX(pixelX + 0.5);
                if (x1 <= radianX && radianX <= x2) {
                    pixelWriter.setColor(pixelX, pixelY, color);
                }
            }
        }
    }

    // x, y1, y2 are in radians
    // Pixels go from 0 to 599, as usual (not 600!)
    private void drawVerticalLine(final double x, final double y1, final double y2,
                                  final PixelWriter pixelWriter, final Color color) {
        final int pixelX = (int) map.pixelX(x);

        if (0 <= pixelX && pixelX < SIDE) {
            for (int pixelY = 0; pixelY < SIDE; pixelY += 1) {
                final double radianY = map.radianY(pixelY + 0.5);
                if (y1 <= radianY && radianY <= y2) {
                    pixelWriter.setColor(pixelX, pixelY, color);
                }
            }
        }
    }

    private void drawObliqueLine(final DoubleUnaryOperator y, final double x1, final double x2,
                                 final DoubleUnaryOperator x, final double y1, final double y2,
                                 final PixelWriter pixelWriter, final Color color) {

        // let's iterate across the x values
        for (int pixelX = 0; pixelX < SIDE; pixelX += 1) {
            final double radianX = map.radianX(pixelX);
            final double radianY = y.applyAsDouble(radianX);

            if (y1 <= radianY && radianY <= y2) {
                final int pixelY = (int) map.pixelY(radianY);

                if (0 <= pixelY && pixelY < SIDE) {
                    pixelWriter.setColor(pixelX, pixelY, color);
                }
            }
        }

        // now iterate over the rows
        for (int pixelY = 0; pixelY < SIDE; pixelY += 1) {
            final double radianY = map.radianY(pixelY);
            final double radianX = x.applyAsDouble(radianY);

            // is it part of the line segment?
            if (x1 <= radianX && radianX <= x2) {
                final int pixelX = (int) map.pixelX(radianX);

                // is it on screen?
                if (0 <= pixelX && pixelX < SIDE) {
                    pixelWriter.setColor(pixelX, pixelY, color);
                }
            }
        }
    }

    private WritableImage renderGuideLines() {
        // render the lines in the background image
        final WritableImage image = new WritableImage(SIDE, SIDE);
        final PixelWriter pixelWriter = image.getPixelWriter();

        // we have several horizontal lines, several vertical lines, and oblique ones
        drawHorizontalLine(0, 0, Math.PI, pixelWriter, lineColor);
        drawVerticalLine(0, 0, Math.PI, pixelWriter, lineColor);

        drawHorizontalLine(Math.PI / 2, 0, Math.PI / 2, pixelWriter, lineColor);
        drawVerticalLine(Math.PI / 2, 0, Math.PI / 2, pixelWriter, lineColor);

        // this is the line y = 67.5 from x = 0 to x= 0.5
        drawHorizontalLine(3 * Math.PI / 8, 0, Math.PI / 360, pixelWriter, lineColor);

        /*
        drawObliqueLine(x -> x, 0, Math.PI / 2,
                y -> y, 0, Math.PI / 2,
                pixelWriter);
         */

        // x = 12 degrees
        drawVerticalLine(Math.PI / 15, Math.PI / 15, 37 * Math.PI / 120, pixelWriter, lineColor);

        // x + y = 90
        drawObliqueLine(x
                        -> Math.PI / 2 - x,
                0, Math.PI / 2, y -> Math.PI / 2 - y, 0, Math.PI / 2, pixelWriter, lineColor);

        // x + y = 180
        drawObliqueLine(
                x -> Math.PI - x, 0, Math.PI, y -> Math.PI - y, 0, Math.PI, pixelWriter, lineColor);

        // IMPORTANT: This is the line x + y = 80
        drawObliqueLine(x
                        -> 4 * Math.PI / 9 - x,
                0, 4 * Math.PI / 9,
                y -> 4 * Math.PI / 9 - y, 0, 4 * Math.PI / 9, pixelWriter, lineColor);

        // IMPORTANT: This is the line x + y = 75
        drawObliqueLine(x
                        -> 15 * Math.PI / 36 - x,
                0, 15 * Math.PI / 36,
                y -> 15 * Math.PI / 36 - y, 0, 15 * Math.PI / 36, pixelWriter, lineColor);

        // IMPORTANT: This is the line x + y = 70
        drawObliqueLine(x
                        -> 7 * Math.PI / 18 - x,
                0, 7 * Math.PI / 18,
                y -> 7 * Math.PI / 18 - y, 0, 7 * Math.PI / 18, pixelWriter, lineColor);

        // IMPORTANT: This is the line x + y = 68
        drawObliqueLine(x
                        -> 17 * Math.PI / 45 - x,
                0, 17 * Math.PI / 45,
                y -> 17 * Math.PI / 45 - y, 0, 17 * Math.PI / 45, pixelWriter, lineColor);

        // This is the line x + y = 67.7
        drawObliqueLine(x
                        -> 677 * Math.PI / 1800 - x,
                0, 677 * Math.PI / 1800,
                y -> 677 * Math.PI / 1800 - y, 0, 677 * Math.PI / 1800, pixelWriter, lineColor);

        // This is the line x + y = 67.6
        drawObliqueLine(x
                        -> 169 * Math.PI / 450 - x,
                0, 169 * Math.PI / 450,
                y -> 169 * Math.PI / 450 - y, 0, 169 * Math.PI / 450, pixelWriter, lineColor);

        // This is the line x + y = 67.55
        drawObliqueLine(x
                        -> 1351 * Math.PI / 3600 - x,
                0, 1351 * Math.PI / 3600,
                y -> 1351 * Math.PI / 3600 - y, 0, 1351 * Math.PI / 3600, pixelWriter, lineColor);

        // This is the line x + y = 67.5
        drawObliqueLine(x
                        -> 15 * Math.PI / 40 - x,
                0, 15 * Math.PI / 40,
                y -> 15 * Math.PI / 40 - y, 0, 15 * Math.PI / 40, pixelWriter, lineColor);

        // IMPORTANT: This is the line x + y = 66
        drawObliqueLine(x
                        -> 11 * Math.PI / 30 - x,
                0, 11 * Math.PI / 30,
                y -> 11 * Math.PI / 30 - y, 0, 11 * Math.PI / 30, pixelWriter, lineColor);

        // IMPORTANT: This is the line x + y = 45
        drawObliqueLine(x
                        -> Math.PI / 4 - x,
                0, Math.PI / 4, y -> Math.PI / 4 - y, 0, Math.PI / 4, pixelWriter, lineColor);

        // IMPORTANT: This is the line x + y = 35
        drawObliqueLine(x
                        -> 35 * Math.PI / 180 - x,
                0, 35 * Math.PI / 180,
                y -> 35 * Math.PI / 180 - y, 0, 35 * Math.PI / 180, pixelWriter, lineColor);

        // IMPORTANT: This is the line x = y
        drawObliqueLine(x -> x, 0, Math.PI / 4, y -> y, 0, Math.PI / 4, pixelWriter, lineColor);

        return image;
    }

    private void renderPolygon(
            final ConvexPolygon poly, final WritableImage image, final Color color) {
        final PixelWriter pixelWriter = image.getPixelWriter();

        final ImmutableList<Vector2> vertices = poly.vertices;
        final int size = vertices.size();

        for (int i = 0; i < size; ++i) {
            final Vector2 a = vertices.get(i);
            final Vector2 b = vertices.get((i + 1) % size);

            // horizontal
            if (a.y == b.y) {
                drawHorizontalLine(a.y, Math.min(a.x, b.x), Math.max(a.x, b.x), pixelWriter, color);
            }
            // vertical
            else if (a.x == b.x) {
                drawVerticalLine(a.x, Math.min(a.y, b.y), Math.max(a.y, b.y), pixelWriter, color);
            }
            // diagonal
            else {
                final double slopeY = (b.y - a.y) / (b.x - a.x);
                final DoubleUnaryOperator funcY = x -> slopeY * (x - a.x) + a.y; // y(x)

                final double slopeX = (b.x - a.x) / (b.y - a.y);
                final DoubleUnaryOperator funcX = y -> slopeX * (y - a.y) + a.x; // x(y)

                drawObliqueLine(funcY, Math.min(a.x, b.x), Math.max(a.x, b.x), funcX,
                        Math.min(a.y, b.y), Math.max(a.y, b.y), pixelWriter, color);
            }
        }
    }

/*
    private void renderRect(final Rectangle rect, final WritableImage image,
                            final Color colorInside, final Color colorBound,
                            int magnification) {
        if (magnification == 0) {
            renderRect(rect, image, colorInside, colorBound);
            return;
        }
        final PixelReader pixelReader = image.getPixelReader();
        final int startPX = Math.max((int) map.pixelX(rect.intervalX.min), 0);
        final int startPY = Math.max((int) map.pixelY(rect.intervalY.min), 0);
        final int endPX = Math.min((int) map.pixelX(rect.intervalX.max), SIDE);
        final int endPY = Math.min((int) map.pixelY(rect.intervalY.max), SIDE);

        try {
            if (pixelReader.getColor(startPX, startPY) != colorInside && pixelReader.getColor(startPX, startPY) != colorBound
                    && pixelReader.getColor(endPX, startPY) != colorInside && pixelReader.getColor(endPX, startPY) != colorBound
                    && pixelReader.getColor(startPX, endPY) != colorInside && pixelReader.getColor(startPX, endPY) != colorBound
                    && pixelReader.getColor(endPX, endPY) != colorInside && pixelReader.getColor(startPX, startPY) != colorBound) {
                renderRect(rect, image, colorInside, colorBound);
                return;
            }
            MutableList<Rectangle> quaters = rect.subdivide();
            for (Rectangle quater : quaters) {
                renderRect(quater, image, colorInside, colorBound, magnification - 1);
            }
        }
        catch (IndexOutOfBoundsException e) {
            renderRect(rect, image, colorInside, colorBound);

            return;
        }
    }
*/

    private void renderRect(final Rectangle rect, final WritableImage image,
                            final Color colorInside, final Color colorBound) {

        final PixelWriter pixelWriter = image.getPixelWriter();
        final PixelReader pixelReader = image.getPixelReader();
        final int startPX = Math.max((int) map.pixelX(rect.intervalX.min), 0);
        final int startPY = Math.max((int) map.pixelY(rect.intervalY.min), 0);
        final int endPX = Math.min((int) map.pixelX(rect.intervalX.max), SIDE);
        final int endPY = Math.min((int) map.pixelY(rect.intervalY.max), SIDE);
        ClassifiedCodeSequence codeSequence = null;
        Storage.Stable stable = null;

        if (coverRects.stableEntrySet().contains(rect)) {
            codeSequence = coverRects.getStable(rect);
        }

        if (rect.trimable && coverRects.HalfTripleEntrySet().contains(rect)) {
            //System.out.println("2");
            codeSequence = coverRects.getHalfTriple(rect).stableNeg;
            IntList intList = Utils.splitString("1 1 1").get();
            Either<InvalidCodeSequence, ClassifiedCodeSequence> unstable = ClassifiedCodeSequence.create(intList);
            //System.out.println(unstable.get());
            //System.out.println(codeSequence);
            if (unstable.get().equals(codeSequence)) {
                //System.out.println("3");
                codeSequence = null;
            }
        }

        // George, August 26, 2021: with the line below, the trim is on now with "//"; It will be off when you remove "//"
        //codeSequence = null;

        if (codeSequence != null) {
            //System.out.println("4");
            final Optional<Storage> optional = Database.loadStorage(codeSequence, pool);
            if (optional.isPresent()) {
                if (codeSequence.stable) {
                    stable = (Storage.Stable) optional.get();
                }
            }
        }

        if (!allCheckBox.isSelected()) {
            for (int i = startPX; i <= endPX; i++) {
                for (int j = startPY; j <= endPY; j++) {
                    if (SIDE > i && i >= 0 && SIDE > j && j >= 0) {
                        double rx = map.radianX(i);
                        double ry = map.radianY(j);
                        if (coverArea.isPresent() && coverArea.get().location(rx, ry) == Location.OUTSIDE) {
                            //continue;
                        }
                        if (stable != null && stable.polygon.location(rx, ry) == Location.OUTSIDE) {
                            continue;
                        }
                        try {
                            if(pixelReader.getColor(i, j) != colorInside && pixelReader.getColor(i, j) != colorBound) {
                                if ((i == startPX || i == endPX || j == startPY || j == endPY)) {
                                    pixelWriter.setColor(i, j, colorBound);
                                } else {
                                    pixelWriter.setColor(i, j, colorInside);
                                }
                            }
                        }
                        catch (final IndexOutOfBoundsException e) {
                            System.out.println("i: " + i + "  j: " + j);
                        }
                    }
                }
            }
        }
        else {
            for (int i = startPX; i <= endPX; i++) {
                for (int j = startPY; j <= endPY; j++) {
                    double rx = map.radianX(i);
                    double ry = map.radianY(j);
                    final double rz = Math.PI - (rx + ry);
                    final double[][] coords = { {rx, rz}, {ry, rz}, {rz, rx}, {rz, ry}, {ry, rx}, {rx, ry}};
                    if (SIDE > i && i >= 0 && SIDE > j && j >= 0) {
                        if ((i == startPX || i == endPX || j == startPY || j == endPY)) {
                            try {
                                for (final double[] coord : coords) {
                                    if (map.getViewRectangle().contains(coord[0], coord[1])) {
                                        if (coord[0] > 0 && coord[1] > 0 && (Math.PI - (coord[0] + coord[1])) > 0) {
                                            final int px = (int) map.pixelX(coord[0]);
                                            final int py = (int) map.pixelY(coord[1]);
                                            pixelWriter.setColor(px, py, colorBound);
                                        }
                                    }
                                }
                            } catch (final IndexOutOfBoundsException e) {
                                System.out.println("i: " + i + "  j: " + j);
                            }

                        } else {
                            for (final double[] coord : coords) {
                                if (map.getViewRectangle().contains(coord[0], coord[1])) {
                                    if (coord[0] > 0 && coord[1] > 0 && (Math.PI - (coord[0] + coord[1])) > 0) {
                                        final int px = (int) map.pixelX(coord[0]);
                                        final int py = (int) map.pixelY(coord[1]);
                                        pixelWriter.setColor(px, py, colorInside);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderRectLoad(final Rectangle rect, final WritableImage image,
                                final Color colorInside, final Color colorBound,
                                final RenderOptions options) {

        final PixelWriter pixelWriter = image.getPixelWriter();
        final PixelReader pixelReader = image.getPixelReader();
        final int startPX = Math.max((int) map.pixelX(rect.intervalX.min), 0);
        final int startPY = Math.max((int) map.pixelY(rect.intervalY.min), 0);
        final int endPX = Math.min((int) map.pixelX(rect.intervalX.max), SIDE);
        final int endPY = Math.min((int) map.pixelY(rect.intervalY.max), SIDE);

        if (!options.allSelected) {
            for (int i = startPX; i <= endPX; i++) {
                for (int j = startPY; j <= endPY; j++) {
                    if (SIDE > i && i >= 0 && SIDE > j && j >= 0) {
                        double rx = map.radianX(i);
                        double ry = map.radianY(j);
                        if (coverArea.isPresent() && coverArea.get().location(rx, ry) == Location.OUTSIDE) {
                            //continue;
                        }
                        try {
                            if(pixelReader.getColor(i, j) != colorInside && pixelReader.getColor(i, j) != colorBound) {
                                if ((i == startPX || i == endPX || j == startPY || j == endPY)) {
                                    pixelWriter.setColor(i, j, colorBound);
                                } else {
                                    pixelWriter.setColor(i, j, colorInside);
                                }
                            }
                        }
                        catch (final IndexOutOfBoundsException e) {
                            System.out.println("i: " + i + "  j: " + j);
                        }
                    }
                }
            }
        }
        else {
            for (int i = startPX; i <= endPX; i++) {
                for (int j = startPY; j <= endPY; j++) {
                    double rx = map.radianX(i);
                    double ry = map.radianY(j);
                    final double rz = Math.PI - (rx + ry);
                    final double[][] coords = { {rx, rz}, {ry, rz}, {rz, rx}, {rz, ry}, {ry, rx}, {rx, ry}};
                    if (SIDE > i && i >= 0 && SIDE > j && j >= 0) {
                        if ((i == startPX || i == endPX || j == startPY || j == endPY)) {
                            try {
                                for (final double[] coord : coords) {
                                    if (map.getViewRectangle().contains(coord[0], coord[1])) {
                                        if (coord[0] > 0 && coord[1] > 0 && (Math.PI - (coord[0] + coord[1])) > 0) {
                                            final int px = (int) map.pixelX(coord[0]);
                                            final int py = (int) map.pixelY(coord[1]);
                                            pixelWriter.setColor(px, py, colorBound);
                                        }
                                    }
                                }
                            } catch (final IndexOutOfBoundsException e) {
                                System.out.println("i: " + i + "  j: " + j);
                            }

                        } else {
                            for (final double[] coord : coords) {
                                if (map.getViewRectangle().contains(coord[0], coord[1])) {
                                    if (coord[0] > 0 && coord[1] > 0 && (Math.PI - (coord[0] + coord[1])) > 0) {
                                        final int px = (int) map.pixelX(coord[0]);
                                        final int py = (int) map.pixelY(coord[1]);
                                        pixelWriter.setColor(px, py, colorInside);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderUnstable(final Storage.Unstable unstable, final PixelWriter pixelWriter,
                                final Rectangle viewRectangle, final Color color,
                                final RenderOptions options) {
        final List<Vector2> points = new ArrayList<>();

        final int xCoeff = unstable.constraint.coeff(XYPi.X);
        final int yCoeff = unstable.constraint.coeff(XYPi.Y);
        final int piCoeff = unstable.constraint.coeff(XYPi.Pi);

        final double halfWidth = map.pixelSize() / 2;
        final double offset = getOffset();

        int startPixelX = 0;
        int startPixelY = 0;
        int endPixelX = SIDE;
        int endPixelY = SIDE;
        int extraPixelX = 0;
        int extraPixelY = 0;

        if (options.allSelected) {
            final double x1 = (viewRectangle.intervalX.min + viewRectangle.intervalX.max) / 2;
            final double y1 = (viewRectangle.intervalY.min + viewRectangle.intervalY.max) / 2;
            final List<Double> translate = Arrays.asList(x1, y1, Math.PI - (x1 + y1));
            Collections.sort(translate);
            startPixelX = (int) map.pixelX(translate.get(0)) - SIDE;
            startPixelY = (int) map.pixelY(translate.get(1)) - SIDE;
            endPixelX = startPixelX + (SIDE * 2);
            endPixelY = startPixelY + (SIDE * 2);
            extraPixelX = (int) map.pixelX(translate.get(1)) - SIDE;
            extraPixelY = (int) map.pixelY(translate.get(0)) - SIDE;
        }

        if (xCoeff == 0) {
            // horizontal line
            // solve for y
            final double y = -(double) piCoeff / yCoeff * Math.PI;

            if (viewRectangle.intervalY.contains(y) || options.allSelected) {
                // now we iterate over the vertical lines
                for (int i = startPixelX; i < endPixelX; i += 1) {
                    final double radX = map.radianX(i);
                    if (options.proverSelected) {
                        if (!unstable.isPositiveProver(radX, y, halfWidth, offset)) {
                            continue;
                        }
                    }
                    final Vector2 point = Vector2.create(radX, y);
                    points.add(point);
                }
            }
        } else if (yCoeff == 0) {
            // vertical line
            // solve for x
            final double x = -(double) piCoeff / xCoeff * Math.PI;

            if (viewRectangle.intervalX.contains(x) || options.allSelected) {
                // now iterate over the horizontal lines
                for (int i = startPixelY; i < endPixelY; i += 1) {
                    final double radY = map.radianY(i);
                    if (options.proverSelected) {
                        if (!unstable.isPositiveProver(x, radY, halfWidth, offset)) {
                            continue;
                        }
                    }
                    final Vector2 point = Vector2.create(x, radY);
                    points.add(point);
                }
            }
        } else {
            // oblique line
            // we can solve for x and y
            final DoubleUnaryOperator x = radY
                    -> - (double) yCoeff / xCoeff * radY - (double) piCoeff / (double) xCoeff * Math.PI;

            final DoubleUnaryOperator y = radX
                    -> - (double) xCoeff / yCoeff * radX - (double) piCoeff / (double) yCoeff * Math.PI;

            for (int i = startPixelX; i < endPixelX; i += 1) {
                final double radX = map.radianX(i);
                final double radY = y.applyAsDouble(radX);
                if (options.proverSelected) {
                    if (!unstable.isPositiveProver(radX, radY, halfWidth, offset)) {
                        continue;
                    }
                }
                // need to make sure intersection is in the viewing box
                if (viewRectangle.intervalY.contains(radY) || options.allSelected) {
                    final Vector2 point = Vector2.create(radX, radY);
                    points.add(point);
                }
            }

            for (int i = startPixelY; i < endPixelY; i += 1) {
                final double radY = map.radianY(i);
                final double radX = x.applyAsDouble(radY);
                if (options.proverSelected) {
                    if (!unstable.isPositiveProver(radX, radY, halfWidth, offset)) {
                        continue;
                    }
                }
                if (viewRectangle.intervalX.contains(radX) || options.allSelected) {
                    final Vector2 point = Vector2.create(radX, radY);
                    points.add(point);
                }
            }
            if (options.allSelected) {
                for (int i = extraPixelX; i < extraPixelX + (SIDE * 2); i += 1) {
                    final double radX = map.radianX(i);
                    final double radY = y.applyAsDouble(radX);
                    if (options.proverSelected) {
                        if (!unstable.isPositiveProver(radX, radY, halfWidth, offset)) {
                            continue;
                        }
                    }
                    final Vector2 point = Vector2.create(radX, radY);
                    points.add(point);
                }

                for (int i = extraPixelY; i < extraPixelY + (SIDE * 2); i += 1) {
                    final double radY = map.radianY(i);
                    final double radX = x.applyAsDouble(radY);
                    if (options.proverSelected) {
                        if (!unstable.isPositiveProver(radX, radY, halfWidth, offset)) {
                            continue;
                        }
                    }
                    final Vector2 point = Vector2.create(radX, radY);
                    points.add(point);
                }
            }

            // now sort the points in lexicographical order
            final Comparator<Vector2> ordering = (a, b) -> {
                final double ax = a.x;
                final double ay = a.y;

                final double bx = b.x;
                final double by = b.y;

                if ((ax == bx) && (ay == by)) {
                    // a == b
                    return 0;
                } else if ((ax < bx) || (ax == bx && ay < by)) {
                    // a < b
                    return -1;
                } else {
                    // a > b
                    return 1;
                }
            };

            points.sort(ordering);
        }

        double startX = unstable.lineSegment.start.x;
        double endX = unstable.lineSegment.end.x;
        double startY = unstable.lineSegment.start.y;
        double endY = unstable.lineSegment.end.y;

        if (startX == endX) {
            startX = startX - 0.01;
            endX = endX + 0.01;
        } else if (startY == endY) {
            startY = startY - 0.01;
            endY = endY + 0.01;
        }

        final Rectangle boundingRectangle = Rectangle.create(startX, endX, startY, endY);

        for (int i = 0; i < points.size() - 1; i += 1) {
            final Vector2 point = points.get(i);
            final Vector2 nextPoint = points.get(i + 1);

            final Vector2 midPoint = point.add(nextPoint).scale(0.5);
            final double rx = midPoint.x;
            final double ry = midPoint.y;

            if ((boundingRectangle.contains(rx, ry) && unstable.isPositive(rx, ry))) {
                final int px = (int) map.pixelX(rx);
                final int py = (int) map.pixelY(ry);
                try {
                    pixelWriter.setColor(px, py, color);
                } catch (final IndexOutOfBoundsException e) {
                }
            }
            if (options.allSelected) {
                final double rz = Math.PI - (rx + ry);
                final double[][] coords = {
                        {rx, rz}, {ry, rz}, {rz, rx}, {rz, ry}, {ry, rx}, {rx, ry}};
                for (final double[] coord : coords) {
                    if ((boundingRectangle.contains(rx, ry) && unstable.isPositive(rx, ry) &&
                            map.getViewRectangle().contains(coord[0], coord[1]))) {
                        if (coord[0] > 0 && coord[1] > 0 && (Math.PI - (coord[0] + coord[1])) > 0) {
                            final int px = (int) map.pixelX(coord[0]);
                            final int py = (int) map.pixelY(coord[1]);
                            pixelWriter.setColor(px, py, color);
                        }
                    }
                }
            }
        }
    }

    void renderRegion(final Storage region, final WritableImage image, final Color color) {
        renderRegion(region, image, color, snapshotRenderOptions());
    }

    private void renderRegion(
            final Storage region,
            final WritableImage image,
            final Color color,
            final RenderOptions options) {
        final PixelReader pixelReader = image.getPixelReader();
        final PixelWriter pixelWriter = image.getPixelWriter();

        // now we create a rectangle that describes the current viewing screen
        final Rectangle viewRectangle = map.getViewRectangle();
        if (region.intersects(viewRectangle) || options.allSelected) {
            if (region.classCodeSeq.stable) {
                final Storage.Stable stable = (Storage.Stable) region;

                final double halfWidth = map.pixelSize() / 2;
                final double offset = getOffset();

                // Determine the color of each pixel in a specified row
                for (int readY = 0; readY < SIDE; readY += 1) {
                    final double ry = map.radianY(readY + 0.5);
                    for (int readX = 0; readX < SIDE; readX += 1) {
                        final double rx = map.radianX(readX + 0.5);
                        final Color pixelColor = pixelReader.getColor(readX, readY);
                        // if it's not colored our color already, then let's see if we can color it
                        if (pixelColor != color && !color.equals(Color.TRANSPARENT)) {
                            // if the point is inside the bounding rectangle and is positive
                            if (options.allSelected) { // all feature part 1/3
                                final List<Double> subList =
                                        Arrays.asList(rx, ry, Math.PI - (rx + ry));
                                Collections.sort(subList);
                                final Location location =
                                        stable.polygon.location(subList.get(0), subList.get(1));
                                if (location == Location.INSIDE) {
                                    if (options.boundsSelected) {
                                        pixelWriter.setColor(readX, readY, fillBoundColor);
                                    }
                                    if (options.proverSelected) {
                                        if (stable.isPositiveProver(subList.get(0), subList.get(1),
                                                halfWidth, offset)) {
                                            pixelWriter.setColor(readX, readY, color);
                                        }
                                    } else {
                                        if (stable.isPositive(subList.get(0), subList.get(1))) {
                                            pixelWriter.setColor(readX, readY, color);
                                        }
                                    }
                                }
                            } else {
                                final Location location = stable.polygon.location(rx, ry);
                                if (location == Location.INSIDE) {
                                    if (options.boundsSelected) {
                                        pixelWriter.setColor(readX, readY, fillBoundColor);
                                    }
                                    if (options.proverSelected) {
                                        if (stable.isPositiveProver(rx, ry, halfWidth, offset)) {
                                            pixelWriter.setColor(readX, readY, color);
                                        }
                                    } else {
                                        if (stable.isPositive(rx, ry)) {
                                            pixelWriter.setColor(readX, readY, color);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                renderUnstable((Storage.Unstable) region, pixelWriter, viewRectangle, color, options);
            }
        }
    }

    public void loadHolesFromFile() {
        final Path path = Paths.get("tmp/holes.txt");
        if (!Files.exists(path)) {
            final Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Load Holes");
            alert.setHeaderText("File Not Found");
            alert.setContentText("tmp/holes.txt does not exist. Run a cover calculation first.");
            alert.showAndWait();
            return;
        }

        final ArrayList<String> lines;
        try {
            lines = new ArrayList<>(Files.readAllLines(path));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        lines.removeIf(String::isBlank);

        final int maxLines = lines.size();
        final TextInputDialog dialog = new TextInputDialog(String.valueOf(maxLines));
        dialog.setTitle("Load Holes");
        dialog.setHeaderText("How many holes to load?");
        dialog.setContentText("How many holes to load? (max " + maxLines + "):");

        final Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                final int n = Math.min(Integer.parseInt(input.trim()), maxLines);
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < n; i++) {
                    sb.append(lines.get(i)).append('\n');
                }
                loadHolesAsOBO(sb.toString());
            } catch (final NumberFormatException e) {
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Load Holes");
                alert.setHeaderText("Invalid Number");
                alert.setContentText("Please enter a valid integer.");
                alert.showAndWait();
            }
        });
    }

    public void loadHolesAsOBO(final String coords) {
        final Path path = Paths.get("tmp/holes_obo.txt");
        try {
            Files.write(path, coords.getBytes());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        // Reuse the one-by-one parser/renderer so holes behave like any other
        // coordinate OBO file and inherit existing forward/back controls.
        fileCodeSequences = parseOBOFile(path);
        if (!fileCodeSequences.isEmpty()) {
            lineNumberTxt.setText("1");
            setOBO(0, pool, executorService);
        }
    }

    public static ArrayList<String> parseOBOFile(final Path path) {
        final ArrayList<String> list = new ArrayList<>();

        try (final Stream<String> stream = Files.lines(path)) {
            stream.forEach(line -> {

                line = Utils.trimCodeLine(line);

                if (Utils.isCoords(line)) {
                    list.add(line);
                }

                else if (!line.isEmpty() && !line.contains("s") && !line.contains("S")
                        && !line.contains("rectangle")) {
                    final Optional<ImmutableIntList> optional = Utils.splitString(line);
                    if (optional.isPresent()) {
                        final ImmutableIntList codeNumbers = optional.get();
                        // an invalid code sequence in an input file is very bad
                        // it likely means there is a typo in the file
                        // that is why we use an unchecked get
                        final ClassifiedCodeSequence codeSeq =
                                ClassifiedCodeSequence.create(codeNumbers).get();
                        list.add(codeSeq.toString());
                    } else {
                        throw new RuntimeException("Invalid line in file: " + line);
                    }
                }
            });
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    public static Tuple2<Tuple3<
            Optional<Rectangle>,
            Map<ClassifiedCodeSequence, Optional<Color>>,
            Map<ClassifiedCodeSequence, Optional<String[]>>
            >,
            ArrayList<ClassifiedCodeSequence[]>>
    parseFile(final Path path, boolean print) {
        return parseFile(path, print, false);
    }

    public static Tuple2<Tuple3<
            Optional<Rectangle>,
            Map<ClassifiedCodeSequence, Optional<Color>>,
            Map<ClassifiedCodeSequence, Optional<String[]>>
            >,
            ArrayList<ClassifiedCodeSequence[]>>
    parseFile(final Path path, boolean print, boolean addToGarbage) {
        // we want to load the code sequences in the order they are given in the file
        // we also do not want duplicates
        // Use mutable JDK collection here

        Optional<Rectangle> optRect = Optional.empty();
        final LinkedHashMap<ClassifiedCodeSequence, Optional<Color>> map = new LinkedHashMap<>();
        final LinkedHashMap<ClassifiedCodeSequence, Optional<String[]>> optIter =
                new LinkedHashMap<>();
        final ArrayList<ClassifiedCodeSequence[]> triples = new ArrayList<>();
        int count = 0;

        // Inside the file, we allow comments starting with //
        // 5's, 6's, etc.
        // OSO, OSNO, CS, CNS, ONS
        try (final Stream<String> stream = Files.lines(path)) {
            for (String line : (Iterable<String>) stream::iterator) {
                count += 1;
                line = Utils.trimCodeLine(line);

                // Zhao Yu Li, May 05, 2025.
                // Prints code from file
                if (print) System.out.println(line);

                if (!line.isEmpty() && !line.contains("s") && !line.contains("S") &&
                        !line.contains("rectangle") && !line.contains("+")) {
                    final String[] sections = line.split(",");
                    if (sections.length == 3) {
                        // triple
                        // Zhao Yu Li, May 21, 2025.
                        // This StringBuilder is used to add the triple (in one line) to the garbage DB
                        final StringBuilder tripleString = new StringBuilder();

                        // This generic array is used to maintain the triple relationship of the three components
                        ClassifiedCodeSequence[] triple = new ClassifiedCodeSequence[3];

                        for (int i = 0; i < 3; i++) {
                            final String sequenceString = sections[i].trim();
                            final ImmutableIntList sequence = Utils.splitString(sequenceString).get();

                            final ClassifiedCodeSequence codeSeq =
                                    ClassifiedCodeSequence.create(sequence).get();

                            // Zhao Yu Li, May 21, 2025.
                            // Add triples to garbage, and maintain triple relationship
                            if (addToGarbage) tripleString.append(codeSeq.toString()).append(",");
                            triple[i] = codeSeq;
                        }

                        triples.add(triple);

                        // Delete the last added comma
                        tripleString.deleteCharAt(tripleString.length() - 1);
                        Database.saveTripleToDatabase(tripleString.toString(), "garbage");
                    } else {
                        // single
                        final String sequenceString = sections[0].trim();
                        final ImmutableIntList sequence = Utils.splitString(sequenceString).get();

                        final ClassifiedCodeSequence codeSeq =
                                ClassifiedCodeSequence.create(sequence).get();

                        final Optional<Color> optColor;
                        if (sections.length == 2) {
                            final String colorString = sections[1].trim();
                            final Color color = Color.web(colorString);
                            optColor = Optional.of(color);
                        } else {
                            optColor = Optional.empty();
                        }

                        map.put(codeSeq, optColor);

                        final Optional<String[]> lineOptIter = Optional.empty();
                        optIter.put(codeSeq, lineOptIter);
                    }
                } else if (line.contains("rectangle")) {
                    final String[] sections = line.split(" ");
                    final double minX = Double.parseDouble(sections[1]);
                    final double maxX = Double.parseDouble(sections[2]);
                    final double minY = Double.parseDouble(sections[3]);
                    final double maxY = Double.parseDouble(sections[4]);

                    final Rectangle rect = Rectangle.create(minX, maxX, minY, maxY);
                    optRect = Optional.of(rect);

                } else if (line.contains("+")) {
                    line = line.replace("+", "");
                    final String[] sections = line.split("#");
                    for (int i = 0; i < sections[0].trim().split(",").length; i++) {
                        final String sequenceString = sections[0].trim().split(",")[i];
                        final ImmutableIntList sequence = Utils.splitString(sequenceString).get();
                        final ClassifiedCodeSequence codeSeq = ClassifiedCodeSequence.create(sequence).get();
                        final Optional<Color> optColor = Optional.empty();
                        map.put(codeSeq, optColor);
                        final ArrayList<String> intermediate = new ArrayList<>(Arrays.asList(sections));
                        intermediate.remove(0);
                        final String[] sectionsCut = new String[intermediate.size()];
                        for (int j = 0; j < sectionsCut.length; j++) {
                            sectionsCut[j] = intermediate.get(j).split(",")[i];
                        }
                        final Optional<String[]> lineOptIter = Optional.of(sectionsCut);
                        optIter.put(codeSeq, lineOptIter);
                    }
                } else if (!line.isEmpty()) {
                    final Alert alert = new Alert(AlertType.INFORMATION);

                    alert.setTitle("Load file");
                    alert.setHeaderText("Load failed");
                    alert.setContentText("Invalid formatting at line " + count + " of the file.");
                    alert.showAndWait();

                    return Tuple.of(Tuple.of(Optional.empty(), new LinkedHashMap<>(), new LinkedHashMap<>()), new ArrayList<>());
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        if (addToGarbage) {
            for (ClassifiedCodeSequence codeSeq : map.keySet())
                Database.saveToDatabase(codeSeq, "garbage");
        }

        return Tuple.of(Tuple.of(optRect, map, optIter), triples);
    }

    private MutableSortedSet<ClassifiedCodeSequence> varyLFunction(final MutableSortedSet<ClassifiedCodeSequence> codesFound, final MutableList<Vector2> points,
                                                                   final int[] maximums, final boolean overrideSS, final int max,
                                                                   final ExecutorService executor2) {
        final int CSmax = maximums[0];
        final int OSOmax = maximums[1];
        final int OSNOmax = maximums[2];
        final int CSmaxSS = maximums[3];
        final int OSOmaxSS = maximums[4];
        final int OSNOmaxSS = maximums[5];

        int count = 1;
        int totalCodes = 0;

        if(overrideSS) {
            System.out.printf("Override side sums: CS-%d OSO-%d OSNO-%d", CSmaxSS, OSOmaxSS, OSNOmaxSS);
        }
        for (Vector2 point : points) {
            System.out.println("");
            System.out.println("//------------- working on point " + count + " -------------"); // george added // sept 27,2017
            final MutableSortedSet<ClassifiedCodeSequence> codes = overrideSS ? boyanMenu.varyTrianglesL(point, CSmaxSS, OSOmaxSS, OSNOmaxSS, executor2) : boyanMenu.varyTrianglesL(point, executor2);
            final MutableSortedSet<ClassifiedCodeSequence> localCodesFound = new TreeSortedSet<>();

            int i = max == 0 ? localCodesFound.size() : max;
            int codeNum = 0;
            final int maxPrint = i;
            List <String> codeList = Arrays.asList(readFromFile(Viewer.tmpDir + "/cover_stables.txt").split(System.lineSeparator()));
            codeList.replaceAll(j -> Utils.tripleTrimmer(j));

            for (ClassifiedCodeSequence code : codes) {
                if(i == 0) break; // Already printed out the first i codes
                if (code.codeType.equals(CodeType.OSO) && code.codeLength > OSOmax) {
                    continue;
                } else if (code.codeType.equals(OSNO) && code.codeLength > OSNOmax) {
                    continue;
                } else if (code.codeType.equals(CodeType.CS) && code.codeLength > CSmax) {
                    continue;
                }
                if (codeNum < maxPrint && !codeList.contains(code.codeSequence.toString()) && i>0) {
                    codeNum += 1;
                    System.out.println(Utils.standard(code, codeNum));
                    Platform.runLater(() -> codesFound.add(code));
                    i -= 1;
                }
            }
            totalCodes += codes.size();
            count += 1;
        }

        System.out.println("//~~~~~~~~~~~~~~~~~~~~~~~~~~~ " + totalCodes
                + " codes found total ~~~~~~~~~~~~~~~~~~~~~~~~~~~");//added // george sept27,2017

        return codesFound;
    }

    // Runs polyVary a set number of times 
    private void superPolyVaryFunction(final Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer> polyVals, final ExecutorService executor) {
        final SimpleObjectProperty<Integer> step = new SimpleObjectProperty<>();
        final ProgressMultiTask overallProgress = new ProgressMultiTask(superAutoCb.isSelected() ? "AutoPolyVary %d out of %d" : "PolyVary %d out of %d", false, 0, SuperPolyVaryLoad.Reps);
        final Array<Color> cycleColors = Array.of(
                Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA,
                Color.CHOCOLATE, Color.ORANGE, Color.PINK, Color.LIME,
                Color.PURPLE, Color.TURQUOISE);

        step.setValue(-1);
        step.addListener((o, oldVal, newVal) -> {
            if(oldVal != -1) {
                final int subdivisions = Integer.parseInt(boyanMenu.autoCycleText.getText());
                final int subdivStep = Integer.parseInt(boyanMenu.cycleStepText.getText());
                boyanMenu.autoCycleText.setText(Integer.toString(Math.max(0, subdivisions + subdivStep)));
            }
            if(newVal >= SuperPolyVaryLoad.Reps || newVal == -1) {
                overallProgress.close();
                return;
            }

            // Zhao Yu Li, Jul 08, 2025.
            // Scale only if the user selected to scale
            boolean magnificationIsSelected = superPolyVaryWindow != null && superPolyVaryWindow.getMagnificationIsSelected();

            if (magnificationIsSelected) {
                // Zhao Yu Li, Jul 08, 2025.
                // Try to get the user enter scale factor
                double scaleFactor = superPolyVaryWindow.getMagnification();

                // Zhao Yu Li, Jul 7, 2025.
                // Scale after every rep
                Interval oldXInterval = map.getViewRectangle().intervalX;
                Interval oldYInterval = map.getViewRectangle().intervalY;
                double oldCenterX = (oldXInterval.min + oldXInterval.max) / 2;
                double oldCenterY = (oldYInterval.min + oldYInterval.max) / 2;

                map.scaleBy(scaleFactor);

                Interval newXInterval = map.getViewRectangle().intervalX;
                Interval newYInterval = map.getViewRectangle().intervalY;
                double newCenterX = (newXInterval.min + newXInterval.max) / 2;
                double newCenterY = (newYInterval.min + newYInterval.max) / 2;

                map.translateXBy(oldCenterX - newCenterX);
                map.translateYBy(oldCenterY - newCenterY);

                viewRectangleBF.add(map.getViewRectangle());
            }

            overallProgress.increment(1);
            final Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer> curVals = Tuple.of(polyVals._1, polyVals._2, polyVals._3, polyVals._4,
                    Math.max(0, polyVals._5 + SuperPolyVaryLoad.BoundCSstep * newVal),
                    Math.max(0, polyVals._6 + SuperPolyVaryLoad.BoundOSOstep * newVal),
                    Math.max(0, polyVals._7 + SuperPolyVaryLoad.BoundOSNOstep * newVal));
            final Optional<Color> curCol;
            if(SuperPolyVaryLoad.ColorCycle) {
                curCol = Optional.of(cycleColors.get(newVal % cycleColors.length()));
            } else {
                curCol = Optional.empty();
            }
            if(superAutoCb.isSelected()) {
                autoPolyVaryFunction(curVals, Optional.of(step), curCol, true, SuperPolyVaryLoad.AutoCover, superPolyVaryWindow.getAutoSmallCover(), executor);
            } else {
                polyVaryFunction(curVals, Optional.of(step), curCol, true, SuperPolyVaryLoad.AutoCover, superPolyVaryWindow.getAutoSmallCover(), executor);
            }
        });
        overallProgress.show();
        step.setValue(0);
    }

    public void autoPolyVaryFunction(final Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer> polyVals,
                                     final Optional<SimpleObjectProperty<Integer>> step, final Optional<Color> colorOpt,
                                     final boolean overrideSS, final boolean autoCover, final boolean autoSmallCover,
                                     final ExecutorService executor
    ) {

        // Zhao Yu Li, Jun 27, 2025.
        // Replaced code block with function call.
        Tuple3<Integer, Integer, Integer> startStepEnd = getStartStepEnd();
        if (startStepEnd._1 == null) return;

        autoVaryArea = Optional.of(polyVals._1);
        // Order is CSmax, OSOmax, OSNOmax, CSmaxSS, OSOmaxSS, OSNOmaxSS
        final int[] maxList = {polyVals._2, polyVals._3, polyVals._4, polyVals._5, polyVals._6, polyVals._7};
        ConvexPolygon area = polyVals._1;
        // Go through all the holes in the specified range
        final int startIdx = startStepEnd._1 - 1;
        final int stepIdx = startStepEnd._2;
        final int endIdx = startStepEnd._3 - 1;
        // Run the AutoPolyVary algorithm parallel to the application so that the screen can be rendered in real time instead
        // of the application appearing to freeze (note that in the latter case, as far as the program is concerned, the screen
        // _is_ updating, but the user is unable to see this happen).
        final int subdivisions = Integer.parseInt(boyanMenu.autoCycleText.getText());
        final int maxMoves = Integer.parseInt(boyanMenu.maxMovesText.getText());
        final int shots = Integer.parseInt(boyanMenu.shotsText.getText());
        System.out.printf(
                "+---------- AutoPolyVary running on %d hole(s): %d shots, %d subdivisions, and %d moves----------+%n",
                endIdx - startIdx + 1,
                shots,
                subdivisions,
                maxMoves
        );
        if(overrideSS) {
            System.out.printf("Overrided Side Sum maximums: CS - %d, OSO - %d, OSNO - %d%n", maxList[3], maxList[4], maxList[5]);
        }
        final ProgressMultiTask progress = new ProgressMultiTask("Line: %d, Stopping at: %d", true, startIdx+1, endIdx+1);
        progress.show();
        final ExecutorService storageExecutor = new PriorityExecutor(Utils.numThreads);
        final ExecutorService shotExecutor = new PriorityExecutor(Utils.numThreads);

        if(autoCover) coverWindow.appendStablesInfo("// Start AutoPolyVary");
        if(!AutoPolyVaryLoad.Reverse) {
            drawAutoPolyVary(maxList, subdivisions, autoCover, autoSmallCover, overrideSS, startIdx, endIdx, stepIdx, area, progress, step, colorOpt, executor, storageExecutor, shotExecutor);
        } else {
            drawAutoPolyVary(maxList, subdivisions, autoCover, autoSmallCover, overrideSS, endIdx, startIdx, -1 * stepIdx, area, progress, step, colorOpt, executor, storageExecutor, shotExecutor);
        }
    }

    private void polyVaryFunction(final Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer> polyVals,
                                  final Optional<SimpleObjectProperty<Integer>> step, final Optional<Color> colorOpt, final boolean overrideSS,
                                  final boolean autoCover, final ExecutorService executor) {
        polyVaryFunction(polyVals, step, colorOpt, overrideSS, autoCover, false, executor);
    }

    // Preforms the necessary preprocessing steps to run drawPolyVary 
    private void polyVaryFunction(final Tuple7<ConvexPolygon, Integer, Integer, Integer, Integer, Integer, Integer> polyVals,
                                  final Optional<SimpleObjectProperty<Integer>> step, final Optional<Color> colorOpt, final boolean overrideSS,
                                  final boolean autoCover, final boolean autoSmallCover, final ExecutorService executor) {
        // Order is CSmax, OSOmax, OSNOmax, CSmaxSS, OSOmaxSS, OSNOmaxSS
        final int[] maxList = {polyVals._2, polyVals._3, polyVals._4, polyVals._5, polyVals._6, polyVals._7};
        final ConvexPolygon area = polyVals._1;
        int subdivisions = Integer.parseInt(boyanMenu.autoCycleText.getText());
        final int sum = Integer.parseInt(boyanMenu.maxMovesText.getText());
        final int shots = Integer.parseInt(boyanMenu.shotsText.getText());

        final double xMin = Math.max(area.projectX().min, map.getViewRectangle().intervalX.min);
        final double xMax = Math.min(area.projectX().max, map.getViewRectangle().intervalX.max);
        final double yMin = Math.max(area.projectY().min, map.getViewRectangle().intervalY.min);
        final double yMax = Math.min(area.projectY().max, map.getViewRectangle().intervalY.max);

        // shooting at points determined by subdivision
        //george may 3,2019 changed the name to polyvary instead of auto vary3
        String headerString = !overrideSS ?
                String.format("+----- poly vary: %d shots, %d moves and %d subdivisions,", shots, sum, subdivisions) +
                        " looking for: " + boyanMenu.typeString() + "-----+" :
                String.format("+----- poly vary: %d shots, Overrided moves and %d subdivisions,", shots, subdivisions) +
                        " looking for: Overrided -----+" ;
        if (proverCheckBox.isSelected()) {
            headerString += "  (with prover)";
        }
        System.out.println(headerString);
        if(overrideSS) {
            System.out.println(String.format("Overrided Side Sum maximums: CS - %d, OSO - %d, OSNO - %d", maxList[3], maxList[4], maxList[5]));
        }

        // 2024-05-23 complete redesign of PolyVary to support multi-threading
        final MutableList<Double> points = new FastList<>();
        final MutableList<Double> pointsFiltered = new FastList<>();
        autoRecurse(xMin, xMax, yMin, yMax, 0, subdivisions, area, points); // Generate list of coords
        final Image image = regionsImageView.getImage();
        final PixelReader reader = image.getPixelReader();
        // Filter out filled pixels
        for(int i = 0; i < points.size(); i += 2) {
            final int midX = (int) map.pixelX(points.get(i));
            final int midY = (int) map.pixelY(points.get(i+1));
            int color = reader.getArgb(midX, midY);
            if(color == 0) {
                pointsFiltered.add(points.get(i));
                pointsFiltered.add(points.get(i+1));
            }
        }
        // Now that points have been found, pass to drawPolyVary for calculations 
        final ExecutorService storageExecutor = new PriorityExecutor(Utils.numThreads);
        final ExecutorService shotExecutor = new PriorityExecutor(Utils.numThreads);
        drawPolyVary(pointsFiltered, maxList, area, step, colorOpt, overrideSS, autoCover, autoSmallCover, executor, storageExecutor, shotExecutor);
    }

    // Calculates and draws codes at each of the list of points 
    private void drawPolyVary(final MutableList<Double> points, final int[] max, final ConvexPolygon area,
                              final Optional<SimpleObjectProperty<Integer>> step, final Optional<Color> colorOpt,
                              final boolean overrideSS, final boolean autoCover, final boolean autoSmallCover, final ExecutorService executor,
                              final ExecutorService storageExecutor, final ExecutorService shotExecutor) {
        // We want to filter the codes to avoid recalculating any codes that are already drawn on screen
        final MutableSortedSet<ClassifiedCodeSequence> onScreenCodes = new TreeSortedSet<>();
        onScreenSequences.keySet().forEach(storage -> {onScreenCodes.add(storage.classCodeSeq);});
        // Create the task
        final PolyVaryTask task = new PolyVaryTask(points, onScreenCodes, boyanMenu, Array.ofAll(max), pool, overrideSS, storageExecutor, shotExecutor, regionsImageView, map, 0, 0);
        //final ObservableList<Storage> partials = task.getPartialProperty().get();
        final ProgressWithStatus progress = new ProgressWithStatus(task, "%d / %d", 0);
        // Count the number of holes we start with
        final int startHoles = findHoles(area).size();
        if(autoCover) coverWindow.appendStablesInfo("// Start PolyVary");

        // Update screen when change detected
        task.getPartialProperty().get().addListener((ListChangeListener.Change<? extends Storage> c) -> {
            while (c.next()) {
                if(!c.wasAdded()) continue;
                // Draw all new additions
                c.getAddedSubList().forEach(storage -> {
                    if(!onScreenSequences.containsKey(storage)) {
                        final Color color;
                        if(colorOpt.isPresent()) {
                            color = colorOpt.get();
                        } else {
                            final int index = cycle.get();
                            color = comboBoxColors.get(index);
                        }
                        addToOnScreenSequences(storage, color);
                        renderRegion(storage, (WritableImage) regionsImageView.getImage(), color);

                        // print the code
                        String codeStr = "" + storage.codeType();
                        // String codeStr = "xxx " + type; //george july 26 2017 -
                        // type whatever you want between the quotes in the line above
                        // make sure to add a space after the xxx
                        if (codeStr.equals("CS")) {
                            codeStr += "  ";
                        } else if (!codeStr.equals("OSNO")) {
                            codeStr += " ";
                        }
                        final String msg = codeStr + " (" + storage.codeLength() + ", " + storage.codeSum() + ") " + storage.toString();
                        System.out.println(msg);
                        if (autoCover) coverWindow.appendStablesInfo(msg);
                        if (autoSmallCover) smallCoverWindow.appendStablesInfo(msg);
                    }
                });
            }
        });

        task.setOnSucceeded(e -> {

            final ObservableList<Storage> storages;
            try {
                storages = task.get();
            } catch (InterruptedException | ExecutionException exception) {
                throw new RuntimeException(exception);
            }

            storages.forEach(storage -> {
                if(!onScreenSequences.containsKey(storage)) {
                    final Color color;
                    final int index = cycle.get();
                    color = comboBoxColors.get(index);
                    addToOnScreenSequences(storage, color);
                }
            });

            Utils.shutdownExecutorAsync(storageExecutor);
            Utils.shutdownExecutorAsync(shotExecutor);

            progress.close();

            // only render the screen after everything has been loaded
            renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);

            final int endHoles = findHoles(area).size();
            if (overrideSS) {
                System.out.printf("Overrided Side Sum maximums: CS - %d, OSO - %d, OSNO - %d%n", max[3], max[4], max[5]);
            }

            // Zhao Yu Li, Aug 6, 2025.
            // Also add to the small cover
            if (autoCover || autoSmallCover) {
                if (autoCover) {
                    coverWindow.show();
                    System.out.printf(
                            "+---- Completed, CODES ARE IN COVER; started with %d holes, filled %d, %d remain ----+%n",
                            startHoles, startHoles - endHoles, endHoles);
                }

                if (autoSmallCover) {
                    smallCoverWindow.show();
                    System.out.printf(
                            "+---- Completed, CODES ARE IN SMALL COVER; started with %d holes, filled %d, %d remain ----+%n",
                            startHoles, startHoles - endHoles, endHoles);
                }

                System.out.println();
            } else {
                System.out.printf(
                        "+-------------- Completed; started with %d holes, filled %d, %d remain --------------+%n",
                        startHoles, startHoles - endHoles, endHoles);
                System.out.println();
            }
            // Increment for superPoly
            step.ifPresent(integerSimpleObjectProperty -> integerSimpleObjectProperty.setValue(integerSimpleObjectProperty.getValue() + 1));
        });

        task.setOnCancelled(e -> {
            task.getPartialProperty().get().forEach(storage -> {
                if(!onScreenSequences.containsKey(storage)) {
                    final Color color;
                    final int index = cycle.get();
                    color = comboBoxColors.get(index);
                    addToOnScreenSequences(storage, color);
                }
            });

            // Wait for orderly cancellation of unfinished tasks 
            Utils.shutdownExecutorAsync(storageExecutor);
            Utils.shutdownExecutorAsync(shotExecutor);

            progress.close();
            // only render the screen after everything has been loaded
            renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);

            final int endHoles = findHoles(area).size();

            if (overrideSS) {
                System.out.printf("Overrided Side Sum maximums: CS - %d, OSO - %d, OSNO - %d%n", max[3], max[4], max[5]);
            }

            // Zhao Yu Li, Aug 6, 2025.
            // Also add to the small cover
            if (autoCover || autoSmallCover) {
                if (autoCover) {
                    coverWindow.show();
                    System.out.printf(
                            "+---- Cancelled, CODES ARE IN COVER; started with %d holes, filled %d, %d remain ----+%n",
                            startHoles, startHoles - endHoles, endHoles);
                }

                if (autoSmallCover) {
                    smallCoverWindow.show();
                    System.out.printf(
                            "+---- Cancelled, CODES ARE IN SMALL COVER; started with %d holes, filled %d, %d remain ----+%n",
                            startHoles, startHoles - endHoles, endHoles);
                }
            } else {
                System.out.printf(
                        "+-------------- Cancelled; started with %d holes, filled %d, %d remain --------------+%n",
                        startHoles, startHoles - endHoles, endHoles);

            }
            // Propagate cancellation for Super
            step.ifPresent(integerSimpleObjectProperty -> integerSimpleObjectProperty.setValue(-1));
        });

        task.setOnFailed(e -> {
            progress.close();
            // Propagate cancellation for Super
            step.ifPresent(integerSimpleObjectProperty -> integerSimpleObjectProperty.setValue(-1));
            throw new RuntimeException(task.getException());
        });

        executor.execute(task);
        progress.show();
    }

    private final class AutoPolyVaryRun {
        private final int[] max;
        private final boolean autoCover;
        private final boolean autoSmallCover;
        private final boolean overrideSS;
        private final ProgressMultiTask progress;
        private final Optional<SimpleObjectProperty<Integer>> step;
        private final ExecutorService drawExecutor;
        private final ExecutorService storageExecutor;
        private final ExecutorService shotExecutor;
        private final AtomicBoolean terminal = new AtomicBoolean(false);

        private AutoPolyVaryRun(final int[] max, final boolean autoCover, final boolean autoSmallCover,
                               final boolean overrideSS, final ProgressMultiTask progress,
                               final Optional<SimpleObjectProperty<Integer>> step,
                               final ExecutorService drawExecutor, final ExecutorService storageExecutor,
                               final ExecutorService shotExecutor) {
            this.max = max;
            this.autoCover = autoCover;
            this.autoSmallCover = autoSmallCover;
            this.overrideSS = overrideSS;
            this.progress = progress;
            this.step = step;
            this.drawExecutor = drawExecutor;
            this.storageExecutor = storageExecutor;
            this.shotExecutor = shotExecutor;
        }

        private boolean isTerminal() {
            return terminal.get();
        }

        private void finish(final boolean cancelled) {
            // Recursive task callbacks can arrive after cancellation. Only the
            // first terminal callback may render, close executors, or advance
            // SuperPolyVary, otherwise one run prints repeated ending banners.
            if (!terminal.compareAndSet(false, true)) {
                return;
            }

            renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, drawExecutor);
            Utils.shutdownExecutorAsync(storageExecutor);
            Utils.shutdownExecutorAsync(shotExecutor);
            if (overrideSS) {
                System.out.printf("Overrided Side Sum maximums: CS - %d, OSO - %d, OSNO - %d%n", max[3], max[4], max[5]);
            }

            if (cancelled) {
                System.out.println("+------------------------------ AutoPolyVary Cancelled ------------------------------+");
                if (autoCover) coverWindow.show();
                if (autoSmallCover) smallCoverWindow.show();
                step.ifPresent(value -> value.setValue(-1));
            } else {
                if (autoCover) {
                    coverWindow.show();
                    System.out.println("+-------------- AutoPolyVary finished successfully, CODES ARE IN COVER --------------+");
                }
                if (autoSmallCover) {
                    smallCoverWindow.show();
                    System.out.println("+-------------- AutoPolyVary finished successfully, CODES ARE IN SMALL COVER --------------+");
                }
                if (!autoCover && !autoSmallCover) {
                    System.out.println("+------------------------ AutoPolyVary finished successfully ------------------------+");
                }
                step.ifPresent(value -> value.setValue(value.getValue() + 1));
            }
            progress.close();
        }

        private void fail() {
            if (!terminal.compareAndSet(false, true)) {
                return;
            }
            Utils.shutdownExecutorAsync(storageExecutor);
            Utils.shutdownExecutorAsync(shotExecutor);
            progress.close();
            step.ifPresent(value -> value.setValue(-1));
        }
    }

    private int drawAutoPolyVary(final int[] max, final int maxSubdivisions, final boolean autoCover, final boolean autoSmallCover, final boolean overrideSS,
                                 final int currIdx, final int endIdx, final int stepIdx, final ConvexPolygon area, final ProgressMultiTask overallProgress,
                                 final Optional<SimpleObjectProperty<Integer>> step, final Optional<Color> colorOpt,
                                 final ExecutorService drawExecutor, final ExecutorService storageExecutor, final ExecutorService shotExecutor) {
        final AutoPolyVaryRun run = new AutoPolyVaryRun(max, autoCover, autoSmallCover, overrideSS, overallProgress,
                step, drawExecutor, storageExecutor, shotExecutor);
        return drawAutoPolyVary(maxSubdivisions, currIdx, endIdx, stepIdx, area, colorOpt, run, new ArrayList<>());
    }

    static boolean hasNextAutoPolyVaryIndex(final int currIdx, final int endIdx, final int stepIdx) {
        if (stepIdx == 0) {
            throw new IllegalArgumentException("AutoPolyVary step must not be zero");
        }
        return stepIdx > 0 ? currIdx + stepIdx <= endIdx : currIdx + stepIdx >= endIdx;
    }

    // Render each OBO coordinate before inspecting its pixels. positionZoom()
    // changes the map immediately, while the full image redraw is asynchronous;
    // sampling before commit pairs the new map with the old image and can turn
    // every remaining coordinate into a zero-work task.
    private int drawAutoPolyVary(final int maxSubdivisions, final int currIdx, final int endIdx,
                                 final int stepIdx, final ConvexPolygon area, final Optional<Color> colorOpt,
                                 final AutoPolyVaryRun run, final ArrayList<Storage> previousCodes) {
        if (run.isTerminal()) {
            return -1;
        }

        lineNumberTxt.setText(Integer.toString(currIdx + 1));
        startAutoPolyVaryCoordinateAfterRender(
                afterCommit -> setOBOAfterRender(currIdx, run.drawExecutor, afterCommit,
                        failure -> {
                            run.fail();
                            throw failure;
                        }),
                () -> continueAutoPolyVaryCoordinate(maxSubdivisions, currIdx, endIdx, stepIdx, area,
                        colorOpt, run, previousCodes));
        return 0;
    }

    static void startAutoPolyVaryCoordinateAfterRender(final Consumer<Runnable> renderRequest,
                                                        final Runnable coordinateWork) {
        renderRequest.accept(coordinateWork);
    }

    private int continueAutoPolyVaryCoordinate(final int maxSubdivisions, final int taskIndex, final int endIdx,
                                               final int stepIdx, final ConvexPolygon area,
                                               final Optional<Color> colorOpt, final AutoPolyVaryRun run,
                                               final ArrayList<Storage> previousCodes) {
        if (run.isTerminal()) {
            return -1;
        }
        if (run.progress.isCancelled()) {
            run.finish(true);
            return 0;
        }

        System.out.println("\n//------------- working on point " + (taskIndex + 1) + " -------------");
        final String[] coords = fileCodeSequences.get(taskIndex).split(" ");
        final double rx = Math.toRadians(Double.parseDouble(coords[0]));
        final double ry = Math.toRadians(Double.parseDouble(coords[1]));
        for (final Storage storage : previousCodes) {
            if (storage.classCodeSeq.stable
                    && ((Storage.Stable) storage).polygon.location(rx, ry) == Location.INSIDE) {
                System.out.println("This coordinate was filled by a code from the previous coordinate.");
                System.out.println(Utils.standard(storage.classCodeSeq, 1));
                advanceAutoPolyVary(maxSubdivisions, taskIndex, endIdx, stepIdx, area, colorOpt, run,
                        previousCodes);
                return 0;
            }
        }

        final double xMin = Math.max(area.projectX().min, map.getViewRectangle().intervalX.min);
        final double xMax = Math.min(area.projectX().max, map.getViewRectangle().intervalX.max);
        final double yMin = Math.max(area.projectY().min, map.getViewRectangle().intervalY.min);
        final double yMax = Math.min(area.projectY().max, map.getViewRectangle().intervalY.max);

        final MutableList<Double> points = new FastList<>();
        final MutableList<Double> pointsFiltered = new FastList<>();
        autoRecurse(xMin, xMax, yMin, yMax, 0, maxSubdivisions, area, points);
        final Image image = regionsImageView.getImage();
        final PixelReader reader = image.getPixelReader();
        // Filter out filled pixels
        for(int i = 0; i < points.size(); i += 2) {
            final int midX = (int) map.pixelX(points.get(i));
            final int midY = (int) map.pixelY(points.get(i+1));
            int color = reader.getArgb(midX, midY);
            if(color == 0) {
                pointsFiltered.add(points.get(i));
                pointsFiltered.add(points.get(i+1));
            }
        }
        System.out.println("//Uncovered vary coordinates: " + (pointsFiltered.size() / 2));
        if (pointsFiltered.isEmpty()) {
            System.out.println("//No uncovered coordinates to vary at this point.");
            advanceAutoPolyVary(maxSubdivisions, taskIndex, endIdx, stepIdx, area, colorOpt, run,
                    new ArrayList<>());
            return 0;
        }
        // We want to filter the codes to avoid recalculating any codes that are already drawn on screen
        final MutableSortedSet<ClassifiedCodeSequence> onScreenCodes = new TreeSortedSet<>();
        onScreenSequences.keySet().forEach(storage -> {onScreenCodes.add(storage.classCodeSeq);});

        if (autoPolyVaryWindow == null) autoPolyVaryWindow = new AutoPolyVaryLoad("AutoPolyVary", "AutoVary", tmpDir + "cover_polygon.txt", tmpDir + "PolyAutoVaryBounds.txt");
        int mode = autoPolyVaryWindow.getMode();
        Integer numGroupToPrint = autoPolyVaryWindow.getNumGroupToPrint();

        if (numGroupToPrint == null) {
            run.finish(true);
            return -1;
        }

        // Create the only PolyVaryTask owned by this run at this coordinate.
        final PolyVaryTask task = new PolyVaryTask(pointsFiltered, onScreenCodes, boyanMenu, Array.ofAll(run.max), pool,
                run.overrideSS, run.storageExecutor, run.shotExecutor, regionsImageView, map, mode, numGroupToPrint);
        final ObservableList<Storage> partials = task.getPartials();
        run.progress.changeTask(task);

        // Zhao Yu Li, Jun 25, 2025.
        // Determines whether to add vary results to the IterateToLimitWindow Cover
        final boolean addToAllPositive = autoPolyVaryWindow.allPositiveIsSelected();  // Add code with all-positive iteration patterns
        final boolean addToPlusMinus = autoPolyVaryWindow.plusMinusIsSelected();  // Add code with plus/minus patterns

        if ((addToAllPositive || addToPlusMinus) && iterateToLimitWindow == null) iterateToLimitWindow = new IterateToLimitWindow(pool);

        // Update screen when change detected
        partials.addListener((ListChangeListener.Change<? extends Storage> c) -> {
            if(run.progress.isCancelled() || run.isTerminal()) return;
            while (c.next()) {
                if(!c.wasAdded()) continue;
                c.getAddedSubList().forEach(storage -> processAutoPolyVaryStorage(storage, colorOpt, mode,
                        addToAllPositive, addToPlusMinus, false, run));
            }
        });

        task.setOnSucceeded(e -> {
            if (run.isTerminal()) return;

            final ObservableList<Storage> storages;
            try {
                storages = task.get();
            } catch (InterruptedException | ExecutionException exception) {
                run.fail();
                throw new RuntimeException(exception);
            }

            storages.forEach(storage -> processAutoPolyVaryStorage(storage, colorOpt, mode,
                    addToAllPositive, addToPlusMinus, false, run));
            advanceAutoPolyVary(maxSubdivisions, taskIndex, endIdx, stepIdx, area, colorOpt, run,
                    new ArrayList<>(storages));
        });

        task.setOnCancelled(e -> {
            partials.forEach(storage -> processAutoPolyVaryStorage(storage, colorOpt, mode,
                    addToAllPositive, addToPlusMinus, true, run));
            run.finish(true);
        });

        task.setOnFailed(e -> {
            run.fail();
            throw new RuntimeException(task.getException());
        });
        try {
            run.drawExecutor.execute(task);
        } catch (final RejectedExecutionException e) {
            run.fail();
            throw e;
        }
        return 0;
    }

    private void advanceAutoPolyVary(final int maxSubdivisions, final int completedIndex, final int endIdx,
                                     final int stepIdx, final ConvexPolygon area, final Optional<Color> colorOpt,
                                     final AutoPolyVaryRun run, final ArrayList<Storage> previousCodes) {
        run.progress.increment(Math.abs(stepIdx));
        if (run.progress.isCancelled()) {
            run.finish(true);
        } else if (hasNextAutoPolyVaryIndex(completedIndex, endIdx, stepIdx)) {
            drawAutoPolyVary(maxSubdivisions, completedIndex + stepIdx, endIdx, stepIdx, area, colorOpt, run,
                    previousCodes);
        } else {
            run.finish(false);
        }
    }

    private void processAutoPolyVaryStorage(final Storage storage, final Optional<Color> colorOpt, final int mode,
                                            final boolean addToAllPositive, final boolean addToPlusMinus,
                                            final boolean forcePrint, final AutoPolyVaryRun run) {
        if (run.isTerminal() || onScreenSequences.containsKey(storage)) {
            return;
        }

        final Color color = colorOpt.isPresent() ? colorOpt.get() : comboBoxColors.get(cycle.get());
        addToOnScreenSequences(storage, color);
        renderRegion(storage, (WritableImage) regionsImageView.getImage(), color);

        final CodeType type = storage.codeType();
        String codePrefix = type.toString();
        if (type.equals(CodeType.CS)) {
            codePrefix += "  ";
        } else if (!type.equals(OSNO)) {
            codePrefix += " ";
        }
        final String message = codePrefix + " (" + storage.codeLength() + ", " + storage.codeSum() + ") " + storage;

        if (mode == 0 || forcePrint) {
            System.out.println(message);
        }
        if (run.autoCover && !coverWindow.containsStableInfo(message)) {
            coverWindow.appendStablesInfo(message);
        }
        if (run.autoSmallCover && !smallCoverWindow.containsStableInfo(message)) {
            smallCoverWindow.appendStablesInfo(message);
        }
        addToIterToLimitCover(storage.toString(), addToAllPositive, addToPlusMinus, iterateToLimitWindow);
    }


    // Calculate 4^max vary locations which are distributed across the entire query area
    public void autoRecurse(final double xMin, final double xMax, final double yMin, final double yMax,
                             final int depth, final int max, final ConvexPolygon area, final MutableList<Double> points) {

        if (depth > max) {
            return;
        }
        if (!findHole((int) map.pixelX(xMin), (int) map.pixelX(xMax), (int) map.pixelY(yMin),
                (int) map.pixelY(yMax), area)
                .isPresent()) {
            return;
        }

        final double rx = (xMax + xMin) / 2;
        final double ry = (yMax + yMin) / 2;

        if (rx < 0 || ry < 0 || rx + ry > Math.PI || rx > ry) {
            return;
        }

        if (area.location(rx, ry).equals(Location.INSIDE)) {
            points.add(rx);
            points.add(ry);
        }

        autoRecurse(xMin, rx, yMin, ry, depth + 1, max, area, points);
        autoRecurse(rx, xMax, yMin, ry, depth + 1, max, area, points);
        autoRecurse(xMin, rx, ry, yMax, depth + 1, max, area, points);
        autoRecurse(rx, xMax, ry, yMax, depth + 1, max, area, points);
    }

    private String drawRegion(ArrayList<Optional<Storage>> storages,
                              ArrayList<ClassifiedCodeSequence> classifiedCodeSequences,
                              boolean draw) {
        final StringBuilder result = new StringBuilder();

        for (int i = 0; i < storages.size(); i++) {
            Optional<Storage> optional = storages.get(i);

            if (i > 0) result.append(", ");

            if (optional.isPresent()) {
                final Storage storage = optional.get();
                result.append(storage.classCodeSeq.toString());
                final String value = calculateChooser.getValue();
                if (draw && (value.equals("Region") || value.equals("All"))) {
                    final int index = cycle.get();
                    final Color color = comboBoxColors.get(index);
                    addToOnScreenSequences(storage, color);
                    renderRegion(storage, (WritableImage) regionsImageView.getImage(), color);
                }
                if (storage.classCodeSeq.stable) {
                    final Storage.Stable stable = (Storage.Stable) storage;
                    if (draw && (value.equals("MRR") || value.equals("All"))) {
                        final ConvexPolygon innerPoly = stable.polygon;
                        innerPolyBounds.add(innerPoly);
                        renderPolygon(innerPoly, (WritableImage) boundsImageView.getImage(), polyBoundColor);
                    }
                    if (draw && (value.equals("Bound") || value.equals("All"))) {
                        // Change outerPoly to the stable.outerPolygon when that exists
                        final ConvexPolygon outerPoly = ConvexPolygon.create(Database.parsePoints(Wrapper.boundingPolygon(stable.classCodeSeq, pool)));
                        outerPolyBounds.add(outerPoly);
                        renderPolygon(outerPoly, (WritableImage) boundsImageView.getImage(), polyBoundColor);
                    }
                }
            } else {
                // Zhao Yu Li, May 02, 2025.
                // Add '//' before 'empty set' so the cover can ignore it
                result.append("// empty set ").append(classifiedCodeSequences.get(i));
            }
        }

        String finalResult = result.toString();
        if (finalResult.contains("empty set") && !finalResult.startsWith("//")) finalResult = "// " + finalResult;

        return finalResult;
    }

    /**
     * Zhao Yu Li, May 28, 2025.
     * Checks for intersection between currentCodeNumbers and a specified polygon. Adds the single or triple, along with
     * the iteration pattern used, to the cover if the single or triple intersects with the polygon.
     * @param pattern The iteration pattern used to produce currentCodeNumbers.
     */
    private void handleIterationIntersect(String pattern) {
        Optional<ConvexPolygon> polygon = iterationPolyWindow.getPolygon();

        if (intersectCheckBox.isSelected() && !polygon.isPresent()) return;

        int numOfCodes = 0;
        int numOfIntersects = 0;

        final StringBuilder codeSeqString = new StringBuilder();
        ArrayList<Optional<Storage>> storages = new ArrayList<>();
        ArrayList<ClassifiedCodeSequence> classifiedCodeSequences = new ArrayList<>();

        Storage firstStorage = null;  // Storage of the first code sequence. Used to get a pretty formatted stable code
        boolean isTriple = false;

        for (int i = 0; i < 3; i++) {
            if (currentCodeNumbers[i].isEmpty()) continue;

            numOfCodes++;
            Either<InvalidCodeSequence, ClassifiedCodeSequence> classCodeSeq = ClassifiedCodeSequence.create(currentCodeNumbers[i]);

            if (classCodeSeq.isLeft()) return;

            if (i > 0) codeSeqString.append(",");

            codeSeqString.append(classCodeSeq.get().toString());
            classifiedCodeSequences.add(classCodeSeq.get());

            Optional<Storage> storage = Database.loadStorage(classCodeSeq.get(), pool);

            storages.add(storage);

            if (storage.isPresent()) {
                if (i == 0) firstStorage = storage.get();
                if (intersectCheckBox.isSelected() && storage.get().intersects(polygon.get())) numOfIntersects++;

                if (i == 0)
                    isTriple = storage.get().classCodeSeq.stable;
                else if (i == 1)
                    isTriple = isTriple && !storage.get().classCodeSeq.stable;
                else
                    isTriple = isTriple && storage.get().classCodeSeq.stable;
            }
        }

        boolean draw = !intersectCheckBox.isSelected() || numOfCodes == numOfIntersects;
        boolean addToCover = intersectCheckBox.isSelected() && numOfCodes == numOfIntersects;

        final String result = drawRegion(storages, classifiedCodeSequences, draw);
        System.out.println(result);

        if (addToCover && numOfIntersects == 1 && isTriple)
            coverWindow.appendStablesInfo(getCoverCodeString(firstStorage) + "  // " + pattern);

        if (addToCover && numOfIntersects == 3 && isTriple)
            coverWindow.appendTriplesInfo(codeSeqString + "  // " + pattern);
    }

    /**
     * Zhao Yu Li, May 23, 2025.
     * Convert the current code numbers into a string as is (without putting it in normalized form). And also computes
     * the odd-even pattern of the current code numbers (as-is). Returns the tuple consisting of the current code numbers
     * as a string, and its odd-even pattern as a string.
     */
    private Tuple2<String, String> getCodeSeqAndOEString() {
        final StringBuilder codeNumbersString = new StringBuilder();
        final StringBuilder OEString = new StringBuilder();

        for (int i = 0; i < currentCodeNumbers.length; i++) {
            if (currentCodeNumbers[i].isEmpty()) continue;
            if (i > 0) {
                codeNumbersString.append(", ");
                OEString.append(",");
            }

            for (int j = 0; j < currentCodeNumbers[i].size(); j++) {
                if (j > 0) {
                    codeNumbersString.append(" ");
                }

                codeNumbersString.append(currentCodeNumbers[i].get(j));
                OEString.append(currentCodeNumbers[i].get(j) % 2 == 0 ? "E" : "O");
            }
        }

        return Tuple.of(codeNumbersString.toString(), OEString.toString());
    }

    /**
     * Zhao Yu Li
     * Adds and subtracts two from the code sequence at the same time. A positive index means we add two, whereas a
     * negative index means we subtract two
     * Zhao Yu Li, May 22, 2025.
     * Updated to add a code sequence - iteration pattern pair to the garbage database.
     * Updated, May 23, 2025.
     * Store the code sequence as-is instead of first normalizing it, because the iteration pattern may only apply to
     * the current form rather than the normalized form.
     * TODO: code sequence - iteration pattern pairs where the code sequence and/or the iteration pattern are invalid are also added to the database.
     */
    private void addSubtract(final TextField textField, final ConnectionPool pool) {
        // the indices to increment
        final String[] pats = textField.getText().split(",");
        final StringBuilder normalizedPattern = new StringBuilder();

        // The code sequence - iteration pattern pair should be stored with the code sequence that the iteration pattern
        // was applied to, and not the ones after modification.
        final Tuple2<String, String> codeSeqAndOEString = getCodeSeqAndOEString();
        final String originalCodeNumbers = codeSeqAndOEString._1;

        for (int i = 0; i < pats.length; i++) {
            final int j = i;
            final ImmutableIntList numbers = Utils.splitString(pats[j].trim()).get();
            numbers.forEach(number -> {
                final int value = number < 0 ? -2 : 2;
                number = number < 0 ? -number : number;
                final int currentNumber = currentCodeNumbers[j].get(number - 1);
                currentCodeNumbers[j].set(number - 1, currentNumber + value);
            });
            normalizedPattern.append(pats[i].trim()).append(",");
        }

        normalizedPattern.deleteCharAt(normalizedPattern.length() - 1);
        Database.saveIterationPatternToDatabase(originalCodeNumbers, codeSeqAndOEString._2, normalizedPattern.toString(), "garbage");

        handleIterationIntersect(textField.getText());
        synchronize();
    }

    /**
     * Zhao Yu Li
     * The reverse operation of addSubtract
     * Adds and subtracts two from the code sequence at the same time. A negative index means we add two, whereas a
     * positive index means we subtract two
     * Zhao Yu Li, May 22, 2025.
     * Updated to add a code sequence - iteration pattern pair to the garbage database.
     * Updated, May 23, 2025.
     * Store the code sequence as-is instead of first normalizing it, because the iteration pattern may only apply to
     * the current form rather than the normalized form.
     * TODO: code sequence - iteration pattern pairs where the code sequence and/or the iteration pattern are invalid are also added to the database.
     */
    private void addSubtractReverse(final TextField textField, final ConnectionPool pool) {
        // the indices to increment
        final String[] pats = textField.getText().split(",");
        final StringBuilder normalizedPattern = new StringBuilder();

        // The code sequence - iteration pattern pair should be stored with the code sequence that the iteration pattern
        // was applied to, and not the ones after modification.
        final Tuple2<String, String> codeSeqAndOEString = getCodeSeqAndOEString();
        final String originalCodeNumbers = codeSeqAndOEString._1;

        for (int i = 0; i < pats.length; i++) {
            final int j = i;
            final ImmutableIntList numbers = Utils.splitString(pats[j].trim()).get();
            numbers.forEach(number -> {
                final int value = number < 0 ? -2 : 2;
                number = number < 0 ? -number : number;
                final int currentNumber = currentCodeNumbers[j].get(number - 1);
                currentCodeNumbers[j].set(number - 1, currentNumber - value);
            });
            normalizedPattern.append(pats[i].trim()).append(",");
        }

        normalizedPattern.deleteCharAt(normalizedPattern.length() - 1);
        Database.saveIterationPatternToDatabase(originalCodeNumbers, codeSeqAndOEString._2, normalizedPattern.toString(), "garbage");

        handleIterationIntersect(textField.getText());
        synchronize();
    }

    // Zhao Yu Li, May 22, 2025.
    // Updated to add a code sequence - iteration pattern pair to the garbage database.
    // Updated, May 23, 2025.
    // Store the code sequence as-is instead of first normalizing it, because the iteration pattern may only apply to
    // the current form rather than the normalized form.
    // TODO: code sequence - iteration pattern pairs where the code sequence and/or the iteration pattern are invalid are also added to the database.
    private void increase(final TextField textField, final ConnectionPool pool) {
        // the indices to increment
        final String[] pats = textField.getText().split(",");
        final StringBuilder normalizedPattern = new StringBuilder();

        // The code sequence - iteration pattern pair should be stored with the code sequence that the iteration pattern
        // was applied to, and not the ones after modification.
        final Tuple2<String, String> codeSeqAndOEString = getCodeSeqAndOEString();
        final String originalCodeNumbers = codeSeqAndOEString._1;

        for (int i = 0; i < pats.length; i++) {
            final int j = i;
            final ImmutableIntList numbers = Utils.splitString(pats[j].trim()).get();
            numbers.forEach(number -> {
                final int currentNumber = currentCodeNumbers[j].get(number - 1);
                currentCodeNumbers[j].set(number - 1, currentNumber + 2);
            });
            normalizedPattern.append(pats[i].trim()).append(",");
        }

        normalizedPattern.deleteCharAt(normalizedPattern.length() - 1);
        Database.saveIterationPatternToDatabase(originalCodeNumbers, codeSeqAndOEString._2, normalizedPattern.toString(), "garbage");

        handleIterationIntersect(textField.getText());
        synchronize();
    }

    // Zhao Yu Li, May 22, 2025.
    // Updated to add a code sequence - iteration pattern pair to the garbage database.
    // Updated, May 23, 2025.
    // Store the code sequence as-is instead of first normalizing it, because the iteration pattern may only apply to
    // the current form rather than the normalized form.
    // TODO: code sequence - iteration pattern pairs where the code sequence and/or the iteration pattern are invalid are also added to the database.
    private void decrease(final TextField textField, final ConnectionPool pool) {
        // the indices to increment
        final String[] pats = textField.getText().split(",");
        final StringBuilder normalizedPattern = new StringBuilder();

        // The code sequence - iteration pattern pair should be stored with the code sequence that the iteration pattern
        // was applied to, and not the ones after modification.
        final Tuple2<String, String> codeSeqAndOEString = getCodeSeqAndOEString();
        final String originalCodeNumbers = codeSeqAndOEString._1;

        for (int i = 0; i < pats.length; i++) {
            final int j = i;
            final ImmutableIntList numbers = Utils.splitString(pats[j].trim()).get();
            numbers.forEach(number -> {
                final int currentNumber = currentCodeNumbers[j].get(number - 1);
                currentCodeNumbers[j].set(number - 1, currentNumber - 2);
            });
            normalizedPattern.append(pats[i].trim()).append(",");
        }
        normalizedPattern.deleteCharAt(normalizedPattern.length() - 1);
        Database.saveIterationPatternToDatabase(originalCodeNumbers, codeSeqAndOEString._2, normalizedPattern.toString(), "garbage");

        handleIterationIntersect(textField.getText());
        synchronize();
    }

    private void setOBO(
            final int index, final ConnectionPool pool, final ExecutorService executor) {

        final String line = fileCodeSequences.get(index);
        if (line.contains(".")) {
            final String[] coords = line.split(" ");
            final double x = Math.toRadians(Double.parseDouble(coords[0]));
            final double y = Math.toRadians(Double.parseDouble(coords[1]));
            zoom(x, x, y, y, executor);
        }

        else {
            final ClassifiedCodeSequence codeSeq;
            final Optional<ImmutableIntList> optionalCode = Utils.splitString(line);
            if (optionalCode.isPresent()) {
                final ImmutableIntList codeNumbers = optionalCode.get();
                codeSeq = ClassifiedCodeSequence.create(codeNumbers).get();

                final Optional<Storage> optional = Database.loadStorage(codeSeq, pool);

                if (optional.isPresent()) {
                    currentOBOStorage = optional.get();
                    System.out.println(currentOBOStorage);

                    final WritableImage oboImage = new WritableImage(SIDE, SIDE);
                    renderRegion(currentOBOStorage, oboImage, currentOBOColor);
                    oboImageView.setImage(oboImage);

                } else {
                    System.out.println("//empty set " + codeSeq);
                    // set it to an empty image
                    currentOBOStorage = null;
                    final WritableImage oboImage = new WritableImage(SIDE, SIDE);
                    oboImageView.setImage(oboImage);
                }
            }
        }

    }

    private void setOBOAfterRender(final int index, final ExecutorService executor,
                                   final Runnable afterCommit,
                                   final Consumer<RuntimeException> onFailure) {
        final String[] coords = fileCodeSequences.get(index).split(" ");
        final double x = Math.toRadians(Double.parseDouble(coords[0]));
        final double y = Math.toRadians(Double.parseDouble(coords[1]));
        zoomAfterRender(x, x, y, y, executor, afterCommit, onFailure);
    }

    // The pattern contains a list of the 1-based indices to add by the increment
    private static ImmutableIntList createVector(
            final IntList pattern, final int increment, final int length) {
        final MutableIntList vector = IntArrayList.newWithNValues(length, 0);
        pattern.forEach(index -> {
            // Zhao Yu Li May 01, 2025.
            // If a negative index is given, its aboslute value becomes the real index, and increment is subtracted from
            // the code value at index
            final int idx = index < 0 ? -index : index;
            final int value = index < 0 ? -increment : increment;
            final int sum = Math.addExact(vector.get(idx - 1), value);
            vector.set(idx - 1, sum);

            //george aug 26,2019
            //System.out.print("sum " + sum + " ");
            //System.out.print("increment " + increment + " ");
            //sum 2 increment 2 sum 2 increment 2   {2.57611,2.57611}
        });

        return vector.toImmutable();
    }

    // destination += scale * vector
    // same length, obviously
    private static void addMultiple(
            final MutableIntList destination, final int scale, final IntList vector) {
        for (int i = 0; i < destination.size(); ++i) {
            final int mul = Math.multiplyExact(scale, vector.get(i));
            final int sum = Math.addExact(destination.get(i), mul);
            destination.set(i, sum);
        }
    }

    /*insert repeated elements for expando
    private static void insertRepeatedElement(final MutableIntList baseList, final int element, final int repeat,final int position) {
       for (int i = 0; i < baseList.size(); ++i){
           if (i==position){
               baseList.addAtIndex(position, element);
           }
       }
    }
    */

    public void drawSearch(final List<Storage> list) {
        list.forEach(storage -> {
            final int index = cycle.get();
            final Color color = comboBoxColors.get(index);
            addToOnScreenSequences(storage, color);
        });

        renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executorService);
    }

    public double getOffset() {
        final String txt = offsetTextField.getText().trim();

        if (txt.isEmpty()) {
            return 0;
        }

        final double num = Double.parseDouble(txt);

        return num;
    }

    public void loadCover(final String dir, final ExecutorService executor) {
        loadCover(dir, executor, false);
    }

    public void loadCover(final String dir, final ExecutorService executor, final boolean small) {

        final String polygonString = readFromFile(dir + "/polygon.txt").trim();
        final String squareString = readFromFile(dir + "/square.txt").trim();
        final String stablesString = readFromFile(dir + "/stables.txt").trim();
        final String triplesString = readFromFile(dir + "/triples.txt").trim();
        final String coverString = readFromFile(dir + "/cover.txt").trim();

        final ConvexPolygon polygon = CoverStuff.parsePolygon(polygonString);
        final Rectangle square = CoverStuff.parseRectangle(squareString);
        final List<ClassifiedCodeSequence> stables = CoverStuff.parseStables(stablesString);
        final List<Triple> triples = CoverStuff.parseTriples(triplesString);

        final Tuple2<MutableMap<Rectangle, ClassifiedCodeSequence>, MutableMap<Rectangle, Triple>> cover = CoverStuff.parseCover(coverString, square, stables, triples);

        mrrBounds.clear();
        coverRects.addStables(cover._1, Color.BLACK);
        coverRects.addTriples(cover._2, Color.BLACK);
        if (!small) coverArea = Optional.of(polygon);
        renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
    }

    public void loadCoverWithoutTrim(final String dir, final ExecutorService executor) {

        final String polygonString = readFromFile(dir + "/polygon.txt").trim();
        final String squareString = readFromFile(dir + "/square.txt").trim();
        final String stablesString = readFromFile(dir + "/stables.txt").trim();
        final String triplesString = readFromFile(dir + "/triples.txt").trim();
        final String coverString = readFromFile(dir + "/cover.txt").trim();

        final ConvexPolygon polygon = CoverStuff.parsePolygon(polygonString);
        final Rectangle square = CoverStuff.parseRectangle(squareString);
        final List<ClassifiedCodeSequence> stables = CoverStuff.parseStables(stablesString);
        final List<Triple> triples = CoverStuff.parseTriples(triplesString);

        final Tuple2<MutableMap<Rectangle, ClassifiedCodeSequence>, MutableMap<Rectangle, Triple>> cover = CoverStuff.parseCover(coverString, square, stables, triples);

        mrrBounds.clear();
        coverRects.addStables(cover._1, Color.BLACK);
        coverRects.addTriples(cover._2, Color.BLACK);
        coverArea = Optional.of(polygon);
        renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
    }

    // Zhao Yu Li, May 14, 2025.
    // Vary tasks that start one after another. This is used for the tetrahedron calculation. We could have launched
    // the three vary tasks in a for loop, but that may enter a race condition, as we can't calculate the intersection
    // until all Vary tasks finish. The recursion method makes sure that the next one starts only after the previous
    // one successfully executes to completion.
    public void queuedVaryTask(final List<Tuple2<Double, Double>> originalPoints,
                               final List<Tuple2<Double, Double>> points,
                               final int index,
                               final int max,
                               ExecutorService executor,
                               final int step,
                               final int numToPrint,
                               final boolean draw,
                               final boolean addToCover)
    {
        int next = index + 1;

        String mode = "";
        if (step == 2) mode = "Bar";
        if (step == 3) mode = "Tetrahedron";

        if (next > max) {
            executor.shutdown();
            System.out.println("// Finished " + mode + ".");
            return;
        }

        if (index == 0) {
            System.out.println("// Start " + mode + ".");
            if (addToCover) coverWindow.appendStablesInfo("// Start " + mode + ".");
        }

        final Task<MutableSortedSet<ClassifiedCodeSequence>> varyTask
                = new Task<MutableSortedSet<ClassifiedCodeSequence>>() {

            @Override
            protected MutableSortedSet<ClassifiedCodeSequence> call() {
                return boyanMenu.varyTriangles(points.get(index)._1, points.get(index)._2,
                        Double.parseDouble(boyanMenu.varyX2Text.getText()),
                        Double.parseDouble(boyanMenu.varyY2Text.getText()),
                        Double.parseDouble(boyanMenu.varyX3Text.getText()),
                        Double.parseDouble(boyanMenu.varyY3Text.getText()),
                        Double.parseDouble(boyanMenu.line1CutText.getText()),
                        Double.parseDouble(boyanMenu.line2CutText.getText()),
                        3, executor);
            }
        };

        //final Progress progress = new Progress(varyTask);
        final ProgressWithStatus progress = new ProgressWithStatus(varyTask, "Calling findCodes3 (no status)", 0);
        final Thread varyThread = new Thread(varyTask);

        String finalMode = mode;
        varyTask.setOnSucceeded(success -> {
            try {
                MutableSortedSet<ClassifiedCodeSequence> allCodes = varyTask.get();
                allCodes.forEach(seq -> Database.saveToDatabase(seq, "garbage"));
                tetrahedronCodes.add(allCodes);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            progress.close();

            // Zhao Yu Li, May 16, 2025.
            // Updated to handle tetrahedron points from a list of coordinates instead of just a single one.
            // For each tetrahedron, we need to perform Vary3 on three of its points. Therefore, every three points we
            // process, we need to compute the intersections of the results from the Vary tasks.
            if (next % step == 0) {
                assert step == tetrahedronCodes.size();

                System.out.println("// " + finalMode +  " results for (" + originalPoints.get(index / step)._1 + ", " + originalPoints.get(index / step)._2 + ")");

                ArrayList<Collection<ClassifiedCodeSequence>> cList = new ArrayList<>();

                cList.add(tetrahedronCodes.get(0));

                for (int i = 1; i < step; i++) {
                    cList.add(tetrahedronCodes.get(i));
                    ArrayList<ClassifiedCodeSequence> matching = Utils.getIntersectionCodes(cList);

                    cList.clear();
                    cList.add(matching);
                }

                if (cList.get(0).isEmpty()) {
                    System.out.println("// None matching...");
                } else {
                    System.out.println("// Found " + cList.get(0).size() + " matching codes.");

                    final int finalNumToPrint = numToPrint == 0 ? cList.get(0).size() : numToPrint;

                    // Zhao Yu Li, Jun 3, 2025.
                    // Lifted the print mid and load storage functionalities into their own utility files.
                    ArrayList<ClassifiedCodeSequence> codesPrinted = PrintMid.printMid(cList.get(0), finalNumToPrint);

                    // Zhao Yu Li, Jun 3, 2025.
                    // Add codes to cover.
                    if (addToCover) {
                        for (ClassifiedCodeSequence code : codesPrinted) {
                            if (ClassifiedCodeSequence.isStableCodeType(code.codeType)) {
                                coverWindow.appendStablesInfo(getCoverCodeString(code));
                            }
                        }
                    }

                    if (draw) {
                        final int colorIndex = cycle.get();
                        Color color = comboBoxColors.get(colorIndex);

                        ArrayList<Storage> storages = BatchLoadStorage.batchLoadStorage(codesPrinted, pool);

                        for (Storage storage : storages) {
                            addToOnScreenSequences(storage, color);
                        }
                    }
                }

                tetrahedronCodes.clear();

                if (draw) {
                    renderRegions(onScreenSequences, guideLinesImageView, regionsImageView, executor);
                }
            }

            queuedVaryTask(originalPoints, points, next, max, executor, step, numToPrint, draw, addToCover);
        });

        varyTask.setOnCancelled(cancelled -> {
            System.out.println("// " + finalMode + " Cancelled");
            varyThread.interrupt();
            executor.shutdownNow();
            progress.close();
        });
        varyTask.setOnFailed(fail -> {
            System.out.println("// " + finalMode + " failed: " + fail);
            executor.shutdown();
            progress.close();
        });

        varyThread.start();

        progress.show();
    }

    public void drawAndAddToCover(boolean draw, boolean addToCover, ArrayList<Storage> storages) {
        Color color = comboBoxColors.get(cycle.get());

        if (draw) {
            for (Storage storage : storages) {
                addToOnScreenSequences(storage, color);
            }
        }

        if (addToCover) {
            if (storages.size() == 1) {
                Storage storage = storages.get(0);

                if (storage.classCodeSeq.stable) coverWindow.appendStablesInfo(Utils.getCoverCodeString(storage));
            } else if (storages.size() == 3) {
                String tripleString = storages.get(0) + "," + storages.get(1) + "," + storages.get(2);
                coverWindow.appendTriplesInfo(tripleString);
            }
        }
    }

    /**
     * Finds an all-positive and a plus/minus iteration pattern for <code>classCodeSeq</code> and add the pairs to the
     * appropriate text areas of the <code>iterateToLimitWindow</code>.
     * @param classCodeSeq The <code>ClassifiedCodeSequence</code> to add to the <code>iterateToLimitWindow</code> Cover.
     * @param addToAllPositive If true, <code>classCodeSeq</code> and its iteration pattern will be added to the appropriate all-positive text area of <code>iterateToLimitWindow</code>.
     * @param addToPlusMinus If true, <code>classCodeSeq</code> and its iteration pattern will be added to the appropriate plus/minus text area of <code>iterateToLimitWindow</code>.
     * @param iterateToLimitWindow The <code>IterateToLimitWindow</code> to add <code>classCodeSeq</code> and its iteration pattern to.
     */
    public static void addToIterToLimitCover(String classCodeSeq, boolean addToAllPositive, boolean addToPlusMinus, IterateToLimitWindow iterateToLimitWindow) {
        if (addToAllPositive) {
            String iterationPattern = IterateToLimitWindow.getIterationPattern(classCodeSeq, true);

            if (iterationPattern.isEmpty()) {
                System.out.println("Skip adding "
                        + classCodeSeq
                        + " to the IterateToLimitWindow Cover (all-positive) because we cannot find a valid iteration pattern");
            } else iterateToLimitWindow.addToContent(classCodeSeq, iterationPattern, true);
        }

        if (addToPlusMinus) {
            String iterationPattern = IterateToLimitWindow.getIterationPattern(classCodeSeq, false);

            if (iterationPattern.isEmpty()) {
                System.out.println("Skip adding "
                        + classCodeSeq
                        + " to the IterateToLimitWindow Cover (+/-) because we cannot find a valid iteration pattern");
            } else iterateToLimitWindow.addToContent(classCodeSeq, iterationPattern, false);
        }
    }

    private Tuple3<Integer, Integer, Integer> getStartStepEnd() {
        return getStartStepEnd(fileCodeSequences.size());
    }

    public Tuple3<Integer, Integer, Integer> getStartStepEnd(int defaultEnd) {
        final String lineStartText = lineStartField.getText();
        final String lineStepText = lineStepField.getText();
        final String lineEndText = lineEndField.getText();
        // The indexes that the user sees (will be converted later)
        int startIdxUser;
        int stepIdxUser;
        int endIdxUser;
        // 2024-05-06 Fixed broken logic (Not all fields filled was not detected properly)
        if (lineStartText.isEmpty() && lineEndText.isEmpty() && lineStepText.isEmpty()) { // All fields empty
            startIdxUser = 1;
            stepIdxUser = 1;
            endIdxUser = defaultEnd;
        } else if (lineStartText.isEmpty() || lineEndText.isEmpty() || lineStepText.isEmpty()) { // At least 1, but not all fields are empty
            showEnterLineNumberErrorAutoVary();
            return Tuple.of(null, null, null);
        } else { // All fields filled
            try {
                startIdxUser = Integer.parseInt(lineStartText);
            } catch (final NumberFormatException e) {
                showInvalidNumberError(lineStartText);
                return Tuple.of(null, null, null);
            }
            try {
                endIdxUser = Integer.parseInt(lineEndText);
            } catch (final NumberFormatException e) {
                showInvalidNumberError(lineEndText);
                return Tuple.of(null, null, null);
            }
            try {
                stepIdxUser = Math.min(defaultEnd, Integer.parseInt(lineStepText)); // Max step is all elements
            } catch (final NumberFormatException e) {
                showInvalidNumberError(lineStepText);
                return Tuple.of(null, null, null);
            }
            if (!(1 <= startIdxUser && startIdxUser <= endIdxUser && endIdxUser <= defaultEnd)) {
                showInvalidLineRangeError(defaultEnd);
                return Tuple.of(null, null, null);
            }
            if(stepIdxUser < 1) {
                showStepErrorAutoVary();
                return Tuple.of(null, null, null);
            }
        }

        return Tuple.of(startIdxUser, stepIdxUser, endIdxUser);
    }

    public void moveScreen(String xString, String yString) {
        double x;
        try {
            x = Double.parseDouble(xString);
        } catch (NumberFormatException e) {
            throw new NumberFormatException();
        }

        double y;
        try {
            y = Double.parseDouble(yString);
        } catch (NumberFormatException e) {
            throw new NumberFormatException();
        }

        moveScreen(x, y);
    }

    public void moveScreen(double x, double y) {
        final double xRad = Math.toRadians(x);
        final double yRad = Math.toRadians(y);
        zoom(xRad, xRad, yRad, yRad, executorService);
    }
}
