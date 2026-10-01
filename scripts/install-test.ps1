param([string]$Instance='D:\mc\.minecraft\versions\OverloadSim-Test-1.21.1')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$jar=Join-Path $projectRoot 'build\libs\overload_sim-0.1.0-alpha.9.jar'
if(-not(Test-Path -LiteralPath $jar)){throw 'Build the mod first.'}
if(-not(Test-Path -LiteralPath (Join-Path $Instance 'mods'))){throw 'Expected an existing test instance with a mods directory.'}
$Instance=(Resolve-Path -LiteralPath $Instance).Path
$modsPath=(Resolve-Path -LiteralPath (Join-Path $Instance 'mods')).Path
# Preserve earlier addon versions; leave all other mods and launcher settings alone.
$backup=Join-Path $Instance ('addon-backups\'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
if(-not([IO.Path]::GetFullPath($backup).StartsWith($Instance+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase))){throw 'Backup path escapes the test instance.'}
foreach($oldJar in (Get-ChildItem -LiteralPath $modsPath -Filter 'overload_sim-*.jar' -File)){
    if(-not($oldJar.FullName.StartsWith($modsPath+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase))){throw 'Addon path escapes the mods directory.'}
    New-Item -ItemType Directory -Force -Path $backup | Out-Null
    Move-Item -LiteralPath $oldJar.FullName -Destination (Join-Path $backup $oldJar.Name)
}
$destination=Join-Path $Instance ('mods\'+(Split-Path -Leaf $jar));Copy-Item -LiteralPath $jar -Destination $destination
if((Get-FileHash -LiteralPath $jar).Hash -ne (Get-FileHash -LiteralPath $destination).Hash){throw 'Installed jar hash differs.'}
Write-Host "Installed $destination"
