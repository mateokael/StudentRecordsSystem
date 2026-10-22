package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Single place for all JDBC connection code.
 *
 * MODE = "AUTO"     -> try the Derby Network Server (localhost:1527) first, then Embedded Derby.
 * MODE = "NETWORK"  -> Network Server only (start Java DB / Derby server first).
 * MODE = "EMBEDDED" -> Embedded Derby only (database folder is created inside the project folder).
 */
public class DatabaseConnection {

    public static final String DB_NAME = "StudentEnrollmentDB";
    private static final String MODE = "AUTO";

    private static final String USER = "app";
    private static final String PASSWORD = "app";

    private static final String NETWORK_URL = "jdbc:derby://localhost:1527/" + DB_NAME + ";create=true";
    private static final String EMBEDDED_URL = "jdbc:derby:" + DB_NAME + ";create=true";

    private static String activeUrl = null;
    private static String activeMode = "Not connected";

    public static synchronized Connection getConnection() throws SQLException {
        if (activeUrl != null) {
            return DriverManager.getConnection(activeUrl, USER, PASSWORD);
        }

        SQLException last = null;

        if (!"EMBEDDED".equals(MODE)) {
            try {
                loadDriver("org.apache.derby.jdbc.ClientDriver");
                Connection c = DriverManager.getConnection(NETWORK_URL, USER, PASSWORD);
                activeUrl = NETWORK_URL;
                activeMode = "Derby Network Server (localhost:1527)";
                return c;
            } catch (SQLException e) {
                last = e;
            }
        }

        if (!"NETWORK".equals(MODE)) {
            try {
                loadDriver("org.apache.derby.jdbc.EmbeddedDriver");
                Connection c = DriverManager.getConnection(EMBEDDED_URL, USER, PASSWORD);
                activeUrl = EMBEDDED_URL;
                activeMode = "Derby Embedded";
                return c;
            } catch (SQLException e) {
                if (last == null) last = e;
            }
        }

        throw new SQLException("Could not connect to Apache Derby. Check that the Derby JAR files "
                + "are added to the project Libraries and (for network mode) that the server is running. "
                + (last == null ? "" : "Details: " + last.getMessage()), "08001", last);
    }

    private static void loadDriver(String className) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            // JDBC 4 drivers are auto-loaded; DriverManager reports "No suitable driver" if missing.
        }
    }

    public static String getMode() {
        return activeMode;
    }

    /** Closes embedded Derby cleanly when the program exits. */
    public static void shutdown() {
        if (activeUrl != null && activeUrl.equals(EMBEDDED_URL)) {
            try {
                DriverManager.getConnection("jdbc:derby:;shutdown=true");
            } catch (SQLException ignored) {
                // Derby always throws SQLState XJ015 on a successful full shutdown.
            }
        }
    }
}
