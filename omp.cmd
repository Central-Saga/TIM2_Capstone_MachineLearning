@echo off
REM Universal OMP Command Wrapper
REM Works in any terminal including Antigravity!
REM Usage: omp.cmd run models refresh --provider=9router

set BUN_PATH=%USERPROFILE%\.bun\bin\bun.exe
set OMP_CLI=%USERPROFILE%\.npm-global\node_modules\@oh-my-pi\pi-coding-agent\dist\cli.js

if not exist "%BUN_PATH%" (
    echo ERROR: Bun not found at %BUN_PATH%
    echo Please install Bun first.
    exit /b 1
)

if not exist "%OMP_CLI%" (
    echo ERROR: OMP CLI not found at %OMP_CLI%
    echo Please reinstall @oh-my-pi/pi-coding-agent
    exit /b 1
)

REM Run via bun
"%BUN_PATH%" run omp %*
