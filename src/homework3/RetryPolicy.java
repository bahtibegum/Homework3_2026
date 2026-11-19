package homework3;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/** Common rollback/retry classification and randomized backoff for the benchmark and DB test. */
final class RetryPolicy {
    private RetryPolicy() { }

    static boolean isRetryable(String sqlState) {
        return "40P01".equals(sqlState) || "40001".equals(sqlState)
                || "55P03".equals(sqlState);
    }

    /** JDBC batch failures may expose the PostgreSQL SQLSTATE in getNextException(). */
    static String retryableState(SQLException failure) {
        Set<SQLException> visited = new HashSet<>();
        SQLException current = failure;
        while (current != null && visited.add(current)) {
            if (isRetryable(current.getSQLState())) return current.getSQLState();
            current = current.getNextException();
        }
        return null;
    }

    static void backoff(int attempt) {
        long capMs = Math.min(200, 2L << Math.min(7, Math.max(0, attempt)));
        long sleepMs = ThreadLocalRandom.current().nextLong(1, capMs + 1);
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(sleepMs));
    }
}
