@echo off
echo ========================================
echo KitchenGuard CSM v3.0 - Quick Start
echo ========================================
echo.
echo Starting KitchenGuard Headless REST API server...
echo JSON API Endpoint: http://localhost:8000/
echo API Health Check: http://localhost:8000/api/health
echo OpenAPI / Swagger Docs: http://localhost:8000/docs
echo.
echo Press Ctrl+C to stop the server
echo.

python -m uvicorn app:app --host 127.0.0.1 --port 8000 --reload

pause
