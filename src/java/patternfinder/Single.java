package patternfinder;

import java.util.Optional;
import billiards.viewer.Utils;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;

public class Single implements Comparable<Single>{
	private final ImmutableIntList code;
	private int coef = -1;
	
	private Single(ImmutableIntList code) {
		this.code = code;
	}
	
	public static Optional<Single> create(final String line) {
		Optional<ImmutableIntList> codeLine = Utils.splitString(Utils.tripleTrimmer(line)); 
		if (codeLine.isPresent()) {
			return Optional.of(new Single(codeLine.get()));
		} else {
			return Optional.empty();
		}
	}
	
	public ImmutableIntList getCode() {
		return code;
	}
	
	public boolean isEmpty() {
		return code.isEmpty();
	}
	
	public void setCoef(int newCoef) {
		if (coef == -1 && newCoef > 0) {
			coef = newCoef;
		}
	}
	
	public int compareTo(Single comparableTrip) {
		if (code.size() < comparableTrip.getCode().size()) {
			return -1;
		} else if (code.size() > comparableTrip.getCode().size()) {
			return 1;
		} else {
			return PatUtils.intListCompare(code, comparableTrip.getCode());
		}
		
	}
	
	@Override 
	public String toString() {
		return "(" + coef + ")" + PatUtils.printImm(code); 
	}
}
