package billiards.wrapper;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.codeseq.CodeType;
import billiards.database.Info;
import billiards.database.InfoAll;
import billiards.database.Picture;
import billiards.viewer.Utils;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

// WARNING: The JNA project was abandoned for a while before being
// resurrected on GitHub. As such, there are many obsolete websites
// and documentation on the interwebs that refer to the old JNA,
// not the new one. As such, always go to the JNA GitHub page
// https://github.com/java-native-access/jna
// to look up information and documentation.

public final class Wrapper {

    static {
        // name of the C++ library
        Native.register("backend");
    }

    private static native void sqlite_error_logging();
    private static native void database_create(final String dbPath);
    private static native void database_clear(final String dbPath);

    public static void errorLogging() {
        sqlite_error_logging();
    }

    public static void createDatabase(final String dbPath) {
        database_create(dbPath);
    }

    public static void clearDatabase(final String dbPath) {
        database_clear(dbPath);
    }

    private static native Pointer create_connection_pool(final String dbPath, final int poolSize);
    private static native void destroy_connection_pool(final Pointer dbPtr);


    public static Pointer createConnectionPool(final String dbPath, final int poolSize) {
        return create_connection_pool(dbPath, poolSize);
    }

    public static void destroyConnectionPool(final Pointer dbPtr) {
        destroy_connection_pool(dbPtr);
    }

    public static native void backend_cancel();

    private static native String cover_wrapper(String polygon, String codes, String unstables,
                                            int digits, int subdivide, int empty,
                                            boolean mrr, Pointer pool);

    private static native String small_cover_wrapper(String polygon, String codes, String unstables,
                                            int digits, int subdivide, int empty,
                                            boolean mrr, Pointer pool, boolean printInfo);

    private static native int cover_wrapper_duplicate_stables(String polygon, String codes, String unstables,
                                            int digits, int subdivide, int empty,
                                            boolean mrr, Pointer pool, boolean show);

    private static native int cover_wrapper_half_duplicate_stables(String polygon, String codes, String unstables,
                                                 int digits, int subdivide, int empty,
                                                 boolean mrr, Pointer pool);

    private static native int cover_wrapper_all(String mrr_dir, Pointer pool, int depth);

    public static String coverWrapper(final String polygon, final String codes, final String unstables,
                                       final int digits, final int subdivide, final int empty,
                                       final boolean mrr, final ConnectionPool pool) {

        return cover_wrapper(polygon, codes, unstables, digits, subdivide, empty, mrr, pool.pointer);
    }

    public static String smallCoverWrapper(final String polygon, final String codes, final String unstables,
                                       final int digits, final int subdivide, final int empty,
                                       final boolean mrr, final ConnectionPool pool, final boolean printInfo) {

       return small_cover_wrapper(polygon, codes, unstables, digits, subdivide, empty, mrr, pool.pointer, printInfo);
    }

    public static native String getNotFilledCoordinates(String polygon, String codes, String unstables,
                                                        int digits, int subdivide, int empty,
                                                        boolean mrr, Pointer pool, boolean isLastCycle);
    
    public static int coverWrapperDuplicateStables(final String polygon, final String codes, final String unstables,
                                       final int digits, final int subdivide, final int empty,
                                       final boolean mrr, final ConnectionPool pool, boolean show) {

        final int equations = cover_wrapper_duplicate_stables(polygon, codes, unstables, digits, subdivide, empty, mrr, pool.pointer, show);

        //Return numbers manually for possible error throwing
        if (equations == 0) {
            return 0;
        } else if (equations == 1) {
            return 1;
        } else if (equations == 2) {
            return 2;
        } else if (equations == -1) {
            throw new RuntimeException("calculation of cover failed");
        } else {
            throw new RuntimeException("unknown return value " + equations);
        }
    }
    
