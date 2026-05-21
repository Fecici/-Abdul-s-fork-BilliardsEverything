package billiards.viewer;

import billiards.codeseq.*;
import billiards.database.InfoAll;
import billiards.database.Database;
import billiards.geometry.Vector2;
import billiards.math.Equation;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import java.util.ArrayList;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.Math;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import java.util.concurrent.ExecutorService;
import javafx.util.Duration;
import javaslang.control.Either;

// Careful, don't put comments in between imports, or Eclipse will
// remove them when you reorganize the imports

// sort vector before getting lr
// use lr starting with empty region so it don't crash
// Make sure any nulls don't propogate forward in Database and Search
// update current code numbers in the right spot
// add option to draw picture when searching

public final class Utils {
    // TODO find the best value for this
    public static final int numThreads = (int) (Runtime.getRuntime().availableProcessors() * 0.5);

    public static Optional<ImmutableIntList> splitString(String textCodeSeq) {

        textCodeSeq = textCodeSeq.split("//")[0];
        // split on whitespace
        final String[] textCodeNumbers = textCodeSeq.trim().split("\\s+");

        final MutableIntList list = new IntArrayList();

        for (final String textCodeNumber : textCodeNumbers) {
            if (!textCodeNumber.isEmpty()) {
                try {
                    final int codeNumber = Integer.parseInt(textCodeNumber);
                    list.add(codeNumber);
                } catch (final NumberFormatException e) {
                    return Optional.empty();
                }
            }
        }

        return Optional.of(list.toImmutable());
    }

