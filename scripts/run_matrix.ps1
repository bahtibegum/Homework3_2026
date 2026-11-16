$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
& .\scripts\build.ps1
New-Item -ItemType Directory -Force -Path results | Out-Null
# Set the PG_PASSWORD environment variable first. All runs reset blocking_data.
$variants = @(@('serial',1), @('concurrent',2), @('concurrent',4), @('concurrent',8),
              @('optimized',2), @('optimized',4), @('optimized',8))
foreach ($v in $variants) {
    $mode = $v[0]; $threads = $v[1]
    Write-Host "Running $mode, $threads workers"
    & java -cp 'out;lib/*' homework3.Main --mode $mode --threads $threads --duration 30 `
         --reset --csv results/benchmarks.csv > "results/${mode}_${threads}_read_sums.log"
    if ($LASTEXITCODE -ne 0) { throw "Benchmark failed: $mode threads=$threads" }
}
