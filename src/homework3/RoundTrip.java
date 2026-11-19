package homework3;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Experimental protocol optimization, separate from the serial/concurrent benchmark baseline.
 * For point reads and writes, sends the EXACT three required SQL statements, preserving key
 * order, in a single JDBC Statement.execute call instead of three client/server round trips.
 * Each SELECT still filters one tuple and each UPDATE still modifies at most one tuple.
 * Numeric parameters come exclusively from generated primitive integers / finite doubles;
 * no user-provided SQL is concatenated.
 *
 * This optimization is a hypothesis until the user's PostgreSQL benchmarks confirm it.
 * Multi-result support is supplied by PostgreSQL JDBC; run the integration smoke test.
 */
final class RoundTrip {
    private RoundTrip() { }

    static String readsSql(int[] ids) {
        requireThreeDistinct(ids);
        StringBuilder b = new StringBuilder(180);
        for (int id : ids) {
            if (!b.isEmpty()) b.append(';');
            b.append("SELECT data_value FROM blocking_data WHERE data_id = ").append(id);
        }
        return b.toString();
    }

    static String updatesSql(Workload.Tx tx) {
        int[] ids = tx.dataIds();
        requireThreeDistinct(ids);
        if (tx.values().length != 3) throw new IllegalArgumentException("Exactly three values required");
        int actor = Math.toIntExact(tx.id() % Integer.MAX_VALUE);
        StringBuilder b = new StringBuilder(290);
        for (int i = 0; i < 3; i++) {
            double value = tx.values()[i];
            if (!Double.isFinite(value)) throw new IllegalArgumentException("UPDATE value is not finite");
            if (!b.isEmpty()) b.append(';');
            b.append("UPDATE blocking_data SET data_value = ")
                    .append(Double.toString(value))
                    .append(", modified_by = ").append(actor)
                    .append(" WHERE data_id = ").append(ids[i]);
        }
        return b.toString();
    }

    static double pointRead(Statement st, int[] ids) throws SQLException {
        boolean resultAvailable = st.execute(readsSql(ids));
        double sum = 0;
        for (int i = 0; i < 3; i++) {
            if (!resultAvailable) throw new SQLException("Expected SELECT result " + (i + 1));
            try (ResultSet rs = st.getResultSet()) {
                if (rs == null || !rs.next()) throw new SQLException("Missing tuple id=" + ids[i]);
                sum += rs.getDouble(1);
                if (rs.next()) throw new SQLException("Point SELECT returned more than one tuple");
            }
            resultAvailable = st.getMoreResults();
        }
        if (resultAvailable || st.getUpdateCount() != -1) {
            throw new SQLException("Unexpected additional SQL result after three SELECTs");
        }
        return sum;
    }

    static void updateThree(Statement st, Workload.Tx tx) throws SQLException {
        boolean resultAvailable = st.execute(updatesSql(tx));
        for (int i = 0; i < 3; i++) {
            if (resultAvailable) throw new SQLException("Unexpected UPDATE result set");
            int changed = st.getUpdateCount();
            if (changed != 1) throw new SQLException("UPDATE " + (i + 1) + " changed " + changed + " rows; expected 1");
            resultAvailable = st.getMoreResults();
        }
        if (resultAvailable || st.getUpdateCount() != -1) {
            throw new SQLException("Unexpected additional SQL result after three UPDATEs");
        }
    }

    private static void requireThreeDistinct(int[] ids) {
        if (ids == null || ids.length != 3 || ids[0] == ids[1] || ids[0] == ids[2] || ids[1] == ids[2]) {
            throw new IllegalArgumentException("Expected three distinct row IDs");
        }
        for (int id : ids) if (id < 0) throw new IllegalArgumentException("Negative key: " + id);
    }
}
