# Advanced Databases — Homework 3 (2026)
## Benchmark results and interpretation — measured run of 16 November 2026

**Status:** Transaction generator, continuous benchmark, and seven 30-second runs completed successfully according to the submitted CSV. This report includes actual measured results but **does not yet include profiler output, PostgreSQL execution plans, or team-member contribution percentages**. Those must be supplied for an evidence-backed final presentation. The program has been tested locally by the student; these numbers come from the uploaded `benchmarks.csv`, not from the historical 2023 solution.

### 1. Assignment objective and workload

Homework 3 asks for **maximum committed-transaction throughput** under a continuous transaction stream, with comparison against a serial execution baseline. A random value `p` determines each new transaction:

| Probability | Operation | Implementation |
|---|---|---|
| `p < 0.60` (60%) | Point read | Three separate point SELECTs of distinct base tuples; sum printed after commit |
| `0.60 <= p < 0.70` (10%) | INSERT | One new tuple; inserts removed on subsequent `--reset` |
| `0.70 <= p < 0.80` (10%) | Full-table scan | `SELECT sum(data_value) FROM blocking_data` |
| `p >= 0.80` (20%) | Write | Three single-tuple UPDATE statements; original key order preserved |

A **closed-loop worker pool** provides the required continuous stream: each worker starts a new transaction after the previous one commits. At most `N` transactions are active for `N` workers; there is no unbounded backlog. Retryable errors (`40P01`, `40001`, `55P03`) cause rollback, randomized backoff, and retry of the **same** transaction plan. The implementation keeps its write order. All four categories contribute to the reported throughput.

**Modeling simplification:** Point queries and updates select only among the 50,000 original rows, although full-table sums include subsequently inserted rows. This is an implementation choice that should be mentioned during the talk. Also, the 30-second limit stops **generating** new transactions; already started operations drain. `actual_seconds` includes this drain but not initialization or connection creation.

### 2. Test environment and methodology

