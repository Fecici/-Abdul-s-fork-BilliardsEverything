[CmdletBinding()]
param(
    [string] $Workload,
    [int] $Samples = 0,
    [int[]] $WorkerCounts,
    [ValidateSet('baseline', 'debug', 'cpu-tuned')]
    [string] $BuildProfile = 'baseline',
    [switch] $SkipBuild,
    [switch] $List
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# Some automation hosts inject both `Path` and `PATH`. Windows treats them as
# one variable, but Windows PowerShell 5 Start-Process throws while copying the
# duplicate keys. Normalize only this runner process; the parent is unchanged.
$processEnvironment = [Environment]::GetEnvironmentVariables('Process')
$pathKeys = @($processEnvironment.Keys | Where-Object { $_ -ieq 'Path' })
if ($pathKeys.Count -gt 1) {
    $processPath = [string] $processEnvironment[$pathKeys[0]]
    foreach ($pathKey in $pathKeys) {
        [Environment]::SetEnvironmentVariable([string] $pathKey, $null, 'Process')
    }
    [Environment]::SetEnvironmentVariable('Path', $processPath, 'Process')
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$manifestPath = Join-Path $PSScriptRoot 'workloads.json'
$manifest = Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json
$allWorkloads = @($manifest.workloads)

if ($List) {
    $allWorkloads | Select-Object id, tier, description | Format-Table -AutoSize
    exit 0
}

if ([string]::IsNullOrWhiteSpace($Workload)) {
    throw 'Specify -Workload <id>, or use -List.'
}

$selected = @($allWorkloads | Where-Object id -eq $Workload)
if ($selected.Count -ne 1) {
    throw "Unknown or duplicate workload '$Workload'. Use -List."
}
$case = $selected[0]

$resolvedWorkers = @(if ($WorkerCounts -and $WorkerCounts.Count -gt 0) {
    $WorkerCounts | ForEach-Object { [int] $_ }
} else {
    $case.workerCounts | ForEach-Object { [int] $_ }
})
$logicalProcessors = [Environment]::ProcessorCount
foreach ($worker in $resolvedWorkers) {
    if ($worker -lt 1 -or $worker -gt $logicalProcessors) {
        throw "Worker count $worker is outside 1..$logicalProcessors for this host."
    }
}

$measuredSamples = if ($Samples -gt 0) { $Samples } else { [int] $case.samples }
if ($measuredSamples -lt 1) {
    throw 'Measured sample count must be positive.'
}

$profileArguments = switch ($BuildProfile) {
    'debug' { @('-PbilliardsNativeDebug=true') }
    'cpu-tuned' { @('-PbilliardsNativeCpuTuned=true') }
    default { @() }
}

$effectiveBuildProfile = $BuildProfile
if ($SkipBuild) {
    $effectiveBuildProfile = "$BuildProfile-unverified-skip-build"
    Write-Warning 'Skipping the build means compiler/profile flags cannot be verified; timings will be labeled unverified.'
}

if (-not $SkipBuild) {
    Push-Location $repoRoot
    try {
        & '.\gradlew.bat' '--no-daemon' @profileArguments 'testExecutable'
        if ($LASTEXITCODE -ne 0) {
            throw "Native benchmark build failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

$executable = Join-Path $repoRoot 'build\exe\test\test.exe'
if (-not (Test-Path -LiteralPath $executable -PathType Leaf)) {
    throw "Missing native test executable: $executable"
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$sessionRoot = Join-Path $repoRoot "build\benchmarks\$timestamp-$($case.id)-$effectiveBuildProfile"
New-Item -ItemType Directory -Force -Path $sessionRoot | Out-Null

function Invoke-GitText {
    param([Parameter(Mandatory)][string[]] $Arguments)
    $value = & git -c "safe.directory=$repoRoot" -C $repoRoot @Arguments 2>$null
    if ($LASTEXITCODE -ne 0) { return $null }
    return ($value -join "`n").Trim()
}

function Get-HostMetadata {
    $cpuName = $null
    $osCaption = [Environment]::OSVersion.VersionString
    try {
        $cpuName = (Get-CimInstance Win32_Processor | Select-Object -First 1 -ExpandProperty Name).Trim()
        $osCaption = (Get-CimInstance Win32_OperatingSystem | Select-Object -ExpandProperty Caption).Trim()
    } catch {
        $cpuName = $env:PROCESSOR_IDENTIFIER
    }

    $javaStart = [Diagnostics.ProcessStartInfo]::new()
    $javaFromHome = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { $null }
    $javaStart.FileName = if ($javaFromHome -and (Test-Path -LiteralPath $javaFromHome -PathType Leaf)) {
        $javaFromHome
    } else {
        (Get-Command java -ErrorAction Stop).Source
    }
    $javaStart.Arguments = '-version'
    $javaStart.UseShellExecute = $false
    $javaStart.RedirectStandardOutput = $true
    $javaStart.RedirectStandardError = $true
    $javaProcess = [Diagnostics.Process]::Start($javaStart)
    $javaVersion = ($javaProcess.StandardOutput.ReadToEnd() + $javaProcess.StandardError.ReadToEnd()).Trim()
    $javaProcess.WaitForExit()
    [pscustomobject]@{
        startedAt = (Get-Date).ToString('o')
        workload = $case.id
        tier = $case.tier
        corpus = $case.corpus
        requestedBuildProfile = $BuildProfile
        effectiveBuildProfile = $effectiveBuildProfile
        skipBuild = [bool] $SkipBuild
        gitCommit = Invoke-GitText -Arguments @('rev-parse', 'HEAD')
        gitStatus = Invoke-GitText -Arguments @('status', '--short')
        os = $osCaption
        cpu = $cpuName
        logicalProcessors = $logicalProcessors
        javaVersion = $javaVersion
        javaHome = $env:JAVA_HOME
        javaExecutable = $javaStart.FileName
        gradleArguments = @('--no-daemon') + $profileArguments + @('testExecutable')
        workerCounts = $resolvedWorkers
        warmupsPerWorker = [int] $case.warmups
        measuredSamplesPerWorker = $measuredSamples
    }
}

$metadata = Get-HostMetadata
$metadata | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 -LiteralPath (Join-Path $sessionRoot 'metadata.json')

function Invoke-Sample {
    param(
        [Parameter(Mandatory)][int] $Worker,
        [Parameter(Mandatory)][int] $Index,
        [Parameter(Mandatory)][bool] $Warmup
    )

    $kind = if ($Warmup) { 'warmup' } else { 'sample' }
    $stem = '{0}-w{1}-{2:D2}' -f $kind, $Worker, $Index
    $stdoutPath = Join-Path $sessionRoot "$stem.stdout.txt"
    $stderrPath = Join-Path $sessionRoot "$stem.stderr.txt"

    $environmentToRestore = @{}
    $sampleEnvironment = @{
        'BILLIARDS_NATIVE_THREADS' = $Worker.ToString()
        'BILLIARDS_BENCHMARK_WORKER' = $Worker.ToString()
        'Path' = "C:\msys64\ucrt64\bin;$([Environment]::GetEnvironmentVariable('Path', 'Process'))"
    }
    if ($case.PSObject.Properties.Name -contains 'environment') {
        foreach ($property in $case.environment.PSObject.Properties) {
            $sampleEnvironment[$property.Name] = [string] $property.Value
        }
    }

    foreach ($name in $sampleEnvironment.Keys) {
        $environmentToRestore[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
        [Environment]::SetEnvironmentVariable($name, $sampleEnvironment[$name], 'Process')
    }

    try {
        $clock = [Diagnostics.Stopwatch]::StartNew()
        $process = Start-Process -FilePath $executable `
            -ArgumentList @($case.arguments) `
            -WorkingDirectory $repoRoot `
            -RedirectStandardOutput $stdoutPath `
            -RedirectStandardError $stderrPath `
            -NoNewWindow -PassThru

        $peakWorkingSet = 0L
        while (-not $process.HasExited) {
            $process.Refresh()
            $peakWorkingSet = [Math]::Max($peakWorkingSet, $process.PeakWorkingSet64)
            Start-Sleep -Milliseconds 50
        }
        # WaitForExit populates ExitCode reliably in Windows PowerShell 5 even
        # after HasExited has already become true.
        $process.WaitForExit()
        $clock.Stop()
        $process.Refresh()
        $peakWorkingSet = [Math]::Max($peakWorkingSet, $process.PeakWorkingSet64)
        $cpuMilliseconds = $null
        try {
            $cpuTime = $process.TotalProcessorTime
            if ($null -ne $cpuTime) {
                $cpuMilliseconds = [double] $cpuTime.TotalMilliseconds
            }
        } catch {
            # Windows can release the process handle before CPU time is read.
            # Keep the sample valid and make unavailable metadata explicit.
        }
        $exitCode = [int] $process.ExitCode
    } finally {
        foreach ($name in $environmentToRestore.Keys) {
            [Environment]::SetEnvironmentVariable($name, $environmentToRestore[$name], 'Process')
        }
    }

    $stdout = Get-Content -Raw -LiteralPath $stdoutPath -ErrorAction SilentlyContinue
    $stderr = Get-Content -Raw -LiteralPath $stderrPath -ErrorAction SilentlyContinue
    $combined = "$stdout`n$stderr"
    $correctnessMatch = [regex]::Match($combined, [string] $case.correctnessRegex)
    $hash = if ($correctnessMatch.Success -and $correctnessMatch.Groups['hash'].Success) {
        $correctnessMatch.Groups['hash'].Value
    } else { $null }
    $valid = $exitCode -eq 0 -and $correctnessMatch.Success

    [pscustomobject]@{
        workload = $case.id
        tier = $case.tier
        workerCount = $Worker
        index = $Index
        warmup = $Warmup
        valid = $valid
        exitCode = $exitCode
        resultHash = $hash
        wallMilliseconds = [Math]::Round($clock.Elapsed.TotalMilliseconds, 3)
        cpuMilliseconds = if ($null -ne $cpuMilliseconds) { [Math]::Round($cpuMilliseconds, 3) } else { $null }
        peakWorkingSetBytes = $peakWorkingSet
        stdout = [IO.Path]::GetFileName($stdoutPath)
        stderr = [IO.Path]::GetFileName($stderrPath)
    }
}

$results = @()
foreach ($worker in $resolvedWorkers) {
    for ($i = 1; $i -le [int] $case.warmups; ++$i) {
        Write-Host "Warmup $i/$($case.warmups), workers=$worker"
        $results += Invoke-Sample -Worker $worker -Index $i -Warmup $true
    }
    for ($i = 1; $i -le $measuredSamples; ++$i) {
        Write-Host "Sample $i/$measuredSamples, workers=$worker"
        $results += Invoke-Sample -Worker $worker -Index $i -Warmup $false
    }
}

$results | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 -LiteralPath (Join-Path $sessionRoot 'samples.json')
$results | Export-Csv -NoTypeInformation -Encoding utf8 -LiteralPath (Join-Path $sessionRoot 'samples.csv')

$measured = @($results | Where-Object { -not $_.warmup })
$invalid = @($measured | Where-Object { -not $_.valid })
$hashes = @($measured.resultHash | Where-Object { $_ } | Sort-Object -Unique)
$summary = foreach ($group in ($measured | Group-Object workerCount | Sort-Object { [int] $_.Name })) {
    $orderedWall = @($group.Group.wallMilliseconds | Sort-Object)
    $middle = [int] [Math]::Floor($orderedWall.Count / 2)
    $median = if ($orderedWall.Count % 2) {
        $orderedWall[$middle]
    } else {
        ($orderedWall[$middle - 1] + $orderedWall[$middle]) / 2.0
    }
    $orderedCpu = @($group.Group.cpuMilliseconds | Where-Object { $null -ne $_ } | Sort-Object)
    $medianCpu = $null
    if ($orderedCpu.Count -gt 0) {
        $cpuMiddle = [int] [Math]::Floor($orderedCpu.Count / 2)
        $medianCpu = if ($orderedCpu.Count % 2) {
            $orderedCpu[$cpuMiddle]
        } else {
            ($orderedCpu[$cpuMiddle - 1] + $orderedCpu[$cpuMiddle]) / 2.0
        }
    }
    [pscustomobject]@{
        workerCount = [int] $group.Name
        samples = $group.Count
        valid = @($group.Group | Where-Object valid).Count
        medianWallMilliseconds = [Math]::Round($median, 3)
        minWallMilliseconds = [Math]::Round(($orderedWall | Measure-Object -Minimum).Minimum, 3)
        maxWallMilliseconds = [Math]::Round(($orderedWall | Measure-Object -Maximum).Maximum, 3)
        medianCpuMilliseconds = if ($null -ne $medianCpu) { [Math]::Round($medianCpu, 3) } else { $null }
        maxPeakWorkingSetBytes = ($group.Group.peakWorkingSetBytes | Measure-Object -Maximum).Maximum
        resultHash = @($group.Group.resultHash | Where-Object { $_ } | Sort-Object -Unique) -join ','
    }
}
$summary | Export-Csv -NoTypeInformation -Encoding utf8 -LiteralPath (Join-Path $sessionRoot 'summary.csv')

$report = @(
    "# Benchmark report: $($case.id)"
    ''
    "- Started: $($metadata.startedAt)"
    "- Commit: $($metadata.gitCommit)"
    "- Build profile: $effectiveBuildProfile"
    "- Corpus: ``$($case.corpus)``"
    "- Correctness failures: $($invalid.Count)"
    "- Distinct result hashes: $($hashes.Count)"
    ''
    '| Workers | Valid / samples | Median wall ms | Median CPU ms | Min ms | Max ms | Max working set bytes | Result hash |'
    '| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |'
    $($summary | ForEach-Object {
        "| $($_.workerCount) | $($_.valid) / $($_.samples) | $($_.medianWallMilliseconds) | $($_.medianCpuMilliseconds) | $($_.minWallMilliseconds) | $($_.maxWallMilliseconds) | $($_.maxPeakWorkingSetBytes) | $($_.resultHash) |"
    })
    ''
    'Raw sample output and metadata are in this directory. Do not compare this'
    'report with another profile, commit, fixture, or cache condition without'
    'calling out that difference.'
)
$report | Set-Content -Encoding utf8 -LiteralPath (Join-Path $sessionRoot 'report.md')

Write-Host "Benchmark evidence: $sessionRoot"
if ($invalid.Count -gt 0) {
    throw "$($invalid.Count) measured sample(s) failed correctness or process checks."
}
if ($hashes.Count -gt 1) {
    throw "Result hashes differ across samples/worker counts: $($hashes -join ', ')"
}
