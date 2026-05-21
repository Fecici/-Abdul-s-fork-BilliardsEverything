package billiards.cover;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.CodeSequence;
import billiards.geometry.ConvexPolygon;
import billiards.geometry.Rectangle;
import billiards.geometry.Vector2;
import billiards.viewer.Utils;

import com.google.common.base.Splitter;

import javaslang.Tuple;
import javaslang.Tuple2;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.fraction.BigFraction;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class CoverStuff {

    private static double rationalToRadians(final String rat) {
        if (rat.indexOf('/') == -1) {
            // Does not contain /, so an integer
            final BigInteger num = new BigInteger(rat.trim());
            return num.doubleValue() * Math.PI / 2.0;
        } else {
            final String[] nums = StringUtils.split(rat, '/');
            final BigInteger numer = new BigInteger(nums[0].trim());
            final BigInteger denom = new BigInteger(nums[1].trim());

            final BigFraction fraction = new BigFraction(numer, denom);

            return fraction.doubleValue() * Math.PI / 2.0;
        }
    }

    public static ConvexPolygon parsePolygon(final String string) {

        final MutableList<Vector2> vertices = new FastList<>();

        final String[] lines = StringUtils.split(string, '\n');

        for (final String line : lines) {
            final String[] coords = StringUtils.split(line, ' ');

            final double x = rationalToRadians(coords[0]);
            final double y = rationalToRadians(coords[1]);

            final Vector2 vertex = Vector2.create(x, y);
            vertices.add(vertex);
        }

        return ConvexPolygon.create(vertices.toImmutable());
    }

    public static Rectangle parseRectangle(final String string) {

        final String[] coords = StringUtils.split(string, ' ');

        final double xMin = rationalToRadians(coords[0]);
        final double xMax = rationalToRadians(coords[1]);

        final double yMin = rationalToRadians(coords[2]);
        final double yMax = rationalToRadians(coords[3]);

        return Rectangle.create(xMin, xMax, yMin, yMax);
    }

    public static List<ClassifiedCodeSequence> parseStables(final String string) {

        final MutableList<ClassifiedCodeSequence> stables = new FastList<>();

        final String[] lines = StringUtils.split(string, '\n');

        for (int i = 0; i < lines.length; ++i) {

            final String line = lines[i].replace(":", ",");

            final String[] comps = StringUtils.split(line, ',');

            final int fileIndex = Integer.parseInt(comps[0].trim());

            if (fileIndex != i) {
                throw new RuntimeException("mismatched indices: " + fileIndex + ", " + i);
            }

            final String stableString = comps[1].trim();

            final IntList stableList = Utils.splitString(stableString).get();

            final ClassifiedCodeSequence stable = ClassifiedCodeSequence.create(stableList).get();
            stables.add(stable);
        }

        return stables;
    }

    public static List<Triple> parseTriples(final String string) {

        final MutableList<Triple> triples = new FastList<>();

        final String[] lines = StringUtils.split(string, '\n');

        for (int i = 0; i < lines.length; ++i) {

            final String line = lines[i].replace(":", ",").replace(";", ",");

            final String[] comps = StringUtils.split(line, ',');

            final int fileIndex = Integer.parseInt(comps[0].trim());

            if (fileIndex != i) {
                throw new RuntimeException("mismatched indices: " + fileIndex + ", " + i);
            }

            final String stableNegString = comps[1].trim();
            final String unstableString = comps[3].trim();
            final String stablePosString = comps[5].trim();

            final IntList stableNegList = Utils.splitString(stableNegString).get();
            final IntList unstableList = Utils.splitString(unstableString).get();
            final IntList stablePosList = Utils.splitString(stablePosString).get();

            final ClassifiedCodeSequence stableNeg = ClassifiedCodeSequence.create(stableNegList).get();
            final ClassifiedCodeSequence unstable = ClassifiedCodeSequence.create(unstableList).get();
            final ClassifiedCodeSequence stablePos = ClassifiedCodeSequence.create(stablePosList).get();

            final Triple triple = new Triple(stableNeg, unstable, stablePos);

            triples.add(triple);
        }

        return triples;
    }

    public static List<HalfTriple> parseHalfTriples(final String string) {

        final MutableList<HalfTriple> half_triples = new FastList<>();

        final String[] lines = StringUtils.split(string, '\n');

        for (int i = 0; i < lines.length; ++i) {

            final String line = lines[i].replace(":", ",").replace(";", ",");

            final String[] comps = StringUtils.split(line, ',');

            final int fileIndex = Integer.parseInt(comps[0].trim());

            if (fileIndex != i) {
                throw new RuntimeException("mismatched indices: " + fileIndex + ", " + i);
            }

            final String stableNegString = comps[1].trim();
            final String unstableString = comps[3].trim();

            final IntList stableNegList = Utils.splitString(stableNegString).get();
            final IntList unstableList = Utils.splitString(unstableString).get();

            final ClassifiedCodeSequence stableNeg = ClassifiedCodeSequence.create(stableNegList).get();
            final ClassifiedCodeSequence unstable = ClassifiedCodeSequence.create(unstableList).get();

            final HalfTriple half_triple = new HalfTriple(stableNeg, unstable);

            half_triples.add(half_triple);
        }

        return half_triples;
    }

    private static Rectangle[] subdivide(final Rectangle rect) {

        // Split the rect into four quarters

        final Vector2 center = rect.center();

        final Rectangle upperLeft = Rectangle.create(rect.intervalX.min, center.x, center.y, rect.intervalY.max);

        final Rectangle upperRight = Rectangle.create(center.x, rect.intervalX.max, center.y, rect.intervalY.max);

        final Rectangle lowerLeft = Rectangle.create(rect.intervalX.min, center.x, rect.intervalY.min, center.y);

        final Rectangle lowerRight = Rectangle.create(center.x, rect.intervalX.max, rect.intervalY.min, center.y);

        // This is UL, UR, LL, LR
        return new Rectangle[] {upperLeft, upperRight, lowerLeft, lowerRight};
    }

    /*
    private static Cover parseCover(final Iterator<String> tokens) {

        final String token = tokens.next();

        if (token.equals("E")) {
            return new Cover.Empty();
        } else if (token.equals("U")) {
            return new Cover.Uncovered();
        } else if (token.equals("S")) {
            final String str = tokens.next();
            final int index = Integer.parseInt(str);
            return new Cover.Stable(index);

        } else if (token.equals("T")) {
            final String str = tokens.next();
            final int index = Integer.parseInt(str);

            return new Cover.Triple(index);

        } else if (token.equals("D")) {

            final Cover cover0 = parseCover(tokens);
            final Cover cover1 = parseCover(tokens);
            final Cover cover2 = parseCover(tokens);
            final Cover cover3 = parseCover(tokens);

            return new Cover.Divide(cover0, cover1, cover2, cover3);

        } else {
            throw new RuntimeException("unknown cover token: " + token);
        }
    }
    */

    private static void parseCover(final Iterator<String> tokens, final Rectangle square, final List<ClassifiedCodeSequence> stables, final List<Triple> triples, final Map<Rectangle, ClassifiedCodeSequence> stableCover, final Map<Rectangle, Triple> tripleCover) {

        final String token = tokens.next();

        if (token.equals("E")) {
            // Nothing to do
        } else if (token.equals("H")) {
            // Also do nothing
        } else if (token.equals("S")) {
            final String str = tokens.next();
            final int index = Integer.parseInt(str);

            final ClassifiedCodeSequence stable = stables.get(index);
            stableCover.put(square, stable);
        } else if (token.equals("T")) {
            final String str = tokens.next();
            final int index = Integer.parseInt(str);

            final Triple triple = triples.get(index);
            tripleCover.put(square, triple);
        }
         else if (token.equals("D")) {

            final Rectangle[] quarters = subdivide(square);

            for (final Rectangle quarter : quarters) {
                parseCover(tokens, quarter, stables, triples, stableCover, tripleCover);
            }

        } else {
            throw new RuntimeException("unknown cover token: " + token);
        }
    }

    public static Tuple2<MutableMap<Rectangle, ClassifiedCodeSequence>, MutableMap<Rectangle, Triple>> parseCover(final String coverString, final Rectangle square, final List<ClassifiedCodeSequence> stables, final List<Triple> triples) {
        final Iterator<String> tokens = Splitter.on(' ').split(coverString).iterator();

        final MutableMap<Rectangle, ClassifiedCodeSequence> stableCover = new UnifiedMap<>();
        final MutableMap<Rectangle, Triple> tripleCover = new UnifiedMap<>();

        parseCover(tokens, square, stables, triples,stableCover, tripleCover);

        if (tokens.hasNext()) {
            throw new RuntimeException("unused tokens when parsing a cover");
        }

        return Tuple.of(stableCover, tripleCover);
    }
    public static Rectangle calculateRectangle(double x, double y, int magnification) {
        double initXMin = rationalToRadians("0");
        double initXMax = rationalToRadians("1");
        double initYMin = rationalToRadians("0");
        double initYMax = rationalToRadians("1");
        Rectangle square = Rectangle.create(initXMin, initXMax, initYMin, initYMax);
        while (magnification > 0) {
            square = calculateRectangle(square, x, y);
            magnification -= 1;
        }
        square.setTrimable(false);
        return square;
    }
    private static Rectangle calculateRectangle(Rectangle square, double x, double y) {
        Rectangle[] subdivides = subdivide(square);
        for (Rectangle subdivide : subdivides) {
            if (subdivide.contains(x, y)) {
                return subdivide;
            }
        }
        return square;
    }
    /*
    public static Cover parseCover(final String coverString) {

        final Iterator<String> tokens = Splitter.on(' ').split(coverString).iterator();

        final Cover cover = parseCover(tokens);

        if (tokens.hasNext()) {
            throw new RuntimeException("unused tokens when parsing a cover");
        }

        return cover;
    }
    */
}
