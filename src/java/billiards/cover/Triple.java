package billiards.cover;

import billiards.codeseq.ClassifiedCodeSequence;

public final class Triple {
    public final ClassifiedCodeSequence stableNeg;
    public final ClassifiedCodeSequence unstable;
    public final ClassifiedCodeSequence stablePos;

    public Triple(final ClassifiedCodeSequence stableNeg,
                  final ClassifiedCodeSequence unstable,
                  final ClassifiedCodeSequence stablePos) {
        this.stableNeg = stableNeg;
        this.unstable = unstable;
        this.stablePos = stablePos;
    }
}