    public static boolean coverWrapperHalfDuplicateStables(final String polygon, final String codes, final String unstables,
                                           final int digits, final int subdivide, final int empty,
                                           final boolean mrr, final ConnectionPool pool) {

        final int rval = cover_wrapper_half_duplicate_stables(polygon, codes, unstables, digits, subdivide, empty, mrr, pool.pointer);

        if (rval == 0) {
            return false;
        } else if (rval == 1) {
            return true;
        } else if (rval == -1) {
            throw new RuntimeException("calculation of cover failed");
        } else {
            throw new RuntimeException("unknown return value " + rval);
        }
    }
    
    public static boolean coverWrapperAll(final String mrrDir, final ConnectionPool pool, final int depth) {

        final int rval = cover_wrapper_all(mrrDir, pool.pointer, depth);

        if (rval == 0) {
            return false;
        } else if (rval == 1) {
            return true;
        } else if (rval == -1) {
            throw new RuntimeException("calculation of all cover failed");
        } else {
            throw new RuntimeException("unknown return value " + rval);
        }
    }
    private static native int load_info_all(int[] codeNumbers, int codeNumbersLength,
                                            CInfoAll cInfoAll, Pointer poolPtr);
    private static native int load_all_equations(int[] codeNumbers, int codeNumbersLength,
                                                CInfoAll cInfoAll, Pointer poolPtr);
    public static Optional<InfoAll> loadAllEquation(final ClassifiedCodeSequence codeSeq, final ConnectionPool pool ){
        final int[] codeNumbersArray = codeSeq.codeSequence.codeNumbers.toArray();

        final int codeNumbersLen = codeNumbersArray.length;

        final CInfoAll cinfoAll = new CInfoAll();
        final int rval = load_all_equations(codeNumbersArray, codeNumbersLen, cinfoAll, pool.pointer);

        if (rval == 1) {
            try{
                final InfoAll info = new InfoAll(cinfoAll);

                // Only release the resources after we have converted the
                // cinfo to a info
                // TODO double check that this doesn't leak memory
                //cleanup_cinfo(info);

                return Optional.of(info);
            }
            catch (NullPointerException e) {
                return  Optional.empty();
            }

        } else if (rval == 0) {
            // empty set
            return Optional.empty();
        } else if (rval == -1) {
            System.err.println("Load all equation failed for " + codeSeq);
            // Empty normally means empty set, but in this case means calculation error
            return Optional.empty();
        } else {
            throw new RuntimeException("unknown return value " + rval);
        }
    }

    public static Optional<InfoAll> loadInfoAll(final ClassifiedCodeSequence codeSeq, final ConnectionPool pool ) {

        final int[] codeNumbersArray = codeSeq.codeSequence.codeNumbers.toArray();

        final int codeNumbersLen = codeNumbersArray.length;

        final CInfoAll cinfoAll = new CInfoAll();
        final int rval = load_info_all(codeNumbersArray, codeNumbersLen, cinfoAll, pool.pointer);

        if (rval == 1) {
            try{
                final InfoAll info = new InfoAll(cinfoAll);

                // Only release the resources after we have converted the
                // cinfo to a info
                // TODO double check that this doesn't leak memory
                //cleanup_cinfo(info);

                return Optional.of(info);
            }
            catch (NullPointerException e) {
                return  Optional.empty();
            }

        } else if (rval == 0) {
            // empty set
            return Optional.empty();
        } else if (rval == -1) {
            System.err.println("Load all failed for " + codeSeq);
            // Empty normally means empty set, but in this case means calculation error
            return Optional.empty();
        } else {
            throw new RuntimeException("unknown return value " + rval);
        }
    }


    private static native int delete_from_database(int[] codeNumbers, int codeNumbersLength, Pointer poolPtr);
    public static boolean deleteFromDatabase(final ClassifiedCodeSequence codeSeq, final ConnectionPool pool) {
        final int[] codeNumbersArray = codeSeq.codeSequence.codeNumbers.toArray();

        final int codeNumbersLen = codeNumbersArray.length;

        final int rval = delete_from_database(codeNumbersArray, codeNumbersLen, pool.pointer);

        if (rval == 1) {
            return true;
        } else if (rval == 0) {
            // unkown issue, this is not supposed to happen unless you changed the delete_from_database of wrapper.cpp
            return false;
        } else if (rval == -1) {
            System.err.println("Deleting " + codeSeq + " failed");
            return false;
        } else {
            throw new RuntimeException("unknown return value " + rval);
        }
    }

