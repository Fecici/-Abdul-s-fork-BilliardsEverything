package patternfinder;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

public class Spattern {
	final private ImmutableIntList pat;
	final private ImmutableIntList base;
	
	public Spattern(ImmutableIntList pat, ImmutableIntList ex) {
		if (pat.max() == 0) {
			throw new RuntimeException("Bad pattern");
		}
		this.pat = pat;
		this.base = makeBase(pat, ex);
	}
	
	// p is the pattern, and ex is an example of a code in the sequence. Any code will do.
	private static ImmutableIntList makeBase(ImmutableIntList p, ImmutableIntList ex) {
		final MutableIntList coefs = new IntArrayList();
		for (int i = 0; i < p.size(); i++) {
			if (p.get(i) != 0) {
				int coef = ex.get(i)/(2 * p.get(i));
				if (ex.get(i) - coef * 2 * p.get(i) == 0) {
					coef -= 1;
					

				}
				coefs.add(coef);
			}
		}
		int min = coefs.min();
		
		final MutableIntList base = new IntArrayList();
		for (int i = 0; i < p.size(); i++) {
			base.add(ex.get(i) - min * 2 * p.get(i));
		}

		return base.toImmutable();
	}
	
	public static boolean same(Spattern p1, Spattern p2) {
		return (p1.getPat().equals(p2.getPat()) && p1.getBase().equals(p2.getBase())); 
	}
	
	public int size() {
		return pat.size();
	}
	
	public int getPat(int i) {
		return pat.get(i);
	}
	
	public ImmutableIntList getPat() {
		return pat;
	}
	
	public ImmutableIntList getBase() {
		return base;
	}
	
	public int getN(final Single trip) {
		int n = 0;
		for (int i = 0; i < pat.size(); i++) {
			if (pat.get(i) != 0) {
				n = (trip.getCode().get(i) - base.get(i)) / pat.get(i);
				return n / 2;
			}
		}
		return 0;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
	    if (!Spattern.class.isAssignableFrom(obj.getClass())) {
	        return false;
	    }
	    final Spattern t2 = (Spattern) obj;
		return pat.equals(t2.getPat()) && base.equals(t2.getBase());
	}
	
	@Override
	public int hashCode() {
	    int hash = 3;
	    hash = 53 * hash + (this.pat != null ? this.pat.hashCode() : 0);
	    hash = 53 * hash + this.base.hashCode();
	    return hash;
	}
	
	@Override
	public String toString() {
		return "// pat: " + PatUtils.printPat(pat) + "\n// base: " + PatUtils.printImm(base);
	}
}
