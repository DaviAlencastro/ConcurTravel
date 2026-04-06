@echo off
echo Starting Travel Policy Service...

echo Ensure Ollama is running with model llama3.
echo Run: ollama serve (in another terminal)
echo Then: ollama pull llama3

REM Check if Maven is available
mvn --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Maven not found. Please install Maven or use the Maven wrapper if available.
    pause
    exit /b 1
)

echo Running Spring Boot application...
mvn spring-boot:run

pause