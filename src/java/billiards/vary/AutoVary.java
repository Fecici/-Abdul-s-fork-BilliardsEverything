package billiards.vary;

import java.util.concurrent.ExecutorService;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.geometry.ConvexPolygon;
import billiards.geometry.Location;
import billiards.math.CoverSquare;
import billiards.viewer.BoyanMenu;
import billiards.wrapper.ConnectionPool;
import billiards.wrapper.Wrapper;

public class AutoVary {

    final int movesMin;
    final int movesMax;
    final int shots;
    final boolean[] types;
    final int maxDepth;
    final ConvexPolygon polygon;
    final ConnectionPool pool;
    final ExecutorService executor;

    public AutoVary(final int min, final int max, final int numShots, boolean[] type, final int depth,
            final ConvexPolygon poly, final ConnectionPool cPool, final ExecutorService exe) {
        movesMin = min;
        movesMax = max;
        shots = numShots;
        types = type;
        maxDepth = depth;
        polygon = poly;
        pool = cPool;
        executor = exe;
    }

    private boolean recurseFireAway(final int depth, final CoverSquare square,
                                 MutableList<ClassifiedCodeSequence> codesFound) {

        final double[] cnt = square.center();
        boolean covered = false;

        final MutableList<ClassifiedCodeSequence> codes = new FastList<>();
        if (polygon.location(cnt[0], cnt[1]).equals(Location.INSIDE)) {
            for (int i = 0; i < shots; i++) {
                codes.addAll(BoyanMenu.findCodes3(cnt[0], cnt[1], movesMin, movesMax, i, types, executor));
            }

            covered = Wrapper.coverWrapper(square.toString(), codes.toString(), "", 0, 1, 0, true, pool).isEmpty();
        }

        if (!covered) {
            if (depth < maxDepth) {
                final boolean[] covs = new boolean[4];
                final CoverSquare[] squares = square.subdivide();

                for (int i = 0; i < 4; i++) covs[i] = recurseFireAway(depth + 1, squares[i], codesFound);

                if (covs[0] && covs[1] && covs[2] && covs[3]) covered = true;
            }
        }

        return covered;
    }

    public MutableList<ClassifiedCodeSequence> fireaway() {
                //final int depth, final int maxCode, final ConvexPolygon poly) {

        final MutableList<ClassifiedCodeSequence> codesFound = new FastList<>();

        recurseFireAway(0, CoverSquare.initial(), codesFound);

        return codesFound;
    }
}
