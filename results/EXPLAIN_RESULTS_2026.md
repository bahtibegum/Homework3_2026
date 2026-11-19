# PostgreSQL EXPLAIN (ANALYZE, BUFFERS) — student measurements, 16 Nov 2026

These three numbers come from the pgAdmin screenshots supplied in this conversation, **before** the code fixes. They are observations of individual SQL statements, not Java transaction throughput.

| Statement | Plan | Planning time | Execution time | Shared hits | Rows relevant |
|---|---|---:|---:|---:|---|
| `SELECT data_value FROM blocking_data WHERE data_id = 123` | Index Scan (`blocking_data_pkey`) | 0.491 ms | 0.115 ms | 3 | 1 |
| `SELECT sum(data_value) FROM blocking_data` | Seq Scan + Aggregate | 0.065 ms | 28.202 ms | 450 | 65,564 scanned |
| `UPDATE blocking_data SET data_value = data_value + 1 WHERE data_id = 123` (rolled back) | Index Scan + Update | 0.179 ms | 0.851 ms | 5 | 1 target row |

Interpretation: the point read and update used the primary-key index appropriately. The full-table aggregation read substantially more rows and took longer in this standalone test. The scan is a *potential* CPU/memory-bandwidth bottleneck, not proof of a throughput bottleneck. All reported shared-buffer accesses were hits in the screenshot. The UPDATE was executed inside BEGIN/ROLLBACK and the rollback succeeded.

To strengthen Task 2, capture `pg_stat_activity` wait events during the benchmark and compare multiple runs before and after tuning. These values may differ after fixes or on another machine.
