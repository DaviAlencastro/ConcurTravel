@echo off
echo Starting Travel Policy Service...

echo Ensure Ollama is running with model llama3.
echo Run: ollama serve (in another terminal)
echo Then: ollama pull llama3

if "%OPENAI_API_KEY%"=="" (
    echo ERROR: OPENAI_API_KEY environment variable is not set (needed for embeddings).
    echo Please set it with: set OPENAI_API_KEY=your_api_key_here
    pause
    exit /b 1
)

echo OPENAI_API_KEY is set.

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