package billiards.viewer;

public final class SideSum {

    private final int[] coeffs;

    private final double x;
    private final double y;
    private final double z;

    private SideSum(final int[] coeffs, final double x, final double y, final double z) {
    	this.coeffs = coeffs;
    	this.x = x;
    	this.y = y;
    	this.z = z;

    }
    
    public static SideSum create(final double x, final double y) {
    	
    	return new SideSum( new int[3], x, y, Math.PI - x - y);
    }

    public void add(final int num) {
        this.coeffs[num] += 1;
    }

    public void sub(final int num) {
        this.coeffs[num] -= 1;
    }

    public double sum() {
        return this.coeffs[0] * this.x + this.coeffs[1] * this.y + this.coeffs[2] * this.z;
    }
    
    public SideSum copy() {
    	final int[] newCoeffs = new int[coeffs.length];
    	for (int i = 0; i < coeffs.length; i++) {
    		final int coef = coeffs[i];
    		newCoeffs[i] = coef;
    	}
    	return new SideSum(newCoeffs, this.x, this.y, this.z);
    }
    
    @Override
    public String toString() {
    	return this.coeffs[0] + " * A + " + this.coeffs[1] 
    			+ " * B + " + this.coeffs[2] + " * C";
    }
}
