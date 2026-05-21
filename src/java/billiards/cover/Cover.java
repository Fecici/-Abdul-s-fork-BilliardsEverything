package billiards.cover;

import billiards.codeseq.ClassifiedCodeSequence;

// Trying to model an ADT in Java

public abstract class Cover {

    public static final class Empty extends Cover {}

    public static final class Uncovered extends Cover {}

    public static final class Stable extends Cover {
        public final ClassifiedCodeSequence stable;

        public Stable(final ClassifiedCodeSequence stable) {
            this.stable = stable;
        }
    }

    public static final class Triple extends Cover {
        public final Triple triple;

        public Triple(final Triple triple) {
            this.triple = triple;
        }
    }

    public static final class Divide extends Cover {
        public final Cover[] quarters;

        public Divide(final Cover[] quarters) {
            this.quarters = quarters;
        }
    }
}
