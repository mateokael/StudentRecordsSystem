import database.DatabaseInitializer;
import ui.LoginFrame;

import javax.swing.*;
import java.sql.SQLException;

/** Entry point: prepares the Derby database, then shows the login screen. */
public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to the default look and feel
        }

        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseInitializer.initialize();
            } catch (SQLException e) {
                boolean connection = "08001".equals(e.getSQLState());
                JOptionPane.showMessageDialog(null,
                        (connection ? "Cannot connect to Apache Derby.\n\n" + e.getMessage()
                                + "\n\nMake sure the Derby JAR files are added to the project Libraries."
                                : "The database was reached, but setting it up failed.\n\n"
                                + ("SCHEMA".equals(e.getSQLState()) ? e.getMessage()
                                        : "SQLState " + e.getSQLState() + ": " + e.getMessage())),
                        connection ? "Database Connection Error" : "Database Setup Error",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new LoginFrame().setVisible(true);
        });
    }
}
