$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
New-Item -ItemType Directory -Force -Path out | Out-Null
$javaFiles = @(Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName })
& javac -encoding UTF-8 -cp 'lib/*' -d out @javaFiles
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }
& java -cp 'out;lib/*' homework3.SelfTest
if ($LASTEXITCODE -ne 0) { throw 'SelfTest failed' }
