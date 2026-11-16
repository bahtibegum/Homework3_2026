package homework3;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.SplittableRandom;

/** Table schema remains identical to the assignment's PostgreSQL blocking_data schema. */
final class Database {
    private Database() { }

    static Connection open(Config c) throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", c.user());
        props.setProperty("password", c.password());
        props.setProperty("ApplicationName", "HW3_2026_" + c.mode());
        props.setProperty("reWriteBatchedInserts", "true");
        return DriverManager.getConnection(c.jdbcUrl(), props);
    }

    static void reset(Config cfg) throws SQLException {
        try (Connection c = open(cfg)) {
            c.setAutoCommit(false);
            try {
                try (Statement st = c.createStatement()) {
                    st.execute("CREATE TABLE IF NOT EXISTS blocking_data (" +
                            "data_id integer PRIMARY KEY, data_value double precision, modified_by integer)");
                    // IMPORTANT: this deletes old INSERT rows (and all other blocking_data rows).
                    st.execute("TRUNCATE TABLE blocking_data");
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO blocking_data (data_id, data_value, modified_by) VALUES (?, ?, ?)")) {
                    SplittableRandom rng = new SplittableRandom(cfg.seed());
                    for (int i = 0; i < cfg.rows(); i++) {
                        ps.setInt(1, i);
                        ps.setDouble(2, rng.nextDouble(0.0, 50.0));
                        ps.setInt(3, 0);
                        ps.addBatch();
                        if ((i + 1) % 1000 == 0) ps.executeBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                System.err.printf("Database reset: %d base rows loaded; previous INSERT rows removed.%n", cfg.rows());
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
