package patternfinder;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

public class Tpattern implements Comparable<Tpattern> {
	final private ImmutableIntList[] pat = new ImmutableIntList[3];
	final private ImmutableIntList[] base = new ImmutableIntList[3];
	
	public Tpattern() {

	}
	
	private ImmutableIntList[] makeBase(ImmutableIntList[] ex) {
		
		final MutableIntList coefs = new IntArrayList();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < pat[i].size(); j++) {
				if (pat[i].get(j) != 0) {
					int coef = ex[i].get(j)/(2 * pat[i].get(j));
					if (ex[i].get(j) - coef * 2 * pat[i].get(j) == 0) {
						coef -= 1;
					}
					coefs.add(coef);
				}
			}
		}

		int min = coefs.min();
		
		final MutableIntList[] muteBase = {new IntArrayList(), new IntArrayList(), new IntArrayList()};
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < pat[i].size(); j++) {
				muteBase[i].add(ex[i].get(j) - min * 2 * pat[i].get(j));
			}
		}
		final ImmutableIntList[] calc = 
			{muteBase[0].toImmutable(), muteBase[1].toImmutable(), muteBase[2].toImmutable()};
		return calc;
	}
	
	public void setPat(final Spattern p, final int i) {
		pat[i] = p.getPat();
	}
	
	public void setPat(final ImmutableIntList p, final int i) {
		pat[i] = p;
	}
	
	public void setBase(final ImmutableIntList[] ex) {
		ImmutableIntList[] calcBase = makeBase(ex);
		for (int i = 0; i < 3; i++) {
			base[i] = calcBase[i];
		}
	}
	
	public int size(final int i) {
		return pat[i].size();
	}
	
	public ImmutableIntList getPat(final int i) {
		return pat[i];
	}
	
	public ImmutableIntList getBase(final int i) {
		return base[i];
	}
	
	public String patString() {
		return PatUtils.printPat(pat[0]) + ", " + PatUtils.printPat(pat[1]) + ", " + PatUtils.printPat(pat[2]);
	}
	
	public String baseString() {
		return PatUtils.printImm(base[0]) + ", " + PatUtils.printImm(base[1]) + ", " + PatUtils.printImm(base[2]);
	}
	
	public int getN(final Triple trip) {
		int n = 0;
		for (int i = 0; i < pat[0].size(); i++) {
			if (pat[0].get(i) != 0) {
				n = (trip.getCode(0).get(i) - base[0].get(i)) / pat[0].get(i);
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
	    if (!Tpattern.class.isAssignableFrom(obj.getClass())) {
	        return false;
	    }
	    final Tpattern t2 = (Tpattern) obj;
		return (pat[0].equals(t2.getPat(0)) && base[0].equals(t2.getBase(0)) &&
				pat[1].equals(t2.getPat(1)) && base[1].equals(t2.getBase(1)) &&
				pat[2].equals(t2.getPat(2)) && base[2].equals(t2.getBase(2))); 
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
		return "// pat: " + PatUtils.printPat(pat[0]) + ", " + PatUtils.printPat(pat[1]) 
		        + ", " + PatUtils.printPat(pat[2]) + "\n// base: " + PatUtils.printImm(base[0]) + ", " 
			    + PatUtils.printImm(base[1]) + ", " + PatUtils.printImm(base[2]);
	}

	@Override
	public int compareTo(Tpattern obj) {
		final boolean eq = this.equals(obj);
		if (eq) {
			return 0;
		}
		return -1;
	}
	
}
