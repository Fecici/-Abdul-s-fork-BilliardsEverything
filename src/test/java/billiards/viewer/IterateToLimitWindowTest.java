package billiards.viewer;

import javafx.beans.property.SimpleBooleanProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IterateToLimitWindowTest {

    @Test
    void closingWithoutExecuteHasNoFinishObserverToNotify() {
        assertDoesNotThrow(() -> IterateToLimitWindow.markFinishedIfPresent(null));
    }

    @Test
    void existingFinishObserverIsNotified() {
        final SimpleBooleanProperty finish = new SimpleBooleanProperty(false);

        IterateToLimitWindow.markFinishedIfPresent(finish);

        assertTrue(finish.get());
    }
}
