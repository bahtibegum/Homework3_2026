package homework3;

/** Entry point. Run `homework3.SelfTest` before the database-backed benchmark. */
public final class Main {
    private Main() { }
    public static void main(String[] args) {
        try {
            Config cfg = Config.parse(args);
            System.err.printf("HW3 2026: mode=%s, workers=%d, duration=%ds, txLimit=%d, url=%s%n",
                    cfg.mode(), cfg.threads(), cfg.durationSeconds(), cfg.transactions(), cfg.jdbcUrl());
            System.err.printf("Host logical processors available to JVM: %d; Java: %s%n",
                    Runtime.getRuntime().availableProcessors(), System.getProperty("java.version"));
            if (!cfg.reset()) {
                throw new IllegalArgumentException("For a clean 2026 experiment, specify --reset. " +
                        "This TRUNCATES only blocking_data; use a dedicated test database.");
            }
            Database.reset(cfg);
            new Benchmark(cfg).execute();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
