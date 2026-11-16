# Homework 3 (2026) — Benchmark & analysis report (fill after running)

> **Do not use this document as evidence until the blanks contain measurements from your own machine.** The archived 2023 results are not interchangeable with 2026.

## 1. Objective and environment

Goal: maximize the number of **committed transactions per second** for a continuous generated stream, compared with a serial execution baseline. Initialization and creating JDBC connections are excluded from the measurement.

- Presenter(s) and team members: **[FILL]**
- Date, CPU model / number of logical processors, available RAM: **[FILL]**
- OS, Java version (e.g. `java -version`), PostgreSQL version (`SELECT version()`), PostgreSQL JDBC version: **[FILL]**
- Database `blocking_data`: 50,000 initial rows; PK `data_id`, `data_value`, `modified_by`; random initial values 0–50.
- Hardware: client and PostgreSQL server on **[same/different machine]**.
- One benchmark length: 30 seconds of new work; started transactions drain to commit after generation ends.

## 2. Transaction generation (Task 1)

At creation, each transaction gets a reproducible pseudorandom `p`. If p < 0.6, choose a three-distinct-tuple `SELECT` workload and print sum after successful commit; if 0.6 ≤ p < 0.7, insert one tuple; if 0.7 ≤ p < 0.8, sum entire `blocking_data`; otherwise run three separate single-row updates without changing the write order. Prior inserted rows are deleted by the `--reset` setup, not counted as part of benchmark timing.

**Continuous-stream equilibrium:** Each of N fixed workers immediately creates a new transaction after committing its previous one, so the number of in-flight transactions is at most N and does not accumulate as a growing queue. With no unbounded arrival rate, long-run admissions naturally match completed commits. A worker retries the same transaction after PostgreSQL deadlock/serialization/lock-unavailable exceptions using randomized backoff. The transaction's SQL order and random parameters are unchanged on retry.

## 3. Results and scaling (paste actual CSV data)

| Mode | Workers | Committed | Elapsed (s) | Throughput (tx/s) | Speed-up vs serial | Retries | Deadlocks |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| serial | 1 | [FILL] | [FILL] | [FILL] | 1.00 | [FILL] | [FILL] |
| concurrent | 2 | [FILL] | [FILL] | [FILL] | [FILL] | [FILL] | [FILL] |
| concurrent | 4 | [FILL] | [FILL] | [FILL] | [FILL] | [FILL] | [FILL] |
| concurrent | 8 | [FILL] | [FILL] | [FILL] | [FILL] | [FILL] | [FILL] |
| optimized | 2 | [FILL] | [FILL] | [FILL] | [FILL] | [FILL] | [FILL] |
| optimized | 4 | [FILL] | [FILL] | [FILL] | [FILL] | [FILL] | [FILL] |
| optimized | 8 | [FILL] | [FILL] | [FILL] | [FILL] | [FILL] | [FILL] |

Formula: `throughput = successful_commits / actual_elapsed_seconds`. Include read-only commits. `speedup = throughput_of_variant / throughput_serial`.

Observed distribution: point-read **[ ]%** / INSERT **[ ]%** / full scan **[ ]%** / WRITE **[ ]%**. Random sampling will not yield exactly 60/10/10/20 for small numbers of transactions.

## 4. Bottleneck analysis (Task 2): evidence, not guesses

**Measure before claiming an actual bottleneck.** Suggestions for experiments:

1. Run `scripts/inspect_db.sql` in `psql` and save output. Point READ / WRITE should typically exploit the primary-key index; full sum usually scans many rows. Record true plans, time and buffers.
2. Inspect `pg_stat_activity.wait_event`, and optionally server deadlock logs / `pg_stat_statements` (if installed and enabled). Save the observations and database settings.
3. Use VisualVM, JFR, or another Java profiler during the benchmark; record costs from JDBC calls, `System.out` output, and statement preparation. Indicate sampling settings and screenshot evidence.
4. Run at 1/2/4/8 workers. Check whether more concurrency increases DB waiting, contention, or console serialization.

| Suspected bottleneck | Measurement/profiler evidence (FILL) | Concrete fix tried | Throughput change (FILL) |
| --- | --- | --- | --- |
| Preparing statements for each transaction | [FILL] | Per-worker prepared SQL reuse in optimized mode | [FILL] |
| Database full scans | [FILL] | Describe tested tuning, *without changing required query* | [FILL] |
| Console I/O / row-lock contention / other | [FILL] | Describe only changes actually implemented and benchmarked | [FILL] |

**Note:** The code currently has one implemented optimization: reuse of prepared statements. If profiler evidence does not support this as a bottleneck or it makes performance worse, say so and test a measured alternative before presenting it as successful.

## 5. Unexpected findings, challenges and individual contribution

- Expected vs actual scalability: **[FILL]**
- Any unexpected slower/faster optimized throughput: **[FILL with real evidence]**
- Deadlock/serialization retry rate and stability: **[FILL]**
- Technical issues faced and how resolved: **[FILL]**
- Team member A: **[NAME, percentage, work]**
- Team member B: **[NAME, percentage, work]**
- Other team members: **[FILL]** (contributions total 100%)
- Sources and AI assistance used: **[FILL, including historical reference if used]**

## 6. Live demo (30 seconds)

Use PowerShell (after `build.ps1` and setting `PG_PASSWORD`):

```powershell
java -cp 'out;lib/*' homework3.Main --mode optimized --threads 4 --duration 30 --reset
```

Show `COMMIT tx=... type=... sum=...` output during the demo and final throughput. `--reset` deletes prior benchmark data from `blocking_data`, so never run against an important production database.
