@echo off
setlocal enabledelayedexpansion
title Hyperlocal Marketplace Launcher

echo ========================================================
echo        Hyperlocal Marketplace - One-Click Launcher      
echo ========================================================
echo.

cd /d "%~dp0"

:: Load environment variables from .env file if present
if exist ".env" (
    echo [OK] Loading environment configuration from .env...
    for /f "usebackq tokens=1* delims==" %%A in (".env") do (
        set "line=%%A"
        if not "!line:~0,1!"=="#" (
            if not "%%A"=="" (
                set "%%A=%%B"
            )
        )
    )
    echo [OK] Environment variables loaded successfully.
) else (
    echo [WARN] .env file not found. Copy .env.example to .env to customize credentials.
    echo        Falling back to existing system environment variables and application defaults.
)

:: Locate Maven executable
set "MVN_CMD=mvn"
if exist "%~dp0tools\apache-maven-3.9.9\bin\mvn.cmd" (
    set "MVN_CMD=%~dp0tools\apache-maven-3.9.9\bin\mvn.cmd"
)

echo.
echo Starting Spring Boot Backend...
start "Hyperlocal Backend (Spring Boot)" cmd /k "cd /d "%~dp0backend" && "%MVN_CMD%" spring-boot:run"

echo Starting React Frontend (Vite)...
start "Hyperlocal Frontend (Vite)" cmd /k "cd /d "%~dp0frontend" && npm run dev"

echo.
echo ========================================================
echo  Services launched in separate windows:
echo  - Frontend Web UI:  http://localhost:5173
echo  - Backend REST API: http://localhost:8080/api/v1
echo ========================================================
echo.
pause
