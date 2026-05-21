package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.CodeSequence;
import billiards.codeseq.CodeType;
import billiards.geometry.Vector2;
import billiards.vary.Vary3;
import billiards.vary.Vary4;
import billiards.vary.VaryCS;
import billiards.database.Database;

import org.apache.commons.math3.util.FastMath;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.sorted.mutable.TreeSortedSet;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class BoyanMenu {

    // when using autovary3, if you want it to print all codes it finds, switch false to true in
    // the next line:
    private static final boolean printAll = false;

    // setting this to true makes it so that when you Vary3, it prints out the codes organized by
    // their type:
    private static final boolean splitUp = false;

    final VBox wrapper = new VBox();

    final Button middleVaryButton = new Button();
    final Button vary3Btn = new Button();
    final Button vary3BBtn = new Button();
    
    final Button vary4Btn = new Button();
    final Button autoVaryBtn = new Button();

    final Label sideSpacingLbl = new Label();
    final Label movesLbl = new Label();

    final TextField maxMovesText = new TextField();
    final TextField minMovesText = new TextField();
    final TextField shotsText = new TextField();

    final public TextField varyX1Text = new TextField();
    final TextField varyX2Text = new TextField();
    final TextField varyX3Text = new TextField();
    final public TextField varyY1Text = new TextField();
    final TextField varyY2Text = new TextField();
    final TextField varyY3Text = new TextField();
    final TextField line1CutText = new TextField();
    final TextField line2CutText = new TextField();

    final TextField autoCycleText = new TextField();
    final TextField cycleStepText = new TextField();
    final TextField autoIterText = new TextField();
    final TextField autoStepText = new TextField();

    final CheckBox allPixCheckBox = new CheckBox();

    final RadioButton line1RDB = new RadioButton();
    final RadioButton line2RDB = new RadioButton();
    final RadioButton line3RDB = new RadioButton();
    final ToggleGroup boyanGroup = new ToggleGroup();

    final CheckBox OSOcb = new CheckBox();
    final CheckBox OSNOcb = new CheckBox();
    final CheckBox ONScb = new CheckBox();
    final CheckBox CScb = new CheckBox();
    final CheckBox CNScb = new CheckBox();
    final CheckBox Triplescb = new CheckBox();
    
    //george june12,2019 start
    final CheckBox OSO2cb = new CheckBox();
    final CheckBox OSNO2cb = new CheckBox();
    final CheckBox ONS2cb = new CheckBox();
    final CheckBox CS2cb = new CheckBox();
    final CheckBox CNS2cb = new CheckBox();
  //george june12,2019 end

    final CheckBox varyOnePoint = new CheckBox();

    final Label hitsLabel = new Label();

    final CheckBox buildPolyCheckBox = new CheckBox();

    final TextField maxPrinting = new TextField();

	public BoyanMenu(final Button cycleVaryButton, final Button middleVaryLButton, final Button polyAutoBtn, final Button varyLBtn,
                     final Button autoPolyVaryBtn, final TextField lineStartField, final TextField lineStepField, final TextField lineEndField,
                     final Button superPolyVaryBtn, final CheckBox superAutoCb, final double TipOpenDelay, final double TipCloseDelay) {

        Utils.setupCustomTooltipBehavior((int) (TipOpenDelay * 1000), (int) (TipCloseDelay * 1000), 200);

        sideSpacingLbl.setText("Shots");

        movesLbl.setText("SideSum");
        
        maxMovesText.setPrefWidth(55);
        maxMovesText.setTooltip(Utils.toolTip("The maximum side sum to search for"));
        maxMovesText.setText("200");
        minMovesText.setPrefWidth(35);
        minMovesText.setTooltip(Utils.toolTip("The minimum side sum to search for"));
        minMovesText.setText("0");
        shotsText.setPrefWidth(35);//george set 40 instead of 60
        shotsText.setText("4");//george set 4 instead of 8
        shotsText.setTooltip(Utils.toolTip("How many places along the base it should shoot"));


        varyX1Text.setPrefWidth(155);
        varyX1Text.setText("30");
        varyX1Text.setTooltip(Utils.toolTip("The first angle in your triangle"));
        varyY1Text.setPrefWidth(155);
        varyY1Text.setText("40");
        varyY1Text.setTooltip(Utils.toolTip("The second angle in your triangle"));
        //varyX2Text.setPrefWidth(155);
        //varyY2Text.setPrefWidth(155);
        //varyX2Text.setText("0");
        //varyY2Text.setText("0");


        final TextField[] fields = {varyX2Text, varyY2Text, varyX3Text, varyY3Text};
        for (final TextField field : fields) {
            field.setPrefWidth(155);
            field.setText("0");
        }

        line1CutText.setPrefWidth(40);
        line1CutText.setText("0");
        line1CutText.setStyle(Utils.hex(Color.LIGHTGREEN));
        line2CutText.setPrefWidth(40);
        line2CutText.setText("0");
        line2CutText.setStyle(Utils.hex(Color.LIGHTGREEN));
        line1RDB.setToggleGroup(boyanGroup);
        line1RDB.setSelected(true);
        line2RDB.setToggleGroup(boyanGroup);
        line3RDB.setToggleGroup(boyanGroup);

        hitsLabel.setText("" + 1);

        OSOcb.setText("OSO");
        OSOcb.setSelected(true);
        OSOcb.setTooltip(Utils.toolTip(
            "The Vary3 functions will look for codes only of chosen types"));
        OSNOcb.setText("OSNO");
        OSNOcb.setSelected(true);
        OSNOcb.setTooltip(Utils.toolTip(
            "The Vary3 and Load Directory functions will look for codes only of chosen types"));
        CScb.setText("CS");
        CScb.setSelected(true);
        CScb.setTooltip(Utils.toolTip(
            "The Vary3 and Load Directory functions will look for codes only of chosen types"));
        CNScb.setText("CNS");
        CNScb.setTooltip(Utils.toolTip(
            "The Vary3 and Load Directory functions will look for codes only of chosen types"));
        ONScb.setText("ONS");
        ONScb.setTooltip(Utils.toolTip(
            "The Vary3 and Load Directory functions will look for codes only of chosen types"));
        Triplescb.setText("Triples");
        Triplescb.setTooltip(Utils.toolTip(
                "The Load Directory function will look for codes only of chosen types"));
      //george june12,2019 start
        OSO2cb.setText("OSO2");
        OSO2cb.setSelected(true);
        OSO2cb.setTooltip(Utils.toolTip(
            "The Vary3 functions will look for codes only of chosen types"));
        OSNO2cb.setText("OSNO2");
        OSNO2cb.setSelected(true);
        OSNO2cb.setTooltip(Utils.toolTip(
            "The Vary3 functions will look for codes only of chosen types"));
        CS2cb.setText("CS2");
        CS2cb.setSelected(true);
        CS2cb.setTooltip(Utils.toolTip(
            "The Vary3 functions will look for codes only of chosen types"));
        CNS2cb.setText("CNS2");
        CNS2cb.setTooltip(Utils.toolTip(
            "The Vary3 functions will look for codes only of chosen types"));
        ONS2cb.setText("ONS2");
        ONS2cb.setTooltip(Utils.toolTip(
            "The Vary3 functions will look for codes only of chosen types"));
      //george june12,2019 end
        varyOnePoint.setText("Vary1Pt");
        varyOnePoint.setTooltip(Utils.toolTip("When using varyL, if this is "
        		+ "selected, it will shoot at that point and draw/write to the "
        		+ "vary one point file the top X specified codes."));

        autoCycleText.setText("3");
        autoCycleText.setTooltip(Utils.toolTip("For PolyVary, the number of subdivisions done"));
        autoCycleText.setPrefWidth(40);
        
        cycleStepText.setText("1");
        cycleStepText.setTooltip(Utils.toolTip("For SuperPolyVary, the change in subdivisions each iteration"));
        cycleStepText.setPrefWidth(40);

        autoIterText.setText("0");
        autoIterText.setPrefWidth(40);
        autoIterText.setTooltip(Utils.toolTip("For autoVary3, the number of side sum iterations done"));
        autoStepText.setText("50");
        autoStepText.setPrefWidth(60);
        autoStepText.setTooltip(Utils.toolTip("For autoVary3, the step when doing side sum iterations"));

        allPixCheckBox.setText("All Pixels");
        allPixCheckBox.setTooltip(Utils.toolTip("For autoVary3, if this is on, no subdivisions are done."
                                                + " Instead, each individual pixel is considered"));

        vary3Btn.setText("Vary");
        vary3Btn.setTooltip(Utils.toolTip("Search for codes at points specified above. See Instructions"
                                          + " for how to use this"));
        Utils.colorButton(vary3Btn, Color.SKYBLUE, Color.GOLD);
        vary3Btn.setOnAction(event -> varyAction("Vary", "vary3.txt", false));

        // Zhao Yu Li, May 05, 2025.
        // A new button that performs the same computations as Vary, but only prints the middle code of each
        // (code type, code length) group.
        middleVaryButton.setText("LiMV");
        middleVaryButton.setTooltip(Utils.toolTip("Middle Vary. Search for codes at points specified above. But for codes of " +
                "the same type (i.e. CS, OSO, OSNO, etc.) and the same code length, only print the middle one."));
        Utils.colorButton(middleVaryButton, Color.SKYBLUE, Color.GOLD);
        middleVaryButton.setOnAction(event -> varyAction("Middle Vary", "middleVary3.txt", true));

        vary3BBtn.setText("Vary3B");
        vary3BBtn.setTooltip(Utils.toolTip("Search for codes at points specified above. See Instructions"
                                          + " for how to use this"));
        Utils.colorButton(vary3BBtn, Color.SKYBLUE, Color.GOLD);
        vary3BBtn.setOnAction(event -> {

        	final long startTime = System.currentTimeMillis();//george june 12,2019 added in !CS2cb.isSelected() && !CNS2cb.isSelected() && !ONS2cb.isSelected() && !OSNO2cb.isSelected() && !OSO2cb.isSelected()
        	if (!CS2cb.isSelected() && !CNS2cb.isSelected() && !ONS2cb.isSelected() && !OSNO2cb.isSelected() && !OSO2cb.isSelected()) {
        		final Alert alert = new Alert(AlertType.ERROR);

                alert.setTitle("Vary");
                alert.setHeaderText("No CodeTypes");
                alert.setContentText("Please select at least one codetype.");
                alert.showAndWait();
        	} else {
	            final int shots = Integer.parseInt(shotsText.getText());
	            final int max = Integer.parseInt(maxMovesText.getText());
	            final int min = Integer.parseInt(minMovesText.getText());

	            final ExecutorService executor = Executors.newFixedThreadPool(Utils.numThreads);

	            System.out.println(String.format(
	                "//------------------------- Vary3B %d shots at %d to %d moves -------------------------//", shots, min, max));

	            final Task<MutableSortedSet<ClassifiedCodeSequence>> varyTask
	            		= new Task<MutableSortedSet<ClassifiedCodeSequence>>() {

					@Override
					protected MutableSortedSet<ClassifiedCodeSequence> call() throws Exception {
						return varyTriangles(2, executor);
					}

	            };

	            final Progress progress = new Progress(varyTask);
	            final Thread varyThread = new Thread(varyTask);

	            varyTask.setOnSucceeded(success -> {
					try {
						MutableSortedSet<ClassifiedCodeSequence> allCodes = varyTask.get();
						allCodes.forEach(seq -> Database.saveToDatabase(seq, "garbage"));
						hitsLabel.setText("" + allCodes.size());
			            printCodes(allCodes, "vary3B.txt", true, true, allCodes.size());
					} catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException(e);
					}
					final long endTime = System.currentTimeMillis();
		        	System.out.println("Time: " + (endTime - startTime));
		        	System.out.println("Time: " + Utils.timeConvert(endTime - startTime));
		        	executor.shutdown();
		        	progress.close();
	            });

	            varyTask.setOnCancelled(cancelled -> {
	            	System.out.println("// Vary3B Cancelled");
	            	varyThread.interrupt();
	            	executor.shutdownNow();
	            	progress.close();
	            });
	            varyTask.setOnFailed(fail -> {
	            	System.out.println("// Vary3B failed");
		        	executor.shutdown();
	            	progress.close();
	            });

	        	varyThread.start();

	            progress.show();
        	}

        });

        
        vary4Btn.setText("V4");
        Utils.colorButton(vary4Btn, Color.SKYBLUE, Color.GOLD);
        vary4Btn.setTooltip(Utils.toolTip("Search for all codes at points specified above. See Instructions"
                + " for how to use this"));
        vary4Btn.setOnAction(event -> {


        	final long startTime = System.currentTimeMillis();
        	if (!CScb.isSelected() && !CNScb.isSelected() && !ONScb.isSelected() && !OSNOcb.isSelected() && !OSOcb.isSelected()) {
        		final Alert alert = new Alert(AlertType.ERROR);

                alert.setTitle("Vary");
                alert.setHeaderText("No CodeTypes");
                alert.setContentText("Please select at least one codetype.");
                alert.showAndWait();
        	} else {
	            final int shots = Integer.parseInt(shotsText.getText());
	            final int max = Integer.parseInt(maxMovesText.getText());
	            final int min = Integer.parseInt(minMovesText.getText());

	            if (max > 250) {
	            	final Alert alert = new Alert(AlertType.CONFIRMATION);

	                alert.setTitle("Vary4");
	                alert.setHeaderText("Vary4");
	                alert.setContentText(
	                    "Vary4 is slow at this number of moves.\nDo you want to continue?");

	                final Optional<ButtonType> response = alert.showAndWait();

	                if (!response.isPresent() || response.get() != ButtonType.OK) {
	                    return;
	                }
	            }

	            System.out.println(String.format(
	                "//------------------------- Vary4 %d shots at %d to %d moves -------------------------//", shots, min, max));
	            final ExecutorService executor = Executors.newFixedThreadPool(Utils.numThreads);

	            final MutableSortedSet<ClassifiedCodeSequence> allCodes = varyTriangles(4, executor);

	            printCodes(allCodes, "garbage.txt", true, false, allCodes.size());
        	}
        	final long endTime = System.currentTimeMillis();
        	System.out.println("Time: " + (endTime - startTime));
        	System.out.println("Time: " + Utils.timeConvert(endTime - startTime));


        });

        buildPolyCheckBox.setText("Make Poly");
        buildPolyCheckBox.setStyle(Utils.hex(Color.MISTYROSE));
        buildPolyCheckBox.setTooltip(Utils.toolTip("Use the console to construct a polygon. See "
                                                   + "instructions for how to use this"));
        buildPolyCheckBox.setOnAction(event -> {
            if (buildPolyCheckBox.isSelected()) {
                System.out.println("");
                System.out.println("+---------- make a polygon ----------+");

            } else {
                System.out.println("+------------------------------------+");
                System.out.println("");
            }
        });

        maxPrinting.setMaxWidth(50);
        maxPrinting.setText("1");
        maxPrinting.setTooltip(Utils.toolTip("Set the max number of codes to be printed from a set"
        		+ " found by varyL or PolyVary"));

        final HBox varyLine1HBox = new HBox(10, line1RDB, varyX1Text, varyY1Text);
        varyLine1HBox.setPadding(new Insets(0, 10, 10, 0));
        varyLine1HBox.setAlignment(Pos.CENTER);

        final HBox varyLine2HBox =
            //new HBox(10, line2RDB, varyX2Text, varyY2Text, line1CutText);
                new HBox(10, line2RDB, varyX2Text, varyY2Text);

                varyLine2HBox.setPadding(new Insets(0, 10, 10, 0));
        varyLine2HBox.setAlignment(Pos.CENTER);

        final HBox varyLine3HBox =
            new HBox(10, line3RDB, varyX3Text, varyY3Text, line2CutText);
        varyLine3HBox.setPadding(new Insets(0, 10, 10, 0));
        varyLine3HBox.setAlignment(Pos.CENTER);

        // George May 17,2023 switch the positions of the bounds and AutoPolyVary
//        final HBox varyInfoHBox =
//            new HBox(10, minMovesText, maxMovesText, movesLbl, shotsText, sideSpacingLbl);
//        varyInfoHBox.setPadding(new Insets(0, 10, 10, 0));
//        varyInfoHBox.setAlignment(Pos.CENTER);
        final HBox autoPolyVaryHBox = new HBox(10, autoPolyVaryBtn, lineStartField, lineStepField, lineEndField);
        autoPolyVaryHBox.setPadding(new Insets(0, 10, 10, 0));
        autoPolyVaryHBox.setAlignment(Pos.CENTER);

        final HBox codeTypesHBox =
            new HBox(10, CNScb, CScb, ONScb, OSNOcb, OSOcb, Triplescb);//george june 18,2019 replaced vary4Btn with vary3Btn
        codeTypesHBox.setPadding(new Insets(0, 10, 10, 0));
        codeTypesHBox.setAlignment(Pos.CENTER);
        
        //george june 12,2019 start
       // final HBox codeTypes2HBox =
         //       new HBox(10, vary3BBtn, CNS2cb, CS2cb, ONS2cb, OSNO2cb, OSO2cb);
        final HBox codeTypes2HBox = new HBox();
            codeTypes2HBox.setPadding(new Insets(0, 10, 10, 0));
            codeTypes2HBox.setAlignment(Pos.CENTER);
          //george june 12,2019 end

        /*final HBox vary3HBox =
            new HBox(10, vary3Btn, varyLBtn, hitsLabel, autoVaryButton, autoCycleText, minPrinting,
            		maxPrinting);*/ //, autoIterText, autoStepText //added minPrinting george june6,2019

        // Zhao Yu Li, May 06, 2025.
        // Moved buildPolyCheckBox from to Viewr.java.
        // Added new MiddleVaryL button.
        final HBox vary3HBox =
        		new HBox(10, cycleVaryButton, middleVaryLButton, varyLBtn, maxPrinting, polyAutoBtn,autoCycleText);
           // new HBox(10, vary4Btn, varyLBtn, hitsLabel, autoVaryButton, autoCycleText, //george june 18,2019 replaced vary3Btn with vary4Btn
            		//maxPrinting); //, autoIterText, autoStepText

        //new HBox(10, varyLBtn, hitsLabel, autoVaryButton, autoCycleText, //george june 18,2019 replaced vary3Btn with vary4Btn
          //      maxPrinting); //, autoIterText, autoStepText
        //new HBox(10, buildPolyCheckBox, varyLBtn, hitsLabel, autoCycleText, //george june 18,2019 replaced vary3Btn with vary4Btn
            //    maxPrinting); //, autoIterText, autoStepText

        final HBox newHBox= new HBox(10, middleVaryButton, vary3Btn, hitsLabel, minMovesText, maxMovesText, movesLbl, shotsText, sideSpacingLbl);
        newHBox.setPadding(new Insets(0, 10, 10, 0));
        newHBox.setAlignment(Pos.CENTER);

        final HBox superHBox = new HBox(10, superPolyVaryBtn, cycleStepText, superAutoCb);
        superHBox.setPadding(new Insets(0, 10, 10, 0));
        superHBox.setAlignment(Pos.CENTER);

        vary3HBox.setPadding(new Insets(0, 10, 10, 0));
        vary3HBox.setAlignment(Pos.CENTER);
        //note vary4Btn says V4 on the button george June 18,2019
        
        //final HBox autoHBox = new HBox(10, buildPolyCheckBox, varyOnePoint, allPixCheckBox, polyAutoBtn);
        //final HBox autoHBox = new HBox(10, buildPolyCheckBox, polyAutoBtn);
        //final HBox autoHBox = new HBox(10, polyAutoBtn);
        //autoHBox.setPadding(new Insets(0, 10, 10, 0));
        //autoHBox.setAlignment(Pos.CENTER);

        wrapper.setSpacing(8);
        //wrapper.getChildren().addAll(varyLine1HBox, varyLine2HBox, varyLine3HBox,
          //      varyInfoHBox, codeTypesHBox, codeTypes2HBox, vary3HBox, autoHBox);

       // wrapper.getChildren().addAll(varyLine1HBox, varyLine2HBox, varyLine3HBox,
         //     varyInfoHBox, codeTypesHBox, codeTypes2HBox, vary3HBox);
        wrapper.getChildren().addAll(varyLine1HBox, varyLine2HBox, superHBox, autoPolyVaryHBox, vary3HBox, codeTypesHBox, newHBox);
        //wrapper.getChildren().addAll(varyLine1HBox, varyLine2HBox, varyLine3HBox,
         //                            varyInfoHBox, codeTypes2HBox, vary3HBox, autoHBox);
    }//george june 12,2019 changed codeTypesHBox to codeTypes2HBox

    // Zhao Yu Li, May 05, 2025.
    // Function that performs the Vary computations. This function is only used for Vary and MiddleVary because they
    // carry out the same computations, but just prints the results differently.
    private void varyAction(String title, String outFile, boolean printMid) {
        final long startTime = System.currentTimeMillis();//george june 12,2019 added in !CS2cb.isSelected() && !CNS2cb.isSelected() && !ONS2cb.isSelected() && !OSNO2cb.isSelected() && !OSO2cb.isSelected()
        if (!CScb.isSelected() && !CNScb.isSelected() && !ONScb.isSelected() && !OSNOcb.isSelected() && !OSOcb.isSelected()) {
            final Alert alert = new Alert(AlertType.ERROR);

            alert.setTitle(title);
            alert.setHeaderText("No CodeTypes");
            alert.setContentText("Please select at least one codetype.");
            alert.showAndWait();
        } else {
            final int shots = Integer.parseInt(shotsText.getText());
            final int max = Integer.parseInt(maxMovesText.getText());
            final int min = Integer.parseInt(minMovesText.getText());

            final ExecutorService executor = Executors.newFixedThreadPool(Utils.numThreads);

            System.out.printf(
                    "//------------------------- " + (printMid ? "Middle " : "") + "Vary %d shots at %d to %d moves -------------------------//%n", shots, min, max);

            final Task<MutableSortedSet<ClassifiedCodeSequence>> varyTask
                    = new Task<MutableSortedSet<ClassifiedCodeSequence>>() {

                @Override
                protected MutableSortedSet<ClassifiedCodeSequence> call() throws Exception {
                    return varyTriangles(3, executor);
                }

            };

            //final Progress progress = new Progress(varyTask);
            final ProgressWithStatus progress = new ProgressWithStatus(varyTask, "Calling findCodes3 (no status)", 0);
            final Thread varyThread = new Thread(varyTask);

            varyTask.setOnSucceeded(success -> {
                try {
                    MutableSortedSet<ClassifiedCodeSequence> allCodes = varyTask.get();
                    allCodes.forEach(seq -> Database.saveToDatabase(seq, "garbage"));
                    hitsLabel.setText("" + allCodes.size());
                    printCodes(allCodes, outFile, true, true, allCodes.size(), printMid);
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                final long endTime = System.currentTimeMillis();
                System.out.println("Time: " + (endTime - startTime));
                System.out.println("Time: " + Utils.timeConvert(endTime - startTime));
                executor.shutdown();
                progress.close();
            });

            varyTask.setOnCancelled(cancelled -> {
                System.out.println("// " + title + " Cancelled");
                varyThread.interrupt();
                executor.shutdownNow();
                progress.close();
            });
            varyTask.setOnFailed(fail -> {
                System.out.println("// " + title + " failed");
                executor.shutdown();
                progress.close();
            });

            varyThread.start();

            progress.show();
        }
    }

    public void click(final double xDeg, final double yDeg) {

        if (buildPolyCheckBox.isSelected()) {
            System.out.println(xDeg + " " + yDeg);
        }

        if (line3RDB.isSelected()) {
            varyX3Text.setText(Double.toString(xDeg));
            varyY3Text.setText(Double.toString(yDeg));
        } else if (line2RDB.isSelected()) {
            varyX2Text.setText(Double.toString(xDeg));
            varyY2Text.setText(Double.toString(yDeg));
        } else {
            varyX1Text.setText(Double.toString(xDeg));
            varyY1Text.setText(Double.toString(yDeg));
        }
    }

    private MutableSortedSet<ClassifiedCodeSequence> varyTriangles(
    		final int version, final ExecutorService exe) {
        return varyTriangles(Double.parseDouble(varyX1Text.getText()),
                              Double.parseDouble(varyY1Text.getText()),
                              Double.parseDouble(varyX2Text.getText()),
                              Double.parseDouble(varyY2Text.getText()),
                              Double.parseDouble(varyX3Text.getText()),
                              Double.parseDouble(varyY3Text.getText()),
                              Double.parseDouble(line1CutText.getText()),
                              Double.parseDouble(line2CutText.getText()),
                              version, exe);
    }

    public MutableSortedSet<ClassifiedCodeSequence> varyTriangles(
    		final double aX1, final double aY1, final double aX2, final double aY2,
            final double aX3, final double aY3, final double aCut1, final double aCut2,
            final int version, final ExecutorService exe) {

        final MutableSortedSet<ClassifiedCodeSequence> bareCodesFound = new TreeSortedSet<>();

        for (double cut2 = 0; cut2 <= aCut2; ++cut2) {
            for (double cut1 = 0; cut1 <= aCut1;  ++cut1) {

                double a;
                double b;

                if (aCut2 > 0 && aCut1 > 0) {
                    a = aX1 + (aX2 - aX1) * cut1 / aCut1 + (aX3 - aX1) * cut2 / aCut2;
                    b = aY1 + (aY2 - aY1) * cut1 / aCut1 + (aY3 - aY1) * cut2 / aCut2;
                } else if (aCut1 > 0) {
                    a = aX1 + (aX2 - aX1) * cut1 / aCut1;
                    b = aY1 + (aY2 - aY1) * cut1 / aCut1;
                } else if (aCut2 > 0) {
                    a = aX1 + (aX3 - aX1) * cut2 / aCut2;
                    b = aY1 + (aY3 - aY1) * cut2 / aCut2;
                } else {
                    a = aX1;
                    b = aY1;
                }

                final MutableSortedSet<ClassifiedCodeSequence> pointCodes = findCodes(a, b, version, exe);
                bareCodesFound.addAll(pointCodes);
            }
        }
        return bareCodesFound;
    }
    public MutableSortedSet<ClassifiedCodeSequence> varyTrianglesL(
    				final Vector2 point, final ExecutorService executor) { // Got rid of findCodes since it was unecessary complexity
        return findCodes(point.x, point.y, 3, executor);
    }

    // Overloading for seperate maximums
    public MutableSortedSet<ClassifiedCodeSequence> varyTrianglesL(
    				final Vector2 point, final int CSmaxSS, final int OSOmaxSS, final int OSNOmaxSS, final ExecutorService executor) { // Got rid of findCodes since it was unecessary complexity
        final int min = Integer.parseInt(minMovesText.getText());
        final double shots = Integer.parseInt(shotsText.getText());
        final boolean[] noCS = {OSOmaxSS > 0, false,
                                 CNScb.isSelected(), ONScb.isSelected(), OSNOmaxSS > 0};
        final boolean[] onlyCS = {false, CSmaxSS > 0, false, false, false};
        final MutableSortedSet<ClassifiedCodeSequence> unfilteredCodesFound = new TreeSortedSet<>();
        final MutableSortedSet<ClassifiedCodeSequence> codesFound = new TreeSortedSet<>();
        unfilteredCodesFound.addAll(findCodes3(point.x, point.y, min, CSmaxSS, shots, onlyCS, executor));
        unfilteredCodesFound.addAll(findCodes3(point.x, point.y, min, Math.max(OSOmaxSS, OSNOmaxSS), shots, noCS, executor));
        for(final ClassifiedCodeSequence code: unfilteredCodesFound) { // Filter out overly large OSO/OSNO
            final CodeType type = code.codeType;
            if(type.equals(CodeType.OSO) && code.codeSum >= OSOmaxSS) {
                continue;
            }
            if(type.equals(CodeType.OSNO) && code.codeSum >= OSNOmaxSS) {
                continue;
            }
            codesFound.add(code);
        }
    	return codesFound;
    }

    public MutableSortedSet<ClassifiedCodeSequence> autoVary(
                final Vector2 point, final ExecutorService exe) {
        int max = Integer.parseInt(maxMovesText.getText());
        int min = Integer.parseInt(minMovesText.getText());
        final int iterate = Integer.parseInt(autoIterText.getText());
        final int step = Integer.parseInt(autoStepText.getText());
        final int shots = Integer.parseInt(shotsText.getText());
        final boolean[] types = {OSOcb.isSelected(), CScb.isSelected(),
                                 CNScb.isSelected(), ONScb.isSelected(), OSNOcb.isSelected(), OSO2cb.isSelected(), CS2cb.isSelected(),
                                 CNS2cb.isSelected(), ONS2cb.isSelected(), OSNO2cb.isSelected()};
        //george june 12,2019 added , OSO2cb.isSelected(), CS2cb.isSelected(),
        //CNS2cb.isSelected(), ONS2cb.isSelected(), OSNO2cb.isSelected()

        for (int i = 0; i < iterate + 1; i++) {
            final MutableSortedSet<ClassifiedCodeSequence> codesFound = new TreeSortedSet<>();
            codesFound.addAll(findCodes3(point.x, point.y, min, max, shots, types, exe));
            printCodes(codesFound, "garbage.txt", printAll, true, Integer.parseInt(maxPrinting.getText()));

            if (codesFound.isEmpty()) {
                min = Integer.valueOf(max);
                max += step;

            } else {
                return codesFound;
            }
        }
        return new TreeSortedSet<>();
    }

    // Overloading of autoVary for seperate maximums during overrideSS
    public MutableSortedSet<ClassifiedCodeSequence> autoVary(
                final Vector2 point, final int CSmaxSS, final int OSOmaxSS, final int OSNOmaxSS, final ExecutorService exe) {
        int CSmin = Integer.parseInt(minMovesText.getText());
        int CSstep = 0;
        int OSmin = Integer.parseInt(minMovesText.getText());
        int OSstep = 0;
        final int iterate = Integer.parseInt(autoIterText.getText());
        final int step = Integer.parseInt(autoStepText.getText());
        final int shots = Integer.parseInt(shotsText.getText());
        final boolean[] noCS = {OSOmaxSS > 0, false,
                                 CNScb.isSelected(), ONScb.isSelected(), OSNOmaxSS > 0, OSO2cb.isSelected(), CS2cb.isSelected(),
                                 CNS2cb.isSelected(), ONS2cb.isSelected(), OSNO2cb.isSelected()};
        
        final boolean[] onlyCS = {false, CSmaxSS > 0,
            false, false, false, false, false, false, false, false
        };
        //george june 12,2019 added , OSO2cb.isSelected(), CS2cb.isSelected(),
        //CNS2cb.isSelected(), ONS2cb.isSelected(), OSNO2cb.isSelected()

        for (int i = 0; i < iterate + 1; i++) {
            final MutableSortedSet<ClassifiedCodeSequence> unfilteredCodesFound = new TreeSortedSet<>();
            final MutableSortedSet<ClassifiedCodeSequence> codesFound = new TreeSortedSet<>();
            if(CSmaxSS > 0) {
                unfilteredCodesFound.addAll(findCodes3(point.x, point.y, CSmin, CSmaxSS + CSstep, shots, onlyCS, exe));
            }
            unfilteredCodesFound.addAll(findCodes3(point.x, point.y, OSmin, Math.max(OSOmaxSS, OSNOmaxSS) + OSstep, shots, noCS, exe));

            for(final ClassifiedCodeSequence code: unfilteredCodesFound) { // Filter out overly large OSO/OSNO
                final CodeType type = code.codeType;
                if(type.equals(CodeType.OSO) && code.codeSum >= OSOmaxSS) {
                    continue;
                }
                if(type.equals(CodeType.OSNO) && code.codeSum >= OSNOmaxSS) {
                    continue;
                }
                codesFound.add(code);
            }
            printCodes(codesFound, "garbage.txt", printAll, true, Integer.parseInt(maxPrinting.getText()));

            if (codesFound.isEmpty()) {
                CSmin = CSmaxSS;
                CSstep += step;
                OSmin = Math.max(OSOmaxSS, OSNOmaxSS);
                OSstep += OSstep;
            } else {
                return codesFound;
            }
        }
        return new TreeSortedSet<>();
    }

    private MutableSortedSet<ClassifiedCodeSequence> findCodes(
    		final double xCoord, final double yCoord, final int version, final ExecutorService exe) {
        final MutableSortedSet<ClassifiedCodeSequence> out = new TreeSortedSet<>();
        final int max = Integer.parseInt(maxMovesText.getText());
        final int min = Integer.parseInt(minMovesText.getText());
        final double shots = Integer.parseInt(shotsText.getText());
        final boolean[] types = {OSOcb.isSelected(), CScb.isSelected(),
                                 CNScb.isSelected(), ONScb.isSelected(), OSNOcb.isSelected()};
        final boolean[] types2 = {OSO2cb.isSelected(), CS2cb.isSelected(),
                CNS2cb.isSelected(), ONS2cb.isSelected(), OSNO2cb.isSelected()};

        
        //george june12,2019 added , OSO2cb.isSelected(), CS2cb.isSelected(),
        //CNS2cb.isSelected(), ONS2cb.isSelected(), OSNO2cb.isSelected()
        if (version == 4) {
        	out.addAll(findCodes4(xCoord, yCoord, min, max, shots, types));
        } else if (version == 3) {
        	out.addAll(findCodes3(xCoord, yCoord, min, max, shots, types, exe));
        } else if (version == 2) {
        	out.addAll(findCodes2(xCoord, yCoord, min, max, shots, types2, exe));
        } else {
        	throw new RuntimeException("Version for varyTriangles must be 3 or 4");
        }
        return out;
    }

    // boolean[] types should be in the order OSO, CS, CNS, ONS, OSNO
    public static MutableSet<ClassifiedCodeSequence> findCodes2(
        final double xCoord, final double yCoord, final int min, final int max, final double shots,
        final boolean[] types, final ExecutorService executor) {

        final double xRad = FastMath.toRadians(xCoord);
        final double yRad = FastMath.toRadians(yCoord);

        final double base = Math.sin(xRad + yRad);

        final MutableSet<ClassifiedCodeSequence> codeSeqs = new UnifiedSet<>();

        final MutableList<Future<MutableList<ClassifiedCodeSequence>>> futures = new FastList<>();

        final double increment = base / (shots + 1);

        if (types[1] && !types[0] && !types[2] && !types[0] && !types[4]) {
        	//run the CS-specific code

        	double xAngle = Double.valueOf(xRad);
        	double yAngle = Double.valueOf(yRad);

        	for (int i = 0; i < 3; i++) {

        		final Double finX = xAngle;
        		final Double finY = yAngle;

	            final Future<MutableList<ClassifiedCodeSequence>> future =
	                executor.submit(() -> VaryCS.fireAway(min, max, finX, finY));

	            futures.add(future);

	            double zAngle = Double.valueOf(Math.PI - xAngle - yAngle);
	            xAngle = Double.valueOf(yAngle);
	        	yAngle = Double.valueOf(zAngle);
	        }
        }
        else {
        	//run the non-CS-specific code
	        for (int count = 1; count <= shots; ++count) {

	            final double pos = count * increment;

	            final Future<MutableList<ClassifiedCodeSequence>> future =
	            		executor.submit(() -> Vary3.fireAway(min, max, xRad, yRad, pos));

	            futures.add(future);
	        }
        }

        for (final Future<MutableList<ClassifiedCodeSequence>> future : futures) {
        	try {
                for (final ClassifiedCodeSequence codeSeq : future.get()) {
                    final CodeType type = codeSeq.codeType;
                    
                    /*if ((types[0] && type.equals(CodeType.OSO)) ||
                        (types[1] && type.equals(CodeType.CS))  ||
                        (types[2] && type.equals(CodeType.CNS)) ||
                        (types[3] && type.equals(CodeType.ONS)) ||
                        (types[4] && type.equals(CodeType.OSNO))) {*/ //george june12,2019 this is the original

                    if ((types[0] && type.equals(CodeType.OSO)) ||
                        (types[1] && type.equals(CodeType.CS))  ||
                        (types[2] && type.equals(CodeType.CNS)) ||
                        (types[3] && type.equals(CodeType.ONS)) ||
                        (types[4] && type.equals(CodeType.OSNO))) 
                        {

                    	if (codeSeq.codeSum >= min) {
                    		codeSeqs.add(codeSeq);
                    	}
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return codeSeqs;
    }

    // boolean[] types should be in the order OSO, CS, CNS, ONS, OSNO
    // boolean[] types should be in the order OSO, CS, CNS, ONS, OSNO
    /* Jul,31 Marco Mai
     * 1. the excuator will run each loop one by one
     * 2. code type will transfer to the backend will computing, only transfer match one back to java here reduce memory usage
     * 3. parallel computing most of the for loop
     */
    public static MutableSet<ClassifiedCodeSequence> findCodes3(
        final double xCoord, final double yCoord, final int min, final int max, final double shots,
        final boolean[] types, final ExecutorService executor) {

        final double xRad = FastMath.toRadians(xCoord);
        final double yRad = FastMath.toRadians(yCoord);

        final double base = Math.sin(xRad + yRad);

        final MutableSet<ClassifiedCodeSequence> codeSeqs = new UnifiedSet<>();

        final MutableList<ClassifiedCodeSequence> futures = new FastList<>();
        final MutableList<Future<MutableList<ClassifiedCodeSequence>>> futures2 = new FastList<>();

        final double increment = base / (shots + 1);

        StringBuilder selectedTypes = new StringBuilder();

        //transfer this to backend checking if right type
        if (types[0] ) selectedTypes.append("OSO ");
        if (types[1] ) selectedTypes.append("CS ");
        if (types[2] ) selectedTypes.append("CNS ");
        if (types[3] ) selectedTypes.append("ONS ");
        if (types[4] ) selectedTypes.append("OSNO ");

        String reqTypes = selectedTypes.toString().trim();
        //run the CS-specific code
        if (types[1]) {
        	double xAngle = Double.valueOf(xRad);
        	double yAngle = Double.valueOf(yRad);

        	for (int i = 0; i < 3; i++) {

        		final Double finX = xAngle;
        		final Double finY = yAngle;

                final Future<MutableList<ClassifiedCodeSequence>> future =
	                executor.submit(() -> VaryCS.fireAway(min, max, finX, finY,reqTypes));
	            // final Future<MutableList<ClassifiedCodeSequence>> future =
	            //     executor.submit(() -> VaryCS.fireAway(min, max, finX, finY));


                try {
                    MutableList<ClassifiedCodeSequence> result = future.get();
                    futures.addAll(result);
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);  // or handle it however you need
                }

	            double zAngle = Double.valueOf(Math.PI - xAngle - yAngle);
	            xAngle = Double.valueOf(yAngle);
	        	yAngle = Double.valueOf(zAngle);
	        }
        }
        //run the non-CS-specific code
        if(types[0] || types[2] || types[3] || types[4]) {

	        for (int count = 1; count <= shots; ++count) {

	            final double pos = count * increment;

	            final Future<MutableList<ClassifiedCodeSequence>> future =
	            		executor.submit(() -> Vary3.fireAway(min, max, xRad, yRad, pos,reqTypes));
                        //executor.submit(() -> Vary3.fireAway(min, max, xRad, yRad, pos));

                futures2.add(future);
	        }
            for (Future<MutableList<ClassifiedCodeSequence>> future : futures2) {
                try {
                    MutableList<ClassifiedCodeSequence> partial = future.get(); // get the actual list
                    futures.addAll(partial); // now addAll on MutableList, not Future
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace(); // handle exceptions as needed
                }
            }
        }

        codeSeqs.addAll(futures);

        return codeSeqs;
    }

    // boolean[] types should be in the order OSO, CS, CNS, ONS, OSNO
    private static MutableSet<ClassifiedCodeSequence> findCodes4(
        final double xCoord, final double yCoord, final int min, final int max, final double shots,
        final boolean[] types) {

        final double xRad = FastMath.toRadians(xCoord);
        final double yRad = FastMath.toRadians(yCoord);

        final MutableSet<ClassifiedCodeSequence> codeSeqs = new UnifiedSet<>();
        

        StringBuilder selectedTypes = new StringBuilder();

        //transfer this to backend checking if right type
        if (types[0] ) selectedTypes.append("OSO ");
        if (types[1] ) selectedTypes.append("CS ");
        if (types[2] ) selectedTypes.append("CNS ");
        if (types[3] ) selectedTypes.append("ONS ");
        if (types[4] ) selectedTypes.append("OSNO ");

        String reqTypes = selectedTypes.toString().trim();

        final ExecutorService executor = Executors.newFixedThreadPool(Utils.numThreads);
        if (types[1] && !types[0] && !types[2] && !types[0] && !types[4]) {
        	//run the CS-specific code


        	double xAngle = Double.valueOf(xRad);
        	double yAngle = Double.valueOf(yRad);

        	for (int i = 0; i < 3; i++) {

        		final Double finX = xAngle;
        		final Double finY = yAngle;

                final Future<MutableList<ClassifiedCodeSequence>> future =
	                executor.submit(() -> VaryCS.fireAway(min, max, finX, finY,reqTypes));
	            // final Future<MutableList<ClassifiedCodeSequence>> future =
	            //     executor.submit(() -> VaryCS.fireAway(min, max, finX, finY));


                try {
                    MutableList<ClassifiedCodeSequence> result = future.get();
                    codeSeqs.addAll(result);
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);  // or handle it however you need
                }

	            double zAngle = Double.valueOf(Math.PI - xAngle - yAngle);
	            xAngle = Double.valueOf(yAngle);
	        	yAngle = Double.valueOf(zAngle);
	        
            }

        }
        else {

            final MutableList<ClassifiedCodeSequence> future = Vary4.fireAway(min, max, xRad, yRad,reqTypes);
            codeSeqs.addAll(future);
        }

        executor.shutdown();
        return codeSeqs;
    }


    // Objects for comparison functionality
    static ArrayList<ArrayList<String>> cList = new ArrayList<>();
    static ArrayList<String> savePairs = new ArrayList<>();
    static ArrayList<String> varySeq = new ArrayList<>();
    // setting this to true enables the compare functionality for code sequences.
    static boolean compare = false;
    boolean dragIntend = false;

    // a method for printing a set of codes. Can set print to false, which makes this function just write
    // to the file.
    public static void printCodes(final MutableSortedSet<ClassifiedCodeSequence> allCodes, final String file,
    		                      final boolean print, final boolean erase, final int Number, final boolean printMid) {

    	final Path path = Paths.get(file);

        if (!Files.exists(path)) {
            try {
				Files.createFile(path);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
        }

        if (erase) {
        	PrintWriter pw;
			try {
				pw = new PrintWriter(file);
			} catch (FileNotFoundException e) {
				throw new RuntimeException("Couldn't erase the file");
			}
        	pw.close();
        }

        final ArrayList<ClassifiedCodeSequence> splitCodes;
        final CodeType[] codeTypes = {CodeType.CS, CodeType.OSO, CodeType.OSNO, CodeType.CNS, CodeType.ONS};
        if (splitUp) {
            for (final CodeType type : codeTypes) {
                for (final ClassifiedCodeSequence code : allCodes) {
                    if (code.codeType.equals(type)) {
                        splitCodes.add(code);
                    }
                }
            }
        } else {
            splitCodes = new ArrayList<>(allCodes);
        }

        final ArrayList<ClassifiedCodeSequence> organizedCodes = new ArrayList<>(splitCodes);

        // Zhao Yu Li, May 05, 2025.
        // Prints only the middle code of the list of codes with the same type (i.e. CS, OSNO, OSO, etc.)
        // and code length.
        // Zhao Yu Li, May 06, 2025.
        // Groups are distinguished by (code type, code length, and odd-even pattern)
        if (printMid) {
            organizedCodes.clear();

            for (final CodeType type : codeTypes) {
                long currentLength = -1;
                Map<String, ArrayList<ClassifiedCodeSequence>> processedCodes = new HashMap<>();
                Map<String, Integer> processedCodesLength = new HashMap<>();

                for (final ClassifiedCodeSequence code : splitCodes) {
                    if (code.codeType.equals(type)) {
                        if (currentLength == -1) {
                            currentLength = code.codeLength;
                        }

                        if (code.codeLength == currentLength) {
                            processedCodesLength.compute(code.oddEvenPattern,
                                    (k, lengthCount) -> (lengthCount == null) ? 1 : lengthCount + 1);

                            if (!processedCodes.containsKey(code.oddEvenPattern)) {
                                processedCodes.put(code.oddEvenPattern, new ArrayList<>());
                            }
                            processedCodes.get(code.oddEvenPattern).add(code);
                        } else {
                            for (String oddEvenPattern : processedCodesLength.keySet()) {
                                // Only add the middle one
                                // Updated Jun 20, 2025.
                                // Also adds the first and the last codes.
                                addFirstMidLast(organizedCodes, processedCodes, processedCodesLength, oddEvenPattern);
                            }

                            // Clear and re-initialize for the next iteration
                            processedCodes.clear();
                            processedCodes.put(code.oddEvenPattern, new ArrayList<>());
                            processedCodes.get(code.oddEvenPattern).add(code);
                            currentLength = code.codeLength;
                            processedCodesLength.clear();
                            processedCodesLength.put(code.oddEvenPattern, 1);
                        }
                    }
                }

                // We reached the end of the iteration, add the middle of last (code type, code length, odd-even) group
                // Updated Jun 20, 2025.
                // Also prints the first and the last codes.
                for (String oddEvenPattern : processedCodesLength.keySet()) {
                    if (!processedCodes.get(oddEvenPattern).isEmpty()) {
                        addFirstMidLast(organizedCodes, processedCodes, processedCodesLength, oddEvenPattern);
                    }
                }
            }
        }

        int count = 0;
        ArrayList<String> codes = new ArrayList<>();
        for (final ClassifiedCodeSequence code : organizedCodes) {
        	count += 1;
    		final String codeString = Utils.standard(code, count);
        	if (count <= Number && print) {
        		System.out.println(codeString);
                //codes.add(codeString.substring(5));
                codes.add(codeString.substring(codeString.indexOf("-") + 2));
        	}
            try {
                final PrintStream output = new PrintStream(new FileOutputStream(file, true));
                output.println(codeString + " " + CodeSequence.evenOddSequence(code.codeSequence.codeNumbers));
                output.close();
            } catch (final FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        varySeq.clear();
        varySeq.addAll(codes);


        if (compare) {
            // Adding the Vary3 codes to 2D ArrayList of ArrayList for comparison.
            if (cList.size() >= 2) {
                cList.remove(0);
                cList.add(1, codes);
            }
            else if(cList.size() == 0) {
                cList.add(0, codes);
            }
            else {
                cList.add(1, codes);
            }

            ArrayList<String> matching = Utils.compare(cList);
            if (cList.size() == 2) {
                System.out.println("//--------------------------- Matching Code Sequences ---------------------------//");
                if (matching.isEmpty()) {
                    System.out.println("None matching...");
                    cList.remove(0);
                }
                else {
                    savePairs.clear();
                    for (String code : matching) {
                        System.out.println(code);
                        savePairs.add(code);
                    }
                    cList.add(matching);

                }
                if (cList.size()>= 2) {
                    cList.remove(0);
                }
            }

        }
    }

    private static void addFirstMidLast(
            ArrayList<ClassifiedCodeSequence> organizedCodes,
            Map<String, ArrayList<ClassifiedCodeSequence>> processedCodes,
            Map<String, Integer> processedCodesLength,
            String oddEvenPattern)
    {
        if (processedCodesLength.get(oddEvenPattern) >= 2)
            organizedCodes.add(processedCodes.get(oddEvenPattern).get(0));

        organizedCodes.add(processedCodes.get(oddEvenPattern)
                .get(processedCodesLength.get(oddEvenPattern) / 2));

        if (processedCodesLength.get(oddEvenPattern) >= 3)
            organizedCodes.add(processedCodes.get(oddEvenPattern).get(processedCodesLength.get(oddEvenPattern) - 1));
    }

    // a method for printing a set of codes. Can set print to false, which makes this function just write
    // to the file.
    public static void printCodes(final MutableSortedSet<ClassifiedCodeSequence> allCodes, final String file,
                                  final boolean print, final boolean erase, final int Number) {
        printCodes(allCodes, file, print, erase, Number, false);
    }

    public String typeString() {
        String types = "";
        if (OSNOcb.isSelected()) {
            types += "OSNO ";
        }
        if (OSOcb.isSelected()) {
            types += "OSO ";
        }
        if (CScb.isSelected()) {
            types += "CS ";
        }
        if (CNScb.isSelected()) {
            types += "CNS ";
        }
        if (ONScb.isSelected()) {
            types += "ONS ";
        }/*//george june 12,2019 start
        if (OSNO2cb.isSelected()) {
            types += "OSNO ";
        }
        if (OSO2cb.isSelected()) {
            types += "OSO ";
        }
        if (CS2cb.isSelected()) {
            types += "CS ";
        }
        if (CNS2cb.isSelected()) {
            types += "CNS ";
        }
        if (ONS2cb.isSelected()) {
            types += "ONS ";
        }//george june 12,2019 end*/
        return types;
    }

    public ArrayList<Double> getRadianCoord() {
        ArrayList<Double> result = new ArrayList<>();
        double x = Math.toRadians(Double.parseDouble(varyX1Text.getText()));
        double y = Math.toRadians(Double.parseDouble(varyY1Text.getText()));
        result.add(x);
        result.add(y);
        return result;
    }
}
