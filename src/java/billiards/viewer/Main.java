package billiards.viewer;

import billiards.database.Admin;
import billiards.wrapper.ConnectionPool;
import billiards.wrapper.Wrapper;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.stage.Stage;
import patternfinder.PatternFinder;

public final class Main extends Application {

    // These are initialized first (like they are in a constructor)
    private final ExecutorService executor = Executors.newFixedThreadPool(Utils.numThreads);
    private ConnectionPool pool = null;

    private final String versionNumber = "10.0.14";

    // Order is constructor, init, start, stop
    // It would be a lot simpler if these methods didn't exist, and I just did
    // stuff myself. A lot less magic that way.

    // We currently don't use this
    @Override
    public void init() {
        Wrapper.errorLogging();
    }

    @Override
    public void start(final Stage mainWindow) {
        System.out.println("Threads available: " + Utils.numThreads);
        final DBGui dbGui = new DBGui();
        // Shows and waits until the window closes
        final Optional<String> databaseName = dbGui.getDbName();

        // IMPORTANT! Create a new garbage database if it does not already exist
        Admin.newJavaDB("garbage");

        // since we just have two programs for now, this is just a boolean telling if we're
        // using viewer or not.
        final boolean viewerSelected = dbGui.getProgram();

        databaseName.ifPresent(dbName -> {

            // 2024-06-06 Austin experimenting with thread and connection pool sizes
            pool = Admin.getConnectionPool(dbName, Utils.numThreads);
            if (viewerSelected) {
	            final Viewer viewer = new Viewer(mainWindow, versionNumber, executor, pool, dbName);
	            viewer.start(executor);
            } else {
            	final PatternFinder pFinder = new PatternFinder(mainWindow, versionNumber, pool, dbName);
                pFinder.start();
            }
        });
    }

    @Override
    public void stop() {

        if (pool != null) {
            pool.destroy();
        }

        executor.shutdown();
    }
}
