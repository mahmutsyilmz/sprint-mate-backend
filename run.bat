@echo off
REM Sprint Mate Backend - Development Startup Script
REM Loads environment variables from .env file and starts the application

echo Loading environment variables...

REM Load .env file
for /f "tokens=1,2 delims==" %%a in (.env) do (
    REM Skip comments (lines starting with #)
    echo %%a | findstr /b "#" >nul || set "%%a=%%b"
)

echo Starting Sprint Mate Backend...
echo.
echo GitHub Client ID: %GITHUB_CLIENT_ID%
echo.

mvn spring-boot:run
