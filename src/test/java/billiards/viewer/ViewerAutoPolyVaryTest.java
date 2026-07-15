package billiards.viewer;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ViewerAutoPolyVaryTest {

    @Test
    void advancesForwardWithoutPassingEnd() {
        assertTrue(Viewer.hasNextAutoPolyVaryIndex(169, 173, 1));
        assertFalse(Viewer.hasNextAutoPolyVaryIndex(173, 173, 1));
    }

    @Test
    void advancesReverseWithoutDependingOnGlobalReverseState() {
        assertTrue(Viewer.hasNextAutoPolyVaryIndex(173, 169, -1));
        assertFalse(Viewer.hasNextAutoPolyVaryIndex(169, 169, -1));
    }

    @Test
    void rejectsZeroStep() {
        assertThrows(IllegalArgumentException.class,
                () -> Viewer.hasNextAutoPolyVaryIndex(170, 173, 0));
    }

    @Test
    void waitsForCommittedCoordinateViewBeforeStartingVaryWork() {
        final AtomicBoolean workStarted = new AtomicBoolean(false);
        final AtomicReference<Runnable> renderCompletion = new AtomicReference<>();

        Viewer.startAutoPolyVaryCoordinateAfterRender(renderCompletion::set,
                () -> workStarted.set(true));

        assertFalse(workStarted.get());
        renderCompletion.get().run();
        assertTrue(workStarted.get());
    }
}
