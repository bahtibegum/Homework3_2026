# Homework 3 (2026): cached prepared statements performance experiment

This is a **test candidate**, not a claimed improvement. The previous trial showed
ordinary `concurrent` outperforming both JDBC batch (`optimized`) and `roundtrip` modes.

## What changed in this candidate

A new `--mode cached` reuses one `PreparedStatement` per worker for each of the four
SQL types. Unlike the previous `optimized` mode, it does **not** batch updates: all
three UPDATEs still run as three separate SQL statements, in the original order.
It uses the PostgreSQL JDBC driver's default `prepareThreshold`, allowing reuse
and potential server-side prepared statement benefit. This isolates statement
reuse from batching, so the trial can answer a clear performance question.

**Unchanged:** workload mix, distinct-key selection, committed INSERT visibility,
transaction commits/retries, database schema, logging, and existing execution modes.
The new mode is integrated in `Config.java` and `Benchmark.java`.

No presentation files were modified or included in this ZIP.

## How to test on Windows / IntelliJ

Back up the existing `Homework3_2026_Revised` folder, then extract this ZIP and
copy the **contents** of `Homework3_2026_Cached_Trial` into the original project
root (replace matching files). Do not paste inside `src` or `scripts`.

Open IntelliJ Terminal (PowerShell). Connection settings are temporary per terminal:

```powershell
$env:PG_URL = 'jdbc:postgresql://localhost:5434/homework3_2026'
$env:PG_USER = 'postgres'
$secret = Read-Host 'PostgreSQL password' -AsSecureString
$env:PG_PASSWORD = [System.Net.NetworkCredential]::new('', $secret).Password
.\scriptsuild.ps1
```

Test five seconds of the **new mode** first:

```powershell
java -cp 'out;lib/*' homework3.Main --mode cached --threads 4 --duration 5 --reset --csv results/cached_smoke.csv
```

If it completes, run the paired comparison:

```powershell
.\scriptsun_cached_trial.ps1
```

Upload **`results/cached_trial.csv`** to compare with baseline at the same worker
counts. Benchmark script refuses to overwrite existing cached_trial.csv; rename
old one before rerunning. Every `--reset` truncates the `blocking_data` table
in the selected database; use only your dedicated homework benchmark database.

The script generates 7 runs (serial + 3 concurrent + 3 cached), 30 seconds each,
and stores read sums as `.log` files. Throughput is measured as committed
transactions divided by elapsed time including drain. Avoid comparing results
from different workload versions, because INSERT visibility changed earlier.

**Interpretation:** a win needs consistent same-worker, same-workload comparisons,
ideally repeated to account for run-to-run variance. If cached still loses, do
not claim a performance improvement: document the negative result and consider
profiling PostgreSQL/JDBC overhead or evaluating other bottlenecks.
