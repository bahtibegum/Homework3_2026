$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
& .\scripts\build.ps1
if ($LASTEXITCODE -ne 0) { throw 'Java build failed' }
New-Item -ItemType Directory -Force -Path results | Out-Null
# The exact working database must be a dedicated homework database.
# Every run resets only that database's blocking_data table.
# PG_URL, PG_USER, PG_PASSWORD must be set in this PowerShell session.
$csv = 'results/cached_trial.csv'
if (Test-Path $csv) { throw "$csv already exists. Rename it first to preserve earlier data." }
# Interleave candidate and baseline to reduce drift and cache-order bias.
$variants = @(
  @('serial',1),
  @('concurrent',2), @('cached',2),
  @('cached',4), @('concurrent',4),
  @('concurrent',8), @('cached',8)
)
foreach ($v in $variants) {
  $mode = [string]$v[0]; $threads = [int]$v[1]
  $log = "results/${mode}_${threads}_cached_trial_read_sums.log"
  Write-Host "Running $mode, $threads workers"
  & java -cp 'out;lib/*' homework3.Main --mode $mode --threads $threads `
    --duration 30 --reset --csv $csv --print-reads true > $log
  if ($LASTEXITCODE -ne 0) { throw "Benchmark failed: $mode / $threads workers" }
}
Write-Host "Finished. Send results/cached_trial.csv for fair comparison."
