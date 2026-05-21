package billiards.viewer;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.Optional;

// How do you write bug resistant code? Immutability
// Mutability causes more logic bugs than pretty well
// anything else. When things are mutable, you have to be
// so careful all the time that the changes you make to
// variables happen at the right time and in the right order. When things
// are immutable, you just don't have to worry about it.

public final class BackwardForward<T> {
    private final MutableList<T> list;
    // Index of the current thing in the list
    private int currentIndex;

    private BackwardForward(final T initialElem) {
        this.list = new FastList<>();
        this.list.add(initialElem);

        this.currentIndex = 0;
    }

    public static <T> BackwardForward<T> create(final T initialElem) {
        return new BackwardForward<>(initialElem);
    }

    public Optional<T> backward() {
        if (this.currentIndex == 0) {
            // can't move backward
            return Optional.empty();
        } else {
            this.currentIndex -= 1;

            final T elem = this.list.get(this.currentIndex);

            return Optional.of(elem);
        }
    }

    public Optional<T> forward() {
        if (this.currentIndex == this.list.size() - 1) {
            // can't move forward
            return Optional.empty();
        } else {
            this.currentIndex += 1;

            final T elem = this.list.get(this.currentIndex);

            return Optional.of(elem);
        }
    }

    public void add(final T elem) {
        final T currentElem = this.list.get(this.currentIndex);

        // Only update the list if the elem we are trying to add is
        // different that the current element
        if (!currentElem.equals(elem)) {
            // Delete everything after the currentIndex
            final MutableList<T> tail = this.list.subList(this.currentIndex + 1, this.list.size());
            tail.clear();

            this.list.add(elem);
            this.currentIndex += 1;
        }
    }
}
