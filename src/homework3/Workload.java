package homework3;

import java.util.SplittableRandom;

/** Deterministic plans for comparable experiment streams. Each transaction draws p independently. */
public final class Workload {
    public enum Kind { POINT_READ, INSERT, FULL_SCAN, WRITE }
    public record Tx(long id, Kind kind, int[] dataIds, double[] values, int insertionId) { }

    private Workload() { }

    public static Tx make(long id, long seed, int initialRows) {
        return make(id, seed, initialRows, null);
    }

    /** With a registry, read/write keys include all committed INSERT rows as they become visible. */
    static Tx make(long id, long seed, int initialRows, InsertedRows insertedRows) {
        // A separate pseudorandom stream for each transaction: retries NEVER change its operation.
        SplittableRandom rng = new SplittableRandom(seed ^ mix64(id));
        double p = rng.nextDouble(); // [0, 1)
        Kind kind = p < 0.60 ? Kind.POINT_READ
                : p < 0.70 ? Kind.INSERT
                : p < 0.80 ? Kind.FULL_SCAN : Kind.WRITE;
        int[] keys = (kind == Kind.POINT_READ || kind == Kind.WRITE)
                ? (insertedRows == null ? threeDistinct(rng, initialRows) : insertedRows.threeDistinct(rng))
                : new int[0];
        double[] values = new double[3];
        for (int j = 0; j < values.length; j++) values[j] = rng.nextDouble(0.0, 50.0);
        // Using a transaction-derived key avoids concurrent INSERT key collisions, and --reset
        // removes all inserted rows from a previous experiment.
        long insertKey = (long) initialRows + id;
        if (insertKey > Integer.MAX_VALUE) throw new IllegalStateException("INSERT key overflow");
        return new Tx(id, kind, keys, values, (int) insertKey);
    }

    static int[] threeDistinct(SplittableRandom rng, int bound) {
        if (bound < 3) throw new IllegalArgumentException("At least three rows are required");
        int a = rng.nextInt(bound);
        int b;
        do { b = rng.nextInt(bound); } while (b == a);
        int c;
        do { c = rng.nextInt(bound); } while (c == a || c == b);
        // Keep the generated order! Homework 3 forbids canonical lock ordering for writes.
        return new int[] {a, b, c};
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
