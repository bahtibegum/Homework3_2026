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
        System.out.printf("PASS: %d reproducible plans, distinct keys, mix: reads=%d, inserts=%d, scans=%d, writes=%d%n",
                n, types[0], types[1], types[2], types[3]);
    }
}
