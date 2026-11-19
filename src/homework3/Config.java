package homework3;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Command-line configuration. Credentials are read from environment variables. */
public record Config(String mode, int threads, int rows, int durationSeconds,
                     long transactions, long seed, String jdbcUrl, String user,
                     String password, boolean reset, boolean printReads, Path csvFile) {
    static Config parse(String[] args) {
        Map<String, String> opts = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--help") || a.equals("-h")) {
                usage();
                System.exit(0);
            }
            if (a.equals("--reset")) {
                opts.put("reset", "true");
            } else if (a.startsWith("--") && i + 1 < args.length) {
                opts.put(a.substring(2), args[++i]);
            } else {
                throw new IllegalArgumentException("Unexpected option/argument: " + a + " (use --help)");
            }
        }
        for (String key : opts.keySet()) {
            if (!java.util.Set.of("reset", "mode", "threads", "rows", "duration", "transactions",
                    "seed", "url", "user", "csv", "print-reads").contains(key)) {
                throw new IllegalArgumentException("Unknown option --" + key);
            }
        }
        String mode = opts.getOrDefault("mode", "optimized").toLowerCase();
        if (!java.util.Set.of("serial", "concurrent", "optimized", "roundtrip", "cached").contains(mode)) {
            throw new IllegalArgumentException("Mode must be serial, concurrent, optimized, roundtrip, or cached");
        }
        int threads = Integer.parseInt(opts.getOrDefault("threads", "4"));
        if (mode.equals("serial")) threads = 1;
        int rows = Integer.parseInt(opts.getOrDefault("rows", "50000"));
        int duration = Integer.parseInt(opts.getOrDefault("duration", "30"));
        long transactions = Long.parseLong(opts.getOrDefault("transactions", "0"));
        long seed = Long.parseLong(opts.getOrDefault("seed", "20261124"));
        if (threads < 1 || threads > 128 || rows < 3 || rows > 1_000_000
                || duration < 0 || transactions < 0 || (duration == 0 && transactions == 0)) {
            throw new IllegalArgumentException("Invalid threads/rows/duration/transactions setting");
        }
        String url = opts.getOrDefault("url", env("PG_URL", "jdbc:postgresql://localhost:5434/homework3_2026"));
        String user = opts.getOrDefault("user", env("PG_USER", "postgres"));
        String password = System.getenv().getOrDefault("PG_PASSWORD", "");
        boolean printReads = Boolean.parseBoolean(opts.getOrDefault("print-reads", "true"));
        Path csv = Path.of(opts.getOrDefault("csv", "results/benchmarks.csv"));
        return new Config(mode, threads, rows, duration, transactions, seed, url, user, password,
                Boolean.parseBoolean(opts.getOrDefault("reset", "false")), printReads, csv);
    }

    private static String env(String key, String fallback) {
        return System.getenv().getOrDefault(key, fallback);
    }

    private static void usage() {
        System.out.println("""
                Homework 3 (2026) - PostgreSQL continuous transaction benchmark
                java -cp "out:lib/*" homework3.Main [options]
                --mode serial|concurrent|optimized|roundtrip|cached
                                                      serial=1 worker; concurrent=prepare each tx;
                                                      cached=reuse prepared statements without batching;
                                                      optimized=earlier JDBC update-batch experiment;
                                                      roundtrip=three separate ordered SQL statements
                                                      delivered together in one JDBC call
                --threads N                           worker count (ignored in serial mode)
                --rows N                              initial tuples (default 50000)
                --duration N                          new transactions for N seconds (default 30)
                --transactions N                      optional cap (default 0 = unlimited)
                --seed N                              deterministic workload seed
                --reset                               CAUTION: TRUNCATES blocking_data and reseeds it
                --url jdbc:postgresql://...           override PG_URL; default localhost:5434/homework3_2026
                --user NAME                           override PG_USER; default postgres
                --csv PATH                            output CSV (default results/benchmarks.csv)
                --print-reads true|false              true required for assignment demo
                Environment: PG_PASSWORD, PG_USER, PG_URL. The --reset flag is REQUIRED for EACH experiment.
                Example: --mode optimized --threads 4 --duration 30 --reset
                """);
    }
}
