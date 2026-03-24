param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Command,

    [Parameter(Position = 1)]
    [string]$LogName = "last-command"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$logsDir = Join-Path $repoRoot "logs"

if (-not (Test-Path $logsDir)) {
    New-Item -ItemType Directory -Path $logsDir | Out-Null
}

$stdoutPath = Join-Path $logsDir "$LogName.stdout.log"
$stderrPath = Join-Path $logsDir "$LogName.stderr.log"
$metaPath = Join-Path $logsDir "$LogName.meta.txt"

Remove-Item $stdoutPath, $stderrPath, $metaPath -ErrorAction SilentlyContinue

$startAt = Get-Date
$wrappedCommand = @"
& {
    $Command
}
"@

$process = Start-Process -FilePath "powershell.exe" `
    -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $wrappedCommand `
    -RedirectStandardOutput $stdoutPath `
    -RedirectStandardError $stderrPath `
    -PassThru `
    -Wait

$endAt = Get-Date
$meta = @(
    "command=$Command"
    "exit_code=$($process.ExitCode)"
    "started_at=$($startAt.ToString("o"))"
    "ended_at=$($endAt.ToString("o"))"
)

Set-Content -Path $metaPath -Value $meta -Encoding UTF8

Write-Host "stdout: $stdoutPath"
Write-Host "stderr: $stderrPath"
Write-Host "meta:   $metaPath"
Write-Host "exit:   $($process.ExitCode)"

exit $process.ExitCode
