package billiards.database;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.Storage;
import billiards.geometry.ConvexPolygon;
import billiards.geometry.LineSegment;
import billiards.geometry.Vector2;
import billiards.math.CosEquation;
import billiards.math.Equation;
import billiards.math.LinCom;
import billiards.math.SinEquation;
import billiards.math.XYEta;
import billiards.math.XYPi;
import billiards.math.XYZ;
import billiards.wrapper.ConnectionPool;
import billiards.wrapper.Wrapper;

import javafx.scene.control.Alert;
import javaslang.Tuple;
import javaslang.Tuple2;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class Database {

    // At some point this will be moved into the CodeSequence class (I think)
    // TODO merge this with the pi one below
    public static LinCom<XYEta> findConstraintEta(final IntList codeNumbers, final XYZ first, final XYZ second) {

        if (codeNumbers.size() % 2 != 0) {
            // odd codes are stable, so 0
            return new LinCom<>(0, 0, 0);
        }

        // even indices are add
        // odd indices are sub

        final LinCom<XYZ> constraint = new LinCom<>(0, 0, 0);
        constraint.add(codeNumbers.get(0), first);
        constraint.sub(codeNumbers.get(1), second);

        XYZ prevPrev = first;
        XYZ prev = second;

        for (int i = 2; i < codeNumbers.size(); ++i) {
            final int prevNumber = codeNumbers.get(i - 1);

            final XYZ current;
            if (prevNumber % 2 == 0) {
                current = prevPrev;
            } else {
                current = XYZ.otherAngle(prevPrev, prev);
            }

            final int currentNumber = codeNumbers.get(i);

            if (i % 2 == 0) {
                // even index, so add
                constraint.add(currentNumber, current);
            } else {
                // odd index, so sub
                constraint.sub(currentNumber, current);
            }

            // Update prev_prev and prev for the next loop iteration
            // Be very careful about the order you do this
            prevPrev = prev;
            prev = current;
        }

        // z = 2*eta - x - y

        final int xCoeff = constraint.coeff(XYZ.X) - constraint.coeff(XYZ.Z);
        final int yCoeff = constraint.coeff(XYZ.Y) - constraint.coeff(XYZ.Z);
        final int etaCoeff = 2 * constraint.coeff(XYZ.Z);

        final LinCom<XYEta> etaConstraint = new LinCom<>(xCoeff, yCoeff, etaCoeff);
        etaConstraint.divideContent();
        etaConstraint.normalizeUnit();

        return etaConstraint;
    }

    // At some point this will be moved into the CodeSequence class (I think)
    public static LinCom<XYPi> findConstraint(final IntList codeNumbers, final XYZ first, final XYZ second) {

        if (codeNumbers.size() % 2 != 0) {
            // odd codes are stable, so 0
            return new LinCom<>(0, 0, 0);
        }

        // even indices are add
        // odd indices are sub

        final LinCom<XYZ> constraint = new LinCom<>(0, 0, 0);
        constraint.add(codeNumbers.get(0), first);
        constraint.sub(codeNumbers.get(1), second);

        XYZ prevPrev = first;
        XYZ prev = second;

        for (int i = 2; i < codeNumbers.size(); ++i) {
            final int prevNumber = codeNumbers.get(i - 1);

            final XYZ current;
            if (prevNumber % 2 == 0) {
                current = prevPrev;
            } else {
                current = XYZ.otherAngle(prevPrev, prev);
            }

            final int currentNumber = codeNumbers.get(i);

            if (i % 2 == 0) {
                // even index, so add
                constraint.add(currentNumber, current);
            } else {
                // odd index, so sub
                constraint.sub(currentNumber, current);
            }

            // Update prev_prev and prev for the next loop iteration
            // Be very careful about the order you do this
            prevPrev = prev;
            prev = current;
        }

        final int xCoeff = constraint.coeff(XYZ.X) - constraint.coeff(XYZ.Z);
        final int yCoeff = constraint.coeff(XYZ.Y) - constraint.coeff(XYZ.Z);
        final int piCoeff = constraint.coeff(XYZ.Z);

        final LinCom<XYPi> piConstraint = new LinCom<>(xCoeff, yCoeff, piCoeff);
        piConstraint.divideContent();
        piConstraint.normalizeUnit();

        return piConstraint;
    }

    public static Tuple2<XYZ, XYZ> parseInitialAngles(final String string) {
        switch (string) {
        case "xy":
            return Tuple.of(XYZ.X, XYZ.Y);
        case "xz":
            return Tuple.of(XYZ.X, XYZ.Z);
        case "yx":
            return Tuple.of(XYZ.Y, XYZ.X);
        case "yz":
            return Tuple.of(XYZ.Y, XYZ.Z);
        case "zx":
            return Tuple.of(XYZ.Z, XYZ.X);
        case "zy":
            return Tuple.of(XYZ.Z, XYZ.Y);
        default:
            throw new RuntimeException("unable to parse initial angles " + string);
        }
    }

    public static ImmutableList<Vector2> parsePoints(final String string) {

        final String[] strings = StringUtils.split(string, '\n');

        final MutableList<Vector2> points = new FastList<>();

        for (final String str : strings) {
            final String[] split = StringUtils.split(str, ' ');

            final double x = Double.parseDouble(split[0]);
            final double y = Double.parseDouble(split[1]);

            final Vector2 point = Vector2.create(x, y);
            points.add(point);
        }

        return points.toImmutable();
    }

    public static ImmutableList<Equation> parseEquations(final String string) {

        final String[] strings = StringUtils.split(string, '\n');

        final MutableList<Equation> equations = new FastList<>();

        for (final String str : strings) {

            final String[] stringCoeffs = StringUtils.split(str, ' ');

            final String trig = stringCoeffs[0];

            final int[] coeffs = new int[stringCoeffs.length - 1];
            for (int i = 1; i < stringCoeffs.length; ++i) {
                final String stringCoeff = stringCoeffs[i];
                coeffs[i - 1] = Integer.parseInt(stringCoeff);
            }

            final Equation eq;
            if (trig.equals("sin")) {
                eq = new SinEquation(coeffs);
            } else if (trig.equals("cos")) {
                eq = new CosEquation(coeffs);
            } else {
                throw new RuntimeException("unknown equation " + string);
            }

            equations.add(eq);
        }

        return equations.toImmutable();
    }

    public static ImmutableList<LeftRight> parseLeftRights(final String string) {

        final String[] strings = StringUtils.split(string, '\n');

        final MutableList<LeftRight> leftRights = new FastList<>();

        for (final String str : strings) {

            final String[] numbers = StringUtils.split(str, ' ');

            final int leftNumber = Integer.parseInt(numbers[0]);
            final int leftBranch = Integer.parseInt(numbers[1]);
            final int rightNumber = Integer.parseInt(numbers[2]);
            final int rightBranch = Integer.parseInt(numbers[3]);
            
            //prints left rights when show left rights is checked 
            //george aug 24,2019
           // System.out.print("leftNumber " + leftNumber +"\n");
           // System.out.print("leftBranch " + leftBranch +"\n");
           // System.out.print("rightNumber " + rightNumber +"\n");
           // System.out.print("rightBranch " + rightBranch +"\n");

            

            final LeftRight leftRight = new LeftRight(leftNumber, leftBranch, rightNumber, rightBranch);
            
          //prints left rights when show left rights is checked 
            //george aug 24,2019
            //System.out.print("leftRight " + leftRight +"\n"); //[(2, 0, 5, 0), (6, 0, 3, 0), (4, 0, 1, 0)]
            //[(2, 0, 5, 0), (6, 0, 3, 0), (4, 0, 1, 0)] 1 3 3
            //same 1 5 5 which means the left rights are as the previous line
            
            leftRights.add(leftRight);
        }

        return leftRights.toImmutable();
    }

    private static Storage convertToStorage(final ClassifiedCodeSequence codeSeq, final String initialAnglesStr, final String pointsStr, final String equationsStr) {

        final ImmutableList<Vector2> points = parsePoints(pointsStr);
        final ImmutableList<Equation> equations = parseEquations(equationsStr);
        
        //george aug 26,2019 using 1 3 3 and the regular computation
        //System.out.print("points " + points + "\n");//points [(0.39269908169872414, 0.7853981633974483), (0.7853981633974483, 0.39269908169872414), (0.7853981633974483, 0.7853981633974483)]
      //System.out.print("equations " + equations + "\n");//equations [-cos(2y)-cos(2x)-cos(2x+2y), cos(2x-y)+cos(2x+y), cos(x-2y)+cos(x+2y)]
       //george aug 26,2019
        
        final Storage storage;

        if (codeSeq.stable) {

            final ConvexPolygon polygon = ConvexPolygon.create(points);
            storage = new Storage.Stable(codeSeq, equations, polygon, pointsStr);
          
            //george aug 24,2019  press 1 3 3 and get the coordinates in radians
          //storage prints 1 3 3 for example
            //System.out.print("polygon " + polygon + "\n");//this is mrr region coordinates
          //System.out.print("storage " + storage + "\n"); //this is 1 3 3
          //System.out.print("codeSeq " + codeSeq + "\n"); //this is 1 3 3
          //System.out.print("equations " + equations + "\n");//this is left right equations [-cos(2y)-cos(2x)-cos(2x+2y), cos(2x-y)+cos(2x+y), cos(x-2y)+cos(x+2y)]
          //System.out.print("polygon " + polygon + "\n");//this is mrr region coordinates polygon [(0.39269908169872414, 0.7853981633974483), (0.7853981633974483, 0.39269908169872414), (0.7853981633974483, 0.7853981633974483)]


        } else {

            final Tuple2<XYZ, XYZ> initialAngles = parseInitialAngles(initialAnglesStr);

            final LinCom<XYPi> constraint = findConstraint(codeSeq.codeSequence.codeNumbers,
                                                           initialAngles._1,
                                                           initialAngles._2);

            final LineSegment lineSegment = LineSegment.create(points);
            
          //george aug 24,2019 press 1 2 1 4 for example
            //System.out.print("initialAngles " + initialAngles + "\n");//zy for example
            //System.out.print("constraint " + constraint + "\n");//3x+2y-2*90 for example
            //System.out.print("lineSegment " + lineSegment + "\n");//coordinates of the two vertices

            storage = new Storage.Unstable(codeSeq, equations, constraint, lineSegment, pointsStr);
           
          //george aug 24,2019 press 1 2 1 4 for example
           // System.out.print("storage " + storage + "\n");//1 2 1 4 for example
           // System.out.print("equations " + equations + "\n");//[-cos(x+2y), cos(y)+cos(2x+y)]
        }

        return storage;
    }

    // In the future, if we were to switch to Bezier curves, we would simply
    // return the string containing the SVG path, nothing more
    // That would be uber nice
    // But right now we can't do that.
    // Wrapper serializes outer MRR calls while each native call uses the
    // configured worker pool. Cache repeated OBO/AutoVary results here, but only
    // cache Optional.empty after the backend certifies an empty region.
    private static final int CACHE_MAX_SIZE = 1000;
    private static final Map<ClassifiedCodeSequence, Optional<Storage>> STORAGE_CACHE =
            Collections.synchronizedMap(new LinkedHashMap<ClassifiedCodeSequence, Optional<Storage>>() {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<ClassifiedCodeSequence, Optional<Storage>> eldest) {
                    return size() > CACHE_MAX_SIZE;
                }
            });

    private static final ScheduledExecutorService HEARTBEAT_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                final Thread t = new Thread(r, "native-compute-heartbeat");
                t.setDaemon(true);
                return t;
            });

    public static Optional<Storage> loadStorage(final ClassifiedCodeSequence codeSeq, final ConnectionPool pool) {

        synchronized (STORAGE_CACHE) {
            if (STORAGE_CACHE.containsKey(codeSeq)) {
                return STORAGE_CACHE.get(codeSeq);
            }
        }

        // Another worker may have finished this code while this thread was
        // waiting for cache access.
        synchronized (STORAGE_CACHE) {
            if (STORAGE_CACHE.containsKey(codeSeq)) {
                return STORAGE_CACHE.get(codeSeq);
            }
        }

        final long startMs = System.currentTimeMillis();
        final ScheduledFuture<?> heartbeat = HEARTBEAT_SCHEDULER.scheduleAtFixedRate(() -> {
            final long elapsed = System.currentTimeMillis() - startMs;
            // Uncomment while diagnosing stuck native calls:
            // System.out.println("[CPP] still computing " + codeSeq + " after " + elapsed / 1000 + "s");
        }, 30, 30, TimeUnit.SECONDS);

        try {
            final Optional<Picture> opt = Wrapper.loadPicture(codeSeq, pool);

            if (!opt.isPresent()) {
                synchronized (STORAGE_CACHE) {
                    STORAGE_CACHE.put(codeSeq, Optional.empty());
                }
                return Optional.empty();
            }

            final Picture picture = opt.get();

            final Storage storage = convertToStorage(codeSeq, picture.initialAngles, picture.points, picture.equations);

            synchronized (STORAGE_CACHE) {
                STORAGE_CACHE.put(codeSeq, Optional.of(storage));
            }

            return Optional.of(storage);
        } finally {
            heartbeat.cancel(false);
        }
    }

    public static Optional<Tuple2<Storage, ImmutableList<LeftRight>>> loadStorageShowLR(final ClassifiedCodeSequence codeSeq, final ConnectionPool pool) {

        final Optional<Info> opt = Wrapper.loadInfo(codeSeq, pool);

        if (!opt.isPresent()) {
            return Optional.empty();
        }

        final Info info = opt.get();

        final Storage storage = convertToStorage(codeSeq, info.initialAngles, info.points, info.equations);
  
        //george aug 24,2019 this is printed when iterations pressed under show left right
        //System.out.print("storage " + storage + "\n");
        //System.out.print("info.equations " + info.equations + "\n");

        final ImmutableList<LeftRight> leftRights = parseLeftRights(info.leftRights);
        
        //george aug 24,2019 this is printed when iterations pressed under show left right
        //System.out.print("leftRights " + leftRights +"\n");

        return Optional.of(Tuple.of(storage, leftRights));
    }

    public static Optional<Storage> loadStorageUseLR(final String lr,final ClassifiedCodeSequence baseCodeSeq, final ClassifiedCodeSequence codeSeq, final ConnectionPool pool) {
        final Optional<Picture> opt;

        opt = Wrapper.loadPictureLR(baseCodeSeq, codeSeq, pool,lr);
        //System.out.println(codeSeq +" "+ opt.get().points);

        if (!opt.isPresent()) {
            return Optional.empty();
        }

        final Picture picture = opt.get();

        final Storage storage = convertToStorage(codeSeq, picture.initialAngles, picture.points, picture.equations);

        return Optional.of(storage);
    }
    
    // Check if the given code sequence exists in the database
    public static boolean exists(final ClassifiedCodeSequence codeSeq, final String dbName) {

        final String sql = String.format("select exists(select 1 from %s where code_sequence = ?) as exist;",
                                         codeSeq.codeType.toString().toLowerCase());
        
        try (final Connection conn = DriverManager.getConnection(Admin.getUrl(dbName))) {
            // With prepared statements, you don't have to automatically worry about formatting your
            // things straight into a string. Instead, you write some parameters, and it will take
            // care of it for you without going through all the hassle of writing it into a string
            final PreparedStatement statement = conn.prepareStatement(sql);

            statement.setString(1, codeSeq.toString());

            final ResultSet result = statement.executeQuery();

            // TODO is there a nicer way of dealing with this exists thing?
            Boolean exists = null;
            while (result.next()) {
                exists = result.getBoolean("exist");
            }

            return exists;
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveToDatabase(final ClassifiedCodeSequence codeSeq, final String dbName) {

    	if (!exists(codeSeq, dbName)) {
	        final String sql = String.format("insert into %s (code_sequence) values (?);",
	                                         codeSeq.codeType.toString().toLowerCase());
	
	        try (final Connection conn = DriverManager.getConnection(Admin.getUrl(dbName))) {
	
	            final PreparedStatement statement = conn.prepareStatement(sql);
	
	            statement.setString(1, codeSeq.toString());
	
	            statement.executeUpdate();
	
	        } catch (final SQLException e) {
	            throw new RuntimeException(e);
	        }
    	}
    }

    public static void deleteBaseFromDatabase(final ClassifiedCodeSequence base, final String db){};

    public static void saveTripleToDatabase(final String Triple, final String db) {
        // First, create the triple table if it does not already exist in the database
        final String createTripleTableQuery = "CREATE TABLE IF NOT EXISTS main.triple (triple text check(typeof(triple) = 'text'),primary key (triple))";

        try (Connection conn = DriverManager.getConnection(Admin.getUrl(db));
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(createTripleTableQuery);

            final String addTripleQuery = "INSERT OR IGNORE INTO main.triple (triple) VALUES (?);";
            PreparedStatement pstmt = conn.prepareStatement(addTripleQuery);
            pstmt.setString(1, Triple);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Zhao Yu Li, May 23, 2025.
     * A function to enforce a code sequence matches with its odd-even pattern since SQLite does not allow the
     * implementation of a similar check constraint.
     * @param codeSeq The code sequence to match with.
     * @param OEPattern The odd-even pattern to match with.
     * @return True if the code sequence and odd-even pattern match in parity; false otherwise.
     */
    private static boolean codeAndOEMatch(final String codeSeq, final String OEPattern) {
        final String[] codeSeqArray = codeSeq.split(", ");
        final String[] OEPatternArray = OEPattern.split(",");

        if (codeSeqArray.length != OEPatternArray.length) return false;

        for (int i = 0; i < codeSeqArray.length; ++i) {
            String[] codeNumbers = codeSeqArray[i].split(" ");

            if (codeNumbers.length != OEPatternArray[i].length()) return false;

            for (int j = 0; j < codeNumbers.length; ++j) {
                // The outer index selects the comma-separated group; the inner
                // index selects the number inside that group. Using i here made
                // later digits validate against the wrong parity character.
                final int codeNumber = Integer.parseInt(codeNumbers[j].trim());
                final char expectedParity = OEPatternArray[i].charAt(j);
                if ((codeNumber % 2 == 0 && expectedParity != 'E')
                        || (codeNumber % 2 == 1 && expectedParity != 'O'))
                    return false;
            }
        }

        return true;
    }

    /**
     * Zhao Yu Li, May 23, 2025.
     * Save the 4-tuple (codeSeq, OEPattern, iterPattern, time of last use) to the database named dbName
     * @param codeSeq The code sequence to save.
     * @param OEPattern The odd-even pattern of codeSeq.
     * @param iterPattern The iteration pattern for codeSeq.
     * @param dbName The name of the database to save to.
     */
    public static void saveIterationPatternToDatabase(final String codeSeq, final String OEPattern, final String iterPattern, final String dbName) {
        if (!codeAndOEMatch(codeSeq, OEPattern)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Incorrect Code Sequence or Odd-Even Pattern");
            alert.setContentText("The code sequence and odd-even pattern must match in length and in parity.");
            alert.showAndWait();
            return;
        }

        // First, create the iteration patterns table if it does not already exist in the database
        final String createIterPatQuery = "CREATE TABLE IF NOT EXISTS main.iteration_pattern (code_sequence text check(typeof(code_sequence) = 'text'),oe_pattern text check(typeof(oe_pattern) = 'text'),iter_pattern text check(typeof(iter_pattern) = 'text'),last_used text check(typeof(last_used) = 'text'),primary key (code_sequence, iter_pattern))";

        try (Connection conn = DriverManager.getConnection(Admin.getUrl(dbName));
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(createIterPatQuery);

            final String addTripleQuery = "INSERT INTO main.iteration_pattern (code_sequence,oe_pattern,iter_pattern,last_used) VALUES (?,?,?,?) ON CONFLICT (code_sequence, iter_pattern) DO UPDATE SET last_used = ?;";
            PreparedStatement pstmt = conn.prepareStatement(addTripleQuery);
            pstmt.setString(1, codeSeq);
            pstmt.setString(2, OEPattern);
            pstmt.setString(3, iterPattern);

            // Setting a time of last used so we can order by most recently used first.
            // Explicitly set timezone so all entries will have the same timezone when ordering by time of last used.
            pstmt.setString(4, OffsetDateTime.now(ZoneId.of("America/Denver")).format(DateTimeFormatter.ISO_DATE_TIME));
            pstmt.setString(5, OffsetDateTime.now(ZoneId.of("America/Denver")).format(DateTimeFormatter.ISO_DATE_TIME));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static ArrayList<String> lookUpIterPatByCodeSeq(final String codeSeq, final String dbName) {
        // First, create the iteration patterns table if it does not already exist in the database
        final String createIterPatQuery = "CREATE TABLE IF NOT EXISTS main.iteration_pattern (code_sequence text check(typeof(code_sequence) = 'text'),oe_pattern text check(typeof(oe_pattern) = 'text'),iter_pattern text check(typeof(iter_pattern) = 'text'),last_used text check(typeof(last_used) = 'text'),primary key (code_sequence, iter_pattern))";

        try (Connection conn = DriverManager.getConnection(Admin.getUrl(dbName));
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(createIterPatQuery);

            final String selectPatternQuery = "SELECT iter_pattern FROM main.iteration_pattern WHERE code_sequence = ? ORDER BY last_used DESC;";
            PreparedStatement pstmt = conn.prepareStatement(selectPatternQuery);
            pstmt.setString(1, codeSeq);
            return getPatternsFromDB(pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static ArrayList<String> lookUpIterPatByOEPat(final String oePattern, final String dbName) {
        // First, create the iteration patterns table if it does not already exist in the database
        final String createIterPatQuery = "CREATE TABLE IF NOT EXISTS main.iteration_pattern (code_sequence text check(typeof(code_sequence) = 'text'),oe_pattern text check(typeof(oe_pattern) = 'text'),iter_pattern text check(typeof(iter_pattern) = 'text'),last_used text check(typeof(last_used) = 'text'),primary key (code_sequence, iter_pattern))";

        try (Connection conn = DriverManager.getConnection(Admin.getUrl(dbName));
             Statement stmt = conn.createStatement();) {
            stmt.executeUpdate(createIterPatQuery);

            final String selectPatternQuery = "SELECT DISTINCT iter_pattern FROM main.iteration_pattern WHERE oe_pattern = ? ORDER BY last_used DESC;";
            PreparedStatement pstmt = conn.prepareStatement(selectPatternQuery);
            pstmt.setString(1, oePattern);
            return getPatternsFromDB(pstmt);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static ArrayList<String> getPatternsFromDB(PreparedStatement pstmt) throws SQLException {
        ResultSet rs = pstmt.executeQuery();

        ArrayList<String> patterns = new ArrayList<>();

        while (rs.next()) {
            patterns.add(rs.getString("iter_pattern"));
        }

        return patterns;
    }
}
