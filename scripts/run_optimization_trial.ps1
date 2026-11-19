$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
& .\scripts\build.ps1
if ($LASTEXITCODE -ne 0) { throw 'Java build failed' }
New-Item -ItemType Directory -Force -Path results | Out-Null
# Safety: each run --reset deletes/reloads blocking_data. Use a DEDICATED homework database.
# Credentials: PG_URL, PG_USER, PG_PASSWORD (set in this PowerShell terminal first).
# Baseline and candidate are tested in interleaved order to reduce order/cache bias.
# Run the matrix again to check measurement stability; use a separate CSV name.
$csv = 'results/optimization_trial.csv'
if (Test-Path $csv) {
    throw "$csv already exists. Move or rename it first; never mix benchmark rounds."
}
$variants = @(
  @('serial',1),
  @('concurrent',2), @('roundtrip',2), @('optimized',2),
  @('roundtrip',4), @('concurrent',4), @('optimized',4),
  @('concurrent',8), @('roundtrip',8), @('optimized',8)
)
foreach ($v in $variants) {
    $mode = $v[0]; $threads = [int]$v[1]
    $log = "results/${mode}_${threads}_trial_read_sums.log"
    Write-Host "Running $mode with $threads workers, 30 seconds"
    & java -cp 'out;lib/*' homework3.Main --mode $mode --threads $threads --duration 30 `
      --reset --csv $csv --print-reads true > $log
    if ($LASTEXITCODE -ne 0) { throw "Benchmark failed: $mode/$threads" }
}
Write-Host "Finished. Upload $csv for analysis."
