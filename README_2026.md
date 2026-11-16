# Advanced Databases – Homework 3 (2026): revised Java/PostgreSQL solution

**Status:** Java 21 compilation and a 100,000-transaction generator self-test **passed** in the preparation environment. **PostgreSQL execution, performance results, query plans and profiling were not tested here** because no running PostgreSQL server was available. Execute the experiment matrix on your machine before submission. This is a working **implementation draft**, not a verified final lab submission with measured results.

## What was wrong with the supplied `group1_homework3.zip`?

The archive contains a **2023** solution (old report dated 22 November 2023), not the new 2026 assignment. The historical code uses mostly write-only range updates, a fixed number of tasks, and execution-time comparisons. Some variants hard-code the worker pool size or print results from historical machines. The 2026 task explicitly requires a different workload, continuous execution, throughput measurement, and a serial baseline.

## Mapping to the 2026 marking criteria

| 2026 requirement | Where addressed | Evidence / remaining action |
| --- | --- | --- |
| Generate p independently for each transaction | `Workload.make` | Deterministic pseudorandom per-transaction stream; self-test passed |
| 60% three distinct point reads; sum printed after commit | `Workload.POINT_READ`, `Benchmark.runStatements` | 3 separate prepared `SELECT data_value ... WHERE data_id = ?` queries |
| 10% INSERT; clean up on restart | `Workload.INSERT`, `Database.reset` | Insert unique new rows; **`--reset` truncates and reseeds** before each run |
| 10% full-table read sum | `Workload.FULL_SCAN` | One `SELECT sum(data_value) FROM blocking_data` |
| 20% write query; three single-tuple updates | `Workload.WRITE` | Three `UPDATE ... WHERE data_id = ?`, same random order on retry |
| Continuous stream; explain equilibrium | `Worker.loop` | Fixed N workers: each completes one transaction and immediately generates the next; active work stays bounded by N, no unbounded queue |
| Throughput and scaling | `Benchmark.printAndStore`, `scripts/run_matrix.*` | Committed transactions / elapsed seconds, 1/2/4/8 workers; **run actual measurements** |
| 30-second live demo | `--duration 30` | Progress logs + per-read sums; **record your own actual demo** |
| Serial execution baseline | `--mode serial` | Exactly one worker, same workload generator |
| Find bottlenecks with Java/Postgres tools | `scripts/inspect_db.sql` and report template | Explain plans and suggested analysis; **collect real profiler and server evidence** |
| Improve bottlenecks | `--mode optimized` | Per-worker prepared-statement reuse instead of preparing for every transaction; **measure whether it actually helps** |
| Compare results, surprises, challenges, contributions and slides | `results/REPORT_TEMPLATE.md`, `slides_2026_template.pptx` | Fill real benchmark numbers, observations, screenshots and contributions before submitting |

## Important behavioral details

- **Only these four workloads occur:** POINT_READ (p < 0.60), INSERT (0.60 ≤ p < 0.70), FULL_SCAN (0.70 ≤ p < 0.80), WRITE (p ≥ 0.80). Observed fractions vary around target fractions due to sampling.
- All point selects and updates use **three distinct keys from the 50,000 base tuples**. This ensures each returns/updates an existing tuple, including during concurrent inserts. Inserts are visible to full-table scans and are removed at reset, but are not sampled by the point-read key generator. This is a workload-design simplification worth disclosing in the presentation.
- Every worker uses its **own JDBC connection** and a transaction with `autoCommit=false` and `READ COMMITTED`. Read-only transactions are explicitly committed too.
- `SQLSTATE 40P01` (deadlock), `40001` (serialization), and `55P03` (lock unavailable) cause **rollback + randomized exponential backoff + retry of the same transaction plan**. Other SQL errors are reported and stop the benchmark instead of silently counting an uncommitted task.
- The 3 writes stay in their generated order, as the assignment explicitly forbids canonical locking.
- When the requested generation time expires, started transactions **drain to commit**; actual measured elapsed time includes this drain. Table initialization and initial JDBC connection creation are excluded.
- An `INSERT` uses a unique integer key `baseRows + transactionId`; a `--reset` eliminates all previous inserted rows. The program **requires `--reset`** before each benchmark, to prevent contamination from prior inserts. Beware: it **DELETES ALL `blocking_data` ROWS**.
- `--print-reads true` (default) outputs each point-read/full-scan sum to standard output after a successful commit. For repeatable measurements, redirect output to a log file (script already does this); the assignment's console output requirement is preserved. Setting it to false is a diagnostics-only option and is **not compliant** with the demonstration requirement.

