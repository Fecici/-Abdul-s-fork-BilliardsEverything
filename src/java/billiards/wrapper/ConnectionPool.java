package billiards.wrapper;

// Instead of having a static variable (too much magic, and I don't trust it)
// we'll just dynamically allocate the pool, keep a pointer to it, and then
// destroy it at the end

// static and thread_local have their place, but they confuse me. Doing this
// is more straight forward and simpler to boot

import com.sun.jna.Pointer;

public class ConnectionPool {

    public final Pointer pointer;

    public ConnectionPool(final String dbPath, final int poolSize) {
        this.pointer = Wrapper.createConnectionPool(dbPath, poolSize);
    }

    public void destroy() {
        Wrapper.destroyConnectionPool(pointer);
    }
}
