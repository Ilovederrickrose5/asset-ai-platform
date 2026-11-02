@echo off
setlocal
title Asset AI Platform Startup

echo ==============================================
echo  Asset AI Platform
echo ==============================================
echo.

set "PROJECT_DIR=%~dp0"
cd /d "%PROJECT_DIR%"

if exist ".env" (
    for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do set "%%A=%%B"
    echo [OK] Loaded environment variables from .env
) else (
    echo [WARN] .env not found. Copy .env.example to .env and fill in the values.
    echo.
)

if "%ALIYUN_API_KEY%"=="" (
    echo [WARN] ALIYUN_API_KEY is not set. AI assistant will fail to call DashScope.
    echo        Set it before running: set ALIYUN_API_KEY=sk-your-api-key
    echo.
)

echo [INFO] Cleaning ports...
for %%P in (8081 8082 8083 9000 5173) do (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%%P"') do taskkill /F /PID %%a 2>nul
)
echo [OK] Ports cleaned.
echo.

echo [INFO] Starting infrastructure (Redis Stack + Nacos)...
docker compose up -d
if errorlevel 1 (
    echo [ERROR] docker compose failed. Make sure Docker Desktop is running.
    pause
    exit /b 1
)
echo [OK] Infrastructure started.
echo.

echo [INFO] Building project...
call mvnw.cmd clean install -DskipTests -q
if errorlevel 1 (
    echo [ERROR] Build failed.
    pause
    exit /b 1
)
echo [OK] Build done.
echo.

echo [INFO] Starting Auth Service (8081)...
start "Auth" cmd /k "cd /d ""%PROJECT_DIR%"" & call mvnw.cmd -pl asset-auth spring-boot:run"
echo [OK] Auth Service starting...
echo.

echo [INFO] Starting Business Service (8082)...
start "Business" cmd /k "cd /d ""%PROJECT_DIR%"" & call mvnw.cmd -pl asset-business spring-boot:run"
echo [OK] Business Service starting...
echo.

echo [INFO] Starting AI Assistant (8083)...
start "AI" cmd /k "cd /d ""%PROJECT_DIR%"" & call mvnw.cmd -pl asset-ai spring-boot:run"
echo [OK] AI Assistant starting...
echo.

echo [INFO] Starting API Gateway (9000)...
start "Gateway" cmd /k "cd /d ""%PROJECT_DIR%"" & call mvnw.cmd -pl asset-gateway spring-boot:run"
echo [OK] API Gateway starting...
echo.

echo [INFO] Starting Frontend...
start "Frontend" cmd /k "cd /d ""%PROJECT_DIR%frontend"" & npm.cmd run dev"
echo [OK] Frontend starting...
echo.

echo ==============================================
echo [SUCCESS] All services starting.
echo.
echo Access:
echo   Frontend: http://localhost:5173
echo   Gateway:  http://localhost:9000
echo   Nacos:    http://localhost:8848/nacos
echo   Auth:     http://localhost:8081
echo   Business: http://localhost:8082
echo   AI:       http://localhost:8083
echo ==============================================

endlocal