    private static native int save_to_database(int[] codeNumbers, int codeNumbersLength, Pointer poolPtr);
    public static boolean saveToDatabase(final ClassifiedCodeSequence codeSeq, final ConnectionPool pool) {

        final int[] codeNumbersArray = codeSeq.codeSequence.codeNumbers.toArray();

        final int codeNumbersLen = codeNumbersArray.length;

        final int rval = save_to_database(codeNumbersArray, codeNumbersLen, pool.pointer);

        if (rval == 1) {
            return true;
        } else if (rval == 0) {
            // empty set
            return false;
        } else if (rval == -1) {
            System.err.println("warning: MRR failed for " + codeSeq);
            return false;
        } else {
            throw new RuntimeException("unknown return value " + rval);
        }
    }

    // added on 16 May 2018 for the pattern finder to use
    public static boolean saveToDatabase(final int[] codeNumbersArray, final ConnectionPool pool) {

        final int codeNumbersLen = codeNumbersArray.length;

        final int rval = save_to_database(codeNumbersArray, codeNumbersLen, pool.pointer);

        if (rval == 1) {
            return true;
        } else if (rval == 0) {
            // empty set
            return false;
        } else if (rval == -1) {
            System.err.println("warning: MRR failed for " + codeNumbersArray);
            return false;
        } else {
            throw new RuntimeException("unknown return value " + rval);
        }
    }

    private static native int load_picture(int[] codeNumbers, int codeNumbersLength,
                                           CPicture cpicture, Pointer poolPtr);
    private static native void cleanup_cpicture(CPicture cpicture);

    public static Optional<Picture> loadPicture(final ClassifiedCodeSequence codeSeq, final ConnectionPool pool) {

        final int[] codeNumbersArray = codeSeq.codeSequence.codeNumbers.toArray();

        final int codeNumbersLen = codeNumbersArray.length;

        final CPicture cpicture = new CPicture();
        final int rval = load_picture(codeNumbersArray, codeNumbersLen, cpicture, pool.pointer);

        if (rval == 1) {
            final Picture picture = new Picture(cpicture);

            // Only release the resources after we have converted the
            // cpicture to a picture
            // TODO double check that this doesn't leak memory
            cleanup_cpicture(cpicture);

            return Optional.of(picture);
        } else if (rval == 0) {
            // empty set
            return Optional.empty();
        } else if (rval == -1) {
            System.err.println("warning: MRR failed for " + codeSeq);
            return Optional.empty();
        } else {
            throw new RuntimeException("unknown return value " + rval);
        }
    }

    private static native int load_picture_lr(int[] baseCodeNumbers, int baseCodeNumbersLen,
                                              int[] codeNumbers, int codeNumbersLength,
                                              CPicture cpicture, Pointer poolPtr);
    private static native int load_picture_lr_expando(int[] codeNumbers, int codeNumbersLength,
                                              CPicture cpicture, Pointer poolPtr, String lr);

