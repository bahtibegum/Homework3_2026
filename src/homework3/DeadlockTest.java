package homework3;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Real PostgreSQL integration test: opposite row lock order deliberately triggers a deadlock.
 * Uses a separate test-only table; it never changes the benchmark's blocking_data table.
 */
public final class DeadlockTest {
    private static final String TABLE = "homework3_deadlock_test";
    private DeadlockTest() { }

    public static void main(String[] args) throws Exception {
        Config cfg = Config.parse(args);
        try (Connection admin = Database.open(cfg); Statement st = admin.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS " + TABLE + " (data_id integer PRIMARY KEY, value integer NOT NULL)");
            st.execute("TRUNCATE " + TABLE);
            st.execute("INSERT INTO " + TABLE + " (data_id, value) VALUES (1, 0), (2, 0)");
        }
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier bothHoldFirstLock = new CyclicBarrier(2);
        try {
            Future<Integer> left = executor.submit(() -> runTx(cfg, 1, 2, bothHoldFirstLock));
            Future<Integer> right = executor.submit(() -> runTx(cfg, 2, 1, bothHoldFirstLock));
            int deadlocks = left.get(30, TimeUnit.SECONDS) + right.get(30, TimeUnit.SECONDS);
            try (Connection c = Database.open(cfg);
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT count(*), min(value), max(value) FROM " + TABLE)) {
                if (!rs.next() || rs.getInt(1) != 2 || rs.getInt(2) != 2 || rs.getInt(3) != 2) {
                    throw new AssertionError("Retry test did not commit both transactions correctly");
                }
            }
            if (deadlocks < 1) throw new AssertionError("Test did not encounter the intended deadlock");
            System.out.println("PASS: PostgreSQL deadlock triggered (" + deadlocks
                    + "), rolled back, retried the same UPDATE sequence; both transactions committed.");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            try (Connection c = Database.open(cfg); Statement st = c.createStatement()) {
                st.execute("DROP TABLE IF EXISTS " + TABLE);
            }
        }
    }

    private static int runTx(Config cfg, int first, int second, CyclicBarrier barrier)
            throws SQLException, InterruptedException, BrokenBarrierException, java.util.concurrent.TimeoutException {
        try (Connection c = Database.open(cfg);
             PreparedStatement ps = c.prepareStatement("UPDATE " + TABLE + " SET value = value + 1 WHERE data_id = ?")) {
            c.setAutoCommit(false);
            int retries = 0;
            int deadlocks = 0;
            while (true) {
                try {
                    ps.setInt(1, first);
                    if (ps.executeUpdate() != 1) throw new SQLException("Missing first key");
                    // Only on attempt 1: force each connection to hold its first row lock.
                    if (retries == 0) barrier.await(10, TimeUnit.SECONDS);
                    ps.setInt(1, second);
                    if (ps.executeUpdate() != 1) throw new SQLException("Missing second key");
                    c.commit();
                    return deadlocks;
                } catch (SQLException e) {
                    c.rollback();
                    String state = RetryPolicy.retryableState(e);
                    if (state == null) throw e;
                    if ("40P01".equals(state)) deadlocks++;
                    retries++;
                    if (retries > 10) throw new SQLException("Too many retries", e);
                    RetryPolicy.backoff(retries);
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                }
            }
        }
    }
}
