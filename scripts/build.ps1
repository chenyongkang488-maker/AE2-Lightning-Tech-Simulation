param([switch]$GameTests,[switch]$Client,[switch]$Mekanism)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
if(-not $env:JAVA_HOME -and (Test-Path -LiteralPath 'D:\DevTools\Java\jdk-21.0.12.1+1')){$env:JAVA_HOME='D:\DevTools\Java\jdk-21.0.12.1+1'}
if(-not $env:JAVA_HOME){throw 'Set JAVA_HOME to a Java 21 JDK.'}
if(-not $env:GRADLE_USER_HOME){$env:GRADLE_USER_HOME='D:\DevTools\GradleCache'}
& (Join-Path $PSScriptRoot 'bootstrap.ps1')
$gradleExecutable=Join-Path $projectRoot 'gradlew.bat'
if(Test-Path -LiteralPath 'D:\DevTools\Gradle-8.8\bin\gradle.bat'){$gradleExecutable='D:\DevTools\Gradle-8.8\bin\gradle.bat'}
$tasks=@('build','--console=plain')
if($GameTests){$tasks+= 'runGameTestServer'}
if($Client){$tasks+= 'runClient'}
if($Mekanism){$tasks+= '-PmekTests'}
Push-Location -LiteralPath $projectRoot
try{& $gradleExecutable @tasks;if($LASTEXITCODE -ne 0){throw "Gradle failed: $LASTEXITCODE"}}finally{Pop-Location}
