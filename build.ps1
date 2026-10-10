<#
.SYNOPSIS
    Builds vsiefix from source into a loadable jar.

.DESCRIPTION
    Compiles src/ against the Minecraft/NeoForge/Mixin jars found in your .minecraft/libraries
    folder, then packages the classes, resources/ and the LICENSE/README (as META-INF/ entries).

    Requirements: JDK 21 ("javac" and "jar" on PATH).

.EXAMPLE
    .\build.ps1 -McDir "$env:APPDATA\.minecraft"
    .\build.ps1 -McDir "E:\PCL\Snapshot 2.12.2\.minecraft" -Out "vsiefix-1.2.1.jar"
#>
param(
    [Parameter(Mandatory = $true)][string]$McDir,
    [string]$Out = "vsiefix-built.jar"
)

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$lib  = Join-Path $McDir 'libraries'
if (-not (Test-Path $lib)) { throw "libraries folder not found: $lib" }

function Find-One([string]$pattern) {
    $f = Get-ChildItem $lib -Recurse -Filter $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $f) { throw "not found under libraries/: $pattern" }
    return $f.FullName
}

$cp = @(
    (Find-One 'sponge-mixin-*.jar'),
    (Find-One 'loader-4.0.43.jar'),
    (Find-One 'client-1.21.1-*-srg.jar'),
    (Find-One 'neoforge-21.1.248-universal.jar')
)
Write-Host "classpath:"; $cp | ForEach-Object { Write-Host "  $_" }

$classes = Join-Path $here 'build\classes'
if (Test-Path $classes) { Remove-Item $classes -Recurse -Force }
New-Item -ItemType Directory -Force -Path $classes | Out-Null

$sources = @(Get-ChildItem -Recurse -Path (Join-Path $here 'src') -Filter '*.java').FullName
Write-Host ("compiling " + $sources.Count + " file(s) ...")
& javac -encoding UTF-8 --release 21 -nowarn -proc:none -cp ($cp -join ';') -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed with $LASTEXITCODE" }

$extra = Join-Path $here 'build\extra\META-INF'
New-Item -ItemType Directory -Force -Path $extra | Out-Null
Copy-Item (Join-Path $here 'LICENSE')   (Join-Path $extra 'LICENSE')   -Force
Copy-Item (Join-Path $here 'README.md') (Join-Path $extra 'README.md') -Force

$outPath = Join-Path $here $Out
if (Test-Path $outPath) { Remove-Item $outPath -Force }
& jar --create --file $outPath -C $classes . -C (Join-Path $here 'resources') . -C (Join-Path $here 'build\extra') .
if ($LASTEXITCODE -ne 0) { throw "jar failed with $LASTEXITCODE" }

Write-Host ("built: " + $outPath)
