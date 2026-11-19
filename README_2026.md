# Advanced Databases — Homework 3 (2026)

**New optimization trial (18 November):** Read `OPTIMIZATION_TRIAL_README.md` before running the new `roundtrip` mode; this original README covers the earlier fixed project and the `optimized` JDBC-batch variant.


## Status — code fixes made, PostgreSQL re-test required

The **Java 21 build and offline tests pass** after these fixes. The attached `results/benchmarks_before_fixes.csv` contains the user's genuine **16 November 2026** PostgreSQL results, which were produced **before** the changes. They are not performance results for this version. **Run the benchmark matrix again on the same Windows/PostgreSQL setup before claiming a gain.** No PowerPoint file was modified.

The professor requires a continuous 60/10/10/20 transaction mix, a serial baseline, scaling tests, performance evidence, an improvement and a talk. This project implements the workload, closed-loop worker stream, retry behavior, serial/concurrent and experimental prepared-statement reuse modes. Additional work on profiling and presentation is described in `FIXES_AND_TESTS.md`.

## Changes in this version

1. **Inserted rows become eligible for point reads and updates.** A thread-safe `InsertedRows` registry is populated only *after* successful INSERT commits. Each point-read or write transaction samples three distinct IDs uniformly from the original rows **and all INSERT rows currently published as committed**. It preserves the random key order. No missing/uncommitted row can be chosen. The 60/10/10/20 transaction type is still generated deterministically for each transaction ID, but the exact key sample can differ between concurrent runs because commit timing varies.
2. **Deadlock integration test added.** `homework3.DeadlockTest` deliberately creates a deadlock on a separate temporary-purpose table, verifies rollback and retry of the same two-update plan, and verifies both transactions eventually commit. It uses the same SQLSTATE classification and randomized backoff helper as the benchmark. **It has not been run against your PostgreSQL yet.**
3. **Experimental optimization adjusted.** The `optimized` mode now **batches its three ordered, single-tuple UPDATE statements with JDBC `addBatch/executeBatch`**, reducing client/server round trips without changing SQL or write-key order. It also reuses client-side JDBC prepared statements and sets pgJDBC's `prepareThreshold=0` to avoid named server-side prepared statements/generic plans. These are **hypotheses**, not proven speed-ups. The ordinary `concurrent` mode remains a valid and historically faster baseline. Do not label the change effective unless your new measurements show that.
4. **Benchmark script now records post-fix measurements separately**, in `results/benchmarks_after_fixes.csv`. It leaves the old CSV intact. This is important because the row-sampling rules changed.
5. **Old `reference_2023` folder removed** from the package. If older code was consulted/adapted, disclose it and any AI help as required by the professor; removing a folder does not change attribution requirements.

## Windows — IntelliJ IDEA / PowerShell / PostgreSQL 18

Open **this extracted project folder** in IntelliJ, using Java 21. In IntelliJ's terminal, set the PostgreSQL connection (replace `YOUR_PASSWORD`; port 5434 matched your last known setup):

```powershell
$env:PG_URL = 'jdbc:postgresql://localhost:5434/homework3_2026'
$env:PG_USER = 'postgres'
$env:PG_PASSWORD = 'YOUR_PASSWORD'
.\scripts\build.ps1
```

**PostgreSQL deadlock integration test** (does not change `blocking_data`):

```powershell
java -cp 'out;lib/*' homework3.DeadlockTest
```

Expected (after PostgreSQL's deadlock detection delay): `PASS: PostgreSQL deadlock triggered (...) ... both transactions committed.` It creates and removes only `homework3_deadlock_test`; do not run alongside other tests using that name.

**Five-second smoke test on a dedicated database**:

```powershell
java -cp 'out;lib/*' homework3.Main --mode concurrent --threads 4 --duration 5 --reset --csv results/smoke_after_fixes.csv
```

**Full 30-second benchmark matrix** (seven runs):

```powershell
.\scripts\run_matrix.ps1
```

This writes `results/benchmarks_after_fixes.csv` and redirects the required completed read-query sums to `results/*_after_fixes_read_sums.log`. To keep a clean matrix, rename/delete any existing `benchmarks_after_fixes.csv` *before* running the script again. The benchmark table is truncated and reloaded before **every** run.

### Safety warning

`--reset` **TRUNCATES ALL `blocking_data` ROWS** in the selected database. Use only your dedicated `homework3_2026` database. Do not run it against data you need to keep. Passwords are supplied through environment variables; do not put them in Git.

## What to measure and report

- **Throughput** = committed transactions / elapsed seconds (including any in-flight drain).
- **Speed-up** = concurrent/optimized throughput divided by serial throughput.
- Compare two, four and eight worker threads, including changes in latency and diminishing returns.
- Describe the closed-loop equilibrium: `N` workers each finish one transaction and immediately start another; no unbounded queue.
- The 2026 code targets serial execution as a bottleneck using concurrent workers; historical concurrent-8 yielded **5,899.014 tx/s** versus serial-1 **2,149.984 tx/s**, but those were **pre-fix** readings. The older `optimized` cache implementation was *slower* than ordinary concurrent. The new optimization trial is unverified.
- See `results/EXPLAIN_RESULTS_2026.md` for PostgreSQL plans that you captured, and `FIXES_AND_TESTS.md` for open checks.
- If you reuse an idea or code from an older project or AI, acknowledge it appropriately, as the assignment allows with attribution and licensing.

## Files

- `src/homework3/` — 2026 implementation and Java self-test + live deadlock test
- `scripts/build.ps1` / `scripts/run_matrix.ps1` — build and run benchmark
- `scripts/inspect_db.sql` — EXPLAIN/PostgreSQL inspection queries
- `results/benchmarks_before_fixes.csv` — **old measured results only**
- `results/REPORT_2026_BEFORE_FIXES.md` — previous measured analysis, not a post-fix claim
- `results/EXPLAIN_RESULTS_2026.md` — real SQL plan observations from your screenshots
- `FIXES_AND_TESTS.md` — what changed, which checks remain
- `slides_2026_measured.pptx`, `slides_2026_template.pptx` — **unchanged**; update later with your group
