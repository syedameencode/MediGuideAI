@echo off
title MediGuide AI - Healthcare Decision Support System
cd /d "%~dp0"
echo ======================================================================
echo   Launching MediGuide AI (Explainable Hybrid Decision Support System)
echo ======================================================================
echo.
call mvnw.cmd javafx:run
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Application exited with an error code %ERRORLEVEL%.
    pause
)
