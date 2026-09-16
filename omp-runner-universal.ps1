# Universal OMP Runner - Works Immediately Without Any Files!
# Run this in any terminal (PowerShell, Antigravity, etc.)

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "OMNIPOTENT OMP RUNNER - NO INSTALLATION NEEDED!" -ForegroundColor Cyan
Write-Host "============================================================`n" -ForegroundColor White

# Detect home directory
$homeDirs = @(
    "C:\Users\Marsel",
    "C:\home\marsel",
    $env:USERPROFILE
)

$homePath = $null
foreach ($dir in $homeDirs) {
    if (Test-Path $dir) {
        $homePath = $dir
        break
    }
}

if (-not $homePath) {
    Write-Host "[ERROR] Cannot detect home directory!" -ForegroundColor Red
    exit 1
}

Write-Host "[INFO] Detected HOME: $homePath" -ForegroundColor Gray

# Find OMP CLI
$ompPaths = @(
    "$homePath\.npm-global\node_modules\@oh-my-pi\pi-coding-agent\dist\cli.js",
    "$HOME_PATH\.npm-global\node_modules\@oh-my-pi\pi-coding-agent\dist\cli.js"
)

$cliPath = $null
foreach ($path in $ompPaths) {
    if (Test-Path $path) {
        $cliPath = $path
        break
    }
}

if (-not $cliPath) {
    Write-Host "[ERROR] OMP installation not found!" -ForegroundColor Red
    Write-Host "Please install: npm install -g @oh-my-pi/pi-coding-agent" -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] Found OMP at: $cliPath" -ForegroundColor Green

# Check for Node.js
$nodeCmd = Get-Command node -ErrorAction SilentlyContinue
if (-not $nodeCmd) {
    Write-Host "[ERROR] Node.js is required but not installed!" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Node.js available" -ForegroundColor Green

# Run OMP command
$command = if ($args.Count -gt 0) { $args } else { @("--help") }

Write-Host "`n[EXECUTING] Running OMP command..." -ForegroundColor Cyan
Write-Host "Command: node $cliPath $($command -join ' ')" -ForegroundColor Gray
Write-Host ""

& node $cliPath $command
