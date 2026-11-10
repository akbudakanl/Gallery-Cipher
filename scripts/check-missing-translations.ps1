# check-missing-translations.ps1
# Compares app/src/main/res/values/strings.xml (English baseline) against
# per-locale strings.xml files and reports missing string keys.
#
# Usage (from repo root):
#   .\scripts\check-missing-translations.ps1              # blind-test priority locales
#   .\scripts\check-missing-translations.ps1 -All         # all locales
#   .\scripts\check-missing-translations.ps1 -Locales tr,de

[CmdletBinding()]
param(
    # Optional comma-separated list of locale suffixes (without the "values-" prefix),
    # e.g. -Locales tr,de,ja
    [string]$Locales,

    # Scan every locale folder instead of the default priority set
    [switch]$All
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$resDir = Join-Path $repoRoot "app/src/main/res"
$baseFile = Join-Path $resDir "values/strings.xml"

if (-not (Test-Path $baseFile)) {
    Write-Error "Baseline not found: $baseFile (run this script from the repo, or via '${PSScriptRoot}')"
}

function Read-StringKeys([string]$Path) {
    (Select-String -Path $Path -Pattern '<string name="([^"]+)"') |
        ForEach-Object { $_.Matches[0].Groups[1].Value }
}

# key name -> raw English line, to display alongside missing keys
$baseKeys = Read-StringKeys $baseFile
$rawLines = @{}
Select-String -Path $baseFile -Pattern '^\s*<string name="([^"]+)"' | ForEach-Object {
    $rawLines[$_.Matches[0].Groups[1].Value] =
        ($_.Line.Trim() -replace '^<string[^>]*>', '' -replace '</string>.*$', '')
}

# Blind-test priority locales (see translation-blind-tests/README.md)
$priority = @('az', 'b+es+419', 'de', 'es', 'es-rUS', 'ja', 'nb-rNO', 'nn', 'tr', 'zh-rTW')

if ($All) {
    $localeDirs = Get-ChildItem $resDir -Directory |
        Where-Object { $_.Name -match '^values-' -and $_.Name -notmatch '^values-(sw|w|land|night|v\d+)' }
} elseif ($Locales) {
    $wanted = $Locales -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ }
    $localeDirs = $wanted | ForEach-Object {
        $d = Join-Path $resDir "values-$_"
        if (Test-Path (Join-Path $d 'strings.xml')) { Get-Item $d }
        else { Write-Warning "No locale folder 'values-$_' found, skipping." }
    }
} else {
    $localeDirs = $priority | ForEach-Object { Get-Item (Join-Path $resDir "values-$_") }
}

$totalMissing = 0
$results = foreach ($dir in $localeDirs) {
    $file = Join-Path $dir.FullName "strings.xml"
    if (-not (Test-Path $file)) { continue }

    $keys = Read-StringKeys $file
    $missing = $baseKeys | Where-Object { $keys -notcontains $_ }
    $extra = $keys | Where-Object { $baseKeys -notcontains $_ }
    $totalMissing += $missing.Count

    [PSCustomObject]@{
        Locale  = $dir.Name
        Have    = $keys.Count
        Missing = $missing.Count
        ExtraKeys = $extra.Count
        _MissingList = $missing
    }
}

$results | Sort-Object Missing -Descending | Format-Table Locale, Have, Missing, ExtraKeys -AutoSize

if ($totalMissing -eq 0) {
    Write-Host "OK - no missing strings in the scanned locales." -ForegroundColor Green
} else {
    Write-Host ""
    foreach ($r in ($results | Where-Object { $_.Missing -gt 0 })) {
        Write-Host "=== $($r.Locale) - $($r.Missing) missing ===" -ForegroundColor Yellow
        foreach ($k in $r._MissingList) {
            Write-Host ("  {0,-40} {1}" -f $k, $rawLines[$k])
        }
    }
    Write-Host ""
    Write-Host "Total missing: $totalMissing"
    Write-Host "Note: keys listed as ExtraKeys exist only in that locale file (leftover from upstream);"
    Write-Host "they are harmless and can be ignored."
}
