param([string]$Target)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
if(-not $Target){$Target=Join-Path $projectRoot 'libs'}
New-Item -ItemType Directory -Force -Path $Target | Out-Null
foreach($dependency in (Get-Content -LiteralPath (Join-Path $PSScriptRoot 'dependencies.json') -Raw | ConvertFrom-Json)){
    $destination=Join-Path $Target $dependency.file
    if((Test-Path -LiteralPath $destination) -and (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant() -eq $dependency.sha256){Write-Host "Verified $($dependency.file)";continue}
    $temporary="$destination.download"
    Invoke-WebRequest -Uri $dependency.url -OutFile $temporary -TimeoutSec 180
    if((Get-FileHash -LiteralPath $temporary -Algorithm SHA256).Hash.ToLowerInvariant() -ne $dependency.sha256){throw "Checksum mismatch: $($dependency.file)"}
    Move-Item -LiteralPath $temporary -Destination $destination -Force
    Write-Host "Downloaded and verified $($dependency.file)"
}
