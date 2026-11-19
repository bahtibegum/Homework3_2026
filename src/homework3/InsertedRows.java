package homework3;

import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * Small shared registry of successfully COMMITTED INSERT rows.
 * The monitor covers only a brief snapshot/key selection or publication, never JDBC work.
 * The key generator therefore never chooses an uncommitted/rolled-back INSERT row.
 */
final class InsertedRows {
    private final int initialRows;
    private int[] committedIds = new int[1024];
    private int committedCount;

    InsertedRows(int initialRows) {
        if (initialRows < 3) throw new IllegalArgumentException("At least three initial rows required");
        this.initialRows = initialRows;
    }

    synchronized void publishedCommit(int key) {
        if (key < initialRows) throw new IllegalArgumentException("INSERT key conflicts with an initial row");
        if (committedCount == committedIds.length) {
            committedIds = Arrays.copyOf(committedIds, Math.multiplyExact(committedIds.length, 2));
        }
        committedIds[committedCount++] = key;
    }

    synchronized int[] threeDistinct(SplittableRandom rng) {
        int available = initialRows + committedCount;
        // Uniform sample of three distinct positions among all committed, existing tuples.
        int a = rng.nextInt(available);
        int b;
        do { b = rng.nextInt(available); } while (b == a);
        int c;
        do { c = rng.nextInt(available); } while (c == a || c == b);
        // Preserve original random key order; no canonical lock ordering for writes.
        return new int[] {keyAt(a), keyAt(b), keyAt(c)};
    }

    private int keyAt(int position) {
        return position < initialRows ? position : committedIds[position - initialRows];
    }

    synchronized int size() { return initialRows + committedCount; }
}
