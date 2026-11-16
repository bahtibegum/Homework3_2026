-- Run from psql to document bottlenecks (not while another reset is taking place).
SELECT current_setting('server_version') AS postgres_version;
SELECT relname, n_live_tup, seq_scan, idx_scan
FROM pg_stat_user_tables WHERE relname = 'blocking_data';
SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'blocking_data';
EXPLAIN (ANALYZE, BUFFERS) SELECT data_value FROM blocking_data WHERE data_id = 123;
EXPLAIN (ANALYZE, BUFFERS) SELECT sum(data_value) FROM blocking_data;
-- This UPDATE explains an actual write; ROLLBACK prevents changing benchmark data.
BEGIN;
EXPLAIN (ANALYZE, BUFFERS) UPDATE blocking_data SET data_value = 25, modified_by = 1 WHERE data_id = 123;
ROLLBACK;
-- During the active benchmark, use:
SELECT wait_event_type, wait_event, state, count(*) FROM pg_stat_activity
WHERE application_name LIKE 'HW3_2026_%'
GROUP BY wait_event_type, wait_event, state;
