[CmdletBinding()]
param(
    [string] $OutputDirectory,
    [string] $BrowserPath,
    [switch] $HtmlOnly
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$sourcePath = Join-Path $repoRoot 'docs\CODEBASE-HANDBOOK.md'
$cssPath = Join-Path $repoRoot 'docs\handbook\handbook-print.css'
$filterPath = Join-Path $repoRoot 'docs\handbook\handbook-render.lua'

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $outputRoot = Join-Path $repoRoot 'build\handbook'
} elseif ([IO.Path]::IsPathRooted($OutputDirectory)) {
    $outputRoot = [IO.Path]::GetFullPath($OutputDirectory)
} else {
    $outputRoot = [IO.Path]::GetFullPath((Join-Path $repoRoot $OutputDirectory))
}

foreach ($requiredPath in @($sourcePath, $cssPath, $filterPath)) {
    if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
        throw "Missing handbook build input: $requiredPath"
    }
}

$pandoc = Get-Command pandoc -ErrorAction Stop
New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null

$htmlPath = Join-Path $outputRoot 'CODEBASE-HANDBOOK.html'
$pdfPath = Join-Path $outputRoot 'CODEBASE-HANDBOOK.pdf'
$docsRoot = Join-Path $repoRoot 'docs'
$resourcePath = "$repoRoot;$docsRoot"

$pandocArguments = @(
    '--from=markdown+pipe_tables+task_lists+strikeout+tex_math_dollars+tex_math_single_backslash'
    '--to=html5'
    '--standalone'
    '--toc'
    '--toc-depth=2'
    '--mathml'
    '--embed-resources'
    "--resource-path=$resourcePath"
    "--css=$cssPath"
    "--lua-filter=$filterPath"
    '--metadata=title:Abdul Windows BilliardsEverything: Codebase Handbook'
    '--metadata=lang:en-US'
    '--syntax-highlighting=tango'
    "--output=$htmlPath"
    $sourcePath
)

& $pandoc.Source @pandocArguments
if ($LASTEXITCODE -ne 0) {
    throw "Pandoc failed with exit code $LASTEXITCODE."
}

$htmlFile = Get-Item -LiteralPath $htmlPath
if ($htmlFile.Length -lt 100000) {
    throw "Generated HTML is unexpectedly small: $($htmlFile.Length) bytes."
}

if ($HtmlOnly) {
    Write-Output "HTML: $htmlPath ($($htmlFile.Length) bytes)"
    exit 0
}

if ([string]::IsNullOrWhiteSpace($BrowserPath)) {
    $browserCandidates = @(
        'C:\Program Files\Google\Chrome\Application\chrome.exe'
        'C:\Program Files (x86)\Google\Chrome\Application\chrome.exe'
        'C:\Program Files\Microsoft\Edge\Application\msedge.exe'
        'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
    )
    $BrowserPath = $browserCandidates |
        Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } |
        Select-Object -First 1
}

if ([string]::IsNullOrWhiteSpace($BrowserPath) -or
    -not (Test-Path -LiteralPath $BrowserPath -PathType Leaf)) {
    throw 'Chrome or Edge was not found. Supply -BrowserPath, or use -HtmlOnly.'
}

$temporaryRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$browserProfile = Join-Path $temporaryRoot ("abdul-handbook-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $browserProfile | Out-Null
if (Test-Path -LiteralPath $pdfPath -PathType Leaf) {
    Remove-Item -LiteralPath $pdfPath -Force
}

try {
    $htmlUri = ([Uri]::new($htmlPath)).AbsoluteUri
    $browserArguments = @(
        '--headless=new'
        '--disable-gpu'
        '--disable-extensions'
        '--disable-breakpad'
        '--disable-crash-reporter'
        '--no-first-run'
        '--no-pdf-header-footer'
        '--generate-pdf-document-outline'
        "--user-data-dir=`"$browserProfile`""
        "--print-to-pdf=`"$pdfPath`""
        $htmlUri
    )

    $browserProcess = Start-Process `
        -FilePath $BrowserPath `
        -ArgumentList $browserArguments `
        -WindowStyle Hidden `
        -Wait `
        -PassThru
    $browserExitCode = $browserProcess.ExitCode
    if ($browserExitCode -ne 0 -and -not (Test-Path -LiteralPath $pdfPath -PathType Leaf)) {
        throw "PDF browser process failed with exit code $LASTEXITCODE."
    }
    if ($browserExitCode -ne 0) {
        Write-Warning "The browser returned exit code $browserExitCode after writing the PDF; validating the completed file."
    }
} finally {
    $resolvedProfile = [IO.Path]::GetFullPath($browserProfile)
    if ($resolvedProfile.StartsWith($temporaryRoot, [StringComparison]::OrdinalIgnoreCase) -and
        (Split-Path -Leaf $resolvedProfile).StartsWith('abdul-handbook-', [StringComparison]::Ordinal)) {
        Remove-Item -LiteralPath $resolvedProfile -Recurse -Force -ErrorAction SilentlyContinue
    }
}

$pdfFile = Get-Item -LiteralPath $pdfPath
if ($pdfFile.Length -lt 100000) {
    throw "Generated PDF is unexpectedly small: $($pdfFile.Length) bytes."
}

$stream = [IO.File]::OpenRead($pdfPath)
try {
    $signatureBytes = New-Object byte[] 5
    $bytesRead = $stream.Read($signatureBytes, 0, $signatureBytes.Length)
    $signature = [Text.Encoding]::ASCII.GetString($signatureBytes, 0, $bytesRead)
    if ($signature -ne '%PDF-') {
        throw "Generated file does not have a PDF signature: '$signature'."
    }
} finally {
    $stream.Dispose()
}

Write-Output "HTML: $htmlPath ($($htmlFile.Length) bytes)"
Write-Output "PDF:  $pdfPath ($($pdfFile.Length) bytes)"
exit 0
