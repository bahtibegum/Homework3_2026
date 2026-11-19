# Homework 3 (2026) — code review, fixes and verification

## Verified here (offline)

- Java 21 compilation with bundled PostgreSQL JDBC driver
- 100,000 deterministic transaction kinds; 60/10/10/20 proportions
- Three distinct keys in one point-read / update transaction
- Committed-INSERT registry tests: rows unavailable before publication, available afterward
- Common SQLSTATE retry classification (`40P01`, `40001`, `55P03`)

## Needs PostgreSQL on your computer

1. **Deadlock test:** `java -cp 'out;lib/*' homework3.DeadlockTest` — must print `PASS` with at least one observed deadlock. It uses a separate `homework3_deadlock_test` table, then drops it.
2. **Integration smoke test:** `java -cp 'out;lib/*' homework3.Main --mode concurrent --threads 4 --duration 5 --reset --csv results/smoke_after_fixes.csv` — should produce committed transactions, 10% INSERTs, no missing-tuple errors.
3. **After-fixes benchmarks:** `./scripts/run_matrix.ps1` in PowerShell — serial-1; concurrent-2/4/8; optimized-2/4/8, each 30 seconds. Results go to `results/benchmarks_after_fixes.csv`. The new sampling rule means historical measurements must not be silently merged into this CSV.
4. **Optimization evidence:** compare matched concurrent and optimized cases at 2/4/8 workers. Re-run if possible (alternating mode order) to distinguish noise. The `prepareThreshold=0` change **is not a proven improvement**. If it still loses, report the negative result and use the demonstrated serial-to-concurrent speed-up as the successful parallelism improvement, or profile/test a further transaction-runtime change.
5. **Correctness for inserts:** after a short run, read/write operations should sometimes target inserted IDs (>= 50,000). The self-test checks key generation; an optional additional integration check can use SQL activity tracing or a debug log.
6. **Final presentation remains for your group:** attach the actual query-plan findings, missing team contribution percentages, citations (including AI/previous code if used), and any further measured optimization. No slides were edited here.

## Review notes and caveats

- `Worker.loop` commits every transaction kind and retries the same generated transaction plan after SQLSTATE deadlock/serialization/lock failures, with rollback and randomized backoff. Write key order is not sorted.
- `InsertedRows` uses a short synchronized section to select distinct keys uniformly among rows *already registered as committed*, then releases it before JDBC calls; a committed INSERT could be published slightly later, so it may not immediately be eligible. This is safe and does not change the required transaction proportions.
- In a continuous multi-threaded benchmark, exact row IDs generated for a given transaction can vary across executions because committed INSERT visibility is schedule-dependent. The probability distribution and SQL semantics stay the same, but a fully identical row sequence is not guaranteed for runs of different concurrency.
- `optimized` is a client-side JDBC batching + prepared-statement caching hypothesis with pgJDBC server-side preparation disabled. Three individual ordered UPDATE statements are sent using `addBatch/executeBatch`, with counts checked and the batch cleared on retry. SQL statements and the random transaction mix are unchanged. JDBC batching is a candidate to reduce network round-trips, not guaranteed to win. No in-container PostgreSQL was available to measure the new outcome.
- Query-plan evidence shows one `FULL_SCAN` was expensive, but one `EXPLAIN ANALYZE` run does not prove it is the dominant system-wide bottleneck. The multiple parallel workers address the *serial throughput ceiling*. Additional PostgreSQL activity/profiler evidence would strengthen the case.
- Attribution: the 2026 assignment permits external ideas and AI if explained, licenses respected and sources cited in the talk. Historical references were excluded from this ZIP because they are not part of the executable solution, not as a reason to suppress attribution.
