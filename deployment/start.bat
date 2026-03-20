@echo off
REM -----------------------------------------------------------------------------
REM start.bat — Start the Paginass application (database + Spring Boot)
REM Usage: deployment\start.bat [--skip-build]
REM -----------------------------------------------------------------------------
setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."
set SKIP_BUILD=false

for %%A in (%*) do (
    if "%%A"=="--skip-build" set SKIP_BUILD=true
)

REM ── 1. Start the database ───────────────────────────────────────────────────
echo ^>^>^> Starting PostgreSQL via Docker Compose...
docker compose -f "%PROJECT_DIR%\docker-compose.yaml" up -d
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to start Docker Compose. >&2
    exit /b 1
)

REM ── 2. Wait for PostgreSQL to accept connections ────────────────────────────
echo ^>^>^> Waiting for PostgreSQL to be ready...
set RETRIES=30
:wait_loop
docker exec postgres_db pg_isready -U myuser -d mydatabase -q >nul 2>&1
if %ERRORLEVEL% equ 0 goto db_ready
set /a RETRIES=RETRIES-1
if %RETRIES% leq 0 (
    echo ERROR: PostgreSQL did not become ready in time. >&2
    exit /b 1
)
timeout /t 2 /nobreak >nul
goto wait_loop
:db_ready
echo ^>^>^> PostgreSQL is ready.

REM ── 3. Build the application ────────────────────────────────────────────────
if "%SKIP_BUILD%"=="false" (
    echo ^>^>^> Building application (skipping tests)...
    call "%PROJECT_DIR%\gradlew.bat" -p "%PROJECT_DIR%" build -x test
    if %ERRORLEVEL% neq 0 (
        echo ERROR: Build failed. >&2
        exit /b 1
    )
)

REM ── 4. Start Spring Boot ────────────────────────────────────────────────────
echo ^>^>^> Starting Paginass...
set DB_USERNAME=myuser
set DB_PASSWORD=mysecretpassword
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mydatabase

if "%GOOGLE_CLIENT_ID%"==""   set GOOGLE_CLIENT_ID=change-me
if "%GOOGLE_CLIENT_SECRET%"=="" set GOOGLE_CLIENT_SECRET=change-me
if "%GITHUB_CLIENT_ID%"==""   set GITHUB_CLIENT_ID=change-me
if "%GITHUB_CLIENT_SECRET%"=="" set GITHUB_CLIENT_SECRET=change-me

call "%PROJECT_DIR%\gradlew.bat" -p "%PROJECT_DIR%" bootRun
