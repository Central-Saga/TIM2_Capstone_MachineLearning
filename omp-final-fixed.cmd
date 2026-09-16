@echo off
REM OMP Command Wrapper - Fixed for Windows paths
REM Works in any terminal (PowerShell, CMD, Antigravity)

REM Detect correct home path
if exist "C:\Users\Marsel" (
    set HOME_PATH=C:\Users\Marsel
) else if exist "C:\home\marsel" (
    set HOME_PATH=C:\home\marsel
) else (
    set HOME_PATH=%USERPROFILE%
)

echo ================================================
echo Running OMP with detected HOME: %HOME_PATH%
echo ================================================
echo.

REM Check if Node.js exists
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found! Please install Node.js first.
    exit /b 1
)
echo [OK] Node.js available

REM Set correct CLI path based on detected HOME
set "CLI_PATH=%HOME_PATH%\.npm-global\node_modules\@oh-my-pi\pi-coding-agent\dist\cli.js"

REM Verify file exists
if not exist "%CLI_PATH%" (
    echo [ERROR] OMP CLI not found at: %CLI_PATH%
    
    REM Try alternative detection
    echo Attempting to find OMP installation...
    
    set "FOUND_CLI="
    
    rem Check multiple locations
    for %%P in ("%HOME_PATH%\.npm-global" "%LOCALAPPDATA%\oh-my-pi" "C:\home\marsel\.npm-global") do (
        if not defined FOUND_CLI (
            if exist "%%P\node_modules\@oh-my-pi\pi-coding-agent\dist\cli.js" (
                set "CLI_PATH=%%P\node_modules\@oh-my-pi\pi-coding-agent\dist\cli.js"
                set "FOUND_CLI=YES"
            )
        )
    )
    
    if not defined CLI_PATH (
        echo [CRITICAL] Could not locate OMP installation!
        echo Please reinstall: npm install -g @oh-my-pi/pi-coding-agent
        exit /b 1
    )
)

echo [OK] OMP CLI located: %CLI_PATH%
echo.
echo [EXECUTING] Running OMP command...
echo.

REM Run via Node.js
node "%CLI_PATH%" %*
