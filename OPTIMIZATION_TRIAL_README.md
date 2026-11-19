# Homework 3 (2026) — optimization trial (PowerPoint left unchanged)

## What this ZIP contains

This is a **modified version of the fixed-code ZIP you uploaded**. That ZIP was a copy of the earlier distributed project; it did **not** contain the latest IntelliJ files or the 18 November CSV. I added the actual 18 November seven-run CSV under `results/benchmarks_2026-11-18_before_roundtrip.csv` from your separately uploaded benchmark.

Your tested modes are preserved, including the working PostgreSQL deadlock test. Added a **fourth experimental mode: `roundtrip`**. This mode groups *three separate ordered SQL statements* (not one combined SELECT/UPDATE operation) into **one JDBC `Statement.execute()` invocation** for 3 point reads or 3 single-row UPDATEs. It consumes three separate JDBC results/update counts, verifies each UPDATE affected exactly one row, and prints the summed read values after commit as before. INSERT and full-table SUM still use the original prepared SQL, and the original random transaction mix, key order, retry policy, cleanup and serial baseline remain unchanged. The database table schema is unchanged.

The candidate avoids two client/server round trips for most read/write transactions, but generates SQL text and uses JDBC multi-results; **there is no guarantee it will be faster on your machine**. This variant uses `Statement` rather than a parameterized `PreparedStatement` for point reads; all interpolated values are locally generated `int`/finite `double`, not user input. Explain this alteration in the talk if selected, because the professor gives a prepared SELECT as an *example*, not an absolute requirement.

## Tests already run here

- **Java 21 build** and 100,000-transaction SelfTest with the additional ordered three-statement checks.
- I **cannot run PostgreSQL locally in this environment**; actual JDBC multi-result compatibility and throughput must be verified on your server before choosing `roundtrip` as the final optimization.
- Your prior real `homework3.DeadlockTest` passed on Windows and remains unchanged.

## Steps in IntelliJ on Windows (run before uploading to GitHub)

**1. Back up** your current `Homework3_2026_Revised` folder, then copy the *contents* of this ZIP's project folder into it (replace matching files).

**2. Open IntelliJ terminal** in `Homework3_2026_Revised`. Reconfigure environment variables if it is a fresh terminal:

```powershell
$env:PG_URL = 'jdbc:postgresql://localhost:5434/homework3_2026'
$env:PG_USER = 'postgres'
$secret = Read-Host 'Enter PostgreSQL password' -AsSecureString
$env:PG_PASSWORD = [System.Net.NetworkCredential]::new('', $secret).Password
.\scripts\build.ps1
```

**3. Run a 5-second integration check** (required before the new matrix):

```powershell
java -cp 'out;lib/*' homework3.Main --mode roundtrip --threads 4 --duration 5 --reset --csv results/roundtrip_smoke.csv
```

Look for **zero errors and nonzero committed transactions**. This check actually executes all four transaction types on PostgreSQL and validates all three point read results and all three ordered UPDATE counts. Use a separate dedicated test database: `--reset` truncates `blocking_data`.

**4. Run the comparison matrix** (10 x 30-second tests, including old and new modes):

```powershell
.\scripts\run_optimization_trial.ps1
```

Upload `results/optimization_trial.csv` here. The script deliberately refuses to overwrite an existing trial CSV: move/rename the file before repeated trials, then compare paired runs. It creates mode-specific `.log` output files containing the required read sums; those logs are untracked by `.gitignore`.

**5. Do not claim an improvement until measured.** Compare `roundtrip` and the original `concurrent` at 2, 4 and 8 workers, and compare both to serial. If `roundtrip` loses, keep the ordinary concurrent version, document the unexpected result, and investigate PostgreSQL profiling. Do not compare this trial's throughput to the old 16 Nov results (workload definitions changed then).

### Your measured 18 November baseline, from the separately uploaded CSV

| Mode | Workers | Throughput (tx/s) |
|---|---:|---:|
| Serial | 1 | 2,217.90 |
| Concurrent | 2 | 3,734.95 |
| Concurrent | 4 | 4,742.25 |
| Concurrent | 8 | 5,811.52 |
| Optimized (JDBC batch) | 2 | 2,907.58 |
| Optimized (JDBC batch) | 4 | 4,305.69 |
| Optimized (JDBC batch) | 8 | 5,406.13 |

Your real deadlock test already passed. All previous seven-run throughput trials reported zero deadlocks, as expected for the relatively low-conflict workload. `RoundTrip` must still be checked under normal PostgreSQL execution, especially with the multi-result SQL driver behavior.

### Presentation and source attribution

No PowerPoint was edited. The input ZIP's slides are included exactly as provided, even if you removed them from your local IntelliJ folder; delete/ignore them if you prefer. The assignment requires team contributions and proper attribution for any reused code/AI use in the final talk.
