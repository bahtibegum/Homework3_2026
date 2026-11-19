package homework3;

import java.util.Arrays;

/** No PostgreSQL required. Smoke-tests the 2026 transaction generation rules. */
public final class SelfTest {
    public static void main(String[] args) {
        long[] types = new long[4];
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            Workload.Tx tx = Workload.make(i, 20261124L, 50_000);
            Workload.Tx again = Workload.make(i, 20261124L, 50_000);
            if (tx.kind() != again.kind() || !Arrays.equals(tx.dataIds(), again.dataIds())) {
                throw new AssertionError("Not reproducible for tx " + i);
            }
            types[tx.kind().ordinal()]++;
            if (tx.kind() == Workload.Kind.POINT_READ || tx.kind() == Workload.Kind.WRITE) {
                int[] ids = tx.dataIds();
                if (ids.length != 3 || ids[0] == ids[1] || ids[0] == ids[2] || ids[1] == ids[2]) {
                    throw new AssertionError("Tuple keys must be distinct");
                }
                for (int id : ids) if (id < 0 || id >= 50_000) throw new AssertionError("Invalid base key");
            }
            if (tx.kind() == Workload.Kind.INSERT && tx.insertionId() < 50_000) {
                throw new AssertionError("INSERT overwrites a base row");
            }
        }
        double[] expected = {.60, .10, .10, .20};
        for (int i = 0; i < 4; i++) {
            double observed = types[i] / (double) n;
            if (Math.abs(observed - expected[i]) > 0.015) {
                throw new AssertionError("Wrong transaction proportion " + i + ": " + observed);
            }
        }
        // Newly inserted rows are sampled ONLY once published after a successful commit.
        InsertedRows published = new InsertedRows(3);
        for (int i = 0; i < 100; i++) {
            for (int key : published.threeDistinct(new java.util.SplittableRandom(i))) {
                if (key < 0 || key >= 3) throw new AssertionError("Uncommitted INSERT visible");
            }
        }
        published.publishedCommit(10);
        published.publishedCommit(100);
        int newlySampled = 0;
        for (int i = 0; i < 1000; i++) {
            int[] ids = published.threeDistinct(new java.util.SplittableRandom(i));
            if (ids[0] == ids[1] || ids[0] == ids[2] || ids[1] == ids[2])
                throw new AssertionError("Not distinct with INSERT rows");
            for (int key : ids) {
                if (key == 10 || key == 100) newlySampled++;
                else if (key < 0 || key >= 3) throw new AssertionError("Unexpected row ID: " + key);
            }
        }
        if (newlySampled == 0 || published.size() != 5) throw new AssertionError("INSERT sampling failed");
        boolean selectedCommittedInsert = false;
        for (int i = 0; i < 1000; i++) {
            Workload.Tx tx = Workload.make(i, 20261124L, 3, published);
            if (tx.kind() == Workload.Kind.POINT_READ || tx.kind() == Workload.Kind.WRITE) {
                for (int key : tx.dataIds()) {
                    if (key == 10 || key == 100) selectedCommittedInsert = true;
                    else if (key < 0 || key >= 3) throw new AssertionError("Invalid generated key " + key);
                }
            }
        }
        if (!selectedCommittedInsert) throw new AssertionError("Workload generator ignores committed inserts");
        // The candidate sends exactly three separate SQL statements without changing key order.
        int[] order = {99, 17, 45};
        String readSql = RoundTrip.readsSql(order);
        if (readSql.split(";").length != 3 || !readSql.contains("data_id = 99;SELECT")
                || !readSql.contains("data_id = 17;SELECT") || !readSql.endsWith("data_id = 45")) {
            throw new AssertionError("Point queries must retain three ordered single-key SELECTs");
        }
        String updateSql = RoundTrip.updatesSql(new Workload.Tx(123, Workload.Kind.WRITE,
                order, new double[] {1.25, 2.5, 3.75}, -1));
        String[] statements = updateSql.split(";");
        if (statements.length != 3 || !statements[0].endsWith("data_id = 99")
                || !statements[1].endsWith("data_id = 17")
                || !statements[2].endsWith("data_id = 45")) {
            throw new AssertionError("UPDATE order changed or statement count not three");
        }
        for (String q : statements) if (!q.startsWith("UPDATE blocking_data SET data_value = "))
            throw new AssertionError("Incorrect UPDATE syntax");
        for (String state : new String[] {"40P01", "40001", "55P03"}) {
            if (!RetryPolicy.isRetryable(state)) throw new AssertionError("Not retrying " + state);
        }
        if (RetryPolicy.isRetryable("23505") || RetryPolicy.isRetryable(null))
            throw new AssertionError("Wrong retry classification");
        java.sql.SQLException wrappedBatch = new java.sql.BatchUpdateException(
                "batch failed", "00000", new int[0]);
        wrappedBatch.setNextException(new java.sql.SQLException("deadlock victim", "40P01"));
        if (!"40P01".equals(RetryPolicy.retryableState(wrappedBatch)))
            throw new AssertionError("Batch exception must expose nested SQLSTATE");
        System.out.printf("PASS: %d reproducible plans, distinct keys, mix: reads=%d, inserts=%d, scans=%d, writes=%d%n",
                n, types[0], types[1], types[2], types[3]);
    }
}
