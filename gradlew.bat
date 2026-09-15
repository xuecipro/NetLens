@echo off
where gradle >nul 2>nul
if %errorlevel% neq 0 (
  echo Gradle not found. Install Gradle 8.7+ or use GitHub Actions CI.
  exit /b 1
)
gradle %*
