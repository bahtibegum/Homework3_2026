$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
& .\scripts\build.ps1
New-Item -ItemType Directory -Force -Path results | Out-Null
# Set PG_URL / PG_USER / PG_PASSWORD first. All runs reset blocking_data.
# Do not mix these post-fix results with 16 Nov pre-fix measurements.
$variants = @(@('serial',1), @('concurrent',2), @('concurrent',4), @('concurrent',8),
              @('optimized',2), @('optimized',4), @('optimized',8))
foreach ($v in $variants) {
    $mode = $v[0]; $threads = $v[1]
    Write-Host "Running $mode, $threads workers"
    & java -cp 'out;lib/*' homework3.Main --mode $mode --threads $threads --duration 30 `
         --reset --csv results/benchmarks_after_fixes.csv > "results/${mode}_${threads}_after_fixes_read_sums.log"
    if ($LASTEXITCODE -ne 0) { throw "Benchmark failed: $mode threads=$threads" }
}
