package patternfinder;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import java.util.ArrayList;
import java.util.Optional;
import java.util.regex.Pattern;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.viewer.Utils;
import billiards.wrapper.ConnectionPool;
import billiards.wrapper.Wrapper;

public class PatUtils {

    public static final int numThreads = Runtime.getRuntime().availableProcessors();

    public static String trimCodeLine(String line) {

        // strip of comment lines
        line = line.split("//")[0];

        if (line.contains("-")) {
            line = line.split("-")[1];
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
    		return line.trim();
    	}
    	else {
    		return trimCodeLine(line);
    	}
    }

    public static String[] removeEmpty(String[] withEmpties) {
    	final ArrayList<String> resultList = new ArrayList<>();
    	for (int i = 0; i < withEmpties.length; i++) {
    		if (!withEmpties[i].replace(" ", "").isEmpty()) {
    			resultList.add(withEmpties[i]);
    		}
    	}
    	final String[] resultArray = new String[resultList.size()];
    	for (int i = 0; i < resultList.size(); i++) {
    		resultArray[i] = resultList.get(i);
    	}
    	return resultArray;
    }

    public static ImmutableIntList listGCD(int[] l) {
    	return listGCD(l, 1);
    }

    public static ImmutableIntList listGCD(int[] l, Integer coef) {
    	int gcd = l[0];

    	for (int i = 1; i < l.length; i++) {
    		if (gcd == 0) {
    			gcd = l[i];
    		} else {
    			gcd = GCD(gcd, l[i]);
    		}
    	}

    	final MutableIntList l2 = new IntArrayList();
    	for (int i = 0; i < l.length; i++) {
    		l2.add(coef * l[i]/gcd);
    	}
    	return l2.toImmutable();
    }

    public static int GCD(Integer a1, Integer a2) {
    	if (a1 == 0 || a2 == 0) {
    		return a1;
    	}
    	int n1 = a1;
    	int n2 = a2;
    	while (n1 != n2) {
    		if (n1 > n2) {
    			n1 -= n2;
    		} else {
    			n2 -= n1;
    		}
    	}
    	return n1;
    }

    public static String printAndTestTrip(final Triple trip, final ConnectionPool pool) {
    	String result = "";
    	for (int i = 0; i < 3; i++) {
			if (!PatUtils.emptyVerify(trip.getCode(i), pool)) {
				result += "empty " + PatUtils.printImm(trip.getCode(i));
			} else {
				result += printImm(trip.getCode(i));
			}
			if (i < 2) {
				result += ", ";
			}

    	}
    	return result;
    }

    public static String repeat(String str, int times) {
        return new String(new char[times]).replace("\0", str);
    }

    public static String printImm(ImmutableIntList imm) {
    	String result = "";
		for (int j = 0; j < imm.size(); j++) {
			result += " " + imm.get(j);
		}
    	return result.trim();
    }

    public static String printPat(ImmutableIntList pat) {
    	String result = "";
		for (int j = 0; j < pat.size(); j++) {
			result += repeat(" " + (j + 1), pat.get(j));
		}
    	return result.trim();
    }

    public static int intListCompare(final ImmutableIntList l1, final ImmutableIntList l2) {
    	for (int i = 0; i < l1.size(); i++) {
    		if (l1.get(i) > l2.get(i)) {
    			return 1;
    		} else if (l1.get(i) < l2.get(i)) {
    			return -1;
    		}
    	}
    	return 0;
    }

	// this checks if the element is an empty set
	public static boolean emptyVerify(final ImmutableIntList pat, final ConnectionPool pool) {
		return Wrapper.saveToDatabase(pat.toArray(), pool);
	}

	public static boolean emptyVerifyLR(final ImmutableIntList b,
	        final ImmutableIntList c, final ConnectionPool pool) {
	    ClassifiedCodeSequence base;
        ClassifiedCodeSequence code;
        try {
            base = ClassifiedCodeSequence.create(b).get();
            code = ClassifiedCodeSequence.create(c).get();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't make a classified code sequence in supercheckLR");
        }

	    return Wrapper.loadPictureLR(base, code, pool, "empty").isPresent();
	}

    // add a pattern to a code, 'times' times.
    public static ImmutableIntList addImm(ImmutableIntList code, ImmutableIntList pat, int times) {

    	final MutableIntList result = new IntArrayList();
    	

    	final int[] muteCode = code.toArray();
    	for (int i = 0; i < pat.size(); i++) {
    		int value = muteCode[pat.get(i) - 1];
    		muteCode[pat.get(i) - 1] = value + (2 * times);
    	}

    	for (int i = 0; i < code.size(); i++) {
    		result.add(muteCode[i]);
    	}
    	return result.toImmutable();
    }

    // check if a line with extend is valid
    public static boolean xtndValidate(final String line) {
    	if (!line.contains("#")) {
    		return false;
    	}

		final String codeStr;
		final String patStr;
		try {
			codeStr = line.replace("+", "").split("#")[0].trim();
			patStr = line.replace("+", "").split("#")[1].trim();
		} catch (IndexOutOfBoundsException e) {
			return false;
		}

		final String[] codeStrs = Utils.tripleTrimmer(codeStr).split(",");
		final String[] patStrs = patStr.split(",");

		if ((codeStrs.length == 1 && patStrs.length == 1) ||
			(codeStrs.length == 3 && patStrs.length == 3)) {

			for (int i = 0; i < codeStrs.length; i++) {
				final Optional<ImmutableIntList> code = splitString(codeStrs[i]);
				if (!code.isPresent()) {
					return false;
				}
				final Optional<ImmutableIntList> pat = splitString(patStrs[i]);
				if (!pat.isPresent()) {
					return false;
				}
				final int max = pat.get().max();
				if (max > code.get().size()) {
					return false;
				}
			}

			return true;
		}

		return false;
    }


    public static Optional<ImmutableIntList> splitString(final String textCodeSeq) {
        // split on whitespace
    	final String tcsTrim = textCodeSeq.trim();
    	if (tcsTrim.isEmpty()) {
    		return Optional.empty();
    	}
        final String[] textCodeNumbers = tcsTrim.split("\\s+");

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
}
