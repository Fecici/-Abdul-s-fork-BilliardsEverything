package billiards.database;

import billiards.wrapper.ConnectionPool;
import billiards.wrapper.Wrapper;
import javaslang.collection.Array;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

// Methods for doing database administration
public final class Admin {

    public static final String databaseDir = System.getProperty("user.home") + "/billiard-databases";

    public static String getDatabasePath(final String dbName) {
        return String.format("%s/%s.sqlite", databaseDir, dbName);
    }

    // it is most important to set settings that can be overrided at
    // compile time. For ones that you can't set at compile time, you
    // can rely on the defaults

    // Also, we need to make sure that these settings are the same here
    // and in the C++ code

    // what about the locking mode, read uncommited, shared cache?
    // Also, look at pragma optimize or something

    // There is a slight chance of database corruption in NORMAL mode, which we want to avoid
    // EXTRA is only when you are using a rollback journal, which we aren't using

    public static ConnectionPool getConnectionPool(final String dbName, final int poolSize) {
        final String dbPath = getDatabasePath(dbName);
        return new ConnectionPool(dbPath, poolSize);
    }

    public static void initDatabaseDirectory() {
        try {
            Files.createDirectories(Paths.get(databaseDir));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> listDatabases() {

        final MutableList<String> databases = new FastList<>();

        final Path dir = Paths.get(databaseDir);

        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {

            for (final Path path : stream) {
                final String name = path.getFileName().toString();

                final boolean db = Files.isRegularFile(path) && name.endsWith(".sqlite");

                if (db) {
                    final String dbName = name.substring(0, name.lastIndexOf('.'));
                    databases.add(dbName);
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        databases.sortThis();

        return databases.asUnmodifiable();
    }
    
    public static String getUrl(final String dbName) {
        return "jdbc:sqlite:" + getDatabasePath(dbName);
    }

    public static void newDatabase(final String dbName) {

        final String dbPath = getDatabasePath(dbName);

        Wrapper.createDatabase(dbPath);
    }
    //Required format for the garbage database
    public static void newJavaDB(final String dbName) {
        final String table = "create table if not exists %s("
                + "code_sequence text check(typeof(code_sequence) = 'text'),"
                + "primary key (code_sequence)"
                + ");";

		final Array<String> codeTypes = Array.of("oso", "osno", "cs", "ons", "cns");
		
		final StringBuilder builder = new StringBuilder();
		
		for (final String codeType : codeTypes) {
			final String sql = String.format(table, codeType);
			builder.append(sql);
		}
		
		final String sql = builder.toString();
		
		final String url = getUrl(dbName);
		
		try (final Connection conn = DriverManager.getConnection(url)) {
			conn.createStatement().executeUpdate(sql);
		} catch (final SQLException e) {
			throw new RuntimeException(e);
		}
    }

    public static void deleteDatabase(final String dbName) {
        final String dbPath = getDatabasePath(dbName);
        final String shmPath = dbPath + "-shm";
        final String walPath = dbPath + "-wal";

        try {
            Files.delete(Paths.get(dbPath));
            Files.deleteIfExists(Paths.get(shmPath));
            Files.deleteIfExists(Paths.get(walPath));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void clearDatabase(final String dbName) {

        final String dbPath = getDatabasePath(dbName);

        Wrapper.clearDatabase(dbPath);
    }
}
