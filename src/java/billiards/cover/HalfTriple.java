package billiards.cover;
import billiards.codeseq.ClassifiedCodeSequence;

public final class HalfTriple {
    public final ClassifiedCodeSequence stableNeg;
    public final ClassifiedCodeSequence unstable;

    public HalfTriple(final ClassifiedCodeSequence stableNeg,
                  final ClassifiedCodeSequence unstable) {
        this.stableNeg = stableNeg;
        this.unstable = unstable;
    }
}
