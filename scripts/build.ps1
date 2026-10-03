param([switch]$GameTests, [switch]$Client, [switch]$Mekanism, [switch]$CropTests)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if ($env:JAVA_HOME) {
    $javaExecutable = Join-Path $env:JAVA_HOME 'bin/java'
} else {
    $javaExecutable = (Get-Command java -ErrorAction Stop).Source
}
$javaVersion = (& $javaExecutable --version | Out-String)
if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch '(?:openjdk|java) 21(?:[.\s])') {
    throw 'Use a Java 21 JDK: set JAVA_HOME or place Java 21 on PATH.'
}
& (Join-Path $PSScriptRoot 'bootstrap.ps1')
$wrapperName = if ($env:OS -eq 'Windows_NT') { 'gradlew.bat' } else { 'gradlew' }
$gradleExecutable = Join-Path $projectRoot $wrapperName
$tasks = @('verifyReleaseVersion', 'build', '--console=plain', '--no-daemon')
if ($GameTests) { $tasks += 'runGameTestServer' }
if ($Client) { $tasks += 'runClient' }
if ($Mekanism) { $tasks += '-PmekTests' }
if ($CropTests) {
    foreach ($file in @('MysticalAgriculture-1.21.1-8.0.28.jar', 'Cucumber-1.21.1-8.0.16.jar')) {
        if (-not (Test-Path -LiteralPath (Join-Path $projectRoot "libs/$file"))) {
            throw "Optional crop tests require libs/$file; see CONTRIBUTING.md."
        }
    }
    $tasks += '-PcropTests'
}
Push-Location -LiteralPath $projectRoot
try {
    & $gradleExecutable @tasks
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
} finally { Pop-Location }