    public static Optional<Picture> loadPictureLR(final ClassifiedCodeSequence baseCodeSeq, final ClassifiedCodeSequence codeSeq, final ConnectionPool pool,final String lr) {

        final int[] baseCodeNumbersArray = baseCodeSeq.codeSequence.codeNumbers.toArray();

        final int baseCodeNumbersLen = baseCodeNumbersArray.length;

        final int[] codeNumbersArray = codeSeq.codeSequence.codeNumbers.toArray();

        final int codeNumbersLen = codeNumbersArray.length;

        final CPicture cpicture = new CPicture();
        final int rval;

        if (lr == "empty") {
            rval = load_picture_lr(baseCodeNumbersArray, baseCodeNumbersLen,
                                         codeNumbersArray, codeNumbersLen,
                                         cpicture, pool.pointer);
        }
        else {
            rval = load_picture_lr_expando(codeNumbersArray, codeNumbersLen,
                cpicture, pool.pointer, lr);
        }

        if (rval == 1) {
            final Picture picture = new Picture(cpicture);

            // Only release the resources after we have converted the
            // cpicture to a picture
            // TODO double check that this doesn't leak memory
            cleanup_cpicture(cpicture);

            return Optional.of(picture);
        } else if (rval == 0) {
            // empty set
            return Optional.empty();
        } else if (rval == -1) {
            //System.err.println("warning: left rights failed for " + codeSeq);
            //System.out.println("warning: left rights failed for " + codeSeq);

            return Optional.empty();

        } else {
            throw new RuntimeException("unknown return value " + rval);
        }
    }

    private static native int load_info(int[] codeNumbers, int codeNumbersLength,
                                        CInfo cinfo, Pointer poolPtr);

    private static native void cleanup_cinfo(CInfo cinfo);

    public static Optional<Info> loadInfo(final ClassifiedCodeSequence codeSeq, final ConnectionPool pool) {

        final int[] codeNumbersArray = codeSeq.codeSequence.codeNumbers.toArray();

        final int codeNumbersLen = codeNumbersArray.length;

        final CInfo cinfo = new CInfo();
        final int rval = load_info(codeNumbersArray, codeNumbersLen, cinfo, pool.pointer);

        if (rval == 1) {
            final Info info = new Info(cinfo);

            // Only release the resources after we have converted the
            // cinfo to a info
            // TODO double check that this doesn't leak memory
            cleanup_cinfo(cinfo);

            return Optional.of(info);
        } else if (rval == 0) {
            // empty set
            return Optional.empty();
        } else if (rval == -1) {
            System.err.println("warning: MRR failed for " + codeSeq);
            // Empty normally means empty set, but in this case means calculation error
            return Optional.empty();
        } else {
            throw new RuntimeException("unknown return value " + rval);
        }
    }

    private static native int merge_covers(String mergeDir, String coverDirs, Pointer poolPtr);

    public static void mergeCovers(final String mergeDir, final List<String> coverDirs, final ConnectionPool pool) {

        final String coverDirStr = String.join("\n", coverDirs);

        System.out.println("Beginning merge");

        final int rval = merge_covers(mergeDir, coverDirStr, pool.pointer);

        if (rval == 0) {
            throw new RuntimeException("merging of covers failed");
        } else {
            System.out.println("Merge successful");
        }
    }

    private static native int code_search_length(int code_type_int, int length_int, CString cstring, Pointer poolPtr);
    private static native int code_search_even_odd(int code_type_int, String evenOdd, CString cstring, Pointer poolPtr);

    private static native void cleanup_string(CString cstring);

    public static String search(final CodeType type, final int length, final ConnectionPool pool) {

        final CString cstring = new CString();

        final int rval = code_search_length(type.ordinal(), length, cstring, pool.pointer);

        if (rval == 1) {

            final String str = cstring.string.getString(0);

            cleanup_string(cstring);

            return str;

        } else if (rval == -1) {
            throw new RuntimeException("searching failed");
        } else {
            throw new RuntimeException("unknown return value: " + rval);
        }
    }

    public static String search(final CodeType type, final String evenOdd, final ConnectionPool pool) {

        final CString cstring = new CString();

        final int rval = code_search_even_odd(type.ordinal(), evenOdd, cstring, pool.pointer);

        if (rval == 1) {

            final String str = cstring.string.getString(0);

            cleanup_string(cstring);

            return str;

        } else if (rval == -1) {
            throw new RuntimeException("searching failed");
        } else {
            throw new RuntimeException("unknown return value: " + rval);
        }
    }

    private static native int trim_cover(String polygonStr, String inDir, String outDir);

