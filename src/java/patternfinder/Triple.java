package patternfinder;

import java.util.Optional;
import billiards.viewer.Utils;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;

public class Triple implements Comparable<Triple> {
	private final ImmutableIntList[] codes;
	private int coef = -1;
	
	public Triple(String line) {
		codes = new ImmutableIntList[3];
		final String[] input = line.split(",");
		if (input.length != 3) {
			throw new RuntimeException("Bad triple was made (len)");
		}
		for (int i = 0; i < 3; i++) {
			Optional<ImmutableIntList> cut = Utils.splitString(input[i]);
			if (!cut.isPresent()) {
				throw new RuntimeException("Bad triple was made (cut)");
			}
			codes[i] = cut.get();
		}
	}
	
	public static Optional<Triple> create(final String line) {
		try {
			final Triple trip = new Triple(line);
			return Optional.of(trip);
		} catch (Exception e) {
			return Optional.empty();
		}
	}
	
	public ImmutableIntList getCode(final int i) {
		return codes[i];
	}
	
	public ImmutableIntList[] getCodes() {
		return codes;
	}
	
	public void setCoef(int newCoef) {
		if (coef == -1 && newCoef > 0) {
			coef = newCoef;
		}
	}
	
	// return -1 if t1 < t2, 1  if t1 > t2, 0 otherwise
	public static int compare(Triple t1, Triple t2) {
		for (int i = 0; i < 3; i++) {
			if (t1.getCode(i).size() < t2.getCode(i).size()) {
				return -1;
			} 
		} 
		for (int i = 0; i < 3; i++) {
			if (t1.getCode(2 - i).size() > t2.getCode(2 - i).size()) {
				return 1;
			} 
		} 
		return 0;
	}
	
	@Override
	public int compareTo(Triple comparableTrip) {
		if (Triple.compare(this, comparableTrip) != 0) {
			return Triple.compare(this, comparableTrip);
		} else {
			return PatUtils.intListCompare(codes[0], comparableTrip.getCode(0));
		}
		
	}
	
	@Override
	public String toString() {
		return PatUtils.printImm(codes[0]) + ", " + PatUtils.printImm(codes[1]) 
				+ ", " + PatUtils.printImm(codes[2]);
	}
}