## Run on Windows (Java 21, IntelliJ, PostgreSQL)

1. Ensure PostgreSQL is running and a database exists, e.g. `task2_db`. Use the existing one only if you accept resetting its `blocking_data` table.
2. Use **Java 21** (works with the bundled PostgreSQL JDBC `42.6.0` driver) and PowerShell inside the unzipped project folder.
3. Set the database connection environment variables for your current PowerShell session (replace examples with real settings):

```powershell
$env:PG_URL = 'jdbc:postgresql://localhost:5434/task2_db'
$env:PG_USER = 'postgres'
$env:PG_PASSWORD = 'YOUR_POSTGRES_PASSWORD'
.\scripts\build.ps1
```

4. First short **live database test** (writes results to `results/benchmarks.csv`):

```powershell
java -cp 'out;lib/*' homework3.Main --mode optimized --threads 4 --duration 5 --reset
```

5. Compare the strategies with the **full 30-second matrix** and fresh table for every experiment:

```powershell
.\scripts\run_matrix.ps1
```

Results will be appended to `results/benchmarks.csv`. Console read sums are captured into `results/*_read_sums.log`. To avoid mixing previous experiments, delete or rename the existing CSV before a complete new matrix. You can perform a controlled comparison of **exactly the same set of transaction plans** with `--duration 0 --transactions 10000 --reset` for each mode, in addition to the continuous-stream benchmarks.

## Run on Linux / macOS

```bash
export PG_URL='jdbc:postgresql://localhost:5432/task2_db'
export PG_USER='postgres'
export PG_PASSWORD='YOUR_POSTGRES_PASSWORD'
./scripts/build.sh
java -cp 'out:lib/*' homework3.Main --mode optimized --threads 4 --duration 30 --reset
./scripts/run_matrix.sh
```

## How to interpret results

- `throughput_per_second = committed / actual_seconds`; every transaction type is included.
- **Relative speedup** for an N-thread variant = variant throughput / one-thread serial throughput.
- More workers does **not** guarantee linear scaling: PostgreSQL scans, row-lock waiting, server CPU/I/O, client console output and JDBC calls may limit speed.
- `concurrent` (non-caching) creates the SQL prepared statement for **each transaction**; `optimized` reuses four prepared statements per worker. Both use the same SQL semantics. The actual benefit needs measurement. This change primarily targets client/parse overhead rather than insert initialization or JDBC connection creation.
- `results/REPORT_TEMPLATE.md` describes a profiling/evidence workflow, possible unexpected outcomes, and the mandatory presentation content.

## Submission and academic attribution

**Do not submit the legacy 2023 PDF or PPTX as 2026 evidence.** Those files remain in your original uploaded ZIP, and are not copied into this updated submission package. This revised Java solution was reimplemented while inspecting the supplied archive; disclose use of the old student example and any AI assistance in your talk, and comply with your course's citation/licensing requirements. The original archive reports Begüm Tosun, Partow Moradi Khushkanab and Anton Urbanek as its 2023 authors, and old Java sources reproduce an MIT-style original-source permission notice. Do not claim other people's measurements, screenshots, contributions, or presentations as your own. Fill your **actual team-member contributions (%)** on the slide template.

## Known limits / outstanding work

- This environment did not provide a PostgreSQL server, so no live JDBC integration test, SQL `EXPLAIN ANALYZE` output, profiler screenshots or honest throughput figures can be supplied yet.
- The `optimized` solution is a concrete candidate improvement; if benchmarks show negligible gains, profile and refine it and explain the actual outcome. The professor evaluates the measured evidence and explanation, not just compiling Java.
- Both strategies are safe under the listed recoverable transaction errors; a nonrecoverable DB/permission/network error aborts visibly. This is preferable to falsely reporting all transactions as committed.
- With large numbers of inserts, `data_id` INTEGER eventually limits the transaction ID range; irrelevant to ordinary 30-second tests but avoid indefinite multi-day runs without reset.