    public static void trimCover(final String polygonStr, final String inDir, final String outDir) {

        System.out.println("Beginning trim");

        final int rval = trim_cover(polygonStr, inDir, outDir);

        if (rval == 1) {
            System.out.println("Trim succeeded");
        } else {
            throw new RuntimeException("trimming failed");
        }
    }

    private static native int bounding_polygon(int[] codeNumbers, int codeNumbersLength,
                                        CString cstring, Pointer poolPtr);

    public static String boundingPolygon(final ClassifiedCodeSequence codeSeq, final ConnectionPool pool) {

        final int[] codeNumbersArray = codeSeq.codeSequence.codeNumbers.toArray();

        final int codeNumbersLen = codeNumbersArray.length;

        final CString cstring = new CString();

        final int rval = bounding_polygon(codeNumbersArray, codeNumbersLen, cstring, pool.pointer);

        if (rval == 1) {

            final String str = cstring.string.getString(0);

            cleanup_string(cstring);

            return str;
        } else {
            throw new RuntimeException("bounding polygon failed");
        }
    }

    private static native int load_slope_info(int[] codeNumbers, int codeNumbersLength, CInfoAll cinfoAll, Pointer poolPtr);
    public static Optional<InfoAll> loadSlopeInfo(ClassifiedCodeSequence codeSequence, ConnectionPool pool) {
        int[] codeNumberArray = codeSequence.codeSequence.codeNumbers.toArray();
        int codeNumberLength = codeNumberArray.length;

        CInfoAll cInfoAll = new CInfoAll();

        int rval = load_slope_info(codeNumberArray, codeNumberLength, cInfoAll, pool.pointer);
        if (rval == 1) {
            try {
                InfoAll infoAll = new InfoAll(cInfoAll);
                return  Optional.of(infoAll);
            }
            catch (NullPointerException e) {
                return Optional.empty();
            }
        }
        else if (rval == 0) {
            // empty set
            return Optional.empty();
        } else if (rval == -1) {
            System.err.println("Load vector failed for " + codeSequence);
            // Empty normally means empty set, but in this case means calculation error
            return Optional.empty();
        } else {
            throw new RuntimeException("unknown return value " + rval);
        }
    }

    private static native int calculate_gradient(String equation_str, double x, double y, boolean from_database, CString result,CString r);
    public static String calculateGradient(String euqation_str, String x_str, String y_str, boolean from_database) {
        final CString cstring = new CString();
        final CString cstring2 = new CString();

        double x = Math.toRadians(Double.parseDouble(x_str));
        double y = Math.toRadians(Double.parseDouble(y_str));
        final double rval = calculate_gradient(euqation_str, x, y, from_database, cstring,cstring2);
        if (rval != -1) {
            final String str = cstring.string.getString(0);
            cleanup_string(cstring);
            return str;
        } else {
            throw new RuntimeException("calculating gradient failed");
        }
    }
    
    public static String calculateGradient2(String euqation_str, String x_str, String y_str, boolean from_database) {
        final CString cstring = new CString();
        final CString cstring2 = new CString();

        double x = Math.toRadians(Double.parseDouble(x_str));
        double y = Math.toRadians(Double.parseDouble(y_str));
        final double rval = calculate_gradient(euqation_str, x, y, from_database, cstring,cstring2);
        if (rval != -1) {
            final String str = cstring2.string.getString(0);
            cleanup_string(cstring);
            return str;
        } else {
            throw new RuntimeException("unknown return value for calculateGradient: " + rval);
        }
    }

        private static native int vary_cs_cpp(int int_movesMin, int int_movesMax, double db_xAngle, double db_yAngle,
            CString result, String reqTypes);

