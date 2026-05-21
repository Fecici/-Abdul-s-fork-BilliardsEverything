package billiards.viewer;

public final class Cycle {
    private final int mod;
    private int current;

    // mod must be positive
    public Cycle(final int mod) {
        if (mod <= 0) {
            throw new RuntimeException("cycle length must be greater than 0");
        }
        this.mod = mod;
        this.current = 0;
    }

    // get will advance the cycle by one, modulus the mod
    public int get() {
        final int rval = this.current;
        this.current = (this.current + 1) % this.mod;
        return rval;
    }
}
