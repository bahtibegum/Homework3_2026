# Read this first — post-fix project

The 2026 Java code was reviewed and fixed after the **16 November 2026** benchmarks. Start with `README_2026.md` and `FIXES_AND_TESTS.md`.

- `results/benchmarks_before_fixes.csv` contains the **original measured results** (not this revised code's results).
- `results/REPORT_2026_BEFORE_FIXES.md` analyzes the original run; its assumptions about selecting only base rows and the optimized mode **no longer describe the revised code**.
- `results/EXPLAIN_RESULTS_2026.md` records the three PostgreSQL plans observed in the student's screenshots.
- `scripts/run_matrix.ps1` generates **new** `results/benchmarks_after_fixes.csv` measurements on your PostgreSQL instance.
- The PowerPoint files have **not been edited**.

**Before submission:** run Java build, `DeadlockTest` integration check, complete benchmark matrix, and document the effect of the new optimization. Add team percentages/citations when the presentation is finalized.

**Warning:** The benchmark's `--reset` truncates `blocking_data` — use only a disposable homework database.