    public static Optional<MutableList<ClassifiedCodeSequence>> varyCSCpp(int movesMin, int movesMax, double xAngle,
            double yAngle, String reqTypes) {
        final CString result = new CString();
        final int rval = vary_cs_cpp(movesMin, movesMax, xAngle, yAngle, result, reqTypes);
        if (rval > 0) {
            String strseq = result.string.getString(0);

            // Estimate number of lines
            int estimatedLines = (int) strseq.chars().filter(c -> c == '\n').count() + 1;
            List<ClassifiedCodeSequence> tmp = Collections.synchronizedList(new ArrayList<>(estimatedLines));

            Arrays.stream(strseq.split("\\R")).parallel().forEach(line -> {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        int[] dirty = Arrays.stream(trimmed.split("\\s+"))
                                .mapToInt(Integer::parseInt)
                                .toArray();
                        IntList list = IntArrayList.newListWith(dirty);
                        Optional<ClassifiedCodeSequence> codeSeq = Utils.convert(list);
                        codeSeq.ifPresent(tmp::add); // now thread-safe
                    } catch (Exception ignored) {
                        // skip malformed
                    }
                }
            });

            return Optional.of(Lists.mutable.ofAll(tmp));

        } else {
            throw new RuntimeException("unknown return value for calculateGradient: " + rval);
        }
    }

    private static native int vary_3_cpp(int int_movesMin, int int_movesMax, double db_initPosition, double db_xAngle,
            double db_yAngle, CString result, String reqTypes);

    public static Optional<MutableList<ClassifiedCodeSequence>> vary3Cpp(int movesMin, int movesMax,
            double initPosition, double xAngle, double yAngle, String reqTypes) {
        final CString result = new CString();
        final int rval = vary_3_cpp(movesMin, movesMax, initPosition, xAngle, yAngle, result, reqTypes);
        if (rval > 0) {
            String strseq = result.string.getString(0);

            // Estimate number of lines
            int estimatedLines = (int) strseq.chars().filter(c -> c == '\n').count() + 1;
            List<ClassifiedCodeSequence> tmp = new ArrayList<>(estimatedLines);

            Arrays.stream(strseq.split("\\R")).parallel().forEach(line -> {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        int[] dirty = Arrays.stream(trimmed.split("\\s+"))
                                .mapToInt(Integer::parseInt)
                                .toArray();
                        IntList list = IntArrayList.newListWith(dirty);
                        Optional<ClassifiedCodeSequence> codeSeq = Utils.convert(list);
                        codeSeq.ifPresent(tmp::add); // thread safety issue here, see below
                    } catch (Exception ignored) {
                        // skip malformed
                    }
                }
            });
            return Optional.of(Lists.mutable.ofAll(tmp));
        } else {
            throw new RuntimeException("unknown return value for calculateGradient: " + rval);
        }
    }

    private static native int vary_4_cpp(int int_movesMin, int int_movesMax, double db_xAngle, double db_yAngle,
            CString result, String reqTypes);

    public static Optional<MutableList<ClassifiedCodeSequence>> vary4Cpp(int movesMin, int movesMax, double xAngle,
            double yAngle, String reqTypes) {
        final CString result = new CString();
        final int rval = vary_4_cpp(movesMin, movesMax, xAngle, yAngle, result, reqTypes);
        if (rval > 0) {
            String strseq = result.string.getString(0);

            // Estimate number of lines
            int estimatedLines = (int) strseq.chars().filter(c -> c == '\n').count() + 1;
            List<ClassifiedCodeSequence> tmp = new ArrayList<>(estimatedLines);

            Arrays.stream(strseq.split("\\R")).parallel().forEach(line -> {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        int[] dirty = Arrays.stream(trimmed.split("\\s+"))
                                .mapToInt(Integer::parseInt)
                                .toArray();
                        IntList list = IntArrayList.newListWith(dirty);
                        Optional<ClassifiedCodeSequence> codeSeq = Utils.convert(list);
                        codeSeq.ifPresent(tmp::add); // thread safety issue here, see below
                    } catch (Exception ignored) {
                        // skip malformed
                    }
                }
            });
            return Optional.of(Lists.mutable.ofAll(tmp));
        } else {
            throw new RuntimeException("unknown return value for calculateGradient: " + rval);
        }
    }

}
