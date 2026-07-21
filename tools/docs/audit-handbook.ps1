[CmdletBinding()]
param(
    [ValidateSet('inventory', 'enforce')]
    [string] $Mode = 'inventory',

    [switch] $SyncLedger,

    [switch] $CleanReaderAppendix,

    [switch] $SkipDoxygen
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$handbookPath = Join-Path $repoRoot 'docs\CODEBASE-HANDBOOK.md'
$ledgerPath = Join-Path $repoRoot 'docs\handbook\coverage.csv'
$outputRoot = Join-Path $repoRoot 'build\doc-audit'
$generatedStart = '<!-- BEGIN GENERATED SYMBOL REFERENCE -->'
$generatedEnd = '<!-- END GENERATED SYMBOL REFERENCE -->'

New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $ledgerPath) | Out-Null

function Convert-ToRepoPath {
    param([Parameter(Mandatory)][string] $Path)

    $absolute = if ([IO.Path]::IsPathRooted($Path)) {
        [IO.Path]::GetFullPath($Path)
    } else {
        [IO.Path]::GetFullPath((Join-Path $repoRoot $Path))
    }

    $repoPrefix = $repoRoot.TrimEnd('\') + '\'
    if (-not $absolute.StartsWith($repoPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Ctags returned a path outside the repository: $absolute"
    }

    return $absolute.Substring($repoPrefix.Length).Replace('\', '/')
}

function Invoke-CtagsInventory {
    $ctags = Get-Command ctags -ErrorAction Stop
    $ctagsOutput = Join-Path $outputRoot 'ctags.jsonl'
    Remove-Item -Force -ErrorAction SilentlyContinue -LiteralPath $ctagsOutput
    $arguments = @(
        '--sort=no'
        '--recurse=yes'
        '--output-format=json'
        '--fields=+nKSE'
        # Include file-scoped/static/private helpers. Omitting them made the old
        # inventory miss exactly the implementation details this handbook must explain.
        '--extras=+F'
        '--languages=Java,C++'
        '--langmap=C++:+.hpp.h'
        '-o'
        $ctagsOutput
        'src/java'
        'src/backend'
        'src/test/java'
        'src/test/cpp'
        'src/test/headers'
    )

    Push-Location $repoRoot
    try {
        $messages = & $ctags.Source @arguments 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "Ctags failed with exit code ${LASTEXITCODE}:`n$($messages -join "`n")"
        }
    } finally {
        Pop-Location
    }

    $ignoredKinds = @('package', 'local', 'parameter', 'label')
    $fileHashes = @{}
    $symbols = foreach ($line in (Get-Content -LiteralPath $ctagsOutput)) {
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }

        $tag = $line | ConvertFrom-Json
        if ($tag._type -ne 'tag' -or $tag.kind -in $ignoredKinds) {
            continue
        }

        $path = Convert-ToRepoPath $tag.path
        $language = if ($path.EndsWith('.java')) { 'java' } else { 'cpp' }
        $area = if ($path.StartsWith('src/test/')) { "$language-test" } else { $language }
        $scope = if ($tag.PSObject.Properties.Name -contains 'scope') { [string] $tag.scope } else { '' }
        $signature = if ($tag.PSObject.Properties.Name -contains 'signature') { [string] $tag.signature } else { '' }
        $qualifiedName = if ($scope) { "$scope#$($tag.name)" } else { [string] $tag.name }
        $id = '{0}:{1}:{2}{3}' -f $area, $path, $qualifiedName, $signature
        if (-not $fileHashes.ContainsKey($path)) {
            $absolutePath = Join-Path $repoRoot $path.Replace('/', '\')
            $fileHashes[$path] = (Get-FileHash -Algorithm SHA256 -LiteralPath $absolutePath).Hash.ToLowerInvariant()
        }

        [pscustomobject]@{
            id = $id
            area = $area
            language = $language
            path = $path
            line = [int] $tag.line
            kind = [string] $tag.kind
            scope = $scope
            name = [string] $tag.name
            signature = $signature
            qualifiedName = $qualifiedName
            sourceFingerprint = $fileHashes[$path]
        }
    }

    return @($symbols | Sort-Object id, line -Unique)
}

function Remove-ReaderGeneratedReference {
    $content = Get-Content -Raw -LiteralPath $handbookPath
    $startIndex = $content.IndexOf($generatedStart, [StringComparison]::Ordinal)
    $endIndex = $content.IndexOf($generatedEnd, [StringComparison]::Ordinal)

    if (($startIndex -ge 0) -xor ($endIndex -ge 0)) {
        throw 'The handbook contains only one generated-reference boundary.'
    }
    if ($startIndex -lt 0) {
        return
    }

    $endIndex += $generatedEnd.Length
    while ($endIndex -lt $content.Length -and ($content[$endIndex] -eq "`r" -or $content[$endIndex] -eq "`n")) {
        $endIndex++
    }
    $updated = $content.Substring(0, $startIndex).TrimEnd() + "`r`n`r`n" + $content.Substring($endIndex).TrimStart()
    [IO.File]::WriteAllText($handbookPath, $updated, [Text.UTF8Encoding]::new($false))
}

function Sync-CoverageLedger {
    param([Parameter(Mandatory)][object[]] $Symbols)

    $existingById = @{}
    if (Test-Path -LiteralPath $ledgerPath) {
        foreach ($row in (Import-Csv -LiteralPath $ledgerPath)) {
            $existingById[$row.id] = $row
        }
    }

    $rows = foreach ($symbol in ($Symbols | Sort-Object path, line, id)) {
        $existing = if ($existingById.ContainsKey($symbol.id)) { $existingById[$symbol.id] } else { $null }
        if ($null -eq $existing) {
            $isTest = $symbol.area.EndsWith('-test')
            [pscustomobject]@{
                id = $symbol.id
                area = $symbol.area
                path = $symbol.path
                line = $symbol.line
                kind = $symbol.kind
                qualifiedName = $symbol.qualifiedName
                signature = $symbol.signature
                classification = if ($isTest) { 'support-test' } else { 'unclassified' }
                entryAnchor = ''
                status = if ($isTest) { 'excluded' } else { 'missing' }
                sourceFingerprint = $symbol.sourceFingerprint
                rationale = if ($isTest) { 'Tests are supporting evidence; production behavior receives deep coverage.' } else { '' }
                reviewedAt = ''
            }
            continue
        }

        $status = [string] $existing.status
        if ($existing.sourceFingerprint -and $existing.sourceFingerprint -ne $symbol.sourceFingerprint -and
            $status -in @('explained', 'reviewed', 'grouped-trivial')) {
            $status = 'stale'
        }
        [pscustomobject]@{
            id = $symbol.id
            area = $symbol.area
            path = $symbol.path
            line = $symbol.line
            kind = $symbol.kind
            qualifiedName = $symbol.qualifiedName
            signature = $symbol.signature
            classification = $existing.classification
            entryAnchor = $existing.entryAnchor
            status = $status
            sourceFingerprint = $symbol.sourceFingerprint
            rationale = $existing.rationale
            reviewedAt = $existing.reviewedAt
        }
    }

    $rows | Export-Csv -NoTypeInformation -Encoding utf8 -LiteralPath $ledgerPath
}

function Write-ExternalInventory {
    param([Parameter(Mandatory)][object[]] $Symbols)

    $builder = [Text.StringBuilder]::new()
    [void] $builder.AppendLine('# Generated source-symbol inventory')
    [void] $builder.AppendLine()
    [void] $builder.AppendLine('This build artifact is navigation input for the explanatory ledger. A row is not documentation.')
    foreach ($fileGroup in ($Symbols | Group-Object path | Sort-Object Name)) {
        [void] $builder.AppendLine()
        [void] $builder.AppendLine("## ``$($fileGroup.Name)``")
        foreach ($symbol in ($fileGroup.Group | Sort-Object line, id)) {
            [void] $builder.AppendLine("- L$($symbol.line) $($symbol.kind) ``$($symbol.qualifiedName)$($symbol.signature)``")
        }
    }
    $builder.ToString() | Set-Content -Encoding utf8 -LiteralPath (Join-Path $outputRoot 'symbols.md')
}

function Invoke-DoxygenXml {
    $doxygen = Get-Command doxygen -ErrorAction Stop
    $configPath = Join-Path $PSScriptRoot 'Doxyfile.handbook'
    Push-Location $repoRoot
    try {
        & $doxygen.Source $configPath
        if ($LASTEXITCODE -ne 0) {
            throw "Doxygen failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

$symbols = @(Invoke-CtagsInventory)
if ($symbols.Count -eq 0) {
    throw 'Ctags produced no documentable symbols.'
}

$symbols | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 -LiteralPath (Join-Path $outputRoot 'symbols.json')
$symbols | Export-Csv -NoTypeInformation -Encoding utf8 -LiteralPath (Join-Path $outputRoot 'symbols.csv')
Write-ExternalInventory -Symbols $symbols

if ($CleanReaderAppendix) {
    Remove-ReaderGeneratedReference
}
if ($SyncLedger) {
    Sync-CoverageLedger -Symbols $symbols
}
if (-not (Test-Path -LiteralPath $ledgerPath)) {
    throw 'Coverage ledger is missing. Run once with -SyncLedger.'
}
if (-not $SkipDoxygen) {
    Invoke-DoxygenXml
}

$ledger = @(Import-Csv -LiteralPath $ledgerPath)
$inventoryById = @{}
foreach ($symbol in $symbols) { $inventoryById[$symbol.id] = $symbol }
$ledgerById = @{}
foreach ($row in $ledger) { $ledgerById[$row.id] = $row }

$validClassifications = @('deep', 'grouped-trivial', 'alias', 'ui-semantic', 'support-test')
$handbook = Get-Content -Raw -LiteralPath $handbookPath
$gaps = [Collections.Generic.List[object]]::new()

foreach ($symbol in $symbols) {
    if (-not $ledgerById.ContainsKey($symbol.id)) {
        $gaps.Add([pscustomobject]@{ id = $symbol.id; reason = 'missing-ledger-row' })
        continue
    }
    $row = $ledgerById[$symbol.id]
    if ($row.classification -notin $validClassifications) {
        $gaps.Add([pscustomobject]@{ id = $symbol.id; reason = 'unclassified' })
        continue
    }
    if ($row.sourceFingerprint -ne $symbol.sourceFingerprint) {
        $gaps.Add([pscustomobject]@{ id = $symbol.id; reason = 'stale-source-fingerprint' })
        continue
    }
    if ($row.classification -eq 'support-test') {
        if ($row.status -ne 'excluded') {
            $gaps.Add([pscustomobject]@{ id = $symbol.id; reason = 'support-test-status-must-be-excluded' })
        }
        continue
    }
    if ($row.classification -eq 'grouped-trivial') {
        if ($row.status -ne 'grouped-trivial' -or -not $row.rationale -or -not $row.entryAnchor) {
            $gaps.Add([pscustomobject]@{ id = $symbol.id; reason = 'incomplete-trivial-grouping' })
            continue
        }
        $marker = '<!-- handbook-group:{0} -->' -f $row.entryAnchor
        if (-not $handbook.Contains($marker)) {
            $gaps.Add([pscustomobject]@{ id = $symbol.id; reason = 'missing-group-anchor' })
        }
        continue
    }
    if ($row.status -ne 'reviewed' -or -not $row.entryAnchor) {
        $gaps.Add([pscustomobject]@{ id = $symbol.id; reason = 'entry-not-reviewed' })
        continue
    }
    $encodedId = [Net.WebUtility]::HtmlEncode($symbol.id)
    $marker = '<!-- handbook-entry:{0} -->' -f $encodedId
    if (-not $handbook.Contains($marker)) {
        $gaps.Add([pscustomobject]@{ id = $symbol.id; reason = 'missing-explanatory-entry-marker' })
    }
}

foreach ($row in $ledger) {
    if (-not $inventoryById.ContainsKey($row.id)) {
        $gaps.Add([pscustomobject]@{ id = $row.id; reason = 'unknown-ledger-symbol' })
    }
}

$production = @($symbols | Where-Object { -not $_.area.EndsWith('-test') })
$completeIds = @($production.id | Where-Object { $id = $_; -not ($gaps | Where-Object id -eq $id) })
$coverage = if ($production.Count -eq 0) { 100.0 } else { [Math]::Round(100.0 * $completeIds.Count / $production.Count, 2) }

$gaps | Export-Csv -NoTypeInformation -Encoding utf8 -LiteralPath (Join-Path $outputRoot 'explanatory-gaps.csv')
$coverageLines = @(
    '# Handbook explanatory-coverage audit'
    ''
    "- Inventory symbols: $($symbols.Count)"
    "- Production symbols: $($production.Count)"
    "- Complete production entries/groupings: $($completeIds.Count)"
    "- Explanatory gaps: $($gaps.Count)"
    "- Explanatory coverage: $coverage%"
    ''
    'Completion requires an authored reviewed entry or a justified trivial grouping.'
    'Generated symbol presence never contributes to this percentage.'
    ''
    '## Gaps by reason'
    ''
    $(if ($gaps.Count) { $gaps | Group-Object reason | Sort-Object Name | ForEach-Object { "- $($_.Name): $($_.Count)" } } else { '- None.' })
)
$coverageLines | Set-Content -Encoding utf8 -LiteralPath (Join-Path $outputRoot 'coverage.md')

Write-Host "Handbook explanatory coverage: $coverage% ($($completeIds.Count)/$($production.Count))"
Write-Host "Audit artifacts: $outputRoot"

if ($Mode -eq 'enforce' -and $gaps.Count -gt 0) {
    throw "Handbook explanatory enforcement failed with $($gaps.Count) gaps."
}