- OS: **Windows** (shown in the student's IntelliJ terminal).
- Java / compiler: **Eclipse Temurin OpenJDK 21.0.7** (verified in the student's terminal screenshots).
- PostgreSQL: pgAdmin showed a **PostgreSQL 18** server; verify the active benchmark server using `SELECT version()` (server version is not recorded in the CSV).
- Database URL shown in the terminal: `jdbc:postgresql://localhost:5434/homework3_2026`.
- PostgreSQL JDBC driver in the project: `postgresql-42.6.0.jar`.
- Available JVM logical processors: **8**, according to the run screenshot; exact CPU model and RAM are **not recorded**.
- Initial population: **50,000 rows**; each experiment resets and reloads `blocking_data`.
- Fixed workload seed: **20261124**. Each mode generates the same plan for a given transaction ID, but **different duration-based runs complete different numbers of plans**.
- Seven configurations: serial (1 worker); concurrent (2, 4, 8); reused-prepared-statement variant named optimized (2, 4, 8).
- One nominal **30-second** run per configuration. Logs contain per-read sums; the CSV contains throughput and latency aggregates.
- An earlier optimized four-worker **5-second** smoke test was recorded in the CSV but deliberately excluded from all comparative calculations.

**Limits of comparability:** This is a single pass per configuration, run sequentially. Warm-cache effects, runtime order, system load, increasing INSERT row counts during each trial, and different stream lengths may influence outcomes. The readings describe this run, not a statistically established performance ranking. The 60/10/10/20 plan distribution is consistent across runs but not exactly identical.

### 3. Measured results: committed transactions and throughput

Formula: `throughput (tx/s) = committed / actual_seconds`. The speed-up for each mode is `its throughput / 2,149.984`, using the serial baseline from the CSV.

| Mode | Workers | Committed | Actual s | Throughput (tx/s) | Speedup vs serial | Mean latency (ms) | Max latency (ms) |
|---|---:|---:|---:|---:|---:|---:|---:|
| Serial | 1 | 64,502 | 30.001 | 2,149.98 | 1.00× | 0.445 | 25.966 |
| Concurrent | 2 | 109,430 | 30.001 | 3,647.51 | 1.70× | 0.527 | 16.394 |
| Concurrent | 4 | 149,747 | 30.004 | 4,990.82 | 2.32× | 0.773 | 26.055 |
| Concurrent | 8 | 177,039 | 30.012 | 5,899.01 | 2.74× | 1.317 | 102.077 |
| Prepared-statement reuse ("optimized") | 2 | 80,919 | 30.005 | 2,696.86 | 1.25× | 0.711 | 166.178 |
| Prepared-statement reuse ("optimized") | 4 | 110,353 | 30.004 | 3,677.97 | 1.71× | 1.054 | 80.668 |
| Prepared-statement reuse ("optimized") | 8 | 155,374 | 30.010 | 5,177.35 | 2.41× | 1.511 | 197.168 |

**Highest measured throughput:** concurrent execution with 8 workers, **5,899.01 commits/s** versus **2,149.98 commits/s** in the serial baseline. This is **2.74×** serial throughput (approximately **174.4%** higher), but far short of the ideal **8×** linear speed-up. The 8-worker prepared-statement-reuse variant reached **5,177.35 commits/s**, or **2.41×** serial.

Across the seven formal experiments, the total is **847,364 committed transactions**. Observed aggregate operation mix:

| Category | Transactions | Share | Target |
|---|---:|---:|---:|
| Point reads | 509,093 | 60.08% | 60% |
| INSERT | 84,732 | 10.00% | 10% |
| Full-table scans | 83,928 | 9.90% | 10% |
| Write | 169,611 | 20.02% | 20% |

All seven formal CSV rows report **0 retry attempts, 0 deadlocks, 0 serialization failures, and 0 lock-not-available events**. This supports the statement that **no such errors occurred in these trials**; it does **not** demonstrate that retry/backoff works under an actual deadlock. A separate induced-conflict test would be necessary to verify that path.

### 4. Concurrent scaling and the attempted optimization

Two modes use a separate JDBC connection per worker and identical workload semantics:
- `concurrent`: allocates/closes the required prepared statement for each transaction.
- `optimized`: caches four prepared statements for reuse on each worker's connection.

| Workers | Concurrent throughput | Reused-prepared throughput | Change when reusing |
|---:|---:|---:|---:|
| 2 | 3,647.51 | 2,696.86 | -26.1% |
| 4 | 4,990.82 | 3,677.97 | -26.3% |
| 8 | 5,899.01 | 5,177.35 | -12.2% |

**Unexpected finding:** Prepared-statement reuse was **slower** at all three tested concurrency levels. The relative decreases were approximately **26.1% (2 workers), 26.3% (4 workers), and 12.2% (8 workers)**. The change therefore **does not establish a successful throughput optimization**, despite its name and intent. This is a central result to present candidly. PostgreSQL JDBC driver behavior, server plan caching, measurement noise, query mix, and server load are **possible** factors, but **none has been established as the cause** by the CSV alone.

Scaling is non-linear: adding more workers produces gains, but they diminish relative to the number of workers. Average transaction latency also rises with more concurrency: from **0.445 ms** (serial) to **1.317 ms** (concurrent, 8 workers), and from **0.711 ms** to **1.511 ms** when increasing the reused-statement variant from 2 to 8 workers. Latency is measured per committed transaction, including its attempts; max latency from the CSV is a single observed maximum, not a percentile.

### 5. Bottlenecks and targeted evidence collection — not yet proven

The assignment asks to identify and *solve* some existing bottlenecks. The current measurements only show non-linear scaling and a slower attempted optimization; they do not isolate root causes. Do **not** report any of the following candidates as a confirmed profiler finding until measured:

1. **Full-table aggregate:** `SELECT sum(data_value) FROM blocking_data` potentially processes all current rows, a larger scan as INSERTs accumulate. Measure with PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)` and table statistics.
2. **Client-side statement/cache behavior:** Compare the allocation/parse/JDBC costs of the `concurrent` and `optimized` modes under repeat runs and Java Flight Recorder or VisualVM. One name in the code does not prove faster execution.
3. **Contention / resource saturation / output:** Record PostgreSQL waiting events, per-session activity, CPU utilization and console/logging overhead during 2/4/8-worker runs. Redirection to log files itself can affect timing; use consistent settings.

Suggested PostgreSQL queries (Query Tool, against `homework3_2026`):

```sql
SELECT version();
EXPLAIN (ANALYZE, BUFFERS)
SELECT data_value FROM blocking_data WHERE data_id = 123;
EXPLAIN (ANALYZE, BUFFERS)
SELECT sum(data_value) FROM blocking_data;
SELECT wait_event_type, wait_event, state, count(*)
FROM pg_stat_activity
WHERE application_name LIKE 'HW3_2026_%'
GROUP BY wait_event_type, wait_event, state;
```

**Important:** `pg_stat_activity` is most informative while the benchmark is running; the `EXPLAIN (ANALYZE)` statements can run afterward. Take screenshots or save text output. Collect profiler screenshots with acquisition method, date, and mode/thread count.

**Recommended controlled follow-up:** Repeat 2/4/8-worker `concurrent` and `optimized` pairs multiple times, alternate execution order, report medians and spread, and run a fixed-number-of-transactions comparison (`--duration 0 --transactions 10000 --reset`) for equivalent transaction plans. If statement reuse still loses, either present it as a **failed attempted improvement**, or implement and measure another justified bottleneck fix after profiling. Do not fabricate a positive optimization result.

### 6. Problems encountered and answers for the talk

Observed setup challenges (from the screenshots and conversation): Java 21 was already installed but had to be selected in IntelliJ; pgAdmin gave a catalog-column error when creating the database, and attempting `CREATE DATABASE` later showed that `homework3_2026` already existed. The JDBC connection was then successfully configured on port `5434`, the generator self-test passed on **100,000** plans, and the program ran the database-backed benchmark. The 2026 workload and continuous-stream requirements are distinct from the legacy 2023 artifact.

**Key conclusions:** All four transaction kinds were committed with nearly the target mix; bounded continuous stream and multi-thread scaling were observed; eight-worker concurrent execution was the best of the measured configurations; prepared-statement reuse did not improve throughput in this trial; retry paths were not exercised because no conflicts were recorded. Further profiling and proof of an effective bottleneck fix remain open.

### 7. Live demonstration and reproducibility

In IntelliJ PowerShell, after setting `PG_URL`, `PG_USER`, `PG_PASSWORD` and running `scripts/build.ps1`, use:

```powershell
java -cp 'out;lib/*' homework3.Main --mode concurrent --threads 8 --duration 30 --reset
```

For a direct comparison, run optimized mode similarly, **but make clear it measured slower**. The `--reset` flag truncates and repopulates the `blocking_data` table. Use the dedicated homework database, not another database containing valuable rows.

### 8. Remaining submission items / academic integrity

- **Team names and contribution shares:** `[ADD TEAM MEMBER NAMES AND PERCENTAGES, TOTAL 100%]`. Never invent contribution percentages.
- **Measured PostgreSQL plan/profiler evidence:** `[ADD REAL EXPLAIN / JFR / pg_stat_activity RESULTS]`.
- **Actual tested bottleneck improvement and before/after data:** `[ADD IF AVAILABLE; CURRENT ATTEMPT FAILED]`.
- **Hardware model / RAM and verified PostgreSQL server version:** `[ADD]`.
- **Citations:** Cite assignment, any legacy 2023 project consulted, and any AI assistance or external snippets in accordance with the course FAQ / license requirements.

**Evidence:** Uploaded `benchmarks.csv` (2026-11-16; seven 30-second experiments and one five-second pre-test), project Java source, and 2026 homework brief. Do not present past students' results as new experiments.