    // Initialize orderly shutdown of an executorService and block the calling thread until complete. Returns true if successful
    public static boolean safeShutdownExecutor(ExecutorService executor) {
        executor.shutdown(); // Prevent further submissions
        try {
            if(!executor.awaitTermination(600, TimeUnit.SECONDS)) {
                // Attempt cancellation again, if necessary
                executor.shutdownNow();
                if(!executor.awaitTermination(600, TimeUnit.SECONDS)) {
                    System.err.println("Warning: Executor not terminated after 20 minutes");
                    return false;
                }
            }
            return true;
        } catch(InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // converts a side sequence to a code sequence
    public static Optional<ClassifiedCodeSequence> convert(final IntList codeList) {

        final MutableIntList newCode = IntArrayList.newList(codeList);
        final int len = newCode.size();
        int count = 0;

        while (newCode.get(0) == newCode.get(newCode.size() - 1) && count < len + 1) {
            CodeSequence.rotateLeft(newCode);
            count += 1;

        }
        if (count >= len) {
        	return Optional.empty();
        }

        final MutableIntList finalList = new IntArrayList();

        int counter = 0;
        final int size = newCode.size();
        for (int i = 0; i < size; i++) {
            counter += 1;
            if (newCode.get(i) != newCode.get((i + 1) % size)) {
                finalList.add(counter);
                counter = 0;
            }
        }

        try {
        	final ClassifiedCodeSequence codeSeq = ClassifiedCodeSequence.create(finalList).get();
        	return Optional.of(codeSeq);

        } catch(final NoSuchElementException e) {
            return Optional.empty();

        }
    }

    public static void writeToFile(final String path, final String contents) {
        try {
            if (!Files.exists(Paths.get(path))) {
                Files.createFile(Paths.get(path));
            }
            Files.write(Paths.get(path), contents.getBytes());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Print everything in the iterable to the given file, separated by newlines
    public static <T> void printToFile(final String path, final Iterable<T> iterable) {
        final StringBuilder builder = new StringBuilder();

        for (final T object : iterable) {
            builder.append(object);
            builder.append(System.lineSeparator());
        }

        final String contents = builder.toString();

        writeToFile(path, contents);
    }

    public static String readFromFile(final String string) {

        try {

            final Path path = Paths.get(string);

            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            final byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, Charset.defaultCharset());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Copy each element of source into dest, overriding the current values
    public static void copyInto(final MutableIntList dest, final IntList source) {
        for (int i = 0; i < dest.size(); ++i) {
            final int elem = source.get(i);
            dest.set(i, elem);
        }
    }

    public static boolean isCoords(final String line) {
    	if (!line.contains(" ")) {
    		return false;
    	} if (line.split(" ").length != 2) {
    		return false;
    	}
    	final boolean[] coords = {false, false};
    	for (int i = 0; i < 2; i++) {
    		final String coord = line.split(" ")[i];
    		if (coord.contains(".") && !coord.replace(".", "").contains(".")) {
    			if (isDouble(coord)) {
    				coords[i] = true;
    			}
    		}
    	}
    	if (coords[0] && coords[1]) {
    		 return true;
    	}
    	return false;
    }

    public static boolean isDouble(final String string) {
    	try {
    		Double.parseDouble(string);
    		return true;
    	}
    	catch(NumberFormatException e) {
    		return false;
    	}
    }

    public static String ifGet(final String[] l, final int i) {
    	if (l.length > i) {
    		return l[i];
    	} else {
    		return "";
    	}

    }

    /**
     * Zhao Yu Li, Jun 3, 2025.
     * Returns a list of ClassifiedCodeSequences which are in both lists.
     * @param lists A list that contains two lists of ClassifiedCodeSequences to be compared.
     * @return A list of ClassifiedCodeSequences that are in both lists.
     */
    public static ArrayList<ClassifiedCodeSequence> getIntersectionCodes(final ArrayList<Collection<ClassifiedCodeSequence>> lists) {
        ArrayList<ClassifiedCodeSequence> toReturn = new ArrayList<>();

        if (lists.size() == 2) {
            Iterable<ClassifiedCodeSequence> l1 = lists.get(0);
            Iterable<ClassifiedCodeSequence> l2 = lists.get(1);

            for (ClassifiedCodeSequence c1 : l1) {
                for (ClassifiedCodeSequence c2 : l2) {
                    if (c1.compareTo(c2) == 0) {
                        if (!toReturn.contains(c1)) {
                            toReturn.add(c1);
                        }
                    }
                }
            }
        }

        return toReturn;
    }

    // this gives comparison functionality by comparing two ArrayLists
    public static ArrayList<String> compare(final ArrayList<ArrayList<String>> seq) {
        ArrayList<String> toReturn = new ArrayList<String>();
        if (seq.size() == 2) {
            ArrayList<String> l1 = seq.get(0);
            ArrayList<String> l2 = seq.get(1);
            for (String code1 : l1) {
                for (String code2 : l2) {
                    if (code1.equals(code2)) {
                        if (!toReturn.contains(code1)) {
                            toReturn.add(code1);
                        }
                    }
                }
            }
        }
        return toReturn;
    }

    public static Optional<String> standard(final IntList code, final int count) {
    	final Either<InvalidCodeSequence, ClassifiedCodeSequence> codeSeq = ClassifiedCodeSequence.create(code);
    	if (codeSeq.isRight()) {
    		return Optional.of(standard(codeSeq.get(), count));
    	}
		return Optional.empty();
    }


    // this gives a neat string of the code with information about it
    public static String standard(final ClassifiedCodeSequence code, final int count) {

    	final CodeType type = code.codeType;

        String countStr = count + "";
        if (count < 10) {
            countStr += " ";
        }
        String codeStr = " - " + type;
        if (codeStr.equals(" - CS")) {
            codeStr += "  ";
        } else if (!codeStr.equals(" - OSNO")) {
            codeStr += " ";
        }
        final String codeString = codeStr + " (" + code.codeLength + ", " + code.codeSum + ") " + code;

        return countStr + codeString;
    }

    /**
     * Zhao Yu Li, May 28, 2025.
     * Gets a code string of STORAGE that is formatted nicely
     * @param storage The Storage instance to get code string for.
     * @return The nicely formatted code string of STORAGE.
     */
    public static String getCoverCodeString(Storage storage) {
        if (storage == null) return "";

        String codeStr = "" + storage.codeType();

        if (codeStr.equals("CS")) {
            codeStr += "  ";
        } else if (!codeStr.equals("OSNO")) {
            codeStr += " ";
        }

        return codeStr + " (" + storage.codeLength() + ", " + storage.codeSum() + ") " + storage;
    }

    /**
     * Zhao Yu Li, May 28, 2025.
     * Gets a code string of STORAGE that is formatted nicely
     * @param classifiedCodeSequence The Storage instance to get code string for.
     * @return The nicely formatted code string of STORAGE.
     */
    public static String getCoverCodeString(ClassifiedCodeSequence classifiedCodeSequence) {
        if (classifiedCodeSequence == null) return "";

        String codeStr = "" + classifiedCodeSequence.codeType;

        if (codeStr.equals("CS")) {
            codeStr += "  ";
        } else if (!codeStr.equals("OSNO")) {
            codeStr += " ";
        }

        return codeStr + " (" + classifiedCodeSequence.codeLength + ", " + classifiedCodeSequence.codeSum + ") " + classifiedCodeSequence;
    }

    public static String hex(final Color color) {
        final long rd = Math.round(color.getRed() * 255);
        final long gr = Math.round(color.getGreen() * 255);
        final long bl = Math.round(color.getBlue() * 255);

        final String hex = String.format("%02x%02x%02x", rd, gr, bl);

        return "-fx-base: #" + hex;
    }

    public static void colorButton(final Button button, final Color color, final Color clicked) {
        button.setStyle(hex(color));
        button.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> button.setStyle(hex(clicked)));
        button.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> button.setStyle(hex(color)));
    }

    public static Tooltip toolTip(final String text) {
        final Tooltip tip = new Tooltip(text);
        tip.setPrefWidth(300);
        tip.setWrapText(true);

        return tip;
    }

    public static String trimCodeLine(String line) {

        // strip of comment lines
        line = line.split("#")[0];
        line = line.split("//")[0];

        if (line.contains("-")) {
            if (line.startsWith("-")) line = line.split("-")[2];
            else line = line.split("-")[1];
        }

        // Remove all the stuff from the other file format
        if (line.contains(")")) {
            line = line.split(Pattern.quote(")"))[1];
            line = line.split("O")[0];
            line = line.split("E")[0];
        }

        return line.trim();
    }

    public static String tripleTrimmer(String line) {
    	if (line.contains(",") && !line.contains(")")) {
    		return line.split("#")[0].trim();
    	}
    	else {
    		return trimCodeLine(line);
    	}
    }

    public static String timeConvert(long millis) {
    	return String.format("%02d:%02d:%02d",
    			TimeUnit.MILLISECONDS.toHours(millis),
    			TimeUnit.MILLISECONDS.toMinutes(millis) -
    			TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
    			TimeUnit.MILLISECONDS.toSeconds(millis) -
    			TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    /*
     * Runs the specified {@link Runnable} on the
     * JavaFX application thread and waits for completion.
     *
     * @param action the {@link Runnable} to run
     * @throws NullPointerException if {@code action} is {@code null}
     *
     * from  http://news.kynosarges.org/2014/05/01/simulating-platform-runandwait/
     */
    public static void runAndWait(final Runnable action) {
        if (action == null) {
            throw new NullPointerException("action");
        }

        // run synchronously on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        // queue on JavaFX thread and wait for completion
        final CountDownLatch doneLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                doneLatch.countDown();
            }
        });

        try {
            doneLatch.await();
        } catch (final InterruptedException e) {
            // ignore exception
        }
    }

    public static int modN(int value, final int n) {
        while (value >= n) {
            value -= n;
        }
        while (value < 0) {
            value += n;
        }
        return value;
    }


    // we break into a the private field of tool tip behaviour and change it around
    // based on a post on https://coderanch.com/t/622070/java/control-Tooltip-visible-time-duration
    public static void setupCustomTooltipBehavior(final int openDelayInMillis, final int visibleDurationInMillis,
                                                  final int closeDelayInMillis) {
        Tooltip tooltip = new Tooltip("My tooltip");
        tooltip.setShowDelay(Duration.millis(500));      // Wait 0.5s before showing
        tooltip.setHideDelay(Duration.millis(200));      // Wait 0.2s before hiding
        tooltip.setShowDuration(Duration.seconds(5));  
        
        // try {

        //     Class<?> TTBehaviourClass = null;
        //     final Class<?>[] declaredClasses = Tooltip.class.getDeclaredClasses();
        //     for (final Class<?> c : declaredClasses) {
        //         if (c.getCanonicalName().equals("javafx.scene.control.Tooltip.TooltipBehavior")) {
        //             TTBehaviourClass = c;
        //             break;
        //         }
        //     }

        //     if (TTBehaviourClass == null) {
        //         return;
        //     }
        //     final Constructor<?> constructor = TTBehaviourClass.getDeclaredConstructor(
        //         Duration.class, Duration.class, Duration.class, boolean.class);
        //     if (constructor == null) {
        //         return;
        //     }
        //     constructor.setAccessible(true);
        //     final Object newTTBehaviour = constructor.newInstance(
        //         new Duration(openDelayInMillis), new Duration(visibleDurationInMillis),
        //         new Duration(closeDelayInMillis), false);
        //     if (newTTBehaviour == null) {
        //         return;
        //     }
        //     final Field ttbehaviourField = Tooltip.class.getDeclaredField("BEHAVIOR");
        //     if (ttbehaviourField == null) {
        //         return;
        //     }

        //     ttbehaviourField.setAccessible(true);
        //     ttbehaviourField.set(Tooltip.class, newTTBehaviour);

        // } catch (final Exception e) {
        //     throw new RuntimeException(e);
        // }
    }


    public static boolean verifyInfo(InfoAll infoAll, Storage storage) {

        String pointsStr = storage.points;
        // TODO George, fix copy
        String sin = infoAll.leftRights;
        String cos = infoAll.codeSeqLR;
        //String sin = infoAll.sinEquations;
        //String cos = infoAll.cosEquations;
        //double epsilon = 1E-8;
        double epsilon = 1E-10;

        // parse
        ImmutableList<Vector2> points = Database.parsePoints(pointsStr);
        final String[] sinEquations = StringUtils.split(sin, '\n');
        final String[] cosEquations = StringUtils.split(cos, '\n');
        //
        for (Vector2 point : points) {
            //sin
            for (final String sinEquation : sinEquations) {
                final String[] stringCoeffs = StringUtils.split(sinEquation, ' ');
                double sum = 0;
                final double[] coeffs = new double[3];
                for (int i = 0; i < stringCoeffs.length; ++i) {
                    coeffs[i % 3] = Double.parseDouble(stringCoeffs[i]);
                    if (i % 3 == 2) {
                        double temp = coeffs[0] * Math.sin(coeffs[1] * point.x + coeffs[2] * point.y);
                        sum += temp;
                    }
                }
                //System.out.println(storage.classCodeSeq + " sin : "+ coeffs[0] +" "+ coeffs[1] +" "+ coeffs[2]);

                //System.out.println(storage.classCodeSeq + " sin : " + Double.toString(sum));

                if (sum < 0 && Math.abs(sum) > epsilon) {
                    return false;
                }

            }
            //cos
            for (final String cosEquation : cosEquations) {
                final String[] stringCoeffs = StringUtils.split(cosEquation, ' ');
                double sum = 0;
                final double[] coeffs = new double[3];
                for (int i = 0; i < stringCoeffs.length; ++i) {
                    coeffs[i % 3] = Double.parseDouble(stringCoeffs[i]);
                    if (i % 3 == 2) {
                        double temp = coeffs[0] * Math.cos(coeffs[1] * point.x + coeffs[2] * point.y);
                        sum += temp;
                    }
                }
                //System.out.println(storage.classCodeSeq + " cos : " + Double.toString(sum));
                if (sum < 0 && Math.abs(sum) > epsilon) {
                    return false;
                }
            }
        }
        return true;
    }
    // this function is from https://stackoverflow.com/a/41434490
    static private String convertDecimalToFraction(double x){
        if (x < 0){
            return "-" + convertDecimalToFraction(-x);
        }
        double tolerance = 1.0E-6;
        double h1=1; double h2=0;
        double k1=0; double k2=1;
        double b = x;
        do {
            double a = Math.floor(b);
            double aux = h1; h1 = a*h1+h2; h2 = aux;
            aux = k1; k1 = a*k1+k2; k2 = aux;
            b = 1/(b-a);
        } while (Math.abs(x-h1/k1) > x*tolerance);

        return h1+"/"+k1;
    }//end

    public static void calculate_formula(String sin, String cos, double radius,Vector2 center){
        double lowerBound = 1000;
        String sin_copy = sin;
        String cos_copy = cos;
        if (!sin_copy.equals("")){
            sin_copy=sin_copy.replace("\n","\nsin ");
            sin_copy="sin "+sin_copy;
        }
        if (!cos_copy.equals("")){
            cos_copy=cos_copy.replace("\n","\ncos ");
            cos_copy="cos "+cos_copy;
        }
        double degreex = Math.toDegrees(center.x);
        double degreey = Math.toDegrees(center.y);
        Vector2 degreeXY = Vector2.create(degreex,degreey);
        //Vector2 rationalXY =Vector2.create(degreex/90,degreey/90);
        String rationalX = convertDecimalToFraction(degreex/90);
        String rationalY = convertDecimalToFraction(degreey/90);

        System.out.println("the center(in degree) is "+ degreeXY);
        System.out.println("the center(in rational) is "+ "("+rationalX+ " , " + rationalY + ")");
        final ImmutableList<Equation> allSin = Database.parseEquations(sin_copy);
        final ImmutableList<Equation> allCos = Database.parseEquations(cos_copy);
        final String[] sinEquations = StringUtils.split(sin, '\n');
        final String[] cosEquations = StringUtils.split(cos, '\n');
        System.out.println("the center(in radian) is "+ center);
        double degreeRadius = Math.toDegrees(radius);
        System.out.println("the radius(in degree) is "+ degreeRadius);
        String rationalR = convertDecimalToFraction(degreeRadius/90);

        System.out.println("the radius(in rational) is "+ rationalR);
        System.out.println("the radius(in radian) is "+ radius);

        for (int j = 0; j< sinEquations.length ; j++) {
            String sinEquation =sinEquations[j];
            int bound = (int)allSin.get(j).bound;
            final String[] stringCoeffs = StringUtils.split(sinEquation, ' ');
            double sum = 0;
            final double[] coeffs = new double[3];
            for (int i = 0; i < stringCoeffs.length; ++i) {
                coeffs[i % 3] = Double.parseDouble(stringCoeffs[i]);
                if (i % 3 == 2) {
                    double temp = coeffs[0] * Math.sin(coeffs[1] * center.x + coeffs[2] * center.y);
                    sum += temp;
                }
            }
            double temp=radius*bound;
            sum-=temp;
            if (sum < lowerBound){
                lowerBound = sum;
            }
            // comment below out to turn off the result part 1/2
            //start
            if (sum < 0){
                System.out.println("## result"+" is "+sum+" for "+allSin.get(j).toString());
            }
            else{
                System.out.println("result"+" is "+sum+" for "+allSin.get(j).toString());
            }
            //end

        }
        //cos
        for (int j = 0; j< cosEquations.length ; j++) {
            int bound = (int)allCos.get(j).bound;
            String cosEquation = cosEquations[j];
            final String[] stringCoeffs = StringUtils.split(cosEquation, ' ');
            double sum = 0;
            final double[] coeffs = new double[3];
            for (int i = 0; i < stringCoeffs.length; ++i) {
                coeffs[i % 3] = Double.parseDouble(stringCoeffs[i]);
                if (i % 3 == 2) {
                    double temp = coeffs[0] * Math.cos(coeffs[1] * center.x + coeffs[2] * center.y);
                    sum += temp;
                }
            }
            double temp=radius*bound;
            sum-=temp;
            if (sum < lowerBound){
                lowerBound = sum;
            }
            // comment below out to turn off the result part 2/2
            //start
            if (sum < 0){
                System.out.println("##result"+" is "+sum+" for "+allCos.get(j).toString());
            }
            else{
                System.out.println("result"+" is "+sum+" for "+allCos.get(j).toString());
            }
            //end
        }
        System.out.println("Lower bound is "+ lowerBound);
    }

    //TODO xiu
    public static void calculate_formula_mrr(String equation, double radius,Vector2 center){
        System.out.println("********\nFor the mrr result:");
        double lowerBound = 1000;
        StringBuilder sin= new StringBuilder();
        StringBuilder cos= new StringBuilder();
        final String[] equations = equation.split("\n");
        for (String s : equations){
            if (s.contains("sin")){
                String b=s.replace("sin ", "");
                sin.append(b);
                sin.append("\n");
            }
            else{
                String a=s.replace("cos ", "");
                cos.append(a);
                cos.append("\n");
            }
        }
        String sin_copy = sin.toString().trim();
        String cos_copy = cos.toString().trim();

        if (!sin_copy.equals("")){
            sin_copy=sin_copy.replace("\n","\nsin ");
            sin_copy="sin "+sin_copy;
        }
        if (!cos_copy.equals("")){
            cos_copy=cos_copy.replace("\n","\ncos ");
            cos_copy="cos "+cos_copy;
        }
        final ImmutableList<Equation> allSin = Database.parseEquations(sin_copy);
        final ImmutableList<Equation> allCos = Database.parseEquations(cos_copy);

        final String[] sinEquations = StringUtils.split(sin.toString(), '\n');
        final String[] cosEquations = StringUtils.split(cos.toString(), '\n');


        for (int j = 0; j< sinEquations.length ; j++) {
            String sinEquation =sinEquations[j];
            int bound = (int)allSin.get(j).bound;
            final String[] stringCoeffs = StringUtils.split(sinEquation, ' ');
            double sum = 0;
            final double[] coeffs = new double[3];
            for (int i = 0; i < stringCoeffs.length; ++i) {
                coeffs[i % 3] = Double.parseDouble(stringCoeffs[i]);
                if (i % 3 == 2) {
                    double temp = coeffs[0] * Math.sin(coeffs[1] * center.x + coeffs[2] * center.y);
                    sum += temp;
                }
            }
            double temp=radius*bound;
            sum-=temp;
            if (sum < lowerBound){
                lowerBound = sum;
            }
            // comment below out to turn off the result part 1/2
            //start
            if (sum < 0){
                System.out.println("## result"+" is "+sum+" for "+allSin.get(j).toString());
            }
            else{
                System.out.println("result"+" is "+sum+" for "+allSin.get(j).toString());
            }
            //end
        }
        //cos
        for (int j = 0; j< cosEquations.length ; j++) {
            int bound = (int)allCos.get(j).bound;
            String cosEquation = cosEquations[j];
            final String[] stringCoeffs = StringUtils.split(cosEquation, ' ');
            double sum = 0;
            final double[] coeffs = new double[3];
            for (int i = 0; i < stringCoeffs.length; ++i) {
                coeffs[i % 3] = Double.parseDouble(stringCoeffs[i]);
                if (i % 3 == 2) {
                    double temp = coeffs[0] * Math.cos(coeffs[1] * center.x + coeffs[2] * center.y);
                    sum += temp;
                }
            }
            double temp=radius*bound;
            sum-=temp;
            if (sum < lowerBound){
                lowerBound = sum;
            }
            // comment below out to turn off the result part 2/2
            //start
            if (sum < 0){
                System.out.println("##result"+" is "+sum+" for "+allCos.get(j).toString());
            }
            else{
                System.out.println("result"+" is "+sum+" for "+allCos.get(j).toString());
            }
            //end
        }
        System.out.println("Lower bound is "+ lowerBound);
        System.out.println("#######");

    }
    public static boolean verifyVector(InfoAll infoAll, Storage storage) {
        String pointsStr = storage.points;
        String sin = infoAll.vectorX;
        String cos = infoAll.vectorY;
        ImmutableList<Vector2> points = Database.parsePoints(pointsStr);
        final String[] sinEquations = StringUtils.split(sin, '\n');
        final String[] cosEquations = StringUtils.split(cos, '\n');

        for (Vector2 point : points){

        }

        return true;
    }
}
