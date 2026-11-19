#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/build.sh
mkdir -p results
# Changing --duration 30 to 5 gives a fast local smoke test. Each --reset cleans the table.
# Write post-fix benchmarks to a NEW CSV; older measurements are not from the same workload.
for variant in 'serial 1' 'concurrent 2' 'concurrent 4' 'concurrent 8' 'optimized 2' 'optimized 4' 'optimized 8'; do
  read -r mode threads <<< "$variant"
  echo "=== $mode | $threads workers ==="
  java -cp 'out:lib/*' homework3.Main --mode "$mode" --threads "$threads" --duration 30 \
     --reset --csv results/benchmarks_after_fixes.csv > "results/${mode}_${threads}_after_fixes_read_sums.log"
done
